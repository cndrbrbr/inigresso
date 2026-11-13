package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.util.HashMap;
import java.util.Map;
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

    public PortalRenderer(PortalService portals) {
        this.portals = portals;
    }

    public void bind(GameEventBus bus) {
        bus.subscribe(GameEvent.PortalChanged.class, e -> {
            if (e.moved()) {
                clearCore(e.previousCore());
            }
            placeCore(e.portal());
            updateLabel(e.portal());
        });
        bus.subscribe(GameEvent.PortalRemoved.class, e -> {
            removeLabel(e.portal().id());
            clearCore(e.portal().core());
        });
    }

    /** Beim Start für alle schon geladenen Chunks. */
    public void showLoaded() {
        for (Portal p : portals.all()) {
            World w = Bukkit.getWorld(p.core().world());
            if (w != null && w.isChunkLoaded(p.core().x() >> 4, p.core().z() >> 4)) {
                updateLabel(p);
            }
        }
    }

    public void removeAll() {
        labels.values().forEach(Entity::remove);
        labels.clear();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk c = event.getChunk();
        for (Entity e : c.getEntities()) {
            if (e.getScoreboardTags().contains(TAG) && !labels.containsValue(e)) {
                e.remove();
            }
        }
        for (Portal p : portals.coresInChunk(c.getWorld().getName(), c.getX(), c.getZ())) {
            updateLabel(p);
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
        sb.append('\n').append(ChatColor.DARK_GRAY).append('#').append(p.id());
        return sb.toString();
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
