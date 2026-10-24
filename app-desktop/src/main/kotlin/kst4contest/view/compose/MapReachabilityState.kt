package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.view.map.PathAnalysisResult

/**
 * Explicit map reachability request shared by station-list and map context menus.
 * A monotonically increasing request ID prevents older callbacks from replacing a
 * result for a newer station or band.
 */
class MapReachabilityState(
    private val ownLocator: String,
    requester: (ChatMember, Band, (PathAnalysisResult) -> Unit) -> Unit = { _, _, _ -> },
) {
    var requester: (ChatMember, Band, (PathAnalysisResult) -> Unit) -> Unit = requester

    var result: PathAnalysisResult? by mutableStateOf(null)
        private set

    var running: Boolean by mutableStateOf(false)
        private set

    var requestRevision: Long by mutableStateOf(0L)
        private set

    var targetIdentity: StationIdentity? by mutableStateOf(null)
        private set

    var requestedBand: Band? by mutableStateOf(null)
        private set

    fun calculate(member: ChatMember?, band: Band?): Boolean {
        if (member == null || band == null) return false
        val identity = StationIdentity.of(member) ?: return false
        val requestId = ++requestRevision
        targetIdentity = identity
        requestedBand = band
        running = true
        result = PathAnalysisResult.loading(ownLocator, member.qra, member.callSignRaw)

        try {
            requester(member, band) { completed ->
                if (requestRevision == requestId
                    && targetIdentity == identity
                    && requestedBand == band
                ) {
                    result = completed
                    running = false
                }
            }
        } catch (exception: RuntimeException) {
            if (requestRevision == requestId) {
                result = PathAnalysisResult.noProfile(
                    "Failed",
                    ownLocator,
                    member.qra,
                    member.callSignRaw,
                    Double.NaN,
                    member.getQTFdirection() ?: Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    band.defaultAnalysisFrequencyMHz,
                    exception.message ?: "Reachability calculation failed.",
                )
                running = false
            }
        }
        return true
    }

    fun selectionChanged(identity: StationIdentity?) {
        if (targetIdentity == null || targetIdentity == identity) return
        requestRevision++
        targetIdentity = null
        requestedBand = null
        result = null
        running = false
    }
}
