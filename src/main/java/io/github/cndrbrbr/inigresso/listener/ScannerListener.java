package io.github.cndrbrbr.inigresso.listener;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.item.Scanner;
import io.github.cndrbrbr.inigresso.render.Navigator;
import io.github.cndrbrbr.inigresso.render.ScannerRenderer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Scanner: Zoom per Schleichen + Rechtsklick, Ausgabe bei Fraktionswahl, Karten-ID beim Login. */
public final class ScannerListener implements Listener {

    private final Scanner scanner;
    private final ScannerRenderer renderer;
    private final Navigator navigator;
    private final org.bukkit.plugin.Plugin plugin;
    /** Hat der Spieler (UUID) eine Fraktion? Dann gehört ein Scanner ins Inventar. */
    private final java.util.function.Predicate<java.util.UUID> hasFaction;

    public ScannerListener(org.bukkit.plugin.Plugin plugin, Scanner scanner, ScannerRenderer renderer,
            Navigator navigator, GameEventBus bus, java.util.function.Predicate<java.util.UUID> hasFaction) {
        this.plugin = plugin;
        this.scanner = scanner;
        this.renderer = renderer;
        this.navigator = navigator;
        this.hasFaction = hasFaction;
        bus.subscribe(GameEvent.FactionChanged.class, e -> {
            Player p = Bukkit.getPlayer(e.profile().uuid());
            if (p != null && e.previous() == null && !scanner.has(p)) {
                scanner.give(p);
                p.sendMessage(ChatColor.GOLD + "Du hast einen " + ChatColor.WHITE + "Scanner" + ChatColor.GOLD
                        + " bekommen. " + ChatColor.GRAY + "Nimm ihn in die Nebenhand; Schleichen + Rechtsklick zoomt. "
                        + "Portale in der Nähe: /ig karte");
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!e.getPlayer().isSneaking() || !scanner.isScanner(e.getItem())) {
            return;
        }
        // Schleichen + Rechtsklick auf einen Portalkern heißt Aufladen, nicht Zoomen.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && e.getClickedBlock().getType() == org.bukkit.Material.LODESTONE) {
            return;
        }
        e.setCancelled(true);
        Player p = e.getPlayer();
        int span = renderer.cycleZoom(p);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.6f);
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacy(ChatColor.GOLD + "Scanner: " + ChatColor.WHITE + span + " Blöcke"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        scanner.fix(p);
        // Verlorene Scanner (Tod, volles Inventar, weggeworfen) ersetzen. Einen Tick später,
        // damit das Profil sicher geladen ist und gehaltene Tode erst respawnen.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !p.isDead() && hasFaction.test(p.getUniqueId()) && !scanner.has(p)) {
                scanner.give(p);
                p.sendMessage(ChatColor.GOLD + "Dein " + ChatColor.WHITE + "Scanner" + ChatColor.GOLD
                        + " fehlte und wurde ersetzt. " + ChatColor.GRAY + "Neuen holen: /ig scanner");
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        renderer.forget(e.getPlayer().getUniqueId());
        navigator.clear(e.getPlayer());
    }
}
