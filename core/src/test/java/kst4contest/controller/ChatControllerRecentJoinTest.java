package kst4contest.controller;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.RecentChatJoin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatControllerRecentJoinTest {

    @Test
    void liveJoinIsRecordedOnceWithoutRenewingTheTimestampOnDuplicateUpdates() {
        ChatController controller = new ChatController();

        controller.addOrUpdateActiveChatMember(member("HB9SJV", 2), 1_000L);
        controller.addOrUpdateActiveChatMember(member("HB9SJV", 2), 9_000L);

        List<RecentChatJoin> joins = controller.getRecentChatJoins().snapshot();
        assertEquals(1, joins.size());
        assertEquals("HB9SJV", joins.get(0).getCallSign());
        assertEquals(2, joins.get(0).getCategoryNumber());
        assertEquals(1_000L, joins.get(0).getJoinedAtEpochMillis());
    }

    @Test
    void theSameCallsignInTwoCategoriesRemainsTwoJoinIdentities() {
        ChatController controller = new ChatController();

        controller.addOrUpdateActiveChatMember(member("DL1KDA", 2), 1_000L);
        controller.addOrUpdateActiveChatMember(member("DL1KDA", 3), 2_000L);

        List<RecentChatJoin> joins = controller.getRecentChatJoins().snapshot();
        assertEquals(2, joins.size());
        assertEquals(List.of(3, 2), joins.stream().map(RecentChatJoin::getCategoryNumber).toList());
    }

    @Test
    void leavingRemovesTheJoinAndARealRejoinStartsAFreshTimestamp() {
        ChatController controller = new ChatController();
        ChatMember member = member("OE5SOS", 2);

        controller.addOrUpdateActiveChatMember(member, 1_000L);
        controller.removeActiveChatMember(member);
        assertTrue(controller.getRecentChatJoins().isEmpty());

        controller.addOrUpdateActiveChatMember(member("OE5SOS", 2), 7_000L);

        assertEquals(7_000L,
                controller.getRecentChatJoins().snapshot().get(0).getJoinedAtEpochMillis());
    }

    @Test
    void clearingTheActiveRosterAlsoClearsTransientJoinEvents() {
        ChatController controller = new ChatController();
        controller.addOrUpdateActiveChatMember(member("PA2M", 2), 1_000L);

        controller.clearActiveChatMembers();

        assertTrue(controller.getRecentChatJoins().isEmpty());
    }

    private static ChatMember member(String callSign, int categoryNumber) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        member.setChatCategory(new ChatCategory(categoryNumber));
        return member;
    }
}
