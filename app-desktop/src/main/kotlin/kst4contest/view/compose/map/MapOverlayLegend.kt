package kst4contest.view.compose.map

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.TooltipCard
import kst4contest.view.i18n.LocalStrings

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun MapOverlayLegend(
    showAntenna: Boolean,
    showSpatialFilters: Boolean,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!showAntenna && !showSpatialFilters) return

    val strings = LocalStrings.current
    TooltipArea(
        tooltip = { TooltipCard(strings.mapOverlayLegendTooltip) },
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (showAntenna) {
                LegendItem(
                    color = ANTENNA_OVERLAY_COLOR,
                    label = strings.mapOverlayAntenna,
                    tag = MAP_ANTENNA_LEGEND_TAG,
                )
            }
            if (showSpatialFilters) {
                LegendItem(
                    color = spatialFilterOverlayLineColor(darkMode),
                    label = strings.mapOverlayFilters,
                    tag = MAP_FILTER_LEGEND_TAG,
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, tag: String) {
    Row(
        modifier = Modifier.testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .size(9.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color),
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

internal const val MAP_ANTENNA_LEGEND_TAG = "station-map-antenna-legend"
internal const val MAP_FILTER_LEGEND_TAG = "station-map-filter-legend"
