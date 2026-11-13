package io.github.cndrbrbr.inigresso.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Proposal;
import io.github.cndrbrbr.inigresso.service.PlayerProgressService;
import io.github.cndrbrbr.inigresso.service.PortalService;
import io.github.cndrbrbr.inigresso.service.ProfileService;
import io.github.cndrbrbr.inigresso.service.ProposalService;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PortalRepositoryTest {

    private static final Logger LOG = Logger.getLogger("test");

    @TempDir
    Path dir;

    @Test
    void portaleUndVorschlaegeUeberlebenNeustart() throws Exception {
        Path file = dir.resolve("db");
        UUID anna = UUID.randomUUID();
        try (Database db = new Database(file, LOG)) {
            GameEventBus bus = new GameEventBus(LOG);
            PortalRepository repo = new PortalRepository(db);
            repo.bind(bus);
            MessageRepository messages = new MessageRepository(db);
            ProfileService profiles = new ProfileService(bus, List.of());
            PortalService portals = new PortalService(bus, Clock.systemUTC(), () -> 4, repo.loadPortals());
            ProposalService proposals = new ProposalService(bus, Clock.systemUTC(),
                    () -> new ProposalService.Rules(List.of("world"), 48, 1, 3), portals,
                    new PlayerProgressService(bus), profiles, repo.loadProposals());
            PlayerProfile a = profiles.join(anna, "anna");
            a.setFaction(Faction.LUMEN);

            Proposal turm = proposals.propose(a, "Turm", new BlockPos("world", 1, 64, 2)).proposal();
            Proposal bruecke = proposals.propose(a, "Brücke", new BlockPos("world", 300, 70, 0)).proposal();
            proposals.propose(a, "Offen", new BlockPos("world", 600, 70, 0));
            proposals.accept(turm, "admin");
            proposals.reject(bruecke, "admin", "zu klein");
            Portal p = portals.byName("Turm").orElseThrow();
            portals.rename(p, "Großer Turm");
            portals.move(p, new BlockPos("world", 5, 65, 6));
            Portal weg = portals.create("Weg", new BlockPos("world", 900, 64, 0), null);
            portals.remove(weg);
            messages.add(anna, "hallo", 1);
        }
        try (Database db = new Database(file, LOG)) {
            assertEquals(2, db.schemaVersion());
            PortalRepository repo = new PortalRepository(db);
            List<Portal> portals = repo.loadPortals();
            assertEquals(1, portals.size());
            Portal p = portals.get(0);
            assertEquals("Großer Turm", p.name());
            assertEquals(new BlockPos("world", 5, 65, 6), p.core());
            assertEquals(anna, p.proposer());
            assertEquals(Faction.NEUTRAL, p.owner());
            assertNull(p.capturedBy());

            List<Proposal> proposals = repo.loadProposals();
            assertEquals(3, proposals.size());
            Proposal b = proposals.stream().filter(x -> x.name().equals("Brücke")).findFirst().orElseThrow();
            assertEquals(Proposal.Status.ABGELEHNT, b.status());
            assertEquals("zu klein", b.reason());
            assertEquals("admin", b.decidedBy());
            assertEquals(1, proposals.stream().filter(Proposal::isOpen).count());

            MessageRepository messages = new MessageRepository(db);
            assertEquals(List.of("hallo"), messages.take(anna));
            assertEquals(List.of(), messages.take(anna));
        }
        try (Database db = new Database(file, LOG)) {
            assertEquals(List.of(), new MessageRepository(db).take(anna), "take löscht auch in der DB");
        }
    }
}
