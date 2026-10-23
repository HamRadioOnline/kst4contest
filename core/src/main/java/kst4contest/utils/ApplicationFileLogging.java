package kst4contest.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Controls the persistent application log without redirecting stdout/stderr. */
public final class ApplicationFileLogging implements AutoCloseable {
    /** Logger namespace owned by the application. */
    private static final String APP_LOGGER_NAME = "kst4contest";
    /** Maximum size of the active error log before it is archived. */
    private static final long DEFAULT_SIZE_LIMIT_BYTES = 20L * 1024L * 1024L;
    /** Active error log plus four archived generations. */
    private static final int DEFAULT_FILE_COUNT = 5;

    /** Runtime switch for additional application diagnostics. */
    private final AtomicBoolean debugEnabled = new AtomicBoolean();
    /** Strong reference to the root logger that owns the file handler. */
    private final Logger rootLogger = Logger.getLogger("");
    /** Strong reference to the application logger whose level is managed. */
    private final Logger appLogger = Logger.getLogger(APP_LOGGER_NAME);
    /** Application logger level that must be restored when this handler closes. */
    private final Level previousAppLevel;
    /** File handler installed on the root logger. */
    private final Handler handler;

    /**
     * Installs one append-only file handler.
     *
     * @param logFile target log file
     * @param debugEnabled whether INFO/FINE application diagnostics are included
     * @throws IOException if the handler cannot be created
     */
    public ApplicationFileLogging(
            final Path logFile,
            final boolean debugEnabled
    )
            throws IOException {
        this(logFile, debugEnabled,
                DEFAULT_SIZE_LIMIT_BYTES, DEFAULT_FILE_COUNT);
    }

    /** Test seam for exercising rotation without writing multi-megabyte files. */
    ApplicationFileLogging(
            final Path logFile,
            final boolean debugEnabled,
            final long sizeLimitBytes,
            final int fileCount
    ) throws IOException {
        this(logFile, debugEnabled, sizeLimitBytes, fileCount, ignored -> { });
    }

    /** Test seam for a transient file-system error after the active stream closes. */
    ApplicationFileLogging(
            final Path logFile,
            final boolean debugEnabled,
            final long sizeLimitBytes,
            final int fileCount,
            final RotationFaultInjector rotationFaultInjector
    ) throws IOException {
        final Path parent = logFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        this.debugEnabled.set(debugEnabled);
        previousAppLevel = appLogger.getLevel();

        final Handler fileHandler = new BoundedRotatingFileHandler(
                logFile, sizeLimitBytes, fileCount, rotationFaultInjector);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFilter(this::isLoggable);
        fileHandler.setFormatter(new UtcLogFormatter());

        try {
            appLogger.setLevel(Level.FINE);
            rootLogger.addHandler(fileHandler);
        } catch (RuntimeException exception) {
            fileHandler.close();
            try {
                appLogger.setLevel(previousAppLevel);
            } catch (RuntimeException restoreFailure) {
                exception.addSuppressed(restoreFailure);
            }
            throw exception;
        }
        handler = fileHandler;
    }

    /** Injects a test-only failure at the point where archive operations begin. */
    @FunctionalInterface
    interface RotationFaultInjector {
        void beforeArchive(Path logFile) throws IOException;
    }

    /** Keeps the active canonical file name while retaining bounded generations. */
    private static final class BoundedRotatingFileHandler extends Handler {
        private final Path logFile;
        private final long sizeLimitBytes;
        private final int fileCount;
        private final RotationFaultInjector rotationFaultInjector;
        private OutputStream output;
        private long currentSize;

        private BoundedRotatingFileHandler(
                final Path logFile,
                final long sizeLimitBytes,
                final int fileCount,
                final RotationFaultInjector rotationFaultInjector
        ) throws IOException {
            if (sizeLimitBytes <= 0L) {
                throw new IllegalArgumentException("sizeLimitBytes must be positive");
            }
            if (fileCount <= 0) {
                throw new IllegalArgumentException("fileCount must be positive");
            }
            this.logFile = logFile;
            this.sizeLimitBytes = sizeLimitBytes;
            this.fileCount = fileCount;
            this.rotationFaultInjector = rotationFaultInjector;
            open(true);
        }

        @Override
        public synchronized void publish(final LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }

            final byte[] encoded = getFormatter().format(record)
                    .getBytes(StandardCharsets.UTF_8);
            try {
                if (output == null) {
                    open(true);
                }
                if (currentSize > 0L
                        && currentSize + encoded.length > sizeLimitBytes) {
                    try {
                        rotate();
                    } catch (IOException | RuntimeException exception) {
                        reportError("Could not rotate " + logFile,
                                exception, ErrorManager.WRITE_FAILURE);
                        if (output == null) {
                            return;
                        }
                    }
                }
                output.write(encoded);
                output.flush();
                currentSize += encoded.length;
            } catch (IOException | RuntimeException exception) {
                reportError("Could not write or rotate " + logFile,
                        exception, ErrorManager.WRITE_FAILURE);
            }
        }

        @Override
        public synchronized void flush() {
            if (output == null) {
                return;
            }
            try {
                output.flush();
            } catch (IOException exception) {
                reportError("Could not flush " + logFile,
                        exception, ErrorManager.FLUSH_FAILURE);
            }
        }

        @Override
        public synchronized void close() {
            closeOutput();
        }

        private void rotate() throws IOException {
            closeOutput();
            try {
                rotationFaultInjector.beforeArchive(logFile);
                if (fileCount > 1) {
                    Files.deleteIfExists(generation(fileCount - 1));
                    for (int generation = fileCount - 2;
                         generation >= 1;
                         generation--) {
                        Path source = generation(generation);
                        if (Files.exists(source)) {
                            Files.move(source, generation(generation + 1),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    if (Files.exists(logFile)) {
                        Files.move(logFile, generation(1),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                open(false);
            } catch (IOException | RuntimeException exception) {
                try {
                    open(true);
                } catch (IOException | RuntimeException recoveryFailure) {
                    exception.addSuppressed(recoveryFailure);
                }
                throw exception;
            }
        }

        private Path generation(final int generation) {
            return logFile.resolveSibling(
                    logFile.getFileName() + "." + generation);
        }

        private void open(final boolean append) throws IOException {
            output = append
                    ? Files.newOutputStream(logFile,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.APPEND)
                    : Files.newOutputStream(logFile,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING);
            currentSize = Files.size(logFile);
        }

        private void closeOutput() {
            if (output == null) {
                return;
            }
            try {
                output.close();
            } catch (IOException exception) {
                reportError("Could not close " + logFile,
                        exception, ErrorManager.CLOSE_FAILURE);
            } finally {
                output = null;
            }
        }
    }

    /** Applies the runtime debug preference to subsequent file log records. */
    public void setDebugEnabled(final boolean enabled) {
        debugEnabled.set(enabled);
    }

    private boolean isLoggable(final LogRecord record) {
        boolean loggable = record.getThrown() != null
                || record.getLevel().intValue() >= Level.WARNING.intValue();
        if (!loggable) {
            final String loggerName = record.getLoggerName();
            final boolean appRecord = loggerName != null
                    && (APP_LOGGER_NAME.equals(loggerName)
                    || loggerName.startsWith(APP_LOGGER_NAME + "."));
            loggable = debugEnabled.get()
                    && appRecord
                    && record.getLevel().intValue() >= Level.FINE.intValue();
        }
        return loggable;
    }

    /** Removes and closes this handler. */
    @Override
    public void close() {
        rootLogger.removeHandler(handler);
        handler.close();
        appLogger.setLevel(previousAppLevel);
    }

    /** Formats file-log records with comparable UTC millisecond timestamps. */
    private static final class UtcLogFormatter extends Formatter {
        /** Timestamp format for every file-log record. */
        private static final DateTimeFormatter TIMESTAMP =
                DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .withZone(ZoneOffset.UTC);

        @Override
        public String format(final LogRecord record) {
            final StringBuilder line = new StringBuilder()
                    .append(TIMESTAMP.format(Instant.ofEpochMilli(
                            record.getMillis())))
                    .append('\t')
                    .append(record.getLevel().getName())
                    .append('\t')
                    .append(record.getLoggerName())
                    .append('\t')
                    .append(formatMessage(record))
                    .append(System.lineSeparator());
            if (record.getThrown() != null) {
                final StringWriter stackTrace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(stackTrace));
                line.append(stackTrace);
            }
            return line.toString();
        }
    }
}
