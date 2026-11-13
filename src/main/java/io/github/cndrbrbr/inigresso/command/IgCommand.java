package io.github.cndrbrbr.inigresso.command;

import io.github.cndrbrbr.inigresso.InigressoPlugin;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.service.FactionService;
import io.github.cndrbrbr.inigresso.service.PlayerProgressService;
import java.util.Optional;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * /ig mit allen Unterbefehlen aus rules.md §10. Noch nicht umgesetzte Befehle melden,
 * in welcher Phase von implement.md sie kommen.
 */
public final class IgCommand implements TabExecutor {

    private record Sub(String name, boolean admin, String usage, String help, int phase) {
    }

    private static final List<Sub> SUBS = List.of(
            new Sub("hilfe", false, "", "Diese Hilfe", 0),
            new Sub("fraktion", false, "[lumen|waechter]", "Fraktion wählen (einmalig)", 2),
            new Sub("info", false, "", "Eigene Stufe, AP und Fraktion", 2),
            new Sub("portal", false, "", "Infos zum nächsten Portal", 3),
            new Sub("vorschlagen", false, "<Name>", "Portal vorschlagen", 3),
            new Sub("link", false, "<Portal>", "Link vom aktuellen Portal ziehen", 6),
            new Sub("aufladen", false, "<Portal>", "Portal per Schlüssel aufladen", 5),
            new Sub("inventar", false, "", "Übersicht deiner Spielgegenstände", 4),
            new Sub("stand", false, "", "Einfluss beider Fraktionen", 7),
            new Sub("zyklus", false, "[alt]", "Ergebnis laufender/vergangener Zyklen", 7),
            new Sub("vorschlaege", true, "", "Offene Vorschläge anzeigen", 3),
            new Sub("vorschlag", true, "tp <id>", "Zum Vorschlag teleportieren", 3),
            new Sub("annehmen", true, "<id>", "Vorschlag annehmen", 3),
            new Sub("ablehnen", true, "<id> <Grund>", "Vorschlag ablehnen", 3),
            new Sub("admin", true, "<fraktion|ap|portal|geben> …", "Portale, Fraktionen, Gegenstände verwalten", 3),
            new Sub("reload", true, "", "Konfiguration neu laden", 0));

    private final InigressoPlugin plugin;
    private final PortalCommands portalCommands;

    public IgCommand(InigressoPlugin plugin) {
        this.plugin = plugin;
        this.portalCommands = new PortalCommands(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = args.length == 0 ? "hilfe" : args[0].toLowerCase(Locale.ROOT);
        Sub sub = SUBS.stream().filter(s -> s.name.equals(name)).findFirst().orElse(null);
        if (sub == null) {
            sender.sendMessage(ChatColor.RED + "Unbekannter Unterbefehl. " + ChatColor.GRAY + "/" + label + " hilfe");
            return true;
        }
        if (sub.admin && !sender.hasPermission("inigresso.admin")) {
            sender.sendMessage(ChatColor.RED + "Dafür brauchst du Admin-Rechte.");
            return true;
        }
        switch (sub.name) {
            case "hilfe" -> help(sender, label);
            case "reload" -> {
                plugin.reloadSettings();
                sender.sendMessage(ChatColor.GREEN + "Inigresso-Konfiguration neu geladen.");
            }
            case "fraktion" -> faction(sender, label, args);
            case "info" -> info(sender, args);
            case "admin" -> admin(sender, label, args);
            case "vorschlagen" -> portalCommands.propose(sender, label, args);
            case "vorschlaege" -> portalCommands.listProposals(sender);
            case "vorschlag" -> portalCommands.teleportToProposal(sender, label, args);
            case "annehmen" -> portalCommands.accept(sender, label, args);
            case "ablehnen" -> portalCommands.reject(sender, label, args);
            case "portal" -> portalCommands.portalInfo(sender);
            case "inventar" -> inventory(sender);
            case "aufladen" -> recharge(sender, label, args);
            default -> sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + sub.name
                    + " ist noch nicht fertig (kommt in Phase " + sub.phase + ").");
        }
        return true;
    }

    private Optional<PlayerProfile> self(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur für Spieler.");
            return Optional.empty();
        }
        return plugin.profiles().get(player.getUniqueId());
    }

    private void faction(CommandSender sender, String label, String[] args) {
        Optional<PlayerProfile> me = self(sender);
        if (me.isEmpty()) {
            return;
        }
        PlayerProfile p = me.get();
        FactionService factions = plugin.factions();
        if (args.length < 2) {
            if (p.hasFaction()) {
                sender.sendMessage(ChatColor.GRAY + "Deine Fraktion: " + p.faction().colored());
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Wähle mit /" + label + " fraktion <lumen|waechter>. Die Wahl gilt dauerhaft.");
            }
            for (Faction f : Faction.playable()) {
                sender.sendMessage("  " + f.colored() + ChatColor.GRAY + ": " + plugin.profiles().count(f) + " Spieler");
            }
            return;
        }
        Optional<Faction> f = Faction.parsePlayable(args[1]);
        if (f.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unbekannte Fraktion. Möglich: lumen, waechter");
            return;
        }
        boolean crowded = factions.isOverpopulated(f.get());
        if (factions.choose(p, f.get()) == FactionService.ChooseResult.ALREADY_CHOSEN) {
            sender.sendMessage(ChatColor.RED + "Du gehörst schon zu " + p.faction().colored() + ChatColor.RED
                    + ". Ein Wechsel geht nur über einen Admin.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Willkommen bei " + f.get().colored() + ChatColor.GOLD + "!");
        if (crowded) {
            sender.sendMessage(ChatColor.YELLOW + "Hinweis: " + f.get().colored() + ChatColor.YELLOW
                    + " hat schon deutlich mehr Spieler als " + f.get().opponent().colored() + ChatColor.YELLOW + ".");
        }
    }

    private void info(CommandSender sender, String[] args) {
        Optional<PlayerProfile> target;
        if (args.length >= 2 && sender.hasPermission("inigresso.admin")) {
            target = plugin.profiles().byName(args[1]);
            if (target.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Spieler " + args[1] + " ist unbekannt.");
                return;
            }
        } else {
            target = self(sender);
            if (target.isEmpty()) {
                return;
            }
        }
        PlayerProfile p = target.get();
        long ap = p.ap();
        long next = PlayerProgressService.apToNextLevel(ap);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + p.lastName());
        sender.sendMessage(ChatColor.GRAY + "Fraktion: " + (p.hasFaction() ? p.faction().colored() : ChatColor.WHITE + "keine"));
        sender.sendMessage(ChatColor.GRAY + "Stufe: " + ChatColor.WHITE + PlayerProgressService.levelFor(ap)
                + ChatColor.GRAY + "  AP: " + ChatColor.WHITE + String.format("%,d", ap)
                + (next >= 0 ? ChatColor.GRAY + " (noch " + String.format("%,d", next) + " bis zur nächsten Stufe)" : ""));
        sender.sendMessage(ChatColor.GRAY + "Spiel-Inventar: " + ChatColor.WHITE + "0 / " + plugin.settings().playerMaxItems());
    }

    private void admin(CommandSender sender, String label, String[] args) {
        String what = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (what) {
            case "fraktion" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " admin fraktion <spieler> <lumen|waechter>");
                    return;
                }
                Optional<PlayerProfile> p = plugin.profiles().byName(args[2]);
                Optional<Faction> f = Faction.parsePlayable(args[3]);
                if (p.isEmpty() || f.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + (p.isEmpty() ? "Spieler unbekannt." : "Unbekannte Fraktion."));
                    return;
                }
                if (!plugin.factions().adminSet(p.get(), f.get())) {
                    sender.sendMessage(ChatColor.YELLOW + p.get().lastName() + " ist schon bei " + f.get().colored());
                    return;
                }
                plugin.audit(sender.getName(), "fraktion", p.get().lastName() + " -> " + f.get().name());
                sender.sendMessage(ChatColor.GREEN + p.get().lastName() + " ist jetzt bei " + f.get().colored()
                        + ChatColor.GREEN + ". Spielgegenstände wurden entfernt.");
                Player online = plugin.getServer().getPlayer(p.get().uuid());
                if (online != null) {
                    online.sendMessage(ChatColor.GOLD + "Ein Admin hat dich zu " + f.get().colored()
                            + ChatColor.GOLD + " verschoben.");
                }
            }
            case "ap" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " admin ap <spieler> <+/-menge>");
                    return;
                }
                Optional<PlayerProfile> p = plugin.profiles().byName(args[2]);
                long amount;
                try {
                    amount = Long.parseLong(args[3].replace("+", ""));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Keine Zahl: " + args[3]);
                    return;
                }
                if (p.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "Spieler unbekannt.");
                    return;
                }
                plugin.progress().add(p.get(), amount);
                plugin.audit(sender.getName(), "ap", p.get().lastName() + " " + amount);
                sender.sendMessage(ChatColor.GREEN + p.get().lastName() + " hat jetzt " + String.format("%,d", p.get().ap())
                        + " AP (Stufe " + PlayerProgressService.levelFor(p.get().ap()) + ").");
            }
            case "portal" -> portalCommands.adminPortal(sender, label, args);
            case "geben" -> give(sender, label, args);
            default -> sender.sendMessage(ChatColor.RED + "/" + label + " admin <fraktion|ap|portal|geben> …");
        }
    }

    private void inventory(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur für Spieler.");
            return;
        }
        var inv = plugin.gameInventory();
        var summary = inv.summary(player);
        sender.sendMessage(ChatColor.GOLD + "Spielgegenstände: " + ChatColor.WHITE + inv.count(player)
                + ChatColor.GRAY + " / " + inv.max());
        if (summary.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Keine. Hacke ein Portal (Rechtsklick auf den Leitstein).");
            return;
        }
        StringBuilder res = new StringBuilder();
        StringBuilder bursts = new StringBuilder();
        for (var en : summary.entrySet()) {
            var g = en.getKey();
            switch (g.type()) {
                case RESONATOR -> res.append(io.github.cndrbrbr.inigresso.item.ItemFactory.levelColor(g.level()))
                        .append("L").append(g.level()).append(ChatColor.GRAY).append("×").append(en.getValue()).append(" ");
                case BURST -> bursts.append(io.github.cndrbrbr.inigresso.item.ItemFactory.levelColor(g.level()))
                        .append("L").append(g.level()).append(ChatColor.GRAY).append("×").append(en.getValue()).append(" ");
                case KEY -> { }
            }
        }
        if (!res.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Resonatoren: " + res);
        }
        if (!bursts.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Bursts: " + bursts);
        }
        summary.forEach((g, n) -> {
            if (g.type() == io.github.cndrbrbr.inigresso.model.GameItem.Type.KEY) {
                String name = plugin.portals().get(g.portalId()).map(p -> p.name()).orElse("(gelöscht)");
                sender.sendMessage(ChatColor.AQUA + "Schlüssel " + ChatColor.WHITE + name + ChatColor.DARK_GRAY
                        + " #" + g.portalId() + ChatColor.GRAY + " ×" + n);
            }
        });
    }

    private void recharge(CommandSender sender, String label, String[] args) {
        Optional<PlayerProfile> me = self(sender);
        if (me.isEmpty()) {
            return;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/" + label + " aufladen <Portal|#id>" + ChatColor.GRAY
                    + " – braucht einen Schlüssel des Portals. Direkt am Portal: Schleichen + Rechtsklick auf den Kern.");
            return;
        }
        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        var portal = plugin.portals().find(query);
        if (portal.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Portal " + query + " gibt es nicht.");
            return;
        }
        if (!me.get().hasFaction() || !plugin.gameInventory().has(player,
                io.github.cndrbrbr.inigresso.model.GameItem.key(portal.get().id(), me.get().faction()))) {
            sender.sendMessage(ChatColor.RED + "Du brauchst einen Schlüssel von " + portal.get().name() + ".");
            return;
        }
        plugin.portalInteract().recharge(player, me.get(), portal.get());
    }

    private void give(CommandSender sender, String label, String[] args) {
        // admin geben <spieler> <resonator|burst|schluessel> <stufe|#portal> [anzahl]
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "/" + label + " admin geben <spieler> <resonator|burst|schluessel> <stufe|#portal> [anzahl]");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Spieler " + args[2] + " ist nicht online.");
            return;
        }
        PlayerProfile profile = plugin.profiles().get(target.getUniqueId()).orElseThrow();
        if (!profile.hasFaction()) {
            sender.sendMessage(ChatColor.RED + target.getName() + " hat noch keine Fraktion.");
            return;
        }
        var type = io.github.cndrbrbr.inigresso.model.GameItem.Type.parse(args[3]);
        if (type.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unbekannter Gegenstand: " + args[3]);
            return;
        }
        int amount = 1;
        if (args.length >= 6) {
            try {
                amount = Math.clamp(Integer.parseInt(args[5]), 1, 500);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Keine Zahl: " + args[5]);
                return;
            }
        }
        io.github.cndrbrbr.inigresso.model.GameItem item;
        if (type.get() == io.github.cndrbrbr.inigresso.model.GameItem.Type.KEY) {
            var portal = plugin.portals().find(args[4]);
            if (portal.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Portal " + args[4] + " gibt es nicht.");
                return;
            }
            item = io.github.cndrbrbr.inigresso.model.GameItem.key(portal.get().id(), profile.faction());
        } else {
            int level;
            try {
                level = Integer.parseInt(args[4].replaceFirst("(?i)^l", ""));
            } catch (NumberFormatException e) {
                level = 0;
            }
            if (level < 1 || level > 8) {
                sender.sendMessage(ChatColor.RED + "Stufe 1–8 angeben.");
                return;
            }
            item = new io.github.cndrbrbr.inigresso.model.GameItem(type.get(), level, 0, profile.faction());
        }
        int got = plugin.gameInventory().give(target, item, amount);
        plugin.audit(sender.getName(), "geben", target.getName() + " " + got + "x " + item);
        sender.sendMessage(ChatColor.GREEN + target.getName() + " hat " + got + " von " + amount + " erhalten.");
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Inigresso " + ChatColor.GRAY
                + plugin.getDescription().getVersion());
        boolean admin = sender.hasPermission("inigresso.admin");
        for (Sub s : SUBS) {
            if (s.admin && !admin) {
                continue;
            }
            String usage = s.usage.isEmpty() ? "" : " " + s.usage;
            sender.sendMessage((s.admin ? ChatColor.RED : ChatColor.AQUA) + "/" + label + " " + s.name + usage
                    + ChatColor.GRAY + " – " + s.help);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            boolean admin = sender.hasPermission("inigresso.admin");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Sub s : SUBS) {
                if ((!s.admin || admin) && s.name.startsWith(prefix)) {
                    out.add(s.name);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("fraktion")) {
            filter(out, List.of("lumen", "waechter"), args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("aufladen")) {
            filter(out, plugin.portals().all().stream().map(p -> "#" + p.id()).toList(), args[1]);
        } else if (sender.hasPermission("inigresso.admin")) {
            if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
                filter(out, onlineNames(), args[1]);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
                filter(out, List.of("fraktion", "ap", "portal", "geben"), args[1]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("portal")) {
                filter(out, List.of("liste", "umbenennen", "verschieben", "loeschen", "neutral", "tp"), args[2]);
            } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("portal")) {
                filter(out, plugin.portals().all().stream().map(p -> "#" + p.id()).toList(), args[3]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
                filter(out, onlineNames(), args[2]);
            } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("geben")) {
                filter(out, List.of("resonator", "burst", "schluessel"), args[3]);
            } else if (args.length == 2 && List.of("annehmen", "ablehnen").contains(args[0].toLowerCase(Locale.ROOT))) {
                filter(out, plugin.proposals().open().stream().map(p -> String.valueOf(p.id())).toList(), args[1]);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("vorschlag")) {
                filter(out, List.of("tp"), args[1]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("vorschlag")) {
                filter(out, plugin.proposals().open().stream().map(p -> String.valueOf(p.id())).toList(), args[2]);
            } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("fraktion")) {
                filter(out, List.of("lumen", "waechter"), args[3]);
            }
        }
        return out;
    }

    private List<String> onlineNames() {
        return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private static void filter(List<String> out, List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).forEach(out::add);
    }
}
