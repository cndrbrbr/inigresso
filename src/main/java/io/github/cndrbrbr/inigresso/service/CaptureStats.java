package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Portaleroberungen im Speicher, für Scoreboard und Rangliste des laufenden Zyklus.
 * Ältere Einträge werden mit {@link #prune(long)} verworfen (bleiben in capture_log).
 */
public final class CaptureStats {

    public record Entry(UUID player, int count) {
    }

    private final List<GameEvent.PortalCaptured> captures = new ArrayList<>();

    public CaptureStats(GameEventBus bus, Collection<GameEvent.PortalCaptured> loaded) {
        captures.addAll(loaded);
        bus.subscribe(GameEvent.PortalCaptured.class, captures::add);
    }

    public int count(Faction f, long since) {
        int n = 0;
        for (GameEvent.PortalCaptured c : captures) {
            if (c.faction() == f && c.timestamp() >= since) {
                n++;
            }
        }
        return n;
    }

    /** Die limit Spieler mit den meisten Eroberungen seit since, bei Gleichstand wer zuerst. */
    public List<Entry> top(long since, int limit) {
        Map<UUID, Integer> count = new HashMap<>();
        Map<UUID, Long> last = new HashMap<>();
        for (GameEvent.PortalCaptured c : captures) {
            if (c.timestamp() >= since) {
                count.merge(c.player(), 1, Integer::sum);
                last.merge(c.player(), c.timestamp(), Math::max);
            }
        }
        return count.entrySet().stream()
                .sorted(Comparator.<Map.Entry<UUID, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparingLong(e -> last.get(e.getKey())))
                .limit(limit)
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .toList();
    }

    public void prune(long before) {
        captures.removeIf(c -> c.timestamp() < before);
    }
}
