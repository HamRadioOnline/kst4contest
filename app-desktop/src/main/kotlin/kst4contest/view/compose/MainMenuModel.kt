package kst4contest.view.compose

import kst4contest.view.i18n.Strings
/**
 * One line of a menu.
 *
 * Toolkit-free on purpose: the bar is drawn two different ways — Compose's `MenuBar`,
 * which is a `javax.swing.JMenuBar` under the covers and only used on macOS, and the
 * in-window Compose row everywhere else. Declaring the menus twice is how one renderer
 * quietly loses an item that the other gained, so they both read this instead.
 */
sealed interface MenuEntry {

    /**
     * @param enabled whether it can be pressed; a menu item that merely looks usable is
     *        worse than a disabled one, because the operator presses it mid-contest and
     *        either nothing happens or the wrong thing does
     */
    data class Item(
        val label: String,
        val enabled: Boolean,
        val onClick: () -> Unit,
    ) : MenuEntry

    data object Separator : MenuEntry
}

/** One menu of the bar: its title and what drops down from it. */
data class MenuSpec(val title: String, val entries: List<MenuEntry>)

/**
 * The whole bar, as data.
 *
 * A plain function of the state rather than a composable, which is what finally makes the
 * structure, the dynamic labels and the enabled states testable: while this lived inside
 * Compose's `MenuBar` it took a running window to look at, and none of it was covered.
 *
 * @param settingsWindowOpen asked for at build time, not held in [MainMenuState]: a Compose
 *        window can be closed from its own frame without telling the menu
 * @param monitorWindowOpen the same for the cluster / stranger-QSO window
 */
fun mainMenuModel(
    state: MainMenuState,
    actions: MainMenuActions,
    settingsWindowOpen: Boolean,
    monitorWindowOpen: Boolean,
    /*
     * Passed in rather than read from the composition, because this model is deliberately not
     * a composable -- both renderers build from it and the two menu tests assert over it
     * without a composition in sight.
     */
    strings: Strings,
): List<MenuSpec> = listOf(
    MenuSpec(
        strings.menuFile,
        listOf(
            MenuEntry.Item(state.connectLabel, state.canConnect, actions::connect),
            MenuEntry.Item(strings.menuDisconnect, state.canDisconnect, actions::disconnect),
            MenuEntry.Item(
                strings.menuSwitchOperatorProfile,
                state.canSwitchProfile,
                actions::switchOperatorProfile,
            ),
            MenuEntry.Item(strings.menuExitAndDisconnect, state.canExit, actions::exitApplication),
        ),
    ),
    MenuSpec(
        strings.menuOptions,
        listOf(
            MenuEntry.Item(
                strings.menuSetQrgAsNameInChat,
                state.canUseChatActions,
                actions::setQrgAsNameInChat,
            ),
            /*
             * This label is the only place the AFK state is visible anywhere in the
             * application, so it follows the state rather than being flipped inside the
             * click handler as JavaFX did.
             */
            MenuEntry.Item(
                state.awayMenuLabel(strings),
                state.canUseChatActions,
                actions::toggleAwayState,
            ),
            MenuEntry.Item(
                if (settingsWindowOpen) strings.menuHideOptions else strings.menuShowOptions,
                enabled = true,
                actions::toggleSettingsWindow,
            ),
            /*
             * Always usable, like the profile switch: it rebuilds the runtime and asks before
             * it gives the session up.
             */
            MenuEntry.Item(strings.menuSetupWizard, state.canSwitchProfile, actions::openSetupWizard),
        ),
    ),
    MenuSpec(
        strings.menuWindows,
        listOf(
            MenuEntry.Item(
                if (monitorWindowOpen) strings.menuHideMonitor else strings.menuShowMonitor,
                enabled = true,
                actions::toggleMonitorWindow,
            ),
            MenuEntry.Item(
                if (settingsWindowOpen) strings.menuHideOptions else strings.menuShowOptions,
                enabled = true,
                actions::toggleSettingsWindow,
            ),
            MenuEntry.Separator,
            MenuEntry.Item(strings.menuToggleStationMap, enabled = true, actions::toggleStationMap),
            MenuEntry.Separator,
            MenuEntry.Item(strings.menuUseDarkDesign, enabled = true, actions::useDarkDesign),
            MenuEntry.Item(strings.menuUseDefaultDesign, enabled = true, actions::useDefaultDesign),
        ),
    ),
    MenuSpec(
        strings.menuInfo,
        listOf(
            MenuEntry.Item(
                strings.menuDonatePayPal,
                enabled = true,
                actions::openDonationPage,
            ),
            MenuEntry.Item(
                strings.menuDonateOv3t,
                enabled = true,
                actions::openOv3tDonationPage,
            ),
            MenuEntry.Item(strings.menuHomepage, enabled = true, actions::openHomepage),
            MenuEntry.Item(strings.menuNewsgroup, enabled = true, actions::openNewsgroup),
            MenuEntry.Item(
                strings.menuContactAuthor,
                enabled = true,
                actions::contactAuthor,
            ),
            MenuEntry.Separator,
            MenuEntry.Item(strings.menuAbout, enabled = true, actions::showAbout),
        ),
    ),
)
