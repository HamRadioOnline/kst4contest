package kst4contest.view.compose

import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StationSelectionStateTest {

    @Test
    fun `identity includes the full callsign and category`() {
        val main = station("DN9APW-2", 2)
        val second = station("DN9APW-2", 3)

        assertNotEquals(StationIdentity.of(main), StationIdentity.of(second))
        assertEquals(2, StationIdentity.of(main)?.categoryNumber)
        assertEquals("DN9APW-2", StationIdentity.of(main)?.callSign)
    }

    @Test
    fun `selecting the same station again creates another scroll request`() {
        val state = StationSelectionState()
        val station = station("DL1ABC", 2)

        state.select(station, StationSelectionSource.PUBLIC_MESSAGE)
        val first = state.scrollRequestRevision
        state.select(station, StationSelectionSource.MAP)

        assertSame(station, state.selected)
        assertTrue(state.scrollRequestRevision > first)
        assertEquals(StationSelectionSource.MAP, state.lastSource)
    }

    @Test
    fun `direct station table selections do not request scrolling`() {
        val state = StationSelectionState()

        state.select(station("DL1ABC", 2), StationSelectionSource.STATION_TABLE)

        assertEquals(0L, state.scrollRequestRevision)
    }

    @Test
    fun `station table context selection does not request scrolling`() {
        val state = StationSelectionState()

        state.select(station("DL1ABC", 2), StationSelectionSource.STATION_CONTEXT_MENU)

        assertEquals(0L, state.scrollRequestRevision)
    }

    @Test
    fun `map context selection requests scrolling again for the same station`() {
        val state = StationSelectionState()
        val station = station("DL1ABC", 2)

        state.select(station, StationSelectionSource.MAP_CONTEXT_MENU)
        val first = state.scrollRequestRevision
        state.select(station, StationSelectionSource.MAP_CONTEXT_MENU)

        assertTrue(state.scrollRequestRevision > first)
    }

    @Test
    fun `reconciling a refreshed instance keeps the selection without requesting a scroll`() {
        val state = StationSelectionState()
        state.select(station("DL1ABC", 2), StationSelectionSource.STATION_TABLE)
        val revision = state.scrollRequestRevision
        val refreshed = station("DL1ABC", 2)

        state.reconcile(refreshed)

        assertSame(refreshed, state.selected)
        assertEquals(revision, state.scrollRequestRevision)
    }

    @Test
    fun `incoming directed message targets its sender`() {
        val message = directedMessage(sender = "DL1ABC", receiver = "OWN", category = 2)

        assertEquals(
            StationIdentity("DL1ABC", 2),
            DirectedMessageStationTarget.forMessage(message, "OWN"),
        )
    }

    @Test
    fun `own directed message targets its receiver`() {
        val message = directedMessage(sender = "own", receiver = "DL2XYZ", category = 3)

        assertEquals(
            StationIdentity("DL2XYZ", 3),
            DirectedMessageStationTarget.forMessage(message, "OWN"),
        )
    }

    @Test
    fun `historical own message can obtain its receiver from the payload`() {
        val message = directedMessage(sender = "OWN", receiver = null, category = 2).apply {
            messageText = "(DL3OLD) test"
        }

        assertEquals(
            StationIdentity("DL3OLD", 2),
            DirectedMessageStationTarget.forMessage(message, "OWN"),
        )
    }

    private fun station(callSign: String, category: Int): ChatMember = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(category)
    }

    private fun directedMessage(sender: String, receiver: String?, category: Int) = ChatMessage().apply {
        this.sender = station(sender, category)
        this.receiver = receiver?.let { station(it, category) }
        chatCategory = ChatCategory(category)
        messageText = "test"
    }
}
