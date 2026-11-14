package io.github.cndrbrbr.inigresso.render;

import java.util.Arrays;

/**
 * 128×128-Bild aus Kartenfarb-Indizes (byte) mit einfachen Zeichenroutinen.
 * Ohne Bukkit, damit es testbar ist; ScannerRenderer kopiert es auf die Karte.
 * Koordinaten sind Pixel, (0,0) oben links.
 */
public final class ScannerImage {

    public static final int SIZE = 128;

    private final byte[] px = new byte[SIZE * SIZE];

    public void fill(byte c) {
        Arrays.fill(px, c);
    }

    public byte get(int x, int y) {
        return px[y * SIZE + x];
    }

    public void set(int x, int y, byte c) {
        if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
            px[y * SIZE + x] = c;
        }
    }

    /** Linie, vorher auf das Bild beschnitten (Liang-Barsky), damit lange Links billig bleiben. */
    public void line(double x0, double y0, double x1, double y1, byte c) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double[] t = {0, 1};
        double max = SIZE - 1;
        if (!clip(-dx, x0, t) || !clip(dx, max - x0, t) || !clip(-dy, y0, t) || !clip(dy, max - y0, t)) {
            return;
        }
        double ax = x0 + t[0] * dx;
        double ay = y0 + t[0] * dy;
        double bx = x0 + t[1] * dx;
        double by = y0 + t[1] * dy;
        int steps = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(by - ay)));
        if (steps == 0) {
            set((int) Math.round(ax), (int) Math.round(ay), c);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            double s = (double) i / steps;
            set((int) Math.round(ax + s * (bx - ax)), (int) Math.round(ay + s * (by - ay)), c);
        }
    }

    private static boolean clip(double p, double q, double[] t) {
        if (p == 0) {
            return q >= 0;
        }
        double r = q / p;
        if (p < 0) {
            if (r > t[1]) {
                return false;
            }
            t[0] = Math.max(t[0], r);
        } else {
            if (r < t[0]) {
                return false;
            }
            t[1] = Math.min(t[1], r);
        }
        return true;
    }

    /**
     * Gefülltes Dreieck. Mit dither nur jedes zweite Pixel im Schachbrett, das wirkt auf der
     * Karte wie eine halbtransparente Fläche (Kartenfarben kennen kein Alpha).
     */
    public void triangle(double ax, double ay, double bx, double by, double cx, double cy, byte c, boolean dither) {
        double det = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy);
        if (Math.abs(det) < 1e-9) {
            return;
        }
        int minX = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx))));
        int maxX = Math.min(SIZE - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
        int minY = Math.max(0, (int) Math.floor(Math.min(ay, Math.min(by, cy))));
        int maxY = Math.min(SIZE - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (dither && ((x + y) & 1) != 0) {
                    continue;
                }
                double px = x + 0.5;
                double py = y + 0.5;
                double l1 = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / det;
                double l2 = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / det;
                if (l1 >= 0 && l2 >= 0 && l1 + l2 <= 1) {
                    set(x, y, c);
                }
            }
        }
    }

    /** Gefüllter Kreis mit Radius r Pixel um (cx, cy). */
    public void disc(double cx, double cy, double r, byte c) {
        int x0 = (int) Math.floor(cx - r);
        int x1 = (int) Math.ceil(cx + r);
        int y0 = (int) Math.floor(cy - r);
        int y1 = (int) Math.ceil(cy + r);
        double r2 = r * r + 0.25;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                double ddx = x + 0.5 - cx;
                double ddy = y + 0.5 - cy;
                if (ddx * ddx + ddy * ddy <= r2) {
                    set(x, y, c);
                }
            }
        }
    }

    /** Kreisring (1 Pixel breit). */
    public void ring(double cx, double cy, double r, byte c) {
        int n = Math.max(12, (int) (r * 8));
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            set((int) Math.floor(cx + r * Math.cos(a)), (int) Math.floor(cy + r * Math.sin(a)), c);
        }
    }
}
