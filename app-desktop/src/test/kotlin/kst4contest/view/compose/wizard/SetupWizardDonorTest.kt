package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Taking the login or the station from another profile: what is offered, what the answers
 * become, what the summary says against the own profile, and what reaches the file.
 */
class SetupWizardDonorTest(@param:TempDir private val directory: Path) {

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("setup-wizard-donor-test-unused.xml")).apply(configure)

    /** Every integration switch stated, so no test hangs on a compiled-in default. */
    private fun ChatPreferences.integrationsOff() {
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
    }

    /** An empty profile, as "new profile" creates it: no login, the template's integrations on. */
    private fun emptyProfile() = prefs {
        integrationsOff()
        setStn_loginCallSign("")
        setStn_loginPassword("")
        setStn_loginLocatorMainCat("JO54ME")
        setStn_bandActive144(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(true)
        setLoginChatCategorySecond(ChatCategory(3))
        setLogsynch_ucxUDPWkdCallListenerEnabled(true)
        setAirScout_asUDPListenerEnabled(true)
    }

    private fun contestStation() = WizardDonor("contest", "Conteststation", prefs {
        integrationsOff()
        setStn_loginCallSign("DL0TEST")
        setStn_loginPassword("secret")
        setStn_loginLocatorMainCat("JO50AA")
        setStn_bandActive144(true)
        setStn_bandActive432(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(false)
        setStn_antennaBeamWidthDeg(28.0)
        setStn_qtfDefault(270.0)
        setLogsynch_wintestNetworkListenerEnabled(true)
        setLogsynch_wintestNetworkPort(9871)
        setLogsynch_wintestNetworkStationNameOfKST("KST")
        setLogsynch_wintestQrgSyncEnabled(true)
        setStn_pstRotatorEnabled(true)
        setStn_pstRotatorHost("127.0.0.1")
        setStn_pstRotatorPort(12000)
    })

    private fun portable() = WizardDonor("portable", "Portabel", prefs {
        integrationsOff()
        setStn_loginCallSign("DL0TEST/P")
        setStn_loginPassword("secret")
        setStn_loginLocatorMainCat("JN58QA")
        setStn_bandActive1240(true)
        setLoginChatCategoryMain(ChatCategory(3))
        setLoginToSecondChatEnabled(false)
    })

    @Test
    fun withoutDonorsNothingChanges() {
        val state = SetupWizardState(emptyProfile())

        assertEquals(listOf(WizardSource.NEW), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.stationSources)
        assertTrue(state.donors.isEmpty())
    }

    @Test
    fun aDonorAddsTakeBetweenNewAndKeep() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))

        assertEquals(listOf(WizardSource.NEW, WizardSource.TAKE), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.TAKE, WizardSource.KEEP), state.stationSources)
        assertTrue(state.hasEntryPage)
        assertNull(state.loginSource, "with a choice nothing is preselected")
    }

    @Test
    fun aSingleDonorIsChosenWithTheSource() {
        val donor = contestStation()
        val state = SetupWizardState(emptyProfile(), listOf(donor))

        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.NEW)

        assertEquals(donor, state.loginDonor, "one donor needs no second question")
        assertTrue(state.canGoNext)
    }

    @Test
    fun severalDonorsNeedAChoice() {
        val contest = contestStation()
        val state = SetupWizardState(emptyProfile(), listOf(contest, portable()))

        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.NEW)
        assertNull(state.loginDonor)
        assertFalse(state.canGoNext, "which profile is not said yet")

        state.chooseLoginDonor(contest)
        assertTrue(state.canGoNext)
    }

    @Test
    fun aDonorThatIsNotOfferedIsNotAccepted() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.TAKE)
        val first = state.loginDonor

        state.chooseLoginDonor(portable())

        assertEquals(first, state.loginDonor)
    }

    @Test
    fun takingTheLoginSkipsItsPageAndLeavesItChangeable() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals("DL0TEST", state.callsign)
        assertEquals("secret", state.password)
        assertFalse(state.pages.contains(WizardPage.LOGIN))

        state.name = "Philipp"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)
        state.edit(WizardPage.LOGIN)
        state.callsign = "DL0TEST/P"
        state.next()
        assertEquals("DL0TEST/P", state.callsign, "a taken callsign gets its /P")
    }

    @Test
    fun takingTheStationLoadsEverythingThePagesShow() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()

        assertEquals("JO50AA", state.locator)
        assertEquals(setOf(WizardBand.B144, WizardBand.B432), state.bands)
        assertEquals(2, state.category)
        assertFalse(state.secondChat)
        assertEquals(28.0, state.antenna.beamWidthDeg)
        assertEquals(WizardLogProgram.WINTEST, state.integrations.logProgram)
        assertTrue(state.integrations.rotator)
        assertTrue(state.summaryQrgFollow)
        assertEquals(
            listOf(WizardPage.ENTRY, WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SUMMARY),
            state.pages,
            "a taken station shows no page of its own",
        )
    }

    @Test
    fun takingAStationNamesWhatTheOwnProfileLoses() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()

        // The own profile had the second chat, the network listener and AirScout on; the
        // donor has none of them. Win-Test and the rotator come new and are not "switched off".
        assertEquals(
            listOf(WizardSwitch.SECOND_CHAT, WizardSwitch.LOG_UDP, WizardSwitch.AIRSCOUT),
            state.switchedOff,
        )
    }

    @Test
    fun aTakenStationKeepsSeveralLogSources() {
        val several = WizardDonor("several", "Zwei Quellen", contestStation().prefs.apply {
            setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        })
        val state = SetupWizardState(emptyProfile(), listOf(several))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()

        assertFalse(state.logPageEditable, "the page knows one source and would switch the other off")
        state.edit(WizardPage.LOG)
        assertFalse(state.pages.contains(WizardPage.LOG))
    }

    @Test
    fun changingTheDonorLoadsItsValues() {
        val contest = contestStation()
        val other = portable()
        val state = SetupWizardState(emptyProfile(), listOf(contest, other))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.chooseStationDonor(contest)
        state.next()
        assertEquals("JO50AA", state.locator)

        state.back()
        state.chooseStationDonor(other)
        state.next()

        assertEquals("JN58QA", state.locator)
        assertEquals(setOf(WizardBand.B1240), state.bands)
        assertEquals(3, state.category)
        assertEquals(WizardLogProgram.NONE, state.integrations.logProgram, "nothing of the first donor is left")
        assertFalse(state.integrations.rotator)
    }

    @Test
    fun goingBackToNewDropsWhatWasTaken() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()
        state.back()
        state.chooseStationSource(WizardSource.NEW)
        state.next()

        assertEquals("", state.locator)
        assertTrue(state.bands.isEmpty())
        assertEquals(WizardLogProgram.NONE, state.integrations.logProgram)
        assertFalse(state.integrations.rotator)
        assertTrue(state.pages.contains(WizardPage.LOG))
    }

    // --- against real files -------------------------------------------------------------

    private fun file(name: String, configure: ChatPreferences.() -> Unit): Path {
        val path = directory.resolve(name)
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), path, StandardCopyOption.REPLACE_EXISTING)
        }
        ChatPreferences(path).apply {
            assertTrue(readPreferencesFromXmlFile())
            configure()
            assertTrue(writePreferencesToXmlFile())
        }
        return path
    }

    private fun read(path: Path) = ChatPreferences(path).also { assertTrue(it.readPreferencesFromXmlFile()) }

    @Test
    fun saveWritesTheWholeTakenStationAndTheTakenLoginIntoTheOwnFile() {
        val donorFile = file("donor.xml") {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            setStn_qtfDefault(270.0)
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
            setStn_pstRotator2Port(12020)
            setLogsynch_wintestNetworkListenerEnabled(true)
            setLogsynch_wintestNetworkStationNameOfWintestClient1("RUN")
        }
        val ownFile = file("own.xml") {
            setStn_loginCallSign("")
            setStn_loginPassword("")
            setStn_on4kstServersDns("own.example")
        }
        val donorBytes = Files.readAllBytes(donorFile)
        val own = read(ownFile)
        val state = SetupWizardState(own, listOf(WizardDonor("contest", "Conteststation", read(donorFile))))
        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()
        state.name = "Philipp"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        assertTrue(state.writeTo(own))

        val restored = read(ownFile)
        assertEquals("DL0TEST", restored.getStn_loginCallSign())
        assertEquals("secret", restored.getStn_loginPassword())
        assertEquals("Philipp", restored.getStn_loginNameMainCat())
        assertEquals("JO50AA", restored.getStn_loginLocatorMainCat())
        assertEquals(270.0, restored.getStn_qtfDefault(), "a station value the wizard never asks about")
        assertTrue(restored.isTrxSynch_so2rEnabled())
        assertTrue(restored.isStn_pstRotator2Enabled())
        assertEquals(12020, restored.getStn_pstRotator2Port())
        assertTrue(restored.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals("RUN", restored.getLogsynch_wintestNetworkStationNameOfWintestClient1())
        assertEquals("own.example", restored.getStn_on4kstServersDns(), "not part of the station")
        assertArrayEquals(donorBytes, Files.readAllBytes(donorFile), "the donor's file was touched")
    }

    @Test
    fun aPageOpenedAfterTakingOverridesTheCopy() {
        val donorFile = file("donor.xml") {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            setStn_pstRotatorEnabled(true)
            setStn_pstRotatorHost("127.0.0.1")
            setStn_pstRotatorPort(12000)
        }
        val ownFile = file("own.xml") {
            setStn_loginCallSign("")
            setStn_loginPassword("")
        }
        val own = read(ownFile)
        val state = SetupWizardState(own, listOf(WizardDonor("contest", "Conteststation", read(donorFile))))
        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()
        state.name = "Philipp"
        state.next()
        state.edit(WizardPage.GEAR)
        state.integrations.rotator = false
        state.next()
        state.edit(WizardPage.SITE)
        state.locator = "JN58QA"
        state.next()

        assertTrue(state.writeTo(own))

        val restored = read(ownFile)
        assertFalse(restored.isStn_pstRotatorEnabled(), "the operator switched the taken rotator off")
        assertEquals("JN58QA", restored.getStn_loginLocatorMainCat(), "the operator changed the taken locator")
    }

    @Test
    fun takingAStationNamesSo2rAndRotatorTwoWhenTheDonorDropsThem() {
        val own = emptyProfile().apply {
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
        }
        val donorOff = contestStation()
        val donorOn = WizardDonor("both-on", "Beide an", prefs {
            integrationsOff()
            setStn_loginCallSign("DL0BOTH")
            setStn_loginPassword("secret")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
        })

        val takeDrops = SetupWizardState(own, listOf(donorOff)).apply {
            chooseLoginSource(WizardSource.NEW)
            chooseStationSource(WizardSource.TAKE)
            next()
        }
        assertEquals(
            listOf(WizardSwitch.SO2R, WizardSwitch.ROTATOR_2),
            takeDrops.switchedOff.takeLast(2),
        )

        val keep = SetupWizardState(own, listOf(donorOff)).apply {
            chooseLoginSource(WizardSource.NEW)
            chooseStationSource(WizardSource.KEEP)
            next()
        }
        assertFalse(WizardSwitch.SO2R in keep.switchedOff)
        assertFalse(WizardSwitch.ROTATOR_2 in keep.switchedOff)

        val takeKeeps = SetupWizardState(own, listOf(donorOn)).apply {
            chooseLoginSource(WizardSource.NEW)
            chooseStationSource(WizardSource.TAKE)
            next()
        }
        assertFalse(WizardSwitch.SO2R in takeKeeps.switchedOff)
        assertFalse(WizardSwitch.ROTATOR_2 in takeKeeps.switchedOff)

        val ownRotator2WithoutSo2r = emptyProfile().apply {
            setTrxSynch_so2rEnabled(false)
            setStn_pstRotator2Enabled(true)
        }
        val noSo2r = SetupWizardState(ownRotator2WithoutSo2r, listOf(donorOff)).apply {
            chooseLoginSource(WizardSource.NEW)
            chooseStationSource(WizardSource.TAKE)
            next()
        }
        assertFalse(WizardSwitch.ROTATOR_2 in noSo2r.switchedOff)
        assertFalse(WizardSwitch.SO2R in noSo2r.switchedOff)
    }

    @Test
    fun theSummaryFallbackBandFollowsTheStationSource() {
        val own = emptyProfile().apply { setNotify_optionalFrequencyPrefix("144") }
        val donor = WizardDonor("uhf", "UHF", prefs {
            integrationsOff()
            setStn_loginCallSign("DL0UHF")
            setStn_loginPassword("secret")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive432(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            setNotify_optionalFrequencyPrefix("432")
        })

        val take = SetupWizardState(own, listOf(donor)).apply {
            chooseLoginSource(WizardSource.NEW)
            chooseStationSource(WizardSource.TAKE)
            next()
        }
        assertEquals("432", take.summaryFallbackBand)

        val keep = SetupWizardState(own, listOf(donor)).apply {
            chooseLoginSource(WizardSource.NEW)
            chooseStationSource(WizardSource.KEEP)
            next()
        }
        assertEquals("144", keep.summaryFallbackBand)

        val fresh = SetupWizardState(own, listOf(donor)).apply {
            chooseLoginSource(WizardSource.NEW)
            chooseStationSource(WizardSource.NEW)
            next()
        }
        assertEquals("144", fresh.summaryFallbackBand)
    }
}
