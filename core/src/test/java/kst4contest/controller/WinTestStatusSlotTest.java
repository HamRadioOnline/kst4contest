package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A Win-Test STATUS of the second station writes SECONDQRG when SO2R is on. */
class WinTestStatusSlotTest {

    /* STATUS layout as parseStatus reads it: token 1 station, 5 mode, 7 frequency in 100 Hz. */
    private static final String STATUS_STN2 = "STATUS: \"STN2\" \"\" 0 12 0 0 4322000 0";

    private final ChatController controller = mock(ChatController.class);
    private final ChatPreferences prefs = mock(ChatPreferences.class);
    private final List<String> first = new ArrayList<>();
    private final List<String> second = new ArrayList<>();

    private static SimpleValue<String> recording(List<String> writes) {
        SimpleValue<String> value = new SimpleValue<>("");
        value.addListener(writes::add);
        return value;
    }

    private ReadUDPByWintestThread thread(boolean so2r) {
        when(controller.getChatPreferences()).thenReturn(prefs);
        when(controller.uiDispatcher()).thenReturn(new DirectUiDispatcher());
        when(prefs.isLogsynch_wintestQrgSyncEnabled()).thenReturn(true);
        when(prefs.isTrxSynch_so2rEnabled()).thenReturn(so2r);
        when(prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1()).thenReturn("STN1");
        when(prefs.getLogsynch_wintestNetworkStationNameOfWintestClient2()).thenReturn("STN2");
        when(prefs.getMYQRGFirstCat()).thenReturn(recording(first));
        when(prefs.getMYQRGSecondCat()).thenReturn(recording(second));
        return new ReadUDPByWintestThread(controller, mock(ThreadStatusCallback.class));
    }

    @Test
    void withSo2rTheSecondStationWritesTheSecondQrg() {
        thread(true).parseStatus(STATUS_STN2);

        assertEquals(1, second.size());
        assertTrue(first.isEmpty());
    }

    /** STATUS counts in 100 Hz; the beacon guard of issue #171 is told in Hz. */
    @Test
    void aStatusReportsTheTransceiverFrequencyOfItsRadioToTheBeaconGuard() {
        thread(true).parseStatus(STATUS_STN2);

        verify(controller).onTrxFrequencyReported(RadioSlot.SECOND, 432_200_000L);
    }

    @Test
    void withQrgSyncSwitchedOffAStatusReportsNothingToTheBeaconGuard() {
        ReadUDPByWintestThread thread = thread(true);
        when(prefs.isLogsynch_wintestQrgSyncEnabled()).thenReturn(false);

        thread.parseStatus(STATUS_STN2);

        verify(controller, never()).onTrxFrequencyReported(any(), anyLong());
    }

    @Test
    void withoutSo2rTheSecondStationIsFilteredOutAsBefore() {
        thread(false).parseStatus(STATUS_STN2);

        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
    }
}
