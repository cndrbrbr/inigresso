package io.github.cndrbrbr.inigresso.model;

import java.util.Objects;
import java.util.UUID;

/** Ein freigegebenes Portal (rules.md §2). Resonatoren folgen in Phase 4. */
public final class Portal {

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
}
