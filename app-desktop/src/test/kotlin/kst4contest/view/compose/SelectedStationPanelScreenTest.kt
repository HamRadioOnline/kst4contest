package kst4contest.view.compose

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.EnumSet
import java.util.function.Consumer
import kotlin.math.abs

/** The selected-station controls remain reachable when its splitter pane is short. */
@OptIn(ExperimentalTestApi::class)
class SelectedStationPanelScreenTest {

    @Test
    fun stationHeaderKeepsAllMetadataOnOneLineWhenWideAndOmitsTheLocator() = runComposeUiTest {
        val station = member("CT1EKD").apply {
            qra = "IM59IB"
            chatCategory = ChatCategory(2)
            setQTFdirection(255.54)
            qrb = 1907.59
            activityTimeLastInEpoch = Instant.now().epochSecond - 240
        }
        val selected = selectedState(station, detectedBands = "Bands: 144/432 MHz")

        setPanel(selected, Modifier.width(900.dp).height(420.dp))

        val callsignTop = onNodeWithText("CT1EKD").fetchSemanticsNode().boundsInRoot.top
        listOf(
            station.chatCategory.getChatCategoryName(station.chatCategory.categoryNumber),
            "QTF: 255.54°",
            "QRB: 1907.59 km",
            "Bands: 144/432 MHz",
        ).forEach { text ->
            val top = onNodeWithText(text).assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
            assertTrue(abs(top - callsignTop) <= 4f, "$text did not remain in the wide title row")
        }
        onNodeWithText("Last activity:", substring = true).assertIsDisplayed()
        onNodeWithText("Bands: Bands:", substring = true).assertDoesNotExist()
        onNodeWithText("IM59IB").assertDoesNotExist()
    }

    @Test
    fun stationHeaderWrapsMetadataWithoutClippingWhenNarrow() = runComposeUiTest {
        val station = member("CT1EKD").apply {
            qra = "IM59IB"
            chatCategory = ChatCategory(2)
            setQTFdirection(255.54)
            qrb = 1907.59
            activityTimeLastInEpoch = Instant.now().epochSecond - 240
        }
        val selected = selectedState(station, detectedBands = "Bands: 144/432 MHz")

        setPanel(selected, Modifier.width(310.dp).height(620.dp))

        val callsign = onNodeWithText("CT1EKD").fetchSemanticsNode().boundsInRoot
        val bands = onNodeWithText("Bands: 144/432 MHz").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val panel = onNodeWithTag(SELECTED_STATION_PANEL_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(bands.top > callsign.top, "the narrow header did not wrap")
        assertTrue(bands.right <= panel.right, "wrapped metadata crossed the panel edge")
        onNodeWithText("IM59IB").assertDoesNotExist()
    }

    @Test
    fun stationHeaderKeepsTheAlreadyLabelledEmptyBandsValueExactlyOnce() = runComposeUiTest {
        val selected = selectedState(member("DM5M"), detectedBands = "Bands: -")

        setPanel(selected, Modifier.width(570.dp).height(420.dp))

        onNodeWithText("Bands: -").assertIsDisplayed()
        onNodeWithText("Bands: Bands:", substring = true).assertDoesNotExist()
    }

    @Test
    fun messageFilterLabelSharesTheFirstFlowItemWithPmToMe() = runComposeUiTest {
        val selected = selectedState("DM5M")

        setPanel(selected, Modifier.width(250.dp).height(520.dp))

        val label = onNodeWithText("Show messages:").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val firstChoice = onNodeWithText("pm to me").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        assertTrue(abs(label.center.y - firstChoice.center.y) <= 2f)
    }

    @Test
    fun notQrvHeadingFollowsTheCurrentStationWithoutKeepingAStaleCallsign() = runComposeUiTest {
        val selected = selectedState("DM5M")

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SelectedStationPanel(
                    state = selected,
                    messages = messageTable(),
                    activeBands = EnumSet.of(Band.B_144),
                    openInBrowser = Consumer {},
                    onShowOnMap = {},
                    onShowPathInAirScout = {},
                    modifier = Modifier.height(420.dp),
                )
            }
        }

        onNodeWithText("Tag DM5M not QRV at bands:").assertIsDisplayed()

        runOnIdle { selected.select(member("DL1ABC")) }
        waitForIdle()
        onNodeWithText("Tag DM5M not QRV at bands:").assertDoesNotExist()
        onNodeWithText("Tag DL1ABC not QRV at bands:").assertIsDisplayed()

        runOnIdle { selected.select(null) }
        waitForIdle()
        onNodeWithText("Tag DL1ABC not QRV at bands:").assertDoesNotExist()
    }

    @Test
    fun skedSentenceUsesTheCompactControlsWithoutTheOldOffsetsLabel() = runComposeUiTest {
        val selected = selectedState("DM5M")

        setContent {
            Kst4ContestTheme(darkMode = true, baseFontSizeSp = 12f) {
                SelectedStationPanel(
                    state = selected,
                    messages = messageTable(),
                    activeBands = EnumSet.of(Band.B_144),
                    openInBrowser = Consumer {},
                    onShowOnMap = {},
                    onShowPathInAirScout = {},
                    modifier = Modifier.height(420.dp),
                )
            }
        }

        onNodeWithText("Sked alert").assertIsDisplayed()
        onNodeWithText("Sked in").assertIsDisplayed()
        onNodeWithText("Remind pm in").assertIsDisplayed()
        onNodeWithText("Reminder offsets").assertDoesNotExist()
    }

    @Test
    fun skedBandChoiceUsesEnabledBandsAndPassesTheSelectionToSkedCreation() = runComposeUiTest {
        var createdMinutes: Int? = null
        var createdBand: Band? = null
        var createdMode: String? = null
        val selected = SelectedStationState(
            propagateNotQrv = {},
            createSked = { _, minutes, band, mode ->
                createdMinutes = minutes
                createdBand = band
                createdMode = mode
                true
            },
        ).apply {
            select(member("DM5M"))
            skedBand = Band.B_144
            skedMode = "SSB"
        }

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SelectedStationPanel(
                    state = selected,
                    messages = messageTable(),
                    activeBands = EnumSet.of(Band.B_144, Band.B_432),
                    openInBrowser = Consumer {},
                    onShowOnMap = {},
                    onShowPathInAirScout = {},
                    modifier = Modifier.width(570.dp).height(520.dp),
                )
            }
        }

        onNodeWithText("144 MHz").performClick()
        onNodeWithText("1296 MHz").assertDoesNotExist()
        onNodeWithText("432 MHz").assertIsDisplayed().performClick()
        assertEquals(Band.B_432, selected.skedBand)

        onNodeWithText("5").performClick()
        onNodeWithText("8").assertIsDisplayed().performClick()
        onNodeWithText("SSB").performClick()
        onNodeWithText("CW").assertIsDisplayed().performClick()

        onNodeWithText("Create sked").performScrollTo().performClick()
        assertEquals(8, createdMinutes)
        assertEquals(Band.B_432, createdBand)
        assertEquals("CW", createdMode)
    }

    @Test
    fun notQrvToggleButtonsKeepAllSemanticsAndFollowTheSelectedStation() = runComposeUiTest {
        val first = member("DM5M").apply {
            isQrv144 = false
            isQrv432 = true
            isQrv1240 = false
        }
        val second = member("DL1ABC").apply {
            isQrv144 = true
            isQrv432 = false
            isQrv1240 = true
        }
        val propagated = mutableListOf<ChatMember>()
        val selected = SelectedStationState(
            propagateNotQrv = { propagated += it },
            createSked = { _, _, _, _ -> true },
        ).apply { select(first) }

        setPanel(
            selected,
            Modifier.width(570.dp).height(520.dp),
            EnumSet.of(Band.B_144, Band.B_432, Band.B_1296),
        )

        onNodeWithText("144").assertIsOn()
        onNodeWithText("432").assertIsOff()
        onNodeWithText("23cm").assertIsOn()
        onNodeWithText("all").assertIsOff().performClick().assertIsOn()
        assertFalse(first.isQrv10G, "all must retain its existing all-supported-bands semantics")
        assertTrue(propagated.last() === first)

        onNodeWithText("all").performClick().assertIsOff()
        assertTrue(first.isQrv144)
        assertTrue(first.isQrv10G)

        runOnIdle { selected.select(second) }
        waitForIdle()
        onNodeWithText("144").assertIsOff()
        onNodeWithText("432").assertIsOn()
        onNodeWithText("23cm").assertIsOff()
    }

    @Test
    fun notQrvAndSkedGroupsFollowTheActualPanelWidth() = runComposeUiTest {
        val selected = selectedState("DM5M")
        var panelWidth by mutableStateOf(620.dp)

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                Panel(selected, Modifier.width(panelWidth).height(560.dp))
            }
        }

        var notQrv = onNodeWithTag(SELECTED_NOT_QRV_GROUP_TAG).fetchSemanticsNode().boundsInRoot
        var sked = onNodeWithTag(SELECTED_SKED_GROUP_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(abs(notQrv.top - sked.top) <= 1f, "wide groups were not arranged horizontally")
        assertTrue(sked.left >= notQrv.right, "wide groups overlap")

        runOnIdle { panelWidth = 360.dp }
        waitForIdle()

        notQrv = onNodeWithTag(SELECTED_NOT_QRV_GROUP_TAG).fetchSemanticsNode().boundsInRoot
        sked = onNodeWithTag(SELECTED_SKED_GROUP_TAG).fetchSemanticsNode().boundsInRoot
        val panel = onNodeWithTag(SELECTED_STATION_PANEL_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(sked.top >= notQrv.bottom, "narrow groups were not stacked vertically")
        assertTrue(notQrv.right <= panel.right && sked.right <= panel.right, "a group crossed the panel edge")

        listOf("Create sked", "Sked fail", "Reset fail").forEach {
            onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun lookupActionsCanBeScrolledIntoViewInAShortPane() = runComposeUiTest {
        val selected = selectedState("DL1ABC")
        val messages = messageTable()

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SelectedStationPanel(
                    state = selected,
                    messages = messages,
                    activeBands = EnumSet.of(Band.B_144),
                    openInBrowser = Consumer {},
                    onShowOnMap = {},
                    onShowPathInAirScout = {},
                    modifier = Modifier.height(220.dp),
                )
            }
        }

        onNodeWithText("Lookup on qrz.com")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun selectedState(callSign: String) = selectedState(member(callSign))

    private fun selectedState(member: ChatMember, detectedBands: String = "") = SelectedStationState(
        propagateNotQrv = {},
        createSked = { _, _, _, _ -> true },
        detectBands = { detectedBands },
    ).apply { select(member) }

    private fun member(callSign: String) = ChatMember().apply {
        this.callSign = callSign
        qra = "JO50AA"
    }

    private fun androidx.compose.ui.test.ComposeUiTest.setPanel(
        selected: SelectedStationState,
        modifier: Modifier,
        activeBands: Set<Band> = EnumSet.of(Band.B_144),
    ) {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                Panel(selected, modifier, activeBands)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Panel(
        selected: SelectedStationState,
        modifier: Modifier,
        activeBands: Set<Band> = EnumSet.of(Band.B_144),
    ) {
        SelectedStationPanel(
            state = selected,
            messages = messageTable(),
            activeBands = activeBands,
            openInBrowser = Consumer {},
            onShowOnMap = {},
            onShowPathInAirScout = {},
            modifier = modifier,
        )
    }

    private fun messageTable() = DataTableState(
            SelectedStationMessageColumns.all(),
            RowKeys.byReference<ChatMessage>(),
            "selected-station-panel-test",
        )
}
