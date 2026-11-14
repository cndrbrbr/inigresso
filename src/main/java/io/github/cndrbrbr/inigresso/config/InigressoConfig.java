package io.github.cndrbrbr.inigresso.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/** Typisierte Sicht auf config.yml. Ungültige Werte fallen auf Standardwerte zurück. */
public record InigressoConfig(
        List<String> worlds,
        int portalMinDistance,
        int portalProtectRadius,
        int resonatorRadius,
        List<Material> resonatorBlocks,
        int proposalMinLevel,
        int proposalMaxOpen,
        int linkMaxRange,
        int linkMaxOutgoing,
        double linkBeamHeight,
        int decayPercentPerDay,
        LocalTime decayTime,
        int hackCooldownMinutes,
        int hackMaxPer4h,
        double hackKeyChance,
        int playerMaxItems,
        DayOfWeek cycleStartDay,
        int cycleLengthHours,
        int measureMinutes,
        boolean autoPortals,
        int autoSpacing,
        int autoJitter,
        boolean scoreboard) {

    private static final List<Material> DEFAULT_BLOCKS = List.of(
            Material.COPPER_BLOCK, Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.REDSTONE_BLOCK,
            Material.LAPIS_BLOCK, Material.EMERALD_BLOCK, Material.AMETHYST_BLOCK, Material.DIAMOND_BLOCK);

    public static InigressoConfig from(FileConfiguration c, java.util.logging.Logger log) {
        return new InigressoConfig(
                List.copyOf(c.getStringList("worlds")),
                c.getInt("portal.min-distance", 48),
                c.getInt("portal.protect-radius", 4),
                c.getInt("portal.resonator-radius", 3),
                blocks(c.getStringList("portal.resonator-blocks"), log),
                c.getInt("proposal.min-level", 2),
                c.getInt("proposal.max-open", 3),
                c.getInt("link.max-range", 2000),
                c.getInt("link.max-outgoing", 8),
                c.getDouble("link.beam-height", 5.0),
                c.getInt("decay.percent-per-day", 15),
                time(c.getString("decay.time", "03:00"), log),
                c.getInt("hack.cooldown-minutes", 5),
                c.getInt("hack.max-per-4h", 4),
                c.getDouble("hack.key-chance", 0.50),
                c.getInt("player.max-items", 500),
                day(c.getString("cycle.start-day", "MONDAY"), log),
                Math.max(1, c.getInt("cycle.length-hours", 168)),
                Math.max(1, c.getInt("cycle.measure-minutes", 60)),
                c.getBoolean("auto-portals.enabled", false),
                Math.max(16, c.getInt("auto-portals.spacing", 100)),
                Math.max(0, c.getInt("auto-portals.jitter", 25)),
                c.getBoolean("scoreboard.enabled", true));
    }

    private static List<Material> blocks(List<String> names, java.util.logging.Logger log) {
        if (names.size() != 8) {
            log.warning("portal.resonator-blocks braucht genau 8 Einträge – nehme Standard.");
            return DEFAULT_BLOCKS;
        }
        Material[] out = new Material[8];
        for (int i = 0; i < 8; i++) {
            Material m = Material.matchMaterial(names.get(i));
            if (m == null || !m.isBlock()) {
                log.warning("Unbekannter Block in portal.resonator-blocks: " + names.get(i) + " – nehme Standard.");
                m = DEFAULT_BLOCKS.get(i);
            }
            out[i] = m;
        }
        return List.of(out);
    }

    private static LocalTime time(String s, java.util.logging.Logger log) {
        try {
            return LocalTime.parse(s);
        } catch (RuntimeException e) {
            log.warning("decay.time ungültig: " + s + " – nehme 03:00.");
            return LocalTime.of(3, 0);
        }
    }

    private static DayOfWeek day(String s, java.util.logging.Logger log) {
        try {
            return DayOfWeek.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            log.warning("cycle.start-day ungültig: " + s + " – nehme MONDAY.");
            return DayOfWeek.MONDAY;
        }
    }
}
