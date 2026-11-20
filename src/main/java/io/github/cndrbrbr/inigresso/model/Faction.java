package io.github.cndrbrbr.inigresso.model;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.ChatColor;

/** Fraktionen nach rules.md §1. NEUTRAL ist nur Besitzer von Portalen, nie von Spielern. */
public enum Faction {
    LUMEN("Lumen", "lumen", ChatColor.GREEN),
    WAECHTER("Wächter", "waechter", ChatColor.BLUE),
    CLIP("Clip", "clip", ChatColor.RED),
    NEUTRAL("Neutral", "neutral", ChatColor.GRAY);

    private final String displayName;
    private final String key;
    private final ChatColor color;

    Faction(String displayName, String key, ChatColor color) {
        this.displayName = displayName;
        this.key = key;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public String key() {
        return key;
    }

    public ChatColor color() {
        return color;
    }

    public String colored() {
        return color + displayName;
    }

    /** Spielbare Fraktion aus Eingabe (lumen, waechter, wächter); NEUTRAL ist nicht wählbar. */
    public static Optional<Faction> parsePlayable(String s) {
        String k = s.toLowerCase(Locale.ROOT).replace("ä", "ae");
        for (Faction f : values()) {
            if (f != NEUTRAL && f.key.equals(k)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    public static Faction[] playable() {
        return new Faction[] {LUMEN, WAECHTER, CLIP};
    }

    /** Schlüssel aller spielbaren Fraktionen, z. B. für Tab-Vervollständigung und Hilfetexte. */
    public static java.util.List<String> playableKeys() {
        return java.util.Arrays.stream(playable()).map(Faction::key).toList();
    }

    /** Spielbare Fraktion mit dem eindeutig höchsten Wert; null bei Gleichstand an der Spitze. */
    public static Faction leader(java.util.function.ToDoubleFunction<Faction> value) {
        Faction best = null;
        double max = Double.NEGATIVE_INFINITY;
        boolean tie = false;
        for (Faction f : playable()) {
            double v = value.applyAsDouble(f);
            if (v > max) {
                max = v;
                best = f;
                tie = false;
            } else if (v == max) {
                tie = true;
            }
        }
        return tie ? null : best;
    }
}
