package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Shape;
import com.ultimateautobuilder.core.geom.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;

/**
 * Gabarit de personnage humanoïde de style anime (unités de conception : hauteur 32,
 * tête volumineuse pour rester lisible en voxels). Les personnages se distinguent par
 * leurs couleurs, leur coiffure, leur tenue et leurs accessoires.
 */
public final class Humanoid {
    public static final double HEIGHT = 32;
    public static final Vec3 HEAD = new Vec3(0, 26.6, 0);
    public static final double HEAD_RX = 4.0, HEAD_RY = 4.3, HEAD_RZ = 3.9;

    public static final String SKIN = "minecraft:white_terracotta";
    public static final String SKIN_TAN = "minecraft:smooth_sandstone";
    public static final String WHITE = "minecraft:white_concrete";
    public static final String BLACK = "minecraft:black_concrete";

    /** Pose d'un bras (définie pour le côté +X, symétrisée pour l'autre côté). */
    public enum ArmPose { DOWN, UP, OUT, FORWARD, HIP, HOLD, THINK, PRAY }

    @FunctionalInterface
    public interface Hair {
        void apply(Sculpt.Builder b, Spec s);
    }

    /** Spécification modifiable d'un personnage (style « builder »). */
    public static final class Spec {
        public String skin = SKIN;
        public String hair = "minecraft:black_concrete";
        public String eyes = "minecraft:black_concrete";
        public String top = "minecraft:white_wool";
        public String sleeve;      // haut des bras (null = couleur du haut)
        public String forearm;     // avant-bras (null = peau)
        public String hands;       // mains (null = peau)
        public String pants = "minecraft:blue_wool";
        public String shoes = "minecraft:brown_concrete";
        public String belt;        // null = pas de ceinture
        public String mouth = "minecraft:brown_terracotta";
        public double bulk = 1.0;  // carrure
        public double legScale = 1.0;
        public Hair hairStyle = HairStyles.SHORT;
        public boolean eyebrows = true;
        public ArmPose leftArm = ArmPose.DOWN;   // bras côté +X (gauche du personnage)
        public ArmPose rightArm = ArmPose.DOWN;  // bras côté -X
        public boolean seated;
        public final List<Consumer<Sculpt.Builder>> extras = new ArrayList<>();

        public Spec skin(String v) { skin = v; return this; }
        public Spec hair(String v) { hair = v; return this; }
        public Spec eyes(String v) { eyes = v; return this; }
        public Spec top(String v) { top = v; return this; }
        public Spec sleeve(String v) { sleeve = v; return this; }
        public Spec forearm(String v) { forearm = v; return this; }
        public Spec hands(String v) { hands = v; return this; }
        public Spec pants(String v) { pants = v; return this; }
        public Spec shoes(String v) { shoes = v; return this; }
        public Spec belt(String v) { belt = v; return this; }
        public Spec mouth(String v) { mouth = v; return this; }
        public Spec bulk(double v) { bulk = v; return this; }
        public Spec legScale(double v) { legScale = v; return this; }
        public Spec style(Hair v) { hairStyle = v; return this; }
        public Spec noEyebrows() { eyebrows = false; return this; }
        public Spec arms(ArmPose left, ArmPose right) { leftArm = left; rightArm = right; return this; }
        public Spec seated() { seated = true; return this; }
        public Spec extra(Consumer<Sculpt.Builder> e) { extras.add(e); return this; }
    }

    private Humanoid() {
    }

    public static Spec spec() {
        return new Spec();
    }

    // Points d'articulation (fonction de la carrure).
    public static Vec3 shoulder(double bulk) {
        return new Vec3(4.9 * bulk, 20.8, 0);
    }

    public static Vec3 elbow(double bulk) {
        return new Vec3(5.9 * bulk, 16.4, 0.2);
    }

    public static Vec3 hand(double bulk) {
        return new Vec3(6.3 * bulk, 12.2, 0.8);
    }

    public static Vec3 elbow(ArmPose pose, double k, boolean leftSide) {
        Vec3 v = switch (pose) {
            case DOWN -> new Vec3(5.9 * k, 16.4, 0.2);
            case UP -> new Vec3(5.6 * k, 24.8, 0.2);
            case OUT -> new Vec3(8.9 * k, 20.7, 0);
            case FORWARD -> new Vec3(5.4 * k, 19.6, 3.8);
            case HIP -> new Vec3(7.6 * k, 17.6, -0.6);
            case HOLD -> new Vec3(5.9 * k, 16.8, 0.8);
            case THINK -> new Vec3(3.4 * k, 13.8, 5.4);
            case PRAY -> new Vec3(5.4 * k, 17.0, 2.6);
        };
        return leftSide ? v : v.mirrorX();
    }

    public static Vec3 hand(ArmPose pose, double k, boolean leftSide) {
        Vec3 v = switch (pose) {
            case DOWN -> new Vec3(6.3 * k, 12.2, 0.8);
            case UP -> new Vec3(5.4 * k, 29.2, 0.6);
            case OUT -> new Vec3(12.6 * k, 20.5, 0.2);
            case FORWARD -> new Vec3(5.0 * k, 19.4, 7.8);
            case HIP -> new Vec3(5.0 * k, 15.2, 1.2);
            case HOLD -> new Vec3(5.6 * k, 15.6, 4.4);
            case THINK -> new Vec3(0.9, 22.4, 4.2);
            case PRAY -> new Vec3(0.9, 19.6, 4.6);
        };
        return leftSide ? v : v.mirrorX();
    }

    public static Sculpt build(Spec s) {
        Sculpt.Builder b = Sculpt.builder(HEIGHT);
        double k = s.bulk;
        double hip = 12.6 * s.legScale + (1 - s.legScale) * 6;
        // Jambes et chaussures
        if (s.seated) {
            b.sym(limb(2.1 * k, hip, 0.2, 2.2 * k, hip + 0.2, 6.2, 1.85 * k, 1.75 * k), s.pants);
            b.sym(limb(2.2 * k, hip + 0.2, 6.2, 2.2 * k, 1.8, 6.8, 1.7 * k, 1.5 * k), s.pants);
            b.sym(ellipsoid(2.2 * k, 1.1, 7.6, 1.75 * k, 1.25, 2.6), s.shoes);
        } else {
            b.sym(limb(2.1 * k, 1.6, 0.1, 2.0 * k, hip, 0, 1.6 * k, 1.85 * k), s.pants);
            b.sym(ellipsoid(2.15 * k, 1.1, 0.7, 1.75 * k, 1.25, 2.6), s.shoes);
        }
        // Bassin, ceinture, torse
        b.add(boxC(0, 13.2, 0, 3.7 * k, 1.9, 2.3 * k), s.pants);
        if (s.belt != null) {
            b.add(boxC(0, 14.9, 0, 3.95 * k, 0.55, 2.55 * k), s.belt);
        }
        b.add(boxC(0, 18.0, 0, 3.6 * k, 3.4, 2.2 * k), s.top);
        b.add(ellipsoid(0, 19.6, 0, 4.7 * k, 2.6, 2.55 * k), s.top);
        // Bras
        Vec3 sh = shoulder(k);
        String sleeve = s.sleeve != null ? s.sleeve : s.top;
        String forearm = s.forearm != null ? s.forearm : s.skin;
        String hands = s.hands != null ? s.hands : s.skin;
        for (int side = 0; side < 2; side++) {
            boolean left = side == 0;
            ArmPose pose = left ? s.leftArm : s.rightArm;
            Vec3 shp = left ? sh : sh.mirrorX();
            Vec3 el = elbow(pose, k, left), ha = hand(pose, k, left);
            b.add(limb(shp, el, 1.45 * k, 1.3 * k), sleeve);
            b.add(limb(el, ha, 1.3 * k, 1.15 * k), forearm);
            Vec3 dir = ha.sub(el).normalize();
            Vec3 palm = ha.add(dir.mul(0.6));
            b.add(sphere(palm.x(), palm.y(), palm.z(), 1.35 * k), hands);
        }
        // Cou, tête, oreilles
        b.add(limb(0, 21.5, 0, 0, 23.6, 0, 1.3 * k, 1.3 * k), s.skin);
        b.add(ellipsoid(HEAD.x(), HEAD.y(), HEAD.z(), HEAD_RX, HEAD_RY, HEAD_RZ), s.skin);
        b.sym(ellipsoid(3.95, 26.4, 0, 0.6, 1.0, 0.7), s.skin);
        // Visage
        face(b, s.eyes, s.mouth, s.eyebrows ? s.hair : null);
        // Coiffure puis accessoires (peuvent recouvrir le reste)
        if (s.hairStyle != null) {
            s.hairStyle.apply(b, s);
        }
        for (Consumer<Sculpt.Builder> e : s.extras) {
            e.accept(b);
        }
        return b.build();
    }

    /** Yeux (blanc + iris), bouche et sourcils peints sur la face avant de la tête. */
    public static void face(Sculpt.Builder b, String iris, String mouth, String brows) {
        b.paintSym(ellipsoid(1.55, 26.9, 3.9, 0.95, 1.15, 0.9), WHITE);
        b.paintSym(ellipsoid(1.35, 26.75, 4.0, 0.55, 0.85, 0.9), iris);
        if (brows != null) {
            b.paintSym(boxC(1.6, 28.4, 3.6, 0.95, 0.28, 0.8), brows);
        }
        if (mouth != null) {
            b.paint(boxC(0, 24.6, 3.6, 0.95, 0.3, 0.9), mouth);
        }
    }

    /** Pointe conique partant de la surface de la tête dans la direction (dx, dy, dz). */
    public static Shape spike(double dx, double dy, double dz, double length, double radius) {
        Vec3 dir = new Vec3(dx, dy, dz).normalize();
        Vec3 base = HEAD.add(new Vec3(dir.x() * HEAD_RX * 0.55, dir.y() * HEAD_RY * 0.55, dir.z() * HEAD_RZ * 0.55));
        Vec3 tip = base.add(dir.mul(length + 2.2));
        return frustum(base, tip, radius, 0.05);
    }

    /** Calotte de cheveux : couvre le haut et l'arrière du crâne sans masquer le visage. */
    public static Shape cap(double thickness, double frontZ) {
        Shape shell = ellipsoid(HEAD.x(), HEAD.y() + 0.5, HEAD.z() - 0.2, HEAD_RX + thickness, HEAD_RY + thickness * 0.8,
                HEAD_RZ + thickness);
        Shape keepTopBack = com.ultimateautobuilder.core.geom.Shapes.union(
                boxC(0, HEAD.y() + 6, 0, 8, 3.6, 8),
                boxC(0, HEAD.y() + 2, frontZ - 8, 8, 9, 8));
        return shell.and(keepTopBack);
    }

    public static Mat m(String state) {
        return Mat.of(state);
    }
}
