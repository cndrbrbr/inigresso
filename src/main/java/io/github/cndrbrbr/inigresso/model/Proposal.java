package io.github.cndrbrbr.inigresso.model;

import java.util.Objects;
import java.util.UUID;

/** Portalvorschlag eines Spielers (rules.md §2.1). */
public final class Proposal {

    public enum Status { OFFEN, ANGENOMMEN, ABGELEHNT }

    private final int id;
    private final String name;
    private final BlockPos pos;
    private final UUID proposer;
    private final long createdAt;
    private Status status;
    private String decidedBy;
    private long decidedAt;
    private String reason;

    public Proposal(int id, String name, BlockPos pos, UUID proposer, long createdAt, Status status,
                    String decidedBy, long decidedAt, String reason) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.pos = Objects.requireNonNull(pos);
        this.proposer = Objects.requireNonNull(proposer);
        this.createdAt = createdAt;
        this.status = Objects.requireNonNull(status);
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
        this.reason = reason;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public BlockPos pos() {
        return pos;
    }

    public UUID proposer() {
        return proposer;
    }

    public long createdAt() {
        return createdAt;
    }

    public Status status() {
        return status;
    }

    public boolean isOpen() {
        return status == Status.OFFEN;
    }

    public String decidedBy() {
        return decidedBy;
    }

    public long decidedAt() {
        return decidedAt;
    }

    public String reason() {
        return reason;
    }

    public void decide(Status status, String by, long at, String reason) {
        if (status == Status.OFFEN) {
            throw new IllegalArgumentException("Entscheidung braucht ANGENOMMEN oder ABGELEHNT");
        }
        this.status = status;
        this.decidedBy = by;
        this.decidedAt = at;
        this.reason = reason;
    }
}
