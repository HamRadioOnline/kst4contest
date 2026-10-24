package kst4contest.view.compose.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import kst4contest.view.map.MaidenheadGridRenderPlanner
import kst4contest.view.map.MaidenheadGridUtils
import kst4contest.view.map.MapCallsignRawSnapshot
import kst4contest.view.map.MapMarkerCluster
import kst4contest.view.map.MapMarkerClusterTooltip
import kst4contest.view.map.MapViewport
import kst4contest.view.map.StationMapClusterer
import kst4contest.view.map.TileFetcher
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.skia.Image
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.sign
import kotlin.math.roundToInt

/**
 * Where the map is looking. Compose state, so a pan or a zoom reaches the screen.
 *
 * Holds no canvas size: the size is whatever the canvas is measured at, and a
 * [MapViewport] is built from the two together for each frame.
 */
class MapState(
    zoom: Float = 6f,
    centerLon: Double = 10.0,
    centerLat: Double = 51.0,
) {
    var zoom by mutableStateOf(zoom.coerceIn(MapViewport.MIN_ZOOM.toFloat(), MapViewport.MAX_ZOOM.toFloat()))
    var centerLon by mutableStateOf(centerLon)
    var centerLat by mutableStateOf(centerLat)

    fun viewportFor(widthPx: Float, heightPx: Float): MapViewport =
        MapViewport(centerLon, centerLat, zoom.toDouble(), widthPx.toDouble(), heightPx.toDouble())

    fun applyFrom(viewport: MapViewport) {
        centerLon = viewport.centerLon()
        centerLat = viewport.centerLat()
        zoom = viewport.zoom().toFloat()
    }
}

/**
 * The station map as a Compose canvas.
 *
 * **Everything but the tiles is drawn in canvas pixels.** A station dot is 12 px wide at
 * zoom 4 and at zoom 17; so is its label, and so is a grid line. Leaflet behaved that
 * way because its overlays sat in the DOM above a scaled tile layer, and the sizes here
 * are the ones its stylesheet declared. [MapViewport] makes that possible by folding the
 * fractional zoom into the projection.
 *
 * The previous version instead wrapped part of the drawing in `scale(scaleFactor)` and
 * divided every size back out by hand. The geometry that came out of it was right, but
 * two things were not. Text was measured at a size divided by the zoom fraction and then
 * magnified by it, so glyphs were rasterised at the wrong size. And the scale block
 * closed before the beam, the path line and the home marker were drawn, which left those
 * three using unscaled coordinates while the markers beside them used scaled ones — so at
 * any zoom with a fraction, the antenna beam did not start where the map said home was.
 * Neither failure is possible here, because nothing is scaled.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposeStationMap(
    markers: List<MapCallsignRawSnapshot>,
    tileFetcher: TileFetcher,
    mapState: MapState,
    groupingEnabled: Boolean,
    selectedMarkerCallsign: String?,
    ownLocator6: String,
    antennaAzimuthDeg: Double,
    antennaBeamWidthDeg: Double,
    maxQrbKm: Double,
    onMarkerClicked: ((MapCallsignRawSnapshot) -> Unit)?,
    onMarkerContextRequested: ((List<MapCallsignRawSnapshot>, IntOffset) -> Unit)? = null,
    workedGrossFields: Set<String> = emptySet(),
    darkMode: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val imageCache = remember { TileBitmapCache() }
    val fetchingTiles = remember { ConcurrentHashMap.newKeySet<String>() }
    val textMeasurer = rememberTextMeasurer()

    var hoverPosition by remember { mutableStateOf<Offset?>(null) }

    /*
     * Measured, not read off the draw pass. Writing Compose state while drawing costs a
     * frame at best: the first frame would group the stations against a zero-sized
     * viewport and the second would correct it, which is visible as a flicker of
     * wrongly grouped markers whenever the window is resized.
     */
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var canvasCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    /* Bumped when a tile arrives, which is what asks for the frame that shows it. */
    var tileRevision by remember { mutableStateOf(0) }

    /*
     * Grouping depends on where the map is looking, so it cannot be cached on the
     * stations alone.
     *
     * It is NOT off the drawing path: derivedStateOf computes lazily at first read, and
     * that read is inside the Canvas lambda. What changed against the first port is the
     * cost of the pass, not when it runs — that one searched for the nearest existing
     * cluster for every station, which is quadratic; this one buckets by screen cell in
     * one linear pass over a few hundred markers.
     */
    val home = remember(ownLocator6) { homePosition(ownLocator6) }
    val clusterResult by remember(markers, groupingEnabled, canvasSize) {
        derivedStateOf {
            StationMapClusterer.cluster(
                markers,
                mapState.viewportFor(canvasSize.width, canvasSize.height),
                groupingEnabled,
            )
        }
    }

    /*
     * Fetching belongs here and not in the draw pass. A tile the fetcher already holds
     * completes its future inline, so a fetch started while drawing would write the
     * cache and ask for a redraw in the middle of the frame that needed it — and the
     * frame would go out without it.
     */
    LaunchedEffect(tileFetcher) {
        /*
         * Read inside a snapshotFlow and not as effect keys. Keys are read during
         * composition, so keying on the zoom and the centre would recompose this whole
         * composable — and rebuild its gesture nodes — on every frame of a drag, when
         * all the frame needs is a redraw.
         */
        snapshotFlow {
            TileRequest(canvasSize, mapState.zoom, mapState.centerLon, mapState.centerLat)
        }.collectLatest { request ->
            if (request.size.width <= 0f || request.size.height <= 0f) {
                return@collectLatest
            }
            val viewport = mapState.viewportFor(request.size.width, request.size.height)
            for (key in visibleTileKeys(viewport)) {
                if (imageCache.contains(key.cacheKey) || !fetchingTiles.add(key.cacheKey)) {
                    continue
                }
                tileFetcher.fetchTileAsync(key.zoom, key.x, key.y).whenComplete { bytes, _ ->
                    try {
                        if (bytes != null) {
                            imageCache.put(key.cacheKey, Image.makeFromEncoded(bytes).toComposeImageBitmap())
                            tileRevision++
                        }
                    } catch (undecodable: Exception) {
                        /*
                         * Bytes that will not decode are a truncated or corrupt cache
                         * entry, and serving them again on the next pan would blank that
                         * square for good. Thrown away so the tile is fetched afresh.
                         */
                        tileFetcher.discardTile(key.zoom, key.x, key.y)
                    } finally {
                        fetchingTiles.remove(key.cacheKey)
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .onGloballyPositioned { canvasCoordinates = it }
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.buttons.isSecondaryPressed) {
                    val local = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                    val viewport = mapState.viewportFor(canvasSize.width, canvasSize.height)
                    val result = StationMapClusterer.cluster(markers, viewport, groupingEnabled)
                    val cluster = result.clusters().firstOrNull { candidate ->
                        within(
                            local,
                            viewport.screenX(candidate.centerLon).toFloat(),
                            viewport.screenY(candidate.centerLat).toFloat(),
                            clusterRadiusPx(candidate.size()),
                        )
                    }
                    val individual = result.individual().firstOrNull { marker ->
                        within(
                            local,
                            viewport.screenX(marker.longitudeDeg()).toFloat(),
                            viewport.screenY(marker.latitudeDeg()).toFloat(),
                            MARKER_HIT_RADIUS_PX,
                        )
                    }
                    val targets = cluster?.getMarkers() ?: individual?.let(::listOf)
                    val window = canvasCoordinates?.localToWindow(local)
                    if (!targets.isNullOrEmpty() && window != null) {
                        onMarkerContextRequested?.invoke(
                            targets,
                            IntOffset(window.x.roundToInt(), window.y.roundToInt()),
                        )
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    var viewport = mapState.viewportFor(size.width.toFloat(), size.height.toFloat())
                    if (pan != Offset.Zero) {
                        viewport = viewport.pannedBy(pan.x.toDouble(), pan.y.toDouble())
                    }
                    if (zoomChange != 1f && zoomChange > 0f) {
                        viewport = viewport.zoomedTo(
                            viewport.zoom() + log2(zoomChange.toDouble()),
                            centroid.x.toDouble(),
                            centroid.y.toDouble(),
                        )
                    }
                    mapState.applyFrom(viewport)
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Scroll -> {
                                val change = event.changes.first()
                                val scrollDelta = change.scrollDelta.y
                                if (scrollDelta != 0f) {
                                    val viewport = mapState.viewportFor(size.width.toFloat(), size.height.toFloat())
                                    mapState.applyFrom(
                                        viewport.zoomedTo(
                                            viewport.zoom() - scrollDelta.sign * ZOOM_STEP_PER_WHEEL_NOTCH,
                                            change.position.x.toDouble(),
                                            change.position.y.toDouble(),
                                        )
                                    )
                                }
                            }

                            PointerEventType.Move -> hoverPosition = event.changes.first().position
                            PointerEventType.Exit -> hoverPosition = null
                        }
                    }
                }
            }
            .pointerInput(markers, groupingEnabled, onMarkerClicked) {
                detectTapGestures { offset ->
                    val viewport = mapState.viewportFor(size.width.toFloat(), size.height.toFloat())
                    val result = StationMapClusterer.cluster(markers, viewport, groupingEnabled)

                    val hitCluster = result.clusters().firstOrNull { cluster ->
                        within(
                            offset,
                            viewport.screenX(cluster.centerLon).toFloat(),
                            viewport.screenY(cluster.centerLat).toFloat(),
                            clusterRadiusPx(cluster.size()),
                        )
                    }
                    if (hitCluster != null) {
                        /*
                         * A cluster click does not select a station. It moves towards the
                         * cluster and zooms in until the stations inside become visible,
                         * which is what the Leaflet map did.
                         */
                        mapState.centerLon = hitCluster.centerLon
                        mapState.centerLat = hitCluster.centerLat
                        mapState.zoom = minOf(
                            StationMapClusterer.DISABLE_AT_ZOOM,
                            mapState.zoom + 1.0,
                        ).toFloat()
                        return@detectTapGestures
                    }

                    val hit = result.individual().firstOrNull { marker ->
                        within(
                            offset,
                            viewport.screenX(marker.longitudeDeg()).toFloat(),
                            viewport.screenY(marker.latitudeDeg()).toFloat(),
                            MARKER_HIT_RADIUS_PX,
                        )
                    }
                    if (hit != null) {
                        onMarkerClicked?.invoke(hit)
                    }
                }
            }
    ) {
        @Suppress("UNUSED_EXPRESSION")
        tileRevision

        if (size.width <= 0f || size.height <= 0f) {
            return@Canvas
        }

        val viewport = mapState.viewportFor(size.width, size.height)

        drawRect(color = if (darkMode) MAP_BACKGROUND_DARK else MAP_BACKGROUND_LIGHT)

        drawTiles(viewport, imageCache, darkMode)
        drawMaidenheadGrid(viewport, textMeasurer, darkMode)
        drawWorkedGrossFields(viewport, workedGrossFields)
        drawBeamAndPath(viewport, home, antennaAzimuthDeg, antennaBeamWidthDeg, maxQrbKm, markers, selectedMarkerCallsign, darkMode)
        drawStations(viewport, clusterResult, selectedMarkerCallsign, textMeasurer, darkMode)
        drawHover(viewport, hoverPosition, clusterResult, textMeasurer)
    }
}

/* ------------------------------------------------------------------ tiles */

/** The view a tile request was made for; the unit snapshotFlow deduplicates on. */
private data class TileRequest(
    val size: Size,
    val zoom: Float,
    val centerLon: Double,
    val centerLat: Double,
)

/** One tile of the current view. */
internal data class TileKey(val zoom: Int, val x: Int, val y: Int) {
    val cacheKey: String get() = "$zoom/$x/$y"
}

/**
 * The tiles the viewport covers, east-west wrap applied.
 *
 * The world wraps at the antimeridian but the tile index does not, so a view straddling
 * it asks for indices outside 0..last and they are brought back round.
 */
internal fun visibleTileKeys(viewport: MapViewport): List<TileKey> {
    val tileZoom = viewport.tileZoom()
    val lastTileIndex = (1 shl tileZoom) - 1

    val firstX = floor(viewport.worldXAt(0.0) / TILE_SIZE).toInt()
    val lastX = floor(viewport.worldXAt(viewport.widthPx()) / TILE_SIZE).toInt()
    val firstY = floor(viewport.worldYAt(0.0) / TILE_SIZE).toInt()
    val lastY = floor(viewport.worldYAt(viewport.heightPx()) / TILE_SIZE).toInt()

    val keys = mutableListOf<TileKey>()
    for (tileY in firstY..lastY) {
        if (tileY < 0 || tileY > lastTileIndex) {
            continue
        }
        for (tileX in firstX..lastX) {
            keys.add(TileKey(tileZoom, wrapTileX(tileX, lastTileIndex), tileY))
        }
    }
    return keys
}

internal fun wrapTileX(tileX: Int, lastTileIndex: Int): Int {
    val span = lastTileIndex + 1
    val wrapped = tileX % span
    return if (wrapped < 0) wrapped + span else wrapped
}

private fun DrawScope.drawTiles(
    viewport: MapViewport,
    imageCache: TileBitmapCache,
    darkMode: Boolean,
) {
    val tileZoom = viewport.tileZoom()
    val lastTileIndex = (1 shl tileZoom) - 1

    /*
     * Rounded up. A tile scaled to a fractional width leaves a hairline of background
     * between itself and its neighbour otherwise, and a grid of hairlines reads as a
     * broken map.
     */
    val tileSizePx = ceil(viewport.tileSizePx()).toInt().coerceAtLeast(1)

    val firstX = floor(viewport.worldXAt(0.0) / TILE_SIZE).toInt()
    val lastX = floor(viewport.worldXAt(viewport.widthPx()) / TILE_SIZE).toInt()
    val firstY = floor(viewport.worldYAt(0.0) / TILE_SIZE).toInt()
    val lastY = floor(viewport.worldYAt(viewport.heightPx()) / TILE_SIZE).toInt()

    for (tileY in firstY..lastY) {
        if (tileY < 0 || tileY > lastTileIndex) {
            continue
        }
        for (tileX in firstX..lastX) {
            val tile = imageCache[TileKey(tileZoom, wrapTileX(tileX, lastTileIndex), tileY).cacheKey]
                ?: continue

            drawImage(
                image = tile,
                dstOffset = IntOffset(
                    floor(viewport.screenXForWorldX(tileX * TILE_SIZE)).toInt(),
                    floor(viewport.screenYForWorldY(tileY * TILE_SIZE)).toInt(),
                ),
                dstSize = IntSize(tileSizePx, tileSizePx),
                alpha = if (darkMode) DARK_TILE_ALPHA else 1.0f,
            )
        }
    }
}

/* ------------------------------------------------------------- grid */

private fun DrawScope.drawMaidenheadGrid(
    viewport: MapViewport,
    textMeasurer: TextMeasurer,
    darkMode: Boolean,
) {
    /*
     * The planner decides the precision, whether labels are shown at all, and how many
     * cells apart they sit. Without it every cell gets a line and a label, which at
     * six-digit precision is a carpet of text rather than a grid.
     */
    val plan = MaidenheadGridRenderPlanner.createPlan(
        viewport.tileZoom(),
        viewport.southLat(),
        viewport.westLon(),
        viewport.northLat(),
        viewport.eastLon(),
        viewport.widthPx(),
        viewport.heightPx(),
    )

    val cells = MaidenheadGridUtils.buildVisibleCells(
        viewport.southLat(),
        viewport.westLon(),
        viewport.northLat(),
        viewport.eastLon(),
        plan.precision(),
    )

    val lineColor = if (darkMode) GRID_LINE_DARK else GRID_LINE_LIGHT
    val lineAlpha = if (darkMode) GRID_LINE_ALPHA_DARK else GRID_LINE_ALPHA_LIGHT

    /*
     * Unique lines, not one stroked rectangle per cell. Neighbouring cells share an
     * edge, so a rectangle each paints every interior line twice: at 0.48 a shared edge
     * reaches about 0.73 while the outermost line stays at 0.48, and the grid comes out
     * visibly uneven. Leaflet drew each line once.
     */
    val verticals = sortedSetOf<Float>()
    val horizontals = sortedSetOf<Float>()
    for (cell in cells) {
        verticals.add(viewport.screenX(cell.westLon()).toFloat())
        verticals.add(viewport.screenX(cell.eastLon()).toFloat())
        horizontals.add(viewport.screenY(cell.northLat()).toFloat())
        horizontals.add(viewport.screenY(cell.southLat()).toFloat())
    }

    for (x in verticals) {
        drawLine(
            color = lineColor,
            alpha = lineAlpha,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = GRID_LINE_WIDTH_PX,
        )
    }
    for (y in horizontals) {
        drawLine(
            color = lineColor,
            alpha = lineAlpha,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = GRID_LINE_WIDTH_PX,
        )
    }

    if (!plan.showLabels()) {
        return
    }

    val labelStyle = TextStyle(
        color = GRID_LABEL_COLOR,
        fontWeight = FontWeight.SemiBold,
        fontSize = TextUnit(plan.labelFontSizePx().toFloat(), TextUnitType.Sp),
    )
    val labelBackground = if (darkMode) GRID_LABEL_BACKGROUND_DARK else GRID_LABEL_BACKGROUND_LIGHT

    for (cell in cells) {
        if (!plan.shouldShowLabel(cell) || cell.locatorLabel().isNullOrBlank()) {
            continue
        }

        /* Centred in the cell, the way the stylesheet's translate(-50%, -50%) was. */
        val centerX = viewport.screenX((cell.westLon() + cell.eastLon()) / 2.0).toFloat()
        val centerY = viewport.screenY((cell.southLat() + cell.northLat()) / 2.0).toFloat()

        val layout = textMeasurer.measure(cell.locatorLabel(), style = labelStyle)
        val x = centerX - layout.size.width / 2f
        val y = centerY - layout.size.height / 2f

        drawRect(
            color = labelBackground,
            topLeft = Offset(x - GRID_LABEL_PADDING_PX, y),
            size = Size(layout.size.width + GRID_LABEL_PADDING_PX * 2f, layout.size.height.toFloat()),
        )
        drawText(textLayoutResult = layout, topLeft = Offset(x, y))
    }
}

/* ------------------------------------------------- beam and path */

private fun DrawScope.drawBeamAndPath(
    viewport: MapViewport,
    home: Pair<Double, Double>?,
    antennaAzimuthDeg: Double,
    antennaBeamWidthDeg: Double,
    maxQrbKm: Double,
    markers: List<MapCallsignRawSnapshot>,
    selectedMarkerCallsign: String?,
    darkMode: Boolean,
) {
    if (home == null) {
        return
    }
    val (homeLat, homeLon) = home

    if (antennaBeamWidthDeg > 0.0 && maxQrbKm > 0.0) {
        val polygon = Path()
        buildBeamPolygon(homeLat, homeLon, antennaAzimuthDeg, antennaBeamWidthDeg, maxQrbKm)
            .forEachIndexed { index, point ->
                val x = viewport.screenX(point.second).toFloat()
                val y = viewport.screenY(point.first).toFloat()
                if (index == 0) polygon.moveTo(x, y) else polygon.lineTo(x, y)
            }
        polygon.close()

        drawPath(path = polygon, color = BEAM_COLOR, alpha = BEAM_FILL_ALPHA, style = Fill)
        drawPath(path = polygon, color = BEAM_COLOR, style = Stroke(width = BEAM_STROKE_WIDTH_PX))
    }

    val selected = markers.firstOrNull {
        it.callSignRaw() == selectedMarkerCallsign && it.hasUsablePosition()
    }
    if (selected != null) {
        drawLine(
            color = if (darkMode) CONNECTION_COLOR_DARK else CONNECTION_COLOR_LIGHT,
            alpha = CONNECTION_ALPHA,
            start = Offset(viewport.screenX(homeLon).toFloat(), viewport.screenY(homeLat).toFloat()),
            end = Offset(
                viewport.screenX(selected.longitudeDeg()).toFloat(),
                viewport.screenY(selected.latitudeDeg()).toFloat(),
            ),
            strokeWidth = CONNECTION_STROKE_WIDTH_PX,
            pathEffect = PathEffect.dashPathEffect(CONNECTION_DASH_PX),
        )
    }

    /*
     * The Leaflet map drew no home marker; the beam's apex was the only hint of where
     * home is, and with the beam switched off there was none. A small dot is kept here
     * deliberately.
     */
    val homeX = viewport.screenX(homeLon).toFloat()
    val homeY = viewport.screenY(homeLat).toFloat()
    drawCircle(color = BEAM_COLOR, radius = HOME_RADIUS_PX, center = Offset(homeX, homeY))
    drawCircle(color = Color.White, radius = HOME_INNER_RADIUS_PX, center = Offset(homeX, homeY))
}

/* ---------------------------------------------------------- stations */

private fun DrawScope.drawStations(
    viewport: MapViewport,
    clusterResult: StationMapClusterer.Result,
    selectedMarkerCallsign: String?,
    textMeasurer: TextMeasurer,
    darkMode: Boolean,
) {
    for (cluster in clusterResult.clusters()) {
        drawClusterBubble(
            x = viewport.screenX(cluster.centerLon).toFloat(),
            y = viewport.screenY(cluster.centerLat).toFloat(),
            count = cluster.size(),
            containsWorked = cluster.getMarkers().any { it.worked() },
            textMeasurer = textMeasurer,
        )
    }

    for (marker in clusterResult.individual()) {
        val x = viewport.screenX(marker.longitudeDeg()).toFloat()
        val y = viewport.screenY(marker.latitudeDeg()).toFloat()
        val selected = marker.callSignRaw() == selectedMarkerCallsign

        drawStationDot(x, y, selected, marker.worked(), marker.warningToMyDirection())
        drawStationLabel(x, y, marker.markerLabel(), marker.warningToMyDirection(), textMeasurer, darkMode)
    }
}

/** 12 px across, 16 px when selected — the sizes `.station-dot` declared. */
private fun DrawScope.drawStationDot(
    x: Float,
    y: Float,
    selected: Boolean,
    worked: Boolean,
    warning: Boolean,
) {
    val radius = if (selected) SELECTED_DOT_RADIUS_PX else DOT_RADIUS_PX
    val borderWidth = if (selected) SELECTED_DOT_BORDER_PX else DOT_BORDER_PX
    val borderColor = when {
        selected -> DOT_BORDER_SELECTED
        warning -> DOT_BORDER_WARNING
        worked -> DOT_BORDER_WORKED
        else -> DOT_BORDER_DEFAULT
    }

    drawCircle(color = DOT_FILL, radius = radius, center = Offset(x, y))
    drawCircle(
        color = borderColor,
        radius = radius - borderWidth / 2f,
        center = Offset(x, y),
        style = Stroke(width = borderWidth),
    )
}

/** Up and to the right of the dot, as `.station-label` placed it. */
private fun DrawScope.drawStationLabel(
    x: Float,
    y: Float,
    label: String,
    warning: Boolean,
    textMeasurer: TextMeasurer,
    darkMode: Boolean,
) {
    if (label.isBlank()) {
        return
    }

    val layout = textMeasurer.measure(
        label,
        style = TextStyle(
            color = if (warning) DOT_BORDER_WARNING else if (darkMode) LABEL_TEXT_DARK else LABEL_TEXT_LIGHT,
            fontWeight = FontWeight.Bold,
            fontSize = TextUnit(LABEL_FONT_SIZE_PX, TextUnitType.Sp),
        ),
    )

    val boxLeft = x + LABEL_OFFSET_X_PX
    val boxTop = y + LABEL_OFFSET_Y_PX
    val boxWidth = layout.size.width + LABEL_PADDING_X_PX * 2f
    val boxHeight = layout.size.height + LABEL_PADDING_Y_PX * 2f

    drawRect(
        color = if (darkMode) LABEL_BACKGROUND_DARK else LABEL_BACKGROUND_LIGHT,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
    )
    drawRect(
        color = if (warning) DOT_BORDER_WARNING else if (darkMode) LABEL_BORDER_DARK else LABEL_BORDER_LIGHT,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        style = Stroke(width = LABEL_BORDER_WIDTH_PX),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(boxLeft + LABEL_PADDING_X_PX, boxTop + LABEL_PADDING_Y_PX),
    )
}

/** 32, 36 or 42 px across, by the thresholds `buildClusterMarkerHtml` used. */
private fun DrawScope.drawClusterBubble(
    x: Float,
    y: Float,
    count: Int,
    containsWorked: Boolean,
    textMeasurer: TextMeasurer,
) {
    val radius = clusterRadiusPx(count)
    val borderColor = if (containsWorked) DOT_BORDER_WORKED else DOT_BORDER_DEFAULT

    drawCircle(color = CLUSTER_FILL, radius = radius, center = Offset(x, y))
    drawCircle(
        color = borderColor,
        radius = radius - DOT_BORDER_PX / 2f,
        center = Offset(x, y),
        style = Stroke(width = DOT_BORDER_PX),
    )

    val layout = textMeasurer.measure(
        count.toString(),
        style = TextStyle(
            color = CLUSTER_TEXT,
            fontWeight = FontWeight.ExtraBold,
            fontSize = TextUnit(clusterFontSizePx(count), TextUnitType.Sp),
        ),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f),
    )
}

/* ------------------------------------------------------------ hover */

private fun DrawScope.drawHover(
    viewport: MapViewport,
    hoverPosition: Offset?,
    clusterResult: StationMapClusterer.Result,
    textMeasurer: TextMeasurer,
) {
    val position = hoverPosition ?: return

    val clusterUnderPointer = clusterResult.clusters().firstOrNull { cluster ->
        within(
            position,
            viewport.screenX(cluster.centerLon).toFloat(),
            viewport.screenY(cluster.centerLat).toFloat(),
            clusterRadiusPx(cluster.size()),
        )
    }
    val markerUnderPointer = clusterResult.individual().firstOrNull { marker ->
        within(
            position,
            viewport.screenX(marker.longitudeDeg()).toFloat(),
            viewport.screenY(marker.latitudeDeg()).toFloat(),
            MARKER_HIT_RADIUS_PX,
        )
    }

    val text = when {
        clusterUnderPointer != null -> MapMarkerClusterTooltip.build(clusterUnderPointer)

        markerUnderPointer != null -> markerUnderPointer.tooltipText()

        else -> return
    }

    val layout: TextLayoutResult = textMeasurer.measure(
        text,
        style = TextStyle(color = Color.Black, fontSize = TextUnit(LABEL_FONT_SIZE_PX, TextUnitType.Sp)),
    )
    val boxWidth = layout.size.width + TOOLTIP_PADDING_PX * 2f
    val boxHeight = layout.size.height + TOOLTIP_PADDING_PX * 2f

    /*
     * Flipped away from the edge it would cross, then held inside the canvas. Flipping
     * alone is not enough: a cluster tooltip is as tall as it has lines, and one taller
     * than the canvas would otherwise be placed at a negative offset and cover the map
     * it exists to explain.
     */
    val left = (
        if (position.x + TOOLTIP_OFFSET_PX + boxWidth > size.width) {
            position.x - TOOLTIP_OFFSET_PX - boxWidth
        } else {
            position.x + TOOLTIP_OFFSET_PX
        }
        ).coerceIn(0f, (size.width - boxWidth).coerceAtLeast(0f))
    val top = (
        if (position.y + TOOLTIP_OFFSET_PX + boxHeight > size.height) {
            position.y - TOOLTIP_OFFSET_PX - boxHeight
        } else {
            position.y + TOOLTIP_OFFSET_PX
        }
        ).coerceIn(0f, (size.height - boxHeight).coerceAtLeast(0f))

    drawRect(color = TOOLTIP_BACKGROUND, topLeft = Offset(left, top), size = Size(boxWidth, boxHeight))
    drawRect(
        color = TOOLTIP_BORDER,
        topLeft = Offset(left, top),
        size = Size(boxWidth, boxHeight),
        style = Stroke(width = 1f),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(left + TOOLTIP_PADDING_PX, top + TOOLTIP_PADDING_PX),
    )
}

private fun DrawScope.drawWorkedGrossFields(
    viewport: MapViewport,
    workedGrossFields: Set<String>,
) {
    visibleWorkedGrossFieldCells(viewport, workedGrossFields).forEach { cell ->
        val left = viewport.screenX(cell.westLon()).toFloat()
        val right = viewport.screenX(cell.eastLon()).toFloat()
        val top = viewport.screenY(cell.northLat()).toFloat()
        val bottom = viewport.screenY(cell.southLat()).toFloat()
        val topLeft = Offset(minOf(left, right), minOf(top, bottom))
        val cellSize = Size(kotlin.math.abs(right - left), kotlin.math.abs(bottom - top))
        drawRect(
            color = WORKED_GRID_FILL,
            topLeft = topLeft,
            size = cellSize,
        )
        drawRect(
            color = WORKED_GRID_BORDER,
            topLeft = topLeft,
            size = cellSize,
            style = Stroke(width = WORKED_GRID_BORDER_WIDTH_PX),
        )
    }
}

/** Worked four-character cells visible in the current viewport, independent of markers. */
internal fun visibleWorkedGrossFieldCells(
    viewport: MapViewport,
    workedGrossFields: Set<String>,
): List<MaidenheadGridUtils.GridCell> {
    if (workedGrossFields.isEmpty()) return emptyList()
    return MaidenheadGridUtils.buildVisibleCells(
        viewport.southLat(),
        viewport.westLon(),
        viewport.northLat(),
        viewport.eastLon(),
        MaidenheadGridUtils.GridPrecision.SQUARE_4,
    ).filter { it.locatorLabel() in workedGrossFields }
}

/* ----------------------------------------------------------- helpers */

private val MapMarkerCluster.centerLon: Double get() = getCenterLon()
private val MapMarkerCluster.centerLat: Double get() = getCenterLat()

private fun within(point: Offset, x: Float, y: Float, radius: Float): Boolean {
    val dx = point.x - x
    val dy = point.y - y
    return dx * dx + dy * dy <= radius * radius
}

private fun clusterRadiusPx(count: Int): Float = when {
    count >= CLUSTER_LARGE_FROM -> CLUSTER_RADIUS_LARGE_PX
    count >= CLUSTER_MEDIUM_FROM -> CLUSTER_RADIUS_MEDIUM_PX
    else -> CLUSTER_RADIUS_SMALL_PX
}

private fun clusterFontSizePx(count: Int): Float = when {
    count >= CLUSTER_LARGE_FROM -> 15f
    count >= CLUSTER_MEDIUM_FROM -> 14f
    else -> 13f
}

/** Null rather than a guess when the operator has no usable locator configured. */
private fun homePosition(ownLocator6: String): Pair<Double, Double>? {
    if (ownLocator6.length < 4) {
        return null
    }
    return try {
        val location = kst4contest.locatorUtils.Location(ownLocator6)
        Pair(location.latitude.toDegrees(), location.longitude.toDegrees())
    } catch (malformedLocator: Exception) {
        null
    }
}

fun calculateDestinationPoint(
    startLatDeg: Double,
    startLonDeg: Double,
    bearingDeg: Double,
    distanceKm: Double,
): Pair<Double, Double> {
    val earthRadiusKm = 6371.009
    val angularDistance = distanceKm / earthRadiusKm
    val bearingRad = Math.toRadians(bearingDeg)
    val startLatRad = Math.toRadians(startLatDeg)
    val startLonRad = Math.toRadians(startLonDeg)

    val destLatRad = kotlin.math.asin(
        kotlin.math.sin(startLatRad) * kotlin.math.cos(angularDistance) +
            kotlin.math.cos(startLatRad) * kotlin.math.sin(angularDistance) * kotlin.math.cos(bearingRad)
    )
    val destLonRad = startLonRad + kotlin.math.atan2(
        kotlin.math.sin(bearingRad) * kotlin.math.sin(angularDistance) * kotlin.math.cos(startLatRad),
        kotlin.math.cos(angularDistance) - kotlin.math.sin(startLatRad) * kotlin.math.sin(destLatRad)
    )

    return Pair(Math.toDegrees(destLatRad), Math.toDegrees(destLonRad))
}

fun buildBeamPolygon(
    startLatDeg: Double,
    startLonDeg: Double,
    centerAzimuthDeg: Double,
    beamWidthDeg: Double,
    radiusKm: Double,
): List<Pair<Double, Double>> {
    val polygon = mutableListOf(Pair(startLatDeg, startLonDeg))

    val segmentCount = maxOf(12, ceil(beamWidthDeg / 4.0).toInt())
    val angleStep = beamWidthDeg / segmentCount
    val startAzimuth = centerAzimuthDeg - beamWidthDeg / 2.0

    for (segment in 0..segmentCount) {
        var azimuth = startAzimuth + segment * angleStep
        if (azimuth < 0.0) azimuth += 360.0
        if (azimuth >= 360.0) azimuth -= 360.0
        polygon.add(calculateDestinationPoint(startLatDeg, startLonDeg, azimuth, radiusKm))
    }

    polygon.add(Pair(startLatDeg, startLonDeg))
    return polygon
}

/* --------------------------------------------------------- constants
 *
 * Every size is in canvas pixels and every colour is the one the Leaflet stylesheet
 * declared, so the two maps can be held side by side.
 */

private const val TILE_SIZE = 256.0
private const val DARK_TILE_ALPHA = 0.82f
private const val ZOOM_STEP_PER_WHEEL_NOTCH = 0.5

private val MAP_BACKGROUND_LIGHT = Color(0xFFEDE9DF)
private val MAP_BACKGROUND_DARK = Color(0xFF23282D)

private val GRID_LINE_DARK = Color(0xFFE1E7EC)
private val GRID_LINE_LIGHT = Color(0xFF46586C)
private const val GRID_LINE_ALPHA_DARK = 0.48f
private const val GRID_LINE_ALPHA_LIGHT = 0.56f
private const val GRID_LINE_WIDTH_PX = 1.4f

private val GRID_LABEL_COLOR = Color(0xFF63067A)
private val GRID_LABEL_BACKGROUND_LIGHT = Color.White.copy(alpha = 0.18f)
private val GRID_LABEL_BACKGROUND_DARK = Color(0xFF22262B).copy(alpha = 0.20f)
private val WORKED_GRID_FILL = Color.Black.copy(alpha = 0.30f)
private val WORKED_GRID_BORDER = Color.Black
private const val WORKED_GRID_BORDER_WIDTH_PX = 3f
private const val GRID_LABEL_PADDING_PX = 3f

private val DOT_FILL = Color(0xFF1D1D1D)
private val DOT_BORDER_DEFAULT = Color(0xFF4DA6FF)
private val DOT_BORDER_WORKED = Color(0xFFFFD24D)
private val DOT_BORDER_WARNING = Color(0xFF00FF66)
private val DOT_BORDER_SELECTED = Color(0xFFFF9900)
private const val DOT_RADIUS_PX = 6f
private const val DOT_BORDER_PX = 2f
private const val SELECTED_DOT_RADIUS_PX = 8f
private const val SELECTED_DOT_BORDER_PX = 3f
private const val MARKER_HIT_RADIUS_PX = 10f

private val LABEL_BACKGROUND_LIGHT = Color(0xFFF8F8F8).copy(alpha = 0.95f)
private val LABEL_BACKGROUND_DARK = Color(0xFF24282D).copy(alpha = 0.96f)
private val LABEL_TEXT_LIGHT = Color(0xFF1C1C1C)
private val LABEL_TEXT_DARK = Color(0xFFF1F3F5)
private val LABEL_BORDER_LIGHT = Color.Black.copy(alpha = 0.20f)
private val LABEL_BORDER_DARK = Color.White.copy(alpha = 0.18f)
private const val LABEL_FONT_SIZE_PX = 12f
private const val LABEL_BORDER_WIDTH_PX = 1f
private const val LABEL_PADDING_X_PX = 5f
private const val LABEL_PADDING_Y_PX = 2f
private const val LABEL_OFFSET_X_PX = 10f
private const val LABEL_OFFSET_Y_PX = -22f

private val CLUSTER_FILL = Color(0xFF202428)
private val CLUSTER_TEXT = Color(0xFFF1F3F5)
private const val CLUSTER_MEDIUM_FROM = 8
private const val CLUSTER_LARGE_FROM = 20
private const val CLUSTER_RADIUS_SMALL_PX = 16f
private const val CLUSTER_RADIUS_MEDIUM_PX = 18f
private const val CLUSTER_RADIUS_LARGE_PX = 21f

private val BEAM_COLOR = Color(0xFFFF4D4D)
private const val BEAM_FILL_ALPHA = 0.12f
private const val BEAM_STROKE_WIDTH_PX = 2f
private const val HOME_RADIUS_PX = 6f
private const val HOME_INNER_RADIUS_PX = 2f

private val CONNECTION_COLOR_DARK = Color(0xFF2FD7FF)
private val CONNECTION_COLOR_LIGHT = Color(0xFF00A4CF)
private const val CONNECTION_ALPHA = 0.85f
private const val CONNECTION_STROKE_WIDTH_PX = 2f
private val CONNECTION_DASH_PX = floatArrayOf(6f, 6f)

private val TOOLTIP_BACKGROUND = Color.White.copy(alpha = 0.92f)
private val TOOLTIP_BORDER = Color.Black
private const val TOOLTIP_PADDING_PX = 4f
private const val TOOLTIP_OFFSET_PX = 12f
