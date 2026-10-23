package kst4contest.view.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class TimelineViewScreenTest {

    @Test
    fun `timeline shows all producer lanes and relative minute ticks`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TimelineView(
                    state = TimelineState(),
                    onCandidateClicked = {},
                    modifier = Modifier.width(700.dp),
                )
            }
        }

        listOf("In beam 1", "In beam 2", "Priority 1", "Priority 2", "Skeds").forEach {
            onNodeWithText(it).assertExists()
        }
        listOf("0 min", "+5 min", "+10 min", "+15 min", "+20 min", "+25 min", "+30 min").forEach {
            onNodeWithText(it).assertExists()
        }
    }
}
