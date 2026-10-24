package kst4contest.view.compose

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kst4contest.model.Band
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StationContextMenuStateTest {

    @Test
    fun `single row target is selected immediately with its context source`() {
        val selections = mutableListOf<Pair<ChatMember, StationSelectionSource>>()
        val state = StationContextMenuState { member, source -> selections += member to source }
        val member = member("DL1ABC", 2)

        state.open(listOf(member), StationSelectionSource.STATION_CONTEXT_MENU, IntOffset(10, 20))

        assertSame(member, state.target)
        assertEquals(listOf(member to StationSelectionSource.STATION_CONTEXT_MENU), selections)
    }

    @Test
    fun `grouped map target requires an explicit full callsign and category choice`() {
        val selections = mutableListOf<Pair<ChatMember, StationSelectionSource>>()
        val state = StationContextMenuState { member, source -> selections += member to source }
        val main = member("DN9APW-2", 2)
        val second = member("DN9APW-2", 3)

        state.open(
            listOf(main, second, main),
            StationSelectionSource.MAP_CONTEXT_MENU,
            IntOffset(30, 40),
        )

        assertNull(state.target)
        assertEquals(2, state.candidates.size)
        assertEquals(emptyList<Pair<ChatMember, StationSelectionSource>>(), selections)

        state.chooseTarget(second)

        assertSame(second, state.target)
        assertEquals(listOf(second to StationSelectionSource.MAP_CONTEXT_MENU), selections)
    }

    @Test
    fun `target label retains the full suffix and category`() {
        val target = member("DN9APW-70", 3)

        val label = stationContextTargetLabel(target)

        assertTrue(label.startsWith("DN9APW-70 | "))
    }

    @Test
    fun `worked menu state follows the existing per-band flags`() {
        val target = member("DL1ABC", 2)
        target.isWorked144 = true

        assertTrue(target.isWorkedOn(Band.B_144))
        assertFalse(target.isWorkedOn(Band.B_432))
    }

    @Test
    fun `popup anchor is clamped at the window edge`() {
        val provider = WindowPointPopupPositionProvider(IntOffset(990, 790))

        val position = provider.calculatePosition(
            anchorBounds = IntRect.Zero,
            windowSize = IntSize(1_000, 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(300, 250),
        )

        assertEquals(IntOffset(700, 550), position)
    }

    private fun member(callSign: String, category: Int) = ChatMember().apply {
        this.callSign = callSign
        chatCategory = ChatCategory(category)
    }
}
