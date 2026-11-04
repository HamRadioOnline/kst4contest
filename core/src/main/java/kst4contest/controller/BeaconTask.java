package kst4contest.controller;

import java.util.TimerTask;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMessage;

/**
 * Sends the configured public-chat beacons for both active chat categories.
 *
 * <p>Both categories deliberately share one timer and therefore the same
 * interval. Their enable flags and message templates remain independent. Every
 * run reads the current preferences, resolves global message variables and
 * sends only the categories which are currently enabled.</p>
 *
 * <p>Beacon messages use the regular outbound chat-message pipeline. They are
 * not assembled as raw ON4KST frames, because that would bypass the common
 * category, delimiter and message-text validation.</p>
 */
public class BeaconTask extends TimerTask {

	private final ChatController chatController;
	private final ThreadStatusCallback callbackToController;
	private final BeaconStatus beaconStatus;

	/**
	 * Creates one execution of the shared beacon timer.
	 *
	 * @param chatController controller providing preferences and the TX queue
	 * @param callbackToController callback used by the thread-status display
	 */
	public BeaconTask(
			ChatController chatController,
			ThreadStatusCallback callbackToController
	) {
		this.chatController = chatController;
		this.callbackToController = callbackToController;
		this.beaconStatus = chatController.beaconStatusState();
	}

	@Override
	public void run() {
		Thread.currentThread().setName("BeaconTask");
		Attempt main = sendMainCategoryBeacon();
		Attempt second = sendSecondCategoryBeacon();
		String activity = "Main category: " + main.description
				+ "; second category: " + second.description;
		beaconStatus.recordRun(
				main == Attempt.INVALID,
				second == Attempt.INVALID,
				activity
		);
		callbackToController.onThreadStatus(
				BeaconStatus.SOURCE_NAME,
				beaconStatus.message(chatController.getChatPreferences())
		);
	}

	/**
	 * Sends the main-category beacon if it is currently enabled.
	 */
	private Attempt sendMainCategoryBeacon() {
		if (!chatController.getChatPreferences()
				.isBcn_beaconsEnabledMainCat()) {
			return Attempt.OFF;
		}

		ChatMessage beaconMessage = buildBeaconMessage(
				chatController.getChatPreferences()
						.getLoginChatCategoryMain(),
				chatController.getChatPreferences()
						.getBcn_beaconTextMainCat(),
				"main category"
		);

		if (beaconMessage == null) {
			return Attempt.INVALID;
		}

		System.out.println(
				new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [BeaconTask, Info]: Sending main-category CQ: "
						+ beaconMessage.getMessageText()
		);

		chatController.getMessageTXBus().add(beaconMessage);
		return Attempt.SENT;
	}

	/**
	 * Sends the second-category beacon if the second login and its beacon are
	 * currently enabled.
	 */
	private Attempt sendSecondCategoryBeacon() {
		if (!chatController.getChatPreferences().isLoginToSecondChatEnabled()) {
			return Attempt.UNAVAILABLE;
		}
		if (!chatController.getChatPreferences().isBcn_beaconsEnabledSecondCat()) {
			return Attempt.OFF;
		}

		ChatMessage beaconMessage = buildBeaconMessage(
				chatController.getChatPreferences()
						.getLoginChatCategorySecond(),
				chatController.getChatPreferences()
						.getBcn_beaconTextSecondCat(),
				"second category"
		);

		if (beaconMessage == null) {
			return Attempt.INVALID;
		}

		System.out.println(
				new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [BeaconTask, Info]: Sending second-category CQ: "
						+ beaconMessage.getMessageText()
		);

		chatController.getMessageTXBus().add(beaconMessage);
		return Attempt.SENT;
	}

	/**
	 * Resolves and validates one beacon before placing it in the regular outbound
	 * message queue.
	 *
	 * <p>The returned message contains only the public-chat payload and its chat
	 * category. {@link WriteThread} creates the final ON4KST frame through
	 * {@link On4KstProtocol#chatMessage(int, String)}. This prevents a configurable
	 * beacon text from bypassing the common protocol validation.</p>
	 *
	 * @param category target ON4KST chat category
	 * @param configuredText configured beacon template
	 * @param categoryDescription text used in diagnostic output
	 * @return prepared message, or {@code null} if the category or text is invalid
	 */
	private ChatMessage buildBeaconMessage(
			ChatCategory category,
			String configuredText,
			String categoryDescription
	) {
		try {
			if (category == null) {
				throw new IllegalArgumentException(
						"No chat category is configured."
				);
			}

			On4KstProtocol.category(category.getCategoryNumber());

			String resolvedText =
					chatController.resolveAndValidateBeaconText(
							configuredText
					);

			ChatMessage beaconMessage = new ChatMessage();
			beaconMessage.setMessageText(resolvedText);
			beaconMessage.setChatCategory(category);
			beaconMessage.setMessageDirectedToServer(false);

			return beaconMessage;
		} catch (IllegalArgumentException exception) {
			System.out.println(
					"[BeaconTask, Warning]: Beacon for "
							+ categoryDescription
							+ " was not queued: "
							+ exception.getMessage()
			);
			return null;
		}
	}

	private enum Attempt {
		SENT("sent"),
		OFF("off"),
		UNAVAILABLE("unavailable"),
		INVALID("invalid text");

		private final String description;

		Attempt(String description) {
			this.description = description;
		}
	}
}
