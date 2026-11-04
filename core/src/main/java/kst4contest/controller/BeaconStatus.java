package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;

/** Keeps category failures and builds the single status shown for both beacons. */
final class BeaconStatus {

	static final String SOURCE_NAME = ChatController.BEACON_STATUS_SOURCE;

	private boolean mainInvalid;
	private boolean secondInvalid;
	private String activity = "Beacon configuration loaded.";

	/** Records the result of one complete timer run. */
	synchronized void recordRun(
			boolean mainInvalid,
			boolean secondInvalid,
			String activity
	) {
		this.mainInvalid = mainInvalid;
		this.secondInvalid = secondInvalid;
		this.activity = activity;
	}

	/** Updates the explanation without hiding a still-relevant category failure. */
	synchronized void recordActivity(String activity) {
		this.activity = activity;
	}

	/** Builds a fresh aggregate from the current preferences and remembered failures. */
	synchronized ThreadStateMessage message(ChatPreferences preferences) {
		boolean secondAvailable = preferences.isLoginToSecondChatEnabled();
		boolean mainEnabled = preferences.isBcn_beaconsEnabledMainCat();
		boolean secondEnabled = secondAvailable
				&& preferences.isBcn_beaconsEnabledSecondCat();
		int available = secondAvailable ? 2 : 1;
		int enabled = (mainEnabled ? 1 : 0) + (secondEnabled ? 1 : 0);
		boolean critical = (mainInvalid && mainEnabled)
				|| (secondInvalid && secondEnabled);

		String description;
		if (critical) {
			description = "FAILED";
		} else if (enabled == 0) {
			description = "off";
		} else if (enabled == available) {
			description = "on";
		} else {
			description = enabled + "/" + available;
		}

		String details = "Main category beacon: " + onOff(mainEnabled)
				+ "; second category beacon: "
				+ (secondAvailable
						? onOff(secondEnabled)
						: "unavailable");
		String information = activity == null || activity.isBlank()
				? details
				: activity + "\n" + details;

		ThreadStateMessage message = new ThreadStateMessage(
				SOURCE_NAME,
				enabled > 0,
				information,
				critical
		);
		message.setRunningInformationTextDescription(description);
		return message;
	}

	private static String onOff(boolean enabled) {
		return enabled ? "on" : "off";
	}
}
