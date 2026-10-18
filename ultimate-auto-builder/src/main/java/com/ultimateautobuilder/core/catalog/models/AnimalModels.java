package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Quadruped;
import com.ultimateautobuilder.core.generator.Quadruped.Ears;
import com.ultimateautobuilder.core.generator.Quadruped.Tail;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;

import static com.ultimateautobuilder.core.generator.Creature.BLACK;
import static com.ultimateautobuilder.core.generator.Creature.WHITE;
import static com.ultimateautobuilder.core.generator.Creature.eyes;
import static com.ultimateautobuilder.core.generator.Creature.eyesWhite;
import static com.ultimateautobuilder.core.generator.Creature.horn;
import static com.ultimateautobuilder.core.generator.Creature.tail;
import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.halfSpace;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** 30 animaux : mammifères (gabarit quadrupède), oiseaux et animaux marins. */
public final class AnimalModels {
    private static final String BROWN = "minecraft:brown_terracotta";
    private static final String DBROWN = "minecraft:brown_concrete";
    private static final String GRAY = "minecraft:stone";
    private static final String LGRAY = "minecraft:light_gray_concrete";
    private static final String PINK = "minecraft:pink_terracotta";
    private static final String ORANGE = "minecraft:orange_concrete";
    private static final String YELLOW = "minecraft:yellow_concrete";
    private static final String SAND = "minecraft:sandstone";
    private static final String GREEN = "minecraft:green_concrete";

    private AnimalModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.ANIMALS);
        SizeSpec S = Reg.CREATURE;
        r.sculpt("vache", "Vache", 2, S, AnimalModels::cow, "animal", "ferme", "cow");
        r.sculpt("cochon", "Cochon", 2, S, AnimalModels::pig, "animal", "ferme", "pig");
        r.sculpt("mouton", "Mouton", 2, S, AnimalModels::sheep, "animal", "ferme", "laine");
        r.sculpt("cheval", "Cheval", 3, S, AnimalModels::horse, "animal", "ferme", "monture");
        r.sculpt("loup", "Loup", 3, S, AnimalModels::wolf, "animal", "foret", "chien");
        r.sculpt("chat", "Chat", 2, S, AnimalModels::cat, "animal", "felin", "domestique");
        r.sculpt("renard", "Renard", 3, S, AnimalModels::fox, "animal", "foret");
        r.sculpt("lion", "Lion", 3, S, AnimalModels::lion, "animal", "felin", "savane", "roi");
        r.sculpt("tigre", "Tigre", 3, S, AnimalModels::tiger, "animal", "felin", "jungle");
        r.sculpt("elephant", "Éléphant", 3, Reg.BIG_CREATURE, AnimalModels::elephant, "animal", "savane", "trompe");
        r.sculpt("girafe", "Girafe", 3, Reg.BIG_CREATURE, AnimalModels::giraffe, "animal", "savane");
        r.sculpt("ours_brun", "Ours brun", 3, S, () -> bear(DBROWN, null), "animal", "foret", "ours");
        r.sculpt("panda", "Panda", 3, S, () -> bear(WHITE, BLACK), "animal", "bambou", "ours");
        r.sculpt("ours_polaire", "Ours polaire", 3, S, () -> bear("minecraft:white_wool", null), "animal", "banquise", "ours");
        r.sculpt("cerf", "Cerf", 3, S, AnimalModels::deer, "animal", "foret", "bois");
        r.sculpt("lapin", "Lapin", 2, S, AnimalModels::rabbit, "animal", "foret", "lievre");
        r.sculpt("chameau", "Chameau", 3, Reg.BIG_CREATURE, AnimalModels::camel, "animal", "desert", "bosse");
        r.sculpt("rhinoceros", "Rhinocéros", 3, S, AnimalModels::rhino, "animal", "savane", "corne");
        r.sculpt("hippopotame", "Hippopotame", 3, S, AnimalModels::hippo, "animal", "riviere");
        r.sculpt("crocodile", "Crocodile", 3, SizeSpec.length(10, 24, 48, 90), AnimalModels::crocodile, "animal", "reptile", "riviere");
        r.sculpt("aigle", "Aigle royal", 3, S, AnimalModels::eagle, "animal", "oiseau", "rapace");
        r.sculpt("hibou", "Hibou", 2, S, AnimalModels::owl, "animal", "oiseau", "chouette", "nuit");
        r.sculpt("manchot", "Manchot empereur", 2, S, AnimalModels::penguin, "animal", "oiseau", "pingouin", "banquise");
        r.sculpt("perroquet", "Perroquet ara", 2, S, AnimalModels::parrot, "animal", "oiseau", "jungle");
        r.sculpt("flamant_rose", "Flamant rose", 2, S, AnimalModels::flamingo, "animal", "oiseau");
        r.sculpt("baleine", "Baleine bleue", 3, SizeSpec.length(14, 32, 64, 128), AnimalModels::whale, "animal", "mer", "ocean");
        r.sculpt("dauphin", "Dauphin", 2, SizeSpec.length(10, 22, 44, 88), AnimalModels::dolphin, "animal", "mer", "ocean");
        r.sculpt("requin", "Requin blanc", 3, SizeSpec.length(10, 26, 52, 100), AnimalModels::shark, "animal", "mer", "ocean");
        r.sculpt("pieuvre", "Pieuvre géante", 3, S, AnimalModels::octopus, "animal", "mer", "poulpe", "tentacules");
        r.sculpt("tortue_marine", "Tortue marine", 2, SizeSpec.length(10, 22, 44, 88), AnimalModels::seaTurtle, "animal", "mer", "tortue");
    }

    // ------------------------------------------------------------------ ferme

    static Sculpt cow() {
        Mat hide = Mat.spots(7, 2.0, 0.6, Mat.of(WHITE), Mat.of(BLACK));
        return Quadruped.build(Quadruped.spec().size(6.5, 3.4, 3.6, 5.5, 1.2).neck(1.5, 10).head(2.6, 1.8, 1.7)
                .body(hide).headMat(WHITE).snoutMat(PINK).nose(DBROWN).hooves(GRAY)
                .ears(Ears.HORNS_CURVED, LGRAY).tail(Tail.TUFT, BLACK, 5)
                .extra((b, f) -> b.add(ellipsoid(0, f.bodyCenter().y() - 3.2, -3.2, 1.6, 1.0, 1.4), PINK)));
    }

    static Sculpt pig() {
        String p = "minecraft:pink_concrete";
        return Quadruped.build(Quadruped.spec().size(5.5, 3.6, 3.4, 3.0, 1.1).neck(0.6, 5).head(3.0, 1.2, 1.5)
                .body(p).snoutMat(PINK).nose("minecraft:magenta_terracotta").ears(Ears.FLOPPY, p).tail(Tail.CURL, p, 2));
    }

    static Sculpt sheep() {
        return Quadruped.build(Quadruped.spec().size(5.5, 4.0, 3.8, 4.0, 0.9).neck(0.8, 20).head(2.2, 1.4, 1.2)
                .body("minecraft:white_wool").legs(LGRAY).headMat(LGRAY).ears(Ears.FLOPPY, LGRAY).tail(Tail.THICK, "minecraft:white_wool", 1.4)
                .extra((b, f) -> b.add(sphere(f.headCenter().x(), f.headCenter().y() + 1.6, f.headCenter().z() - 0.6, 1.8), "minecraft:white_wool")));
    }

    static Sculpt horse() {
        String coat = "minecraft:brown_terracotta";
        return Quadruped.build(Quadruped.spec().size(6.0, 2.8, 3.0, 8.0, 0.9).neck(5.0, 55).head(1.9, 3.2, 1.3)
                .body(coat).hooves(BLACK).ears(Ears.POINTY, coat).tail(Tail.BUSHY, BLACK, 6)
                .extra((b, f) -> {
                    Vec3 a = f.neckBase().add(new Vec3(0, 1.6, -0.8));
                    Vec3 c = f.headCenter().add(new Vec3(0, 1.4, -1.6));
                    b.add(limb(a, c, 0.9, 0.8), BLACK);
                    b.paint(boxC(0, 1.6, 0, 6, 1.0, 9), WHITE);
                }));
    }

    // ------------------------------------------------------------------ canidés et félins

    static Sculpt wolf() {
        Mat fur = Mat.mix(11, 0.9, 0.3, Mat.of(LGRAY), Mat.of(GRAY));
        return Quadruped.build(Quadruped.spec().size(5.0, 2.4, 2.6, 5.5, 0.8).neck(1.6, 30).head(2.2, 2.4, 1.0)
                .body(fur).belly(WHITE).headMat(LGRAY).ears(Ears.POINTY, GRAY).tail(Tail.BUSHY, GRAY, 6).eyes(YELLOW)
                .extra((b, f) -> b.add(sphere(f.neckBase().x(), f.neckBase().y() + 0.4, f.neckBase().z(), 2.4), LGRAY)));
    }

    static Sculpt cat() {
        String o = "minecraft:orange_terracotta";
        Mat tabby = Mat.stripesX(1.6, 0.5, Mat.of(o), Mat.of("minecraft:brown_terracotta"));
        return Quadruped.build(Quadruped.spec().size(4.0, 2.0, 2.2, 4.0, 0.7).neck(0.6, 50).head(2.2, 0.6, 0.9)
                .body(tabby).belly(WHITE).headMat(o).snoutMat(WHITE).nose(PINK).ears(Ears.POINTY, o).eyes(GREEN)
                .tail(Tail.NONE, o, 0)
                .extra((b, f) -> tail(b, o, 0.55, 0.45, f.tailBase(), f.tailBase().add(new Vec3(0, 3.0, -2.0)),
                        f.tailBase().add(new Vec3(0, 5.6, -1.2)))));
    }

    static Sculpt fox() {
        return Quadruped.build(Quadruped.spec().size(4.4, 2.0, 2.2, 4.2, 0.65).neck(1.0, 30).head(2.0, 2.4, 0.8)
                .body(ORANGE).belly(WHITE).legs(DBROWN).headMat(ORANGE).snoutMat(WHITE).ears(Ears.POINTY, ORANGE)
                .tail(Tail.BUSHY, ORANGE, 7)
                .extra((b, f) -> {
                    Vec3 tip = f.tailBase().add(new Vec3(0, -2.1, -7.0));
                    b.add(sphere(tip.x(), tip.y(), tip.z(), 1.6), WHITE);
                }));
    }

    static Sculpt lion() {
        String fur = "minecraft:yellow_terracotta";
        return Quadruped.build(Quadruped.spec().size(5.5, 2.8, 3.0, 6.0, 1.1).neck(0.8, 30).head(2.5, 1.6, 1.4)
                .body(fur).headMat(fur).snoutMat(SAND).ears(Ears.ROUND, fur).tail(Tail.TUFT, DBROWN, 6).eyes(DBROWN)
                .extra((b, f) -> {
                    Vec3 h = f.headCenter();
                    b.add(ellipsoid(h.x(), h.y(), h.z() - 1.2, 4.2, 4.2, 3.0), "minecraft:brown_terracotta");
                    b.add(sphere(h.x(), h.y(), h.z() + 0.4, 2.5), fur);
                    b.add(limb(h.add(new Vec3(0, -0.6, 1.4)), f.snoutTip(), 1.4, 1.2), SAND);
                    b.paint(sphere(f.snoutTip().x(), f.snoutTip().y() + 0.4, f.snoutTip().z() + 0.9, 0.6), DBROWN);
                    b.paintSym(sphere(h.x() + 1.3, h.y() + 0.8, h.z() + 2.3, 0.5), DBROWN);
                }));
    }

    static Sculpt tiger() {
        Mat stripes = Mat.stripesX(2.0, 0.55, Mat.of(ORANGE), Mat.of(BLACK));
        return Quadruped.build(Quadruped.spec().size(6.0, 2.6, 2.8, 5.5, 1.1).neck(0.8, 25).head(2.4, 1.4, 1.3)
                .body(stripes).belly(WHITE).headMat(ORANGE).snoutMat(WHITE).ears(Ears.ROUND, ORANGE).eyes(YELLOW)
                .tail(Tail.THIN, ORANGE, 7)
                .extra((b, f) -> {
                    Vec3 h = f.headCenter();
                    b.paint(boxC(h.x(), h.y() + 1.6, h.z(), 0.3, 1.0, 3.0), BLACK);
                    b.paintSym(boxC(h.x() + 2.2, h.y(), h.z(), 0.4, 0.3, 2.0), BLACK);
                }));
    }

    // ------------------------------------------------------------------ grands mammifères

    static Sculpt elephant() {
        return Quadruped.build(Quadruped.spec().size(7.0, 5.0, 5.0, 7.0, 2.0).neck(0.4, 20).head(4.0, 0, 0)
                .body(GRAY).ears(Ears.BIG_FLAP, GRAY).tail(Tail.THIN, GRAY, 6).eyes(BLACK)
                .extra((b, f) -> {
                    Vec3 h = f.headCenter();
                    b.add(tube(1.5, 0.7, h.add(new Vec3(0, -1.0, 3.0)), h.add(new Vec3(0, -4.0, 5.0)),
                            h.add(new Vec3(0, -8.0, 5.6)), h.add(new Vec3(0, -10.0, 6.8))), GRAY);
                    b.sym(tube(0.7, 0.3, h.add(new Vec3(1.6, -2.2, 3.0)), h.add(new Vec3(2.0, -4.6, 5.4)),
                            h.add(new Vec3(1.6, -4.2, 7.4))), WHITE);
                }));
    }

    static Sculpt giraffe() {
        Mat coat = Mat.spots(23, 1.6, 0.58, Mat.of("minecraft:yellow_terracotta"), Mat.of("minecraft:brown_terracotta"));
        return Quadruped.build(Quadruped.spec().size(5.5, 2.6, 2.8, 11.0, 0.8).neck(13.0, 72).head(1.8, 2.4, 1.0)
                .body(coat).headMat("minecraft:yellow_terracotta").ears(Ears.POINTY, "minecraft:yellow_terracotta")
                .tail(Tail.TUFT, BLACK, 6)
                .extra((b, f) -> {
                    Vec3 h = f.headCenter();
                    b.sym(limb(h.add(new Vec3(0.6, 1.2, -0.4)), h.add(new Vec3(0.8, 3.0, -0.6)), 0.35, 0.3), DBROWN);
                    b.sym(sphere(h.x() + 0.8, h.y() + 3.0, h.z() - 0.6, 0.5), DBROWN);
                    b.paint(limb(f.neckBase(), h, 1.9, 1.4), coat);
                }));
    }

    static Sculpt bear(String fur, String accent) {
        Quadruped.Spec q = Quadruped.spec().size(5.5, 3.6, 3.8, 4.5, 1.6).neck(0.6, 15).head(2.8, 1.6, 1.4)
                .body(fur).ears(Ears.ROUND, accent != null ? accent : fur).tail(Tail.NONE, fur, 0).eyes(BLACK);
        if (accent != null) {
            q.legs(accent).snoutMat(WHITE).extra((b, f) -> {
                Vec3 h = f.headCenter();
                b.paintSym(ellipsoid(h.x() + 1.1, h.y() + 0.5, h.z() + 2.2, 0.9, 1.1, 0.8), accent);
                b.paintSym(sphere(h.x() + 1.1, h.y() + 0.7, h.z() + 2.7, 0.35), WHITE);
                b.paint(boxC(0, f.bodyCenter().y() + 1.0, f.bodyCenter().z() + 3.0, 4.2, 1.6, 2.0), accent);
            });
        } else {
            q.snoutMat(fur.equals(DBROWN) ? "minecraft:brown_terracotta" : fur);
        }
        return Quadruped.build(q);
    }

    static Sculpt deer() {
        String coat = "minecraft:brown_terracotta";
        return Quadruped.build(Quadruped.spec().size(5.0, 2.2, 2.6, 7.5, 0.6).neck(3.4, 60).head(1.8, 2.0, 0.9)
                .body(coat).belly(WHITE).hooves(BLACK).ears(Ears.ANTLERS, "minecraft:stripped_oak_log").tail(Tail.TUFT, WHITE, 1.4));
    }

    static Sculpt rabbit() {
        String fur = "minecraft:white_terracotta";
        return Quadruped.build(Quadruped.spec().size(3.2, 2.4, 2.6, 1.6, 0.9).neck(0.4, 50).head(2.0, 0.6, 0.9)
                .body(fur).snoutMat(WHITE).nose(PINK).ears(Ears.LONG, fur).tail(Tail.TUFT, WHITE, 0.8)
                .extra((b, f) -> b.sym(ellipsoid(1.6, 1.0, -1.4, 1.2, 1.2, 2.2), fur)));
    }

    static Sculpt camel() {
        String sand = "minecraft:smooth_sandstone";
        return Quadruped.build(Quadruped.spec().size(6.0, 2.8, 2.8, 10.0, 0.8).neck(6.0, 45).head(1.8, 2.4, 1.0)
                .body(sand).ears(Ears.ROUND, sand).tail(Tail.TUFT, "minecraft:brown_terracotta", 4)
                .extra((b, f) -> {
                    Vec3 c = f.bodyCenter();
                    b.add(ellipsoid(0, c.y() + 3.0, c.z() + 2.0, 2.2, 2.6, 2.4), sand);
                    b.add(ellipsoid(0, c.y() + 3.0, c.z() - 2.4, 2.2, 2.6, 2.4), sand);
                }));
    }

    static Sculpt rhino() {
        return Quadruped.build(Quadruped.spec().size(6.5, 3.8, 3.8, 4.5, 1.6).neck(0.6, 0).head(2.8, 2.6, 1.9)
                .body(GRAY).ears(Ears.POINTY, GRAY).tail(Tail.TUFT, GRAY, 3)
                .extra((b, f) -> {
                    Vec3 s = f.snoutTip();
                    b.add(horn(s.x(), s.y() + 1.2, s.z() - 0.6, s.x(), s.y() + 5.0, s.z() - 0.2, 1.2), LGRAY);
                    b.add(horn(s.x(), s.y() + 1.8, s.z() - 2.6, s.x(), s.y() + 3.6, s.z() - 2.6, 0.8), LGRAY);
                }));
    }

    static Sculpt hippo() {
        String skin = "minecraft:purple_terracotta";
        return Quadruped.build(Quadruped.spec().size(6.5, 4.4, 3.8, 3.0, 1.6).neck(0.2, 0).head(3.0, 2.6, 2.6)
                .body(skin).belly(PINK).snoutMat(PINK).ears(Ears.ROUND, skin).tail(Tail.THIN, skin, 1.6)
                .extra((b, f) -> {
                    Vec3 s = f.snoutTip();
                    b.paintSym(sphere(s.x() + 1.0, s.y() + 1.6, s.z() + 0.6, 0.45), BLACK);
                    b.sym(horn(s.x() + 1.6, s.y() - 0.6, s.z(), s.x() + 1.6, s.y() + 0.6, s.z() + 0.6, 0.45), WHITE);
                }));
    }

    static Sculpt crocodile() {
        Sculpt.Builder b = Sculpt.builder(5);
        String g = "minecraft:green_terracotta";
        b.add(ellipsoid(0, 2.0, 0, 2.8, 1.6, 6.0), g);
        b.paint(ellipsoid(0, 1.2, 0, 2.6, 0.8, 5.6), "minecraft:lime_terracotta");
        b.add(limb(0, 2.0, 5.0, 0, 1.8, 13.0, 1.8, 0.9), g);
        b.add(ellipsoid(0, 2.6, 7.4, 1.8, 1.2, 1.8), g);
        b.paintSym(sphere(1.2, 3.3, 7.8, 0.4), YELLOW);
        b.carve(boxC(0, 1.6, 10.6, 3, 0.18, 3.0));
        for (int i = 0; i < 5; i++) {
            b.sym(horn(1.0, 1.6, 8.6 + i * 0.9, 1.1, 1.0, 8.7 + i * 0.9, 0.25), WHITE);
        }
        b.add(tube(1.8, 0.4, new Vec3(0, 2.0, -5.0), new Vec3(1.0, 1.6, -9.0), new Vec3(0, 1.4, -13.0), new Vec3(-1.4, 1.2, -16.0)), g);
        for (int i = 0; i < 8; i++) {
            b.add(horn(0, 3.2, 4.0 - i * 2.0, 0, 4.2, 3.6 - i * 2.0, 0.5), "minecraft:green_concrete");
        }
        for (double z : new double[]{3.4, -3.6}) {
            b.sym(limb(2.4, 1.6, z, 3.8, 0.4, z + 0.8, 0.8, 0.7), g);
        }
        return b.build();
    }

    // ------------------------------------------------------------------ oiseaux

    static Sculpt eagle() {
        Sculpt.Builder b = Sculpt.builder(16);
        String body = DBROWN;
        b.add(ellipsoid(0, 7.4, 0, 2.6, 3.8, 2.4), body);
        b.add(sphere(0, 12.0, 0.6, 1.9), WHITE);
        b.add(horn(0, 11.6, 2.2, 0, 10.4, 3.8, 0.8), YELLOW);
        b.paintSym(sphere(1.0, 12.4, 1.8, 0.35), BLACK);
        b.sym(tri(new Vec3(2.0, 10.0, -0.4), new Vec3(13.0, 13.0, -2.0), new Vec3(10.0, 6.0, -1.6), 0.5), body);
        b.sym(tri(new Vec3(2.0, 10.0, -0.4), new Vec3(10.0, 6.0, -1.6), new Vec3(2.6, 5.4, -1.0), 0.5), body);
        b.add(tri(new Vec3(0, 5.0, -2.0), new Vec3(2.0, 1.6, -4.4), new Vec3(-2.0, 1.6, -4.4), 0.5), WHITE);
        b.sym(limb(1.0, 4.0, 0.4, 1.2, 0.8, 0.8, 0.5, 0.4), YELLOW);
        b.sym(ellipsoid(1.2, 0.6, 1.4, 0.8, 0.5, 1.0), YELLOW);
        return b.build();
    }

    static Sculpt owl() {
        Sculpt.Builder b = Sculpt.builder(14);
        String f = "minecraft:brown_terracotta";
        b.add(ellipsoid(0, 6.0, 0, 3.6, 4.6, 3.2), f);
        b.paint(ellipsoid(0, 5.4, 2.2, 2.6, 3.6, 1.2), Mat.spots(3, 0.8, 0.6, Mat.of("minecraft:white_terracotta"), Mat.of(f)));
        b.add(ellipsoid(0, 11.2, 0.2, 3.4, 2.8, 2.8), f);
        b.sym(frustum(new Vec3(2.2, 13.0, 0), new Vec3(2.8, 15.2, -0.4), 0.9, 0.1), f);
        b.paintSym(sphere(1.5, 11.4, 2.6, 1.3), "minecraft:white_terracotta");
        eyesWhite(b, 1.5, 11.4, 2.9, 0.9, ORANGE);
        b.paintSym(sphere(1.5, 11.4, 3.3, 0.45), BLACK);
        b.add(horn(0, 10.6, 2.6, 0, 9.6, 3.6, 0.5), YELLOW);
        b.sym(ellipsoid(3.4, 6.0, -0.4, 0.8, 3.6, 2.4), DBROWN);
        b.sym(ellipsoid(1.4, 0.6, 1.4, 0.9, 0.6, 1.0), YELLOW);
        return b.build();
    }

    static Sculpt penguin() {
        Sculpt.Builder b = Sculpt.builder(18);
        b.add(ellipsoid(0, 8.0, 0, 4.0, 7.6, 3.6), BLACK);
        b.paint(ellipsoid(0, 7.0, 2.0, 3.2, 6.6, 2.0), WHITE);
        b.add(sphere(0, 15.4, 0.4, 2.6), BLACK);
        b.paintSym(ellipsoid(2.0, 14.6, 1.0, 0.8, 1.2, 1.0), YELLOW);
        b.add(horn(0, 15.0, 2.6, 0, 14.4, 4.4, 0.6), ORANGE);
        b.paintSym(sphere(1.0, 15.8, 2.2, 0.35), WHITE);
        b.sym(ellipsoid(4.2, 8.6, 0, 0.8, 4.0, 1.6).rotated(0, 0, 18, new Vec3(4.2, 8.6, 0)), BLACK);
        b.sym(ellipsoid(1.6, 0.5, 1.6, 1.2, 0.5, 1.6), ORANGE);
        return b.build();
    }

    static Sculpt parrot() {
        Sculpt.Builder b = Sculpt.builder(18);
        String red = "minecraft:red_concrete";
        b.add(ellipsoid(0, 9.0, 0, 2.4, 3.6, 2.2), red);
        b.add(sphere(0, 13.4, 0.6, 2.0), red);
        b.paintSym(ellipsoid(1.2, 13.4, 1.8, 0.9, 1.0, 0.8), WHITE);
        b.paintSym(sphere(1.2, 13.6, 2.1, 0.35), BLACK);
        b.add(ellipsoid(0, 12.6, 2.8, 0.9, 1.2, 1.2), "minecraft:white_terracotta");
        b.add(horn(0, 12.0, 3.2, 0, 10.8, 3.4, 0.6), BLACK);
        b.sym(ellipsoid(2.4, 8.6, -0.6, 0.8, 3.4, 1.8), Mat.splitY(8.0, Mat.of("minecraft:blue_concrete"), Mat.of(YELLOW)));
        b.add(tri(new Vec3(0, 6.6, -1.6), new Vec3(1.4, -1.0, -3.6), new Vec3(-1.4, -1.0, -3.6), 0.5), red);
        b.add(limb(-3.0, 5.4, 0.8, 3.0, 5.4, 0.8, 0.45, 0.45), "minecraft:stripped_oak_log");
        b.add(limb(3.0, 5.4, 0.8, 3.0, 0, 0.8, 0.45, 0.45), "minecraft:stripped_oak_log");
        return b.build();
    }

    static Sculpt flamingo() {
        Sculpt.Builder b = Sculpt.builder(22);
        String p = "minecraft:pink_concrete";
        b.add(ellipsoid(0, 12.4, 0, 2.2, 2.2, 3.6), p);
        b.add(limb(0.4, 10.6, 0, 0.4, 0, 0.4, 0.3, 0.3), PINK);
        b.add(limb(-0.4, 10.6, 0, -0.6, 6.0, -1.6, 0.3, 0.3), PINK);
        b.add(limb(-0.6, 6.0, -1.6, -0.4, 7.0, 0.6, 0.3, 0.3), PINK);
        b.add(tube(0.6, 0.5, new Vec3(0, 13.4, 3.0), new Vec3(0, 16.4, 4.6), new Vec3(0, 18.6, 2.6), new Vec3(0, 20.4, 2.4)), p);
        b.add(sphere(0, 20.6, 2.8, 1.1), p);
        b.add(horn(0, 20.4, 3.6, 0, 19.0, 5.2, 0.5), BLACK);
        b.paintSym(sphere(0.7, 21.0, 3.2, 0.3), BLACK);
        b.add(tri(new Vec3(0, 13.0, -3.0), new Vec3(1.2, 12.4, -5.4), new Vec3(-1.2, 12.4, -5.4), 0.5), p);
        return b.build();
    }

    // ------------------------------------------------------------------ animaux marins

    static Sculpt whale() {
        Sculpt.Builder b = Sculpt.builder(10);
        String blue = "minecraft:cyan_terracotta";
        b.add(ellipsoid(0, 5.0, 2.0, 5.0, 4.4, 14.0), blue);
        b.paint(ellipsoid(0, 3.0, 4.0, 4.0, 2.6, 11.0).and(halfSpace(new Vec3(0, 3.6, 0), new Vec3(0, 1, 0))),
                Mat.stripesX(0.9, 0.3, Mat.of(LGRAY), Mat.of(GRAY)));
        b.add(limb(0, 5.0, -10.0, 0, 6.0, -19.0, 2.4, 0.8), blue);
        b.add(tri(new Vec3(0, 6.0, -18.6), new Vec3(5.6, 7.0, -22.0), new Vec3(-5.6, 7.0, -22.0), 0.6), blue);
        b.sym(tri(new Vec3(3.8, 3.4, 6.0), new Vec3(8.6, 1.0, 2.0), new Vec3(4.0, 3.0, 1.6), 0.5), blue);
        b.paintSym(sphere(3.8, 5.0, 9.6, 0.5), BLACK);
        b.carve(boxC(0, 2.6, 12.0, 6, 0.2, 5.0));
        return b.build();
    }

    static Sculpt dolphin() {
        Sculpt.Builder b = Sculpt.builder(8);
        String g = "minecraft:light_blue_terracotta";
        b.add(ellipsoid(0, 4.0, 0, 2.2, 2.4, 6.0), g);
        b.paint(ellipsoid(0, 3.0, 0, 2.0, 1.6, 5.4).and(halfSpace(new Vec3(0, 3.2, 0), new Vec3(0, 1, 0))), WHITE);
        b.add(limb(0, 4.0, 5.0, 0, 3.4, 8.6, 1.0, 0.6), g);
        b.add(tri(new Vec3(0, 6.0, 0.6), new Vec3(0, 8.6, -1.6), new Vec3(0, 6.0, -2.4), 0.45), g);
        b.add(limb(0, 4.0, -5.0, 0, 5.6, -9.0, 1.4, 0.5), g);
        b.add(tri(new Vec3(0, 5.6, -8.8), new Vec3(3.0, 6.2, -10.8), new Vec3(-3.0, 6.2, -10.8), 0.45), g);
        b.sym(tri(new Vec3(1.8, 3.0, 2.4), new Vec3(4.0, 1.4, 0.6), new Vec3(1.8, 3.0, 0.4), 0.4), g);
        b.paintSym(sphere(1.4, 4.6, 4.6, 0.35), BLACK);
        return b.build();
    }

    static Sculpt shark() {
        Sculpt.Builder b = Sculpt.builder(8);
        String g = LGRAY;
        b.add(ellipsoid(0, 4.0, 0, 2.4, 2.6, 7.0), g);
        b.paint(ellipsoid(0, 3.0, 0, 2.2, 1.8, 6.6).and(halfSpace(new Vec3(0, 3.4, 0), new Vec3(0, 1, 0))), WHITE);
        b.add(ellipsoid(0, 3.8, 6.4, 1.8, 1.8, 2.4), g);
        b.carve(boxC(0, 2.8, 8.0, 3, 0.4, 1.6));
        b.sym(horn(1.0, 3.2, 7.6, 1.0, 2.6, 7.8, 0.3), WHITE);
        b.add(tri(new Vec3(0, 6.0, 1.4), new Vec3(0, 10.0, -1.6), new Vec3(0, 6.0, -2.4), 0.5), g);
        b.add(limb(0, 4.0, -6.0, 0, 4.6, -10.0, 1.2, 0.5), g);
        b.add(tri(new Vec3(0, 4.6, -9.6), new Vec3(0, 9.0, -12.6), new Vec3(0, 1.0, -11.6), 0.5), g);
        b.sym(tri(new Vec3(2.0, 2.6, 3.0), new Vec3(5.6, 0.4, 0.0), new Vec3(2.0, 2.6, 0.4), 0.45), g);
        b.paintSym(sphere(1.6, 4.4, 6.4, 0.35), BLACK);
        return b.build();
    }

    static Sculpt octopus() {
        Sculpt.Builder b = Sculpt.builder(16);
        String r = "minecraft:red_terracotta";
        b.add(ellipsoid(0, 10.0, -1.0, 4.2, 5.4, 4.6), r);
        b.add(ellipsoid(0, 6.0, 0.6, 3.6, 2.6, 3.6), r);
        eyesWhite(b, 2.0, 6.8, 3.6, 1.0, BLACK);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45 + 22.5);
            double cx = Math.cos(a), cz = Math.sin(a);
            b.add(tube(1.1, 0.35, new Vec3(cx * 2.0, 4.6, cz * 2.0 + 0.6), new Vec3(cx * 5.0, 2.0, cz * 5.0 + 0.6),
                    new Vec3(cx * 8.0, 0.8, cz * 8.0 + 0.6), new Vec3(cx * 10.0, 2.4, cz * 9.4 + 0.6)), r);
        }
        b.paint(boxC(0, 0.6, 0, 12, 0.8, 12), "minecraft:pink_terracotta");
        return b.build();
    }

    static Sculpt seaTurtle() {
        Sculpt.Builder b = Sculpt.builder(5);
        String shell = "minecraft:brown_terracotta";
        String skin = "minecraft:lime_terracotta";
        b.add(ellipsoid(0, 2.6, 0, 4.6, 2.0, 5.6), Mat.checker(1.6, Mat.of(shell), Mat.of(DBROWN)));
        b.add(ellipsoid(0, 1.6, 0, 4.2, 1.0, 5.2), "minecraft:yellow_terracotta");
        b.add(limb(0, 2.2, 5.0, 0, 2.6, 7.4, 1.2, 1.1), skin);
        b.add(sphere(0, 2.8, 7.8, 1.6), skin);
        b.paintSym(sphere(0.9, 3.2, 9.0, 0.35), BLACK);
        b.sym(tri(new Vec3(3.6, 2.0, 3.0), new Vec3(9.6, 1.6, 4.4), new Vec3(4.6, 1.8, 0.4), 0.45), skin);
        b.sym(tri(new Vec3(3.0, 1.8, -3.6), new Vec3(6.0, 1.4, -6.6), new Vec3(2.0, 1.6, -5.0), 0.45), skin);
        return b.build();
    }

    /** Point d'accès commun : yeux génériques des animaux sculptés à la main. */
    static void simpleEyes(Sculpt.Builder b, double x, double y, double z, double r) {
        eyes(b, x, y, z, r, BLACK);
    }
}
