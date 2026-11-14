package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.model.Faction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Scoreboard rechts am Bildschirm (Haupt-Scoreboard, für alle gleich): Fläche, Einfluss,
 * Portale und Eroberungen beider Fraktionen, Restzeit des Zyklus, Top-Eroberer.
 */
public final class Sidebar {

    static final String OBJECTIVE = "inigresso";
    static final int MAX_LINES = 15;

    /** Werte einer Fraktion. */
    public record Side(Faction faction, double area, long influence, double average, int portals, int captures) {
    }

    /** Ein Eintrag der Rangliste. */
    public record Top(String name, int count) {
    }

    /** Alles, was angezeigt wird. */
    public record Data(long remainingMs, Side lumen, Side waechter, List<Top> top) {
    }

    private final List<String> shown = new ArrayList<>();

    /** Zeilen von oben nach unten, ohne Bukkit (testbar). */
    public static List<String> lines(Data d) {
        List<String> out = new ArrayList<>();
        out.add(ChatColor.GRAY + "Zyklus: noch " + ChatColor.WHITE + remaining(d.remainingMs()));
        Faction leader = d.lumen().average() > d.waechter().average() ? Faction.LUMEN
                : d.waechter().average() > d.lumen().average() ? Faction.WAECHTER : null;
        for (Side s : List.of(d.lumen(), d.waechter())) {
            out.add(s.faction().color() + "" + ChatColor.BOLD + s.faction().displayName()
                    + (s.faction() == leader ? ChatColor.GOLD + " ★ führt" : ""));
            out.add(ChatColor.GRAY + " Fläche: " + ChatColor.WHITE + num(s.area()) + " m²");
            out.add(ChatColor.GRAY + " Einfluss: " + ChatColor.WHITE + num(s.influence())
                    + ChatColor.GRAY + " (Ø " + num(s.average()) + ")");
            out.add(ChatColor.GRAY + " Portale: " + ChatColor.WHITE + s.portals()
                    + ChatColor.GRAY + "  Erobert: " + ChatColor.WHITE + s.captures());
            out.add(""); // 1 + 2 × 5 + Kopf + 3 Plätze = 15 Zeilen
        }
        out.add(ChatColor.GOLD + "Top-Eroberer");
        if (d.top().isEmpty()) {
            out.add(ChatColor.DARK_GRAY + " noch keine");
        }
        int i = 1;
        for (Top t : d.top()) {
            out.add(ChatColor.GRAY + " " + i++ + ". " + ChatColor.WHITE + t.name() + ChatColor.GRAY + " " + t.count());
        }
        return out.size() > MAX_LINES ? out.subList(0, MAX_LINES) : out;
    }

    static String num(double v) {
        return String.format(Locale.GERMANY, "%,.0f", v);
    }

    static String remaining(long ms) {
        long min = Math.max(0, ms / 60_000);
        long d = min / 1440;
        long h = (min % 1440) / 60;
        long m = min % 60;
        return d > 0 ? d + " T " + h + " h" : h > 0 ? h + " h " + m + " min" : m + " min";
    }

    /**
     * Zeilen zu eindeutigen Einträgen machen: ein unsichtbarer Farbcode je Zeile am Ende,
     * damit gleiche Texte (z. B. Leerzeilen) nicht zusammenfallen.
     */
    static String entry(String line, int index) {
        return line + ChatColor.values()[index] + ChatColor.RESET;
    }

    /** Alle 20 Ticks: nur geänderte Zeilen neu setzen, damit nichts flackert. */
    public void show(Data d) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective o = sb.getObjective(OBJECTIVE);
        if (o == null) {
            o = sb.registerNewObjective(OBJECTIVE, org.bukkit.scoreboard.Criteria.DUMMY,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Inigresso");
            shown.clear();
        }
        if (o.getDisplaySlot() != DisplaySlot.SIDEBAR) {
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        List<String> lines = lines(d);
        for (int i = 0; i < Math.max(lines.size(), shown.size()); i++) {
            String now = i < lines.size() ? entry(lines.get(i), i) : null;
            String before = i < shown.size() ? shown.get(i) : null;
            if (now != null && now.equals(before)) {
                continue;
            }
            if (before != null) {
                sb.resetScores(before);
            }
            if (now != null) {
                o.getScore(now).setScore(lines.size() - i);
            }
        }
        shown.clear();
        for (int i = 0; i < lines.size(); i++) {
            shown.add(entry(lines.get(i), i));
        }
    }

    /** Beim Abschalten entfernen, damit keine Reste im scoreboard.dat bleiben. */
    public void remove() {
        Objective o = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(OBJECTIVE);
        if (o != null) {
            o.unregister();
        }
        shown.clear();
    }
}
