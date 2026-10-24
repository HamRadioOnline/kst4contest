package kst4contest.view.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale

import androidx.compose.foundation.onClick
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kst4contest.model.ContestSked
import kst4contest.view.i18n.LocalStrings
import kotlin.math.roundToInt

/** The strip is read like a radar screen, so it stays dark in both designs. */
private val STRIP_BACKGROUND = Color(0xFF2B2B2B)
private val AXIS_COLOR = Color.Gray

/** The glow that marks a target the antenna is centred on. */
private val GLOW_COLOR = Color(0xFF32CD32)

private val MARKER_SIZE = 12.dp
private val LABEL_GAP = 4.dp
private val CHIP_SHAPE = RoundedCornerShape(6.dp)
private val CHIP_BACKGROUND = Color(0xA6000000)
private val LANE_LABEL_WIDTH = 68.dp
private val AXIS_LABEL_PADDING = 6.dp

/**
 * The strip above the send field: what is coming in the next half hour.
 *
 * The one view in this client that is read as a picture rather than as text. Skeds and
 * priority candidates are placed by time, coloured by how promising the aircraft
 * reflection is, and faded when the antenna points elsewhere — so one glance answers
 * both "what is next" and "could I even hear it right now".
 *
 * Time grows to the right, which means the imminent things are on the LEFT. That is the
 * opposite of a progress bar and it is the original's choice, kept because operators
 * have learned it.
 *
 * Markers are drawn rather than built from widgets: this is a diagram, with no rows and
 * nothing to select. Only the candidates answer a click.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineView(
    state: TimelineState,
    onCandidateClicked: (TimelineCandidate) -> Unit,
    skedTooltipExtra: (ContestSked) -> String? = { null },
    modifier: Modifier = Modifier,
) {
    val nowMillis = androidx.compose.runtime.mutableStateOf(System.currentTimeMillis())
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            nowMillis.value = System.currentTimeMillis()
        }
    }

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val strings = LocalStrings.current

    BoxWithConstraints(
        modifier
            .height(TimelineGeometry.HEIGHT.dp)
            .background(STRIP_BACKGROUND),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val labelGutterPx = with(density) { LANE_LABEL_WIDTH.toPx() }.coerceAtMost(widthPx)
        val axisWidthPx = (widthPx - labelGutterPx).coerceAtLeast(0f)
        val axisY = with(density) { TimelineGeometry.AXIS_Y.dp.toPx() }
        val candidateMargin = with(density) { TimelineGeometry.CANDIDATE_MARGIN.dp.toPx() }
        val skedMargin = with(density) { TimelineGeometry.SKED_MARGIN.dp.toPx() }
        val tickLabels = TimelineGeometry.minuteTicks(
            axisWidth = axisWidthPx,
            minimumLabelSpacing = measurer.measure("+30 ${strings.timelineMinuteSuffix}", AXIS_TEXT_STYLE)
                .size.width.toFloat() + with(density) { AXIS_LABEL_PADDING.toPx() },
        ).map { tick ->
            tick to if (tick.minutes == 0) {
                "0 ${strings.timelineMinuteSuffix}"
            } else {
                "+${tick.minutes} ${strings.timelineMinuteSuffix}"
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            drawLine(AXIS_COLOR, Offset(labelGutterPx, axisY), Offset(size.width, axisY))
            tickLabels.forEach { (tick, _) ->
                val x = labelGutterPx + tick.x
                drawLine(AXIS_COLOR, Offset(x, axisY - 2.dp.toPx()), Offset(x, axisY + 3.dp.toPx()))
            }
        }

        val laneLabels = listOf(
            strings.timelineInBeam1,
            strings.timelineInBeam2,
            strings.timelinePriority1,
            strings.timelinePriority2,
        )
        laneLabels.forEachIndexed { index, label ->
            TimelineAxisLabel(label, TimelineGeometry.laneY(index).dp)
        }
        TimelineAxisLabel(strings.timelineSkeds, TimelineGeometry.SKED_Y.dp)

        tickLabels.forEach { (tick, label) ->
            val labelWidthPx = measurer.measure(label, AXIS_TEXT_STYLE).size.width.toFloat()
            val x = (labelGutterPx + tick.x - labelWidthPx / 2f)
                .coerceIn(labelGutterPx, (widthPx - labelWidthPx).coerceAtLeast(labelGutterPx))
            Text(
                text = label,
                modifier = Modifier.offset {
                    IntOffset(x.roundToInt(), with(density) { TimelineGeometry.TICK_LABEL_Y.dp.toPx() }.roundToInt())
                },
                style = AXIS_TEXT_STYLE,
                maxLines = 1,
            )
        }

        // The candidate lanes, above the axis.
        state.candidates.forEach { candidate ->
            val x = TimelineGeometry.xFor(candidate.timeUntilMs, axisWidthPx, candidateMargin)
                ?.plus(labelGutterPx)
                ?: return@forEach

            TimelineMarker(
                centerXPx = x,
                topDp = TimelineGeometry.laneY(candidate.laneIndex).dp,
                stripWidthPx = widthPx,
                label = candidate.displayCallSign,
                tooltip = candidate.tooltipText,
                level = PotentialLevel.forPercent(candidate.opportunityPotentialPercent),
                emphasis = state.emphasisFor(candidate.targetAzimuth),
                shape = MarkerShape.CANDIDATE,
                labelWidthPx = { text -> measurer.measure(text, CHIP_TEXT_STYLE).size.width.toFloat() },
                onClick = { onCandidateClicked(candidate) },
            )
        }

        // The skeds, in their own lane below the axis. In JavaFX they shared the space
        // above it, which is why their labels sat on top of the candidates'.
        val now = nowMillis.value
        state.skeds.forEach { sked ->
            val x = TimelineGeometry.xFor(sked.skedTimeEpoch - now, axisWidthPx, skedMargin)
                ?.plus(labelGutterPx)
                ?: return@forEach

            TimelineMarker(
                centerXPx = x,
                topDp = TimelineGeometry.SKED_Y.dp,
                stripWidthPx = widthPx,
                label = "SKED: ${sked.targetChatCallsign}",
                tooltip = skedTooltip(sked, skedTooltipExtra),
                level = PotentialLevel.forPercent(sked.opportunityPotentialPercent),
                emphasis = state.emphasisFor(sked.targetAzimuth),
                shape = MarkerShape.SKED,
                labelWidthPx = { text -> measurer.measure(text, CHIP_TEXT_STYLE).size.width.toFloat() },
                onClick = null,
            )
        }
    }
}

/**
 * The sked tooltip: who, on what band, in which direction — plus whatever the caller can
 * add about the chance itself.
 */
private fun skedTooltip(sked: ContestSked, extraProvider: (ContestSked) -> String?): String {
    val base = "${sked.targetChatCallsign} (${sked.band})\nAz: ${sked.targetAzimuth}"
    val extra = extraProvider(sked)
    return if (extra.isNullOrBlank()) base else "$base\n$extra"
}

private enum class MarkerShape { SKED, CANDIDATE }

private val CHIP_TEXT_STYLE = TextStyle(
    color = Color.White,
    fontSize = 9.sp,
    fontWeight = FontWeight.Bold,
)

private val AXIS_TEXT_STYLE = TextStyle(
    color = Color(0xFFB8B8B8),
    fontSize = 8.sp,
)

@Composable
private fun TimelineAxisLabel(text: String, top: Dp) {
    Text(
        text = text,
        modifier = Modifier
            .offset(x = 4.dp, y = top)
            .width(LANE_LABEL_WIDTH - 8.dp),
        style = AXIS_TEXT_STYLE,
        maxLines = 1,
    )
}

/**
 * One marker: the symbol on the time axis and its callsign beside it.
 *
 * The label is never faded, only the symbol is. A ghost marker still has to be
 * readable — that is the whole point of showing a chance in a direction the antenna is
 * not pointing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineMarker(
    centerXPx: Float,
    topDp: Dp,
    stripWidthPx: Float,
    label: String,
    tooltip: String?,
    level: PotentialLevel,
    emphasis: AntennaEmphasis,
    shape: MarkerShape,
    labelWidthPx: (String) -> Float,
    onClick: (() -> Unit)?,
) {
    val density = LocalDensity.current
    val markerSizePx = with(density) { MARKER_SIZE.toPx() }
    val gapPx = with(density) { LABEL_GAP.toPx() }

    val labelWidth = labelWidthPx(label)
    val labelX = TimelineGeometry.labelStartX(
        markerX = centerXPx + markerSizePx / 2f,
        labelWidth = labelWidth,
        stripWidth = stripWidthPx,
        gap = gapPx,
    )

    val symbol: @Composable () -> Unit = {
        Canvas(
            Modifier
                .size(MARKER_SIZE)
                .alpha(emphasis.iconAlpha),
        ) {
            scale(emphasis.scale) {
                if (emphasis.glowing) drawGlow()
                when (shape) {
                    MarkerShape.SKED -> drawDiamond(markerColor(level))
                    MarkerShape.CANDIDATE -> drawCandidate(markerColor(level))
                }
            }
        }
    }

    Box(
        Modifier.offset {
            IntOffset(
                (centerXPx - markerSizePx / 2f).roundToInt(),
                with(density) { topDp.toPx() }.roundToInt(),
            )
        },
    ) {
        val clickable = if (onClick != null) Modifier.onClick { onClick() } else Modifier
        if (tooltip.isNullOrBlank()) {
            Box(clickable) { symbol() }
        } else {
            TooltipArea(tooltip = { TimelineTooltip(tooltip) }) {
                Box(clickable) { symbol() }
            }
        }
    }

    // The label is placed on its own so it can flip sides without moving the symbol.
    Box(
        Modifier.offset {
            IntOffset(
                labelX.roundToInt(),
                with(density) { topDp.toPx() }.roundToInt(),
            )
        },
    ) {
        Box(
            Modifier
                .background(CHIP_BACKGROUND, CHIP_SHAPE)
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(text = label, style = CHIP_TEXT_STYLE, maxLines = 1)
        }
    }
}

/** Magenta means now or never; deep sky blue means it is barely worth turning for. */
private fun markerColor(level: PotentialLevel): Color = when (level) {
    PotentialLevel.HIGHEST -> Color.Magenta
    PotentialLevel.HIGH -> Color.Red
    PotentialLevel.MEDIUM -> Color.Yellow
    PotentialLevel.LOW -> Color(0xFF00BFFF)
}

/** The diamond of a sked: an appointment, not a chance. */
private fun DrawScope.drawDiamond(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w / 2f, 0f)
        lineTo(w, h / 2f)
        lineTo(w / 2f, h)
        lineTo(0f, h / 2f)
        close()
    }
    drawPath(path, color)
}

/** A candidate: a downward triangle over a dot, so it reads even at this size. */
private fun DrawScope.drawCandidate(color: Color) {
    val w = size.width
    val h = size.height
    drawCircle(color, radius = w / 3f, center = Offset(w / 2f, h / 2f))
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w / 2f, h * 0.85f)
        close()
    }
    drawPath(path, color)
}

/**
 * The glow for a target the antenna is centred on. JavaFX used a drop shadow; here it is
 * three fading rings, which costs nothing and reads the same at this size.
 */
private fun DrawScope.drawGlow() {
    val center = Offset(size.width / 2f, size.height / 2f)
    listOf(1.6f to 0.10f, 1.3f to 0.18f, 1.05f to 0.28f).forEach { (factor, alpha) ->
        drawCircle(
            color = GLOW_COLOR.copy(alpha = alpha),
            radius = size.width / 2f * factor,
            center = center,
        )
    }
}

@Composable
private fun TimelineTooltip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = Density.BUTTON_SHAPE,
        tonalElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
