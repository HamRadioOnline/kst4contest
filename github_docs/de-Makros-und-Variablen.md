# Makros und Variablen

> 🇬🇧 [English version](en-Macros-and-Variables) | 🇩🇪 Du liest gerade die deutsche Version

KST4Contest unterscheidet zwischen Shortcut-Schaltflächen, Text-Snippets und Variablen. Shortcuts und Snippets enthalten vorbereitete Texte. Variablen ergänzen Informationen, die sich während des Betriebs ändern können.

Der eingefügte Text bleibt im Sendfeld sichtbar und kann vor dem Senden geprüft oder geändert werden.

---

## Überblick

| Mechanismus | Aufruf | Verwendung |
|---|---|---|
| **Shortcut** | Schaltfläche oberhalb des Sendfeldes | Fügt einen konfigurierten Text in das Sendfeld ein |
| **Snippet** | Kontextmenü oder `Ctrl+1` bis `Ctrl+0` | Bereitet einen Text für die ausgewählte Station vor |
| **Variable** | Platzhalter innerhalb eines Nachrichtentextes | Fügt aktuelle QRG-, Locator-, Richtungs-, Stations- oder AirScout-Informationen ein |

Shortcuts und Snippets speichern Texte. Variablen liefern die dazugehörigen aktuellen Werte.

Ein Shortcut wie

```text
pse sked?
```

fügt immer denselben Text ein. Ein Shortcut mit

```text
pse call me at MYQRGSHORT
```

verwendet dagegen die QRG, die beim Anklicken des Shortcuts aktuell in KST4Contest hinterlegt ist.

---

## Shortcut-Schaltflächen

Shortcuts werden unter **Preferences → Shortcut Settings** konfiguriert.

![Konfiguration der Shortcut-Schaltflächen und Text-Snippets](client_settings_window_shortcuts.png)

Jeder Eintrag erzeugt eine Schaltfläche im Hauptfenster. Ein Klick hängt den konfigurierten Text an den vorhandenen Inhalt des Sendfeldes an. Ein bereits vorbereiteter Nachrichtentext wird dabei nicht gelöscht.

Enthält der Shortcut eine Variable, wird sie beim Einfügen aufgelöst. Aus

```text
pse call me at MYQRGSHORT
```

kann beispielsweise werden:

```text
pse call me at 144.388
```

Die exakten Einträge `MYQRG` und `SECONDQRG` werden als QRG-Schaltflächen hervorgehoben. Sie fügen die aktuelle QRG der ersten beziehungsweise zweiten Chat-Kategorie ein.

Auch der Shortcut

```text
/SETNAME MYQRG
```

wird hervorgehoben. Beim Anklicken wird `MYQRG` aufgelöst und der daraus entstehende Serverbefehl in das Sendfeld übernommen. Der Befehl wird nicht automatisch gesendet.

Die Reihenfolge der Einträge in den Einstellungen bestimmt die Reihenfolge der Schaltflächen im Hauptfenster. Bearbeitung, Sortierung und Speicherung sind unter [Konfiguration – Shortcut Settings](de-Konfiguration#shortcut-settings-schnellzugriff-schaltflächen) beschrieben.

---

## Text-Snippets

Snippets werden unter **Preferences → Snippet Settings** konfiguriert. Sie sind vor allem für wiederkehrende Nachrichten an eine bestimmte Station vorgesehen.

Snippets können aufgerufen werden:

- per Rechtsklick auf eine Station in der Benutzerliste,
- per Rechtsklick auf eine öffentliche Nachricht,
- per Rechtsklick auf eine Privatnachricht oder
- mit `Ctrl+1` bis `Ctrl+0` für die ersten zehn Einträge der Snippet-Liste.

### Verwendung über das Kontextmenü

Die Auswahl einer Station oder Nachricht bereitet normalerweise bereits den passenden `/cq`-Empfänger im Sendfeld vor. Das anschließend im Kontextmenü ausgewählte Snippet wird an diesen Text angehängt.

Ein bereits vorhandener Nachrichtentext kann dadurch gezielt erweitert werden.

### Verwendung über die Tastatur

Ein mit `Ctrl+1` bis `Ctrl+0` aufgerufenes Snippet ersetzt den bisherigen Inhalt des Sendfeldes durch eine vollständig adressierte Nachricht:

```text
/cq RUFZEICHEN Snippet-Text
```

Das vollständige sichtbare Rufzeichen einschließlich eines vorhandenen Suffixes bleibt erhalten. Für die ausgewählte Station `9A0BB-70` kann beispielsweise entstehen:

```text
/cq 9A0BB-70 pse ur qrg?
```

KST4Contest behält intern auch die Chat-Kategorie der ausgewählten Station bei. Ein Snippet für `9A0BB-70` wird deshalb nicht versehentlich über die andere aktive Chat-Kategorie gesendet.

Ist keine Station ausgewählt oder existiert für die gedrückte Tastenkombination kein Snippet, wird nichts eingefügt.

Der vorbereitete Text wird nicht automatisch versendet:

- `Enter` oder **TX** sendet die Nachricht.
- `Esc` leert das Sendfeld.

### Zuordnung der Tastenkombinationen

Die Zuordnung folgt der Reihenfolge in der Snippet-Liste:

| Tastenkombination | Verwendeter Eintrag |
|---|---:|
| `Ctrl+1` | erster Eintrag |
| `Ctrl+2` | zweiter Eintrag |
| … | … |
| `Ctrl+9` | neunter Eintrag |
| `Ctrl+0` | zehnter Eintrag |

Die Tastenkombinationen können auch einer programmierbaren Makro-Tastatur zugewiesen werden. Die Idee zu dieser Bedienung stammt von IU3OAR, Gianluca Costantino.

KST4Contest legt keine verbindliche Liste von Standard-Snippets fest. Welche Texte sinnvoll sind, hängt vom eigenen Contestbetrieb und der verwendeten Betriebsart ab.

Bearbeitung, Sortierung und Speicherung sind unter [Konfiguration – Snippet Settings](de-Konfiguration#snippet-settings-text-snippets) beschrieben.

---

## Variablen

Variablen sind reservierte Platzhalter innerhalb eines Nachrichtentextes. Sie müssen in Großbuchstaben geschrieben werden und unterscheiden zwischen Groß- und Kleinschreibung.

Variablen können verwendet werden in:

- Shortcuts,
- Snippets,
- Beacon-Texten und
- direkt eingegebenen oder eingefügten Nachrichtentexten.

Bei einem Shortcut oder Snippet werden die Variablen bereits beim Einfügen in das Sendfeld aufgelöst. Direkt in das Sendfeld geschriebene oder eingefügte Variablen werden unmittelbar vor der Übernahme in die Sendewarteschlange aufgelöst.

Stationsbezogene Variablen verwenden immer die aktuell ausgewählte Station. KST4Contest leitet diese Station nicht aus einem von Hand in den Nachrichtentext geschriebenen `/cq`-Empfänger ab.

---

## Globale Variablen

Globale Variablen benötigen keine ausgewählte Gegenstation.

| Variable | Ersetzter Wert |
|---|---|
| `MYQRG` | aktuelle QRG der ersten beziehungsweise primären Chat-Kategorie |
| `MYQRGSHORT` | erste sieben Zeichen von `MYQRG` |
| `SECONDQRG` | aktuelle QRG der zweiten Chat-Kategorie |
| `MYLOCATOR` | vollständiger Locator der eigenen Station |
| `MYLOCATORSHORT` | erste vier Zeichen des eigenen Locators |
| `MYCALL` | konfiguriertes eigenes Rufzeichen |
| `MYQTF` | aktuelle Antennenrichtung als numerischer Wert in Grad |

Beispiel:

```text
cq at MYQRGSHORT, qtf MYQTF, loc MYLOCATOR
```

kann aufgelöst werden zu:

```text
cq at 144.388, qtf 135, loc JO51IJ
```

### QRG-Variablen

`MYQRG` enthält die QRG der ersten Chat-Kategorie. Der Wert kann aus der TRX-Synchronisation des Logprogramms oder aus dem manuell bearbeiteten QRG-Feld stammen.

`MYQRGSHORT` verwendet denselben Wert, beschränkt ihn aber auf die ersten sieben Zeichen:

```text
144.388.03 → 144.388
```

`SECONDQRG` enthält die QRG der zweiten Chat-Kategorie. Die Auswahl einer Station aus dem zweiten Chat verändert die Bedeutung von `MYQRG` nicht. Soll ausdrücklich die QRG der zweiten Kategorie eingesetzt werden, muss `SECONDQRG` verwendet werden.

### Locator-Variablen

`MYLOCATOR` übernimmt den vollständigen konfigurierten Locator der eigenen Station:

```text
JO51IJ
```

`MYLOCATORSHORT` verwendet nur die ersten vier Zeichen:

```text
JO51
```

### MYQTF

`MYQTF` übernimmt die aktuelle, in KST4Contest hinterlegte Antennenrichtung als numerischen Winkel in Grad.

Beispiel:

```text
ant MYQTF deg
```

kann werden zu:

```text
ant 135 deg
```

Die Richtung wird nicht in Himmelsrichtungen wie `north`, `north-east` oder `south-west` umgewandelt.

---

## Variablen für die ausgewählte Station

Diese Variablen benötigen eine ausgewählte Gegenstation:

| Variable | Ersetzter Wert |
|---|---|
| `QRZNAME` | Name der ausgewählten Station oder deren vollständiges Rufzeichen, wenn kein Name verfügbar ist |
| `FIRSTAP` | Beschreibung und Ankunftszeit des ersten von AirScout gemeldeten Flugzeugs |
| `SECONDAP` | Beschreibung und Ankunftszeit des zweiten von AirScout gemeldeten Flugzeugs |

Beispiel:

```text
Hi QRZNAME, FIRSTAP, pse lsn at MYQRGSHORT
```

kann werden zu:

```text
Hi David, a very big AP in 2 min, pse lsn at 144.388
```

### QRZNAME

KST4Contest verwendet den Namen aus dem Namensfeld der ausgewählten Station. Ist dort kein verwendbarer Name vorhanden, wird stattdessen das vollständige sichtbare Rufzeichen eingesetzt.

### FIRSTAP

Ist ein AirScout-Kandidat verfügbar, enthält `FIRSTAP` dessen Beschreibung und die voraussichtliche Zeit bis zum Reflexionsfenster.

Beispiel:

```text
a very big AP in 2 min
```

Ist für die ausgewählte Station kein Flugzeug verfügbar, wird eingesetzt:

```text
no ap available
```

### SECONDAP

`SECONDAP` verwendet den zweiten verfügbaren AirScout-Kandidaten.

Beispiel:

```text
Next big AP in 9 min
```

Ist kein zweiter Kandidat vorhanden, wird `SECONDAP` durch einen leeren Text ersetzt.

### Verhalten ohne ausgewählte Station

Ist keine Station ausgewählt, bleiben `QRZNAME`, `FIRSTAP` und `SECONDAP` im Text sichtbar. KST4Contest entfernt diese Platzhalter nicht automatisch.

Ein sichtbarer, nicht aufgelöster Platzhalter ist eindeutiger als eine formal vollständige Nachricht, in der unbemerkt eine wichtige Information fehlt. Vor dem Senden sollte deshalb geprüft werden, ob die richtige Station ausgewählt ist und alle benötigten Variablen aufgelöst wurden.

---

## Variablen im Beacon

Ein öffentlicher Beacon besitzt keine ausgewählte Gegenstation. Deshalb können dort ausschließlich globale Variablen verwendet werden:

- `MYQRG`
- `MYQRGSHORT`
- `SECONDQRG`
- `MYLOCATOR`
- `MYLOCATORSHORT`
- `MYCALL`
- `MYQTF`

`QRZNAME`, `FIRSTAP` und `SECONDAP` dürfen in einem Beacon nicht verwendet werden.

Eine mögliche Vorlage für die erste Chat-Kategorie ist:

```text
calling cq at MYQRGSHORT, ant MYQTF deg, loc MYLOCATOR
```

Verwendet die zweite Chat-Kategorie eine andere QRG, muss deren Vorlage `SECONDQRG` enthalten:

```text
calling cq at SECONDQRG, ant MYQTF deg, loc MYLOCATOR
```

Die globalen Variablen werden bei jedem Timer-Lauf erneut ausgewertet. `MYQRG` und `MYQRGSHORT` benötigen immer eine gültige QRG der Hauptkategorie, `SECONDQRG` immer eine gültige QRG der zweiten Kategorie – unabhängig davon, in welcher Beacon-Vorlage sie stehen. Fehlt eine benötigte QRG, lässt sich nur der betroffene Beacon nicht aktivieren beziehungsweise wird ausgeschaltet. Eine Vorlage ohne QRG-Variable darf auch ohne vorhandene QRG aktiviert werden.

Beim Einschalten merkt sich jeder Beacon die QRG seiner eigenen Kategorie. Eine manuelle QRG-Änderung verschiebt diese Referenz. Ändert UCXLog oder Win-Test die zugeordnete QRG später um mehr als 5 kHz, schaltet KST4Contest den betreffenden Beacon automatisch aus, statt die neue Frequenz unbemerkt in die nächste Aussendung zu übernehmen. Ist beim Einschalten noch keine eigene QRG vorhanden, wird die erste später gültig eingegebene oder empfangene Frequenz zur Referenz.

Der vollständig aufgelöste Beacon-Inhalt vor dem festen Suffix ` *a*`:

- muss mindestens ein gültiges Zeichen enthalten,
- darf höchstens 120 Zeichen lang sein,
- darf das Protokoll-Trennzeichen `|` nicht enthalten und
- darf keine Zeilenumbrüche enthalten.

KST4Contest hängt das Suffix anschließend automatisch an. Die tatsächlich gesendete Beacon-Nachricht kann deshalb bis zu 124 Zeichen lang sein.

Ist der aufgelöste Text beim vorgesehenen Versand aus einem anderen Grund leer oder ungültig, wird der betreffende Beacon-Lauf ausgelassen und der Status-Chip zeigt **FAILED**.

Intervall, Aktivierung und Verhalten beider Kategorien sind unter [Konfiguration – Beacon Settings](de-Konfiguration#beacon-settings-automatischer-beacon) beschrieben.

---

## Beispiel für einen Snippet-Workflow

Als erstes Snippet ist beispielsweise konfiguriert:

```text
Hi QRZNAME, pse sked? I call at MYQRGSHORT
```

Der Ablauf kann dann folgendermaßen aussehen:

1. In der Benutzerliste wird `DL1ABC-432` ausgewählt.
2. `Ctrl+1` wird gedrückt.
3. KST4Contest bereitet die adressierte Nachricht vor und löst die Variablen auf.
4. Der vollständige Text wird im Sendfeld geprüft.
5. Falls die Gegenstation eine andere QRG vorgeschlagen hat, wird der Text entsprechend angepasst.
6. `Enter` oder **TX** sendet die Nachricht.

Das Ergebnis kann beispielsweise lauten:

```text
/cq DL1ABC-432 Hi Peter, pse sked? I call at 432.088
```

Das vollständige Rufzeichen bestimmt den Empfänger. Die ausgewählte Chat-Kategorie bestimmt den Versandweg. Die Variablen verringern die wiederholte Texteingabe, entscheiden aber nicht, ob die eingesetzten Informationen noch zur aktuellen Betriebssituation passen.

---

## Grenzen der Variablenauflösung

Variablen geben den Informationsstand wieder, den KST4Contest im Moment der Auflösung besitzt.

Dabei ist insbesondere zu beachten:

- Eine vom Logprogramm gelieferte QRG kann sich inzwischen geändert haben.
- Eine manuell eingetragene QRG bleibt aktiv, bis sie erneut geändert wird.
- `MYQRG` bleibt die QRG der primären Kategorie, auch wenn eine Station aus der zweiten Kategorie ausgewählt wurde.
- Die ausgewählte Station kann von einem manuell eingegebenen `/cq`-Empfänger abweichen.
- AirScout kann für die betreffende Strecke keine aktuellen Flugzeugdaten liefern.
- Stationsbezogene Variablen bleiben sichtbar, wenn keine Station ausgewählt ist.
- Der eingefügte Text wird nicht automatisch auf seine betriebliche Richtigkeit geprüft.

Das Sendfeld bleibt deshalb nach dem Einfügen eines Shortcuts oder Snippets bearbeitbar. Die Variablen vermeiden wiederholte Eingaben; die abschließende Prüfung bleibt beim Operator.

---

## Verwendung im Contest

Shortcuts, Snippets und Variablen sind einzelne Werkzeuge innerhalb des laufenden Betriebs. Ihr Zusammenspiel mit CQ-Betrieb, Stationsauswahl, Skeds, QRG-Wechseln und Log-Synchronisation ist unter [Contest-Workflow mit KST4Contest](de-Contest-Workflow) beschrieben.
