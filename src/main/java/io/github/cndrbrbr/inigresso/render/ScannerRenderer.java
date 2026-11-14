package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.geo.Compass;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Field;
import io.github.cndrbrbr.inigresso.model.Link;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.LinkService;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

/**
 * Scanner-Karte: dunkler Hintergrund mit Gitter, Felder als gerasterte Fläche, Links als
 * Linien, Portale als Punkte in Fraktionsfarbe. Kontextbezogen, d. h. jeder Spieler sieht
 * seine eigene Umgebung auf derselben Karten-ID.
 */
@SuppressWarnings("deprecation") // MapPalette.matchColor/setPixel: schnellster Weg für ganze Bilder
public final class ScannerRenderer extends MapRenderer {

    /** Kartenbreite in Blöcken je Zoomstufe. */
    public static final int[] ZOOMS = {128, 256, 512, 1024};
    static final int DEFAULT_ZOOM = 1;
    /** Neu zeichnen höchstens so oft (ms), ohne Bewegung spätestens nach REFRESH_MS. */
    static final long MIN_MS = 100;
    static final long REFRESH_MS = 1000;

    private static final byte BG = MapPalette.matchColor(14, 16, 24);
    private static final byte GRID = MapPalette.matchColor(38, 48, 66);
    private static final byte NEUTRAL = MapPalette.matchColor(170, 170, 170);
    private static final byte LUMEN = MapPalette.matchColor(110, 240, 70);
    private static final byte WAECHTER = MapPalette.matchColor(70, 120, 255);
    private static final byte LUMEN_FIELD = MapPalette.matchColor(40, 120, 30);
    private static final byte WAECHTER_FIELD = MapPalette.matchColor(30, 55, 150);
    private static final byte TARGET = MapPalette.matchColor(255, 200, 40);

    private final PortalService portals;
    private final LinkService links;
    private final Navigator navigator;
    private final java.util.function.Function<UUID, Faction> factionOf;
    private final Map<UUID, Settings> settings = new HashMap<>();

    /** Einstellungen eines Spielers (nur im Speicher). */
    private static final class Settings {
        int zoom = DEFAULT_ZOOM;
        boolean rotate = true;
        long lastKey;
        long lastAt;
    }

    public ScannerRenderer(PortalService portals, LinkService links, Navigator navigator,
            java.util.function.Function<UUID, Faction> factionOf) {
        super(true);
        this.portals = portals;
        this.links = links;
        this.navigator = navigator;
        this.factionOf = factionOf;
    }

    private Settings of(Player p) {
        return settings.computeIfAbsent(p.getUniqueId(), k -> new Settings());
    }

    /** Nächste Zoomstufe; liefert die neue Kartenbreite in Blöcken. */
    public int cycleZoom(Player p) {
        Settings s = of(p);
        s.zoom = (s.zoom + 1) % ZOOMS.length;
        s.lastAt = 0;
        return ZOOMS[s.zoom];
    }

    /** Zoom auf die passende Stufe setzen; false, wenn es die Breite nicht gibt. */
    public boolean setZoom(Player p, int span) {
        for (int i = 0; i < ZOOMS.length; i++) {
            if (ZOOMS[i] == span) {
                of(p).zoom = i;
                of(p).lastAt = 0;
                return true;
            }
        }
        return false;
    }

    public int zoom(Player p) {
        return ZOOMS[of(p).zoom];
    }

    public void setRotate(Player p, boolean rotate) {
        of(p).rotate = rotate;
        of(p).lastAt = 0;
    }

    public void forget(UUID player) {
        settings.remove(player);
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        Settings s = of(player);
        Location loc = player.getLocation();
        int span = ZOOMS[s.zoom];
        double bpp = (double) span / ScannerImage.SIZE;
        double heading = Compass.heading(loc.getYaw());
        Optional<Portal> target = navigator.target(player.getUniqueId());
        // Nur neu zeichnen, wenn sich etwas Sichtbares geändert hat oder REFRESH_MS vorbei ist.
        long key = 31L * Math.floorDiv((long) loc.getX(), (long) Math.max(1, bpp))
                + 1009L * Math.floorDiv((long) loc.getZ(), (long) Math.max(1, bpp))
                + 7919L * (s.rotate ? Math.round(heading / 4) : -1)
                + 104729L * s.zoom + 1299709L * target.map(Portal::id).orElse(0)
                + loc.getWorld().getName().hashCode();
        long now = System.currentTimeMillis();
        if (now - s.lastAt < MIN_MS || (key == s.lastKey && now - s.lastAt < REFRESH_MS)) {
            return;
        }
        s.lastKey = key;
        s.lastAt = now;

        ScannerProjection proj = new ScannerProjection(loc.getX(), loc.getZ(), heading, span, s.rotate);
        String world = loc.getWorld().getName();
        ScannerImage img = paint(proj, world, portals.all(), links.links(), links.fields(), portals::get,
                target.orElse(null));
        for (int y = 0; y < ScannerImage.SIZE; y++) {
            for (int x = 0; x < ScannerImage.SIZE; x++) {
                canvas.setPixel(x, y, img.get(x, y));
            }
        }
        drawLabels(canvas, proj, span, s.rotate);
        drawNumbers(canvas, proj, world, span, target.orElse(null), s.rotate);
        drawFaction(canvas, factionOf.apply(player.getUniqueId()));
        canvas.setCursors(cursors(proj, s.rotate, heading, target.filter(t -> t.core().world().equals(world))));
    }

    /** Das ganze Bild ohne Bukkit-Laufzeit (auch für Vorschaubilder). */
    public static ScannerImage paint(ScannerProjection proj, String world, java.util.Collection<Portal> allPortals,
            java.util.Collection<Link> allLinks, java.util.Collection<Field> allFields,
            java.util.function.IntFunction<Optional<Portal>> lookup, Portal target) {
        int span = (int) proj.span();
        ScannerImage img = new ScannerImage();
        img.fill(BG);
        drawGrid(img, proj, span);
        for (Field f : allFields) {
            Optional<Portal> a = lookup.apply(f.a());
            Optional<Portal> b = lookup.apply(f.b());
            Optional<Portal> c = lookup.apply(f.c());
            if (a.isEmpty() || b.isEmpty() || c.isEmpty() || !a.get().core().world().equals(world)) {
                continue;
            }
            double[] pa = screen(proj, a.get());
            double[] pb = screen(proj, b.get());
            double[] pc = screen(proj, c.get());
            img.triangle(pa[0], pa[1], pb[0], pb[1], pc[0], pc[1],
                    f.faction() == Faction.LUMEN ? LUMEN_FIELD : WAECHTER_FIELD, true);
        }
        for (Link l : allLinks) {
            Optional<Portal> a = lookup.apply(l.from());
            Optional<Portal> b = lookup.apply(l.to());
            if (a.isEmpty() || b.isEmpty() || !a.get().core().world().equals(world)) {
                continue;
            }
            double[] pa = screen(proj, a.get());
            double[] pb = screen(proj, b.get());
            img.line(pa[0], pa[1], pb[0], pb[1], color(l.faction()));
        }
        double dot = dotSize(span);
        for (Portal p : allPortals) {
            if (!p.core().world().equals(world)) {
                continue;
            }
            double[] pp = screen(proj, p);
            if (ScannerProjection.visible(pp, 3)) {
                img.disc(pp[0], pp[1], p.owner() == Faction.NEUTRAL ? Math.max(1, dot - 0.5) : dot, color(p.owner()));
            }
        }
        if (target != null && target.core().world().equals(world)) {
            double[] tp = screen(proj, target);
            if (ScannerProjection.visible(tp, 0)) {
                img.ring(tp[0], tp[1], dot + 2.5, TARGET);
            }
        }
        return img;
    }

    private static double[] screen(ScannerProjection proj, Portal p) {
        return proj.toScreen(p.core().x() + 0.5, p.core().z() + 0.5);
    }

    private static byte color(Faction f) {
        return switch (f) {
            case LUMEN -> LUMEN;
            case WAECHTER -> WAECHTER;
            case NEUTRAL -> NEUTRAL;
        };
    }

    /** Gitter an festen Weltkoordinaten, vier Linien über die Kartenbreite. */
    private static void drawGrid(ScannerImage img, ScannerProjection proj, int span) {
        int step = span / 4;
        double reach = span * 0.75; // deckt auch die Ecken der gedrehten Karte ab
        double minX = proj.centerX() - reach;
        double maxX = proj.centerX() + reach;
        double minZ = proj.centerZ() - reach;
        double maxZ = proj.centerZ() + reach;
        for (long gx = (long) Math.ceil(minX / step) * step; gx <= maxX; gx += step) {
            double[] a = proj.toScreen(gx, minZ);
            double[] b = proj.toScreen(gx, maxZ);
            img.line(a[0], a[1], b[0], b[1], GRID);
        }
        for (long gz = (long) Math.ceil(minZ / step) * step; gz <= maxZ; gz += step) {
            double[] a = proj.toScreen(minX, gz);
            double[] b = proj.toScreen(maxX, gz);
            img.line(a[0], a[1], b[0], b[1], GRID);
        }
    }

    /** Portalnummern neben den Punkten, ohne Überlappung (siehe ScannerLabels). */
    private void drawNumbers(MapCanvas canvas, ScannerProjection proj, String world, int span, Portal target,
            boolean rotate) {
        List<int[]> reserved = new java.util.ArrayList<>(List.of(new int[] {0, 0, 32, 10}, new int[] {80, 0, 127, 10}));
        if (rotate) {
            int[] n = northLabel(proj);
            reserved.add(new int[] {n[0] - 2, n[1] - 2, n[0] + 7, n[1] + 9}); // rotes N freihalten
        }
        for (ScannerLabels.Label l : ScannerLabels.place(proj, world, portals.all(), target, dotSize(span), reserved)) {
            byte c = l.portal().equals(target) ? TARGET : color(l.portal().owner());
            canvas.drawText(l.x(), l.y(), MinecraftFont.Font, "§" + (c & 0xff) + ";" + l.text());
        }
    }

    static double dotSize(int span) {
        return span <= 256 ? 2 : span <= 512 ? 1.5 : 1;
    }

    /** Eigene Fraktion oben rechts, damit jeder sieht, wozu er gehört. */
    private static void drawFaction(MapCanvas canvas, Faction f) {
        if (f == null || f == Faction.NEUTRAL) {
            return;
        }
        String name = MinecraftFont.Font.isValid(f.displayName()) ? f.displayName() : f.key();
        int w = MinecraftFont.Font.getWidth(name);
        canvas.drawText(ScannerImage.SIZE - 2 - w, 2, MinecraftFont.Font, "§" + (color(f) & 0xff) + ";" + name);
    }

    private static void drawLabels(MapCanvas canvas, ScannerProjection proj, int span, boolean rotate) {
        canvas.drawText(2, 2, MinecraftFont.Font, "§" + (MapPalette.WHITE & 0xff) + ";" + span + "m");
        if (rotate) {
            int[] n = northLabel(proj);
            canvas.drawText(n[0], n[1], MinecraftFont.Font, "§" + (MapPalette.RED & 0xff) + ";N");
        }
    }

    /** Linke obere Ecke des roten N am Kartenrand in Richtung Norden. */
    static int[] northLabel(ScannerProjection proj) {
        double[] n = proj.toScreen(proj.centerX(), proj.centerZ() - proj.blocksPerPixel());
        double dx = n[0] - ScannerProjection.MID;
        double dy = n[1] - ScannerProjection.MID;
        double[] edge = ScannerProjection.clampToEdge(
                new double[] {ScannerProjection.MID + dx * 200, ScannerProjection.MID + dy * 200}, 6);
        return new int[] {(int) edge[0] - 2, (int) edge[1] - 3};
    }

    private static MapCursorCollection cursors(ScannerProjection proj, boolean rotate, double heading,
            Optional<Portal> target) {
        MapCursorCollection c = new MapCursorCollection();
        // Richtung 0–15; 8 = nach oben, 0 = nach unten (wie Vanilla: Yaw 0 = Süden).
        byte dir = rotate ? 8 : (byte) ((Math.round(heading / 22.5) + 8) & 15);
        c.addCursor(new MapCursor((byte) 0, (byte) 0, dir, MapCursor.Type.PLAYER, true));
        target.ifPresent(t -> {
            double[] p = ScannerProjection.clampToEdge(screen(proj, t), 4);
            c.addCursor(new MapCursor(cursorCoord(p[0]), cursorCoord(p[1]), (byte) 0,
                    MapCursor.Type.TARGET_POINT, true, t.name()));
        });
        return c;
    }

    /** Pixel 0–128 → Cursor-Koordinate −128…127. */
    private static byte cursorCoord(double px) {
        return (byte) Math.clamp(Math.round(px * 2 - 128), -128, 127);
    }
}
