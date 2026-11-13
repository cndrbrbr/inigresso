package io.github.cndrbrbr.inigresso.store;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tabelle hack_log; alte Einträge werden beim Start aufgeräumt. */
public final class HackRepository {

    private final Database db;

    public HackRepository(Database db) {
        this.db = db;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.Hacked.class, e -> {
            String id = e.player().toString();
            db.submit(c -> {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO hack_log (uuid, portal_id, ts) VALUES (?, ?, ?)")) {
                    ps.setString(1, id);
                    ps.setInt(2, e.portalId());
                    ps.setLong(3, e.timestamp());
                    ps.executeUpdate();
                }
            });
        });
    }

    /** Hacks seit since; ältere werden gelöscht. Beim Start auf dem Server-Thread. */
    public List<GameEvent.Hacked> loadSince(long since) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM hack_log WHERE ts < ?")) {
            ps.setLong(1, since);
            ps.executeUpdate();
        }
        List<GameEvent.Hacked> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement("SELECT uuid, portal_id, ts FROM hack_log");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new GameEvent.Hacked(UUID.fromString(rs.getString(1)), rs.getInt(2), rs.getLong(3)));
            }
        }
        return out;
    }
}
