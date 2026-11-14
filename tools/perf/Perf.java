import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.geo.*;
import io.github.cndrbrbr.inigresso.model.*;
import io.github.cndrbrbr.inigresso.render.*;
import io.github.cndrbrbr.inigresso.service.*;
import java.time.Clock;
import java.util.*;
import java.util.logging.Logger;

public class Perf {
    static long sink;

    interface Op { void run(int i); }

    static double bench(String name, Op op) {
        for (int i = 0; i < 2000; i++) op.run(i); // Aufwärmen
        int n = 0;
        long t0 = System.nanoTime();
        while (System.nanoTime() - t0 < 2_000_000_000L) { op.run(n++); }
        double us = (System.nanoTime() - t0) / 1000.0 / n;
        System.out.printf(Locale.ROOT, "%-46s %10.1f µs%n", name, us);
        return us;
    }

    public static void main(String[] a) {
        Random rnd = new Random(1);
        GameEventBus bus = new GameEventBus(Logger.getLogger("perf"));
        int cols = 20, rows = 10, sp = 120;
        List<Portal> list = new ArrayList<>();
        Portal[][] grid = new Portal[cols][rows];
        int id = 1;
        UUID who = UUID.randomUUID();
        for (int x = 0; x < cols; x++) for (int z = 0; z < rows; z++) {
            Faction f = x < cols / 2 ? Faction.LUMEN : Faction.WAECHTER;
            if (x == cols / 2 - 1 && z % 3 == 0) f = Faction.NEUTRAL;
            Portal p = new Portal(id++, "P" + id, new BlockPos("world", x * sp + rnd.nextInt(30), 64, z * sp + rnd.nextInt(30)),
                    f, null, 0, null, 0);
            if (f != Faction.NEUTRAL) for (int s = 0; s < 8; s++) p.setResonator(s, Resonator.full(8, who));
            grid[x][z] = p; list.add(p);
        }
        PortalService portals = new PortalService(bus, Clock.systemUTC(), () -> 4, new PlayerProgressService(bus), list);
        List<Link> links = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        int lid = 1, fid = 1;
        Set<Long> have = new HashSet<>();
        java.util.function.BiFunction<Portal, Portal, Boolean> link = (p, q) -> p.owner() != Faction.NEUTRAL && p.owner() == q.owner();
        for (int x = 0; x < cols; x++) for (int z = 0; z < rows; z++) {
            Portal p = grid[x][z];
            Portal[] nb = {x + 1 < cols ? grid[x + 1][z] : null, z + 1 < rows ? grid[x][z + 1] : null,
                    x + 1 < cols && z + 1 < rows ? grid[x + 1][z + 1] : null};
            for (Portal q : nb) if (q != null && link.apply(p, q)) links.add(new Link(lid++, p.id(), q.id(), p.owner(), who, 0));
            if (x + 1 < cols && z + 1 < rows) {
                Portal b = grid[x + 1][z], c = grid[x][z + 1], d = grid[x + 1][z + 1];
                if (link.apply(p, b) && link.apply(p, d) && link.apply(b, d))
                    fields.add(new Field(fid++, p.id(), b.id(), d.id(), p.owner(), 150, who, 0));
                if (link.apply(p, c) && link.apply(p, d) && link.apply(c, d))
                    fields.add(new Field(fid++, p.id(), c.id(), d.id(), p.owner(), 150, who, 0));
            }
        }
        LinkService ls = new LinkService(portals, new PlayerProgressService(bus), bus, Clock.systemUTC(),
                () -> new LinkService.Rules(2000, 8), links, fields);
        System.out.printf("Portale %d, Links %d, Felder %d%n", list.size(), links.size(), fields.size());
        int W = cols * sp, H = rows * sp;
        double[][] pos = new double[1024][];
        for (int i = 0; i < pos.length; i++) pos[i] = new double[] {rnd.nextDouble() * W, rnd.nextDouble() * H, rnd.nextDouble() * 360};

        Map<String, Double> r = new LinkedHashMap<>();
        for (int span : new int[] {256, 512, 1024}) {
            r.put("scan" + span, bench("Scanner-Bild (paint) " + span + " Blöcke", i -> {
                double[] p = pos[i & 1023];
                var proj = new ScannerProjection(p[0], p[1], p[2], span, true);
                sink += ScannerRenderer.paint(proj, "world", portals.all(), ls.links(), ls.fields(), portals::get, null).get(64, 64);
            }));
            r.put("lab" + span, bench("Scanner-Nummern (place) " + span + " Blöcke", i -> {
                double[] p = pos[i & 1023];
                var proj = new ScannerProjection(p[0], p[1], p[2], span, true);
                sink += ScannerLabels.place(proj, "world", portals.all(), null, 2, List.of()).size();
            }));
        }
        r.put("bar", bench("Bossbar: fieldsAt je Spieler", i -> {
            double[] p = pos[i & 1023];
            sink += ls.fieldsAt("world", new Vec2((long) p[0], (long) p[1])).size();
        }));
        r.put("side", bench("Scoreboard: Fläche+Portale beider Fraktionen", i -> {
            double s = 0;
            for (Faction f : Faction.playable()) {
                s += ls.fields().stream().filter(x -> x.faction() == f).mapToDouble(x -> ls.triangle(x).map(Triangle::area).orElse(0.0)).sum();
                s += portals.all().stream().filter(p -> p.owner() == f).count();
                s += ls.influence(f);
            }
            sink += (long) s;
        }));
        r.put("links", bench("Link-Partikel: Abstand zu allen Links je Spieler", i -> {
            double[] p = pos[i & 1023];
            int near = 0;
            for (Link l : ls.links()) {
                var x = portals.get(l.from()); var y = portals.get(l.to());
                if (x.isEmpty() || y.isEmpty() || !x.get().core().world().equals("world")) continue;
                double ax = x.get().core().x(), az = x.get().core().z();
                double dx = y.get().core().x() - ax, dz = y.get().core().z() - az, len = Math.hypot(dx, dz);
                double t = Math.clamp(((p[0] - ax) * dx + (p[1] - az) * dz) / (len * len), 0, 1);
                if (Math.hypot(ax + t * dx - p[0], az + t * dz - p[1]) <= 64) near++;
            }
            sink += near;
        }));
        r.put("cross", bench("Link ziehen: Kreuzungsprüfung über alle Links", i -> {
            Segment s = new Segment(new Vec2(-5000, -5000), new Vec2(-4000, -4500 + (i & 7)));
            int c = 0;
            for (Link l : ls.links()) { var o = ls.segment(l); if (o.isPresent() && Geometry.crosses(s, o.get())) c++; }
            sink += c;
        }));
        // Hochrechnung auf 20 Spieler pro Sekunde (Server hat 1000 ms je Sekunde)
        double perSec = 20 * 10 * r.get("scan512") + 20 * 10 * r.get("lab512") // Scanner höchstens 10×/s je Spieler
                + 20 * 1 * r.get("bar") + 1 * r.get("side") + 20 * 2 * r.get("links");
        System.out.printf(Locale.ROOT, "%nSchlimmster Fall 20 Spieler, alle mit Scanner in Bewegung (512): %.1f ms je Sekunde = %.1f %% der Serverzeit%n",
                perSec / 1000, perSec / 1000 / 10);
        double typical = 20 * 2 * (r.get("scan256") + r.get("lab256")) + 20 * r.get("bar") + r.get("side") + 20 * 2 * r.get("links");
        System.out.printf(Locale.ROOT, "Typisch (Scanner ~2×/s neu, 256): %.1f ms je Sekunde = %.2f %%%n", typical / 1000, typical / 1000 / 10);
        System.out.println("(sink " + (sink & 1) + ")");
    }
}
