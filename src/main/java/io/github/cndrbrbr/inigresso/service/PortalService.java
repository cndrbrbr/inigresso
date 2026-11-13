package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
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
 * Portale im Speicher mit Chunk-Index für den Schutz (rules.md §2). Resonatoren, Erobern
 * und Verfall kommen in Phase 4/5 dazu.
 */
public final class PortalService {

    private final Map<Integer, Portal> portals = new HashMap<>();
    /** Chunk-Schlüssel → Portale, deren Schutzbereich diesen Chunk berührt. */
    private final Map<Long, List<Portal>> byChunk = new HashMap<>();
    private final GameEventBus bus;
    private final Clock clock;
    private final IntSupplier protectRadius;
    private int nextId;

    public PortalService(GameEventBus bus, Clock clock, IntSupplier protectRadius, Collection<Portal> loaded) {
        this.bus = bus;
        this.clock = clock;
        this.protectRadius = protectRadius;
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

    /** Setzt das Portal neutral. Ab Phase 4 werden dabei auch die Resonatoren entfernt. */
    public void neutralize(Portal p) {
        if (p.owner() == Faction.NEUTRAL) {
            return;
        }
        p.setOwner(Faction.NEUTRAL, null, 0);
        bus.publish(new GameEvent.PortalChanged(p, p.core(), false));
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
