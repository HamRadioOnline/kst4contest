package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.Form
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.compose.SettingsQuestion
import kst4contest.view.compose.runOnce
import kst4contest.view.compose.SettingsContent
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.SettingsNotice
import kst4contest.view.compose.SettingsNotices
import kst4contest.view.compose.SettingsTab
import kst4contest.view.compose.applyAndSave
import kst4contest.view.compose.SettingsWindow
import kst4contest.view.compose.applyChecked
import kst4contest.view.compose.awaitGone
import kst4contest.view.compose.awaitNode
import kst4contest.view.compose.inDialog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The settings window around its tabs: edits wait in the draft, Apply/Close applies them,
 * Save applies before it writes, and the X asks about what is still waiting.
 *
 * Drawn with the Beacon tab because it has all three kinds of control that matter here: a
 * checkbox, a field that commits on Enter or focus loss, and an effect (the timer restart).
 * The button bar is a stand-in: the real one needs a ChatController, which opens a database;
 * it calls the same [applyChecked] and [applyAndSave] this one does.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsApplyScreenTest {

    @TempDir
    lateinit var directory: Path

    private inner class Window(refuse: Boolean = false, timerFails: Boolean = false) {
        val prefs = ChatPreferences(directory.resolve("preferences.xml")).apply {
            bcn_beaconIntervalInMinutesMainCat = 5
            isBcn_beaconsEnabledMainCat = false
        }
        val draft = SettingsDraft()
        val notices = SettingsNotices()
        val question = SettingsQuestion()
        var closed = false
        var restarts = 0

        /** The title bar's X. */
        fun x() = question.settleClose(draft, notices)

        /** A profile switch or the exit, as SettingsWindow.settleThen hands it on. */
        fun switchOrExit(action: () -> Unit) = question.settle(draft, notices, then = action, nothingPending = action)
        val state = BeaconTabState(
            prefs,
            draft,
            validateBeaconTemplate = { if (refuse) throw IllegalArgumentException("no variable %foo%") },
            restartBeaconTimer = {
                restarts++
                if (timerFails) throw IllegalStateException("timer gone")
            },
            mainCategoryName = "Microwave",
            secondCategoryName = "144/432 MHz",
        )
        val station = StationTabState(prefs, draft, { }, { })
        val airscout = AirscoutTabState(prefs, draft)
        val tabs = listOf(
            SettingsTab("Beacon") {
                androidx.compose.foundation.layout.Column {
                    BeaconTab(state, notices::problem)
                    StationTab(station)
                }
            },
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.show(w: Window, tabs: List<SettingsTab> = w.tabs) {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SettingsContent(
                    tabs = tabs,
                    notices = w.notices,
                    buttons = { close ->
                        Form.button(APPLY_AND_CLOSE) { if (applyChecked(w.draft, w.notices)) close() }
                    },
                    close = { w.closed = true },
                    draft = w.draft,
                    question = w.question,
                )
            }
        }
        waitForIdle()
    }

    @Test
    fun beaconEnableTakesEffectImmediatelyWhileTheIntervalWaitsForApply() = runComposeUiTest {
        val w = Window()
        show(w)

        toggle("Send the beacon in this category")
        typeAndCommit("Interval in whole minutes", "9")

        assertTrue(w.prefs.isBcn_beaconsEnabledMainCat, "the runtime switch did not take effect immediately")
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(0, w.restarts, "the timer restarted before Apply")

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertTrue(w.prefs.isBcn_beaconsEnabledMainCat)
        assertEquals(9, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(1, w.restarts)
        assertTrue(w.closed)
    }

    /*
     * A click on a button leaves the focus in the text field, so the field has not committed
     * when the button runs. Apply commits it first; otherwise the entry would reach the draft
     * only when the closing window removed the field -- after the apply, into nothing.
     */
    @Test
    fun applyAndCloseTakesTheEntryStillInTheFocusedField() = runComposeUiTest {
        val w = Window()
        show(w)

        val field = fieldBeside("Interval in whole minutes")
        field.performScrollTo().performClick()
        waitForIdle()
        field.performTextReplacement("9")
        waitForIdle()

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertEquals(9, w.prefs.bcn_beaconIntervalInMinutesMainCat, "the typed entry was lost")
        assertEquals(1, w.restarts)
        assertTrue(w.closed)
    }

    /* A refused entry stops the button, so the reason is not closed away with the window. */
    @Test
    fun aRefusedEntryKeepsTheWindowOpenAndShowsWhy() = runComposeUiTest {
        val w = Window(refuse = true)
        show(w)

        val field = fieldBeside("Beacon message")
        field.performScrollTo().performClick()
        waitForIdle()
        field.performTextReplacement("cq %foo%")
        waitForIdle()

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertFalse(w.closed, "the window closed over a refused entry")
        assertTrue(w.notices.current is SettingsNotice.Problem)
    }

    /*
     * A refusal stops the apply before anything goes into effect: the valid edit beside the
     * refused one is still pending, so the operator can still discard everything.
     */
    @Test
    fun aRefusalAppliesNothingSoDiscardIsStillPossible() = runComposeUiTest {
        val w = Window()
        show(w)
        typeAndCommit("Interval in whole minutes", "9")
        type("Server port", "23x")

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertFalse(w.closed)
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat, "the valid edit went into effect despite the refusal")
        assertTrue(w.draft.hasPendingChanges)
        runOnIdle { w.notices.dismiss() }

        runOnIdle { w.draft.discard() }
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat)
    }

    /*
     * A number field showing text that does not parse holds the last valid value in the draft.
     * Applying that value would put something into effect the operator no longer sees.
     */
    @Test
    fun anInvalidNumberRefusesTheApplyAndNamesTheField() = runComposeUiTest {
        val w = Window()
        val storedPort = w.prefs.stn_on4kstServersPort
        show(w)

        type("Server port", "23005")
        type("Server port", "23x")
        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertFalse(w.closed, "the window closed with the old port applied")
        assertEquals(storedPort, w.prefs.stn_on4kstServersPort)
        val notice = w.notices.current
        assertTrue(notice is SettingsNotice.Problem && notice.message.contains("Server port") &&
            notice.message.contains("23x"), "the notice does not name the field: $notice")
    }

    /* The same for the guarded fields of the external interfaces. */
    @Test
    fun anInvalidGuardedEntryRefusesTheApplyAndNamesTheField() = runComposeUiTest {
        val w = Window()
        show(w, listOf(SettingsTab("Airscout") { AirscoutTab(w.airscout) }))

        type(AIRSCOUT_PORT, "0")
        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertFalse(w.closed)
        val notice = w.notices.current
        assertTrue(notice is SettingsNotice.Problem && notice.message.contains(AIRSCOUT_PORT),
            "the notice does not name the field: $notice")
    }

    /* A failing effect is reported, the window stays open, and the other changes are in. */
    @Test
    fun aFailingEffectIsReportedAndTheRestIsApplied() = runComposeUiTest {
        val w = Window(timerFails = true)
        show(w)
        toggle("Send the beacon in this category")
        typeAndCommit("Interval in whole minutes", "9")

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertFalse(w.closed, "the failure was closed away")
        assertTrue(w.prefs.isBcn_beaconsEnabledMainCat)
        assertEquals(9, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        val notice = w.notices.current
        assertTrue(notice is SettingsNotice.Problem && notice.message.contains("timer gone"), "$notice")
    }

    @Test
    fun theXClosesAtOnceWhenNothingIsPending() = runComposeUiTest {
        val w = Window()
        show(w)

        runOnIdle { w.x() }
        waitForIdle()

        assertTrue(w.closed)
    }

    @Test
    fun theXAsksAboutPendingEditsAndApplyAppliesThem() = runComposeUiTest {
        val w = Window()
        show(w)
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.x() }
        awaitNode(inDialog(PENDING_QUESTION))
        assertFalse(w.closed, "the window closed without asking")

        awaitNode(inDialog("Apply")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertEquals(9, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertTrue(w.closed)
    }

    @Test
    fun discardingOnTheXChangesNothing() = runComposeUiTest {
        val w = Window()
        show(w)
        toggle("Send the beacon in this category")
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.x() }
        awaitNode(inDialog("Discard")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertTrue(w.prefs.isBcn_beaconsEnabledMainCat, "the immediate runtime switch was unexpectedly reverted")
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(0, w.restarts)
        assertTrue(w.closed)
        assertFalse(w.draft.hasPendingChanges)
    }

    @Test
    fun goingBackKeepsTheWindowAndTheEdits() = runComposeUiTest {
        val w = Window()
        show(w)
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.x() }
        awaitNode(inDialog("Back to the settings")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertFalse(w.closed)
        assertTrue(w.draft.hasPendingChanges)
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        checkBeside("Send the beacon in this category").assertExists()
    }

    /* An entry typed just before the X counts as pending, although no Enter committed it. */
    @Test
    fun theXAsksAboutAnEntryStillInTheFocusedField() = runComposeUiTest {
        val w = Window()
        show(w)

        val field = fieldBeside("Interval in whole minutes")
        field.performScrollTo().performClick()
        waitForIdle()
        field.performTextReplacement("9")
        waitForIdle()

        runOnIdle { w.x() }
        awaitNode(inDialog(PENDING_QUESTION))

        assertFalse(w.closed)
    }

    /* A list row still being edited counts too: the lists commit a row on Enter or focus loss. */
    @Test
    fun theXAsksAboutARowStillBeingEdited() = runComposeUiTest {
        val prefs = ChatPreferences(directory.resolve("preferences.xml")).apply {
            lst_txtShortCutBtnList.setAll(listOf("MYQRG", "pse sked"))
        }
        val draft = SettingsDraft()
        val question = SettingsQuestion()
        val notices = SettingsNotices()
        var closed = false
        val state = ShortcutsTabState(prefs, draft, { }, { })
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SettingsContent(
                    tabs = listOf(SettingsTab("Shortcuts") { ShortcutsTab(state) { } }),
                    notices = notices,
                    buttons = { },
                    close = { closed = true },
                    draft = draft,
                    question = question,
                )
            }
        }
        waitForIdle()

        val row = onNode(hasSetTextAction() and hasText("pse sked"))
        row.performScrollTo().performClick()
        waitForIdle()
        row.performTextReplacement("pse sked 432")
        waitForIdle()

        runOnIdle { question.settleClose(draft, notices) }
        awaitNode(inDialog("Apply")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertEquals(listOf("MYQRG", "pse sked 432"), prefs.lst_txtShortCutBtnList.snapshot())
        assertTrue(closed)
    }

    /*
     * A profile switch or the exit asks the same question. Apply applies the edits to the
     * profile still in force and goes on; the window itself is left to the teardown.
     */
    @Test
    fun aSwitchOrExitAsksAndApplyGoesOnWithTheEditsApplied() = runComposeUiTest {
        val w = Window()
        var wentOn = false
        show(w)
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.switchOrExit { wentOn = true } }
        awaitNode(inDialog("Apply")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertEquals(9, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertTrue(wentOn)
        assertFalse(w.closed, "the window closed itself instead of leaving that to the teardown")
    }

    @Test
    fun goingBackCancelsTheSwitchOrExit() = runComposeUiTest {
        val w = Window()
        var wentOn = false
        show(w)
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.switchOrExit { wentOn = true } }
        awaitNode(inDialog("Back to the settings")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertFalse(wentOn, "the switch or exit went ahead after Back")
        assertTrue(w.draft.hasPendingChanges)
    }

    @Test
    fun discardingGoesOnWithNothingApplied() = runComposeUiTest {
        val w = Window()
        var wentOn = false
        show(w)
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.switchOrExit { wentOn = true } }
        awaitNode(inDialog("Discard")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertTrue(wentOn)
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat)
    }

    @Test
    fun aRefusalAtTheQuestionStopsTheSwitchOrExit() = runComposeUiTest {
        val w = Window()
        var wentOn = false
        show(w)
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.switchOrExit { wentOn = true } }
        awaitNode(inDialog(PENDING_QUESTION))
        type("Server port", "23x")
        awaitNode(inDialog("Apply")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertFalse(wentOn, "went on over a refused entry")
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat, "applied despite the refusal")
    }

    @Test
    fun withNothingPendingTheSwitchOrExitGoesOnAtOnce() = runComposeUiTest {
        val w = Window()
        var wentOn = false
        show(w)

        runOnIdle { w.switchOrExit { wentOn = true } }
        waitForIdle()

        assertTrue(wentOn)
        assertFalse(w.closed)
    }

    /*
     * An X while the exit or switch question is open must not replace its continuation:
     * Apply would then only close the window, and the exit would be dropped without a word.
     */
    @Test
    fun theXDoesNotReplaceAnOpenSwitchOrExitQuestion() = runComposeUiTest {
        val w = Window()
        var wentOn = false
        show(w)
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.switchOrExit { wentOn = true } }
        awaitNode(inDialog(PENDING_QUESTION))
        runOnIdle { w.x() }
        awaitNode(inDialog("Apply")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertTrue(wentOn, "the exit or switch was dropped")
        assertFalse(w.closed)
    }

    /*
     * Invalid text in a field refuses Apply, but must not trap the operator: the question is
     * shown anyway, with the reason, and Discard closes.
     */
    @Test
    fun anInvalidFieldStillLetsTheXDiscard() = runComposeUiTest {
        val w = Window()
        show(w)
        typeAndCommit("Interval in whole minutes", "9")
        type("Server port", "23x")

        runOnIdle { w.x() }
        awaitNode(inDialog(PENDING_QUESTION))
        awaitNode(inDialog("Apply")).assertIsNotEnabled()
        onNode(hasText("23x", substring = true) and hasAnyAncestor(isDialog())).assertExists()

        awaitNode(inDialog("Discard")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertTrue(w.closed)
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat, "something was applied")
    }

    /* The same for a switch or the exit, even when only the invalid field is unsettled. */
    @Test
    fun anInvalidFieldStillLetsASwitchOrExitDiscard() = runComposeUiTest {
        val w = Window()
        var wentOn = false
        show(w)
        type("Server port", "23x")

        runOnIdle { w.switchOrExit { wentOn = true } }
        assertFalse(wentOn, "went on past a field that refuses its text")
        awaitNode(inDialog("Discard")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertTrue(wentOn)
    }

    @Test
    fun aContinuationRunsOnlyOnce() {
        var runs = 0
        val once = runOnce { runs++ }

        once.run()
        once.run()

        assertEquals(1, runs)
    }

    /* Without a settings window there is nothing to ask: the action runs at once, synchronously. */
    @Test
    fun settleThenRunsAtOnceWithoutAnOpenWindow() {
        var ran = false
        SettingsWindow.settleThen { ran = true }
        assertTrue(ran)
    }

    /* A failed effect does not keep the file from being written; its setting was stored. */
    @Test
    fun saveStillWritesWhenOnlyAnEffectFailed() {
        val prefs = ChatPreferences(directory.resolve("preferences.xml"))
        prefs.bcn_beaconIntervalInMinutesMainCat = 5
        val draft = SettingsDraft()
        val notices = SettingsNotices()
        val state = BeaconTabState(prefs, draft, { }, { throw IllegalStateException("timer gone") }, "Microwave", "144/432 MHz")
        var saved = false

        state.commitBeaconInterval("9")
        applyAndSave(draft, notices) {
            saved = true
            "preferences.xml"
        }

        assertTrue(saved)
        val notice = notices.current
        assertTrue(notice is SettingsNotice.Problem && notice.message.contains("timer gone") &&
            notice.message.contains("preferences.xml"), "$notice")
    }

    /* Save applies first, so the file holds what the window shows; the window stays open. */
    @Test
    fun saveAppliesBeforeItWrites() {
        val prefs = ChatPreferences(directory.resolve("preferences.xml"))
        prefs.bcn_beaconIntervalInMinutesMainCat = 5
        val draft = SettingsDraft()
        val notices = SettingsNotices()
        val state = BeaconTabState(prefs, draft, { }, { }, "Microwave", "144/432 MHz")
        var intervalWhenSaved = -1

        state.commitBeaconInterval("9")
        applyAndSave(draft, notices) {
            intervalWhenSaved = prefs.bcn_beaconIntervalInMinutesMainCat
            "preferences.xml"
        }

        assertEquals(9, intervalWhenSaved, "the file was written before the draft was applied")
        assertFalse(draft.hasPendingChanges)
        assertTrue(notices.current is SettingsNotice.Info)
    }

    private companion object {
        const val APPLY_AND_CLOSE = "Apply/Close prefs"
        const val AIRSCOUT_PORT = "AirScout UDP port [9872] — reconnect after changing:"
        const val PENDING_QUESTION =
            "Some settings have been changed but not applied yet. Apply them first?"
    }
}
