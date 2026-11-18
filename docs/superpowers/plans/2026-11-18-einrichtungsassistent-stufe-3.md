# Einrichtungsassistent, Ausbaustufe 3 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Der Einrichtungsassistent kann Anmeldung und Station getrennt aus einem anderen, eingerichteten Profil übernehmen.

**Architecture:** Der Starter liest die Profildateien der anderen Profile in eigene `ChatPreferences`-Objekte und reicht sie dem Zustand als „Spender". Eine dritte Quelle `TAKE` belegt die Antworten einer Gruppe aus dem Spender vor; beim Speichern kopiert `copyStation` zusätzlich alle Stationsfelder, die der Assistent nie fragt. Geschrieben wird nur in das aktive Profil. Der Weg bleibt der bisherige: `preferences.xml` vor dem Bau der Laufzeit, keine laufende Sitzung.

**Tech Stack:** Kotlin 2.2 und Compose Multiplatform 1.8 (`app-desktop`), JUnit 5, Mockito 5, Gradle-Wrapper, Java 21.

**Spec:** `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md` — dieser Plan setzt **Ausbaustufe 3** um („von Profil X übernehmen"). Danach ist die Spec vollständig umgesetzt.

## Global Constraints

- Zweig `feature/setup-wizard`, offener PR #174. **Kein Merge**, bevor das Feature fertig und CI grün ist.
- Jeder Commit ist signiert (`commit.gpgsign=true`, SSH) und trägt am Ende `Written-by:`-Zeilen mit dem Modellnamen, ohne E-Mail: die des Umsetzers und `Written-by: Claude Fable 5.2` für den Plan. Kein `Co-Authored-By`. Vor jedem Push prüft der Controller `git log --format='%G?' main..HEAD` auf ausschließlich `G`.
- Gradle, nicht Maven. Java 21. Bau und Tests über `./gradlew`.
- Quelltextkommentare und Javadoc ausschließlich Englisch. Handbuch-Deutsch in neuer Rechtschreibung (`muss`, `dass`).
- Oberflächentexte in `app-desktop/src/main/i18n/strings_en.properties` (Basis) und `strings_de.properties`. Beide Dateien bekommen jeden neuen Schlüssel. Kein Apostroph in neuen Texten.
- **Ein Spender wird nur gelesen.** Seine Datei wird nie geschrieben, nie angelegt und nie verändert. Die Datei eines fremden Profils wird über `ChatPreferences(Path)` gelesen, nicht über `ChatPreferences(String)`: der zweite Konstruktor legt eine fehlende Datei aus der Vorlage an.
- **Spender ist nur ein eingerichtetes Profil:** Datei vorhanden, lesbar, und `LoginConfiguration.isUnconfigured` ist falsch. Das aktive Profil ist nie sein eigener Spender.
- **Übernommen wird getrennt:** Anmeldung (Rufzeichen, Passwort) und Station je für sich, auch aus verschiedenen Profilen.
- **Station heißt die ganze Station:** auch Felder, die der Assistent nie fragt (QTF-Vorgabe, größte Entfernung, alle Pfadanalysewerte, Rotor 2, SO2R, Win-Test-Einzelheiten, mehrere Logquellen zugleich). Nicht zur Station gehören Anmeldung, Namen, Sprache, Aussehen, Fensteranordnung, Farben, Kurzbefehle, Textbausteine, Baken, automatische Antworten, Töne, Filter, Serveradresse, Spotter-Rufzeichen.
- Persönliches (Sprache, Anzeigename) wird nie übernommen und immer gefragt.
- „Wird abgeschaltet" vergleicht immer mit dem **eigenen** Profil, nie mit dem Spender.
- Übernommene Werte bleiben änderbar: jede Seite lässt sich aus der Zusammenfassung öffnen, außer der Logseite, wenn die übernommene Station mehrere Logquellen hat.
- Nie blockierend auf dem AWT-Ereignisfaden warten. An `ApplicationRuntimeLauncher.java` und `Kst4ContestApplication.java` ändert dieser Plan nichts.
- Unverändert bleiben: das Format von `preferences.xml`, das Einstellungsfenster, `SettingsDraft.apply`, `OperatorProfileManagementService` (das Duplizieren löscht die Anmeldedaten weiterhin), `praktiKSTpreferences.xml`.
- Agenten starten das Programm nie (`:app-desktop:run`) und fassen `~/.praktiKST` nicht an. Handprüfungen bleiben beim Nutzer.
- Ein Testfehler bricht den Bau, PMD und SpotBugs nicht. Ergebnis aus der Test-XML lesen.

## Review Focus

1. **Eine Einstellung kommt neu zu `ChatPreferences` hinzu**: sie muss ausdrücklich als „Station" oder „nicht Station" eingeordnet werden, sonst wird sie beim Übernehmen still vergessen oder still mitkopiert. Test in Task 1 (`everySetterIsClassified`, `copyStationCallsExactlyTheStationSetters`).
2. **Spenderdatei fehlt, ist kaputt oder gehört einem uneingerichteten Profil**: dieses Profil wird nicht angeboten, und der Assistent öffnet sich trotzdem. Die Datei wird dabei nicht angelegt. Test in Task 3 (`onlyConfiguredReadableProfilesBecomeDonors`, `aMissingDonorFileIsNotCreated`).
3. **Spender mit mehreren Logquellen zugleich**: alle kommen an, und die Logseite bleibt zu. Test in Task 2 (`aTakenStationKeepsSeveralLogSources`).
4. **Eigene Einstellungen, die der Spender nicht hat**: „Wird abgeschaltet" nennt sie, obwohl keine Seite gezeigt wurde. Test in Task 2 (`takingAStationNamesWhatTheOwnProfileLoses`).
5. **Spender nach dem Wählen gewechselt oder Quelle zurück auf „neu"**: es bleibt nichts vom vorigen Spender stehen. Test in Task 2 (`changingTheDonorLoadsItsValues`, `goingBackToNewDropsWhatWasTaken`).

---

## Dateien

| Datei | Aufgabe |
|---|---|
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardStationCopy.kt` (neu) | `copyStation`: alle Stationsfelder von einem `ChatPreferences` in ein anderes |
| `app-desktop/src/test/resources/wizard-station-setters.txt`, `wizard-other-setters.txt` (neu) | Einordnung jedes Setters von `ChatPreferences` |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardIntegrations.kt` | Schalter als Mengen statt Abschaltlisten |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt` | Quelle `TAKE`, Spender, Übernehmen beim Schreiben |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardWindow.kt` | Spender laden |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt` | Einstiegsseite, Herkunft in der Zusammenfassung |
| `app-desktop/src/main/i18n/strings_en.properties`, `strings_de.properties` | neue Texte |
| `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/*Test.kt` | Tests |
| Spec, `docs/PROJECT_CONTEXT.md`, `github_docs/de-Konfiguration.md`, `github_docs/en-Configuration.md` | Dokumentation |

---

### Task 1: Die Station kopieren

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardStationCopy.kt`
- Create: `app-desktop/src/test/resources/wizard-station-setters.txt`
- Create: `app-desktop/src/test/resources/wizard-other-setters.txt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardStationCopyTest.kt`

**Interfaces:**
- Consumes: Getter und Setter von `ChatPreferences`; `ChatCategory(int)`.
- Produces: `internal fun copyStation(from: ChatPreferences, to: ChatPreferences)` im Paket `kst4contest.view.compose.wizard`.

- [ ] **Step 1: Die Einordnung anlegen**

`app-desktop/src/test/resources/wizard-station-setters.txt` — die 65 Setter, die zur Station gehören, einer je Zeile:

```text
setAirScout_asBandString
setAirScout_asClientNameString
setAirScout_asCommunicationPort
setAirScout_asServerNameString
setAirScout_asUDPListenerEnabled
setAirScout_autoBandSelectionEnabled
setLoginChatCategoryMain
setLoginChatCategorySecond
setLoginToSecondChatEnabled
setLogsynch_fileBasedWkdCallInterpreterEnabled
setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly
setLogsynch_ucxUDPWkdCallListenerEnabled
setLogsynch_ucxUDPWkdCallListenerPort
setLogsynch_wintestNetworkBroadcastAddress
setLogsynch_wintestNetworkListenerEnabled
setLogsynch_wintestNetworkPort
setLogsynch_wintestNetworkSimulationEnabled
setLogsynch_wintestNetworkSkedPushEnabled
setLogsynch_wintestNetworkStationIDOfKST
setLogsynch_wintestNetworkStationNameOfKST
setLogsynch_wintestNetworkStationNameOfWintestClient1
setLogsynch_wintestNetworkStationNameOfWintestClient2
setLogsynch_wintestQrgSyncEnabled
setLogsynch_wintestSkedMode
setLogsynch_wintestUsePassQrg
setNotify_dxClusterServerEnabled
setNotify_dxclusterServerPort
setStn_antennaBeamWidthDeg
setStn_bandActive10G
setStn_bandActive1240
setStn_bandActive144
setStn_bandActive2300
setStn_bandActive3400
setStn_bandActive432
setStn_bandActive50
setStn_bandActive5600
setStn_bandActive70
setStn_loginLocatorMainCat
setStn_maxQRBDefault
setStn_pathAnalysisContestMarginDb
setStn_pathAnalysisDefaultTargetAntennaGainDbi
setStn_pathAnalysisDefaultTargetAntennaHeightMeters
setStn_pathAnalysisDefaultTargetTxPowerWatts
setStn_pathAnalysisDemDatasetId
setStn_pathAnalysisDemRootDirectory
setStn_pathAnalysisDxHighestPointInSquare
setStn_pathAnalysisFeederLossIncreaseDbPer200MHz
setStn_pathAnalysisMaxEstimatedFeederLossPerStationDb
setStn_pathAnalysisOwnAntennaGainDbi
setStn_pathAnalysisOwnAntennaHeightMeters
setStn_pathAnalysisOwnHeightAslMeters
setStn_pathAnalysisOwnTxPowerWatts
setStn_pathAnalysisRequiredCwSignalDbm
setStn_pathAnalysisRequiredSsbSignalDbm
setStn_pathAnalysisVhfFeederLossPerStationDb
setStn_pstRotator2Enabled
setStn_pstRotator2Host
setStn_pstRotator2Port
setStn_pstRotatorEnabled
setStn_pstRotatorHost
setStn_pstRotatorPort
setStn_qtfDefault
setStn_useQtfFilterDirectionAsAntennaHeading
setTrxSynch_so2rEnabled
setTrxSynch_ucxLogUDPListenerEnabled
```

`app-desktop/src/test/resources/wizard-other-setters.txt` — die 75 übrigen, einer je Zeile:

```text
setActualQTF
setBcn_beaconIntervalInMinutesMainCat
setBcn_beaconIntervalInMinutesSecondCat
setBcn_beaconTextMainCat
setBcn_beaconTextSecondCat
setBcn_beaconsEnabledMainCat
setBcn_beaconsEnabledSecondCat
setBcn_contestScoreSum
setChatState
setGUI_darkModeActive
setGUI_darkModeActiveByDefault
setGUIclusterAndQSOMonStage_SceneSizeHW
setGUImainWindowLeftSplitPane_dividerposition
setGUImainWindowRightSplitPane_dividerposition
setGUImessageSectionSplitpane_dividerposition
setGUIpnl_directedMSGWin_dividerpositionDefault
setGUIscn_ChatwindowMainSceneSizeHW
setGUIselectedCallSignSplitPane_dividerposition
setGUIsettingsStageSceneSizeHW
setGUIstage_updateStage_SceneSizeHW
setGUIstationMapAlwaysOnTop
setGUIstationMapClusteringEnabled
setGUIstationMapPathAnalysisVisible
setGUIstationMapStagePositionXY
setGUIstationMapStageSceneSizeHW
setGuiOptions_defaultFilterNothing
setGuiOptions_defaultFilterPmToMe
setGuiOptions_defaultFilterPmToOther
setGuiOptions_defaultFilterPublicMsgs
setGuiOptions_language
setGuiOptions_paletteOverridesDay
setGuiOptions_paletteOverridesEvening
setGuiOptions_rememberWindowPositions
setGuiOptions_showFreshCallHintInBandColumns
setGuiOptions_showGrossFieldWorkedHintInBandColumns
setLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup
setLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup
setLstNotify_QSOSniffer_sniffedPrefixLocList
setLst_txtShortCutBtnList
setLst_txtSnipList
setMYQRGFirstCat
setMYQRGSecondCat
setMessageHandling_autoAnswerEnabled
setMessageHandling_autoAnswerEnabledSecondCat
setMessageHandling_autoAnswerTextMainCat
setMessageHandling_autoAnswerTextSecondCat
setMessageHandling_autoAnswerToQRGRequestEnabled
setMessageHandling_beaconUnworkedstationsPrefix
setMessageHandling_debugModeToFileEnabled
setMessageHandling_unworkedStnRequesterBeaconsEnabled
setMessageHandling_unworkedStnRequesterBeaconsInterval
setMessageHandling_unworkedStnRequesterBeaconsText
setNotify_DXCSrv_SpottersCallSign
setNotify_DXClusterServerTriggerBearing
setNotify_DXClusterServerTriggerOnQRGDetect
setNotify_bandUpgradeHintOnLogEnabled
setNotify_bandUpgradePriorityBoostEnabled
setNotify_momentumWindowSeconds
setNotify_noReplyPenaltyMinutes
setNotify_optionalFrequencyPrefix
setNotify_playCWCallsignsOnRxedPMs
setNotify_playSimpleSounds
setNotify_playVoiceCallsignsOnRxedPMs
setNotify_positiveSignalsPatterns
setProgramVersion
setShortcuts
setStn_loginAFKState
setStn_loginCallSign
setStn_loginNameMainCat
setStn_loginNameSecondCat
setStn_loginPassword
setStn_on4kstServersDns
setStn_on4kstServersPort
setStoreAndRestorePreferencesFileName
setTextSnippets
```

- [ ] **Step 2: Write the failing test**

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Path

/**
 * What "the station" is when one profile takes it from another.
 *
 * The two lists under src/test/resources are the decision, one line per ChatPreferences
 * setter. A setter in neither list fails the first test, so a new setting cannot be forgotten
 * here: somebody has to say whether another operator at the same station should inherit it.
 */
class WizardStationCopyTest {

    private fun lines(name: String): Set<String> =
        File("src/test/resources/$name").readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private val stationSetters = lines("wizard-station-setters.txt")
    private val otherSetters = lines("wizard-other-setters.txt")

    /** Every public setter with one parameter, the shape every preference has. */
    private fun allSetters(): Set<String> =
        ChatPreferences::class.java.methods
            .filter { it.name.startsWith("set") && it.parameterCount == 1 && Modifier.isPublic(it.modifiers) }
            .map { it.name }
            .toSet()

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("wizard-station-copy-test-unused.xml")).apply(configure)

    @Test
    fun everySetterIsClassified() {
        val all = allSetters()

        assertEquals(
            emptySet<String>(),
            all - stationSetters - otherSetters,
            "new ChatPreferences setters: add each to wizard-station-setters.txt (and to copyStation) " +
                "or to wizard-other-setters.txt",
        )
        assertEquals(emptySet<String>(), (stationSetters + otherSetters) - all, "listed setters that no longer exist")
        assertEquals(emptySet<String>(), stationSetters.intersect(otherSetters), "a setter cannot be in both lists")
    }

    @Test
    fun copyStationCallsExactlyTheStationSetters() {
        val target = Mockito.mock(ChatPreferences::class.java)

        copyStation(prefs(), target)

        val called = Mockito.mockingDetails(target).invocations
            .map { it.method.name }
            .filter { it.startsWith("set") }

        assertEquals(stationSetters, called.toSet(), "copyStation and wizard-station-setters.txt disagree")
        assertEquals(called.size, called.toSet().size, "a setter was called twice")
        assertTrue(
            Mockito.mockingDetails(target).invocations.none { it.method.name == "writePreferencesToXmlFile" },
            "copying must not write a file",
        )
    }

    @Test
    fun theStationArrivesIncludingWhatTheWizardNeverAsks() {
        val donor = prefs {
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setStn_bandActive1240(true)
            setStn_bandActive432(false)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(true)
            setLoginChatCategorySecond(ChatCategory(3))
            setStn_qtfDefault(270.0)
            setStn_maxQRBDefault(800.0)
            setStn_pathAnalysisDemRootDirectory("/srv/dem")
            setStn_pathAnalysisContestMarginDb(9.0)
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
            setStn_pstRotator2Port(12020)
            // Several log sources at once: the wizard's own question could not express this.
            setLogsynch_wintestNetworkListenerEnabled(true)
            setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
            setLogsynch_wintestNetworkStationNameOfWintestClient1("RUN")
            setLogsynch_wintestUsePassQrg(true)
            setAirScout_asBandString("4320000")
        }
        val target = prefs {
            setStn_bandActive432(true)
            setLoginChatCategoryMain(ChatCategory(3))
        }

        copyStation(donor, target)

        assertEquals("JO50AA", target.getStn_loginLocatorMainCat())
        assertTrue(target.isStn_bandActive144())
        assertTrue(target.isStn_bandActive1240())
        assertFalse(target.isStn_bandActive432(), "a band the donor does not have goes off")
        assertEquals(2, target.getLoginChatCategoryMain().categoryNumber)
        assertTrue(target.isLoginToSecondChatEnabled())
        assertEquals(3, target.getLoginChatCategorySecond().categoryNumber)
        assertEquals(270.0, target.getStn_qtfDefault())
        assertEquals(800.0, target.getStn_maxQRBDefault())
        assertEquals("/srv/dem", target.getStn_pathAnalysisDemRootDirectory())
        assertEquals(9.0, target.getStn_pathAnalysisContestMarginDb())
        assertTrue(target.isTrxSynch_so2rEnabled())
        assertTrue(target.isStn_pstRotator2Enabled())
        assertEquals(12020, target.getStn_pstRotator2Port())
        assertTrue(target.isLogsynch_wintestNetworkListenerEnabled())
        assertTrue(target.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertEquals("RUN", target.getLogsynch_wintestNetworkStationNameOfWintestClient1())
        assertTrue(target.isLogsynch_wintestUsePassQrg())
        assertEquals("4320000", target.getAirScout_asBandString())
    }

    @Test
    fun whatBelongsToTheOperatorStays() {
        val donor = prefs {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("donor-secret")
            setStn_loginNameMainCat("Donor")
            setStn_loginNameSecondCat("Donor SHF")
            setGuiOptions_language("en")
            setGUI_darkModeActiveByDefault(true)
            setStn_on4kstServersDns("donor.example")
            setNotify_DXCSrv_SpottersCallSign("DL0TEST")
            setBcn_beaconTextMainCat("donor beacon")
        }
        val target = prefs {
            setStn_loginCallSign("DM5XX")
            setStn_loginPassword("own-secret")
            setStn_loginNameMainCat("Own")
            setStn_loginNameSecondCat("Own SHF")
            setGuiOptions_language("de")
            setGUI_darkModeActiveByDefault(false)
            setStn_on4kstServersDns("own.example")
            setNotify_DXCSrv_SpottersCallSign("DM5XX")
            setBcn_beaconTextMainCat("own beacon")
        }

        copyStation(donor, target)

        assertEquals("DM5XX", target.getStn_loginCallSign())
        assertEquals("own-secret", target.getStn_loginPassword())
        assertEquals("Own", target.getStn_loginNameMainCat())
        assertEquals("Own SHF", target.getStn_loginNameSecondCat())
        assertEquals("de", target.getGuiOptions_language())
        assertFalse(target.isGUI_darkModeActiveByDefault())
        assertEquals("own.example", target.getStn_on4kstServersDns())
        assertEquals("DM5XX", target.getNotify_DXCSrv_SpottersCallSign().get())
        assertEquals("own beacon", target.getBcn_beaconTextMainCat())
    }

    @Test
    fun theDonorIsNotChangedAndSharesNoObjectWithTheTarget() {
        val donor = prefs { setLoginChatCategoryMain(ChatCategory(2)) }
        val target = prefs()

        copyStation(donor, target)
        target.getLoginChatCategoryMain().categoryNumber = 7

        assertEquals(2, donor.getLoginChatCategoryMain().categoryNumber, "the two profiles must not share a category object")
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.WizardStationCopyTest'`
Expected: Kompilierfehler, `Unresolved reference: copyStation`.

- [ ] **Step 4: Write minimal implementation**

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences

/**
 * Copies the station from one profile's preferences to another's.
 *
 * "The station" is what hangs on the site and the equipment: locator, bands, chat
 * categories, antenna and path-analysis values, log program, frequency source, DX cluster
 * server, rotators, AirScout. It is deliberately more than the setup wizard asks about -- a
 * further operator at the same station should inherit the second rotator and the Win-Test
 * station filter too, without ever seeing a question for them.
 *
 * It is deliberately not the operator: login, names, language, appearance, layout,
 * shortcuts, snippets, beacons, automatic answers, sounds, filters. Nor the server address
 * or the spotter callsign.
 *
 * Which setter belongs where is decided line by line in
 * `src/test/resources/wizard-station-setters.txt` and `wizard-other-setters.txt`; a test
 * holds this function and those lists together, and fails when ChatPreferences gains a
 * setter nobody classified.
 *
 * Nothing is written to a file here, and [from] is only read.
 */
internal fun copyStation(from: ChatPreferences, to: ChatPreferences) {
    // Location, bands and chats. The categories are copied by number: the two profiles
    // must not share one mutable ChatCategory.
    to.setStn_loginLocatorMainCat(from.getStn_loginLocatorMainCat())
    to.setLoginChatCategoryMain(from.getLoginChatCategoryMain()?.let { ChatCategory(it.categoryNumber) })
    to.setLoginChatCategorySecond(from.getLoginChatCategorySecond()?.let { ChatCategory(it.categoryNumber) })
    to.setLoginToSecondChatEnabled(from.isLoginToSecondChatEnabled())

    // Bands.
    to.setStn_bandActive50(from.isStn_bandActive50())
    to.setStn_bandActive70(from.isStn_bandActive70())
    to.setStn_bandActive144(from.isStn_bandActive144())
    to.setStn_bandActive432(from.isStn_bandActive432())
    to.setStn_bandActive1240(from.isStn_bandActive1240())
    to.setStn_bandActive2300(from.isStn_bandActive2300())
    to.setStn_bandActive3400(from.isStn_bandActive3400())
    to.setStn_bandActive5600(from.isStn_bandActive5600())
    to.setStn_bandActive10G(from.isStn_bandActive10G())

    // Antenna, defaults and everything the path analysis needs to know about the site.
    to.setStn_antennaBeamWidthDeg(from.getStn_antennaBeamWidthDeg())
    to.setStn_qtfDefault(from.getStn_qtfDefault())
    to.setStn_maxQRBDefault(from.getStn_maxQRBDefault())
    to.setStn_useQtfFilterDirectionAsAntennaHeading(from.isStn_useQtfFilterDirectionAsAntennaHeading())
    to.setStn_pathAnalysisOwnAntennaGainDbi(from.getStn_pathAnalysisOwnAntennaGainDbi())
    to.setStn_pathAnalysisOwnAntennaHeightMeters(from.getStn_pathAnalysisOwnAntennaHeightMeters())
    to.setStn_pathAnalysisOwnTxPowerWatts(from.getStn_pathAnalysisOwnTxPowerWatts())
    to.setStn_pathAnalysisOwnHeightAslMeters(from.getStn_pathAnalysisOwnHeightAslMeters())
    to.setStn_pathAnalysisDefaultTargetAntennaGainDbi(from.getStn_pathAnalysisDefaultTargetAntennaGainDbi())
    to.setStn_pathAnalysisDefaultTargetAntennaHeightMeters(from.getStn_pathAnalysisDefaultTargetAntennaHeightMeters())
    to.setStn_pathAnalysisDefaultTargetTxPowerWatts(from.getStn_pathAnalysisDefaultTargetTxPowerWatts())
    to.setStn_pathAnalysisDxHighestPointInSquare(from.isStn_pathAnalysisDxHighestPointInSquare())
    to.setStn_pathAnalysisDemRootDirectory(from.getStn_pathAnalysisDemRootDirectory())
    to.setStn_pathAnalysisDemDatasetId(from.getStn_pathAnalysisDemDatasetId())
    to.setStn_pathAnalysisVhfFeederLossPerStationDb(from.getStn_pathAnalysisVhfFeederLossPerStationDb())
    to.setStn_pathAnalysisFeederLossIncreaseDbPer200MHz(from.getStn_pathAnalysisFeederLossIncreaseDbPer200MHz())
    to.setStn_pathAnalysisMaxEstimatedFeederLossPerStationDb(from.getStn_pathAnalysisMaxEstimatedFeederLossPerStationDb())
    to.setStn_pathAnalysisRequiredSsbSignalDbm(from.getStn_pathAnalysisRequiredSsbSignalDbm())
    to.setStn_pathAnalysisRequiredCwSignalDbm(from.getStn_pathAnalysisRequiredCwSignalDbm())
    to.setStn_pathAnalysisContestMarginDb(from.getStn_pathAnalysisContestMarginDb())

    // Log sources. All of them as they are: a station may run several at once.
    to.setLogsynch_fileBasedWkdCallInterpreterEnabled(from.isLogsynch_fileBasedWkdCallInterpreterEnabled())
    to.setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(from.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly())
    to.setLogsynch_ucxUDPWkdCallListenerEnabled(from.isLogsynch_ucxUDPWkdCallListenerEnabled())
    to.setLogsynch_ucxUDPWkdCallListenerPort(from.getLogsynch_ucxUDPWkdCallListenerPort())
    to.setLogsynch_wintestNetworkListenerEnabled(from.isLogsynch_wintestNetworkListenerEnabled())
    to.setLogsynch_wintestNetworkPort(from.getLogsynch_wintestNetworkPort())
    to.setLogsynch_wintestNetworkBroadcastAddress(from.getLogsynch_wintestNetworkBroadcastAddress())
    to.setLogsynch_wintestNetworkStationNameOfKST(from.getLogsynch_wintestNetworkStationNameOfKST())
    to.setLogsynch_wintestNetworkStationIDOfKST(from.getLogsynch_wintestNetworkStationIDOfKST())
    to.setLogsynch_wintestNetworkStationNameOfWintestClient1(from.getLogsynch_wintestNetworkStationNameOfWintestClient1())
    to.setLogsynch_wintestNetworkStationNameOfWintestClient2(from.getLogsynch_wintestNetworkStationNameOfWintestClient2())
    to.setLogsynch_wintestNetworkSkedPushEnabled(from.isLogsynch_wintestNetworkSkedPushEnabled())
    to.setLogsynch_wintestNetworkSimulationEnabled(from.isLogsynch_wintestNetworkSimulationEnabled())
    to.setLogsynch_wintestSkedMode(from.getLogsynch_wintestSkedMode())

    // Frequency sources, SO2R and the DX cluster server for the log program.
    to.setLogsynch_wintestQrgSyncEnabled(from.isLogsynch_wintestQrgSyncEnabled())
    to.setLogsynch_wintestUsePassQrg(from.isLogsynch_wintestUsePassQrg())
    to.setTrxSynch_ucxLogUDPListenerEnabled(from.isTrxSynch_ucxLogUDPListenerEnabled())
    to.setTrxSynch_so2rEnabled(from.isTrxSynch_so2rEnabled())
    to.setNotify_dxClusterServerEnabled(from.isNotify_dxClusterServerEnabled())
    to.setNotify_dxclusterServerPort(from.getNotify_dxclusterServerPort())

    // Rotators.
    to.setStn_pstRotatorEnabled(from.isStn_pstRotatorEnabled())
    to.setStn_pstRotatorHost(from.getStn_pstRotatorHost())
    to.setStn_pstRotatorPort(from.getStn_pstRotatorPort())
    to.setStn_pstRotator2Enabled(from.isStn_pstRotator2Enabled())
    to.setStn_pstRotator2Host(from.getStn_pstRotator2Host())
    to.setStn_pstRotator2Port(from.getStn_pstRotator2Port())

    // AirScout.
    to.setAirScout_asUDPListenerEnabled(from.isAirScout_asUDPListenerEnabled())
    to.setAirScout_asCommunicationPort(from.getAirScout_asCommunicationPort())
    to.setAirScout_asServerNameString(from.getAirScout_asServerNameString())
    to.setAirScout_asClientNameString(from.getAirScout_asClientNameString())
    to.setAirScout_autoBandSelectionEnabled(from.isAirScout_autoBandSelectionEnabled())
    to.setAirScout_asBandString(from.getAirScout_asBandString())
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.WizardStationCopyTest'`
Expected: 5 Tests, 0 Fehler in der XML.

Schlägt `everySetterIsClassified` fehl, weil `ChatPreferences` inzwischen einen Setter mehr oder weniger hat als die 140 dieses Plans: **nicht** raten. Den Namen melden; die Einordnung ist eine fachliche Entscheidung.

Schlägt `theStationArrivesIncludingWhatTheWizardNeverAsks` an einem einzelnen Wert fehl, weil ein Setter von `ChatPreferences` ihn verändert (etwa einen Port auf seine Werksvorgabe setzt): den Setter und den Wert melden, den Test nicht ändern.

- [ ] **Step 6: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardStationCopy.kt app-desktop/src/test/resources/wizard-station-setters.txt app-desktop/src/test/resources/wizard-other-setters.txt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardStationCopyTest.kt
git commit -m "Copy a station from one profile's preferences to another's" -m "Written-by: <your model name>
Written-by: Claude Fable 5.2"
```

---

### Task 2: Quelle „übernehmen" im Zustand

**Files:**
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardIntegrations.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardIntegrationsTest.kt` (zwei Tests ersetzen)
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardDonorTest.kt` (neu)

**Interfaces:**
- Consumes: `copyStation` (Task 1); `WizardAntenna`, `WizardIntegrations`, `LoginConfiguration`.
- Produces:
  - `enum class WizardSource { NEW, TAKE, KEEP }`
  - `class WizardDonor(val profileId: String, val displayName: String, val prefs: ChatPreferences)` mit `val callsign: String`, `val locator: String`
  - `class SetupWizardState(prefs: ChatPreferences, donors: List<WizardDonor> = emptyList())`
  - in `SetupWizardState`: `val donors: List<WizardDonor>`, `val loginDonor: WizardDonor?`, `val stationDonor: WizardDonor?`, `fun chooseLoginDonor(WizardDonor)`, `fun chooseStationDonor(WizardDonor)`; `antenna` und `integrations` werden bei einem Quellwechsel durch neue Objekte ersetzt (weiterhin als `state.antenna`, `state.integrations` lesbar)
  - in `WizardIntegrations`: `fun storedLogSwitches(): Set<WizardSwitch>`, `fun storedGearSwitches(): Set<WizardSwitch>`, `fun answeredLogSwitches(): Set<WizardSwitch>`, `fun answeredGearSwitches(): Set<WizardSwitch>`; entfallen: `switchedOffByLog()`, `switchedOffByGear()`

Die „Alt"-Ausschnitte unten stammen aus dem Stand `68d86ce3`. Weicht eine Zeile ab, die Stelle gleichen Inhalts ersetzen und die Abweichung im Bericht nennen.

- [ ] **Step 1: Write the failing tests**

(a) In `WizardIntegrationsTest.kt` die beiden Tests `theSwitchedOffListNamesWhatWasOnAndWillBeOff` und `nothingIsSwitchedOffOnAStationThatHadNothingOn` löschen und an ihrer Stelle einfügen:

```kotlin
    @Test
    fun theStoredSwitchesAreWhatTheProfileHasOn() {
        val template = WizardIntegrations(likeTheTemplate())

        assertEquals(
            setOf(WizardSwitch.LOG_UDP, WizardSwitch.LOG_FILE, WizardSwitch.QRG_FOLLOW, WizardSwitch.CLUSTER_SERVER),
            template.storedLogSwitches(),
        )
        assertEquals(setOf(WizardSwitch.AIRSCOUT), template.storedGearSwitches())

        val nothing = WizardIntegrations(allOff())
        assertEquals(emptySet<WizardSwitch>(), nothing.storedLogSwitches())
        assertEquals(emptySet<WizardSwitch>(), nothing.storedGearSwitches())
    }

    @Test
    fun theAnsweredSwitchesFollowThePages() {
        val answers = WizardIntegrations(likeTheTemplate())
        answers.loadFresh()

        assertEquals(emptySet<WizardSwitch>(), answers.answeredLogSwitches())
        assertEquals(emptySet<WizardSwitch>(), answers.answeredGearSwitches())

        answers.chooseLogProgram(WizardLogProgram.WINTEST)
        answers.qrgFollow = true
        answers.clusterServer = true
        answers.rotator = true

        assertEquals(
            setOf(WizardSwitch.LOG_WINTEST, WizardSwitch.QRG_FOLLOW, WizardSwitch.CLUSTER_SERVER),
            answers.answeredLogSwitches(),
        )
        assertEquals(setOf(WizardSwitch.ROTATOR), answers.answeredGearSwitches())
        // The stored side does not move with the answers.
        assertEquals(setOf(WizardSwitch.AIRSCOUT), answers.storedGearSwitches())
    }
```

(b) Neue Datei `SetupWizardDonorTest.kt`:

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Taking the login or the station from another profile: what is offered, what the answers
 * become, what the summary says against the own profile, and what reaches the file.
 */
class SetupWizardDonorTest(@param:TempDir private val directory: Path) {

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("setup-wizard-donor-test-unused.xml")).apply(configure)

    /** Every integration switch stated, so no test hangs on a compiled-in default. */
    private fun ChatPreferences.integrationsOff() {
        setLogsynch_wintestNetworkListenerEnabled(false)
        setLogsynch_ucxUDPWkdCallListenerEnabled(false)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(false)
        setTrxSynch_ucxLogUDPListenerEnabled(false)
        setLogsynch_wintestQrgSyncEnabled(false)
        setNotify_dxClusterServerEnabled(false)
        setStn_pstRotatorEnabled(false)
        setAirScout_asUDPListenerEnabled(false)
        setTrxSynch_so2rEnabled(false)
        setStn_pstRotator2Enabled(false)
    }

    /** An empty profile, as "new profile" creates it: no login, the template's integrations on. */
    private fun emptyProfile() = prefs {
        integrationsOff()
        setStn_loginCallSign("")
        setStn_loginPassword("")
        setStn_loginLocatorMainCat("JO54ME")
        setStn_bandActive144(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(true)
        setLoginChatCategorySecond(ChatCategory(3))
        setLogsynch_ucxUDPWkdCallListenerEnabled(true)
        setAirScout_asUDPListenerEnabled(true)
    }

    private fun contestStation() = WizardDonor("contest", "Conteststation", prefs {
        integrationsOff()
        setStn_loginCallSign("DL0TEST")
        setStn_loginPassword("secret")
        setStn_loginLocatorMainCat("JO50AA")
        setStn_bandActive144(true)
        setStn_bandActive432(true)
        setLoginChatCategoryMain(ChatCategory(2))
        setLoginToSecondChatEnabled(false)
        setStn_antennaBeamWidthDeg(28.0)
        setStn_qtfDefault(270.0)
        setLogsynch_wintestNetworkListenerEnabled(true)
        setLogsynch_wintestNetworkPort(9871)
        setLogsynch_wintestNetworkStationNameOfKST("KST")
        setLogsynch_wintestQrgSyncEnabled(true)
        setStn_pstRotatorEnabled(true)
        setStn_pstRotatorHost("127.0.0.1")
        setStn_pstRotatorPort(12000)
    })

    private fun portable() = WizardDonor("portable", "Portabel", prefs {
        integrationsOff()
        setStn_loginCallSign("DL0TEST/P")
        setStn_loginPassword("secret")
        setStn_loginLocatorMainCat("JN58QA")
        setStn_bandActive1240(true)
        setLoginChatCategoryMain(ChatCategory(3))
        setLoginToSecondChatEnabled(false)
    })

    @Test
    fun withoutDonorsNothingChanges() {
        val state = SetupWizardState(emptyProfile())

        assertEquals(listOf(WizardSource.NEW), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.KEEP), state.stationSources)
        assertTrue(state.donors.isEmpty())
    }

    @Test
    fun aDonorAddsTakeBetweenNewAndKeep() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))

        assertEquals(listOf(WizardSource.NEW, WizardSource.TAKE), state.loginSources)
        assertEquals(listOf(WizardSource.NEW, WizardSource.TAKE, WizardSource.KEEP), state.stationSources)
        assertTrue(state.hasEntryPage)
        assertNull(state.loginSource, "with a choice nothing is preselected")
    }

    @Test
    fun aSingleDonorIsChosenWithTheSource() {
        val donor = contestStation()
        val state = SetupWizardState(emptyProfile(), listOf(donor))

        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.NEW)

        assertEquals(donor, state.loginDonor, "one donor needs no second question")
        assertTrue(state.canGoNext)
    }

    @Test
    fun severalDonorsNeedAChoice() {
        val contest = contestStation()
        val state = SetupWizardState(emptyProfile(), listOf(contest, portable()))

        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.NEW)
        assertNull(state.loginDonor)
        assertFalse(state.canGoNext, "which profile is not said yet")

        state.chooseLoginDonor(contest)
        assertTrue(state.canGoNext)
    }

    @Test
    fun aDonorThatIsNotOfferedIsNotAccepted() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.TAKE)
        val first = state.loginDonor

        state.chooseLoginDonor(portable())

        assertEquals(first, state.loginDonor)
    }

    @Test
    fun takingTheLoginSkipsItsPageAndLeavesItChangeable() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals("DL0TEST", state.callsign)
        assertEquals("secret", state.password)
        assertFalse(state.pages.contains(WizardPage.LOGIN))

        state.name = "Philipp"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)
        state.edit(WizardPage.LOGIN)
        state.callsign = "DL0TEST/P"
        state.next()
        assertEquals("DL0TEST/P", state.callsign, "a taken callsign gets its /P")
    }

    @Test
    fun takingTheStationLoadsEverythingThePagesShow() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()

        assertEquals("JO50AA", state.locator)
        assertEquals(setOf(WizardBand.B144, WizardBand.B432), state.bands)
        assertEquals(2, state.category)
        assertFalse(state.secondChat)
        assertEquals(28.0, state.antenna.beamWidthDeg)
        assertEquals(WizardLogProgram.WINTEST, state.integrations.logProgram)
        assertTrue(state.integrations.rotator)
        assertTrue(state.summaryQrgFollow)
        assertEquals(
            listOf(WizardPage.ENTRY, WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SUMMARY),
            state.pages,
            "a taken station shows no page of its own",
        )
    }

    @Test
    fun takingAStationNamesWhatTheOwnProfileLoses() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()

        // The own profile had the second chat, the network listener and AirScout on; the
        // donor has none of them. Win-Test and the rotator come new and are not "switched off".
        assertEquals(
            listOf(WizardSwitch.SECOND_CHAT, WizardSwitch.LOG_UDP, WizardSwitch.AIRSCOUT),
            state.switchedOff,
        )
    }

    @Test
    fun aTakenStationKeepsSeveralLogSources() {
        val several = WizardDonor("several", "Zwei Quellen", contestStation().prefs.apply {
            setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        })
        val state = SetupWizardState(emptyProfile(), listOf(several))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()

        assertFalse(state.logPageEditable, "the page knows one source and would switch the other off")
        state.edit(WizardPage.LOG)
        assertFalse(state.pages.contains(WizardPage.LOG))
    }

    @Test
    fun changingTheDonorLoadsItsValues() {
        val contest = contestStation()
        val other = portable()
        val state = SetupWizardState(emptyProfile(), listOf(contest, other))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.chooseStationDonor(contest)
        state.next()
        assertEquals("JO50AA", state.locator)

        state.back()
        state.chooseStationDonor(other)
        state.next()

        assertEquals("JN58QA", state.locator)
        assertEquals(setOf(WizardBand.B1240), state.bands)
        assertEquals(3, state.category)
        assertEquals(WizardLogProgram.NONE, state.integrations.logProgram, "nothing of the first donor is left")
        assertFalse(state.integrations.rotator)
    }

    @Test
    fun goingBackToNewDropsWhatWasTaken() {
        val state = SetupWizardState(emptyProfile(), listOf(contestStation()))
        state.chooseLoginSource(WizardSource.NEW)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()
        state.back()
        state.chooseStationSource(WizardSource.NEW)
        state.next()

        assertEquals("", state.locator)
        assertTrue(state.bands.isEmpty())
        assertEquals(WizardLogProgram.NONE, state.integrations.logProgram)
        assertFalse(state.integrations.rotator)
        assertTrue(state.pages.contains(WizardPage.LOG))
    }

    // --- against real files -------------------------------------------------------------

    private fun file(name: String, configure: ChatPreferences.() -> Unit): Path {
        val path = directory.resolve(name)
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), path, StandardCopyOption.REPLACE_EXISTING)
        }
        ChatPreferences(path).apply {
            assertTrue(readPreferencesFromXmlFile())
            configure()
            assertTrue(writePreferencesToXmlFile())
        }
        return path
    }

    private fun read(path: Path) = ChatPreferences(path).also { assertTrue(it.readPreferencesFromXmlFile()) }

    @Test
    fun saveWritesTheWholeTakenStationAndTheTakenLoginIntoTheOwnFile() {
        val donorFile = file("donor.xml") {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            setStn_qtfDefault(270.0)
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
            setStn_pstRotator2Port(12020)
            setLogsynch_wintestNetworkListenerEnabled(true)
            setLogsynch_wintestNetworkStationNameOfWintestClient1("RUN")
        }
        val ownFile = file("own.xml") {
            setStn_loginCallSign("")
            setStn_loginPassword("")
            setStn_on4kstServersDns("own.example")
        }
        val donorBytes = Files.readAllBytes(donorFile)
        val own = read(ownFile)
        val state = SetupWizardState(own, listOf(WizardDonor("contest", "Conteststation", read(donorFile))))
        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()
        state.name = "Philipp"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        assertTrue(state.writeTo(own))

        val restored = read(ownFile)
        assertEquals("DL0TEST", restored.getStn_loginCallSign())
        assertEquals("secret", restored.getStn_loginPassword())
        assertEquals("Philipp", restored.getStn_loginNameMainCat())
        assertEquals("JO50AA", restored.getStn_loginLocatorMainCat())
        assertEquals(270.0, restored.getStn_qtfDefault(), "a station value the wizard never asks about")
        assertTrue(restored.isTrxSynch_so2rEnabled())
        assertTrue(restored.isStn_pstRotator2Enabled())
        assertEquals(12020, restored.getStn_pstRotator2Port())
        assertTrue(restored.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals("RUN", restored.getLogsynch_wintestNetworkStationNameOfWintestClient1())
        assertEquals("own.example", restored.getStn_on4kstServersDns(), "not part of the station")
        assertArrayEquals(donorBytes, Files.readAllBytes(donorFile), "the donor's file was touched")
    }

    @Test
    fun aPageOpenedAfterTakingOverridesTheCopy() {
        val donorFile = file("donor.xml") {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("secret")
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
            setStn_pstRotatorEnabled(true)
            setStn_pstRotatorHost("127.0.0.1")
            setStn_pstRotatorPort(12000)
        }
        val ownFile = file("own.xml") {
            setStn_loginCallSign("")
            setStn_loginPassword("")
        }
        val own = read(ownFile)
        val state = SetupWizardState(own, listOf(WizardDonor("contest", "Conteststation", read(donorFile))))
        state.chooseLoginSource(WizardSource.TAKE)
        state.chooseStationSource(WizardSource.TAKE)
        state.next()
        state.name = "Philipp"
        state.next()
        state.edit(WizardPage.GEAR)
        state.integrations.rotator = false
        state.next()
        state.edit(WizardPage.SITE)
        state.locator = "JN58QA"
        state.next()

        assertTrue(state.writeTo(own))

        val restored = read(ownFile)
        assertFalse(restored.isStn_pstRotatorEnabled(), "the operator switched the taken rotator off")
        assertEquals("JN58QA", restored.getStn_loginLocatorMainCat(), "the operator changed the taken locator")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app-desktop:compileTestKotlin`
Expected: Kompilierfehler, unter anderem `Unresolved reference: WizardDonor` und `Unresolved reference: storedLogSwitches`.

- [ ] **Step 3: `WizardIntegrations.kt` ändern**

Die beiden Funktionen `switchedOffByLog()` und `switchedOffByGear()` samt ihren KDoc-Zeilen löschen und an ihrer Stelle einfügen:

```kotlin
    /** The log-page switches the profile these answers were built from has on. */
    fun storedLogSwitches(): Set<WizardSwitch> = buildSet {
        if (storedWintest) add(WizardSwitch.LOG_WINTEST)
        if (storedUdp) add(WizardSwitch.LOG_UDP)
        if (storedFile) add(WizardSwitch.LOG_FILE)
        if (storedQrgFollow) add(WizardSwitch.QRG_FOLLOW)
        if (storedCluster) add(WizardSwitch.CLUSTER_SERVER)
    }

    /** The equipment switches the profile these answers were built from has on. */
    fun storedGearSwitches(): Set<WizardSwitch> = buildSet {
        if (storedRotator) add(WizardSwitch.ROTATOR)
        if (storedAirScout) add(WizardSwitch.AIRSCOUT)
    }

    /** The log-page switches that are on once [writeLogTo] has run. */
    fun answeredLogSwitches(): Set<WizardSwitch> = buildSet {
        when (logProgram) {
            WizardLogProgram.WINTEST -> add(WizardSwitch.LOG_WINTEST)
            WizardLogProgram.UDP -> add(WizardSwitch.LOG_UDP)
            WizardLogProgram.FILE -> add(WizardSwitch.LOG_FILE)
            WizardLogProgram.NONE -> Unit
        }
        if (networked && qrgFollow) add(WizardSwitch.QRG_FOLLOW)
        if (networked && clusterServer) add(WizardSwitch.CLUSTER_SERVER)
    }

    /** The equipment switches that are on once [writeGearTo] has run. */
    fun answeredGearSwitches(): Set<WizardSwitch> = buildSet {
        if (rotator) add(WizardSwitch.ROTATOR)
        if (airScout) add(WizardSwitch.AIRSCOUT)
    }
```

- [ ] **Step 4: `SetupWizardState.kt` ändern**

(a) Die Aufzählung der Quellen ersetzen und den Spender danach einfügen:

```kotlin
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
```

(b) Die Klassenzeile ersetzen. Alt:

```kotlin
class SetupWizardState(prefs: ChatPreferences) {
```

Neu:

```kotlin
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
```

Im KDoc der Klasse den Parameter ergänzen:

```kotlin
 * @param donors other configured profiles the login or the station can be taken from
```

(c) Die fünf gespeicherten Stationswerte ersetzen. Alt:

```kotlin
    /** The locator the profile holds, for the label of "leave unchanged". */
    val storedLocator: String = prefs.getStn_loginLocatorMainCat().orEmpty()
    private val storedBands: Set<WizardBand> = WizardBand.entries.filter { it.isActive(prefs) }.toSet()
    private val storedCategory: Int? = prefs.getLoginChatCategoryMain()?.categoryNumber
    private val storedSecondChat: Boolean = prefs.isLoginToSecondChatEnabled()
    private val storedSecondCategory: Int? = prefs.getLoginChatCategorySecond()?.categoryNumber
```

Neu:

```kotlin
    /** The locator the profile holds, for the label of "leave unchanged". */
    val storedLocator: String = ownStation.locator
```

(d) Die Quellenlisten ersetzen. Alt:

```kotlin
    /** A login can only be kept where an operator entered one. */
    val loginSources: List<WizardSource> = buildList {
        add(WizardSource.NEW)
        if (loginConfigured) add(WizardSource.KEEP)
    }
```

Neu:

```kotlin
    /** A login can be taken where another profile has one, and kept where an operator entered one. */
    val loginSources: List<WizardSource> = buildList {
        add(WizardSource.NEW)
        if (donors.isNotEmpty()) add(WizardSource.TAKE)
        if (loginConfigured) add(WizardSource.KEEP)
    }
```

und in `stationSources` nach `add(WizardSource.NEW)` einfügen:

```kotlin
        if (donors.isNotEmpty()) add(WizardSource.TAKE)
```

(e) Die beiden Zeilen `private var appliedLoginSource…` und `private var appliedStationSource…` ersetzen durch:

```kotlin
    /** The profile the login is taken from; decided with the source when there is only one. */
    var loginDonor: WizardDonor? by mutableStateOf(null)
        private set

    /** The profile the station is taken from; decided with the source when there is only one. */
    var stationDonor: WizardDonor? by mutableStateOf(null)
        private set

    /* A group is reloaded when its source or its donor changes, and only then. */
    private var appliedLogin: Pair<WizardSource?, WizardDonor?> = null to null
    private var appliedStation: Pair<WizardSource?, WizardDonor?> = null to null
```

(f) `val antenna = WizardAntenna(prefs)` und `val integrations = WizardIntegrations(prefs)` samt ihren Kommentaren ersetzen durch:

```kotlin
    /**
     * Antenna and power; always proposals from the profile the station comes from. Replaced
     * when the station's source changes, so nothing typed under one choice shows under another.
     */
    var antenna: WizardAntenna by mutableStateOf(WizardAntenna(prefs))
        private set

    /** Log program, rotator and AirScout; replaced like [antenna]. */
    var integrations: WizardIntegrations by mutableStateOf(WizardIntegrations(prefs))
        private set
```

(g) `chooseLoginSource`, `chooseStationSource` und `applySources` ersetzen durch:

```kotlin
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
```

(h) In `canGoNext` die Zeile für die Einstiegsseite ersetzen. Alt:

```kotlin
            WizardPage.ENTRY -> loginSource != null && stationSource != null
```

Neu:

```kotlin
            WizardPage.ENTRY -> decided(loginSource, loginDonor) && decided(stationSource, stationDonor)
```

(i) `switchedOff` ersetzen:

```kotlin
    /**
     * What is on in the own profile and will be off after Save, for the summary. Always
     * against the own profile: with a station taken from another one, that is what the
     * operator loses, whatever the other profile had.
     *
     * A page that is not part of the run contributes what its profile holds -- the own one
     * for a station left unchanged, the donor's for a station taken over.
     */
    val switchedOff: List<WizardSwitch>
        get() {
            val after = buildSet {
                if (secondChat) add(WizardSwitch.SECOND_CHAT)
                addAll(if (logShown) integrations.answeredLogSwitches() else integrations.storedLogSwitches())
                addAll(if (gearShown) integrations.answeredGearSwitches() else integrations.storedGearSwitches())
            }
            val before = ownSwitches + if (ownStation.secondChat) setOf(WizardSwitch.SECOND_CHAT) else emptySet()
            return WizardSwitch.entries.filter { it in before && it !in after }
        }
```

(j) In `writeTo` unmittelbar vor der Zeile `prefs.setStn_loginLocatorMainCat(locator.trim())` einfügen:

```kotlin
        // The whole station of the other profile first, also what no page asks about; the
        // answers below then lie on top, so a taken value the operator changed wins.
        if (stationSource == WizardSource.TAKE) {
            stationDonor?.let { copyStation(it.prefs, prefs) }
        }

```

und im KDoc von `writeTo` ergänzen:

```kotlin
     *
     * A station taken from another profile is copied whole before the answers are written.
```

(k) Wo `storedSecondChat` sonst noch gelesen wird, `ownStation.secondChat` einsetzen. Nach dieser Aufgabe darf es in der Datei kein `storedBands`, `storedCategory`, `storedSecondChat`, `storedSecondCategory`, `appliedLoginSource` und `appliedStationSource` mehr geben.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.*'`
Expected: alle grün, 0 Fehler, 0 übersprungen. `SetupWizardDonorTest` 13 Tests. `WizardIntegrationsTest` hat dieselbe Zahl wie vorher (zwei gelöscht, zwei neu). Die Zahlen von `SetupWizardStateTest` und `SetupWizardWriteTest` sind unverändert: die Stufen 1 und 2 müssen sich ohne Spender genau wie vorher verhalten. Schlägt dort ein Test fehl, ist das ein Fehler dieser Aufgabe und kein Anlass, den Test anzupassen.

`SetupWizardContent.kt` benutzt `WizardSource` in zwei `when`-Ausdrücken, die nach dieser Aufgabe nicht mehr vollständig sind. Damit der Bau grün bleibt, dort je einen Zweig ergänzen, der bis Task 3 nur den Namen des Spenders zeigt:

```kotlin
                        WizardSource.TAKE -> state.loginDonor?.displayName.orEmpty()
```

bzw. `state.stationDonor?.displayName.orEmpty()`. Task 3 ersetzt beide.

- [ ] **Step 6: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardIntegrations.kt app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardIntegrationsTest.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardDonorTest.kt
git commit -m "Let the setup wizard take login and station from another profile" -m "Written-by: <your model name>
Written-by: Claude Fable 5.2"
```

---

### Task 3: Spender laden und auf der Einstiegsseite anbieten

**Files:**
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardWindow.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt`
- Modify: `app-desktop/src/main/i18n/strings_en.properties`, `strings_de.properties`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardLauncherTest.kt` (vier ergänzen)
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardContentScreenTest.kt` (zwei ergänzen)

**Interfaces:**
- Consumes: `WizardDonor`, `SetupWizardState(prefs, donors)` (Task 2); `OperatorProfileStore().loadProfiles()`, `OperatorProfilePaths.preferencesRelativeFileName(OperatorProfile)`, `ApplicationFileUtils.getFilePath(String, String)`, `ApplicationConstants.APPLICATION_NAME`, `LoginConfiguration.isUnconfigured`.
- Produces, in `SetupWizardWindow.kt`:
  - `internal class DonorCandidate(val profileId: String, val displayName: String, val file: Path)`
  - `internal fun loadDonors(candidates: List<DonorCandidate>): List<WizardDonor>`
  - `SetupWizardLauncher.run(prefs, forced, donors: () -> List<WizardDonor>, show)` — die bisherige Form mit drei Parametern bleibt als Überladung ohne Spender
  - Die Signatur von `SetupWizardLauncher.runIfNeeded(String, Boolean)` bleibt, weil Java sie aufruft.
- Texte: `wizard.source.loginTake`, `wizard.source.loginTakeFrom`, `wizard.source.stationTake`, `wizard.source.stationTakeFrom`, `wizard.entry.donor`, `wizard.summary.takenFrom`.

- [ ] **Step 1: Texte anlegen**

An das Ende von `strings_en.properties` anfügen:

```properties
wizard.source.loginTake=Take the login from profile "{0}" ({1})
wizard.source.loginTakeFrom=Take the login from another profile
wizard.source.stationTake=Take the station from profile "{0}" (locator {1})
wizard.source.stationTakeFrom=Take the station from another profile
wizard.entry.donor=Profile
wizard.summary.takenFrom=Taken from profile "{0}"
```

An das Ende von `strings_de.properties` anfügen:

```properties
wizard.source.loginTake=Anmeldung von Profil „{0}" übernehmen ({1})
wizard.source.loginTakeFrom=Anmeldung von einem anderen Profil übernehmen
wizard.source.stationTake=Station von Profil „{0}" übernehmen (Locator {1})
wizard.source.stationTakeFrom=Station von einem anderen Profil übernehmen
wizard.entry.donor=Profil
wizard.summary.takenFrom=Übernommen von Profil „{0}"
```

- [ ] **Step 2: Write the failing tests**

(a) In `SetupWizardLauncherTest.kt` an das Ende der Klasse anfügen:

```kotlin
    private fun profileFile(name: String, callsign: String, password: String): Path {
        val file = directory.resolve(name)
        ChatPreferences::class.java.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE).use { template ->
            Files.copy(requireNotNull(template), file, StandardCopyOption.REPLACE_EXISTING)
        }
        ChatPreferences(file).apply {
            assertTrue(readPreferencesFromXmlFile())
            setStn_loginCallSign(callsign)
            setStn_loginPassword(password)
            assertTrue(writePreferencesToXmlFile())
        }
        return file
    }

    @Test
    fun onlyConfiguredReadableProfilesBecomeDonors() {
        val configured = profileFile("configured.xml", "DL0TEST", "secret")
        val emptyLogin = profileFile("empty.xml", "", "")
        val template = profileFile("template.xml", "DO5AMF", "changeMe")
        val garbage = directory.resolve("garbage.xml").also { Files.writeString(it, "this is not xml <<<") }

        val donors = loadDonors(
            listOf(
                DonorCandidate("a", "Conteststation", configured),
                DonorCandidate("b", "Leer", emptyLogin),
                DonorCandidate("c", "Vorlage", template),
                DonorCandidate("d", "Kaputt", garbage),
                DonorCandidate("e", "Fehlt", directory.resolve("missing.xml")),
            )
        )

        assertEquals(listOf("a"), donors.map { it.profileId })
        assertEquals("Conteststation", donors.single().displayName)
        assertEquals("DL0TEST", donors.single().callsign)
    }

    @Test
    fun aMissingDonorFileIsNotCreated() {
        val missing = directory.resolve("missing.xml")

        loadDonors(listOf(DonorCandidate("e", "Fehlt", missing)))

        assertFalse(Files.exists(missing), "looking for a donor must not create its file")
    }

    @Test
    fun readingADonorLeavesItsFileAlone() {
        val configured = profileFile("configured.xml", "DL0TEST", "secret")
        val before = Files.readAllBytes(configured)

        loadDonors(listOf(DonorCandidate("a", "Conteststation", configured)))

        assertArrayEquals(before, Files.readAllBytes(configured))
    }

    @Test
    fun donorsAreOnlyLoadedWhenTheWizardOpens() {
        var loaded = 0
        val donors = { loaded++; emptyList<WizardDonor>() }

        SetupWizardLauncher.run(ChatPreferences(configuredFile()), forced = false, donors, neverShown)
        assertEquals(0, loaded, "a configured profile starts without reading other profiles")

        var seen: List<WizardDonor>? = null
        SetupWizardLauncher.run(ChatPreferences(templateFile()), forced = false, donors) { state, _ ->
            seen = state.donors
            SetupWizardOutcome.LATER
        }
        assertEquals(1, loaded)
        assertEquals(emptyList<WizardDonor>(), seen)
    }
```

Fehlt in der Datei ein Import für `assertFalse`, ihn ergänzen.

(b) In `SetupWizardContentScreenTest.kt` die Hilfe `show` um einen Parameter erweitern lassen ist nicht nötig: der Zustand trägt die Spender. An das Ende der Klasse anfügen:

```kotlin
    private fun donor(id: String, name: String, callsign: String, locator: String) =
        WizardDonor(id, name, ChatPreferences(Path.of("setup-wizard-screen-donor-unused.xml")).apply {
            setStn_loginCallSign(callsign)
            setStn_loginPassword("secret")
            setStn_loginLocatorMainCat(locator)
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
        })

    private fun emptyProfile() =
        ChatPreferences(Path.of("setup-wizard-screen-test-unused.xml")).apply {
            setStn_loginCallSign("")
            setStn_loginPassword("")
            setStn_loginLocatorMainCat("JO54ME")
            setStn_bandActive144(true)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(false)
        }

    @Test
    fun aSingleDonorIsNamedInTheOptionAndTakenWithOneClick() = runComposeUiTest {
        val state = SetupWizardState(emptyProfile(), listOf(donor("a", "Conteststation", "DL0TEST", "JO50AA")))
        show(state)

        onNodeWithText("Take the login from profile \"Conteststation\" (DL0TEST)").performClick()
        onNodeWithText("Take the station from profile \"Conteststation\" (locator JO50AA)").performClick()

        assertEquals(WizardSource.TAKE, state.loginSource)
        assertEquals("a", state.loginDonor?.profileId)
        assertEquals(WizardSource.TAKE, state.stationSource)

        onNodeWithText("Next").performClick()
        assertEquals(WizardPage.PERSONAL, state.page)
        assertEquals("DL0TEST", state.callsign)
    }

    @Test
    fun withSeveralDonorsNextWaitsForAProfile() = runComposeUiTest {
        val state = SetupWizardState(
            emptyProfile(),
            listOf(donor("a", "Conteststation", "DL0TEST", "JO50AA"), donor("b", "Portabel", "DL0TEST/P", "JN58QA")),
        )
        show(state)

        onNodeWithText("Take the login from another profile").performClick()
        onNodeWithText("Set up a new station").performClick()
        onNodeWithText("Next").performClick()

        assertEquals(WizardPage.ENTRY, state.page, "which profile is not said yet")
    }
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :app-desktop:compileTestKotlin`
Expected: Kompilierfehler, `Unresolved reference: loadDonors`.

- [ ] **Step 4: `SetupWizardWindow.kt` ändern**

Importe ergänzen:

```kotlin
import kst4contest.ApplicationConstants
import kst4contest.controller.OperatorProfilePaths
import kst4contest.controller.OperatorProfileStore
import kst4contest.utils.ApplicationFileUtils
import java.nio.file.Files
import java.nio.file.Path
```

Den Pfad von `ApplicationFileUtils` und `ApplicationConstants` vor dem Schreiben mit `grep -rn 'class ApplicationFileUtils\|class ApplicationConstants' core/src/main/java` prüfen und die Importe danach richten.

Vor `object SetupWizardLauncher` einfügen:

```kotlin
/** Another profile that might serve as a donor: who it is and where its preferences lie. */
internal class DonorCandidate(val profileId: String, val displayName: String, val file: Path)

/**
 * Reads the candidates that can serve as donors: the file exists, can be read, and holds a
 * login an operator entered.
 *
 * A candidate's file is opened through `ChatPreferences(Path)` and never through the
 * constructor that takes a name below the application directory: that one seeds a missing
 * file from the bundled template, and looking for a donor must not create a profile's file.
 * Nothing here writes.
 */
internal fun loadDonors(candidates: List<DonorCandidate>): List<WizardDonor> =
    candidates.mapNotNull { candidate ->
        if (!Files.isRegularFile(candidate.file)) return@mapNotNull null

        val prefs = ChatPreferences(candidate.file)
        // A throwing read counts as failed, as for the profile being set up.
        if (!runCatching { prefs.readPreferencesFromXmlFile() }.getOrDefault(false)) return@mapNotNull null
        if (LoginConfiguration.isUnconfigured(prefs)) return@mapNotNull null

        WizardDonor(candidate.profileId, candidate.displayName, prefs)
    }
```

In `SetupWizardLauncher` die Funktion `runIfNeeded` ersetzen und die Hilfe darunter einfügen:

```kotlin
    @JvmStatic
    fun runIfNeeded(preferencesRelativeFileName: String, forced: Boolean): SetupWizardOutcome =
        run(
            ChatPreferences(preferencesRelativeFileName),
            forced,
            { loadDonors(otherProfiles(preferencesRelativeFileName)) },
            SetupWizardWindow::show,
        )

    /**
     * Every registered profile but the one being set up, with the file its preferences lie
     * in. Empty on an installation without a profile registry: there is only the root
     * profile then, and it is the active one.
     */
    private fun otherProfiles(activeRelativeFileName: String): List<DonorCandidate> =
        OperatorProfileStore().loadProfiles().mapNotNull { profile ->
            val relativeFileName = OperatorProfilePaths.preferencesRelativeFileName(profile)
            if (relativeFileName == activeRelativeFileName) return@mapNotNull null

            DonorCandidate(
                profile.profileId,
                profile.displayName,
                Path.of(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, relativeFileName)),
            )
        }
```

Die bisherige Funktion `run(prefs, forced, show)` durch diese beiden ersetzen:

```kotlin
    /** Without other profiles; for callers and tests that have none. */
    internal fun run(
        prefs: ChatPreferences,
        forced: Boolean,
        show: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome,
    ): SetupWizardOutcome = run(prefs, forced, { emptyList() }, show)

    /**
     * @param prefs bound to the profile's file and not read yet
     * @param donors reads the other profiles; called only once the wizard is certain to
     *        open, so an ordinary start reads no file but its own
     * @param show opens the window; replaced in the tests
     */
    internal fun run(
        prefs: ChatPreferences,
        forced: Boolean,
        donors: () -> List<WizardDonor>,
        show: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome,
    ): SetupWizardOutcome {
        /*
         * A file that cannot be read is not a profile to set up: the wizard would start from
         * compiled-in defaults and Save would write them over whatever the file holds. The
         * runtime deals with such a file as it always did. A throwing read counts as failed too.
         */
        if (!runCatching { prefs.readPreferencesFromXmlFile() }.getOrDefault(false)) {
            return SetupWizardOutcome.NOT_SHOWN
        }

        if (!forced && !LoginConfiguration.isUnconfigured(prefs)) return SetupWizardOutcome.NOT_SHOWN

        return show(SetupWizardState(prefs, donors()), prefs)
    }
```

- [ ] **Step 5: `SetupWizardContent.kt` ändern**

(a) `EntryPage` ganz ersetzen:

```kotlin
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
```

Damit entfallen die beiden vorläufigen `TAKE`-Zweige aus Task 2.

(b) In `SummaryPage` als erste Zeile im Abschnitt `Form.section(strings.wizardSummaryLogin) {` einfügen:

```kotlin
        if (state.loginSource == WizardSource.TAKE) {
            state.loginDonor?.let { Text(strings.wizardSummaryTakenFrom(it.displayName)) }
        }
```

und als erste Zeile im Abschnitt `Form.section(strings.wizardSummaryStation) {`:

```kotlin
        if (state.stationSource == WizardSource.TAKE) {
            state.stationDonor?.let { Text(strings.wizardSummaryTakenFrom(it.displayName)) }
        }
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.*' --tests 'kst4contest.view.i18n.*'`
Expected: alle grün, 0 Fehler, 0 übersprungen. `SetupWizardLauncherTest` vier Tests mehr, `SetupWizardContentScreenTest` zwei mehr als vorher; die Zahl vorher und nachher nennen.

Findet `onNodeWithText` die Option mit den Anführungszeichen nicht, den erzeugten Text prüfen (`MessageFormat` lässt `"` stehen) und melden, was tatsächlich gezeichnet wird; den Text in der Sprachdatei nicht umbauen, ohne es zu sagen.

- [ ] **Step 7: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardWindow.kt app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt app-desktop/src/main/i18n/strings_en.properties app-desktop/src/main/i18n/strings_de.properties app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardLauncherTest.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardContentScreenTest.kt
git commit -m "Offer other profiles as a source on the setup wizard's entry page" -m "Written-by: <your model name>
Written-by: Claude Fable 5.2"
```

---

### Task 4: Dokumentation und Gesamtbau

**Files:**
- Modify: `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md`
- Modify: `docs/PROJECT_CONTEXT.md`
- Modify: `github_docs/de-Konfiguration.md`, `github_docs/en-Configuration.md`

**Interfaces:**
- Consumes: das fertige Verhalten aus Task 1 bis 3.
- Produces: nichts, worauf Code aufbaut.

Die „Alt"-Ausschnitte stammen aus dem Stand `68d86ce3`. Weicht eine Zeile ab, die Stelle gleichen Inhalts ersetzen und die Abweichung nennen. Fehlt eine Stelle ganz: NEEDS_CONTEXT melden mit dem, was dort steht.

- [ ] **Step 1: Spec nachziehen**

In `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md` im Abschnitt „Ausbaustufen" den Satz „Stufe 1 und 2 sind umgesetzt. Offen ist Stufe 3." ersetzen durch:

```markdown
Alle drei Stufen sind umgesetzt.
```

Im Abschnitt „Mechanismus" nach dem Punkt, der mit „**Übernehmen von Profil X**" beginnt, einen Punkt einfügen:

```markdown
- **Was „Station" beim Übernehmen heißt**, steht Zeile für Zeile in
  `app-desktop/src/test/resources/wizard-station-setters.txt`: jeder Setter von
  `ChatPreferences` ist dort oder in `wizard-other-setters.txt` eingeordnet, und ein Test
  schlägt fehl, wenn ein neuer Setter in keiner der beiden Listen steht. Übernommen wird die
  ganze Station, auch was der Assistent nie fragt und was seine Fragen nicht ausdrücken
  können (Rotor 2, SO2R, mehrere Logquellen). „Wird abgeschaltet" vergleicht dabei immer mit
  dem eigenen Profil. Als Spender gilt nur ein Profil, dessen Datei vorhanden und lesbar ist
  und das eine eigene Anmeldung hat; gelesen wird es über `ChatPreferences(Path)`, damit
  eine fehlende Datei nicht aus der Vorlage angelegt wird.
```

- [ ] **Step 2: `docs/PROJECT_CONTEXT.md`**

Im Punkt „The setup wizard writes `preferences.xml` …" den Satz, der mit „The reasons and the open stage 3 are in" beginnt, ersetzen durch:

```markdown
Login and station can
  each be taken from another configured profile; "station" then means every setting listed
  in `app-desktop/src/test/resources/wizard-station-setters.txt`, also those no page asks
  about, and a test fails when a new `ChatPreferences` setter is in neither that list nor
  `wizard-other-setters.txt`. A new setting therefore needs that one decision: does another
  operator at the same station inherit it. The other profile's file is only read, through
  `ChatPreferences(Path)`, so a missing one is not seeded from the template. The reasons are
  in `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md`.
```

- [ ] **Step 3: Handbuch, deutsch**

In `github_docs/de-Konfiguration.md` im Abschnitt „Einrichtungsassistent" den Absatz, der mit „Hat das Profil schon eine Station (etwa nach dem Duplizieren)" beginnt, ersetzen durch:

```markdown
Gibt es schon ein eingerichtetes Profil oder hat das Profil selbst schon eine Station, fragt die erste Seite getrennt für **Anmeldung** und **Station**, woher die Angaben kommen:

- **neu eingeben** – die Schritte erscheinen, eine neue Station beginnt leer;
- **von einem anderen Profil übernehmen** – die Werte kommen aus dem gewählten Profil;
- **unverändert lassen** – das Profil behält, was es hat.

Damit sind die üblichen Fälle mit wenigen Klicks erledigt:

| Fall | Anmeldung | Station |
|---|---|---|
| Mehrmannbetrieb, gemeinsames Rufzeichen | übernehmen | übernehmen |
| Mehrmannbetrieb, eigenes Rufzeichen | neu | übernehmen |
| Ein Operator, mehrere Standorte | übernehmen | neu |

Übernommen wird beim Punkt **Station** die ganze Station: Locator, Bänder, Chatkategorien, Antennen- und Pfadanalysewerte, Logprogramm, Rotoren und AirScout, auch Einzelheiten, die der Assistent nie fragt. Sprache, Anzeigename, Fensteranordnung, Farben, Kurzbefehle und Baken gehören zum Operator und werden nie übernommen. Schaltet das Übernehmen etwas ab, das im eigenen Profil eingeschaltet war, steht es in der Zusammenfassung unter „Wird abgeschaltet".

Übernommene Werte lassen sich in der Zusammenfassung über **Ändern** anpassen, etwa ein `/P` am Rufzeichen. Das andere Profil wird dabei nur gelesen und nie verändert. Angeboten werden nur Profile, die selbst eine Anmeldung haben.
```

- [ ] **Step 4: Handbuch, englisch**

In `github_docs/en-Configuration.md` im Abschnitt „Setup Assistant" den Absatz, der mit „If the profile already has a station (after duplicating one, for example)" beginnt, ersetzen durch:

```markdown
If a configured profile already exists, or the profile itself already has a station, the first page asks separately for **login** and **station** where the entries come from:

- **enter new** – the steps appear, a new station starts empty;
- **take from another profile** – the values come from the chosen profile;
- **leave unchanged** – the profile keeps what it has.

This settles the usual cases with a few clicks:

| Case | Login | Station |
|---|---|---|
| Multi-operator, shared callsign | take | take |
| Multi-operator, own callsign | new | take |
| One operator, several sites | take | new |

For **station**, the whole station is taken: locator, bands, chat categories, antenna and path-analysis values, log program, rotators and AirScout, including details the assistant never asks about. Language, display name, window layout, colours, shortcuts and beacons belong to the operator and are never taken. If taking over switches off something that was enabled in the own profile, the summary lists it under "Will be switched off".

Taken values can be adjusted in the summary through **Change**, for example a `/P` on the callsign. The other profile is only read and never changed. Only profiles that have a login of their own are offered.
```

- [ ] **Step 5: Der ganze Bau**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL.

```bash
grep -l '<failure\|<error' core/build/test-results/test/*.xml app-desktop/build/test-results/test/*.xml i18n-generator/build/test-results/test/*.xml
```

Expected: keine Ausgabe. Die Summen `tests`, `skipped`, `failures`, `errors` je Modul aus der XML nennen.

```bash
grep -c '<violation' app-desktop/build/reports/pmd/main.xml
grep -c '<BugInstance' app-desktop/build/reports/spotbugs/main.xml
```

Beide Zahlen nennen und die Funde aufführen, die in `WizardStationCopy.kt` oder den geänderten Teilen der übrigen Assistentendateien liegen. Kein Vergleich mit `main`, kein zweiter Checkout.

- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md docs/PROJECT_CONTEXT.md github_docs/de-Konfiguration.md github_docs/en-Configuration.md
git commit -m "Document taking login and station from another profile" -m "Written-by: <your model name>
Written-by: Claude Fable 5.2"
```

- [ ] **Step 7: Von Hand prüfen (Nutzer, kein Agent)**

Start mit `./gradlew :app-desktop:run`; vorher `~/.praktiKST` sichern (das ganze Verzeichnis, weil mehrere Profile gelesen werden).

1. Neues Profil anlegen und dorthin wechseln. Erwartet: die Einstiegsseite bietet für Anmeldung „neu" und „von Profil … übernehmen", für Station zusätzlich „unverändert". Mit nur einem anderen Profil steht dessen Name samt Rufzeichen bzw. Locator in der Option.
2. Beides übernehmen, Namen eintragen, „Speichern". Erwartet: Reiter Station, Log-Synch, TRX-Synch und AirScout zeigen die Werte des anderen Profils, auch Rotor 2 und SO2R, falls dort gesetzt.
3. Das andere Profil danach öffnen. Erwartet: unverändert. `diff` seiner `preferences.xml` gegen die Sicherung zeigt nichts.
4. Mit zwei anderen Profilen: Erwartet: nach „von einem anderen Profil übernehmen" erscheint eine Auswahl, „Weiter" geht erst mit gewähltem Profil.
5. Anmeldung übernehmen, in der Zusammenfassung über „Ändern" ein `/P` anhängen, speichern. Erwartet: das eigene Profil hat das `/P`, das andere nicht.
6. Testprofile löschen.

- [ ] **Step 8: Den Plan entfernen (Controller, nach der Handprüfung)**

```bash
git rm docs/superpowers/plans/2026-11-18-einrichtungsassistent-stufe-3.md
git commit -m "Remove the executed setup wizard stage 3 plan" -m "Written-by: Claude Fable 5.2"
```

Danach den Text von PR #174 nachziehen: alle drei Stufen enthalten.
