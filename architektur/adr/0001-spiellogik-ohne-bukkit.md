# ADR-0001: Spiellogik ohne Bukkit, Zeit und Zufall injiziert

- **Status:** angenommen
- **Datum:** 2026-11-13
- **Version:** 0.1.0

## Kontext

Die Regeln (Link-Kreuzung, Felder, Verfall, Wochenzyklus, Loot) sind umfangreich und voller
Sonderfälle. Ein Test auf einem echten Server dauert Minuten, Zeitabläufe (Woche, 03:00) lassen
sich dort kaum prüfen, und echte Mitspieler zum Testen gibt es selten.

## Entscheidung

`geo`, `model`, `event` und `service` hängen nicht von Bukkit ab. Positionen sind eigene
`BlockPos`-Records, Zeit kommt über `java.time.Clock`, Zufall über `RandomGenerator`, Regeln über
`Supplier<Rules>`. Bukkit-Code steht nur in `listener`, `command`, `render`, `item` und
`InigressoPlugin`.

## Alternativen

- Services direkt mit Bukkit-Typen und Tests mit MockBukkit: schwer zu pflegen bei neuen
  Spigot-Versionen (26.x), langsamer.
- Nur manuell testen: nicht machbar bei Zeitregeln.

## Folgen

- 109 schnelle Unit-Tests, auch für Nachholen nach Ausfall und Kurzzyklen.
- Spigot-Updates treffen nur den Rand.
- Etwas Übersetzungscode zwischen Bukkit und Modell (`BlockPos` ↔ `Location`).
- Dasselbe Muster für Anzeigen: `ScannerImage`, `WebMapContent`, `Sidebar.Data` sind Bukkit-frei
  und dadurch testbar bzw. als Vorschau-PNG renderbar.
