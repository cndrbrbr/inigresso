package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gezogene Links im Speicher, für die Rangliste „Top-Linker“ des laufenden Zyklus.
 * Ältere Einträge werden mit {@link #prune(long)} verworfen (bleiben in link_log).
 */
public final class LinkStats {

    /** Ein gezogener Link: wer, wann. */
    public record Drawn(UUID player, long timestamp) {
    }

    private final List<Drawn> drawn = new ArrayList<>();

    public LinkStats(GameEventBus bus, Collection<Drawn> loaded) {
        drawn.addAll(loaded);
        bus.subscribe(GameEvent.LinkCreated.class,
                e -> drawn.add(new Drawn(e.link().createdBy(), e.link().createdAt())));
    }

    /** Die limit Spieler mit den meisten gezogenen Links seit since, bei Gleichstand wer zuerst. */
    public List<CaptureStats.Entry> top(long since, int limit) {
        Map<UUID, Integer> count = new HashMap<>();
        Map<UUID, Long> last = new HashMap<>();
        for (Drawn d : drawn) {
            if (d.timestamp() >= since) {
                count.merge(d.player(), 1, Integer::sum);
                last.merge(d.player(), d.timestamp(), Math::max);
            }
        }
        return count.entrySet().stream()
                .sorted(Comparator.<Map.Entry<UUID, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparingLong(e -> last.get(e.getKey())))
                .limit(limit)
                .map(e -> new CaptureStats.Entry(e.getKey(), e.getValue()))
                .toList();
    }

    public void prune(long before) {
        drawn.removeIf(d -> d.timestamp() < before);
    }
}
