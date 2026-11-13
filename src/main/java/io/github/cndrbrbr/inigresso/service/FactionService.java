package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;

/** Fraktionswahl und Admin-Wechsel nach rules.md §1. */
public final class FactionService {

    /** Ab diesem Verhältnis (größer / kleiner) gibt es einen Hinweis. */
    static final double IMBALANCE = 1.3;

    public enum ChooseResult { OK, ALREADY_CHOSEN }

    private final ProfileService profiles;
    private final GameEventBus bus;

    public FactionService(ProfileService profiles, GameEventBus bus) {
        this.profiles = profiles;
        this.bus = bus;
    }

    /** Einmalige Wahl durch den Spieler. */
    public ChooseResult choose(PlayerProfile p, Faction f) {
        requirePlayable(f);
        if (p.hasFaction()) {
            return ChooseResult.ALREADY_CHOSEN;
        }
        p.setFaction(f);
        bus.publish(new GameEvent.ProfileChanged(p));
        bus.publish(new GameEvent.FactionChanged(p, null));
        return ChooseResult.OK;
    }

    /**
     * Wechsel durch einen Admin. Spielgegenstände werden über das FactionChanged-Ereignis
     * entfernt (rules.md §1). Gibt false zurück, wenn der Spieler schon in f ist.
     */
    public boolean adminSet(PlayerProfile p, Faction f) {
        requirePlayable(f);
        Faction previous = p.faction();
        if (previous == f) {
            return false;
        }
        p.setFaction(f);
        bus.publish(new GameEvent.ProfileChanged(p));
        bus.publish(new GameEvent.FactionChanged(p, previous));
        return true;
    }

    /**
     * Hinweis, wenn f schon mehr als 30 % größer ist als die andere Fraktion (kein Zwang).
     * Bei sehr wenigen Spielern (andere Fraktion < 3) kein Hinweis, das wäre nur Rauschen.
     */
    public boolean isOverpopulated(Faction f) {
        requirePlayable(f);
        long mine = profiles.count(f);
        long other = profiles.count(f.opponent());
        return other >= 3 && mine > other * IMBALANCE;
    }

    private static void requirePlayable(Faction f) {
        if (f == null || f == Faction.NEUTRAL) {
            throw new IllegalArgumentException("Keine spielbare Fraktion: " + f);
        }
    }
}
