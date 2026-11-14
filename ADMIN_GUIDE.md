# Inigresso – Anleitung für Admins

Diese Anleitung richtet sich an alle, die Inigresso auf einem Server betreiben. Die Spielregeln
stehen in [rules.md](rules.md), die bebilderte Spieleranleitung unter
<https://cndrbrbr.github.io/inigresso/>.

## 1. Installation

1. Voraussetzung: **Spigot 26.3** mit Java 25. Weitere Plugins sind nicht nötig; die
   SQLite-Datenbank nutzt den Treiber, der in Spigot steckt.
2. `inigresso-<version>.jar` nach `plugins/` kopieren und den Server **neu starten**
   (kein `/reload` des ganzen Servers).
3. Beim ersten Start entstehen `plugins/Inigresso/config.yml` und
   `plugins/Inigresso/inigresso.db`.
4. In `config.yml` unter `worlds` die Welten eintragen, in denen gespielt wird
   (Standard: `[world]`), danach `/ig reload`.

**Update:** alte Jar löschen, neue hineinlegen, Server neu starten. Die Datenbank wird beim
Start automatisch auf den neuen Stand gebracht (im Log: `Datenbank-Migration V… angewendet`).
Ein Zurück auf eine ältere Version ist danach nicht vorgesehen – vorher sichern (Abschnitt 6).

## 2. Erste Schritte nach der Installation

1. **Portale anlegen.** Zwei Wege, auch kombiniert:
   - *Vorschläge:* Spieler ab Stufe `proposal.min-level` stellen sich an ein Bauwerk und
     schreiben `/ig vorschlagen <Name>`. Du prüfst mit `/ig vorschlaege` und
     `/ig vorschlag tp <id>` und entscheidest mit `/ig annehmen <id>` oder
     `/ig ablehnen <id> <Grund>`. Der Spieler erfährt die Entscheidung (auch offline).
   - *Automatische Portale:* `auto-portals.enabled: true` setzt beim ersten Laden eines
     Chunks je Rasterzelle (`spacing` × `spacing` Blöcke) ein Portal auf natürlichen Boden.
     Bauten werden übersprungen. Gilt nur für Chunks, die nach dem Einschalten zum ersten Mal
     geprüft werden.
2. Für die ersten Spieler: Neue Spieler starten auf Stufe 1 und dürfen erst ab Stufe 2
   vorschlagen. Für einen Testlauf `proposal.min-level: 1` setzen.
3. Prüfen, ob alles läuft: `/ig stand`, `/ig admin portal liste`.

## 3. Befehle für Admins

Alle brauchen `inigresso.admin` (Standard: op).

| Befehl | Wozu |
|---|---|
| `/ig vorschlaege` | Offene Vorschläge |
| `/ig vorschlag tp <id>` | Zum Vorschlag teleportieren |
| `/ig annehmen <id>` | Vorschlag annehmen → Portal entsteht |
| `/ig ablehnen <id> <Grund>` | Vorschlag ablehnen |
| `/ig admin portal liste` | Alle Portale mit Nummer und Position |
| `/ig admin portal tp <#id\|Name>` | Zum Portal teleportieren |
| `/ig admin portal umbenennen <#id> <Name>` | Umbenennen |
| `/ig admin portal verschieben <#id>` | Kern an die eigene Position setzen (Links fallen weg) |
| `/ig admin portal neutral <#id>` | Resonatoren entfernen, Portal wird neutral |
| `/ig admin portal loeschen <#id> bestaetigen` | Portal ganz entfernen |
| `/ig admin fraktion <Spieler> <lumen\|waechter>` | Fraktion wechseln (Spielgegenstände der alten Fraktion verfallen) |
| `/ig admin geben <Spieler> <resonator\|burst\|schluessel> <Stufe\|#Portal> [Anzahl]` | Gegenstände geben (Tests, Ersatz) |
| `/ig admin ap <Spieler> <+/-Menge>` | AP korrigieren |
| `/ig info <Spieler>` | Profil eines Spielers |
| `/ig reload` | `config.yml` neu laden |

Alle Eingriffe mit Spielfolgen landen im **Audit-Log** (Tabelle `audit` in der Datenbank und
im Server-Log als `[Audit] …`).

Rechte: `inigresso.use` (alle Spielerbefehle, Standard: alle), `inigresso.admin` (op),
`inigresso.bypass` (in geschützten Portalbereichen bauen, op). Portalkerne selbst sind auch
mit Bypass nicht abbaubar – dafür `/ig admin portal loeschen`.

## 4. Konfiguration (`config.yml`)

Nach Änderungen `/ig reload`. Werte, die nicht passen, fallen auf den Standard zurück (mit
Warnung im Log).

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `worlds` | `[world]` | Welten, in denen vorgeschlagen und automatisch gesetzt wird |
| `portal.min-distance` | 48 | Mindestabstand zwischen Portalen (Blöcke) |
| `portal.protect-radius` | 4 | Schutzzone um den Kern: kein Bauen, keine Explosionen |
| `portal.resonator-radius` | 3 | Abstand der Resonatoren vom Kern |
| `portal.resonator-blocks` | Kupfer … Diamant | Aussehen der Resonatoren L1–L8 (genau 8 Blöcke) |
| `proposal.min-level` | 2 | Ab dieser Spielerstufe darf man vorschlagen |
| `proposal.max-open` | 3 | Offene Vorschläge je Spieler |
| `link.max-range` | 2000 | Obergrenze der Link-Reichweite (Blöcke) |
| `link.max-outgoing` | 8 | Links, die von einem Portal ausgehen dürfen |
| `link.beam-height` | 5.0 | Höhe des Partikelstrahls über dem Kern |
| `decay.percent-per-day` | 15 | Energieverlust der Resonatoren pro Tag |
| `decay.time` | `"03:00"` | Uhrzeit des Verfalls (Serverzeit); verpasste Tage werden nachgeholt |
| `hack.cooldown-minutes` | 5 | Wartezeit je Spieler und Portal |
| `hack.max-per-4h` | 4 | Hacks je Spieler und Portal in 4 Stunden |
| `hack.key-chance` | 0.50 | Chance auf einen Portalschlüssel |
| `player.max-items` | 500 | Höchstzahl Spielgegenstände je Spieler |
| `cycle.start-day` | `MONDAY` | Beginn des Wertungszyklus |
| `cycle.length-hours` | 168 | Zykluslänge (zum Testen z. B. 1) |
| `cycle.measure-minutes` | 60 | Abstand der Messpunkte (zum Testen z. B. 5) |
| `auto-portals.enabled` | false | Automatische Portale an |
| `auto-portals.spacing` | 100 | Rasterweite (mind. 16) |
| `auto-portals.jitter` | 25 | Zufälliger Versatz in der Zelle |
| `scoreboard.enabled` | true | Scoreboard rechts für alle (Spieler können es mit `/ig scoreboard aus` für sich ausblenden) |

**Testlauf mit wenigen Leuten:** `proposal.min-level: 1`, `cycle.length-hours: 24`,
`cycle.measure-minutes: 10`, `auto-portals.enabled: true`. Mit `/ig admin geben` und
`/ig admin ap` lassen sich Stufen und Gegenstände schnell herstellen.

## 5. Was Spieler sehen

- **Portal:** Leitstein mit Leuchtumriss in Fraktionsfarbe (grün/blau/grau), darüber Name,
  Fraktion, Stufe, Energie und Nummer; Resonatoren schweben als kleine Blöcke im Ring.
  Es werden keine Weltblöcke außer dem Leitstein verändert.
- **Links und Felder:** Partikelstrahlen und -flächen in Fraktionsfarbe; unter einem Feld
  eine Bossbar.
- **Scanner:** Karte mit allen Portalen (inkl. Nummer), Links und Feldern. Spieler bekommen
  ihn bei der Fraktionswahl, Ersatz mit `/ig scanner`. Alle Scanner teilen eine Karten-ID
  (in der Datenbank gespeichert).
- **Scoreboard** rechts, **Navigation** mit `/ig karte` und `/ig ziel`.

## 6. Sicherung und Zurücksetzen

Der ganze Spielstand steckt in `plugins/Inigresso/inigresso.db` (Portale, Resonatoren,
Spieler, Links, Felder, Zyklen, Audit). Die Anzeigen (Texte, Resonatoren, Leuchten) werden
nicht gespeichert, sondern beim Laden der Chunks neu erzeugt.

- **Sichern:** Server stoppen und die Datei kopieren. Im laufenden Betrieb geht es mit
  `sqlite3 inigresso.db ".backup sicherung.db"`.
- **Neue Saison:** Server stoppen, `inigresso.db` umbenennen, Server starten. Leitsteine der
  alten Portale bleiben in der Welt stehen und sind dann normale Blöcke.
- **Einzelne Portale aufräumen:** `/ig admin portal neutral` oder `loeschen`.

## 7. Leistung

Gemessen (0.9.3) mit 200 Portalen, 509 Links und 315 Feldern, ohne Netzwerk:

| Aufgabe | Zeit | Wie oft |
|---|---|---|
| Scanner-Bild zeichnen | ca. 0,26 ms | je Spieler mit Scanner höchstens 10×/s, nur bei Bewegung |
| Portalnummern auf dem Scanner | ca. 0,02 ms | ebenso |
| Bossbar-Prüfung (Feld unter dem Spieler) | ca. 0,02 ms | je Spieler 1×/s |
| Link-Partikel: Abstand zu allen Links | ca. 0,04 ms | je Spieler 2×/s |
| Scoreboard | ca. 0,04 ms | 1×/s |
| Kreuzungsprüfung beim Link ziehen | ca. 0,02 ms | je Versuch |

Hochgerechnet auf 20 Spieler: typisch etwa 1–2 %, im ungünstigsten Fall (alle laufen mit
Scanner herum) etwa 6 % der Rechenzeit eines Server-Ticks. Nicht enthalten ist die Menge der
Partikel, die an die Spieler geschickt wird: Links im Umkreis von 64 Blöcken und Felder im
Umkreis von 40 Blöcken, je 2×/s. Das belastet vor allem die Verbindung der Spieler, kaum den
Server.

## 8. Fehlersuche

| Problem | Lösung |
|---|---|
| Plugin deaktiviert sich beim Start | Log prüfen: meist ist `inigresso.db` gesperrt oder kaputt → aus Sicherung zurückholen |
| Portal ohne Leitstein / Anzeige | Chunk neu laden (weggehen und zurückkommen); fehlende Kerne werden beim Laden ergänzt |
| Scanner bleibt leer | `/ig scanner` gibt einen neuen; alte Scanner werden beim Login auf die aktuelle Karte umgestellt |
| Scoreboard fehlt bei einem Spieler | `/ig scoreboard an`; ein anderes Plugin kann ein eigenes Scoreboard setzen |
| Zyklus/Verfall „verpasst“ während der Server aus war | wird beim Start automatisch nachgeholt (siehe Log) |
| Automatische Portale erscheinen nicht | nur in `worlds`, nur auf natürlichem Boden, nur in Chunks, die noch nie geprüft wurden (Tabelle `auto_cell`) |
