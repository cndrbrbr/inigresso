package io.github.cndrbrbr.inigresso.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompassTest {

    @Test
    void himmelsrichtungen() {
        assertEquals("N", Compass.cardinal(0, -10));
        assertEquals("O", Compass.cardinal(10, 0));
        assertEquals("S", Compass.cardinal(0, 10));
        assertEquals("W", Compass.cardinal(-10, 0));
        assertEquals("NO", Compass.cardinal(10, -10));
        assertEquals("SW", Compass.cardinal(-10, 10));
    }

    @Test
    void blickrichtungAusYaw() {
        assertEquals(180, Compass.heading(0f), 1e-9);   // Yaw 0 = Süden
        assertEquals(270, Compass.heading(90f), 1e-9);  // Yaw 90 = Westen
        assertEquals(0, Compass.heading(180f), 1e-9);   // Norden
        assertEquals(90, Compass.heading(-90f), 1e-9);  // Osten
    }

    @Test
    void pfeilRelativZurBlickrichtung() {
        // Blick nach Norden, Ziel im Norden → geradeaus
        assertEquals("↑", Compass.arrow(180f, 0, -50));
        // Blick nach Norden, Ziel im Osten → rechts
        assertEquals("→", Compass.arrow(180f, 50, 0));
        // Blick nach Süden, Ziel im Osten → links
        assertEquals("←", Compass.arrow(0f, 50, 0));
        // Blick nach Osten, Ziel im Westen → hinten
        assertEquals("↓", Compass.arrow(-90f, -50, 0));
        // Blick nach Norden, Ziel Nordwest → schräg links
        assertEquals("↖", Compass.arrow(180f, -50, -50));
    }
}
