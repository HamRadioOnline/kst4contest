package kst4contest.view.compose.wizard

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.i18n.LanguageStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.Locale

/**
 * The drawn wizard, clicked the way an operator clicks it. The rules live in
 * SetupWizardState and are tested there; this holds the wiring -- that a button reaches the
 * state, and that a button which must not work does not.
 */
@OptIn(ExperimentalTestApi::class)
class SetupWizardContentScreenTest {

    private fun configuredProfile() =
        ChatPreferences(Path.of("setup-wizard-screen-test-unused.xml")).apply {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginNameMainCat("Philipp")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            // Stated, not left to the compiled-in default: with a second chat and no second
            // category the summary could not be saved.
            setLoginToSecondChatEnabled(false)
        }

    private class Recorded {
        var later = 0
        val saves = mutableListOf<Boolean>()
    }

    private fun ComposeUiTest.show(state: SetupWizardState, saveFailed: Boolean = false): Recorded {
        val recorded = Recorded()
        // English, whatever language an earlier test in this JVM left behind.
        val english = LanguageStore({ "en" }, {}, Locale.ENGLISH)
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SetupWizardContent(
                    state = state,
                    languageStore = english,
                    saveFailed = saveFailed,
                    onLater = { recorded.later++ },
                    onSave = { recorded.saves += it },
                )
            }
        }
        return recorded
    }

    @Test
    fun nextDoesNothingUntilBothSourcesAreChosen() = runComposeUiTest {
        val state = SetupWizardState(configuredProfile())
        show(state)

        onNodeWithText("Next").performClick()
        assertEquals(WizardPage.ENTRY, state.page, "Next worked without a choice")

        onNodeWithText("Leave the login unchanged (DL0TEST)").performClick()
        onNodeWithText("Leave the station unchanged (locator JO50AA)").performClick()
        assertEquals(WizardSource.KEEP, state.loginSource)
        assertEquals(WizardSource.KEEP, state.stationSource)

        onNodeWithText("Next").performClick()
        assertEquals(WizardPage.PERSONAL, state.page)
    }

    @Test
    fun laterIsReportedAndNothingElse() = runComposeUiTest {
        val recorded = show(SetupWizardState(configuredProfile()))

        onNodeWithText("Later").performClick()

        assertEquals(1, recorded.later)
        assertTrue(recorded.saves.isEmpty())
    }

    @Test
    fun theSummaryOffersBothWaysOfSaving() = runComposeUiTest {
        val state = SetupWizardState(configuredProfile())
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        val recorded = show(state)

        onNodeWithText("Save").performClick()
        onNodeWithText("Save and connect").performClick()

        assertEquals(listOf(false, true), recorded.saves)
    }

    @Test
    fun aFailedSaveIsToldToTheOperator() = runComposeUiTest {
        val state = SetupWizardState(configuredProfile())
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        show(state, saveFailed = true)

        onNodeWithText("The settings could not be written. Nothing was changed.").assertExists()
    }

    @Test
    fun theSummaryShowsTheChosenLanguage() = runComposeUiTest {
        val state = SetupWizardState(configuredProfile().apply { setGuiOptions_language("de") })
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        show(state)

        onNodeWithText("DE").assertExists()
    }

    /** A first installation: the template's credentials, so the wizard starts from scratch. */
    private fun firstInstallation() =
        ChatPreferences(Path.of("setup-wizard-screen-test-unused.xml")).apply {
            setStn_loginCallSign("DO5AMF")
            setStn_loginPassword("changeMe")
            setLogsynch_wintestNetworkListenerEnabled(false)
            setLogsynch_ucxUDPWkdCallListenerEnabled(true)
            setLogsynch_fileBasedWkdCallInterpreterEnabled(false)
            // Every other switch stated too, so the summary's wording does not hang on a
            // compiled-in default.
            setTrxSynch_ucxLogUDPListenerEnabled(false)
            setLogsynch_wintestQrgSyncEnabled(false)
            setNotify_dxClusterServerEnabled(false)
            setStn_pstRotatorEnabled(false)
            setAirScout_asUDPListenerEnabled(true)
            setLoginToSecondChatEnabled(false)
        }

    private fun SetupWizardState.walkToTheLogPage() {
        name = "Philipp"
        next()
        callsign = "DL0TEST"
        password = "secret"
        next()
        locator = "JO50AA"
        setBand(WizardBand.B144, true)
        next()
        next()
    }

    @Test
    fun choosingALogProgramOnTheDrawnPageReachesTheState() = runComposeUiTest {
        val state = SetupWizardState(firstInstallation())
        state.walkToTheLogPage()
        assertEquals(WizardPage.LOG, state.page)
        show(state)

        onNodeWithText("Win-Test").performClick()

        assertEquals(WizardLogProgram.WINTEST, state.integrations.logProgram)
        onNodeWithText("Take my own frequency from the log program").assertExists()
    }

    @Test
    fun nextRefusesAnUnreadableUdpPortAndAcceptsItsCorrection() = runComposeUiTest {
        val state = SetupWizardState(firstInstallation())
        state.walkToTheLogPage()
        state.integrations.chooseLogProgram(WizardLogProgram.UDP)
        val originalPort = state.integrations.udpPort
        show(state)

        val portField = onNode(hasSetTextAction())
        val refusal = onNodeWithText(
            "Shared UDP port for QSO and TRX messages [default 12060]:: \"abc\" is not a valid entry.",
        )
        portField.performTextReplacement("abc")
        onNodeWithText("Next").performClick()

        assertEquals(originalPort, state.integrations.udpPort)
        assertEquals(WizardPage.LOG, state.page)
        refusal.assertExists()

        portField.performTextReplacement("12061")
        refusal.assertExists()
        onNodeWithText("Next").performClick()

        assertEquals(WizardPage.GEAR, state.page)
        assertEquals(12061, state.integrations.udpPort)
        refusal.assertDoesNotExist()
    }

    @Test
    fun finishRefusesAnUnreadableUdpPort() = runComposeUiTest {
        val state = SetupWizardState(firstInstallation())
        state.walkToTheLogPage()
        state.integrations.chooseLogProgram(WizardLogProgram.UDP)
        show(state)

        onNode(hasSetTextAction()).performTextReplacement("abc")
        onNodeWithText("Finish").performClick()

        assertEquals(WizardPage.LOG, state.page)
    }

    @Test
    fun theSummaryNamesWhatANewStationSwitchesOff() = runComposeUiTest {
        val state = SetupWizardState(firstInstallation())
        state.walkToTheLogPage()
        state.finish()
        show(state)

        onNodeWithText("Will be switched off: network log synchronisation, AirScout.").assertExists()
    }
}
