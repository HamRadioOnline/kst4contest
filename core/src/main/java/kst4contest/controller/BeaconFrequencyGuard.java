package kst4contest.controller;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Notices that a transceiver has left the frequency a beacon was switched on at.
 *
 * <p>A beacon announces where the station is calling. It keeps doing so after the
 * operator has tuned away, and nothing in the chat window shows that the announced
 * frequency has become wrong. This guard remembers the frequency each radio reported
 * when a beacon was switched on and names the beacons whose radio has since moved by
 * more than {@link #LIMIT_HZ}.</p>
 *
 * <p>The comparison is made against the frequency at switch-on, not against the
 * previous report: tuning slowly across the band never jumps, yet it ends just as far
 * away.</p>
 *
 * <p>Pure and free of preferences, so the rule is tested without a controller. The
 * caller says which beacons are on and switches off the ones returned.</p>
 */
final class BeaconFrequencyGuard {

	/** The two beacons, named after the chat category they send in. */
	enum Beacon {
		MAIN,
		SECOND
	}

	/** A move of exactly this size still counts as staying on the frequency. */
	static final long LIMIT_HZ = 2_000L;

	private final Map<RadioSlot, Long> lastReportedHz = new EnumMap<>(RadioSlot.class);
	private final Map<Beacon, Map<RadioSlot, Long>> referenceHz = new EnumMap<>(Beacon.class);

	/**
	 * Takes the frequencies last reported as the reference of a beacon that is being
	 * switched on. A radio that has not reported yet contributes its first report.
	 */
	synchronized void arm(Beacon beacon) {
		referenceHz.put(beacon, new EnumMap<>(lastReportedHz));
	}

	/**
	 * Records the frequency a radio reported and compares it with the references.
	 *
	 * <p>Without SO2R only one radio is known, so its move concerns both beacons. With
	 * SO2R radio 1 belongs to the main category and radio 2 to the second one, the same
	 * assignment MYQRG and SECONDQRG follow.</p>
	 *
	 * @param slot the radio the report belongs to
	 * @param frequencyHz the reported frequency
	 * @param so2rEnabled whether the two radios are told apart
	 * @param enabledBeacons the beacons that are switched on right now
	 * @return the enabled beacons whose radio moved beyond the limit; the caller
	 *         switches these off
	 */
	synchronized Set<Beacon> report(
			RadioSlot slot,
			long frequencyHz,
			boolean so2rEnabled,
			Set<Beacon> enabledBeacons
	) {
		lastReportedHz.put(slot, frequencyHz);

		Set<Beacon> moved = EnumSet.noneOf(Beacon.class);
		for (Beacon beacon : Beacon.values()) {
			if (!enabledBeacons.contains(beacon)) {
				/*
				 * A reference only means something while its beacon is on. Dropping it
				 * here keeps a switch that was flipped without arm() from being judged
				 * against the frequency of an earlier activation.
				 */
				referenceHz.remove(beacon);
				continue;
			}
			if (!watches(so2rEnabled, slot, beacon)) {
				continue;
			}

			Long reference = referenceHz
					.computeIfAbsent(beacon, unused -> new EnumMap<>(RadioSlot.class))
					.putIfAbsent(slot, frequencyHz);
			if (reference != null && Math.abs(frequencyHz - reference) > LIMIT_HZ) {
				moved.add(beacon);
				referenceHz.remove(beacon);
			}
		}
		return moved;
	}

	private static boolean watches(boolean so2rEnabled, RadioSlot slot, Beacon beacon) {
		if (!so2rEnabled) {
			return true;
		}
		return (slot == RadioSlot.FIRST) == (beacon == Beacon.MAIN);
	}
}
