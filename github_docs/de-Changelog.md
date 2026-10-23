# Changelog

> 🇬🇧 [English version](en-Changelog) | 🇩🇪 Du liest gerade die deutsche Version

Versionsverlauf von KST4Contest / PraktiKST.

Die veröffentlichten Stable-Versionen und ihre Programmpakete stehen unter [GitHub Releases](https://github.com/HamRadioOnline/kst4contest/releases). Zusätzlich enthält diese Seite die Änderungen des aktuellen Entwicklungsstands, soweit sie bereits implementiert und geprüft wurden.

---

## v1.50 (in Entwicklung)

**Operator-Profile**

Mehrere Operateure an einem Rechner können jetzt eigene Rufzeichen, Locators und Layouts verwenden, ohne sich gegenseitig die Konfiguration zu überschreiben. Damit ist [Issue #57](https://github.com/HamRadioOnline/kst4contest/issues/57) umgesetzt.

### Neu

- **Neue Oberfläche:** Die Oberfläche ist von JavaFX auf Compose Multiplatform umgestellt. Alle Fenster — Hauptfenster, Stationskarte, Einstellungen, Monitor, Update-Fenster und Dialoge — sind neu aufgebaut; Bedienung und Anordnung bleiben, wie sie waren. Das Programm braucht keine JavaFX-Laufzeit mehr.

- **Deutsch und Englisch:** Die Oberfläche gibt es auf Deutsch und Englisch. Die Sprache wird im Reiter **GUI** unter **Sprache** gewählt; Vorgabe ist die Systemsprache. Der Wechsel wirkt sofort, ohne Neustart. Rufzeichen und Fachkürzel wie **Call** und **Act** bleiben unübersetzt.

- **Farben einstellbar:** Im neuen Reiter **Farben** lassen sich die Farben für das helle und das dunkle Design einzeln anpassen. Das Fenster warnt, wenn eine Kombination schwer lesbar wird, und bietet Wege zurück zum vorherigen Stand oder zur Auslieferung. Wer lieber direkt in der Stilvorlage arbeitet, kann sie dort bearbeiten.

- **Operator-Profile:** Jedes Profil hat seine eigene `preferences.xml` und damit eigene fachliche Einstellungen und einen eigenen Layoutstand. Verwaltet werden Profile im neuen Reiter **Profiles** des Einstellungsfensters: anlegen, duplizieren, umbenennen, löschen und aktivieren.

- **Gemeinsame oder eigene gearbeitete Stationen:** Pro Profil wird entschieden, ob es eine eigene Worked-Datenbank bekommt oder die gemeinsame Stationsdatenbank benutzt. Eine Multi-OP-Station mit einem einzigen Stationslog teilt den Worked-Status, zwei OMs mit verschiedenen Rufzeichen an einem Rechner trennen ihn.

- **Profilwahl beim Start:** Mit nur einem Profil fragt KST4Contest beim Start nichts und verhält sich unverändert. Ab zwei Profilen erscheint eine kleine Auswahl mit vorausgewähltem letzten Profil; Enter oder Doppelklick starten sofort. Der Aufrufparameter `--profile=<Name>` überspringt die Auswahl.

- **Profilwechsel im laufenden Betrieb:** **File → Switch operator profile...** trennt die Verbindung und baut die Oberfläche mit den Einstellungen des gewählten Profils neu auf, ohne Programmneustart.

- **Filter für eine Mindestentfernung:** Neben **Show only QRB [km] <=** gibt es in der Filterleiste jetzt **Show only QRB [km] >** (deutsche Oberfläche: **Nur QRB [km] >**). Er blendet alle Stationen bis zur eingetragenen Entfernung aus. Sind beide eingeschaltet, zeigt die Liste einen Entfernungsbereich, etwa nur Stationen zwischen 300 und 800 km. Stationen ohne bekannte Entfernung werden ausgeblendet, solange einer der beiden Filter aktiv ist, und die Stationskarte folgt dem Filter. Wie beim Höchstwert gilt der Wert nur für die laufende Sitzung. Damit ist [Issue #21](https://github.com/HamRadioOnline/kst4contest/issues/21) umgesetzt.

- **Windows-Installer:** Neben dem portablen ZIP-Paket gibt es für Windows bei Stable-Releases jetzt einen MSI-Installer (`KST4Contest-v<Version>-windows-x64.msi`). Er installiert KST4Contest ohne Administratorrechte für das aktuelle Benutzerkonto, legt einen Startmenü-Eintrag und eine Desktop-Verknüpfung an und ersetzt bei einem Update die vorhandene Installation. Einstellungen und Datenbank bleiben unter `%USERPROFILE%\.praktiKST` und werden mit einer portablen Kopie geteilt. Damit ist die erste Stufe von [Issue #61](https://github.com/HamRadioOnline/kst4contest/issues/61) umgesetzt.

- **Eigene Standorthöhe und höchster Punkt des Gegenstationsfelds in der Streckenanalyse:** Der Reiter **Station** hat im Abschnitt der Streckenanalyse zwei neue Einstellungen. **Eigene Standorthöhe über NN, Meter** ersetzt den Wert des Höhenmodells am eigenen Locatormittelpunkt durch die Höhe des tatsächlichen Standorts; bleibt das Feld leer, entscheidet wie bisher das Höhenmodell. **Gegenstation auf dem höchsten Punkt ihres Locatorfelds**, standardmäßig eingeschaltet, lässt die Strecke am höchsten von 25 abgetasteten Punkten des Locatorfelds der Gegenstation enden statt in dessen Mitte, weil Conteststationen meist vom höchsten Punkt ihres Feldes aus arbeiten. Die Suche kostet pro Gegenstationsfeld und Sitzung eine zusätzliche Anfrage an den Höhendienst; schlägt sie fehl, rechnet die Analyse wie bisher mit der Feldmitte.

- **SO2R mit zwei Funkgeräten:** Die neue Option **SO2R** im Reiter **TRX-Abgleich** (standardmäßig aus) führt zwei Geräte getrennt. N1MM, DXLog und UCXLog werden über `RadioNr` zugeordnet: Gerät 1 setzt MYQRG, Gerät 2 setzt SECONDQRG. Für Win-Test wird je Gerät ein Stationsname eingetragen. Ein zweiter PSTRotator (eigener Host und Port, im Reiter **Station**) bringt **Antenne 2 drehen** neben **Antenne 1 drehen** und ein Feld QTF2 neben QTF. Ohne SO2R bleibt alles wie bisher. Das setzt den ersten Teil von [Issue #46](https://github.com/HamRadioOnline/kst4contest/issues/46) um.

### Geändert

- **Neues Zuhause auf GitHub:** Das Projekt ist vom Konto `praktimarc` in die GitHub-Organisation [HamRadioOnline](https://github.com/HamRadioOnline) umgezogen und liegt jetzt unter [HamRadioOnline/kst4contest](https://github.com/HamRadioOnline/kst4contest). Alte Links auf das Repository, die Releases, die Issues und das Wiki leiten auf die neue Adresse weiter. Auch das Flatpak-Repository ist umgezogen und liegt jetzt unter `https://hamradioonline.github.io/kst4contest/`; bestehende Flatpak-Installationen aktualisieren über die Weiterleitung der alten Adresse weiter, neue Einrichtungen verwenden die neue Adresse aus der Installationsanleitung. Die AUR-Pakete zeigen ebenfalls auf die neue Adresse.

- **Zwischenspeicher der Geländeprofile getrennt:** Berechnete Geländeprofile liegen jetzt in der eigenen, gemeinsam genutzten Datei `terrainprofilecache.db` und werden nach Besitzer getrennt gespeichert. Bisher wurde der gesamte Zwischenspeicher gelöscht, sobald sich Rufzeichen oder Locator änderten; bei einem Profilwechsel wäre damit jedes berechnete Profil verloren gegangen.

- **Rufzeichen-Vorgabe ist leer:** Fehlt in der `preferences.xml` ein Login-Rufzeichen, bleibt das Feld jetzt leer, statt auf ein im Programm hinterlegtes Rufzeichen zurückzufallen. Ein neu angelegtes Profil startet damit bewusst ohne Anmeldedaten.

- **PSTRotator-Azimut mit einer Nachkommastelle:** Der an PSTRotator gesendete Azimut wurde bisher auf ganze Grad abgeschnitten, sodass eine Station bei 145,7° die Antenne auf 145° drehte. Jetzt wird er auf eine Nachkommastelle gerundet, immer mit Punkt als Trennzeichen.

- **Dark Mode pro Profil:** Im Reiter **GUI** legt jedes Operator-Profil unter **Design** fest, ob KST4Contest hell oder dunkel startet. Die Menüeinträge unter **Windows** schalten weiterhin schnell um, gelten aber nur für die laufende Sitzung. Das Farbschema erfasst jetzt auch das Update-Fenster und die Liste der Top-Kandidaten.

- **Einstellungen wirken erst mit Übernehmen:** Änderungen im Einstellungsfenster wirken nicht mehr während der Eingabe, sondern erst mit **Übernehmen/Schließen** oder **Einstellungen speichern**; **Verbinden** übernimmt sie ebenfalls vorher. Das gilt auch während einer laufenden Sitzung. Wer mit offenen Änderungen das Fenster schließt, das Profil wechselt oder das Programm beendet, wird zuerst gefragt, ob sie übernommen oder verworfen werden sollen, oder kann zurück. Farben, Sprache und die Anmeldedaten im Reiter **Station** wirken wie bisher sofort.

- **Fenster öffnen auf dem Bildschirm des Hauptfensters:** Stationskarte, Monitor, Einstellungen, Update-Fenster, Top-Kandidaten und die Hinweisdialoge öffneten bisher auf dem primären Bildschirm, auch wenn das Hauptfenster auf einem anderen lag oder der primäre Bildschirm ausgeschaltet war. Sie öffnen jetzt zentriert auf dem Bildschirm, auf dem das Hauptfenster gerade steht. Ein Fenster, das in der laufenden Sitzung verschoben wurde, öffnet beim nächsten Mal wieder dort, sofern seine Titelleiste noch auf einem angeschlossenen Bildschirm erreichbar ist. Mit der neuen Option **Fenster nach einem Neustart dort öffnen, wohin sie zuletzt geschoben wurden** im Reiter **GUI** (standardmäßig aus) behalten das Hauptfenster und alle anderen Fenster ihre Position auch über einen Neustart; eine gespeicherte Position wird nur verwendet, wenn genau derselbe Bildschirm (gleiche Lage und Auflösung) wieder angeschlossen ist. Ist ein Fenster größer als der Bildschirm, wird es auf dessen nutzbare Fläche verkleinert, auch wenn es später auf einen kleineren Bildschirm geschoben wird, etwa einen 640×480-Monitor; die gespeicherte Fenstergröße bleibt dabei unverändert. Das behebt [Issue #59](https://github.com/HamRadioOnline/kst4contest/issues/59).

### Behoben

- **Verbindung aus den Einstellungen erneut aufbauen:** Nach **Disconnect** blieb **Connect to ...** im geöffneten Einstellungsfenster deaktiviert. Die Buttons folgen jetzt dem Verbindungszustand des Connection Managers, sperren doppelte Verbindungsversuche und werden nach einem vollständigen Disconnect oder einem fehlgeschlagenen Versuch wieder freigegeben.

- **Reachability-Bedienung wieder sichtbar:** Die Compose-Filterleiste enthält wieder **Reachability** mit **Auto** und den lokal aktivierten Bändern sowie **Calc selected**. Nur die ausdrückliche Berechnung der ausgewählten Station darf Online-Höhendaten anfordern; Tabellenaufbau, Sortierung und Bandauswahl verwenden ausschließlich vorhandene Ergebnisse.

- **Kompaktere Oberfläche auf kleinen Bildschirmen:** Der Further-Info-Bereich ist scrollbar, leere Tabellen zeigen einen eindeutigen Leerzustand, Filterzeilen brechen bei Bedarf um und lange Sendetexte lassen sich vollständig als Hinweis anzeigen. Bedienelemente und rechte Fensterbereiche sind deutlicher voneinander abgegrenzt.

- **Diagnosedateien gebündelt und begrenzt:** Neue Fehlerprotokolle und ON4KST-Messagehistory-Dateien werden unter `.praktiKST/debug` angelegt. Vorhandene Dateien im bisherigen Verzeichnis bleiben unverändert. Das Fehlerprotokoll rotiert bei 20 MiB und behält einschließlich der aktiven Datei höchstens fünf Generationen.

- **Einheitliche Schriftgröße:** Unter macOS und Linux verwendet JavaFX eine etwas größere Standardschrift als unter Windows. Dadurch wurden Beschriftungen in fest bemessenen Bedienelementen abgeschnitten. KST4Contest verwendet jetzt auf allen Systemen dieselbe Grundschriftgröße; unter Windows ändert sich nichts.

- **Dark Mode nach dem Neustart:** Das Hauptfenster startete immer hell, auch wenn zuletzt der Dark Mode aktiv war, während die Stationskarte dunkel blieb. Jetzt starten alle Fenster einheitlich mit dem Farbschema des Profils.

- **Freigegebene Hintergrundressourcen:** Der ON4KST-Überwachungsthread, der Sked-Erinnerungs-Scheduler, der Reachability-Executor, der PSTRotator-Wiederholungs-Scheduler und der Kachel-Proxy der Karte werden beim Schließen des Chatcontrollers freigegeben. Bisher liefen sie bis zum Programmende weiter.

- **AP-Spalte sortiert numerisch:** Die Spalte **AP [minutes / pot%]** in der Benutzerliste und bei den gerichteten Nachrichten wurde nach ihrem Text sortiert, sodass `13 (75%)` vor `3 (50%)` stand. Jetzt sortiert sie nach den Minuten des ersten Flugzeugs und bei gleichen Minuten nach dem besseren Potenzial; leere und `nil`-Zellen stehen aufsteigend am Ende. Damit ist [Issue #87](https://github.com/HamRadioOnline/kst4contest/issues/87) behoben.
- **Keine automatischen Cluster-Spots für gearbeitete Stationen:** Der lokale DX-Cluster-Server erzeugte Spots aus Richtungsgelegenheiten, ohne den Worked-Status zu prüfen. Jetzt wird kein Spot mehr erzeugt, wenn die Station auf dem Band der erkannten Frequenz bereits gearbeitet ist; ohne bandbezogene Information zählt der allgemeine Worked-Status. Manuelle Spots aus der Karte und der Test-Spot in den Einstellungen bleiben unverändert. Damit ist [Issue #91](https://github.com/HamRadioOnline/kst4contest/issues/91) behoben.
- **QSO-Monitoring übernimmt keine Fremdeinträge mehr:** Die `preferences.xml` enthielt neben der Liste der überwachten Rufzeichen eine zweite Liste `snifferWords`, die nirgends in der Oberfläche erschien. Beim Laden wurden ihre Einträge trotzdem als Rufzeichen in das QSO-Monitoring übernommen. Die tote Liste ist entfernt; ein vorhandenes `snifferWords`-Element in alten Dateien wird ignoriert. Damit ist [Issue #103](https://github.com/HamRadioOnline/kst4contest/issues/103) behoben.
- **Kein Locator für die zweite Kategorie mehr:** Intern gab es eine Einstellung für einen eigenen Locator der zweiten Chat-Kategorie, die weder in der Oberfläche noch in der `preferences.xml` erschien und deshalb immer auf ihrem eingebauten Standardwert stand. Beim Verbindungsaufbau wurde dieser Standardwert mit dem eigenen Locator verglichen und eine Warnung ausgegeben, dass der zweite Locator ignoriert werde. ON4KST verwendet ohnehin einen Locator pro TCP-Sitzung; die Einstellung und die Warnung sind entfernt. Damit ist [Issue #104](https://github.com/HamRadioOnline/kst4contest/issues/104) behoben.
- **Linux-Beta-Pakete lassen sich auf das stabile Paket aktualisieren:** DEB- und RPM-Pakete eines Beta-Tags trugen dieselbe Versionsnummer wie das folgende stabile Paket, sodass `dnf` und `dpkg` die Aktualisierung als gleiche Version ablehnten. Beta-Pakete erhalten jetzt eine Vorab-Release-Nummer (`1.50.0-0.beta.001`) unter der des stabilen Pakets (`1.50.0-1`), und der Release-Lauf bricht ab, wenn der Tag nicht zur `composePackageVersion` passt. Damit ist [Issue #113](https://github.com/HamRadioOnline/kst4contest/issues/113) behoben.

- **UCXLog-Pakete werden nicht mehr abgeschnitten:** Nach einem kurzen UDP-Paket las der Empfänger für UCXLog-kompatible Logprogramme jedes längere Folgepaket nur bis zur Länge des kurzen, sodass Logeinträge unvollständig ankommen konnten. Außerdem schließt das Trennen den Socket des Empfängers jetzt sofort, statt bis zum nächsten Timeout zu warten. UCXLog-Pakete und Antworten von AirScout werden jetzt immer als UTF-8 dekodiert statt im Standardzeichensatz des Systems, sodass Umlaute in Stationsnamen und Operator-Rufzeichen unter Windows nicht mehr verstümmelt ankommen. Damit ist [Issue #115](https://github.com/HamRadioOnline/kst4contest/issues/115) behoben.

- **UCXLog/DXLog-Empfänger läuft weiter:** Eine nicht beschreibbare Sicherungsdatei für empfangene QSOs oder ein RadioInfo-Paket ohne Frequenz oder Betriebsart hielt den Empfänger für UCXLog-kompatible Logprogramme bis zum nächsten Neustart an, sodass weder gearbeitete Stationen noch QRG-Änderungen ankamen. Jetzt wird der Sicherungseintrag beziehungsweise das unvollständige Paket übersprungen, und der Empfänger empfängt weiter. Jeder andere unerwartete Fehler beim Verarbeiten eines Pakets verwirft jetzt nur dieses Paket und erscheint als Statusmeldung, statt den Empfänger anzuhalten.

### Hinweise zur Aktualisierung

- Bestehende Installationen werden **nicht** verändert: `preferences.xml` und `praktiKST.db` bleiben genau dort liegen, wo sie sind, und werden zum Profil **Default**. Es wird keine Datei verschoben oder kopiert.
- Eine Profil-Registry entsteht erst beim Anlegen des zweiten Profils. Wer nur ein Profil benutzt, merkt von der Änderung nichts.
- Eine Rückkehr zu einer älteren KST4Contest-Version bleibt möglich; sie findet ihre Dateien unverändert vor.
- Passwörter stehen weiterhin im Klartext in der `preferences.xml` des jeweiligen Profils. Profile trennen die Konfiguration, sie sind kein Zugriffsschutz.

---

## v1.43.1 (2026-09-03)

**Korrigierte Versionsmetadaten**

v1.43.1 enthält dieselben funktionalen Änderungen wie v1.43.0. Korrigiert wurden die Anwendungs- und Build-Metadaten für Anzeige, ON4KST-Kennung und Update-Vergleich. Ein Teil der Metadaten des ersten v1.43.0-Pakets wies den Build noch als Version 1.42 aus.

### Behoben

- **Einheitliche semantische Version:** Die sichtbare Anwendungsversion verwendet jetzt vollständig `1.43.1`. Der kompakte Wert `1.431` bleibt ausschließlich im veralteten numerischen Feld für die Kompatibilität mit älteren Update-Feeds erhalten.

- **Update-Feed im Tagged-Release-Workflow:** Website-Build, Prüfung des Versionsfeeds und Upload der Artefakte laufen jetzt tatsächlich nach der Veröffentlichung des GitHub Releases. Diese Schritte waren zuvor versehentlich in die Artefaktliste der Release-Action eingerückt und wurden deshalb übersprungen.

Wer v1.43.0 installiert hat, sollte v1.43.1 verwenden. Die Funktionen bleiben unverändert; korrigiert werden nur die Versionsmetadaten und der Release-Workflow.

Die korrigierte Version ist als [Release v1.43.1](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.43.1) verfügbar.

---

## v1.43.0 (2026-09-03)

**Zuverlässigere Log-Synchronisation, gespeicherte Tabellenlayouts und bessere DX-Cluster-Kompatibilität**

v1.43 konzentriert sich auf die zuverlässige Verarbeitung externer Logdaten und den stabilen Betrieb während längerer Conteste. Hinzu kommen praktische Einstellmöglichkeiten für Tabellenlayouts und die Stationsgruppierung auf der Karte.

### Neu

- **Optionale Kartencluster:** **Group nearby stations** schaltet die räumliche Gruppierung bei niedrigen Zoomstufen unmittelbar ein oder aus. Die gespeicherte Auswahl verändert weder Kartenausschnitt noch Stationsauswahl; die Zusammenfassung aktiver Varianten desselben Basisrufzeichens bleibt davon unabhängig. Damit ist [Issue #79](https://github.com/HamRadioOnline/kst4contest/issues/79) umgesetzt.

- **Tabellenlayout automatisch gesichert:** Tabellen erhalten beim ersten brauchbaren Inhalt sinnvolle Breiten. Manuell geänderte Spaltenbreiten, Fenstergrößen und relevante Divider werden nach kurzer Verzögerung in `preferences.xml` geschrieben. Haupt- und Monitorfenster behalten getrennte Layouts für DXCluster und QSO of the other.

- **Tooltips für gekürzte Tabellenwerte:** Normale Tabellenzellen zeigen ihren vollständigen Inhalt, wenn die sichtbare Spalte zu schmal ist. Funktionale Tooltips und anklickbare Links bleiben davon unberührt.

- **Hinweis bei neu angelegtem Simplelogfile:** Fehlt die ausgewählte Datei, legt KST4Contest sie an und zeigt einen Hinweis mit Dateipfad, konkretem Testablauf und Link zum passenden Abschnitt des Handbuchs.

### Geändert

- **Robuste Simplelogfile-Auswertung:** Die ausgewählte Datei wird einmal pro Minute ausgewertet und nach jedem Durchlauf geschlossen, damit das Logprogramm sie ersetzen oder rotieren kann. Erkannte Rufzeichen setzen den globalen Worked-Status für alle aktiven Suffixvarianten des Basisrufzeichens. Bei deaktivierter Funktion findet kein Dateizugriff statt; Lese- oder Erstellungsfehler beenden die periodische Aufgabe nicht mehr.

- **Einheitliche Bandwerte externer Logger:** UCXLog-kompatible Pakete und Win-Test-Ereignisse verwenden eine gemeinsame Bandnormalisierung. Numerische Werte, Meter- und Zentimeterangaben sowie die vorhandenen Win-Test-IDs setzen damit dieselben Worked-Markierungen und Worked-Großfelder. Insbesondere `2320`, `5760` und `10368` werden zuverlässig verarbeitet; bei fehlendem oder unbekanntem Band wird nur der globale Worked-Status gesetzt.

- **Kompakte vollständige Frequenzen erkannt:** Vollständige Frequenzangaben ohne Dezimaltrenner werden auf allen unterstützten Bändern akzeptiert; die letzten drei Ziffern bilden den kHz-Anteil. Nackte dreistellige Zahlen benötigen weiterhin einen erkennbaren Frequenzkontext, damit Signalrapporte und andere Zahlen nicht als QRG behandelt werden.

- **DXSpider-kompatibles Spotformat:** Lokale DX-Cluster-Spots verwenden eine feste 75-Zeichen-Nutzzeile mit dem DX-Rufzeichen ab Spalte 27, einem 30 Zeichen breiten Kommentarfeld und der UTC-Zeit ab Spalte 71. Das Format bleibt bis 24 GHz stabil. Überlange DX-Rufzeichen werden verworfen und protokolliert, statt unbemerkt abgeschnitten zu werden. Damit ist [Issue #86](https://github.com/HamRadioOnline/kst4contest/issues/86) behoben.

- **Aktive ON4KST-Verbindungsprüfung:** Ein ruhiger Chatserver wird mit einer expliziten, sitzungsweiten Abfrage geprüft, bevor die Verbindung als unterbrochen gilt. Heartbeat und Prüftelegramme behalten das erforderliche CR/LF-Framing.

- **Zuverlässige Altersmarkierung privater Nachrichten:** Eingehende Privatnachrichten verwenden bis zu fünf Minuten lang die definierten grünen Altersstufen. Eigene Nachrichten behalten ihre separate Darstellung; leere oder wiederverwendete Tabellenzeilen kehren zum normalen Design zurück und behalten keine veraltete Hervorhebung.

### Behoben

- **Worked-Status nach dem Login:** Persistierte SQLite-Informationen werden vor der Veröffentlichung jeder initialen ON4KST-Benutzerliste geladen und angewendet. Erneute Verbindungen, beide Kategorien und alle aktiven Varianten eines Basisrufzeichens starten dadurch mit dem richtigen Status. Damit ist [Issue #85](https://github.com/HamRadioOnline/kst4contest/issues/85) behoben.

- **Fehlerhafte Trennung bei ruhigem Server:** Eine gültige ON4KST-Verbindung wird nicht mehr beendet, nur weil der Server gerade keine Aktivitätszeilen überträgt.

### Dokumentation und Auslieferung

- Das deutsche und englische Handbuch wurden mit der Implementierung abgeglichen und überarbeitet. Ein neues Kapitel zum Contest-Workflow verbindet die einzelnen Funktionen zu einem praktischen Betriebsablauf; außerdem wurden die Abschnitte zu Dual Chat, Privatnachrichten, QRG-Synchronisation, Simplelogfile-Auswertung und Konfiguration präzisiert.

- Die Website beschreibt Band- und Richtungsgelegenheiten, Stationskarte, QRG-Verarbeitung, Filter, globale Nachrichtenansichten, Privatnachrichten und Log-Synchronisation jetzt ausführlicher.

- Die AUR-Paketdefinitionen wurden auf v1.43.0 aktualisiert.

Die vollständige Funktionalität von v1.43.0 steht im [Release v1.43.0](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.43.0) bereit. Wegen der inkonsistenten eingebetteten Versionsmetadaten ist v1.43.1 der empfohlene Paketstand.

---

## v1.42.0 (2026-08-22)

**Gemeinsamer Bandkontext, sitzungsbasierte ON4KST-Verbindung und signierte macOS-Pakete**

v1.42 führt mehrere bisher getrennte Auswertungen zusammen. Bandinformationen, Worked-Status, NOT-QRV-Markierungen, Rufzeichensuffixe und Frequenzen werden dadurch konsistenter in der Benutzerliste, der Stationskarte, der Prioritätsberechnung und den externen Schnittstellen verwendet.

### Neu

- **Sichtbarer ON4KST-Verbindungsstatus:** Ein kompakter `LINK`-Indicator im Hauptfenster zeigt den tatsächlichen Zustand der ON4KST-Verbindung an. Grün bedeutet vollständig angemeldet und synchronisiert, Gelb kennzeichnet Verbindungsaufbau und Synchronisation, Rot eine unterbrochene Verbindung, einen Fehler oder die Wartezeit vor dem nächsten Verbindungsversuch.

- **Gemeinsame Herleitung verfügbarer Bänder:** Ein zentraler `BandOpportunityResolver` wertet aktuelle QRGs, Bandangaben im Namensfeld, aktive Rufzeichenvarianten, Worked-Informationen und NOT-QRV-Markierungen gemeinsam aus. Benutzerliste, **New bands**, Band-Upgrade-Hinweis, Priority Score, Stationskarte und automatische Bandauswahl verwenden damit dieselbe Grundlage.

- **Erweiterte Bandanzeige:** Die Bandspalten unterscheiden jetzt:
  - `X` für auf diesem Band gearbeitet,
  - `a` für ein angebotenes, noch nicht gearbeitetes Band einer insgesamt neuen Station,
  - `B+` für ein angebotenes, noch nicht gearbeitetes Band einer bereits auf einem anderen Band gearbeiteten Station und
  - `o` für ein auf diesem Band bereits gearbeitetes Locator-Großfeld.

  Die Anzeigen können kombiniert werden, beispielsweise als `ao` oder `B+o`. Die zusätzlichen Kennzeichnungen `a` und `o` lassen sich in den GUI-Einstellungen separat ausblenden.

- **Unterstützung für 50 und 70 MHz:** Beide Bänder stehen in der Stationskonfiguration, den Worked- und NOT-QRV-Funktionen, der Benutzerliste, den Filtern, der internen Datenbank, der UCXLog-Auswertung und dem Win-Test-Listener zur Verfügung. Bereits gespeicherte Datenbanken werden um die benötigten Spalten ergänzt.

- **Globale Nachrichtentabs:** Öffentliche Nachrichten, ON4KST-DX-Cluster-Meldungen und gerichtete Nachrichten zwischen anderen Stationen können direkt im Hauptfenster angezeigt werden. Das bisherige separate Monitorfenster bleibt zusätzlich verfügbar und verwendet dieselben Nachrichtenspeicher.

- **Manuelle QTF-Eingabe:** Die aktuelle Antennenrichtung kann auch ohne PSTRotator direkt in KST4Contest geändert werden.

- **Nachrichtenvariable `MYQTF`:** Die aktuelle Antennenrichtung kann als numerischer Winkel in Grad in Shortcuts, Snippets und andere unterstützte Nachrichtentexte eingesetzt werden.

- **Filter zurücksetzen:** Ein eigener Reset-Button entfernt die aktiven Filterprädikate der Benutzerliste zuverlässig.

- **Kartencluster:** Räumlich dicht beieinanderliegende Stationen werden bei niedrigen Zoomstufen zusammengefasst. Die ausgewählte Station und relevante Richtungsgelegenheiten bleiben einzeln sichtbar.

- **Ausblendbare Streckenanalyse:** Geländeprofil und Analysebereich der Stationskarte können vollständig ausgeblendet werden. Die Auswahl wird gespeichert und beim nächsten Programmstart wiederhergestellt.

### Geändert

- **Sessionbezogene ON4KST-Verbindungssteuerung:** Socket, Reader, Writer, Messagebus und Warteschlangen gehören jetzt zu einer eindeutig identifizierten Verbindungssession. Veraltete Threads einer abgelösten Verbindung können dadurch keine Daten mehr verarbeiten oder die neue Verbindung schließen. `ONLINE` wird erst nach bestätigtem Login und vollständig empfangenen Benutzerlisten gemeldet. Verbindungsaufbau, Login und Synchronisation besitzen feste Zeitlimits; Heartbeats, ausbleibende Eingangsdaten, EOF sowie Lese- und Schreibfehler werden überwacht und lösen bei Bedarf einen kontrollierten Neuaufbau mit Backoff aus.

- **ON4KST-Protokollbefehle abgesichert:** Ausgehende Befehle werden zentral aufgebaut und auf gültige Kategorien, Locatoren und unerlaubte Frame-Trennzeichen geprüft. Da ON4KST pro TCP-Session nur einen Locator verwaltet, wird für beide Chat-Kategorien der Hauptlocator verwendet und eine abweichende zweite Konfiguration protokolliert, statt widersprüchliche Befehle an den Server zu senden.

- **QRG-Erkennung präzisiert:** Vollständige und relative Frequenzangaben werden weiterhin erkannt. Nackte dreistellige Zahlen gelten nur noch bei erkennbarem Frequenzkontext als QRG. Signalrapporte, Bandangaben und andere Zahlen erzeugen dadurch seltener falsche Frequenzen.

- **Stationsbezogener Frequenzkontext:** Bei relativen QRGs verwendet KST4Contest zuerst einen höchstens 30 Minuten alten Bandkontext derselben Station. Erst wenn dieser fehlt, wird das global konfigurierte Fallback-Band verwendet.

- **Fallback-Band als Dropdown:** Das globale Fallback kann nur noch aus unterstützten Bandwerten ausgewählt werden. Es betrifft die gesamte QRG-Erkennung und nicht nur DX-Cluster-Spots.

- **Einheitliche QRG-Darstellung:** Frequenzen werden in Benutzer- und Nachrichtentabellen mit mindestens drei Nachkommastellen dargestellt.

- **Bandabhängige AirScout- und Streckenberechnung:** KST4Contest leitet für jede Station eine möglichst realistische Frequenz aus der aktuellen QRG und den bekannten Bandinformationen ab. AirScout erhält kanonische Bandwerte. Die frühere Zwischenlösung mit 430 MHz wurde durch 432 MHz ersetzt.

- **Gemeinsame Frequenzherleitung:** AirScout, **Calc selected** und die Pfadanalyse der Stationskarte verwenden denselben `PropagationFrequencyResolver`. Ein im Reachability-Dropdown ausdrücklich gewähltes Band wird bei manuellen Berechnungen berücksichtigt.

- **Rufzeichenvarianten getrennt verarbeitet:** Aktive Chatmember werden durch das vollständige Rufzeichen einschließlich Suffix und die Chat-Kategorie unterschieden. `DN9APW`, `DN9APW-2` oder vergleichbare Logins bleiben dadurch getrennte Nachrichtenziele.

- **Gemeinsame Basisinformationen:** Worked-Flags, NOT-QRV-Informationen und der Priority Score werden weiterhin für Varianten desselben Basisrufzeichens gemeinsam ausgewertet. Getrennte Nachrichtenziele führen damit nicht zu widersprüchlichen Worked-Daten.

- **Priority Score korrigiert:** Stationen ohne gemeinsames verfügbares Band oder mit übersteuernder NOT-QRV-Markierung werden nicht mehr als Prioritätskandidaten angeboten. Bandgelegenheiten bereits gearbeiteter Stationen können einen eigenen Priority Boost erhalten.

- **Sked-Erstellung erweitert:** Das Band wird aus den lokal aktivierten Bändern gewählt. Für die Win-Test-Übergabe wird `SSB` oder `CW` ausdrücklich ausgewählt, statt den Mode unzuverlässig aus dem Band abzuleiten.

- **Win-Test-Sked-Übergabe präzisiert:** Die QRG muss zum gewählten Band passen. Sichtbare KST-Suffixe werden für das Logziel entfernt, portable Bestandteile bleiben erhalten und die Zeitangaben der `ADDSKED`-Pakete werden korrekt erzeugt. Ein Fehler bei der Übergabe entfernt den internen KST4Contest-Sked nicht.

- **Exakte Sked-Ziele:** Timeline und automatische Erinnerungen verwenden das vollständige sichtbare KST-Rufzeichen. Ein Sked für `DN9APW-2` wird nicht versehentlich an eine andere Variante desselben Basisrufzeichens gesendet.

- **Beacon und Autoantwort überarbeitet:** Beide Chat-Kategorien verwenden einen gemeinsamen Timer, behalten aber getrennte Aktivierungsschalter und Texte. Das zulässige Mindestintervall beträgt eine Minute; Nachrichtentexte sind auf 120 Zeichen begrenzt. Die gespeicherte Beacon-Aktivierung wird beim Start aus der Konfiguration übernommen.

- **Variablen zentral aufgelöst:** Nachrichtenvariablen für Beacons, Shortcuts, Snippets und andere automatisch erzeugte Texte werden über einen gemeinsamen Resolver verarbeitet.

- **Nachrichtentabellen verbessert:** Abgeschnittene Nachrichtentexte erhalten einen Tooltip mit dem vollständigen Inhalt. Erkannte Webadressen können im Systembrowser geöffnet werden.

- **Kompaktere Filterleiste:** Die Filter bleiben bei normaler Breite in einer kompakten Anordnung und werden erst dann umgebrochen, wenn der tatsächlich verfügbare Platz nicht mehr ausreicht. Der mittlere Divider kann dadurch weiter verschoben werden.

- **DXLog-Gesamtlog übernommen:** Der UCXLog-kompatible UDP-Listener verarbeitet neben `contactinfo` auch `contactreplace`. Dadurch kann ein von DXLog.net als vollständiges Log ausgesendeter Datenbestand eingelesen werden.

- **Automatische QRG-Übernahme abgesichert:** `MYQRG` wird nur von einer aktivierten Schnittstelle aktualisiert, die tatsächlich gültige `RadioInfo`- beziehungsweise Win-Test-`STATUS`-Pakete liefert. Eine aktivierte, aber nicht liefernde Quelle ersetzt die notwendige Funktionsprüfung oder manuelle QRG-Pflege nicht.

- **Versionserkennung verbessert:** Versionsnummern werden semantisch verglichen, damit beispielsweise Patch-Versionen und Nightly-Stände nicht mehr durch eine einfache Fließkommazahl falsch eingeordnet werden.

### Behoben

- **Zuverlässige Benutzerliste beim Login:** Ungültige oder unvollständige `UA0`-Teilnehmerdatensätze werden einzeln verworfen und protokolliert, ohne die Verarbeitung der alphabetisch folgenden Teilnehmer abzubrechen. Die gültigen Einträge werden zunächst pro Kategorie gesammelt und erst mit dem ersten zugehörigen `UE`-Abschlussframe vollständig veröffentlicht.

- **Benutzerliste verschwindet nach dem Login:** ON4KST kann nach Namens-, Status- oder anderen Live-Änderungen weitere `UE`-Frames für dieselbe Kategorie senden. Wiederholte Abschlussframes werden jetzt erkannt und ignoriert, damit eine bereits gefüllte Benutzerliste nicht durch eine leere Momentaufnahme ersetzt wird.

- **Fehlgeschlagener Erstaufbau und Verbindungsverlust:** Wenn beim Programmstart keine Verbindung zum Server hergestellt werden kann, läuft KST4Contest nicht mehr in eine Endlos- oder Busy-Wait-Schleife. Die Oberfläche bleibt bedienbar und weitere Versuche erfolgen mit begrenztem Backoff. Auch ein vom Server geschlossener oder über längere Zeit stummer Socket wird zuverlässig erkannt.

- **Messagebus-Protokollierung:** Bereits korrekt verarbeitete ON4KST-Frames werden nicht mehr zusätzlich als `Critical, detected unhandled Chatmessage` gemeldet. Nur tatsächlich unbekannte Telegramme erreichen noch diesen Logzweig.

- **Passwort im Fehlerlog:** Das ON4KST-Passwort wird beim Verbindungsaufbau nicht mehr im Klartext in die Konsole oder Logdatei geschrieben.

- **Langzeitfehler der Stationsauswahl:** Die vom Message-Thread verwalteten Chatmember wurden von der JavaFX-Ansicht entkoppelt. Gleichzeitige Änderungen der Daten und Tabellenansicht führen dadurch nicht mehr nach längerer Laufzeit zu fehlerhaften Auswahlmodellen oder Concurrent-Modification-Problemen.

- **Keine Phantom-Chatmember durch UM3:** Historische oder zusätzliche Servermeldungen erzeugen keine Benutzerlisteneinträge für Stationen, die nicht tatsächlich im Chat angemeldet sind.

- **Nachrichten an Rufzeichen mit Suffix:** Mehrere gleichzeitig angemeldete Varianten desselben Basisrufzeichens überschreiben sich nicht mehr gegenseitig. Damit wurde [Issue #73](https://github.com/HamRadioOnline/kst4contest/issues/73) behoben.

- **DX-Cluster-Locatoren:** Sender und gemeldete Station erhalten nicht mehr versehentlich denselben Locator. Damit wurde [Issue #48](https://github.com/HamRadioOnline/kst4contest/issues/48) behoben.

- **Worked-Anzeige in „QSO of the other“:** Die Worked-Spalten verwenden wieder die jeweils richtige sendende beziehungsweise empfangende Station.

- **Fehlende SECONDAP-Daten:** Eine nicht vorhandene zweite Aircraft-Scatter-Gelegenheit führt beim Bearbeiten der Anzeige nicht mehr zu einer ungültigen Textauswahl und JavaFX-Exception.

- **Historische Rufzeichen:** Das Hervorheben oder Anklicken eines Rufzeichens, das nicht mehr in der aktuellen Benutzerliste vorhanden ist, läuft nicht mehr in eine Exception.

- **Win-Test-Sked-Zeit und Rufzeichen:** Zeitstempel, Band-QRG-Zuordnung sowie die Behandlung von KST-Suffixen und portablen Rufzeichen wurden korrigiert.

- **Filter-Reset:** Alle Filterprädikate werden tatsächlich entfernt; der sichtbare Zustand des Buttons entspricht wieder dem wirksamen Filterzustand.

### Dokumentation und Auslieferung

- Das deutsche und englische Handbuch wurde systematisch mit dem Quellcode abgeglichen, erweitert und mit aktuellen Screenshots versehen. Die Dokumentation erklärt nicht nur die Bedienelemente, sondern auch Herleitung, Datenquellen und Grenzen der Funktionen.

- Für Arch Linux stehen `kst4contest-bin`, `kst4contest` und `kst4contest-git` im AUR zur Verfügung.

- Die Downloadseite unterscheidet Stable, Beta und Nightly und bietet die jeweils tatsächlich vorhandenen Pakete für Windows, Linux und macOS an.

- Nightly-Pakete werden automatisiert aus dem aktuellen `main`-Branch gebaut. Stable- und Beta-Releases verwenden reproduzierbare Paketnamen für die unterstützten Plattformen.

- **Signierte und notarisierte macOS-Pakete:** Die DMG-Dateien für Apple Silicon und Intel sind mit einer Apple Developer ID signiert und von Apple notarisiert; das Notarisierungsticket ist in der DMG hinterlegt. Der erste Start funktioniert damit per Doppelklick, ohne den bisherigen Umweg über **Öffnen** im Kontextmenü, und die Prüfung gelingt auch ohne Internetverbindung. Betroffen sind Nightly-, Beta- und Stable-Pakete gleichermaßen. Die Windows-Pakete sind weiterhin nicht signiert.

- **Korrekte Bundle-Kennung und Version unter macOS:** Die Anwendung meldet sich jetzt als `de.x08.KST4Contest` statt als `kst4contest.view` und trägt die tatsächliche Versionsnummer im Bundle. Bisher wies jedes Release im Finder unter **Informationen** die Version `1.0` aus. Bestehende Einstellungen sind davon nicht betroffen, da KST4Contest seine Daten in `~/.praktiKST/` ablegt und nicht an der Bundle-Kennung festmacht.

### Bekannte Grenzen

- Die aktive Geländedatenquelle verwendet Open-Meteo mit Copernicus-GLO-90-Daten und höchstens 100 Höhenpunkten pro Strecke.

- Der atmosphärische K-Faktor ist derzeit fest auf `4/3` eingestellt.

- Für die Gegenstation wird eine Antennenhöhe von 10 Metern über Grund angenommen.

- Aircraft-Scatter-Daten aus AirScout und die Geländeanalyse der Stationskarte sind weiterhin getrennte Bewertungen.

- Eine genauere Konfiguration von Stationshöhe, Frequenz und K-Faktor wird in [Issue #74](https://github.com/HamRadioOnline/kst4contest/issues/74) weiterverfolgt.

Die veröffentlichte Version ist als [Release v1.42.0](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.42.0) verfügbar.

---

## v1.41.1 (2026-07-08)

**Hotfix für Texteingabe und Fokusverhalten**

### Behoben

- Das Nachrichteneingabefeld wurde nach einiger Zeit beziehungsweise bei bestimmten UI-Aktualisierungen unerwartet geleert.
- Beim Filtern oder Auswählen einer Station wurde der Eingabefokus unbeabsichtigt wieder in das Sendefeld verschoben.

Die korrigierte Version ist als [Release v1.41.1](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.41.1) verfügbar.

---


## v1.41.0 (2026-07-01)

**Stationskarte, begrenzte Nachrichtenspeicher und bildschirmgerechtes Hauptfenster**

### Neu

- **Stationskarte:** Eine interaktive OpenStreetMap-Karte stellt aktive Chatmember mit brauchbarem Locator geografisch dar.

- **Antennensektor und Verbindungslinie:** Die Karte zeigt den aktuellen eigenen QTF, den konfigurierten Antennen-Öffnungswinkel, das maximale QRB und die Verbindung zur ausgewählten Station.

- **Maidenhead-Raster:** Ein an die Zoomstufe angepasstes Locator-Raster erleichtert die geografische Einordnung.

- **Geländeprofil:** Für ausgewählte Stationen kann ein Höhenprofil über die Open-Meteo Elevation API berechnet werden. Die aktive Datenquelle verwendet Copernicus GLO-90 und höchstens 100 gleichmäßig verteilte Abfragepunkte.

- **Geometrische Streckenanalyse:** Die Auswertung berücksichtigt Sichtlinie, Erdkrümmung mit `k = 4/3`, Radio- und Geländehorizont, erste Fresnel-Zone und eine grobe Hindernisabschätzung.

- **Lokaler Karten-Proxy:** Leaflet wird mit der Anwendung ausgeliefert. Kartenkacheln werden über einen lokalen Proxy geladen, damit keine externe JavaScript-Bibliothek zur Laufzeit nachgeladen werden muss. Für die OpenStreetMap-Kacheln und die Online-Höhendaten ist weiterhin eine Internetverbindung erforderlich.

### Geändert

- **Begrenzte Nachrichtenspeicher:** Die globale Chatnachrichtenliste wird oberhalb von 30.000 Einträgen auf 25.000 verkleinert. Der getrennte DX-Cluster-Speicher wird oberhalb von 10.000 Einträgen auf 8.000 verkleinert.

- **Bildschirmgerechte Startgröße:** Das Hauptfenster wird beim Start gegen den sichtbaren Bereich des primären Bildschirms geprüft und bei Bedarf verkleinert oder verschoben.

- **Kompaktere Benutzeroberfläche:** Mehrere Bereiche wurden für kleinere Bildschirme und verstellbare Divider angepasst.

### Einordnung der Kartenfunktion

Die Stationskarte und AirScout können dieselbe Gegenstation betreffen, führen aber getrennte Berechnungen durch. Aircraft-Scatter-Flugzeuge werden nicht in das Geländeprofil eingerechnet.

Im Quellcode vorhandene Klassen für Copernicus GLO-30, Offline-DEM-Import und weitere Terrain-Provider waren in v1.41 nicht Bestandteil der aktiv verwendeten Berechnungskette. Die tatsächlich verwendete Online-Höhenquelle ist Open-Meteo auf Basis von Copernicus GLO-90.

---

## v1.40 (2026-02-16)
**Großes Feature-Release: Score-System, AP-Timeline, Win-Test, PSTRotator**

**Neu:**
- **Chatmember Score-System**: Jeder Chatmember erhält automatisch eine Prioritätsbewertung anhand von Antennenrichtung, Aktivitätszeit, Nachrichtenanzahl, aktiven Bändern, Frequenzen, Sked-Richtung und anderen Faktoren. Die Top-Kandidaten werden in einer eigenen Liste hervorgehoben.
- **AP-Timeline**: Für jeden möglichen AP-Ankunftsminuten-Slot werden bis zu 4 hochbewertete Stationen angezeigt, die erreichbar wären. Bevorzugt werden APs mit dem höchsten Potenzial, nicht die schnellste Ankunft. Stationen, auf die die eigene Antenne nicht zeigt, werden transparent dargestellt.
- **Win-Test-Unterstützung** (ab v1.31 als Beta, jetzt vollständig konfigurierbar): Log-Synchronisation, Frequenzauswertung und **Sked-Übergabe via UDP** vollständig integriert. In den Preferences aktivier-/deaktivierbar.
- **PSTRotator-Interface** (ab v1.31 als Beta, jetzt vollständig konfigurierbar): Aktualisierung der Rotatorposition direkt aus KST4Contest. In den Preferences aktivier-/deaktivierbar.
- **QSO-Sniffer**: Nachrichten von konfigurierbaren Rufzeichen-Listen werden automatisch in das PM-Fenster weitergeleitet.
- **Band-Alert bei gearbeiteten Stationen**: Wenn eine Station geloggt wird, erscheint ein Hinweis, wenn diese Station ein weiteres Band aktiv hat, auf dem man selbst ebenfalls QRV ist.
- **Sked-Erinnerungs-ALERT**: Pro Chatmember kann ein Sked-Alarm mit automatischen Nachrichten in konfigurierbaren Intervallen (2+1 / 5+2+1 / 10+5+2+1 Minuten vor dem Sked) eingerichtet werden, plus akustische und optische Benachrichtigung.
- **Chat-Historie beim Start laden**: Beim Verbindungsaufbau wird die Serverhistorie geladen, um aktive Chatmember und letzte Nachrichten sofort sichtbar zu machen.
- **Skedfail-Button**: Im FurtherInfo-Panel kann ein Sked-Misserfolg für einen Chatmember markiert werden, was dessen Score senkt.

**Geändert:**
- AP-Notizen in DX-Cluster-Spots integriert.
- Scrolling der Chatmember-Tabelle folgt automatisch der aktuellen Nachrichtenauswahl.
- Generic Auto-Antwort und QRG-Auto-Antwort senden max. einmal pro 45 Sekunden pro Rufzeichen (verhindert Spam-Schleifen).
- Speicherbare Einstellungen erweitert: ServerDNS/Port, PSTRotator-Interface, Win-Test-Interface, Callsign-Sniffer, Dark-Mode-Standard.
- Datum in der Chat-Tabelle entfernt (nur Uhrzeit verbleibt – spart Platz).

**Behoben:**
- Benutzerliste wird jetzt bei jedem Neu-Login automatisch sortiert.
- Posonpill-Nachrichten beenden jetzt nur genau eine Client-Instanz (nicht alle und nicht wtKST).
- wtKST: Absturz bei KST4Contest-Trennung behoben.
- Mehrere Probleme mit Rufzeichen-Suffixen wie `/p`, `-2` etc. behoben.
- `QTFDefault` wurde nicht korrekt gespeichert → behoben.
- AirScout-Watchlist (ASWATCHLIST) wurde nicht korrekt aktualisiert → behoben.
- Dark Mode: QRG-Felder wurden nicht vollständig angezeigt → behoben.
- Versionsnummer-Anzeige korrigiert.

---

## v1.31 (2025-12-13)
**Win-Test + PSTRotator Beta, QSO-Sniffer, DNS-Hotfix**

**Neu:**
- **Win-Test-Unterstützung** (Beta, noch nicht deaktivierbar): Log-Synchronisation und Frequenzauswertung.
- **PSTRotator-Unterstützung** (Beta, noch nicht deaktivierbar).
- **QSO-Sniffer**: Nachrichten von konfigurierbaren Rufzeichen werden ins PM-Fenster weitergeleitet.

**Geändert:**
- **DNS-Server geändert**: Von `www.on4kst.info` auf `www.on4kst.org` (Hotfix). Der DNS-Server ist ab sofort in den Preferences änderbar.

**Behoben:**
- Endlosschleife im Fehlerfall friert den Client ein → behoben.

---

## v1.266 (2025-10-03)
**AirScout-Fix für Rufzeichen mit Suffix**

**Behoben:**
- AirScout-Interface funktionierte nicht, wenn das Login-Rufzeichen einen Suffix enthielt (z. B. `9A1W-2`). AirScout kann mit diesem Format nicht umgehen – es wird jetzt nur noch das Basis-Rufzeichen ohne Suffix an AirScout übergeben.

*(Fehler gemeldet und getestet von 9A2HM / Kreso – herzlichen Dank!)*

---

## v1.265 (2025-09-28)
**Richtungs-Buttons bleiben aktiviert eingefärbt**

**Behoben:**
- Richtungs-Buttons (N / NE / E usw.) behalten jetzt ihre Farbe, wenn sie aktiviert sind, sodass der Aktivierungsstatus auf einen Blick erkennbar ist.

---

## v1.264 (2025-08-02)
**Simplelogfile: Rufzeichen-Erkennung verbessert**

**Behoben:**
- Rufzeichen wie `S53CC`, `S51A` usw. wurden in der SimpleLogFile-Auswertung nicht als gearbeitet markiert → Erkennungsmuster verbessert.

*(Fehler gemeldet von Boris, S53CC – danke!)*

---

## v1.263 (2025-06-08)
**AirScout-Kommunikation und Login-Name**

**Geändert:**
- AirScout-Kommunikation grundlegend überarbeitet: Nur noch Stationen mit QRB < max-QRB werden an AirScout gesendet.
- Abfrage-Intervall von 12 Sekunden auf **60 Sekunden** erhöht.
- Deutlich weniger Berechnungsaufwand und Nachrichtenverkehr → Stabileres AirScout-Tracking.
- Name des AS-Clients und AS-Servers ist jetzt aus den Preferences konfigurierbar (war vorher hartcodiert auf „KST" / „AS").

**Behoben:**
- „Track in AirScout"-Button war sehr träge → durch neue Kommunikationslogik deutlich verbessert.
- Name im Chat ist jetzt speicherbar (Fehler behoben).
- Visuelle Korrekturen vor und nach dem Login.
- Fehler behoben, der von 9A2HM (Kreso) gemeldet wurde.

---

## v1.262 (2025-05-21)
**Freeze-Fix bei vorzeitiger Nachrichtenlieferung**

**Behoben:**
- ON4KST liefert manchmal Nachrichten, bevor der Login abgeschlossen ist. Das verursachte Fehler in der Nachrichtenverarbeitung → jetzt behoben.

---

## v1.26 (2025-05)
**Multi-Channel-Login und Dark Mode**

**Neu:**
- **Dark Mode**: Umschaltbar über `Window → Use Dark Mode`.
- **Multi-Channel-Login**: Gleichzeitiger Login in zwei Chat-Kategorien.
- **Opposite Station Multi-Callsign Login-Tagging**: Unterstützung für Stationen mit mehreren Rufzeichen.

**Geändert:**
- Farbgebungs-Mechanismus überarbeitet: Farben können jetzt über CSS angepasst werden.

**Behoben:**
- Stationsmarkierung komplett überarbeitet und korrekt gestellt.

---

## v1.251 (2025-02)
**Bugfix für UDP-Broadcast-Spot-Info**

**Behoben:**
- Problem beim Lesen von UDP-Broadcast-Spot-Informationen behoben (gemeldet von Steve Clements – danke!).
- Stationsmarkierung (erneut verbessert).

---

## v1.25 (2025-02)
**Wunschliste umgesetzt**

**Neu:**
- **Neuer Einstellungs-Tab: Messagehandling**
  - Auto-Antwort auf eingehende Nachrichten konfigurierbar.
  - Automatische Antwort mit eigener CQ-QRG, wenn jemand danach fragt.
  - Konfigurierbarer Standard-Filter für das Userinfo-Fenster *(für Gianluca :-) )*.
- **Farbige PM-Zeilen**: Neue Privatnachrichten erscheinen rot und faden alle 30 Sekunden über Gelb bis Weiß ab *(Idee von IU3OAR, Gianluca)*.

**Behoben:**
- Stationen mit Suffixen wie „-2" und „-70" wurden nicht als gearbeitet markiert → werden jetzt ignoriert, Station wird korrekt markiert.

---

## v1.24 (2024-11)
**Wunschliste + DX-Cluster-Spots**

**Neu:**
- Button zum Öffnen des **QRZ.com-Profils** der ausgewählten Station.
- Button zum Öffnen des **QRZ-CQ-Profils** der ausgewählten Station.
- **DX-Cluster-Server-Integration**: Richtungs-Warnungen werden als Spots an das Logprogramm gesendet (wenn QRG bekannt).

*(Zusätzlich wurden Farbgebungen der PM-Zeilen hinzugefügt – tnx Gianluca)*

---

## v1.23 (2024-10)
**Integrierter DX-Cluster-Server**

**Neu:**
- KST4Contest enthält jetzt einen **integrierten DX-Cluster-Server**.
- Generiert DX-Cluster-Spots und sendet sie an das Logprogramm, wenn eine Richtungs-Warnung ausgelöst und eine QRG bekannt ist.
- Spotter-Rufzeichen muss sich vom Contest-Rufzeichen unterscheiden (für korrekte Filterung im Logprogramm).

*(Idee von OM0AAO, Viliam Petrik – danke!)*

---

## v1.22 (2024-05)
**Usability-Verbesserungen und AirScout-Button-Fix**

**Neu:**
- Neue Variablen (tnx OM0AAO, Viliam Petrik):
  - `MYLOCATORSHORT`
  - `MYQRGSHORT`
  - `QRZNAME`

**Geändert:**
- Sendfeld-Fokus: Nach Klick auf Rufzeichen in der Benutzerliste erhält das Sendfeld sofort den Fokus – kein Doppelklick notwendig *(tnx Gianluca)*.

**Behoben:**
- Worked-Station-Filter ist jetzt live-aktiv: Gearbeitete Stationen verschwinden sofort nach Aktivierung des Filters *(tnx Gianluca)*.
- QRB-Sortierung war lexikografisch → jetzt numerisch *(tnx Alessandro Murador)*.
- AirScout-„Show Path"-Button: Klick maximiert AirScout und zeigt den Pfad korrekt an.

---

## v1.21 (2024-04)
**Usability-Verbesserungen**

**Geändert:**
- Fenstergrößen und Divider-Positionen werden beim Klick auf „Save Settings" in der Konfigurationsdatei gespeichert und beim Start wiederhergestellt.
- Filter-Bereich als Flowpane → bessere Darstellung auf kleineren Bildschirmen.

---

## v1.2 (2024-04)
**Bandselektion und NOT-QRV-Tags**

**Neu:**
- **Bandselektion**: In den Preferences auswählbar, welche Bänder aktiv sind. Nur für gewählte Bänder erscheinen Buttons und Felder in der UI. Speichern und Neustart erforderlich.
- **NOT-QRV-Tags pro Station und Band**: Stationen können für jedes Band als „nicht QRV" markiert werden. Kombinierbar mit dem Userlist-Filter.
- **QTF-Pfeil**: Der „Show path in AS"-Button zeigt jetzt einen Pfeil mit dem QTF der ausgewählten Station an.

---

## Frühere Versionen

### v1.1
Erste öffentlich veröffentlichte Version. Grundfunktionen:
- Worked-Markierung via Simplelogfile und UDP
- Sked-Richtungs-Hervorhebung
- QRG-Erkennung
- Text-Snippets und Shortcuts
- AirScout-Interface (erste Version)
- Intervall-Beacon
- PM-Abfang für öffentliche Nachrichten mit eigenem Rufzeichen
- Update-Hinweis-Dienst

---

## Geplante Features

- ~~Lebensdauer für den Worked-Status (automatisches Zurücksetzen)~~ ✅ **Umgesetzt in v1.40** (3-Tage-Lebensdauer, kein manuelles Zurücksetzen mehr nötig)
- Filterung des „Cluster & QSO der anderen"-Fensters auf eigenes QTF
- Weitere Topografie-basierte Berechnungen für die Richtungswarnung
