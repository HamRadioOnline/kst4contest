package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import kst4contest.ApplicationConstants;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;
import kst4contest.utils.PlayAudioUtils;

/**
 * Characterises the automatic reply to private ON4KST chat messages by running
 * real {@code CH} frames through a started {@link MessageBusManagementThread}.
 *
 * <p>The thread reads from its receive queue until it takes the poison pill, so
 * every test enqueues its frames followed by the pill and joins the thread. The
 * transmit bus is a real queue: an automatic reply is observable as exactly one
 * element on it.</p>
 *
 * <p>Frame layout, as split on {@code |} by the thread:
 * {@code CH|category|epoch|senderCall|senderName|0|text|receiverCall|}; a
 * receiver of {@code 0} marks a public message.</p>
 */
class MessageBusManagementThreadAutoAnswerTest {

    private static final String MY_CALL = "DN9APW";
    private static final String REMOTE_CALL = "DL1ABC";
    private static final String FIRST_QRG = "144.300";
    private static final String SECOND_QRG = "1296.200";

    @TempDir
    Path tempDir;

    private ChatController controller;
    private ChatPreferences prefs;
    private LinkedBlockingQueue<ChatMessage> txBus;
    private LinkedBlockingQueue<ChatMessage> rxQueue;
    private ChatCategory mainCategory;
    private ChatCategory secondCategory;

    @BeforeEach
    void setUp() {
        prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));
        prefs.setStn_loginCallSign(MY_CALL);
        prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(true);
        prefs.setMessageHandling_autoAnswerEnabled(false);
        prefs.setMYQRGFirstCat(FIRST_QRG);
        prefs.setMYQRGSecondCat(SECOND_QRG);

        mainCategory = new ChatCategory(ChatCategory.VUHF);
        secondCategory = new ChatCategory(ChatCategory.MICROWAVE);
        txBus = new LinkedBlockingQueue<>();
        rxQueue = new LinkedBlockingQueue<>();

        controller = mock(ChatController.class);
        when(controller.getChatPreferences()).thenReturn(prefs);
        when(controller.getMessageTXBus()).thenReturn(txBus);
        when(controller.getChatCategoryMain()).thenReturn(mainCategory);
        when(controller.getChatCategorySecondChat()).thenReturn(secondCategory);
        when(controller.getPlayAudioUtils()).thenReturn(mock(PlayAudioUtils.class));
        when(controller.getStationMetricsService())
                .thenReturn(mock(StationMetricsService.class));
        when(controller.getScoreService()).thenReturn(mock(ScoreService.class));
        when(controller.getDxClusterServer())
                .thenReturn(mock(DXClusterThreadPooledServer.class));
        when(controller.findActiveChatMember(any(ChatMember.class)))
                .thenAnswer(invocation -> activeMemberFor(invocation.getArgument(0)));
    }

    @Test
    @Timeout(10)
    void answersQrgQuestionAddressedToMeExactlyOnce() throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL));

        List<ChatMessage> replies = new ArrayList<>(txBus);
        assertEquals(1, replies.size(), "exactly one automatic reply expected");

        ChatMessage reply = replies.get(0);
        assertEquals(
                "/CQ " + REMOTE_CALL + " " + ApplicationConstants.AUTOANSWER_PREFIX
                        + " QRG is: " + FIRST_QRG + " *a*",
                reply.getMessageText());
        assertEquals(MY_CALL, reply.getSender().getCallSign());
        assertEquals(REMOTE_CALL, reply.getReceiver().getCallSign());
        assertSame(mainCategory, reply.getChatCategory());

        // The incoming private message itself is still published to the UI.
        ArgumentCaptor<ChatMessage> published = ArgumentCaptor.forClass(ChatMessage.class);
        verify(controller, times(1)).publishChatMessage(published.capture());
        assertEquals("ur qrg?", published.getValue().getMessageText());
        assertEquals(REMOTE_CALL, published.getValue().getSender().getCallSign());
    }

    @Test
    @Timeout(10)
    void matchesQuestionTextCaseInsensitivelyInsideLongerMessage()
            throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL, "GM Hans, PSE QRG for a sked", MY_CALL));

        assertEquals(1, txBus.size());
    }

    @Test
    @Timeout(10)
    void secondQuestionFromSameStationWithinCooldownIsNotAnswered()
            throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL),
                chatFrame(2, REMOTE_CALL, "qrg?", MY_CALL));

        assertEquals(1, txBus.size(), "cooldown must suppress the second reply");
        verify(controller, times(2)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void cooldownIsKeyedByStationAndCategory() throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL),
                chatFrame(2, "DL2XYZ", "ur qrg?", MY_CALL),
                chatFrame(3, REMOTE_CALL, "ur qrg?", MY_CALL));

        List<ChatMessage> replies = new ArrayList<>(txBus);
        assertEquals(3, replies.size(),
                "another station and another category each get their own reply");
        assertEquals(
                "/CQ " + REMOTE_CALL + " " + ApplicationConstants.AUTOANSWER_PREFIX
                        + " QRG is: " + SECOND_QRG + " *a*",
                replies.get(2).getMessageText(),
                "the second-category question is answered with the second QRG");
        assertSame(secondCategory, replies.get(2).getChatCategory());
    }

    @Test
    @Timeout(10)
    void questionCarryingTheAutoAnswerMarkerIsNeverAnswered()
            throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL,
                        ApplicationConstants.AUTOANSWER_PREFIX + " QRG is: 432.200 ur qrg?",
                        MY_CALL));

        assertTrue(txBus.isEmpty(), "automatic messages must not be answered");
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void publicQuestionIsNotAnswered() throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", "0"));

        assertTrue(txBus.isEmpty());
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void questionToAnotherStationIsNotAnswered() throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", "DL2XYZ"));

        assertTrue(txBus.isEmpty());
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void disabledQrgAutoAnswerStaysSilent() throws InterruptedException {
        prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(false);

        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL));

        assertTrue(txBus.isEmpty());
    }

    @Test
    @Timeout(10)
    void emptyQrgIsNotAnsweredAndDoesNotConsumeTheCooldown()
            throws InterruptedException {
        prefs.setMYQRGFirstCat("");

        MessageBusManagementThread thread = newThread();
        CountDownLatch markerProcessed = new CountDownLatch(1);
        doAnswer(invocation -> {
            markerProcessed.countDown();
            return null;
        }).when(controller).onOn4KstInitialUserListCompleted(anyLong(), any());

        thread.start();
        rxQueue.add(chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL));
        // The thread handles frames strictly in order: once the marker frame has
        // been processed, the question above has been decided without a QRG.
        rxQueue.add(message("UE|2|22562|"));
        assertTrue(markerProcessed.await(5, TimeUnit.SECONDS));
        assertTrue(txBus.isEmpty(), "no reply without a QRG");

        prefs.setMYQRGFirstCat(FIRST_QRG);
        rxQueue.add(chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL));
        rxQueue.add(poisonPill());
        thread.join();

        assertEquals(1, txBus.size(),
                "the rejected reply must not have started the cooldown");
        assertEquals(
                "/CQ " + REMOTE_CALL + " " + ApplicationConstants.AUTOANSWER_PREFIX
                        + " QRG is: " + FIRST_QRG + " *a*",
                txBus.peek().getMessageText());
    }

    @Test
    @Timeout(10)
    void genericAutoAnswerRepliesToOtherPrivateMessages() throws InterruptedException {
        prefs.setMessageHandling_autoAnswerEnabled(true);
        prefs.setMessageHandling_autoAnswerTextMainCat("  not qrv, back at 18z  ");

        runThrough(
                chatFrame(2, REMOTE_CALL, "hi, sked on 144?", MY_CALL));

        assertEquals(1, txBus.size());
        assertEquals(
                "/CQ " + REMOTE_CALL + " " + ApplicationConstants.AUTOANSWER_PREFIX
                        + " not qrv, back at 18z *a*",
                txBus.peek().getMessageText(),
                "the configured text is trimmed");
    }

    @Test
    @Timeout(10)
    void qrgQuestionWinsOverGenericAnswer() throws InterruptedException {
        prefs.setMessageHandling_autoAnswerEnabled(true);
        prefs.setMessageHandling_autoAnswerTextMainCat("not qrv");

        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL));

        assertEquals(1, txBus.size());
        assertEquals(
                "/CQ " + REMOTE_CALL + " " + ApplicationConstants.AUTOANSWER_PREFIX
                        + " QRG is: " + FIRST_QRG + " *a*",
                txBus.peek().getMessageText());
    }

    @Test
    @Timeout(10)
    void genericAnswerWithBlankTextIsRejectedWithoutConsumingCooldown()
            throws InterruptedException {
        prefs.setMessageHandling_autoAnswerEnabled(true);
        prefs.setMessageHandling_autoAnswerTextMainCat("   ");

        runThrough(
                chatFrame(2, REMOTE_CALL, "hi", MY_CALL),
                chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL));

        assertEquals(1, txBus.size(),
                "the blank generic reply is dropped; the QRG reply still goes out");
        assertTrue(txBus.peek().getMessageText().endsWith("QRG is: " + FIRST_QRG + " *a*"));
    }

    @Test
    @Timeout(10)
    void versionRequestIsAnsweredOutsideTheCooldown() throws InterruptedException {
        runThrough(
                chatFrame(2, REMOTE_CALL, "//ver", MY_CALL));

        assertEquals(1, txBus.size());
        assertEquals(
                "/CQ " + REMOTE_CALL + " " + ApplicationConstants.AUTOANSWER_PREFIX
                        + " KST4Contest v" + ApplicationConstants.APPLICATION_CURRENT_VERSION
                        + " by DO5AMF *a*",
                txBus.peek().getMessageText());
    }

    /**
     * ODDITY (pinned, not fixed): the receiver callsign of a CH frame is upper-cased
     * by {@link ChatMember#setCallSign(String)}, but it is compared with
     * {@code equals} against the login callsign exactly as configured. With a
     * lower-case login callsign a private message is therefore not recognised as
     * addressed to the local station, and no automatic reply is sent. The
     * "sent by me" branch directly below compares both sides upper-cased.
     */
    @Test
    @Timeout(10)
    void lowerCaseLoginCallSignDefeatsTheToMeDetection() throws InterruptedException {
        prefs.setStn_loginCallSign(MY_CALL.toLowerCase());

        runThrough(
                chatFrame(2, REMOTE_CALL, "ur qrg?", MY_CALL));

        assertTrue(txBus.isEmpty());
        verify(controller, never()).getPlayAudioUtils();
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void autoMarkerInMyOwnEchoIsNotAnswered() throws InterruptedException {
        // The server echoes our own directed message; the sender equals the login.
        runThrough(
                chatFrame(2, MY_CALL, "ur qrg?", REMOTE_CALL));

        assertTrue(txBus.isEmpty());
        ArgumentCaptor<ChatMessage> published = ArgumentCaptor.forClass(ChatMessage.class);
        verify(controller, times(1)).publishChatMessage(published.capture());
        assertEquals("(>" + REMOTE_CALL + ")ur qrg?", published.getValue().getMessageText());
    }

    // ----------------------------------------------------------------------

    private void runThrough(ChatMessage... frames) throws InterruptedException {
        MessageBusManagementThread thread = newThread();
        for (ChatMessage frame : frames) {
            rxQueue.add(frame);
        }
        rxQueue.add(poisonPill());
        thread.start();
        thread.join();
    }

    private MessageBusManagementThread newThread() {
        return new MessageBusManagementThread(
                controller,
                mock(ThreadStatusCallback.class),
                7L,
                rxQueue,
                ignored -> true);
    }

    private static ChatMessage chatFrame(
            int category, String senderCall, String text, String receiverCall) {
        return message("CH|" + category + "|1700000000|" + senderCall + "|Hans|0|"
                + text + "|" + receiverCall + "|");
    }

    private static ChatMessage message(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageText(text);
        return message;
    }

    private static ChatMessage poisonPill() {
        ChatMessage message = message(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        message.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        return message;
    }

    /** Mimics the active-member lookup: every remote station is known with a locator. */
    private static ChatMember activeMemberFor(ChatMember lookup) {
        ChatMember member = new ChatMember();
        member.setCallSign(lookup.getCallSign());
        member.setChatCategory(lookup.getChatCategory());
        member.setQra("JO31AA");
        member.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
        return member;
    }
}
