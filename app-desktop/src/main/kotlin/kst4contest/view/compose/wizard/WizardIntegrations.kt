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

    private val storedUdpPort = prefs.getLogsynch_ucxUDPWkdCallListenerPort()
    private val storedWintestPort = prefs.getLogsynch_wintestNetworkPort()
    private val storedWintestStationName = prefs.getLogsynch_wintestNetworkStationNameOfKST().orEmpty()
    private val storedFilePath = prefs.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly().orEmpty()
    private val storedRotatorHost = prefs.getStn_pstRotatorHost().orEmpty()
    private val storedRotatorPort = prefs.getStn_pstRotatorPort()
    private val storedAirScoutPort = prefs.getAirScout_asCommunicationPort()
    private val storedAirScoutServerName = prefs.getAirScout_asServerNameString().orEmpty()
    private val storedAirScoutClientName = prefs.getAirScout_asClientNameString().orEmpty()

    /**
     * Whether the profile takes its own frequency from a log program, whichever one. Known
     * apart from [qrgFollow] because the settings window allows a frequency source without
     * the matching log source, which the page's single choice cannot show.
     */
    val storedQrgFollow: Boolean = storedUdpQrg || storedWintestQrg

    /** Whether the profile runs the local DX cluster server, with or without a log source. */
    val storedClusterServer: Boolean = storedCluster

    var udpPort: Int by mutableStateOf(storedUdpPort)
    var wintestPort: Int by mutableStateOf(storedWintestPort)
    var wintestStationName: String by mutableStateOf(storedWintestStationName)
    var filePath: String by mutableStateOf(storedFilePath)

    /** Take the own frequency from the chosen network program. */
    var qrgFollow: Boolean by mutableStateOf(false)

    /** Pass spots to the log program through the local DX cluster server. */
    var clusterServer: Boolean by mutableStateOf(false)

    var rotator: Boolean by mutableStateOf(false)
    var rotatorHost: String by mutableStateOf(storedRotatorHost)
    var rotatorPort: Int by mutableStateOf(storedRotatorPort)

    var airScout: Boolean by mutableStateOf(false)
    var airScoutPort: Int by mutableStateOf(storedAirScoutPort)
    var airScoutServerName: String by mutableStateOf(storedAirScoutServerName)
    var airScoutClientName: String by mutableStateOf(storedAirScoutClientName)

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
        loadStoredDetails()
    }

    /** A station set up anew: every switch off, every number left as a proposal. */
    fun loadFresh() {
        logProgram = WizardLogProgram.NONE
        qrgFollow = false
        clusterServer = false
        rotator = false
        airScout = false
        loadStoredDetails()
    }

    /**
     * Ports, names, host and identifiers back to what the profile holds. Both loads do this:
     * they run when the operator changes his mind about where the station comes from, and a
     * number typed under one choice must not turn up under the other.
     */
    private fun loadStoredDetails() {
        udpPort = storedUdpPort
        wintestPort = storedWintestPort
        wintestStationName = storedWintestStationName
        filePath = storedFilePath
        rotatorHost = storedRotatorHost
        rotatorPort = storedRotatorPort
        airScoutPort = storedAirScoutPort
        airScoutServerName = storedAirScoutServerName
        airScoutClientName = storedAirScoutClientName
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
                (rotatorHost.isNotBlank() && rotatorPort in ROTATOR_PORTS && !rotatorPortClashes)
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

    /** The log-page switches the profile these answers were built from has on. */
    fun storedLogSwitches(): Set<WizardSwitch> = buildSet {
        if (storedWintest) add(WizardSwitch.LOG_WINTEST)
        if (storedUdp) add(WizardSwitch.LOG_UDP)
        if (storedFile) add(WizardSwitch.LOG_FILE)
        if (storedQrgFollow) add(WizardSwitch.QRG_FOLLOW)
        if (storedCluster) add(WizardSwitch.CLUSTER_SERVER)
    }

    /** The equipment switches the profile these answers were built from has on. */
    fun storedGearSwitches(): Set<WizardSwitch> = buildSet {
        if (storedRotator) add(WizardSwitch.ROTATOR)
        if (storedAirScout) add(WizardSwitch.AIRSCOUT)
    }

    /** The log-page switches that are on once [writeLogTo] has run. */
    fun answeredLogSwitches(): Set<WizardSwitch> = buildSet {
        when (logProgram) {
            WizardLogProgram.WINTEST -> add(WizardSwitch.LOG_WINTEST)
            WizardLogProgram.UDP -> add(WizardSwitch.LOG_UDP)
            WizardLogProgram.FILE -> add(WizardSwitch.LOG_FILE)
            WizardLogProgram.NONE -> Unit
        }
        if (networked && qrgFollow) add(WizardSwitch.QRG_FOLLOW)
        if (networked && clusterServer) add(WizardSwitch.CLUSTER_SERVER)
    }

    /** The equipment switches that are on once [writeGearTo] has run. */
    fun answeredGearSwitches(): Set<WizardSwitch> = buildSet {
        if (rotator) add(WizardSwitch.ROTATOR)
        if (airScout) add(WizardSwitch.AIRSCOUT)
    }

    private companion object {
        /**
         * The ports setStn_pstRotatorPort stores. One short of the full range, because
         * PSTRotator answers on port + 1; the setter replaces anything else by its factory
         * default, which would be the wizard inventing a number.
         */
        val ROTATOR_PORTS = 1..65534
    }
}
