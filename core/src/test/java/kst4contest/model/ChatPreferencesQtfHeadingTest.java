package kst4contest.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPreferencesQtfHeadingTest {

    @TempDir
    Path directory;

    @Test
    void loadedQtfDefaultInitializesTheRuntimeHeadingOnce() {
        Path file = directory.resolve("preferences.xml");
        ChatPreferences written = new ChatPreferences(file);
        written.setStn_qtfDefault(247.5);
        assertTrue(written.writePreferencesToXmlFile());

        ChatPreferences restored = new ChatPreferences(file);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertEquals(247.5, restored.getActualQTF().get(), 0.0001);

        restored.getActualQTF().set(91.0);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertEquals(91.0, restored.getActualQTF().get(), 0.0001,
                "reloading preferences overwrote a later runtime rotor heading");
    }

    @Test
    void eachProfileStartsWithItsOwnQtfDefault() {
        Path rootFile = directory.resolve("preferences.xml");
        Path operatorFile = directory.resolve("profiles/OP2/preferences.xml");
        writeQtfDefault(rootFile, 135.0);
        writeQtfDefault(operatorFile, 302.0);

        ChatPreferences root = read(rootFile);
        ChatPreferences operator = read(operatorFile);

        assertEquals(135.0, root.getActualQTF().get(), 0.0001);
        assertEquals(302.0, operator.getActualQTF().get(), 0.0001);
    }

    @Test
    void qtfFilterHeadingSynchronizationDefaultsToEnabledForRootAndOperatorProfiles() {
        ChatPreferences root = new ChatPreferences(directory.resolve("preferences.xml"));
        ChatPreferences operator = new ChatPreferences(directory.resolve("profiles/OP2/preferences.xml"));

        assertTrue(root.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertTrue(root.isQtfFilterDirectionAsAntennaHeadingEnabled());
        assertTrue(operator.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertTrue(operator.isQtfFilterDirectionAsAntennaHeadingEnabled());
    }

    @Test
    void primaryRotatorOnlySuspendsTheStoredManualHeadingPreference() {
        ChatPreferences preferences = new ChatPreferences(directory.resolve("preferences.xml"));

        preferences.setStn_pstRotatorEnabled(true);

        assertTrue(preferences.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertFalse(preferences.isQtfFilterDirectionAsAntennaHeadingEnabled());

        preferences.setStn_pstRotatorEnabled(false);

        assertTrue(preferences.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertTrue(preferences.isQtfFilterDirectionAsAntennaHeadingEnabled());
    }

    @Test
    void versionEightProfilesMigrateToTheEnabledDefault() throws IOException {
        Path file = directory.resolve("preferences.xml");
        Files.writeString(file, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>8</configVersion>
                    <station>
                        <stn_pstRotatorEnabled>false</stn_pstRotatorEnabled>
                        <stn_useQtfFilterDirectionAsAntennaHeading>false</stn_useQtfFilterDirectionAsAntennaHeading>
                    </station>
                </praktiKST>
                """);

        ChatPreferences migrated = read(file);

        assertTrue(migrated.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertTrue(migrated.isQtfFilterDirectionAsAntennaHeadingEnabled());

        assertTrue(migrated.writePreferencesToXmlFile());
        String migratedXml = Files.readString(file);
        assertTrue(migratedXml.contains("<configVersion>9</configVersion>"));
        assertTrue(migratedXml.contains(
                "<stn_qtfFilterDirectionAsAntennaHeadingOptedOut>false"));
        assertTrue(read(file).isQtfFilterDirectionAsAntennaHeadingEnabled());
    }

    @Test
    void explicitOptOutSurvivesRestartAndPrimaryRotatorChanges() throws IOException {
        Path file = directory.resolve("preferences.xml");
        ChatPreferences preferences = new ChatPreferences(file);
        preferences.setStn_useQtfFilterDirectionAsAntennaHeading(false);
        assertTrue(preferences.writePreferencesToXmlFile());

        ChatPreferences restored = read(file);
        assertFalse(restored.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertFalse(restored.isQtfFilterDirectionAsAntennaHeadingEnabled());
        assertTrue(Files.readString(file).contains(
                "<stn_qtfFilterDirectionAsAntennaHeadingOptedOut>true"));

        restored.setStn_pstRotatorEnabled(true);
        restored.setStn_pstRotatorEnabled(false);

        assertFalse(restored.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertFalse(restored.isQtfFilterDirectionAsAntennaHeadingEnabled());
    }

    @Test
    void profilesKeepIndependentManualHeadingDecisions() {
        Path rootFile = directory.resolve("preferences.xml");
        Path operatorFile = directory.resolve("profiles/OP2/preferences.xml");
        ChatPreferences root = new ChatPreferences(rootFile);
        root.setStn_useQtfFilterDirectionAsAntennaHeading(false);
        assertTrue(root.writePreferencesToXmlFile());

        ChatPreferences operator = new ChatPreferences(operatorFile);
        assertTrue(operator.writePreferencesToXmlFile());

        assertFalse(read(rootFile).isStn_useQtfFilterDirectionAsAntennaHeading());
        assertTrue(read(operatorFile).isStn_useQtfFilterDirectionAsAntennaHeading());
    }

    @Test
    void laterRotorHeadingReplacesTheLoadedStartValue() {
        Path file = directory.resolve("preferences.xml");
        ChatPreferences written = new ChatPreferences(file);
        written.setStn_qtfDefault(188.0);
        written.setStn_pstRotatorEnabled(true);
        assertTrue(written.writePreferencesToXmlFile());

        ChatPreferences restored = read(file);
        restored.getActualQTF().set(271.25);

        assertEquals(271.25, restored.getActualQTF().get(), 0.0001);
        assertEquals(188.0, restored.getStn_qtfDefault(), 0.0001,
                "a runtime heading changed the persisted start direction");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-0.1", "360.1"})
    void invalidStoredQtfDefaultsAreIgnored(String invalidValue) throws IOException {
        Path file = directory.resolve("preferences.xml");
        ChatPreferences written = new ChatPreferences(file);
        written.setStn_qtfDefault(247.5);
        assertTrue(written.writePreferencesToXmlFile());
        Files.writeString(file, Files.readString(file).replace(
                "<stn_qtfDefault>247.5</stn_qtfDefault>",
                "<stn_qtfDefault>" + invalidValue + "</stn_qtfDefault>"));

        ChatPreferences restored = read(file);

        assertEquals(135.0, restored.getStn_qtfDefault(), 0.0001);
        assertEquals(135.0, restored.getActualQTF().get(), 0.0001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "-0.1", "360.1"})
    void invalidQtfDefaultsCannotReplaceAValidValue(String invalidValue) {
        ChatPreferences preferences = new ChatPreferences(directory.resolve("preferences.xml"));
        preferences.setStn_qtfDefault(247.5);

        preferences.setStn_qtfDefault(Double.parseDouble(invalidValue));

        assertEquals(247.5, preferences.getStn_qtfDefault(), 0.0001);
    }

    private void writeQtfDefault(Path file, double qtf) {
        ChatPreferences preferences = new ChatPreferences(file);
        preferences.setStn_qtfDefault(qtf);
        assertTrue(preferences.writePreferencesToXmlFile());
    }

    private ChatPreferences read(Path file) {
        ChatPreferences preferences = new ChatPreferences(file);
        assertTrue(preferences.readPreferencesFromXmlFile());
        return preferences;
    }
}
