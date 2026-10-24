package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalLoggerListenerValidationTest {

	@Mock
	ChatController controller;

	@Mock
	ChatPreferences preferences;

	@Mock
	ThreadStatusCallback statusCallback;

	@BeforeEach
	void configurePreferences() {
		when(controller.getChatPreferences()).thenReturn(preferences);
	}

	@Test
	void ucxLogPacketWithoutCallsignIsDiscardedWithoutStoppingProcessing() {
		when(preferences.isLogsynch_ucxUDPWkdCallListenerEnabled()).thenReturn(true);
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(
				12060, controller, statusCallback);
		String packet = "<?xml version=\"1.0\"?><contactinfo>"
				+ "<band>144</band><call>   </call><gridsquare>JO50AA</gridsquare>"
				+ "</contactinfo>";

		assertDoesNotThrow(() -> listener.processUCXUDPMessage(packet));

		verify(controller, never()).recordLoggedQso(any(), any());
		verify(controller, never()).getDbHandler();
	}

	@Test
	void winTestPacketWithoutCallsignIsDiscardedWithoutStoppingProcessing() {
		when(preferences.getStn_loginCallSignRaw()).thenReturn("DL0TEST");
		when(preferences.getLogsynch_wintestNetworkPort()).thenReturn(9871);
		ReadUDPByWintestThread listener = new ReadUDPByWintestThread(controller, statusCallback);
		String packet = "ADDQSO: \"STN1\" \"\" \"STN1\" 1762202297 1440000 0 12 0 0 0 2 2 "
				+ "\"   \" \"599\" \"599001\" \"JO51UM\" \"\" \"\" 0 \"\" \"\" \"\" 44510";

		assertDoesNotThrow(() -> listener.processWinTestMessage(packet));

		verify(controller, never()).recordLoggedQso(any(), any());
		verify(controller, never()).getDbHandler();
	}
}
