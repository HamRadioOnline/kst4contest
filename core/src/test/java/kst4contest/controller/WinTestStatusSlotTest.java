package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A Win-Test STATUS of the second station writes SECONDQRG when SO2R is on. */
class WinTestStatusSlotTest {

    /* STATUS layout as parseStatus reads it: token 1 station, 5 mode, 7 frequency in 100 Hz. */
    private static final String STATUS_STN2 = "STATUS: \"STN2\" \"\" 0 12 0 0 4322000 0";
    private static final String STATUS_STN1 = "STATUS: \"STN1\" \"\" 0 12 0 0 1443000 0";

    private final ChatController controller = mock(ChatController.class);
    private final ChatPreferences prefs = mock(ChatPreferences.class);
    private ReadUDPByWintestThread thread(boolean so2r) {
        when(controller.getChatPreferences()).thenReturn(prefs);
        when(controller.uiDispatcher()).thenReturn(new DirectUiDispatcher());
        when(prefs.isLogsynch_wintestQrgSyncEnabled()).thenReturn(true);
        when(prefs.isTrxSynch_so2rEnabled()).thenReturn(so2r);
        when(prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1()).thenReturn("STN1");
        when(prefs.getLogsynch_wintestNetworkStationNameOfWintestClient2()).thenReturn("STN2");
        return new ReadUDPByWintestThread(controller, mock(ThreadStatusCallback.class));
    }

    @Test
    void withSo2rTheSecondStationWritesTheSecondQrg() {
        thread(true).parseStatus(STATUS_STN2);

        verify(controller).applyExternalQrg(RadioSlot.SECOND, "432.200.00", 432_200_000L);
    }

    @Test
    void withoutSo2rTheSecondStationIsFilteredOutAsBefore() {
        thread(false).parseStatus(STATUS_STN2);

        org.mockito.Mockito.verify(controller, org.mockito.Mockito.never())
                .applyExternalQrg(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void withoutSo2rTheFirstStationUpdatesOnlyTheMainSlotWithExactHz() {
        thread(false).parseStatus(STATUS_STN1);

        verify(controller).applyExternalQrg(RadioSlot.FIRST, "144.300.00", 144_300_000L);
    }
}
