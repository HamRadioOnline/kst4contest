package kst4contest.view.compose

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Compose brings no SplitPane, so this is ours. An operator arranges the main window
 * once for their screen and expects it back at the next contest, which makes the stored
 * divider positions a setting rather than decoration.
 */
class SplitterStateTest {

    // ---- what it starts from ----------------------------------------------

    @Test
    fun `the stored positions are what it starts from`() {
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.51)) {}
        assertEquals(listOf(0.51f), state.positions)
    }

    /**
     * A profile written by an older version has fewer positions than the window now has
     * dividers. Falling back beats starting with a window whose panes have no size.
     */
    @Test
    fun `too few stored positions fall back to an even split`() {
        val state = SplitterState(paneCount = 5, stored = doubleArrayOf(0.62)) {}
        assertEquals(listOf(0.2f, 0.4f, 0.6f, 0.8f), state.positions)
    }

    @Test
    fun `too many stored positions fall back as well`() {
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.3, 0.6, 0.9)) {}
        assertEquals(listOf(0.5f), state.positions)
    }

    @Test
    fun `positions out of order fall back rather than drawing inside out`() {
        val state = SplitterState(paneCount = 3, stored = doubleArrayOf(0.8, 0.2)) {}
        assertEquals(listOf(1f / 3f, 2f / 3f), state.positions)
    }

    @Test
    fun `a position outside the pane falls back`() {
        assertEquals(listOf(0.5f), SplitterState(paneCount = 2, stored = doubleArrayOf(1.4)) {}.positions)
        assertEquals(listOf(0.5f), SplitterState(paneCount = 2, stored = doubleArrayOf(-0.1)) {}.positions)
    }

    @Test
    fun `a single pane has no divider at all`() {
        val state = SplitterState(paneCount = 1, stored = doubleArrayOf()) {}
        assertTrue(state.positions.isEmpty())
    }

    // ---- dragging ----------------------------------------------------------

    @Test
    fun `dragging a divider moves it`() {
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.5)) {}
        state.drag(index = 0, to = 0.7f, totalPx = 1000f)
        assertEquals(0.7f, state.positions[0], 0.001f)
    }

    /**
     * A divider stops at its neighbours. In JavaFX pushing one along shoved the next out
     * of the way, which is how operators ended up with panes they could not get back.
     */
    @Test
    fun `a divider stops at its neighbour rather than pushing it`() {
        val state = SplitterState(paneCount = 3, stored = doubleArrayOf(0.3, 0.6)) {}
        state.drag(index = 0, to = 0.9f, totalPx = 1000f)

        assertTrue(state.positions[0] < state.positions[1], "the first divider passed the second")
        assertEquals(0.6f, state.positions[1], 0.001f, "the second divider moved")
    }

    @Test
    fun `a divider stops at the previous one too`() {
        val state = SplitterState(paneCount = 3, stored = doubleArrayOf(0.3, 0.6)) {}
        state.drag(index = 1, to = 0.05f, totalPx = 1000f)
        assertTrue(state.positions[1] > state.positions[0])
    }

    /** Every pane keeps a minimum, or a table can be dragged away to nothing by accident. */
    @Test
    fun `no pane can be dragged smaller than its minimum`() {
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.5), minPanePx = 100f) {}
        state.drag(index = 0, to = 0.01f, totalPx = 1000f)
        assertEquals(0.1f, state.positions[0], 0.001f)
    }

    @Test
    fun `the minimum holds at the far edge as well`() {
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.5), minPanePx = 100f) {}
        state.drag(index = 0, to = 0.99f, totalPx = 1000f)
        assertEquals(0.9f, state.positions[0], 0.001f)
    }

    /** A window too narrow for the minimums must still lay out, not divide by zero. */
    @Test
    fun `an impossibly narrow window still yields usable positions`() {
        val state = SplitterState(paneCount = 3, stored = doubleArrayOf(0.33, 0.66), minPanePx = 100f) {}
        state.drag(index = 0, to = 0.5f, totalPx = 50f)
        assertTrue(state.positions.all { it.isFinite() })
        assertTrue(state.positions[0] <= state.positions[1])
    }

    // ---- saving ------------------------------------------------------------

    /**
     * Saved on release, not on every mouse move. The JavaFX version called
     * requestLayoutSave() on every pixel of a drag and wrote the profile to disk while
     * the operator was still holding the mouse down.
     */
    @Test
    fun `dragging saves nothing until the mouse is released`() {
        var saved: DoubleArray? = null
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.5)) { saved = it }

        state.drag(index = 0, to = 0.7f, totalPx = 1000f)
        assertNull(saved, "a drag in progress wrote to the profile")

        state.dragFinished()
        assertArrayEquals(doubleArrayOf(0.7), saved!!, 0.001)
    }

    /** A drag that changed nothing must not write the profile either. */
    @Test
    fun `a release without a change saves nothing`() {
        var saves = 0
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.5)) { saves++ }

        state.dragFinished()
        assertEquals(0, saves)

        state.drag(index = 0, to = 0.5f, totalPx = 1000f)
        state.dragFinished()
        assertEquals(0, saves, "a drag that moved nothing was saved")
    }

    /** The counterpart of the deferred Etappe 4 minor: a refused move is not a change. */
    @Test
    fun `a refused drag is not saved`() {
        var saves = 0
        val state = SplitterState(paneCount = 2, stored = doubleArrayOf(0.9), minPanePx = 100f) { saves++ }

        state.drag(index = 0, to = 0.99f, totalPx = 1000f)
        state.dragFinished()
        assertEquals(0, saves, "a drag that was clamped back to where it started was saved")
    }

    // ---- the dividers take room too ---------------------------------------

    /**
     * The panes and the divider handles are laid out as siblings, so the panes may only
     * have what is left after the handles. Distributing the full length instead overflows
     * the splitter by one handle width per divider — 20 dp for the five-pane message
     * column — and the last pane is clipped off the bottom.
     */
    @Test
    fun `panes and dividers together fit the splitter exactly`() {
        val state = SplitterState(paneCount = 5, stored = doubleArrayOf(0.62, 0.7, 0.75, 0.9)) {}
        val total = 800f
        val handle = 5f

        val available = SplitterState.availableFor(total, dividerPx = handle, paneCount = 5)
        val panes = state.paneSizes(available)

        assertEquals(
            total,
            panes.sum() + handle * 4,
            0.01f,
            "panes plus handles must be the splitter's length",
        )
    }

    @Test
    fun `a splitter with one pane needs no room for dividers`() {
        assertEquals(500f, SplitterState.availableFor(500f, dividerPx = 5f, paneCount = 1))
    }

    /** A splitter too small for its own handles must not hand out negative lengths. */
    @Test
    fun `a splitter smaller than its handles hands out nothing rather than less than nothing`() {
        val available = SplitterState.availableFor(10f, dividerPx = 5f, paneCount = 5)
        assertEquals(0f, available)

        val state = SplitterState(paneCount = 5, stored = doubleArrayOf(0.2, 0.4, 0.6, 0.8)) {}
        assertTrue(state.paneSizes(available).all { it >= 0f })
    }

    // ---- what a draw subscribes to ----------------------------------------

    @Test
    fun `drawing the splitter subscribes to the positions`() {
        val state = SplitterState(paneCount = 3, stored = doubleArrayOf(0.3, 0.6)) {}
        val read = mutableSetOf<String>()

        Snapshot.observe(readObserver = { read += it.toString() }) {
            state.positions.forEach { it.toString() }
        }

        assertTrue(read.isNotEmpty(), "the positions were read without subscribing")
    }

    @Test
    fun `the pane sizes follow from the positions`() {
        val state = SplitterState(paneCount = 3, stored = doubleArrayOf(0.25, 0.75)) {}
        assertEquals(listOf(250f, 500f, 250f), state.paneSizes(totalPx = 1000f))
    }

    @Test
    fun `the pane sizes always add up to the whole`() {
        val state = SplitterState(paneCount = 4, stored = doubleArrayOf(0.1, 0.2, 0.9)) {}
        assertEquals(777f, state.paneSizes(totalPx = 777f).sum(), 0.01f)
    }

    @Test
    fun `pane-specific minimums keep fixed-height controls visible without replacing stored positions`() {
        val stored = doubleArrayOf(0.62, 0.7, 0.75, 0.9)
        val state = SplitterState(paneCount = 5, stored = stored) {}

        val sizes = state.paneSizes(
            totalPx = 800f,
            minimumPaneSizesPx = floatArrayOf(24f, 26f, 104f, 26f, 24f),
        )

        assertTrue(sizes[0] >= 24f)
        assertTrue(sizes[1] >= 26f)
        assertTrue(sizes[2] >= 104f)
        assertTrue(sizes[3] >= 26f)
        assertTrue(sizes[4] >= 24f)
        assertEquals(800f, sizes.sum(), 0.01f)
        stored.forEachIndexed { index, position ->
            assertEquals(position.toFloat(), state.positions[index], 0.001f)
        }
    }

    @Test
    fun `pane-specific minimums scale down together in an impossibly small splitter`() {
        val state = SplitterState(paneCount = 3, stored = doubleArrayOf(0.2, 0.8)) {}

        val sizes = state.paneSizes(
            totalPx = 100f,
            minimumPaneSizesPx = floatArrayOf(50f, 100f, 50f),
        )

        assertEquals(listOf(25f, 50f, 25f), sizes)
        assertEquals(100f, sizes.sum(), 0.01f)
    }

    @Test
    fun `pane-specific minimum scaling tolerates float rounding`() {
        val state = SplitterState(
            paneCount = 2,
            stored = doubleArrayOf(0.48),
            minPanePx = 160f,
        ) {}

        val sizes = state.paneSizes(
            totalPx = 24.405657f,
            minimumPaneSizesPx = floatArrayOf(52f, 160f),
        )

        assertTrue(sizes.all { it >= 0f })
        assertEquals(24.405657f, sizes.sum(), 0.0001f)
    }

    private fun assertNull(value: Any?, message: String) {
        assertFalse(value != null, message)
    }
}
