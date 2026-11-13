package io.github.cndrbrbr.inigresso.event;

import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Proposal;
import java.util.UUID;

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

    /** Vorschlag angelegt oder entschieden – muss gespeichert werden. */
    record ProposalChanged(Proposal proposal) implements GameEvent {
    }

    /**
     * Portal angelegt (created), verschoben (previousCore ≠ portal.core()), umbenannt oder
     * Besitzer geändert. previousCore ist die Kernposition vor der Änderung.
     */
    record PortalChanged(Portal portal, BlockPos previousCore, boolean created) implements GameEvent {

        public boolean moved() {
            return !created && !previousCore.equals(portal.core());
        }
    }

    /** Portal gelöscht. */
    record PortalRemoved(Portal portal) implements GameEvent {
    }

    /** Nachricht an einen Spieler; wird beim nächsten Login zugestellt, falls er offline ist. */
    record Notify(UUID player, String text) implements GameEvent {
    }
}
