# ADR-0002: SQLite, Zustand im Speicher, eine Schreibqueue

- **Status:** angenommen
- **Datum:** 2026-11-13
- **Version:** 0.1.0

## Kontext

Der Spielstand muss Neustarts überleben und sicherbar sein. Abfragen (Kreuzung, Felder unter dem
Spieler) laufen oft pro Tick; DB-Zugriffe auf dem Server-Thread würden laggen. Admins sollen nichts
zusätzlich installieren müssen.

## Entscheidung

- SQLite-Datei `plugins/Inigresso/inigresso.db` über den in Spigot enthaltenen Treiber.
- Beim Start wird alles in den Speicher geladen; danach liest nur noch der Speicher.
- Schreiben asynchron über einen einzigen Thread mit Queue, jeder Auftrag in einer Transaktion.
- Schema über nummerierte SQL-Migrationen.

## Alternativen

- YAML-Dateien: keine Transaktionen, unhandlich bei tausenden Links.
- MySQL/MariaDB: zusätzlicher Dienst, für einen Server übertrieben.
- Synchrones Schreiben auf dem Server-Thread: einfach, aber Lag-Spitzen.

## Folgen

- Schnelle Abfragen, kein Lag durch I/O.
- Datenmenge ist durch den Speicher begrenzt (heute weit darunter).
- Beim Absturz können die letzten Aufträge der Queue fehlen; `onDisable` leert die Queue.
- Migrationen sind nur vorwärts; vor Updates sichern.
