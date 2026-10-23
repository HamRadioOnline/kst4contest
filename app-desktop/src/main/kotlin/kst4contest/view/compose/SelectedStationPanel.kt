package kst4contest.view.compose

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import kst4contest.model.Band
import java.util.function.Consumer

internal const val SELECTED_STATION_PANEL_TAG = "selected-station-panel"
internal const val SELECTED_NOT_QRV_GROUP_TAG = "selected-not-qrv-group"
internal const val SELECTED_SKED_GROUP_TAG = "selected-sked-group"

internal fun skedBandOptions(activeBands: Set<Band>): List<Band> {
    val options = ArrayList<Band>(activeBands.size)
    Band.entries.forEach { band ->
        if (activeBands.contains(band)) options.add(band)
    }
    return options
}

/**
 * The panel below the station list, for whichever station is selected.
 *
 * Its rules live in [SelectedStationState] and are tested there; what is here is the
 * drawing. With nothing selected it says so rather than showing dead controls — the
 * JavaFX pane disabled them, which looks the same as broken.
 *
 * @param openInBrowser opens a lookup address; reaching the browser is the host
 *        application's business
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedStationPanel(
    state: SelectedStationState,
    messages: DataTableState<kst4contest.model.ChatMessage>,
    /** My own station's enabled bands; used for NOT-QRV and sked controls. */
    activeBands: Set<Band>,
    openInBrowser: Consumer<String>,
    onShowOnMap: () -> Unit,
    onShowPathInAirScout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val member = state.selected

    val scroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SELECTED_STATION_PANEL_TAG)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .border(Density.HAIRLINE, MaterialTheme.colorScheme.outlineVariant),
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 3.dp, end = Density.SCROLLBAR_GUTTER, bottom = 4.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (member == null) {
            Text(
                strings.selectedNone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            return@Column
        }

        val category = member.chatCategory?.let { it.getChatCategoryName(it.categoryNumber) }
        val detectedBands = state.detectedBands()
        val metadata = buildList {
            category?.let(::add)
            member.getQTFdirection()?.let { add("QTF: $it°") }
            member.qrb?.let { add("QRB: $it km") }
            val lastActSec = member.activityTimeLastInEpoch
            if (lastActSec > 0L) {
                val actStr = MessageFormats.clockTime(lastActSec.toString())
                val minAgo = kst4contest.controller.Utils4KST
                    .time_getSecondsBetweenEpochAndNow(lastActSec.toString()) / 60
                add(strings.selectedLastActivity(actStr, minAgo))
            }
            add(detectedBands.ifBlank { strings.tooltipBandsNone })
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp),
        ) {
            Text(
                member.callSign.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            metadata.forEach { value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "|",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = Density.BUTTON_GAP),
                    )
                    Text(value, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
            verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = Density.CONTROL_HEIGHT),
            ) {
                Text(
                    "${strings.selectedShowMessages}:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = Density.BUTTON_GAP),
                )
                RadioChoice(
                    label = SelectedMessageFilter.PM_TO_ME.label,
                    selected = state.messageFilter == SelectedMessageFilter.PM_TO_ME,
                    onSelect = { state.messageFilter = SelectedMessageFilter.PM_TO_ME },
                )
            }
            SelectedMessageFilter.entries.drop(1).forEach { filter ->
                RadioChoice(
                    label = filter.label,
                    selected = state.messageFilter == filter,
                    onSelect = { state.messageFilter = filter },
                )
            }
        }

        DataTableView(
            state = messages,
            modifier = Modifier
                .fillMaxWidth()
                .height(Density.SELECTED_MESSAGE_TABLE_HEIGHT),
        )

        SelectedStationActionGroups(
            state = state,
            activeBands = activeBands,
        )

        Form.section(strings.selectedLookUpAndShow) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                Form.button(strings.selectedShowPathInAs, onClick = onShowPathInAirScout)
                Form.button(strings.selectedShowOnMap, onClick = onShowOnMap)
                Form.button(
                    strings.selectedTurnAntenna(state.selected?.callSignRaw.orEmpty()),
                    enabled = state.canTurnAntenna,
                ) { state.turnAntenna() }
                if (state.showsSecondAntenna) {
                    Form.button(
                        strings.selectedTurnAntenna2(state.selected?.callSignRaw.orEmpty()),
                        enabled = state.canTurnSecondAntenna,
                    ) { state.turnSecondAntenna() }
                }
                Form.button(strings.selectedLookupQrz, enabled = state.canLookUp) {
                    openInBrowser.accept(state.qrzComUrl())
                }
                Form.button(strings.selectedLookupQrzcq, enabled = state.canLookUp) {
                    openInBrowser.accept(state.qrzCqUrl())
                }
            }
        }
    }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scroll),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = LocalScrollbarStyle.current.copy(
                unhoverColor = MaterialTheme.colorScheme.outline,
                hoverColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedStationActionGroups(
    state: SelectedStationState,
    activeBands: Set<Band>,
) {
    val strings = LocalStrings.current
    val member = state.selected ?: return
    val activeNotQrvBands = NotQrvBand.entries.filter { activeBands.contains(it.band) }
    val notQrvContent: @Composable () -> Unit = {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
            verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
        ) {
            activeNotQrvBands.forEach { band ->
                Form.toggle(band.label, state.isTaggedNotQrv(band)) {
                    state.tagNotQrv(band, it)
                }
            }
            Form.toggle(
                strings.selectedNotQrvAll,
                activeNotQrvBands.all { state.isTaggedNotQrv(it) },
            ) {
                state.tagNotQrvAll(it)
            }
        }
    }
    val skedContent: @Composable () -> Unit = {
        SkedAlertControls(state, activeBands)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= Density.SELECTED_ACTION_GROUPS_ROW_MIN_WIDTH) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
                verticalAlignment = Alignment.Top,
            ) {
                CompactFunctionGroup(
                    title = strings.selectedTagNotQrv(member.callSign.orEmpty()),
                    modifier = Modifier.weight(0.35f).testTag(SELECTED_NOT_QRV_GROUP_TAG),
                    content = notQrvContent,
                )
                CompactFunctionGroup(
                    title = strings.selectedSked,
                    modifier = Modifier.weight(0.65f).testTag(SELECTED_SKED_GROUP_TAG),
                    content = skedContent,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
            ) {
                CompactFunctionGroup(
                    title = strings.selectedTagNotQrv(member.callSign.orEmpty()),
                    modifier = Modifier.fillMaxWidth().testTag(SELECTED_NOT_QRV_GROUP_TAG),
                    content = notQrvContent,
                )
                CompactFunctionGroup(
                    title = strings.selectedSked,
                    modifier = Modifier.fillMaxWidth().testTag(SELECTED_SKED_GROUP_TAG),
                    content = skedContent,
                )
            }
        }
    }
}

@Composable
private fun CompactFunctionGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .border(Density.HAIRLINE, MaterialTheme.colorScheme.outlineVariant, Density.BUTTON_SHAPE)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkedAlertControls(state: SelectedStationState, activeBands: Set<Band>) {
    val strings = LocalStrings.current

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
        verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
    ) {
        Form.inlineChoice(
            label = strings.selectedSkedIn,
            items = SelectedStationState.SKED_MINUTES,
            selected = state.skedMinutes,
            describe = { it.toString() },
            trailingText = strings.selectedSkedMinutesAt,
            onSelect = { state.skedMinutes = it },
        )
        Form.inlineChoice(
            label = "",
            items = skedBandOptions(activeBands),
            selected = state.skedBand,
            describe = { it.prefix + " MHz" },
            trailingText = strings.selectedSkedModeLink,
            onSelect = { state.skedBand = it },
        )
        Form.inlineChoice(
            label = "",
            items = SelectedStationState.SKED_MODES,
            selected = state.skedMode,
            describe = { it },
            trailingText = ".",
            onSelect = { state.skedMode = it },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(Density.CONTROL_HEIGHT),
        ) {
            CompactCheckbox(state.remindPm) { state.remindPm = it }
            Text(
                strings.selectedRemindPmIn,
                style = compactControlTextStyle(),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            )
            Form.inlineChoice(
                label = "",
                items = SelectedStationState.REMINDER_OFFSETS,
                selected = state.reminderOffsets,
                describe = { it },
                enabled = state.remindPm,
                trailingText = strings.selectedMinutes,
                onSelect = { state.reminderOffsets = it },
            )
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
        verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
    ) {
        Form.button(strings.selectedCreateSked, enabled = state.canCreateSked) { state.createSked() }
        Form.button(strings.selectedSkedFail, enabled = state.canCreateSked) { state.markSkedFail() }
        Form.button(strings.selectedResetFail, enabled = state.canCreateSked) { state.resetSkedFail() }
    }
}

/**
 * The two priority buttons beside the station list: a shortcut to the station the
 * score says to work next.
 */
@Composable
fun TopPriorityBar(
    state: TopPriorityState,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(Density.HAIRLINE, MaterialTheme.colorScheme.outlineVariant)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(strings.selectedPriority, style = MaterialTheme.typography.bodySmall)

        state.entries.forEach { entry ->
            Form.button(entry.label, onClick = entry.select)
        }

        Form.button(strings.selectedMore, onClick = onShowMore)
    }
}


/**
 * One of a set of mutually exclusive choices.
 *
 * Material's RadioButton carries the same 48dp minimum that made every field too tall on
 * the desktop, so the touch target is released the way the other compact controls do it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioChoice(label: String, selected: Boolean, onSelect: () -> Unit) {
    val strings = LocalStrings.current
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = Modifier
                .selectable(selected = selected, onClick = onSelect)
                .padding(end = Density.BUTTON_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                modifier = Modifier.size(Density.CHECK_SIZE),
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
