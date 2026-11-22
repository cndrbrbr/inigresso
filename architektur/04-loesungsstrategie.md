# 4. Lösungsstrategie

| Ziel (Abschnitt 1.2) | Ansatz | Details |
|---|---|---|
| Korrekte Spiellogik | Spiellogik (`geo`, `model`, `service`) ohne Bukkit, Zeit und Zufall injiziert | [ADR-0001](adr/0001-spiellogik-ohne-bukkit.md) |
| Kein Datenverlust | Zustand im Speicher, SQLite als einzige Wahrheit, Schreiben asynchron über eine Queue | [ADR-0002](adr/0002-sqlite-im-speicher-mit-schreibqueue.md) |
| Selbstheilung | Anzeigen sind abgeleitet und werden beim Chunk-Laden neu gesetzt; zeitgesteuerte Dinge holen Verpasstes nach | [ADR-0003](adr/0003-anzeigen-nicht-in-der-welt-speichern.md), [Konzepte 8.3](08-konzepte.md#83-zeit-und-nachholen) |
| Lose Kopplung | Services melden Änderungen über einen synchronen `GameEventBus`; Speicherung und Anzeige hören zu | [Konzepte 8.2](08-konzepte.md#82-ereignisse) |
| Leicht für den Server | Alles im Speicher, Partikel nur nahe Spielern, Scanner nur bei Bewegung neu zeichnen | [ADMIN_GUIDE §7](../ADMIN_GUIDE.md) |
| Einfacher Betrieb | Eine Jar, nummerierte SQL-Migrationen, BlueMap optional | [Verteilung](07-verteilungssicht.md) |

Spielgegenstände sind markierte Items im normalen Inventar
([ADR-0004](adr/0004-spielgegenstaende-als-markierte-items.md)), damit Spieler sie wie
Minecraft-Items sehen und benutzen.
