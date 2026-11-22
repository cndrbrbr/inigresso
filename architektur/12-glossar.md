# 12. Glossar

Spielbegriffe mit ihrer Entsprechung im Code. Regeln dazu in [rules.md](../rules.md).

| Begriff | Bedeutung | Im Code |
|---|---|---|
| Fraktion | Lumen (grün), Wächter (blau), Clip (rot); Neutral nur für Portale | `model.Faction` |
| Portal | Ort mit Leitstein als Kern, gehört einer Fraktion oder ist neutral | `model.Portal`, `PortalService` |
| Kern | Leitstein-Block des Portals; Ziel aller Rechtsklicks | `Portal.core()` (`BlockPos`) |
| Resonator | Bis zu 8 je Portal, Stufe 1–8, hat Energie | `model.Resonator` |
| Portalstufe | Durchschnitt der Resonatorstufen (leere Plätze = 0, abgerundet, mind. 1 sobald einer steht), bestimmt Link-Reichweite | `Portal.level()` |
| Erobern | Ersten Resonator auf ein neutrales Portal setzen | `PortalCaptured`, `capture_log` |
| Hacken | Rechtsklick ohne Gegenstand: Loot, evtl. Schlüssel, XP | `HackService` |
| Burst | Angriffsgegenstand, Schaden an gegnerischen Resonatoren im Umkreis | `CombatService` |
| Schlüssel | Nötig, um zu einem Portal zu linken | `GameItem` (Schlüssel), `ItemFactory.keyName` |
| Link | Verbindung zweier Portale derselben Fraktion; darf keinen anderen kreuzen | `model.Link`, `LinkService` |
| Feld | Dreieck aus drei Links einer Fraktion | `model.Field`, `geo.Triangle` |
| Einfluss | Feldfläche / 100, aufgerundet; Summe je Fraktion | `LinkService.influence` |
| Messpunkt | Gespeicherter Einfluss je Fraktion zu einem Zeitpunkt | Tabelle `measurement` |
| Zyklus | Wertungszeitraum (Standard: Woche ab Montag) | `ScoreService`, Tabelle `cycle` |
| AP / Spielerstufe | Aktionspunkte; Stufe 1–8 nach AP | `PlayerProgressService` |
| Verfall | Tägliche Energieabnahme der Resonatoren | `EnergyService` |
| Vorschlag | Von Spielern gemeldeter Portalort, Admin entscheidet | `model.Proposal`, `ProposalService` |
| Automatisches Portal | Portal je Rasterzelle auf natürlichem Boden | `AutoPortalPlanner`, `AutoPortalListener`, `auto_cell` |
| Scanner | Karten-Item mit Portalen, Links, Feldern | `item.Scanner`, `render.ScannerRenderer` |
| Webkarte | BlueMap mit Inigresso-Markern | `render.WebMapMarkers` |
