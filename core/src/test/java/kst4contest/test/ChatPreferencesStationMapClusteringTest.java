package kst4contest.test;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPreferencesStationMapClusteringTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void clusteringIsEnabledByDefault() {
        assertTrue(new ChatPreferences(
                temporaryDirectory.resolve("unused-default.xml"))
                .isGUIstationMapClusteringEnabled());
    }

    @Test
    void disabledClusteringSurvivesFullXmlRoundTrip() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("preferences.xml");
        ChatPreferences written = preferencesAt(preferencesFile);
        written.setGUIstationMapClusteringEnabled(false);

        assertTrue(written.writePreferencesToXmlFile());

        String writtenXml = Files.readString(preferencesFile);
        assertTrue(writtenXml.contains("<configVersion>9</configVersion>"));
        assertTrue(writtenXml.contains("<GUIstationMapClusteringEnabled>false"
                + "</GUIstationMapClusteringEnabled>"));

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertFalse(restored.isGUIstationMapClusteringEnabled());
    }

    @Test
    void versionSixWithoutClusteringSettingKeepsClusteringEnabled() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("version-six.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>6</configVersion>
                    <guiOptions>
                        <GUIstationMapStageSceneSizeHW>1000.0;800.0</GUIstationMapStageSceneSizeHW>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences restored = preferencesAt(preferencesFile);
        restored.setGUIstationMapClusteringEnabled(false);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(restored.isGUIstationMapClusteringEnabled());
    }

    @Test
    void missingGuiOptionsKeepsClusteringEnabled() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("missing.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>6</configVersion>
                </praktiKST>
                """);

        ChatPreferences restored = preferencesAt(preferencesFile);
        restored.setGUIstationMapClusteringEnabled(false);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(restored.isGUIstationMapClusteringEnabled());
    }

    @Test
    void invalidClusteringValueKeepsClusteringEnabled() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("invalid.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>8</configVersion>
                    <guiOptions>
                        <GUIstationMapClusteringEnabled>sometimes</GUIstationMapClusteringEnabled>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences restored = preferencesAt(preferencesFile);
        restored.setGUIstationMapClusteringEnabled(false);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(restored.isGUIstationMapClusteringEnabled());
    }

    private ChatPreferences preferencesAt(Path preferencesFile) {
        return new ChatPreferences(preferencesFile);
    }
}
