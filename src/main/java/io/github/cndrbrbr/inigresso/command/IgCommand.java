package io.github.cndrbrbr.inigresso.command;

import io.github.cndrbrbr.inigresso.InigressoPlugin;
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
            new Sub("fraktion", false, "<lumen|waechter>", "Fraktion wählen (einmalig)", 2),
            new Sub("info", false, "", "Eigene Stufe, AP und Fraktion", 2),
            new Sub("portal", false, "", "Infos zum nächsten Portal", 3),
            new Sub("vorschlagen", false, "<Name>", "Portal vorschlagen", 3),
            new Sub("link", false, "<Portal>", "Link vom aktuellen Portal ziehen", 6),
            new Sub("aufladen", false, "<Portal>", "Portal per Schlüssel aufladen", 5),
            new Sub("inventar", false, "", "Spiel-Inventar öffnen", 4),
            new Sub("stand", false, "", "Einfluss beider Fraktionen", 7),
            new Sub("zyklus", false, "[alt]", "Ergebnis laufender/vergangener Zyklen", 7),
            new Sub("vorschlaege", true, "", "Offene Vorschläge anzeigen", 3),
            new Sub("vorschlag", true, "tp <id>", "Zum Vorschlag teleportieren", 3),
            new Sub("annehmen", true, "<id>", "Vorschlag annehmen", 3),
            new Sub("ablehnen", true, "<id> <Grund>", "Vorschlag ablehnen", 3),
            new Sub("admin", true, "<portal|fraktion|geben> …", "Portale, Fraktionen, Gegenstände verwalten", 3),
            new Sub("reload", true, "", "Konfiguration neu laden", 0));

    private final InigressoPlugin plugin;

    public IgCommand(InigressoPlugin plugin) {
        this.plugin = plugin;
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
            default -> sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + sub.name
                    + " ist noch nicht fertig (kommt in Phase " + sub.phase + ").");
        }
        return true;
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
            for (String f : List.of("lumen", "waechter")) {
                if (f.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(f);
                }
            }
        }
        return out;
    }
}
