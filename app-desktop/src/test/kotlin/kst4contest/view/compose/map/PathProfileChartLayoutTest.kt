package kst4contest.view.compose.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import kst4contest.view.compose.onEventThread
import org.junit.jupiter.api.Test

/**
 * The terrain profile chart has to survive being small.
 *
 * It is the only part of the map window that draws text onto a canvas whose size it
 * does not control: the splitter above it and the window around it both shrink it. A
 * canvas narrower than the chart's own margins puts the axis labels outside the canvas,
 * and Compose builds the text measurement constraints from the distance between the
 * label and the canvas edge — a distance that is negative once the label is outside.
 */
class PathProfileChartLayoutTest {

    private fun renderAt(widthPx: Int, heightPx: Int) {
        onEventThread {
            val scene = ImageComposeScene(
                width = widthPx,
                height = heightPx,
                density = Density(1f),
            ) {
                PathProfileChart(modifier = Modifier.fillMaxSize(), darkMode = true)
            }
            try {
                scene.render()
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun `renders at the height the window gives it`() = renderAt(800, 210)

    @Test
    fun `renders when narrower than its own left margin`() = renderAt(40, 210)

    @Test
    fun `renders when shorter than its own header`() = renderAt(800, 20)

    @Test
    fun `renders at one pixel`() = renderAt(1, 1)
}
