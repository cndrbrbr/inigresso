# 9. Architekturentscheidungen

Wichtige Entscheidungen als ADR (Architecture Decision Record) in [`adr/`](adr/): eine Datei je
Entscheidung, nummeriert, nie gelöscht. Wird eine Entscheidung ersetzt, bekommt sie den Status
„ersetzt durch ADR-xxxx“.

| Nr. | Entscheidung | Status |
|---|---|---|
| [0001](adr/0001-spiellogik-ohne-bukkit.md) | Spiellogik ohne Bukkit, Zeit und Zufall injiziert | angenommen |
| [0002](adr/0002-sqlite-im-speicher-mit-schreibqueue.md) | SQLite, Zustand im Speicher, eine Schreibqueue | angenommen |
| [0003](adr/0003-anzeigen-nicht-in-der-welt-speichern.md) | Anzeigen als Display-Entities, nicht als Weltblöcke | angenommen |
| [0004](adr/0004-spielgegenstaende-als-markierte-items.md) | Spielgegenstände als markierte Items im normalen Inventar | angenommen |
| [0005](adr/0005-portale-vorschlag-und-automatisch.md) | Portale per Vorschlag mit Freigabe und automatisch je Rasterzelle | angenommen |
| [0006](adr/0006-webkarte-ueber-bluemap.md) | Webkarte über BlueMap statt eigenem Webserver | angenommen |
| [0007](adr/0007-link-reichweite.md) | Link-Reichweite: Grundwert + Zuwachs × (Stufe² − 1) | angenommen |

Vorlage für neue ADRs: [adr/0000-vorlage.md](adr/0000-vorlage.md).
