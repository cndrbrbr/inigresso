package io.github.cndrbrbr.inigresso.listener;

import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.AutoPortalPlanner;
import io.github.cndrbrbr.inigresso.service.PortalService;
import io.github.cndrbrbr.inigresso.store.AutoCellRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

/**
 * Setzt automatisch Portale an der Oberfläche, eins je Rasterzelle (rules.md §2.4).
 * Geprüft wird, wenn der Chunk mit dem Zielpunkt lädt. Gebaut wird nur auf natürlichem Boden,
 * damit keine Spielerbauten getroffen werden; sonst wird in der Nähe gesucht oder die Zelle
 * übersprungen. Jede Zelle wird nur einmal geprüft (Tabelle auto_cell).
 */
public final class AutoPortalListener implements Listener {

    /** Bodenblöcke, auf denen ein Kern stehen darf. */
    private static final Set<Material> GROUND = EnumSet.of(Material.GRAVEL, Material.SANDSTONE,
            Material.RED_SANDSTONE, Material.CLAY, Material.SNOW_BLOCK, Material.CALCITE, Material.PACKED_ICE);
    /** Natürliche Blöcke, die zusätzlich im Umfeld vorkommen dürfen (Bäume, Pflanzen). */
    private static final Set<Material> NATURE = EnumSet.of(Material.CACTUS, Material.BAMBOO, Material.SUGAR_CANE,
            Material.PUMPKIN, Material.MELON, Material.MUSHROOM_STEM, Material.BROWN_MUSHROOM_BLOCK,
            Material.RED_MUSHROOM_BLOCK, Material.BEE_NEST, Material.POWDER_SNOW, Material.MOSS_CARPET,
            Material.SWEET_BERRY_BUSH, Material.POINTED_DRIPSTONE);
    /** Suchversatz um den Zielpunkt, wenn er selbst ungeeignet ist. */
    private static final int SEARCH = 12;
    private static final int STEP = 4;

    private final Plugin plugin;
    private final PortalService portals;
    private final AutoCellRepository repo;
    private final Set<String> done;
    private final Supplier<AutoPortalPlanner> planner;
    private final IntSupplier protectRadius;
    private final Supplier<List<String>> worlds;
    private final Supplier<Boolean> enabled;
    private final Logger log;

    public AutoPortalListener(Plugin plugin, PortalService portals, AutoCellRepository repo, Set<String> done,
            Supplier<AutoPortalPlanner> planner, IntSupplier protectRadius, Supplier<List<String>> worlds,
            Supplier<Boolean> enabled) {
        this.plugin = plugin;
        this.portals = portals;
        this.repo = repo;
        this.done = done;
        this.planner = planner;
        this.protectRadius = protectRadius;
        this.worlds = worlds;
        this.enabled = enabled;
        this.log = plugin.getLogger();
    }

    /** Beim Start für schon geladene Chunks. */
    public void scanLoaded() {
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                check(w, c.getX(), c.getZ());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e) {
        World w = e.getWorld();
        int cx = e.getChunk().getX();
        int cz = e.getChunk().getZ();
        if (!enabled.get() || !worlds.get().contains(w.getName())) {
            return;
        }
        // Erst im nächsten Tick bauen: während des Ladeereignisses keine Blöcke ändern.
        // Auch die Nachbarchunks prüfen: ihr Zielpunkt konnte vielleicht erst jetzt bewertet werden.
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    check(w, cx + dx, cz + dz);
                }
            }
        });
    }

    private void check(World w, int cx, int cz) {
        if (!enabled.get() || !worlds.get().contains(w.getName()) || w.getEnvironment() != World.Environment.NORMAL
                || !w.isChunkLoaded(cx, cz)) {
            return;
        }
        AutoPortalPlanner p = planner.get();
        for (AutoPortalPlanner.Target t : p.targetsInChunk(w.getName(), cx, cz)) {
            if (done.contains(t.cell().key())) {
                continue;
            }
            Optional<BlockPos> spot = findSpot(w, t.x(), t.z(), p.spacing());
            if (spot.isEmpty() && !surroundingsLoaded(w, t.x(), t.z())) {
                continue; // später noch einmal versuchen, wenn die Nachbarchunks da sind
            }
            done.add(t.cell().key());
            if (spot.isEmpty()) {
                repo.mark(w.getName(), t.cell().gx(), t.cell().gz(), null);
                continue;
            }
            String name = p.name(t.cell(), n -> portals.byName(n).isPresent());
            Portal portal = portals.create(name, spot.get(), null);
            repo.mark(w.getName(), t.cell().gx(), t.cell().gz(), portal.id());
            log.info("Automatisches Portal #" + portal.id() + " „" + name + "“ bei " + spot.get().x() + " "
                    + spot.get().y() + " " + spot.get().z());
        }
    }

    private boolean surroundingsLoaded(World w, int x, int z) {
        int r = SEARCH + protectRadius.getAsInt();
        return w.isChunkLoaded((x - r) >> 4, (z - r) >> 4) && w.isChunkLoaded((x + r) >> 4, (z - r) >> 4)
                && w.isChunkLoaded((x - r) >> 4, (z + r) >> 4) && w.isChunkLoaded((x + r) >> 4, (z + r) >> 4);
    }

    /** Zielpunkt, sonst Punkte auf Ringen bis SEARCH Blöcke Abstand. */
    private Optional<BlockPos> findSpot(World w, int x, int z, int spacing) {
        double keepAway = Math.max(spacing / 2.0, 1);
        for (int r = 0; r <= SEARCH; r += STEP) {
            for (int dx = -r; dx <= r; dx += STEP) {
                for (int dz = -r; dz <= r; dz += STEP) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    Optional<BlockPos> pos = suitable(w, x + dx, z + dz);
                    if (pos.isPresent() && portals.nearest(pos.get(), keepAway).isEmpty()) {
                        return pos;
                    }
                    if (pos.isPresent()) {
                        return Optional.empty(); // schon ein Portal in der Nähe → Zelle fertig
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Kernposition, wenn Boden und Umgebung (Schutzradius) natürlich und eben genug sind. */
    private Optional<BlockPos> suitable(World w, int x, int z) {
        int pr = protectRadius.getAsInt();
        if (!loaded(w, x - pr, z - pr) || !loaded(w, x + pr, z + pr) || !loaded(w, x - pr, z + pr)
                || !loaded(w, x + pr, z - pr)) {
            return Optional.empty();
        }
        Block ground = top(w, x, z);
        if (!isGround(ground.getType())) {
            return Optional.empty();
        }
        Block core = ground.getRelative(0, 1, 0);
        if (!core.isPassable() || core.isLiquid()) {
            return Optional.empty();
        }
        int gy = ground.getY();
        for (int dx = -pr; dx <= pr; dx++) {
            for (int dz = -pr; dz <= pr; dz++) {
                Block b = top(w, x + dx, z + dz);
                Material m = b.getType();
                boolean natural = isGround(m) || Tag.LOGS.isTagged(m) || NATURE.contains(m);
                if (!natural) {
                    return Optional.empty();
                }
                // Steile Hänge und Klippen meiden (Bäume ausgenommen).
                if (!Tag.LOGS.isTagged(m) && !NATURE.contains(m) && Math.abs(b.getY() - gy) > 3) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new BlockPos(w.getName(), x, gy + 1, z));
    }

    private static boolean loaded(World w, int x, int z) {
        return w.isChunkLoaded(x >> 4, z >> 4);
    }

    private static Block top(World w, int x, int z) {
        Block b = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        // Schneeschicht zählt nicht als Boden.
        return b.getType() == Material.SNOW ? b.getRelative(0, -1, 0) : b;
    }

    private static boolean isGround(Material m) {
        return Tag.DIRT.isTagged(m) || Tag.SAND.isTagged(m) || Tag.BASE_STONE_OVERWORLD.isTagged(m)
                || Tag.TERRACOTTA.isTagged(m) || GROUND.contains(m) || m == Material.GRASS_BLOCK
                || m == Material.PODZOL || m == Material.MYCELIUM;
    }
}
