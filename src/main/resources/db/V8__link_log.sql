-- Gezogene Links je Spieler fuer die Rangliste Top-Linker je Zyklus.
-- Zerstoerte Links bleiben hier stehen, gezaehlt wird das Ziehen.
CREATE TABLE link_log (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    link_id INTEGER NOT NULL,
    faction TEXT    NOT NULL,
    uuid    TEXT    NOT NULL,
    ts      INTEGER NOT NULL
);
CREATE INDEX link_log_ts ON link_log (ts);
-- Bestehende Links uebernehmen, damit der laufende Zyklus nicht bei null beginnt.
INSERT INTO link_log (link_id, faction, uuid, ts) SELECT id, faction, created_by, created_at FROM link
