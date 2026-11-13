# Inigresso – Spielregeln

Inigresso ist ein dauerhaft laufendes Territorialspiel für Spigot 26.3, inspiriert vom
Spielprinzip „Orte erobern, verbinden, Gebiete aufspannen“. Zwei Fraktionen kämpfen um
Portale an Bauwerken der Welt. Portale werden von Spielern vorgeschlagen und von Admins
freigegeben.

## 1. Fraktionen

| Fraktion | Farbe | Kurzname |
|---|---|---|
| **Lumen** | Grün | `lumen` |
| **Wächter** | Blau | `waechter` |

- Beim ersten Betreten wählt jeder Spieler mit `/ig fraktion <lumen|waechter>` eine Fraktion.
  Bis dahin kann er nichts erobern, aber herumlaufen und Portale ansehen.
- Die Wahl ist dauerhaft. Wechsel nur per Admin (`/ig admin fraktion <spieler> <fraktion>`),
  dabei verliert der Spieler seine Schlüssel und Ausrüstung aus dem Spiel.
- Ist eine Fraktion um mehr als 30 % größer, wird bei der Wahl ein Hinweis angezeigt
  (kein Zwang). Erst ab 3 Spielern in der kleineren Fraktion, sonst wäre es nur Rauschen.
- Mitgliedschaft wird über die UUID gespeichert.

## 2. Portale

### 2.1 Vorschlagen und Freigeben

- Ein Spieler mit Fraktion steht an einem Bauwerk und schlägt mit
  `/ig vorschlagen <Name>` ein Portal vor. Der Kern kommt genau an die Stelle, an der
  er steht. Gespeichert werden Position, Welt, Name, Vorschlagender und Zeitpunkt.
- Namen: 3–32 Zeichen, nicht nur Ziffern, eindeutig (ohne Groß-/Kleinschreibung) unter
  allen Portalen und offenen Vorschlägen. In Befehlen kann ein Portal per Name oder `#ID`
  angegeben werden.
- Voraussetzungen:
  - mindestens Spielerstufe 2,
  - höchstens 3 offene Vorschläge pro Spieler,
  - Abstand zu allen bestehenden und offenen Portalen mindestens **48 Blöcke**
    (`portal.min-distance`),
  - nur in freigegebenen Welten (`worlds`).
- Admins sehen offene Vorschläge mit `/ig vorschlaege`, können sich mit
  `/ig vorschlag tp <id>` hinteleportieren und mit `/ig annehmen <id>` bzw.
  `/ig ablehnen <id> <Grund>` entscheiden. Der Vorschlagende bekommt eine Nachricht
  (auch beim nächsten Login) und bei Annahme Aktionspunkte.
- Bei Annahme wird das Portal neutral erzeugt (Abschnitt 2.2). Abstand und Name werden
  dabei erneut geprüft, weil inzwischen ein anderes Portal entstanden sein kann.
- Admins können Portale umbenennen, verschieben und löschen.

### 2.2 Aufbau eines Portals

```
        R . R
      .       .
      R   K   R        K = Kern (Lodestone)
      .       .        R = Resonator-Platz (8 Stück, Radius 3, schwebend)
        R . R
```

- Der **Kern** ist ein Leitstein (Lodestone). Darüber schwebt eine Textanzeige mit
  Name, Fraktion, Portalstufe und Besitzer.
- Rund um den Kern gibt es **8 Resonator-Plätze** (N, NO, O, SO, S, SW, W, NW) im
  Abstand 3. Ein besetzter Platz zeigt einen schwebenden, halb großen Block passend zur
  Resonatorstufe, der in der Fraktionsfarbe leuchtet. Die Welt wird dabei nicht verändert,
  damit das Bauwerk am Portal heil bleibt.
- Die Anzeige über dem Kern zeigt bei eroberten Portalen auch Portalstufe und die Zahl der
  Resonatoren.
- Der Bereich im Radius 4 um den Kern ist geschützt: Niemand kann dort Blöcke abbauen
  oder setzen, außer über die Spielmechanik.

### 2.3 Zustände

- **Resonator setzen**: Mit einem Resonator in der Hand Rechtsklick auf den Kern. Er kommt
  auf den ersten freien Platz. Sind alle 8 Plätze belegt, ersetzt er den schwächsten
  Resonator, wenn dieser eine niedrigere Stufe hat (**Aufrüsten**).
- **Neutral**: keine Resonatoren. Jeder Spieler mit Fraktion kann einen Resonator
  setzen und erobert das Portal damit für seine Fraktion.
- **Erobert**: mindestens ein Resonator. Nur die eigene Fraktion kann weitere Resonatoren
  setzen oder aufrüsten. Gegner können Resonatoren nur mit Bursts zerstören.
- Sind alle Resonatoren zerstört, wird das Portal wieder neutral. Alle Links des
  Portals und damit die Felder daran verschwinden.

### 2.4 Portalstufe

- Die Portalstufe ist der Durchschnitt der Resonatorstufen, wobei leere Plätze als 0
  zählen, abgerundet, mindestens 1 sobald ein Resonator steht.
- Ein Spieler darf auf einem Portal von jeder Resonatorstufe nur begrenzt viele setzen:
  Stufe 8: 1, Stufe 7: 1, Stufe 6: 2, Stufe 5: 2, Stufe 4: 4, Stufen 1–3: 8.
  So braucht ein Portal der Stufe 8 acht verschiedene Spieler.

### 2.5 Automatische Portale

- Optional (`auto-portals.enabled`): Das Plugin setzt selbst Portale an der Oberfläche, eins
  je Rasterfeld von 100 × 100 Blöcken, um bis zu 25 Blöcke zufällig, aber immer gleich versetzt.
- Gebaut wird nur auf natürlichem Boden (Gras, Erde, Sand, Stein, Schnee; Bäume in der Nähe
  sind erlaubt) und auf halbwegs ebenem Gelände. Liegt im Schutzbereich ein Bau, Wasser oder ein
  Weg, wird bis zu 12 Blöcke daneben gesucht, sonst bleibt das Feld leer.
- Kein automatisches Portal näher als 50 Blöcke an einem bestehenden Portal.
- Jedes Rasterfeld wird nur einmal geprüft, wenn seine Gegend zum ersten Mal geladen wird.
  Löscht ein Admin ein automatisches Portal, entsteht dort kein neues.
- Namen werden aus Wortteilen gebildet (z. B. „Nebelborn“, „Sternruh“) und lassen sich mit
  `/ig admin portal umbenennen` ändern.

## 3. Ausrüstung

Alle Spielgegenstände sind normale Minecraft-Items mit einer Markierung
(PersistentDataContainer). Sie können nicht gecraftet, im Amboss verändert oder
außerhalb des Spiels nachgemacht werden.

| Gegenstand | Item | Zweck |
|---|---|---|
| Resonator L1–L8 | Amethystscherbe | Rechtsklick auf einen Portalkern: setzen oder aufrüsten |
| Burst L1–L8 | Feuerkugel | Rechtsklick: Schaden an gegnerischen Resonatoren im Umkreis |
| Portalschlüssel | Trial Key, benannt nach dem Portal | Nötig, um einen Link **zu** diesem Portal zu ziehen |

- Spielgegenstände liegen im normalen Inventar. Sie droppen beim Tod nicht, sondern sind
  nach dem Wiederbeleben wieder da. `/ig inventar` zeigt eine Übersicht.
- Höchstens 500 Spielgegenstände im Inventar (`player.max-items`). Was beim Hacken darüber
  hinausgeht oder keinen Platz mehr hat, verfällt. In Truhen gelagerte Gegenstände zählen
  nicht mit.
- Jeder Gegenstand trägt die Fraktion seines Besitzers. Gegenstände der anderen Fraktion
  wirken nicht und werden beim Fraktionswechsel bzw. beim nächsten Login entfernt.
- Spielgegenstände wirken nicht als normale Items: Feuerkugeln legen kein Feuer,
  Schlüssel öffnen keine Tresore, Werfer schießen sie nicht, Werkbank, Amboss und
  Schmiedetisch nehmen sie nicht an.
- Ein Spieler darf nur Resonatoren und Bursts bis zu seiner eigenen Spielerstufe benutzen.

## 4. Hacken

- Rechtsklick auf den Kern eines Portals ohne Resonator oder Burst in der Hand = **Hacken**.
- Ergebnis: zufällig 2–4 Gegenstände, am gegnerischen Portal 3–5. Je zur Hälfte
  Resonatoren oder Bursts, Stufe = Portalstufe −1, ±0 oder +1 (neutrale Portale zählen als
  Stufe 1), begrenzt auf 1–8. Dazu mit 50 % Chance ein Schlüssel dieses Portals.
- Man darf Gegenstände über der eigenen Stufe erbeuten, aber erst mit höherer Stufe benutzen.
- Abklingzeit pro Spieler und Portal: 5 Minuten. Höchstens 4 Hacks pro Portal und
  Spieler in 4 Stunden.
- Jeder Hack gibt Aktionspunkte (eigenes/neutrales Portal 100, gegnerisches 150).

## 5. Energie und Angriff

- Jeder Resonator hat Energie: Stufe 1: 1000, 2: 1500, 3: 2000, 4: 2500, 5: 3000,
  6: 4000, 7: 5000, 8: 6000.
- Ein **Burst** wird mit Rechtsklick (in die Luft oder auf einen Block) gezündet und
  verbraucht. Er wirkt im Radius 2 + 2 × Stufe um den Werfer, Schaden
  100 × Stufe × Abstandsfaktor (1 direkt daneben, 0 am Rand), nur auf gegnerische
  Resonatoren und nur bis 6 Blöcke Höhenunterschied zum Portalkern. Der Abstand wird zum
  jeweiligen Resonator-Platz gemessen, man sollte also nah an die Resonatoren heran.
- Ein Resonator mit 0 Energie wird zerstört.
- **Verfall**: Jeden Tag um 03:00 (Serverzeit) verlieren alle Resonatoren 15 % ihrer
  maximalen Energie. Ein unbetreutes Portal stirbt also nach etwa einer Woche. War der
  Server zu dieser Zeit aus, wird der Verfall beim nächsten Start nachgeholt. Beim allerersten
  Start verfällt nichts rückwirkend.
- Stirbt der letzte Resonator, wird das Portal neutral.
- **Aufladen**: Mit `/ig aufladen <Portal>` und einem Schlüssel des Portals (er wird nicht
  verbraucht), oder direkt am Kern mit Schleichen + Rechtsklick, kann man eigene Portale
  aufladen. Kosten: XP-Punkte des Spielers (1 XP = 10 Energie), höchstens 1000 Energie
  pro Vorgang, die schwächsten Resonatoren zuerst.
- Die Anzeige über dem Kern zeigt die Energie des Portals in Prozent.

## 6. Links

- Ein Link ist eine gerade Verbindung zwischen zwei Portalen derselben Fraktion.
- Um von Portal A nach Portal B zu linken, gibt man `/ig link <B>` ein (Name oder `#ID`):
  - man steht an A (Radius 6 um den Kern),
  - A hat alle 8 Resonatoren,
  - man hat einen Schlüssel von B (der Schlüssel wird verbraucht),
  - B gehört der eigenen Fraktion,
  - die Entfernung ist höchstens die Reichweite von A:
    `64 × Portalstufe²` Blöcke (L1: 64, L2: 256, L3: 576, L4: 1024, L5: 1600),
    gekappt auf `link.max-range` (Standard 2000 Blöcke),
  - der Link kreuzt keinen bestehenden Link (egal welcher Fraktion), berechnet in der
    X/Z-Ebene; ein Link, der genau durch ein anderes Portal oder über einen anderen Link
    läuft, zählt ebenfalls als Kreuzung,
  - A liegt nicht innerhalb eines gegnerischen Feldes,
  - von A gehen höchstens 8 Links aus (eingehende Links unbegrenzt).
- Links werden als Partikelstrahl in Fraktionsfarbe angezeigt, wenn ein Spieler in
  der Nähe ist (nur der Abschnitt im Umkreis von 64 Blöcken).
- `/ig portal` zeigt Resonatoren, Energie, Reichweite und Links eines Portals.
- Ein Link verschwindet, sobald eines seiner beiden Portale neutral wird.

## 7. Felder

- Drei Portale, die paarweise durch Links verbunden sind, bilden automatisch ein **Feld**.
- Entsteht durch einen neuen Link mehr als ein mögliches Feld, werden auf jeder Seite
  des Links das größte mögliche Feld erzeugt (höchstens 2 Felder pro Link).
- Wer unter einem Feld steht, sieht eine Bossbar in Fraktionsfarbe mit Einfluss und Zahl
  der Schichten. Neue Felder werden allen Spielern im Chat gemeldet. (Anzeige auf der
  Karte: später, siehe implement.md.)
- Ein Feld zerfällt, sobald einer seiner drei Links verschwindet.

## 8. Wertung

- **Einfluss** eines Feldes = Fläche in Blöcken² / 100 (Dreiecksfläche in der X/Z-Ebene),
  aufgerundet.
- Überlappende Felder einer Fraktion zählen jeweils voll (wie im Vorbild – Schichten
  lohnen sich).
- **Messpunkte**: Jede volle Stunde wird der Gesamteinfluss jeder Fraktion gespeichert.
  War der Server aus, fehlen diese Messpunkte (es wird nichts erfunden).
- **Zyklus**: Eine Woche (Montag 00:00 bis Sonntag 23:59, Serverzeit). Gewinner ist die
  Fraktion mit dem höchsten Durchschnitt der Messpunkte, bei Gleichstand (auch ohne
  Messpunkte) unentschieden. Das Ergebnis wird allen im Chat angekündigt und gespeichert
  (`/ig stand`, `/ig zyklus alt`). Verpasste Zyklusenden werden beim Start nachgeholt.
- Starttag, Zykluslänge und Messabstand sind einstellbar (`cycle.start-day`,
  `cycle.length-hours`, `cycle.measure-minutes`), z. B. für kurze Testrunden. Nach einer
  Änderung wird der laufende Zyklus einmal verkürzt, damit er wieder in den Takt passt.
- Felder und Portale bleiben über Zyklusgrenzen hinweg bestehen.

## 9. Spielerstufe und Aktionspunkte (AP)

| Aktion | AP |
|---|---|
| Resonator setzen | 125 |
| Resonator aufrüsten | 65 |
| Portal erobern (erster Resonator) | 500 |
| Gegnerischen Resonator zerstören | 75 |
| Link ziehen | 313 |
| Feld erzeugen | 1250 |
| Hacken (eigenes/neutrales / gegnerisches Portal) | 100 / 150 |
| Portal aufladen | 10 |
| Vorschlag angenommen | 1000 |

Stufen: 1 = 0 AP, 2 = 2 500, 3 = 20 000, 4 = 70 000, 5 = 150 000, 6 = 300 000,
7 = 600 000, 8 = 1 200 000.

## 10. Befehle

### Spieler (`inigresso.use`, Standard: alle)

| Befehl | Wirkung |
|---|---|
| `/ig fraktion [lumen\|waechter]` | Fraktion wählen (einmalig); ohne Angabe: Größe beider Fraktionen |
| `/ig info` | Eigene Stufe, AP, Fraktion, Inventar-Füllstand |
| `/ig portal` | Infos zum nächsten Portal im Umkreis von 16 Blöcken |
| `/ig vorschlagen <Name>` | Portal vorschlagen |
| `/ig link <Schlüssel-Portal>` | Link vom aktuellen Portal ziehen |
| `/ig aufladen <Portal>` | Portal über Schlüssel aufladen |
| `/ig inventar` | Übersicht der eigenen Spielgegenstände |
| `/ig stand` | Aktueller Einfluss beider Fraktionen, Zyklusstand |
| `/ig zyklus [alt]` | Ergebnis laufender/vergangener Zyklen |

### Admin (`inigresso.admin`, Standard: op)

| Befehl | Wirkung |
|---|---|
| `/ig vorschlaege` | Offene Vorschläge anzeigen |
| `/ig vorschlag tp <id>` | Zum Vorschlag teleportieren |
| `/ig annehmen <id>` / `/ig ablehnen <id> <Grund>` | Vorschlag entscheiden |
| `/ig admin portal liste` | Alle Portale mit ID und Position |
| `/ig admin portal <umbenennen\|verschieben\|loeschen\|neutral\|tp> <#id\|Name> …` | Portale verwalten; `verschieben` setzt den Kern an die eigene Position, `loeschen` braucht zusätzlich `bestaetigen` |
| `/ig admin fraktion <spieler> <fraktion>` | Fraktion wechseln |
| `/ig admin geben <spieler> <resonator\|burst\|schluessel> <stufe\|#portal> [anzahl]` | Gegenstände geben (Test) |
| `/ig admin ap <spieler> <+/-menge>` | AP geben/abziehen (Test, Korrektur) |
| `/ig info <spieler>` | Profil eines anderen Spielers ansehen |
| `/ig reload` | Konfiguration neu laden |

## 11. Technik

- Spigot 26.3, Java 25, Maven.
- Speicherung in SQLite (`plugins/Inigresso/inigresso.db`) über den in Spigot
  mitgelieferten sqlite-jdbc-Treiber; Schreibzugriffe asynchron, Spielzustand im
  Speicher.
- Geometrie (Link-Kreuzung, Dreiecksfläche, Punkt-im-Dreieck) als eigene, testbare
  Klasse ohne Bukkit-Abhängigkeit.
- Admin-Aktionen mit Spielfolgen (Portal löschen, neutral setzen) werden protokolliert.
