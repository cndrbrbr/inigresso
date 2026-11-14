package io.github.cndrbrbr.inigresso.render;

/**
 * Welt → Kartenpixel für den Scanner. Der Spieler steht in der Mitte (64, 64).
 * Gedreht: Blickrichtung zeigt nach oben. Nicht gedreht: Norden oben.
 *
 * @param centerX Weltposition des Spielers
 * @param centerZ Weltposition des Spielers
 * @param heading Blickrichtung in Grad ab Norden (im Uhrzeigersinn)
 * @param span    Kartenbreite in Blöcken
 * @param rotate  true = Blickrichtung oben
 */
public record ScannerProjection(double centerX, double centerZ, double heading, double span, boolean rotate) {

    public static final double MID = ScannerImage.SIZE / 2.0;

    public double blocksPerPixel() {
        return span / ScannerImage.SIZE;
    }

    /** Pixelkoordinaten {x, y} eines Weltpunkts; kann außerhalb des Bilds liegen. */
    public double[] toScreen(double x, double z) {
        double dx = (x - centerX) / blocksPerPixel();
        double dz = (z - centerZ) / blocksPerPixel();
        if (!rotate) {
            return new double[] {MID + dx, MID + dz};
        }
        double h = Math.toRadians(heading);
        double cos = Math.cos(h);
        double sin = Math.sin(h);
        return new double[] {MID + dx * cos + dz * sin, MID + dz * cos - dx * sin};
    }

    /** Liegt der Pixel im Bild (mit Rand)? */
    public static boolean visible(double[] p, double margin) {
        return p[0] >= -margin && p[1] >= -margin && p[0] < ScannerImage.SIZE + margin
                && p[1] < ScannerImage.SIZE + margin;
    }

    /** Punkt auf den Bildrand ziehen (für Ziele außerhalb der Karte), Richtung bleibt erhalten. */
    public static double[] clampToEdge(double[] p, double inset) {
        double dx = p[0] - MID;
        double dy = p[1] - MID;
        double lim = MID - inset;
        double m = Math.max(Math.abs(dx), Math.abs(dy));
        if (m <= lim) {
            return p;
        }
        return new double[] {MID + dx * lim / m, MID + dy * lim / m};
    }
}
