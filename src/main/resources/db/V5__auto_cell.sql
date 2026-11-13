-- Automatisch geprüfte Rasterzellen (rules.md §2.4). portal_id NULL = kein geeigneter Platz.
CREATE TABLE auto_cell (
    world     TEXT    NOT NULL,
    gx        INTEGER NOT NULL,
    gz        INTEGER NOT NULL,
    portal_id INTEGER,
    PRIMARY KEY (world, gx, gz)
);
