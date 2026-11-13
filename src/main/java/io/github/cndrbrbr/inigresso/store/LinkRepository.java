package io.github.cndrbrbr.inigresso.store;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Field;
import io.github.cndrbrbr.inigresso.model.Link;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tabellen link und field (field.area enthält den Einfluss). */
public final class LinkRepository {

    private final Database db;

    public LinkRepository(Database db) {
        this.db = db;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.LinkCreated.class, e -> {
            Link l = e.link();
            db.submit(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO link (id, from_portal, to_portal, faction, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, l.id());
                    ps.setInt(2, l.from());
                    ps.setInt(3, l.to());
                    ps.setString(4, l.faction().name());
                    ps.setString(5, l.createdBy().toString());
                    ps.setLong(6, l.createdAt());
                    ps.executeUpdate();
                }
            });
        });
        bus.subscribe(GameEvent.LinkRemoved.class, e -> delete("link", e.link().id()));
        bus.subscribe(GameEvent.FieldCreated.class, e -> {
            Field f = e.field();
            db.submit(c -> {
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO field (id, portal_a, portal_b, portal_c, faction, area, created_by, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)""")) {
                    ps.setInt(1, f.id());
                    ps.setInt(2, f.a());
                    ps.setInt(3, f.b());
                    ps.setInt(4, f.c());
                    ps.setString(5, f.faction().name());
                    ps.setLong(6, f.influence());
                    ps.setString(7, f.createdBy().toString());
                    ps.setLong(8, f.createdAt());
                    ps.executeUpdate();
                }
            });
        });
        bus.subscribe(GameEvent.FieldRemoved.class, e -> delete("field", e.field().id()));
    }

    private void delete(String table, int id) {
        db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        });
    }

    public List<Link> loadLinks() throws SQLException {
        List<Link> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT id, from_portal, to_portal, faction, created_by, created_at FROM link");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Link(rs.getInt(1), rs.getInt(2), rs.getInt(3), Faction.valueOf(rs.getString(4)),
                        UUID.fromString(rs.getString(5)), rs.getLong(6)));
            }
        }
        return out;
    }

    public List<Field> loadFields() throws SQLException {
        List<Field> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT id, portal_a, portal_b, portal_c, faction, area, created_by, created_at FROM field");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Field(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                        Faction.valueOf(rs.getString(5)), rs.getLong(6), UUID.fromString(rs.getString(7)), rs.getLong(8)));
            }
        }
        return out;
    }
}
