package kst4contest.view.compose

import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import java.util.Locale
import java.util.function.Predicate

/** Builds the selected-station message filter without changing the source history. */
object SelectedStationMessagePredicate {

    @JvmStatic
    fun forSelection(
        filter: SelectedMessageFilter,
        selected: ChatMember?,
        ownCallSign: String?,
    ): Predicate<ChatMessage> {
        val selectedIdentity = StationIdentity.of(selected)
        val ownCall = ownCallSign.normalizedCall()

        return Predicate { message ->
            if (selectedIdentity?.categoryNumber == null) {
                return@Predicate false
            }

            val senderCall = message?.sender?.callSign.normalizedCall()
            val receiverCall = message?.receiver?.callSign.normalizedCall()
            val messageCategory = message?.chatCategory?.categoryNumber
            if (senderCall.isBlank() || receiverCall.isBlank() ||
                messageCategory != selectedIdentity.categoryNumber
            ) {
                return@Predicate false
            }

            val selectedIsSender = senderCall == selectedIdentity.callSign
            val selectedIsReceiver = receiverCall == selectedIdentity.callSign
            val ownIsSender = senderCall == ownCall
            val ownIsReceiver = receiverCall == ownCall
            val isPublic = message.isMessageDirectedToCommunity || receiverCall == PUBLIC_RECEIVER

            when (filter) {
                SelectedMessageFilter.PM_TO_ME ->
                    ownCall.isNotBlank() && (
                        (!isPublic && ((selectedIsSender && ownIsReceiver) ||
                            (ownIsSender && selectedIsReceiver))) ||
                            (isPublic && selectedIsSender && message.isMessageDirectedToMe)
                        )

                SelectedMessageFilter.PM_TO_OTHER ->
                    ownCall.isNotBlank() && selectedIsSender && !isPublic && !ownIsReceiver

                SelectedMessageFilter.PUBLIC -> selectedIsSender && isPublic
                SelectedMessageFilter.NOTHING -> selectedIsSender || selectedIsReceiver
            }
        }
    }

    private fun String?.normalizedCall(): String = this?.trim()?.uppercase(Locale.ROOT).orEmpty()

    private const val PUBLIC_RECEIVER = "ALL"
}
