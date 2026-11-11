# Contest-Workflow mit KST4Contest

> [English version](en-Contest-Workflow) | Du liest gerade die deutsche Version

KST4Contest fasst Chat, Stationsauswahl, bekannte QRGs, Worked-Status, Skeds, Aircraft-Scatter-Zeiten und weitere Stationsdaten in einer gemeinsamen Oberfläche zusammen. Der Nutzen entsteht nicht aus einer einzelnen Anzeige, sondern aus dem Zusammenspiel dieser Informationen während des laufenden Contests.

Diese Seite beschreibt einen vollständigen Arbeitsablauf. Die einzelnen Funktionen und ihre technischen Grenzen werden weiterhin in den Kapiteln [Funktionen](de-Funktionen), [Benutzeroberfläche](de-Benutzeroberflaeche), [Log-Synchronisation](de-Log-Synchronisation) und [AirScout-Integration](de-AirScout-Integration) erläutert.

---

## Zweck und Grenzen

KST4Contest soll die Zeit zwischen einer erkannten Möglichkeit und dem tatsächlichen QSO verkürzen.

Das Programm kann unter anderem anzeigen:

- welche Stationen aktiv sind,
- auf welchen Bändern und QRGs sie zuletzt erkannt wurden,
- welche Stationen bereits gearbeitet wurden,
- welche zusätzlichen Bänder noch infrage kommen,
- welche Kandidaten zur aktuellen Antennenrichtung passen,
- wann ein Aircraft-Scatter-Fenster erwartet wird und
- welche Station gerade in eine für die eigene Station brauchbare Richtung arbeitet.

Diese Angaben bleiben Entscheidungshilfen. Ein hoher Prioritätsscore ist keine QSO-Wahrscheinlichkeit. Auch eine von AirScout mit 100 % bewertete Reflexionsgeometrie garantiert kein QSO. Ob eine QRG tatsächlich frei ist, die Gegenstation zuhört und der Funkweg unter den aktuellen Bedingungen funktioniert, muss der Operator weiterhin selbst beurteilen.

---

## Vor dem Contest

Die wesentlichen Einstellungen sollten nicht erst unmittelbar vor dem ersten interessanten Sked geprüft werden.

### Grundkonfiguration

Prüfe mindestens:

- eigenes Rufzeichen, Passwort und Locator,
- primäre Chat-Kategorie,
- Login und Einstellungen der zweiten Kategorie, falls sie verwendet wird,
- lokal aktive Bänder,
- Antennenöffnungswinkel,
- maximale sinnvolle Entfernung,
- `MYQRG` und gegebenenfalls `SECONDQRG`,
- Log-Synchronisation und
- benötigte Shortcuts, Snippets und Nachrichtenvariablen.

Antennenöffnungswinkel und maximale Entfernung sind stationsabhängig. Bei DM5M wird die reale Antennenanlage beispielsweise mit einem Öffnungswinkel von 69° und einer maximalen Entfernung von 900 km abgebildet. Das sind keine allgemeinen Vorgabewerte.

Speichere dauerhafte Änderungen mit **Save Settings**. Nach dem Verbindungsaufbau sollte der `LINK`-Indikator grün sein. Erst dann sind Anmeldung und Benutzerlistensynchronisation vollständig abgeschlossen.

### Automatische Antworten

Die automatische QRG-Antwort gehört zum aktiven Contest-Workflow. Sie beantwortet wiederkehrende QRG-Anfragen und nimmt dem Chatter damit einen Teil der Routinearbeit ab.

Davon zu unterscheiden ist die allgemeine automatische Antwort. Sie kann auf sämtliche eingehenden Anfragen reagieren und ist vor allem dann sinnvoll, wenn die Station vorübergehend nicht QRV ist oder nicht am Sked-Betrieb teilnehmen möchte. Sie erspart sowohl der eigenen Station als auch den anfragenden Stationen unnötige Folgefragen.

### Optionale Anbindungen

Aktiviere nur die Schnittstellen, die tatsächlich verwendet und vorher getestet wurden:

- Logprogramm beziehungsweise Simplelogfile,
- TRX-Synchronisation,
- AirScout,
- PSTRotator,
- Win-Test-Skedübergabe und
- lokaler DX-Cluster-Server.

Ein Contest ist ein ungünstiger Zeitpunkt, um gleichzeitig die Funkbedingungen und eine erstmals aktivierte Netzwerkschnittstelle zu untersuchen.

Beim Simplelogfile sollte vor jedem Contest geprüft werden, ob das Logprogramm die aktuelle Contestdatei an den in KST4Contest ausgewählten Pfad schreibt. Ein Test-QSO muss innerhalb einer Minute als global gearbeitet erscheinen. KST4Contest setzt die aus dieser Datei abgeleiteten Worked-Markierungen beim Contestwechsel nicht automatisch zurück.

---

## Grundablauf während des Contests

Der typische Ablauf wiederholt sich:

1. CQ rufen oder einen vereinbarten Sked durchführen.
2. Chat, Prioritätsliste, Karte und AP-Timeline beobachten.
3. Einen geeigneten Kandidaten auswählen.
4. Eigene oder fremde QRG festlegen.
5. QSO versuchen.
6. Erfolgreiches QSO sofort loggen.
7. Eine weitere Bandmöglichkeit prüfen.
8. Einen erfolglosen, aussagekräftigen Versuch mit **Sked fail** kennzeichnen.
9. Zum CQ-Betrieb oder zum nächsten Kandidaten zurückkehren.

KST4Contest hält die benötigten Informationen zwischen diesen Schritten zusammen. Das eigentliche Umschalten, Rufen, Hören und Entscheiden bleibt bewusst beim Operator.

---

## CQ-Betrieb

Bei einer weitgehend festen CQ-QRG sollten `MYQRG` beziehungsweise `SECONDQRG` den tatsächlich verwendeten Frequenzen entsprechen. Eine aktivierte und tatsächlich liefernde TRX-Synchronisation kann `MYQRG` automatisch aktualisieren. Die Aktivierung allein reicht nicht: Vor dem Contest sollte mit einer realen Frequenzänderung geprüft werden, ob gültige Pakete ankommen. Ohne funktionierende automatische Quelle muss der Wert von Hand gepflegt werden.

Der Beacon kann die aktuelle QRG, den Locator und die Antennenrichtung regelmäßig im Chat veröffentlichen. Seine Variablen werden bei jedem Sendedurchlauf erneut ausgewertet.

Wird während des CQ-Betriebs über mehrere Frequenzen gescannt, sollte der Beacon deaktiviert werden. Eine automatisch veröffentlichte QRG ist nur hilfreich, solange sie noch stimmt. Liefert das Logprogramm die Frequenz, schaltet KST4Contest den Beacon bei einer Abweichung von mehr als 2 kHz [selbst ab](de-Konfiguration#automatische-abschaltung-beim-frequenzwechsel). Nach jedem Programmstart ist der Beacon aus und muss neu eingeschaltet werden.

Shortcuts und Snippets sollten die regelmäßig benötigten Nachrichten abdecken, beispielsweise:

- Bitte auf der eigenen QRG hören,
- QRG der Gegenstation erfragen,
- Wechsel zur QRG der Gegenstation ankündigen,
- Antennenrichtung bestätigen und
- einen Sked vorschlagen.

Einzelheiten stehen unter [Makros und Variablen](de-Makros-und-Variablen).

---

## Kandidaten auswählen

Die Benutzerliste kann mit QTF-, QRB-, Worked-, Band-, Aktivitäts-, New-Bands-, Tropo- und AirScout-Filtern auf den aktuellen Betriebszustand begrenzt werden.

Die Prioritätsliste und die AP-Timeline ergänzen diese Auswahl:

- Der Prioritätsscore fasst mehrere bekannte Kriterien zusammen.
- Die AP-Timeline ordnet Skeds und erwartete Aircraft-Scatter-Möglichkeiten zeitlich ein.
- Die Stationskarte zeigt die geografische Lage, Antennenrichtung und den Funkweg.
- Der Worked- und Bandstatus verhindert unnötige Doppelarbeit.

![Prioritätsliste und Bewertungsinformationen](priority_score_overview.png)

Der Score ist eine Sortierhilfe. Prüfe vor einem Versuch weiterhin Rufzeichen, Kategorie, Band, QRG, Richtung, Entfernung und die Aktualität der zugrunde liegenden Informationen.

Eine bewusste Auswahl in Benutzerliste, Prioritätsliste, Timeline oder Karte übernimmt den konkreten Chatmember. Dabei bleiben das vollständige sichtbare Rufzeichen und die zugehörige Chat-Kategorie erhalten. KST4Contest bereitet anschließend `/cq RUFZEICHEN` im Sendfeld vor.

---

## Eigene oder fremde QRG verwenden

Wenn eine gute Ausbreitungsrichtung erkannt wird und ein passendes Flugzeug für einen Kandidaten vorhanden ist, wird die Gegenstation zunächst häufig auf die eigene QRG gebeten. Das ist besonders sinnvoll, wenn dort bereits CQ gerufen wird und die Station ohne weiteren Umbau sofort empfangen kann.

Reagiert die Gegenstation nicht, ist ihre eigene QRG geeigneter oder kann sie die angefragte QRG nicht verwenden, wird gewechselt. KST4Contest hält die zuletzt erkannten QRGs bereit, damit der Operator nicht erneut den gesamten Chatverlauf durchsuchen muss.

Auch ein gezielter Versuch auf der QRG eines Sked-Partners ist Teil des normalen Workflows. Entscheidend ist nicht, grundsätzlich auf der eigenen QRG zu bleiben, sondern die vorhandene Möglichkeit mit möglichst wenig Verzögerung zu nutzen.

Vor dem Wechsel sollten mindestens geprüft werden:

- korrektes Band,
- vollständiges Zielrufzeichen,
- QRG der Gegenstation,
- Antennenrichtung,
- erwartetes Aircraft-Scatter-Fenster und
- Belegung der QRG.

---

## Richtungsgelegenheiten nutzen

Eine gerichtete Nachricht zwischen zwei anderen Stationen kann zeigen, dass der Absender seine Antenne ungefähr in Richtung des Empfängers ausgerichtet hat. Passt diese Richtung auch zur eigenen Station, markiert KST4Contest den Absender vorübergehend grün und fett.

![Grün und fett markierte Richtungsgelegenheit](direction_opportunity_highlight.png)

Die Markierung erscheint in der Benutzerliste und den zugehörigen Ansichten. Bei geeigneter Cluster-Konfiguration und bekannter QRG kann die Gelegenheit zusätzlich über den lokalen DX-Cluster ausgegeben werden.

Damit stehen im entscheidenden Moment bereits mehrere Informationen zur Verfügung:

- vollständiges Rufzeichen,
- Locator und Richtung,
- zuletzt erkannte QRG,
- Bandinformationen,
- AirScout-Daten und
- die aktuelle Reachability- beziehungsweise Tropo-Auswertung.

Der Operator muss diese Daten nicht erst zusammensuchen. Er muss lediglich entscheiden, ob die Gelegenheit den laufenden CQ-Betrieb kurz unterbrechen darf.

Bei DM5M lag die Erfolgsquote solcher opportunistischen Versuche nach der bisherigen praktischen Auswertung ungefähr bei 35–40 %. Dieser Wert beschreibt die Erfahrung einer konkreten Station. Er ist keine allgemeine Erfolgsprognose und hängt unter anderem von Band, Entfernung, Stationsausrüstung, Reaktionszeit und Ausbreitungsbedingungen ab.

Nach dem Versuch kann unmittelbar weiter CQ gerufen oder mit dem nächsten Sked fortgefahren werden.

---

## Skeds planen und auswerten

Ein Sked sollte mit dem tatsächlich vorgesehenen Band und einer realistischen Uhrzeit eingetragen werden. KST4Contest übernimmt ihn in die eigene Sked-Verwaltung und berücksichtigt ihn bei Erinnerungen, Timeline und Prioritätsberechnung.

Die Skeds werden nur für die laufende Programmsitzung verwaltet. Sie sind kein dauerhafter Ersatz für Contestlog oder Notizen.

Ist die Win-Test-Anbindung aktiviert, versucht KST4Contest den Sked zusätzlich an Win-Test zu übergeben. Fehlt eine verwendbare QRG oder passt das Band nicht, bleibt der interne Sked trotzdem erhalten. Lediglich die zusätzliche Übergabe kann dann entfallen.

### Fehlgeschlagene 100-%-Airplane-Skeds

Scheitert ein sorgfältig vorbereiteter Versuch trotz einer von AirScout mit 100 % bewerteten Reflexionsgeometrie, sollte die Station mit **Sked fail** gekennzeichnet werden.

Die 100-%-Anzeige ist keine Erfolgswahrscheinlichkeit. Ein Fehlschlag unter diesen Bedingungen ist aber ein brauchbarer betrieblicher Hinweis darauf, dass der Funkweg mit der aktuellen Stationskonfiguration und den aktuellen Bedingungen nicht funktioniert hat.

Die Kennzeichnung reduziert die Priorität der Station für den Rest der laufenden Sitzung. Dadurch können zunächst Kandidaten bearbeitet werden, für die noch keine vergleichbar deutliche negative Betriebserfahrung vorliegt.

**Sked fail** darf nicht als dauerhafte Aussage verstanden werden, dass die Station grundsätzlich nicht erreichbar ist. Andere Bedingungen, ein anderes Band oder eine geänderte Stationskonfiguration können zu einem anderen Ergebnis führen. Die Kennzeichnung kann zurückgesetzt werden und bleibt nicht über einen Programmneustart erhalten.

---

## Nach jedem QSO: Log und weiteres Band

Ein erfolgreiches QSO sollte sofort im angebundenen Logprogramm eingetragen werden. Nur dann können Worked-Status, Bandstatus, Filter und Prioritätsbewertung zeitnah aktualisiert werden.

Welche Details übernommen werden können, hängt von der Logquelle ab. Einige Schnittstellen liefern Band, QRG und Locator, während einfachere Quellen nur einen globalen Worked-Status melden.

Unmittelbar nach jedem Logeintrag sollte geprüft werden, ob für dieselbe Station ein weiteres gemeinsames, lokal aktiviertes und noch nicht gearbeitetes Band vorhanden ist. KST4Contest weist darauf mit `BAND+` und den Bandinformationen der Station hin, soweit die vorhandenen Daten eine solche Bewertung erlauben.

Diese Prüfung ist in allen Multiband-Betriebsarten sinnvoll. Die Gegenstation kann direkt mit einer konkreten Band- und Frequenzangabe weiterkoordiniert werden, bevor sie ihre Antenne wieder wegdreht oder einen anderen Sked beginnt.

Im Klartext: Das nächste mögliche QSO sollte geprüft werden, solange die Gegenstation noch erreichbar und der gemeinsame Kontext noch vorhanden ist.

Ist die Station auf einem angezeigten Band tatsächlich nicht QRV, sollte das Band als NOT QRV markiert werden. Dadurch verschwindet die unbrauchbare Möglichkeit aus Filtern und Bewertung, statt bei jeder Aktualisierung erneut aufzutauchen.

---

## Mehrkategorien- und Multibandbetrieb

Der Mehrkategorienbetrieb ist bei Multibandstationen keine Nebenfunktion. Sein wesentlicher Vorteil besteht darin, dass Informationen aus zwei Chat-Kategorien in einem gemeinsamen Arbeitsablauf ausgewertet werden.

Besonders groß ist der Vorteil bei:

- Einmann-Multibandstationen,
- Multi-Operator-Multibandstationen mit einem zentralen Chat-Koordinator und
- Stationen, die nach einem QSO regelmäßig direkt ein weiteres Band versuchen.

Worked-Status, bekannte Bandaktivitäten und Band Opportunities können gemeinsam bewertet werden. Das konkrete Nachrichtenziel behält trotzdem sein vollständiges Rufzeichen und seine Chat-Kategorie.

Dadurch kann der Chatter eine Station unmittelbar vom ersten QSO auf das nächste Band koordinieren, ohne Rufzeichen, QRG und Bandstatus erneut zusammensuchen zu müssen. Im praktischen Betrieb kann daraus eine sehr schnelle Folge nutzbarer QSO-Möglichkeiten entstehen. Genau an dieser Stelle entfaltet der Mehrkategorienbetrieb seinen größten Workflow-Vorteil.

Auch im Einmannbetrieb bleibt diese Arbeitsweise wirksam. Der Operator muss den Bandwechsel zwar selbst durchführen, erhält aber die nächste sinnvolle Möglichkeit bereits vorbereitet.

Multi-Multi-Stationen mit mehreren gleichzeitig arbeitenden Chattern profitieren ebenfalls. Dort müssen Zuständigkeiten, Bandwechsel und bereits laufende Anfragen allerdings klar koordiniert werden. Mehrere Chatter mit denselben Informationen sind hilfreich; mehrere widersprüchliche Sked-Anfragen an dieselbe Station eher nicht.

---

## Praxisbeispiele

### DM5M: Erst CQ, später mehr Skeds

Bei DM5M wird während der ersten vier bis fünf Stunden eines VHF-/UHF-Contests überwiegend CQ gerufen. Der Chat wird beobachtet, aber nur für wenige gezielte Eingriffe verwendet.

Später nimmt der Sked-Betrieb deutlich zu. Gute Ausbreitungsrichtungen, passende Aircraft-Scatter-Fenster, noch nicht gearbeitete Stationen und zusätzliche Bandmöglichkeiten werden dann gezielt miteinander kombiniert.

Eine geeignete Station wird zunächst auf die eigene QRG gebeten. Reagiert sie nicht oder ist die QRG bei der Gegenstation nicht verwendbar, wechselt DM5M auf deren QRG. Auch geplante Versuche direkt auf der QRG eines Sked-Partners gehören dazu.

Eine grün und fett markierte Richtungsgelegenheit kann den CQ-Betrieb kurzfristig unterbrechen. Nach dem Versuch wird unmittelbar weitergerufen oder der nächste Sked bearbeitet.

Dieser Ablauf ist ein Praxisbeispiel und keine verpflichtende Betriebsart. Andere Stationen können wesentlich früher skedden, dauerhaft zwischen QRGs wechseln oder den Chat von Beginn an intensiver nutzen.

### G1YBB: Richtungsgelegenheiten systematisch abarbeiten

G1YBB verwendet die Richtungsanzeige besonders konsequent. Grün markierte Stationen werden systematisch geprüft und nach Möglichkeit gearbeitet, während parallel der normale CQ-Betrieb weiterläuft.

KST4Contest automatisiert dabei nicht das QSO. Der Vorteil besteht darin, dass QRG, Richtung, Aircraft-Scatter-Informationen und weitere Bewertungsdaten bereits vorliegen, wenn die Gelegenheit entsteht. Die verbleibende Aufgabe ist eine schnelle betriebliche Entscheidung.

Eine weitere, besonders konsequente Betriebsweise nutzt die Stationskarte als geografische Arbeitsliste:

1. Der **wkd**-Filter blendet bereits gearbeitete Rufzeichen aus der Benutzerliste und damit auch aus der Karte aus.
2. G1YBB wählt eine interessante Station auf der Karte aus.
3. **Trigger cluster spot** übergibt die bekannte QRG an Minos.
4. Ein Klick auf den Spot wechselt in Minos auf diese QRG.
5. Nach dem QSO wird der Kontakt geloggt.
6. Die Log-Synchronisation aktualisiert den Worked-Status; der Filter entfernt die Station anschließend aus Benutzerliste und Karte.

Die Karte wird so zu einer räumlichen Liste der noch abzuarbeitenden Stationen. Dieser Ablauf ist optional. Er setzt eine zuverlässig eingerichtete Log-Synchronisation und DX-Cluster-Verbindung voraus und ist vor allem für Operatoren interessant, die den Contest bewusst auf diese geografische Weise strukturieren möchten.

[G1YBB zeigt diesen Workflow im Video.](https://www.youtube.com/watch?v=lMQZMiSHlUI)

---

## Optionale Schnittstellen im Workflow

| Schnittstelle | Aufgabe im Contest |
|---|---|
| [Log-Synchronisation](de-Log-Synchronisation) | Aktualisiert Worked- und Bandstatus nach dem QSO |
| [AirScout](de-AirScout-Integration) | Liefert Aircraft-Scatter-Kandidaten und erwartete Zeitfenster |
| [PSTRotator](de-Konfiguration) | Übernimmt oder setzt die Antennenrichtung |
| [Win-Test](de-Log-Synchronisation) | Kann angelegte Skeds zusätzlich an Win-Test übergeben |
| [DX-Cluster-Server](de-DX-Cluster-Server) | Übergibt erkannte Möglichkeiten an verbundene Logprogramme |
| [Stationskarte](de-Benutzeroberflaeche) | Zeigt Stationen, Richtungen, Auswahl und Funkweg |

Keine dieser Schnittstellen ist für den grundlegenden Chatbetrieb zwingend erforderlich. Ihr Wert entsteht dann, wenn sie zuverlässig eingerichtet ist und eine konkrete manuelle Aufgabe verkürzt.

---

## Was KST4Contest nicht entscheidet

KST4Contest entscheidet nicht:

- ob eine QRG tatsächlich frei ist,
- ob die Gegenstation gerade hören kann,
- ob ein Flugzeug ein QSO ermöglicht,
- ob eine berechnete Funkstrecke unter den aktuellen Bedingungen funktioniert,
- ob ein laufender CQ-Ruf für eine Gelegenheit unterbrochen werden sollte oder
- welcher Kandidat für die aktuelle Conteststrategie den größten Wert besitzt.

Das Programm stellt die vorhandenen Informationen zusammen und hält sie aktuell. Die letzte Entscheidung bleibt beim Operator. Das ist keine Einschränkung des Workflows, sondern der Teil, für den weiterhin Funkbetrieb statt Tabellenkalkulation betrieben wird.
