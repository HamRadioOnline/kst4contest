package kst4contest.view.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatPreferences

/**
 * The line the operator types into, and the shortcut buttons above it.
 *
 * This is the busiest strip of the window: everything that leaves this station passes
 * through it. Two details matter more here than anywhere else — the field keeps the
 * focus, and the caret stays at the end. A button that swallows the focus mid sentence
 * costs a QSO.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutButtonRow(
    shortcuts: List<String>,
    state: ChatInputState,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(Density.HAIRLINE, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        shortcuts.forEach { shortcut ->
            when (shortcut) {
                // The two frequency buttons take their text straight from the field beside
                // them rather than through the variable resolver, which is why they are
                // not ordinary shortcuts.
                MYQRG_SHORTCUT -> ShortcutButton(shortcut, accented = true) {
                    state.appendOwnQrg(mainCategory = true)
                }

                SECOND_QRG_SHORTCUT -> ShortcutButton(shortcut, accented = true) {
                    state.appendOwnQrg(mainCategory = false)
                }

                else -> ShortcutButton(
                    label = shortcut,
                    accented = shortcut == SET_NAME_MYQRG_SHORTCUT,
                ) {
                    state.appendShortcut(shortcut)
                }
            }
        }
    }
}

/** The two names the shortcut roster gives special meaning to. */
const val MYQRG_SHORTCUT = "MYQRG"
const val SECOND_QRG_SHORTCUT = "SECONDQRG"

/** Accented like the two frequency buttons, but an ordinary appending shortcut otherwise. */
const val SET_NAME_MYQRG_SHORTCUT = "/SETNAME MYQRG"

/**
 * A shortcut button.
 *
 * Three shortcuts wear `.buttonMyQrg1` in both stylesheets — the two frequency buttons and
 * "/SETNAME MYQRG" — the same green-to-lightgreen gradient the palette already reads as the
 * accent, so they are told apart by colour rather than by position. Everything else is an
 * ordinary button.
 */
@Composable
private fun ShortcutButton(label: String, accented: Boolean, onClick: () -> Unit) {
    if (!accented) {
        Form.button(text = label, onClick = onClick)
        return
    }

    Form.button(text = label, accented = true, onClick = onClick)
}

/**
 * The send row: the message field, TX, clear, and the three small fields the operator
 * keeps their own frequencies and antenna heading in.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatInputRow(
    state: ChatInputState,
    prefs: ChatPreferences,
    /**
     * The three fields' live values. Mirrors and not preference reads: the rotator and the
     * frequency follower move these while the operator is looking at them, and a plain read
     * would draw the value once and then never again.
     */
    ownQrgMain: ObservedValue<String>,
    ownQrgSecond: ObservedValue<String>,
    antennaQtf: ObservedValue<Double>,
    antennaQtf2: ObservedValue<Double>,
    so2rEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    // Every insertion bumps focusRequests; this is requestFocus() + selectEnd().
    LaunchedEffect(state.focusRequests) {
        if (state.focusRequests > 0) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessageField(
            state = state,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )

        Form.button(text = "TX", onClick = { state.send() })
        Form.button(text = kst4contest.view.i18n.LocalStrings.current.chatClear, onClick = { state.clear() })

        VerticalRule()

        // Commit on focus loss, as in JavaFX: typing a frequency should not write the
        // profile on every keystroke.
        InlineCommittedField(
            stored = ownQrgMain.value.orEmpty(),
            onCommit = { prefs.getMYQRGFirstCat().set(it) },
            tooltip = kst4contest.view.i18n.CurrentStrings.get().chatOwnQrgTooltip,
        )
        InlineCommittedField(
            stored = ownQrgSecond.value.orEmpty(),
            onCommit = { prefs.getMYQRGSecondCat().set(it) },
            tooltip = if (so2rEnabled) {
                kst4contest.view.i18n.CurrentStrings.get().chatSecondQrgTooltipSo2r
            } else {
                kst4contest.view.i18n.CurrentStrings.get().chatSecondQrgTooltip
            },
        )
        InlineCommittedField(
            stored = antennaQtf.value?.toString().orEmpty(),
            onCommit = { text -> text.toDoubleOrNull()?.let { prefs.getActualQTF().set(it) } },
            tooltip = kst4contest.view.i18n.CurrentStrings.get().chatQtfTooltip,
            width = QTF_FIELD_WIDTH,
        )
        if (so2rEnabled) {
            // Display only: rotator 2 writes it, the operator does not.
            InlineCommittedField(
                stored = antennaQtf2.value?.takeIf { it.isFinite() }?.toString().orEmpty(),
                onCommit = { },
                tooltip = kst4contest.view.i18n.CurrentStrings.get().chatQtf2Tooltip,
                editable = false,
                width = QTF_FIELD_WIDTH,
            )
        }
    }
}

/**
 * One of the three narrow fields beside the send controls.
 *
 * Written back when the focus leaves, not on every keystroke — the JavaFX fields hung
 * their write on focusedProperty for the same reason: a half-typed frequency is not a
 * frequency, and the profile is a file.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InlineCommittedField(
    stored: String,
    onCommit: (String) -> Unit,
    tooltip: String,
    editable: Boolean = true,
    width: Dp = QRG_FIELD_WIDTH,
) {
    var draft by remember(stored) { mutableStateOf(stored) }

    TooltipArea(tooltip = { TooltipChip(tooltip) }) {
        CompactTextField(
            value = draft,
            onValueChange = { draft = it },
            enabled = editable,
            modifier = Modifier
                .width(width)
                .onFocusChanged { focus ->
                    if (!focus.isFocused && draft != stored) onCommit(draft)
                },
        )
    }
}

@Composable
private fun TooltipChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = Density.BUTTON_SHAPE,
        tonalElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private val QRG_FIELD_WIDTH = 96.dp
private val QTF_FIELD_WIDTH = 56.dp

/**
 * The message field.
 *
 * Enter sends and is consumed, so the newline never reaches the text. Escape clears —
 * both were on the scene in JavaFX, which means they worked wherever the focus was; here
 * they are on the field, and the window handles them for the rest of the surface.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageField(
    state: ChatInputState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    TooltipArea(
        tooltip = {
            if (state.value.text.isNotBlank()) {
                TooltipChip(state.value.text)
            }
        },
        modifier = modifier,
    ) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Density.CONTROL_HEIGHT)
            .border(1.5.dp, scheme.primary, Density.BUTTON_SHAPE)
            .background(scheme.surface, Density.BUTTON_SHAPE)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = state.value,
            onValueChange = { state.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> { state.send(); true }
                        Key.Escape -> { state.clear(); true }
                        else -> false
                    }
                },
            singleLine = true,
            textStyle = compactControlTextStyle().copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
        )
    }
    }
}

/** The vertical separator that sets the frequency fields apart from the send controls. */
@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .width(Density.HAIRLINE)
            .height(Density.CONTROL_HEIGHT)
            .background(MaterialTheme.colorScheme.outline),
    )
}
