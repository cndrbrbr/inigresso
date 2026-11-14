package io.github.cndrbrbr.inigresso.command;

import io.github.cndrbrbr.inigresso.InigressoPlugin;
import io.github.cndrbrbr.inigresso.geo.Compass;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.render.ScannerRenderer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ig karte, /ig ziel, /ig scanner. */
final class NavCommands {

    /** So viele Portale listet /ig karte. */
    static final int LIST = 10;

    private final InigressoPlugin plugin;

    NavCommands(InigressoPlugin plugin) {
        this.plugin = plugin;
    }

    private static Optional<Player> player(CommandSender sender) {
        if (sender instanceof Player p) {
            return Optional.of(p);
        }
        sender.sendMessage(ChatColor.RED + "Nur für Spieler.");
        return Optional.empty();
    }

    /** /ig karte [radius] – die nächsten Portale, anklickbar als Ziel. */
    void map(CommandSender sender, String label, String[] args) {
        Optional<Player> me = player(sender);
        if (me.isEmpty()) {
            return;
        }
        Player p = me.get();
        double radius = Double.POSITIVE_INFINITY;
        if (args.length >= 2) {
            try {
                radius = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "/" + label + " karte [radius]");
                return;
            }
        }
        Location l = p.getLocation();
        String world = l.getWorld().getName();
        double r = radius;
        List<Portal> near = plugin.portals().all().stream()
                .filter(x -> x.core().world().equals(world))
                .filter(x -> dist(l, x) <= r)
                .sorted(Comparator.comparingDouble(x -> dist(l, x)))
                .limit(LIST)
                .toList();
        if (near.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + (Double.isInfinite(radius)
                    ? "In dieser Welt gibt es keine Portale."
                    : "Keine Portale im Umkreis von " + (int) radius + " Blöcken."));
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Portale in der Nähe" + ChatColor.GRAY + " (Klick = Ziel setzen):");
        for (Portal x : near) {
            double dx = x.core().x() + 0.5 - l.getX();
            double dz = x.core().z() + 0.5 - l.getZ();
            String line = ChatColor.WHITE + String.format(Locale.ROOT, "%5d m ", Math.round(dist(l, x)))
                    + ChatColor.GRAY + pad(Compass.cardinal(dx, dz)) + " "
                    + x.owner().color() + "● " + ChatColor.WHITE + x.name()
                    + ChatColor.DARK_GRAY + " #" + x.id()
                    + (x.level() > 0 ? ChatColor.GRAY + " L" + x.level() + " " + x.resonatorCount() + "/8" : "");
            BaseComponent c = TextComponent.fromLegacy(line);
            c.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " ziel #" + x.id()));
            c.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text("Navigation zu " + x.name() + " starten")));
            p.spigot().sendMessage(c);
        }
        if (!plugin.scanner().has(p)) {
            sender.sendMessage(ChatColor.GRAY + "Alle Portale, Links und Felder zeigt der Scanner: /" + label + " scanner");
        }
    }

    private static String pad(String s) {
        return s.length() == 1 ? s + " " : s;
    }

    private static double dist(Location l, Portal p) {
        return Math.hypot(p.core().x() + 0.5 - l.getX(), p.core().z() + 0.5 - l.getZ());
    }

    /** /ig ziel <Portal|#id|aus> */
    void target(CommandSender sender, String label, String[] args) {
        Optional<Player> me = player(sender);
        if (me.isEmpty()) {
            return;
        }
        Player p = me.get();
        if (args.length < 2) {
            Optional<Portal> t = plugin.navigator().target(p.getUniqueId());
            sender.sendMessage(t.isPresent()
                    ? ChatColor.GRAY + "Ziel: " + ChatColor.WHITE + t.get().name() + ChatColor.GRAY
                            + ". Beenden mit /" + label + " ziel aus"
                    : ChatColor.RED + "/" + label + " ziel <Portal|#id|aus>" + ChatColor.GRAY
                            + " – Portale in der Nähe: /" + label + " karte");
            return;
        }
        String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (query.equalsIgnoreCase("aus")) {
            sender.sendMessage(plugin.navigator().clear(p)
                    ? ChatColor.GRAY + "Navigation beendet."
                    : ChatColor.GRAY + "Du hattest kein Ziel.");
            return;
        }
        Optional<Portal> portal = plugin.portals().find(query);
        if (portal.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Portal " + query + " gibt es nicht.");
            return;
        }
        plugin.navigator().set(p, portal.get());
        sender.sendMessage(ChatColor.GREEN + "Ziel gesetzt: " + ChatColor.WHITE + portal.get().name()
                + ChatColor.GRAY + " – Richtung steht über der Hotbar"
                + (plugin.scanner().has(p) ? " und auf dem Scanner." : "."));
    }

    /** /ig scanner [zoom <Blöcke>|norden|drehen] */
    void scanner(CommandSender sender, String label, String[] args) {
        Optional<Player> me = player(sender);
        if (me.isEmpty()) {
            return;
        }
        Player p = me.get();
        ScannerRenderer r = plugin.scannerRenderer();
        String what = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (what) {
            case "" -> {
                if (plugin.scanner().has(p)) {
                    sender.sendMessage(ChatColor.GRAY + "Du hast schon einen Scanner. Einstellungen: /" + label
                            + " scanner <zoom <Blöcke>|norden|drehen>");
                    return;
                }
                boolean fit = plugin.scanner().give(p);
                sender.sendMessage(ChatColor.GREEN + "Hier ist dein Scanner" + (fit ? "." : " (liegt vor dir, Inventar voll).")
                        + ChatColor.GRAY + " Nebenhand: beim Laufen sichtbar. Schleichen + Rechtsklick: Zoom.");
            }
            case "zoom" -> {
                int span = -1;
                if (args.length >= 3) {
                    try {
                        span = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {
                        // span bleibt -1
                    }
                }
                if (!r.setZoom(p, span)) {
                    sender.sendMessage(ChatColor.RED + "Zoom: " + zooms() + " Blöcke");
                    return;
                }
                sender.sendMessage(ChatColor.GRAY + "Scanner zeigt jetzt " + span + " Blöcke.");
            }
            case "norden" -> {
                r.setRotate(p, false);
                sender.sendMessage(ChatColor.GRAY + "Scanner: Norden ist oben.");
            }
            case "drehen" -> {
                r.setRotate(p, true);
                sender.sendMessage(ChatColor.GRAY + "Scanner: Blickrichtung ist oben.");
            }
            default -> sender.sendMessage(ChatColor.RED + "/" + label + " scanner [zoom <" + zooms() + ">|norden|drehen]");
        }
    }

    static String zooms() {
        return String.join("|", Arrays.stream(ScannerRenderer.ZOOMS).mapToObj(String::valueOf).toList());
    }
}
