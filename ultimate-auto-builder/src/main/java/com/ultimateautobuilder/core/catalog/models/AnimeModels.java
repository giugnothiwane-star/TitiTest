package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.generator.HairStyles;
import com.ultimateautobuilder.core.generator.Humanoid;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Shape;
import com.ultimateautobuilder.core.geom.Shapes;
import com.ultimateautobuilder.core.geom.Vec3;

import static com.ultimateautobuilder.core.generator.Humanoid.BLACK;
import static com.ultimateautobuilder.core.generator.Humanoid.SKIN;
import static com.ultimateautobuilder.core.generator.Humanoid.WHITE;
import static com.ultimateautobuilder.core.generator.Humanoid.elbow;
import static com.ultimateautobuilder.core.generator.Humanoid.hand;
import static com.ultimateautobuilder.core.generator.Humanoid.shoulder;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.torusY;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** Personnages de style anime (35 modèles), construits sur le gabarit {@link Humanoid}. */
public final class AnimeModels {
    private static final String ORANGE = "minecraft:orange_wool";
    private static final String BLUE = "minecraft:blue_wool";
    private static final String BLUE_C = "minecraft:blue_concrete";
    private static final String NAVY = "minecraft:blue_terracotta";
    private static final String GOLD_HAIR = "minecraft:yellow_concrete";
    private static final String GRAY = "minecraft:gray_wool";
    private static final String BROWN = "minecraft:brown_concrete";
    private static final String RED = "minecraft:red_wool";
    private static final String IRON = "minecraft:iron_block";

    private AnimeModels() {
    }

    public static void register(Catalog catalog) {
        Reg r = new Reg(catalog, Category.ANIME);
        var S = Reg.CHARACTER;
        r.sculpt("goku", "Son Goku", 4, S, () -> Humanoid.build(goku(BLACK, HairStyles.SAIYAN, BLACK)), "dragon ball", "dbz", "saiyan", "kakarot");
        r.sculpt("goku_ssj", "Goku Super Saiyan", 4, S, () -> Humanoid.build(goku(GOLD_HAIR, HairStyles.SUPER_SAIYAN, "minecraft:cyan_concrete")), "dragon ball", "dbz", "ssj", "super saiyan");
        r.sculpt("goku_ui", "Goku Ultra Instinct", 4, S, () -> Humanoid.build(goku("minecraft:light_gray_concrete", HairStyles.UPSWEPT, "minecraft:light_gray_concrete")
                .top(BLUE).sleeve(BLUE)), "dragon ball", "ultra instinct", "migatte");
        r.sculpt("vegeta", "Vegeta", 4, S, () -> Humanoid.build(vegeta(BLACK, BLACK)), "dragon ball", "dbz", "prince", "saiyan");
        r.sculpt("vegeta_blue", "Vegeta Super Saiyan Blue", 4, S, () -> Humanoid.build(vegeta("minecraft:light_blue_concrete", "minecraft:light_blue_concrete")), "dragon ball", "ssb", "blue", "saiyan");
        r.sculpt("gohan", "Gohan", 4, S, AnimeModels::gohan, "dragon ball", "dbz", "saiyan");
        r.sculpt("trunks", "Trunks", 4, S, AnimeModels::trunks, "dragon ball", "dbz", "epee", "futur");
        r.sculpt("piccolo", "Piccolo", 4, S, AnimeModels::piccolo, "dragon ball", "namek", "namekien");
        r.sculpt("freezer", "Freezer", 4, S, AnimeModels::freezer, "dragon ball", "frieza", "freeza", "mechant");
        r.sculpt("cell", "Cell", 4, S, AnimeModels::cell, "dragon ball", "androide", "mechant");
        r.sculpt("majin_buu", "Majin Buu", 4, S, AnimeModels::buu, "dragon ball", "boo", "majin");
        r.sculpt("naruto", "Naruto", 4, S, () -> Humanoid.build(naruto()), "naruto", "ninja", "uzumaki", "konoha");
        r.sculpt("naruto_sage", "Naruto mode ermite", 4, S, AnimeModels::narutoSage, "naruto", "ninja", "sage", "ermite");
        r.sculpt("sasuke", "Sasuke", 4, S, AnimeModels::sasuke, "naruto", "ninja", "uchiha", "sharingan");
        r.sculpt("kakashi", "Kakashi", 4, S, AnimeModels::kakashi, "naruto", "ninja", "hatake", "sensei");
        r.sculpt("itachi", "Itachi", 4, S, AnimeModels::itachi, "naruto", "ninja", "uchiha", "akatsuki");
        r.sculpt("luffy", "Luffy", 4, S, AnimeModels::luffy, "one piece", "pirate", "chapeau de paille", "monkey d");
        r.sculpt("zoro", "Zoro", 4, S, AnimeModels::zoro, "one piece", "pirate", "sabreur", "roronoa");
        r.sculpt("sanji", "Sanji", 4, S, AnimeModels::sanji, "one piece", "pirate", "cuisinier");
        r.sculpt("nami", "Nami", 4, S, AnimeModels::nami, "one piece", "pirate", "navigatrice");
        r.sculpt("chopper", "Chopper", 4, S, AnimeModels::chopper, "one piece", "renne", "medecin");
        r.sculpt("law", "Trafalgar Law", 4, S, AnimeModels::law, "one piece", "pirate", "chirurgien");
        r.sculpt("shanks", "Shanks", 4, S, AnimeModels::shanks, "one piece", "pirate", "le roux", "empereur");
        r.sculpt("ichigo", "Ichigo", 4, S, AnimeModels::ichigo, "bleach", "shinigami", "zangetsu");
        r.sculpt("rukia", "Rukia", 4, S, AnimeModels::rukia, "bleach", "shinigami");
        r.sculpt("saitama", "Saitama", 3, S, AnimeModels::saitama, "one punch man", "opm", "heros", "chauve");
        r.sculpt("genos", "Genos", 4, S, AnimeModels::genos, "one punch man", "opm", "cyborg");
        r.sculpt("tanjiro", "Tanjiro", 4, S, AnimeModels::tanjiro, "demon slayer", "kimetsu", "pourfendeur");
        r.sculpt("nezuko", "Nezuko", 4, S, AnimeModels::nezuko, "demon slayer", "kimetsu", "demon");
        r.sculpt("zenitsu", "Zenitsu", 4, S, AnimeModels::zenitsu, "demon slayer", "kimetsu", "foudre");
        r.sculpt("inosuke", "Inosuke", 4, S, AnimeModels::inosuke, "demon slayer", "kimetsu", "sanglier");
        r.sculpt("gojo", "Satoru Gojo", 4, S, AnimeModels::gojo, "jujutsu kaisen", "jjk", "sorcier");
        r.sculpt("yuji", "Yuji Itadori", 4, S, AnimeModels::yuji, "jujutsu kaisen", "jjk", "sukuna");
        r.sculpt("eren", "Eren Jäger", 4, S, AnimeModels::eren, "attaque des titans", "snk", "aot", "bataillon");
        r.sculpt("levi", "Livaï (Levi)", 4, S, AnimeModels::levi, "attaque des titans", "snk", "aot", "caporal");
    }

    // ------------------------------------------------------------------ accessoires communs

    static void wristbands(Sculpt.Builder b, String mat, double k) {
        Vec3 el = elbow(k), ha = hand(k);
        b.sym(limb(el.lerp(ha, 0.62), el.lerp(ha, 0.92), 1.42 * k, 1.32 * k), mat);
    }

    static void boots(Sculpt.Builder b, String mat, double top, double k) {
        b.sym(limb(2.05 * k, 1.0, 0.2, 2.05 * k, top, 0.05, 1.95 * k, 1.95 * k), mat);
        b.sym(ellipsoid(2.15 * k, 1.1, 0.8, 1.9 * k, 1.35, 2.7), mat);
    }

    static void vNeck(Sculpt.Builder b, String mat, double depth) {
        b.paint(tri(new Vec3(-2.3, 21.6, 2.6), new Vec3(2.3, 21.6, 2.6), new Vec3(0, 21.6 - depth, 2.6), 0.9), mat);
    }

    static void cape(Sculpt.Builder b, String mat, double bottom) {
        Vec3 a = new Vec3(-5.0, 21.6, -2.6), c = new Vec3(5.0, 21.6, -2.6);
        Vec3 d = new Vec3(-6.8, bottom, -4.4), e = new Vec3(6.8, bottom, -4.4);
        b.fill(tri(a, c, d, 0.5), mat);
        b.fill(tri(c, e, d, 0.5), mat);
    }

    static void shoulderPads(Sculpt.Builder b, String mat, double k) {
        b.sym(ellipsoid(4.7 * k, 21.4, 0, 2.0 * k, 1.1, 2.6 * k), mat);
    }

    static void headband(Sculpt.Builder b, String cloth, String plate) {
        b.add(torusY(0, 28.5, -0.1, 3.9, 0.55), cloth);
        b.add(boxC(0, 28.5, 3.85, 1.5, 0.85, 0.45), plate);
    }

    /** Épée portée en diagonale dans le dos (poignée au-dessus de l'épaule droite). */
    static void backSword(Sculpt.Builder b, String handle, String blade, double length) {
        Vec3 top = new Vec3(-4.2, 24.5, -3.0);
        Vec3 guard = new Vec3(-2.6, 21.5, -3.2);
        Vec3 tip = guard.add(new Vec3(1, -1.9, 0).normalize().mul(length));
        b.add(limb(top, guard, 0.45, 0.45), handle);
        b.add(limb(guard, tip, 0.5, 0.35), blade);
        b.add(sphere(guard.x(), guard.y(), guard.z(), 0.8), "minecraft:gold_block");
    }

    /** Sabre à la hanche gauche du personnage (côté +X). */
    static void hipSword(Sculpt.Builder b, String handle, String sheath, double z) {
        b.add(limb(3.0, 15.4, 2.6 + z, 4.6, 12.6, 0.8 + z, 0.45, 0.45), handle);
        b.add(limb(4.6, 12.6, 0.8 + z, 6.4, 6.0, -4.0 + z, 0.5, 0.45), sheath);
    }

    static void paintTorsoAndSleeves(Sculpt.Builder b, Mat mat, double k, double bottom) {
        Shape torso = boxC(0, (22.6 + bottom) / 2, 0, 5.2 * k, (22.6 - bottom) / 2, 3.2 * k);
        b.paint(torso, mat);
        b.paint(limb(shoulder(k), elbow(k), 1.7 * k, 1.6 * k), mat);
        b.paint(limb(shoulder(k), elbow(k), 1.7 * k, 1.6 * k).mirrorX(), mat);
    }

    /** Robe ou manteau long tombant des épaules jusqu'à {@code bottom}. */
    static void robe(Sculpt.Builder b, Mat mat, double bottom, double flare) {
        b.add(frustum(new Vec3(0, 16.5, 0), new Vec3(0, bottom, 0), 3.9, flare), mat);
    }

    // ------------------------------------------------------------------ Dragon Ball

    static Humanoid.Spec goku(String hair, Humanoid.Hair style, String eyes) {
        return Humanoid.spec().hair(hair).style(style).eyes(eyes).top(ORANGE).sleeve(ORANGE).pants(ORANGE)
                .belt(BLUE).shoes(BLUE_C).bulk(1.08)
                .extra(b -> {
                    vNeck(b, BLUE, 3.2);
                    wristbands(b, BLUE, 1.08);
                    boots(b, BLUE_C, 5.2, 1.08);
                    b.paint(sphere(2.3, 19.6, 2.6, 0.9), WHITE);
                });
    }

    static Humanoid.Spec vegeta(String hair, String eyes) {
        return Humanoid.spec().hair(hair).style(HairStyles.VEGETA).eyes(eyes).top(BLUE_C).sleeve(BLUE_C).forearm(WHITE)
                .hands(WHITE).pants(BLUE_C).shoes(WHITE).bulk(1.05)
                .extra(b -> {
                    b.add(ellipsoid(0, 19.4, 0, 4.9, 3.1, 2.85), WHITE);
                    b.add(boxC(0, 15.9, 0, 3.85, 0.9, 2.45), "minecraft:yellow_concrete");
                    shoulderPads(b, "minecraft:yellow_concrete", 1.05);
                    boots(b, WHITE, 5.5, 1.05);
                });
    }

    static Sculpt gohan() {
        return Humanoid.build(Humanoid.spec().hair(BLACK).style(HairStyles.MESSY).top("minecraft:purple_wool")
                .sleeve("minecraft:purple_wool").pants("minecraft:purple_wool").belt(RED).shoes(BROWN).bulk(1.05)
                .extra(b -> {
                    wristbands(b, "minecraft:light_blue_wool", 1.05);
                    boots(b, BROWN, 4.5, 1.05);
                    b.add(limb(0.3, 31.0, 2.4, 0.9, 27.6, 4.3, 0.8, 0.2), BLACK);
                }));
    }

    static Sculpt trunks() {
        return Humanoid.build(Humanoid.spec().hair("minecraft:purpur_block").style(HairStyles.BOWL).eyes(BLUE_C)
                .top(BLUE_C).sleeve(BLUE_C).pants(GRAY).shoes("minecraft:yellow_concrete").belt(BROWN)
                .extra(b -> {
                    b.paint(boxC(0, 18.5, 2.6, 1.6, 3.6, 0.8), BLACK);
                    boots(b, "minecraft:yellow_concrete", 4.0, 1.0);
                    backSword(b, BROWN, IRON, 15);
                }));
    }

    static Sculpt piccolo() {
        String green = "minecraft:lime_terracotta";
        return Humanoid.build(Humanoid.spec().skin(green).hair(green).style(HairStyles.BALD).noEyebrows()
                .top("minecraft:purple_wool").sleeve("minecraft:purple_wool").pants("minecraft:purple_wool")
                .belt("minecraft:light_blue_wool").shoes(BROWN).bulk(1.1).legScale(1.05)
                .extra(b -> {
                    b.paint(limb(elbow(1.1), hand(1.1), 1.4, 1.3), "minecraft:pink_terracotta");
                    b.paint(limb(elbow(1.1), hand(1.1), 1.4, 1.3).mirrorX(), "minecraft:pink_terracotta");
                    wristbands(b, RED, 1.1);
                    cape(b, WHITE, 4.0);
                    shoulderPads(b, WHITE, 1.15);
                    b.add(Humanoid.cap(1.0, 1.4), WHITE);
                    b.add(torusY(0, 28.3, 0, 4.2, 0.6), "minecraft:purple_wool");
                    b.sym(frustum(new Vec3(3.7, 26.5, 0), new Vec3(6.2, 28.2, -0.8), 0.8, 0.1), green);
                    b.sym(limb(0.8, 28.0, 3.8, 1.3, 31.5, 4.8, 0.35, 0.3), green);
                }));
    }

    static Sculpt freezer() {
        String purple = "minecraft:purple_concrete";
        return Humanoid.build(Humanoid.spec().skin(WHITE).hair(purple).style(HairStyles.BALD).noEyebrows()
                .eyes("minecraft:red_concrete").top(WHITE).pants(WHITE).shoes(WHITE).mouth(purple).bulk(0.95)
                .extra(b -> {
                    b.add(Humanoid.cap(0.4, -1.2), purple);
                    shoulderPads(b, purple, 0.95);
                    b.paint(ellipsoid(0, 19.2, 2.6, 2.4, 1.6, 0.9), purple);
                    b.paint(limb(elbow(0.95), hand(0.95), 1.4, 1.3).and(boxC(6, 14.5, 1, 2, 1.2, 3)), purple);
                    b.paint(limb(elbow(0.95), hand(0.95), 1.4, 1.3).and(boxC(6, 14.5, 1, 2, 1.2, 3)).mirrorX(), purple);
                    b.add(limb(0, 13.2, -2.0, 0, 7.5, -6.5, 1.3, 1.1), WHITE);
                    b.add(limb(0, 7.5, -6.5, 3.5, 2.0, -8.5, 1.1, 0.8), WHITE);
                    b.add(limb(3.5, 2.0, -8.5, 6.5, 1.0, -5.5, 0.8, 0.5), WHITE);
                }));
    }

    static Sculpt cell() {
        Mat spots = Mat.spots(77, 1.1, 0.64, Mat.of("minecraft:lime_concrete"), Mat.of(BLACK));
        String green = "minecraft:lime_concrete";
        return Humanoid.build(Humanoid.spec().skin(SKIN).hair(BLACK).style(HairStyles.BALD).noEyebrows()
                .eyes("minecraft:magenta_concrete").top(green).sleeve(green).forearm(green).hands(green).pants(green)
                .shoes(WHITE).bulk(1.05)
                .extra(b -> {
                    b.paint(boxC(0, 11.5, 0, 9, 10.8, 6), spots);
                    b.add(ellipsoid(0, 19.4, 0, 4.7, 2.6, 2.6), BLACK);
                    b.add(Humanoid.cap(0.5, -1), green);
                    b.sym(frustum(new Vec3(2.0, 29.6, -0.5), new Vec3(4.6, 34.0, -1.6), 1.2, 0.2), BLACK);
                    b.sym(tri(new Vec3(1.2, 21, -2.6), new Vec3(7.5, 25, -4.5), new Vec3(3.2, 12, -3.4), 0.45), BLACK);
                    b.paintSym(ellipsoid(2.5, 25.4, 3.3, 0.8, 0.7, 0.9), "minecraft:purple_concrete");
                }));
    }

    static Sculpt buu() {
        String pink = "minecraft:pink_concrete";
        return Humanoid.build(Humanoid.spec().skin(pink).hair(pink).style(HairStyles.BALD).noEyebrows()
                .top(BLACK).sleeve(pink).pants(WHITE).shoes("minecraft:yellow_concrete").belt("minecraft:yellow_concrete")
                .hands(WHITE).bulk(1.45)
                .extra(b -> {
                    b.add(ellipsoid(0, 15.8, 0.6, 5.8, 5.0, 4.4), pink);
                    b.paint(ellipsoid(0, 19.6, 0, 6.0, 2.4, 4.6).minus(boxC(0, 18, 4, 2.5, 4, 3)), BLACK);
                    b.add(boxC(0, 12.6, 0.3, 5.4, 0.8, 4.2), "minecraft:yellow_concrete");
                    b.sym(ellipsoid(3.2, 7.0, 0, 2.8, 4.8, 2.8), WHITE);
                    cape(b, "minecraft:purple_wool", 6);
                    b.add(limb(0, 30.5, -0.5, 0, 33.0, -2.0, 1.0, 0.9), pink);
                    b.add(limb(0, 33.0, -2.0, 0, 31.5, -5.0, 0.9, 0.5), pink);
                }));
    }

    // ------------------------------------------------------------------ Naruto

    static Humanoid.Spec naruto() {
        return Humanoid.spec().hair(GOLD_HAIR).style(HairStyles.NARUTO).eyes("minecraft:light_blue_concrete")
                .top(ORANGE).sleeve(ORANGE).pants(ORANGE).shoes(BLUE_C)
                .extra(b -> {
                    b.paint(boxC(0, 21.6, 0, 6.5, 1.3, 3.5), BLACK);
                    headband(b, BLACK, IRON);
                    for (int i = 0; i < 3; i++) {
                        b.paintSym(boxC(2.9, 24.9 + i * 0.7, 3.2, 0.9, 0.12, 0.9), BLACK);
                    }
                });
    }

    static Sculpt narutoSage() {
        Humanoid.Spec s = naruto();
        s.extra(b -> {
            robe(b, Mat.of(RED), 4.5, 5.6);
            b.add(frustum(new Vec3(0, 6.5, 0), new Vec3(0, 4.2, 0), 5.0, 5.6), BLACK);
            b.paintSym(ellipsoid(1.7, 27.0, 3.7, 1.5, 1.5, 0.9), "minecraft:orange_concrete");
            Humanoid.face(b, "minecraft:yellow_concrete", "minecraft:brown_terracotta", null);
        });
        return Humanoid.build(s);
    }

    static Sculpt sasuke() {
        return Humanoid.build(Humanoid.spec().hair(BLACK).style(HairStyles.SASUKE).eyes("minecraft:red_concrete")
                .top("minecraft:white_wool").sleeve("minecraft:white_wool").pants(NAVY).shoes(BLACK)
                .extra(b -> {
                    vNeck(b, SKIN, 5.0);
                    b.add(torusY(0, 14.6, 0, 3.9, 0.9), "minecraft:purple_wool");
                    backSword(b, BLACK, IRON, 13);
                }));
    }

    static Sculpt kakashi() {
        return Humanoid.build(Humanoid.spec().hair("minecraft:light_gray_concrete").style(HairStyles.TILTED)
                .top("minecraft:green_wool").sleeve(NAVY).forearm(NAVY).pants(NAVY).shoes(BLACK)
                .extra(b -> {
                    b.paint(boxC(0, 24.3, 1.8, 4.6, 1.75, 3.0), NAVY);
                    b.add(torusY(0, 28.5, -0.1, 3.9, 0.55).rotated(0, 0, -8, new Vec3(0, 28.5, 0)), NAVY);
                    b.add(boxC(1.5, 27.5, 3.85, 1.6, 1.1, 0.45), IRON);
                    b.paint(boxC(0, 17.5, 2.6, 0.7, 3.5, 0.8), "minecraft:lime_wool");
                }));
    }

    static Sculpt itachi() {
        Mat cloak = Mat.spots(31, 1.9, 0.74, Mat.of(BLACK), Mat.of("minecraft:red_concrete"));
        return Humanoid.build(Humanoid.spec().hair(BLACK).style(HairStyles.PONYTAIL).eyes("minecraft:red_concrete")
                .top(BLACK).sleeve(BLACK).forearm(BLACK).pants(BLACK).shoes(GRAY)
                .extra(b -> {
                    robe(b, cloak, 4.0, 6.0);
                    paintTorsoAndSleeves(b, cloak, 1.0, 13);
                    b.add(frustum(new Vec3(0, 21.0, 0), new Vec3(0, 24.0, 0), 3.4, 3.2).minus(boxC(0, 23, 3.5, 2.4, 3, 1.5)), BLACK);
                    headband(b, NAVY, IRON);
                    b.paint(boxC(0, 28.5, 4.3, 1.2, 0.12, 0.3), BLACK);
                }));
    }

    // ------------------------------------------------------------------ One Piece

    static Sculpt luffy() {
        return Humanoid.build(Humanoid.spec().hair(BLACK).style(HairStyles.MESSY).top(RED).sleeve(SKIN).pants(BLUE)
                .shoes("minecraft:brown_terracotta").belt("minecraft:yellow_wool")
                .extra(b -> {
                    b.paint(boxC(0, 18.3, 2.6, 1.2, 3.8, 0.8), SKIN);
                    b.paintSym(boxC(2.1, 4.7, 0, 2.4, 2.4, 2.6), SKIN);
                    b.add(cylinderY(0, 30.0, -0.2, 0.7, 6.6), "minecraft:hay_block");
                    b.add(cylinderY(0, 30.0, -0.2, 3.0, 4.15), "minecraft:hay_block");
                    b.add(cylinderY(0, 30.6, -0.2, 0.9, 4.3), "minecraft:red_concrete");
                    b.paint(boxC(-1.5, 25.6, 3.7, 0.7, 0.12, 0.8), "minecraft:red_terracotta");
                }));
    }

    static Sculpt zoro() {
        return Humanoid.build(Humanoid.spec().hair("minecraft:green_wool").style(HairStyles.SHORT).top("minecraft:white_wool")
                .sleeve("minecraft:white_wool").pants(BLACK).shoes(BLACK).bulk(1.08)
                .extra(b -> {
                    b.add(boxC(0, 15.0, 0, 4.1, 1.5, 2.6), "minecraft:green_concrete");
                    boots(b, BLACK, 5.0, 1.08);
                    hipSword(b, BLACK, WHITE, 0);
                    hipSword(b, "minecraft:red_concrete", BLACK, 1.0);
                    hipSword(b, "minecraft:white_concrete", "minecraft:red_concrete", -1.0);
                    b.add(sphere(4.2, 25.4, 0.4, 0.45), "minecraft:gold_block");
                    b.paint(limb(shoulder(1.08), elbow(1.08), 1.6, 1.5).and(boxC(-6, 18.8, 0, 2, 0.8, 3)).mirrorX(), BLACK);
                }));
    }

    static Sculpt sanji() {
        return Humanoid.build(Humanoid.spec().hair(GOLD_HAIR).style(HairStyles.SWEPT_EYE).eyes(BLUE_C).top(BLACK).sleeve(BLACK)
                .forearm(BLACK).pants(BLACK).shoes(BLACK).bulk(0.95).legScale(1.06)
                .extra(b -> {
                    vNeck(b, "minecraft:light_blue_wool", 4.2);
                    b.paint(boxC(0, 19.6, 2.6, 0.35, 1.8, 0.8), BLACK);
                    b.add(limb(0.8, 24.6, 3.8, 2.4, 24.3, 5.2, 0.25, 0.25), WHITE);
                }));
    }

    static Sculpt nami() {
        Mat stripes = Mat.stripesY(1.6, 0.8, Mat.of("minecraft:white_wool"), Mat.of(BLUE));
        return Humanoid.build(Humanoid.spec().hair("minecraft:orange_concrete").style(HairStyles.LONG).eyes(BROWN)
                .top("minecraft:white_wool").sleeve(SKIN).pants(BLUE).shoes(BROWN).bulk(0.88)
                .extra(b -> {
                    paintTorsoAndSleeves(b, stripes, 0.88, 15);
                    b.paint(limb(shoulder(0.88), elbow(0.88), 1.7, 1.6).mirrorX(), SKIN);
                    b.paint(limb(shoulder(0.88), elbow(0.88), 1.7, 1.6), SKIN);
                    b.paint(sphere(4.6, 19.5, 0.8, 0.8), BLUE_C);
                    b.add(limb(5.4, 10.0, 1.2, 5.8, 24.0, 1.6, 0.4, 0.4), "minecraft:light_blue_concrete");
                }));
    }

    static Sculpt chopper() {
        String fur = "minecraft:brown_wool";
        return Humanoid.build(Humanoid.spec().skin(fur).hair(fur).style(HairStyles.BALD).noEyebrows().top(fur).sleeve(fur)
                .pants(BLUE).shoes("minecraft:brown_terracotta").legScale(0.55).bulk(1.1)
                .extra(b -> {
                    b.paint(ellipsoid(0, 25.6, 3.0, 3.3, 2.6, 1.5), "minecraft:white_terracotta");
                    Humanoid.face(b, BLACK, BLACK, null);
                    b.add(sphere(0, 25.8, 4.2, 0.75), BLUE_C);
                    b.add(cylinderY(0, 29.6, 0, 0.6, 5.5), "minecraft:pink_concrete");
                    b.add(cylinderY(0, 29.6, 0, 4.0, 4.0), "minecraft:pink_concrete");
                    b.paint(boxC(0, 31.6, 4.0, 1.4, 0.3, 0.6).rotated(0, 0, 45, new Vec3(0, 31.6, 4)), WHITE);
                    b.paint(boxC(0, 31.6, 4.0, 1.4, 0.3, 0.6).rotated(0, 0, -45, new Vec3(0, 31.6, 4)), WHITE);
                    b.sym(limb(3.4, 31.5, 0, 6.6, 34.5, 0, 0.55, 0.45), "minecraft:brown_terracotta");
                    b.sym(limb(5.4, 33.3, 0, 5.6, 36.0, 0, 0.45, 0.35), "minecraft:brown_terracotta");
                    b.sym(limb(6.6, 34.5, 0, 8.6, 35.6, 0, 0.45, 0.35), "minecraft:brown_terracotta");
                }));
    }

    static Sculpt law() {
        Mat spotted = Mat.spots(13, 1.2, 0.7, Mat.of(BLUE), Mat.of(BLACK));
        return Humanoid.build(Humanoid.spec().hair(BLACK).style(HairStyles.SHORT).eyes(GRAY).top("minecraft:yellow_concrete")
                .sleeve(BLACK).forearm(BLACK).pants(BLUE).shoes(BLACK)
                .extra(b -> {
                    b.paint(boxC(0, 8.0, 0, 7, 7, 4), spotted);
                    b.add(Humanoid.cap(1.4, 1.0), WHITE);
                    b.paint(Humanoid.cap(1.4, 1.0), Mat.spots(5, 1.0, 0.72, Mat.of(WHITE), Mat.of(BROWN)));
                    b.add(cylinderY(0, 28.6, 0.4, 0.6, 5.0), WHITE);
                    b.paint(ellipsoid(0, 18.6, 2.6, 1.5, 1.5, 0.9), BLACK);
                    b.paint(boxC(0, 23.6, 3.4, 0.8, 0.5, 0.9), BLACK);
                    b.add(limb(6.6, 7.0, 1.6, 6.9, 33.0, 1.0, 0.5, 0.5), BLACK);
                    b.add(sphere(6.7, 13.4, 1.4, 0.8), "minecraft:gold_block");
                }));
    }

    static Sculpt shanks() {
        return Humanoid.build(Humanoid.spec().hair("minecraft:red_concrete").style(HairStyles.SHOULDER).top("minecraft:white_wool")
                .sleeve("minecraft:white_wool").pants(BROWN).shoes(BLACK).belt("minecraft:green_wool").bulk(1.08)
                .extra(b -> {
                    vNeck(b, SKIN, 5.5);
                    cape(b, BLACK, 3.0);
                    shoulderPads(b, BLACK, 1.15);
                    for (int i = 0; i < 3; i++) {
                        b.paint(boxC(1.2 + i * 0.6, 27.0, 3.9, 0.12, 1.6, 0.6), "minecraft:red_terracotta");
                    }
                }));
    }

    // ------------------------------------------------------------------ Bleach

    static Sculpt ichigo() {
        return Humanoid.build(Humanoid.spec().hair("minecraft:orange_concrete").style(HairStyles.UPSWEPT).eyes(BROWN)
                .top(BLACK).sleeve(BLACK).forearm(BLACK).pants(BLACK).belt(WHITE).shoes(WHITE).bulk(1.05)
                .extra(b -> {
                    vNeck(b, WHITE, 3.0);
                    b.sym(ellipsoid(2.6, 8.0, 0, 2.7, 6.0, 2.7), BLACK);
                    b.add(boxC(0, 15.5, -3.4, 1.7, 9.5, 0.35).rotated(0, 0, 32, new Vec3(0, 15.5, -3.4)), IRON);
                    b.add(limb(-5.6, 25.5, -3.4, -4.0, 23.5, -3.4, 0.5, 0.5), WHITE);
                }));
    }

    static Sculpt rukia() {
        return Humanoid.build(Humanoid.spec().hair(BLACK).style(HairStyles.BOB).eyes("minecraft:purple_concrete").top(BLACK)
                .sleeve(BLACK).forearm(BLACK).pants(BLACK).belt(WHITE).shoes(WHITE).bulk(0.85).legScale(0.92)
                .extra(b -> {
                    vNeck(b, WHITE, 2.6);
                    b.sym(ellipsoid(2.3, 8.0, 0, 2.4, 5.8, 2.4), BLACK);
                    hipSword(b, WHITE, WHITE, 0);
                }));
    }

    // ------------------------------------------------------------------ One Punch Man

    static Sculpt saitama() {
        String yellow = "minecraft:yellow_concrete";
        String red = "minecraft:red_concrete";
        return Humanoid.build(Humanoid.spec().hair(SKIN).style(HairStyles.BALD).noEyebrows().top(yellow).sleeve(yellow)
                .forearm(red).hands(red).pants(yellow).belt(BLACK).shoes(red)
                .extra(b -> {
                    boots(b, red, 6.0, 1.0);
                    cape(b, WHITE, 5.0);
                    b.paint(boxC(0, 18.0, 2.6, 0.2, 3.6, 0.8), "minecraft:light_gray_concrete");
                    b.add(sphere(0, 31.0, 0, 0.6), SKIN);
                }));
    }

    static Sculpt genos() {
        return Humanoid.build(Humanoid.spec().hair(GOLD_HAIR).style(HairStyles.SHORT).eyes("minecraft:yellow_concrete")
                .top(BLACK).sleeve(IRON).forearm(BLACK).hands(IRON).pants(BLACK).shoes(BLACK)
                .extra(b -> {
                    b.paint(sphere(0, 19.4, 2.6, 1.1), "minecraft:orange_concrete");
                    b.add(sphere(shoulder(1).x(), shoulder(1).y(), 0, 1.8), "minecraft:gray_concrete");
                    b.add(sphere(-shoulder(1).x(), shoulder(1).y(), 0, 1.8), "minecraft:gray_concrete");
                }));
    }

    // ------------------------------------------------------------------ Demon Slayer

    static Sculpt tanjiro() {
        Mat check = Mat.checker(1.4, Mat.of("minecraft:green_concrete"), Mat.of(BLACK));
        return Humanoid.build(Humanoid.spec().hair("minecraft:red_terracotta").style(HairStyles.MESSY).eyes("minecraft:red_terracotta")
                .top(BLACK).sleeve(BLACK).pants(BLACK).belt(WHITE).shoes(WHITE)
                .extra(b -> {
                    robe(b, check, 8.5, 5.2);
                    paintTorsoAndSleeves(b, check, 1.0, 13);
                    b.paint(boxC(0, 18.5, 2.7, 1.0, 3.4, 0.6), BLACK);
                    b.sym(boxC(4.4, 25.0, 0, 0.25, 1.0, 0.6), WHITE);
                    b.paint(boxC(-1.6, 29.0, 3.4, 0.7, 0.5, 0.8), "minecraft:red_terracotta");
                    hipSword(b, BLACK, BLACK, 0);
                }));
    }

    static Sculpt nezuko() {
        String pink = "minecraft:pink_wool";
        return Humanoid.build(Humanoid.spec().hair(BLACK).style(HairStyles.LONG).eyes("minecraft:pink_concrete")
                .top(pink).sleeve(pink).forearm(pink).pants(pink).shoes(BROWN).belt("minecraft:brown_terracotta").bulk(0.85)
                .extra(b -> {
                    robe(b, Mat.of(pink), 3.5, 5.0);
                    b.paint(boxC(0, 17.0, 0, 6, 3.0, 5), "minecraft:orange_concrete");
                    b.paint(boxC(0, 15.0, 0, 6, 1.0, 5), "minecraft:brown_terracotta");
                    b.add(boxC(0, 24.6, 3.7, 2.9, 0.45, 0.6), "minecraft:bamboo_block[axis=x]");
                    b.sym(ellipsoid(1.3, 27.0, -4.4, 1.1, 0.8, 0.6), "minecraft:pink_concrete");
                }));
    }

    static Sculpt zenitsu() {
        Mat haori = Mat.spots(9, 1.1, 0.7, Mat.of("minecraft:yellow_concrete"), Mat.of(WHITE));
        return Humanoid.build(Humanoid.spec().hair(GOLD_HAIR).style(HairStyles.UPSWEPT).eyes(BROWN).top(BLACK).sleeve(BLACK)
                .pants(BLACK).belt(WHITE).shoes(WHITE).bulk(0.95)
                .extra(b -> {
                    robe(b, haori, 8.5, 5.0);
                    paintTorsoAndSleeves(b, haori, 0.95, 13);
                    b.paint(boxC(0, 18.5, 2.7, 1.0, 3.4, 0.6), BLACK);
                    b.paint(boxC(0, 33, 0, 8, 2.2, 8), "minecraft:orange_concrete");
                    hipSword(b, "minecraft:yellow_concrete", BLACK, 0);
                }));
    }

    static Sculpt inosuke() {
        String fur = "minecraft:light_gray_wool";
        Mat pelt = Mat.spots(19, 1.0, 0.62, Mat.of("minecraft:brown_wool"), Mat.of(BLACK));
        return Humanoid.build(Humanoid.spec().top(SKIN).sleeve(SKIN).pants("minecraft:brown_wool").shoes(GRAY)
                .style(HairStyles.BALD).noEyebrows().bulk(1.1)
                .extra(b -> {
                    b.paint(boxC(0, 8.0, 0, 7, 7.5, 4), pelt);
                    b.add(ellipsoid(0, 27.2, -0.2, 4.6, 4.8, 4.6), fur);
                    b.add(frustum(new Vec3(0, 26.0, 3.2), new Vec3(0, 25.2, 6.6), 2.4, 1.8), fur);
                    b.add(sphere(0, 25.2, 6.6, 1.6), "minecraft:pink_terracotta");
                    b.paintSym(sphere(0.6, 25.3, 7.8, 0.4), BLACK);
                    b.paintSym(ellipsoid(2.0, 28.0, 4.0, 0.9, 0.9, 0.9), "minecraft:light_blue_concrete");
                    b.sym(frustum(new Vec3(3.2, 30.0, 0), new Vec3(4.4, 32.8, -0.6), 1.0, 0.15), fur);
                    b.sym(limb(6.4, 11.2, 1.4, 8.6, 20.0, 5.0, 0.55, 0.4), IRON);
                    b.paint(boxC(0, 16.4, 0, 6, 0.7, 4), "minecraft:brown_terracotta");
                }));
    }

    // ------------------------------------------------------------------ Jujutsu Kaisen

    static Sculpt gojo() {
        return Humanoid.build(Humanoid.spec().hair("minecraft:white_concrete").style(HairStyles.UPSWEPT).eyes(BLACK)
                .top(BLACK).sleeve(BLACK).forearm(BLACK).pants(BLACK).shoes(BLACK).legScale(1.12).bulk(1.02)
                .extra(b -> {
                    b.paint(boxC(0, 27.0, 1.6, 4.6, 0.95, 3.2), BLACK);
                    b.add(frustum(new Vec3(0, 21.0, 0), new Vec3(0, 23.6, 0), 3.2, 2.9).minus(boxC(0, 23, 3.4, 1.2, 3, 1.5)), BLACK);
                }));
    }

    static Sculpt yuji() {
        return Humanoid.build(Humanoid.spec().hair("minecraft:pink_concrete").style(HairStyles.MESSY).eyes(BROWN).top(BLACK)
                .sleeve(BLACK).pants(BLACK).shoes(WHITE).bulk(1.05)
                .extra(b -> {
                    b.add(ellipsoid(0, 22.4, -2.6, 3.6, 1.6, 1.3), "minecraft:red_concrete");
                    b.paint(boxC(0, 25.0, -2, 6, 1.6, 3), BROWN);
                    b.paintSym(boxC(2.0, 25.9, 3.6, 0.6, 0.12, 0.8), "minecraft:red_terracotta");
                    b.paint(boxC(0, 18.0, 2.6, 0.25, 3.6, 0.8), "minecraft:gold_block");
                }));
    }

    // ------------------------------------------------------------------ L'Attaque des Titans

    static Humanoid.Spec scout(String hair, Humanoid.Hair style, String eyes, double bulk) {
        String jacket = "minecraft:brown_terracotta";
        return Humanoid.spec().hair(hair).style(style).eyes(eyes).top("minecraft:white_wool").sleeve(jacket).forearm(jacket)
                .pants("minecraft:white_wool").shoes(BROWN).belt(BLACK).bulk(bulk)
                .extra(b -> {
                    b.paint(boxC(0, 20.0, 0, 6, 1.6, 4), jacket);
                    b.paintSym(boxC(1.6, 18.0, 2.6, 0.25, 3.0, 0.8), BLACK);
                    boots(b, BROWN, 6.5, bulk);
                    cape(b, "minecraft:green_wool", 7.0);
                    b.paint(sphere(0, 15.5, -4.0, 1.4), WHITE);
                    b.paint(sphere(0.9, 15.5, -4.0, 0.8), BLUE_C);
                    b.sym(boxC(4.8, 13.0, -0.5, 0.8, 1.6, 1.8), IRON);
                });
    }

    static Sculpt eren() {
        return Humanoid.build(scout(BROWN, HairStyles.SHOULDER, "minecraft:cyan_concrete", 1.0));
    }

    static Sculpt levi() {
        Humanoid.Spec s = scout(BLACK, HairStyles.UNDERCUT, GRAY, 0.92).legScale(0.95);
        s.extra(b -> b.add(ellipsoid(0, 21.6, 2.4, 1.3, 1.2, 0.9), WHITE));
        return Humanoid.build(s);
    }

    /** Accès pour les autres catégories (statues humanoïdes). */
    static Shape unionOf(Shape... shapes) {
        return Shapes.union(shapes);
    }
}
