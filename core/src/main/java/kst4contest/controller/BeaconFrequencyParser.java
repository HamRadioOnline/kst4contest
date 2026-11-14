package kst4contest.controller;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kst4contest.model.Band;

/** Parses complete beacon-reference frequencies without floating-point comparisons. */
final class BeaconFrequencyParser {
    private static final Pattern GROUPED = Pattern.compile(
            "^(\\d{2,5})[.,](\\d{1,3})(?:[.,](\\d{1,3}))?$"
    );
    private static final Pattern COMPACT = Pattern.compile("^\\d{5,8}$");

    private BeaconFrequencyParser() {
    }

    static OptionalLong parseHz(String value) {
        if (value == null) {
            return OptionalLong.empty();
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return OptionalLong.empty();
        }

        try {
            long frequencyHz;
            Matcher grouped = GROUPED.matcher(normalized);
            if (grouped.matches()) {
                long megahertz = Long.parseLong(grouped.group(1));
                long kilohertz = rightPadToThree(grouped.group(2));
                long hertz = grouped.group(3) == null
                        ? 0L
                        : rightPadToThree(grouped.group(3));
                frequencyHz = Math.addExact(
                        Math.multiplyExact(megahertz, 1_000_000L),
                        Math.addExact(Math.multiplyExact(kilohertz, 1_000L), hertz)
                );
            } else if (COMPACT.matcher(normalized).matches()) {
                long compact = Long.parseLong(normalized);
                frequencyHz = Math.multiplyExact(compact, 1_000L);
            } else {
                return OptionalLong.empty();
            }

            if (frequencyHz <= 0L
                    || Band.fromFrequency(frequencyHz / 1_000_000.0) == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(frequencyHz);
        } catch (ArithmeticException | NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    private static long rightPadToThree(String digits) {
        long value = Long.parseLong(digits);
        for (int index = digits.length(); index < 3; index++) {
            value *= 10L;
        }
        return value;
    }
}
