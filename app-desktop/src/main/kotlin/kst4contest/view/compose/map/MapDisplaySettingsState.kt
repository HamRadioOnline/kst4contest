package kst4contest.view.compose.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatPreferences

/** Compose-observable map geometry read from the active profile. */
internal class MapDisplaySettingsState(private val prefs: ChatPreferences) {

    var antennaBeamWidthDeg: Double by mutableStateOf(prefs.stn_antennaBeamWidthDeg)
        private set

    var maxQrbKm: Double by mutableStateOf(prefs.stn_maxQRBDefault)
        private set

    fun refreshFromPreferences() {
        antennaBeamWidthDeg = prefs.stn_antennaBeamWidthDeg
        maxQrbKm = prefs.stn_maxQRBDefault
    }
}
