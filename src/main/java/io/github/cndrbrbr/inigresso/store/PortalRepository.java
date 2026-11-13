package io.github.cndrbrbr.inigresso.store;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Proposal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tabellen portal und proposal. IDs vergeben die Services, damit sie sofort bekannt sind. */
public final class PortalRepository {

    private final Database db;

    public PortalRepository(Database db) {
        this.db = db;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.PortalChanged.class, e -> savePortal(e.portal()));
        bus.subscribe(GameEvent.PortalRemoved.class, e -> deletePortal(e.portal().id()));
        bus.subscribe(GameEvent.ProposalChanged.class, e -> saveProposal(e.proposal()));
    }

    public List<Portal> loadPortals() throws SQLException {
        List<Portal> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT id, name, world, x, y, z, owner, captured_by, captured_at, proposer, created_at FROM portal");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Portal(rs.getInt(1), rs.getString(2),
                        new BlockPos(rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6)),
                        Faction.valueOf(rs.getString(7)), uuid(rs.getString(8)), rs.getLong(9),
                        uuid(rs.getString(10)), rs.getLong(11)));
            }
        }
        return out;
    }

    public List<Proposal> loadProposals() throws SQLException {
        List<Proposal> out = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT id, name, world, x, y, z, proposer, created_at, status, decided_by, decided_at, reason FROM proposal");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Proposal(rs.getInt(1), rs.getString(2),
                        new BlockPos(rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6)),
                        UUID.fromString(rs.getString(7)), rs.getLong(8), Proposal.Status.valueOf(rs.getString(9)),
                        rs.getString(10), rs.getLong(11), rs.getString(12)));
            }
        }
        return out;
    }

    private static UUID uuid(String s) {
        return s == null ? null : UUID.fromString(s);
    }

    private void savePortal(Portal p) {
        int id = p.id();
        String name = p.name();
        BlockPos c = p.core();
        String owner = p.owner().name();
        String capturedBy = p.capturedBy() == null ? null : p.capturedBy().toString();
        long capturedAt = p.capturedAt();
        String proposer = p.proposer() == null ? null : p.proposer().toString();
        long createdAt = p.createdAt();
        db.submit(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO portal (id, name, world, x, y, z, owner, captured_by, captured_at, proposer, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET name = excluded.name, world = excluded.world,
                        x = excluded.x, y = excluded.y, z = excluded.z, owner = excluded.owner,
                        captured_by = excluded.captured_by, captured_at = excluded.captured_at""")) {
                ps.setInt(1, id);
                ps.setString(2, name);
                ps.setString(3, c.world());
                ps.setInt(4, c.x());
                ps.setInt(5, c.y());
                ps.setInt(6, c.z());
                ps.setString(7, owner);
                ps.setString(8, capturedBy);
                ps.setLong(9, capturedAt);
                ps.setString(10, proposer);
                ps.setLong(11, createdAt);
                ps.executeUpdate();
            }
        });
    }

    private void deletePortal(int id) {
        db.submit(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM portal WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        });
    }

    private void saveProposal(Proposal p) {
        int id = p.id();
        String name = p.name();
        BlockPos pos = p.pos();
        String proposer = p.proposer().toString();
        long createdAt = p.createdAt();
        String status = p.status().name();
        String decidedBy = p.decidedBy();
        long decidedAt = p.decidedAt();
        String reason = p.reason();
        db.submit(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO proposal (id, name, world, x, y, z, proposer, created_at, status, decided_by, decided_at, reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET status = excluded.status, decided_by = excluded.decided_by,
                        decided_at = excluded.decided_at, reason = excluded.reason""")) {
                ps.setInt(1, id);
                ps.setString(2, name);
                ps.setString(3, pos.world());
                ps.setInt(4, pos.x());
                ps.setInt(5, pos.y());
                ps.setInt(6, pos.z());
                ps.setString(7, proposer);
                ps.setLong(8, createdAt);
                ps.setString(9, status);
                ps.setString(10, decidedBy);
                ps.setLong(11, decidedAt);
                ps.setString(12, reason);
                ps.executeUpdate();
            }
        });
    }
}
