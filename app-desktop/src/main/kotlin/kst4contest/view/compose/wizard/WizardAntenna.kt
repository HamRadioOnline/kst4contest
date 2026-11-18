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

    private val storedBeamWidthDeg = prefs.getStn_antennaBeamWidthDeg()
    private val storedGainDbi = prefs.getStn_pathAnalysisOwnAntennaGainDbi()
    // The XML reader assigns the field directly without clamping, whereas the setter and path analysis treat < 0 as 0.
    private val storedHeightMeters = prefs.getStn_pathAnalysisOwnAntennaHeightMeters().coerceAtLeast(0.0)
    private val storedTxPowerWatts = prefs.getStn_pathAnalysisOwnTxPowerWatts()
    private val storedHeightAslText = elevationText(prefs.getStn_pathAnalysisOwnHeightAslMeters())

    var beamWidthDeg: Double by mutableStateOf(storedBeamWidthDeg)
    var gainDbi: Double by mutableStateOf(storedGainDbi)
    var heightMeters: Double by mutableStateOf(storedHeightMeters)
    var txPowerWatts: Double by mutableStateOf(storedTxPowerWatts)

    /** The site elevation above sea level as the operator types it; empty for the model. */
    var heightAslText: String by mutableStateOf(storedHeightAslText)

    /**
     * Puts every answer back to what the profile holds. For the moment the operator changes
     * his mind about where the station comes from: what he typed for one choice must not
     * show in the summary of the other, where it would not be written.
     */
    fun loadStored() {
        beamWidthDeg = storedBeamWidthDeg
        gainDbi = storedGainDbi
        heightMeters = storedHeightMeters
        txPowerWatts = storedTxPowerWatts
        heightAslText = storedHeightAslText
    }

    /** A site elevation that is neither empty nor a number in the accepted range. */
    val heightAslRefused: Boolean
        get() = StationTabState.parseOwnHeightAsl(heightAslText) == null

    /** Whether every answer can be stored as it stands. */
    val complete: Boolean
        get() = beamWidthDeg.isFinite() && gainDbi.isFinite() && heightMeters.isFinite() &&
            heightMeters >= 0.0 && txPowerWatts.isFinite() && !heightAslRefused

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
