package kst4contest.test;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPreferencesStationMapAlwaysOnTopTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void alwaysOnTopIsDisabledByDefault() {
        assertFalse(new ChatPreferences(temporaryDirectory.resolve("unused.xml"))
                .isGUIstationMapAlwaysOnTop());
    }

    @Test
    void enabledAlwaysOnTopSurvivesFullRoundTrip() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("preferences.xml");
        ChatPreferences written = new ChatPreferences(preferencesFile);
        written.setGUIstationMapAlwaysOnTop(true);

        assertTrue(written.writePreferencesToXmlFile());
        assertTrue(Files.readString(preferencesFile).contains(
                "<GUIstationMapAlwaysOnTop>true</GUIstationMapAlwaysOnTop>"));

        ChatPreferences restored = new ChatPreferences(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(restored.isGUIstationMapAlwaysOnTop());
    }

    @Test
    void missingOrMalformedValueKeepsAlwaysOnTopDisabled() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("legacy.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>8</configVersion>
                    <guiOptions>
                        <GUIstationMapAlwaysOnTop>sometimes</GUIstationMapAlwaysOnTop>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences restored = new ChatPreferences(preferencesFile);
        restored.setGUIstationMapAlwaysOnTop(true);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertFalse(restored.isGUIstationMapAlwaysOnTop());
    }

    @Test
    void selectiveLayoutWritePersistsAlwaysOnTopWithoutFunctionalSettings() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("selective.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>8</configVersion>
                    <station><LoginCallSign>SAVED-CALL</LoginCallSign></station>
                    <guiOptions/>
                </praktiKST>
                """);
        ChatPreferences preferences = new ChatPreferences(preferencesFile);
        assertTrue(preferences.readPreferencesFromXmlFile());
        preferences.setStn_loginCallSign("UNSAVED-CALL");
        preferences.setGUIstationMapAlwaysOnTop(true);

        assertTrue(preferences.writeLayoutPreferencesToXmlFile());

        String xml = Files.readString(preferencesFile);
        assertTrue(xml.contains("<LoginCallSign>SAVED-CALL</LoginCallSign>"));
        assertFalse(xml.contains("UNSAVED-CALL"));
        assertTrue(xml.contains("<GUIstationMapAlwaysOnTop>true</GUIstationMapAlwaysOnTop>"));
    }
}
