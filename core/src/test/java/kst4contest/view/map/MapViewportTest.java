package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The viewport that turns a position into a place on the canvas.
 *
 * The Compose map used to compute positions at the integer zoom level and then scale the
 * whole canvas by the fraction, which made every drawn size — dot radii, stroke widths,
 * font sizes — depend on the fraction. Leaflet drew overlays in screen pixels and kept
 * them constant. This class is how that is recovered: the fraction is folded into the
 * projection, so the drawing code works in screen pixels and needs no scaling at all.
 */
class MapViewportTest {

    private static final double TOLERANCE_PX = 1e-6;
    private static final double TOLERANCE_DEG = 1e-9;

    private MapViewport viewport(double zoom) {
        return new MapViewport(10.0, 51.0, zoom, 800.0, 600.0);
    }

    @Test
    void the_centre_of_the_viewport_lands_in_the_middle_of_the_canvas() {
        MapViewport viewport = viewport(6.0);

        assertEquals(400.0, viewport.screenX(10.0), TOLERANCE_PX);
        assertEquals(300.0, viewport.screenY(51.0), TOLERANCE_PX);
    }

    @Test
    void the_fractional_part_of_the_zoom_is_folded_into_the_projection() {
        MapViewport viewport = viewport(6.5);

        assertEquals(400.0, viewport.screenX(10.0), TOLERANCE_PX);
        assertEquals(300.0, viewport.screenY(51.0), TOLERANCE_PX);
    }

    /**
     * Half a zoom level is a factor of sqrt(2) in scale. This is the property the old
     * drawing code faked by scaling the canvas, and the reason sizes drifted with zoom.
     */
    @Test
    void half_a_zoom_level_doubles_the_distance_on_screen_over_a_whole_level() {
        double atSix = viewport(6.0).screenX(15.0) - viewport(6.0).screenX(10.0);
        double atSixAndAHalf = viewport(6.5).screenX(15.0) - viewport(6.5).screenX(10.0);
        double atSeven = viewport(7.0).screenX(15.0) - viewport(7.0).screenX(10.0);

        assertEquals(2.0, atSeven / atSix, 1e-9);
        assertEquals(Math.sqrt(2.0), atSixAndAHalf / atSix, 1e-9);
    }

    @Test
    void a_screen_position_maps_back_to_the_position_it_came_from() {
        MapViewport viewport = viewport(6.5);

        assertEquals(12.5, viewport.lonAt(viewport.screenX(12.5)), TOLERANCE_DEG);
        assertEquals(48.25, viewport.latAt(viewport.screenY(48.25)), TOLERANCE_DEG);
    }

    @Test
    void the_edges_of_the_canvas_are_the_edges_of_the_viewport() {
        MapViewport viewport = viewport(6.5);

        assertEquals(viewport.lonAt(0.0), viewport.westLon(), TOLERANCE_DEG);
        assertEquals(viewport.lonAt(800.0), viewport.eastLon(), TOLERANCE_DEG);
        assertEquals(viewport.latAt(0.0), viewport.northLat(), TOLERANCE_DEG);
        assertEquals(viewport.latAt(600.0), viewport.southLat(), TOLERANCE_DEG);

        assertTrue(viewport.northLat() > viewport.southLat());
        assertTrue(viewport.eastLon() > viewport.westLon());
    }

    /** Tiles only exist at integer levels; the fraction becomes the scale they are drawn at. */
    @Test
    void tiles_come_from_the_level_below_the_fractional_zoom() {
        assertEquals(6, viewport(6.0).tileZoom());
        assertEquals(6, viewport(6.99).tileZoom());
        assertEquals(7, viewport(7.0).tileZoom());

        assertEquals(1.0, viewport(6.0).tileScale(), TOLERANCE_PX);
        assertEquals(Math.sqrt(2.0), viewport(6.5).tileScale(), 1e-9);
    }

    /**
     * Leaflet was configured for 3 to 18 and the tile source carries nothing outside
     * that; a zoom beyond it would request tiles that do not exist.
     */
    @Test
    void the_zoom_stays_within_the_range_the_tile_source_serves() {
        assertEquals(MapViewport.MIN_ZOOM, new MapViewport(10.0, 51.0, 1.0, 800.0, 600.0).zoom(), TOLERANCE_PX);
        assertEquals(MapViewport.MAX_ZOOM, new MapViewport(10.0, 51.0, 25.0, 800.0, 600.0).zoom(), TOLERANCE_PX);
        assertEquals(3.0, MapViewport.MIN_ZOOM, TOLERANCE_PX);
        assertEquals(18.0, MapViewport.MAX_ZOOM, TOLERANCE_PX);
    }

    /**
     * A canvas can be measured at zero before the window is laid out. The viewport must
     * answer rather than divide by it.
     */
    @Test
    void a_canvas_without_a_size_still_answers() {
        MapViewport viewport = new MapViewport(10.0, 51.0, 6.0, 0.0, 0.0);

        assertEquals(0.0, viewport.screenX(10.0), TOLERANCE_PX);
        assertTrue(Double.isFinite(viewport.westLon()));
        assertTrue(Double.isFinite(viewport.northLat()));
    }

    @Test
    void a_drag_moves_the_view_opposite_to_the_pointer() {
        MapViewport dragged = viewport(6.5).pannedBy(100.0, 50.0);

        /* Dragging the map right brings positions further west into view. */
        assertTrue(dragged.centerLon() < 10.0);
        assertTrue(dragged.centerLat() > 51.0);
    }

    /** A drag of n pixels moves what was under the pointer by exactly n pixels. */
    @Test
    void a_drag_moves_a_position_by_the_distance_dragged() {
        MapViewport before = viewport(6.5);
        double xBefore = before.screenX(12.0);

        MapViewport after = before.pannedBy(100.0, -40.0);

        assertEquals(xBefore + 100.0, after.screenX(12.0), 1e-6);
        assertEquals(before.screenY(49.0) - 40.0, after.screenY(49.0), 1e-6);
    }

    @Test
    void a_drag_and_its_reverse_return_the_view_it_started_from() {
        MapViewport roundTrip = viewport(6.5).pannedBy(130.0, -70.0).pannedBy(-130.0, 70.0);

        assertEquals(10.0, roundTrip.centerLon(), 1e-9);
        assertEquals(51.0, roundTrip.centerLat(), 1e-9);
    }

    /**
     * The point under the pointer stays under the pointer. This is what makes a wheel
     * zoom feel like a map rather than a jump to the centre.
     */
    @Test
    void zooming_keeps_the_position_under_the_pointer_in_place() {
        MapViewport before = viewport(6.0);
        double anchorX = 650.0;
        double anchorY = 120.0;
        double anchorLon = before.lonAt(anchorX);
        double anchorLat = before.latAt(anchorY);

        MapViewport after = before.zoomedTo(8.25, anchorX, anchorY);

        assertEquals(8.25, after.zoom(), 1e-9);
        assertEquals(anchorX, after.screenX(anchorLon), 1e-6);
        assertEquals(anchorY, after.screenY(anchorLat), 1e-6);
    }

    @Test
    void zooming_about_the_centre_leaves_the_centre_alone() {
        MapViewport after = viewport(6.0).zoomedTo(7.5, 400.0, 300.0);

        assertEquals(10.0, after.centerLon(), 1e-9);
        assertEquals(51.0, after.centerLat(), 1e-9);
    }

    @Test
    void a_resize_keeps_the_centre_and_the_zoom() {
        MapViewport resized = viewport(6.5).resizedTo(1200.0, 400.0);

        assertEquals(10.0, resized.centerLon(), 1e-9);
        assertEquals(51.0, resized.centerLat(), 1e-9);
        assertEquals(6.5, resized.zoom(), 1e-9);
        assertEquals(600.0, resized.screenX(10.0), 1e-6);
        assertEquals(200.0, resized.screenY(51.0), 1e-6);
    }

    /**
     * Panning east forever must not walk the centre off the world.
     *
     * The tile indices wrap, so the basemap keeps drawing; the overlays do not wrap, so
     * a centre at longitude 262 puts a station at -170 nine thousand pixels off the
     * canvas and leaves the grid with a degenerate column. Stations and grid then vanish
     * while the map underneath looks fine.
     */
    @Test
    void panning_east_past_the_antimeridian_keeps_the_centre_on_the_world() {
        MapViewport viewport = new MapViewport(175.0, 0.0, 5.0, 800.0, 600.0);

        for (int drag = 0; drag < 10; drag++) {
            viewport = viewport.pannedBy(-200.0, 0.0);
        }

        assertTrue(viewport.centerLon() >= -180.0 && viewport.centerLon() < 180.0,
                "centre ran off the world at longitude " + viewport.centerLon());
    }

    @Test
    void a_longitude_handed_in_past_the_antimeridian_is_brought_back_round() {
        assertEquals(-179.0, new MapViewport(181.0, 0.0, 6.0, 800.0, 600.0).centerLon(), TOLERANCE_DEG);
        assertEquals(179.0, new MapViewport(-181.0, 0.0, 6.0, 800.0, 600.0).centerLon(), TOLERANCE_DEG);
        assertEquals(0.0, new MapViewport(360.0, 0.0, 6.0, 800.0, 600.0).centerLon(), TOLERANCE_DEG);
    }

    @Test
    void wrapped_screen_coordinates_use_the_nearest_world_copy() {
        MapViewport viewport = new MapViewport(179.0, 0.0, 6.0, 800.0, 600.0);

        assertTrue(Math.abs(viewport.screenXWrapped(-179.0) - viewport.screenXWrapped(179.0)) < 100.0);
        assertTrue(Math.abs(viewport.screenX(-179.0) - viewport.screenX(179.0)) > 10_000.0);
    }
}
