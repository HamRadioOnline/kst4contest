# Entwurf — Einrichtungsassistent für unkonfigurierte Profile

Stand 2026-11-18. Konzept mit Philipp abgestimmt, **noch nicht vom Maintainer freigegeben**.
Berührt den Entwurfsmechanismus aus `2026-10-16-settings-apply-drafts-design.md` bewusst nicht.

## Warum

Das Einstellungsfenster hat zwölf Reiter und 74 Felder (`settings-fields.txt`). Wer das
Programm zum ersten Mal startet, landet genau dort (`Kst4ContestApplication.startRuntime`
ruft am Ende `openSettingsWindow()`), und nichts sagt ihm, welche fünf Angaben er braucht
und welche siebzig er lassen kann.

Dazu kommt, dass die mitgelieferte Vorlage `praktiKSTpreferences.xml` keine neutrale
Ausgangslage ist. Sie trägt das Rufzeichen `DO5AMF` mit dem Passwort `changeMe` und schaltet
ein, was nur mit passender Gegenstelle sinnvoll ist: den UDP-Empfang für UCXLog, den
Abgleich über eine Logdatei, die Frequenzübernahme, AirScout, den lokalen DX-Cluster-Server
und den Zweitchat. Ein Neuling ohne Logprogramm hat das alles aktiv, ohne es zu wissen.

Der Assistent fragt das Nötige ab, schaltet ab, was nicht vorhanden ist, und lässt den Rest
auf seinen Werten.

## Wann er erscheint

Ein Profil gilt als **unkonfiguriert**, wenn sein Rufzeichen leer ist oder wenn es noch die
Anmeldedaten der Vorlage trägt (`DO5AMF` mit `changeMe`). Das erste trifft auf jedes neu
angelegte und jedes duplizierte Profil zu, weil `OperatorProfileManagementService` dort
Rufzeichen und Passwort löscht; das zweite auf das Stammprofil einer frischen Installation.

Die Regel gilt **pro Profil**, nicht pro Installation, und sie braucht kein eigenes Merkmal
in `preferences.xml`: ohne Rufzeichen lässt sich ohnehin nicht verbinden.

- Beim Start eines unkonfigurierten Profils öffnet sich der Assistent als eigenes Fenster,
  **bevor** die Laufzeit des Profils gebaut wird: nach der Profilwahl, vor dem
  `ChatController`.
- Schließt er sich, gleich auf welchem Weg, geht der Start weiter wie heute, und das
  Einstellungsfenster öffnet sich. Es bleibt der Weg in den Chat.
- Später ist er über „Optionen → Einrichtungsassistent" erreichbar, für jedes Profil. Dort
  kostet er die laufende Sitzung, siehe „Mechanismus", und fragt vorher.
- Assistent und Einstellungsfenster sind nie zugleich offen. Der Aufruf aus dem Menü läuft
  durch `SettingsWindow.settleThen`, damit offene Änderungen dort erst entschieden sind.

## Drei Gruppen

Der Assistent ordnet, was er fragt, in drei Gruppen. Die Ordnung folgt der Frage, **woran**
eine Angabe hängt, nicht dem Reiter, auf dem sie heute steht.

**Persönliches** hängt am Operator: Sprache, Anzeigename (`stn_loginNameMainCat`, und
`stn_loginNameSecondCat` folgt ihm, solange es nicht eigens gesetzt wurde).

**Anmeldung**: `stn_loginCallSign`, `stn_loginPassword`.

**Station** hängt am Standort und an der Technik:

- gefragt: Locator (`stn_loginLocatorMainCat`), aktive Bänder (`stn_bandActive…`),
  Hauptkategorie, Zweitchat und dessen Kategorie, Logprogramm mit seinen Angaben
  (`logsynch_…`, `trxSynch_ucxLogUDPListenerEnabled`, `notify_dxClusterServerEnabled`),
  PSTRotator (`stn_pstRotator…`), AirScout (`AirScout_…`);
- gefragt, aber freiwillig: Öffnungswinkel der Antenne (`stn_antennaBeamWidthDeg`) und die
  eigenen Werte der Pfadanalyse — Antennengewinn, Antennenhöhe, Sendeleistung,
  Standorthöhe über NN (`stn_pathAnalysisOwnAntennaGainDbi`, `…OwnAntennaHeightMeters`,
  `…OwnTxPowerWatts`, `…OwnHeightAslMeters`);
- nie gefragt, aber Teil der Station: QTF-Vorgabe, größte Entfernung, das
  DEM-Verzeichnis, die Annahmen über die Gegenstation und die Dämpfungs- und
  Empfindlichkeitswerte der Pfadanalyse (`stn_qtfDefault`, `stn_maxQRBDefault`, übrige
  `stn_pathAnalysis…`).

Der Locator steht im Einstellungsfenster bei den Anmeldefeldern und bleibt dort. Im
Assistenten gehört er zur Station: wer mit demselben Rufzeichen an zwei Standorten arbeitet,
hat zwei Locator und eine Anmeldung.

## Die Einstiegsseite

Anmeldung und Station sind zwei unabhängige Entscheidungen. Für jede fragt die erste Seite,
woher sie kommt:

| Wahl | Bedeutung | angeboten, wenn |
|---|---|---|
| neu eingeben | die Schritte der Gruppe erscheinen, mit den Startwerten des Assistenten | immer |
| von Profil X übernehmen | die Werte der Gruppe aus einem anderen Profil | ein anderes, konfiguriertes Profil existiert |
| unverändert lassen | die Werte, die das Profil schon hat | Anmeldung: das Profil ist konfiguriert. Station: das Profil trägt nicht die Anmeldedaten der Vorlage |

Nichts ist vorausgewählt; der Assistent rät nicht, welcher Fall vorliegt. Bleibt für beide
Gruppen nur „neu eingeben" übrig — die frische Installation —, entfällt die Seite.

Damit sind die Betriebsarten abgedeckt, ohne dass der Assistent sie beim Namen kennen muss:

| Fall | Anmeldung | Station |
|---|---|---|
| Erste Installation | neu | neu |
| Mehrmannbetrieb, gemeinsames Rufzeichen | übernehmen | übernehmen oder unverändert |
| Mehrmannbetrieb, eigenes Rufzeichen | neu | übernehmen oder unverändert |
| Ein Operator, mehrere Standorte | übernehmen | neu |
| Neuer Operator, neue Station | neu | neu |

„Unverändert" ist der Fall des duplizierten Profils: `duplicateProfile` hat die Station
schon kopiert und nur die Anmeldung geleert. „Übernehmen" ist derselbe Fall für ein leer
angelegtes Profil.

Die Wahl bestimmt, **womit die Schritte vorbelegt sind**, und ob sie im Ablauf erscheinen:

- neu → Startwerte des Assistenten, Schritte erscheinen;
- übernehmen → Werte des gewählten Profils, Schritte erscheinen nicht;
- unverändert → Werte des eigenen Profils, Schritte erscheinen nicht.

In allen drei Fällen zeigt die Zusammenfassung die Werte, und von dort lässt sich jeder
Schritt öffnen und ändern. Ein übernommenes Rufzeichen bekommt so sein `/P`.

Eine Ausnahme hat das Logprogramm. Das Einstellungsfenster erlaubt mehrere Logquellen
zugleich, die Frage des Assistenten nur eine. Hat ein unverändert gelassenes Profil mehr als
eine Quelle eingeschaltet, zeigt die Zusammenfassung das an und hält die Seite geschlossen:
sie würde beim Öffnen alle Quellen bis auf eine abschalten.

**Bei einer unverändert gelassenen Station wird nur geschrieben, was geöffnet wurde.**
Öffnet der Nutzer weder Antenne noch Logprogramm noch Rotor und AirScout, fasst der
Assistent diese Einstellungen nicht an. Die Zusammenfassung zeigt dann, was im Profil steht,
auch wo das Einstellungsfenster mehr erlaubt als die Fragen des Assistenten: einen
DX-Cluster-Server oder eine Frequenzquelle ohne Logquelle.

Bei einer **neu eingerichteten** Station gehören alle Seiten zum Lauf. Wer sie mit „Fertig"
überspringt, bekommt ihre Startwerte geschrieben: Integrationen aus, Antennenwerte wie
gespeichert.

Die **Startwerte des Assistenten** für eine neue Station sind bewusst nicht die der Vorlage:
kein Band gewählt, Locator leer, kein Zweitchat, kein Logprogramm, kein lokaler
DX-Cluster-Server, kein Rotor, kein AirScout. Wer „neu einrichten" wählt, bekommt nur
eingeschaltet, was er bejaht hat.

Die Zahlen des Schritts „Antenne und Leistung" haben keinen leeren Zustand. Sie bleiben auch
bei einer neuen Station mit den gespeicherten Werten vorbelegt und sind Vorschläge.

## Die Schritte

1. **Persönliches** — Sprache, Anzeigename. Immer.
2. **Anmeldung** — Rufzeichen, Passwort.
3. **Standort und Bänder** — Locator, Bänder, Hauptkategorie. Der Assistent schlägt die
   Kategorie aus den Bändern vor (1 für 50/70 MHz, 2 für 144/432 MHz, 3 ab 1240 MHz), der
   Nutzer bestätigt sie. Der Zweitchat ist eine Ja/Nein-Frage; bei Ja folgt seine Kategorie.
   Aus der Bandwahl wird **keine Frequenz** abgeleitet.
4. **Antenne und Leistung** — Öffnungswinkel, Antennengewinn, Antennenhöhe über Grund,
   Sendeleistung, Standorthöhe über NN. Freiwillig: der Schritt lässt sich unverändert
   durchgehen. Ein leeres Feld für die Standorthöhe heißt, wie im Reiter Station, dass das
   Höhenmodell entscheidet. Die Werte speisen die Pfadanalyse und die Antennenkeule auf
   der Karte; für den Chatbetrieb braucht sie niemand.
5. **Logprogramm** — keines, Win-Test, UCXLog/DXLog.net/N1MM+/QARTEST, oder eine Logdatei.
   Erst nach der Wahl erscheinen die zugehörigen Angaben (Port, Stationsnamen, Datei),
   vorbelegt mit den gespeicherten Werten. Mit einem Netzwerk-Logprogramm folgen zwei
   Ja/Nein-Fragen: eigene Frequenz übernehmen, Spots über den lokalen DX-Cluster-Server
   an das Logprogramm geben.
6. **Rotor und AirScout** — je eine Ja/Nein-Frage; Host, Port und Kennungen erst bei Ja.
7. **Zusammenfassung** — alle drei Gruppen, das Passwort verdeckt, je Gruppe „Ändern". Was
   der Assistent **abschaltet**, steht dort ausdrücklich. Darunter „Speichern" und
   „Speichern und verbinden".

„Fertig" führt von jedem Schritt zur Zusammenfassung, sobald die **Pflichtangaben**
stehen: Anzeigename, Rufzeichen, Passwort, Locator, mindestens ein Band und die
Chatkategorie. Vorher wäre die Zusammenfassung nicht speicherbar. Wer eine neue Station
einrichtet, kann also nach Schritt 3 fertig sein; wer die Station übernimmt, nach der
Anmeldung. Niemand muss durch Antenne, Logprogramm und Rotor, um in den Chat zu kommen.

„Später" und das Schließen des Fensters schreiben nichts, auch die Sprache nicht: der
Assistent zeigt sich sofort in der gewählten Sprache, gespeichert wird sie erst mit allem
anderen.

## Was unsichtbar bleibt

Serveradresse und -port, Baken, automatische Antworten, Kurzbefehle und Textbausteine,
Töne, Farben, Filter, SO2R, die Worked-Datenbank, der Fehlermitschnitt, die Fensteranordnung
und alle Profilaktionen. Der Assistent fasst keinen dieser Werte an.

Die automatische Antwort auf Frequenzanfragen (`autoAnswerToQrgRequestEnabled`) steht in der
Vorlage auf an. Ob der Assistent sie bei einer Station ohne Frequenzquelle abschalten soll,
ist **nicht entschieden** und nicht Teil dieses Entwurfs.

## Mechanismus

**Der Assistent schreibt `preferences.xml`, und die Laufzeit liest sie beim Start.** Er
ändert keine laufende Sitzung.

Das Einstellungsfenster muss beides können: einen Wert speichern und die laufende Sitzung
nachführen — einen Listener neu binden, den Rotor neu einrichten, die Kategorie am
`ChatController` setzen. Dafür gibt es `SettingsDraft` mit seinen Wirkungen. Der Assistent
braucht das zweite nicht, denn er läuft, bevor es eine Sitzung gibt. Was er schreibt, liest
`startRuntime` danach so ein, wie es jede gespeicherte Einstellung einliest. Dieser Weg ist
der am besten erprobte im Programm: jeder Start und jeder Profilwechsel geht ihn.

Im Einzelnen:

- Der Assistent arbeitet auf einem eigenen `ChatPreferences`-Objekt, das die Datei des
  Profils gelesen hat, so wie `OperatorProfileManagementService.createPreferencesFile` es
  beim Anlegen eines Profils tut. Bis zum Speichern hält er seine Antworten in einem
  `SetupWizardState` und fasst dieses Objekt nicht an.
- „Speichern" setzt die Antworten über die Setter von `ChatPreferences` und ruft
  `writePreferencesToXmlFile`. Alles, was der Assistent nicht gefragt hat, steht danach
  unverändert in der Datei, weil es gelesen und wieder geschrieben wurde.
- **Beim Start** gibt es danach nichts neu zu laden: die Laufzeit wird erst jetzt gebaut.
- **Aus dem Menü** läuft eine Laufzeit. Der Assistent fragt deshalb zuerst, ob die Sitzung
  aufgegeben werden darf, wie der Profilwechsel fragt. Dann wird die Laufzeit abgebaut,
  der Assistent gezeigt, und danach das **selbe** Profil neu gestartet — der Weg von
  `ApplicationRuntimeLauncher.switchProfile`, der ein Ziel gleich dem aktiven Profil
  zulässt. Abgebaut wird **vor** dem Assistenten, nicht danach: sonst schriebe die
  Laufzeit beim Abbau (`layoutAutosave.flushPending`) über die Datei, die der Assistent
  gerade geschrieben hat.
- **Übernehmen von Profil X** liest dessen Datei in ein zweites `ChatPreferences`-Objekt
  und belegt die Antworten der Gruppe damit vor. Geschrieben wird nur in das aktive Profil.
  `duplicateProfile` bleibt unverändert und löscht die Anmeldedaten weiterhin: das
  Duplizieren kopiert sie nie von selbst, im Assistenten wählt der Nutzer es ausdrücklich.
- **„Speichern und verbinden"** merkt sich den Wunsch; nach dem Start der Laufzeit geht er
  den Weg des Verbinden-Knopfs (`connectFromSettings`).

`SetupWizardState` kennt weder Fenster noch Laufzeit und ist ohne Oberfläche prüfbar. Weil
er nur ein `ChatPreferences`-Objekt braucht, hängt er an keinem Reiterzustand, und die
Frage, welche Eigenschaft eines Reiterzustands von außen setzbar ist, stellt sich nicht.

**Die Erkennung** ist eine Abfrage in `core` auf `ChatPreferences`. Die Anmeldedaten der
Vorlage stehen als Konstanten daneben; ein Test liest die mitgelieferte
`praktiKSTpreferences.xml` und hält beide gleich.

Der Preis dieses Wegs: der Assistent aus dem Menü beendet die Sitzung. Das ist hingenommen.
Er ist für die Einrichtung da; wer im Contest einen Port ändert, nimmt das
Einstellungsfenster, das dafür gebaut ist.

## Fäden

Beim Start läuft der Assistent auf dem Weg, den die Profilwahl geht
(`OperatorProfilePickerWindow.showAndSelect`): ein modaler Dialog, auf dem
AWT-Ereignisfaden gebaut, der seinen Aufrufer anhält und dabei weiter Ereignisse
verarbeitet. **Kein** eigener Faden mit Riegel — genau diese Bauart hat sich bei der
Profilwahl verklemmt, die Begründung steht dort im Quelltext. Aus dem Menü kommt der Aufruf
vom Ereignisfaden und geht denselben Weg.

Der Assistent liest die Profilliste und höchstens zwei Profildateien, kleine lokale Dateien.

Netzprüfungen gibt es in diesem Entwurf nicht. Ein Probe-Login wäre eine zweite echte
Sitzung am Server, und ein Lauschen auf UDP sagt bei Stille nichts: die Logprogramme senden
nur bei einem QSO oder Statuswechsel. Geprüft wird lokal — Form von Rufzeichen und Locator,
Portkollisionen wie in `StationTabState` — und danach zeigt „Speichern und verbinden" den
tatsächlichen Anmeldestatus.

## Ausbaustufen

1. Erkennung, Fenster, Menüeintrag, Einstiegsseite mit „neu" und „unverändert", Schritte
   1 bis 3, Zusammenfassung, Speichern, Speichern und verbinden.
2. Schritte 4 bis 6: Antenne und Leistung, dann Logprogramm, Rotor und AirScout mit den
   Startwerten, die Integrationen abschalten.
3. „Von Profil X übernehmen".

Stufe 1 und 2 sind umgesetzt. Offen ist Stufe 3.

## Unverändert

Das Format von `preferences.xml`. Das Einstellungsfenster mit seinen zwölf Reitern, ihrer
Reihenfolge und ihren Titeln. Der Vertrag von `SettingsDraft.apply`. Die Sperre der
Anmeldefelder während einer Sitzung. Das Anlegen und Duplizieren von Profilen. Die
Vorlage `praktiKSTpreferences.xml`. Der Start- und der Profilwechselweg. Kein
automatisches Verbinden, außer der Nutzer wählt „Speichern und verbinden".

## Prüfung

- Erkennung: leeres Rufzeichen, Anmeldedaten der Vorlage, konfiguriertes Profil; die
  Konstanten gegen die mitgelieferte Vorlage.
- Einstiegsseite: welche Wahl in welcher Lage angeboten wird, und dass sie auf der frischen
  Installation entfällt.
- Ablauf: die fünf Fälle der Tabelle, „Fertig" erst mit allen Pflichtangaben, Zurück erhält die
  Eingaben, die Zusammenfassung nennt das Abgeschaltete.
- Schreiben, gegen eine echte Datei wie in `ChatPreferencesRoundTripTest`: nach „Später"
  ist die Datei Byte für Byte unverändert; nach „Speichern" liest ein frisches
  `ChatPreferences` genau die gewählten Werte; jedes nicht gefragte Feld hat seinen alten
  Wert; eine neue Station ohne Logprogramm hat alle Abgleiche aus.
- Aus dem Menü: die Rückfrage kommt vor dem Abbau, „Zurück" lässt die Sitzung stehen, und
  nach dem Speichern steht nichts von der abgebauten Laufzeit in der Datei.
- Übernehmen: nur die Felder der gewählten Gruppe wandern, das Quellprofil bleibt
  unberührt.
- `SettingsFieldCoverageTest` und `SettingsForeignWriteCoverageTest` bleiben unberührt:
  sie prüfen, dass das Einstellungsfenster jeden Setter noch ruft, und der Assistent nimmt
  dort nichts weg.
- Oberflächentexte in `strings_de.properties` und `strings_en.properties`; der Erzeuger
  prüft beim Bau.
