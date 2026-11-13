package io.github.cndrbrbr.inigresso.model;

import java.util.Objects;
import java.util.UUID;

/** Dauerhafte Spielerdaten. Wird nur auf dem Server-Thread verändert. */
public final class PlayerProfile {

    private final UUID uuid;
    private String lastName;
    private Faction faction;
    private long ap;

    public PlayerProfile(UUID uuid, String lastName, Faction faction, long ap) {
        this.uuid = Objects.requireNonNull(uuid);
        this.lastName = Objects.requireNonNull(lastName);
        this.faction = faction;
        this.ap = ap;
    }

    public UUID uuid() {
        return uuid;
    }

    public String lastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = Objects.requireNonNull(lastName);
    }

    /** null = noch keine Fraktion gewählt. */
    public Faction faction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        if (faction == Faction.NEUTRAL) {
            throw new IllegalArgumentException("Spieler können nicht neutral sein");
        }
        this.faction = faction;
    }

    public boolean hasFaction() {
        return faction != null;
    }

    public long ap() {
        return ap;
    }

    public void setAp(long ap) {
        this.ap = Math.max(0, ap);
    }
}
