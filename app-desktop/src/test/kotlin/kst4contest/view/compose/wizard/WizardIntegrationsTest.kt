package kst4contest.view.compose.wizard

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * The answers about log program, rotator and AirScout: what a new station starts with, what
 * a kept one shows, when a page may be left, and what is written.
 */
class WizardIntegrationsTest {

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("wizard-integrations-test-unused.xml")).apply(configure)

    /** Everything off, so each test states what it switches on. */
    private fun allOff(configure: ChatPreferences.() -> Unit = {}) = prefs {
        setLogsynch_wintestNetworkListenerEnabled(false)
        setLogsynch_ucxUDPWkdCallListenerEnabled(false)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(false)
        setTrxSynch_ucxLogUDPListenerEnabled(false)
        setLogsynch_wintestQrgSyncEnabled(false)
        setNotify_dxClusterServerEnabled(false)
        setStn_pstRotatorEnabled(false)
        setAirScout_asUDPListenerEnabled(false)
        setTrxSynch_so2rEnabled(false)
        setStn_pstRotator2Enabled(false)
        setLogsynch_ucxUDPWkdCallListenerPort(12060)
        setLogsynch_wintestNetworkPort(9871)
        setLogsynch_wintestNetworkStationNameOfKST("KST")
        setStn_pstRotatorHost("127.0.0.1")
        setStn_pstRotatorPort(12000)
        setStn_pstRotator2Port(12010)
        setAirScout_asCommunicationPort(9872)
        setAirScout_asServerNameString("AS")
        setAirScout_asClientNameString("KST")
        configure()
    }

    /** What the bundled template switches on. */
    private fun likeTheTemplate() = allOff {
        setLogsynch_ucxUDPWkdCallListenerEnabled(true)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        setTrxSynch_ucxLogUDPListenerEnabled(true)
        setNotify_dxClusterServerEnabled(true)
        setAirScout_asUDPListenerEnabled(true)
    }

    private fun winTestStation() = allOff {
        setLogsynch_wintestNetworkListenerEnabled(true)
        setLogsynch_wintestNetworkPort(9999)
        setLogsynch_wintestQrgSyncEnabled(true)
        setNotify_dxClusterServerEnabled(true)
        setStn_pstRotatorEnabled(true)
    }

    @Test
    fun aKeptStationShowsWhatIsStored() {
        val answers = WizardIntegrations(winTestStation())

        assertEquals(WizardLogProgram.WINTEST, answers.logProgram)
        assertEquals(9999, answers.wintestPort)
        assertTrue(answers.qrgFollow)
        assertTrue(answers.clusterServer)
        assertTrue(answers.rotator)
        assertFalse(answers.airScout)
        assertFalse(answers.severalStoredLogSources)
    }

    @Test
    fun aNewStationStartsWithEverythingOffButKeepsTheNumbers() {
        val answers = WizardIntegrations(winTestStation())

        answers.loadFresh()

        assertEquals(WizardLogProgram.NONE, answers.logProgram)
        assertFalse(answers.qrgFollow)
        assertFalse(answers.clusterServer)
        assertFalse(answers.rotator)
        assertFalse(answers.airScout)
        assertEquals(9999, answers.wintestPort, "a port the operator once set is a proposal, not lost")
        assertTrue(answers.logComplete)
        assertTrue(answers.gearComplete)
    }

    @Test
    fun loadingTheStoredValuesAgainUndoesAFreshStart() {
        val answers = WizardIntegrations(winTestStation())
        answers.loadFresh()

        answers.loadStored()

        assertEquals(WizardLogProgram.WINTEST, answers.logProgram)
        assertTrue(answers.rotator)
    }

    @Test
    fun severalStoredSourcesMakeTheLogPageUneditable() {
        val answers = WizardIntegrations(likeTheTemplate())

        assertTrue(answers.severalStoredLogSources, "the template has the network listener and the file on")
        // Shown as one of them, the network one first; the state decides not to offer the page.
        assertEquals(WizardLogProgram.UDP, answers.logProgram)
    }

    @Test
    fun leavingANetworkProgramDropsWhatOnlyANetworkProgramCanDo() {
        val answers = WizardIntegrations(allOff())
        answers.chooseLogProgram(WizardLogProgram.UDP)
        answers.qrgFollow = true
        answers.clusterServer = true
        assertTrue(answers.networked)

        answers.chooseLogProgram(WizardLogProgram.FILE)

        assertFalse(answers.networked)
        assertFalse(answers.qrgFollow)
        assertFalse(answers.clusterServer)
    }

    @Test
    fun aLogFileNeedsAFile() {
        val answers = WizardIntegrations(allOff())
        // The compiled-in default names a file, which is a proposal like every stored value.
        answers.filePath = ""

        answers.chooseLogProgram(WizardLogProgram.FILE)
        assertFalse(answers.logComplete)

        answers.filePath = "/home/op/log.txt"
        assertTrue(answers.logComplete)
    }

    @Test
    fun anInvalidPortMakesItsPageIncomplete() {
        val answers = WizardIntegrations(allOff())

        answers.chooseLogProgram(WizardLogProgram.UDP)
        answers.udpPort = 0
        assertFalse(answers.logComplete)
        answers.udpPort = 70000
        assertFalse(answers.logComplete)
        answers.udpPort = 12060
        assertTrue(answers.logComplete)

        answers.chooseLogProgram(WizardLogProgram.WINTEST)
        answers.wintestStationName = "  "
        assertFalse(answers.logComplete, "Win-Test needs a station name to send from")
        answers.wintestStationName = "KST"
        answers.wintestPort = -1
        assertFalse(answers.logComplete)

        // A port of a program that is not chosen is nobody's problem.
        answers.chooseLogProgram(WizardLogProgram.NONE)
        assertTrue(answers.logComplete)

        answers.airScout = true
        answers.airScoutPort = 0
        assertFalse(answers.gearComplete)
        answers.airScoutPort = 9872
        answers.airScoutServerName = "A\"S"
        assertFalse(answers.gearComplete, "a quotation mark breaks the AirScout message")
        answers.airScoutServerName = "AS"
        assertTrue(answers.gearComplete)

        answers.rotator = true
        answers.rotatorHost = " "
        assertFalse(answers.gearComplete)
    }

    @Test
    fun aRotatorPortNextToRotatorTwoIsRefusedOnlyWithSo2r() {
        val withSo2r = WizardIntegrations(allOff {
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
        })
        withSo2r.rotator = true
        withSo2r.rotatorPort = 12011
        assertTrue(withSo2r.rotatorPortClashes, "PSTRotator answers on port + 1")
        assertFalse(withSo2r.gearComplete)
        withSo2r.rotatorPort = 12000
        assertFalse(withSo2r.rotatorPortClashes)
        assertTrue(withSo2r.gearComplete)

        val withoutSo2r = WizardIntegrations(allOff())
        withoutSo2r.rotator = true
        withoutSo2r.rotatorPort = 12011
        assertFalse(withoutSo2r.rotatorPortClashes, "rotator 2 is not in use without SO2R")
        assertTrue(withoutSo2r.gearComplete)
    }

    @Test
    fun writingTheLogPageSwitchesExactlyTheChosenSourceOn() {
        val target = likeTheTemplate()
        val answers = WizardIntegrations(target)
        answers.loadFresh()
        answers.chooseLogProgram(WizardLogProgram.WINTEST)
        answers.wintestPort = 9871
        answers.wintestStationName = " KST "
        answers.qrgFollow = true

        answers.writeLogTo(target)

        assertTrue(target.isLogsynch_wintestNetworkListenerEnabled())
        assertFalse(target.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertFalse(target.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertEquals(9871, target.getLogsynch_wintestNetworkPort())
        assertEquals("KST", target.getLogsynch_wintestNetworkStationNameOfKST(), "trimmed, as the tab stores it")
        assertTrue(target.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(target.isTrxSynch_ucxLogUDPListenerEnabled(), "the other program's frequency source goes off")
        assertFalse(target.isNotify_dxClusterServerEnabled())
        assertTrue(target.isAirScout_asUDPListenerEnabled(), "the log page does not touch AirScout")
    }

    @Test
    fun writingNoLogProgramSwitchesEverySynchronisationOff() {
        val target = likeTheTemplate()
        val answers = WizardIntegrations(target)
        answers.loadFresh()

        answers.writeLogTo(target)

        assertFalse(target.isLogsynch_wintestNetworkListenerEnabled())
        assertFalse(target.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertFalse(target.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertFalse(target.isTrxSynch_ucxLogUDPListenerEnabled())
        assertFalse(target.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(target.isNotify_dxClusterServerEnabled())
        assertEquals(12060, target.getLogsynch_ucxUDPWkdCallListenerPort(), "an unused port is left alone")
    }

    @Test
    fun writingTheGearPageSetsRotatorAndAirScoutOnly() {
        val target = likeTheTemplate()
        val answers = WizardIntegrations(target)
        answers.loadFresh()
        answers.rotator = true
        answers.rotatorHost = " 192.168.1.20 "
        answers.rotatorPort = 12040

        answers.writeGearTo(target)

        assertTrue(target.isStn_pstRotatorEnabled())
        assertEquals("192.168.1.20", target.getStn_pstRotatorHost())
        assertEquals(12040, target.getStn_pstRotatorPort())
        assertFalse(target.isAirScout_asUDPListenerEnabled())
        assertEquals(9872, target.getAirScout_asCommunicationPort(), "an unused port is left alone")
        assertTrue(target.isLogsynch_ucxUDPWkdCallListenerEnabled(), "the gear page does not touch the log sources")
    }

    @Test
    fun theSwitchedOffListNamesWhatWasOnAndWillBeOff() {
        val answers = WizardIntegrations(likeTheTemplate())
        answers.loadFresh()

        assertEquals(
            listOf(WizardSwitch.LOG_UDP, WizardSwitch.LOG_FILE, WizardSwitch.QRG_FOLLOW, WizardSwitch.CLUSTER_SERVER),
            answers.switchedOffByLog(),
        )
        assertEquals(listOf(WizardSwitch.AIRSCOUT), answers.switchedOffByGear())

        answers.chooseLogProgram(WizardLogProgram.UDP)
        answers.qrgFollow = true
        answers.airScout = true
        assertEquals(listOf(WizardSwitch.LOG_FILE, WizardSwitch.CLUSTER_SERVER), answers.switchedOffByLog())
        assertEquals(emptyList<WizardSwitch>(), answers.switchedOffByGear())
    }

    @Test
    fun nothingIsSwitchedOffOnAStationThatHadNothingOn() {
        val answers = WizardIntegrations(allOff())
        answers.loadFresh()

        assertEquals(emptyList<WizardSwitch>(), answers.switchedOffByLog())
        assertEquals(emptyList<WizardSwitch>(), answers.switchedOffByGear())
    }
}
