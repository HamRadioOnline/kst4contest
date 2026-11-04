package kst4contest.view.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kst4contest.controller.On4KstConnectionState

/**
 * The status bar along the top of the main window.
 *
 * In JavaFX this was one FlowPane holding the menu bar and the indicators side by side.
 * Compose puts the menu bar in the window's own scope (see [Kst4ContestMenuBar]), so
 * only the indicators are left here — which is also what finally makes the bar wrap
 * properly on a narrow window instead of pushing the badges out of sight.
 *
 * The connection and notice indicators remain non-clickable. A background-thread status is
 * actionable only when the application explicitly registers an action for that source; the
 * beacon uses this for its quick switch, while every other worker remains a plain label.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusBar(
    connectionState: On4KstConnectionState?,
    connectionDetail: String?,
    skedNotice: BlinkingNotice,
    bandUpgradeNotice: BlinkingNotice,
    threadButtons: ThreadStatusButtons,
    showConnectionBadge: Boolean = true,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // On macOS the connection state lives in the system menu bar instead.
        if (showConnectionBadge) {
            ConnectionBadgeView(connectionState, connectionDetail)
        }

        BlinkingNoticeView(skedNotice, SKED_BACKGROUND)
        BlinkingNoticeView(bandUpgradeNotice, BAND_UPGRADE_BACKGROUND)

        threadButtons.buttons.forEach { ThreadStatusButtonView(it) }
    }
}

/** rgba(255,0,255,0.85) — the sked reminder, the one colour nothing else in the UI uses. */
private val SKED_BACKGROUND = Color(0xD9FF00FF)

/** rgba(255,255,0,0.85) — the band upgrade hint. */
private val BAND_UPGRADE_BACKGROUND = Color(0xD9FFFF00)

private val NOTICE_SHAPE = RoundedCornerShape(6.dp)
private val NOTICE_PADDING = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

/**
 * The connection badge, at least as large as JavaFX pinned it (44 × 22).
 *
 * A minimum and not a fixed size: JavaFX also set setMaxSize(44,22), which this does not, so
 * a larger base font grows the badge rather than clipping its label. The three labels are
 * within a character of each other, so the bar barely reflows either way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConnectionBadgeView(state: On4KstConnectionState?, detail: String?) {
    val badge = ConnectionIndicator.badgeFor(state)
    val colors = badgeColors(badge)

    TooltipArea(tooltip = { TooltipCard(ConnectionIndicator.tooltipFor(state, detail)) }) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 44.dp, minHeight = 22.dp)
                .background(colors.background, NOTICE_SHAPE)
                .border(2.dp, colors.border, NOTICE_SHAPE)
                .padding(horizontal = 5.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge.label,
                color = colors.text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private class BadgeColors(val background: Color, val border: Color, val text: Color)

/**
 * The badge colours are deliberately not taken from the theme. They are traffic-light
 * colours and have to mean the same thing in both designs; a red that turns into the
 * dark theme's accent would be unreadable as a warning.
 */
private fun badgeColors(badge: ConnectionBadge): BadgeColors = when (badge) {
    ConnectionBadge.ONLINE -> BadgeColors(Color(0xFF238636), Color(0xFF56D364), Color.White)
    ConnectionBadge.BUSY -> BadgeColors(Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFF1B1B1B))
    ConnectionBadge.OFFLINE -> BadgeColors(Color(0xFFD50000), Color(0xFFFF6B6B), Color.White)
}

/**
 * A notice that blinks and then removes itself.
 *
 * The blink is the JavaFX timeline: bright, quarter-dim after 250 ms, bright again at
 * 500 ms, twenty-four times, then gone. It is long enough to catch the eye of someone
 * looking at the chat, and short enough not to become furniture.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlinkingNoticeView(notice: BlinkingNotice, background: Color) {
    if (!notice.visible) return

    val alpha = remember { Animatable(1f) }

    LaunchedEffect(notice.revision) {
        val generation = notice.revision
        try {
            alpha.snapTo(1f)
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = repeatable(
                    iterations = BlinkingNotice.BLINK_CYCLES,
                    animation = keyframes {
                        durationMillis = BlinkingNotice.BLINK_CYCLE_MILLIS.toInt()
                        1f at 0
                        0.25f at 250
                        1f at BlinkingNotice.BLINK_CYCLE_MILLIS.toInt()
                    },
                ),
            )
        } finally {
            // Also on cancellation: a blink cut short by the bar leaving the composition
            // would otherwise leave the notice showing for the rest of the session.
            notice.hide(generation)
        }
    }

    TooltipArea(tooltip = { TooltipCard(notice.tooltip) }) {
        Box(
            modifier = Modifier
                .alpha(alpha.value)
                .background(background, NOTICE_SHAPE)
                .padding(NOTICE_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = notice.text,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

/**
 * One background thread's state. It flashes in the accent colour whenever a message
 * arrives, which is how a running worker is told from a stuck one at a glance.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadStatusButtonView(button: ThreadStatusButton) {
    LaunchedEffect(button.revision) {
        try {
            delay(ThreadStatusButtons.HIGHLIGHT_MILLIS)
        } finally {
            // On cancellation too, so a button that left the composition mid flash does not
            // come back still lit.
            button.clearHighlight()
        }
    }

    val scheme = MaterialTheme.colorScheme
    val background = if (button.highlighted) scheme.primary else scheme.surfaceVariant
    val foreground = if (button.highlighted) scheme.onPrimary else scheme.onSurfaceVariant

    TooltipArea(tooltip = { TooltipCard(button.tooltip) }) {
        val actionModifier = if (button.hasAction) {
            Modifier.clickable(role = Role.Button) { button.activate() }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 22.dp)
                .background(background, NOTICE_SHAPE)
                .then(actionModifier)
                .padding(NOTICE_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = button.label,
                color = foreground,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

/** The tooltips in this bar carry the long text the badges had to cut. */
@Composable
internal fun TooltipCard(text: String) {
    if (text.isBlank()) return
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
