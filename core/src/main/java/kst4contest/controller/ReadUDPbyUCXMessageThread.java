package kst4contest.controller;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import kst4contest.ApplicationConstants;
import kst4contest.model.ThreadStateMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import kst4contest.model.ChatMember;
import kst4contest.model.Band;


/**
 * This thread is responsible for reading server's input and printing it to the
 * console. It runs in an infinite loop until the client disconnects from the
 * server.
 *
 * @author www.codejava.net
 */
public class ReadUDPbyUCXMessageThread extends Thread {
	private BufferedReader reader;
	/**
	 * The listening socket. It is owned by this thread: {@link #run()} binds it
	 * (unless a pre-bound socket was handed in) and {@link #interrupt()} closes
	 * it, which is the only way to wake a blocking {@code receive()}.
	 */
	private volatile DatagramSocket socket;
	private ChatController client;
    private int udpPortNr = 12060;
    private ThreadStatusCallback callBackToController;
    private String ThreadNickName = "UDP-Log msg";

//	public ReadUDPbyUCXMessageThread(int localPort , ThreadStatusCallback callback) {
//
////        this.callBackToController = callback;
//	}

	public ReadUDPbyUCXMessageThread(int localPort, ChatController client, ThreadStatusCallback callback) {
		this.udpPortNr = localPort;
        this.client = client;
        this.callBackToController = callback;
	}

	/**
	 * Creates a listener on a socket that is already bound. Tests use this to
	 * listen on an ephemeral loopback port; the application binds the configured
	 * port itself through the public constructor.
	 *
	 * @param boundSocket bound datagram socket this thread takes ownership of
	 * @param client controller that receives the parsed packets
	 * @param callback thread-status sink
	 */
	ReadUDPbyUCXMessageThread(DatagramSocket boundSocket, ChatController client, ThreadStatusCallback callback) {
		this.socket = boundSocket;
		this.udpPortNr = boundSocket.getLocalPort();
		this.client = client;
		this.callBackToController = callback;
	}

	@Override
	public void interrupt() {
		super.interrupt();
		try {
			if (this.socket != null) {
				System.out.println(">>>>>>>>>>>>>>ReadUdpbyUCS: closing socket");
				terminateConnection();
//                callBackToController.onThreadStatus("UDPReceiver", new String[]);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("UCXUDPRDR: catched error " + e.getMessage());
		}
	}

	/**
	 * Strips binary logger framing bytes before the XML payload. Some UCXLog packets
	 * contain transport bytes before the XML declaration.
	 *
	 * @param rawPacket raw UDP payload
	 * @return cleaned XML string or trimmed original text
	 */
	private String helper_extractXmlPayload(String rawPacket) {
		if (rawPacket == null) {
			return "";
		}

		int xmlStart = rawPacket.indexOf("<?xml");
		if (xmlStart < 0) {
			xmlStart = rawPacket.indexOf("<contactinfo");
		}
		if (xmlStart < 0) {
			xmlStart = rawPacket.indexOf("<contactreplace");
		}
		if (xmlStart < 0) {
			xmlStart = rawPacket.indexOf("<RadioInfo");
		}

		return xmlStart >= 0
				? rawPacket.substring(xmlStart).trim()
				: rawPacket.trim();
	}

	/**
	 * Reads an optional XML child node.
	 *
	 * @param element parent element
	 * @param tagName tag to read
	 * @return trimmed value or empty string
	 */
	private String helper_getOptionalElementText(Element element, String tagName) {
		if (element == null || tagName == null) {
			return "";
		}

		NodeList nodeList = element.getElementsByTagName(tagName);
		if (nodeList == null || nodeList.getLength() == 0 || nodeList.item(0) == null) {
			return "";
		}

		String textContent = nodeList.item(0).getTextContent();
		return textContent == null ? "" : textContent.trim();
	}

	/**
	 * Resolves the QSO locator from UCXLog contactinfo. gridsquare is preferred,
	 * rcvnr is used as fallback for exchanges such as 001JO41HK.
	 *
	 * @param element contactinfo XML element
	 * @return normalized six-character locator or null
	 */
	private String helper_resolveLocatorFromContactInfo(Element element) {
		String gridSquare = WorkedGrossFieldCache.extractLocator6(helper_getOptionalElementText(element, "gridsquare"));
		if (gridSquare != null) {
			return gridSquare;
		}

		return WorkedGrossFieldCache.extractLocator6(helper_getOptionalElementText(element, "rcvnr"));
	}

	public void run() {

        System.out.println("ReadUDPByUCXLogThread: started Thread for UCXLog getUDP");
		Thread.currentThread().setName("ReadUDPByUCXLogThread");


        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "initialized", false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

        boolean running;
		byte[] buf = new byte[1777];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		try {
//			socket = new DatagramSocket(12060);
			if (socket == null) {
				socket = new DatagramSocket(udpPortNr);
			}
			socket.setSoTimeout(2000); //TODO try for end properly
		}

		catch (SocketException e) {
			//this will catch the repeating Sockettimeoutexception...nothing to do
//			e.printStackTrace();
		}

		/*
		 * interrupt() sets the flag and closes the socket; the closed socket makes
		 * the blocking receive() throw, and the flag ends the loop.
		 */
		while (!isInterrupted()) {

			boolean timeOutIndicator = false;

//			packet = new DatagramPacket(buf, buf.length); //TODO: Changed that due to memory leak, check if all works (seems like that)
//    		 DatagramPacket packet  = new DatagramPacket(SRPDefinitions.BYTE_BUFFER_MAX_LENGTH); //TODO: Changed that due to memory leak, check if all works (seems like that)
			try {
				/*
				 * A DatagramPacket remembers the length of the last datagram it
				 * received; without this reset a long packet after a short one is
				 * truncated to the short one's length (#115).
				 */
				packet.setLength(packet.getData().length);
				socket.receive(packet);

			} catch (SocketTimeoutException e2) {

				timeOutIndicator = true;
				// this will catch the repeating Sockettimeoutexception...nothing to do
//				e2.printStackTrace();
			}
			catch (IOException e) {
				if (isInterrupted() || socket == null || socket.isClosed()) {
					break;
				}
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException nE) {
				// TODO Auto-generated catch block
				nE.printStackTrace();
				System.out.println("ReadUdpByUCXTH: Socket not ready");

				try {
					socket = new DatagramSocket(client.getChatPreferences().getLogsynch_ucxUDPWkdCallListenerPort());
					socket.setSoTimeout(2000);
				} catch (SocketException e) {
					System.out.println("[ReadUDPByUCSMsgTH, Error]: socket in use or something:");
					e.printStackTrace();

					try {
						socket = new DatagramSocket(null);
						socket.setReuseAddress(true);
						socket.bind(new InetSocketAddress(client.getChatPreferences().getLogsynch_ucxUDPWkdCallListenerPort()));
						packet.setLength(packet.getData().length);
						socket.setSoTimeout(3000);
						socket.receive(packet);
					} catch (Exception ex) {
						System.out.println("ReadUDPByUCXMsgTh: Could not solve that. Program Restart needed.");
						throw new RuntimeException(ex);
					}

				}

			}

			InetAddress address = packet.getAddress();
			int port = packet.getPort();
//			packet = new DatagramPacket(buf, buf.length, address, port);
			String received = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
			received = received.trim();


//			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<recv " + received);

			if (received.contains(ApplicationConstants.DISCONNECT_RDR_POISONPILL)) {
				System.out.println("ReadUdpByUCX, Info: got poison, now dieing....");
				socket.close();
				timeOutIndicator = true;

//                threadStatusMessage = new String[2];
//                threadStatusMessage[0] = "stopped";
//                threadStatusMessage[1] = "by poisonpill message (disconnect on purpose)";
                threadStateMessage = new ThreadStateMessage(this.ThreadNickName, false, "stopped by Poisonpill", false);
                callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
				break;
			}

			if (this.client.isDisconnectionPerformedByUser()) {
				break;//TODO: what if it´s not the finally closage but a band channel change?
			}

			if (!timeOutIndicator) {
				/*
				 * Safety net: one malformed or unexpected packet must not end the
				 * listener. The packet is dropped and the loop keeps receiving.
				 */
				try {
					processUCXUDPMessage(received);
				} catch (RuntimeException exception) {
					System.out.println("[ReadUDPFromUCX, warning]: packet dropped after unexpected error: "
							+ exception);
					exception.printStackTrace();
					ThreadStateMessage droppedPacketMessage = new ThreadStateMessage(this.ThreadNickName, true,
							"packet dropped after error: " + exception, false);
					try {
						callBackToController.onThreadStatus(ThreadNickName, droppedPacketMessage);
					} catch (RuntimeException callbackException) {
						// A failing status callback must not end the listener either.
						System.out.println("[ReadUDPFromUCX, warning]: status callback failed: "
								+ callbackException);
					}
				}
			} else {
				//dont process the empty message
			}

			buf = new byte[1777]; // reset buffer for future smaller packets

		}

		// Covers an interrupt() that arrived before the socket was bound.
		DatagramSocket listeningSocket = socket;
		if (listeningSocket != null && !listeningSocket.isClosed()) {
			listeningSocket.close();
		}

	}

	public String processUCXUDPMessage(String udpPacketToProcess) {

		String udpMsg = helper_extractXmlPayload(udpPacketToProcess);

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "received Message\n" + udpMsg, false);

        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e1) {

			e1.printStackTrace();
		}

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(udpMsg)));

			/**
			 * case Log-QSO-Packet in ucxlog / DXLog and compatible
			 * 
			 */
			NodeList list = doc.getElementsByTagName("contactinfo");

			if (list.getLength() == 0) {
				list = doc.getElementsByTagName("contactreplace");
				//DXlog will send contactreplace instead of contactinfo on clicking "broadcast whole logbook"
			}

			if (list.getLength() != 0) {

				/*
				 * QSO and TRX information share the same UDP receiver. The log-sync
				 * preference therefore controls processing, not ownership of the socket.
				 */
				if (!client.getChatPreferences().isLogsynch_ucxUDPWkdCallListenerEnabled()) {
					return "";
				}

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String call = helper_getOptionalElementText(element, "call");
						String rawBand = helper_getOptionalElementText(element, "band");
						String gridSquare = helper_resolveLocatorFromContactInfo(element);
						String points = helper_getOptionalElementText(element, "points");
						LoggedQsoBand loggedBand = LoggedQsoBand.fromLoggerValue(rawBand);
						ExternalLoggedQso loggedQso = ExternalLoggedQso.create(
								call, loggedBand, gridSquare, "UCXLOG").orElse(null);
						if (loggedQso == null) {
							System.out.println("[ReadUDPFromUCX, warning]: QSO packet without usable callsign ignored");
							continue;
						}

						System.out.println("[Readudp, info ]: received Current Element :" + node.getNodeName()
								+ "call: " + call + " / " + rawBand + " ----> " + points + " POINTS");

//						client.getChatPreferences().setBcn_contestScoreSum(Long.parseLong(points));

						ChatMember workedCall = loggedQso.toWorkedChatMember();
						if (loggedBand == null && !rawBand.isEmpty()) {
							System.out.println("[ReadUDPFromUCX, warning]: unexpected band value: \"" + rawBand + "\"");
						}

						client.recordLoggedQso(loggedQso, workedCall);
						

						appendToUdpBackupFile(this.client.getChatPreferences()
								.getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup(),
								"\n" + workedCall.toString());

					}
				}
			} else {
				list = doc.getElementsByTagName("RadioInfo");

				/*
				 * RadioInfo packets may arrive on the shared UDP port even when automatic
				 * QRG synchronization is disabled. Ignore them unless TRX sync is enabled.
				 */
				if (list.getLength() != 0
						&& !client.getChatPreferences().isTrxSynch_ucxLogUDPListenerEnabled()) {
					return "";
				}

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						String formattedQRG;

						Element element = (Element) node;

						Node freqNode = element.getElementsByTagName("Freq").item(0);
						Node modeNode = element.getElementsByTagName("Mode").item(0);
						if (freqNode == null || modeNode == null) {
							System.out.println("[ReadUDPFromUCX, warning]: RadioInfo without Freq or Mode ignored");
							continue;
						}

						Node radioNrNode = element.getElementsByTagName("RadioNr").item(0);
						String radioNr = radioNrNode == null ? null : radioNrNode.getTextContent();
						java.util.Optional<RadioSlot> slot = RadioSlotRouter.forRadioInfo(
								client.getChatPreferences().isTrxSynch_so2rEnabled(), radioNr);
						if (slot.isEmpty()) {
							System.out.println("[ReadUDPFromUCX, warning]: RadioInfo for unknown RadioNr \"" + radioNr + "\" ignored");
							continue;
						}

						String qrg = freqNode.getTextContent();
						String mode = modeNode.getTextContent();

//						System.out.println("QRG Length: " + qrg.length() + " // " + qrg);

						/**
						 * The following if statement is only for formatting the frequency input for
						 * good readability to avoid values like 129601000 and set it to something
						 * readable like 1296.010.00
						 * 
						 */
						if (qrg.length() == 6) {
							// 701000 KHz
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 1), qrg.substring(2, 5),
									qrg.substring(5, 6));

						} else if (qrg.length() == 7) {
							// 700000 KHz
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 2), qrg.substring(2, 5),
									qrg.substring(5, 7));
						} else if (qrg.length() == 8) {
							// 144.123.22 KHz
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 3), qrg.substring(3, 6),
									qrg.substring(6, 8));
						} else if (qrg.length() == 9) {
							// 1296.010.00
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 4), qrg.substring(4, 7),
									qrg.substring(7, 9));
						} else if (qrg.length() == 10) {
							// 10000.010.00
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 5), qrg.substring(5, 8),
									qrg.substring(8, 10));
						}

						else {
							formattedQRG = qrg;
						}

//						System.out.println("Current Element :" + node.getNodeName());
//						System.out.println("Radio QRG : " + qrg);
//						System.out.println("Radio Mode: " + mode);
//						System.out.println("[ReadUDPFromUCX, Info:] Setted QRG pref to: \"" + qrg + "\"" );

//						this.client.getChatPreferences().getMYQRGFirstCat().set(formattedQRG);

						final String finalFormattedQRG = formattedQRG;
						final RadioSlot target = slot.get();
						helper_runOnFxThread(() ->
								target.qrgIn(this.client.getChatPreferences()).set(finalFormattedQRG)
						);

//						System.out.println("[ReadUDPbyUCXTh: ] Radioinfo processed: " + formattedQRG);
					}
				}

			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			System.out.println(e.getCause());
			System.out.println(e.getMessage());

//            threadStatusMessage = new String[2];
//            threadStatusMessage[0] = "STOPPED";
//            threadStatusMessage[1] = Arrays.toString(e.getStackTrace());
            threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "CRASHED" + udpMsg, true);
            threadStateMessage.setCriticalStateFurtherInfo(Arrays.toString(e.getStackTrace()));

            callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

		}

//		System.out.println("[ReadUDPbyUCXTh: ] worked size = " + this.client.getMap_ucxLogInfoWorkedCalls().size());
//		System.out.println("[ReadUDPbyUCXTh: ] worked size = removeThisActions" );

		return "";
	}

	/**
	 * Appends one line to the UDP backup file. The backup is a convenience copy:
	 * the QSO has already been applied and stored when this runs, so a missing,
	 * empty or unwritable path is logged and the write is skipped.
	 *
	 * @param backupFilePath configured backup file path, may be null or blank
	 * @param text text to append
	 */
	private void appendToUdpBackupFile(String backupFilePath, String text) {
		if (backupFilePath == null || backupFilePath.isBlank()) {
			System.out.println("[ReadUDPFromUCX, warning]: no UDP backup file configured, backup write skipped");
			return;
		}

		try (BufferedWriter backupWriter = new BufferedWriter(new FileWriter(new File(backupFilePath), true))) {
			backupWriter.write(text);
		} catch (IOException exception) {
			System.out.println("[ReadUDPFromUCX, warning]: cannot write UDP backup file \""
					+ backupFilePath + "\": " + exception.getMessage());
		}
	}

	public boolean terminateConnection() throws IOException {
//        String[] threadStatusMessage = new String[2];
//        threadStatusMessage = new String[2];
//        threadStatusMessage[0] = "STOPPED";
//        threadStatusMessage[1] = "Connection terminated for purpose.";
//        callBackToController.onThreadStatus(ThreadNickName,threadStatusMessage);

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, false, "terminated", false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

		DatagramSocket listeningSocket = this.socket;
		if (listeningSocket != null && !listeningSocket.isClosed()) {
			listeningSocket.close();
		}

		return true;

	}

	/**
	 * Runs UI-bound changes on the JavaFX application thread.
	 *
	 * <p>UCXLog UDP packets are processed in a background thread. Some preference
	 * properties are bound to JavaFX controls, so setting them directly from this
	 * thread can crash JavaFX with "Not on FX application thread".</p>
	 *
	 * @param runnable UI-bound update
	 */
	private void helper_runOnFxThread(Runnable runnable) {
		if (runnable == null) {
			return;
		}

		if (client.uiDispatcher().isUiThread()) {
			runnable.run();
		} else {
			client.uiDispatcher().runOnUi(runnable);
		}
	}

}
