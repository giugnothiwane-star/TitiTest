package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.util.Noise;
import com.ultimateautobuilder.core.util.Rng;

import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;

/**
 * Arbres procéduraux : tronc effilé, ramification récursive pseudo-aléatoire (graine fixe)
 * et houppiers en amas d'ellipsoïdes bruités. Unités de conception : hauteur 30.
 */
public final class Trees {
    public static final double H = 30;

    public static final class Spec {
        public String log = "minecraft:oak_log[axis=y]";
        public Mat leaves = Mat.of("minecraft:oak_leaves[persistent=true]");
        public double trunkHeight = 0.55;   // fraction de H
        public double trunkRadius = 1.6;
        public int branches = 5;
        public int depth = 2;
        public double branchLen = 7;
        public double pitch = 40;
        public double leafRadius = 4.5;
        public boolean leaves_ = true;
        public long seed = 1;

        public Spec log(String v) { log = v; return this; }
        public Spec leaves(Mat v) { leaves = v; return this; }
        public Spec leaves(String v) { leaves = Mat.of(v); return this; }
        public Spec trunk(double heightFrac, double radius) { trunkHeight = heightFrac; trunkRadius = radius; return this; }
        public Spec branches(int n, int depth, double len, double pitch) { branches = n; this.depth = depth; branchLen = len; this.pitch = pitch; return this; }
        public Spec crown(double r) { leafRadius = r; return this; }
        public Spec bare() { leaves_ = false; return this; }
        public Spec seed(long s) { seed = s; return this; }
    }

    private Trees() {
    }

    public static Spec spec() {
        return new Spec();
    }

    /** Arbre feuillu ramifié. */
    public static Sculpt branched(Spec s) {
        Sculpt.Builder b = Sculpt.builder(H);
        Rng rng = new Rng(s.seed);
        double th = H * s.trunkHeight;
        b.add(frustum(new Vec3(0, -0.5, 0), new Vec3(0, th, 0), s.trunkRadius, s.trunkRadius * 0.6), s.log);
        // Contreforts racinaires
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90 + rng.range(-20, 20));
            b.add(limb(0, 2.0, 0, Math.cos(a) * s.trunkRadius * 2.2, 0, Math.sin(a) * s.trunkRadius * 2.2, s.trunkRadius * 0.5, 0.3), s.log);
        }
        grow(b, s, rng, new Vec3(0, th, 0), new Vec3(0, 1, 0), s.branchLen, s.trunkRadius * 0.6, s.depth);
        return b.build();
    }

    private static void grow(Sculpt.Builder b, Spec s, Rng rng, Vec3 from, Vec3 dir, double len, double r, int depth) {
        int n = depth == s.depth ? s.branches : Math.max(2, s.branches - 2);
        for (int i = 0; i < n; i++) {
            double yaw = Math.toRadians(i * 360.0 / n + rng.range(-25, 25));
            double pitch = Math.toRadians(s.pitch + rng.range(-12, 12));
            Vec3 d = new Vec3(Math.cos(yaw) * Math.sin(pitch), Math.cos(pitch), Math.sin(yaw) * Math.sin(pitch));
            d = d.add(dir.mul(0.4)).normalize();
            double l = len * rng.range(0.75, 1.15);
            Vec3 to = from.add(d.mul(l));
            b.add(limb(from, to, r, r * 0.65), s.log);
            if (depth > 1) {
                grow(b, s, rng, to, d, len * 0.62, r * 0.6, depth - 1);
            } else if (s.leaves_) {
                double lr = s.leafRadius * rng.range(0.8, 1.15);
                long ls = rng.nextLong();
                Mat base = s.leaves;
                Mat noisy = (x, y, z) -> Noise.value3(ls, x * 0.6, y * 0.6, z * 0.6) > 0.82 ? null : base.at(x, y, z);
                b.fill(ellipsoid(to.x(), to.y() + 0.5, to.z(), lr, lr * 0.75, lr), noisy);
            }
        }
    }

    /** Conifère : tronc droit et étages coniques de branchages. */
    public static Sculpt conifer(String log, String leaves, double slenderness, long seed) {
        Sculpt.Builder b = Sculpt.builder(H);
        b.add(frustum(new Vec3(0, -0.5, 0), new Vec3(0, H - 1, 0), 1.3, 0.4), log);
        int tiers = 7;
        for (int i = 0; i < tiers; i++) {
            double y0 = 5 + i * (H - 7) / tiers;
            double r = (7.5 - i * 0.9) * slenderness;
            b.fill(frustum(new Vec3(0, y0, 0), new Vec3(0, y0 + 4.2, 0), r, 0.6), leaves);
        }
        b.fill(frustum(new Vec3(0, H - 3.5, 0), new Vec3(0, H + 0.5, 0), 1.4, 0.2), leaves);
        return b.build();
    }

    /** Insère un cerisier dans une grille (décor des scènes japonaises). */
    public static void cherryInto(VoxelGrid g, int x, int y, int z, int height, long seed) {
        Sculpt tree = branched(spec().log("minecraft:cherry_log[axis=y]").leaves("minecraft:cherry_leaves[persistent=true]")
                .trunk(0.45, 1.3).branches(4, 2, 7, 55).crown(4.5).seed(seed));
        VoxelGrid t = tree.voxelize(height, false);
        g.paste(t, x, y, z, 0, true);
    }

    /** Insère un arbre feuillu générique (villes, jardins, îles). */
    public static void oakInto(VoxelGrid g, int x, int y, int z, int height, long seed, String log, String leaves) {
        Sculpt tree = branched(spec().log(log).leaves(leaves).trunk(0.5, 1.4).branches(4, 2, 6.5, 40).crown(4.4).seed(seed));
        VoxelGrid t = tree.voxelize(height, false);
        g.paste(t, x, y, z, 0, true);
    }

    public static void coniferInto(VoxelGrid g, int x, int y, int z, int height, long seed) {
        VoxelGrid t = conifer("minecraft:spruce_log[axis=y]", "minecraft:spruce_leaves[persistent=true]", 1.0, seed).voxelize(height, false);
        g.paste(t, x, y, z, 0, true);
    }

    public static void palmInto(VoxelGrid g, int x, int y, int z, int height, long seed) {
        VoxelGrid t = palm(seed).voxelize(height, false);
        g.paste(t, x, y, z, 0, true);
    }

    public static Sculpt palm(long seed) {
        Sculpt.Builder b = Sculpt.builder(H);
        Rng rng = new Rng(seed);
        Vec3 prev = new Vec3(0, -0.5, 0);
        double lean = rng.range(3, 6);
        for (int i = 1; i <= 8; i++) {
            double t = i / 8.0;
            Vec3 p = new Vec3(Math.pow(t, 2) * lean, t * (H - 6), 0);
            b.add(limb(prev, p, 1.3 - t * 0.4, 1.2 - t * 0.4), "minecraft:jungle_log[axis=y]");
            prev = p;
        }
        Vec3 top = prev;
        for (int i = 0; i < 7; i++) {
            double a = Math.toRadians(i * 360.0 / 7 + rng.range(-10, 10));
            Vec3 mid = top.add(new Vec3(Math.cos(a) * 5, 2.0, Math.sin(a) * 5));
            Vec3 end = top.add(new Vec3(Math.cos(a) * 9.5, -2.5, Math.sin(a) * 9.5));
            b.add(limb(top, mid, 0.9, 0.8), "minecraft:jungle_leaves[persistent=true]");
            b.add(limb(mid, end, 0.8, 0.5), "minecraft:jungle_leaves[persistent=true]");
        }
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(i * 120 + 30);
            b.add(sphere(top.x() + Math.cos(a) * 1.4, top.y() - 1.2, top.z() + Math.sin(a) * 1.4, 0.8), "minecraft:brown_concrete");
        }
        return b.build();
    }
}
