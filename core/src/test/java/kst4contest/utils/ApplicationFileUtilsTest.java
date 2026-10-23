package kst4contest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ApplicationFileUtilsTest {

    @Test
    void debugFilesResolveBelowTheDedicatedApplicationDirectory() {
        assertEquals(
                Path.of(ApplicationFileUtils.getFilePath("praktiKST", "debug"),
                        "kst4contest-errors.log").toString(),
                ApplicationFileUtils.getDebugFilePath(
                        "praktiKST", "kst4contest-errors.log"));
    }
}
