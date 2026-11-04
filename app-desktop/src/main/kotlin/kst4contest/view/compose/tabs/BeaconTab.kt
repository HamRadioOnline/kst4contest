package kst4contest.view.compose.tabs

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form
import kst4contest.view.compose.observeAsState

/**
 * The beacon tab: one CQ template per chat category and the interval they share.
 *
 * The interval is one field for both categories. The second category keeps its own
 * stored value for the sake of the configuration file, but the operator never saw
 * two intervals and setting them apart was never possible.
 */
@Composable
fun BeaconTab(state: BeaconTabState, onRefused: (String) -> Unit) {
    val strings = LocalStrings.current
    val mainEnabled by state.mainBeaconEnabled.observeAsState()
    val secondEnabled by state.secondBeaconEnabled.observeAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section(strings.beaconSectionFor(state.mainCategoryName)) {
            Form.check(strings.beaconEnabled, mainEnabled == true) {
                state.bcn_beaconsEnabledMainCat = it
            }
            Form.committed(
                label = strings.beaconMessage,
                stored = state.beaconTextMainCat,
                commit = state::commitBeaconTextMainCat,
                onRefused = onRefused,
                current = { state.beaconTextMainCat },
            )
        }

        Form.section(strings.beaconSectionFor(state.secondCategoryName)) {
            Form.check(strings.beaconEnabled, secondEnabled == true) {
                state.bcn_beaconsEnabledSecondCat = it
            }
            Form.committed(
                label = strings.beaconMessage,
                stored = state.beaconTextSecondCat,
                commit = state::commitBeaconTextSecondCat,
                onRefused = onRefused,
                current = { state.beaconTextSecondCat },
            )
        }

        Form.section(strings.beaconIntervalSection) {
            Form.committed(
                label = strings.beaconIntervalMinutes,
                stored = state.beaconIntervalMinutes.toString(),
                commit = state::commitBeaconInterval,
                onRefused = onRefused,
                /*
                 * Compared as the number commitBeaconInterval stores, so " 9" or "09" after
                 * Enter does not count as a second edit and restart the timer again.
                 */
                isUnchanged = { typed, now -> typed.trim().toIntOrNull()?.toString() == now },
                current = { state.beaconIntervalMinutes.toString() },
            )
        }
    }
}
