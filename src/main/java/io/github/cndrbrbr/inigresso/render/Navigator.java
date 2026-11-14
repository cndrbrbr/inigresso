package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.geo.Compass;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Navigationsziel je Spieler (/ig ziel): Pfeil und Entfernung in der Actionbar,
 * Markierung auf dem Scanner. Nur im Speicher, nach einem Neustart weg.
 */
public final class Navigator {

    /** Ab dieser Entfernung (Blöcke) gilt das Ziel als erreicht. */
    static final double ARRIVED = 5;

    private final PortalService portals;
    private final Map<UUID, Integer> targets = new HashMap<>();

    public Navigator(PortalService portals) {
        this.portals = portals;
    }

    public void set(Player p, Portal target) {
        targets.put(p.getUniqueId(), target.id());
    }

    public boolean clear(Player p) {
        return targets.remove(p.getUniqueId()) != null;
    }

    public Optional<Portal> target(UUID player) {
        Integer id = targets.get(player);
        return id == null ? Optional.empty() : portals.get(id);
    }

    /** Alle 10 Ticks: Actionbar aktualisieren, Ankunft melden, verwaiste Ziele löschen. */
    public void tick() {
        targets.entrySet().removeIf(en -> {
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null) {
                return true;
            }
            Optional<Portal> portal = portals.get(en.getValue());
            if (portal.isEmpty()) {
                p.sendMessage(ChatColor.YELLOW + "Dein Navigationsziel gibt es nicht mehr.");
                return true;
            }
            Portal t = portal.get();
            Location l = p.getLocation();
            if (!t.core().world().equals(l.getWorld().getName())) {
                actionBar(p, ChatColor.GRAY + "Ziel " + ChatColor.WHITE + t.name() + ChatColor.GRAY
                        + " liegt in " + t.core().world());
                return false;
            }
            double dx = t.core().x() + 0.5 - l.getX();
            double dz = t.core().z() + 0.5 - l.getZ();
            double dist = Math.hypot(dx, dz);
            if (dist <= ARRIVED) {
                actionBar(p, ChatColor.GREEN + "Ziel erreicht: " + ChatColor.WHITE + t.name());
                p.playSound(l, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.4f);
                return true;
            }
            actionBar(p, ChatColor.GOLD + "" + ChatColor.BOLD + Compass.arrow(l.getYaw(), dx, dz) + " "
                    + ChatColor.RESET + t.owner().color() + t.name() + ChatColor.GRAY + "  "
                    + ChatColor.WHITE + Math.round(dist) + " m " + ChatColor.GRAY + Compass.cardinal(dx, dz));
            return false;
        });
    }

    static void actionBar(Player p, String text) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(text));
    }
}
