package io.github.cndrbrbr.inigresso.store;

import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.service.ScoreService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Tabellen measurement und cycle (+ cycle_end, Migration V4). */
public final class ScoreRepository {

    private final Database db;
    private final StateRepository state;

    public ScoreRepository(Database db, StateRepository state) {
        this.db = db;
        this.state = state;
    }

    public long storedCycleStart() throws SQLException {
        return state.getLong("cycle_start", 0);
    }

    public List<ScoreService.Measurement> measurementsSince(long since) throws SQLException {
        List<ScoreService.Measurement> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT ts, lumen, waechter FROM measurement WHERE ts >= ? ORDER BY ts")) {
            ps.setLong(1, since);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ScoreService.Measurement(rs.getLong(1), rs.getLong(2), rs.getLong(3)));
                }
            }
        }
        return out;
    }

    /** Letzte Zyklusergebnisse, neueste zuerst (Server-Thread, kleine Tabelle). */
    public List<ScoreService.CycleResult> lastCycles(int limit) throws SQLException {
        List<ScoreService.CycleResult> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT start_ts, end_ts, lumen_avg, waechter_avg, measurements, winner FROM cycle ORDER BY start_ts DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String w = rs.getString(6);
                    out.add(new ScoreService.CycleResult(rs.getLong(1), rs.getLong(2), rs.getDouble(3), rs.getDouble(4),
                            rs.getInt(5), w == null ? null : Faction.valueOf(w)));
                }
            }
        }
        return out;
    }

    public ScoreService.Sink sink(java.util.function.Consumer<ScoreService.CycleResult> announce) {
        return new ScoreService.Sink() {
            @Override
            public void measured(ScoreService.Measurement m) {
                db.submit(c -> {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT OR REPLACE INTO measurement (ts, lumen, waechter) VALUES (?, ?, ?)")) {
                        ps.setLong(1, m.ts());
                        ps.setLong(2, m.lumen());
                        ps.setLong(3, m.waechter());
                        ps.executeUpdate();
                    }
                });
            }

            @Override
            public void cycleClosed(ScoreService.CycleResult r) {
                db.submit(c -> {
                    try (PreparedStatement ps = c.prepareStatement("""
                            INSERT OR REPLACE INTO cycle (start_ts, end_ts, lumen_avg, waechter_avg, measurements, winner)
                            VALUES (?, ?, ?, ?, ?, ?)""")) {
                        ps.setLong(1, r.start());
                        ps.setLong(2, r.end());
                        ps.setDouble(3, r.lumenAvg());
                        ps.setDouble(4, r.waechterAvg());
                        ps.setInt(5, r.measurements());
                        ps.setString(6, r.winner() == null ? null : r.winner().name());
                        ps.executeUpdate();
                    }
                });
                announce.accept(r);
            }

            @Override
            public void currentCycle(long start) {
                state.set("cycle_start", Long.toString(start));
            }
        };
    }
}
