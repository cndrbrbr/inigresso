package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CaptureStatsTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();

    private static GameEvent.PortalCaptured cap(UUID p, Faction f, long ts) {
        return new GameEvent.PortalCaptured(1, p, f, ts);
    }

    @Test
    void zaehltJeFraktionAbZyklusbeginn() {
        var stats = new CaptureStats(new GameEventBus(Logger.getLogger("t")), List.of(
                cap(A, Faction.LUMEN, 50), cap(A, Faction.LUMEN, 150), cap(B, Faction.WAECHTER, 160),
                cap(A, Faction.LUMEN, 170)));
        assertEquals(2, stats.count(Faction.LUMEN, 100));
        assertEquals(3, stats.count(Faction.LUMEN, 0));
        assertEquals(1, stats.count(Faction.WAECHTER, 100));
        stats.prune(100);
        assertEquals(2, stats.count(Faction.LUMEN, 0));
    }

    @Test
    void ranglisteMitGleichstand() {
        var stats = new CaptureStats(new GameEventBus(Logger.getLogger("t")), List.of(
                cap(B, Faction.WAECHTER, 10), cap(C, Faction.LUMEN, 5), cap(A, Faction.LUMEN, 20),
                cap(A, Faction.LUMEN, 30), cap(B, Faction.WAECHTER, 40)));
        var top = stats.top(0, 3);
        assertEquals(3, top.size());
        // A und B je 2; A war zuerst bei 2 (30 < 40)
        assertEquals(A, top.get(0).player());
        assertEquals(B, top.get(1).player());
        assertEquals(C, top.get(2).player());
        assertEquals(2, stats.top(0, 2).size());
    }

    @Test
    void eroberungKommtUeberDenBus() {
        GameEventBus bus = new GameEventBus(Logger.getLogger("t"));
        var stats = new CaptureStats(bus, List.of());
        var portals = new PortalService(bus, Clock.systemUTC(), () -> 4, new PlayerProgressService(bus), List.of());
        var portal = portals.create("Turm", new BlockPos("world", 0, 64, 0), null);
        PlayerProfile p = new PlayerProfile(UUID.randomUUID(), "p", null, 0);
        p.setFaction(Faction.WAECHTER);
        portals.deploy(p, portal, 1);
        portals.deploy(p, portal, 1); // zweiter Resonator ist keine Eroberung
        assertEquals(1, stats.count(Faction.WAECHTER, 0));
        assertEquals(p.uuid(), stats.top(0, 1).get(0).player());
    }
}
