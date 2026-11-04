package kst4contest.view.map;

/**
 * What the map currently shows, and where a position lands on the canvas.
 *
 * Everything the map draws except the tiles is drawn in screen pixels: a station dot is
 * 12 px at every zoom, a label is 12 px at every zoom, a grid line is 1.4 px at every
 * zoom. Leaflet behaved that way because its overlays live in the DOM above a scaled
 * tile layer. The first Compose port instead computed positions at the integer zoom and
 * then scaled the whole canvas by the fractional part, which made every drawn size a
 * function of that fraction and forced the drawing code to divide each size back out
 * again. Sizes were then right at one zoom and wrong at the next.
 *
 * This class folds the fraction into the projection instead. {@link #screenX(double)}
 * and {@link #screenY(double)} answer in canvas pixels, so the drawing code needs no
 * scaling and no compensation. Only the tiles are scaled, by {@link #tileScale()},
 * because tiles exist at integer levels only.
 *
 * Immutable: a viewport describes one frame. Panning and zooming produce a new one.
 */
public final class MapViewport {

    /**
     * The range the tile source serves. Leaflet was configured for exactly this
     * ({@code KST_MIN_ZOOM}/{@code KST_MAX_ZOOM}); outside it the requested tiles do
     * not exist.
     */
    public static final double MIN_ZOOM = 3.0;
    public static final double MAX_ZOOM = 18.0;

    private static final double TILE_SIZE_PX = 256.0;

    private final double centerLon;
    private final double centerLat;
    private final double zoom;
    private final double widthPx;
    private final double heightPx;

    private final int tileZoom;
    private final double tileScale;
    private final double centerWorldX;
    private final double centerWorldY;

    public MapViewport(double centerLon,
                       double centerLat,
                       double zoom,
                       double widthPx,
                       double heightPx) {

        this.centerLon = normaliseLongitude(centerLon);
        this.centerLat = centerLat;
        this.zoom = clamp(zoom, MIN_ZOOM, MAX_ZOOM);
        this.widthPx = Math.max(0.0, widthPx);
        this.heightPx = Math.max(0.0, heightPx);

        this.tileZoom = (int) Math.floor(this.zoom);
        this.tileScale = Math.pow(2.0, this.zoom - this.tileZoom);
        this.centerWorldX = WebMercatorProjection.lonToX(this.centerLon, this.tileZoom);
        this.centerWorldY = WebMercatorProjection.latToY(centerLat, this.tileZoom);
    }

    public double centerLon() {
        return centerLon;
    }

    public double centerLat() {
        return centerLat;
    }

    /** The zoom actually in force, after clamping to the served range. */
    public double zoom() {
        return zoom;
    }

    public double widthPx() {
        return widthPx;
    }

    public double heightPx() {
        return heightPx;
    }

    /** The integer level the tiles come from. */
    public int tileZoom() {
        return tileZoom;
    }

    /** What a tile of that level must be scaled by to match the fractional zoom. */
    public double tileScale() {
        return tileScale;
    }

    /** The on-screen size of one tile, in canvas pixels. */
    public double tileSizePx() {
        return TILE_SIZE_PX * tileScale;
    }

    public double screenX(double lon) {
        return widthPx / 2.0 + (WebMercatorProjection.lonToX(lon, tileZoom) - centerWorldX) * tileScale;
    }

    /**
     * Places a longitude in the closest wrapped copy of the world.
     *
     * <p>A target at 179 degrees east and a centre at 179 degrees west are two degrees
     * apart, not 358. Tiles already wrap at the anti-meridian; overlays and hit testing
     * need the same world copy to stay aligned with them.</p>
     */
    public double screenXWrapped(double lon) {
        double worldSize = TILE_SIZE_PX * Math.pow(2.0, tileZoom);
        double worldX = WebMercatorProjection.lonToX(lon, tileZoom);
        double delta = worldX - centerWorldX;
        delta -= Math.rint(delta / worldSize) * worldSize;
        return widthPx / 2.0 + delta * tileScale;
    }

    public double screenY(double lat) {
        return heightPx / 2.0 + (WebMercatorProjection.latToY(lat, tileZoom) - centerWorldY) * tileScale;
    }

    /** Where a world pixel of {@link #tileZoom()} lands on the canvas. */
    public double screenXForWorldX(double worldX) {
        return widthPx / 2.0 + (worldX - centerWorldX) * tileScale;
    }

    public double screenYForWorldY(double worldY) {
        return heightPx / 2.0 + (worldY - centerWorldY) * tileScale;
    }

    public double lonAt(double screenX) {
        return WebMercatorProjection.xToLon(worldXAt(screenX), tileZoom);
    }

    public double latAt(double screenY) {
        return WebMercatorProjection.yToLat(worldYAt(screenY), tileZoom);
    }

    public double worldXAt(double screenX) {
        return centerWorldX + (screenX - widthPx / 2.0) / tileScale;
    }

    public double worldYAt(double screenY) {
        return centerWorldY + (screenY - heightPx / 2.0) / tileScale;
    }

    public double westLon() {
        return lonAt(0.0);
    }

    public double eastLon() {
        return lonAt(widthPx);
    }

    /** North is the top of the canvas, which is y = 0. */
    public double northLat() {
        return latAt(0.0);
    }

    public double southLat() {
        return latAt(heightPx);
    }

    /**
     * The same view moved by a drag, in canvas pixels.
     *
     * A drag of n pixels moves what was under the pointer by exactly n pixels, which is
     * why the delta is divided by the tile scale rather than applied to the world
     * coordinates directly.
     */
    public MapViewport pannedBy(double deltaX, double deltaY) {
        return centredOnWorld(
                centerWorldX - deltaX / tileScale,
                centerWorldY - deltaY / tileScale,
                zoom
        );
    }

    /**
     * The same view at a different zoom, with the position under
     * {@code anchorX}/{@code anchorY} left where it is.
     *
     * Zooming about the pointer rather than the centre is what makes a wheel zoom feel
     * like a map. The anchor's position is read before the zoom changes and the new
     * centre is derived from it afterwards, because the world coordinates of a position
     * change with the tile level.
     */
    public MapViewport zoomedTo(double newZoom, double anchorX, double anchorY) {
        double anchorLon = lonAt(anchorX);
        double anchorLat = latAt(anchorY);

        double clampedZoom = clamp(newZoom, MIN_ZOOM, MAX_ZOOM);
        int newTileZoom = (int) Math.floor(clampedZoom);
        double newTileScale = Math.pow(2.0, clampedZoom - newTileZoom);

        double anchorWorldX = WebMercatorProjection.lonToX(anchorLon, newTileZoom);
        double anchorWorldY = WebMercatorProjection.latToY(anchorLat, newTileZoom);

        return new MapViewport(
                WebMercatorProjection.xToLon(anchorWorldX - (anchorX - widthPx / 2.0) / newTileScale, newTileZoom),
                WebMercatorProjection.yToLat(anchorWorldY - (anchorY - heightPx / 2.0) / newTileScale, newTileZoom),
                clampedZoom,
                widthPx,
                heightPx
        );
    }

    /** The same view on a canvas of a different size. */
    public MapViewport resizedTo(double newWidthPx, double newHeightPx) {
        return new MapViewport(centerLon, centerLat, zoom, newWidthPx, newHeightPx);
    }

    private MapViewport centredOnWorld(double worldX, double worldY, double atZoom) {
        return new MapViewport(
                WebMercatorProjection.xToLon(worldX, tileZoom),
                WebMercatorProjection.yToLat(worldY, tileZoom),
                atZoom,
                widthPx,
                heightPx
        );
    }

    /**
     * Brings a longitude back into [-180, 180).
     *
     * Panning east is unbounded arithmetic. The tile indices wrap, so the basemap keeps
     * drawing, but nothing else does: a centre at longitude 262 puts a station at -170
     * thousands of pixels off the canvas and hands the grid a span it clamps to
     * nothing. The stations and the grid then disappear while the map underneath looks
     * perfectly fine.
     */
    private static double normaliseLongitude(double lon) {
        if (!Double.isFinite(lon)) {
            return 0.0;
        }
        double wrapped = (lon + 180.0) % 360.0;
        if (wrapped < 0.0) {
            wrapped += 360.0;
        }
        return wrapped - 180.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
