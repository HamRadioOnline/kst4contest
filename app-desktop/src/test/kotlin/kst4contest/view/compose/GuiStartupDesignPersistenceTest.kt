package kst4contest.view.compose

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.tabs.GuiOptionsTabState
import kst4contest.view.i18n.LanguageStore
import kst4contest.view.i18n.SYSTEM_LANGUAGE
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Locale

class GuiStartupDesignPersistenceTest(@param:TempDir private val tempDir: Path) {

    @Test
    fun `root profile keeps evening startup design across save layout autosave and reload`() {
        assertStartupDesignSurvives(tempDir.resolve("preferences.xml"))
    }

    @Test
    fun `named profile keeps evening startup design across save layout autosave and reload`() {
        assertStartupDesignSurvives(tempDir.resolve("profiles/CONTEST/preferences.xml"))
    }

    private fun assertStartupDesignSurvives(preferencesFile: Path) {
        val preferences = ChatPreferences(preferencesFile)
        val draft = SettingsDraft()
        val notices = SettingsNotices()
        val state = GuiOptionsTabState(preferences, draft, languageStore())

        state.gUI_darkModeActiveByDefault = true
        applyAndSave(draft, notices) {
            preferences.writePreferencesToXmlFile()
                .takeIf { it }
                ?.let { preferences.storeAndRestorePreferencesFileName }
        }

        assertTrue(preferences.isGUI_darkModeActiveByDefault)

        // The menu switch is session-only and must not alter the next startup.
        preferences.isGUI_darkModeActive = false
        preferences.getGUIsettingsStageSceneSizeHW()[0] = 777.0
        assertTrue(preferences.writeLayoutPreferencesToXmlFile())

        val restored = ChatPreferences(preferencesFile)
        assertTrue(restored.readPreferencesFromXmlFile())
        assertTrue(restored.isGUI_darkModeActiveByDefault)
        assertFalse(restored.isGUI_darkModeActive)
    }

    private fun languageStore() = LanguageStore(
        storedLanguage = { SYSTEM_LANGUAGE },
        storeLanguage = { },
        systemDefault = Locale.UK,
    )
}
