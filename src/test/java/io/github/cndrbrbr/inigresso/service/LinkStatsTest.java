package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Link;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkStatsTest {

    @Test
    void zaehltGezogeneLinksImZyklus() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        GameEventBus bus = new GameEventBus(java.util.logging.Logger.getAnonymousLogger());
        LinkStats stats = new LinkStats(bus, List.of(new LinkStats.Drawn(a, 50), new LinkStats.Drawn(b, 5)));
        bus.publish(new GameEvent.LinkCreated(new Link(1, 1, 2, Faction.LUMEN, b, 100)));
        bus.publish(new GameEvent.LinkCreated(new Link(2, 2, 3, Faction.LUMEN, b, 110)));
        bus.publish(new GameEvent.LinkCreated(new Link(3, 3, 4, Faction.CLIP, c, 120)));
        var top = stats.top(10, 3);
        assertEquals(b, top.get(0).player());
        assertEquals(2, top.get(0).count());
        assertEquals(a, top.get(1).player()); // 1 Link, früher als c
        assertEquals(c, top.get(2).player());
        stats.prune(60);
        assertEquals(2, stats.top(0, 5).size());
    }
}
