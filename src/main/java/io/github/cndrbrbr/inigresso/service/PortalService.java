package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Resonator;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * Portale im Speicher mit Chunk-Index für den Schutz, Resonatoren setzen und Erobern
 * (rules.md §2). Angriff und Verfall kommen in Phase 5 dazu.
 */
public final class PortalService {

    private final Map<Integer, Portal> portals = new HashMap<>();
    /** Chunk-Schlüssel → Portale, deren Schutzbereich diesen Chunk berührt. */
    private final Map<Long, List<Portal>> byChunk = new HashMap<>();
    private final GameEventBus bus;
    private final Clock clock;
    private final IntSupplier protectRadius;
    private final PlayerProgressService progress;
    private int nextId;

    public enum DeployOutcome {
        DEPLOYED, UPGRADED, CAPTURED,
        NO_FACTION, ENEMY_PORTAL, LEVEL_TOO_HIGH, LIMIT_REACHED, NOTHING_TO_UPGRADE
    }

    public record DeployResult(DeployOutcome outcome, int slot) {
        public boolean ok() {
            return outcome == DeployOutcome.DEPLOYED || outcome == DeployOutcome.UPGRADED
                    || outcome == DeployOutcome.CAPTURED;
        }
    }

    public PortalService(GameEventBus bus, Clock clock, IntSupplier protectRadius, PlayerProgressService progress,
                         Collection<Portal> loaded) {
        this.bus = bus;
        this.clock = clock;
        this.protectRadius = protectRadius;
        this.progress = progress;
        for (Portal p : loaded) {
            portals.put(p.id(), p);
            nextId = Math.max(nextId, p.id());
        }
        reindex();
    }

    public Portal create(String name, BlockPos core, UUID proposer) {
        Portal p = new Portal(++nextId, name, core, Faction.NEUTRAL, null, 0, proposer, clock.millis());
        portals.put(p.id(), p);
        index(p);
        bus.publish(new GameEvent.PortalChanged(p, core, true));
        return p;
    }

    public void rename(Portal p, String name) {
        p.setName(name);
        bus.publish(new GameEvent.PortalChanged(p, p.core(), false));
    }

    public void move(Portal p, BlockPos core) {
        BlockPos old = p.core();
        p.setCore(core);
        reindex();
        bus.publish(new GameEvent.PortalChanged(p, old, false));
    }

    public void remove(Portal p) {
        if (portals.remove(p.id()) != null) {
            reindex();
            bus.publish(new GameEvent.PortalRemoved(p));
        }
    }

    /** Setzt das Portal neutral und entfernt alle Resonatoren. */
    public void neutralize(Portal p) {
        if (p.owner() == Faction.NEUTRAL && p.resonatorCount() == 0) {
            return;
        }
        p.clearResonators();
        p.setOwner(Faction.NEUTRAL, null, 0);
        bus.publish(new GameEvent.PortalChanged(p, p.core(), false));
    }

    /**
     * Resonator der Stufe level setzen (rules.md §2.3/2.4): zuerst auf einen freien Platz,
     * sonst wird der schwächste Resonator unter dieser Stufe ersetzt (Aufrüsten).
     */
    public DeployResult deploy(PlayerProfile by, Portal p, int level) {
        if (!by.hasFaction()) {
            return new DeployResult(DeployOutcome.NO_FACTION, -1);
        }
        if (p.owner() != Faction.NEUTRAL && p.owner() != by.faction()) {
            return new DeployResult(DeployOutcome.ENEMY_PORTAL, -1);
        }
        if (level > PlayerProgressService.levelFor(by.ap())) {
            return new DeployResult(DeployOutcome.LEVEL_TOO_HIGH, -1);
        }
        if (p.countBy(by.uuid(), level) >= Portal.perPlayerLimit(level)) {
            return new DeployResult(DeployOutcome.LIMIT_REACHED, -1);
        }
        int slot = -1;
        for (int i = 0; i < Portal.SLOTS; i++) {
            if (p.resonator(i) == null) {
                slot = i;
                break;
            }
        }
        boolean upgrade = false;
        if (slot < 0) {
            int lowest = level;
            for (int i = 0; i < Portal.SLOTS; i++) {
                if (p.resonator(i).level() < lowest) {
                    lowest = p.resonator(i).level();
                    slot = i;
                }
            }
            if (slot < 0) {
                return new DeployResult(DeployOutcome.NOTHING_TO_UPGRADE, -1);
            }
            upgrade = true;
        }
        boolean capture = p.owner() == Faction.NEUTRAL;
        if (capture) {
            p.setOwner(by.faction(), by.uuid(), clock.millis());
        }
        p.setResonator(slot, Resonator.full(level, by.uuid()));
        bus.publish(new GameEvent.PortalChanged(p, p.core(), false));
        if (capture) {
            bus.publish(new GameEvent.PortalCaptured(p.id(), by.uuid(), by.faction(), p.capturedAt()));
            progress.award(by, PlayerProgressService.Action.CAPTURE_PORTAL);
        }
        progress.award(by, upgrade ? PlayerProgressService.Action.UPGRADE_RESONATOR
                : PlayerProgressService.Action.DEPLOY_RESONATOR);
        DeployOutcome o = capture ? DeployOutcome.CAPTURED : upgrade ? DeployOutcome.UPGRADED : DeployOutcome.DEPLOYED;
        return new DeployResult(o, slot);
    }

    public Optional<Portal> get(int id) {
        return Optional.ofNullable(portals.get(id));
    }

    /** "#12" oder "12" = ID, sonst Name ohne Groß-/Kleinschreibung. */
    public Optional<Portal> find(String idOrName) {
        String s = idOrName.startsWith("#") ? idOrName.substring(1) : idOrName;
        try {
            return get(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return byName(idOrName);
        }
    }

    public Optional<Portal> byName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return portals.values().stream().filter(p -> p.name().toLowerCase(Locale.ROOT).equals(n)).findFirst();
    }

    public Collection<Portal> all() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public Optional<Portal> nearest(BlockPos pos, double maxDistance) {
        return portals.values().stream()
                .filter(p -> p.core().horizontalDistance(pos) <= maxDistance)
                .min(Comparator.comparingDouble(p -> p.core().horizontalDistance(pos)));
    }

    /** Liegt der Block im Schutzbereich irgendeines Portals? */
    public Optional<Portal> protecting(BlockPos pos) {
        List<Portal> candidates = byChunk.get(pos.chunkKey());
        if (candidates == null) {
            return Optional.empty();
        }
        int r = protectRadius.getAsInt();
        for (Portal p : candidates) {
            if (p.core().withinBox(pos, r)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /** Portale, deren Kern in diesem Chunk liegt (für die Anzeige beim Chunk-Laden). */
    public List<Portal> coresInChunk(String world, int cx, int cz) {
        List<Portal> out = new ArrayList<>();
        for (Portal p : portals.values()) {
            if (p.core().world().equals(world) && p.core().x() >> 4 == cx && p.core().z() >> 4 == cz) {
                out.add(p);
            }
        }
        return out;
    }

    /** Nach Änderung des Schutzradius (reload) aufrufen. */
    public void reindex() {
        byChunk.clear();
        portals.values().forEach(this::index);
    }

    private void index(Portal p) {
        int r = protectRadius.getAsInt();
        BlockPos c = p.core();
        for (int cx = (c.x() - r) >> 4; cx <= (c.x() + r) >> 4; cx++) {
            for (int cz = (c.z() - r) >> 4; cz <= (c.z() + r) >> 4; cz++) {
                byChunk.computeIfAbsent(BlockPos.chunkKey(cx, cz), k -> new ArrayList<>()).add(p);
            }
        }
    }
}
