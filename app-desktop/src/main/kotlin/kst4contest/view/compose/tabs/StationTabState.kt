package kst4contest.view.compose.tabs

import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.view.compose.Form
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting
import kst4contest.view.i18n.CurrentStrings

/**
 * The Station tab as a typed facade over ChatPreferences.
 *
 * Edits are staged in the window's [SettingsDraft] and take effect when the operator
 * applies them (Apply/Close prefs, Save settings). The login credentials are the exception
 * and still write through at once: callsign, names, locator, password and both chat
 * categories. They are locked while a session exists ([loginFieldsEnabled]), and without
 * one they take effect at the next login anyway, so holding them back would only make
 * Connect use something other than what the tab shows.
 *
 * The property list was derived mechanically from the setters the JavaFX tab
 * called, intersected with the setters ChatPreferences declares.
 */
class StationTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    /**
     * Applies the chat category to the running session. Passed in rather than taken
     * from a ChatController so the rules stay testable without building a controller,
     * which would open a database.
     */
    private val applyMainCategory: (ChatCategory?) -> Unit,
    private val applySecondCategory: (ChatCategory?) -> Unit,
    /**
     * The state of the ON4KST session. A lambda over Compose state, so the login fields
     * follow it while the tab is open; the default suits a state built without a session.
     */
    private val connectionState: () -> On4KstConnectionState = { On4KstConnectionState.DISCONNECTED },
    /**
     * Restarts or stops rotator 2 to match the stored settings; shared by this tab and the
     * TRX synch tab through [SECOND_ROTATOR], so it runs once per apply.
     */
    private val applySecondRotator: () -> Unit = {},
) {

    var loginChatCategoryMain: ChatCategory?
        get() = prefs.getLoginChatCategoryMain()
        set(value) { prefs.setLoginChatCategoryMain(value) }

    /** Null means "no second chat" — the JavaFX window wrote null for exactly that. */
    var loginChatCategorySecond: ChatCategory?
        get() = prefs.getLoginChatCategorySecond()
        set(value) { prefs.setLoginChatCategorySecond(value) }

    var stn_antennaBeamWidthDeg: Double by draft.setting({ prefs.getStn_antennaBeamWidthDeg() }) { prefs.setStn_antennaBeamWidthDeg(it) }

    var stn_bandActive10G: Boolean by draft.setting({ prefs.isStn_bandActive10G() }) { prefs.setStn_bandActive10G(it) }

    var stn_bandActive1240: Boolean by draft.setting({ prefs.isStn_bandActive1240() }) { prefs.setStn_bandActive1240(it) }

    var stn_bandActive144: Boolean by draft.setting({ prefs.isStn_bandActive144() }) { prefs.setStn_bandActive144(it) }

    var stn_bandActive2300: Boolean by draft.setting({ prefs.isStn_bandActive2300() }) { prefs.setStn_bandActive2300(it) }

    var stn_bandActive3400: Boolean by draft.setting({ prefs.isStn_bandActive3400() }) { prefs.setStn_bandActive3400(it) }

    var stn_bandActive432: Boolean by draft.setting({ prefs.isStn_bandActive432() }) { prefs.setStn_bandActive432(it) }

    var stn_bandActive50: Boolean by draft.setting({ prefs.isStn_bandActive50() }) { prefs.setStn_bandActive50(it) }

    var stn_bandActive5600: Boolean by draft.setting({ prefs.isStn_bandActive5600() }) { prefs.setStn_bandActive5600(it) }

    var stn_bandActive70: Boolean by draft.setting({ prefs.isStn_bandActive70() }) { prefs.setStn_bandActive70(it) }

    var stn_loginCallSign: String
        get() = prefs.getStn_loginCallSign()
        set(value) { prefs.setStn_loginCallSign(value) }

    var stn_loginLocatorMainCat: String
        get() = prefs.getStn_loginLocatorMainCat()
        set(value) { prefs.setStn_loginLocatorMainCat(value) }

    var stn_loginNameMainCat: String
        get() = prefs.getStn_loginNameMainCat()
        set(value) { prefs.setStn_loginNameMainCat(value) }

    var stn_loginNameSecondCat: String
        get() = prefs.getStn_loginNameSecondCat()
        set(value) { prefs.setStn_loginNameSecondCat(value) }

    var stn_loginPassword: String
        get() = prefs.getStn_loginPassword()
        set(value) { prefs.setStn_loginPassword(value) }

    var stn_maxQRBDefault: Double by draft.setting({ prefs.getStn_maxQRBDefault() }) { prefs.setStn_maxQRBDefault(it) }

    var stn_on4kstServersDns: String by draft.setting({ prefs.getStn_on4kstServersDns() }) { prefs.setStn_on4kstServersDns(it) }

    var stn_on4kstServersPort: Int by draft.setting({ prefs.getStn_on4kstServersPort() }) { prefs.setStn_on4kstServersPort(it) }

    var stn_pathAnalysisDefaultTargetAntennaGainDbi: Double by draft.setting({ prefs.getStn_pathAnalysisDefaultTargetAntennaGainDbi() }) { prefs.setStn_pathAnalysisDefaultTargetAntennaGainDbi(it) }

    var stn_pathAnalysisDefaultTargetTxPowerWatts: Double by draft.setting({ prefs.getStn_pathAnalysisDefaultTargetTxPowerWatts() }) { prefs.setStn_pathAnalysisDefaultTargetTxPowerWatts(it) }

    var stn_pathAnalysisDemRootDirectory: String by draft.setting({ prefs.getStn_pathAnalysisDemRootDirectory() }) { prefs.setStn_pathAnalysisDemRootDirectory(it) }

    var stn_pathAnalysisOwnAntennaGainDbi: Double by draft.setting({ prefs.getStn_pathAnalysisOwnAntennaGainDbi() }) { prefs.setStn_pathAnalysisOwnAntennaGainDbi(it) }

    var stn_pathAnalysisOwnAntennaHeightMeters: Double by draft.setting({ prefs.getStn_pathAnalysisOwnAntennaHeightMeters() }) { prefs.setStn_pathAnalysisOwnAntennaHeightMeters(it) }

    var stn_pathAnalysisOwnTxPowerWatts: Double by draft.setting({ prefs.getStn_pathAnalysisOwnTxPowerWatts() }) { prefs.setStn_pathAnalysisOwnTxPowerWatts(it) }

    /**
     * Own ground elevation above sea level in metres, NaN while the elevation model decides.
     * Staged like every path-analysis setting; the reachability service reads the stored
     * value, so a changed height reaches the analysis only once it is applied.
     */
    var stn_pathAnalysisOwnHeightAslMeters: Double by draft.setting({ prefs.getStn_pathAnalysisOwnHeightAslMeters() }) { prefs.setStn_pathAnalysisOwnHeightAslMeters(it) }

    /** Whether the path ends at the highest point of the remote locator square. */
    var stn_pathAnalysisDxHighestPointInSquare: Boolean by draft.setting({ prefs.isStn_pathAnalysisDxHighestPointInSquare() }) { prefs.setStn_pathAnalysisDxHighestPointInSquare(it) }

    /**
     * The entry last committed to the own-height field, as the operator typed it.
     *
     * [Form.committed] keys its draft text on the stored text, and the staged number reads
     * back in its own spelling ("25.0" for "25"). The settings draft is Compose state, so
     * the commit itself recomposes the field with the new key, and without this the field
     * would swap "25" for "25.0" under the cursor right after Enter, or drop keys typed
     * since. Handing back the committed spelling while it still names the same number
     * keeps key and text in agreement.
     */
    private var committedOwnHeightAslText by mutableStateOf<String?>(null)

    /**
     * The own ground elevation as its field shows it: empty while the elevation model
     * decides. The spelling of the last commit while it still names the drafted number,
     * otherwise that number itself (discarded, applied elsewhere, or never committed here).
     * Written only through [commitOwnHeightAsl].
     */
    val stn_pathAnalysisOwnHeightAslText: String
        get() {
            val drafted = stn_pathAnalysisOwnHeightAslMeters.let { if (it.isFinite()) it.toString() else "" }
            val committed = committedOwnHeightAslText

            return if (committed != null && isOwnHeightAslUnchanged(committed, drafted)) committed else drafted
        }

    /**
     * Commits an own ground elevation entry into the draft. Empty hands the elevation back
     * to the model; a number in range is staged; anything else is refused and nothing is
     * staged.
     *
     * @return null when staged, otherwise the reason shown to the operator
     */
    fun commitOwnHeightAsl(entered: String): String? {
        val meters = parseOwnHeightAsl(entered)
            ?: return CurrentStrings.get().stationInvalidOwnHeightAsl(entered.trim())

        stn_pathAnalysisOwnHeightAslMeters = meters
        committedOwnHeightAslText = entered.trim()
        return null
    }

    var stn_pstRotatorEnabled: Boolean by draft.setting(
        { prefs.isStn_pstRotatorEnabled() },
    ) { prefs.setStn_pstRotatorEnabled(it) }

    var stn_useQtfFilterDirectionAsAntennaHeading: Boolean by draft.setting(
        { prefs.isStn_useQtfFilterDirectionAsAntennaHeading() },
    ) { prefs.setStn_useQtfFilterDirectionAsAntennaHeading(it) }

    var stn_pstRotatorHost: String by draft.setting({ prefs.getStn_pstRotatorHost() }) { prefs.setStn_pstRotatorHost(it) }

    /**
     * Refused on apply when it clashes with rotor 2's port as it will be stored, but only
     * while rotor 2 is in use (SO2R on and rotor 2 enabled), so a station without SO2R is
     * never held back by rotor 2's default port. See [portsClash].
     */
    var stn_pstRotatorPort: Int by draft.setting({ prefs.getStn_pstRotatorPort() }) { port ->
        if (prefs.isTrxSynch_so2rEnabled() && stn_pstRotator2Enabled) {
            require(!portsClash(port, stn_pstRotator2Port)) { CurrentStrings.get().stationRotator2PortClash(port.toString()) }
        }
        prefs.setStn_pstRotatorPort(port)
    }

    var stn_pstRotator2Enabled: Boolean by draft.setting(
        { prefs.isStn_pstRotator2Enabled() },
        effectKey = SECOND_ROTATOR,
        effect = { applySecondRotator() },
    ) { prefs.setStn_pstRotator2Enabled(it) }

    var stn_pstRotator2Host: String by draft.setting(
        { prefs.getStn_pstRotator2Host() },
        effectKey = SECOND_ROTATOR,
        effect = { applySecondRotator() },
    ) { prefs.setStn_pstRotator2Host(it) }

    /**
     * Refused on apply when it is the same as or adjacent to rotator 1's port as it will be
     * stored (see [portsClash]). Compared with the drafted value of rotator 1, so changing
     * both in one apply is judged by the result, not by the order of the edits.
     */
    var stn_pstRotator2Port: Int by draft.setting(
        { prefs.getStn_pstRotator2Port() },
        effectKey = SECOND_ROTATOR,
        effect = { applySecondRotator() },
    ) { port ->
        require(!portsClash(port, stn_pstRotatorPort)) { CurrentStrings.get().stationRotator2PortClash(port.toString()) }
        prefs.setStn_pstRotator2Port(port)
    }

    /** Whether the rotator 2 block is active; follows the stored SO2R switch. */
    val so2rEnabled: Boolean
        get() = prefs.isTrxSynch_so2rEnabled()

    var stn_qtfDefault: Double by draft.setting({ prefs.getStn_qtfDefault() }) { prefs.setStn_qtfDefault(it) }

    /**
     * The chat category is held twice: in ChatPreferences and in ChatController.
     * The JavaFX window wrote both on every selection change, and the second write
     * is invisible to the settings coverage test because ChatController is not
     * ChatPreferences. Dropping it would leave the running session on the old
     * category while the stored setting said otherwise.
     */
    /**
     * Whether the station logs into a second chat category as well.
     *
     * It lives on the Station tab because that is where the JavaFX checkbox stood
     * (Kst4ContestApplication:9779, "2nd Chat:"), right next to the two category
     * pickers it governs. It decides what the Connect button does, so it belongs with
     * the login settings and not with the display options.
     */
    var loginToSecondChatEnabled: Boolean by draft.setting({ prefs.isLoginToSecondChatEnabled() }) { prefs.setLoginToSecondChatEnabled(it) }

    /**
     * Whether the login fields may be edited. The JavaFX window disabled callsign,
     * password, name, locator and both category pickers once the session was logged
     * in: changing them there would not reach the running connection, so an edit
     * would look applied and be ignored until the next login.
     *
     * Locked for every state but DISCONNECTED: a reconnect wait or a session being
     * stopped still belongs to the credentials it was started with. Derived on each read
     * rather than held, so a composition reading it follows the connection state the
     * moment that changes, provided [connectionState] reads Compose state.
     */
    val loginFieldsEnabled: Boolean
        get() = connectionState() == On4KstConnectionState.DISCONNECTED

    /**
     * The categories the operator can log into, built the way the JavaFX choice box
     * was: one per entry of `getPossibleCategoryNumbers`, numbered from 1.
     */
    val availableCategories: List<ChatCategory> by lazy {
        val probe = ChatCategory(0)

        (1..probe.possibleCategoryNumbers.size).map { ChatCategory(it) }
    }

    /**
     * The categories offered for the second chat. The main category is left out — the
     * JavaFX box did that too, and logging into the same chat twice is not a thing.
     */
    val secondCategoryOptions: List<ChatCategory>
        get() {
            val mainNumber = loginChatCategoryMain?.categoryNumber

            return availableCategories.filter { it.categoryNumber != mainNumber }
        }

    /** The label a category carries in the pickers. */
    fun describeCategory(category: ChatCategory): String =
        category.getChatCategoryName(category.categoryNumber)

    fun selectMainCategory(category: ChatCategory?) {
        loginChatCategoryMain = category
        applyMainCategory(category)
    }

    fun selectSecondCategory(category: ChatCategory?) {
        loginChatCategorySecond = category
        applySecondCategory(category)
    }

    companion object {
        /** The effect key of everything that restarts rotator 2; shared with the TRX synch tab. */
        const val SECOND_ROTATOR: String = "secondRotator"

        /**
         * Whether two PSTRotator ports collide: the same port, or one apart because
         * PSTRotator answers on port + 1, which would be the other client's local port.
         */
        fun portsClash(a: Int, b: Int): Boolean = kotlin.math.abs(a - b) <= 1

        /**
         * Reads an own ground elevation entry: empty means "elevation model" (NaN), a
         * number in the accepted range is taken, anything else is refused with null.
         * A comma is read as the decimal separator, as in the other number fields.
         */
        fun parseOwnHeightAsl(text: String): Double? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                return Double.NaN
            }
            val meters = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
            return if (ChatPreferences.isValidOwnHeightAslMeters(meters)) meters else null
        }

        /**
         * Whether a draft names the stored height. Compared as numbers, so "250", "250.0"
         * and "250,0" all count as the stored 250.0, and an empty draft as the unset value;
         * a draft that does not parse is never unchanged, so leaving the field refuses it.
         */
        fun isOwnHeightAslUnchanged(draft: String, stored: String): Boolean {
            val typed = parseOwnHeightAsl(draft) ?: return false
            val now = parseOwnHeightAsl(stored) ?: return false
            return if (typed.isNaN() || now.isNaN()) typed.isNaN() && now.isNaN() else typed == now
        }
    }
}
