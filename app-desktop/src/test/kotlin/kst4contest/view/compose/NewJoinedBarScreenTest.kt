package kst4contest.view.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.RecentChatJoin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class NewJoinedBarScreenTest {

    @Test
    fun fiveNewestStationsAreDirectAndAdditionalStationsUseTheMoreDropdown() = runComposeUiTest {
        val selection = StationSelectionState()
        val members = (1..7).map { member("DL${it}ABC") }
        val newJoined = NewJoinedState(clock = { 10_000L }) { member ->
            selection.select(member, StationSelectionSource.NEW_JOINED)
        }.apply {
            updateActiveMembers(members)
            updateEvents(
                members.mapIndexed { index, member ->
                    RecentChatJoin(member.callSign, 2, 9_000L + index)
                },
            )
        }
        val priority = TopPriorityState(topStations = { emptyList() }, onSelect = {})

        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                TopPriorityBar(
                    state = priority,
                    newJoined = newJoined,
                    onShowMore = {},
                )
            }
        }

        onNodeWithText("New joined:").assertIsDisplayed()
        listOf("DL7ABC", "DL6ABC", "DL5ABC", "DL4ABC", "DL3ABC").forEach {
            onNodeWithText(it).assertIsDisplayed()
        }
        onNodeWithText("DL2ABC").assertDoesNotExist()

        onNodeWithTag(NEW_JOINED_MORE_TAG).performClick()
        onNodeWithText("DL2ABC").assertIsDisplayed().performClick()

        assertSame(members[1], selection.selected)
        assertEquals(StationSelectionSource.NEW_JOINED, selection.lastSource)
        assertTrue(selection.scrollRequestRevision > 0L)
    }

    @Test
    fun emptyNewJoinedStateUsesNoSpaceBesidePriority() = runComposeUiTest {
        val newJoined = NewJoinedState(onSelect = {})
        val priorityMember = member("OK1FPR")
        val priority = TopPriorityState(
            topStations = { listOf(priorityMember to 767.0) },
            onSelect = {},
        ).apply { refresh() }

        setContent {
            Kst4ContestTheme(darkMode = true, baseFontSizeSp = 12f) {
                TopPriorityBar(
                    state = priority,
                    newJoined = newJoined,
                    onShowMore = {},
                )
            }
        }

        onNodeWithText("New joined:").assertDoesNotExist()
        onNodeWithText("Priority:").assertIsDisplayed()
        onNodeWithText("1 OK1FPR 767").assertIsDisplayed()
    }

    @Test
    fun narrowBarMovesAdditionalDirectEntriesIntoTheSameDropdownWithoutOverflow() = runComposeUiTest {
        val members = (1..7).map { member("DL${it}ABC") }
        val newJoined = NewJoinedState(clock = { 10_000L }, onSelect = {}).apply {
            updateActiveMembers(members)
            updateEvents(members.mapIndexed { index, member ->
                RecentChatJoin(member.callSign, 2, 9_000L + index)
            })
        }

        setContent {
            Kst4ContestTheme(darkMode = true, baseFontSizeSp = 12f) {
                Box(Modifier.width(300.dp)) {
                    TopPriorityBar(
                        state = TopPriorityState(topStations = { emptyList() }, onSelect = {}),
                        newJoined = newJoined,
                        onShowMore = {},
                    )
                }
            }
        }

        onNodeWithText("DL7ABC").assertIsDisplayed()
        onNodeWithText("DL6ABC").assertDoesNotExist()
        val limit = with(density) { 300.dp.toPx() }
        listOf(NEW_JOINED_GROUP_TAG, PRIORITY_GROUP_TAG).forEach { tag ->
            val bounds = onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.right <= limit + 0.5f, "$tag crosses the narrow bar")
        }

        onNodeWithTag(NEW_JOINED_MORE_TAG).performClick()
        onNodeWithText("DL6ABC").assertIsDisplayed()
    }

    private fun member(callSign: String) = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(2)
    }
}
