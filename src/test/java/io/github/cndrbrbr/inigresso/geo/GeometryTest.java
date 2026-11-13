package io.github.cndrbrbr.inigresso.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeometryTest {

    private static Vec2 p(long x, long z) {
        return new Vec2(x, z);
    }

    private static Segment s(long ax, long az, long bx, long bz) {
        return new Segment(p(ax, az), p(bx, bz));
    }

    @Test
    void echteKreuzung() {
        assertTrue(Geometry.crosses(s(0, 0, 100, 100), s(0, 100, 100, 0)));
    }

    @Test
    void parallelUndGetrennt() {
        assertFalse(Geometry.crosses(s(0, 0, 100, 0), s(0, 10, 100, 10)));
    }

    @Test
    void wuerdeSichErstVerlaengertSchneiden() {
        assertFalse(Geometry.crosses(s(0, 0, 10, 10), s(20, 0, 15, 5)));
    }

    @Test
    void gemeinsamesPortalIstKeineKreuzung() {
        assertFalse(Geometry.crosses(s(0, 0, 100, 0), s(0, 0, 0, 100)));
        assertFalse(Geometry.crosses(s(0, 0, 100, 0), s(100, 0, 50, 80)));
    }

    @Test
    void gemeinsamesPortalGegenteiligeRichtungIstErlaubt() {
        // A–B und A–C liegen auf einer Geraden, zeigen aber in entgegengesetzte Richtungen.
        assertFalse(Geometry.crosses(s(0, 0, 100, 0), s(0, 0, -100, 0)));
    }

    @Test
    void gemeinsamesPortalUebereinanderIstVerboten() {
        // A–C läuft über B, A–B liegt darauf.
        assertTrue(Geometry.crosses(s(0, 0, 100, 0), s(0, 0, 50, 0)));
    }

    @Test
    void derselbeLinkInBeideRichtungen() {
        assertTrue(Geometry.crosses(s(0, 0, 100, 0), s(100, 0, 0, 0)));
    }

    @Test
    void endpunktAufFremdemLinkZaehltAlsKreuzung() {
        // Portal liegt genau auf einem bestehenden Link.
        assertTrue(Geometry.crosses(s(0, 0, 100, 0), s(50, 0, 50, 80)));
    }

    @Test
    void kollinearUeberlappend() {
        assertTrue(Geometry.crosses(s(0, 0, 100, 0), s(50, 0, 150, 0)));
    }

    @Test
    void kollinearGetrennt() {
        assertFalse(Geometry.crosses(s(0, 0, 100, 0), s(150, 0, 250, 0)));
    }

    @Test
    void flaecheUndEinfluss() {
        Triangle t = new Triangle(p(0, 0), p(100, 0), p(0, 100));
        assertEquals(5000.0, t.area());
        assertEquals(50, Geometry.influence(t));
        // 0.5 Blöcke² wird auf 1 Einfluss aufgerundet
        assertEquals(1, Geometry.influence(new Triangle(p(0, 0), p(1, 0), p(0, 1))));
        // Reihenfolge der Ecken ist egal
        assertEquals(5000.0, new Triangle(p(0, 100), p(100, 0), p(0, 0)).area());
    }

    @Test
    void entartetesDreieck() {
        Triangle t = new Triangle(p(0, 0), p(50, 0), p(100, 0));
        assertTrue(t.isDegenerate());
        assertEquals(0, Geometry.influence(t));
        assertFalse(Geometry.contains(t, p(50, 0)));
    }

    @Test
    void punktImDreieck() {
        Triangle t = new Triangle(p(0, 0), p(100, 0), p(0, 100));
        assertTrue(Geometry.contains(t, p(10, 10)));
        assertTrue(Geometry.contains(t, p(50, 0)), "Rand zählt für contains");
        assertTrue(Geometry.contains(t, p(0, 0)), "Ecke zählt für contains");
        assertFalse(Geometry.contains(t, p(60, 60)));
        assertTrue(Geometry.strictlyContains(t, p(10, 10)));
        assertFalse(Geometry.strictlyContains(t, p(50, 0)));
        assertFalse(Geometry.strictlyContains(t, p(0, 0)));
    }

    @Test
    void grosseKoordinatenOhneUeberlauf() {
        long far = 30_000_000;
        assertTrue(Geometry.crosses(s(-far, -far, far, far), s(-far, far, far, -far)));
        assertEquals((double) far * far, new Triangle(p(-far, 0), p(far, 0), p(0, far)).area());
    }
}
