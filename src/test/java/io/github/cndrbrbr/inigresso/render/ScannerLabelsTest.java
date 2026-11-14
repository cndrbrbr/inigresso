package io.github.cndrbrbr.inigresso.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScannerLabelsTest {

    private static Portal portal(int id, int x, int z) {
        return new Portal(id, "P" + id, new BlockPos("world", x, 64, z), Faction.NEUTRAL, null, 0, null, 0);
    }

    private static final ScannerProjection NORTH = new ScannerProjection(0, 0, 0, 256, false);

    @Test
    void nummerRechtsNebenDemPunkt() {
        var labels = ScannerLabels.place(NORTH, "world", List.of(portal(12, 40, 0)), null, 2, List.of());
        assertEquals(1, labels.size());
        assertEquals("12", labels.get(0).text());
        // Punkt bei x = 64 + 20.25 → Text ab 84 + 4
        assertEquals(88, labels.get(0).x());
    }

    @Test
    void keineUeberlappungUndNaeheresZuerst() {
        List<Portal> ps = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ps.add(portal(10 + i, 30, i * 2)); // dicht übereinander
        }
        var labels = ScannerLabels.place(NORTH, "world", ps, null, 2, List.of());
        assertEquals("10", labels.get(0).text());
        for (int i = 0; i < labels.size(); i++) {
            for (int j = i + 1; j < labels.size(); j++) {
                var a = labels.get(i);
                var b = labels.get(j);
                boolean overlap = a.x() <= b.x() + ScannerLabels.width(b.text()) && b.x() <= a.x() + ScannerLabels.width(a.text())
                        && a.y() <= b.y() + ScannerLabels.HEIGHT && b.y() <= a.y() + ScannerLabels.HEIGHT;
                assertFalse(overlap, a + " / " + b);
            }
        }
        assertTrue(labels.size() < ps.size());
    }

    @Test
    void weitWegNurDasZiel() {
        var far = new ScannerProjection(0, 0, 0, 1024, false);
        Portal t = portal(7, 100, 100);
        var labels = ScannerLabels.place(far, "world", List.of(portal(3, -100, 0), t), t, 1, List.of());
        assertEquals(1, labels.size());
        assertEquals("7", labels.get(0).text());
    }

    @Test
    void reservierterBereichUndAndereWelt() {
        Portal other = new Portal(5, "X", new BlockPos("nether", 10, 64, 10), Faction.NEUTRAL, null, 0, null, 0);
        var labels = ScannerLabels.place(NORTH, "world", List.of(other, portal(9, -100, -120)), null, 2,
                List.of(new int[] {0, 0, 32, 10}));
        // Portal 9 liegt oben links im reservierten Feld → Nummer weicht aus oder fehlt, nie im Feld
        for (var l : labels) {
            assertFalse(l.x() <= 32 && l.y() <= 10, l.toString());
            assertEquals("9", l.text());
        }
    }
}
