package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.Band
import kst4contest.model.ChatMember

/** One selectable entry in the reachability-band control. A null band means Auto. */
data class ReachabilityBandOption(val band: Band?, val label: String)

/**
 * Operator-controlled reachability selection and the one explicit calculation action.
 *
 * Band selection only invalidates cached table/filter presentation. Online terrain work
 * stays behind [calculateSelected], so recomposition and sorting cannot consume API calls.
 */
class ReachabilityState(
    activeBands: Set<Band>,
    private val resolveAutoBand: (ChatMember) -> Band? = { null },
    private val selectedStation: () -> ChatMember? = { null },
    private val calculate: (ChatMember, Band) -> Unit = { _, _ -> },
    private val refresh: () -> Unit = { },
) {
    private val supportedBands = activeBands.toSet()

    val options: List<ReachabilityBandOption> =
        listOf(ReachabilityBandOption(null, "Auto")) +
            activeBands.sortedBy(Band::ordinal).map { band ->
                ReachabilityBandOption(band, band.displayLabel)
            }

    var bandOverride: Band? by mutableStateOf(null)
        private set

    val selectedOption: ReachabilityBandOption
        get() = options.first { it.band == bandOverride }

    val canCalculate: Boolean
        get() = selectedStation()?.let(::resolveBand) != null

    fun selectBand(band: Band?) {
        val accepted = band?.takeIf(supportedBands::contains)
        if (bandOverride == accepted) return
        bandOverride = accepted
        refresh()
    }

    fun resolveBand(member: ChatMember): Band? = bandOverride ?: resolveAutoBand(member)

    /** Returns whether a complete station/band request was handed to the service. */
    fun calculateSelected(): Boolean {
        val member = selectedStation() ?: return false
        val band = resolveBand(member) ?: return false
        calculate(member, band)
        return true
    }
}
