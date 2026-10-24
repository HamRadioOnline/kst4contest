package kst4contest.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kst4contest.ApplicationConstants;
import kst4contest.controller.ChatController;
import kst4contest.controller.MessageBusManagementThread;
import kst4contest.controller.ScoreService;
import kst4contest.controller.StationMetricsService;
import kst4contest.controller.ThreadStatusCallback;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleRoster;
import kst4contest.observe.SimpleValue;
import kst4contest.view.compose.DataTableState;
import kst4contest.view.compose.RowKeys;
import kst4contest.view.compose.SelectedMessageFilter;
import kst4contest.view.compose.SelectedStationMessagePredicate;
import kst4contest.view.feed.SelectedStationMessagesFeed;

/** Exercises protocol parsing, selected-station filtering and the displayed table together. */
class SelectedStationMessageFlowTest {

    private static final String OWN_CALL = "DL0OWN";
    private static final String SELECTED_CALL = "DM5M";

    private final LinkedBlockingQueue<ChatMessage> receiveQueue = new LinkedBlockingQueue<>();
    private final SimpleRoster<ChatMessage> messages = new SimpleRoster<>();
    private final AtomicReference<Predicate<ChatMessage>> filter = new AtomicReference<>();

    private ChatController controller;
    private ChatPreferences preferences;
    private ChatMember selected;
    private RosterListBinding<ChatMessage> binding;
    private DataTableState<ChatMessage> displayed;

    @BeforeEach
    void setUp() {
        preferences = mock(ChatPreferences.class);
        when(preferences.getStn_loginCallSign()).thenReturn(OWN_CALL);
        when(preferences.getStn_loginLocatorMainCat()).thenReturn("JO50LL");
        when(preferences.getMYQRGFirstCat()).thenReturn(new SimpleValue<>("144.300"));
        when(preferences.getMYQRGSecondCat()).thenReturn(new SimpleValue<>("1296.200"));

        controller = mock(ChatController.class);
        when(controller.getChatPreferences()).thenReturn(preferences);
        when(controller.getChatCategoryMain()).thenReturn(new ChatCategory(ChatCategory.VUHF));
        when(controller.getChatCategorySecondChat()).thenReturn(new ChatCategory(ChatCategory.MICROWAVE));
        when(controller.getStationMetricsService()).thenReturn(mock(StationMetricsService.class));
        when(controller.getScoreService()).thenReturn(mock(ScoreService.class));
        when(controller.findActiveChatMember(any(ChatMember.class)))
                .thenAnswer(invocation -> activeMember(invocation.getArgument(0)));
        doAnswer(invocation -> {
            messages.add(invocation.getArgument(0));
            return null;
        }).when(controller).publishChatMessage(any(ChatMessage.class));

        selected = member(SELECTED_CALL, ChatCategory.VUHF);
        filter.set(SelectedStationMessagePredicate.forSelection(
                SelectedMessageFilter.PM_TO_ME, selected, OWN_CALL));

        displayed = new DataTableState<>(List.of(), RowKeys.INSTANCE.byReference(), "selected-flow-test");
        SelectedStationMessagesFeed feed =
                new SelectedStationMessagesFeed(displayed, new DirectUiDispatcher());
        binding = RosterListBinding.derived(
                new DirectUiDispatcher(),
                () -> messages.snapshot().stream().filter(filter.get()).toList(),
                messages);
        binding.onChanged(feed::push);
        feed.push(binding.list());
    }

    @Test
    @Timeout(10)
    void realisticFramesReachTheDisplayedRowsUnderAllFourModes() throws InterruptedException {
        runThrough(
                chatFrame(2, SELECTED_CALL, "private", OWN_CALL),
                chatFrame(2, SELECTED_CALL, "(" + OWN_CALL + ") public reply", "0"),
                chatFrame(2, SELECTED_CALL, "general public message", "0"),
                chatFrame(2, SELECTED_CALL, "private to another station", "DL2OTHER"),
                chatFrame(2, "DL2OTHER", "other station", "0"),
                chatFrame(3, SELECTED_CALL, "same call, other category", "0"));

        assertEquals(2, displayed.getRows().size(),
                "pm to me must contain the genuine PM and the explicitly addressed public line");

        selectMode(SelectedMessageFilter.PUBLIC);
        assertEquals(2, displayed.getRows().size(),
                "the two public lines overlap with the addressed-public PM view");

        selectMode(SelectedMessageFilter.PM_TO_OTHER);
        assertEquals(1, displayed.getRows().size(),
                "pm to other keeps private traffic sent by the selected station to another station");

        selectMode(SelectedMessageFilter.NOTHING);
        assertEquals(4, displayed.getRows().size(),
                "nothing removes only the type/receiver restriction");

        selectStation(member("DL2OTHER", ChatCategory.VUHF));
        assertEquals(2, displayed.getRows().size(),
                "changing the selected station must replace the displayed station scope immediately");
        selectStation(member(SELECTED_CALL, ChatCategory.VUHF));

        runThrough(chatFrame(2, SELECTED_CALL, "new while the panel is open", OWN_CALL));
        assertEquals(5, displayed.getRows().size(),
                "a new matching line must reach the already open table");
    }

    private void selectMode(SelectedMessageFilter mode) {
        filter.set(SelectedStationMessagePredicate.forSelection(mode, selected, OWN_CALL));
        binding.refresh();
    }

    private void selectStation(ChatMember station) {
        selected = station;
        selectMode(SelectedMessageFilter.NOTHING);
    }

    private void runThrough(ChatMessage... frames) throws InterruptedException {
        MessageBusManagementThread thread = new MessageBusManagementThread(
                controller,
                mock(ThreadStatusCallback.class),
                11L,
                receiveQueue,
                ignored -> true);
        for (ChatMessage frame : frames) {
            receiveQueue.add(frame);
        }
        receiveQueue.add(poisonPill());
        thread.start();
        thread.join();
    }

    private static ChatMessage chatFrame(int category, String sender, String text, String receiver) {
        return wireMessage("CH|" + category + "|1700000000|" + sender
                + "|Operator|0|" + text + "|" + receiver + "|");
    }

    private static ChatMessage poisonPill() {
        ChatMessage message = wireMessage(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        message.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
        return message;
    }

    private static ChatMessage wireMessage(String text) {
        ChatMessage message = new ChatMessage();
        message.setMessageText(text);
        return message;
    }

    private static ChatMember activeMember(ChatMember lookup) {
        return member(lookup.getCallSign(), lookup.getChatCategory().getCategoryNumber());
    }

    private static ChatMember member(String callSign, int category) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        member.setChatCategory(new ChatCategory(category));
        member.setQra("JO31AA");
        member.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
        return member;
    }
}
