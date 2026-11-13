package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FactionServiceTest {

    private GameEventBus bus;
    private ProfileService profiles;
    private FactionService factions;
    private final List<GameEvent> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bus = new GameEventBus(Logger.getLogger("test"));
        bus.subscribe(GameEvent.class, events::add);
        profiles = new ProfileService(bus, List.of());
        factions = new FactionService(profiles, bus);
    }

    private PlayerProfile join(String name) {
        return profiles.join(UUID.randomUUID(), name);
    }

    @Test
    void wahlIstEinmalig() {
        PlayerProfile p = join("anna");
        events.clear();
        assertEquals(FactionService.ChooseResult.OK, factions.choose(p, Faction.LUMEN));
        assertEquals(Faction.LUMEN, p.faction());
        assertEquals(FactionService.ChooseResult.ALREADY_CHOSEN, factions.choose(p, Faction.WAECHTER));
        assertEquals(Faction.LUMEN, p.faction());
        GameEvent.FactionChanged fc = events.stream().filter(GameEvent.FactionChanged.class::isInstance)
                .map(GameEvent.FactionChanged.class::cast).findFirst().orElseThrow();
        assertNull(fc.previous());
    }

    @Test
    void adminWechselMeldetVorherigeFraktion() {
        PlayerProfile p = join("ben");
        factions.choose(p, Faction.LUMEN);
        events.clear();
        assertTrue(factions.adminSet(p, Faction.WAECHTER));
        assertEquals(Faction.WAECHTER, p.faction());
        assertTrue(events.contains(new GameEvent.FactionChanged(p, Faction.LUMEN)));
        assertFalse(factions.adminSet(p, Faction.WAECHTER), "kein Wechsel in dieselbe Fraktion");
    }

    @Test
    void neutralIstNichtWaehlbar() {
        PlayerProfile p = join("carl");
        assertThrows(IllegalArgumentException.class, () -> factions.choose(p, Faction.NEUTRAL));
        assertTrue(Faction.parsePlayable("neutral").isEmpty());
        assertEquals(Faction.WAECHTER, Faction.parsePlayable("Wächter").orElseThrow());
    }

    @Test
    void hinweisBeiUngleichgewicht() {
        for (int i = 0; i < 3; i++) {
            factions.choose(join("w" + i), Faction.WAECHTER);
        }
        for (int i = 0; i < 3; i++) {
            factions.choose(join("l" + i), Faction.LUMEN);
        }
        assertFalse(factions.isOverpopulated(Faction.LUMEN));
        factions.choose(join("l3"), Faction.LUMEN); // 4 > 3 * 1.3 = 3.9
        assertTrue(factions.isOverpopulated(Faction.LUMEN));
        assertFalse(factions.isOverpopulated(Faction.WAECHTER));
    }

    @Test
    void keinHinweisBeiSehrWenigenSpielern() {
        factions.choose(join("a"), Faction.LUMEN);
        factions.choose(join("b"), Faction.LUMEN);
        assertFalse(factions.isOverpopulated(Faction.LUMEN));
    }

    @Test
    void namensaenderungWirdUebernommen() {
        UUID id = UUID.randomUUID();
        profiles.join(id, "alt");
        profiles.join(id, "Neu");
        assertEquals("Neu", profiles.get(id).orElseThrow().lastName());
        assertTrue(profiles.byName("neu").isPresent());
        assertTrue(profiles.byName("alt").isEmpty());
    }
}
