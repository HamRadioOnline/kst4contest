package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The filters the station list can have switched on.
 *
 * One entry per predicate the JavaFX bar added to the controller. The controller does
 * the filtering — it has since Etappe 2 — so this names which predicates should be
 * attached, and nothing more.
 */
enum class StationFilter {
    ONLY_NEW_GRIDS,
    TROPO_REACHABLE,
    NEW_BANDS,
    AIRSCOUT_NEXT_5_MIN,
    MAX_QRB,
    MIN_QRB,
    QTF,
    SEARCH_TEXT,
    HIDE_INACTIVE,
    HIDE_WORKED_ANY,
    HIDE_WORKED_50,
    HIDE_WORKED_70,
    HIDE_WORKED_144,
    HIDE_WORKED_432,
    HIDE_WORKED_23,
    HIDE_WORKED_13,
    HIDE_WORKED_9,
    HIDE_WORKED_6,
    HIDE_WORKED_3,
}

/**
 * What a QRB field of the filter bar makes of the text typed into it: a distance in km, or
 * `null` when the text is none.
 *
 * `toDouble` alone also reads "NaN" and "Infinity", and a pasted "Infinity" as the minimum
 * hid every station while "NaN" let every one through. A negative distance means nothing
 * either. All of these are refused, so the field keeps its last valid value (#21).
 */
internal fun parseQrbKm(text: String): Double? =
    text.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

/** A deliberate antenna/filter heading in the convention used by the QTF display. */
internal fun parseQtfDegrees(text: String): Double? =
    text.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..360.0 }

/** The bands the "hide worked" row offers, under the labels the bar shows. */
enum class WorkedBandFilter(val label: String, val filter: StationFilter, val modelBand: kst4contest.model.Band) {
    B_50("50", StationFilter.HIDE_WORKED_50, kst4contest.model.Band.B_50),
    B_70("70", StationFilter.HIDE_WORKED_70, kst4contest.model.Band.B_70),
    B_144("144", StationFilter.HIDE_WORKED_144, kst4contest.model.Band.B_144),
    B_432("432", StationFilter.HIDE_WORKED_432, kst4contest.model.Band.B_432),
    B_23("23", StationFilter.HIDE_WORKED_23, kst4contest.model.Band.B_1296),
    B_13("13", StationFilter.HIDE_WORKED_13, kst4contest.model.Band.B_2320),
    B_9("9", StationFilter.HIDE_WORKED_9, kst4contest.model.Band.B_3400),
    B_6("6", StationFilter.HIDE_WORKED_6, kst4contest.model.Band.B_5760),
    B_3("3", StationFilter.HIDE_WORKED_3, kst4contest.model.Band.B_10G),
}

/**
 * The eight bearing buttons, with the degrees each writes into the QTF field.
 *
 * They were one ToggleGroup, so only one can be picked, and picking one filters
 * nothing by itself — it sets a bearing the QTF filter uses once that is switched on.
 */
enum class Bearing(val degrees: Double) {
    N(0.0), NE(45.0), E(90.0), SE(135.0), S(180.0), SW(225.0), W(270.0), NW(315.0)
}

/**
 * What the filter bar has switched on.
 *
 * Deliberately free of the drawing: its 25 toggles are not 25 states, and which of
 * them mean what is worth pinning where it can be tested. Getting it wrong puts a
 * station in front of the operator that their filter excludes, or hides one it does
 * not — and in a contest that is a missed contact either way.
 */
class StationFilterState {

    private val switched = mutableStateMapOf<StationFilter, Boolean>()

    var onlyNewGrids: Boolean
        get() = isOn(StationFilter.ONLY_NEW_GRIDS)
        set(value) = switch(StationFilter.ONLY_NEW_GRIDS, value)

    var tropoReachable: Boolean
        get() = isOn(StationFilter.TROPO_REACHABLE)
        set(value) = switch(StationFilter.TROPO_REACHABLE, value)

    var newBands: Boolean
        get() = isOn(StationFilter.NEW_BANDS)
        set(value) = switch(StationFilter.NEW_BANDS, value)

    var airScoutNext5Min: Boolean
        get() = isOn(StationFilter.AIRSCOUT_NEXT_5_MIN)
        set(value) = switch(StationFilter.AIRSCOUT_NEXT_5_MIN, value)

    var hideInactive: Boolean
        get() = isOn(StationFilter.HIDE_INACTIVE)
        set(value) = switch(StationFilter.HIDE_INACTIVE, value)

    var hideWorkedAny: Boolean
        get() = isOn(StationFilter.HIDE_WORKED_ANY)
        set(value) = switch(StationFilter.HIDE_WORKED_ANY, value)

    var maxQrbEnabled: Boolean
        get() = isOn(StationFilter.MAX_QRB)
        set(value) = switch(StationFilter.MAX_QRB, value)

    /** Only meaningful while [maxQrbEnabled]; a distance alone filters nothing. */
    var maxQrbKm: Double by mutableStateOf(0.0)

    var minQrbEnabled: Boolean
        get() = isOn(StationFilter.MIN_QRB)
        set(value) = switch(StationFilter.MIN_QRB, value)

    /**
     * Only meaningful while [minQrbEnabled]. Exclusive: a station exactly this far away is
     * hidden, so that "QRB > 300" and "QRB <= 300" split the list without overlap (#21).
     */
    var minQrbKm: Double by mutableStateOf(0.0)

    var qtfEnabled: Boolean
        get() = isOn(StationFilter.QTF)
        set(value) = switch(StationFilter.QTF, value)

    var qtfDegrees: Double by mutableStateOf(0.0)

    var bearing: Bearing? by mutableStateOf(null)

    /**
     * Colours the locator cell by grid status. **Not a filter** — the JavaFX toggle
     * sat among the filter buttons but only redrew the table, and a station must not
     * disappear because of it.
     */
    var gridColouring: Boolean by mutableStateOf(false)

    private var searchTextValue: String by mutableStateOf("")

    var searchText: String
        get() = searchTextValue
        set(value) {
            searchTextValue = value
            /* Blank is not a search: leaving it on would hide every station. */
            switch(StationFilter.SEARCH_TEXT, value.isNotBlank())
        }

    /** The filters that should be attached to the controller right now. */
    val activeFilters: Set<StationFilter>
        get() = switched.filterValues { it }.keys

    fun isOn(filter: StationFilter): Boolean = switched[filter] == true

    /**
     * Whether a station [qrb] km away passes the distance filters that are switched on.
     *
     * Both on make a range: `min < qrb <= max`. A station without a known distance is
     * hidden as soon as either is on — `null` is unknown, not zero, and an unknown distance
     * can be in neither range.
     */
    fun acceptsQrb(qrb: Double?): Boolean {
        val max = isOn(StationFilter.MAX_QRB)
        val min = isOn(StationFilter.MIN_QRB)
        if (!max && !min) return true
        if (qrb == null) return false
        if (max && qrb > maxQrbKm) return false
        if (min && qrb <= minQrbKm) return false
        return true
    }

    fun setHideWorkedOn(band: WorkedBandFilter, hide: Boolean) = switch(band.filter, hide)

    fun isHideWorkedOn(band: WorkedBandFilter): Boolean = isOn(band.filter)

    /** Picks one bearing and writes its degrees, or deselects if already selected.
     * Mirrors JavaFX ToggleGroup behavior which also toggles the QTF filter checkbox. */
    fun toggleBearing(direction: Bearing) {
        if (bearing == direction) {
            bearing = null
            qtfEnabled = false
        } else {
            bearing = direction
            qtfDegrees = direction.degrees
            qtfEnabled = true
        }
    }

    private fun switch(filter: StationFilter, on: Boolean) {
        if (on) switched[filter] = true else switched.remove(filter)
    }

    fun reset() {
        switched.clear()
        searchText = ""
        qtfDegrees = 0.0
        bearing = null
        gridColouring = false
    }
}
