# Konfiguration

> 🇬🇧 [English version](en-Configuration) | 🇩🇪 Du liest gerade die deutsche Version

Nach dem ersten Start öffnet sich das **Einstellungsfenster** – dieses ist der zentrale Ausgangspunkt für alle Konfigurationen. Es empfiehlt sich, das Einstellungsfenster während des Betriebs geöffnet zu lassen (z. B. um den Beacon schnell ein- und auszuschalten).

> **Wichtig**: Änderungen im Einstellungsfenster wirken, sobald **Apply/Close prefs** oder **Save Settings** gedrückt wird; **Connect** übernimmt sie ebenfalls vorher. Wer mit offenen Änderungen das Fenster schließt, das Profil wechselt oder das Programm beendet, wird zuerst gefragt, ob sie übernommen oder verworfen werden sollen, oder kann zurück. Farben, Sprache und die Anmeldedaten im Reiter **Station** wirken sofort.
>
> Fachliche Einstellungen mit **„Save Settings"** sichern, wenn sie beim nächsten Start wieder gelten sollen. Layoutänderungen wie Fenstergrößen, Divider-Positionen und Tabellenbreiten speichert KST4Contest automatisch. Die gemeinsame Datei liegt unter Linux und macOS in `~/.praktiKST/preferences.xml` und unter Windows in `%USERPROFILE%\.praktiKST\preferences.xml` (bzw. `C:\Users\<Benutzername>\.praktiKST\preferences.xml`).

---

## Station Settings (Stationseinstellungen)

![Stationseinstellungen](client_settings_window_station.png)

### Login und Chat-Kategorien

Hier werden das lokale Login-Rufzeichen und das Passwort für den ON4KST-Chat eingetragen. Zudem wird die **primäre Chat-Kategorie** (z. B. IARU Region 1 VHF/Microwave) ausgewählt.

Mit der Option für einen **zweiten Chat** (Multi-Channel-Login) wird eine weitere Kategorie (z. B. UHF/SHF) per Single Sign-on in derselben ON4KST-TCP-Sitzung aktiviert. KST4Contest öffnet dafür keine zweite TCP-Verbindung und verwendet weder ein zweites lokales Login-Rufzeichen noch ein zweites Passwort.

**Name in Chat 2** konfiguriert ausschließlich das sichtbare, kategoriebasierte Namensfeld der zweiten Kategorie. Es ist weder ein zweites Login noch ein Nachrichtenempfänger. Das sichtbare Namensfeld, der Nachrichtenkontext, die QRG und der Beacon bleiben für beide Kategorien getrennt. **Opposite Station Multi-Callsign Login-Tagging** bezieht sich dagegen ausschließlich auf Gegenstationen, die mit mehreren vollständigen sichtbaren Rufzeichenvarianten im Chat erscheinen.

### Rufzeichen und Locator

Eigenes Rufzeichen und Maidenhead-Locator (6-stellig, z. B. `JN49IJ`) eintragen. Der Locator ist kein Bestandteil der Authentifizierung. ON4KST verwendet ihn gemeinsam für beide Kategorien der einen TCP-Sitzung; KST4Contest benötigt ihn außerdem für Distanz- und Richtungsberechnungen.

### Aktivierte Bänder

Über die Checkboxen **My station uses …** wird festgelegt, auf welchen Bändern die eigene Station im aktuellen Setup arbeiten kann. Unterstützt werden 50 MHz, 70 MHz, 144 MHz, 432 MHz, 1296 MHz, 2320 MHz, 3400 MHz, 5760 MHz und 10 GHz.

Die Auswahl steuert nicht nur die sichtbaren Bandspalten. Sie wird außerdem verwendet für:

- die bandbezogenen Worked- und NOT-QRV-Filter,
- die im **Further Info**-Bereich sichtbaren NOT-QRV-Schalter,
- die Herleitung von `a`- und `B+`-Bandmöglichkeiten,
- den Filter **New bands**,
- den Band-Upgrade-Hinweis nach einem Logeintrag und
- bandbezogene Prioritäts- und Reachability-Funktionen.

Nach einer Änderung **Save Settings** verwenden und KST4Contest neu starten. Die Bandspalten und mehrere zugehörige Bedienelemente werden beim Aufbau der Benutzeroberfläche erzeugt und deshalb nicht vollständig in der laufenden Sitzung ergänzt oder entfernt.

### Antennen-Öffnungswinkel (Antenna Beamwidth)

Trage den vollständigen horizontalen Öffnungswinkel der eigenen Antenne in Grad ein. KST4Contest verwendet jeweils die Hälfte dieses Werts links und rechts der gewählten beziehungsweise hergeleiteten Antennenrichtung. Ein eingetragener Wert von `70°` entspricht daher einem Korridor von `±35°`.

Der Wert wird an mehreren Stellen verwendet:

- für den QTF-Filter der Benutzerliste,
- für die Darstellung des eigenen Antennenkorridors,
- als angenommener Öffnungswinkel einer fremden Station bei der [Herleitung von Richtungsgelegenheiten](de-Funktionen#richtungsgelegenheiten-aus-gerichteten-nachrichten).

Der letzte Punkt ist bewusst eine Näherung. ON4KST überträgt weder die verwendete Antenne noch deren Öffnungswinkel. KST4Contest verwendet deshalb den eigenen Wert als praktikable Annahme für die Gegenstation.

Wähle einen realistischen Wert. Ein zu großer Öffnungswinkel erzeugt viele geometrische Treffer, die praktisch kaum noch eine Aussage haben. Ein zu kleiner Wert kann dagegen brauchbare Richtungsgelegenheiten ausblenden.

### Standard-Maximum-QRB

Trage die maximale Entfernung in Kilometern ein, innerhalb der KST4Contest Richtungsgelegenheiten berücksichtigen soll. Maßgeblich ist die Entfernung zwischen der eigenen Station und dem Absender der gerichteten Nachricht – nicht die Entfernung zwischen Absender und Empfänger.

Liegt der Absender weiter entfernt, wird die Situation auch dann nicht hervorgehoben und nicht als Richtungsgelegenheit an den lokalen DX-Cluster-Server weitergegeben, wenn der berechnete Winkel passen würde.

Der Wert sollte zum eigenen Stationsaufbau und zum vorgesehenen Contestbetrieb passen. Ein unnötig großer Bereich erzeugt Hinweise für Stationen, die praktisch nicht mehr zum Arbeitsbereich gehören; ein zu kleiner Bereich blendet mögliche Kandidaten bereits vor der Richtungsbewertung aus.

---

### Streckenanalyse und Link-Budget

Die Stationskarte verwendet mehrere Werte aus dem Reiter **Station**, um das Geländeprofil und das Link-Budget zur ausgewählten Gegenstation zu berechnen. Die Angaben zum eigenen Stationsaufbau sollten deshalb möglichst realistisch sein. Bei der Gegenstation handelt es sich dagegen um globale Annahmen, solange keine genaueren Daten vorliegen.

| Einstellung | Verwendung |
|---|---|
| **Own antenna height AGL** | Höhe der eigenen Antenne über dem lokalen Gelände in Metern |
| **Eigene Standorthöhe über NN, Meter** | Optional: Geländehöhe des eigenen Standorts über dem Meeresspiegel. Leer lassen, um das Höhenmodell zu verwenden |
| **Own TX power W** | Eigene Sendeleistung in Watt |
| **Own ant. gain dBi** | Gewinn der eigenen Antenne in dBi |
| **DX OM TX power W** | Angenommene Sendeleistung der Gegenstation in Watt |
| **DX OM ant. gain dBi** | Angenommener Antennengewinn der Gegenstation in dBi |
| **Gegenstation auf dem höchsten Punkt ihres Locatorfelds** | Lässt die Strecke am höchsten Punkt des Locatorfelds der Gegenstation enden statt in dessen Mitte (standardmäßig eingeschaltet) |

**AGL** bedeutet *Above Ground Level*. Trage hier nicht die Höhe über dem Meeresspiegel ein. Die Geländehöhe am eigenen Standort stammt bereits aus dem Höhenprofil; KST4Contest addiert die konfigurierte Antennenhöhe zu diesem Wert.

Dieses Feld nimmt nur die Antennenhöhe über Grund auf; für die Höhe des Standorts über dem Meeresspiegel gibt es ein eigenes Feld, das im nächsten Absatz beschrieben ist.

Das Höhenprofil nimmt die Geländehöhe am Mittelpunkt des eigenen Locatorfelds. Liegt der tatsächliche Standort deutlich höher oder tiefer, etwa auf einem Hügel am Rand des Feldes, trägst du seine Geländehöhe unter **Eigene Standorthöhe über NN, Meter** ein. KST4Contest verwendet dann diesen Wert statt des Höhenmodells und addiert die Antennenhöhe weiterhin darauf. Zulässig sind Werte von -500 bis 9000 Metern. Ein leeres Feld stellt das Höhenmodell wieder her. Die eigene Position auf der Karte und im Profil bleibt der Locatormittelpunkt.

Ein Locator sagt nur, in welchem Feld eine Gegenstation steht, nicht wo darin. Conteststationen arbeiten meist vom höchsten Punkt ihres Feldes aus. Ist **Gegenstation auf dem höchsten Punkt ihres Locatorfelds** eingeschaltet, tastet KST4Contest das Feld der Gegenstation mit einem Raster von 5 x 5 Punkten ab, nimmt den höchsten davon und lässt die Strecke dort enden, mit Position und Höhe dieses Punktes. Die Suche braucht je Locatorfeld der Gegenstation eine Anfrage an den Online-Höhendienst; das Ergebnis bleibt bis zum Beenden von KST4Contest erhalten. Schlägt die Suche fehl, rechnet die Analyse wie bisher mit der Feldmitte und versucht es einige Minuten später erneut.

Für die Gegenstation verwendet KST4Contest derzeit eine feste Antennenhöhe von 10 Metern über dem lokalen Gelände. Sendeleistung und Antennengewinn der Gegenstation stammen aus den beiden **DX OM**-Feldern. Diese Werte sind bewusst nur Annahmen: Der ON4KST-Chat überträgt weder die tatsächliche Antennenhöhe noch die vollständigen Stationsdaten der Gegenstation.

Antennengewinne müssen in `dBi` eingetragen werden. Liegt ein Wert in `dBd` vor, muss vor der Eingabe `2.15 dB` addiert werden:

```text
dBi = dBd + 2.15 dB
```

Die aktuelle QTF, der konfigurierte Antennen-Öffnungswinkel und das Standard-Maximum-QRB beeinflussen die Darstellung der Stationskarte. Die für die eigene Station aktivierten Bänder und die für die Gegenstation hergeleitete Frequenz wirken sich zusätzlich auf die Auswahl der Analysefrequenz aus.

Für das Geländeprofil berücksichtigt KST4Contest die Antennenhöhen, das Höhenmodell und die Erdkrümmung mit einem festen effektiven Erdradiusfaktor von `k = 4/3`. Das Link-Budget verwendet außerdem:

- die Entfernung und die aktuelle Analysefrequenz,
- die Sendeleistungen und Antennengewinne beider Stationen,
- frequenzabhängig geschätzte Speiseleitungsverluste,
- die Freiraumdämpfung und
- eine grobe zusätzliche Dämpfung durch das maßgebliche Hindernis im Geländeprofil.

Die Berechnung erfolgt für beide Übertragungsrichtungen. Für die gemeinsame SSB- beziehungsweise CW-Marge ist die ungünstigere Richtung maßgeblich. Zu optimistische Leistungs- oder Antennenwerte verbessern daher zwar das angezeigte Ergebnis, nicht aber den realen Funkweg.

Die Werte bleiben technische Abschätzungen. Aktuelle Ausbreitungsbedingungen, lokale Abschattungen, Bewuchs, Gebäude, Störungen und nicht bekannte Stationsparameter können das tatsächliche Ergebnis deutlich verändern. Bedienung, Frequenzauswahl und Grenzen der Berechnung sind unter [Stationskarte und Streckenanalyse](de-Funktionen#stationskarte-und-streckenanalyse-ab-v141) beschrieben.

---



## Log-Sync-Einstellungen

Drei Methoden stehen zur Verfügung, um gearbeitete Stationen automatisch zu markieren. Details: [Log-Synchronisation](de-Log-Synchronisation).

### Universal File Based Callsign Interpreter (Simplelogfile)

Liest die ausgewählte Textdatei einmal pro Minute mit einem fest eingebauten Rufzeichenmuster. Die Treffer werden auf Basisrufzeichen normalisiert und setzen für alle aktiven Varianten nur den globalen Worked-Status. Band- und Locatorinformationen können daraus nicht abgeleitet werden. Die Funktion eignet sich als Fallback für Logprogramme ohne unterstützte Netzwerkschnittstelle. Vor der Verwendung sollte deshalb geprüft werden, ob das eingesetzte Logprogramm bereits eine der unterstützten Netzwerkschnittstellen bereitstellt.

Existiert die ausgewählte Datei noch nicht, legt KST4Contest sie an und zeigt einmalig den vollständigen Pfad sowie Hinweise zum Funktions- und Contesttest. Die Datei ist selbst die dauerhafte Quelle; aus ihr abgeleitete Worked-Markierungen werden nicht in SQLite gespeichert und nicht automatisch für einen neuen Contest zurückgesetzt.

### Netzwerk-Listener für QSO-UDP-Broadcast

**Empfohlene Methode.** KST4Contest hört auf UDP-Pakete, die das Logprogramm beim Speichern eines QSOs an die Broadcast-Adresse sendet. Die Stationen werden mit Bandinformation markiert. UDP-Port: Standard **12060**. (Wird z. B. von UCXLog, N1MM+, QARTest, DXLog.net genutzt).

### Win-Test Network-Listener (Zusätzlicher UDP-Listener)

Dedizierter Netzwerk-Erkenner für Win-Test. KST4Contest empfängt und verarbeitet Win-Test-spezifische UDP-Pakete (inkl. Sked-Übergabe) auf dem dafür konfigurierten Port.

---

## TRX-Sync-Einstellungen

Die TRX-Synchronisation übernimmt die aktuelle Frequenz aus dem Logprogramm und stellt sie in KST4Contest als eigene QRG der ersten Chat-Kategorie bereit. QSO- und Frequenzsynchronisation verwenden teilweise denselben UDP-Empfänger, sind funktional aber voneinander getrennt: Ein empfangenes `RadioInfo`-Paket markiert keine Station als gearbeitet, und ein QSO-Paket ändert nicht automatisch die eigene QRG.

![Einstellungen für die TRX-Synchronisation](client_settings_window_trxsync.png)

### Verfügbare QRG-Quellen

| Quelle | Aktivierung | Verhalten |
|---|---|---|
| **Allgemeiner RadioInfo-Listener** | `Update MYQRG from RadioInfo messages received on the shared log-sync port` | Verarbeitet kompatible `RadioInfo`-Pakete auf dem gemeinsam mit der QSO-Synchronisation verwendeten UDP-Port. Der Standardport ist `12060`. |
| **Win-Test STATUS** | `Win-Test STATUS QRG Sync` | Verarbeitet die Haupt- oder Pass-Frequenz aus nativen Win-Test-`STATUS`-Paketen. Der Win-Test-Listener verwendet seinen separat konfigurierten Port, standardmäßig `9871`. |
| **Manuelle Eingabe** | Beide automatischen QRG-Quellen deaktivieren | Die eigene QRG kann im Hauptfenster von Hand eingetragen werden. |

Der allgemeine Listener ist für Logprogramme vorgesehen, die kompatible `RadioInfo`-Pakete senden. Dazu gehören – abhängig von deren jeweiliger Konfiguration – UCXLog, N1MM+, QARTest und DXLog.net. QSO- und `RadioInfo`-Pakete verwenden denselben unter **Log sync** konfigurierten Port. Dort wird jedoch getrennt festgelegt, ob KST4Contest QSO-Informationen, TRX-Informationen oder beide Paketarten verarbeitet. Eine automatische QRG-Quelle muss aktiviert sein und tatsächlich gültige Pakete liefern; die Aktivierung allein aktualisiert `MYQRG` nicht.

Wird der gemeinsame UDP-Port geändert, muss KST4Contest neu gestartet werden. Eine reine Änderung der Checkboxen wird dagegen berücksichtigt, sobald sie übernommen ist.

### Welche QRG wird aktualisiert?

Beide automatischen Quellen aktualisieren ausschließlich `MYQRG`. Das ist die eigene QRG der ersten beziehungsweise primären Chat-Kategorie.

Bei aktiviertem zweiten Chat bleibt dessen QRG davon unabhängig. Sie wird nicht aus den empfangenen TRX-Paketen abgeleitet und steht als `SECONDQRG` zur Verfügung. Dadurch kann beispielsweise die erste Kategorie automatisch der Frequenz des Logprogramms folgen, während für die zweite Kategorie eine eigene QRG von Hand eingetragen wird.

Sobald mindestens eine automatische QRG-Quelle aktiviert ist und einen gültigen Wert liefert, folgt das QRG-Feld der ersten Kategorie im Hauptfenster der empfangenen Frequenz. Bleiben die erwarteten Pakete aus, sollte die Schnittstelle vor dem Contest geprüft werden. Eine manuelle Eingabe in dieses Feld ist möglich, wenn sowohl der allgemeine RadioInfo-Listener als auch die Win-Test-STATUS-Synchronisation deaktiviert sind.

### Haupt- oder Pass-Frequenz aus Win-Test

Standardmäßig verwendet KST4Contest die Hauptfrequenz des empfangenen Win-Test-`STATUS`-Pakets.

Die Option `Use pass frequency from Win-Test STATUS` verwendet stattdessen die im Paket enthaltene Pass-Frequenz. Das ist beispielsweise sinnvoll, wenn Win-Test im Split-Betrieb eine abweichende Frequenz führt und genau diese im Chat als Arbeitsfrequenz veröffentlicht werden soll.

Enthält das Paket keine gültige Pass-Frequenz, fällt KST4Contest automatisch auf die Hauptfrequenz zurück. Eine fehlende Pass-Frequenz löscht daher weder `MYQRG` noch ersetzt sie den Wert durch eine offensichtlich falsche Zahl.

Die Frequenzen werden einheitlich im KST4Contest-Format dargestellt, beispielsweise:

```text
50.300.00
144.300.00
1296.100.00
10368.100.00
```

Die Werte werden erst beim Auflösen des Nachrichtentextes eingesetzt. Ändert das Logprogramm zwischen zwei Beacon-Läufen die Frequenz, verwendet die nächste Nachricht bereits den aktualisierten Wert.

Die eigene QRG kann außerdem als Fallback für die Übergabe eines Skeds an Win-Test verwendet werden. Das geschieht nur, wenn die QRG auswertbar ist und zum ausdrücklich ausgewählten Sked-Band gehört. Einzelheiten stehen unter [Log-Synchronisation](de-Log-Synchronisation#skeds-an-win-test-übergeben).

Weitere Informationen zu den Textvariablen: [Makros und Variablen](de-Makros-und-Variablen#variablen).

### Mehrere Logger oder Funkgeräte

Ohne SO2R schreiben alle aktivierten QRG-Quellen in denselben Wert `MYQRG`. KST4Contest ordnet eingehende `RadioInfo`- oder `STATUS`-Pakete weder einem bestimmten Funkgerät noch einer Chat-Kategorie zu.

Sind der allgemeine RadioInfo-Listener und die Win-Test-Synchronisation gleichzeitig aktiviert, bestimmt deshalb das zuletzt verarbeitete Paket die angezeigte QRG. Dasselbe gilt, wenn mehrere Logger ihre Frequenzpakete an dieselbe KST4Contest-Instanz senden.

Für ein Setup mit mehreren Funkgeräten gilt daher:

- QSO-Pakete dürfen von mehreren Loggern empfangen werden.
- Frequenzpakete sollten nur von der Quelle gesendet werden, die `MYQRG` tatsächlich steuern soll.
- In einem Win-Test-Netzwerk sollte zusätzlich der Stationsfilter verwendet werden.
- Werden zwei vollständig unabhängige QRG-Synchronisationen benötigt, sind zwei getrennte KST4Contest-Instanzen die eindeutigere Lösung.

Anders ausgedrückt: Mehrere Worked-Quellen lassen sich sinnvoll zusammenführen. Mehrere gleichzeitig sendende Frequenzquellen erzeugen dagegen keine zusätzliche Information, sondern lediglich einen Wettbewerb darum, welches Paket zuletzt angekommen ist.

### SO2R: zwei Funkgeräte getrennt führen

Mit der Option **SO2R: zwei Geräte getrennt führen (Gerät 2 schreibt SECONDQRG)** ordnet KST4Contest die Frequenzpakete dem jeweiligen Funkgerät zu. Die Option ist standardmäßig aus; ohne SO2R verhält sich alles wie in den Abschnitten oben beschrieben.

- **N1MM+, DXLog.net, UCXLog:** Die Zuordnung erfolgt über das Feld `RadioNr` im `RadioInfo`-Paket. Dafür ist nichts einzustellen. Gerät 1 setzt `MYQRG`, Gerät 2 setzt `SECONDQRG`.
- **Win-Test:** Hier gibt es kein `RadioNr`, deshalb wird je Gerät ein Stationsname eingetragen, zum Beispiel `STN1` für Gerät 1 im bisherigen Stationsfilter und `STN2` im Feld **Win-Test-Stationsname von Gerät 2**. Das Feld ist ohne SO2R ausgegraut.
- **Zweiter PSTRotator:** Nach **Apply** erscheint im Reiter **Station** der Abschnitt **PSTRotator 2 (SO2R)** mit Schalter, Host und Port. Die zweite Instanz braucht einen eigenen Port, zum Beispiel `12010`; sie antwortet dann auf `12011`. Derselbe Port wie bei PSTRotator 1 oder ein direkt benachbarter wird beim Übernehmen abgelehnt. Der zweite Rotor startet mit der ON4KST-Sitzung.
- **Bedienung:** Im Bereich der gewählten Station steht neben **Antenne 1 drehen** nun **Antenne 2 drehen …**, und in der Sendezeile zeigt ein reines Anzeigefeld QTF2 die Richtung der zweiten Antenne. Der Tooltip von `SECONDQRG` nennt Gerät 2.
- **QTF-Filter:** Die QTF-Filter der Stationsliste folgen weiterhin Rotor 1.

Nach Abschluss der Konfiguration **Save Settings** verwenden.

---


## AirScout-Einstellungen

Im Reiter **AirScout** wird die UDP-Verbindung zwischen KST4Contest und AirScout eingerichtet. KST4Contest fordert dort keine allgemeinen Flugzeugdaten an, sondern übermittelt die aktuell relevanten Stationspfade. AirScout berechnet die dazu passenden Flugzeuge und sendet das Ergebnis an die anfragende KST4Contest-Instanz zurück.

Vorausgesetzt wird AirScout `0.9.9.5` oder neuer.

![AirScout-Einstellungen in KST4Contest](as_plane_feed_3.png){ width=85% }

### Einstellungen der UDP-Verbindung

| Einstellung | Standardwert | Verwendung |
|---|---:|---|
| **Enable AirScout UDP integration** | deaktiviert | Aktiviert das Senden von AirScout-Anfragen und die Verarbeitung der Antworten |
| **AirScout server identifier** | `AS` | Logischer Name der angesprochenen AirScout-Instanz |
| **KST4Contest client identifier** | `KST` | Logischer Name dieser KST4Contest-Instanz |
| **AirScout UDP port** | `9872` | Gemeinsamer UDP-Port für Anfragen und Antworten |
| **Select AirScout frequency automatically per station** | aktiviert | Ermittelt Band und Frequenz für jede Gegenstation aus dem aktuellen Stationskontext |
| **Forced AirScout band value** | `1440000` | Verwendet bei deaktivierter Automatik einen festen AirScout-Bandwert für alle Stationen |

Ist **Enable AirScout UDP integration** deaktiviert, sendet KST4Contest keine AirScout-Anfragen und verwirft eingehende AirScout-Antworten. Der UDP-Empfänger kann trotzdem gebunden bleiben, damit sich die Funktion während einer laufenden Verbindung wieder einschalten lässt.

KST4Contest verwendet für ausgehende AirScout-Pakete die Broadcast-Adresse `255.255.255.255`. Eine Ziel-IP wird deshalb nicht separat konfiguriert. AirScout und KST4Contest müssen den verwendeten UDP-Broadcast empfangen können; Router leiten einen solchen Broadcast normalerweise nicht in ein anderes Netz weiter. Bei Problemen sollten daher zuerst der UDP-Port, die lokale Firewall und die Netzzuordnung geprüft werden.

### Automatische Bandauswahl pro Station

**Auto per station** ist die empfohlene Einstellung. KST4Contest verwendet dann nicht einen festen Bandwert für alle Gegenstationen, sondern leitet eine geeignete Frequenz aus den verfügbaren Informationen ab.

Die Quellen werden in folgender Reihenfolge ausgewertet:

1. die zuletzt erkannte, höchstens 30 Minuten alte QRG der Gegenstation,
2. eine eindeutige vollständige QRG im Namensfeld eines aktiven Chat-Eintrags,
3. eindeutige Bandangaben im Namensfeld,
4. 432 MHz, wenn dieselbe Station gleichzeitig in der VHF/UHF- und Microwave-Kategorie aktiv ist und 432 MHz für die eigene Station aktiviert wurde,
5. das niedrigste für die eigene Station aktivierte Band, das zur unterstützten Chat-Kategorie passt.

Aktive Chat-Varianten desselben Basisrufzeichens werden gemeinsam ausgewertet. Die Einträge `CALLSIGN`, `CALLSIGN-2` und `CALLSIGN-432` können dadurch gemeinsam zur Bandherleitung beitragen, bleiben für die Nachrichtenverarbeitung aber getrennte Chat-Teilnehmer.

Nur Bänder, die unter **My station uses …** aktiviert wurden, kommen für die automatische Auswahl infrage. Ein manuell gesetztes NOT-QRV-Kennzeichen schließt das betreffende Band aus und hat Vorrang vor automatisch erkannten QRG- oder Namensinformationen.

Unterstützt werden die Chat-Kategorien für 50/70 MHz, VHF/UHF, Microwave und EME/JT65. Andere ON4KST-Kategorien werden für die AirScout-Bandherleitung ignoriert. Kann keine ausreichend belastbare Frequenz bestimmt werden, sendet KST4Contest für diese Station keine Anfrage. Ein beliebiger Rückfall auf 144 MHz würde zwar ein syntaktisch vollständiges Paket erzeugen, aber nicht zwangsläufig eine sinnvolle Berechnung.

Die automatische AirScout-Auswahl verwendet dieselbe Herleitung wie die interne Streckenanalyse. Damit bewerten beide Funktionen den Stationspfad auf derselben fachlichen Grundlage.

### Festes AirScout-Band

Wird **Auto per station** deaktiviert, verwendet KST4Contest den unter **Forced AirScout band value** eingetragenen Wert für alle Stationen.

Der Wert wird in der von der AirScout-UDP-Schnittstelle verwendeten Einheit eingetragen:

| Band | AirScout-Wert |
|---|---:|
| 50 MHz | `500000` |
| 70 MHz | `700000` |
| 144 MHz | `1440000` |
| 432 MHz | `4320000` |
| 1296 MHz | `12960000` |
| 2320 MHz | `23200000` |
| 3400 MHz | `34000000` |
| 5760 MHz | `57600000` |
| 10368 MHz | `103680000` |
| 24048 MHz | `240480000` |

Im festen Modus wird weder die zuletzt erkannte QRG noch das im Namen genannte Band der Gegenstation berücksichtigt. Diese Einstellung ist deshalb hauptsächlich für einen eindeutig auf ein Band begrenzten Stationsbetrieb oder zur Fehlersuche sinnvoll.

### Server- und Client-Identifier

Die Identifier gehören zum AirScout-Protokoll und sind keine DNS-Namen oder IP-Adressen.

Ausgehende Anfragen enthalten zunächst den Client- und anschließend den Server-Identifier:

```text
"KST" "AS"
```

AirScout antwortet in umgekehrter Reihenfolge:

```text
"AS" "KST"
```

KST4Contest verarbeitet eine Antwort nur, wenn beide Identifier exakt mit der aktuellen Konfiguration übereinstimmen. Der Vergleich unterscheidet zwischen Groß- und Kleinschreibung.

Die Identifier dürfen nicht leer sein und keine Anführungszeichen oder Zeilenumbrüche enthalten.

Werden mehrere KST4Contest-Instanzen im selben Netz betrieben, sollte jede einen eigenen Client-Identifier erhalten, beispielsweise:

```text
KST-144
KST-432
```

Bei mehreren AirScout-Instanzen müssen zusätzlich unterschiedliche Server-Identifier verwendet werden. Dadurch wird verhindert, dass die Antwort für einen Arbeitsplatz von einer anderen KST4Contest-Instanz verarbeitet wird.

### Welche Stationen werden angefragt?

KST4Contest startet die erste periodische AirScout-Abfrage ungefähr zehn Sekunden nach dem Aufbau der Chat-Verbindung. Weitere Abfragen folgen im Abstand von 60 Sekunden.

Eine aktive Station wird nur berücksichtigt, wenn:

- ein verwendbares Rufzeichen vorhanden ist,
- ein Locator vorhanden ist,
- die Entfernung berechnet werden konnte,
- die Entfernung kleiner als das konfigurierte **Maximum-QRB** ist und
- ein verwendbares Band bestimmt werden konnte.

Mehrere aktive Chat-Einträge desselben Basisrufzeichens erzeugen nicht für jeden Suffix eine eigene identische Pfadberechnung. Die zurückgegebenen AirScout-Informationen werden anschließend wieder den passenden aktiven Chat-Varianten zugeordnet.

Die Auswahl begrenzt nicht nur den Netzwerkverkehr. Sie verhindert außerdem, dass AirScout dauerhaft Pfade berechnet, die außerhalb des für die eigene Station vorgesehenen Arbeitsbereichs liegen.

### Übernahme geänderter Einstellungen

Folgende Änderungen werden für neue Pakete verwendet, sobald sie übernommen sind:

- Aktivierung oder Deaktivierung der AirScout-Integration,
- Server-Identifier,
- Client-Identifier,
- automatische oder feste Bandauswahl und
- fester Bandwert.

Nach einer Änderung des UDP-Ports muss die Chat-Verbindung getrennt und neu aufgebaut oder KST4Contest neu gestartet werden. Der bereits laufende UDP-Empfänger bleibt sonst weiterhin an den vorherigen Port gebunden.

Zum dauerhaften Speichern anschließend **Save Settings** verwenden.

Die Einrichtung der AirScout-Seite, die Anzeige der Flugzeuge und die Bedeutung der AP-Daten sind unter [AirScout-Integration](de-AirScout-Integration) beschrieben.

---

## Notification Settings (Benachrichtigungen)

![Benachrichtigungen, DX-Cluster-Ausgabe und QSO-Monitoring](client_settings_window_notification.png)

Im Reiter **Notification** werden nicht nur akustische Hinweise konfiguriert. Hier befinden sich auch die Einstellungen für den lokalen DX-Cluster-Server, den Band-Upgrade-Hinweis und das QSO-Monitoring.

### Akustische Hinweise

Die drei Audiofunktionen arbeiten unabhängig voneinander:

- **Play notification sounds …** aktiviert kurze Hinweistöne für neue Privatnachrichten, erkannte Richtungsgelegenheiten, Sked-Erinnerungen und Band-Upgrade-Hinweise.
- **Spell the sender's callsign in CW …** gibt das Rufzeichen des Absenders einer neuen Privatnachricht als CW-Signal aus.
- **Speak the sender's callsign phonetically …** spricht das Rufzeichen des Absenders phonetisch aus.

CW- und Sprachausgabe können gleichzeitig aktiviert werden. Das ist technisch möglich, im Contest aber nicht zwingend hilfreich. In der Praxis sollte nur die Ausgabe eingeschaltet werden, die im eigenen Stationsbetrieb tatsächlich wahrgenommen werden kann, ohne den Operator dauerhaft zu beschäftigen.

Die PM-bezogenen Audioausgaben reagieren nur auf Nachrichten, die tatsächlich an das eigene Login-Rufzeichen gerichtet sind. Öffentliche Nachrichten, die durch PM-Catching zusätzlich in der PM-Tabelle erscheinen, und dort eingeblendete Monitoring-Nachrichten bleiben akustisch still.

### Fallback-Band für relative QRG-Erkennung

Das Dropdown **Fallback band for relative QRG detection** legt fest, welches Band KST4Contest verwendet, wenn eine relative QRG keinem aktuellen Stationskontext zugeordnet werden kann.

Zur Auswahl stehen ausschließlich die vom Frequenzparser unterstützten Bandpräfixe:

```text
50 MHz
70 MHz
144 MHz
432 MHz
1296 MHz
2320 MHz
3400 MHz
5760 MHz
10368 MHz (10G)
24048 MHz (24G)
```

Das Dropdown ist kein Filter und keine Vorgabe für vollständig angegebene Frequenzen. `432.088` wird unabhängig von der Auswahl als Frequenz im 432-MHz-Band erkannt. Benötigt wird das Fallback bei relativen Angaben wie `.205`, `,205` oder `qrg 205`.

Bevor KST4Contest auf das Fallback zurückgreift, prüft es den Bandkontext des Absenders. Wurde für dieselbe Station innerhalb der letzten 30 Minuten bereits eine passende vollständige Frequenz erkannt, hat dieses Band Vorrang. Ein Fallback von `144 MHz` macht aus `.100` daher `432.100 MHz`, wenn die Station kurz zuvor beispielsweise `432.088` genannt hat.

Die Einstellung befindet sich im Notification-Bereich, wirkt aber auf die gesamte QRG-Erkennung. Damit beeinflusst sie nicht nur mögliche DX-Cluster-Spots, sondern auch die QRG-Spalte, erkannte aktive Bänder, Priorisierung, Band-Upgrade-Hinweise und Funktionen, die eine bekannte Stationsfrequenz verwenden.

Mehr zur Erkennungslogik und zu absichtlich ignorierten Zahlen: [QRG-Erkennung](de-Funktionen#qrg-erkennung).

### Local DX Cluster output

KST4Contest kann erkannte Richtungsgelegenheiten als DX-Cluster-Spots an ein Logprogramm weitergeben. Eine im Chat erkannte Frequenz erscheint dadurch direkt in der Bandmap des Logprogramms und muss nicht erst von Hand übertragen werden.

Die Checkbox **Enable the local DX Cluster server …** startet beziehungsweise beendet den lokalen TCP-Server. Bei einer laufenden Chat-Verbindung wird die Änderung wirksam, sobald sie übernommen ist.

Folgende Einstellungen und Schaltflächen gehören zur lokalen DX-Cluster-Ausgabe:

- **TCP port**: Port, auf dem KST4Contest Verbindungen von DX-Cluster-Clients annimmt. Der Standardwert ist `8000`. Wird der Port während einer laufenden Verbindung geändert, startet KST4Contest den Server auf dem neuen Port neu. Der Logger muss sich anschließend ebenfalls mit dem neuen Port verbinden.
- **Fallback band for relative QRG detection**: Das oben beschriebene globale Fallback-Band. Der Testspot verwendet `.300` dieses Bandes. Reale Spots verwenden dagegen die für den jeweiligen Absender erkannte QRG.
- **Spotter callsign**: Rufzeichen, das im erzeugten DX-Cluster-Spot als Spotter erscheint. Hier sollte ein anderes Rufzeichen als das im Contest verwendete Stationsrufzeichen eingetragen werden. Einige Logprogramme filtern Spots des eigenen Rufzeichens oder behandeln sie anders als fremde Spots.
- **Send test spot**: Sendet den folgenden Testspot an alle aktuell verbundenen DX-Cluster-Clients:

```text
Spotted callsign: DO5AMF
Comment: DXC test: You donated $100!
Frequency: .300 des ausgewählten Fallback-Bandes
```

Bei einem Fallback-Band von `144 MHz` wird daraus beispielsweise eine Frequenz von ungefähr `144.300 MHz`.

Alle Spots verwenden eine feste, DXSpider-kompatible 75-Zeichen-Nutzzeile mit einem 30 Zeichen breiten Kommentarfeld. Längere Kommentare werden an dieser Protokollgrenze kontrolliert gekürzt; das DX-Rufzeichen wird dagegen nicht abgeschnitten. Rufzeichen mit mehr als zwölf Zeichen führen dazu, dass der betreffende Spot verworfen und protokolliert wird.

Der Kommentar des Testspots ist ein bewusst beibehaltenes Easteregg. Er hat keine technische Bedeutung und löst – trotz seiner erfreulich konkreten Formulierung – keine Zahlung aus. Entscheidend ist, dass der Spot im verbundenen Logprogramm erscheint.

Der Test funktioniert nur, wenn

1. KST4Contest mit dem ON4KST-Chat verbunden ist,
2. der lokale DX-Cluster-Server aktiviert ist und
3. mindestens ein DX-Cluster-Client mit KST4Contest verbunden ist.

KST4Contest erzeugt nicht bei jeder im Chat gefundenen Frequenz automatisch einen Spot. Ein realer Spot entsteht nur dann, wenn eine gerichtete Nachricht zwischen zwei Stationen auf eine für die eigene Station interessante Antennenrichtung schließen lässt und für den Absender eine nutzbare Frequenz bekannt ist.

Die vollständige Herleitung und die Einrichtung des Logprogramms sind im Kapitel [Integrierter DX-Cluster-Server](de-DX-Cluster-Server) beschrieben.

### Band-Upgrade-Hinweis nach einem Logeintrag

Nach einem über UCXLog oder Win-Test empfangenen Logeintrag kann KST4Contest prüfen, ob die gerade gearbeitete Station noch ein weiteres gemeinsames, aber bisher nicht gearbeitetes Band anbietet.

Die Prüfung verwendet dieselbe Bandherleitung wie die `a`- und `B+`-Anzeige:

1. die in den Stationseinstellungen aktivierten eigenen Bänder,
2. höchstens 30 Minuten alte QRG-Erkennungen der Gegenstation,
3. eindeutige Bandangaben im Namensfeld ihrer aktiven Chat-Einträge,
4. die pro Band gespeicherten Worked-Markierungen und
5. manuell gesetzte NOT-QRV-Tags.

Aktive Chat-Varianten desselben normalisierten Rufzeichens werden gemeinsam ausgewertet. NOT-QRV hat Vorrang vor einer automatisch erkannten QRG oder Bandangabe.

Bleibt mindestens ein gemeinsames, noch nicht gearbeitetes Band übrig, erscheint im Hauptfenster für ungefähr zwölf Sekunden ein blinkender **BAND+**-Hinweis mit Rufzeichen und den noch offenen Bändern. Der Tooltip zeigt die vollständige Herleitung. Ist die allgemeine Soundausgabe aktiviert, wird zusätzlich ein kurzer Hinweiston abgespielt.

Die beiden Optionen haben unterschiedliche Aufgaben:

- **Blink + sound …** aktiviert den Hinweis nach einem passenden Logeintrag.
- **Priority boost …** erhöht zusätzlich den Score von Stationen, die bereits auf mindestens einem Band gearbeitet wurden, aber noch ein weiteres gemeinsames und nicht gearbeitetes Band anbieten.

Der Priority Boost ist nur ein Faktor innerhalb der gesamten Berechnung. Entfernung, Antennenrichtung, aktuelle Aktivität, AirScout-Daten, Skeds und negative Hinweise können den endgültigen Listenplatz weiterhin verändern. Die aktivierte Option garantiert deshalb weder einen bestimmten Score noch einen bestimmten Platz in der Prioritätsliste.

Die übrigen Score-Gewichte besitzen derzeit keine eigenen Bedienelemente. Mehrere vorhandene Einstellungen liefern jedoch Eingangsdaten für die Berechnung, insbesondere die [aktivierten Bänder](#aktivierte-bänder), der [Antennen-Öffnungswinkel](#antennen-öffnungswinkel-antenna-beamwidth), der [Standard-Maximum-QRB](#standard-maximum-qrb) und die [AirScout-Einstellungen](#airscout-einstellungen).

Die vollständige Herleitung ist unter [Prioritätsscore und Prioritätsliste](de-Funktionen#prioritätsscore-und-prioritätsliste-ab-v140) beschrieben.

Der Hinweis setzt eine Log-Synchronisation mit Bandinformation voraus. Der einfache dateibasierte Callsign-Interpreter erkennt lediglich Rufzeichen und liefert deshalb keine sichere Information über das Band des gerade geloggten QSOs.

Weitere Hintergründe: [Band-Upgrade-Hinweis nach einem Logeintrag](de-Funktionen#band-upgrade-hinweis-nach-einem-logeintrag).

### Sniffer-Einstellungen (ab v1.31)

Das QSO-Monitoring ist für Stationen gedacht, deren Kommunikation man gezielt verfolgen möchte. Das kann beispielsweise eine seltene Station, eine DXpedition oder eine andere Station des eigenen Contest-Teams sein, deren Sked-Absprachen im allgemeinen Chatverkehr nicht untergehen sollen.

Die Rufzeichen werden im Reiter **Notification** unter **QSO monitoring** verwaltet.

Für jedes eingetragene Basisrufzeichen zeigt KST4Contest Nachrichten zusätzlich in der PM-Tabelle an, wenn eine Variante dieses Rufzeichens entweder Absender oder Empfänger der Nachricht ist. Berücksichtigt werden beide verbundenen Chat-Kategorien.

Die Liste arbeitet bewusst mit dem normalisierten Basisrufzeichen. Folgende Eingaben erzeugen deshalb denselben Monitoring-Eintrag:

```text
DN9APW
DN9APW-2
DN9APW-70
DN9APW-144
```

In allen Fällen speichert und zeigt KST4Contest den Eintrag als:

```text
DN9APW
```

Damit müssen die verschiedenen KST-Suffixe einer Station nicht einzeln eingetragen werden. Wird später eine Nachricht von `DN9APW-2` gesendet oder an `DN9APW-70` adressiert, wird sie durch denselben Eintrag erfasst.

Diese Zusammenführung gilt ausschließlich für das QSO-Monitoring. Die aktiven ChatMember-Objekte, vollständigen Nachrichtenempfänger und Chat-Kategorien bleiben getrennt. Eine an `DN9APW-70` gerichtete Nachricht wird deshalb nicht an `DN9APW-2` umgeleitet.

Überwachte Nachrichten werden in der PM-Tabelle mit den vollständigen sichtbaren Rufzeichen von Absender und Empfänger gekennzeichnet:

```text
Sniffed: (DN9APW-2 > DL0ABC-70) Nachrichtentext
```

Die ursprüngliche Nachricht bleibt gleichzeitig in ihrer normalen Tabelle erhalten. Das Monitoring verändert weder den Nachrichteninhalt noch dessen Routing.

Erfasst werden Nachrichten, bei denen das überwachte Rufzeichen tatsächlich Absender oder Empfänger ist. Eine bloße Erwähnung des Rufzeichens im Nachrichtentext reicht nicht aus. Öffentliche Nachrichten einer überwachten Station werden ebenfalls angezeigt; als Empfänger erscheint dabei `ALL`.

Ist eine Nachricht bereits direkt an das eigene Rufzeichen gerichtet, erscheint sie als normale Privatnachricht und erhält keine zusätzliche `Sniffed:`-Kennzeichnung.

Rufzeichen werden folgendermaßen verwaltet:

1. Mit **Add monitored callsign** ein neues Rufzeichen hinzufügen.
2. Ein vorhandenes Rufzeichen per Doppelklick bearbeiten und die Änderung mit `Enter` übernehmen.
3. Zum Entfernen den Inhalt einer Tabellenzelle löschen und mit `Enter` bestätigen.

Die Eingabe darf ein sichtbares KST-Suffix oder portable Bestandteile enthalten. KST4Contest normalisiert sie vor dem Speichern auf das Basisrufzeichen. Mehrere Varianten desselben Basisrufzeichens gelten deshalb als Duplikat.

Die Änderung der Liste wirkt, sobald sie übernommen ist. Zum dauerhaften Speichern anschließend **Save Settings** verwenden. Die Basisrufzeichen werden in der `preferences.xml` gespeichert und beim nächsten Programmstart wiederhergestellt.

> Die Zusammenführung der KST-Suffixe über das Basisrufzeichen ist ab v1.42 enthalten.

Weitere Hintergründe und die Abgrenzung zum Nachrichtenrouting: [QSO-Monitoring](de-Funktionen#qso-monitoring-ab-v131).
---

## Shortcut Settings (Schnellzugriff-Schaltflächen)

![Konfiguration der Shortcut-Schaltflächen und Text-Snippets](client_settings_window_shortcuts.png)

Jeder Eintrag im oberen Bereich des Reiters **Shortcuts** erzeugt eine Schaltfläche oberhalb des Nachrichteneingabefeldes im Hauptfenster. Ein Klick hängt den konfigurierten Text an den bereits vorhandenen Inhalt des Sendfeldes an.

Enthält der Shortcut eine [Variable](de-Makros-und-Variablen#variablen), wird sie beim Einfügen durch ihren aktuellen Wert ersetzt. Ein Shortcut wie

```text
pse call me at MYQRGSHORT
```

kann dadurch beispielsweise folgenden Text einfügen:

```text
pse call me at 144.388
```

Die exakten Einträge `MYQRG` und `SECONDQRG` werden zusätzlich als QRG-Schaltflächen hervorgehoben. Sie fügen die aktuelle QRG der ersten beziehungsweise zweiten Chat-Kategorie ein.

Auch der Shortcut `/SETNAME MYQRG` wird hervorgehoben. Beim Anklicken löst KST4Contest `MYQRG` auf und übernimmt den vollständigen Serverbefehl in das Sendfeld. Der Befehl wird nicht automatisch versendet und kann vor dem Senden mit `Enter` oder **TX** noch geprüft werden.


Die Reihenfolge der Tabelle entspricht der Reihenfolge der Schaltflächen im Hauptfenster. Die Einträge werden folgendermaßen verwaltet:

1. Mit **Add shortcut** wird am Anfang der Liste ein neuer Eintrag angelegt und sofort zur Bearbeitung geöffnet.
2. Ein vorhandener Eintrag kann per Doppelklick bearbeitet werden. `Enter` übernimmt die Änderung.
3. Wird der Inhalt vollständig gelöscht und anschließend mit `Enter` bestätigt, entfernt KST4Contest den Eintrag.
4. Mit **Move selected up** und **Move selected down** wird der markierte Eintrag innerhalb der Liste verschoben.

Übernommene Änderungen werden sofort im Hauptfenster sichtbar. Damit sie auch nach dem nächsten Programmstart erhalten bleiben, anschließend **Save Settings** verwenden.

---

## Snippet Settings (Text-Snippets)

Snippets sind längere Textbausteine, die vor allem für Nachrichten an eine ausgewählte Station vorgesehen sind. Sie können über folgende Wege aufgerufen werden:

- per Rechtsklick auf eine Station in der Benutzerliste,
- per Rechtsklick auf eine Nachricht in der öffentlichen Chat-Tabelle,
- per Rechtsklick auf eine Nachricht in der PM-Tabelle oder
- mit `Ctrl+1` bis `Ctrl+0` für die ersten zehn Einträge der Snippet-Liste.

Bei den Tastenkombinationen entspricht die Zuordnung der Tabellenreihenfolge:

| Tastenkombination | Snippet |
|---|---:|
| `Ctrl+1` | erster Eintrag |
| `Ctrl+2` | zweiter Eintrag |
| … | … |
| `Ctrl+9` | neunter Eintrag |
| `Ctrl+0` | zehnter Eintrag |

Ein über das Kontextmenü ausgewähltes Snippet wird an den bereits vorbereiteten Nachrichtentext angehängt. Die Auswahl einer Station oder Nachricht hat das Sendfeld zuvor normalerweise bereits mit dem passenden `/cq`-Empfänger vorbereitet.

Eine Tastenkombination verhält sich etwas anders: Sie ersetzt den bisherigen Inhalt des Sendfeldes durch eine vollständig adressierte Privatnachricht:

```text
/cq RUFZEICHEN Snippet-Text
```

Dabei wird das vollständige sichtbare Rufzeichen einschließlich eines vorhandenen Suffixes verwendet. Für `9A0BB-70` entsteht daher beispielsweise:

```text
/cq 9A0BB-70 pse ur qrg?
```

Die Chat-Kategorie der ausgewählten Station bleibt für den späteren Versand erhalten. Ist keine Station ausgewählt oder ist für die gedrückte Tastenkombination kein Snippet vorhanden, wird nichts eingefügt.

Variablen werden beim Einfügen des Snippets aufgelöst. Stationsbezogene Variablen wie `QRZNAME`, `FIRSTAP` oder `SECONDAP` verwenden die aktuell ausgewählte Station. Der vorbereitete Text wird nicht automatisch gesendet und kann deshalb noch geprüft oder geändert werden. `Enter` oder **TX** sendet die Nachricht; `Esc` leert das Sendfeld.

Die Snippet-Liste wird genauso bearbeitet wie die Shortcut-Liste:

1. **Add new snippet** legt am Anfang der Liste einen neuen Eintrag an.
2. Ein Doppelklick öffnet einen vorhandenen Eintrag zur Bearbeitung.
3. `Enter` übernimmt die Änderung.
4. Ein leer bestätigter Eintrag wird entfernt.
5. **Move selected up** und **Move selected down** ändern die Reihenfolge und damit auch die Zuordnung zu `Ctrl+1` bis `Ctrl+0`.

Die Kontextmenüs und Tastenkombinationen werden aktualisiert, sobald die Änderung übernommen ist. Für die dauerhafte Speicherung anschließend **Save Settings** verwenden.

Eine vollständige Übersicht der verfügbaren Platzhalter und ihrer Grenzen steht unter [Makros und Variablen](de-Makros-und-Variablen).

---

## Beacon Settings (Automatischer Beacon)

![Beacon-Einstellungen](client_settings_window_beacon.png)

Ein Beacon sendet in regelmäßigen Abständen eine öffentliche CQ-Nachricht. Er ist für Betriebssituationen gedacht, in denen die eigene Station über längere Zeit auf einer festen Frequenz ruft. Andere Stationen erhalten dadurch eine aktuelle QRG-Information, ohne dass der Operator denselben Text immer wieder von Hand in den Chat schreiben muss.

KST4Contest verwendet einen gemeinsamen Timer für beide Chat-Kategorien. Aktivierung und Nachrichtentext werden trotzdem getrennt konfiguriert:

- **Enable CQ beacon** aktiviert den Beacon der betreffenden Kategorie.
- **Beacon message** enthält den öffentlichen Nachrichtentext dieser Kategorie.
- **Shared beacon interval** legt das gemeinsame Intervall für beide Kategorien fest.

Sind beide Beacons aktiviert, werden sie beim selben Timer-Lauf nacheinander in ihren jeweiligen Kategorien gesendet. Der zweite Beacon wird nur berücksichtigt, wenn die zweite Kategorie in derselben ON4KST-Sitzung aktiviert und vollständig synchronisiert ist.

### Intervall und Timer-Verhalten

Das Intervall wird in ganzen Minuten angegeben. Der kleinste zulässige Wert ist eine Minute.

Nach dem Aufbau der Chat-Verbindung prüft KST4Contest die Beacons erstmals nach ungefähr zehn Sekunden. Anschließend gilt das eingestellte Intervall.

Wird das Intervall während einer laufenden Verbindung geändert, beginnt der Countdown mit dem neuen Wert erneut. Die Änderung selbst löst keine sofortige Beacon-Nachricht aus.

Beide Kategorien verwenden denselben Timer. Unterschiedliche Intervalle für den ersten und zweiten Chat können deshalb nicht eingestellt werden.

### Nachrichtentext und Variablen

Ein Beacon darf die [globalen Variablen](de-Makros-und-Variablen#variablen-im-beacon) verwenden, die sich ausschließlich auf die eigene Station beziehen:

- `MYQRG`
- `MYQRGSHORT`
- `SECONDQRG`
- `MYLOCATOR`
- `MYLOCATORSHORT`
- `MYCALL`
- `MYQTF`

Eine mögliche Nachricht für die erste Chat-Kategorie ist:

```text
calling cq at MYQRGSHORT, ant MYQTF deg, loc MYLOCATOR
```

Für den zweiten Chat muss `SECONDQRG` verwendet werden, wenn dessen Frequenz von der ersten Kategorie abweicht:

```text
calling cq at SECONDQRG, ant MYQTF deg, loc MYLOCATOR
```

Die Variablen werden bei jedem Timer-Lauf neu aufgelöst. Ändert die Logsoftware zwischenzeitlich die in `MYQRG` gespeicherte Frequenz, verwendet bereits der nächste Beacon den aktualisierten Wert. Das Template selbst muss dafür nicht geändert werden.

`MYQRG` und `MYQRGSHORT` beziehen sich immer auf die erste Chat-Kategorie. Die Auswahl oder Aktivierung des zweiten Chats ändert diese Zuordnung nicht.

Stationsbezogene Variablen wie `QRZNAME`, `FIRSTAP` oder `SECONDAP` benötigen eine ausgewählte Gegenstation. Da ein öffentlicher Beacon keine bestimmte Station adressiert, werden diese Variablen im Beacon nicht aufgelöst.

### Prüfung des Nachrichtentextes

KST4Contest prüft sowohl das eingetragene Template als auch den nach der Variablenauflösung tatsächlich zu sendenden Text.

Für Beacon-Nachrichten gelten folgende Bedingungen:

- Der endgültige Nachrichtentext darf nicht leer sein.
- Er darf höchstens 120 Zeichen enthalten.
- Das Protokoll-Trennzeichen `|` ist nicht zulässig.
- Zeilenumbrüche sind nicht zulässig.

Eine ungültige Eingabe wird nicht als neue Beacon-Konfiguration übernommen. Wird ein Template erst durch eine spätere Variablenauflösung ungültig, beispielsweise weil der aufgelöste Text länger als 120 Zeichen ist, wird dieser Beacon-Lauf ausgelassen.

Ein Template, das ausschließlich aus einer momentan noch leeren globalen Variable besteht, kann gespeichert werden. Das ist beispielsweise beim Start möglich, bevor die erste QRG vom Logprogramm empfangen wurde. Solange die Variable keinen verwendbaren Inhalt liefert, sendet KST4Contest jedoch keine leere Nachricht.

### Wann sollte der Beacon ausgeschaltet werden?

Der Beacon ist nur dann hilfreich, wenn seine QRG-Angabe zum tatsächlichen Betrieb passt. Bleibt er beim Absuchen oder häufigen Wechseln von Frequenzen aktiviert, können andere Stationen auf einer inzwischen falschen Frequenz nach der eigenen Station suchen.

Im Klartext: Solange auf einer festen QRG CQ gerufen wird, spart der Beacon Arbeit. Beim „Schleichen“ über das Band sollte er ausgeschaltet werden.

Änderungen wirken während der laufenden Verbindung. Damit Aktivierung, Texte und Intervall auch nach dem nächsten Programmstart erhalten bleiben, anschließend **Save Settings** verwenden.

---

## Messagehandling Settings (ab v1.25)

![Automatische Antworten](client_settings_window_messagehandling.png)

Die wichtigste Anwendung der allgemeinen automatischen Antwort betrifft Stationen, die zwar im ON4KST-Chat eingeloggt sind, den laufenden Contest aber nicht mitfunken. Gerade während größerer Contests werden Sked-Anfragen teilweise unkoordiniert und in großer Zahl an eingeloggte Stationen verteilt, ohne vorher zu prüfen, ob sie überhaupt teilnehmen. Die Empfänger müssten sonst immer wieder dieselbe Absage schreiben.

KST4Contest kann darauf mit einem vorher festgelegten Text reagieren. Davon getrennt steht eine gezielte QRG-Auskunft zur Verfügung. Beide Funktionen können unabhängig voneinander aktiviert werden.

### Allgemeine automatische Antwort

**Enable automatic reply to all private messages** beantwortet eingehende Privatnachrichten mit dem Text im Feld rechts daneben. Es gibt einen gemeinsamen Text für beide Chat-Kategorien. Die beim Eingeben verwendete Groß- und Kleinschreibung bleibt erhalten.

Eine zweckmäßige Nachricht ist beispielsweise:

```text
Sri, I am not taking part in this contest. No skeds.
```

Das Präfix `[KST4C Automsg]` muss nicht in das Eingabefeld geschrieben werden. KST4Contest ergänzt es automatisch.

Die beim Empfänger sichtbare Nachricht lautet daher beispielsweise:

```text
[KST4C Automsg] Sri, I am not taking part in this contest. No skeds.
```

Die eingegangene Privatnachricht bleibt sichtbar. Die Funktion blockiert oder verwirft keine Anfrage, sondern erspart lediglich die wiederholte manuelle Antwort.

Die Antwort wird an das vollständige Rufzeichen des Absenders einschließlich eines vorhandenen Suffixes und in derselben Chat-Kategorie gesendet, in der die Privatnachricht eingegangen ist. Das ist bei einem parallelen Login in zwei Kategorien entscheidend: Eine Nachricht aus dem Microwave-Chat darf nicht versehentlich im VHF/UHF-Chat beantwortet werden.

Ein leerer oder ausschließlich aus Leerzeichen bestehender Antworttext erzeugt keine automatische Nachricht. Enthält der Text ein Protokoll-Trennzeichen wie `|` oder einen Zeilenumbruch, wird die Antwort ebenfalls verworfen.

### Automatische QRG-Antwort

**Enable automatic QRG replies** reagiert auf typische QRG-Anfragen. Die Erkennung unterscheidet nicht zwischen Groß- und Kleinschreibung und sucht nach folgenden Textbestandteilen:

```text
ur qrg?
your qrg?
qrg?
freq?
pse qrg
```

Die Antwort enthält ausschließlich die QRG der Kategorie, in der die Anfrage eingegangen ist:

| Eingegangene Privatnachricht | Verwendete QRG |
|---|---|
| Hauptkategorie | aktuelle QRG der Hauptkategorie |
| zweite Chat-Kategorie | aktuelle QRG der zweiten Kategorie |

Eine mögliche Antwort lautet:

```text
[KST4C Automsg] QRG is: 144.300.00
```

Die Werte stammen aus denselben QRG-Feldern, die auch von `MYQRG` und `SECONDQRG` verwendet werden. Die Haupt-QRG kann manuell eingetragen oder durch die [TRX-Synchronisation](#trx-sync-einstellungen) aktualisiert werden. Für die zweite Kategorie wird der dort konfigurierte beziehungsweise manuell eingetragene Wert verwendet; mit SO2R schreibt Funkgerät 2 dieses Feld (`SECONDQRG`).

Ist für die betreffende Kategorie keine QRG vorhanden, sendet KST4Contest keine unvollständige Antwort. Eine Nachricht wie `QRG is:` ohne Frequenz würde zwar auf die Frage reagieren, dem Anfragenden aber keine Information liefern. Sie wird deshalb bereits vor der Übergabe an die Sendequeue verworfen.

Sind die allgemeine und die QRG-bezogene Antwort gleichzeitig aktiviert, hat die QRG-Antwort Vorrang. Eine erkannte QRG-Anfrage erzeugt daher nicht zusätzlich den allgemeinen Antworttext. Fehlt die benötigte QRG, fällt KST4Contest auch nicht auf die allgemeine Antwort zurück.

### Debug- und Contest-History

**Enable debug mode to file** ist standardmäßig aktiviert und wirkt ohne Neustart. Neue Diagnosedateien liegen im Unterverzeichnis `.praktiKST/debug` des Benutzerprofils:

- `kst4contest-errors.log` enthält bei aktiviertem Debug-Modus zusätzliche `INFO`- und `FINE`-Diagnosen. Warnungen, Fehler und Exceptions werden unabhängig vom Schalter immer geschrieben.
- `Messagehistory.raw` zeichnet den vollständigen ON4KST-Verkehr der aktuellen Programmsitzung auf. Dazu gehören RX und TX, also neben Chat- und Privatnachrichten auch Benutzer-, Status-, Control- und Keepalive-Frames.

Unter Windows liegt die History beispielsweise unter `C:\Users\<Benutzername>\.praktiKST\debug\Messagehistory.raw`, unter Linux und macOS unter `~/.praktiKST/debug/Messagehistory.raw`. KST4Contest legt das Verzeichnis bei Bedarf an. Vorhandene Diagnosedateien direkt unter `.praktiKST` werden weder verschoben noch gelöscht.

Die Datei `kst4contest-errors.log` rotiert bei 20 MiB. Einschließlich der aktiven Datei bleiben höchstens fünf Generationen erhalten.

Jede Zeile enthält genau ein Ereignis:

```text
UTC<TAB>RX|TX<TAB>RAW_FRAME
```

Der UTC-Zeitstempel enthält Millisekunden. Das dritte Feld bleibt bis auf die Entfernung des TCP-Zeilenabschlusses unverändert. Loginframes bleiben für eine spätere Analyse erkennbar, das Passwort wird jedoch durch `<REDACTED>` ersetzt.

Beim ersten Aktivieren in einer Programmsitzung wird eine vorhandene, nichtleere `Messagehistory.raw` als `Messagehistory-YYYYMMDD-HHMMSSZ.raw` archiviert. Reconnects und erneutes Aktivieren innerhalb derselben Programmsitzung erzeugen kein weiteres Archiv. Beim Abschalten endet die Aufzeichnung sofort; erneutes Aktivieren hängt an die aktuelle Datei an.

Die History ist als maschinenlesbare Grundlage für spätere Replay- und Simulationswerkzeuge gedacht. Sie enthält den vollständigen Contest-/Session-Verkehr und damit in der Regel personenbezogene Chat- und Stationsdaten. Vor einer Weitergabe muss die Datei entsprechend geprüft und behandelt werden.

Das vorhandene Bild `client_settings_window_messagehandling.png` zeigt die neue Checkbox noch nicht und sollte bei der nächsten Screenshot-Aktualisierung ersetzt werden.

### Schutz vor wiederholten Antworten

Jede automatisch erzeugte Nachricht trägt das feste Präfix:

```text
[KST4C Automsg]
```

Die allgemeine und die QRG-bezogene Antwort reagieren nicht auf Nachrichten, die dieses Präfix bereits enthalten. Dadurch beantworten sich zwei entsprechend arbeitende Clients nicht gegenseitig in einer Schleife.

Zusätzlich gilt eine gemeinsame Sperrzeit von zwei Minuten für beide Antwortarten. Die Sperre wird getrennt nach vollständigem Rufzeichen und Chat-Kategorie geführt.

Daraus folgt:

- `CALLSIGN-2` und `CALLSIGN-70` besitzen getrennte Sperrzeiten.
- Dasselbe vollständige Rufzeichen kann in einer anderen Chat-Kategorie unabhängig beantwortet werden.
- Eine allgemeine Antwort sperrt für zwei Minuten auch eine QRG-Antwort an dasselbe Rufzeichen in derselben Kategorie.
- Eine QRG-Antwort sperrt entsprechend auch die allgemeine Antwort.

Die Sperrzeit beginnt nur, wenn KST4Contest eine vollständige und lokal gültige Antwort in die Sendequeue übernimmt. Eine fehlende QRG, ein leerer allgemeiner Antworttext oder ein wegen ungültiger Zeichen verworfener Text startet keine Sperrzeit. Sobald die fehlende Information korrigiert wurde, kann daher unmittelbar eine gültige Antwort erzeugt werden.

> **Hinweis**: Der Antworttext sollte den tatsächlichen Status eindeutig benennen. Wer den Contest nur beobachtet und keine Skeds fahren möchte, sollte genau das mitteilen. Eine vage Nachricht erzeugt im Zweifel nur die nächste Rückfrage – und damit exakt die Arbeit, welche die Funktion vermeiden soll.

Änderungen wirken während der laufenden Verbindung. Damit Aktivierung und Text nach dem nächsten Programmstart erhalten bleiben, anschließend **Save Settings** verwenden.

Weitere Hintergründe: [Automatische Antworten auf Privatnachrichten](de-Funktionen#automatische-antworten-auf-privatnachrichten-ab-v125).

---

## Win-Test-Netzwerk-Listener (ab v1.31)

Der Win-Test-Netzwerk-Listener verarbeitet das native Win-Test-UDP-Protokoll. Er ist vom allgemeinen QSO-UDP-Listener auf Port `12060` unabhängig und übernimmt drei Aufgaben:

- QSOs einschließlich Band- und Locatorinformation auswerten,
- STATUS-Pakete für die eigene QRG verarbeiten und
- Skeds an das Win-Test-Netzwerk übergeben.

### Einstellungen unter Log sync

| Einstellung | Funktion |
|---|---|
| **Receive Win-Test network based UDP log messages** | Aktiviert den Win-Test-Netzwerk-Listener. Bei aktiviertem Listener wird nach **Create sked** auch die Sked-Übergabe versucht. |
| **UDP-Port for Win-Test listener** | Port des Win-Test-Netzwerks. Standard ist `9871`. Der Port wird auch für die Sked-Übergabe verwendet. |
| **KST station name in Win-Test network (src of SKED packets)** | Stationsname, unter dem KST4Contest die Sked-Pakete sendet. In einem Netzwerk mit mehreren Clients sollte ein eindeutiger Name verwendet werden. |
| **Win-Test network broadcast address** | Zieladresse für ausgehende Win-Test-Netzwerkpakete. Bei lokalem Netzwerkbetrieb muss hier eine vom Win-Test-Rechner erreichbare Broadcast-Adresse eingetragen sein. |

Die Broadcast-Adresse ist konfigurierbar, weil `255.255.255.255` nicht in jedem Stationsnetz und nicht über jede Netzwerkschnittstelle zuverlässig weitergeleitet wird. Bei mehreren Rechnern kann stattdessen die zum Stationsnetz gehörende gerichtete Broadcast-Adresse erforderlich sein.

### Einstellungen unter TRX sync

| Einstellung | Funktion |
|---|---|
| **Win-Test STATUS QRG Sync** | Übernimmt die aktuelle Frequenz aus Win-Test-STATUS-Paketen als eigene QRG. |
| **Use pass frequency from Win-Test STATUS** | Verwendet die übertragene Pass-Frequenz anstelle der normalen TRX-QRG. |
| **Win-Test station name filter** | Verarbeitet nur STATUS-Pakete der angegebenen Win-Test-Station. Ein leeres Feld akzeptiert alle Stationsnamen. |

Der Stationsfilter ist insbesondere bei mehreren Win-Test-Clients sinnvoll. Ohne Filter kann die zuletzt eingegangene STATUS-Meldung eines anderen Arbeitsplatzes die eigene QRG in KST4Contest überschreiben.

### Sked-Übergabe

Für die Sked-Übergabe gibt es keinen davon getrennten internen Sked-Modus. Ist der Listener aktiviert, versucht **Create sked** zusätzlich zur internen Anlage die Übertragung an Win-Test.

KST4Contest sendet nur dann ein `ADDSKED`-Paket, wenn eine QRG ermittelt wurde, die zum ausdrücklich ausgewählten Band gehört. Kann keine passende QRG gefunden werden, bleibt der interne Sked bestehen und die Win-Test-Übergabe wird ausgelassen.

Die Auswahl `SSB` oder `CW` erfolgt direkt im Further-Info-Bereich beim Anlegen des Skeds. Eine automatische Mode-Ableitung wird nicht verwendet.

Nach Änderungen **Save Settings** verwenden, damit Port, Stationsname, Broadcast-Adresse und TRX-Optionen beim nächsten Programmstart wiederhergestellt werden.

Datenbehandlung und QRG-Auswahl: [Log-Synchronisation – Win-Test](de-Log-Synchronisation#win-test)


## PSTRotator-Einstellungen (ab v1.31, vollständig konfigurierbar ab v1.40)

KST4Contest kann eine ausgewählte Antennenrichtung über die UDP-Schnittstelle von [PSTRotator](https://www.pstrotator.com/) einstellen und die von PSTRotator gemeldete aktuelle Position als eigene QTF übernehmen.

Die Einstellungen befinden sich im Reiter **Station**:

| Einstellung | Standardwert | Verwendung |
|---|---:|---|
| **Enable PSTRotator** | deaktiviert | Startet die UDP-Kommunikation mit PSTRotator |
| **QTF-Filterrichtung als eigene Antennenrichtung übernehmen** | aktiviert, wenn PSTRotator 1 deaktiviert ist | Übernimmt bewusste Richtungseingaben im QTF-Filter als aktuelle eigene Antennenrichtung |
| **PSTRotator host** | `127.0.0.1` | Hostname oder IP-Adresse des Rechners, auf dem PSTRotator läuft |
| **PSTRotator UDP port** | `12000` | UDP-Port, auf dem PSTRotator die Steuerbefehle empfängt |

Bei Betrieb auf demselben Rechner ist `127.0.0.1` normalerweise die eindeutigste Einstellung. Läuft PSTRotator auf einem anderen Rechner im Stationsnetz, muss dessen erreichbare IP-Adresse oder DNS-Name eingetragen werden.

Der Port darf zwischen `1` und `65534` liegen. Port `65535` ist nicht möglich, weil PSTRotator seine Positionsmeldungen auf dem jeweils folgenden Port sendet.

### PSTRotator vorbereiten

In PSTRotator muss unter **Communication → UDP Control Port** derselbe UDP-Port eingetragen werden wie in KST4Contest. Anschließend muss **UDP Control** in PSTRotator aktiviert werden.

Bei der Standardeinstellung ergibt sich folgendes Portpaar:

| Richtung | UDP-Port |
|---|---:|
| KST4Contest → PSTRotator | `12000` |
| PSTRotator → KST4Contest | `12001` |

KST4Contest bindet den Empfangsport automatisch. Er wird nicht separat konfiguriert.

Bei Betrieb auf zwei Rechnern müssen die lokale Firewall und das Stationsnetz UDP-Pakete in beiden Richtungen zulassen. Ist der Empfangsport bereits durch ein anderes Programm belegt, kann KST4Contest die Positionsmeldungen nicht empfangen.

Das vollständige UDP-Protokoll ist im [PSTRotatorAz User Manual](https://www.qsl.net/yo3dmu/ANT/PstRotatorAz%20User%20Manual.pdf) beschrieben.

### Übernahme der aktuellen QTF

Beim Programmstart wird die aktuelle eigene QTF mit der im aktiven Root- oder Operatorprofil gespeicherten Vorgabe **QTF** initialisiert. Sie dient nur als Startwert. Änderungen während des Betriebs verändern diese gespeicherte Vorgabe nicht.

KST4Contest fragt PSTRotator alle zwei Sekunden nach der aktuellen Azimutposition und dem Betriebsmodus. Die zurückgemeldete Azimutposition wird als `actualQTF` übernommen.

Bei aktivierter PSTRotator-Integration ist das QTF-Feld im Hauptfenster deshalb nicht manuell editierbar. Es zeigt die zuletzt von PSTRotator gemeldete Position.

Diese QTF wird unter anderem verwendet für:

- den Richtungsfilter,
- die Bewertung von Richtungsgelegenheiten,
- den Priority Score,
- die Darstellung des Antennensektors auf der Stationskarte,
- die AP- und Sked-Timeline und
- die Variable `MYQTF`.

Eine empfangene Rotatorposition ist damit nicht nur eine Anzeige. Sie verändert mehrere Funktionen, die auf der aktuellen Antennenrichtung beruhen.

Ohne PSTRotator 1 kann **QTF-Filterrichtung als eigene Antennenrichtung übernehmen** einen direkt in den QTF-Filter eingetragenen Winkel oder eine über dessen Richtungstasten gewählte Richtung in dasselbe `actualQTF` übernehmen. Das funktioniert auch, wenn **Show only QTF** ausgeschaltet ist. **Reset filters**, das Ausschalten des Filters und ein automatisch wiederhergestellter Filterzustand verändern die Antennenrichtung nicht. Die Übernahme erfolgt nur vom Filter zur Antennenrichtung und sendet keinen PSTRotator-Befehl.

Die Option steht im Abschnitt **Manuelle Antennenrichtung** und ist standardmäßig aktiviert. Das gilt nach dem Update auch für bestehende Root- und Operatorprofile. Wer die Kopplung nicht verwenden möchte, schaltet sie aus; diese Entscheidung wird im jeweiligen Profil gespeichert. Bei aktiviertem PSTRotator 1 ist die Checkbox nicht bedienbar und die Übernahme vorübergehend unwirksam. Die gespeicherte Auswahl bleibt dabei erhalten und gilt wieder, sobald PSTRotator 1 deaktiviert wird. PSTRotator 2 unter SO2R ist von dieser Regel unabhängig.

KST4Contest verwendet derzeit nur den Azimut. Eine Elevationssteuerung oder eine vollständige Azimut-/Elevationsnachführung ist nicht Bestandteil dieser Integration.

### Änderungen übernehmen

Aktivierung, Host und Port werden beim Start der Rotatorverbindung ausgewertet. Nach einer Änderung sollte die ON4KST-Verbindung getrennt und erneut aufgebaut oder KST4Contest neu gestartet werden.

Anschließend **Save Settings** verwenden, damit die Werte auch beim nächsten Programmstart wiederhergestellt werden.

---

## GUI Settings: Hinweise in den Bandspalten

Im Reiter **GUI** lassen sich zwei Zusatzinformationen der Bandspalten ein- oder ausblenden:

- **Show "o" in band columns …** zeigt ein `o`, wenn das vierstellige Großfeld auf dem betreffenden Band bereits gearbeitet wurde. Das Abschalten entfernt keine Daten aus der Datenbank; nur die zusätzliche Anzeige in den Bandspalten wird ausgeblendet. `wkdany` bleibt davon unberührt.
- **Show "a" in band columns …** unterscheidet ein vollständig neues Rufzeichen von einer Bandmöglichkeit mit einem bereits auf einem anderen Band gearbeiteten Rufzeichen. Ist die Option ausgeschaltet, werden beide Fälle als `B+` dargestellt. Die Bandherleitung selbst ändert sich dadurch nicht.

Übernommene Änderungen werden in der laufenden Benutzeroberfläche unmittelbar sichtbar. Damit sie nach dem nächsten Programmstart erhalten bleiben, anschließend **Save Settings** verwenden.

![GUI-Einstellungen für die Hinweise in den Bandspalten](client_settings_window_gui.png)

---

## Worked Station Database Settings (Gearbeitete-Stationen-Datenbank)

Die interne SQLite-Datenbank speichert die contestbezogenen Zustände unabhängig von der Datenbank des Logprogramms:

- globaler Worked-Status eines Rufzeichens,
- Worked-Status pro Band,
- manuell gesetzte NOT-QRV-Tags pro Band und
- gearbeitete vierstellige Großfelder pro Band.

Eine Ausnahme bilden Worked-Markierungen des Simplelogfile-Interpreters. Sie werden nur aus der ausgewählten Datei in den laufenden Zustand übernommen und nicht in SQLite persistiert. Die Datei ist die dauerhafte Quelle und besitzt keinen automatischen Contest-Reset. Ein Datenbank-Reset verändert oder leert sie nicht; enthaltene Rufzeichen werden bei der nächsten periodischen Auswertung erneut als gearbeitet markiert.

Als Schlüssel wird das normalisierte Rufzeichen ohne sichtbare Chat-Klammern oder Kategorieformatierung verwendet. Dadurch können aktive Varianten desselben Rufzeichens konsistent ausgewertet werden.

Worked- und NOT-QRV-Informationen laufen drei Tage nach ihrer letzten Änderung automatisch ab. Gespeicherte Großfelder laufen drei Tage nach dem zugehörigen Logeintrag ab. Ein manuelles Zurücksetzen vor jedem Contest ist deshalb normalerweise nicht erforderlich.

Die Schaltfläche **Reset worked, NOT-QRV and grid data...** entfernt sämtliche Worked-Markierungen, NOT-QRV-Tags und gespeicherten Großfelder. Vor dem Reset erscheint eine Sicherheitsabfrage. Die bekannten Rufzeichenzeilen bleiben erhalten; zurückgesetzt werden nur die contestbezogenen Zustände.

Ein Reset ist sinnvoll, wenn bewusst mit einem leeren Conteststand begonnen werden soll oder Testdaten eingelesen wurden. Als tägliche Wartungsmaßnahme ist er nicht vorgesehen.

Anzeige und Herleitung: [Gearbeitete Rufzeichen, neue Bänder und neue Großfelder](de-Funktionen#gearbeitete-rufzeichen-neue-bänder-und-neue-großfelder).

---

## Operator-Profile (ab v1.50)

Mehrere Operateure an einem Rechner brauchen unterschiedliche Rufzeichen, Locators und Layouts. Ein Operator-Profil bündelt genau das: **jedes Profil hat seine eigene `preferences.xml` und damit seine eigenen fachlichen Einstellungen und seinen eigenen Layoutstand.**

### Wo die Profile liegen

| Profil | Einstellungen | Gearbeitete Stationen |
|---|---|---|
| **Default** | `~/.praktiKST/preferences.xml` | `~/.praktiKST/praktiKST.db` |
| weitere Profile | `~/.praktiKST/profiles/<Profil-ID>/preferences.xml` | je nach Einstellung gemeinsam oder `~/.praktiKST/profiles/<Profil-ID>/praktiKST.db` |

Unter Windows entsprechend unterhalb von `%USERPROFILE%\.praktiKST\`.

Alle übrigen Daten bleiben gemeinsam: Klangdateien, Farbschemata, DEM- und Terrainpakete, der Zwischenspeicher der Geländeprofile (`terrainprofilecache.db`), das Fehlerprotokoll und die Versionsinformationen.

Das Profil **Default** benutzt weiterhin genau die Dateien, die eine bestehende Installation schon hat. **Bei der Aktualisierung auf v1.50 wird keine Datei verschoben, kopiert oder umgeschrieben.** Wer eine ältere KST4Contest-Version wieder installiert, findet seine Konfiguration und seine gearbeiteten Stationen unverändert vor.

### Gemeinsame oder eigene gearbeitete Stationen

Beim Anlegen eines Profils wird entschieden, woher dessen Worked-, NOT-QRV- und Großfeld-Daten kommen:

- **Eigene gearbeitete Stationen** (Vorgabe): Das Profil bekommt eine eigene, zunächst leere Datenbank. Sinnvoll, wenn sich zwei OMs mit verschiedenen Rufzeichen einen Rechner teilen.
- **Gemeinsame Stationsdatenbank**: Das Profil benutzt `~/.praktiKST/praktiKST.db`, also dieselbe Datenbank wie das Profil **Default**. Das ist der Fall der **Multi-OP-Station**: es gibt nur ein Stationslog, also soll auch der Worked-Status für alle Operateure derselbe sein.

Ein Wechsel zwischen beiden Einstellungen verschiebt keine Daten. Bereits gesammelte Worked-Daten bleiben dort liegen, wo sie entstanden sind. Der Drei-Tage-Ablauf und die Reset-Schaltfläche wirken jeweils auf die Datenbank des gerade aktiven Profils.

### Profile verwalten

Der Reiter **Profiles** im Einstellungsfenster zeigt alle Profile mit Name, Art der Worked-Daten und letzter Benutzung.

- **New profile...** legt ein Profil an. Es startet **ohne Rufzeichen und ohne Passwort**; beides wird anschließend im Reiter **Station** eingetragen.
- **Duplicate...** übernimmt die komplette Konfiguration des gewählten Profils — Antenne, Locator, Layout, Beacon, Integrationen — **außer Rufzeichen und Passwort**. Gearbeitete Stationen werden nie mitkopiert.
- **Rename...** ändert nur den angezeigten Namen. Verzeichnisse und Dateien bleiben unberührt.
- **Delete...** entfernt das Profilverzeichnis endgültig. Das Profil **Default** und das gerade aktive Profil lassen sich nicht löschen. Bei einem Profil mit gemeinsamer Stationsdatenbank bleibt diese unangetastet.
- **Change worked stations...** schaltet zwischen gemeinsamer und eigener Datenbank um.
- **Switch to selected profile...** wechselt das Profil im laufenden Betrieb.

### Profilwahl beim Start

- Solange nur **ein** Profil existiert, fragt KST4Contest beim Start **nichts** und startet wie bisher. Es wird auch keine Profil-Registry angelegt. Erst das Anlegen des zweiten Profils erzeugt `~/.praktiKST/profiles.xml`.
- Ab **zwei** Profilen erscheint beim Start eine kleine Auswahl. Das zuletzt benutzte Profil ist vorausgewählt, **Enter** oder ein Doppelklick starten sofort.
- Der Aufrufparameter `--profile=<Name>` überspringt die Auswahl und startet direkt das genannte Profil. Erlaubt sind die Profil-ID und der angezeigte Name, Groß- und Kleinschreibung spielen keine Rolle. Ein unbekannter Name führt zu einem Hinweis und danach zur normalen Auswahl — der Start wird nie verweigert.

### Profil im laufenden Betrieb wechseln

**File → Switch operator profile...** oder die Schaltfläche im Reiter **Profiles** wechseln ohne Programmneustart. Nach einer Sicherheitsabfrage wird die ON4KST-Verbindung getrennt und die Oberfläche mit den Einstellungen und dem Layout des gewählten Profils neu aufgebaut. Hält das Einstellungsfenster noch nicht übernommene Änderungen, fragt KST4Contest zuerst, ob sie auf das bisherige Profil übernommen, verworfen oder der Wechsel abgebrochen werden soll. Der Layoutstand des bisherigen Profils wird vorher gesichert; noch nicht mit **Save Settings** bestätigte fachliche Änderungen gehen dabei verloren.

Sobald mehr als ein Profil existiert, zeigt die Titelzeile des Hauptfensters zusätzlich den Profilnamen.

### Hinweis zu Passwörtern

Das ON4KST-Passwort steht wie bisher im Klartext in der `preferences.xml` des jeweiligen Profils. Auf einem gemeinsam genutzten Rechner kann jeder, der Zugriff auf das Benutzerkonto hat, die Passwörter aller Profile lesen. Profile trennen die Konfiguration, sie sind **kein** Zugriffsschutz.

---

## Dark Mode (ab v1.26)

Mit welchem Farbschema KST4Contest startet, legt jedes Operator-Profil selbst fest: im Reiter **GUI** unter **Design** mit **Light mode** oder **Dark mode**. Die Auswahl wirkt, sobald sie übernommen ist, und wird mit **Save Settings** in der `preferences.xml` des Profils gespeichert.

Für einen kurzen Wechsel während des Betriebs gibt es **Windows → Use dark mode design** und **Windows → Use default mode design**. Diese Umschaltung gilt nur bis zum nächsten Start oder Profilwechsel; danach gilt wieder die Einstellung im Reiter **GUI**.

---

## Einstellungen speichern

**Save Settings** übernimmt zuerst offene Änderungen und speichert dann die fachlichen Einstellungen und den vollständigen aktuellen Layoutstand. Änderungen an Fenstergrößen und -positionen, relevanten Dividern, verwalteten Tabellenbreiten sowie den Karteneinstellungen **Gruppierung** und **Immer im Vordergrund** werden zusätzlich automatisch mit kurzer Verzögerung gespeichert. Ein ausstehender Layoutstand wird beim Programmende noch geschrieben.

- Speicherort: unter Linux und macOS `~/.praktiKST/preferences.xml` und unter Windows `%USERPROFILE%\.praktiKST\preferences.xml` (bzw. `C:\Users\<Benutzername>\.praktiKST\preferences.xml`)
- Der automatische Layout-Writer übernimmt keine noch nicht mit **Save Settings** bestätigten fachlichen Änderungen.
- Die Konfigurationsversion 6 ergänzt optionale Spaltenbreiten unter `guiOptions`. Ältere `preferences.xml`-Dateien bleiben lesbar; fehlen Breiten oder sind Einträge ungültig, ermittelt KST4Contest wieder brauchbare Anfangsbreiten.
- Die Konfigurationsversion 7 ergänzt `GUIstationMapClusteringEnabled` unter `guiOptions`. Fehlt der Eintrag oder ist sein Wert unbrauchbar, bleibt die räumliche Kartengruppierung aktiviert.
- `GUIstationMapAlwaysOnTop` ist ein optionaler Layoutwert unter `guiOptions`. Fehlt der Eintrag oder ist sein Wert unbrauchbar, verhält sich das Kartenfenster normal und bleibt nicht dauerhaft im Vordergrund.
- Ältere Programmversionen ignorieren die zusätzlichen XML-Einträge. Wenn eine ältere Version die Datei vollständig neu speichert, können die Spaltenbreiten sowie die gespeicherten Einstellungen für **Gruppierung** und **Immer im Vordergrund** verloren gehen.
- Bei Problemen: Konfigurationsdatei löschen → KST4Contest erstellt eine neue mit Standardwerten.
