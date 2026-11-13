package io.github.cndrbrbr.inigresso.model;

import java.util.UUID;

/** Verbindung zwischen zwei Portalen einer Fraktion (rules.md §6). from = Ausgangsportal. */
public record Link(int id, int from, int to, Faction faction, UUID createdBy, long createdAt) {

    public boolean touches(int portalId) {
        return from == portalId || to == portalId;
    }

    public boolean connects(int a, int b) {
        return (from == a && to == b) || (from == b && to == a);
    }

    public int other(int portalId) {
        return from == portalId ? to : from;
    }
}
