package kst4contest.view.compose.map

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpatialFilterOverlayStateTest {

    @Test
    fun `maximum QRB is inclusive and minimum QRB is exclusive`() {
        val state = overlay(minEnabled = true, minKm = 200.0, maxEnabled = true, maxKm = 500.0)

        assertFalse(state.accepts(200.0, 90.0))
        assertTrue(state.accepts(200.1, 90.0))
        assertTrue(state.accepts(500.0, 90.0))
        assertFalse(state.accepts(500.1, 90.0))
    }

    @Test
    fun `QTF uses the same wrapped beamwidth semantics as the station filter`() {
        val state = overlay(qtf = true, direction = 0.0, beamWidth = 60.0)

        assertTrue(state.accepts(300.0, 350.0))
        assertTrue(state.accepts(300.0, 30.0))
        assertFalse(state.accepts(300.0, 31.0))
    }

    @Test
    fun `unusual full-circle beamwidth retains the existing station filter semantics`() {
        val state = overlay(qtf = true, direction = 0.0, beamWidth = 360.0)

        assertTrue(state.accepts(300.0, 180.0))
        assertFalse(state.accepts(300.0, 0.0))
    }

    @Test
    fun `minimum above maximum is an empty distance range`() {
        val state = overlay(minEnabled = true, minKm = 500.0, maxEnabled = true, maxKm = 200.0)

        assertTrue(state.hasEmptyDistanceRange)
        assertFalse(state.accepts(100.0, 0.0))
        assertFalse(state.accepts(300.0, 0.0))
        assertFalse(state.accepts(600.0, 0.0))
    }

    @Test
    fun `non-finite active values cannot be rendered as a misleading area`() {
        assertFalse(overlay(qtf = true, direction = Double.NaN).hasRenderableValues)
        assertFalse(overlay(minEnabled = true, minKm = Double.POSITIVE_INFINITY).hasRenderableValues)
        assertFalse(overlay(maxEnabled = true, maxKm = Double.NaN).hasRenderableValues)
    }

    @Test
    fun `zero distance boundaries report only actually drawable geometry`() {
        assertFalse(overlay(maxEnabled = true, maxKm = 0.0).hasDrawableGeometry)
        assertFalse(overlay(qtf = true, maxEnabled = true, maxKm = 0.0).hasDrawableGeometry)
        assertFalse(
            overlay(minEnabled = true, minKm = 0.0, maxEnabled = true, maxKm = 0.0).hasDrawableGeometry
        )
        assertTrue(overlay(minEnabled = true, minKm = 0.0).hasDrawableGeometry)
        assertTrue(overlay(qtf = true).hasDrawableGeometry)
        assertTrue(overlay(minEnabled = true, minKm = 100.0, maxEnabled = true, maxKm = 0.0).hasDrawableGeometry)
        assertTrue(overlay(maxEnabled = true, maxKm = 500.0).hasDrawableGeometry)
    }

    @Test
    fun `no spatial filter accepts finite positions`() {
        val state = overlay()

        assertFalse(state.isActive)
        assertTrue(state.accepts(1000.0, 275.0))
    }

    private fun overlay(
        qtf: Boolean = false,
        direction: Double = 0.0,
        beamWidth: Double = 60.0,
        minEnabled: Boolean = false,
        minKm: Double = 0.0,
        maxEnabled: Boolean = false,
        maxKm: Double = 0.0,
    ) = SpatialFilterOverlayState(
        qtfEnabled = qtf,
        qtfDegrees = direction,
        beamWidthDeg = beamWidth,
        minQrbEnabled = minEnabled,
        minQrbKm = minKm,
        maxQrbEnabled = maxEnabled,
        maxQrbKm = maxKm,
    )
}
