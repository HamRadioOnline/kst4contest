package kst4contest.view.compose.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.SplitterOrientation
import kst4contest.view.compose.SplitterPane
import kst4contest.view.compose.SplitterState
import kst4contest.view.compose.onEventThread
import kst4contest.view.map.MapCallsignRawSnapshot
import kst4contest.view.map.PathAnalysisResult
import kst4contest.view.map.TileFetcher
import org.junit.jupiter.api.Test

/**
 * The map window's body, assembled the way the window assembles it.
 *
 * Each part of this body renders on its own at one pixel. The window still put up a
 * Constraints error dialog, and the answer was a barrier that leaves the window blank
 * below 500x400 dp — so the combination is what was never checked. The splitter hands
 * its panes a width it computed, the left pane stacks a weighted canvas above a fixed
 * 210 dp chart, and the right pane scrolls. That arrangement is this test.
 */
class StationMapBodyLayoutTest {

    private val stations = listOf(
        MapCallsignRawSnapshot(
            "DN9APW", "DN9APW", "JO50JP", 50.5, 10.5, "144, 432", emptyMap(),
            true, false, false, false, 100.0, 90.0, 0, 0L,
        ),
    )

    @Composable
    private fun Body(pathAnalysisVisible: Boolean) {
        val splitter = SplitterState(paneCount = 2, stored = doubleArrayOf(0.65), save = { })

        val leftPane: @Composable () -> Unit = {
            Column(Modifier.fillMaxHeight()) {
                ComposeStationMap(
                    markers = stations,
                    tileFetcher = TileFetcher(),
                    mapState = MapState(zoom = 6.5f, centerLon = 10.0, centerLat = 51.0),
                    groupingEnabled = true,
                    selectedMarkerCallsign = null,
                    ownLocator6 = "JO50JP",
                    antennaAzimuthDeg = 229.0,
                    antennaBeamWidthDeg = 60.0,
                    maxQrbKm = 800.0,
                    onMarkerClicked = null,
                    darkMode = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                if (pathAnalysisVisible) {
                    PathProfileChart(
                        modifier = Modifier.fillMaxWidth().height(210.dp),
                        darkMode = true,
                    )
                }
            }
        }

        val rightPane: @Composable () -> Unit = {
            PathAnalysisDetails(
                result = PathAnalysisResult.loading("JO50JP", "JN24JB", "F5JMI"),
                darkMode = true,
                modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
            )
        }

        if (pathAnalysisVisible) {
            SplitterPane(
                state = splitter,
                orientation = SplitterOrientation.HORIZONTAL,
                panes = listOf(leftPane, rightPane),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            leftPane()
        }
    }

    private fun renderAt(widthPx: Int, heightPx: Int, pathAnalysisVisible: Boolean = true) {
        onEventThread {
            val scene = ImageComposeScene(width = widthPx, height = heightPx, density = Density(1f)) {
                Body(pathAnalysisVisible)
            }
            try {
                scene.render()
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun `renders at the window's minimum size`() = renderAt(900, 600)

    /** Below the barrier that commit e7c8a9b9 installed. */
    @Test
    fun `renders below the barrier that hid the defect`() = renderAt(499, 399)

    @Test
    fun `renders when shorter than the chart it stacks`() = renderAt(900, 150)

    @Test
    fun `renders when the detail pane is a sliver`() = renderAt(120, 400)

    @Test
    fun `renders without the path analysis`() = renderAt(300, 200, pathAnalysisVisible = false)

    @Test
    fun `renders at one pixel`() = renderAt(1, 1)
}
