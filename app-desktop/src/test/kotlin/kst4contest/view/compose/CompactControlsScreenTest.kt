package kst4contest.view.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/** Shared fields and choices use the same compact desktop height. */
@OptIn(ExperimentalTestApi::class)
class CompactControlsScreenTest {

    @Test
    fun fieldsChoicesAndButtonsShareTheDesktopControlHeight() = runComposeUiTest {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                Column {
                    CompactTextField(value = "144.310.000", onValueChange = {})
                    Form.inlineChoice(
                        label = "Reachability",
                        items = listOf("Auto"),
                        selected = "Auto",
                        describe = { it },
                        onSelect = {},
                    )
                    Form.button(text = "TX", onClick = {})
                    Form.button(text = "Filter", onClick = {})
                }
            }
        }

        val heights = listOf("144.310.000", "Auto", "TX", "Filter")
            .map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.height }
        val targetHeight = with(density) { 26.dp.toPx() }

        assertTrue(
            heights.all { abs(it - targetHeight) <= 1f },
            "controls are $heights px tall instead of $targetHeight px",
        )
        assertTrue(
            heights.max() - heights.min() <= 1f,
            "control heights differ by more than one pixel: $heights",
        )
    }

    @Test
    fun accentAndToggleButtonsShareEnabledAndDisabledClickBehaviour() = runComposeUiTest {
        var accentClicks = 0
        var disabledClicks = 0
        setContent {
            Kst4ContestTheme(darkMode = true, baseFontSizeSp = 12f) {
                Column {
                    Form.button(
                        text = "Accent",
                        accented = true,
                        onClick = { accentClicks++ },
                    )
                    Form.button(
                        text = "Selected",
                        selected = true,
                        onClick = {},
                    )
                    Form.button(
                        text = "Disabled accent",
                        enabled = false,
                        accented = true,
                        onClick = { disabledClicks++ },
                    )
                }
            }
        }

        onNodeWithText("Accent").performClick()
        onNodeWithText("Disabled accent").performClick()

        assertEquals(1, accentClicks)
        assertEquals(0, disabledClicks)
        assertTrue(onNodeWithText("Selected").fetchSemanticsNode().boundsInRoot.height < 40f)
    }

    @Test
    fun controlsStayAlignedAt125PercentDensityInEveningDesign() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(
                LocalDensity provides androidx.compose.ui.unit.Density(1.25f),
            ) {
                Kst4ContestTheme(darkMode = true, baseFontSizeSp = 12f) {
                    Column {
                        CompactTextField(value = "10368.000", onValueChange = {})
                        Form.inlineChoice(
                            label = "Reachability",
                            items = listOf("Auto"),
                            selected = "Auto",
                            describe = { it },
                            onSelect = {},
                        )
                        Form.button(text = "TX", onClick = {})
                    }
                }
            }
        }

        val heights = listOf("10368.000", "Auto", "TX")
            .map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.height }
        val expected = 26f * 1.25f
        assertTrue(
            heights.all { abs(it - expected) <= 1f },
            "125% controls are $heights px tall instead of about $expected px",
        )
        assertTrue(
            heights.max() - heights.min() <= 1f,
            "125% control heights differ by more than one pixel: $heights",
        )
    }

    @Test
    fun controlsGrowTogetherInsteadOfClippingALargerConfiguredFont() = runComposeUiTest {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 24f) {
                Column {
                    CompactTextField(value = "1296.123.000", onValueChange = {})
                    Form.inlineChoice(
                        label = "Reachability",
                        items = listOf("Auto"),
                        selected = "Auto",
                        describe = { it },
                        onSelect = {},
                    )
                    Form.button(text = "TX", onClick = {})
                }
            }
        }

        val heights = listOf("1296.123.000", "Auto", "TX")
            .map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.height }
        assertTrue(heights.min() > with(density) { 26.dp.toPx() })
        assertTrue(
            heights.max() - heights.min() <= 1f,
            "large-font controls do not grow together: $heights",
        )
    }
}
