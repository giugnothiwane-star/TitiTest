package com.ultimateautobuilder.core.geom;

public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public static Vec3 of(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 sub(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 mul(double k) {
        return new Vec3(x * k, y * k, z * k);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalize() {
        double l = length();
        return l < 1e-12 ? new Vec3(0, 1, 0) : mul(1.0 / l);
    }

    public Vec3 lerp(Vec3 o, double t) {
        return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    public Vec3 mirrorX() {
        return new Vec3(-x, y, z);
    }
}
