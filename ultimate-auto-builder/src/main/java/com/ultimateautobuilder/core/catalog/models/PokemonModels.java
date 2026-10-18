package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Shape;
import com.ultimateautobuilder.core.geom.Vec3;

import static com.ultimateautobuilder.core.generator.Creature.BLACK;
import static com.ultimateautobuilder.core.generator.Creature.CREAM;
import static com.ultimateautobuilder.core.generator.Creature.WHITE;
import static com.ultimateautobuilder.core.generator.Creature.eyes;
import static com.ultimateautobuilder.core.generator.Creature.eyesWhite;
import static com.ultimateautobuilder.core.generator.Creature.flame;
import static com.ultimateautobuilder.core.generator.Creature.horn;
import static com.ultimateautobuilder.core.generator.Creature.leg;
import static com.ultimateautobuilder.core.generator.Creature.tail;
import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.generator.Creature.wing;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.halfSpace;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.torusY;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** 40 Pokémon sculptés en voxels (silhouettes et couleurs caractéristiques). */
public final class PokemonModels {
    private static final String YELLOW = "minecraft:yellow_concrete";
    private static final String ORANGE = "minecraft:orange_concrete";
    private static final String RED = "minecraft:red_concrete";
    private static final String BROWN = "minecraft:brown_concrete";
    private static final String LBLUE = "minecraft:light_blue_concrete";
    private static final String BLUE = "minecraft:blue_concrete";
    private static final String CYAN = "minecraft:cyan_concrete";
    private static final String TEAL = "minecraft:oxidized_copper";
    private static final String GREEN = "minecraft:green_concrete";
    private static final String LIME = "minecraft:lime_concrete";
    private static final String PINK = "minecraft:pink_concrete";
    private static final String PURPLE = "minecraft:purple_concrete";
    private static final String GRAY = "minecraft:gray_concrete";
    private static final String LGRAY = "minecraft:light_gray_concrete";
    private static final String GOLD = "minecraft:gold_block";

    private PokemonModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.POKEMON);
        SizeSpec S = SizeSpec.height(8, 20, 40, 80);
        SizeSpec L = SizeSpec.height(10, 26, 50, 96);
        r.sculpt("pikachu", "Pikachu", 3, S, PokemonModels::pikachu, "pokemon", "electrique", "souris");
        r.sculpt("raichu", "Raichu", 3, S, PokemonModels::raichu, "pokemon", "electrique");
        r.sculpt("dracaufeu", "Dracaufeu", 5, L, PokemonModels::dracaufeu, "pokemon", "charizard", "feu", "dragon");
        r.sculpt("salameche", "Salamèche", 3, S, () -> fireLizard(ORANGE, false), "pokemon", "charmander", "feu");
        r.sculpt("reptincel", "Reptincel", 3, S, () -> fireLizard("minecraft:red_terracotta", true), "pokemon", "charmeleon", "feu");
        r.sculpt("bulbizarre", "Bulbizarre", 3, S, () -> bulba(0), "pokemon", "bulbasaur", "plante");
        r.sculpt("herbizarre", "Herbizarre", 4, S, () -> bulba(1), "pokemon", "ivysaur", "plante");
        r.sculpt("florizarre", "Florizarre", 5, L, () -> bulba(2), "pokemon", "venusaur", "plante");
        r.sculpt("carapuce", "Carapuce", 3, S, () -> turtle(0), "pokemon", "squirtle", "eau", "tortue");
        r.sculpt("carabaffe", "Carabaffe", 3, S, () -> turtle(1), "pokemon", "wartortle", "eau", "tortue");
        r.sculpt("tortank", "Tortank", 5, L, () -> turtle(2), "pokemon", "blastoise", "eau", "canons");
        r.sculpt("evoli", "Évoli", 3, S, () -> eevee(0), "pokemon", "eevee", "normal");
        r.sculpt("aquali", "Aquali", 3, S, () -> eevee(1), "pokemon", "vaporeon", "eau", "evolition");
        r.sculpt("voltali", "Voltali", 3, S, () -> eevee(2), "pokemon", "jolteon", "electrique", "evolition");
        r.sculpt("pyroli", "Pyroli", 3, S, () -> eevee(3), "pokemon", "flareon", "feu", "evolition");
        r.sculpt("mentali", "Mentali", 3, S, () -> eevee(4), "pokemon", "espeon", "psy", "evolition");
        r.sculpt("noctali", "Noctali", 3, S, () -> eevee(5), "pokemon", "umbreon", "tenebres", "evolition");
        r.sculpt("dracolosse", "Dracolosse", 4, L, PokemonModels::dracolosse, "pokemon", "dragonite", "dragon");
        r.sculpt("mew", "Mew", 3, S, PokemonModels::mew, "pokemon", "legendaire", "psy", "fabuleux");
        r.sculpt("mewtwo", "Mewtwo", 4, L, PokemonModels::mewtwo, "pokemon", "legendaire", "psy");
        r.sculpt("lucario", "Lucario", 4, L, PokemonModels::lucario, "pokemon", "aura", "combat");
        r.sculpt("gardevoir", "Gardevoir", 4, L, PokemonModels::gardevoir, "pokemon", "psy", "fee");
        r.sculpt("rayquaza", "Rayquaza", 5, SizeSpec.length(16, 40, 80, 150), PokemonModels::rayquaza, "pokemon", "legendaire", "dragon", "serpent");
        r.sculpt("ectoplasma", "Ectoplasma (Gengar)", 3, S, PokemonModels::gengar, "pokemon", "gengar", "spectre", "fantome");
        r.sculpt("ronflex", "Ronflex", 3, L, PokemonModels::ronflex, "pokemon", "snorlax", "dormeur");
        r.sculpt("tortipouss", "Tortipouss", 3, S, PokemonModels::tortipouss, "pokemon", "turtwig", "plante", "tortue");
        r.sculpt("tiplouf", "Tiplouf (Piplup)", 3, S, PokemonModels::tiplouf, "pokemon", "piplup", "eau", "pingouin");
        r.sculpt("ouisticram", "Ouisticram", 3, S, PokemonModels::ouisticram, "pokemon", "chimchar", "feu", "singe");
        r.sculpt("grenousse", "Grenousse", 3, S, PokemonModels::grenousse, "pokemon", "froakie", "eau", "grenouille");
        r.sculpt("amphinobi", "Amphinobi", 4, L, PokemonModels::amphinobi, "pokemon", "greninja", "eau", "ninja");
        r.sculpt("electhor", "Électhor", 4, L, () -> bird(0), "pokemon", "zapdos", "legendaire", "oiseau");
        r.sculpt("sulfura", "Sulfura", 4, L, () -> bird(1), "pokemon", "moltres", "legendaire", "oiseau", "feu");
        r.sculpt("artikodin", "Artikodin", 4, L, () -> bird(2), "pokemon", "articuno", "legendaire", "oiseau", "glace");
        r.sculpt("leviator", "Léviator", 5, SizeSpec.length(16, 40, 80, 150), PokemonModels::leviator, "pokemon", "gyarados", "serpent", "eau");
        r.sculpt("metalosse", "Métalosse", 4, L, PokemonModels::metalosse, "pokemon", "metagross", "acier");
        r.sculpt("tyranocif", "Tyranocif", 4, L, PokemonModels::tyranocif, "pokemon", "tyranitar", "roche");
        r.sculpt("absol", "Absol", 3, S, PokemonModels::absol, "pokemon", "tenebres");
        r.sculpt("carchacrok", "Carchacrok", 4, L, PokemonModels::carchacrok, "pokemon", "garchomp", "dragon", "requin");
        r.sculpt("arceus", "Arceus", 5, L, PokemonModels::arceus, "pokemon", "legendaire", "dieu");
        r.sculpt("rondoudou", "Rondoudou", 2, S, PokemonModels::rondoudou, "pokemon", "jigglypuff", "fee", "chanteur");
    }

    // ------------------------------------------------------------------ souris électriques

    static Sculpt pikachu() {
        Sculpt.Builder b = Sculpt.builder(24);
        b.add(ellipsoid(0, 6.6, 0, 4.2, 5.4, 3.7), YELLOW);
        b.add(ellipsoid(0, 14.0, 0.4, 4.7, 4.1, 3.9), YELLOW);
        b.sym(frustum(new Vec3(2.4, 16.8, 0), new Vec3(5.6, 24.0, -0.6), 1.35, 0.15), YELLOW);
        b.sym(frustum(new Vec3(4.8, 22.2, -0.45), new Vec3(5.6, 24.0, -0.6), 0.75, 0.1), BLACK);
        b.paintSym(sphere(3.3, 12.9, 3.1, 1.25), RED);
        eyes(b, 1.85, 14.9, 3.9, 0.9, BLACK);
        b.paint(sphere(0, 13.8, 4.4, 0.3), BLACK);
        b.paint(boxC(0, 12.8, 4.1, 0.8, 0.22, 0.6), "minecraft:brown_terracotta");
        b.sym(limb(3.4, 9.4, 1.4, 2.8, 8.0, 3.9, 0.95, 0.85), YELLOW);
        b.sym(ellipsoid(2.2, 0.9, 1.2, 1.5, 0.95, 2.1), YELLOW);
        b.paint(boxC(0, 8.5, -3.5, 3.0, 0.5, 1.0), BROWN);
        b.paint(boxC(0, 6.6, -3.6, 3.2, 0.5, 1.0), BROWN);
        b.add(boxC(0, 7.3, -5.0, 0.5, 2.2, 0.8).rotated(0, -35, 0, new Vec3(0, 7.3, -5.0)), BROWN);
        b.add(boxC(0, 10.0, -7.0, 0.55, 2.4, 1.0).rotated(0, 55, 0, new Vec3(0, 10.0, -7.0)), YELLOW);
        b.add(boxC(0, 13.6, -8.4, 0.6, 3.4, 2.0).rotated(0, -25, 0, new Vec3(0, 13.6, -8.4)), YELLOW);
        return b.build();
    }

    static Sculpt raichu() {
        Sculpt.Builder b = Sculpt.builder(25);
        String o = ORANGE;
        b.add(ellipsoid(0, 7.4, 0, 5.0, 6.2, 4.4), o);
        b.paint(ellipsoid(0, 7.0, 2.4, 3.8, 5.0, 2.4), CREAM);
        b.add(ellipsoid(0, 15.4, 0.4, 5.0, 4.2, 4.0), o);
        b.sym(ellipsoid(5.4, 19.6, -0.4, 1.6, 4.0, 0.9).rotated(0, 0, -32, new Vec3(5.4, 19.6, -0.4)), BROWN);
        b.paintSym(ellipsoid(5.9, 20.6, 0.4, 0.9, 2.6, 0.6).rotated(0, 0, -32, new Vec3(5.4, 19.6, -0.4)), YELLOW);
        b.paintSym(sphere(3.6, 14.2, 3.0, 1.2), YELLOW);
        eyes(b, 2.0, 16.2, 3.8, 0.95, BLACK);
        b.paint(boxC(0, 14.0, 4.0, 1.0, 0.25, 0.6), "minecraft:brown_terracotta");
        b.sym(limb(4.3, 10.4, 1.4, 3.6, 8.6, 4.2, 1.05, 0.95), o);
        b.sym(ellipsoid(2.6, 1.0, 1.0, 1.7, 1.0, 2.4), BROWN);
        tail(b, BLACK, 0.6, 0.45, new Vec3(0, 4, -4), new Vec3(0, 3, -9), new Vec3(0, 6, -13), new Vec3(0, 12, -14));
        b.add(boxC(0, 13.6, -14.2, 0.5, 2.2, 1.6).rotated(0, -20, 0, new Vec3(0, 13.6, -14.2)), YELLOW);
        return b.build();
    }

    // ------------------------------------------------------------------ lignée de Salamèche

    static Sculpt fireLizard(String skin, boolean evolved) {
        Sculpt.Builder b = Sculpt.builder(evolved ? 24 : 20);
        double k = evolved ? 1.15 : 1.0;
        b.add(ellipsoid(0, 6.5 * k, 0, 4.0 * k, 5.2 * k, 3.6 * k), skin);
        b.paint(ellipsoid(0, 6.0 * k, 2.2 * k, 3.0 * k, 4.4 * k, 2.0 * k), CREAM);
        b.add(ellipsoid(0, 13.6 * k, 0.8, 4.0 * k, 3.6 * k, 3.8 * k), skin);
        b.add(ellipsoid(0, 12.9 * k, 3.4 * k, 3.0 * k, 2.4 * k, 2.4 * k), skin);
        eyesWhite(b, 1.9 * k, 14.3 * k, 4.0 * k, 0.95, evolved ? "minecraft:cyan_concrete" : BLACK);
        b.paint(boxC(0, 12.2 * k, 5.6 * k, 1.6, 0.25, 0.5), "minecraft:brown_terracotta");
        if (evolved) {
            b.add(horn(0, 16.0 * k, -1.5, 0, 17.6 * k, -5.5, 1.2), skin);
        }
        b.sym(limb(3.6 * k, 9.4 * k, 1.0, 4.4 * k, 7.0 * k, 3.6 * k, 1.0, 0.9), skin);
        b.sym(ellipsoid(2.3 * k, 1.0, 1.0, 1.6 * k, 1.1, 2.3 * k), skin);
        b.sym(horn(2.5 * k, 0.8, 3.0 * k, 2.5 * k, 0.6, 4.4 * k, 0.5), WHITE);
        tail(b, skin, 1.6 * k, 0.8, new Vec3(0, 4 * k, -3 * k), new Vec3(0, 3 * k, -7 * k), new Vec3(0, 5.5 * k, -10 * k));
        flame(b, 0, 8.0 * k, -10.4 * k, 1.1 * k);
        return b.build();
    }

    static Sculpt dracaufeu() {
        Sculpt.Builder b = Sculpt.builder(30);
        String o = ORANGE;
        b.add(ellipsoid(0, 11.0, 0, 5.2, 7.2, 4.6), o);
        b.paint(ellipsoid(0, 10.4, 2.4, 4.0, 6.2, 2.8), CREAM);
        b.sym(ellipsoid(3.4, 5.0, 0.6, 2.5, 3.6, 2.7), o);
        b.sym(ellipsoid(3.7, 1.0, 2.0, 1.9, 1.1, 2.7), o);
        b.sym(horn(3.7, 0.8, 4.2, 3.7, 0.6, 5.6, 0.5), WHITE);
        b.add(limb(0, 16.0, 0.6, 0, 21.0, 1.6, 2.6, 2.0), o);
        b.add(ellipsoid(0, 23.4, 2.4, 3.1, 2.8, 3.4), o);
        b.add(ellipsoid(0, 22.6, 5.4, 2.2, 1.7, 2.5), o);
        b.sym(horn(1.6, 25.6, 0.8, 2.6, 27.6, -2.6, 0.75), o);
        eyesWhite(b, 2.0, 23.9, 4.5, 0.85, "minecraft:cyan_concrete");
        b.paint(boxC(0, 22.0, 7.6, 1.8, 0.25, 0.5), BLACK);
        b.sym(limb(4.6, 15.0, 1.5, 5.8, 11.4, 4.2, 1.2, 1.0), o);
        wing(b, new Vec3(3.0, 18.0, -2.6), new Vec3(16.0, 27.0, -5.0), new Vec3(15.0, 13.0, -6.5), new Vec3(5.0, 11.0, -4.0),
                "minecraft:cyan_terracotta", o);
        tail(b, o, 2.2, 1.0, new Vec3(0, 8, -4), new Vec3(0, 5, -9), new Vec3(0, 6, -13), new Vec3(0, 9, -15.5));
        flame(b, 0, 11.6, -15.8, 1.4);
        return b.build();
    }

    // ------------------------------------------------------------------ lignée de Bulbizarre

    static Sculpt bulba(int stage) {
        double k = 1 + stage * 0.35;
        Sculpt.Builder b = Sculpt.builder(stage == 0 ? 14 : stage == 1 ? 18 : 26);
        String spots = "minecraft:cyan_terracotta";
        Mat skin = Mat.spots(41, 1.4 * k, 0.7, Mat.of(TEAL), Mat.of(spots));
        b.add(ellipsoid(0, 5.0 * k, -0.5, 4.6 * k, 3.6 * k, 5.2 * k), skin);
        b.add(ellipsoid(0, 7.0 * k, 4.6 * k, 4.1 * k, 3.2 * k, 3.3 * k), skin);
        b.add(ellipsoid(0, 6.3 * k, 6.6 * k, 3.0 * k, 2.1 * k, 1.9 * k), skin);
        b.sym(horn(2.6 * k, 9.2 * k, 3.6 * k, 3.9 * k, 11.2 * k, 3.2 * k, 1.1 * k), skin);
        eyesWhite(b, 2.3 * k, 7.6 * k, 7.2 * k, 0.95 * k, RED);
        b.paint(boxC(0, 5.6 * k, 8.4 * k, 1.8 * k, 0.25, 0.6), BLACK);
        leg(b, 3.0 * k, 3.0 * k, 3.6 * k, 1.5 * k, TEAL, TEAL);
        leg(b, 3.0 * k, -3.6 * k, 3.6 * k, 1.5 * k, TEAL, TEAL);
        if (stage == 0) {
            b.add(ellipsoid(0, 9.6, -1.4, 3.6, 3.4, 3.6), GREEN);
            b.add(horn(0, 12.0, -1.4, 0, 14.0, -1.8, 1.4), GREEN);
            b.paint(ellipsoid(1.4, 10.8, 0.6, 1.0, 1.6, 1.0), LIME);
        } else if (stage == 1) {
            b.add(ellipsoid(0, 13.5, -1.8, 3.4, 3.8, 3.4), PINK);
            b.add(horn(0, 15.6, -1.8, 0, 19.0, -1.8, 2.2), PINK);
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(i * 90 + 45);
                b.add(ellipsoid(Math.cos(a) * 4.6, 11.2, -1.8 + Math.sin(a) * 4.6, 2.6, 0.5, 1.6)
                        .rotated(-Math.toDegrees(a), 0, 0, new Vec3(Math.cos(a) * 4.6, 11.2, -1.8 + Math.sin(a) * 4.6)), GREEN);
            }
        } else {
            b.add(cylinderY(0, 10.0, -1.8, 4.0, 2.6), "minecraft:brown_terracotta");
            for (int i = 0; i < 5; i++) {
                double a = Math.toRadians(i * 72 + 18);
                Vec3 c = new Vec3(Math.cos(a) * 6.0, 14.4, -1.8 + Math.sin(a) * 6.0);
                b.add(ellipsoid(c.x(), c.y(), c.z(), 4.0, 0.7, 2.4).rotated(-Math.toDegrees(a), 0, 0, c), "minecraft:pink_terracotta");
                b.paint(sphere(c.x() * 1.15, c.y() + 0.5, -1.8 + (c.z() + 1.8) * 1.15, 0.9), "minecraft:white_terracotta");
            }
            b.add(sphere(0, 15.0, -1.8, 2.4), "minecraft:yellow_terracotta");
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(i * 90);
                Vec3 c = new Vec3(Math.cos(a) * 5.0, 11.2, -1.8 + Math.sin(a) * 5.0);
                b.add(ellipsoid(c.x(), c.y(), c.z(), 4.2, 0.6, 2.0).rotated(-Math.toDegrees(a), 0, 12, c), GREEN);
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------ lignée de Carapuce

    static Sculpt turtle(int stage) {
        double k = 1 + stage * 0.32;
        Sculpt.Builder b = Sculpt.builder(stage == 2 ? 26 : stage == 1 ? 20 : 17);
        String skin = stage == 0 ? LBLUE : stage == 1 ? "minecraft:light_blue_terracotta" : BLUE;
        b.add(ellipsoid(0, 6.4 * k, 0, 4.2 * k, 5.2 * k, 3.6 * k), skin);
        b.add(ellipsoid(0, 6.4 * k, -1.2 * k, 4.6 * k, 5.4 * k, 3.4 * k), "minecraft:brown_terracotta");
        b.add(ellipsoid(0, 6.4 * k, 1.4 * k, 3.6 * k, 4.8 * k, 2.6 * k), "minecraft:yellow_terracotta");
        b.add(torusY(0, 6.4 * k, -0.2, 4.4 * k, 0.6 * k).rotated(0, 90, 0, new Vec3(0, 6.4 * k, -0.2)), WHITE);
        b.add(ellipsoid(0, 13.6 * k, 0.8, 3.8 * k, 3.6 * k, 3.6 * k), skin);
        eyesWhite(b, 1.8 * k, 14.2 * k, 4.0 * k, 0.95 * k, stage == 2 ? BROWN : "minecraft:red_terracotta");
        b.paint(boxC(0, 12.4 * k, 4.2 * k, 1.4, 0.25, 0.5), BLACK);
        b.sym(limb(3.8 * k, 9.4 * k, 0.8, 4.8 * k, 7.0 * k, 3.2 * k, 1.1 * k, 1.0 * k), skin);
        b.sym(ellipsoid(2.3 * k, 1.0, 0.8, 1.6 * k, 1.1, 2.3 * k), skin);
        if (stage == 0) {
            tail(b, skin, 1.2, 0.8, new Vec3(0, 3.4, -3.4), new Vec3(0, 3.0, -6.4), new Vec3(0, 5.6, -7.6), new Vec3(0, 6.4, -6.0));
        } else if (stage == 1) {
            b.sym(ellipsoid(3.4 * k, 18.4 * k, -0.6, 1.1, 2.8, 1.6).rotated(0, -20, -25, new Vec3(3.4 * k, 18.4 * k, -0.6)), WHITE);
            b.add(ellipsoid(0, 5.0 * k, -7.5 * k, 2.4 * k, 3.0 * k, 3.0 * k), WHITE);
        } else {
            Vec3 base = new Vec3(3.4 * k, 12.8 * k, -2.4 * k);
            Shape cannon = frustum(base, base.add(new Vec3(0.4, 1.4, 5.6 * k)), 1.2 * k, 1.1 * k);
            b.sym(cannon, "minecraft:iron_block");
            b.sym(frustum(base.add(new Vec3(0.4, 1.4, 5.6 * k)), base.add(new Vec3(0.45, 1.6, 6.4 * k)), 0.7 * k, 0.7 * k), BLACK);
            b.sym(horn(2.6 * k, 16.4 * k, 1.0, 3.6 * k, 17.6 * k, -0.6, 0.8), skin);
        }
        return b.build();
    }

    // ------------------------------------------------------------------ Évoli et ses évolutions

    static Sculpt eevee(int form) {
        String[] body = {BROWN, LBLUE, YELLOW, ORANGE, "minecraft:purpur_block", BLACK};
        String[] collar = {CREAM, WHITE, WHITE, YELLOW, "minecraft:purpur_block", BLACK};
        String skin = body[form];
        Sculpt.Builder b = Sculpt.builder(16);
        b.add(ellipsoid(0, 6.0, -0.6, 3.0, 2.8, 4.6), skin);
        leg(b, 1.7, 2.4, 4.2, 0.95, skin, skin);
        leg(b, 1.9, -3.4, 4.2, 1.05, skin, skin);
        b.add(ellipsoid(0, 11.0, 3.6, 3.4, 3.2, 3.2), skin);
        b.add(ellipsoid(0, 10.2, 6.0, 1.8, 1.4, 1.6), skin);
        eyesWhite(b, 1.5, 11.3, 6.1, 0.85, form == 5 ? RED : BROWN);
        b.paint(sphere(0, 10.4, 7.6, 0.3), BLACK);
        // Oreilles
        double earLen = form == 1 ? 3.0 : 6.0;
        b.sym(frustum(new Vec3(1.8, 13.0, 3.0), new Vec3(4.6, 13.0 + earLen, 1.6), 1.4, 0.2), skin);
        if (form == 5) {
            b.paintSym(torusY(3.6, 16.4, 2.0, 0.9, 0.35).rotated(0, 90, 0, new Vec3(3.6, 16.4, 2.0)), YELLOW);
        }
        if (form == 1) {
            b.sym(ellipsoid(3.6, 13.6, 2.6, 0.6, 2.4, 2.0), WHITE);
        }
        switch (form) {
            case 0 -> {
                b.add(torusY(0, 9.4, 2.4, 2.6, 1.6), collar[0]);
                tail(b, skin, 1.8, 2.4, new Vec3(0, 6.6, -4.6), new Vec3(0, 9.6, -7.2), new Vec3(0, 12.6, -7.0));
                b.add(ellipsoid(0, 13.6, -6.6, 2.0, 1.6, 2.0), collar[0]);
            }
            case 1 -> {
                b.add(torusY(0, 9.4, 2.4, 2.6, 1.3), WHITE);
                tail(b, skin, 1.6, 0.6, new Vec3(0, 6.0, -4.6), new Vec3(0, 4.0, -8.0), new Vec3(0, 3.0, -11.0));
                b.add(tri(new Vec3(0, 3.0, -11), new Vec3(3.4, 2.4, -14.0), new Vec3(-3.4, 2.4, -14.0), 0.5), "minecraft:blue_terracotta");
                b.add(tri(new Vec3(0, 8.0, -1), new Vec3(0, 11.0, -3.0), new Vec3(0, 8.0, -5.0), 0.5), "minecraft:blue_terracotta");
            }
            case 2 -> {
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    b.add(horn(Math.cos(a) * 1.5, 9.6 + Math.sin(a) * 1.5, 2.4, Math.cos(a) * 4.4, 9.6 + Math.sin(a) * 4.0, 1.0, 0.9), WHITE);
                }
                for (int i = 0; i < 5; i++) {
                    b.add(horn(0, 6.4 + i * 0.4, -3.8 - i * 0.3, (i - 2) * 1.2, 9.0 + i * 0.7, -7.0 - i * 0.4, 1.0), skin);
                }
            }
            case 3 -> {
                b.add(ellipsoid(0, 10.0, 2.0, 3.6, 3.4, 2.8), YELLOW);
                b.add(ellipsoid(0, 13.6, 3.0, 1.6, 1.4, 1.4), YELLOW);
                tail(b, YELLOW, 2.2, 3.0, new Vec3(0, 6.6, -4.6), new Vec3(0, 9.6, -7.6), new Vec3(0, 13.0, -8.0));
            }
            case 4 -> {
                b.paint(sphere(0, 13.4, 6.0, 0.6), RED);
                tail(b, skin, 0.8, 0.6, new Vec3(0, 6.6, -4.6), new Vec3(0, 10.4, -7.0));
                b.add(limb(0, 10.4, -7.0, 1.6, 13.0, -6.6, 0.6, 0.4), skin);
                b.add(limb(0, 10.4, -7.0, -1.6, 13.0, -6.6, 0.6, 0.4), skin);
            }
            default -> {
                b.paint(torusY(0, 6.0, -0.6, 2.8, 0.7).rotated(0, 0, 90, new Vec3(0, 6.0, -0.6)).translated(0, 0, -2.0), YELLOW);
                b.paint(sphere(0, 13.6, 4.8, 0.7), YELLOW);
                tail(b, skin, 1.0, 0.8, new Vec3(0, 6.6, -4.6), new Vec3(0, 9.6, -7.6), new Vec3(0, 11.6, -7.0));
                b.paint(torusY(0, 9.4, -7.4, 1.0, 0.5), YELLOW);
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------ dragons, légendaires

    static Sculpt dracolosse() {
        Sculpt.Builder b = Sculpt.builder(28);
        String o = ORANGE;
        b.add(ellipsoid(0, 10.0, 0, 6.6, 8.6, 5.6), o);
        b.paint(ellipsoid(0, 9.4, 3.2, 5.0, 7.4, 2.8), CREAM);
        b.paint(ellipsoid(0, 9.4, 3.4, 5.0, 7.4, 2.8), Mat.stripesY(1.6, 0.4, Mat.of(CREAM), Mat.of("minecraft:yellow_terracotta")));
        b.add(limb(0, 17.0, 0.6, 0, 20.6, 1.4, 3.0, 2.4), o);
        b.add(ellipsoid(0, 23.0, 2.0, 3.6, 3.2, 3.6), o);
        b.add(ellipsoid(0, 22.0, 4.8, 2.6, 1.8, 2.2), o);
        eyes(b, 1.7, 23.8, 5.0, 0.8, BLACK);
        b.sym(limb(1.2, 26.0, 1.6, 2.8, 29.0, 3.6, 0.35, 0.3), o);
        b.sym(horn(1.2, 25.6, 0, 1.4, 27.0, -1.0, 0.5), o);
        b.sym(limb(5.8, 13.6, 1.2, 6.4, 10.4, 4.6, 1.4, 1.2), o);
        b.sym(ellipsoid(3.6, 1.4, 1.6, 2.4, 1.6, 3.0), o);
        wing(b, new Vec3(3.2, 16.4, -4.4), new Vec3(8.4, 21.0, -6.6), new Vec3(8.0, 15.6, -7.4), new Vec3(4.0, 13.0, -5.0),
                "minecraft:cyan_terracotta", o);
        tail(b, o, 2.8, 1.0, new Vec3(0, 4.0, -4.0), new Vec3(0, 2.0, -9.0), new Vec3(0, 2.0, -13.0));
        return b.build();
    }

    static Sculpt mew() {
        Sculpt.Builder b = Sculpt.builder(18);
        String pk = "minecraft:pink_terracotta";
        b.add(ellipsoid(0, 7.4, 0, 2.6, 3.2, 2.4), pk);
        b.add(ellipsoid(0, 12.2, 0.6, 3.6, 3.2, 3.2), pk);
        b.sym(frustum(new Vec3(2.2, 14.6, 0.2), new Vec3(3.4, 17.4, 0), 1.2, 0.3), pk);
        eyes(b, 1.4, 12.4, 3.4, 1.0, "minecraft:light_blue_concrete");
        b.sym(limb(2.2, 9.0, 0.8, 3.2, 8.2, 2.4, 0.7, 0.6), pk);
        b.sym(limb(1.4, 5.0, 0.2, 2.6, 2.6, 1.6, 1.0, 0.8), pk);
        tail(b, pk, 0.55, 0.45, new Vec3(0, 5.4, -2.0), new Vec3(0, 3.0, -6.0), new Vec3(0, 6.0, -9.5), new Vec3(0, 12.0, -10.0),
                new Vec3(0, 15.4, -7.4));
        b.add(ellipsoid(0, 15.8, -7.0, 0.9, 1.4, 0.9), pk);
        return b.build();
    }

    static Sculpt mewtwo() {
        Sculpt.Builder b = Sculpt.builder(30);
        String g = "minecraft:light_gray_concrete";
        b.add(ellipsoid(0, 15.6, 0, 3.6, 3.6, 2.6), g);
        b.add(ellipsoid(0, 10.6, 0.2, 3.4, 3.4, 2.8), PURPLE);
        b.sym(ellipsoid(2.6, 7.6, 0, 2.6, 2.6, 2.6), g);
        b.sym(limb(2.6, 7.0, 0, 2.8, 1.6, 0.6, 1.6, 1.2), g);
        b.sym(ellipsoid(2.9, 0.9, 1.4, 1.4, 0.9, 2.0), g);
        b.add(limb(0, 18.6, 0, 0, 21.4, 0.6, 1.2, 1.1), g);
        b.add(ellipsoid(0, 23.4, 1.0, 2.8, 2.6, 2.8), g);
        b.sym(horn(1.4, 25.2, 0.6, 2.4, 27.6, -0.6, 0.9), g);
        eyesWhite(b, 1.2, 23.6, 3.4, 0.7, PURPLE);
        b.add(tube(1.0, 0.8, new Vec3(0, 23.0, -1.6), new Vec3(0, 20.6, -3.4), new Vec3(0, 18.0, -2.4)), g);
        b.sym(limb(4.0, 17.6, 0, 5.2, 13.2, 1.0, 1.0, 0.9), g);
        b.sym(limb(5.2, 13.2, 1.0, 5.4, 9.6, 2.6, 0.9, 0.8), g);
        b.sym(sphere(5.4, 9.2, 2.8, 1.0), g);
        tail(b, PURPLE, 2.0, 0.8, new Vec3(0, 7.0, -2.0), new Vec3(0, 3.0, -6.0), new Vec3(0, 3.6, -11.0), new Vec3(0, 8.0, -13.0));
        return b.build();
    }

    static Sculpt lucario() {
        Sculpt.Builder b = Sculpt.builder(26);
        String blue = "minecraft:blue_terracotta";
        String bl = LBLUE;
        b.add(ellipsoid(0, 12.6, 0, 3.4, 3.6, 2.6), CREAM);
        b.add(horn(0, 13.2, 2.4, 0, 13.4, 4.4, 0.8), WHITE);
        b.sym(limb(2.0, 9.6, 0, 2.4, 4.8, 0.6, 1.8, 1.5), BLACK);
        b.sym(limb(2.4, 4.8, 0.6, 2.4, 1.2, 0.2, 1.4, 1.1), bl);
        b.sym(ellipsoid(2.4, 0.8, 1.4, 1.2, 0.8, 1.8), bl);
        b.add(ellipsoid(0, 8.4, 0, 3.0, 2.4, 2.4), BLACK);
        b.sym(limb(3.8, 15.0, 0, 4.8, 11.6, 0.8, 1.1, 1.0), bl);
        b.sym(limb(4.8, 11.6, 0.8, 5.0, 8.8, 2.0, 1.0, 0.9), BLACK);
        b.sym(horn(5.0, 10.4, 1.4, 5.0, 10.6, 3.2, 0.6), WHITE);
        b.add(limb(0, 16.0, 0, 0, 18.0, 0.4, 1.4, 1.3), bl);
        b.add(ellipsoid(0, 20.4, 0.8, 2.8, 2.6, 2.6), bl);
        b.add(ellipsoid(0, 19.6, 3.2, 1.6, 1.2, 1.8), BLACK);
        b.paint(ellipsoid(0, 21.0, 2.4, 3.2, 0.9, 1.6), BLACK);
        eyesWhite(b, 1.2, 20.8, 3.0, 0.6, RED);
        b.sym(frustum(new Vec3(1.4, 22.4, 0.2), new Vec3(2.6, 26.0, -0.6), 1.1, 0.2), bl);
        b.sym(tube(0.6, 0.4, new Vec3(1.2, 19.6, -1.8), new Vec3(1.6, 17.0, -3.4), new Vec3(1.8, 14.6, -3.0)), BLACK);
        tail(b, bl, 1.4, 1.0, new Vec3(0, 8.0, -2.2), new Vec3(0, 6.0, -5.0), new Vec3(0, 5.6, -6.6));
        return b.build();
    }

    static Sculpt gardevoir() {
        Sculpt.Builder b = Sculpt.builder(28);
        String w = "minecraft:white_concrete";
        b.add(frustum(new Vec3(0, 13.0, 0), new Vec3(0, 0.5, 0), 2.0, 6.6), w);
        b.paint(frustum(new Vec3(0, 8.0, 0), new Vec3(0, 0.5, 0), 4.4, 6.8).and(halfSpace(new Vec3(0, 0, 0), new Vec3(0, 0, -1))), LIME);
        b.add(ellipsoid(0, 15.4, 0, 2.4, 3.0, 1.8), w);
        b.add(horn(0, 15.4, 1.2, 0, 13.6, 4.6, 1.0), RED);
        b.add(horn(0, 15.4, -1.2, 0, 13.6, -4.6, 0.9), RED);
        b.sym(limb(2.2, 17.4, 0, 3.8, 13.0, 1.4, 0.7, 0.6), w);
        b.sym(limb(3.8, 13.0, 1.4, 3.6, 9.6, 2.6, 0.6, 0.5), w);
        b.add(limb(0, 18.0, 0, 0, 20.0, 0.2, 0.9, 0.9), w);
        b.add(ellipsoid(0, 22.0, 0.4, 2.4, 2.4, 2.4), w);
        b.add(ellipsoid(0, 23.0, -0.4, 3.0, 2.6, 2.6), GREEN);
        b.sym(ellipsoid(2.6, 20.6, 0.6, 1.0, 3.2, 1.6), GREEN);
        b.add(ellipsoid(0, 23.6, 1.6, 2.6, 1.2, 1.6), GREEN);
        eyesWhite(b, 1.0, 22.0, 2.6, 0.55, RED);
        return b.build();
    }

    static Sculpt rayquaza() {
        Sculpt.Builder b = Sculpt.builder(40);
        String g = "minecraft:green_concrete";
        Vec3[] path = new Vec3[14];
        for (int i = 0; i < path.length; i++) {
            double t = i / (double) (path.length - 1);
            double a = t * Math.PI * 3.0;
            path[i] = new Vec3(Math.sin(a) * 7.0 * (1 - t * 0.3), 2.5 + t * 24, -Math.cos(a) * 7.0 * (1 - t * 0.3));
        }
        for (int i = 0; i + 1 < path.length; i++) {
            double r = 2.6 - i * 0.07;
            b.add(limb(path[i], path[i + 1], r, r), g);
            if (i % 2 == 0) {
                b.paint(sphere(path[i].x(), path[i].y(), path[i].z(), r + 0.2).and(boxC(path[i].x(), path[i].y(), path[i].z(), 9, 0.6, 9)),
                        YELLOW);
            }
            if (i % 3 == 1) {
                Vec3 m = path[i];
                b.add(tri(m, m.add(new Vec3(m.x() * 0.4, 1.0, m.z() * 0.4)), m.add(new Vec3(0, 3.6, 0)), 0.4), RED);
            }
        }
        b.add(limb(0, 0.4, 4.0, path[0].x(), path[0].y(), path[0].z(), 1.2, 2.4), g);
        Vec3 head = path[path.length - 1].add(new Vec3(0, 2.0, 1.0));
        b.add(ellipsoid(head.x(), head.y(), head.z(), 2.8, 2.4, 3.6), g);
        b.add(ellipsoid(head.x(), head.y() - 0.6, head.z() + 3.4, 2.0, 1.4, 2.4), g);
        b.paintSym(ellipsoid(head.x() + 1.4, head.y() + 0.6, head.z() + 2.0, 0.7, 0.6, 0.8), YELLOW);
        b.sym(horn(head.x() + 1.0, head.y() + 1.6, head.z() - 1.0, head.x() + 2.0, head.y() + 2.4, head.z() - 7.0, 0.9), g);
        b.sym(limb(head.x() + 1.6, head.y() + 0.4, head.z(), head.x() + 4.6, head.y() + 1.6, head.z() - 3.0, 0.5, 0.3), YELLOW);
        return b.build();
    }

    static Sculpt gengar() {
        Sculpt.Builder b = Sculpt.builder(18);
        String p = "minecraft:purple_terracotta";
        b.add(ellipsoid(0, 8.0, 0, 6.4, 6.4, 5.2), p);
        b.sym(frustum(new Vec3(3.4, 12.6, -0.6), new Vec3(5.6, 17.2, -1.4), 1.8, 0.2), p);
        for (int i = 0; i < 5; i++) {
            b.add(horn(0, 12.4 - i * 1.8, -4.0 - i * 0.1, 0, 14.4 - i * 1.6, -7.4 + i * 0.1, 1.2), p);
        }
        b.paintSym(ellipsoid(2.2, 10.4, 4.6, 1.4, 0.9, 1.0), RED);
        b.paintSym(ellipsoid(2.0, 10.2, 4.9, 0.6, 0.5, 0.8), WHITE);
        b.paint(ellipsoid(0, 6.4, 4.4, 4.2, 1.2, 1.6), WHITE);
        b.paint(boxC(0, 6.4, 5.4, 4.6, 0.12, 1.0), BLACK);
        b.sym(limb(5.4, 7.4, 1.0, 7.2, 5.4, 2.4, 1.2, 1.0), p);
        b.sym(ellipsoid(3.0, 1.2, 0.6, 2.0, 1.4, 2.4), p);
        tail(b, p, 1.0, 0.6, new Vec3(0, 3.0, -4.8), new Vec3(0, 3.6, -7.4));
        return b.build();
    }

    static Sculpt ronflex() {
        Sculpt.Builder b = Sculpt.builder(26);
        String body = "minecraft:cyan_terracotta";
        b.add(ellipsoid(0, 10.0, 0, 9.0, 10.0, 7.6), body);
        b.paint(ellipsoid(0, 9.0, 4.6, 7.4, 8.2, 3.6), CREAM);
        b.add(ellipsoid(0, 21.6, 0.6, 5.6, 4.2, 4.6), body);
        b.paint(ellipsoid(0, 20.6, 3.6, 4.6, 3.2, 2.0), CREAM);
        b.sym(frustum(new Vec3(3.8, 24.6, 0), new Vec3(4.6, 27.0, -0.4), 1.2, 0.3), body);
        b.paintSym(boxC(1.8, 21.6, 5.0, 1.2, 0.18, 0.8), BLACK);
        b.paint(boxC(0, 19.4, 5.0, 2.2, 0.25, 0.8), WHITE);
        b.sym(limb(8.0, 14.0, 0.6, 10.6, 10.0, 2.6, 2.2, 2.0), body);
        b.sym(ellipsoid(4.6, 1.6, 4.6, 2.8, 2.0, 2.4), CREAM);
        b.sym(ellipsoid(4.6, 1.8, 6.4, 1.6, 1.2, 0.6), "minecraft:brown_terracotta");
        return b.build();
    }

    // ------------------------------------------------------------------ Pokémon de départ récents

    static Sculpt tortipouss() {
        Sculpt.Builder b = Sculpt.builder(14);
        String g = LIME;
        b.add(ellipsoid(0, 4.4, -0.6, 3.6, 2.6, 4.2), "minecraft:yellow_terracotta");
        b.add(ellipsoid(0, 6.0, -1.4, 3.8, 2.0, 3.8), "minecraft:brown_terracotta");
        b.add(ellipsoid(0, 7.0, 4.0, 3.0, 2.8, 2.8), g);
        b.paint(ellipsoid(0, 5.6, 5.6, 2.4, 1.0, 1.4), YELLOW);
        b.paint(ellipsoid(0, 9.0, 4.4, 3.0, 1.0, 2.6), "minecraft:brown_terracotta");
        eyesWhite(b, 1.4, 7.6, 6.2, 0.7, BLACK);
        b.add(limb(0, 9.4, 3.6, 0, 11.4, 3.6, 0.3, 0.3), "minecraft:brown_terracotta");
        b.sym(ellipsoid(1.4, 12.0, 3.6, 1.4, 0.4, 0.8).rotated(0, 0, 25, new Vec3(1.4, 12, 3.6)), GREEN);
        leg(b, 2.4, 2.0, 2.6, 1.0, g, g);
        leg(b, 2.4, -3.0, 2.6, 1.0, g, g);
        return b.build();
    }

    static Sculpt tiplouf() {
        Sculpt.Builder b = Sculpt.builder(16);
        b.add(ellipsoid(0, 5.6, 0, 3.6, 4.6, 3.2), LBLUE);
        b.paint(ellipsoid(0, 5.4, 1.8, 2.6, 3.6, 1.8), WHITE);
        b.add(ellipsoid(0, 11.4, 0.4, 3.6, 3.2, 3.2), BLUE);
        b.paint(ellipsoid(0, 10.6, 2.6, 2.8, 2.0, 1.4), WHITE);
        b.sym(sphere(1.0, 11.6, 2.4, 1.2), WHITE);
        eyes(b, 1.2, 11.6, 3.4, 0.6, BLACK);
        b.add(ellipsoid(0, 10.2, 3.8, 1.2, 0.8, 1.4), YELLOW);
        b.add(horn(0, 13.0, 1.6, 0, 15.6, 1.0, 0.7), BLUE);
        b.sym(ellipsoid(4.0, 6.6, 0.4, 0.8, 3.0, 1.4).rotated(0, 0, 20, new Vec3(4, 6.6, 0.4)), BLUE);
        b.sym(ellipsoid(1.8, 0.6, 1.4, 1.4, 0.6, 2.0), "minecraft:orange_concrete");
        return b.build();
    }

    static Sculpt ouisticram() {
        Sculpt.Builder b = Sculpt.builder(17);
        String o = ORANGE;
        b.add(ellipsoid(0, 5.8, 0, 3.0, 3.8, 2.6), o);
        b.paint(ellipsoid(0, 5.6, 1.6, 2.2, 3.0, 1.4), CREAM);
        b.add(ellipsoid(0, 11.0, 0.4, 3.4, 3.0, 3.0), o);
        b.paint(ellipsoid(0, 10.4, 2.6, 2.6, 2.0, 1.2), CREAM);
        b.sym(ellipsoid(3.2, 11.0, 0, 1.2, 1.2, 0.6), CREAM);
        eyes(b, 1.2, 11.4, 3.2, 0.7, BLACK);
        b.paint(boxC(0, 9.6, 3.2, 1.2, 0.2, 0.6), BLACK);
        b.sym(limb(2.6, 8.0, 0.4, 3.6, 5.4, 1.8, 0.8, 0.7), o);
        b.sym(limb(1.6, 3.0, 0, 1.8, 0.8, 0.6, 1.0, 0.9), o);
        tail(b, o, 0.6, 0.5, new Vec3(0, 4.0, -2.4), new Vec3(0, 3.0, -5.6), new Vec3(0, 6.0, -6.6));
        flame(b, 0, 8.0, -6.6, 0.9);
        b.add(horn(0, 13.4, 0, 0, 15.6, -1.0, 1.0), o);
        return b.build();
    }

    static Sculpt grenousse() {
        Sculpt.Builder b = Sculpt.builder(13);
        String blue = "minecraft:light_blue_terracotta";
        b.add(ellipsoid(0, 4.4, -0.6, 3.0, 3.2, 3.2), blue);
        b.add(ellipsoid(0, 8.4, 0.6, 3.8, 2.6, 3.2), blue);
        b.paint(ellipsoid(0, 7.8, 2.6, 2.6, 1.6, 1.0), WHITE);
        b.add(torusY(0, 6.2, 0.4, 3.0, 1.2), WHITE);
        b.sym(ellipsoid(2.0, 10.6, 1.4, 1.6, 1.4, 1.4), blue);
        eyesWhite(b, 2.0, 10.8, 2.6, 0.75, YELLOW);
        b.sym(limb(2.4, 2.6, 0.4, 3.6, 0.8, 2.4, 1.0, 0.9), blue);
        b.sym(limb(2.6, 5.2, 1.2, 3.2, 3.4, 3.0, 0.7, 0.6), blue);
        return b.build();
    }

    static Sculpt amphinobi() {
        Sculpt.Builder b = Sculpt.builder(28);
        String blue = BLUE;
        b.add(ellipsoid(0, 15.0, 0, 3.0, 3.6, 2.2), blue);
        b.paint(ellipsoid(0, 13.0, 1.6, 2.2, 2.2, 1.0), "minecraft:light_blue_terracotta");
        b.sym(limb(1.8, 11.6, 0, 3.0, 6.0, -0.8, 1.4, 1.0), blue);
        b.sym(limb(3.0, 6.0, -0.8, 3.2, 1.0, 0.8, 1.0, 0.7), blue);
        b.sym(ellipsoid(3.2, 0.6, 1.8, 1.0, 0.6, 2.0), blue);
        b.sym(limb(3.0, 17.2, 0, 4.6, 12.6, 1.4, 0.8, 0.7), blue);
        b.sym(sphere(4.8, 12.0, 1.8, 0.9), WHITE);
        b.add(ellipsoid(0, 20.6, 0.6, 2.6, 2.4, 2.6), blue);
        b.paint(ellipsoid(0, 21.6, 2.0, 2.6, 0.9, 1.4), BLACK);
        b.sym(horn(1.4, 22.2, 0, 2.6, 25.6, -1.6, 0.8), blue);
        eyesWhite(b, 1.2, 21.4, 2.6, 0.6, RED);
        Shape scarf = tube(1.2, 0.5, new Vec3(-2.4, 18.6, 1.0), new Vec3(0, 18.4, 2.6), new Vec3(2.4, 18.6, 1.0),
                new Vec3(4.4, 17.0, -2.4), new Vec3(7.0, 14.0, -4.4));
        b.add(scarf, "minecraft:pink_concrete");
        b.add(torusY(0, 18.8, 0, 2.2, 1.0), "minecraft:pink_concrete");
        return b.build();
    }

    // ------------------------------------------------------------------ oiseaux légendaires

    static Sculpt bird(int kind) {
        String[] body = {YELLOW, "minecraft:yellow_terracotta", "minecraft:light_blue_terracotta"};
        String[] accent = {BLACK, ORANGE, LBLUE};
        String c = body[kind];
        Sculpt.Builder b = Sculpt.builder(26);
        b.add(ellipsoid(0, 12.0, 0, 3.6, 4.0, 4.6), c);
        b.add(limb(0, 15.0, 2.0, 0, 18.6, 3.6, 1.8, 1.4), c);
        b.add(ellipsoid(0, 20.2, 4.0, 2.0, 2.0, 2.4), c);
        b.add(horn(0, 19.8, 5.8, 0, 19.2, 8.8, 0.9), kind == 2 ? GRAY : "minecraft:orange_concrete");
        eyes(b, 1.2, 20.6, 5.6, 0.5, BLACK);
        b.sym(limb(1.6, 9.0, 0.6, 2.0, 2.0, 1.6, 0.7, 0.5), kind == 0 ? BLACK : "minecraft:orange_concrete");
        b.sym(ellipsoid(2.0, 0.8, 2.4, 1.0, 0.6, 1.8), kind == 0 ? BLACK : "minecraft:orange_concrete");
        Vec3 root = new Vec3(3.0, 14.6, -0.4);
        if (kind == 0) {
            for (int i = 0; i < 5; i++) {
                b.sym(tri(root, new Vec3(9 + i * 2.6, 22 - i * 3.0, -1.0 - i * 0.4), new Vec3(7 + i * 2.6, 17 - i * 2.4, -1.6 - i * 0.4), 0.5), c);
            }
            for (int i = 0; i < 5; i++) {
                b.add(horn(0, 21.4, 3.0 + i * 0.4, (i - 2) * 1.4, 25.0 - Math.abs(i - 2) * 0.6, 1.0 + i * 0.6, 0.8), c);
            }
            for (int i = 0; i < 3; i++) {
                b.add(tri(new Vec3(0, 10, -3.6), new Vec3((i - 1) * 3.0, 6.0, -10), new Vec3((i - 1) * 3.0 + 1.6, 9.0, -9), 0.5), c);
            }
        } else {
            b.sym(tri(root, new Vec3(17, 22, -2.6), new Vec3(14, 12, -4.0), 0.6), c);
            b.sym(tri(root, new Vec3(14, 12, -4.0), new Vec3(5, 10, -3), 0.6), c);
            if (kind == 1) {
                b.paintSym(tri(root, new Vec3(17, 22, -2.6), new Vec3(14, 12, -4.0), 0.7).and(halfSpace(new Vec3(10, 0, 0), new Vec3(-1, 0, 0))),
                        Mat.mix(3, 0.9, 0.35, Mat.of("minecraft:orange_concrete"), Mat.of(RED)));
                b.add(horn(0, 21.4, 3.4, 0, 25.4, 0.6, 1.4), "minecraft:orange_concrete");
                tail(b, "minecraft:orange_concrete", 1.6, 0.4, new Vec3(0, 11, -4), new Vec3(0, 9, -10), new Vec3(0, 11, -14));
            } else {
                b.add(horn(0, 21.6, 3.2, 0, 23.0, 0.0, 1.2), accent[2]);
                tail(b, accent[2], 1.2, 0.4, new Vec3(0, 11, -4), new Vec3(0, 6, -10), new Vec3(0, 2.6, -16), new Vec3(0, 1.0, -22));
                b.paintSym(ellipsoid(1.8, 14.0, 3.6, 1.2, 1.4, 1.2), WHITE);
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------ grands Pokémon

    static Sculpt leviator() {
        Sculpt.Builder b = Sculpt.builder(40);
        Vec3[] path = new Vec3[12];
        for (int i = 0; i < path.length; i++) {
            double t = i / (double) (path.length - 1);
            path[i] = new Vec3(Math.sin(t * Math.PI * 2.2) * 9 * (1 - t), 2.6 + t * 24, -Math.cos(t * Math.PI * 2.2) * 6 * (1 - t) - 4);
        }
        for (int i = 0; i + 1 < path.length; i++) {
            double r = 2.0 + i * 0.12;
            b.add(limb(path[i], path[i + 1], r, r + 0.12), BLUE);
            b.paint(limb(path[i], path[i + 1], r + 0.3, r + 0.4).and(halfSpace(path[i], new Vec3(0, 0, -1))), CREAM);
            b.add(tri(path[i], path[i].add(new Vec3(0, 2.0, -3.0)), path[i + 1], 0.35), "minecraft:light_blue_concrete");
        }
        Vec3 h = path[path.length - 1].add(new Vec3(0, 2.0, 2.0));
        b.add(ellipsoid(h.x(), h.y(), h.z(), 4.0, 3.4, 4.6), BLUE);
        b.add(ellipsoid(h.x(), h.y() - 2.4, h.z() + 3.0, 3.4, 1.4, 3.0), CREAM);
        b.carve(ellipsoid(h.x(), h.y() - 0.8, h.z() + 5.4, 2.6, 1.2, 2.0));
        for (int i = -1; i <= 1; i += 2) {
            b.add(horn(h.x() + i * 1.6, h.y() - 0.4, h.z() + 5.0, h.x() + i * 1.6, h.y() - 1.6, h.z() + 5.4, 0.45), WHITE);
        }
        b.sym(tri(new Vec3(h.x() + 2.4, h.y() + 1.6, h.z() - 1.0), new Vec3(h.x() + 6.6, h.y() + 4.6, h.z() - 3.0),
                new Vec3(h.x() + 4.0, h.y() + 0.6, h.z() - 3.4), 0.4), WHITE);
        b.add(horn(h.x(), h.y() + 2.6, h.z() - 0.6, h.x(), h.y() + 6.6, h.z() - 3.6, 1.2), BLUE);
        b.paintSym(ellipsoid(h.x() + 2.2, h.y() + 1.4, h.z() + 2.6, 1.0, 0.6, 1.0), RED);
        b.add(tri(path[0], path[0].add(new Vec3(4.6, -1.0, -3.0)), path[0].add(new Vec3(-4.6, -1.0, -3.0)), 0.4), WHITE);
        return b.build();
    }

    static Sculpt metalosse() {
        Sculpt.Builder b = Sculpt.builder(18);
        String m = "minecraft:light_blue_terracotta";
        b.add(ellipsoid(0, 11.0, 0, 6.0, 3.2, 6.0), m);
        b.paint(boxC(0, 11.0, 0, 7, 3.6, 1.2).rotated(45, 0, 0, new Vec3(0, 11, 0)), "minecraft:iron_block");
        b.paint(boxC(0, 11.0, 0, 7, 3.6, 1.2).rotated(-45, 0, 0, new Vec3(0, 11, 0)), "minecraft:iron_block");
        b.paintSym(ellipsoid(2.2, 11.6, 5.2, 1.2, 0.8, 1.0), RED);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(45 + i * 90);
            double cx = Math.cos(a) * 5.4, cz = Math.sin(a) * 5.4;
            double fx = Math.cos(a) * 9.6, fz = Math.sin(a) * 9.6;
            b.add(limb(cx, 11.0, cz, fx, 12.4, fz, 1.4, 1.2), m);
            b.add(limb(fx, 12.4, fz, fx * 1.05, 1.0, fz * 1.05, 1.2, 1.0), m);
            b.add(sphere(fx, 12.4, fz, 1.6), "minecraft:iron_block");
            b.add(horn(fx * 1.05, 1.2, fz * 1.05, fx * 1.25, 0.4, fz * 1.25, 0.6), "minecraft:iron_block");
        }
        return b.build();
    }

    static Sculpt tyranocif() {
        Sculpt.Builder b = Sculpt.builder(30);
        String g = "minecraft:green_terracotta";
        b.add(ellipsoid(0, 12.0, 0, 5.4, 7.4, 4.6), g);
        b.paint(ellipsoid(0, 11.0, 3.0, 3.6, 5.2, 2.0), "minecraft:light_blue_terracotta");
        b.paint(ellipsoid(0, 13.6, 3.6, 2.4, 1.8, 1.0), "minecraft:blue_terracotta");
        b.sym(ellipsoid(3.4, 5.4, 0.4, 2.6, 4.0, 2.8), g);
        b.sym(ellipsoid(3.6, 1.2, 1.6, 2.0, 1.2, 2.8), g);
        b.add(limb(0, 18.0, 0.6, 0, 21.0, 1.6, 2.6, 2.2), g);
        b.add(ellipsoid(0, 23.0, 2.6, 2.8, 2.6, 3.2), g);
        b.add(horn(0, 25.0, 2.6, 0, 29.0, 1.6, 1.0), g);
        eyesWhite(b, 1.4, 23.4, 5.0, 0.6, RED);
        b.paint(boxC(0, 21.8, 5.6, 1.6, 0.25, 0.6), BLACK);
        b.sym(limb(5.0, 17.6, 0.6, 6.2, 13.4, 3.0, 1.4, 1.2), g);
        for (int i = 0; i < 4; i++) {
            b.add(horn(0, 18.0 - i * 3.0, -3.8, 0, 19.6 - i * 3.0, -7.0, 1.2), g);
        }
        b.sym(horn(4.4, 18.0, -1.0, 6.0, 20.0, -3.0, 1.0), g);
        tail(b, g, 2.6, 1.0, new Vec3(0, 6.0, -4.0), new Vec3(0, 3.0, -9.0), new Vec3(0, 2.0, -13.0));
        return b.build();
    }

    static Sculpt absol() {
        Sculpt.Builder b = Sculpt.builder(18);
        String w = WHITE;
        String d = "minecraft:blue_terracotta";
        b.add(ellipsoid(0, 7.0, -0.6, 2.8, 2.8, 5.0), w);
        leg(b, 1.6, 3.0, 5.0, 0.9, w, d);
        leg(b, 1.8, -3.6, 5.0, 1.0, w, d);
        b.add(ellipsoid(0, 10.6, 4.0, 2.6, 3.0, 2.8), w);
        b.add(ellipsoid(0, 12.6, 5.6, 2.0, 1.8, 2.0), d);
        eyesWhite(b, 1.0, 12.8, 7.2, 0.5, RED);
        b.add(tri(new Vec3(1.4, 13.6, 5.0), new Vec3(4.6, 18.0, 2.4), new Vec3(3.6, 13.4, 3.0), 0.45), d);
        b.add(tri(new Vec3(0, 9.0, 1.0), new Vec3(0, 13.0, -2.6), new Vec3(0, 8.0, -2.0), 0.6), w);
        b.add(tri(new Vec3(0, 7.0, -5.0), new Vec3(0, 9.6, -12.0), new Vec3(0, 5.0, -9.0), 0.5), d);
        return b.build();
    }

    static Sculpt carchacrok() {
        Sculpt.Builder b = Sculpt.builder(28);
        String blue = BLUE;
        b.add(ellipsoid(0, 13.0, 0, 4.4, 6.6, 3.8), blue);
        b.paint(ellipsoid(0, 12.0, 2.8, 3.0, 4.6, 1.6), RED);
        b.paint(ellipsoid(0, 9.0, 3.0, 2.6, 3.0, 1.6), YELLOW);
        b.sym(ellipsoid(3.0, 6.0, 0.4, 2.4, 3.6, 2.6), blue);
        b.sym(ellipsoid(3.4, 1.0, 1.8, 1.8, 1.0, 2.6), blue);
        b.add(limb(0, 18.0, 0.6, 0, 20.6, 2.0, 2.0, 1.8), blue);
        b.add(ellipsoid(0, 22.6, 3.4, 2.4, 2.4, 4.6), blue);
        b.paint(ellipsoid(0, 21.4, 5.6, 2.0, 1.0, 2.6), RED);
        eyesWhite(b, 1.4, 23.4, 5.8, 0.55, YELLOW);
        b.sym(tri(new Vec3(1.4, 23.6, 1.4), new Vec3(5.6, 26.6, -1.6), new Vec3(2.0, 21.6, -1.0), 0.5), blue);
        b.add(tri(new Vec3(0, 17.0, -3.4), new Vec3(0, 23.0, -8.6), new Vec3(0, 11.0, -5.0), 0.6), blue);
        b.sym(limb(4.0, 17.0, 0.6, 6.4, 13.0, 2.4, 1.2, 1.0), blue);
        b.sym(tri(new Vec3(6.4, 13.8, 2.0), new Vec3(10.6, 12.0, 0.6), new Vec3(6.6, 10.4, -1.6), 0.5), blue);
        tail(b, blue, 2.0, 0.8, new Vec3(0, 7.0, -3.4), new Vec3(0, 3.4, -8.0), new Vec3(0, 2.0, -11.0));
        b.add(tri(new Vec3(0, 2.4, -10.0), new Vec3(2.6, 3.6, -12.6), new Vec3(-2.6, 3.6, -12.6), 0.4), blue);
        return b.build();
    }

    static Sculpt arceus() {
        Sculpt.Builder b = Sculpt.builder(28);
        String w = WHITE;
        String g = "minecraft:light_gray_concrete";
        b.add(ellipsoid(0, 11.0, -0.6, 4.0, 3.6, 7.0), w);
        b.paint(ellipsoid(0, 9.0, -0.6, 4.2, 2.0, 7.2), g);
        leg(b, 2.4, 4.0, 9.0, 1.4, w, GOLD);
        leg(b, 2.6, -5.0, 9.0, 1.5, w, GOLD);
        b.add(limb(0, 13.6, 5.0, 0, 18.6, 7.6, 2.2, 1.8), w);
        b.add(ellipsoid(0, 20.4, 8.4, 2.4, 2.4, 2.8), w);
        b.paint(ellipsoid(0, 19.4, 10.4, 1.6, 1.2, 1.2), g);
        eyesWhite(b, 1.2, 21.0, 10.2, 0.55, RED);
        b.add(horn(0, 22.4, 7.6, 0, 26.6, 2.6, 1.6), w);
        b.sym(horn(1.4, 21.6, 7.0, 4.6, 24.6, 4.6, 0.9), w);
        b.add(torusY(0, 11.6, -0.6, 7.6, 0.9).rotated(0, 0, 90, new Vec3(0, 11.6, -0.6)).rotated(90, 0, 0, new Vec3(0, 11.6, -0.6)), GOLD);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(30 + i * 40);
            b.add(sphere(Math.cos(a) * 7.6, 11.6 + Math.sin(a) * 7.6, -0.6, 1.2), "minecraft:emerald_block");
            b.add(sphere(-Math.cos(a) * 7.6, 11.6 + Math.sin(a) * 7.6, -0.6, 1.2), "minecraft:emerald_block");
        }
        b.add(ellipsoid(0, 11.0, -7.6, 2.4, 1.6, 2.4), w);
        b.add(tri(new Vec3(0, 12, -8.6), new Vec3(4.6, 15.0, -13.0), new Vec3(-4.6, 15.0, -13.0), 0.5), g);
        return b.build();
    }

    static Sculpt rondoudou() {
        Sculpt.Builder b = Sculpt.builder(14);
        String p = PINK;
        b.add(ellipsoid(0, 6.6, 0, 5.4, 5.6, 5.0), p);
        b.add(horn(0, 11.4, 2.6, 0, 9.6, 5.6, 1.6), p);
        b.sym(frustum(new Vec3(3.2, 10.6, 0.4), new Vec3(4.8, 13.4, 0.2), 1.4, 0.3), p);
        b.paintSym(frustum(new Vec3(3.3, 10.8, 1.0), new Vec3(4.8, 13.2, 0.6), 0.7, 0.2), BLACK);
        eyesWhite(b, 2.0, 7.6, 4.6, 1.4, "minecraft:light_blue_concrete");
        b.paint(boxC(0, 4.8, 5.0, 0.8, 0.25, 0.6), BLACK);
        b.sym(limb(4.6, 5.6, 1.4, 6.0, 6.4, 2.6, 0.9, 0.8), p);
        b.sym(ellipsoid(2.4, 0.9, 0.8, 1.6, 0.9, 2.2), p);
        return b.build();
    }
}
