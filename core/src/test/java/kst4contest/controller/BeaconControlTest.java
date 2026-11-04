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

	private void assertLastDescription(String expected) {
		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ThreadStateMessage> message = ArgumentCaptor.forClass(ThreadStateMessage.class);
		org.mockito.Mockito.verify(listener, org.mockito.Mockito.atLeastOnce())
				.onThreadStatusChanged(key.capture(), message.capture());
		assertEquals("MyBeacon", key.getValue());
		assertEquals(expected, message.getValue().getRunningInformationTextDescription());
	}
}
