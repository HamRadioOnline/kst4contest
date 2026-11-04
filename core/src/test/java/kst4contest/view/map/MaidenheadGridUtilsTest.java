package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaidenheadGridUtilsTest {

    @Test
    void a_view_crossing_the_anti_meridian_contains_cells_from_both_world_edges() {
        var cells = MaidenheadGridUtils.buildVisibleCells(
                -2.0,
                175.0,
                2.0,
                185.0,
                MaidenheadGridUtils.GridPrecision.FIELD_2
        );
        Set<String> labels = cells.stream()
                .map(MaidenheadGridUtils.GridCell::locatorLabel)
                .collect(Collectors.toSet());

        assertTrue(labels.stream().anyMatch(label -> label.startsWith("R")), labels.toString());
        assertTrue(labels.stream().anyMatch(label -> label.startsWith("A")), labels.toString());
        assertEquals(labels.size(), cells.size(), "a wrapped cell was returned twice");
    }
}
