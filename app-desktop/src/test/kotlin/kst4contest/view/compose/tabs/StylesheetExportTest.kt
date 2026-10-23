package kst4contest.view.compose.tabs

import kst4contest.view.compose.JavaFxStylesheet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

/**
 * Writing the shipped stylesheet into the profile, so it can be edited by hand.
 *
 * The one rule with teeth here is that an existing file is never overwritten. The file this
 * writes to is the operator's own hand-edited work, and a button that silently replaced it
 * would destroy an afternoon's work on one misclick.
 */
class StylesheetExportTest {

    private fun tempDirectory(): File =
        Files.createTempDirectory("kst4contest-export").toFile()

    @Test
    fun aMissingFileIsWrittenFromTheShippedSheet() {
        val directory = tempDirectory()
        val target = File(directory, "KST4ContestDefaultDay.css")

        val outcome = exportShippedStylesheet(target, darkMode = false)

        assertTrue(outcome is StylesheetExportOutcome.Written, "outcome was $outcome")
        assertTrue(target.isFile, "nothing was written")
        assertEquals(
            JavaFxStylesheet::class.java.getResource("/KST4ContestDefaultDay.css")!!.readText(),
            target.readText(),
            "the written file is not the shipped sheet",
        )

        target.delete()
        directory.delete()
    }

    @Test
    fun theEveningDesignWritesTheEveningSheet() {
        val directory = tempDirectory()
        val target = File(directory, "KST4ContestDefaultEvening.css")

        exportShippedStylesheet(target, darkMode = true)

        assertEquals(
            JavaFxStylesheet::class.java.getResource("/KST4ContestDefaultEvening.css")!!.readText(),
            target.readText(),
        )

        target.delete()
        directory.delete()
    }

    @Test
    fun anExistingFileIsLeftAloneAndReported() {
        /*
         * The operator's hand-edited sheet. Overwriting it on a misclick is the one failure
         * this button could cause that cannot be undone from inside the application.
         */
        val directory = tempDirectory()
        val target = File(directory, "KST4ContestDefaultDay.css")
        target.writeText(".root { -fx-base: #112233; } /* mine */")

        val outcome = exportShippedStylesheet(target, darkMode = false)

        assertTrue(outcome is StylesheetExportOutcome.AlreadyThere, "outcome was $outcome")
        assertEquals(".root { -fx-base: #112233; } /* mine */", target.readText())

        target.delete()
        directory.delete()
    }

    @Test
    fun aMissingProfileDirectoryIsCreated() {
        // Only the root profile's directory exists already; every other profile's may not.
        val directory = tempDirectory()
        val target = File(directory, "profiles/OP2/KST4ContestDefaultDay.css")

        val outcome = exportShippedStylesheet(target, darkMode = false)

        assertTrue(outcome is StylesheetExportOutcome.Written, "outcome was $outcome")
        assertTrue(target.isFile, "the directory was not created")

        target.delete()
        target.parentFile.delete()
        target.parentFile.parentFile.delete()
        directory.delete()
    }

    @Test
    fun anUnwritablePlaceIsReportedRatherThanThrown() {
        val blockingFile = Files.createTempFile("kst4contest-export-blocked-parent", ".tmp").toFile()
        try {
            val target = File(blockingFile, "day.css")
            val outcome = exportShippedStylesheet(target, darkMode = false)

            assertTrue(outcome is StylesheetExportOutcome.Failed, "outcome was $outcome")
            assertNotNull((outcome as StylesheetExportOutcome.Failed).reason)
        } finally {
            blockingFile.delete()
        }
    }

    @Test
    fun whatIsWrittenCanBeReadBackAsRoles() {
        /*
         * The round trip the whole hand-editing route depends on: the file this writes has to
         * be a file the layering can read. The evening sheet is the one that states colours.
         */
        val directory = tempDirectory()
        val target = File(directory, "KST4ContestDefaultEvening.css")

        exportShippedStylesheet(target, darkMode = true)

        // All six: five stated outright, the window surface derived from the stated base.
        assertEquals(
            kst4contest.model.PaletteRole.values().toSet(),
            JavaFxStylesheet.declaredRolesOfFile(target).keys,
        )

        target.delete()
        directory.delete()
    }
}
