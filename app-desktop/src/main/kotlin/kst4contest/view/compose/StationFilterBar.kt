package kst4contest.view.compose

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal const val FILTER_DISTANCE_GROUP_TAG = "filter-distance-group"
internal const val FILTER_DIRECTION_GROUP_TAG = "filter-direction-group"
internal const val FILTER_STATION_GROUP_TAG = "filter-station-group"
internal const val FILTER_WORKED_GROUP_TAG = "filter-worked-group"
internal const val FILTER_PROPAGATION_GROUP_TAG = "filter-propagation-group"

/**
 * The station filter bar above the station table.
 *
 * Compact surfaces keep related filters together while the outer flow rows still wrap
 * against the station panel's actual width.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationFilterBar(
    filters: StationFilterState,
    activeBands: Set<kst4contest.model.Band>,
    reachability: ReachabilityState,
    onChanged: () -> Unit,
    onQtfDirectionChanged: (Double) -> Unit = {},
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .border(Density.HAIRLINE, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        /*
         * ROW 1: Reset | QRB max + min | QTF | bearings
         *
         * It wraps (#21): with both QRB filters the line no longer fits a narrow station
         * pane, and a plain row cut the bearing buttons off at the right edge. Each group
         * stays together on its line; only the QRB pair and the bearings may break up
         * inside their group when even that group is wider than the pane.
         */
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            CompactTextField(
                value = filters.searchText,
                onValueChange = {
                    filters.searchText = it
                    onChanged()
                },
                placeholder = strings.filterFind,
                modifier = Modifier.width(90.dp),
            )

            // Reset filters
            FilterButton(strings.filterReset, on = false, isAccent = true) {
                if (onReset != null) {
                    onReset()
                } else {
                    filters.reset()
                    onChanged()
                }
            }

            // Distance and bearing-width controls form one functional surface.
            FilterGroupSurface(FILTER_DISTANCE_GROUP_TAG) {
                FilterGroup {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterGroup {
                            CompactCheckbox(
                                checked = filters.maxQrbEnabled,
                                onCheckedChange = {
                                    filters.maxQrbEnabled = it
                                    onChanged()
                                },
                            )
                            Text(strings.filterShowOnlyQrb, style = compactControlTextStyle())
                            CompactTextField(
                                value = if (filters.maxQrbKm.rem(1) == 0.0) {
                                    filters.maxQrbKm.toInt().toString()
                                } else {
                                    filters.maxQrbKm.toString()
                                },
                                onValueChange = {
                                    val parsed = parseQrbKm(it)
                                    if (parsed != null) {
                                        filters.maxQrbKm = parsed
                                        if (filters.maxQrbEnabled) onChanged()
                                    } else if (it.isEmpty()) {
                                        filters.maxQrbKm = 0.0
                                        if (filters.maxQrbEnabled) onChanged()
                                    }
                                },
                                modifier = Modifier.width(50.dp),
                            )
                        }
                        FilterGroup {
                            CompactCheckbox(
                                checked = filters.minQrbEnabled,
                                onCheckedChange = {
                                    filters.minQrbEnabled = it
                                    onChanged()
                                },
                            )
                            Text(strings.filterShowOnlyQrbAbove, style = compactControlTextStyle())
                            CompactTextField(
                                value = if (filters.minQrbKm.rem(1) == 0.0) {
                                    filters.minQrbKm.toInt().toString()
                                } else {
                                    filters.minQrbKm.toString()
                                },
                                onValueChange = {
                                    val parsed = parseQrbKm(it)
                                    if (parsed != null) {
                                        filters.minQrbKm = parsed
                                        if (filters.minQrbEnabled) onChanged()
                                    } else if (it.isEmpty()) {
                                        filters.minQrbKm = 0.0
                                        if (filters.minQrbEnabled) onChanged()
                                    }
                                },
                                modifier = Modifier.width(50.dp),
                            )
                        }
                    }
                }
            }

            FilterGroupSurface(FILTER_DIRECTION_GROUP_TAG) {
                FilterGroup {
                    CompactCheckbox(
                        checked = filters.qtfEnabled,
                        onCheckedChange = {
                            filters.qtfEnabled = it
                            onChanged()
                        },
                    )
                    Text(strings.filterShowOnlyQtf, style = compactControlTextStyle())
                    CompactTextField(
                        value = if (filters.qtfDegrees.rem(1) == 0.0) {
                            filters.qtfDegrees.toInt().toString()
                        } else {
                            filters.qtfDegrees.toString()
                        },
                        onValueChange = {
                            val parsed = parseQtfDegrees(it)
                            if (parsed != null) {
                                filters.qtfDegrees = parsed
                                filters.bearing = null
                                onQtfDirectionChanged(parsed)
                                if (filters.qtfEnabled) onChanged()
                            } else if (it.isEmpty()) {
                                filters.qtfDegrees = 0.0
                                filters.bearing = null
                                if (filters.qtfEnabled) onChanged()
                            }
                        },
                        modifier = Modifier.width(50.dp),
                    )
                    Text("± BW/2", style = compactControlTextStyle())
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Bearing.entries.forEach { bearing ->
                        FilterButton(
                            bearing.name,
                            on = filters.bearing == bearing,
                            modifier = Modifier.width(Density.DIRECTION_BUTTON_WIDTH),
                            horizontalPadding = Density.DIRECTION_BUTTON_HORIZONTAL_PADDING,
                        ) {
                            filters.toggleBearing(bearing)
                            if (filters.bearing == bearing) {
                                onQtfDirectionChanged(bearing.degrees)
                            }
                            onChanged()
                        }
                    }
                }
            }

            FilterGroupSurface(FILTER_STATION_GROUP_TAG) {
                Text(strings.filterHide, style = compactControlTextStyle())
                FilterButton("wkdany", on = filters.hideWorkedAny) {
                    filters.hideWorkedAny = !filters.hideWorkedAny
                    onChanged()
                }
                WorkedBandFilter.entries.forEach { band ->
                    if (band.modelBand in activeBands) {
                        FilterButton(band.label, on = filters.isHideWorkedOn(band)) {
                            filters.setHideWorkedOn(band, !filters.isHideWorkedOn(band))
                            onChanged()
                        }
                    }
                }
                FilterButton(strings.filterInactiveStations, on = filters.hideInactive) {
                    filters.hideInactive = !filters.hideInactive
                    onChanged()
                }
            }

            FilterGroupSurface(FILTER_WORKED_GROUP_TAG) {
                Text(strings.filterShow, style = compactControlTextStyle())
                FilterButton(strings.filterOnlyNewGrids, on = filters.onlyNewGrids) {
                    filters.onlyNewGrids = !filters.onlyNewGrids
                    onChanged()
                }
                FilterButton(strings.filterGridColour, on = filters.gridColouring) {
                    filters.gridColouring = !filters.gridColouring
                }
                FilterButton(strings.filterTropo, on = filters.tropoReachable) {
                    filters.tropoReachable = !filters.tropoReachable
                    onChanged()
                }
                FilterButton(strings.filterNewBands, on = filters.newBands) {
                    filters.newBands = !filters.newBands
                    onChanged()
                }
                FilterButton("AS next 5m", on = filters.airScoutNext5Min) {
                    filters.airScoutNext5Min = !filters.airScoutNext5Min
                    onChanged()
                }
            }

            FilterGroupSurface(FILTER_PROPAGATION_GROUP_TAG) {
                FilterGroup {
                    Text(strings.filterReachability, style = compactControlTextStyle())
                    Form.inlineChoice(
                        label = "",
                        items = reachability.options,
                        selected = reachability.selectedOption,
                        describe = ReachabilityBandOption::label,
                        onSelect = { reachability.selectBand(it.band) },
                    )
                    Form.button(
                        text = strings.filterCalcSelected,
                        enabled = reachability.canCalculate,
                        onClick = { reachability.calculateSelected() },
                    )
                }
            }
        }
    }
}

/**
 * A compact toggle using the same interaction states as every other desktop button.
 */
@Composable
private fun FilterButton(
    label: String,
    on: Boolean,
    isAccent: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 6.dp,
    onClick: () -> Unit,
) {
    Form.button(
        text = label,
        enabled = enabled,
        accented = isAccent,
        selected = on,
        modifier = modifier,
        horizontalPadding = horizontalPadding,
        onClick = onClick,
    )
}

/** Controls that belong together and move to the next line of a wrapping row as one. */
@Composable
private fun FilterGroup(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/** A compact surface that keeps related filters visually and spatially together. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroupSurface(tag: String, content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier
            .testTag(tag)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .border(Density.HAIRLINE, MaterialTheme.colorScheme.outlineVariant, Density.BUTTON_SHAPE)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}
