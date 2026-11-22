# Inigresso – Feature-Wünsche

Offene Wünsche und Ideen. Erledigtes wandert mit Versionsnummer nach unten.
Regeln: [rules.md](rules.md), Aufbau: [architektur/](architektur/README.md).

## F-1: Links als leuchtende Sternchen in Fraktionsfarbe

- **Status:** offen
- **Gemeldet:** 2026-11-22 (Link Clip #16 Mondkreuz → Mooswacht, im Spiel keine Partikel gesehen)

### Wunsch

Ein Link soll im Spiel deutlich als **Strahl aus leuchtenden Sternchen** in der Farbe seiner Fraktion
zu sehen sein: Lumen grün, Wächter blau, Clip rot. Auch nachts, bei Regen und aus einiger
Entfernung.

### Stand heute (0.16.1)

- `NetworkRenderer.drawLinks` zeichnet alle 10 Ticks `Particle.DUST` in Fraktionsfarbe (Größe 1.2)
  entlang des Links, 5 Blöcke über den Kernen (`link.beam-height`), alle 1,5 Blöcke ein Partikel.
- Nur für Spieler, die waagerecht höchstens 64 Blöcke vom Link entfernt sind, und nur auf ±64 Blöcken
  um den nächsten Punkt.
- Die Farbe für Clip (Rot) ist seit 0.14.0 hinterlegt; Link #16 steht in der Datenbank, im Server-Log
  gibt es keine Fehler.

### Warum man trotzdem fast nichts sieht

1. **`DUST` leuchtet nicht.** Die Pünktchen werden vom Umgebungslicht abgedunkelt; nachts, unter
   Bäumen oder bei Regen sind sie kaum zu erkennen.
2. **Der Client verwirft Partikel weiter als 32 Blöcke**, wenn sie nicht als „Fernsicht“ (`force`)
   geschickt werden. Bis 64 Blöcke rechnet der Server also umsonst.
3. **Grafikeinstellung „Partikel: Verringert/Minimal“** lässt viele oder alle `DUST`-Partikel weg.
4. Ein Partikel je 1,5 Blöcke in Größe 1.2 ergibt eine dünne, gepunktete Linie, die vor hellem
   Himmel leicht übersehen wird.

### Umsetzung (Vorschlag)

- Partikel mit `force = true` schicken: im Spiel bis 256 Blöcke sichtbar und auch bei „Partikel: Minimal“.
  API prüfen: `World.spawnParticle` kennt `force`; für Partikel nur an einzelne Spieler die passende
  `Player.spawnParticle`-Variante in Spigot 26.3 suchen.
- Zwei Schichten:
  - **Farbe:** `DUST_COLOR_TRANSITION` von Fraktionsfarbe zu einer helleren Variante, Größe ~1.5,
    oder `DUST` größer und dichter.
  - **Leuchten:** vereinzelt leuchtende Funken, die nicht vom Licht abhängen (z. B. `END_ROD` oder
    `ELECTRIC_SPARK`, weiß), damit der Strahl „glitzert“. Prüfen, ob es einen leuchtenden Partikel gibt,
    der sich einfärben lässt; sonst die Farbe über die `DUST`-Schicht.
- Optional langsames „Wandern“ der Sternchen vom Ausgangs- zum Zielportal (zeigt die Richtung).
- Werte in die Konfiguration: `link.particle-size`, `link.particle-step`, `link.sparkle` (an/aus).
- Netzlast im Blick behalten: Partikel werden je Spieler geschickt (siehe ADMIN_GUIDE §7); Dichte nur
  nahe am Spieler erhöhen.
- Gleiches Konzept für Felder (`drawFields`) prüfen.

### Fertig, wenn

- Ein Link jeder Fraktion ist im Spiel bei Tag und Nacht aus 50 Blöcken klar als farbiger, leuchtender
  Strahl zu erkennen (Screenshot in `scrnshts/`).
- Grafikeinstellung „Partikel: Alle“ und „Verringert“ geprüft.
- Leistungstest (`tools/perf`) zeigt keine spürbare Mehrlast.
