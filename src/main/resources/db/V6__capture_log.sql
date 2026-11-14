-- Portaleroberungen (neutral → Fraktion) für Scoreboard und Rangliste je Zyklus.
CREATE TABLE capture_log (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    portal_id INTEGER NOT NULL,
    faction   TEXT    NOT NULL,
    uuid      TEXT    NOT NULL,
    ts        INTEGER NOT NULL
);
CREATE INDEX capture_log_ts ON capture_log (ts);
