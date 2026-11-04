package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;

class BeaconTaskTest {

	@TempDir
	Path tempDir;

	private ChatPreferences preferences;
	private LinkedBlockingQueue<ChatMessage> txBus;
	private List<ThreadStateMessage> statuses;
	private ChatController controller;
	private BeaconTask task;

	@BeforeEach
	void setUp() {
		preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));
		preferences.setLoginChatCategoryMain(new ChatCategory(ChatCategory.VUHF));
		preferences.setLoginChatCategorySecond(new ChatCategory(ChatCategory.MICROWAVE));
		preferences.setLoginToSecondChatEnabled(true);
		preferences.setBcn_beaconTextMainCat("CQ MAIN");
		preferences.setBcn_beaconTextSecondCat("CQ SECOND");
		preferences.setBcn_beaconsEnabledMainCat(false);
		preferences.setBcn_beaconsEnabledSecondCat(false);

		statuses = new ArrayList<>();
		controller = new ChatController();
		controller.setChatPreferences(preferences);
		txBus = new LinkedBlockingQueue<>();
		controller.setMessageTXBus(txBus);
		controller.setStatusListener(new StatusUpdateListener() {
			@Override
			public void onThreadStatusChanged(String key, ThreadStateMessage message) {
				statuses.add(message);
			}

			@Override
			public void onUserListUpdated() {
				// Not used by beacon tests.
			}

			@Override
			public void onUserListUpdated(String reason) {
				// Not used by beacon tests.
			}
		});
		task = new BeaconTask(controller, (key, message) -> statuses.add(message));
	}

	@Test
	void invalidEnabledBeaconIsNotQueuedAndReportsOneCriticalAggregateState() {
		preferences.setBcn_beaconsEnabledMainCat(true);
		preferences.setBcn_beaconTextMainCat("invalid|template");

		task.run();

		assertTrue(txBus.isEmpty());
		assertEquals(1, statuses.size());
		ThreadStateMessage status = statuses.get(0);
		assertEquals("FAILED", status.getRunningInformationTextDescription());
		assertTrue(status.isRunning());
		assertTrue(status.isCriticalState());
	}

	@Test
	void changingTheOtherCheckboxDoesNotHideAnActiveCategoryFailure() {
		preferences.setBcn_beaconsEnabledMainCat(true);
		preferences.setBcn_beaconTextMainCat("invalid|template");
		task.run();
		statuses.clear();

		controller.setSecondBeaconEnabled(true);

		assertEquals(1, statuses.size());
		ThreadStateMessage status = statuses.get(0);
		assertEquals("FAILED", status.getRunningInformationTextDescription());
		assertTrue(status.isCriticalState());
	}

	@Test
	void everyRunUsesTheCurrentEnableFlagsAndReportsOneAggregateState() {
		task.run();
		assertTrue(txBus.isEmpty());
		assertAggregateStatus("off", false);

		statuses.clear();
		preferences.setBcn_beaconsEnabledMainCat(true);
		task.run();
		assertEquals(1, txBus.size());
		assertEquals(ChatCategory.VUHF, txBus.peek().getChatCategory().getCategoryNumber());
		assertAggregateStatus("1/2", true);

		statuses.clear();
		preferences.setBcn_beaconsEnabledSecondCat(true);
		task.run();
		assertEquals(3, txBus.size());
		assertAggregateStatus("on", true);
	}

	private void assertAggregateStatus(String description, boolean running) {
		assertEquals(1, statuses.size(), "one timer run must leave one authoritative chip state");
		ThreadStateMessage status = statuses.get(0);
		assertEquals(description, status.getRunningInformationTextDescription());
		assertEquals(running, status.isRunning());
		assertFalse(status.isCriticalState());
	}
}
