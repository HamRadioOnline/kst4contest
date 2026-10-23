package kst4contest.controller;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleRoster;
import kst4contest.observe.UiDispatcher;
import kst4contest.observe.MutableValue;
import kst4contest.observe.ObservableRoster;
import kst4contest.ApplicationConstants;
import kst4contest.controller.interfaces.PstRotatorEventListener;
import kst4contest.locatorUtils.DirectionUtils;
import kst4contest.logic.BandOpportunityResolver;
import kst4contest.logic.PriorityCalculator;
import kst4contest.model.*;
import kst4contest.utils.ApplicationFileUtils;
import kst4contest.utils.PlayAudioUtils;
import kst4contest.locatorUtils.Location;

import java.io.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.nio.charset.StandardCharsets;
import kst4contest.logic.FrequencyTextParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import kst4contest.observe.SimpleValue;



/**
 * 
 * Central Chat kst4contest.controller. Instantiate only one time per category of kst Chat.
 * Provides complex data types for communication with the gui and drives the
 * threads of telnet tx, telnet rx and message processing. <br/>
 * <b>SINGLETON</b>
 * 
 */
public class ChatController implements ThreadStatusCallback {

	/**
	 * Chat selection ? 50/70 MHz..............1 144/432 MHz............2
	 * Microwave..............3 EME/JT65...............4 Low Band...............5 50
	 * MHz IARU Region 3...6 50 MHz IARU Region 2...7 144/432 MHz IARU R 2...8
	 * 144/432 MHz IARU R 3...9 kHz (2000-630m).......10 Warc (30,17,12m)......11 28
	 * MHz................12 Your choice :
	 * 
	 */

	private static final Logger LOGGER =
			Logger.getLogger(ChatController.class.getName());

	private static final boolean DEBUG_BAND_UPGRADE_HINT = true; //for new band hint


		public static final int MIN_BEACON_INTERVAL_MINUTES = 1;
	public static final int MAX_BEACON_TEXT_LENGTH = 120;
	private static final long INITIAL_BEACON_DELAY_MILLIS = 10_000L;

	private Consumer<Double> viewRotorCallback;

	/*
	 * The rotator retry must never block the JavaFX Application Thread.
	 * A daemon scheduler performs the delayed SPID compatibility check.
	 */
	private final ScheduledExecutorService rotatorCommandScheduler =
			Executors.newSingleThreadScheduledExecutor(runnable -> {
				Thread thread = new Thread(
						runnable,
						"PSTRotator-Command-Retry"
				);
				thread.setDaemon(true);
				return thread;
			});

    private StatusUpdateListener statusListener; //update info interface for the threads


    private UpdateInformation updateInformation;
	private ChatPreferences chatPreferences;

	private ChatCategory chatCategoryMain;
	private ChatCategory chatCategorySecondChat;
	boolean connectedAndLoggedIn;
	boolean connectedAndNOTLoggedIn;
	boolean disconnected;
	boolean disconnectionPerformedByUser = false;
	private MessageHistoryRecorder messageHistoryRecorder =
			MessageHistoryRecorder.disabled();

	private final On4KstConnectionManager on4KstConnectionManager =
			new On4KstConnectionManager(this);


    public boolean isDisconnectionPerformedByUser() {
		return disconnectionPerformedByUser;
	}

	public void setDisconnectionPerformedByUser(boolean disconnectionPerformedByUser) {
		this.disconnectionPerformedByUser = disconnectionPerformedByUser;
	}


	public ChatCategory getChatCategorySecondChat() {
		return chatCategorySecondChat;
	}

	public void setChatCategorySecondChat(ChatCategory chatCategorySecondChat) {
		this.chatCategorySecondChat = chatCategorySecondChat;
	}

	public UpdateInformation getUpdateInformation() {
		return updateInformation;
	}

	public void setUpdateInformation(UpdateInformation updateInformation) {
		this.updateInformation = updateInformation;
	}

	public String getChatState() {
		return chatState;
	}

	public void setChatState(String chatState) {
		this.chatState = chatState;
	}

	public boolean isConnectedAndLoggedIn() {
		return connectedAndLoggedIn;
	}

	public void setConnectedAndLoggedIn(boolean connectedAndLoggedIn) {
		this.connectedAndLoggedIn = connectedAndLoggedIn;
	}

	public boolean isConnectedAndNOTLoggedIn() {
		return connectedAndNOTLoggedIn;
	}

	public void setConnectedAndNOTLoggedIn(boolean connectedAndNOTLoggedIn) {
		this.connectedAndNOTLoggedIn = connectedAndNOTLoggedIn;
	}

	public boolean isDisconnected() {
		return disconnected;
	}

	/**
	 * Returns the authoritative ON4KST lifecycle state.
	 *
	 * @return current connection, authentication or synchronization state
	 */
	public On4KstConnectionState getOn4KstConnectionState() {
		return on4KstConnectionManager.getState();
	}

	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}


    public StatusUpdateListener getStatusListener() {
        return statusListener;
    }

    public void setStatusListener(StatusUpdateListener statusListener) {
        this.statusListener = statusListener;
    }

    @Override
    public void onThreadStatus(String threadName, ThreadStateMessage threadStateMessage) {
        // Weiterleiten an die View
        if (statusListener != null) {
            statusListener.onThreadStatusChanged(threadName, threadStateMessage);
        } else System.out.println("ERRRRRRRRRRRRRRRRRRRRRRRRRRRÖRRRRRRRRRRRRRRRRRRR");
    }

	/**
	 * Publishes one authoritative connection-state transition to legacy controller
	 * flags, generic worker status listeners and the dedicated UI callback.
	 *
	 * <p>The compatibility flags remain derived values. No caller may set them to
	 * infer socket health; only the connection manager owns that decision.</p>
	 *
	 * @param state new lifecycle state
	 * @param detail human-readable progress or failure reason
	 * @param critical whether the transition should be emphasized as an error
	 */
	void updateOn4KstConnectionState(
			On4KstConnectionState state,
			String detail,
			boolean critical
	) {
		setConnectedAndLoggedIn(state == On4KstConnectionState.ONLINE);
		setConnectedAndNOTLoggedIn(
				state.isConnectionAttemptActive()
						&& state != On4KstConnectionState.ONLINE);
		setDisconnected(state == On4KstConnectionState.DISCONNECTED);

		ThreadStateMessage status = new ThreadStateMessage(
				"ON4KST", state.isConnectionAttemptActive(), detail, critical);
		status.setRunningInformationTextDescription(state.name());
		onThreadStatus("ON4KST", status);

		if (statusListener != null) {
			statusListener.onConnectionStateChanged(state, detail);
		}
	}

	/**
	 * Installs all resources belonging to one successfully opened connection
	 * generation.
	 *
	 * <p>The session-id check prevents a slow connection attempt from overwriting a
	 * newer socket and its queues.</p>
	 */
	synchronized void installOn4KstSession(
			long connectionSessionId,
			Socket sessionSocket,
			LinkedBlockingQueue<ChatMessage> receiveQueue,
			LinkedBlockingQueue<ChatMessage> transmitQueue,
			ReadThread sessionReadThread,
			WriteThread sessionWriteThread,
			MessageBusManagementThread sessionMessageProcessor
	) {
		if (!on4KstConnectionManager.isActiveSession(connectionSessionId)) {
			return;
		}
		this.socket = sessionSocket;
		this.messageRXBus = receiveQueue;
		this.messageTXBus = transmitQueue;
		this.readThread = sessionReadThread;
		this.writeThread = sessionWriteThread;
		this.messageProcessor = sessionMessageProcessor;
	}

	void onOn4KstLogstat(long connectionSessionId, String[] fields) {
		on4KstConnectionManager.onLogstat(connectionSessionId, fields);
	}

	void stageInitialOn4KstChatMember(
			long connectionSessionId,
			ChatMember member
	) {
		on4KstConnectionManager.stageInitialChatMember(
				connectionSessionId, member);
	}

	void onOn4KstInitialUserListCompleted(
			long connectionSessionId,
			ChatCategory category
	) {
		on4KstConnectionManager.onInitialUserListCompleted(
				connectionSessionId, category);
	}

	void onOn4KstConnectionOnline() {
		scheduleBeaconTimer(INITIAL_BEACON_DELAY_MILLIS);
	}

	void onOn4KstConnectionLost() {
		stopBeaconTimer();
	}

	void onOn4KstOutboundFrameRejected(String reason) {
		onOn4KstConnectionWarning("Outbound frame rejected locally: " + reason);
	}

	/**
	 * Receives a non-fatal ON4KST diagnostic, writes it to the persistent warning
	 * log and forwards it to the status UI.
	 *
	 * @param warning sanitized diagnostic text; passwords and raw chat frames must
	 *                never be included
	 */
	void onOn4KstConnectionWarning(String warning) {
		LOGGER.log(Level.WARNING, "ON4KST warning: {0}", warning);
		ThreadStateMessage status = new ThreadStateMessage(
				"ON4KST", true, warning, false);
		status.setRunningInformationTextDescription("WARNING");
		onThreadStatus("ON4KST", status);
	}


    /********************************************************************************
     * PSTRotator controlling
     *******************************************************************************/




	public void initRotor() {
		rotator1.start();
	}

	/** Kept for existing references; the value lives in RotatorControl. */
	static final double ROTATOR_REACHED_TOLERANCE_DEGREES = RotatorControl.REACHED_TOLERANCE_DEGREES;

	/** Rotator of radio 1; also the only one without SO2R. */
	private final RotatorControl rotator1 = new RotatorControl(
			"PSTRotator",
			control -> startPstRotatorClient(control, chatPreferences.getStn_pstRotatorHost(),
					chatPreferences.getStn_pstRotatorPort(), "PSTRotator"),
			rotatorCommandScheduler,
			azimuth -> runOnUi(() -> chatPreferences.getActualQTF().set(azimuth)));

	/** Rotator of radio 2; only with SO2R and its own switch. */
	private final RotatorControl rotator2 = new RotatorControl(
			"PSTRotator2",
			control -> startPstRotatorClient(control, chatPreferences.getStn_pstRotator2Host(),
					chatPreferences.getStn_pstRotator2Port(), "PSTRotator2"),
			rotatorCommandScheduler,
			azimuth -> runOnUi(() -> chatPreferences.getActualQTF2().set(azimuth)));

	/** Test seam: the rotator of radio 2. */
	RotatorControl secondRotator() {
		return rotator2;
	}

	static boolean secondRotatorWanted(ChatPreferences prefs) {
		return prefs.isTrxSynch_so2rEnabled() && prefs.isStn_pstRotator2Enabled();
	}

	/** Turns the antenna of [slot]; radio 2 only does something while its rotator runs. */
	public void rotateTo(RadioSlot slot, double azimuth) {
		(slot == RadioSlot.SECOND ? rotator2 : rotator1).rotateTo(azimuth);
	}

	/*
	 * Not isDisconnected(): that flag defaults to false before the first connect,
	 * so it would claim a session where none exists.
	 */
	private boolean isSessionRunning() {
		return on4KstConnectionManager != null && sessionRunning(on4KstConnectionManager.getState());
	}

	static boolean sessionRunning(On4KstConnectionState state) {
		return state != null
				&& state != On4KstConnectionState.DISCONNECTED
				&& state != On4KstConnectionState.STOPPING;
	}

	/**
	 * Follows the settings for rotator 2 after Apply: always stopped first, so a changed
	 * host or port is bound anew, and started again only while a session runs. Without it,
	 * QTF2 shows nothing rather than a stale heading.
	 */
	public void reconfigureSecondRotator() {
		rotator2.stop();
		if (secondRotatorWanted(chatPreferences)) {
			if (isSessionRunning()) {
				rotator2.start();
			}
		} else {
			runOnUi(() -> chatPreferences.getActualQTF2().set(Double.NaN));
		}
	}

	/**
	 * Opens one PSTRotator connection whose position reports go to [control]. The
	 * returned link is what RotatorControl drives; stopping it also stops the polling.
	 */
	private RotatorControl.Link startPstRotatorClient(RotatorControl control, String host, int port, String threadName) {
		PstRotatorClient client = new PstRotatorClient(host, port, threadName, new PstRotatorEventListener() {
			@Override public void onAzimuthUpdate(double azimuth) { control.onAzimuthUpdate(azimuth); }
			@Override public void onElevationUpdate(double elevation) { /* not used */ }
			@Override public void onModeUpdate(boolean isTracking) { /* not used */ }
			@Override public void onMessageReceived(String raw) { /* not used */ }
		}, this);
		client.start();
		return new RotatorControl.Link() {
			@Override public void setTrackingMode(boolean on) { client.setTrackingMode(on); }
			@Override public void setAzimuth(double azimuth) { client.setAzimuth(azimuth); }
			@Override public void stopRotation() { client.stopRotor(); }
			@Override public void stop() { client.stop(); }
		};
	}

	/* Runs on the UI thread, directly when already there. */
	private void runOnUi(Runnable update) {
		if (uiDispatcher.isUiThread()) {
			update.run();
		} else {
			uiDispatcher.runOnUi(update);
		}
	}

	/**
	 * Sends a new azimuth to PSTRotator without blocking the UI thread; see RotatorControl.
	 *
	 * @param azimuth required antenna azimuth in degrees
	 */
	public void rotateTo(double azimuth) {
		rotator1.rotateTo(azimuth);
	}

	/**
	 * Called when an external logger reports that a QSO was logged.
	 *
	 * <p>The common resolver combines recent QRG evidence and station-name bands
	 * across every active callsignRaw variant. A manual NOT-QRV tag overrides this
	 * automatic evidence before the hint and priority boost are evaluated.</p>
	 */
	public void onExternalLogEntryReceived(String callSignRaw) {

		if (callSignRaw == null || callSignRaw.isBlank()) return;
		if (chatPreferences == null) return;
		if (!chatPreferences.isNotify_bandUpgradeHintOnLogEnabled()) return;

		final String callRaw = normalizeCallRaw(callSignRaw);

		if (DEBUG_BAND_UPGRADE_HINT) {
			System.out.println("[BandUpgradeHint] LOG received for call=" + callRaw);
		}

		EnumSet<Band> myEnabledBands =
				BandOpportunityResolver.getEnabledStationBands(chatPreferences);
		if (myEnabledBands.isEmpty()) return;

		List<ChatMember> variants = findActiveChatMembersByRawCall(callRaw);
		BandOpportunityResolver.Resolution bandResolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis());

		EnumSet<Band> stationOfferedBands = bandResolution.getOfferedBands();
		if (stationOfferedBands.isEmpty()) return;

		EnumSet<Band> workedBands = bandResolution.getWorkedBands();
		EnumSet<Band> remainingBands =
				bandResolution.getUnworkedEnabledBands(myEnabledBands);
		if (remainingBands.isEmpty()) return;

		if (DEBUG_BAND_UPGRADE_HINT) {
			System.out.println("[BandUpgradeHint] call=" + callRaw
					+ " enabled=" + formatBandsHuman(myEnabledBands)
					+ " offered=" + formatBandsHuman(stationOfferedBands)
					+ " worked=" + (workedBands.isEmpty() ? "-" : formatBandsHuman(workedBands))
					+ " NOT-QRV=" + formatBandsHuman(bandResolution.getNotQrvBands())
					+ " remaining=" + formatBandsHuman(remainingBands));
		}

		String remainingHuman = formatBandsHuman(remainingBands);
		String shortText = "BAND+ " + callRaw + " " + remainingHuman;

		String tooltip = "Logged " + callRaw
				+ ", but the station still offers additional band(s): "
				+ remainingHuman
				+ "\n(Enabled: " + formatBandsHuman(myEnabledBands)
				+ " | Worked: " + (workedBands.isEmpty() ? "-" : formatBandsHuman(workedBands))
				+ " | NOT QRV: " + formatBandsHuman(bandResolution.getNotQrvBands()) + ")";

		ThreadStateMessage msg = new ThreadStateMessage("BandUpgradeHint", true, tooltip, false);
		msg.setRunningInformationTextDescription(shortText);
		onThreadStatus("BandUpgradeHint", msg);

		if (chatPreferences.isNotify_playSimpleSounds()) {
			try {
				getPlayAudioUtils().playNoiseLauncher('!');
			} catch (Exception e) {
				System.out.println(
						"[ChatController, warning]: failed to play band-upgrade hint sound: "
								+ e.getMessage()
				);
			}
		}

		if (getScoreService() != null) {
			getScoreService().requestRecompute("BandUpgradeHint");
		}
	}

	/** Normalize callsign raw to a stable key for comparisons. */
	private static String normalizeCallRaw(String callRaw) {
		return callRaw.trim().toUpperCase(Locale.ROOT);
	}

	private static String formatBandsHuman(EnumSet<Band> bands) {
		if (bands == null || bands.isEmpty()) return "-";
		return bands.stream().map(ChatController::bandToHumanLabel).sorted().reduce((a, b) -> a + ", " + b).orElse("-");
	}

	private static String bandToHumanLabel(Band b) {
		if (b == null) return "?";
		return switch (b) {
			case B_50 -> "6m";
			case B_70 -> "4m";
			case B_144 -> "2m";
			case B_432 -> "70cm";
			case B_1296 -> "23cm";
			case B_2320 -> "13cm";
			case B_3400 -> "9cm";
			case B_5760 -> "6cm";
			case B_10G -> "3cm";
			case B_24G -> "1.2cm";
			default -> b.name();
		};
	}


	public void stopRotator() {
		rotator1.stop();
	}

	/**
	 * Helping
	 * @param targetCallSignRaw
	 * @param preferredCategory
	 * @param messageAfterCq
	 */
	public void queuePrivateCqMessage(String targetCallSignRaw, ChatCategory preferredCategory, String messageAfterCq) {
		if (targetCallSignRaw == null || targetCallSignRaw.isBlank()) return;

		ChatCategory categoryToUse = preferredCategory;
		if (categoryToUse == null) {
			ChatCategory last = lastInboundCategoryByCallSignRaw.get(targetCallSignRaw.trim().toUpperCase());
			categoryToUse = (last != null) ? last : chatCategoryMain;
		}

		String text = "/cq " + targetCallSignRaw.trim().toUpperCase() + " " + (messageAfterCq == null ? "" : messageAfterCq);

		ChatMessage msg = new ChatMessage();
		msg.setChatCategory(categoryToUse);
		msg.setMessageText(text);
		msg.setMessageDirectedToServer(false);

		messageTXBus.add(msg);

		// Metrics: treat this as an outbound ping
		stationMetricsService.tryRecordOutboundCq(text, System.currentTimeMillis());

		// Scoring should react quickly to outbound actions
		if (scoreService != null) {
			scoreService.requestRecompute("outbound-cq");
		}
	}



	/**
     *
     * @param remoteChatMember with callsign of the foreign station
     */

	/**
	 * Requests AirScout to display the path between the own station and the
	 * selected remote station.
	 *
	 * The configured server identifier, client identifier, band and UDP port are
	 * used for every request. This allows several AirScout servers or clients to
	 * coexist in the same network.
	 *
	 * @param remoteChatMember selected remote station
	 */
	public void airScout_SendAsShowPathPacket(
			ChatMember remoteChatMember
	) {
		if (!chatPreferences.isAirScout_asUDPListenerEnabled()) {
			System.out.println(
					"[AirScout, info]: Show-path request ignored because "
							+ "the AirScout integration is disabled."
			);
			return;
		}

		if (remoteChatMember == null
				|| remoteChatMember.getCallSign() == null
				|| remoteChatMember.getCallSign().isBlank()
				|| remoteChatMember.getQra() == null
				|| remoteChatMember.getQra().isBlank()) {
			System.out.println(
					"[AirScout, warning]: Show-path request ignored because "
							+ "the selected station has no usable callsign or locator."
			);
			return;
		}

		String ownCallSign = chatPreferences.getStn_loginCallSign();
		if (ownCallSign == null || ownCallSign.isBlank()) {
			return;
		}

		int suffixSeparator = ownCallSign.indexOf("-");
		if (suffixSeparator > 0) {
			ownCallSign = ownCallSign.substring(0, suffixSeparator);
		}

		String ownLocator =
				chatPreferences.getStn_loginLocatorMainCat();

		if (ownLocator == null || ownLocator.isBlank()) {
			return;
		}

		String clientIdentifier =
				chatPreferences.getAirScout_asClientNameString();
		String serverIdentifier =
				chatPreferences.getAirScout_asServerNameString();
		String bandValue = resolveAirScoutBandValue(remoteChatMember);
		if (bandValue == null) {
			System.out.println(
					"[AirScout, info]: Show-path request ignored because no "
							+ "usable propagation frequency could be resolved."
			);
			return;
		}

		int port =
				chatPreferences.getAirScout_asCommunicationPort();

		String query =
				"ASSHOWPATH: \""
						+ clientIdentifier
						+ "\" \""
						+ serverIdentifier
						+ "\" "
						+ bandValue
						+ ","
						+ ownCallSign
						+ ","
						+ ownLocator
						+ ","
						+ remoteChatMember.getCallSign()
						+ ","
						+ remoteChatMember.getQra()
						+ " ";

		byte[] payload = query.getBytes(StandardCharsets.UTF_8);

		try (
				DatagramSocket socket = new DatagramSocket()
		) {
			socket.setBroadcast(true);

			InetAddress address =
					InetAddress.getByName("255.255.255.255");

			DatagramPacket packet = new DatagramPacket(
					payload,
					payload.length,
					address,
					port
			);

			socket.send(packet);
		} catch (IOException exception) {
			System.out.println(
					"[AirScout, error]: Could not send show-path request: "
							+ exception.getMessage()
			);
		}
	}

	/**
	 * Resolves the AirScout protocol frequency for one station. In automatic mode
	 * this uses the same station-specific resolution as the internal path analysis;
	 * manual mode keeps the configured forced value for compatibility.
	 *
	 * @param remoteChatMember target station
	 * @return AirScout frequency in 100-Hz units, or {@code null} if auto mode has
	 *         no safe result
	 */
	public String resolveAirScoutBandValue(ChatMember remoteChatMember) {
		if (!chatPreferences.isAirScout_autoBandSelectionEnabled()) {
			return chatPreferences.getAirScout_asBandString();
		}

		if (reachabilityService == null) {
			return null;
		}

		var resolution = reachabilityService
				.resolveAutomaticPropagationFrequency(remoteChatMember);

		return resolution == null ? null : resolution.getAirScoutBandValue();
	}

	/**
	 * starts the calculation scheduler for scores / priorities of skeds to be made
	 */
	private void startScoreScheduler() {

		if (scoreScheduler != null && !scoreScheduler.isShutdown()) return;

		scoreScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r);
			t.setName("ScoreServiceScheduler");
			t.setDaemon(true);
			return t;
		});

		scoreScheduler.scheduleAtFixedRate(() -> {
			try {
				scoreService.tick();
			} catch (Exception e) {
				System.err.println("[ChatController] CRITICAL ERROR in ScoreService tick:");
				e.printStackTrace();
			}
		}, 1, 3, TimeUnit.SECONDS);

		scoreService.requestRecompute("startup");
		System.out.println("[ChatController] ScoreService scheduler started.");
	}

	private void stopScoreScheduler() {
		if (scoreScheduler != null) scoreScheduler.shutdownNow();
		scoreScheduler = null;
	}


	/**
	 * Handles the disconnect of either the chat (Case DISCONNECTONLY) or the
	 * complete application life including all threads (case CLOSEALL)<br/><br/>
	 * Look in ApplicationConstants for the DISCSTRINGS
	 * 
	 * @param action: "CLOSEALL" or "DISCONNECTONLYCHAT", on application close event
	 *                (Settings Window closed), Disconnect on Disconnect-Button
	 *                clicked (new connection may follow)
	 */
	public void disconnect(String action) {

		/*
		 * New connections are owned by the session manager. Keep the historic cleanup
		 * below as a compatibility fallback, but do not let it manipulate resources
		 * belonging to a replacement session.
		 */
		if (on4KstConnectionManager != null) {
			disconnectManaged(action);
			return;
		}

//		stopContextLoop(); //stops thread for calculating sked priorities

		stopScoreScheduler();

		stopDxClusterServer();

		this.setDisconnectionPerformedByUser(true);

		try {
			/**
			 * Kill UCX and Wintest packetreader by sending poison pill to the reader threads
			 */
			DatagramSocket dsocket;

			String host = "255.255.255.255";
			int port = chatPreferences.getLogsynch_ucxUDPWkdCallListenerPort();
			InetAddress address;

			address = InetAddress.getByName("255.255.255.255");
			DatagramPacket packet = new DatagramPacket(ApplicationConstants.DISCONNECT_RDR_POISONPILL.getBytes(), ApplicationConstants.DISCONNECT_RDR_POISONPILL.length(), address, port);
            DatagramPacket killWintestReaderPacket = new DatagramPacket(ApplicationConstants.DISCONNECT_RDR_POISONPILL.getBytes(), ApplicationConstants.DISCONNECT_RDR_POISONPILL.length(), address, chatPreferences.getLogsynch_wintestNetworkPort());

            dsocket = new DatagramSocket();
			dsocket.setBroadcast(true);
			dsocket.send(packet);
			dsocket.close();

            dsocket = new DatagramSocket();
            dsocket.setBroadcast(true);
            dsocket.send(killWintestReaderPacket);
            dsocket.close();

			readUDPbyUCXThread.interrupt();
			stopWintestUdpListener();


		} catch (Exception error) {
			System.out.println("Chatcrontroller, ERROR: unable to send poison pill to ucxThread");
		}

		try {
			/**
			 * Kill AS packetreader by sending poison pill to the reader thread
			 */
			DatagramSocket dsocket;

			String host = "255.255.255.255";
			int port = chatPreferences.getAirScout_asCommunicationPort();
			InetAddress address;

			address = InetAddress.getByName("255.255.255.255");
			DatagramPacket packet = new DatagramPacket(ApplicationConstants.DISCONNECT_RDR_POISONPILL.getBytes(), ApplicationConstants.DISCONNECT_RDR_POISONPILL.length(), address, port);
			dsocket = new DatagramSocket();
			dsocket.setBroadcast(true);
			dsocket.send(packet);
			dsocket.close();
		} catch (Exception error) {
			System.out.println("Chatcrontroller, ERROR: unable to send poison pill to ucxThread");
		}


		if (action.equals(ApplicationConstants.DISCSTRING_DISCONNECT_AND_CLOSE)) {

//			this.lst_chatMemberList.clear();;
			this.clearActiveChatMembers();
			this.lst_clusterMemberList.clear();

			this.setDisconnected(true);
			this.setConnectedAndLoggedIn(false);
			this.setConnectedAndNOTLoggedIn(false);
			// disconnect telnet and kill all sockets and connections
			
			keepAliveTimer.cancel();
			keepAliveTimer.purge();
			
			ChatMessage killThreadPoisonPillMsg = new ChatMessage();
			killThreadPoisonPillMsg.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
			killThreadPoisonPillMsg.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
			
			messageRXBus.clear();
			messageTXBus.clear();
			messageRXBus.add(killThreadPoisonPillMsg); //kills messageprocessor
			messageTXBus.add(killThreadPoisonPillMsg); //kills writethread

//			writeThread.interrupt();
//			readThread.interrupt();

			stopBeaconTimer();
			ASQueryTimer.purge();
			ASQueryTimer.cancel();
			socketCheckTimer.purge();
			socketCheckTimer.cancel();

			userActualizationtimer.purge();
			userActualizationtimer.cancel();

			userActualizationtimer.purge();
			userActualizationtimer.cancel();

//			consoleReader.interrupt();
			messageProcessor.interrupt();
			
			readUDPbyUCXThread.interrupt();
			stopWintestUdpListener();


			
			airScoutUDPReaderThread.interrupt();
			
			dbHandler.closeDBConnection();

            rotator1.stopRotation();
            rotator1.stop();
            rotator2.stopRotation();
            rotator2.stop();

			try {

				if (socket != null) {
					socket.close();
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		} else if (action.equals(ApplicationConstants.DISCSTRING_DISCONNECTONLY)){

			this.clearActiveChatMembers();
			this.lst_clusterMemberList.clear();


			this.setDisconnected(true);
			this.setConnectedAndLoggedIn(false);
			this.setConnectedAndNOTLoggedIn(false);
			// disconnect telnet and kill all sockets and connections

			keepAliveTimer.cancel();
			keepAliveTimer.purge();

			ChatMessage killThreadPoisonPillMsg = new ChatMessage();
			killThreadPoisonPillMsg.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
			killThreadPoisonPillMsg.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);

			messageRXBus.clear();
			messageTXBus.clear();
			messageRXBus.add(killThreadPoisonPillMsg); //kills messageprocessor
			messageTXBus.add(killThreadPoisonPillMsg); //kills writethread

			writeThread.interrupt();
			readThread.interrupt();

			beaconTimer.purge();
			beaconTimer.cancel();
			ASQueryTimer.purge();
			ASQueryTimer.cancel();
			socketCheckTimer.purge();
			socketCheckTimer.cancel();

			userActualizationtimer.purge();
			userActualizationtimer.cancel();

//			consoleReader.interrupt();
//			messageProcessor.interrupt();

			readUDPbyUCXThread.interrupt(); //need poisonpill?
			stopWintestUdpListener();

			airScoutUDPReaderThread.interrupt(); //need poisonpill?

//			dbHandler.closeDBConnection();
//			this.dbHandler = null;


			try {

				if (socket != null) {
					socket.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e2) {
				e2.printStackTrace();
			}

		}

	}

	private void disconnectManaged(String action) {
		setDisconnectionPerformedByUser(true);
		on4KstConnectionManager.stopByUser();
		clearActiveChatMembers();
		runOnFxThread(() -> lst_clusterMemberList.clear());

		stopBeaconTimer();
		stopScoreScheduler();
		stopDxClusterServer();
		cancelTimer(userActualizationtimer);
		userActualizationtimer = null;
		cancelTimer(ASQueryTimer);
		ASQueryTimer = null;

		stopUdpReader(readUDPbyUCXThread,
				chatPreferences.getLogsynch_ucxUDPWkdCallListenerPort());
		readUDPbyUCXThread = null;
		stopUdpReader(readUDPByWintestThread,
				chatPreferences.getLogsynch_wintestNetworkPort());
		stopWintestUdpListener();
		stopUdpReader(airScoutUDPReaderThread,
				chatPreferences.getAirScout_asCommunicationPort());
		airScoutUDPReaderThread = null;

		if (ApplicationConstants.DISCSTRING_DISCONNECT_AND_CLOSE.equals(action)) {
			if (dbHandler != null) {
				dbHandler.closeDBConnection();
			}
			rotator1.stopRotation();
			rotator1.stop();
			rotator2.stopRotation();
			rotator2.stop();

			releaseBackgroundExecutors();
		}
	}

	/**
	 * Stops the background executors that live as long as this controller.
	 *
	 * <p>These are not bound to one ON4KST session, so disconnecting leaves them running
	 * on purpose. When the controller itself is discarded they have to go, otherwise a
	 * discarded controller stays reachable through its own threads.</p>
	 */
	private void releaseBackgroundExecutors() {

		on4KstConnectionManager.shutdown();
		skedReminderService.shutdown();

		if (reachabilityService != null) {
			reachabilityService.shutdown();
		}

		rotatorCommandScheduler.shutdownNow();
	}

	private void cancelTimer(Timer timer) {
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
	}

	private void stopUdpReader(Thread readerThread, int listenerPort) {
		if (readerThread != null) {
			readerThread.interrupt();
		}
		try (DatagramSocket datagramSocket = new DatagramSocket()) {
			datagramSocket.setBroadcast(true);
			byte[] poison = ApplicationConstants.DISCONNECT_RDR_POISONPILL.getBytes(
					StandardCharsets.UTF_8);
			DatagramPacket packet = new DatagramPacket(
					poison, poison.length,
					InetAddress.getByName("255.255.255.255"), listenerPort);
			datagramSocket.send(packet);
		} catch (IOException exception) {
			System.out.println("[ChatController, warning]: could not wake UDP reader on port "
					+ listenerPort + ": " + exception.getMessage());
		}
	}



//	private ObservableList<ContestSked> activeSkeds = FXCollections.observableArrayList();
//	public ObservableList<ContestSked> getActiveSkeds() {
//		return activeSkeds;
//	}

	// SIGNAL: Ein Property, das wir hochzählen, um der GUI zu sagen "Daten haben sich geändert"
//	private LongProperty uiRefreshSignal = new SimpleLongProperty(0);
//	public LongProperty uiRefreshSignalProperty() {
//		return uiRefreshSignal;
//	}

//	public void addSked(ContestSked sked) {
//		uiDispatcher.runOnUi(() -> {
//			this.activeSkeds.add(sked);
//			runContextLoopCycle(); // Trigger sofort
//		});
//	}

//	private PriorityCalculator priorityCalculator = new PriorityCalculator();
//	private ScheduledExecutorService contextLoopService;


	private final SimpleRoster<ContestSked> activeSkeds = new SimpleRoster<>();

	public ObservableRoster<ContestSked> getActiveSkeds() {
		return activeSkeds;
	}

	/**
	 * Priority score pipeline (replaces the former 1-second ContextLoop).
	 */
	private final Map<String, ChatCategory> lastInboundCategoryByCallSignRaw =
			new java.util.concurrent.ConcurrentHashMap<>();

	private final ScoreService scoreService = new ScoreService(this, new PriorityCalculator(), 15);
	private ScheduledExecutorService scoreScheduler;
	private final StationMetricsService stationMetricsService = new StationMetricsService();
	private final SkedReminderService skedReminderService = new SkedReminderService(this);


	public ScoreService getScoreService() {
		return scoreService;
	}

	public void addSked(ContestSked sked) {
		uiDispatcher.runOnUi(() -> {
			this.activeSkeds.add(sked);
			scoreService.requestRecompute("sked-added");
		});

		// Push sked to Win-Test via UDP if enabled
		if (chatPreferences.isLogsynch_wintestNetworkListenerEnabled()) {
			pushSkedToWinTest(sked);
		}
	}

	/**
	 * Pushes a sked to Win-Test via UDP broadcast.
	 *
	 * <p>The internal sked is independent from this handover. If no frequency can
	 * be resolved safely for the selected band, the internal sked remains active,
	 * but no misleading Win-Test entry is created.</p>
	 */
	private void pushSkedToWinTest(ContestSked sked) {
		new Thread(() -> {
			try {
				Double frequencyKHz = resolveSkedFrequencyKHz(sked);

				if (frequencyKHz == null) {
					reportSkippedWinTestSked(
							sked,
							"no recent or configured QRG matches "
									+ sked.getBand().getDisplayLabel()
					);
					return;
				}

				String winTestCallsign =
						toWinTestSkedCallsign(
								sked.getTargetChatCallsign()
						);

				if (winTestCallsign == null || winTestCallsign.isBlank()) {
					reportSkippedWinTestSked(
							sked,
							"the target callsign could not be converted"
					);
					return;
				}

				InetAddress broadcastAddress =
						winTestAddressResolver.resolveBroadcastAddress(
								chatPreferences
										.getLogsynch_wintestNetworkBroadcastAddress()
						);

				int port =
						chatPreferences.getLogsynch_wintestNetworkPort();

				String stationName =
						chatPreferences
								.getLogsynch_wintestNetworkStationNameOfKST();

				WinTestSkedSender sender = new WinTestSkedSender(
						stationName,
						broadcastAddress,
						port,
						this
				);

				String targetLocator =
						resolveSkedTargetLocator(
								sked.getTargetCallsign()
						);

				String notes = "sked via KST4Contest";

				if (targetLocator != null
						&& !targetLocator.isBlank()
						&& sked.getTargetAzimuth() > 0) {

					notes = String.format(
							"[%s - %.0f°] %s",
							targetLocator,
							sked.getTargetAzimuth(),
							notes
					);

				} else if (targetLocator != null
						&& !targetLocator.isBlank()) {

					notes = String.format(
							"[%s] %s",
							targetLocator,
							notes
					);

				} else if (sked.getTargetAzimuth() > 0) {

					notes = String.format(
							"[%.0f°] %s",
							sked.getTargetAzimuth(),
							notes
					);
				}

				String configuredMode =
						chatPreferences.getLogsynch_wintestSkedMode();

				/*
				 * Win-Test mode IDs:
				 * 0 = CW
				 * 1 = SSB
				 *
				 * SSB is the safe fallback for missing or obsolete settings.
				 * The former AUTO mode was frequency-based and could not produce
				 * reliable results on every supported VHF, UHF and SHF band.
				 */
				int winTestMode =
						"CW".equalsIgnoreCase(configuredMode)
								? 0
								: 1;

				sender.pushSkedToWinTest(
						sked,
						winTestCallsign,
						frequencyKHz,
						notes,
						winTestMode
				);

			} catch (Exception exception) {
				String message =
						"Error pushing sked to Win-Test: "
								+ exception.getMessage();

				System.out.println(
						"[ChatController] " + message
				);

				onThreadStatus(
						"WT-SkedSend",
						new ThreadStateMessage(
								"WT-SkedSend",
								false,
								message,
								true
						)
				);

				exception.printStackTrace();
			}
		}, "WinTestSkedPush").start();
	}

	/**
	 * Resolves the frequency used for the Win-Test sked.
	 *
	 * <ol>
	 *     <li>Newest QRG detected for the target station on the selected band</li>
	 *     <li>Own QRG belonging to the target's chat category</li>
	 *     <li>No result: do not send the Win-Test sked</li>
	 * </ol>
	 */
	private Double resolveSkedFrequencyKHz(ContestSked sked) {
		if (sked == null || sked.getBand() == null) {
			return null;
		}

		Double targetFrequencyKHz =
				resolveRecentTargetFrequencyKHz(sked);

		if (targetFrequencyKHz != null) {
			System.out.println(
					"[ChatController] SKED frequency from target: "
							+ targetFrequencyKHz
							+ " kHz on "
							+ sked.getBand()
			);
			return targetFrequencyKHz;
		}

		String ownQrg =
				resolveOwnQrgForSkedCategory(
						sked.getTargetChatCategory()
				);

		Double ownFrequencyKHz =
				parseSkedFrequencyKHz(
						ownQrg,
						sked.getBand()
				);

		if (ownFrequencyKHz != null) {
			System.out.println(
					"[ChatController] SKED frequency from own category QRG: "
							+ ownFrequencyKHz
							+ " kHz on "
							+ sked.getBand()
			);
			return ownFrequencyKHz;
		}

		return null;
	}

	/**
	 * Returns the newest QRG detected on the selected band across all active
	 * suffix and category variants of the base callsign.
	 */
	private Double resolveRecentTargetFrequencyKHz(ContestSked sked) {
		List<ChatMember> variants =
				findActiveChatMembersByRawCall(
						sked.getTargetCallsign()
				);

		long now = System.currentTimeMillis();
		long newestTimestamp = Long.MIN_VALUE;
		Double newestFrequencyKHz = null;

		for (ChatMember member : variants) {
			if (member == null || member.getKnownActiveBands() == null) {
				continue;
			}

			ChatMember.ActiveFrequencyInfo frequencyInfo =
					member.getKnownActiveBands().get(
							sked.getBand()
					);

			if (frequencyInfo == null
					|| frequencyInfo.frequency <= 0.0
					|| !sked.getBand().isPlausible(
					frequencyInfo.frequency
			)) {
				continue;
			}

			long ageMs = now - frequencyInfo.timestampEpoch;

			if (ageMs < 0L
					|| ageMs > BandOpportunityResolver
					.RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS) {
				continue;
			}

			if (frequencyInfo.timestampEpoch > newestTimestamp) {
				newestTimestamp = frequencyInfo.timestampEpoch;
				newestFrequencyKHz =
						frequencyInfo.frequency * 1000.0;
			}
		}

		return newestFrequencyKHz;
	}

	/**
	 * Returns the own QRG belonging to the chat category in which the target
	 * station was selected.
	 */
	private String resolveOwnQrgForSkedCategory(
			ChatCategory targetCategory) {

		if (sameChatCategory(
				targetCategory,
				getChatCategorySecondChat())) {

			return chatPreferences
					.getMYQRGSecondCat()
					.get();
		}

		return chatPreferences
				.getMYQRGFirstCat()
				.get();
	}

	private boolean sameChatCategory(ChatCategory first,
	                                 ChatCategory second) {

		return first != null
				&& second != null
				&& first.getCategoryNumber()
				== second.getCategoryNumber();
	}

	/**
	 * Parses the QRG formats used by KST4Contest and Win-Test.
	 *
	 * <p>Examples: 144.300, 144.300.03, 144300 and 144300.0.</p>
	 */
	private Double parseSkedFrequencyKHz(String value,
	                                     Band expectedBand) {

		if (value == null
				|| value.isBlank()
				|| expectedBand == null) {
			return null;
		}

		String normalized = value
				.trim()
				.replace(',', '.')
				.replaceAll("\\s+", "");

		try {
			java.util.regex.Matcher groupedFrequency =
					java.util.regex.Pattern.compile(
							"^(\\d{2,5})\\.(\\d{3})(?:\\.(\\d{1,2}))?$"
					).matcher(normalized);

			if (groupedFrequency.matches()) {
				double frequencyKHz =
						Integer.parseInt(groupedFrequency.group(1))
								* 1000.0
								+ Integer.parseInt(
								groupedFrequency.group(2)
						);

				String subKHzPart =
						groupedFrequency.group(3);

				if (subKHzPart != null) {
					double subKHz =
							Integer.parseInt(subKHzPart);

					frequencyKHz +=
							subKHzPart.length() == 1
									? subKHz / 10.0
									: subKHz / 100.0;
				}

				return expectedBand.isPlausible(
						frequencyKHz / 1000.0
				) ? frequencyKHz : null;
			}

			double numericValue =
					Double.parseDouble(normalized);

			if (expectedBand.isPlausible(numericValue)) {
				return numericValue * 1000.0;
			}

			if (expectedBand.isPlausible(
					numericValue / 1000.0)) {
				return numericValue;
			}

		} catch (NumberFormatException ignored) {
			// Invalid or unsupported QRG format.
		}

		return null;
	}

	/**
	 * Removes KST dash suffixes while retaining ordinary amateur-radio slash
	 * notation such as /P, /M or a country prefix.
	 *
	 * <p>Examples:
	 * DN9APW-2 -> DN9APW
	 * EA5/G8MBI/P-70 -> EA5/G8MBI/P
	 * DN9APW-2/P -> DN9APW/P</p>
	 */
	private String toWinTestSkedCallsign(String chatCallsign) {
		if (chatCallsign == null || chatCallsign.isBlank()) {
			return null;
		}

		String normalized =
				chatCallsign.trim().toUpperCase(Locale.ROOT);

		return normalized.replaceAll(
				"-[^/]*(?=/|$)",
				""
		);
	}

	private void reportSkippedWinTestSked(ContestSked sked,
	                                      String reason) {

		String target =
				sked == null
						? "unknown station"
						: sked.getTargetChatCallsign();

		String message =
				"Win-Test sked not sent for "
						+ target
						+ ": "
						+ reason
						+ ". The internal KST4Contest sked remains active.";

		System.out.println(
				"[ChatController] " + message
		);

		onThreadStatus(
				"WT-SkedSend",
				new ThreadStateMessage(
						"WT-SkedSend",
						false,
						message,
						true
				)
		);
	}

	private String resolveSkedTargetLocator(
			String targetCallsignRaw) {

		if (targetCallsignRaw == null
				|| targetCallsignRaw.isBlank()) {
			return null;
		}

		for (ChatMember member
				: findActiveChatMembersByRawCall(
				targetCallsignRaw)) {

			String locator = member.getQra();

			if (locator != null && !locator.isBlank()) {
				return locator
						.trim()
						.toUpperCase(Locale.ROOT);
			}
		}

		return null;
	}

	public StationMetricsService getStationMetricsService() {
		return stationMetricsService;
	}

	public SkedReminderService getSkedReminderService() {
		return skedReminderService;
	}

	/**
	 * saves the last recognized chat category for a chatmember, for example when we seen a message
	 * @param callSignRaw
	 * @param category
	 */
	public void rememberLastInboundCategory(String callSignRaw, ChatCategory category) {
		if (callSignRaw == null || category == null) return;
		lastInboundCategoryByCallSignRaw.put(callSignRaw.trim().toUpperCase(), category);
	}

	public Map<String, ChatCategory> snapshotLastInboundCategoryMap() {
		return new HashMap<>(lastInboundCategoryByCallSignRaw);
	}

//	public List<ChatMember> snapshotChatMembers() {
//		synchronized (getLst_chatMemberList()) {
//			return new ArrayList<>(getLst_chatMemberList());
//		}
//	}

	public List<ChatMember> snapshotChatMembers() {
		return new ArrayList<>(activeChatMembersByCallAndCategory.values());
	}

	public List<ContestSked> snapshotActiveSkeds() {
		return new ArrayList<>(activeSkeds.snapshot());
	}

	public void requestRemoveExpiredSkeds(long nowEpochMs) {
		uiDispatcher.runOnUi(() -> {
			/*
			 * Only mutate when something really expired. An unconditional mutate would
			 * wake every roster listener on each cycle of the periodic score service.
			 */
			boolean anyExpired = activeSkeds.snapshot().stream()
					.anyMatch(sked -> (nowEpochMs - sked.getSkedTimeEpoch()) > 300_000);
			if (!anyExpired) {
				return;
			}
			activeSkeds.mutate(list ->
					list.removeIf(sked -> (nowEpochMs - sked.getSkedTimeEpoch()) > 300_000));
		});
	}

	private String userName;
	private String password;
	private String showedName;
	private String qra;

	private String chatState;

//	private String hostname = "109.90.0.130";
    private String hostname;
//	private String praktiKSTVersion = "praktiKST 1.0";
	private String praktiKSTVersionInfo = "2022-10 - 2022-12\ndeveloped by DO5AMF, Marc and DN9APW, Philipp Wagner\nContact: praktimarc@gmail.com\nDonations via paypal are welcome";

	private int port = 23001; // kst4contest.test 4 23001 //TODO: auslagern in Chatprefs
	private ReadUDPbyUCXMessageThread readUDPbyUCXThread;
    private ReadUDPByWintestThread readUDPByWintestThread;

	/**
	 * Shared resolver for the Win-Test broadcast address. Win-Test only reacts
	 * to broadcasts, so both the log synchronization and the SKED handover have
	 * to reach the network the station was actually heard on.
	 */
	private final WinTestNetworkAddressResolver winTestAddressResolver =
			new WinTestNetworkAddressResolver();

	/**
	 * @return resolver for outgoing Win-Test packets
	 */
	public WinTestNetworkAddressResolver getWinTestAddressResolver() {
		return winTestAddressResolver;
	}
	private WriteThread writeThread;
	private ReadThread readThread;
	private InputReaderThread consoleReader;
	private ChatMember ownChatMemberObject; // Todo: set at startup
	private ChatController chatController;
	private MessageBusManagementThread messageProcessor;
	private ReadUDPbyAirScoutMessageThread airScoutUDPReaderThread;
	private DXClusterThreadPooledServer dxClusterServer;

	private PlayAudioUtils playAudioUtils = new PlayAudioUtils();

	public PlayAudioUtils getPlayAudioUtils() {
		return playAudioUtils;
	}


	private TimerTask userActualizationTask;

	private TimerTask keepAliveMessageSenderTask;

	private LinkedBlockingQueue<ChatMessage> messageRXBus; // Queue in which all Chatmessages are buffered, sources are
															// read- and write-thread
	private LinkedBlockingQueue<ChatMessage> messageTXBus; // Queue in which all Chatmessages are buffered, sources are
															// read- and write-thread
	private String observedSendThisMessageString;

	private DBController dbHandler;

	private ReachabilityService reachabilityService;
	private final WorkedGrossFieldCache workedGrossFieldCache = new WorkedGrossFieldCache();


	private Socket socket;
	private ServerSocket cluster_telnetServerSocket; // socket that accepts telnet client connects (cluster client)
//	private ServerSocketChannel cluster_telnetServerSocketChannel;


	private Timer userActualizationtimer;

	private Timer keepAliveTimer;

	private Timer beaconTimer;

	private Timer ASQueryTimer;

	private Timer socketCheckTimer;

	// ******All abstract types below here are used by the messageprocessor!
	// ***************

	private final SimpleRoster<ChatMessage> lst_globalChatMessageList = new SimpleRoster<>(); //All chatmessages will be put in there, later create filtered message lists

	/*
	 * The four message views used to be FilteredList views on the global message
	 * list. They are now stored predicates: the derived list is computed on demand
	 * from lst_globalChatMessageList.snapshot(). A null predicate lets everything
	 * through, exactly like FilteredList.setPredicate(null) did.
	 */
	private volatile Predicate<ChatMessage> toAllMessageFilter; // directed to all (beacon)
	private volatile Predicate<ChatMessage> toMeMessageFilter; // directed to my call
	private volatile Predicate<ChatMessage> toOtherMessageFilter; // directed to a call but not mine
	private volatile Predicate<ChatMessage> selectedCallSignInfoFilter;

//    private SimpleRoster<String> lstNotify_QSOSniffer_sniffedCallSignList = new SimpleRoster<>();
private SimpleRoster<String>
		lstNotify_QSOSniffer_sniffedCallSignList;
	/**
	 * we do some trick here with the chatmemberlist to not make it neccessary to change all boolean properties if the
	 * chatmember object to observables. We trigger the list for changes on an object which we change whenever a list
	 * update will be neccessary to process (important for correct lifetime filtering!)
	 */
//	private ObservableList<ChatMember> chatMemberList = FXCollections.observableArrayList(workedInfoChange -> new Observable[] {workedInfoChange.workedInfoChangeFireListEventTriggerProperty()}); // List of active stations
																								// in chat
	/**
	 * The canonical station list. SimpleRoster serialises its own writes, so the
	 * former FXCollections.synchronizedObservableList wrapper is gone and
	 * lst_chatMemberList is this very field.
	 */
	private final SimpleRoster<ChatMember> chatMemberList = new SimpleRoster<>(); // List of active stations

	private final SimpleRoster<Predicate<ChatMember>> lst_chatMemberListFilterPredicates = new SimpleRoster<>();
	private final SimpleRoster<ClusterMessage> lst_clusterMemberList = new SimpleRoster<>();

	/**
	 * The order the station list is presented in. Fed by the TableView column
	 * headers, which used to drive the SortedList comparator directly. A null
	 * comparator keeps the incoming order, exactly like an unset SortedList
	 * comparator did.
	 */
	private volatile Comparator<ChatMember> chatMemberOrder;

	/*
	 * Thread-safe active-member model.
	 *
	 * MessageBusManagementThread must not use the UI-bound station list as its
	 * primary data model. Otherwise every add/remove wakes the mirror and the
	 * TableView selection listeners on the MessageBus thread, and JavaFX can throw
	 * "Not on FX application thread" or internal listener NPEs.
	 *
	 * This map is the worker-thread safe source of truth. The roster below remains
	 * a UI mirror and is mutated only through the UI dispatcher.
	 */
	private final java.util.concurrent.ConcurrentMap<String, ChatMember> activeChatMembersByCallAndCategory =
			new java.util.concurrent.ConcurrentHashMap<>();

	/*
	 * Message table update buffers.
	 *
	 * Do not write directly to lst_globalChatMessageList from worker threads.
	 * Use publishChatMessage(...) instead.
	 *
	 * The actual roster mutation is batched and executed on the user-interface
	 * thread. The visible list order remains newest-first.
	 */
	private final Object pendingChatMessagesLock = new Object();
	private final List<ChatMessage> pendingChatMessages = new ArrayList<>();
	private boolean chatMessageFlushScheduled = false;

	private static final int RECENT_INBOUND_MESSAGE_KEYS_MAX = 5_000;
	private final LinkedHashMap<String, Boolean> recentInboundMessageKeys =
			new LinkedHashMap<>();

	/*
	 * Same idea for DXCluster messages.
	 */
	private final Object pendingClusterMessagesLock = new Object();
	private final List<ClusterMessage> pendingClusterMessages = new ArrayList<>();
	private boolean clusterMessageFlushScheduled = false;

	private final SimpleRoster<ChatMember> lst_DBBasedWkdCallSignList = new SimpleRoster<>();

//	private HashMap<String, ChatMember> map_ucxLogInfoWorkedCalls = new HashMap<String, ChatMember>(); //Destination of ucx-log worked-messages

	// ******************************************************************************************************************************************


	/**
	 * Executes UI-bound work on the JavaFX application thread.
	 *
	 * Worker threads may call this safely. If the caller is already on the FX
	 * thread, the action is executed immediately to preserve ordering.
	 */
	private void runOnFxThread(Runnable action) {
		if (action == null) {
			return;
		}

		if (uiDispatcher.isUiThread()) {
			action.run();
		} else {
			uiDispatcher.runOnUi(action);
		}
	}

	/**
	 * Builds the unique identity key for one active ON4KST login.
	 *
	 * <p>The complete callsign must be preserved here. Callsigns such as
	 * {@code 9A0BB-2}, {@code 9A0BB-70} and {@code 9A0BB-144} represent different
	 * active chat sessions and must therefore remain separate objects, even when
	 * they are logged into the same chat category.</p>
	 *
	 * <p>The normalized base callsign ({@code callSignRaw}) is deliberately not
	 * used for this key. It remains the common station key for worked, band and
	 * NOT-QRV information.</p>
	 */
	private String buildActiveChatMemberKey(String callSign, ChatCategory category) {

		if (callSign == null) {
			return null;
		}

		String fullCallSign = callSign.trim().toUpperCase(Locale.ROOT);

		if (fullCallSign.isBlank()) {
			return null;
		}

		int categoryNumber =
				category == null ? -1 : category.getCategoryNumber();

		return fullCallSign + "|" + categoryNumber;
	}

	private String buildActiveChatMemberKey(ChatMember member) {

		if (member == null) {
			return null;
		}

		return buildActiveChatMemberKey(
				member.getCallSign(),
				member.getChatCategory()
		);
	}


	/**
	 * Uses one unambiguous explicit QRG from the current station name as the
	 * initial compatibility frequency.
	 *
	 * <p>Multiple explicit QRGs are valid band evidence, but there is no safe
	 * assumption which one is the current run frequency. Therefore the legacy
	 * frequency property is initialized only when exactly one explicit QRG exists.</p>
	 */
	/* package */ void initializeFrequencyFromStationNameIfUnambiguous(
			ChatMember member
	) {
		if (member == null) {
			return;
		}

		List<FrequencyTextParser.DetectedFrequency> detectedFrequencies =
				FrequencyTextParser.findExplicitFrequencies(
						member.getName()
				);

		if (detectedFrequencies.size() != 1) {
			return;
		}

		member.initializeFrequencyIfEmpty(
				detectedFrequencies
						.get(0)
						.getFrequencyMHz()
		);
	}

	/**
	 * Adds or replaces an active chat member in the worker-thread model and mirrors
	 * that change to the JavaFX list. This is the only supported path for ON4KST
	 * user-enter events.
	 */
	public void addOrUpdateActiveChatMember(ChatMember member) {
		String key = buildActiveChatMemberKey(member);
		if (key == null) {
			return;
		}

		initializeFrequencyFromStationNameIfUnambiguous(member);

		activeChatMembersByCallAndCategory.put(key, member);

		runOnFxThread(() -> chatMemberList.mutate(list -> {
			int existingIndex = findChatMemberIndexInList(list, key);
			if (existingIndex >= 0) {
				list.set(existingIndex, member);
			} else {
				list.add(member);
			}
		}));
	}

	/**
	 * Removes an active chat member from the worker-thread model and from the UI
	 * mirror. Removal from the roster is always performed on the user-interface
	 * thread because the roster is mirrored into the TableView.
	 */
	public boolean removeActiveChatMember(ChatMember member) {
		String key = buildActiveChatMemberKey(member);
		if (key == null) {
			return false;
		}

		ChatMember removedMember = activeChatMembersByCallAndCategory.remove(key);
		runOnFxThread(() -> chatMemberList.mutate(list ->
				list.removeIf(currentMember -> key.equals(buildActiveChatMemberKey(currentMember)))));
		return removedMember != null;
	}

	/**
	 * Clears the active-member model and the JavaFX UI mirror. Use this instead of
	 * getLst_chatMemberList() mutations from timers or worker threads.
	 */
	public void clearActiveChatMembers() {
		activeChatMembersByCallAndCategory.clear();
		runOnFxThread(chatMemberList::clear);
	}

	/**
	 * Atomically replaces one category after its terminating UE frame arrived.
	 * Partial UA0 snapshots are never exposed to the TableView.
	 *
	 * @param connectionSessionId session that produced the completed snapshot
	 * @param category chat category whose members are being replaced
	 * @param completeMembers validated members staged before the UE terminator
	 */
	public void replaceActiveChatMembersForCategory(
			long connectionSessionId,
			ChatCategory category,
			Collection<ChatMember> completeMembers
	) {
		if (category == null
				|| !on4KstConnectionManager.isActiveSession(connectionSessionId)) {
			return;
		}

		int categoryNumber = category.getCategoryNumber();
		List<ChatMember> safeMembers = completeMembers == null
				? List.of() : new ArrayList<>(completeMembers);
		Map<String, ChatMember> workedDataFromDatabase =
				loadWorkedStateForInitialUserList(safeMembers);
		for (ChatMember member : safeMembers) {
			initializeFrequencyFromStationNameIfUnambiguous(member);
		}

		activeChatMembersByCallAndCategory.entrySet().removeIf(entry -> {
			ChatMember member = entry.getValue();
			return member != null && member.getChatCategory() != null
					&& member.getChatCategory().getCategoryNumber() == categoryNumber;
		});
		for (ChatMember member : safeMembers) {
			String key = buildActiveChatMemberKey(member);
			if (key != null) {
				activeChatMembersByCallAndCategory.put(key, member);
			}
		}

		runOnFxThread(() -> {
			if (!on4KstConnectionManager.isActiveSession(connectionSessionId)) {
				return;
			}
			/*
			 * One batch: replacing a whole category must wake the mirror once, not
			 * once per station. An ON4KST user list can hold several thousand of them.
			 */
			chatMemberList.mutate(list -> {
				list.removeIf(member -> member != null
						&& member.getChatCategory() != null
						&& member.getChatCategory().getCategoryNumber() == categoryNumber);
				list.addAll(safeMembers);
			});
			if (workedDataFromDatabase != null) {
				setLst_DBBasedWkdCallSignList(
						workedDataFromDatabase.values());
			}
			fireUserListUpdate("Complete ON4KST user list received");
		});
	}

	/**
	 * Loads one database snapshot for a completed initial ON4KST user list and
	 * applies it before those members are published to the active model or UI.
	 * Every callsign variant receives the state stored for its normalized base
	 * callsign.
	 *
	 * @param initialMembers completed members of one chat category
	 * @return loaded snapshot, or {@code null} when the database read failed
	 */
	/* package */ Map<String, ChatMember> loadWorkedStateForInitialUserList(
			Collection<ChatMember> initialMembers
	) {
		if (dbHandler == null) {
			LOGGER.warning(
					"Cannot load Worked state for initial ON4KST user list: "
							+ "database is not initialized");
			return null;
		}

		try {
			Map<String, ChatMember> workedDataFromDatabase =
					dbHandler.fetchChatMemberWkdDataFromDB();
			applyWorkedAndQrvStateFromDatabase(
					initialMembers, workedDataFromDatabase);
			return workedDataFromDatabase;
		} catch (SQLException | RuntimeException exception) {
			LOGGER.log(
					Level.WARNING,
					"Could not load Worked state for completed initial ON4KST user list",
					exception);
			return null;
		}
	}

	/**
	 * Resolves a member from the thread-safe active model. This avoids reading the
	 * TableView backing list from MessageBusManagementThread.
	 */
	public ChatMember findActiveChatMember(ChatMember lookForThis) {
		String key = buildActiveChatMemberKey(lookForThis);
		return key == null ? null : activeChatMembersByCallAndCategory.get(key);
	}

	public ChatMember findActiveChatMember(String callSign, ChatCategory category) {
		String key = buildActiveChatMemberKey(callSign, category);
		return key == null ? null : activeChatMembersByCallAndCategory.get(key);
	}

	public int getActiveChatMemberCount() {
		return activeChatMembersByCallAndCategory.size();
	}

	/**
	 * Returns all active category variants of a callsign. This is used when a QRG or
	 * band hint should be propagated from one category instance to the same station
	 * in another category.
	 */
	public List<ChatMember> findActiveChatMembersByRawCall(String callSignRaw) {
		String normalizedCallsign = ChatMember.normalizeCallSignToBaseCallSign(callSignRaw);
		if (normalizedCallsign == null || normalizedCallsign.isBlank()) {
			return Collections.emptyList();
		}

		String normalizedKeyPart = normalizedCallsign.trim().toUpperCase(Locale.ROOT);
		List<ChatMember> matchingMembers = new ArrayList<>();

		for (ChatMember member : activeChatMembersByCallAndCategory.values()) {
			if (member == null) {
				continue;
			}

			String memberCall = member.getCallSignRaw() != null ? member.getCallSignRaw() : member.getCallSign();
			String normalizedMemberCall = ChatMember.normalizeCallSignToBaseCallSign(memberCall);
			if (normalizedMemberCall != null && normalizedMemberCall.equalsIgnoreCase(normalizedKeyPart)) {
				matchingMembers.add(member);
			}
		}

		return matchingMembers;
	}

	/**
	 * Applies a QSO received from an external logger to all active callsign
	 * variants. Active member objects back JavaFX views, so their state is changed
	 * only on the JavaFX Application Thread. The band-upgrade hint is evaluated
	 * afterwards and therefore sees the new Worked state.
	 *
	 * @param loggedQso validated external QSO
	 */
	public void applyExternalLoggedQso(ExternalLoggedQso loggedQso) {
		if (loggedQso == null) {
			return;
		}

		runOnFxThread(() -> {
			int updatedMembers = markExternalLoggedQsoMembers(
					activeChatMembersByCallAndCategory.values(), loggedQso);
			if (updatedMembers == 0) {
				return;
			}

			fireUserListUpdate("External logger Worked state updated");
			try {
				onExternalLogEntryReceived(loggedQso.getCallSign());
			} catch (Exception exception) {
				LOGGER.log(Level.WARNING,
						"Band-upgrade hint failed after external logger update for "
								+ loggedQso.getCallSign(), exception);
			}
		});
	}

	static int markExternalLoggedQsoMembers(
			Collection<ChatMember> members,
			ExternalLoggedQso loggedQso
	) {
		if (members == null || loggedQso == null) {
			return 0;
		}

		String workedBaseCall = ChatMember.normalizeCallSignToBaseCallSign(
				loggedQso.getCallSign());
		if (workedBaseCall == null || workedBaseCall.isBlank()) {
			return 0;
		}

		int updatedMembers = 0;
		for (ChatMember member : members) {
			if (member == null) {
				continue;
			}

			String memberCall = member.getCallSignRaw() != null
					? member.getCallSignRaw() : member.getCallSign();
			String memberBaseCall = ChatMember.normalizeCallSignToBaseCallSign(memberCall);
			if (memberBaseCall != null && memberBaseCall.equalsIgnoreCase(workedBaseCall)) {
				loggedQso.applyToActiveMember(member);
				updatedMembers++;
			}
		}
		return updatedMembers;
	}

	/**
	 * Applies the global Worked state from the Simplelogfile to every active
	 * callsign variant with the same base callsign. UI-backed state is changed only
	 * on the JavaFX Application Thread.
	 *
	 * @param workedBaseCalls normalized callsigns detected in the selected file
	 */
	public void applySimpleLogWorkedBaseCalls(Set<String> workedBaseCalls) {
		Set<String> normalizedWorkedBaseCalls = normalizeWorkedBaseCalls(workedBaseCalls);
		if (normalizedWorkedBaseCalls.isEmpty()) {
			return;
		}

		runOnFxThread(() -> {
			int changedMembers = markSimpleLogWorkedMembers(
					activeChatMembersByCallAndCategory.values(), normalizedWorkedBaseCalls);
			int changedClusterMessages = markSimpleLogWorkedClusterMessages(
					lst_clusterMemberList.snapshot(), normalizedWorkedBaseCalls);

			if (changedMembers > 0) {
				fireUserListUpdate("Simplelogfile Worked status updated");
			}

			LOGGER.log(Level.FINE,
					"Simplelogfile marked {0} active members and {1} cluster messages as worked.",
					new Object[] { changedMembers, changedClusterMessages });
		});
	}

	static int markSimpleLogWorkedMembers(
			Collection<ChatMember> members,
			Set<String> workedBaseCalls
	) {
		if (members == null || workedBaseCalls == null || workedBaseCalls.isEmpty()) {
			return 0;
		}

		int changed = 0;
		for (ChatMember member : members) {
			if (member == null || member.isWorked()) {
				continue;
			}

			String memberCall = member.getCallSignRaw() != null
					? member.getCallSignRaw() : member.getCallSign();
			String baseCall = ChatMember.normalizeCallSignToBaseCallSign(memberCall);
			if (baseCall != null && workedBaseCalls.contains(baseCall.toUpperCase(Locale.ROOT))) {
				member.setWorked(true);
				changed++;
			}
		}
		return changed;
	}

	static int markSimpleLogWorkedClusterMessages(
			Collection<ClusterMessage> clusterMessages,
			Set<String> workedBaseCalls
	) {
		if (clusterMessages == null || workedBaseCalls == null || workedBaseCalls.isEmpty()) {
			return 0;
		}

		int changed = 0;
		for (ClusterMessage clusterMessage : clusterMessages) {
			if (clusterMessage == null || clusterMessage.isReceiverWkd()
					|| clusterMessage.getReceiver() == null) {
				continue;
			}

			ChatMember receiver = clusterMessage.getReceiver();
			String receiverCall = receiver.getCallSignRaw() != null
					? receiver.getCallSignRaw() : receiver.getCallSign();
			String baseCall = ChatMember.normalizeCallSignToBaseCallSign(receiverCall);
			if (baseCall != null && workedBaseCalls.contains(baseCall.toUpperCase(Locale.ROOT))) {
				clusterMessage.setReceiverWkd(true);
				changed++;
			}
		}
		return changed;
	}

	private static Set<String> normalizeWorkedBaseCalls(Set<String> workedBaseCalls) {
		if (workedBaseCalls == null || workedBaseCalls.isEmpty()) {
			return Set.of();
		}

		Set<String> normalizedCalls = new HashSet<>();
		for (String callSign : workedBaseCalls) {
			String baseCall = ChatMember.normalizeCallSignToBaseCallSign(callSign);
			if (baseCall != null && !baseCall.isBlank()) {
				normalizedCalls.add(baseCall.toUpperCase(Locale.ROOT));
			}
		}
		return Set.copyOf(normalizedCalls);
	}

	/**
	 * Notifies the UI after a missing Simplelogfile was created successfully.
	 *
	 * @param filePath absolute path of the new file
	 */
	public void notifySimpleLogFileCreated(Path filePath) {
		if (filePath == null || statusListener == null) {
			return;
		}

		runOnFxThread(() -> statusListener.onSimpleLogFileCreated(filePath));
	}

	/**
	 * Copies the band-specific NOT-QRV state to every active category variant of the
	 * same base callsign. The database already uses callSignRaw as its key; applying
	 * the state to the runtime model immediately prevents category-dependent B+,
	 * filter, map and score results before the next database refresh.
	 *
	 * @param sourceMember member whose current NOT-QRV checkboxes are authoritative
	 */
	public void propagateNotQrvStateToActiveMembers(ChatMember sourceMember) {
		if (sourceMember == null) {
			return;
		}

		String rawCall = sourceMember.getCallSignRaw() != null
				? sourceMember.getCallSignRaw()
				: sourceMember.getCallSign();

		List<ChatMember> variants = findActiveChatMembersByRawCall(rawCall);
		if (variants.isEmpty()) {
			variants = List.of(sourceMember);
		}

		for (ChatMember target : variants) {
			if (target == null) {
				continue;
			}

			target.setQrv50(sourceMember.isQrv50());
			target.setQrv70(sourceMember.isQrv70());
			target.setQrv144(sourceMember.isQrv144());
			target.setQrv432(sourceMember.isQrv432());
			target.setQrv1240(sourceMember.isQrv1240());
			target.setQrv2300(sourceMember.isQrv2300());
			target.setQrv3400(sourceMember.isQrv3400());
			target.setQrv5600(sourceMember.isQrv5600());
			target.setQrv10G(sourceMember.isQrv10G());
		}

		if (scoreService != null) {
			scoreService.requestRecompute("NOT-QRV state changed");
		}

		fireUserListUpdate("NOT-QRV state propagated to callsign variants");
	}

	/**
	 * Updates locator and derived direction values in the active model. The UI list
	 * contains the same member object, but the user-list refresh is still triggered
	 * on the JavaFX thread so TableView/map derived displays can repaint safely.
	 */
	public boolean updateActiveChatMemberLocator(ChatMember lookupMember, String newLocator) {
		ChatMember activeMember = findActiveChatMember(lookupMember);
		if (activeMember == null) {
			return false;
		}

		activeMember.setQra(newLocator);
		activeMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(chatPreferences.getStn_loginLocatorMainCat(), newLocator));
		activeMember.setQTFdirection(new Location(chatPreferences.getStn_loginLocatorMainCat()).getBearing(new Location(newLocator)));
		fireUserListUpdate("Locator changed");
		return true;
	}

	public boolean updateActiveChatMemberState(ChatMember lookupMember, int newState) {
		ChatMember activeMember = findActiveChatMember(lookupMember);
		if (activeMember == null) {
			return false;
		}

		activeMember.setState(newState);
		fireUserListUpdate("User state changed");
		return true;
	}

	/**
	 * Applies a UM3-style user-info update only to an already active chat member.
	 *
	 * ON4KST may send UM3 messages for stations that are not logged into the
	 * current channel, for example when a user changes locator/profile data on the
	 * website. Such stations must not be added to the visible chat member list,
	 * because they cannot be addressed in the channel. Their full information will
	 * be delivered again with UA0/UA5/UA2 when they actually join.
	 *
	 * @return true if an active member was found and updated, false if the UM3
	 *         information was intentionally ignored.
	 */
	public boolean updateActiveChatMemberInfoIfPresent(ChatMember updatedMember) {
		String key = buildActiveChatMemberKey(updatedMember);
		if (key == null) {
			return false;
		}

		ChatMember activeMember = activeChatMembersByCallAndCategory.get(key);
		if (activeMember == null) {
			return false;
		}

		activeMember.setName(updatedMember.getName());
		initializeFrequencyFromStationNameIfUnambiguous(
				activeMember
		);
		activeMember.setQra(updatedMember.getQra());
		activeMember.setState(updatedMember.getState());
		activeMember.setLastActivity(updatedMember.getLastActivity());
		activeMember.setActivityTimeLastInEpoch(updatedMember.getActivityTimeLastInEpoch());
		activeMember.setQrb(updatedMember.getQrb());
		activeMember.setQTFdirection(updatedMember.getQTFdirection());
		fireUserListUpdate("User info updated");
		return true;
	}

	/**
	 * Backward-compatible wrapper for older call sites. Despite the historic name,
	 * this method no longer adds unknown UM3 users to the active member model.
	 */
	public boolean updateOrAddActiveChatMemberInfo(ChatMember updatedMember) {
		return updateActiveChatMemberInfoIfPresent(updatedMember);
	}

	/**
	 * Stores a newly detected QRG/band on all active category variants of a station.
	 * The old StringProperty is still set for compatibility with existing TableView
	 * columns and DXCluster code.
	 */
	public void applyDetectedFrequencyToActiveMembers(ChatMember sender, Band detectedBand, double detectedFrequencyMhz) {
		if (sender == null || detectedBand == null || detectedFrequencyMhz <= 0) {
			return;
		}

		String displayFrequency = String.valueOf(detectedFrequencyMhz);
		String rawCall = sender.getCallSignRaw() != null ? sender.getCallSignRaw() : sender.getCallSign();

		for (ChatMember member : findActiveChatMembersByRawCall(rawCall)) {
			if (member == null) {
				continue;
			}
			member.addKnownFrequency(detectedBand, detectedFrequencyMhz);
			member.setFrequency(new SimpleValue<>(displayFrequency));
		}

		// Also cover dummy senders that are not in the active model, e.g. [n/a] senders.
		sender.addKnownFrequency(detectedBand, detectedFrequencyMhz);
		sender.setFrequency(new SimpleValue<>(displayFrequency));
		fireUserListUpdate("Frequency detected");
	}

	private int findChatMemberIndexInList(List<ChatMember> list, String key) {
		if (key == null) {
			return -1;
		}

		for (int i = 0; i < list.size(); i++) {
			if (key.equals(buildActiveChatMemberKey(list.get(i)))) {
				return i;
			}
		}
		return -1;
	}




	/**
	 * checks if the callsign-String of a given chatmember instance and a given list
	 * instance is in the list (multiple entries are possible to find by this method! <br/>
	 * If yes, returns an Array of int with the list indexes <b>if not, returns empty array</b>
	 * <br/>
	 * <br/>Also gives back indexes for callsign-70 or callsign-2 etc.<br/>
	 *
	 * @param lookForThis
	 * @return int[]
	 */
	public ArrayList<Integer> checkListForChatMemberIndexesByCallSign(ChatMember lookForThis) {

		ArrayList<Integer> resultingIndexes = new ArrayList<Integer>();

		if (lookForThis == null) {

//			System.out.println("[ChatCtrl] ERROR: null Value for Chatmember detected! Member cannot be in the list!");
			return resultingIndexes;

		} else if (lookForThis.getCallSignRaw() == null) {
			System.out.println("[ChatCtrl] ERROR: null Value in Callsign detected! Member cannot be in the list!");
			return resultingIndexes;
		}

		List<ChatMember> currentMembers = chatMemberList.snapshot();

		for (Iterator iterator = currentMembers.iterator(); iterator.hasNext();) {
			ChatMember chatMember = (ChatMember) iterator.next();
			if (chatMember.getCallSignRaw().equals(lookForThis.getCallSignRaw())) { //Change for stations with -2 or -70 in logincallsign
//				System.out
//						.println("chtctrlr: Found raw " + chatMember.getCallSignRaw() + " //  " + lookForThis.getCallSign());

				resultingIndexes.add(currentMembers.indexOf(chatMember));

			} else {

			}
		}
		return resultingIndexes;

	}


	/**
	 * Notifies the UI that station-list derived values have changed.
	 *
	 * <p>This method may be called from parser threads, UDP listener threads and
	 * reachability worker threads. Therefore the listener is always invoked on the
	 * JavaFX application thread.</p>
	 *
	 * This overload requests the functional refresh without producing a periodic
	 * diagnostic reason.
	 */
	public void fireUserListUpdate() {
		if (statusListener == null) {
			return;
		}

		if (uiDispatcher.isUiThread()) {
			statusListener.onUserListUpdated();
		} else {
			uiDispatcher.runOnUi(statusListener::onUserListUpdated);
		}
	}

	/**
	 * Notifies the UI that station-list derived values have changed.
	 *
	 * @param reason short debug reason for the UI log
	 */
	public void fireUserListUpdate(String reason) {
		if (statusListener == null) {
			return;
		}

		if (uiDispatcher.isUiThread()) {
			statusListener.onUserListUpdated(reason);
		} else {
			uiDispatcher.runOnUi(() -> statusListener.onUserListUpdated(reason));
		}
	}

//	/**
//	 * checks if the callsign-String of a given chatmember instance and a given list
//	 * instance is in the list. If yes, returns the index in the List, <b>if not,
//	 * returns -1.</b>
//	 *
//	 * @param lookForThis
//	 * @return Integer (index), -1 for not found
//	 */
//	public int checkListForChatMemberIndexByCallSign(ChatMember lookForThis) {
//
//		if (lookForThis == null) {
//
////			System.out.println("[ChatCtrl] ERROR: null Value for Chatmember detected! Member cannot be in the list!");
//			return -1;
//		} else if (lookForThis.getCallSign() == null) {
//			System.out.println("[ChatCtrl] ERROR: null Value in Callsign detected! Member cannot be in the list!");
//			return -1;
//		}
//
//		for (Iterator iterator = lst_chatMemberList.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
////			if (chatMember.getCallSign().equals(lookForThis.getCallSign())) {
//			if (chatMember.getCallSignRaw().equals(lookForThis.getCallSignRaw())) { //TODO: Change for stations with -2 or -70 in logincallsign
////				System.out
////						.println("chtctrlr: Found raw " + chatMember.getCallSignRaw() + " //  " + lookForThis.getCallSign());
//
//				return lst_chatMemberList.indexOf(chatMember);
//			} else {
//
//			}
//		}
		/**
		 * At this point we know, the callsign is not active in the chat.
		 */
//		System.out
//				.println("[ChatCtrl, ERROR:] ChecklistForChatMemberIndexByCallsign: " + lookForThis.getCallSign() + "\n" + "List: ");
//		for (Iterator iterator = lst_chatMemberList.iterator(); iterator.hasNext();) {
//			ChatMember chatMember = (ChatMember) iterator.next();
//			System.out.println(lst_chatMemberList.indexOf(lookForThis) + ": " + chatMember.getCallSign());
//		}

//		return -1;

//	}

	public ObservableRoster<ChatMessage> getLst_globalChatMessageList() {
		return lst_globalChatMessageList;
	}

	/**
	 * Applies the stored filter to the current message store.
	 *
	 * @param filter {@code null} lets every message through, exactly like
	 *               {@code FilteredList.setPredicate(null)} did
	 */
	private List<ChatMessage> filtered(Predicate<ChatMessage> filter) {
		if (filter == null) {
			return lst_globalChatMessageList.snapshot();
		}
		return lst_globalChatMessageList.snapshot().stream().filter(filter).toList();
	}

	public void setToAllMessageFilter(Predicate<ChatMessage> filter) {
		this.toAllMessageFilter = filter;
	}

	/** The public messages, newest first. */
	public List<ChatMessage> toAllMessages() {
		return filtered(toAllMessageFilter);
	}

	public void setToMeMessageFilter(Predicate<ChatMessage> filter) {
		this.toMeMessageFilter = filter;
	}

	/** The messages addressed to the operator, newest first. */
	public List<ChatMessage> toMeMessages() {
		return filtered(toMeMessageFilter);
	}

	public void setToOtherMessageFilter(Predicate<ChatMessage> filter) {
		this.toOtherMessageFilter = filter;
	}

	/** The messages between other stations, newest first. */
	public List<ChatMessage> toOtherMessages() {
		return filtered(toOtherMessageFilter);
	}

	public void setSelectedCallSignInfoFilter(Predicate<ChatMessage> filter) {
		this.selectedCallSignInfoFilter = filter;
	}

	/** The messages shown in the info window of the selected station. */
	public List<ChatMessage> selectedCallSignInfoMessages() {
		return filtered(selectedCallSignInfoFilter);
	}

	/**
	 * Adds a chat message to the UI message store.
	 *
	 * Important:
	 * - This method may be called from worker threads.
	 * - The roster is modified only on the user-interface thread.
	 * - The backing list remains newest-first.
	 * - Old messages are trimmed to avoid unlimited memory growth.
	 */
	public void publishChatMessage(ChatMessage message) {
		if (message == null) {
			return;
		}
		if (isDuplicateInboundMessage(message)) {
			return;
		}

		synchronized (pendingChatMessagesLock) {
			pendingChatMessages.add(message);

			if (chatMessageFlushScheduled) {
				return;
			}

			chatMessageFlushScheduled = true;
		}

		uiDispatcher.runOnUi(this::flushPendingChatMessagesToUi);
	}

	/**
	 * Suppresses the small replay overlap deliberately requested after a reconnect.
	 *
	 * <p>The manager asks for messages beginning one timestamp before the last known
	 * message so that a boundary message cannot be lost. This bounded key cache
	 * removes the expected duplicate without growing for the lifetime of the
	 * application.</p>
	 */
	private boolean isDuplicateInboundMessage(ChatMessage message) {
		String timestamp = message.getMessageGeneratedTime();
		if (timestamp == null || timestamp.isBlank()) {
			return false;
		}

		String sender = message.getSender() == null
				? "" : String.valueOf(message.getSender().getCallSign());
		String receiver = message.getReceiver() == null
				? "" : String.valueOf(message.getReceiver().getCallSign());
		int category = message.getChatCategory() == null
				? -1 : message.getChatCategory().getCategoryNumber();
		String key = category + "|" + timestamp + "|" + sender + "|"
				+ receiver + "|" + String.valueOf(message.getMessageText());

		synchronized (recentInboundMessageKeys) {
			if (recentInboundMessageKeys.containsKey(key)) {
				return true;
			}
			recentInboundMessageKeys.put(key, Boolean.TRUE);
			while (recentInboundMessageKeys.size()
					> RECENT_INBOUND_MESSAGE_KEYS_MAX) {
				Iterator<String> iterator = recentInboundMessageKeys.keySet().iterator();
				if (!iterator.hasNext()) {
					break;
				}
				iterator.next();
				iterator.remove();
			}
			return false;
		}
	}

	private void flushPendingChatMessagesToUi() {
		List<ChatMessage> batch;

		synchronized (pendingChatMessagesLock) {
			batch = new ArrayList<>(pendingChatMessages);
			pendingChatMessages.clear();
			chatMessageFlushScheduled = false;
		}

		if (batch.isEmpty()) {
			return;
		}

		/*
		 * pendingChatMessages is collected in arrival order:
		 * old -> new
		 *
		 * lst_globalChatMessageList must stay newest-first:
		 * new -> old
		 */
		Collections.reverse(batch);

		/*
		 * Insertion and trimming happen inside one mutate, so a whole batch wakes
		 * the listeners exactly once instead of once per message.
		 */
		lst_globalChatMessageList.mutate(list -> {
			list.addAll(0, batch);
			trimIfNeeded(
					list,
					ApplicationConstants.CHAT_MESSAGE_STORE_MAX_SIZE,
					ApplicationConstants.CHAT_MESSAGE_STORE_TRIM_TO_SIZE);
		});
	}

	/**
	 * Drops the oldest entries once the store grew past its limit.
	 *
	 * <p>List order is newest-first, so the old entries sit at the end.
	 *
	 * @param list       the list being mutated
	 * @param maxSize    size above which trimming starts
	 * @param trimToSize size the list is cut back to
	 */
	private static void trimIfNeeded(List<?> list, int maxSize, int trimToSize) {
		if (maxSize <= 0 || trimToSize <= 0 || trimToSize >= maxSize) {
			return;
		}

		int currentSize = list.size();

		if (currentSize <= maxSize) {
			return;
		}

		list.subList(trimToSize, currentSize).clear();
	}

	/**
	 * Adds a DXCluster message to the UI cluster message store.
	 *
	 * Same policy as for chat messages:
	 * - batched UI update
	 * - user-interface thread only for roster mutation
	 * - newest-first visible order
	 * - bounded list size
	 */
	public void publishClusterMessage(ClusterMessage message) {
		if (message == null) {
			return;
		}

		synchronized (pendingClusterMessagesLock) {
			pendingClusterMessages.add(message);

			if (clusterMessageFlushScheduled) {
				return;
			}

			clusterMessageFlushScheduled = true;
		}

		uiDispatcher.runOnUi(this::flushPendingClusterMessagesToUi);
	}

	private void flushPendingClusterMessagesToUi() {
		List<ClusterMessage> batch;

		synchronized (pendingClusterMessagesLock) {
			batch = new ArrayList<>(pendingClusterMessages);
			pendingClusterMessages.clear();
			clusterMessageFlushScheduled = false;
		}

		if (batch.isEmpty()) {
			return;
		}

		Collections.reverse(batch);

		lst_clusterMemberList.mutate(list -> {
			list.addAll(0, batch);
			trimIfNeeded(
					list,
					ApplicationConstants.CLUSTER_MESSAGE_STORE_MAX_SIZE,
					ApplicationConstants.CLUSTER_MESSAGE_STORE_TRIM_TO_SIZE);
		});
	}

	public void setLst_globalChatMessageList(Collection<ChatMessage> lst_globalChatMessageList) {
		this.lst_globalChatMessageList.setAll(lst_globalChatMessageList);
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public void setMessageTXBus(LinkedBlockingQueue<ChatMessage> messageTXBus) {
		this.messageTXBus = messageTXBus;
	}

//	public String getPraktiKSTVersion() {
//		return praktiKSTVersion;
//	}

//	public void setPraktiKSTVersion(String praktiKSTVersion) {
//		this.praktiKSTVersion = praktiKSTVersion;
//	}

	public String getPraktiKSTVersionInfo() {
		return praktiKSTVersionInfo;
	}

	public void setPraktiKSTVersionInfo(String praktiKSTVersionInfo) {
		this.praktiKSTVersionInfo = praktiKSTVersionInfo;
	}

//	public HashMap getMap_ucxLogInfoWorkedCalls() {
//		return map_ucxLogInfoWorkedCalls;
//	}

//	public void setMap_ucxLogInfoWorkedCalls(HashMap map_ucxLogInfoWorkedCalls) {
//		this.map_ucxLogInfoWorkedCalls = map_ucxLogInfoWorkedCalls;
//	}

	public ObservableRoster<ChatMember> getLst_chatMemberList() {
		return chatMemberList;
	}

	public ObservableRoster<ChatMember> getLst_DBBasedWkdCallSignList() {
		return lst_DBBasedWkdCallSignList;
	}

	public void setLst_DBBasedWkdCallSignList(Collection<ChatMember> lst_DBBasedWkdCallSignList) {
		this.lst_DBBasedWkdCallSignList.setAll(lst_DBBasedWkdCallSignList);
	}

	public void setLst_chatMemberList(Collection<ChatMember> lst_chatMemberList) {
		this.chatMemberList.setAll(lst_chatMemberList);
	}

	public ObservableRoster<Predicate<ChatMember>> getLst_chatMemberListFilterPredicates() {
		return lst_chatMemberListFilterPredicates;
	}

	public void setLst_chatMemberListFilterPredicates(Collection<Predicate<ChatMember>> lst_chatMemberListFilterPredicates) {
		this.lst_chatMemberListFilterPredicates.setAll(lst_chatMemberListFilterPredicates);
	}

	/** Adds one station-list filter. Nothing happens if it is already active. */
	public void addChatMemberListFilterPredicate(Predicate<ChatMember> predicate) {
		if (predicate == null || lst_chatMemberListFilterPredicates.snapshot().contains(predicate)) {
			return;
		}
		lst_chatMemberListFilterPredicates.add(predicate);
	}

	/** Removes one station-list filter. Unknown predicates are ignored. */
	public void removeChatMemberListFilterPredicate(Predicate<ChatMember> predicate) {
		if (predicate == null || !lst_chatMemberListFilterPredicates.snapshot().contains(predicate)) {
			return;
		}
		lst_chatMemberListFilterPredicates.remove(predicate);
	}

	/** Drops every station-list filter. Nothing happens if none is active. */
	public void clearChatMemberListFilterPredicates() {
		if (lst_chatMemberListFilterPredicates.isEmpty()) {
			return;
		}
		lst_chatMemberListFilterPredicates.clear();
	}

	/** The order the station list is presented in; {@code null} keeps the incoming order. */
	public Comparator<ChatMember> getChatMemberOrder() {
		return chatMemberOrder;
	}

	public void setChatMemberOrder(Comparator<ChatMember> chatMemberOrder) {
		this.chatMemberOrder = chatMemberOrder;
	}

	/**
	 * The visible station list, derived on demand from the canonical list, the
	 * active filters and the current order. This replaces the former
	 * FilteredList/SortedList chain.
	 */
	/**
	 * How many members are currently visible. Deliberately not
	 * {@code visibleChatMembers().size()}: that would sort several thousand rows to
	 * produce one number for the status line.
	 */
	public int visibleChatMemberCount() {
		return ChatMemberListView.countMatching(
				chatMemberList.snapshot(),
				lst_chatMemberListFilterPredicates.snapshot());
	}

	public List<ChatMember> visibleChatMembers() {
		return ChatMemberListView.compute(
				chatMemberList.snapshot(),
				lst_chatMemberListFilterPredicates.snapshot(),
				chatMemberOrder);
	}

	public ObservableRoster<ClusterMessage> getLst_clusterMemberList() {
		return lst_clusterMemberList;
	}

	public void setLst_clusterMemberList(Collection<ClusterMessage> lst_clusterMemberList) {
		this.lst_clusterMemberList.setAll(lst_clusterMemberList);
	}

	public LinkedBlockingQueue<ChatMessage> getMessageTXBus() {
		return messageTXBus;
	}

	/**
	 * Hands UI-visible changes over to the thread that owns the user interface.
	 * Headless construction defaults to running them on the calling thread.
	 */
	private UiDispatcher uiDispatcher = new DirectUiDispatcher();

	/** The boundary worker threads use instead of touching the user interface. */
	public UiDispatcher uiDispatcher() {
		return uiDispatcher;
	}

	public ChatController() {

		super();
		chatCategoryMain = new ChatCategory(2);//Todo: selectable chatcategory, switched by user
		chatCategorySecondChat = new ChatCategory(3); //Todo: selectable chatcategory, switched by user

		ownChatMemberObject = new ChatMember();
		ownChatMemberObject.setCallSign(userName);
		ownChatMemberObject.setName(showedName);
		ownChatMemberObject.setQra(qra);

//		this.category = ChatCategory.VUHF;
		this.userName = ownChatMemberObject.getName();
//		this.hostname = "www.on4kst.org";
		this.port = port;
	}

	/**
	 * This constructor is used by the Main()-Class of the praktiKST javaFX-gui.
	 * 
	 *
	 * @param setOwnChatMemberObject
	 */
	public ChatController(ChatMember setOwnChatMemberObject,StatusUpdateListener listener) {
		this(setOwnChatMemberObject,
				listener,
				ChatPreferences.PREFERENCES_FILE,
				DBController.DATABASE_FILE,
				true,
				// Headless construction: callers with a user interface use the
				// constructor that takes the dispatcher explicitly.
				new DirectUiDispatcher());
	}

	/**
	 * Creates a chat controller bound to the files of one operator profile.
	 *
	 * <p>Both file names are resolved below the application directory. This is the only
	 * place where the active operator profile enters the controller; everything below
	 * works on the resulting {@link ChatPreferences} and {@link DBController} instances
	 * without knowing about profiles at all.</p>
	 *
	 * @param setOwnChatMemberObject          chat member object representing the local station
	 * @param listener                        callback for thread status updates
	 * @param preferencesRelativeFileName     preferences file name relative to the application directory
	 * @param workedDatabaseRelativeFileName  worked-station database file name relative to the application directory
	 * @param seedWorkedDatabaseFromResource  true to seed a missing database from the bundled template
	 */
	public ChatController(ChatMember setOwnChatMemberObject,
			StatusUpdateListener listener,
			String preferencesRelativeFileName,
			String workedDatabaseRelativeFileName,
			boolean seedWorkedDatabaseFromResource,
			UiDispatcher uiDispatcher) {
		super();
		this.uiDispatcher = java.util.Objects.requireNonNull(uiDispatcher, "uiDispatcher");

        chatPreferences = new ChatPreferences(preferencesRelativeFileName);
        chatPreferences.readPreferencesFromXmlFile();
		Path historyFile = Path.of(ApplicationFileUtils.getDebugFilePath(
				ApplicationConstants.APPLICATION_NAME,
				MessageHistoryRecorder.CURRENT_FILE_NAME));
		messageHistoryRecorder = new MessageHistoryRecorder(
				historyFile.getParent(),
				chatPreferences.isMessageHandling_debugModeToFileEnabled(),
				java.time.Clock.systemUTC());
//        this.statusListener = listener;
		lstNotify_QSOSniffer_sniffedCallSignList =
				chatPreferences
						.getLstNotify_QSOSniffer_sniffedCallSignList();

        String dnsFromPrefs = chatPreferences.getStn_on4kstServersDns();
        if (dnsFromPrefs != null && !dnsFromPrefs.isEmpty()) {
            this.hostname = dnsFromPrefs;
        } else {
            this.hostname = "109.90.0.130";
        }

		UpdateChecker checkForUpdates = new UpdateChecker(this);

		if (checkForUpdates.downloadLatestVersionInfoXML()) {
			updateInformation = checkForUpdates.parseUpdateXMLFile();
		};

        initLst_toMeMessageList();



		setToAllMessageFilter(new Predicate<ChatMessage>() {
			@Override
			public boolean test(ChatMessage chatMessage) {

				try {
				if (chatMessage.getReceiver().getCallSign().equals("ALL")) { //TODO: ALL have to be an application-constant
					return true;
				} else return false;

				}
				catch (Exception nullPointerExc) {
					nullPointerExc.printStackTrace();
					System.out.println("ChatController, ERROR: maybe the receiver was null, mostly like a cq message!");
					return true;
				}

			}
		});

		setToOtherMessageFilter(new Predicate<ChatMessage>() {
			@Override
			public boolean test(ChatMessage chatMessage) {
				try {
					if ((!chatMessage.getSender().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) &&
							(!chatMessage.getReceiver().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL")) )
					//RX not own callsign and TX not own callsign and callsign is not "ALL" (that means, directed to public)
					{
						return true;
					} else return false;

				} catch (Exception nullPointerExc) {
//					nullPointerExc.printStackTrace();
					System.out.println("ChatController, <<<catched ERROR>>>: maybe the receiver was null!");
					return false;
				}
			}
		});

		dbHandler = new DBController(workedDatabaseRelativeFileName, seedWorkedDatabaseFromResource);
		reachabilityService = new ReachabilityService(this);
		rebuildWorkedGrossFieldCacheFromDatabase();

//		chatPreferences = new ChatPreferences();
//		chatPreferences.readPreferencesFromXmlFile(); // set the praktikst Prefs by file or default if file is corrupted

		chatCategoryMain = chatPreferences.getLoginChatCategoryMain();
		chatCategorySecondChat = chatPreferences.getLoginChatCategorySecond();
		this.userName = chatPreferences.getStn_loginCallSign();
		this.password = chatPreferences.getStn_loginPassword();
//		category = setCategory;
		ownChatMemberObject = setOwnChatMemberObject;

//		this.userName = ownChatMemberObject.getName();
//		this.password = ownChatMemberObject.getPassword();
		this.hostname = this.getChatPreferences().getStn_on4kstServersDns(); //default: www.on4kst.org

    }


    private void initLst_toMeMessageList() {
//        ObservableList<String> sniffedList = chatPreferences.getLstNotify_QSOSniffer_sniffedCallSignList();

        Predicate<ChatMessage> chatFilterPredicate = chatMessage -> {
            // Sicherheits-Checks gegen NullPointer (statt try-catch)
            if (chatMessage == null || chatMessage.getSender() == null || chatMessage.getReceiver() == null) {
                return false;
            }

            String myCallSign = getChatPreferences().getStn_loginCallSign();
            String senderCall = chatMessage.getSender().getCallSign();
            String receiverCall = chatMessage.getReceiver().getCallSign();
            String msgText = chatMessage.getMessageText();

            // --- NEUE LOGIK: Sniffer Liste prüfen ---
            // Wenn Absender ODER Empfänger in der Beobachtungsliste stehen -> Anzeigen
//            if ((lstNotify_QSOSniffer_sniffedCallSignList.contains(senderCall) ||
//                    lstNotify_QSOSniffer_sniffedCallSignList.contains(receiverCall)) &&
//                    (!receiverCall.equals(this.getChatPreferences().getStn_loginCallSignRaw()))) {
//
//                msgText = ("Sniffed: " + "(" + senderCall + " > ") + receiverCall +") " + msgText;
//                chatMessage.setMessageText(msgText);
//                return true;
//            }

			if (isSniffedMessage(chatMessage)) {
				return true;
			}

            // --- BESTEHENDE LOGIK ---

            // 1. Nachrichten direkt an dich
            if (receiverCall.equals(myCallSign)) {
                return true;
            }

            // 2. Deine eigenen Nachrichten (außer an ALL)
            if (senderCall.equals(myCallSign) && !receiverCall.equals("ALL")) {
                return true;
            }

            // 3. Mentions im Text (jemand schreibt über dich)
            // Nur prüfen, wenn Text nicht null ist und du nicht selbst der Absender bist
            if (msgText != null && !senderCall.equals(myCallSign)) {
                // containsIgnoreCase Logik (etwas robuster als deine Variante)
                if (msgText.toLowerCase().contains(myCallSign.toLowerCase())) {
                    return true;
                }
            }

            return false;
        };

        /*
         * The former FilteredList had to be told to rescan when the monitoring list
         * changed. The derived view is recomputed on every read instead, so the
         * user interface only has to watch the monitoring roster as a second source.
         */

//        lstNotify_QSOSniffer_sniffedCallSignList.add("DF0GEB");


        setToMeMessageFilter(chatFilterPredicate); //sniffed callsign filter predicate is here!

//        lst_toMeMessageList.setPredicate(new Predicate<ChatMessage>() {
//            @Override
//            public boolean test(ChatMessage chatMessage) {
//
//                try {
//
//                    if (chatMessage.getReceiver().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) {
//                        return true; //messages addressed to you
//                    }
//                    if ((chatMessage.getSender().getCallSign().equals(getChatPreferences().getStn_loginCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL"))){
//                        return true; //your own echo except texts to all (CQ)
//                    }
//
//                    String ignoreCaseString = chatMessage.getMessageText();
//
//                    if ((chatMessage.getMessageText().contains(chatPreferences.getStn_loginCallSign().toLowerCase()) || (chatMessage.getMessageText().contains(chatPreferences.getStn_loginCallSign().toUpperCase())))
//                            && (!chatMessage.getSender().getCallSign().equals(getChatPreferences().getStn_loginCallSign()))) {
//                        return true; //if someone writes about you, you will get the mail, too, except you are the sender...!
//                    }
//
//                    else {
//                        return false;
//                    }
//                }
//                catch (Exception nullPointerExc) {
//                    nullPointerExc.printStackTrace();
//                    System.out.println("ChatController, <<<catched ERROR>>>: maybe the receiver was null, message received b4 user entered chatmessage...!" + nullPointerExc.getMessage());
//                    return false;
//                }
//            }
//        });



    }

	/**
	 * starts wintest udp listener thread
	 */
	public synchronized void startWintestUdpListener() {
		if (readUDPByWintestThread != null && readUDPByWintestThread.isAlive()) {
			return;
		}

		readUDPByWintestThread = new ReadUDPByWintestThread(this, this);
		readUDPByWintestThread.setName("readUDPByWintestThread");
		readUDPByWintestThread.start();

		System.out.println("[ChatController] Win-Test UDP listener started.");
	}

	/**
	 * stops wintest udp listener thread
	 */
	public synchronized void stopWintestUdpListener() {
		if (readUDPByWintestThread == null) return;

		try {
			readUDPByWintestThread.interrupt();
		} catch (Exception ignored) { }

		readUDPByWintestThread = null;
		System.out.println("[ChatController] Win-Test UDP listener stopped.");
	}

	/**
	 * restarts wintest udp listener thread
	 */
	public synchronized void restartWintestUdpListenerIfEnabled() {
		stopWintestUdpListener();
		if (chatPreferences.isLogsynch_wintestNetworkListenerEnabled()) {
			startWintestUdpListener();
		}
	}



    public SimpleRoster<String> getLstNotify_QSOSniffer_sniffedCallSignList() {
        return lstNotify_QSOSniffer_sniffedCallSignList;
    }

    public void setLstNotify_QSOSniffer_sniffedCallSignList(SimpleRoster<String> lstNotify_QSOSniffer_sniffedCallSignList) {
        this.lstNotify_QSOSniffer_sniffedCallSignList = lstNotify_QSOSniffer_sniffedCallSignList;
    }

    public ChatPreferences getChatPreferences() {
		return chatPreferences;
	}

	public MessageHistoryRecorder getMessageHistoryRecorder() {
		return messageHistoryRecorder;
	}

	/** Applies the runtime debug preference to the raw session recorder. */
	public void setMessageHistoryRecordingEnabled(boolean enabled) {
		messageHistoryRecorder.setEnabled(enabled);
	}

	public void setChatPreferences(ChatPreferences chatPreferences) {
		this.chatPreferences = chatPreferences;
	}

	public ChatMember getownChatMemberObject() {
		return ownChatMemberObject;
	}

	public void setOwnCall(ChatMember ownCall) {
		this.ownChatMemberObject = ownCall;
	}

	public LinkedBlockingQueue<ChatMessage> getMessageRXBus() {
		return messageRXBus;
	}

	public void setMessageRXBus(LinkedBlockingQueue<ChatMessage> messageBus) {
		this.messageRXBus = messageBus;
	}

	public WriteThread getWriteThread() {
		return writeThread;
	}


	public void setWriteThread(WriteThread writeThread) {
		this.writeThread = writeThread;
	}

	public ReadThread getReadThread() {
		return readThread;
	}

	public void setReadThread(ReadThread readThread) {
		this.readThread = readThread;
	}

	public ChatCategory getChatCategoryMain() {
		return chatCategoryMain;
	}

	public void setChatCategoryMain(ChatCategory chatCategoryMain) {
		this.chatCategoryMain = chatCategoryMain;
	}

	public DXClusterThreadPooledServer getDxClusterServer() {
		return dxClusterServer;
	}

	public synchronized void startDxClusterServerIfEnabled() {
		if (!chatPreferences.isNotify_dxClusterServerEnabled()
				|| dxClusterServer != null) {
			return;
		}

		dxClusterServer = new DXClusterThreadPooledServer(
				chatPreferences.getNotify_dxclusterServerPort(),
				this,
				this
		);

		Thread serverThread = new Thread(dxClusterServer);
		serverThread.setName("DXCluster-thread-pooled-server");
		serverThread.setDaemon(true);
		serverThread.start();
	}

	public synchronized void stopDxClusterServer() {
		DXClusterThreadPooledServer serverToStop = dxClusterServer;
		dxClusterServer = null;

		if (serverToStop != null) {
			serverToStop.stop();
		}
	}

	public synchronized void restartDxClusterServerIfEnabled() {
		stopDxClusterServer();
		startDxClusterServerIfEnabled();
	}

	//	public void setChatMemberTable(Hashtable<String, ChatMember> chatMemberTable) {
//		this.chatMemberTable = chatMemberTable;
//	}
//	
//	public void setChatMemberTable(HashMap<String, ChatMember> chatMemberTable) {
//		this.chatMemberTable = chatMemberTable;
//	}

	public DBController getDbHandler() {
		return dbHandler;
	}

	public void setDbHandler(DBController dbHandler) {
		this.dbHandler = dbHandler;
	}


	/**
	 * Validates a configured beacon template before it is stored.
	 *
	 * <p>The template itself must be a non-empty, protocol-safe message. Global
	 * variables are resolved as far as their values are currently available.
	 * A template which temporarily resolves to an empty string, for example
	 * {@code MYQRG} before a frequency is known, remains valid. The timer performs
	 * the stricter final validation immediately before transmission.</p>
	 *
	 * @param configuredText configured beacon template
	 * @throws IllegalArgumentException if the template contains invalid protocol
	 *                                  content or currently resolves to more than
	 *                                  {@value MAX_BEACON_TEXT_LENGTH} characters
	 */
	public void validateBeaconTemplate(String configuredText) {
		String normalizedTemplate =
				On4KstProtocol.messageText(configuredText);

		String resolvedText =
				new MessageVariableResolver(chatPreferences)
						.resolveGlobalVariables(normalizedTemplate);

		/*
		 * A template consisting only of a variable such as MYQRG may temporarily
		 * resolve to an empty value. It can still be stored because the value may
		 * become available through TRX synchronisation before the timer runs.
		 */
		if (resolvedText == null || resolvedText.isBlank()) {
			return;
		}

		validateResolvedBeaconText(resolvedText);
	}

	/**
	 * Resolves and validates the final beacon text immediately before transmission.
	 *
	 * <p>This is deliberately stricter than {@link #validateBeaconTemplate(String)}.
	 * A timer run must never queue an empty message merely because a dynamic value
	 * is not available at that moment.</p>
	 *
	 * @param configuredText configured beacon template
	 * @return normalized text which may safely pass through the regular ON4KST
	 *         message pipeline
	 * @throws IllegalArgumentException if the resolved message is empty, too long
	 *                                  or contains an ON4KST delimiter
	 */
	public String resolveAndValidateBeaconText(String configuredText) {
		String resolvedText =
				new MessageVariableResolver(chatPreferences)
						.resolveGlobalVariables(configuredText);

		return validateResolvedBeaconText(resolvedText);
	}

	/**
	 * Applies the message-text and length rules to a fully resolved beacon.
	 */
	private String validateResolvedBeaconText(String resolvedText) {
		String normalizedText =
				On4KstProtocol.messageText(resolvedText);

		if (normalizedText.length() > MAX_BEACON_TEXT_LENGTH) {
			throw new IllegalArgumentException(
					"The resolved beacon message contains "
							+ normalizedText.length()
							+ " characters; maximum is "
							+ MAX_BEACON_TEXT_LENGTH
							+ "."
			);
		}

		return normalizedText;
	}

	/**
	 * Starts the shared beacon timer with the interval currently stored in the
	 * preferences.
	 *
	 * <p>Both chat categories deliberately use the same timer. The task checks the
	 * individual enable flag and text for each category every time it runs.</p>
	 *
	 * @param initialDelayMillis delay before the next beacon run
	 */
	private synchronized void scheduleBeaconTimer(long initialDelayMillis) {
		stopBeaconTimer();

		int configuredInterval =
				chatPreferences.getBcn_beaconIntervalInMinutesMainCat();
		int effectiveInterval =
				Math.max(MIN_BEACON_INTERVAL_MINUTES, configuredInterval);

		/*
		 * Keep both legacy XML values synchronized. The second value remains in the
		 * configuration for backward compatibility, but no longer represents an
		 * independent timer.
		 */
		chatPreferences.setBcn_beaconIntervalInMinutesMainCat(effectiveInterval);
		chatPreferences.setBcn_beaconIntervalInMinutesSecondCat(effectiveInterval);

		long intervalMillis = TimeUnit.MINUTES.toMillis(effectiveInterval);
		long safeInitialDelay = Math.max(0L, initialDelayMillis);

		beaconTimer = new Timer("BeaconTimer");
		beaconTimer.schedule(
				new BeaconTask(this, this),
				safeInitialDelay,
				intervalMillis
		);

		System.out.println("[ChatController, Info]: Shared beacon timer scheduled every "
				+ effectiveInterval + " minute(s).");
	}

	/**
	 * Applies a changed beacon interval while the chat connection is running.
	 *
	 * <p>The countdown starts again with the new interval. No beacon is sent
	 * immediately merely because the setting was changed.</p>
	 */
	public synchronized void restartBeaconTimer() {
		if (!isConnectedAndLoggedIn()) {
			return;
		}

		long intervalMillis = TimeUnit.MINUTES.toMillis(
				Math.max(
						MIN_BEACON_INTERVAL_MINUTES,
						chatPreferences.getBcn_beaconIntervalInMinutesMainCat()
				)
		);
		scheduleBeaconTimer(intervalMillis);
	}

	/**
	 * Stops the shared beacon timer if it is currently running.
	 */
	private synchronized void stopBeaconTimer() {
		if (beaconTimer == null) {
			return;
		}

		beaconTimer.cancel();
		beaconTimer.purge();
		beaconTimer = null;
	}

	/**
	 * execute is the main entry point where the application starts.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void execute() throws InterruptedException, IOException {

		if (on4KstConnectionManager != null) {
			executeManaged();
			return;
		}

		chatController = this;

//		ApplicationConstants constants = new ApplicationConstants();

		// This block constructs a sample message
//		ChatMessage Test = new ChatMessage();
//		Test.setMessage("kst4contest.test");
//		Test.setMessageDirectedToCommunity(false);
//		Test.setMessageGeneratedTime(new Utils4KST().time_convertEpochToReadable("1664669836"));
//		Test.setMessageSenderName("marc");
//		Test.setMessageText("test2");
//		Test.setSender(ownChatMemberObject);
//		getLst_toAllMessageList().add(Test);

		try {
			setDisconnectionPerformedByUser(false);

			startScoreScheduler();
			//runs sked priority thread

			messageRXBus = new LinkedBlockingQueue<ChatMessage>();
			messageTXBus = new LinkedBlockingQueue<ChatMessage>();

//			socket = new Socket(hostname, port);//socket for the on4kst chat server
			socket = new Socket(
					chatPreferences.getStn_on4kstServersDns(),
					chatPreferences.getStn_on4kstServersPort()
			);//socket for the on4kst chat server

			System.out.println("Connected to the chat server: " + socket.isConnected());

			ByteBuffer buffer = ByteBuffer.allocate(1024);
			Selector selector = Selector.open();

//			consoleReader = new InputReaderThread(this);
//			consoleReader.start();

			readThread = new ReadThread(socket, this);
			readThread.setName("ReadThread-telnetreader");
			readThread.start();

			writeThread = new WriteThread(socket, this);
			writeThread.setName("Writethread-telnetwriter");
			writeThread.start();

			readUDPbyUCXThread = new ReadUDPbyUCXMessageThread(chatPreferences.getLogsynch_ucxUDPWkdCallListenerPort(), this, this);
			readUDPbyUCXThread.setName("readUDPbyUCXThread");
			readUDPbyUCXThread.start();

			if (chatPreferences.isLogsynch_wintestNetworkListenerEnabled()) {
				startWintestUdpListener();
			} else {
				System.out.println("[ChatController] Win-Test listener disabled by preference -> not starting.");
			}

			messageProcessor = new MessageBusManagementThread(this, this);
			messageProcessor.setName("messagebusManagementThread");
			messageProcessor.start();

			airScoutUDPReaderThread = new ReadUDPbyAirScoutMessageThread(
					chatPreferences.getAirScout_asCommunicationPort(),
					this,
					this
			);
			airScoutUDPReaderThread.setName("airscoutudpreaderThread");
			airScoutUDPReaderThread.start();

			userActualizationtimer = new Timer();
			userActualizationtimer.schedule(new UserActualizationTask(this), 4000, 60000);// TODO: Temporary userlistoutput known qrgs

			keepAliveTimer = new Timer();
			keepAliveTimer.schedule(new keepAliveMessageSenderTask(this), 4000, 60000);//

			if (chatPreferences.isStn_pstRotatorEnabled()) {
				initRotor();
			} else {
				System.out.println("[ChatController, info]: PSTRotator disabled by user preference -> not starting rotator client.");
			}
			if (secondRotatorWanted(chatPreferences) && !rotator2.isRunning()) {
				rotator2.start();
			}

			/**
			 * DX cluster service running config
			 */
			startDxClusterServerIfEnabled();


			this.setConnectedAndLoggedIn(true);

			/**
			 * The CQ-beacon-Task will be executed every time but checks for itself whether
			 * CQ messages are enabled or not
			 */
			scheduleBeaconTimer(INITIAL_BEACON_DELAY_MILLIS);

			/**
			 * The AS querier task will be executed every time but checks for itself whether
			 * AS usage is enabled or not
			 */
//			Timer ASQueryTimer;
			ASQueryTimer = new Timer();
			ASQueryTimer.schedule(new AirScoutPeriodicalAPReflectionInquirerTask(this), 10000, 60000);
			// 60000 * intervalInMinutes = IntervalInMillis

			/**
			 * Check if socket works
			 */
//			Timer socketCheckTimer;
			socketCheckTimer = new Timer();
			socketCheckTimer.schedule(new TimerTask() {

				@Override
				public void run() {
//					System.out.println("[Chatcontroller, info: ] periodical socketcheck");

					Thread.currentThread().setName("SocketcheckTimer");

					if (!socket.isConnected() || socket.isClosed()) {
						try {
							messageRXBus.clear();
							messageTXBus.clear();
							socket.close();
							
							chatController.setConnectedAndLoggedIn(false);
							chatController.clearActiveChatMembers();

							System.out.println("[Chatcontroller, Warning: ] Socket closed or disconnected");
						
							ChatMessage killThreadPoisonPillMsg = new ChatMessage();
							killThreadPoisonPillMsg.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							killThreadPoisonPillMsg.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							
							ChatMessage killThreadPoisonPillMsg2 = new ChatMessage();
							killThreadPoisonPillMsg2.setMessageText(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							killThreadPoisonPillMsg2.setMessageSenderName(ApplicationConstants.DISCONNECT_RDR_POISONPILL);
							
							
							messageRXBus.add(killThreadPoisonPillMsg);
							
							messageTXBus.add(killThreadPoisonPillMsg2);
							chatController.getReadThread().interrupt();

						
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						/**
						 * The next block will attempt to reinit the chatclient after accidental
						 * disconnection
						 */
						try {

							if (!disconnectionPerformedByUser) {

								messageRXBus.clear();
								messageTXBus.clear();

								socket = new Socket(hostname, port);

//							readThread.interrupt();

								chatController.setReadThread(new ReadThread(socket, chatController));
								chatController.readThread.start();

								chatController.setWriteThread(new WriteThread(socket, chatController));
								chatController.writeThread.start();
								
								messageProcessor = new MessageBusManagementThread(chatController, chatController);
								messageProcessor.start();

								System.out.println("[Chatcontroller, info: initialized new socket, is connected? ] "
										+ socket.isConnected() + " " + socket.isClosed());

								initialize23001();

								Timer waitABit = new Timer();
								socketCheckTimer.schedule(new TimerTask() {

									@Override
									public void run() {
										Thread.currentThread().setName("waiting");
										
										if (socket.isConnected()) {
											chatController.setConnectedAndLoggedIn(true);
										}

										//just take a breath
									}
								}, 5000);


//								keepAliveTimer.notify();

							}

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}
			}, 10000, 10000);

		} catch (UnknownHostException ex) {
			System.out.println("Server not found: " + ex.getMessage());
		} catch (IOException ex) {
			System.out.println("I/O Error: " + ex.getMessage());
		}

		while (readThread == null) {
			// do nothing, wait!
			System.out.println("Reader not ready.");
		}

//		initialize();//kst4contest.test 4 23001
		initialize23001(); // init Chatcontroller for using port 23001

	}

	private synchronized void executeManaged() throws IOException {
		chatController = this;
		setDisconnectionPerformedByUser(false);
		startScoreScheduler();

		if (readUDPbyUCXThread == null || !readUDPbyUCXThread.isAlive()) {
			readUDPbyUCXThread = new ReadUDPbyUCXMessageThread(
					chatPreferences.getLogsynch_ucxUDPWkdCallListenerPort(),
					this, this);
			readUDPbyUCXThread.setName("readUDPbyUCXThread");
			readUDPbyUCXThread.start();
		}

		if (chatPreferences.isLogsynch_wintestNetworkListenerEnabled()) {
			startWintestUdpListener();
		}

		if (airScoutUDPReaderThread == null || !airScoutUDPReaderThread.isAlive()) {
			airScoutUDPReaderThread = new ReadUDPbyAirScoutMessageThread(
					chatPreferences.getAirScout_asCommunicationPort(),
					this, this);
			airScoutUDPReaderThread.setName("airscoutudpreaderThread");
			airScoutUDPReaderThread.start();
		}

		cancelTimer(userActualizationtimer);
		userActualizationtimer = new Timer("UserActualizationTimer", true);
		userActualizationtimer.schedule(
				new UserActualizationTask(this), 4_000L, 60_000L);

		cancelTimer(ASQueryTimer);
		ASQueryTimer = new Timer("AirScoutQueryTimer", true);
		ASQueryTimer.schedule(
				new AirScoutPeriodicalAPReflectionInquirerTask(this),
				10_000L, 60_000L);

		if (chatPreferences.isStn_pstRotatorEnabled() && !rotator1.isRunning()) {
			initRotor();
		}
		if (secondRotatorWanted(chatPreferences) && !rotator2.isRunning()) {
			rotator2.start();
		}
		startDxClusterServerIfEnabled();
		on4KstConnectionManager.start();
	}



	/**
	 * Returns the background reachability service used by the station table.
	 *
	 * @return reachability service
	 */
	public ReachabilityService getReachabilityService() {
		return reachabilityService;
	}

	/**
	 * Returns the runtime gross-field cache used by the new-locator filter.
	 *
	 * @return worked gross-field cache
	 */
	public WorkedGrossFieldCache getWorkedGrossFieldCache() {
		return workedGrossFieldCache;
	}

	/**
	 * Rebuilds the gross-field cache from persistent SQLite data and enriches it with
	 * legacy ChatMember worked/qra rows where possible.
	 */
	public void rebuildWorkedGrossFieldCacheFromDatabase() {
		if (dbHandler == null) {
			return;
		}

		try {
			workedGrossFieldCache.rebuildFromDatabaseSnapshot(dbHandler.fetchWorkedGrossFieldsFromDB());
			workedGrossFieldCache.addWorkedBandsFromStoredChatMembers(dbHandler.fetchChatMemberWkdDataFromDB().values());
		} catch (Exception exception) {
			System.out.println("[ChatController, warning]: could not rebuild worked gross-field cache: "
					+ exception.getMessage());
		}
	}

	/**
	 * Persists and caches one worked gross field after a logger QSO packet.
	 *
	 * @param band worked band
	 * @param locator6 six-character Maidenhead locator
	 * @param workedCall worked station
	 * @param source logger/source name
	 */
	public void registerWorkedGrossField(Band band, String locator6, ChatMember workedCall, String source) {
		if (band == null || locator6 == null) {
			return;
		}

		String normalizedLocator6 = WorkedGrossFieldCache.extractLocator6(locator6);
		if (normalizedLocator6 == null) {
			return;
		}

		String callSignRaw = workedCall == null ? null : workedCall.getCallSignRaw();

		try {
			dbHandler.upsertWorkedGrossField(band, normalizedLocator6, callSignRaw, source);
		} catch (Exception exception) {
			System.out.println("[ChatController, warning]: could not persist worked gross field: "
					+ exception.getMessage());
		}

		workedGrossFieldCache.addWorked(band, normalizedLocator6);
		fireUserListUpdate("Worked gross field updated");
	}

	/**
	 * Returns true when the member's four-character grid square has already been
	 * worked on any band.
	 *
	 * <p>This method intentionally ignores the currently enabled own bands. The grid
	 * status in the user list is meant to be a simple and robust any-band indicator.</p>
	 *
	 * @param member member to inspect
	 * @return true if the gross grid square was worked on any band
	 */
	public boolean isGridSquareWorkedAny(ChatMember member) {
		if (member == null || member.getQra() == null) {
			return false;
		}

		return workedGrossFieldCache.isGrossFieldWorkedAny(member.getQra());
	}

	/**
	 * Returns true when the member's four-character grid square has not been worked
	 * on any band yet.
	 *
	 * <p>This is the predicate behind the optional "Only new grids" filter. It no
	 * longer depends on active station band settings.</p>
	 *
	 * @param member member to inspect
	 * @return true if the gross grid square is still new
	 */
	public boolean isNewGridSquare(ChatMember member) {
		if (member == null || member.getQra() == null) {
			return false;
		}

		return !workedGrossFieldCache.isGrossFieldWorkedAny(member.getQra());
	}

	public long getCurrentEpochTime() {

		OffsetDateTime currentTimeInUtc = OffsetDateTime.now(ZoneOffset.UTC);

//		System.out.println(currentTimeInUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm X")));

		long millisecondsSinceEpoch = currentTimeInUtc.toInstant().toEpochMilli() / 1000;
//	    System.out.println(millisecondsSinceEpoch);
		return millisecondsSinceEpoch;
	}

	/**
	 * Setting the initial parameters at the chat via port 23001 <br/>
	 * <br/>
	 * <b>Login parameter format is like that: </b><br/>
	 * LOGINC|do5amf|password|2|kst4contest1251|25|0|1|1663879818|0| <br/>
	 * SDONE|2| <br/>
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void initialize23001() throws InterruptedException, IOException {

		if (on4KstConnectionManager != null) {
			on4KstConnectionManager.start();
			return;
		}

		messageTXBus.clear();

		ChatMessage message = new ChatMessage();

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {

				Thread.currentThread().setName("LoginStringTimer");

				//this is the original loginC without history abonnement
//				String loginString = "";
//				loginString = "LOGINC|" + chatPreferences.getStn_loginCallSign() + "|" + chatPreferences.getStn_loginPassword()
//						+ "|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber() + "|praktiKST v" + ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER
//						+ "|25|0|1|" + getCurrentEpochTime() + "|0|";

				String loginString = "";
				loginString = "LOGINC|" + chatPreferences.getStn_loginCallSign() + "|" + chatPreferences.getStn_loginPassword()
						+ "|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber() + "|praktiKST v" + ApplicationConstants.APPLICATION_CURRENT_VERSION
						+ "|25|0|1|" + "0" + "|0|";

				// System.out.println(loginString);
				ChatMessage message = new ChatMessage();
				message.setMessageText(loginString);
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 2000);

		/**
		 * Entering second chat
		 *
		 * ACHAT|chat id|past messages number|past dx/map number|users list/update flags|last Unix timestamp for messages|last Unix timestamp for dx/map|
		 */

		if (this.chatController.getChatPreferences().isLoginToSecondChatEnabled()) { //only login to second if wished

			new Timer().schedule(new TimerTask() {

				@Override
				public void run() { //test second chat

					Thread.currentThread().setName("LoginStringTimerSecond");

					String loginString = "";
					loginString = "ACHAT|" + chatController.getChatPreferences().getLoginChatCategorySecond().getCategoryNumber() + "|" + "25"
							+ "|" + "10" + "|2|" + getCurrentEpochTime() + "|" + getCurrentEpochTime();

					// System.out.println(loginString);
					ChatMessage message = new ChatMessage();
					message.setMessageText(loginString);
					message.setMessageDirectedToServer(true);
					getMessageTXBus().add(message);

				}
			}, 5000);
		}
		/**
		 * end testing second chat
		 *
		 */

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {

				Thread.currentThread().setName("SDONEStringTimer");
				ChatMessage message = new ChatMessage();
				message.setMessageText("SDONE|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber() + "|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);

			}
		}, 3000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETLOCTIMER");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber()
						+ "|0|/SETLOC " + chatPreferences.getStn_loginLocatorMainCat() + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);
			}
		}, 4000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETNAMETIMER");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber()
						+ "|0|/SETNAME " + chatPreferences.getStn_loginNameMainCat() + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);
			}
		}, 5000);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				Thread.currentThread().setName("SETHereTimerMain");
				ChatMessage message = new ChatMessage();
				message.setMessageText("MSG|" + chatPreferences.getLoginChatCategoryMain().getCategoryNumber()
						+ "|0|/BACK" + "|0|\r");
				message.setMessageDirectedToServer(true);
				getMessageTXBus().add(message);
			}
		}, 6500);


		if (chatPreferences.isLoginToSecondChatEnabled()) { //only if second category had been enabled

			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					Thread.currentThread().setName("SETNAMETIMER2nd");
					ChatMessage message = new ChatMessage();
					message.setMessageText("MSG|" + chatPreferences.getLoginChatCategorySecond().getCategoryNumber()
							+ "|0|/SETNAME " + chatPreferences.getStn_loginNameSecondCat() + "|0|\r");
					message.setMessageDirectedToServer(true);
					getMessageTXBus().add(message);
				}
			}, 5500);

			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					Thread.currentThread().setName("SETHereTimerSecond");
					ChatMessage message = new ChatMessage();
					message.setMessageText("MSG|" + chatPreferences.getLoginChatCategorySecond().getCategoryNumber()
							+ "|0|/BACK" + "|0|\r");
					message.setMessageDirectedToServer(true);
					getMessageTXBus().add(message);
				}
			}, 7000);

		}

//		new Timer().schedule(new TimerTask() {
//			HashMap<String, ChatMember> getWorkedDataFromDb;
//
//			@Override
//			public void run() {
//
//				Thread.currentThread().setName("fetchWorkedFromDBTimer");
//
//				try {
//					getWorkedDataFromDb = dbHandler.fetchChatMemberWkdDataFromDB();
//				} catch (SQLException e) {
//					System.out.println("[Chatctrl, Error: ] got no worked data from DB due to communication error");
//				}
//
//				for (Iterator iterator = getLst_chatMemberList().iterator(); iterator.hasNext();) {
//
//
//					ChatMember chatMember = (ChatMember) iterator.next();
//					System.out.println("[Chatctrl]: Marking ChatMembers wkd information: "
//							+ getWorkedDataFromDb.get(chatMember.getCallSign()).getCallSign());
//					chatMember.setWorked(getWorkedDataFromDb.get(chatMember.getCallSign()).isWorked());
//					chatMember.setWorked144(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked144());
//					;
//					chatMember.setWorked432(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked432());
//					;
//					chatMember.setWorked1240(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked1240());
//					;
//					chatMember.setWorked2300(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked2300());
//					;
//					chatMember.setWorked3400(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked3400());
//					;
//					chatMember.setWorked5600(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked5600());
//					;
//					chatMember.setWorked10G(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isWorked10G());
//					/**
//					 * v1.2 since here
//					 * TODO: Change that, this ins not generative
//					 */
//
//					chatMember.setQrv144(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv144());
//					;
//					chatMember.setQrv432(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv432());
//					;
//					chatMember.setQrv1240(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv1240());
//					;
//					chatMember.setQrv2300(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv2300());
//					;
//					chatMember.setQrv3400(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv3400());
//					;
//					chatMember.setQrv5600(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv5600());
//					;
//					chatMember.setQrv10G(getWorkedDataFromDb.get(chatMember.getCallSignRaw()).isQrv10G());
//					;
//				}
//
//				/**
//				 *
//				 * This creates the list of the worked stations which had to be displayed in the
//				 * settings menu. TODO: May make this List editable
//				 *
//				 */
//
//				getWorkedDataFromDb.forEach((key, value) -> {
//
//					chatController.getLst_DBBasedWkdCallSignList().add(value);
//
////			        System.out.println("Key=" + key + ", Value=" + value);
//				});
//			}
//		}, 10000);

//		message = new ChatMessage();
//		message.setMessageText("MSG|2|0|/SETNAME " + ownChatMemberObject.getName() + "|0|\r");
//		message.setMessageDirectedToServer(true);
//		this.getMessageTXBus().add(message);

		// message.setMessageText(ownCall.getCallSign());
//		this.getMessageTXBus().add(message);

	}

	/**
	 * Reloads the worked/not-QRV state from the internal database and applies the
	 * result both to the active chatmember list and to the database table list in the
	 * settings dialog. UI-bound list modifications are executed on the JavaFX thread..
	 */
	public void refreshWorkedStateAndDatabaseListFromDatabase() {

		HashMap<String, ChatMember> workedDataFromDatabase;

		try {
			workedDataFromDatabase = dbHandler.fetchChatMemberWkdDataFromDB();
		} catch (SQLException e) {
			System.out.println("[Chatctrl, Error: ] got no worked data from DB due to communication error");
			e.printStackTrace();
			return;
		}

		rebuildWorkedGrossFieldCacheFromDatabase();

		HashMap<String, ChatMember> finalWorkedDataFromDatabase = workedDataFromDatabase;

		uiDispatcher.runOnUi(() -> {
			applyWorkedAndQrvStateFromDatabase(
					activeChatMembersByCallAndCategory.values(),
					finalWorkedDataFromDatabase);
			setLst_DBBasedWkdCallSignList(finalWorkedDataFromDatabase.values());
			fireUserListUpdate("Worked database state refreshed");
		});
	}

	/**
	 * Applies the worked and not-QRV state from a database snapshot to chat members.
	 * Database rows are keyed by normalized base callsign, so all active category
	 * and suffix variants receive the same persisted state.
	 *
	 * @param chatMembers members that should receive persisted state
	 * @param workedDataFromDatabase map keyed by normalized raw callsign
	 */
	/* package */ static void applyWorkedAndQrvStateFromDatabase(
			Collection<ChatMember> chatMembers,
			Map<String, ChatMember> workedDataFromDatabase
	) {
		if (chatMembers == null || workedDataFromDatabase == null) {
			return;
		}

		for (ChatMember activeChatMember : chatMembers) {
			if (activeChatMember == null) {
				continue;
			}
			ChatMember storedChatMemberState = workedDataFromDatabase.get(activeChatMember.getCallSignRaw());

			if (storedChatMemberState == null) {
				continue;
			}

			activeChatMember.setWorked(storedChatMemberState.isWorked());
			activeChatMember.setWorked144(storedChatMemberState.isWorked144());
			activeChatMember.setWorked432(storedChatMemberState.isWorked432());
			activeChatMember.setWorked1240(storedChatMemberState.isWorked1240());
			activeChatMember.setWorked2300(storedChatMemberState.isWorked2300());
			activeChatMember.setWorked3400(storedChatMemberState.isWorked3400());
			activeChatMember.setWorked5600(storedChatMemberState.isWorked5600());
			activeChatMember.setWorked10G(storedChatMemberState.isWorked10G());
			activeChatMember.setWorked50(storedChatMemberState.isWorked50());
			activeChatMember.setWorked70(storedChatMemberState.isWorked70());
			activeChatMember.setQrv144(storedChatMemberState.isQrv144());
			activeChatMember.setQrv432(storedChatMemberState.isQrv432());
			activeChatMember.setQrv1240(storedChatMemberState.isQrv1240());
			activeChatMember.setQrv2300(storedChatMemberState.isQrv2300());
			activeChatMember.setQrv3400(storedChatMemberState.isQrv3400());
			activeChatMember.setQrv5600(storedChatMemberState.isQrv5600());
			activeChatMember.setQrv10G(storedChatMemberState.isQrv10G());
			activeChatMember.setQrv50(storedChatMemberState.isQrv50());
			activeChatMember.setQrv70(storedChatMemberState.isQrv70());
		}
	}

	/**
	 * Resets all worked flags in the live GUI chatmember list.
	 */
	public void resetWorkedInfoInGuiLists() {

		this.chatController.getLst_chatMemberList().snapshot().forEach(
				chatMember -> chatMember.resetWorkedInformationAtAllBands());
	}

	/**
	 * Resets all not-QRV flags in the live GUI chatmember list.
	 */
	public void resetQRVInfoInGuiLists() {

		this.chatController.getLst_chatMemberList().snapshot().forEach(
				chatMember -> chatMember.resetQRVInformationAtAllBands());
	}

	/**
	 * Resets both worked and not-QRV flags in the live GUI chatmember list.
	 */
	public void resetWorkedAndQrvInfoInGuiLists() {
		resetWorkedInfoInGuiLists();
		resetQRVInfoInGuiLists();
	}



	
//	public void resetWorkedInfoInGuiLists() {
//
//		this.chatController.getLst_chatMemberList().forEach(
//			chatMember -> chatMember.resetWorkedInformationAtAllBands());
//
//	}
//
//	public void resetQRVInfoInGuiLists() {
//
//		this.chatController.getLst_chatMemberList().forEach(
//				chatMember -> chatMember.resetQRVInformationAtAllBands());
//
//	}
	
	/**
	 * Setting the initial parameters at the chat
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void initialize23000() throws InterruptedException, IOException {

		ChatMessage message = new ChatMessage();

//		message.setDirectedToServer(true);
		message.setMessageText(ownChatMemberObject.getCallSign());

		this.getMessageTXBus().add(message);

		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText(password);
		this.getMessageTXBus().add(message);
//    	
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText(chatCategoryMain + "");
		this.getMessageTXBus().add(message);
//    	
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText("/set qra " + ownChatMemberObject.getQra());
		this.getMessageTXBus().add(message);
//    
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText("/set name " + ownChatMemberObject.getName());
		this.getMessageTXBus().add(message);
//    	
		message = new ChatMessage();
//		message.setDirectedToServer(true);
		message.setMessageText("/set here");
		this.getMessageTXBus().add(message);

//		message = new ChatMessage();
//		message.setDirectedToServer(true);
//		message.setMessageText("/show user");
//		this.getMessageTXBus().add(message);
		// will done by another Thread
	}

	public static final class UiReminderEvent {
		private final String callSignRaw;
		private final int minutesBefore;
		private final long epochMs;

		public UiReminderEvent(String callSignRaw, int minutesBefore, long epochMs) {
			this.callSignRaw = callSignRaw;
			this.minutesBefore = minutesBefore;
			this.epochMs = epochMs;
		}

		public String getCallSignRaw() { return callSignRaw; }
		public int getMinutesBefore() { return minutesBefore; }
		public long getEpochMs() { return epochMs; }
	}

	public void fireUiReminderEvent(String callSignRaw, int minutesBefore) {
		final String raw = callSignRaw == null ? null : callSignRaw.trim().toUpperCase();
		final long now = System.currentTimeMillis();

		// Ensure property updates happen on FX thread
		if (uiDispatcher.isUiThread()) {
			lastUiReminderEvent.set(new UiReminderEvent(raw, minutesBefore, now));
		} else {
			uiDispatcher.runOnUi(() -> lastUiReminderEvent.set(new UiReminderEvent(raw, minutesBefore, now)));
		}
	}


	private final MutableValue<UiReminderEvent> lastUiReminderEvent = new SimpleValue<>(null);

	/** The most recent sked reminder; {@code null} until the first one fires. */
	public kst4contest.observe.ObservableValue<UiReminderEvent> lastUiReminderEvent() {
		return lastUiReminderEvent;
	}

	/**
	 * Helper method to check if a chatmember is in my beam(range)
	 *
	 * @param member
	 * @return
	 */
	public boolean isChatMemberInMyBeam(ChatMember member) {
		if (member == null || member.getQTFdirection() == null) return false;

		double targetAz = member.getQTFdirection();
		double myAz = getChatPreferences().getActualQTF().get();
		double beamWidth = getChatPreferences().getStn_antennaBeamWidthDeg();

		return DirectionUtils.isAngleInRange(targetAz, myAz, beamWidth);
	}

	/**
	 * Checks whether a message belongs to a monitored base callsign.
	 *
	 * <p>QSO monitoring intentionally combines all visible ON4KST variants of
	 * the same station. An entry for DN9APW therefore also matches DN9APW-2,
	 * DN9APW-70 and other suffixes. This affects monitoring only; message
	 * routing continues to use the complete callsign and chat category.</p>
	 *
	 * @param chatMessage message to examine
	 * @return true if sender or receiver belongs to a monitored base callsign
	 */
	public boolean isSniffedMessage(ChatMessage chatMessage) {

		if (chatMessage == null
				|| chatMessage.getSender() == null
				|| chatMessage.getReceiver() == null) {
			return false;
		}

		String senderCall =
				chatMessage.getSender().getCallSign();
		String receiverCall =
				chatMessage.getReceiver().getCallSign();

		if (senderCall == null || receiverCall == null) {
			return false;
		}

		if (lstNotify_QSOSniffer_sniffedCallSignList == null
				|| lstNotify_QSOSniffer_sniffedCallSignList.size() == 0) {
			return false;
		}

		String senderBaseCall =
				ChatMember.normalizeCallSignToBaseCallSign(
						senderCall
				);
		String receiverBaseCall =
				ChatMember.normalizeCallSignToBaseCallSign(
						receiverCall
				);

		boolean observedCall =
				lstNotify_QSOSniffer_sniffedCallSignList.snapshot()
						.stream()
						.anyMatch(
								monitoredCall -> {
									String monitoredBaseCall =
											ChatMember
													.normalizeCallSignToBaseCallSign(
															monitoredCall
													);

									return monitoredBaseCall != null
											&& (
											monitoredBaseCall
													.equalsIgnoreCase(
															senderBaseCall
													)
													|| monitoredBaseCall
													.equalsIgnoreCase(
															receiverBaseCall
													)
									);
								}
						);

		if (!observedCall) {
			return false;
		}

		String myCall =
				getChatPreferences() != null
						? getChatPreferences().getStn_loginCallSign()
						: null;
		String myRawCall =
				getChatPreferences() != null
						? getChatPreferences().getStn_loginCallSignRaw()
						: null;

		/*
		 * Messages which are already addressed directly to the local station
		 * belong in the PM table without an additional Sniffed marker.
		 */
		boolean directedToOwnCall =
				(myCall != null
						&& receiverCall.equalsIgnoreCase(myCall))
						|| (myRawCall != null
						&& receiverCall.equalsIgnoreCase(myRawCall));

		return !directedToOwnCall;
	}

	/**
	 * changes the chatmessage if it had been a sniffed one and not directed to me. Only for marking.
	 * @param chatMessage
	 * @return
	 */
	public String formatChatMessageTextForDisplay(ChatMessage chatMessage) {
		if (chatMessage == null) {
			return "";
		}

		String msgText = chatMessage.getMessageText();

		if (msgText == null) {
			msgText = "";
		}

		if (!isSniffedMessage(chatMessage)) {
			return msgText;
		}

		String senderCall = chatMessage.getSender() != null ? chatMessage.getSender().getCallSign() : "";
		String receiverCall = chatMessage.getReceiver() != null ? chatMessage.getReceiver().getCallSign() : "";

		return "Sniffed: (" + senderCall + " > " + receiverCall + ") " + msgText;
	}
}
