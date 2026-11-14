package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.service.PlayerProgressService;
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
 * Fraktionsfarbe und Stufe im Namen über ein Team je Spieler auf dem Haupt-Scoreboard
 * (Namensschild: „[Lumen] Name · L3“). Ein Team je Spieler, weil die Stufe als Suffix
 * pro Spieler verschieden ist. Die Tabliste setzen wir zusätzlich direkt: „Name L3“.
 *
 * <p>Wer die Sidebar ausblendet (/ig scoreboard aus), bekommt ein zweites Scoreboard ohne
 * Sidebar. Darauf liegen dieselben Fraktionsteams, damit die Namensfarben erhalten bleiben.
 */
public final class FactionTeams {

    /** Alte Teams je Fraktion (bis 0.11.0), werden beim Start entfernt. */
    private static final String OLD_PREFIX = "ig_";
    /** Team je Spieler: igp_ + 16 Hex-Zeichen der UUID. */
    private static final String PREFIX = "igp_";

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
        bus.subscribe(GameEvent.ProfileChanged.class, e -> apply(e.profile()));
    }

    private List<Scoreboard> boards() {
        return List.of(Bukkit.getScoreboardManager().getMainScoreboard(), quietBoard);
    }

    public void setup() {
        quietBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Faction f : Faction.playable()) {
            Team old = main.getTeam(OLD_PREFIX + f.key());
            if (old != null) {
                old.unregister();
            }
        }
    }

    static String teamName(UUID id) {
        return PREFIX + id.toString().replace("-", "").substring(0, 16);
    }

    /** Text hinter dem Namen im Namensschild. */
    public static String suffix(int level) {
        return ChatColor.GRAY + " · L" + level;
    }

    public void apply(PlayerProfile p) {
        String entry = p.lastName();
        String name = teamName(p.uuid());
        int level = PlayerProgressService.levelFor(p.ap());
        for (Scoreboard sb : boards()) {
            Team t = sb.getTeam(name);
            if (!p.hasFaction()) {
                if (t != null) {
                    t.unregister();
                }
                continue;
            }
            if (t == null) {
                t = sb.registerNewTeam(name);
                t.setAllowFriendlyFire(true);
            }
            Faction f = p.faction();
            if (t.getColor() != f.color()) {
                t.setColor(f.color());
            }
            String prefix = f.color() + "[" + f.displayName() + "] ";
            if (!prefix.equals(t.getPrefix())) {
                t.setPrefix(prefix);
            }
            if (!suffix(level).equals(t.getSuffix())) {
                t.setSuffix(suffix(level));
            }
            if (!t.hasEntry(entry)) {
                t.addEntry(entry); // nimmt den Eintrag aus einem anderen Team heraus
            }
        }
        Player online = Bukkit.getPlayer(p.uuid());
        if (online != null && p.hasFaction()) {
            String list = p.faction().color() + online.getName() + ChatColor.GRAY + " L" + level + ChatColor.RESET;
            if (!list.equals(online.getPlayerListName())) {
                online.setPlayerListName(list);
            }
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
