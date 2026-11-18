package kst4contest.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the return value of {@link ChatPreferences#readPreferencesFromXmlFile()}: it must
 * report a failed read as {@code false} so callers such as the setup wizard launcher can
 * tell an unreadable profile file from a readable one.
 */
class ChatPreferencesReadResultTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void aFileThatIsNotXmlIsReportedAsUnreadable() throws IOException {
        Path file = temporaryDirectory.resolve("notxml.xml");
        byte[] original = "this is not xml <<<".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);

        ChatPreferences preferences = new ChatPreferences(file);
        assertFalse(preferences.readPreferencesFromXmlFile(), "a non-XML file must be reported as unreadable");
        assertArrayEquals(original, Files.readAllBytes(file), "the unreadable file must be left untouched");
    }

    @Test
    void aMissingFileIsReportedAsUnreadable() {
        Path file = temporaryDirectory.resolve("missing.xml");

        ChatPreferences preferences = new ChatPreferences(file);
        assertFalse(preferences.readPreferencesFromXmlFile(), "a missing file must be reported as unreadable");
        assertFalse(Files.exists(file), "reading a missing file must not create it");
    }

    @Test
    void theBundledTemplateIsReadable() throws IOException {
        Path templateCopy = temporaryDirectory.resolve("template.xml");
        try (InputStream template = ChatPreferences.class
                .getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE)) {
            assertNotNull(template, "bundled resource " + ChatPreferences.PREFERENCE_RESOURCE);
            Files.copy(template, templateCopy, StandardCopyOption.REPLACE_EXISTING);
        }

        ChatPreferences fromTemplate = new ChatPreferences(templateCopy);
        assertTrue(fromTemplate.readPreferencesFromXmlFile(), "the bundled template must be readable");
    }
}
