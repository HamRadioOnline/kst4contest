package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ThreadStateMessage
import java.util.Locale

/**
 * How the connection indicator reads at a glance.
 *
 * Three badges for nine states: mid contest nobody parses "SYNCING_SECOND_CHAT". What
 * the operator needs to know is whether something can be sent right now.
 */
enum class ConnectionBadge(val label: String) {
    ONLINE("LINK"),
    BUSY("LINK…"),
    OFFLINE("LINK!"),
}

/** The badge, its tooltip and the macOS menu title, all derived from one state. */
object ConnectionIndicator {

    /** No state at all is the state before the first callback, and that is not a link. */
    fun badgeFor(state: On4KstConnectionState?): ConnectionBadge =
        when (state ?: On4KstConnectionState.DISCONNECTED) {
            On4KstConnectionState.ONLINE -> ConnectionBadge.ONLINE

            On4KstConnectionState.CONNECTING,
            On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT,
            On4KstConnectionState.AUTHENTICATING,
            On4KstConnectionState.SYNCING_MAIN_CHAT,
            On4KstConnectionState.SYNCING_SECOND_CHAT,
            On4KstConnectionState.STOPPING -> ConnectionBadge.BUSY

            // A scheduled reconnect is not a working link, so it reads as offline.
            On4KstConnectionState.DISCONNECTED,
            On4KstConnectionState.RECONNECT_WAIT -> ConnectionBadge.OFFLINE
        }

    fun tooltipFor(state: On4KstConnectionState?, detail: String?): String {
        val effective = state ?: On4KstConnectionState.DISCONNECTED
        return kst4contest.view.i18n.CurrentStrings.get().statusLinkTooltip(effective.name, detailFor(state, detail))
    }

    /**
     * The detail line, as its own string.
     *
     * The tooltip carries it as a second line; the macOS menu carries it as its single
     * disabled item, which is the same information reaching the operator by the other route.
     */
    fun detailFor(state: On4KstConnectionState?, detail: String?): String {
        val effective = state ?: On4KstConnectionState.DISCONNECTED
        return if (detail.isNullOrBlank()) effective.name else detail
    }

    /**
     * Title of the macOS connection state menu, e.g. kst4contest.view.i18n.CurrentStrings.get().statusLinkConnected.
     *
     * Finer than the badge: in the menu bar there is room to distinguish shutting down
     * from connecting, and a reconnect from a dead link.
     */
    fun macOsMenuTitle(state: On4KstConnectionState?): String =
        when (state ?: On4KstConnectionState.DISCONNECTED) {
            On4KstConnectionState.ONLINE -> kst4contest.view.i18n.CurrentStrings.get().statusLinkConnected
            On4KstConnectionState.CONNECTING,
            On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT,
            On4KstConnectionState.AUTHENTICATING,
            On4KstConnectionState.SYNCING_MAIN_CHAT,
            On4KstConnectionState.SYNCING_SECOND_CHAT -> kst4contest.view.i18n.CurrentStrings.get().statusLinkConnecting
            On4KstConnectionState.STOPPING -> kst4contest.view.i18n.CurrentStrings.get().statusLinkDisconnecting
            On4KstConnectionState.RECONNECT_WAIT -> kst4contest.view.i18n.CurrentStrings.get().statusLinkReconnecting
            On4KstConnectionState.DISCONNECTED -> kst4contest.view.i18n.CurrentStrings.get().statusLinkDisconnected
        }
}

/**
 * A non-clickable notice that blinks for a while and then disappears.
 *
 * Both the sked reminder and the band upgrade hint work this way. They are not
 * dialogs on purpose — a dialog steals the keyboard, and the operator is typing.
 *
 * The blink itself belongs to the view; this holds what to draw and a revision the
 * view restarts the blink on, so a second reminder blinks again instead of sitting
 * there statically.
 */
class BlinkingNotice {

    var text: String by mutableStateOf("")
        private set

    var tooltip: String by mutableStateOf("")
        private set

    var visible: Boolean by mutableStateOf(false)
        private set

    var revision: Int by mutableStateOf(0)
        private set

    fun show(text: String, tooltip: String = text) {
        this.text = shorten(text)
        this.tooltip = tooltip
        visible = true
        revision++
    }

    fun hide() {
        visible = false
    }

    /**
     * Hides the notice only if it is still the one that [generation] showed.
     *
     * The blink runs in the view and is cancelled if the bar leaves the composition — a
     * splitter dragged shut, a profile switch — so the cleanup has to hide the notice, or
     * a magenta reminder stays in the status bar for the rest of the session. But a second
     * reminder arriving mid blink means the first blink's cleanup runs after the second is
     * already showing, and it must not take that one down with it.
     */
    fun hide(generation: Int) {
        if (revision == generation) {
            visible = false
        }
    }

    companion object {
        /** The badge shares a row with the menu, so the text is cut and the tooltip keeps it. */
        const val MAX_LABEL_LENGTH = 38
        const val BLINK_CYCLES = 24
        const val BLINK_CYCLE_MILLIS = 500L

        val blinkDurationMillis: Long get() = BLINK_CYCLES * BLINK_CYCLE_MILLIS

        fun shorten(text: String): String =
            if (text.length > MAX_LABEL_LENGTH) text.take(MAX_LABEL_LENGTH - 3) + "..." else text
    }
}

/** What makes a notice appear. */
object StatusIndicators {

    /** The wording the operator has learned to recognize; the double space is deliberate. */
    fun reminderText(callSignRaw: String?, minutesBefore: Int): String =
        "REMINDER: ${callSignRaw.orEmpty()}  T-${minutesBefore}m"

    /**
     * The band upgrade hint rides on the ordinary thread status messages, recognized by
     * name. Returns the badge text and the tooltip, or null if this message is about
     * something else.
     */
    fun bandUpgradeNoticeFor(key: String?, message: ThreadStateMessage?): Pair<String, String>? {
        if (message == null) return null

        val nick = message.threadNickName?.lowercase(Locale.ROOT).orEmpty()
        val lowerKey = key?.lowercase(Locale.ROOT).orEmpty()
        if (!lowerKey.contains("bandupgrade") && !nick.contains("bandupgrade")) return null

        // ThreadStateMessage has its own fallback, so this rarely fires; it is here so a
        // blank badge can never reach the status bar.
        val badge = message.runningInformationTextDescription
            ?.takeUnless { it.isBlank() } ?: "BAND+"
        val tooltip = message.runningInformation?.takeUnless { it.isBlank() } ?: badge
        return badge to tooltip
    }
}

/**
 * One button per background thread that reports itself, in the order they first did.
 *
 * The flash on every message is the point: it is how the operator sees a worker is
 * still alive without reading anything.
 */
class ThreadStatusButtons {

    private val bySource = LinkedHashMap<String, ThreadStatusButton>()
    private val actions = LinkedHashMap<String, () -> Unit>()
    private val statusProviders = LinkedHashMap<String, () -> ThreadStateMessage>()
    private val ordered = mutableStateListOf<ThreadStatusButton>()

    val buttons: List<ThreadStatusButton> get() = ordered

    fun update(sourceName: String, message: ThreadStateMessage) {
        val effectiveMessage = statusProviders[sourceName]?.invoke() ?: message
        val button = bySource.getOrPut(sourceName) {
            ThreadStatusButton(sourceName, actions[sourceName]).also { ordered.add(it) }
        }
        button.report(
            label = "$sourceName: ${effectiveMessage.runningInformationTextDescription}",
            tooltip = effectiveMessage.runningInformation.orEmpty(),
        )
    }

    /** Gives one status source an explicit action without changing any other source. */
    fun registerAction(sourceName: String, action: () -> Unit) {
        actions[sourceName] = action
        bySource[sourceName]?.setAction(action)
    }

    /** Resolves a status only when its queued UI update is actually applied. */
    fun registerStatusProvider(sourceName: String, provider: () -> ThreadStateMessage) {
        statusProviders[sourceName] = provider
    }

    companion object {
        /** How long the flash stays on; JavaFX used a 0.2 s PauseTransition. */
        const val HIGHLIGHT_MILLIS = 200L
    }
}

/** One background thread's button. */
class ThreadStatusButton(
    val sourceName: String,
    initialAction: (() -> Unit)? = null,
) {

    var label: String by mutableStateOf(sourceName)
        private set

    var tooltip: String by mutableStateOf("")
        private set

    /** True while the flash is on; the view clears it after HIGHLIGHT_MILLIS. */
    var highlighted: Boolean by mutableStateOf(false)
        private set

    var revision: Int by mutableStateOf(0)
        private set

    var action: (() -> Unit)? by mutableStateOf(initialAction)
        private set

    val hasAction: Boolean
        get() = action != null

    internal fun report(label: String, tooltip: String) {
        this.label = label
        this.tooltip = tooltip
        highlighted = true
        revision++
    }

    fun clearHighlight() {
        highlighted = false
    }

    internal fun setAction(action: () -> Unit) {
        this.action = action
    }

    /** Runs the registered action and reports whether this status is actionable. */
    fun activate(): Boolean {
        val current = action ?: return false
        current()
        return true
    }
}
