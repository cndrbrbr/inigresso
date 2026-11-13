package io.github.cndrbrbr.inigresso.service;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Predicate;

/**
 * Raster für automatisch gesetzte Portale (rules.md §2.4): je Zelle spacing × spacing ein
 * Zielpunkt, um bis zu jitter Blöcke zufällig, aber reproduzierbar verschoben. Ohne Bukkit.
 */
public final class AutoPortalPlanner {

    public record Cell(String world, int gx, int gz) {
        public String key() {
            return world + ":" + gx + ":" + gz;
        }
    }

    public record Target(Cell cell, int x, int z) {
    }

    private static final String[] FIRST = {
        "Moos", "Wind", "Stern", "Eichen", "Fels", "Nebel", "Sonnen", "Mond", "Birken", "Bach",
        "Kiesel", "Farn", "Funken", "Ahorn", "Wolken", "Silber", "Kupfer", "Rosen", "Distel", "Tannen",
        "Linden", "Raben", "Falken", "Quarz", "Glut", "Tau", "Eis", "Weiden", "Hasel", "Erlen"};
    private static final String[] SECOND = {
        "stein", "tor", "hain", "warte", "quell", "turm", "bühl", "au", "eck", "kreuz",
        "brunnen", "feld", "hütte", "horn", "grund", "blick", "born", "wacht", "mal", "ruh",
        "lichtung", "pfad", "hang", "spitze", "senke"};
    private static final String[] SUFFIX = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    private final int spacing;
    private final int jitter;

    public AutoPortalPlanner(int spacing, int jitter) {
        if (spacing < 16) {
            throw new IllegalArgumentException("spacing muss mindestens 16 sein");
        }
        this.spacing = spacing;
        this.jitter = Math.clamp(jitter, 0, spacing / 2 - 1);
    }

    public int spacing() {
        return spacing;
    }

    private static SplittableRandom random(Cell c) {
        long h = c.world().hashCode();
        h = h * 0x9E3779B97F4A7C15L + c.gx();
        h = h * 0xC2B2AE3D27D4EB4FL + c.gz();
        return new SplittableRandom(h);
    }

    public Target target(Cell c) {
        SplittableRandom r = random(c);
        int dx = jitter == 0 ? 0 : r.nextInt(-jitter, jitter + 1);
        int dz = jitter == 0 ? 0 : r.nextInt(-jitter, jitter + 1);
        return new Target(c, c.gx() * spacing + dx, c.gz() * spacing + dz);
    }

    /** Alle Zielpunkte, die im Chunk (cx, cz) liegen. */
    public List<Target> targetsInChunk(String world, int cx, int cz) {
        int minX = cx << 4;
        int minZ = cz << 4;
        List<Target> out = new ArrayList<>(1);
        for (int gx = Math.floorDiv(minX - jitter, spacing); gx <= Math.floorDiv(minX + 15 + jitter, spacing) + 1; gx++) {
            for (int gz = Math.floorDiv(minZ - jitter, spacing); gz <= Math.floorDiv(minZ + 15 + jitter, spacing) + 1; gz++) {
                Target t = target(new Cell(world, gx, gz));
                if (t.x() >> 4 == cx && t.z() >> 4 == cz) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    /** Reproduzierbarer Name; ist er vergeben, kommt eine römische Ziffer dazu. */
    public String name(Cell c, Predicate<String> taken) {
        SplittableRandom r = random(c);
        r.nextInt(); // nicht dieselben Zahlen wie beim Versatz
        r.nextInt();
        String base = FIRST[r.nextInt(FIRST.length)] + SECOND[r.nextInt(SECOND.length)];
        for (String s : SUFFIX) {
            if (!taken.test(base + s)) {
                return base + s;
            }
        }
        return base + " " + c.gx() + "|" + c.gz();
    }
}
