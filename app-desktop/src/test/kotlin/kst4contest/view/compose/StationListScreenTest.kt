package kst4contest.view.compose

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.IntOffset
import kst4contest.model.AirPlane
import kst4contest.model.AirPlaneReflectionInfo
import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.RecentChatJoin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.EnumSet

/**
 * The station list of the main window, drawn: the operator's main working surface (#107).
 *
 * The same [DataTableView] over [StationColumns] and [StationColumns.rowKey] that
 * `Kst4ContestApplication` builds; only the computed cells (tropo, score, band status) are
 * stubbed, because they come from the controller. Three regressions are why this list is
 * tested on screen and not only as state:
 *
 * - `21251377` (Codex review findings on PR 89): the row key was the callsign alone, so the
 *   same callsign in two chat categories collided -- two rows under one key, which a
 *   `LazyColumn` refuses outright.
 * - `21251377` again: sorting worked on all rows instead of the filtered ones, so choosing a
 *   sort column silently switched the filters off.
 * - `ab282162` (#87): the AP column had no comparator and sorted by its text, so
 *   "13 (75%)" came before "3 (50%)".
 *
 * Plus the rule the table was built around in `cc8c9020` (Draw the whole interface in
 * Compose): rows replaced after the first draw reach the screen, and the selection is kept by
 * key across a refresh that hands in new instances.
 *
 * Headless: only a composition, no window.
 */
@OptIn(ExperimentalTestApi::class)
class StationListScreenTest {

    @Test
    fun anEmptyTableKeepsItsStructureAndExplainsTheEmptyBody() = runComposeUiTest {
        show(stationList())

        onNodeWithText("No entries").assertExists()
        onNodeWithText("QRB").assertExists()
    }

    private fun stationList(): DataTableState<ChatMember> = DataTableState(
        StationColumns.all(
            tropoOf = { "" },
            priorityScoreOf = { 0.0 },
            formatBand = { _, _, _ -> "" },
            activeBands = EnumSet.noneOf(Band::class.java),
        ),
        RowKeys.byValue(StationColumns::rowKey),
        "compose-stations",
    )

    private fun station(
        call: String,
        category: Int = 2,
        qrb: Double? = null,
        planes: List<Pair<Int, Int>>? = null,
    ): ChatMember = ChatMember().apply {
        callSign = call
        name = "op $call"
        chatCategory = ChatCategory(category)
        this.qrb = qrb
        if (planes != null) {
            airPlaneReflectInfo = AirPlaneReflectionInfo().apply {
                risingAirplanes = planes.map { (minutes, potential) ->
                    AirPlane().apply {
                        arrivingDurationMinutes = minutes
                        this.potential = potential
                    }
                }.toMutableList()
            }
        }
    }

    private fun ComposeUiTest.show(
        state: DataTableState<ChatMember>,
        onRowClick: (ChatMember) -> Unit = {},
        onRowContextMenu: ((ChatMember, IntOffset) -> Unit)? = null,
    ) {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                DataTableView(
                    state = state,
                    onRowClick = onRowClick,
                    onRowContextMenu = onRowContextMenu,
                )
            }
        }
        waitForIdle()
    }

    /** The callsigns as they stand on screen, top to bottom. */
    private fun ComposeUiTest.drawnOrder(vararg calls: String): List<String> =
        calls.sortedBy { call ->
            onNodeWithText(call).fetchSemanticsNode().positionInRoot.y
        }

    @Test
    fun theSameCallsignInTwoCategoriesIsTwoRowsAndOnlyTheClickedOneIsSelected() = runComposeUiTest {
        val state = stationList()
        val inMain = station("DL1ABC", category = 2)
        val inSecond = station("DL1ABC", category = 3)
        val clicked = mutableListOf<ChatMember>()

        show(state, onRowClick = { clicked += it })
        runOnIdle { state.replaceRows(listOf(inMain, inSecond)) }
        waitForIdle()

        assertEquals(
            2,
            onAllNodesWithText("DL1ABC").fetchSemanticsNodes().size,
            "the second category's row did not reach the screen",
        )

        // Unsorted, the rows keep their arrival order: the second category is the lower row.
        val rows = onAllNodesWithText("DL1ABC")
        val lower = rows.fetchSemanticsNodes().withIndex().maxBy { it.value.positionInRoot.y }.index
        rows[lower].performClick()
        waitForIdle()

        assertSame(inSecond, clicked.single(), "the click reached the other category's row")
        assertSame(inSecond, state.selectedRow)
        assertEquals(false, state.isSelected(inMain), "both categories read as selected")
    }

    @Test
    fun aSortChosenWhileFilteredKeepsTheFilter() = runComposeUiTest {
        val state = stationList()
        show(state)
        runOnIdle {
            state.replaceRows(
                listOf(
                    station("DL3CCC", qrb = 300.0),
                    station("DL1AAA", qrb = 100.0),
                    station("DL2BBB", qrb = 200.0),
                ),
            )
            state.rowFilter = { it.callSign != "DL2BBB" }
        }
        waitForIdle()
        onNodeWithText("DL2BBB").assertDoesNotExist()

        onNodeWithText("QRB").performClick()
        waitForIdle()

        assertEquals("qrb", state.sortedColumnId, "clicking the header did not select its sort")
        onNodeWithText("DL2BBB").assertDoesNotExist()
        assertEquals(listOf("DL1AAA", "DL3CCC"), drawnOrder("DL3CCC", "DL1AAA"))
    }

    @Test
    fun theApColumnSortsByMinutesAndNotByItsText() = runComposeUiTest {
        val state = stationList()
        show(state)
        runOnIdle {
            state.replaceRows(
                listOf(
                    station("DL1THIRTEEN", planes = listOf(13 to 75)),
                    station("DL1NONE", planes = emptyList()),
                    station("DL1THREE", planes = listOf(3 to 50)),
                ),
            )
        }
        waitForIdle()

        onNodeWithText("AP [minutes / pot%]").performClick()
        waitForIdle()

        assertEquals("airscout", state.sortedColumnId, "clicking the header did not select its sort")
        assertEquals(
            listOf("DL1THREE", "DL1THIRTEEN", "DL1NONE"),
            drawnOrder("DL1THIRTEEN", "DL1NONE", "DL1THREE"),
            "the text order puts \"13 (75%)\" above \"3 (50%)\"",
        )
        onNodeWithText("AP [minutes / pot%] ▲").assertExists()
    }

    @Test
    fun rowsArrivingAfterTheFirstDrawReachTheScreenAndKeepTheSelection() = runComposeUiTest {
        val state = stationList()
        show(state)

        runOnIdle { state.replaceRows(listOf(station("DL1AAA"), station("DL2BBB"))) }
        waitForIdle()
        onNodeWithText("DL2BBB").performClick()
        waitForIdle()

        // A refresh from the roster hands in new instances, with one more station on top.
        val refreshed = station("DL2BBB")
        runOnIdle { state.replaceRows(listOf(station("DL9NEW"), station("DL1AAA"), refreshed)) }
        waitForIdle()

        onNodeWithText("DL9NEW").assertExists()
        assertSame(refreshed, state.selectedRow, "the selection did not follow the station by key")

        // A station that left takes the selection with it rather than handing it on.
        runOnIdle { state.replaceRows(listOf(station("DL9NEW"), station("DL1AAA"))) }
        waitForIdle()

        onNodeWithText("DL2BBB").assertDoesNotExist()
        assertEquals(null, state.selectedRow)
    }

    @Test
    fun aSecondaryClickReportsTheExactStationAndWindowPosition() = runComposeUiTest {
        val state = stationList()
        val member = station("DL1ABC", category = 3)
        state.replaceRows(listOf(member))
        val requests = mutableListOf<Pair<ChatMember, IntOffset>>()

        show(state, onRowContextMenu = { target, position -> requests += target to position })
        onNodeWithText("DL1ABC").performMouseInput {
            click(button = MouseButton.Secondary)
        }
        waitForIdle()

        assertEquals(1, requests.size)
        assertSame(member, requests.single().first)
        assertTrue(requests.single().second.x >= 0 && requests.single().second.y >= 0)
    }

    @Test
    fun anExplicitSelectionScrollsUsingTheCurrentSortedPosition() = runComposeUiTest {
        val selection = StationSelectionState()
        val state = DataTableState(
            StationColumns.all(
                tropoOf = { "" },
                priorityScoreOf = { 0.0 },
                formatBand = { _, _, _ -> "" },
                activeBands = EnumSet.noneOf(Band::class.java),
            ),
            RowKeys.byValue(StationColumns::rowKey),
            initialSortColumnId = "callsign",
            selectionModel = selection,
            keepSelectionVisible = true,
        )
        val stations = (1..60).map { number -> station("DL${number.toString().padStart(2, '0')}TEST") }
        state.replaceRows(stations.reversed())

        show(state)
        onNodeWithText("DL60TEST").assertDoesNotExist()

        runOnIdle { selection.select(stations.last(), StationSelectionSource.PUBLIC_MESSAGE) }
        waitForIdle()

        onNodeWithText("DL60TEST").assertExists()
        assertSame(stations.last(), state.selectedRow)
    }

    /**
     * A station's fields change in place and the application calls forceRedraw: the list
     * filters again, so a station the filter now excludes leaves the screen (#21).
     */
    @Test
    fun aRedrawAfterAFieldChangedFiltersTheListAgain() = runComposeUiTest {
        val state = stationList()
        val moving = station("DL1MOVE", qrb = 500.0)
        show(state)
        runOnIdle {
            state.replaceRows(listOf(moving, station("DL2STAY", qrb = 800.0)))
            state.rowFilter = { (it.qrb ?: 0.0) > 300.0 }
        }
        waitForIdle()
        onNodeWithText("DL1MOVE").assertExists()

        runOnIdle {
            moving.qrb = 100.0
            state.forceRedraw()
        }
        waitForIdle()

        onNodeWithText("DL1MOVE").assertDoesNotExist()
        onNodeWithText("DL2STAY").assertExists()
    }

    @Test
    fun onlyTheCallsignCellOfARecentlyJoinedStationIsEmphasized() = runComposeUiTest {
        val state = stationList()
        val joined = station("HB9SJV", 2)
        val existing = station("DL1OLD", 2)
        val recent = NewJoinedState(clock = { 5_000L }, onSelect = {}).apply {
            updateActiveMembers(listOf(joined, existing))
            updateEvents(listOf(RecentChatJoin("HB9SJV", 2, 1_000L)))
        }
        val emphasized = mutableSetOf<Pair<String, String>>()
        state.replaceRows(listOf(joined, existing))

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                DataTableView(
                    state = state,
                    emphasizeCell = { member, column ->
                        val emphasize = column.id == "callsign" && recent.isRecent(member)
                        if (emphasize) emphasized += member.callSign to column.id
                        emphasize
                    },
                )
            }
        }
        waitForIdle()

        assertEquals(setOf("HB9SJV" to "callsign"), emphasized)
    }
}
