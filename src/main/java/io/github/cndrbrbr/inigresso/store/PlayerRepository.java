package io.github.cndrbrbr.inigresso.store;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tabelle player (+ player_item beim Fraktionswechsel). */
public final class PlayerRepository {

    private final Database db;

    public PlayerRepository(Database db) {
        this.db = db;
    }

    /** Beim Start auf dem Server-Thread. */
    public List<PlayerProfile> loadAll() throws SQLException {
        List<PlayerProfile> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement("SELECT uuid, last_name, faction, ap FROM player");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String f = rs.getString(3);
                out.add(new PlayerProfile(UUID.fromString(rs.getString(1)), rs.getString(2),
                        f == null ? null : Faction.valueOf(f), rs.getLong(4)));
            }
        }
        return out;
    }

    /** Hört auf Profiländerungen und schreibt sie asynchron weg. */
    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.ProfileChanged.class, e -> save(e.profile()));
        bus.subscribe(GameEvent.FactionChanged.class, e -> {
            if (e.previous() != null) {
                clearItems(e.profile().uuid());
            }
        });
    }

    public void save(PlayerProfile p) {
        // Werte jetzt (Server-Thread) abgreifen, nicht erst im Schreib-Thread.
        String uuid = p.uuid().toString();
        String name = p.lastName();
        String faction = p.faction() == null ? null : p.faction().name();
        long ap = p.ap();
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO player (uuid, last_name, faction, ap) VALUES (?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET last_name = excluded.last_name,
                        faction = excluded.faction, ap = excluded.ap""")) {
                ps.setString(1, uuid);
                ps.setString(2, name);
                ps.setString(3, faction);
                ps.setLong(4, ap);
                ps.executeUpdate();
            }
        });
    }

    private void clearItems(UUID uuid) {
        String id = uuid.toString();
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM player_item WHERE uuid = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        });
    }
}
