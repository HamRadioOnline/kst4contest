# Live Beacon Control Design

Freigegeben von Marc am 2026-11-04.

> Durch das freigegebene [Beacon Session Safety Design](2026-11-11-beacon-session-safety-design.md)
> abgelöst, soweit Aktivierungspersistenz, Mindestintervall, QRG-Sicherung und Chip-Darstellung
> betroffen sind.

## Ziel

Die beiden Beacon-Aktivierungsschalter wirken in der laufenden Sitzung sofort. Der gemeinsame
Beacon-Timer bleibt bestehen und liest bei jedem Lauf die aktuellen Zustände. Der Beacon-Chip
im Hauptfenster zeigt den zusammengefassten Zustand und kann die aktuell nutzbaren Beacons
gemeinsam ein- oder ausschalten.

## Verbindliches Verhalten

- Nach einem Programmstart und Profilwechsel sind beide Beacon-Zustände ausgeschaltet.
- Ein normaler ON4KST-Reconnect innerhalb derselben Programmsitzung verändert sie nicht.
- Ein Klick auf eine Beacon-Checkbox wirkt sofort im Speicher und damit beim nächsten
  regulären Timerlauf.
- Aktivieren sendet nicht sofort und startet den Timer nicht neu.
- Aktivierungszustände werden nicht in die Profil-XML übernommen; ein vollständiger
  Einstellungsspeichervorgang schreibt die kompatiblen Aktivierungs-Tags als `false`.
- Beacon-Text und Intervall bleiben normale Settings-Entwurfswerte. Das Intervall startet den
  Timer erst beim Anwenden neu.
- Der Status-Chip zeigt `off`, `1/2`, `on` oder `FAILED` für die aktuell nutzbaren Kategorien.
- Sind alle nutzbaren Beacons aus, aktiviert ein Chip-Klick alle nutzbaren Beacons. Ist
  mindestens eine nutzbare Beacon an, deaktiviert der Klick alle nutzbaren Beacons.
- Bei deaktiviertem zweiten Chat schaltet der Chip nur die Hauptkategorie.
- Chip und geöffnetes Settings-Fenster zeigen denselben beobachtbaren Zustand.
- Nur der Beacon-Chip wird anklickbar. Andere Thread-Status-Chips bleiben reine Anzeigen.

## Unveränderte Grenzen

- Beide Kategorien behalten eigene Aktivierungsflags und Texte.
- Der zweite Beacon-Versand setzt weiterhin einen aktivierten zweiten Chat voraus.
- Nachrichtenvalidierung, TX-Warteschlange und ON4KST-Protokoll bleiben unverändert.
- Gemeinsamer Timer und Intervall-Persistenz bleiben unverändert; das Mindestintervall beträgt
  fünf Minuten.
- Profilzustände werden nicht miteinander vermischt.

## Technischer Ansatz

`BeaconSessionState` ist die maßgebliche sitzungsbezogene Quelle für Aktivierung,
QRG-Referenzen, Abschaltgründe und Fehler. Die vorhandenen beobachtbaren
`ChatPreferences`-Werte bleiben die kompatible Projektion für Settings und UI. Controller und
`BeaconTask` veröffentlichen einen strukturierten `BeaconStatusSnapshot`, aus dem Chip-Text,
Tooltip und die Beacon-spezifische Farbgebung entstehen. Details der QRG-Prüfung und
Darstellung stehen im ablösenden Design vom 2026-11-11.
