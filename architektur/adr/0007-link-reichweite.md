# ADR-0007: Link-Reichweite: Grundwert + Zuwachs × (Stufe² − 1)

- **Status:** angenommen (ersetzt die Formeln aus 0.6.0 und 0.16.0)
- **Datum:** 2026-11-21
- **Version:** 0.16.1

## Kontext

Am Event-Tag konnte ein Spieler nicht linken: alle Portale nahe dem Spawn waren Stufe 1
(Reichweite 64 nach der ursprünglichen Formel), die automatischen Portale aber ~100 Blöcke
auseinander. Erster Versuch 0.16.0 (160 × Stufe²) reichte nicht für schräge Nachbarn mit Versatz.
Vorgabe: „Nachbarn sollte man linken können.“

## Entscheidung

Reichweite = `link.range-base` (250) + `link.range-growth` (100) × (Stufe² − 1), höchstens
`link.max-range` (2000). L1 250, L2 550, L3 1050, L4 1750, ab L5 2000. Die Fehlermeldung nennt
die nötige Portalstufe.

## Alternativen

- Feste große Reichweite: Stufe wäre für Links bedeutungslos.
- Rein quadratisch ab kleinem Wert: Einstieg zu schwer.

## Folgen

- Stufe 1 erreicht alle acht Nachbarzellen (max. ~212 Blöcke; 98 % der 8 nächsten Portale in den
  Live-Daten innerhalb 250).
- Werte konfigurierbar, falls das Raster (`auto-portals.spacing`) geändert wird – dann Grundwert
  mitziehen.
