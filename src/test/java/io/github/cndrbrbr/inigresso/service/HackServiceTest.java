package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.GameItem;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HackServiceTest {

    /** Uhr, die der Test vorstellen kann. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }
    }

    private final TestClock clock = new TestClock();
    private GameEventBus bus;
    private PortalService portals;
    private PlayerProgressService progress;
    private Portal portal;

    @BeforeEach
    void setUp() {
        bus = new GameEventBus(Logger.getLogger("test"));
        progress = new PlayerProgressService(bus);
        portals = new PortalService(bus, clock, () -> 4, progress, List.of());
        portal = portals.create("Turm", new BlockPos("world", 0, 64, 0), null);
    }

    private HackService service(double keyChance, List<GameEvent.Hacked> history) {
        return new HackService(bus, clock, new Random(42), () -> new HackService.Rules(5, 4, keyChance),
                progress, history);
    }

    private static PlayerProfile player(Faction f) {
        PlayerProfile p = new PlayerProfile(UUID.randomUUID(), "p", null, 0);
        p.setFaction(f);
        return p;
    }

    @Test
    void abklingzeitUndVierInVierStunden() {
        HackService s = service(0.3, List.of());
        PlayerProfile a = player(Faction.LUMEN);
        assertTrue(s.hack(a, portal).ok());
        HackService.Result r = s.hack(a, portal);
        assertEquals(HackService.Problem.COOLDOWN, r.problem());
        assertEquals(5 * 60_000, r.waitMs());
        clock.advance(Duration.ofMinutes(5));
        assertTrue(s.hack(a, portal).ok());
        clock.advance(Duration.ofMinutes(5));
        assertTrue(s.hack(a, portal).ok());
        clock.advance(Duration.ofMinutes(5));
        assertTrue(s.hack(a, portal).ok());
        clock.advance(Duration.ofMinutes(5));
        r = s.hack(a, portal);
        assertEquals(HackService.Problem.BURNED_OUT, r.problem());
        assertEquals(Duration.ofHours(4).minusMinutes(20).toMillis(), r.waitMs());
        // anderer Spieler ist nicht betroffen
        assertTrue(s.hack(player(Faction.LUMEN), portal).ok());
        clock.advance(Duration.ofHours(4).minusMinutes(20));
        assertTrue(s.hack(a, portal).ok());
    }

    @Test
    void verlaufUeberlebtNeustart() {
        PlayerProfile a = player(Faction.LUMEN);
        HackService s = service(0.3, List.of(new GameEvent.Hacked(a.uuid(), portal.id(), clock.millis() - 60_000)));
        assertEquals(HackService.Problem.COOLDOWN, s.hack(a, portal).problem());
    }

    @Test
    void beuteUndAp() {
        HackService s = service(1.0, List.of());
        PlayerProfile a = player(Faction.LUMEN);
        HackService.Result r = s.hack(a, portal);
        long normal = r.loot().stream().filter(g -> g.type() != GameItem.Type.KEY).count();
        assertTrue(normal >= 2 && normal <= 4);
        assertTrue(r.loot().contains(GameItem.key(portal.id(), Faction.LUMEN)));
        assertTrue(r.loot().stream().allMatch(g -> g.faction() == Faction.LUMEN));
        assertTrue(r.loot().stream().filter(g -> g.type() != GameItem.Type.KEY)
                .allMatch(g -> g.level() >= 1 && g.level() <= 2), "neutrales Portal: um Stufe 1");
        assertEquals(100, a.ap());
    }

    @Test
    void gegnerischesPortalGibtMehr() {
        PlayerProfile owner = player(Faction.WAECHTER);
        owner.setAp(1_200_000);
        for (int i = 0; i < 8; i++) {
            portals.deploy(owner, portal, i < 1 ? 8 : i < 2 ? 7 : i < 4 ? 6 : 5);
        }
        HackService s = service(0.0, List.of());
        PlayerProfile a = player(Faction.LUMEN);
        for (int i = 0; i < 4; i++) {
            HackService.Result r = s.hack(a, portal);
            assertTrue(r.loot().size() >= 3 && r.loot().size() <= 5);
            assertTrue(r.loot().stream().allMatch(g -> g.level() >= portal.level() - 1 && g.level() <= portal.level() + 1));
            assertFalse(r.loot().stream().anyMatch(g -> g.type() == GameItem.Type.KEY));
            clock.advance(Duration.ofMinutes(5));
        }
        assertEquals(4 * 150, a.ap());
    }
}
