# 6. Laufzeitsicht

Drei Abläufe, an denen man das Zusammenspiel versteht.

## 6.1 Start des Plugins

```mermaid
sequenceDiagram
    participant S as Spigot
    participant P as InigressoPlugin
    participant D as Database
    participant Sv as Services
    participant R as render/listener
    S->>P: onEnable
    P->>P: config.yml lesen
    P->>D: öffnen, Migrationen V1..Vn
    P->>D: alle Tabellen lesen (Server-Thread)
    P->>Sv: Services mit geladenen Daten bauen
    P->>R: Teams, Portalanzeige, Listener, Scheduler
    P->>Sv: energy.tick(), score.tick() — Verpasstes nachholen
    Note over P: Bei SQLException: Plugin deaktiviert sich, nichts halb gestartet
```

Reihenfolge: Konfiguration → Datenbank → Services → Anzeige → Listener/Befehle. Nach dem Start
wird nicht mehr aus der DB gelesen.

## 6.2 Link ziehen (`/ig link <ziel>`)

```mermaid
sequenceDiagram
    participant Pl as Spieler
    participant C as IgCommand
    participant L as LinkService
    participant B as GameEventBus
    participant St as LinkRepository
    participant N as NetworkRenderer / WebMapMarkers
    Pl->>C: /ig link 12
    C->>L: link(spieler, von, nach)
    L->>L: an A (≤ 6), A hat 8 Resonatoren, Schlüssel von B,<br/>B eigene Fraktion, Reichweite, max. 8 ausgehend,<br/>keine Kreuzung, A nicht im gegnerischen Feld
    alt Bedingung verletzt
        L-->>C: Result(problem, detail)
        C-->>Pl: Meldung (z. B. nötige Portalstufe)
    else ok
        L->>L: Felder: größtes Dreieck je Seite
        L->>B: LinkCreated, FieldCreated…, ProfileChanged (AP)
        B->>St: in Schreibqueue
        B->>N: Webkarte als geändert markieren
        L-->>C: Result(link, fields)
        C-->>Pl: Erfolgsmeldung
    end
```

Partikel brauchen kein Ereignis: `NetworkRenderer` liest alle 10 Ticks den aktuellen Stand.

## 6.3 Zyklus nach Ausfall nachholen

1. Jede Minute (und einmal beim Start) ruft der Scheduler `ScoreService.tick()`.
2. `tick()` vergleicht die Uhr mit dem gespeicherten Zyklusstart. Ist ein oder mehr Zyklusende
   vorbei, wird jeder verpasste Zyklus aus den vorhandenen Messpunkten abgeschlossen,
   gespeichert und angekündigt.
3. Fehlende Messpunkte werden **nicht** erfunden; ein Zyklus ohne Messpunkte ist unentschieden.
4. Hat sich die Zyklus-Konfiguration geändert, wird der laufende Zyklus einmal verkürzt, damit
   er wieder auf `start-day` passt.

`EnergyService.tick()` macht dasselbe für den täglichen Verfall (`state.last_decay`).
