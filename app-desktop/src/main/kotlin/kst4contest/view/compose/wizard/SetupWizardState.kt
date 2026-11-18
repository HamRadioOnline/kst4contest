package kst4contest.view.compose.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.controller.LoginConfiguration
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences

/** Where the answers of one group come from. */
enum class WizardSource {
    /** Entered in the wizard, starting from the wizard's own empty values. */
    NEW,

    /** What the profile already holds. */
    KEEP,
}

/** The pages, in the order they can appear. */
enum class WizardPage { ENTRY, PERSONAL, LOGIN, SITE, SUMMARY }

/**
 * A band the station can be active on.
 *
 * The labels are amateur shorthand and deliberately not in the translation files.
 */
enum class WizardBand(
    val label: String,
    val megahertz: Int,
    internal val isActive: (ChatPreferences) -> Boolean,
    internal val setActive: (ChatPreferences, Boolean) -> Unit,
) {
    B50("50 MHz", 50, { it.isStn_bandActive50() }, { p, on -> p.setStn_bandActive50(on) }),
    B70("70 MHz", 70, { it.isStn_bandActive70() }, { p, on -> p.setStn_bandActive70(on) }),
    B144("144 MHz", 144, { it.isStn_bandActive144() }, { p, on -> p.setStn_bandActive144(on) }),
    B432("432 MHz", 432, { it.isStn_bandActive432() }, { p, on -> p.setStn_bandActive432(on) }),
    B1240("1240 MHz", 1240, { it.isStn_bandActive1240() }, { p, on -> p.setStn_bandActive1240(on) }),
    B2300("2300 MHz", 2300, { it.isStn_bandActive2300() }, { p, on -> p.setStn_bandActive2300(on) }),
    B3400("3400 MHz", 3400, { it.isStn_bandActive3400() }, { p, on -> p.setStn_bandActive3400(on) }),
    B5600("5600 MHz", 5600, { it.isStn_bandActive5600() }, { p, on -> p.setStn_bandActive5600(on) }),
    B10G("10 GHz", 10000, { it.isStn_bandActive10G() }, { p, on -> p.setStn_bandActive10G(on) }),
}

/**
 * The setup wizard's answers and the rules of its course, free of any window.
 *
 * It reads the preferences once, when it is built, and does not touch them again until
 * [writeTo]: closing the wizard without saving therefore leaves the profile exactly as it
 * was. The design is in `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md`.
 *
 * The answers are Compose state, so a composition reading them follows; nothing else here
 * depends on Compose.
 *
 * @param prefs the profile's preferences, already read from its file
 */
class SetupWizardState(prefs: ChatPreferences) {

    private val loginConfigured = !LoginConfiguration.isUnconfigured(prefs)
    private val templateCredentials = LoginConfiguration.carriesTemplateCredentials(prefs)

    /** The callsign the profile holds, for the label of "leave unchanged". */
    val storedCallsign: String = prefs.getStn_loginCallSign().orEmpty()
    private val storedPassword: String = prefs.getStn_loginPassword().orEmpty()
    private val storedName: String = prefs.getStn_loginNameMainCat().orEmpty()
    private val storedSecondName: String = prefs.getStn_loginNameSecondCat().orEmpty()

    /** The locator the profile holds, for the label of "leave unchanged". */
    val storedLocator: String = prefs.getStn_loginLocatorMainCat().orEmpty()
    private val storedBands: Set<WizardBand> = WizardBand.entries.filter { it.isActive(prefs) }.toSet()
    private val storedCategory: Int? = prefs.getLoginChatCategoryMain()?.categoryNumber
    private val storedSecondChat: Boolean = prefs.isLoginToSecondChatEnabled()
    private val storedSecondCategory: Int? = prefs.getLoginChatCategorySecond()?.categoryNumber

    /** A login can only be kept where an operator entered one. */
    val loginSources: List<WizardSource> = buildList {
        add(WizardSource.NEW)
        if (loginConfigured) add(WizardSource.KEEP)
    }

    /**
     * A station can be kept unless the profile is the untouched template: its locator and
     * bands are then nobody's.
     */
    val stationSources: List<WizardSource> = buildList {
        add(WizardSource.NEW)
        if (!templateCredentials) add(WizardSource.KEEP)
    }

    /** Whether there is anything to choose on the entry page. */
    val hasEntryPage: Boolean = loginSources.size > 1 || stationSources.size > 1

    var loginSource: WizardSource? by mutableStateOf(loginSources.singleOrNull())
        private set

    var stationSource: WizardSource? by mutableStateOf(stationSources.singleOrNull())
        private set

    private var appliedLoginSource: WizardSource? = null
    private var appliedStationSource: WizardSource? = null

    /** The stored form of the language: a code, or empty for the system language. */
    var language: String by mutableStateOf(prefs.getGuiOptions_language().orEmpty())

    /** Empty on an unconfigured profile: its stored name is the template's or another operator's. */
    var name: String by mutableStateOf(if (loginConfigured) storedName else "")

    var callsign: String by mutableStateOf("")
    var password: String by mutableStateOf("")
    var locator: String by mutableStateOf("")

    var bands: Set<WizardBand> by mutableStateOf(emptySet())
        private set

    var category: Int? by mutableStateOf(null)
        private set

    private var categoryChosenByOperator = false

    var secondChat: Boolean by mutableStateOf(false)
        private set

    var secondCategory: Int? by mutableStateOf(null)

    private var loginShown by mutableStateOf(false)
    private var stationShown by mutableStateOf(false)

    var page: WizardPage by mutableStateOf(if (hasEntryPage) WizardPage.ENTRY else WizardPage.PERSONAL)
        private set

    init {
        if (!hasEntryPage) applySources()
    }

    fun chooseLoginSource(source: WizardSource) {
        if (source in loginSources) loginSource = source
    }

    fun chooseStationSource(source: WizardSource) {
        if (source in stationSources) stationSource = source
    }

    /**
     * Loads the answers of a group whose source changed. A group whose source is the one
     * already applied is left alone, so going back to the entry page costs nothing typed.
     */
    private fun applySources() {
        if (loginSource != appliedLoginSource) {
            val keep = loginSource == WizardSource.KEEP
            callsign = if (keep) storedCallsign else ""
            password = if (keep) storedPassword else ""
            loginShown = !keep || !loginComplete
            appliedLoginSource = loginSource
        }

        if (stationSource != appliedStationSource) {
            val keep = stationSource == WizardSource.KEEP
            locator = if (keep) storedLocator else ""
            bands = if (keep) storedBands else emptySet()
            category = if (keep) storedCategory else null
            secondChat = keep && storedSecondChat
            secondCategory = if (keep) storedSecondCategory else null
            // A kept category is the operator's; the suggestion must not replace it.
            categoryChosenByOperator = keep
            stationShown = !keep || !stationComplete
            appliedStationSource = stationSource
        }
    }

    fun setBand(band: WizardBand, active: Boolean) {
        bands = if (active) bands + band else bands - band
        if (!categoryChosenByOperator) updateCategory(suggestCategory(bands))
    }

    /** What the bands suggest, shown beside the picker whatever is chosen. */
    val suggestedCategory: Int?
        get() = suggestCategory(bands)

    fun chooseCategory(number: Int) {
        categoryChosenByOperator = true
        updateCategory(number)
    }

    private fun updateCategory(number: Int?) {
        category = number
        if (secondCategory == number) secondCategory = null
    }

    fun chooseSecondChat(enabled: Boolean) {
        secondChat = enabled
    }

    /** The main category is left out, as the Station tab does. */
    val secondCategoryOptions: List<Int>
        get() = CATEGORY_NUMBERS.filter { it != category }

    /** The pages of this run, in order. */
    val pages: List<WizardPage>
        get() = buildList {
            if (hasEntryPage) add(WizardPage.ENTRY)
            add(WizardPage.PERSONAL)
            if (loginShown) add(WizardPage.LOGIN)
            if (stationShown) add(WizardPage.SITE)
            add(WizardPage.SUMMARY)
        }

    private val personalComplete: Boolean
        get() = isStorableName(name)

    private val loginComplete: Boolean
        get() = isPlausibleCallsign(callsign) && isStorablePassword(password)

    private val stationComplete: Boolean
        get() = isLocator(locator) && bands.isNotEmpty() && category in CATEGORY_NUMBERS &&
            (!secondChat || secondCategory in secondCategoryOptions)

    /** Whether everything the profile needs to log in is there. */
    val mandatoryComplete: Boolean
        get() = personalComplete && loginComplete && stationComplete

    /** A chat name that was typed and cannot be sent. An empty or blank field is not an error yet. */
    val nameRefused: Boolean
        get() = name.isNotBlank() && !isStorableName(name)

    /** A callsign that was typed and does not look like one. An empty field is not an error yet. */
    val callsignRefused: Boolean
        get() = callsign.isNotBlank() && !isPlausibleCallsign(callsign)

    /** A password that was typed and cannot be stored or sent. An empty field is not an error yet. */
    val passwordRefused: Boolean
        get() = password.isNotEmpty() && !isStorablePassword(password)

    /** A locator that was typed and is not one. An empty field is not an error yet. */
    val locatorRefused: Boolean
        get() = locator.isNotBlank() && !isLocator(locator)

    val canGoNext: Boolean
        get() = when (page) {
            WizardPage.ENTRY -> loginSource != null && stationSource != null
            WizardPage.PERSONAL -> personalComplete
            WizardPage.LOGIN -> loginComplete
            WizardPage.SITE -> stationComplete
            WizardPage.SUMMARY -> false
        }

    val canGoBack: Boolean
        get() = pages.indexOf(page) > 0

    /**
     * Whether the remaining steps may be skipped. Only with every mandatory answer, because
     * a summary that cannot be saved helps nobody.
     */
    val canFinish: Boolean
        get() = page != WizardPage.ENTRY && page != WizardPage.SUMMARY && mandatoryComplete

    fun next() {
        if (!canGoNext) return
        if (page == WizardPage.ENTRY) applySources()
        page = pages[pages.indexOf(page) + 1]
    }

    fun back() {
        if (canGoBack) page = pages[pages.indexOf(page) - 1]
    }

    fun finish() {
        if (canFinish) page = WizardPage.SUMMARY
    }

    /** Opens a step from the summary, also one this run skipped. */
    fun edit(target: WizardPage) {
        when (target) {
            WizardPage.LOGIN -> loginShown = true
            WizardPage.SITE -> stationShown = true
            WizardPage.PERSONAL -> Unit
            WizardPage.ENTRY, WizardPage.SUMMARY -> return
        }
        page = target
    }

    /** The profile had a second chat and will have none. */
    val turnsOffSecondChat: Boolean
        get() = storedSecondChat && !secondChat

    /**
     * A new station is set up, and this stage does not ask about log program, rotator,
     * AirScout and DX cluster server: they keep their stored values, and the summary says so.
     */
    val leavesIntegrationsUntouched: Boolean
        get() = stationSource == WizardSource.NEW

    /**
     * Puts the answers into [prefs] and writes its file.
     *
     * Everything the wizard did not ask about stays as [prefs] read it, because the whole
     * file is read and written back. The callsign and the locator are stored as typed, the
     * way the Station tab stores them; only surrounding blanks go.
     *
     * The second chat's name follows the first unless the operator had set it apart, and the
     * second category is left alone while the second chat is off.
     *
     * @return false when a mandatory answer is missing -- nothing is touched then -- or when
     *         the file could not be written
     */
    fun writeTo(prefs: ChatPreferences): Boolean {
        if (!mandatoryComplete) return false

        val newName = name.trim()

        prefs.setGuiOptions_language(language)
        prefs.setStn_loginNameMainCat(newName)
        if (storedSecondName.isBlank() || storedSecondName == storedName) {
            prefs.setStn_loginNameSecondCat(newName)
        }

        prefs.setStn_loginCallSign(callsign.trim())
        prefs.setStn_loginPassword(password)

        prefs.setStn_loginLocatorMainCat(locator.trim())
        WizardBand.entries.forEach { it.setActive(prefs, it in bands) }
        prefs.setLoginChatCategoryMain(ChatCategory(requireNotNull(category)))
        prefs.setLoginToSecondChatEnabled(secondChat)
        if (secondChat) {
            prefs.setLoginChatCategorySecond(ChatCategory(requireNotNull(secondCategory)))
        }

        // A write that throws is a write that failed; the wizard must stay open either way.
        return runCatching { prefs.writePreferencesToXmlFile() }.getOrDefault(false)
    }

    companion object {

        /** The categories the pickers offer, the way the Station tab builds them. */
        val CATEGORY_NUMBERS: List<Int> = ChatCategory(0).possibleCategoryNumbers.toList()

        private val CALLSIGN = Regex("[A-Za-z0-9]+([-/][A-Za-z0-9]+)?")
        private val LOCATOR = Regex("[A-Ra-r]{2}[0-9]{2}[A-Xa-x]{2}")

        /**
         * Whether a text can be a login callsign: letters and digits, at most one "-" or "/"
         * suffix, and a base of three characters with a letter and a digit. Deliberately
         * loose -- ON4KST decides what it accepts, this only catches a slip.
         */
        fun isPlausibleCallsign(text: String): Boolean {
            val trimmed = text.trim()
            if (!CALLSIGN.matches(trimmed)) return false
            val base = trimmed.substringBefore('-').substringBefore('/')
            return base.length >= 3 && base.any(Char::isDigit) && base.any(Char::isLetter)
        }

        /** Whether a text is a six-character Maidenhead locator, in either case. */
        fun isLocator(text: String): Boolean = LOCATOR.matches(text.trim())

        /**
         * Whether a password can be stored in preferences.xml and sent in an ON4KST login frame:
         * not blank, free of surrounding whitespace (which the XML reader trims), and free of
         * frame delimiters.
         */
        fun isStorablePassword(text: String): Boolean =
            text.isNotBlank() && text == text.trim() && '|' !in text && '\r' !in text && '\n' !in text

        /**
         * Whether a chat display name can be sent in an ON4KST frame: not blank and free of
         * frame delimiters.
         */
        fun isStorableName(text: String): Boolean =
            text.isNotBlank() && '|' !in text && '\r' !in text && '\n' !in text

        /**
         * The chat category the lowest band belongs to: 1 for 50/70 MHz, 2 for 144/432 MHz,
         * 3 above. Null without a band. A suggestion only -- the operator confirms it.
         */
        fun suggestCategory(bands: Set<WizardBand>): Int? {
            val lowest = bands.minOfOrNull { it.megahertz } ?: return null
            return when {
                lowest <= 70 -> ChatCategory.FIFTYSEVENTYMHz
                lowest <= 432 -> ChatCategory.VUHF
                else -> ChatCategory.MICROWAVE
            }
        }
    }
}
