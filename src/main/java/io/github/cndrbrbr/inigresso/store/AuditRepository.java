package io.github.cndrbrbr.inigresso.store;

import java.sql.PreparedStatement;

/** Tabelle audit: wer hat wann welchen Admin-Eingriff gemacht. */
public final class AuditRepository {

    private final Database db;

    public AuditRepository(Database db) {
        this.db = db;
    }

    public void log(String actor, String action, String detail) {
        long ts = System.currentTimeMillis();
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit (ts, actor, action, detail) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, ts);
                ps.setString(2, actor);
                ps.setString(3, action);
                ps.setString(4, detail);
                ps.executeUpdate();
            }
        });
    }
}
