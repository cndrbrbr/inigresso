package io.github.cndrbrbr.inigresso.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.service.FactionService;
import io.github.cndrbrbr.inigresso.service.PlayerProgressService;
import io.github.cndrbrbr.inigresso.service.ProfileService;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerRepositoryTest {

    private static final Logger LOG = Logger.getLogger("test");

    @TempDir
    Path dir;

    @Test
    void profileUeberlebenNeustartUndWechselLeertInventar() throws Exception {
        Path file = dir.resolve("db");
        UUID anna = UUID.randomUUID();
        UUID ben = UUID.randomUUID();
        try (Database db = new Database(file, LOG)) {
            GameEventBus bus = new GameEventBus(LOG);
            PlayerRepository repo = new PlayerRepository(db);
            repo.bind(bus);
            ProfileService profiles = new ProfileService(bus, repo.loadAll());
            FactionService factions = new FactionService(profiles, bus);
            PlayerProgressService progress = new PlayerProgressService(bus);

            PlayerProfile a = profiles.join(anna, "anna");
            factions.choose(a, Faction.LUMEN);
            progress.add(a, 3_000);
            profiles.join(ben, "ben");

            db.submit(c -> {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT INTO player_item (uuid, item, level, count) VALUES ('"
                            + anna + "', 'RESONATOR', 3, 5)");
                }
            });
            factions.adminSet(a, Faction.WAECHTER);
        }
        try (Database db = new Database(file, LOG)) {
            List<PlayerProfile> loaded = new PlayerRepository(db).loadAll();
            assertEquals(2, loaded.size());
            PlayerProfile a = loaded.stream().filter(p -> p.uuid().equals(anna)).findFirst().orElseThrow();
            assertEquals(Faction.WAECHTER, a.faction());
            assertEquals(3_000, a.ap());
            PlayerProfile b = loaded.stream().filter(p -> p.uuid().equals(ben)).findFirst().orElseThrow();
            assertNull(b.faction());
            try (Statement st = db.connection().createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM player_item")) {
                rs.next();
                assertEquals(0, rs.getInt(1));
            }
        }
    }
}
