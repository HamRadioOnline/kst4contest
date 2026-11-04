package kst4contest.view.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ThreadStateMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class StatusBarScreenTest {

    @Test
    fun onlyAThreadWithARegisteredActionIsClickable() = runComposeUiTest {
        val buttons = ThreadStatusButtons()
        var beaconClicks = 0
        buttons.registerAction("MyBeacon") { beaconClicks++ }
        buttons.update("MyBeacon", status("off"))
        buttons.update("Cluster", status("on"))

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                StatusBar(
                    connectionState = null,
                    connectionDetail = null,
                    skedNotice = BlinkingNotice(),
                    bandUpgradeNotice = BlinkingNotice(),
                    threadButtons = buttons,
                    showConnectionBadge = false,
                )
            }
        }
        waitForIdle()

        onNodeWithText("MyBeacon: off").assertHasClickAction().performClick()
        waitForIdle()
        assertEquals(1, beaconClicks)
        onNodeWithText("Cluster: on").assertHasNoClickAction()
    }

    private fun status(description: String): ThreadStateMessage =
        ThreadStateMessage("worker", true, description, false).apply {
            runningInformationTextDescription = description
        }
}
