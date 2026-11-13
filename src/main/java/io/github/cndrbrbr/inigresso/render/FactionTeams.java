package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Fraktionsfarbe im Namen über Teams auf dem Haupt-Scoreboard. Vanilla zeigt Teamfarbe und
 * Präfix in Tabliste, Namensschild und Chat.
 */
public final class FactionTeams {

    private static final String PREFIX = "ig_";

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.FactionChanged.class, e -> apply(e.profile()));
    }

    public void setup() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Faction f : Faction.playable()) {
            Team t = sb.getTeam(PREFIX + f.key());
            if (t == null) {
                t = sb.registerNewTeam(PREFIX + f.key());
            }
            t.setColor(f.color());
            t.setPrefix(f.color() + "[" + f.displayName() + "] ");
            t.setAllowFriendlyFire(true);
        }
    }

    public void apply(PlayerProfile p) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        String entry = p.lastName();
        for (Faction f : Faction.playable()) {
            Team t = sb.getTeam(PREFIX + f.key());
            if (t == null) {
                continue;
            }
            if (f == p.faction()) {
                t.addEntry(entry);
            } else if (t.hasEntry(entry)) {
                t.removeEntry(entry);
            }
        }
        Player online = Bukkit.getPlayer(p.uuid());
        if (online != null && p.faction() != null) {
            online.setPlayerListName(p.faction().color() + online.getName() + ChatColor.RESET);
        }
    }
}
