package kst4contest.view.compose

import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ThreadStateMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The status bar tells the operator whether the link is alive and reminds them of a
 * sked. Both are read at a glance mid contest, so the mapping from state to badge is
 * pinned here rather than left to the eye.
 */
class StatusIndicatorStateTest {

    // ---- the connection badge ----------------------------------------------

    @Test
    fun `online shows the plain badge`() {
        assertEquals(ConnectionBadge.ONLINE, ConnectionIndicator.badgeFor(On4KstConnectionState.ONLINE))
        assertEquals("LINK", ConnectionBadge.ONLINE.label)
    }

    @Test
    fun `every step of the handshake counts as busy`() {
        listOf(
            On4KstConnectionState.CONNECTING,
            On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT,
            On4KstConnectionState.AUTHENTICATING,
            On4KstConnectionState.SYNCING_MAIN_CHAT,
            On4KstConnectionState.SYNCING_SECOND_CHAT,
            On4KstConnectionState.STOPPING,
        ).forEach {
            assertEquals(ConnectionBadge.BUSY, ConnectionIndicator.badgeFor(it), "$it")
        }
        assertEquals("LINK…", ConnectionBadge.BUSY.label)
    }

    /**
     * A scheduled reconnect is not a working link. It reads as offline on purpose: the
     * operator must see that nothing is being sent right now.
     */
    @Test
    fun `waiting for a reconnect reads as offline`() {
        assertEquals(ConnectionBadge.OFFLINE, ConnectionIndicator.badgeFor(On4KstConnectionState.RECONNECT_WAIT))
        assertEquals(ConnectionBadge.OFFLINE, ConnectionIndicator.badgeFor(On4KstConnectionState.DISCONNECTED))
        assertEquals("LINK!", ConnectionBadge.OFFLINE.label)
    }

    /** Before the first callback there is no state, and no state is not a link. */
    @Test
    fun `no state at all reads as offline`() {
        assertEquals(ConnectionBadge.OFFLINE, ConnectionIndicator.badgeFor(null))
    }

    @Test
    fun `no connection state is left unmapped`() {
        On4KstConnectionState.entries.forEach {
            assertNotNull(ConnectionIndicator.badgeFor(it), "$it has no badge")
        }
    }

    @Test
    fun `the tooltip names the state and the detail`() {
        assertEquals(
            "ON4KST link: ONLINE\nLogged in as DN9APW",
            ConnectionIndicator.tooltipFor(On4KstConnectionState.ONLINE, "Logged in as DN9APW"),
        )
    }

    @Test
    fun `a missing detail falls back to the state name`() {
        assertEquals(
            "ON4KST link: DISCONNECTED\nDISCONNECTED",
            ConnectionIndicator.tooltipFor(On4KstConnectionState.DISCONNECTED, "   "),
        )
        assertEquals(
            "ON4KST link: DISCONNECTED\nDISCONNECTED",
            ConnectionIndicator.tooltipFor(On4KstConnectionState.DISCONNECTED, null),
        )
    }

    // ---- the macOS menu title ---------------------------------------------

    /**
     * The menu title splits finer than the badge: shutting down and reconnecting each
     * get their own wording, because in the menu bar there is room to say it.
     */
    @Test
    fun `the macOS title distinguishes what the badge merges`() {
        assertEquals("🟢 LINK: Connected", ConnectionIndicator.macOsMenuTitle(On4KstConnectionState.ONLINE))
        assertEquals("🟡 LINK: Connecting…", ConnectionIndicator.macOsMenuTitle(On4KstConnectionState.CONNECTING))
        assertEquals("🟡 LINK: Disconnecting…", ConnectionIndicator.macOsMenuTitle(On4KstConnectionState.STOPPING))
        assertEquals("🔴 LINK: Reconnecting…", ConnectionIndicator.macOsMenuTitle(On4KstConnectionState.RECONNECT_WAIT))
        assertEquals("🔴 LINK: Disconnected", ConnectionIndicator.macOsMenuTitle(On4KstConnectionState.DISCONNECTED))
        assertEquals("🔴 LINK: Disconnected", ConnectionIndicator.macOsMenuTitle(null))
    }

    /**
     * The detail line under the macOS menu title. Same text the tooltip's second line
     * carries, because it is the same information reaching the operator by the other route.
     */
    @Test
    fun `the macOS menu carries the detail as its own line`() {
        assertEquals(
            "Logged in as DN9APW",
            ConnectionIndicator.detailFor(On4KstConnectionState.ONLINE, "Logged in as DN9APW"),
        )
    }

    @Test
    fun `a missing macOS detail falls back to the state name`() {
        assertEquals(
            "DISCONNECTED",
            ConnectionIndicator.detailFor(On4KstConnectionState.DISCONNECTED, "  "),
        )
        assertEquals("DISCONNECTED", ConnectionIndicator.detailFor(null, null))
    }

    @Test
    fun `no connection state is left without a macOS title`() {
        On4KstConnectionState.entries.forEach {
            assertTrue(
                ConnectionIndicator.macOsMenuTitle(it).contains("LINK"),
                "$it has no macOS title",
            )
        }
    }

    // ---- the blinking notices ---------------------------------------------

    @Test
    fun `a notice starts hidden`() {
        val notice = BlinkingNotice()
        assertFalse(notice.visible)
        assertEquals("", notice.text)
    }

    @Test
    fun `showing a notice reveals it with its text`() {
        val notice = BlinkingNotice()
        notice.show("REMINDER: DL0ABC  T-5m")
        assertTrue(notice.visible)
        assertEquals("REMINDER: DL0ABC  T-5m", notice.text)
        assertEquals("REMINDER: DL0ABC  T-5m", notice.tooltip)
    }

    /**
     * The badge sits in one row with the menu, so a long text is cut. The full text
     * stays in the tooltip — nothing is lost, it is only not shouted.
     */
    @Test
    fun `a long text is cut for the badge but kept in the tooltip`() {
        val long = "REMINDER: DL0ABC/P  T-10m on 1296 MHz, please turn the antenna"
        val notice = BlinkingNotice()
        notice.show(long)
        assertEquals(38, notice.text.length)
        assertTrue(notice.text.endsWith("..."))
        assertEquals(long.take(35) + "...", notice.text)
        assertEquals(long, notice.tooltip)
    }

    @Test
    fun `a text that just fits is left alone`() {
        val exactly38 = "x".repeat(38)
        val notice = BlinkingNotice()
        notice.show(exactly38)
        assertEquals(exactly38, notice.text)
    }

    @Test
    fun `a separate tooltip survives the cut`() {
        val notice = BlinkingNotice()
        notice.show("BAND+ DL0ABC", tooltip = "DL0ABC is still missing 23cm and 13cm")
        assertEquals("BAND+ DL0ABC", notice.text)
        assertEquals("DL0ABC is still missing 23cm and 13cm", notice.tooltip)
    }

    @Test
    fun `hiding a notice takes it out of the bar`() {
        val notice = BlinkingNotice()
        notice.show("REMINDER: DL0ABC  T-5m")
        notice.hide()
        assertFalse(notice.visible)
    }

    /**
     * A second reminder while the first is still blinking must blink again, not sit
     * there statically. The revision is what the blink restarts on.
     */
    @Test
    fun `a second notice restarts the blink`() {
        val notice = BlinkingNotice()
        notice.show("first")
        val afterFirst = notice.revision
        notice.show("second")
        assertTrue(notice.revision > afterFirst)
        assertEquals("second", notice.text)
    }

    /**
     * The blink runs in the view, so it is cancelled if the bar leaves the composition —
     * a splitter dragged shut, a profile switch. Without a generation-aware hide the
     * notice would stay visible for the rest of the session: a magenta reminder stuck in
     * the status bar for a sked that has long passed.
     */
    @Test
    fun `a notice can be hidden by the generation that showed it`() {
        val notice = BlinkingNotice()
        notice.show("REMINDER: DL0ABC  T-5m")
        val generation = notice.revision

        notice.hide(generation)

        assertFalse(notice.visible)
    }

    /**
     * And a stale blink must not hide the notice that replaced it. When a second reminder
     * arrives while the first is still blinking, the first blink's cleanup runs after the
     * second has already been shown.
     */
    @Test
    fun `an older generation cannot hide a newer notice`() {
        val notice = BlinkingNotice()
        notice.show("first")
        val firstGeneration = notice.revision

        notice.show("second")
        notice.hide(firstGeneration)

        assertTrue(notice.visible, "the stale blink hid the reminder that replaced it")
        assertEquals("second", notice.text)
    }

    @Test
    fun `the blink lasts as long as it did in JavaFX`() {
        assertEquals(24, BlinkingNotice.BLINK_CYCLES)
        assertEquals(500L, BlinkingNotice.BLINK_CYCLE_MILLIS)
        assertEquals(12_000L, BlinkingNotice.blinkDurationMillis)
    }

    // ---- what triggers a notice -------------------------------------------

    @Test
    fun `the reminder text names the call and the minutes left`() {
        assertEquals("REMINDER: DL0ABC  T-5m", StatusIndicators.reminderText("DL0ABC", 5))
    }

    @Test
    fun `a band upgrade is recognized by the key`() {
        val notice = StatusIndicators.bandUpgradeNoticeFor(
            key = "BandUpgradeWatcher",
            message = threadState(nick = "watcher", description = "BAND+ DL0ABC", info = "23cm missing"),
        )
        assertNotNull(notice)
        assertEquals("BAND+ DL0ABC", notice!!.first)
        assertEquals("23cm missing", notice.second)
    }

    @Test
    fun `a band upgrade is recognized by the thread nickname`() {
        assertNotNull(
            StatusIndicators.bandUpgradeNoticeFor(
                key = "worker-7",
                message = threadState(nick = "bandUpgrade", description = "BAND+", info = "x"),
            ),
        )
    }

    @Test
    fun `any other thread message is not a band upgrade`() {
        assertNull(
            StatusIndicators.bandUpgradeNoticeFor(
                key = "ClusterReader",
                message = threadState(nick = "cluster", description = "12 spots", info = "running"),
            ),
        )
        assertNull(StatusIndicators.bandUpgradeNoticeFor(key = "bandupgrade", message = null))
        assertNull(StatusIndicators.bandUpgradeNoticeFor(key = null, message = null))
    }

    /**
     * ThreadStateMessage never hands out a blank description — it falls back to "on"
     * or "FAILED" itself. So the notice shows that, and the tooltip, which has no such
     * fallback, borrows the button text rather than appearing empty.
     */
    @Test
    fun `a band upgrade without its own wording borrows what it has`() {
        val notice = StatusIndicators.bandUpgradeNoticeFor(
            key = "bandupgrade",
            message = ThreadStateMessage("x", true, "   ", false),
        )
        assertNotNull(notice)
        assertEquals("on", notice!!.first)
        assertEquals("on", notice.second)
    }

    // ---- the background thread buttons ------------------------------------

    @Test
    fun `a thread reports itself into the bar once`() {
        val buttons = ThreadStatusButtons()
        buttons.update("ClusterReader", threadState(nick = "cluster", description = "12 spots", info = "running"))
        buttons.update("ClusterReader", threadState(nick = "cluster", description = "13 spots", info = "still running"))

        assertEquals(1, buttons.buttons.size)
        assertEquals("ClusterReader: 13 spots", buttons.buttons.single().label)
        assertEquals("still running", buttons.buttons.single().tooltip)
    }

    /** The bar is read left to right; a thread keeps the place it first took. */
    @Test
    fun `threads keep the order they first reported in`() {
        val buttons = ThreadStatusButtons()
        buttons.update("Cluster", threadState(nick = "c", description = "a", info = "a"))
        buttons.update("Sked", threadState(nick = "s", description = "b", info = "b"))
        buttons.update("Cluster", threadState(nick = "c", description = "c", info = "c"))

        assertEquals(listOf("Cluster", "Sked"), buttons.buttons.map { it.sourceName })
    }

    /**
     * The flash is the whole point of these buttons: it shows the thread is alive.
     * It is on when the message lands and the view times it out again.
     */
    @Test
    fun `a fresh message flashes the button`() {
        val buttons = ThreadStatusButtons()
        buttons.update("Cluster", threadState(nick = "c", description = "a", info = "a"))
        val button = buttons.buttons.single()
        assertTrue(button.highlighted)

        button.clearHighlight()
        assertFalse(button.highlighted)

        buttons.update("Cluster", threadState(nick = "c", description = "b", info = "b"))
        assertTrue(button.highlighted)
    }

    @Test
    fun `the flash lasts as long as it did in JavaFX`() {
        assertEquals(200L, ThreadStatusButtons.HIGHLIGHT_MILLIS)
    }

    // ---- what a draw subscribes to ----------------------------------------

    /**
     * The bar is drawn from these values, so reading them has to register as a read.
     * A plain field would draw once and then quietly go stale — the failure mode that
     * cost this migration eleven defects, none of which any assertion could see.
     */
    @Test
    fun `drawing a notice subscribes to what can change`() {
        val notice = BlinkingNotice()
        notice.show("REMINDER: DL0ABC  T-5m")

        val read = mutableSetOf<String>()
        Snapshot.observe(readObserver = { read += it.toString() }) {
            notice.visible
            notice.text
            notice.tooltip
            notice.revision
        }

        assertEquals(4, read.size, "a notice value was read without subscribing: $read")
    }

    @Test
    fun `drawing the thread buttons subscribes to the list and the labels`() {
        val buttons = ThreadStatusButtons()
        buttons.update("Cluster", threadState(nick = "c", description = "a", info = "a"))

        val read = mutableSetOf<String>()
        Snapshot.observe(readObserver = { read += it.toString() }) {
            buttons.buttons.forEach {
                it.label
                it.tooltip
                it.highlighted
            }
        }

        // the list itself plus three values of the single button
        assertEquals(4, read.size, "a button value was read without subscribing: $read")
    }

    /**
     * A thread that reports again does not change the list, only its own button. If the
     * label were not state, the bar would freeze on the first message a worker sent.
     */
    @Test
    fun `a later message changes the button without touching the list`() {
        val buttons = ThreadStatusButtons()
        buttons.update("Cluster", threadState(nick = "c", description = "a", info = "a"))
        val listBefore = buttons.buttons
        val button = buttons.buttons.single()

        buttons.update("Cluster", threadState(nick = "c", description = "b", info = "b"))

        assertSame(listBefore, buttons.buttons)
        assertSame(button, buttons.buttons.single())
        assertEquals("Cluster: b", button.label)
    }

    @Test
    fun `an action registered before the first report reaches the created button`() {
        val buttons = ThreadStatusButtons()
        var clicks = 0
        buttons.registerAction("MyBeacon") { clicks++ }

        buttons.update("MyBeacon", threadState(nick = "beacon", description = "off", info = "disabled"))
        buttons.buttons.single().activate()

        assertEquals(1, clicks)
    }

    @Test
    fun `an action registered after a report reaches the existing button`() {
        val buttons = ThreadStatusButtons()
        var clicks = 0
        buttons.update("MyBeacon", threadState(nick = "beacon", description = "off", info = "disabled"))

        buttons.registerAction("MyBeacon") { clicks++ }
        buttons.buttons.single().activate()

        assertEquals(1, clicks)
    }

    @Test
    fun `a thread without an action cannot be activated`() {
        val buttons = ThreadStatusButtons()
        buttons.update("Cluster", threadState(nick = "cluster", description = "on", info = "running"))

        assertFalse(buttons.buttons.single().activate())
    }

    @Test
    fun `a status provider replaces a stale worker snapshot when the UI applies it`() {
        val buttons = ThreadStatusButtons()
        var current = threadState(nick = "beacon", description = "on", info = "enabled")
        buttons.registerStatusProvider("MyBeacon") { current }
        val queuedSnapshot = current
        current = threadState(nick = "beacon", description = "off", info = "disabled")

        buttons.update("MyBeacon", queuedSnapshot)

        assertEquals("MyBeacon: off", buttons.buttons.single().label)
        assertEquals("disabled", buttons.buttons.single().tooltip)
    }

    private fun threadState(nick: String, description: String, info: String): ThreadStateMessage =
        ThreadStateMessage(nick, true, info, false).apply {
            setRunningInformationTextDescription(description)
        }
}
