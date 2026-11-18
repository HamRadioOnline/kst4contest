package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * The wizard's rules without a window: which pages appear, what they start with, and when
 * the operator may go on. Nothing here reads or writes a file.
 */
class SetupWizardStateTest {

    /** Preferences that never touch a file: the path is only read by read/write calls. */
    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("setup-wizard-state-test-unused.xml")).apply(configure)

    private fun freshInstallation() = prefs {
        setStn_loginCallSign("DO5AMF")
        setStn_loginPassword("changeMe")
        setStn_loginNameMainCat("KST4Contest1263")
        setStn_loginLocatorMainCat("JO54ME")
        setStn_bandActive144(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(true)
        setLoginChatCategorySecond(ChatCategory(3))
    }

    private fun duplicatedProfile() = prefs {
        setStn_loginCallSign("")
        setStn_loginPassword("")
        setStn_loginNameMainCat("Other Operator")
        setStn_loginLocatorMainCat("JO50AA")
        setStn_bandActive144(true)
        setStn_bandActive432(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(false)
    }

    private fun configuredProfile() = duplicatedProfile().apply {
        setStn_loginCallSign("DL0TEST")
        setStn_loginPassword("secret")
        setStn_loginNameMainCat("Philipp")
    }

    /** Fills every mandatory answer of a first installation, leaving the wizard on SITE. */
    private fun SetupWizardState.fillFirstInstallation() {
        name = "Philipp"
        next()
        callsign = "DL0TEST"
        password = "secret"
        next()
        locator = "JO50AA"
        setBand(WizardBand.B144, true)
    }

    @Test
    fun aFreshInstallationHasNoEntryPageAndStartsEmpty() {
        val state = SetupWizardState(freshInstallation())

        assertFalse(state.hasEntryPage)
        assertEquals(WizardPage.PERSONAL, state.page)
        assertEquals(
            listOf(
                WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SITE,
                WizardPage.ANTENNA, WizardPage.LOG, WizardPage.GEAR, WizardPage.SUMMARY,
            ),
            state.pages,
        )
        assertEquals("", state.name, "the template's name is nobody's name")
        assertEquals("", state.callsign)
        assertEquals("", state.locator)
        assertTrue(state.bands.isEmpty())
        assertNull(state.category)
        assertFalse(state.secondChat)
    }

    @Test
    fun aDuplicatedProfileIsAskedOnlyAboutTheStation() {
        val state = SetupWizardState(duplicatedProfile())

        assertEquals(listOf(WizardSource.NEW), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.stationSources)
        assertTrue(state.hasEntryPage)
        assertEquals(WizardPage.ENTRY, state.page)
        assertEquals(WizardSource.NEW, state.loginSource, "a single option is chosen already")
        assertNull(state.stationSource, "nothing is preselected where there is a choice")
        assertFalse(state.canGoNext)
    }

    @Test
    fun aConfiguredProfileMayKeepBoth() {
        val state = SetupWizardState(configuredProfile())

        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.stationSources)
        assertEquals("Philipp", state.name, "a configured profile keeps its operator's name")
    }

    @Test
    fun keepingTheStationSkipsItsStepAndCarriesTheStoredValues() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals(
            listOf(WizardPage.ENTRY, WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SUMMARY),
            state.pages,
        )
        assertEquals("JO50AA", state.locator)
        assertEquals(setOf(WizardBand.B144, WizardBand.B432), state.bands)
        assertEquals(2, state.category)
    }

    @Test
    fun aNewStationStartsEmptyEvenOnAProfileThatHasOne() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.NEW)
        state.next()

        assertEquals("", state.locator)
        assertTrue(state.bands.isEmpty())
        assertNull(state.category)
        assertTrue(state.pages.contains(WizardPage.SITE))
    }

    @Test
    fun goingBackToTheEntryPageAndOnAgainKeepsWhatWasTyped() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.NEW)
        state.next()
        state.name = "Philipp"
        state.back()
        state.next()

        assertEquals("Philipp", state.name)

        state.next()
        state.callsign = "DL0TEST"
        state.back()
        state.back()
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals("JO50AA", state.locator, "a changed source loads its own values")
        state.next()
        assertEquals("DL0TEST", state.callsign, "the login source did not change")
    }

    @Test
    fun callsignsWithSuffixAndLowerCaseAreAccepted() {
        listOf("DL0TEST", "DL0TEST/P", "DO5AMF-2", "do5amf", " DL0TEST ").forEach {
            assertTrue(SetupWizardState.isPlausibleCallsign(it), "'$it' must be accepted")
        }
        listOf("", "DL", "ABC", "123", "DL0 TEST", "DL0TEST|X", "DL0-TEST-2", "DL0TEST/").forEach {
            assertFalse(SetupWizardState.isPlausibleCallsign(it), "'$it' must be refused")
        }
    }

    @Test
    fun onlyASixCharacterLocatorIsAccepted() {
        listOf("JO50AA", "jo50aa", " JN49FL ").forEach {
            assertTrue(SetupWizardState.isLocator(it), "'$it' must be accepted")
        }
        listOf("", "JO50", "JO50AAA", "SS50AA", "JO50YY", "JO5OAA").forEach {
            assertFalse(SetupWizardState.isLocator(it), "'$it' must be refused")
        }
    }

    @Test
    fun theCategoryIsSuggestedFromTheLowestBand() {
        assertNull(SetupWizardState.suggestCategory(emptySet()))
        assertEquals(1, SetupWizardState.suggestCategory(setOf(WizardBand.B50, WizardBand.B144)))
        assertEquals(1, SetupWizardState.suggestCategory(setOf(WizardBand.B70)))
        assertEquals(2, SetupWizardState.suggestCategory(setOf(WizardBand.B144, WizardBand.B1240)))
        assertEquals(2, SetupWizardState.suggestCategory(setOf(WizardBand.B432)))
        assertEquals(3, SetupWizardState.suggestCategory(setOf(WizardBand.B1240, WizardBand.B10G)))
    }

    @Test
    fun theSuggestionFollowsTheBandsUntilTheOperatorChooses() {
        val state = SetupWizardState(freshInstallation())

        state.setBand(WizardBand.B1240, true)
        assertEquals(3, state.category)

        state.setBand(WizardBand.B144, true)
        assertEquals(2, state.category)

        state.chooseCategory(3)
        state.setBand(WizardBand.B50, true)
        assertEquals(3, state.category, "an operator's choice is not overwritten")
        assertEquals(1, state.suggestedCategory, "the suggestion is still shown")
    }

    @Test
    fun aSecondChatNeedsADifferentCategory() {
        val state = SetupWizardState(freshInstallation())
        state.fillFirstInstallation()
        assertTrue(state.canGoNext)

        state.chooseSecondChat(true)
        assertFalse(state.canGoNext, "the second chat has no category yet")
        assertFalse(state.secondCategoryOptions.contains(2), "the main category is not offered twice")

        state.secondCategory = 3
        assertTrue(state.canGoNext)

        state.chooseCategory(3)
        assertNull(state.secondCategory, "the same chat cannot be opened twice")
        assertFalse(state.canGoNext)
    }

    @Test
    fun finishNeedsEveryMandatoryAnswer() {
        val state = SetupWizardState(freshInstallation())

        state.name = "Philipp"
        assertFalse(state.canFinish)
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        assertFalse(state.canFinish, "a new station still lacks its locator")
        state.next()
        state.locator = "JO50AA"
        assertFalse(state.canFinish, "no band yet")
        state.setBand(WizardBand.B144, true)
        assertTrue(state.canFinish)

        state.finish()
        assertEquals(WizardPage.SUMMARY, state.page)
        assertTrue(state.mandatoryComplete)
        assertFalse(state.canFinish, "the summary has Save instead")
    }

    @Test
    fun aKeptStationLetsTheOperatorFinishAfterTheLogin() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST/P"
        state.password = "secret"

        assertTrue(state.canFinish)
    }

    @Test
    fun aRefusedEntryIsOnlyShownOnceSomethingWasTyped() {
        val state = SetupWizardState(freshInstallation())

        assertFalse(state.callsignRefused, "an empty field is not an error yet")
        state.callsign = "DL"
        assertTrue(state.callsignRefused)
        state.callsign = "DL0TEST"
        assertFalse(state.callsignRefused)

        assertFalse(state.locatorRefused)
        state.locator = "JO50"
        assertTrue(state.locatorRefused)
    }

    @Test
    fun changeFromTheSummaryOpensAStepThatWasSkipped() {
        val state = SetupWizardState(configuredProfile())
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        state.edit(WizardPage.LOGIN)

        assertEquals(WizardPage.LOGIN, state.page)
        assertEquals("DL0TEST", state.callsign, "the kept value is there to be changed")
        state.callsign = "DL0TEST/P"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)
    }

    @Test
    fun keepingAStationWithMissingLocatorKeepsTheSitePageInTheCourse() {
        val state = SetupWizardState(duplicatedProfile().apply { setStn_loginLocatorMainCat("") })
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertTrue(state.pages.contains(WizardPage.SITE))
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()

        assertEquals(WizardPage.SITE, state.page)
        assertEquals(setOf(WizardBand.B144, WizardBand.B432), state.bands)
        assertEquals(2, state.category)
    }

    @Test
    fun keepingALoginWithMissingPasswordKeepsTheLoginPageInTheCourse() {
        val state = SetupWizardState(configuredProfile().apply { setStn_loginPassword("") })
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertTrue(state.pages.contains(WizardPage.LOGIN))
        assertEquals("DL0TEST", state.callsign)
        assertEquals("", state.password)
        assertFalse(state.mandatoryComplete)
    }

    @Test
    fun keepingAnUnknownCategoryIsIncompleteAndKeepsTheSitePage() {
        val state = SetupWizardState(configuredProfile().apply { setLoginChatCategoryMain(ChatCategory(13)) })
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertFalse(state.mandatoryComplete)
        assertTrue(state.pages.contains(WizardPage.SITE))
    }

    @Test
    fun keepingValidCategoriesOutsideTheBandSuggestionsCompletesAndSkipsSite() {
        val state = SetupWizardState(
            configuredProfile().apply {
                setLoginChatCategoryMain(ChatCategory(7))
                setLoginToSecondChatEnabled(true)
                setLoginChatCategorySecond(ChatCategory(9))
            },
        )
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertTrue(state.mandatoryComplete)
        assertFalse(state.pages.contains(WizardPage.SITE))
    }

    @Test
    fun storablePasswordsAndNamesRejectDelimitersAndOuterWhitespace() {
        listOf("secret", "se cret").forEach {
            assertTrue(SetupWizardState.isStorablePassword(it), "'$it' must be accepted")
        }
        listOf("", "   ", " secret", "secret ", "se|cret", "se\ncret", "se\rcret").forEach {
            assertFalse(SetupWizardState.isStorablePassword(it), "'$it' must be refused")
        }

        listOf("Philipp", "Philipp W").forEach {
            assertTrue(SetupWizardState.isStorableName(it), "'$it' must be accepted")
        }
        listOf("", "   ", "Phil|ipp", "Phil\nipp", "Phil\ripp").forEach {
            assertFalse(SetupWizardState.isStorableName(it), "'$it' must be refused")
        }
    }

    @Test
    fun aRefusedPasswordOrNameIsReportedAndBlocksCompletion() {
        val state = SetupWizardState(freshInstallation())
        assertFalse(state.nameRefused)
        state.name = "   "
        assertFalse(state.nameRefused)
        assertFalse(state.canGoNext)
        state.name = "Phil|ipp"
        assertTrue(state.nameRefused)
        assertFalse(state.canGoNext)

        state.fillFirstInstallation()
        state.back()
        assertEquals(WizardPage.LOGIN, state.page)
        assertFalse(state.passwordRefused)

        listOf(" secret", "secret ", "   ", "se|cret", "se\ncret").forEach { refused ->
            state.password = refused
            assertTrue(state.passwordRefused, "'$refused' must set passwordRefused")
            assertFalse(state.canGoNext, "'$refused' must block Next on login")
            assertFalse(state.mandatoryComplete, "'$refused' must block mandatoryComplete")
        }

        state.password = ""
        assertFalse(state.passwordRefused)
        assertFalse(state.canGoNext)

        state.password = "se cret"
        assertFalse(state.passwordRefused)
        assertTrue(state.canGoNext)
        assertTrue(state.mandatoryComplete)
    }

    @Test
    fun keepingALoginWithAStorablePasswordSkipsTheLoginPage() {
        val state = SetupWizardState(configuredProfile())
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertFalse(state.pages.contains(WizardPage.LOGIN))
        assertEquals(WizardPage.PERSONAL, state.page)
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)
    }

    /** A first installation whose stored integrations are those of the bundled template. */
    private fun freshInstallationLikeTheTemplate() = freshInstallation().apply {
        setLogsynch_wintestNetworkListenerEnabled(false)
        setLogsynch_ucxUDPWkdCallListenerEnabled(true)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        setTrxSynch_ucxLogUDPListenerEnabled(true)
        setLogsynch_wintestQrgSyncEnabled(false)
        setNotify_dxClusterServerEnabled(true)
        setStn_pstRotatorEnabled(false)
        setAirScout_asUDPListenerEnabled(true)
        setLogsynch_ucxUDPWkdCallListenerPort(12060)
    }

    /** A duplicated profile at a Win-Test station with a rotator. */
    private fun duplicatedWinTestStation() = duplicatedProfile().apply {
        setLogsynch_wintestNetworkListenerEnabled(true)
        setLogsynch_wintestNetworkPort(9871)
        setLogsynch_wintestNetworkStationNameOfKST("KST")
        setLogsynch_ucxUDPWkdCallListenerEnabled(false)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(false)
        setLogsynch_wintestQrgSyncEnabled(true)
        setTrxSynch_ucxLogUDPListenerEnabled(false)
        setNotify_dxClusterServerEnabled(false)
        setStn_pstRotatorEnabled(true)
        setStn_pstRotatorHost("127.0.0.1")
        setStn_pstRotatorPort(12000)
        setAirScout_asUDPListenerEnabled(false)
        setTrxSynch_so2rEnabled(false)
    }

    @Test
    fun aNewStationStartsWithEveryIntegrationOff() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())

        assertEquals(WizardLogProgram.NONE, state.integrations.logProgram)
        assertFalse(state.integrations.qrgFollow)
        assertFalse(state.integrations.clusterServer)
        assertFalse(state.integrations.rotator)
        assertFalse(state.integrations.airScout)
        assertEquals(
            listOf(
                WizardSwitch.SECOND_CHAT, WizardSwitch.LOG_UDP, WizardSwitch.LOG_FILE,
                WizardSwitch.QRG_FOLLOW, WizardSwitch.CLUSTER_SERVER, WizardSwitch.AIRSCOUT,
            ),
            state.switchedOff,
            "the summary names everything the template had on",
        )
    }

    @Test
    fun theNewPagesComeAfterTheLocationAndCanBeWalkedThrough() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())
        state.fillFirstInstallation()

        state.next()
        assertEquals(WizardPage.ANTENNA, state.page)
        assertTrue(state.canGoNext, "the antenna values are proposals and valid as they stand")
        state.next()
        assertEquals(WizardPage.LOG, state.page)
        state.next()
        assertEquals(WizardPage.GEAR, state.page)
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        state.back()
        assertEquals(WizardPage.GEAR, state.page)
    }

    @Test
    fun finishingEarlyStillSwitchesANewStationsIntegrationsOff() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())
        state.fillFirstInstallation()
        assertTrue(state.canFinish)

        state.finish()

        assertEquals(WizardPage.SUMMARY, state.page)
        assertTrue(state.switchedOff.contains(WizardSwitch.LOG_UDP), "a skipped page of a new station still applies")
        assertTrue(state.switchedOff.contains(WizardSwitch.AIRSCOUT))
    }

    @Test
    fun aKeptStationSkipsTheNewPagesAndSwitchesNothingOff() {
        val state = SetupWizardState(duplicatedWinTestStation())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals(
            listOf(WizardPage.ENTRY, WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SUMMARY),
            state.pages,
        )
        assertEquals(WizardLogProgram.WINTEST, state.integrations.logProgram, "the summary shows what is stored")
        assertTrue(state.integrations.rotator)
        assertEquals(emptyList<WizardSwitch>(), state.switchedOff)
        assertTrue(state.logPageEditable)
    }

    @Test
    fun changeFromTheSummaryOpensANewPageOfAKeptStation() {
        val state = SetupWizardState(duplicatedWinTestStation())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        state.edit(WizardPage.GEAR)
        assertEquals(WizardPage.GEAR, state.page)
        state.integrations.rotator = false
        state.next()

        assertEquals(WizardPage.SUMMARY, state.page)
        assertEquals(listOf(WizardSwitch.ROTATOR), state.switchedOff)
    }

    @Test
    fun aKeptStationWithSeveralLogSourcesCannotOpenTheLogPage() {
        val state = SetupWizardState(duplicatedProfile().apply {
            setLogsynch_wintestNetworkListenerEnabled(false)
            setLogsynch_ucxUDPWkdCallListenerEnabled(true)
            setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        })
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()

        assertFalse(state.logPageEditable, "the page knows one source and would switch the other off")
        state.edit(WizardPage.LOG)
        assertEquals(WizardPage.SUMMARY, state.page, "the log page must stay shut")

        state.edit(WizardPage.GEAR)
        assertEquals(WizardPage.GEAR, state.page, "the other pages are not affected")
    }

    @Test
    fun anInvalidOptionalPageBlocksSaving() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())
        state.fillFirstInstallation()
        state.next()
        state.next()
        assertEquals(WizardPage.LOG, state.page)

        state.integrations.chooseLogProgram(WizardLogProgram.UDP)
        state.integrations.udpPort = 0

        assertFalse(state.canGoNext)
        assertFalse(state.canFinish, "a summary that cannot be saved helps nobody")
        assertFalse(state.mandatoryComplete)

        state.integrations.udpPort = 12060
        assertTrue(state.canGoNext)
        assertTrue(state.mandatoryComplete)
    }

    @Test
    fun antennaValuesTypedForANewStationAreGoneWhenTheStationIsKeptAfterAll() {
        val profile = duplicatedWinTestStation().apply { setStn_antennaBeamWidthDeg(28.0) }
        val state = SetupWizardState(profile)
        state.chooseStationSource(WizardSource.NEW)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()
        state.locator = "JO50AA"
        state.setBand(WizardBand.B144, true)
        state.next()
        assertEquals(WizardPage.ANTENNA, state.page)
        state.antenna.beamWidthDeg = 99.0

        repeat(4) { state.back() }
        assertEquals(WizardPage.ENTRY, state.page)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertFalse(state.pages.contains(WizardPage.ANTENNA), "a kept station does not write the antenna page")
        assertEquals(
            28.0,
            state.antenna.beamWidthDeg,
            "the summary must not show a value that Save will not write",
        )
    }

    @Test
    fun theSummaryOfAKeptStationShowsWhatSaveLeavesOn() {
        // No log source, but the DX cluster server and a frequency source are on.
        val state = SetupWizardState(duplicatedWinTestStation().apply {
            setLogsynch_wintestNetworkListenerEnabled(false)
            setLogsynch_wintestQrgSyncEnabled(true)
            setNotify_dxClusterServerEnabled(true)
        })
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        assertTrue(state.summaryClusterServer, "Save leaves it on, so the summary must not say no")
        assertTrue(state.summaryQrgFollow)
        assertFalse(state.switchedOff.contains(WizardSwitch.CLUSTER_SERVER))

        // Once the log page is part of the run its answers count, and what they drop is named.
        state.edit(WizardPage.LOG)
        state.next()

        assertFalse(state.summaryClusterServer)
        assertFalse(state.summaryQrgFollow)
        assertTrue(state.switchedOff.containsAll(listOf(WizardSwitch.QRG_FOLLOW, WizardSwitch.CLUSTER_SERVER)))
    }
}
