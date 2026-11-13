package io.github.cndrbrbr.inigresso.item;

import org.bukkit.entity.Player;

/** XP-Punkte genau berechnen und abziehen (getTotalExperience ist nach Verzauberungen falsch). */
public final class Xp {

    private Xp() {
    }

    /** Punkte, die man für Level 0 → level braucht (Minecraft-Formel). */
    static int pointsForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** Punkte von level nach level + 1. */
    static int pointsToNext(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    public static int total(Player p) {
        return pointsForLevel(p.getLevel()) + Math.round(p.getExp() * pointsToNext(p.getLevel()));
    }

    public static void set(Player p, int total) {
        total = Math.max(0, total);
        int level = 0;
        while (pointsForLevel(level + 1) <= total) {
            level++;
        }
        p.setLevel(level);
        p.setExp((float) (total - pointsForLevel(level)) / pointsToNext(level));
        p.setTotalExperience(total);
    }
}
