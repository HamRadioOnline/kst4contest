package kst4contest.view.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The cluster and QSO monitor's body, drawn with live tables (#107).
 *
 * This was the first window whose tables fill while the operator watches, and the defects
 * that the Compose port shipped here were all about the coupling between a live list and
 * what gets drawn, not about the table itself (`cc8c9020`, Draw the whole interface in
 * Compose; in `main` as `2a5c89b9`):
 *
 * - The monitor sat empty beside a filling JavaFX tab and only redrew when an unrelated
 *   click forced it: `DataTableState.visibleRows` returned its cache before reading the row
 *   state, so a recomposition that hit the cache left the table subscribed to nothing.
 * - Rows without identity were keyed in a way that could hand out the same key twice, which
 *   a `LazyColumn` refuses; two messages that read the same are still two messages.
 *
 * The tables here are built exactly as `Kst4ContestApplication` builds them: the monitor
 * columns, keyed by reference.
 *
 * Headless: [MonitorContent] is drawn on its own, no window is opened.
 */
@OptIn(ExperimentalTestApi::class)
class MonitorWindowScreenTest {

    private fun clusterTable() =
        DataTableState(MonitorColumns.dxCluster(), RowKeys.byReference<ClusterMessage>(), "dx-cluster-monitor")

    private fun qsoTable() =
        DataTableState(MonitorColumns.qsoOfTheOther(), RowKeys.byReference<ChatMessage>(), "qso-other-monitor")

    private fun member(call: String) = ChatMember().apply { callSign = call }

    private fun spot(from: String, to: String, text: String) = ClusterMessage().apply {
        sender = member(from)
        receiver = member(to)
        messageInhibited = text
    }

    private fun qso(from: String, to: String, text: String) = ChatMessage().apply {
        sender = member(from)
        receiver = member(to)
        messageText = text
    }

    private fun ComposeUiTest.show(
        cluster: DataTableState<ClusterMessage>,
        qsos: DataTableState<ChatMessage>,
        splitter: SplitterState = SplitterState(paneCount = 2, stored = doubleArrayOf(0.5)) {},
    ) {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                Box(Modifier.size(600.dp, 400.dp)) {
                    MonitorContent(cluster, qsos, splitter)
                }
            }
        }
        waitForIdle()
    }

    @Test
    fun bothTablesAreDrawnUnderTheirHeadings() = runComposeUiTest {
        show(clusterTable(), qsoTable())

        onNodeWithText("DX cluster").assertExists()
        onNodeWithText("QSO of the other").assertExists()
        onNodeWithText("Call tx").assertExists()
        onNodeWithText("Call TX").assertExists()
    }

    /**
     * The two tables used to get half the height each, fixed, and the stored position the
     * JavaFX SplitPane had written was read by nobody (#144). The QSO heading is the top of
     * the lower pane, so where it sits says where the divider is.
     */
    @Test
    fun theStoredDividerPositionSplitsTheWindow() = runComposeUiTest {
        show(clusterTable(), qsoTable(), SplitterState(paneCount = 2, stored = doubleArrayOf(0.8)) {})

        val window = onRoot().getBoundsInRoot()
        val qsoHeadingTop = onNodeWithText("QSO of the other").getBoundsInRoot().top
        val share = (qsoHeadingTop - window.top) / (window.bottom - window.top)

        assertTrue(share in 0.75f..0.85f, "QSO pane starts at $share of the height, expected about 0.8")
    }

    @Test
    fun messagesArrivingWhileTheWindowIsOpenReachBothTables() = runComposeUiTest {
        val cluster = clusterTable()
        val qsos = qsoTable()
        show(cluster, qsos)

        runOnIdle {
            cluster.replaceRows(listOf(spot("DL1AAA", "OK1BBB", "144.300 tnx qso")))
            qsos.replaceRows(listOf(qso("DL2CCC", "OK2DDD", "pse sked 432")))
        }
        waitForIdle()

        onNodeWithText("144.300 tnx qso").assertExists()
        onNodeWithText("pse sked 432").assertExists()
    }

    /**
     * The empty-monitor fault in its exact shape: the table recomposes once for a reason that
     * is not its rows -- here a column moved by the operator -- and the rows that arrive after
     * that must still come through.
     */
    @Test
    fun rowsStillArriveAfterARedrawThatWasNotAboutTheRows() = runComposeUiTest {
        val cluster = clusterTable()
        show(cluster, qsoTable())

        runOnIdle { cluster.replaceRows(listOf(spot("DL1AAA", "OK1BBB", "first spot"))) }
        waitForIdle()
        onNodeWithText("first spot").assertExists()

        // A redraw with the order cache warm.
        runOnIdle { cluster.moveColumn("message", 0) }
        waitForIdle()

        runOnIdle {
            cluster.replaceRows(
                listOf(spot("DL3EEE", "OK3FFF", "second spot")) + cluster.rows,
            )
        }
        waitForIdle()

        onNodeWithText("second spot").assertExists()
        onNodeWithText("first spot").assertExists()
    }

    /** Two spots that read alike are two rows; neither is dropped and the list does not throw. */
    @Test
    fun twoIdenticalLookingMessagesAreTwoRows() = runComposeUiTest {
        val cluster = clusterTable()
        show(cluster, qsoTable())

        runOnIdle {
            cluster.replaceRows(
                listOf(
                    spot("DL1AAA", "OK1BBB", "repeated spot"),
                    spot("DL1AAA", "OK1BBB", "repeated spot"),
                ),
            )
        }
        waitForIdle()

        assertEquals(2, onAllNodesWithText("repeated spot").fetchSemanticsNodes().size)
    }

    /** A half-filled message off the wire draws as empty cells, not as a crash. */
    @Test
    fun aMessageWithoutSenderOrReceiverStillDraws() = runComposeUiTest {
        val cluster = clusterTable()
        val qsos = qsoTable()
        show(cluster, qsos)

        runOnIdle {
            cluster.replaceRows(listOf(ClusterMessage().apply { messageInhibited = "orphan spot" }))
            qsos.replaceRows(listOf(ChatMessage().apply { messageText = "orphan qso" }))
        }
        waitForIdle()

        onNodeWithText("orphan spot").assertExists()
        onNodeWithText("orphan qso").assertExists()
    }
}
