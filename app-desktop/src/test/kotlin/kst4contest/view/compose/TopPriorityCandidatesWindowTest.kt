package kst4contest.view.compose

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import kst4contest.controller.ScoreService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The window the "more" button opens.
 *
 * Its whole content is one line per candidate, so the line is the thing worth pinning:
 * an operator reads the callsign and the score and nothing else, and the JavaFX version
 * spelled it a particular way for two releases.
 */
class TopPriorityCandidatesWindowTest {

    /**
     * The category is irrelevant to the label and is left null on purpose: a candidate
     * that reached the list without one must still read as a line rather than as a
     * crash.
     */
    private fun candidate(call: String, score: Double) =
        ScoreService.TopCandidate(call, call, null, score)

    private val twoCandidates =
        listOf(candidate("PA6I", 968.0), candidate("DK7SE", 895.0))

    /** Runs [block] against a rendered list of [twoCandidates] and reports what it picked. */
    private fun picking(block: (ImageComposeScene) -> Unit): String? {
        var picked: String? = null
        onEventThread {
            val scene = ImageComposeScene(width = 360, height = 500, density = Density(1f)) {
                TopPriorityCandidatesWindow.CandidateListForTest(twoCandidates) {
                    picked = it.displayCallSign
                }
            }
            try {
                scene.render()
                block(scene)
                scene.render()
            } finally {
                scene.close()
            }
        }
        return picked
    }

    private fun ImageComposeScene.clickAt(y: Float, timeMillis: Long) {
        val at = Offset(100f, y)
        sendPointerEvent(PointerEventType.Press, at, timeMillis = timeMillis)
        sendPointerEvent(PointerEventType.Release, at, timeMillis = timeMillis)
    }

    @Test
    fun `a candidate reads as its callsign and its score`() {
        assertEquals("PA6I  |  score 968", candidateLabel(candidate("PA6I", 968.0)))
    }

    /** The JavaFX cell used %.0f; 967.6 showed as 968, not as 967.6 or 967. */
    @Test
    fun `the score is rounded to whole points`() {
        assertEquals("DK7SE  |  score 968", candidateLabel(candidate("DK7SE", 967.6)))
        assertEquals("DK7SE  |  score 895", candidateLabel(candidate("DK7SE", 895.4)))
    }

    @Test
    fun `a score of zero still reads as a score`() {
        assertEquals("DL1ABC  |  score 0", candidateLabel(candidate("DL1ABC", 0.0)))
    }

    /**
     * Review Focus 3. Empty is the state the window opens in before the first score
     * run, and a list that throws while measuring takes the window with it — which is
     * the failure class the map's layout tests exist to catch.
     */
    @Test
    fun `the list renders with candidates and without`() {
        listOf(emptyList(), twoCandidates).forEach { ranking ->
            onEventThread {
                val scene = ImageComposeScene(width = 360, height = 500, density = Density(1f)) {
                    TopPriorityCandidatesWindow.CandidateListForTest(ranking) { }
                }
                try {
                    scene.render()
                } finally {
                    scene.close()
                }
            }
        }
    }

    /**
     * A double click picks the candidate under it, and picks the right one.
     *
     * Both clicks need an explicit `timeMillis`. `detectTapGestures` discards a second
     * press that arrives less than `doubleTapMinTimeMillis` (40 ms) after the first
     * release, and back-to-back `sendPointerEvent` calls default to the same timestamp
     * — so a synthetic burst reads as one click, not two. The gap below is 50 ms.
     *
     * Note the asymmetry if you change these numbers: the 40 ms floor is measured
     * against the timestamps supplied here, while the 300 ms ceiling is measured
     * against the wall clock, so an implausible 2000 ms "gap" would also pass. 50 ms is
     * used because it is what the assertion claims to be testing.
     *
     * The y of 32 is the second row, so this pins the per-row binding rather than only
     * that something fired.
     */
    @Test
    fun `a double click picks the candidate it lands on`() {
        assertEquals("PA6I", picking { scene ->
            scene.clickAt(y = 10f, timeMillis = 1000L)
            scene.clickAt(y = 10f, timeMillis = 1050L)
        })

        assertEquals("DK7SE", picking { scene ->
            scene.clickAt(y = 32f, timeMillis = 1000L)
            scene.clickAt(y = 32f, timeMillis = 1050L)
        })
    }

    /**
     * A single click must not pick a candidate.
     *
     * The window exists so the operator can read more than the first two candidates,
     * which means clicking about in it has to be safe: picking closes the window. The
     * JavaFX list required `getClickCount() >= 2` for exactly that reason.
     *
     * This is the half that protects the window's reason to exist, so it stays even
     * though the positive case above now covers the binding.
     */
    @Test
    fun `a single click does not pick a candidate`() {
        val picked = picking { scene ->
            scene.clickAt(y = 10f, timeMillis = 1000L)
            Thread.sleep(400)
        }

        assertNull(picked) { "a single click picked $picked and would have closed the window" }
    }
}
