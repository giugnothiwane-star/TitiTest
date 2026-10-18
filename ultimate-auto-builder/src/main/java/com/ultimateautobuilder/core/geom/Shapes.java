package com.ultimateautobuilder.core.geom;

/** Primitives volumiques et opérateurs de composition. */
public final class Shapes {
    private static final double BIG = 1e6;

    private Shapes() {
    }

    // ---------------------------------------------------------------- fabriques

    public static Shape ellipsoid(double cx, double cy, double cz, double rx, double ry, double rz) {
        return new Ellipsoid(new Vec3(cx, cy, cz), rx, ry, rz);
    }

    public static Shape sphere(double cx, double cy, double cz, double r) {
        return new Ellipsoid(new Vec3(cx, cy, cz), r, r, r);
    }

    public static Shape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new Box(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1));
    }

    /** Pavé centré en (cx, cy, cz) de demi-dimensions (hx, hy, hz). */
    public static Shape boxC(double cx, double cy, double cz, double hx, double hy, double hz) {
        return new Box(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
    }

    /** Membre effilé à extrémités arrondies (bras, jambes, queues, cous). */
    public static Shape limb(Vec3 a, Vec3 b, double ra, double rb) {
        return new Tube(a, b, ra, rb, true);
    }

    public static Shape limb(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb) {
        return new Tube(new Vec3(ax, ay, az), new Vec3(bx, by, bz), ra, rb, true);
    }

    /** Cylindre ou tronc de cône à extrémités plates (rb = 0 pour un cône). */
    public static Shape frustum(Vec3 a, Vec3 b, double ra, double rb) {
        return new Tube(a, b, ra, rb, false);
    }

    public static Shape cylinderY(double cx, double y0, double cz, double height, double r) {
        return new Tube(new Vec3(cx, y0, cz), new Vec3(cx, y0 + height, cz), r, r, false);
    }

    public static Shape coneY(double cx, double y0, double cz, double height, double r) {
        return new Tube(new Vec3(cx, y0, cz), new Vec3(cx, y0 + height, cz), r, 0, false);
    }

    public static Shape torusY(double cx, double cy, double cz, double major, double minor) {
        return new Torus(new Vec3(cx, cy, cz), major, minor);
    }

    /** Plaque triangulaire épaisse (ailes, nageoires, flammes, lames). */
    public static Shape tri(Vec3 a, Vec3 b, Vec3 c, double thickness) {
        return new Tri(a, b, c, thickness);
    }

    /** Demi-espace du côté opposé à la normale. */
    public static Shape halfSpace(Vec3 point, Vec3 normal) {
        return new HalfSpace(point, normal.normalize());
    }

    public static Shape union(Shape... shapes) {
        return new Union(shapes);
    }

    // ---------------------------------------------------------------- primitives

    public static final class Ellipsoid implements Shape {
        private final Vec3 c;
        private final double rx, ry, rz;

        Ellipsoid(Vec3 c, double rx, double ry, double rz) {
            this.c = c;
            this.rx = Math.max(rx, 1e-6);
            this.ry = Math.max(ry, 1e-6);
            this.rz = Math.max(rz, 1e-6);
        }

        @Override
        public boolean contains(double x, double y, double z) {
            double dx = (x - c.x()) / rx, dy = (y - c.y()) / ry, dz = (z - c.z()) / rz;
            return dx * dx + dy * dy + dz * dz <= 1.0;
        }

        @Override
        public Aabb bounds() {
            return Aabb.around(c, rx, ry, rz);
        }

        @Override
        public Shape thicken(double m) {
            return new Ellipsoid(c, Math.max(rx, m), Math.max(ry, m), Math.max(rz, m));
        }
    }

    public static final class Box implements Shape {
        private final double x0, y0, z0, x1, y1, z1;

        Box(double x0, double y0, double z0, double x1, double y1, double z1) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }

        @Override
        public Aabb bounds() {
            return new Aabb(x0, y0, z0, x1, y1, z1);
        }

        @Override
        public Shape thicken(double m) {
            double cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, cz = (z0 + z1) / 2;
            double hx = Math.max((x1 - x0) / 2, m), hy = Math.max((y1 - y0) / 2, m), hz = Math.max((z1 - z0) / 2, m);
            return new Box(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
        }
    }

    public static final class Tube implements Shape {
        private final Vec3 a, b, ab;
        private final double ra, rb, len2;
        private final boolean roundCaps;

        Tube(Vec3 a, Vec3 b, double ra, double rb, boolean roundCaps) {
            this.a = a;
            this.b = b;
            this.ab = b.sub(a);
            this.ra = Math.max(ra, 0);
            this.rb = Math.max(rb, 0);
            this.len2 = Math.max(ab.dot(ab), 1e-12);
            this.roundCaps = roundCaps;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            double px = x - a.x(), py = y - a.y(), pz = z - a.z();
            double t = (px * ab.x() + py * ab.y() + pz * ab.z()) / len2;
            if (!roundCaps && (t < 0 || t > 1)) {
                return false;
            }
            double tc = Math.max(0, Math.min(1, t));
            double qx = px - ab.x() * tc, qy = py - ab.y() * tc, qz = pz - ab.z() * tc;
            double r = ra + (rb - ra) * tc;
            return qx * qx + qy * qy + qz * qz <= r * r;
        }

        @Override
        public Aabb bounds() {
            double r = Math.max(ra, rb);
            return Aabb.of(a, b).inflate(r);
        }

        @Override
        public Shape thicken(double m) {
            return new Tube(a, b, Math.max(ra, m), Math.max(rb, m), roundCaps);
        }
    }

    public static final class Torus implements Shape {
        private final Vec3 c;
        private final double major, minor;

        Torus(Vec3 c, double major, double minor) {
            this.c = c;
            this.major = major;
            this.minor = minor;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            double dx = x - c.x(), dy = y - c.y(), dz = z - c.z();
            double q = Math.sqrt(dx * dx + dz * dz) - major;
            return q * q + dy * dy <= minor * minor;
        }

        @Override
        public Aabb bounds() {
            return Aabb.around(c, major + minor, minor, major + minor);
        }

        @Override
        public Shape thicken(double m) {
            return new Torus(c, major, Math.max(minor, m));
        }
    }

    public static final class Tri implements Shape {
        private final Vec3 a, b, c;
        private final double t;

        Tri(Vec3 a, Vec3 b, Vec3 c, double thickness) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.t = thickness;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            Vec3 p = new Vec3(x, y, z);
            Vec3 q = closest(p);
            Vec3 d = p.sub(q);
            return d.dot(d) <= t * t;
        }

        // Point le plus proche sur le triangle (Ericson, Real-Time Collision Detection 5.1.5).
        private Vec3 closest(Vec3 p) {
            Vec3 ab = b.sub(a), ac = c.sub(a), ap = p.sub(a);
            double d1 = ab.dot(ap), d2 = ac.dot(ap);
            if (d1 <= 0 && d2 <= 0) return a;
            Vec3 bp = p.sub(b);
            double d3 = ab.dot(bp), d4 = ac.dot(bp);
            if (d3 >= 0 && d4 <= d3) return b;
            double vc = d1 * d4 - d3 * d2;
            if (vc <= 0 && d1 >= 0 && d3 <= 0) return a.add(ab.mul(d1 / (d1 - d3)));
            Vec3 cp = p.sub(c);
            double d5 = ab.dot(cp), d6 = ac.dot(cp);
            if (d6 >= 0 && d5 <= d6) return c;
            double vb = d5 * d2 - d1 * d6;
            if (vb <= 0 && d2 >= 0 && d6 <= 0) return a.add(ac.mul(d2 / (d2 - d6)));
            double va = d3 * d6 - d5 * d4;
            if (va <= 0 && (d4 - d3) >= 0 && (d5 - d6) >= 0) {
                return b.add(c.sub(b).mul((d4 - d3) / ((d4 - d3) + (d5 - d6))));
            }
            double denom = 1.0 / (va + vb + vc);
            double v = vb * denom, w = vc * denom;
            return a.add(ab.mul(v)).add(ac.mul(w));
        }

        @Override
        public Aabb bounds() {
            return Aabb.of(a, b).union(Aabb.of(c, c)).inflate(t);
        }

        @Override
        public Shape thicken(double m) {
            return new Tri(a, b, c, Math.max(t, m));
        }
    }

    public static final class HalfSpace implements Shape {
        private final Vec3 p, n;

        HalfSpace(Vec3 p, Vec3 n) {
            this.p = p;
            this.n = n;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return (x - p.x()) * n.x() + (y - p.y()) * n.y() + (z - p.z()) * n.z() <= 0;
        }

        @Override
        public Aabb bounds() {
            return new Aabb(-BIG, -BIG, -BIG, BIG, BIG, BIG);
        }
    }

    // ---------------------------------------------------------------- opérateurs

    public static final class Rotated implements Shape {
        private final Shape inner;
        private final Vec3 pivot;
        private final double[] m; // matrice de rotation (ligne par ligne)
        private final double yaw, pitch, roll;

        Rotated(Shape inner, double yawDeg, double pitchDeg, double rollDeg, Vec3 pivot) {
            this.inner = inner;
            this.pivot = pivot;
            this.yaw = yawDeg;
            this.pitch = pitchDeg;
            this.roll = rollDeg;
            double cy = Math.cos(Math.toRadians(yawDeg)), sy = Math.sin(Math.toRadians(yawDeg));
            double cp = Math.cos(Math.toRadians(pitchDeg)), sp = Math.sin(Math.toRadians(pitchDeg));
            double cr = Math.cos(Math.toRadians(rollDeg)), sr = Math.sin(Math.toRadians(rollDeg));
            // R = Ry(yaw) * Rx(pitch) * Rz(roll)
            double[] rz = {cr, -sr, 0, sr, cr, 0, 0, 0, 1};
            double[] rx = {1, 0, 0, 0, cp, -sp, 0, sp, cp};
            double[] ry = {cy, 0, sy, 0, 1, 0, -sy, 0, cy};
            this.m = mul(ry, mul(rx, rz));
        }

        private static double[] mul(double[] p, double[] q) {
            double[] r = new double[9];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    r[i * 3 + j] = p[i * 3] * q[j] + p[i * 3 + 1] * q[3 + j] + p[i * 3 + 2] * q[6 + j];
                }
            }
            return r;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            double dx = x - pivot.x(), dy = y - pivot.y(), dz = z - pivot.z();
            // Rotation inverse = transposée.
            double ix = m[0] * dx + m[3] * dy + m[6] * dz;
            double iy = m[1] * dx + m[4] * dy + m[7] * dz;
            double iz = m[2] * dx + m[5] * dy + m[8] * dz;
            return inner.contains(ix + pivot.x(), iy + pivot.y(), iz + pivot.z());
        }

        @Override
        public Aabb bounds() {
            Aabb ib = inner.bounds();
            if (ib.maxX() - ib.minX() > BIG) {
                return ib;
            }
            Aabb out = Aabb.EMPTY;
            for (int i = 0; i < 8; i++) {
                Vec3 p = ib.corner(i).sub(pivot);
                Vec3 r = new Vec3(m[0] * p.x() + m[1] * p.y() + m[2] * p.z(),
                        m[3] * p.x() + m[4] * p.y() + m[5] * p.z(),
                        m[6] * p.x() + m[7] * p.y() + m[8] * p.z()).add(pivot);
                out = out.union(Aabb.of(r, r));
            }
            return out;
        }

        @Override
        public Shape thicken(double minRadius) {
            return new Rotated(inner.thicken(minRadius), yaw, pitch, roll, pivot);
        }
    }

    public static final class Translated implements Shape {
        private final Shape inner;
        private final double dx, dy, dz;

        Translated(Shape inner, double dx, double dy, double dz) {
            this.inner = inner;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return inner.contains(x - dx, y - dy, z - dz);
        }

        @Override
        public Aabb bounds() {
            return inner.bounds().translate(dx, dy, dz);
        }

        @Override
        public Shape thicken(double m) {
            return new Translated(inner.thicken(m), dx, dy, dz);
        }
    }

    public static final class MirrorX implements Shape {
        private final Shape inner;

        MirrorX(Shape inner) {
            this.inner = inner;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return inner.contains(-x, y, z);
        }

        @Override
        public Aabb bounds() {
            return inner.bounds().mirrorX();
        }

        @Override
        public Shape thicken(double m) {
            return new MirrorX(inner.thicken(m));
        }
    }

    public static final class Intersection implements Shape {
        private final Shape a, b;

        Intersection(Shape a, Shape b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return a.contains(x, y, z) && b.contains(x, y, z);
        }

        @Override
        public Aabb bounds() {
            return a.bounds().intersect(b.bounds());
        }

        @Override
        public Shape thicken(double m) {
            return new Intersection(a.thicken(m), b);
        }
    }

    public static final class Difference implements Shape {
        private final Shape a, b;

        Difference(Shape a, Shape b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return a.contains(x, y, z) && !b.contains(x, y, z);
        }

        @Override
        public Aabb bounds() {
            return a.bounds();
        }

        @Override
        public Shape thicken(double m) {
            return new Difference(a.thicken(m), b);
        }
    }

    public static final class Union implements Shape {
        private final Shape[] parts;

        Union(Shape[] parts) {
            this.parts = parts.clone();
        }

        @Override
        public boolean contains(double x, double y, double z) {
            for (Shape s : parts) {
                if (s.contains(x, y, z)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Aabb bounds() {
            Aabb out = Aabb.EMPTY;
            for (Shape s : parts) {
                out = out.union(s.bounds());
            }
            return out;
        }

        @Override
        public Shape thicken(double m) {
            Shape[] t = new Shape[parts.length];
            for (int i = 0; i < parts.length; i++) {
                t[i] = parts[i].thicken(m);
            }
            return new Union(t);
        }
    }
}
