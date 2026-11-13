package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class PortalServiceTest {

    private final List<GameEvent> events = new ArrayList<>();

    private PortalService service(int radius, List<Portal> loaded) {
        GameEventBus bus = new GameEventBus(Logger.getLogger("test"));
        bus.subscribe(GameEvent.class, events::add);
        return new PortalService(bus, Clock.systemUTC(), () -> radius, loaded);
    }

    private static BlockPos at(int x, int y, int z) {
        return new BlockPos("world", x, y, z);
    }

    @Test
    void schutzbereichAuchUeberChunkgrenzen() {
        PortalService s = service(4, List.of());
        Portal p = s.create("Rand", at(15, 64, 15), null); // Ecke eines Chunks
        assertTrue(s.protecting(at(19, 64, 19)).isPresent(), "Nachbarchunk");
        assertTrue(s.protecting(at(11, 60, 11)).isPresent());
        assertTrue(s.protecting(at(15, 70, 15)).isPresent(), "über dem Kern (r + 2)");
        assertFalse(s.protecting(at(20, 64, 15)).isPresent());
        assertFalse(s.protecting(at(15, 71, 15)).isPresent());
        assertFalse(s.protecting(new BlockPos("world_nether", 15, 64, 15)).isPresent());
        assertFalse(s.protecting(at(-15, 64, -15)).isPresent());
        s.remove(p);
        assertFalse(s.protecting(at(15, 64, 15)).isPresent());
    }

    @Test
    void negativeKoordinaten() {
        PortalService s = service(4, List.of());
        s.create("Minus", at(-1, 64, -1), null);
        assertTrue(s.protecting(at(2, 64, 2)).isPresent());
        assertTrue(s.protecting(at(-5, 64, -5)).isPresent());
    }

    @Test
    void verschiebenAktualisiertIndexUndMeldetAltePosition() {
        PortalService s = service(4, List.of());
        Portal p = s.create("Turm", at(0, 64, 0), null);
        events.clear();
        s.move(p, at(100, 70, 100));
        assertFalse(s.protecting(at(0, 64, 0)).isPresent());
        assertTrue(s.protecting(at(100, 70, 100)).isPresent());
        GameEvent.PortalChanged e = (GameEvent.PortalChanged) events.get(0);
        assertTrue(e.moved());
        assertEquals(at(0, 64, 0), e.previousCore());
    }

    @Test
    void suchenUndIdsNachDemLaden() {
        Portal alt = new Portal(7, "Alt", at(0, 64, 0), io.github.cndrbrbr.inigresso.model.Faction.NEUTRAL,
                null, 0, null, 0);
        PortalService s = service(4, List.of(alt));
        Portal neu = s.create("Neu", at(200, 64, 0), null);
        assertEquals(8, neu.id());
        assertEquals(alt, s.find("#7").orElseThrow());
        assertEquals(alt, s.find("7").orElseThrow());
        assertEquals(neu, s.find("nEu").orElseThrow());
        assertEquals(alt, s.nearest(at(10, 64, 10), 16).orElseThrow());
        assertTrue(s.nearest(at(50, 64, 50), 16).isEmpty());
        assertEquals(List.of(alt), s.coresInChunk("world", 0, 0));
    }
}
