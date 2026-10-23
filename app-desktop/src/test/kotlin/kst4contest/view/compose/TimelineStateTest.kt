package kst4contest.view.compose

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The timeline is the one view in this client that is read as a picture rather than as
 * text, so its geometry and its fading rules are pinned here. Everything in this file
 * is the original JavaFX behaviour; where it looks surprising, it is surprising there
 * too and the test says so.
 */
class TimelineStateTest {

    private val width = 400f

    // ---- the potential colours --------------------------------------------

    @Test
    fun `the potential steps break where they always did`() {
        assertEquals(PotentialLevel.HIGHEST, PotentialLevel.forPercent(100))
        assertEquals(PotentialLevel.HIGHEST, PotentialLevel.forPercent(95))
        assertEquals(PotentialLevel.HIGH, PotentialLevel.forPercent(94))
        assertEquals(PotentialLevel.HIGH, PotentialLevel.forPercent(75))
        assertEquals(PotentialLevel.MEDIUM, PotentialLevel.forPercent(74))
        assertEquals(PotentialLevel.MEDIUM, PotentialLevel.forPercent(50))
        assertEquals(PotentialLevel.LOW, PotentialLevel.forPercent(49))
        assertEquals(PotentialLevel.LOW, PotentialLevel.forPercent(0))
    }

    /** A missing potential arrives as a negative number and must not fall through. */
    @Test
    fun `an unknown potential is the lowest step`() {
        assertEquals(PotentialLevel.LOW, PotentialLevel.forPercent(-1))
    }

    // ---- the geometry ------------------------------------------------------

    /**
     * Counter-intuitive and deliberate: the strip is not a progress bar. Time grows to
     * the right, so what is about to happen sits at the LEFT edge.
     */
    @Test
    fun `now is on the left and the far future on the right`() {
        val imminent = TimelineGeometry.xFor(0, width, TimelineGeometry.SKED_MARGIN)!!
        val distant = TimelineGeometry.xFor(TimelineGeometry.PREVIEW_MILLIS, width, TimelineGeometry.SKED_MARGIN)!!
        assertTrue(imminent < distant, "time must grow to the right")
    }

    @Test
    fun `the middle of the window is the middle of the strip`() {
        assertEquals(
            200f,
            TimelineGeometry.xFor(TimelineGeometry.PREVIEW_MILLIS / 2, width, TimelineGeometry.SKED_MARGIN)!!,
            0.01f,
        )
    }

    /** Both edges are held off, or half a marker would hang outside the strip. */
    @Test
    fun `a marker never reaches the edge`() {
        assertEquals(
            TimelineGeometry.SKED_MARGIN,
            TimelineGeometry.xFor(0, width, TimelineGeometry.SKED_MARGIN)!!,
        )
        assertEquals(
            width - TimelineGeometry.SKED_MARGIN,
            TimelineGeometry.xFor(TimelineGeometry.PREVIEW_MILLIS, width, TimelineGeometry.SKED_MARGIN)!!,
        )
    }

    /**
     * Candidates are held further from the edge than skeds. Their labels are wider, so
     * the original picked two different margins — the difference is real and kept.
     */
    @Test
    fun `candidates keep further from the edge than skeds`() {
        assertTrue(TimelineGeometry.CANDIDATE_MARGIN > TimelineGeometry.SKED_MARGIN)
        assertEquals(
            TimelineGeometry.CANDIDATE_MARGIN,
            TimelineGeometry.xFor(0, width, TimelineGeometry.CANDIDATE_MARGIN)!!,
        )
    }

    @Test
    fun `an event outside the window is not placed at all`() {
        assertNull(TimelineGeometry.xFor(-1, width, TimelineGeometry.SKED_MARGIN))
        assertNull(
            TimelineGeometry.xFor(TimelineGeometry.PREVIEW_MILLIS + 1, width, TimelineGeometry.SKED_MARGIN),
        )
    }

    /**
     * A pane dragged narrower than two margins used to throw: coerceIn(30f, 10f) is an
     * empty range, and Kotlin refuses it rather than picking a side. The operator runs a
     * tiling compositor, so a strip narrower than 60 px is a Tuesday, not a corner case.
     */
    @Test
    fun `a strip narrower than its own margins still places a marker`() {
        val x = TimelineGeometry.xFor(
            timeUntilMs = 5 * 60 * 1000,
            width = 40f,
            margin = TimelineGeometry.CANDIDATE_MARGIN,
        )
        assertEquals(20f, x!!, 0.01f, "a narrow strip should centre the marker")
    }

    @Test
    fun `a strip of no width at all does not throw`() {
        val x = TimelineGeometry.xFor(0, width = 0f, margin = TimelineGeometry.SKED_MARGIN)
        assertEquals(0f, x!!, 0.01f)
    }

    @Test
    fun `the window looks thirty minutes ahead`() {
        assertEquals(30L * 60L * 1000L, TimelineGeometry.PREVIEW_MILLIS)
    }

    // ---- the lanes ---------------------------------------------------------

    @Test
    fun `the lanes stack downwards in even steps`() {
        assertEquals(2f, TimelineGeometry.laneY(0))
        assertEquals(18f, TimelineGeometry.laneY(1))
        assertEquals(34f, TimelineGeometry.laneY(2))
        assertEquals(50f, TimelineGeometry.laneY(3))
    }

    /**
     * The candidate lanes stay above the axis and the skeds below it. This is the one
     * place where this port does not copy the original: there the lanes were laid out
     * for a 40 px strip that the call site had already made 80 px tall, so the candidate
     * labels, the sked diamonds and the sked labels all landed on the same pixels while
     * the lower half stayed empty.
     */
    @Test
    fun `nothing is drawn across the axis or off the strip`() {
        val laneBottom = TimelineGeometry.laneY(TimelineGeometry.LANE_COUNT - 1) + TimelineGeometry.LANE_HEIGHT
        assertTrue(
            laneBottom <= TimelineGeometry.AXIS_Y,
            "candidate lanes reach the axis: $laneBottom vs ${TimelineGeometry.AXIS_Y}",
        )
        assertTrue(
            TimelineGeometry.SKED_Y > TimelineGeometry.AXIS_Y,
            "skeds must sit below the axis",
        )
        assertTrue(
            TimelineGeometry.SKED_Y + TimelineGeometry.LANE_HEIGHT <= TimelineGeometry.HEIGHT,
            "the sked lane runs off the bottom",
        )
    }

    /** Unknown producer values stay visible in the nearest defined lane. */
    @Test
    fun `a lane beyond the available ones folds onto the last`() {
        assertEquals(TimelineGeometry.laneY(3), TimelineGeometry.laneY(7))
        assertEquals(TimelineGeometry.laneY(0), TimelineGeometry.laneY(-1))
    }

    @Test
    fun `lane definitions expose the producer semantics in display order`() {
        assertEquals(
            listOf(
                TimelineLane.IN_BEAM_1,
                TimelineLane.IN_BEAM_2,
                TimelineLane.PRIORITY_1,
                TimelineLane.PRIORITY_2,
            ),
            TimelineLane.entries,
        )
        assertEquals(TimelineLane.PRIORITY_1, TimelineLane.forIndex(2))
        assertEquals(TimelineLane.PRIORITY_2, TimelineLane.forIndex(99))
    }

    @Test
    fun `a wide timeline gets five minute ticks over the full preview`() {
        val ticks = TimelineGeometry.minuteTicks(axisWidth = 600f, minimumLabelSpacing = 30f)

        assertEquals(listOf(0, 5, 10, 15, 20, 25, 30), ticks.map { it.minutes })
        assertEquals(0f, ticks.first().x, 0.01f)
        assertEquals(600f, ticks.last().x, 0.01f)
    }

    @Test
    fun `tick interval grows when labels would overlap`() {
        val ticks = TimelineGeometry.minuteTicks(axisWidth = 100f, minimumLabelSpacing = 30f)

        assertEquals(listOf(0, 10, 20, 30), ticks.map { it.minutes })
        assertTrue(ticks.zipWithNext().all { (left, right) -> right.x - left.x >= 30f })
    }

    // ---- the labels --------------------------------------------------------

    @Test
    fun `a label sits to the right of its marker`() {
        assertEquals(54f, TimelineGeometry.labelStartX(markerX = 50f, labelWidth = 60f, stripWidth = width, gap = 4f))
    }

    /** Near the right edge it flips instead of running off — half an hour away there is always room. */
    @Test
    fun `a label near the right edge flips to the left`() {
        assertEquals(
            336f,
            TimelineGeometry.labelStartX(markerX = 400f, labelWidth = 60f, stripWidth = width, gap = 4f),
        )
    }

    @Test
    fun `a label wider than the strip still starts on it`() {
        assertEquals(
            0f,
            TimelineGeometry.labelStartX(markerX = 10f, labelWidth = 900f, stripWidth = width, gap = 4f),
        )
    }

    // ---- the antenna fading -----------------------------------------------

    @Test
    fun `a target dead ahead glows and grows`() {
        val e = AntennaEmphasis.forAzimuth(currentAzimuth = 90.0, targetAzimuth = 90.0, beamWidthDeg = 50.0)
        assertEquals(1f, e.iconAlpha)
        assertTrue(e.glowing)
        assertEquals(1.10f, e.scale)
    }

    @Test
    fun `the glow reaches exactly half a beam width`() {
        assertTrue(AntennaEmphasis.forAzimuth(0.0, 25.0, 50.0).glowing)
        assertFalse(AntennaEmphasis.forAzimuth(0.0, 25.1, 50.0).glowing)
    }

    /**
     * Between half a beam and a full beam the marker stays bright without the glow. The
     * station is still workable; it is just not centred.
     */
    @Test
    fun `just off centre stays bright without the glow`() {
        val e = AntennaEmphasis.forAzimuth(0.0, 40.0, 50.0)
        assertEquals(1f, e.iconAlpha)
        assertFalse(e.glowing)
        assertEquals(1f, e.scale)
    }

    @Test
    fun `the full beam width is still bright`() {
        assertEquals(1f, AntennaEmphasis.forAzimuth(0.0, 50.0, 50.0).iconAlpha)
    }

    /**
     * Beyond the beam the marker fades but does not vanish. An operator must be able to
     * see a chance in a direction they are not pointing, or turning the rotator would
     * never occur to them.
     */
    @Test
    fun `a target outside the beam fades to a ghost but stays visible`() {
        val e = AntennaEmphasis.forAzimuth(0.0, 120.0, 50.0)
        assertEquals(0.30f, e.iconAlpha)
        assertFalse(e.glowing)
        assertTrue(e.iconAlpha > 0f, "a ghost is still a marker")
    }

    /** North is the case that breaks a naive subtraction: 350° and 10° are 20° apart. */
    @Test
    fun `the beam reaches across north`() {
        assertTrue(AntennaEmphasis.forAzimuth(350.0, 10.0, 50.0).glowing)
        assertTrue(AntennaEmphasis.forAzimuth(10.0, 350.0, 50.0).glowing)
    }

    @Test
    fun `an unknown azimuth is left readable`() {
        assertEquals(AntennaEmphasis.PLAIN, AntennaEmphasis.forAzimuth(0.0, -1.0, 50.0))
        assertEquals(AntennaEmphasis.PLAIN, AntennaEmphasis.forAzimuth(0.0, Double.NaN, 50.0))
    }

    // ---- the state --------------------------------------------------------

    @Test
    fun `the default beam width is the one the original assumed`() {
        assertEquals(50.0, TimelineState().beamWidthDeg)
    }

    @Test
    fun `a nonsense beam width is refused rather than drawn`() {
        val state = TimelineState()
        state.setBeamWidth(0.0)
        assertEquals(50.0, state.beamWidthDeg)
        state.setBeamWidth(-30.0)
        assertEquals(50.0, state.beamWidthDeg)
        state.setBeamWidth(Double.NaN)
        assertEquals(50.0, state.beamWidthDeg)
        state.setBeamWidth(12.5)
        assertEquals(12.5, state.beamWidthDeg)
    }

    @Test
    fun `the antenna direction decides the emphasis`() {
        val state = TimelineState()
        state.antennaAzimuth = 200.0
        assertTrue(state.emphasisFor(200.0).glowing)
        assertEquals(0.30f, state.emphasisFor(20.0).iconAlpha)
    }

    // ---- what a draw subscribes to ----------------------------------------

    /**
     * The strip is redrawn on the score pulse, on every rotator step and on every
     * resize. Each of those has to reach the screen on its own, so reading any of it
     * must register as a read.
     */
    @Test
    fun `drawing the timeline subscribes to what can change`() {
        val state = TimelineState()
        val read = mutableSetOf<String>()

        Snapshot.observe(readObserver = { read += it.toString() }) {
            state.skeds.size
            state.candidates.size
            state.antennaAzimuth
            state.beamWidthDeg
        }

        assertEquals(4, read.size, "a timeline value was read without subscribing: $read")
    }

    @Test
    fun `replacing the candidates replaces them rather than appending`() {
        val state = TimelineState()
        state.replaceCandidates(listOf(candidate("DL0ABC", lane = 0)))
        state.replaceCandidates(listOf(candidate("DL0XYZ", lane = 0), candidate("DL0QRS", lane = 1)))

        assertEquals(listOf("DL0XYZ", "DL0QRS"), state.candidates.map { it.displayCallSign })
    }

    private fun candidate(call: String, lane: Int) = TimelineCandidate(
        callSignRaw = call,
        displayCallSign = call,
        preferredChatCategory = null,
        timeUntilMs = 60_000,
        minuteBucket = 1,
        laneIndex = lane,
        targetAzimuth = 90.0,
        score = 1.0,
        opportunityPotentialPercent = 80,
        tooltipText = null,
    )
}
