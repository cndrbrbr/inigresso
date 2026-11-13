package io.github.cndrbrbr.inigresso.command;

import io.github.cndrbrbr.inigresso.InigressoPlugin;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Proposal;
import io.github.cndrbrbr.inigresso.service.ProposalService;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Unterbefehle rund um Vorschläge und Portale (Phase 3). */
final class PortalCommands {

    private final InigressoPlugin plugin;

    PortalCommands(InigressoPlugin plugin) {
        this.plugin = plugin;
    }

    static BlockPos pos(Location l) {
        return new BlockPos(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    private static String name(java.util.UUID uuid) {
        if (uuid == null) {
            return "Admin";
        }
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n == null ? uuid.toString().substring(0, 8) : n;
    }

    void propose(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur für Spieler.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/" + label + " vorschlagen <Name des Bauwerks>");
            sender.sendMessage(ChatColor.GRAY + "Stell dich dabei genau dorthin, wo der Portalkern hin soll.");
            return;
        }
        PlayerProfile me = plugin.profiles().get(player.getUniqueId()).orElseThrow();
        ProposalService.Result r = plugin.proposals().propose(me, join(args, 1), pos(player.getLocation()));
        if (r.ok()) {
            Proposal p = r.proposal();
            sender.sendMessage(ChatColor.GREEN + "Vorschlag #" + p.id() + " „" + p.name()
                    + "“ eingereicht. Ein Admin prüft ihn bald.");
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("inigresso.admin") && admin != player) {
                    admin.sendMessage(ChatColor.GOLD + "[Inigresso] " + ChatColor.YELLOW + player.getName()
                            + " schlägt das Portal „" + p.name() + "“ vor (#" + p.id() + "). /ig vorschlaege");
                }
            }
            return;
        }
        sender.sendMessage(ChatColor.RED + problem(r));
    }

    static String problem(ProposalService.Result r) {
        return switch (r.problem()) {
            case NO_FACTION -> "Wähle zuerst eine Fraktion mit /ig fraktion.";
            case LEVEL_TOO_LOW -> "Vorschlagen geht erst ab Stufe " + r.detail() + ".";
            case TOO_MANY_OPEN -> "Du hast schon " + r.detail() + " offene Vorschläge. Warte, bis ein Admin sie prüft.";
            case WORLD_NOT_ALLOWED -> "In dieser Welt gibt es keine Portale.";
            case NAME_INVALID -> "Der Name muss " + ProposalService.MIN_NAME + " bis " + ProposalService.MAX_NAME
                    + " Zeichen lang sein und darf nicht nur aus Ziffern bestehen.";
            case NAME_TAKEN -> "Den Namen „" + r.detail() + "“ gibt es schon.";
            case TOO_CLOSE_TO_PORTAL -> "Zu nah am Portal „" + r.detail() + "“. Halte mehr Abstand.";
            case TOO_CLOSE_TO_PROPOSAL -> "Zu nah am offenen Vorschlag „" + r.detail() + "“.";
        };
    }

    void listProposals(CommandSender sender) {
        List<Proposal> open = plugin.proposals().open();
        if (open.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Keine offenen Vorschläge.");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY);
        sender.sendMessage(ChatColor.GOLD + "Offene Vorschläge (" + open.size() + "):");
        for (Proposal p : open) {
            sender.sendMessage(ChatColor.AQUA + "#" + p.id() + " " + ChatColor.WHITE + p.name() + ChatColor.GRAY
                    + " von " + name(p.proposer()) + ", " + fmt.format(new Date(p.createdAt())) + ", " + p.pos());
        }
        sender.sendMessage(ChatColor.GRAY + "/ig vorschlag tp <id> · /ig annehmen <id> · /ig ablehnen <id> <Grund>");
    }

    private Optional<Proposal> openProposal(CommandSender sender, String idArg) {
        Optional<Proposal> p;
        try {
            p = plugin.proposals().get(Integer.parseInt(idArg.replace("#", "")));
        } catch (NumberFormatException e) {
            p = Optional.empty();
        }
        if (p.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Vorschlag " + idArg + " gibt es nicht.");
            return Optional.empty();
        }
        if (!p.get().isOpen()) {
            sender.sendMessage(ChatColor.RED + "Vorschlag #" + p.get().id() + " ist schon "
                    + p.get().status().name().toLowerCase(Locale.ROOT) + ".");
            return Optional.empty();
        }
        return p;
    }

    void teleportToProposal(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur für Spieler.");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("tp")) {
            sender.sendMessage(ChatColor.RED + "/" + label + " vorschlag tp <id>");
            return;
        }
        Optional<Proposal> p = openProposal(sender, args[2]);
        if (p.isEmpty()) {
            return;
        }
        BlockPos b = p.get().pos();
        World w = Bukkit.getWorld(b.world());
        if (w == null) {
            sender.sendMessage(ChatColor.RED + "Welt " + b.world() + " ist nicht geladen.");
            return;
        }
        player.teleport(safeSpot(w, b.x(), b.y(), b.z() + 2));
        sender.sendMessage(ChatColor.GRAY + "Vorschlag #" + p.get().id() + " „" + p.get().name()
                + "“ – der Kern käme an die Stelle direkt vor dir.");
    }

    void accept(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/" + label + " annehmen <id>");
            return;
        }
        Optional<Proposal> p = openProposal(sender, args[1]);
        if (p.isEmpty()) {
            return;
        }
        ProposalService.Result r = plugin.proposals().accept(p.get(), sender.getName());
        if (!r.ok()) {
            sender.sendMessage(ChatColor.RED + problem(r) + " Lehne den Vorschlag ab oder verschiebe das andere Portal.");
            return;
        }
        plugin.audit(sender.getName(), "annehmen", "#" + p.get().id() + " " + p.get().name());
        sender.sendMessage(ChatColor.GREEN + "Portal „" + p.get().name() + "“ angelegt.");
    }

    void reject(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "/" + label + " ablehnen <id> <Grund>");
            return;
        }
        Optional<Proposal> p = openProposal(sender, args[1]);
        if (p.isEmpty()) {
            return;
        }
        String reason = join(args, 2);
        plugin.proposals().reject(p.get(), sender.getName(), reason);
        plugin.audit(sender.getName(), "ablehnen", "#" + p.get().id() + " " + p.get().name() + ": " + reason);
        sender.sendMessage(ChatColor.YELLOW + "Vorschlag #" + p.get().id() + " abgelehnt.");
    }

    void portalInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur für Spieler.");
            return;
        }
        Optional<Portal> near = plugin.portals().nearest(pos(player.getLocation()), 16);
        if (near.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Kein Portal im Umkreis von 16 Blöcken.");
            return;
        }
        Portal p = near.get();
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + p.name() + ChatColor.DARK_GRAY + " #" + p.id());
        sender.sendMessage(ChatColor.GRAY + "Fraktion: " + p.owner().colored());
        if (p.capturedBy() != null) {
            sender.sendMessage(ChatColor.GRAY + "Erobert von: " + ChatColor.WHITE + name(p.capturedBy()));
        }
        if (p.resonatorCount() > 0) {
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < Portal.SLOTS; i++) {
                var r = p.resonator(i);
                res.append(ChatColor.GRAY).append(Portal.SLOT_NAMES[i]).append(":")
                        .append(r == null ? ChatColor.DARK_GRAY + "–" : io.github.cndrbrbr.inigresso.item.ItemFactory
                                .levelColor(r.level()) + "L" + r.level() + ChatColor.GRAY + "("
                                + (100 * r.energy() / r.maxEnergy()) + "%)").append(" ");
            }
            sender.sendMessage(ChatColor.GRAY + "Stufe " + ChatColor.WHITE + p.level() + ChatColor.GRAY + ", Energie "
                    + ChatColor.WHITE + io.github.cndrbrbr.inigresso.service.EnergyService.percent(p) + " %"
                    + ChatColor.GRAY + ", Reichweite " + ChatColor.WHITE + plugin.links().range(p) + ChatColor.GRAY + " Blöcke");
            sender.sendMessage(res.toString());
        }
        var links = plugin.links().linksOf(p.id());
        if (!links.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Links: " + ChatColor.WHITE + links.stream()
                    .map(l -> plugin.portals().get(l.other(p.id())).map(Portal::name).orElse("?")
                            + (l.from() == p.id() ? " →" : " ←"))
                    .reduce((a, b) -> a + ", " + b).orElse(""));
        }
        sender.sendMessage(ChatColor.GRAY + "Vorgeschlagen von: " + ChatColor.WHITE + name(p.proposer()));
    }

    void adminPortal(CommandSender sender, String label, String[] args) {
        // args: admin portal <aktion> ...
        String action = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
        if (action.equals("liste")) {
            List<Portal> all = plugin.portals().all().stream()
                    .sorted((a, b) -> Integer.compare(a.id(), b.id())).toList();
            sender.sendMessage(ChatColor.GOLD + "Portale (" + all.size() + "):");
            for (Portal p : all) {
                sender.sendMessage(ChatColor.AQUA + "#" + p.id() + " " + p.owner().color() + p.name()
                        + ChatColor.GRAY + " " + p.core());
            }
            return;
        }
        if (action.isEmpty() || args.length < 4) {
            sender.sendMessage(ChatColor.RED + "/" + label + " admin portal <liste|umbenennen|verschieben|loeschen|neutral|tp> <#id|Name> …");
            return;
        }
        Optional<Portal> found = plugin.portals().find(args[3]);
        if (found.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Portal " + args[3] + " gibt es nicht.");
            return;
        }
        Portal p = found.get();
        switch (action) {
            case "umbenennen" -> {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " admin portal umbenennen <#id> <neuer Name>");
                    return;
                }
                String name = join(args, 4).strip();
                Optional<Portal> clash = plugin.portals().byName(name);
                if (name.length() < ProposalService.MIN_NAME || name.length() > ProposalService.MAX_NAME
                        || (clash.isPresent() && clash.get() != p)) {
                    sender.sendMessage(ChatColor.RED + "Name ungültig oder schon vergeben.");
                    return;
                }
                String old = p.name();
                plugin.portals().rename(p, name);
                plugin.audit(sender.getName(), "portal umbenennen", "#" + p.id() + " " + old + " -> " + name);
                sender.sendMessage(ChatColor.GREEN + "Umbenannt in „" + name + "“.");
            }
            case "verschieben" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Nur im Spiel: das Portal wird an deine Position verschoben.");
                    return;
                }
                BlockPos to = pos(player.getLocation());
                Optional<Portal> near = plugin.portals().nearest(to, plugin.settings().portalMinDistance() - 0.0001)
                        .filter(o -> o != p);
                if (near.isPresent()) {
                    sender.sendMessage(ChatColor.RED + "Zu nah an Portal „" + near.get().name() + "“.");
                    return;
                }
                BlockPos from = p.core();
                plugin.portals().move(p, to);
                plugin.audit(sender.getName(), "portal verschieben", "#" + p.id() + " " + from + " -> " + to);
                sender.sendMessage(ChatColor.GREEN + "Portal „" + p.name() + "“ verschoben.");
            }
            case "loeschen" -> {
                if (args.length < 5 || !args[4].equalsIgnoreCase("bestaetigen")) {
                    sender.sendMessage(ChatColor.YELLOW + "Wirklich löschen? Hänge " + ChatColor.WHITE + "bestaetigen"
                            + ChatColor.YELLOW + " an: /" + label + " admin portal loeschen #" + p.id() + " bestaetigen");
                    return;
                }
                plugin.portals().remove(p);
                plugin.audit(sender.getName(), "portal loeschen", "#" + p.id() + " " + p.name() + " " + p.core());
                sender.sendMessage(ChatColor.GREEN + "Portal „" + p.name() + "“ gelöscht.");
            }
            case "neutral" -> {
                plugin.portals().neutralize(p);
                plugin.audit(sender.getName(), "portal neutral", "#" + p.id() + " " + p.name());
                sender.sendMessage(ChatColor.GREEN + "Portal „" + p.name() + "“ ist jetzt neutral.");
            }
            case "tp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Nur für Spieler.");
                    return;
                }
                World w = Bukkit.getWorld(p.core().world());
                if (w != null) {
                    player.teleport(safeSpot(w, p.core().x(), p.core().y(), p.core().z() + 5));
                }
            }
            default -> sender.sendMessage(ChatColor.RED + "Unbekannte Aktion: " + action);
        }
    }

    /**
     * Freier Stehplatz in Spalte x/z: ab Höhe y nach oben die erste Stelle mit zwei Luftblöcken
     * über festem Boden; sonst oberster Block + 1. Blick nach Norden (zum Kern).
     */
    static Location safeSpot(World w, int x, int y, int z) {
        for (int yy = Math.max(y - 3, w.getMinHeight() + 1); yy < Math.min(y + 24, w.getMaxHeight() - 2); yy++) {
            if (w.getBlockAt(x, yy - 1, z).getType().isSolid()
                    && w.getBlockAt(x, yy, z).isPassable() && w.getBlockAt(x, yy + 1, z).isPassable()) {
                return new Location(w, x + 0.5, yy, z + 0.5, 180, 0);
            }
        }
        return new Location(w, x + 0.5, w.getHighestBlockYAt(x, z) + 1, z + 0.5, 180, 0);
    }
}
