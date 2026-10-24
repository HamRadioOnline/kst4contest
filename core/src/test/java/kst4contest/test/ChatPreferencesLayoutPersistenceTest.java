package kst4contest.test;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPreferencesLayoutPersistenceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void columnWidthsSurviveFullXmlRoundTripAndLayoutsStayIndependent() {
        Path preferencesFile = temporaryDirectory.resolve("preferences.xml");
        ChatPreferences written = preferencesAt(preferencesFile);
        written.setTableColumnWidth("dx-cluster-main", "message", 410.5);
        written.setTableColumnWidth("dx-cluster-monitor", "message", 275.25);

        assertTrue(written.writePreferencesToXmlFile());

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertEquals(410.5,
                restored.getTableColumnWidth("dx-cluster-main", "message").orElseThrow());
        assertEquals(275.25,
                restored.getTableColumnWidth("dx-cluster-monitor", "message").orElseThrow());
        assertFalse(restored.getTableColumnWidth("qso-other-main", "message").isPresent());
    }

    @Test
    void legacyXmlWithoutColumnWidthsKeepsWidthsAbsent() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("legacy.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>5</configVersion>
                    <guiOptions>
                        <GUIscn_ChatwindowMainSceneSizeHW>768;1234</GUIscn_ChatwindowMainSceneSizeHW>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());

        assertFalse(restored.getTableColumnWidth("public-messages", "time").isPresent());
    }

    @Test
    void invalidColumnWidthEntriesAreIgnored() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("invalid.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>6</configVersion>
                    <guiOptions>
                        <tableColumnWidth tableId="public-messages" columnId="callsign" pixels="NaN"/>
                        <tableColumnWidth tableId="public-messages" columnId="name" pixels="-20"/>
                        <tableColumnWidth tableId="public-messages" columnId="category" pixels="999999"/>
                        <tableColumnWidth tableId="public-messages" columnId="time" pixels="88.5"/>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());

        assertFalse(restored.getTableColumnWidth("public-messages", "callsign").isPresent());
        assertFalse(restored.getTableColumnWidth("public-messages", "name").isPresent());
        assertFalse(restored.getTableColumnWidth("public-messages", "category").isPresent());
        assertEquals(88.5,
                restored.getTableColumnWidth("public-messages", "time").orElseThrow());
    }

    @Test
    void selectiveLayoutWritePreservesDiskSettingsAndUnknownXml() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("selective.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>5</configVersion>
                    <station>
                        <LoginCallSign>SAVED-CALL</LoginCallSign>
                    </station>
                    <futureExtension mode="keep-me"><value>42</value></futureExtension>
                    <guiOptions>
                        <GUIscn_ChatwindowMainSceneSizeHW>700;1100</GUIscn_ChatwindowMainSceneSizeHW>
                        <futureLayoutValue>untouched</futureLayoutValue>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences preferences = preferencesAt(preferencesFile);
        assertTrue(preferences.readPreferencesFromXmlFile());
        preferences.setStn_loginCallSign("UNSAVED-CALL");
        preferences.getGUIscn_ChatwindowMainSceneSizeHW()[0] = 812;
        preferences.getGUIscn_ChatwindowMainSceneSizeHW()[1] = 1340;
        preferences.setGUIstationMapClusteringEnabled(false);
        preferences.setGUIselectedCallSignSplitPane_dividerposition(
                new double[] {0.68});
        preferences.setTableColumnWidth("qso-other-monitor", "call-tx", 123.75);

        assertTrue(preferences.writeLayoutPreferencesToXmlFile());

        String writtenXml = Files.readString(preferencesFile);
        assertTrue(writtenXml.contains("<LoginCallSign>SAVED-CALL</LoginCallSign>"));
        assertFalse(writtenXml.contains("UNSAVED-CALL"));
        assertTrue(writtenXml.contains("<futureExtension mode=\"keep-me\">"));
        assertTrue(writtenXml.contains("<futureLayoutValue>untouched</futureLayoutValue>"));
        assertTrue(writtenXml.contains("<configVersion>9</configVersion>"));
        assertTrue(writtenXml.contains("<GUIscn_ChatwindowMainSceneSizeHW>812.0;1340.0"));
        assertTrue(writtenXml.contains("<GUIstationMapClusteringEnabled>false"
                + "</GUIstationMapClusteringEnabled>"));

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertEquals("SAVED-CALL", restored.getStn_loginCallSign());
        assertFalse(restored.isGUIstationMapClusteringEnabled());
        assertArrayEquals(new double[] {0.68},
                restored.getGUIselectedCallSignSplitPane_dividerposition(), 0.0001);
        assertEquals(123.75,
                restored.getTableColumnWidth("qso-other-monitor", "call-tx").orElseThrow());
    }

    @Test
    void windowPositionsAndTheirOptInSurviveFullXmlRoundTrip() {
        Path preferencesFile = temporaryDirectory.resolve("windows.xml");
        ChatPreferences written = preferencesAt(preferencesFile);
        written.setGuiOptions_rememberWindowPositions(true);
        written.setWindowPosition("Map", 2100, -40, 1920, -200, 2560, 1440);

        assertTrue(written.writePreferencesToXmlFile());

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(restored.isGuiOptions_rememberWindowPositions());
        assertArrayEquals(new int[] { 2100, -40, 1920, -200, 2560, 1440 }, restored.getWindowPosition("Map"));
        assertNull(restored.getWindowPosition("settings-window"));
    }

    @Test
    void windowPositionsAreWrittenByTheLayoutAutosaveAndTheOptInDefaultsOff() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("windows-layout.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>8</configVersion>
                    <guiOptions>
                        <windowPosition windowId="Map" x="1" y="2" screenX="0" screenY="0" screenWidth="10" screenHeight="10"/>
                        <windowPosition windowId="broken" x="a" y="2" screenX="0" screenY="0" screenWidth="10" screenHeight="10"/>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences preferences = preferencesAt(preferencesFile);
        assertTrue(preferences.readPreferencesFromXmlFile());
        assertFalse(preferences.isGuiOptions_rememberWindowPositions());
        assertNull(preferences.getWindowPosition("broken"));
        preferences.setWindowPosition("main-window", 5, 6, 0, 0, 1920, 1080);

        assertTrue(preferences.writeLayoutPreferencesToXmlFile());

        ChatPreferences restored = preferencesAt(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertArrayEquals(new int[] { 1, 2, 0, 0, 10, 10 }, restored.getWindowPosition("Map"));
        assertArrayEquals(new int[] { 5, 6, 0, 0, 1920, 1080 }, restored.getWindowPosition("main-window"));
        assertEquals(1, Files.readString(preferencesFile).split("windowId=\"Map\"", -1).length - 1);
    }

    private ChatPreferences preferencesAt(Path preferencesFile) {
        return new ChatPreferences(preferencesFile);
    }
}
