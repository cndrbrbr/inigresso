# ADR-0004: Spielgegenstände als markierte Items im normalen Inventar

- **Status:** angenommen
- **Datum:** 2026-11-13
- **Version:** 0.4.0

## Kontext

Spieler brauchen Resonatoren, Bursts und Schlüssel. Geplant war ein virtuelles Inventar in der
Tabelle `player_item`, das man über Befehle bedient.

## Entscheidung

Spielgegenstände sind normale Items mit Markierung im `PersistentDataContainer`. Sie liegen im
Minecraft-Inventar und werden per Rechtsklick benutzt. `ItemGuardListener` verhindert Missbrauch
(Craften, Amboss, Feuer legen, Tresore öffnen) und behält sie beim Tod.

## Alternativen

- Virtuelles Inventar (`player_item`): unsichtbar, nur per Befehl, für Kinder schwer verständlich.

## Folgen

- Intuitiv: Spieler sehen und halten, was sie haben.
- Mehr Randfälle: Tod, Respawn, volles Inventar, Neustart vor Respawn (siehe Risiken).
- Tabelle `player_item` bleibt ungenutzt im Schema (technische Schuld).
