package kst4contest.controller;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatControllerNotQrvPersistenceTest {

    @Test
    void propagatedNotQrvStateIsPersistedForTheSharedRawCall() throws SQLException {
        DBController database = mock(DBController.class);
        ChatController controller = new ChatController();
        controller.setDbHandler(database);
        ChatMember selected = member("9A0BB-2", 2);
        ChatMember second = member("9A0BB-70", 3);
        controller.addOrUpdateActiveChatMember(selected);
        controller.addOrUpdateActiveChatMember(second);
        selected.setQrv432(false);

        controller.propagateNotQrvStateToActiveMembers(selected);

        assertFalse(second.isQrv432());
        verify(database).updateNotQRVInfoOnChatMember(argThat(member ->
                "9A0BB".equals(member.getCallSignRaw()) && !member.isQrv432()));
    }

    private static ChatMember member(String callSign, int category) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        member.setChatCategory(new ChatCategory(category));
        return member;
    }
}
