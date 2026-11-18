package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * What the wizard does to a real preferences file: nothing until Save, exactly the answers
 * afterwards, and every setting it never asked about left as it was.
 */
class SetupWizardWriteTest(@param:TempDir private val directory: Path) {

    /** A copy of the bundled template, read the way the runtime reads a profile. */
    private fun templateFile(): Path {
        val file = directory.resolve("preferences.xml")
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), file, StandardCopyOption.REPLACE_EXISTING)
        }
        return file
    }

    private fun read(file: Path): ChatPreferences =
        ChatPreferences(file).also { assertTrue(it.readPreferencesFromXmlFile(), "reading $file failed") }

    /** A profile an operator set up: read the template, change it, write it back. */
    private fun configuredFile(change: ChatPreferences.() -> Unit = {}): Path {
        val file = templateFile()
        read(file).apply {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginNameMainCat("Philipp")
            setStn_loginNameSecondCat("Philipp")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            change()
            assertTrue(writePreferencesToXmlFile())
        }
        return file
    }

    private fun SetupWizardState.answerFirstInstallation() {
        name = "Philipp"
        next()
        callsign = "DL0TEST"
        password = "secret"
        next()
        locator = "JO50AA"
        setBand(WizardBand.B144, true)
        setBand(WizardBand.B1240, true)
        finish()
    }

    @Test
    fun nothingIsWrittenBeforeSave() {
        val file = templateFile()
        val prefs = read(file)
        // Taken after the read, so only the wizard is measured here.
        val before = Files.readAllBytes(file)

        SetupWizardState(prefs).answerFirstInstallation()

        assertArrayEquals(before, Files.readAllBytes(file), "the file changed without Save")
        assertEquals("DO5AMF", prefs.getStn_loginCallSign(), "the preferences changed without Save")
    }

    @Test
    fun saveWritesExactlyTheAnswers() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.answerFirstInstallation()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals("DL0TEST", restored.getStn_loginCallSign())
        assertEquals("secret", restored.getStn_loginPassword())
        assertEquals("Philipp", restored.getStn_loginNameMainCat())
        assertEquals("JO50AA", restored.getStn_loginLocatorMainCat())
        assertTrue(restored.isStn_bandActive144())
        assertTrue(restored.isStn_bandActive1240())
        assertFalse(restored.isStn_bandActive432(), "a band the operator did not tick is off")
        assertEquals(2, restored.getLoginChatCategoryMain().categoryNumber)
        assertFalse(restored.isLoginToSecondChatEnabled(), "a new station starts without a second chat")
    }

    @Test
    fun whatTheWizardNeverAskedAboutIsLeftAsItWas() {
        val file = templateFile()
        val prefs = read(file)
        val server = prefs.getStn_on4kstServersDns()
        val rotatorPort = prefs.getStn_pstRotatorPort()
        val beamWidth = prefs.getStn_antennaBeamWidthDeg()

        val state = SetupWizardState(prefs)
        state.answerFirstInstallation()
        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(server, restored.getStn_on4kstServersDns())
        assertEquals(rotatorPort, restored.getStn_pstRotatorPort())
        assertEquals(beamWidth, restored.getStn_antennaBeamWidthDeg())
    }

    @Test
    fun aCallsignIsStoredAsTyped() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.name = "Philipp"
        state.next()
        state.callsign = " do5amf-2 "
        state.password = "secret"
        state.next()
        state.locator = " jo50aa "
        state.setBand(WizardBand.B144, true)

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals("do5amf-2", restored.getStn_loginCallSign(), "only the surrounding blanks go")
        assertEquals("jo50aa", restored.getStn_loginLocatorMainCat(), "the Station tab stores it as typed too")
    }

    @Test
    fun aKeptCategoryOutsideTheSuggestionsSurvives() {
        val file = configuredFile {
            setLoginChatCategoryMain(ChatCategory(7))
            setLoginToSecondChatEnabled(true)
            setLoginChatCategorySecond(ChatCategory(9))
        }
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(7, restored.getLoginChatCategoryMain().categoryNumber)
        assertTrue(restored.isLoginToSecondChatEnabled())
        assertEquals(9, restored.getLoginChatCategorySecond().categoryNumber)
    }

    @Test
    fun theSecondNameFollowsTheFirstUnlessItWasSetApart() {
        val following = configuredFile()
        val prefs = read(following)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Marc"
        state.next()
        assertTrue(state.writeTo(prefs))
        assertEquals("Marc", read(following).getStn_loginNameSecondCat())

        val apart = configuredFile { setStn_loginNameSecondCat("Philipp SHF") }
        val apartPrefs = read(apart)
        val apartState = SetupWizardState(apartPrefs)
        apartState.chooseLoginSource(WizardSource.KEEP)
        apartState.chooseStationSource(WizardSource.KEEP)
        apartState.next()
        apartState.name = "Marc"
        apartState.next()
        assertTrue(apartState.writeTo(apartPrefs))
        assertEquals("Philipp SHF", read(apart).getStn_loginNameSecondCat(), "a name set apart is the operator's")
    }

    @Test
    fun anUntouchedLanguageStaysEvenIfThisBuildDoesNotKnowIt() {
        val file = configuredFile { setGuiOptions_language("fr") }
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()

        assertTrue(state.writeTo(prefs))

        assertEquals("fr", read(file).getGuiOptions_language())
    }

    @Test
    fun incompleteAnswersAreNotWritten() {
        val file = templateFile()
        val prefs = read(file)
        val before = Files.readAllBytes(file)
        val state = SetupWizardState(prefs)
        state.name = "Philipp"

        assertFalse(state.writeTo(prefs))

        assertArrayEquals(before, Files.readAllBytes(file))
        assertEquals("DO5AMF", prefs.getStn_loginCallSign())
    }

    @Test
    fun aFailedWriteIsReported() {
        val source = read(templateFile())
        // A directory where the file should be: the write cannot succeed.
        val unwritable = ChatPreferences(Files.createDirectory(directory.resolve("not-a-file")))
        val state = SetupWizardState(source)
        state.answerFirstInstallation()

        assertFalse(state.writeTo(unwritable), "a write that failed must not read as saved")
    }

    @Test
    fun aNewStationWithoutALogProgramHasEverySynchronisationOff() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.answerFirstInstallation()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertFalse(restored.isLogsynch_wintestNetworkListenerEnabled())
        assertFalse(restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertFalse(restored.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertFalse(restored.isTrxSynch_ucxLogUDPListenerEnabled())
        assertFalse(restored.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(restored.isNotify_dxClusterServerEnabled())
        assertFalse(restored.isStn_pstRotatorEnabled())
        assertFalse(restored.isAirScout_asUDPListenerEnabled())
    }

    @Test
    fun theAnswersOfTheNewPagesAreWritten() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()
        state.locator = "JO50AA"
        state.setBand(WizardBand.B144, true)
        state.next()
        state.antenna.beamWidthDeg = 28.0
        state.antenna.heightAslText = "612"
        state.next()
        state.integrations.chooseLogProgram(WizardLogProgram.WINTEST)
        state.integrations.wintestPort = 9871
        // Stated, not taken from the template: without a name the page could not be left.
        state.integrations.wintestStationName = "KST"
        state.integrations.qrgFollow = true
        state.next()
        assertEquals(WizardPage.GEAR, state.page)
        state.integrations.rotator = true
        state.integrations.rotatorHost = "127.0.0.1"
        state.integrations.rotatorPort = 12000

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(28.0, restored.getStn_antennaBeamWidthDeg())
        assertEquals(612.0, restored.getStn_pathAnalysisOwnHeightAslMeters())
        assertTrue(restored.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals(9871, restored.getLogsynch_wintestNetworkPort())
        assertTrue(restored.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertTrue(restored.isStn_pstRotatorEnabled())
        assertFalse(restored.isAirScout_asUDPListenerEnabled())
    }

    @Test
    fun aKeptStationKeepsEveryIntegrationAsItWas() {
        // The template's integrations, several log sources among them, under a real login.
        val file = configuredFile()
        val before = read(file)
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(before.isLogsynch_ucxUDPWkdCallListenerEnabled(), restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertEquals(
            before.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            restored.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
        )
        assertEquals(before.isTrxSynch_ucxLogUDPListenerEnabled(), restored.isTrxSynch_ucxLogUDPListenerEnabled())
        assertEquals(before.isNotify_dxClusterServerEnabled(), restored.isNotify_dxClusterServerEnabled())
        assertEquals(before.isAirScout_asUDPListenerEnabled(), restored.isAirScout_asUDPListenerEnabled())
        assertEquals(before.getStn_antennaBeamWidthDeg(), restored.getStn_antennaBeamWidthDeg())
        assertTrue(
            restored.isLogsynch_ucxUDPWkdCallListenerEnabled() && restored.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            "the template has two log sources on; if this fails the template changed and the test needs another example",
        )
    }

    @Test
    fun openingOneNewPageOfAKeptStationWritesOnlyThatPage() {
        val file = configuredFile()
        val before = read(file)
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        state.edit(WizardPage.GEAR)
        state.integrations.rotator = true
        state.integrations.rotatorHost = "127.0.0.1"
        state.integrations.rotatorPort = 12000
        state.next()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertTrue(restored.isStn_pstRotatorEnabled())
        assertEquals(
            before.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            restored.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            "the log page was not opened, so the log sources stay",
        )
        assertEquals(before.isNotify_dxClusterServerEnabled(), restored.isNotify_dxClusterServerEnabled())
    }
}
