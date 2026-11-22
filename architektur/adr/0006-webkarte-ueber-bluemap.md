# ADR-0006: Webkarte über BlueMap statt eigenem Webserver

- **Status:** angenommen
- **Datum:** 2026-11-14
- **Version:** 0.10.0

## Kontext

Spieler wollen das Netz aus Portalen, Links und Feldern auch außerhalb des Spiels sehen. Ein eigener
Webserver mit eigener Karte hieße: Kartendarstellung der Welt selbst bauen, Port öffnen, absichern.

## Entscheidung

Inigresso nutzt BlueMap als optionale Abhängigkeit (`softdepend`, API `provided`). Ist BlueMap da,
legt `WebMapMarkers` einen Markersatz an, schreibt `stand.json` und meldet eigenes JS/CSS für den
Spielstand-Kasten an. Den Inhalt berechnet das Bukkit-freie `WebMapContent`.

## Alternativen

- Eigener Webserver + Leaflet auf DB-Daten: mehr Code, keine Weltansicht.

## Folgen

- Weltkarte und Spielnetz in einem; kaum eigener Webcode.
- Abhängig von BlueMap-API und -Caching (BlueMap schickt `max-age=86400` → eigene Dateien mit
  `?v=<version>` bzw. `?t=` laden).
- Leistung bei vielen Markern muss beachtet werden (0.15.0: `webmap.neutral-max-distance`).
