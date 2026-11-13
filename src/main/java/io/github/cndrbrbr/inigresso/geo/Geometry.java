package io.github.cndrbrbr.inigresso.geo;

/**
 * Geometrie für Links und Felder, ganzzahlig und damit ohne Rundungsfehler.
 * Siehe architecture.md, Abschnitt 4.
 */
public final class Geometry {

    private Geometry() {
    }

    /** Vorzeichen der Orientierung von c relativ zur Strecke a→b: 1 links, -1 rechts, 0 kollinear. */
    public static int orientation(Vec2 a, Vec2 b, Vec2 c) {
        return Long.signum(b.minus(a).cross(c.minus(a)));
    }

    /**
     * Ob sich zwei Links kreuzen. Links, die ein Portal gemeinsam haben, kreuzen sich nie
     * (Regel aus rules.md §6). Berührt oder überlappt ein Link einen anderen sonst irgendwo
     * (auch kollinear oder Endpunkt auf der Strecke), zählt das als Kreuzung.
     */
    public static boolean crosses(Segment s, Segment t) {
        if (s.sharesEndpointWith(t)) {
            return collinearOverlap(s, t);
        }
        int o1 = orientation(s.a(), s.b(), t.a());
        int o2 = orientation(s.a(), s.b(), t.b());
        int o3 = orientation(t.a(), t.b(), s.a());
        int o4 = orientation(t.a(), t.b(), s.b());
        if (o1 != o2 && o3 != o4 && o1 != 0 && o2 != 0 && o3 != 0 && o4 != 0) {
            return true;
        }
        return (o1 == 0 && onSegment(s, t.a()))
                || (o2 == 0 && onSegment(s, t.b()))
                || (o3 == 0 && onSegment(t, s.a()))
                || (o4 == 0 && onSegment(t, s.b()));
    }

    /**
     * Zwei Links mit gemeinsamem Portal liegen übereinander, wenn sie in dieselbe Richtung
     * zeigen (z. B. A–C und A–B, wobei B auf A–C liegt). Das ist verboten, weil der
     * kürzere Link durch das Portal B des längeren laufen würde.
     */
    private static boolean collinearOverlap(Segment s, Segment t) {
        if (orientation(s.a(), s.b(), t.a()) != 0 || orientation(s.a(), s.b(), t.b()) != 0) {
            return false;
        }
        if (s.equals(t) || (s.a().equals(t.b()) && s.b().equals(t.a()))) {
            return true;
        }
        Vec2 shared = s.a().equals(t.a()) || s.a().equals(t.b()) ? s.a() : s.b();
        Vec2 sOther = shared.equals(s.a()) ? s.b() : s.a();
        Vec2 tOther = shared.equals(t.a()) ? t.b() : t.a();
        Vec2 d1 = sOther.minus(shared);
        Vec2 d2 = tOther.minus(shared);
        return d1.x() * d2.x() + d1.z() * d2.z() > 0;
    }

    /** p liegt (kollinear vorausgesetzt) innerhalb der Begrenzung der Strecke. */
    private static boolean onSegment(Segment s, Vec2 p) {
        return Math.min(s.a().x(), s.b().x()) <= p.x() && p.x() <= Math.max(s.a().x(), s.b().x())
                && Math.min(s.a().z(), s.b().z()) <= p.z() && p.z() <= Math.max(s.a().z(), s.b().z());
    }

    /** Ob p im Dreieck liegt; Punkte auf dem Rand zählen als innen. */
    public static boolean contains(Triangle t, Vec2 p) {
        if (t.isDegenerate()) {
            return false;
        }
        int d1 = orientation(t.a(), t.b(), p);
        int d2 = orientation(t.b(), t.c(), p);
        int d3 = orientation(t.c(), t.a(), p);
        boolean hasNeg = d1 < 0 || d2 < 0 || d3 < 0;
        boolean hasPos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(hasNeg && hasPos);
    }

    /** Ob p echt innerhalb liegt (nicht auf dem Rand). Für "Portal liegt im gegnerischen Feld". */
    public static boolean strictlyContains(Triangle t, Vec2 p) {
        if (t.isDegenerate()) {
            return false;
        }
        int d1 = orientation(t.a(), t.b(), p);
        int d2 = orientation(t.b(), t.c(), p);
        int d3 = orientation(t.c(), t.a(), p);
        return (d1 > 0 && d2 > 0 && d3 > 0) || (d1 < 0 && d2 < 0 && d3 < 0);
    }

    /** Einfluss eines Feldes nach rules.md §8: Fläche / 100, aufgerundet. */
    public static long influence(Triangle t) {
        long doubled = Math.abs(t.doubleSignedArea());
        return (doubled + 199) / 200;
    }
}
