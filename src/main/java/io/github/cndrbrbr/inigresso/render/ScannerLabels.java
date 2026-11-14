package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.model.Portal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Wo die Portalnummern auf dem Scanner stehen. Ohne Bukkit: Schriftbreite wird für Ziffern
 * der Karten-Schrift angenähert (5 Pixel + 1 Abstand, 7 hoch). Nummern dürfen sich nicht
 * überlappen; Portale nahe am Spieler und das Navigationsziel haben Vorrang.
 */
public final class ScannerLabels {

    /** Ab dieser Kartenbreite (Blöcke) nur noch die Nummer des Ziels. */
    static final int MAX_SPAN = 512;
    static final int HEIGHT = 7;

    public record Label(int x, int y, String text, Portal portal) {
    }

    private record Box(int x0, int y0, int x1, int y1) {
        boolean hits(Box o) {
            return x0 <= o.x1 && o.x0 <= x1 && y0 <= o.y1 && o.y0 <= y1;
        }
    }

    private ScannerLabels() {
    }

    static int width(String text) {
        return text.length() * 6 - 1;
    }

    /**
     * @param reserved freizuhaltende Bereiche {x0, y0, x1, y1}, z. B. Zoom- und Fraktionsanzeige
     */
    public static List<Label> place(ScannerProjection proj, String world, Collection<Portal> portals,
            Portal target, double dot, List<int[]> reserved) {
        List<Box> used = new ArrayList<>();
        for (int[] r : reserved) {
            used.add(new Box(r[0], r[1], r[2], r[3]));
        }
        int mid = (int) ScannerProjection.MID;
        used.add(new Box(mid - 4, mid - 4, mid + 4, mid + 4)); // Spielerpfeil
        // Bildposition nur einmal je Portal rechnen, unsichtbare gleich verwerfen.
        record Spot(Portal p, double x, double y, double d) {
        }
        List<Spot> order = new ArrayList<>();
        for (Portal p : portals) {
            if (!p.core().world().equals(world) || (proj.span() > MAX_SPAN && !p.equals(target))) {
                continue;
            }
            double[] s = screen(proj, p);
            if (ScannerProjection.visible(s, 0)) {
                order.add(new Spot(p, s[0], s[1], p.equals(target) ? -1 : Math.hypot(s[0] - mid, s[1] - mid)));
            }
        }
        order.sort(Comparator.comparingDouble(Spot::d));
        List<Label> out = new ArrayList<>();
        for (Spot spot : order) {
            Portal p = spot.p();
            double[] s = {spot.x(), spot.y()};
            String text = String.valueOf(p.id());
            int w = width(text);
            int gap = (int) Math.ceil(dot) + 2;
            int y = (int) Math.round(s[1]) - HEIGHT / 2;
            int[][] tries = {
                {(int) Math.round(s[0]) + gap, y},         // rechts
                {(int) Math.round(s[0]) - gap - w, y},     // links
                {(int) Math.round(s[0]) - w / 2, (int) Math.round(s[1]) - gap - HEIGHT}, // oben
                {(int) Math.round(s[0]) - w / 2, (int) Math.round(s[1]) + gap},          // unten
            };
            for (int[] t : tries) {
                Box b = new Box(t[0] - 1, t[1] - 1, t[0] + w, t[1] + HEIGHT);
                if (t[0] < 0 || t[1] < 0 || t[0] + w >= ScannerImage.SIZE || t[1] + HEIGHT >= ScannerImage.SIZE
                        || used.stream().anyMatch(b::hits)) {
                    continue;
                }
                used.add(b);
                out.add(new Label(t[0], t[1], text, p));
                break;
            }
        }
        return out;
    }

    private static double[] screen(ScannerProjection proj, Portal p) {
        return proj.toScreen(p.core().x() + 0.5, p.core().z() + 0.5);
    }
}
