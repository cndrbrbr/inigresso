package io.github.cndrbrbr.inigresso.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.model.Faction;
import java.util.List;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class SidebarTest {

    private static Sidebar.Data data(double lumenAvg, double waechterAvg, List<Sidebar.Top> top) {
        return new Sidebar.Data(3L * 86_400_000 + 4 * 3_600_000 + 5 * 60_000,
                new Sidebar.Side(Faction.LUMEN, 12_345.6, 124, lumenAvg, 5, 7),
                new Sidebar.Side(Faction.WAECHTER, 800, 8, waechterAvg, 3, 2), top);
    }

    @Test
    void hoechstens15ZeilenUndInhalt() {
        var lines = Sidebar.lines(data(100, 20, List.of(new Sidebar.Top("anna", 4),
                new Sidebar.Top("ben", 3), new Sidebar.Top("cem", 1))));
        assertEquals(15, lines.size());
        List<String> plain = lines.stream().map(ChatColor::stripColor).toList();
        assertEquals("Zyklus: noch 3 T 4 h", plain.get(0));
        assertEquals("Lumen ★ führt", plain.get(1));
        assertEquals(" Fläche: 12.346 m²", plain.get(2));
        assertEquals(" Einfluss: 124 (Ø 100)", plain.get(3));
        assertEquals(" Portale: 5  Erobert: 7", plain.get(4));
        assertEquals("Wächter", plain.get(6));
        assertEquals(" 1. anna 4", plain.get(12));
        assertEquals(" 3. cem 1", plain.get(14));
    }

    @Test
    void gleichstandOhneFuehrungUndLeereRangliste() {
        List<String> plain = Sidebar.lines(data(0, 0, List.of())).stream().map(ChatColor::stripColor).toList();
        assertEquals("Lumen", plain.get(1));
        assertTrue(plain.contains(" noch keine"));
    }

    @Test
    void eintraegeSindEindeutig() {
        assertNotEquals(Sidebar.entry("", 5), Sidebar.entry("", 10));
        assertEquals("1 h 0 min", Sidebar.remaining(3_600_000));
        assertEquals("7 min", Sidebar.remaining(7 * 60_000));
    }
}
