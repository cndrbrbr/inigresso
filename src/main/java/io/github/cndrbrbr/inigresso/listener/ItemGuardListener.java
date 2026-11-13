package io.github.cndrbrbr.inigresso.listener;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.item.GameInventory;
import io.github.cndrbrbr.inigresso.item.ItemFactory;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.service.ProfileService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Spielgegenstände dürfen nicht wie normale Items wirken (Feuer legen, Tresore öffnen,
 * craften, Amboss) und droppen beim Tod nicht (rules.md §3).
 */
public final class ItemGuardListener implements Listener {

    private final ItemFactory items;
    private final GameInventory inventory;
    private final ProfileService profiles;
    /** Beim Tod einbehaltene Spielgegenstände bis zum Respawn. */
    private final Map<UUID, List<ItemStack>> kept = new HashMap<>();

    public ItemGuardListener(ItemFactory items, GameInventory inventory, ProfileService profiles, GameEventBus bus) {
        this.items = items;
        this.inventory = inventory;
        this.profiles = profiles;
        bus.subscribe(GameEvent.FactionChanged.class, e -> {
            Player p = Bukkit.getPlayer(e.profile().uuid());
            if (p != null && e.previous() != null) {
                inventory.purgeForeign(p, e.profile().faction());
            }
        });
    }

    /** Feuerkugel/Trial Key nicht vanilla benutzen. Portalkern-Klicks behandelt PortalInteractListener. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onUse(PlayerInteractEvent e) {
        if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)
                && items.isGameItem(e.getItem())) {
            e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent e) {
        if (items.isGameItem(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack s : e.getInventory().getMatrix()) {
            if (items.isGameItem(s)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        for (ItemStack s : e.getInventory().getContents()) {
            if (items.isGameItem(s)) {
                e.setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onSmithing(PrepareSmithingEvent e) {
        for (ItemStack s : e.getInventory().getContents()) {
            if (items.isGameItem(s)) {
                e.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        if (e.getKeepInventory()) {
            return;
        }
        List<ItemStack> mine = new ArrayList<>();
        for (Iterator<ItemStack> it = e.getDrops().iterator(); it.hasNext(); ) {
            ItemStack s = it.next();
            if (items.isGameItem(s)) {
                mine.add(s);
                it.remove();
            }
        }
        if (!mine.isEmpty()) {
            kept.computeIfAbsent(e.getEntity().getUniqueId(), k -> new ArrayList<>()).addAll(mine);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        List<ItemStack> mine = kept.remove(e.getPlayer().getUniqueId());
        if (mine != null) {
            e.getPlayer().getInventory().addItem(mine.toArray(new ItemStack[0]));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerProfile profile = profiles.get(p.getUniqueId()).orElse(null);
        if (profile != null && profile.hasFaction()) {
            int removed = inventory.purgeForeign(p, profile.faction());
            if (removed > 0) {
                p.sendMessage(ChatColor.YELLOW + "" + removed + " Spielgegenstände der anderen Fraktion wurden entfernt.");
            }
        }
    }
}
