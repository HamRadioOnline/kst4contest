package kst4contest.view.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.utils.PlatformUtils
import java.util.function.Consumer

/**
 * The main window, assembled.
 *
 * The JavaFX original built this in 2164 lines inside start(). What is left here is the
 * arrangement — every part was built and tested on its own in 5a, 5b and tasks 10 to 13d.
 * That is the point of the split: this file says how the window is put together and
 * nothing else, so a change to the arrangement cannot break a table.
 *
 * The window is a pure function of [MainWindowState]. That is what makes the profile
 * switch a state swap instead of a restart.
 */
@Composable
fun FrameWindowScope.MainWindow(
    state: MainWindowState,
    actions: MainMenuActions,
    openInBrowser: Consumer<String>,
    onShowOnMap: () -> Unit,
    onShowPathInAirScout: () -> Unit,
    onStationSelected: (ChatMember) -> Unit,
    /**
     * A clicked private message prepares a reply to it. JavaFX resolved the receiver out of
     * the message text when the sender was the local station, so replying to one's own
     * message aims at whoever it was addressed to rather than at oneself.
     */
    onDirectedMessageClicked: (ChatMessage) -> Unit,
    onMessageStationClicked: (ChatMessage, StationSelectionSource) -> Unit,
    /** Whether a message came from this station; decides its row colour. */
    isOwnMessage: (ChatMessage) -> Boolean,
    /** How old a message is, for the fading highlight. */
    messageAgeSeconds: (ChatMessage) -> Long,
    onFiltersChanged: () -> Unit,
    onShowMorePriorities: () -> Unit,
    onCandidateClicked: (TimelineCandidate) -> Unit,
) {
    /*
     * macOS gets the system menu bar at the top of the screen, which is where that platform
     * expects it and which Swing draws natively. Everywhere else Compose's MenuBar meant an
     * unthemed Metal menu above a themed window, so the bar is drawn in the window instead —
     * see Kst4ContestMenuRow.
     */
    if (PlatformUtils.isMacOs()) {
        Kst4ContestMenuBar(
            state = state.menu,
            actions = actions,
            settingsWindowOpen = state.surroundings.settingsWindowOpen,
            monitorWindowOpen = state.surroundings.monitorWindowOpen,
            connectionState = state.surroundings.connectionState,
            connectionDetail = state.surroundings.connectionDetail,
        )
    }

    val menuRow = remember { MainMenuRowState() }

    /*
     * The window-wide keys, where JavaFX had them: on the scene. Enter sends and Escape
     * clears wherever the focus is, and Ctrl+1..Ctrl+0 inserts a snippet — on the field alone
     * they would be gone the moment the operator clicked a station or a message row, which is
     * most of the time.
     */
    Surface(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                /*
                 * Not while a menu is down. This is a PREVIEW handler, so it runs before the
                 * dropdown sees the key — without the guard, Escape to dismiss a menu would
                 * first clear the operator's half-typed message.
                 */
                if (menuRow.anyOpen) {
                    false
                } else {
                    WindowShortcuts.handle(
                        event = event,
                        send = state.chatInput::send,
                        clear = state.chatInput::clear,
                        insertSnippet = { index ->
                            state.chatInput.insertSnippet(state.snippets.entries, index)
                        },
                    )
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            if (!PlatformUtils.isMacOs()) {
                Kst4ContestMenuRow(
                    state = state.menu,
                    actions = actions,
                    settingsWindowOpen = state.surroundings.settingsWindowOpen,
                    monitorWindowOpen = state.surroundings.monitorWindowOpen,
                    rowState = menuRow,
                )

                HorizontalDivider(thickness = Density.HAIRLINE)
            }

            StatusBar(
                connectionState = state.surroundings.connectionState,
                connectionDetail = state.surroundings.connectionDetail,
                skedNotice = state.skedNotice,
                bandUpgradeNotice = state.bandUpgradeNotice,
                threadButtons = state.threadButtons,
                showConnectionBadge = !PlatformUtils.isMacOs(),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(thickness = Density.HAIRLINE)

            SplitterPane(
                state = state.outerSplitter,
                orientation = SplitterOrientation.HORIZONTAL,
                modifier = Modifier.fillMaxSize(),
                panes = listOf(
                    {
                        MessageColumn(
                            state = state,
                            onCandidateClicked = onCandidateClicked,
                            onDirectedMessageClicked = onDirectedMessageClicked,
                            onMessageStationClicked = onMessageStationClicked,
                            isOwnMessage = isOwnMessage,
                            messageAgeSeconds = messageAgeSeconds,
                        )
                    },
                    {
                        StationColumn(
                            state = state,
                            openInBrowser = openInBrowser,
                            onShowOnMap = onShowOnMap,
                            onShowPathInAirScout = onShowPathInAirScout,
                            onStationSelected = onStationSelected,
                            onFiltersChanged = onFiltersChanged,
                            onShowMorePriorities = onShowMorePriorities,
                        )
                    },
                ),
            )
        }
    }
}

/**
 * The left column: what was said, and what this station says back.
 *
 * Five panes in the order the operator has looked at them for years — directed messages
 * at the top, then the shortcut buttons, the timeline, the send line, and the public
 * message tabs at the bottom.
 */
@Composable
private fun MessageColumn(
    state: MainWindowState,
    onCandidateClicked: (TimelineCandidate) -> Unit,
    onDirectedMessageClicked: (ChatMessage) -> Unit,
    onMessageStationClicked: (ChatMessage, StationSelectionSource) -> Unit,
    isOwnMessage: (ChatMessage) -> Boolean,
    messageAgeSeconds: (ChatMessage) -> Long,
) {
    SplitterPane(
        state = state.messageSplitter,
        orientation = SplitterOrientation.VERTICAL,
        modifier = Modifier.fillMaxSize(),
        minimumPaneSizes = listOf(
            SplitterState.DEFAULT_MIN_PANE_PX.dp,
            Density.CONTROL_HEIGHT,
            TimelineGeometry.HEIGHT.dp,
            Density.CONTROL_HEIGHT,
            SplitterState.DEFAULT_MIN_PANE_PX.dp,
        ),
        panes = listOf(
            {
                SnippetContextMenu(state.snippets.entries, state.chatInput) {
                    DataTableView(
                        state = state.directedMessages,
                        modifier = Modifier.fillMaxSize(),
                        rowStyle = { message ->
                            DirectedMessageRowStyle.accentFor(
                                ownMessage = isOwnMessage(message),
                                ageSeconds = messageAgeSeconds(message),
                            )
                        },
                        onRowClick = onDirectedMessageClicked,
                    )
                }
            },
            { ShortcutButtonRow(state.shortcuts.entries, state.chatInput) },
            {
                TimelineView(
                    state = state.timeline,
                    onCandidateClicked = onCandidateClicked,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            {
                ChatInputRow(
                    state = state.chatInput,
                    prefs = state.prefs,
                    ownQrgMain = state.ownQrgMain,
                    ownQrgSecond = state.ownQrgSecond,
                    antennaQtf = state.antennaQtf,
                    antennaQtf2 = state.antennaQtf2,
                    so2rEnabled = state.surroundings.so2rEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            {
                SnippetContextMenu(state.snippets.entries, state.chatInput) {
                    MessageTabs(
                        publicMessages = state.publicMessages,
                        clusterMessages = state.clusterMessages,
                        qsoOfTheOther = state.qsoOfTheOther,
                        onStationMessageClicked = onMessageStationClicked,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        ),
    )
}

/**
 * The right column: who is on, who to work next, and everything about the one selected.
 */
@Composable
private fun StationColumn(
    state: MainWindowState,
    openInBrowser: Consumer<String>,
    onShowOnMap: () -> Unit,
    onShowPathInAirScout: () -> Unit,
    onStationSelected: (ChatMember) -> Unit,
    onFiltersChanged: () -> Unit,
    onShowMorePriorities: () -> Unit,
) {
    val stationContextMenu = remember(state) {
        StationContextMenuState(state::selectStation)
    }

    SplitterPane(
        state = state.rightSplitter,
        orientation = SplitterOrientation.VERTICAL,
        modifier = Modifier.fillMaxSize(),
        panes = listOf(
            {
                Column(Modifier.fillMaxSize()) {
                    StationFilterBar(
                        filters = state.stationFilter,
                        activeBands = state.activeBands,
                        reachability = state.reachability,
                        onChanged = {
                            onFiltersChanged()
                        },
                        onReset = state::resetStationFilters,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HorizontalDivider(thickness = Density.HAIRLINE)
                    DataTableView(
                        state = state.stations,
                        modifier = Modifier.fillMaxSize(),
                        emphasizeCell = { member, column ->
                            column.id == "callsign" && state.newJoined.isRecent(member)
                        },
                        onRowClick = onStationSelected,
                        onRowContextMenu = { member, position ->
                            stationContextMenu.open(
                                listOf(member),
                                StationSelectionSource.STATION_CONTEXT_MENU,
                                position,
                            )
                        },
                        selectOnRowClick = false,
                    )
                }
            },
            {
                TopPriorityBar(
                    state = state.topPriority,
                    newJoined = state.newJoined,
                    onShowMore = onShowMorePriorities,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            {
                SelectedStationPanel(
                    state = state.selectedStation,
                    messages = state.selectedStationMessages,
                    activeBands = state.activeBands,
                    contentSplitter = state.selectedStationSplitter,
                    openInBrowser = openInBrowser,
                    onShowOnMap = onShowOnMap,
                    onShowPathInAirScout = onShowPathInAirScout,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        ),
    )
    StationContextOverlays(stationContextMenu, state, onShowOnMap)
}
