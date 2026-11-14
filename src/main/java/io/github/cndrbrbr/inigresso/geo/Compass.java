package io.github.cndrbrbr.inigresso.geo;

/**
 * Himmelsrichtungen und Richtungspfeile ohne Bukkit. Weltachsen wie in Minecraft:
 * +X = Osten, +Z = Süden. Winkel im Uhrzeigersinn ab Norden.
 */
public final class Compass {

    private static final String[] CARDINAL = {"N", "NO", "O", "SO", "S", "SW", "W", "NW"};
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private Compass() {
    }

    /** Peilung (Grad, 0 = Norden, 90 = Osten) für den Versatz dx/dz. */
    public static double bearing(double dx, double dz) {
        return norm(Math.toDegrees(Math.atan2(dx, -dz)));
    }

    /** Blickrichtung (Grad ab Norden) aus dem Minecraft-Yaw (0 = Süden, 90 = Westen). */
    public static double heading(float yaw) {
        return norm(yaw + 180.0);
    }

    /** N, NO, O, … für den Versatz dx/dz. */
    public static String cardinal(double dx, double dz) {
        return CARDINAL[sector(bearing(dx, dz))];
    }

    /** Pfeil relativ zur Blickrichtung: ↑ = geradeaus, → = rechts. */
    public static String arrow(float yaw, double dx, double dz) {
        return ARROWS[sector(bearing(dx, dz) - heading(yaw))];
    }

    static int sector(double degrees) {
        return (int) Math.floor((norm(degrees) + 22.5) / 45.0) % 8;
    }

    static double norm(double degrees) {
        double d = degrees % 360.0;
        return d < 0 ? d + 360.0 : d;
    }
}
