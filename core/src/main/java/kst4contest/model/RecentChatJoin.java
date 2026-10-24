package kst4contest.model;

import java.util.Locale;
import java.util.Objects;

/**
 * One live ON4KST join event.
 *
 * <p>The full callsign and category form the identity. The timestamp belongs to the
 * first event while that identity remains active; ordinary user updates must not renew it.</p>
 */
public final class RecentChatJoin {

	private final String callSign;
	private final int categoryNumber;
	private final long joinedAtEpochMillis;

	public RecentChatJoin(
			String callSign,
			int categoryNumber,
			long joinedAtEpochMillis
	) {
		this.callSign = Objects.requireNonNull(callSign, "callSign")
				.trim()
				.toUpperCase(Locale.ROOT);
		this.categoryNumber = categoryNumber;
		this.joinedAtEpochMillis = joinedAtEpochMillis;
	}

	public String getCallSign() {
		return callSign;
	}

	public int getCategoryNumber() {
		return categoryNumber;
	}

	public long getJoinedAtEpochMillis() {
		return joinedAtEpochMillis;
	}

	/** Returns whether the event belongs to the same complete chat identity. */
	public boolean matches(String otherCallSign, int otherCategoryNumber) {
		return otherCallSign != null
				&& categoryNumber == otherCategoryNumber
				&& callSign.equalsIgnoreCase(otherCallSign.trim());
	}
}
