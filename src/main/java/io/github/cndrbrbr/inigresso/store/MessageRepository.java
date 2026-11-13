package io.github.cndrbrbr.inigresso.store;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Nachrichten an Spieler, die gerade offline sind (Tabelle message). Hält sie im Speicher. */
public final class MessageRepository {

    private final Database db;
    private final Map<UUID, List<String>> pending = new HashMap<>();

    public MessageRepository(Database db) throws SQLException {
        this.db = db;
        try (PreparedStatement ps = db.connection().prepareStatement("SELECT uuid, text FROM message ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pending.computeIfAbsent(UUID.fromString(rs.getString(1)), k -> new ArrayList<>()).add(rs.getString(2));
            }
        }
    }

    public void add(UUID player, String text, long now) {
        pending.computeIfAbsent(player, k -> new ArrayList<>()).add(text);
        String id = player.toString();
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO message (uuid, text, created_at) VALUES (?, ?, ?)")) {
                ps.setString(1, id);
                ps.setString(2, text);
                ps.setLong(3, now);
                ps.executeUpdate();
            }
        });
    }

    /** Holt und löscht alle wartenden Nachrichten eines Spielers. */
    public List<String> take(UUID player) {
        List<String> out = pending.remove(player);
        if (out == null) {
            return List.of();
        }
        String id = player.toString();
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM message WHERE uuid = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        });
        return out;
    }
}
