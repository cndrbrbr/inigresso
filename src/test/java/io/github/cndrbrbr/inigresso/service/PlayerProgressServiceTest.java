package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class PlayerProgressServiceTest {

    @Test
    void stufenGrenzen() {
        assertEquals(1, PlayerProgressService.levelFor(0));
        assertEquals(1, PlayerProgressService.levelFor(2_499));
        assertEquals(2, PlayerProgressService.levelFor(2_500));
        assertEquals(3, PlayerProgressService.levelFor(20_000));
        assertEquals(7, PlayerProgressService.levelFor(1_199_999));
        assertEquals(8, PlayerProgressService.levelFor(1_200_000));
        assertEquals(8, PlayerProgressService.levelFor(Long.MAX_VALUE));
        assertEquals(2_500, PlayerProgressService.apToNextLevel(0));
        assertEquals(-1, PlayerProgressService.apToNextLevel(5_000_000));
    }

    @Test
    void aufstiegWirdGemeldet() {
        GameEventBus bus = new GameEventBus(Logger.getLogger("test"));
        List<GameEvent.LevelUp> ups = new ArrayList<>();
        bus.subscribe(GameEvent.LevelUp.class, ups::add);
        PlayerProgressService s = new PlayerProgressService(bus);
        PlayerProfile p = new PlayerProfile(UUID.randomUUID(), "x", null, 2_400);

        s.award(p, PlayerProgressService.Action.HACK_OWN); // 2 500
        assertEquals(2_500, p.ap());
        assertEquals(List.of(new GameEvent.LevelUp(p, 2)), ups);

        s.add(p, 100_000); // springt auf Stufe 4, nur ein Ereignis
        assertEquals(2, ups.size());
        assertEquals(4, ups.get(1).newLevel());

        s.add(p, -1_000_000);
        assertEquals(0, p.ap(), "AP werden nicht negativ");
        assertEquals(2, ups.size());
    }
}
