package io.github.cndrbrbr.inigresso.event;

import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;

/** Änderungen am Spielzustand, auf die Speicherung und Darstellung reagieren. */
public sealed interface GameEvent {

    /** Profil neu angelegt, Name/AP/Fraktion geändert – muss gespeichert werden. */
    record ProfileChanged(PlayerProfile profile) implements GameEvent {
    }

    /** Fraktion eines Spielers gesetzt oder gewechselt (previous = null bei Erstwahl). */
    record FactionChanged(PlayerProfile profile, Faction previous) implements GameEvent {
    }

    /** Spieler hat eine neue Stufe erreicht. */
    record LevelUp(PlayerProfile profile, int newLevel) implements GameEvent {
    }
}
