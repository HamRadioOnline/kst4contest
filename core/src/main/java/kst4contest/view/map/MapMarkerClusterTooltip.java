package kst4contest.view.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What hovering a cluster bubble says.
 *
 * The count first, because that is the question the bubble raises. Then the callsigns,
 * alphabetically and capped, because a cell at low zoom can hold a hundred of them and
 * a tooltip taller than the window hides the map it was meant to explain. Then the hint
 * that clicking zooms in, without which a bubble looks like a dead end.
 *
 * Kept as the Leaflet tooltip had it (buildClusterTooltipHtml); only the line breaks
 * differ, because this one is drawn on a canvas rather than written into HTML.
 */
public final class MapMarkerClusterTooltip {

    /** Twenty, as the Leaflet tooltip did. Beyond that a list stops being readable. */
    public static final int MAX_PREVIEWED_STATIONS = 20;

    private MapMarkerClusterTooltip() {
    }

    public static String build(MapMarkerCluster cluster) {
        List<String> callsigns = new ArrayList<>();
        for (MapCallsignRawSnapshot marker : cluster.getMarkers()) {
            String label = marker.tooltipText().replace('\n', ' ');
            if (label != null && !label.isBlank()) {
                callsigns.add(label.trim());
            }
        }
        Collections.sort(callsigns);

        StringBuilder text = new StringBuilder();
        text.append(cluster.size()).append(" stations");

        int previewed = Math.min(MAX_PREVIEWED_STATIONS, callsigns.size());
        for (int i = 0; i < previewed; i++) {
            text.append('\n').append(callsigns.get(i));
        }

        int remaining = callsigns.size() - previewed;
        if (remaining > 0) {
            text.append('\n').append('+').append(remaining).append(" more");
        }

        return text.append('\n').append("Click to zoom in").toString();
    }
}
