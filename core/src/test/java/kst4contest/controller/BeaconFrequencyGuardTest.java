package kst4contest.controller;

import static kst4contest.controller.BeaconFrequencyGuard.Beacon.MAIN;
import static kst4contest.controller.BeaconFrequencyGuard.Beacon.SECOND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import kst4contest.controller.BeaconFrequencyGuard.Beacon;

class BeaconFrequencyGuardTest {

	private static final long QRG = 144_300_000L;
	private static final Set<Beacon> NONE = EnumSet.noneOf(Beacon.class);
	private static final Set<Beacon> BOTH = EnumSet.allOf(Beacon.class);

	private final BeaconFrequencyGuard guard = new BeaconFrequencyGuard();

	@Test
	void aMoveOfExactlyTheLimitKeepsTheBeaconAndOneStepMoreSwitchesItOff() {
		guard.report(RadioSlot.FIRST, QRG, false, NONE);
		guard.arm(MAIN);

		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG + 2_000, false, EnumSet.of(MAIN)));
		assertEquals(EnumSet.of(MAIN), guard.report(RadioSlot.FIRST, QRG + 2_010, false, EnumSet.of(MAIN)));
	}

	@Test
	void aMoveDownwardsCountsLikeAMoveUpwards() {
		guard.report(RadioSlot.FIRST, QRG, false, NONE);
		guard.arm(MAIN);

		assertEquals(EnumSet.of(MAIN), guard.report(RadioSlot.FIRST, QRG - 2_010, false, EnumSet.of(MAIN)));
	}

	/** Tuning slowly across the band never jumps, so the reference must not follow the radio. */
	@Test
	void smallStepsAddUpAgainstTheFrequencyTheBeaconWasSwitchedOnAt() {
		guard.report(RadioSlot.FIRST, QRG, false, NONE);
		guard.arm(MAIN);

		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG + 900, false, EnumSet.of(MAIN)));
		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG + 1_800, false, EnumSet.of(MAIN)));
		assertEquals(EnumSet.of(MAIN), guard.report(RadioSlot.FIRST, QRG + 2_700, false, EnumSet.of(MAIN)));
	}

	@Test
	void switchingABeaconOnAgainTakesTheCurrentFrequencyAsTheNewReference() {
		guard.report(RadioSlot.FIRST, QRG, false, NONE);
		guard.arm(MAIN);
		guard.report(RadioSlot.FIRST, QRG + 50_000, false, EnumSet.of(MAIN));

		guard.arm(MAIN);

		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG + 50_500, false, EnumSet.of(MAIN)));
	}

	@Test
	void aBeaconSwitchedOnBeforeAnyReportTakesTheFirstReportAsItsReference() {
		guard.arm(MAIN);

		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG, false, EnumSet.of(MAIN)));
		assertEquals(EnumSet.of(MAIN), guard.report(RadioSlot.FIRST, QRG + 5_000, false, EnumSet.of(MAIN)));
	}

	/** A switch that was flipped without arm() must not be judged against a stale reference. */
	@Test
	void aReferenceIsForgottenWhileItsBeaconIsOff() {
		guard.report(RadioSlot.FIRST, QRG, false, NONE);
		guard.arm(MAIN);
		guard.report(RadioSlot.FIRST, QRG + 50_000, false, NONE);

		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG + 50_100, false, EnumSet.of(MAIN)));
	}

	@Test
	void withoutSo2rTheOneRadioSwitchesBothBeaconsOff() {
		guard.report(RadioSlot.FIRST, QRG, false, NONE);
		guard.arm(MAIN);
		guard.arm(SECOND);

		assertEquals(BOTH, guard.report(RadioSlot.FIRST, QRG + 10_000, false, BOTH));
	}

	@Test
	void withSo2rEachRadioSwitchesOnlyTheBeaconOfItsOwnCategory() {
		guard.report(RadioSlot.FIRST, QRG, true, NONE);
		guard.report(RadioSlot.SECOND, 432_200_000L, true, NONE);
		guard.arm(MAIN);
		guard.arm(SECOND);

		assertEquals(EnumSet.of(SECOND), guard.report(RadioSlot.SECOND, 432_250_000L, true, BOTH));
		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG + 500, true, EnumSet.of(MAIN)));
		assertEquals(EnumSet.of(MAIN), guard.report(RadioSlot.FIRST, QRG + 10_000, true, EnumSet.of(MAIN)));
	}

	@Test
	void aBeaconThatIsOffIsNeverReported() {
		guard.report(RadioSlot.FIRST, QRG, false, NONE);
		guard.arm(MAIN);

		assertEquals(NONE, guard.report(RadioSlot.FIRST, QRG + 10_000, false, NONE));
	}
}
