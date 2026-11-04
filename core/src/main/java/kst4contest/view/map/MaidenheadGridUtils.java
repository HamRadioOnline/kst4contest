package kst4contest.view.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Utility methods for generating visible Maidenhead grid rectangles.
 *
 * Supported levels:
 * - 2 characters (fields)
 * - 4 characters (squares)
 * - 6 characters (subsquares)
 */
public final class MaidenheadGridUtils {

    private static final double EPSILON = 1e-9;

    private MaidenheadGridUtils() {
    }

    public enum GridPrecision {
        FIELD_2(2, 20.0, 10.0),
        SQUARE_4(4, 2.0, 1.0),
        SUBSQUARE_6(6, 5.0 / 60.0, 2.5 / 60.0);

        private final int locatorLength;
        private final double cellWidthDeg;
        private final double cellHeightDeg;

        GridPrecision(int locatorLength, double cellWidthDeg, double cellHeightDeg) {
            this.locatorLength = locatorLength;
            this.cellWidthDeg = cellWidthDeg;
            this.cellHeightDeg = cellHeightDeg;
        }

        public int locatorLength() {
            return locatorLength;
        }

        public double cellWidthDeg() {
            return cellWidthDeg;
        }

        public double cellHeightDeg() {
            return cellHeightDeg;
        }
    }

    public record GridCell(
            String locatorLabel,
            double southLat,
            double westLon,
            double northLat,
            double eastLon,
            int rowIndex,
            int columnIndex
    ) {
    }

    public static GridPrecision precisionForZoom(int leafletZoom) {
        if (leafletZoom <= 5) {
            return GridPrecision.FIELD_2;
        }
        if (leafletZoom <= 7) {
            return GridPrecision.SQUARE_4;
        }
        return GridPrecision.SUBSQUARE_6;
    }

    public static List<GridCell> buildVisibleCells(double southLat,
                                                   double westLon,
                                                   double northLat,
                                                   double eastLon,
                                                   int leafletZoom) {
        return buildVisibleCells(southLat, westLon, northLat, eastLon, precisionForZoom(leafletZoom));
    }

    public static List<GridCell> buildVisibleCells(double southLat,
                                                   double westLon,
                                                   double northLat,
                                                   double eastLon,
                                                   GridPrecision precision) {
        if (!Double.isFinite(westLon) || !Double.isFinite(eastLon)) {
            return List.of();
        }

        double unwrappedEast = eastLon;
        if (unwrappedEast < westLon) {
            unwrappedEast += 360.0;
        }

        if (unwrappedEast - westLon >= 360.0) {
            return buildVisibleCellsWithinWorld(southLat, -180.0, northLat, 180.0, precision);
        }

        int firstWorld = (int) Math.floor((westLon + 180.0) / 360.0);
        int lastWorld = (int) Math.floor((unwrappedEast + 180.0 - EPSILON) / 360.0);
        Map<String, GridCell> cells = new LinkedHashMap<>();
        for (int world = firstWorld; world <= lastWorld; world++) {
            double worldWest = -180.0 + world * 360.0;
            double worldEast = 180.0 + world * 360.0;
            double localWest = Math.max(westLon, worldWest) - world * 360.0;
            double localEast = Math.min(unwrappedEast, worldEast) - world * 360.0;
            buildVisibleCellsWithinWorld(southLat, localWest, northLat, localEast, precision)
                    .forEach(cell -> cells.putIfAbsent(cell.locatorLabel(), cell));
        }
        return List.copyOf(cells.values());
    }

    private static List<GridCell> buildVisibleCellsWithinWorld(double southLat,
                                                               double westLon,
                                                               double northLat,
                                                               double eastLon,
                                                               GridPrecision precision) {

        double clampedSouth = clamp(southLat, -90.0 + EPSILON, 90.0 - EPSILON);
        double clampedNorth = clamp(northLat, -90.0 + EPSILON, 90.0 - EPSILON);
        double clampedWest = clamp(westLon, -180.0 + EPSILON, 180.0 - EPSILON);
        double clampedEast = clamp(eastLon, -180.0 + EPSILON, 180.0 - EPSILON);

        return switch (precision) {
            case FIELD_2 -> build2CharFields(clampedSouth, clampedWest, clampedNorth, clampedEast);
            case SQUARE_4 -> build4CharSquares(clampedSouth, clampedWest, clampedNorth, clampedEast);
            case SUBSQUARE_6 -> build6CharSubsquares(clampedSouth, clampedWest, clampedNorth, clampedEast);
        };
    }

    private static List<GridCell> build2CharFields(double southLat, double westLon, double northLat, double eastLon) {
        List<GridCell> cells = new ArrayList<>();

        int lonStart = clampIndex((int) Math.floor((westLon + 180.0) / 20.0), 0, 17);
        int lonEnd = clampIndex((int) Math.floor((eastLon + 180.0 - EPSILON) / 20.0), 0, 17);

        int latStart = clampIndex((int) Math.floor((southLat + 90.0) / 10.0), 0, 17);
        int latEnd = clampIndex((int) Math.floor((northLat + 90.0 - EPSILON) / 10.0), 0, 17);

        for (int lonIndex = lonStart; lonIndex <= lonEnd; lonIndex++) {
            for (int latIndex = latStart; latIndex <= latEnd; latIndex++) {
                double west = -180.0 + lonIndex * 20.0;
                double east = west + 20.0;
                double south = -90.0 + latIndex * 10.0;
                double north = south + 10.0;

                String label = "" + (char) ('A' + lonIndex) + (char) ('A' + latIndex);
                cells.add(new GridCell(label, south, west, north, east, latIndex, lonIndex));
            }
        }

        return cells;
    }

    private static List<GridCell> build4CharSquares(double southLat, double westLon, double northLat, double eastLon) {
        List<GridCell> cells = new ArrayList<>();

        int lonStart = clampIndex((int) Math.floor((westLon + 180.0) / 2.0), 0, 179);
        int lonEnd = clampIndex((int) Math.floor((eastLon + 180.0 - EPSILON) / 2.0), 0, 179);

        int latStart = clampIndex((int) Math.floor((southLat + 90.0) / 1.0), 0, 179);
        int latEnd = clampIndex((int) Math.floor((northLat + 90.0 - EPSILON) / 1.0), 0, 179);

        for (int lonTotalIndex = lonStart; lonTotalIndex <= lonEnd; lonTotalIndex++) {
            for (int latTotalIndex = latStart; latTotalIndex <= latEnd; latTotalIndex++) {

                int lonFieldIndex = lonTotalIndex / 10;
                int lonSquareIndex = lonTotalIndex % 10;

                int latFieldIndex = latTotalIndex / 10;
                int latSquareIndex = latTotalIndex % 10;

                double west = -180.0 + lonTotalIndex * 2.0;
                double east = west + 2.0;
                double south = -90.0 + latTotalIndex;
                double north = south + 1.0;

                String label = String.format(
                        Locale.ROOT,
                        "%c%c%d%d",
                        (char) ('A' + lonFieldIndex),
                        (char) ('A' + latFieldIndex),
                        lonSquareIndex,
                        latSquareIndex
                );

                cells.add(new GridCell(label, south, west, north, east, latTotalIndex, lonTotalIndex));
            }
        }

        return cells;
    }

    private static List<GridCell> build6CharSubsquares(double southLat, double westLon, double northLat, double eastLon) {
        List<GridCell> cells = new ArrayList<>();

        double lonStepDeg = 5.0 / 60.0;
        double latStepDeg = 2.5 / 60.0;

        int lonStart = clampIndex((int) Math.floor((westLon + 180.0) / lonStepDeg), 0, 4319);
        int lonEnd = clampIndex((int) Math.floor((eastLon + 180.0 - EPSILON) / lonStepDeg), 0, 4319);

        int latStart = clampIndex((int) Math.floor((southLat + 90.0) / latStepDeg), 0, 4319);
        int latEnd = clampIndex((int) Math.floor((northLat + 90.0 - EPSILON) / latStepDeg), 0, 4319);

        for (int lonTotalIndex = lonStart; lonTotalIndex <= lonEnd; lonTotalIndex++) {
            for (int latTotalIndex = latStart; latTotalIndex <= latEnd; latTotalIndex++) {

                int lonFieldIndex = lonTotalIndex / 240;
                int lonWithinField = lonTotalIndex % 240;
                int lonSquareIndex = lonWithinField / 24;
                int lonSubsquareIndex = lonWithinField % 24;

                int latFieldIndex = latTotalIndex / 240;
                int latWithinField = latTotalIndex % 240;
                int latSquareIndex = latWithinField / 24;
                int latSubsquareIndex = latWithinField % 24;

                double west = -180.0 + lonTotalIndex * lonStepDeg;
                double east = west + lonStepDeg;
                double south = -90.0 + latTotalIndex * latStepDeg;
                double north = south + latStepDeg;

                String label = String.format(
                        Locale.ROOT,
                        "%c%c%d%d%c%c",
                        (char) ('A' + lonFieldIndex),
                        (char) ('A' + latFieldIndex),
                        lonSquareIndex,
                        latSquareIndex,
                        (char) ('a' + lonSubsquareIndex),
                        (char) ('a' + latSubsquareIndex)
                );

                cells.add(new GridCell(label, south, west, north, east, latTotalIndex, lonTotalIndex));
            }
        }

        return cells;
    }

    private static int clampIndex(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
