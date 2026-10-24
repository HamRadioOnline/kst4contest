package kst4contest.view.compose

import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.RecentChatJoin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

class NewJoinedStateTest {

    private val now = AtomicLong(1_000_000L)
    private var selected: ChatMember? = null
    private val state = NewJoinedState(clock = now::get, onSelect = { selected = it })

    @Test
    fun `an initial active roster without live events produces no entries`() {
        state.updateActiveMembers(listOf(member("HB9SJV", 2)))

        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `a live join is shown once and duplicate events do not renew it`() {
        val station = member("HB9SJV", 2)
        state.updateActiveMembers(listOf(station))
        state.updateEvents(
            listOf(
                event("HB9SJV", 2, now.get() - 2_000L),
                event("HB9SJV", 2, now.get() - 1_000L),
            ),
        )

        assertEquals(listOf("HB9SJV"), state.entries.map { it.label })
        assertEquals(now.get() - 2_000L, state.entries.single().joinedAtEpochMillis)
    }

    @Test
    fun `the same full callsign in two categories stays distinguishable`() {
        val main = member("DL1KDA", 2)
        val microwave = member("DL1KDA", 3)
        state.updateActiveMembers(listOf(main, microwave))
        state.updateEvents(
            listOf(
                event("DL1KDA", 3, now.get()),
                event("DL1KDA", 2, now.get() - 1L),
            ),
        )

        assertEquals(listOf(3, 2), state.entries.map { it.identity.categoryNumber })
        state.entries.first().select()
        assertSame(microwave, selected)
    }

    @Test
    fun `an entry expires exactly ten minutes after the join`() {
        val station = member("OE5SOS", 2)
        state.updateActiveMembers(listOf(station))
        state.updateEvents(listOf(event("OE5SOS", 2, now.get())))

        now.addAndGet(NewJoinedState.VALIDITY_MILLIS - 1L)
        state.refresh()
        assertTrue(state.isRecent(station))

        now.incrementAndGet()
        state.refresh()
        assertFalse(state.isRecent(station))
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `only five newest entries are direct and the rest remain in the overflow`() {
        val members = (1..7).map { member("DL${it}ABC", 2) }
        state.updateActiveMembers(members)
        state.updateEvents(
            members.mapIndexed { index, member ->
                event(member.callSign, 2, now.get() - (members.size - index))
            },
        )

        assertEquals(listOf("DL7ABC", "DL6ABC", "DL5ABC", "DL4ABC", "DL3ABC"),
            state.visibleEntries.map { it.label })
        assertEquals(listOf("DL2ABC", "DL1ABC"), state.overflowEntries.map { it.label })
    }

    @Test
    fun `a station leaving disappears and a real rejoin gets the new event`() {
        val first = member("PA2M", 2)
        state.updateActiveMembers(listOf(first))
        state.updateEvents(listOf(event("PA2M", 2, now.get())))

        state.updateActiveMembers(emptyList())
        assertTrue(state.entries.isEmpty())

        val rejoined = member("PA2M", 2)
        now.addAndGet(5_000L)
        state.updateActiveMembers(listOf(rejoined))
        state.updateEvents(listOf(event("PA2M", 2, now.get())))

        state.entries.single().select()
        assertSame(rejoined, selected)
        assertEquals(now.get(), state.entries.single().joinedAtEpochMillis)
    }

    private fun member(callSign: String, categoryNumber: Int) = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(categoryNumber)
    }

    private fun event(callSign: String, categoryNumber: Int, joinedAt: Long) =
        RecentChatJoin(callSign, categoryNumber, joinedAt)
}
