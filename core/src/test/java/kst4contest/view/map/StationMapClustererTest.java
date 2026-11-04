package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grouping stations that sit on top of each other on screen.
 *
 * The rules are not ours to invent: they are the ones the Leaflet map ran for the whole
 * of release 1.4x, and an operator who has learned them must not have to relearn them.
 * They live here rather than in the drawing code because they are decisions about what
 * the operator sees, and because the drawing code has no test harness.
 *
 * Every constant below is pinned against the JavaScript it came from —
 * MapHtmlResources.java, KST_CLUSTER_* and renderClusteredStations().
 */
class StationMapClustererTest {

    private MapViewport viewportAtZoom(double zoom) {
        return new MapViewport(10.0, 51.0, zoom, 1000.0, 800.0);
    }

    private MapCallsignRawSnapshot station(String call, double lat, double lon) {
        return station(call, lat, lon, false, false);
    }

    private MapCallsignRawSnapshot station(String call,
                                           double lat,
                                           double lon,
                                           boolean selected,
                                           boolean warning) {
        return new MapCallsignRawSnapshot(
                call, call, "JO50JP", lat, lon, "144", Map.of(),
                false, warning, false, selected, 100.0, 90.0, 0, 0L
        );
    }

    /** Stations close enough to share one cluster cell at the given zoom. */
    private List<MapCallsignRawSnapshot> huddle(int count, double lat, double lon) {
        List<MapCallsignRawSnapshot> stations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stations.add(station("DL" + i + "AA", lat + i * 0.0001, lon + i * 0.0001));
        }
        return stations;
    }

    @Test
    void two_stations_on_the_same_spot_stay_separate() {
        StationMapClusterer.Result result = StationMapClusterer.cluster(
                huddle(2, 51.0, 10.0), viewportAtZoom(6.0), true);

        assertTrue(result.clusters().isEmpty(), "two nearby stations are still readable");
        assertEquals(2, result.individual().size());
    }

    @Test
    void three_stations_on_the_same_spot_become_one_cluster() {
        StationMapClusterer.Result result = StationMapClusterer.cluster(
                huddle(3, 51.0, 10.0), viewportAtZoom(6.0), true);

        assertEquals(1, result.clusters().size());
        assertEquals(3, result.clusters().get(0).size());
        assertTrue(result.individual().isEmpty());
    }

    @Test
    void clustering_stops_once_the_stations_are_far_enough_apart_on_screen() {
        StationMapClusterer.Result result = StationMapClusterer.cluster(
                huddle(5, 51.0, 10.0), viewportAtZoom(StationMapClusterer.DISABLE_AT_ZOOM), true);

        assertTrue(result.clusters().isEmpty(), "at zoom 8 and above the map shows every station");
        assertEquals(5, result.individual().size());
    }

    @Test
    void clustering_can_be_switched_off_entirely() {
        StationMapClusterer.Result result = StationMapClusterer.cluster(
                huddle(5, 51.0, 10.0), viewportAtZoom(4.0), false);

        assertTrue(result.clusters().isEmpty());
        assertEquals(5, result.individual().size());
    }

    /**
     * The one the operator is working and the one pointing at their antenna are the two
     * they must not lose in a bubble.
     */
    @Test
    void the_selected_station_is_never_hidden_in_a_cluster() {
        List<MapCallsignRawSnapshot> stations = new ArrayList<>(huddle(4, 51.0, 10.0));
        stations.add(station("F5JMI", 51.0, 10.0, true, false));

        StationMapClusterer.Result result = StationMapClusterer.cluster(
                stations, viewportAtZoom(5.0), true);

        assertTrue(result.individual().stream().anyMatch(s -> s.callSignRaw().equals("F5JMI")));
        assertFalse(
                result.clusters().stream().anyMatch(c -> c.getMarkers().stream()
                        .anyMatch(s -> s.callSignRaw().equals("F5JMI"))),
                "a selected station inside a bubble cannot be seen"
        );
    }

    @Test
    void a_station_warning_towards_my_direction_is_never_hidden_in_a_cluster() {
        List<MapCallsignRawSnapshot> stations = new ArrayList<>(huddle(4, 51.0, 10.0));
        stations.add(station("OE5SOS", 51.0, 10.0, false, true));

        StationMapClusterer.Result result = StationMapClusterer.cluster(
                stations, viewportAtZoom(5.0), true);

        assertTrue(result.individual().stream().anyMatch(s -> s.callSignRaw().equals("OE5SOS")));
    }

    @Test
    void a_station_without_a_usable_position_is_dropped_rather_than_drawn_at_zero() {
        List<MapCallsignRawSnapshot> stations = List.of(
                new MapCallsignRawSnapshot("DL1AAA", "DL1AAA", "", 0.0, 0.0, "", Map.of(),
                        false, false, false, false, 0.0, 0.0, 0, 0L)
        );

        StationMapClusterer.Result result = StationMapClusterer.cluster(
                stations, viewportAtZoom(6.0), true);

        assertTrue(result.clusters().isEmpty());
        assertTrue(result.individual().isEmpty());
    }

    /** The four sizes the Leaflet map used, by the zoom thresholds it used. */
    @Test
    void the_cell_grows_as_the_map_zooms_out() {
        assertEquals(55.0, StationMapClusterer.cellSizePxForZoom(7.0));
        assertEquals(55.0, StationMapClusterer.cellSizePxForZoom(7.9));
        assertEquals(70.0, StationMapClusterer.cellSizePxForZoom(6.0));
        assertEquals(95.0, StationMapClusterer.cellSizePxForZoom(5.0));
        assertEquals(125.0, StationMapClusterer.cellSizePxForZoom(4.0));
        assertEquals(125.0, StationMapClusterer.cellSizePxForZoom(3.0));
    }

    @Test
    void stations_in_different_screen_cells_are_not_grouped_together() {
        double cell = StationMapClusterer.cellSizePxForZoom(6.0);
        MapViewport viewport = viewportAtZoom(6.0);

        /* Two huddles, placed more than one cell apart on screen. */
        List<MapCallsignRawSnapshot> stations = new ArrayList<>(huddle(3, 51.0, 10.0));
        double farLon = viewport.lonAt(viewport.screenX(10.0) + cell * 3.0);
        for (int i = 0; i < 3; i++) {
            stations.add(station("PA" + i + "BB", 51.0 + i * 0.0001, farLon + i * 0.0001));
        }

        StationMapClusterer.Result result = StationMapClusterer.cluster(stations, viewport, true);

        assertEquals(2, result.clusters().size());
        assertTrue(result.clusters().stream().allMatch(c -> c.size() == 3));
    }

    @Test
    void every_station_handed_in_comes_back_exactly_once() {
        List<MapCallsignRawSnapshot> stations = new ArrayList<>(huddle(7, 51.0, 10.0));
        stations.add(station("F5JMI", 51.0, 10.0, true, false));

        StationMapClusterer.Result result = StationMapClusterer.cluster(
                stations, viewportAtZoom(5.0), true);

        int clustered = result.clusters().stream().mapToInt(MapMarkerCluster::size).sum();
        assertEquals(stations.size(), clustered + result.individual().size());
    }

    @Test
    void nearby_stations_across_the_anti_meridian_share_a_cluster() {
        MapViewport viewport = new MapViewport(180.0, 0.0, 5.0, 1000.0, 800.0);
        List<MapCallsignRawSnapshot> stations = List.of(
                station("ZL1AAA", 0.0, 179.9),
                station("ZL1BBB", 0.0, -179.9),
                station("ZL1CCC", 0.01, 179.8)
        );

        StationMapClusterer.Result result = StationMapClusterer.cluster(stations, viewport, true);

        assertEquals(1, result.clusters().size());
        assertTrue(Math.abs(Math.abs(result.clusters().get(0).getCenterLon()) - 180.0) < 0.2);
    }

    /**
     * Selecting a station moves the map to it. It must not throw the operator's zoom
     * away doing so.
     *
     * The Leaflet map panned and kept the zoom, and touched the zoom only when the
     * station was hidden inside a bubble — then just far enough to break the bubble
     * open (focusCallsignRaw, MapHtmlResources.java). The first Compose port instead
     * recomputed a zoom from a bounding box on every selection and clamped it into
     * 4..12, so an operator working at zoom 15 was yanked out to about 7 by clicking
     * the next station. AGENTS.md names zoom first among the things not to change as an
     * incidental side effect.
     */
    @Test
    void selecting_a_station_that_is_already_visible_keeps_the_zoom() {
        assertEquals(15.0, StationMapClusterer.zoomToRevealStations(15.0, true));
        assertEquals(9.0, StationMapClusterer.zoomToRevealStations(9.0, true));
    }

    @Test
    void selecting_a_station_hidden_in_a_bubble_zooms_just_far_enough_to_open_it() {
        assertEquals(StationMapClusterer.DISABLE_AT_ZOOM, StationMapClusterer.zoomToRevealStations(5.0, true));
        assertEquals(StationMapClusterer.DISABLE_AT_ZOOM, StationMapClusterer.zoomToRevealStations(7.9, true));
    }

    @Test
    void with_grouping_switched_off_every_station_is_already_visible() {
        assertEquals(4.0, StationMapClusterer.zoomToRevealStations(4.0, false));
        assertEquals(3.0, StationMapClusterer.zoomToRevealStations(3.0, false));
    }
}
