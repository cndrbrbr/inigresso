package io.github.cndrbrbr.inigresso.model;

import java.util.Objects;
import java.util.UUID;

/** Ein freigegebenes Portal (rules.md §2). */
public final class Portal {

    public static final int SLOTS = 8;

    /** Wie viele Resonatoren einer Stufe ein Spieler pro Portal setzen darf (Index = Stufe - 1). */
    private static final int[] PER_PLAYER_LIMIT = {8, 8, 8, 4, 2, 2, 1, 1};

    /** Himmelsrichtungen der Plätze: N, NO, O, SO, S, SW, W, NW. */
    public static final String[] SLOT_NAMES = {"N", "NO", "O", "SO", "S", "SW", "W", "NW"};
    private static final int[][] SLOT_DIRS = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

    private final Resonator[] resonators = new Resonator[SLOTS];

    private final int id;
    private String name;
    private BlockPos core;
    private Faction owner;
    private UUID capturedBy;
    private long capturedAt;
    private final UUID proposer;
    private final long createdAt;

    public Portal(int id, String name, BlockPos core, Faction owner, UUID capturedBy, long capturedAt,
                  UUID proposer, long createdAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.core = Objects.requireNonNull(core);
        this.owner = Objects.requireNonNull(owner);
        this.capturedBy = capturedBy;
        this.capturedAt = capturedAt;
        this.proposer = proposer;
        this.createdAt = createdAt;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public BlockPos core() {
        return core;
    }

    public void setCore(BlockPos core) {
        this.core = Objects.requireNonNull(core);
    }

    public Faction owner() {
        return owner;
    }

    public UUID capturedBy() {
        return capturedBy;
    }

    public long capturedAt() {
        return capturedAt;
    }

    public void setOwner(Faction owner, UUID capturedBy, long capturedAt) {
        this.owner = Objects.requireNonNull(owner);
        this.capturedBy = capturedBy;
        this.capturedAt = capturedAt;
    }

    /** null bei Portalen, die ein Admin direkt angelegt hat. */
    public UUID proposer() {
        return proposer;
    }

    public long createdAt() {
        return createdAt;
    }

    public Resonator resonator(int slot) {
        return resonators[slot];
    }

    public void setResonator(int slot, Resonator r) {
        resonators[slot] = r;
    }

    public void clearResonators() {
        java.util.Arrays.fill(resonators, null);
    }

    public int resonatorCount() {
        int n = 0;
        for (Resonator r : resonators) {
            if (r != null) {
                n++;
            }
        }
        return n;
    }

    /** Durchschnitt der Resonatorstufen (leere Plätze = 0), abgerundet, mind. 1 wenn erobert; neutral = 0. */
    public int level() {
        int sum = 0;
        for (Resonator r : resonators) {
            if (r != null) {
                sum += r.level();
            }
        }
        if (sum == 0) {
            return 0;
        }
        return Math.max(1, sum / SLOTS);
    }

    /** Wie viele Resonatoren dieser Stufe der Spieler hier schon gesetzt hat. */
    public int countBy(UUID player, int level) {
        int n = 0;
        for (Resonator r : resonators) {
            if (r != null && r.level() == level && r.deployedBy().equals(player)) {
                n++;
            }
        }
        return n;
    }

    public static int perPlayerLimit(int level) {
        return PER_PLAYER_LIMIT[level - 1];
    }

    /** Versatz (dx, dz) eines Platzes bei gegebenem Radius; Diagonalen auf den Kreis gerundet. */
    public static int[] slotOffset(int slot, int radius) {
        int[] d = SLOT_DIRS[slot];
        int diag = (int) Math.round(radius / Math.sqrt(2));
        int f = d[0] != 0 && d[1] != 0 ? diag : radius;
        return new int[] {d[0] * f, d[1] * f};
    }
}
