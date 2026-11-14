package kst4contest.view;

import kst4contest.controller.RadioSlot;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.view.compose.ChatInputActions;

/**
 * What the Compose send line needs from the runtime.
 *
 * <p>Every method forwards to the same code the JavaFX send button runs, so that while the two
 * windows sit side by side a message sent from either takes exactly one path. The order the
 * steps are called in lives in {@code ChatInputState}, where it is tested against the
 * original; this class only answers questions.</p>
 */
public final class ComposeChatInputActions implements ChatInputActions {

    private final Kst4ContestApplication application;

    public ComposeChatInputActions(final Kst4ContestApplication application) {
        this.application = application;
    }

    @Override
    public ChatMember panelSelection() {
        return application.selectedCallSignInfoStageChatMember;
    }

    @Override
    public ChatMember tableSelection() {
        /*
         * The JavaFX station table is gone. Kept rather than dropped from the interface
         * because ChatInputState's three-step lookup is the documented selection order, and
         * a Compose station selection may fill this in again.
         */
        return null;
    }

    @Override
    public ChatMember scoreSelection() {
        if (application.chatcontroller == null
                || application.chatcontroller.getScoreService() == null) {
            return null;
        }
        return application.chatcontroller.getScoreService().getSelectedChatMember();
    }

    @Override
    public String resolveVariables(final String template, final ChatMember member) {
        if (application.messageVariableResolver == null) {
            return template;
        }
        return application.messageVariableResolver.resolveForSelectedStation(template, member);
    }

    @Override
    public boolean isAddressedToOwnCallsign(final String text) {
        return application.isMessageAddressedToOwnCallsignFromCompose(text);
    }

    @Override
    public ChatCategory resolveCategory(final String text, final ChatMember member) {
        return application.resolveOutgoingChatCategoryFromCompose(text, member);
    }

    @Override
    public ChatCategory mainCategory() {
        return application.chatcontroller.getChatCategoryMain();
    }

    @Override
    public void recordOutboundCq(final String text) {
        application.chatcontroller.getStationMetricsService()
                .tryRecordOutboundCq(text, System.currentTimeMillis());
    }

    @Override
    public void requestRecompute(final String reason) {
        if (application.chatcontroller.getScoreService() != null) {
            application.chatcontroller.getScoreService().requestRecompute(reason);
        }
    }

    @Override
    public void queue(final ChatMessage message) {
        application.chatcontroller.getMessageTXBus().add(message);
    }

    @Override
    public String ownQrgMain() {
        String value = application.chatcontroller.getChatPreferences().getMYQRGFirstCat().get();
        return value == null ? "" : value;
    }

    @Override
    public String ownQrgSecond() {
        String value = application.chatcontroller.getChatPreferences().getMYQRGSecondCat().get();
        return value == null ? "" : value;
    }

    @Override
    public void applyManualQrg(final RadioSlot slot, final String value) {
        application.chatcontroller.applyManualQrg(slot, value);
    }
}
