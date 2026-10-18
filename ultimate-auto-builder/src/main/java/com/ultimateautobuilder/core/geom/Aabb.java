package com.ultimateautobuilder.core.geom;

public record Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public static final Aabb EMPTY = new Aabb(0, 0, 0, -1, -1, -1);

    public static Aabb of(Vec3 a, Vec3 b) {
        return new Aabb(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z()),
                Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()));
    }

    public static Aabb around(Vec3 c, double rx, double ry, double rz) {
        return new Aabb(c.x() - rx, c.y() - ry, c.z() - rz, c.x() + rx, c.y() + ry, c.z() + rz);
    }

    public boolean isEmpty() {
        return maxX < minX || maxY < minY || maxZ < minZ;
    }

    public Aabb union(Aabb o) {
        if (isEmpty()) {
            return o;
        }
        if (o.isEmpty()) {
            return this;
        }
        return new Aabb(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ),
                Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
    }

    public Aabb intersect(Aabb o) {
        return new Aabb(Math.max(minX, o.minX), Math.max(minY, o.minY), Math.max(minZ, o.minZ),
                Math.min(maxX, o.maxX), Math.min(maxY, o.maxY), Math.min(maxZ, o.maxZ));
    }

    public Aabb inflate(double d) {
        return new Aabb(minX - d, minY - d, minZ - d, maxX + d, maxY + d, maxZ + d);
    }

    public Aabb translate(double dx, double dy, double dz) {
        return new Aabb(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public Aabb mirrorX() {
        return new Aabb(-maxX, minY, minZ, -minX, maxY, maxZ);
    }

    public Vec3 corner(int i) {
        return new Vec3((i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ);
    }
}
