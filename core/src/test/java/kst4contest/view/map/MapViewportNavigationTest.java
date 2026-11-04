package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapViewportNavigationTest {

    private static final double PADDING_PX = 48.0;

    @Test
    void a_path_that_fits_keeps_the_existing_zoom() {
        MapViewport before = new MapViewport(10.0, 51.0, 8.5, 1000.0, 700.0);

        MapViewport fitted = MapViewportNavigation.fitPath(
                before, 51.0, 10.0, 51.2, 10.4, PADDING_PX);

        assertEquals(8.5, fitted.zoom(), 1e-9);
        assertInsidePadding(fitted, 51.0, 10.0);
        assertInsidePadding(fitted, 51.2, 10.4);
    }

    @Test
    void a_long_path_zooms_out_until_both_endpoints_fit() {
        MapViewport before = new MapViewport(10.0, 51.0, 12.0, 900.0, 600.0);

        MapViewport fitted = MapViewportNavigation.fitPath(
                before, 51.0, 10.0, 44.0, -3.0, PADDING_PX);

        assertTrue(fitted.zoom() < before.zoom());
        assertInsidePadding(fitted, 51.0, 10.0);
        assertInsidePadding(fitted, 44.0, -3.0);
    }

    @Test
    void fitting_across_the_anti_meridian_uses_the_short_path() {
        MapViewport before = new MapViewport(180.0, 0.0, 7.0, 900.0, 600.0);

        MapViewport fitted = MapViewportNavigation.fitPath(
                before, 0.0, 179.0, 0.0, -179.0, PADDING_PX);

        assertEquals(7.0, fitted.zoom(), 1e-9, "two degrees should not look like a world-spanning path");
        assertInsidePadding(fitted, 0.0, 179.0);
        assertInsidePadding(fitted, 0.0, -179.0);
    }

    @Test
    void bearing_navigation_preserves_zoom_and_moves_towards_the_sector() {
        MapViewport before = new MapViewport(10.0, 51.0, 8.25, 1000.0, 600.0);

        MapViewport northEast = MapViewportNavigation.centreOnBearing(
                before, 51.0, 10.0, 45.0, 0.25);

        assertEquals(8.25, northEast.zoom(), 1e-9);
        assertTrue(northEast.centerLon() > 10.0);
        assertTrue(northEast.centerLat() > 51.0);
        assertTrue(northEast.screenXWrapped(10.0) < northEast.widthPx() / 2.0);
        assertTrue(northEast.screenY(51.0) > northEast.heightPx() / 2.0);
    }

    private static void assertInsidePadding(MapViewport viewport, double latitude, double longitude) {
        double x = viewport.screenXWrapped(longitude);
        double y = viewport.screenY(latitude);
        assertTrue(x >= PADDING_PX - 1e-6 && x <= viewport.widthPx() - PADDING_PX + 1e-6,
                "longitude outside padded viewport at x=" + x);
        assertTrue(y >= PADDING_PX - 1e-6 && y <= viewport.heightPx() - PADDING_PX + 1e-6,
                "latitude outside padded viewport at y=" + y);
    }
}
