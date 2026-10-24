# Benutzeroberfläche

> 🇬🇧 [English version](en-User-Interface) | 🇩🇪 Du liest gerade die deutsche Version

## Verbinden mit dem Chat

Vor dem ersten Verbindungsaufbau müssen im Einstellungsfenster das lokale Login-Rufzeichen und Passwort, der gemeinsame Locator der TCP-Sitzung sowie die primäre Chat-Kategorie konfiguriert werden. Soll zusätzlich eine zweite Kategorie verwendet werden, wird sie dort aktiviert und ausgewählt. KST4Contest meldet sich einmal an und ergänzt die zweite Kategorie per Single Sign-on innerhalb derselben TCP-Sitzung.

Die Verbindung kann auf zwei Wegen aufgebaut werden:

- Mit **Connect to …** im Einstellungsfenster werden die dort eingetragenen Werte übernommen und die Verbindung gestartet.
- **File → Connect to …** verwendet die bereits in KST4Contest übernommenen Einstellungen.

Geänderte Einstellungen müssen mit **Save Settings** gespeichert werden, wenn sie auch beim nächsten Programmstart verwendet werden sollen.

Eine bestehende Verbindung kann über **File → Disconnect** oder über **Disconnect** im Einstellungsfenster beendet werden. **Exit + disconnect** beendet zusätzlich das Programm.

Bei einem unerwarteten Verbindungsverlust versucht KST4Contest nach einer begrenzten Wartezeit, die ON4KST-Verbindung kontrolliert neu aufzubauen. Ein fehlgeschlagener Erstaufbau blockiert die Benutzeroberfläche nicht mehr.

Ob die Verbindung lediglich als TCP-Verbindung besteht oder bereits vollständig angemeldet und synchronisiert ist, zeigt der [`LINK`-Status](#statusleiste-und-hinweise) im Hauptfenster.

---

## Hauptfenster-Überblick

Das Hauptfenster besteht aus mehreren Bereichen:


### Statusleiste und Hinweise

Die Statusleiste befindet sich am oberen Rand des Hauptfensters neben dem Menü.

![Statusleiste mit ON4KST-Verbindungsanzeige](connection_status_indicator.png)

Der dauerhaft sichtbare `LINK`-Indikator zeigt den tatsächlichen Zustand der ON4KST-Verbindung:

| Anzeige | Bedeutung |
|---|---|
| grünes `LINK` | Login und Synchronisation der konfigurierten Chat-Kategorien sind vollständig abgeschlossen |
| gelbes `LINK…` | Verbindung, Anmeldung, Benutzerlistensynchronisation oder kontrolliertes Beenden läuft |
| rotes `LINK!` | keine Verbindung oder Wartezeit vor einem automatischen Neuaufbau |

Der Tooltip enthält den internen Verbindungsstatus und eine genauere Beschreibung des aktuellen Schritts. Der Indikator ist keine Schaltfläche.

`ONLINE` wird erst gemeldet, nachdem die Anmeldung bestätigt und die Benutzerlisten der konfigurierten Kategorien vollständig empfangen wurden. Während die Verbindung noch aufgebaut oder neu synchronisiert wird, bleiben Sendfeld und **TX** deaktiviert.

Bei bestimmten Ereignissen erscheinen vorübergehend weitere Hinweise:

- `SKED` weist auf eine fällige Sked-Erinnerung hin. Der Text enthält das vollständige Zielrufzeichen und die verbleibende Zeit.
- `BAND+` erscheint nach einem Logeintrag, wenn für die gearbeitete Station noch mindestens ein gemeinsames, aktiviertes und nicht gearbeitetes Band erkannt wurde.

Beide Hinweise blinken ungefähr zwölf Sekunden und verschwinden anschließend wieder. Der vollständige Inhalt beziehungsweise die Herleitung steht im jeweiligen Tooltip. Die Anzeigen sind nicht anklickbar.


### PM-Fenster (oben links)

Das PM-Fenster zeigt die an das lokale Login-Rufzeichen adressierten Privatnachrichten und die zugehörigen ausgehenden Antworten.

Nicht selbst gesendete Nachrichten erscheinen dort zusätzlich, wenn ihr Text ohne Beachtung der Groß- und Kleinschreibung das konfigurierte eigene Login-Rufzeichen enthält. Das gilt für öffentliche Nachrichten an `ALL` ebenso wie für gerichtete Nachrichten zwischen anderen Chatteilnehmern. Gerade im zweiten Fall lässt sich dieses PM-Catching flapsig als **„Lästererkennung“** bezeichnen.

Der tatsächliche Empfänger, der Nachrichtentext, die Chat-Kategorie und das Routing bleiben unverändert. Die Nachricht erhält lediglich eine zusätzliche Darstellung im PM-Fenster.

Ist das [QSO-Monitoring](de-Funktionen#qso-monitoring-ab-v131) aktiviert, erscheinen dort zusätzlich die erfassten Nachrichten der überwachten Basisrufzeichen. Diese Einträge erhalten eine `Sniffed:`-Kennzeichnung mit dem vollständigen sichtbaren Absender und Empfänger.

Neue, nicht selbst gesendete Zeilen durchlaufen sechs grüne Altersstufen und kehren nach fünf Minuten zur normalen Tabellenfarbe zurück. Eigene Nachrichten behalten ihre separate Hervorhebung. Die farbliche Darstellung ist nur ein zeitlicher Hinweis; sie verändert weder Inhalt noch Routing der Nachricht.

Die Auswahl einer eingehenden Zeile bereitet eine Antwort an den Absender vor. Bei einer eigenen ausgehenden Nachricht wird stattdessen der ursprüngliche Empfänger als Nachrichtenziel wiederhergestellt. Caught- und Monitoring-Zeilen lösen keine PM-Audioausgabe aus.

Altersstufen: [Farbige PM-Zeilen](de-Funktionen#farbige-pm-zeilen-ab-v125). Erkennung und Grenzen: [PM-Abfang](de-Funktionen#pm-abfang-catching-personal-messages-ab-v11).

### Benutzerliste (Chat Members)

Die zentrale Tabelle aller aktuell aktiven Chat-Nutzer. Spalten (je nach Konfiguration):

| Spalte | Inhalt |
|---|---|
| Callsign | Rufzeichen der Station |
| Name | Name beziehungsweise Zusatzinformationen aus dem Chat-Namensfeld |
| QRA | Maidenhead-Locator |
| QRB | Entfernung in km |
| QTF | Richtung in Grad |
| QRG | Zuletzt aus einer Chat-Nachricht erkannte Frequenz |
| Tropo | Ergebnis der bandbezogenen Tropo- beziehungsweise Streckenbewertung |
| Score | Aktueller, numerisch sortierbarer Prioritätswert des normalisierten Basisrufzeichens |
| Act | Minuten seit der letzten Aktivität |
| AP | AirScout-Flugzeugdaten, sofern aktiviert |
| worked | Bandbezogener Worked-, Bandmöglichkeits- und Großfeldstatus sowie `wkdany` |
| NOT QRV @ | Bänder, auf denen die Station manuell als nicht QRV markiert wurde |
| Category | Chat-Kategorie des Eintrags |

Die QRG-Spalte zeigt die zuletzt für eine Station erkannte Frequenz. Fehlende Nullen werden für die Anzeige ergänzt, sodass beispielsweise `144.21` als `144.210` erscheint. Erkennt KST4Contest nacheinander Frequenzen auf mehreren Bändern, zeigt die Spalte den letzten Treffer; die internen Bandinformationen können trotzdem mehrere aktuelle Bänder der Station enthalten.

Relative Angaben werden zunächst mit einem höchstens 30 Minuten alten Bandkontext desselben Absenders kombiniert. Nur wenn dieser fehlt, verwendet KST4Contest das globale Fallback-Band. Erkennungsregeln, Beispiele und Grenzen: [QRG-Erkennung](de-Funktionen#qrg-erkennung).

### Worked-, Band- und Großfeldstatus

Die Unterspalten unter **worked** sind kompakt, weil bei mehreren aktivierten Bändern kaum Platz für ausgeschriebene Zustände bleibt. `X` kennzeichnet ein auf diesem Band gearbeitetes Rufzeichen. `a` und `B+` weisen auf ein angebotenes, noch nicht gearbeitetes Band hin. Ein angehängtes `o` bedeutet, dass das vierstellige Großfeld auf diesem Band bereits gearbeitet wurde.

Die Unterspalte **wkdany** ist bandunabhängig: `x` steht für ein bereits gearbeitetes Rufzeichen, `o` für ein auf irgendeinem Band gearbeitetes Großfeld und `xo` für beides.

Jede Statuszelle besitzt einen Tooltip mit der Legende und dem für die betreffende Station ermittelten Zustand. Die vollständige Herleitung einschließlich NOT-QRV-Vorrang: [Gearbeitete Rufzeichen, neue Bänder und neue Großfelder](de-Funktionen#gearbeitete-rufzeichen-neue-bänder-und-neue-großfelder).

![Bandbezogener Worked-Status und Worked-Großfelder](worked_band_status.png)

Beim ersten Aufbau ist die Benutzerliste alphabetisch nach dem vollständigen Rufzeichen sortiert, ohne Groß- und Kleinschreibung zu unterscheiden. Neue Stationen werden an der passenden Stelle eingefügt. Eine anschließend über einen Spaltenkopf gewählte Sortierung bleibt bei neuen Stationen, Daten- und Filteraktualisierungen erhalten. QRB wird numerisch sortiert.

Die Stationsauswahl gilt gemeinsam für Benutzerliste, PM-, Public- und QSO-of-the-other-Tabelle, Prioritätsanzeige, AP-Timeline und Karte. Eine Auswahl aus einer anderen Ansicht scrollt die Benutzerliste zur aktuellen Position des Chatmembers und hebt die Zeile hervor. Ist die Station durch einen Filter ausgeblendet, erscheint sie vorübergehend zusätzlich in der Benutzerliste; die gespeicherten Filter und die exakt gefilterte Kartenansicht ändern sich dadurch nicht. Rufzeichen und Chat-Kategorie bilden gemeinsam die Identität.

Ein fett dargestelltes Rufzeichen kennzeichnet eine Station, die während der laufenden Sitzung tatsächlich neu in den Chat gekommen ist. Die Hervorhebung bleibt zehn Minuten sichtbar. Die anfängliche Benutzerliste und eine vollständige Neusynchronisierung nach einem Reconnect erzeugen keine solchen Markierungen. Im Day-Design wird eine kontrastreiche dunkle, im Evening-Design eine entsprechend helle Schrift verwendet.

Ein grün und fett dargestelltes Rufzeichen kennzeichnet eine aus einer gerichteten Nachricht hergeleitete Richtungsgelegenheit. Die Markierung bezieht sich auf den Absender der Nachricht und bleibt höchstens fünf Minuten sichtbar. Herleitung und Grenzen: [Richtungsgelegenheiten aus gerichteten Nachrichten](de-Funktionen#richtungsgelegenheiten-aus-gerichteten-nachrichten).

### Sendfeld

Das Sendfeld enthält den vorbereiteten Text für die nächste ausgehende Nachricht.

Wird eine Station bewusst per Maus oder Tastatur in der Benutzerliste ausgewählt, bereitet KST4Contest eine gerichtete Nachricht vor:

```text
/cq RUFZEICHEN
```

Dabei werden das vollständige sichtbare Rufzeichen einschließlich eines vorhandenen Suffixes und die Chat-Kategorie der ausgewählten Station beibehalten. Ein Ziel wie `9A0BB-70` wird nicht auf `9A0BB` verkürzt.

Eine Hintergrundaktualisierung, Neusortierung oder Filteränderung darf einen bereits bearbeiteten Nachrichtentext nicht überschreiben. Nur eine tatsächliche Auswahl durch den Operator bereitet den `/cq`-Empfänger erneut vor.

- **TX** oder `Enter` sendet den vorbereiteten Text.
- `Esc` leert das Sendfeld.
- Während KST4Contest nicht vollständig mit ON4KST verbunden ist, bleiben Sendfeld und **TX** deaktiviert.

Shortcuts, Snippets und Variablen sind unter [Makros und Variablen](de-Makros-und-Variablen) beschrieben.

### MYQRG- und SECONDQRG-Feld

Die beiden QRG-Felder enthalten die eigenen Frequenzen der primären und sekundären Chat-Kategorie.

`MYQRG` kann von einer aktivierten TRX-Synchronisation aktualisiert oder – bei deaktivierten automatischen QRG-Quellen – von Hand eingetragen werden. `SECONDQRG` bleibt davon unabhängig und enthält die QRG der zweiten Kategorie.

Die Auswahl einer Station aus dem zweiten Chat verändert die Bedeutung der beiden Werte nicht: `MYQRG` gehört weiterhin zur primären, `SECONDQRG` zur sekundären Kategorie.

Weitere Einzelheiten: [TRX-Sync-Einstellungen](de-Konfiguration#trx-sync-einstellungen).

### MYQTF-Feld

Das MYQTF-Feld zeigt die aktuelle Antennenrichtung als numerischen Winkel in Grad.

Ist PSTRotator aktiviert, wird der Wert automatisch übernommen und das Feld ist nicht manuell bearbeitbar. Ohne aktive Rotatorsynchronisation kann die Antennenrichtung direkt eingetragen werden. Die Änderung wird beim Verlassen des Feldes übernommen.

Der Wert beeinflusst unter anderem:

- die QTF-Filterung,
- die Darstellung des Antennensektors auf der Stationskarte,
- die Prioritätsberechnung,
- die AP-Timeline und
- die Variable `MYQTF`.

---

## Tabellenbreiten und gekürzte Zellinhalte

Beim ersten brauchbaren Datenbestand richtet KST4Contest die Spalten der Benutzerliste, der Nachrichtenansichten, der DXCluster- und QSO-Tabellen sowie der Worked-Datenbank einmalig nach Überschrift und vorhandenem Inhalt aus. Bereits gespeicherte Breiten haben Vorrang. **Name**, **AP** und **NOT QRV @** werden dabei begrenzt, damit einzelne lange Werte nicht den restlichen Tabellenbereich verdrängen. **Message** und vergleichbare Freitextspalten bleiben flexibel und richten sich nicht nach der längsten Nachricht.

Manuell geänderte Spaltenbreiten werden automatisch gespeichert und beim nächsten Start wiederhergestellt. Spätere Nachrichten oder Stationsaktualisierungen überschreiben diese Auswahl nicht.

Passt ein normaler Textwert nicht vollständig in seine Zelle, zeigt ein Tooltip den vollständigen Wert. Der Tooltip erscheint nur bei tatsächlich gekürztem Text. Funktionale Tooltips, etwa für QRA-, Worked- oder Bandzustände, bleiben erhalten; bei gekürztem Zelltext stehen Volltext und Erklärung gemeinsam im Tooltip.

### Nachrichtentext und Links

KST4Contest zeigt Nachrichtentexte bewusst einzeilig an. So bleiben auch bei hohem Chat-Aufkommen viele Einträge gleichzeitig sichtbar. Der Nachteil liegt auf der Hand: Bei einer schmalen **Message**-Spalte passt nicht jede Nachricht vollständig in die Zeile.

Ist ein Nachrichtentext breiter als die sichtbare Zelle, zeigt KST4Contest den vollständigen Inhalt als Tooltip an. Dazu die Maus kurz über die betreffende **Message**-Zelle halten. Passt der Text vollständig in die Spalte, wird kein zusätzlicher Volltext-Tooltip eingeblendet.

Webadressen mit `http://`, `https://` oder dem Präfix `www.` werden innerhalb des Nachrichtentextes als Links dargestellt. Ein Klick öffnet die Adresse im Standardbrowser des Betriebssystems. Andere Protokolle werden nicht als Link behandelt.

![Abgeschnittener Nachrichtentext mit Volltext-Tooltip und Link](message_tooltip_and_link.png)

Damit muss der Divider nicht allein deshalb verschoben werden, um eine einzelne längere Nachricht zu lesen. Für einen dauerhaft breiteren Nachrichtenbereich kann er selbstverständlich weiterhin angepasst werden.

---

## Filter und Reachability-Steuerung

Die Filterleiste befindet sich oberhalb der Chatmember-Tabelle. Filter können miteinander kombiniert werden; eine Station bleibt nur sichtbar, wenn sie alle aktiven Bedingungen erfüllt.

Dezent umrahmte Flächen fassen **QRB**, **QTF** einschließlich Richtungstasten, **Hide**, **Show** und **Reachability** jeweils funktional zusammen. **Find** und **Reset filters** bleiben eigenständige Controls. Die Gruppen ändern nur die Darstellung; Filterzustände, Reset-Verhalten und Kartenkopplung bleiben gemeinsam verwaltet.

![Umgebrochene Filterleiste bei schmaler Chatmember-Ansicht](filter_bar_wrapped.png)

> **Hinweis:** Dieser Screenshot zeigt noch die frühere Anordnung der Filterleiste. Er wird nach der visuellen Prüfung der neuen kompakten, umbrechenden Leiste ersetzt.

### Stationsfilter

| Bedienelement | Wirkung |
|---|---|
| **Show only QTF** | Zeigt nur Stationen innerhalb der gewählten Antennenrichtung und des konfigurierten Öffnungswinkels |
| **Show only QRB [km] <=** | Begrenzt die Liste auf die eingetragene maximale Entfernung |
| **Show only QRB [km] >** | Blendet Stationen bis zur eingetragenen Mindestentfernung aus; zusammen mit dem Höchstwert ergibt sich ein Entfernungsbereich |
| **Find** | Filtert nach einem vollständigen oder teilweisen Rufzeichen |
| **wkd** | Blendet Basisrufzeichen aus, die bereits auf mindestens einem unterstützten Band gearbeitet wurden |
| einzelne Band-Schaltflächen | Blenden Stationen aus, die auf dem betreffenden Band bereits gearbeitet oder dort als NOT QRV markiert wurden |
| **Inactive stations** | Blendet Stationen aus, deren letzte Chataktivität mehr als 20 Minuten zurückliegt |
| **Only new grids** | Zeigt nur Stationen aus vierstelligen Großfeldern, die bisher auf keinem Band gearbeitet wurden |
| **New bands** | Zeigt Stationen mit mindestens einer erkannten, lokal aktivierten und noch nicht gearbeiteten Bandmöglichkeit |
| **Tropo >=0dB** | Zeigt Stationen mit einer berechneten, nicht negativen SSB-Marge |
| **AS next 5m** | Zeigt Stationen mit einem aktuellen oder innerhalb der nächsten fünf Minuten erwarteten AirScout-Fenster |

Standardmäßig aktualisiert eine gültige direkte Eingabe in der QTF-Gruppe oder der Klick auf eine Richtungstaste zugleich die aktuelle eigene Antennenrichtung. Diese ausdrückliche Übernahme funktioniert unabhängig davon, ob **Show only QTF** ein- oder ausgeschaltet ist. Das Zurücksetzen oder bloße Ausschalten des Filters verändert die Antennenrichtung nicht. Im Abschnitt **Station → Manuelle Antennenrichtung** kann das Verhalten mit **QTF-Filterrichtung als eigene Antennenrichtung übernehmen** profilbezogen ausgeschaltet werden. PSTRotator 1 hat vorübergehend Vorrang, ohne diese gespeicherte Auswahl zu verändern.

Bei **New bands** werden aktuelle QRGs, Bandangaben im Namensfeld und aktive Rufzeichenvarianten gemeinsam ausgewertet. Manuelle NOT-QRV-Markierungen haben Vorrang.

Der Filter **Tropo >=0dB** entfernt nur Stationen, für die eine abgeschlossene Berechnung eine negative Marge ergeben hat. Noch nicht berechnete oder fehlgeschlagene Auswertungen bleiben sichtbar. Andernfalls würde ein fehlender API-Wert wie ein nachgewiesen ungeeigneter Funkweg behandelt.

### Grid color

**Grid color** ist kein Filter. Die Funktion verändert ausschließlich die Darstellung des QRA-Feldes und kennzeichnet bereits gearbeitete vierstellige Großfelder.

Die Station bleibt unabhängig von der Farbmarkierung in der Tabelle sichtbar. **Reset filters** deaktiviert diese Anzeige deshalb nicht.

### Reachability und Calc selected

Das Dropdown **Reachability** bestimmt das Band, auf das sich die Tropo-Spalte, der Tropo-Filter und eine ausdrücklich gestartete Streckenberechnung beziehen.

- **Auto** leitet das Band aus der aktuellen Stations-QRG, Bandangaben im Namensfeld und der unterstützten Chat-Kategorie her.
- Ein ausdrücklich gewähltes Band übersteuert diese automatische Auswahl für die Reachability-Auswertung.

Eine Änderung des Dropdowns startet keine Berechnung für die gesamte Benutzerliste. Das wäre bei einer Online-Höhendatenquelle unnötig langsam und würde externe API-Abfragen vervielfachen.

**Calc selected** berechnet ausschließlich die aktuell ausgewählte Station auf dem gewählten beziehungsweise automatisch hergeleiteten Band. Das Ergebnis wird anschließend in der Tropo-Spalte und den zugehörigen Ansichten verwendet.

### Filter zurücksetzen

**Reset filters** entfernt:

- den QTF-Filter,
- den QRB-Filter,
- den Inhalt des Rufzeichen-Suchfeldes,
- alle Worked- und Bandfilter,
- **Inactive stations**,
- **Only new grids**,
- **New bands**,
- **Tropo >=0dB** und
- **AS next 5m**.

Die internen Filterprädikate werden dabei ausdrücklich geleert. Es genügt nicht, lediglich die sichtbaren Toggle-Buttons zurückzusetzen.

Nicht verändert werden:

- **Grid color**, weil es sich um eine Darstellungsoption handelt, und
- die Auswahl im **Reachability**-Dropdown, weil sie das Berechnungsband festlegt und nicht unmittelbar die Tabelle filtert.

### Verhalten bei schmaler Ansicht

Die Filterleiste besitzt keine feste Breite. QTF-, Worked- und Reachability-Controls nutzen zunächst den verfügbaren Platz ihrer jeweiligen Zeile.

Wird der mittlere Divider nach rechts verschoben und die Chatmember-Ansicht dadurch schmaler, wechseln Bedienelemente erst dann in die nächste Zeile, wenn ihre tatsächlich benötigte Breite nicht mehr ausreicht. Wird der Bereich wieder breiter, ordnen sie sich unmittelbar neu an.

Im Klartext: Die Filter bestimmen den Tabelleninhalt, aber nicht mehr die Mindestbreite der gesamten rechten Programmseite.

---

## Stationsinfo-Panel (Further Info)

Rechts unten werden die Nachrichten der ausgewählten Station zusammengeführt. Dazu gehören öffentliche Nachrichten, Privatnachrichten an die eigene Station und – soweit im Chat sichtbar – Privatnachrichten an andere Stationen.

Der im Panel gewählte Filter bestimmt als eigene Dimension, welche Nachrichtenart für die ausgewählte Station angezeigt wird. Unter **Settings → GUI** lässt sich der Startwert festlegen:

- **pm to me**: private Nachrichten zwischen der ausgewählten und der eigenen Station sowie öffentliche Nachrichten der ausgewählten Station, deren strukturierte Adressangabe am Anfang das eigene Login-Rufzeichen nennt,
- **pm to other**: private Nachrichten der ausgewählten Station an andere Teilnehmer,
- **Public msgs**: öffentliche Nachrichten der ausgewählten Station an `ALL`,
- **nothing**: alle Nachrichten mit Beteiligung der ausgewählten Station in ihrer Chat-Kategorie, ohne zusätzliche Einschränkung nach Nachrichtenart oder Empfänger.

Eine bloße Erwähnung des eigenen Rufzeichens irgendwo im Nachrichtentext macht eine öffentliche Zeile nicht zu **pm to me**. Dafür muss die ON4KST-Nachricht mit der ausdrücklichen Adressangabe in Klammern beginnen.

Die Einstellung verändert nur die Darstellung im Stationsinfo-Panel. Nachrichten werden dadurch weder verworfen noch aus den übrigen Nachrichtentabellen entfernt. Der Filter kann im Panel jederzeit für die aktuell betrachtete Station gewechselt werden.

Ein horizontaler Divider im Further-Info-Bereich teilt die Höhe zwischen Nachrichtentabelle und Stationsfunktionen. Die Nachrichtentabelle wird beim Verschieben unmittelbar höher oder niedriger. NOT-QRV und Sked besitzen im unteren Bereich einen eigenen vertikalen Scrollbereich; **Look up and show** bleibt darunter fest sichtbar. Die Dividerposition wird mit dem Layout des aktiven Root- oder Operatorprofils gespeichert und bei einem kleineren Fenster auf einen bedienbaren Wert begrenzt. Leere Tabellen zeigen weiterhin ihre Spaltenstruktur und den Hinweis **Keine Einträge**.

Der Stationskopf fasst Rufzeichen, Chat-Kategorie, QTF, QRB, letzte Aktivität und erkannte Bänder platzsparend zusammen. Der Locator wird im Kopf nicht noch einmal wiederholt; in den übrigen Stationsansichten bleibt er unverändert verfügbar. Bei einem schmalen Panel fließen die Angaben kontrolliert in weitere Zeilen. **Show messages** steht direkt vor den vier Nachrichtenfiltern und fließt gemeinsam mit ihnen um.

Die Bereiche **NOT QRV** und **Sked alert** stehen nebeneinander, solange die tatsächliche Breite des rechten Panels dafür ausreicht. Wird der Splitter weiter nach rechts verschoben, ordnet KST4Contest die beiden umrahmten Gruppen untereinander an. Die enthaltenen Controls brechen innerhalb ihrer Gruppe ebenfalls um; ein horizontaler Scrollbereich ist dafür nicht nötig.

Im unteren Bereich können für die ausgewählte Station bandbezogene **Not QRV**-Markierungen gesetzt werden. Sichtbar sind kompakte Togglebuttons für die Bänder, die in den Stationseinstellungen für die eigene Station aktiviert wurden. **all** setzt beziehungsweise entfernt die Markierung für alle unterstützten Bänder gemeinsam, einschließlich momentan nicht eingeblendeter Bänder.

Die Änderung wirkt sofort auf die Spalte **NOT QRV @**, die Bandmöglichkeiten und die zugehörigen Filter. Sie wird in der internen Datenbank gespeichert und nach einem Neustart wiederhergestellt.

![Bandbezogene NOT-QRV-Markierungen im Further-Info-Bereich](not_qrv_controls.png)

> **Screenshot-Hinweis:** Dieses Bild zeigt noch die frühere Checkbox-Darstellung. Es wird nach der manuellen Abnahme der kompakten Togglebuttons ersetzt.

Im selben Bereich wird der aktuelle **Priority score** der ausgewählten Station angezeigt.

Mit **Sked fail** lässt sich ein fehlgeschlagener Versuch markieren. Der Score des normalisierten Basisrufzeichens wird dadurch stark reduziert. **Reset fail** entfernt diese Markierung wieder. Die Markierung gilt für alle aktiven Suffix- und Kategorievarianten der Station und bleibt innerhalb der laufenden Programmsitzung erhalten.

Darunter befinden sich die Bedienelemente zum Anlegen eines Skeds:

| Bedienelement | Bedeutung |
|---|---|
| **Sked in** | Zeit bis zum Sked |
| **Band** | vereinbartes Band aus den eigenen aktivierten Bändern |
| **Mode** | `SSB` oder `CW` für eine mögliche Win-Test-Übergabe |
| **Create sked** | internen Sked anlegen |
| **Remind-PM in** | automatische Reminder-PMs aktivieren |
| **2+1**, **5+2+1**, **10+5+2+1** | Zeitpunkte der Reminder-PMs vor dem Termin |

![Sked-Steuerung im Further-Info-Bereich](sked_controls.png)

> **Screenshot-Hinweis:** Dieses Bild zeigt noch die frühere getrennte Anordnung. Es wird nach der manuellen Abnahme der responsiven Bereiche **NOT QRV** und **Sked alert** ersetzt.

Das vorgeschlagene Band wird aus aktuellen QRG- und Namensinformationen der Station hergeleitet. Vor dem Anlegen kann es ausdrücklich geändert werden. Die Mode-Auswahl betrifft nur die Übergabe an Win-Test; der interne Sked und die Reminder-PMs funktionieren unabhängig davon.

**Create sked** legt den Termin immer zuerst in KST4Contest an. Ist der Win-Test-Netzwerk-Listener aktiv, wird anschließend zusätzlich eine Übergabe an Win-Test versucht. Kann keine zum ausgewählten Band passende QRG ermittelt werden oder ist Win-Test nicht erreichbar, bleiben der interne Sked, seine Priorisierung und gegebenenfalls angelegte Reminder erhalten.

Die vollständige Herleitung und die Grenzen der Funktion sind unter [Skeds und Sked-Erinnerungen](de-Funktionen#skeds-und-sked-erinnerungen) beschrieben.

---

## Prioritätsliste

Die kompakte Leiste befindet sich rechts zwischen Benutzerliste und Further-Info-Bereich. Links erscheint bei Bedarf die Gruppe **Neu hinzugekommen**, rechts bleibt die unabhängige Gruppe **Priority**.

**Neu hinzugekommen** zeigt die neuesten tatsächlichen Chat-Beitritte für zehn Minuten an, neuere Einträge zuerst. Bei ausreichender Breite sind bis zu fünf Stationen direkt sichtbar; weitere oder wegen geringer Breite ausgeblendete Einträge stehen im kompakten **more**-Dropdown. Die anfängliche Benutzerliste, eine vollständige Neusynchronisierung, reine Profilaktualisierungen und das eigene Rufzeichen werden nicht aufgenommen. Verlässt eine Station den Chat, verschwindet ihr Eintrag sofort. Ein späterer echter Wiedereintritt startet die Frist neu. Vollständiges Rufzeichen und Chat-Kategorie bleiben getrennte Identitäten.

Ein Klick auf einen Eintrag verwendet die gemeinsame Stationsauswahl, aktualisiert das Further-Info-Panel und scrollt bei Bedarf zur Station in der Benutzerliste.

Die Gruppe **Priority** zeigt die beiden derzeit höchstbewerteten Kandidaten unmittelbar im Hauptfenster:

```text
Priority:  1 RUFZEICHEN SCORE  2 RUFZEICHEN SCORE  more
```

Ein Klick auf einen der beiden Kandidaten wählt den dazugehörigen aktiven Chatmember aus. Dabei werden das vollständige Rufzeichen einschließlich Suffix und die zugehörige Chat-Kategorie verwendet.

Die Schaltfläche **more** öffnet ein separates Fenster mit bis zu 15 Kandidaten. Die Liste ist nach absteigendem Score sortiert. Ein Doppelklick wählt den betreffenden Kandidaten aus und schließt das Fenster.

![Priority Score, kompakte Kandidatenliste und Further-Info-Steuerung](priority_score_overview.png)

> **Hinweis:** Dieser Screenshot zeigt noch die frühere Geometrie des Further-Info-Bereichs. Er wird nach der visuellen Prüfung des neuen scrollbar begrenzten Stationsbereichs ersetzt.

Stationen mit einem Score von `0` werden nicht in die Prioritätsliste aufgenommen. In der Benutzerliste bleiben sie sichtbar, sodass der Ausschluss nachvollzogen und beispielsweise durch eine geänderte NOT-QRV-Markierung korrigiert werden kann.

Der Score wird für das normalisierte Basisrufzeichen berechnet. Mehrere aktive Varianten wie `9A0BB-2` und `9A0BB-70` können daher in der Benutzerliste denselben Wert anzeigen. Die Chatmember bleiben trotzdem getrennte Nachrichtenziele.

Neue Nachrichten, AirScout-Daten, Skeds und Statusänderungen lösen eine Neuberechnung aus. Zusätzlich erfolgt eine regelmäßige Aktualisierung im Hintergrund. Eine kurzzeitig noch nicht angepasste Reihenfolge ist deshalb kein Fehler.

Herleitung und Grenzen: [Prioritätsscore und Prioritätsliste](de-Funktionen#prioritätsscore-und-prioritätsliste-ab-v140).

---

## AP-Timeline

Die AP-Timeline zeigt Chancen im kommenden Zeitfenster relativ zu jetzt. Die zurückhaltende Minutenachse läuft von `0 min` links bis `+30 min` rechts. Normalerweise erscheinen Marken in Fünf-Minuten-Schritten; bei wenig horizontalem Platz wird der Abstand vergrößert, damit die Beschriftungen lesbar bleiben.

Die vier Kandidatenzeilen entsprechen direkt der vorhandenen Prioritätsauswahl: **Im Strahl 1**, **Im Strahl 2**, **Priorität 1** und **Priorität 2**. **Skeds** besitzen eine eigene Zeile. Die bisherige Farbcodierung, Antennenausrichtung und Auswahl einer Station per Klick bleiben erhalten.

---

## Stationskarte

Die Stationskarte kann auf zwei Wegen geöffnet werden:

- **Windows → Show / hide station map** öffnet oder schließt das Kartenfenster.
- **Show on map** im **Further Info**-Bereich öffnet die Karte und fokussiert die ausgewählte Station.

Die Karte verwendet die Stationen, die nach Anwendung der aktuellen Benutzerlistenfilter noch sichtbar sind. Die Kopfzeile zeigt die Anzahl der dargestellten Stationen und weist mit `filtered view active` auf eine gefilterte Ansicht hin.

![Stationskarte mit ausgewählter Station und eingeblendeter Streckenanalyse](station_map_path_analysis.png)

> **Screenshot-Hinweis:** Das Bild zeigt noch die frühere Markerbeschriftung und weder das neue Kontextmenü noch die Worked-Großfeldfläche. Es wird nach der manuellen Kartenabnahme ersetzt.

### Station auswählen

Ein Marker zeigt als sichtbare Beschriftung ausschließlich das Rufzeichen. Locator, erkannte Bänder, `B+`-Status und zuletzt bekannte QRGs stehen weiterhin im Tooltip.

Ein einzelner Stationsmarker kann direkt angeklickt werden. KST4Contest:

1. übernimmt den konkreten Chatmember als aktuelle Auswahl,
2. scrollt die Benutzerliste zum entsprechenden Eintrag,
3. aktualisiert den **Further Info**-Bereich und
4. bereitet das vollständige sichtbare Rufzeichen als `/cq`-Empfänger vor.

Ist **Gruppierung** ausgewählt, werden Marker, die bei niedrigen Zoomstufen zu dicht beieinanderliegen, als Cluster mit einer Stationsanzahl dargestellt. Ein Klick auf einen Cluster vergrößert den betreffenden Kartenausschnitt. Erst ein anschließend sichtbarer einzelner Marker wählt eine konkrete Station aus. Wird die Checkbox ausgeschaltet, zeigt die Karte unabhängig von der Zoomstufe alle positionierbaren Stationen als einzelne Marker. Der Tooltip erläutert den vollständigen Zweck des kompakten Schalters.

Das Umschalten wirkt sofort und verändert weder Zoom noch Kartenausschnitt oder Stationsauswahl. Die Einstellung wird automatisch gespeichert und beim nächsten Programmstart wiederhergestellt. Ohne gespeicherte Einstellung bleibt **Gruppierung** ausgewählt, damit bestehende Installationen zunächst das bisherige Verhalten behalten.

Der Schalter betrifft nur die räumlichen Cluster auf dem Bildschirm. Aktive Chatvarianten desselben normalisierten Basisrufzeichens können weiterhin einen gemeinsamen geografischen Marker verwenden und bleiben unabhängig davon getrennte Nachrichtenziele.

Ein Rechtsklick auf einen Marker oder eine Zeile der Benutzerliste öffnet dasselbe Stations-Kontextmenü. Es enthält **Message snippets**, **Create sked…**, bandbezogene **Tag NOT QRV**- und **Mark as worked**-Aktionen sowie **Calculate reachability**. Repräsentiert ein Marker mehrere vollständige Rufzeichen oder Chat-Kategorien, muss zuerst das konkrete Ziel gewählt werden. Dadurch werden keine Aktionen willkürlich auf den ersten Eintrag angewendet.

Die Snippet-Aktion ergänzt lediglich das Sendefeld und sendet nicht selbst. Der Sked-Dialog arbeitet mit dem beim Öffnen festgehaltenen Ziel; ein späterer Auswahlwechsel ändert diesen Entwurf nicht. **Mark as worked** verwendet dieselbe additive Worked- und Datenbanklogik wie ein empfangenes Logger-`contactinfo` und entfernt keine bereits vorhandene Markierung.

**Calculate reachability** startet die vorhandene Streckenberechnung ausdrücklich für das gewählte Band. Wird die Aktion aus der Benutzerliste aufgerufen, öffnet KST4Contest die Karte und zeigt dort Ladezustand, Ergebnis oder Fehler für genau dieses Ziel an.

Die Kopfzeile ergänzt bei ausgewählter Station:

- vollständiges Rufzeichen,
- Locator,
- QRB und QTF,
- erkannte aktive Bänder,
- eine gegebenenfalls vorhandene `B+`-Bandmöglichkeit und
- die zuletzt bekannten QRGs.

Lange Inhalte werden in der Kopfzeile gekürzt. Der vollständige Text steht im Tooltip.

**Filter zurücksetzen** führt dieselbe Aktion wie die Schaltfläche in der Benutzerliste aus. Dabei werden alle aktivierten Stationsfilter, die Rufzeichensuche und Richtungsfilter zurückgesetzt. Die reine Darstellungsoption **Grid color** bleibt ebenso unverändert wie Sortierung, Stationsauswahl, Zoom und Kartenausschnitt. **Immer im Vordergrund** wirkt ausschließlich auf das Kartenfenster, wird sofort angewendet und für dieses Operatorprofil automatisch gespeichert; ohne gespeicherten Wert ist die Option ausgeschaltet.

Bei aktivem **Grid color** kennzeichnet die Karte dauerhaft bekannte, bereits gearbeitete vierstellige Maidenhead-Großfelder mit einem schwarzen Rahmen und einer schwarzen Flächenüberblendung von 30 Prozent. Die Darstellung stammt aus derselben persistenten Worked-Grid-Verwaltung wie **Only new grids** und bleibt deshalb auch sichtbar, wenn sich momentan keine Chatstation in dem Großfeld befindet. Bei ausgeschaltetem **Grid color** werden weder Rahmen noch Flächenüberblendung gezeichnet.

### Auswahl mit Reset view löschen

**Reset view** löscht die Stationsauswahl, ohne die Kartenposition oder den Zoomlevel zu verändern.

Dabei werden:

- die ausgewählte Station zurückgesetzt,
- die Auswahl in der Benutzerliste aufgehoben,
- die Verbindungslinie zur Gegenstation entfernt,
- eine noch laufende Auswertung der vorherigen Station verworfen und
- der rechte Analysebereich entfernt.

Die Karte selbst bleibt im zuvor gewählten Ausschnitt. Die Funktion ist deshalb kein geografischer Reset auf den eigenen Standort.

![Stationskarte nach Reset view ohne ausgewählte Station](station_map_reset.png)

Wird anschließend wieder ein einzelner Marker gewählt, erscheinen Stationsauswahl und Analysebereich erneut.

### DX-Cluster-Spot auslösen

**Trigger cluster spot** erscheint nur bei ausgewählter Station. Die Schaltfläche sendet einen einzelnen Spot an die mit dem integrierten DX-Cluster-Server verbundenen Logprogramme.

Vorausgesetzt werden:

- ein aktivierter lokaler DX-Cluster-Server,
- mindestens ein verbundener Cluster-Client und
- eine für die ausgewählte Station verwendbare QRG.

Der Spot wird nicht an einen öffentlichen Internet-Cluster gesendet.

### Streckenanalyse

Unterhalb der Karte befindet sich das Höhenprofil. Der rechte Analysebereich zeigt unter anderem:

- verwendete Datenquelle und Anzahl der Höhenpunkte,
- Analysefrequenz,
- Erdkrümmungs- beziehungsweise Refraktionsmodell,
- Radio- und Geländehorizont,
- Fresnel-Freiheit,
- erkannte Hindernisse,
- Link-Budget,
- geschätzten Empfangspegel und
- eine zusammenfassende Pfadbewertung.

Die Auswertung verwendet dasselbe zentral hergeleitete Band wie die Reachability-Funktionen. Ein im **Reachability**-Dropdown ausdrücklich gewähltes Band wird berücksichtigt.

Die Werte bleiben technische Abschätzungen. Gebäude, Bewuchs, lokale Abschattungen, aktuelle Ausbreitungsbedingungen und nicht bekannte Stationsparameter können das reale Ergebnis deutlich verändern.

### Streckenanalyse ausblenden

Mit **Hide path analysis** werden Höhenprofil und rechter Analysebereich gemeinsam ausgeblendet. Der Kartenbereich erhält dadurch mehr Platz.

![Stationskarte mit ausgeblendeter Pfadanalyse](station_map_compact.png)

Der Hinweis **Path analysis is hidden** und die Schaltfläche **Show path analysis** bleiben sichtbar. Die Funktion kann daher ohne Umweg wieder eingeschaltet werden.

Ist beim Wiedereinblenden keine Station ausgewählt, erscheint kein leerer rechter Bereich. Er wird erst wieder aufgebaut, nachdem eine konkrete Station gewählt wurde.

Die Auswahl wird gespeichert und beim nächsten Programmstart wiederhergestellt.

Der Divider zwischen Karte und Detailbereich lässt sich horizontal verschieben. Bei einem schmalen Detailbereich werden längere Angaben umgebrochen; reicht die verfügbare Höhe nicht aus, erscheint eine vertikale Scrollleiste.

Ausführliche Herleitung und Grenzen: [Stationskarte und Streckenanalyse](de-Funktionen#stationskarte-und-streckenanalyse-ab-v141).

---

## Globale Nachrichtentabs und Monitorfenster

Der untere Bereich des Hauptfensters enthält drei globale Nachrichtentabs. Ihr Inhalt ist nicht von der aktuell in der Benutzerliste ausgewählten Station abhängig.

| Tab | Inhalt |
|---|---|
| **Public messages** | Öffentliche Chatnachrichten, CQ-Rufe und Beacons |
| **DXCluster messages** | Über ON4KST empfangene DX-Cluster-Meldungen |
| **QSO of the other** | Gerichtete Nachrichten zwischen zwei anderen Stationen |

Ein Klick auf eine Zeile unter **Public messages** oder **QSO of the other** wählt den Absender als aktuelle Station aus und scrollt die Benutzerliste zu dessen Rufzeichen und Chat-Kategorie. Der Inhalt der globalen Tabs bleibt dabei unverändert.

![Globale Nachrichtentabs im Hauptfenster](global_message_tabs.png)

Im Tab **QSO of the other** werden Absender und Empfänger getrennt dargestellt. Die Spalten **Last QRG TX** und **Last QRG RX** enthalten die zuletzt für beide Stationen bekannten Frequenzen. Sie geben nicht zwingend die QRG der angezeigten Unterhaltung wieder.

**wkd TX?** und **wkd RX?** zeigen den globalen Worked-Status der beiden Basisrufzeichen. Die Angaben sind nicht bandbezogen.

Der Tab **DXCluster messages** zeigt den meldenden und den gemeldeten Teilnehmer, deren Locator, die QRG, den Meldungstext und den globalen Worked-Status der gemeldeten Station. Welche Felder tatsächlich gefüllt sind, hängt von der vom ON4KST-Server übertragenen Meldung ab.

Nachrichtentexte bleiben einzeilig. Ist eine Zelle zu schmal, erscheint der vollständige Inhalt als Tooltip. Webadressen im Meldungstext lassen sich anklicken.

### Separates Monitorfenster

Zusätzlich öffnet KST4Contest das Fenster **Cluster & QSO of the other**. Es zeigt oben die DX-Cluster-Meldungen und darunter die gerichteten Nachrichten zwischen anderen Stationen.

![Separates Cluster- und QSO-Monitorfenster](cluster_qso_monitor.png)

Die Position des vertikalen Dividers sowie die Fenstergröße werden automatisch gespeichert. Die DXCluster- und QSO-Tabellen besitzen hier eigene Spaltenbreiten; Änderungen im Monitorfenster verändern daher nicht das Tabellenlayout der Hauptfenster-Tabs.

Das Fenster lässt sich über das Menü aus- und wieder einblenden:

```text
Windows → Hide cluster / stranger QSOs
Windows → Show cluster / stranger QSOs
```

Die Tabellen im Hauptfenster und im Monitorfenster greifen auf dieselben Daten zu. Das Ausblenden des Monitorfensters beendet daher weder den Empfang noch die Darstellung in den unteren Tabs.

Herleitung und Grenzen: [Globale Nachrichtenansichten](de-Funktionen#globale-nachrichtenansichten).

---

## Menü

### File

- **Connect to …** startet die Verbindung mit den bereits übernommenen Einstellungen.
- **Disconnect** beendet die aktuelle ON4KST-Verbindung, ohne KST4Contest zu schließen.
- **Exit + disconnect** beendet die Verbindung und anschließend das Programm.

Die Connect- und Disconnect-Einträge werden entsprechend dem aktuellen Verbindungszustand aktiviert oder deaktiviert.

### Options

- **Set QRG as name in Chat (main category)** sendet `/SETNAME` mit der aktuellen `MYQRG` an die primäre Chat-Kategorie.
- **Show me as away in chat** sendet `/AWAY`.
- **Show me as active in chat** sendet `/BACK`.
- **Show options** blendet das Einstellungsfenster ein beziehungsweise aus.

Die serverbezogenen Funktionen sind nur bei vollständig aufgebauter ON4KST-Verbindung verfügbar.

### Windows

- **Hide cluster / stranger QSOs** beziehungsweise **Show cluster / stranger QSOs** blendet das zusätzliche Cluster- und QSO-Monitorfenster aus oder wieder ein.
- **hide options** beziehungsweise **show options** blendet das Einstellungsfenster aus oder wieder ein.
- **Use dark mode design** aktiviert das dunkle Farbschema für die laufende Sitzung.
- **Use default mode design** aktiviert das normale helle Farbschema für die laufende Sitzung.
  Das Farbschema beim Start wird pro Profil im Einstellungsreiter **GUI** unter **Design** festgelegt.
- **Show / hide station map** öffnet beziehungsweise schließt das separate Fenster mit Stationskarte und Streckenanalyse.

---

## Fenstergrößen und Divider

KST4Contest speichert Größen und Positionen der Programmfenster, die relevanten Divider sowie manuell geänderte Tabellenbreiten automatisch nach einer kurzen Verzögerung in der Konfigurationsdatei. Ein ausstehender Layoutstand wird beim Programmende noch geschrieben. **Save Settings** ist dafür nicht erforderlich, speichert aber weiterhin den vollständigen aktuellen Stand einschließlich Layout.

Dazu gehört der horizontale Divider zwischen der Nachrichtentabelle und den Stationsfunktionen im Further-Info-Bereich. Sein Wert gilt für das aktive Root- oder Operatorprofil; beim Wiederherstellen begrenzt KST4Contest ihn auf die im aktuellen Fenster nutzbare Höhe.

Das Hauptfenster wird beim Start zusätzlich gegen den sichtbaren Bereich des primären Bildschirms geprüft. Ist die gespeicherte Größe zu groß, verkleinert und verschiebt KST4Contest das Fenster so, dass es wieder erreichbar bleibt. Die genaue Herleitung ist unter [Bildschirmgerechte Größe des Hauptfensters](de-Funktionen#bildschirmgerechte-größe-des-hauptfensters-ab-v141) beschrieben.

Für die übrigen Programmfenster gilt diese zusätzliche Größenbegrenzung derzeit nicht. Wird beispielsweise das separate Monitorfenster nach einem Wechsel auf einen kleineren Bildschirm zu groß dargestellt, genügt eine manuelle Korrektur; die neue Größe wird automatisch gespeichert.

Bei einer ungünstigen Aufteilung sollten zuerst die Divider und Spaltenbreiten wieder an brauchbare Positionen verschoben werden. Das Löschen der Konfigurationsdatei setzt zwar die UI-Werte zurück, entfernt aber auch die übrigen gespeicherten Programmeinstellungen und sollte deshalb nur verwendet werden, wenn sich die Oberfläche auf anderem Weg nicht mehr herstellen lässt.

---

## Tipps zur Bedienung

- **Einstellungsfenster geöffnet lassen**: Schneller Zugriff auf Beacon-Aktivierung/Deaktivierung.
- **Rechtsklick in der Benutzerliste**: Öffnet das Snippet-Menü und weitere Aktionen (QRZ.com-Profil, NOT-QRV-Tags setzen).
- **Enter aus dem Chat heraus**: Wenn im Sendfeld Text steht, sendet Enter direkt – auch wenn der Fokus woanders liegt.
- **Beacon stoppen**: Beim Scannen von Frequenzen den Beacon ausschalten, damit der Chat nicht mit Meldungen überflutet wird.
