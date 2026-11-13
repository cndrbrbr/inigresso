package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnergyServiceTest {

    private final HackServiceTest.TestClock clock = new HackServiceTest.TestClock();
    private GameEventBus bus;
    private PlayerProgressService progress;
    private PortalService portals;
    private Portal portal;
    private PlayerProfile owner;
    private final List<Long> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clock.now = Instant.parse("2026-11-13T12:00:00Z");
        bus = new GameEventBus(Logger.getLogger("test"));
        progress = new PlayerProgressService(bus);
        portals = new PortalService(bus, clock, () -> 4, progress, List.of());
        portal = portals.create("Turm", new BlockPos("world", 0, 64, 0), null);
        owner = new PlayerProfile(UUID.randomUUID(), "o", null, 0);
        owner.setFaction(Faction.LUMEN);
        portals.deploy(owner, portal, 1); // 1000 Energie
    }

    private EnergyService service(long lastDecay) {
        return new EnergyService(portals, progress, bus, clock,
                () -> new EnergyService.DecayRules(15, LocalTime.of(3, 0)), lastDecay, saved::add);
    }

    @Test
    void ersterStartVerfaelltNichtRueckwirkend() {
        EnergyService s = service(0);
        assertEquals(List.of(clock.millis()), saved);
        assertEquals(0, s.tick());
        assertEquals(1000, portal.resonator(0).energy());
    }

    @Test
    void verfallUm3UhrUndNachholen() {
        EnergyService s = service(clock.millis());
        clock.advance(Duration.ofHours(14).plusMinutes(59)); // 02:59 nächster Tag
        assertEquals(0, s.tick());
        clock.advance(Duration.ofMinutes(1)); // 03:00
        assertEquals(1, s.tick());
        assertEquals(850, portal.resonator(0).energy());
        assertEquals(0, s.tick(), "nicht zweimal am selben Tag");
        clock.advance(Duration.ofDays(3)); // Server war drei Tage aus
        assertEquals(3, s.tick());
        assertEquals(850 - 3 * 150, portal.resonator(0).energy());
        assertEquals(Instant.parse("2026-11-17T03:00:00Z").toEpochMilli(), s.lastDecay());
    }

    @Test
    void portalStirbtNachEinerWoche() {
        EnergyService s = service(clock.millis());
        clock.advance(Duration.ofDays(7));
        assertEquals(7, s.tick());
        assertEquals(0, portal.resonatorCount());
        assertEquals(Faction.NEUTRAL, portal.owner());
    }

    @Test
    void aufladen() {
        EnergyService s = service(clock.millis());
        portals.deploy(owner, portal, 1);
        portal.resonator(0).setEnergy(200);
        portal.resonator(1).setEnergy(900);
        assertEquals(55, EnergyService.percent(portal)); // 1100 von 2000
        long apBefore = owner.ap();
        int used = s.recharge(owner, portal, 500);
        assertEquals(500, used);
        assertEquals(700, portal.resonator(0).energy(), "schwächster zuerst");
        assertEquals(900, portal.resonator(1).energy());
        assertEquals(10, owner.ap() - apBefore);
        assertEquals(400, s.recharge(owner, portal, 5000), "nur was fehlt");
        assertEquals(0, EnergyService.deficit(portal));
        assertEquals(0, s.recharge(owner, portal, 5000));
        PlayerProfile gegner = new PlayerProfile(UUID.randomUUID(), "g", null, 0);
        gegner.setFaction(Faction.WAECHTER);
        portal.resonator(0).setEnergy(0);
        assertEquals(0, s.recharge(gegner, portal, 500));
    }
}
