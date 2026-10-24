package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.Band
import kst4contest.model.ChatMember

/** Complete immutable request handed to the shared sked creation action. */
data class SkedRequest(
    val target: ChatMember,
    val minutes: Int,
    val band: Band?,
    val mode: String?,
    val remindPm: Boolean,
    val reminderOffsets: String,
)

/**
 * Isolated editor draft used by the station-context popup.
 *
 * The target is captured when the popup opens. Later station selections therefore do
 * not redirect a partially edited sked.
 */
class SkedEditorState(
    val target: ChatMember,
    minutes: Int,
    band: Band?,
    mode: String?,
    remindPm: Boolean,
    reminderOffsets: String,
    private val submitter: (SkedRequest) -> Boolean,
) {
    var minutes by mutableStateOf(minutes)
    var band by mutableStateOf(band)
    var mode by mutableStateOf(mode)
    var remindPm by mutableStateOf(remindPm)
    var reminderOffsets by mutableStateOf(reminderOffsets)

    fun request(): SkedRequest = SkedRequest(
        target = target,
        minutes = minutes,
        band = band,
        mode = mode,
        remindPm = remindPm,
        reminderOffsets = reminderOffsets,
    )

    fun submit(): Boolean = submitter(request())
}
