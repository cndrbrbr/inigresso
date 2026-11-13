package io.github.cndrbrbr.inigresso.store;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Einfache Schlüssel/Wert-Tabelle state für Zeitpunkte wie letzter Verfall oder Messpunkt. */
public final class StateRepository {

    private final Database db;

    public StateRepository(Database db) {
        this.db = db;
    }

    /** Lesen beim Start (Server-Thread). */
    public Optional<String> get(String key) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement("SELECT value FROM state WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public long getLong(String key, long def) throws SQLException {
        return get(key).map(Long::parseLong).orElse(def);
    }

    public void set(String key, String value) {
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO state (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            }
        });
    }
}
