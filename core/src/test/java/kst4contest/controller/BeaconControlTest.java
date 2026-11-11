package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;

class BeaconControlTest {

	@TempDir
	Path tempDir;

	private ChatPreferences preferences;
	private ChatController controller;
	private StatusUpdateListener listener;

	@BeforeEach
	void setUp() {
		preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));
		controller = new ChatController();
		controller.setChatPreferences(preferences);
		listener = mock(StatusUpdateListener.class);
		controller.setStatusListener(listener);
	}

	@Test
	void toggleWithSecondChatDisabledChangesOnlyTheMainBeacon() {
		preferences.setLoginToSecondChatEnabled(false);
		preferences.setBcn_beaconsEnabledMainCat(false);
		preferences.setBcn_beaconsEnabledSecondCat(true);

		controller.toggleUsableBeacons();

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat(),
				"the dormant second-category choice must not be overwritten");
		assertLastDescription("on");

		controller.toggleUsableBeacons();

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("off");
	}

	@Test
	void toggleWithSecondChatEnabledSwitchesBothUsableBeacons() {
		preferences.setLoginToSecondChatEnabled(true);
		preferences.setBcn_beaconsEnabledMainCat(false);
		preferences.setBcn_beaconsEnabledSecondCat(false);

		controller.toggleUsableBeacons();

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("on");

		preferences.setBcn_beaconsEnabledSecondCat(false);
		controller.toggleUsableBeacons();

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("off");
	}

	@Test
	void anIndividualCheckboxPublishesThePartialStateImmediately() {
		preferences.setLoginToSecondChatEnabled(true);
		preferences.setBcn_beaconsEnabledMainCat(false);
		preferences.setBcn_beaconsEnabledSecondCat(false);

		controller.setMainBeaconEnabled(true);

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("1/2");
	}

	@Test
	void aTransceiverMoveBeyondTwoKilohertzSwitchesTheBeaconsOffAndSaysWhy() {
		preferences.setLoginToSecondChatEnabled(true);
		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_300_000L);
		controller.setMainBeaconEnabled(true);
		controller.setSecondBeaconEnabled(true);

		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_301_500L);

		assertTrue(preferences.isBcn_beaconsEnabledMainCat(), "1.5 kHz is within the limit");
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat());

		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_302_500L);

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat(),
				"without SO2R the one reported radio stands for both categories");
		assertLastDescription("off");
		assertTrue(lastMessage().getRunningInformation().contains("2 kHz"),
				lastMessage().getRunningInformation());
	}

	@Test
	void withSo2rARadioSwitchesOnlyTheBeaconOfItsOwnCategoryOff() {
		preferences.setLoginToSecondChatEnabled(true);
		preferences.setTrxSynch_so2rEnabled(true);
		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_300_000L);
		controller.onTrxFrequencyReported(RadioSlot.SECOND, 432_200_000L);
		controller.toggleUsableBeacons();

		controller.onTrxFrequencyReported(RadioSlot.SECOND, 432_250_000L);

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("1/2");
	}

	@Test
	void aBeaconSwitchedOnAgainAfterTheMoveStaysOnAtTheNewFrequency() {
		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_300_000L);
		controller.setMainBeaconEnabled(true);
		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_350_000L);
		assertFalse(preferences.isBcn_beaconsEnabledMainCat());

		controller.toggleUsableBeacons();
		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_350_500L);

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
	}

	@Test
	void aTransceiverMoveWhileNoBeaconIsOnPublishesNothing() {
		controller.onTrxFrequencyReported(RadioSlot.FIRST, 144_300_000L);
		controller.onTrxFrequencyReported(RadioSlot.FIRST, 432_200_000L);

		org.mockito.Mockito.verifyNoInteractions(listener);
	}

	/** Stored values of earlier releases may lie below the minimum of five minutes. */
	@Test
	void aStoredIntervalBelowTheMinimumIsRaisedWhenTheTimerStarts() {
		preferences.setBcn_beaconIntervalInMinutesMainCat(1);
		preferences.setBcn_beaconIntervalInMinutesSecondCat(3);

		controller.onOn4KstConnectionOnline();
		controller.onOn4KstConnectionLost();

		assertEquals(5, ChatController.MIN_BEACON_INTERVAL_MINUTES);
		assertEquals(5, preferences.getBcn_beaconIntervalInMinutesMainCat());
		assertEquals(5, preferences.getBcn_beaconIntervalInMinutesSecondCat());
	}

	private ThreadStateMessage lastMessage() {
		ArgumentCaptor<ThreadStateMessage> message = ArgumentCaptor.forClass(ThreadStateMessage.class);
		org.mockito.Mockito.verify(listener, org.mockito.Mockito.atLeastOnce())
				.onThreadStatusChanged(org.mockito.ArgumentMatchers.anyString(), message.capture());
		return message.getValue();
	}

	private void assertLastDescription(String expected) {
		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ThreadStateMessage> message = ArgumentCaptor.forClass(ThreadStateMessage.class);
		org.mockito.Mockito.verify(listener, org.mockito.Mockito.atLeastOnce())
				.onThreadStatusChanged(key.capture(), message.capture());
		assertEquals("MyBeacon", key.getValue());
		assertEquals(expected, message.getValue().getRunningInformationTextDescription());
	}
}
