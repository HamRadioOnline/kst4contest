package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatCategory
import kst4contest.model.ContestSked
import kotlin.math.abs

/**
 * How promising an aircraft reflection is, in the four steps the timeline distinguishes.
 *
 * The colours are the original ones and they are not theme colours: this strip is read
 * like a radar screen, where magenta means "now or never" in both designs.
 */
enum class PotentialLevel {
    HIGHEST,
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun forPercent(percent: Int): PotentialLevel = when {
            percent >= 95 -> HIGHEST
            percent >= 75 -> HIGH
            percent >= 50 -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * One priority candidate on the timeline: a station worth calling at a given minute,
 * placed on the next minute an aeroplane is expected.
 *
 * The Kotlin counterpart of TimelineView.CandidateEvent, built from the score service.
 */
data class TimelineCandidate(
    val callSignRaw: String,
    val displayCallSign: String,
    val preferredChatCategory: ChatCategory?,
    val timeUntilMs: Long,
    val minuteBucket: Int,
    val laneIndex: Int,
    val targetAzimuth: Double,
    val score: Double,
    val opportunityPotentialPercent: Int,
    val tooltipText: String?,
)

/** The four candidate groups produced for every minute bucket, in display order. */
enum class TimelineLane(val index: Int) {
    IN_BEAM_1(0),
    IN_BEAM_2(1),
    PRIORITY_1(2),
    PRIORITY_2(3);

    companion object {
        fun forIndex(index: Int): TimelineLane = entries[index.coerceIn(0, entries.lastIndex)]
    }
}

data class TimelineTick(
    val minutes: Int,
    val x: Float,
)

/**
 * Where things sit on the strip.
 *
 * Time runs left to right as it *increases*, so "now" is at the left edge and the end
 * of the preview window at the right. Imminent events are therefore on the left, which
 * is the opposite of a progress bar and worth knowing before reading the numbers.
 */
object TimelineGeometry {

    /** How far ahead the strip looks. */
    const val PREVIEW_MILLIS: Long = 30L * 60L * 1000L

    /**
     * The height the strip is drawn into, and the height the JavaFX call site actually
     * gave it. The class itself claimed 40 and then only ever used the upper half, which
     * is why the candidate labels and the sked labels landed on top of each other. The
     * lanes below are laid out for the real height.
     */
    const val HEIGHT: Float = 104f

    /** The time axis, with the candidate lanes above it and the skeds below. */
    const val AXIS_Y: Float = 70f
    const val TICK_LABEL_Y: Float = 72f

    /** Candidates keep further from the edges than skeds do, because their labels are wider. */
    const val CANDIDATE_MARGIN: Float = 30f
    const val SKED_MARGIN: Float = 10f

    const val LANE_BASE_Y: Float = 2f
    const val LANE_HEIGHT: Float = 16f

    /**
     * How many candidate lanes the producer defines: two in-beam choices followed by
     * two global priority choices.
     */
    const val LANE_COUNT: Int = 4

    /** Skeds get their own lane below the axis, which is what frees the labels. */
    const val SKED_Y: Float = 86f

    const val DEFAULT_BEAM_WIDTH_DEG: Double = 50.0

    /**
     * The x for an event that many milliseconds away, or null when it falls outside the
     * preview window and is not drawn at all.
     */
    fun xFor(timeUntilMs: Long, width: Float, margin: Float): Float? {
        if (timeUntilMs < 0 || timeUntilMs > PREVIEW_MILLIS) return null

        val x = timeUntilMs.toFloat() / PREVIEW_MILLIS.toFloat() * width

        // A strip narrower than two margins leaves no room to hold the marker off both
        // edges, and coerceIn refuses an empty range rather than picking a side. Centring
        // is the honest answer: on a strip that narrow the position carries no information
        // anyway, and this window lives on a tiling compositor where that happens.
        val lowerBound = margin
        val upperBound = width - margin
        if (lowerBound > upperBound) return width / 2f

        return x.coerceIn(lowerBound, upperBound)
    }

    /**
     * Where a label starts, given how wide it turned out. It sits to the right of its
     * marker, and flips to the left rather than running off the strip — near the right
     * edge, which is half an hour away, there is always room on the other side.
     */
    fun labelStartX(markerX: Float, labelWidth: Float, stripWidth: Float, gap: Float): Float {
        val toTheRight = markerX + gap
        return if (toTheRight + labelWidth <= stripWidth) toTheRight
        else (markerX - gap - labelWidth).coerceAtLeast(0f)
    }

    fun laneY(laneIndex: Int): Float =
        LANE_BASE_Y + LANE_HEIGHT * laneIndex.coerceIn(0, LANE_COUNT - 1)

    /**
     * Relative minute ticks for the current preview window. Five-minute ticks are used
     * whenever their labels fit; compact layouts increase the interval instead of
     * drawing overlapping text.
     */
    fun minuteTicks(axisWidth: Float, minimumLabelSpacing: Float): List<TimelineTick> {
        if (axisWidth <= 0f) return emptyList()

        val previewMinutes = (PREVIEW_MILLIS / 60_000L).toInt()
        val interval = listOf(5, 10, 15, previewMinutes).first { minutes ->
            axisWidth * minutes / previewMinutes >= minimumLabelSpacing
        }
        return (0..previewMinutes step interval).map { minutes ->
            TimelineTick(minutes, axisWidth * minutes / previewMinutes)
        }
    }
}

/**
 * How strongly a marker is drawn, given where the antenna points.
 *
 * Off-beam markers fade rather than disappear: the operator has to be able to see that
 * a chance exists in a direction they are not pointing, otherwise turning the rotator
 * would never occur to them.
 */
data class AntennaEmphasis(
    val iconAlpha: Float,
    val glowing: Boolean,
    val scale: Float,
) {
    companion object {
        /** Full strength, no glow — also what an unknown azimuth gets, so it stays readable. */
        val PLAIN = AntennaEmphasis(iconAlpha = 1f, glowing = false, scale = 1f)

        const val OFF_BEAM_ALPHA = 0.30f
        const val ON_TARGET_SCALE = 1.10f

        /**
         * Within half a beam width the marker glows and grows; out to a full beam width
         * it stays bright; beyond that it fades to a ghost.
         */
        fun forAzimuth(
            currentAzimuth: Double,
            targetAzimuth: Double,
            beamWidthDeg: Double,
        ): AntennaEmphasis {
            // An unknown or negative azimuth is not a direction, so nothing is implied by it.
            if (!targetAzimuth.isFinite() || targetAzimuth < 0) return PLAIN

            var delta = abs(currentAzimuth - targetAzimuth)
            if (delta > 180) delta = 360 - delta

            val onTarget = delta <= beamWidthDeg / 2.0
            val inBeam = delta <= beamWidthDeg

            return AntennaEmphasis(
                iconAlpha = if (inBeam) 1f else OFF_BEAM_ALPHA,
                glowing = onTarget,
                scale = if (onTarget) ON_TARGET_SCALE else 1f,
            )
        }
    }
}

/**
 * What the timeline draws.
 *
 * Compose state throughout: the strip is redrawn on the score service's pulse, on every
 * rotator movement and on every resize, and each of those has to reach the screen on
 * its own.
 */
class TimelineState {

    val skeds = mutableStateListOf<ContestSked>()
    val candidates = mutableStateListOf<TimelineCandidate>()

    /** Where the antenna points now, from the rotator or the manually set QTF. */
    var antennaAzimuth: Double by mutableStateOf(0.0)

    var beamWidthDeg: Double by mutableStateOf(TimelineGeometry.DEFAULT_BEAM_WIDTH_DEG)
        private set

    /** A station without a sensible beam width keeps the default rather than a nonsense one. */
    fun setBeamWidth(degrees: Double) {
        if (degrees > 0 && degrees.isFinite()) {
            beamWidthDeg = degrees
        }
    }

    fun replaceSkeds(newSkeds: List<ContestSked>) {
        skeds.clear()
        skeds.addAll(newSkeds)
    }

    fun replaceCandidates(newCandidates: List<TimelineCandidate>) {
        candidates.clear()
        candidates.addAll(newCandidates)
    }

    fun emphasisFor(targetAzimuth: Double): AntennaEmphasis =
        AntennaEmphasis.forAzimuth(antennaAzimuth, targetAzimuth, beamWidthDeg)
}
