package kst4contest.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import kst4contest.observe.MutableValue;
import kst4contest.observe.SimpleRoster;
import kst4contest.observe.SimpleValue;

/**
 * refactored version if ChatPreferences
 */
public class ChatPreferences {


	// Main window: right split pane (ChatMember table / Priority list / FurtherInfo)
	// 3 items => 2 dividers => 2 positions.
	private double[] GUImainWindowRightSplitPane_dividerposition = new double[] { 0.53, 0.78 };

	// Defaults used for config upgrades / first start.
	private final double[] GUImainWindowRightSplitPane_dividerpositionDefault = new double[] { 0.53, 0.78 };


	/**
	 * Bump this when a change to the XML schema needs a reader to act on the version.
	 * <p>
	 * Adding an element does not: reading must stay backwards compatible, every setting falls
	 * back to its own default, and nothing in this application branches on the number -- the
	 * only use is a best-effort warning when a file is newer than the application. Release
	 * 1.50 added the two palette-override elements and deliberately left this at 7. A bump
	 * that no reader acts on only invites the next person to bump it again.
	 * Version 9 marks the manual-QTF-heading opt-out migration. A missing opt-out marker
	 * enables the feature, and a subsequently written marker preserves the user's decision.
	 * <p>
	 * Reading must stay backwards compatible: missing/unknown tags should fall back to defaults.
	 */
//	private static final int CONFIG_VERSION = 2;
	public static final int CONFIG_VERSION = 9;

	// Prefer writing tag names that mirror variable names (human readable). Keep legacy tags for compatibility.
	private static final String TAG_CONFIG_VERSION = "configVersion";

	/**
	 * Name of file to store preferences in.
	 */
	public static final String PREFERENCES_FILE = "preferences.xml";

	/**
	 * Resource with the example properties xml file.
	 */
	public static final String PREFERENCE_RESOURCE = "/praktiKSTpreferences.xml";



	/**
	 * Default constructor will set the default values (also for predefined texts
	 * and shorts) automatically at initialization
	 *
	 * TODO: delete this from the kst4contest.view/Main.java!
	 */
	public ChatPreferences() {
		this(PREFERENCES_FILE);
	}

	/**
	 * Creates preferences backed by an explicit file without touching the user
	 * profile. This is useful for isolated imports, tools and tests.
	 *
	 * @param preferencesFile preferences XML path
	 */
	public ChatPreferences(Path preferencesFile) {
		if (preferencesFile == null) {
			throw new IllegalArgumentException("Preferences file must not be null");
		}
		storeAndRestorePreferencesFileName = preferencesFile.toString();
	}

	/**
	 * Creates preferences bound to one operator profile.
	 *
	 * <p>The file name is resolved below the application directory, so both
	 * "preferences.xml" for the root installation and "profiles/OP2/preferences.xml"
	 * for an additional operator profile are valid. A missing file is seeded from the
	 * bundled template, which gives a new profile the same clean defaults a first-ever
	 * installation gets.</p>
	 *
	 * @param applicationRelativeFileName preferences file name relative to the application directory
	 */
	public ChatPreferences(final String applicationRelativeFileName) {
		ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, PREFERENCE_RESOURCE, applicationRelativeFileName);
		this.storeAndRestorePreferencesFileName = ApplicationFileUtils.getFilePath(
				ApplicationConstants.APPLICATION_NAME,
				applicationRelativeFileName
		);

//        lstNotify_QSOSniffer_sniffedCallSignList.add("DF0GEB");

//		shortcuts[2] = "pse";
//		shortcuts[3] = "turn";
//		shortcuts[4] = "ant";
//		shortcuts[5] = "my";
//		shortcuts[6] = "dir";
//		shortcuts[7] = "sked";
//		shortcuts[8] = "ssb";
//		shortcuts[9] = "cw";
//		shortcuts[10] = "try";
//		shortcuts[11] = "agn";
//		shortcuts[12] = "nw";
//		shortcuts[13] = "qrg";
//		shortcuts[14] = "beaming";
//		shortcuts[15] = "calling";
//		shortcuts[16] = "lsn to";
//		shortcuts[17] = "qsb";
//		shortcuts[18] = "rpt";
//		shortcuts[19] = "nr";
//		shortcuts[20] = "ur";
//		shortcuts[21] = "I";
//		shortcuts[22] = "hear";
//		shortcuts[23] = "you";
//		shortcuts[24] = "weak";
//		shortcuts[25] = "nil, sry";
//		shortcuts[26] = "maybe";
//		shortcuts[27] = "later";
//		shortcuts[28] = "tmw";
//		shortcuts[29] = "rrr";
//		shortcuts[30] = "tnx";
//		shortcuts[31] = "qso";
//		shortcuts[32] = "73";
//		shortcuts[33] = "?";
//		shortcuts[34] = "!";
//		shortcuts[35] = ",";
//		shortcuts[36] = "MYQRG";
//
//		textSnippets[0] = "Hi OM, try sked?";
//		textSnippets[1] = "I am calling cq ur dir, pse lsn to me at ";
//		textSnippets[2] = "pse ur qrg?";
//		textSnippets[3] = "rrr, I move to your qrg nw, pse ant dir me";
//		textSnippets[4] = "Hrd you but many qrm here, pse agn";
//
//		textSnippets[5] = "I turn my ant to you";
//
//		textSnippets[6] = "Sry, strong qrm by local station there, may try ";
//		textSnippets[7] = "Sry, in qso nw, pse qrx, I will meep you";
//
//		textSnippets[8] = "Ur ant my dir nw?";
//		textSnippets[9] = "nil?";
//		textSnippets[10] = "No cw op here, could we use ssb?";
//		textSnippets[11] = "No chance in ssb, could we use cw?";
//
//		textSnippets[12] = "Nil till now, are you calling?";
//		textSnippets[13] = "Nil, I will look for an ap";
//		textSnippets[14] = "Tnx try, maybe later!";
//		textSnippets[15] = "Tnx fb qso, all ok, 73 es gl!";
	}

	/**
	 * Preferences for the preferences
	 * kst4contest@googlegroups.com
	 * praktimarc+kst4contest@gmail.com
	 *
	 */

	String programVersion = "Chat is powered by ON4KST \n\nUsage is free. You are welcome to support: \n\n- my project (donations, bugreports, good ideas are welcome), \n- ON4KST Servers, \n- AirScout developers and \n- OV3T (best AS-data provider of the world). \n\n73 de DO5AMF, Marc (DM5M / DARC X08)\nand DN9APW, Philipp Wagner";
	String logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = "udpReaderBackup.txt";
	String storeAndRestorePreferencesFileName = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, PREFERENCES_FILE);
	String chatState; // working variable only for use by primarystage (title bar)
//	ObservableStringValue chatState;

	/**
	 * Station preferences
	 */



	String stn_on4kstServersDns = "www.on4kst.org";
	int stn_on4kstServersPort = 23001;

	boolean stn_pstRotatorEnabled = false;
	boolean stn_qtfFilterDirectionAsAntennaHeadingOptedOut;
	String stn_pstRotatorHost = "127.0.0.1";
	int stn_pstRotatorPort = 12000;
	boolean stn_pstRotator2Enabled = false;
	String stn_pstRotator2Host = "127.0.0.1";
	int stn_pstRotator2Port = 12010;

	boolean stn_loginAFKState = false; //always start as here
	/*
	 * The login credentials default to empty on purpose. A missing or empty value in
	 * preferences.xml means "not configured yet", and falling back to a real callsign
	 * would let an operator transmit under someone else's call. This matters for every
	 * additional operator profile, whose preferences are created without credentials.
	 */
	String stn_loginCallSign = "";
	String stn_loginCallSignRaw = ""; //for example: do5amf instead of logincallsign do5amf-2
	String stn_loginPassword = "";
	String stn_loginNameMainCat = "KST4Contest";
	String stn_loginNameSecondCat = "KST4ContestSHF";
	String stn_loginLocatorMainCat = "jn49fk";

	double stn_antennaBeamWidthDeg = 50;
	double stn_maxQRBDefault = 900;
	double stn_qtfDefault = 135;

	double stn_pathAnalysisOwnAntennaHeightMeters = 10.0;
	double stn_pathAnalysisDefaultTargetAntennaHeightMeters = 10.0;
	/**
	 * Ground elevation of the own site above mean sea level, entered by the operator.
	 * NaN (the default, and an empty field) leaves it to the elevation model at the
	 * locator centre.
	 */
	double stn_pathAnalysisOwnHeightAslMeters = Double.NaN;
	/**
	 * Whether the path analysis ends at the highest sampled point of the remote station's
	 * locator square instead of at the square centre.
	 */
	boolean stn_pathAnalysisDxHighestPointInSquare = true;
	String stn_pathAnalysisDemRootDirectory = "";
	String stn_pathAnalysisDemDatasetId = "copernicus_glo_30";
	double stn_pathAnalysisOwnTxPowerWatts = 750.0;
	double stn_pathAnalysisOwnAntennaGainDbi = 8.0;
	double stn_pathAnalysisDefaultTargetTxPowerWatts = 100.0;
	double stn_pathAnalysisDefaultTargetAntennaGainDbi = 8.0;

	double stn_pathAnalysisVhfFeederLossPerStationDb = 2.0;
	double stn_pathAnalysisFeederLossIncreaseDbPer200MHz = 2.0;
	double stn_pathAnalysisMaxEstimatedFeederLossPerStationDb = 20.0;

	double stn_pathAnalysisRequiredSsbSignalDbm = -126.0;
	double stn_pathAnalysisRequiredCwSignalDbm = -132.0;
	double stn_pathAnalysisContestMarginDb = 6.0;

	ChatCategory loginChatCategoryMain = new ChatCategory(2);
	ChatCategory loginChatCategorySecond = new ChatCategory(3);
	boolean loginToSecondChatEnabled;
	MutableValue<Double> actualQTF = new SimpleValue<>(360.0); // will be updated by user at runtime!
	MutableValue<Double> actualQTF2 = new SimpleValue<>(Double.NaN); // heading of rotator 2 (SO2R), NaN when unknown
	private boolean actualQtfInitializedFromPreferences;

	boolean stn_bandActive50;
	boolean stn_bandActive70;
	boolean stn_bandActive144;
	boolean stn_bandActive432;
	boolean stn_bandActive1240;
	boolean stn_bandActive2300;
	boolean stn_bandActive3400;
	boolean stn_bandActive5600;
	boolean stn_bandActive10G;

	/**
	 * Log Synch preferences
	 */
	String logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = "SimpleLogFile.txt";
	boolean logsynch_fileBasedWkdCallInterpreterEnabled = true;

	int logsynch_ucxUDPWkdCallListenerPort = 12060;
	boolean logsynch_ucxUDPWkdCallListenerEnabled = true;

	String logsynch_wintestNetworkStationNameOfKST = "KST4Contest";
	String logsynch_wintestNetworkStationNameOfWintestClient1 = "STN1";
	String logsynch_wintestNetworkStationNameOfWintestClient2 = "";
	boolean logsynch_wintestNetworkSimulationEnabled = false;
	int logsynch_wintestNetworkStationIDOfKST = 55555;
	int logsynch_wintestNetworkPort = 9871;
	boolean logsynch_wintestNetworkListenerEnabled = true; // default true = bisheriges Verhalten
	String logsynch_wintestNetworkBroadcastAddress = "255.255.255.255"; // UDP broadcast address for sending to Win-Test
	boolean logsynch_wintestNetworkSkedPushEnabled = false; // push SKEDs to Win-Test via UDP
	String logsynch_wintestSkedMode = "SSB"; // Supported values: SSB or CW
	boolean logsynch_wintestQrgSyncEnabled = true;  // sync QRG from Win-Test STATUS packet
	boolean logsynch_wintestUsePassQrg = false;      // use pass frequency instead of main QRG from STATUS packet



	/**
	 * TRX Synch prefs
	 */
	MutableValue<String> MYQRGFirstCat = new SimpleValue<>(); // own qrg will be set by user entry or ucxlog if trx Synch is
	// activated
	MutableValue<String> MYQRGSecondCat = new SimpleValue<>(); // own qrg will be set by user entry or ucxlog if trx Synch is activated

	boolean trxSynch_ucxLogUDPListenerEnabled = false;
	boolean trxSynch_so2rEnabled = false;

	/**
	 * AirScout prefs
	 */
	boolean AirScout_asUDPListenerEnabled = true;
	String AirScout_asServerNameString = "AS";
	String AirScout_asClientNameString = "KST";
	boolean AirScout_autoBandSelectionEnabled = true;
	String AirScout_asBandString = "1440000";
	int AirScout_asCommunicationPort = 9872;

	/**
	 * Notification prefs
	 */



	//Audio section
	boolean notify_playSimpleSounds = true;
	boolean notify_playCWCallsignsOnRxedPMs = true;
	boolean notify_playVoiceCallsignsOnRxedPMs = true;

	//DXCluster section
	boolean notify_dxClusterServerEnabled = true;
	int notify_dxclusterServerPort = 8000; //default 8000 like db0sue.de
	MutableValue<String> notify_optionalFrequencyPrefix = new SimpleValue<>("144");

	MutableValue<String> notify_DXCSrv_SpottersCallSign = new SimpleValue<>("DO5AMF");

	boolean notify_DXClusterServerTriggerBearing;
	boolean notify_DXClusterServerTriggerOnQRGDetect;

	SimpleRoster<String>
			lstNotify_QSOSniffer_sniffedCallSignList =
			new SimpleRoster<>();
	//    SimpleRoster<String> lstNotify_QSOSniffer_sniffedCallSignList = new SimpleRoster<>();
	SimpleRoster<String> lstNotify_QSOSniffer_sniffedPrefixLocList = new SimpleRoster<>();

	// Scoring / interaction metrics
	int notify_noReplyPenaltyMinutes = 13;          // if we ping via /cq and no response arrives within X minutes => penalty strike
	int notify_momentumWindowSeconds = 180;         // momentum window size (count inbound lines)
	String notify_positiveSignalsPatterns = "QRV;READY;RX;RGR;RR;OK;YES;TNX;TU;HEARD;LSN"; //TODO: to be continued

	boolean notify_bandUpgradeHintOnLogEnabled = true;          // show hint after log entry if station QRV on other unworked enabled band
	boolean notify_bandUpgradePriorityBoostEnabled = false;     // optional score boost to make it stand out in toplists


	/**
	 * Shortcuts and Textsnippets prefs
	 */
//	String[] shortcuts = new String[37];
//	String[] textSnippets = new String[16];
	SimpleRoster<String> lst_txtSnipList = new SimpleRoster<>();
	SimpleRoster<String> lst_txtShortCutBtnList = new SimpleRoster<>();

	/**
	 * Beacon prefs
	 */

	boolean bcn_beaconsEnabledMainCat = true;
	boolean bcn_beaconsEnabledSecondCat = false;
	int bcn_beaconIntervalInMinutesMainCat = 20;
	int bcn_beaconIntervalInMinutesSecondCat = 20;
	String bcn_beaconTextMainCat = "Hi, pse call us";
	String bcn_beaconTextSecondCat = "Hi, pse call us";
	long bcn_contestScoreSum;
	int bcn_contestQsoSum;
	String bcn_contestODXCallsignKms;

	/**
	 * Unworked station requester prefs
	 */

	boolean messageHandling_unworkedStnRequesterBeaconsEnabled;
	int messageHandling_unworkedStnRequesterBeaconsInterval;
	String messageHandling_unworkedStnRequesterBeaconsText;
	String messageHandling_beaconUnworkedstationsPrefix;

	String messageHandling_autoAnswerTextMainCat = "Hi, sry I am not qrv, just testing new features of KST4Contest " +  ApplicationConstants.APPLICATION_CURRENT_VERSION;
	String messageHandling_autoAnswerTextSecondCat = "Hi, sry I am not qrv, just testing new features of KST4Contest " +  ApplicationConstants.APPLICATION_CURRENT_VERSION;

	boolean messageHandling_autoAnswerEnabled = false;
	boolean messageHandling_autoAnswerEnabledSecondCat = false;

	boolean messageHandling_autoAnswerToQRGRequestEnabled;
	private volatile boolean messageHandling_debugModeToFileEnabled = true;

	/*********************************************************************************
	 *
	 * GUI OPTIONS WHICH CAN BE CHANGED WILL BE SAVED HERE
	 *
	 *********************************************************************************/

	boolean guiOptions_defaultFilterNothing;
	boolean guiOptions_defaultFilterPmToMe;
	boolean guiOptions_defaultFilterPmToOther;
	boolean guiOptions_defaultFilterPublicMsgs;
	boolean guiOptions_showGrossFieldWorkedHintInBandColumns = true; // show "o" (grid square already worked on this band) in the band columns
	boolean guiOptions_showFreshCallHintInBandColumns = true; // show "a" (band available, call not worked on any band yet) instead of always "B+" in the band columns

	/**
	 * The operator's colour overrides for the daylight design, encoded by PaletteOverrides.
	 * Per profile, like every other preference: the file this is written to is the profile's
	 * own. One string rather than six elements, because this file is written element by
	 * element by hand and twelve more would be twelve write blocks and twelve read blocks.
	 */
	private String guiOptions_paletteOverridesDay = "";

	/** The same for the evening design. Two designs, two independent sets. */
	private String guiOptions_paletteOverridesEvening = "";

	/**
	 * The operator's interface language: empty for the system language, otherwise a language
	 * code such as "de". Per profile, like every other GUI preference -- at a multi operator
	 * station two operators may want two languages.
	 */
	private String guiOptions_language = "";

	private double[] GUIstationMapStageSceneSizeHW = new double[] { 1000, 800 };
	private double[] GUIstationMapStagePositionXY = new double[] { Double.NaN, Double.NaN };
	private boolean GUIstationMapPathAnalysisVisible = true;
	private boolean GUIstationMapClusteringEnabled = true;
	private boolean GUIstationMapAlwaysOnTop = false;
	private final Map<String, Double> tableColumnWidths = new LinkedHashMap<>();

	private static final String TAG_TABLE_COLUMN_WIDTH = "tableColumnWidth";
	private static final double MIN_TABLE_COLUMN_WIDTH = 16.0;
	private static final double MAX_TABLE_COLUMN_WIDTH = 10_000.0;


	/*********************************************************************************
	 *
	 * GUI SETTING VARIABLES WILL BE BUFFERED HERE, DEFAULTS TOO
	 *
	 *********************************************************************************/

	private boolean GUI_darkModeActive = false;
	private boolean GUI_darkModeActiveByDefault = false;

	/**
	 * Opt-in: windows reopen across restarts where the operator last moved them, on the
	 * same screen. Off by default, because a stored position outlives the monitor layout
	 * it was made on; the positions are only used when that exact screen is connected.
	 */
	private boolean guiOptions_rememberWindowPositions = false;

	/**
	 * Last operator-chosen window positions by window id, each as
	 * {x, y, screenX, screenY, screenWidth, screenHeight} in desktop coordinates.
	 */
	private final Map<String, int[]> windowPositions = new LinkedHashMap<>();

	private static final String TAG_WINDOW_POSITION = "windowPosition";

	private double[] GUIscn_ChatwindowMainSceneSizeHW = new double[] {768, 1234};
	private double[] GUIclusterAndQSOMonStage_SceneSizeHW = new double[] {700, 500};
	private double[] GUIstage_updateStage_SceneSizeHW = new double[] {580, 480};
	private double[] GUIsettingsStageSceneSizeHW = new double[] {720, 768};

	private double[] GUIselectedCallSignSplitPane_dividerposition = {0.55};
	private double[] GUImainWindowLeftSplitPane_dividerposition = {0.51};
	private double[] GUImessageSectionSplitpane_dividerposition = {0.62, 0.7, 0.75, 0.9}; //3 deviders now //TODO: more should be possible?
//	private double[] GUImainWindowRightSplitPane_dividerposition = {0.72};
	private double[] GUIpnl_directedMSGWin_dividerpositionDefault = {0.8};


	public boolean isMessageHandling_autoAnswerEnabledSecondCat() {
		return messageHandling_autoAnswerEnabledSecondCat;
	}

	public boolean isMessageHandling_debugModeToFileEnabled() {
		return messageHandling_debugModeToFileEnabled;
	}

	public void setMessageHandling_debugModeToFileEnabled(
			boolean messageHandling_debugModeToFileEnabled
	) {
		this.messageHandling_debugModeToFileEnabled =
				messageHandling_debugModeToFileEnabled;
	}

	public void setMessageHandling_autoAnswerEnabledSecondCat(boolean messageHandling_autoAnswerEnabledSecondCat) {
		this.messageHandling_autoAnswerEnabledSecondCat = messageHandling_autoAnswerEnabledSecondCat;
	}

	public MutableValue<String> getMYQRGSecondCat() {
		return MYQRGSecondCat;
	}

	public void setMYQRGSecondCat(String MYQRGSecondCat) {
		this.MYQRGSecondCat.set(MYQRGSecondCat);
	}

	public boolean isMessageHandling_autoAnswerEnabled() {
		return messageHandling_autoAnswerEnabled;
	}

	public String getMessageHandling_autoAnswerTextSecondCat() {
		return messageHandling_autoAnswerTextSecondCat;
	}

	public void setMessageHandling_autoAnswerTextSecondCat(String messageHandling_autoAnswerTextSecondCat) {
		this.messageHandling_autoAnswerTextSecondCat = messageHandling_autoAnswerTextSecondCat;
	}

	public String getMessageHandling_beaconUnworkedstationsPrefix() {
		return messageHandling_beaconUnworkedstationsPrefix;
	}

	public MutableValue<String> MYQRGFirstCatProperty() {
		return MYQRGFirstCat;
	}

	public void setMYQRGFirstCat(String MYQRGFirstCat) {
		this.MYQRGFirstCat.set(MYQRGFirstCat);
	}

	public double getStn_pathAnalysisOwnAntennaHeightMeters() {
		return stn_pathAnalysisOwnAntennaHeightMeters;
	}

	public void setStn_pathAnalysisOwnAntennaHeightMeters(double stn_pathAnalysisOwnAntennaHeightMeters) {
		this.stn_pathAnalysisOwnAntennaHeightMeters = Math.max(0.0, stn_pathAnalysisOwnAntennaHeightMeters);
	}

	/** Lowest own ground elevation accepted, metres: below the Dead Sea shore. */
	public static final double PATH_ANALYSIS_OWN_HEIGHT_ASL_MIN_METERS = -500.0;
	/** Highest own ground elevation accepted, metres: above the highest summit. */
	public static final double PATH_ANALYSIS_OWN_HEIGHT_ASL_MAX_METERS = 9000.0;

	/**
	 * @return own ground elevation above mean sea level in metres, or NaN when the
	 *         elevation model decides
	 */
	public double getStn_pathAnalysisOwnHeightAslMeters() {
		return stn_pathAnalysisOwnHeightAslMeters;
	}

	/**
	 * Sets the own ground elevation above mean sea level. NaN, or a value outside
	 * {@link #PATH_ANALYSIS_OWN_HEIGHT_ASL_MIN_METERS} ..
	 * {@link #PATH_ANALYSIS_OWN_HEIGHT_ASL_MAX_METERS}, clears it, so a broken value in
	 * the XML file falls back to the elevation model instead of distorting every path.
	 */
	public void setStn_pathAnalysisOwnHeightAslMeters(double stn_pathAnalysisOwnHeightAslMeters) {
		this.stn_pathAnalysisOwnHeightAslMeters = isValidOwnHeightAslMeters(stn_pathAnalysisOwnHeightAslMeters)
				? stn_pathAnalysisOwnHeightAslMeters
				: Double.NaN;
	}

	/**
	 * @return whether the value is a usable own ground elevation (finite and in range)
	 */
	public static boolean isValidOwnHeightAslMeters(double meters) {
		return Double.isFinite(meters)
				&& meters >= PATH_ANALYSIS_OWN_HEIGHT_ASL_MIN_METERS
				&& meters <= PATH_ANALYSIS_OWN_HEIGHT_ASL_MAX_METERS;
	}

	public boolean isStn_pathAnalysisDxHighestPointInSquare() {
		return stn_pathAnalysisDxHighestPointInSquare;
	}

	public void setStn_pathAnalysisDxHighestPointInSquare(boolean stn_pathAnalysisDxHighestPointInSquare) {
		this.stn_pathAnalysisDxHighestPointInSquare = stn_pathAnalysisDxHighestPointInSquare;
	}

	public double getStn_pathAnalysisDefaultTargetAntennaHeightMeters() {
		return stn_pathAnalysisDefaultTargetAntennaHeightMeters;
	}

	public void setStn_pathAnalysisDefaultTargetAntennaHeightMeters(double stn_pathAnalysisDefaultTargetAntennaHeightMeters) {
		this.stn_pathAnalysisDefaultTargetAntennaHeightMeters = Math.max(0.0, stn_pathAnalysisDefaultTargetAntennaHeightMeters);
	}

	public String getStn_pathAnalysisDemRootDirectory() {
		return stn_pathAnalysisDemRootDirectory;
	}

	public void setStn_pathAnalysisDemRootDirectory(String stn_pathAnalysisDemRootDirectory) {
		this.stn_pathAnalysisDemRootDirectory = stn_pathAnalysisDemRootDirectory == null
				? ""
				: stn_pathAnalysisDemRootDirectory.trim();
	}

	public String getStn_pathAnalysisDemDatasetId() {
		return stn_pathAnalysisDemDatasetId;
	}

	public void setStn_pathAnalysisDemDatasetId(String stn_pathAnalysisDemDatasetId) {
		this.stn_pathAnalysisDemDatasetId = (stn_pathAnalysisDemDatasetId == null || stn_pathAnalysisDemDatasetId.isBlank())
				? "copernicus_glo_30"
				: stn_pathAnalysisDemDatasetId.trim().toLowerCase();
	}

	public String getStn_loginNameSecondCat() {
		return stn_loginNameSecondCat;
	}

	public void setStn_loginNameSecondCat(String stn_loginNameSecondCat) {
		this.stn_loginNameSecondCat = stn_loginNameSecondCat;
	}

	public int getStn_on4kstServersPort() {
		return stn_on4kstServersPort;
	}

	public void setStn_on4kstServersPort(int stn_on4kstServersPort) {
		this.stn_on4kstServersPort = stn_on4kstServersPort;
	}

	public String getStn_on4kstServersDns() {
		return stn_on4kstServersDns;
	}

	public void setStn_on4kstServersDns(String stn_on4kstServersDns) {
		this.stn_on4kstServersDns = stn_on4kstServersDns;
	}

	public SimpleRoster<String>
	getLstNotify_QSOSniffer_sniffedCallSignList() {
		return lstNotify_QSOSniffer_sniffedCallSignList;
	}

	public SimpleRoster<String> getLstNotify_QSOSniffer_sniffedPrefixLocList() {
		return lstNotify_QSOSniffer_sniffedPrefixLocList;
	}

	public void setLstNotify_QSOSniffer_sniffedPrefixLocList(java.util.Collection<String> lstNotify_QSOSniffer_sniffedPrefixLocList) {
		// Fill the existing roster: replacing the field would orphan its listeners.
		this.lstNotify_QSOSniffer_sniffedPrefixLocList.setAll(lstNotify_QSOSniffer_sniffedPrefixLocList);
	}

	public String getLogsynch_wintestNetworkStationNameOfKST() {
		return logsynch_wintestNetworkStationNameOfKST;
	}

	public void setLogsynch_wintestNetworkStationNameOfKST(String logsynch_wintestNetworkStationNameOfKST) {
		this.logsynch_wintestNetworkStationNameOfKST = logsynch_wintestNetworkStationNameOfKST;
	}

	public String getLogsynch_wintestNetworkStationNameOfWintestClient1() {
		return logsynch_wintestNetworkStationNameOfWintestClient1;
	}

	public void setLogsynch_wintestNetworkStationNameOfWintestClient1(String logsynch_wintestNetworkStationNameOfWintestClient1) {
		this.logsynch_wintestNetworkStationNameOfWintestClient1 = logsynch_wintestNetworkStationNameOfWintestClient1;
	}

	public String getLogsynch_wintestNetworkStationNameOfWintestClient2() {
		return logsynch_wintestNetworkStationNameOfWintestClient2;
	}

	public void setLogsynch_wintestNetworkStationNameOfWintestClient2(String name) {
		this.logsynch_wintestNetworkStationNameOfWintestClient2 = name == null ? "" : name.trim();
	}

	public boolean isLogsynch_wintestNetworkSimulationEnabled() {
		return logsynch_wintestNetworkSimulationEnabled;
	}

	public void setLogsynch_wintestNetworkSimulationEnabled(boolean logsynch_wintestNetworkSimulationEnabled) {
		this.logsynch_wintestNetworkSimulationEnabled = logsynch_wintestNetworkSimulationEnabled;
	}

	public int getLogsynch_wintestNetworkStationIDOfKST() {
		return logsynch_wintestNetworkStationIDOfKST;
	}

	public void setLogsynch_wintestNetworkStationIDOfKST(int logsynch_wintestNetworkStationIDOfKST) {
		this.logsynch_wintestNetworkStationIDOfKST = logsynch_wintestNetworkStationIDOfKST;
	}

	public int getLogsynch_wintestNetworkPort() {
		return logsynch_wintestNetworkPort;
	}

	public void setLogsynch_wintestNetworkPort(int logsynch_wintestNetworkPort) {
		this.logsynch_wintestNetworkPort = logsynch_wintestNetworkPort;
	}

	public boolean isLogsynch_wintestNetworkListenerEnabled() {
		return logsynch_wintestNetworkListenerEnabled;
	}

	public void setLogsynch_wintestNetworkListenerEnabled(boolean logsynch_wintestNetworkListenerEnabled) {
		this.logsynch_wintestNetworkListenerEnabled = logsynch_wintestNetworkListenerEnabled;
	}

	public String getLogsynch_wintestNetworkBroadcastAddress() {
		return logsynch_wintestNetworkBroadcastAddress;
	}

	public void setLogsynch_wintestNetworkBroadcastAddress(String logsynch_wintestNetworkBroadcastAddress) {
		this.logsynch_wintestNetworkBroadcastAddress = logsynch_wintestNetworkBroadcastAddress;
	}

	public boolean isLogsynch_wintestNetworkSkedPushEnabled() {
		return logsynch_wintestNetworkSkedPushEnabled;
	}

	public void setLogsynch_wintestNetworkSkedPushEnabled(boolean logsynch_wintestNetworkSkedPushEnabled) {
		this.logsynch_wintestNetworkSkedPushEnabled = logsynch_wintestNetworkSkedPushEnabled;
	}

	public String getLogsynch_wintestSkedMode() {
		return logsynch_wintestSkedMode;
	}

	public void setLogsynch_wintestSkedMode(String logsynch_wintestSkedMode) {
		this.logsynch_wintestSkedMode = logsynch_wintestSkedMode;
	}

	public boolean isLogsynch_wintestQrgSyncEnabled() {
		return logsynch_wintestQrgSyncEnabled;
	}

	public void setLogsynch_wintestQrgSyncEnabled(boolean logsynch_wintestQrgSyncEnabled) {
		this.logsynch_wintestQrgSyncEnabled = logsynch_wintestQrgSyncEnabled;
	}

	public boolean isLogsynch_wintestUsePassQrg() {
		return logsynch_wintestUsePassQrg;
	}

	public void setLogsynch_wintestUsePassQrg(boolean logsynch_wintestUsePassQrg) {
		this.logsynch_wintestUsePassQrg = logsynch_wintestUsePassQrg;
	}

	public boolean isBcn_beaconsEnabledSecondCat() {
		return bcn_beaconsEnabledSecondCat;
	}

	public void setBcn_beaconsEnabledSecondCat(boolean bcn_beaconsEnabledSecondCat) {
		this.bcn_beaconsEnabledSecondCat = bcn_beaconsEnabledSecondCat;
	}

	public int getBcn_beaconIntervalInMinutesSecondCat() {
		return bcn_beaconIntervalInMinutesSecondCat;
	}

	public void setBcn_beaconIntervalInMinutesSecondCat(int bcn_beaconIntervalInMinutesSecondCat) {
		this.bcn_beaconIntervalInMinutesSecondCat = bcn_beaconIntervalInMinutesSecondCat;
	}

	public String getBcn_beaconTextSecondCat() {
		return bcn_beaconTextSecondCat;
	}

	public void setBcn_beaconTextSecondCat(String bcn_beaconTextSecondCat) {
		this.bcn_beaconTextSecondCat = bcn_beaconTextSecondCat;
	}

	public ChatCategory getLoginChatCategorySecond() {
		return loginChatCategorySecond;
	}

	public void setLoginChatCategorySecond(ChatCategory loginChatCategorySecond) {
		this.loginChatCategorySecond = loginChatCategorySecond;
	}

	public boolean isLoginToSecondChatEnabled() {
		return loginToSecondChatEnabled;
	}

	public void setLoginToSecondChatEnabled(boolean loginToSecondChatEnabled) {
		this.loginToSecondChatEnabled = loginToSecondChatEnabled;
	}

	public double[] getGUIstationMapStageSceneSizeHW() {
		return GUIstationMapStageSceneSizeHW;
	}

	public void setGUIstationMapStageSceneSizeHW(double[] GUIstationMapStageSceneSizeHW) {
		this.GUIstationMapStageSceneSizeHW = GUIstationMapStageSceneSizeHW;
	}

	public double[] getGUIstationMapStagePositionXY() {
		return GUIstationMapStagePositionXY;
	}

	public void setGUIstationMapStagePositionXY(double[] GUIstationMapStagePositionXY) {
		this.GUIstationMapStagePositionXY = GUIstationMapStagePositionXY;
	}

	public boolean isGUIstationMapPathAnalysisVisible() {
		return GUIstationMapPathAnalysisVisible;
	}

	public void setGUIstationMapPathAnalysisVisible(boolean GUIstationMapPathAnalysisVisible) {
		this.GUIstationMapPathAnalysisVisible = GUIstationMapPathAnalysisVisible;
	}

	public boolean isGUIstationMapClusteringEnabled() {
		return GUIstationMapClusteringEnabled;
	}

	public void setGUIstationMapClusteringEnabled(boolean GUIstationMapClusteringEnabled) {
		this.GUIstationMapClusteringEnabled = GUIstationMapClusteringEnabled;
	}

	public boolean isGUIstationMapAlwaysOnTop() {
		return GUIstationMapAlwaysOnTop;
	}

	public void setGUIstationMapAlwaysOnTop(boolean GUIstationMapAlwaysOnTop) {
		this.GUIstationMapAlwaysOnTop = GUIstationMapAlwaysOnTop;
	}

	/**
	 * Returns a stored width for one stable table/leaf-column identity.
	 *
	 * @param tableId stable table layout identifier
	 * @param columnId stable leaf-column identifier
	 * @return stored pixel width, or empty when no usable value exists
	 */
	public synchronized OptionalDouble getTableColumnWidth(String tableId, String columnId) {
		if (!isValidTableColumnIdentity(tableId, columnId)) {
			return OptionalDouble.empty();
		}
		Double width = tableColumnWidths.get(tableColumnWidthKey(tableId, columnId));
		return isValidTableColumnWidth(width) ? OptionalDouble.of(width) : OptionalDouble.empty();
	}

	/**
	 * Updates one table leaf-column width in memory. Persistence is coordinated by
	 * the layout autosave layer.
	 */
	public synchronized void setTableColumnWidth(String tableId, String columnId, double width) {
		if (isValidTableColumnIdentity(tableId, columnId) && isValidTableColumnWidth(width)) {
			tableColumnWidths.put(tableColumnWidthKey(tableId, columnId), width);
		}
	}

	public boolean isGuiOptions_defaultFilterNothing() {
		return guiOptions_defaultFilterNothing;
	}

	public void setGuiOptions_defaultFilterNothing(boolean guiOptions_defaultFilterNothing) {
		this.guiOptions_defaultFilterNothing = guiOptions_defaultFilterNothing;
	}

	public boolean isGuiOptions_defaultFilterPmToMe() {
		return guiOptions_defaultFilterPmToMe;
	}

	public void setGuiOptions_defaultFilterPmToMe(boolean guiOptions_defaultFilterPmToMe) {
		this.guiOptions_defaultFilterPmToMe = guiOptions_defaultFilterPmToMe;
	}

	public boolean isGuiOptions_showGrossFieldWorkedHintInBandColumns() {
		return guiOptions_showGrossFieldWorkedHintInBandColumns;
	}

	public void setGuiOptions_showGrossFieldWorkedHintInBandColumns(boolean guiOptions_showGrossFieldWorkedHintInBandColumns) {
		this.guiOptions_showGrossFieldWorkedHintInBandColumns = guiOptions_showGrossFieldWorkedHintInBandColumns;
	}

	public boolean isGuiOptions_showFreshCallHintInBandColumns() {
		return guiOptions_showFreshCallHintInBandColumns;
	}

	public void setGuiOptions_showFreshCallHintInBandColumns(boolean guiOptions_showFreshCallHintInBandColumns) {
		this.guiOptions_showFreshCallHintInBandColumns = guiOptions_showFreshCallHintInBandColumns;
	}

	/**
	 * Returns the colour overrides of the daylight design.
	 *
	 * @return the encoded overrides, empty when the operator has changed nothing
	 */
	public String getGuiOptions_paletteOverridesDay() {
		return guiOptions_paletteOverridesDay;
	}

	/**
	 * Sets the colour overrides of the daylight design.
	 *
	 * @param guiOptions_paletteOverridesDay the encoded overrides; null becomes empty
	 */
	public void setGuiOptions_paletteOverridesDay(String guiOptions_paletteOverridesDay) {
		this.guiOptions_paletteOverridesDay =
				guiOptions_paletteOverridesDay == null ? "" : guiOptions_paletteOverridesDay;
	}

	/**
	 * Returns the colour overrides of the evening design.
	 *
	 * @return the encoded overrides, empty when the operator has changed nothing
	 */
	public String getGuiOptions_paletteOverridesEvening() {
		return guiOptions_paletteOverridesEvening;
	}

	/**
	 * Sets the colour overrides of the evening design.
	 *
	 * @param guiOptions_paletteOverridesEvening the encoded overrides; null becomes empty
	 */
	public void setGuiOptions_paletteOverridesEvening(String guiOptions_paletteOverridesEvening) {
		this.guiOptions_paletteOverridesEvening =
				guiOptions_paletteOverridesEvening == null ? "" : guiOptions_paletteOverridesEvening;
	}

	/**
	 * Returns the operator's interface language.
	 *
	 * @return a language code, or empty for the system language
	 */
	public String getGuiOptions_language() {
		return guiOptions_language;
	}

	/**
	 * Sets the operator's interface language.
	 *
	 * @param guiOptions_language a language code, or empty for the system language; null
	 *        becomes empty
	 */
	public void setGuiOptions_language(String guiOptions_language) {
		this.guiOptions_language = guiOptions_language == null ? "" : guiOptions_language;
	}

	public boolean isGuiOptions_defaultFilterPmToOther() {
		return guiOptions_defaultFilterPmToOther;
	}

	public void setGuiOptions_defaultFilterPmToOther(boolean guiOptions_defaultFilterPmToOther) {
		this.guiOptions_defaultFilterPmToOther = guiOptions_defaultFilterPmToOther;
	}

	public boolean isGuiOptions_defaultFilterPublicMsgs() {
		return guiOptions_defaultFilterPublicMsgs;
	}

	public void setGuiOptions_defaultFilterPublicMsgs(boolean guiOptions_defaultFilterPublicMsgs) {
		this.guiOptions_defaultFilterPublicMsgs = guiOptions_defaultFilterPublicMsgs;
	}

	public boolean isMessageHandling_autoAnswerToQRGRequestEnabled() {
		return messageHandling_autoAnswerToQRGRequestEnabled;
	}

	public void setMessageHandling_autoAnswerToQRGRequestEnabled(boolean messageHandling_autoAnswerToQRGRequestEnabled) {
		this.messageHandling_autoAnswerToQRGRequestEnabled = messageHandling_autoAnswerToQRGRequestEnabled;
	}

	public String getMessageHandling_autoAnswerTextMainCat() {
		return messageHandling_autoAnswerTextMainCat;
	}

	public void setMessageHandling_autoAnswerTextMainCat(String messageHandling_autoAnswerTextMainCat) {
		this.messageHandling_autoAnswerTextMainCat = messageHandling_autoAnswerTextMainCat;
	}

	public boolean isMsgHandling_autoAnswerEnabled() {
		return messageHandling_autoAnswerEnabled;
	}

	public void setMessageHandling_autoAnswerEnabled(boolean messageHandling_autoAnswerEnabled) {
		this.messageHandling_autoAnswerEnabled = messageHandling_autoAnswerEnabled;
	}

	public long getBcn_contestScoreSum() {
		return bcn_contestScoreSum;
	}

	public void setBcn_contestScoreSum(long bcn_contestScoreSum) {
		this.bcn_contestScoreSum = bcn_contestScoreSum;
	}

	public void setNotify_DXCSrv_SpottersCallSign(String notify_DXCSrv_SpottersCallSign) {
		this.notify_DXCSrv_SpottersCallSign.set(notify_DXCSrv_SpottersCallSign);
	}

	public boolean isNotify_DXClusterServerTriggerBearing() {
		return notify_DXClusterServerTriggerBearing;
	}

	public void setNotify_DXClusterServerTriggerBearing(boolean notify_DXClusterServerTriggerBearing) {
		this.notify_DXClusterServerTriggerBearing = notify_DXClusterServerTriggerBearing;
	}

	public boolean isNotify_DXClusterServerTriggerOnQRGDetect() {
		return notify_DXClusterServerTriggerOnQRGDetect;
	}

	public void setNotify_DXClusterServerTriggerOnQRGDetect(boolean notify_DXClusterServerTriggerOnQRGDetect) {
		this.notify_DXClusterServerTriggerOnQRGDetect = notify_DXClusterServerTriggerOnQRGDetect;
	}

	public boolean isNotify_dxClusterServerEnabled() {
		return notify_dxClusterServerEnabled;
	}

	public void setNotify_dxClusterServerEnabled(boolean notify_dxClusterServerEnabled) {
		this.notify_dxClusterServerEnabled = notify_dxClusterServerEnabled;
	}


	public int getNotify_noReplyPenaltyMinutes() {
		return notify_noReplyPenaltyMinutes;
	}

	public void setNotify_noReplyPenaltyMinutes(int notify_noReplyPenaltyMinutes) {
		this.notify_noReplyPenaltyMinutes = notify_noReplyPenaltyMinutes;
	}

	public int getNotify_momentumWindowSeconds() {
		return notify_momentumWindowSeconds;
	}

	public void setNotify_momentumWindowSeconds(int notify_momentumWindowSeconds) {
		this.notify_momentumWindowSeconds = notify_momentumWindowSeconds;
	}

	public String getNotify_positiveSignalsPatterns() {
		return notify_positiveSignalsPatterns;
	}

	public void setNotify_positiveSignalsPatterns(String notify_positiveSignalsPatterns) {
		this.notify_positiveSignalsPatterns = notify_positiveSignalsPatterns;
	}


	public boolean isNotify_bandUpgradePriorityBoostEnabled() {
		return notify_bandUpgradePriorityBoostEnabled;
	}

	public void setNotify_bandUpgradePriorityBoostEnabled(boolean notify_bandUpgradePriorityBoostEnabled) {
		this.notify_bandUpgradePriorityBoostEnabled = notify_bandUpgradePriorityBoostEnabled;
	}

	public boolean isNotify_bandUpgradeHintOnLogEnabled() {
		return notify_bandUpgradeHintOnLogEnabled;
	}

	public void setNotify_bandUpgradeHintOnLogEnabled(boolean notify_bandUpgradeHintOnLogEnabled) {
		this.notify_bandUpgradeHintOnLogEnabled = notify_bandUpgradeHintOnLogEnabled;
	}

	public boolean isStn_pstRotatorEnabled() {
		return stn_pstRotatorEnabled;
	}

	public void setStn_pstRotatorEnabled(boolean stn_pstRotatorEnabled) {
		this.stn_pstRotatorEnabled = stn_pstRotatorEnabled;
	}

	public boolean isStn_useQtfFilterDirectionAsAntennaHeading() {
		return !stn_qtfFilterDirectionAsAntennaHeadingOptedOut;
	}

	public void setStn_useQtfFilterDirectionAsAntennaHeading(
			boolean stn_useQtfFilterDirectionAsAntennaHeading) {
		this.stn_qtfFilterDirectionAsAntennaHeadingOptedOut =
				!stn_useQtfFilterDirectionAsAntennaHeading;
	}

	public boolean isQtfFilterDirectionAsAntennaHeadingEnabled() {
		return !stn_pstRotatorEnabled && !stn_qtfFilterDirectionAsAntennaHeadingOptedOut;
	}

	public String getStn_pstRotatorHost() {
		return stn_pstRotatorHost;
	}

	public void setStn_pstRotatorHost(String stn_pstRotatorHost) {
		if (stn_pstRotatorHost == null || stn_pstRotatorHost.isBlank()) {
			this.stn_pstRotatorHost = "127.0.0.1";
		} else {
			this.stn_pstRotatorHost = stn_pstRotatorHost.trim();
		}
	}

	public int getStn_pstRotatorPort() {
		return stn_pstRotatorPort;
	}

	public void setStn_pstRotatorPort(int stn_pstRotatorPort) {
		/*
		 * PSTRotator sends position reports to the configured UDP port + 1.
		 * Port 65535 would therefore require the invalid local port 65536.
		 */
		if (stn_pstRotatorPort < 1 || stn_pstRotatorPort > 65534) {
			this.stn_pstRotatorPort = 12000;
		} else {
			this.stn_pstRotatorPort = stn_pstRotatorPort;
		}
	}

	public boolean isStn_pstRotator2Enabled() {
		return stn_pstRotator2Enabled;
	}

	public void setStn_pstRotator2Enabled(boolean enabled) {
		this.stn_pstRotator2Enabled = enabled;
	}

	public String getStn_pstRotator2Host() {
		return stn_pstRotator2Host;
	}

	public void setStn_pstRotator2Host(String host) {
		this.stn_pstRotator2Host = host == null || host.isBlank() ? "127.0.0.1" : host.trim();
	}

	public int getStn_pstRotator2Port() {
		return stn_pstRotator2Port;
	}

	/** PSTRotator answers on port + 1, so 65535 is not usable. */
	public void setStn_pstRotator2Port(int port) {
		this.stn_pstRotator2Port = port < 1 || port > 65534 ? 12010 : port;
	}

	public MutableValue<String> getNotify_optionalFrequencyPrefix() {
		return notify_optionalFrequencyPrefix;
	}

	public int getNotify_dxclusterServerPort() {
		return notify_dxclusterServerPort;
	}

	public void setNotify_dxclusterServerPort(
			int notify_dxclusterServerPort
	) {
		if (notify_dxclusterServerPort < 1
				|| notify_dxclusterServerPort > 65535) {
			this.notify_dxclusterServerPort = 8000;
		} else {
			this.notify_dxclusterServerPort =
					notify_dxclusterServerPort;
		}
	}

	public double[] getGUIscn_ChatwindowMainSceneSizeHW() {
		return GUIscn_ChatwindowMainSceneSizeHW;
	}

	public void setGUIscn_ChatwindowMainSceneSizeHW(double[] GUIscn_ChatwindowMainSceneSizeHW) {
		this.GUIscn_ChatwindowMainSceneSizeHW = GUIscn_ChatwindowMainSceneSizeHW;
	}

	public double[] getGUIclusterAndQSOMonStage_SceneSizeHW() {
		return GUIclusterAndQSOMonStage_SceneSizeHW;
	}

	public void setGUIclusterAndQSOMonStage_SceneSizeHW(double[] GUIclusterAndQSOMonStage_SceneSizeHW) {
		this.GUIclusterAndQSOMonStage_SceneSizeHW = GUIclusterAndQSOMonStage_SceneSizeHW;
	}

	public double[] getGUIstage_updateStage_SceneSizeHW() {
		return GUIstage_updateStage_SceneSizeHW;
	}

	public void setGUIstage_updateStage_SceneSizeHW(double[] GUIstage_updateStage_SceneSizeHW) {
		this.GUIstage_updateStage_SceneSizeHW = GUIstage_updateStage_SceneSizeHW;
	}

	public double[] getGUIsettingsStageSceneSizeHW() {
		return GUIsettingsStageSceneSizeHW;
	}

	public void setGUIsettingsStageSceneSizeHW(double[] GUIsettingsStageSceneSizeHW) {
		this.GUIsettingsStageSceneSizeHW = GUIsettingsStageSceneSizeHW;
	}

	public double[] getGUIselectedCallSignSplitPane_dividerposition() {
		return GUIselectedCallSignSplitPane_dividerposition;
	}

	public void setGUIselectedCallSignSplitPane_dividerposition(double[] GUIselectedCallSignSplitPane_dividerposition) {
		this.GUIselectedCallSignSplitPane_dividerposition = GUIselectedCallSignSplitPane_dividerposition;
	}

	public double[] getGUImainWindowLeftSplitPane_dividerposition() {
		return GUImainWindowLeftSplitPane_dividerposition;
	}

	public void setGUImainWindowLeftSplitPane_dividerposition(double[] GUImainWindowLeftSplitPane_dividerposition) {
		this.GUImainWindowLeftSplitPane_dividerposition = GUImainWindowLeftSplitPane_dividerposition;
	}

	public double[] getGUImessageSectionSplitpane_dividerposition() {
		return GUImessageSectionSplitpane_dividerposition;
	}

	public double[] getGUImainWindowRightSplitPane_dividerposition() {
		return GUImainWindowRightSplitPane_dividerposition;
	}

	public void setGUImessageSectionSplitpane_dividerposition(double[] GUImessageSectionSplitpane_dividerposition) {
		this.GUImessageSectionSplitpane_dividerposition = GUImessageSectionSplitpane_dividerposition;
	}



	public void setGUImainWindowRightSplitPane_dividerposition(double[] positions) {
		this.GUImainWindowRightSplitPane_dividerposition = positions;
	}

//	public void setGUImainWindowRightSplitPane_dividerposition(double[] GUImainWindowRightSplitPane_dividerposition) {
//		this.GUImainWindowRightSplitPane_dividerposition = GUImainWindowRightSplitPane_dividerposition;
//	}

	public double[] getGUIpnl_directedMSGWin_dividerpositionDefault() {
		return GUIpnl_directedMSGWin_dividerpositionDefault;
	}

	public void setGUIpnl_directedMSGWin_dividerpositionDefault(double[] GUIpnl_directedMSGWin_dividerpositionDefault) {
		this.GUIpnl_directedMSGWin_dividerpositionDefault = GUIpnl_directedMSGWin_dividerpositionDefault;
	}

	public double getStn_antennaBeamWidthDeg() {
		return stn_antennaBeamWidthDeg;
	}

	public void setStn_antennaBeamWidthDeg(double stn_antennaBeamWidthDeg) {
		this.stn_antennaBeamWidthDeg = stn_antennaBeamWidthDeg;
	}

	public double getStn_maxQRBDefault() {
		return stn_maxQRBDefault;
	}

	public void setStn_maxQRBDefault(double stn_maxQRBDefault) {
		this.stn_maxQRBDefault = stn_maxQRBDefault;
	}

	public double getStn_qtfDefault() {
		return stn_qtfDefault;
	}

	public void setStn_qtfDefault(double stn_qtfDefault) {
		if (Double.isFinite(stn_qtfDefault) && stn_qtfDefault >= 0.0 && stn_qtfDefault <= 360.0) {
			this.stn_qtfDefault = stn_qtfDefault;
		}
	}

	public boolean isStn_loginAFKState() {
		return stn_loginAFKState;
	}

	public void setStn_loginAFKState(boolean stn_loginAFKState) {
		this.stn_loginAFKState = stn_loginAFKState;
	}

	public String getStn_loginCallSign() {
		return stn_loginCallSign;
	}

	public String getStn_loginCallSignRaw() {
		return stn_loginCallSignRaw;
	}


	/**
	 * Normalizes an AirScout routing identifier.
	 *
	 * AirScout encloses the identifiers in quotation marks. Empty identifiers,
	 * quotation marks and line breaks would therefore produce an invalid protocol
	 * message and are replaced with the supplied default value.
	 *
	 * @param identifier configured identifier
	 * @param defaultIdentifier fallback value
	 * @return normalized identifier
	 */
	private String normalizeAirScoutIdentifier(
			String identifier,
			String defaultIdentifier
	) {
		if (identifier == null) {
			return defaultIdentifier;
		}

		String normalizedIdentifier = identifier.trim();

		if (normalizedIdentifier.isEmpty()
				|| normalizedIdentifier.contains("\"")
				|| normalizedIdentifier.contains("\r")
				|| normalizedIdentifier.contains("\n")) {
			return defaultIdentifier;
		}

		return normalizedIdentifier;
	}

	public String getAirScout_asBandString() {
		return AirScout_asBandString;
	}

	public boolean isAirScout_autoBandSelectionEnabled() {
		return AirScout_autoBandSelectionEnabled;
	}

	public void setAirScout_autoBandSelectionEnabled(
			boolean airScoutAutoBandSelectionEnabled
	) {
		AirScout_autoBandSelectionEnabled = airScoutAutoBandSelectionEnabled;
	}

	public void setAirScout_asBandString(String airScout_asBandString) {
		if (airScout_asBandString == null) {
			AirScout_asBandString = "1440000";
			return;
		}

		try {
			long parsedBandValue = Long.parseLong(
					airScout_asBandString.trim()
			);

			AirScout_asBandString = parsedBandValue > 0
					? Long.toString(parsedBandValue)
					: "1440000";
		} catch (NumberFormatException exception) {
			AirScout_asBandString = "1440000";
		}
	}

	public String getAirScout_asServerNameString() {
		return AirScout_asServerNameString;
	}

	public void setAirScout_asServerNameString(
			String airScout_asServerNameString
	) {
		AirScout_asServerNameString = normalizeAirScoutIdentifier(
				airScout_asServerNameString,
				"AS"
		);
	}

	public String getAirScout_asClientNameString() {
		return AirScout_asClientNameString;
	}

	public void setAirScout_asClientNameString(
			String airScout_asClientNameString
	) {
		AirScout_asClientNameString = normalizeAirScoutIdentifier(
				airScout_asClientNameString,
				"KST"
		);
	}

	public int getAirScout_asCommunicationPort() {
		return AirScout_asCommunicationPort;
	}

	public void setAirScout_asCommunicationPort(
			int airScout_asCommunicationPort
	) {
		if (airScout_asCommunicationPort < 1
				|| airScout_asCommunicationPort > 65535) {
			AirScout_asCommunicationPort = 9872;
			return;
		}

		AirScout_asCommunicationPort = airScout_asCommunicationPort;
	}

	public boolean isAirScout_asUDPListenerEnabled() {
		return AirScout_asUDPListenerEnabled;
	}

	public void setAirScout_asUDPListenerEnabled(
			boolean airScout_asUDPListenerEnabled
	) {
		AirScout_asUDPListenerEnabled = airScout_asUDPListenerEnabled;
	}


	public String getChatState() {
		return chatState;
	}

	public void setChatState(String chatState) {
		this.chatState = chatState;
	}

	public String getProgramVersion() {
		return programVersion;
	}

	public void setProgramVersion(String programVersion) {
		this.programVersion = programVersion;
	}

	public String getLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup() {
		return logsynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public void setLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup(
			String logsynch_storeWorkedCallSignsFileNameUDPMessageBackup) {
		this.logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = logsynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public boolean isNotify_playSimpleSounds() {
		return notify_playSimpleSounds;
	}

	public boolean isNotify_playCWCallsignsOnRxedPMs() {
		return notify_playCWCallsignsOnRxedPMs;
	}

	public boolean isNotify_playVoiceCallsignsOnRxedPMs() {
		return notify_playVoiceCallsignsOnRxedPMs;
	}

	public void setNotify_playSimpleSounds(boolean notify_playSimpleSounds) {
		this.notify_playSimpleSounds = notify_playSimpleSounds;
	}

	public void setNotify_playCWCallsignsOnRxedPMs(boolean notify_playCWCallsignsOnRxedPMs) {
		this.notify_playCWCallsignsOnRxedPMs = notify_playCWCallsignsOnRxedPMs;
	}

	public void setNotify_playVoiceCallsignsOnRxedPMs(boolean notify_playVoiceCallsignsOnRxedPMs) {
		this.notify_playVoiceCallsignsOnRxedPMs = notify_playVoiceCallsignsOnRxedPMs;
	}

	/**
	 * actualQTF, int, QTF in degrees
	 *
	 */

	public MutableValue<String> getMYQRGFirstCat() {
		return MYQRGFirstCat;
	}

	public MutableValue<Double> getActualQTF() {
		return actualQTF;
	}

	/** Heading reported by rotator 2; NaN while there is none. Display only. */
	public MutableValue<Double> getActualQTF2() {
		return actualQTF2;
	}

	public void setActualQTF(MutableValue<Double> actualQTF) {
		this.actualQTF = actualQTF;
	}

	public void setStn_loginCallSign(String stn_loginCallSign) {

		this.stn_loginCallSign = stn_loginCallSign;
		this.stn_loginCallSignRaw = stn_loginCallSign;

		if (stn_loginCallSign.contains("-")) {

			this.stn_loginCallSignRaw = stn_loginCallSign.split("-")[0];

		} else if (stn_loginCallSign.contains("/")) {

			this.stn_loginCallSignRaw = stn_loginCallSign.split("/")[0];

		}

		if ((stn_loginCallSign.split("-").length > 2 ) || stn_loginCallSign.split("/").length > 2) {
			System.out.println("ChatPreferences, WARNING! Logincallsign is not plausible"); //strange login like do5-amf-2
		}

	}

	public String getStn_loginPassword() {
		return stn_loginPassword;
	}

	public void setStn_loginPassword(String stn_loginPassword) {
		this.stn_loginPassword = stn_loginPassword;
	}

	public String getStn_loginNameMainCat() {
		return stn_loginNameMainCat;
	}

	public void setStn_loginNameMainCat(String stn_loginNameMainCat) {
		this.stn_loginNameMainCat = stn_loginNameMainCat;
	}

	public String getStn_loginLocatorMainCat() {
		return stn_loginLocatorMainCat;
	}

	public void setStn_loginLocatorMainCat(String stn_loginLocatorMainCat) {
		this.stn_loginLocatorMainCat = stn_loginLocatorMainCat;
	}

	public ChatCategory getLoginChatCategoryMain() {
		return loginChatCategoryMain;
	}

	public void setLoginChatCategoryMain(ChatCategory loginChatCategoryMain) {
		this.loginChatCategoryMain = loginChatCategoryMain;
	}

	public String getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly() {
		return logsynch_fileBasedWkdCallInterpreterFileNameReadOnly;
	}

	public void setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(
			String logsynch_fileBasedWkdCallInterpreterFileNameReadOnly) {
		this.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = logsynch_fileBasedWkdCallInterpreterFileNameReadOnly;
	}

	public boolean isLogsynch_fileBasedWkdCallInterpreterEnabled() {
		return logsynch_fileBasedWkdCallInterpreterEnabled;
	}

	public void setLogsynch_fileBasedWkdCallInterpreterEnabled(boolean logsynch_fileBasedWkdCallInterpreterEnabled) {
		this.logsynch_fileBasedWkdCallInterpreterEnabled = logsynch_fileBasedWkdCallInterpreterEnabled;
	}

	public int getLogsynch_ucxUDPWkdCallListenerPort() {
		return logsynch_ucxUDPWkdCallListenerPort;
	}

	public void setLogsynch_ucxUDPWkdCallListenerPort(int logsynch_ucxUDPWkdCallListenerPort) {
		this.logsynch_ucxUDPWkdCallListenerPort = logsynch_ucxUDPWkdCallListenerPort;
	}

	public boolean isLogsynch_ucxUDPWkdCallListenerEnabled() {
		return logsynch_ucxUDPWkdCallListenerEnabled;
	}

	public void setLogsynch_ucxUDPWkdCallListenerEnabled(boolean logsynch_ucxUDPWkdCallListenerEnabled) {
		this.logsynch_ucxUDPWkdCallListenerEnabled = logsynch_ucxUDPWkdCallListenerEnabled;
	}

	public boolean isTrxSynch_ucxLogUDPListenerEnabled() {
		return trxSynch_ucxLogUDPListenerEnabled;
	}

	public void setTrxSynch_ucxLogUDPListenerEnabled(boolean trxSynch_ucxLogUDPListenerEnabled) {
		this.trxSynch_ucxLogUDPListenerEnabled = trxSynch_ucxLogUDPListenerEnabled;
	}

	public boolean isTrxSynch_so2rEnabled() {
		return trxSynch_so2rEnabled;
	}

	public void setTrxSynch_so2rEnabled(boolean enabled) {
		this.trxSynch_so2rEnabled = enabled;
	}

	public void setNotify_optionalFrequencyPrefix(String notify_optionalFrequencyPrefix) {
		this.notify_optionalFrequencyPrefix.set(notify_optionalFrequencyPrefix);
	}

	public MutableValue<String> getNotify_DXCSrv_SpottersCallSign() {
		return notify_DXCSrv_SpottersCallSign;
	}

	//	public String[] getShortcuts() {
//		return shortcuts;
//	}
//
//	public void setShortcuts(String[] shortcuts) {
//		this.shortcuts = shortcuts;
//	}

//	public String[] getTextSnippets() {
//		return textSnippets;
//	}
//
//	public void setTextSnippets(String[] textSnippets) {
//		this.textSnippets = textSnippets;
//	}

	public boolean isBcn_beaconsEnabledMainCat() {
		return bcn_beaconsEnabledMainCat;
	}

	public SimpleRoster<String> getLst_txtShortCutBtnList() {
		return lst_txtShortCutBtnList;
	}

	public void setLst_txtShortCutBtnList(java.util.Collection<String> lst_txtShortCutBtnList) {
		// Fill the existing roster: replacing the field would orphan its listeners.
		this.lst_txtShortCutBtnList.setAll(lst_txtShortCutBtnList);
	}

	public SimpleRoster<String> getLst_txtSnipList() {
		return lst_txtSnipList;
	}

	public void setLst_txtSnipList(java.util.Collection<String> lst_txtSnipList) {
		// Fill the existing roster: replacing the field would orphan its listeners.
		this.lst_txtSnipList.setAll(lst_txtSnipList);
	}

	public void setBcn_beaconsEnabledMainCat(boolean bcn_beaconsEnabledMainCat) {
		this.bcn_beaconsEnabledMainCat = bcn_beaconsEnabledMainCat;
	}

	public int getBcn_beaconIntervalInMinutesMainCat() {
		return bcn_beaconIntervalInMinutesMainCat;
	}

	public void setBcn_beaconIntervalInMinutesMainCat(int bcn_beaconIntervalInMinutesMainCat) {
		this.bcn_beaconIntervalInMinutesMainCat = bcn_beaconIntervalInMinutesMainCat;
	}

	public String getBcn_beaconTextMainCat() {
		return bcn_beaconTextMainCat;
	}

	public void setBcn_beaconTextMainCat(String bcn_beaconTextMainCat) {

		this.bcn_beaconTextMainCat = bcn_beaconTextMainCat;
	}



	public String messageHandling_beaconUnworkedstationsPrefix() {
		return messageHandling_beaconUnworkedstationsPrefix;
	}

	public void setMessageHandling_beaconUnworkedstationsPrefix(String messageHandling_beaconUnworkedstationsPrefix) {
		this.messageHandling_beaconUnworkedstationsPrefix = messageHandling_beaconUnworkedstationsPrefix;
	}

	public boolean isMessageHandling_unworkedStnRequesterBeaconsEnabled() {
		return messageHandling_unworkedStnRequesterBeaconsEnabled;
	}

	public void setMessageHandling_unworkedStnRequesterBeaconsEnabled(boolean messageHandling_unworkedStnRequesterBeaconsEnabled) {
		this.messageHandling_unworkedStnRequesterBeaconsEnabled = messageHandling_unworkedStnRequesterBeaconsEnabled;
	}

	public int getMessageHandling_unworkedStnRequesterBeaconsInterval() {
		return messageHandling_unworkedStnRequesterBeaconsInterval;
	}

	public void setMessageHandling_unworkedStnRequesterBeaconsInterval(int messageHandling_unworkedStnRequesterBeaconsInterval) {
		this.messageHandling_unworkedStnRequesterBeaconsInterval = messageHandling_unworkedStnRequesterBeaconsInterval;
	}

	public String getMessageHandling_unworkedStnRequesterBeaconsText() {
		return messageHandling_unworkedStnRequesterBeaconsText;
	}

	public void setMessageHandling_unworkedStnRequesterBeaconsText(String messageHandling_unworkedStnRequesterBeaconsText) {
		this.messageHandling_unworkedStnRequesterBeaconsText = messageHandling_unworkedStnRequesterBeaconsText;
	}

	public String getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup() {
		return logsynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public void setLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup(
			String logSynch_storeWorkedCallSignsFileNameUDPMessageBackup) {
		this.logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = logSynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public String getStoreAndRestorePreferencesFileName() {
		return storeAndRestorePreferencesFileName;
	}

	public void setStoreAndRestorePreferencesFileName(String storeAndRestorePreferencesFileName) {
		this.storeAndRestorePreferencesFileName = storeAndRestorePreferencesFileName;
	}



	/**
	 *
	 * @return true if the file writing was successful, else false
	 */
	public synchronized boolean writePreferencesToXmlFile() {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {



			// root elements
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("praktiKST");
			doc.appendChild(rootElement);

			// Schema version
			Element configVersion = doc.createElement("configVersion");
			configVersion.setTextContent(String.valueOf(CONFIG_VERSION));
			rootElement.appendChild(configVersion);


			Element station = doc.createElement("station");
			rootElement.appendChild(station);

			Element LoginCallSign = doc.createElement("LoginCallSign");
			LoginCallSign.setTextContent(this.getStn_loginCallSign());
			station.appendChild(LoginCallSign);

			// New preferred tag names (closer to variable names). Keep legacy tags above for old versions.
			Element stn_loginCallSign = doc.createElement("stn_loginCallSign");
			stn_loginCallSign.setTextContent(this.getStn_loginCallSign());
			station.appendChild(stn_loginCallSign);


			Element LoginPassword = doc.createElement("LoginPassword");
			LoginPassword.setTextContent(this.getStn_loginPassword());
			station.appendChild(LoginPassword);

			Element stn_loginPassword = doc.createElement("stn_loginPassword");
			stn_loginPassword.setTextContent(this.getStn_loginPassword());
			station.appendChild(stn_loginPassword);

			Element LoginDisplayedName = doc.createElement("LoginDisplayedName");
			LoginDisplayedName.setTextContent(this.getStn_loginNameMainCat());
			station.appendChild(LoginDisplayedName);

			Element stn_loginNameMainCat = doc.createElement("stn_loginNameMainCat");
			stn_loginNameMainCat.setTextContent(this.getStn_loginNameMainCat());
			station.appendChild(stn_loginNameMainCat);

			Element stn_loginNameSecondCat = doc.createElement("stn_loginNameSecondCat");
			stn_loginNameSecondCat.setTextContent(this.getStn_loginNameSecondCat());
			station.appendChild(stn_loginNameSecondCat);

			Element LoginLocator = doc.createElement("LoginLocator");
			LoginLocator.setTextContent(this.getStn_loginLocatorMainCat());
			station.appendChild(LoginLocator);

			Element stn_loginLocatorMainCat = doc.createElement("stn_loginLocatorMainCat");
			stn_loginLocatorMainCat.setTextContent(this.getStn_loginLocatorMainCat());
			station.appendChild(stn_loginLocatorMainCat);

			Element ChatCategory = doc.createElement("ChatCategory");
			ChatCategory.setTextContent(this.getLoginChatCategoryMain().getCategoryNumber()+"");
			station.appendChild(ChatCategory);

			Element ChatCategorySecond = doc.createElement("ChatCategorySecond");
			ChatCategorySecond.setTextContent(this.getLoginChatCategorySecond().getCategoryNumber()+"");
			station.appendChild(ChatCategorySecond);

			Element stn_secondCatEnabled = doc.createElement("stn_secondCatEnabled");
			stn_secondCatEnabled.setTextContent(this.loginToSecondChatEnabled+"");
			station.appendChild(stn_secondCatEnabled);

			Element stn_antennaBeamWidthDeg = doc.createElement("stn_antennaBeamWidthDeg");
			stn_antennaBeamWidthDeg.setTextContent(this.stn_antennaBeamWidthDeg+"");
			station.appendChild(stn_antennaBeamWidthDeg);

			Element stn_maxQRBDefault = doc.createElement("stn_maxQRBDefault");
			stn_maxQRBDefault.setTextContent(this.stn_maxQRBDefault+"");
			station.appendChild(stn_maxQRBDefault);

			Element stn_qtfDefault = doc.createElement("stn_qtfDefault");
			stn_qtfDefault.setTextContent(this.stn_qtfDefault+"");
			station.appendChild(stn_qtfDefault);

			Element stn_qtfFilterDirectionAsAntennaHeadingOptedOut =
					doc.createElement("stn_qtfFilterDirectionAsAntennaHeadingOptedOut");
			stn_qtfFilterDirectionAsAntennaHeadingOptedOut.setTextContent(
					Boolean.toString(this.stn_qtfFilterDirectionAsAntennaHeadingOptedOut));
			station.appendChild(stn_qtfFilterDirectionAsAntennaHeadingOptedOut);

			Element stn_pathAnalysisOwnAntennaHeightMeters = doc.createElement("stn_pathAnalysisOwnAntennaHeightMeters");
			stn_pathAnalysisOwnAntennaHeightMeters.setTextContent(this.stn_pathAnalysisOwnAntennaHeightMeters + "");
			station.appendChild(stn_pathAnalysisOwnAntennaHeightMeters);

			Element stn_pathAnalysisDefaultTargetAntennaHeightMeters = doc.createElement("stn_pathAnalysisDefaultTargetAntennaHeightMeters");
			stn_pathAnalysisDefaultTargetAntennaHeightMeters.setTextContent(this.stn_pathAnalysisDefaultTargetAntennaHeightMeters + "");
			station.appendChild(stn_pathAnalysisDefaultTargetAntennaHeightMeters);

			// Empty when unset: reading an empty element keeps the NaN default.
			Element stn_pathAnalysisOwnHeightAslMeters = doc.createElement("stn_pathAnalysisOwnHeightAslMeters");
			stn_pathAnalysisOwnHeightAslMeters.setTextContent(Double.isFinite(this.stn_pathAnalysisOwnHeightAslMeters)
					? this.stn_pathAnalysisOwnHeightAslMeters + ""
					: "");
			station.appendChild(stn_pathAnalysisOwnHeightAslMeters);

			Element stn_pathAnalysisDxHighestPointInSquare = doc.createElement("stn_pathAnalysisDxHighestPointInSquare");
			stn_pathAnalysisDxHighestPointInSquare.setTextContent(this.stn_pathAnalysisDxHighestPointInSquare + "");
			station.appendChild(stn_pathAnalysisDxHighestPointInSquare);

			Element stn_pathAnalysisDemRootDirectory = doc.createElement("stn_pathAnalysisDemRootDirectory");
			stn_pathAnalysisDemRootDirectory.setTextContent(this.stn_pathAnalysisDemRootDirectory);
			station.appendChild(stn_pathAnalysisDemRootDirectory);

			Element stn_pathAnalysisDemDatasetId = doc.createElement("stn_pathAnalysisDemDatasetId");
			stn_pathAnalysisDemDatasetId.setTextContent(this.stn_pathAnalysisDemDatasetId);
			station.appendChild(stn_pathAnalysisDemDatasetId);
			Element stn_pathAnalysisOwnTxPowerWatts = doc.createElement("stn_pathAnalysisOwnTxPowerWatts");
			stn_pathAnalysisOwnTxPowerWatts.setTextContent(this.stn_pathAnalysisOwnTxPowerWatts + "");
			station.appendChild(stn_pathAnalysisOwnTxPowerWatts);

			Element stn_pathAnalysisOwnAntennaGainDbi = doc.createElement("stn_pathAnalysisOwnAntennaGainDbi");
			stn_pathAnalysisOwnAntennaGainDbi.setTextContent(this.stn_pathAnalysisOwnAntennaGainDbi + "");
			station.appendChild(stn_pathAnalysisOwnAntennaGainDbi);

			Element stn_pathAnalysisDefaultTargetTxPowerWatts = doc.createElement("stn_pathAnalysisDefaultTargetTxPowerWatts");
			stn_pathAnalysisDefaultTargetTxPowerWatts.setTextContent(this.stn_pathAnalysisDefaultTargetTxPowerWatts + "");
			station.appendChild(stn_pathAnalysisDefaultTargetTxPowerWatts);

			Element stn_pathAnalysisDefaultTargetAntennaGainDbi = doc.createElement("stn_pathAnalysisDefaultTargetAntennaGainDbi");
			stn_pathAnalysisDefaultTargetAntennaGainDbi.setTextContent(this.stn_pathAnalysisDefaultTargetAntennaGainDbi + "");
			station.appendChild(stn_pathAnalysisDefaultTargetAntennaGainDbi);

			Element stn_pathAnalysisVhfFeederLossPerStationDb = doc.createElement("stn_pathAnalysisVhfFeederLossPerStationDb");
			stn_pathAnalysisVhfFeederLossPerStationDb.setTextContent(this.stn_pathAnalysisVhfFeederLossPerStationDb + "");
			station.appendChild(stn_pathAnalysisVhfFeederLossPerStationDb);

			Element stn_pathAnalysisFeederLossIncreaseDbPer200MHz = doc.createElement("stn_pathAnalysisFeederLossIncreaseDbPer200MHz");
			stn_pathAnalysisFeederLossIncreaseDbPer200MHz.setTextContent(this.stn_pathAnalysisFeederLossIncreaseDbPer200MHz + "");
			station.appendChild(stn_pathAnalysisFeederLossIncreaseDbPer200MHz);

			Element stn_pathAnalysisMaxEstimatedFeederLossPerStationDb = doc.createElement("stn_pathAnalysisMaxEstimatedFeederLossPerStationDb");
			stn_pathAnalysisMaxEstimatedFeederLossPerStationDb.setTextContent(this.stn_pathAnalysisMaxEstimatedFeederLossPerStationDb + "");
			station.appendChild(stn_pathAnalysisMaxEstimatedFeederLossPerStationDb);

			Element stn_pathAnalysisRequiredSsbSignalDbm = doc.createElement("stn_pathAnalysisRequiredSsbSignalDbm");
			stn_pathAnalysisRequiredSsbSignalDbm.setTextContent(this.stn_pathAnalysisRequiredSsbSignalDbm + "");
			station.appendChild(stn_pathAnalysisRequiredSsbSignalDbm);

			Element stn_pathAnalysisRequiredCwSignalDbm = doc.createElement("stn_pathAnalysisRequiredCwSignalDbm");
			stn_pathAnalysisRequiredCwSignalDbm.setTextContent(this.stn_pathAnalysisRequiredCwSignalDbm + "");
			station.appendChild(stn_pathAnalysisRequiredCwSignalDbm);

			Element stn_pathAnalysisContestMarginDb = doc.createElement("stn_pathAnalysisContestMarginDb");
			stn_pathAnalysisContestMarginDb.setTextContent(this.stn_pathAnalysisContestMarginDb + "");
			station.appendChild(stn_pathAnalysisContestMarginDb);



			Element stn_bandActive50 = doc.createElement("stn_bandActive50");
			stn_bandActive50.setTextContent(this.stn_bandActive50+"");
			station.appendChild(stn_bandActive50);

			Element stn_bandActive70 = doc.createElement("stn_bandActive70");
			stn_bandActive70.setTextContent(this.stn_bandActive70+"");
			station.appendChild(stn_bandActive70);

			Element stn_bandActive144 = doc.createElement("stn_bandActive144");
			stn_bandActive144.setTextContent(this.stn_bandActive144+"");
			station.appendChild(stn_bandActive144);

			Element stn_bandActive432 = doc.createElement("stn_bandActive432");
			stn_bandActive432.setTextContent(this.stn_bandActive432+"");
			station.appendChild(stn_bandActive432);

			Element stn_bandActive1240 = doc.createElement("stn_bandActive1240");
			stn_bandActive1240.setTextContent(this.stn_bandActive1240+"");
			station.appendChild(stn_bandActive1240);

			Element stn_bandActive2300 = doc.createElement("stn_bandActive2300");
			stn_bandActive2300.setTextContent(this.stn_bandActive2300+"");
			station.appendChild(stn_bandActive2300);

			Element stn_bandActive3400 = doc.createElement("stn_bandActive3400");
			stn_bandActive3400.setTextContent(this.stn_bandActive3400+"");
			station.appendChild(stn_bandActive3400);

			Element stn_bandActive5600 = doc.createElement("stn_bandActive5600");
			stn_bandActive5600.setTextContent(this.stn_bandActive5600+"");
			station.appendChild(stn_bandActive5600);

			Element stn_bandActive10G = doc.createElement("stn_bandActive10G");
			stn_bandActive10G.setTextContent(this.stn_bandActive10G+"");
			station.appendChild(stn_bandActive10G);

			Element stn_on4kstServersDns = doc.createElement("stn_on4kstServersDns");
			stn_on4kstServersDns.setTextContent(this.stn_on4kstServersDns);
			station.appendChild(stn_on4kstServersDns);

			Element stn_on4kstServersPort = doc.createElement("stn_on4kstServersPort");
			stn_on4kstServersPort.setTextContent(this.stn_on4kstServersPort + "");
			station.appendChild(stn_on4kstServersPort);

			Element stn_loginAFKState = doc.createElement("stn_loginAFKState");
			stn_loginAFKState.setTextContent(this.stn_loginAFKState + "");
			station.appendChild(stn_loginAFKState);

			Element stn_pstRotatorEnabled = doc.createElement("stn_pstRotatorEnabled");
			stn_pstRotatorEnabled.setTextContent(this.stn_pstRotatorEnabled + "");
			station.appendChild(stn_pstRotatorEnabled);

			Element stn_pstRotatorHost = doc.createElement("stn_pstRotatorHost");
			stn_pstRotatorHost.setTextContent(this.stn_pstRotatorHost);
			station.appendChild(stn_pstRotatorHost);

			Element stn_pstRotatorPort = doc.createElement("stn_pstRotatorPort");
			stn_pstRotatorPort.setTextContent(Integer.toString(this.stn_pstRotatorPort));
			station.appendChild(stn_pstRotatorPort);

			Element stn_pstRotator2Enabled = doc.createElement("stn_pstRotator2Enabled");
			stn_pstRotator2Enabled.setTextContent(this.stn_pstRotator2Enabled + "");
			station.appendChild(stn_pstRotator2Enabled);

			Element stn_pstRotator2Host = doc.createElement("stn_pstRotator2Host");
			stn_pstRotator2Host.setTextContent(this.stn_pstRotator2Host);
			station.appendChild(stn_pstRotator2Host);

			Element stn_pstRotator2Port = doc.createElement("stn_pstRotator2Port");
			stn_pstRotator2Port.setTextContent(Integer.toString(this.stn_pstRotator2Port));
			station.appendChild(stn_pstRotator2Port);


			/**
			 * LOGSYNCH
			 */

			Element logsynch = doc.createElement("logsynch");
			rootElement.appendChild(logsynch);

			Element logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = doc.createElement("logsynch_fileBasedWkdCallInterpreterFileNameReadOnly");
			logsynch_fileBasedWkdCallInterpreterFileNameReadOnly.setTextContent(this.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly());
			logsynch.appendChild(logsynch_fileBasedWkdCallInterpreterFileNameReadOnly);

			Element logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = doc.createElement("logsynch_storeWorkedCallSignsFileNameUDPMessageBackup");
			logsynch_storeWorkedCallSignsFileNameUDPMessageBackup.setTextContent(this.getLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup());
			logsynch.appendChild(logsynch_storeWorkedCallSignsFileNameUDPMessageBackup);

			Element logsynch_fileBasedWkdCallInterpreterEnabled = doc.createElement("logsynch_fileBasedWkdCallInterpreterEnabled");
			logsynch_fileBasedWkdCallInterpreterEnabled.setTextContent(this.isLogsynch_fileBasedWkdCallInterpreterEnabled()+"");
			logsynch.appendChild(logsynch_fileBasedWkdCallInterpreterEnabled);

			Element logsynch_ucxUDPWkdCallListenerPort = doc.createElement("logsynch_ucxUDPWkdCallListenerPort");
			logsynch_ucxUDPWkdCallListenerPort.setTextContent(this.getLogsynch_ucxUDPWkdCallListenerPort()+"");
			logsynch.appendChild(logsynch_ucxUDPWkdCallListenerPort);

			Element logsynch_ucxUDPWkdCallListenerEnabled = doc.createElement("logsynch_ucxUDPWkdCallListenerEnabled");
			// BUGFIX: this is the log-synch listener flag, not the TRX-synch flag (copy&paste error in older versions)
			logsynch_ucxUDPWkdCallListenerEnabled.setTextContent(this.isLogsynch_ucxUDPWkdCallListenerEnabled()+"");
			logsynch.appendChild(logsynch_ucxUDPWkdCallListenerEnabled);

			// WinTest Settings
			Element logsynch_wintestNetworkStationNameOfKST = doc.createElement("logsynch_wintestNetworkStationNameOfKST");
			logsynch_wintestNetworkStationNameOfKST.setTextContent(this.logsynch_wintestNetworkStationNameOfKST);
			logsynch.appendChild(logsynch_wintestNetworkStationNameOfKST);

			Element logsynch_wintestNetworkStationNameOfWintestClient1 = doc.createElement("logsynch_wintestNetworkStationNameOfWintestClient1");
			logsynch_wintestNetworkStationNameOfWintestClient1.setTextContent(this.logsynch_wintestNetworkStationNameOfWintestClient1);
			logsynch.appendChild(logsynch_wintestNetworkStationNameOfWintestClient1);

			Element logsynch_wintestNetworkStationNameOfWintestClient2 = doc.createElement("logsynch_wintestNetworkStationNameOfWintestClient2");
			logsynch_wintestNetworkStationNameOfWintestClient2.setTextContent(this.logsynch_wintestNetworkStationNameOfWintestClient2);
			logsynch.appendChild(logsynch_wintestNetworkStationNameOfWintestClient2);

			Element logsynch_wintestNetworkSimulationEnabled = doc.createElement("logsynch_wintestNetworkSimulationEnabled");
			logsynch_wintestNetworkSimulationEnabled.setTextContent(this.logsynch_wintestNetworkSimulationEnabled + "");
			logsynch.appendChild(logsynch_wintestNetworkSimulationEnabled);

			Element logsynch_wintestNetworkStationIDOfKST = doc.createElement("logsynch_wintestNetworkStationIDOfKST");
			logsynch_wintestNetworkStationIDOfKST.setTextContent(this.logsynch_wintestNetworkStationIDOfKST + "");
			logsynch.appendChild(logsynch_wintestNetworkStationIDOfKST);

			Element logsynch_wintestNetworkPort = doc.createElement("logsynch_wintestNetworkPort");
			logsynch_wintestNetworkPort.setTextContent(this.logsynch_wintestNetworkPort + "");
			logsynch.appendChild(logsynch_wintestNetworkPort);

			Element logsynch_wintestNetworkListenerEnabled = doc.createElement("logsynch_wintestNetworkListenerEnabled");
			logsynch_wintestNetworkListenerEnabled.setTextContent(this.logsynch_wintestNetworkListenerEnabled + "");
			logsynch.appendChild(logsynch_wintestNetworkListenerEnabled);

			Element logsynch_wintestNetworkBroadcastAddress = doc.createElement("logsynch_wintestNetworkBroadcastAddress");
			logsynch_wintestNetworkBroadcastAddress.setTextContent(this.logsynch_wintestNetworkBroadcastAddress);
			logsynch.appendChild(logsynch_wintestNetworkBroadcastAddress);

			Element logsynch_wintestNetworkSkedPushEnabled = doc.createElement("logsynch_wintestNetworkSkedPushEnabled");
			logsynch_wintestNetworkSkedPushEnabled.setTextContent(this.logsynch_wintestNetworkSkedPushEnabled + "");
			logsynch.appendChild(logsynch_wintestNetworkSkedPushEnabled);

			Element logsynch_wintestSkedMode = doc.createElement("logsynch_wintestSkedMode");
			logsynch_wintestSkedMode.setTextContent(this.logsynch_wintestSkedMode);
			logsynch.appendChild(logsynch_wintestSkedMode);

			Element logsynch_wintestQrgSyncEnabled = doc.createElement("logsynch_wintestQrgSyncEnabled");
			logsynch_wintestQrgSyncEnabled.setTextContent(this.logsynch_wintestQrgSyncEnabled + "");
			logsynch.appendChild(logsynch_wintestQrgSyncEnabled);

			Element logsynch_wintestUsePassQrg = doc.createElement("logsynch_wintestUsePassQrg");
			logsynch_wintestUsePassQrg.setTextContent(this.logsynch_wintestUsePassQrg + "");
			logsynch.appendChild(logsynch_wintestUsePassQrg);


			/**
			 * trxSynchUCX
			 */

			Element trxSynchUCX = doc.createElement("trxSynchUCX");
			rootElement.appendChild(trxSynchUCX);

			Element trxSynch_ucxLogUDPListenerEnabled = doc.createElement("trxSynch_ucxLogUDPListenerEnabled");
			trxSynch_ucxLogUDPListenerEnabled.setTextContent(this.isTrxSynch_ucxLogUDPListenerEnabled()+"");
			trxSynchUCX.appendChild(trxSynch_ucxLogUDPListenerEnabled);

			Element trxSynch_so2rEnabled = doc.createElement("trxSynch_so2rEnabled");
			trxSynch_so2rEnabled.setTextContent(this.trxSynch_so2rEnabled + "");
			trxSynchUCX.appendChild(trxSynch_so2rEnabled);

			Element trxSynch_defaultMYQRGValue = doc.createElement("trxSynch_defaultMYQRGValue");
			trxSynch_defaultMYQRGValue.setTextContent(this.getMYQRGFirstCat().get());
			trxSynchUCX.appendChild(trxSynch_defaultMYQRGValue);

			Element trxSynch_defaultMYQRG2Value = doc.createElement("trxSynch_defaultMYQRG2Value");
			// Safe null check falls Property noch nicht initialisiert ist
			trxSynch_defaultMYQRG2Value.setTextContent(this.getMYQRGSecondCat().get() != null ? this.getMYQRGSecondCat().get() : "1296.200.00");
			trxSynchUCX.appendChild(trxSynch_defaultMYQRG2Value);


			/**
			 * AirScout
			 */

			Element AirScoutQuerier = doc.createElement("AirScoutQuerier");
			rootElement.appendChild(AirScoutQuerier);


			Element asQry_airScoutCommunicationEnabled = doc.createElement("asQry_airScoutCommunicationEnabled");
			asQry_airScoutCommunicationEnabled.setTextContent(this.isAirScout_asUDPListenerEnabled()+"");
			AirScoutQuerier.appendChild(asQry_airScoutCommunicationEnabled);

			Element asQry_airScoutServerName = doc.createElement("asQry_airScoutServerName");
			asQry_airScoutServerName.setTextContent(this.getAirScout_asServerNameString());
			AirScoutQuerier.appendChild(asQry_airScoutServerName);

			Element asQry_airScoutClientName = doc.createElement("asQry_airScoutClientName");
			asQry_airScoutClientName.setTextContent(this.getAirScout_asClientNameString());
			AirScoutQuerier.appendChild(asQry_airScoutClientName);

			Element asQry_airScoutUDPPort = doc.createElement("asQry_airScoutUDPPort");
			asQry_airScoutUDPPort.setTextContent(this.getAirScout_asCommunicationPort()+"");
			AirScoutQuerier.appendChild(asQry_airScoutUDPPort);

			Element asQry_airScoutAutoBandSelectionEnabled =
					doc.createElement("asQry_airScoutAutoBandSelectionEnabled");
			asQry_airScoutAutoBandSelectionEnabled.setTextContent(
					Boolean.toString(this.isAirScout_autoBandSelectionEnabled())
			);
			AirScoutQuerier.appendChild(asQry_airScoutAutoBandSelectionEnabled);

			Element asQry_airScoutBandValue = doc.createElement("asQry_airScoutBandValue");
			asQry_airScoutBandValue.setTextContent(this.getAirScout_asBandString());
			AirScoutQuerier.appendChild(asQry_airScoutBandValue);


			/**
			 * Notifications
			 */

			Element notifications = doc.createElement("notifications");
			rootElement.appendChild(notifications);

			Element notify_SimpleAudioNotificationsEnabled = doc.createElement("notify_SimpleAudioNotificationsEnabled");
			notify_SimpleAudioNotificationsEnabled.setTextContent(this.isNotify_playSimpleSounds()+"");
			notifications.appendChild(notify_SimpleAudioNotificationsEnabled);

			Element notify_CWCallSignAudioNotificationsEnabled = doc.createElement("notify_CWCallsignAudioNotificationsEnabled");
			notify_CWCallSignAudioNotificationsEnabled.setTextContent(this.isNotify_playCWCallsignsOnRxedPMs()+"");
			notifications.appendChild(notify_CWCallSignAudioNotificationsEnabled);

			Element notify_VoiceCallSignAudioNotificationsEnabled = doc.createElement("notify_VoiceCallsignAudioNotificationsEnabled");
			notify_VoiceCallSignAudioNotificationsEnabled.setTextContent(this.isNotify_playVoiceCallsignsOnRxedPMs()+"");
			notifications.appendChild(notify_VoiceCallSignAudioNotificationsEnabled);

			Element notify_dxClusterServerEnabledToFile = doc.createElement("notify_dxClusterServerEnabled");
			notify_dxClusterServerEnabledToFile.setTextContent(this.isNotify_dxClusterServerEnabled() + "");
			notifications.appendChild(notify_dxClusterServerEnabledToFile);

			Element notify_DXClusterServerTriggerBearingToFile = doc.createElement("notify_DXClusterServerTriggerBearing");
			notify_DXClusterServerTriggerBearingToFile.setTextContent(this.isNotify_DXClusterServerTriggerBearing() + "");
			notifications.appendChild(notify_DXClusterServerTriggerBearingToFile);

			Element notify_DXClusterServerTriggerOnQRGDetectToFile = doc.createElement("notify_DXClusterServerTriggerOnQRGDetect");
			notify_DXClusterServerTriggerOnQRGDetectToFile.setTextContent(this.isNotify_DXClusterServerTriggerOnQRGDetect() + "");
			notifications.appendChild(notify_DXClusterServerTriggerOnQRGDetectToFile);

			Element notify_dxclusterServerPortToFile = doc.createElement("notify_dxclusterServerPort");
			notify_dxclusterServerPortToFile.setTextContent(this.getNotify_dxclusterServerPort()+ "");
			notifications.appendChild(notify_dxclusterServerPortToFile);

			Element notify_optionalFrequencyPrefixToFile = doc.createElement("notify_optionalFrequencyPrefix");
			notify_optionalFrequencyPrefixToFile.setTextContent(this.getNotify_optionalFrequencyPrefix().get());
			notifications.appendChild(notify_optionalFrequencyPrefixToFile);

			Element notify_DXCSrv_SpottersCallSignToFile = doc.createElement("notify_DXCSrv_SpottersCallSign");
			notify_DXCSrv_SpottersCallSignToFile.setTextContent(this.getNotify_DXCSrv_SpottersCallSign().get());
			notifications.appendChild(notify_DXCSrv_SpottersCallSignToFile);

			Element notify_noReplyPenaltyMinutes = doc.createElement("notify_noReplyPenaltyMinutes");
			notify_noReplyPenaltyMinutes.setTextContent(this.getNotify_noReplyPenaltyMinutes() + "");
			notifications.appendChild(notify_noReplyPenaltyMinutes);

			Element notify_momentumWindowSeconds = doc.createElement("notify_momentumWindowSeconds");
			notify_momentumWindowSeconds.setTextContent(this.getNotify_momentumWindowSeconds() + "");
			notifications.appendChild(notify_momentumWindowSeconds);

			Element notify_positiveSignalsPatterns = doc.createElement("notify_positiveSignalsPatterns");
			notify_positiveSignalsPatterns.setTextContent(this.getNotify_positiveSignalsPatterns());
			notifications.appendChild(notify_positiveSignalsPatterns);

			Element notify_bandUpgradeHintOnLogEnabled = doc.createElement("notify_bandUpgradeHintOnLogEnabled");
			notify_bandUpgradeHintOnLogEnabled.setTextContent(this.notify_bandUpgradeHintOnLogEnabled + "");
			notifications.appendChild(notify_bandUpgradeHintOnLogEnabled);

			Element notify_bandUpgradePriorityBoostEnabled = doc.createElement("notify_bandUpgradePriorityBoostEnabled");
			notify_bandUpgradePriorityBoostEnabled.setTextContent(this.notify_bandUpgradePriorityBoostEnabled + "");
			notifications.appendChild(notify_bandUpgradePriorityBoostEnabled);


			/**
			 * Shortcuts
			 */

			Element shortCuts = doc.createElement("shortCuts");
			rootElement.appendChild(shortCuts);

			for (Iterator iterator = lst_txtShortCutBtnList.snapshot().iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				Element temp = doc.createElement("t");
				temp.setTextContent(string);
				shortCuts.appendChild(temp);
			}

			/**
			 * QSO Sniffer Lists
			 */
			Element snifferCallSigns =
					doc.createElement("snifferCallSigns");
			rootElement.appendChild(snifferCallSigns);

			for (String callSign
					: lstNotify_QSOSniffer_sniffedCallSignList.snapshot()) {
				Element temp = doc.createElement("callSign");
				temp.setTextContent(callSign);
				snifferCallSigns.appendChild(temp);
			}


			Element snifferPrefixes = doc.createElement("snifferPrefixes");
			rootElement.appendChild(snifferPrefixes);

			for (String prefix : lstNotify_QSOSniffer_sniffedPrefixLocList.snapshot()) {
				Element temp = doc.createElement("p");
				temp.setTextContent(prefix);
				snifferPrefixes.appendChild(temp);
			}

			/**
			 * Textsnippets (right click menu)
			 */

			Element textSnippets = doc.createElement("textSnippets");
			rootElement.appendChild(textSnippets);

			for (Iterator iterator = lst_txtSnipList.snapshot().iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				Element temp = doc.createElement("t");
				temp.setTextContent(string);
				textSnippets.appendChild(temp);
			}


			/**
			 * BeaconCQ
			 */

			Element beaconCQ = doc.createElement("beaconCQ");
			rootElement.appendChild(beaconCQ);

			Element beaconCQText = doc.createElement("beaconCQText");
			beaconCQText.setTextContent(this.getBcn_beaconTextMainCat());
			beaconCQ.appendChild(beaconCQText);

			// Preferred tag name (close to variable name)
			Element bcn_beaconTextMainCat = doc.createElement("bcn_beaconTextMainCat");
			bcn_beaconTextMainCat.setTextContent(this.getBcn_beaconTextMainCat());
			beaconCQ.appendChild(bcn_beaconTextMainCat);

			Element beaconCQIntervalMinutes = doc.createElement("beaconCQIntervalMinutes");
			beaconCQIntervalMinutes.setTextContent(this.getBcn_beaconIntervalInMinutesMainCat()+"");
			beaconCQ.appendChild(beaconCQIntervalMinutes);

			Element bcn_beaconIntervalInMinutesMainCat = doc.createElement("bcn_beaconIntervalInMinutesMainCat");
			bcn_beaconIntervalInMinutesMainCat.setTextContent(this.getBcn_beaconIntervalInMinutesMainCat()+"");
			beaconCQ.appendChild(bcn_beaconIntervalInMinutesMainCat);

			Element beaconCQEnabled = doc.createElement("beaconCQEnabled");
			beaconCQEnabled.setTextContent(this.isBcn_beaconsEnabledMainCat()+"");
			beaconCQ.appendChild(beaconCQEnabled);

			Element bcn_beaconsEnabledMainCat = doc.createElement("bcn_beaconsEnabledMainCat");
			bcn_beaconsEnabledMainCat.setTextContent(this.isBcn_beaconsEnabledMainCat()+"");
			beaconCQ.appendChild(bcn_beaconsEnabledMainCat);


			Element beaconCQTextSecondText = doc.createElement("beaconCQTextSecondText");
			beaconCQTextSecondText.setTextContent(this.getBcn_beaconTextSecondCat());
			beaconCQ.appendChild(beaconCQTextSecondText);

			Element bcn_beaconTextSecondCat = doc.createElement("bcn_beaconTextSecondCat");
			bcn_beaconTextSecondCat.setTextContent(this.getBcn_beaconTextSecondCat());
			beaconCQ.appendChild(bcn_beaconTextSecondCat);

			Element beaconCQIntervalMinutesSecondCat = doc.createElement("beaconCQIntervalMinutesSecondCat");
			beaconCQIntervalMinutesSecondCat.setTextContent(this.getBcn_beaconIntervalInMinutesSecondCat()+"");
			beaconCQ.appendChild(beaconCQIntervalMinutesSecondCat);

			Element bcn_beaconIntervalInMinutesSecondCat = doc.createElement("bcn_beaconIntervalInMinutesSecondCat");
			bcn_beaconIntervalInMinutesSecondCat.setTextContent(this.getBcn_beaconIntervalInMinutesSecondCat()+"");
			beaconCQ.appendChild(bcn_beaconIntervalInMinutesSecondCat);

			Element beaconCQEnabledSecondCat = doc.createElement("beaconCQEnabledSecondCat");
			beaconCQEnabledSecondCat.setTextContent(this.isBcn_beaconsEnabledSecondCat()+"");
			beaconCQ.appendChild(beaconCQEnabledSecondCat);

			Element bcn_beaconsEnabledSecondCat = doc.createElement("bcn_beaconsEnabledSecondCat");
			bcn_beaconsEnabledSecondCat.setTextContent(this.isBcn_beaconsEnabledSecondCat()+"");
			beaconCQ.appendChild(bcn_beaconsEnabledSecondCat);

			/**
			 * Messagehandling section / ex Beacon Unworked Stations
			 */

			Element beaconUnworkedstations = doc.createElement("beaconUnworkedstations");
			rootElement.appendChild(beaconUnworkedstations);

			Element beaconUnworkedstationsText = doc.createElement("beaconUnworkedstationsText");
			beaconUnworkedstationsText.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsText());
			beaconUnworkedstations.appendChild(beaconUnworkedstationsText);

			Element messageHandling_unworkedStnRequesterBeaconsText = doc.createElement("messageHandling_unworkedStnRequesterBeaconsText");
			messageHandling_unworkedStnRequesterBeaconsText.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsText());
			beaconUnworkedstations.appendChild(messageHandling_unworkedStnRequesterBeaconsText);

			Element beaconUnworkedstationsIntervalMinutes = doc.createElement("beaconUnworkedstationsIntervalMinutes");
			beaconUnworkedstationsIntervalMinutes.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsInterval()+"");
			beaconUnworkedstations.appendChild(beaconUnworkedstationsIntervalMinutes);

			Element messageHandling_unworkedStnRequesterBeaconsInterval = doc.createElement("messageHandling_unworkedStnRequesterBeaconsInterval");
			messageHandling_unworkedStnRequesterBeaconsInterval.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsInterval()+"");
			beaconUnworkedstations.appendChild(messageHandling_unworkedStnRequesterBeaconsInterval);

			Element beaconUnworkedstationsEnabled = doc.createElement("beaconUnworkedstationsEnabled");
			beaconUnworkedstationsEnabled.setTextContent(this.isMessageHandling_unworkedStnRequesterBeaconsEnabled()+"");
			beaconUnworkedstations.appendChild(beaconUnworkedstationsEnabled);

			Element messageHandling_unworkedStnRequesterBeaconsEnabled = doc.createElement("messageHandling_unworkedStnRequesterBeaconsEnabled");
			messageHandling_unworkedStnRequesterBeaconsEnabled.setTextContent(this.isMessageHandling_unworkedStnRequesterBeaconsEnabled()+"");
			beaconUnworkedstations.appendChild(messageHandling_unworkedStnRequesterBeaconsEnabled);

			Element beaconUnworkedstationsPrefix = doc.createElement("beaconUnworkedstationsPrefix");
			beaconUnworkedstationsPrefix.setTextContent(this.messageHandling_beaconUnworkedstationsPrefix());
			beaconUnworkedstations.appendChild(beaconUnworkedstationsPrefix);

			Element messageHandling_beaconUnworkedstationsPrefix = doc.createElement("messageHandling_beaconUnworkedstationsPrefix");
			messageHandling_beaconUnworkedstationsPrefix.setTextContent(this.messageHandling_beaconUnworkedstationsPrefix());
			beaconUnworkedstations.appendChild(messageHandling_beaconUnworkedstationsPrefix);

			/*****************************************************************
			 * MESSAGEHANDLING NEW  .... BEACONUNWORKED HAVE TO BE REPLACED
			 ****************************************************************/

			Element messageHandling = doc.createElement("messageHandling");
			rootElement.appendChild(messageHandling);

			Element autoAnswerText = doc.createElement("autoAnswerText");
			autoAnswerText.setTextContent(this.getMessageHandling_autoAnswerTextMainCat());
			messageHandling.appendChild(autoAnswerText);

			Element autoAnswerEnabled = doc.createElement("autoAnswerEnabled");
			autoAnswerEnabled.setTextContent(this.isMsgHandling_autoAnswerEnabled()+"");
			messageHandling.appendChild(autoAnswerEnabled);

			Element autoAnswerTextSecondCat = doc.createElement("autoAnswerTextSecondCat");
			autoAnswerTextSecondCat.setTextContent(this.getMessageHandling_autoAnswerTextSecondCat());
			messageHandling.appendChild(autoAnswerTextSecondCat);

			Element autoAnswerEnabledSecondCat = doc.createElement("autoAnswerEnabledSecondCat");
			autoAnswerEnabledSecondCat.setTextContent(this.isMessageHandling_autoAnswerEnabledSecondCat()+"");
			messageHandling.appendChild(autoAnswerEnabledSecondCat);

			Element autoAnswerToQrgRequestEnabled = doc.createElement("autoAnswerToQrgRequestEnabled");
			autoAnswerToQrgRequestEnabled.setTextContent(isMessageHandling_autoAnswerToQRGRequestEnabled()+"");
			messageHandling.appendChild(autoAnswerToQrgRequestEnabled);

			Element debugModeToFileEnabled = doc.createElement(
					"messageHandling_debugModeToFileEnabled");
			debugModeToFileEnabled.setTextContent(
					Boolean.toString(isMessageHandling_debugModeToFileEnabled()));
			messageHandling.appendChild(debugModeToFileEnabled);

			/****************************
			 * GUI BEHAVIOUR
			 ***************************/

			Element guiSaveableOptions = doc.createElement("guiSaveableOptions");
			rootElement.appendChild(guiSaveableOptions);

			Element guiOptions_defaultFilterNothing = doc.createElement("guiOptions_defaultFilterNothing");
			guiOptions_defaultFilterNothing.setTextContent(this.isGuiOptions_defaultFilterNothing()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterNothing);

			Element guiOptions_defaultFilterPmToMe = doc.createElement("guiOptions_defaultFilterPmToMe");
			guiOptions_defaultFilterPmToMe.setTextContent(this.isGuiOptions_defaultFilterPmToMe()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterPmToMe);

			Element guiOptions_defaultFilterPmToOther = doc.createElement("guiOptions_defaultFilterPmToOther");
			guiOptions_defaultFilterPmToOther.setTextContent(this.isGuiOptions_defaultFilterPmToOther()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterPmToOther);

			Element guiOptions_defaultFilterPublicMsgs = doc.createElement("guiOptions_defaultFilterPublicMsgs");
			guiOptions_defaultFilterPublicMsgs.setTextContent(this.isGuiOptions_defaultFilterPublicMsgs()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterPublicMsgs);

			Element guiOptions_showGrossFieldWorkedHintInBandColumns = doc.createElement("guiOptions_showGrossFieldWorkedHintInBandColumns");
			guiOptions_showGrossFieldWorkedHintInBandColumns.setTextContent(this.isGuiOptions_showGrossFieldWorkedHintInBandColumns()+"");
			guiSaveableOptions.appendChild(guiOptions_showGrossFieldWorkedHintInBandColumns);

			Element guiOptions_showFreshCallHintInBandColumns = doc.createElement("guiOptions_showFreshCallHintInBandColumns");
			guiOptions_showFreshCallHintInBandColumns.setTextContent(this.isGuiOptions_showFreshCallHintInBandColumns()+"");
			guiSaveableOptions.appendChild(guiOptions_showFreshCallHintInBandColumns);

			Element guiOptions_paletteOverridesDay = doc.createElement("guiOptions_paletteOverridesDay");
			guiOptions_paletteOverridesDay.setTextContent(this.getGuiOptions_paletteOverridesDay());
			guiSaveableOptions.appendChild(guiOptions_paletteOverridesDay);

			Element guiOptions_paletteOverridesEvening = doc.createElement("guiOptions_paletteOverridesEvening");
			guiOptions_paletteOverridesEvening.setTextContent(this.getGuiOptions_paletteOverridesEvening());
			guiSaveableOptions.appendChild(guiOptions_paletteOverridesEvening);

			Element guiOptions_language = doc.createElement("guiOptions_language");
			guiOptions_language.setTextContent(this.getGuiOptions_language());
			guiSaveableOptions.appendChild(guiOptions_language);

			Element guiOptions_darkModeActive = doc.createElement("guiOptions_darkModeActive");
			guiOptions_darkModeActive.setTextContent(this.GUI_darkModeActive + "");
			guiSaveableOptions.appendChild(guiOptions_darkModeActive);

			Element guiOptions_darkModeActiveByDefault = doc.createElement("guiOptions_darkModeActiveByDefault");
			guiOptions_darkModeActiveByDefault.setTextContent(this.GUI_darkModeActiveByDefault + "");
			guiSaveableOptions.appendChild(guiOptions_darkModeActiveByDefault);

			Element guiOptions_rememberWindowPositions = doc.createElement("guiOptions_rememberWindowPositions");
			guiOptions_rememberWindowPositions.setTextContent(String.valueOf(this.guiOptions_rememberWindowPositions));
			guiSaveableOptions.appendChild(guiOptions_rememberWindowPositions);

			// --- GUImainWindowRightSplitPane_dividerposition (2 dividers => 2 values) --- NEW
			ensureMainWindowRightSplitPaneDividerPositions(2);

			Element eDiv0 = doc.createElement("GUImainWindowRightSplitPane_dividerposition0");
			eDiv0.setTextContent(String.valueOf(GUImainWindowRightSplitPane_dividerposition[0]));
			guiSaveableOptions.appendChild(eDiv0);

			Element eDiv1 = doc.createElement("GUImainWindowRightSplitPane_dividerposition1");
			eDiv1.setTextContent(String.valueOf(GUImainWindowRightSplitPane_dividerposition[1]));
			guiSaveableOptions.appendChild(eDiv1);

			/**
			 * window sizes
			 */
			Element guiOptions = doc.createElement("guiOptions");
			rootElement.appendChild(guiOptions);

			Element GUIscn_ChatwindowMainSceneSizeHW = doc.createElement("GUIscn_ChatwindowMainSceneSizeHW");
			GUIscn_ChatwindowMainSceneSizeHW.setTextContent(this.getGUIscn_ChatwindowMainSceneSizeHW()[0]+";"+this.getGUIscn_ChatwindowMainSceneSizeHW()[1]);
			guiOptions.appendChild(GUIscn_ChatwindowMainSceneSizeHW);

			Element GUIclusterAndQSOMonStage_SceneSizeHW = doc.createElement("GUIclusterAndQSOMonStage_SceneSizeHW");
			GUIclusterAndQSOMonStage_SceneSizeHW.setTextContent(this.getGUIclusterAndQSOMonStage_SceneSizeHW()[0]+";"+this.getGUIclusterAndQSOMonStage_SceneSizeHW()[1]);
			guiOptions.appendChild(GUIclusterAndQSOMonStage_SceneSizeHW);

			Element GUIstage_updateStage_SceneSizeHW = doc.createElement("GUIstage_updateStage_SceneSizeHW");
			GUIstage_updateStage_SceneSizeHW.setTextContent(this.getGUIstage_updateStage_SceneSizeHW()[0]+";"+this.getGUIstage_updateStage_SceneSizeHW()[1]);
			guiOptions.appendChild(GUIstage_updateStage_SceneSizeHW);

			Element GUIsettingsStageSceneSizeHW = doc.createElement("GUIsettingsStageSceneSizeHW");
			GUIsettingsStageSceneSizeHW.setTextContent(this.getGUIsettingsStageSceneSizeHW()[0]+";"+this.getGUIsettingsStageSceneSizeHW()[1]);
			guiOptions.appendChild(GUIsettingsStageSceneSizeHW);

			/************************************
			 * save splitpanel divider positions
			 ************************************/

			Element GUIselectedCallSignSplitPane_dividerposition = doc.createElement("GUIselectedCallSignSplitPane_dividerposition");
			GUIselectedCallSignSplitPane_dividerposition.setTextContent(doubleArrayToCSVString(getGUIselectedCallSignSplitPane_dividerposition()));
			guiOptions.appendChild(GUIselectedCallSignSplitPane_dividerposition);

			Element GUImainWindowLeftSplitPane_dividerposition = doc.createElement("GUImainWindowLeftSplitPane_dividerposition");
			GUImainWindowLeftSplitPane_dividerposition.setTextContent(doubleArrayToCSVString(getGUImainWindowLeftSplitPane_dividerposition()));
			guiOptions.appendChild(GUImainWindowLeftSplitPane_dividerposition);

			Element GUImessageSectionSplitpane_dividerposition = doc.createElement("GUImessageSectionSplitpane_dividerposition");
			GUImessageSectionSplitpane_dividerposition.setTextContent(doubleArrayToCSVString(getGUImessageSectionSplitpane_dividerposition()));
			guiOptions.appendChild(GUImessageSectionSplitpane_dividerposition);

			Element GUImainWindowRightSplitPane_dividerposition = doc.createElement("GUImainWindowRightSplitPane_dividerposition");
			GUImainWindowRightSplitPane_dividerposition.setTextContent(doubleArrayToCSVString(getGUImainWindowRightSplitPane_dividerposition()));
			guiOptions.appendChild(GUImainWindowRightSplitPane_dividerposition);

			Element GUIpnl_directedMSGWin_dividerpositionDefault = doc.createElement("GUIpnl_directedMSGWin_dividerpositionDefault");
			GUIpnl_directedMSGWin_dividerpositionDefault.setTextContent(doubleArrayToCSVString(getGUIpnl_directedMSGWin_dividerpositionDefault()));
			guiOptions.appendChild(GUIpnl_directedMSGWin_dividerpositionDefault);

			Element GUIstationMapStageSceneSizeHW = doc.createElement("GUIstationMapStageSceneSizeHW");
			GUIstationMapStageSceneSizeHW.setTextContent(
					this.getGUIstationMapStageSceneSizeHW()[0] + ";" + this.getGUIstationMapStageSceneSizeHW()[1]
			);
			guiOptions.appendChild(GUIstationMapStageSceneSizeHW);

			Element GUIstationMapStagePositionXY = doc.createElement("GUIstationMapStagePositionXY");
			GUIstationMapStagePositionXY.setTextContent(
					this.getGUIstationMapStagePositionXY()[0] + ";" + this.getGUIstationMapStagePositionXY()[1]
			);
			guiOptions.appendChild(GUIstationMapStagePositionXY);

			Element GUIstationMapPathAnalysisVisible = doc.createElement("GUIstationMapPathAnalysisVisible");
			GUIstationMapPathAnalysisVisible.setTextContent(
					String.valueOf(this.isGUIstationMapPathAnalysisVisible())
			);
			guiOptions.appendChild(GUIstationMapPathAnalysisVisible);

			Element GUIstationMapClusteringEnabled = doc.createElement("GUIstationMapClusteringEnabled");
			GUIstationMapClusteringEnabled.setTextContent(
					String.valueOf(this.isGUIstationMapClusteringEnabled())
			);
			guiOptions.appendChild(GUIstationMapClusteringEnabled);

			Element GUIstationMapAlwaysOnTop = doc.createElement("GUIstationMapAlwaysOnTop");
			GUIstationMapAlwaysOnTop.setTextContent(
					String.valueOf(this.isGUIstationMapAlwaysOnTop())
			);
			guiOptions.appendChild(GUIstationMapAlwaysOnTop);

			appendTableColumnWidths(doc, guiOptions);
			appendWindowPositions(doc, guiOptions);

			/****************************************************************************************
			 ****************************** now write this XML! *************************************
			 ****************************************************************************************/

			writeDocumentAtomically(doc);


		} catch (ParserConfigurationException | TransformerException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
		return true;

		// root elements

		//...create XML elements, and others...

		// write dom document to a file

	}

	/**
	 * Writes only layout values into the existing preferences document. Functional
	 * settings are deliberately read from disk and left untouched.
	 */
	public synchronized boolean writeLayoutPreferencesToXmlFile() {
		Path preferencesPath = Path.of(storeAndRestorePreferencesFileName).toAbsolutePath();
		if (!Files.isRegularFile(preferencesPath)) {
			System.out.println("[ChatPreferences, Warning]: Cannot autosave layout because preferences.xml does not exist.");
			return false;
		}

		try {
			DocumentBuilder documentBuilder = createSecureDocumentBuilderFactory().newDocumentBuilder();
			Document document = documentBuilder.parse(preferencesPath.toFile());
			Element root = document.getDocumentElement();
			if (root == null) {
				return false;
			}

			upsertDirectChildText(document, root, TAG_CONFIG_VERSION, String.valueOf(CONFIG_VERSION));
			Element guiOptions = getDirectChildElement(root, "guiOptions");
			if (guiOptions == null) {
				guiOptions = document.createElement("guiOptions");
				root.appendChild(guiOptions);
			}

			updateLayoutElements(document, guiOptions);
			removeDirectChildren(guiOptions, TAG_TABLE_COLUMN_WIDTH);
			appendTableColumnWidths(document, guiOptions);
			removeDirectChildren(guiOptions, TAG_WINDOW_POSITION);
			appendWindowPositions(document, guiOptions);
			writeDocumentAtomically(document);
			return true;
		} catch (ParserConfigurationException | SAXException | IOException | TransformerException exception) {
			exception.printStackTrace();
			return false;
		}
	}

	private void updateLayoutElements(Document document, Element guiOptions) {
		upsertDirectChildText(document, guiOptions, "GUIscn_ChatwindowMainSceneSizeHW",
				getGUIscn_ChatwindowMainSceneSizeHW()[0] + ";" + getGUIscn_ChatwindowMainSceneSizeHW()[1]);
		upsertDirectChildText(document, guiOptions, "GUIclusterAndQSOMonStage_SceneSizeHW",
				getGUIclusterAndQSOMonStage_SceneSizeHW()[0] + ";" + getGUIclusterAndQSOMonStage_SceneSizeHW()[1]);
		upsertDirectChildText(document, guiOptions, "GUIstage_updateStage_SceneSizeHW",
				getGUIstage_updateStage_SceneSizeHW()[0] + ";" + getGUIstage_updateStage_SceneSizeHW()[1]);
		upsertDirectChildText(document, guiOptions, "GUIsettingsStageSceneSizeHW",
				getGUIsettingsStageSceneSizeHW()[0] + ";" + getGUIsettingsStageSceneSizeHW()[1]);
		upsertDirectChildText(document, guiOptions, "GUIselectedCallSignSplitPane_dividerposition",
				doubleArrayToCSVString(getGUIselectedCallSignSplitPane_dividerposition()));
		upsertDirectChildText(document, guiOptions, "GUImainWindowLeftSplitPane_dividerposition",
				doubleArrayToCSVString(getGUImainWindowLeftSplitPane_dividerposition()));
		upsertDirectChildText(document, guiOptions, "GUImessageSectionSplitpane_dividerposition",
				doubleArrayToCSVString(getGUImessageSectionSplitpane_dividerposition()));
		upsertDirectChildText(document, guiOptions, "GUImainWindowRightSplitPane_dividerposition",
				doubleArrayToCSVString(getGUImainWindowRightSplitPane_dividerposition()));
		upsertDirectChildText(document, guiOptions, "GUIpnl_directedMSGWin_dividerpositionDefault",
				doubleArrayToCSVString(getGUIpnl_directedMSGWin_dividerpositionDefault()));
		upsertDirectChildText(document, guiOptions, "GUIstationMapStageSceneSizeHW",
				getGUIstationMapStageSceneSizeHW()[0] + ";" + getGUIstationMapStageSceneSizeHW()[1]);
		upsertDirectChildText(document, guiOptions, "GUIstationMapStagePositionXY",
				getGUIstationMapStagePositionXY()[0] + ";" + getGUIstationMapStagePositionXY()[1]);
		upsertDirectChildText(document, guiOptions, "GUIstationMapClusteringEnabled",
				String.valueOf(isGUIstationMapClusteringEnabled()));
		upsertDirectChildText(document, guiOptions, "GUIstationMapAlwaysOnTop",
				String.valueOf(isGUIstationMapAlwaysOnTop()));
	}

	private void appendTableColumnWidths(Document document, Element guiOptions) {
		for (Map.Entry<String, Double> entry : tableColumnWidths.entrySet()) {
			if (!isValidTableColumnWidth(entry.getValue())) {
				continue;
			}

			int separatorIndex = entry.getKey().indexOf('\u0000');
			if (separatorIndex <= 0 || separatorIndex >= entry.getKey().length() - 1) {
				continue;
			}

			Element widthElement = document.createElement(TAG_TABLE_COLUMN_WIDTH);
			widthElement.setAttribute("tableId", entry.getKey().substring(0, separatorIndex));
			widthElement.setAttribute("columnId", entry.getKey().substring(separatorIndex + 1));
			widthElement.setAttribute("pixels", String.valueOf(entry.getValue()));
			guiOptions.appendChild(widthElement);
		}
	}

	private void writeDocumentAtomically(Document document) throws IOException, TransformerException {
		Path target = Path.of(storeAndRestorePreferencesFileName).toAbsolutePath();
		Path parent = target.getParent();
		Path fileName = target.getFileName();
		if (parent == null || fileName == null) {
			throw new IOException("Preferences path has no parent directory: " + target);
		}

		Files.createDirectories(parent);
		Path temporary = Files.createTempFile(parent, fileName.toString(), ".tmp");
		try {
			try (OutputStream output = Files.newOutputStream(temporary)) {
				writeXml(document, output);
			}
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	// write doc to output stream
	private static void writeXml(Document doc, OutputStream output) throws TransformerException {

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(output);

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		transformer.transform(source, result);

	}

	/**
	 *
	 * @return true if the file reading was successful, else false
	 */
	public boolean readPreferencesFromXmlFile() {

		System.out.println("[ChatPreferences, Info]: restoring prefs from file " + storeAndRestorePreferencesFileName);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {

			File xmlConfigFile = new File(storeAndRestorePreferencesFileName);

			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xmlConfigFile);
			NodeList list;

			// --- Schema version (optional) ---
			// Missing in older files -> assume version 1.
			int xmlVersion = getIntFromDoc(doc, CONFIG_VERSION, "configVersion", "ConfigVersion");
			// Currently we only need the version for debugging / future migrations.
			if (xmlVersion > CONFIG_VERSION) {
				System.out.println("[ChatPreferences, Info]: preferences.xml version (" + xmlVersion + ") is newer than this application (" + CONFIG_VERSION + "). Trying best-effort load.");
			}


			/**
			 * case station settings
			 *
			 */
			Element stationEl = getFirstElement(doc, "station");
			if (stationEl != null) {
				// Prefer tag names close to variable names, but accept legacy tags for backwards compatibility.
				stn_loginCallSign = getText(stationEl, stn_loginCallSign, "stn_loginCallSign", "LoginCallSign");
				stn_loginPassword = getText(stationEl, stn_loginPassword, "stn_loginPassword", "LoginPassword");
				stn_loginNameMainCat = getText(stationEl, stn_loginNameMainCat, "stn_loginNameMainCat", "LoginDisplayedName");
				stn_loginNameSecondCat = getText(stationEl, stn_loginNameSecondCat, "stn_loginNameSecondCat");
				stn_loginLocatorMainCat = getText(stationEl, stn_loginLocatorMainCat, "stn_loginLocatorMainCat", "LoginLocator");

				stn_on4kstServersDns = getText(stationEl, stn_on4kstServersDns, "stn_on4kstServersDns");
				stn_on4kstServersPort = getInt(stationEl, stn_on4kstServersPort, "stn_on4kstServersPort");
				stn_loginAFKState = getBoolean(stationEl, stn_loginAFKState, "stn_loginAFKState");

				String category = getText(stationEl, null, "loginChatCategoryMain", "ChatCategory");
				if (isNumeric(category)) {
					loginChatCategoryMain = new ChatCategory(Integer.parseInt(category));
				} else {
					loginChatCategoryMain = new ChatCategory(2);
				}

				String categorySecond = getText(stationEl, null, "loginChatCategorySecond", "ChatCategorySecond");
				if (isNumeric(categorySecond)) {
					loginChatCategorySecond = new ChatCategory(Integer.parseInt(categorySecond));
				} else {
					loginChatCategorySecond = new ChatCategory(3);
				}

				loginToSecondChatEnabled = getBoolean(stationEl, loginToSecondChatEnabled, "stn_secondCatEnabled");

				stn_antennaBeamWidthDeg = getDouble(stationEl, stn_antennaBeamWidthDeg, "stn_antennaBeamWidthDeg");
				stn_maxQRBDefault = getDouble(stationEl, stn_maxQRBDefault, "stn_maxQRBDefault");
				setStn_qtfDefault(getDouble(stationEl, stn_qtfDefault, "stn_qtfDefault"));

				stn_pathAnalysisOwnAntennaHeightMeters = getDouble(
						stationEl,
						stn_pathAnalysisOwnAntennaHeightMeters,
						"stn_pathAnalysisOwnAntennaHeightMeters"
				);

				stn_pathAnalysisDefaultTargetAntennaHeightMeters = getDouble(
						stationEl,
						stn_pathAnalysisDefaultTargetAntennaHeightMeters,
						"stn_pathAnalysisDefaultTargetAntennaHeightMeters"
				);

				setStn_pathAnalysisOwnHeightAslMeters(getDouble(
						stationEl,
						Double.NaN,
						"stn_pathAnalysisOwnHeightAslMeters"
				));

				stn_pathAnalysisDxHighestPointInSquare = getBooleanOrDefault(
						stationEl,
						stn_pathAnalysisDxHighestPointInSquare,
						"stn_pathAnalysisDxHighestPointInSquare"
				);

				stn_pathAnalysisDemRootDirectory = getText(
						stationEl,
						stn_pathAnalysisDemRootDirectory,
						"stn_pathAnalysisDemRootDirectory"
				);

				stn_pathAnalysisDemDatasetId = getText(
						stationEl,
						stn_pathAnalysisDemDatasetId,
						"stn_pathAnalysisDemDatasetId"
				);

				stn_pathAnalysisOwnTxPowerWatts = getDouble(
						stationEl,
						stn_pathAnalysisOwnTxPowerWatts,
						"stn_pathAnalysisOwnTxPowerWatts"
				);

				stn_pathAnalysisOwnAntennaGainDbi = getDouble(
						stationEl,
						stn_pathAnalysisOwnAntennaGainDbi,
						"stn_pathAnalysisOwnAntennaGainDbi"
				);

				stn_pathAnalysisDefaultTargetTxPowerWatts = getDouble(
						stationEl,
						stn_pathAnalysisDefaultTargetTxPowerWatts,
						"stn_pathAnalysisDefaultTargetTxPowerWatts"
				);

				stn_pathAnalysisDefaultTargetAntennaGainDbi = getDouble(
						stationEl,
						stn_pathAnalysisDefaultTargetAntennaGainDbi,
						"stn_pathAnalysisDefaultTargetAntennaGainDbi"
				);

				stn_pathAnalysisVhfFeederLossPerStationDb = getDouble(
						stationEl,
						stn_pathAnalysisVhfFeederLossPerStationDb,
						"stn_pathAnalysisVhfFeederLossPerStationDb"
				);

				stn_pathAnalysisFeederLossIncreaseDbPer200MHz = getDouble(
						stationEl,
						stn_pathAnalysisFeederLossIncreaseDbPer200MHz,
						"stn_pathAnalysisFeederLossIncreaseDbPer200MHz"
				);

				stn_pathAnalysisMaxEstimatedFeederLossPerStationDb = getDouble(
						stationEl,
						stn_pathAnalysisMaxEstimatedFeederLossPerStationDb,
						"stn_pathAnalysisMaxEstimatedFeederLossPerStationDb"
				);

				stn_pathAnalysisRequiredSsbSignalDbm = getDouble(
						stationEl,
						stn_pathAnalysisRequiredSsbSignalDbm,
						"stn_pathAnalysisRequiredSsbSignalDbm"
				);

				stn_pathAnalysisRequiredCwSignalDbm = getDouble(
						stationEl,
						stn_pathAnalysisRequiredCwSignalDbm,
						"stn_pathAnalysisRequiredCwSignalDbm"
				);

				stn_pathAnalysisContestMarginDb = getDouble(
						stationEl,
						stn_pathAnalysisContestMarginDb,
						"stn_pathAnalysisContestMarginDb"
				);

				// Band activity flags (introduced later; if missing -> keep defaults)
				stn_bandActive50 = getBoolean(stationEl, stn_bandActive50, "stn_bandActive50");
				stn_bandActive70 = getBoolean(stationEl, stn_bandActive70, "stn_bandActive70");
				stn_bandActive144 = getBoolean(stationEl, stn_bandActive144, "stn_bandActive144");
				stn_bandActive432 = getBoolean(stationEl, stn_bandActive432, "stn_bandActive432");
				stn_bandActive1240 = getBoolean(stationEl, stn_bandActive1240, "stn_bandActive1240");
				stn_bandActive2300 = getBoolean(stationEl, stn_bandActive2300, "stn_bandActive2300");
				stn_bandActive3400 = getBoolean(stationEl, stn_bandActive3400, "stn_bandActive3400");
				stn_bandActive5600 = getBoolean(stationEl, stn_bandActive5600, "stn_bandActive5600");
				stn_bandActive10G = getBoolean(stationEl, stn_bandActive10G, "stn_bandActive10G");

				stn_pstRotatorEnabled = getBoolean(stationEl, stn_pstRotatorEnabled, "stn_pstRotatorEnabled");
				/*
				 * The positive version-8 setting cannot distinguish an explicit false from its old
				 * default. Absence of the opt-out marker therefore performs the approved migration
				 * to enabled; once written, the marker preserves every later user decision.
				 */
				stn_qtfFilterDirectionAsAntennaHeadingOptedOut = getBoolean(
						stationEl,
						false,
						"stn_qtfFilterDirectionAsAntennaHeadingOptedOut"
				);

				setStn_pstRotatorHost(
						getText(
								stationEl,
								stn_pstRotatorHost,
								"stn_pstRotatorHost"
						)
				);

				setStn_pstRotatorPort(
						getInt(
								stationEl,
								stn_pstRotatorPort,
								"stn_pstRotatorPort"
						)
				);

				// Added in release 1.50 for SO2R; absent in older files.
				stn_pstRotator2Enabled = getBoolean(stationEl, stn_pstRotator2Enabled, "stn_pstRotator2Enabled");
				setStn_pstRotator2Host(getText(stationEl, stn_pstRotator2Host, "stn_pstRotator2Host"));
				setStn_pstRotator2Port(getInt(stationEl, stn_pstRotator2Port, "stn_pstRotator2Port"));

			}

			/**
			 * Case log synchronizatrion
			 */

			Element logsynchEl = getFirstElement(doc, "logsynch");
			if (logsynchEl != null) {
				logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = getText(
						logsynchEl,
						logsynch_fileBasedWkdCallInterpreterFileNameReadOnly,
						"logsynch_fileBasedWkdCallInterpreterFileNameReadOnly");
				logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = getText(
						logsynchEl,
						logsynch_storeWorkedCallSignsFileNameUDPMessageBackup,
						"logsynch_storeWorkedCallSignsFileNameUDPMessageBackup");
				logsynch_fileBasedWkdCallInterpreterEnabled = getBoolean(
						logsynchEl,
						logsynch_fileBasedWkdCallInterpreterEnabled,
						"logsynch_fileBasedWkdCallInterpreterEnabled");
				logsynch_ucxUDPWkdCallListenerPort = getInt(
						logsynchEl,
						logsynch_ucxUDPWkdCallListenerPort,
						"logsynch_ucxUDPWkdCallListenerPort");
				logsynch_ucxUDPWkdCallListenerEnabled = getBoolean(
						logsynchEl,
						logsynch_ucxUDPWkdCallListenerEnabled,
						"logsynch_ucxUDPWkdCallListenerEnabled");

				// Optional Win-Test network settings
				logsynch_wintestNetworkStationNameOfKST = getText(
						logsynchEl,
						logsynch_wintestNetworkStationNameOfKST,
						"logsynch_wintestNetworkStationNameOfKST");
				logsynch_wintestNetworkStationNameOfWintestClient1 = getText(
						logsynchEl,
						logsynch_wintestNetworkStationNameOfWintestClient1,
						"logsynch_wintestNetworkStationNameOfWintestClient1");
				setLogsynch_wintestNetworkStationNameOfWintestClient2(getText(
						logsynchEl,
						logsynch_wintestNetworkStationNameOfWintestClient2,
						"logsynch_wintestNetworkStationNameOfWintestClient2"));
				logsynch_wintestNetworkSimulationEnabled = getBoolean(
						logsynchEl,
						logsynch_wintestNetworkSimulationEnabled,
						"logsynch_wintestNetworkSimulationEnabled");
				logsynch_wintestNetworkStationIDOfKST = getInt(
						logsynchEl,
						logsynch_wintestNetworkStationIDOfKST,
						"logsynch_wintestNetworkStationIDOfKST");
				logsynch_wintestNetworkPort = getInt(
						logsynchEl,
						logsynch_wintestNetworkPort,
						"logsynch_wintestNetworkPort");

				logsynch_wintestNetworkListenerEnabled = getBoolean(
						logsynchEl,
						logsynch_wintestNetworkListenerEnabled,
						"logsynch_wintestNetworkListenerEnabled");

				logsynch_wintestNetworkBroadcastAddress = getText(
						logsynchEl,
						logsynch_wintestNetworkBroadcastAddress,
						"logsynch_wintestNetworkBroadcastAddress");

				logsynch_wintestNetworkSkedPushEnabled = getBoolean(
						logsynchEl,
						logsynch_wintestNetworkSkedPushEnabled,
						"logsynch_wintestNetworkSkedPushEnabled");

				logsynch_wintestSkedMode = getText(
						logsynchEl,
						logsynch_wintestSkedMode,
						"logsynch_wintestSkedMode");

				logsynch_wintestQrgSyncEnabled = getBoolean(
						logsynchEl,
						logsynch_wintestQrgSyncEnabled,
						"logsynch_wintestQrgSyncEnabled");

				logsynch_wintestUsePassQrg = getBoolean(
						logsynchEl,
						logsynch_wintestUsePassQrg,
						"logsynch_wintestUsePassQrg");

				System.out.println(
						"[ChatPreferences, info]: file based worked-call interpreter: " + logsynch_fileBasedWkdCallInterpreterEnabled);
				System.out.println(
						"[ChatPreferences, info]: UCX UDP worked-call listener: " + logsynch_ucxUDPWkdCallListenerEnabled);
			}

			/**
			 * Case trx synchronizatrion
			 */

			Element trxSynchEl = getFirstElement(doc, "trxSynchUCX");
			if (trxSynchEl != null) {
				trxSynch_ucxLogUDPListenerEnabled = getBoolean(
						trxSynchEl,
						trxSynch_ucxLogUDPListenerEnabled,
						"trxSynch_ucxLogUDPListenerEnabled");
				trxSynch_so2rEnabled = getBoolean(trxSynchEl, trxSynch_so2rEnabled, "trxSynch_so2rEnabled");

				String qrg1 = getText(trxSynchEl, null, "trxSynch_defaultMYQRGValue");
				if (qrg1 != null) {
					this.getMYQRGFirstCat().set(qrg1);
				}
				String qrg2 = getText(trxSynchEl, "1296.123.00", "trxSynch_defaultMYQRG2Value");
				this.getMYQRGSecondCat().set(qrg2);

				System.out.println(
						"[ChatPreferences, info]: trx qrg synch=" + trxSynch_ucxLogUDPListenerEnabled
								+ ", default=" + this.getMYQRGFirstCat().get() + " // " + this.getMYQRGSecondCat().get());
			}

			/**
			 * Case notifications
			 */

			Element notificationsEl = getFirstElement(doc, "notifications");
			if (notificationsEl != null) {
				notify_playSimpleSounds = getBoolean(notificationsEl, notify_playSimpleSounds, "notify_SimpleAudioNotificationsEnabled");
				notify_playCWCallsignsOnRxedPMs = getBoolean(notificationsEl, notify_playCWCallsignsOnRxedPMs, "notify_CWCallsignAudioNotificationsEnabled");
				notify_playVoiceCallsignsOnRxedPMs = getBoolean(notificationsEl, notify_playVoiceCallsignsOnRxedPMs, "notify_VoiceCallsignAudioNotificationsEnabled");

				// DXCluster / Monitoring (introduced later) -> keep defaults if absent
				notify_dxClusterServerEnabled = getBoolean(notificationsEl, notify_dxClusterServerEnabled, "notify_dxClusterServerEnabled");
				notify_DXClusterServerTriggerBearing = getBoolean(notificationsEl, notify_DXClusterServerTriggerBearing, "notify_DXClusterServerTriggerBearing");
				notify_DXClusterServerTriggerOnQRGDetect = getBoolean(notificationsEl, notify_DXClusterServerTriggerOnQRGDetect, "notify_DXClusterServerTriggerOnQRGDetect");
				setNotify_dxclusterServerPort(
						getInt(
								notificationsEl,
								notify_dxclusterServerPort,
								"notify_dxclusterServerPort"
						)
				);

				String spotter = getText(notificationsEl, null, "notify_DXCSrv_SpottersCallSign");
				if (spotter != null) {
					notify_DXCSrv_SpottersCallSign.set(spotter);
				}
				String prefix = getText(notificationsEl, null, "notify_optionalFrequencyPrefix");
				if (prefix != null) {
					notify_optionalFrequencyPrefix.set(prefix);
				}

				Integer noReply = getInt(notificationsEl, 13, "notify_noReplyPenaltyMinutes");
				if (noReply != null) {
					notify_noReplyPenaltyMinutes = noReply;
				}

				Integer momentum = getInt(
						notificationsEl,
						notify_momentumWindowSeconds,
						"notify_momentumWindowSeconds"
				);				if (momentum != null) {
					notify_momentumWindowSeconds = momentum;
				}

				String pos = getText(notificationsEl, null, "notify_positiveSignalsPatterns");
				if (pos != null) {
					notify_positiveSignalsPatterns = pos;
				}

				notify_bandUpgradeHintOnLogEnabled = getBoolean(
						notificationsEl,
						notify_bandUpgradeHintOnLogEnabled,
						"notify_bandUpgradeHintOnLogEnabled"
				);
				notify_bandUpgradePriorityBoostEnabled = getBoolean(
						notificationsEl,
						notify_bandUpgradePriorityBoostEnabled,
						"notify_bandUpgradePriorityBoostEnabled"
				);


				System.out.println(
						"[ChatPreferences, info]: audio notifications simple=" + notify_playSimpleSounds
								+ ", CW=" + notify_playCWCallsignsOnRxedPMs
								+ ", Voice=" + notify_playVoiceCallsignsOnRxedPMs);
			}


			/**
			 * Case AirScout querier
			 */

			Element airScoutEl = getFirstElement(doc, "AirScoutQuerier");
			if (airScoutEl != null) {
				setAirScout_asUDPListenerEnabled(
						getBoolean(
								airScoutEl,
								AirScout_asUDPListenerEnabled,
								"asQry_airScoutCommunicationEnabled"
						)
				);

				setAirScout_asServerNameString(
						getText(
								airScoutEl,
								AirScout_asServerNameString,
								"asQry_airScoutServerName"
						)
				);

				setAirScout_asClientNameString(
						getText(
								airScoutEl,
								AirScout_asClientNameString,
								"asQry_airScoutClientName"
						)
				);

				setAirScout_asCommunicationPort(
						getInt(
								airScoutEl,
								AirScout_asCommunicationPort,
								"asQry_airScoutUDPPort"
						)
				);

				setAirScout_autoBandSelectionEnabled(
						getBoolean(
								airScoutEl,
								AirScout_autoBandSelectionEnabled,
								"asQry_airScoutAutoBandSelectionEnabled"
						)
				);

				setAirScout_asBandString(
						getText(
								airScoutEl,
								AirScout_asBandString,
								"asQry_airScoutBandValue"
						)
				);

				System.out.println(
						"[ChatPreferences, info]: AirScout integration enabled="
								+ AirScout_asUDPListenerEnabled
								+ ", server identifier="
								+ AirScout_asServerNameString
								+ ", client identifier="
								+ AirScout_asClientNameString
								+ ", port="
								+ AirScout_asCommunicationPort
								+ ", automatic band selection="
								+ AirScout_autoBandSelectionEnabled
								+ ", band="
								+ AirScout_asBandString
				);
			}

			/**
			 * Case shortCuts
			 */

			Element shortCutsEl = getFirstElement(doc, "shortCuts");
			if (shortCutsEl != null) {
				lst_txtShortCutBtnList.clear();
				NodeList children = shortCutsEl.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						String v = child.getTextContent();
						if (v != null) {
							v = v.trim();
						}
						if (v != null && !v.isEmpty()) {
							lst_txtShortCutBtnList.add(v);
						}
					}
				}
			}

			/**
			 * Case textSnippets
			 */

			Element textSnippetsEl = getFirstElement(doc, "textSnippets");
			if (textSnippetsEl != null) {
				lst_txtSnipList.clear();
				NodeList children = textSnippetsEl.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						String v = child.getTextContent();
						if (v != null) {
							v = v.trim();
						}
						if (v != null && !v.isEmpty()) {
							lst_txtSnipList.add(v);
						}
					}
				}
			}

			/**
			 * Case QSO-sniffer lists (added later; older configs won't have them)
			 */

			list = doc.getElementsByTagName("snifferCallSigns");

			if (list != null && list.getLength() != 0) {
				lstNotify_QSOSniffer_sniffedCallSignList.clear();

				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {
						NodeList children = node.getChildNodes();

						for (int i = 0; i < children.getLength(); i++) {
							Node child = children.item(i);

							if (child.getNodeType()
									== Node.ELEMENT_NODE) {
								String callSign =
										child.getTextContent();

								if (callSign != null
										&& !callSign.isBlank()) {
									/*
									 * Older preference files may still contain complete KST callsigns
									 * such as DN9APW-2. Monitoring is stored by base callsign so that
									 * every suffix of the same station is covered by one entry.
									 */
									String normalizedCallSign =
											ChatMember.normalizeCallSignToBaseCallSign(
													callSign
											);

									if (!lstNotify_QSOSniffer_sniffedCallSignList.snapshot()
											.contains(normalizedCallSign)) {
										lstNotify_QSOSniffer_sniffedCallSignList
												.add(normalizedCallSign);
									}
								}
							}
						}
					}
				}
			}

			list = doc.getElementsByTagName("snifferPrefixes");
			if (list != null && list.getLength() != 0) {
				lstNotify_QSOSniffer_sniffedPrefixLocList.clear();
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node;
						NodeList children = element.getChildNodes();
						for (int i = 0; i < children.getLength(); i++) {
							Node child = children.item(i);
							if (child.getNodeType() == Node.ELEMENT_NODE) {
								String prefix = child.getTextContent();
								if (prefix != null) {
									prefix = prefix.trim();
								}
								if (prefix != null && !prefix.isEmpty()) {
									lstNotify_QSOSniffer_sniffedPrefixLocList.add(prefix);
								}
							}
						}
					}
				}
			}

			/**
			 * Case beaconCQ
			 */

			Element beaconCQEl = getFirstElement(doc, "beaconCQ");
			if (beaconCQEl != null) {
				bcn_beaconsEnabledMainCat = getBoolean(beaconCQEl, bcn_beaconsEnabledMainCat, "bcn_beaconsEnabledMainCat", "beaconCQEnabled");
				bcn_beaconIntervalInMinutesMainCat = getInt(beaconCQEl, bcn_beaconIntervalInMinutesMainCat, "bcn_beaconIntervalInMinutesMainCat", "beaconCQIntervalMinutes");
				bcn_beaconTextMainCat = getText(beaconCQEl, bcn_beaconTextMainCat, "bcn_beaconTextMainCat", "beaconCQText");

				bcn_beaconsEnabledSecondCat = getBoolean(beaconCQEl, bcn_beaconsEnabledSecondCat, "bcn_beaconsEnabledSecondCat", "beaconCQEnabledSecondCat");
				bcn_beaconIntervalInMinutesSecondCat = getInt(beaconCQEl, bcn_beaconIntervalInMinutesSecondCat, "bcn_beaconIntervalInMinutesSecondCat", "beaconCQIntervalMinutesSecondCat");
				bcn_beaconTextSecondCat = getText(beaconCQEl, bcn_beaconTextSecondCat, "bcn_beaconTextSecondCat", "beaconCQTextSecondText");

				System.out.println("[ChatPreferences, info]: beaconCQ main='" + bcn_beaconTextMainCat + "', second='" + bcn_beaconTextSecondCat + "'");
			}

			/**
			 * Case beaconUnworkedstations
			 *
			 */

			Element beaconUnworkedEl = getFirstElement(doc, "beaconUnworkedstations");
			if (beaconUnworkedEl != null) {
				messageHandling_unworkedStnRequesterBeaconsText = getText(
						beaconUnworkedEl,
						messageHandling_unworkedStnRequesterBeaconsText,
						"messageHandling_unworkedStnRequesterBeaconsText",
						"beaconUnworkedstationsText");
				messageHandling_unworkedStnRequesterBeaconsInterval = getInt(
						beaconUnworkedEl,
						messageHandling_unworkedStnRequesterBeaconsInterval,
						"messageHandling_unworkedStnRequesterBeaconsInterval",
						"beaconUnworkedstationsIntervalMinutes");
				messageHandling_unworkedStnRequesterBeaconsEnabled = getBoolean(
						beaconUnworkedEl,
						messageHandling_unworkedStnRequesterBeaconsEnabled,
						"messageHandling_unworkedStnRequesterBeaconsEnabled",
						"beaconUnworkedstationsEnabled");
				messageHandling_beaconUnworkedstationsPrefix = getText(
						beaconUnworkedEl,
						messageHandling_beaconUnworkedstationsPrefix,
						"messageHandling_beaconUnworkedstationsPrefix",
						"beaconUnworkedstationsPrefix");
				System.out.println("[ChatPreferences, info]: unworked-stations beacon text='" + messageHandling_unworkedStnRequesterBeaconsText + "'");
			}



			/***********************************************
			 *
			 * case messageHandling
			 *
			 ***********************************************/
			messageHandling_debugModeToFileEnabled = true;
			Element messageHandlingEl = getFirstElement(doc, "messageHandling");
			if (messageHandlingEl != null) {
				messageHandling_autoAnswerTextMainCat = getText(
						messageHandlingEl,
						messageHandling_autoAnswerTextMainCat,
						"messageHandling_autoAnswerTextMainCat",
						"autoAnswerText");
				messageHandling_autoAnswerEnabled = getBoolean(
						messageHandlingEl,
						messageHandling_autoAnswerEnabled,
						"messageHandling_autoAnswerEnabled",
						"autoAnswerEnabled");
				messageHandling_autoAnswerToQRGRequestEnabled = getBoolean(
						messageHandlingEl,
						messageHandling_autoAnswerToQRGRequestEnabled,
						"messageHandling_autoAnswerToQRGRequestEnabled",
						"autoAnswerToQrgRequestEnabled",
						"autoAnswerToQRGRequestEnabled");

				messageHandling_autoAnswerTextSecondCat = getText(
						messageHandlingEl,
						messageHandling_autoAnswerTextSecondCat,
						"messageHandling_autoAnswerTextSecondCat",
						"autoAnswerTextSecondCat");
				messageHandling_autoAnswerEnabledSecondCat = getBoolean(
						messageHandlingEl,
						messageHandling_autoAnswerEnabledSecondCat,
						"messageHandling_autoAnswerEnabledSecondCat",
						"autoAnswerEnabledSecondCat");
				messageHandling_debugModeToFileEnabled = getBoolean(
						messageHandlingEl,
						messageHandling_debugModeToFileEnabled,
						"messageHandling_debugModeToFileEnabled");

				/*
				 * The user interface intentionally exposes one shared generic auto-answer
				 * setting for both chat categories. Keep the legacy second-category XML
				 * fields synchronized so existing configuration files remain compatible.
				 */
				messageHandling_autoAnswerTextSecondCat = messageHandling_autoAnswerTextMainCat;
				messageHandling_autoAnswerEnabledSecondCat = messageHandling_autoAnswerEnabled;
			}









			/***********************************************
			 *
			 * case read GUI options
			 *
			 ***********************************************/
			this.setGUIstationMapClusteringEnabled(true);
			this.setGUIstationMapAlwaysOnTop(false);
			list = doc.getElementsByTagName("guiOptions");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						// Scene sizes (semicolon separated), defaults are already in the arrays.
						parseSemicolonDoublesInto(getText(element, null, "GUIscn_ChatwindowMainSceneSizeHW"), this.getGUIscn_ChatwindowMainSceneSizeHW());
						parseSemicolonDoublesInto(getText(element, null, "GUIclusterAndQSOMonStage_SceneSizeHW"), this.getGUIclusterAndQSOMonStage_SceneSizeHW());
						parseSemicolonDoublesInto(getText(element, null, "GUIstage_updateStage_SceneSizeHW"), this.getGUIstage_updateStage_SceneSizeHW());
						parseSemicolonDoublesInto(getText(element, null, "GUIsettingsStageSceneSizeHW"), this.getGUIsettingsStageSceneSizeHW());

						parseSemicolonDoublesInto(
								getText(element, null, "GUIstationMapStageSceneSizeHW"),
								this.getGUIstationMapStageSceneSizeHW()
						);

						parseSemicolonDoublesInto(
								getText(element, null, "GUIstationMapStagePositionXY"),
								this.getGUIstationMapStagePositionXY()
						);

						/*
						 * Files written before config version 5 do not contain this value.
						 * Keep the default true in that case so existing users discover the
						 * path-analysis feature before choosing to hide it themselves.
						 */
						this.setGUIstationMapPathAnalysisVisible(getBoolean(
								element,
								this.isGUIstationMapPathAnalysisVisible(),
								"GUIstationMapPathAnalysisVisible"
						));

						/*
						 * Files written before config version 7 do not contain this value.
						 * Missing or malformed values keep clustering enabled so existing
						 * installations retain the established map behaviour.
						 */
						this.setGUIstationMapClusteringEnabled(getBooleanOrDefault(
								element,
								true,
								"GUIstationMapClusteringEnabled"
						));

						this.setGUIstationMapAlwaysOnTop(getBooleanOrDefault(
								element,
								false,
								"GUIstationMapAlwaysOnTop"
						));

						// Splitpane divider positions
						String s1 = getText(element, null, "GUIselectedCallSignSplitPane_dividerposition");
						if (s1 != null) {
							this.setGUIselectedCallSignSplitPane_dividerposition(csvStringToDoubleArray(s1));
						}
						String s2 = getText(element, null, "GUImainWindowLeftSplitPane_dividerposition");
						if (s2 != null) {
							this.setGUImainWindowLeftSplitPane_dividerposition(csvStringToDoubleArray(s2));
						}
						String s3 = getText(element, null, "GUImessageSectionSplitpane_dividerposition");
						if (s3 != null) {
							double[] parsed = csvStringToDoubleArray(s3);
							// Config files older than ~1.40 had fewer panes.
							if (parsed.length >= 4) {
								this.setGUImessageSectionSplitpane_dividerposition(parsed);
							}
						}
						// GUImainWindowRightSplitPane divider positions (2 dividers => 2 values)
// Backward compatible: old configs stored a single CSV tag.
						String rightLegacy = getText(element, null, "GUImainWindowRightSplitPane_dividerposition");
						String right0 = getText(element, null, "GUImainWindowRightSplitPane_dividerposition0");
						String right1 = getText(element, null, "GUImainWindowRightSplitPane_dividerposition1");

						if (right0 != null || right1 != null) {

							// New format: two dedicated tags
							double p0 = (right0 != null) ? parseDoubleOrDefault(right0, this.GUImainWindowRightSplitPane_dividerpositionDefault[0])
									: this.GUImainWindowRightSplitPane_dividerpositionDefault[0];

							double p1 = (right1 != null) ? parseDoubleOrDefault(right1, this.GUImainWindowRightSplitPane_dividerpositionDefault[1])
									: this.GUImainWindowRightSplitPane_dividerpositionDefault[1];

							this.setGUImainWindowRightSplitPane_dividerposition(new double[] { p0, p1 });

						} else if (rightLegacy != null) {

							// Old format: CSV array (often length 1)
							double[] parsed = csvStringToDoubleArray(rightLegacy);

							// Upgrade older config files gracefully
							if (parsed.length >= 2) {
								this.setGUImainWindowRightSplitPane_dividerposition(new double[] { parsed[0], parsed[1] });
							} else if (parsed.length == 1) {
								this.setGUImainWindowRightSplitPane_dividerposition(new double[] {
										parsed[0],
										this.GUImainWindowRightSplitPane_dividerpositionDefault[1]
								});
							}
						}

						// Ensure correct length no matter what was in the config (prevents AIOOBE)
						ensureMainWindowRightSplitPaneDividerPositions(2);


						String s5 = getText(element, null, "GUIpnl_directedMSGWin_dividerpositionDefault");
						if (s5 != null) {
							this.setGUIpnl_directedMSGWin_dividerpositionDefault(csvStringToDoubleArray(s5));
						}

						readWindowPositions(element);

						tableColumnWidths.clear();
						NodeList widthElements = element.getElementsByTagName(TAG_TABLE_COLUMN_WIDTH);
						for (int widthIndex = 0; widthIndex < widthElements.getLength(); widthIndex++) {
							Node widthNode = widthElements.item(widthIndex);
							if (!(widthNode instanceof Element widthElement)) {
								continue;
							}

							String tableId = widthElement.getAttribute("tableId");
							String columnId = widthElement.getAttribute("columnId");
							double width = parseDoubleOrDefault(widthElement.getAttribute("pixels"), Double.NaN);
							if (isValidTableColumnIdentity(tableId, columnId) && isValidTableColumnWidth(width)) {
								tableColumnWidths.put(tableColumnWidthKey(tableId, columnId), width);
							}
						}
					}
				}
			}

			/***********************************************
			 *
			 * case read guiSaveableOptions
			 *
			 ***********************************************/
			Element guiSaveableOptionsEl = getFirstElement(doc, "guiSaveableOptions");
			if (guiSaveableOptionsEl != null) {
				this.setGuiOptions_defaultFilterNothing(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterNothing(), "guiOptions_defaultFilterNothing"));
				this.setGuiOptions_defaultFilterPmToMe(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterPmToMe(), "guiOptions_defaultFilterPmToMe"));
				this.setGuiOptions_defaultFilterPmToOther(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterPmToOther(), "guiOptions_defaultFilterPmToOther"));
				this.setGuiOptions_defaultFilterPublicMsgs(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterPublicMsgs(), "guiOptions_defaultFilterPublicMsgs"));
				this.setGuiOptions_showGrossFieldWorkedHintInBandColumns(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_showGrossFieldWorkedHintInBandColumns(), "guiOptions_showGrossFieldWorkedHintInBandColumns"));
				this.setGuiOptions_showFreshCallHintInBandColumns(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_showFreshCallHintInBandColumns(), "guiOptions_showFreshCallHintInBandColumns"));

				// Added in release 1.50: the operator's own colours, one string per design.
				this.setGuiOptions_paletteOverridesDay(getText(guiSaveableOptionsEl, this.getGuiOptions_paletteOverridesDay(), "guiOptions_paletteOverridesDay"));
				this.setGuiOptions_paletteOverridesEvening(getText(guiSaveableOptionsEl, this.getGuiOptions_paletteOverridesEvening(), "guiOptions_paletteOverridesEvening"));
				this.setGuiOptions_language(getText(guiSaveableOptionsEl, this.getGuiOptions_language(), "guiOptions_language"));

				// Added in later versions: dark mode flags
				this.GUI_darkModeActive = getBoolean(guiSaveableOptionsEl, this.GUI_darkModeActive, "guiOptions_darkModeActive");
				this.GUI_darkModeActiveByDefault = getBoolean(guiSaveableOptionsEl, this.GUI_darkModeActiveByDefault, "guiOptions_darkModeActiveByDefault");

				// Added in release 1.50: opt-in for window positions across restarts.
				this.guiOptions_rememberWindowPositions = getBoolean(guiSaveableOptionsEl, false, "guiOptions_rememberWindowPositions");
			}



		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			System.out.println(e.getCause());
			System.out.println(e.getMessage());
		}

		initializeActualQtfFromDefaultIfRequired();
		return true;

	}

	private void initializeActualQtfFromDefaultIfRequired() {
		if (!actualQtfInitializedFromPreferences) {
			actualQTF.set(stn_qtfDefault);
			actualQtfInitializedFromPreferences = true;
		}
	}

	/**
	 *
	 * @return double array with separated values for each doublevalue, seperated by ";", empty array if none
	 */
	private double [] csvStringToDoubleArray(String csvStringWithDoubles){

		String separator = ";";
		double[] result = new double[csvStringWithDoubles.split(separator).length];

		for (int i = 0; i < (csvStringWithDoubles.split(separator).length); i++) {
			result[i] =
					Double.parseDouble(csvStringWithDoubles.split(separator)[i]);
//			System.out.println("EXTRACTED " + result[i]);
		}

		return result;
	}

	private String doubleArrayToCSVString(double[] doubleArrayToCSVConvert){

		String separator = ";";
		String result = "";

		for (int i = 0; i < (doubleArrayToCSVConvert.length); i++) {
			result += doubleArrayToCSVConvert[i];

			if (i+1<doubleArrayToCSVConvert.length) {
				result += separator;
			}

		}

		return result;
	}

	public boolean isStn_bandActive50() {
		return stn_bandActive50;
	}

	public void setStn_bandActive50(boolean stn_bandActive50) {
		this.stn_bandActive50 = stn_bandActive50;
	}

	public boolean isStn_bandActive70() {
		return stn_bandActive70;
	}

	public void setStn_bandActive70(boolean stn_bandActive70) {
		this.stn_bandActive70 = stn_bandActive70;
	}

	public boolean isStn_bandActive144() {
		return stn_bandActive144;
	}

	public void setStn_bandActive144(boolean stn_bandActive144) {
		this.stn_bandActive144 = stn_bandActive144;
	}

	public boolean isStn_bandActive432() {
		return stn_bandActive432;
	}

	public void setStn_bandActive432(boolean stn_bandActive432) {
		this.stn_bandActive432 = stn_bandActive432;
	}

	public boolean isStn_bandActive1240() {
		return stn_bandActive1240;
	}

	public void setStn_bandActive1240(boolean stn_bandActive1240) {
		this.stn_bandActive1240 = stn_bandActive1240;
	}

	public boolean isStn_bandActive2300() {
		return stn_bandActive2300;
	}

	public void setStn_bandActive2300(boolean stn_bandActive2300) {
		this.stn_bandActive2300 = stn_bandActive2300;
	}

	public boolean isStn_bandActive3400() {
		return stn_bandActive3400;
	}

	public void setStn_bandActive3400(boolean stn_bandActive3400) {
		this.stn_bandActive3400 = stn_bandActive3400;
	}

	public boolean isStn_bandActive5600() {
		return stn_bandActive5600;
	}

	public void setStn_bandActive5600(boolean stn_bandActive5600) {
		this.stn_bandActive5600 = stn_bandActive5600;
	}

	public boolean isStn_bandActive10G() {
		return stn_bandActive10G;
	}

	public void setStn_bandActive10G(boolean stn_bandActive10G) {
		this.stn_bandActive10G = stn_bandActive10G;
	}

	public boolean isGUI_darkModeActive() {
		return GUI_darkModeActive;
	}

	public void setGUI_darkModeActive(boolean GUI_darkModeActive) {
		this.GUI_darkModeActive = GUI_darkModeActive;
	}

	public boolean isGUI_darkModeActiveByDefault() {
		return GUI_darkModeActiveByDefault;
	}

	public void setGUI_darkModeActiveByDefault(boolean GUI_darkModeActiveByDefault) {
		this.GUI_darkModeActiveByDefault = GUI_darkModeActiveByDefault;
	}

	public boolean isGuiOptions_rememberWindowPositions() {
		return guiOptions_rememberWindowPositions;
	}

	public void setGuiOptions_rememberWindowPositions(boolean guiOptions_rememberWindowPositions) {
		this.guiOptions_rememberWindowPositions = guiOptions_rememberWindowPositions;
	}

	/**
	 * Returns the stored position of one window.
	 *
	 * @param windowId stable window identifier
	 * @return a copy of {x, y, screenX, screenY, screenWidth, screenHeight}, or null when
	 *         nothing usable is stored
	 */
	public synchronized int[] getWindowPosition(String windowId) {
		int[] stored = windowPositions.get(windowId);
		return stored == null ? null : stored.clone();
	}

	/**
	 * Updates one window position in memory. Persistence is coordinated by the layout
	 * autosave layer, like the table column widths.
	 */
	public synchronized void setWindowPosition(String windowId, int x, int y,
			int screenX, int screenY, int screenWidth, int screenHeight) {
		if (isValidWindowId(windowId) && screenWidth > 0 && screenHeight > 0) {
			windowPositions.put(windowId, new int[] { x, y, screenX, screenY, screenWidth, screenHeight });
		}
	}

	private static boolean isValidWindowId(String windowId) {
		return windowId != null && !windowId.isBlank();
	}

	private void appendWindowPositions(Document document, Element guiOptions) {
		for (Map.Entry<String, int[]> entry : windowPositions.entrySet()) {
			int[] p = entry.getValue();
			Element element = document.createElement(TAG_WINDOW_POSITION);
			element.setAttribute("windowId", entry.getKey());
			element.setAttribute("x", String.valueOf(p[0]));
			element.setAttribute("y", String.valueOf(p[1]));
			element.setAttribute("screenX", String.valueOf(p[2]));
			element.setAttribute("screenY", String.valueOf(p[3]));
			element.setAttribute("screenWidth", String.valueOf(p[4]));
			element.setAttribute("screenHeight", String.valueOf(p[5]));
			guiOptions.appendChild(element);
		}
	}

	private void readWindowPositions(Element guiOptions) {
		windowPositions.clear();
		NodeList elements = guiOptions.getElementsByTagName(TAG_WINDOW_POSITION);
		for (int index = 0; index < elements.getLength(); index++) {
			if (!(elements.item(index) instanceof Element element)) {
				continue;
			}
			try {
				setWindowPosition(element.getAttribute("windowId"),
						Integer.parseInt(element.getAttribute("x")),
						Integer.parseInt(element.getAttribute("y")),
						Integer.parseInt(element.getAttribute("screenX")),
						Integer.parseInt(element.getAttribute("screenY")),
						Integer.parseInt(element.getAttribute("screenWidth")),
						Integer.parseInt(element.getAttribute("screenHeight")));
			} catch (NumberFormatException ignored) {
				// A damaged entry is dropped; the window then opens by the default rule.
			}
		}
	}

	/**
	 *
	 * If the file-reading goes wrong, set the defaults
	 */
	public void setPreferencesDefaults() {
		System.out.println("[ChatPreferences, Info]: restoring prefs from defaults");

	}

	// ---------------------------------------------------------------------
	// XML helper methods
	// ---------------------------------------------------------------------

	private static Element getFirstElement(Document doc, String tagName) {
		NodeList nl = doc.getElementsByTagName(tagName);
		if (nl == null || nl.getLength() == 0) {
			return null;
		}
		Node n = nl.item(0);
		return (n instanceof Element) ? (Element) n : null;
	}

	private static DocumentBuilderFactory createSecureDocumentBuilderFactory()
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		return factory;
	}

	private static Element getDirectChildElement(Element parent, String tagName) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element && tagName.equals(element.getTagName())) {
				return element;
			}
		}
		return null;
	}

	private static void upsertDirectChildText(
			Document document,
			Element parent,
			String tagName,
			String value
	) {
		Element element = getDirectChildElement(parent, tagName);
		if (element == null) {
			element = document.createElement(tagName);
			parent.appendChild(element);
		}
		element.setTextContent(value);
	}

	private static void removeDirectChildren(Element parent, String tagName) {
		for (Node child = parent.getFirstChild(); child != null; ) {
			Node next = child.getNextSibling();
			if (child instanceof Element element && tagName.equals(element.getTagName())) {
				parent.removeChild(child);
			}
			child = next;
		}
	}

	private static String tableColumnWidthKey(String tableId, String columnId) {
		if (!isValidTableColumnIdentity(tableId, columnId)) {
			return "";
		}
		return tableId + '\u0000' + columnId;
	}

	private static boolean isValidTableColumnIdentity(String tableId, String columnId) {
		return tableId != null && !tableId.isBlank() && tableId.indexOf('\u0000') < 0
				&& columnId != null && !columnId.isBlank() && columnId.indexOf('\u0000') < 0;
	}

	private static boolean isValidTableColumnWidth(Double width) {
		return width != null && Double.isFinite(width)
				&& width >= MIN_TABLE_COLUMN_WIDTH && width <= MAX_TABLE_COLUMN_WIDTH;
	}

	/**
	 * Returns the text content of the first matching child tag (directly under {@code parent})
	 * or {@code defaultValue} if the tag does not exist or is empty.
	 */
	private static String getText(Element parent, String defaultValue, String... tagNames) {
		if (parent == null || tagNames == null) {
			return defaultValue;
		}
		for (String t : tagNames) {
			if (t == null || t.isEmpty()) {
				continue;
			}
			NodeList nl = parent.getElementsByTagName(t);
			if (nl == null || nl.getLength() == 0) {
				continue;
			}
			Node n = nl.item(0);
			if (n == null) {
				continue;
			}
			String v = n.getTextContent();
			if (v != null) {
				v = v.trim();
			}
			if (v != null && !v.isEmpty()) {
				return v;
			}
		}
		return defaultValue;
	}

	private static boolean getBoolean(Element parent, boolean defaultValue, String... tagNames) {
		String v = getText(parent, null, tagNames);
		if (v == null) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
	}

	private static boolean getBooleanOrDefault(Element parent, boolean defaultValue, String... tagNames) {
		String value = getText(parent, null, tagNames);
		if (value == null) {
			return defaultValue;
		}
		if ("true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value) || "0".equals(value) || "no".equalsIgnoreCase(value)) {
			return false;
		}
		return defaultValue;
	}

	private static int getInt(Element parent, int defaultValue, String... tagNames) {
		String v = getText(parent, null, tagNames);
		if (v == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException ignore) {
			return defaultValue;
		}
	}

	private static double getDouble(Element parent, double defaultValue, String... tagNames) {
		String v = getText(parent, null, tagNames);
		if (v == null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(v.trim());
		} catch (NumberFormatException ignore) {
			return defaultValue;
		}
	}

	private static int getIntFromDoc(Document doc, int defaultValue, String... tagNames) {
		if (doc == null) {
			return defaultValue;
		}
		for (String t : tagNames) {
			if (t == null || t.isEmpty()) {
				continue;
			}
			NodeList nl = doc.getElementsByTagName(t);
			if (nl == null || nl.getLength() == 0) {
				continue;
			}
			Node n = nl.item(0);
			if (n == null) {
				continue;
			}
			String v = n.getTextContent();
			if (v == null) {
				continue;
			}
			try {
				return Integer.parseInt(v.trim());
			} catch (NumberFormatException ignore) {
				// try next
			}
		}
		return defaultValue;
	}

	private static void parseSemicolonDoublesInto(String input, double[] target) {
		if (input == null || target == null) {
			return;
		}
		String[] parts = input.trim().split(";");
		int len = Math.min(parts.length, target.length);
		for (int i = 0; i < len; i++) {
			try {
				target[i] = Double.parseDouble(parts[i].trim());
			} catch (NumberFormatException ignore) {
				// keep existing value in target[i]
			}
		}
	}

	/**
	 * Checks wheter the input value of the String is numeric or not, true if yes
	 * TODO: Move to a utils class for checking input values by user...
	 *
	 * @param str
	 * @return
	 */
	private static boolean isNumeric(String str) {
		return str != null && str.matches("[0-9.]+");
	}

//	public ObservableList<String> getTxtSnippetsAsObservableList(){
//		SimpleRoster<String> lst_txtSnipList = new SimpleRoster<>();
//
//		for (int i = 0; i < textSnippets.length; i++) {
//			lst_txtSnipList.add(textSnippets[i]);
//			System.out.println(textSnippets[i]);
//		}
//
//		return lst_txtSnipList;
//
//	}


	/**
	 * Ensures that the divider position array matches the current number of dividers.
	 * This upgrades older XML configs (e.g. only 1 divider stored) without crashing.
	 */
	public void ensureMainWindowRightSplitPaneDividerPositions(int requiredDividerCount) {
		if (requiredDividerCount < 0) return;

		if (GUImainWindowRightSplitPane_dividerposition != null
				&& GUImainWindowRightSplitPane_dividerposition.length == requiredDividerCount) {
			return;
		}

		double[] upgraded = new double[requiredDividerCount];

		for (int i = 0; i < requiredDividerCount; i++) {

			// Prefer existing stored values if present
			if (GUImainWindowRightSplitPane_dividerposition != null
					&& i < GUImainWindowRightSplitPane_dividerposition.length) {
				upgraded[i] = GUImainWindowRightSplitPane_dividerposition[i];
				continue;
			}

			// Otherwise use defaults, fallback to even spacing
			if (i < GUImainWindowRightSplitPane_dividerpositionDefault.length) {
				upgraded[i] = GUImainWindowRightSplitPane_dividerpositionDefault[i];
			} else {
				upgraded[i] = (i + 1.0) / (requiredDividerCount + 1.0);
			}
		}

		GUImainWindowRightSplitPane_dividerposition = upgraded;
	}

	private static double parseDoubleOrDefault(String value, double defaultValue) {
		if (value == null) return defaultValue;
		String v = value.trim();
		if (v.isEmpty()) return defaultValue;
		try {
			return Double.parseDouble(v);
		} catch (Exception ignore) {
			return defaultValue;
		}
	}

	public double getStn_pathAnalysisOwnTxPowerWatts() {
		return stn_pathAnalysisOwnTxPowerWatts;
	}

	public void setStn_pathAnalysisOwnTxPowerWatts(double stn_pathAnalysisOwnTxPowerWatts) {
		this.stn_pathAnalysisOwnTxPowerWatts = stn_pathAnalysisOwnTxPowerWatts;
	}

	public double getStn_pathAnalysisOwnAntennaGainDbi() {
		return stn_pathAnalysisOwnAntennaGainDbi;
	}

	public void setStn_pathAnalysisOwnAntennaGainDbi(double stn_pathAnalysisOwnAntennaGainDbi) {
		this.stn_pathAnalysisOwnAntennaGainDbi = stn_pathAnalysisOwnAntennaGainDbi;
	}

	public double getStn_pathAnalysisDefaultTargetTxPowerWatts() {
		return stn_pathAnalysisDefaultTargetTxPowerWatts;
	}

	public void setStn_pathAnalysisDefaultTargetTxPowerWatts(double stn_pathAnalysisDefaultTargetTxPowerWatts) {
		this.stn_pathAnalysisDefaultTargetTxPowerWatts = stn_pathAnalysisDefaultTargetTxPowerWatts;
	}

	public double getStn_pathAnalysisDefaultTargetAntennaGainDbi() {
		return stn_pathAnalysisDefaultTargetAntennaGainDbi;
	}

	public void setStn_pathAnalysisDefaultTargetAntennaGainDbi(double stn_pathAnalysisDefaultTargetAntennaGainDbi) {
		this.stn_pathAnalysisDefaultTargetAntennaGainDbi = stn_pathAnalysisDefaultTargetAntennaGainDbi;
	}

	public double getStn_pathAnalysisVhfFeederLossPerStationDb() {
		return stn_pathAnalysisVhfFeederLossPerStationDb;
	}

	public void setStn_pathAnalysisVhfFeederLossPerStationDb(double stn_pathAnalysisVhfFeederLossPerStationDb) {
		this.stn_pathAnalysisVhfFeederLossPerStationDb = stn_pathAnalysisVhfFeederLossPerStationDb;
	}

	public double getStn_pathAnalysisFeederLossIncreaseDbPer200MHz() {
		return stn_pathAnalysisFeederLossIncreaseDbPer200MHz;
	}

	public void setStn_pathAnalysisFeederLossIncreaseDbPer200MHz(double stn_pathAnalysisFeederLossIncreaseDbPer200MHz) {
		this.stn_pathAnalysisFeederLossIncreaseDbPer200MHz = stn_pathAnalysisFeederLossIncreaseDbPer200MHz;
	}

	public double getStn_pathAnalysisMaxEstimatedFeederLossPerStationDb() {
		return stn_pathAnalysisMaxEstimatedFeederLossPerStationDb;
	}

	public void setStn_pathAnalysisMaxEstimatedFeederLossPerStationDb(double stn_pathAnalysisMaxEstimatedFeederLossPerStationDb) {
		this.stn_pathAnalysisMaxEstimatedFeederLossPerStationDb = stn_pathAnalysisMaxEstimatedFeederLossPerStationDb;
	}

	public double getStn_pathAnalysisRequiredSsbSignalDbm() {
		return stn_pathAnalysisRequiredSsbSignalDbm;
	}

	public void setStn_pathAnalysisRequiredSsbSignalDbm(double stn_pathAnalysisRequiredSsbSignalDbm) {
		this.stn_pathAnalysisRequiredSsbSignalDbm = stn_pathAnalysisRequiredSsbSignalDbm;
	}

	public double getStn_pathAnalysisRequiredCwSignalDbm() {
		return stn_pathAnalysisRequiredCwSignalDbm;
	}

	public void setStn_pathAnalysisRequiredCwSignalDbm(double stn_pathAnalysisRequiredCwSignalDbm) {
		this.stn_pathAnalysisRequiredCwSignalDbm = stn_pathAnalysisRequiredCwSignalDbm;
	}

	public double getStn_pathAnalysisContestMarginDb() {
		return stn_pathAnalysisContestMarginDb;
	}

	public void setStn_pathAnalysisContestMarginDb(double stn_pathAnalysisContestMarginDb) {
		this.stn_pathAnalysisContestMarginDb = stn_pathAnalysisContestMarginDb;
	}

	public kst4contest.view.map.PathLinkBudgetSettings buildPathLinkBudgetSettings() {
		return new kst4contest.view.map.PathLinkBudgetSettings(
				stn_pathAnalysisOwnTxPowerWatts,
				stn_pathAnalysisOwnAntennaGainDbi,
				stn_pathAnalysisDefaultTargetTxPowerWatts,
				stn_pathAnalysisDefaultTargetAntennaGainDbi,
				stn_pathAnalysisVhfFeederLossPerStationDb,
				stn_pathAnalysisFeederLossIncreaseDbPer200MHz,
				stn_pathAnalysisMaxEstimatedFeederLossPerStationDb,
				stn_pathAnalysisRequiredSsbSignalDbm,
				stn_pathAnalysisRequiredCwSignalDbm,
				stn_pathAnalysisContestMarginDb
		);
	}
}



