package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import kst4contest.model.ChatPreferences;
import java.nio.file.Path;


class On4KstProtocolTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsLoginWithReplayOverlap() {
        assertEquals(
                "LOGINC|DL1ABC|secret|2|KST4Contest v1.2.3|25|0|1|12344|0|",
                On4KstProtocol.login(
                        "DL1ABC", "secret", 2,
                        "KST4Contest v1.2.3", 12_344L));
    }

    @Test
    void buildsContextSafeSecondChatFrames() {
        assertEquals("SDONE|2|", On4KstProtocol.settingsDone(2));
        assertEquals("ACHAT|3|25|10|2|100|0|",
                On4KstProtocol.addChat(3, 100L));
        assertEquals("MSG|2|0|/SETLOC JO31AA|0|",
                On4KstProtocol.setLocator(2, "jo31aa"));
        assertEquals("MSG|3|0|/SETNAME 10G 10368.200|0|",
                On4KstProtocol.setName(3, "10G 10368.200"));
    }

    @Test
    void stripsOnlyTrailingLineEndings() {
        assertEquals("CK|", On4KstProtocol.normalizeRawFrame("CK|\r\n"));
        assertThrows(IllegalArgumentException.class,
                () -> On4KstProtocol.normalizeRawFrame("CK|\rBROKEN"));
    }

    @Test
    void rejectsValuesThatCouldCreateASecondProtocolFrame() {
        assertThrows(IllegalArgumentException.class,
                () -> On4KstProtocol.chatMessage(2, "hello|0|"));
        assertThrows(IllegalArgumentException.class,
                () -> On4KstProtocol.chatMessage(2, "hello\r\nQUIT|"));
        assertThrows(IllegalArgumentException.class,
                () -> On4KstProtocol.login(
                        "DL1ABC", "bad|password", 2, "client", 0L));
    }

    @Test
    void rejectsInvalidLocatorAndCategoryBeforeTheyReachTheServer() {
        assertThrows(IllegalArgumentException.class,
                () -> On4KstProtocol.setLocator(2, "JO31"));
        assertThrows(IllegalArgumentException.class,
                () -> On4KstProtocol.settingsDone(99));
    }

    @Test
    void convertsBothHistoryAndLiveMessageTimestampsForReconnect() {
        assertEquals(1_186_819_108L,
                On4KstConnectionManager.parseMessageTimestamp(
                        "CR|2|1186819108|EA6VQ|Gabriel|0|msg|0|"));
        assertEquals(
                LocalDateTime.of(2026, 8, 13, 12, 34, 56)
                        .toEpochSecond(ZoneOffset.UTC),
                On4KstConnectionManager.parseMessageTimestamp(
                        "CH|2|20260813123456|DL1ABC|Op|0|msg|0|"));
    }

    @Test
    void resolvesBeaconVariablesBeforeApplyingProtocolValidation() {
        ChatPreferences preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));
        preferences.setMYQRGFirstCat("144.300");

        ChatController controller = new ChatController();
        controller.setChatPreferences(preferences);

        controller.validateBeaconTemplate(
                "calling cq at MYQRG"
        );

        assertEquals(
                "calling cq at 144.300 *a*",
                controller.resolveAndValidateBeaconText(
                        "calling cq at MYQRG"
                )
        );
    }

    @Test
    void keepsThe120CharacterPayloadAllowanceWithA124CharacterFinalBeacon() {
        ChatPreferences preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));
        ChatController controller = new ChatController();
        controller.setChatPreferences(preferences);
        String payload = "x".repeat(120);

        assertDoesNotThrow(() -> controller.validateBeaconTemplate(payload));

        String finalMessage = controller.resolveAndValidateBeaconText(payload);
        assertEquals(124, finalMessage.length());
        assertEquals(payload + " *a*", finalMessage);

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateBeaconTemplate("x".repeat(121))
        );
    }

    @Test
    void acceptsTemporarilyUnresolvedVariableOnlyBeaconTemplate() {
        ChatPreferences preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));
        preferences.setMYQRGFirstCat("");

        ChatController controller = new ChatController();
        controller.setChatPreferences(preferences);

        assertDoesNotThrow(
                () -> controller.validateBeaconTemplate("MYQRG")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.resolveAndValidateBeaconText("MYQRG")
        );
    }

    @Test
    void rejectsEmptyOverlongAndProtocolBreakingBeaconText() {
        ChatPreferences preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));

        ChatController controller = new ChatController();
        controller.setChatPreferences(preferences);

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateBeaconTemplate("   ")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateBeaconTemplate(
                        "cq at 144.300|0|QUIT"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateBeaconTemplate(
                        "x".repeat(121)
                )
        );
    }
}
