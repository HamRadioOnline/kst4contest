package kst4contest.view.compose

import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SelectedStationMessagePredicateTest {

    private val ownCall = "DL0OWN"
    private val selected = member("DL1SEL", 2)

    @Test
    fun `pm to me contains both private directions with the selected station`() {
        val predicate = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.PM_TO_ME,
            selected,
            ownCall,
        )

        assertTrue(predicate.test(message(selected, member(ownCall, 2), 2)))
        assertTrue(predicate.test(message(member(ownCall, 2), selected, 2)))
        assertFalse(predicate.test(message(selected, member("DL2OTHER", 2), 2)))
        assertFalse(predicate.test(publicMessage(selected, 2)))
    }

    @Test
    fun `pm to me also contains an explicitly addressed public message`() {
        val predicate = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.PM_TO_ME,
            selected,
            ownCall,
        )

        assertTrue(predicate.test(publicMessage(selected, 2, addressedToMe = true)))
        assertFalse(predicate.test(publicMessage(selected, 2, addressedToMe = false)))

        val mentionOnly = publicMessage(selected, 2).apply {
            messageText = "Please ask $ownCall whether the path is open"
        }
        assertFalse(predicate.test(mentionOnly))
    }

    @Test
    fun `pm to other contains only private messages sent by the selected station to others`() {
        val other = member("DL2OTHER", 2)
        val predicate = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.PM_TO_OTHER,
            selected,
            ownCall,
        )

        assertTrue(predicate.test(message(selected, other, 2)))
        assertFalse(predicate.test(message(other, selected, 2)))
        assertFalse(predicate.test(message(selected, member(ownCall, 2), 2)))
        assertFalse(predicate.test(publicMessage(selected, 2)))
    }

    @Test
    fun `public contains only public messages sent by the selected station`() {
        val predicate = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.PUBLIC,
            selected,
            ownCall,
        )

        assertTrue(predicate.test(publicMessage(selected, 2)))
        assertTrue(predicate.test(publicMessage(selected, 2, addressedToMe = true)))
        assertFalse(predicate.test(publicMessage(member("DL2OTHER", 2), 2)))
        assertFalse(predicate.test(message(selected, member("DL2OTHER", 2), 2)))
    }

    @Test
    fun `nothing applies no additional message type restriction`() {
        val predicate = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.NOTHING,
            selected,
            ownCall,
        )

        assertTrue(predicate.test(message(selected, member(ownCall, 2), 2)))
        assertTrue(predicate.test(message(member(ownCall, 2), selected, 2)))
        assertTrue(predicate.test(publicMessage(selected, 2)))
        assertFalse(predicate.test(publicMessage(member("DL2OTHER", 2), 2)))
    }

    @Test
    fun `the same callsign in another category does not match`() {
        val predicate = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.PUBLIC,
            selected,
            ownCall,
        )

        assertFalse(predicate.test(publicMessage(member("DL1SEL", 3), 3)))
    }

    @Test
    fun `message category is authoritative when a member snapshot has no category`() {
        val senderWithoutCategory = member("DL1SEL", 2).apply { chatCategory = null }
        val predicate = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.PM_TO_ME,
            selected,
            ownCall,
        )

        assertTrue(predicate.test(message(senderWithoutCategory, member(ownCall, 2), 2)))
    }

    @Test
    fun `missing selection or incomplete historical data fails closed`() {
        val withoutSelection = SelectedStationMessagePredicate.forSelection(
            SelectedMessageFilter.PUBLIC,
            null,
            ownCall,
        )
        val incomplete = ChatMessage().apply { chatCategory = ChatCategory(2) }

        assertFalse(withoutSelection.test(publicMessage(selected, 2)))
        assertFalse(
            SelectedStationMessagePredicate.forSelection(SelectedMessageFilter.PUBLIC, selected, ownCall)
                .test(incomplete),
        )
        assertFalse(
            SelectedStationMessagePredicate.forSelection(
                SelectedMessageFilter.PUBLIC,
                ChatMember().apply { callSign = "DL1SEL" },
                ownCall,
            ).test(publicMessage(selected, 2)),
        )
    }

    private fun message(sender: ChatMember, receiver: ChatMember, category: Int): ChatMessage = ChatMessage().apply {
        this.sender = sender
        this.receiver = receiver
        chatCategory = ChatCategory(category)
        messageText = "test"
    }

    private fun publicMessage(
        sender: ChatMember,
        category: Int,
        addressedToMe: Boolean = false,
    ): ChatMessage = message(sender, member("ALL", category), category).apply {
        isMessageDirectedToCommunity = true
        isMessageDirectedToMe = addressedToMe
    }

    private fun member(callSign: String, category: Int): ChatMember = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(category)
    }
}
