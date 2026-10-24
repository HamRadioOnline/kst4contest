package kst4contest.view.compose

import kst4contest.model.Band
import kst4contest.model.ChatMember
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.EnumSet

class ReachabilityStateTest {

    @Test
    fun `auto and only locally active bands are offered`() {
        val state = ReachabilityState(EnumSet.of(Band.B_144, Band.B_1296))

        assertEquals(listOf(null, Band.B_144, Band.B_1296), state.options.map { it.band })
        assertNull(state.bandOverride)
        assertEquals("Auto", state.selectedOption.label)
    }

    @Test
    fun `band changes redraw but never calculate`() {
        val station = ChatMember().apply { callSign = "DL1ABC" }
        val calculated = mutableListOf<Pair<ChatMember, Band>>()
        var redraws = 0
        val state = ReachabilityState(
            activeBands = EnumSet.of(Band.B_144, Band.B_432),
            resolveAutoBand = { Band.B_144 },
            selectedStation = { station },
            calculate = { member, band -> calculated += member to band },
            refresh = { redraws++ },
        )

        state.selectBand(Band.B_432)

        assertEquals(Band.B_432, state.resolveBand(station))
        assertEquals(1, redraws)
        assertTrue(calculated.isEmpty())

        assertTrue(state.calculateSelected())
        assertEquals(listOf(station to Band.B_432), calculated)
    }

    @Test
    fun `auto uses shared resolver and unavailable selections do not calculate`() {
        var selected: ChatMember? = ChatMember()
        var calculations = 0
        val state = ReachabilityState(
            activeBands = EnumSet.of(Band.B_144),
            resolveAutoBand = { null },
            selectedStation = { selected },
            calculate = { _, _ -> calculations++ },
        )

        assertFalse(state.canCalculate)
        assertFalse(state.calculateSelected())
        selected = null
        assertFalse(state.calculateSelected())
        assertEquals(0, calculations)
    }
}
