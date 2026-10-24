package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageHistoryRecorderTest {
    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-10-03T10:15:27.384Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void disabledRecorderDoesNotCreateOrWriteHistory() throws Exception {
        try (MessageHistoryRecorder recorder = recorder(false)) {
            recorder.recordRx("OK|");
            recorder.recordTx("CK|");
        }

        assertFalse(Files.exists(historyFile()));
    }

    @Test
    void writesRxAndTxAsThreeTabSeparatedFieldsWithUtcMilliseconds()
            throws Exception {
        try (MessageHistoryRecorder recorder = recorder(true)) {
            recorder.recordRx("CH|2|raw\tframe|");
            recorder.recordTx("MSG|2|0|hello|0|");
        }

        assertEquals(List.of(
                "2026-10-03T10:15:27.384Z\tRX\tCH|2|raw\tframe|",
                "2026-10-03T10:15:27.384Z\tTX\tMSG|2|0|hello|0|"),
                Files.readAllLines(historyFile()));
    }

    @Test
    void redactsOnlyThePasswordFieldOfLoginFrames() throws Exception {
        try (MessageHistoryRecorder recorder = recorder(true)) {
            recorder.recordTx(
                    "LOGINC|DL1ABC|top-secret|2|KST4Contest|25|0|1|0|0|");
            recorder.recordTx("MSG|2|0|top-secret and LOGINC in chat|0|");
        }

        List<String> lines = Files.readAllLines(historyFile());
        assertTrue(lines.get(0).endsWith(
                "\tTX\tLOGINC|DL1ABC|<REDACTED>|2|KST4Contest|25|0|1|0|0|"));
        assertFalse(lines.get(0).contains("top-secret"));
        assertTrue(lines.get(1).endsWith(
                "\tTX\tMSG|2|0|top-secret and LOGINC in chat|0|"));
    }

    @Test
    void concurrentRxAndTxWritesNeverMixLines() throws Exception {
        int eventCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try (MessageHistoryRecorder recorder = recorder(true)) {
            for (int index = 0; index < eventCount; index++) {
                int event = index;
                executor.submit(() -> {
                    start.await();
                    if (event % 2 == 0) {
                        recorder.recordRx("RXFRAME|" + event + "|");
                    } else {
                        recorder.recordTx("TXFRAME|" + event + "|");
                    }
                    return null;
                });
            }
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        List<String> lines = Files.readAllLines(historyFile());
        assertEquals(eventCount, lines.size());
        Set<Integer> events = new HashSet<>();
        for (String line : lines) {
            String[] fields = line.split("\t", 3);
            assertEquals(3, fields.length);
            assertTrue(fields[1].equals("RX") || fields[1].equals("TX"));
            String[] frame = fields[2].split("\\|");
            events.add(Integer.parseInt(frame[1]));
        }
        assertEquals(eventCount, events.size());
    }

    @Test
    void rotatesOncePerProgramSessionAndReenableAppendsCurrentFile()
            throws Exception {
        Files.writeString(historyFile(), "previous session\n");

        try (MessageHistoryRecorder recorder = recorder(true)) {
            recorder.recordRx("FIRST|");
            recorder.setEnabled(false);
            recorder.setEnabled(true);
            recorder.recordTx("SECOND|");
        }

        List<Path> archives = new ArrayList<>();
        try (var files = Files.list(temporaryDirectory)) {
            files.filter(path -> path.getFileName().toString()
                            .matches("Messagehistory-\\d{8}-\\d{6}Z(?:-\\d+)?\\.raw"))
                    .forEach(archives::add);
        }
        assertEquals(1, archives.size());
        assertEquals("previous session\n", Files.readString(archives.get(0)));
        assertEquals(2, Files.readAllLines(historyFile()).size());
    }

    @Test
    void ioFailureNeverEscapesToConnectionThreads() throws Exception {
        Path applicationPathThatIsAFile = temporaryDirectory.resolve("not-a-dir");
        Files.writeString(applicationPathThatIsAFile, "occupied");

        assertDoesNotThrow(() -> {
            try (MessageHistoryRecorder recorder = new MessageHistoryRecorder(
                    applicationPathThatIsAFile,
                    true,
                    Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC))) {
                recorder.recordRx("OK|");
                recorder.recordTx("CK|");
            }
        });
    }

    @Test
    void dedicatedDebugDirectoryIsCreatedWithoutMovingLegacyHistory()
            throws Exception {
        Path legacyHistory = historyFile();
        Files.writeString(legacyHistory, "legacy history\n");
        Path debugDirectory = temporaryDirectory.resolve("debug");

        try (MessageHistoryRecorder recorder = new MessageHistoryRecorder(
                debugDirectory,
                true,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC))) {
            recorder.recordRx("OK|");
        }

        assertEquals("legacy history\n", Files.readString(legacyHistory));
        assertTrue(Files.isRegularFile(
                debugDirectory.resolve("Messagehistory.raw")));
    }

    private MessageHistoryRecorder recorder(boolean enabled) {
        return new MessageHistoryRecorder(
                temporaryDirectory,
                enabled,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    }

    private Path historyFile() {
        return temporaryDirectory.resolve("Messagehistory.raw");
    }
}
