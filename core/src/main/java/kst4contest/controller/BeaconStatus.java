package kst4contest.controller;

import kst4contest.model.BeaconStatusSnapshot;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;

/** Builds the single status shown for both beacons from their session state. */
final class BeaconStatus {

	static final String SOURCE_NAME = ChatController.BEACON_STATUS_SOURCE;

	private String activity = "Beacon configuration loaded.";

	/** Records the result of one complete timer run. */
	synchronized void recordRun(String activity) {
		this.activity = activity;
	}

	/** Updates the explanation without hiding a still-relevant category failure. */
	synchronized void recordActivity(String activity) {
		this.activity = activity;
	}

	/** Builds a fresh aggregate from the current preferences and remembered failures. */
	synchronized ThreadStateMessage message(
			ChatPreferences preferences,
			BeaconSessionState sessionState
	) {
		BeaconStatusSnapshot snapshot = sessionState.snapshot(preferences);
		boolean critical = snapshot.getAggregateState()
				== BeaconStatusSnapshot.AggregateState.FAILED;
		boolean running = snapshot.getAggregateState()
				!= BeaconStatusSnapshot.AggregateState.OFF;

		String description;
		switch (snapshot.getAggregateState()) {
			case FAILED -> description = "FAILED";
			case OFF -> description = "off";
			case ON -> description = "on";
			case PARTIAL -> description = "1/2";
			default -> throw new IllegalStateException("Unknown beacon status");
		}

		ThreadStateMessage message = new ThreadStateMessage(
				SOURCE_NAME,
				running,
				activity,
				critical
		);
		message.setRunningInformationTextDescription(description);
		message.setBeaconStatusSnapshot(snapshot);
		return message;
	}
}
