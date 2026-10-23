package kst4contest.view.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.view.compose.tabs.checkBeside
import kst4contest.view.compose.tabs.fieldBeside
import kst4contest.view.compose.tabs.showTab
import kst4contest.view.compose.tabs.toggle
import kst4contest.view.compose.tabs.type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.EnumSet
import kotlin.math.abs

/**
 * The QRB part of the station filter bar, drawn (#21).
 *
 * [StationFilterStateTest] pins which distances pass; this pins that the two QRB controls
 * on screen write into their own filter and not into the other one. The two labels differ
 * only in `<=` and `>`, so a swapped binding would look right and filter the wrong side.
 *
 * Headless: only a composition, no window.
 */
@OptIn(ExperimentalTestApi::class)
class StationFilterBarScreenTest {

    private val maxLabel = "Show only QRB [km] <="
    private val minLabel = "Show only QRB [km] >"

    private class Bar {
        val filters = StationFilterState()
        var changes = 0
    }

    private fun ComposeUiTest.showBar(): Bar {
        val bar = Bar()
        showTab {
            StationFilterBar(
                filters = bar.filters,
                activeBands = EnumSet.of(Band.B_144),
                reachability = ReachabilityState(EnumSet.of(Band.B_144)),
                onChanged = { bar.changes++ },
            )
        }
        return bar
    }

    @Test
    fun reachabilityBandChangesDoNotCalculateUntilTheButtonIsClicked() = runComposeUiTest {
        val filters = StationFilterState()
        val station = kst4contest.model.ChatMember()
        var calculations = 0
        val reachability = ReachabilityState(
            activeBands = EnumSet.of(Band.B_144, Band.B_432),
            resolveAutoBand = { Band.B_144 },
            selectedStation = { station },
            calculate = { member, band ->
                assertEquals(station, member)
                assertEquals(Band.B_432, band)
                calculations++
            },
        )

        showTab {
            StationFilterBar(
                filters = filters,
                activeBands = EnumSet.of(Band.B_144, Band.B_432),
                reachability = reachability,
                onChanged = { },
            )
        }

        onNodeWithText("Reachability").assertIsDisplayed()
        onNodeWithText("Auto").performClick()
        awaitNode(inPopup("432")).performClick()
        assertEquals(0, calculations)

        onNodeWithText("Calc selected").performClick()
        assertEquals(1, calculations)
    }

    @Test
    fun theMinimumQrbControlsWriteTheMinimumFilterOnly() = runComposeUiTest {
        val bar = showBar()

        type(minLabel, "300")
        toggle(minLabel)

        assertTrue(bar.filters.minQrbEnabled)
        assertEquals(300.0, bar.filters.minQrbKm)
        assertEquals(setOf(StationFilter.MIN_QRB), bar.filters.activeFilters)
        assertEquals(0.0, bar.filters.maxQrbKm, "the minimum field wrote into the maximum")
        assertTrue(bar.changes > 0, "switching the filter on did not reapply the filters")
        checkBeside(minLabel).assertIsOn()
        checkBeside(maxLabel).assertIsOff()
    }

    @Test
    fun bothQrbControlsTogetherMakeARange() = runComposeUiTest {
        val bar = showBar()

        type(maxLabel, "500")
        toggle(maxLabel)
        type(minLabel, "200")
        toggle(minLabel)

        assertEquals(setOf(StationFilter.MAX_QRB, StationFilter.MIN_QRB), bar.filters.activeFilters)
        fieldBeside(maxLabel).assertTextEquals("500")
        fieldBeside(minLabel).assertTextEquals("200")
        assertFalse(bar.filters.acceptsQrb(150.0))
        assertTrue(bar.filters.acceptsQrb(350.0))
        assertFalse(bar.filters.acceptsQrb(650.0))
    }

    /** A pasted "Infinity" as the minimum hid every station, a "NaN" let every one through. */
    @Test
    fun theQrbFieldsKeepTheirValueWhenTheTextIsNoDistance() = runComposeUiTest {
        val bar = showBar()
        type(maxLabel, "500")
        type(minLabel, "200")

        listOf("Infinity", "NaN", "-5").forEach { text ->
            type(maxLabel, text)
            type(minLabel, text)
        }

        assertEquals(500.0, bar.filters.maxQrbKm)
        assertEquals(200.0, bar.filters.minQrbKm)
        fieldBeside(maxLabel).assertTextEquals("500")
        fieldBeside(minLabel).assertTextEquals("200")
    }

    @Test
    fun resettingTheFiltersUnticksTheMinimumQrb() = runComposeUiTest {
        val bar = showBar()
        toggle(minLabel)
        checkBeside(minLabel).assertIsOn()

        onNodeWithText("Reset filters").performClick()
        waitForIdle()

        checkBeside(minLabel).assertIsOff()
        assertFalse(bar.filters.minQrbEnabled)
    }

    /**
     * The first row used to be a plain row: with both QRB filters in it, a narrow station
     * pane cut the bearing buttons off at the right edge. It wraps now, and every control
     * has to stay on screen.
     */
    @Test
    fun aNarrowBarWrapsInsteadOfCuttingControlsOff() = runComposeUiTest {
        val filters = StationFilterState()
        val width = 300.dp
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                Box(Modifier.width(width)) {
                    StationFilterBar(
                        filters = filters,
                        activeBands = EnumSet.of(Band.B_144),
                        reachability = ReachabilityState(EnumSet.of(Band.B_144)),
                        onChanged = {},
                    )
                }
            }
        }
        waitForIdle()

        val limit = with(density) { width.toPx() }
        val controls = Bearing.entries.map { onNodeWithText(it.name) } +
            listOf(
                onNodeWithText(maxLabel),
                onNodeWithText(minLabel),
                onNodeWithText("Show only QTF:"),
                onNodeWithText("Inactive stations"),
                onNodeWithText("Only new grids"),
                onNodeWithText("AS next 5m"),
                onNodeWithText("Calc selected"),
                fieldBeside(maxLabel),
                fieldBeside(minLabel),
            )
        controls.forEach { node ->
            node.assertIsDisplayed()
            val right = node.fetchSemanticsNode().boundsInRoot.right
            assertTrue(right <= limit + 0.5f, "a control ends at $right px, beyond the $limit px wide bar")
        }
    }

    @Test
    fun filterControlsShareOneHeightAndBearingsShareOneCompactWidth() = runComposeUiTest {
        showBar()

        val fieldHeight = fieldBeside(maxLabel).fetchSemanticsNode().boundsInRoot.height
        val checkboxHeight = checkBeside(maxLabel).fetchSemanticsNode().boundsInRoot.height
        val findFieldHeight = onNodeWithText("Find...").fetchSemanticsNode().boundsInRoot.height
        val controlHeights = listOf("Reset filters", "N", "Auto", "Calc selected")
            .map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.height } +
            listOf(fieldHeight, checkboxHeight, findFieldHeight)
        assertTrue(
            controlHeights.max() - controlHeights.min() <= 1f,
            "filter control heights differ: $controlHeights",
        )

        val bearingWidths = Bearing.entries
            .map { onNodeWithText(it.name).fetchSemanticsNode().boundsInRoot.width }
        assertTrue(
            bearingWidths.max() - bearingWidths.min() <= 1f,
            "bearing widths differ: $bearingWidths",
        )
        val expectedWidth = with(density) { Density.DIRECTION_BUTTON_WIDTH.toPx() }
        assertTrue(
            bearingWidths.all { abs(it - expectedWidth) <= 1f },
            "bearing buttons are not $expectedWidth px wide: $bearingWidths",
        )
    }
}
