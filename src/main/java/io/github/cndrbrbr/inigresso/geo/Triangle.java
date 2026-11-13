package io.github.cndrbrbr.inigresso.geo;

/** Feld aus drei Portalen in der X/Z-Ebene. */
public record Triangle(Vec2 a, Vec2 b, Vec2 c) {

    /** Doppelte Fläche mit Vorzeichen (positiv = gegen den Uhrzeigersinn). */
    public long doubleSignedArea() {
        return b.minus(a).cross(c.minus(a));
    }

    /** Fläche in Blöcken². */
    public double area() {
        return Math.abs(doubleSignedArea()) / 2.0;
    }

    public boolean isDegenerate() {
        return doubleSignedArea() == 0;
    }
}
