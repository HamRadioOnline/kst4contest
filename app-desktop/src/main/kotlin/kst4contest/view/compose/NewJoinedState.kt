package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatMember
import kst4contest.model.RecentChatJoin

/** One selectable station in the transient New joined group. */
class NewJoinedEntry internal constructor(
    val identity: StationIdentity,
    val label: String,
    val joinedAtEpochMillis: Long,
    private val member: ChatMember,
    private val onSelect: (ChatMember) -> Unit,
) {
    fun select() = onSelect(member)
}

/**
 * Compose-facing view of live ON4KST join events.
 *
 * Initial and resynchronized user lists never reach this state. It receives the core's
 * live-event roster and intersects it with the currently active station roster, so a
 * departed station disappears immediately. The clock is injected to make expiry exact
 * and testable without waiting ten minutes.
 */
class NewJoinedState(
    private val clock: () -> Long = System::currentTimeMillis,
    private val onSelect: (ChatMember) -> Unit,
) {

    private var events: List<RecentChatJoin> = emptyList()
    private var activeMembers: List<ChatMember> = emptyList()
    private var liveEntries by mutableStateOf<List<NewJoinedEntry>>(emptyList())

    val entries: List<NewJoinedEntry>
        get() = liveEntries

    val visibleEntries: List<NewJoinedEntry>
        get() = liveEntries.take(SHOWN)

    val overflowEntries: List<NewJoinedEntry>
        get() = liveEntries.drop(SHOWN)

    fun updateEvents(newEvents: List<RecentChatJoin>) {
        events = newEvents.toList()
        refresh()
    }

    fun updateActiveMembers(newMembers: List<ChatMember>) {
        activeMembers = newMembers.toList()
        refresh()
    }

    /** Removes expired or departed entries and resolves each event to the latest row instance. */
    fun refresh() {
        val now = clock()
        val activeByIdentity = activeMembers.mapNotNull { member ->
            StationIdentity.of(member)?.let { identity -> identity to member }
        }.toMap()

        val firstJoinByIdentity = linkedMapOf<StationIdentity, RecentChatJoin>()
        events.forEach { event ->
            val identity = StationIdentity.of(event.callSign, event.categoryNumber)
                ?: return@forEach
            val existing = firstJoinByIdentity[identity]
            if (existing == null || event.joinedAtEpochMillis < existing.joinedAtEpochMillis) {
                firstJoinByIdentity[identity] = event
            }
        }

        liveEntries = firstJoinByIdentity
            .mapNotNull { (identity, event) ->
                val active = activeByIdentity[identity] ?: return@mapNotNull null
                val ageMillis = now - event.joinedAtEpochMillis
                if (ageMillis < 0L || ageMillis >= VALIDITY_MILLIS) {
                    return@mapNotNull null
                }
                NewJoinedEntry(
                    identity = identity,
                    label = identity.callSign,
                    joinedAtEpochMillis = event.joinedAtEpochMillis,
                    member = active,
                    onSelect = onSelect,
                )
            }
            .sortedByDescending(NewJoinedEntry::joinedAtEpochMillis)
    }

    fun isRecent(member: ChatMember): Boolean {
        val identity = StationIdentity.of(member) ?: return false
        return liveEntries.any { it.identity == identity }
    }

    companion object {
        const val VALIDITY_MILLIS = 10L * 60L * 1000L
        const val SHOWN = 5
    }
}
