package kst4contest.controller;

import static kst4contest.controller.BeaconSessionState.Channel.MAIN;
import static kst4contest.controller.BeaconSessionState.Channel.SECOND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import kst4contest.model.BeaconStatusSnapshot;
import kst4contest.model.BeaconStatusSnapshot.AggregateState;
import kst4contest.model.BeaconStatusSnapshot.InactiveReason;
import kst4contest.model.ChatPreferences;

class BeaconSessionStateTest {
    @TempDir Path tempDir;
    private ChatPreferences preferences;
    private BeaconSessionState state;

    @BeforeEach
    void setUp() {
        preferences = new ChatPreferences(tempDir.resolve("preferences.xml"));
        preferences.setLoginToSecondChatEnabled(true);
        preferences.setBcn_beaconTextMainCat("CQ MAIN");
        preferences.setBcn_beaconTextSecondCat("CQ SECOND");
        preferences.setMYQRGFirstCat("");
        preferences.setMYQRGSecondCat("");
        state = new BeaconSessionState();
        state.resetSession(preferences);
    }

    @Test
    void resetStartsBothCategoriesOffWithSessionReason() {
        BeaconStatusSnapshot snapshot = state.snapshot(preferences);
        assertEquals(AggregateState.OFF, snapshot.getAggregateState());
        assertEquals(InactiveReason.NOT_ACTIVATED_THIS_SESSION, snapshot.getMain().getInactiveReason());
        assertEquals(InactiveReason.NOT_ACTIVATED_THIS_SESSION, snapshot.getSecond().getInactiveReason());
        assertFalse(preferences.isBcn_beaconsEnabledMainCat());
        assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
    }

    @Test
    void enablesValidCategoryAndRejectsOtherCategoryWithMissingCrossTemplateQrg() {
        preferences.setMYQRGSecondCat("1296.123");
        preferences.setBcn_beaconTextMainCat("CQ SECONDQRG");
        preferences.setBcn_beaconTextSecondCat("CQ MYQRG");

        assertTrue(state.setEnabled(MAIN, true, preferences));
        assertFalse(state.setEnabled(SECOND, true, preferences));

        BeaconStatusSnapshot snapshot = state.snapshot(preferences);
        assertEquals(AggregateState.PARTIAL, snapshot.getAggregateState());
        assertTrue(snapshot.getMain().isEnabled());
        assertEquals(InactiveReason.PRIMARY_QRG_UNAVAILABLE, snapshot.getSecond().getInactiveReason());
    }

    @Test
    void manualQrgRebasesAndInvalidValueClearsReferenceWithoutPlaceholder() {
        assertTrue(state.setEnabled(MAIN, true, preferences));
        state.onManualQrgChanged(MAIN, "144.300", preferences);
        state.onExternalQrgChanged(MAIN, 144_306_000L, preferences);
        assertFalse(state.isEnabled(MAIN));

        assertTrue(state.setEnabled(MAIN, true, preferences));
        state.onManualQrgChanged(MAIN, "invalid", preferences);
        assertTrue(state.isEnabled(MAIN));
        state.onExternalQrgChanged(MAIN, 144_310_000L, preferences);
        assertTrue(state.isEnabled(MAIN), "first valid report establishes the cleared reference");
    }

    @Test
    void invalidManualQrgDisablesEveryActiveTemplateThatNeedsIt() {
        preferences.setMYQRGFirstCat("144.300");
        preferences.setBcn_beaconTextMainCat("CQ MYQRG");
        preferences.setBcn_beaconTextSecondCat("CQ MYQRGSHORT");
        assertTrue(state.setEnabled(MAIN, true, preferences));
        assertTrue(state.setEnabled(SECOND, true, preferences));

        state.onManualQrgChanged(MAIN, "", preferences);

        assertFalse(state.isEnabled(MAIN));
        assertFalse(state.isEnabled(SECOND));
        assertEquals(InactiveReason.PRIMARY_QRG_UNAVAILABLE,
                state.snapshot(preferences).getSecond().getInactiveReason());
    }

    @Test
    void externalMoveUsesStrictFiveKhzBoundaryAndOnlyItsCategory() {
        preferences.setMYQRGFirstCat("144.300");
        preferences.setMYQRGSecondCat("1296.100");
        assertTrue(state.setEnabled(MAIN, true, preferences));
        assertTrue(state.setEnabled(SECOND, true, preferences));

        state.onExternalQrgChanged(MAIN, 144_305_000L, preferences);
        assertTrue(state.isEnabled(MAIN));
        state.onExternalQrgChanged(MAIN, 144_305_001L, preferences);

        assertFalse(state.isEnabled(MAIN));
        assertTrue(state.isEnabled(SECOND));
        assertEquals(InactiveReason.AUTOMATIC_QRG_CHANGE,
                state.snapshot(preferences).getMain().getInactiveReason());
    }

    @Test
	void invalidExternalQrgClearsTheReferenceWithoutAPlaceholder() {
        assertTrue(state.setEnabled(MAIN, true, preferences));
		preferences.setMYQRGFirstCat("7.100.00");
		state.onExternalQrgChanged(MAIN, 7_100_000L, preferences);
        assertTrue(state.isEnabled(MAIN));

        preferences.setMYQRGFirstCat("144.300");
        state.onExternalQrgChanged(MAIN, 144_300_000L, preferences);

        assertTrue(state.isEnabled(MAIN), "the first valid report must establish the reference");
    }

    @Test
    void failureOverridesAggregateAndA_successClearsOnlyThatCategory() {
        assertTrue(state.setEnabled(MAIN, true, preferences));
        assertTrue(state.setEnabled(SECOND, true, preferences));
        state.recordSendFailure(MAIN, true);
        state.recordSendFailure(SECOND, true);
        assertEquals(AggregateState.FAILED, state.snapshot(preferences).getAggregateState());

        state.recordSendFailure(MAIN, false);
        BeaconStatusSnapshot snapshot = state.snapshot(preferences);
        assertEquals(AggregateState.FAILED, snapshot.getAggregateState());
        assertFalse(snapshot.getMain().isFailed());
        assertTrue(snapshot.getSecond().isFailed());
    }
}
