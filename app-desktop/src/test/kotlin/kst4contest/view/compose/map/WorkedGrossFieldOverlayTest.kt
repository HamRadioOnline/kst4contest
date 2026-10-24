package kst4contest.view.compose.map

import kst4contest.view.map.MapViewport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkedGrossFieldOverlayTest {

    @Test
    fun `worked cells are planned from persistent gross fields without station markers`() {
        val viewport = MapViewport(10.0, 51.0, 6.0, 900.0, 600.0)

        val cells = visibleWorkedGrossFieldCells(viewport, setOf("JO51", "AA00"))

        assertEquals(listOf("JO51"), cells.map { it.locatorLabel() })
    }

    @Test
    fun `disabled grid colour produces no worked overlay cells`() {
        val viewport = MapViewport(10.0, 51.0, 6.0, 900.0, 600.0)

        assertTrue(visibleWorkedGrossFieldCells(viewport, emptySet()).isEmpty())
    }
}
