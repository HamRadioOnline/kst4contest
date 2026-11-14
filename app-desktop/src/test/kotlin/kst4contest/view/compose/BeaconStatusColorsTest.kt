package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BeaconStatusColorsTest {
    private val neutral = Color(0xFF123456)

    @Test
    fun `active and activity colours are the approved traffic colours`() {
        assertEquals(
            ThreadButtonColors(Color(0xFF238636), Color(0xFF56D364), Color.White),
            beaconThreadButtonColors(ThreadStatusVisualStyle.BEACON_ACTIVE, false, false, neutral),
        )
        assertEquals(
            ThreadButtonColors(Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFF1B1B1B)),
            beaconThreadButtonColors(ThreadStatusVisualStyle.BEACON_ACTIVE, true, false, neutral),
        )
    }

    @Test
    fun `off uses neutral background and theme specific red text`() {
        assertEquals(Color(0xFFD50000),
            beaconThreadButtonColors(ThreadStatusVisualStyle.BEACON_OFF, false, false, neutral).text)
        assertEquals(Color(0xFFFF6B6B),
            beaconThreadButtonColors(ThreadStatusVisualStyle.BEACON_OFF, false, true, neutral).text)
        assertEquals(neutral,
            beaconThreadButtonColors(ThreadStatusVisualStyle.BEACON_OFF, false, true, neutral).background)
    }

    @Test
    fun `failed stays red even when a highlight is requested`() {
        val colors = beaconThreadButtonColors(
            ThreadStatusVisualStyle.BEACON_FAILED,
            true,
            false,
            neutral,
        )
        assertEquals(ThreadButtonColors(Color(0xFFD50000), Color(0xFFFF6B6B), Color.White), colors)
    }

    @Test
    fun `failed suppresses the flash while active status allows it`() {
        val button = ThreadStatusButton("MyBeacon")
        button.report("MyBeacon: on", "active", ThreadStatusVisualStyle.BEACON_ACTIVE)
        assertTrue(button.highlighted)

        button.report("MyBeacon: FAILED", "failed", ThreadStatusVisualStyle.BEACON_FAILED)
        assertFalse(button.highlighted)
        assertEquals(
            ThreadButtonColors(Color(0xFFD50000), Color(0xFFFF6B6B), Color.White),
            beaconThreadButtonColors(ThreadStatusVisualStyle.BEACON_FAILED, true, false, neutral),
        )
    }
}
