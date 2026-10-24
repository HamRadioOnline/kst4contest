package kst4contest.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import kst4contest.model.ChatPreferences;

class ChatPreferencesDebugModeTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void debugModeToFileIsEnabledByDefault() {
        assertTrue(new ChatPreferences(
                temporaryDirectory.resolve("unused-default.xml"))
                .isMessageHandling_debugModeToFileEnabled());
    }

    @Test
    void versionSevenWithoutDebugSettingKeepsDebugModeEnabled()
            throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("version-seven.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>7</configVersion>
                    <messageHandling>
                        <autoAnswerEnabled>false</autoAnswerEnabled>
                    </messageHandling>
                </praktiKST>
                """);

        ChatPreferences restored = preferencesAt(preferencesFile);
        restored.setMessageHandling_debugModeToFileEnabled(false);

        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(restored.isMessageHandling_debugModeToFileEnabled());
    }

    @Test
    void disabledDebugModeSurvivesFullXmlRoundTrip() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("preferences.xml");
        ChatPreferences written = preferencesAt(preferencesFile);
        written.setMessageHandling_debugModeToFileEnabled(false);

        assertTrue(written.writePreferencesToXmlFile());

        String writtenXml = Files.readString(preferencesFile);
        assertTrue(writtenXml.contains("<configVersion>9</configVersion>"));
        assertTrue(writtenXml.contains(
                "<messageHandling_debugModeToFileEnabled>false"
                        + "</messageHandling_debugModeToFileEnabled>"));

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertFalse(restored.isMessageHandling_debugModeToFileEnabled());
    }

    private ChatPreferences preferencesAt(Path preferencesFile) {
        return new ChatPreferences(preferencesFile);
    }
}
