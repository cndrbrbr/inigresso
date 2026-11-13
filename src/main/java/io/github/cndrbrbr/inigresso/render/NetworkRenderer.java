package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.geo.Vec2;
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
import java.util.function.DoubleSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/** Partikelstrahlen für Links und Bossbar unter Feldern (architecture.md §5). */
public final class NetworkRenderer {

    /** Sichtweite der Link-Partikel um einen Spieler. */
    static final double VIEW = 64;
    static final double STEP = 1.5;

    private final LinkService links;
    private final PortalService portals;
    private final DoubleSupplier height;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    /** @param height Höhe des Strahls über dem Kernblock (config link.beam-height) */
    public NetworkRenderer(LinkService links, PortalService portals, DoubleSupplier height) {
        this.links = links;
        this.portals = portals;
        this.height = height;
    }

    private static Color color(Faction f) {
        return f == Faction.LUMEN ? Color.LIME : Color.fromRGB(0x3c, 0x6e, 0xff);
    }

    /** Alle 10 Ticks: Partikel nur auf dem Abschnitt, der nah am Spieler liegt, nur für diesen Spieler. */
    public void drawLinks() {
        double h = height.getAsDouble();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            String world = loc.getWorld().getName();
            for (Link l : links.links()) {
                Optional<Portal> a = portals.get(l.from());
                Optional<Portal> b = portals.get(l.to());
                if (a.isEmpty() || b.isEmpty() || !a.get().core().world().equals(world)) {
                    continue;
                }
                draw(player, loc, a.get(), b.get(), h, new Particle.DustOptions(color(l.faction()), 1.2f));
            }
        }
    }

    private static void draw(Player player, Location loc, Portal a, Portal b, double h, Particle.DustOptions dust) {
        double ax = a.core().x() + 0.5;
        double az = a.core().z() + 0.5;
        double ay = a.core().y() + h;
        double dx = b.core().x() + 0.5 - ax;
        double dz = b.core().z() + 0.5 - az;
        double dy = b.core().y() + h - ay;
        double len = Math.hypot(dx, dz);
        if (len < 1) {
            return;
        }
        // Parameter t des nächsten Punkts zum Spieler, dann nur ±VIEW darum zeichnen.
        double t = ((loc.getX() - ax) * dx + (loc.getZ() - az) * dz) / (len * len);
        t = Math.clamp(t, 0, 1);
        double px = ax + t * dx;
        double pz = az + t * dz;
        if (Math.hypot(px - loc.getX(), pz - loc.getZ()) > VIEW) {
            return;
        }
        double span = VIEW / len;
        double from = Math.max(0, t - span);
        double to = Math.min(1, t + span);
        double stepT = STEP / len;
        for (double s = from; s <= to; s += stepT) {
            player.spawnParticle(Particle.DUST, ax + s * dx, ay + s * dy, az + s * dz, 1, 0, 0, 0, 0, dust);
        }
    }

    /** Alle 20 Ticks: Bossbar, wenn der Spieler unter einem Feld steht. */
    public void updateBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            List<Field> over = links.fieldsAt(loc.getWorld().getName(), new Vec2(loc.getBlockX(), loc.getBlockZ()));
            BossBar bar = bars.get(player.getUniqueId());
            if (over.isEmpty()) {
                if (bar != null) {
                    bar.removeAll();
                    bars.remove(player.getUniqueId());
                }
                continue;
            }
            long lumen = over.stream().filter(f -> f.faction() == Faction.LUMEN).count();
            long waechter = over.size() - lumen;
            Faction top = lumen >= waechter ? Faction.LUMEN : Faction.WAECHTER;
            long influence = over.stream().filter(f -> f.faction() == top).mapToLong(Field::influence).sum();
            String title = top.color() + "Feld der " + top.displayName()
                    + (over.size() > 1 ? " (" + over.size() + " Schichten)" : "") + " · " + influence + " Einfluss";
            if (bar == null) {
                bar = Bukkit.createBossBar(title, BarColor.WHITE, BarStyle.SOLID);
                bar.addPlayer(player);
                bars.put(player.getUniqueId(), bar);
            }
            bar.setTitle(title);
            bar.setColor(top == Faction.LUMEN ? BarColor.GREEN : BarColor.BLUE);
            bar.setProgress(1.0);
        }
        bars.keySet().removeIf(id -> {
            if (Bukkit.getPlayer(id) == null) {
                bars.get(id).removeAll();
                return true;
            }
            return false;
        });
    }

    public void removeAll() {
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }
}
