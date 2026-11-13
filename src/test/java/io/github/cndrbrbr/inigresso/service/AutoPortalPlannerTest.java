package io.github.cndrbrbr.inigresso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.service.AutoPortalPlanner.Cell;
import io.github.cndrbrbr.inigresso.service.AutoPortalPlanner.Target;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AutoPortalPlannerTest {

    private final AutoPortalPlanner planner = new AutoPortalPlanner(100, 25);

    @Test
    void targetStaysWithinJitterAndIsReproducible() {
        for (int gx = -5; gx <= 5; gx++) {
            for (int gz = -5; gz <= 5; gz++) {
                Cell c = new Cell("world", gx, gz);
                Target t = planner.target(c);
                assertTrue(Math.abs(t.x() - gx * 100) <= 25);
                assertTrue(Math.abs(t.z() - gz * 100) <= 25);
                assertEquals(t, planner.target(c));
            }
        }
    }

    @Test
    void everyTargetIsFoundInExactlyItsChunk() {
        Set<String> seen = new HashSet<>();
        for (int cx = -20; cx <= 20; cx++) {
            for (int cz = -20; cz <= 20; cz++) {
                for (Target t : planner.targetsInChunk("world", cx, cz)) {
                    assertEquals(cx, t.x() >> 4);
                    assertEquals(cz, t.z() >> 4);
                    assertTrue(seen.add(t.cell().key()), "doppelt: " + t.cell());
                }
            }
        }
        // Chunks -20..20 decken x/z -320..335 ab → Zellen -2..2 (±25 Versatz) sicher drin.
        for (int gx = -2; gx <= 2; gx++) {
            for (int gz = -2; gz <= 2; gz++) {
                assertTrue(seen.contains(new Cell("world", gx, gz).key()), gx + "/" + gz);
            }
        }
    }

    @Test
    void neighbouringTargetsKeepMinimumDistance() {
        for (int gx = -10; gx <= 10; gx++) {
            for (int gz = -10; gz <= 10; gz++) {
                Target a = planner.target(new Cell("world", gx, gz));
                Target b = planner.target(new Cell("world", gx + 1, gz));
                assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) >= 48);
            }
        }
    }

    @Test
    void nameIsStableAndAvoidsTakenNames() {
        Cell c = new Cell("world", 3, -7);
        String n = planner.name(c, s -> false);
        assertEquals(n, planner.name(c, s -> false));
        assertTrue(n.length() >= 3 && n.length() <= 32);
        String other = planner.name(c, s -> s.equals(n));
        assertNotEquals(n, other);
        assertTrue(other.startsWith(n));
    }
}
