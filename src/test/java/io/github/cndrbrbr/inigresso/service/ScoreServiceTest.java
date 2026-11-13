package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.model.Faction;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScoreServiceTest {

    private final HackServiceTest.TestClock clock = new HackServiceTest.TestClock();
    private final Map<Faction, Long> influence = new EnumMap<>(Faction.class);
    private final List<ScoreService.Measurement> measured = new ArrayList<>();
    private final List<ScoreService.CycleResult> closed = new ArrayList<>();
    private final List<Long> starts = new ArrayList<>();

    private final ScoreService.Sink sink = new ScoreService.Sink() {
        @Override
        public void measured(ScoreService.Measurement m) {
            measured.add(m);
        }

        @Override
        public void cycleClosed(ScoreService.CycleResult r) {
            closed.add(r);
        }

        @Override
        public void currentCycle(long start) {
            starts.add(start);
        }
    };

    @BeforeEach
    void setUp() {
        clock.now = Instant.parse("2026-11-13T12:30:00Z"); // Freitag
        influence.put(Faction.LUMEN, 0L);
        influence.put(Faction.WAECHTER, 0L);
    }

    private ScoreService service(long stored, List<ScoreService.Measurement> ms) {
        return new ScoreService(clock, () -> new ScoreService.Rules(DayOfWeek.MONDAY, 168, 60),
                f -> influence.get(f), sink, stored, ms);
    }

    private static long ms(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }

    @Test
    void zyklusBeginntMontags() {
        ScoreService s = service(0, List.of());
        assertEquals(ms("2026-11-09T00:00:00Z"), s.cycleStart());
        assertEquals(ms("2026-11-16T00:00:00Z"), s.cycleEnd());
        assertEquals(List.of(ms("2026-11-09T00:00:00Z")), starts);
    }

    @Test
    void stuendlicheMesspunkteOhneDoppelte() {
        ScoreService s = service(0, List.of());
        influence.put(Faction.LUMEN, 50L);
        s.tick();
        s.tick();
        assertEquals(1, measured.size());
        assertEquals(new ScoreService.Measurement(ms("2026-11-13T12:00:00Z"), 50, 0), measured.get(0));
        clock.advance(Duration.ofMinutes(30));
        s.tick();
        assertEquals(2, measured.size());
        assertEquals(ms("2026-11-13T13:00:00Z"), measured.get(1).ts());
        clock.advance(Duration.ofHours(5)); // Ausfall: keine erfundenen Messpunkte
        s.tick();
        assertEquals(3, measured.size());
    }

    @Test
    void zyklusAbschlussMitDurchschnittUndGewinner() {
        ScoreService s = service(0, List.of());
        influence.put(Faction.LUMEN, 100L);
        s.tick();
        influence.put(Faction.LUMEN, 0L);
        influence.put(Faction.WAECHTER, 60L);
        clock.advance(Duration.ofHours(1));
        s.tick();
        clock.advance(Duration.ofHours(1));
        s.tick();
        // Lumen (100+0+0)/3 = 33.3, Wächter (0+60+60)/3 = 40
        clock.now = Instant.parse("2026-11-16T00:00:30Z");
        List<ScoreService.CycleResult> r = s.tick();
        assertEquals(1, r.size());
        assertEquals(Faction.WAECHTER, r.get(0).winner());
        assertEquals(3, r.get(0).measurements());
        assertEquals(40.0, r.get(0).waechterAvg(), 1e-9);
        assertEquals(ms("2026-11-16T00:00:00Z"), s.cycleStart());
        // der Messpunkt um 00:00 gehört schon zum neuen Zyklus
        assertEquals(1, s.standing().measurements());
    }

    @Test
    void verpassteZyklenWerdenNachgeholt() {
        ScoreService s = service(ms("2026-10-26T00:00:00Z"),
                List.of(new ScoreService.Measurement(ms("2026-10-27T10:00:00Z"), 10, 5)));
        List<ScoreService.CycleResult> r = s.tick();
        assertEquals(2, r.size(), "26.10. und 02.11.");
        assertEquals(Faction.LUMEN, r.get(0).winner());
        assertNull(r.get(1).winner(), "ohne Messpunkte unentschieden");
        assertEquals(ms("2026-11-09T00:00:00Z"), s.cycleStart());
    }

    @Test
    void verschobenerBeginnFindetZurueckZuMontag() {
        // gespeicherter Beginn an einem Donnerstag (z. B. Starttag wurde geändert)
        ScoreService s = service(ms("2026-10-29T00:00:00Z"), List.of());
        List<ScoreService.CycleResult> r = s.tick();
        assertEquals(ms("2026-11-02T00:00:00Z"), r.get(0).end(), "verkürzter erster Zyklus endet Montag");
        assertEquals(ms("2026-11-09T00:00:00Z"), s.cycleStart());
        assertEquals(2, r.size());
    }

    @Test
    void kurzerTestzyklus() {
        ScoreService s = new ScoreService(clock, () -> new ScoreService.Rules(DayOfWeek.MONDAY, 1, 5),
                f -> influence.get(f), sink, 0, List.of());
        assertEquals(ms("2026-11-13T12:00:00Z"), s.cycleStart());
        influence.put(Faction.LUMEN, 7L);
        s.tick();
        clock.advance(Duration.ofMinutes(30));
        assertTrue(s.tick().size() == 1);
        assertEquals(Faction.LUMEN, closed.get(0).winner());
    }
}
