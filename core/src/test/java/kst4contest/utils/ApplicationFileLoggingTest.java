package kst4contest.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationFileLoggingTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void warningsAreAlwaysLoggedAndDiagnosticsFollowDebugMode()
            throws Exception {
        Path logFile = temporaryDirectory.resolve("kst4contest-errors.log");
        Logger logger = Logger.getLogger(
                "kst4contest.test.filelogging." + UUID.randomUUID());

        try (ApplicationFileLogging logging =
                     new ApplicationFileLogging(logFile, false)) {
            logger.log(Level.INFO, "diagnostic while disabled");
            logger.log(Level.WARNING, "warning while disabled");
            logger.log(Level.INFO, "exception while disabled",
                    new IllegalStateException("simulated failure"));

            logging.setDebugEnabled(true);
            logger.log(Level.FINE, "fine diagnostic while enabled");
            logger.log(Level.INFO, "info diagnostic while enabled");
            logger.log(Level.SEVERE, "error while enabled");
        }

        String log = Files.readString(logFile);
        assertFalse(log.contains("diagnostic while disabled"));
        assertTrue(log.contains("warning while disabled"));
        assertTrue(log.contains("exception while disabled"));
        assertTrue(log.contains("simulated failure"));
        assertTrue(log.contains("fine diagnostic while enabled"));
        assertTrue(log.contains("info diagnostic while enabled"));
        assertTrue(log.contains("error while enabled"));
        assertTrue(log.contains("Z\tWARNING\t"));
    }

    @Test
    void createsTheDebugDirectoryOnDemand() throws Exception {
        Path logFile = temporaryDirectory.resolve("debug/kst4contest-errors.log");
        Logger logger = Logger.getLogger(
                "kst4contest.test.filelogging." + UUID.randomUUID());

        try (ApplicationFileLogging logging =
                     new ApplicationFileLogging(logFile, false)) {
            logger.warning("directory test");
        }

        assertTrue(Files.isRegularFile(logFile));
    }

    @Test
    void rotatesAtTheConfiguredLimitAndKeepsFiveFiles() throws Exception {
        Path logFile = temporaryDirectory.resolve("debug/kst4contest-errors.log");
        Logger logger = Logger.getLogger(
                "kst4contest.test.filelogging." + UUID.randomUUID());

        try (ApplicationFileLogging logging =
                     new ApplicationFileLogging(logFile, true, 320L, 5)) {
            for (int index = 0; index < 40; index++) {
                logger.info("rotation entry " + index + " " + "x".repeat(120));
            }
        }

        assertTrue(Files.isRegularFile(logFile));
        for (int generation = 1; generation < 5; generation++) {
            assertTrue(Files.isRegularFile(
                    logFile.resolveSibling(logFile.getFileName() + "." + generation)));
        }
        try (var files = Files.list(logFile.getParent())) {
            assertEquals(5L, files.filter(path -> path.getFileName().toString()
                    .startsWith("kst4contest-errors.log")).count());
        }
    }

    @Test
    void anUnusableLogFileIsReportedWithoutChangingTheLoggerLevel() throws Exception {
        Path logFile = temporaryDirectory.resolve("debug/kst4contest-errors.log");
        Files.createDirectories(logFile);

        Logger appLogger = Logger.getLogger("kst4contest");
        Level previousLevel = appLogger.getLevel();
        appLogger.setLevel(Level.SEVERE);

        try {
            assertThrows(IOException.class, () -> new ApplicationFileLogging(
                    logFile, false));
            assertEquals(Level.SEVERE, appLogger.getLevel());
        } finally {
            appLogger.setLevel(previousLevel);
        }
    }

    @Test
    void continuesWritingAfterATransientRotationFailure() throws Exception {
        Path logFile = temporaryDirectory.resolve("debug/kst4contest-errors.log");
        Logger logger = Logger.getLogger(
                "kst4contest.test.filelogging." + UUID.randomUUID());
        AtomicBoolean failNextRotation = new AtomicBoolean(true);

        try (ApplicationFileLogging logging = new ApplicationFileLogging(
                logFile, true, 320L, 5,
                ignored -> {
                    if (failNextRotation.getAndSet(false)) {
                        throw new IOException("simulated transient rotation failure");
                    }
                })) {
            logger.info("before rotation " + "x".repeat(180));
            logger.info("written despite failed rotation " + "x".repeat(180));
            logger.info("written after rotation recovery " + "x".repeat(180));
        }

        Path firstGeneration = logFile.resolveSibling(
                logFile.getFileName() + ".1");
        assertTrue(Files.readString(firstGeneration)
                .contains("written despite failed rotation"));
        assertTrue(Files.readString(logFile)
                .contains("written after rotation recovery"));
    }
}
