CREATE TABLE portal (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    world       TEXT    NOT NULL,
    x           INTEGER NOT NULL,
    y           INTEGER NOT NULL,
    z           INTEGER NOT NULL,
    owner       TEXT    NOT NULL DEFAULT 'NEUTRAL',
    captured_by TEXT,
    captured_at INTEGER,
    created_at  INTEGER NOT NULL
);

CREATE TABLE resonator (
    portal_id   INTEGER NOT NULL REFERENCES portal(id) ON DELETE CASCADE,
    slot        INTEGER NOT NULL CHECK (slot BETWEEN 0 AND 7),
    level       INTEGER NOT NULL CHECK (level BETWEEN 1 AND 8),
    energy      INTEGER NOT NULL,
    deployed_by TEXT    NOT NULL,
    PRIMARY KEY (portal_id, slot)
);

CREATE TABLE link (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    from_portal INTEGER NOT NULL REFERENCES portal(id) ON DELETE CASCADE,
    to_portal   INTEGER NOT NULL REFERENCES portal(id) ON DELETE CASCADE,
    faction     TEXT    NOT NULL,
    created_by  TEXT    NOT NULL,
    created_at  INTEGER NOT NULL
);

CREATE TABLE field (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    portal_a    INTEGER NOT NULL REFERENCES portal(id) ON DELETE CASCADE,
    portal_b    INTEGER NOT NULL REFERENCES portal(id) ON DELETE CASCADE,
    portal_c    INTEGER NOT NULL REFERENCES portal(id) ON DELETE CASCADE,
    faction     TEXT    NOT NULL,
    area        INTEGER NOT NULL,
    created_by  TEXT    NOT NULL,
    created_at  INTEGER NOT NULL
);

CREATE TABLE proposal (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    world       TEXT    NOT NULL,
    x           INTEGER NOT NULL,
    y           INTEGER NOT NULL,
    z           INTEGER NOT NULL,
    proposer    TEXT    NOT NULL,
    created_at  INTEGER NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'OFFEN',
    decided_by  TEXT,
    decided_at  INTEGER,
    reason      TEXT
);

CREATE TABLE player (
    uuid        TEXT    PRIMARY KEY,
    last_name   TEXT    NOT NULL,
    faction     TEXT,
    ap          INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE player_item (
    uuid        TEXT    NOT NULL REFERENCES player(uuid) ON DELETE CASCADE,
    item        TEXT    NOT NULL,
    level       INTEGER NOT NULL DEFAULT 0,
    portal_id   INTEGER NOT NULL DEFAULT 0,
    count       INTEGER NOT NULL,
    PRIMARY KEY (uuid, item, level, portal_id)
);

CREATE TABLE hack_log (
    uuid        TEXT    NOT NULL,
    portal_id   INTEGER NOT NULL,
    ts          INTEGER NOT NULL
);
CREATE INDEX hack_log_idx ON hack_log (uuid, portal_id, ts);

CREATE TABLE measurement (
    ts          INTEGER PRIMARY KEY,
    lumen       INTEGER NOT NULL,
    waechter    INTEGER NOT NULL
);

CREATE TABLE cycle (
    start_ts    INTEGER PRIMARY KEY,
    lumen_avg   REAL    NOT NULL,
    waechter_avg REAL   NOT NULL,
    winner      TEXT
);

CREATE TABLE message (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid        TEXT    NOT NULL,
    text        TEXT    NOT NULL,
    created_at  INTEGER NOT NULL
);

CREATE TABLE audit (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    ts          INTEGER NOT NULL,
    actor       TEXT    NOT NULL,
    action      TEXT    NOT NULL,
    detail      TEXT
);
