package io.github.cndrbrbr.inigresso;

import io.github.cndrbrbr.inigresso.command.IgCommand;
import io.github.cndrbrbr.inigresso.config.InigressoConfig;
import io.github.cndrbrbr.inigresso.store.Database;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        try {
            getDataFolder().mkdirs();
            database = new Database(getDataFolder().toPath().resolve("inigresso.db"), getLogger());
            getLogger().info("Datenbank bereit (Schema-Version " + database.schemaVersion() + ").");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Datenbank konnte nicht geöffnet werden – Inigresso wird deaktiviert.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        IgCommand ig = new IgCommand(this);
        Objects.requireNonNull(getCommand("ig"), "ig fehlt in plugin.yml").setExecutor(ig);
        getCommand("ig").setTabCompleter(ig);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public void reloadSettings() {
        reloadConfig();
        settings = InigressoConfig.from(getConfig(), getLogger());
    }

    public InigressoConfig settings() {
        return settings;
    }

    public Database database() {
        return database;
    }
}
