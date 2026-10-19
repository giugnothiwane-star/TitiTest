package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;

/**
 * Gabarit paramétrique de quadrupède (mammifères, reptiles, créatures).
 * Le corps est orienté selon Z, tête vers +Z.
 */
public final class Quadruped {
    public enum Ears { NONE, POINTY, ROUND, FLOPPY, LONG, HORNS_CURVED, ANTLERS, BIG_FLAP }

    public enum Tail { NONE, THIN, BUSHY, TUFT, THICK, CURL }

    /** Points clés calculés, transmis aux détails spécifiques. */
    public record Frame(Vec3 bodyCenter, Vec3 neckBase, Vec3 headCenter, Vec3 snoutTip, Vec3 tailBase, double headR) {
    }

    public static final class Spec {
        public double bodyLen = 6, bodyR = 3, bodyH = 3.2, legLen = 6, legR = 1.1;
        public double neckLen = 2.5, neckPitch = 35, headR = 2.6, snoutLen = 2.2, snoutR = 1.3;
        public Mat body = Mat.of("minecraft:brown_terracotta");
        public Mat belly;
        public String legs, hooves, head, snout, nose = "minecraft:black_concrete", eyes = "minecraft:black_concrete";
        public Ears ears = Ears.POINTY;
        public Tail tail = Tail.THIN;
        public String earMat, tailMat;
        public double tailLen = 5;
        public final List<BiConsumer<Sculpt.Builder, Frame>> extras = new ArrayList<>();

        public Spec size(double bodyLen, double bodyR, double bodyH, double legLen, double legR) {
            this.bodyLen = bodyLen; this.bodyR = bodyR; this.bodyH = bodyH; this.legLen = legLen; this.legR = legR;
            return this;
        }

        public Spec neck(double len, double pitch) { neckLen = len; neckPitch = pitch; return this; }
        public Spec head(double r, double snoutLen, double snoutR) { headR = r; this.snoutLen = snoutLen; this.snoutR = snoutR; return this; }
        public Spec body(Mat m) { body = m; return this; }
        public Spec body(String s) { body = Mat.of(s); return this; }
        public Spec belly(String s) { belly = Mat.of(s); return this; }
        public Spec legs(String s) { legs = s; return this; }
        public Spec hooves(String s) { hooves = s; return this; }
        public Spec headMat(String s) { head = s; return this; }
        public Spec snoutMat(String s) { snout = s; return this; }
        public Spec nose(String s) { nose = s; return this; }
        public Spec eyes(String s) { eyes = s; return this; }
        public Spec ears(Ears e, String mat) { ears = e; earMat = mat; return this; }
        public Spec tail(Tail t, String mat, double len) { tail = t; tailMat = mat; tailLen = len; return this; }
        public Spec extra(BiConsumer<Sculpt.Builder, Frame> e) { extras.add(e); return this; }
    }

    private Quadruped() {
    }

    public static Spec spec() {
        return new Spec();
    }

    public static Sculpt build(Spec q) {
        double by = q.legLen + q.bodyH * 0.75;
        Sculpt.Builder b = Sculpt.builder(by + q.bodyH + q.neckLen + q.headR * 2);
        String bodyBase = q.body.at(0, by, 0);
        String legs = q.legs != null ? q.legs : bodyBase;
        String headMat = q.head != null ? q.head : bodyBase;
        // Corps
        b.add(ellipsoid(0, by, 0, q.bodyR, q.bodyH, q.bodyLen), q.body);
        if (q.belly != null) {
            b.paint(ellipsoid(0, by - q.bodyH * 0.45, 0, q.bodyR * 0.85, q.bodyH * 0.6, q.bodyLen * 0.85), q.belly);
        }
        // Pattes
        double lx = q.bodyR * 0.58;
        for (double lz : new double[]{q.bodyLen * 0.62, -q.bodyLen * 0.62}) {
            b.sym(limb(lx, by - q.bodyH * 0.3, lz, lx, 0.7, lz + 0.2, q.legR * 1.15, q.legR * 0.9), legs);
            if (q.hooves != null) {
                b.sym(frustum(new Vec3(lx, 1.3, lz + 0.2), new Vec3(lx, 0, lz + 0.3), q.legR * 1.0, q.legR * 1.05), q.hooves);
            }
        }
        // Cou et tête
        double pitch = Math.toRadians(q.neckPitch);
        Vec3 neckBase = new Vec3(0, by + q.bodyH * 0.35, q.bodyLen * 0.8);
        Vec3 dir = new Vec3(0, Math.sin(pitch), Math.cos(pitch));
        Vec3 headC = neckBase.add(dir.mul(q.neckLen + q.headR * 0.6));
        b.add(limb(neckBase, headC, Math.min(q.bodyR, q.bodyH) * 0.62, q.headR * 0.7), q.body);
        b.add(sphere(headC.x(), headC.y(), headC.z(), q.headR), headMat);
        Vec3 snoutTip = headC.add(new Vec3(0, -q.headR * 0.3, q.headR * 0.6 + q.snoutLen));
        if (q.snoutLen > 0) {
            b.add(limb(headC.add(new Vec3(0, -q.headR * 0.2, q.headR * 0.4)), snoutTip, q.snoutR * 1.05, q.snoutR * 0.85),
                    q.snout != null ? q.snout : headMat);
            b.paint(sphere(snoutTip.x(), snoutTip.y() + q.snoutR * 0.3, snoutTip.z() + q.snoutR * 0.7, q.snoutR * 0.45), q.nose);
        }
        // Yeux
        double ex = q.headR * 0.55;
        Vec3 eye = headC.add(new Vec3(ex, q.headR * 0.3, q.headR * 0.72));
        b.paintSym(sphere(eye.x(), eye.y(), eye.z(), q.headR * 0.24), q.eyes);
        // Oreilles
        String earMat = q.earMat != null ? q.earMat : headMat;
        Vec3 earBase = headC.add(new Vec3(q.headR * 0.55, q.headR * 0.75, -q.headR * 0.1));
        switch (q.ears) {
            case POINTY -> b.sym(frustum(earBase, earBase.add(new Vec3(0.3, q.headR * 0.9, -0.2)), q.headR * 0.35, 0.1), earMat);
            case ROUND -> b.sym(sphere(earBase.x(), earBase.y() + 0.2, earBase.z(), q.headR * 0.38), earMat);
            case FLOPPY -> b.sym(limb(earBase, earBase.add(new Vec3(q.headR * 0.7, -q.headR * 0.8, 0.2)), q.headR * 0.32, q.headR * 0.25), earMat);
            case LONG -> b.sym(limb(earBase, earBase.add(new Vec3(0.4, q.headR * 2.4, -0.6)), q.headR * 0.3, q.headR * 0.25), earMat);
            case HORNS_CURVED -> {
                b.sym(limb(earBase, earBase.add(new Vec3(q.headR * 0.8, 0.4, -0.6)), q.headR * 0.28, q.headR * 0.22), earMat);
                b.sym(frustum(earBase.add(new Vec3(q.headR * 0.8, 0.4, -0.6)), earBase.add(new Vec3(q.headR * 0.9, q.headR * 0.9, 0.4)),
                        q.headR * 0.22, 0.05), earMat);
            }
            case ANTLERS -> {
                Vec3 a = earBase.add(new Vec3(q.headR * 0.9, q.headR * 1.6, -0.6));
                b.sym(limb(earBase, a, 0.35, 0.3), earMat);
                b.sym(limb(a, a.add(new Vec3(q.headR * 0.8, q.headR * 0.9, -0.4)), 0.3, 0.25), earMat);
                b.sym(limb(earBase.lerp(a, 0.5), earBase.lerp(a, 0.5).add(new Vec3(-0.2, q.headR * 0.9, q.headR * 0.4)), 0.3, 0.25), earMat);
                b.sym(limb(a, a.add(new Vec3(-0.3, q.headR * 1.0, 0.3)), 0.3, 0.25), earMat);
            }
            case BIG_FLAP -> b.sym(ellipsoid(q.headR * 1.1, headC.y(), headC.z() - q.headR * 0.4, q.headR * 0.35, q.headR * 1.2,
                    q.headR * 1.1), earMat);
            default -> {
            }
        }
        // Queue
        Vec3 tailBase = new Vec3(0, by + q.bodyH * 0.3, -q.bodyLen * 0.92);
        String tailMat = q.tailMat != null ? q.tailMat : bodyBase;
        switch (q.tail) {
            case THIN -> b.add(limb(tailBase, tailBase.add(new Vec3(0, -q.tailLen * 0.7, -q.tailLen * 0.6)), 0.45, 0.35), tailMat);
            case BUSHY -> b.add(limb(tailBase, tailBase.add(new Vec3(0, -q.tailLen * 0.3, -q.tailLen)), 0.9, q.tailLen * 0.28), tailMat);
            case TUFT -> {
                Vec3 end = tailBase.add(new Vec3(0, -q.tailLen * 0.8, -q.tailLen * 0.4));
                b.add(limb(tailBase, end, 0.4, 0.35), bodyBase);
                b.add(sphere(end.x(), end.y(), end.z(), 0.9), tailMat);
            }
            case THICK -> b.add(limb(tailBase.add(new Vec3(0, -q.bodyH * 0.4, 0)), tailBase.add(new Vec3(0, -q.bodyH * 0.9, -q.tailLen)),
                    q.bodyH * 0.55, 0.3), tailMat);
            case CURL -> {
                Vec3 a = tailBase.add(new Vec3(0, 0.8, -1.0));
                b.add(limb(tailBase, a, 0.45, 0.4), tailMat);
                b.add(limb(a, a.add(new Vec3(0.8, 0.4, 0.4)), 0.4, 0.35), tailMat);
            }
            default -> {
            }
        }
        Frame f = new Frame(new Vec3(0, by, 0), neckBase, headC, snoutTip, tailBase, q.headR);
        for (BiConsumer<Sculpt.Builder, Frame> e : q.extras) {
            e.accept(b, f);
        }
        return b.build();
    }
}
