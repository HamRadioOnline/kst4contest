package kst4contest.view.compose.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kst4contest.locatorUtils.DirectionUtils
import kst4contest.view.map.MapViewport
import kst4contest.view.map.PathGeometryUtils
import kotlin.math.ceil

/** Immutable effective QTF/QRB filter values drawn by the station map. */
data class SpatialFilterOverlayState(
    val qtfEnabled: Boolean,
    val qtfDegrees: Double,
    val beamWidthDeg: Double,
    val minQrbEnabled: Boolean,
    val minQrbKm: Double,
    val maxQrbEnabled: Boolean,
    val maxQrbKm: Double,
) {
    val isActive: Boolean
        get() = qtfEnabled || minQrbEnabled || maxQrbEnabled

    val hasEmptyDistanceRange: Boolean
        get() = minQrbEnabled && maxQrbEnabled && minQrbKm >= maxQrbKm

    val hasRenderableValues: Boolean
        get() = (!qtfEnabled || qtfDegrees.isFinite() && beamWidthDeg.isFinite()) &&
            (!minQrbEnabled || minQrbKm.isFinite()) &&
            (!maxQrbEnabled || maxQrbKm.isFinite())

    /** True when the current values produce at least one visible fill or boundary. */
    val hasDrawableGeometry: Boolean
        get() {
            if (!isActive || !hasRenderableValues) return false
            if (minQrbEnabled && !maxQrbEnabled) return true
            if (minQrbEnabled && minQrbKm > 0.0) return true
            if (maxQrbEnabled && maxQrbKm > 0.0) return true
            return qtfEnabled && !maxQrbEnabled
        }

    /** Mirrors the filter predicate: minimum exclusive, maximum inclusive. */
    fun accepts(distanceKm: Double, bearingDeg: Double): Boolean {
        if (!distanceKm.isFinite() || !bearingDeg.isFinite()) return false
        if (minQrbEnabled && distanceKm <= minQrbKm) return false
        if (maxQrbEnabled && distanceKm > maxQrbKm) return false
        if (qtfEnabled && !DirectionUtils.isAngleInRange(bearingDeg, qtfDegrees, beamWidthDeg)) return false
        return true
    }
}

internal fun DrawScope.drawSpatialFilterOverlay(
    viewport: MapViewport,
    homeLatitudeDeg: Double,
    homeLongitudeDeg: Double,
    state: SpatialFilterOverlayState,
    darkMode: Boolean,
) {
    if (!state.hasDrawableGeometry ||
        !homeLatitudeDeg.isFinite() || !homeLongitudeDeg.isFinite()
    ) return

    val fillColor = if (darkMode) FILTER_FILL_DARK else FILTER_FILL_LIGHT
    val lineColor = spatialFilterOverlayLineColor(darkMode)
    val angular = state.qtfEnabled && state.beamWidthDeg.isFinite()
    val validAngularArea = !angular || state.beamWidthDeg > 0.0 && state.beamWidthDeg < 360.0
    val visibleRadius = visibleRadiusKm(viewport, homeLatitudeDeg, homeLongitudeDeg)
    val outerRadius = if (state.maxQrbEnabled) state.maxQrbKm else visibleRadius
    val innerRadius = if (state.minQrbEnabled) state.minQrbKm else 0.0

    if (!state.hasEmptyDistanceRange && validAngularArea && outerRadius > innerRadius && outerRadius > 0.0) {
        val fillPath = when {
            angular -> annularSectorPath(
                viewport,
                homeLatitudeDeg,
                homeLongitudeDeg,
                state.qtfDegrees,
                state.beamWidthDeg,
                innerRadius.coerceAtLeast(0.0),
                outerRadius,
            )

            state.minQrbEnabled && !state.maxQrbEnabled -> outsideCirclePath(
                viewport,
                homeLatitudeDeg,
                homeLongitudeDeg,
                innerRadius.coerceAtLeast(0.0),
            )

            else -> ringPath(
                viewport,
                homeLatitudeDeg,
                homeLongitudeDeg,
                innerRadius.coerceAtLeast(0.0),
                outerRadius,
            )
        }
        drawPath(fillPath, fillColor, style = Fill)
    }

    val stroke = Stroke(
        width = FILTER_LINE_WIDTH_PX,
        pathEffect = PathEffect.dashPathEffect(FILTER_DASH_PX),
    )
    if (state.minQrbEnabled && state.minQrbKm > 0.0) {
        drawPath(circlePath(viewport, homeLatitudeDeg, homeLongitudeDeg, state.minQrbKm), lineColor, style = stroke)
    }
    if (state.maxQrbEnabled && state.maxQrbKm > 0.0) {
        drawPath(circlePath(viewport, homeLatitudeDeg, homeLongitudeDeg, state.maxQrbKm), lineColor, style = stroke)
    }
    if (angular) {
        val boundaryRadius = if (state.maxQrbEnabled) state.maxQrbKm else visibleRadius
        val halfWidth = state.beamWidthDeg.coerceAtLeast(0.0) / 2.0
        listOf(state.qtfDegrees - halfWidth, state.qtfDegrees + halfWidth).forEach { bearing ->
            val endpoint = calculateDestinationPoint(
                homeLatitudeDeg,
                homeLongitudeDeg,
                bearing,
                boundaryRadius,
            )
            drawLine(
                color = lineColor,
                start = Offset(
                    viewport.screenXWrapped(homeLongitudeDeg).toFloat(),
                    viewport.screenY(homeLatitudeDeg).toFloat(),
                ),
                end = Offset(
                    viewport.screenXWrapped(endpoint.second).toFloat(),
                    viewport.screenY(endpoint.first).toFloat(),
                ),
                strokeWidth = FILTER_LINE_WIDTH_PX,
                pathEffect = PathEffect.dashPathEffect(FILTER_DASH_PX),
            )
        }
    }
}

private fun outsideCirclePath(
    viewport: MapViewport,
    homeLatitudeDeg: Double,
    homeLongitudeDeg: Double,
    radiusKm: Double,
): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addRect(Rect(0f, 0f, viewport.widthPx().toFloat(), viewport.heightPx().toFloat()))
    addPath(circlePath(viewport, homeLatitudeDeg, homeLongitudeDeg, radiusKm))
}

private fun ringPath(
    viewport: MapViewport,
    homeLatitudeDeg: Double,
    homeLongitudeDeg: Double,
    innerRadiusKm: Double,
    outerRadiusKm: Double,
): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addPath(circlePath(viewport, homeLatitudeDeg, homeLongitudeDeg, outerRadiusKm))
    if (innerRadiusKm > 0.0) {
        addPath(circlePath(viewport, homeLatitudeDeg, homeLongitudeDeg, innerRadiusKm))
    }
}

private fun annularSectorPath(
    viewport: MapViewport,
    homeLatitudeDeg: Double,
    homeLongitudeDeg: Double,
    centerBearingDeg: Double,
    beamWidthDeg: Double,
    innerRadiusKm: Double,
    outerRadiusKm: Double,
): Path {
    val halfWidth = beamWidthDeg / 2.0
    val outer = arcPoints(
        homeLatitudeDeg,
        homeLongitudeDeg,
        centerBearingDeg - halfWidth,
        beamWidthDeg,
        outerRadiusKm,
    )
    val inner = if (innerRadiusKm > 0.0) {
        arcPoints(
            homeLatitudeDeg,
            homeLongitudeDeg,
            centerBearingDeg - halfWidth,
            beamWidthDeg,
            innerRadiusKm,
        ).asReversed()
    } else {
        listOf(homeLatitudeDeg to homeLongitudeDeg)
    }

    return pathThrough(viewport, outer + inner, close = true)
}

private fun circlePath(
    viewport: MapViewport,
    homeLatitudeDeg: Double,
    homeLongitudeDeg: Double,
    radiusKm: Double,
): Path = pathThrough(
    viewport,
    arcPoints(homeLatitudeDeg, homeLongitudeDeg, 0.0, 360.0, radiusKm),
    close = true,
)

private fun arcPoints(
    homeLatitudeDeg: Double,
    homeLongitudeDeg: Double,
    startBearingDeg: Double,
    sweepDeg: Double,
    radiusKm: Double,
): List<Pair<Double, Double>> {
    val segmentCount = maxOf(24, ceil(kotlin.math.abs(sweepDeg) / 3.0).toInt())
    return (0..segmentCount).map { segment ->
        calculateDestinationPoint(
            homeLatitudeDeg,
            homeLongitudeDeg,
            startBearingDeg + sweepDeg * segment / segmentCount,
            radiusKm,
        )
    }
}

private fun pathThrough(
    viewport: MapViewport,
    points: List<Pair<Double, Double>>,
    close: Boolean,
): Path = Path().apply {
    points.forEachIndexed { index, point ->
        val x = viewport.screenXWrapped(point.second).toFloat()
        val y = viewport.screenY(point.first).toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    if (close && points.isNotEmpty()) close()
}

private fun visibleRadiusKm(
    viewport: MapViewport,
    homeLatitudeDeg: Double,
    homeLongitudeDeg: Double,
): Double {
    val corners = listOf(
        viewport.northLat() to viewport.westLon(),
        viewport.northLat() to viewport.eastLon(),
        viewport.southLat() to viewport.westLon(),
        viewport.southLat() to viewport.eastLon(),
    )
    val farthestCorner = corners.maxOfOrNull { (latitude, longitude) ->
        PathGeometryUtils.calculateGreatCircleDistanceKm(
            homeLatitudeDeg,
            homeLongitudeDeg,
            latitude,
            longitude,
        )
    } ?: 0.0
    return farthestCorner * VIEWPORT_RADIUS_MARGIN
}

private val FILTER_FILL_LIGHT = Color(0xFF5B4BC4).copy(alpha = 0.16f)
private val FILTER_FILL_DARK = Color(0xFF78A9FF).copy(alpha = 0.18f)
private val FILTER_LINE_LIGHT = Color(0xFF4937B8).copy(alpha = 0.88f)
private val FILTER_LINE_DARK = Color(0xFF8CB8FF).copy(alpha = 0.92f)
internal fun spatialFilterOverlayLineColor(darkMode: Boolean): Color =
    if (darkMode) FILTER_LINE_DARK else FILTER_LINE_LIGHT
private const val FILTER_LINE_WIDTH_PX = 2f
private val FILTER_DASH_PX = floatArrayOf(7f, 5f)
private const val VIEWPORT_RADIUS_MARGIN = 1.12
