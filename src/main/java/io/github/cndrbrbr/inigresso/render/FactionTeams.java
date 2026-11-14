package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Fraktionsfarbe im Namen über Teams auf dem Haupt-Scoreboard. Vanilla zeigt Teamfarbe und
 * Präfix in Tabliste, Namensschild und Chat.
 *
 * <p>Wer die Sidebar ausblendet (/ig scoreboard aus), bekommt ein zweites Scoreboard ohne
 * Sidebar. Darauf liegen dieselben Fraktionsteams, damit die Namensfarben erhalten bleiben.
 */
public final class FactionTeams {

    private static final String PREFIX = "ig_";

    private final Set<UUID> quiet;
    private final Consumer<Set<UUID>> saveQuiet;
    private Scoreboard quietBoard;

    /**
     * @param quiet     Spieler, die die Sidebar ausgeblendet haben (gespeicherter Stand)
     * @param saveQuiet speichert die Menge nach jeder Änderung
     */
    public FactionTeams(Set<UUID> quiet, Consumer<Set<UUID>> saveQuiet) {
        this.quiet = new HashSet<>(quiet);
        this.saveQuiet = saveQuiet;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.FactionChanged.class, e -> apply(e.profile()));
    }

    private List<Scoreboard> boards() {
        return List.of(Bukkit.getScoreboardManager().getMainScoreboard(), quietBoard);
    }

    public void setup() {
        quietBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        for (Scoreboard sb : boards()) {
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
    }

    public void apply(PlayerProfile p) {
        String entry = p.lastName();
        for (Scoreboard sb : boards()) {
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
        }
        Player online = Bukkit.getPlayer(p.uuid());
        if (online != null && p.faction() != null) {
            online.setPlayerListName(p.faction().color() + online.getName() + ChatColor.RESET);
        }
    }

    public boolean isQuiet(UUID player) {
        return quiet.contains(player);
    }

    /** Sidebar für einen Spieler aus- (quiet = true) oder einblenden. */
    public void setQuiet(Player player, boolean on) {
        boolean changed = on ? quiet.add(player.getUniqueId()) : quiet.remove(player.getUniqueId());
        if (changed) {
            saveQuiet.accept(Set.copyOf(quiet));
        }
        applyBoard(player);
    }

    /** Beim Login und nach Änderungen: passendes Scoreboard setzen. */
    public void applyBoard(Player player) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard want = quiet.contains(player.getUniqueId()) ? quietBoard : main;
        Scoreboard has = player.getScoreboard();
        // Nur zwischen unseren beiden wechseln; ein Scoreboard eines anderen Plugins bleibt.
        if (has != want && (has == main || has == quietBoard)) {
            player.setScoreboard(want);
        }
    }

    /** Beim Abschalten: alle zurück aufs Haupt-Scoreboard. */
    public void restoreAll() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() == quietBoard) {
                p.setScoreboard(main);
            }
        }
    }
}
