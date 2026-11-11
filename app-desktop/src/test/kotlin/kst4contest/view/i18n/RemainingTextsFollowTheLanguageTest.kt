package kst4contest.view.i18n

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsContent
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.SettingsNotices
import kst4contest.view.compose.SettingsTab
import kst4contest.view.compose.StationColumns
import kst4contest.view.compose.tabs.BeaconTabState
import kst4contest.view.compose.tabs.GuiOptionsTab
import kst4contest.view.compose.tabs.GuiOptionsTabState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Locale

/**
 * The texts that stayed English after stage 9 now follow the language as well.
 *
 * One sample from each of the three ways a text reaches the operator: a label read inside a
 * composition, a column title built outside one, and a refusal produced by a tab state. All
 * three have to switch, and each goes through a different door.
 */
@OptIn(ExperimentalTestApi::class)
class RemainingTextsFollowTheLanguageTest {

    /* Never the no-argument ChatPreferences: it would touch the operator's real profile. */
    @TempDir
    lateinit var directory: Path

    @Test
    fun theMovedTextsAppearInGermanAfterTheSwitch() = runComposeUiTest {
        var stored = "en"
        val store = LanguageStore({ stored }, { stored = it }, Locale.UK)
        val prefs = ChatPreferences(directory.resolve("preferences.xml"))
        val state = GuiOptionsTabState(prefs, SettingsDraft(), store)
        val beacon = BeaconTabState(
            prefs,
            SettingsDraft(),
            validateBeaconTemplate = {},
            restartBeaconTimer = {},
            mainCategoryName = "Microwave",
            secondCategoryName = "144/432 MHz",
        )
        val score = StationColumns.all(
            tropoOf = { "" },
            priorityScoreOf = { 0.0 },
            formatBand = { _, _, _ -> "" },
        ).single { it.id == "score" }

        setContent {
            CompositionLocalProvider(LocalStrings provides store.strings) {
                GuiOptionsTab(state)
            }
        }
        waitForIdle()

        onNodeWithText("Start in the evening design").assertExists()
        assertEquals("Score", score.title())

        try {
            runOnIdle { store.use("de") }
            waitForIdle()

            onNodeWithText("Im Abenddesign starten").assertExists()
            onNodeWithText("Kein Filter").assertExists()
            onNodeWithText("Ein gearbeitetes Großfeld markieren").assertExists()

            assertEquals("Wertung", score.title(), "the column title stayed in the old language")
            assertEquals(
                "Einen ganzzahligen Bakenabstand von mindestens 5 Minuten eingeben.",
                beacon.commitBeaconInterval("0"),
                "the refusal from the tab state stayed in the old language",
            )
        } finally {
            // use() also sets the JVM-wide CurrentStrings; leave the next test the base language.
            runOnIdle { store.use("en") }
        }
    }

    /**
     * The settings window's notice: its title is read in the composition, its message is
     * produced outside one by the save action -- the same split as above, one window further.
     */
    @Test
    fun theSettingsNoticeFollowsTheLanguage() = runComposeUiTest {
        var stored = "en"
        val store = LanguageStore({ stored }, { stored = it }, Locale.UK)
        val notices = SettingsNotices()

        setContent {
            CompositionLocalProvider(LocalStrings provides store.strings) {
                SettingsContent(
                    tabs = listOf(SettingsTab("Tab") {}),
                    notices = notices,
                    buttons = {},
                    close = {},
                )
            }
        }

        try {
            runOnIdle {
                store.use("de")
                notices.problem(CurrentStrings.get().settingsWriteFailed)
            }
            waitForIdle()

            onNodeWithText("Einstellungen").assertExists()
            onNodeWithText(
                "Die Konfigurationsdatei konnte nicht geschrieben werden. " +
                    "Die Einstellungen bleiben für diese Sitzung aktiv.",
            ).assertExists()
        } finally {
            runOnIdle { store.use("en") }
        }
    }
}
