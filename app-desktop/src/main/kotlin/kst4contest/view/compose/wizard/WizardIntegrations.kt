package kst4contest.view.compose.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.tabs.AirscoutTabState
import kst4contest.view.compose.tabs.StationTabState

/** Where the worked stations come from. The wizard knows one source at a time. */
enum class WizardLogProgram {
    NONE,

    /** The Win-Test network listener. */
    WINTEST,

    /** UCXLog, DXLog.net, N1MM+ and QARTEST: QSO messages on the shared UDP port. */
    UDP,

    /** A log file polled for callsigns. */
    FILE,
}

/** Something that was on in the profile and will be off after Save, for the summary. */
enum class WizardSwitch {
    SECOND_CHAT,
    LOG_WINTEST,
    LOG_UDP,
    LOG_FILE,
    QRG_FOLLOW,
    CLUSTER_SERVER,
    ROTATOR,
    AIRSCOUT,
}

/**
 * The setup wizard's answers about log program, rotator and AirScout.
 *
 * Two pages share this class because they share a rule: a station set up anew gets only what
 * the operator said yes to. [loadFresh] therefore switches everything off -- the bundled
 * template has several of these on, which a newcomer without a log program does not know --
 * while ports, names, host and identifiers stay as stored and are proposals.
 *
 * The wizard switches things on and off and takes numbers over. It invents none: no port,
 * no identifier and no default here differs from what the settings tabs accept.
 *
 * The settings window allows several log sources at once and the wizard's question does
 * not. [severalStoredLogSources] says when the stored profile is such a case, so the caller
 * can keep the log page shut rather than collapse the sources into one.
 *
 * @param prefs the profile's preferences, already read from its file
 */
class WizardIntegrations(prefs: ChatPreferences) {

    private val storedWintest = prefs.isLogsynch_wintestNetworkListenerEnabled()
    private val storedUdp = prefs.isLogsynch_ucxUDPWkdCallListenerEnabled()
    private val storedFile = prefs.isLogsynch_fileBasedWkdCallInterpreterEnabled()
    private val storedUdpQrg = prefs.isTrxSynch_ucxLogUDPListenerEnabled()
    private val storedWintestQrg = prefs.isLogsynch_wintestQrgSyncEnabled()
    private val storedCluster = prefs.isNotify_dxClusterServerEnabled()
    private val storedRotator = prefs.isStn_pstRotatorEnabled()
    private val storedAirScout = prefs.isAirScout_asUDPListenerEnabled()

    /** Rotator 2 only counts while SO2R uses it, as on the Station tab. */
    private val rotatorTwoInUse = prefs.isTrxSynch_so2rEnabled() && prefs.isStn_pstRotator2Enabled()
    private val rotatorTwoPort = prefs.getStn_pstRotator2Port()

    /** More than one log source is on in the profile; the log page cannot show that. */
    val severalStoredLogSources: Boolean = listOf(storedWintest, storedUdp, storedFile).count { it } > 1

    var logProgram: WizardLogProgram by mutableStateOf(WizardLogProgram.NONE)
        private set

    var udpPort: Int by mutableStateOf(prefs.getLogsynch_ucxUDPWkdCallListenerPort())
    var wintestPort: Int by mutableStateOf(prefs.getLogsynch_wintestNetworkPort())
    var wintestStationName: String by mutableStateOf(prefs.getLogsynch_wintestNetworkStationNameOfKST().orEmpty())
    var filePath: String by mutableStateOf(prefs.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly().orEmpty())

    /** Take the own frequency from the chosen network program. */
    var qrgFollow: Boolean by mutableStateOf(false)

    /** Pass spots to the log program through the local DX cluster server. */
    var clusterServer: Boolean by mutableStateOf(false)

    var rotator: Boolean by mutableStateOf(false)
    var rotatorHost: String by mutableStateOf(prefs.getStn_pstRotatorHost().orEmpty())
    var rotatorPort: Int by mutableStateOf(prefs.getStn_pstRotatorPort())

    var airScout: Boolean by mutableStateOf(false)
    var airScoutPort: Int by mutableStateOf(prefs.getAirScout_asCommunicationPort())
    var airScoutServerName: String by mutableStateOf(prefs.getAirScout_asServerNameString().orEmpty())
    var airScoutClientName: String by mutableStateOf(prefs.getAirScout_asClientNameString().orEmpty())

    init {
        loadStored()
    }

    /** The switches as the profile has them. Several sources show as one, the network ones first. */
    fun loadStored() {
        logProgram = when {
            storedWintest -> WizardLogProgram.WINTEST
            storedUdp -> WizardLogProgram.UDP
            storedFile -> WizardLogProgram.FILE
            else -> WizardLogProgram.NONE
        }
        qrgFollow = when (logProgram) {
            WizardLogProgram.WINTEST -> storedWintestQrg
            WizardLogProgram.UDP -> storedUdpQrg
            else -> false
        }
        clusterServer = networked && storedCluster
        rotator = storedRotator
        airScout = storedAirScout
    }

    /** A station set up anew: every switch off, every number left as a proposal. */
    fun loadFresh() {
        logProgram = WizardLogProgram.NONE
        qrgFollow = false
        clusterServer = false
        rotator = false
        airScout = false
    }

    /** Only a network program can deliver a frequency or take spots. */
    val networked: Boolean
        get() = logProgram == WizardLogProgram.WINTEST || logProgram == WizardLogProgram.UDP

    fun chooseLogProgram(program: WizardLogProgram) {
        logProgram = program
        if (!networked) {
            qrgFollow = false
            clusterServer = false
        }
    }

    /** Whether what the chosen program needs is there. A program not chosen needs nothing. */
    val logComplete: Boolean
        get() = when (logProgram) {
            WizardLogProgram.NONE -> true
            WizardLogProgram.WINTEST -> AirscoutTabState.isValidPort(wintestPort) && wintestStationName.isNotBlank()
            WizardLogProgram.UDP -> AirscoutTabState.isValidPort(udpPort)
            WizardLogProgram.FILE -> filePath.isNotBlank()
        }

    /** The same or the next port as rotator 2, which answers on its port + 1. */
    val rotatorPortClashes: Boolean
        get() = rotator && rotatorTwoInUse && StationTabState.portsClash(rotatorPort, rotatorTwoPort)

    /** Whether what the switched-on equipment needs is there. */
    val gearComplete: Boolean
        get() {
            val rotatorComplete = !rotator ||
                (rotatorHost.isNotBlank() && AirscoutTabState.isValidPort(rotatorPort) && !rotatorPortClashes)
            val airScoutComplete = !airScout ||
                (AirscoutTabState.isValidPort(airScoutPort) &&
                    AirscoutTabState.isValidIdentifier(airScoutServerName) &&
                    AirscoutTabState.isValidIdentifier(airScoutClientName))
            return rotatorComplete && airScoutComplete
        }

    /**
     * Puts the log page's answers into [prefs]: exactly the chosen source on, its details
     * stored, and the frequency source and the DX cluster server only with a network program.
     * Details of a program that is not chosen are left as they are. Call only while
     * [logComplete].
     */
    fun writeLogTo(prefs: ChatPreferences) {
        prefs.setLogsynch_wintestNetworkListenerEnabled(logProgram == WizardLogProgram.WINTEST)
        prefs.setLogsynch_ucxUDPWkdCallListenerEnabled(logProgram == WizardLogProgram.UDP)
        prefs.setLogsynch_fileBasedWkdCallInterpreterEnabled(logProgram == WizardLogProgram.FILE)

        when (logProgram) {
            WizardLogProgram.WINTEST -> {
                prefs.setLogsynch_wintestNetworkPort(wintestPort)
                prefs.setLogsynch_wintestNetworkStationNameOfKST(wintestStationName.trim())
            }
            WizardLogProgram.UDP -> prefs.setLogsynch_ucxUDPWkdCallListenerPort(udpPort)
            WizardLogProgram.FILE -> prefs.setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(filePath)
            WizardLogProgram.NONE -> Unit
        }

        prefs.setLogsynch_wintestQrgSyncEnabled(logProgram == WizardLogProgram.WINTEST && qrgFollow)
        prefs.setTrxSynch_ucxLogUDPListenerEnabled(logProgram == WizardLogProgram.UDP && qrgFollow)
        prefs.setNotify_dxClusterServerEnabled(networked && clusterServer)
    }

    /**
     * Puts the equipment page's answers into [prefs]. Host, port and identifiers are stored
     * only for what is switched on. Call only while [gearComplete].
     */
    fun writeGearTo(prefs: ChatPreferences) {
        prefs.setStn_pstRotatorEnabled(rotator)
        if (rotator) {
            prefs.setStn_pstRotatorHost(rotatorHost.trim())
            prefs.setStn_pstRotatorPort(rotatorPort)
        }

        prefs.setAirScout_asUDPListenerEnabled(airScout)
        if (airScout) {
            prefs.setAirScout_asCommunicationPort(airScoutPort)
            prefs.setAirScout_asServerNameString(airScoutServerName.trim())
            prefs.setAirScout_asClientNameString(airScoutClientName.trim())
        }
    }

    /** What the log page's answers switch off that the profile had on, in a fixed order. */
    fun switchedOffByLog(): List<WizardSwitch> = buildList {
        if (storedWintest && logProgram != WizardLogProgram.WINTEST) add(WizardSwitch.LOG_WINTEST)
        if (storedUdp && logProgram != WizardLogProgram.UDP) add(WizardSwitch.LOG_UDP)
        if (storedFile && logProgram != WizardLogProgram.FILE) add(WizardSwitch.LOG_FILE)
        if ((storedUdpQrg || storedWintestQrg) && !qrgFollow) add(WizardSwitch.QRG_FOLLOW)
        if (storedCluster && !(networked && clusterServer)) add(WizardSwitch.CLUSTER_SERVER)
    }

    /** What the equipment page's answers switch off that the profile had on. */
    fun switchedOffByGear(): List<WizardSwitch> = buildList {
        if (storedRotator && !rotator) add(WizardSwitch.ROTATOR)
        if (storedAirScout && !airScout) add(WizardSwitch.AIRSCOUT)
    }
}
