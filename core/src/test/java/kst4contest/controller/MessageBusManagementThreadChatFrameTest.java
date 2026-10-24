package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import kst4contest.ApplicationConstants;
import kst4contest.controller.MessageBusManagementThread.DirectionalOpportunity;
import kst4contest.controller.MessageBusManagementThread.DxClusterSpotDecision;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.Band;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;
import kst4contest.observe.SimpleValue;
import kst4contest.utils.PlayAudioUtils;

/**
 * Characterises the live ({@code CH}) and history ({@code CR}) chat-frame branches
 * of {@link MessageBusManagementThread} by running real frames through a started
 * thread. The history branch must publish like the live one but never play sounds,
 * answer, tick or spot.
 *
 * <p>Frame layout, as split on {@code |} by the thread:
 * {@code <CH|CR>|category|epoch|senderCall|senderName|0|text|receiverCall|}; a
 * receiver of {@code 0} marks a public message.</p>
 */
class MessageBusManagementThreadChatFrameTest {

    private static final String MY_CALL = "DN9APW";
    private static final String REMOTE_CALL = "DL1ABC";
    private static final String OTHER_CALL = "DL2XYZ";
    private static final String FIRST_QRG = "144.300";

    // Local station between a western sender and an eastern receiver, all on one latitude.
    private static final String OWN_LOCATOR = "JO50LL";
    private static final String SENDER_LOCATOR = "JO30LL";
    private static final String RECEIVER_LOCATOR = "JO70LL";
    private static final double MAX_QRB_KM = 900.0;
    private static final double BEAM_WIDTH_DEG = 50.0;
    private static final long FIXED_NOW_MILLIS = 1_700_000_123_456L;

    @TempDir
    Path tempDir;

    private ChatController controller;
    private ChatPreferences prefs;
    private PlayAudioUtils playAudioUtils;
    private DXClusterThreadPooledServer dxClusterServer;
    private LinkedBlockingQueue<ChatMessage> txBus;
    private LinkedBlockingQueue<ChatMessage> rxQueue;
    private Map<String, ChatMember> activeMembers;

    @BeforeEach
    void setUp() {
        prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));
        prefs.setStn_loginCallSign(MY_CALL);
        prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(true);
        prefs.setMessageHandling_autoAnswerEnabled(false);
        prefs.setMYQRGFirstCat(FIRST_QRG);
        prefs.setMYQRGSecondCat("1296.200");

        ChatCategory mainCategory = new ChatCategory(ChatCategory.VUHF);
        ChatCategory secondCategory = new ChatCategory(ChatCategory.MICROWAVE);
        txBus = new LinkedBlockingQueue<>();
        rxQueue = new LinkedBlockingQueue<>();
        activeMembers = new HashMap<>();
        playAudioUtils = mock(PlayAudioUtils.class);
        dxClusterServer = mock(DXClusterThreadPooledServer.class);

        controller = mock(ChatController.class);
        when(controller.getChatPreferences()).thenReturn(prefs);
        when(controller.getMessageTXBus()).thenReturn(txBus);
        when(controller.getChatCategoryMain()).thenReturn(mainCategory);
        when(controller.getChatCategorySecondChat()).thenReturn(secondCategory);
        when(controller.getPlayAudioUtils()).thenReturn(playAudioUtils);
        when(controller.getStationMetricsService())
                .thenReturn(mock(StationMetricsService.class));
        when(controller.getScoreService()).thenReturn(mock(ScoreService.class));
        when(controller.getDxClusterServer()).thenReturn(dxClusterServer);
        DBController database = mock(DBController.class);
        when(controller.getDbHandler()).thenReturn(database);
        when(database.fetchChatMemberWkdDataForOnlyOneCallsignFromDB(any(ChatMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(controller.findActiveChatMember(any(ChatMember.class)))
                .thenAnswer(invocation -> activeMemberFor(invocation.getArgument(0)));
    }

    private void enableAllPrivateMessageSounds() {
        prefs.setNotify_playSimpleSounds(true);
        prefs.setNotify_playCWCallsignsOnRxedPMs(true);
        prefs.setNotify_playVoiceCallsignsOnRxedPMs(true);
    }

    // --- addressed to me ----------------------------------------------------

    @Test
    @Timeout(10)
    void liveMessageToMePlaysSoundsAndAnswersVersion() throws InterruptedException {
        enableAllPrivateMessageSounds();

        runThrough(chatFrame("CH", REMOTE_CALL, "//ver //bell", MY_CALL));

        verify(playAudioUtils).playNoiseLauncher('P');
        verify(playAudioUtils).playCWLauncher("  " + REMOTE_CALL);
        verify(playAudioUtils).playVoiceLauncher("?" + REMOTE_CALL);
        verify(playAudioUtils).playVoiceLauncher("!");
        assertEquals(1, txBus.size());
        assertEquals(
                "/CQ " + REMOTE_CALL + " " + ApplicationConstants.AUTOANSWER_PREFIX
                        + " KST4Contest v" + ApplicationConstants.APPLICATION_CURRENT_VERSION
                        + " by DO5AMF",
                txBus.peek().getMessageText());
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void historyMessageToMePublishesOnly() throws InterruptedException {
        enableAllPrivateMessageSounds();

        runThrough(chatFrame("CR", REMOTE_CALL, "//ver //bell ur qrg?", MY_CALL));

        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
        assertTrue(txBus.isEmpty());
        verifyNoInteractions(playAudioUtils);
    }

    @Test
    @Timeout(10)
    void genuinePrivateMessageCarriesTheAuthoritativeRouteFlags() throws InterruptedException {
        runThrough(chatFrame("CH", REMOTE_CALL, "pse ur qrg", MY_CALL));

        ChatMessage published = singlePublished();
        assertEquals(MY_CALL, published.getReceiver().getCallSign());
        assertTrue(published.isMessageDirectedToMe());
        assertFalse(published.isMessageDirectedToCommunity());
        assertEquals(ChatCategory.VUHF, published.getChatCategory().getCategoryNumber());
    }

    @Test
    @Timeout(10)
    void publicMessageWithLeadingAddressIsMarkedAsExplicitlyAddressedToMe()
            throws InterruptedException {
        runThrough(chatFrame("CH", REMOTE_CALL, "(" + MY_CALL + ") pse ur qrg", "0"));

        ChatMessage published = singlePublished();
        assertEquals("ALL", published.getReceiver().getCallSign());
        assertTrue(published.isMessageDirectedToCommunity());
        assertTrue(published.isMessageDirectedToMe());
    }

    @Test
    @Timeout(10)
    void arbitraryPublicMentionIsNotMarkedAsExplicitlyAddressedToMe()
            throws InterruptedException {
        runThrough(chatFrame("CH", REMOTE_CALL, "ask " + MY_CALL + " about the sked", "0"));

        ChatMessage published = singlePublished();
        assertTrue(published.isMessageDirectedToCommunity());
        assertFalse(published.isMessageDirectedToMe());
    }

    @Test
    @Timeout(10)
    void liveUserEntryCarriesTheInjectedJoinTimestampToTheController()
            throws InterruptedException {
        runThrough(message("UA5|2|HB9SJV|Contest team|JN47AA|3|"));

        ArgumentCaptor<ChatMember> member = ArgumentCaptor.forClass(ChatMember.class);
        verify(controller).addOrUpdateActiveChatMember(member.capture(),
                org.mockito.ArgumentMatchers.eq(FIXED_NOW_MILLIS));
        assertEquals("HB9SJV", member.getValue().getCallSign());
        assertEquals(ChatCategory.VUHF,
                member.getValue().getChatCategory().getCategoryNumber());
    }

    @Test
    @Timeout(10)
    void alternateLiveUserEntryCarriesTheInjectedJoinTimestampToTheController()
            throws InterruptedException {
        runThrough(message("UA2|3|OE5SOS|Microwave team|JN78AA|3|"));

        ArgumentCaptor<ChatMember> member = ArgumentCaptor.forClass(ChatMember.class);
        verify(controller).addOrUpdateActiveChatMember(member.capture(),
                org.mockito.ArgumentMatchers.eq(FIXED_NOW_MILLIS));
        assertEquals("OE5SOS", member.getValue().getCallSign());
        assertEquals(ChatCategory.MICROWAVE,
                member.getValue().getChatCategory().getCategoryNumber());
    }

    @Test
    @Timeout(10)
    void initialUserListAndItsTerminatorDoNotCreateLiveJoinEvents()
            throws InterruptedException {
        runThrough(
                message("UA0|2|HB9SJV|Contest team|JN47AA|3|"),
                message("UE|2|1|"));

        verify(controller, never()).addOrUpdateActiveChatMember(
                any(ChatMember.class), org.mockito.ArgumentMatchers.anyLong());
        verify(controller).stageInitialOn4KstChatMember(
                org.mockito.ArgumentMatchers.eq(7L), any(ChatMember.class));
        verify(controller).onOn4KstInitialUserListCompleted(
                org.mockito.ArgumentMatchers.eq(7L), any(ChatCategory.class));
    }

    @Test
    @Timeout(10)
    void ownCallIsExcludedFromLiveJoinEventsIgnoringCase()
            throws InterruptedException {
        runThrough(message("UA5|2|dn9apw|Own station|JO50LL|3|"));

        verify(controller, never()).addOrUpdateActiveChatMember(
                any(ChatMember.class), org.mockito.ArgumentMatchers.anyLong());
    }

    // --- sent by me ---------------------------------------------------------

    @Test
    @Timeout(10)
    void liveMessageFromMeIsMarkedWithTheReceiver() throws InterruptedException {
        runThrough(chatFrame("CH", MY_CALL.toLowerCase(), "hello", REMOTE_CALL));

        assertEquals("(>" + REMOTE_CALL + ")hello", singlePublished().getMessageText());
    }

    @Test
    @Timeout(10)
    void historyMessageFromMeIsMarkedWithTheReceiver() throws InterruptedException {
        runThrough(chatFrame("CR", MY_CALL.toLowerCase(), "hello", REMOTE_CALL));

        assertEquals("(>" + REMOTE_CALL + ")hello", singlePublished().getMessageText());
    }

    /**
     * Pinned: the receiver is resolved case-insensitively against the login call and
     * {@code ChatMember#setCallSign} upper-cases it, so a lower-case receiver in the
     * frame still counts as addressed to me. (Only a lower-case login call defeats
     * the case-sensitive TO_ME comparison, see the auto-answer characterisation.)
     */
    @Test
    @Timeout(10)
    void receiverInDifferentCaseIsStillTreatedAsMe() throws InterruptedException {
        runThrough(chatFrame("CH", REMOTE_CALL, "ur qrg?", MY_CALL.toLowerCase()));

        assertEquals(1, txBus.size());
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    // --- between two other stations ----------------------------------------

    @Test
    @Timeout(10)
    void liveMessageBetweenOthersInAngleTicksAndSpots() throws InterruptedException {
        ChatMember sender = prepareInAngleScene();
        prefs.setNotify_playSimpleSounds(true);
        prefs.setNotify_dxClusterServerEnabled(true);

        runThrough(chatFrame("CH", REMOTE_CALL, "hello", OTHER_CALL));

        verify(playAudioUtils).playNoiseLauncher('-');
        ArgumentCaptor<ChatMember> spot = ArgumentCaptor.forClass(ChatMember.class);
        verify(dxClusterServer, times(1)).broadcastSingleDXClusterEntryToLoggers(spot.capture());
        assertEquals(REMOTE_CALL, spot.getValue().getCallSign());
        assertEquals(FIRST_QRG, spot.getValue().getFrequency().get());
        assertTrue(sender.isInAngleAndRange());
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void historyMessageBetweenOthersInAngleOnlySetsTheFlag() throws InterruptedException {
        ChatMember sender = prepareInAngleScene();
        prefs.setNotify_playSimpleSounds(true);
        prefs.setNotify_dxClusterServerEnabled(true);

        runThrough(chatFrame("CR", REMOTE_CALL, "hello", OTHER_CALL));

        assertTrue(sender.isInAngleAndRange());
        verify(playAudioUtils, never()).playNoiseLauncher('-');
        verify(dxClusterServer, never()).broadcastSingleDXClusterEntryToLoggers(any());
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    // --- SERVER sender ------------------------------------------------------

    @Test
    @Timeout(10)
    void liveServerSenderGetsAirScoutInfo() throws InterruptedException {
        runThrough(chatFrame("CH", "SERVER", "welcome", "0"));

        ChatMessage live = singlePublished();
        assertEquals("SERVER", live.getSender().getCallSign());
        assertEquals("Sysop", live.getSender().getName());
        assertNotNull(live.getSender().getAirPlaneReflectInfo());
    }

    @Test
    @Timeout(10)
    void historyServerSenderGetsNoAirScoutInfo() throws InterruptedException {
        runThrough(chatFrame("CR", "SERVER", "welcome", "0"));

        ChatMessage history = singlePublished();
        assertEquals("SERVER", history.getSender().getCallSign());
        assertEquals("Sysop", history.getSender().getName());
        assertNull(history.getSender().getAirPlaneReflectInfo());
    }

    // --- frequency detection order -----------------------------------------

    @Test
    @Timeout(10)
    void liveDetectsFrequencyBeforePublishingHistoryAfter() throws InterruptedException {
        stubFrequencyDetection();

        List<Boolean> knownAtPublish = new ArrayList<>();
        List<ChatMember> senders = new ArrayList<>();
        doAnswer(invocation -> {
            ChatMessage published = invocation.getArgument(0);
            senders.add(published.getSender());
            knownAtPublish.add(hasFrequency(published.getSender()));
            return null;
        }).when(controller).publishChatMessage(any(ChatMessage.class));

        runThrough(chatFrame("CH", REMOTE_CALL, "qrv 144.300", "0"));
        assertEquals(List.of(true), knownAtPublish);

        knownAtPublish.clear();
        senders.clear();
        runThrough(chatFrame("CR", REMOTE_CALL, "qrv 144.300", "0"));
        assertEquals(List.of(false), knownAtPublish);
        assertTrue(hasFrequency(senders.get(0)), "known once the thread has finished");
    }

    @Test
    @Timeout(10)
    void liveFrequencyFromADirectedMessageIsAvailableForTheSpot() throws InterruptedException {
        stubFrequencyDetection();
        ChatMember sender = prepareInAngleSceneWithoutFrequency();
        prefs.setNotify_dxClusterServerEnabled(true);
        List<Boolean> knownAtSpot = new ArrayList<>();
        doAnswer(invocation -> {
            knownAtSpot.add(hasFrequency(invocation.<ChatMember>getArgument(0)));
            return null;
        }).when(dxClusterServer).broadcastSingleDXClusterEntryToLoggers(any());

        runThrough(chatFrame("CH", REMOTE_CALL, "qrv 144.300", OTHER_CALL));

        assertEquals(List.of(true), knownAtSpot);
        assertTrue(sender.isInAngleAndRange());
        verify(controller, times(1)).publishChatMessage(any(ChatMessage.class));
    }

    @Test
    @Timeout(10)
    void historyFrequencyFromADirectedMessageComesAfterPublishingWithoutSpot()
            throws InterruptedException {
        stubFrequencyDetection();
        ChatMember sender = prepareInAngleSceneWithoutFrequency();
        prefs.setNotify_dxClusterServerEnabled(true);
        List<Boolean> knownAtPublish = new ArrayList<>();
        doAnswer(invocation -> {
            knownAtPublish.add(hasFrequency(invocation.<ChatMessage>getArgument(0).getSender()));
            return null;
        }).when(controller).publishChatMessage(any(ChatMessage.class));

        runThrough(chatFrame("CR", REMOTE_CALL, "qrv 144.300", OTHER_CALL));

        assertEquals(List.of(false), knownAtPublish);
        assertTrue(hasFrequency(sender), "known once the thread has finished");
        assertTrue(sender.isInAngleAndRange());
        verify(dxClusterServer, never()).broadcastSingleDXClusterEntryToLoggers(any());
    }

    // ----------------------------------------------------------------------

    /** The controller is a mock: mimic what the real one does to the sender. */
    private void stubFrequencyDetection() {
        doAnswer(invocation -> {
            ChatMember sender = invocation.getArgument(0);
            sender.setFrequency(new SimpleValue<>(String.valueOf((double) invocation.getArgument(2))));
            return null;
        }).when(controller).applyDetectedFrequencyToActiveMembers(
                any(ChatMember.class), any(Band.class), anyDouble());
    }

    /** In-angle scene whose sender has no known frequency yet. */
    private ChatMember prepareInAngleSceneWithoutFrequency() {
        prefs.setStn_loginLocatorMainCat(OWN_LOCATOR);
        prefs.setStn_maxQRBDefault(MAX_QRB_KM);
        prefs.setStn_antennaBeamWidthDeg(BEAM_WIDTH_DEG);

        ChatMember sender = knownMember(REMOTE_CALL, SENDER_LOCATOR);
        knownMember(OTHER_CALL, RECEIVER_LOCATOR);

        assertEquals(DirectionalOpportunity.IN_ANGLE_AND_RANGE,
                MessageBusManagementThread.classifyDirectionalOpportunity(
                        OWN_LOCATOR, SENDER_LOCATOR, RECEIVER_LOCATOR, MAX_QRB_KM, BEAM_WIDTH_DEG));
        assertFalse(hasFrequency(sender));
        assertFalse(sender.isInAngleAndRange());
        return sender;
    }

    /**
     * Registers sender and receiver with locators that form an in-angle opportunity
     * and a spottable sender; the setup is checked with the pure functions first.
     */
    private ChatMember prepareInAngleScene() {
        prefs.setStn_loginLocatorMainCat(OWN_LOCATOR);
        prefs.setStn_maxQRBDefault(MAX_QRB_KM);
        prefs.setStn_antennaBeamWidthDeg(BEAM_WIDTH_DEG);

        ChatMember sender = knownMember(REMOTE_CALL, SENDER_LOCATOR);
        sender.setFrequency(new SimpleValue<>(FIRST_QRG));
        knownMember(OTHER_CALL, RECEIVER_LOCATOR);

        assertEquals(DirectionalOpportunity.IN_ANGLE_AND_RANGE,
                MessageBusManagementThread.classifyDirectionalOpportunity(
                        OWN_LOCATOR, SENDER_LOCATOR, RECEIVER_LOCATOR, MAX_QRB_KM, BEAM_WIDTH_DEG));
        assertEquals(DxClusterSpotDecision.SPOT,
                MessageBusManagementThread.decideDxClusterSpot(sender));
        assertFalse(sender.isInAngleAndRange());
        return sender;
    }

    private ChatMember knownMember(String callSign, String locator) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        member.setQra(locator);
        member.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
        activeMembers.put(callSign, member);
        return member;
    }

    private static boolean hasFrequency(ChatMember member) {
        return member.getFrequency() != null
                && member.getFrequency().get() != null
                && !member.getFrequency().get().isBlank();
    }

    private ChatMessage singlePublished() {
        ArgumentCaptor<ChatMessage> published = ArgumentCaptor.forClass(ChatMessage.class);
        verify(controller, times(1)).publishChatMessage(published.capture());
        return published.getValue();
    }

    private void runThrough(ChatMessage... frames) throws InterruptedException {
        MessageBusManagementThread thread = new MessageBusManagementThread(
                controller,
                mock(ThreadStatusCallback.class),
                7L,
                rxQueue,
                ignored -> true,
                () -> FIXED_NOW_MILLIS);
        for (ChatMessage frame : frames) {
            rxQueue.add(frame);
        }
        rxQueue.add(poisonPill());
        thread.start();
        thread.join();
    }

    private static ChatMessage chatFrame(
            String opcode, String senderCall, String text, String receiverCall) {
        return message(opcode + "|2|1700000000|" + senderCall + "|Hans|0|"
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

    /** Mimics the active-member lookup: registered stations as-is, others with a default locator. */
    private ChatMember activeMemberFor(ChatMember lookup) {
        ChatMember registered = activeMembers.get(lookup.getCallSign());
        if (registered != null) {
            return registered;
        }
        ChatMember member = new ChatMember();
        member.setCallSign(lookup.getCallSign());
        member.setChatCategory(lookup.getChatCategory());
        member.setQra("JO31AA");
        member.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
        return member;
    }
}
