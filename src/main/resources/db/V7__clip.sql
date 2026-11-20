-- Dritte Fraktion Clip (rot): Spalten für Messpunkte und Zyklusergebnisse, alte Zeilen bekommen 0.
ALTER TABLE measurement ADD COLUMN clip INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cycle ADD COLUMN clip_avg REAL NOT NULL DEFAULT 0;
