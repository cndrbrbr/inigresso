package io.github.cndrbrbr.inigresso.store;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.service.LinkStats;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tabelle link_log: jeder gezogene Link (bleibt, auch wenn der Link später fällt). */
public final class LinkLogRepository {

    private final Database db;

    public LinkLogRepository(Database db) {
        this.db = db;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.LinkCreated.class, e -> db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO link_log (link_id, faction, uuid, ts) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, e.link().id());
                ps.setString(2, e.link().faction().name());
                ps.setString(3, e.link().createdBy().toString());
                ps.setLong(4, e.link().createdAt());
                ps.executeUpdate();
            }
        }));
    }

    /** Gezogene Links ab since (beim Start auf dem Server-Thread). */
    public List<LinkStats.Drawn> loadSince(long since) throws SQLException {
        List<LinkStats.Drawn> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT uuid, ts FROM link_log WHERE ts >= ? ORDER BY ts")) {
            ps.setLong(1, since);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new LinkStats.Drawn(UUID.fromString(rs.getString(1)), rs.getLong(2)));
                }
            }
        }
        return out;
    }
}
