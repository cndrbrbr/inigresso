package io.github.cndrbrbr.inigresso.model;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.ChatColor;

/** Fraktionen nach rules.md §1. NEUTRAL ist nur Besitzer von Portalen, nie von Spielern. */
public enum Faction {
    LUMEN("Lumen", "lumen", ChatColor.GREEN),
    WAECHTER("Wächter", "waechter", ChatColor.BLUE),
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

    public Faction opponent() {
        return switch (this) {
            case LUMEN -> WAECHTER;
            case WAECHTER -> LUMEN;
            case NEUTRAL -> NEUTRAL;
        };
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
        return new Faction[] {LUMEN, WAECHTER};
    }
}
