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

    /** Taken from another, configured profile. */
    TAKE,

    /** What the profile already holds. */
    KEEP,
}

/**
 * Another profile the login or the station can be taken from.
 *
 * Its preferences are read from its file and never written: taking over fills the wizard's
 * answers, and Save writes only into the active profile.
 *
 * @param prefs the other profile's preferences, already read from its file
 */
class WizardDonor(val profileId: String, val displayName: String, val prefs: ChatPreferences) {

    /** For the entry page's label. */
    val callsign: String = prefs.getStn_loginCallSign().orEmpty()

    /** For the entry page's label. */
    val locator: String = prefs.getStn_loginLocatorMainCat().orEmpty()
}

/** The pages, in the order they can appear. */
enum class WizardPage { ENTRY, PERSONAL, LOGIN, SITE, ANTENNA, LOG, GEAR, SUMMARY }

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
 * @param donors other configured profiles the login or the station can be taken from
 */
class SetupWizardState(prefs: ChatPreferences, val donors: List<WizardDonor> = emptyList()) {

    /** What the pages of the station group show of one profile's preferences. */
    private class StationSnapshot(prefs: ChatPreferences) {
        val locator: String = prefs.getStn_loginLocatorMainCat().orEmpty()
        val bands: Set<WizardBand> = WizardBand.entries.filter { it.isActive(prefs) }.toSet()
        val category: Int? = prefs.getLoginChatCategoryMain()?.categoryNumber
        val secondChat: Boolean = prefs.isLoginToSecondChatEnabled()
        val secondCategory: Int? = prefs.getLoginChatCategorySecond()?.categoryNumber
    }

    /** Kept to rebuild the answers of the station group when its source changes. */
    private val ownPrefs: ChatPreferences = prefs
    private val ownStation = StationSnapshot(prefs)

    /** What the own profile has on, whatever the answers are built from. */
    private val ownSwitches: Set<WizardSwitch> = WizardIntegrations(prefs).let {
        it.storedLogSwitches() + it.storedGearSwitches()
    }

    private val loginConfigured = !LoginConfiguration.isUnconfigured(prefs)
    private val templateCredentials = LoginConfiguration.carriesTemplateCredentials(prefs)

    /** The callsign the profile holds, for the label of "leave unchanged". */
    val storedCallsign: String = prefs.getStn_loginCallSign().orEmpty()
    private val storedPassword: String = prefs.getStn_loginPassword().orEmpty()
    private val storedName: String = prefs.getStn_loginNameMainCat().orEmpty()
    private val storedSecondName: String = prefs.getStn_loginNameSecondCat().orEmpty()

    /** The locator the profile holds, for the label of "leave unchanged". */
    val storedLocator: String = ownStation.locator

    /** A login can be taken where another profile has one, and kept where an operator entered one. */
    val loginSources: List<WizardSource> = buildList {
        add(WizardSource.NEW)
        if (donors.isNotEmpty()) add(WizardSource.TAKE)
        if (loginConfigured) add(WizardSource.KEEP)
    }

    /**
     * A station can be kept unless the profile is the untouched template: its locator and
     * bands are then nobody's.
     */
    val stationSources: List<WizardSource> = buildList {
        add(WizardSource.NEW)
        if (donors.isNotEmpty()) add(WizardSource.TAKE)
        if (!templateCredentials) add(WizardSource.KEEP)
    }

    /** Whether there is anything to choose on the entry page. */
    val hasEntryPage: Boolean = loginSources.size > 1 || stationSources.size > 1

    var loginSource: WizardSource? by mutableStateOf(loginSources.singleOrNull())
        private set

    var stationSource: WizardSource? by mutableStateOf(stationSources.singleOrNull())
        private set

    /** The profile the login is taken from; decided with the source when there is only one. */
    var loginDonor: WizardDonor? by mutableStateOf(null)
        private set

    /** The profile the station is taken from; decided with the source when there is only one. */
    var stationDonor: WizardDonor? by mutableStateOf(null)
        private set

    /* A group is reloaded when its source or its donor changes, and only then. */
    private var appliedLogin: Pair<WizardSource?, WizardDonor?> = null to null
    private var appliedStation: Pair<WizardSource?, WizardDonor?> = null to null

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

    /**
     * Antenna and power; always proposals from the profile the station comes from. Replaced
     * when the station's source changes, so nothing typed under one choice shows under another.
     */
    var antenna: WizardAntenna by mutableStateOf(WizardAntenna(prefs))
        private set

    /** Log program, rotator and AirScout; replaced like [antenna]. */
    var integrations: WizardIntegrations by mutableStateOf(WizardIntegrations(prefs))
        private set

    private var loginShown by mutableStateOf(false)

    /*
     * One flag per page of the station group. A page that was shown in this run is written;
     * a page that never was leaves its settings exactly as the profile has them. That is
     * what keeps "leave the station unchanged" from touching integrations the wizard's
     * simpler questions could not express.
     */
    private var siteShown by mutableStateOf(false)
    private var antennaShown by mutableStateOf(false)
    private var logShown by mutableStateOf(false)
    private var gearShown by mutableStateOf(false)

    var page: WizardPage by mutableStateOf(if (hasEntryPage) WizardPage.ENTRY else WizardPage.PERSONAL)
        private set

    init {
        if (!hasEntryPage) applySources()
    }

    fun chooseLoginSource(source: WizardSource) {
        if (source !in loginSources) return
        loginSource = source
        if (source == WizardSource.TAKE && loginDonor == null) loginDonor = donors.singleOrNull()
    }

    fun chooseStationSource(source: WizardSource) {
        if (source !in stationSources) return
        stationSource = source
        if (source == WizardSource.TAKE && stationDonor == null) stationDonor = donors.singleOrNull()
    }

    fun chooseLoginDonor(donor: WizardDonor) {
        if (donor in donors) loginDonor = donor
    }

    fun chooseStationDonor(donor: WizardDonor) {
        if (donor in donors) stationDonor = donor
    }

    /** A source is decided once it is chosen and, when it takes over, says from whom. */
    private fun decided(source: WizardSource?, donor: WizardDonor?): Boolean =
        source != null && (source != WizardSource.TAKE || donor != null)

    /**
     * Loads the answers of a group whose source or donor changed. A group whose choice is the
     * one already applied is left alone, so going back to the entry page costs nothing typed.
     */
    private fun applySources() {
        val login = loginSource to loginDonor.takeIf { loginSource == WizardSource.TAKE }
        if (login != appliedLogin) {
            when (loginSource) {
                WizardSource.KEEP -> {
                    callsign = storedCallsign
                    password = storedPassword
                }
                WizardSource.TAKE -> {
                    callsign = loginDonor?.prefs?.getStn_loginCallSign().orEmpty()
                    password = loginDonor?.prefs?.getStn_loginPassword().orEmpty()
                }
                else -> {
                    callsign = ""
                    password = ""
                }
            }
            // Shown when entered anew, and when what was kept or taken cannot be saved as it is.
            loginShown = loginSource == WizardSource.NEW || !loginComplete
            appliedLogin = login
        }

        val station = stationSource to stationDonor.takeIf { stationSource == WizardSource.TAKE }
        if (station != appliedStation) {
            val fresh = stationSource == WizardSource.NEW
            val base = if (stationSource == WizardSource.TAKE) stationDonor?.prefs ?: ownPrefs else ownPrefs
            val snapshot = if (base === ownPrefs) ownStation else StationSnapshot(base)

            locator = if (fresh) "" else snapshot.locator
            bands = if (fresh) emptySet() else snapshot.bands
            category = if (fresh) null else snapshot.category
            secondChat = !fresh && snapshot.secondChat
            secondCategory = if (fresh) null else snapshot.secondCategory
            // A kept or taken category is an operator's; the suggestion must not replace it.
            categoryChosenByOperator = !fresh

            siteShown = fresh || !stationComplete
            antennaShown = fresh
            logShown = fresh
            gearShown = fresh

            // Built anew from the profile the station comes from; a new station gets only
            // what the operator says yes to.
            antenna = WizardAntenna(base)
            integrations = WizardIntegrations(base).also { if (fresh) it.loadFresh() }
            appliedStation = station
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
            if (siteShown) add(WizardPage.SITE)
            if (antennaShown) add(WizardPage.ANTENNA)
            if (logShown) add(WizardPage.LOG)
            if (gearShown) add(WizardPage.GEAR)
            add(WizardPage.SUMMARY)
        }

    private val personalComplete: Boolean
        get() = isStorableName(name)

    private val loginComplete: Boolean
        get() = isPlausibleCallsign(callsign) && isStorablePassword(password)

    private val stationComplete: Boolean
        get() = isLocator(locator) && bands.isNotEmpty() && category in CATEGORY_NUMBERS &&
            (!secondChat || secondCategory in secondCategoryOptions)

    /** The optional pages that are part of this run hold nothing that cannot be stored. */
    private val shownOptionalPagesComplete: Boolean
        get() = (!antennaShown || antenna.complete) &&
            (!logShown || integrations.logComplete) &&
            (!gearShown || integrations.gearComplete)

    /**
     * Whether the answers can be saved: everything the profile needs to log in is there, and
     * no page of this run holds a value that cannot be stored. The optional pages need no
     * answer, but an out-of-range port on one of them still blocks.
     */
    val mandatoryComplete: Boolean
        get() = personalComplete && loginComplete && stationComplete && shownOptionalPagesComplete

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
            WizardPage.ENTRY -> decided(loginSource, loginDonor) && decided(stationSource, stationDonor)
            WizardPage.PERSONAL -> personalComplete
            WizardPage.LOGIN -> loginComplete
            WizardPage.SITE -> stationComplete
            WizardPage.ANTENNA -> antenna.complete
            WizardPage.LOG -> integrations.logComplete
            WizardPage.GEAR -> integrations.gearComplete
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

    /**
     * Whether the log page may be opened: always once it is part of the run, and otherwise
     * only if the profile has at most one log source on. The page knows one source; opened
     * over several it would switch the others off.
     */
    val logPageEditable: Boolean
        get() = logShown || !integrations.severalStoredLogSources

    /** Opens a step from the summary, also one this run skipped. */
    fun edit(target: WizardPage) {
        when (target) {
            WizardPage.LOGIN -> loginShown = true
            WizardPage.SITE -> siteShown = true
            WizardPage.ANTENNA -> antennaShown = true
            WizardPage.LOG -> if (logPageEditable) logShown = true else return
            WizardPage.GEAR -> gearShown = true
            WizardPage.PERSONAL -> Unit
            WizardPage.ENTRY, WizardPage.SUMMARY -> return
        }
        page = target
    }

    /**
     * Whether the frequency is taken from a log program after Save, for the summary: the
     * page's answer once the log page is part of the run, and what the profile holds before.
     * The two differ where a frequency source is on without its log source, which the
     * settings window allows and the page cannot show.
     */
    val summaryQrgFollow: Boolean
        get() = if (logShown) integrations.qrgFollow else integrations.storedQrgFollow

    /** Whether the DX cluster server runs after Save; see [summaryQrgFollow]. */
    val summaryClusterServer: Boolean
        get() = if (logShown) integrations.networked && integrations.clusterServer else integrations.storedClusterServer

    /**
     * The fallback band prefix the profile has after Save, for the summary: taken with the
     * donor's station when chosen, otherwise left as the own profile holds it.
     */
    val summaryFallbackBand: String
        get() {
            val sourcePrefs = stationDonor?.takeIf { stationSource == WizardSource.TAKE }?.prefs ?: ownPrefs
            return sourcePrefs.getNotify_optionalFrequencyPrefix()?.get().orEmpty()
        }

    /** Rotator 2 only counts while SO2R uses it, as on the Station tab. */
    private fun so2rSwitches(prefs: ChatPreferences): Set<WizardSwitch> = buildSet {
        if (prefs.isTrxSynch_so2rEnabled()) {
            add(WizardSwitch.SO2R)
            if (prefs.isStn_pstRotator2Enabled()) add(WizardSwitch.ROTATOR_2)
        }
    }

    /**
     * What is on in the own profile and will be off after Save, for the summary. Always
     * against the own profile: with a station taken from another one, that is what the
     * operator loses, whatever the other profile had.
     *
     * A page that is not part of the run contributes what its profile holds -- the own one
     * for a station left unchanged, the donor's for a station taken over. The wizard never
     * writes SO2R or rotator 2 itself, so after Save they come from the donor when taking
     * over a station, otherwise from the own profile.
     */
    val switchedOff: List<WizardSwitch>
        get() {
            val neverAskedPrefs = stationDonor?.takeIf { stationSource == WizardSource.TAKE }?.prefs ?: ownPrefs
            val after = buildSet {
                if (secondChat) add(WizardSwitch.SECOND_CHAT)
                addAll(if (logShown) integrations.answeredLogSwitches() else integrations.storedLogSwitches())
                addAll(if (gearShown) integrations.answeredGearSwitches() else integrations.storedGearSwitches())
                addAll(so2rSwitches(neverAskedPrefs))
            }
            val before = ownSwitches + so2rSwitches(ownPrefs) +
                if (ownStation.secondChat) setOf(WizardSwitch.SECOND_CHAT) else emptySet()
            return WizardSwitch.entries.filter { it in before && it !in after }
        }

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
     * Antenna, log program and equipment are written only for the pages that were part of
     * this run.
     *
     * A station taken from another profile is copied whole before the answers are written.
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

        // The whole station of the other profile first, also what no page asks about; the
        // answers below then lie on top, so a taken value the operator changed wins.
        if (stationSource == WizardSource.TAKE) {
            stationDonor?.let { copyStation(it.prefs, prefs) }
        }

        prefs.setStn_loginLocatorMainCat(locator.trim())
        WizardBand.entries.forEach { it.setActive(prefs, it in bands) }
        prefs.setLoginChatCategoryMain(ChatCategory(requireNotNull(category)))
        prefs.setLoginToSecondChatEnabled(secondChat)
        if (secondChat) {
            prefs.setLoginChatCategorySecond(ChatCategory(requireNotNull(secondCategory)))
        }

        if (antennaShown) antenna.writeTo(prefs)
        if (logShown) integrations.writeLogTo(prefs)
        if (gearShown) integrations.writeGearTo(prefs)

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
