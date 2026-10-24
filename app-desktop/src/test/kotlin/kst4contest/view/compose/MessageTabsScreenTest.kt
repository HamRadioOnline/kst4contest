package kst4contest.view.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class MessageTabsScreenTest {

    @Test
    fun `tab semantics distinguish the active tab and follow selection`() = runComposeUiTest {
        setContent {
            Kst4ContestTheme(darkMode = true, baseFontSizeSp = 12f) {
                MessageTabs(
                    publicMessages = messageTable(),
                    clusterMessages = clusterTable(),
                    qsoOfTheOther = messageTable(),
                )
            }
        }

        onNodeWithText("Public messages").assertIsSelected()
        onNodeWithText("DXCluster messages").assertIsNotSelected().performClick()
        waitForIdle()
        onNodeWithText("Public messages").assertIsNotSelected()
        onNodeWithText("DXCluster messages").assertIsSelected()
    }

    @Test
    fun `a public message row reports its sender selection source`() = runComposeUiTest {
        val public = messageTable()
        val message = messageFrom("DL1PUBLIC")
        public.replaceRows(listOf(message))
        val clicks = mutableListOf<Pair<ChatMessage, StationSelectionSource>>()

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                MessageTabs(
                    publicMessages = public,
                    clusterMessages = clusterTable(),
                    qsoOfTheOther = messageTable(),
                    onStationMessageClicked = { clicked, source -> clicks += clicked to source },
                )
            }
        }
        waitForIdle()

        onNodeWithText("DL1PUBLIC").performClick()
        waitForIdle()

        assertSame(message, clicks.single().first)
        assertEquals(StationSelectionSource.PUBLIC_MESSAGE, clicks.single().second)
    }

    @Test
    fun `a qso of the other row reports its sender selection source`() = runComposeUiTest {
        val qso = messageTable()
        val message = messageFrom("DL2QSO")
        qso.replaceRows(listOf(message))
        val clicks = mutableListOf<Pair<ChatMessage, StationSelectionSource>>()

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                MessageTabs(
                    publicMessages = messageTable(),
                    clusterMessages = clusterTable(),
                    qsoOfTheOther = qso,
                    onStationMessageClicked = { clicked, source -> clicks += clicked to source },
                )
            }
        }
        waitForIdle()

        onNodeWithText("QSO of the other").performClick()
        waitForIdle()
        onNodeWithText("DL2QSO").performClick()
        waitForIdle()

        assertSame(message, clicks.single().first)
        assertEquals(StationSelectionSource.QSO_OF_THE_OTHER, clicks.single().second)
    }

    private fun messageTable() = DataTableState(
        columns = listOf(
            DataColumn(
                id = "sender",
                title = "Sender",
                value = { message: ChatMessage -> message.sender?.callSign.orEmpty() },
            ),
        ),
        rowKey = RowKeys.byReference<ChatMessage>(),
    )

    private fun clusterTable() = DataTableState(
        columns = emptyList<DataColumn<ClusterMessage>>(),
        rowKey = RowKeys.byReference<ClusterMessage>(),
    )

    private fun messageFrom(callSign: String): ChatMessage = ChatMessage().apply {
        sender = ChatMember().apply {
            this.callSign = callSign
            chatCategory = ChatCategory(2)
        }
        receiver = ChatMember().apply { this.callSign = "ALL" }
        chatCategory = ChatCategory(2)
        messageText = "test"
    }
}
