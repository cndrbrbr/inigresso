package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.model.Faction;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.ToLongFunction;

/** Messpunkte und Zyklen (rules.md §8). */
public final class ScoreService {

    public record Rules(DayOfWeek startDay, int lengthHours, int measureMinutes) {
        long lengthMs() {
            return lengthHours * 3_600_000L;
        }

        long intervalMs() {
            return measureMinutes * 60_000L;
        }
    }

    public record Measurement(long ts, long lumen, long waechter) {
    }

    /** winner null = unentschieden. */
    public record CycleResult(long start, long end, double lumenAvg, double waechterAvg, int measurements,
                              Faction winner) {
    }

    /** Wohin Ergebnisse gehen (Speichern, Ankündigen). */
    public interface Sink {
        void measured(Measurement m);

        void cycleClosed(CycleResult r);

        void currentCycle(long start);
    }

    private final Clock clock;
    private final java.util.function.Supplier<Rules> rules;
    private final ToLongFunction<Faction> influence;
    private final Sink sink;
    private final List<Measurement> current = new ArrayList<>();
    private long cycleStart;
    private long lastSlot;

    /**
     * @param storedCycleStart gespeicherter Beginn des laufenden Zyklus, 0 = erster Start
     * @param measurements     Messpunkte ab storedCycleStart
     */
    public ScoreService(Clock clock, java.util.function.Supplier<Rules> rules, ToLongFunction<Faction> influence,
                        Sink sink, long storedCycleStart, Collection<Measurement> measurements) {
        this.clock = clock;
        this.rules = rules;
        this.influence = influence;
        this.sink = sink;
        this.cycleStart = storedCycleStart > 0 ? storedCycleStart : cycleStartAt(clock.millis());
        if (storedCycleStart <= 0) {
            sink.currentCycle(cycleStart);
        }
        measurements.stream().sorted((a, b) -> Long.compare(a.ts(), b.ts())).forEach(m -> {
            current.add(m);
            lastSlot = Math.max(lastSlot, m.ts());
        });
    }

    /** Beginn des Zyklus, in dem t liegt: Zyklen beginnen am Starttag 00:00 und dauern lengthHours. */
    public long cycleStartAt(long t) {
        Rules r = rules.get();
        ZoneId zone = clock.getZone();
        long anchor = LocalDate.of(2026, 1, 1).with(TemporalAdjusters.nextOrSame(r.startDay()))
                .atStartOfDay(zone).toInstant().toEpochMilli();
        long len = r.lengthMs();
        return anchor + Math.floorDiv(t - anchor, len) * len;
    }

    /** Regelmäßig aufrufen (z. B. jede Minute): Zyklus abschließen, Messpunkt nehmen. */
    public List<CycleResult> tick() {
        Rules r = rules.get();
        long now = clock.millis();
        List<CycleResult> closed = new ArrayList<>();
        long nowCycle = cycleStartAt(now);
        while (cycleStart < nowCycle) {
            // Endet an der nächsten regulären Grenze; so findet ein verschobener Beginn
            // (z. B. nach Änderung des Starttags) wieder in den Takt.
            long end = cycleStartAt(cycleStart) + r.lengthMs();
            List<Measurement> in = current.stream().filter(m -> m.ts() >= cycleStart && m.ts() < end).toList();
            CycleResult res = result(cycleStart, end, in);
            closed.add(res);
            sink.cycleClosed(res);
            current.removeIf(m -> m.ts() < end);
            cycleStart = end;
            sink.currentCycle(cycleStart);
        }
        long slot = Math.floorDiv(now, r.intervalMs()) * r.intervalMs();
        if (slot > lastSlot) {
            lastSlot = slot;
            Measurement m = new Measurement(slot, influence.applyAsLong(Faction.LUMEN),
                    influence.applyAsLong(Faction.WAECHTER));
            current.add(m);
            sink.measured(m);
        }
        return closed;
    }

    static CycleResult result(long start, long end, List<Measurement> in) {
        double l = in.stream().mapToLong(Measurement::lumen).average().orElse(0);
        double w = in.stream().mapToLong(Measurement::waechter).average().orElse(0);
        Faction winner = l > w ? Faction.LUMEN : w > l ? Faction.WAECHTER : null;
        return new CycleResult(start, end, l, w, in.size(), winner);
    }

    /** Zwischenstand des laufenden Zyklus. */
    public CycleResult standing() {
        return result(cycleStart, cycleEnd(), current);
    }

    public long cycleStart() {
        return cycleStart;
    }

    public long cycleEnd() {
        return cycleStartAt(cycleStart) + rules.get().lengthMs();
    }

    public long influenceNow(Faction f) {
        return influence.applyAsLong(f);
    }
}
