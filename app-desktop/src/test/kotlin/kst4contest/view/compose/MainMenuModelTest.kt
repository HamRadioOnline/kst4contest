package kst4contest.view.compose

import kst4contest.view.i18n.Strings
import kst4contest.view.i18n.Translations
import kst4contest.controller.On4KstConnectionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The menus, their labels and which of them can be used.
 *
 * None of this was covered while the bar was declared straight into Compose's `MenuBar`:
 * the structure only existed inside a composable, so it took a running window to look at.
 * It is now a plain function of the state, and these are the parts that go wrong silently —
 * a label that stops following the away state, an item that stays usable while the station
 * is not in the chat, or a menu that loses an entry when a second renderer is added.
 */
class MainMenuModelTest {

    private class RecordingActions : MainMenuActions {
        val calls = mutableListOf<String>()
        override fun connect() { calls += "connect" }
        override fun disconnect() { calls += "disconnect" }
        override fun switchOperatorProfile() { calls += "switchOperatorProfile" }
        override fun exitApplication() { calls += "exitApplication" }
        override fun setQrgAsNameInChat() { calls += "setQrgAsNameInChat" }
        override fun toggleAwayState() { calls += "toggleAwayState" }
        override fun toggleSettingsWindow() { calls += "toggleSettingsWindow" }
        override fun openSetupWizard() { calls += "openSetupWizard" }
        override fun toggleMonitorWindow() { calls += "toggleMonitorWindow" }
        override fun toggleStationMap() { calls += "toggleStationMap" }
        override fun useDarkDesign() { calls += "useDarkDesign" }
        override fun useDefaultDesign() { calls += "useDefaultDesign" }
        override fun openDonationPage() { calls += "openDonationPage" }
        override fun openHomepage() { calls += "openHomepage" }
        override fun openNewsgroup() { calls += "openNewsgroup" }
        override fun openChangelog() { calls += "openChangelog" }
        override fun openOv3tDonationPage() { calls += "openOv3tDonationPage" }
        override fun contactAuthor() { calls += "contactAuthor" }
        override fun showAbout() { calls += "showAbout" }
    }

    private fun model(
        connectionState: On4KstConnectionState = On4KstConnectionState.DISCONNECTED,
        away: Boolean = false,
        settingsOpen: Boolean = false,
        monitorOpen: Boolean = false,
        actions: MainMenuActions = RecordingActions(),
    ): List<MenuSpec> {
        val state = MainMenuState()
        state.connectionState = connectionState
        state.awayFromChat = away
        return mainMenuModel(state, actions, settingsOpen, monitorOpen, baseStrings())
    }

    /*
     * The base language's texts, built here rather than taken from a store: these tests
     * assert over real titles ("File", "Options"), and a store would make them depend on
     * whichever language ran last in the shared JVM.
     */
    private fun baseStrings(): Strings {
        val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE)
        return Strings { key -> base[key] ?: key }
    }

    private fun items(menu: MenuSpec) = menu.entries.filterIsInstance<MenuEntry.Item>()

    private fun labels(menu: MenuSpec) = items(menu).map { it.label }

    private fun item(model: List<MenuSpec>, label: String): MenuEntry.Item =
        model.flatMap { items(it) }.single { it.label == label }

    @Test
    fun theFourMenusAppearInTheOrderTheyAlwaysHad() {
        assertEquals(listOf("File", "Options", "Windows", "Info"), model().map { it.title })
    }

    @Test
    fun noMenuIsEmptyAndTheSeparatorsSurviveTheTranslation() {
        val model = model()

        model.forEach { menu ->
            assertTrue(items(menu).isNotEmpty(), "menu '${menu.title}' has no items")
        }

        // Windows has two separators, Info one. A renderer that drops them makes the
        // design switch read as just another window toggle.
        assertEquals(2, model.single { it.title == "Windows" }.entries.count { it is MenuEntry.Separator })
        assertEquals(1, model.single { it.title == "Info" }.entries.count { it is MenuEntry.Separator })
    }

    @Test
    fun theConnectItemCarriesTheStatesOwnLabel() {
        val state = MainMenuState()
        state.connectLabel = "Connect to 144/432 MHz"

        val model = mainMenuModel(state, RecordingActions(), false, false, baseStrings())

        assertEquals(
            "Connect to 144/432 MHz",
            labels(model.single { it.title == "File" }).first(),
            "the label names the chat the station lands in; it must not be hardcoded here",
        )
    }

    @Test
    fun connectAndDisconnectAreNeverBothUsable() {
        for (connectionState in On4KstConnectionState.values()) {
            val model = model(connectionState = connectionState)
            val connect = model.flatMap { items(it) }.first()
            val disconnect = item(model, "Disconnect")

            assertFalse(
                connect.enabled && disconnect.enabled,
                "both were usable while $connectionState",
            )
            assertTrue(
                connect.enabled || disconnect.enabled,
                "neither was usable while $connectionState, which would strand the operator",
            )
        }
    }

    @Test
    fun theChatActionsNeedASessionThatIsReallyOnline() {
        val offline = model(connectionState = On4KstConnectionState.CONNECTING)

        // Logging in is not being in the chat: a command sent then goes nowhere.
        assertFalse(item(offline, "Set QRG as name in Chat (main category)").enabled)
        assertFalse(item(offline, "Show me as AWAY FROM chat!").enabled)
    }

    @Test
    fun theAwayLabelNamesTheNextActionAndFollowsTheState() {
        assertEquals(
            "Show me as AWAY FROM chat!",
            item(model(away = false), "Show me as AWAY FROM chat!").label,
        )
        assertEquals(
            "Show me as ACTIVE in chat!",
            item(model(away = true), "Show me as ACTIVE in chat!").label,
        )
    }

    @Test
    fun theWindowTogglesSayWhatPressingThemWillDo() {
        /*
         * "Show options" with a capital S in both menus. The JavaFX wording had it capital in
         * the Options menu and lower case in the Windows menu for the same action; pulling
         * both onto one key settled that, and in German the two would have been identical
         * anyway.
         */
        val closed = model(settingsOpen = false, monitorOpen = false)
        assertTrue(labels(closed.single { it.title == "Windows" }).any { it == "Show options" })
        assertTrue(
            labels(closed.single { it.title == "Windows" })
                .any { it == "Show cluster / stranger QSOs" },
        )

        val open = model(settingsOpen = true, monitorOpen = true)
        assertTrue(labels(open.single { it.title == "Windows" }).any { it == "hide options" })
        assertTrue(
            labels(open.single { it.title == "Windows" })
                .any { it == "Hide cluster / stranger QSOs" },
        )
    }

    @Test
    fun everyItemIsWiredToSomething() {
        val actions = RecordingActions()
        val model = model(connectionState = On4KstConnectionState.ONLINE, actions = actions)

        model.flatMap { items(it) }.forEach { it.onClick() }

        // One click each, and nothing silently wired to an empty lambda: the JavaFX bar had
        // an item whose handler was commented out, and it looked exactly like the others.
        assertEquals(
            model.flatMap { items(it) }.size,
            actions.calls.size,
            "every item must reach an action; one of them did nothing",
        )
    }

    @Test
    fun theSetupAssistantIsReachableFromOptionsWhateverTheConnection() {
        for (connectionState in On4KstConnectionState.values()) {
            val actions = RecordingActions()
            val model = model(connectionState = connectionState, actions = actions)
            val entry = items(model.single { it.title == "Options" }).single { it.label == "Setup assistant..." }

            // Like the profile switch: it gives the session up itself, after asking.
            assertTrue(entry.enabled, "the assistant must be reachable while $connectionState")

            entry.onClick()
            assertEquals("openSetupWizard", actions.calls.last())
        }
    }
}
