package kst4contest.view.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kst4contest.controller.On4KstConnectionState
import kst4contest.utils.PlatformUtils
import kst4contest.view.i18n.LocalStrings

/**
 * What the menu bar does when something is picked.
 *
 * An interface rather than a dozen lambdas: every window is meant to carry this same bar, and a
 * parameter list that long is misread sooner or later.
 */
interface MainMenuActions {
    fun connect()
    fun disconnect()
    fun switchOperatorProfile()
    fun exitApplication()
    fun setQrgAsNameInChat()
    fun toggleAwayState()
    fun toggleSettingsWindow()

    /** Ends the session after asking and runs the setup wizard for the active profile. */
    fun openSetupWizard()
    fun toggleMonitorWindow()
    fun toggleStationMap()
    fun useDarkDesign()
    fun useDefaultDesign()
    fun openDonationPage()
    fun openHomepage()
    fun openNewsgroup()
    /** The JavaFX item and its handler are both commented out; kept so the bar can say so. */
    fun openChangelog()

    /** OV3T runs the aeroplane feed the AirScout predictions come from. */
    fun openOv3tDonationPage()
    fun contactAuthor()
    fun showAbout()
}

/**
 * The menu bar in the system's own bar — macOS only.
 *
 * Compose's `MenuBar` is a `javax.swing.JMenuBar` set on the window, so Swing draws it and
 * no Compose theme reaches it. On macOS that is what one wants: the bar belongs at the top
 * of the screen, drawn by the system. Everywhere else it meant an unthemed Metal menu
 * sitting above a themed window, which is why [Kst4ContestMenuRow] exists.
 *
 * Both renderers read [mainMenuModel]. Declaring the menus twice is how one of them quietly
 * loses an item the other gained.
 */
@Composable
fun FrameWindowScope.Kst4ContestMenuBar(
    state: MainMenuState,
    actions: MainMenuActions,
    settingsWindowOpen: Boolean,
    monitorWindowOpen: Boolean,
    /**
     * The connection state, for the read-only macOS menu. On macOS the status bar hides its
     * badge because the indicator lives in the system menu bar, so without this menu a
     * macOS operator would have no connection indicator at all.
     */
    connectionState: On4KstConnectionState? = null,
    connectionDetail: String? = null,
) {
    val strings = LocalStrings.current

    MenuBar {
        mainMenuModel(state, actions, settingsWindowOpen, monitorWindowOpen, strings)
            .forEach { menu ->
            Menu(menu.title) {
                menu.entries.forEach { entry ->
                    when (entry) {
                        is MenuEntry.Item ->
                            Item(entry.label, enabled = entry.enabled, onClick = entry.onClick)
                        MenuEntry.Separator -> Separator()
                    }
                }
            }
        }

        /*
         * The connection indicator, for macOS only: the system menu bar is where it lives
         * there, because the status bar's badge is hidden on that platform. Read-only — the
         * single item is disabled, exactly as menuItemConnectionStateDetailMacOs was.
         */
        if (PlatformUtils.isMacOs()) {
            Menu(ConnectionIndicator.macOsMenuTitle(connectionState)) {
                Item(
                    ConnectionIndicator.detailFor(connectionState, connectionDetail),
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}

/**
 * The menu bar drawn inside the window, by Compose.
 *
 * Used everywhere except macOS. It exists because Compose's `MenuBar` hands the job to
 * Swing, which knows nothing of the stylesheet palette: under Linux that produced the
 * default cross-platform Metal menu above a themed window. This row reads the same palette
 * as every other control, so a design switch reaches it.
 *
 * A menu title is shaped like a tab — flat, highlighted while active — so it takes the tab
 * measurements rather than the button ones, and it highlights with the accent colour, which
 * is what a JavaFX menu did.
 *
 * @param rowState which menu is down. Owned by the caller because `MainWindow` has to ask
 *        [MainMenuRowState.anyOpen] before letting the window-wide keys run.
 */
@Composable
fun Kst4ContestMenuRow(
    state: MainMenuState,
    actions: MainMenuActions,
    settingsWindowOpen: Boolean,
    monitorWindowOpen: Boolean,
    rowState: MainMenuRowState,
    modifier: Modifier = Modifier,
) {
    val palette = LocalJavaFxPalette.current
    val strings = LocalStrings.current
    val menus = mainMenuModel(state, actions, settingsWindowOpen, monitorWindowOpen, strings)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.base)
            .heightIn(min = Density.TAB_MIN_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        menus.forEach { menu ->
            MenuTitle(menu = menu, rowState = rowState)
        }
    }
}

@Composable
private fun MenuTitle(menu: MenuSpec, rowState: MainMenuRowState) {
    val palette = LocalJavaFxPalette.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val open = rowState.openMenu == menu.title

    /*
     * Full bar height, so the popup anchored to this box starts at the bar's bottom edge
     * rather than at the centred title text a few pixels above it.
     */
    Box(
        modifier = Modifier.heightIn(min = Density.TAB_MIN_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = menu.title,
            style = MaterialTheme.typography.bodyMedium,
            /*
             * contrastingText and not textAccent: the accent is the background right behind
             * this title, and once an operator can set the accent the two can be the same
             * colour -- an invisible menu title. Black or white by brightness is readable by
             * construction.
             */
            color = if (open) contrastingText(palette.accent) else palette.labelTextFill,
            modifier = Modifier
                .background(
                    when {
                        open -> palette.accent
                        /*
                         * Hover is a hint, not a selection: the accent at full strength on
                         * mere pointer travel makes the bar flicker as the operator crosses
                         * it on the way to the chat field.
                         */
                        hovered -> palette.accent.copy(alpha = HOVER_ALPHA)
                        else -> Color.Transparent
                    }
                )
                .hoverable(interactions)
                .clickable { rowState.toggle(menu.title) }
                .padding(
                    horizontal = Density.TAB_HORIZONTAL_PADDING,
                    vertical = MENU_TITLE_VERTICAL_PADDING,
                ),
        )

        /*
         * A hand-rolled stand-in for Material3's DropdownMenu, because that one cannot open
         * flush under a menu bar — see MenuRowPopupPositionProvider. Same dismissal (outside
         * click and Escape come with a focusable popup), same MenuDefaults look; only the
         * expand animation is gone, which a menu bar does not have anyway.
         */
        if (open) {
            Popup(
                popupPositionProvider = MenuRowPopupPositionProvider,
                onDismissRequest = rowState::close,
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = MenuDefaults.shape,
                    color = MenuDefaults.containerColor,
                    tonalElevation = MenuDefaults.TonalElevation,
                    shadowElevation = MenuDefaults.ShadowElevation,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = MENU_POPUP_VERTICAL_PADDING)
                            .width(IntrinsicSize.Max)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        menu.entries.forEach { entry ->
                            when (entry) {
                                is MenuEntry.Item -> DropdownMenuItem(
                                    text = {
                                        Text(entry.label, style = MaterialTheme.typography.bodyMedium)
                                    },
                                    enabled = entry.enabled,
                                    onClick = { rowState.pick(entry.onClick) },
                                )

                                MenuEntry.Separator ->
                                    HorizontalDivider(
                                        thickness = Density.HAIRLINE,
                                        color = palette.separatorLine,
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Places a menu-bar menu: flush under its anchor, leading edges aligned.
 *
 * Material3's DropdownMenu cannot be told to do this. Its position provider refuses any
 * position closer than 48 dp (`MenuVerticalMargin`) to the window's top edge, and a menu
 * bar sits exactly there, so every menu opened pushed down to that margin — a visible gap
 * below the bar. The margin is applied after the `offset` parameter and the provider is
 * not replaceable, hence this one and the hand-rolled popup in [MenuTitle].
 */
internal object MenuRowPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val preferredX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.left
            LayoutDirection.Rtl -> anchorBounds.right - popupContentSize.width
        }
        val x = preferredX.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))
        val below = anchorBounds.bottom
        val above = anchorBounds.top - popupContentSize.height
        val y = when {
            below + popupContentSize.height <= windowSize.height -> below
            above >= 0 -> above
            else -> maxOf(0, windowSize.height - popupContentSize.height)
        }
        return IntOffset(x, y)
    }
}

/** Enough tint to show the pointer is over a title, not enough to read as a selection. */
private const val HOVER_ALPHA = 0.35f

private val MENU_TITLE_VERTICAL_PADDING = androidx.compose.ui.unit.Dp(4f)

/** Material3's DropdownMenuVerticalPadding, which is internal; keep the menus looking alike. */
private val MENU_POPUP_VERTICAL_PADDING = 8.dp
