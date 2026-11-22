# 8. Querschnittliche Konzepte

## 8.1 Speicherung

- **Wahrheit ist die DB, gearbeitet wird im Speicher.** Beim Start wird alles gelesen (erwartet
  < 1 000 Portale, < 5 000 Links; nach dem Reset 20.11. ~350 Portale). Danach liest niemand mehr.
- **Schreiben** geht als `Database.Write` in eine Queue, die ein einzelner Daemon-Thread
  `Inigresso-DB` nacheinander abarbeitet, jeder Auftrag in eigener Transaktion. `onDisable`
  wartet, bis die Queue leer ist.
- SQLite mit `journal_mode=WAL`, `synchronous=NORMAL`, `foreign_keys=ON`. Treiber aus Spigot.
- **Migrationen**: `resources/db/V<n>__<name>.sql`, Liste in `Database.MIGRATIONS`, Stand in
  `schema_version`. Nur anhängen, nie ändern. Achtung: Skripte werden an `;` getrennt – **kein
  `;` in SQL-Kommentaren**.
- Kein Weg zurück auf eine ältere Version; vor Updates sichern.

## 8.2 Ereignisse

- `GameEvent` ist ein `sealed interface` mit Records (`PortalChanged`, `LinkCreated`,
  `FieldRemoved`, `ProfileChanged`, `Notify`, …).
- `GameEventBus` ist synchron und läuft nur auf dem Server-Thread. Services kennen weder
  Repositories noch Anzeigen; die melden sich per `bind(bus)` an.
- Fehler in einem Abonnenten werden geloggt und brechen die anderen nicht ab.

## 8.3 Zeit und Nachholen

- `Clock` und `RandomGenerator` werden injiziert → Tests mit fester Zeit und festem Zufall.
- Alles Zeitgesteuerte prüft einmal pro Minute „was ist fällig?“ statt sich auf einen exakten
  Takt zu verlassen. Damit wird nach einem Ausfall automatisch nachgeholt.

| Aufgabe | Takt | Nachholen |
|---|---|---|
| Link-Partikel / Feld-Partikel | 10 Ticks | – |
| Bossbar im Feld, Sidebar | 20 Ticks | – |
| Zielpfeil | 10 Ticks | – |
| Webkarte neu bauen | 5 s, wenn geändert, sonst jede Minute | – |
| `stand.json` | 20 s | – |
| Verfall | täglich `decay.time` | ja, je verpasstem Tag |
| Messpunkt | `cycle.measure-minutes` | nein (nichts erfinden) |
| Zyklusende | `cycle.length-hours` ab `start-day` | ja |

## 8.4 Spielgegenstände

Resonatoren, Bursts, Schlüssel und Scanner sind normale Items mit Markierung im
`PersistentDataContainer` ([ADR-0004](adr/0004-spielgegenstaende-als-markierte-items.md)).

- `ItemFactory` erzeugt und erkennt sie, `GameInventory` zählt (Limit `player.max-items`),
  gibt und verbraucht.
- `ItemGuardListener`: nicht craftbar, nicht im Amboss benutzbar; beim Tod behalten, beim Respawn
  zurück; was nicht mehr passt, fällt auf den Boden.
- Fraktionswechsel löscht Spielgegenstände des Spielers.
- Scanner: fehlt er einem Fraktionsspieler beim Join, gibt `ScannerListener` nach 1 s einen neuen.

## 8.5 Anzeige ohne Weltänderung

- Außer dem Leitstein als Kern ändert das Plugin keine Weltblöcke. Resonatoren, Namen und
  Leuchten sind nicht persistente Display-Entities, die beim Chunk-Laden neu entstehen
  ([ADR-0003](adr/0003-anzeigen-nicht-in-der-welt-speichern.md)).
- Fehlt beim Chunk-Laden der Leitstein, wird er ergänzt.
- Wo Zeichnen teuer oder schwer zu prüfen ist (Scanner, Webkarte), steckt der Inhalt in einer
  Bukkit-freien Klasse, die Tests und Vorschau-PNGs erzeugen können.

## 8.6 Schutz

Radius `portal.protect-radius` um jeden Kern: kein Abbauen, Setzen, Explodieren, Kolben,
Fließen hinein. `inigresso.bypass` darf bauen, aber den Kern selbst kann niemand abbauen.

## 8.7 Konfiguration

`InigressoConfig` liest `config.yml` typisiert, mit Standardwerten und Warnungen bei Unsinn.
Services bekommen Regeln als `Supplier<Rules>`, deshalb greift `/ig reload` ohne Neustart.

## 8.8 Audit und Nachrichten

- Admin-Eingriffe mit Spielfolgen landen in `audit` und im Log (`InigressoPlugin.audit`).
- `Notify` an Offline-Spieler wird in `message` gespeichert und beim Login zugestellt.

## 8.9 Sprache und Texte

Alle Texte im Spiel sind deutsch und stehen derzeit direkt im Code (kein Sprachdatei-System).
