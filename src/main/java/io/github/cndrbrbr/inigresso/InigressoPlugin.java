package io.github.cndrbrbr.inigresso;

import io.github.cndrbrbr.inigresso.command.IgCommand;
import io.github.cndrbrbr.inigresso.config.InigressoConfig;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.listener.PlayerConnectionListener;
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
            portals = new PortalService(bus, clock, () -> settings.portalProtectRadius(), portalRepo.loadPortals());
            proposals = new ProposalService(bus, clock,
                    () -> new ProposalService.Rules(settings.worlds(), settings.portalMinDistance(),
                            settings.proposalMinLevel(), settings.proposalMaxOpen()),
                    portals, progress, profiles, portalRepo.loadProposals());
            getLogger().info(profiles.all().size() + " Spielerprofile, " + portals.all().size() + " Portale, "
                    + proposals.open().size() + " offene Vorschläge geladen.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Datenbank konnte nicht geöffnet werden – Inigresso wird deaktiviert.", e);
            if (database != null) {
                database.close();
                database = null;
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        FactionTeams teams = new FactionTeams();
        teams.setup();
        teams.bind(bus);
        portalRenderer = new PortalRenderer(portals);
        portalRenderer.bind(bus);
        portalRenderer.showLoaded();
        getServer().getPluginManager().registerEvents(portalRenderer, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(portals), this);
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(profiles, teams, messages, clock, bus), this);
        // Nach /reload sind Spieler schon online.
        for (Player p : getServer().getOnlinePlayers()) {
            teams.apply(profiles.join(p.getUniqueId(), p.getName()));
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

    /** Admin-Eingriffe protokollieren (rules.md §11). */
    public void audit(String actor, String action, String detail) {
        audit.log(actor, action, detail);
        getLogger().info("[Audit] " + actor + " " + action + ": " + detail);
    }
}
