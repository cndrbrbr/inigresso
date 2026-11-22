# 11. Risiken und technische Schulden

## 11.1 Risiken

| Risiko | Folge | Gegenmaßnahme / Stand |
|---|---|---|
| Spieler- und Spielwerte nie mit vielen echten Spielern geprüft | Spiel zu leicht/zu schwer, eine Fraktion dominiert | Balancing ist offener Punkt in [implement.md](../implement.md) Phase 8; Werte sind alle konfigurierbar |
| Spigot-Update (26.x → neuer) ändert API/Verhalten | Plugin startet nicht oder Anzeigen fehlen (z. B. Karten-Farbcodes 0.14.1) | Bukkit-Code nur am Rand; nach jedem Update Testserver |
| Viele automatische Portale | Webkarte ruckelt, große `markers.json` | 0.15.0: neutrale Portale nur nah; Kompression im Webserver empfohlen |
| DB-Datei kaputt/gesperrt | Plugin deaktiviert sich | Sicherung vor Updates; WAL; Fehlersuche im ADMIN_GUIDE |
| Absturz zwischen Tod und Respawn | Einbehaltene Spielgegenstände weg | bekannt (Phase 4); selten |
| Sehr viele Links (> einige tausend) | Kreuzungsprüfung prüft alle Links | heute ~0,02 ms; Rasterindex bei Bedarf nachrüsten |

## 11.2 Technische Schulden

| Schuld | Wo | Aufwand |
|---|---|---|
| `InigressoPlugin` verdrahtet alles von Hand, viele voll qualifizierte Namen, Spielstand-Berechnung (`standing()`) gehört eher in einen Service | `InigressoPlugin.java` | mittel |
| Tabelle `player_item` ungenutzt | Schema V1 | klein (Migration zum Löschen) |
| Texte fest im Code, keine Sprachdatei | `command`, `listener`, `render` | mittel |
| `plugin.yml`-Beschreibung spricht noch von „zwei Fraktionen“ | `plugin.yml` | klein |
| Code-Kommentare verweisen auf alte `architecture.md §n` | einige Klassen | klein; `architecture.md` leitet bis dahin weiter |
| Partikel-Netzlast nie gemessen | `NetworkRenderer` | klein |
