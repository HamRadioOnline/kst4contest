package kst4contest.view.compose.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import kst4contest.view.compose.onEventThread
import kst4contest.view.map.PathAnalysisResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * The value column keeps its share of a narrow analysis pane.
 *
 * Every row of this column pairs a label with a value, and the value is what an
 * operator reads: a locator, a distance, a propagation assessment. The JavaFX original
 * used a GridPane whose value column took the remaining width. The Compose port gave
 * the assessment label a fixed 130 dp instead, so once the splitter narrowed the pane
 * the label kept its 130 dp and the assessment was squeezed — visible as clipped text
 * in the 30 September screenshot.
 *
 * Measured on the rendered image rather than through a callback, so the production
 * composable carries nothing that exists only for this test. The assessment box is the
 * one opaque coloured band in the column, which makes its left edge findable.
 */
class PathAnalysisDetailsWidthTest {

    /** The severity-0 assessment background in dark mode, as PathAnalysisDetails paints it. */
    private val assessmentBackground = 0x33383E

    private fun render(paneWidthPx: Int, heightPx: Int = 600): BufferedImage {
        return onEventThread {
            val scene = ImageComposeScene(width = paneWidthPx, height = heightPx, density = Density(1f)) {
                PathAnalysisDetails(
                    result = PathAnalysisResult.loading("JO50JP", "JN24JB", "F5JMI"),
                    darkMode = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            try {
                val png = scene.render().encodeToData()!!.bytes
                ImageIO.read(ByteArrayInputStream(png))
            } finally {
                scene.close()
            }
        }
    }

    /** The smallest x at which the assessment background appears, or -1 when it does not. */
    private fun assessmentLeftEdge(image: BufferedImage): Int {
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                if ((image.getRGB(x, y) and 0xFFFFFF) == assessmentBackground) return x
            }
        }
        return -1
    }

    @Test
    fun `the assessment value starts where every other value starts`() {
        val paneWidth = 200
        val leftEdge = assessmentLeftEdge(render(paneWidth))

        assertTrue(leftEdge >= 0) { "the assessment box was not drawn at all" }
        assertTrue(
            leftEdge <= paneWidth * 0.45,
            {
                "the assessment value starts at x=$leftEdge of $paneWidth px; " +
                    "DetailRow starts its values at about 38%, so this column is out of line"
            },
        )
    }

    /**
     * The real column scrolls, so height is not what limits it. At 60 px every value
     * above wraps into a stack of lines and pushes this row far down; the height here
     * is generous for that reason, not because the pane is ever this tall.
     */
    @Test
    fun `the assessment box is still drawn in a sliver of a pane`() {
        assertTrue(assessmentLeftEdge(render(60, heightPx = 3000)) >= 0)
    }
}
