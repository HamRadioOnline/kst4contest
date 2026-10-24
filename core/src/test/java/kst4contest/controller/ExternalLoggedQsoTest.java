package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import kst4contest.model.Band;
import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExternalLoggedQsoTest {

	@ParameterizedTest
	@MethodSource("loggerBandAliases")
	void resolvesEveryLoggerBandAlias(String rawValue, LoggedQsoBand expectedBand) {
		assertEquals(expectedBand, LoggedQsoBand.fromLoggerValue(rawValue));
	}

	@ParameterizedTest
	@MethodSource("winTestBandIds")
	void resolvesEveryExistingWinTestBandId(String rawValue, LoggedQsoBand expectedBand) {
		assertEquals(expectedBand, LoggedQsoBand.fromWinTestBandId(rawValue));
	}

	@Test
	void unknownOrMissingBandRemainsUnavailable() {
		assertNull(LoggedQsoBand.fromLoggerValue(null));
		assertNull(LoggedQsoBand.fromLoggerValue(""));
		assertNull(LoggedQsoBand.fromLoggerValue("unknown"));
		assertNull(LoggedQsoBand.fromWinTestBandId(null));
		assertNull(LoggedQsoBand.fromWinTestBandId("99"));
	}

	@Test
	void resolvesEveryProjectBandForManualWorkedActions() {
		for (Band band : Band.values()) {
			LoggedQsoBand loggedBand = LoggedQsoBand.fromProjectBand(band);

			assertEquals(band, loggedBand.getProjectBand());
		}
	}

	@Test
	void missingCallsignIsRejectedBeforeAnyStateIsBuilt() {
		assertTrue(ExternalLoggedQso.create(null, LoggedQsoBand.BAND_144, "JO50AA", "TEST").isEmpty());
		assertTrue(ExternalLoggedQso.create("   ", LoggedQsoBand.BAND_144, "JO50AA", "TEST").isEmpty());
	}

	@Test
	void missingBandSetsOnlyGlobalWorkedState() {
		ExternalLoggedQso qso = qso("DL1ABC", null, null);
		ChatMember workedCall = qso.toWorkedChatMember();

		assertTrue(workedCall.isWorked());
		assertFalse(workedCall.isWorked50());
		assertFalse(workedCall.isWorked70());
		assertFalse(workedCall.isWorked144());
		assertFalse(workedCall.isWorked2300());
		assertNull(workedCall.getQra());
	}

	@Test
	void winTest50And70MhzIdsSetTheirExistingFlags() {
		String qso50Packet = winTestAddQsoPacket("10", "DL1ABC");
		String qso70Packet = winTestAddQsoPacket("11", "DL1ABC");
		String band50Id = ReadUDPByWintestThread.extractBandIdFromWinTestAddQso(qso50Packet);
		String band70Id = ReadUDPByWintestThread.extractBandIdFromWinTestAddQso(qso70Packet);
		ChatMember worked50 = qso("DL1ABC", LoggedQsoBand.fromWinTestBandId(band50Id), null)
				.toWorkedChatMember();
		ChatMember worked70 = qso("DL1ABC", LoggedQsoBand.fromWinTestBandId(band70Id), null)
				.toWorkedChatMember();

		assertEquals("10", band50Id);
		assertEquals("11", band70Id);
		assertTrue(worked50.isWorked50());
		assertTrue(worked70.isWorked70());
	}

	@Test
	void winTest47And76GhzFlagsRemainAvailableWithoutInventingProjectBands() {
		LoggedQsoBand band47 = LoggedQsoBand.fromWinTestBandId("22");
		LoggedQsoBand band76 = LoggedQsoBand.fromWinTestBandId("23");
		ChatMember worked47 = qso("DL1ABC", band47, "JO50AA").toWorkedChatMember();
		ChatMember worked76 = qso("DL1ABC", band76, "JO50AA").toWorkedChatMember();

		assertTrue(worked47.isWorked47G());
		assertTrue(worked76.isWorked76G());
		assertNull(band47.getProjectBand());
		assertNull(band76.getProjectBand());
	}

	@Test
	void normalized2320AliasUpdatesEveryActiveCallsignVariant() {
		ChatMember firstVariant = member("9A0BB-2");
		ChatMember secondVariant = member("9A0BB-70");
		ChatMember unrelated = member("DL1ABC");
		ExternalLoggedQso qso = qso(
				"9A0BB", LoggedQsoBand.fromLoggerValue(" 2320 "), "JO50AA");

		int updated = ChatController.markExternalLoggedQsoMembers(
				List.of(firstVariant, secondVariant, unrelated), qso);

		assertEquals(2, updated);
		assertTrue(firstVariant.isWorked());
		assertTrue(firstVariant.isWorked2300());
		assertEquals("JO50AA", firstVariant.getQra());
		assertTrue(secondVariant.isWorked());
		assertTrue(secondVariant.isWorked2300());
		assertEquals("JO50AA", secondVariant.getQra());
		assertFalse(unrelated.isWorked());
	}

	private static Stream<Arguments> loggerBandAliases() {
		return Stream.of(
				Arguments.of("50", LoggedQsoBand.BAND_50),
				Arguments.of("6m", LoggedQsoBand.BAND_50),
				Arguments.of("70", LoggedQsoBand.BAND_70),
				Arguments.of("4m", LoggedQsoBand.BAND_70),
				Arguments.of("144", LoggedQsoBand.BAND_144),
				Arguments.of("2m", LoggedQsoBand.BAND_144),
				Arguments.of("432", LoggedQsoBand.BAND_432),
				Arguments.of("70cm", LoggedQsoBand.BAND_432),
				Arguments.of("1240", LoggedQsoBand.BAND_1296),
				Arguments.of("1296", LoggedQsoBand.BAND_1296),
				Arguments.of("23cm", LoggedQsoBand.BAND_1296),
				Arguments.of("2300", LoggedQsoBand.BAND_2320),
				Arguments.of(" 2320 ", LoggedQsoBand.BAND_2320),
				Arguments.of("13cm", LoggedQsoBand.BAND_2320),
				Arguments.of("3400", LoggedQsoBand.BAND_3400),
				Arguments.of("9cm", LoggedQsoBand.BAND_3400),
				Arguments.of("5600", LoggedQsoBand.BAND_5760),
				Arguments.of(" 5760 ", LoggedQsoBand.BAND_5760),
				Arguments.of("6cm", LoggedQsoBand.BAND_5760),
				Arguments.of("10G", LoggedQsoBand.BAND_10G),
				Arguments.of(" 10368 ", LoggedQsoBand.BAND_10G),
				Arguments.of("3cm", LoggedQsoBand.BAND_10G)
		);
	}

	private static Stream<Arguments> winTestBandIds() {
		return Stream.of(
				Arguments.of(" 10 ", LoggedQsoBand.BAND_50),
				Arguments.of("11", LoggedQsoBand.BAND_70),
				Arguments.of("12", LoggedQsoBand.BAND_144),
				Arguments.of("14", LoggedQsoBand.BAND_432),
				Arguments.of("16", LoggedQsoBand.BAND_1296),
				Arguments.of("17", LoggedQsoBand.BAND_2320),
				Arguments.of("18", LoggedQsoBand.BAND_3400),
				Arguments.of("19", LoggedQsoBand.BAND_5760),
				Arguments.of("20", LoggedQsoBand.BAND_10G),
				Arguments.of("21", LoggedQsoBand.BAND_24G),
				Arguments.of("22", LoggedQsoBand.BAND_47G),
				Arguments.of("23", LoggedQsoBand.BAND_76G)
		);
	}

	private static ExternalLoggedQso qso(
			String callSign,
			LoggedQsoBand band,
			String locator
	) {
		return ExternalLoggedQso.create(callSign, band, locator, "TEST").orElseThrow();
	}

	private static ChatMember member(String callSign) {
		ChatMember member = new ChatMember();
		member.setCallSign(callSign);
		return member;
	}

	private static String winTestAddQsoPacket(String bandId, String callSign) {
		return "ADDQSO: \"STN1\" \"\" \"STN1\" 1762202297 1440000 0 "
				+ bandId + " 0 0 0 2 2 \"" + callSign
				+ "\" \"599\" \"599001\" \"JO51UM\" \"\" \"\" 0 \"\" \"\" \"\" 44510";
	}
}
