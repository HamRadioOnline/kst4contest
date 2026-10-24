package kst4contest.view.compose

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage

/**
 * The three tabs below the station list.
 *
 * Two of them are the monitor window's tables under their own layout ids — the same
 * columns, separate stored widths, which is what PROJECT_CONTEXT requires. Only the
 * public message tab is its own.
 */
@Composable
fun MessageTabs(
    publicMessages: DataTableState<ChatMessage>,
    clusterMessages: DataTableState<ClusterMessage>,
    qsoOfTheOther: DataTableState<ChatMessage>,
    onStationMessageClicked: (ChatMessage, StationSelectionSource) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(0) }

    val strings = LocalStrings.current
    val titles = listOf(
        strings.messagesPublic,
        strings.messagesDxCluster,
        strings.messagesQsoOfTheOther,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .border(Density.HAIRLINE, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            titles.forEachIndexed { index, title ->
                val isSelected = index == selected

                CompactTab(
                    text = title,
                    selected = isSelected,
                    onClick = { selected = index },
                )
            }
        }

        HorizontalDivider(thickness = Density.HAIRLINE)

        Box(modifier = Modifier.fillMaxSize()) {
            when (selected) {
                0 -> DataTableView(
                    publicMessages,
                    modifier = Modifier.fillMaxSize(),
                    onRowClick = { message ->
                        onStationMessageClicked(message, StationSelectionSource.PUBLIC_MESSAGE)
                    },
                )
                1 -> DataTableView(clusterMessages, modifier = Modifier.fillMaxSize())
                else -> DataTableView(
                    qsoOfTheOther,
                    modifier = Modifier.fillMaxSize(),
                    onRowClick = { message ->
                        onStationMessageClicked(message, StationSelectionSource.QSO_OF_THE_OTHER)
                    },
                )
            }
        }
    }
}
