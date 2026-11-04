package kst4contest.view.compose.tabs

import kst4contest.controller.ChatController
import kst4contest.model.ChatPreferences
import kst4contest.view.i18n.CurrentStrings
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting

/**
 * State of the beacon tab: the two CQ templates and the one interval they share.
 *
 * The two commit methods are the ports of `applyBeaconTextSetting` and
 * `applySharedBeaconInterval`. Both returned nothing and opened an alert; here they
 * return the refusal text instead, so the tab decides how to show it and a test can
 * read it. Accepted templates and intervals are staged in the window's [SettingsDraft]
 * and take effect when the operator applies them. The two enable switches deliberately
 * bypass the draft so the running timer sees their values immediately.
 *
 * @param validateBeaconTemplate throws IllegalArgumentException with the reason,
 *        the contract of `ChatController.validateBeaconTemplate`.
 * @param restartBeaconTimer called when a changed interval is applied; the JavaFX code
 *        restarted the shared timer there and nowhere else.
 */
class BeaconTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    private val validateBeaconTemplate: (String) -> Unit,
    private val restartBeaconTimer: () -> Unit,
    val mainCategoryName: String,
    val secondCategoryName: String,
    private val setMainBeaconEnabled: (Boolean) -> Unit = prefs::setBcn_beaconsEnabledMainCat,
    private val setSecondBeaconEnabled: (Boolean) -> Unit = prefs::setBcn_beaconsEnabledSecondCat,
) {

    val mainBeaconEnabled = prefs.bcnBeaconsEnabledMainCatProperty()
    val secondBeaconEnabled = prefs.bcnBeaconsEnabledSecondCatProperty()

    var bcn_beaconsEnabledMainCat: Boolean
        get() = prefs.isBcn_beaconsEnabledMainCat()
        set(value) = setMainBeaconEnabled(value)

    var bcn_beaconsEnabledSecondCat: Boolean
        get() = prefs.isBcn_beaconsEnabledSecondCat()
        set(value) = setSecondBeaconEnabled(value)

    private var textMainCat: String by draft.setting(
        { prefs.getBcn_beaconTextMainCat() },
    ) { prefs.setBcn_beaconTextMainCat(it) }

    private var textSecondCat: String by draft.setting(
        { prefs.getBcn_beaconTextSecondCat() },
    ) { prefs.setBcn_beaconTextSecondCat(it) }

    /*
     * Both stored intervals written together, as applySharedBeaconInterval did. The
     * second-category value is legacy XML which nothing reads any more, but it is written
     * to keep the two in step.
     */
    private var interval: Int by draft.setting(
        { prefs.getBcn_beaconIntervalInMinutesMainCat() },
        effect = { restartBeaconTimer() },
    ) { minutes ->
        prefs.setBcn_beaconIntervalInMinutesMainCat(minutes)
        prefs.setBcn_beaconIntervalInMinutesSecondCat(minutes)
    }

    val beaconTextMainCat: String
        get() = textMainCat

    val beaconTextSecondCat: String
        get() = textSecondCat

    /**
     * The interval both categories share, never below the minimum.
     *
     * The clamp is not cosmetic: a stored zero would otherwise be displayed as a
     * valid setting while `ChatController` sends at the minimum anyway
     * when scheduling the timer. The JavaFX field applied the same `Math.max` when it
     * restored the value after a rejected entry.
     */
    val beaconIntervalMinutes: Int
        get() = maxOf(
            ChatController.MIN_BEACON_INTERVAL_MINUTES,
            interval,
        )

    /** @return null when staged, otherwise the reason it was refused. */
    fun commitBeaconTextMainCat(template: String): String? =
        commitBeaconText(template) { textMainCat = it }

    /** @return null when staged, otherwise the reason it was refused. */
    fun commitBeaconTextSecondCat(template: String): String? =
        commitBeaconText(template) { textSecondCat = it }

    private fun commitBeaconText(template: String, store: (String) -> Unit): String? {
        try {
            validateBeaconTemplate(template)
        } catch (refusal: IllegalArgumentException) {
            /*
             * The stored template stands. MYCALL and MYQRG are resolved by the timer,
             * so a refused template that replaced a working one would silently stop
             * the beacon instead of reporting anything.
             */
            return CurrentStrings.get().beaconInvalidMessage(refusal.message.orEmpty())
        }

        store(template)
        return null
    }

    /**
     * Stages the shared interval for both categories; applying it restarts the timer.
     *
     * @return null when staged, otherwise the reason it was refused.
     */
    fun commitBeaconInterval(entered: String): String? {
        val minutes = entered.trim().toIntOrNull()

        if (minutes == null || minutes < ChatController.MIN_BEACON_INTERVAL_MINUTES) {
            return CurrentStrings.get().beaconIntervalTooShort(ChatController.MIN_BEACON_INTERVAL_MINUTES.toString())
        }

        interval = minutes
        return null
    }
}
