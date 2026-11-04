package kst4contest.view.map;

/** Pure viewport calculations for deliberate station-map navigation. */
public final class MapViewportNavigation {

    private static final double WEB_MERCATOR_MAX_LATITUDE = 85.05112878;

    private MapViewportNavigation() {
    }

    /**
     * Centres the path and zooms out only when both endpoints do not fit with padding.
     *
     * @return the fitted viewport, or the original viewport for invalid positions or size
     */
    public static MapViewport fitPath(MapViewport current,
                                      double homeLatitudeDeg,
                                      double homeLongitudeDeg,
                                      double targetLatitudeDeg,
                                      double targetLongitudeDeg,
                                      double paddingPx) {
        if (current == null
                || !isPosition(homeLatitudeDeg, homeLongitudeDeg)
                || !isPosition(targetLatitudeDeg, targetLongitudeDeg)
                || current.widthPx() <= 0.0
                || current.heightPx() <= 0.0) {
            return current;
        }

        double padding = Math.max(0.0, paddingPx);
        double availableWidth = current.widthPx() - 2.0 * padding;
        double availableHeight = current.heightPx() - 2.0 * padding;
        if (availableWidth <= 0.0 || availableHeight <= 0.0) {
            return current;
        }

        double homeWorldX = WebMercatorProjection.lonToX(homeLongitudeDeg, 0);
        double targetWorldX = WebMercatorProjection.lonToX(targetLongitudeDeg, 0);
        double worldSize = 256.0;
        double deltaX = targetWorldX - homeWorldX;
        deltaX -= Math.rint(deltaX / worldSize) * worldSize;

        double centerWorldX = homeWorldX + deltaX / 2.0;
        double homeWorldY = WebMercatorProjection.latToY(homeLatitudeDeg, 0);
        double targetWorldY = WebMercatorProjection.latToY(targetLatitudeDeg, 0);
        double centerWorldY = (homeWorldY + targetWorldY) / 2.0;

        double centerLongitude = WebMercatorProjection.xToLon(centerWorldX, 0);
        double centerLatitude = WebMercatorProjection.yToLat(centerWorldY, 0);
        MapViewport centred = new MapViewport(
                centerLongitude,
                centerLatitude,
                current.zoom(),
                current.widthPx(),
                current.heightPx()
        );

        double widthAtCurrentZoom = Math.abs(
                centred.screenXWrapped(targetLongitudeDeg)
                        - centred.screenXWrapped(homeLongitudeDeg)
        );
        double heightAtCurrentZoom = Math.abs(
                centred.screenY(targetLatitudeDeg) - centred.screenY(homeLatitudeDeg)
        );

        double widthRatio = widthAtCurrentZoom == 0.0
                ? Double.POSITIVE_INFINITY
                : availableWidth / widthAtCurrentZoom;
        double heightRatio = heightAtCurrentZoom == 0.0
                ? Double.POSITIVE_INFINITY
                : availableHeight / heightAtCurrentZoom;
        double fitRatio = Math.min(widthRatio, heightRatio);

        if (!Double.isFinite(fitRatio) || fitRatio >= 1.0) {
            return centred;
        }

        double fittedZoom = current.zoom() + Math.log(fitRatio) / Math.log(2.0);
        return new MapViewport(
                centerLongitude,
                centerLatitude,
                fittedZoom,
                current.widthPx(),
                current.heightPx()
        );
    }

    /**
     * Leaves the zoom unchanged and places most of the viewport in the chosen direction.
     */
    public static MapViewport centreOnBearing(MapViewport current,
                                              double homeLatitudeDeg,
                                              double homeLongitudeDeg,
                                              double bearingDeg,
                                              double forwardCentreFraction) {
        if (current == null
                || !isPosition(homeLatitudeDeg, homeLongitudeDeg)
                || !Double.isFinite(bearingDeg)
                || current.widthPx() <= 0.0
                || current.heightPx() <= 0.0) {
            return current;
        }

        double fraction = Math.max(0.0, Math.min(0.45, forwardCentreFraction));
        double shiftPx = Math.min(current.widthPx(), current.heightPx()) * fraction;
        double bearingRad = Math.toRadians(normaliseBearing(bearingDeg));

        MapViewport homeCentred = new MapViewport(
                homeLongitudeDeg,
                homeLatitudeDeg,
                current.zoom(),
                current.widthPx(),
                current.heightPx()
        );
        double centreX = current.widthPx() / 2.0 + Math.sin(bearingRad) * shiftPx;
        double centreY = current.heightPx() / 2.0 - Math.cos(bearingRad) * shiftPx;

        return new MapViewport(
                homeCentred.lonAt(centreX),
                homeCentred.latAt(centreY),
                current.zoom(),
                current.widthPx(),
                current.heightPx()
        );
    }

    public static MapViewport centreOnHome(MapViewport current,
                                           double homeLatitudeDeg,
                                           double homeLongitudeDeg) {
        if (current == null || !isPosition(homeLatitudeDeg, homeLongitudeDeg)) {
            return current;
        }
        return new MapViewport(
                homeLongitudeDeg,
                homeLatitudeDeg,
                current.zoom(),
                current.widthPx(),
                current.heightPx()
        );
    }

    private static boolean isPosition(double latitudeDeg, double longitudeDeg) {
        return Double.isFinite(latitudeDeg)
                && Double.isFinite(longitudeDeg)
                && latitudeDeg >= -WEB_MERCATOR_MAX_LATITUDE
                && latitudeDeg <= WEB_MERCATOR_MAX_LATITUDE;
    }

    private static double normaliseBearing(double bearingDeg) {
        double normalised = bearingDeg % 360.0;
        return normalised < 0.0 ? normalised + 360.0 : normalised;
    }
}
