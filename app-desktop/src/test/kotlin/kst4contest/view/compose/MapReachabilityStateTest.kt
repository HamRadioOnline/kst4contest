package kst4contest.view.compose

import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.view.map.PathAnalysisResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapReachabilityStateTest {

    @Test
    fun `latest station and band request rejects stale results`() {
        val callbacks = mutableListOf<(PathAnalysisResult) -> Unit>()
        val state = MapReachabilityState("JO51AA") { _, _, callback -> callbacks += callback }
        val first = member("DL1ABC", 2, "JN49AA")
        val second = member("DL2XYZ", 3, "JO60AA")

        assertTrue(state.calculate(first, Band.B_144))
        assertEquals("Loading", state.result?.analysisMode())
        assertTrue(state.calculate(second, Band.B_432))

        callbacks[1](result("DL2XYZ", 432.0))
        callbacks[0](result("DL1ABC", 144.0))

        assertEquals("DL2XYZ", state.result?.toCallsignRaw())
        assertEquals(432.0, state.result?.analysisFrequencyMHz())
        assertFalse(state.running)
    }

    @Test
    fun `selecting another station invalidates an outstanding explicit result`() {
        lateinit var callback: (PathAnalysisResult) -> Unit
        val state = MapReachabilityState("JO51AA") { _, _, result -> callback = result }
        val first = member("DL1ABC", 2, "JN49AA")

        state.calculate(first, Band.B_144)
        state.selectionChanged(StationIdentity("DL2XYZ", 2))
        callback(result("DL1ABC", 144.0))

        assertNull(state.result)
        assertFalse(state.running)
    }

    @Test
    fun `missing station or band does not start a request`() {
        val state = MapReachabilityState("JO51AA") { _, _, _ -> error("must not run") }

        assertFalse(state.calculate(null, Band.B_144))
        assertFalse(state.calculate(member("DL1ABC", 2, "JN49AA"), null))
    }

    private fun member(callSign: String, category: Int, locator: String) = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(category)
        qra = locator
    }

    private fun result(callSign: String, frequency: Double) = PathAnalysisResult.noProfile(
        "Calculated", "JO51AA", "JN49AA", callSign,
        100.0, 180.0, 10.0, 10.0, frequency, "done",
    )
}
