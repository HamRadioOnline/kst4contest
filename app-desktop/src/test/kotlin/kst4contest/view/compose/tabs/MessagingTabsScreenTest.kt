package kst4contest.view.compose.tabs

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.ImmediateDraft
import kst4contest.view.compose.SettingsDraft
import kst4contest.observe.SimpleRoster
import kst4contest.view.i18n.LanguageStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kst4contest.view.compose.awaitNode
import kst4contest.view.compose.awaitGone
import kst4contest.view.compose.inDialog
import kst4contest.view.compose.inPopup
import androidx.compose.ui.test.onNodeWithTag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Locale

/**
 * The tabs that decide what this station says and hears, drawn: Message handling, GUI,
 * Beacon and Notification (#107).
 *
 * Each is composed headless, shows its sections and key controls, and an edit made through
 * the drawn control reaches the preferences. The message-handling tab carries the
 * debug-to-file switch that the Compose redraw had dropped and `c8ce0e98` gave back.
 */
@OptIn(ExperimentalTestApi::class)
class MessagingTabsScreenTest {

    /*
     * Never the no-argument ChatPreferences: it resolves the operator's real profile and
     * seeds preferences.xml there when it is missing. A file in a temporary directory is
     * never read or written by these tests, it only gives the preferences a home.
     */
    @TempDir
    lateinit var directory: Path

    private fun preferences(): ChatPreferences = ChatPreferences(directory.resolve("preferences.xml"))

    // ------------------------------------------------------------------ Message handling

    @Test
    fun messageHandlingWritesBothCategoriesAndAppliesTheFileRecording() = runComposeUiTest {
        val prefs = preferences().apply {
            isMessageHandling_autoAnswerEnabled = false
            isMessageHandling_debugModeToFileEnabled = false
        }
        val recording = mutableListOf<Boolean>()
        val state = MessageHandlingTabState(prefs, ImmediateDraft()) { recording += it }
        showTab { MessageHandlingTab(state) }

        onNodeWithText("Automatic answer to private messages").assertExists()
        onNodeWithText("Diagnostics").assertExists()

        toggle("Answer private messages automatically")
        assertTrue(prefs.isMessageHandling_autoAnswerEnabled)
        assertTrue(prefs.isMessageHandling_autoAnswerEnabledSecondCat, "the second category was left behind")

        type("Answer text", "qrv 432.200")
        assertEquals("qrv 432.200", prefs.messageHandling_autoAnswerTextMainCat)
        assertEquals("qrv 432.200", prefs.messageHandling_autoAnswerTextSecondCat)

        // The c8ce0e98 control: it writes the preference and asks the application to act.
        toggle("Record the whole session to file")
        assertTrue(prefs.isMessageHandling_debugModeToFileEnabled)
        assertEquals(listOf(true), recording)
    }

    // ------------------------------------------------------------------ GUI

    @Test
    fun guiOptionsWritesTheStartupChoicesAndTheLanguage() = runComposeUiTest {
        val prefs = preferences().apply { isGUI_darkModeActiveByDefault = false }
        var storedLanguage = "en"
        val languages = LanguageStore({ storedLanguage }, { storedLanguage = it }, Locale.UK)
        val state = GuiOptionsTabState(prefs, ImmediateDraft(), languages)
        showTab { GuiOptionsTab(state) }

        onNodeWithText("Startup design").assertExists()
        onNodeWithText("Message filters active at startup").assertExists()
        checkBeside("Start in the evening design").assertIsOff()

        toggle("Start in the evening design")
        assertTrue(prefs.isGUI_darkModeActiveByDefault)

        val before = prefs.isGuiOptions_defaultFilterPublicMsgs
        toggle("Public messages")
        assertEquals(!before, prefs.isGuiOptions_defaultFilterPublicMsgs)

        try {
            onNodeWithText("EN").performScrollTo().performClick()
            waitForIdle()
            awaitNode(inPopup("DE")).performClick()
            waitForIdle()

            assertEquals("de", storedLanguage, "the picked language was not stored")
            assertEquals("de", languages.language)
        } finally {
            // use() also sets the JVM-wide CurrentStrings; leave the next test the base language.
            runOnIdle { languages.use("en") }
        }
    }

    // ------------------------------------------------------------------ Beacon

    /*
     * A draft that applies at once by default, so the tests below keep pinning the field's
     * commit rules (#128) against the stored value; the drafted behaviour is pinned by the
     * tests that pass a real draft.
     */
    private class Beacon(
        val prefs: ChatPreferences,
        val refuse: Boolean = false,
        val draft: SettingsDraft = ImmediateDraft(),
    ) {
        var restarts = 0
        val refusals = mutableListOf<String>()
        val state = BeaconTabState(
            prefs,
            draft,
            validateBeaconTemplate = { if (refuse) throw IllegalArgumentException("no variable %foo%") },
            restartBeaconTimer = { restarts++ },
            mainCategoryName = "Microwave",
            secondCategoryName = "144/432 MHz",
        )
    }

    @Test
    fun beaconShowsOneSectionPerCategoryAndCommitsOnEnter() = runComposeUiTest {
        val s = Beacon(preferences())
        showTab { BeaconTab(s.state) { s.refusals += it } }

        onNodeWithText("CQ beacon for Microwave").assertExists()
        onNodeWithText("CQ beacon for 144/432 MHz").assertExists()
        onNodeWithText("Shared interval").assertExists()

        typeAndCommit("Beacon message", "cq de dn9apw %MYQRG%", occurrence = 1)
        assertEquals("cq de dn9apw %MYQRG%", s.prefs.bcn_beaconTextSecondCat)

        typeAndCommit("Interval in whole minutes", "7")
        assertEquals(7, s.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertTrue(s.refusals.isEmpty(), "a valid entry was refused: ${s.refusals}")
    }

    /** A refused template is reported, not stored, and the field goes back to what is stored. */
    @Test
    fun beaconRefusesAnInvalidTemplateAndPutsTheFieldBack() = runComposeUiTest {
        val s = Beacon(preferences(), refuse = true)
        s.prefs.bcn_beaconTextMainCat = "cq cq"
        showTab { BeaconTab(s.state) { s.refusals += it } }

        typeAndCommit("Beacon message", "cq %foo%")

        assertEquals("cq cq", s.prefs.bcn_beaconTextMainCat)
        assertEquals(1, s.refusals.size)
        fieldBeside("Beacon message").assertTextEquals("cq cq")
    }

    /*
     * Opening the tab used to commit all three fields once, because a committed field's
     * focus callback fires with "not focused" when it is first attached: the beacon timer
     * was restarted and a stored template the validator refuses was reported, although
     * the operator had done nothing but look.
     */
    @Test
    fun openingTheBeaconTabNeitherRestartsTheTimerNorValidates() = runComposeUiTest {
        val s = Beacon(preferences(), refuse = true)
        s.prefs.bcn_beaconTextMainCat = "cq %foo%"
        s.prefs.bcn_beaconIntervalInMinutesMainCat = 5
        showTab { BeaconTab(s.state) { s.refusals += it } }

        assertEquals(0, s.restarts, "opening the tab restarted the beacon timer")
        assertTrue(s.refusals.isEmpty(), "opening the tab reported: ${s.refusals}")
        assertEquals("cq %foo%", s.prefs.bcn_beaconTextMainCat)
    }

    @Test
    fun anExternalBeaconToggleUpdatesTheOpenSettingsTab() = runComposeUiTest {
        val s = Beacon(preferences())
        s.prefs.isBcn_beaconsEnabledMainCat = false
        showTab { BeaconTab(s.state) { s.refusals += it } }
        checkBeside("Send the beacon in this category").assertIsOff()

        runOnIdle { s.prefs.isBcn_beaconsEnabledMainCat = true }
        waitForIdle()

        checkBeside("Send the beacon in this category").assertIsOn()
    }

    /*
     * The tab reads the interval from a plain preference getter, so nothing recomposes it
     * after Enter commits. Compared with that stale value, leaving the field afterwards
     * looked like a second edit and restarted the timer again.
     */
    @Test
    fun enterAndThenLeavingTheFieldRestartsTheTimerOnce() = runComposeUiTest {
        val s = Beacon(preferences())
        s.prefs.bcn_beaconIntervalInMinutesMainCat = 5
        showTab { BeaconTab(s.state) { s.refusals += it } }

        typeAndCommit("Interval in whole minutes", "9")
        fieldBeside("Beacon message").performScrollTo().performClick()
        waitForIdle()

        assertEquals(9, s.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(1, s.restarts, "one edit restarted the beacon timer more than once")
    }

    /*
     * Pins what happens to a typed but uncommitted entry when the field leaves the screen
     * while it has the focus -- the settings window switching to another tab, or closing.
     * Compose clears the focus of a removed focused node and reports that to the field as a
     * focus loss, so the entry is committed, exactly once. That was so before the field
     * learned to tell a real focus loss from its first attachment, and must stay so: an
     * operator who typed a value and switched tabs expects it to count.
     */
    @Test
    fun aFieldRemovedWhileTypingCommitsTheEntryOnce() = runComposeUiTest {
        val s = Beacon(preferences())
        s.prefs.bcn_beaconIntervalInMinutesMainCat = 5
        val visible = mutableStateOf(true)
        showTab { if (visible.value) BeaconTab(s.state) { s.refusals += it } }

        val field = fieldBeside("Interval in whole minutes")
        field.performScrollTo().performClick()
        waitForIdle()
        field.performTextReplacement("9")
        waitForIdle()
        runOnIdle { visible.value = false }
        waitForIdle()

        assertEquals(9, s.prefs.bcn_beaconIntervalInMinutesMainCat, "the typed entry was lost")
        assertEquals(1, s.restarts)
        assertTrue(s.refusals.isEmpty(), "a valid entry was refused: ${s.refusals}")
    }

    /*
     * The stored value can change under a field -- a profile switch puts 5 back after the field
     * committed 9. The field used to keep showing 9 then, because nothing recomposed the tab
     * after a commit, and #128 made leaving it commit the shown 9 again. Since the edits go
     * through the settings draft, which is Compose state, a commit recomposes the tab: the field
     * follows the stored value, shows the 5, and leaving it commits nothing. Either way what the
     * field shows is what is stored once the operator has left it.
     */
    @Test
    fun aValueResetBehindTheFieldIsShownAndNotCommittedAgain() = runComposeUiTest {
        val s = Beacon(preferences())
        s.prefs.bcn_beaconIntervalInMinutesMainCat = 5
        val generation = mutableStateOf(0)
        val reportA: (String) -> Unit = { s.refusals += it }
        val reportB: (String) -> Unit = { s.refusals += it }
        // A different callback per generation, so bumping it recomposes BeaconTab itself.
        showTab { BeaconTab(s.state, if (generation.value % 2 == 0) reportA else reportB) }

        typeAndCommit("Interval in whole minutes", "9")
        assertEquals(9, s.prefs.bcn_beaconIntervalInMinutesMainCat)

        runOnIdle {
            s.prefs.bcn_beaconIntervalInMinutesMainCat = 5
            generation.value++
        }
        waitForIdle()
        fieldBeside("Interval in whole minutes").assertTextEquals("5")

        fieldBeside("Beacon message").performScrollTo().performClick()
        waitForIdle()

        assertEquals(5, s.prefs.bcn_beaconIntervalInMinutesMainCat, "the field committed a value it no longer showed")
        assertEquals(1, s.restarts)
    }

    /** Leaving the field commits what was typed, without Enter, and restarts the timer once. */
    @Test
    fun aTypedIntervalIsCommittedWhenTheFieldIsLeft() = runComposeUiTest {
        val s = Beacon(preferences())
        s.prefs.bcn_beaconIntervalInMinutesMainCat = 5
        showTab { BeaconTab(s.state) { s.refusals += it } }

        typeAndLeave("Interval in whole minutes", "9", leaveTo = "Beacon message")

        assertEquals(9, s.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(1, s.restarts)

        typeAndLeave("Beacon message", "cq de dn9apw", leaveTo = "Beacon message", leaveToOccurrence = 1)

        assertEquals("cq de dn9apw", s.prefs.bcn_beaconTextMainCat)
        assertEquals(1, s.restarts, "leaving a template field restarted the timer")
        assertTrue(s.refusals.isEmpty(), "a valid entry was refused: ${s.refusals}")
    }

    // ------------------------------------------------------------------ Notification

    private class Notification(val prefs: ChatPreferences, val draft: SettingsDraft = ImmediateDraft()) {
        init {
            prefs.isNotify_dxClusterServerEnabled = false
        }
        val monitored = SimpleRoster<String>()
        val serverEnabled = mutableListOf<Boolean>()
        val ports = mutableListOf<Int>()
        val spots = mutableListOf<ChatMember>()
        val refusals = mutableListOf<String>()
        val state = NotificationTabState(
            prefs,
            draft,
            monitored,
            // What the server is brought to on apply: its switch, and the port it binds.
            applyDxClusterServer = {
                serverEnabled += prefs.isNotify_dxClusterServerEnabled
                ports += prefs.notify_dxclusterServerPort
            },
            broadcastTestSpot = { spots += it; "no logger connected" },
        )
    }

    @Test
    fun notificationStoresItsFieldsAndStartsTheClusterServer() = runComposeUiTest {
        val s = Notification(preferences())
        showTab { NotificationTab(s.state) { s.refusals += it } }

        listOf("Sounds", "Band upgrade hints", "Local DX Cluster server", "QSO monitoring")
            .forEach { onNodeWithText(it).assertExists() }

        val before = s.prefs.isNotify_playSimpleSounds
        toggle("Play simple sounds")
        assertEquals(!before, s.prefs.isNotify_playSimpleSounds)

        toggle("Run the DX Cluster server")
        assertTrue(s.prefs.isNotify_dxClusterServerEnabled)
        assertEquals(listOf(true), s.serverEnabled)
        s.ports.clear()

        typeAndCommit("Spotter callsign", "dn9apw")
        assertEquals("DN9APW", s.prefs.notify_DXCSrv_SpottersCallSign.get())

        val storedPort = s.prefs.notify_dxclusterServerPort
        typeAndCommit("TCP port", "0")
        assertEquals(1, s.refusals.size, "an impossible port was not refused")
        assertEquals(storedPort, s.prefs.notify_dxclusterServerPort)
        assertTrue(s.ports.isEmpty(), "the server was moved to a refused port")
    }

    /*
     * Opening the tab used to commit the port and the spotter callsign once, because a
     * committed field's focus callback fires with "not focused" when it is first attached.
     * A stored value the rule refuses was then reported the moment the tab was looked at.
     */
    @Test
    fun openingTheNotificationTabCommitsNothing() = runComposeUiTest {
        val s = Notification(preferences())
        s.prefs.setNotify_DXCSrv_SpottersCallSign("x")
        showTab { NotificationTab(s.state) { s.refusals += it } }

        assertTrue(s.refusals.isEmpty(), "opening the tab reported: ${s.refusals}")
        assertTrue(s.ports.isEmpty(), "opening the tab moved the cluster server")
        assertEquals("x", s.prefs.notify_DXCSrv_SpottersCallSign.get())
    }

    /** Leaving the field commits what was typed, without Enter. */
    @Test
    fun aTypedSpotterCallsignAndPortAreCommittedWhenTheFieldIsLeft() = runComposeUiTest {
        val s = Notification(preferences())
        s.prefs.notify_dxclusterServerPort = 7300
        showTab { NotificationTab(s.state) { s.refusals += it } }

        typeAndLeave("Spotter callsign", "do5amf", leaveTo = "TCP port")
        assertEquals("DO5AMF", s.prefs.notify_DXCSrv_SpottersCallSign.get())

        typeAndLeave("TCP port", "7301", leaveTo = "Spotter callsign")
        assertEquals(7301, s.prefs.notify_dxclusterServerPort)
        assertEquals(listOf(7301), s.ports)
        assertTrue(s.refusals.isEmpty(), "a valid entry was refused: ${s.refusals}")
    }

    @Test
    fun theFallbackBandAndTheTestSpotReachTheirTargets() = runComposeUiTest {
        val s = Notification(preferences())
        showTab { NotificationTab(s.state) { s.refusals += it } }

        val current = s.state.notify_optionalFrequencyPrefix
        onNodeWithText(current.prefix + " MHz").performScrollTo().performClick()
        waitForIdle()
        val other = Band.entries.first { it != current }
        awaitNode(inPopup(other.prefix + " MHz")).performClick()
        waitForIdle()
        assertEquals(other.prefix, s.prefs.notify_optionalFrequencyPrefix.get())

        onNodeWithText("Send a test spot").performScrollTo().performClick()
        waitForIdle()
        assertEquals("DO5AMF", s.spots.single().callSign)
        assertEquals(listOf("no logger connected"), s.refusals, "the outcome was not reported")
    }

    /** The add button asks for the callsign first; what is confirmed reaches the roster. */
    @Test
    fun aMonitoredCallsignAddedThroughTheDialogReachesTheRoster() = runComposeUiTest {
        val s = Notification(preferences())
        showTab { NotificationTab(s.state) { s.refusals += it } }

        onNodeWithText("Add monitored callsign").performScrollTo().performClick()
        waitForIdle()
        awaitNode(inDialog("Add a callsign to the monitoring list"))

        onNodeWithTag(MONITORED_CALLSIGN_FIELD_TAG).performTextReplacement("DL1ABC")
        waitForIdle()
        awaitNode(inDialog("OK")).performClick()
        awaitGone(inDialog("OK"))

        assertEquals(listOf("DL1ABC"), s.monitored.snapshot())
        assertFalse(s.refusals.isNotEmpty(), "the callsign was refused: ${s.refusals}")
    }
}
