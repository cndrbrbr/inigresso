package io.github.cndrbrbr.inigresso.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Ein Spielgegenstand ohne Bukkit-Bezug (rules.md §3). level für Resonator/Burst 1–8,
 * portalId nur für Schlüssel. faction = Fraktion des Besitzers beim Erhalt; Gegenstände einer
 * anderen Fraktion werden nach einem Fraktionswechsel entfernt.
 */
public record GameItem(Type type, int level, int portalId, Faction faction) {

    public enum Type {
        RESONATOR("Resonator", "resonator"),
        BURST("Burst", "burst"),
        KEY("Portalschlüssel", "schluessel");

        private final String displayName;
        private final String key;

        Type(String displayName, String key) {
            this.displayName = displayName;
            this.key = key;
        }

        public String displayName() {
            return displayName;
        }

        public String key() {
            return key;
        }

        public static Optional<Type> parse(String s) {
            String k = s.toLowerCase(Locale.ROOT).replace("ü", "ue");
            for (Type t : values()) {
                if (t.key.equals(k) || t.name().equalsIgnoreCase(k)) {
                    return Optional.of(t);
                }
            }
            return Optional.empty();
        }
    }

    public static GameItem resonator(int level, Faction f) {
        return new GameItem(Type.RESONATOR, level, 0, f);
    }

    public static GameItem burst(int level, Faction f) {
        return new GameItem(Type.BURST, level, 0, f);
    }

    public static GameItem key(int portalId, Faction f) {
        return new GameItem(Type.KEY, 0, portalId, f);
    }
}
