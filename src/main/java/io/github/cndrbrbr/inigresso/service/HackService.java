package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.GameItem;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/** Hacken nach rules.md §4. */
public final class HackService {

    public static final long WINDOW_MS = 4 * 60 * 60 * 1000L;

    public record Rules(int cooldownMinutes, int maxPerWindow, double keyChance) {
    }

    public enum Problem { NO_FACTION, COOLDOWN, BURNED_OUT }

    /** Entweder loot (ok) oder problem mit Wartezeit in ms. */
    public record Result(List<GameItem> loot, Problem problem, long waitMs) {
        public boolean ok() {
            return problem == null;
        }
    }

    private record Key(UUID player, int portalId) {
    }

    private final Map<Key, Deque<Long>> log = new HashMap<>();
    private final GameEventBus bus;
    private final Clock clock;
    private final RandomGenerator random;
    private final Supplier<Rules> rules;
    private final PlayerProgressService progress;

    public HackService(GameEventBus bus, Clock clock, RandomGenerator random, Supplier<Rules> rules,
                       PlayerProgressService progress, Collection<GameEvent.Hacked> history) {
        this.bus = bus;
        this.clock = clock;
        this.random = random;
        this.rules = rules;
        this.progress = progress;
        history.stream().sorted((a, b) -> Long.compare(a.timestamp(), b.timestamp()))
                .forEach(h -> log.computeIfAbsent(new Key(h.player(), h.portalId()), k -> new ArrayDeque<>())
                        .addLast(h.timestamp()));
    }

    public Result hack(PlayerProfile by, Portal p) {
        if (!by.hasFaction()) {
            return new Result(List.of(), Problem.NO_FACTION, 0);
        }
        Rules r = rules.get();
        long now = clock.millis();
        Deque<Long> times = log.computeIfAbsent(new Key(by.uuid(), p.id()), k -> new ArrayDeque<>());
        while (!times.isEmpty() && times.peekFirst() <= now - WINDOW_MS) {
            times.pollFirst();
        }
        long cooldown = r.cooldownMinutes() * 60_000L;
        if (!times.isEmpty() && now - times.peekLast() < cooldown) {
            return new Result(List.of(), Problem.COOLDOWN, cooldown - (now - times.peekLast()));
        }
        if (times.size() >= r.maxPerWindow()) {
            return new Result(List.of(), Problem.BURNED_OUT, times.peekFirst() + WINDOW_MS - now);
        }
        times.addLast(now);
        bus.publish(new GameEvent.Hacked(by.uuid(), p.id(), now));

        boolean enemy = p.owner() != Faction.NEUTRAL && p.owner() != by.faction();
        List<GameItem> loot = new ArrayList<>();
        int count = (enemy ? 3 : 2) + random.nextInt(3);
        int base = Math.max(1, p.level());
        for (int i = 0; i < count; i++) {
            int level = Math.clamp(base + random.nextInt(-1, 2), 1, 8);
            loot.add(random.nextBoolean() ? GameItem.resonator(level, by.faction()) : GameItem.burst(level, by.faction()));
        }
        if (random.nextDouble() < r.keyChance()) {
            loot.add(GameItem.key(p.id(), by.faction()));
        }
        progress.award(by, enemy ? PlayerProgressService.Action.HACK_ENEMY : PlayerProgressService.Action.HACK_OWN);
        return new Result(loot, null, 0);
    }
}
