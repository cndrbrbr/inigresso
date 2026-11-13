package io.github.cndrbrbr.inigresso.listener;

import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/** Schutzbereich um Portalkerne (rules.md §2.2, architecture.md §6). */
public final class ProtectionListener implements Listener {

    static final String BYPASS = "inigresso.bypass";

    private final PortalService portals;

    public ProtectionListener(PortalService portals) {
        this.portals = portals;
    }

    private boolean isProtected(Block b) {
        return portals.protecting(new BlockPos(b.getWorld().getName(), b.getX(), b.getY(), b.getZ())).isPresent();
    }

    /** Kernblock oder der Block direkt darunter – für niemanden veränderbar, auch nicht mit Bypass. */
    private boolean isCore(Block b) {
        String w = b.getWorld().getName();
        BlockPos at = new BlockPos(w, b.getX(), b.getY(), b.getZ());
        BlockPos above = new BlockPos(w, b.getX(), b.getY() + 1, b.getZ());
        return portals.protecting(at).filter(p -> p.core().equals(at)).isPresent()
                || portals.protecting(above).filter(p -> p.core().equals(above)).isPresent();
    }

    private void guard(Player player, Block block, Cancellable event) {
        if (isCore(block)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Portalkerne lassen sich nicht abbauen."
                    + (player.hasPermission(BYPASS) ? " Entfernen: /ig admin portal loeschen" : ""));
            return;
        }
        if (isProtected(block) && !player.hasPermission(BYPASS)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Dieser Bereich gehört zu einem Portal und ist geschützt.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        guard(e.getPlayer(), e.getBlock(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        guard(e.getPlayer(), e.getBlock(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        guard(e.getPlayer(), e.getBlock(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        guard(e.getPlayer(), e.getBlock(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (anyProtected(e.getBlocks()) || anyProtected(e.getBlocks().stream()
                .map(b -> b.getRelative(e.getDirection())).toList()) || isProtected(e.getBlock().getRelative(e.getDirection()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (anyProtected(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (isProtected(e.getToBlock()) && !isProtected(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent e) {
        if (isProtected(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (isProtected(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (isProtected(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    private boolean anyProtected(List<Block> blocks) {
        for (Block b : blocks) {
            if (isProtected(b)) {
                return true;
            }
        }
        return false;
    }
}
