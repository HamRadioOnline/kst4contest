package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import kst4contest.view.compose.Kst4ContestTheme

/*
 * Shared plumbing for the settings-tab screen tests (#107).
 *
 * The form controls draw a label beside a field, not a labelled field: there is no semantic
 * link between the two. These helpers find a control by the label the operator reads, on the
 * same row, which is exactly how the operator finds it -- and which breaks, as it should,
 * when a label moves away from its field.
 */

/**
 * Draws one tab the way the settings window does: themed, in a column that scrolls, so a
 * control further down can be scrolled to and clicked.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showTab(
    darkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    setContent {
        Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = 12f) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                content()
            }
        }
    }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.labelNode(label: String, occurrence: Int): SemanticsNode {
    val labels = onAllNodes(hasText(label, substring = false)).fetchSemanticsNodes()
    check(labels.size > occurrence) { "no label '$label' (#$occurrence) on screen; found ${labels.size}" }
    return labels.sortedBy { it.boundsInRoot.top }[occurrence]
}

/** The control matching [matcher] on the row of [label], picked by [choose] among candidates. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.besideLabel(
    label: String,
    occurrence: Int,
    matcher: SemanticsMatcher,
    choose: (label: SemanticsNode, candidates: List<SemanticsNode>) -> SemanticsNode?,
): SemanticsNodeInteraction {
    val anchor = labelNode(label, occurrence)
    val all = onAllNodes(matcher).fetchSemanticsNodes()
    val sameRow = all.filter { node ->
        node.boundsInRoot.center.y in anchor.boundsInRoot.top..anchor.boundsInRoot.bottom
    }
    val chosen = choose(anchor, sameRow) ?: error("no control beside '$label' (#$occurrence)")
    return onAllNodes(matcher)[all.indexOf(chosen)]
}

/**
 * The text field to the right of [label]. Matched by its editable text rather than by its
 * set-text action, which a disabled field does not offer, so a locked field is found too.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.fieldBeside(label: String, occurrence: Int = 0): SemanticsNodeInteraction =
    besideLabel(label, occurrence, SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)) { anchor, candidates ->
        candidates.filter { it.boundsInRoot.left >= anchor.boundsInRoot.right - 1f }
            .minByOrNull { it.boundsInRoot.left }
    }

/** The checkbox directly to the left of [label], as [kst4contest.view.compose.Form.check] draws it. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkBeside(label: String, occurrence: Int = 0): SemanticsNodeInteraction =
    besideLabel(label, occurrence, isToggleable()) { anchor, candidates ->
        candidates.filter { it.boundsInRoot.right <= anchor.boundsInRoot.left + 1f }
            .maxByOrNull { it.boundsInRoot.right }
    }

/** Ticks or unticks the checkbox beside [label], scrolling to it first. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.toggle(label: String, occurrence: Int = 0) {
    checkBeside(label, occurrence).performScrollTo().performClick()
    waitForIdle()
}

/** Replaces the text of the field beside [label], as typing a whole new value would. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.type(label: String, text: String, occurrence: Int = 0) {
    fieldBeside(label, occurrence).performTextReplacement(text)
    waitForIdle()
}

/**
 * Types into a field that commits on Enter or on leaving it, the way
 * [kst4contest.view.compose.Form.committed] does, and presses Enter.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.typeAndCommit(label: String, text: String, occurrence: Int = 0) {
    val field = fieldBeside(label, occurrence)
    field.performScrollTo().performClick()
    field.performTextReplacement(text)
    field.performKeyInput { pressKey(Key.Enter) }
    waitForIdle()
}

/**
 * Types into a field that commits on leaving it, then leaves it the way the operator does:
 * by clicking into the field beside [leaveTo]. No Enter is pressed, so only the focus loss
 * can commit the entry.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.typeAndLeave(
    label: String,
    text: String,
    leaveTo: String,
    occurrence: Int = 0,
    leaveToOccurrence: Int = 0,
) {
    val field = fieldBeside(label, occurrence)
    field.performScrollTo().performClick()
    waitForIdle()
    field.performTextReplacement(text)
    waitForIdle()
    fieldBeside(leaveTo, leaveToOccurrence).performScrollTo().performClick()
    waitForIdle()
}
