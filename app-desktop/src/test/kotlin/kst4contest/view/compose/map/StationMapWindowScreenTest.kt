package kst4contest.view.compose.map

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import kst4contest.locatorUtils.Location
import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ChatPreferences
import kst4contest.model.ClusterMessage
import kst4contest.view.compose.BlinkingNotice
import kst4contest.view.compose.ChatInputActions
import kst4contest.view.compose.ChatInputState
import kst4contest.view.compose.DataTableState
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.compose.MainMenuState
import kst4contest.view.compose.MainWindowState
import kst4contest.view.compose.MainWindowSurroundings
import kst4contest.view.compose.RowKeys
import kst4contest.view.compose.SelectedStationState
import kst4contest.view.compose.SplitterState
import kst4contest.view.compose.StationColumns
import kst4contest.view.compose.StationFilterState
import kst4contest.view.compose.StationSelectionSource
import kst4contest.view.compose.StationSelectionState
import kst4contest.view.compose.ThreadStatusButtons
import kst4contest.view.compose.TimelineState
import kst4contest.view.compose.TopPriorityState
import kst4contest.view.map.StationMapClusterer
import kst4contest.view.map.MapCallsignRawSnapshot
import kst4contest.view.map.PathAnalysisResult
import kst4contest.view.map.TileFetcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.io.IOException
import java.util.EnumSet

/**
 * The station map window's body, drawn and clicked (#107).
 *
 * The map has the longest regression history of the Compose windows, and the two that
 * reached an operator are both about what a click on the map does to the rest of the
 * application:
 *
 * - `21251377` (Codex review findings on PR 89): a marker click and "Reset view" set the
 *   map's own copy of the selection instead of going through the central station selector,
 *   so the station list, the score service and the send line did not follow the map.
 * - `cc8c9020` (Draw the whole interface in Compose, in `main` as `2a5c89b9`): an earlier
 *   Compose version recomputed the zoom on every selection and threw an operator working at
 *   zoom 15 out to about 7. AGENTS.md: reset clears the selection without changing the zoom.
 *
 * Tiles never arrive: an empty temporary cache and a source that always fails, the
 * arrangement `3dd2cf10` gave the drawing test after the default fetcher read the user's
 * disk cache and the real tile server. No running application exists: the path analysis is
 * either absent, as it is before a runtime exists, or a recording stand-in passed through the
 * window's [MapPathAnalysis] seam, which is what lets the zoom rule on selection run here.
 *
 * Headless: [StationMapWindow.Content] is drawn on its own, no window is opened.
 */
@OptIn(ExperimentalTestApi::class)
class StationMapWindowScreenTest {

    /*
     * Never the no-argument ChatPreferences: it resolves the operator's real profile and
     * seeds preferences.xml there when it is missing. A file in a temporary directory is
     * never read or written by these tests, it only gives the preferences a home.
     */
    @TempDir
    lateinit var directory: Path

    private fun preferences(): ChatPreferences = ChatPreferences(directory.resolve("preferences.xml"))

    private lateinit var offlineTileFetcher: TileFetcher

    @BeforeEach
    fun openTileFetcher() {
        offlineTileFetcher = TileFetcher(
            directory.resolve("tile-cache"),
            { throw IOException("no tile source in this test") },
        )
    }

    /** The fetcher owns a thread pool; the window closes it on dispose, and so does this. */
    @AfterEach
    fun closeTileFetcher() {
        offlineTileFetcher.close()
    }

    /** Records what the window asks the path analysis for, instead of a running application. */
    private class RecordingPathAnalysis : MapPathAnalysis {
        val requested = mutableListOf<ChatMember>()
        override fun request(
            member: ChatMember,
            snapshot: MapCallsignRawSnapshot?,
            onResult: (PathAnalysisResult) -> Unit,
        ) {
            requested += member
        }
    }

    private val ownLocator = "JO40HA"

    private fun station(call: String, category: Int, locator: String = "JO50JP") = ChatMember().apply {
        callSign = call
        qra = locator
        chatCategory = ChatCategory(category)
    }

    private class NoChatActions : ChatInputActions {
        override fun panelSelection(): ChatMember? = null
        override fun tableSelection(): ChatMember? = null
        override fun scoreSelection(): ChatMember? = null
        override fun resolveVariables(template: String, member: ChatMember?): String? = template
        override fun isAddressedToOwnCallsign(text: String): Boolean = false
        override fun resolveCategory(text: String, member: ChatMember?): ChatCategory? = null
        override fun mainCategory(): ChatCategory? = null
        override fun recordOutboundCq(text: String) {}
        override fun requestRecompute(reason: String) {}
        override fun queue(message: ChatMessage) {}
        override fun ownQrgMain(): String = ""
        override fun ownQrgSecond(): String = ""
    }

    private fun windowState(prefs: ChatPreferences): MainWindowState {
        val stationSelection = StationSelectionState()
        val stations = DataTableState(
            StationColumns.all(
                tropoOf = { "" },
                priorityScoreOf = { 0.0 },
                formatBand = { _, _, _ -> "" },
            ),
            RowKeys.byValue(StationColumns::rowKey),
            selectionModel = stationSelection,
            keepSelectionVisible = true,
        )
        val menu = MainMenuState()
        return MainWindowState(
            activeBands = EnumSet.of(Band.B_144, Band.B_432),
            prefs = prefs,
            stations = stations,
            stationFilter = StationFilterState(),
            directedMessages = DataTableState(emptyList(), RowKeys.byReference<ChatMessage>()),
            publicMessages = DataTableState(emptyList(), RowKeys.byReference<ChatMessage>()),
            clusterMessages = DataTableState(emptyList(), RowKeys.byReference<ClusterMessage>()),
            qsoOfTheOther = DataTableState(emptyList(), RowKeys.byReference<ChatMessage>()),
            selectedStationMessages = DataTableState(emptyList(), RowKeys.byReference<ChatMessage>()),
            selectedStation = SelectedStationState(
                propagateNotQrv = {},
                createSked = { _, _, _, _ -> false },
                stationSelection = stationSelection,
            ),
            topPriority = TopPriorityState(topStations = { emptyList() }, onSelect = {}),
            timeline = TimelineState(),
            chatInput = ChatInputState(NoChatActions()),
            menu = menu,
            surroundings = MainWindowSurroundings(menu),
            skedNotice = BlinkingNotice(),
            bandUpgradeNotice = BlinkingNotice(),
            threadButtons = ThreadStatusButtons(),
            outerSplitter = SplitterState(2, doubleArrayOf(0.5)) {},
            messageSplitter = SplitterState(MainWindowState.MESSAGE_PANE_COUNT, doubleArrayOf(0.2, 0.4, 0.6, 0.8)) {},
            rightSplitter = SplitterState(MainWindowState.RIGHT_PANE_COUNT, doubleArrayOf(0.4, 0.6)) {},
        )
    }

    private fun prefs(pathAnalysisVisible: Boolean = false) = preferences().apply {
        stn_loginLocatorMainCat = ownLocator
        isGUIstationMapPathAnalysisVisible = pathAnalysisVisible
        isGUIstationMapClusteringEnabled = false
    }

    private fun ComposeUiTest.show(
        state: MainWindowState,
        members: List<ChatMember>,
        mapState: MapState,
        pathAnalysis: MapPathAnalysis? = null,
        onPathAnalysisAsked: () -> Unit = {},
        onAlwaysOnTopChanged: (Boolean) -> Unit = {},
    ) {
        state.stations.replaceRows(members)
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                StationMapWindow.Content(
                    mainWindowState = state,
                    tileFetcher = offlineTileFetcher,
                    chatMembers = { members },
                    mapState = mapState,
                    pathAnalysis = { onPathAnalysisAsked(); pathAnalysis },
                    onAlwaysOnTopChanged = onAlwaysOnTopChanged,
                )
            }
        }
        waitForIdle()
    }

    /** A map looking straight at a locator, so its marker sits in the middle of the canvas. */
    private fun mapLookingAt(locator: String, zoom: Float): MapState {
        val location = Location(locator)
        return MapState(
            zoom = zoom,
            centerLon = location.longitude.toDegrees(),
            centerLat = location.latitude.toDegrees(),
        )
    }

    @Test
    fun resetViewClearsTheSelectionThroughTheSelectorAndKeepsTheZoom() = runComposeUiTest {
        val state = windowState(prefs())
        val dl1abc = station("DL1ABC", 2)
        state.selectedStation.select(dl1abc)
        val selectorCalls = mutableListOf<ChatMember?>()
        state.stationSelector = { member, _ -> selectorCalls += member }
        val mapState = mapLookingAt("JO50JP", zoom = 15f)

        show(state, listOf(dl1abc), mapState)

        onNodeWithText("Reset view").performClick()
        waitForIdle()

        assertEquals(listOf<ChatMember?>(null), selectorCalls, "reset did not go through the station selector")
        assertSame(
            dl1abc,
            state.selectedStation.selected,
            "reset cleared the panel's selection behind the selector's back",
        )
        assertEquals(15f, mapState.zoom, "reset changed the zoom")

        val home = Location(ownLocator)
        assertEquals(home.longitude.toDegrees(), mapState.centerLon, 1e-9)
        assertEquals(home.latitude.toDegrees(), mapState.centerLat, 1e-9)
    }

    /** Without a selector -- before the host set one -- reset still clears the selection. */
    @Test
    fun resetViewWithoutASelectorClearsThePanelSelection() = runComposeUiTest {
        val state = windowState(prefs())
        val dl1abc = station("DL1ABC", 2)
        state.selectedStation.select(dl1abc)

        show(state, listOf(dl1abc), mapLookingAt("JO50JP", zoom = 9f))

        onNodeWithText("Reset view").performClick()
        waitForIdle()

        assertEquals(null, state.selectedStation.selected)
    }

    /**
     * A marker stands for a raw callsign. With one of its category variants already selected
     * the click keeps that one, goes through the selector, and prepares "/cq CALL" over
     * whatever was typed -- the JavaFX handleMapCallsignSelection behaviour.
     */
    @Test
    fun aMarkerClickSelectsThroughTheSelectorAndKeepsTheSelectedCategory() = runComposeUiTest {
        val state = windowState(prefs())
        val inMain = station("DL1ABC", 2)
        val inSecond = station("DL1ABC", 3)
        state.selectedStation.select(inSecond)
        state.chatInput.value = TextFieldValue("half typed")
        val selectorCalls = mutableListOf<ChatMember?>()
        state.stationSelector = { member, _ -> selectorCalls += member }
        val mapState = mapLookingAt("JO50JP", zoom = 12f)

        show(state, listOf(inMain, inSecond), mapState)

        onNodeWithTag(StationMapWindow.MAP_CANVAS_TAG).performClick()
        waitForIdle()

        assertEquals(1, selectorCalls.size, "the marker click did not reach the station selector")
        assertSame(inSecond, selectorCalls.single(), "the click swapped the selected category")
        assertEquals("/cq DL1ABC ", state.chatInput.value.text)
        assertEquals(12f, mapState.zoom, "selecting from the map changed the zoom")
    }

    @Test
    fun aMarkerClickWithNothingSelectedTakesTheMainCategory() = runComposeUiTest {
        val state = windowState(prefs())
        val inMain = station("DL1ABC", 2)
        val inSecond = station("DL1ABC", 3)
        val scrollRevision = state.stations.scrollRequestRevision

        show(state, listOf(inMain, inSecond), mapLookingAt("JO50JP", zoom = 12f))

        onNodeWithTag(StationMapWindow.MAP_CANVAS_TAG).performClick()
        waitForIdle()

        assertSame(inMain, state.stations.selectedRow)
        assertEquals(StationSelectionSource.MAP, state.stationSelection.lastSource)
        assertTrue(state.stations.scrollRequestRevision > scrollRevision)
    }

    /**
     * The cc8c9020 rule: selecting a station pans the map to it and leaves the zoom alone.
     * The earlier Compose version recomputed the zoom here and threw zoom 15 out to about 7.
     */
    @Test
    fun selectingAStationPansToItAndKeepsTheZoom() = runComposeUiTest {
        val state = windowState(prefs())
        val dl1abc = station("DL1ABC", 2, locator = "JO50JP")
        val analysis = RecordingPathAnalysis()
        val mapState = mapLookingAt(ownLocator, zoom = 15f)

        show(state, listOf(dl1abc), mapState, analysis)

        runOnIdle { state.selectedStation.select(dl1abc) }
        waitForIdle()

        val target = Location("JO50JP")
        assertEquals(target.longitude.toDegrees(), mapState.centerLon, 1e-9, "the map did not pan")
        assertEquals(target.latitude.toDegrees(), mapState.centerLat, 1e-9, "the map did not pan")
        assertEquals(15f, mapState.zoom, "selecting a station changed the zoom")
        assertEquals(listOf(dl1abc), analysis.requested, "the path analysis was not asked")
    }

    /**
     * With grouping on, a selection zooms in only as far as it takes to break the station out
     * of its bubble, and never out: StationMapClusterer.zoomToRevealStations.
     */
    @Test
    fun withGroupingOnASelectionOnlyZoomsInFarEnoughToRevealTheStation() = runComposeUiTest {
        val prefs = prefs().apply { isGUIstationMapClusteringEnabled = true }
        val state = windowState(prefs)
        val dl1abc = station("DL1ABC", 2, locator = "JO50JP")
        val far = mapLookingAt(ownLocator, zoom = 5f)

        show(state, listOf(dl1abc), far, RecordingPathAnalysis())

        runOnIdle { state.selectedStation.select(dl1abc) }
        waitForIdle()

        assertEquals(StationMapClusterer.DISABLE_AT_ZOOM.toFloat(), far.zoom)
    }

    /** Without a running application there is nothing to ask, and the map stays put. */
    @Test
    fun withoutAPathAnalysisASelectionLeavesTheMapWhereItIs() = runComposeUiTest {
        val state = windowState(prefs())
        val dl1abc = station("DL1ABC", 2, locator = "JO50JP")
        val mapState = mapLookingAt(ownLocator, zoom = 15f)
        val home = Location(ownLocator)
        var asked = 0

        show(state, listOf(dl1abc), mapState, pathAnalysis = null, onPathAnalysisAsked = { asked++ })

        runOnIdle { state.selectedStation.select(dl1abc) }
        waitForIdle()

        // Proves the selection effect ran, so the unchanged map is not a vacuous pass.
        assertEquals(1, asked, "the selection effect did not ask for a path analysis")
        assertEquals(home.longitude.toDegrees(), mapState.centerLon, 1e-9)
        assertEquals(15f, mapState.zoom)
    }

    /** The two header switches write the profile, so the map opens the same way next time. */
    @Test
    fun theHeaderSwitchesWriteThePreferences() = runComposeUiTest {
        val prefs = prefs(pathAnalysisVisible = true)
        val state = windowState(prefs)

        show(state, emptyList(), MapState())

        onNodeWithText("Hide path analysis").performClick()
        waitForIdle()
        assertEquals(false, prefs.isGUIstationMapPathAnalysisVisible)
        onNodeWithText("Path analysis is hidden.").assertExists()
        onNodeWithText("Show path analysis").assertExists()

        onNodeWithText("Show path analysis").performClick()
        waitForIdle()
        assertEquals(true, prefs.isGUIstationMapPathAnalysisVisible)

        // The grouping checkbox sits beside its label; the label itself is not clickable.
        onNodeWithText("Grouping").assertExists()
        onNodeWithTag(StationMapWindow.GROUPING_TAG).assertIsOff().performClick()
        waitForIdle()
        onNodeWithTag(StationMapWindow.GROUPING_TAG).assertIsOn()
        assertEquals(true, prefs.isGUIstationMapClusteringEnabled)
    }

    @Test
    fun alwaysOnTopWritesThePreferenceAndAppliesBothStatesImmediately() = runComposeUiTest {
        val prefs = prefs()
        val state = windowState(prefs)
        val applied = mutableListOf<Boolean>()

        show(state, emptyList(), MapState(), onAlwaysOnTopChanged = applied::add)

        onNodeWithText("Always on top").assertExists()
        onNodeWithTag(StationMapWindow.ALWAYS_ON_TOP_TAG).assertIsOff().performClick()
        waitForIdle()
        assertEquals(true, prefs.isGUIstationMapAlwaysOnTop)
        assertEquals(listOf(true), applied)

        onNodeWithTag(StationMapWindow.ALWAYS_ON_TOP_TAG).assertIsOn().performClick()
        waitForIdle()
        assertEquals(false, prefs.isGUIstationMapAlwaysOnTop)
        assertEquals(listOf(true, false), applied)
    }

    @Test
    fun resetFiltersUsesTheSharedActionAndDoesNotResetTheView() = runComposeUiTest {
        val state = windowState(prefs())
        state.stationFilter.minQrbKm = 300.0
        state.stationFilter.minQrbEnabled = true
        var resets = 0
        state.stationFiltersChanged = { resets++ }
        val mapState = mapLookingAt("JO50JP", zoom = 14f)
        val beforeLon = mapState.centerLon
        val beforeLat = mapState.centerLat

        show(state, emptyList(), mapState)
        onNodeWithText("Reset filters").performClick()
        waitForIdle()

        assertEquals(false, state.stationFilter.minQrbEnabled)
        assertEquals(1, resets)
        assertEquals(beforeLon, mapState.centerLon)
        assertEquals(beforeLat, mapState.centerLat)
        assertEquals(14f, mapState.zoom)
    }

    /**
     * The map shows the stations the list shows (#21). The Compose map drew every station
     * whatever the filter bar said, although its status line reported a filtered view; the
     * JavaFX map took the filtered list. A filter switched on after the first draw has to
     * take the marker away too, and a click where it stood selects nothing.
     */
    @Test
    fun aStationTheFiltersHideHasNoMarker() = runComposeUiTest {
        val state = windowState(prefs())
        val near = station("DL1NEAR", 2, locator = "JO50JP").apply { qrb = 150.0 }
        val far = station("DL2FAR", 2, locator = "JO62QM").apply { qrb = 600.0 }
        val selectorCalls = mutableListOf<ChatMember?>()
        state.stationSelector = { member, _ -> selectorCalls += member }

        show(state, listOf(near, far), mapLookingAt("JO50JP", zoom = 12f))
        onNodeWithText("Showing 2 visible stations").assertExists()

        runOnIdle {
            state.stationFilter.minQrbKm = 300.0
            state.stationFilter.minQrbEnabled = true
            state.stations.rowFilter = { state.stationFilter.acceptsQrb(it.qrb) }
        }
        waitForIdle()

        onNodeWithText("Showing 1 visible stations | filtered view active").assertExists()

        // The map looks straight at the hidden station: a click there finds no marker.
        onNodeWithTag(StationMapWindow.MAP_CANVAS_TAG).performClick()
        waitForIdle()
        assertEquals(emptyList<ChatMember?>(), selectorCalls, "the hidden station still had a marker")
    }

    /** A station whose QRB changed in place leaves the map after the redraw, as it leaves the list. */
    @Test
    fun aRedrawAfterAFieldChangedFiltersTheMapAgain() = runComposeUiTest {
        val state = windowState(prefs())
        val moving = station("DL1MOVE", 2, locator = "JO50JP").apply { qrb = 500.0 }
        val stay = station("DL2STAY", 2, locator = "JO62QM").apply { qrb = 800.0 }
        state.stationFilter.minQrbKm = 300.0
        state.stationFilter.minQrbEnabled = true
        state.stations.rowFilter = { state.stationFilter.acceptsQrb(it.qrb) }

        show(state, listOf(moving, stay), mapLookingAt("JO50JP", zoom = 12f))
        onNodeWithText("Showing 2 visible stations | filtered view active").assertExists()

        runOnIdle {
            moving.qrb = 100.0
            state.stations.forceRedraw()
        }
        waitForIdle()

        onNodeWithText("Showing 1 visible stations | filtered view active").assertExists()
    }

    /**
     * A selected station the filters hide has no marker, and so nothing to analyse: no
     * request goes out and the analysis does not sit at "Loading" with an empty target (#21).
     * The visible station beside it is the control: selecting it does ask and does load.
     */
    @Test
    fun aSelectedStationTheFiltersHideGetsNoPathAnalysis() = runComposeUiTest {
        val state = windowState(prefs(pathAnalysisVisible = true))
        val hidden = station("DL1HIDE", 2, locator = "JO50JP").apply { qrb = 100.0 }
        val shown = station("DL2SHOW", 2, locator = "JO62QM").apply { qrb = 800.0 }
        state.stationFilter.minQrbKm = 300.0
        state.stationFilter.minQrbEnabled = true
        state.stations.rowFilter = { state.stationFilter.acceptsQrb(it.qrb) }
        val analysis = RecordingPathAnalysis()
        val mapState = mapLookingAt(ownLocator, zoom = 9f)
        val home = Location(ownLocator)

        show(state, listOf(hidden, shown), mapState, analysis)

        runOnIdle { state.selectedStation.select(hidden) }
        waitForIdle()

        assertEquals(emptyList<ChatMember>(), analysis.requested, "a hidden station was analysed")
        onAllNodesWithText("Loading", substring = true).assertCountEquals(0)
        assertEquals(home.longitude.toDegrees(), mapState.centerLon, 1e-9, "the map panned to a hidden station")

        runOnIdle { state.selectedStation.select(shown) }
        waitForIdle()

        assertEquals(listOf(shown), analysis.requested)
        assertEquals(true, onAllNodesWithText("Loading", substring = true).fetchSemanticsNodes().isNotEmpty())
    }
}
