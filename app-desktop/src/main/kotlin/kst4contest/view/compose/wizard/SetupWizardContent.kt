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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatCategory
import kst4contest.view.compose.Density
import kst4contest.view.compose.Form
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

    CompositionLocalProvider(LocalStrings provides strings) {
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
                        WizardPage.SUMMARY -> SummaryPage(state, strings, saveFailed)
                    }
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
                        Form.button(strings.wizardSave, enabled = state.mandatoryComplete) { onSave(false) }
                        Form.button(
                            strings.wizardSaveAndConnect,
                            enabled = state.mandatoryComplete,
                            accented = true,
                        ) { onSave(true) }
                    } else {
                        if (state.canFinish) {
                            Form.button(strings.wizardFinish, onClick = state::finish)
                        }
                        Form.button(
                            strings.wizardNext,
                            enabled = state.canGoNext,
                            accented = true,
                            onClick = state::next,
                        )
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
private fun SourceOption(label: String, selected: Boolean, onSelect: () -> Unit) {
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

    /* An axis with a single option is decided already and is not asked about. */
    if (state.loginSources.size > 1) {
        Form.section(strings.wizardEntryLogin) {
            state.loginSources.forEach { source ->
                SourceOption(
                    label = when (source) {
                        WizardSource.NEW -> strings.wizardSourceLoginNew
                        WizardSource.KEEP -> strings.wizardSourceLoginKeep(state.storedCallsign)
                    },
                    selected = state.loginSource == source,
                    onSelect = { state.chooseLoginSource(source) },
                )
            }
        }
    }

    if (state.stationSources.size > 1) {
        Form.section(strings.wizardEntryStation) {
            state.stationSources.forEach { source ->
                SourceOption(
                    label = when (source) {
                        WizardSource.NEW -> strings.wizardSourceStationNew
                        WizardSource.KEEP -> strings.wizardSourceStationKeep(state.storedLocator)
                    },
                    selected = state.stationSource == source,
                    onSelect = { state.chooseStationSource(source) },
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

@Composable
private fun SummaryRow(label: String, value: String) {
    Form.labelledRow(label) { Text(value) }
}

@Composable
private fun SummaryPage(state: SetupWizardState, strings: Strings, saveFailed: Boolean) {
    Heading(strings.wizardSummaryTitle, strings.wizardSummaryLead)

    if (saveFailed) Refusal(strings.wizardSaveFailed)
    if (!state.mandatoryComplete) Refusal(strings.wizardMissing)
    if (state.turnsOffSecondChat) Text(strings.wizardSummaryTurnedOff)
    if (state.leavesIntegrationsUntouched) Text(strings.wizardSummaryIntegrationsUnchanged)

    Form.section(strings.wizardSummaryPersonal) {
        SummaryRow(
            strings.guiLanguage,
            if (state.language == SYSTEM_LANGUAGE) strings.guiLanguageSystem else state.language.uppercase(),
        )
        SummaryRow(strings.wizardPersonalName, state.name.ifBlank { strings.wizardSummaryNone })
        Form.button(strings.wizardChange) { state.edit(WizardPage.PERSONAL) }
    }

    Form.section(strings.wizardSummaryLogin) {
        SummaryRow(strings.stationCallsign, state.callsign.ifBlank { strings.wizardSummaryNone })
        // Never the password itself: the summary may be read over a shoulder.
        SummaryRow(
            strings.stationPassword,
            if (state.password.isEmpty()) strings.wizardSummaryNone else strings.wizardSummaryPasswordSet,
        )
        Form.button(strings.wizardChange) { state.edit(WizardPage.LOGIN) }
    }

    Form.section(strings.wizardSummaryStation) {
        SummaryRow(strings.stationLocator, state.locator.ifBlank { strings.wizardSummaryNone })
        SummaryRow(
            strings.wizardSummaryBands,
            WizardBand.entries.filter { it in state.bands }.joinToString(", ") { it.label }
                .ifEmpty { strings.wizardSummaryNone },
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
}
