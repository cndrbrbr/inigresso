package io.github.cndrbrbr.inigresso.store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite-Speicher (der Treiber steckt im Spigot-Server-Jar). Gelesen wird nur beim Start
 * auf dem Server-Thread; alle Schreibzugriffe laufen nacheinander über einen einzigen
 * Hintergrund-Thread (siehe architecture.md, Abschnitt 2).
 */
public final class Database implements AutoCloseable {

    /** Migrationen in Reihenfolge, Ressourcen unter /db/. Neue nur anhängen. */
    static final List<String> MIGRATIONS = List.of("V1__init.sql", "V2__portal_proposer.sql", "V3__state.sql", "V4__cycle_details.sql",
            "V5__auto_cell.sql", "V6__capture_log.sql");

    /** Ein Schreibauftrag, läuft im Hintergrund-Thread in einer eigenen Transaktion. */
    @FunctionalInterface
    public interface Write {
        void run(Connection connection) throws SQLException;
    }

    private final Connection connection;
    private final Logger logger;
    private final ExecutorService writer;

    public Database(Path file, Logger logger) throws SQLException {
        this.logger = logger;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous = NORMAL");
        }
        migrate();
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Inigresso-DB");
            t.setDaemon(true);
            return t;
        });
    }

    /** Für Lesezugriffe beim Start (Server-Thread, bevor Schreibaufträge laufen). */
    public Connection connection() {
        return connection;
    }

    public int schemaVersion() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void migrate() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, applied_at INTEGER NOT NULL)");
        }
        int current = schemaVersion();
        for (int i = current; i < MIGRATIONS.size(); i++) {
            String name = MIGRATIONS.get(i);
            String sql = readResource("/db/" + name);
            boolean auto = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                for (String part : sql.split(";")) {
                    if (!part.isBlank()) {
                        st.execute(part);
                    }
                }
                st.execute("INSERT INTO schema_version (version, applied_at) VALUES (" + (i + 1) + ", "
                        + System.currentTimeMillis() + ")");
                connection.commit();
                logger.info("Datenbank-Migration " + name + " angewendet.");
            } catch (SQLException e) {
                connection.rollback();
                throw new SQLException("Migration " + name + " fehlgeschlagen", e);
            } finally {
                connection.setAutoCommit(auto);
            }
        }
    }

    private static String readResource(String path) throws SQLException {
        try (InputStream in = Database.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new SQLException("Ressource fehlt: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Ressource nicht lesbar: " + path, e);
        }
    }

    /** Reiht einen Schreibauftrag ein; Fehler werden geloggt, nicht geworfen. */
    public void submit(Write write) {
        writer.execute(() -> {
            try {
                connection.setAutoCommit(false);
                write.run(connection);
                connection.commit();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Schreiben in die Datenbank fehlgeschlagen", e);
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // Verbindung ist ohnehin kaputt; der Fehler oben ist geloggt.
                }
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // siehe oben
                }
            }
        });
    }

    /** Wartet, bis alle eingereihten Schreibaufträge erledigt sind, und schließt die Verbindung. */
    @Override
    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.severe("Datenbank-Schreibqueue nach 30 s nicht leer – breche ab.");
                writer.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writer.shutdownNow();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Datenbank konnte nicht sauber geschlossen werden", e);
        }
    }
}
