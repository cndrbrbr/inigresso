package io.github.cndrbrbr.inigresso.model;

import java.util.Set;
import java.util.UUID;

/** Feld aus drei paarweise verlinkten Portalen (rules.md §7). influence nach §8. */
public record Field(int id, int a, int b, int c, Faction faction, long influence, UUID createdBy, long createdAt) {

    public boolean has(int portalId) {
        return a == portalId || b == portalId || c == portalId;
    }

    /** Liegt der Link (x, y) auf einer Kante dieses Feldes? */
    public boolean hasEdge(int x, int y) {
        return has(x) && has(y) && x != y;
    }

    public Set<Integer> portals() {
        return Set.of(a, b, c);
    }
}
