# Inigresso – Architektur

Ziel: Spiellogik (Regeln aus [rules.md](rules.md)) so weit wie möglich ohne Bukkit
testbar halten. Bukkit-Code nur an den Rändern: Listener, Befehle, Anzeige, Scheduler.

## 1. Überblick

```
             ┌──────────── Bukkit / Spigot 26.3 ────────────┐
 Events ───► │ listener/*      command/IgCommand            │
             │      │                 │                      │
             │      ▼                 ▼                      │
             │  ┌──────────── service/* (Spiellogik) ─────┐  │
             │  │ FactionService  ProposalService          │  │
             │  │ PortalService   HackService  CombatService│ │
             │  │ LinkService     FieldService ScoreService │ │
             │  │ PlayerProgressService  ItemService        │ │
             │  └────────┬─────────────────────┬──────────┘  │
             │           │                     │             │
             │      model/* + geo/*        render/*  ◄─ Scheduler
             │           │                 (Blöcke, Partikel,│
             │           ▼                  TextDisplay,     │
             │     store/* (Repositories)   Bossbar)         │
             │           │                                   │
             └───────────┼───────────────────────────────────┘
                         ▼
               SQLite plugins/Inigresso/inigresso.db
```

Paket-Basis: `io.github.cndrbrbr.inigresso`

| Paket | Inhalt | Bukkit-abhängig? |
|---|---|---|
| `geo` | `Vec2`, `Segment`, `Triangle`, `Geometry` (Kreuzung, Fläche, Punkt-im-Dreieck) | nein |
| `model` | `Faction`, `Portal`, `Resonator`, `Link`, `Field`, `Proposal`, `PlayerProfile`, `GameItem`, `CycleResult` | nein (Positionen als eigene `BlockPos`-Records) |
| `service` | Regeln und Zustandsänderungen, siehe Abschnitt 3 | nein (Zeit über `Clock`, Zufall über `RandomGenerator` injiziert) |
| `store` | Repositories + `Database` (Schema, Migrationen, asynchrone Schreibqueue) | nur JDBC |
| `render` | Kernblock, Resonatorblöcke, `TextDisplay`, Partikel-Links, Feld-Bossbar | ja |
| `listener` | Interaktion, Schutz, Join/Quit, Tod | ja |
| `command` | `/ig` mit Unterbefehlen und Tab-Completion | ja |
| `item` | Erzeugen/Erkennen markierter Items (PersistentDataContainer) | ja |
| `config` | `InigressoConfig` (typisiert aus `config.yml`) | lesend ja |

## 2. Datenmodell

```
Faction        LUMEN | WAECHTER | NEUTRAL
BlockPos       (world, x, y, z)
Portal         id, name, core: BlockPos, owner: Faction, capturedBy: UUID?,
               capturedAt, resonators: Resonator?[8]
Resonator      slot 0..7, level 1..8, energy, deployedBy: UUID
Link           id, from: portalId, to: portalId, faction, createdBy, createdAt
Field          id, a, b, c: portalId, faction, linkIds[3], area (Blöcke²), createdBy
Proposal       id, name, pos, proposer, createdAt, status (OFFEN|ANGENOMMEN|ABGELEHNT),
               decidedBy, reason
PlayerProfile  uuid, lastName, faction, ap, level, inventory: Map<GameItemKey,int>,
               pendingMessages
HackLog        uuid, portalId, timestamp      (für Abklingzeit/4-in-4h)
Measurement    timestamp, lumenInfluence, waechterInfluence
CycleResult    cycleStart, lumenAvg, waechterAvg, winner
```

Die Portalstufe, Reichweite und der Einfluss werden berechnet, nicht gespeichert.

### SQLite-Schema (Tabellen)

`portal`, `resonator` (portal_id, slot PK), `link`, `field`, `proposal`, `player`,
`player_item` (uuid, item, level, count), `hack_log`, `measurement`, `cycle`,
`message`, `audit`, `state`, `auto_cell` (geprüfte Rasterzellen für automatische Portale),
`schema_version`.

- Alle Daten werden beim Start komplett in den Speicher geladen (erwartet: < 1 000
  Portale, < 5 000 Links).
- Änderungen gehen als Befehle in eine Schreibqueue, die ein einzelner Hintergrund-Thread
  in Transaktionen abarbeitet. Beim Deaktivieren wird die Queue geleert (blockierend).
- Migrationen: nummerierte SQL-Skripte unter `resources/db/`, `schema_version` merkt den
  Stand.

## 3. Services

Alle Services laufen auf dem Server-Thread und sind nicht thread-sicher; nur `store`
arbeitet asynchron.

| Service | Verantwortung |
|---|---|
| `FactionService` | Wahl, Admin-Wechsel, Größenhinweis |
| `ProposalService` | Vorschlag prüfen (Stufe, Anzahl, Abstand, Welt), annehmen/ablehnen, Portal anlegen |
| `PortalService` | Resonator setzen/aufrüsten, Stufenlimits pro Spieler, Portalstufe, Neutralisierung (löst Link-/Feldabbau aus), Aufladen, täglicher Verfall |
| `HackService` | Abklingzeit, Loot-Tabelle, Schlüsselchance |
| `CombatService` | Burst-Schaden an Resonatoren, Zerstörung, AP |
| `LinkService` | Linkbedingungen (Reichweite, Kreuzung, gegnerisches Feld, max. 8 ausgehend, Schlüssel), Link anlegen/entfernen |
| `FieldService` | Felder nach neuem Link bestimmen (größtes Dreieck je Seite), Felder bei Linkverlust entfernen, Punkt-in-Feld-Abfrage |
| `ScoreService` | Einfluss je Fraktion, stündliche Messpunkte, Wochenzyklus, Ergebnis |
| `PlayerProgressService` | AP vergeben, Stufe berechnen, Benachrichtigungen |
| `HackService` → `item/GameInventory` | Gegenstände liegen als markierte Items im normalen Inventar; `GameInventory` zählt (Limit 500), gibt und verbraucht sie. Die Tabelle `player_item` wird dafür nicht benutzt (Reserve). |

Services melden Änderungen über einen einfachen `GameEventBus` (z. B. `PortalChanged`,
`LinkCreated`, `LinkRemoved`, `FieldCreated`, `FieldRemoved`). `render` und `store`
hören darauf, Services kennen beide nicht direkt.

## 4. Wichtige Algorithmen

- **Link-Kreuzung**: Orientierungstest (Kreuzprodukt) für zwei Segmente in X/Z.
  Gemeinsame Endpunkte zählen nicht als Kreuzung. Kandidaten vorfiltern über ein
  Raster (Zellen 256 × 256 Blöcke), damit nicht alle Links geprüft werden.
- **Neue Felder**: Für den neuen Link A–B alle Portale C suchen, die mit A und B
  verlinkt sind. C nach Seite (Vorzeichen des Kreuzprodukts) trennen, je Seite das
  Dreieck mit größter Fläche nehmen.
- **Punkt im Feld**: baryzentrischer Test; für Bossbar und die Regel „A liegt nicht in
  gegnerischem Feld“.
- **Fläche**: `|cross(B−A, C−A)| / 2`.

## 5. Darstellung (`render`)

- Kern: Lodestone + `TextDisplay` (Name, Fraktion in Farbe, Stufe, Besitzer).
  Entities sind nicht persistent und werden beim Chunk-Laden neu gesetzt (Erfahrung
  aus der Buildcontest-Statustafel).
- Resonatoren als `BlockDisplay` (halbe Größe, leuchtend in Fraktionsfarbe) je Stufe aus
  der Konfiguration, z. B. L1 Kupferblock … L8 Diamantblock. Weltblöcke bleiben unverändert.
- Links: alle 10 Ticks Partikel (`DUST` in Fraktionsfarbe) entlang des Segments, nur
  für Abschnitte im Umkreis von 64 Blöcken um Spieler.
- Felder: Bossbar „Feld der Lumen – 1 234 Einfluss“, wenn der Spieler darin steht
  (Prüfung alle 20 Ticks).
- Scanner: `ScannerRenderer` (kontextbezogener `MapRenderer`, eine gemeinsame Karten-ID in
  `state.scanner_map`). Bild wird ohne Bukkit in `ScannerImage` gezeichnet
  (`ScannerProjection`: Welt → Pixel, gedreht oder Norden oben), neu höchstens alle 100 ms
  und nur bei Bewegung/Drehung, sonst einmal pro Sekunde. Ziel-Navigation in `Navigator`
  (Actionbar alle 10 Ticks).
- Optional später: Webkarte / BlueMap-Marker (Portale, Links, Felder als Overlay), siehe
  `implement.md`.

## 6. Schutz (`listener/ProtectionListener`)

- Blöcke im Radius 4 um Kerne: kein Abbau, kein Setzen, keine Explosion, keine Kolben,
  kein Fließen von Wasser/Lava hinein.
- Admins mit `inigresso.bypass` dürfen bauen (zum Reparieren).

## 7. Zeitgesteuerte Aufgaben

| Aufgabe | Takt |
|---|---|
| Partikel für Links | alle 10 Ticks |
| Bossbar Feld-Anzeige | alle 20 Ticks |
| Verfall | täglich 03:00 (Serverzeit), nachgeholt, falls der Server zu der Zeit aus war |
| Messpunkt | stündlich, verpasste Stunden werden nicht erfunden |
| Zyklusabschluss | Montag 00:00, ebenfalls nachgeholt |

`Clock` wird injiziert, damit Verfall und Zyklus in Tests mit fester Zeit laufen.

## 8. Konfiguration (`config.yml`)

```yaml
worlds: [world]
portal:
  min-distance: 48
  protect-radius: 4
  resonator-radius: 3
  resonator-blocks: [COPPER_BLOCK, IRON_BLOCK, GOLD_BLOCK, REDSTONE_BLOCK,
                     LAPIS_BLOCK, EMERALD_BLOCK, AMETHYST_BLOCK, DIAMOND_BLOCK]
proposal:
  min-level: 2
  max-open: 3
link:
  max-range: 2000
  max-outgoing: 8
  beam-height: 5.0
decay:
  percent-per-day: 15
  time: "03:00"
hack:
  cooldown-minutes: 5
  max-per-4h: 4
  key-chance: 0.50
player:
  max-items: 500
cycle:
  start-day: MONDAY
auto-portals:
  enabled: false
  spacing: 100
  jitter: 25
```

## 9. Tests

- JUnit 5 + Mockito wie im Buildcontest.
- `geo` und `service` vollständig unit-getestet (feste `Clock`, fester Zufall,
  In-Memory-Repositories).
- `store` gegen eine temporäre SQLite-Datei.
- Bukkit-Teile werden manuell auf einem Testserver geprüft.

## 10. Build

```
docker run --rm -v $PWD:/src -v /root/.m2:/root/.m2 -w /src \
  maven:3.9-eclipse-temurin-25 mvn -q -B package
```

Ergebnis: `target/inigresso-<version>.jar`.
