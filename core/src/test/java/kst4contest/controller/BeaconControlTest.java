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
		controller.setSecondBeaconEnabled(true);

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

		controller.toggleUsableBeacons();

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("on");

		controller.setSecondBeaconEnabled(false);
		controller.toggleUsableBeacons();

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("off");
	}

	@Test
	void anIndividualCheckboxPublishesThePartialStateImmediately() {
		preferences.setLoginToSecondChatEnabled(true);

		controller.setMainBeaconEnabled(true);

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
		assertLastDescription("1/2");
	}

	@Test
	void activationRejectsOnlyTheTemplateWhoseRequiredQrgIsMissing() {
		preferences.setLoginToSecondChatEnabled(true);
		preferences.setMYQRGSecondCat("1296.200");
		preferences.setBcn_beaconTextMainCat("CQ SECONDQRG");
		preferences.setBcn_beaconTextSecondCat("CQ MYQRG");

		controller.toggleUsableBeacons();

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
	}

	@Test
	void manualQrgRebasesBeforeAnAutomaticMoveIsCompared() {
		preferences.setBcn_beaconTextMainCat("CQ");
		controller.setMainBeaconEnabled(true);
		controller.applyManualQrg(RadioSlot.FIRST, "144.300");
		controller.applyExternalQrg(RadioSlot.FIRST, "144.305", 144_305_000L);
		assertTrue(preferences.isBcn_beaconsEnabledMainCat());

		controller.applyManualQrg(RadioSlot.FIRST, "144.310");
		controller.applyExternalQrg(RadioSlot.FIRST, "144.315.01", 144_315_010L);

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
	}

	@Test
	void nonPositiveExternalQrgCannotOverwriteTheDisplayOrClearTheReference() {
		preferences.setBcn_beaconTextMainCat("CQ");
		controller.applyManualQrg(RadioSlot.FIRST, "144.300");
		controller.setMainBeaconEnabled(true);

		controller.applyExternalQrg(RadioSlot.FIRST, "0", 0L);

		assertEquals("144.300", preferences.getMYQRGFirstCat().get());
		controller.applyExternalQrg(RadioSlot.FIRST, "144.306", 144_306_000L);
		assertFalse(preferences.isBcn_beaconsEnabledMainCat(),
				"the rejected packet must not clear the 144.300 reference");
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
