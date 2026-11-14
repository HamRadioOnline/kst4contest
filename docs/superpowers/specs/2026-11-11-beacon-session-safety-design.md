# Beacon Session Safety Design

Freigegeben von Marc am 2026-11-11.

## Ziel

Die Beacon-Funktion darf nach einem Programmstart oder Profilwechsel nicht aus einer
gespeicherten Aktivierung weiterlaufen. Jeder Beacon wird in der laufenden Sitzung bewusst
eingeschaltet, hält seine eigene Kategorie-QRG als Referenz fest und schaltet nur bei einer
automatisch von UCXLog oder Win-Test gemeldeten Abweichung von mehr als 5 kHz ab.

Der zusammengefasste `MyBeacon`-Chip macht Aktivierung, Teilaktivierung, automatische
Abschaltung und Versandfehler ohne Umweg über das Einstellungsfenster erkennbar.

## Verbindliches Verhalten

### Intervall und Sitzung

- Das kleinste zulässige gemeinsame Beacon-Intervall beträgt fünf Minuten.
- Genau fünf Minuten sind zulässig. Kleinere geladene Werte werden im Speicher auf fünf
  Minuten normalisiert; die mitgelieferte Konfiguration verwendet für beide Kategorien fünf.
- Nach jedem Programmstart und Profilwechsel sind beide Beacons ausgeschaltet.
- Ein Disconnect oder Reconnect innerhalb derselben Programmsitzung verändert ihre
  Aktivierung nicht.
- Die Aktivierungsflags werden nicht aus XML gelesen. Die vorhandenen XML-Elemente bleiben
  für die Dateikompatibilität erhalten, werden aber immer mit `false` geschrieben.

### Aktivierung und benötigte QRG-Werte

- `MYQRG` und `MYQRGSHORT` benötigen eine gültige Haupt-QRG.
- `SECONDQRG` benötigt eine gültige zweite QRG.
- Diese Voraussetzung gilt unabhängig davon, in welchem der beiden Bakentexte die Variable
  vorkommt.
- Fehlt ein benötigter Wert oder ist er ungültig, bleibt der betroffene Beacon aus. Sein
  kategorieweiser Status nennt die fehlende oder ungültige QRG als Grund.
- Der gemeinsame Chip aktiviert beide nutzbaren Kategorien unabhängig. Ein gültiger Beacon
  wird eingeschaltet, auch wenn der andere abgelehnt wird; der Chip zeigt dann `1/2`.
- Ein Text ohne QRG-Platzhalter darf ohne bekannte QRG aktiviert werden.
- Geprüft wird der bereits angewendete Text, nicht ein noch offener Settings-Entwurf.

### Kategoriebezogene Referenz-QRG

- Beim Einschalten übernimmt jeder Beacon die aktuell gültige QRG seiner eigenen Kategorie
  als Referenz.
- Ist keine gültige Kategorie-QRG vorhanden und benötigt der Text sie nicht, wird die erste
  später gültige manuelle oder externe QRG zur Referenz.
- Eine gültige manuelle QRG-Eingabe ist eine bewusste Operatorentscheidung. Sie deaktiviert
  keinen Beacon und verschiebt die Referenz der zugehörigen Kategorie.
- Eine ungültige manuelle Eingabe deaktiviert jeden aktiven Beacon, dessen angewendeter Text
  den betroffenen QRG-Wert benötigt. Benötigt sein Text ihn nicht, bleibt der Beacon aktiv;
  nur die Referenz seiner Kategorie wird gelöscht.
- Ausschließlich QRG-Übernahmen von UCXLog `RadioInfo` und Win-Test `STATUS`, für die der
  bestehende QRG-Abgleich aktiv ist, dürfen automatisch abschalten.
- Der Vergleich erfolgt in ganzzahligen Hz gegen die aktuelle Referenz. Genau 5.000 Hz sind
  zulässig; erst eine größere Abweichung schaltet ab.
- Ohne Referenz setzt die erste externe QRG nur die Referenz und schaltet nicht ab.
- Ohne SO2R betrifft eine externe Änderung von `MYQRG` ausschließlich die Hauptbake. Mit
  SO2R folgt Radio 1 `MYQRG` und Radio 2 `SECONDQRG`.

### Status und Tooltip

- Der Status wird als strukturierter Snapshot aus `core` geliefert. Übersetzte Anzeigetexte
  entstehen ausschließlich in `app-desktop`.
- Der Snapshot kennt `OFF`, `PARTIAL`, `ON` und `FAILED` sowie den Zustand beider Kategorien.
- Der Tooltip zeigt je Kategorie den aktuellen Zustand oder letzten Abschalt-/Ablehnungsgrund:
  noch nicht in dieser Sitzung aktiviert, durch den Benutzer ausgeschaltet, fehlende oder
  ungültige Haupt-QRG, fehlende oder ungültige zweite QRG oder automatische QRG-Abweichung
  von mehr als 5 kHz.
- Der QRG-Grund nennt keine konkreten Frequenzen.
- Timeraktivität überschreibt den kategorieweisen Grund nicht.
- Ein erfolgreicher späterer Lauf löscht einen früheren Versandfehler der Kategorie.

### Verkehrsfarben des `MyBeacon`-Chips

- `on` und `1/2`: Hintergrund `#238636`, Rand `#56D364`, weiße Schrift.
- Der bestehende 200-ms-Aktivitätsblitz verwendet Hintergrund `#FFB300`, Rand `#FFE082`
  und Schrift `#1B1B1B`.
- `off`: neutraler Theme-Hintergrund mit `#D50000` im hellen beziehungsweise `#FF6B6B` im
  dunklen Design.
- `FAILED`: dauerhaft `#D50000` mit Rand `#FF6B6B` und weißer Schrift.
- `FAILED` unterdrückt den orangefarbenen Aktivitätsblitz.
- Andere Status-Chips behalten ihr heutiges Theme-Verhalten.

## Technischer Ansatz

Eine controllerseitige `BeaconSessionState`-Komponente koordiniert den Zustand beider
Kategorien. Die beobachtbaren Flags in `ChatPreferences` bleiben kompatible UI-Projektion,
aber kein persistenter Produktzustand. Die Komponente verwaltet Referenzen, kategorieweise
Gründe und Versandfehler und ist der einzige Produktionsschreibpfad der Aktivierungsflags.

Manuelle QRG-Eingaben und externe Logger-Meldungen verwenden getrennte Controller-Einstiege.
UCXLog und Win-Test übergeben die bereits ermittelte Kategorie, den formatierten Anzeigewert
und die genaue Frequenz in Hz innerhalb der bestehenden `UiDispatcher`-Grenze. Damit ist die
Herkunft explizit; Listener, Thread-lokale Markierungen oder erratene Ursprünge sind unnötig.

Der vorhandene `BeaconTask` bleibt für Auflösung, Validierung und Einreihung in die reguläre
TX-Warteschlange verantwortlich. Er meldet je Kategorie Erfolg oder Fehler an den
Sitzungszustand zurück. Ein DTO im gemeinsamen Model transportiert den strukturierten Status
über den bestehenden Threadstatus-Kanal; Compose übersetzt Gründe und wählt die Chipfarben.

## Unveränderte Grenzen

- Beide Kategorien teilen einen Timer und besitzen getrennte Aktivierung und Texte.
- Der zweite Versand setzt weiterhin eine verfügbare zweite Chatkategorie voraus.
- Aktivieren sendet nicht sofort und startet den Timer nicht neu.
- Der erste Timerlauf nach dem Verbindungsaufbau bleibt bei ungefähr zehn Sekunden.
- Nachrichtenauflösung, Längenlimit, TX-Warteschlange und ON4KST-Framing bleiben unverändert.
- Die Chip-Texte `off`, `1/2`, `on` und `FAILED` und seine gemeinsame Klickfunktion bleiben.
- Es gibt keine neue Produktionsabhängigkeit, kein neues persistentes Schema und keine
  Änderung eines externen Protokolls.

## Berücksichtigte Ansätze

1. **Gewählt:** controllerseitiger Sitzungszustand mit expliziten manuellen und externen
   Ereignissen. Das hält Policy, Referenzen und Gründe zusammen und bleibt testbar.
2. **Verworfen:** Flags in Preferences, Referenzen in einem Guard und Gründe in `BeaconStatus`.
   Der zusammengehörige Zustand wäre auf mehrere Besitzer verteilt.
3. **Verworfen:** Beobachtung der QRG-Werte mit temporärer Herkunftsmarkierung. `MutableValue`
   trägt keine Quelle; die asynchrone UI-Weitergabe machte diesen Ansatz fragil.

## Dokumentationswirkung

Die Änderung ist benutzerseitig sichtbar, betrifft Laufzeit/Persistenz sowie die bestehende
UCXLog-/Win-Test-Integration. Geprüft und bei Bedarf angepasst werden die relevanten
deutschen und englischen Konfigurations-, Workflow-, Variablen-, Funktions- und
Benutzeroberflächenabschnitte, Changelog, Website-Texte, `docs/PROJECT_CONTEXT.md` und der
ältere Live-Beacon-Entwurf. README und verbundene Projekte sind voraussichtlich nicht
betroffen.
