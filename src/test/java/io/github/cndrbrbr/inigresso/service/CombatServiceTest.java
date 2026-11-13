package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CombatServiceTest {

    private PortalService portals;
    private CombatService combat;
    private Portal portal;
    private PlayerProfile owner;

    @BeforeEach
    void setUp() {
        GameEventBus bus = new GameEventBus(Logger.getLogger("test"));
        PlayerProgressService progress = new PlayerProgressService(bus);
        portals = new PortalService(bus, Clock.systemUTC(), () -> 4, progress, List.of());
        combat = new CombatService(portals, progress, bus, () -> 3);
        portal = portals.create("Turm", new BlockPos("world", 0, 64, 0), null);
        owner = player(Faction.WAECHTER, 0);
        portals.deploy(owner, portal, 1); // Platz N (0, -3), 1000 Energie
    }

    private static PlayerProfile player(Faction f, long ap) {
        PlayerProfile p = new PlayerProfile(UUID.randomUUID(), "p", null, ap);
        p.setFaction(f);
        return p;
    }

    private static BlockPos at(int x, int z) {
        return new BlockPos("world", x, 64, z);
    }

    @Test
    void schadenNimmtMitAbstandAb() {
        assertEquals(100, CombatService.damage(1, 0));
        assertEquals(50, CombatService.damage(1, 2));
        assertEquals(0, CombatService.damage(1, 4));
        assertEquals(800, CombatService.damage(8, 0));
        assertEquals(18, CombatService.radius(8));
    }

    @Test
    void trifftNurGegnerUndZerstoert() {
        PlayerProfile a = player(Faction.LUMEN, 0);
        CombatService.Result r = combat.burst(a, 1, at(0, -3));
        assertEquals(1, r.hits().size());
        assertEquals(100, r.hits().get(0).damage());
        assertEquals(900, portal.resonator(0).energy());
        for (int i = 0; i < 9; i++) {
            combat.burst(a, 1, at(0, -3));
        }
        assertEquals(0, portal.resonatorCount());
        assertEquals(Faction.NEUTRAL, portal.owner());
        assertEquals(75, a.ap(), "75 AP für den zerstörten Resonator");
    }

    @Test
    void eigeneFraktionUndAusserReichweite() {
        assertTrue(combat.burst(player(Faction.WAECHTER, 0), 1, at(0, -3)).hits().isEmpty());
        assertTrue(combat.burst(player(Faction.LUMEN, 0), 1, at(0, 10)).hits().isEmpty());
        assertTrue(combat.burst(player(Faction.LUMEN, 0), 1, new BlockPos("world", 0, 80, -3)).hits().isEmpty(),
                "zu großer Höhenunterschied");
        assertEquals(1000, portal.resonator(0).energy());
    }

    @Test
    void stufeDesSpielers() {
        assertEquals(CombatService.Problem.LEVEL_TOO_HIGH, combat.burst(player(Faction.LUMEN, 0), 2, at(0, 0)).problem());
    }

    @Test
    void neutralisierenMeldetPortal() {
        PlayerProfile a = player(Faction.LUMEN, 1_200_000);
        CombatService.Result r = combat.burst(a, 8, at(0, -3)); // 800
        assertEquals(List.of(), r.neutralized());
        r = combat.burst(a, 8, at(0, -3));
        assertEquals(List.of(portal), r.neutralized());
        assertTrue(r.hits().get(0).destroyed());
    }
}
