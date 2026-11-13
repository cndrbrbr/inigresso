package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.PortalService.DeployOutcome;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeployTest {

    private PortalService portals;
    private Portal portal;

    @BeforeEach
    void setUp() {
        GameEventBus bus = new GameEventBus(Logger.getLogger("test"));
        portals = new PortalService(bus, Clock.systemUTC(), () -> 4, new PlayerProgressService(bus), List.of());
        portal = portals.create("Turm", new BlockPos("world", 0, 64, 0), null);
    }

    private static PlayerProfile player(Faction f, long ap) {
        PlayerProfile p = new PlayerProfile(UUID.randomUUID(), "p", null, ap);
        p.setFaction(f);
        return p;
    }

    @Test
    void erobernUndAp() {
        PlayerProfile anna = player(Faction.LUMEN, 0);
        PortalService.DeployResult r = portals.deploy(anna, portal, 1);
        assertEquals(DeployOutcome.CAPTURED, r.outcome());
        assertEquals(0, r.slot());
        assertEquals(Faction.LUMEN, portal.owner());
        assertEquals(anna.uuid(), portal.capturedBy());
        assertEquals(500 + 125, anna.ap());
        assertEquals(1, portal.level());
        assertEquals(DeployOutcome.DEPLOYED, portals.deploy(anna, portal, 1).outcome());
        assertEquals(500 + 250, anna.ap());
    }

    @Test
    void gegnerischesPortal() {
        portals.deploy(player(Faction.LUMEN, 0), portal, 1);
        assertEquals(DeployOutcome.ENEMY_PORTAL, portals.deploy(player(Faction.WAECHTER, 0), portal, 1).outcome());
    }

    @Test
    void stufeDesSpielers() {
        assertEquals(DeployOutcome.LEVEL_TOO_HIGH, portals.deploy(player(Faction.LUMEN, 0), portal, 2).outcome());
        assertEquals(DeployOutcome.CAPTURED, portals.deploy(player(Faction.LUMEN, 2_500), portal, 2).outcome());
    }

    @Test
    void limitProSpielerUndStufe() {
        PlayerProfile hoch = player(Faction.LUMEN, 1_200_000);
        assertEquals(DeployOutcome.CAPTURED, portals.deploy(hoch, portal, 8).outcome());
        assertEquals(DeployOutcome.LIMIT_REACHED, portals.deploy(hoch, portal, 8).outcome());
        assertEquals(DeployOutcome.DEPLOYED, portals.deploy(hoch, portal, 6).outcome());
        assertEquals(DeployOutcome.DEPLOYED, portals.deploy(hoch, portal, 6).outcome());
        assertEquals(DeployOutcome.LIMIT_REACHED, portals.deploy(hoch, portal, 6).outcome());
        // anderer Spieler darf wieder L8
        assertEquals(DeployOutcome.DEPLOYED, portals.deploy(player(Faction.LUMEN, 1_200_000), portal, 8).outcome());
    }

    @Test
    void aufruestenErsetztSchwaechstenResonator() {
        PlayerProfile a = player(Faction.LUMEN, 1_200_000);
        for (int i = 0; i < 8; i++) {
            portals.deploy(a, portal, 1);
        }
        assertEquals(8, portal.resonatorCount());
        assertEquals(1, portal.level());
        PlayerProfile b = player(Faction.LUMEN, 1_200_000);
        PortalService.DeployResult r = portals.deploy(b, portal, 3);
        assertEquals(DeployOutcome.UPGRADED, r.outcome());
        assertEquals(3, portal.resonator(r.slot()).level());
        assertEquals(b.uuid(), portal.resonator(r.slot()).deployedBy());
        assertEquals(65, b.ap() - 1_200_000);
        // alle mindestens L1, L1 kann nichts mehr aufrüsten
        assertEquals(DeployOutcome.NOTHING_TO_UPGRADE, portals.deploy(b, portal, 1).outcome());
    }

    @Test
    void portalstufeIstDurchschnitt() {
        PlayerProfile a = player(Faction.LUMEN, 1_200_000);
        portals.deploy(a, portal, 8); // 8/8 = 1
        assertEquals(1, portal.level());
        portals.deploy(a, portal, 7);
        portals.deploy(a, portal, 6); // 21/8 = 2
        assertEquals(2, portal.level());
    }

    @Test
    void neutralisierenEntferntResonatoren() {
        portals.deploy(player(Faction.LUMEN, 0), portal, 1);
        portals.neutralize(portal);
        assertEquals(Faction.NEUTRAL, portal.owner());
        assertEquals(0, portal.resonatorCount());
        assertNull(portal.capturedBy());
        assertEquals(0, portal.level());
    }

    @Test
    void plaetzeImKreis() {
        assertArrayEquals(new int[] {0, -3}, Portal.slotOffset(0, 3));
        assertArrayEquals(new int[] {2, -2}, Portal.slotOffset(1, 3));
        assertArrayEquals(new int[] {3, 0}, Portal.slotOffset(2, 3));
        assertArrayEquals(new int[] {-2, -2}, Portal.slotOffset(7, 3));
    }
}
