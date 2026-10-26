package kst4contest.view.compose.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import kst4contest.view.compose.onEventThread
import kst4contest.view.map.MapCallsignRawSnapshot
import kst4contest.view.map.TileFetcher
import org.junit.jupiter.api.Test

/**
 * The map canvas has to survive every zoom and every canvas size.
 *
 * Markers carry labels, and a label is measured against the canvas it is drawn on. A
 * label whose anchor has drifted outside the canvas makes Compose build text
 * constraints from a negative remaining width, which is not a Constraints value that
 * exists. Fractional zoom levels are included deliberately: the canvas scales its whole
 * drawing by the fraction, so sizes that are fine at zoom 6 are not at zoom 6.5.
 */
class ComposeStationMapLayoutTest {

    private fun station(call: String, lat: Double, lon: Double, selected: Boolean = false) =
        MapCallsignRawSnapshot(
            call, call, "JO50JP", lat, lon, "144, 432", emptyMap(),
            true, false, false, selected, 100.0, 90.0, 0, 0L,
        )

    private val stations = listOf(
        station("DN9APW", 50.5, 10.5),
        station("DL3JIN", 50.9, 11.9),
        station("F5JMI", 44.0, 5.0, selected = true),
        station("SM7KOJ", 60.0, 15.0),
    )

    private fun renderAt(widthPx: Int, heightPx: Int, zoom: Float) {
        onEventThread {
            val scene = ImageComposeScene(
                width = widthPx,
                height = heightPx,
                density = Density(1f),
            ) {
                ComposeStationMap(
                    markers = stations,
                    tileFetcher = TileFetcher(),
                    mapState = MapState(zoom = zoom, centerLon = 10.0, centerLat = 51.0),
                    groupingEnabled = true,
                    selectedMarkerCallsign = "F5JMI",
                    ownLocator6 = "JO50JP",
                    antennaAzimuthDeg = 229.0,
                    antennaBeamWidthDeg = 60.0,
                    maxQrbKm = 800.0,
                    onMarkerClicked = null,
                    darkMode = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            try {
                scene.render()
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun `renders at a normal window size and integer zoom`() = renderAt(900, 600, 6f)

    @Test
    fun `renders at a fractional zoom`() = renderAt(900, 600, 6.5f)

    @Test
    fun `renders when the canvas is narrow`() = renderAt(40, 600, 6.5f)

    @Test
    fun `renders when the canvas is short`() = renderAt(900, 30, 6.5f)

    @Test
    fun `renders at the highest zoom`() = renderAt(900, 600, 17.5f)

    @Test
    fun `renders at one pixel`() = renderAt(1, 1, 6.5f)
}
