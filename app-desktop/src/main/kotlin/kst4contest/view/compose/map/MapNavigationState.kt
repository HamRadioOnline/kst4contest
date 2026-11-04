package kst4contest.view.compose.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.view.compose.StationIdentity

/** A deliberate operator action that may move the station-map camera. */
internal sealed interface MapNavigationRequest {
    data class FitSelectedPath(val identity: StationIdentity) : MapNavigationRequest

    data class CenterOnQtf(val qtfDegrees: Double) : MapNavigationRequest

    data object ResetToHome : MapNavigationRequest
}
/**
 * The latest deliberate map-navigation request.
 *
 * Ordinary model refreshes never write this state. Keeping one monotonically increasing
 * revision makes repeated selections observable and gives the most recent station or QTF
 * action deterministic precedence when several actions happen before the map draws.
 */
internal class MapNavigationState {

    var request: MapNavigationRequest? by mutableStateOf(null)
        private set

    var requestRevision: Long by mutableStateOf(0L)
        private set

    fun fitSelectedPath(identity: StationIdentity) {
        publish(MapNavigationRequest.FitSelectedPath(identity))
    }

    fun centerOnQtf(qtfDegrees: Double) {
        if (!qtfDegrees.isFinite() || qtfDegrees !in 0.0..360.0) return
        publish(MapNavigationRequest.CenterOnQtf(qtfDegrees))
    }

    fun resetToHome() {
        publish(MapNavigationRequest.ResetToHome)
    }

    private fun publish(next: MapNavigationRequest) {
        request = next
        requestRevision++
    }
}
