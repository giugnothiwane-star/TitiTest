package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Trees;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;

/** 20 arbres géants, végétaux et jardins. */
public final class NatureModels {
    private NatureModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.NATURE);
        SizeSpec T = Reg.TREE;
        r.sculpt("chene_geant", "Chêne géant", 3, T, () -> Trees.branched(Trees.spec().trunk(0.45, 2.0).branches(6, 2, 8, 45).crown(5).seed(11)),
                "arbre", "chene", "foret");
        r.sculpt("bouleau_geant", "Bouleau géant", 2, T, () -> Trees.branched(Trees.spec().log("minecraft:birch_log[axis=y]")
                .leaves("minecraft:birch_leaves[persistent=true]").trunk(0.62, 1.2).branches(4, 2, 5, 25).crown(3.8).seed(12)), "arbre", "bouleau");
        r.sculpt("sapin_geant", "Sapin géant", 2, T, () -> Trees.conifer("minecraft:spruce_log[axis=y]", "minecraft:spruce_leaves[persistent=true]", 1.0, 13),
                "arbre", "sapin", "conifere", "noel");
        r.sculpt("cerisier", "Cerisier en fleurs", 3, T, () -> Trees.branched(Trees.spec().log("minecraft:cherry_log[axis=y]")
                .leaves("minecraft:cherry_leaves[persistent=true]").trunk(0.4, 1.5).branches(5, 2, 8, 58).crown(5).seed(14)), "arbre", "sakura", "japon", "rose");
        r.sculpt("baobab", "Baobab", 3, T, NatureModels::baobab, "arbre", "afrique", "savane");
        r.sculpt("saule_pleureur", "Saule pleureur", 3, T, NatureModels::willow, "arbre", "saule", "riviere");
        r.sculpt("palmier", "Palmier", 2, T, () -> Trees.palm(15), "arbre", "plage", "tropical", "cocotier");
        r.sculpt("paletuvier", "Palétuvier (mangrove)", 3, T, NatureModels::mangrove, "arbre", "mangrove", "marais");
        r.sculpt("erable_automne", "Érable d'automne", 3, T, () -> Trees.branched(Trees.spec().leaves(Mat.mix(16, 1.0, 0.45,
                Mat.mix(17, 1.0, 0.4, Mat.of("minecraft:orange_terracotta"), Mat.of("minecraft:red_terracotta")), Mat.of("minecraft:yellow_terracotta")))
                .trunk(0.45, 1.6).branches(5, 2, 7, 45).crown(4.8).seed(16)), "arbre", "automne", "erable");
        r.sculpt("arbre_mort", "Arbre mort tordu", 2, T, () -> Trees.branched(Trees.spec().log("minecraft:dark_oak_log[axis=y]").bare()
                .trunk(0.5, 1.6).branches(5, 3, 7, 50).seed(17)), "arbre", "mort", "halloween", "effrayant");
        r.sculpt("arbre_cristal", "Arbre de cristal", 4, T, NatureModels::crystalTree, "arbre", "cristal", "magique", "amethyste");
        r.sculpt("champignon_rouge", "Champignon géant rouge", 2, T, () -> mushroom(true), "champignon", "geant", "fantastique");
        r.sculpt("champignon_brun", "Champignon géant brun", 2, T, () -> mushroom(false), "champignon", "geant", "plat");
        r.sculpt("cactus_geant", "Cactus saguaro", 2, T, NatureModels::cactus, "cactus", "desert", "western");
        r.gen("bosquet_bambous", "Bosquet de bambous", 2, Reg.ISLAND, p -> bamboo(p.size(), p.seed()), "bambou", "panda", "asie");
        r.gen("jardin_fleuri", "Jardin fleuri", 3, Reg.ISLAND, p -> flowerGarden(p.size(), p.seed()), "jardin", "fleurs", "parc");
        r.sculpt("yggdrasil", "Yggdrasil, l'arbre-monde", 5, SizeSpec.height(20, 45, 80, 140), NatureModels::worldTree, "arbre", "monde", "mythologie", "geant");
        r.sculpt("sequoia", "Séquoia géant", 3, SizeSpec.height(16, 36, 70, 130), NatureModels::sequoia, "arbre", "sequoia", "immense");
        r.sculpt("bonsai", "Bonsaï en pot", 3, SizeSpec.height(6, 12, 24, 48), NatureModels::bonsai, "arbre", "bonsai", "japon", "pot");
        r.sculpt("azalee_fleurie", "Azalée fleurie", 2, T, () -> Trees.branched(Trees.spec().leaves(Mat.mix(18, 1.0, 0.45,
                Mat.of("minecraft:azalea_leaves[persistent=true]"), Mat.of("minecraft:flowering_azalea_leaves[persistent=true]")))
                .trunk(0.35, 1.4).branches(5, 2, 6, 55).crown(4.2).seed(18)), "arbre", "azalee", "fleurs");
    }

    static Sculpt baobab() {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        String log = "minecraft:stripped_acacia_log[axis=y]";
        b.add(frustum(new Vec3(0, -0.5, 0), new Vec3(0, 18, 0), 6.0, 3.6), log);
        b.add(ellipsoid(0, 9, 0, 6.6, 9, 6.6), log);
        Rng rng = new Rng(19);
        for (int i = 0; i < 7; i++) {
            double a = Math.toRadians(i * 51.4);
            Vec3 end = new Vec3(Math.cos(a) * rng.range(6, 9), rng.range(22, 25), Math.sin(a) * rng.range(6, 9));
            b.add(limb(new Vec3(0, 17, 0), end, 1.4, 0.6), log);
            b.fill(ellipsoid(end.x(), end.y() + 1.2, end.z(), 3.6, 1.6, 3.6), "minecraft:acacia_leaves[persistent=true]");
        }
        return b.build();
    }

    static Sculpt willow() {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        String log = "minecraft:oak_log[axis=y]";
        b.add(frustum(new Vec3(0, -0.5, 0), new Vec3(0, 14, 0), 2.2, 1.4), log);
        Rng rng = new Rng(20);
        String leaves = "minecraft:oak_leaves[persistent=true]";
        b.fill(ellipsoid(0, 19, 0, 10, 5, 10), leaves);
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60);
            b.add(limb(0, 13, 0, Math.cos(a) * 7, 18, Math.sin(a) * 7, 1.0, 0.5), log);
        }
        for (int i = 0; i < 40; i++) {
            double a = rng.range(0, Math.PI * 2);
            double rr = rng.range(5.5, 10.5);
            double x = Math.cos(a) * rr, z = Math.sin(a) * rr;
            b.fill(limb(x, 18, z, x * 1.05, rng.range(3, 8), z * 1.05, 0.55, 0.45), leaves);
        }
        return b.build();
    }

    static Sculpt mangrove() {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        String log = "minecraft:mangrove_log[axis=y]";
        b.add(frustum(new Vec3(0, 6, 0), new Vec3(0, 18, 0), 1.6, 1.0), log);
        for (int i = 0; i < 7; i++) {
            double a = Math.toRadians(i * 51);
            b.add(tube(0.8, 0.6, new Vec3(0, 7, 0), new Vec3(Math.cos(a) * 4, 6, Math.sin(a) * 4), new Vec3(Math.cos(a) * 6.5, -0.5, Math.sin(a) * 6.5)),
                    "minecraft:mangrove_roots[waterlogged=false]");
        }
        Sculpt crown = Trees.branched(Trees.spec().log(log).leaves("minecraft:mangrove_leaves[persistent=true]").trunk(0.0, 0.1)
                .branches(5, 2, 6, 45).crown(4.6).seed(21));
        b.addAll(crown, 0, 16, 0, 0, 0.6);
        return b.build();
    }

    static Sculpt crystalTree() {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        Sculpt base = Trees.branched(Trees.spec().log("minecraft:calcite").bare().trunk(0.45, 1.5).branches(5, 2, 7, 45).seed(22));
        b.addAll(base);
        Rng rng = new Rng(23);
        String[] glass = {"minecraft:light_blue_stained_glass", "minecraft:purple_stained_glass", "minecraft:amethyst_block",
                "minecraft:magenta_stained_glass", "minecraft:cyan_stained_glass"};
        for (int i = 0; i < 26; i++) {
            double a = rng.range(0, Math.PI * 2);
            double rr = rng.range(2, 11);
            Vec3 p = new Vec3(Math.cos(a) * rr, rng.range(16, 28), Math.sin(a) * rr);
            Vec3 tip = p.add(new Vec3(rng.range(-1, 1), rng.range(1.5, 3.5), rng.range(-1, 1)));
            b.fill(frustum(p, tip, rng.range(0.8, 1.4), 0.1), glass[rng.nextInt(glass.length)]);
        }
        b.fill(ellipsoid(0, 0.3, 0, 7, 0.8, 7), "minecraft:amethyst_block");
        return b.build();
    }

    static Sculpt mushroom(boolean red) {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        b.add(frustum(new Vec3(0, -0.5, 0), new Vec3(0, 20, 0), 2.6, 2.0), "minecraft:mushroom_stem");
        if (red) {
            b.add(ellipsoid(0, 20, 0, 10, 8, 10).and(com.ultimateautobuilder.core.geom.Shapes.box(-11, 20, -11, 11, 30, 11)),
                    Mat.spots(24, 1.6, 0.72, Mat.of("minecraft:red_mushroom_block"), Mat.of("minecraft:white_concrete")));
        } else {
            b.add(cylinderY(0, 20, 0, 2.0, 12), "minecraft:brown_mushroom_block");
            b.add(cylinderY(0, 22, 0, 1.0, 9), "minecraft:brown_mushroom_block");
        }
        b.add(cylinderY(0, 18.5, 0, 0.8, 4.5), "minecraft:white_concrete");
        return b.build();
    }

    static Sculpt cactus() {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        Mat skin = Mat.stripesX(1.2, 0.35, Mat.of("minecraft:green_concrete"), Mat.of("minecraft:lime_terracotta"));
        b.add(limb(0, -0.5, 0, 0, 27, 0, 2.6, 2.4), skin);
        b.add(tube(1.8, 1.6, new Vec3(1.5, 10, 0), new Vec3(6.5, 11, 0), new Vec3(7, 15, 0), new Vec3(7, 21, 0)), skin);
        b.add(tube(1.6, 1.5, new Vec3(-1.5, 14, 0), new Vec3(-5.5, 15, 0), new Vec3(-6, 19, 0), new Vec3(-6, 24, 0)), skin);
        b.add(sphere(0, 27.6, 0, 1.0), "minecraft:pink_concrete");
        return b.build();
    }

    static VoxelModel bamboo(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(5, width / 2);
        Arch.disk(g, 0, 0, 0, r, "minecraft:podzol");
        for (int i = 0; i < r * r; i++) {
            int x = rng.range(-r + 1, r - 1), z = rng.range(-r + 1, r - 1);
            if (x * x + z * z > (r - 1) * (r - 1) || g.isSet(x, 1, z)) {
                continue;
            }
            int h = rng.range(r + 2, r * 2 + 6);
            g.fill(x, 1, z, x, h, z, "minecraft:bamboo_block[axis=y]");
            g.set(x, h + 1, z, B.leaves("jungle"));
            g.set(x + 1, h, z, B.leaves("jungle"));
            g.set(x - 1, h, z, B.leaves("jungle"));
        }
        g.fill(-1, 0, -r, 1, 0, r, "minecraft:dirt_path");
        g.fill(-1, 1, -r, 1, 2 * r + 8, r, null);
        g.fill(-1, 1, -r, 1, 3, r, B.AIR);
        g.set(2, 1, r - 2, "minecraft:stone_brick_wall");
        g.set(2, 2, r - 2, B.lantern(false));
        return g.freeze();
    }

    static VoxelModel flowerGarden(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(13, width);
        Rect r = Rect.centered(w, w);
        Arch.floor(g, r, 0, "minecraft:grass_block");
        String[][] beds = {{"minecraft:red_tulip", "minecraft:white_tulip"}, {"minecraft:cornflower", "minecraft:allium"},
                {"minecraft:dandelion", "minecraft:oxeye_daisy"}, {"minecraft:poppy", "minecraft:azure_bluet"},
                {"minecraft:pink_tulip", "minecraft:lily_of_the_valley"}, {"minecraft:orange_tulip", "minecraft:blue_orchid"}};
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int z = r.z0(); z <= r.z1(); z++) {
                boolean path = Math.abs(x) <= 1 || Math.abs(z) <= 1 || Math.abs(Math.abs(x) - Math.abs(z)) <= 0;
                if (r.onEdge(x, z)) {
                    g.set(x, 1, z, B.leaves("oak"));
                    continue;
                }
                if (path) {
                    g.set(x, 0, z, "minecraft:smooth_stone");
                    continue;
                }
                String[] bed = beds[Math.floorMod((x > 0 ? 1 : 0) + (z > 0 ? 2 : 0) + (Math.abs(x) > Math.abs(z) ? 3 : 0), beds.length)];
                g.set(x, 1, z, bed[rng.nextInt(2)]);
            }
        }
        Arch.disk(g, 0, 1, 0, 2.5, "minecraft:stone_bricks");
        Arch.disk(g, 0, 1, 0, 1.5, "minecraft:water[level=0]");
        g.fill(0, 1, 0, 0, 3, 0, "minecraft:stone_brick_wall");
        g.set(0, 4, 0, "minecraft:sea_lantern");
        for (int i = 0; i < 4; i++) {
            int x = (i & 1) == 0 ? r.x0() + 3 : r.x1() - 3;
            int z = (i & 2) == 0 ? r.z0() + 3 : r.z1() - 3;
            Trees.oakInto(g, x, 1, z, Math.max(6, w / 3), rng.nextLong(), "minecraft:birch_log[axis=y]", "minecraft:flowering_azalea_leaves[persistent=true]");
        }
        return g.freeze();
    }

    static Sculpt worldTree() {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        Rng rng = new Rng(25);
        b.add(frustum(new Vec3(0, -0.5, 0), new Vec3(0, 16, 0), 4.5, 2.6), "minecraft:dark_oak_log[axis=y]");
        for (int i = 0; i < 9; i++) {
            double a = Math.toRadians(i * 40 + rng.range(-10, 10));
            b.add(tube(1.8, 0.4, new Vec3(Math.cos(a) * 2, 3, Math.sin(a) * 2), new Vec3(Math.cos(a) * 7, 0.5, Math.sin(a) * 7),
                    new Vec3(Math.cos(a) * 12, 0, Math.sin(a) * 12)), "minecraft:dark_oak_log[axis=y]");
        }
        Sculpt crown = Trees.branched(Trees.spec().log("minecraft:dark_oak_log[axis=y]").leaves(Mat.mix(26, 1.5, 0.06,
                Mat.of("minecraft:dark_oak_leaves[persistent=true]"), Mat.of("minecraft:shroomlight"))).trunk(0.0, 0.1)
                .branches(7, 3, 7, 50).crown(4.0).seed(27));
        b.addAll(crown, 0, 14, 0, 0, 0.75);
        return b.build();
    }

    static Sculpt sequoia() {
        Sculpt.Builder b = Sculpt.builder(Trees.H);
        b.add(frustum(new Vec3(0, -0.5, 0), new Vec3(0, 28, 0), 2.6, 0.8), "minecraft:mangrove_log[axis=y]");
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90 + 45);
            b.add(limb(0, 2, 0, Math.cos(a) * 4.2, 0, Math.sin(a) * 4.2, 1.4, 0.6), "minecraft:mangrove_log[axis=y]");
        }
        for (int i = 0; i < 6; i++) {
            double y = 14 + i * 2.6;
            b.fill(ellipsoid(0, y, 0, 4.2 - i * 0.45, 1.6, 4.2 - i * 0.45), "minecraft:spruce_leaves[persistent=true]");
        }
        b.fill(frustum(new Vec3(0, 28, 0), new Vec3(0, 31, 0), 1.2, 0.1), "minecraft:spruce_leaves[persistent=true]");
        return b.build();
    }

    static Sculpt bonsai() {
        Sculpt.Builder b = Sculpt.builder(12);
        b.add(boxC(0, 1.0, 0, 4.2, 1.0, 3.0), "minecraft:blue_terracotta");
        b.add(boxC(0, 2.05, 0, 3.8, 0.1, 2.6), "minecraft:podzol");
        b.add(tube(0.8, 0.45, new Vec3(0, 2, 0), new Vec3(1.4, 4.5, 0.2), new Vec3(-0.8, 6.5, 0), new Vec3(0.6, 8.5, -0.2)),
                "minecraft:spruce_log[axis=y]");
        b.add(limb(-0.8, 6.5, 0, -3.6, 7.0, 0.4, 0.35, 0.25), "minecraft:spruce_log[axis=y]");
        b.add(limb(1.4, 4.5, 0.2, 3.8, 5.6, -0.4, 0.35, 0.25), "minecraft:spruce_log[axis=y]");
        String leaves = "minecraft:azalea_leaves[persistent=true]";
        b.fill(ellipsoid(-3.6, 7.6, 0.4, 2.0, 0.9, 1.6), leaves);
        b.fill(ellipsoid(3.8, 6.2, -0.4, 1.8, 0.8, 1.4), leaves);
        b.fill(ellipsoid(0.6, 9.4, -0.2, 2.4, 1.1, 2.0), leaves);
        return b.build();
    }
}
