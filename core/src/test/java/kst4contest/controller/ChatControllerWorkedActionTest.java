package kst4contest.controller;

import kst4contest.model.Band;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerWorkedActionTest {

    @Test
    void manualWorkedActionUsesContactInfoPersistenceAndUpdatesEveryActiveVariant()
            throws SQLException {
        DBController database = mock(DBController.class);
        when(database.updateWkdInfoOnChatMember(argThat(member -> member != null)))
                .thenReturn(true);
        ChatController controller = new ChatController();
        controller.setDbHandler(database);
        ChatMember selected = member("9A0BB-2", 2, "JN75AA");
        ChatMember secondCategory = member("9A0BB-70", 3, "JN75AA");
        controller.addOrUpdateActiveChatMember(selected);
        controller.addOrUpdateActiveChatMember(secondCategory);

        assertTrue(controller.markStationWorkedOnBand(selected, Band.B_432));

        assertTrue(selected.isWorked());
        assertTrue(selected.isWorked432());
        assertTrue(secondCategory.isWorked());
        assertTrue(secondCategory.isWorked432());
        assertTrue(controller.getWorkedGrossFieldCache()
                .isGrossFieldWorked(Band.B_432, "JN75"));
        verify(database).updateWkdInfoOnChatMember(argThat(member ->
                "9A0BB".equals(member.getCallSignRaw())
                        && member.isWorked()
                        && member.isWorked432()
                        && "JN75AA".equals(member.getQra())));
        verify(database).upsertWorkedGrossField(
                Band.B_432, "JN75AA", "9A0BB", "MANUAL");
    }

    @Test
    void manualWorkedActionRejectsMissingStationOrBandWithoutChangingState() {
        ChatController controller = new ChatController();
        ChatMember member = member("DL1ABC", 2, "JO50AA");

        assertFalse(controller.markStationWorkedOnBand(null, Band.B_144));
        assertFalse(controller.markStationWorkedOnBand(member, null));
        assertFalse(member.isWorked());
    }

    private static ChatMember member(String callSign, int category, String locator) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        member.setChatCategory(new ChatCategory(category));
        member.setQra(locator);
        return member;
    }
}
