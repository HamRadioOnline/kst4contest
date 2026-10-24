package kst4contest.controller;

import kst4contest.model.Band;
import kst4contest.model.ChatMember;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime cache for worked Maidenhead gross fields per band.
 *
 * <p>The cache is used by the "new locator" station filter. It is kept in memory
 * for fast UI predicates and rebuilt from SQLite on startup/refresh. New QSO log
 * packets update both SQLite and this cache immediately.</p>
 */
public final class WorkedGrossFieldCache {

    private static final Pattern MAIDENHEAD_6_PATTERN =
            Pattern.compile("(?i)([A-R]{2}[0-9]{2}[A-X]{2})");

    private final Map<Band, Set<String>> workedGrossFieldsByBand = new EnumMap<>(Band.class);

    /**
     * Replaces the full cache content with data read from the database.
     *
     * @param databaseSnapshot map of band to worked gross fields
     */
    public synchronized void rebuildFromDatabaseSnapshot(Map<Band, Set<String>> databaseSnapshot) {
        workedGrossFieldsByBand.clear();

        if (databaseSnapshot == null) {
            return;
        }

        for (Map.Entry<Band, Set<String>> entry : databaseSnapshot.entrySet()) {
            Band band = entry.getKey();
            Set<String> grossFields = entry.getValue();

            if (band == null || grossFields == null) {
                continue;
            }

            Set<String> normalizedGrossFields = workedGrossFieldsByBand.computeIfAbsent(band, ignored -> new HashSet<>());
            for (String grossField : grossFields) {
                String normalizedGrossField = normalizeGrossField(grossField);
                if (normalizedGrossField != null) {
                    normalizedGrossFields.add(normalizedGrossField);
                }
            }
        }
    }

    /**
     * Adds one worked locator to the cache.
     *
     * @param band worked band
     * @param locatorOrGrossField six-character locator or four-character gross field
     */
    public synchronized void addWorked(Band band, String locatorOrGrossField) {
        String grossField = extractGrossField(locatorOrGrossField);
        if (band == null || grossField == null) {
            return;
        }

        workedGrossFieldsByBand
                .computeIfAbsent(band, ignored -> new HashSet<>())
                .add(grossField);
    }

    /**
     * Adds all worked-band flags from stored ChatMember rows. This is only a fallback
     * for legacy data before WorkedGrossField existed or when logger packets did not
     * provide a locator.
     *
     * @param storedMembers stored ChatMember rows
     */
    public synchronized void addWorkedBandsFromStoredChatMembers(Collection<ChatMember> storedMembers) {
        if (storedMembers == null) {
            return;
        }

        for (ChatMember member : storedMembers) {
            if (member == null) {
                continue;
            }

            String locator = member.getQra();
            if (member.isWorked144()) addWorked(Band.B_144, locator);
            if (member.isWorked432()) addWorked(Band.B_432, locator);
            if (member.isWorked1240()) addWorked(Band.B_1296, locator);
            if (member.isWorked2300()) addWorked(Band.B_2320, locator);
            if (member.isWorked3400()) addWorked(Band.B_3400, locator);
            if (member.isWorked5600()) addWorked(Band.B_5760, locator);
            if (member.isWorked10G()) addWorked(Band.B_10G, locator);
            if (member.isWorked50()) addWorked(Band.B_50, locator);
            if (member.isWorked70()) addWorked(Band.B_70, locator);
        }
    }

    /**
     * Checks whether a locator gross field is already worked on a band.
     *
     * @param band band to check
     * @param locatorOrGrossField six-character locator or four-character gross field
     * @return true if the gross field is already worked on that band
     */
    public synchronized boolean isGrossFieldWorked(Band band, String locatorOrGrossField) {
        String grossField = extractGrossField(locatorOrGrossField);
        if (band == null || grossField == null) {
            return false;
        }

        return workedGrossFieldsByBand
                .getOrDefault(band, Set.of())
                .contains(grossField);
    }

    /**
     * Checks whether a locator gross field has already been worked on any band.
     *
     * <p>The UI grid status and the "Only new grids" filter use this any-band logic.
     * This avoids user errors caused by wrong active-band settings and fits single
     * band contests such as NAC better.</p>
     *
     * @param locatorOrGrossField six-character locator or four-character gross field
     * @return true if the gross field exists in the cache on any band
     */
    public synchronized boolean isGrossFieldWorkedAny(String locatorOrGrossField) {
        String grossField = extractGrossField(locatorOrGrossField);
        if (grossField == null) {
            return false;
        }

        for (Set<String> workedGrossFields : workedGrossFieldsByBand.values()) {
            if (workedGrossFields != null && workedGrossFields.contains(grossField)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns an immutable snapshot of all gross fields worked on at least one
     * band. Map rendering uses the snapshot without holding this cache's monitor.
     *
     * @return immutable any-band gross-field snapshot
     */
    public synchronized Set<String> snapshotWorkedGrossFieldsAny() {
        Set<String> snapshot = new HashSet<>();
        for (Set<String> workedGrossFields : workedGrossFieldsByBand.values()) {
            if (workedGrossFields != null) {
                snapshot.addAll(workedGrossFields);
            }
        }
        return Set.copyOf(snapshot);
    }

    /**
     * Tries to normalize a locator. Accepts a plain six-character locator or extracts
     * one from exchange strings such as "001JO41HK".
     *
     * @param rawLocatorOrExchange raw locator/exchange text
     * @return normalized six-character locator, or null if none was found
     */
    public static String extractLocator6(String rawLocatorOrExchange) {
        if (rawLocatorOrExchange == null || rawLocatorOrExchange.isBlank()) {
            return null;
        }

        Matcher matcher = MAIDENHEAD_6_PATTERN.matcher(rawLocatorOrExchange.trim());
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).toUpperCase(Locale.ROOT);
    }

    /**
     * Extracts and normalizes the four-character gross field.
     *
     * @param rawLocatorOrGrossField six-character locator, four-character gross field or exchange text
     * @return normalized gross field, or null if no valid value is available
     */
    public static String extractGrossField(String rawLocatorOrGrossField) {
        if (rawLocatorOrGrossField == null || rawLocatorOrGrossField.isBlank()) {
            return null;
        }

        String trimmed = rawLocatorOrGrossField.trim().toUpperCase(Locale.ROOT);

        if (trimmed.matches("[A-R]{2}[0-9]{2}")) {
            return trimmed;
        }

        String locator6 = extractLocator6(trimmed);
        if (locator6 == null || locator6.length() < 4) {
            return null;
        }

        return locator6.substring(0, 4);
    }

    private static String normalizeGrossField(String grossField) {
        if (grossField == null) {
            return null;
        }

        String normalized = grossField.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-R]{2}[0-9]{2}") ? normalized : null;
    }
}
