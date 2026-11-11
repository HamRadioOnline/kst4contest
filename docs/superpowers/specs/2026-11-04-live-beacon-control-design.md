# Live Beacon Control Design

Freigegeben von Marc am 2026-11-04.

> **Teilweise abgelöst durch Issue #171 (2026-11-11):** Die Beacon-Zustände werden nicht mehr
> gespeichert und nicht mehr beim Profilstart geladen; jede Sitzung beginnt mit „aus“. Das
> Mindestintervall beträgt fünf Minuten, und eine TRX-Frequenzänderung über 2 kHz schaltet die
> Beacon ab. Der aktuelle Stand steht in `docs/PROJECT_CONTEXT.md` unter „Autoanswer / Beacon“.
> Alles Übrige in diesem Dokument gilt weiter.

## Ziel

Die beiden Beacon-Aktivierungsschalter wirken in der laufenden Sitzung sofort. Der gemeinsame
Beacon-Timer bleibt bestehen und liest bei jedem Lauf die aktuellen Zustände. Der Beacon-Chip
im Hauptfenster zeigt den zusammengefassten Zustand und kann die aktuell nutzbaren Beacons
gemeinsam ein- oder ausschalten.

## Verbindliches Verhalten

- Die gespeicherten Beacon-Zustände werden beim Profilstart geladen.
- Ein Klick auf eine Beacon-Checkbox wirkt sofort im Speicher und damit beim nächsten
  regulären Timerlauf.
- Aktivieren sendet nicht sofort und startet den Timer nicht neu.
- Erst **Einstellungen speichern** schreibt die Zustände in die Profil-XML. Ohne explizites
  Speichern gilt die Änderung nur für die laufende Sitzung.
- Beacon-Text und Intervall bleiben normale Settings-Entwurfswerte. Das Intervall startet den
  Timer erst beim Anwenden neu.
- Der Status-Chip zeigt `off`, `1/2` oder `on` für die aktuell nutzbaren Kategorien.
- Sind alle nutzbaren Beacons aus, aktiviert ein Chip-Klick alle nutzbaren Beacons. Ist
  mindestens eine nutzbare Beacon an, deaktiviert der Klick alle nutzbaren Beacons.
- Bei deaktiviertem zweiten Chat schaltet der Chip nur die Hauptkategorie und verändert das
  gespeicherte Flag der zweiten Kategorie nicht.
- Chip und geöffnetes Settings-Fenster zeigen denselben beobachtbaren Zustand.
- Nur der Beacon-Chip wird anklickbar. Andere Thread-Status-Chips bleiben reine Anzeigen.

## Unveränderte Grenzen

- Beide Kategorien behalten eigene Aktivierungsflags und Texte.
- Der zweite Beacon-Versand setzt weiterhin einen aktivierten zweiten Chat voraus.
- Nachrichtenvalidierung, TX-Warteschlange und ON4KST-Protokoll bleiben unverändert.
- Mindestintervall, gemeinsamer Timer und Intervall-Persistenz bleiben unverändert.
- Profilzustände werden nicht miteinander vermischt.

## Technischer Ansatz

Die vorhandenen `ChatPreferences`-Getter und -Setter bleiben die kompatible Schnittstelle,
werden für die beiden Aktivierungsflags aber auf thread-sichere beobachtbare Werte gestellt.
Der Settings-Tab liest diese Werte als Compose-State und schreibt Aktivierungen direkt durch
den Controller. Der Controller veröffentlicht nach jeder manuellen Umschaltung einen
zusammengefassten `MyBeacon`-Status. `BeaconTask` meldet nach einem Timerlauf ebenfalls genau
diesen zusammengefassten Status. `ThreadStatusButtons` erhält eine optionale Aktion pro
Quelle; die Anwendung registriert sie nur für `MyBeacon`.
