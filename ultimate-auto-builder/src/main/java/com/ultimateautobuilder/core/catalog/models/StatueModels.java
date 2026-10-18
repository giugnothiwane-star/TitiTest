package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.HairStyles;
import com.ultimateautobuilder.core.generator.Humanoid;
import com.ultimateautobuilder.core.generator.Humanoid.ArmPose;
import com.ultimateautobuilder.core.generator.Quadruped;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;

import static com.ultimateautobuilder.core.generator.Creature.horn;
import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.geom.Shapes.box;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.torusY;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** 25 statues monumentales sur socle. */
public final class StatueModels {
    private static final String STONE = "minecraft:stone";
    private static final String QUARTZ = "minecraft:smooth_quartz";
    private static final String GOLD = "minecraft:gold_block";
    private static final String COPPER = "minecraft:oxidized_copper";
    private static final String BRONZE = "minecraft:cut_copper";
    private static final String BRICKS = "minecraft:stone_bricks";
    private static final String DARK = "minecraft:polished_deepslate";

    private StatueModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.STATUES);
        SizeSpec S = Reg.STATUE;
        r.sculpt("statue_liberte", "Statue de la Liberté", 5, SizeSpec.height(16, 40, 80, 150), StatueModels::liberty, "statue", "new york", "torche", "monument");
        r.sculpt("moai", "Moaï", 3, S, StatueModels::moai, "statue", "ile de paques", "tete", "monument");
        r.sculpt("penseur", "Le Penseur", 4, S, StatueModels::thinker, "statue", "rodin", "assis", "bronze");
        r.sculpt("chevalier", "Chevalier en armure", 4, S, StatueModels::knight, "statue", "medieval", "epee", "bouclier");
        r.sculpt("ange", "Ange gardien", 4, S, StatueModels::angel, "statue", "ailes", "aureole");
        r.sculpt("bouddha", "Bouddha assis", 4, S, StatueModels::buddha, "statue", "meditation", "or", "asie");
        r.sculpt("sphinx", "Sphinx", 4, SizeSpec.length(16, 36, 70, 130), StatueModels::sphinx, "statue", "egypte", "lion", "monument");
        r.sculpt("pharaon", "Buste de pharaon", 4, S, StatueModels::pharaoh, "statue", "egypte", "or", "masque");
        r.sculpt("hoplite", "Guerrier grec", 4, S, StatueModels::hoplite, "statue", "grece", "spartiate", "lance");
        r.sculpt("atlas", "Atlas portant le monde", 4, S, StatueModels::atlas, "statue", "mythologie", "globe", "titan");
        r.sculpt("colosse_bras_ouverts", "Colosse aux bras ouverts", 4, SizeSpec.height(16, 40, 80, 150), StatueModels::openArms, "statue", "monument", "rio");
        r.sculpt("buste_geant", "Buste antique", 3, S, StatueModels::bust, "statue", "marbre", "antique", "tete");
        r.sculpt("lion_pierre", "Lion de pierre", 3, S, StatueModels::stoneLion, "statue", "lion", "gardien");
        r.sculpt("statue_equestre", "Statue équestre", 5, S, StatueModels::equestrian, "statue", "cheval", "cavalier", "bronze");
        r.sculpt("sirene", "Sirène sur son rocher", 4, S, StatueModels::mermaid, "statue", "mer", "copenhague");
        r.sculpt("samourai", "Samouraï", 4, S, StatueModels::samurai, "statue", "japon", "katana", "armure");
        r.sculpt("viking", "Viking", 4, S, StatueModels::viking, "statue", "nordique", "hache", "guerrier");
        r.sculpt("magicien", "Grand magicien", 4, S, StatueModels::wizard, "statue", "sorcier", "baton", "fantasy");
        r.sculpt("roi", "Statue du roi", 4, S, StatueModels::king, "statue", "couronne", "sceptre", "royal");
        r.sculpt("reine", "Statue de la reine", 4, S, StatueModels::queen, "statue", "couronne", "royal");
        r.sculpt("astronaute", "Astronaute", 4, S, StatueModels::astronaut, "statue", "espace", "lune", "drapeau");
        r.sculpt("robot_statue", "Robot géant", 3, S, StatueModels::robot, "statue", "robot", "metal");
        r.sculpt("main_geante", "Main géante", 3, S, StatueModels::hand, "statue", "main", "sculpture");
        r.sculpt("visage_montagne", "Visage taillé dans la roche", 4, S, StatueModels::rockFace, "statue", "falaise", "rushmore", "visage");
        r.sculpt("colosse_bronze", "Colosse de bronze", 4, SizeSpec.height(16, 40, 80, 150), StatueModels::colossus, "statue", "rhodes", "bronze", "antique");
    }

    // ------------------------------------------------------------------ outils

    static Humanoid.Spec mono(String mat) {
        return Humanoid.spec().skin(mat).hair(mat).top(mat).pants(mat).shoes(mat).eyes(DARK).mouth(DARK).style(HairStyles.SHORT);
    }

    /** Socle à corniche puis la statue posée dessus. */
    static Sculpt onPedestal(Sculpt statue, double halfWidth, double height, String mat) {
        Sculpt.Builder b = Sculpt.builder(statue.designHeight() + height);
        b.add(boxC(0, height * 0.5, 0, halfWidth, height * 0.5, halfWidth), mat);
        b.add(boxC(0, height - 0.4, 0, halfWidth + 0.6, 0.4, halfWidth + 0.6), mat);
        b.add(boxC(0, 0.4, 0, halfWidth + 0.6, 0.4, halfWidth + 0.6), mat);
        b.addAll(statue, 0, height, 0, 0, 1);
        return b.build();
    }

    static void robe(Sculpt.Builder b, String mat, double bottom, double flare) {
        b.add(frustum(new Vec3(0, 18.0, 0), new Vec3(0, bottom, 0), 3.9, flare), mat);
    }

    static void crown(Sculpt.Builder b, String mat, double y, double r) {
        b.add(cylinderY(0, y, 0, 1.4, r), mat);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            b.add(horn(Math.cos(a) * r * 0.9, y + 1.2, Math.sin(a) * r * 0.9, Math.cos(a) * r, y + 3.0, Math.sin(a) * r, 0.5), mat);
        }
    }

    // ------------------------------------------------------------------ statues

    static Sculpt liberty() {
        Humanoid.Spec s = mono(COPPER).arms(ArmPose.HOLD, ArmPose.UP).style(HairStyles.BOB);
        s.extra(b -> {
            robe(b, COPPER, 0.4, 6.2);
            b.add(tri(new Vec3(-4.6, 21.0, 2.4), new Vec3(4.6, 13.0, 3.0), new Vec3(-2.0, 12.0, 3.2), 0.6), COPPER);
            for (int i = 0; i < 7; i++) {
                double a = Math.toRadians(-60 + i * 20);
                b.add(horn(Math.sin(a) * 3.6, 29.6, Math.cos(a) * 3.0, Math.sin(a) * 7.6, 31.6, Math.cos(a) * 6.2, 0.6), COPPER);
            }
            b.add(cylinderY(0, 29.0, 0, 1.2, 4.4), COPPER);
            Vec3 h = Humanoid.hand(ArmPose.UP, 1, false);
            b.add(frustum(h, h.add(new Vec3(0, 4.0, 0)), 0.6, 1.2), COPPER);
            b.add(ellipsoid(h.x(), h.y() + 5.6, h.z(), 1.3, 2.0, 1.3), GOLD);
            Vec3 l = Humanoid.hand(ArmPose.HOLD, 1, true);
            b.add(boxC(l.x(), l.y() + 1.0, l.z() - 0.8, 0.6, 3.6, 2.2).rotated(0, 0, 10, l), COPPER);
        });
        return onPedestal(Humanoid.build(s), 6.0, 10, BRICKS);
    }

    static Sculpt moai() {
        Sculpt.Builder b = Sculpt.builder(30);
        String t = "minecraft:tuff";
        b.add(box(-6, 0, -4, 6, 10, 4), "minecraft:tuff_bricks");
        b.add(ellipsoid(0, 18, 0, 5.4, 12, 4.4), t);
        b.add(box(-5.6, 22.6, 1.0, 5.6, 25.0, 4.8), t);
        b.add(frustum(new Vec3(0, 22.5, 3.0), new Vec3(0, 14.6, 6.4), 1.2, 2.2), t);
        b.carveSym(boxC(2.6, 21.4, 4.4, 1.6, 1.0, 1.0));
        b.add(box(-3.2, 9.6, 1.6, 3.2, 12.6, 5.4), t);
        b.paint(box(-2.6, 11.4, 4.6, 2.6, 11.9, 5.6), "minecraft:polished_tuff");
        b.sym(ellipsoid(5.6, 18, 0, 0.8, 6, 1.6), t);
        b.add(cylinderY(0, 29.0, -0.6, 2.4, 4.0), "minecraft:red_terracotta");
        return b.build();
    }

    static Sculpt thinker() {
        Humanoid.Spec s = mono(BRONZE).arms(ArmPose.DOWN, ArmPose.THINK).seated().style(HairStyles.MESSY);
        s.extra(b -> b.add(box(-5, 0, -4, 5, 11.4, 4), "minecraft:polished_andesite"));
        return onPedestal(Humanoid.build(s), 7.0, 4, BRICKS);
    }

    static Sculpt knight() {
        String iron = "minecraft:iron_block";
        Humanoid.Spec s = mono(iron).arms(ArmPose.HOLD, ArmPose.HOLD).style(HairStyles.BALD).bulk(1.12);
        s.extra(b -> {
            b.add(Humanoid.cap(0.6, 3.0), iron);
            b.paint(boxC(0, 26.8, 4.0, 3.0, 0.3, 1.0), DARK);
            b.add(horn(0, 31.4, -0.6, 0, 34.0, -3.6, 0.9), "minecraft:red_wool");
            b.sym(ellipsoid(5.4, 21.4, 0, 2.2, 1.3, 2.6), iron);
            Vec3 r = Humanoid.hand(ArmPose.HOLD, 1.12, false);
            b.add(limb(r.add(new Vec3(0, -1.6, 0)), r.add(new Vec3(0, 1.0, 0)), 0.45, 0.45), "minecraft:dark_oak_planks");
            b.add(boxC(r.x(), r.y() + 1.4, r.z(), 2.0, 0.4, 0.5), GOLD);
            b.add(boxC(r.x(), r.y() + 9.0, r.z(), 0.7, 7.2, 0.3), "minecraft:iron_block");
            Vec3 l = Humanoid.hand(ArmPose.HOLD, 1.12, true);
            b.add(ellipsoid(l.x() + 0.6, l.y() + 1.0, l.z() + 1.2, 0.6, 4.6, 3.4), "minecraft:blue_concrete");
            b.paint(boxC(l.x() + 1.2, l.y() + 1.0, l.z() + 1.2, 0.6, 3.2, 0.6), GOLD);
            b.paint(boxC(l.x() + 1.2, l.y() + 1.6, l.z() + 1.2, 0.6, 0.6, 2.0), GOLD);
        });
        return onPedestal(Humanoid.build(s), 6.0, 4, BRICKS);
    }

    static Sculpt angel() {
        Humanoid.Spec s = mono(QUARTZ).arms(ArmPose.PRAY, ArmPose.PRAY).style(HairStyles.LONG);
        s.extra(b -> {
            robe(b, QUARTZ, 0.4, 5.2);
            Vec3 root = new Vec3(1.6, 21.0, -2.6);
            b.sym(tri(root, new Vec3(12.0, 30.0, -6.0), new Vec3(9.0, 14.0, -6.0), 0.6), "minecraft:white_wool");
            b.sym(tri(root, new Vec3(9.0, 14.0, -6.0), new Vec3(3.0, 8.0, -4.6), 0.6), "minecraft:white_wool");
            b.add(torusY(0, 33.0, 0, 2.8, 0.45), GOLD);
        });
        return onPedestal(Humanoid.build(s), 5.0, 4, "minecraft:quartz_bricks");
    }

    static Sculpt buddha() {
        Sculpt.Builder b = Sculpt.builder(30);
        b.add(cylinderY(0, 0, 0, 3.0, 10.0), "minecraft:pink_terracotta");
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            b.add(ellipsoid(Math.cos(a) * 9.0, 3.0, Math.sin(a) * 9.0, 2.6, 1.2, 2.6), "minecraft:pink_concrete");
        }
        b.add(ellipsoid(0, 5.0, 1.0, 8.0, 2.4, 5.0), GOLD);
        b.add(ellipsoid(0, 12.0, 0, 6.0, 7.0, 4.6), GOLD);
        b.add(ellipsoid(0, 10.0, 2.4, 4.6, 4.6, 3.0), GOLD);
        b.sym(limb(5.2, 15.0, 0, 6.4, 9.0, 2.4, 1.6, 1.4), GOLD);
        b.sym(limb(6.4, 9.0, 2.4, 1.4, 7.4, 3.8, 1.4, 1.2), GOLD);
        b.add(sphere(0, 22.0, 0.4, 4.2), GOLD);
        b.sym(ellipsoid(4.0, 21.0, 0.4, 0.8, 2.6, 1.0), GOLD);
        b.add(sphere(0, 26.4, 0, 2.2), GOLD);
        b.paintSym(boxC(1.6, 22.4, 4.2, 1.0, 0.2, 0.8), "minecraft:orange_concrete");
        b.paint(boxC(0, 20.2, 4.1, 0.9, 0.2, 0.6), "minecraft:orange_concrete");
        b.paint(sphere(0, 23.8, 4.2, 0.4), "minecraft:orange_concrete");
        return b.build();
    }

    static Sculpt sphinx() {
        String s = "minecraft:smooth_sandstone";
        Quadruped.Spec q = Quadruped.spec().size(9.0, 4.0, 3.6, 1.4, 1.8).neck(0.6, 70).head(3.2, 0, 0)
                .body(s).ears(Quadruped.Ears.NONE, s).tail(Quadruped.Tail.THIN, s, 4)
                .extra((b, f) -> {
                    Vec3 h = f.headCenter();
                    b.sym(limb(2.6, 1.4, 6.0, 2.6, 1.0, 14.0, 1.6, 1.5), s);
                    b.add(box(h.x() - 4.2, h.y() - 4.0, h.z() - 2.6, h.x() + 4.2, h.y() + 2.6, h.z() + 1.2),
                            Mat.stripesY(1.0, 0.5, Mat.of("minecraft:sandstone"), Mat.of("minecraft:cut_sandstone")));
                    b.add(sphere(h.x(), h.y(), h.z() + 1.0, 3.0), s);
                    b.paintSym(boxC(h.x() + 1.2, h.y() + 0.6, h.z() + 3.8, 0.7, 0.3, 0.6), "minecraft:brown_terracotta");
                    b.add(limb(h.add(new Vec3(0, -3.0, 2.6)), h.add(new Vec3(0, -6.0, 2.6)), 0.7, 0.6), "minecraft:sandstone");
                });
        return Quadruped.build(q);
    }

    static Sculpt pharaoh() {
        Sculpt.Builder b = Sculpt.builder(30);
        String gold = GOLD;
        String lapis = "minecraft:lapis_block";
        b.add(box(-8, 0, -5, 8, 6, 5), "minecraft:sandstone");
        b.add(ellipsoid(0, 9.6, 0, 9.0, 4.4, 4.6), gold);
        b.add(ellipsoid(0, 18.0, 0.6, 4.4, 5.6, 4.6), "minecraft:orange_terracotta");
        Mat stripes = Mat.stripesY(1.4, 0.7, Mat.of(gold), Mat.of(lapis));
        b.add(ellipsoid(0, 20.6, -0.6, 5.6, 6.0, 5.0).minus(box(-3.6, 10, 2.6, 3.6, 19.6, 8)), stripes);
        b.sym(box(4.0, 9.0, -1.0, 6.4, 18.0, 2.6), stripes);
        b.add(horn(0, 24.6, 4.2, 0, 25.6, 5.6, 0.6), gold);
        b.add(limb(0, 13.0, 3.6, 0, 9.0, 4.6, 0.8, 0.7), stripes);
        b.paintSym(ellipsoid(1.6, 19.0, 4.4, 1.2, 0.6, 0.8), "minecraft:white_concrete");
        b.paintSym(sphere(1.6, 19.0, 4.8, 0.4), "minecraft:black_concrete");
        b.paintSym(boxC(2.6, 19.0, 4.2, 1.0, 0.18, 0.6), "minecraft:black_concrete");
        return b.build();
    }

    static Sculpt hoplite() {
        String bronze = BRONZE;
        Humanoid.Spec s = mono(bronze).arms(ArmPose.HOLD, ArmPose.UP).style(HairStyles.BALD).bulk(1.1);
        s.extra(b -> {
            b.add(Humanoid.cap(0.7, 3.6), bronze);
            b.carve(boxC(0, 26.6, 4.6, 1.6, 1.4, 1.2));
            b.add(tri(new Vec3(0, 31.0, 4.0), new Vec3(0, 35.4, -1.0), new Vec3(0, 27.4, -6.0), 0.8), "minecraft:red_wool");
            Vec3 l = Humanoid.hand(ArmPose.HOLD, 1.1, true);
            b.add(cylinderY(0, 0, 0, 1.0, 5.6).rotated(0, 90, 0, Vec3.ZERO).translated(l.x() + 0.6, l.y() + 1.4, l.z() + 1.6), bronze);
            b.paint(cylinderY(0, 0, 0, 1.2, 1.4).rotated(0, 90, 0, Vec3.ZERO).translated(l.x() + 0.6, l.y() + 1.4, l.z() + 1.8), GOLD);
            Vec3 r = Humanoid.hand(ArmPose.UP, 1.1, false);
            b.add(limb(r.add(new Vec3(0, -26, 0)), r.add(new Vec3(0, 8, 0)), 0.4, 0.4), "minecraft:dark_oak_log");
            b.add(horn(r.x(), r.y() + 8, r.z(), r.x(), r.y() + 11.6, r.z(), 0.9), "minecraft:iron_block");
        });
        return onPedestal(Humanoid.build(s), 6.0, 4, "minecraft:polished_diorite");
    }

    static Sculpt atlas() {
        Humanoid.Spec s = mono(STONE).arms(ArmPose.UP, ArmPose.UP).style(HairStyles.MESSY).bulk(1.25);
        s.extra(b -> {
            Mat globe = Mat.spots(51, 2.0, 0.55, Mat.of("minecraft:lapis_block"), Mat.of("minecraft:emerald_block"));
            b.add(sphere(0, 38.0, 0, 8.0), globe);
            b.add(torusY(0, 38.0, 0, 8.4, 0.4), GOLD);
        });
        return onPedestal(Humanoid.build(s), 6.0, 3, BRICKS);
    }

    static Sculpt openArms() {
        String w = "minecraft:calcite";
        Humanoid.Spec s = mono(w).arms(ArmPose.OUT, ArmPose.OUT).style(HairStyles.SHOULDER);
        s.extra(b -> {
            robe(b, w, 0.4, 5.0);
            b.sym(limb(Humanoid.shoulder(1), Humanoid.hand(ArmPose.OUT, 1, true).add(new Vec3(-1.0, -3.0, -0.6)), 1.2, 0.5), w);
        });
        return onPedestal(Humanoid.build(s), 5.0, 12, "minecraft:polished_diorite");
    }

    static Sculpt bust() {
        Sculpt.Builder b = Sculpt.builder(30);
        String m = QUARTZ;
        b.add(cylinderY(0, 0, 0, 3.0, 6.0), "minecraft:quartz_pillar");
        b.add(cylinderY(0, 3.0, 0, 5.0, 2.6), "minecraft:quartz_pillar");
        b.add(ellipsoid(0, 11.0, 0, 9.0, 4.0, 4.2), m);
        b.add(limb(0, 12.0, 0, 0, 16.0, 0.4, 2.6, 2.4), m);
        b.add(ellipsoid(0, 20.0, 0.6, 4.6, 5.6, 4.8), m);
        for (int i = 0; i < 14; i++) {
            double a = Math.toRadians(i * 26);
            b.add(sphere(Math.cos(a) * 4.0, 23.6 + Math.sin(i * 1.7) * 0.6, Math.sin(a) * 3.6 - 0.8, 1.6), m);
        }
        b.add(ellipsoid(0, 16.6, 3.2, 3.6, 2.4, 2.0), Mat.mix(4, 0.6, 0.4, Mat.of(m), Mat.of("minecraft:quartz_bricks")));
        b.add(frustum(new Vec3(0, 21.0, 4.6), new Vec3(0, 18.4, 6.0), 0.8, 1.0), m);
        b.paintSym(boxC(1.8, 21.0, 4.8, 0.9, 0.3, 0.6), "minecraft:light_gray_concrete");
        return b.build();
    }

    static Sculpt stoneLion() {
        String s = "minecraft:stone";
        Quadruped.Spec q = Quadruped.spec().size(5.0, 2.8, 3.0, 5.0, 1.2).neck(1.4, 60).head(2.6, 1.4, 1.4)
                .body(s).ears(Quadruped.Ears.ROUND, s).tail(Quadruped.Tail.TUFT, s, 5)
                .extra((b, f) -> {
                    Vec3 h = f.headCenter();
                    b.add(ellipsoid(h.x(), h.y() - 0.6, h.z() - 1.4, 4.0, 4.6, 3.0), "minecraft:cobblestone");
                    b.add(sphere(h.x(), h.y(), h.z() + 0.6, 2.6), s);
                    b.add(box(-4.6, -3, -7, 4.6, 0, 8), BRICKS);
                });
        return Quadruped.build(q);
    }

    static Sculpt equestrian() {
        Sculpt horse = Quadruped.build(Quadruped.spec().size(6.0, 2.8, 3.0, 8.0, 0.9).neck(5.0, 55).head(1.9, 3.2, 1.3)
                .body(BRONZE).ears(Quadruped.Ears.POINTY, BRONZE).tail(Quadruped.Tail.BUSHY, BRONZE, 6));
        Humanoid.Spec rider = mono(BRONZE).arms(ArmPose.UP, ArmPose.HOLD).seated().style(HairStyles.SHORT);
        rider.extra(b -> {
            Vec3 h = Humanoid.hand(ArmPose.UP, 1, true);
            b.add(limb(h.add(new Vec3(0, -1, 0)), h.add(new Vec3(0, 9, 3)), 0.35, 0.3), "minecraft:iron_block");
        });
        Sculpt.Builder b = Sculpt.builder(36);
        b.add(box(-6, 0, -12, 6, 4, 12), BRICKS);
        b.addAll(horse, 0, 4, 0, 0, 1);
        b.addAll(Humanoid.build(rider), 0, 4 + 8.4, -1.6, 0, 0.62);
        return b.build();
    }

    static Sculpt mermaid() {
        String skin = BRONZE;
        Humanoid.Spec s = mono(skin).arms(ArmPose.HIP, ArmPose.DOWN).style(HairStyles.LONG);
        s.extra(b -> {
            b.carve(box(-6, 0, -4, 6, 12.0, 4));
            b.add(tube(2.6, 0.6, new Vec3(0, 12.6, 0), new Vec3(1.0, 9.0, 3.0), new Vec3(4.0, 7.6, 6.0), new Vec3(8.0, 8.4, 6.0)),
                    Mat.checker(0.9, Mat.of("minecraft:weathered_copper"), Mat.of(skin)));
            b.add(tri(new Vec3(8.0, 8.4, 6.0), new Vec3(11.0, 11.0, 5.0), new Vec3(11.0, 6.6, 7.6), 0.5), "minecraft:weathered_copper");
        });
        Sculpt.Builder b = Sculpt.builder(36);
        b.add(ellipsoid(0, 4.0, 0, 9.0, 5.0, 8.0), Mat.mix(2, 1.2, 0.4, Mat.of("minecraft:cobblestone"), Mat.of("minecraft:mossy_cobblestone")));
        b.addAll(Humanoid.build(s), 0, 2.0, 0, 0, 1);
        return b.build();
    }

    static Sculpt samurai() {
        String red = "minecraft:red_concrete";
        String black = "minecraft:black_concrete";
        Humanoid.Spec s = Humanoid.spec().skin("minecraft:white_terracotta").top(red).sleeve(black).forearm(red).pants(black)
                .shoes(black).belt(GOLD).style(HairStyles.BALD).arms(ArmPose.FORWARD, ArmPose.FORWARD).bulk(1.1);
        s.extra(b -> {
            b.paint(boxC(0, 18.4, 0, 6, 4, 4), Mat.stripesY(1.0, 0.3, Mat.of(red), Mat.of(black)));
            b.add(Humanoid.cap(0.9, 2.0), black);
            b.add(ellipsoid(0, 28.6, -0.4, 6.0, 0.8, 5.6), black);
            b.sym(tri(new Vec3(0.6, 30.0, 3.6), new Vec3(4.2, 34.6, 4.0), new Vec3(1.6, 30.0, 3.6), 0.4), GOLD);
            b.sym(ellipsoid(6.0, 20.6, 0, 2.6, 2.6, 3.0).and(box(0, 18, -4, 9, 23, 4)), red);
            Vec3 h = Humanoid.hand(ArmPose.FORWARD, 1.1, true);
            b.add(limb(h.add(new Vec3(-5.0, 0, 0)), h.add(new Vec3(-5.0, 0, 1.6)), 0.4, 0.4), black);
            b.add(limb(h.add(new Vec3(-5.0, 0, 1.6)), h.add(new Vec3(-5.0, 11.0, 6.0)), 0.4, 0.2), "minecraft:iron_block");
        });
        return onPedestal(Humanoid.build(s), 6.0, 3, "minecraft:dark_oak_planks");
    }

    static Sculpt viking() {
        String fur = "minecraft:brown_wool";
        Humanoid.Spec s = Humanoid.spec().skin("minecraft:white_terracotta").hair("minecraft:orange_terracotta").top(fur)
                .sleeve("minecraft:brown_terracotta").pants("minecraft:gray_wool").shoes(fur).belt("minecraft:black_concrete")
                .style(HairStyles.SHOULDER).arms(ArmPose.HOLD, ArmPose.UP).bulk(1.25);
        s.extra(b -> {
            b.add(Humanoid.cap(0.5, 1.0).and(box(-8, 28, -8, 8, 40, 8)), "minecraft:iron_block");
            b.sym(tube(0.7, 0.2, new Vec3(3.8, 29.0, 0), new Vec3(6.2, 30.6, 0.4), new Vec3(6.6, 33.6, 1.2)), "minecraft:white_terracotta");
            b.add(ellipsoid(0, 22.8, 3.0, 2.8, 3.4, 1.6), "minecraft:orange_terracotta");
            Vec3 l = Humanoid.hand(ArmPose.HOLD, 1.25, true);
            b.add(cylinderY(0, 0, 0, 0.8, 5.4).rotated(0, 90, 0, Vec3.ZERO).translated(l.x() + 0.8, l.y() + 1.0, l.z() + 1.4),
                    Mat.stripesX(2.0, 1.0, Mat.of("minecraft:red_concrete"), Mat.of("minecraft:white_concrete")));
            Vec3 r = Humanoid.hand(ArmPose.UP, 1.25, false);
            b.add(limb(r.add(new Vec3(0, -4, 0)), r.add(new Vec3(0, 4, 0)), 0.4, 0.4), "minecraft:oak_log");
            b.add(tri(r.add(new Vec3(0, 4.4, 0)), r.add(new Vec3(-3.6, 6.0, 0)), r.add(new Vec3(-3.6, 1.6, 0)), 0.4), "minecraft:iron_block");
        });
        return onPedestal(Humanoid.build(s), 6.0, 3, "minecraft:cobblestone");
    }

    static Sculpt wizard() {
        String robeMat = "minecraft:purple_concrete";
        Humanoid.Spec s = Humanoid.spec().skin("minecraft:white_terracotta").hair("minecraft:white_wool").top(robeMat).sleeve(robeMat)
                .pants(robeMat).shoes(robeMat).style(HairStyles.LONG).arms(ArmPose.HOLD, ArmPose.DOWN);
        s.extra(b -> {
            robe(b, robeMat, 0.4, 5.6);
            b.paint(frustum(new Vec3(0, 18.0, 0), new Vec3(0, 0.4, 0), 4.2, 6.0), Mat.spots(8, 1.0, 0.82, Mat.of(robeMat), Mat.of(GOLD)));
            b.add(ellipsoid(0, 21.6, 3.0, 3.0, 4.4, 1.6), "minecraft:white_wool");
            b.add(cylinderY(0, 29.8, 0, 0.6, 7.0), robeMat);
            b.add(frustum(new Vec3(0, 30.2, 0), new Vec3(1.6, 40.0, -2.6), 4.0, 0.1), robeMat);
            Vec3 h = Humanoid.hand(ArmPose.HOLD, 1, true);
            b.add(limb(h.add(new Vec3(0, -15, 0)), h.add(new Vec3(0, 12, 0)), 0.45, 0.45), "minecraft:dark_oak_log");
            b.add(sphere(h.x(), h.y() + 13.4, h.z(), 1.6), "minecraft:sea_lantern");
        });
        return onPedestal(Humanoid.build(s), 6.0, 3, "minecraft:deepslate_bricks");
    }

    static Sculpt king() {
        String red = "minecraft:red_concrete";
        Humanoid.Spec s = Humanoid.spec().skin("minecraft:white_terracotta").hair("minecraft:brown_concrete").top(red).sleeve(red)
                .pants(red).shoes("minecraft:black_concrete").belt(GOLD).style(HairStyles.SHORT).arms(ArmPose.HOLD, ArmPose.DOWN).bulk(1.1);
        s.extra(b -> {
            robe(b, red, 0.4, 5.6);
            b.add(torusY(0, 21.8, 0, 4.2, 1.2), "minecraft:white_wool");
            b.add(tri(new Vec3(-5.4, 22, -2.4), new Vec3(5.4, 22, -2.4), new Vec3(0, 0.4, -6.4), 0.5), "minecraft:blue_concrete");
            b.add(ellipsoid(0, 24.0, 3.0, 2.6, 2.6, 1.4), "minecraft:brown_concrete");
            crown(b, GOLD, 29.6, 3.8);
            Vec3 h = Humanoid.hand(ArmPose.HOLD, 1.1, true);
            b.add(limb(h.add(new Vec3(0, -3, 0)), h.add(new Vec3(0, 8, 0)), 0.4, 0.4), GOLD);
            b.add(sphere(h.x(), h.y() + 8.8, h.z(), 1.1), "minecraft:emerald_block");
        });
        return onPedestal(Humanoid.build(s), 6.0, 4, "minecraft:polished_blackstone_bricks");
    }

    static Sculpt queen() {
        String gown = "minecraft:magenta_concrete";
        Humanoid.Spec s = Humanoid.spec().skin("minecraft:white_terracotta").hair("minecraft:yellow_terracotta").top(gown).sleeve(gown)
                .pants(gown).shoes(gown).style(HairStyles.LONG).arms(ArmPose.PRAY, ArmPose.PRAY).bulk(0.92);
        s.extra(b -> {
            robe(b, gown, 0.4, 7.0);
            b.paint(frustum(new Vec3(0, 14.0, 0), new Vec3(0, 0.4, 0), 5.6, 7.4), Mat.stripesY(3.0, 0.5, Mat.of(gown), Mat.of(GOLD)));
            crown(b, GOLD, 29.8, 3.2);
            b.add(torusY(0, 21.6, 0, 3.2, 0.4), "minecraft:white_concrete");
        });
        return onPedestal(Humanoid.build(s), 7.0, 4, "minecraft:quartz_bricks");
    }

    static Sculpt astronaut() {
        String w = "minecraft:white_concrete";
        Humanoid.Spec s = mono(w).style(HairStyles.BALD).arms(ArmPose.HOLD, ArmPose.DOWN).bulk(1.2);
        s.extra(b -> {
            b.add(sphere(0, 26.8, 0, 5.2), w);
            b.paint(ellipsoid(0, 27.0, 4.0, 3.6, 2.6, 2.0), "minecraft:orange_stained_glass");
            b.paint(ellipsoid(0, 27.0, 4.4, 3.2, 2.2, 1.2), GOLD);
            b.add(box(-4.0, 13.0, -6.6, 4.0, 23.0, -2.2), "minecraft:light_gray_concrete");
            b.paint(boxC(0, 19.0, 2.6, 2.0, 1.4, 0.6), "minecraft:blue_concrete");
            b.paint(sphere(-1.0, 19.4, 2.8, 0.4), "minecraft:red_concrete");
            Vec3 h = Humanoid.hand(ArmPose.HOLD, 1.2, true);
            b.add(limb(h.add(new Vec3(0, -15, 0)), h.add(new Vec3(0, 14, 0)), 0.35, 0.35), "minecraft:iron_block");
            b.add(box(h.x(), h.y() + 7.0, h.z() - 0.2, h.x() + 9.0, h.y() + 13.0, h.z() + 0.2),
                    Mat.stripesY(1.2, 0.6, Mat.of("minecraft:red_concrete"), Mat.of(w)));
            b.add(box(h.x(), h.y() + 10.0, h.z() - 0.25, h.x() + 3.6, h.y() + 13.0, h.z() + 0.25), "minecraft:blue_concrete");
        });
        Sculpt.Builder b = Sculpt.builder(36);
        b.add(ellipsoid(0, 0, 0, 10.0, 2.4, 9.0), "minecraft:light_gray_concrete");
        b.carve(ellipsoid(5.0, 2.4, -4.0, 2.4, 1.6, 2.4));
        b.addAll(Humanoid.build(s), 0, 2.0, 0, 0, 1);
        return b.build();
    }

    static Sculpt robot() {
        Sculpt.Builder b = Sculpt.builder(30);
        String m = "minecraft:iron_block";
        String d = "minecraft:gray_concrete";
        b.sym(box(1.4, 0, -2.4, 4.8, 12, 2.4), m);
        b.sym(box(1.0, 0, -3.0, 5.2, 2.4, 4.0), d);
        b.add(box(-5.6, 11, -3.4, 5.6, 22, 3.4), m);
        b.paint(boxC(0, 17.0, 3.4, 3.0, 2.6, 0.4), "minecraft:light_blue_stained_glass");
        b.paint(boxC(0, 17.0, 3.6, 1.2, 1.0, 0.4), "minecraft:redstone_block");
        b.sym(box(5.6, 8.0, -1.6, 8.6, 21.0, 1.6), d);
        b.sym(box(5.2, 5.0, -2.2, 9.0, 8.4, 2.2), m);
        b.add(box(-1.0, 22, -1.0, 1.0, 23.4, 1.0), d);
        b.add(box(-4.0, 23.4, -3.4, 4.0, 29.6, 3.4), m);
        b.paintSym(boxC(1.8, 27.0, 3.5, 0.9, 0.7, 0.3), "minecraft:redstone_block");
        b.paint(box(-2.4, 24.6, 3.3, 2.4, 25.2, 3.6), d);
        b.add(limb(0, 29.6, 0, 0, 32.6, 0, 0.3, 0.3), d);
        b.add(sphere(0, 33.0, 0, 0.8), "minecraft:redstone_block");
        return b.build();
    }

    static Sculpt hand() {
        Sculpt.Builder b = Sculpt.builder(30);
        String m = "minecraft:smooth_quartz";
        b.add(cylinderY(0, 0, 0, 2.0, 7.0), BRICKS);
        b.add(limb(0, 2.0, 0, 0, 12.0, 0.4, 3.0, 3.4), m);
        b.add(ellipsoid(0, 15.0, 0.6, 4.6, 4.4, 2.2), m);
        double[] xs = {-3.4, -1.2, 1.1, 3.3};
        double[] lens = {6.0, 8.4, 9.0, 7.6};
        for (int i = 0; i < 4; i++) {
            b.add(limb(xs[i], 17.6, 0.6, xs[i] * 1.12, 17.6 + lens[i], 0.9, 1.05, 0.85), m);
        }
        b.add(tube(1.2, 0.9, new Vec3(4.4, 13.4, 1.2), new Vec3(7.0, 16.4, 2.4), new Vec3(8.6, 20.4, 2.6)), m);
        return b.build();
    }

    static Sculpt rockFace() {
        Sculpt.Builder b = Sculpt.builder(30);
        Mat rock = Mat.mix(13, 1.2, 0.35, Mat.of("minecraft:stone"), Mat.of("minecraft:andesite"));
        b.add(box(-14, 0, -8, 14, 26, 2), rock);
        b.add(ellipsoid(-6, 14, 2.6, 5.0, 7.6, 4.4), rock);
        b.add(ellipsoid(6, 14, 2.6, 5.0, 7.6, 4.4), rock);
        for (int x : new int[]{-6, 6}) {
            b.add(frustum(new Vec3(x, 16.4, 5.0), new Vec3(x, 11.6, 8.0), 1.0, 1.6), rock);
            b.carve(ellipsoid(x - 2.0, 16.4, 6.8, 1.0, 0.7, 1.0));
            b.carve(ellipsoid(x + 2.0, 16.4, 6.8, 1.0, 0.7, 1.0));
            b.add(box(x - 3.6, 17.8, 4.6, x + 3.6, 18.6, 6.4), rock);
            b.carve(box(x - 2.0, 8.8, 6.0, x + 2.0, 9.4, 8.0));
            b.add(ellipsoid(x, 21.6, 1.0, 5.6, 2.6, 3.6), rock);
        }
        b.add(box(-14, 0, 2, 14, 3, 8), "minecraft:cobblestone");
        return b.build();
    }

    static Sculpt colossus() {
        Humanoid.Spec s = mono(BRONZE).arms(ArmPose.UP, ArmPose.HOLD).style(HairStyles.MESSY).bulk(1.15);
        s.extra(b -> {
            b.add(box(-3.6, 12.0, -2.6, 3.6, 15.6, 2.6), "minecraft:exposed_copper");
            for (int i = 0; i < 7; i++) {
                double a = Math.toRadians(-70 + i * 23);
                b.add(horn(Math.sin(a) * 3.4, 29.6, Math.cos(a) * 3.0, Math.sin(a) * 6.0, 31.0, Math.cos(a) * 5.0, 0.5), BRONZE);
            }
            Vec3 h = Humanoid.hand(ArmPose.UP, 1.15, true);
            b.add(frustum(h, h.add(new Vec3(0, 3.0, 0)), 0.6, 1.0), BRONZE);
            b.add(ellipsoid(h.x(), h.y() + 4.4, h.z(), 1.1, 1.8, 1.1), "minecraft:glowstone");
            Vec3 r = Humanoid.hand(ArmPose.HOLD, 1.15, false);
            b.add(limb(r.add(new Vec3(0, -14, 0)), r.add(new Vec3(0, 3, 0)), 0.35, 0.35), BRONZE);
        });
        return onPedestal(Humanoid.build(s), 6.0, 6, "minecraft:stone_bricks");
    }
}
