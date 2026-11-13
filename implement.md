# Inigresso – Umsetzungsplan

Reihenfolge so gewählt, dass nach jeder Phase etwas Spielbares auf einem Testserver
läuft. Regeln: [rules.md](rules.md), Aufbau: [architecture.md](architecture.md).

## Phase 0 – Grundgerüst

- [x] Maven-Projekt (Spigot 26.3-API, Java 25, JUnit 5, Mockito), `plugin.yml`,
      `config.yml`, `.gitignore`
- [x] `InigressoPlugin` mit Startreihenfolge: Konfiguration → Datenbank → Services →
      Render → Listener/Befehle
- [x] `Database`: SQLite-Verbindung, Migration `V1__init.sql`, asynchrone Schreibqueue,
      sauberes Herunterfahren
- [x] `/ig` mit Unterbefehl-Gerüst, Hilfe, Tab-Completion, Permissions
- **Fertig, wenn:** Plugin startet und stoppt sauber auf Spigot 26.3, DB-Datei wird angelegt.

## Phase 1 – Geometrie (ohne Bukkit)

- [x] `Vec2`, `Segment`, `Triangle`, `Geometry.crosses`, `area`, `contains`
- [x] Sonderfälle testen: gemeinsame Endpunkte, kollineare Segmente, Punkt auf Kante,
      degenerierte Dreiecke
- **Fertig, wenn:** Tests grün. ✔ (0.1.0: 14 Geometrie- + 3 DB-Tests; Start/Stopp auf Spigot 26.3 geprüft)

## Phase 2 – Fraktionen und Spielerprofil

- [x] `PlayerProfile` laden/anlegen beim Join, speichern
- [x] `/ig fraktion`, `/ig info`, Größenhinweis
- [x] Fraktionsfarbe im Namen (Scoreboard-Team) und im Chat-Präfix
- [x] AP und Stufe (`PlayerProgressService`)
- **Fertig, wenn:** Fraktion überlebt Neustart, Admin-Wechsel funktioniert. ✔ unit-getestet (0.2.0); im Spiel mit echtem Spieler noch zu prüfen.

## Phase 3 – Vorschläge und Portale

- [x] `/ig vorschlagen` mit allen Prüfungen
- [x] `/ig vorschlaege`, `tp`, `annehmen`, `ablehnen`, Nachrichten an Offline-Spieler
- [x] Portal anlegen: Lodestone, `TextDisplay`, Schutzbereich
- [x] `ProtectionListener` (Abbau, Setzen, Explosion, Kolben, Flüssigkeiten)
- [x] `/ig portal`, Admin-Portalbefehle
- **Fertig, wenn:** Ein Spieler schlägt vor, ein Admin nimmt an, das Portal steht
      geschützt in der Welt und bleibt nach Neustart. ✔ 0.3.0: unit-getestet; auf Spigot 26.3
      geprüft: Anzeige beim Chunk-Laden, Leitstein, Umbenennen, Löschen. Vorschlagen und Schutz
      mit echtem Spieler noch zu prüfen.

## Phase 4 – Gegenstände, Resonatoren, Hacken

- [x] Markierte Items (PDC), nicht craftbar, kein Drop beim Tod, `/ig inventar` als Übersicht
- [x] Resonator setzen (Rechtsklick auf den Kern), Aufrüsten, Stufenlimits pro Spieler
- [x] Erobern eines neutralen Portals, Portalstufe, Anzeige der Resonatorblöcke
- [x] Hacken mit Abklingzeit, Loot, Schlüsselchance
- [x] `/ig admin geben` für Tests
- **Fertig, wenn:** Zwei Spieler verschiedener Fraktionen können Portale erobern und
      hacken. ✔ 0.4.0: unit-getestet; Resonator-Anzeige auf Spigot 26.3 geprüft. Setzen,
      Hacken und Item-Schutz mit echtem Spieler noch zu prüfen.
- Bekannt: Beim Tod einbehaltene Gegenstände gehen verloren, wenn der Server vor dem
  Wiederbeleben neu startet.

## Phase 5 – Angriff und Energie

- [ ] Burst: Radius, Schaden mit Abstandsfaktor, Zerstörung, Neutralisierung
- [ ] Aufladen am Kern und per Schlüssel (XP-Kosten)
- [ ] Täglicher Verfall inkl. Nachholen nach Ausfall
- **Fertig, wenn:** Ein Portal lässt sich zerstören und von der anderen Fraktion
      übernehmen.

## Phase 6 – Links und Felder

- [ ] `LinkService` mit allen Bedingungen aus rules.md §6, Rasterindex für Kreuzungen
- [ ] Partikeldarstellung
- [ ] `FieldService`: Felder bei neuem Link, Abbau bei Linkverlust
- [ ] Bossbar beim Stehen im Feld
- **Fertig, wenn:** Drei Portale ergeben ein Feld; Zerstören eines Portals entfernt Links
      und Feld.

## Phase 7 – Wertung

- [ ] Einfluss je Fraktion, stündliche Messpunkte
- [ ] Wochenzyklus mit Ergebnis, Ankündigung, `/ig stand`, `/ig zyklus [alt]`
- [ ] Nachholen verpasster Zyklusabschlüsse
- **Fertig, wenn:** Ein Testzyklus (verkürzt über Konfiguration) ergibt einen Gewinner.

## Phase 8 – Feinschliff und Betrieb

- [ ] Spielanleitung `PLAYER_GUIDE.md` und `ADMIN_GUIDE.md`
- [ ] Audit-Log für Admin-Eingriffe
- [ ] Leistungstest: 200 Portale, 500 Links, 20 Spieler
- [ ] Balancing-Werte nach ersten Testrunden anpassen
- [ ] GitHub-Release, Eintrag im Plugin-Katalog von minecraftHostingServer

## Später / Ideen

- BlueMap-Overlay mit Portalen, Links und Feldern (karte.cndrbrbr.de)
- Webseite mit Ranglisten und Zyklusergebnissen
- Weitere Gegenstände: Schild-Mod (mehr Widerstand), Portal-Virus (Fraktion umdrehen)
- Missionen: vorgegebene Portalrouten mit Belohnung
- Mehrere Welten / getrennte Spielfelder
