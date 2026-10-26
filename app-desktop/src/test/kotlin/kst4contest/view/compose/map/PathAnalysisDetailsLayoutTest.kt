package kst4contest.view.compose.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import kst4contest.view.compose.onEventThread
import kst4contest.view.map.PathAnalysisResult
import org.junit.jupiter.api.Test

/**
 * The analysis column has to survive being narrow.
 *
 * The station map splitter can be dragged until this column is a sliver, and the
 * window may be resized below any width the layout assumed. Both happened, and both
 * ended in an IllegalArgumentException out of Constraints rather than a squeezed
 * column — the window then put up an error dialog over the map.
 *
 * Rendering is the assertion: ImageComposeScene measures, lays out and draws, which is
 * exactly where invalid constraints are built. It ships with compose.desktop.currentOs,
 * so this needs no test dependency the project does not already have.
 */
class PathAnalysisDetailsLayoutTest {

    /** Narrower than the 130 dp the assessment label used to reserve for itself. */
    private val narrowWidthPx = 120

    private fun renderAt(widthPx: Int, result: PathAnalysisResult?) {
        onEventThread {
            val scene = ImageComposeScene(
                width = widthPx,
                height = 600,
                density = Density(1f),
            ) {
                PathAnalysisDetails(
                    result = result,
                    darkMode = true,
                    modifier = Modifier.fillMaxWidth(),
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
    fun `renders when the column is narrower than the assessment label`() {
        renderAt(narrowWidthPx, PathAnalysisResult.loading("JO50JP", "JN24JB", "F5JMI"))
    }

    @Test
    fun `renders at a width no operator could drag it below`() {
        renderAt(1, PathAnalysisResult.loading("JO50JP", "JN24JB", "F5JMI"))
    }

    @Test
    fun `renders without a result`() {
        renderAt(narrowWidthPx, null)
    }

    @Test
    fun `renders at a comfortable width`() {
        renderAt(350, PathAnalysisResult.loading("JO50JP", "JN24JB", "F5JMI"))
    }
}
