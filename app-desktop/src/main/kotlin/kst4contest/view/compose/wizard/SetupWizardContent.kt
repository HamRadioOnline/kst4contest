package kst4contest.view.compose.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatCategory
import kst4contest.view.compose.Density
import kst4contest.view.compose.Form
import kst4contest.view.compose.LocalSettingsDraft
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.tabs.FilePathRow
import kst4contest.view.i18n.LanguageStore
import kst4contest.view.i18n.LocalStrings
import kst4contest.view.i18n.SYSTEM_LANGUAGE
import kst4contest.view.i18n.Strings
import kst4contest.view.i18n.Translations

/**
 * A language store for the wizard: a choice takes effect on screen at once and is kept in
 * the wizard's answers, not in the preferences. "Later" therefore stores no language, and
 * the runtime built afterwards puts the stored one back in force.
 */
fun wizardLanguageStore(state: SetupWizardState): LanguageStore =
    LanguageStore(storedLanguage = { state.language }, storeLanguage = { state.language = it })

/** The name a chat category carries in the pickers, the one the Station tab shows. */
private fun categoryName(number: Int): String = ChatCategory(number).getChatCategoryName(number)

/**
 * The wizard's pages and its button row.
 *
 * All decisions are in [state]; this draws them. The texts are read from [languageStore] in
 * the composition, so the language picker on the personal page switches the wizard itself.
 *
 * @param saveFailed whether the last Save could not write the file
 * @param onLater the operator does not want to set up now; nothing is written
 * @param onSave the operator saves; true when the session should be started afterwards
 */
@Composable
fun SetupWizardContent(
    state: SetupWizardState,
    languageStore: LanguageStore,
    saveFailed: Boolean,
    onLater: () -> Unit,
    onSave: (connect: Boolean) -> Unit,
) {
    val strings = languageStore.strings
    // The draft only registers typed entries. Wizard answers are never staged or applied through it.
    val typedEntries = remember { SettingsDraft() }
    var typedEntryRefusals by remember { mutableStateOf(emptyList<String>()) }

    fun withAcceptedTypedEntries(action: () -> Unit) {
        val refusals = typedEntries.commitTypedEntries()
        typedEntryRefusals = refusals
        if (refusals.isEmpty()) action()
    }

    CompositionLocalProvider(LocalStrings provides strings, LocalSettingsDraft provides typedEntries) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(15.dp)) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (state.page) {
                        WizardPage.ENTRY -> EntryPage(state, strings)
                        WizardPage.PERSONAL -> PersonalPage(state, strings, languageStore)
                        WizardPage.LOGIN -> LoginPage(state, strings)
                        WizardPage.SITE -> SitePage(state, strings)
                        WizardPage.ANTENNA -> AntennaPage(state.antenna, strings)
                        WizardPage.LOG -> LogPage(state.integrations, strings)
                        WizardPage.GEAR -> GearPage(state.integrations, strings)
                        WizardPage.SUMMARY -> SummaryPage(state, strings, saveFailed)
                    }
                }

                if (typedEntryRefusals.isNotEmpty()) {
                    Refusal(typedEntryRefusals.joinToString("\n"))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
                ) {
                    Form.button(strings.wizardLater, onClick = onLater)
                    Spacer(Modifier.weight(1f))
                    if (state.canGoBack) {
                        Form.button(strings.wizardBack, onClick = state::back)
                    }
                    if (state.page == WizardPage.SUMMARY) {
                        Form.button(strings.wizardSave, enabled = state.mandatoryComplete) {
                            withAcceptedTypedEntries { onSave(false) }
                        }
                        Form.button(
                            strings.wizardSaveAndConnect,
                            enabled = state.mandatoryComplete,
                            accented = true,
                        ) { withAcceptedTypedEntries { onSave(true) } }
                    } else {
                        if (state.canFinish) {
                            Form.button(strings.wizardFinish) { withAcceptedTypedEntries(state::finish) }
                        }
                        Form.button(
                            strings.wizardNext,
                            enabled = state.canGoNext,
                            accented = true,
                        ) { withAcceptedTypedEntries(state::next) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(title: String, lead: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(lead, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun Refusal(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun RadioOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun EntryPage(state: SetupWizardState, strings: Strings) {
    Heading(strings.wizardEntryTitle, strings.wizardEntryLead)
    val oneDonor = state.donors.singleOrNull()

    /* An axis with a single option is decided already and is not asked about. */
    if (state.loginSources.size > 1) {
        Form.section(strings.wizardEntryLogin) {
            state.loginSources.forEach { source ->
                RadioOption(
                    label = when (source) {
                        WizardSource.NEW -> strings.wizardSourceLoginNew
                        // One other profile is named in the option; several get a picker below.
                        WizardSource.TAKE ->
                            if (oneDonor != null) strings.wizardSourceLoginTake(oneDonor.displayName, oneDonor.callsign)
                            else strings.wizardSourceLoginTakeFrom
                        WizardSource.KEEP -> strings.wizardSourceLoginKeep(state.storedCallsign)
                    },
                    selected = state.loginSource == source,
                    onSelect = { state.chooseLoginSource(source) },
                )
            }
            if (state.loginSource == WizardSource.TAKE && oneDonor == null) {
                Form.choice(
                    label = strings.wizardEntryDonor,
                    items = state.donors,
                    selected = state.loginDonor,
                    describe = { "${it.displayName} (${it.callsign})" },
                    onSelect = state::chooseLoginDonor,
                )
            }
        }
    }

    if (state.stationSources.size > 1) {
        Form.section(strings.wizardEntryStation) {
            state.stationSources.forEach { source ->
                RadioOption(
                    label = when (source) {
                        WizardSource.NEW -> strings.wizardSourceStationNew
                        WizardSource.TAKE ->
                            if (oneDonor != null) strings.wizardSourceStationTake(oneDonor.displayName, oneDonor.locator)
                            else strings.wizardSourceStationTakeFrom
                        WizardSource.KEEP -> strings.wizardSourceStationKeep(state.storedLocator)
                    },
                    selected = state.stationSource == source,
                    onSelect = { state.chooseStationSource(source) },
                )
            }
            if (state.stationSource == WizardSource.TAKE && oneDonor == null) {
                Form.choice(
                    label = strings.wizardEntryDonor,
                    items = state.donors,
                    selected = state.stationDonor,
                    describe = { "${it.displayName} (${it.locator})" },
                    onSelect = state::chooseStationDonor,
                )
            }
        }
    }
}

@Composable
private fun PersonalPage(state: SetupWizardState, strings: Strings, languageStore: LanguageStore) {
    Heading(strings.wizardPersonalTitle, strings.wizardPersonalLead)

    Form.choice(
        label = strings.guiLanguage,
        items = listOf(SYSTEM_LANGUAGE) + Translations.BY_LANGUAGE.keys.sorted(),
        selected = languageStore.stored,
        // The code in capitals, as the GUI tab shows it.
        describe = { code -> if (code == SYSTEM_LANGUAGE) strings.guiLanguageSystem else code.uppercase() },
        onSelect = { languageStore.use(it) },
    )
    Form.text(strings.wizardPersonalName, state.name) { state.name = it }
    if (state.nameRefused) Refusal(strings.wizardPersonalNameInvalid)
}

@Composable
private fun LoginPage(state: SetupWizardState, strings: Strings) {
    Heading(strings.wizardLoginTitle, strings.wizardLoginLead)

    Form.text(strings.stationCallsign, state.callsign) { state.callsign = it }
    if (state.callsignRefused) Refusal(strings.wizardLoginCallsignInvalid)
    Form.password(strings.stationPassword, state.password) { state.password = it }
    if (state.passwordRefused) Refusal(strings.wizardLoginPasswordInvalid)
}

@Composable
private fun SitePage(state: SetupWizardState, strings: Strings) {
    Heading(strings.wizardSiteTitle, strings.wizardSiteLead)

    Form.text(strings.stationLocator, state.locator) { state.locator = it }
    if (state.locatorRefused) Refusal(strings.wizardSiteLocatorInvalid)

    Form.flowingSection(strings.wizardSiteBands) {
        WizardBand.entries.forEach { band ->
            Form.check(band.label, band in state.bands) { state.setBand(band, it) }
        }
    }
    Text(strings.wizardSiteBandsHint, style = MaterialTheme.typography.bodySmall)

    Form.choice(
        label = strings.stationMainCategory,
        items = SetupWizardState.CATEGORY_NUMBERS,
        selected = state.category,
        describe = ::categoryName,
        onSelect = state::chooseCategory,
    )
    state.suggestedCategory?.let { suggested ->
        Text(
            strings.wizardSiteCategorySuggestion(categoryName(suggested)),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Form.check(strings.wizardSiteSecondChat, state.secondChat) { state.chooseSecondChat(it) }
    if (state.secondChat) {
        Form.choice(
            label = strings.stationSecondCategory,
            items = state.secondCategoryOptions,
            selected = state.secondCategory,
            describe = ::categoryName,
            onSelect = { state.secondCategory = it },
        )
    }
}

/** The name a log program carries on its page and in the summary. */
private fun logProgramName(program: WizardLogProgram, strings: Strings): String = when (program) {
    WizardLogProgram.NONE -> strings.wizardLogNone
    WizardLogProgram.WINTEST -> strings.wizardLogWintest
    WizardLogProgram.UDP -> strings.wizardLogUdp
    WizardLogProgram.FILE -> strings.wizardLogFile
}

/** The name of something that will be switched off, for the summary's notice. */
private fun switchName(switch: WizardSwitch, strings: Strings): String = when (switch) {
    WizardSwitch.SECOND_CHAT -> strings.wizardSwitchSecondChat
    WizardSwitch.LOG_WINTEST -> strings.wizardSwitchLogWintest
    WizardSwitch.LOG_UDP -> strings.wizardSwitchLogUdp
    WizardSwitch.LOG_FILE -> strings.wizardSwitchLogFile
    WizardSwitch.QRG_FOLLOW -> strings.wizardSwitchQrgFollow
    WizardSwitch.CLUSTER_SERVER -> strings.wizardSwitchClusterServer
    WizardSwitch.ROTATOR -> strings.wizardSwitchRotator
    WizardSwitch.AIRSCOUT -> strings.wizardSwitchAirScout
    WizardSwitch.SO2R -> strings.wizardSwitchSo2r
    WizardSwitch.ROTATOR_2 -> strings.wizardSwitchRotator2
}

@Composable
private fun AntennaPage(antenna: WizardAntenna, strings: Strings) {
    Heading(strings.wizardAntennaTitle, strings.wizardAntennaLead)

    Form.decimal(strings.stationBeamWidth, antenna.beamWidthDeg, isValid = { it.isFinite() }) {
        antenna.beamWidthDeg = it
    }
    Form.decimal(strings.stationOwnAntennaGain, antenna.gainDbi, isValid = { it.isFinite() }) {
        antenna.gainDbi = it
    }
    Form.decimal(strings.stationOwnAntennaHeight, antenna.heightMeters, isValid = { it.isFinite() && it >= 0.0 }) {
        antenna.heightMeters = it
    }
    Form.decimal(strings.stationOwnTxPower, antenna.txPowerWatts, isValid = { it.isFinite() }) {
        antenna.txPowerWatts = it
    }

    Form.text(strings.stationOwnHeightAsl, antenna.heightAslText) { antenna.heightAslText = it }
    if (antenna.heightAslRefused) {
        Refusal(strings.stationInvalidOwnHeightAsl(antenna.heightAslText.trim()))
    }
    Text(strings.wizardAntennaAslHint, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun LogPage(integrations: WizardIntegrations, strings: Strings) {
    Heading(strings.wizardLogTitle, strings.wizardLogLead)

    WizardLogProgram.entries.forEach { program ->
        RadioOption(
            label = logProgramName(program, strings),
            selected = integrations.logProgram == program,
            onSelect = { integrations.chooseLogProgram(program) },
        )
    }

    /* The details of a program appear only once it is chosen. */
    when (integrations.logProgram) {
        WizardLogProgram.WINTEST -> {
            Form.int(strings.logSynchWinTestPort, integrations.wintestPort) { integrations.wintestPort = it }
            Form.text(strings.logSynchWinTestStationName, integrations.wintestStationName) {
                integrations.wintestStationName = it
            }
        }
        WizardLogProgram.UDP ->
            Form.int(strings.logSynchSharedPort, integrations.udpPort) { integrations.udpPort = it }
        WizardLogProgram.FILE ->
            FilePathRow(
                label = strings.logSynchFileLabel,
                value = integrations.filePath,
                chooserTitle = strings.logSynchFileChooserTitle,
            ) { integrations.filePath = it }
        WizardLogProgram.NONE -> Unit
    }
    if (!integrations.logComplete) Refusal(strings.wizardLogIncomplete)

    if (integrations.networked) {
        Form.check(strings.wizardLogQrgFollow, integrations.qrgFollow) { integrations.qrgFollow = it }
        Form.check(strings.wizardLogClusterServer, integrations.clusterServer) { integrations.clusterServer = it }
        // The settings tab's own words: an enabled source does not produce a frequency by itself.
        Text(strings.trxSynchHint, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GearPage(integrations: WizardIntegrations, strings: Strings) {
    Heading(strings.wizardGearTitle, strings.wizardGearLead)

    Form.check(strings.wizardGearRotator, integrations.rotator) { integrations.rotator = it }
    if (integrations.rotator) {
        Form.text(strings.stationRotatorHost, integrations.rotatorHost) { integrations.rotatorHost = it }
        Form.int(strings.stationRotatorPort, integrations.rotatorPort) { integrations.rotatorPort = it }
        if (integrations.rotatorPortClashes) {
            Refusal(strings.stationRotator2PortClash(integrations.rotatorPort.toString()))
        }
    }

    Form.check(strings.wizardGearAirScout, integrations.airScout) { integrations.airScout = it }
    if (integrations.airScout) {
        Form.int(strings.airscoutUdpPort, integrations.airScoutPort) { integrations.airScoutPort = it }
        Form.text(strings.airscoutServerIdentifier, integrations.airScoutServerName) {
            integrations.airScoutServerName = it
        }
        Form.text(strings.airscoutClientIdentifier, integrations.airScoutClientName) {
            integrations.airScoutClientName = it
        }
    }
    if (!integrations.gearComplete) Refusal(strings.wizardGearIncomplete)
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Form.labelledRow(label) { Text(value) }
}

@Composable
private fun SummaryPage(state: SetupWizardState, strings: Strings, saveFailed: Boolean) {
    Heading(strings.wizardSummaryTitle, strings.wizardSummaryLead)

    if (saveFailed) Refusal(strings.wizardSaveFailed)
    if (!state.mandatoryComplete) Refusal(strings.wizardMissing)
    if (state.switchedOff.isNotEmpty()) {
        Text(strings.wizardSummarySwitchedOff(state.switchedOff.joinToString(", ") { switchName(it, strings) }))
    }

    Form.section(strings.wizardSummaryPersonal) {
        SummaryRow(
            strings.guiLanguage,
            if (state.language == SYSTEM_LANGUAGE) strings.guiLanguageSystem else state.language.uppercase(),
        )
        SummaryRow(strings.wizardPersonalName, state.name.ifBlank { strings.wizardSummaryNone })
        Form.button(strings.wizardChange) { state.edit(WizardPage.PERSONAL) }
    }

    Form.section(strings.wizardSummaryLogin) {
        if (state.loginSource == WizardSource.TAKE) {
            state.loginDonor?.let { Text(strings.wizardSummaryTakenFrom(it.displayName)) }
        }
        SummaryRow(strings.stationCallsign, state.callsign.ifBlank { strings.wizardSummaryNone })
        // Never the password itself: the summary may be read over a shoulder.
        SummaryRow(
            strings.stationPassword,
            if (state.password.isEmpty()) strings.wizardSummaryNone else strings.wizardSummaryPasswordSet,
        )
        Form.button(strings.wizardChange) { state.edit(WizardPage.LOGIN) }
    }

    Form.section(strings.wizardSummaryStation) {
        if (state.stationSource == WizardSource.TAKE) {
            state.stationDonor?.let { Text(strings.wizardSummaryTakenFrom(it.displayName)) }
        }
        SummaryRow(strings.stationLocator, state.locator.ifBlank { strings.wizardSummaryNone })
        SummaryRow(
            strings.wizardSummaryBands,
            WizardBand.entries.filter { it in state.bands }.joinToString(", ") { it.label }
                .ifEmpty { strings.wizardSummaryNone },
        )
        SummaryRow(
            strings.wizardSummaryFallbackBand,
            if (state.summaryFallbackBand.isBlank()) strings.wizardSummaryNone else "${state.summaryFallbackBand} MHz",
        )
        SummaryRow(
            strings.stationMainCategory,
            state.category?.let(::categoryName) ?: strings.wizardSummaryNone,
        )
        SummaryRow(
            strings.stationSecondCategory,
            state.secondCategory?.takeIf { state.secondChat }?.let(::categoryName) ?: strings.wizardSummaryNone,
        )
        Form.button(strings.wizardChange) { state.edit(WizardPage.SITE) }
    }

    val antenna = state.antenna
    Form.section(strings.wizardSummaryAntenna) {
        SummaryRow(strings.stationBeamWidth, antenna.beamWidthDeg.toString())
        SummaryRow(strings.stationOwnAntennaGain, antenna.gainDbi.toString())
        SummaryRow(strings.stationOwnAntennaHeight, antenna.heightMeters.toString())
        SummaryRow(strings.stationOwnTxPower, antenna.txPowerWatts.toString())
        SummaryRow(strings.stationOwnHeightAsl, antenna.heightAslText.trim().ifEmpty { strings.wizardSummaryNone })
        Form.button(strings.wizardChange) { state.edit(WizardPage.ANTENNA) }
    }

    val integrations = state.integrations
    val yesNo = { on: Boolean -> if (on) strings.wizardSummaryYes else strings.wizardSummaryNo }

    Form.section(strings.wizardSummaryLog) {
        if (state.logPageEditable) {
            SummaryRow(strings.wizardSummaryLog, logProgramName(integrations.logProgram, strings))
            SummaryRow(strings.wizardSummaryQrgFollow, yesNo(state.summaryQrgFollow))
            SummaryRow(strings.wizardSummaryClusterServer, yesNo(state.summaryClusterServer))
            Form.button(strings.wizardChange) { state.edit(WizardPage.LOG) }
        } else {
            /* Several sources are on; the page cannot show that and must not be opened. */
            Text(strings.wizardSummaryLogSeveral)
        }
    }

    Form.section(strings.wizardSummaryGear) {
        SummaryRow(strings.wizardSwitchRotator, yesNo(integrations.rotator))
        SummaryRow(strings.wizardSwitchAirScout, yesNo(integrations.airScout))
        Form.button(strings.wizardChange) { state.edit(WizardPage.GEAR) }
    }
}
