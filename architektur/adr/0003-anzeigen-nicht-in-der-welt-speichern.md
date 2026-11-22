# ADR-0003: Anzeigen als Display-Entities, nicht als Weltblöcke

- **Status:** angenommen
- **Datum:** 2026-11-13
- **Version:** 0.4.0

## Kontext

Portale brauchen sichtbare Resonatoren, Namen und Fraktionsfarben. Würde man echte Blöcke setzen,
müsste man sie gegen Abbau schützen, bei Änderungen sauber zurückbauen, und Welt und DB könnten
auseinanderlaufen (Absturz, Weltreset, Backup nur einer Seite).

## Entscheidung

Nur der Kern ist ein echter Block (Leitstein). Resonatoren sind `BlockDisplay`, Namen
`TextDisplay`, das Leuchten eine leicht vergrößerte Display-Hülle. Diese Entities sind nicht
persistent und werden beim Chunk-Laden aus dem Spielstand neu erzeugt. Fehlt ein Leitstein, wird
er ergänzt.

## Alternativen

- Echte Blöcke für Resonatoren: Schutz- und Aufräumaufwand.
- Persistente Entities: verwaisen bei Abstürzen und Doppelungen.

## Folgen

- DB ist die einzige Wahrheit; Welt kann nicht „falsch“ sein.
- Beim Löschen eines Portals bleibt höchstens ein Leitstein stehen.
- Erfahrung aus dem Buildcontest-Plugin übernommen.
