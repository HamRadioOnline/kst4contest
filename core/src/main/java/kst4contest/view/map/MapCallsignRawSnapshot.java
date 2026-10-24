package kst4contest.view.map;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Immutable map render model.
 *
 * One snapshot represents exactly one visible marker on the map,
 * aggregated by callSignRaw.
 */
public record MapCallsignRawSnapshot(
        String callSignRaw,
        String displayCallSign,
        String locator6,
        double latitudeDeg,
        double longitudeDeg,
        String bandSummary,
        Map<String, String> lastKnownFrequenciesByBand,
        boolean offersSelectedBand,
        boolean warningToMyDirection,
        boolean worked,
        boolean selected,
        double qrbKm,
        double qtfDeg,
        int reachableAirplanes,
        long lastActivityEpochMs
) {

    public MapCallsignRawSnapshot {
        callSignRaw = normalizeUpper(callSignRaw);
        displayCallSign = (displayCallSign == null || displayCallSign.isBlank())
                ? callSignRaw
                : displayCallSign.trim();

        locator6 = locator6 == null ? "" : locator6.trim().toUpperCase(Locale.ROOT);
        bandSummary = bandSummary == null ? "" : bandSummary.trim();

        LinkedHashMap<String, String> orderedFrequencies = new LinkedHashMap<>();
        if (lastKnownFrequenciesByBand != null) {
            orderedFrequencies.putAll(lastKnownFrequenciesByBand);
        }
        lastKnownFrequenciesByBand = Collections.unmodifiableMap(orderedFrequencies);
    }

    private static String normalizeUpper(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public boolean hasUsablePosition() {
        return !locator6.isBlank()
                && Double.isFinite(latitudeDeg)
                && Double.isFinite(longitudeDeg);
    }

    public String markerLabel() {
        return displayCallSign;
    }

    /**
     * Returns the details removed from the visible marker label for its hover text.
     *
     * @return multiline callsign, locator, band and frequency summary
     */
    public String tooltipText() {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(displayCallSign);
        if (!locator6.isBlank()) {
            joiner.add("Locator: " + locator6);
        }
        if (!bandSummary.isBlank()) {
            joiner.add("Bands: " + bandSummary);
        }
        if (offersSelectedBand) {
            joiner.add("B+");
        }
        if (!lastKnownFrequenciesByBand.isEmpty()) {
            joiner.add(detailFrequencyText());
        }
        return joiner.toString();
    }

    public String detailFrequencyText() {
        if (lastKnownFrequenciesByBand.isEmpty()) {
            return "-";
        }

        StringJoiner joiner = new StringJoiner("\n");
        for (Map.Entry<String, String> entry : lastKnownFrequenciesByBand.entrySet()) {
            joiner.add(entry.getKey() + ": " + entry.getValue());
        }
        return joiner.toString();
    }
}
