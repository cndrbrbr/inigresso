package io.github.cndrbrbr.inigresso.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScannerTest {

    private static final double E = 1e-9;

    @Test
    void nordenObenOhneDrehung() {
        var p = new ScannerProjection(100, 200, 123, 256, false); // 2 Blöcke je Pixel
        assertArrayEquals(new double[] {64, 64}, p.toScreen(100, 200), E);
        assertArrayEquals(new double[] {64, 54}, p.toScreen(100, 180), E);  // Norden = oben
        assertArrayEquals(new double[] {74, 64}, p.toScreen(120, 200), E);  // Osten = rechts
    }

    @Test
    void blickrichtungZeigtNachOben() {
        // Blick nach Osten: Punkt im Osten liegt oben, Punkt im Süden rechts.
        var p = new ScannerProjection(0, 0, 90, 128, true);
        assertArrayEquals(new double[] {64, 54}, p.toScreen(10, 0), 1e-6);
        assertArrayEquals(new double[] {74, 64}, p.toScreen(0, 10), 1e-6);
        // Blick nach Süden: Süden oben, Westen rechts.
        var s = new ScannerProjection(0, 0, 180, 128, true);
        assertArrayEquals(new double[] {64, 54}, s.toScreen(0, 10), 1e-6);
        assertArrayEquals(new double[] {74, 64}, s.toScreen(-10, 0), 1e-6);
        // Blick nach Norden gedreht = ungedreht
        var n = new ScannerProjection(5, 5, 0, 128, true);
        assertArrayEquals(new ScannerProjection(5, 5, 0, 128, false).toScreen(30, -12), n.toScreen(30, -12), 1e-6);
    }

    @Test
    void zielAmRandFesthalten() {
        double[] in = {70, 60};
        assertArrayEquals(in, ScannerProjection.clampToEdge(in, 4), E);
        double[] out = ScannerProjection.clampToEdge(new double[] {64 + 600, 64}, 4);
        assertArrayEquals(new double[] {124, 64}, out, E);
        double[] diag = ScannerProjection.clampToEdge(new double[] {64 - 300, 64 - 150}, 4);
        assertArrayEquals(new double[] {4, 34}, diag, E);
        assertTrue(ScannerProjection.visible(new double[] {-2, 5}, 3));
        assertFalse(ScannerProjection.visible(new double[] {-5, 5}, 3));
    }

    @Test
    void linieWirdBeschnitten() {
        var img = new ScannerImage();
        img.fill((byte) 0);
        img.line(-1000, 10, 1000, 10, (byte) 5);
        for (int x = 0; x < ScannerImage.SIZE; x++) {
            assertEquals(5, img.get(x, 10));
        }
        assertEquals(0, img.get(5, 11));
        // komplett außerhalb: nichts
        var empty = new ScannerImage();
        empty.line(-50, -50, -10, 300, (byte) 7);
        for (int y = 0; y < ScannerImage.SIZE; y++) {
            for (int x = 0; x < ScannerImage.SIZE; x++) {
                assertEquals(0, empty.get(x, y));
            }
        }
    }

    @Test
    void dreieckGerastert() {
        var img = new ScannerImage();
        img.triangle(0, 0, 100, 0, 0, 100, (byte) 3, true);
        assertEquals(3, img.get(10, 10));   // gerade Summe → gesetzt
        assertEquals(0, img.get(11, 10));   // ungerade → frei (Raster)
        assertEquals(0, img.get(90, 90));   // außerhalb
        var full = new ScannerImage();
        full.triangle(0, 0, 100, 0, 0, 100, (byte) 3, false);
        assertEquals(3, full.get(11, 10));
    }

    @Test
    void punktUndRing() {
        var img = new ScannerImage();
        img.disc(64, 64, 2, (byte) 9);
        assertEquals(9, img.get(64, 64));
        assertEquals(9, img.get(65, 64));
        assertEquals(0, img.get(68, 64));
        img.disc(-1, -1, 2, (byte) 9); // am Rand, darf nicht werfen
        img.ring(0, 0, 10, (byte) 4);
    }
}
