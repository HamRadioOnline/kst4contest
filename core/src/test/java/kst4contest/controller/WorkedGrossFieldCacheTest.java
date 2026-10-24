package kst4contest.controller;

import kst4contest.model.Band;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkedGrossFieldCacheTest {

    @Test
    void anyBandSnapshotContainsUniqueNormalizedGrossFieldsAndIsImmutable() {
        WorkedGrossFieldCache cache = new WorkedGrossFieldCache();
        cache.addWorked(Band.B_144, "jo51aa");
        cache.addWorked(Band.B_432, "JO51BB");
        cache.addWorked(Band.B_1296, "JN49AA");

        Set<String> snapshot = cache.snapshotWorkedGrossFieldsAny();

        assertEquals(Set.of("JO51", "JN49"), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("JO50"));
    }
}
