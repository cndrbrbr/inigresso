package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.geo.Vec2;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Field;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.LinkService.Problem;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkServiceTest {

    private GameEventBus bus;
    private PortalService portals;
    private LinkService links;
    private CombatService combat;
    private PlayerProfile lumen;
    private PlayerProfile waechter;
    private final List<GameEvent> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bus = new GameEventBus(Logger.getLogger("test"));
        PlayerProgressService progress = new PlayerProgressService(bus);
        portals = new PortalService(bus, Clock.systemUTC(), () -> 4, progress, List.of());
        links = new LinkService(portals, progress, bus, Clock.systemUTC(), () -> new LinkService.Rules(2000, 8),
                List.of(), List.of());
        combat = new CombatService(portals, progress, bus, () -> 3);
        bus.subscribe(GameEvent.class, events::add);
        lumen = player(Faction.LUMEN);
        waechter = player(Faction.WAECHTER);
    }

    private static PlayerProfile player(Faction f) {
        PlayerProfile p = new PlayerProfile(UUID.randomUUID(), "p", null, 1_200_000);
        p.setFaction(f);
        return p;
    }

    /** Portal mit 8 Resonatoren der Stufe level (verschiedene Spieler wegen der Limits). */
    private Portal portal(String name, int x, int z, Faction f, int level) {
        Portal p = portals.create(name, new BlockPos("world", x, 64, z), null);
        for (int i = 0; i < 8; i++) {
            portals.deploy(player(f), p, level);
        }
        return p;
    }

    private LinkService.Result link(PlayerProfile by, Portal from, Portal to) {
        return links.link(by, from.core(), from, to);
    }

    @Test
    void reichweite() {
        Portal a = portal("A", 0, 0, Faction.LUMEN, 1);
        Portal b = portal("B", 64, 0, Faction.LUMEN, 1);
        Portal c = portal("C", 0, 65, Faction.LUMEN, 1);
        assertEquals(64, links.range(a));
        assertTrue(link(lumen, a, b).ok());
        assertEquals(Problem.TOO_FAR, link(lumen, a, c).problem());
        assertEquals("65/64", link(lumen, a, c).detail());
        Portal far = portal("Weit", 3000, 0, Faction.LUMEN, 8);
        assertEquals(2000, links.range(far), "gekappt");
    }

    @Test
    void bedingungen() {
        Portal a = portal("A", 0, 0, Faction.LUMEN, 1);
        Portal b = portal("B", 50, 0, Faction.LUMEN, 1);
        Portal g = portal("G", 0, 50, Faction.WAECHTER, 1);
        Portal halb = portals.create("Halb", new BlockPos("world", -50, 64, 0), null);
        portals.deploy(lumen, halb, 1);
        assertEquals(Problem.NO_FACTION,
                links.link(new PlayerProfile(UUID.randomUUID(), "x", null, 0), a.core(), a, b).problem());
        assertEquals(Problem.NOT_AT_PORTAL, links.link(lumen, new BlockPos("world", 10, 64, 0), a, b).problem());
        assertEquals(Problem.SOURCE_NOT_OWN, link(lumen, g, a).problem());
        assertEquals(Problem.SOURCE_NOT_FULL, link(lumen, halb, a).problem());
        assertEquals(Problem.TARGET_NOT_OWN, link(lumen, a, g).problem());
        assertEquals(Problem.SAME_PORTAL, link(lumen, a, a).problem());
        assertTrue(link(lumen, a, b).ok());
        assertEquals(Problem.ALREADY_LINKED, link(lumen, b, a).problem());
    }

    @Test
    void kreuzungVerboten() {
        Portal a = portal("A", 0, 0, Faction.LUMEN, 2);
        Portal b = portal("B", 50, 50, Faction.LUMEN, 2);
        Portal c = portal("C", 0, 50, Faction.WAECHTER, 2);
        Portal d = portal("D", 50, 0, Faction.WAECHTER, 2);
        assertTrue(link(lumen, a, b).ok());
        LinkService.Result r = link(waechter, c, d);
        assertEquals(Problem.CROSSES, r.problem());
        assertEquals("A – B", r.detail());
    }

    @Test
    void portalAufDerStrecke() {
        Portal a = portal("A", 0, 0, Faction.LUMEN, 2);
        portals.create("Mitte", new BlockPos("world", 50, 64, 0), null);
        Portal c = portal("C", 100, 0, Faction.LUMEN, 2);
        LinkService.Result r = link(lumen, a, c);
        assertEquals(Problem.CROSSES, r.problem());
        assertEquals("Mitte", r.detail());
    }

    @Test
    void feldEntstehtUndZerfaellt() {
        Portal a = portal("A", 0, 0, Faction.LUMEN, 2);
        Portal b = portal("B", 60, 0, Faction.LUMEN, 2);
        Portal c = portal("C", 0, 60, Faction.LUMEN, 2);
        assertTrue(link(lumen, a, b).ok());
        assertTrue(link(lumen, b, c).fields().isEmpty());
        LinkService.Result r = link(lumen, c, a);
        assertEquals(1, r.fields().size());
        Field f = r.fields().get(0);
        assertEquals(18, f.influence()); // 1800 Blöcke² / 100
        assertEquals(18, links.influence(Faction.LUMEN));
        assertEquals(List.of(f), links.fieldsAt("world", new Vec2(10, 10)));
        assertTrue(links.fieldsAt("world", new Vec2(50, 50)).isEmpty());

        // Burst auf C bis neutral: Links und Feld weg
        PlayerProfile w = player(Faction.WAECHTER);
        for (int i = 0; i < 20 && c.owner() != Faction.NEUTRAL; i++) {
            combat.burst(w, 8, c.core());
        }
        assertEquals(Faction.NEUTRAL, c.owner());
        assertEquals(1, links.links().size(), "nur A–B bleibt");
        assertTrue(links.fields().isEmpty());
        assertEquals(0, links.influence(Faction.LUMEN));
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.FieldRemoved));
    }

    @Test
    void zweiFelderAufBeidenSeitenUndNurDasGroesste() {
        Portal a = portal("A", 0, 0, Faction.LUMEN, 2);
        Portal b = portal("B", 100, 0, Faction.LUMEN, 2);
        Portal oben = portal("Oben", 50, 60, Faction.LUMEN, 2);
        Portal obenWeit = portal("ObenWeit", 50, 150, Faction.LUMEN, 2);
        Portal unten = portal("Unten", 50, -60, Faction.LUMEN, 2);
        for (Portal c : List.of(oben, obenWeit, unten)) {
            assertTrue(link(lumen, c, a).ok(), c.name());
        }
        assertTrue(link(lumen, oben, b).ok());
        assertTrue(link(lumen, obenWeit, b).ok());
        assertTrue(link(lumen, unten, b).ok());
        // Basislink A–B: oben gibt es Oben (3000 Blöcke²) und ObenWeit (7500) – nur das größte
        assertEquals(0, links.fields().size());
        LinkService.Result r = link(lumen, a, b);
        assertEquals(2, r.fields().size());
        assertEquals(List.of(75L, 30L), r.fields().stream().map(Field::influence).toList());
        assertTrue(r.fields().stream().anyMatch(f -> f.has(obenWeit.id())));
        assertTrue(r.fields().stream().noneMatch(f -> f.has(oben.id())));
    }

    @Test
    void ausgangInGegnerischemFeld() {
        Portal a = portal("A", 0, 0, Faction.WAECHTER, 2);
        Portal b = portal("B", 200, 0, Faction.WAECHTER, 2);
        Portal c = portal("C", 100, 200, Faction.WAECHTER, 2);
        assertTrue(link(waechter, a, b).ok());
        assertTrue(link(waechter, b, c).ok());
        assertEquals(1, link(waechter, c, a).fields().size());
        Portal innen = portal("Innen", 100, 60, Faction.LUMEN, 2);
        Portal aussen = portal("Aussen", 100, -60, Faction.LUMEN, 2);
        // Link von innen nach außen kreuzt ohnehin A–B; ein Link zu einem Portal im selben Feld:
        Portal innen2 = portal("Innen2", 100, 120, Faction.LUMEN, 2);
        assertEquals(Problem.SOURCE_IN_ENEMY_FIELD, link(lumen, innen, innen2).problem());
        assertEquals(Problem.CROSSES, link(lumen, aussen, innen).problem());
    }

    @Test
    void hoechstensAchtAusgehende() {
        Portal hub = portal("Hub", 0, 0, Faction.LUMEN, 8);
        for (int i = 0; i < 9; i++) {
            double a = 2 * Math.PI * i / 9;
            Portal t = portal("T" + i, (int) (500 * Math.cos(a)), (int) (500 * Math.sin(a)), Faction.LUMEN, 1);
            LinkService.Result r = link(lumen, hub, t);
            if (i < 8) {
                assertTrue(r.ok(), "Link " + i);
            } else {
                assertEquals(Problem.TOO_MANY_OUTGOING, r.problem());
            }
        }
    }
}
