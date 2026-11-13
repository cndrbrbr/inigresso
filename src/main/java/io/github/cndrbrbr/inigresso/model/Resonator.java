package io.github.cndrbrbr.inigresso.model;

import java.util.Objects;
import java.util.UUID;

/** Ein Resonator auf einem der 8 Plätze eines Portals (rules.md §2.2, §5). */
public final class Resonator {

    /** Maximale Energie je Stufe (Index = Stufe - 1). */
    private static final int[] MAX_ENERGY = {1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000};

    private final int level;
    private final UUID deployedBy;
    private int energy;

    public Resonator(int level, int energy, UUID deployedBy) {
        if (level < 1 || level > 8) {
            throw new IllegalArgumentException("Stufe 1–8: " + level);
        }
        this.level = level;
        this.energy = Math.max(0, Math.min(energy, maxEnergy(level)));
        this.deployedBy = Objects.requireNonNull(deployedBy);
    }

    public static Resonator full(int level, UUID deployedBy) {
        return new Resonator(level, maxEnergy(level), deployedBy);
    }

    public static int maxEnergy(int level) {
        return MAX_ENERGY[level - 1];
    }

    public int level() {
        return level;
    }

    public UUID deployedBy() {
        return deployedBy;
    }

    public int energy() {
        return energy;
    }

    public int maxEnergy() {
        return maxEnergy(level);
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, maxEnergy()));
    }
}
