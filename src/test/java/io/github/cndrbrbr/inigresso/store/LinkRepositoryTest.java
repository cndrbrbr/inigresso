package io.github.cndrbrbr.inigresso.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.LinkService;
import io.github.cndrbrbr.inigresso.service.PlayerProgressService;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinkRepositoryTest {

    private static final Logger LOG = Logger.getLogger("test");

    @TempDir
    Path dir;

    @Test
    void linksUndFelderUeberlebenNeustart() throws Exception {
        Path file = dir.resolve("db");
        try (Database db = new Database(file, LOG)) {
            GameEventBus bus = new GameEventBus(LOG);
            PortalRepository portalRepo = new PortalRepository(db);
            portalRepo.bind(bus);
            LinkRepository linkRepo = new LinkRepository(db);
            linkRepo.bind(bus);
            PlayerProgressService progress = new PlayerProgressService(bus);
            PortalService portals = new PortalService(bus, Clock.systemUTC(), () -> 4, progress, List.of());
            LinkService links = new LinkService(portals, progress, bus, Clock.systemUTC(),
                    () -> new LinkService.Rules(2000, 8), List.of(), List.of());
            Portal[] p = new Portal[4];
            int[][] xz = {{0, 0}, {60, 0}, {0, 60}, {-60, -60}};
            for (int i = 0; i < 4; i++) {
                p[i] = portals.create("P" + i, new BlockPos("world", xz[i][0], 64, xz[i][1]), null);
                for (int j = 0; j < 8; j++) {
                    PlayerProfile pl = new PlayerProfile(UUID.randomUUID(), "x", null, 2_500);
                    pl.setFaction(Faction.LUMEN);
                    portals.deploy(pl, p[i], 2);
                }
            }
            PlayerProfile me = new PlayerProfile(UUID.randomUUID(), "me", null, 0);
            me.setFaction(Faction.LUMEN);
            links.link(me, p[0].core(), p[0], p[1]);
            links.link(me, p[1].core(), p[1], p[2]);
            links.link(me, p[2].core(), p[2], p[0]);
            links.link(me, p[3].core(), p[3], p[0]);
            portals.remove(p[3]);
        }
        try (Database db = new Database(file, LOG)) {
            LinkRepository repo = new LinkRepository(db);
            assertEquals(3, repo.loadLinks().size());
            assertEquals(1, repo.loadFields().size());
            assertEquals(18, repo.loadFields().get(0).influence());
        }
    }
}
