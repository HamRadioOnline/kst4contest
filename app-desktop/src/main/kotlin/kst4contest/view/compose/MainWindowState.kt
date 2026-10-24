package kst4contest.view.compose

import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage
import kst4contest.model.ChatMember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatPreferences
import kst4contest.observe.SimpleRoster

/**
 * Everything the main window draws, in one object.
 *
 * This is the shape the profile switch needs. Today a switch tears the runtime down and
 * builds a new Kst4ContestApplication, because the controls are inline-created instance
 * fields and there is no other way to be rid of them. Once the window is a pure function
 * of this object, switching profiles is replacing it — the reason the JavaFX version had
 * to restart itself is gone.
 *
 * Nothing here holds a window or a thread; those belong to the runtime that builds this. It
 * does hold three preference listeners, which is why [release] exists and has to be called
 * when the state is dropped.
 */
class MainWindowState(
    val activeBands: Set<kst4contest.model.Band>,
    val prefs: ChatPreferences,

    /** The station list with its filters: the operator's main working surface. */
    val stations: DataTableState<ChatMember>,
    val stationFilter: StationFilterState,

    /** Messages addressed to this station, in their own pane above the send line. */
    val directedMessages: DataTableState<ChatMessage>,

    /** The three tabs at the bottom. */
    val publicMessages: DataTableState<ChatMessage>,
    val clusterMessages: DataTableState<ClusterMessage>,
    val qsoOfTheOther: DataTableState<ChatMessage>,

    val selectedStationMessages: DataTableState<ChatMessage>,
    val selectedStation: SelectedStationState,
    val topPriority: TopPriorityState,
    val timeline: TimelineState,
    val chatInput: ChatInputState,
    val menu: MainMenuState,

    /** The link and the other windows, pushed in because none of it is Compose state at source. */
    val surroundings: MainWindowSurroundings,

    /** The status indicators along the top. */
    val skedNotice: BlinkingNotice,
    val bandUpgradeNotice: BlinkingNotice,
    val threadButtons: ThreadStatusButtons,

    /** The splitters whose positions the operator has arranged and stored. */
    val outerSplitter: SplitterState,
    val messageSplitter: SplitterState,
    val rightSplitter: SplitterState,
    val selectedStationSplitter: SplitterState = SplitterState(
        SELECTED_STATION_PANE_COUNT,
        doubleArrayOf(0.55),
    ) {},
    val newJoined: NewJoinedState = NewJoinedState(onSelect = {}),
    val reachability: ReachabilityState = ReachabilityState(activeBands),
) {

    /** Explicit map analysis requested from either station context menu. */
    val mapReachability = MapReachabilityState(prefs.stn_loginLocatorMainCat.orEmpty())

    /** The shortcut buttons above the send line. */
    /**
     * The one path that selects a station for the whole application (panel, table, score
     * service, send line). Set by the host after construction; other windows, such as the
     * map, go through it instead of touching [selectedStation] alone. Null clears.
     */
    var stationSelector: ((ChatMember?, StationSelectionSource) -> Unit)? = null

    /** Application callback that rebuilds the controller and Compose station filters. */
    var stationFiltersChanged: (() -> Unit)? = null

    /** Debounced persistence hook shared by the main and map windows. */
    var layoutSaveRequested: (() -> Unit)? = null

    /** Contactinfo-equivalent manual Worked action supplied by the controller host. */
    var markStationWorked: ((ChatMember, kst4contest.model.Band) -> Boolean)? = null

    /** Persistent any-band Worked gross fields used by the optional map overlay. */
    var workedGrossFieldsProvider: (() -> Set<String>) = { emptySet() }

    val stationSelection: StationSelectionState
        get() = selectedStation.stationSelection

    /** Routes every explicit station choice through the application-level action. */
    fun selectStation(member: ChatMember?, source: StationSelectionSource) {
        mapReachability.selectionChanged(StationIdentity.of(member))
        val selector = stationSelector
        if (selector != null) {
            selector(member, source)
        } else {
            selectedStation.select(member, source)
        }
    }

    fun resetStationFilters() {
        stationFilter.reset()
        stationFiltersChanged?.invoke()
    }

    fun requestLayoutSave() {
        layoutSaveRequested?.invoke()
    }

    val shortcuts = RosterMirror(prefs.getLst_txtShortCutBtnList())

    /** The snippets behind Ctrl+1..Ctrl+0 and the right-click menus. */
    val snippets = RosterMirror(prefs.getLst_txtSnipList())

    /**
     * The four small fields beside the send controls. Mirrored rather than read straight
     * from the preferences, because all of them move without the operator touching them: the
     * rotator writes the heading, and the frequency follower writes the own QRG.
     */
    val ownQrgMain = ObservedValue(prefs.getMYQRGFirstCat())
    val ownQrgSecond = ObservedValue(prefs.getMYQRGSecondCat())
    val antennaQtf = ObservedValue(prefs.getActualQTF())
    val antennaQtf2 = ObservedValue(prefs.getActualQTF2())

    /**
     * Stops following the preferences. Called when this state is dropped — a profile switch
     * builds a new window over the same preferences, and a leaked follower keeps the old
     * one alive and writing.
     */
    fun release() {
        ownQrgMain.release()
        ownQrgSecond.release()
        antennaQtf.release()
        antennaQtf2.release()
    }

    companion object {

        /** The five panes of the message column, in the order they are stacked. */
        const val MESSAGE_PANE_COUNT = 5

        /** Station list, top priorities, selected station. */
        const val RIGHT_PANE_COUNT = 3

        /** Selected-station message table and its scrollable action area. */
        const val SELECTED_STATION_PANE_COUNT = 2
    }
}


/**
 * A roster from the preferences, mirrored into Compose state.
 *
 * SimpleRoster notifies its own listeners, none of which Compose can see, so a view drawn
 * straight from one would render once and then go stale — the failure mode that produced
 * most of this migration's invisible defects. The mirror is what the settings window's
 * refresh hooks have been calling into thin air since Etappe 3b.
 */
class RosterMirror(private val roster: SimpleRoster<String>?) {

    private val live = mutableStateListOf<String>().apply {
        addAll(roster?.snapshot().orEmpty())
    }

    /** What a view reads; a read here is a subscription. */
    val entries: List<String> get() = live

    /** Catches the mirror up with the roster. Called after every committed edit. */
    fun refresh() {
        val current = roster?.snapshot().orEmpty()
        if (current == live.toList()) return
        live.clear()
        live.addAll(current)
    }
}


/**
 * What the window shows about its surroundings: the link, and which other windows are open.
 *
 * None of this is Compose state at its source — the connection state lives in the controller,
 * the window flags in AtomicBooleans inside the hosts — so it is pushed in here instead of
 * being read through a lambda at composition time. Read that way it would be drawn once: the
 * badge frozen on whatever the link was when the window opened, and the menu's "hide options"
 * saying the wrong thing for the rest of the session.
 *
 * @param menu kept in step with the connection state, because the badge and the menu are two
 *        renderings of one fact and an operator comparing them must not have to choose
 */
class MainWindowSurroundings(private val menu: MainMenuState = MainMenuState()) {

    private val liveConnectionState =
        mutableStateOf(On4KstConnectionState.DISCONNECTED)

    var connectionState: On4KstConnectionState
        get() = liveConnectionState.value
        set(value) {
            liveConnectionState.value = value
            menu.connectionState = value
        }

    var connectionDetail: String by mutableStateOf(kst4contest.view.i18n.CurrentStrings.get().statusNoConnection)

    var settingsWindowOpen: Boolean by mutableStateOf(false)

    var monitorWindowOpen: Boolean by mutableStateOf(false)

    /**
     * Whether SO2R is switched on. Compose state, because the preference is not: the
     * application pushes the stored value in at start-up and after every settings Apply.
     */
    var so2rEnabled: Boolean by mutableStateOf(false)

    /** SO2R with rotator 2 enabled, i.e. antenna 2 can be turned; pushed in like [so2rEnabled]. */
    var secondAntennaOffered: Boolean by mutableStateOf(false)
}
