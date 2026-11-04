package kst4contest.view.compose.map

import kst4contest.view.compose.WindowPlacementOnScreen
import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.view.ApplicationRuntimeLauncher
import kst4contest.view.compose.PaletteStore
import kst4contest.view.compose.ComposeWindowHost
import kst4contest.view.compose.MainWindowState
import kst4contest.view.compose.StationIdentity
import kst4contest.view.compose.StationContextMenuState
import kst4contest.view.compose.StationContextOverlays
import kst4contest.view.compose.StationSelectionSource
import kst4contest.view.map.MapCallsignRawSnapshotBuilder
import kst4contest.view.map.PathAnalysisResult
import kst4contest.view.map.MapViewportNavigation
import kst4contest.view.map.StationMapStatusText
import kst4contest.view.map.TileFetcher
import java.util.EnumSet

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)
object StationMapWindow {

    private val host = ComposeWindowHost("Map")
    var isDarkThemeState = androidx.compose.runtime.mutableStateOf(false)

    fun show(
        mainWindowState: MainWindowState,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        /**
         * The palette of the active profile, handed to the theme. Null draws the shipped
         * palette.
         */
        paletteStore: PaletteStore? = null,
        /**
         * The interface language of the active profile, handed to the window. Null draws the
         * base language.
         */
        languageStore: kst4contest.view.i18n.LanguageStore? = null,
        widthDp: Float,
        heightDp: Float,
        onResized: (Float, Float) -> Unit,
    ) {
        isDarkThemeState.value = darkMode
        host.show(
            title = { kst4contest.view.i18n.CurrentStrings.get().mapTitle },
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            languageStore = languageStore,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
            alwaysOnTop = mainWindowState.prefs.isGUIstationMapAlwaysOnTop,
        ) { _ ->
            val window = this.window
            /*
             * Never larger than the smallest screen: AWT enforces a minimum size even past
             * the edge, and some operators work on 640x480.
             */
            androidx.compose.runtime.LaunchedEffect(window, mainWindowState.prefs.isGUIstationMapAlwaysOnTop) {
                window?.minimumSize = WindowPlacementOnScreen.minimumSizeOnScreens(900, 600)
                window?.isAlwaysOnTop = mainWindowState.prefs.isGUIstationMapAlwaysOnTop
            }
            val tileFetcher = remember { TileFetcher() }

            /*
             * Released when the window closes. The fetcher owns a thread pool, and a
             * live pool thread is a GC root: without this, every open of this window
             * leaves its whole tile cache reachable for the rest of the session.
             */
            androidx.compose.runtime.DisposableEffect(tileFetcher) {
                onDispose { tileFetcher.close() }
            }

            Content(
                mainWindowState = mainWindowState,
                tileFetcher = tileFetcher,
                onAlwaysOnTopChanged = { enabled -> window?.isAlwaysOnTop = enabled },
            )
        } // host.show content
    }

    /**
     * The window's body, without the window.
     *
     * Split out of [show] so it can be drawn headless in a test, with a tile fetcher that
     * stays off the network and the user's tile cache, a roster that is not the running
     * application's, and a [MapState] the test can read. The defaults are exactly what
     * [show] used inline before the split.
     *
     * @param chatMembers the roster a marker click looks its station up in; a marker stands
     *        for a raw callsign, and this is where its category variants come from
     * @param mapState where the map is looking
     * @param pathAnalysis asked for an explicit selection; null disables only the terrain
     *        analysis while viewport navigation remains available
     */
    @Composable
    internal fun Content(
        mainWindowState: MainWindowState,
        tileFetcher: TileFetcher,
        chatMembers: () -> List<kst4contest.model.ChatMember> = {
            ApplicationRuntimeLauncher.getCurrent()?.chatController?.lst_chatMemberList?.snapshot().orEmpty()
        },
        mapState: MapState = remember { MapState() },
        pathAnalysis: () -> MapPathAnalysis? = ::runtimePathAnalysis,
        onAlwaysOnTopChanged: (Boolean) -> Unit = { },
    ) {
            val strings = LocalStrings.current

            /*
             * The fetcher is plain Java state, so nothing about it recomposes on its
             * own. Polled rather than wired through a callback: this drives one line of
             * text, and a callback from a background fetch thread into Compose state is
             * more machinery than the question deserves.
             */
            var tilesUnavailable by remember { mutableStateOf(false) }
            LaunchedEffect(tileFetcher) {
                while (true) {
                    kotlinx.coroutines.delay(1_000)
                    tilesUnavailable = tileFetcher.isTileSourceUnavailable()
                }
            }
            val prefs = mainWindowState.prefs
            
            var groupingEnabled by remember { mutableStateOf(prefs.isGUIstationMapClusteringEnabled) }
            var pathAnalysisVisible by remember { mutableStateOf(prefs.isGUIstationMapPathAnalysisVisible) }
            var alwaysOnTop by remember { mutableStateOf(prefs.isGUIstationMapAlwaysOnTop) }
            val stationContextMenu = remember(mainWindowState) {
                StationContextMenuState(mainWindowState::selectStation)
            }
            
            val selectedMember = mainWindowState.selectedStation.selected
            
            val ownLocator6 by remember { androidx.compose.runtime.mutableStateOf(mainWindowState.prefs.stn_loginLocatorMainCat) }
            val antennaAzimuthDeg = mainWindowState.antennaQtf.value
            val antennaBeamWidthDeg = mainWindowState.mapDisplaySettings.antennaBeamWidthDeg
            val maxQrbKm = mainWindowState.mapDisplaySettings.maxQrbKm

            /*
             * The markers are the stations the list shows, not all of them (#21): the
             * JavaFX map took the sorted, filtered list (StationMapBridge.refreshNow), and
             * the status line below already says "filtered view active". A selected
             * station that a filter hides gets no marker either, as it did there.
             */
            val markers by remember(
                mainWindowState.stations.rows,
                mainWindowState.stations.rowFilter,
                mainWindowState.stations.contentRevision,
                selectedMember,
                mainWindowState.activeBands,
            ) {
                derivedStateOf {
                    val builder = MapCallsignRawSnapshotBuilder()
                    val activeBands = mainWindowState.activeBands
                    
                    val bandSet = EnumSet.noneOf(Band::class.java)
                    if (activeBands.isNotEmpty()) {
                        bandSet.addAll(activeBands)
                    }

                    builder.buildSnapshots(
                        mainWindowState.stations.filteredRows,
                        selectedMember,
                        bandSet
                    )
                }
            }

            /*
             * Selection is independent of the filtered marker set. A selected station
             * remains a target even when an active filter removes its normal marker; it is
             * drawn in a separate selection layer and never enters marker counts or clusters.
             */
            val selectedTarget by remember(
                selectedMember,
                mainWindowState.stations.contentRevision,
                mainWindowState.activeBands,
            ) {
                derivedStateOf {
                    selectedMember?.let { selected ->
                        val variants = chatMembers().filter { it.callSignRaw == selected.callSignRaw }
                            .ifEmpty { listOf(selected) }
                        val bands = EnumSet.noneOf(Band::class.java).apply {
                            addAll(mainWindowState.activeBands)
                        }
                        MapCallsignRawSnapshotBuilder()
                            .buildSnapshots(variants, selected, bands)
                            .firstOrNull()
                    }
                }
            }

            var pathAnalysisResult by remember { mutableStateOf<PathAnalysisResult?>(null) }
            var automaticAnalysisRevision by remember { mutableStateOf(0L) }

            // Read through updated state so a caller passing a new lambda is never served a stale one.
            val currentPathAnalysis by rememberUpdatedState(pathAnalysis)

            LaunchedEffect(mainWindowState.stationSelection.explicitSelectionRevision) {
                val automaticRequest = ++automaticAnalysisRevision
                val explicitRevisionAtStart = mainWindowState.mapReachability.requestRevision
                if (selectedMember == null) {
                    pathAnalysisResult = null
                    return@LaunchedEffect
                }

                val selectedSnapshot = selectedTarget
                if (selectedSnapshot == null) {
                    // No valid locator: no request and no misleading loading state.
                    pathAnalysisResult = null
                    return@LaunchedEffect
                }

                val analysis = currentPathAnalysis()
                if (analysis != null) {
                    pathAnalysisResult = PathAnalysisResult.loading(
                        prefs.stn_loginLocatorMainCat,
                        selectedSnapshot.locator6(),
                        selectedSnapshot.callSignRaw()
                    )
                    
                    analysis.request(selectedMember, selectedSnapshot) { result ->
                        if (automaticAnalysisRevision == automaticRequest &&
                            mainWindowState.mapReachability.requestRevision == explicitRevisionAtStart
                        ) {
                            pathAnalysisResult = result
                        }
                    }
                }
            }

            var mapCanvasSize by remember { mutableStateOf(Size.Zero) }
            var consumedNavigationRevision by remember(mapState) { mutableStateOf(Long.MIN_VALUE) }
            val navigationRevision = mainWindowState.mapNavigation.requestRevision
            val navigationRequest = mainWindowState.mapNavigation.request
            val navigationPaddingPx = with(LocalDensity.current) { 40.dp.toPx().toDouble() }

            LaunchedEffect(
                navigationRevision,
                navigationRequest,
                mapCanvasSize,
                selectedTarget,
                ownLocator6,
            ) {
                if (navigationRevision == consumedNavigationRevision ||
                    mapCanvasSize.width <= 0f || mapCanvasSize.height <= 0f
                ) {
                    return@LaunchedEffect
                }

                val home = runCatching { kst4contest.locatorUtils.Location(ownLocator6) }.getOrNull()
                val current = mapState.viewportFor(mapCanvasSize.width, mapCanvasSize.height)
                val next = when (val request = navigationRequest) {
                    is MapNavigationRequest.FitSelectedPath -> {
                        val target = selectedTarget
                        if (home == null || target == null || !target.hasUsablePosition() ||
                            request.identity != StationIdentity.of(selectedMember)
                        ) {
                            null
                        } else {
                            MapViewportNavigation.fitPath(
                                current,
                                home.latitude.toDegrees(),
                                home.longitude.toDegrees(),
                                target.latitudeDeg(),
                                target.longitudeDeg(),
                                navigationPaddingPx,
                            )
                        }
                    }

                    is MapNavigationRequest.CenterOnQtf -> {
                        if (home == null) null else MapViewportNavigation.centreOnBearing(
                            current,
                            home.latitude.toDegrees(),
                            home.longitude.toDegrees(),
                            request.qtfDegrees,
                            QTF_FORWARD_CENTRE_FRACTION,
                        )
                    }

                    MapNavigationRequest.ResetToHome -> {
                        if (home == null) null else MapViewportNavigation.centreOnHome(
                            current,
                            home.latitude.toDegrees(),
                            home.longitude.toDegrees(),
                        )
                    }

                    null -> null
                }
                consumedNavigationRevision = navigationRevision
                if (next != null) {
                    mapState.applyFrom(next)
                }
            }

            val explicitReachabilityResult = mainWindowState.mapReachability.result
            LaunchedEffect(mainWindowState.mapReachability.requestRevision, explicitReachabilityResult) {
                if (explicitReachabilityResult != null) {
                    automaticAnalysisRevision++
                    pathAnalysisResult = explicitReachabilityResult
                    pathAnalysisVisible = true
                }
            }

            val workedGrossFields = if (mainWindowState.stationFilter.gridColouring) {
                mainWindowState.stations.contentRevision
                mainWindowState.workedGrossFieldsProvider()
            } else {
                emptySet()
            }
            val spatialFilterOverlay = SpatialFilterOverlayState(
                qtfEnabled = mainWindowState.stationFilter.qtfEnabled,
                qtfDegrees = mainWindowState.stationFilter.qtfDegrees,
                beamWidthDeg = antennaBeamWidthDeg,
                minQrbEnabled = mainWindowState.stationFilter.minQrbEnabled,
                minQrbKm = mainWindowState.stationFilter.minQrbKm,
                maxQrbEnabled = mainWindowState.stationFilter.maxQrbEnabled,
                maxQrbKm = mainWindowState.stationFilter.maxQrbKm,
            )
            val mapHome = remember(ownLocator6) { homePosition(ownLocator6) }
            val showAntennaOverlay = isAntennaSectorDrawable(
                mapHome,
                antennaAzimuthDeg ?: 0.0,
                antennaBeamWidthDeg,
                maxQrbKm,
            )
            val showSpatialFilterOverlay = mapHome != null && spatialFilterOverlay.hasDrawableGeometry
            val isDarkTheme = isDarkThemeState.value

            androidx.compose.material3.Surface(
                color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize()
            ) {
            Column(Modifier.fillMaxSize()) {
                /*
                 * The status line takes the room, so the buttons sit at the right edge
                 * the way the JavaFX header had them (HBox.setHgrow on the label). The
                 * controls are the shared ones: a raw Material button paints itself in
                 * the theme's primary colour, which in the evening sheet is a bright
                 * green, and the map window was the only one wearing it.
                 */
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    val statusText = StationMapStatusText.build(
                        markers.size,
                        mainWindowState.stationFilter.activeFilters.isNotEmpty() ||
                            mainWindowState.stationFilter.searchText.isNotBlank(),
                        selectedTarget,
                    )

                    /*
                     * The tail of this line carries the bands and the frequency, and on a
                     * narrow window that is exactly what the ellipsis eats. The JavaFX
                     * header put the same text into a tooltip for that reason
                     * (statusTooltip, StationMapView.updateStatusLabel).
                     */
                    androidx.compose.foundation.TooltipArea(
                        tooltip = { kst4contest.view.compose.TooltipCard(statusText) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = statusText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    MapOverlayLegend(
                        showAntenna = showAntennaOverlay,
                        showSpatialFilters = showSpatialFilterOverlay,
                        darkMode = isDarkTheme,
                    )

                    if (selectedMember != null) {
                        kst4contest.view.compose.Form.button(strings.mapTriggerClusterSpot) {
                            ApplicationRuntimeLauncher.getCurrent()?.chatController
                                ?.dxClusterServer?.broadcastSingleDXClusterEntryToLoggers(selectedMember)
                        }
                    }

                    kst4contest.view.compose.Form.button(strings.mapResetView) {
                        mainWindowState.selectStation(null, StationSelectionSource.RESET_VIEW)
                    }

                    kst4contest.view.compose.Form.button(strings.filterReset) {
                        mainWindowState.resetStationFilters()
                    }

                    androidx.compose.foundation.TooltipArea(
                        tooltip = { kst4contest.view.compose.TooltipCard(strings.mapGroupNearbyTooltip) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            kst4contest.view.compose.CompactCheckbox(
                                checked = groupingEnabled,
                                modifier = Modifier.testTag(GROUPING_TAG),
                            ) {
                                groupingEnabled = it
                                prefs.isGUIstationMapClusteringEnabled = it
                                mainWindowState.requestLayoutSave()
                            }
                            Text(strings.mapGroupNearby, modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        kst4contest.view.compose.CompactCheckbox(
                            checked = alwaysOnTop,
                            onCheckedChange = { enabled ->
                                alwaysOnTop = enabled
                                prefs.isGUIstationMapAlwaysOnTop = enabled
                                onAlwaysOnTopChanged(enabled)
                                mainWindowState.requestLayoutSave()
                            },
                            modifier = Modifier.testTag(ALWAYS_ON_TOP_TAG),
                        )
                        Text(strings.mapAlwaysOnTop, modifier = Modifier.padding(start = 4.dp))
                    }

                    if (!pathAnalysisVisible) {
                        Text(
                            text = strings.mapPathAnalysisHidden,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.alpha(0.85f)
                        )
                    }

                    kst4contest.view.compose.Form.button(
                        if (pathAnalysisVisible) strings.mapHidePathAnalysis else strings.mapShowPathAnalysis
                    ) {
                        pathAnalysisVisible = !pathAnalysisVisible
                        prefs.isGUIstationMapPathAnalysisVisible = pathAnalysisVisible
                        mainWindowState.requestLayoutSave()
                    }
                }

                // Main Layout
                Column(Modifier.weight(1f).fillMaxWidth()) {
                val mapSplitterState = androidx.compose.runtime.remember {
                    kst4contest.view.compose.SplitterState(
                        paneCount = 2,
                        stored = doubleArrayOf(0.65),
                        save = { }
                    )
                }

                val leftPane: @Composable () -> Unit = {
                    Column(Modifier.fillMaxHeight()) {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth()) {
                        ComposeStationMap(
                            markers = markers,
                            tileFetcher = tileFetcher,
                            mapState = mapState,
                            groupingEnabled = groupingEnabled,
                            selectedMarkerCallsign = selectedMember?.callSignRaw,
                            ownLocator6 = ownLocator6,
                            antennaAzimuthDeg = antennaAzimuthDeg ?: 0.0,
                            antennaBeamWidthDeg = antennaBeamWidthDeg,
                            maxQrbKm = maxQrbKm,
                            selectedTarget = selectedTarget,
                            spatialFilterOverlay = spatialFilterOverlay,
                            darkMode = isDarkTheme,
                            workedGrossFields = workedGrossFields,
                            onCanvasSizeChanged = { mapCanvasSize = it },
                            onMarkerClicked = { snapshot ->
                                val variants = chatMembers()
                                    .filter { it.callSignRaw == snapshot.callSignRaw() }
                                /*
                                 * A marker stands for a raw callsign, not a category. Keep the
                                 * category already selected when it is one of the variants.
                                 */
                                val selectedIdentity = StationIdentity.of(mainWindowState.selectedStation.selected)
                                val member = variants.firstOrNull {
                                    StationIdentity.of(it) == selectedIdentity
                                } ?: variants.firstOrNull {
                                    it.chatCategory?.categoryNumber ==
                                        prefs.loginChatCategoryMain?.categoryNumber
                                } ?: variants.singleOrNull()
                                if (member != null) {
                                    mainWindowState.selectStation(member, StationSelectionSource.MAP)
                                    // Mirror old JavaFX handleMapCallsignSelection logic: prefill /cq
                                    mainWindowState.chatInput.prepareCq(member.callSign ?: "", forceOverwrite = true)
                                }
                            },
                            onMarkerContextRequested = { snapshots, position ->
                                val callsigns = snapshots.mapTo(linkedSetOf()) { it.callSignRaw() }
                                val candidates = chatMembers().filter { it.callSignRaw in callsigns }
                                stationContextMenu.open(
                                    candidates,
                                    StationSelectionSource.MAP_CONTEXT_MENU,
                                    position,
                                )
                            },
                            modifier = Modifier.fillMaxSize().testTag(MAP_CANVAS_TAG)
                        )

                        MapOverlayControls(
                            darkMode = isDarkTheme,
                            tilesUnavailable = tilesUnavailable,
                            onZoomIn = { mapState.zoom = (mapState.zoom + 1f).coerceAtMost(MAX_MAP_ZOOM) },
                            onZoomOut = { mapState.zoom = (mapState.zoom - 1f).coerceAtLeast(MIN_MAP_ZOOM) },
                        )
                        }

                        if (pathAnalysisVisible) {
                            val points = pathAnalysisResult?.profilePoints() ?: emptyList()
                            val isDarkTheme = isDarkThemeState.value
                            PathProfileChart(
                                profilePoints = points,
                                totalDistanceKm = pathAnalysisResult?.distanceKm() ?: Double.NaN,
                                darkMode = isDarkTheme,
                                homeAntennaHeightMeters = pathAnalysisResult?.homeAntennaHeightMeters() ?: Double.NaN,
                                targetAntennaHeightMeters = pathAnalysisResult?.targetAntennaHeightMeters() ?: Double.NaN,
                                analysisFrequencyMHz = pathAnalysisResult?.analysisFrequencyMHz() ?: Double.NaN,
                                effectiveEarthRadiusFactor = pathAnalysisResult?.effectiveEarthRadiusFactor() ?: kst4contest.view.map.PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                                horizonSummary = pathAnalysisResult?.horizonSummary() ?: kst4contest.view.map.PathHorizonSummary.empty(),
                                obstructionSummary = pathAnalysisResult?.obstructionSummary() ?: kst4contest.view.map.PathObstructionSummary.empty(),
                                modifier = Modifier.fillMaxWidth().height(210.dp)
                            )
                        }
                    }
                }

                val rightPane: @Composable () -> Unit = {
                    val scrollState = rememberScrollState()
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxHeight()) {
                        PathAnalysisDetails(
                            result = pathAnalysisResult,
                            darkMode = isDarkThemeState.value,
                            modifier = Modifier.fillMaxHeight()
                                .padding(horizontal = 4.dp)
                                .padding(end = 12.dp)
                                .verticalScroll(scrollState),
                        )
                        androidx.compose.foundation.VerticalScrollbar(
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).fillMaxHeight(),
                            adapter = androidx.compose.foundation.rememberScrollbarAdapter(scrollState)
                        )
                    }
                }

                if (pathAnalysisVisible) {
                    kst4contest.view.compose.SplitterPane(
                        state = mapSplitterState,
                        orientation = kst4contest.view.compose.SplitterOrientation.HORIZONTAL,
                        panes = listOf(leftPane, rightPane),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize()) {
                        leftPane()
                    }
                }
                } // main layout column
            } // window column
            } // Surface
            StationContextOverlays(stationContextMenu, mainWindowState)
    } // Content

    /** Marks the map canvas so a test can click a marker on it. */
    internal const val MAP_CANVAS_TAG = "station-map-canvas"
    internal const val GROUPING_TAG = "station-map-grouping"
    internal const val ALWAYS_ON_TOP_TAG = "station-map-always-on-top"
    private const val QTF_FORWARD_CENTRE_FRACTION = 0.25

    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) {
        isDarkThemeState.value = darkMode
        host.applyDarkMode(darkMode)
    }

    fun hide() {
        host.close()
    }
    
    fun isShowing(): Boolean = host.isOpen
    
    fun toggle(
        mainWindowState: MainWindowState,
        paletteStore: PaletteStore? = null,
        languageStore: kst4contest.view.i18n.LanguageStore? = null,
    ) {
        if (isShowing()) {
            hide()
        } else {
            show(
                mainWindowState = mainWindowState,
                darkMode = mainWindowState.prefs.isGUI_darkModeActive,
                baseFontSizeSp = 12f,
                paletteStore = paletteStore,
                languageStore = languageStore,
                widthDp = 1000f,
                heightDp = 700f,
                onResized = { w, h -> }
            )
        }
    }
}

/**
 * Asks for the path analysis of the station selected on the map.
 *
 * The seam between the map window and the running application: the window pans to the
 * station and starts the analysis only when there is one to ask, which a test supplies
 * without building a chat controller.
 */
internal fun interface MapPathAnalysis {
    fun request(
        member: kst4contest.model.ChatMember,
        snapshot: kst4contest.view.map.MapCallsignRawSnapshot?,
        onResult: (PathAnalysisResult) -> Unit,
    )
}

/**
 * The running application's reachability service, or null before there is one.
 *
 * Looked up on every call, as the window did inline: a profile switch replaces the runtime.
 * No band override -- the map always analyses on the automatically resolved band.
 */
private fun runtimePathAnalysis(): MapPathAnalysis? {
    val chatController = ApplicationRuntimeLauncher.getCurrent()?.chatController ?: return null

    return MapPathAnalysis { member, snapshot, onResult ->
        chatController.reachabilityService.requestPathAnalysisForMap(member, snapshot, null) { result ->
            onResult(result)
        }
    }
}
