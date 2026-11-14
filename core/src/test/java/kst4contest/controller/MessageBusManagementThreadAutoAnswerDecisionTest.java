package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;

/**
 * Unit tests for the pure autoanswer decisions extracted from the private-message
 * branch of {@link MessageBusManagementThread}. No mocks, no thread, no preferences.
 */
class MessageBusManagementThreadAutoAnswerDecisionTest {

    private static final String MARKER = ApplicationConstants.AUTOANSWER_PREFIX;
    private static final ChatCategory VUHF = new ChatCategory(ChatCategory.VUHF);
    private static final ChatCategory MICROWAVE = new ChatCategory(ChatCategory.MICROWAVE);

    // --- matchesQrgQuestion -------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "ur qrg?",
            "UR QRG?",
            "hi, your qrg? 73",
            "qrg?",
            "freq?",
            "GM Hans, PSE QRG for a sked"
    })
    void recognisesEveryConfiguredQuestionCaseInsensitively(String text) {
        assertTrue(MessageBusManagementThread.matchesQrgQuestion(
                text, MessageBusManagementThread.QRG_QUESTION_TEXTS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "my qrg is 144.300",
            "qrg",
            "what is your frequency",
            "pse  qrg",
            ""
    })
    void ignoresTextsWithoutAConfiguredQuestion(String text) {
        assertFalse(MessageBusManagementThread.matchesQrgQuestion(
                text, MessageBusManagementThread.QRG_QUESTION_TEXTS));
    }

    @Test
    void matchesQrgQuestionHandlesNullInputs() {
        assertFalse(MessageBusManagementThread.matchesQrgQuestion(
                null, MessageBusManagementThread.QRG_QUESTION_TEXTS));
        assertFalse(MessageBusManagementThread.matchesQrgQuestion("ur qrg?", null));
        assertFalse(MessageBusManagementThread.matchesQrgQuestion("ur qrg?", Set.of()));
    }

    @Test
    void questionTextsAreTheEstablishedFive() {
        assertEquals(
                Set.of("ur qrg?", "your qrg?", "qrg?", "freq?", "pse qrg"),
                MessageBusManagementThread.QRG_QUESTION_TEXTS);
    }

    // --- isAutoMessage ------------------------------------------------------

    @Test
    void detectsTheMarkerAnywhereInTheText() {
        assertTrue(MessageBusManagementThread.isAutoMessage(MARKER));
        assertTrue(MessageBusManagementThread.isAutoMessage("/CQ DL1ABC " + MARKER + " QRG is: 144.300"));
        assertTrue(MessageBusManagementThread.isAutoMessage("ur qrg? " + MARKER));
    }

    @Test
    void plainTextsAreNotAutoMessages() {
        assertFalse(MessageBusManagementThread.isAutoMessage(null));
        assertFalse(MessageBusManagementThread.isAutoMessage(""));
        assertFalse(MessageBusManagementThread.isAutoMessage("ur qrg?"));
        // The marker is matched literally, not case-insensitively.
        assertFalse(MessageBusManagementThread.isAutoMessage(MARKER.toLowerCase()));
    }

    // --- selectAutoAnswerQrg ------------------------------------------------

    @Test
    void secondCategoryGetsTheSecondQrg() {
        assertEquals("1296.200", MessageBusManagementThread.selectAutoAnswerQrg(
                MICROWAVE, MICROWAVE, "144.300", "1296.200"));
        // Categories are compared by number, not by identity.
        assertEquals("1296.200", MessageBusManagementThread.selectAutoAnswerQrg(
                new ChatCategory(ChatCategory.MICROWAVE), MICROWAVE, "144.300", "1296.200"));
    }

    @Test
    void everyOtherCategoryFallsBackToTheFirstQrg() {
        assertEquals("144.300", MessageBusManagementThread.selectAutoAnswerQrg(
                VUHF, MICROWAVE, "144.300", "1296.200"));
        assertEquals("144.300", MessageBusManagementThread.selectAutoAnswerQrg(
                null, MICROWAVE, "144.300", "1296.200"));
        assertEquals("144.300", MessageBusManagementThread.selectAutoAnswerQrg(
                MICROWAVE, null, "144.300", "1296.200"));
        assertNull(MessageBusManagementThread.selectAutoAnswerQrg(
                VUHF, MICROWAVE, null, "1296.200"));
    }

    // --- buildAutoAnswerMessageText ----------------------------------------

    @Test
    void buildsTheQrgReply() {
        assertEquals(
                "/CQ DL1ABC " + MARKER + " QRG is: 144.300 *a*",
                MessageBusManagementThread.buildAutoAnswerMessageText(
                        "DL1ABC", VUHF, true, " 144.300 ", false, null));
    }

    @Test
    void qrgRequestWinsOverTheGenericReply() {
        assertEquals(
                "/CQ DL1ABC " + MARKER + " QRG is: 144.300 *a*",
                MessageBusManagementThread.buildAutoAnswerMessageText(
                        "DL1ABC", VUHF, true, "144.300", true, "not qrv"));
    }

    @Test
    void qrgRequestWithoutQrgYieldsNoReply() {
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, true, null, true, "not qrv"));
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, true, "   ", true, "not qrv"));
        // The category is used for diagnostics only; a null category must not fail.
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", null, true, "", false, null));
    }

    @Test
    void buildsTheGenericReplyTrimmed() {
        assertEquals(
                "/CQ DL1ABC " + MARKER + " not qrv, back at 18z *a*",
                MessageBusManagementThread.buildAutoAnswerMessageText(
                        "DL1ABC", VUHF, false, "144.300", true, "  not qrv, back at 18z  "));
    }

    @Test
    void genericReplyNeedsTextAndTheSwitch() {
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, false, "144.300", true, null));
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, false, "144.300", true, "  "));
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, false, "144.300", false, "not qrv"));
    }

    @Test
    void missingSenderCallSignYieldsNoReply() {
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                null, VUHF, true, "144.300", true, "not qrv"));
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "  ", VUHF, true, "144.300", true, "not qrv"));
    }

    @Test
    void senderCallSignIsTrimmedButNotUpperCased() {
        assertEquals(
                "/CQ dl1abc " + MARKER + " QRG is: 144.300 *a*",
                MessageBusManagementThread.buildAutoAnswerMessageText(
                        " dl1abc ", VUHF, true, "144.300", false, null));
    }

    @Test
    void replyContainingTheFieldSeparatorIsRejected() {
        // On4KstProtocol.messageText refuses '|'; the reply is dropped, not repaired.
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, true, "144.300|432.200", false, null));
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, false, null, true, "not qrv | back later"));
    }

    @Test
    void replyContainingALineBreakIsRejected() {
        assertNull(MessageBusManagementThread.buildAutoAnswerMessageText(
                "DL1ABC", VUHF, false, null, true, "not qrv\nback later"));
    }

    // --- autoAnswerCooldownKey ---------------------------------------------

    @Test
    void cooldownKeyIsUpperCaseCallAndMessageCategory() {
        ChatMessage incoming = incoming("dl1abc", VUHF, MICROWAVE);

        assertEquals("DL1ABC|2", MessageBusManagementThread.autoAnswerCooldownKey(incoming));
    }

    @Test
    void cooldownKeyFallsBackToTheSenderCategory() {
        ChatMessage incoming = incoming("DL1ABC", null, MICROWAVE);

        assertEquals("DL1ABC|3", MessageBusManagementThread.autoAnswerCooldownKey(incoming));
    }

    @Test
    void cooldownKeyKeepsSuffixVariantsApart() {
        // Cooldown identity must not collapse DL1ABC and DL1ABC-2 (AGENTS.md).
        assertEquals("DL1ABC-2|2", MessageBusManagementThread.autoAnswerCooldownKey(
                incoming("DL1ABC-2", VUHF, null)));
        assertEquals("DL1ABC/P|2", MessageBusManagementThread.autoAnswerCooldownKey(
                incoming("DL1ABC/P", VUHF, null)));
    }

    @Test
    void cooldownKeyHasSafeDefaults() {
        assertEquals("UNKNOWN|0", MessageBusManagementThread.autoAnswerCooldownKey(null));
        assertEquals("UNKNOWN|0", MessageBusManagementThread.autoAnswerCooldownKey(new ChatMessage()));

        ChatMessage withoutCall = new ChatMessage();
        withoutCall.setSender(new ChatMember());
        withoutCall.setChatCategory(VUHF);
        assertEquals("UNKNOWN|2", MessageBusManagementThread.autoAnswerCooldownKey(withoutCall));
    }

    // --- isCooldownElapsed -------------------------------------------------

    @Test
    void cooldownIsElapsedWithoutAnEarlierReply() {
        assertTrue(MessageBusManagementThread.isCooldownElapsed(null, 0L));
    }

    @Test
    void cooldownBoundaryIsInclusive() {
        long last = 1_000_000L;
        long cooldown = MessageBusManagementThread.AUTOANSWER_COOLDOWN_MS;

        assertEquals(120_000L, cooldown);
        assertFalse(MessageBusManagementThread.isCooldownElapsed(last, last));
        assertFalse(MessageBusManagementThread.isCooldownElapsed(last, last + cooldown - 1));
        assertTrue(MessageBusManagementThread.isCooldownElapsed(last, last + cooldown));
        assertTrue(MessageBusManagementThread.isCooldownElapsed(last, last + cooldown + 1));
    }

    /**
     * ODDITY (pinned, not fixed): a clock that runs backwards, for example after an
     * NTP correction, does not re-open the cooldown; the difference is simply
     * negative and therefore below the threshold until the wall clock catches up.
     */
    @Test
    void clockRunningBackwardsKeepsTheCooldownClosed() {
        assertFalse(MessageBusManagementThread.isCooldownElapsed(1_000_000L, 500_000L));
    }

    // -----------------------------------------------------------------------

    private static ChatMessage incoming(
            String senderCall, ChatCategory messageCategory, ChatCategory senderCategory) {
        ChatMember sender = new ChatMember();
        sender.setCallSign(senderCall);
        sender.setChatCategory(senderCategory);

        ChatMessage incoming = new ChatMessage();
        incoming.setSender(sender);
        incoming.setChatCategory(messageCategory);
        return incoming;
    }
}
