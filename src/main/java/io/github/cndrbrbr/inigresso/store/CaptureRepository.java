package io.github.cndrbrbr.inigresso.store;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tabelle capture_log: jede Portaleroberung. */
public final class CaptureRepository {

    private final Database db;

    public CaptureRepository(Database db) {
        this.db = db;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.PortalCaptured.class, e -> db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO capture_log (portal_id, faction, uuid, ts) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, e.portalId());
                ps.setString(2, e.faction().name());
                ps.setString(3, e.player().toString());
                ps.setLong(4, e.timestamp());
                ps.executeUpdate();
            }
        }));
    }

    /** Eroberungen ab since (beim Start auf dem Server-Thread). */
    public List<GameEvent.PortalCaptured> loadSince(long since) throws SQLException {
        List<GameEvent.PortalCaptured> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT portal_id, uuid, faction, ts FROM capture_log WHERE ts >= ? ORDER BY ts")) {
            ps.setLong(1, since);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new GameEvent.PortalCaptured(rs.getInt(1), UUID.fromString(rs.getString(2)),
                            Faction.valueOf(rs.getString(3)), rs.getLong(4)));
                }
            }
        }
        return out;
    }
}
