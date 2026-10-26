package kst4contest.view.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.EnumSet
import java.util.function.Consumer

/** Produces review images for the selected-station controls and lower message tabs. */
class SelectedStationMessageUiVisualRenderingTest {

    @Test
    fun rendersDayAndEveningAt100And125Percent() {
        val reportDirectory = Path.of("build", "reports", "selected-station-message-ui")
        Files.createDirectories(reportDirectory)

        val scenarios = listOf(
            RenderScenario(prefix = "", panelWidth = 570.dp, windowHeight = 768),
            RenderScenario(prefix = "narrow-", panelWidth = 360.dp, windowHeight = 768),
            RenderScenario(prefix = "short-", panelWidth = 570.dp, windowHeight = 480),
        )
        scenarios.forEach { scenario ->
            listOf(false, true).forEach { darkMode ->
                listOf(1f, 1.25f).forEach { scale ->
                    val bytes = render(darkMode, scale, scenario.panelWidth, scenario.windowHeight)
                    val palette = if (darkMode) "evening" else "day"
                    val scaling = if (scale == 1f) "100" else "125"
                    val name = "${scenario.prefix}$palette-$scaling.png"
                    Files.write(reportDirectory.resolve(name), bytes)
                    assertTrue(bytes.size > 10_000, "$name did not render")
                }
            }
        }
    }

    private fun render(darkMode: Boolean, scale: Float, panelWidth: Dp, windowHeight: Int): ByteArray {
        val selectedMember = member("DM5M", 2).apply {
            setQTFdirection(255.54)
            qrb = 1907.59
            activityTimeLastInEpoch = Instant.now().epochSecond - 240
            isQrv144 = false
            isQrv1240 = false
        }
        val selectedState = SelectedStationState(
            propagateNotQrv = {},
            createSked = { _, _, _, _ -> true },
            detectBands = { "Bands: 144/432 MHz" },
        ).apply {
            select(selectedMember)
            skedBand = Band.B_144
            skedMode = "SSB"
            remindPm = true
        }
        val selectedMessages = messageTable().apply {
            replaceRows(
                listOf(
                    message(selectedMember, member("DL0OWN", 2), "pse ur qrg"),
                    message(selectedMember, member("ALL", 2), "(DL0OWN) qrv 144.300"),
                ),
            )
        }
        val publicMessages = messageTable().apply {
            replaceRows(listOf(message(selectedMember, member("ALL", 2), "cq 144.300")))
        }
        val qsoMessages = messageTable().apply {
            replaceRows(listOf(message(member("DL1ABC", 2), member("DL2XYZ", 2), "pse sked")))
        }

        return onEventThread {
            val scene = ImageComposeScene(width = 1366, height = windowHeight, density = Density(scale)) {
                Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = 12f) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Row(Modifier.fillMaxSize().padding(6.dp)) {
                            MessageTabs(
                                publicMessages = publicMessages,
                                clusterMessages = clusterTable(),
                                qsoOfTheOther = qsoMessages,
                                modifier = Modifier.weight(1f),
                            )
                            SelectedStationPanel(
                                state = selectedState,
                                messages = selectedMessages,
                                activeBands = EnumSet.of(Band.B_144, Band.B_432, Band.B_1296),
                                openInBrowser = Consumer {},
                                onShowOnMap = {},
                                onShowPathInAirScout = {},
                                modifier = Modifier.width(panelWidth).padding(start = 6.dp),
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

    private fun messageTable() = DataTableState(
        columns = listOf(
            DataColumn("sender", "Sender", value = { message: ChatMessage -> message.sender.callSign }),
            DataColumn("message", "Message", value = { message: ChatMessage -> message.messageText.orEmpty() }),
        ),
        rowKey = RowKeys.byReference<ChatMessage>(),
    )

    private fun clusterTable() = DataTableState(
        columns = listOf(
            DataColumn(
                "message",
                "DXCluster",
                value = { message: ClusterMessage -> message.messageInhibited.orEmpty() },
            ),
        ),
        rowKey = RowKeys.byReference<ClusterMessage>(),
    )

    private fun message(sender: ChatMember, receiver: ChatMember, text: String) = ChatMessage().apply {
        this.sender = sender
        this.receiver = receiver
        chatCategory = sender.chatCategory
        messageText = text
    }

    private fun member(callSign: String, category: Int) = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(category)
        qra = "JO31AA"
    }

    private data class RenderScenario(
        val prefix: String,
        val panelWidth: Dp,
        val windowHeight: Int,
    )
}
