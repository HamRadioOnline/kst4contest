package kst4contest.view.compose

import androidx.compose.runtime.snapshots.Snapshot
import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The shortcut and snippet rosters are ChatPreferences.SimpleRoster, which notifies
 * nothing Compose can see. Mirroring them into Compose state is what makes the button row
 * above the send line follow the settings window — the reason ShortcutsTabState has been
 * calling refreshShortcutButtons into thin air since Etappe 3b.
 */
class MainWindowStateRosterTest(@TempDir private val tempDir: Path) {

    @Test
    fun `the shortcuts start out as what the profile holds`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.getLst_txtShortCutBtnList().setAll(listOf("MYQRG", "pse sked"))

        val mirror = RosterMirror(prefs.getLst_txtShortCutBtnList())

        assertEquals(listOf("MYQRG", "pse sked"), mirror.entries)
    }

    /**
     * The roster is written through by the settings window on every committed edit. The
     * mirror only catches up when it is told to, which is what the refresh hook is for.
     */
    @Test
    fun `a roster change reaches the mirror when it is refreshed`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.getLst_txtShortCutBtnList().setAll(listOf("MYQRG"))
        val mirror = RosterMirror(prefs.getLst_txtShortCutBtnList())

        prefs.getLst_txtShortCutBtnList().setAll(listOf("MYQRG", "SECONDQRG", "qrz?"))
        assertEquals(listOf("MYQRG"), mirror.entries, "the mirror moved without being refreshed")

        mirror.refresh()
        assertEquals(listOf("MYQRG", "SECONDQRG", "qrz?"), mirror.entries)
    }

    @Test
    fun `an emptied roster empties the mirror`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.getLst_txtShortCutBtnList().setAll(listOf("MYQRG"))
        val mirror = RosterMirror(prefs.getLst_txtShortCutBtnList())

        prefs.getLst_txtShortCutBtnList().clear()
        mirror.refresh()

        assertTrue(mirror.entries.isEmpty())
    }

    /**
     * The button row is drawn from this, so reading it has to register as a read — the
     * failure that produced most of this migration's invisible defects.
     */
    @Test
    fun `drawing the buttons subscribes to the mirror`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.getLst_txtShortCutBtnList().setAll(listOf("MYQRG"))
        val mirror = RosterMirror(prefs.getLst_txtShortCutBtnList())

        val read = mutableSetOf<String>()
        Snapshot.observe(readObserver = { read += it.toString() }) {
            mirror.entries.forEach { it.length }
        }

        assertTrue(read.isNotEmpty(), "the mirror was read without subscribing")
    }
}

/**
 * The splitters of the main window against the positions the profile actually stores.
 *
 * A mismatch here is silent and total: SplitterState falls back to an even split, so
 * every operator would lose the layout they arranged without any error saying so.
 */
class MainWindowSplitterDefaultsTest(@TempDir private val tempDir: Path) {

    private val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))

    @Test
    fun `the message column has as many dividers as the profile stores positions`() {
        val stored = prefs.getGUImessageSectionSplitpane_dividerposition()
        assertEquals(
            MainWindowState.MESSAGE_PANE_COUNT - 1,
            stored.size,
            "the message splitter has ${MainWindowState.MESSAGE_PANE_COUNT} panes but the " +
                "profile stores ${stored.size} positions",
        )

        val splitter = SplitterState(MainWindowState.MESSAGE_PANE_COUNT, stored) {}
        assertEquals(
            stored.map { it.toFloat() },
            splitter.positions,
            "the stored positions were rejected and silently replaced by an even split",
        )
    }

    @Test
    fun `the station column has as many dividers as the profile stores positions`() {
        val stored = prefs.getGUImainWindowRightSplitPane_dividerposition()
        assertEquals(MainWindowState.RIGHT_PANE_COUNT - 1, stored.size)

        val splitter = SplitterState(MainWindowState.RIGHT_PANE_COUNT, stored) {}
        assertEquals(stored.map { it.toFloat() }, splitter.positions)
    }

    @Test
    fun `the outer splitter takes the one position the profile stores`() {
        val stored = prefs.getGUImainWindowLeftSplitPane_dividerposition()
        assertEquals(1, stored.size)

        val splitter = SplitterState(paneCount = 2, stored = stored) {}
        assertEquals(stored.map { it.toFloat() }, splitter.positions)
    }

    @Test
    fun `the selected station content splitter takes the one stored profile position`() {
        val stored = prefs.getGUIselectedCallSignSplitPane_dividerposition()
        assertEquals(1, stored.size)

        val splitter = SplitterState(paneCount = MainWindowState.SELECTED_STATION_PANE_COUNT, stored = stored) {}
        assertEquals(stored.map { it.toFloat() }, splitter.positions)
    }
}

/**
 * What the window draws about its surroundings — the link, and which other windows are
 * open — has to be Compose state as well.
 *
 * These come from the controller and from AtomicBooleans inside the window hosts, none of
 * which Compose can see. Reading them through a plain lambda at composition time draws them
 * once: the badge would freeze on whatever the link was when the window opened, and the
 * menu's "hide options" would keep saying the wrong thing.
 */
class MainWindowSurroundingsTest {

    @Test
    fun `the connection state starts disconnected and follows what it is told`() {
        val state = MainWindowSurroundings()

        assertEquals(On4KstConnectionState.DISCONNECTED, state.connectionState)

        state.connectionState = On4KstConnectionState.ONLINE
        assertEquals(On4KstConnectionState.ONLINE, state.connectionState)
    }

    @Test
    fun `drawing the surroundings subscribes to every value`() {
        val state = MainWindowSurroundings()
        val read = mutableSetOf<String>()

        Snapshot.observe(readObserver = { read += it.toString() }) {
            state.connectionState
            state.connectionDetail
            state.settingsWindowOpen
            state.monitorWindowOpen
        }

        assertEquals(4, read.size, "a value was read without subscribing: $read")
    }

    /**
     * The menu and the badge must never disagree about the link — they are two renderings of
     * one fact, and an operator comparing them would not know which to believe.
     */
    @Test
    fun `the menu follows the same connection state as the badge`() {
        val menu = MainMenuState()
        val state = MainWindowSurroundings(menu)

        state.connectionState = On4KstConnectionState.ONLINE

        assertEquals(On4KstConnectionState.ONLINE, menu.connectionState)
        assertTrue(menu.canUseChatActions)
    }
}
