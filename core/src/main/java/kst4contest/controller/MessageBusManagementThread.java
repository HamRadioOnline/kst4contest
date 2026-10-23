package kst4contest.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
//import java.net.Socket;
//import java.util.ArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

import kst4contest.ApplicationConstants;
import kst4contest.locatorUtils.DirectionUtils;
import kst4contest.locatorUtils.Location;
import kst4contest.model.*;
import kst4contest.logic.FrequencyTextParser;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import kst4contest.observe.SimpleValue;

/**
 *
 * This thread is responsible for processing received messages.
 * It checks all messages from server for their functional contest, such as commands to build or change the userlist
 * or their settings, get clustermessages and sure the content of all chatmessages, which are delivered.
 *
 */
public class MessageBusManagementThread extends Thread {
	private static final Logger LOGGER =
			Logger.getLogger(MessageBusManagementThread.class.getName());

	int index;

	private String ThreadNickName = "MessageBus";
	private ThreadStatusCallback callBackToController;

	private PrintWriter writer;
	//	private Socket socket;
	private ChatController client;

	private final long connectionSessionId;
	private final LinkedBlockingQueue<ChatMessage> receiveQueue;
	private final LongPredicate connectionSessionIsActive;

	//	private File fileLogRAW;
//	private TimerTask userActualizationTask; // Is used as a temporary userout-print
//	private TimerTask userActualizationTask; //kst4contest.test 4 23001

	/*
	 * Frequency formats handled by the smart parser:
	 *
	 * Group 1: full frequencies, for example 144.210, 144210 or 10368100
	 * Group 2: relative frequencies with a separator, for example .210 or ,210
	 * Group 3: bare three-digit values, for example 210
	 *
	 * Group 3 is deliberately subjected to an additional context check. Without
	 * that check, ordinary chat values such as 599 or a bare band name such as 144
	 * would be converted into plausible but incorrect frequencies.
	 */
	private static final Pattern SMART_FREQUENCY_PATTERN = Pattern.compile(
			"(?<![A-Z0-9])(\\d{2,5}[.,]\\d{1,3}(?:[.,]\\d{1,3})?|\\d{5,8})(?![A-Z0-9])"
					+ "|(?<![\\d])([.,]\\d{3}(?:[.,]\\d{1,3})?)(?![\\d])"
					+ "|(?<=\\s|^)(\\d{3})(?=\\s|$)"
	);

	/*
	 * A bare three-digit value is accepted only when the nearby text makes its
	 * meaning sufficiently clear. Examples:
	 *
	 * qrg 210
	 * QRG: 210
	 * freq is 210
	 * on 210
	 * pse 210
	 * at 210
	 * 210 MHz
	 * 210 qrg
	 */
	private static final Pattern BARE_FREQUENCY_PREFIX_CONTEXT_PATTERN =
			Pattern.compile(
					"(?i)\\b(?:qrg|freq(?:uency)?|on|pse|at)\\b"
							+ "\\s*(?:is\\s*)?[:=@-]?\\s*$"
			);

	private static final Pattern BARE_FREQUENCY_SUFFIX_CONTEXT_PATTERN =
			Pattern.compile(
					"(?i)^\\s*(?:mhz|qrg|freq(?:uency)?)\\b"
			);

	private static final int BARE_FREQUENCY_CONTEXT_CHARACTERS = 32;


	// ==== Auto-answer flood/ping-pong protection ====
	private static final String AUTOANSWER_PREFIX = ApplicationConstants.AUTOANSWER_PREFIX;
	static final long AUTOANSWER_COOLDOWN_MS = 120_000L; // two minutes

	/*
	 * Lower-case fragments that identify a question for the local QRG. An incoming
	 * private message is matched case-insensitively against each of them.
	 */
	static final Set<String> QRG_QUESTION_TEXTS = Set.of(
			"ur qrg?",
			"your qrg?",
			"qrg?",
			"freq?",
			"pse qrg"
	);

	// Cooldown per remote station and chat category; updated only after this client sends.
	private final Hashtable<String, Long> lastLocalAutoAnswerPerRemoteMs = new Hashtable<>();

	// Wall clock for the autoanswer cooldown; injectable for tests.
	private final LongSupplier millisClock;
//	BufferedWriter bufwrtrDBGMSGOut;

//	    private String text;

//	public BufferedWriter getBufwrtrDBGMSGOut() {
//		return bufwrtrDBGMSGOut;
//	}
//
//	public void setBufwrtrDBGMSGOut(BufferedWriter bufwrtrDBGMSGOut) {
//		this.bufwrtrDBGMSGOut = bufwrtrDBGMSGOut;
//	}

	public MessageBusManagementThread(ChatController client, ThreadStatusCallback callBack) {
		this(client, callBack, 0L, client.getMessageRXBus(), ignored -> true);
	}

	public MessageBusManagementThread(
			ChatController client,
			ThreadStatusCallback callBack,
			long connectionSessionId,
			LinkedBlockingQueue<ChatMessage> receiveQueue,
			LongPredicate connectionSessionIsActive
	) {
		this(client, callBack, connectionSessionId, receiveQueue,
				connectionSessionIsActive, System::currentTimeMillis);
	}

	/**
	 * Full constructor with an explicit wall clock.
	 *
	 * @param millisClock source of the current time in epoch milliseconds, used for
	 *                    the autoanswer cooldown; production code passes
	 *                    {@code System::currentTimeMillis}
	 */
	MessageBusManagementThread(
			ChatController client,
			ThreadStatusCallback callBack,
			long connectionSessionId,
			LinkedBlockingQueue<ChatMessage> receiveQueue,
			LongPredicate connectionSessionIsActive,
			LongSupplier millisClock
	) {

		this.callBackToController = callBack;
		this.client = client;
		this.connectionSessionId = connectionSessionId;
		this.receiveQueue = receiveQueue;
		this.connectionSessionIsActive = connectionSessionIsActive;
		this.millisClock = millisClock;

		ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "initialized", false);
		callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

	}

	/**
	 * Detects complete and relative frequencies in a chat message and stores the
	 * result on the sender.
	 *
	 * <p>Complete frequencies determine their band directly. Relative frequencies
	 * first use the sender's most recent band context if that context is not older
	 * than 30 minutes. Only when no suitable sender context exists does the parser
	 * use the globally configured fallback band.</p>
	 *
	 * <p>A relative value beginning with a dot or comma is sufficiently explicit
	 * on its own. A bare three-digit value is ambiguous and is therefore accepted
	 * only with nearby frequency-related text.</p>
	 *
	 * @param message message whose text is inspected
	 * @param prefs preferences containing the global fallback band
	 */
	/* package */ void smartFrequencyExtraction(ChatMessage message, ChatPreferences prefs) {
		if (message == null || message.getMessageText() == null) {
			return;
		}

		ChatMember sender = message.getSender();
		if (sender == null) {
			return;
		}

		Map<Band, Long> bandContextTimestamps = new EnumMap<>(Band.class);
		if (sender.getKnownActiveBands() != null) {
			for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry
					: sender.getKnownActiveBands().entrySet()) {
				if (entry.getKey() != null && entry.getValue() != null) {
					bandContextTimestamps.put(entry.getKey(), entry.getValue().timestampEpoch);
				}
			}
		}

		String configuredPrefix = null;
		if (prefs != null
				&& prefs.getNotify_optionalFrequencyPrefix() != null) {
			configuredPrefix =
					prefs.getNotify_optionalFrequencyPrefix().get();
		}

		List<DetectedChatFrequency> detectedFrequencies = detectFrequencies(
				message.getMessageText(),
				bandContextTimestamps,
				configuredPrefix,
				millisClock.getAsLong()
		);

		for (DetectedChatFrequency detected : detectedFrequencies) {
			/*
			 * Store the result in the thread-safe active-member model. The existing
			 * compatibility property used by the TableView and DX-Cluster code is
			 * updated by applyDetectedFrequencyToActiveMembers(...), too.
			 */
			client.applyDetectedFrequencyToActiveMembers(
					sender,
					detected.band(),
					detected.frequencyMHz()
			);

			LOGGER.log(Level.FINE,
					"[SmartParser] Detected for "
							+ sender.getCallSign()
							+ ": "
							+ detected.frequencyMHz()
							+ " MHz ("
							+ detected.band()
							+ ") "
							+ (detected.derivedFromShortForm()
							? "[derived from " + detected.rawMatch() + "]"
							: "[full match]")
			);
		}
	}

	/**
	 * One frequency found in a chat message by
	 * {@link #detectFrequencies(String, Map, String, long)}.
	 *
	 * @param band band the frequency belongs to
	 * @param frequencyMHz frequency in MHz, always greater than zero
	 * @param rawMatch matched text with commas replaced by dots and, for a relative
	 *                 value, without its leading dot
	 * @param derivedFromShortForm {@code true} when the value was relative
	 *                             ({@code .210}) or bare ({@code 210}) and the band
	 *                             had to be taken from context
	 */
	record DetectedChatFrequency(
			Band band,
			double frequencyMHz,
			String rawMatch,
			boolean derivedFromShortForm
	) {
	}

	/**
	 * Detects complete and relative frequencies in a chat message, in the order in
	 * which they occur.
	 *
	 * <p>Complete frequencies determine their band directly. A relative or bare
	 * value is completed with the band whose most recent observation is the newest
	 * among those not older than 30 minutes ({@code nowMillis - timestamp <= 1_800_000})
	 * and whose band range contains the completed frequency. Only when no such band
	 * exists is the configured fallback prefix used; an unknown or missing prefix
	 * falls back to 144 MHz.</p>
	 *
	 * <p>Every detection is fed back as band context with timestamp
	 * {@code nowMillis} before the next match is examined. This reproduces the
	 * production path, where storing a detection on the sender updates the very
	 * map the next match reads: in {@code "10368.100 then .150"} the second value
	 * becomes 10368.150, not 144.150.</p>
	 *
	 * <p>A bare three-digit value is accepted only with nearby frequency-related
	 * text, see {@link #hasExplicitBareFrequencyContext(String, int, int)}.</p>
	 *
	 * @param messageText chat message text, may be {@code null}
	 * @param bandContextTimestamps epoch milliseconds of the sender's most recent
	 *                              observation per band; not modified; may be
	 *                              {@code null}
	 * @param fallbackPrefix configured fallback band prefix such as {@code "432"},
	 *                       may be {@code null}
	 * @param nowMillis current time in epoch milliseconds
	 * @return detected frequencies; never {@code null}
	 */
	static List<DetectedChatFrequency> detectFrequencies(
			String messageText,
			Map<Band, Long> bandContextTimestamps,
			String fallbackPrefix,
			long nowMillis
	) {
		List<DetectedChatFrequency> result = new ArrayList<>();

		if (messageText == null) {
			return result;
		}

		Map<Band, Long> bandContext = new EnumMap<>(Band.class);
		if (bandContextTimestamps != null) {
			for (Map.Entry<Band, Long> entry : bandContextTimestamps.entrySet()) {
				if (entry.getKey() != null && entry.getValue() != null) {
					bandContext.put(entry.getKey(), entry.getValue());
				}
			}
		}

		Matcher matcher = SMART_FREQUENCY_PATTERN.matcher(messageText);

		while (matcher.find()) {
			boolean dottedShortForm = matcher.group(2) != null;
			boolean bareShortForm = matcher.group(3) != null;
			boolean shortForm = dottedShortForm || bareShortForm;

			if (bareShortForm
					&& !hasExplicitBareFrequencyContext(
					messageText,
					matcher.start(),
					matcher.end()
			)) {
				continue;
			}

			String foundRaw = matcher.group().trim().replace(',', '.');

			double finalDetectedFrequency = 0.0;
			Band finalDetectedBand = null;

			if (shortForm) {
				if (foundRaw.startsWith(".")) {
					foundRaw = foundRaw.substring(1);
				}

				/*
				 * Priority 1: use this sender's most recently observed band if its
				 * information is not older than 30 minutes and the reconstructed
				 * frequency lies inside that band.
				 */
				long bestTimestamp = 0L;

				for (Map.Entry<Band, Long> entry : bandContext.entrySet()) {
					Band candidateBand = entry.getKey();
					long timestamp = entry.getValue();

					if (nowMillis - timestamp > 1_800_000L) {
						continue;
					}

					FrequencyTextParser.DetectedFrequency detectedFrequency =
							FrequencyTextParser.parseExplicitFrequency(
									candidateBand.getPrefix() + "." + foundRaw
							);

					if (detectedFrequency != null
							&& detectedFrequency.getBand() == candidateBand
							&& timestamp > bestTimestamp) {

						finalDetectedFrequency =
								detectedFrequency.getFrequencyMHz();

						finalDetectedBand =
								candidateBand;

						bestTimestamp =
								timestamp;
					}
				}

				/*
				 * Priority 2: use the configured fallback band. Invalid values from
				 * an older hand-edited configuration fall back safely to 144 MHz.
				 * The current UI itself offers only values from Band.values().
				 */
				if (finalDetectedBand == null) {
					Band fallbackBand = Band.fromPrefix(fallbackPrefix);
					if (fallbackBand == null) {
						fallbackBand = Band.B_144;
					}

					FrequencyTextParser.DetectedFrequency detectedFrequency =
							FrequencyTextParser.parseExplicitFrequency(
									fallbackBand.getPrefix() + "." + foundRaw
							);

					if (detectedFrequency != null
							&& detectedFrequency.getBand() == fallbackBand) {

						finalDetectedFrequency =
								detectedFrequency.getFrequencyMHz();

						finalDetectedBand =
								fallbackBand;
					}
				}
			} else {
				FrequencyTextParser.DetectedFrequency detectedFrequency =
						FrequencyTextParser.parseExplicitFrequency(
								foundRaw
						);

				if (detectedFrequency != null) {
					finalDetectedFrequency =
							detectedFrequency.getFrequencyMHz();

					finalDetectedBand =
							detectedFrequency.getBand();
				}
			}

			if (finalDetectedBand == null || finalDetectedFrequency <= 0.0) {
				continue;
			}

			result.add(new DetectedChatFrequency(
					finalDetectedBand,
					finalDetectedFrequency,
					foundRaw,
					shortForm
			));

			// The caller stores this detection on the sender, which makes it the
			// newest context for the remaining matches of the same message.
			bandContext.put(finalDetectedBand, nowMillis);
		}

		return result;
	}


	/**
	 * Checks whether a bare three-digit value is surrounded by text that identifies
	 * it as a frequency.
	 *
	 * <p>The check intentionally uses only a small area around the value. A remote
	 * occurrence of the word "QRG" elsewhere in a long message must not turn every
	 * three-digit number in that message into a frequency.</p>
	 *
	 * @param messageText complete chat message
	 * @param matchStart start index of the three-digit match
	 * @param matchEnd end index of the three-digit match
	 * @return {@code true} if the value has explicit frequency context
	 */
	static boolean hasExplicitBareFrequencyContext(
			String messageText,
			int matchStart,
			int matchEnd
	) {
		if (messageText == null
				|| matchStart < 0
				|| matchEnd < matchStart
				|| matchEnd > messageText.length()) {
			return false;
		}

		int prefixStart = Math.max(
				0,
				matchStart - BARE_FREQUENCY_CONTEXT_CHARACTERS
		);
		int suffixEnd = Math.min(
				messageText.length(),
				matchEnd + BARE_FREQUENCY_CONTEXT_CHARACTERS
		);

		String prefixContext = messageText.substring(prefixStart, matchStart);
		String suffixContext = messageText.substring(matchEnd, suffixEnd);

		return BARE_FREQUENCY_PREFIX_CONTEXT_PATTERN
				.matcher(prefixContext)
				.find()
				|| BARE_FREQUENCY_SUFFIX_CONTEXT_PATTERN
				.matcher(suffixContext)
				.find();
	}

	/**
	 * Helper: Normalizes weird frequency formats to valid Double strings.
	 * Example: "144.210.10" -> "144.21010"
	 * Example: "144.210"    -> "144.210"
	 */
//	private String normalizeFrequencyString(String rawInput) {
//		// Input is already guaranteed to have only dots as separators (commas replaced earlier)
//
//		int firstDotIndex = rawInput.indexOf(".");
//
//		if (firstDotIndex != -1) {
//			// Check if there are more dots after the first one
//			String decimalPart = rawInput.substring(firstDotIndex + 1);
//			if (decimalPart.contains(".")) {
//				// Remove all subsequent dots to make it a valid double
//				decimalPart = decimalPart.replace(".", "");
//				return rawInput.substring(0, firstDotIndex) + "." + decimalPart;
//			}
//		}
//		return rawInput;
//	}


	/**
	 * Creates a minimal receiver object for ON4KST broadcast messages.
	 * Keeping this as an object, not null, protects all downstream chat filters
	 * and debug logging from null receiver paths.
	 */
	private ChatMember createAllReceiver() {
		ChatMember dummy = new ChatMember();
		dummy.setCallSign("ALL");
		dummy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
		return dummy;
	}

	/**
	 * Resolves a message sender from the thread-safe active-member model.
	 *
	 * <p>The local login is handled before the active-user lookup because the own
	 * ChatMember is intentionally not stored in the visible user list. This also
	 * prevents another active login with the same base callsign from replacing the
	 * identity of our own message echo.</p>
	 */
	private ChatMember resolveInboundSender(
			String senderCallSign,
			ChatCategory category,
			ChatMessage message
	) {

		String myCall =
				this.client.getChatPreferences().getStn_loginCallSign();

		if (senderCallSign != null
				&& myCall != null
				&& senderCallSign.equalsIgnoreCase(myCall)) {

			ChatMember ownSender = new ChatMember();
			ownSender.setCallSign(senderCallSign);
			ownSender.setChatCategory(category);
			ownSender.setAirPlaneReflectInfo(
					new AirPlaneReflectionInfo()
			);

			return ownSender;
		}

		ChatMember lookup = new ChatMember();
		lookup.setCallSign(senderCallSign);
		lookup.setChatCategory(category);

		ChatMember senderObj =
				this.client.findActiveChatMember(lookup);

		if (senderObj != null) {
			senderObj.setActivityTimeLastInEpoch(
					new Utils4KST().time_generateCurrentEpochTime()
			);

			this.client.rememberLastInboundCategory(
					senderObj.getCallSignRaw(),
					senderObj.getChatCategory()
			);

			this.client.getStationMetricsService().onInboundMessage(
					senderObj.getCallSignRaw(),
					System.currentTimeMillis(),
					message == null ? null : message.getMessageText(),
					this.client.getChatPreferences(),
					myCall
			);

			this.client.getScoreService().requestRecompute(
					"rx-chat-message"
			);

			return senderObj;
		}

		ChatMember fallbackSender = new ChatMember();
		fallbackSender.setCallSign("[n/a]" + senderCallSign);
		fallbackSender.setChatCategory(category);
		fallbackSender.setAirPlaneReflectInfo(
				new AirPlaneReflectionInfo()
		);

		return fallbackSender;
	}

	/**
	 * Resolves a message receiver from the thread-safe active-member model.
	 *
	 * <p>The local login is handled before the active-user lookup. The receiver
	 * from the ON4KST packet therefore remains authoritative even if another
	 * station with the same base callsign is logged into the same category.</p>
	 */
	private ChatMember resolveInboundReceiver(
			String receiverCallSign,
			ChatCategory category
	) {

		if (receiverCallSign == null
				|| receiverCallSign.equals("0")) {
			return createAllReceiver();
		}

		String myCall =
				this.client.getChatPreferences().getStn_loginCallSign();

		if (myCall != null
				&& receiverCallSign.equalsIgnoreCase(myCall)) {

			ChatMember ownReceiver = new ChatMember();
			ownReceiver.setCallSign(receiverCallSign);
			ownReceiver.setChatCategory(category);
			ownReceiver.setAirPlaneReflectInfo(
					new AirPlaneReflectionInfo()
			);

			return ownReceiver;
		}

		ChatMember lookup = new ChatMember();
		lookup.setCallSign(receiverCallSign);
		lookup.setChatCategory(category);

		ChatMember receiverObj =
				this.client.findActiveChatMember(lookup);

		if (receiverObj != null) {
			return receiverObj;
		}

		ChatMember fallbackReceiver = new ChatMember();
		fallbackReceiver.setCallSign(receiverCallSign + "(left)");
		fallbackReceiver.setChatCategory(category);
		fallbackReceiver.setAirPlaneReflectInfo(
				new AirPlaneReflectionInfo()
		);

		return fallbackReceiver;
	}

	private boolean validateInboundUserFrame(String[] fields) {
		if (fields == null || fields.length < 6) {
			logRejectedInboundUserFrame(
					"Ignoring truncated ON4KST user frame", fields);
			return false;
		}
		if (fields[2] == null || fields[2].isBlank()) {
			logRejectedInboundUserFrame(
					"Ignoring ON4KST user frame with empty callsign", fields);
			return false;
		}
		try {
			On4KstProtocol.category(Integer.parseInt(fields[1]));
			On4KstProtocol.locator(fields[4]);
			Integer.parseInt(fields[5]);
			return true;
		} catch (IllegalArgumentException invalidUser) {
			logRejectedInboundUserFrame(
					"Ignoring malformed ON4KST user '" + fields[2]
							+ "': " + invalidUser.getMessage(), fields);
			return false;
		}
	}

	/**
	 * Records a rejected user frame without writing the complete raw frame or user
	 * name to the diagnostic log.
	 *
	 * <p>The opcode, category and callsign are sufficient to identify the offending
	 * list position. Omitting the remaining fields avoids unnecessary disclosure of
	 * free-form profile text.</p>
	 */
	private void logRejectedInboundUserFrame(String reason, String[] fields) {
		String opcode = fields != null && fields.length > 0 ? fields[0] : "UNKNOWN";
		String category = fields != null && fields.length > 1 ? fields[1] : "UNKNOWN";
		String callsign = fields != null && fields.length > 2 ? fields[2] : "UNKNOWN";
		client.onOn4KstConnectionWarning(
				reason + "; opcode=" + opcode
						+ ", category=" + category
						+ ", callsign=" + callsign
						+ ", fieldCount=" + (fields == null ? 0 : fields.length));
	}

	/**
	 * Processes one ON4KST chat frame: {@code CH} (live) or {@code CR} (history).
	 *
	 * <p>Both share sender and receiver resolution, routing and publishing. The
	 * differences are kept exactly as they were in the two former branches:
	 * a live SERVER sender gets an empty AirScout info, a historic one does not;
	 * live frames detect the sender's frequency before routing, historic frames
	 * after publishing and the diagnostic log; historic frames never play sounds,
	 * answer, tick or spot. Which side effects apply is decided by
	 * {@link #decideChatFrameEffects}; this method only executes them.</p>
	 *
	 * <p>The fields are read in the same order as before, so a truncated frame
	 * fails at the same point, after the same side effects.</p>
	 *
	 * @param fields the frame split on {@code |}
	 * @param live {@code true} for {@code CH}, {@code false} for {@code CR}
	 */
	private void processChatFrame(String[] fields, boolean live) {

		ChatPreferences prefs = this.client.getChatPreferences();

		ChatMessage newMessageArrived = new ChatMessage();
		ChatCategory chategoryForMessageAndMessageSender;

		newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(fields[1]));

		chategoryForMessageAndMessageSender = newMessageArrived.getChatCategory();
		newMessageArrived.setMessageGeneratedTime(fields[2]);
		newMessageArrived.setMessageSenderName(fields[4]);
		newMessageArrived.setMessageText(fields[6]);

		if (fields[3].equals("SERVER")) {
			ChatMember dummy = new ChatMember();
			if (live) {
				dummy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
			}
			dummy.setCallSign("SERVER");
			dummy.setName("Sysop");
			newMessageArrived.setSender(dummy);
			newMessageArrived.setChatCategory(util_getChatCategoryByCategoryNrString(fields[1]));
			dummy.setChatCategory(util_getChatCategoryByCategoryNrString(fields[1]));

		} else {

			newMessageArrived.setSender(resolveInboundSender(
					fields[3],
					chategoryForMessageAndMessageSender,
					newMessageArrived));
		}

		if (live) {
			/*
			 * Detect and store the sender's QRG before any directional-opportunity
			 * or DX-Cluster processing. A frequency first mentioned in this directed
			 * message is then immediately available for the resulting spot.
			 *
			 * The parser needs only the message text, sender and preferences. The
			 * receiver does not have to be resolved yet.
			 */
			smartFrequencyExtraction(newMessageArrived, prefs);
		}

		// Built only now: the receiver field has always been read after the sender.
		InboundChatFrame frame = InboundChatFrame.of(fields);
		boolean publicMessage = frame.receiverField().equals("0");
		newMessageArrived.setMessageDirectedToCommunity(publicMessage);
		newMessageArrived.setMessageDirectedToMe(
				publicMessage
						&& isPublicMessageExplicitlyAddressedTo(
								frame.text(),
								prefs.getStn_loginCallSign()));

		if (publicMessage) {
			// message is not directed to anyone, move it to the cq messages.
			newMessageArrived.setReceiver(createAllReceiver());

			this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)

		} else {
			//message is directed to another chatmember, process as such!

			newMessageArrived.setReceiver(resolveInboundReceiver(
					frame.receiverField(),
					chategoryForMessageAndMessageSender));

			try {
				ChatRoute route = classifyRoute(
						frame.receiverField(),
						newMessageArrived.getSender().getCallSign(),
						newMessageArrived.getReceiver().getCallSign(),
						prefs.getStn_loginCallSign());
				newMessageArrived.setMessageDirectedToMe(route == ChatRoute.TO_ME);

				DirectionalOpportunity opportunity = null;
				boolean senderAlreadyInAngleAndRange = false;
				if (route == ChatRoute.BETWEEN_OTHERS) {
					/*
					 * With explicit fallback sender/receiver objects, this path may also see
					 * stations that already left the chat or arrived before their user-list
					 * entry. Their locators may be missing, so angle/range analysis and DX
					 * spotting are skipped while the chat message itself is still published.
					 */
					opportunity = classifyDirectionalOpportunity(
							prefs.getStn_loginLocatorMainCat(),
							newMessageArrived.getSender().getQra(),
							newMessageArrived.getReceiver().getQra(),
							prefs.getStn_maxQRBDefault(),
							prefs.getStn_antennaBeamWidthDeg());
					senderAlreadyInAngleAndRange = newMessageArrived.getSender().isInAngleAndRange();
				}

				// Built only where it was built before: it logs when no reply is possible.
				String autoAnswerTextCandidate = null;
				if (route == ChatRoute.TO_ME && live && !isAutoMessage(newMessageArrived.getMessageText())) {
					boolean qrgRequested =
							prefs.isMessageHandling_autoAnswerToQRGRequestEnabled()
									&& matchesQrgQuestion(newMessageArrived.getMessageText(), QRG_QUESTION_TEXTS);

					ChatMember incomingSender = newMessageArrived.getSender();

					autoAnswerTextCandidate = buildAutoAnswerMessageText(
							incomingSender == null ? null : incomingSender.getCallSign(),
							// Diagnostics only: same fallback as autoAnswerCooldownKey.
							newMessageArrived.getChatCategory() != null || incomingSender == null
									? newMessageArrived.getChatCategory()
									: incomingSender.getChatCategory(),
							qrgRequested,
							selectAutoAnswerQrg(
									newMessageArrived.getChatCategory(),
									this.client.getChatCategorySecondChat(),
									prefs.getMYQRGFirstCat().get(),
									prefs.getMYQRGSecondCat().get()
							),
							prefs.isMsgHandling_autoAnswerEnabled(),
							prefs.getMessageHandling_autoAnswerTextMainCat()
					);
				}

				ChatMessageEffects effects = decideChatFrameEffects(
						route,
						live,
						newMessageArrived.getMessageText(),
						opportunity,
						senderAlreadyInAngleAndRange,
						new ChatFrameSettings(
								prefs.isNotify_playSimpleSounds(),
								prefs.isNotify_playCWCallsignsOnRxedPMs(),
								prefs.isNotify_playVoiceCallsignsOnRxedPMs(),
								prefs.isNotify_dxClusterServerEnabled(),
								autoAnswerTextCandidate));

				if (route == ChatRoute.TO_ME) {
					/**
					 * message is directed to me, will be put in the "to me" messagelist
					 */
					this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)

					if (effects.playPmSound()) {
						this.client.getPlayAudioUtils().playNoiseLauncher('P');
					}
					if (effects.playCwCallsign()) {
						this.client.getPlayAudioUtils().playCWLauncher(" " + " " + newMessageArrived.getSender().getCallSign().toUpperCase());
					}
					if (effects.playVoiceCallsign()) {
						this.client.getPlayAudioUtils().playVoiceLauncher( "?" + newMessageArrived.getSender().getCallSign().toUpperCase());
					}
					if (effects.playBell()) {
						this.client.getPlayAudioUtils().playVoiceLauncher("!");
					}
					if (effects.sendVersionReply()) {

						ChatMessage versionInfo = new ChatMessage();
						ChatMember itsMe = new ChatMember();
						itsMe.setCallSign(prefs.getStn_loginCallSign());

						versionInfo.setSender(itsMe);
						versionInfo.setReceiver(newMessageArrived.getSender());
						versionInfo.setChatCategory(newMessageArrived.getChatCategory());
						versionInfo.setMessageText("/CQ " + newMessageArrived.getSender().getCallSign()
								+ " " + AUTOANSWER_PREFIX
								+ " KST4Contest v" + ApplicationConstants.APPLICATION_CURRENT_VERSION
								+ " by DO5AMF");

						this.client.getMessageTXBus().add(versionInfo);
					}

					/*
					 * Unified auto-answer (generic + QRG) with ping-pong guard and
					 * per-remote cooldown. Invalid or incomplete replies are rejected
					 * before the cooldown is checked or updated. A missing QRG must
					 * therefore not suppress a later valid reply.
					 */
					if (effects.autoAnswerText() != null
							&& isAutoAnswerAllowedNow(newMessageArrived)) {

						ChatMessage automaticAnswer = new ChatMessage();
						ChatMember itsMe = new ChatMember();

						itsMe.setCallSign(prefs.getStn_loginCallSign());

						automaticAnswer.setSender(itsMe);
						automaticAnswer.setReceiver(newMessageArrived.getSender());
						automaticAnswer.setChatCategory(newMessageArrived.getChatCategory());
						automaticAnswer.setMessageText(effects.autoAnswerText());

						this.client.getMessageTXBus().add(automaticAnswer);

						/*
						 * Record the cooldown only after a complete and locally
						 * validated reply has been placed in the transmit queue.
						 */
						markLocalAutoAnswerSent(newMessageArrived);
					}

					LOGGER.fine((live
							? "Message directed to local callsign: "
							: "Historic message directed to local callsign: ")
							+ newMessageArrived.getReceiver().getCallSign());

				} else if (route == ChatRoute.FROM_ME) {
					/**
					 * message sent by me!
					 * message from me will appear in the PM window, too, with (>CALLSIGN) before
					 */
					String originalMessage = newMessageArrived.getMessageText();
					newMessageArrived
							.setMessageText("(>" + newMessageArrived.getReceiver().getCallSign() + ")" + originalMessage);
					this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)

					// if you sent the message to another station, it will be sorted in to
					// the "to me message list" with modified messagetext, added rxers callsign

				} else {
					// Message sent from one other station to another other station.

					//play only tick sound if the sender was not set directedtome before
					if (effects.playDirectionalTick()) {
						this.client.getPlayAudioUtils().playNoiseLauncher('-');
					}

					// The log statements keep their former position relative to the flag change.
					if (live && opportunity == DirectionalOpportunity.OUT_OF_ANGLE_OR_RANGE) {
						LOGGER.fine("Not in angle " +  newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getSender().getQra() + " -> " + newMessageArrived.getReceiver().getCallSign() + ", " + newMessageArrived.getReceiver().getQra() + " = " +
								new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(newMessageArrived.getReceiver().getQra())) +
								" ; sender bearing to me: " + new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(prefs.getStn_loginLocatorMainCat())));
					}

					newMessageArrived.getSender().setInAngleAndRange(effects.inAngleAndRange());

					if (effects.evaluateDxClusterSpot()) {
						try {
							ChatMember sender = newMessageArrived.getSender();

							DxClusterSpotDecision spotDecision = decideDxClusterSpot(sender);

							if (spotDecision == DxClusterSpotDecision.SPOT) {
								/*
								 * The DX Cluster spot must not depend on AirScout data. A known
								 * frequency and a valid directional opportunity are sufficient.
								 * Available AP information is added only as an optional comment.
								 */
								this.client
										.getDxClusterServer()
										.broadcastSingleDXClusterEntryToLoggers(
												buildDxClusterSpotEntry(sender)
										);

							} else if (spotDecision == DxClusterSpotDecision.ALREADY_WORKED) {
								// Only reached when the worked check was the reason (issue #91).
								LOGGER.fine("DX Cluster spot suppressed, station already worked on this band: "
										+ sender.getCallSign());
							}
						} catch (Exception exception) {
							LOGGER.log(Level.WARNING,
									"DXCluster message-server error while processing spot for "
											+ newMessageArrived.getSender().getCallSign(),
									exception);
						}
					}

					if (live && opportunity == DirectionalOpportunity.IN_ANGLE_AND_RANGE) {
						LOGGER.fine("Angle warning " +  newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getSender().getQra() + " -> " + newMessageArrived.getReceiver().getCallSign() + ", " + newMessageArrived.getReceiver().getQra() + " = " +
								new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(newMessageArrived.getReceiver().getQra())) +
								" / sender bearing to me: " + new Location(newMessageArrived.getSender().getQra()).getBearing(new Location(prefs.getStn_loginLocatorMainCat())));
					} else if (live && opportunity == DirectionalOpportunity.MISSING_LOCATOR) {
						LOGGER.info("Skipping angle/range analysis for message with missing locator: "
								+ newMessageArrived.getSender().getCallSign() + " -> "
								+ newMessageArrived.getReceiver().getCallSign());
					}

					this.client.publishChatMessage(newMessageArrived); // sdtout to all message-List (new from v1.7)
				}
			} catch (NullPointerException referenceDeletedByUserLeftChatDuringMessageprocessing) {
				LOGGER.log(Level.WARNING,
						live
								? "Referenced user disappeared while processing a message"
								: "Referenced user disappeared while processing a historic message",
						referenceDeletedByUserLeftChatDuringMessageprocessing);
			}
		}

		try {

			LOGGER.fine((live ? "Processed message: " : "Processed historic message: ")
					+ newMessageArrived.getChatCategory().getCategoryNumber()
					+ " " + newMessageArrived.getSender().getCallSign() + ", " + newMessageArrived.getMessageSenderName() + " -> "
					+ newMessageArrived.getReceiver().getCallSign() + ": " + newMessageArrived.getMessageText());
		} catch (Exception exceptionOccured) {
			LOGGER.log(Level.WARNING,
					live
							? "Could not format processed-message diagnostic"
							: "Could not format historic-message diagnostic",
					exceptionOccured);
		}

		if (!live) {
			// --- Band/QRG recognition (fills ChatMember.knownActiveBands) ---
			smartFrequencyExtraction(newMessageArrived, prefs);
		}
	}


	/**
	 * Processes received messages via port 23001 (improved telnet Interface)
	 *
	 * @param messageToProcess
	 * @throws IOException
	 * @throws SQLException
	 */
	private void processRXMessage23001(ChatMessage messageToProcess) throws IOException, SQLException {

		ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "Last message processed:\n" + messageToProcess.getMessageText(), false);
		callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

		final String INITIALUSERLISTENTRY = "UA0";
		final String USERENTEREDCHAT = "UA5";
		final String USERENTEREDCHAT2 = "UA2"; // seen at 50MHZ Chat
		final String initialChatHistoryEntry = "CR";
		final String SERVERMESSAGEHISTORIC = "CR"; //takes messages out of the ON4KST history
		final String USERLEFTCHAT = "UR6";
		final String USERLEFTCHAT2 = "UR7";
		final String CHATCHANNELMESSAGE = "CH";
		final String REGISTREDUSERCOUNT = "UE";
		final String USERSTATECHANGE = "US4";
		final String USERLOCATORCHANGE = "LOC";
		final String USERINFOUPDATEORUSERISBACK = "UM3";
		final String DXCLUSTERMESSAGE1 = "DM";
		final String DXCLUSTERMESSAGE2 = "DL";
		final String DXCLUSTERMESSAGE3 = "MA";
		final String SRVR_DXCEND = "DF";
		final String SRVR_USERLISTEND = "UE";
		final String SRVR_COMMUNICATIONK = "CK";
		final String SRVR_LOGSTAT = "LOGSTAT";
		final String SRVR_LOGSTAT_WRONGPASSWORD = "Wrong password!";
		final String SRVR_LOGINOK = "100";
		final String SRVR_LOGINWRONGPW = "114";
		final String SRVR_LOGINWRONGEMPTYCALL = "102";
		final String SRVR_LOGINWRONGCALLSYNTAX = "103";
		final String SRVR_LOGINWRONGCALLUNKNOWN = "101";

		if (messageToProcess.getMessageText() == null
				|| messageToProcess.getMessageText().isEmpty()) {
			// No processable data.
		} else {
			if (On4KstProtocol.isInternalDxqResponse(
					messageToProcess.getMessageText())) {
				// Preserve the established internal handling of DXQ server data.
				return;
			}

			if (messageToProcess.getMessageText().startsWith(SRVR_LOGSTAT + "|")) {
				String[] logstatMessage =
						messageToProcess.getMessageText().split("\\|", -1);
				this.client.onOn4KstLogstat(
						connectionSessionId,
						logstatMessage);
			}

			String[] splittedMessageLine =
					messageToProcess.getMessageText().split("\\|");

			String opcode = splittedMessageLine.length == 0
					? ""
					: splittedMessageLine[0];

			if ((INITIALUSERLISTENTRY.equals(opcode)
					|| USERENTEREDCHAT.equals(opcode)
					|| USERENTEREDCHAT2.equals(opcode))
					&& !validateInboundUserFrame(splittedMessageLine)) {
				return;
			}
//			String splittedMessageLine[] = messageToProcess.getMessageText().split("\\|");

			/**
			 * Initializes the Userlist if entry fits UA0
			 * UA0|3|DL6SAQ|walter not qrv|JN58CK|1| <- RXed
			 *
			 *
			 */
			if (splittedMessageLine[0].equals(INITIALUSERLISTENTRY)) {
//				System.out.println("MSGBUS: User detected");

				ChatMember newMember = new ChatMember();

				newMember.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());

				newMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

				newMember.setCallSign(splittedMessageLine[2]);
				newMember.setName(splittedMessageLine[3]);
				newMember.setQra(splittedMessageLine[4]);
				newMember.setState(Integer.parseInt(splittedMessageLine[5]));
//				newMember.setQTFdirection(LocatorUtils);
				newMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(client.getChatPreferences().getStn_loginLocatorMainCat(), newMember.getQra()));
				newMember.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat()).getBearing(new Location(newMember.getQra())));
				newMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());//TODO evt obsolete!
				newMember.setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());


				if (!client.getChatPreferences().getStn_loginCallSign().equals(newMember.getCallSign())) {
					this.client.stageInitialOn4KstChatMember(
							connectionSessionId,
							newMember);
//					this.client.getReachabilityService().ensureAutoTropoMarginCalculated(newMember);
					// Reachability is calculated on demand only: map click, selected station, or manual request.
				}


				this.client.getDbHandler().storeChatMember(newMember);


//				bufwrtrDBGMSGOut.write(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
//						+ "[MSGBUSMGT:] User detected and added to list [" + this.client.getChatMemberTable().size()
//						+ "] :" + newMember.getCallSign() + "\n");
//				bufwrtrDBGMSGOut.flush();
//				System.out.println("[MSGBUSMGT:] User detected and added to list ["
//						+ this.client.getChatMemberTable().size() + "] :" + newMember.getCallSign());
			} else

			/**
			 * Actualize Userlist, add new entry UA5 or UA2
			 *
			 * UA5|2|IU4CHE|Giorgio 2-70-23|JN64GB|2|
			 * UA2|2|W5ADD|Parker|EM40WL|2|
			 *
			 */
				if (splittedMessageLine[0].equals(USERENTEREDCHAT)
						|| splittedMessageLine[0].equals(USERENTEREDCHAT2)) {
//				System.out.println("MSGBUS: User detected");


					if (!client.getChatPreferences().getStn_loginCallSign().equals(splittedMessageLine[2])) { //own call ignore

						ChatMember newMember = new ChatMember();

						newMember.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());

						newMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

						newMember.setCallSign(splittedMessageLine[2]);
						newMember.setName(splittedMessageLine[3]);
						newMember.setQra(splittedMessageLine[4]);
						newMember.setState(Integer.parseInt(splittedMessageLine[5]));
						newMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
						newMember.setActivityTimeLastInEpoch(new Utils4KST().time_generateCurrentEpochTime());
						newMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(client.getChatPreferences().getStn_loginLocatorMainCat(), newMember.getQra()));
						newMember.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat()).getBearing(new Location(newMember.getQra())));

						newMember = this.client.getDbHandler().fetchChatMemberWkdDataForOnlyOneCallsignFromDB(newMember);

						this.client.addOrUpdateActiveChatMember(newMember);
//					this.client.getReachabilityService().ensureAutoTropoMarginCalculated(newMember);

						this.client.getDbHandler().storeChatMember(newMember);
					}


					this.client.fireUserListUpdate("User entered the chat");

//				this.client.getChatMemberTable().put(splittedMessageLine[2], newMember);

//				System.out.println("[MSGBUSMGT:] New entered User detected and added to list ["
//						+ this.client.getChatMemberTable().size() + "] :" + newMember.getCallSign());
				} else

				/**
				 * Actualize Userlist, remove entry UR6, UR7
				 */
					if (splittedMessageLine[0].contains(USERLEFTCHAT) || splittedMessageLine[0].contains(USERLEFTCHAT2)) {
//					System.out.println("MSGBUS: User detected");

						ChatMember newMember = new ChatMember();

						newMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

						newMember.setCallSign(splittedMessageLine[2]);

						LOGGER.info("User left chat and will be removed from list ["
								+ this.client.getActiveChatMemberCount() + "] :" + newMember.getCallSign());

						if (!this.client.removeActiveChatMember(newMember)) {
							LOGGER.info("User sent left-chat frame but was not active: "
									+ newMember.getCallSign() + " / " + newMember.getChatCategory());
						}

					} else

					/**
					 * Chatmessage dm5m to do5amf CH|2|1663966534|DM5M|dm5m-team|0|kst4contest.test|DO5AMF|
					 *
					 * CH|2|1663966535|DM5M|dm5m-team|0|kst4contest.test|0|
					 */
						if (splittedMessageLine[0].contains(CHATCHANNELMESSAGE)) {
							processChatFrame(splittedMessageLine, true);
						} else

						/**
						 * LOC|1664012560|I4GHG/6|JN63DT| Actualize singleton Userlist, changes locator
						 * of existing user or add him with this locator
						 */
							if (splittedMessageLine[0].contains(USERLOCATORCHANGE)) {
//					System.out.println("MSGBUS: User detected");

								ChatMember temp4 = new ChatMember();
								temp4.setChatCategory(this.client.getChatCategoryMain()); //not really detectable and not really neccessarry to detect

								temp4.setCallSign(splittedMessageLine[2]);
								temp4.setQra(splittedMessageLine[3]);
								temp4.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());

								ChatMember foundThisInChatMemberList = this.client.findActiveChatMember(temp4);
								if (foundThisInChatMemberList == null && this.client.getChatCategorySecondChat() != null) {
									/*
									 * LOC messages do not carry the category. The old implementation checked
									 * only the main category. Try the second active category as a fallback so
									 * dual-channel operation can still update locator changes.
									 */
									temp4.setChatCategory(this.client.getChatCategorySecondChat());
									foundThisInChatMemberList = this.client.findActiveChatMember(temp4);
								}

								if (foundThisInChatMemberList != null) {
									LOGGER.info("Locator change of [" + (splittedMessageLine[2] + "], old was: "
											+ foundThisInChatMemberList.getQra() + " new is: " + splittedMessageLine[3]));
									this.client.updateActiveChatMemberLocator(temp4, splittedMessageLine[3]);
								} else {
									LOGGER.warning("Locator change of ["
											+ (splittedMessageLine[2] + "] is not possible, user is not in the Table!"));

								}

								this.client.getDbHandler().storeChatMember(temp4); // TODO thats a bit unclean, its less an insert but a
								// locator update

							} else

							/**
							 * DX-Cluster-Message type 1
							 * DM|0|1664050013|2006|w4cwf|144118.0|PA2CHR|EM85WH<>JO22 hrd
							 * -21db|EM85WH|JO32DB|
							 */
								if (splittedMessageLine[0].contains(DXCLUSTERMESSAGE1)) {
//					System.out.println("MSGBUS: User detected");

									ClusterMessage dxcMsg = new ClusterMessage();

									dxcMsg.setTimeGenerated(splittedMessageLine[2]);

									ChatMember newDXCListSender = new ChatMember();
									newDXCListSender.setCallSign(splittedMessageLine[4]);
									newDXCListSender.setQra(splittedMessageLine[8]);

									ChatMember newDXCListReceiver = new ChatMember();
									newDXCListReceiver.setFrequency(new SimpleValue<>(splittedMessageLine[5]));
									newDXCListReceiver.setCallSign(splittedMessageLine[6]);
									newDXCListReceiver.setQra(splittedMessageLine[9]);

									dxcMsg.setSender(newDXCListSender);
									dxcMsg.setReceiver(newDXCListReceiver);

									dxcMsg.setMessageInhibited(splittedMessageLine[7]);
									dxcMsg.setQrgSpotted(splittedMessageLine[5]);

//				this.client.getLst_clusterMemberList().add(0, dxcMsg);
									this.client.publishClusterMessage(dxcMsg);

//				System.out.println("[MSGBUSMGT:] DXCluster Message detected ");

//				if (!this.client.getdXClusterMemberTable().contains(splittedMessageLine[6])) {
//					this.client.getdXClusterMemberTable().put(newDXCListMember.getCallSign(), newDXCListMember);
//				}

								} else

								/**
								 * DX-Cluster-Message type 2 <br/>
								 * DL|1664047594|1926|dg9yih|144000.0|DL6BF|JO32PC
								 * <TR>
								 * JO32QI zerstoert qso|JO32PC|JO32QI| -> Clustermessage
								 * DL|1664048232|1937|pu2pyb|144500.0|PU2NEZ|FM| | |
								 */
									if (splittedMessageLine[0].contains(DXCLUSTERMESSAGE2)) {
//					System.out.println("MSGBUS: User detected");

										ClusterMessage dxcMsg2 = new ClusterMessage();

										dxcMsg2.setTimeGenerated(splittedMessageLine[1]);

										ChatMember newDXCListSender2 = new ChatMember();
										newDXCListSender2.setCallSign(splittedMessageLine[3]);
										newDXCListSender2.setQra(splittedMessageLine[7]);

										ChatMember newDXCListReceiver2 = new ChatMember();
										newDXCListReceiver2.setFrequency(new SimpleValue<>(splittedMessageLine[4]));
										newDXCListReceiver2.setCallSign(splittedMessageLine[5]);
										newDXCListReceiver2.setQra(splittedMessageLine[8]);

										dxcMsg2.setSender(newDXCListSender2);
										dxcMsg2.setReceiver(newDXCListReceiver2);

										dxcMsg2.setMessageInhibited(splittedMessageLine[6]);
										dxcMsg2.setQrgSpotted(splittedMessageLine[4]);

//				this.client.getLst_clusterMemberList().add(0, dxcMsg2);
										this.client.publishClusterMessage(dxcMsg2);

									} else

									/**
									 * DX-Cluster-Message type 3 <br/>
									 * MA|0|1687204743|e77ar|OK2AF|JN94AS|JN89AR|
									 */
										if (splittedMessageLine[0].contains(DXCLUSTERMESSAGE3)) {
//						System.out.println("MSGBUS: User detected");

											ClusterMessage dxcMsg3 = new ClusterMessage();

											dxcMsg3.setTimeGenerated(splittedMessageLine[2]);

											ChatMember newDXCListSender3 = new ChatMember();
											newDXCListSender3.setCallSign(splittedMessageLine[3]);
											newDXCListSender3.setQra(splittedMessageLine[5]);

											ChatMember newDXCListReceiver3 = new ChatMember();
											newDXCListReceiver3.setCallSign(splittedMessageLine[4]);
											/*
											 * MA format:
											 * MA|0|epoch|sender|receiver|sender locator|receiver locator|
											 */
											newDXCListReceiver3.setQra(splittedMessageLine[6]);

											dxcMsg3.setSender(newDXCListSender3);
											dxcMsg3.setReceiver(newDXCListReceiver3);

											dxcMsg3.setMessageInhibited("");
											dxcMsg3.setQrgSpotted("");

//				this.client.getLst_clusterMemberList().add(0, dxcMsg3);
											this.client.publishClusterMessage(dxcMsg3);
										} else

										/**
										 * Userstatechange:, last digit 0 = in chat, 1 away, 2 here, 3 also away...
										 * US4|2|DM5M|0|
										 */
											if (splittedMessageLine[0].contains(USERSTATECHANGE)) {
//					System.out.println("MSGBUS: User detected");

												ChatMember stateChangeMember = new ChatMember();

												stateChangeMember.setCallSign(splittedMessageLine[2]);
												stateChangeMember.setState(Integer.parseInt(splittedMessageLine[3]));

//				System.out.println("[MSGBUSMGT:] DXCluster Message detected ");

												stateChangeMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));

												if (!this.client.updateActiveChatMemberState(stateChangeMember, stateChangeMember.getState())) {
											LOGGER.info("State change for inactive user: "
															+ stateChangeMember.getCallSign() + " / " + stateChangeMember.getChatCategory());
												}

//				this.client.getChatMemberTable().get(stateChangeMember.getCallSign())
//						.setState(stateChangeMember.getState());

											} else

											/**
											 * Userinfo-update: UM3|2|HA4XN|Zoli 2m SSB/CW|JN96LX|2|
											 */
												if (splittedMessageLine[0].contains(USERINFOUPDATEORUSERISBACK)) {

													if (splittedMessageLine.length < 6) {
												LOGGER.warning("Malformed UM3 message ignored: "
																+ messageToProcess.getMessageText());
													} else {
														ChatMember stateChangeMember = new ChatMember();

														stateChangeMember.setChatCategory(util_getChatCategoryByCategoryNrString(splittedMessageLine[1]));
														stateChangeMember.setCallSign(splittedMessageLine[2]);
														stateChangeMember.setName(splittedMessageLine[3]);
														stateChangeMember.setQra(splittedMessageLine[4]);
														stateChangeMember.setState(Integer.parseInt(splittedMessageLine[5]));
														stateChangeMember.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
														stateChangeMember.setQrb(new Location().getDistanceKmByTwoLocatorStrings(
																client.getChatPreferences().getStn_loginLocatorMainCat(),
																stateChangeMember.getQra()));
														stateChangeMember.setQTFdirection(new Location(client.getChatPreferences().getStn_loginLocatorMainCat())
																.getBearing(new Location(stateChangeMember.getQra())));

														/*
														 * UM3 is only an info/profile update. ON4KST also sends it for stations
														 * that are not logged into this channel. Such stations must not become
														 * visible chat members, otherwise the user could try to address an
														 * offline callsign and receive server-side errors.
														 *
														 * Therefore UM3 updates existing active members only. Unknown UM3 calls
														 * are intentionally ignored; if the station joins later, UA0/UA5/UA2
														 * will deliver the complete current user information again.
														 */
														if (!client.getChatPreferences().getStn_loginCallSign().equalsIgnoreCase(stateChangeMember.getCallSign())) {
															boolean updatedActiveMember = this.client.updateActiveChatMemberInfoIfPresent(stateChangeMember);
															if (updatedActiveMember) {
																this.client.getDbHandler().storeChatMember(stateChangeMember); // TODO: not clean, it should be an update
															} else {
														LOGGER.info("UM3 ignored for inactive user: "
																		+ stateChangeMember.getCallSign() + " / " + stateChangeMember.getChatCategory());
															}
														}
													}

												} else

												/**
												 * Handled like normal messages, but historic...will not trigger any functions
												 *
												 * Chat history line like:
												 * CR|6|1771165971|DF0GEB|test|0|ok|0|
												 * ^^hist
												 * 	  ^chan
												 * 	    ^^^^^^^^^^time ...
												 */
													if (splittedMessageLine[0].contains(SERVERMESSAGEHISTORIC)) {
														processChatFrame(splittedMessageLine, false);
													} else

													/**
													 * Userinfo-update: UE|2|22562|
													 */
														if (SRVR_USERLISTEND.equals(opcode)) {
															if (splittedMessageLine.length < 2) {
													LOGGER.warning(
															"Ignoring malformed UE frame: "
																				+ messageToProcess.getMessageText());
																return;
															}

															this.client.onOn4KstInitialUserListCompleted(
																	connectionSessionId,
																	util_getChatCategoryByCategoryNrString(
																			splittedMessageLine[1]));

														} else if (SRVR_DXCEND.equals(opcode)) {

															// DF marks the end of the initial DX-cluster data.
															// The frame contains no data that needs to be published.

														} else if (SRVR_COMMUNICATIONK.equals(opcode)) {

															// CK is a regular server delimiter/acknowledgement.
															// It is intentionally accepted without further processing.

														} else

															//-> LOGSTAT|114|Wrong password!|
															if (splittedMessageLine[0].contains(SRVR_LOGSTAT) && splittedMessageLine.length <= 5) {
													LOGGER.warning("ON4KST reported an invalid password");

																if (splittedMessageLine[2].contains("password")) {
																	splittedMessageLine[2] += " pse disc- and reconnect";
																}

																ChatMember server = new ChatMember();
																server.setCallSign("SERVER");
																server.setName("SERVER");

																ChatMessage pwErrorMsg = new ChatMessage();

																pwErrorMsg.setMessageGeneratedTime(client.getCurrentEpochTime()+"");
																pwErrorMsg.setSender(server);
																pwErrorMsg.setMessageText(splittedMessageLine[2]);

																ChatMember receiverDummy = new ChatMember();
																receiverDummy.setCallSign(client.getChatPreferences().getStn_loginCallSign());
																receiverDummy.setQrb(0.);
																receiverDummy.setQTFdirection(0.);
																pwErrorMsg.setReceiver(receiverDummy);



																for (int i = 0; i < 10; i++) {
																	client.publishChatMessage(pwErrorMsg);
//					client.getLst_toMeMessageList().add(pwErrorMsg);
//					client.getLst_toAllMessageList().add(pwErrorMsg);
																}

//				Kst4ContestApplication.alertWindowEvent("Password was wrong. Pse check!");

																client.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);

//				this.client.disconnect();
															}

															else if (splittedMessageLine[0].equals("DE")) {
																// DXCluster delimiter/end marker; intentionally ignored.
															}

															else {

//				bufwrtrDBGMSGOut.write(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
//						+ "[MSGBUSMGT:] Critical, detected unhandled Chatmessage -> "
//						+ messageToProcess.getMessageText() + "\n");
//				bufwrtrDBGMSGOut.flush();

											LOGGER.warning("Detected unhandled ON4KST frame: "
													+ messageToProcess.getMessageText());

															}

			// ******************************************************************QUICKNDIRTY........
//			ChatMember thisMemberActualizesUserListForRefreshingIntheGuy = new ChatMember();
//			thisMemberActualizesUserListForRefreshingIntheGuy.setCallSign("REFR");
//			thisMemberActualizesUserListForRefreshingIntheGuy.setAirPlaneReflectInfo(new AirPlaneReflectionInfo());
//			this.client.getLst_chatMemberList().add(thisMemberActualizesUserListForRefreshingIntheGuy);
//			this.client.getLst_chatMemberList().isEmpty();
//			System.out.println("MSGBUS BGFX Listactualizer");
//			this.client.getLst_chatMemberList().remove(thisMemberActualizesUserListForRefreshingIntheGuy);
			// ******************************************************************QUICKNDIRTY........

		}
	}


	/**
	 * Decides whether an automatic DX Cluster spot must be suppressed because
	 * the sender has already been worked on the band of its known frequency.
	 *
	 * <p>Rules (issue #91):</p>
	 * <ul>
	 *   <li>No sender, no frequency or a frequency outside the supported
	 *       bands: the existing behaviour is kept and the spot is allowed.</li>
	 *   <li>The per-band worked flag of the detected band is set: suppress.</li>
	 *   <li>Only the global worked flag is set and no per-band flag exists
	 *       (Simplelogfile source without band information): suppress, because
	 *       the band cannot be told apart.</li>
	 *   <li>The global flag is set together with per-band flags for other
	 *       bands: allow, because the per-band information is authoritative.</li>
	 * </ul>
	 *
	 * <p>Manual spots from the map window and the settings test spot do not
	 * use this check.</p>
	 *
	 * @param sender station that triggered the directional opportunity
	 * @return {@code true} when the automatic spot has to be suppressed
	 */
	static boolean isAlreadyWorkedForDxClusterSpot(ChatMember sender) {
		if (sender == null || sender.getFrequency() == null) {
			return false;
		}

		FrequencyTextParser.DetectedFrequency detectedFrequency =
				FrequencyTextParser.parseExplicitFrequency(
						sender.getFrequency().get()
				);

		if (detectedFrequency == null) {
			// Unknown band: keep the existing behaviour and allow the spot.
			return false;
		}

		if (sender.isWorkedOnBand(detectedFrequency.getBand())) {
			return true;
		}

		// Worked source without band information (Simplelogfile): global flag only.
		return sender.isWorked() && !sender.hasAnyBandWorkedFlag();
	}

	/**
	 * Builds the comment transmitted with a local DX Cluster spot.
	 *
	 * <p>The station locator is always retained. AirScout information is optional:
	 * a missing response, an empty aircraft list or an incomplete aircraft entry
	 * must never prevent the spot itself from being sent.</p>
	 *
	 * @param sender station for which the DX Cluster spot is generated
	 * @return locator with up to two optional AP entries
	 */
	static String buildDxClusterSpotComment(ChatMember sender) {
		if (sender == null) {
			return "";
		}

		String locator = sender.getQra() == null
				? ""
				: sender.getQra().trim().toUpperCase(Locale.ROOT);

		AirPlaneReflectionInfo reflectionInfo =
				sender.getAirPlaneReflectInfo();

		if (reflectionInfo == null
				|| reflectionInfo.getRisingAirplanes() == null
				|| reflectionInfo.getRisingAirplanes().isEmpty()) {
			return locator;
		}

		ArrayList<String> aircraftComments = new ArrayList<>();

		int aircraftCount = Math.min(
				2,
				reflectionInfo.getRisingAirplanes().size()
		);

		for (int index = 0; index < aircraftCount; index++) {
			AirPlane aircraft =
					reflectionInfo.getRisingAirplanes().get(index);

			if (aircraft == null) {
				continue;
			}

			aircraftComments.add(
					aircraft.getArrivingDurationMinutes()
							+ "m/"
							+ aircraft.getPotential()
							+ "%"
			);
		}

		if (aircraftComments.isEmpty()) {
			return locator;
		}

		String apComment =
				"AP " + String.join(";", aircraftComments);

		return locator.isEmpty()
				? apComment
				: locator + " " + apComment;
	}

	/**
	 * Outcome of the directional check for a message between two other stations.
	 */
	enum DirectionalOpportunity {
		/** Sender or receiver locator is missing or blank; nothing was calculated. */
		MISSING_LOCATOR,
		/** The sender beams towards the receiver and the local station is covered. */
		IN_ANGLE_AND_RANGE,
		/** Both locators are known, but the local station is outside beam or range. */
		OUT_OF_ANGLE_OR_RANGE
	}

	/**
	 * Decides whether a message from one other station to another one is a
	 * directional opportunity ("Richtungs-Chance") for the local station.
	 *
	 * <p>Without both a sender and a receiver locator the geometry is not
	 * evaluated at all. Otherwise the decision is
	 * {@link DirectionUtils#isInAngleAndRange(String, String, String, double, double)}:
	 * the local station must be within {@code maxQrbKm} of the sender and inside
	 * the sender's beam towards the receiver.</p>
	 *
	 * @param ownLocator locator of the local station
	 * @param senderLocator locator of the sending station, may be {@code null}
	 * @param receiverLocator locator of the receiving station, may be {@code null}
	 * @param maxQrbKm maximum distance from the local station to the sender
	 * @param beamWidthDeg assumed antenna beam width of the sender in degrees
	 * @return the classification of the message
	 */
	static DirectionalOpportunity classifyDirectionalOpportunity(
			String ownLocator,
			String senderLocator,
			String receiverLocator,
			double maxQrbKm,
			double beamWidthDeg
	) {
		boolean senderHasLocator = senderLocator != null && !senderLocator.isBlank();
		boolean receiverHasLocator = receiverLocator != null && !receiverLocator.isBlank();

		if (!senderHasLocator || !receiverHasLocator) {
			return DirectionalOpportunity.MISSING_LOCATOR;
		}

		return DirectionUtils.isInAngleAndRange(
				ownLocator,
				senderLocator,
				receiverLocator,
				maxQrbKm,
				beamWidthDeg)
				? DirectionalOpportunity.IN_ANGLE_AND_RANGE
				: DirectionalOpportunity.OUT_OF_ANGLE_OR_RANGE;
	}

	/**
	 * Where a chat message belongs. Keeps the long-standing asymmetry on purpose: the
	 * receiver is compared with the login call case-sensitively, the sender
	 * case-insensitively. Changing either changes which messages are answered or marked.
	 *
	 * @param receiverField the raw receiver field; {@code 0} means public
	 * @param senderCall the sender's call
	 * @param receiverCall the resolved receiver's call, unused for public messages
	 * @param ownCall the raw login call from the preferences
	 * @return the route of the message
	 */
	static ChatRoute classifyRoute(String receiverField, String senderCall, String receiverCall, String ownCall) {
		if ("0".equals(receiverField)) {
			return ChatRoute.PUBLIC;
		}
		if (receiverCall.equals(ownCall)) {
			return ChatRoute.TO_ME;
		}
		if (senderCall.toUpperCase().equals(ownCall.toUpperCase())) {
			return ChatRoute.FROM_ME;
		}
		return ChatRoute.BETWEEN_OTHERS;
	}

	/**
	 * Recognizes the structured address prefix produced by ON4KST for a public
	 * {@code /cq CALL ...} message.
	 *
	 * <p>The address must be the first non-whitespace token and occupy the complete
	 * parenthesized prefix. A callsign occurring later in ordinary prose is therefore
	 * not treated as an address.</p>
	 */
	static boolean isPublicMessageExplicitlyAddressedTo(String messageText, String ownCall) {
		if (messageText == null || ownCall == null || ownCall.isBlank()) {
			return false;
		}

		String text = messageText.stripLeading();
		if (!text.startsWith("(")) {
			return false;
		}

		int closingParenthesis = text.indexOf(')');
		if (closingParenthesis <= 1) {
			return false;
		}

		String addressedCall = text.substring(1, closingParenthesis).trim();
		return addressedCall.equalsIgnoreCase(ownCall.trim());
	}

	/**
	 * Returns whether the short tick sound for a new directional opportunity is
	 * played: only with simple sounds enabled and only on the transition into
	 * the opportunity, not again while the sender is already marked.
	 *
	 * @param simpleSoundsEnabled whether simple notification sounds are enabled
	 * @param senderAlreadyInAngleAndRange the sender's flag before this message
	 * @return {@code true} when the tick is to be played
	 */
	static boolean shouldPlayDirectionalTick(
			boolean simpleSoundsEnabled,
			boolean senderAlreadyInAngleAndRange
	) {
		return simpleSoundsEnabled && !senderAlreadyInAngleAndRange;
	}

	/**
	 * Settings the chat frame decision depends on.
	 *
	 * @param simpleSounds whether simple notification sounds are enabled
	 * @param cwOnPm whether the sender's call is sent in CW for private messages
	 * @param voiceOnPm whether the sender's call is spoken for private messages
	 * @param dxClusterServerEnabled whether the DX Cluster server is enabled
	 * @param autoAnswerTextCandidate the auto answer text built for this message, or {@code null}
	 */
	record ChatFrameSettings(
			boolean simpleSounds,
			boolean cwOnPm,
			boolean voiceOnPm,
			boolean dxClusterServerEnabled,
			String autoAnswerTextCandidate
	) {
	}

	/**
	 * Decides the side effects of a chat frame.
	 * <ul>
	 * <li>Public and own messages: none.</li>
	 * <li>Live message to me: PM sound, CW and voice call per settings, bell for {@code //BELL}
	 * (with simple sounds), version reply for {@code //VER}, auto answer unless the text is automatic.</li>
	 * <li>History message to me: none.</li>
	 * <li>Between others: the sender's flag follows the opportunity; live and in angle and range
	 * additionally the tick and the DX Cluster spot evaluation.</li>
	 * </ul>
	 *
	 * @param route the route of the message
	 * @param live {@code true} for a live frame, {@code false} for history
	 * @param messageText the message text
	 * @param opportunity the directional opportunity, only used between others
	 * @param senderAlreadyInAngleAndRange the sender's flag before this message
	 * @param settings the settings the decision depends on
	 * @return the effects to execute
	 */
	static ChatMessageEffects decideChatFrameEffects(
			ChatRoute route,
			boolean live,
			String messageText,
			DirectionalOpportunity opportunity,
			boolean senderAlreadyInAngleAndRange,
			ChatFrameSettings settings
	) {
		switch (route) {
			case TO_ME:
				if (!live) {
					return ChatMessageEffects.NONE;
				}
				String upper = messageText.toUpperCase();
				return new ChatMessageEffects(
						settings.simpleSounds(),
						settings.cwOnPm(),
						settings.voiceOnPm(),
						settings.simpleSounds() && upper.contains("//BELL"),
						upper.contains("//VER"),
						isAutoMessage(messageText) ? null : settings.autoAnswerTextCandidate(),
						null,
						false,
						false);
			case BETWEEN_OTHERS:
				boolean inRange = opportunity == DirectionalOpportunity.IN_ANGLE_AND_RANGE;
				boolean tick = live && inRange
						&& shouldPlayDirectionalTick(settings.simpleSounds(), senderAlreadyInAngleAndRange);
				boolean spot = live && inRange && settings.dxClusterServerEnabled();
				return new ChatMessageEffects(false, false, false, false, false, null, inRange, tick, spot);
			default:
				return ChatMessageEffects.NONE;
		}
	}

	/**
	 * Outcome of the automatic DX Cluster spot decision for a directional opportunity.
	 */
	enum DxClusterSpotDecision {
		/** The sender has no known frequency; nothing is spotted or logged. */
		NO_FREQUENCY,
		/** The sender is already worked on the band of its frequency (issue #91). */
		ALREADY_WORKED,
		/** The spot is to be sent. */
		SPOT
	}

	/**
	 * Decides whether an automatic DX Cluster spot is sent for the sender of a
	 * directional opportunity. Whether the local DX Cluster server is enabled at
	 * all is checked by the caller.
	 *
	 * <p>A frequency counts as known when its text is neither {@code null} nor
	 * blank; it is not parsed here. A text that names no supported band therefore
	 * still leads to {@link DxClusterSpotDecision#SPOT}, see
	 * {@link #isAlreadyWorkedForDxClusterSpot(ChatMember)}.</p>
	 *
	 * @param sender station that triggered the directional opportunity, may be {@code null}
	 * @return the decision
	 */
	static DxClusterSpotDecision decideDxClusterSpot(ChatMember sender) {
		String detectedFrequency = sender == null || sender.getFrequency() == null
				? null
				: sender.getFrequency().get();

		if (detectedFrequency == null || detectedFrequency.isBlank()) {
			return DxClusterSpotDecision.NO_FREQUENCY;
		}

		return isAlreadyWorkedForDxClusterSpot(sender)
				? DxClusterSpotDecision.ALREADY_WORKED
				: DxClusterSpotDecision.SPOT;
	}

	/**
	 * Builds the entry handed to the local DX Cluster server for an automatic spot:
	 * the sender's callsign, the sender's frequency value (the same instance, not a
	 * copy) and, in the locator field, the comment from
	 * {@link #buildDxClusterSpotComment(ChatMember)}.
	 *
	 * @param sender station to spot
	 * @return a new member object used only for spotting
	 */
	static ChatMember buildDxClusterSpotEntry(ChatMember sender) {
		ChatMember onlyForSpottingObject = new ChatMember();
		onlyForSpottingObject.setCallSign(sender.getCallSign());
		onlyForSpottingObject.setFrequency(sender.getFrequency());
		onlyForSpottingObject.setQra(
				buildDxClusterSpotComment(sender)
		);
		return onlyForSpottingObject;
	}

	/**
	 * Method gets a String with a messagecategory-number and returns out of which of the existing categories
	 * (chat channels) this message/user had written from
	 *
	 * @param categoryNumber
	 * @return used Chatcategory (instance of singletons)
	 */
	private ChatCategory util_getChatCategoryByCategoryNrString(String categoryNumber) {

//		System.out.println("MSGBSMGT Debug: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> try to find out category for a member; category is " + categoryNumber + " // 1st is " + this.client.getChatCategoryMain().getCategoryNumber() + " // 2nd is " + this.client.getChatCategorySecondChat().getCategoryNumber());

		if (categoryNumber.equals(this.client.getChatCategoryMain().getCategoryNumber() + "")) {
			return this.client.getChatCategoryMain();
		} else if (categoryNumber.equals(this.client.getChatCategorySecondChat().getCategoryNumber() + "")) {
			return this.client.getChatCategorySecondChat();
		} else {
			LOGGER.warning("Category for ON4KST message does not exist; using established main-category fallback");
			return this.client.getChatCategoryMain(); //Chatcategory default decision
		}

	}

	@Override
	public void interrupt() {
		super.interrupt();

	}


	/**
	 * Returns whether a private message asks for the local QRG.
	 *
	 * <p>The comparison is a case-insensitive substring match against each of the
	 * given fragments, so {@code "GM, PSE QRG for a sked"} matches {@code "pse qrg"}.</p>
	 *
	 * @param text incoming message text, may be {@code null}
	 * @param questionTexts lower-case fragments that identify a QRG question
	 * @return {@code true} when any fragment occurs in the text
	 */
	static boolean matchesQrgQuestion(String text, Set<String> questionTexts) {
		if (text == null || questionTexts == null) {
			return false;
		}

		String lower = text.toLowerCase(Locale.ROOT);

		for (String questionText : questionTexts) {
			if (lower.contains(questionText)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns whether a message carries the fixed marker used for automatic replies.
	 * Such a message must never be answered automatically (ping-pong guard).
	 *
	 * @param text message text, may be {@code null}
	 * @return {@code true} when the marker occurs anywhere in the text
	 */
	static boolean isAutoMessage(String text) {
		return text != null && text.contains(AUTOANSWER_PREFIX);
	}

	/**
	 * Selects the configured QRG for the category in which a request was received.
	 *
	 * <p>Only a request from the second chat category is answered with the second
	 * QRG. Any other category, including an unresolved one, falls back to the first
	 * QRG. Categories are compared by number, not by identity.</p>
	 *
	 * @param incomingCategory category of the incoming message, may be {@code null}
	 * @param secondCategory configured second chat category, may be {@code null}
	 * @param firstCategoryQrg QRG configured for the main category, may be {@code null}
	 * @param secondCategoryQrg QRG configured for the second category, may be {@code null}
	 * @return the QRG belonging to the incoming category
	 */
	static String selectAutoAnswerQrg(
			ChatCategory incomingCategory,
			ChatCategory secondCategory,
			String firstCategoryQrg,
			String secondCategoryQrg
	) {
		if (incomingCategory != null
				&& secondCategory != null
				&& incomingCategory.getCategoryNumber() == secondCategory.getCategoryNumber()) {
			return secondCategoryQrg;
		}

		return firstCategoryQrg;
	}

	/**
	 * Builds and validates one automatic private reply.
	 *
	 * <p>A QRG request is answered only when the QRG belonging to the
	 * incoming chat category is available. The generic answer is used
	 * only for other private messages and only when it contains actual
	 * text.</p>
	 *
	 * <p>The complete message is validated before it enters the transmit
	 * queue. Invalid configuration values must neither produce an empty
	 * automatic reply nor start the cooldown.</p>
	 *
	 * @param senderCallSign callsign of the station that sent the private message
	 * @param category chat category of the incoming message, used for diagnostics only
	 * @param qrgRequested whether the message contains a recognised QRG request
	 * @param categoryQrg the QRG configured for that category, see
	 *                    {@link #selectAutoAnswerQrg(ChatCategory, ChatCategory, String, String)}
	 * @param genericEnabled whether the general automatic reply is enabled
	 * @param genericAnswerText the configured general reply text
	 * @return validated message text or {@code null} when no reply may be sent
	 */
	static String buildAutoAnswerMessageText(
			String senderCallSign,
			ChatCategory category,
			boolean qrgRequested,
			String categoryQrg,
			boolean genericEnabled,
			String genericAnswerText
	) {
		if (senderCallSign == null || senderCallSign.isBlank()) {
			LOGGER.warning(
					"KST4Contest auto-answer skipped: "
							+ "incoming message has no valid sender callsign."
			);
			return null;
		}

		String payload;

		if (qrgRequested) {
			if (categoryQrg == null || categoryQrg.isBlank()) {
				LOGGER.warning(
						"KST4Contest QRG auto-answer skipped for "
								+ senderCallSign
								+ ": no QRG is available for chat category "
								+ senderCallSign.toUpperCase()
								+ "|"
								+ (category == null ? 0 : category.getCategoryNumber())
								+ "."
				);
				return null;
			}

			payload = "QRG is: " + categoryQrg.trim();

		} else if (genericEnabled) {
			if (genericAnswerText == null || genericAnswerText.isBlank()) {
				LOGGER.warning(
						"KST4Contest generic auto-answer skipped for "
								+ senderCallSign
								+ ": the configured answer text is empty."
				);
				return null;
			}

			payload = genericAnswerText.trim();

		} else {
			return null;
		}

		String messageText =
				"/CQ "
						+ senderCallSign.trim()
						+ " "
						+ AUTOANSWER_PREFIX
						+ " "
						+ payload;

		try {
			return On4KstProtocol.messageText(messageText);

		} catch (IllegalArgumentException invalidMessage) {
			LOGGER.warning(
					"KST4Contest auto-answer skipped for "
							+ senderCallSign
							+ ": "
							+ invalidMessage.getMessage()
			);
			return null;
		}
	}

	/**
	 * Builds the cooldown identity of an incoming private message: the upper-cased
	 * remote callsign and the category number, separated by {@code |}.
	 *
	 * <p>The message category wins; the sender's category is only a fallback. A
	 * missing callsign yields {@code UNKNOWN}, a missing category {@code 0}.</p>
	 *
	 * @param incoming incoming private message, may be {@code null}
	 * @return key such as {@code DL1ABC|2}
	 */
	static String autoAnswerCooldownKey(ChatMessage incoming) {

		String remoteCall = "UNKNOWN";
		if (incoming != null && incoming.getSender() != null && incoming.getSender().getCallSign() != null) {
			remoteCall = incoming.getSender().getCallSign().toUpperCase();
		}

		int categoryNumber = 0;
		if (incoming != null && incoming.getChatCategory() != null) {
			categoryNumber = incoming.getChatCategory().getCategoryNumber();
		} else if (incoming != null
				&& incoming.getSender() != null
				&& incoming.getSender().getChatCategory() != null) {
			categoryNumber = incoming.getSender().getChatCategory().getCategoryNumber();
		}

		return remoteCall + "|" + categoryNumber;
	}

	/**
	 * Returns whether the cooldown has elapsed since the last local automatic reply.
	 *
	 * @param lastSentMillis epoch milliseconds of the last reply, {@code null} if none
	 * @param nowMillis current epoch milliseconds
	 * @return {@code true} when no reply was sent yet or at least
	 *         {@link #AUTOANSWER_COOLDOWN_MS} have passed
	 */
	static boolean isCooldownElapsed(Long lastSentMillis, long nowMillis) {
		return lastSentMillis == null || (nowMillis - lastSentMillis) >= AUTOANSWER_COOLDOWN_MS;
	}

	private boolean isAutoAnswerAllowedNow(ChatMessage incoming) {
		Long last = lastLocalAutoAnswerPerRemoteMs.get(autoAnswerCooldownKey(incoming));
		return isCooldownElapsed(last, millisClock.getAsLong());
	}

	private void markLocalAutoAnswerSent(ChatMessage incoming) {
		lastLocalAutoAnswerPerRemoteMs.put(autoAnswerCooldownKey(incoming), millisClock.getAsLong());
	}


	public void run() {

//		fileLogRAW = new File(new Utils4KST().time_generateCurrentMMddString() + "_praktiKST_raw.txt");

//		FileWriter fileWriterRAWChatMSGOut = null;
//		BufferedWriter bufwrtrRawMSGOut;

//		try {
//			fileWriterRAWChatMSGOut = new FileWriter(fileLogRAW, true);
//
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

//		bufwrtrRawMSGOut = new BufferedWriter(fileWriterRAWChatMSGOut);

//		File fileLogClientOut = new File(new Utils4KST().time_generateCurrentMMddString() + "_praktiKST_out.txt");

//		FileWriter fileWriterOutChatMSGOut = null;
//
//		try {
//			fileWriterOutChatMSGOut = new FileWriter(fileLogClientOut, true);
//		} catch (IOException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//
//		try {
//			fileWriterRAWChatMSGOut = new FileWriter(fileLogClientOut, true);
//
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//			try {
//				fileWriterRAWChatMSGOut.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

//		bufwrtrDBGMSGOut = new BufferedWriter(fileWriterOutChatMSGOut);

//		new Timer().schedule(new UserActualizationTask(client), 4000, 60000);// TODO: Temporary userlistoutput with
//																				// known qrgs
//
//		userActualizationTask = new UserActualizationTask(client); // kst4contest.test 4 23001
//		userActualizationTask.run();// kst4contest.test 4 23001

		ChatMessage messageTextRaw = new ChatMessage(); // moved out of the while
		String messageLine;

		while (true) {

			try {
				messageTextRaw = receiveQueue.take();

				if (ApplicationConstants.DISCONNECT_RDR_POISONPILL.equals(messageTextRaw.getMessageText())
						&& ApplicationConstants.DISCONNECT_RDR_POISONPILL.equals(messageTextRaw.getMessageSenderName())) {
					receiveQueue.clear();
					break;
				}
				else {
					if (!connectionSessionIsActive.test(connectionSessionId)) {
						break;
					}
					messageLine = messageTextRaw.getMessageText();

					/***********************************************
					 * CASE RX
					 ***********************************************/

//						if (client.getMessageRXBus().peek() != null) {

//							try {
//								messageTextRaw = client.getMessageRXBus().take();
					//
////								System.out.println("MSBGBUS: rxed: " + messageTextRaw);
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}

					if (messageTextRaw.getMessageText() == null) {
						LOGGER.severe("Received ON4KST message with null payload");
//							this.interrupt();
//							break;
					}

					messageLine = messageTextRaw.getMessageText();

//							try {
//								bufwrtrRawMSGOut.write(messageLine + "\n");
//								bufwrtrRawMSGOut.flush();
					//
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}

					try {
						processRXMessage23001(messageTextRaw);
					} catch (IOException e) {
					LOGGER.log(Level.WARNING,
							"I/O error while processing ON4KST frame", e);
					} catch (SQLException e) {
					LOGGER.log(Level.WARNING,
							"SQL error while processing ON4KST frame", e);
					} catch (RuntimeException e) {
					LOGGER.log(Level.WARNING,
							"Runtime error while processing ON4KST opcode "
									+ On4KstProtocol.opcode(
											messageTextRaw.getMessageText()),
							e);
					}
				}

			} catch (InterruptedException e1) {
				this.interrupt();

				LOGGER.log(Level.FINE,
						"ON4KST message processor interrupted", e1);
				break;// TODO Change at may24, avoid uncloadability. Check if this could lead to further errors on instable link!
				//				client.getMessageRXBus().clear();
			}

		} // while true end
		LOGGER.fine("ON4KST message processor stopped");
		this.interrupt();
	}
}
