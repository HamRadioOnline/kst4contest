package kst4contest.view.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role

/** One font size and natural line box for every compact interactive control. */
@Composable
internal fun compactControlTextStyle(): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(lineHeight = TextUnit.Unspecified)

/**
 * Form controls shared by the settings tabs.
 *
 * Numeric fields keep the operator's text while it is being typed and only hand a
 * parsed value upwards when it parses. Rejecting keystrokes instead would make a
 * field impossible to clear, and replacing an unparsable entry with a default would
 * silently change a setting — PROJECT_CONTEXT is explicit that an absent or malformed
 * value must not become a fabricated zero.
 *
 * Every control here is built to desktop density; see [Density] for why and for the
 * measurements.
 */
object Form {

    @Composable
    fun text(label: String, value: String?, enabled: Boolean = true, onChange: (String) -> Unit) {
        val draft = rememberDraft(value ?: "")

        labelled(label) {
            CompactTextField(
                value = draft.value,
                onValueChange = { typed ->
                    draft.value = typed
                    onChange(typed)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    fun password(label: String, value: String?, enabled: Boolean = true, onChange: (String) -> Unit) {
        val draft = rememberDraft(value ?: "")

        labelled(label) {
            CompactTextField(
                value = draft.value,
                onValueChange = { typed ->
                    draft.value = typed
                    onChange(typed)
                },
                enabled = enabled,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    /**
     * A text field that is validated when it is committed, not while it is typed.
     *
     * This is the Compose counterpart of the JavaFX fields which hung their rule on
     * `focusedProperty` — the DX cluster port, the spotter callsign, the beacon
     * templates and the beacon interval. Committing on every keystroke would refuse
     * every prefix of a valid entry, so the draft lives here until the operator leaves
     * the field or presses Enter.
     *
     * Only an edited draft is committed: drawing the field, or entering and leaving it
     * without typing, commits nothing, and neither does Enter on an unchanged value.
     * A field removed while it holds the focus -- its tab switched away, its window
     * closed -- commits a typed entry once: Compose reports the cleared focus to the
     * field as a focus loss.
     *
     * @param stored the value stored when the caller composed; the draft starts from it
     * @param commit returns null when the value was accepted, otherwise the reason
     * @param onRefused shown to the operator; the JavaFX fields opened an alert
     * @param isUnchanged whether the draft names the stored value. Textual by default,
     *        looser where two spellings mean one value, as with colours. It must agree
     *        with what [commit] stores: a draft that commit would store as `c` has to count
     *        as unchanged against `c`, or leaving the field after Enter commits it again.
     * @param current the value in force right now, read when the field commits: inside the
     *        settings window the value staged in the draft, otherwise the stored one. The
     *        typed text is judged against it, and falls back to it on a refusal (what the
     *        JavaFX fields did with setText). Required, because [stored] alone goes stale
     *        when it comes from a plain getter: nothing need recompose the caller after a
     *        commit, so after Enter it can still name the old value.
     */
    @Composable
    fun committed(
        label: String,
        stored: String,
        commit: (String) -> String?,
        onRefused: (String) -> Unit,
        isUnchanged: (draft: String, stored: String) -> Boolean = { typed, now -> typed == now },
        current: () -> String,
    ) {
        var draft by remember(stored) { mutableStateOf(stored) }

        /*
         * Whether the field has held the focus since it was last committed. onFocusChanged
         * also fires when the modifier is first attached, with "not focused"; reading that
         * as a focus loss committed every such field the moment its tab was opened. Only a
         * change from focused to unfocused is the operator leaving the field. A plain holder,
         * not state: nothing is drawn from it.
         */
        val hadFocus = remember { booleanArrayOf(false) }

        fun commitDraft() {
            /*
             * An untouched draft is no edit. Committing it anyway would turn a shipped colour
             * into an override of itself, restart the beacon timer, or report a stored value
             * the rule refuses although the operator only passed through the field.
             */
            if (isUnchanged(draft, current())) return

            val refusal = commit(draft)
            if (refusal != null) {
                onRefused(refusal)
                draft = current()
            }
        }

        /*
         * Inside the settings window the draft can commit this field without a focus change:
         * a click on "Apply/Close prefs" may leave the focus in the field, and the entry would
         * otherwise reach the draft only when the closing window removed the field. Through
         * an updated state, because commitDraft closes over this composition's parameters.
         * A refusal is reported by commitDraft itself through onRefused, as on focus loss, so
         * the registration answers null and the caller sees the refusal as the notice.
         */
        val settingsDraft = LocalSettingsDraft.current
        val latestCommit = rememberUpdatedState(::commitDraft)
        DisposableEffect(settingsDraft) {
            val entry = settingsDraft?.registerTypedEntry {
                latestCommit.value()
                null
            }
            onDispose { entry?.close() }
        }

        labelled(label) {
            CompactTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            hadFocus[0] = true
                        } else if (hadFocus[0]) {
                            hadFocus[0] = false
                            commitDraft()
                        }
                    }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                            commitDraft()
                            true
                        } else {
                            false
                        }
                    },
            )
        }
    }

    /**
     * Picks one item from a fixed list, the counterpart of the JavaFX ChoiceBox.
     *
     * A null selection is possible and shown as such: the second chat category has no
     * value until the operator picks one, and inventing one would log the station into
     * a chat it never chose.
     */
    @Composable
    fun <T> choice(
        label: String,
        items: List<T>,
        selected: T?,
        describe: (T) -> String,
        enabled: Boolean = true,
        onSelect: (T) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        val draft = rememberDraft(selected)

        labelled(label) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    shape = Density.BUTTON_SHAPE,
                    contentPadding = Density.FIELD_CONTENT_PADDING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Density.CONTROL_HEIGHT),
                ) {
                    Text(
                        draft.value?.let(describe) ?: kst4contest.view.i18n.LocalStrings.current.formNotSelected,
                        style = compactControlTextStyle(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(describe(item), style = compactControlTextStyle()) },
                            contentPadding = Density.FIELD_CONTENT_PADDING,
                            modifier = Modifier.heightIn(min = Density.CONTROL_HEIGHT),
                            onClick = {
                                expanded = false
                                draft.value = item
                                onSelect(item)
                            },
                        )
                    }
                }
            }
            }
        }
    }

    @Composable
    fun <T> inlineChoice(
        label: String,
        items: List<T>,
        selected: T?,
        describe: (T) -> String,
        enabled: Boolean = true,
        trailingText: String = "",
        onSelect: (T) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        val draft = rememberDraft(selected)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = Density.CONTROL_HEIGHT),
        ) {
            if (label.isNotBlank()) {
                Text(label, style = compactControlTextStyle(), modifier = Modifier.padding(end = 4.dp))
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    shape = Density.BUTTON_SHAPE,
                    contentPadding = Density.FIELD_CONTENT_PADDING,
                    modifier = Modifier.heightIn(min = Density.CONTROL_HEIGHT),
                ) {
                    Text(
                        draft.value?.let(describe) ?: kst4contest.view.i18n.LocalStrings.current.formNotSelected,
                        style = compactControlTextStyle(),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(describe(item), style = compactControlTextStyle()) },
                            contentPadding = Density.FIELD_CONTENT_PADDING,
                            modifier = Modifier.heightIn(min = Density.CONTROL_HEIGHT),
                            onClick = {
                                expanded = false
                                draft.value = item
                                onSelect(item)
                            },
                        )
                    }
                }
            }
            }
            if (trailingText.isNotBlank()) {
                Text(
                    trailingText,
                    style = compactControlTextStyle(),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }

    @Composable
    fun check(
        label: String,
        value: Boolean,
        enabled: Boolean = true,
        onChange: (Boolean) -> Unit,
    ) {
        val draft = rememberDraft(value)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = Density.CONTROL_HEIGHT),
        ) {
            CompactCheckbox(draft.value, enabled = enabled) { ticked ->
                draft.value = ticked
                onChange(ticked)
            }
            Text(
                label,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                style = compactControlTextStyle(),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    /** A compact two-state button for dense desktop option groups. */
    @Composable
    fun toggle(
        text: String,
        checked: Boolean,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        CompactToggleButton(
            text = text,
            checked = checked,
            enabled = enabled,
            modifier = modifier,
            onCheckedChange = onCheckedChange,
        )
    }

    @Composable
    fun int(label: String, value: Int, onChange: (Int) -> Unit) =
        number(label, value, { it.trim().toIntOrNull() }, onChange)

    @Composable
    fun decimal(
        label: String,
        value: Double,
        isValid: (Double) -> Boolean = { true },
        onChange: (Double) -> Unit,
    ) = number(
        label,
        value,
        { text -> text.trim().replace(',', '.').toDoubleOrNull()?.takeIf(isValid) },
        onChange,
    )

    /**
     * The number field behind [int] and [decimal].
     *
     * The text follows the stored value only when that value is not what the operator's own
     * text already says. A settings draft is Compose state, so the value an entry stages comes
     * straight back as [value]; resetting the text to `value.toString()` then would turn a
     * typed "1" into "1.0" before the next digit, and "135,5" could never be typed.
     */
    @Composable
    private fun <N> number(label: String, value: N, parse: (String) -> N?, onChange: (N) -> Unit) {
        // The last text typed. A plain holder: it only seeds the text below, nothing draws it.
        val typed = remember { arrayOf(value.toString()) }
        var draft by remember(value) {
            mutableStateOf(if (parse(typed[0]) == value) typed[0] else value.toString())
        }

        /*
         * Text that does not parse stages nothing, so the draft still holds the last valid
         * value while the field shows something else in red. Applying then would put a value
         * into effect the operator no longer sees; the field refuses instead, naming itself.
         */
        val strings = kst4contest.view.i18n.LocalStrings.current
        val settingsDraft = LocalSettingsDraft.current
        val refusal = rememberUpdatedState {
            if (parse(draft) == null) strings.settingsInvalidEntry(label, draft) else null
        }
        DisposableEffect(settingsDraft) {
            val entry = settingsDraft?.registerTypedEntry { refusal.value() }
            onDispose { entry?.close() }
        }

        labelled(label) {
            CompactTextField(
                value = draft,
                onValueChange = { entered ->
                    typed[0] = entered
                    draft = entered
                    parse(entered)?.let(onChange)
                },
                isError = parse(draft) == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    /**
     * A titled section.
     *
     * The JavaFX window drew `generateLabeledSeparator`: a rule, the label, another
     * rule. Here the label leads and one rule runs to the right edge, which sits
     * better above the left-aligned label column than a centred title does. Either
     * way it is a visible division, which a bare line of text was not.
     */
    @Composable
    fun section(title: String, content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                HorizontalDivider(modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
            content()
        }
    }

    /** A compact settings group that needs stronger separation than a labelled divider. */
    @Composable
    fun panelSection(title: String, content: @Composable () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = Density.BUTTON_SHAPE,
                )
                .border(
                    width = Density.HAIRLINE,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = Density.BUTTON_SHAPE,
                )
                .padding(Density.GROUP_CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }

    /**
     * A section whose controls flow across the width instead of stacking.
     *
     * For short, like-for-like options — the nine band checkboxes, the startup filters.
     * Stacked they cost nine rows of height for nine words; flowing they fill the width
     * the window actually has and re-flow when it changes.
     */
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun flowingSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
        section(title) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }

    /**
     * A labelled row holding a control this object does not provide, so a tab can add
     * one without losing the shared label column.
     */
    @Composable
    fun labelledRow(label: String, field: @Composable () -> Unit) {
        labelled(label, field)
    }

    /**
     * The shared desktop button used in settings, filter and shortcut areas.
     *
     * Material's own Button is a 40dp pill; this one is square-ish and at desktop
     * height. It also carries the two states the JavaFX stylesheets give every button:
     * a gradient while the pointer is over it and a red border while it is held down.
     */
    @Composable
    fun button(
        text: String,
        enabled: Boolean = true,
        accented: Boolean = false,
        selected: Boolean = false,
        modifier: Modifier = Modifier,
        horizontalPadding: Dp = 6.dp,
        onClick: () -> Unit,
    ) {
        val palette = LocalJavaFxPalette.current
        val interactions = remember { MutableInteractionSource() }
        val hovered by interactions.collectIsHoveredAsState()
        val pressed by interactions.collectIsPressedAsState()
        val hoverGradient = palette.buttonHoverGradient
        val accentGradient = if (palette.windowBackground.luminance() < 0.5f) {
            listOf(Color(0xFF008000), Color(0xFF90EE90))
        } else {
            listOf(Color(0xFF00FFFF), Color(0xFFFF99FF))
        }
        val backgroundGradient = when {
            !enabled -> emptyList()
            hovered && hoverGradient.size >= 2 -> hoverGradient
            accented -> accentGradient
            else -> emptyList()
        }
        val containerColor = when {
            backgroundGradient.isNotEmpty() -> Color.Transparent
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val contentColor = when {
            hovered && enabled && hoverGradient.isNotEmpty() -> Color.Black
            accented -> Color(0xFF395306)
            selected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 0.dp
        ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = Density.BUTTON_SHAPE,
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 0.dp),
            interactionSource = interactions,
            border = when {
                pressed -> BorderStroke(Density.SELECTION_BORDER, palette.buttonPressedBorder)
                enabled -> BorderStroke(Density.HAIRLINE, MaterialTheme.colorScheme.outline)
                else -> BorderStroke(
                    Density.HAIRLINE,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            ),
            modifier = modifier
                .heightIn(min = Density.CONTROL_HEIGHT)
                .then(
                    if (backgroundGradient.size >= 2) {
                        Modifier.background(
                            brush = Brush.verticalGradient(backgroundGradient),
                            shape = Density.BUTTON_SHAPE,
                        )
                    } else {
                        Modifier
                    }
                ),
        ) {
            /*
             * One line, always. A label left to wrap turns a squeezed button into a tall
             * bar instead of simply being too narrow, and that bar then pushes the rest
             * of the window out of view.
             */
            Text(
                text,
                style = compactControlTextStyle(),
                maxLines = 1,
            )
        }
        }
    }

    /**
     * Holds what the operator sees while they change it.
     *
     * The settings controls are bound to plain ChatPreferences values, and those are not
     * Compose state: writing one triggers no recomposition, so a field bound straight to
     * the stored value keeps painting the value from the last composition. Typing then
     * writes through correctly and shows nothing, a checkbox never appears to tick, and a
     * picker never appears to change — which is exactly how it behaved.
     *
     * Keyed on the stored value, so an external change still reaches the control, and so
     * a setter that normalises its input (a trimmed space, an upper-cased callsign) puts
     * the normalised value back. This is the pattern GuardedTextField already used for
     * the external-interface fields.
     */
    @Composable
    private fun <T> rememberDraft(stored: T): MutableState<T> =
        remember(stored) { mutableStateOf(stored) }

    @Composable
    private fun labelled(label: String, field: @Composable () -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = Density.ROW_MIN_HEIGHT),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(Density.LABEL_COLUMN_WIDTH),
            )
            field()
        }
    }
}

/**
 * A content-sized toggle with the same typography, height and palette as the shared
 * desktop buttons. Checkbox semantics keep keyboard and accessibility behaviour while
 * avoiding the extra symbol and label width of a checkbox row.
 */
@Composable
internal fun CompactToggleButton(
    text: String,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        pressed -> MaterialTheme.colorScheme.primaryContainer
        hovered && checked -> MaterialTheme.colorScheme.primaryContainer
        checked -> MaterialTheme.colorScheme.primary
        hovered || focused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        (pressed || hovered) && checked -> MaterialTheme.colorScheme.onPrimaryContainer
        checked -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val outlineColor = when {
        focused || checked -> MaterialTheme.colorScheme.primary
        enabled -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    }

    Box(
        modifier = modifier
            .heightIn(min = Density.CONTROL_HEIGHT)
            .background(containerColor, Density.BUTTON_SHAPE)
            .border(
                if (focused) Density.SELECTION_BORDER else Density.HAIRLINE,
                outlineColor,
                Density.BUTTON_SHAPE,
            )
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactions,
                indication = null,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = compactControlTextStyle(),
            color = contentColor,
            maxLines = 1,
        )
    }
}

/**
 * A compact desktop tab with the same interaction palette as the shared controls.
 * The selected surface visually continues into the content below it, while the border
 * keeps inactive entries recognizable as tabs in both palettes.
 */
@Composable
internal fun CompactTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val primary = MaterialTheme.colorScheme.primary
    val background = when {
        pressed -> MaterialTheme.colorScheme.primaryContainer
        selected -> MaterialTheme.colorScheme.surface
        hovered || focused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val outline = when {
        selected || focused -> primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .heightIn(min = Density.TAB_MIN_HEIGHT)
            .background(background)
            .border(
                if (focused) Density.SELECTION_BORDER else Density.HAIRLINE,
                outline,
            )
            .selectable(
                selected = selected,
                interactionSource = interactions,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .drawBehind {
                if (selected) {
                    val thickness = 2.dp.toPx()
                    drawRect(
                        color = primary,
                        topLeft = Offset(0f, size.height - thickness),
                        size = Size(size.width, thickness),
                    )
                }
            }
            .padding(horizontal = Density.TAB_HORIZONTAL_PADDING, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = compactControlTextStyle(),
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * A text field at desktop density.
 *
 * Built from BasicTextField and the outlined decoration box rather than from
 * OutlinedTextField, because that composable enforces a 56dp minimum height which
 * cannot be passed a smaller one. The decoration box takes the content padding, and
 * that is what buys the height back.
 *
 * The decoration box and its container are marked experimental in Material 3. The
 * opt-in is deliberate and there is no alternative: it is the only supported way to
 * build a text field below the enforced minimum height. Should the signature change,
 * the two calls below are the whole surface to fix, and the tests around the tab states
 * stay unaffected because none of them render.
 *
 * @param borderColor overrides the outline, so a caller can mark a selected row
 * @param borderThickness overrides the outline width for the same reason
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    borderColor: Color? = null,
    borderThickness: Dp? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    /*
     * The container is the JavaFX -fx-control-inner-background, which both sheets set
     * apart from the window background. Material leaves an outlined field transparent,
     * which made every field vanish into the surface.
     */
    val colors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = borderColor ?: MaterialTheme.colorScheme.outline,
        focusedBorderColor = borderColor ?: MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    /*
     * The colour has to be carried in the text style: BasicTextField has no theme of
     * its own, so without this the text would stay black in the evening design.
     */
    val onField = MaterialTheme.colorScheme.surfaceVariant.let { field ->
        val brightness = 0.3f * field.red + 0.59f * field.green + 0.11f * field.blue
        if (brightness > 0.5f) Color.Black else Color.White
    }
    val textColor = if (enabled) onField else onField.copy(alpha = 0.38f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = compactControlTextStyle().copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = Density.CONTROL_HEIGHT),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = true,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            isError = isError,
            colors = colors,
            contentPadding = Density.FIELD_CONTENT_PADDING,
            placeholder = if (placeholder.isNotEmpty()) {
                {
                    Text(
                        placeholder,
                        style = compactControlTextStyle().copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                    )
                }
            } else {
                null
            },
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = Density.BUTTON_SHAPE,
                    focusedBorderThickness = borderThickness ?: 2.dp,
                    unfocusedBorderThickness = borderThickness ?: 1.dp,
                )
            },
        )
    }
}

/**
 * A checkbox on the shared compact control surface instead of Material's 48dp touch target.
 */
@Composable
internal fun CompactCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = modifier.size(Density.CONTROL_HEIGHT),
        )
    }
}
