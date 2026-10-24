package kst4contest.controller;

import java.util.Locale;

import kst4contest.model.Band;
import kst4contest.model.ChatMember;

/**
 * Normalized band of a QSO received from an external logger.
 *
 * <p>Logger protocols use different representations for the same amateur-radio
 * band. UCXLog-compatible XML may contain a frequency-like number or a metre /
 * centimetre label, while Win-Test uses its own numeric band IDs. Resolving the
 * raw value once prevents the Worked flag and gross-field update from interpreting
 * the same packet differently.</p>
 */
public enum LoggedQsoBand {
	BAND_50(Band.B_50),
	BAND_70(Band.B_70),
	BAND_144(Band.B_144),
	BAND_432(Band.B_432),
	BAND_1296(Band.B_1296),
	BAND_2320(Band.B_2320),
	BAND_3400(Band.B_3400),
	BAND_5760(Band.B_5760),
	BAND_10G(Band.B_10G),
	BAND_24G(Band.B_24G),
	BAND_47G(null),
	BAND_76G(null);

	private final Band projectBand;

	LoggedQsoBand(Band projectBand) {
		this.projectBand = projectBand;
	}

	/**
	 * Resolves aliases used by UCXLog, N1MM+, DXLog.net, QARTest and compatible
	 * loggers. The input is trimmed and case-normalized exactly once here.
	 *
	 * @param rawBand band value received in the XML packet
	 * @return normalized band, or {@code null} for a missing or unknown value
	 */
	public static LoggedQsoBand fromLoggerValue(String rawBand) {
		if (rawBand == null) {
			return null;
		}

		String normalizedBand = rawBand.trim().toLowerCase(Locale.ROOT);
		return switch (normalizedBand) {
			case "50", "6m" -> BAND_50;
			case "70", "4m" -> BAND_70;
			case "144", "2m" -> BAND_144;
			case "432", "70cm" -> BAND_432;
			case "1240", "1296", "23cm" -> BAND_1296;
			case "2300", "2320", "13cm" -> BAND_2320;
			case "3400", "9cm" -> BAND_3400;
			case "5600", "5760", "6cm" -> BAND_5760;
			case "10g", "10368", "3cm" -> BAND_10G;
			default -> null;
		};
	}

	/**
	 * Resolves the established Win-Test ADDQSO band IDs. IDs 22 and 23 retain
	 * their existing Worked flags even though the project Band enum has no 47 GHz
	 * or 76 GHz value for gross-field storage.
	 *
	 * @param rawBandId Win-Test band ID
	 * @return normalized band, or {@code null} for a missing or unknown ID
	 */
	public static LoggedQsoBand fromWinTestBandId(String rawBandId) {
		if (rawBandId == null) {
			return null;
		}

		String normalizedBandId = rawBandId.trim();
		return switch (normalizedBandId) {
			case "10" -> BAND_50;
			case "11" -> BAND_70;
			case "12" -> BAND_144;
			case "14" -> BAND_432;
			case "16" -> BAND_1296;
			case "17" -> BAND_2320;
			case "18" -> BAND_3400;
			case "19" -> BAND_5760;
			case "20" -> BAND_10G;
			case "21" -> BAND_24G;
			case "22" -> BAND_47G;
			case "23" -> BAND_76G;
			default -> null;
		};
	}

	/**
	 * Resolves a project band for UI actions that must follow the same Worked
	 * processing as an external logger contact.
	 *
	 * @param band project band
	 * @return normalized logged-QSO band, or {@code null} for a missing band
	 */
	public static LoggedQsoBand fromProjectBand(Band band) {
		if (band == null) {
			return null;
		}

		for (LoggedQsoBand loggedBand : values()) {
			if (loggedBand.projectBand == band) {
				return loggedBand;
			}
		}

		return null;
	}

	/**
	 * Returns the common Band value used by worked-grid persistence.
	 *
	 * @return project band, or {@code null} where no matching Band value exists
	 */
	public Band getProjectBand() {
		return projectBand;
	}

	/**
	 * Applies the established per-band Worked flag to a chat member.
	 *
	 * @param member worked station or active callsign variant
	 */
	public void applyWorkedFlag(ChatMember member) {
		if (member == null) {
			return;
		}

		switch (this) {
			case BAND_50 -> member.setWorked50(true);
			case BAND_70 -> member.setWorked70(true);
			case BAND_144 -> member.setWorked144(true);
			case BAND_432 -> member.setWorked432(true);
			case BAND_1296 -> member.setWorked1240(true);
			case BAND_2320 -> member.setWorked2300(true);
			case BAND_3400 -> member.setWorked3400(true);
			case BAND_5760 -> member.setWorked5600(true);
			case BAND_10G -> member.setWorked10G(true);
			case BAND_24G -> member.setWorked24G(true);
			case BAND_47G -> member.setWorked47G(true);
			case BAND_76G -> member.setWorked76G(true);
		}
	}
}
