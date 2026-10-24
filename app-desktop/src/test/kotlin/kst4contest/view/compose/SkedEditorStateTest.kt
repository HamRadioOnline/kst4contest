package kst4contest.view.compose

import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkedEditorStateTest {

    @Test
    fun `panel submission includes reminder settings in one request`() {
        val requests = mutableListOf<SkedRequest>()
        val member = member("DL1ABC", 2)
        val state = SelectedStationState(
            propagateNotQrv = { },
            createSked = { _, _, _, _ -> false },
        )
        state.skedRequestSubmitter = { request -> requests += request; true }
        state.select(member)
        state.skedMinutes = 8
        state.skedBand = Band.B_432
        state.skedMode = "CW"
        state.remindPm = true
        state.reminderOffsets = "5+2+1"

        assertTrue(state.createSked())
        assertEquals(
            SkedRequest(member, 8, Band.B_432, "CW", true, "5+2+1"),
            requests.single(),
        )
    }

    @Test
    fun `popup draft keeps its target when the shared selection changes`() {
        val requests = mutableListOf<SkedRequest>()
        val original = member("DL1ABC", 2)
        val later = member("DL2XYZ", 3)
        val draft = SkedEditorState(
            target = original,
            minutes = 5,
            band = Band.B_144,
            mode = "SSB",
            remindPm = false,
            reminderOffsets = "2+1",
            submitter = { request -> requests += request; true },
        )

        draft.minutes = 10
        draft.band = Band.B_1296
        draft.remindPm = true
        @Suppress("UNUSED_VARIABLE") val unrelatedSelection = later

        assertTrue(draft.submit())
        assertSame(original, requests.single().target)
        assertEquals(Band.B_1296, requests.single().band)
        assertTrue(requests.single().remindPm)
    }

    @Test
    fun `rejected popup submission reports failure so popup can remain open`() {
        val draft = SkedEditorState(
            target = member("DL1ABC", 2),
            minutes = 5,
            band = null,
            mode = "SSB",
            remindPm = false,
            reminderOffsets = "2+1",
            submitter = { false },
        )

        assertFalse(draft.submit())
    }

    private fun member(callSign: String, category: Int) = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(category)
    }
}
