package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Shape;
import com.ultimateautobuilder.core.geom.Vec3;

import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** Briques anatomiques partagées par les créatures, Pokémon, animaux et monstres. */
public final class Creature {
    public static final String BLACK = "minecraft:black_concrete";
    public static final String WHITE = "minecraft:white_concrete";
    public static final String CREAM = "minecraft:end_stone";

    private Creature() {
    }

    /** Paire d'yeux ronds (noir + reflet blanc) peinte sur une surface orientée vers +Z. */
    public static void eyes(Sculpt.Builder b, double x, double y, double z, double r, String iris) {
        b.paintSym(ellipsoid(x, y, z, r * 0.85, r * 1.1, r), iris);
        b.paintSym(sphere(x - r * 0.25, y + r * 0.4, z + r * 0.2, r * 0.38), WHITE);
    }

    /** Yeux avec sclère blanche (Pokémon « réalistes », dragons). */
    public static void eyesWhite(Sculpt.Builder b, double x, double y, double z, double r, String iris) {
        b.paintSym(ellipsoid(x, y, z, r, r * 1.05, r), WHITE);
        b.paintSym(ellipsoid(x - r * 0.15, y, z + r * 0.2, r * 0.55, r * 0.8, r), iris);
    }

    /** Patte de quadrupède : cuisse, jambe et pied. */
    public static void leg(Sculpt.Builder b, double x, double z, double hip, double r, String mat, String foot) {
        b.sym(limb(x, hip, z, x, 0.9, z + 0.2, r, r * 0.8), mat);
        if (foot != null) {
            b.sym(ellipsoid(x, 0.7, z + 0.5, r * 0.95, 0.75, r * 1.25), foot);
        }
    }

    /** Queue articulée passant par une suite de points avec rayon décroissant. */
    public static void tail(Sculpt.Builder b, String mat, double r0, double r1, Vec3... pts) {
        for (int i = 0; i + 1 < pts.length; i++) {
            double t0 = (double) i / (pts.length - 1), t1 = (double) (i + 1) / (pts.length - 1);
            b.add(limb(pts[i], pts[i + 1], r0 + (r1 - r0) * t0, r0 + (r1 - r0) * t1), mat);
        }
    }

    public static Shape tube(double r0, double r1, Vec3... pts) {
        Shape[] parts = new Shape[pts.length - 1];
        for (int i = 0; i + 1 < pts.length; i++) {
            double t0 = (double) i / (pts.length - 1), t1 = (double) (i + 1) / (pts.length - 1);
            parts[i] = limb(pts[i], pts[i + 1], r0 + (r1 - r0) * t0, r0 + (r1 - r0) * t1);
        }
        return com.ultimateautobuilder.core.geom.Shapes.union(parts);
    }

    /** Aile membraneuse : membrane triangulée et nervures, symétrique. */
    public static void wing(Sculpt.Builder b, Vec3 root, Vec3 tip, Vec3 back, Vec3 low, String membrane, String bone) {
        b.sym(tri(root, tip, back, 0.45), membrane);
        b.sym(tri(root, back, low, 0.45), membrane);
        b.sym(limb(root, tip, 0.6, 0.35), bone);
        b.sym(limb(tip, back, 0.4, 0.3), bone);
    }

    /** Flamme (queue de Salamèche, torches) : trois ellipsoïdes imbriqués. */
    public static void flame(Sculpt.Builder b, double x, double y, double z, double s) {
        b.add(ellipsoid(x, y, z, 1.1 * s, 2.0 * s, 1.1 * s), "minecraft:orange_concrete");
        b.add(ellipsoid(x, y - 0.3 * s, z, 0.7 * s, 1.3 * s, 0.7 * s), "minecraft:yellow_concrete");
        b.add(frustum(new Vec3(x, y + 1.4 * s, z), new Vec3(x, y + 3.0 * s, z), 0.6 * s, 0.05), "minecraft:red_concrete");
    }

    /** Corne ou griffe conique. */
    public static Shape horn(double x0, double y0, double z0, double x1, double y1, double z1, double r) {
        return frustum(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), r, 0.05);
    }
}
