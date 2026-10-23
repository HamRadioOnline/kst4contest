package kst4contest.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kst4contest.controller.ChatController
import kst4contest.controller.ActiveOperatorProfile
import kst4contest.controller.OperatorProfileManagementService
import kst4contest.controller.On4KstConnectionState
import kst4contest.controller.OperatorProfilePaths
import kst4contest.model.ChatMember
import kst4contest.model.OperatorProfile
import kst4contest.view.compose.OperatorProfilePaletteFiles
import kst4contest.view.compose.tabs.AirscoutTab
import kst4contest.view.compose.tabs.AirscoutTabState
import kst4contest.view.compose.tabs.BeaconTab
import kst4contest.view.compose.tabs.BeaconTabState
import kst4contest.view.compose.tabs.ColoursTab
import kst4contest.view.compose.tabs.ColoursTabState
import kst4contest.view.compose.tabs.GuiOptionsTab
import kst4contest.view.compose.tabs.GuiOptionsTabState
import kst4contest.view.compose.tabs.LogSynchTab
import kst4contest.view.compose.tabs.LogSynchTabState
import kst4contest.view.compose.tabs.MessageHandlingTab
import kst4contest.view.compose.tabs.MessageHandlingTabState
import kst4contest.view.compose.tabs.NotificationTab
import kst4contest.view.compose.tabs.NotificationTabState
import kst4contest.view.compose.tabs.ProfilesTab
import kst4contest.view.compose.tabs.ProfilesTabState
import kst4contest.view.compose.tabs.ShortcutsTab
import kst4contest.view.compose.tabs.ShortcutsTabState
import kst4contest.view.compose.tabs.StationTab
import kst4contest.view.compose.tabs.StationTabState
import kst4contest.view.compose.tabs.TrxSynchTab
import kst4contest.view.compose.tabs.TrxSynchTabState
import kst4contest.view.compose.tabs.WorkedDatabaseTab
import kst4contest.view.compose.tabs.WorkedDatabaseTabState
import kst4contest.view.compose.tabs.repairStoredSettings

/**
 * The work the settings window needs from the main window.
 *
 * These are the callbacks whose implementation lives in the JavaFX window and cannot
 * be reached from a ChatController: rebuilding controls, binding the own-QRG
 * follower and refreshing a table. Everything else the tabs need comes from the
 * controller, which is why it is not listed here.
 */
interface SettingsHost {

    /**
     * Rebuilds the button row above the message field from the shortcut list.
     *
     * The suffix keeps this distinct from the window's own private method of the same
     * purpose, which touches JavaFX directly; the implementation of this one has to hop
     * onto the JavaFX thread first, because the settings window runs on its own.
     */
    fun refreshShortcutButtonsFromSettings()

    /**
     * Pushes the stored SO2R switch and the rotator 2 switch into the main window's Compose
     * state, so the QTF2 field and the "Turn ant2" button follow an Apply. The
     * implementation has to hop onto the UI thread; the settings window runs on its own.
     */
    fun refreshSo2rState()

    /** Rebuilds the Ctrl+1..Ctrl+0 context menus from the snippet list. */
    fun refreshTextSnippetContextMenusFromSettings()

    /** The detected subnet broadcast address for Win-Test, or null. */
    fun detectWintestBroadcastAddress(): String?

    /** Attaches or detaches the follower that mirrors the own QRG into the station field. */
    fun applyOwnQrgFollower(enabled: Boolean)

    /**
     * Turns file recording of the whole session on or off, with the history recorder.
     *
     * Lives here and not on the controller because enabling it opens the error-log file
     * handler, which the application owns. The order — log before recorder when enabling,
     * recorder before log when disabling — is the one the JavaFX checkbox kept.
     */
    fun applyDebugModeToFile(enabled: Boolean)

    /** Re-reads the worked-stations table for display. */
    fun refreshWorkedStationsView()

    /**
     * Switches the application to another operator profile. The tab must not do this
     * itself: the switch reloads preferences and the worked database, which only the
     * application can sequence.
     */
    fun switchToOperatorProfile(profile: OperatorProfile)

    /**
     * Starts the ON4KST session — the work of the Connect button.
     *
     * @return null when the session was started, otherwise the reason it was not
     */
    fun connectFromSettings(): String?

    /** Drops the session and keeps the client running. */
    fun disconnectOnlyFromSettings()

    /** Drops the session and closes the client. */
    fun disconnectAndCloseChatFromSettings()

    /**
     * Persists the preferences.
     *
     * @return the file the settings went to, or null when it could not be written
     */
    fun savePreferencesFromSettings(): String?
}

/**
 * Builds the settings tabs in the order the JavaFX TabPane had them.
 *
 * The wiring lives here rather than in the Java window on purpose: every call that
 * reaches past ChatPreferences into the running session is made in one readable
 * place, and the foreign-write coverage test can see it. A tab that only wrote a
 * preference and skipped its session call would look correct in the file and do
 * nothing until the next restart.
 *
 * @param connectionState the session state as Compose state reads it; the Station tab
 *        locks the login fields from it. The controller's own getter is not observable,
 *        so a tab reading it would stay on whatever the link was when the window opened.
 * @param draft the edits of this opening of the window; every tab state stages into it,
 *        and the button bar applies it
 */
fun buildSettingsTabs(
    controller: ChatController,
    host: SettingsHost,
    notices: SettingsNotices,
    paletteStore: PaletteStore,
    languageStore: kst4contest.view.i18n.LanguageStore,
    connectionState: () -> On4KstConnectionState,
    draft: SettingsDraft,
): List<SettingsTab> {

    val prefs = controller.chatPreferences

    // Once, before any tab reads: see repairStoredSettings for why not inside the tabs.
    repairStoredSettings(prefs, host::detectWintestBroadcastAddress)

    val stationState = StationTabState(
        prefs,
        draft,
        applyMainCategory = { category -> controller.setChatCategoryMain(category) },
        applySecondCategory = { category -> controller.setChatCategorySecondChat(category) },
        connectionState = connectionState,
        applySecondRotator = { controller.reconfigureSecondRotator(); host.refreshSo2rState() },
    )

    val notificationState = NotificationTabState(
        prefs,
        draft,
        controller.lstNotify_QSOSniffer_sniffedCallSignList,
        applyDxClusterServer = {
            /*
             * Starting a server that nothing can reach yet is pointless, so the JavaFX
             * checkbox only touched the server while the session was logged in; the
             * flag alone decides what happens at the next login. Restarted rather than
             * started when enabled: the same call then binds a changed port, and a server
             * that was not running is simply started.
             */
            if (controller.isConnectedAndLoggedIn) {
                if (prefs.isNotify_dxClusterServerEnabled()) {
                    controller.restartDxClusterServerIfEnabled()
                } else {
                    controller.stopDxClusterServer()
                }
            }
        },
        broadcastTestSpot = { spot -> deliverTestSpot(controller, spot) },
    )

    val shortcutsState = ShortcutsTabState(
        prefs,
        draft,
        refreshShortcutButtons = host::refreshShortcutButtonsFromSettings,
        refreshTextSnippetContextMenus = host::refreshTextSnippetContextMenusFromSettings,
    )

    val beaconState = BeaconTabState(
        prefs,
        draft,
        validateBeaconTemplate = { template -> controller.validateBeaconTemplate(template) },
        restartBeaconTimer = { controller.restartBeaconTimer() },
        mainCategoryName = categoryName(controller.chatCategoryMain, kst4contest.view.i18n.CurrentStrings.get().settingsCategoryMainFallback),
        secondCategoryName = categoryName(controller.chatCategorySecondChat, kst4contest.view.i18n.CurrentStrings.get().settingsCategorySecondFallback),
    )

    val logSynchState = LogSynchTabState(
        prefs,
        draft,
        restartWintestListener = { controller.restartWintestUdpListenerIfEnabled() },
        stopWintestListener = { controller.stopWintestUdpListener() },
    )

    val workedDatabaseState = WorkedDatabaseTabState(
        resetInDatabase = { controller.dbHandler.resetWorkedDataInDB() },
        refreshGuiLists = {
            controller.resetWorkedAndQrvInfoInGuiLists()
            controller.refreshWorkedStateAndDatabaseListFromDatabase()
        },
        refreshView = host::refreshWorkedStationsView,
        workedRoster = controller.lst_DBBasedWkdCallSignList,
    )

    val profilesState = ProfilesTabState(
        OperatorProfileManagementService(),
        activeSelection = { ActiveOperatorProfile.get() },
        requestActivation = host::switchToOperatorProfile,
    )

    val reportRefusal: (String) -> Unit = notices::problem

    /*
     * The profile whose stylesheet the colours tab offers to write. Resolved here rather than
     * captured, because the settings window outlives nothing but is built once per profile.
     */
    fun activeProfileOrRoot(): OperatorProfile =
        ActiveOperatorProfile.get()?.profile
            ?: OperatorProfilePaths.buildRootProfile(prefs.stn_loginCallSign ?: "")

    /*
     * Order and titles are those of the JavaFX TabPane, character for character. The
     * comment there is explicit that operators navigate these tabs by muscle memory,
     * so a tidier order or a shorter title would be a regression, not an improvement.
     */
    return listOf(
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabStation }) { StationTab(stationState, reportRefusal) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabLogSynch }) { LogSynchTab(logSynchState) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabTrxSynch }) { TrxSynchTab(TrxSynchTabState(prefs, draft, applySecondRotator = { controller.reconfigureSecondRotator(); host.refreshSo2rState() }, applyOwnQrgFollower = host::applyOwnQrgFollower)) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabAirscout }) { AirscoutTab(AirscoutTabState(prefs, draft)) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabNotification }) { NotificationTab(notificationState, reportRefusal) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabShortcuts }) { ShortcutsTab(shortcutsState, reportRefusal) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabBeacon }) { BeaconTab(beaconState, reportRefusal) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabMessageHandling }) { MessageHandlingTab(MessageHandlingTabState(prefs, draft, host::applyDebugModeToFile)) },
        /*
         * Selecting this tab re-read the database in the JavaFX window; the list is a
         * snapshot and would otherwise show what was true when the window opened.
         */
        SettingsTab(
            { kst4contest.view.i18n.CurrentStrings.get().tabWorkedDatabase },
            onSelected = { controller.refreshWorkedStateAndDatabaseListFromDatabase() },
        ) { WorkedDatabaseTab(workedDatabaseState) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabGui }) { GuiOptionsTab(GuiOptionsTabState(prefs, draft, languageStore)) },
        /*
         * Appended last on purpose so no established tab position shifts, the reason
         * the JavaFX tab gave for the same placement.
         */
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabProfiles }, onSelected = profilesState::refresh) { ProfilesTab(profilesState) },
        /*
         * Appended after Profiles for the same reason Profiles was appended after GUI. The
         * tab edits whichever design is in force, which is why it reads the flag through a
         * lambda rather than taking its value here: the operator can switch day/evening from
         * the main window's menu while this tab is open.
         */
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabColours }) {
            /*
             * Remembered rather than rebuilt: the tab recomposes on every colour change, and
             * the design flag is read through a lambda precisely so the object itself does not
             * have to be rebuilt to follow a day/evening switch.
             */
            val coloursState = remember(paletteStore) {
                ColoursTabState(
                    paletteStore,
                    { prefs.isGUI_darkModeActive },
                    OperatorProfilePaletteFiles.of(activeProfileOrRoot()),
                )
            }
            ColoursTab(coloursState, reportRefusal, notices::info)
        },
    )
}

/**
 * Delivers the test spot and reports why it could not be delivered, in the order the
 * JavaFX button checked: session first, then a connected logger, then the delivery
 * itself. The messages are the ones the operator knows.
 */
private fun deliverTestSpot(controller: ChatController, spot: ChatMember): String? {
    if (!controller.isConnectedAndLoggedIn || !controller.chatPreferences.isNotify_dxClusterServerEnabled()) {
        return kst4contest.view.i18n.CurrentStrings.get().settingsSpotNeedsConnection
    }

    val server = controller.dxClusterServer
        ?: return kst4contest.view.i18n.CurrentStrings.get().settingsSpotNeedsConnection

    if (!server.hasConnectedClients()) {
        return kst4contest.view.i18n.CurrentStrings.get().settingsSpotNoClient
    }

    if (!server.broadcastSingleDXClusterEntryToLoggers(spot)) {
        return kst4contest.view.i18n.CurrentStrings.get().settingsSpotNotDelivered
    }

    return null
}

/** The label a chat category carries in the beacon tab, or a fallback when unset. */
private fun categoryName(category: kst4contest.model.ChatCategory?, fallback: String): String =
    category?.getChatCategoryName(category.categoryNumber) ?: fallback

/**
 * The bottom button row of the settings window.
 *
 * Built here because it needs the controller for its state and the host for its
 * actions, and because it belongs with the tabs it sits under. The `close` action comes
 * from the window itself: only the window can end its own Compose application.
 *
 * Three buttons apply the [draft] before they act. "Apply/Close prefs" applies and closes,
 * "Save settings" applies and writes the file, and Connect applies before it connects: the
 * session would otherwise use the stored server or second-chat switch rather than the one
 * the tab shows. A field that refuses what it holds stops the button before anything is
 * applied, so the operator sees why instead of a closed window or a saved file; the X still
 * offers Discard. A write or effect that fails is reported in a problem notice.
 */
fun settingsButtons(
    controller: ChatController,
    host: SettingsHost,
    notices: SettingsNotices,
    draft: SettingsDraft,
    connectionState: () -> On4KstConnectionState,
): @Composable (close: () -> Unit) -> Unit {
    val barState = ConnectionBarState(controller.chatPreferences, connectionState)

    return { close ->
        ConnectionBar(
            state = barState,
            onConnect = {
                if (applyChecked(draft, notices)) {
                    val refusal = host.connectFromSettings()
                    if (refusal != null) notices.problem(refusal)
                }
            },
            onSave = { applyAndSave(draft, notices, host::savePreferencesFromSettings) },
            onApplyAndClose = { if (applyChecked(draft, notices)) close() },
            onDisconnectAndCloseChat = host::disconnectAndCloseChatFromSettings,
            onDisconnectOnly = {
                host.disconnectOnlyFromSettings()
            },
        )
    }
}

/**
 * Commits what the fields still hold and tells whether all of it was accepted. On a refusal
 * the reason is the notice on screen and nothing has been applied yet. The operator corrects
 * the field, or discards everything through the window's X, whose question offers Discard
 * even while a field refuses (see SettingsQuestion).
 *
 * A problem notice already standing counts as a refusal too. The click on a button can
 * itself take the focus from a field, which then commits -- and is refused, reporting it --
 * before the button's action runs. A problem notice from earlier cannot be standing here:
 * its dialog covers the window, so nothing in it could have been clicked.
 */
internal fun typedEntriesAccepted(draft: SettingsDraft, notices: SettingsNotices): Boolean {
    val refusals = draft.commitTypedEntries()
    if (refusals.isNotEmpty()) {
        notices.problem(refusals.joinToString("\n"))
        return false
    }
    return notices.current !is SettingsNotice.Problem
}

/**
 * Checks the typed entries, then applies [draft], and tells whether the caller may go on:
 * false on a refusal (nothing applied) or when a write or an effect failed (the rest
 * applied, the failures named in a problem notice).
 */
internal fun applyChecked(draft: SettingsDraft, notices: SettingsNotices): Boolean {
    if (!typedEntriesAccepted(draft, notices)) return false

    val outcome = draft.apply()
    if (!outcome.isClean) {
        notices.problem(applyFailedText(outcome))
        return false
    }
    return true
}

/**
 * The work of "Save settings": check the typed entries, apply the draft, write the file, say
 * where it went. The window stays open.
 *
 * A failed effect does not stop the file: its setting was stored, and the file should hold
 * it. A failed write does, because the file would then miss what the window shows.
 *
 * @param save writes the preferences and returns the file, or null when it could not
 */
internal fun applyAndSave(draft: SettingsDraft, notices: SettingsNotices, save: () -> String?) {
    if (!typedEntriesAccepted(draft, notices)) return

    val outcome = draft.apply()
    if (outcome.failedWrites.isNotEmpty()) {
        notices.problem(applyFailedText(outcome))
        return
    }

    val strings = kst4contest.view.i18n.CurrentStrings.get()
    val target = save()
    when {
        target == null -> notices.problem(
            listOfNotNull(
                outcome.takeIf { !it.isClean }?.let(::applyFailedText),
                strings.settingsWriteFailed,
            ).joinToString("\n\n")
        )
        !outcome.isClean -> notices.problem(applyFailedText(outcome) + "\n\n" + strings.settingsWrittenTo(target))
        else -> notices.info(strings.settingsWrittenTo(target))
    }
}

private fun applyFailedText(outcome: ApplyOutcome): String =
    kst4contest.view.i18n.CurrentStrings.get().settingsApplyFailed(outcome.failures.joinToString("; "))
