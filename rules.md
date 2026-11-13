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
      .       .        R = Resonator-Platz (8 Stück, Radius 3)
        R . R
```

- Der **Kern** ist ein Leitstein (Lodestone). Darüber schwebt eine Textanzeige mit
  Name, Fraktion, Portalstufe und Besitzer.
- Rund um den Kern gibt es **8 Resonator-Plätze** (N, NO, O, SO, S, SW, W, NW) im
  Abstand 3. Ein besetzter Platz zeigt einen Block passend zur Resonatorstufe.
- Der Bereich im Radius 4 um den Kern ist geschützt: Niemand kann dort Blöcke abbauen
  oder setzen, außer über die Spielmechanik.

### 2.3 Zustände

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

## 3. Ausrüstung

Alle Spielgegenstände sind normale Minecraft-Items mit einer Markierung
(PersistentDataContainer). Sie können nicht gecraftet, im Amboss verändert oder
außerhalb des Spiels nachgemacht werden.

| Gegenstand | Item | Zweck |
|---|---|---|
| Resonator L1–L8 | Amethystscherbe | Auf einen Resonator-Platz setzen (Rechtsklick auf den Platz) |
| Burst L1–L8 | Feuerkugel | Rechtsklick: Schaden an gegnerischen Resonatoren im Umkreis |
| Portalschlüssel | Trial Key, benannt nach dem Portal | Nötig, um einen Link **zu** diesem Portal zu ziehen |

- Spielgegenstände droppen beim Tod nicht und landen im Spiel-Inventar (`/ig inventar`),
  maximal 500 Stück insgesamt (`player.max-items`).
- Ein Spieler darf nur Resonatoren und Bursts bis zu seiner eigenen Spielerstufe benutzen.

## 4. Hacken

- Rechtsklick auf den Kern eines Portals = **Hacken**.
- Ergebnis: zufällig 2–4 Gegenstände (Resonatoren und Bursts um die Portalstufe herum)
  und mit 30 % Chance ein Portalschlüssel. Am gegnerischen Portal gibt es mehr Gegenstände,
  aber keinen Schlüssel-Bonus.
- Abklingzeit pro Spieler und Portal: 5 Minuten. Höchstens 4 Hacks pro Portal und
  Spieler in 4 Stunden.
- Jeder Hack gibt Aktionspunkte (eigenes/neutrales Portal 100, gegnerisches 150).

## 5. Energie und Angriff

- Jeder Resonator hat Energie: Stufe 1: 1000, 2: 1500, 3: 2000, 4: 2500, 5: 3000,
  6: 4000, 7: 5000, 8: 6000.
- Ein **Burst** wirkt im Radius 2 + 2 × Stufe um den Werfer, Schaden
  100 × Stufe × Abstandsfaktor (1 direkt daneben, 0 am Rand), nur auf gegnerische
  Resonatoren.
- Ein Resonator mit 0 Energie wird zerstört.
- **Verfall**: Jeden Tag um 03:00 verlieren alle Resonatoren 15 % ihrer maximalen
  Energie. Ein unbetreutes Portal stirbt also nach etwa einer Woche.
- **Aufladen**: Mit `/ig aufladen <Portal>` und einem Schlüssel des Portals, oder
  direkt am Kern mit Rechtsklick + Schleichen, kann man eigene Portale aufladen.
  Kosten: XP-Punkte des Spielers (1 XP = 10 Energie).

## 6. Links

- Ein Link ist eine gerade Verbindung zwischen zwei Portalen derselben Fraktion.
- Um von Portal A nach Portal B zu linken:
  - man steht an A (Radius 6 um den Kern),
  - A hat alle 8 Resonatoren,
  - man hat einen Schlüssel von B (der Schlüssel wird verbraucht),
  - B gehört der eigenen Fraktion,
  - die Entfernung ist höchstens die Reichweite von A:
    `64 × Portalstufe²` Blöcke (L1: 64, L2: 256, L3: 576, L4: 1024, L5: 1600),
    gekappt auf `link.max-range` (Standard 2000 Blöcke),
  - der Link kreuzt keinen bestehenden Link (egal welcher Fraktion), berechnet in der
    X/Z-Ebene,
  - A liegt nicht innerhalb eines gegnerischen Feldes,
  - von A gehen höchstens 8 Links aus (eingehende Links unbegrenzt).
- Links werden als Partikelstrahl in Fraktionsfarbe angezeigt, wenn ein Spieler in
  der Nähe ist.
- Ein Link verschwindet, sobald eines seiner beiden Portale neutral wird.

## 7. Felder

- Drei Portale, die paarweise durch Links verbunden sind, bilden automatisch ein **Feld**.
- Entsteht durch einen neuen Link mehr als ein mögliches Feld, werden auf jeder Seite
  des Links das größte mögliche Feld erzeugt (höchstens 2 Felder pro Link).
- Felder werden auf der Karte und als Fraktionsfarbe in der Bossbar angezeigt, wenn ein
  Spieler darunter steht.
- Ein Feld zerfällt, sobald einer seiner drei Links verschwindet.

## 8. Wertung

- **Einfluss** eines Feldes = Fläche in Blöcken² / 100 (Dreiecksfläche in der X/Z-Ebene),
  aufgerundet.
- Überlappende Felder einer Fraktion zählen jeweils voll (wie im Vorbild – Schichten
  lohnen sich).
- **Messpunkte**: Jede volle Stunde wird der Gesamteinfluss jeder Fraktion gespeichert.
- **Zyklus**: Eine Woche (Montag 00:00 bis Sonntag 23:59). Gewinner ist die Fraktion
  mit dem höchsten Durchschnitt der Messpunkte. Ergebnis wird angekündigt und
  gespeichert (`/ig zyklus`, `/ig zyklus alt`).
- Felder und Portale bleiben über Zyklusgrenzen hinweg bestehen.

## 9. Spielerstufe und Aktionspunkte (AP)

| Aktion | AP |
|---|---|
| Resonator setzen | 125 |
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
| `/ig inventar` | Spiel-Inventar öffnen |
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
| `/ig admin geben <spieler> <item> <stufe> [anzahl]` | Gegenstände geben (Test) |
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
