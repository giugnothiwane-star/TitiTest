package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Humanoid;
import com.ultimateautobuilder.core.generator.HairStyles;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;

import static com.ultimateautobuilder.core.generator.Creature.BLACK;
import static com.ultimateautobuilder.core.generator.Creature.WHITE;
import static com.ultimateautobuilder.core.generator.Creature.eyesWhite;
import static com.ultimateautobuilder.core.generator.Creature.horn;
import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.generator.Creature.wing;
import static com.ultimateautobuilder.core.geom.Shapes.box;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** 20 dragons et monstres. */
public final class DragonModels {
    private DragonModels() {
    }

    /** Palette d'un dragon occidental. */
    record DragonColors(String body, String belly, String wing, String horn, String eye) {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.DRAGONS);
        SizeSpec D = SizeSpec.height(12, 28, 56, 110);
        r.sculpt("dragon_rouge", "Dragon rouge", 5, D, () -> dragon(new DragonColors("minecraft:red_concrete", "minecraft:orange_terracotta",
                "minecraft:red_terracotta", "minecraft:black_concrete", "minecraft:yellow_concrete"), 4, false), "dragon", "feu", "ailes");
        r.sculpt("dragon_glace", "Dragon de glace", 5, D, () -> dragon(new DragonColors("minecraft:light_blue_concrete", WHITE,
                "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:blue_concrete"), 4, false), "dragon", "glace", "givre");
        r.sculpt("dragon_ender", "Dragon de l'Ender", 5, D, () -> dragon(new DragonColors(BLACK, "minecraft:gray_concrete",
                "minecraft:gray_concrete", "minecraft:gray_concrete", "minecraft:magenta_concrete"), 4, false), "dragon", "ender", "end", "boss");
        r.sculpt("dragon_dore", "Dragon doré", 5, D, () -> dragon(new DragonColors("minecraft:gold_block", "minecraft:yellow_terracotta",
                "minecraft:orange_concrete", "minecraft:white_concrete", "minecraft:red_concrete"), 4, false), "dragon", "or", "tresor");
        r.sculpt("wyverne", "Wyverne verte", 4, D, () -> dragon(new DragonColors("minecraft:green_concrete", "minecraft:lime_terracotta",
                "minecraft:green_terracotta", "minecraft:white_terracotta", "minecraft:orange_concrete"), 2, false), "dragon", "wyvern");
        r.sculpt("dragon_squelette", "Dragon squelette", 5, D, () -> dragon(new DragonColors("minecraft:bone_block", "minecraft:bone_block",
                "minecraft:gray_stained_glass", "minecraft:bone_block", "minecraft:cyan_concrete"), 4, true), "dragon", "squelette", "os", "mort");
        r.sculpt("dragon_chinois", "Dragon chinois", 5, SizeSpec.length(18, 44, 88, 160), DragonModels::chineseDragon, "dragon", "asie", "serpent", "lunaire");
        r.sculpt("hydre", "Hydre à trois têtes", 5, D, DragonModels::hydra, "hydre", "monstre", "serpent", "tetes");
        r.sculpt("kraken", "Kraken", 4, D, DragonModels::kraken, "monstre", "mer", "tentacules", "calmar");
        r.sculpt("araignee_geante", "Araignée géante", 3, SizeSpec.height(6, 14, 28, 56), DragonModels::spider, "monstre", "araignee", "insecte");
        r.sculpt("cyclope", "Cyclope", 4, D, DragonModels::cyclops, "monstre", "geant", "oeil", "mythologie");
        r.sculpt("golem_pierre", "Golem de pierre", 3, D, DragonModels::golem, "monstre", "golem", "pierre");
        r.sculpt("minotaure", "Minotaure", 4, D, DragonModels::minotaur, "monstre", "taureau", "mythologie", "labyrinthe");
        r.sculpt("troll", "Troll des montagnes", 4, D, DragonModels::troll, "monstre", "troll", "massue");
        r.sculpt("slime_geant", "Slime géant", 2, SizeSpec.height(6, 16, 32, 64), DragonModels::slime, "monstre", "minecraft", "slime", "gelee");
        r.sculpt("creeper_geant", "Creeper géant", 2, D, DragonModels::creeper, "monstre", "minecraft", "creeper", "explosion");
        r.sculpt("zombie_geant", "Zombie géant", 3, D, () -> mobHumanoid("minecraft:green_terracotta", "minecraft:cyan_wool", "minecraft:blue_wool",
                "minecraft:gray_concrete", true), "monstre", "minecraft", "zombie", "mort vivant");
        r.sculpt("squelette_geant", "Squelette géant", 3, D, DragonModels::skeleton, "monstre", "minecraft", "squelette", "arc");
        r.sculpt("enderman_geant", "Enderman géant", 3, SizeSpec.height(14, 32, 64, 120), DragonModels::enderman, "monstre", "minecraft", "enderman", "end");
        r.sculpt("ghast_geant", "Ghast géant", 3, D, DragonModels::ghast, "monstre", "minecraft", "ghast", "nether");
    }

    // ------------------------------------------------------------------ dragon occidental

    /** @param legs 4 = dragon quadrupède, 2 = wyverne (ailes servant de pattes avant) */
    static Sculpt dragon(DragonColors c, int legs, boolean skeletal) {
        Sculpt.Builder b = Sculpt.builder(30);
        Mat body = skeletal ? Mat.stripesY(1.2, 0.6, Mat.of(c.body()), Mat.of("minecraft:air")) : Mat.of(c.body());
        b.add(ellipsoid(0, 11.0, 0, 4.4, 4.2, 8.0), skeletal ? Mat.of(c.body()) : body);
        if (skeletal) {
            b.carve(ellipsoid(0, 11.0, 0, 3.4, 3.2, 7.0));
            for (int i = -3; i <= 3; i++) {
                b.add(boxC(0, 11.0, i * 2.0, 4.6, 4.4, 0.4).and(ellipsoid(0, 11.0, 0, 4.6, 4.4, 8.2)), c.body());
            }
            b.carve(boxC(0, 11.0, 0, 3.6, 2.0, 7.4).and(ellipsoid(0, 11.0, 0, 3.8, 3.6, 7.6)));
        } else {
            b.paint(ellipsoid(0, 9.4, 1.0, 3.4, 2.6, 7.0), Mat.stripesY(1.4, 0.3, Mat.of(c.belly()), Mat.of(c.body())));
        }
        // Cou serpentin et tête
        Vec3 n0 = new Vec3(0, 13.0, 6.6), n1 = new Vec3(0, 17.0, 9.6), n2 = new Vec3(0, 21.6, 10.6), head = new Vec3(0, 23.6, 12.4);
        b.add(tube(2.6, 1.6, n0, n1, n2, head), c.body());
        b.add(ellipsoid(head.x(), head.y(), head.z() + 0.6, 2.2, 2.0, 2.8), c.body());
        b.add(ellipsoid(head.x(), head.y() - 0.6, head.z() + 3.6, 1.6, 1.2, 2.4), c.body());
        b.carve(boxC(head.x(), head.y() - 1.2, head.z() + 4.6, 2.0, 0.2, 2.0));
        eyesWhite(b, 1.4, head.y() + 0.6, head.z() + 2.4, 0.55, c.eye());
        b.sym(horn(1.2, head.y() + 1.4, head.z() - 0.4, 2.4, head.y() + 3.6, head.z() - 4.0, 0.7), c.horn());
        b.sym(horn(1.8, head.y() + 0.6, head.z() - 0.6, 3.6, head.y() + 1.2, head.z() - 3.4, 0.45), c.horn());
        for (int i = 0; i < 4; i++) {
            Vec3 p = n0.lerp(head, i / 4.0);
            b.add(horn(0, p.y() + 1.6, p.z() - 0.4, 0, p.y() + 3.0, p.z() - 1.6, 0.5), c.horn());
        }
        for (int i = 0; i < 6; i++) {
            b.add(horn(0, 14.6 - i * 0.2, 5.0 - i * 2.4, 0, 16.4 - i * 0.3, 4.0 - i * 2.4, 0.7), c.horn());
        }
        // Pattes
        if (legs == 4) {
            b.sym(limb(3.4, 10.0, 5.0, 4.0, 5.0, 6.4, 1.6, 1.2), c.body());
            b.sym(limb(4.0, 5.0, 6.4, 3.8, 1.0, 6.2, 1.2, 1.0), c.body());
            b.sym(horn(3.8, 0.8, 7.0, 3.8, 0.4, 8.6, 0.5), c.horn());
        }
        b.sym(ellipsoid(3.6, 7.6, -4.0, 2.2, 3.4, 2.8), c.body());
        b.sym(limb(3.8, 5.6, -3.4, 4.0, 1.0, -2.6, 1.4, 1.1), c.body());
        b.sym(horn(4.0, 0.8, -2.0, 4.0, 0.4, -0.4, 0.5), c.horn());
        // Queue
        Vec3[] tail = {new Vec3(0, 10.0, -7.0), new Vec3(0, 7.0, -12.0), new Vec3(3.0, 4.0, -16.0), new Vec3(6.0, 2.4, -19.0),
                new Vec3(5.0, 2.0, -23.0)};
        b.add(tube(2.4, 0.5, tail), c.body());
        b.add(tri(new Vec3(5.0, 2.6, -22.0), new Vec3(7.4, 2.4, -25.6), new Vec3(3.0, 2.4, -25.0), 0.4), c.horn());
        // Ailes
        Vec3 root = new Vec3(3.4, 15.0, 2.0);
        Vec3 tip = new Vec3(20.0, 27.0, -2.0);
        Vec3 back = new Vec3(18.0, 15.0, -8.0);
        Vec3 low = new Vec3(4.0, 13.0, -5.0);
        if (legs == 2) {
            tip = new Vec3(16.0, 22.0, 2.0);
            back = new Vec3(15.0, 10.0, -6.0);
        }
        wing(b, root, tip, back, low, c.wing(), c.body());
        b.sym(limb(tip, new Vec3(back.x() + 1.0, back.y() + 5.0, back.z() + 1.0), 0.35, 0.25), c.body());
        if (legs == 2) {
            b.sym(limb(tip, new Vec3(tip.x() - 3.0, 0.8, tip.z() + 4.0), 0.6, 0.4), c.body());
        }
        return b.build();
    }

    static Sculpt chineseDragon() {
        Sculpt.Builder b = Sculpt.builder(16);
        String red = "minecraft:red_concrete";
        String gold = "minecraft:gold_block";
        int n = 22;
        Vec3[] path = new Vec3[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            path[i] = new Vec3(Math.sin(t * Math.PI * 3) * 4.5, 4.0 + Math.sin(t * Math.PI * 4) * 2.6 + t * 6, -t * 40 + 20);
        }
        for (int i = 0; i + 1 < n; i++) {
            double r = 2.2 - i * 0.07;
            b.add(limb(path[i], path[i + 1], r, r - 0.07), red);
            b.paint(limb(path[i], path[i + 1], r + 0.2, r).and(com.ultimateautobuilder.core.geom.Shapes.halfSpace(path[i],
                    new Vec3(0, 1, 0))), Mat.stripesY(1.0, 0.4, Mat.of("minecraft:yellow_terracotta"), Mat.of(gold)));
            if (i % 2 == 0) {
                b.add(horn(path[i].x(), path[i].y() + r, path[i].z(), path[i].x(), path[i].y() + r + 1.6, path[i].z() - 0.8, 0.6), gold);
            }
            if (i == 5 || i == 14) {
                Vec3 p = path[i];
                b.sym(limb(p.add(new Vec3(1.6, -0.6, 0)), p.add(new Vec3(3.4, -p.y() + 0.8, 0.6)), 0.7, 0.6), red);
            }
        }
        Vec3 h = path[0].add(new Vec3(0, 1.0, 2.0));
        b.add(ellipsoid(h.x(), h.y(), h.z(), 2.8, 2.4, 3.0), red);
        b.add(ellipsoid(h.x(), h.y() - 0.6, h.z() + 3.0, 2.0, 1.4, 2.4), red);
        b.carve(boxC(h.x(), h.y() - 1.2, h.z() + 4.0, 2.4, 0.2, 2.0));
        eyesWhite(b, h.x() + 1.6, h.y() + 0.8, h.z() + 2.0, 0.6, BLACK);
        b.sym(tube(0.3, 0.2, h.add(new Vec3(1.2, -0.4, 4.6)), h.add(new Vec3(4.0, 0.4, 6.0)), h.add(new Vec3(6.6, -1.6, 5.0))), gold);
        b.sym(tube(0.5, 0.3, h.add(new Vec3(1.2, 1.8, -0.6)), h.add(new Vec3(2.4, 4.2, -2.6)), h.add(new Vec3(2.0, 5.4, -4.6))), gold);
        b.add(ellipsoid(h.x(), h.y() + 2.2, h.z() - 1.6, 2.0, 1.2, 2.4), "minecraft:green_concrete");
        b.add(sphere(path[n - 1].x(), path[n - 1].y(), path[n - 1].z() - 1.0, 1.6), gold);
        return b.build();
    }

    static Sculpt hydra() {
        Sculpt.Builder b = Sculpt.builder(28);
        String g = "minecraft:green_terracotta";
        String belly = "minecraft:lime_terracotta";
        b.add(ellipsoid(0, 8.0, 0, 6.0, 5.4, 8.0), g);
        b.paint(ellipsoid(0, 6.4, 1.0, 5.0, 3.6, 7.0), belly);
        for (double z : new double[]{4.4, -4.4}) {
            b.sym(limb(4.4, 6.0, z, 5.0, 1.0, z + 0.6, 1.8, 1.4), g);
        }
        for (int i = -1; i <= 1; i++) {
            Vec3 base = new Vec3(i * 2.6, 11.0, 5.0);
            Vec3 mid = new Vec3(i * 6.0, 18.0 - Math.abs(i) * 1.6, 8.6);
            Vec3 h = new Vec3(i * 7.4, 24.0 - Math.abs(i) * 3.0, 11.6);
            b.add(tube(2.2, 1.4, base, mid, h), g);
            b.add(ellipsoid(h.x(), h.y(), h.z() + 1.0, 1.8, 1.6, 2.6), g);
            b.add(ellipsoid(h.x(), h.y() - 0.4, h.z() + 3.2, 1.2, 0.9, 1.8), g);
            b.paint(sphere(h.x() + 1.0, h.y() + 0.6, h.z() + 2.2, 0.45), "minecraft:red_concrete");
            b.paint(sphere(h.x() - 1.0, h.y() + 0.6, h.z() + 2.2, 0.45), "minecraft:red_concrete");
            b.add(horn(h.x(), h.y() + 1.2, h.z(), h.x(), h.y() + 3.2, h.z() - 1.8, 0.6), "minecraft:white_terracotta");
        }
        b.add(tube(2.6, 0.5, new Vec3(0, 7.0, -7.0), new Vec3(0, 4.0, -13.0), new Vec3(3.0, 2.0, -18.0)), g);
        return b.build();
    }

    static Sculpt kraken() {
        Sculpt.Builder b = Sculpt.builder(30);
        String m = "minecraft:purple_terracotta";
        b.add(ellipsoid(0, 18.0, -2.0, 6.0, 9.0, 6.0), m);
        b.add(com.ultimateautobuilder.core.geom.Shapes.frustum(new Vec3(0, 25, -2), new Vec3(0, 31, -3), 4.6, 0.5), m);
        b.sym(tri(new Vec3(3.0, 26.0, -2.0), new Vec3(8.0, 30.0, -3.0), new Vec3(3.0, 30.0, -3.0), 0.5), m);
        eyesWhite(b, 3.0, 14.0, 3.2, 1.6, "minecraft:yellow_concrete");
        b.paintSym(boxC(3.0, 14.0, 4.6, 0.3, 1.2, 0.6), BLACK);
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(i * 36);
            double cx = Math.cos(a), cz = Math.sin(a);
            double len = (i % 3 == 0) ? 15 : 11;
            b.add(tube(1.8, 0.4, new Vec3(cx * 3.0, 10.0, cz * 3.0 - 1.0), new Vec3(cx * 7.0, 4.0, cz * 7.0 - 1.0),
                    new Vec3(cx * len, 1.4, cz * len - 1.0), new Vec3(cx * (len + 3), 6.0 + (i % 2) * 6, cz * (len + 3) - 1.0)), m);
        }
        b.paint(boxC(0, 1.6, 0, 22, 1.4, 22), "minecraft:pink_terracotta");
        return b.build();
    }

    static Sculpt spider() {
        Sculpt.Builder b = Sculpt.builder(10);
        String body = "minecraft:black_wool";
        b.add(ellipsoid(0, 5.0, -4.0, 4.0, 3.2, 4.6), body);
        b.add(ellipsoid(0, 4.6, 2.4, 2.8, 2.4, 2.8), "minecraft:gray_concrete");
        b.paintSym(sphere(1.2, 5.6, 5.0, 0.6), "minecraft:red_concrete");
        b.paintSym(sphere(0.6, 4.6, 5.2, 0.45), "minecraft:red_concrete");
        for (int i = 0; i < 4; i++) {
            double z = 3.6 - i * 1.6;
            double spread = 1.2 + i * 0.4;
            b.sym(limb(2.0, 5.0, z, 6.4, 8.6, z + (1.5 - i) * spread, 0.5, 0.45), body);
            b.sym(limb(6.4, 8.6, z + (1.5 - i) * spread, 9.6, 0.4, z + (1.5 - i) * spread * 2, 0.45, 0.3), body);
        }
        b.sym(horn(0.8, 3.6, 4.8, 0.6, 2.0, 5.6, 0.4), "minecraft:white_terracotta");
        return b.build();
    }

    // ------------------------------------------------------------------ humanoïdes monstrueux

    static Sculpt cyclops() {
        String skin = "minecraft:yellow_terracotta";
        return Humanoid.build(Humanoid.spec().skin(skin).hair(BLACK).style(HairStyles.BALD).noEyebrows().top(skin).sleeve(skin)
                .pants("minecraft:brown_terracotta").belt("minecraft:brown_concrete").shoes(skin).bulk(1.35).mouth(BLACK)
                .arms(Humanoid.ArmPose.DOWN, Humanoid.ArmPose.HOLD)
                .extra(b -> {
                    b.paint(boxC(0, 26.8, 3.6, 4.5, 2.0, 1.4), skin);
                    b.paint(sphere(0, 27.0, 3.9, 1.6), WHITE);
                    b.paint(sphere(0, 27.0, 4.4, 0.9), "minecraft:red_concrete");
                    b.add(horn(0, 30.4, 0, 0, 33.0, -0.6, 0.9), "minecraft:white_terracotta");
                    Vec3 h = Humanoid.hand(Humanoid.ArmPose.HOLD, 1.35, false);
                    b.add(limb(h.add(new Vec3(0, -8, 1.0)), h.add(new Vec3(0, 6, -1.0)), 0.8, 1.6), "minecraft:stripped_oak_log");
                }));
    }

    static Sculpt golem() {
        Sculpt.Builder b = Sculpt.builder(30);
        Mat stone = Mat.mix(5, 1.2, 0.3, Mat.of("minecraft:stone"), Mat.of("minecraft:mossy_cobblestone"));
        b.add(box(-7, 10, -3.5, 7, 22, 3.5), stone);
        b.add(box(-4, 22, -3, 4, 28, 3), stone);
        b.paintSym(boxC(1.8, 25.6, 3.1, 0.8, 0.8, 0.3), "minecraft:orange_concrete");
        b.add(box(-6, 6, -3, 6, 10, 3), stone);
        b.sym(box(1.2, 0, -2.6, 5.8, 6, 2.6), stone);
        b.sym(box(7, 4, -2.4, 11, 21, 2.4), stone);
        b.sym(box(7.4, 1, -2.8, 11.6, 5, 2.8), "minecraft:cobblestone");
        b.paint(boxC(0, 16, 3.6, 3, 3, 0.6), "minecraft:emerald_block");
        return b.build();
    }

    static Sculpt minotaur() {
        String fur = "minecraft:brown_terracotta";
        return Humanoid.build(Humanoid.spec().skin(fur).hair(fur).style(HairStyles.BALD).noEyebrows().top(fur).sleeve(fur)
                .pants("minecraft:red_terracotta").belt("minecraft:gold_block").shoes("minecraft:black_concrete").bulk(1.3)
                .eyes("minecraft:red_concrete").arms(Humanoid.ArmPose.DOWN, Humanoid.ArmPose.HOLD)
                .extra(b -> {
                    b.add(ellipsoid(0, 25.6, 3.2, 2.6, 2.0, 2.8), fur);
                    b.paint(sphere(0, 25.4, 5.8, 1.4), "minecraft:pink_terracotta");
                    b.add(com.ultimateautobuilder.core.geom.Shapes.torusY(0, 25.0, 6.2, 0.7, 0.25).rotated(0, 90, 0, new Vec3(0, 25, 6.2)),
                            "minecraft:gold_block");
                    b.sym(limb(3.0, 29.0, 0, 6.4, 29.6, 0.4, 0.8, 0.6), "minecraft:white_terracotta");
                    b.sym(horn(6.4, 29.6, 0.4, 7.0, 32.6, 1.2, 0.6), "minecraft:white_terracotta");
                    Vec3 h = Humanoid.hand(Humanoid.ArmPose.HOLD, 1.3, false);
                    b.add(limb(h.add(new Vec3(0, -6, 0)), h.add(new Vec3(0, 10, 0)), 0.4, 0.4), "minecraft:dark_oak_log");
                    b.add(boxC(h.x(), h.y() + 9, h.z(), 0.4, 2.6, 2.4), "minecraft:iron_block");
                }));
    }

    static Sculpt troll() {
        String skin = "minecraft:green_terracotta";
        return Humanoid.build(Humanoid.spec().skin(skin).hair(BLACK).style(HairStyles.BALD).noEyebrows().top("minecraft:brown_wool")
                .sleeve(skin).pants("minecraft:brown_terracotta").shoes(skin).bulk(1.5).legScale(0.8)
                .arms(Humanoid.ArmPose.DOWN, Humanoid.ArmPose.HOLD)
                .extra(b -> {
                    b.add(ellipsoid(0, 17.0, 1.4, 5.6, 5.0, 4.0), skin);
                    b.add(ellipsoid(0, 25.6, 3.8, 1.2, 1.6, 1.4), skin);
                    b.sym(horn(1.4, 24.4, 3.4, 1.6, 26.2, 4.2, 0.45), WHITE);
                    b.sym(limb(3.8, 27.0, 0, 6.0, 28.6, -0.6, 0.6, 0.2), skin);
                    Vec3 h = Humanoid.hand(Humanoid.ArmPose.HOLD, 1.5, false);
                    b.add(limb(h.add(new Vec3(0, -2, 0)), h.add(new Vec3(0, 3, 8)), 0.8, 2.0), "minecraft:oak_log");
                }));
    }

    // ------------------------------------------------------------------ créatures de Minecraft

    static Sculpt slime() {
        Sculpt.Builder b = Sculpt.builder(16);
        b.add(box(-8, 0, -8, 8, 16, 8), "minecraft:slime_block");
        b.carve(box(-6, 2, -6, 6, 14, 6));
        b.add(box(-4, 4, -4, 4, 12, 4), "minecraft:lime_concrete");
        b.paintSym(box(2, 9, 7, 5, 12, 8), "minecraft:green_concrete");
        b.paint(box(-2, 5, 7, 2, 6, 8), "minecraft:green_concrete");
        return b.build();
    }

    static Sculpt creeper() {
        Sculpt.Builder b = Sculpt.builder(26);
        Mat skin = Mat.mix(9, 1.0, 0.35, Mat.of("minecraft:lime_concrete"), Mat.of("minecraft:green_concrete"));
        b.add(box(-4, 18, -4, 4, 26, 4), skin);
        b.add(box(-4, 6, -2, 4, 18, 2), skin);
        b.add(box(-4, 0, 2, 4, 6, 6), skin);
        b.add(box(-4, 0, -6, 4, 6, -2), skin);
        b.paint(box(-3, 22, 3.5, -1, 24, 4.1), BLACK);
        b.paint(box(1, 22, 3.5, 3, 24, 4.1), BLACK);
        b.paint(box(-1, 19, 3.5, 1, 22, 4.1), BLACK);
        b.paint(box(-2, 18, 3.5, -1, 21, 4.1), BLACK);
        b.paint(box(1, 18, 3.5, 2, 21, 4.1), BLACK);
        return b.build();
    }

    static Sculpt mobHumanoid(String skin, String shirt, String pants, String shoes, boolean armsForward) {
        Sculpt.Builder b = Sculpt.builder(32);
        b.add(box(-4, 24, -4, 4, 32, 4), skin);
        b.add(box(-4, 12, -2, 4, 24, 2), shirt);
        b.add(box(-4, 0, -2, 0, 12, 2), pants);
        b.add(box(0, 0, -2, 4, 12, 2), pants);
        b.add(box(-4, 0, -2, 4, 2, 2), shoes);
        if (armsForward) {
            b.add(box(4, 20, -2, 8, 24, 10), skin);
            b.add(box(-8, 20, -2, -4, 24, 10), skin);
            b.add(box(4, 20, -2, 8, 24, 2), shirt);
            b.add(box(-8, 20, -2, -4, 24, 2), shirt);
        } else {
            b.add(box(4, 12, -2, 8, 24, 2), skin);
            b.add(box(-8, 12, -2, -4, 24, 2), skin);
        }
        b.paint(box(-3, 27, 3.5, -1, 29, 4.1), BLACK);
        b.paint(box(1, 27, 3.5, 3, 29, 4.1), BLACK);
        b.paint(box(-2, 25, 3.5, 2, 26, 4.1), "minecraft:green_concrete");
        return b.build();
    }

    static Sculpt skeleton() {
        Sculpt.Builder b = Sculpt.builder(32);
        String bone = "minecraft:bone_block";
        String light = "minecraft:white_concrete";
        b.add(box(-4, 24, -4, 4, 32, 4), light);
        b.paint(box(-3, 27, 3.5, -1, 29, 4.1), BLACK);
        b.paint(box(1, 27, 3.5, 3, 29, 4.1), BLACK);
        b.paint(box(-2, 25, 3.5, 2, 26, 4.1), "minecraft:gray_concrete");
        b.add(box(-0.8, 12, -0.8, 0.8, 24, 0.8), bone);
        for (int i = 0; i < 4; i++) {
            b.add(box(-3.6, 15 + i * 2.2, -1.6, 3.6, 15.8 + i * 2.2, 1.6), bone);
        }
        b.add(box(-3.6, 11, -1.6, 3.6, 12.6, 1.6), bone);
        b.sym(box(1.4, 0, -0.8, 2.8, 11, 0.8), bone);
        b.sym(box(4.6, 12, -0.8, 6.0, 23.4, 0.8), bone);
        b.add(box(-6.0, 20, -0.8, -4.6, 23.4, 11), bone);
        b.add(com.ultimateautobuilder.core.geom.Shapes.torusY(-5.3, 21.0, 11.0, 6.0, 0.5).and(box(-6, 15, 11, -4.6, 27, 18)),
                "minecraft:stripped_oak_log");
        b.add(box(-5.6, 15.4, 11.4, -5.0, 26.6, 11.8), "minecraft:white_wool");
        return b.build();
    }

    static Sculpt enderman() {
        Sculpt.Builder b = Sculpt.builder(58);
        String black = "minecraft:black_concrete";
        b.add(box(-4, 50, -4, 4, 58, 4), black);
        b.paint(box(-4, 52.6, 3.6, -1, 53.6, 4.1), "minecraft:magenta_concrete");
        b.paint(box(1, 52.6, 3.6, 4, 53.6, 4.1), "minecraft:magenta_concrete");
        b.paint(box(-2.4, 52.6, 3.6, -1, 53.6, 4.1), "minecraft:purple_concrete");
        b.paint(box(1, 52.6, 3.6, 2.4, 53.6, 4.1), "minecraft:purple_concrete");
        b.add(box(-4, 38, -2, 4, 50, 2), black);
        b.sym(box(1, 0, -1, 3, 38, 1), black);
        b.sym(box(4, 22, -1, 6, 50, 1), black);
        return b.build();
    }

    static Sculpt ghast() {
        Sculpt.Builder b = Sculpt.builder(30);
        String w = "minecraft:white_concrete";
        b.add(box(-8, 14, -8, 8, 30, 8), w);
        b.paint(box(-6, 22, 7.6, -2, 24, 8.1), "minecraft:gray_concrete");
        b.paint(box(2, 22, 7.6, 6, 24, 8.1), "minecraft:gray_concrete");
        b.paint(box(-3, 16, 7.6, 3, 19, 8.1), "minecraft:gray_concrete");
        double[][] t = {{-6, -6, 12}, {-1, -6, 8}, {4, -6, 10}, {-5, -1, 9}, {0, 0, 13}, {5, 0, 7}, {-4, 5, 11}, {1, 5, 9}, {6, 5, 12}};
        for (double[] p : t) {
            b.add(box(p[0] - 1, 14 - p[2], p[1] - 1, p[0] + 1, 14, p[1] + 1), w);
        }
        return b.build();
    }
}
