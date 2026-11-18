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
            listOf(WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SITE, WizardPage.SUMMARY),
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
    fun theSummarySaysWhatIsSwitchedOffAndWhatIsLeftAlone() {
        val fresh = SetupWizardState(freshInstallation())
        assertTrue(fresh.turnsOffSecondChat, "the template has the second chat on, a new station has it off")
        assertTrue(fresh.leavesIntegrationsUntouched)

        val kept = SetupWizardState(duplicatedProfile())
        kept.chooseStationSource(WizardSource.KEEP)
        kept.next()
        assertFalse(kept.turnsOffSecondChat)
        assertFalse(kept.leavesIntegrationsUntouched)
    }
}
