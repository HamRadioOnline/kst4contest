package kst4contest.view.compose.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.i18n.LanguageStore
import kst4contest.view.i18n.LocalStrings
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
class MapOverlayLegendScreenTest {

    @Test
    fun germanEveningLegendWrapsBothItemsInsideANarrowHeader() = runComposeUiTest {
        var storedLanguage = "de"
        val languages = LanguageStore(
            storedLanguage = { storedLanguage },
            storeLanguage = { storedLanguage = it },
            systemDefault = Locale.GERMANY,
        )

        try {
            setContent {
                CompositionLocalProvider(LocalStrings provides languages.strings) {
                    Kst4ContestTheme(darkMode = true, baseFontSizeSp = 12f) {
                        Box(Modifier.width(110.dp)) {
                            MapOverlayLegend(
                                showAntenna = true,
                                showSpatialFilters = true,
                                darkMode = true,
                            )
                        }
                    }
                }
            }

            onNodeWithText("Antenne").assertIsDisplayed()
            onNodeWithText("Aktive Filter").assertIsDisplayed()

            val antenna = onNodeWithTag(MAP_ANTENNA_LEGEND_TAG).fetchSemanticsNode().boundsInRoot
            val filters = onNodeWithTag(MAP_FILTER_LEGEND_TAG).fetchSemanticsNode().boundsInRoot
            val widthLimit = with(density) { 110.dp.toPx() }
            assertTrue(antenna.right <= widthLimit + 0.5f)
            assertTrue(filters.right <= widthLimit + 0.5f)
            assertTrue(filters.top >= antenna.bottom, "the legend items did not wrap at narrow width")
        } finally {
            runOnIdle { languages.use("en") }
        }
    }
}
