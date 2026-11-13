package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Resonator;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Kernblock (Leitstein) und schwebende Namensanzeige (architecture.md §5). Die Anzeigen sind
 * nicht persistent und werden beim Laden des Chunks neu erzeugt.
 */
public final class PortalRenderer implements Listener {

    /** Scoreboard-Tag zum Wiedererkennen eigener Anzeigen (z. B. Reste nach einem Absturz). */
    static final String TAG = "inigresso_portal";

    private final PortalService portals;
    private final Map<Integer, TextDisplay> labels = new HashMap<>();
    private final Map<Integer, BlockDisplay[]> resonators = new HashMap<>();
    private final IntSupplier resonatorRadius;
    private final Supplier<List<Material>> resonatorBlocks;

    public PortalRenderer(PortalService portals, IntSupplier resonatorRadius, Supplier<List<Material>> resonatorBlocks) {
        this.portals = portals;
        this.resonatorRadius = resonatorRadius;
        this.resonatorBlocks = resonatorBlocks;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.PortalChanged.class, e -> {
            if (e.moved()) {
                clearCore(e.previousCore());
            }
            placeCore(e.portal());
            updateLabel(e.portal());
            updateResonators(e.portal());
        });
        bus.subscribe(GameEvent.PortalRemoved.class, e -> {
            removeLabel(e.portal().id());
            removeResonators(e.portal().id());
            clearCore(e.portal().core());
        });
    }

    /** Beim Start für alle schon geladenen Chunks. */
    public void showLoaded() {
        for (Portal p : portals.all()) {
            World w = Bukkit.getWorld(p.core().world());
            if (w != null && w.isChunkLoaded(p.core().x() >> 4, p.core().z() >> 4)) {
                placeCore(p);
                updateLabel(p);
                updateResonators(p);
            }
        }
    }

    public void removeAll() {
        labels.values().forEach(Entity::remove);
        labels.clear();
        for (BlockDisplay[] arr : resonators.values()) {
            for (BlockDisplay d : arr) {
                if (d != null) {
                    d.remove();
                }
            }
        }
        resonators.clear();
    }

    /** Nach reload (Radius/Blöcke geändert): alles neu aufbauen. */
    public void refreshAll() {
        removeAll();
        showLoaded();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk c = event.getChunk();
        for (Entity e : c.getEntities()) {
            if (e.getScoreboardTags().contains(TAG) && !isTracked(e)) {
                e.remove();
            }
        }
        for (Portal p : portals.coresInChunk(c.getWorld().getName(), c.getX(), c.getZ())) {
            // Fehlender Kern (z. B. importierte Portale, WorldEdit) wird ergänzt.
            placeCore(p);
            updateLabel(p);
            updateResonators(p);
        }
    }

    private void placeCore(Portal p) {
        Block b = block(p.core());
        if (b != null && b.getType() != Material.LODESTONE) {
            b.setType(Material.LODESTONE, false);
        }
    }

    private void clearCore(BlockPos pos) {
        Block b = block(pos);
        if (b != null && b.getType() == Material.LODESTONE) {
            b.setType(Material.AIR, false);
        }
    }

    private void updateLabel(Portal p) {
        TextDisplay label = labels.get(p.id());
        Location at = labelLocation(p.core());
        if (at == null) {
            return;
        }
        if (label == null || !label.isValid()) {
            if (!at.getWorld().isChunkLoaded(p.core().x() >> 4, p.core().z() >> 4)) {
                return;
            }
            label = at.getWorld().spawn(at, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(TAG);
                d.setBillboard(Display.Billboard.CENTER);
                d.setSeeThrough(false);
                d.setShadowed(true);
                d.setViewRange(0.5f);
            });
            labels.put(p.id(), label);
        } else if (!label.getLocation().toVector().equals(at.toVector())) {
            label.teleport(at);
        }
        label.setText(text(p));
    }

    private boolean isTracked(Entity e) {
        if (labels.containsValue(e)) {
            return true;
        }
        for (BlockDisplay[] arr : resonators.values()) {
            for (BlockDisplay d : arr) {
                if (e.equals(d)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resonatoren als schwebende, halb große Blöcke im Ring um den Kern. Es werden keine
     * Weltblöcke verändert, damit das Bauwerk am Portal unbeschädigt bleibt.
     */
    private void updateResonators(Portal p) {
        World w = Bukkit.getWorld(p.core().world());
        if (w == null || !w.isChunkLoaded(p.core().x() >> 4, p.core().z() >> 4)) {
            return;
        }
        BlockDisplay[] arr = resonators.computeIfAbsent(p.id(), k -> new BlockDisplay[Portal.SLOTS]);
        List<Material> blocks = resonatorBlocks.get();
        for (int i = 0; i < Portal.SLOTS; i++) {
            Resonator r = p.resonator(i);
            BlockDisplay d = arr[i];
            if (r == null) {
                if (d != null) {
                    d.remove();
                    arr[i] = null;
                }
                continue;
            }
            int[] off = Portal.slotOffset(i, resonatorRadius.getAsInt());
            Location at = new Location(w, p.core().x() + off[0] + 0.25, p.core().y() + 1.2, p.core().z() + off[1] + 0.25);
            BlockData data = blocks.get(r.level() - 1).createBlockData();
            if (d == null || !d.isValid()) {
                d = w.spawn(at, BlockDisplay.class, bd -> {
                    bd.setPersistent(false);
                    bd.addScoreboardTag(TAG);
                    bd.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                            new Vector3f(0.5f, 0.5f, 0.5f), new AxisAngle4f()));
                });
                arr[i] = d;
            } else if (!d.getLocation().toVector().equals(at.toVector())) {
                d.teleport(at);
            }
            d.setBlock(data);
            d.setGlowing(true);
            d.setGlowColorOverride(p.owner() == Faction.LUMEN ? org.bukkit.Color.LIME : org.bukkit.Color.BLUE);
        }
    }

    private void removeResonators(int portalId) {
        BlockDisplay[] arr = resonators.remove(portalId);
        if (arr != null) {
            for (BlockDisplay d : arr) {
                if (d != null) {
                    d.remove();
                }
            }
        }
    }

    private void removeLabel(int portalId) {
        TextDisplay d = labels.remove(portalId);
        if (d != null) {
            d.remove();
        }
    }

    static String text(Portal p) {
        Faction f = p.owner();
        StringBuilder sb = new StringBuilder();
        sb.append(f.color()).append(ChatColor.BOLD).append(p.name()).append(ChatColor.RESET).append('\n');
        sb.append(f.color()).append(f.displayName());
        if (f != Faction.NEUTRAL) {
            String owner = p.capturedBy() == null ? null : Bukkit.getOfflinePlayer(p.capturedBy()).getName();
            if (owner != null) {
                sb.append(ChatColor.GRAY).append(" · ").append(owner);
            }
        }
        if (f != Faction.NEUTRAL) {
            sb.append('\n').append(ChatColor.WHITE).append("Stufe ").append(p.level())
                    .append(ChatColor.GRAY).append(" · ").append(p.resonatorCount()).append("/8 Resonatoren")
                    .append(ChatColor.GRAY).append(" · ").append(energyColor(p))
                    .append(io.github.cndrbrbr.inigresso.service.EnergyService.percent(p)).append(" %");
        }
        sb.append('\n').append(ChatColor.DARK_GRAY).append('#').append(p.id());
        return sb.toString();
    }

    private static ChatColor energyColor(Portal p) {
        int pct = io.github.cndrbrbr.inigresso.service.EnergyService.percent(p);
        return pct >= 60 ? ChatColor.GREEN : pct >= 30 ? ChatColor.YELLOW : ChatColor.RED;
    }

    private static Block block(BlockPos pos) {
        World w = Bukkit.getWorld(pos.world());
        return w == null ? null : w.getBlockAt(pos.x(), pos.y(), pos.z());
    }

    private static Location labelLocation(BlockPos pos) {
        World w = Bukkit.getWorld(pos.world());
        return w == null ? null : new Location(w, pos.x() + 0.5, pos.y() + 2.2, pos.z() + 0.5);
    }
}
