package io.github.cndrbrbr.inigresso.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseTest {

    private static final Logger LOG = Logger.getLogger("test");

    @TempDir
    Path dir;

    @Test
    void legtSchemaAnUndMigriertNurEinmal() throws Exception {
        Path file = dir.resolve("inigresso.db");
        try (Database db = new Database(file, LOG)) {
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion());
        }
        try (Database db = new Database(file, LOG)) {
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion());
            try (Statement st = db.connection().createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM schema_version")) {
                rs.next();
                assertEquals(Database.MIGRATIONS.size(), rs.getInt(1));
            }
        }
    }

    @Test
    void schreibqueueWirdBeimSchliessenGeleert() throws Exception {
        Path file = dir.resolve("inigresso.db");
        try (Database db = new Database(file, LOG)) {
            for (int i = 0; i < 100; i++) {
                int n = i;
                db.submit(c -> {
                    try (var ps = c.prepareStatement("INSERT INTO player (uuid, last_name) VALUES (?, ?)")) {
                        ps.setString(1, "uuid-" + n);
                        ps.setString(2, "spieler" + n);
                        ps.executeUpdate();
                    }
                });
            }
        }
        try (Database db = new Database(file, LOG);
             Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM player")) {
            rs.next();
            assertEquals(100, rs.getInt(1));
        }
    }

    @Test
    void fehlerhafterSchreibauftragWirdZurueckgerolltUndQueueLaeuftWeiter() throws Exception {
        Path file = dir.resolve("inigresso.db");
        try (Database db = new Database(file, LOG)) {
            db.submit(c -> {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT INTO player (uuid, last_name) VALUES ('a', 'a')");
                    st.executeUpdate("INSERT INTO gibtsnicht VALUES (1)");
                }
            });
            db.submit(c -> {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT INTO player (uuid, last_name) VALUES ('b', 'b')");
                }
            });
        }
        try (Database db = new Database(file, LOG);
             Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT GROUP_CONCAT(uuid) FROM player")) {
            rs.next();
            assertEquals("b", rs.getString(1));
        }
    }
}
