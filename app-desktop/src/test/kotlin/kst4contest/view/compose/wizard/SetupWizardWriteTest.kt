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
class SetupWizardWriteTest(@TempDir private val directory: Path) {

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
        val udpListener = prefs.isLogsynch_ucxUDPWkdCallListenerEnabled()
        val clusterServer = prefs.isNotify_dxClusterServerEnabled()
        val airScout = prefs.isAirScout_asUDPListenerEnabled()
        val beamWidth = prefs.getStn_antennaBeamWidthDeg()

        val state = SetupWizardState(prefs)
        state.answerFirstInstallation()
        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(server, restored.getStn_on4kstServersDns())
        assertEquals(rotatorPort, restored.getStn_pstRotatorPort())
        assertEquals(udpListener, restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertEquals(clusterServer, restored.isNotify_dxClusterServerEnabled())
        assertEquals(airScout, restored.isAirScout_asUDPListenerEnabled())
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
}
