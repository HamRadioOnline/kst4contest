package kst4contest.view.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups stations that sit on top of each other on screen.
 *
 * The rules come from the Leaflet map and are kept as they were, because an operator
 * has learned them: grouping is by screen cell rather than by distance, a cell holds a
 * bubble only from three stations upwards, grouping stops once the map is zoomed in far
 * enough to show everything, and two kinds of station are never hidden in a bubble.
 *
 * Screen based and not locator based on purpose. The problem being solved is too many
 * labels in one part of the window, and that is a question about pixels.
 *
 * The first Compose port replaced this with nearest-neighbour grouping at a fixed 50 px
 * from two stations upwards and never switched it off, which put a "2" bubble over
 * most of the map.
 */
public final class StationMapClusterer {

    /**
     * Two nearby stations are still readable and should stay individually clickable, so
     * a bubble starts at three.
     */
    public static final int MIN_STATIONS_PER_CLUSTER = 3;

    /** From this zoom upwards the map shows every station. */
    public static final double DISABLE_AT_ZOOM = 8.0;

    private static final double CELL_SIZE_HIGH_ZOOM_PX = 55.0;
    private static final double CELL_SIZE_MEDIUM_ZOOM_PX = 70.0;
    private static final double CELL_SIZE_LOW_ZOOM_PX = 95.0;
    private static final double CELL_SIZE_VERY_LOW_ZOOM_PX = 125.0;

    private StationMapClusterer() {
    }

    /**
     * What the map should draw: bubbles for the crowded cells, single markers for
     * everything else.
     *
     * @param clusters stations grouped into a bubble; never fewer than
     *        {@link #MIN_STATIONS_PER_CLUSTER} in one
     * @param individual stations to draw as themselves, with their label
     */
    public record Result(List<MapMarkerCluster> clusters, List<MapCallsignRawSnapshot> individual) {

        public Result {
            clusters = List.copyOf(clusters);
            individual = List.copyOf(individual);
        }
    }

    /**
     * @param viewport decides both the cell size, through its zoom, and which screen
     *        cell a station falls into
     * @param clusteringEnabled the operator's "Group nearby stations" setting
     */
    public static Result cluster(List<MapCallsignRawSnapshot> markers,
                                 MapViewport viewport,
                                 boolean clusteringEnabled) {

        if (markers == null || markers.isEmpty()) {
            return new Result(List.of(), List.of());
        }

        List<MapCallsignRawSnapshot> positioned = new ArrayList<>();
        for (MapCallsignRawSnapshot marker : markers) {
            if (marker != null && marker.hasUsablePosition()) {
                positioned.add(marker);
            }
        }

        if (!clusteringEnabled || viewport.zoom() >= DISABLE_AT_ZOOM) {
            return new Result(List.of(), positioned);
        }

        double cellSizePx = cellSizePxForZoom(viewport.zoom());

        List<MapCallsignRawSnapshot> individual = new ArrayList<>();
        Map<Long, List<MapCallsignRawSnapshot>> buckets = new LinkedHashMap<>();

        for (MapCallsignRawSnapshot marker : positioned) {
            if (mustStayVisible(marker)) {
                individual.add(marker);
                continue;
            }

            long cellX = (long) Math.floor(viewport.screenXWrapped(marker.longitudeDeg()) / cellSizePx);
            long cellY = (long) Math.floor(viewport.screenY(marker.latitudeDeg()) / cellSizePx);

            buckets.computeIfAbsent(cellKey(cellX, cellY), key -> new ArrayList<>()).add(marker);
        }

        List<MapMarkerCluster> clusters = new ArrayList<>();
        for (List<MapCallsignRawSnapshot> bucket : buckets.values()) {
            if (bucket.size() >= MIN_STATIONS_PER_CLUSTER) {
                MapMarkerCluster cluster = new MapMarkerCluster(bucket.get(0));
                for (int i = 1; i < bucket.size(); i++) {
                    cluster.add(bucket.get(i));
                }
                clusters.add(cluster);
            } else {
                individual.addAll(bucket);
            }
        }

        return new Result(Collections.unmodifiableList(clusters), Collections.unmodifiableList(individual));
    }

    /**
     * The zoom at which a station the operator just selected is drawn as itself.
     *
     * Their current zoom, unless grouping would hide the station inside a bubble — and
     * then only far enough to break the bubble open. Selecting a station is not a
     * reason to throw away the zoom they chose.
     */
    public static double zoomToRevealStations(double currentZoom, boolean clusteringEnabled) {
        if (!clusteringEnabled) {
            return currentZoom;
        }
        return Math.max(currentZoom, DISABLE_AT_ZOOM);
    }

    /**
     * The cluster grid size in screen pixels for a zoom level.
     *
     * Smaller cells make grouping less eager: stations have to be closer together on
     * screen before they are grouped at all.
     */
    public static double cellSizePxForZoom(double zoom) {
        if (zoom >= 7.0) {
            return CELL_SIZE_HIGH_ZOOM_PX;
        }
        if (zoom >= 6.0) {
            return CELL_SIZE_MEDIUM_ZOOM_PX;
        }
        if (zoom >= 5.0) {
            return CELL_SIZE_LOW_ZOOM_PX;
        }
        return CELL_SIZE_VERY_LOW_ZOOM_PX;
    }

    /**
     * The station the operator is working, and the one pointing at their antenna. A
     * bubble over either of them hides the thing it was opened for.
     */
    private static boolean mustStayVisible(MapCallsignRawSnapshot marker) {
        return marker.selected() || marker.warningToMyDirection();
    }

    private static long cellKey(long cellX, long cellY) {
        return (cellX << 32) ^ (cellY & 0xFFFFFFFFL);
    }
}
