package kst4contest.view.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.RecentChatJoin
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumSet
import java.util.function.Consumer

/** Produces review images for the station filters, join bar and selected-station splitter. */
class StationWorkspaceVisualRenderingTest {

    @Test
    fun rendersDayAndEveningAt100And125Percent() {
        val reportDirectory = Path.of("build", "reports", "station-workspace-ui")
        Files.createDirectories(reportDirectory)

        listOf(
            Scenario("wide", 700, 768),
            Scenario("narrow", 360, 768),
            Scenario("short", 570, 480),
        ).forEach { scenario ->
            listOf(false, true).forEach { darkMode ->
                listOf(1f, 1.25f).forEach { scale ->
                    val bytes = render(scenario, darkMode, scale)
                    val palette = if (darkMode) "evening" else "day"
                    val scaling = if (scale == 1f) "100" else "125"
                    val name = "${scenario.name}-$palette-$scaling.png"
                    Files.write(reportDirectory.resolve(name), bytes)
                    assertTrue(bytes.size > 10_000, "$name did not render")
                }
            }
        }
    }

    private fun render(scenario: Scenario, darkMode: Boolean, scale: Float): ByteArray {
        val members = (1..7).map { index -> member("DL${index}ABC", if (index == 7) 3 else 2) }
        val selectedMember = members.last().apply {
            qrb = 438.2
            setQTFdirection(76.0)
            isQrv432 = false
        }
        val newJoined = NewJoinedState(clock = { 20_000L }, onSelect = {}).apply {
            updateActiveMembers(members)
            updateEvents(members.mapIndexed { index, member ->
                RecentChatJoin(member.callSign, member.chatCategory.categoryNumber, 10_000L + index)
            })
        }
        val stations = DataTableState(
            StationColumns.all(
                tropoOf = { "12 dB" },
                priorityScoreOf = { 500.0 },
                formatBand = { _, _, _ -> "" },
                activeBands = EnumSet.of(Band.B_144, Band.B_432),
            ),
            RowKeys.byValue(StationColumns::rowKey),
        ).apply { replaceRows(members) }
        val priority = TopPriorityState(
            topStations = { listOf(members[4] to 767.0, members[3] to 740.0) },
            onSelect = {},
        ).apply { refresh() }
        val selected = SelectedStationState(
            propagateNotQrv = {},
            createSked = { _, _, _, _ -> true },
            detectBands = { "Bands: 144/432 MHz" },
        ).apply { select(selectedMember) }
        val selectedMessages = DataTableState(
            SelectedStationMessageColumns.all(),
            RowKeys.byReference<ChatMessage>(),
        )

        val scene = ImageComposeScene(
            width = 1366,
            height = scenario.height,
            density = Density(scale),
        ) {
            Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = 12f) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .width(scenario.width.dp)
                            .fillMaxHeight()
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    ) {
                        StationFilterBar(
                            filters = StationFilterState(),
                            activeBands = EnumSet.of(Band.B_144, Band.B_432),
                            reachability = ReachabilityState(EnumSet.of(Band.B_144, Band.B_432)),
                            onChanged = {},
                        )
                        DataTableView(
                            state = stations,
                            modifier = Modifier.weight(0.45f),
                            emphasizeCell = { member, column ->
                                column.id == "callsign" && newJoined.isRecent(member)
                            },
                        )
                        TopPriorityBar(
                            state = priority,
                            newJoined = newJoined,
                            onShowMore = {},
                        )
                        SelectedStationPanel(
                            state = selected,
                            messages = selectedMessages,
                            activeBands = EnumSet.of(Band.B_144, Band.B_432),
                            contentSplitter = SplitterState(2, doubleArrayOf(0.48)) {},
                            openInBrowser = Consumer {},
                            onShowOnMap = {},
                            onShowPathInAirScout = {},
                            modifier = Modifier.weight(0.55f),
                        )
                    }
                    }
                }
            }
        }

        return try {
            scene.render().encodeToData()!!.bytes
        } finally {
            scene.close()
        }
    }

    private fun member(callSign: String, category: Int) = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(category)
        qra = "JO31AA"
        name = "Contest station"
    }

    private data class Scenario(val name: String, val width: Int, val height: Int)
}
