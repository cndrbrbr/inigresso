package io.github.cndrbrbr.inigresso.model;

import io.github.cndrbrbr.inigresso.geo.Vec2;

/** Blockposition ohne Bukkit-Abhängigkeit. */
public record BlockPos(String world, int x, int y, int z) {

    public Vec2 xz() {
        return new Vec2(x, z);
    }

    /** Waagerechter Abstand (X/Z), unendlich bei verschiedenen Welten. */
    public double horizontalDistance(BlockPos o) {
        if (!world.equals(o.world)) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.hypot(x - o.x, z - o.z);
    }

    /** Liegt o in einem Würfel mit Kantenlänge 2r+1 um diese Position (gleiche Welt)? */
    public boolean withinBox(BlockPos o, int r) {
        return world.equals(o.world) && Math.abs(x - o.x) <= r && Math.abs(y - o.y) <= r + 2
                && Math.abs(z - o.z) <= r;
    }

    public long chunkKey() {
        return chunkKey(x >> 4, z >> 4);
    }

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    @Override
    public String toString() {
        return world + " " + x + " " + y + " " + z;
    }
}
