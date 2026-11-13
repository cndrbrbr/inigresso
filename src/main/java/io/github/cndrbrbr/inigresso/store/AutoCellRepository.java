package io.github.cndrbrbr.inigresso.store;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/** Tabelle auto_cell: welche Rasterzellen schon geprüft wurden. */
public final class AutoCellRepository {

    private final Database db;

    public AutoCellRepository(Database db) {
        this.db = db;
    }

    /** Schlüssel world:gx:gz aller geprüften Zellen. */
    public Set<String> loadDone() throws SQLException {
        Set<String> out = new HashSet<>();
        try (PreparedStatement ps = db.connection().prepareStatement("SELECT world, gx, gz FROM auto_cell");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(rs.getString(1) + ":" + rs.getInt(2) + ":" + rs.getInt(3));
            }
        }
        return out;
    }

    public void mark(String world, int gx, int gz, Integer portalId) {
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR REPLACE INTO auto_cell (world, gx, gz, portal_id) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, world);
                ps.setInt(2, gx);
                ps.setInt(3, gz);
                if (portalId == null) {
                    ps.setNull(4, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(4, portalId);
                }
                ps.executeUpdate();
            }
        });
    }
}
