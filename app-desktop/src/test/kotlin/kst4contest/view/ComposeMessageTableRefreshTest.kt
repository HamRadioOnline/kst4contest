package kst4contest.view

import kst4contest.controller.ChatController
import kst4contest.model.ChatMessage
import kst4contest.view.compose.DataTableState
import kst4contest.view.compose.RowKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ComposeMessageTableRefreshTest {

    @Test
    fun `refresh keeps public table on the filtered controller view`() {
        val controller = mock(ChatController::class.java)
        val privateToMe = message("private to me")
        val publicInitial = message("public initial")
        val betweenOthers = message("between others")
        val directed = table()
        val publicMessages = table()
        val qsoOfTheOther = table()

        `when`(controller.toMeMessages()).thenReturn(listOf(privateToMe))
        `when`(controller.toAllMessages()).thenReturn(listOf(publicInitial))
        `when`(controller.toOtherMessages()).thenReturn(listOf(betweenOthers))

        Kst4ContestApplication.refreshComposeMessageTables(
            controller,
            directed,
            publicMessages,
            qsoOfTheOther,
        )

        assertEquals(listOf(privateToMe), directed.rows)
        assertEquals(listOf(publicInitial), publicMessages.rows)
        assertEquals(listOf(betweenOthers), qsoOfTheOther.rows)

        val publicLater = message("public later")
        `when`(controller.toAllMessages()).thenReturn(listOf(publicLater, publicInitial))

        Kst4ContestApplication.refreshComposeMessageTables(
            controller,
            directed,
            publicMessages,
            qsoOfTheOther,
        )

        assertEquals(listOf(publicLater, publicInitial), publicMessages.rows)
    }

    private fun table() = DataTableState(
        columns = emptyList(),
        rowKey = RowKeys.byReference<ChatMessage>(),
    )

    private fun message(text: String) = ChatMessage().apply {
        messageText = text
    }
}
