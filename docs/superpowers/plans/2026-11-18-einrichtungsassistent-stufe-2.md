# Einrichtungsassistent, Ausbaustufe 2 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Der Einrichtungsassistent fragt zusätzlich Antenne und Leistung, Logprogramm, Rotor und AirScout ab; eine neu eingerichtete Station hat danach nur eingeschaltet, was der Nutzer bejaht hat.

**Architecture:** Zwei neue, fensterfreie Antwortklassen (`WizardAntenna`, `WizardIntegrations`) halten die Antworten der drei neuen Seiten, prüfen sie und schreiben sie in `ChatPreferences`. `SetupWizardState` bindet sie in den Ablauf ein und schreibt eine Seite nur, wenn sie in diesem Lauf gezeigt wurde. Der Weg bleibt der von Stufe 1: schreiben in `preferences.xml` vor dem Bau der Laufzeit, keine laufende Sitzung, kein `SettingsDraft`.

**Tech Stack:** Kotlin 2.2 und Compose Multiplatform 1.8 (`app-desktop`), JUnit 5, Gradle-Wrapper, Java 21.

**Spec:** `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md` — dieser Plan setzt **Ausbaustufe 2** um (Schritte 4 bis 6 der Spec). Stufe 3 („von Profil X übernehmen") bleibt offen.

## Global Constraints

- Zweig `feature/setup-wizard`, offener PR #174. **Kein Merge**, bevor das Feature fertig und CI grün ist. Commits lokal; Push nur auf Wort des Nutzers.
- Gradle, nicht Maven. Java 21. Bau und Tests über `./gradlew`.
- Quelltextkommentare und Javadoc ausschließlich Englisch. Handbuch-Deutsch in neuer Rechtschreibung (`muss`, `dass`).
- Oberflächentexte in `app-desktop/src/main/i18n/strings_en.properties` (Basis) und `strings_de.properties`. Beide Dateien bekommen jeden neuen Schlüssel; ein entfernter Schlüssel verschwindet aus beiden. Kein Apostroph in neuen Texten.
- Programmnamen (`Win-Test`, `UCXLog`, `DXLog.net`, `N1MM+`, `QARTEST`, `PSTRotator`, `AirScout`) und Bandbezeichnungen werden nicht übersetzt.
- Ports, Transporte, Kennungen und Vorgabewerte der Integrationen bleiben, wie sie sind. Der Assistent schaltet ein und aus und übernimmt Zahlen; er erfindet keine.
- **Startwerte einer neuen Station:** kein Logprogramm, keine Frequenzübernahme, kein lokaler DX-Cluster-Server, kein Rotor, kein AirScout. Ports, Namen, Host und Kennungen bleiben mit den gespeicherten Werten vorbelegt.
- **Antenne und Leistung** haben keinen leeren Zustand: die Zahlen sind immer mit den gespeicherten Werten vorbelegt, auch bei einer neuen Station. Leere Standorthöhe heißt „Höhenmodell".
- **Nur gezeigte Seiten werden geschrieben.** Bleibt die Station unverändert und öffnet der Nutzer keine der drei neuen Seiten, ändert der Assistent an Antenne, Logprogramm, Rotor und AirScout nichts.
- Aus der Bandwahl wird keine Frequenz abgeleitet. Kein Probe-Login, keine UDP-Erkennung.
- Nie blockierend auf dem AWT-Ereignisfaden warten. An `SetupWizardWindow.kt`, `ApplicationRuntimeLauncher.java` und `Kst4ContestApplication.java` ändert dieser Plan nichts.
- Unverändert bleiben: das Format von `preferences.xml`, das Einstellungsfenster, `SettingsDraft.apply`, alle Reiterzustände, `praktiKSTpreferences.xml`.
- Agenten starten das Programm nie (`:app-desktop:run`) und fassen `~/.praktiKST` nicht an. Handprüfungen bleiben beim Nutzer.
- Ein Testfehler bricht den Bau, PMD und SpotBugs nicht. Ergebnis aus der Test-XML unter `*/build/test-results/test/` lesen.

## Review Focus

1. **Gespeicherte Station mit mehreren Logquellen zugleich** (die Vorlage hat Netzwerk-Abgleich und Logdatei an): „unverändert lassen" darf keine davon abschalten, und die Logseite darf sich dann nicht öffnen lassen, weil sie nur eine Quelle kennt. Test in Task 2 (`severalStoredSourcesMakeTheLogPageUneditable`) und Task 3 (`aKeptStationWithSeveralLogSourcesCannotOpenTheLogPage`, `aKeptStationKeepsEveryIntegrationAsItWas`).
2. **Port außerhalb 1–65535 oder leerer Pflichtwert auf einer freiwilligen Seite**: „Weiter" und „Speichern" bleiben gesperrt, obwohl die Seite freiwillig ist. Test in Task 2 (`anInvalidPortMakesItsPageIncomplete`) und Task 3 (`anInvalidOptionalPageBlocksSaving`).
3. **„Fertig" auf der Standortseite einer neuen Station**: die übersprungenen Seiten gelten trotzdem, also werden die Integrationen abgeschaltet und die Zusammenfassung sagt es. Test in Task 3 (`finishingEarlyStillSwitchesANewStationsIntegrationsOff`).
4. **Standorthöhe über NN als Text** („250,5", leer, „abc", „99999"): Komma wird angenommen, leer heißt Höhenmodell, Unsinn und Werte außerhalb −500…9000 werden abgelehnt und nicht als 0 gespeichert. Test in Task 1 (`theSiteElevationIsReadTheWayTheStationTabReadsIt`).
5. **Rotorport neben dem Port von Rotor 2 bei eingeschaltetem SO2R**: wird abgelehnt wie im Reiter Station. Test in Task 2 (`aRotatorPortNextToRotatorTwoIsRefusedOnlyWithSo2r`).

---

## Dateien

| Datei | Aufgabe |
|---|---|
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardAntenna.kt` (neu) | Antworten der Seite „Antenne und Leistung" |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardIntegrations.kt` (neu) | Antworten der Seiten „Logprogramm" und „Rotor und AirScout" |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt` | drei neue Seiten im Ablauf, Schreiben, Liste des Abgeschalteten |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt` | drei neue Seiten, erweiterte Zusammenfassung |
| `app-desktop/src/main/i18n/strings_en.properties`, `strings_de.properties` | neue Texte, zwei entfallen |
| `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/*Test.kt` | Tests |
| `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md`, `docs/PROJECT_CONTEXT.md`, `github_docs/de-Konfiguration.md`, `github_docs/en-Configuration.md` | Dokumentation |

---

### Task 1: Antworten zu Antenne und Leistung

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardAntenna.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardAntennaTest.kt`

**Interfaces:**
- Consumes: Getter und Setter von `ChatPreferences` für `stn_antennaBeamWidthDeg`, `stn_pathAnalysisOwnAntennaGainDbi`, `stn_pathAnalysisOwnAntennaHeightMeters`, `stn_pathAnalysisOwnTxPowerWatts`, `stn_pathAnalysisOwnHeightAslMeters`; `StationTabState.parseOwnHeightAsl(String): Double?` (Paket `kst4contest.view.compose.tabs`).
- Produces: `class WizardAntenna(prefs: ChatPreferences)` mit
  - `var beamWidthDeg: Double`, `var gainDbi: Double`, `var heightMeters: Double`, `var txPowerWatts: Double`
  - `var heightAslText: String`
  - `val heightAslRefused: Boolean`, `val complete: Boolean`
  - `fun writeTo(prefs: ChatPreferences)`

- [ ] **Step 1: Write the failing test**

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * The antenna and power answers: always proposals taken from what is stored, never empty,
 * and the site elevation read the way the Station tab reads it.
 */
class WizardAntennaTest {

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("wizard-antenna-test-unused.xml")).apply(configure)

    private fun stored() = prefs {
        setStn_antennaBeamWidthDeg(28.0)
        setStn_pathAnalysisOwnAntennaGainDbi(16.5)
        setStn_pathAnalysisOwnAntennaHeightMeters(18.0)
        setStn_pathAnalysisOwnTxPowerWatts(400.0)
        setStn_pathAnalysisOwnHeightAslMeters(612.0)
    }

    @Test
    fun theAnswersStartAsTheStoredValues() {
        val antenna = WizardAntenna(stored())

        assertEquals(28.0, antenna.beamWidthDeg)
        assertEquals(16.5, antenna.gainDbi)
        assertEquals(18.0, antenna.heightMeters)
        assertEquals(400.0, antenna.txPowerWatts)
        assertEquals("612", antenna.heightAslText, "a whole number is shown without a decimal point")
        assertTrue(antenna.complete)
    }

    @Test
    fun anUnsetSiteElevationIsAnEmptyField() {
        val antenna = WizardAntenna(prefs { setStn_pathAnalysisOwnHeightAslMeters(Double.NaN) })

        assertEquals("", antenna.heightAslText)
        assertFalse(antenna.heightAslRefused, "empty means the elevation model, not an error")
        assertTrue(antenna.complete)
    }

    @Test
    fun aFractionalSiteElevationKeepsItsFraction() {
        val antenna = WizardAntenna(prefs { setStn_pathAnalysisOwnHeightAslMeters(250.5) })

        assertEquals("250.5", antenna.heightAslText)
    }

    @Test
    fun theSiteElevationIsReadTheWayTheStationTabReadsIt() {
        val antenna = WizardAntenna(stored())

        listOf("", "  ", "250", "250,5", "250.5", "-100").forEach {
            antenna.heightAslText = it
            assertFalse(antenna.heightAslRefused, "'$it' must be accepted")
        }
        listOf("abc", "99999", "-501", "12 m").forEach {
            antenna.heightAslText = it
            assertTrue(antenna.heightAslRefused, "'$it' must be refused")
            assertFalse(antenna.complete)
        }
    }

    @Test
    fun aNumberThatIsNotOneMakesThePageIncomplete() {
        val antenna = WizardAntenna(stored())

        antenna.gainDbi = Double.NaN
        assertFalse(antenna.complete)

        antenna.gainDbi = 12.0
        antenna.txPowerWatts = Double.POSITIVE_INFINITY
        assertFalse(antenna.complete)
    }

    @Test
    fun writingPutsTheAnswersIntoThePreferences() {
        val target = prefs()
        val antenna = WizardAntenna(stored())
        antenna.beamWidthDeg = 35.0
        antenna.heightAslText = "250,5"

        antenna.writeTo(target)

        assertEquals(35.0, target.getStn_antennaBeamWidthDeg())
        assertEquals(16.5, target.getStn_pathAnalysisOwnAntennaGainDbi())
        assertEquals(18.0, target.getStn_pathAnalysisOwnAntennaHeightMeters())
        assertEquals(400.0, target.getStn_pathAnalysisOwnTxPowerWatts())
        assertEquals(250.5, target.getStn_pathAnalysisOwnHeightAslMeters())
    }

    @Test
    fun anEmptySiteElevationIsWrittenAsUnset() {
        val target = prefs { setStn_pathAnalysisOwnHeightAslMeters(612.0) }
        val antenna = WizardAntenna(stored())
        antenna.heightAslText = ""

        antenna.writeTo(target)

        assertTrue(target.getStn_pathAnalysisOwnHeightAslMeters().isNaN(), "empty hands the elevation back to the model")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.WizardAntennaTest'`
Expected: Kompilierfehler, `Unresolved reference: WizardAntenna`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package kst4contest.view.compose.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.tabs.StationTabState

/**
 * The setup wizard's answers about antenna and power.
 *
 * These numbers have no empty state: every answer starts as the stored value and is a
 * proposal the operator may pass unchanged, also when a station is set up anew. They feed the
 * path analysis and the antenna sector on the map; the chat does not need them.
 *
 * The site elevation is the exception in form, not in rule: it is text, because empty is a
 * value of its own -- "let the elevation model decide" -- and it is read exactly as the
 * Station tab reads it.
 *
 * @param prefs the profile's preferences, already read from its file
 */
class WizardAntenna(prefs: ChatPreferences) {

    var beamWidthDeg: Double by mutableStateOf(prefs.getStn_antennaBeamWidthDeg())
    var gainDbi: Double by mutableStateOf(prefs.getStn_pathAnalysisOwnAntennaGainDbi())
    var heightMeters: Double by mutableStateOf(prefs.getStn_pathAnalysisOwnAntennaHeightMeters())
    var txPowerWatts: Double by mutableStateOf(prefs.getStn_pathAnalysisOwnTxPowerWatts())

    /** The site elevation above sea level as the operator types it; empty for the model. */
    var heightAslText: String by mutableStateOf(elevationText(prefs.getStn_pathAnalysisOwnHeightAslMeters()))

    /** A site elevation that is neither empty nor a number in the accepted range. */
    val heightAslRefused: Boolean
        get() = StationTabState.parseOwnHeightAsl(heightAslText) == null

    /** Whether every answer can be stored as it stands. */
    val complete: Boolean
        get() = beamWidthDeg.isFinite() && gainDbi.isFinite() && heightMeters.isFinite() &&
            txPowerWatts.isFinite() && !heightAslRefused

    /** Puts the answers into [prefs]. Call only while [complete]. */
    fun writeTo(prefs: ChatPreferences) {
        prefs.setStn_antennaBeamWidthDeg(beamWidthDeg)
        prefs.setStn_pathAnalysisOwnAntennaGainDbi(gainDbi)
        prefs.setStn_pathAnalysisOwnAntennaHeightMeters(heightMeters)
        prefs.setStn_pathAnalysisOwnTxPowerWatts(txPowerWatts)
        prefs.setStn_pathAnalysisOwnHeightAslMeters(
            StationTabState.parseOwnHeightAsl(heightAslText) ?: Double.NaN
        )
    }

    private companion object {
        /** Empty for "unset"; a whole number without ".0", so the field shows what one types. */
        fun elevationText(meters: Double): String = when {
            meters.isNaN() -> ""
            meters == Math.floor(meters) && !meters.isInfinite() -> meters.toLong().toString()
            else -> meters.toString()
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.WizardAntennaTest'`
Expected: 7 Tests, 0 Fehler in `app-desktop/build/test-results/test/TEST-kst4contest.view.compose.wizard.WizardAntennaTest.xml`.

- [ ] **Step 5: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardAntenna.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardAntennaTest.kt
git commit -m "Add the setup wizard's antenna and power answers"
```

---

### Task 2: Antworten zu Logprogramm, Rotor und AirScout

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardIntegrations.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardIntegrationsTest.kt`

**Interfaces:**
- Consumes: Getter und Setter von `ChatPreferences` für `logsynch_*`, `trxSynch_ucxLogUDPListenerEnabled`, `notify_dxClusterServerEnabled`, `stn_pstRotator*`, `stn_pstRotator2Enabled`, `stn_pstRotator2Port`, `trxSynch_so2rEnabled`, `AirScout_as*`; `AirscoutTabState.isValidPort(Int)`, `AirscoutTabState.isValidIdentifier(String?)`, `StationTabState.portsClash(Int, Int)` (Paket `kst4contest.view.compose.tabs`).
- Produces (Paket `kst4contest.view.compose.wizard`):
  - `enum class WizardLogProgram { NONE, WINTEST, UDP, FILE }`
  - `enum class WizardSwitch { SECOND_CHAT, LOG_WINTEST, LOG_UDP, LOG_FILE, QRG_FOLLOW, CLUSTER_SERVER, ROTATOR, AIRSCOUT }`
  - `class WizardIntegrations(prefs: ChatPreferences)` mit
    - `val logProgram: WizardLogProgram`, `fun chooseLogProgram(WizardLogProgram)`
    - `var udpPort: Int`, `var wintestPort: Int`, `var wintestStationName: String`, `var filePath: String`
    - `var qrgFollow: Boolean`, `var clusterServer: Boolean`, `val networked: Boolean`
    - `var rotator: Boolean`, `var rotatorHost: String`, `var rotatorPort: Int`, `val rotatorPortClashes: Boolean`
    - `var airScout: Boolean`, `var airScoutPort: Int`, `var airScoutServerName: String`, `var airScoutClientName: String`
    - `val severalStoredLogSources: Boolean`
    - `val logComplete: Boolean`, `val gearComplete: Boolean`
    - `fun loadStored()`, `fun loadFresh()`
    - `fun writeLogTo(prefs: ChatPreferences)`, `fun writeGearTo(prefs: ChatPreferences)`
    - `fun switchedOffByLog(): List<WizardSwitch>`, `fun switchedOffByGear(): List<WizardSwitch>`

- [ ] **Step 1: Write the failing test**

```kotlin
package kst4contest.view.compose.wizard

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * The answers about log program, rotator and AirScout: what a new station starts with, what
 * a kept one shows, when a page may be left, and what is written.
 */
class WizardIntegrationsTest {

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("wizard-integrations-test-unused.xml")).apply(configure)

    /** Everything off, so each test states what it switches on. */
    private fun allOff(configure: ChatPreferences.() -> Unit = {}) = prefs {
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
        setLogsynch_ucxUDPWkdCallListenerPort(12060)
        setLogsynch_wintestNetworkPort(9871)
        setLogsynch_wintestNetworkStationNameOfKST("KST")
        setStn_pstRotatorHost("127.0.0.1")
        setStn_pstRotatorPort(12000)
        setStn_pstRotator2Port(12010)
        setAirScout_asCommunicationPort(9872)
        setAirScout_asServerNameString("AS")
        setAirScout_asClientNameString("KST")
        configure()
    }

    /** What the bundled template switches on. */
    private fun likeTheTemplate() = allOff {
        setLogsynch_ucxUDPWkdCallListenerEnabled(true)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        setTrxSynch_ucxLogUDPListenerEnabled(true)
        setNotify_dxClusterServerEnabled(true)
        setAirScout_asUDPListenerEnabled(true)
    }

    private fun winTestStation() = allOff {
        setLogsynch_wintestNetworkListenerEnabled(true)
        setLogsynch_wintestNetworkPort(9999)
        setLogsynch_wintestQrgSyncEnabled(true)
        setNotify_dxClusterServerEnabled(true)
        setStn_pstRotatorEnabled(true)
    }

    @Test
    fun aKeptStationShowsWhatIsStored() {
        val answers = WizardIntegrations(winTestStation())

        assertEquals(WizardLogProgram.WINTEST, answers.logProgram)
        assertEquals(9999, answers.wintestPort)
        assertTrue(answers.qrgFollow)
        assertTrue(answers.clusterServer)
        assertTrue(answers.rotator)
        assertFalse(answers.airScout)
        assertFalse(answers.severalStoredLogSources)
    }

    @Test
    fun aNewStationStartsWithEverythingOffButKeepsTheNumbers() {
        val answers = WizardIntegrations(winTestStation())

        answers.loadFresh()

        assertEquals(WizardLogProgram.NONE, answers.logProgram)
        assertFalse(answers.qrgFollow)
        assertFalse(answers.clusterServer)
        assertFalse(answers.rotator)
        assertFalse(answers.airScout)
        assertEquals(9999, answers.wintestPort, "a port the operator once set is a proposal, not lost")
        assertTrue(answers.logComplete)
        assertTrue(answers.gearComplete)
    }

    @Test
    fun loadingTheStoredValuesAgainUndoesAFreshStart() {
        val answers = WizardIntegrations(winTestStation())
        answers.loadFresh()

        answers.loadStored()

        assertEquals(WizardLogProgram.WINTEST, answers.logProgram)
        assertTrue(answers.rotator)
    }

    @Test
    fun severalStoredSourcesMakeTheLogPageUneditable() {
        val answers = WizardIntegrations(likeTheTemplate())

        assertTrue(answers.severalStoredLogSources, "the template has the network listener and the file on")
        // Shown as one of them, the network one first; the state decides not to offer the page.
        assertEquals(WizardLogProgram.UDP, answers.logProgram)
    }

    @Test
    fun leavingANetworkProgramDropsWhatOnlyANetworkProgramCanDo() {
        val answers = WizardIntegrations(allOff())
        answers.chooseLogProgram(WizardLogProgram.UDP)
        answers.qrgFollow = true
        answers.clusterServer = true
        assertTrue(answers.networked)

        answers.chooseLogProgram(WizardLogProgram.FILE)

        assertFalse(answers.networked)
        assertFalse(answers.qrgFollow)
        assertFalse(answers.clusterServer)
    }

    @Test
    fun aLogFileNeedsAFile() {
        val answers = WizardIntegrations(allOff())

        answers.chooseLogProgram(WizardLogProgram.FILE)
        assertFalse(answers.logComplete)

        answers.filePath = "/home/op/log.txt"
        assertTrue(answers.logComplete)
    }

    @Test
    fun anInvalidPortMakesItsPageIncomplete() {
        val answers = WizardIntegrations(allOff())

        answers.chooseLogProgram(WizardLogProgram.UDP)
        answers.udpPort = 0
        assertFalse(answers.logComplete)
        answers.udpPort = 70000
        assertFalse(answers.logComplete)
        answers.udpPort = 12060
        assertTrue(answers.logComplete)

        answers.chooseLogProgram(WizardLogProgram.WINTEST)
        answers.wintestStationName = "  "
        assertFalse(answers.logComplete, "Win-Test needs a station name to send from")
        answers.wintestStationName = "KST"
        answers.wintestPort = -1
        assertFalse(answers.logComplete)

        // A port of a program that is not chosen is nobody's problem.
        answers.chooseLogProgram(WizardLogProgram.NONE)
        assertTrue(answers.logComplete)

        answers.airScout = true
        answers.airScoutPort = 0
        assertFalse(answers.gearComplete)
        answers.airScoutPort = 9872
        answers.airScoutServerName = "A\"S"
        assertFalse(answers.gearComplete, "a quotation mark breaks the AirScout message")
        answers.airScoutServerName = "AS"
        assertTrue(answers.gearComplete)

        answers.rotator = true
        answers.rotatorHost = " "
        assertFalse(answers.gearComplete)
    }

    @Test
    fun aRotatorPortNextToRotatorTwoIsRefusedOnlyWithSo2r() {
        val withSo2r = WizardIntegrations(allOff {
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
        })
        withSo2r.rotator = true
        withSo2r.rotatorPort = 12011
        assertTrue(withSo2r.rotatorPortClashes, "PSTRotator answers on port + 1")
        assertFalse(withSo2r.gearComplete)
        withSo2r.rotatorPort = 12000
        assertFalse(withSo2r.rotatorPortClashes)
        assertTrue(withSo2r.gearComplete)

        val withoutSo2r = WizardIntegrations(allOff())
        withoutSo2r.rotator = true
        withoutSo2r.rotatorPort = 12011
        assertFalse(withoutSo2r.rotatorPortClashes, "rotator 2 is not in use without SO2R")
        assertTrue(withoutSo2r.gearComplete)
    }

    @Test
    fun writingTheLogPageSwitchesExactlyTheChosenSourceOn() {
        val target = likeTheTemplate()
        val answers = WizardIntegrations(target)
        answers.loadFresh()
        answers.chooseLogProgram(WizardLogProgram.WINTEST)
        answers.wintestPort = 9871
        answers.wintestStationName = " KST "
        answers.qrgFollow = true

        answers.writeLogTo(target)

        assertTrue(target.isLogsynch_wintestNetworkListenerEnabled())
        assertFalse(target.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertFalse(target.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertEquals(9871, target.getLogsynch_wintestNetworkPort())
        assertEquals("KST", target.getLogsynch_wintestNetworkStationNameOfKST(), "trimmed, as the tab stores it")
        assertTrue(target.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(target.isTrxSynch_ucxLogUDPListenerEnabled(), "the other program's frequency source goes off")
        assertFalse(target.isNotify_dxClusterServerEnabled())
        assertTrue(target.isAirScout_asUDPListenerEnabled(), "the log page does not touch AirScout")
    }

    @Test
    fun writingNoLogProgramSwitchesEverySynchronisationOff() {
        val target = likeTheTemplate()
        val answers = WizardIntegrations(target)
        answers.loadFresh()

        answers.writeLogTo(target)

        assertFalse(target.isLogsynch_wintestNetworkListenerEnabled())
        assertFalse(target.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertFalse(target.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertFalse(target.isTrxSynch_ucxLogUDPListenerEnabled())
        assertFalse(target.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(target.isNotify_dxClusterServerEnabled())
        assertEquals(12060, target.getLogsynch_ucxUDPWkdCallListenerPort(), "an unused port is left alone")
    }

    @Test
    fun writingTheGearPageSetsRotatorAndAirScoutOnly() {
        val target = likeTheTemplate()
        val answers = WizardIntegrations(target)
        answers.loadFresh()
        answers.rotator = true
        answers.rotatorHost = " 192.168.1.20 "
        answers.rotatorPort = 12040

        answers.writeGearTo(target)

        assertTrue(target.isStn_pstRotatorEnabled())
        assertEquals("192.168.1.20", target.getStn_pstRotatorHost())
        assertEquals(12040, target.getStn_pstRotatorPort())
        assertFalse(target.isAirScout_asUDPListenerEnabled())
        assertEquals(9872, target.getAirScout_asCommunicationPort(), "an unused port is left alone")
        assertTrue(target.isLogsynch_ucxUDPWkdCallListenerEnabled(), "the gear page does not touch the log sources")
    }

    @Test
    fun theSwitchedOffListNamesWhatWasOnAndWillBeOff() {
        val answers = WizardIntegrations(likeTheTemplate())
        answers.loadFresh()

        assertEquals(
            listOf(WizardSwitch.LOG_UDP, WizardSwitch.LOG_FILE, WizardSwitch.QRG_FOLLOW, WizardSwitch.CLUSTER_SERVER),
            answers.switchedOffByLog(),
        )
        assertEquals(listOf(WizardSwitch.AIRSCOUT), answers.switchedOffByGear())

        answers.chooseLogProgram(WizardLogProgram.UDP)
        answers.qrgFollow = true
        answers.airScout = true
        assertEquals(listOf(WizardSwitch.LOG_FILE, WizardSwitch.CLUSTER_SERVER), answers.switchedOffByLog())
        assertEquals(emptyList<WizardSwitch>(), answers.switchedOffByGear())
    }

    @Test
    fun nothingIsSwitchedOffOnAStationThatHadNothingOn() {
        val answers = WizardIntegrations(allOff())
        answers.loadFresh()

        assertEquals(emptyList<WizardSwitch>(), answers.switchedOffByLog())
        assertEquals(emptyList<WizardSwitch>(), answers.switchedOffByGear())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.WizardIntegrationsTest'`
Expected: Kompilierfehler, `Unresolved reference: WizardIntegrations`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package kst4contest.view.compose.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.tabs.AirscoutTabState
import kst4contest.view.compose.tabs.StationTabState

/** Where the worked stations come from. The wizard knows one source at a time. */
enum class WizardLogProgram {
    NONE,

    /** The Win-Test network listener. */
    WINTEST,

    /** UCXLog, DXLog.net, N1MM+ and QARTEST: QSO messages on the shared UDP port. */
    UDP,

    /** A log file polled for callsigns. */
    FILE,
}

/** Something that was on in the profile and will be off after Save, for the summary. */
enum class WizardSwitch {
    SECOND_CHAT,
    LOG_WINTEST,
    LOG_UDP,
    LOG_FILE,
    QRG_FOLLOW,
    CLUSTER_SERVER,
    ROTATOR,
    AIRSCOUT,
}

/**
 * The setup wizard's answers about log program, rotator and AirScout.
 *
 * Two pages share this class because they share a rule: a station set up anew gets only what
 * the operator said yes to. [loadFresh] therefore switches everything off -- the bundled
 * template has several of these on, which a newcomer without a log program does not know --
 * while ports, names, host and identifiers stay as stored and are proposals.
 *
 * The wizard switches things on and off and takes numbers over. It invents none: no port,
 * no identifier and no default here differs from what the settings tabs accept.
 *
 * The settings window allows several log sources at once and the wizard's question does
 * not. [severalStoredLogSources] says when the stored profile is such a case, so the caller
 * can keep the log page shut rather than collapse the sources into one.
 *
 * @param prefs the profile's preferences, already read from its file
 */
class WizardIntegrations(prefs: ChatPreferences) {

    private val storedWintest = prefs.isLogsynch_wintestNetworkListenerEnabled()
    private val storedUdp = prefs.isLogsynch_ucxUDPWkdCallListenerEnabled()
    private val storedFile = prefs.isLogsynch_fileBasedWkdCallInterpreterEnabled()
    private val storedUdpQrg = prefs.isTrxSynch_ucxLogUDPListenerEnabled()
    private val storedWintestQrg = prefs.isLogsynch_wintestQrgSyncEnabled()
    private val storedCluster = prefs.isNotify_dxClusterServerEnabled()
    private val storedRotator = prefs.isStn_pstRotatorEnabled()
    private val storedAirScout = prefs.isAirScout_asUDPListenerEnabled()

    /** Rotator 2 only counts while SO2R uses it, as on the Station tab. */
    private val rotatorTwoInUse = prefs.isTrxSynch_so2rEnabled() && prefs.isStn_pstRotator2Enabled()
    private val rotatorTwoPort = prefs.getStn_pstRotator2Port()

    /** More than one log source is on in the profile; the log page cannot show that. */
    val severalStoredLogSources: Boolean = listOf(storedWintest, storedUdp, storedFile).count { it } > 1

    var logProgram: WizardLogProgram by mutableStateOf(WizardLogProgram.NONE)
        private set

    var udpPort: Int by mutableStateOf(prefs.getLogsynch_ucxUDPWkdCallListenerPort())
    var wintestPort: Int by mutableStateOf(prefs.getLogsynch_wintestNetworkPort())
    var wintestStationName: String by mutableStateOf(prefs.getLogsynch_wintestNetworkStationNameOfKST().orEmpty())
    var filePath: String by mutableStateOf(prefs.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly().orEmpty())

    /** Take the own frequency from the chosen network program. */
    var qrgFollow: Boolean by mutableStateOf(false)

    /** Pass spots to the log program through the local DX cluster server. */
    var clusterServer: Boolean by mutableStateOf(false)

    var rotator: Boolean by mutableStateOf(false)
    var rotatorHost: String by mutableStateOf(prefs.getStn_pstRotatorHost().orEmpty())
    var rotatorPort: Int by mutableStateOf(prefs.getStn_pstRotatorPort())

    var airScout: Boolean by mutableStateOf(false)
    var airScoutPort: Int by mutableStateOf(prefs.getAirScout_asCommunicationPort())
    var airScoutServerName: String by mutableStateOf(prefs.getAirScout_asServerNameString().orEmpty())
    var airScoutClientName: String by mutableStateOf(prefs.getAirScout_asClientNameString().orEmpty())

    init {
        loadStored()
    }

    /** The switches as the profile has them. Several sources show as one, the network ones first. */
    fun loadStored() {
        logProgram = when {
            storedWintest -> WizardLogProgram.WINTEST
            storedUdp -> WizardLogProgram.UDP
            storedFile -> WizardLogProgram.FILE
            else -> WizardLogProgram.NONE
        }
        qrgFollow = when (logProgram) {
            WizardLogProgram.WINTEST -> storedWintestQrg
            WizardLogProgram.UDP -> storedUdpQrg
            else -> false
        }
        clusterServer = networked && storedCluster
        rotator = storedRotator
        airScout = storedAirScout
    }

    /** A station set up anew: every switch off, every number left as a proposal. */
    fun loadFresh() {
        logProgram = WizardLogProgram.NONE
        qrgFollow = false
        clusterServer = false
        rotator = false
        airScout = false
    }

    /** Only a network program can deliver a frequency or take spots. */
    val networked: Boolean
        get() = logProgram == WizardLogProgram.WINTEST || logProgram == WizardLogProgram.UDP

    fun chooseLogProgram(program: WizardLogProgram) {
        logProgram = program
        if (!networked) {
            qrgFollow = false
            clusterServer = false
        }
    }

    /** Whether what the chosen program needs is there. A program not chosen needs nothing. */
    val logComplete: Boolean
        get() = when (logProgram) {
            WizardLogProgram.NONE -> true
            WizardLogProgram.WINTEST -> AirscoutTabState.isValidPort(wintestPort) && wintestStationName.isNotBlank()
            WizardLogProgram.UDP -> AirscoutTabState.isValidPort(udpPort)
            WizardLogProgram.FILE -> filePath.isNotBlank()
        }

    /** The same or the next port as rotator 2, which answers on its port + 1. */
    val rotatorPortClashes: Boolean
        get() = rotator && rotatorTwoInUse && StationTabState.portsClash(rotatorPort, rotatorTwoPort)

    /** Whether what the switched-on equipment needs is there. */
    val gearComplete: Boolean
        get() {
            val rotatorComplete = !rotator ||
                (rotatorHost.isNotBlank() && AirscoutTabState.isValidPort(rotatorPort) && !rotatorPortClashes)
            val airScoutComplete = !airScout ||
                (AirscoutTabState.isValidPort(airScoutPort) &&
                    AirscoutTabState.isValidIdentifier(airScoutServerName) &&
                    AirscoutTabState.isValidIdentifier(airScoutClientName))
            return rotatorComplete && airScoutComplete
        }

    /**
     * Puts the log page's answers into [prefs]: exactly the chosen source on, its details
     * stored, and the frequency source and the DX cluster server only with a network program.
     * Details of a program that is not chosen are left as they are. Call only while
     * [logComplete].
     */
    fun writeLogTo(prefs: ChatPreferences) {
        prefs.setLogsynch_wintestNetworkListenerEnabled(logProgram == WizardLogProgram.WINTEST)
        prefs.setLogsynch_ucxUDPWkdCallListenerEnabled(logProgram == WizardLogProgram.UDP)
        prefs.setLogsynch_fileBasedWkdCallInterpreterEnabled(logProgram == WizardLogProgram.FILE)

        when (logProgram) {
            WizardLogProgram.WINTEST -> {
                prefs.setLogsynch_wintestNetworkPort(wintestPort)
                prefs.setLogsynch_wintestNetworkStationNameOfKST(wintestStationName.trim())
            }
            WizardLogProgram.UDP -> prefs.setLogsynch_ucxUDPWkdCallListenerPort(udpPort)
            WizardLogProgram.FILE -> prefs.setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(filePath)
            WizardLogProgram.NONE -> Unit
        }

        prefs.setLogsynch_wintestQrgSyncEnabled(logProgram == WizardLogProgram.WINTEST && qrgFollow)
        prefs.setTrxSynch_ucxLogUDPListenerEnabled(logProgram == WizardLogProgram.UDP && qrgFollow)
        prefs.setNotify_dxClusterServerEnabled(networked && clusterServer)
    }

    /**
     * Puts the equipment page's answers into [prefs]. Host, port and identifiers are stored
     * only for what is switched on. Call only while [gearComplete].
     */
    fun writeGearTo(prefs: ChatPreferences) {
        prefs.setStn_pstRotatorEnabled(rotator)
        if (rotator) {
            prefs.setStn_pstRotatorHost(rotatorHost.trim())
            prefs.setStn_pstRotatorPort(rotatorPort)
        }

        prefs.setAirScout_asUDPListenerEnabled(airScout)
        if (airScout) {
            prefs.setAirScout_asCommunicationPort(airScoutPort)
            prefs.setAirScout_asServerNameString(airScoutServerName.trim())
            prefs.setAirScout_asClientNameString(airScoutClientName.trim())
        }
    }

    /** What the log page's answers switch off that the profile had on, in a fixed order. */
    fun switchedOffByLog(): List<WizardSwitch> = buildList {
        if (storedWintest && logProgram != WizardLogProgram.WINTEST) add(WizardSwitch.LOG_WINTEST)
        if (storedUdp && logProgram != WizardLogProgram.UDP) add(WizardSwitch.LOG_UDP)
        if (storedFile && logProgram != WizardLogProgram.FILE) add(WizardSwitch.LOG_FILE)
        if ((storedUdpQrg || storedWintestQrg) && !qrgFollow) add(WizardSwitch.QRG_FOLLOW)
        if (storedCluster && !(networked && clusterServer)) add(WizardSwitch.CLUSTER_SERVER)
    }

    /** What the equipment page's answers switch off that the profile had on. */
    fun switchedOffByGear(): List<WizardSwitch> = buildList {
        if (storedRotator && !rotator) add(WizardSwitch.ROTATOR)
        if (storedAirScout && !airScout) add(WizardSwitch.AIRSCOUT)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.WizardIntegrationsTest'`
Expected: 13 Tests, 0 Fehler in der XML.

Schlägt ein Test fehl, weil ein Setter von `ChatPreferences` den Wert verändert (die AirScout-Setter ersetzen ungültige Werte durch die Werksvorgabe): den Test **nicht** abschwächen, den Befund mit Setter und Wert melden.

- [ ] **Step 5: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/WizardIntegrations.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/WizardIntegrationsTest.kt
git commit -m "Add the setup wizard's log program, rotator and AirScout answers"
```

---

### Task 3: Die drei Seiten im Ablauf und auf dem Bildschirm

Zustand und Seiten gehören in eine Aufgabe: `WizardPage` bekommt drei Einträge, und `SetupWizardContent` verzweigt mit einem vollständigen `when` darüber. Getrennt ließe sich keins von beiden übersetzen.

**Files:**
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt`
- Modify: `app-desktop/src/main/i18n/strings_en.properties`, `strings_de.properties`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardStateTest.kt` (einen Test ändern, einen löschen, sieben ergänzen)
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardWriteTest.kt` (vier ergänzen)
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardContentScreenTest.kt` (zwei ergänzen)

**Interfaces:**
- Consumes: `WizardAntenna` (Task 1), `WizardIntegrations`, `WizardLogProgram`, `WizardSwitch` (Task 2); `FilePathRow` aus `kst4contest.view.compose.tabs` (dort `internal`, selbes Modul); vorhandene Texte `stationBeamWidth`, `stationOwnAntennaGain`, `stationOwnAntennaHeight`, `stationOwnTxPower`, `stationOwnHeightAsl`, `stationInvalidOwnHeightAsl(…)`, `stationRotatorHost`, `stationRotatorPort`, `stationRotator2PortClash(…)`, `logSynchWinTestPort`, `logSynchWinTestStationName`, `logSynchSharedPort`, `logSynchFileLabel`, `logSynchFileChooserTitle`, `airscoutUdpPort`, `airscoutServerIdentifier`, `airscoutClientIdentifier`, `trxSynchHint`.
- Produces, in `SetupWizardState`:
  - `enum class WizardPage { ENTRY, PERSONAL, LOGIN, SITE, ANTENNA, LOG, GEAR, SUMMARY }`
  - `val antenna: WizardAntenna`, `val integrations: WizardIntegrations`
  - `val logPageEditable: Boolean`
  - `val switchedOff: List<WizardSwitch>`
  - entfallen: `turnsOffSecondChat`, `leavesIntegrationsUntouched`
- Texte: neue Schlüssel `wizard.antenna.*`, `wizard.log.*`, `wizard.gear.*`, `wizard.switch.*`, `wizard.summary.*`; entfernt `wizard.summary.turnedOff`, `wizard.summary.integrationsUnchanged`.

- [ ] **Step 1: Texte anlegen**

In **beiden** Dateien die Zeilen mit den Schlüsseln `wizard.summary.turnedOff` und `wizard.summary.integrationsUnchanged` löschen.

An das Ende von `strings_en.properties` anfügen:

```properties
wizard.antenna.title=Antenna and power
wizard.antenna.lead=Optional. These values feed the path analysis and the antenna sector on the map; the chat does not need them.
wizard.antenna.aslHint=Leave the site elevation empty to let the elevation model decide.
wizard.log.title=Log program
wizard.log.lead=Where do your worked stations come from?
wizard.log.none=None, no synchronisation
wizard.log.wintest=Win-Test
wizard.log.udp=UCXLog, DXLog.net, N1MM+ or QARTEST
wizard.log.file=A log file
wizard.log.qrgFollow=Take my own frequency from the log program
wizard.log.clusterServer=Pass spots to the log program through the local DX cluster server
wizard.log.incomplete=The entries for this log program are incomplete or out of range.
wizard.gear.title=Rotator and AirScout
wizard.gear.lead=Only what is really there.
wizard.gear.rotator=PSTRotator is in use
wizard.gear.airScout=AirScout is in use
wizard.gear.incomplete=The entries on this page are incomplete or out of range.
wizard.switch.secondChat=second chat
wizard.switch.logWintest=Win-Test synchronisation
wizard.switch.logUdp=network log synchronisation
wizard.switch.logFile=log file polling
wizard.switch.qrgFollow=frequency from the log program
wizard.switch.clusterServer=DX cluster server
wizard.switch.rotator=PSTRotator
wizard.switch.airScout=AirScout
wizard.summary.antenna=Antenna and power
wizard.summary.log=Log program
wizard.summary.gear=Rotator and AirScout
wizard.summary.qrgFollow=Own frequency from the log program
wizard.summary.clusterServer=DX cluster server
wizard.summary.yes=yes
wizard.summary.no=no
wizard.summary.switchedOff=Will be switched off: {0}.
wizard.summary.logSeveral=Several log sources are enabled in this profile. Change them in the settings window.
```

An das Ende von `strings_de.properties` anfügen:

```properties
wizard.antenna.title=Antenne und Leistung
wizard.antenna.lead=Freiwillig. Die Werte speisen die Pfadanalyse und den Antennensektor auf der Karte; für den Chat braucht sie niemand.
wizard.antenna.aslHint=Standorthöhe leer lassen, dann entscheidet das Höhenmodell.
wizard.log.title=Logprogramm
wizard.log.lead=Woher kommen deine gearbeiteten Stationen?
wizard.log.none=Keines, kein Abgleich
wizard.log.wintest=Win-Test
wizard.log.udp=UCXLog, DXLog.net, N1MM+ oder QARTEST
wizard.log.file=Eine Logdatei
wizard.log.qrgFollow=Eigene Frequenz aus dem Logprogramm übernehmen
wizard.log.clusterServer=Spots über den lokalen DX-Cluster-Server an das Logprogramm geben
wizard.log.incomplete=Die Angaben zu diesem Logprogramm sind unvollständig oder außerhalb des Bereichs.
wizard.gear.title=Rotor und AirScout
wizard.gear.lead=Nur was wirklich vorhanden ist.
wizard.gear.rotator=PSTRotator wird benutzt
wizard.gear.airScout=AirScout wird benutzt
wizard.gear.incomplete=Die Angaben auf dieser Seite sind unvollständig oder außerhalb des Bereichs.
wizard.switch.secondChat=Zweitchat
wizard.switch.logWintest=Win-Test-Abgleich
wizard.switch.logUdp=Netzwerk-Logabgleich
wizard.switch.logFile=Logdatei-Abgleich
wizard.switch.qrgFollow=Frequenz aus dem Logprogramm
wizard.switch.clusterServer=DX-Cluster-Server
wizard.switch.rotator=PSTRotator
wizard.switch.airScout=AirScout
wizard.summary.antenna=Antenne und Leistung
wizard.summary.log=Logprogramm
wizard.summary.gear=Rotor und AirScout
wizard.summary.qrgFollow=Eigene Frequenz aus dem Logprogramm
wizard.summary.clusterServer=DX-Cluster-Server
wizard.summary.yes=ja
wizard.summary.no=nein
wizard.summary.switchedOff=Wird abgeschaltet: {0}.
wizard.summary.logSeveral=In diesem Profil sind mehrere Logquellen eingeschaltet. Im Einstellungsfenster ändern.
```

Noch nicht bauen: `SetupWizardContent.kt` benutzt die beiden gelöschten Schlüssel bis Step 5.

- [ ] **Step 2: Write the failing tests (Zustand)**

In `SetupWizardStateTest.kt`:

(a) Im Test `aFreshInstallationHasNoEntryPageAndStartsEmpty` die erwartete Seitenliste ersetzen. Alt:

```kotlin
            listOf(WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SITE, WizardPage.SUMMARY),
```

Neu:

```kotlin
            listOf(
                WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SITE,
                WizardPage.ANTENNA, WizardPage.LOG, WizardPage.GEAR, WizardPage.SUMMARY,
            ),
```

(b) Den Test `theSummarySaysWhatIsSwitchedOffAndWhatIsLeftAlone` ganz löschen.

(c) Diese Hilfen und Tests an das Ende der Klasse anfügen:

```kotlin
    /** A first installation whose stored integrations are those of the bundled template. */
    private fun freshInstallationLikeTheTemplate() = freshInstallation().apply {
        setLogsynch_wintestNetworkListenerEnabled(false)
        setLogsynch_ucxUDPWkdCallListenerEnabled(true)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        setTrxSynch_ucxLogUDPListenerEnabled(true)
        setLogsynch_wintestQrgSyncEnabled(false)
        setNotify_dxClusterServerEnabled(true)
        setStn_pstRotatorEnabled(false)
        setAirScout_asUDPListenerEnabled(true)
        setLogsynch_ucxUDPWkdCallListenerPort(12060)
    }

    /** A duplicated profile at a Win-Test station with a rotator. */
    private fun duplicatedWinTestStation() = duplicatedProfile().apply {
        setLogsynch_wintestNetworkListenerEnabled(true)
        setLogsynch_wintestNetworkPort(9871)
        setLogsynch_wintestNetworkStationNameOfKST("KST")
        setLogsynch_ucxUDPWkdCallListenerEnabled(false)
        setLogsynch_fileBasedWkdCallInterpreterEnabled(false)
        setLogsynch_wintestQrgSyncEnabled(true)
        setTrxSynch_ucxLogUDPListenerEnabled(false)
        setNotify_dxClusterServerEnabled(false)
        setStn_pstRotatorEnabled(true)
        setStn_pstRotatorHost("127.0.0.1")
        setStn_pstRotatorPort(12000)
        setAirScout_asUDPListenerEnabled(false)
        setTrxSynch_so2rEnabled(false)
    }

    @Test
    fun aNewStationStartsWithEveryIntegrationOff() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())

        assertEquals(WizardLogProgram.NONE, state.integrations.logProgram)
        assertFalse(state.integrations.qrgFollow)
        assertFalse(state.integrations.clusterServer)
        assertFalse(state.integrations.rotator)
        assertFalse(state.integrations.airScout)
        assertEquals(
            listOf(
                WizardSwitch.SECOND_CHAT, WizardSwitch.LOG_UDP, WizardSwitch.LOG_FILE,
                WizardSwitch.QRG_FOLLOW, WizardSwitch.CLUSTER_SERVER, WizardSwitch.AIRSCOUT,
            ),
            state.switchedOff,
            "the summary names everything the template had on",
        )
    }

    @Test
    fun theNewPagesComeAfterTheLocationAndCanBeWalkedThrough() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())
        state.fillFirstInstallation()

        state.next()
        assertEquals(WizardPage.ANTENNA, state.page)
        assertTrue(state.canGoNext, "the antenna values are proposals and valid as they stand")
        state.next()
        assertEquals(WizardPage.LOG, state.page)
        state.next()
        assertEquals(WizardPage.GEAR, state.page)
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        state.back()
        assertEquals(WizardPage.GEAR, state.page)
    }

    @Test
    fun finishingEarlyStillSwitchesANewStationsIntegrationsOff() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())
        state.fillFirstInstallation()
        assertTrue(state.canFinish)

        state.finish()

        assertEquals(WizardPage.SUMMARY, state.page)
        assertTrue(state.switchedOff.contains(WizardSwitch.LOG_UDP), "a skipped page of a new station still applies")
        assertTrue(state.switchedOff.contains(WizardSwitch.AIRSCOUT))
    }

    @Test
    fun aKeptStationSkipsTheNewPagesAndSwitchesNothingOff() {
        val state = SetupWizardState(duplicatedWinTestStation())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()

        assertEquals(
            listOf(WizardPage.ENTRY, WizardPage.PERSONAL, WizardPage.LOGIN, WizardPage.SUMMARY),
            state.pages,
        )
        assertEquals(WizardLogProgram.WINTEST, state.integrations.logProgram, "the summary shows what is stored")
        assertTrue(state.integrations.rotator)
        assertEquals(emptyList<WizardSwitch>(), state.switchedOff)
        assertTrue(state.logPageEditable)
    }

    @Test
    fun changeFromTheSummaryOpensANewPageOfAKeptStation() {
        val state = SetupWizardState(duplicatedWinTestStation())
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()
        assertEquals(WizardPage.SUMMARY, state.page)

        state.edit(WizardPage.GEAR)
        assertEquals(WizardPage.GEAR, state.page)
        state.integrations.rotator = false
        state.next()

        assertEquals(WizardPage.SUMMARY, state.page)
        assertEquals(listOf(WizardSwitch.ROTATOR), state.switchedOff)
    }

    @Test
    fun aKeptStationWithSeveralLogSourcesCannotOpenTheLogPage() {
        val state = SetupWizardState(duplicatedProfile().apply {
            setLogsynch_wintestNetworkListenerEnabled(false)
            setLogsynch_ucxUDPWkdCallListenerEnabled(true)
            setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        })
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()

        assertFalse(state.logPageEditable, "the page knows one source and would switch the other off")
        state.edit(WizardPage.LOG)
        assertEquals(WizardPage.SUMMARY, state.page, "the log page must stay shut")

        state.edit(WizardPage.GEAR)
        assertEquals(WizardPage.GEAR, state.page, "the other pages are not affected")
    }

    @Test
    fun anInvalidOptionalPageBlocksSaving() {
        val state = SetupWizardState(freshInstallationLikeTheTemplate())
        state.fillFirstInstallation()
        state.next()
        state.next()
        assertEquals(WizardPage.LOG, state.page)

        state.integrations.chooseLogProgram(WizardLogProgram.UDP)
        state.integrations.udpPort = 0

        assertFalse(state.canGoNext)
        assertFalse(state.canFinish, "a summary that cannot be saved helps nobody")
        assertFalse(state.mandatoryComplete)

        state.integrations.udpPort = 12060
        assertTrue(state.canGoNext)
        assertTrue(state.mandatoryComplete)
    }
```

- [ ] **Step 3: Write the failing tests (Schreiben)**

In `SetupWizardWriteTest.kt` an das Ende der Klasse anfügen:

```kotlin
    @Test
    fun aNewStationWithoutALogProgramHasEverySynchronisationOff() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.answerFirstInstallation()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertFalse(restored.isLogsynch_wintestNetworkListenerEnabled())
        assertFalse(restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertFalse(restored.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertFalse(restored.isTrxSynch_ucxLogUDPListenerEnabled())
        assertFalse(restored.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(restored.isNotify_dxClusterServerEnabled())
        assertFalse(restored.isStn_pstRotatorEnabled())
        assertFalse(restored.isAirScout_asUDPListenerEnabled())
    }

    @Test
    fun theAnswersOfTheNewPagesAreWritten() {
        val file = templateFile()
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.name = "Philipp"
        state.next()
        state.callsign = "DL0TEST"
        state.password = "secret"
        state.next()
        state.locator = "JO50AA"
        state.setBand(WizardBand.B144, true)
        state.next()
        state.antenna.beamWidthDeg = 28.0
        state.antenna.heightAslText = "612"
        state.next()
        state.integrations.chooseLogProgram(WizardLogProgram.WINTEST)
        state.integrations.wintestPort = 9871
        // Stated, not taken from the template: without a name the page could not be left.
        state.integrations.wintestStationName = "KST"
        state.integrations.qrgFollow = true
        state.next()
        assertEquals(WizardPage.GEAR, state.page)
        state.integrations.rotator = true
        state.integrations.rotatorHost = "127.0.0.1"
        state.integrations.rotatorPort = 12000

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(28.0, restored.getStn_antennaBeamWidthDeg())
        assertEquals(612.0, restored.getStn_pathAnalysisOwnHeightAslMeters())
        assertTrue(restored.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals(9871, restored.getLogsynch_wintestNetworkPort())
        assertTrue(restored.isLogsynch_wintestQrgSyncEnabled())
        assertFalse(restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertTrue(restored.isStn_pstRotatorEnabled())
        assertFalse(restored.isAirScout_asUDPListenerEnabled())
    }

    @Test
    fun aKeptStationKeepsEveryIntegrationAsItWas() {
        // The template's integrations, several log sources among them, under a real login.
        val file = configuredFile()
        val before = read(file)
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertEquals(before.isLogsynch_ucxUDPWkdCallListenerEnabled(), restored.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertEquals(
            before.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            restored.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
        )
        assertEquals(before.isTrxSynch_ucxLogUDPListenerEnabled(), restored.isTrxSynch_ucxLogUDPListenerEnabled())
        assertEquals(before.isNotify_dxClusterServerEnabled(), restored.isNotify_dxClusterServerEnabled())
        assertEquals(before.isAirScout_asUDPListenerEnabled(), restored.isAirScout_asUDPListenerEnabled())
        assertEquals(before.getStn_antennaBeamWidthDeg(), restored.getStn_antennaBeamWidthDeg())
        assertTrue(
            restored.isLogsynch_ucxUDPWkdCallListenerEnabled() && restored.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            "the template has two log sources on; if this fails the template changed and the test needs another example",
        )
    }

    @Test
    fun openingOneNewPageOfAKeptStationWritesOnlyThatPage() {
        val file = configuredFile()
        val before = read(file)
        val prefs = read(file)
        val state = SetupWizardState(prefs)
        state.chooseLoginSource(WizardSource.KEEP)
        state.chooseStationSource(WizardSource.KEEP)
        state.next()
        state.next()
        state.edit(WizardPage.GEAR)
        state.integrations.rotator = true
        state.integrations.rotatorHost = "127.0.0.1"
        state.integrations.rotatorPort = 12000
        state.next()

        assertTrue(state.writeTo(prefs))

        val restored = read(file)
        assertTrue(restored.isStn_pstRotatorEnabled())
        assertEquals(
            before.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            restored.isLogsynch_fileBasedWkdCallInterpreterEnabled(),
            "the log page was not opened, so the log sources stay",
        )
        assertEquals(before.isNotify_dxClusterServerEnabled(), restored.isNotify_dxClusterServerEnabled())
    }
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `./gradlew :app-desktop:compileTestKotlin`
Expected: Kompilierfehler, unter anderem `Unresolved reference: integrations` und `Unresolved reference: ANTENNA`.

- [ ] **Step 5: `SetupWizardState.kt` ändern**

(a) Die Aufzählung der Seiten ersetzen:

```kotlin
/** The pages, in the order they can appear. */
enum class WizardPage { ENTRY, PERSONAL, LOGIN, SITE, ANTENNA, LOG, GEAR, SUMMARY }
```

(b) Nach der Zeile `var secondCategory: Int? by mutableStateOf(null)` einfügen:

```kotlin
    /** Antenna and power; always proposals from what is stored. */
    val antenna = WizardAntenna(prefs)

    /** Log program, rotator and AirScout. */
    val integrations = WizardIntegrations(prefs)
```

(c) Die Zeile `private var stationShown by mutableStateOf(false)` ersetzen durch:

```kotlin
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
```

(d) In `applySources()` den Block der Station ersetzen. Alt:

```kotlin
            // A kept category is the operator's; the suggestion must not replace it.
            categoryChosenByOperator = keep
            stationShown = !keep || !stationComplete
            appliedStationSource = stationSource
```

Neu:

```kotlin
            // A kept category is the operator's; the suggestion must not replace it.
            categoryChosenByOperator = keep
            siteShown = !keep || !stationComplete
            antennaShown = !keep
            logShown = !keep
            gearShown = !keep
            // A new station gets only what the operator says yes to.
            if (keep) integrations.loadStored() else integrations.loadFresh()
            appliedStationSource = stationSource
```

(e) `pages` ersetzen:

```kotlin
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
```

(f) `mandatoryComplete` ersetzen:

```kotlin
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
```

(g) In `canGoNext` vor `WizardPage.SUMMARY -> false` einfügen:

```kotlin
            WizardPage.ANTENNA -> antenna.complete
            WizardPage.LOG -> integrations.logComplete
            WizardPage.GEAR -> integrations.gearComplete
```

(h) `edit` ersetzen:

```kotlin
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
```

(i) Die beiden Eigenschaften `turnsOffSecondChat` und `leavesIntegrationsUntouched` samt ihren Kommentaren löschen und an ihrer Stelle einfügen:

```kotlin
    /**
     * What was on in the profile and will be off after Save, for the summary. Pages that are
     * not part of this run change nothing and contribute nothing.
     */
    val switchedOff: List<WizardSwitch>
        get() = buildList {
            if (storedSecondChat && !secondChat) add(WizardSwitch.SECOND_CHAT)
            if (logShown) addAll(integrations.switchedOffByLog())
            if (gearShown) addAll(integrations.switchedOffByGear())
        }
```

(j) In `writeTo` unmittelbar vor dem Kommentar `// A write that throws is a write that failed` einfügen:

```kotlin
        if (antennaShown) antenna.writeTo(prefs)
        if (logShown) integrations.writeLogTo(prefs)
        if (gearShown) integrations.writeGearTo(prefs)

```

und im KDoc von `writeTo` nach dem Absatz über den Namen des Zweitchats ergänzen:

```kotlin
     *
     * Antenna, log program and equipment are written only for the pages that were part of
     * this run.
```

- [ ] **Step 6: `SetupWizardContent.kt` ändern**

(a) Importe ergänzen:

```kotlin
import kst4contest.view.compose.tabs.FilePathRow
```

(b) Im `when (state.page)` vor `WizardPage.SUMMARY ->` einfügen:

```kotlin
                        WizardPage.ANTENNA -> AntennaPage(state.antenna, strings)
                        WizardPage.LOG -> LogPage(state.integrations, strings)
                        WizardPage.GEAR -> GearPage(state.integrations, strings)
```

(c) Die Funktion `SourceOption` umbenennen in `RadioOption` (Deklaration und alle Aufrufe in `EntryPage`). Sie wird jetzt auch für das Logprogramm benutzt.

(d) Vor `private fun SummaryRow` einfügen:

```kotlin
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
    Form.decimal(strings.stationOwnAntennaHeight, antenna.heightMeters, isValid = { it.isFinite() }) {
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
```

(e) In `SummaryPage` die beiden Zeilen

```kotlin
    if (state.turnsOffSecondChat) Text(strings.wizardSummaryTurnedOff)
    if (state.leavesIntegrationsUntouched) Text(strings.wizardSummaryIntegrationsUnchanged)
```

ersetzen durch:

```kotlin
    if (state.switchedOff.isNotEmpty()) {
        Text(strings.wizardSummarySwitchedOff(state.switchedOff.joinToString(", ") { switchName(it, strings) }))
    }
```

(f) In `SummaryPage` nach dem Abschnitt `Form.section(strings.wizardSummaryStation) { … }`, also als letzte Anweisungen der Funktion, einfügen:

```kotlin

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
            SummaryRow(strings.wizardSummaryQrgFollow, yesNo(integrations.qrgFollow))
            SummaryRow(strings.wizardSummaryClusterServer, yesNo(integrations.networked && integrations.clusterServer))
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
```

- [ ] **Step 7: Write the failing tests (Bildschirm)**

In `SetupWizardContentScreenTest.kt` an das Ende der Klasse anfügen:

```kotlin
    /** A first installation: the template's credentials, so the wizard starts from scratch. */
    private fun firstInstallation() =
        ChatPreferences(Path.of("setup-wizard-screen-test-unused.xml")).apply {
            setStn_loginCallSign("DO5AMF")
            setStn_loginPassword("changeMe")
            setLogsynch_wintestNetworkListenerEnabled(false)
            setLogsynch_ucxUDPWkdCallListenerEnabled(true)
            setLogsynch_fileBasedWkdCallInterpreterEnabled(false)
            // Every other switch stated too, so the summary's wording does not hang on a
            // compiled-in default.
            setTrxSynch_ucxLogUDPListenerEnabled(false)
            setLogsynch_wintestQrgSyncEnabled(false)
            setNotify_dxClusterServerEnabled(false)
            setStn_pstRotatorEnabled(false)
            setAirScout_asUDPListenerEnabled(true)
            setLoginToSecondChatEnabled(false)
        }

    private fun SetupWizardState.walkToTheLogPage() {
        name = "Philipp"
        next()
        callsign = "DL0TEST"
        password = "secret"
        next()
        locator = "JO50AA"
        setBand(WizardBand.B144, true)
        next()
        next()
    }

    @Test
    fun choosingALogProgramOnTheDrawnPageReachesTheState() = runComposeUiTest {
        val state = SetupWizardState(firstInstallation())
        state.walkToTheLogPage()
        assertEquals(WizardPage.LOG, state.page)
        show(state)

        onNodeWithText("Win-Test").performClick()

        assertEquals(WizardLogProgram.WINTEST, state.integrations.logProgram)
        onNodeWithText("Take my own frequency from the log program").assertExists()
    }

    @Test
    fun theSummaryNamesWhatANewStationSwitchesOff() = runComposeUiTest {
        val state = SetupWizardState(firstInstallation())
        state.walkToTheLogPage()
        state.finish()
        show(state)

        onNodeWithText("Will be switched off: network log synchronisation, AirScout.").assertExists()
    }
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.wizard.*' --tests 'kst4contest.view.i18n.*'`
Expected: alle grün, 0 Fehler, 0 übersprungen. In der XML: `WizardAntennaTest` 7 und `WizardIntegrationsTest` 13 Tests. Für `SetupWizardStateTest` (einer gelöscht, sieben neu), `SetupWizardWriteTest` (vier neu) und `SetupWizardContentScreenTest` (zwei neu) die Zahl vor und nach der Änderung nennen; die Differenz muss +6, +4 und +2 sein.

Schlägt `aKeptStationKeepsEveryIntegrationAsItWas` an der letzten Zusicherung fehl, hat die Vorlage nicht mehr zwei Logquellen an. Dann stimmt die Annahme des Tests nicht mehr: melden, nicht anpassen.

- [ ] **Step 9: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardState.kt app-desktop/src/main/kotlin/kst4contest/view/compose/wizard/SetupWizardContent.kt app-desktop/src/main/i18n/strings_en.properties app-desktop/src/main/i18n/strings_de.properties app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardStateTest.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardWriteTest.kt app-desktop/src/test/kotlin/kst4contest/view/compose/wizard/SetupWizardContentScreenTest.kt
git commit -m "Ask about antenna, log program, rotator and AirScout in the setup wizard"
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

- [ ] **Step 1: Spec nachziehen**

Die Spec ist die bindende Quelle; zwei Dinge, die Stufe 2 festgelegt hat, stehen dort noch nicht.

In `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md` im Abschnitt „Die Einstiegsseite" nach dem Absatz, der mit „In allen drei Fällen zeigt die Zusammenfassung die Werte" beginnt, einfügen:

```markdown
Eine Ausnahme hat das Logprogramm. Das Einstellungsfenster erlaubt mehrere Logquellen
zugleich, die Frage des Assistenten nur eine. Hat ein unverändert gelassenes Profil mehr als
eine Quelle eingeschaltet, zeigt die Zusammenfassung das an und lässt die Seite zu: sie
würde beim Öffnen alle Quellen bis auf eine abschalten.

**Geschrieben wird nur, was gezeigt wurde.** Bleibt die Station unverändert und öffnet der
Nutzer weder Antenne noch Logprogramm noch Rotor und AirScout, fasst der Assistent diese
Einstellungen nicht an.
```

Im Abschnitt „Ausbaustufen" den Satz „Stufe 1 ist für sich benutzbar; bis Stufe 2 lässt „neu einrichten" die Integrationen auf ihren gespeicherten Werten und sagt das in der Zusammenfassung." ersetzen durch:

```markdown
Stufe 1 und 2 sind umgesetzt. Offen ist Stufe 3.
```

- [ ] **Step 2: `docs/PROJECT_CONTEXT.md`**

Im Punkt „The setup wizard writes `preferences.xml` …" unter „Important Decisions and Workarounds" den Satz ersetzen. Alt:

```markdown
Stage 1
  asks for name, login, locator, bands and chat category; the reasons and the later stages
  are in `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md`.
```

Neu:

```markdown
It
  asks for name, login, locator, bands and chat category, and optionally for antenna and
  power, log program, rotator and AirScout. A station set up anew gets only what the
  operator said yes to: the bundled template has several integrations on, and the wizard
  switches them off and says so in its summary. A page that was not shown in a run is not
  written, which is what lets "leave the station unchanged" keep a profile with several log
  sources as it is. The reasons and the open stage 3 are in
  `docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md`.
```

Der Zeilenumbruch im vorhandenen Text kann anders liegen; den Satz, der mit „Stage 1" beginnt und mit dem Pfad der Spec endet, als Ganzes ersetzen.

- [ ] **Step 3: Handbuch, deutsch**

In `github_docs/de-Konfiguration.md` im Abschnitt „Einrichtungsassistent" die nummerierte Liste ersetzen. Alt:

```markdown
1. **Persönliches** – Sprache und Anzeigename im Chat.
2. **Anmeldung** – Rufzeichen und Passwort des ON4KST-Kontos.
3. **Standort und Bänder** – Locator, aktive Bänder und Chatkategorie. Die Kategorie wird aus den Bändern vorgeschlagen und lässt sich ändern. Ein zweiter Chat ist freiwillig.
4. **Zusammenfassung** – erst **Speichern** schreibt die Einstellungen. **Speichern und verbinden** meldet danach gleich am Chat an.
```

Neu:

```markdown
1. **Persönliches** – Sprache und Anzeigename im Chat.
2. **Anmeldung** – Rufzeichen und Passwort des ON4KST-Kontos.
3. **Standort und Bänder** – Locator, aktive Bänder und Chatkategorie. Die Kategorie wird aus den Bändern vorgeschlagen und lässt sich ändern. Ein zweiter Chat ist freiwillig.
4. **Antenne und Leistung** – freiwillig: Öffnungswinkel, Antennengewinn, Antennenhöhe, Sendeleistung und Standorthöhe. Die Werte speisen die Pfadanalyse; für den Chat braucht sie niemand.
5. **Logprogramm** – keines, Win-Test, UCXLog/DXLog.net/N1MM+/QARTEST oder eine Logdatei. Port, Stationsname oder Datei erscheinen erst nach der Wahl. Mit einem Netzwerk-Logprogramm lässt sich außerdem die eigene Frequenz übernehmen und der lokale DX-Cluster-Server einschalten.
6. **Rotor und AirScout** – je ein Schalter; Host, Port und Kennungen erscheinen erst, wenn er gesetzt ist.
7. **Zusammenfassung** – erst **Speichern** schreibt die Einstellungen. **Speichern und verbinden** meldet danach gleich am Chat an.

Sobald Anmeldung, Locator, mindestens ein Band und die Chatkategorie stehen, führt **Fertig** direkt zur Zusammenfassung. Die Schritte 4 bis 6 lassen sich also überspringen.

Eine **neu eingerichtete** Station bekommt nur eingeschaltet, was im Assistenten bejaht wurde. Die mitgelieferte Vorlage hat Logabgleich, Frequenzübernahme, DX-Cluster-Server und AirScout an; der Assistent schaltet ab, was nicht gewählt wurde, und nennt es in der Zusammenfassung unter „Wird abgeschaltet".
```

Den Absatz „Logprogramm, Rotor, AirScout und DX-Cluster-Server fragt der Assistent noch nicht ab; …" ersetzen durch:

```markdown
Bleibt die Station **unverändert**, fasst der Assistent Antenne, Logprogramm, Rotor und AirScout nicht an, solange die zugehörige Seite nicht über **Ändern** geöffnet wird. Sind im Profil mehrere Logquellen zugleich eingeschaltet, lässt sich die Seite „Logprogramm" nicht öffnen: sie kennt nur eine Quelle. Solche Stationen stellt man weiter im Einstellungsfenster ein.
```

- [ ] **Step 4: Handbuch, englisch**

In `github_docs/en-Configuration.md` im Abschnitt „Setup Assistant" die nummerierte Liste ersetzen. Alt:

```markdown
1. **Personal** – language and the name shown in the chat.
2. **Login** – callsign and password of the ON4KST account.
3. **Location and bands** – locator, active bands and chat category. The category is suggested from the bands and can be changed. A second chat is optional.
4. **Summary** – nothing is written until you click **Save**. **Save and connect** logs into the chat right afterwards.
```

Neu:

```markdown
1. **Personal** – language and the name shown in the chat.
2. **Login** – callsign and password of the ON4KST account.
3. **Location and bands** – locator, active bands and chat category. The category is suggested from the bands and can be changed. A second chat is optional.
4. **Antenna and power** – optional: beam width, antenna gain, antenna height, transmit power and site elevation. These values feed the path analysis; the chat does not need them.
5. **Log program** – none, Win-Test, UCXLog/DXLog.net/N1MM+/QARTEST, or a log file. Port, station name or file appear only after the choice. With a network log program you can also take your own frequency from it and enable the local DX cluster server.
6. **Rotator and AirScout** – one switch each; host, port and identifiers appear only once it is set.
7. **Summary** – nothing is written until you click **Save**. **Save and connect** logs into the chat right afterwards.

As soon as login, locator, at least one band and the chat category are there, **Finish** leads straight to the summary. Steps 4 to 6 can be skipped.

A station that is **set up anew** gets only what was confirmed in the assistant. The bundled template has log synchronisation, frequency takeover, DX cluster server and AirScout enabled; the assistant switches off what was not chosen and names it in the summary under "Will be switched off".
```

Den Absatz „The assistant does not ask about log program, rotator, AirScout and DX cluster server yet; …" ersetzen durch:

```markdown
If the station is left **unchanged**, the assistant does not touch antenna, log program, rotator and AirScout unless the corresponding page is opened through **Change**. If several log sources are enabled in the profile at once, the "Log program" page cannot be opened: it knows one source only. Such stations are still configured in the settings window.
```

- [ ] **Step 5: Der ganze Bau**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL.

Danach:

```bash
grep -l '<failure\|<error' core/build/test-results/test/*.xml app-desktop/build/test-results/test/*.xml i18n-generator/build/test-results/test/*.xml
```

Expected: keine Ausgabe. Die Summen `tests`, `skipped`, `failures`, `errors` je Modul aus der XML nennen.

```bash
grep -c '<violation' app-desktop/build/reports/pmd/main.xml
grep -c '<BugInstance' app-desktop/build/reports/spotbugs/main.xml
```

Beide Zahlen nennen und die Funde aufführen, die in `WizardAntenna.kt`, `WizardIntegrations.kt` oder den geänderten Teilen von `SetupWizardState.kt` und `SetupWizardContent.kt` liegen. Kein Vergleich mit `main`, kein zweiter Checkout.

- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/specs/2026-11-18-einrichtungsassistent-design.md docs/PROJECT_CONTEXT.md github_docs/de-Konfiguration.md github_docs/en-Configuration.md
git commit -m "Document stage 2 of the setup wizard"
```

- [ ] **Step 7: Von Hand prüfen (Nutzer, kein Agent)**

Start mit `./gradlew :app-desktop:run`; vorher `~/.praktiKST/preferences.xml` sichern.

1. Neues Profil anlegen und dorthin wechseln, auf der Einstiegsseite „Station neu einrichten". Durch alle Seiten gehen, kein Logprogramm, kein Rotor, kein AirScout. Erwartet: die Zusammenfassung nennt unter „Wird abgeschaltet" Netzwerk-Logabgleich, Logdatei-Abgleich, Frequenz aus dem Logprogramm, DX-Cluster-Server und AirScout. Nach „Speichern" zeigen die Reiter Log-Synch, TRX-Synch, Notification und AirScout die Schalter aus.
2. Dasselbe Profil, Assistent aus dem Menü, Station „neu einrichten", Win-Test wählen. Erwartet: Port und Stationsname erscheinen erst nach der Wahl; nach dem Speichern ist im Reiter Log-Synch nur Win-Test an.
3. Auf der Seite „Logprogramm" eine Logdatei wählen. Erwartet: der Dateidialog öffnet sich über dem Assistenten und lässt sich bedienen. Bleibt er hinter dem Fenster oder reagiert nicht, ist das ein Befund: der Assistent ist ein modaler Dialog, der Dateidialog des Einstellungsfensters wurde dafür nicht gebaut.
4. Eigenes Profil, Assistent aus dem Menü, beides „unverändert", in der Zusammenfassung nichts ändern, „Speichern". Erwartet: `diff` der gesicherten `preferences.xml` gegen die neue zeigt keinen fachlichen Unterschied.
5. Testprofil löschen.

Punkt 3 kann kein Test dieses Plans abdecken.

- [ ] **Step 8: Den Plan entfernen**

Nach der Handprüfung:

```bash
git rm docs/superpowers/plans/2026-11-18-einrichtungsassistent-stufe-2.md
git commit -m "Remove the executed setup wizard stage 2 plan"
```

Danach den Text von PR #174 nachziehen: Stufe 2 ist enthalten, die Liste „not asked about yet" entfällt bis auf Stufe 3.
