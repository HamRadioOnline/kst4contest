package kst4contest.view.compose.wizard

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

/** When the wizard opens at all, decided without a window. */
class SetupWizardLauncherTest(@param:TempDir private val directory: Path) {

    private fun templateFile(): Path {
        val file = directory.resolve("preferences.xml")
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), file, StandardCopyOption.REPLACE_EXISTING)
        }
        return file
    }

    private fun configuredFile(): Path {
        val file = templateFile()
        ChatPreferences(file).apply {
            assertTrue(readPreferencesFromXmlFile())
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            assertTrue(writePreferencesToXmlFile())
        }
        return file
    }

    private val neverShown: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome =
        { _, _ -> throw AssertionError("the wizard must not open here") }

    @Test
    fun anUnconfiguredProfileOpensTheWizard() {
        var shown = 0

        val outcome = SetupWizardLauncher.run(ChatPreferences(templateFile()), forced = false) { _, _ ->
            shown++
            SetupWizardOutcome.LATER
        }

        assertEquals(1, shown)
        assertEquals(SetupWizardOutcome.LATER, outcome)
    }

    @Test
    fun aConfiguredProfileStartsWithoutTheWizard() {
        assertEquals(
            SetupWizardOutcome.NOT_SHOWN,
            SetupWizardLauncher.run(ChatPreferences(configuredFile()), forced = false, neverShown),
        )
    }

    @Test
    fun theMenuOpensItOnAConfiguredProfileToo() {
        val outcome = SetupWizardLauncher.run(ChatPreferences(configuredFile()), forced = true) { state, _ ->
            assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.loginSources)
            SetupWizardOutcome.SAVED
        }

        assertEquals(SetupWizardOutcome.SAVED, outcome)
    }

    @Test
    fun anUnreadableFileNeverOpensTheWizard() {
        val garbage = directory.resolve("broken.xml")
        Files.writeString(garbage, "this is not xml <<<")
        val before = Files.readAllBytes(garbage)

        // Forced as well: a wizard over a file it could not read would save over it.
        assertEquals(
            SetupWizardOutcome.NOT_SHOWN,
            SetupWizardLauncher.run(ChatPreferences(garbage), forced = true, neverShown),
        )
        assertArrayEquals(before, Files.readAllBytes(garbage), "the unreadable file was touched")
    }

    @Test
    fun aFileTheReaderThrowsOnNeverOpensTheWizard() {
        val file = templateFile()
        val original = Files.readString(file)
        assertTrue(original.contains("<ChatCategory>2</ChatCategory>"))
        Files.writeString(file, original.replace("<ChatCategory>2</ChatCategory>", "<ChatCategory>2.0</ChatCategory>"))
        val before = Files.readAllBytes(file)

        assertEquals(
            SetupWizardOutcome.NOT_SHOWN,
            SetupWizardLauncher.run(ChatPreferences(file), forced = true, neverShown),
        )
        assertArrayEquals(before, Files.readAllBytes(file), "the unreadable file was touched")
    }

    @Test
    fun aFailedSaveKeepsTheWizardOpen() {
        val source = ChatPreferences(templateFile()).apply { assertTrue(readPreferencesFromXmlFile()) }
        val state = SetupWizardState(source).apply {
            name = "Philipp"
            next()
            callsign = "DL0TEST"
            password = "secret"
            next()
            locator = "JO50AA"
            setBand(WizardBand.B144, true)
        }
        val unwritable = ChatPreferences(Files.createDirectory(directory.resolve("not-a-file")))

        assertNull(saveFromWizard(state, unwritable, connect = true), "a failed write must not close the wizard")
    }

    @Test
    fun aSaveReportsWhetherToConnect() {
        val file = templateFile()
        val prefs = ChatPreferences(file).apply { assertTrue(readPreferencesFromXmlFile()) }
        val state = SetupWizardState(prefs).apply {
            name = "Philipp"
            next()
            callsign = "DL0TEST"
            password = "secret"
            next()
            locator = "JO50AA"
            setBand(WizardBand.B144, true)
        }

        assertEquals(SetupWizardOutcome.SAVED, saveFromWizard(state, prefs, connect = false))
        assertEquals(SetupWizardOutcome.SAVED_AND_CONNECT, saveFromWizard(state, prefs, connect = true))
    }

    private fun profileFile(name: String, callsign: String, password: String): Path {
        val file = directory.resolve(name)
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), file, StandardCopyOption.REPLACE_EXISTING)
        }
        ChatPreferences(file).apply {
            assertTrue(readPreferencesFromXmlFile())
            setStn_loginCallSign(callsign)
            setStn_loginPassword(password)
            assertTrue(writePreferencesToXmlFile())
        }
        return file
    }

    @Test
    fun onlyConfiguredReadableProfilesBecomeDonors() {
        val configured = profileFile("configured.xml", "DL0TEST", "secret")
        val emptyLogin = profileFile("empty.xml", "", "")
        val template = profileFile("template.xml", "DO5AMF", "changeMe")
        val garbage = directory.resolve("garbage.xml").also { Files.writeString(it, "this is not xml <<<") }

        val donors = loadDonors(
            listOf(
                DonorCandidate("a", "Conteststation", configured),
                DonorCandidate("b", "Leer", emptyLogin),
                DonorCandidate("c", "Vorlage", template),
                DonorCandidate("d", "Kaputt", garbage),
                DonorCandidate("e", "Fehlt", directory.resolve("missing.xml")),
            )
        )

        assertEquals(listOf("a"), donors.map { it.profileId })
        assertEquals("Conteststation", donors.single().displayName)
        assertEquals("DL0TEST", donors.single().callsign)
    }

    @Test
    fun aMissingDonorFileIsNotCreated() {
        val missing = directory.resolve("missing.xml")

        loadDonors(listOf(DonorCandidate("e", "Fehlt", missing)))

        assertFalse(Files.exists(missing), "looking for a donor must not create its file")
    }

    @Test
    fun readingADonorLeavesItsFileAlone() {
        val configured = profileFile("configured.xml", "DL0TEST", "secret")
        val before = Files.readAllBytes(configured)

        loadDonors(listOf(DonorCandidate("a", "Conteststation", configured)))

        assertArrayEquals(before, Files.readAllBytes(configured))
    }

    @Test
    fun donorsAreOnlyLoadedWhenTheWizardOpens() {
        var loaded = 0
        val donors = { loaded++; emptyList<WizardDonor>() }

        SetupWizardLauncher.run(ChatPreferences(configuredFile()), forced = false, donors, neverShown)
        assertEquals(0, loaded, "a configured profile starts without reading other profiles")

        var seen: List<WizardDonor>? = null
        SetupWizardLauncher.run(ChatPreferences(templateFile()), forced = false, donors) { state, _ ->
            seen = state.donors
            SetupWizardOutcome.LATER
        }
        assertEquals(1, loaded)
        assertEquals(emptyList<WizardDonor>(), seen)
    }
}
