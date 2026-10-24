package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCallsignRawSnapshotLabelTest {

    @Test
    void visibleMarkerLabelContainsOnlyTheCallsign() {
        MapCallsignRawSnapshot snapshot = snapshot();

        assertEquals("DM5M", snapshot.markerLabel());
    }

    @Test
    void tooltipRetainsLocatorBandsUpgradeAndFrequencyDetails() {
        String tooltip = snapshot().tooltipText();

        assertTrue(tooltip.contains("DM5M"));
        assertTrue(tooltip.contains("JO51AA"));
        assertTrue(tooltip.contains("144, 432"));
        assertTrue(tooltip.contains("B+"));
        assertTrue(tooltip.contains("144: 144.310"));
    }

    private MapCallsignRawSnapshot snapshot() {
        return new MapCallsignRawSnapshot(
                "DM5M", "DM5M", "JO51AA", 51.0, 10.0,
                "144, 432", Map.of("144", "144.310"), true,
                false, false, false, 100.0, 180.0, 0, 0L
        );
    }
}
