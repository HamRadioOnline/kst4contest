package kst4contest.controller;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ThreadStateMessage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadUDPByWintestThread extends Thread {

    private static final Pattern STATUS_TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private DatagramSocket socket;
    private ChatController client;

    private volatile boolean running = true;

    private int PORT = 9871; //default

    private static final int BUFFER_SIZE = 4096;

    private long lastPacketTime = 0;

    private String myStation = "DO5AMF";

    private String targetStation = "";
    private String stationID = "";

    private ThreadStatusCallback callBackToController;
    private String ThreadNickName = "Wintest-msg";

    /**
     * Number of fields of a complete ADDQSO packet, including message type,
     * source and destination.
     */
    private static final int ADDQSO_FIELD_COUNT = 24;

    /** Field position of the Win-Test QSO number inside an ADDQSO packet. */
    private static final int ADDQSO_QSO_NUMBER_INDEX = 11;

    /** Field position of the logging station name inside an ADDQSO packet. */
    private static final int ADDQSO_STATION_NAME_INDEX = 3;

    private final WinTestLogSyncService logSyncService;

    private WinTestLogSyncService.SyncState lastReportedSyncState;

    /**
     * Last IHAVE payload seen per station. Win-Test repeats the inventory
     * periodically, so tracing only the changes keeps the output readable.
     */
    private final Map<String, String> lastTracedIhaveByStation = new HashMap<>();

    private final WinTestNetworkAddressResolver addressResolver;


    public ReadUDPByWintestThread(ChatController client, ThreadStatusCallback callback) {

        this.callBackToController = callback;
        this.client = client;
        this.myStation = client.getChatPreferences().getStn_loginCallSignRaw(); //callsign of the logging stn
        this.PORT =  client.getChatPreferences().getLogsynch_wintestNetworkPort();

        WinTestNetworkAddressResolver sharedAddressResolver =
                client.getWinTestAddressResolver();
        this.addressResolver = sharedAddressResolver != null
                ? sharedAddressResolver
                : new WinTestNetworkAddressResolver();

        /*
         * Preferences are read late on purpose: station name, port and broadcast
         * address can be changed while the listener is running.
         */
        this.logSyncService = new WinTestLogSyncService(
                this::sendNeedQso,
                this::resolveOwnWinTestStationName
        );

    }

    @Override
    public void interrupt() {
        running = false;
        if (socket != null && !socket.isClosed()) socket.close();
        super.interrupt();
    }

    @Override
    public void run() {

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "initialized", false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
        Thread.currentThread().setName("ReadUDPByWintestThread");

        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            socket = new DatagramSocket(null); //first init with null, then make ready for reuse
            socket.setReuseAddress(true);
//            socket = new DatagramSocket(PORT);
            int boundPort = client.getChatPreferences().getLogsynch_wintestNetworkPort();
            socket.bind(new InetSocketAddress(boundPort));
            socket.setSoTimeout(3000);
            System.out.println("[WinTest UDP listener] started at port: " + boundPort);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        while (running) {
            try {
                /*
                 * DatagramPacket keeps the length of the previous datagram, so
                 * without resetting it a long packet would be truncated after a
                 * short one. A truncated packet loses its trailing fields and
                 * its checksum.
                 */
                packet.setLength(buffer.length);
                socket.receive(packet);
                processWinTestDatagram(
                        packet.getData(), packet.getLength(), packet.getAddress());
            } catch (SocketTimeoutException e) {
                logSyncService.tick();
                reportSyncStateIfChanged();
            } catch (IOException e) {
                //TODO: here is something to catch
            }
        }
    }

    /**
     * Resolves the Win-Test framing of a received datagram and processes it.
     *
     * <p>The checksum byte and the NUL terminator are removed on the raw bytes
     * before any text parsing, so the trailing fields of the packet stay
     * readable. Afterwards the log synchronization gets its chance to request
     * QSOs that were logged before this listener was started.</p>
     *
     * @param datagram raw datagram buffer
     * @param length number of valid bytes in the buffer
     */
    void processWinTestDatagram(byte[] datagram, int length, InetAddress source) {
        if (datagram == null || length <= 0) {
            return;
        }

        WinTestPacket packet = WinTestPacket.fromDatagram(datagram, length);

        if (packet != null && isWinTestStationMessage(packet.getMessageType())) {
            /*
             * Win-Test only answers broadcasts. Remembering where its packets
             * come from keeps outgoing requests on the network the station
             * actually lives in, even when the configured broadcast address
             * belongs to a different or no longer existing network.
             */
            addressResolver.rememberStationAddress(source);
        }

        if (packet == null) {
            /*
             * The datagram does not follow the Win-Test framing. It still
             * reaches the established text handling, which also recognizes the
             * poison pill that stops this listener.
             */
            processWinTestPacket(
                    null,
                    new String(datagram, 0, length, StandardCharsets.US_ASCII).trim()
            );
            return;
        }

        processWinTestPacket(packet, packet.getMessageText());

        logSyncService.tick();
        reportSyncStateIfChanged();
    }

    void processWinTestMessage(String msg) {
        processWinTestPacket(WinTestPacket.fromMessageText(msg), msg);
    }

    /**
     * Processes one Win-Test message.
     *
     * @param packet parsed packet, or {@code null} when the message does not
     *               follow the Win-Test framing
     * @param msg complete message text
     */
    private void processWinTestPacket(WinTestPacket packet, String msg) {
//        System.out.println("Wintest-Message received: " + msg);

        if (msg == null) {
            return;
        }

        lastPacketTime = System.currentTimeMillis();

        if (msg.startsWith("HELLO:")) { //Client Signon of wintest
            parseHello(msg);

            if (packet != null) {
                System.out.println("[WinTest RX] HELLO from " + packet.getSource());
                logSyncService.onStationSeen(packet.getSource());
            }

        } else if (msg.startsWith("ADDQSO:")) { //adding qso to wintest log
            try {

                if (packet != null && !packet.getDestination().isEmpty()) {
                    /*
                     * A directed ADDQSO is the answer to one of our NEEDQSO
                     * requests. Tracing it separates a missing answer from a
                     * failing evaluation of the answer.
                     */
                    System.out.println("[WinTest RX] ADDQSO answer from "
                            + packet.getSource() + " to " + packet.getDestination());
                }

                parseAddQso(msg);
            } catch (Exception e) {
                ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "Parsing ERROR: " + Arrays.toString(e.getStackTrace()), true);
                callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
            }

        } else if (msg.startsWith("STATUS")) {
            parseStatus(msg);

            /*
             * HELLO is only sent when a log is opened, so a listener that was
             * started later learns about a station from its periodic STATUS.
             * The configured station-name filter stays a QRG-sync setting: in a
             * multi-station setup every band station keeps its own log, and all
             * of them contribute Worked state.
             */
            if (packet != null) {
                logSyncService.onStationSeen(packet.getSource());
            }

        } else if (msg.startsWith("IHAVE:")) { //periodical message of wintest, which qsos are in the log
            parseIHave(packet);
        }

        else if (msg.contains(ApplicationConstants.DISCONNECT_RDR_POISONPILL)) {
            System.out.println("ReadUdpByWintest, Info: got poison, now dieing....");
            socket.close();
            running = false;

        }

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "message received\n" + msg, false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
    }

    /**
     * Hands the periodic Win-Test log inventory to the log synchronization.
     *
     * <p>A packet with a broken checksum is discarded here. The run-length
     * inventory is the last field of an IHAVE packet, so a corrupted packet
     * would announce QSO ranges that do not exist. The established handling of
     * the other message types is deliberately left unchanged, because it never
     * verified the checksum.</p>
     *
     * @param packet received IHAVE packet
     */
    private void parseIHave(WinTestPacket packet) {
        if (packet == null) {
            return;
        }

        if (packet.isChecksumPresent() && !packet.isChecksumValid()) {
            System.out.println("[WinTest] IHAVE with invalid checksum ignored");
            return;
        }

        String tracedPayload = String.join(" ", packet.getDataTokens());
        if (!tracedPayload.equals(lastTracedIhaveByStation.put(packet.getSource(), tracedPayload))) {
            System.out.println("[WinTest RX] IHAVE from " + packet.getSource()
                    + " to '" + packet.getDestination() + "': " + tracedPayload
                    + (WinTestIhaveInventory.fromPacket(packet).isEmpty()
                            ? "  <-- not usable as inventory" : ""));
        }

        logSyncService.onIhaveReceived(packet);
    }

    /**
     * Reports a change of the log-synchronization progress to the controller.
     */
    private void reportSyncStateIfChanged() {
        WinTestLogSyncService.SyncState currentSyncState = logSyncService.getState();

        if (currentSyncState == lastReportedSyncState) {
            return;
        }

        lastReportedSyncState = currentSyncState;

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(
                this.ThreadNickName, true, "log sync: " + currentSyncState, false);
        callBackToController.onThreadStatus(ThreadNickName, threadStateMessage);
    }

    /**
     * Sends a NEEDQSO request as a UDP broadcast.
     *
     * <p>The framing follows the wtKST implementation exactly, including the
     * leading blank of the data part:</p>
     *
     * <pre>
     *   NEEDQSO: "KST4Contest" "STN1"  "STN1@44510" 1 50{checksum}\0
     * </pre>
     *
     * @param targetStation Win-Test station the request is addressed to
     * @param logId log identity in the form {@code StationName@LogUniqueID}
     * @param countFrom first requested QSO number
     * @param countTo last requested QSO number
     */
    private void sendNeedQso(String targetStation, String logId, long countFrom, long countTo) {
        String data = " \"" + logId + "\" " + countFrom + " " + countTo;

        WinTestMessage needQsoMessage = new WinTestMessage(
                WinTestMessage.MessageType.NEEDQSO,
                resolveOwnWinTestStationName(),
                targetStation,
                data
        );

        try (DatagramSocket sendSocket = new DatagramSocket()) {
            sendSocket.setBroadcast(true);
            sendSocket.setReuseAddress(true);

            byte[] messageBytes = needQsoMessage.toBytes();
            InetAddress broadcastAddress = addressResolver.resolveBroadcastAddress(
                    client.getChatPreferences().getLogsynch_wintestNetworkBroadcastAddress());
            int targetPort = client.getChatPreferences().getLogsynch_wintestNetworkPort();

            sendSocket.send(new DatagramPacket(
                    messageBytes, messageBytes.length, broadcastAddress, targetPort));

            System.out.println("[WinTest LogSync] NEEDQSO to " + targetStation
                    + " for " + logId + " " + countFrom + "-" + countTo);
        } catch (IOException | RuntimeException exception) {
            System.out.println("[WinTest LogSync] NEEDQSO could not be sent: "
                    + exception.getMessage());
        }
    }

    /**
     * @return own station name in the Win-Test network, never blank
     */
    private String resolveOwnWinTestStationName() {
        String configuredStationName =
                client.getChatPreferences().getLogsynch_wintestNetworkStationNameOfKST();

        if (configuredStationName == null || configuredStationName.isBlank()) {
            return "KST4Contest";
        }

        return configuredStationName.trim();
    }

    /**
     * Checks whether a message type identifies a genuine Win-Test station.
     *
     * <p>Internal control messages such as the poison pill must not influence
     * the address of outgoing Win-Test packets.</p>
     *
     * @param messageType message type of a received packet
     * @return {@code true} for a Win-Test station message
     */
    private static boolean isWinTestStationMessage(String messageType) {
        return "HELLO".equals(messageType)
                || "STATUS".equals(messageType)
                || "IHAVE".equals(messageType)
                || "ADDQSO".equals(messageType);
    }

    /**
     * parsing of the hello message of wintest:
     * "HELLO: "STN1" "" 6667 130 "SLAVE" 1 0 1762201985"
     * @param msg
     */
    private void parseHello(String msg) {
        try {
            String[] tokens = msg.split("\"");
            if (tokens.length >= 2) {
                targetStation = tokens[1];
                System.out.println("[WinTest rcv: found logger instance: " + targetStation);
            }
        } catch (Exception e) {
            System.out.println("[WinTest] ERROR on HELLO-Parsing: " + e.getMessage());
        }
    }

    private byte util_calculateChecksum(byte[] bytes) {
        int sum = 0;
        for (byte b : bytes) sum += b;
        return (byte) ((sum | 0x80) & 0xFF);
    }

    /**
     * Formats a frequency from a Win-Test STATUS packet for use as MYQRG.
     *
     * <p>Win-Test transmits the frequency in units of 0.1 kHz. KST4Contest
     * displays frequencies as {@code MHz.kHz.10Hz}, for example
     * {@code 144.300.00} or {@code 10368.100.00}. The MHz part may contain
     * between one and five digits. Deriving its length from the complete value
     * avoids separate and incomplete handling for individual bands.</p>
     *
     * @param frequencyIn100Hz frequency received from Win-Test in units of
     *                         0.1 kHz, equivalent to 100 Hz
     * @return frequency formatted for MYQRG
     * @throws IllegalArgumentException if the supplied frequency is not positive
     *                                  or too small to be formatted
     * @throws ArithmeticException if the supplied value exceeds the supported
     *                             numeric range
     */
    private String helper_formatWinTestFrequency(long frequencyIn100Hz) {
        if (frequencyIn100Hz <= 0) {
            throw new IllegalArgumentException(
                    "Win-Test frequency must be greater than zero"
            );
        }

        /*
         * Multiplication by ten creates a digit sequence whose final five
         * digits represent kHz and 10-Hz groups:
         *
         * 1443210   -> 14432100   -> 144.321.00
         * 103681000 -> 1036810000 -> 10368.100.00
         */
        long frequencyIn10Hz = Math.multiplyExact(
                frequencyIn100Hz,
                10L
        );

        String frequencyDigits = Long.toString(frequencyIn10Hz);

        if (frequencyDigits.length() < 6) {
            throw new IllegalArgumentException(
                    "Win-Test frequency is too small: " + frequencyIn100Hz
            );
        }

        int mhzEndIndex = frequencyDigits.length() - 5;
        int khzEndIndex = frequencyDigits.length() - 2;

        return frequencyDigits.substring(0, mhzEndIndex)
                + "."
                + frequencyDigits.substring(mhzEndIndex, khzEndIndex)
                + "."
                + frequencyDigits.substring(khzEndIndex);
    }

    /**
     * Parses a Win-Test STATUS packet and routes it to a radio.
     *
     * <p>The packet is tokenised while preserving quoted station names.
     * {@code RadioSlotRouter} decides before any frequency is processed: radio 1
     * updates MYQRG, radio 2 (only with SO2R) updates SECONDQRG, and a packet
     * from any other station is ignored. An empty radio 1 station name accepts
     * STATUS packets from every Win-Test station.</p>
     *
     * <p>The main frequency is read from token 7. If pass-frequency use is
     * enabled and token 11 contains a valid frequency, the pass frequency is
     * used instead. A missing or invalid pass frequency deliberately falls
     * back to the main frequency.</p>
     *
     * @param msg complete Win-Test STATUS packet
     */
    /* package-private for tests */
    void parseStatus(String msg) {
        try {
            ArrayList<String> parts = new ArrayList<>();
            Matcher matcher = STATUS_TOKEN_PATTERN.matcher(msg);

            while (matcher.find()) {
                if (matcher.group(1) != null) {
                    parts.add(matcher.group(1));
                } else {
                    parts.add(matcher.group(2));
                }
            }

            if (parts.size() < 8) {
                System.out.println(
                        "[WinTest] STATUS too short: " + msg
                );
                return;
            }

            String stationName = parts.get(1);
            ChatPreferences prefs = client.getChatPreferences();
            java.util.Optional<RadioSlot> slot = RadioSlotRouter.forWinTestStation(
                    prefs.isTrxSynch_so2rEnabled(),
                    stationName,
                    prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1(),
                    prefs.getLogsynch_wintestNetworkStationNameOfWintestClient2());
            if (slot.isEmpty()) {
                return;
            }

            String modeValue = parts.get(5);
            long mainFrequencyRaw = Long.parseLong(parts.get(7));
            double mainFrequencyKHz = mainFrequencyRaw / 10.0;

            String mode;

            if ("1".equals(modeValue)) {
                mode = mainFrequencyKHz > 10000.0 ? "usb" : "lsb";
            } else {
                mode = "cw";
            }

            String formattedMainQrg =
                    helper_formatWinTestFrequency(mainFrequencyRaw);

            /*
             * Token 11 may contain the pass frequency, depending on the
             * Win-Test STATUS packet. Small numeric flag values must not be
             * interpreted as frequencies.
             */
            String formattedPassQrg = null;

            if (parts.size() > 11) {
                try {
                    long passFrequencyRaw =
                            Long.parseLong(parts.get(11));
                    double passFrequencyKHz =
                            passFrequencyRaw / 10.0;

                    if (passFrequencyKHz > 100.0) {
                        formattedPassQrg =
                                helper_formatWinTestFrequency(
                                        passFrequencyRaw
                                );
                    }
                } catch (IllegalArgumentException
                         | ArithmeticException ignored) {
                    /*
                     * Token 11 does not contain a usable frequency.
                     * The main frequency remains the safe fallback.
                     */
                }
            }

            boolean usePassQrg = client
                    .getChatPreferences()
                    .isLogsynch_wintestUsePassQrg();

            final String qrgToSet =
                    usePassQrg && formattedPassQrg != null
                            ? formattedPassQrg
                            : formattedMainQrg;

            if (prefs.isLogsynch_wintestQrgSyncEnabled()) {
                RadioSlot target = slot.get();
                client.uiDispatcher().runOnUi(() -> target.qrgIn(prefs).set(qrgToSet));
            }

            System.out.println(
                    "[WinTest STATUS] stn=" + stationName
                            + ", mode=" + mode
                            + ", qrg=" + formattedMainQrg
                            + (formattedPassQrg != null
                            ? ", passQrg=" + formattedPassQrg
                            : "")
                            + ", selectedQrg=" + qrgToSet
                            + ", syncActive="
                            + client
                            .getChatPreferences()
                            .isLogsynch_wintestQrgSyncEnabled()
            );
        } catch (Exception exception) {
            System.out.println(
                    "[WinTest] STATUS parsing error: "
                            + exception.getMessage()
            );
        }
    }

//    private void send_hello() throws IOException {
//        String payload = String.format("HELLO:\"%s\" \"%s\" \"%s\" %d %d?\0",
//                "DO5AMF", "", stationID, "SLAVE", 1, 14);
//        InetAddress broadcast = InetAddress.getByName("255.255.255.255");
//        byte[] bytes = payload.getBytes(StandardCharsets.US_ASCII);
//        bytes[bytes.length - 2] = util_calculateChecksum((bytes));
//        socket.send(new DatagramPacket(bytes, bytes.length, broadcast, 9871));
//    }


    /**
     * Extracts the unchanged Win-Test band ID from the unquoted ADDQSO fields.
     *
     * @param message complete ADDQSO packet
     * @return raw band ID or an empty value when the field is missing
     */
    static String extractBandIdFromWinTestAddQso(String message) {
        if (message == null) {
            return "";
        }

        String[] quotedParts = message.split("\"");
        if (quotedParts.length <= 6) {
            return "";
        }

        String unquotedFields = quotedParts[6].trim();
        if (unquotedFields.isEmpty()) {
            return "";
        }

        String[] packetFields = unquotedFields.split("\\s+");
        return packetFields.length > 3 ? packetFields[3] : "";
    }

    /**
     * Builds the log identity of an ADDQSO packet.
     *
     * <p>Win-Test numbers the QSOs of every log continuously, so a QSO is only
     * identified by the combination of the logging station, the unique log ID
     * and the QSO number. The log ID is the last field of the packet.</p>
     *
     * @param packetFields fields of the ADDQSO packet
     * @return identity in the form {@code StationName@LogUniqueID}, or
     *         {@code null} when the packet does not carry both values
     */
    static String extractLogIdFromWinTestAddQso(List<String> packetFields) {
        if (packetFields == null || packetFields.size() < ADDQSO_FIELD_COUNT) {
            return null;
        }

        String stationName = packetFields.get(ADDQSO_STATION_NAME_INDEX);
        String logUniqueId = packetFields.get(packetFields.size() - 1);

        if (stationName == null || stationName.isBlank()
                || logUniqueId == null || logUniqueId.isBlank()) {
            return null;
        }

        return stationName.trim() + "@" + logUniqueId.trim();
    }

    /**
     * Extracts the Win-Test QSO number of an ADDQSO packet.
     *
     * <p>Win-Test sends {@code 0} instead of {@code 1} for the first QSO of a
     * log in some situations. wtKST corrects that the same way.</p>
     *
     * @param packetFields fields of the ADDQSO packet
     * @return QSO number, or {@code 0} when the packet carries no usable value
     */
    static long extractQsoNumberFromWinTestAddQso(List<String> packetFields) {
        if (packetFields == null || packetFields.size() < ADDQSO_FIELD_COUNT) {
            return 0L;
        }

        String rawQsoNumber = packetFields.get(ADDQSO_QSO_NUMBER_INDEX);

        if (rawQsoNumber == null) {
            return 0L;
        }

        try {
            long qsoNumber = Long.parseLong(rawQsoNumber.trim());
            return qsoNumber <= 0L ? 1L : qsoNumber;
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    /**
     * Extracts the locator from a Win-Test ADDQSO packet.
     *
     * <p>Current parser model based on the existing split-by-quotes code:
     * <ul>
     *     <li>{@code split("\"")[7]}  = callsign</li>
     *     <li>{@code split("\"")[11]} = received exchange, e.g. 599001</li>
     *     <li>{@code split("\"")[13]} = locator, e.g. JO51UM</li>
     * </ul>
     *
     * <p>If the dedicated locator field is empty, the exchange is used as fallback.</p>
     *
     * @param msg raw ADDQSO message
     * @return normalized six-character locator or null
     */
    private String helper_resolveLocatorFromWinTestAddQso(String msg) {
        if (msg == null) {
            return null;
        }

        String[] quotedParts = msg.split("\"");

        if (quotedParts.length > 13) {
            String locator = WorkedGrossFieldCache.extractLocator6(quotedParts[13]);
            if (locator != null) {
                return locator;
            }
        }

        if (quotedParts.length > 11) {
            return WorkedGrossFieldCache.extractLocator6(quotedParts[11]);
        }

        return null;
    }


    /**
     * Catches add-qso messages of wintest if a new qso gets into the log<br/>
     *
     * String is like this:<br/><br/>
     *ADDQSO: "STN1" "" "STN1" 1762202297 1440000 0 12 0 0 0 2 2 "DM2RN" "599" "599001" "JO51UM" "" "" 0 "" "" "" 44510
     *
     *          ^^^^sentby<br/>
     *                          ^^^^^^^^^^time<br/>
     *                                      ^^^^^^qrg<br/>
     *                                               ^^band<br/>
     *                                                             ^^^^^callsign logged<br/>
     *                                                                                                     stn-id   ^^^^
     * @param msg
     */
    private void parseAddQso(String msg) {
        try {
            List<String> packetFields = WinTestPacket.tokenize(msg);
            String logId = extractLogIdFromWinTestAddQso(packetFields);
            long qsoNumber = extractQsoNumberFromWinTestAddQso(packetFields);

            /*
             * The QSO number is registered before any validation. Otherwise the
             * log synchronization would request a QSO with unusable content
             * over and over again.
             */
            boolean isUnknownQso = logSyncService.registerReceivedQso(logId, qsoNumber);

            String[] quotedParts = msg == null ? new String[0] : msg.split("\"");
            String callSign = quotedParts.length > 7 ? quotedParts[7] : "";
            String rawBandId = extractBandIdFromWinTestAddQso(msg);
            String locatorFromLogger = helper_resolveLocatorFromWinTestAddQso(msg);
            LoggedQsoBand loggedBand = LoggedQsoBand.fromWinTestBandId(rawBandId);
            ExternalLoggedQso loggedQso = ExternalLoggedQso.create(
                    callSign, loggedBand, locatorFromLogger, "WINTEST").orElse(null);
            if (loggedQso == null) {
                System.out.println("[WinTestUDPRcvr: warning] ADDQSO without usable callsign ignored");
                return;
            }

            if (!isUnknownQso) {
                /*
                 * Win-Test resends known QSOs when a NEEDQSO request overlaps
                 * with QSOs that already arrived as a broadcast. Worked state
                 * and database entry exist in that case, so repeating the write
                 * would only cost time during the initial log recovery.
                 */
                return;
            }

            if (loggedBand == null && !rawBandId.isEmpty()) {
                System.out.println("[WinTestUDPRcvr: warning] Unknown band ID: " + rawBandId);
            }

            ChatMember workedCall = loggedQso.toWorkedChatMember();
            client.recordLoggedQso(loggedQso, workedCall);

            File logUDPMessageToThisFile = new File(this.client.getChatPreferences()
                    .getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup());

            FileWriter fileWriterPersistUDPToFile = null;
            BufferedWriter bufwrtrRawMSGOut;

            try {
                fileWriterPersistUDPToFile = new FileWriter(logUDPMessageToThisFile, true);

            } catch (IOException e1) {
                e1.printStackTrace();
            }

            bufwrtrRawMSGOut = new BufferedWriter(fileWriterPersistUDPToFile);

            bufwrtrRawMSGOut.write("\n" + workedCall.toString());
            bufwrtrRawMSGOut.flush();
            bufwrtrRawMSGOut.close();


            System.out.println("[WinTest, Info: Marking Chatmember as worked: " + workedCall.toString());

//            markChatMemberAsWorked(call, band); //TODO

        } catch (Exception e) {
            System.out.println("[WinTest] Fehler beim ADDQSO-Parsing: " + e.getMessage());
        }
    }

}
