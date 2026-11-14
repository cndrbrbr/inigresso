package io.github.cndrbrbr.inigresso;

import io.github.cndrbrbr.inigresso.command.IgCommand;
import io.github.cndrbrbr.inigresso.config.InigressoConfig;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.item.GameInventory;
import io.github.cndrbrbr.inigresso.item.ItemFactory;
import io.github.cndrbrbr.inigresso.listener.ItemGuardListener;
import io.github.cndrbrbr.inigresso.listener.PlayerConnectionListener;
import io.github.cndrbrbr.inigresso.listener.PortalInteractListener;
import io.github.cndrbrbr.inigresso.service.CombatService;
import io.github.cndrbrbr.inigresso.service.EnergyService;
import io.github.cndrbrbr.inigresso.service.HackService;
import io.github.cndrbrbr.inigresso.service.LinkService;
import io.github.cndrbrbr.inigresso.service.ScoreService;
import io.github.cndrbrbr.inigresso.store.ScoreRepository;
import io.github.cndrbrbr.inigresso.store.LinkRepository;
import io.github.cndrbrbr.inigresso.render.NetworkRenderer;
import io.github.cndrbrbr.inigresso.store.StateRepository;
import io.github.cndrbrbr.inigresso.store.AutoCellRepository;
import io.github.cndrbrbr.inigresso.service.AutoPortalPlanner;
import io.github.cndrbrbr.inigresso.listener.AutoPortalListener;
import io.github.cndrbrbr.inigresso.store.HackRepository;
import java.util.random.RandomGenerator;
import io.github.cndrbrbr.inigresso.listener.ProtectionListener;
import io.github.cndrbrbr.inigresso.render.FactionTeams;
import io.github.cndrbrbr.inigresso.render.PortalRenderer;
import io.github.cndrbrbr.inigresso.service.FactionService;
import io.github.cndrbrbr.inigresso.service.PlayerProgressService;
import io.github.cndrbrbr.inigresso.service.PortalService;
import io.github.cndrbrbr.inigresso.service.ProfileService;
import io.github.cndrbrbr.inigresso.service.ProposalService;
import io.github.cndrbrbr.inigresso.store.AuditRepository;
import io.github.cndrbrbr.inigresso.store.Database;
import io.github.cndrbrbr.inigresso.store.MessageRepository;
import io.github.cndrbrbr.inigresso.store.PlayerRepository;
import io.github.cndrbrbr.inigresso.store.PortalRepository;
import java.time.Clock;
import org.bukkit.entity.Player;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Einstiegspunkt. Startreihenfolge nach implement.md Phase 0:
 * Konfiguration → Datenbank → Services → Render → Listener/Befehle.
 */
public final class InigressoPlugin extends JavaPlugin {

    private InigressoConfig settings;
    private Database database;
    private GameEventBus bus;
    private ProfileService profiles;
    private FactionService factions;
    private PlayerProgressService progress;
    private AuditRepository audit;
    private PortalService portals;
    private ProposalService proposals;
    private MessageRepository messages;
    private PortalRenderer portalRenderer;
    private HackService hacks;
    private GameInventory gameInventory;
    private CombatService combat;
    private EnergyService energy;
    private PortalInteractListener portalInteract;
    private LinkService links;
    private NetworkRenderer network;
    private AutoCellRepository autoCells;
    private java.util.Set<String> autoDone;
    private ScoreService score;
    private ScoreRepository scoreRepo;
    private StateRepository state;
    private FactionTeams teams;
    private io.github.cndrbrbr.inigresso.render.Navigator navigator;
    private io.github.cndrbrbr.inigresso.render.ScannerRenderer scannerRenderer;
    private io.github.cndrbrbr.inigresso.item.Scanner scanner;
    private io.github.cndrbrbr.inigresso.service.CaptureStats captures;
    private io.github.cndrbrbr.inigresso.render.Sidebar sidebar;
    private final Clock clock = Clock.systemDefaultZone();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        try {
            getDataFolder().mkdirs();
            database = new Database(getDataFolder().toPath().resolve("inigresso.db"), getLogger());
            getLogger().info("Datenbank bereit (Schema-Version " + database.schemaVersion() + ").");

            bus = new GameEventBus(getLogger());
            PlayerRepository players = new PlayerRepository(database);
            players.bind(bus);
            audit = new AuditRepository(database);
            profiles = new ProfileService(bus, players.loadAll());
            factions = new FactionService(profiles, bus);
            progress = new PlayerProgressService(bus);
            PortalRepository portalRepo = new PortalRepository(database);
            portalRepo.bind(bus);
            messages = new MessageRepository(database);
            portals = new PortalService(bus, clock, () -> settings.portalProtectRadius(), progress,
                    portalRepo.loadPortals());
            HackRepository hackRepo = new HackRepository(database);
            hackRepo.bind(bus);
            hacks = new HackService(bus, clock, RandomGenerator.getDefault(),
                    () -> new HackService.Rules(settings.hackCooldownMinutes(), settings.hackMaxPer4h(),
                            settings.hackKeyChance()),
                    progress, hackRepo.loadSince(clock.millis() - HackService.WINDOW_MS));
            combat = new CombatService(portals, progress, bus, () -> settings.resonatorRadius());
            state = new StateRepository(database);
            LinkRepository linkRepo = new LinkRepository(database);
            linkRepo.bind(bus);
            links = new LinkService(portals, progress, bus, clock,
                    () -> new LinkService.Rules(settings.linkMaxRange(), settings.linkMaxOutgoing()),
                    linkRepo.loadLinks(), linkRepo.loadFields());
            scoreRepo = new ScoreRepository(database, state);
            long storedCycle = scoreRepo.storedCycleStart();
            score = new ScoreService(clock,
                    () -> new ScoreService.Rules(settings.cycleStartDay(), settings.cycleLengthHours(),
                            settings.measureMinutes()),
                    links::influence, scoreRepo.sink(this::announceCycle), storedCycle,
                    scoreRepo.measurementsSince(storedCycle));
            energy = new EnergyService(portals, progress, bus, clock,
                    () -> new EnergyService.DecayRules(settings.decayPercentPerDay(), settings.decayTime()),
                    state.getLong("last_decay", 0), t -> state.set("last_decay", Long.toString(t)));
            proposals = new ProposalService(bus, clock,
                    () -> new ProposalService.Rules(settings.worlds(), settings.portalMinDistance(),
                            settings.proposalMinLevel(), settings.proposalMaxOpen()),
                    portals, progress, profiles, portalRepo.loadProposals());
            var captureRepo = new io.github.cndrbrbr.inigresso.store.CaptureRepository(database);
            captureRepo.bind(bus);
            captures = new io.github.cndrbrbr.inigresso.service.CaptureStats(bus,
                    captureRepo.loadSince(score.cycleStart()));
            autoCells = new AutoCellRepository(database);
            autoDone = autoCells.loadDone();
            getLogger().info(profiles.all().size() + " Spielerprofile, " + portals.all().size() + " Portale, "
                    + proposals.open().size() + " offene Vorschläge, " + links.links().size() + " Links, "
                    + links.fields().size() + " Felder geladen.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Datenbank konnte nicht geöffnet werden – Inigresso wird deaktiviert.", e);
            if (database != null) {
                database.close();
                database = null;
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        java.util.Set<java.util.UUID> quiet = new java.util.HashSet<>();
        try {
            for (String s : state.get("sidebar_hidden").orElse("").split(",")) {
                if (!s.isBlank()) {
                    quiet.add(java.util.UUID.fromString(s.trim()));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            getLogger().log(Level.WARNING, "Ausgeblendete Scoreboards nicht lesbar.", e);
        }
        teams = new FactionTeams(quiet, set -> state.set("sidebar_hidden",
                String.join(",", set.stream().map(java.util.UUID::toString).toList())));
        teams.setup();
        teams.bind(bus);
        portalRenderer = new PortalRenderer(portals, () -> settings.resonatorRadius(), () -> settings.resonatorBlocks());
        portalRenderer.bind(bus);
        portalRenderer.showLoaded();
        getServer().getPluginManager().registerEvents(portalRenderer, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(portals), this);
        AutoPortalListener auto = new AutoPortalListener(this, portals, autoCells, autoDone,
                () -> new AutoPortalPlanner(settings.autoSpacing(), settings.autoJitter()),
                () -> settings.portalProtectRadius(), () -> settings.worlds(), () -> settings.autoPortals());
        getServer().getPluginManager().registerEvents(auto, this);
        auto.scanLoaded();
        ItemFactory itemFactory = new ItemFactory(this, portals);
        gameInventory = new GameInventory(itemFactory, () -> settings.playerMaxItems());
        portalInteract = new PortalInteractListener(portals, hacks, profiles, itemFactory, gameInventory, combat, energy);
        getServer().getPluginManager().registerEvents(portalInteract, this);
        ItemGuardListener guard = new ItemGuardListener(itemFactory, gameInventory, profiles, bus);
        getServer().getPluginManager().registerEvents(guard, this);
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(profiles, teams, messages, clock, bus), this);
        network = new NetworkRenderer(links, portals, () -> settings.linkBeamHeight());
        getServer().getScheduler().runTaskTimer(this, network::drawLinks, 10L, 10L);
        getServer().getScheduler().runTaskTimer(this, network::drawFields, 15L, 10L);
        getServer().getScheduler().runTaskTimer(this, network::updateBars, 20L, 20L);
        // Webkarte: Marker auf BlueMap, nur wenn installiert (sonst fehlen die API-Klassen)
        if (getServer().getPluginManager().getPlugin("BlueMap") != null) {
            new io.github.cndrbrbr.inigresso.render.WebMapMarkers(this, portals, links,
                    () -> settings.linkBeamHeight()).start(bus);
            getLogger().info("BlueMap gefunden: Portale, Links und Felder erscheinen auf der Webkarte.");
        }
        // Scanner-Karte und Navigation (/ig karte, /ig ziel, /ig scanner)
        navigator = new io.github.cndrbrbr.inigresso.render.Navigator(portals);
        scannerRenderer = new io.github.cndrbrbr.inigresso.render.ScannerRenderer(portals, links, navigator,
                id -> profiles.get(id).map(pr -> pr.faction()).orElse(null));
        scanner = new io.github.cndrbrbr.inigresso.item.Scanner(this, scannerRenderer);
        long storedMap;
        try {
            storedMap = state.getLong("scanner_map", -1);
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "Karten-ID des Scanners nicht lesbar, lege neue an.", e);
            storedMap = -1;
        }
        int mapId = scanner.setup(storedMap);
        if (mapId != storedMap) {
            state.set("scanner_map", Integer.toString(mapId));
            getLogger().info("Scanner-Karte #" + mapId + " angelegt.");
        }
        guard.keepAlso(scanner::isScanner);
        getServer().getPluginManager().registerEvents(
                new io.github.cndrbrbr.inigresso.listener.ScannerListener(scanner, scannerRenderer, navigator, bus), this);
        getServer().getScheduler().runTaskTimer(this, navigator::tick, 10L, 10L);
        sidebar = new io.github.cndrbrbr.inigresso.render.Sidebar();
        getServer().getScheduler().runTaskTimer(this, this::updateSidebar, 40L, 20L);
        // Verfall: verpasste Tage sofort nachholen, dann jede Minute prüfen.
        int missed = energy.tick();
        if (missed > 0) {
            getLogger().info("Energieverfall für " + missed + " Tag(e) nachgeholt.");
        }
        score.tick();
        getServer().getScheduler().runTaskTimer(this, () -> {
            int days = energy.tick();
            if (days > 0) {
                getLogger().info("Täglicher Energieverfall angewendet.");
            }
            score.tick();
        }, 1200L, 1200L);
        // Nach /reload sind Spieler schon online.
        for (Player p : getServer().getOnlinePlayers()) {
            teams.apply(profiles.join(p.getUniqueId(), p.getName()));
            teams.applyBoard(p);
            scanner.fix(p);
        }

        IgCommand ig = new IgCommand(this);
        Objects.requireNonNull(getCommand("ig"), "ig fehlt in plugin.yml").setExecutor(ig);
        getCommand("ig").setTabCompleter(ig);
    }

    @Override
    public void onDisable() {
        if (portalRenderer != null) {
            portalRenderer.removeAll();
        }
        if (network != null) {
            network.removeAll();
        }
        if (sidebar != null) {
            sidebar.remove();
        }
        if (teams != null) {
            teams.restoreAll();
        }
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public void reloadSettings() {
        reloadConfig();
        settings = InigressoConfig.from(getConfig(), getLogger());
        if (portals != null) {
            portals.reindex();
        }
        if (portalRenderer != null) {
            portalRenderer.refreshAll();
        }
    }

    public InigressoConfig settings() {
        return settings;
    }

    public Database database() {
        return database;
    }

    public ProfileService profiles() {
        return profiles;
    }

    public FactionService factions() {
        return factions;
    }

    public PlayerProgressService progress() {
        return progress;
    }

    public PortalService portals() {
        return portals;
    }

    public ProposalService proposals() {
        return proposals;
    }

    public GameInventory gameInventory() {
        return gameInventory;
    }

    private void announceCycle(ScoreService.CycleResult r) {
        String msg = r.winner() == null
                ? org.bukkit.ChatColor.GOLD + "[Inigresso] Zyklus beendet: unentschieden."
                : org.bukkit.ChatColor.GOLD + "[Inigresso] Zyklus beendet: " + r.winner().colored()
                        + org.bukkit.ChatColor.GOLD + " gewinnt!";
        getServer().broadcastMessage(msg);
        getServer().broadcastMessage(org.bukkit.ChatColor.GRAY + String.format(java.util.Locale.GERMANY,
                "Durchschnittlicher Einfluss: Lumen %,.0f – Wächter %,.0f (%d Messpunkte)",
                r.lumenAvg(), r.waechterAvg(), r.measurements()));
        getLogger().info("Zyklus abgeschlossen: " + r);
    }

    public ScoreService score() {
        return score;
    }

    public ScoreRepository scoreRepository() {
        return scoreRepo;
    }

    public LinkService links() {
        return links;
    }

    public FactionTeams teams() {
        return teams;
    }

    public io.github.cndrbrbr.inigresso.render.Navigator navigator() {
        return navigator;
    }

    public io.github.cndrbrbr.inigresso.render.ScannerRenderer scannerRenderer() {
        return scannerRenderer;
    }

    public io.github.cndrbrbr.inigresso.item.Scanner scanner() {
        return scanner;
    }

    public PortalInteractListener portalInteract() {
        return portalInteract;
    }

    private void updateSidebar() {
        if (!settings.scoreboard()) {
            sidebar.remove();
            return;
        }
        long since = score.cycleStart();
        captures.prune(since);
        var st = score.standing();
        sidebar.show(new io.github.cndrbrbr.inigresso.render.Sidebar.Data(
                score.cycleEnd() - clock.millis(),
                side(io.github.cndrbrbr.inigresso.model.Faction.LUMEN, st.lumenAvg(), since),
                side(io.github.cndrbrbr.inigresso.model.Faction.WAECHTER, st.waechterAvg(), since),
                captures.top(since, 3).stream()
                        .map(e -> new io.github.cndrbrbr.inigresso.render.Sidebar.Top(
                                profiles.get(e.player()).map(p -> p.lastName()).orElse("?"), e.count()))
                        .toList()));
    }

    private io.github.cndrbrbr.inigresso.render.Sidebar.Side side(io.github.cndrbrbr.inigresso.model.Faction f,
            double average, long since) {
        double area = links.fields().stream().filter(x -> x.faction() == f)
                .mapToDouble(x -> links.triangle(x).map(t -> t.area()).orElse(0.0)).sum();
        int owned = (int) portals.all().stream().filter(p -> p.owner() == f).count();
        return new io.github.cndrbrbr.inigresso.render.Sidebar.Side(f, area, score.influenceNow(f), average, owned,
                captures.count(f, since));
    }

    /** Admin-Eingriffe protokollieren (rules.md §11). */
    public void audit(String actor, String action, String detail) {
        audit.log(actor, action, detail);
        getLogger().info("[Audit] " + actor + " " + action + ": " + detail);
    }
}
