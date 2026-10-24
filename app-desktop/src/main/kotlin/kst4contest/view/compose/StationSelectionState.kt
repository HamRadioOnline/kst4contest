package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import java.util.Locale

/** Full ON4KST station identity used across tables, messages and the map. */
data class StationIdentity(
    val callSign: String,
    val categoryNumber: Int?,
) {
    companion object {
        @JvmStatic
        fun of(member: ChatMember?): StationIdentity? =
            of(member?.callSign, member?.chatCategory?.categoryNumber)

        @JvmStatic
        fun of(callSignValue: String?, categoryNumber: Int?): StationIdentity? {
            val callSign = callSignValue?.trim()?.uppercase(Locale.ROOT).orEmpty()
            if (callSign.isBlank()) return null

            return StationIdentity(callSign, categoryNumber)
        }
    }
}

/** Resolves the station navigated to by a directed-message row click. */
object DirectedMessageStationTarget {

    @JvmStatic
    fun forMessage(message: ChatMessage?, ownCallSign: String?): StationIdentity? {
        val senderCallSign = message?.sender?.callSign ?: return null
        val ownCall = ownCallSign?.trim()?.uppercase(Locale.ROOT).orEmpty()
        val senderCall = senderCallSign.trim().uppercase(Locale.ROOT)
        val targetCall = if (senderCall == ownCall) {
            message.receiver?.callSign ?: receiverFromPayload(message.messageText)
        } else {
            senderCallSign
        }

        return StationIdentity.of(targetCall, message.chatCategory?.categoryNumber)
    }

    private fun receiverFromPayload(messageText: String?): String? {
        if (messageText == null || !messageText.startsWith('(')) return null
        val closing = messageText.indexOf(')')
        return if (closing > 1) messageText.substring(1, closing).trim() else null
    }
}

/** Where an explicit station selection came from. */
enum class StationSelectionSource {
    STATION_TABLE,
    STATION_CONTEXT_MENU,
    DIRECTED_MESSAGE,
    PUBLIC_MESSAGE,
    QSO_OF_THE_OTHER,
    MAP,
    MAP_CONTEXT_MENU,
    PRIORITY,
    NEW_JOINED,
    TIMELINE,
    RESET_VIEW,
    PROGRAMMATIC,
}

/**
 * Canonical station selection shared by the station table and detail views.
 *
 * Data refreshes reconcile the selected instance without moving the table. Explicit
 * user navigation increments [scrollRequestRevision], even when the identity is the
 * same as before.
 */
class StationSelectionState : TableSelectionModel<ChatMember> {

    private var current: ChatMember? by mutableStateOf(null)

    override val selected: ChatMember?
        get() = current

    override var selectionRevision: Long by mutableStateOf(0L)
        private set

    override var scrollRequestRevision: Long by mutableStateOf(0L)
        private set

    var lastSource: StationSelectionSource by mutableStateOf(StationSelectionSource.PROGRAMMATIC)
        private set

    val identity: StationIdentity?
        get() = StationIdentity.of(current)

    override fun select(row: ChatMember?) {
        select(row, StationSelectionSource.STATION_TABLE)
    }

    fun select(row: ChatMember?, source: StationSelectionSource) {
        current = row
        lastSource = source
        selectionRevision++
        if (row != null && source.requestsStationTableScroll()) {
            scrollRequestRevision++
        }
    }

    override fun reconcile(row: ChatMember?) {
        current = row
        selectionRevision++
    }

    override fun requestScroll() {
        if (current != null) {
            scrollRequestRevision++
        }
    }
}

private fun StationSelectionSource.requestsStationTableScroll(): Boolean = when (this) {
    StationSelectionSource.STATION_TABLE,
    StationSelectionSource.STATION_CONTEXT_MENU,
    StationSelectionSource.RESET_VIEW,
    StationSelectionSource.PROGRAMMATIC -> false

    StationSelectionSource.DIRECTED_MESSAGE,
    StationSelectionSource.PUBLIC_MESSAGE,
    StationSelectionSource.QSO_OF_THE_OTHER,
    StationSelectionSource.MAP,
    StationSelectionSource.MAP_CONTEXT_MENU,
    StationSelectionSource.PRIORITY,
    StationSelectionSource.NEW_JOINED,
    StationSelectionSource.TIMELINE -> true
}
