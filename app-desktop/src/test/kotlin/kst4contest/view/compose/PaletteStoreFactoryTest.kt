package kst4contest.view.compose

import kst4contest.model.ChatPreferences
import kst4contest.model.OperatorProfile
import kst4contest.model.PaletteRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The factory binds the store to one profile's preferences and one profile's stylesheet.
 *
 * Two wires, and each of them is a place where one profile could end up reading or writing
 * another's colours. The stylesheet path is injected here rather than derived inside, so the
 * test does not have to write into the operator's home directory to check which file is read.
 */
class PaletteStoreFactoryTest(@TempDir private val tempDir: Path) {

    private val daylight = OperatorProfilePaletteFiles(
        dayFile = File("/nonexistent/day.css"),
        eveningFile = File("/nonexistent/evening.css"),
    )

    @Test
    fun aChangeLandsInTheDesignsOwnPreference() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))

        val store = PaletteStoreFactory.create(prefs, daylight)
        store.set(darkMode = false, role = PaletteRole.ACCENT, colour = "#FF0000")

        assertEquals("accent=#FF0000", prefs.guiOptions_paletteOverridesDay)
        assertTrue(prefs.guiOptions_paletteOverridesEvening.isEmpty())
    }

    @Test
    fun anEveningChangeLandsInTheEveningPreference() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))

        val store = PaletteStoreFactory.create(prefs, daylight)
        store.set(darkMode = true, role = PaletteRole.ACCENT, colour = "#FF0000")

        assertEquals("accent=#FF0000", prefs.guiOptions_paletteOverridesEvening)
        assertTrue(prefs.guiOptions_paletteOverridesDay.isEmpty())
    }

    @Test
    fun whatIsAlreadyStoredIsInForceFromTheStart() {
        // The store is built at startup; a colour saved last session has to be there at once.
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.guiOptions_paletteOverridesDay = "accent=#FF0000"

        val store = PaletteStoreFactory.create(prefs, daylight)

        assertEquals(PaletteSource.CHANGED, store.resolved(false).roles[PaletteRole.ACCENT]!!.source)
    }

    @Test
    fun aMissingStylesheetLeavesEveryRoleWithTheShippedSheet() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))

        val store = PaletteStoreFactory.create(prefs, daylight)

        store.resolved(false).roles.forEach { (role, resolved) ->
            assertEquals(PaletteSource.SHIPPED, resolved.source, "$role")
        }
    }

    @Test
    fun aStylesheetInTheProfileSuppliesTheRolesItNames() {
        val file = File.createTempFile("kst4contest-profile", ".css")
        try {
            file.writeText(".root { -fx-accent: #123456; }")

            val store = PaletteStoreFactory.create(
                ChatPreferences(tempDir.resolve("preferences.xml")),
                OperatorProfilePaletteFiles(dayFile = file, eveningFile = file),
            )

            assertEquals(
                PaletteSource.FILE,
                store.resolved(false).roles[PaletteRole.ACCENT]!!.source,
            )
            assertEquals(colourOf("#123456"), store.resolved(false).palette.accent)
        } finally {
            file.delete()
        }
    }

    @Test
    fun theFilesOfAProfileFollowOperatorProfilePaths() {
        /*
         * The one assertion that ties the factory to the project's path convention. A
         * different shape here would have one profile reading another profile's colours.
         */
        val other = OperatorProfile("OP2", "DN9APW", false, false)

        val files = OperatorProfilePaletteFiles.of(other)

        assertTrue(
            files.dayFile.toPath().endsWith(Path.of("profiles", "OP2", "KST4ContestDefaultDay.css")),
            "the daylight file is ${files.dayFile.path}",
        )
        assertTrue(
            files.eveningFile.toPath().endsWith(Path.of("profiles", "OP2", "KST4ContestDefaultEvening.css")),
            "the evening file is ${files.eveningFile.path}",
        )
    }
}
