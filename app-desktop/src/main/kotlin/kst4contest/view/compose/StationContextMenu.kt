package kst4contest.view.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.view.i18n.LocalStrings

internal const val STATION_CONTEXT_MENU_TAG = "station-context-menu"
internal const val STATION_CONTEXT_TARGET_CHOOSER_TAG = "station-context-target-chooser"
internal const val STATION_SKED_POPUP_TAG = "station-sked-popup"

private enum class StationContextPage {
    ROOT,
    SNIPPETS,
    NOT_QRV,
    WORKED,
    REACHABILITY,
}

/** Shared station action popup used by the user list and the map. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationContextOverlays(
    menuState: StationContextMenuState,
    mainWindowState: MainWindowState,
    onShowMap: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    var page by remember { mutableStateOf(StationContextPage.ROOT) }
    var skedDraft by remember { mutableStateOf<SkedEditorState?>(null) }
    var skedPosition by remember { mutableStateOf(IntOffset.Zero) }

    LaunchedEffect(menuState.candidates, menuState.target) {
        page = StationContextPage.ROOT
    }

    if (menuState.isOpen) {
        Popup(
            popupPositionProvider = WindowPointPopupPositionProvider(menuState.position),
            onDismissRequest = menuState::close,
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier = Modifier.testTag(STATION_CONTEXT_MENU_TAG),
                shape = MenuDefaults.shape,
                color = MenuDefaults.containerColor,
                tonalElevation = MenuDefaults.TonalElevation,
                shadowElevation = MenuDefaults.ShadowElevation,
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 190.dp, max = 360.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                    ) {
                        val target = menuState.target
                        if (target == null) {
                            ContextHeading(
                                strings.contextChooseStation,
                                Modifier.testTag(STATION_CONTEXT_TARGET_CHOOSER_TAG),
                            )
                            menuState.candidates.forEach { candidate ->
                                ContextItem(stationContextTargetLabel(candidate)) {
                                    menuState.chooseTarget(candidate)
                                }
                            }
                        } else {
                            when (page) {
                                StationContextPage.ROOT -> {
                                    ContextSubmenu(strings.contextMessageSnippets) {
                                        page = StationContextPage.SNIPPETS
                                    }
                                    ContextItem(strings.contextCreateSked) {
                                        skedPosition = menuState.position
                                        skedDraft = mainWindowState.selectedStation.createSkedDraft(target)
                                        menuState.close()
                                    }
                                    ContextSeparator()
                                    ContextSubmenu(strings.contextTagNotQrv) {
                                        page = StationContextPage.NOT_QRV
                                    }
                                    ContextSubmenu(strings.contextMarkWorked) {
                                        page = StationContextPage.WORKED
                                    }
                                    ContextSeparator()
                                    ContextSubmenu(strings.contextCalculateReachability) {
                                        page = StationContextPage.REACHABILITY
                                    }
                                }

                                StationContextPage.SNIPPETS -> {
                                    ContextBack { page = StationContextPage.ROOT }
                                    mainWindowState.snippets.entries.forEach { snippet ->
                                        ContextItem(snippet) {
                                            mainWindowState.chatInput.appendSnippet(snippet)
                                            menuState.close()
                                        }
                                    }
                                }

                                StationContextPage.NOT_QRV -> {
                                    ContextBack { page = StationContextPage.ROOT }
                                    NotQrvBand.entries
                                        .filter { it.band in mainWindowState.activeBands }
                                        .forEach { band ->
                                            val tagged = mainWindowState.selectedStation
                                                .isTaggedNotQrv(target, band)
                                            ContextCheckItem(band.label, tagged) {
                                                mainWindowState.selectedStation
                                                    .tagNotQrv(target, band, !tagged)
                                            }
                                        }
                                }

                                StationContextPage.WORKED -> {
                                    ContextBack { page = StationContextPage.ROOT }
                                    mainWindowState.activeBands.sortedBy(Band::ordinal).forEach { band ->
                                        val worked = target.isWorkedOn(band)
                                        ContextCheckItem(band.displayLabel, worked, enabled = !worked) {
                                            if (mainWindowState.markStationWorked?.invoke(target, band) == true) {
                                                menuState.close()
                                            }
                                        }
                                    }
                                }

                                StationContextPage.REACHABILITY -> {
                                    ContextBack { page = StationContextPage.ROOT }
                                    mainWindowState.activeBands.sortedBy(Band::ordinal).forEach { band ->
                                        ContextItem(band.displayLabel) {
                                            if (mainWindowState.mapReachability.calculate(target, band)) {
                                                onShowMap?.invoke()
                                                menuState.close()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    skedDraft?.let { draft ->
        Popup(
            popupPositionProvider = WindowPointPopupPositionProvider(skedPosition),
            onDismissRequest = { skedDraft = null },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 420.dp, max = 560.dp)
                    .testTag(STATION_SKED_POPUP_TAG),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        strings.contextSkedTitle(draft.target.callSign.orEmpty()),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    SkedEditorControls(
                        activeBands = mainWindowState.activeBands,
                        minutes = draft.minutes,
                        onMinutesChanged = { draft.minutes = it },
                        band = draft.band,
                        onBandChanged = { draft.band = it },
                        mode = draft.mode,
                        onModeChanged = { draft.mode = it },
                        remindPm = draft.remindPm,
                        onRemindPmChanged = { draft.remindPm = it },
                        reminderOffsets = draft.reminderOffsets,
                        onReminderOffsetsChanged = { draft.reminderOffsets = it },
                        canCreate = draft.band != null,
                        onCreate = {
                            if (draft.submit()) skedDraft = null
                        },
                        additionalActions = {
                            Form.button(strings.contextCancel) { skedDraft = null }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ContextBack(onClick: () -> Unit) {
    val strings = LocalStrings.current
    ContextItem("‹ ${strings.contextBack}", onClick = onClick)
    ContextSeparator()
}

@Composable
private fun ContextSubmenu(text: String, onClick: () -> Unit) {
    ContextItem("$text  ›", onClick = onClick)
}

@Composable
private fun ContextCheckItem(
    text: String,
    checked: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ContextItem((if (checked) "✓ " else "  ") + text, enabled, onClick)
}

@Composable
private fun ContextItem(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = compactControlTextStyle(), maxLines = 1) },
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier.heightIn(min = Density.CONTROL_HEIGHT),
    )
}

@Composable
private fun ContextSeparator() {
    HorizontalDivider(
        thickness = Density.HAIRLINE,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

internal fun stationContextTargetLabel(member: ChatMember): String {
    val category = member.chatCategory
    val categoryName = category?.getChatCategoryName(category.categoryNumber)
        ?.takeIf { it.isNotBlank() }
        ?: category?.categoryNumber?.toString().orEmpty()
    return if (categoryName.isBlank()) member.callSign.orEmpty()
    else "${member.callSign.orEmpty()} | $categoryName"
}

internal fun ChatMember.isWorkedOn(band: Band): Boolean = when (band) {
    Band.B_50 -> isWorked50
    Band.B_70 -> isWorked70
    Band.B_144 -> isWorked144
    Band.B_432 -> isWorked432
    Band.B_1296 -> isWorked1240
    Band.B_2320 -> isWorked2300
    Band.B_3400 -> isWorked3400
    Band.B_5760 -> isWorked5600
    Band.B_10G -> isWorked10G
    Band.B_24G -> isWorked24G
}

/** Window-relative popup position clamped to the visible window on every DPI scale. */
internal data class WindowPointPopupPositionProvider(
    val point: IntOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = point.x.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)),
        y = point.y.coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height)),
    )
}
