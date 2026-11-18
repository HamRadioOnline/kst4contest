# Einrichtungsassistent, Ausbaustufe 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ein unkonfiguriertes Operatorprofil startet in einem Einrichtungsassistenten, der Persönliches, Anmeldung, Standort und Bänder abfragt und `preferences.xml` schreibt, bevor die Laufzeit gebaut wird.

**Architecture:** Der Assistent läuft vor dem `ChatController` auf einem eigenen `ChatPreferences`-Objekt und schreibt die Datei; `startRuntime` liest sie danach wie jede gespeicherte Einstellung. Er ändert keine laufende Sitzung und berührt `SettingsDraft` nicht. Aus dem Menü wird die Laufzeit erst abgebaut, dann das selbe Profil mit dem Assistenten neu gestartet.

**Tech Stack:** Java 21 (`core`), Kotlin 2.2 und Compose Multiplatform 1.8 (`app-desktop`), JUnit 5, Gradle-Wrapper.

**Spec:** `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md` — dieser Plan setzt **nur Ausbaustufe 1** um. Die Stufen 2 (Antenne, Logprogramm, Rotor, AirScout) und 3 („von Profil X übernehmen") bekommen eigene Pläne, sobald Stufe 1 steht; ihre Schritte hängen an den Formen, die hier entstehen.

## Global Constraints

- Gradle, nicht Maven. Java 21. Bau und Tests über `./gradlew`.
- Quelltextkommentare und Javadoc ausschließlich Englisch. Kommunikation und Handbuch-Deutsch in neuer Rechtschreibung (`muss`, `dass`).
- `core` erzeugt keinen Anzeigetext und kennt kein Toolkit. Die Erkennung liegt in `core`, alles Sichtbare in `app-desktop`.
- Oberflächentexte stehen in `app-desktop/src/main/i18n/strings_en.properties` (Basis) und `strings_de.properties`. Beide Dateien bekommen jeden neuen Schlüssel. Ein Schlüssel nur in der deutschen Datei bricht den Bau.
- Kein Apostroph in den neuen Texten: parametrisierte Texte laufen durch `MessageFormat`.
- Bandbezeichnungen (`144 MHz`) und Kategorienamen sind Funkerkürzel und werden nicht übersetzt.
- Rufzeichen und Locator werden gespeichert, wie sie getippt wurden, nur ohne Leerraum am Rand. Keine neue Normalisierung.
- Nie blockierend auf dem AWT-Ereignisfaden warten. Das Fenster ist ein modaler `ComposeDialog`, geöffnet über `openOnEventThread`, wie `OperatorProfilePickerWindow`.
- Unverändert bleiben: das Format von `preferences.xml`, das Einstellungsfenster mit seinen zwölf Reitern, `SettingsDraft.apply`, `OperatorProfileManagementService`, `praktiKSTpreferences.xml`.
- Unkonfiguriert heißt: Rufzeichen leer, oder Rufzeichen `DO5AMF` mit Passwort `changeMe`.
- Pflichtangaben für „Fertig" und „Speichern": Anzeigename, Rufzeichen, Passwort, Locator, mindestens ein Band, Chatkategorie.
- Kein Commit, kein Push ohne ausdrücklichen Auftrag des Nutzers (`AGENTS.md`). Die Commit-Schritte unten gelten nur, wenn der Auftrag zur Ausführung das Committen einschließt; sonst bleiben die Änderungen im Arbeitsbaum.
- Ein Testfehler bricht den Bau, PMD und SpotBugs nicht. Ergebnis aus der Test-XML unter `*/build/test-results/` lesen, nicht aus dem Rückgabewert.

## Review Focus

1. **Rufzeichen mit Zusatz oder in Kleinschrift** (`DL0TEST/P`, `DO5AMF-2`, `do5amf`): wird angenommen und unverändert gespeichert. Test in Task 2 (`callsignsWithSuffixAndLowerCaseAreAccepted`) und Task 3 (`aCallsignIsStoredAsTyped`).
2. **Gespeicherte Chatkategorie außerhalb 1–3** (etwa 7) bei „unverändert lassen": bleibt 7, der Assistent schlägt nichts darüber. Test in Task 3 (`aKeptCategoryOutsideTheSuggestionsSurvives`).
3. **Die Datei lässt sich nicht schreiben**: der Assistent bleibt offen und meldet es, statt sich mit „gespeichert" zu schließen. Test in Task 3 (`aFailedWriteIsReported`) und Task 5 (`aFailedSaveKeepsTheWizardOpen`).
4. **Die Profildatei ist unlesbar**: der Assistent erscheint nicht und überschreibt nichts; der Start geht weiter wie heute. Test in Task 5 (`anUnreadableFileNeverOpensTheWizard`).
5. **Fenster über das X geschlossen**: zählt als „Später", schreibt nichts. Test in Task 5 (`closingTheWindowCountsAsLater`).

---

## Dateien

| Datei | Aufgabe |
|---|---|
| `core/src/main/java/kst4contest/controller/LoginConfiguration.java` (neu) | Erkennung „unkonfiguriert" und Vorlagen-Anmeldedaten |
| `core/src/test/java/kst4contest/controller/LoginConfigurationTest.java` (neu) | deren Tests, auch gegen die mitgelieferte Vorlage |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt` (neu) | Zustand, Ablauf, Prüfregeln, Schreiben der Antworten |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt` (neu) | die Seiten als Composables |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardWindow.kt` (neu) | modaler Dialog, Ergebnis, Starter |
| `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/*Test.kt` (neu) | Tests dazu |
| `app-desktop/src/main/i18n/strings_en.properties`, `strings_de.properties` | neue Texte |
| `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` | Aufruf im Start, Verbinden danach, Menüaktion |
| `app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java` | Wunsch „Assistent zeigen", Neustart des selben Profils |
| `app-desktop/src/main/java/kst4contest/view/ComposeMenuActions.java`, `.../compose/MainMenuBar.kt`, `.../compose/MainMenuModel.kt` | Menüeintrag |
| `docs/PROJECT_CONTEXT.md`, `github_docs/de-Konfiguration.md`, `github_docs/en-Configuration.md` | Dokumentation |

---

### Task 1: Erkennung eines unkonfigurierten Profils

**Files:**
- Create: `core/src/main/java/kst4contest/controller/LoginConfiguration.java`
- Test: `core/src/test/java/kst4contest/controller/LoginConfigurationTest.java`

**Interfaces:**
- Consumes: `ChatPreferences.getStn_loginCallSign()`, `getStn_loginPassword()`, `ChatPreferences.PREFERENCE_RESOURCE`, Konstruktor `ChatPreferences(Path)`.
- Produces:
  - `public static final String LoginConfiguration.TEMPLATE_CALLSIGN` (`"DO5AMF"`)
  - `public static final String LoginConfiguration.TEMPLATE_PASSWORD` (`"changeMe"`)
  - `public static boolean LoginConfiguration.carriesTemplateCredentials(ChatPreferences)`
  - `public static boolean LoginConfiguration.isUnconfigured(ChatPreferences)`

- [ ] **Step 1: Write the failing test**

```java
package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a profile counts as not set up yet. The setup wizard appears on exactly this
 * condition, so a wrong answer either nags a working station or leaves a newcomer alone
 * with the template's callsign.
 */
class LoginConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    private ChatPreferences preferences(String callsign, String password) {
        ChatPreferences preferences = new ChatPreferences(temporaryDirectory.resolve("unused.xml"));
        preferences.setStn_loginCallSign(callsign);
        preferences.setStn_loginPassword(password);
        return preferences;
    }

    @Test
    void anEmptyCallsignIsUnconfigured() {
        assertTrue(LoginConfiguration.isUnconfigured(preferences("", "secret")));
        assertTrue(LoginConfiguration.isUnconfigured(preferences("   ", "secret")));
    }

    @Test
    void theTemplateCredentialsAreUnconfigured() {
        ChatPreferences template = preferences("DO5AMF", "changeMe");

        assertTrue(LoginConfiguration.carriesTemplateCredentials(template));
        assertTrue(LoginConfiguration.isUnconfigured(template));
    }

    @Test
    void theTemplateCallsignWithItsOwnPasswordIsConfigured() {
        // The callsign belongs to a real operator; only the pair marks an untouched template.
        ChatPreferences owner = preferences("DO5AMF", "a-real-password");

        assertFalse(LoginConfiguration.carriesTemplateCredentials(owner));
        assertFalse(LoginConfiguration.isUnconfigured(owner));
    }

    @Test
    void aRealLoginIsConfigured() {
        assertFalse(LoginConfiguration.isUnconfigured(preferences("DL0TEST", "secret")));
        assertFalse(LoginConfiguration.isUnconfigured(preferences("DL0TEST/P", "")));
    }

    @Test
    void theConstantsMatchTheBundledTemplate() throws IOException {
        Path templateCopy = temporaryDirectory.resolve("template.xml");
        try (InputStream template = ChatPreferences.class
                .getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE)) {
            assertNotNull(template, "bundled resource " + ChatPreferences.PREFERENCE_RESOURCE);
            Files.copy(template, templateCopy, StandardCopyOption.REPLACE_EXISTING);
        }

        ChatPreferences fromTemplate = new ChatPreferences(templateCopy);
        assertTrue(fromTemplate.readPreferencesFromXmlFile());

        assertTrue(LoginConfiguration.carriesTemplateCredentials(fromTemplate),
                "the template's credentials changed; a fresh installation would no longer "
                        + "be recognised as unconfigured");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:test --tests 'kst4contest.controller.LoginConfigurationTest'`
Expected: Kompilierfehler, `cannot find symbol ... LoginConfiguration`.

- [ ] **Step 3: Write minimal implementation**

```java
package kst4contest.controller;

import kst4contest.model.ChatPreferences;

/**
 * Tells whether a profile has login credentials an operator entered.
 *
 * <p>Two states count as unconfigured. An empty callsign is what every created or duplicated
 * profile starts with, because {@link OperatorProfileManagementService} clears the
 * credentials on purpose. The bundled template's credentials are what the root profile of a
 * fresh installation carries. Neither can log in as the operator, so neither needs a flag of
 * its own in {@code preferences.xml}.</p>
 */
public final class LoginConfiguration {

    /** Callsign shipped in {@code praktiKSTpreferences.xml}; held equal by a test. */
    public static final String TEMPLATE_CALLSIGN = "DO5AMF";

    /** Password shipped in {@code praktiKSTpreferences.xml}; held equal by a test. */
    public static final String TEMPLATE_PASSWORD = "changeMe";

    private LoginConfiguration() {
        // Utility class.
    }

    /**
     * Whether the preferences still hold the bundled template's credentials.
     *
     * <p>Only the pair counts. The callsign alone belongs to a real operator.</p>
     *
     * @param preferences preferences of the profile to judge
     * @return true for an untouched template
     */
    public static boolean carriesTemplateCredentials(final ChatPreferences preferences) {
        return TEMPLATE_CALLSIGN.equalsIgnoreCase(trimmed(preferences.getStn_loginCallSign()))
                && TEMPLATE_PASSWORD.equals(preferences.getStn_loginPassword());
    }

    /**
     * Whether the profile has no login an operator entered.
     *
     * @param preferences preferences of the profile to judge
     * @return true when the callsign is empty or the template's credentials are still there
     */
    public static boolean isUnconfigured(final ChatPreferences preferences) {
        return trimmed(preferences.getStn_loginCallSign()).isEmpty()
                || carriesTemplateCredentials(preferences);
    }

    private static String trimmed(final String value) {
        return value == null ? "" : value.trim();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:test --tests 'kst4contest.controller.LoginConfigurationTest'`
Expected: 5 Tests, 0 Fehler in `core/build/test-results/test/TEST-kst4contest.controller.LoginConfigurationTest.xml`.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/kst4contest/controller/LoginConfiguration.java core/src/test/java/kst4contest/controller/LoginConfigurationTest.java
git commit -m "Detect operator profiles without entered login credentials"
```

---

### Task 2: Zustand und Ablauf des Assistenten

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardStateTest.kt`

**Interfaces:**
- Consumes: `LoginConfiguration.isUnconfigured`, `LoginConfiguration.carriesTemplateCredentials` (Task 1); Getter von `ChatPreferences`.
- Produces (alle in Paket `kst4contest.view.compose.wizard`):
  - `enum class WizardSource { NEW, KEEP }`
  - `enum class WizardPage { ENTRY, PERSONAL, LOGIN, SITE, SUMMARY }`
  - `enum class WizardBand(val label: String, val megahertz: Int)` mit `B50, B70, B144, B432, B1240, B2300, B3400, B5600, B10G`
  - `class SetupWizardState(prefs: ChatPreferences)` mit
    - `val loginSources: List<WizardSource>`, `val stationSources: List<WizardSource>`, `val hasEntryPage: Boolean`
    - `val loginSource: WizardSource?`, `val stationSource: WizardSource?`, `fun chooseLoginSource(WizardSource)`, `fun chooseStationSource(WizardSource)`
    - `val storedCallsign: String`, `val storedLocator: String`
    - `var language: String`, `var name: String`, `var callsign: String`, `var password: String`, `var locator: String`
    - `val bands: Set<WizardBand>`, `fun setBand(WizardBand, Boolean)`
    - `val category: Int?`, `fun chooseCategory(Int)`, `val suggestedCategory: Int?`
    - `val secondChat: Boolean`, `fun chooseSecondChat(Boolean)`, `var secondCategory: Int?`, `val secondCategoryOptions: List<Int>`
    - `val page: WizardPage`, `val pages: List<WizardPage>`
    - `val canGoNext: Boolean`, `val canGoBack: Boolean`, `val canFinish: Boolean`, `val mandatoryComplete: Boolean`
    - `val callsignRefused: Boolean`, `val locatorRefused: Boolean`
    - `fun next()`, `fun back()`, `fun finish()`, `fun edit(WizardPage)`
    - `val turnsOffSecondChat: Boolean`, `val leavesIntegrationsUntouched: Boolean`
    - `companion`: `val CATEGORY_NUMBERS: List<Int>`, `fun isPlausibleCallsign(String): Boolean`, `fun isLocator(String): Boolean`, `fun suggestCategory(Set<WizardBand>): Int?`

- [ ] **Step 1: Write the failing test**

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * The wizard's rules without a window: which pages appear, what they start with, and when
 * the operator may go on. Nothing here reads or writes a file.
 */
class SetupWizardStateTest {

    /** Preferences that never touch a file: the path is only read by read/write calls. */
    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("setup-wizard-state-test-unused.xml")).apply(configure)

    private fun freshInstallation() = prefs {
        setStn_loginCallSign("DO5AMF")
        setStn_loginPassword("changeMe")
        setStn_loginNameMainCat("KST4Contest1263")
        setStn_loginLocatorMainCat("JO54ME")
        setStn_bandActive144(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(true)
        setLoginChatCategorySecond(ChatCategory(3))
    }

    private fun duplicatedProfile() = prefs {
        setStn_loginCallSign("")
        setStn_loginPassword("")
        setStn_loginNameMainCat("Other Operator")
        setStn_loginLocatorMainCat("JO50AA")
        setStn_bandActive144(true)
        setStn_bandActive432(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(false)
    }

    private fun configuredProfile() = duplicatedProfile().apply {
        setStn_loginCallSign("DL0TEST")
        setStn_loginPassword("secret")
        setStn_loginNameMainCat("Philipp")
    }

    /** Fills every mandatory answer of a first installation, leaving the wizard on SITE. */
    private fun SetupWizardState.fillFirstInstallation() {
        name = "Philipp"
        next()
        callsign = "DL0TEST"
        password = "secret"
        next()
        locator = "JO50AA"
        setBand(WizardBand.B144, true)
    }

    @Test
    fun aFreshInstallationHasNoEntryPageAndStartsEmpty() {
        val state = SetupWizardState(freshInstallation())

        assertFalse(state.hasEntryPage)
        assertEquals(WizardPage.PERSONAL, state.page)
        assertEquals(
            listOf(WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SITE, WizardPage.SUMMARY),
            state.pages,
        )
        assertEquals("", state.name, "the template's name is nobody's name")
        assertEquals("", state.callsign)
        assertEquals("", state.locator)
        assertTrue(state.bands.isEmpty())
        assertNull(state.category)
        assertFalse(state.secondChat)
    }

    @Test
    fun aDuplicatedProfileIsAskedOnlyAboutTheStation() {
        val state = SetupWizardState(duplicatedProfile())

        assertEquals(listOf(WizardSource.NEW), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.stationSources)
        assertTrue(state.hasEntryPage)
        assertEquals(WizardPage.ENTRY, state.page)
        assertEquals(WizardSource.NEW, state.loginSource, "a single option is chosen already")
        assertNull(state.stationSource, "nothing is preselected where there is a choice")
        assertFalse(state.canGoNext)
    }

    @Test
    fun aConfiguredProfileMayKeepBoth() {
        val state = SetupWizardState(configuredProfile())

        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.stationSources)
        assertEquals("Philipp", state.name, "a configured profile keeps its operator's name")
    }

    @Test
    fun keepingTheStationSkipsItsStepAndCarriesTheStoredValues() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals(
            listOf(WizardPage.ENTRY, WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SUMMARY),
            state.pages,
        )
        assertEquals("JO50AA", state.locator)
        assertEquals(setOf(WizardBand.B144, WizardBand.B432), state.bands)
        assertEquals(2, state.category)
    }

    @Test
    fun aNewStationStartsEmptyEvenOnAProfileThatHasOne() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.NEW)
        state.next()

        assertEquals("", state.locator)
        assertTrue(state.bands.isEmpty())
        assertNull(state.category)
        assertTrue(state.pages.contains(WizardPage.SITE))
    }

    @Test
    fun goingBackToTheEntryPageAndOnAgainKeepsWhatWasTyped() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.NEW)
        state.next()
        state.name = "Philipp"
        state.back()
        state.next()

        assertEquals("Philipp", state.name)

        state.next()
        state.callsign = "DL0TEST"
        state.back()
        state.back()
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals("JO50AA", state.locator, "a changed source loads its own values")
        state.next()
        assertEquals("DL0TEST", state.callsign, "the login source did not change")
    }

    @Test
    fun callsignsWithSuffixAndLowerCaseAreAccepted() {
        listOf("DL0TEST", "DL0TEST/P", "DO5AMF-2", "do5amf", " DL0TEST ").forEach {
            assertTrue(SetupWizardState.isPlausibleCallsign(it), "'$it' must be accepted")
        }
        listOf("", "DL", "ABC", "123", "DL0 TEST", "DL0TEST|X", "DL0-TEST-2", "DL0TEST/").forEach {
            assertFalse(SetupWizardState.isPlausibleCallsign(it), "'$it' must be refused")
        }
    }

    @Test
    fun onlyASixCharacterLocatorIsAccepted() {
        listOf("JO50AA", "jo50aa", " JN49FL ").forEach {
            assertTrue(SetupWizardState.isLocator(it), "'$it' must be accepted")
        }
        listOf("", "JO50", "JO50AAA", "SS50AA", "JO50YY", "JO5OAA").forEach {
            assertFalse(SetupWizardState.isLocator(it), "'$it' must be refused")
        }
    }

    @Test
    fun theCategoryIsSuggestedFromTheLowestBand() {
        assertNull(SetupWizardState.suggestCategory(emptySet()))
        assertEquals(1, SetupWizardState.suggestCategory(setOf(WizardBand.B50, WizardBand.B144)))
        assertEquals(1, SetupWizardState.suggestCategory(setOf(WizardBand.B70)))
        assertEquals(2, SetupWizardState.suggestCategory(setOf(WizardBand.B144, WizardBand.B1240)))
        assertEquals(2, SetupWizardState.suggestCategory(setOf(WizardBand.B432)))
        assertEquals(3, SetupWizardState.suggestCategory(setOf(WizardBand.B1240, WizardBand.B10G)))
    }

    @Test
    fun theSuggestionFollowsTheBandsUntilTheOperatorChooses() {
        val state = SetupWizardState(freshInstallation())

        state.setBand(WizardBand.B1240, true)
        assertEquals(3, state.category)

        state.setBand(WizardBand.B144, true)
        assertEquals(2, state.category)

        state.chooseCategory(3)
        state.setBand(WizardBand.B50, true)
        assertEquals(3, state.category, "an operator's choice is not overwritten")
        assertEquals(1, state.suggestedCategory, "the suggestion is still shown")
    }

    @Test
    fun aSecondChatNeedsADifferentCategory() {
        val state = SetupWizardState(freshInstallation())
        state.fillFirstInstallation()
        assertTrue(state.canGoNext)

        state.chooseSecondChat(true)
        assertFalse(state.canGoNext, "the second chat has no category yet")
        assertFalse(state.secondCategoryOptions.contains(2), "the main category is not offered twice")

        state.secondCategory = 3
        assertTrue(state.canGoNext)

        state.chooseCategory(3)
        assertNull(state.secondCategory, "the same chat cannot be opened twice")
        assertFalse(state.canGoNext)
    }

    @Test
    fun finishNeedsEveryMandatoryAnswer() {
        val state = SetupWizardState(freshInstallation())

        state.name = "Philipp"
        assertFalse(state.canFinish)
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        assertFalse(state.canFinish, "a new station still lacks its locator")
        state.next()
        state.locator = "JO50AA"
        assertFalse(state.canFinish, "no band yet")
        state.setBand(WizardBand.B144, true)
        assertTrue(state.canFinish)

        state.finish()
        assertEquals(WizardPage.SUMMARY, state.page)
        assertTrue(state.mandatoryComplete)
        assertFalse(state.canFinish, "the summary has Save instead")
    }

    @Test
    fun aKeptStationLetsTheOperatorFinishAfterTheLogin() {
        val state = SetupWizardState(duplicatedProfile())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST/P"
        state.password = "secret"

        assertTrue(state.canFinish)
    }

    @Test
    fun aRefusedEntryIsOnlyShownOnceSomethingWasTyped() {
        val state = SetupWizardState(freshInstallation())

        assertFalse(state.callsignRefused, "an empty field is not an error yet")
        state.callsign = "DL"
        assertTrue(state.callsignRefused)
        state.callsign = "DL0TEST"
        assertFalse(state.callsignRefused)

        assertFalse(state.locatorRefused)
        state.locator = "JO50"
        assertTrue(state.locatorRefused)
    }

    @Test
    fun changeFromTheSummaryOpensAStepThatWasSkipped() {
        val state = SetupWizardState(configuredProfile())
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        state.edit(WizardPage.LOGIN)

        assertEquals(WizardPage.LOGIN, state.page)
        assertEquals("DL0TEST", state.callsign, "the kept value is there to be changed")
        state.callsign = "DL0TEST/P"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)
    }

    @Test
    fun theSummarySaysWhatIsSwitchedOffAndWhatIsLeftAlone() {
        val fresh = SetupWizardState(freshInstallation())
        assertTrue(fresh.turnsOffSecondChat, "the template has the second chat on, a new station has it off")
        assertTrue(fresh.leavesIntegrationsUntouched)

        val kept = SetupWizardState(duplicatedProfile())
        kept.chooseStationSource(WizardSource.KEEP)
        kept.next()
        assertFalse(kept.turnsOffSecondChat)
        assertFalse(kept.leavesIntegrationsUntouched)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.SetupWizardStateTest'`
Expected: Kompilierfehler, `Unresolved reference: SetupWizardState`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
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
            loginShown = !keep
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
            stationShown = !keep
            appliedStationSource = stationSource
        }
    }

    fun setBand(band: WizardBand, active: Boolean) {
        bands = if (active) bands + band else bands - band
        if (!categoryChosenByOperator) setCategory(suggestCategory(bands))
    }

    /** What the bands suggest, shown beside the picker whatever is chosen. */
    val suggestedCategory: Int?
        get() = suggestCategory(bands)

    fun chooseCategory(number: Int) {
        categoryChosenByOperator = true
        setCategory(number)
    }

    private fun setCategory(number: Int?) {
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
        get() = name.isNotBlank()

    private val loginComplete: Boolean
        get() = isPlausibleCallsign(callsign) && password.isNotEmpty()

    private val stationComplete: Boolean
        get() = isLocator(locator) && bands.isNotEmpty() && category != null &&
            (!secondChat || (secondCategory != null && secondCategory != category))

    /** Whether everything the profile needs to log in is there. */
    val mandatoryComplete: Boolean
        get() = personalComplete && loginComplete && stationComplete

    /** A callsign that was typed and does not look like one. An empty field is not an error yet. */
    val callsignRefused: Boolean
        get() = callsign.isNotBlank() && !isPlausibleCallsign(callsign)

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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.SetupWizardStateTest'`
Expected: 16 Tests, 0 Fehler in `app-desktop/build/test-results/test/TEST-kst4contest.view.compose.wizard.SetupWizardStateTest.xml`.

- [ ] **Step 5: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardStateTest.kt
git commit -m "Add setup wizard state and its course rules"
```

---

### Task 3: Antworten in die Profildatei schreiben

**Files:**
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt` (Methode `writeTo` vor dem `companion object` ergänzen)
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardWriteTest.kt`

**Interfaces:**
- Consumes: `SetupWizardState` aus Task 2; Setter und `writePreferencesToXmlFile()` von `ChatPreferences`.
- Produces: `fun SetupWizardState.writeTo(prefs: ChatPreferences): Boolean` — setzt die Antworten und schreibt die Datei; `false`, wenn Pflichtangaben fehlen oder das Schreiben scheitert. Bei fehlenden Pflichtangaben wird `prefs` nicht angefasst.

- [ ] **Step 1: Write the failing test**

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * What the wizard does to a real preferences file: nothing until Save, exactly the answers
 * afterwards, and every setting it never asked about left as it was.
 */
class SetupWizardWriteTest(@TempDir private val directory: Path) {

    /** A copy of the bundled template, read the way the runtime reads a profile. */
    private fun templateFile(): Path {
        val file = directory.resolve("preferences.xml")
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), file, StandardCopyOption.REPLACE_EXISTING)
        }
        return file
    }

    private fun read(file: Path): ChatPreferences =
        ChatPreferences(file).also { assertTrue(it.readPreferencesFromXmlFile(), "reading $file failed") }

    /** A profile an operator set up: read the template, change it, write it back. */
    private fun configuredFile(change: ChatPreferences.() -> Unit = {}): Path {
        val file = templateFile()
        read(file).apply {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginNameMainCat("Philipp")
            setStn_loginNameSecondCat("Philipp")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            change()
            assertTrue(writePreferencesToXmlFile())
        }
        return file
    }

    private fun SetupWizardState.answerFirstInstallation() {
        name = "Philipp"
        next()
        callsign = "DL0TEST"
        password = "secret"
        next()
        locator = "JO50AA"
        setBand(WizardBand.B144, true)
        setBand(WizardBand.B1240, true)
        finish()
    }

    @Test
    fun nothingIsWrittenBeforeSave() {
        val file = templateFile()
        val prefs = read(file)
        // Taken after the read, so only the wizard is measured here.
        val before = Files.readAllBytes(file)

        SetupWizardState(prefs).answerFirstInstallation()

        assertArrayEquals(before, Files.readAllBytes(file), "the file changed without Save")
        assertEquals("DO5AMF", prefs.getStn_loginCallSign(), "the preferences changed without Save")
    }

    @Test
    fun saveWritesExactlyTheAnswers() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.answerFirstInstallation()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals("DL0TEST", restored.getStn_loginCallSign())
        assertEquals("secret", restored.getStn_loginPassword())
        assertEquals("Philipp", restored.getStn_loginNameMainCat())
        assertEquals("JO50AA", restored.getStn_loginLocatorMainCat())
        assertTrue(restored.isStn_bandActive144())
        assertTrue(restored.isStn_bandActive1240())
        assertFalse(restored.isStn_bandActive432(), "a band the operator did not tick is off")
        assertEquals(2, restored.getLoginChatCategoryMain().categoryNumber)
        assertFalse(restored.isLoginToSecondChatEnabled(), "a new station starts without a second chat")
    }

    @Test
    fun whatTheWizardNeverAskedAboutIsLeftAsItWas() {
        val file = templateFile()
        val prefs = read(file)
        val server = prefs.getStn_on4kstServersDns()
        val rotatorPort = prefs.getStn_pstRotatorPort()
        val udpListener = prefs.isLogsynch_ucxUDPWkdCallListenerEnabled()
        val clusterServer = prefs.isNotify_dxClusterServerEnabled()
        val airScout = prefs.isAirScout_asUDPListenerEnabled()
        val beamWidth = prefs.getStn_antennaBeamWidthDeg()

        val state = SetupWizardState(prefs)
        state.answerFirstInstallation()
        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(server, restored.getStn_on4kstServersDns())
        assertEquals(rotatorPort, restored.getStn_pstRotatorPort())
        assertEquals(udpListener, restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertEquals(clusterServer, restored.isNotify_dxClusterServerEnabled())
        assertEquals(airScout, restored.isAirScout_asUDPListenerEnabled())
        assertEquals(beamWidth, restored.getStn_antennaBeamWidthDeg())
    }

    @Test
    fun aCallsignIsStoredAsTyped() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.name = "Philipp"
        state.next()
        state.callsign = " do5amf-2 "
        state.password = "secret"
        state.next()
        state.locator = " jo50aa "
        state.setBand(WizardBand.B144, true)

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals("do5amf-2", restored.getStn_loginCallSign(), "only the surrounding blanks go")
        assertEquals("jo50aa", restored.getStn_loginLocatorMainCat(), "the Station tab stores it as typed too")
    }

    @Test
    fun aKeptCategoryOutsideTheSuggestionsSurvives() {
        val file = configuredFile {
            setLoginChatCategoryMain(ChatCategory(7))
            setLoginToSecondChatEnabled(true)
            setLoginChatCategorySecond(ChatCategory(9))
        }
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(7, restored.getLoginChatCategoryMain().categoryNumber)
        assertTrue(restored.isLoginToSecondChatEnabled())
        assertEquals(9, restored.getLoginChatCategorySecond().categoryNumber)
    }

    @Test
    fun theSecondNameFollowsTheFirstUnlessItWasSetApart() {
        val following = configuredFile()
        val prefs = read(following)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Marc"
        state.next()
        assertTrue(state.writeTo(prefs))
        assertEquals("Marc", read(following).getStn_loginNameSecondCat())

        val apart = configuredFile { setStn_loginNameSecondCat("Philipp SHF") }
        val apartPrefs = read(apart)
        val apartState = SetupWizardState(apartPrefs)
        apartState.chooseLoginSource(WizardSource.KEEP)
        apartState.chooseStationSource(WizardSource.KEEP)
        apartState.next()
        apartState.name = "Marc"
        apartState.next()
        assertTrue(apartState.writeTo(apartPrefs))
        assertEquals("Philipp SHF", read(apart).getStn_loginNameSecondCat(), "a name set apart is the operator's")
    }

    @Test
    fun anUntouchedLanguageStaysEvenIfThisBuildDoesNotKnowIt() {
        val file = configuredFile { setGuiOptions_language("fr") }
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()

        assertTrue(state.writeTo(prefs))

        assertEquals("fr", read(file).getGuiOptions_language())
    }

    @Test
    fun incompleteAnswersAreNotWritten() {
        val file = templateFile()
        val prefs = read(file)
        val before = Files.readAllBytes(file)
        val state = SetupWizardState(prefs)
        state.name = "Philipp"

        assertFalse(state.writeTo(prefs))

        assertArrayEquals(before, Files.readAllBytes(file))
        assertEquals("DO5AMF", prefs.getStn_loginCallSign())
    }

    @Test
    fun aFailedWriteIsReported() {
        val source = read(templateFile())
        // A directory where the file should be: the write cannot succeed.
        val unwritable = ChatPreferences(Files.createDirectory(directory.resolve("not-a-file")))
        val state = SetupWizardState(source)
        state.answerFirstInstallation()

        assertFalse(state.writeTo(unwritable), "a write that failed must not read as saved")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.SetupWizardWriteTest'`
Expected: Kompilierfehler, `Unresolved reference: writeTo`.

- [ ] **Step 3: Write minimal implementation**

In `SetupWizardState.kt` unmittelbar vor `companion object {` einfügen:

```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.SetupWizardWriteTest'`
Expected: 9 Tests, 0 Fehler in `app-desktop/build/test-results/test/TEST-kst4contest.view.compose.wizard.SetupWizardWriteTest.xml`.

Schlägt `aFailedWriteIsReported` fehl, weil `writePreferencesToXmlFile` für ein Verzeichnis `true` liefert: **nicht** den Test abschwächen. Den Befund melden — dann meldet auch „Save settings" im Einstellungsfenster einen Erfolg, den es nicht gab, und das ist ein eigener Fehler in `ChatPreferences`.

- [ ] **Step 5: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardWriteTest.kt
git commit -m "Write setup wizard answers to the profile's preferences file"
```

---

### Task 4: Die Seiten des Assistenten

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt`
- Modify: `app-desktop/src/main/i18n/strings_en.properties`, `app-desktop/src/main/i18n/strings_de.properties` (Schlüssel am Dateiende anfügen)
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardContentScreenTest.kt`

**Interfaces:**
- Consumes: `SetupWizardState`, `WizardPage`, `WizardSource`, `WizardBand` (Task 2); `Form`, `Density`, `Kst4ContestTheme` aus `kst4contest.view.compose`; `LanguageStore`, `LocalStrings`, `Strings`, `Translations`, `SYSTEM_LANGUAGE` aus `kst4contest.view.i18n`; vorhandene Texte `stationCallsign`, `stationPassword`, `stationLocator`, `stationMainCategory`, `stationSecondCategory`, `guiLanguage`, `guiLanguageSystem`.
- Produces:
  - `@Composable fun SetupWizardContent(state: SetupWizardState, languageStore: LanguageStore, saveFailed: Boolean, onLater: () -> Unit, onSave: (connect: Boolean) -> Unit)`
  - `fun wizardLanguageStore(state: SetupWizardState): LanguageStore` — ein `LanguageStore`, dessen Wahl in `state.language` landet und nicht in den Einstellungen
  - die Textschlüssel `wizard.*` (als `strings.wizard…` erzeugt)

- [ ] **Step 1: Texte anlegen**

An das Ende von `app-desktop/src/main/i18n/strings_en.properties` anfügen:

```properties
wizard.title=Setup assistant
wizard.later=Later
wizard.back=Back
wizard.next=Next
wizard.finish=Finish
wizard.save=Save
wizard.saveAndConnect=Save and connect
wizard.change=Change
wizard.saveFailed=The settings could not be written. Nothing was changed.
wizard.missing=Some required entries are still missing. Use Change to complete them.
wizard.entry.title=Where do the entries come from?
wizard.entry.lead=Choose separately for login and station.
wizard.entry.login=Login (callsign, password)
wizard.entry.station=Station (locator, bands, chat category)
wizard.source.loginNew=Enter a new login
wizard.source.loginKeep=Leave the login unchanged ({0})
wizard.source.stationNew=Set up a new station
wizard.source.stationKeep=Leave the station unchanged (locator {0})
wizard.personal.title=Personal
wizard.personal.lead=Belongs to the operator.
wizard.personal.name=Name shown in the chat
wizard.login.title=ON4KST login
wizard.login.lead=The credentials of your ON4KST account.
wizard.login.callsignInvalid=This does not look like a callsign.
wizard.site.title=Location and bands
wizard.site.lead=Belongs to the station, not to the operator.
wizard.site.locatorInvalid=Six-character Maidenhead locator, for example JO50AA.
wizard.site.bands=Active bands
wizard.site.bandsHint=No frequency is derived from the bands.
wizard.site.categorySuggestion=Suggested from the bands: {0}
wizard.site.secondChat=Open a second chat
wizard.summary.title=Summary
wizard.summary.lead=Nothing is written until you save.
wizard.summary.personal=Personal
wizard.summary.login=Login
wizard.summary.station=Station
wizard.summary.bands=Bands
wizard.summary.turnedOff=Will be switched off: second chat.
wizard.summary.integrationsUnchanged=Log program, rotator, AirScout and DX cluster server keep their stored settings. Check them in the settings window.
wizard.summary.none=none
wizard.summary.passwordSet=set
```

An das Ende von `app-desktop/src/main/i18n/strings_de.properties` anfügen:

```properties
wizard.title=Einrichtungsassistent
wizard.later=Später
wizard.back=Zurück
wizard.next=Weiter
wizard.finish=Fertig
wizard.save=Speichern
wizard.saveAndConnect=Speichern und verbinden
wizard.change=Ändern
wizard.saveFailed=Die Einstellungen konnten nicht geschrieben werden. Es wurde nichts geändert.
wizard.missing=Es fehlen noch Pflichtangaben. Über „Ändern" ergänzen.
wizard.entry.title=Woher kommen die Angaben?
wizard.entry.lead=Für Anmeldung und Station getrennt wählen.
wizard.entry.login=Anmeldung (Rufzeichen, Passwort)
wizard.entry.station=Station (Locator, Bänder, Chatkategorie)
wizard.source.loginNew=Anmeldung neu eingeben
wizard.source.loginKeep=Anmeldung unverändert lassen ({0})
wizard.source.stationNew=Station neu einrichten
wizard.source.stationKeep=Station unverändert lassen (Locator {0})
wizard.personal.title=Persönliches
wizard.personal.lead=Gehört zum Operator.
wizard.personal.name=Anzeigename im Chat
wizard.login.title=Anmeldung bei ON4KST
wizard.login.lead=Die Zugangsdaten deines ON4KST-Kontos.
wizard.login.callsignInvalid=Das sieht nicht nach einem Rufzeichen aus.
wizard.site.title=Standort und Bänder
wizard.site.lead=Gehört zur Station, nicht zum Operator.
wizard.site.locatorInvalid=Sechsstelliger Maidenhead-Locator, zum Beispiel JO50AA.
wizard.site.bands=Aktive Bänder
wizard.site.bandsHint=Aus den Bändern wird keine Frequenz abgeleitet.
wizard.site.categorySuggestion=Vorschlag aus den Bändern: {0}
wizard.site.secondChat=Zweiten Chat öffnen
wizard.summary.title=Zusammenfassung
wizard.summary.lead=Erst „Speichern" schreibt in die Einstellungsdatei.
wizard.summary.personal=Persönliches
wizard.summary.login=Anmeldung
wizard.summary.station=Station
wizard.summary.bands=Bänder
wizard.summary.turnedOff=Wird abgeschaltet: Zweitchat.
wizard.summary.integrationsUnchanged=Logprogramm, Rotor, AirScout und DX-Cluster-Server behalten ihre gespeicherten Werte. Im Einstellungsfenster prüfen.
wizard.summary.none=keine
wizard.summary.passwordSet=gesetzt
```

Run: `./gradlew :app-desktop:compileKotlin`
Expected: BUILD SUCCESSFUL. Ein Fehler des Erzeugers nennt den Schlüssel; er prüft doppelte Schlüssel, Waisen und die Zahl der Platzhalter.

- [ ] **Step 2: Write the failing test**

```kotlin
package kst4contest.view.compose.wizard

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.i18n.LanguageStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.Locale

/**
 * The drawn wizard, clicked the way an operator clicks it. The rules live in
 * SetupWizardState and are tested there; this holds the wiring -- that a button reaches the
 * state, and that a button which must not work does not.
 */
@OptIn(ExperimentalTestApi::class)
class SetupWizardContentScreenTest {

    private fun configuredProfile() =
        ChatPreferences(Path.of("setup-wizard-screen-test-unused.xml")).apply {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginNameMainCat("Philipp")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            // Stated, not left to the compiled-in default: with a second chat and no second
            // category the summary could not be saved.
            setLoginToSecondChatEnabled(false)
        }

    private class Recorded {
        var later = 0
        val saves = mutableListOf<Boolean>()
    }

    private fun ComposeUiTest.show(state: SetupWizardState, saveFailed: Boolean = false): Recorded {
        val recorded = Recorded()
        // English, whatever language an earlier test in this JVM left behind.
        val english = LanguageStore({ "en" }, {}, Locale.ENGLISH)
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SetupWizardContent(
                    state = state,
                    languageStore = english,
                    saveFailed = saveFailed,
                    onLater = { recorded.later++ },
                    onSave = { recorded.saves += it },
                )
            }
        }
        return recorded
    }

    @Test
    fun nextDoesNothingUntilBothSourcesAreChosen() = runComposeUiTest {
        val state = SetupWizardState(configuredProfile())
        show(state)

        onNodeWithText("Next").performClick()
        assertEquals(WizardPage.ENTRY, state.page, "Next worked without a choice")

        onNodeWithText("Leave the login unchanged (DL0TEST)").performClick()
        onNodeWithText("Leave the station unchanged (locator JO50AA)").performClick()
        assertEquals(WizardSource.KEEP, state.loginSource)
        assertEquals(WizardSource.KEEP, state.stationSource)

        onNodeWithText("Next").performClick()
        assertEquals(WizardPage.PERSONAL, state.page)
    }

    @Test
    fun laterIsReportedAndNothingElse() = runComposeUiTest {
        val recorded = show(SetupWizardState(configuredProfile()))

        onNodeWithText("Later").performClick()

        assertEquals(1, recorded.later)
        assertTrue(recorded.saves.isEmpty())
    }

    @Test
    fun theSummaryOffersBothWaysOfSaving() = runComposeUiTest {
        val state = SetupWizardState(configuredProfile())
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        val recorded = show(state)

        onNodeWithText("Save").performClick()
        onNodeWithText("Save and connect").performClick()

        assertEquals(listOf(false, true), recorded.saves)
    }

    @Test
    fun aFailedSaveIsToldToTheOperator() = runComposeUiTest {
        val state = SetupWizardState(configuredProfile())
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        show(state, saveFailed = true)

        onNodeWithText("The settings could not be written. Nothing was changed.").assertExists()
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.SetupWizardContentScreenTest'`
Expected: Kompilierfehler, `Unresolved reference: SetupWizardContent`.

- [ ] **Step 4: Write minimal implementation**

```kotlin
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
 * the composition, so the language picker on the first page switches the wizard itself.
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
}

@Composable
private fun LoginPage(state: SetupWizardState, strings: Strings) {
    Heading(strings.wizardLoginTitle, strings.wizardLoginLead)

    Form.text(strings.stationCallsign, state.callsign) { state.callsign = it }
    if (state.callsignRefused) Refusal(strings.wizardLoginCallsignInvalid)
    Form.password(strings.stationPassword, state.password) { state.password = it }
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
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.SetupWizardContentScreenTest'`
Expected: 4 Tests, 0 Fehler.

Meldet der Übersetzer, dass `Density.BUTTON_GAP` oder `Form.labelledRow` nicht sichtbar ist: beide werden in `OperatorProfilePickerWindow.kt` bzw. `FormControls.kt` im Paket `kst4contest.view.compose` benutzt. Ist eines davon `internal`, bleibt es das — der Assistent liegt im selben Modul.

- [ ] **Step 6: Die Sprachtests laufen lassen**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.i18n.*'`
Expected: alle grün. `TranslationBoundaryTest` hält Deutsch vollständig; ein fehlender deutscher Schlüssel fällt hier auf.

- [ ] **Step 7: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt app-desktop/src/main/i18n/strings_en.properties app-desktop/src/main/i18n/strings_de.properties app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardContentScreenTest.kt
git commit -m "Draw the setup wizard pages"
```

---

### Task 5: Fenster und Starter

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardWindow.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardLauncherTest.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardWindowTest.kt`

**Interfaces:**
- Consumes: `SetupWizardState`, `writeTo` (Task 2, 3); `SetupWizardContent`, `wizardLanguageStore` (Task 4); `LoginConfiguration.isUnconfigured` (Task 1); `openOnEventThread`, `Kst4ContestTheme`, `WindowPlacementOnScreen.centreOnMainWindowScreen` aus `kst4contest.view.compose`; Testhelfer `kst4contest.testing.Await.awaitUntil`.
- Produces:
  - `enum class SetupWizardOutcome { NOT_SHOWN, LATER, SAVED, SAVED_AND_CONNECT }`
  - `object SetupWizardWindow { fun show(state: SetupWizardState, prefs: ChatPreferences): SetupWizardOutcome }` — blockiert, bis das Fenster zu ist
  - `object SetupWizardLauncher` mit
    - `@JvmStatic fun runIfNeeded(preferencesRelativeFileName: String, forced: Boolean): SetupWizardOutcome`
    - `internal fun run(prefs: ChatPreferences, forced: Boolean, show: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome): SetupWizardOutcome`
  - `internal fun saveFromWizard(state: SetupWizardState, prefs: ChatPreferences, connect: Boolean): SetupWizardOutcome?` — `null`, wenn das Schreiben scheiterte

- [ ] **Step 1: Write the failing tests**

`SetupWizardLauncherTest.kt`:

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** When the wizard opens at all, decided without a window. */
class SetupWizardLauncherTest(@TempDir private val directory: Path) {

    private fun templateFile(): Path {
        val file = directory.resolve("preferences.xml")
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), file, StandardCopyOption.REPLACE_EXISTING)
        }
        return file
    }

    private fun configuredFile(): Path {
        val file = templateFile()
        ChatPreferences(file).apply {
            assertTrue(readPreferencesFromXmlFile())
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            assertTrue(writePreferencesToXmlFile())
        }
        return file
    }

    private val neverShown: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome =
        { _, _ -> throw AssertionError("the wizard must not open here") }

    @Test
    fun anUnconfiguredProfileOpensTheWizard() {
        var shown = 0

        val outcome = SetupWizardLauncher.run(ChatPreferences(templateFile()), forced = false) { _, _ ->
            shown++
            SetupWizardOutcome.LATER
        }

        assertEquals(1, shown)
        assertEquals(SetupWizardOutcome.LATER, outcome)
    }

    @Test
    fun aConfiguredProfileStartsWithoutTheWizard() {
        assertEquals(
            SetupWizardOutcome.NOT_SHOWN,
            SetupWizardLauncher.run(ChatPreferences(configuredFile()), forced = false, neverShown),
        )
    }

    @Test
    fun theMenuOpensItOnAConfiguredProfileToo() {
        val outcome = SetupWizardLauncher.run(ChatPreferences(configuredFile()), forced = true) { state, _ ->
            assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.loginSources)
            SetupWizardOutcome.SAVED
        }

        assertEquals(SetupWizardOutcome.SAVED, outcome)
    }

    @Test
    fun anUnreadableFileNeverOpensTheWizard() {
        val garbage = directory.resolve("broken.xml")
        Files.writeString(garbage, "this is not xml <<<")
        val before = Files.readAllBytes(garbage)

        // Forced as well: a wizard over a file it could not read would save over it.
        assertEquals(
            SetupWizardOutcome.NOT_SHOWN,
            SetupWizardLauncher.run(ChatPreferences(garbage), forced = true, neverShown),
        )
        assertArrayEquals(before, Files.readAllBytes(garbage), "the unreadable file was touched")
    }

    @Test
    fun aFailedSaveKeepsTheWizardOpen() {
        val source = ChatPreferences(templateFile()).apply { assertTrue(readPreferencesFromXmlFile()) }
        val state = SetupWizardState(source).apply {
            name = "Philipp"
            next()
            callsign = "DL0TEST"
            password = "secret"
            next()
            locator = "JO50AA"
            setBand(WizardBand.B144, true)
        }
        val unwritable = ChatPreferences(Files.createDirectory(directory.resolve("not-a-file")))

        assertNull(saveFromWizard(state, unwritable, connect = true), "a failed write must not close the wizard")
    }

    @Test
    fun aSaveReportsWhetherToConnect() {
        val file = templateFile()
        val prefs = ChatPreferences(file).apply { assertTrue(readPreferencesFromXmlFile()) }
        val state = SetupWizardState(prefs).apply {
            name = "Philipp"
            next()
            callsign = "DL0TEST"
            password = "secret"
            next()
            locator = "JO50AA"
            setBand(WizardBand.B144, true)
        }

        assertEquals(SetupWizardOutcome.SAVED, saveFromWizard(state, prefs, connect = false))
        assertEquals(SetupWizardOutcome.SAVED_AND_CONNECT, saveFromWizard(state, prefs, connect = true))
    }
}
```

`SetupWizardWindowTest.kt`:

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatPreferences
import kst4contest.testing.Await.awaitUntil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * The wizard's dialog must open from both threads that open it: `main` at startup, and the
 * event thread when the menu rebuilds the runtime. A latch-based wait deadlocked the profile
 * picker in exactly the second case; see OperatorProfilePickerWindow.
 */
class SetupWizardWindowTest {

    private fun prefs() = ChatPreferences(Path.of("setup-wizard-window-test-unused.xml"))

    private fun showingDialog(): Window? =
        Window.getWindows().firstOrNull { it.isShowing && it.javaClass.name.contains("ComposeDialog") }

    private val closer = Executors.newSingleThreadExecutor()

    /** Closes the dialog the way its X does, as soon as it is on screen. */
    private fun disposeOnceShowing(): Future<*> =
        closer.submit {
            awaitUntil(Duration.ofSeconds(10), "the wizard's dialog never appeared") {
                showingDialog() != null
            }
            EventQueue.invokeLater { showingDialog()?.dispose() }
        }

    @Test
    fun closingTheWindowCountsAsLater() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        try {
            val closing = disposeOnceShowing()
            val prefs = prefs()

            val outcome = SetupWizardWindow.show(SetupWizardState(prefs), prefs)

            assertEquals(SetupWizardOutcome.LATER, outcome)
            assertEquals("", prefs.getStn_loginCallSign(), "closing the window wrote something")
            closing.get(1, TimeUnit.SECONDS)
        } finally {
            closer.shutdownNow()
        }
    }

    @Test
    fun opensAndReturnsWhenCalledOnTheEventThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val worker = Executors.newSingleThreadExecutor()
        try {
            val closing = disposeOnceShowing()
            val prefs = prefs()
            val call = worker.submit {
                EventQueue.invokeAndWait { SetupWizardWindow.show(SetupWizardState(prefs), prefs) }
            }

            // A deadlock here has no timeout of its own: without one this never completes.
            call.get(20, TimeUnit.SECONDS)
            closing.get(1, TimeUnit.SECONDS)
        } finally {
            worker.shutdownNow()
            closer.shutdownNow()
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.SetupWizardLauncherTest' --tests 'kst4contest.view.compose.wizard.SetupWizardWindowTest'`
Expected: Kompilierfehler, `Unresolved reference: SetupWizardLauncher`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package kst4contest.view.compose.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeDialog
import kst4contest.controller.LoginConfiguration
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.compose.WindowPlacementOnScreen
import kst4contest.view.compose.openOnEventThread
import java.awt.Dialog
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicReference
import javax.swing.WindowConstants

/** How a run of the setup wizard ended. */
enum class SetupWizardOutcome {
    /** The profile is set up, or its file could not be read: the wizard did not open. */
    NOT_SHOWN,

    /** Closed without saving; the file is as it was. */
    LATER,

    /** The answers are in the file. */
    SAVED,

    /** The answers are in the file and the operator wants the session started. */
    SAVED_AND_CONNECT,
}

/**
 * Writes the answers and says how the wizard ends.
 *
 * @return the outcome to close with, or null when the file could not be written -- the
 *         wizard then stays open, because "saved" would be a lie
 */
internal fun saveFromWizard(
    state: SetupWizardState,
    prefs: ChatPreferences,
    connect: Boolean,
): SetupWizardOutcome? =
    when {
        !state.writeTo(prefs) -> null
        connect -> SetupWizardOutcome.SAVED_AND_CONNECT
        else -> SetupWizardOutcome.SAVED
    }

/**
 * The setup wizard's window.
 *
 * A modal [ComposeDialog] opened through [openOnEventThread], the construction
 * OperatorProfilePickerWindow uses and for the reason given there: a wait that does not
 * pump events deadlocks once the caller is the event thread, and this one is called from
 * `main` at startup and from the event thread when the menu rebuilds the runtime.
 */
object SetupWizardWindow {

    /** Runs before a profile's runtime exists, so there is no configured size to read. */
    private const val DIALOG_FONT_SIZE_SP = 12f

    private val DIALOG_SIZE = Dimension(640, 620)

    /**
     * Opens the wizard and blocks until it is closed.
     *
     * @param prefs the profile's preferences; written only by Save
     * @return never [SetupWizardOutcome.NOT_SHOWN]
     */
    fun show(state: SetupWizardState, prefs: ChatPreferences): SetupWizardOutcome {
        /* The window's X leaves this untouched: closing is "later". */
        val outcome = AtomicReference(SetupWizardOutcome.LATER)

        openOnEventThread { open(state, prefs, outcome) }

        return outcome.get()
    }

    private fun open(
        state: SetupWizardState,
        prefs: ChatPreferences,
        outcome: AtomicReference<SetupWizardOutcome>,
    ) {
        val languageStore = wizardLanguageStore(state)
        var saveFailed by mutableStateOf(false)

        val dialog = ComposeDialog(owner = null, modalityType = Dialog.ModalityType.APPLICATION_MODAL)
        dialog.title = languageStore.strings.wizardTitle
        dialog.size = DIALOG_SIZE
        WindowPlacementOnScreen.centreOnMainWindowScreen(dialog)
        /* Disposed and not hidden: a hidden dialog keeps its window and Skia layer alive. */
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        dialog.setContent {
            Kst4ContestTheme(
                darkMode = prefs.isGUI_darkModeActiveByDefault(),
                baseFontSizeSp = DIALOG_FONT_SIZE_SP,
            ) {
                SetupWizardContent(
                    state = state,
                    languageStore = languageStore,
                    saveFailed = saveFailed,
                    onLater = { dialog.dispose() },
                    onSave = { connect ->
                        val saved = saveFromWizard(state, prefs, connect)
                        if (saved == null) {
                            saveFailed = true
                        } else {
                            outcome.set(saved)
                            dialog.dispose()
                        }
                    },
                )
            }
        }

        dialog.isVisible = true
    }
}

/** Decides whether the wizard opens for a profile, and opens it. */
object SetupWizardLauncher {

    /**
     * Opens the wizard when the profile needs it, before the profile's runtime is built.
     *
     * @param preferencesRelativeFileName the profile's preferences file below the
     *        application directory; a missing one is seeded from the bundled template, as
     *        the runtime itself would do
     * @param forced the operator asked for the wizard from the menu
     */
    @JvmStatic
    fun runIfNeeded(preferencesRelativeFileName: String, forced: Boolean): SetupWizardOutcome =
        run(ChatPreferences(preferencesRelativeFileName), forced, SetupWizardWindow::show)

    /**
     * @param prefs bound to the profile's file and not read yet
     * @param show opens the window; replaced in the tests
     */
    internal fun run(
        prefs: ChatPreferences,
        forced: Boolean,
        show: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome,
    ): SetupWizardOutcome {
        /*
         * A file that cannot be read is not a profile to set up: the wizard would start from
         * compiled-in defaults and Save would write them over whatever the file holds. The
         * runtime deals with such a file as it always did.
         */
        if (!prefs.readPreferencesFromXmlFile()) return SetupWizardOutcome.NOT_SHOWN

        if (!forced && !LoginConfiguration.isUnconfigured(prefs)) return SetupWizardOutcome.NOT_SHOWN

        return show(SetupWizardState(prefs), prefs)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.*'`
Expected: alle Tests der vier Klassen grün; `SetupWizardWindowTest` läuft nur mit Bildschirm und wird sonst übersprungen (`skipped` in der XML, kein Fehler).

Liefert `readPreferencesFromXmlFile` für die kaputte Datei `true`: den Test nicht abschwächen, den Befund melden. Dann liest `ChatPreferences` Müll als gültig, und das betrifft auch den normalen Start.

- [ ] **Step 5: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardWindow.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardLauncherTest.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardWindowTest.kt
git commit -m "Open the setup wizard as a modal dialog"
```

---

### Task 6: Der Assistent im Programmstart

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java`
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` (Methode `startRuntime`, ab Zeile 2174)
- Test: `app-desktop/src/test/java/kst4contest/view/ApplicationRuntimeLauncherSetupWizardTest.java`

**Interfaces:**
- Consumes: `SetupWizardLauncher.runIfNeeded(String, boolean)`, `SetupWizardOutcome` (Task 5); `ActiveOperatorProfile.get().getPreferencesRelativeFileName()`; `Kst4ContestApplication.menuActionConnect()`.
- Produces:
  - `static void ApplicationRuntimeLauncher.requestSetupWizard()` (paketsichtbar)
  - `static boolean ApplicationRuntimeLauncher.consumeSetupWizardRequest()` (paketsichtbar) — liefert den Wunsch genau einmal

- [ ] **Step 1: Write the failing test**

```java
package kst4contest.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The request to show the setup wizard on the next runtime start is honoured exactly once.
 * A request that stuck would open the wizard on every later profile switch.
 */
class ApplicationRuntimeLauncherSetupWizardTest {

    @Test
    void aRequestIsHonouredOnce() {
        assertFalse(ApplicationRuntimeLauncher.consumeSetupWizardRequest(), "nothing was requested");

        ApplicationRuntimeLauncher.requestSetupWizard();

        assertTrue(ApplicationRuntimeLauncher.consumeSetupWizardRequest());
        assertFalse(ApplicationRuntimeLauncher.consumeSetupWizardRequest(), "the request must not stick");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.ApplicationRuntimeLauncherSetupWizardTest'`
Expected: Kompilierfehler, `cannot find symbol ... consumeSetupWizardRequest`.

- [ ] **Step 3: Den Wunsch in `ApplicationRuntimeLauncher` anlegen**

Nach dem Feld `private static Kst4ContestApplication currentRuntime;` einfügen:

```java
    /**
     * Whether the next runtime start shows the setup wizard although the profile is set up.
     * Set by the menu entry and cleared by the start that honours it.
     */
    private static final java.util.concurrent.atomic.AtomicBoolean SETUP_WIZARD_REQUESTED =
            new java.util.concurrent.atomic.AtomicBoolean(false);
```

Vor `public static boolean switchProfile(` einfügen:

```java
    /** Asks the next runtime start to show the setup wizard. */
    static void requestSetupWizard() {
        SETUP_WIZARD_REQUESTED.set(true);
    }

    /**
     * Tells a starting runtime whether the wizard was asked for, and forgets the request.
     *
     * @return true exactly once per request
     */
    static boolean consumeSetupWizardRequest() {
        return SETUP_WIZARD_REQUESTED.getAndSet(false);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.ApplicationRuntimeLauncherSetupWizardTest'`
Expected: 1 Test, 0 Fehler.

- [ ] **Step 5: Den Assistenten in `startRuntime` aufrufen**

In `Kst4ContestApplication.java` die Importe ergänzen:

```java
import kst4contest.view.compose.wizard.SetupWizardLauncher;
import kst4contest.view.compose.wizard.SetupWizardOutcome;
```

In `startRuntime` den Anfang ersetzen. Alt:

```java
		if (!resolveOperatorProfileIfRequired()) {
			return;
		}

		ApplicationRuntimeLauncher.setCurrent(this);
```

Neu:

```java
		if (!resolveOperatorProfileIfRequired()) {
			return;
		}

		/*
		 * The setup wizard, before anything of this profile's runtime exists. It writes
		 * preferences.xml and the ChatController built below reads it like any stored
		 * setting, so there is no running session to bring up to date and no reload.
		 * Blocks until the wizard is closed; see SetupWizardWindow for why that is safe on
		 * either thread this method is called from.
		 */
		SetupWizardOutcome setupWizardOutcome = SetupWizardLauncher.runIfNeeded(
				ActiveOperatorProfile.get().getPreferencesRelativeFileName(),
				ApplicationRuntimeLauncher.consumeSetupWizardRequest());

		ApplicationRuntimeLauncher.setCurrent(this);
```

Am Ende von `startRuntime`, unmittelbar vor dem Kommentar `Replaces Application.stop()` und dem `Runtime.getRuntime().addShutdownHook(`, einfügen:

```java
		/*
		 * "Save and connect" in the setup wizard. Here and not earlier: connecting needs the
		 * runtime this method has just finished building. The same path as File -> Connect,
		 * so a refused or failed connection is reported the way it is there.
		 */
		if (setupWizardOutcome == SetupWizardOutcome.SAVED_AND_CONNECT) {
			uiDispatcher.runOnUi(this::menuActionConnect);
		}

```

Die Spec nennt hier `connectFromSettings`. `menuActionConnect` ruft dasselbe `chatcontroller.execute()` und zeigt einen Fehlschlag selbst an; `connectFromSettings` gibt nur einen Text zurück, den das Einstellungsfenster anzeigt, und das ist zu diesem Zeitpunkt nicht der Aufrufer.

- [ ] **Step 6: Bauen und die berührten Tests laufen lassen**

Run: `./gradlew :app-desktop:compileJava :app-desktop:test --tests 'kst4contest.view.*'`
Expected: BUILD SUCCESSFUL; in `app-desktop/build/test-results/test/` kein `<failure` und kein `<error`.

- [ ] **Step 7: Von Hand prüfen**

Die eigenen Einstellungen bleiben unberührt, weil ein neues leeres Profil benutzt wird.

1. `./gradlew :app-desktop:run`
2. Einstellungsfenster → Reiter „Profiles" → ein Profil `Assistent-Test` anlegen, dorthin wechseln.
3. Erwartet: der Einrichtungsassistent erscheint, **bevor** Haupt- und Einstellungsfenster des neuen Profils da sind. Die Einstiegsseite fragt nur nach der Station (zwei Wahlmöglichkeiten); die Anmeldung ist nicht wählbar.
4. „Später" drücken. Erwartet: Hauptfenster und Einstellungsfenster öffnen sich wie immer; Reiter „Station" zeigt ein leeres Rufzeichen.
5. Zurück zum eigenen Profil wechseln, dann wieder zu `Assistent-Test`. Erwartet: der Assistent erscheint erneut.
6. Diesmal ausfüllen und „Speichern". Erwartet: Reiter „Station" zeigt Rufzeichen, Locator, Bänder und Kategorie aus dem Assistenten.
7. Profil `Assistent-Test` wieder löschen.

Ergebnis jedes Punkts notieren. Ein grüner Bau sagt über Fadenzugehörigkeit nichts (`docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`); bleibt die Oberfläche in Schritt 3 stehen, ist das ein Verklemmen und kein Darstellungsfehler.

- [ ] **Step 8: Commit**

```bash
git add app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java app-desktop/src/test/java/kst4contest/view/ApplicationRuntimeLauncherSetupWizardTest.java
git commit -m "Run the setup wizard before an unconfigured profile's runtime is built"
```

---

### Task 7: Menüeintrag „Einrichtungsassistent"

**Files:**
- Modify: `app-desktop/src/main/i18n/strings_en.properties`, `strings_de.properties`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuBar.kt` (Interface `MainMenuActions`)
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuModel.kt` (Menü „Options")
- Modify: `app-desktop/src/main/java/kst4contest/view/ComposeMenuActions.java`
- Modify: `app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java`
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/MainMenuModelTest.kt`, `MainMenuRowScreenTest.kt`

**Interfaces:**
- Consumes: `ApplicationRuntimeLauncher.requestSetupWizard()`, `switchProfile(OperatorProfile)` (vorhanden); `SettingsWindow.settleThen(Runnable)`; `ComposeAlert.confirm(String, String, String, String, String, boolean)`.
- Produces:
  - `fun MainMenuActions.openSetupWizard()`
  - `public static boolean ApplicationRuntimeLauncher.rerunSetupWizard()`
  - `void Kst4ContestApplication.menuActionOpenSetupWizard()`
  - Textschlüssel `menu.setupWizard`, `alert.setupWizardTitle`, `alert.setupWizardQuestion`, `alert.setupWizardBody`, `dialog.startSetupWizard`

- [ ] **Step 1: Texte anlegen**

An `strings_en.properties` anfügen:

```properties
menu.setupWizard=Setup assistant...
alert.setupWizardTitle=Setup assistant
alert.setupWizardQuestion=End the session and start the setup assistant?
alert.setupWizardBody=The ON4KST connection is closed and all windows are rebuilt after the assistant. For single changes during operation use the settings window.
dialog.startSetupWizard=Start assistant
```

An `strings_de.properties` anfügen:

```properties
menu.setupWizard=Einrichtungsassistent...
alert.setupWizardTitle=Einrichtungsassistent
alert.setupWizardQuestion=Sitzung beenden und den Einrichtungsassistenten starten?
alert.setupWizardBody=Die ON4KST-Verbindung wird geschlossen, und alle Fenster werden nach dem Assistenten neu aufgebaut. Für einzelne Änderungen im laufenden Betrieb ist das Einstellungsfenster da.
dialog.startSetupWizard=Assistent starten
```

- [ ] **Step 2: Write the failing tests**

In `MainMenuModelTest.kt` in `RecordingActions` nach `toggleSettingsWindow` ergänzen:

```kotlin
        override fun openSetupWizard() { calls += "openSetupWizard" }
```

und am Ende der Klasse einen Test:

```kotlin
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
```

In `MainMenuRowScreenTest.kt` in `RecordingActions` nach `toggleSettingsWindow` ergänzen:

```kotlin
        override fun openSetupWizard() { calls += "openSetupWizard" }
```

und in der Liste `expected` von `everyItemPickedInTheRowRunsItsOwnAction` nach der Zeile `Triple("Options", "Show options", "toggleSettingsWindow"),` einfügen:

```kotlin
            Triple("Options", "Setup assistant...", "openSetupWizard"),
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.MainMenuModelTest' --tests 'kst4contest.view.compose.MainMenuRowScreenTest'`
Expected: Kompilierfehler, `'openSetupWizard' overrides nothing`.

- [ ] **Step 4: Menü erweitern**

In `MainMenuBar.kt` im Interface `MainMenuActions` nach `fun toggleSettingsWindow()` einfügen:

```kotlin
    /** Ends the session after asking and runs the setup wizard for the active profile. */
    fun openSetupWizard()
```

In `MainMenuModel.kt` im Menü `strings.menuOptions` nach dem `MenuEntry.Item` für `actions::toggleSettingsWindow` einfügen:

```kotlin
            /*
             * Always usable, like the profile switch: it rebuilds the runtime and asks before
             * it gives the session up.
             */
            MenuEntry.Item(strings.menuSetupWizard, state.canSwitchProfile, actions::openSetupWizard),
```

In `ComposeMenuActions.java` nach der Methode `toggleSettingsWindow` einfügen:

```java
    @Override
    public void openSetupWizard() {
        application.runOnUi(application::menuActionOpenSetupWizard);
    }
```

- [ ] **Step 5: Neustart des selben Profils**

In `ApplicationRuntimeLauncher.java` nach `consumeSetupWizardRequest()` einfügen:

```java
    /**
     * Rebuilds the runtime of the active profile with the setup wizard in front of it.
     *
     * <p>The running runtime is torn down first and the wizard shown afterwards, not the
     * other way round: a runtime shutting down flushes its pending layout write, and that
     * must not land on the file the wizard has just written.</p>
     *
     * @return true if the new runtime was built
     */
    public static boolean rerunSetupWizard() {
        OperatorProfileSelection active = ActiveOperatorProfile.get();

        if (active == null) {
            return false;
        }

        requestSetupWizard();
        return switchProfile(active.getProfile());
    }
```

und die Importe ergänzen, soweit sie fehlen:

```java
import kst4contest.controller.ActiveOperatorProfile;
import kst4contest.model.OperatorProfileSelection;
```

- [ ] **Step 6: Menüaktion in `Kst4ContestApplication`**

Nach der Methode `menuActionToggleSettingsWindow()` einfügen:

```java
	/**
	 * Options -> setup assistant. Rebuilds the runtime of the active profile with the wizard
	 * in front of it, which costs the running session; hence the question, and hence the
	 * settings edits are settled first, as before a profile switch.
	 */
	void menuActionOpenSetupWizard() {
		SettingsWindow.settleThen(() -> {
			boolean confirmed = ComposeAlert.confirm(
					kst4contest.view.i18n.CurrentStrings.get().getAlertSetupWizardTitle(),
					kst4contest.view.i18n.CurrentStrings.get().getAlertSetupWizardQuestion(),
					kst4contest.view.i18n.CurrentStrings.get().getAlertSetupWizardBody(),
					kst4contest.view.i18n.CurrentStrings.get().getDialogStartSetupWizard(),
					kst4contest.view.i18n.CurrentStrings.get().getDialogCancel(),
					chatcontroller.getChatPreferences().isGUI_darkModeActive());

			if (confirmed) {
				ApplicationRuntimeLauncher.rerunSetupWizard();
			}
		});
	}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.MainMenuModelTest' --tests 'kst4contest.view.compose.MainMenuRowScreenTest' --tests 'kst4contest.view.i18n.*'`
Expected: alle grün.

- [ ] **Step 8: Von Hand prüfen**

1. `./gradlew :app-desktop:run`, mit dem eigenen Profil verbinden.
2. Im Einstellungsfenster einen Wert ändern, **nicht** übernehmen. „Options → Setup assistant..." wählen.
3. Erwartet: zuerst die Frage des Einstellungsfensters nach den offenen Änderungen; „Zurück" bricht alles ab, die Sitzung steht noch.
4. Erneut wählen, Änderungen verwerfen. Erwartet: die Rückfrage „Sitzung beenden …". „Abbrechen" lässt die Sitzung stehen.
5. Erneut, „Assistent starten". Erwartet: alle Fenster schließen, der Assistent erscheint, die Einstiegsseite bietet für Anmeldung und Station je „neu" und „unverändert".
6. Beides „unverändert", bis zur Zusammenfassung, „Speichern". Erwartet: das Programm startet neu; Rufzeichen, Locator, Bänder, Fensteranordnung und alle Reiter wie vorher.
7. `~/.praktiKST/preferences.xml` vor Schritt 5 kopieren und nach Schritt 6 vergleichen (`diff`). Erwartet: kein Unterschied in fachlichen Werten.

Schritt 7 prüft die Annahme der Spec, dass die Laufzeit beim Abbau nicht über die Datei des Assistenten schreibt. Zeigt der Vergleich einen Verlust, das melden und nicht weitermachen.

- [ ] **Step 9: Commit**

```bash
git add app-desktop/src/main/i18n/strings_en.properties app-desktop/src/main/i18n/strings_de.properties app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuBar.kt app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuModel.kt app-desktop/src/main/java/kst4contest/view/ComposeMenuActions.java app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java app-desktop/src/test/kotlin/kst4contest/view/compose/MainMenuModelTest.kt app-desktop/src/test/kotlin/kst4contest/view/compose/MainMenuRowScreenTest.kt
git commit -m "Reach the setup wizard from the Options menu"
```

---

### Task 8: Dokumentation und Abschluss

**Files:**
- Modify: `docs/PROJECT_CONTEXT.md` (Abschnitt „Important Decisions and Workarounds", ein Punkt am Ende der Liste)
- Modify: `github_docs/de-Konfiguration.md`, `github_docs/en-Configuration.md` (neuer Abschnitt vor „Station Settings")
- Modify: `app-desktop/src/main/i18n/strings_en.properties`, `strings_de.properties` (Schlüssel `profiles.createdWithoutCredentials`)
- Delete: `docs/superpowers/plans/2026-11-18-einrichtungsassistent-stufe-1.md`

**Interfaces:**
- Consumes: das fertige Verhalten aus Task 1 bis 7.
- Produces: nichts, worauf Code aufbaut.

- [ ] **Step 1: Der Hinweis beim Anlegen eines Profils**

Der Text schickt den Nutzer heute in den Reiter „Station". Nach dem Wechsel erscheint jetzt der Assistent.

In `strings_de.properties` die Zeile `profiles.createdWithoutCredentials=…` ersetzen durch:

```properties
profiles.createdWithoutCredentials=Das Profil „{0}" wurde ohne Rufzeichen und Passwort angelegt. Nach dem Wechsel zu diesem Profil fragt der Einrichtungsassistent beides ab.
```

In `strings_en.properties` die Zeile mit demselben Schlüssel ersetzen durch:

```properties
profiles.createdWithoutCredentials=The profile "{0}" was created without callsign and password. After switching to it, the setup assistant asks for both.
```

Run: `grep -rn 'profilesCreatedWithoutCredentials\|createdWithoutCredentials' app-desktop/src/test`
Jeden Test, der den alten Wortlaut festhält, auf den neuen ändern.

- [ ] **Step 2: `docs/PROJECT_CONTEXT.md`**

Am Ende der Liste unter „Important Decisions and Workarounds" anfügen:

```markdown
- **The setup wizard writes `preferences.xml` and does not touch a running session.** It runs
  in `startRuntime` after the profile is resolved and before the `ChatController` exists, on
  a `ChatPreferences` of its own, so what it stores is read like any stored setting. That is
  why it needs neither `SettingsDraft` nor the tab states: there is no session to bring up to
  date. From the menu the runtime is torn down first and the same profile restarted
  (`ApplicationRuntimeLauncher.rerunSetupWizard`), which costs the session and asks first. A
  profile counts as unconfigured when its callsign is empty or it still carries the bundled
  template's credentials (`LoginConfiguration`); there is no flag for it in the file. Stage 1
  asks for name, login, locator, bands and chat category; the reasons and the later stages
  are in `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md`.
```

- [ ] **Step 3: Handbuch, deutsch**

In `github_docs/de-Konfiguration.md` den ersten Absatz nach der Sprachzeile ersetzen. Alt:

```markdown
Nach dem ersten Start öffnet sich das **Einstellungsfenster** – dieses ist der zentrale Ausgangspunkt für alle Konfigurationen. Es empfiehlt sich, das Einstellungsfenster während des Betriebs geöffnet zu lassen (z. B. um den Beacon schnell ein- und auszuschalten).
```

Neu:

```markdown
Beim ersten Start fragt der **Einrichtungsassistent** das Nötigste ab (siehe unten). Danach öffnet sich das **Einstellungsfenster** – dieses ist der zentrale Ausgangspunkt für alle Konfigurationen. Es empfiehlt sich, das Einstellungsfenster während des Betriebs geöffnet zu lassen (z. B. um den Beacon schnell ein- und auszuschalten).
```

Unmittelbar vor `## Station Settings (Stationseinstellungen)` einfügen:

```markdown
## Einrichtungsassistent (ab v1.51)

Solange ein Profil kein eigenes Rufzeichen hat, startet es mit dem Einrichtungsassistenten. Das gilt für die erste Installation und für jedes neu angelegte oder duplizierte Operator-Profil.

Der Assistent fragt:

1. **Persönliches** – Sprache und Anzeigename im Chat.
2. **Anmeldung** – Rufzeichen und Passwort des ON4KST-Kontos.
3. **Standort und Bänder** – Locator, aktive Bänder und Chatkategorie. Die Kategorie wird aus den Bändern vorgeschlagen und lässt sich ändern. Ein zweiter Chat ist freiwillig.
4. **Zusammenfassung** – erst **Speichern** schreibt die Einstellungen. **Speichern und verbinden** meldet danach gleich am Chat an.

Hat das Profil schon eine Station (etwa nach dem Duplizieren), fragt die erste Seite, ob sie **unverändert** bleiben oder **neu eingerichtet** werden soll. So braucht ein weiterer Operator an derselben Station nur Namen und Anmeldung einzutragen.

**Später** schließt den Assistenten, ohne etwas zu schreiben. Er erscheint beim nächsten Start des Profils wieder, solange das Rufzeichen fehlt.

Über **Options → Einrichtungsassistent...** lässt er sich jederzeit erneut aufrufen. Dabei wird die laufende Sitzung nach einer Rückfrage beendet und das Profil neu gestartet. Für einzelne Änderungen im Betrieb ist das Einstellungsfenster gedacht.

Logprogramm, Rotor, AirScout und DX-Cluster-Server fragt der Assistent noch nicht ab; sie behalten ihre Werte und werden wie bisher im Einstellungsfenster eingestellt.

---

```

Die Versionsangabe „ab v1.51" gegen `gradle.properties` prüfen und auf die Version setzen, in der die Änderung erscheint. Kein Versionssprung in `gradle.properties` selbst.

- [ ] **Step 4: Handbuch, englisch**

In `github_docs/en-Configuration.md` den ersten Absatz ersetzen. Alt:

```markdown
After the first start, the **settings window** opens – this is the central starting point for all configuration. It is recommended to keep the settings window open during operation (e.g. to quickly toggle the beacon on and off).
```

Neu:

```markdown
On the first start, the **setup assistant** asks for the essentials (see below). After that the **settings window** opens – this is the central starting point for all configuration. It is recommended to keep the settings window open during operation (e.g. to quickly toggle the beacon on and off).
```

Unmittelbar vor `## Station Settings` einfügen:

```markdown
## Setup Assistant (since v1.51)

As long as a profile has no callsign of its own, it starts with the setup assistant. This applies to the first installation and to every newly created or duplicated operator profile.

The assistant asks for:

1. **Personal** – language and the name shown in the chat.
2. **Login** – callsign and password of the ON4KST account.
3. **Location and bands** – locator, active bands and chat category. The category is suggested from the bands and can be changed. A second chat is optional.
4. **Summary** – nothing is written until you click **Save**. **Save and connect** logs into the chat right afterwards.

If the profile already has a station (after duplicating one, for example), the first page asks whether to leave it **unchanged** or to **set it up anew**. A further operator at the same station then only enters a name and a login.

**Later** closes the assistant without writing anything. It appears again on the next start of the profile as long as the callsign is missing.

**Options → Setup assistant...** opens it again at any time. This ends the running session after a confirmation and restarts the profile. Use the settings window for single changes during operation.

The assistant does not ask about log program, rotator, AirScout and DX cluster server yet; they keep their values and are configured in the settings window as before.

---

```

- [ ] **Step 5: Der ganze Bau**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL.

Danach, weil der Rückgabewert allein nichts beweist:

```bash
grep -l '<failure\|<error' core/build/test-results/test/*.xml app-desktop/build/test-results/test/*.xml i18n-generator/build/test-results/test/*.xml
```

Expected: keine Ausgabe.

```bash
grep -c '<violation' app-desktop/build/reports/pmd/main.xml core/build/reports/pmd/main.xml
grep -c '<BugInstance' app-desktop/build/reports/spotbugs/main.xml core/build/reports/spotbugs/main.xml
```

Die vier Zahlen notieren und mit `main` vergleichen (`git stash`-frei: auf einem zweiten Checkout oder aus dem letzten CI-Lauf). Neue Funde in den Dateien dieses Plans benennen.

- [ ] **Step 6: Den Plan entfernen**

Umsetzungspläne bleiben nach der Ausführung nicht liegen (`CLAUDE.md`, „Laufende Arbeit"): ein liegengebliebener Plan liest sich wie eine offene Absicht.

```bash
git rm docs/superpowers/plans/2026-11-18-einrichtungsassistent-stufe-1.md
```

- [ ] **Step 7: Commit**

```bash
git add docs/PROJECT_CONTEXT.md github_docs/de-Konfiguration.md github_docs/en-Configuration.md app-desktop/src/main/i18n/strings_en.properties app-desktop/src/main/i18n/strings_de.properties
git commit -m "Document the setup wizard"
```

- [ ] **Step 8: Abschlussbericht**

Auf Deutsch, nach `AGENTS.md` „Completion": erfülltes Verständnis; geänderte Dateien; Entwurfsentscheidungen (Schreiben vor dem Bau der Laufzeit, `menuActionConnect` statt `connectFromSettings`, Speichern wie getippt); Tests und Bau mit den gelesenen Zahlen; Ergebnis der beiden Handprüfungen aus Task 6 und 7; Dokumentations-Einstufung; veraltete Bildschirmfotos (keines zeigt den Assistenten, ein neues wäre fällig, nicht erfinden); verbleibende Unsicherheit; vorgeschlagener nächster Schritt (Plan für Ausbaustufe 2), ohne ihn auszuführen.
