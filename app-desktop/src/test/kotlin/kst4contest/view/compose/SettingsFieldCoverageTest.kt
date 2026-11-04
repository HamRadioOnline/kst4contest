package kst4contest.view.compose

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Guards against a setting quietly disappearing during the port.
 *
 * The JavaFX settings window called these ChatPreferences setters. Every one of
 * them must still be called from the Compose settings sources, or an operator
 * loses a setting they could configure before — and nothing about that fails to
 * compile.
 *
 * The list was derived mechanically: the setters called between settingsStage and
 * the Profiles tab, intersected with the setters ChatPreferences actually declares,
 * so JavaFX control setters like setDisable or setPrefWidth are excluded.
 *
 * This checks text occurrence, not runtime behaviour. It cannot tell whether a
 * setter is called with the right value, only that it is called at all. That is
 * little, but it is the only check that covers all of them mechanically.
 */
class SettingsFieldCoverageTest {

    @Test
    fun `every setting the JavaFX window could write is still written`() {
        val expected = File("src/test/resources/settings-fields.txt")
            .readLines().map { it.trim() }.filter { it.isNotEmpty() }

        val composeDir = File("src/main/kotlin/kst4contest/view/compose")
        val composeSources = if (composeDir.isDirectory) {
            composeDir.walkTopDown().filter { it.extension == "kt" }
                .joinToString("\n") { it.readText() }
        } else {
            ""
        }

        val missing = expected.filterNot {
            composeSources.contains("$it(") || composeSources.contains("::$it")
        }

        assertTrue(missing.isEmpty(),
            "settings not written by any Compose source (${missing.size} of ${expected.size}):\n" +
                missing.joinToString("\n") { "  $it" })
    }
}
