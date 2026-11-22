# ADR-0005: Portale per Vorschlag mit Freigabe und automatisch je Rasterzelle

- **Status:** angenommen
- **Datum:** 2026-11-13 (Vorschläge), 2026-11-14 (automatisch)
- **Version:** 0.3.0 / automatisch ab 0.7.x

## Kontext

Im Vorbild liegen Portale an echten Orten. Auf einem Minecraft-Server sollen Portale an Bauwerken
der Spieler liegen, aber Missbrauch (Portal vor der eigenen Haustür, Spam) muss verhindert werden.
Auf einer frischen Welt gibt es außerdem zu wenige Bauwerke, um überhaupt spielen zu können.

## Entscheidung

- Spieler ab einer Stufe schlagen Portale vor, Admins nehmen an oder lehnen ab.
- Zusätzlich optional automatische Portale: je Rasterzelle (Standard 100 × 100) eins, deterministisch
  versetzt, nur auf natürlichem Boden, beim ersten Laden der Gegend. Geprüfte Zellen in `auto_cell`.

## Folgen

- Eine Welt ist sofort spielbar; Bauwerke bleiben durch Vorschläge wichtig.
- Viele Portale (nach Reset ~350) → Last auf der Webkarte (siehe ADR-0006, Risiken).
- Raster bestimmt die Link-Reichweite mit (ADR-0007).
