package kst4contest.view.compose.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.tabs.StationTabState

/**
 * The setup wizard's answers about antenna and power.
 *
 * These numbers have no empty state: every answer starts as the stored value and is a
 * proposal the operator may pass unchanged, also when a station is set up anew. They feed the
 * path analysis and the antenna sector on the map; the chat does not need them.
 *
 * The site elevation is the exception in form, not in rule: it is text, because empty is a
 * value of its own -- "let the elevation model decide" -- and it is read exactly as the
 * Station tab reads it.
 *
 * @param prefs the profile's preferences, already read from its file
 */
class WizardAntenna(prefs: ChatPreferences) {

    var beamWidthDeg: Double by mutableStateOf(prefs.getStn_antennaBeamWidthDeg())
    var gainDbi: Double by mutableStateOf(prefs.getStn_pathAnalysisOwnAntennaGainDbi())
    var heightMeters: Double by mutableStateOf(prefs.getStn_pathAnalysisOwnAntennaHeightMeters())
    var txPowerWatts: Double by mutableStateOf(prefs.getStn_pathAnalysisOwnTxPowerWatts())

    /** The site elevation above sea level as the operator types it; empty for the model. */
    var heightAslText: String by mutableStateOf(elevationText(prefs.getStn_pathAnalysisOwnHeightAslMeters()))

    /** A site elevation that is neither empty nor a number in the accepted range. */
    val heightAslRefused: Boolean
        get() = StationTabState.parseOwnHeightAsl(heightAslText) == null

    /** Whether every answer can be stored as it stands. */
    val complete: Boolean
        get() = beamWidthDeg.isFinite() && gainDbi.isFinite() && heightMeters.isFinite() &&
            txPowerWatts.isFinite() && !heightAslRefused

    /** Puts the answers into [prefs]. Call only while [complete]. */
    fun writeTo(prefs: ChatPreferences) {
        prefs.setStn_antennaBeamWidthDeg(beamWidthDeg)
        prefs.setStn_pathAnalysisOwnAntennaGainDbi(gainDbi)
        prefs.setStn_pathAnalysisOwnAntennaHeightMeters(heightMeters)
        prefs.setStn_pathAnalysisOwnTxPowerWatts(txPowerWatts)
        prefs.setStn_pathAnalysisOwnHeightAslMeters(
            StationTabState.parseOwnHeightAsl(heightAslText) ?: Double.NaN
        )
    }

    private companion object {
        /** Empty for "unset"; a whole number without ".0", so the field shows what one types. */
        fun elevationText(meters: Double): String = when {
            meters.isNaN() -> ""
            meters == Math.floor(meters) && !meters.isInfinite() -> meters.toLong().toString()
            else -> meters.toString()
        }
    }
}
