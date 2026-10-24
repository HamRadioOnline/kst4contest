package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import kst4contest.model.ChatMember

/** Current target and window-relative anchor of a station context menu. */
class StationContextMenuState(
    private val selectStation: (ChatMember, StationSelectionSource) -> Unit,
) {
    var position: IntOffset by mutableStateOf(IntOffset.Zero)
        private set

    var candidates: List<ChatMember> by mutableStateOf(emptyList())
        private set

    var target: ChatMember? by mutableStateOf(null)
        private set

    var source: StationSelectionSource by mutableStateOf(StationSelectionSource.PROGRAMMATIC)
        private set

    val isOpen: Boolean
        get() = candidates.isNotEmpty()

    fun open(
        members: List<ChatMember>,
        source: StationSelectionSource,
        position: IntOffset,
    ) {
        val unique = members
            .filter { StationIdentity.of(it) != null }
            .distinctBy(StationIdentity::of)
        if (unique.isEmpty()) {
            close()
            return
        }

        this.position = position
        this.source = source
        candidates = unique
        target = unique.singleOrNull()
        target?.let { selectStation(it, source) }
    }

    fun chooseTarget(member: ChatMember) {
        val identity = StationIdentity.of(member) ?: return
        val resolved = candidates.firstOrNull { StationIdentity.of(it) == identity } ?: return
        target = resolved
        selectStation(resolved, source)
    }

    fun close() {
        candidates = emptyList()
        target = null
    }
}
