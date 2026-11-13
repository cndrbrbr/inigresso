package io.github.cndrbrbr.inigresso.listener;

import io.github.cndrbrbr.inigresso.item.GameInventory;
import io.github.cndrbrbr.inigresso.item.ItemFactory;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.GameItem;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.HackService;
import io.github.cndrbrbr.inigresso.service.PortalService;
import io.github.cndrbrbr.inigresso.service.ProfileService;
import java.util.Map;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Rechtsklick auf einen Portalkern: Resonator setzen oder hacken (rules.md §2.3, §4). */
public final class PortalInteractListener implements Listener {

    private final PortalService portals;
    private final HackService hacks;
    private final ProfileService profiles;
    private final ItemFactory items;
    private final GameInventory inventory;

    public PortalInteractListener(PortalService portals, HackService hacks, ProfileService profiles,
                                  ItemFactory items, GameInventory inventory) {
        this.portals = portals;
        this.hacks = hacks;
        this.profiles = profiles;
        this.items = items;
        this.inventory = inventory;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) {
            return;
        }
        Block b = e.getClickedBlock();
        BlockPos pos = new BlockPos(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        Optional<Portal> portal = portals.protecting(pos).filter(p -> p.core().equals(pos));
        if (portal.isEmpty()) {
            return;
        }
        // Leitstein-Verhalten (Kompass verbinden) und Platzieren immer unterbinden.
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = e.getPlayer();
        PlayerProfile me = profiles.get(player.getUniqueId()).orElseThrow();
        if (!me.hasFaction()) {
            player.sendMessage(ChatColor.YELLOW + "Wähle zuerst eine Fraktion: /ig fraktion lumen|waechter");
            return;
        }
        Optional<GameItem> held = items.read(player.getInventory().getItemInMainHand());
        if (held.isPresent() && held.get().faction() != me.faction()) {
            player.sendMessage(ChatColor.RED + "Dieser Gegenstand gehört zur anderen Fraktion.");
            return;
        }
        if (held.isPresent() && held.get().type() == GameItem.Type.RESONATOR) {
            deploy(player, me, portal.get(), held.get().level());
        } else if (held.isPresent() && held.get().type() == GameItem.Type.BURST) {
            player.sendMessage(ChatColor.YELLOW + "Bursts wirken per Rechtsklick in die Luft (kommt in Phase 5).");
        } else {
            hack(player, me, portal.get());
        }
    }

    private void deploy(Player player, PlayerProfile me, Portal p, int level) {
        PortalService.DeployResult r = portals.deploy(me, p, level);
        if (!r.ok()) {
            player.sendMessage(ChatColor.RED + switch (r.outcome()) {
                case ENEMY_PORTAL -> "Das Portal gehört " + p.owner().colored() + ChatColor.RED
                        + ". Zerstöre erst seine Resonatoren.";
                case LEVEL_TOO_HIGH -> "Resonator L" + level + " ist zu stark für deine Stufe.";
                case LIMIT_REACHED -> "Du hast hier schon " + Portal.perPlayerLimit(level) + "× L" + level
                        + " gesetzt – mehr geht nicht.";
                case NOTHING_TO_UPGRADE -> "Alle Plätze sind belegt und mindestens L" + level + ".";
                default -> "Geht nicht.";
            });
            return;
        }
        inventory.consumeMainHand(player);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        String slot = Portal.SLOT_NAMES[r.slot()];
        switch (r.outcome()) {
            case CAPTURED -> {
                player.sendMessage(ChatColor.GREEN + "Du hast " + ChatColor.WHITE + p.name() + ChatColor.GREEN
                        + " für " + me.faction().colored() + ChatColor.GREEN + " erobert!");
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        player.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0.3);
            }
            case UPGRADED -> player.sendMessage(ChatColor.GREEN + "Resonator " + slot + " auf L" + level + " aufgerüstet.");
            default -> player.sendMessage(ChatColor.GREEN + "Resonator L" + level + " auf Platz " + slot + " gesetzt ("
                    + p.resonatorCount() + "/8).");
        }
    }

    private void hack(Player player, PlayerProfile me, Portal p) {
        HackService.Result r = hacks.hack(me, p);
        if (!r.ok()) {
            long s = (r.waitMs() + 999) / 1000;
            String wait = s >= 60 ? (s / 60) + " min " + (s % 60) + " s" : s + " s";
            player.sendMessage(ChatColor.RED + (r.problem() == HackService.Problem.COOLDOWN
                    ? "Das Portal muss sich noch erholen: " + wait
                    : "Du hast dieses Portal zu oft gehackt. Wieder möglich in " + wait));
            return;
        }
        int got = inventory.give(player, r.loot());
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);
        Map<GameItem, Integer> summary = new java.util.LinkedHashMap<>();
        r.loot().subList(0, got).forEach(g -> summary.merge(g, 1, Integer::sum));
        StringBuilder sb = new StringBuilder(ChatColor.AQUA + "Gehackt: ");
        if (summary.isEmpty()) {
            sb.append(ChatColor.GRAY).append("nichts");
        }
        boolean first = true;
        for (Map.Entry<GameItem, Integer> en : summary.entrySet()) {
            if (!first) {
                sb.append(ChatColor.GRAY).append(", ");
            }
            first = false;
            GameItem g = en.getKey();
            sb.append(ChatColor.WHITE).append(en.getValue()).append("× ");
            sb.append(g.type() == GameItem.Type.KEY ? ChatColor.AQUA + "Schlüssel"
                    : ItemFactory.levelColor(g.level()) + g.type().displayName() + " L" + g.level());
        }
        player.sendMessage(sb.toString());
        if (got < r.loot().size()) {
            player.sendMessage(ChatColor.YELLOW + "Dein Inventar ist voll (" + inventory.max()
                    + " Spielgegenstände oder kein Platz) – " + (r.loot().size() - got) + " Gegenstände verfallen.");
        }
    }
}
