package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * Loopback tests for the UCXLog/DXLog UDP listener. Every socket is bound to
 * port 0 and every datagram goes to the loopback address, so the tests neither
 * collide with a running logger nor send anything to a broadcast address.
 */
class ReadUDPbyUCXMessageThreadLoopbackTest {

	/** RadioInfo that sets the first-category QRG to 1296.200.00; used as an ordering marker. */
	private static final String MARKER_PACKET = "<RadioInfo><Freq>129620000</Freq><Mode>CW</Mode></RadioInfo>";

	private final ChatController controller = mock(ChatController.class);
	private final ChatPreferences preferences = mock(ChatPreferences.class);
	private final BlockingQueue<ThreadStateMessage> statusMessages = new LinkedBlockingQueue<>();
	/**
	 * Makes {@link #statusCallback} throw after recording the safety net's
	 * dropped-packet report, to simulate a failing controller there. Other
	 * reports pass, so the ordering marker can still be processed.
	 */
	private volatile boolean statusCallbackThrows;
	private final ThreadStatusCallback statusCallback = (threadName, message) -> {
		statusMessages.add(message);
		if (statusCallbackThrows
				&& message.getRunningInformation().startsWith("packet dropped after error")) {
			throw new IllegalStateException("callback boom");
		}
	};
	/** First-category QRG that the RadioInfo ordering marker sets. */
	private final SimpleValue<String> markerQrg = new SimpleValue<>("");

	/**
	 * A processed RadioInfo packet shows that the receive loop is running,
	 * instead of a sleep. The receive timeout is 2 s, so ending within 1 s
	 * shows that interrupt() does not wait for the next timeout. Whether the
	 * thread sat inside receive() or between two receives at that moment is
	 * not observable; the closed socket and the interrupt flag end the loop
	 * in either case.
	 */
	@Test
	void interruptEndsTheReceiveLoopBeforeTheReceiveTimeout() throws Exception {
		givenRadioInfoMarkerEnabled();
		CountDownLatch marker = markerLatch();
		InetAddress loopback = InetAddress.getLoopbackAddress();
		DatagramSocket bound = new DatagramSocket(new InetSocketAddress(loopback, 0));
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(bound, controller, statusCallback);
		listener.setDaemon(true);
		listener.start();
		try (DatagramSocket sender = new DatagramSocket(new InetSocketAddress(loopback, 0))) {
			send(sender, new InetSocketAddress(loopback, bound.getLocalPort()), MARKER_PACKET);
			assertTrue(marker.await(5, TimeUnit.SECONDS), "marker packet must be processed");
		}

		listener.interrupt();
		listener.join(1_000);

		assertFalse(listener.isAlive(), "listener must stop once interrupted");
	}

	/**
	 * A DatagramPacket remembers the length of the last datagram it received.
	 * Without resetting it before every receive(), a long RadioInfo packet that
	 * follows a short one is cut to the short one's length and fails to parse.
	 * The QRG probe needs no database: RadioInfo only sets the first-category
	 * frequency through the UI dispatcher.
	 */
	@Test
	void longDatagramAfterShortOneIsProcessedCompletely() throws Exception {
		when(controller.getChatPreferences()).thenReturn(preferences);
		when(controller.uiDispatcher()).thenReturn(new DirectUiDispatcher());
		when(preferences.isTrxSynch_ucxLogUDPListenerEnabled()).thenReturn(true);
		SimpleValue<String> qrg = new SimpleValue<>("");
		when(preferences.getMYQRGFirstCat()).thenReturn(qrg);
		CountDownLatch firstQrg = new CountDownLatch(1);
		CountDownLatch secondQrg = new CountDownLatch(1);
		qrg.addListener(value -> {
			if (value.equals("144.200.00")) firstQrg.countDown();
			if (value.equals("432.200.00")) secondQrg.countDown();
		});

		String shortPacket = "<RadioInfo><Freq>14420000</Freq><Mode>CW</Mode></RadioInfo>";
		String longPacket = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<RadioInfo><StationName>CONTEST-PC-WITH-A-LONG-NAME</StationName>"
				+ "<RadioNr>1</RadioNr><Freq>43220000</Freq><TXFreq>43220000</TXFreq>"
				+ "<Mode>SSB</Mode><OpCall>DL0TEST</OpCall><IsRunning>False</IsRunning>"
				+ "<FocusEntry>0</FocusEntry><Antenna>1</Antenna><Rotors>1</Rotors>"
				+ "<FocusRadioNr>1</FocusRadioNr><IsStereo>False</IsStereo>"
				+ "<ActiveRadioNr>1</ActiveRadioNr></RadioInfo>";
		assertTrue(longPacket.length() > shortPacket.length());

		InetAddress loopback = InetAddress.getLoopbackAddress();
		DatagramSocket bound = new DatagramSocket(new InetSocketAddress(loopback, 0));
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(bound, controller, statusCallback);
		listener.setDaemon(true);
		listener.start();
		try (DatagramSocket sender = new DatagramSocket(new InetSocketAddress(loopback, 0))) {
			InetSocketAddress target = new InetSocketAddress(loopback, bound.getLocalPort());

			send(sender, target, shortPacket);
			assertTrue(firstQrg.await(5, TimeUnit.SECONDS), "short packet must be processed");
			assertEquals("144.200.00", qrg.get());

			send(sender, target, longPacket);
			assertTrue(secondQrg.await(5, TimeUnit.SECONDS), "long packet must be processed");
			assertEquals("432.200.00", qrg.get());
		} finally {
			listener.interrupt();
			listener.join(2_000);
		}
	}

	/**
	 * A UCXLog QSO packet delegates the complete contact to the shared controller
	 * action and writes the derived row to the UDP backup file.
	 * Bytes before the XML declaration are transport framing and are dropped.
	 */
	@Test
	void contactInfoPacketUsesSharedWorkedActionAndWritesBackup(@TempDir Path tempDir) throws Exception {
		Path backupFile = tempDir.resolve("udp-backup.txt");
		DBController database = givenQsoProcessingEnabled(backupFile);
		when(database.updateWkdInfoOnChatMember(any())).thenReturn(false);

		sendThenAwaitMarker("\u0001\u0002UCX"
				+ "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<contactinfo><app>UCXLog</app><mycall>DL0TEST</mycall><band>144</band>"
				+ "<rxfreq>14420000</rxfreq><txfreq>14420000</txfreq><mode>CW</mode>"
				+ "<call>OK1ABC</call><snt>599</snt><sntnr>001</sntnr><rcv>599</rcv>"
				+ "<rcvnr>001JO70FD</rcvnr><gridsquare>jo70fd</gridsquare><points>350</points>"
				+ "</contactinfo>");

		ArgumentCaptor<ExternalLoggedQso> qso = ArgumentCaptor.forClass(ExternalLoggedQso.class);
		verify(controller).recordLoggedQso(qso.capture(), any(ChatMember.class));
		assertEquals("OK1ABC", qso.getValue().getCallSign());
		assertEquals(LoggedQsoBand.BAND_144, qso.getValue().getBand());
		assertEquals("JO70FD", qso.getValue().getLocator());
		assertEquals("UCXLOG", qso.getValue().getSource());
		String backup = Files.readString(backupFile, StandardCharsets.UTF_8);
		assertTrue(backup.startsWith("\nOK1ABC;null;JO70FD;"), backup);
	}

	/**
	 * DXLog sends contactreplace when it broadcasts the whole log. Without a
	 * gridsquare the locator comes from the received exchange, and a station
	 * already in the database is updated rather than stored again.
	 */
	@Test
	void contactReplaceTakesTheLocatorFromTheReceivedExchange(@TempDir Path tempDir) throws Exception {
		DBController database = givenQsoProcessingEnabled(tempDir.resolve("udp-backup.txt"));
		when(database.updateWkdInfoOnChatMember(any())).thenReturn(true);

		sendThenAwaitMarker("<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<contactreplace><app>DXLog</app><band>432</band><call>SP2XYZ</call>"
				+ "<rcvnr>042JO93AB</rcvnr></contactreplace>");

		ArgumentCaptor<ExternalLoggedQso> qso = ArgumentCaptor.forClass(ExternalLoggedQso.class);
		verify(controller).recordLoggedQso(qso.capture(), any(ChatMember.class));
		assertEquals(LoggedQsoBand.BAND_432, qso.getValue().getBand());
		assertEquals("JO93AB", qso.getValue().getLocator());
	}

	@Test
	void contactInfoIsIgnoredWhileLogSyncIsDisabled(@TempDir Path tempDir) throws Exception {
		DBController database = givenQsoProcessingEnabled(tempDir.resolve("udp-backup.txt"));
		when(preferences.isLogsynch_ucxUDPWkdCallListenerEnabled()).thenReturn(false);

		sendThenAwaitMarker("<contactinfo><band>144</band><call>OK1ABC</call></contactinfo>");

		verify(controller, never()).recordLoggedQso(any(), any());
		verify(database, never()).updateWkdInfoOnChatMember(any());
	}

	/**
	 * An unwritable backup path (here: a directory) used to throw a
	 * NullPointerException that ended the listener for good. The backup write
	 * is skipped now; the QSO has already reached the controller and the
	 * database, and the following packet is still processed.
	 */
	@Test
	void unwritableBackupFileDoesNotStopQsoProcessingOrTheListener(@TempDir Path tempDir) throws Exception {
		DBController database = givenQsoProcessingEnabled(tempDir);
		when(database.updateWkdInfoOnChatMember(any())).thenReturn(false);

		sendThenAwaitMarker("<contactinfo><band>144</band><call>OK1ABC</call>"
				+ "<gridsquare>JO70FD</gridsquare></contactinfo>");

		verify(controller).recordLoggedQso(any(), any());
	}

	/** A RadioInfo without Freq used to end the listener with a NullPointerException. */
	@Test
	void radioInfoWithoutFrequencyIsSkippedAndTheListenerKeepsRunning(@TempDir Path tempDir) throws Exception {
		givenQsoProcessingEnabled(tempDir.resolve("udp-backup.txt"));

		sendThenAwaitMarker("<RadioInfo><Mode>CW</Mode></RadioInfo>");

		assertEquals("1296.200.00", markerQrg.get());
	}

	/**
	 * Any runtime exception while processing one packet drops that packet,
	 * reports a non-critical status and leaves the receive loop running.
	 */
	@Test
	void runtimeExceptionWhileProcessingDropsOnlyThatPacket(@TempDir Path tempDir) throws Exception {
		givenQsoProcessingEnabled(tempDir.resolve("udp-backup.txt"));
		doThrow(new IllegalStateException("boom")).when(controller).recordLoggedQso(any(), any());

		sendThenAwaitMarker("<contactinfo><band>144</band><call>OK1ABC</call></contactinfo>");

		ThreadStateMessage dropped = statusMessages.stream()
				.filter(message -> message.getRunningInformation().startsWith("packet dropped after error"))
				.findFirst()
				.orElseThrow();
		assertTrue(dropped.getRunningInformation().contains("boom"));
		assertTrue(dropped.isRunning());
		assertFalse(dropped.isCriticalState());
	}

	/**
	 * Wires the mocks for QSO processing, including the RadioInfo ordering
	 * marker of {@link #givenRadioInfoMarkerEnabled()}.
	 */
	private DBController givenQsoProcessingEnabled(Path backupFile) {
		DBController database = mock(DBController.class);
		givenRadioInfoMarkerEnabled();
		when(controller.getDbHandler()).thenReturn(database);
		when(preferences.isLogsynch_ucxUDPWkdCallListenerEnabled()).thenReturn(true);
		when(preferences.getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup())
				.thenReturn(backupFile.toString());
		return database;
	}

	/**
	 * Enables TRX sync so that {@link #MARKER_PACKET} sets {@link #markerQrg}.
	 * The listener handles datagrams one at a time on a single thread, so once
	 * the marker's QRG is set, every datagram it received before the marker
	 * has been processed completely.
	 */
	private void givenRadioInfoMarkerEnabled() {
		when(controller.getChatPreferences()).thenReturn(preferences);
		when(controller.uiDispatcher()).thenReturn(new DirectUiDispatcher());
		when(preferences.isTrxSynch_ucxLogUDPListenerEnabled()).thenReturn(true);
		when(preferences.getMYQRGFirstCat()).thenReturn(markerQrg);
	}

	/**
	 * The safety net reports a dropped packet through the status callback. If
	 * that callback throws as well, the listener must still keep running.
	 */
	@Test
	void failingStatusCallbackDoesNotEndTheListener(@TempDir Path tempDir) throws Exception {
		givenQsoProcessingEnabled(tempDir.resolve("udp-backup.txt"));
		doThrow(new IllegalStateException("boom")).when(controller).recordLoggedQso(any(), any());
		statusCallbackThrows = true;

		sendThenAwaitMarker("<contactinfo><band>144</band><call>OK1ABC</call></contactinfo>");

		assertTrue(statusMessages.stream()
				.anyMatch(message -> message.getRunningInformation().startsWith("packet dropped after error")));
	}

	/** Records what radio 2 receives. */
	private final List<String> secondQrgWrites = new CopyOnWriteArrayList<>();
	private final SimpleValue<String> secondQrg = new SimpleValue<>("");

	@Test
	void withSo2rRadioTwoGoesToTheSecondQrgAndLeavesMyQrgAlone(@TempDir Path tempDir) throws Exception {
		givenRadioInfoMarkerEnabled();
		givenSo2r(true);
		List<String> firstWrites = new CopyOnWriteArrayList<>();
		markerQrg.addListener(firstWrites::add);

		sendThenAwaitMarker("<RadioInfo><RadioNr>2</RadioNr><Freq>43220000</Freq><Mode>SSB</Mode></RadioInfo>");

		assertEquals(List.of("432.200.00"), secondQrgWrites);
		assertEquals(List.of("1296.200.00"), firstWrites, "only the marker may write MYQRG");
	}

	@Test
	void withSo2rARadioInfoWithoutRadioNumberIsRadioOne(@TempDir Path tempDir) throws Exception {
		givenRadioInfoMarkerEnabled();
		givenSo2r(true);
		List<String> firstWrites = new CopyOnWriteArrayList<>();
		markerQrg.addListener(firstWrites::add);

		sendThenAwaitMarker("<RadioInfo><Freq>14420000</Freq><Mode>CW</Mode></RadioInfo>");

		assertEquals(List.of("144.200.00", "1296.200.00"), firstWrites);
		assertTrue(secondQrgWrites.isEmpty());
	}

	@Test
	void withSo2rAnUnknownRadioNumberIsDroppedAndTheListenerKeepsRunning(@TempDir Path tempDir) throws Exception {
		givenRadioInfoMarkerEnabled();
		givenSo2r(true);

		sendThenAwaitMarker("<RadioInfo><RadioNr>x</RadioNr><Freq>43220000</Freq><Mode>SSB</Mode></RadioInfo>");

		assertTrue(secondQrgWrites.isEmpty());
	}

	@Test
	void withoutSo2rRadioTwoStillWritesMyQrg(@TempDir Path tempDir) throws Exception {
		givenRadioInfoMarkerEnabled();
		givenSo2r(false);
		List<String> firstWrites = new CopyOnWriteArrayList<>();
		markerQrg.addListener(firstWrites::add);

		sendThenAwaitMarker("<RadioInfo><RadioNr>2</RadioNr><Freq>43220000</Freq><Mode>SSB</Mode></RadioInfo>");

		assertEquals(List.of("432.200.00", "1296.200.00"), firstWrites);
		assertTrue(secondQrgWrites.isEmpty());
	}

	private void givenSo2r(boolean enabled) {
		when(preferences.isTrxSynch_so2rEnabled()).thenReturn(enabled);
		secondQrg.addListener(secondQrgWrites::add);
		when(preferences.getMYQRGSecondCat()).thenReturn(secondQrg);
	}

	private CountDownLatch markerLatch() {
		CountDownLatch marker = new CountDownLatch(1);
		markerQrg.addListener(value -> {
			if (value.equals("1296.200.00")) marker.countDown();
		});
		return marker;
	}

	/**
	 * Sends the packet and then the marker, and waits for the marker. This
	 * relies on the two datagrams arriving in the order they were sent. UDP
	 * does not guarantee that in general, but on the loopback interface, from
	 * one socket to one socket, they are delivered in order in practice.
	 */
	private void sendThenAwaitMarker(String packet) throws Exception {
		CountDownLatch marker = markerLatch();

		InetAddress loopback = InetAddress.getLoopbackAddress();
		DatagramSocket bound = new DatagramSocket(new InetSocketAddress(loopback, 0));
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(bound, controller, statusCallback);
		listener.setDaemon(true);
		listener.start();
		try (DatagramSocket sender = new DatagramSocket(new InetSocketAddress(loopback, 0))) {
			InetSocketAddress target = new InetSocketAddress(loopback, bound.getLocalPort());
			send(sender, target, packet);
			send(sender, target, MARKER_PACKET);
			assertTrue(marker.await(5, TimeUnit.SECONDS), "marker packet must be processed");
		} finally {
			listener.interrupt();
			listener.join(2_000);
		}
	}

	private static void send(DatagramSocket sender, InetSocketAddress target, String payload) throws Exception {
		byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
		sender.send(new DatagramPacket(bytes, bytes.length, target));
	}
}
