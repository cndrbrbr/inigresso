package io.github.cndrbrbr.inigresso.geo;

/** Gerade Verbindung zwischen zwei Portalen in der X/Z-Ebene. */
public record Segment(Vec2 a, Vec2 b) {

    public double length() {
        return a.distance(b);
    }

    public boolean sharesEndpointWith(Segment o) {
        return a.equals(o.a) || a.equals(o.b) || b.equals(o.a) || b.equals(o.b);
    }
}
