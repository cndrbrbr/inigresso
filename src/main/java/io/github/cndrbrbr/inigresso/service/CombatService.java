package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Resonator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

/** Bursts gegen gegnerische Resonatoren (rules.md §5). */
public final class CombatService {

    /** Höhenunterschied, bis zu dem ein Burst noch trifft. */
    static final int MAX_DY = 6;

    public enum Problem { NO_FACTION, LEVEL_TOO_HIGH }

    public record Hit(Portal portal, int slot, int damage, boolean destroyed) {
    }

    public record Result(Problem problem, List<Hit> hits, List<Portal> neutralized) {
        public boolean ok() {
            return problem == null;
        }
    }

    private final PortalService portals;
    private final PlayerProgressService progress;
    private final GameEventBus bus;
    private final IntSupplier resonatorRadius;

    public CombatService(PortalService portals, PlayerProgressService progress, GameEventBus bus,
                         IntSupplier resonatorRadius) {
        this.portals = portals;
        this.progress = progress;
        this.bus = bus;
        this.resonatorRadius = resonatorRadius;
    }

    public static int radius(int level) {
        return 2 + 2 * level;
    }

    /** Schaden bei Abstand d: 100 × Stufe × (1 − d / Radius), mindestens 0. */
    public static int damage(int level, double distance) {
        double factor = Math.max(0, 1 - distance / radius(level));
        return (int) Math.round(100 * level * factor);
    }

    public Result burst(PlayerProfile by, int level, BlockPos at) {
        if (!by.hasFaction()) {
            return new Result(Problem.NO_FACTION, List.of(), List.of());
        }
        if (level > PlayerProgressService.levelFor(by.ap())) {
            return new Result(Problem.LEVEL_TOO_HIGH, List.of(), List.of());
        }
        int radius = radius(level);
        int rr = resonatorRadius.getAsInt();
        List<Hit> hits = new ArrayList<>();
        Set<Portal> touched = new LinkedHashSet<>();
        List<Portal> neutralized = new ArrayList<>();
        for (Portal p : List.copyOf(portals.all())) {
            if (p.owner() == Faction.NEUTRAL || p.owner() == by.faction()
                    || p.core().horizontalDistance(at) > radius + rr + 1
                    || Math.abs(p.core().y() - at.y()) > MAX_DY) {
                continue;
            }
            for (int slot = 0; slot < Portal.SLOTS; slot++) {
                Resonator r = p.resonator(slot);
                if (r == null) {
                    continue;
                }
                int[] off = Portal.slotOffset(slot, rr);
                double d = Math.hypot(p.core().x() + off[0] - at.x(), p.core().z() + off[1] - at.z());
                int dmg = damage(level, d);
                if (dmg <= 0) {
                    continue;
                }
                r.setEnergy(r.energy() - dmg);
                boolean destroyed = r.energy() == 0;
                if (destroyed) {
                    p.setResonator(slot, null);
                    progress.award(by, PlayerProgressService.Action.DESTROY_RESONATOR);
                }
                hits.add(new Hit(p, slot, dmg, destroyed));
                touched.add(p);
            }
        }
        for (Portal p : touched) {
            if (p.resonatorCount() == 0) {
                portals.neutralize(p);
                neutralized.add(p);
            } else {
                bus.publish(new GameEvent.PortalChanged(p, p.core(), false));
            }
        }
        return new Result(null, hits, neutralized);
    }
}
