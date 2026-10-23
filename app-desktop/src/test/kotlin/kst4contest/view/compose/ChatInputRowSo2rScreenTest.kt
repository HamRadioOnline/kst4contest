package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.math.abs

/** The send line with SO2R: the display-only QTF2 field appears only while SO2R is on. */
@OptIn(ExperimentalTestApi::class)
class ChatInputRowSo2rScreenTest {

    @TempDir
    lateinit var tempDir: Path

    private fun prefs() = ChatPreferences(tempDir.resolve("preferences.xml")).apply {
        getActualQTF().set(145.0)
        getActualQTF2().set(270.0)
    }

    private fun chatInputStateFor(prefs: ChatPreferences) = ChatInputState(object : ChatInputActions {
        override fun panelSelection(): ChatMember? = null
        override fun tableSelection(): ChatMember? = null
        override fun scoreSelection(): ChatMember? = null
        override fun resolveVariables(template: String, member: ChatMember?): String? = template
        override fun isAddressedToOwnCallsign(text: String): Boolean = false
        override fun resolveCategory(text: String, member: ChatMember?): ChatCategory? = null
        override fun mainCategory(): ChatCategory? = null
        override fun recordOutboundCq(text: String) {}
        override fun requestRecompute(reason: String) {}
        override fun queue(message: ChatMessage) {}
        override fun ownQrgMain(): String = prefs.getMYQRGFirstCat().get().orEmpty()
        override fun ownQrgSecond(): String = prefs.getMYQRGSecondCat().get().orEmpty()
    })

    @Test
    fun `QTF2 is shown only with SO2R and cannot be edited`() = runComposeUiTest {
        val prefs = prefs()
        var so2r by mutableStateOf(false)
        setContent {
            ChatInputRow(
                state = chatInputStateFor(prefs),
                prefs = prefs,
                ownQrgMain = ObservedValue(prefs.getMYQRGFirstCat()),
                ownQrgSecond = ObservedValue(prefs.getMYQRGSecondCat()),
                antennaQtf = ObservedValue(prefs.getActualQTF()),
                antennaQtf2 = ObservedValue(prefs.getActualQTF2()),
                so2rEnabled = so2r,
            )
        }

        onAllNodesWithText("270.0").assertCountEquals(0)
        so2r = true
        waitForIdle()
        onNodeWithText("270.0").assertExists()
        onNodeWithText("270.0").assertIsNotEnabled()
    }

    @Test
    fun `send buttons message and station fields share height and preserve realistic values`() = runComposeUiTest {
        val prefs = prefs().apply {
            getMYQRGFirstCat().set("1296.123.000")
            getMYQRGSecondCat().set("144.310.000")
            getActualQTF().set(360.0)
        }
        val state = chatInputStateFor(prefs).apply {
            value = TextFieldValue("/cq OK1IN test test test aasdasd ggggg asd asd gggg")
        }
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                ChatInputRow(
                    state = state,
                    prefs = prefs,
                    ownQrgMain = ObservedValue(prefs.getMYQRGFirstCat()),
                    ownQrgSecond = ObservedValue(prefs.getMYQRGSecondCat()),
                    antennaQtf = ObservedValue(prefs.getActualQTF()),
                    antennaQtf2 = ObservedValue(prefs.getActualQTF2()),
                    so2rEnabled = false,
                )
            }
        }

        val values = listOf("1296.123.000", "144.310.000", "360.0")
        val heights = listOf("TX", "clear")
            .map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.height } +
            values.map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.height }
        assertTrue(
            heights.max() - heights.min() <= 1f,
            "send-row control heights differ: $heights",
        )

        val minimumQrgWidth = with(density) { 96.dp.toPx() }
        values.take(2).forEach { value ->
            val width = onNodeWithText(value).fetchSemanticsNode().boundsInRoot.width
            assertTrue(width >= minimumQrgWidth - 1f, "$value has only $width px")
        }
        val qtfWidth = onNodeWithText("360.0").fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            abs(qtfWidth - with(density) { 56.dp.toPx() }) <= 1f,
            "360.0 field is $qtfWidth px wide",
        )
        assertTrue(
            abs(onNodeWithText("TX").fetchSemanticsNode().boundsInRoot.height -
                with(density) { 26.dp.toPx() }) <= 1f,
        )
    }
}
