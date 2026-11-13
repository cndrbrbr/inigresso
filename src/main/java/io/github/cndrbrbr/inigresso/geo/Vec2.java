package io.github.cndrbrbr.inigresso.geo;

/** Punkt bzw. Vektor in der X/Z-Ebene (Blockkoordinaten, Y wird ignoriert). */
public record Vec2(long x, long z) {

    public Vec2 minus(Vec2 o) {
        return new Vec2(x - o.x, z - o.z);
    }

    /** Z-Komponente des Kreuzprodukts (this × o). */
    public long cross(Vec2 o) {
        return x * o.z - z * o.x;
    }

    public double distance(Vec2 o) {
        return Math.hypot(x - o.x, z - o.z);
    }
}
