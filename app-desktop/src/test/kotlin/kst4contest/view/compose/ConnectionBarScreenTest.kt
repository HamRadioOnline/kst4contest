package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.tabs.showTab
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalTestApi::class)
class ConnectionBarScreenTest(@param:TempDir private val tempDir: Path) {

    @Test
    fun `button availability follows every manager lifecycle state`() {
        var connectionState = On4KstConnectionState.DISCONNECTED
        val state = ConnectionBarState(preferences()) { connectionState }

        On4KstConnectionState.entries.forEach { lifecycle ->
            connectionState = lifecycle
            assertEqualsLifecycleRules(lifecycle, state)
        }
    }

    @Test
    fun `async disconnect redraws the open settings connection bar`() = runComposeUiTest {
        var connectionState by mutableStateOf(On4KstConnectionState.ONLINE)
        val state = ConnectionBarState(preferences()) { connectionState }

        showTab {
            ConnectionBar(
                state = state,
                onConnect = { },
                onSave = { },
                onApplyAndClose = { },
                onDisconnectAndCloseChat = { },
                onDisconnectOnly = { },
            )
        }

        onNodeWithText("Connect", substring = true).assertIsNotEnabled()
        onNodeWithText("Disconnect").assertIsEnabled()

        runOnIdle { connectionState = On4KstConnectionState.STOPPING }
        onNodeWithText("Connect", substring = true).assertIsNotEnabled()
        onNodeWithText("Disconnect").assertIsNotEnabled()

        runOnIdle { connectionState = On4KstConnectionState.DISCONNECTED }
        onNodeWithText("Connect", substring = true).assertIsEnabled()
        onNodeWithText("Disconnect").assertIsNotEnabled()
    }

    @Test
    fun `a failed attempt enables one new connection attempt`() = runComposeUiTest {
        var connectionState by mutableStateOf(On4KstConnectionState.CONNECTING)
        var attempts = 0
        val state = ConnectionBarState(preferences()) { connectionState }

        showTab {
            ConnectionBar(
                state = state,
                onConnect = {
                    attempts++
                    connectionState = On4KstConnectionState.CONNECTING
                },
                onSave = { },
                onApplyAndClose = { },
                onDisconnectAndCloseChat = { },
                onDisconnectOnly = { },
            )
        }

        runOnIdle { connectionState = On4KstConnectionState.DISCONNECTED }
        onNodeWithText("Connect", substring = true).performClick()

        assertTrue(attempts == 1)
        onNodeWithText("Connect", substring = true).assertIsNotEnabled()
    }

    private fun assertEqualsLifecycleRules(
        lifecycle: On4KstConnectionState,
        state: ConnectionBarState,
    ) {
        assertTrue(state.canConnect == (lifecycle == On4KstConnectionState.DISCONNECTED), lifecycle.name)
        assertTrue(state.canDisconnect == lifecycle.isConnectionAttemptActive, lifecycle.name)
        if (lifecycle == On4KstConnectionState.STOPPING) {
            assertFalse(state.canConnect)
            assertFalse(state.canDisconnect)
        }
    }

    private fun preferences() = ChatPreferences(tempDir.resolve("preferences.xml"))
}
