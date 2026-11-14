package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeaconFrequencyParserTest {

    @Test
    void parsesEveryCompleteQrgFormWithoutLosingHertz() {
        assertHz("144.300", 144_300_000L);
        assertHz("144,300", 144_300_000L);
        assertHz("144.300.03", 144_300_030L);
        assertHz("1296.123.000", 1_296_123_000L);
        assertHz("144300", 144_300_000L);
        assertHz("10368100", 10_368_100_000L);
    }

    @Test
    void rejectsIncompleteMalformedAndUnsupportedValues() {
        for (String value : new String[] {null, "", " ", "210", "144 MHz", "0", "-144.300",
                "999999999999999999999999999999"}) {
            assertTrue(BeaconFrequencyParser.parseHz(value).isEmpty(), value);
        }
    }

    private static void assertHz(String value, long expected) {
        assertEquals(expected, BeaconFrequencyParser.parseHz(value).orElseThrow());
    }
}
