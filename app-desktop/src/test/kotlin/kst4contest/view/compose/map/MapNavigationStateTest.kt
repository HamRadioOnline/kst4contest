package kst4contest.view.compose.map

import kst4contest.view.compose.StationIdentity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapNavigationStateTest {

    @Test
    fun `repeated station choices remain observable`() {
        val state = MapNavigationState()
        val identity = StationIdentity("DL1ABC", 2)

        state.fitSelectedPath(identity)
        val firstRevision = state.requestRevision
        state.fitSelectedPath(identity)

        assertTrue(state.requestRevision > firstRevision)
        assertEquals(MapNavigationRequest.FitSelectedPath(identity), state.request)
    }

    @Test
    fun `latest explicit action replaces an older one`() {
        val state = MapNavigationState()

        state.fitSelectedPath(StationIdentity("DL1ABC", 2))
        state.centerOnQtf(45.0)

        assertEquals(MapNavigationRequest.CenterOnQtf(45.0), state.request)
    }

    @Test
    fun `invalid QTF values do not create requests`() {
        val state = MapNavigationState()

        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 360.1).forEach(state::centerOnQtf)

        assertEquals(0L, state.requestRevision)
        assertEquals(null, state.request)
    }
}
