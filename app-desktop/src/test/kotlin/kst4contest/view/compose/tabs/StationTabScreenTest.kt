package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.mutableStateOf
import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.MainWindowSurroundings
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.feed.ConnectionStateFeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kst4contest.view.compose.awaitNode
import kst4contest.view.compose.awaitGone
import kst4contest.view.compose.inDialog
import kst4contest.view.compose.inPopup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The Station tab, drawn (#107).
 *
 * [StationTabStateTest] pins the facade; this pins that the drawn controls show what the
 * profile holds and that editing one writes through, which is the class of fault the
 * settings window shipped repeatedly with a green build (`83e23f55`, Etappe 7).
 */
@OptIn(ExperimentalTestApi::class)
class StationTabScreenTest {

    /*
     * Never the no-argument ChatPreferences: it resolves the operator's real profile and
     * seeds preferences.xml there when it is missing. A file in a temporary directory is
     * never read or written by these tests, it only gives the preferences a home.
     */
    @TempDir
    lateinit var directory: Path

    private fun preferences(): ChatPreferences = ChatPreferences(directory.resolve("preferences.xml"))

    private class Session(isolated: ChatPreferences) {
        val prefs = isolated.apply {
            stn_loginCallSign = "DN9APW"
            stn_on4kstServersPort = 23001
            isStn_bandActive144 = true
            isStn_bandActive10G = false
        }
        val mainApplied = mutableListOf<ChatCategory?>()
        val link = mutableStateOf(On4KstConnectionState.DISCONNECTED)
        val draft = SettingsDraft()
        val state = StationTabState(prefs, draft, { mainApplied += it }, { }, { link.value })
    }

    @Test
    fun theTabShowsItsSectionsAndWhatTheProfileHolds() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        listOf("Own station", "Chat category", "ON4KST server", "Active bands", "Defaults",
            "Manual antenna heading", "PSTRotator", "Path analysis defaults").forEach {
            onNodeWithText(it).assertExists()
        }

        fieldBeside("Callsign").assertTextEquals("DN9APW")
        fieldBeside("Server port").assertTextEquals("23001")
        checkBeside("144 MHz").assertIsOn()
        checkBeside("10 GHz").assertIsOff()
    }

    @Test
    fun qtfFilterHeadingOptionFollowsTheDraftedPrimaryRotatorState() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        val label = "Use QTF filter direction as antenna heading"
        checkBeside(label).assertIsEnabled().assertIsOn()

        toggle("Rotator sync enabled")

        checkBeside(label).assertIsNotEnabled().assertIsOn()
        toggle("Rotator sync enabled")
        checkBeside(label).assertIsEnabled().assertIsOn()
        runOnIdle { s.draft.apply() }
        assertFalse(s.prefs.isStn_pstRotatorEnabled())
        assertTrue(s.prefs.isStn_useQtfFilterDirectionAsAntennaHeading())
    }

    @Test
    fun manualAntennaHeadingPanelRendersInEveningTheme() = runComposeUiTest {
        val s = Session(preferences())

        showTab(darkMode = true) { StationTab(s.state) }

        onNodeWithText("Manual antenna heading").assertExists()
        checkBeside("Use QTF filter direction as antenna heading")
            .assertIsEnabled()
            .assertIsOn()
    }

    /* The credentials write through; every other field waits for Apply. */
    @Test
    fun editingTheTextFieldsStagesAllButTheCredentials() = runComposeUiTest {
        val s = Session(preferences())
        val storedHost = s.prefs.stn_on4kstServersDns
        showTab { StationTab(s.state) }

        type("Callsign", "DO5AMF")
        type("Locator", "JO51IJ")
        type("Server host name", "www.on4kst.info")

        assertEquals("DO5AMF", s.prefs.stn_loginCallSign)
        assertEquals("JO51IJ", s.prefs.stn_loginLocatorMainCat)
        assertEquals(storedHost, s.prefs.stn_on4kstServersDns, "the host took effect before Apply")
        fieldBeside("Server host name").assertTextEquals("www.on4kst.info")

        runOnIdle { s.draft.apply() }
        assertEquals("www.on4kst.info", s.prefs.stn_on4kstServersDns)
        fieldBeside("Callsign").assertTextEquals("DO5AMF")
    }

    /** A number field writes only what parses; a half-typed entry leaves the stored value. */
    @Test
    fun theNumberFieldsWriteOnlyWhatParses() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        type("Server port", "23000")
        runOnIdle { s.draft.apply() }
        assertEquals(23000, s.prefs.stn_on4kstServersPort)

        type("Server port", "23x")
        runOnIdle { s.draft.apply() }
        assertEquals(23000, s.prefs.stn_on4kstServersPort, "an unparsable port was written")

        type("QTF", "135,5")
        runOnIdle { s.draft.apply() }
        assertEquals(135.5, s.prefs.stn_qtfDefault, 1e-9)
    }

    @Test
    fun invalidQtfDefaultsAreNotStaged() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        listOf("360,1", "-0,1", "NaN", "Infinity").forEach { invalid ->
            type("QTF", invalid)
            runOnIdle { s.draft.apply() }
            assertEquals(135.0, s.prefs.stn_qtfDefault, 1e-9, "accepted $invalid")
        }
    }

    /*
     * The draft is Compose state, so a staged number comes straight back to its field. The
     * field must keep the operator's text rather than redraw the number: "1" shown as "1.0"
     * before the next key would make "135,5" impossible to type.
     */
    @Test
    fun aDecimalCanBeTypedKeyByKey() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        val field = fieldBeside("QTF")
        field.performScrollTo().performClick()
        field.performTextReplacement("")
        "135,5".forEach { key ->
            field.performTextInput(key.toString())
            waitForIdle()
        }

        field.assertTextEquals("135,5")
        runOnIdle { s.draft.apply() }
        assertEquals(135.5, s.prefs.stn_qtfDefault, 1e-9)
    }

    @Test
    fun aBandCheckboxIsStagedAndApplied() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        toggle("10 GHz")
        checkBeside("10 GHz").assertIsOn()
        assertFalse(s.prefs.isStn_bandActive10G, "the box took effect before Apply")

        toggle("144 MHz")
        runOnIdle { s.draft.apply() }
        assertTrue(s.prefs.isStn_bandActive10G)
        assertFalse(s.prefs.isStn_bandActive144)
    }

    /*
     * In a contest the session is up. Everything but the credentials stays editable then,
     * and an edit still waits for Apply.
     */
    @Test
    fun theDraftedFieldsCanBeEditedWhileConnected() = runComposeUiTest {
        val s = Session(preferences())
        s.link.value = On4KstConnectionState.ONLINE
        showTab { StationTab(s.state) }

        fieldBeside("Callsign").assertIsNotEnabled()
        fieldBeside("Server port").assertIsEnabled()
        toggle("10 GHz")
        type("Server port", "23005")

        assertFalse(s.prefs.isStn_bandActive10G)
        runOnIdle { s.draft.apply() }
        assertTrue(s.prefs.isStn_bandActive10G)
        assertEquals(23005, s.prefs.stn_on4kstServersPort)
    }

    @Test
    fun choosingTheMainCategoryWritesAndAppliesIt() = runComposeUiTest {
        val s = Session(preferences())
        s.prefs.loginChatCategoryMain = ChatCategory(2)
        showTab { StationTab(s.state) }

        val target = s.state.availableCategories.first { it.categoryNumber == 3 }
        val current = s.state.describeCategory(s.prefs.loginChatCategoryMain)
        // The main category is the upper of the two pickers; the second may show the same name.
        onAllNodesWithText(current).onFirst().performScrollTo().performClick()
        waitForIdle()
        awaitNode(inPopup(s.state.describeCategory(target))).performClick()
        waitForIdle()

        assertEquals(3, s.prefs.loginChatCategoryMain?.categoryNumber)
        assertEquals(3, s.mainApplied.single()?.categoryNumber, "the running session was not told")
    }

    /*
     * The credentials are locked while a session exists or is being made: the running
     * connection would not pick up a change, so an edit would look applied and be ignored
     * until the next login. The lock used to be a plain field nothing ever wrote.
     */
    @Test
    fun theLoginFieldsAreLockedWhileConnectedOrConnecting() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }
        val credentials = listOf("Callsign", "Name, main category", "Name, second category", "Locator", "Password")

        credentials.forEach { fieldBeside(it).assertIsEnabled() }

        runOnIdle { s.link.value = On4KstConnectionState.CONNECTING }
        waitForIdle()
        credentials.forEach { fieldBeside(it).assertIsNotEnabled() }

        runOnIdle { s.link.value = On4KstConnectionState.ONLINE }
        waitForIdle()
        credentials.forEach { fieldBeside(it).assertIsNotEnabled() }
        fieldBeside("Server host name").assertIsEnabled()

        runOnIdle { s.link.value = On4KstConnectionState.DISCONNECTED }
        waitForIdle()
        credentials.forEach { fieldBeside(it).assertIsEnabled() }
    }

    /*
     * The application hands the tab `surroundings.connectionState`, which ConnectionStateFeed
     * fills from the controller's callback. The application class itself cannot be built in
     * a test -- it needs a controller with its database, the windows and the update service --
     * so this drives the same chain from the feed on: a pushed state reaches the drawn tab
     * without anything rebuilding it.
     */
    @Test
    fun theLoginFieldsFollowWhatTheConnectionFeedPushes() = runComposeUiTest {
        val surroundings = MainWindowSurroundings()
        val immediately = object : UiDispatcher {
            override fun runOnUi(task: Runnable) = task.run()
            override fun isUiThread(): Boolean = true
        }
        val feed = ConnectionStateFeed(surroundings, immediately)
        val state = StationTabState(preferences(), SettingsDraft(), { }, { }, { surroundings.connectionState })
        showTab { StationTab(state) }

        fieldBeside("Callsign").assertIsEnabled()

        runOnIdle { feed.push(On4KstConnectionState.AUTHENTICATING, null) }
        waitForIdle()
        fieldBeside("Callsign").assertIsNotEnabled()

        // An absent state reads as DISCONNECTED in the feed, and unlocks the fields.
        runOnIdle { feed.push(null, null) }
        waitForIdle()
        fieldBeside("Callsign").assertIsEnabled()
    }

    private val ownHeightLabel = "Own ground height ASL, metres"
    private val antennaHeightLabel = "Own antenna height, metres"

    /** Far down the tab: the row lookup compares on-screen bounds, so bring it into view. */
    private fun androidx.compose.ui.test.ComposeUiTest.scrollToOwnHeight() {
        onNodeWithText(ownHeightLabel).performScrollTo()
        waitForIdle()
    }

    /** Types [keys] one by one into the cleared own-height field, as an operator would. */
    private fun androidx.compose.ui.test.ComposeUiTest.typeKeysIntoOwnHeight(keys: String) {
        val field = fieldBeside(ownHeightLabel)
        field.performClick()
        field.performTextClearance()
        keys.forEach { field.performTextInput(it.toString()) }
        waitForIdle()
    }

    /** Leaves the own-height field by clicking the antenna height field above it. */
    private fun androidx.compose.ui.test.ComposeUiTest.leaveOwnHeight() {
        fieldBeside(antennaHeightLabel).performClick()
        waitForIdle()
    }

    /*
     * Typed key by key, nothing is staged until the field is left, nothing reaches the
     * preferences before Apply, and the typed text is never replaced by the value read back
     * ("2" did turn into "2.0" while typing "250").
     */
    @Test
    fun theOwnHeightIsTypedKeyByKeyAndCommittedOnLeavingTheField() = runComposeUiTest {
        val s = Session(preferences())
        val refusals = mutableListOf<String>()
        showTab { StationTab(s.state) { refusals += it } }
        scrollToOwnHeight()

        fieldBeside(ownHeightLabel).assertTextEquals("")

        typeKeysIntoOwnHeight("250")
        fieldBeside(ownHeightLabel).assertTextEquals("250")
        assertTrue(s.state.stn_pathAnalysisOwnHeightAslMeters.isNaN(), "staged before the field was left")

        leaveOwnHeight()
        assertEquals(250.0, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9)

        typeKeysIntoOwnHeight("12,5")
        fieldBeside(ownHeightLabel).assertTextEquals("12,5")
        leaveOwnHeight()
        assertEquals(12.5, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9)
        assertTrue(s.prefs.stn_pathAnalysisOwnHeightAslMeters.isNaN(), "the height took effect before Apply")

        runOnIdle { s.draft.apply() }
        waitForIdle()
        assertEquals(12.5, s.prefs.stn_pathAnalysisOwnHeightAslMeters, 1e-9)
        fieldBeside(ownHeightLabel).assertTextEquals("12,5")

        assertEquals(listOf<String>(), refusals)
    }

    /*
     * After Enter the field keeps the operator's draft while the stored value reads back
     * differently ("250" against "250.0"). A recomposition of the tab must not swap the
     * draft for the stored text and drop what was typed since. The tab is forced to
     * recompose down to the field: the refusal callback captures a counter, so a new
     * counter value hands the path-analysis section a new lambda and it re-reads the
     * stored text. A connection change, the tab's own state, is driven as well.
     */
    @Test
    fun aRecompositionAfterACommitKeepsTheKeysTypedSince() = runComposeUiTest {
        val s = Session(preferences())
        val tick = mutableStateOf(0)
        val refusals = mutableListOf<String>()
        showTab {
            val seen = tick.value
            StationTab(s.state) { refusals += "$it ($seen)" }
        }
        scrollToOwnHeight()

        typeKeysIntoOwnHeight("25")
        fieldBeside(ownHeightLabel).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(25.0, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9)

        fieldBeside(ownHeightLabel).performTextInput("0")
        waitForIdle()
        fieldBeside(ownHeightLabel).assertTextEquals("250")

        runOnIdle { tick.value++ }
        waitForIdle()
        fieldBeside(ownHeightLabel).assertTextEquals("250")

        runOnIdle { s.link.value = On4KstConnectionState.CONNECTING }
        waitForIdle()
        fieldBeside(ownHeightLabel).assertTextEquals("250")

        fieldBeside(ownHeightLabel).performTextInput("1")
        waitForIdle()
        runOnIdle { tick.value++ }
        waitForIdle()
        fieldBeside(ownHeightLabel).assertTextEquals("2501")

        leaveOwnHeight()
        assertEquals(2501.0, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9)
        assertEquals(listOf<String>(), refusals)

        // Applying changes the stored spelling to "2501.0"; the field must keep "2501".
        runOnIdle { s.draft.apply() }
        waitForIdle()
        assertEquals(2501.0, s.prefs.stn_pathAnalysisOwnHeightAslMeters, 1e-9)
        fieldBeside(ownHeightLabel).assertTextEquals("2501")
    }

    @Test
    fun anOutOfRangeOwnHeightIsRefusedAndFallsBackToTheStoredValue() = runComposeUiTest {
        val s = Session(preferences())
        val refusals = mutableListOf<String>()
        showTab { StationTab(s.state) { refusals += it } }
        scrollToOwnHeight()

        typeAndCommit(ownHeightLabel, "250")
        assertEquals(250.0, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9)

        typeKeysIntoOwnHeight("9500")
        leaveOwnHeight()

        assertEquals(250.0, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9, "an out-of-range height was written")
        assertEquals(1, refusals.size, "the refusal was not reported")
        assertTrue(refusals.single().contains("9500"))
        fieldBeside(ownHeightLabel).assertTextEquals("250")
    }

    @Test
    fun clearingTheOwnHeightHandsItBackToTheElevationModel() = runComposeUiTest {
        val s = Session(preferences())
        s.prefs.stn_pathAnalysisOwnHeightAslMeters = 312.0
        showTab { StationTab(s.state) }
        scrollToOwnHeight()

        fieldBeside(ownHeightLabel).assertTextEquals("312.0")

        typeAndCommit(ownHeightLabel, "")

        assertTrue(s.state.stn_pathAnalysisOwnHeightAslMeters.isNaN(), "clearing did not hand back to the model")
        assertEquals(312.0, s.prefs.stn_pathAnalysisOwnHeightAslMeters, 1e-9, "cleared before Apply")

        runOnIdle { s.draft.apply() }
        assertTrue(s.prefs.stn_pathAnalysisOwnHeightAslMeters.isNaN())
    }

    @Test
    fun theHighestPointCheckboxIsOnAndTakesEffectOnApply() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }
        val label = "Target station on the highest point of its locator square"

        checkBeside(label).assertIsOn()

        toggle(label)
        checkBeside(label).assertIsOff()
        assertTrue(s.prefs.isStn_pathAnalysisDxHighestPointInSquare, "the switch took effect before Apply")

        runOnIdle { s.draft.apply() }
        assertFalse(s.prefs.isStn_pathAnalysisDxHighestPointInSquare)
    }
}
