package kst4contest.view.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MapMarkerCluster {
    private final List<MapCallsignRawSnapshot> markers = new ArrayList<>();
    private double sumLon = 0.0;
    private double sumLat = 0.0;
    private double sumLonSin = 0.0;
    private double sumLonCos = 0.0;

    public MapMarkerCluster(MapCallsignRawSnapshot first) {
        add(first);
    }

    public void add(MapCallsignRawSnapshot marker) {
        markers.add(marker);
        sumLon += marker.longitudeDeg();
        sumLat += marker.latitudeDeg();
        double longitudeRad = Math.toRadians(marker.longitudeDeg());
        sumLonSin += Math.sin(longitudeRad);
        sumLonCos += Math.cos(longitudeRad);
    }

    public double getCenterLon() {
        if (Math.abs(sumLonSin) < 1e-12 && Math.abs(sumLonCos) < 1e-12) {
            return sumLon / markers.size();
        }
        return Math.toDegrees(Math.atan2(sumLonSin, sumLonCos));
    }

    public double getCenterLat() {
        return sumLat / markers.size();
    }

    public double getCenterX(int zoom) {
        return WebMercatorProjection.lonToX(getCenterLon(), zoom);
    }

    public double getCenterY(int zoom) {
        return WebMercatorProjection.latToY(getCenterLat(), zoom);
    }

    public List<MapCallsignRawSnapshot> getMarkers() {
        return Collections.unmodifiableList(markers);
    }

    public int size() {
        return markers.size();
    }

    public boolean isSingle() {
        return markers.size() == 1;
    }

    public MapCallsignRawSnapshot getSingle() {
        return markers.get(0);
    }
}
