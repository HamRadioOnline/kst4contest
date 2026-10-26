package kst4contest.view.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumSet

/** Renders the compact control strips for visual inspection in both supported palettes. */
class CompactControlsVisualRenderingTest(@param:TempDir private val tempDir: Path) {

    @Test
    fun rendersDayAndEveningControlsAt100And125Percent() {
        val reportDirectory = Path.of("build", "reports", "compact-controls")
        Files.createDirectories(reportDirectory)

        listOf(false, true).forEach { darkMode ->
            listOf(1f, 1.25f).forEach { scale ->
                val bytes = render(darkMode, scale)
                val palette = if (darkMode) "evening" else "day"
                val scaling = if (scale == 1f) "100" else "125"
                Files.write(reportDirectory.resolve("$palette-$scaling.png"), bytes)
                assertTrue(bytes.size > 10_000, "$palette at $scaling% did not render")
            }
        }
    }

    private fun render(darkMode: Boolean, scale: Float): ByteArray {
        val preferences = ChatPreferences(tempDir.resolve("preferences-$darkMode-$scale.xml")).apply {
            getMYQRGFirstCat().set("1296.123.000")
            getMYQRGSecondCat().set("144.310.000")
            getActualQTF().set(360.0)
        }
        val chatInputState = ChatInputState(RenderActions(preferences)).apply {
            value = TextFieldValue("/cq OK1IN test test test aasdasd ggggg asd asd gggg")
        }
        val filters = StationFilterState()

        return onEventThread {
            val scene = ImageComposeScene(width = 1366, height = 380, density = Density(scale)) {
                Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = 12f) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            TimelineView(
                                state = TimelineState(),
                                onCandidateClicked = {},
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            StationFilterBar(
                                filters = filters,
                                activeBands = EnumSet.of(Band.B_144, Band.B_432, Band.B_1296),
                                reachability = ReachabilityState(
                                    EnumSet.of(Band.B_144, Band.B_432, Band.B_1296),
                                ),
                                onChanged = {},
                            )
                            Spacer(Modifier.height(12.dp))
                            ChatInputRow(
                                state = chatInputState,
                                prefs = preferences,
                                ownQrgMain = ObservedValue(preferences.getMYQRGFirstCat()),
                                ownQrgSecond = ObservedValue(preferences.getMYQRGSecondCat()),
                                antennaQtf = ObservedValue(preferences.getActualQTF()),
                                antennaQtf2 = ObservedValue(preferences.getActualQTF2()),
                                so2rEnabled = false,
                            )
                        }
                    }
                }
            }
            try {
                scene.render().encodeToData()!!.bytes
            } finally {
                scene.close()
            }
        }
    }

    private class RenderActions(private val preferences: ChatPreferences) : ChatInputActions {
        override fun panelSelection(): ChatMember? = null
        override fun tableSelection(): ChatMember? = null
        override fun scoreSelection(): ChatMember? = null
        override fun resolveVariables(template: String, member: ChatMember?): String = template
        override fun isAddressedToOwnCallsign(text: String): Boolean = false
        override fun resolveCategory(text: String, member: ChatMember?): ChatCategory? = null
        override fun mainCategory(): ChatCategory? = null
        override fun recordOutboundCq(text: String) = Unit
        override fun requestRecompute(reason: String) = Unit
        override fun queue(message: ChatMessage) = Unit
        override fun ownQrgMain(): String = preferences.getMYQRGFirstCat().get().orEmpty()
        override fun ownQrgSecond(): String = preferences.getMYQRGSecondCat().get().orEmpty()
    }
}
