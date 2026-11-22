# Inigresso – Architekturdokumentation

Gegliedert nach [arc42](https://arc42.org/), aber schlank: nur die Abschnitte, die für ein
Plugin dieser Größe etwas aussagen. Die Nummern folgen arc42, damit man sich in jedem Repo
gleich zurechtfindet; ausgelassene Nummern sind Absicht.

| Nr. | Abschnitt | Frage |
|---|---|---|
| 1 | [Einführung und Ziele](01-einfuehrung-und-ziele.md) | Was soll das Plugin, was ist wichtig? |
| 3 | [Kontextabgrenzung](03-kontext.md) | Mit wem redet das Plugin? |
| 4 | [Lösungsstrategie](04-loesungsstrategie.md) | Die wenigen Grundentscheidungen |
| 5 | [Bausteinsicht](05-bausteinsicht.md) | Pakete und Klassen |
| 6 | [Laufzeitsicht](06-laufzeitsicht.md) | Start, Link ziehen, Zyklus nachholen |
| 7 | [Verteilungssicht](07-verteilungssicht.md) | Wo läuft es (meck mc2, BlueMap, Caddy)? |
| 8 | [Querschnittliche Konzepte](08-konzepte.md) | Speicherung, Ereignisse, Zeit, Items, Anzeige |
| 9 | [Architekturentscheidungen](09-entscheidungen.md) | ADRs in [`adr/`](adr/) |
| 11 | [Risiken und technische Schulden](11-risiken.md) | Was kann schiefgehen, was ist offen? |
| 12 | [Glossar](12-glossar.md) | Spielbegriffe ↔ Klassen |

Ausgelassen: **2 Randbedingungen** (stehen kurz in 1), **10 Qualitätsanforderungen** (die
messbaren Werte stehen in 1.2 und im Leistungstest, [ADMIN_GUIDE.md §7](../ADMIN_GUIDE.md)).

## Andere Dokumente im Repo

| Dokument | Für wen | Inhalt |
|---|---|---|
| [README.md](../README.md) | alle | Kurzüberblick, Bauen |
| [rules.md](../rules.md) | Entwickler, Admins | Spielregeln mit allen Zahlen – **die Anforderungen** |
| [SPIELANLEITUNG.md](../SPIELANLEITUNG.md), [Webseite](https://cndrbrbr.github.io/inigresso/) | Spieler | Wie spiele ich? |
| [ADMIN_GUIDE.md](../ADMIN_GUIDE.md) | Admins | Installation, Konfiguration, Sicherung |
| [implement.md](../implement.md) | Entwickler | Umsetzungsphasen und Stand |
| `architektur/` (hier) | Entwickler | Wie ist es gebaut und warum? |

## Pflege

- Regeländerung → `rules.md`. Bauänderung → hier. Neue Grundsatzentscheidung → neues ADR.
- Diagramme als Mermaid direkt im Markdown (GitHub zeigt sie an).
- Lieber einen Abschnitt löschen als halbleer stehen lassen.
