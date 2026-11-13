package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Proposal;
import io.github.cndrbrbr.inigresso.service.ProposalService.Problem;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProposalServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
    private final List<GameEvent> events = new ArrayList<>();
    private ProfileService profiles;
    private PortalService portals;
    private ProposalService proposals;
    private PlayerProfile anna;

    @BeforeEach
    void setUp() {
        GameEventBus bus = new GameEventBus(Logger.getLogger("test"));
        bus.subscribe(GameEvent.class, events::add);
        profiles = new ProfileService(bus, List.of());
        PlayerProgressService progress = new PlayerProgressService(bus);
        portals = new PortalService(bus, clock, () -> 4, List.of());
        proposals = new ProposalService(bus, clock,
                () -> new ProposalService.Rules(List.of("world"), 48, 2, 3), portals, progress, profiles, List.of());
        anna = profiles.join(UUID.randomUUID(), "anna");
        anna.setFaction(Faction.LUMEN);
        anna.setAp(2_500);
    }

    private static BlockPos at(int x, int z) {
        return new BlockPos("world", x, 64, z);
    }

    private ProposalService.Result propose(String name, int x, int z) {
        return proposals.propose(anna, name, at(x, z));
    }

    @Test
    void gueltigerVorschlag() {
        ProposalService.Result r = propose("  Alte   Mühle ", 0, 0);
        assertTrue(r.ok());
        assertEquals("Alte Mühle", r.proposal().name());
        assertEquals(1, r.proposal().id());
        assertEquals(clock.millis(), r.proposal().createdAt());
    }

    @Test
    void voraussetzungen() {
        PlayerProfile neu = profiles.join(UUID.randomUUID(), "neu");
        assertEquals(Problem.NO_FACTION, proposals.propose(neu, "Turm", at(0, 0)).problem());
        neu.setFaction(Faction.WAECHTER);
        assertEquals(Problem.LEVEL_TOO_LOW, proposals.propose(neu, "Turm", at(0, 0)).problem());
        assertEquals(Problem.WORLD_NOT_ALLOWED,
                proposals.propose(anna, "Turm", new BlockPos("world_nether", 0, 64, 0)).problem());
        assertEquals(Problem.NAME_INVALID, propose("ab", 0, 0).problem());
        assertEquals(Problem.NAME_INVALID, propose("12345", 0, 0).problem());
        assertEquals(Problem.NAME_INVALID, propose("#Turm", 0, 0).problem());
        assertEquals(Problem.NAME_INVALID, propose("x".repeat(33), 0, 0).problem());
    }

    @Test
    void hoechstensDreiOffene() {
        assertTrue(propose("Eins", 0, 0).ok());
        assertTrue(propose("Zwei", 100, 0).ok());
        assertTrue(propose("Drei", 200, 0).ok());
        assertEquals(Problem.TOO_MANY_OPEN, propose("Vier", 300, 0).problem());
    }

    @Test
    void abstandUndName() {
        assertTrue(propose("Turm", 0, 0).ok());
        assertEquals(Problem.NAME_TAKEN, propose("TURM", 500, 0).problem());
        assertEquals(Problem.TOO_CLOSE_TO_PROPOSAL, propose("Brücke", 47, 0).problem());
        assertTrue(propose("Brücke", 48, 0).ok(), "genau 48 Blöcke sind erlaubt");
    }

    @Test
    void annehmenLegtPortalAnUndGibtAp() {
        Proposal p = propose("Turm", 0, 0).proposal();
        events.clear();
        assertTrue(proposals.accept(p, "admin").ok());
        assertEquals(Proposal.Status.ANGENOMMEN, p.status());
        Portal portal = portals.byName("turm").orElseThrow();
        assertEquals(Faction.NEUTRAL, portal.owner());
        assertEquals(anna.uuid(), portal.proposer());
        assertEquals(3_500, anna.ap());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.Notify n && n.player().equals(anna.uuid())));
        assertTrue(proposals.open().isEmpty());
        assertThrows(IllegalStateException.class, () -> proposals.reject(p, "admin", "x"));
        // neuer Vorschlag zu nah am jetzt bestehenden Portal
        assertEquals(Problem.TOO_CLOSE_TO_PORTAL, propose("Nebenan", 10, 10).problem());
    }

    @Test
    void annehmenPrueftAbstandErneut() {
        Proposal a = propose("Turm", 0, 0).proposal();
        // Admin hat inzwischen direkt daneben ein Portal verschoben
        portals.create("Admin-Portal", at(5, 5), null);
        ProposalService.Result r = proposals.accept(a, "admin");
        assertFalse(r.ok());
        assertEquals(Problem.TOO_CLOSE_TO_PORTAL, r.problem());
        assertTrue(a.isOpen());
    }

    @Test
    void ablehnenBenachrichtigt() {
        Proposal p = propose("Turm", 0, 0).proposal();
        proposals.reject(p, "admin", "kein Bauwerk");
        assertEquals(Proposal.Status.ABGELEHNT, p.status());
        assertEquals("kein Bauwerk", p.reason());
        assertTrue(events.contains(new GameEvent.Notify(anna.uuid(), "§cDein Portalvorschlag §fTurm§c wurde abgelehnt: §fkein Bauwerk")));
        assertEquals(2_500, anna.ap());
    }
}
