package kst4contest.view.compose

import kst4contest.model.BeaconStatusSnapshot
import kst4contest.model.BeaconStatusSnapshot.AggregateState
import kst4contest.model.BeaconStatusSnapshot.CategoryStatus
import kst4contest.model.BeaconStatusSnapshot.InactiveReason
import kst4contest.view.i18n.CurrentStrings
import kst4contest.view.i18n.Strings

enum class ThreadStatusVisualStyle {
    DEFAULT,
    BEACON_OFF,
    BEACON_ACTIVE,
    BEACON_FAILED,
}

data class BeaconStatusPresentation(
    val label: String,
    val tooltip: String,
    val style: ThreadStatusVisualStyle,
) {
    companion object {
        fun from(
            snapshot: BeaconStatusSnapshot,
            strings: Strings = CurrentStrings.get(),
        ): BeaconStatusPresentation {
            val label = when (snapshot.aggregateState) {
                AggregateState.OFF -> "off"
                AggregateState.PARTIAL -> "1/2"
                AggregateState.ON -> "on"
                AggregateState.FAILED -> "FAILED"
            }
            val style = when (snapshot.aggregateState) {
                AggregateState.OFF -> ThreadStatusVisualStyle.BEACON_OFF
                AggregateState.PARTIAL, AggregateState.ON -> ThreadStatusVisualStyle.BEACON_ACTIVE
                AggregateState.FAILED -> ThreadStatusVisualStyle.BEACON_FAILED
            }
            val tooltip = listOf(
                strings.beaconStatusMain(describe(snapshot.main, strings)),
                strings.beaconStatusSecond(describe(snapshot.second, strings)),
            ).joinToString("\n")
            return BeaconStatusPresentation(label, tooltip, style)
        }

        private fun describe(category: CategoryStatus, strings: Strings): String {
            if (!category.isAvailable()) return strings.beaconStatusUnavailable
            if (category.isFailed()) return strings.beaconStatusFailed
            if (category.isEnabled()) return strings.beaconStatusEnabled
            return when (category.inactiveReason) {
                InactiveReason.NOT_ACTIVATED_THIS_SESSION -> strings.beaconReasonSession
                InactiveReason.USER_DISABLED -> strings.beaconReasonUser
                InactiveReason.PRIMARY_QRG_UNAVAILABLE -> strings.beaconReasonPrimaryQrg
                InactiveReason.SECONDARY_QRG_UNAVAILABLE -> strings.beaconReasonSecondaryQrg
                InactiveReason.AUTOMATIC_QRG_CHANGE -> strings.beaconReasonAutomaticQrg
            }
        }
    }
}
