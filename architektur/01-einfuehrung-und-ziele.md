# 1. Einführung und Ziele

## 1.1 Aufgabe

Inigresso ist ein dauerhaft laufendes Territorialspiel als Spigot-Plugin. Drei Fraktionen
(Lumen grün, Wächter blau, Clip rot) erobern Portale in der Welt, verbinden sie mit Links und
spannen aus drei Links Felder auf. Die Fläche der Felder ergibt Einfluss; wer im Wochenzyklus
den höchsten Durchschnitt hat, gewinnt.

Die vollständigen fachlichen Anforderungen sind die Spielregeln in [rules.md](../rules.md).
Dieses Dokument wiederholt sie nicht.

Wichtigste Funktionen:

- Portale: von Spielern vorgeschlagen und von Admins freigegeben, oder automatisch je Rasterzelle
- Resonatoren setzen, hacken, Bursts, Energie und täglicher Verfall
- Links mit Kreuzungsprüfung, Felder (größtes Dreieck je Seite)
- Stündliche Messpunkte, Wochenzyklus, Ergebnisse
- Anzeige im Spiel (Leitstein, Displays, Partikel, Bossbar, Sidebar, Scanner-Karte) und auf
  der Webkarte (BlueMap)

## 1.2 Qualitätsziele

| Prio | Ziel | Woran man es merkt |
|---|---|---|
| 1 | **Korrekte Spiellogik** | Regeln ohne Server testbar; 109 Unit-Tests (Stand 0.16.1) |
| 2 | **Kein Datenverlust, Selbstheilung** | Spielstand nur in der DB; Anzeigen werden neu erzeugt; verpasster Verfall und Zyklus werden nach Ausfall nachgeholt |
| 3 | **Leicht für den Server** | Lasttest 200 Portale / 509 Links / 315 Felder, 20 Spieler: typisch 1–2 %, höchstens ~6 % der Tickzeit |
| 4 | **Einfacher Betrieb** | Eine Jar, eine DB-Datei, keine Pflichtabhängigkeit; Migrationen laufen beim Start |
| 5 | **Für Kinder und Gelegenheitsspieler verständlich** | Deutsche Meldungen, Scanner und Zielpfeil statt Koordinaten |

## 1.3 Beteiligte

| Rolle | Erwartung |
|---|---|
| Spieler (meist Kinder/Jugendliche auf meckminecraft) | Spaß, verständliche Meldungen, nichts geht verloren |
| Admins | Vorschläge entscheiden, eingreifen können, wenig Aufwand |
| Entwickler (cndrbrbr, Claude) | Änderungen schnell und ohne Angst vor Nebenwirkungen |

## 1.4 Randbedingungen

- Spigot 26.3 (`api-version: '26.3'`), Java 25, Maven; Bau im Docker-Image
  `maven:3.9-eclipse-temurin-25`.
- Keine fremden Bibliotheken im Jar: SQLite-Treiber kommt mit Spigot, BlueMap-API nur `provided`.
- Spielbegriffe und Fraktionsnamen sind eigene, nicht die des Vorbilds.
- Sprache im Spiel und in der Doku: Deutsch.
