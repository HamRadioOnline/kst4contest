package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What hovering a cluster bubble says.
 *
 * The Leaflet tooltip (buildClusterTooltipHtml, MapHtmlResources.java) led with the
 * count, listed at most twenty callsigns in alphabetical order, said how many more there
 * were, and ended by telling the operator that clicking zooms in. The Compose port
 * listed every member and nothing else: at zoom 3 a European roster puts a hundred
 * stations into one cell, which is a white box taller than the window, and the
 * click-to-zoom behaviour became undiscoverable.
 */
class MapMarkerClusterTooltipTest {

    private MapMarkerCluster clusterOf(int count) {
        List<MapCallsignRawSnapshot> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            members.add(new MapCallsignRawSnapshot(
                    String.format("DL%03dAA", i), String.format("DL%03dAA", i), "JO50JP",
                    51.0, 10.0, "", Map.of(), false, false, false, false, 1.0, 1.0, 0, 0L
            ));
        }
        MapMarkerCluster cluster = new MapMarkerCluster(members.get(0));
        for (int i = 1; i < members.size(); i++) {
            cluster.add(members.get(i));
        }
        return cluster;
    }

    @Test
    void it_leads_with_the_count() {
        assertTrue(MapMarkerClusterTooltip.build(clusterOf(5)).startsWith("5 stations"));
    }

    @Test
    void it_ends_by_saying_that_clicking_zooms_in() {
        assertTrue(MapMarkerClusterTooltip.build(clusterOf(5)).endsWith("Click to zoom in"));
    }

    @Test
    void it_lists_the_members_in_alphabetical_order() {
        List<String> lines = List.of(MapMarkerClusterTooltip.build(clusterOf(3)).split("\\n"));

        assertEquals("3 stations", lines.get(0));
        assertTrue(lines.get(1).startsWith("DL000AA"));
        assertTrue(lines.get(2).startsWith("DL001AA"));
        assertTrue(lines.get(3).startsWith("DL002AA"));
        assertTrue(lines.get(1).contains("JO50JP"));
    }

    /** A hundred callsigns is a box taller than the window. */
    @Test
    void a_crowded_cluster_is_summarised_rather_than_listed_in_full() {
        String tooltip = MapMarkerClusterTooltip.build(clusterOf(100));
        List<String> lines = List.of(tooltip.split("\\n"));

        assertEquals(
                MapMarkerClusterTooltip.MAX_PREVIEWED_STATIONS + 3,
                lines.size(),
                "count line, 20 callsigns, a remainder line and the hint"
        );
        assertTrue(tooltip.contains("+80 more"), tooltip);
    }

    @Test
    void a_cluster_of_exactly_twenty_needs_no_remainder_line() {
        String tooltip = MapMarkerClusterTooltip.build(clusterOf(MapMarkerClusterTooltip.MAX_PREVIEWED_STATIONS));

        assertTrue(!tooltip.contains("more"), tooltip);
        assertEquals(MapMarkerClusterTooltip.MAX_PREVIEWED_STATIONS + 2, List.of(tooltip.split("\\n")).size());
    }
}
