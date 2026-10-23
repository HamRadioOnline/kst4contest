# KST4Contest – Handbuch

> [English version](en-Home) | Du liest gerade die deutsche Version

KST4Contest ist ein Desktop-Client für den [ON4KST-Chat](https://www.on4kst.org/chat/login.php), der für den Contest-Betrieb auf den VHF-, UHF- und SHF-Bändern entwickelt wurde. Er verbindet Chat, Stationsauswahl, Sked-Planung, Aircraft-Scatter-Daten und die Anbindung an weitere Programme in einer gemeinsamen Arbeitsoberfläche.

Entwickelt wird KST4Contest von **DO5AMF (Marc Fröhlich)**, Operator bei DM5M und (seit Mai 2025) **DN9APW (Philipp Wagner)**. Der Quellcode ist öffentlich auf [GitHub](https://github.com/HamRadioOnline/kst4contest) verfügbar.

---

## Wozu braucht man einen eigenen ON4KST-Client?

Der ON4KST-Chat liefert während eines Contests eine große Menge an Informationen: aktive Stationen, Locator, Frequenzen, Sked-Anfragen und Hinweise auf aktuelle Aktivität. Das eigentliche Problem besteht nicht darin, diese Daten zu sehen. Man muss daraus rechtzeitig die nächste sinnvolle Verbindung ableiten.

KST4Contest wertet die verfügbaren Informationen aus, ordnet sie und stellt sie in einem contestgerechten Arbeitsablauf dar. Dabei werden unter anderem Antennenrichtung, Entfernung, bekannte Bänder und Frequenzen, bereits gearbeitete Stationen, Chat-Aktivität und Aircraft-Scatter-Zeiten berücksichtigt.

Das Programm entscheidet jedoch nicht, welches QSO tatsächlich möglich ist. Der Prioritätsscore, die AP-Timeline und die verschiedenen Hervorhebungen sind Entscheidungshilfen. Die endgültige Beurteilung bleibt beim Operator – schon deshalb, weil auch ein recht überzeugender Computerbildschirm noch keine Funkverbindung herstellt.

---

## Was KST4Contest im Contest unterstützt

- **Chat beobachten und einordnen:** KST4Contest kann zwei ON4KST-Chat-Kategorien gleichzeitig darstellen. Nachrichten, Frequenzangaben und bekannte Bandaktivitäten werden den jeweiligen Stationen zugeordnet.

- **Relevante Stationen herausfiltern:** Richtungs-, Entfernungs-, Worked- und NOT-QRV-Filter reduzieren die Benutzerliste auf die Stationen, die für den aktuellen Betriebszustand tatsächlich interessant sind.

- **Kandidaten priorisieren:** Das Score-System bewertet aktive Chatmember anhand mehrerer bekannter Kriterien. Die derzeit relevantesten Kandidaten erscheinen zusätzlich in einer eigenen Prioritätsliste.

- **Skeds vorbereiten und im Blick behalten:** Sked-Erinnerungen, automatische Vorwarnungen und die AP-Timeline helfen dabei, vereinbarte Verbindungen nicht zwischen Chat, Log und laufendem CQ-Betrieb zu verlieren.

- **Aircraft Scatter einbeziehen:** Über die AirScout-Schnittstelle werden geeignete Flugzeuge und erwartete Reflexionszeiten in die Stationsbewertung und Zeitplanung übernommen.

- **Log und Funkstation anbinden:** KST4Contest synchronisiert gearbeitete Stationen und Frequenzinformationen mit unterstützten Logprogrammen. Zusätzlich stehen Schnittstellen zu Win-Test, PSTRotator und ein integrierter DX-Cluster-Server zur Verfügung.

- **Stationen und Funkwege darstellen:** Die Stationskarte zeigt aktive Chatmember, Locator-Felder, Antennenrichtungen und den Weg zur ausgewählten Station. Für ausgewählte Verbindungen kann außerdem ein Geländeprofil berechnet werden.

Die Funktionen greifen ineinander. Eine erkannte Frequenz kann beispielsweise einem aktiven Band zugeordnet werden, der Worked-Status stammt aus dem Log, Aircraft-Scatter-Daten ergänzen die zeitliche Bewertung und der daraus berechnete Score beeinflusst die Prioritätsliste. Fehlende oder veraltete Eingangsdaten können deshalb auch das Ergebnis beeinflussen.

---

## Voraussetzungen

Für die Anmeldung ist ein registrierter ON4KST-Account erforderlich. Registrierung und Login sind über die [offizielle ON4KST-Anmeldeseite](https://www.on4kst.org/chat/login.php) erreichbar.

Die offizielle Sprache im ON4KST-Chat ist Englisch. Das gilt auch für Nachrichten an Stationen aus dem eigenen Land. Übliche Amateurfunk-Abkürzungen wie `pse`, `agn`, `qrg`, `dir`, `rrr`, `tnx` oder `73` sind dabei normal und meistens erheblich schneller als ausformulierte Prosa.

Download, unterstützte Betriebssysteme und Installationswege sind im Kapitel [Installation](de-Installation) beschrieben.

---

## Versionsstand dieses Handbuchs

Dieses Handbuch unterscheidet zwischen der veröffentlichten Stable-Version und dem aktuellen Entwicklungsstand.

Die derzeit veröffentlichte Stable-Version ist **v1.42.0**. Funktionen oder Änderungen, die erst mit einer bestimmten Version hinzugekommen sind, werden ausdrücklich als **ab v1.42** gekennzeichnet. Funktionen, die es nur im aktuellen Entwicklungsstand gibt, sind als **Nightly** gekennzeichnet. Fehlt eine solche Kennzeichnung, bezieht sich die Beschreibung auf die Stable-Version.

- [Stable, Beta und Nightly herunterladen](https://kst4contest.hamradioonline.de/download/)
- [Veröffentlichte GitHub Releases](https://github.com/HamRadioOnline/kst4contest/releases)
- [Versionsgeschichte und aktueller Nightly-Stand](de-Changelog)

Für einen Contest ist grundsätzlich die Stable-Version zu empfehlen. Nightly-Builds enthalten neuere Korrekturen und Funktionen, können sich aber zwischen zwei Builds verändern. Sie sind sinnvoll, wenn eine konkrete Änderung getestet werden soll. Der erste Versuch zehn Minuten vor Contestbeginn ist dagegen meistens eine recht effiziente Methode, gleichzeitig Software und Operator zu testen.

---

## Schnellnavigation

| Seite | Inhalt |
|---|---|
| [Installation](de-Installation) | ON4KST-Account, Download, Installation und Updates |
| [Konfiguration](de-Konfiguration) | Login, Station, Bänder, Benutzeroberfläche und externe Schnittstellen |
| [Contest-Workflow](de-Contest-Workflow) | Vorstartprüfung, CQ-Betrieb, Kandidatenauswahl, Skeds, Logeintrag und Bandwechsel |
| [Log-Synchronisation](de-Log-Synchronisation) | Simplelogfile, UCXLog, N1MM+, QARTest, DXLog.net und Win-Test |
| [AirScout-Integration](de-AirScout-Integration) | Verbindung zu AirScout und Auswertung von Aircraft-Scatter-Zeiten |
| [DX-Cluster-Server](de-DX-Cluster-Server) | Übergabe erkannter Möglichkeiten an das Logprogramm |
| [Funktionen](de-Funktionen) | Arbeitsweise, Herleitung und Grenzen der einzelnen Funktionen |
| [Makros und Variablen](de-Makros-und-Variablen) | Wiederverwendbare Texte, Shortcuts und automatisch ersetzte Werte |
| [Benutzeroberfläche](de-Benutzeroberflaeche) | Aufbau der Oberfläche und Bedienung im Contest |
| [Changelog](de-Changelog) | Releases, Nightly-Änderungen und behobene Fehler |

---

## Kontakt und Support

- **Download:** [Stable, Beta und Nightly](https://kst4contest.hamradioonline.de/download/)
- **Quellcode:** [HamRadioOnline/kst4contest](https://github.com/HamRadioOnline/kst4contest)
- **Fehler und Funktionswünsche:** [GitHub Issues](https://github.com/HamRadioOnline/kst4contest/issues)
- **E-Mail:** praktimarc+kst4contest@gmail.com  
  Bitte nur für Themen verwenden, die KST4Contest betreffen.

### Einen Fehler melden

Ein Fehler lässt sich wesentlich schneller nachvollziehen, wenn die Meldung mindestens folgende Angaben enthält:

1. verwendete KST4Contest-Version,
2. Betriebssystem und Installationsart,
3. genaue Schritte bis zum Fehler,
4. erwartetes und tatsächlich beobachtetes Verhalten,
5. gegebenenfalls einen Screenshot,
6. die Fehler-Logdatei.

Die Fehler-Logdatei wird hier gespeichert:

| Betriebssystem | Pfad |
|---|---|
| Linux / macOS | `~/.praktiKST/debug/kst4contest-errors.log` |
| Windows | `C:\Users\<Benutzername>\.praktiKST\debug\kst4contest-errors.log` |

Vor dem Hochladen sollte die Datei kurz geprüft werden. Ein Fehlerprotokoll enthält überwiegend technische Informationen, kann abhängig vom Fehler aber beispielsweise lokale Dateipfade oder weitere Kontextdaten enthalten.

Ist unter **Settings → Messagehandling → Enable debug mode to file** der Debug-Modus aktiviert, enthält das Verzeichnis `debug` zusätzlich `Messagehistory.raw` und gegebenenfalls archivierte `Messagehistory-*.raw`. Diese Dateien zeichnen den vollständigen ON4KST-Sessionverkehr mit RX und TX auf. Loginpasswörter werden redigiert, Chat- und Stationsdaten bleiben jedoch enthalten. Eine solche Datei deshalb nur nach bewusster Prüfung weitergeben.

In Datei- und Verzeichnisnamen wird teilweise noch der technische Name `praktiKST` verwendet. Gemeint ist dasselbe Programm.

---

## Danksagungen

KST4Contest wurde durch Rückmeldungen aus dem praktischen Contest-Betrieb wesentlich verbessert. Viele Funktionen entstanden nicht aus einer theoretischen Anforderungsliste, sondern aus Situationen, in denen während eines Contests eine Information fehlte, zu spät sichtbar wurde oder schlicht an der falschen Stelle stand.

Besonderer Dank gilt:

- Gianluca Costantino (IU3OAR),
- Alessandro Murador (IZ3VTH),
- Reczetár István (HA1FV),
- Viliam Petrik (OM0AAO) für die Idee zum integrierten DX-Cluster,
- Konrad Neitzel (DC9DJ) für Unterstützung bei der Projektstruktur,
- Andreas (DO5ALF), Webmaster von funkerportal.de,
- Franz van Velzen (PE0WGA) für Tests sowie
- DN9APW (Philipp Wagner), der seit Mai 2026 an der Entwicklung und den CI/CD-Pipelines mitarbeitet.

Ebenso wichtig sind die Fehlermeldungen, Tests und konkreten Betriebserfahrungen aller weiteren Nutzer. Eine genaue Beschreibung eines reproduzierbaren Problems hilft dem Projekt meistens mehr als ein allgemeines „läuft gut“ – auch wenn Letzteres natürlich angenehmer zu lesen ist.
