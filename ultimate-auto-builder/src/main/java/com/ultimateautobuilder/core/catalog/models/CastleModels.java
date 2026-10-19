package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.Castle;
import com.ultimateautobuilder.core.generator.Castle.TowerShape;
import com.ultimateautobuilder.core.generator.House;
import com.ultimateautobuilder.core.generator.House.Feature;
import com.ultimateautobuilder.core.generator.RoofMat;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

import java.util.function.Supplier;

/** 30 châteaux, fortifications et bâtiments médiévaux. */
public final class CastleModels {
    private CastleModels() {
    }

    private static void castle(Reg r, String id, String name, int cx, Supplier<Castle.Spec> spec, String... tags) {
        r.gen(id, name, cx, Reg.CASTLE, p -> Castle.generate(spec.get(), p.size(), p.seed()), tags);
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.CASTLES);
        castle(r, "chateau_fort_petit", "Petit château fort", 4, () -> Castle.spec().towers(TowerShape.SQUARE, 0.9).roofs(null).noHouses(),
                "chateau", "fort", "medieval", "chevalier");
        castle(r, "chateau_fort", "Château fort à douves", 5, () -> Castle.spec().moat(), "chateau", "fort", "douves", "pont levis", "medieval");
        castle(r, "grand_chateau_royal", "Grand château royal", 5, () -> Castle.spec().concentric().moat().roofs("blue_concrete")
                .flag("blue_wool").keep(royalKeep()), "chateau", "royal", "roi", "concentrique");
        castle(r, "chateau_conte_fees", "Château de conte de fées", 5, () -> Castle.spec().walls("calcite", "smooth_quartz")
                .roofs("light_blue_concrete").cone(3).towers(TowerShape.ROUND, 0.8).height(1.3).flag("pink_wool").keep(fairyKeep())
                .houses(House.spec().walls("white_terracotta", "birch_log[axis=y]").roof(RoofType.STEEP, "prismarine_brick")),
                "chateau", "princesse", "conte", "fees", "disney");
        castle(r, "chateau_sombre", "Château sombre", 5, () -> Castle.spec().walls("polished_blackstone_bricks", "blackstone")
                .roofs("red_nether_bricks").flag("black_wool").ground("soul_soil", "blackstone").keep(darkKeep())
                .houses(House.spec().walls("deepslate_bricks", "blackstone").roof(RoofType.STEEP, "blackstone")), "chateau", "sombre", "vampire", "mechant");
        castle(r, "chateau_desert", "Château du désert", 4, () -> Castle.spec().walls("cut_sandstone", "smooth_sandstone").roofs(null)
                .ground("sand", "smooth_sandstone").flag("orange_wool").keep(House.spec().walls("cut_sandstone", "chiseled_sandstone")
                        .foundation("smooth_sandstone").trim("smooth_sandstone").roof(RoofType.DOME, "smooth_sandstone").floors(2))
                .houses(House.spec().walls("sandstone", "smooth_sandstone").roof(RoofType.FLAT, "smooth_sandstone")), "chateau", "desert", "sable", "orient");
        castle(r, "chateau_glace", "Château de glace", 5, () -> Castle.spec().walls("packed_ice", "blue_ice").roofs("blue_ice").cone(3)
                .ground("snow_block", "packed_ice").flag("light_blue_wool").keep(House.spec().walls("packed_ice", "blue_ice")
                        .foundation("snow_block").floorMat("packed_ice").trim("blue_ice").window("light_blue_stained_glass_pane")
                        .roof(RoofType.STEEP, "prismarine").floors(3).with(Feature.ROOMS)).noHouses(), "chateau", "glace", "reine des neiges", "hiver");
        castle(r, "chateau_nether", "Forteresse infernale", 5, () -> Castle.spec().walls("nether_bricks", "red_nether_bricks").roofs(null)
                .ground("netherrack", "nether_bricks").flag("red_wool").keep(House.spec().walls("nether_bricks", "red_nether_bricks")
                        .foundation("netherrack").floorMat("nether_bricks").trim("magma_block").window("iron_bars")
                        .roof(RoofType.FLAT, "nether_brick").floors(3)).noHouses(), "chateau", "nether", "enfer", "forteresse");
        castle(r, "chateau_elfique", "Château elfique", 5, () -> Castle.spec().walls("quartz_bricks", "smooth_quartz").roofs("green_concrete")
                .cone(3).towers(TowerShape.ROUND, 0.75).height(1.4).flag("lime_wool").ground("moss_block", "smooth_quartz")
                .keep(House.spec().walls("quartz_bricks", "birch_log[axis=y]").roof(RoofType.STEEP, "warped").floors(3).furniture("birch"))
                .houses(House.spec().walls("birch_planks", "birch_log[axis=y]").roof(RoofType.STEEP, "warped").furniture("birch")),
                "chateau", "elfe", "fantasy", "foret");
        r.gen("chateau_japonais", "Château japonais", 5, Reg.CASTLE, p -> japaneseCastle(p.size(), p.seed()), "chateau", "japon", "samourai", "himeji");
        castle(r, "forteresse_carree", "Forteresse carrée", 4, () -> Castle.spec().walls("deepslate_bricks", "cobbled_deepslate")
                .towers(TowerShape.SQUARE, 1.1).roofs(null).height(1.2).keep(House.spec().walls("deepslate_bricks", "cobbled_deepslate")
                        .roof(RoofType.FLAT, "deepslate_brick").floors(2).window("iron_bars")), "forteresse", "militaire", "garnison");
        castle(r, "citadelle", "Citadelle sur butte", 5, () -> Castle.spec().raised().concentric().roofs("dark_oak_planks"),
                "citadelle", "forteresse", "colline");
        r.gen("tour_garde", "Tour de garde", 2, SizeSpec.width(7, 11, 17, 27), p -> watchTower(p.size(), p.seed()), "tour", "garde", "guet");
        r.gen("tour_sorcier", "Tour du sorcier", 3, SizeSpec.width(7, 11, 17, 27), p -> wizardTower(p.size(), p.seed()), "tour", "sorcier", "magie");
        r.gen("donjon_medieval", "Donjon médiéval", 3, Reg.BUILDING, p -> keepAlone(p.size(), p.seed()), "donjon", "tour", "medieval");
        r.gen("porte_fortifiee", "Porte fortifiée et pont-levis", 3, Reg.BUILDING, p -> fortifiedGate(p.size()), "porte", "pont levis", "herse");
        r.gen("rempart_tours", "Rempart à tours", 3, Reg.CASTLE, p -> rampart(p.size()), "rempart", "muraille", "tours");
        r.gen("village_medieval", "Village médiéval", 5, Reg.CASTLE, p -> medievalVillage(p.size(), p.seed()), "village", "medieval", "maisons");
        r.gen("taverne", "Taverne médiévale", 3, Reg.HOUSE, p -> House.generate(House.spec().walls("white_terracotta", "dark_oak_log[axis=y]")
                .trim("dark_oak_log[axis=x]").roof(RoofType.STEEP, "spruce").floors(2).depth(1.1).furniture("spruce").colors("red", "brown")
                .with(Feature.CHIMNEY, Feature.LANTERNS, Feature.ROOMS, Feature.PORCH), p.size(), p.seed()), "taverne", "auberge", "medieval");
        r.gen("forge", "Forge du forgeron", 3, Reg.HOUSE, p -> forge(p.size()), "forge", "forgeron", "enclume");
        r.gen("eglise_village", "Église de village", 4, Reg.BUILDING, p -> church(p.size()), "eglise", "clocher", "village");
        r.gen("marche_medieval", "Marché médiéval", 3, Reg.BUILDING, p -> market(p.size(), p.seed()), "marche", "etals", "medieval");
        r.gen("ecurie", "Écurie", 3, Reg.BUILDING, p -> stable(p.size()), "ecurie", "chevaux", "foin");
        r.gen("moulin_eau", "Moulin à eau", 3, Reg.HOUSE, p -> waterMill(p.size(), p.seed()), "moulin", "eau", "roue");
        castle(r, "chateau_loire", "Château de la Loire", 5, () -> Castle.spec().walls("smooth_sandstone", "calcite").roofs("deepslate_tiles")
                .cone(3).moat().flag("white_wool").keep(House.spec().walls("smooth_sandstone", "calcite").trim("calcite")
                        .roof(RoofType.MANSARD, "deepslate_tile").floors(3).depth(0.6).with(Feature.ROOMS, Feature.TOWER)), "chateau", "loire", "renaissance");
        castle(r, "chateau_ruines", "Château en ruines", 4, () -> Castle.spec().ruined().roofs(null), "chateau", "ruines", "abandonne");
        r.gen("chateau_motte", "Motte castrale en bois", 3, Reg.CASTLE, p -> motte(p.size(), p.seed()), "motte", "palissade", "bois");
        r.gen("trebuchet", "Trébuchet", 3, SizeSpec.length(8, 16, 32, 64), p -> trebuchet(p.size()), "trebuchet", "siege", "catapulte");
        castle(r, "chateau_falaise", "Château sur la falaise", 5, () -> Castle.spec().raised().towers(TowerShape.ROUND, 1.2).height(1.2)
                .walls("stone_bricks", "andesite").flag("purple_wool"), "chateau", "falaise", "rocher");
        r.gen("arene_tournoi", "Lice de tournoi", 3, Reg.CASTLE, p -> tournament(p.size()), "tournoi", "joute", "arene", "chevalier");
    }

    static House.Spec royalKeep() {
        return House.spec().walls("stone_bricks", "polished_andesite").trim("gold_block").roof(RoofType.STEEP, "deepslate_tile").floors(4)
                .depth(0.8).window("glass_pane").furniture("dark_oak").colors("blue", "blue").with(Feature.ROOMS, Feature.TOWER);
    }

    static House.Spec fairyKeep() {
        return House.spec().walls("calcite", "smooth_quartz").trim("smooth_quartz").roof(RoofType.STEEP, "prismarine_brick").floors(4)
                .furniture("birch").colors("pink", "pink").with(Feature.ROOMS, Feature.TOWER, Feature.BALCONY);
    }

    static House.Spec darkKeep() {
        return House.spec().walls("deepslate_bricks", "blackstone").trim("red_nether_bricks").foundation("blackstone")
                .roof(RoofType.STEEP, "blackstone").floors(4).window("red_stained_glass_pane").furniture("crimson").colors("black", "red")
                .with(Feature.ROOMS, Feature.TOWER);
    }

    // ------------------------------------------------------------------ constructions spécifiques

    static VoxelModel japaneseCastle(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width * 2 / 3);
        int baseH = Math.max(4, w / 5);
        for (int i = 0; i < baseH; i++) {
            Rect l = Rect.centered(w + 2 * (baseH - i), w + 2 * (baseH - i));
            Arch.floor(g, l, i, "minecraft:stone_bricks");
        }
        int y = baseH;
        int tw = w;
        RoofMat roof = RoofMat.of("deepslate_tile");
        for (int tier = 0; tw >= 5 && tier < 5; tier++) {
            Rect t = Rect.centered(tw, tw);
            int h = 4;
            Arch.floor(g, t, y, "minecraft:dark_oak_planks");
            Arch.walls(g, t, y + 1, y + h - 1, "minecraft:white_concrete");
            Arch.air(g, t.inset(1), y + 1, y + h - 1);
            Arch.pillars(g, t, y + 1, y + h - 1, "minecraft:dark_oak_log[axis=y]");
            Arch.windows(g, t, y + 2, 1, 3, "minecraft:dark_oak_trapdoor[facing=south,half=bottom,open=true]", null);
            Arch.hip(g, t, y + h, roof, 2, true);
            Arch.air(g, t.inset(1), y + h, y + h + 1);
            y += h + 1;
            tw -= Math.max(2, tw / 4);
            if (tw % 2 == 0) {
                tw--;
            }
        }
        Rect top = Rect.centered(Math.max(3, tw + 2), Math.max(3, tw + 2));
        int ry = Arch.hip(g, top, y, roof, 1, true);
        g.set(-1, ry, 0, "minecraft:gold_block");
        g.set(1, ry, 0, "minecraft:gold_block");
        Arch.door(g, 0, baseH + 1, w / 2, "dark_oak", B.S);
        for (int i = 0; i < baseH; i++) {
            g.set(0, i, w / 2 + baseH - i + 1, B.stairs("stone_brick", B.N, false));
            g.set(-1, i, w / 2 + baseH - i + 1, B.stairs("stone_brick", B.N, false));
            g.set(1, i, w / 2 + baseH - i + 1, B.stairs("stone_brick", B.N, false));
        }
        Rng rng = new Rng(seed);
        for (int i = 0; i < 6; i++) {
            int x = rng.range(-w, w), z = rng.range(-w, w);
            if (Math.abs(x) > w / 2 + baseH + 2 || Math.abs(z) > w / 2 + baseH + 2) {
                com.ultimateautobuilder.core.generator.Trees.cherryInto(g, x, 0, z, Math.max(6, w / 3), rng.nextLong());
            }
        }
        return g.freeze();
    }

    static VoxelModel watchTower(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(5, width / 2 * 2 + 1);
        int h = w * 2 + 6;
        Rect r = Rect.centered(w, w);
        for (int y = 0; y < h; y++) {
            Arch.walls(g, r, y, y, "minecraft:cobblestone");
            Arch.air(g, r.inset(1), y, y);
        }
        Arch.pillars(g, r, 0, h, "minecraft:stripped_spruce_log[axis=y]");
        Arch.floor(g, r.inset(1), 0, "minecraft:spruce_planks");
        for (int y = 1; y <= h; y++) {
            g.set(r.x0() + 1, y, r.z0() + 1, "minecraft:ladder[facing=south]");
        }
        Rect plat = r.grow(1);
        Arch.floor(g, plat, h, "minecraft:spruce_planks");
        g.set(r.x0() + 1, h, r.z0() + 1, "minecraft:ladder[facing=south]");
        Arch.walls(g, plat, h + 1, h + 1, "minecraft:spruce_fence");
        Arch.pillars(g, plat, h + 1, h + 3, "minecraft:spruce_fence");
        Arch.hip(g, plat, h + 4, RoofMat.of("spruce"), 1, false);
        g.set(0, h + 1, 0, "minecraft:campfire[lit=true,signal_fire=false,facing=north]");
        Arch.door(g, r.cx(), 1, r.z1(), "spruce", B.S);
        for (int y = 4; y < h - 2; y += 5) {
            g.set(r.cx(), y, r.z0(), "minecraft:glass_pane");
            g.set(r.x0(), y, r.cz(), "minecraft:glass_pane");
            g.set(r.x1(), y, r.cz(), "minecraft:glass_pane");
        }
        return g.freeze();
    }

    static VoxelModel wizardTower(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(3, width / 2);
        int h = r * 5 + 6;
        Arch.cylinder(g, 0, 0, 0, r, h, "minecraft:stone_bricks", "minecraft:dark_oak_planks", 5);
        for (int y = 0; y < h; y++) {
            int ri = (int) Math.ceil(r);
            for (int dz = -ri; dz <= ri; dz++) {
                for (int dx = -ri; dx <= ri; dx++) {
                    String st = g.get(dx, y, dz);
                    if ("minecraft:stone_bricks".equals(st) && com.ultimateautobuilder.core.util.Noise.hash(seed, dx, y, dz) < 0.3) {
                        g.set(dx, y, dz, "minecraft:mossy_stone_bricks");
                    }
                }
            }
        }
        for (int y = 3; y < h - 1; y += 5) {
            g.set(0, y, r, "minecraft:purple_stained_glass_pane");
            g.set(r, y + 2, 0, "minecraft:purple_stained_glass_pane");
            g.set(-r, y + 1, 0, "minecraft:purple_stained_glass_pane");
        }
        for (int y = 1; y < h; y++) {
            g.set(0, y, -r + 1, "minecraft:ladder[facing=south]");
        }
        Arch.disk(g, 0, h, 0, r + 1, "minecraft:dark_oak_planks");
        g.set(0, h, -r + 1, "minecraft:ladder[facing=south]");
        int cone = r * 3 + 3;
        for (int i = 0; i <= cone; i++) {
            double ri = (r + 1.5) * (1.0 - (double) i / (cone + 1));
            int shift = (int) Math.round(Math.pow((double) i / cone, 2) * r);
            Arch.ring(g, shift, h + 1 + i, 0, Math.max(0.4, ri), "minecraft:purple_concrete");
        }
        g.set(r, h + cone + 2, 0, "minecraft:sea_lantern");
        g.set(0, h + 1, 0, "minecraft:enchanting_table");
        g.set(1, h + 1, 1, "minecraft:bookshelf");
        g.set(-1, h + 1, 1, "minecraft:brewing_stand");
        Arch.door(g, 0, 1, r, "dark_oak", B.S);
        return g.freeze();
    }

    static VoxelModel keepAlone(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Castle.Spec s = Castle.spec();
        int w = Math.max(9, width / 2 * 2 + 1);
        House.Spec k = House.spec().walls("stone_bricks", "cobblestone").foundation("cobblestone").trim("stone_bricks")
                .roof(RoofType.FLAT, "stone_brick").floors(3).depth(1.0).window("iron_bars").door("dark_oak").furniture("dark_oak")
                .with(Feature.ROOMS);
        Rect r = House.build(g, k, w, seed, 0, 0, 0);
        int top = House.floorsFor(k, w) * k.floorHeight + (w >= 45 ? House.floorsFor(k, w) : 0);
        Arch.crenellate(g, r, top + 2, s.wall);
        int tr = Math.max(1, w / 8);
        for (int[] p : new int[][]{{r.x0(), r.z0()}, {r.x1(), r.z0()}, {r.x0(), r.z1()}, {r.x1(), r.z1()}}) {
            Castle.tower(g, s, p[0], 0, p[1], tr, top + 4);
        }
        return g.freeze();
    }

    static VoxelModel fortifiedGate(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width);
        int h = Math.max(8, w / 2);
        Rect wall = new Rect(-w, -1, w, 1);
        g.fill(wall.x0(), 0, wall.z0(), wall.x1(), h, wall.z1(), "minecraft:stone_bricks");
        Arch.crenellate(g, wall, h + 1, "minecraft:stone_bricks");
        Castle.Spec s = Castle.spec();
        for (int side = -1; side <= 1; side += 2) {
            Castle.tower(g, s, side * Math.max(5, w / 3), 0, 0, Math.max(3, w / 5), h + 5);
        }
        g.fill(-2, 1, -2, 2, 6, 2, B.AIR);
        g.fill(-2, 0, -2, 2, 0, 2, "minecraft:cobblestone");
        g.fill(-2, 6, 2, 2, 6, 2, "minecraft:iron_bars");
        g.fill(-2, 5, 2, 2, 5, 2, "minecraft:iron_bars");
        // Douve devant la porte
        g.fill(-w, -3, 3, w, -3, 8, "minecraft:stone_bricks");
        g.fill(-w, -2, 3, w, 0, 8, "minecraft:water[level=0]");
        g.fill(-w, -2, 9, w, 0, 9, "minecraft:stone_bricks");
        g.fill(-w - 1, -3, 3, -w - 1, 0, 9, "minecraft:stone_bricks");
        g.fill(w + 1, -3, 3, w + 1, 0, 9, "minecraft:stone_bricks");
        g.fill(-2, 1, 3, 2, 1, 9, "minecraft:spruce_planks");
        Arch.line(g, -3, 2, 9, -3, 7, 2, "minecraft:chain[axis=y]");
        Arch.line(g, 3, 2, 9, 3, 7, 2, "minecraft:chain[axis=y]");
        g.set(-3, 4, 2, B.wallTorch(B.S));
        g.set(3, 4, 2, B.wallTorch(B.S));
        return g.freeze();
    }

    static VoxelModel rampart(int length) {
        VoxelGrid g = new VoxelGrid();
        int half = Math.max(12, length / 2);
        int h = Math.max(7, length / 6);
        Rect wall = new Rect(-half, -1, half, 1);
        g.fill(wall.x0(), 0, wall.z0(), wall.x1(), h, wall.z1(), "minecraft:stone_bricks");
        Arch.crenellate(g, new Rect(-half, 1, half, 1), h + 1, "minecraft:stone_bricks");
        Arch.crenellate(g, new Rect(-half, -1, half, -1), h + 1, "minecraft:stone_bricks");
        Castle.Spec s = Castle.spec();
        int step = Math.max(10, length / 4);
        for (int x = -half; x <= half; x += step) {
            Castle.tower(g, s, x, 0, 0, Math.max(2, h / 3), h + 5);
        }
        Castle.tower(g, s, half, 0, 0, Math.max(2, h / 3), h + 5);
        return g.freeze();
    }

    static VoxelModel medievalVillage(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(32, width);
        int half = w / 2;
        // Rues pavées en croix et place centrale
        g.fill(-half, 0, -1, half, 0, 1, "minecraft:cobblestone");
        g.fill(-1, 0, -half, 1, 0, half, "minecraft:cobblestone");
        Arch.disk(g, 0, 0, 0, 4, "minecraft:stone_bricks");
        g.fill(-1, -3, -1, 1, 0, 1, "minecraft:cobblestone");
        g.set(0, -2, 0, "minecraft:water[level=0]");
        g.set(0, -1, 0, "minecraft:water[level=0]");
        g.set(0, 0, 0, "minecraft:water[level=0]");
        House.Spec[] styles = {
                House.spec().walls("white_terracotta", "dark_oak_log[axis=y]").trim("dark_oak_log[axis=x]").roof(RoofType.STEEP, "dark_oak").floors(2),
                House.spec().walls("cobblestone", "oak_log[axis=y]").roof(RoofType.GABLE, "spruce").with(Feature.CHIMNEY),
                House.spec().walls("oak_planks", "stripped_oak_log[axis=y]").roof(RoofType.STEEP, "spruce").floors(2),
                House.spec().walls("yellow_terracotta", "spruce_log[axis=y]").roof(RoofType.STEEP, "brick")};
        int hw = Math.max(7, w / 6);
        int cell = hw + 3;
        int first = 3 + hw / 2 + 1;
        for (int d = first; d + hw / 2 <= half; d += cell) {
            for (int side = -1; side <= 1; side += 2) {
                // Rue est-ouest : maisons au nord (porte vers +Z) et au sud (porte vers -Z)
                placeHouse(g, styles[rng.nextInt(styles.length)], hw, rng.nextLong(), side * d, -first, 0);
                placeHouse(g, styles[rng.nextInt(styles.length)], hw, rng.nextLong(), side * d, first, 2);
                // Rue nord-sud : les angles sont déjà occupés par la rue est-ouest
                if (d > first) {
                    placeHouse(g, styles[rng.nextInt(styles.length)], hw, rng.nextLong(), -first, side * d, 3);
                    placeHouse(g, styles[rng.nextInt(styles.length)], hw, rng.nextLong(), first, side * d, 1);
                }
            }
        }
        // Église au bout de la rue principale, au nord du village
        VoxelModel church = church(Math.max(11, w / 4));
        g.paste(church, 0, 0, -half - church.sizeZ() / 2);
        return g.freeze();
    }

    /** Construit une maison à part puis la colle pivotée pour orienter sa porte vers la rue. */
    private static void placeHouse(VoxelGrid g, House.Spec spec, int width, long seed, int x, int z, int turns) {
        VoxelGrid tmp = new VoxelGrid();
        House.build(tmp, spec, width, seed, 0, 0, 0);
        g.paste(tmp, x, 0, z, turns, false);
    }

    static VoxelModel forge(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(7, width);
        int d = Math.max(5, w * 2 / 3);
        Rect r = Rect.centered(w, d);
        Arch.floor(g, r, 0, "minecraft:cobblestone");
        Arch.air(g, r, 1, 4);
        Arch.walls(g, new Rect(r.x0(), r.z0(), r.x1(), r.z0()), 1, 4, "minecraft:cobblestone");
        Arch.walls(g, new Rect(r.x0(), r.z0(), r.x0(), r.z1()), 1, 4, "minecraft:cobblestone");
        Arch.pillars(g, r, 1, 4, "minecraft:spruce_log[axis=y]");
        Arch.roof(g, RoofType.GABLE, r, 5, RoofMat.of("spruce"), 1, "minecraft:spruce_planks");
        Arch.floor(g, r, 5, "minecraft:spruce_planks");
        g.fill(r.x1() - 2, 1, r.z0() + 1, r.x1() - 1, 3, r.z0() + 2, "minecraft:bricks");
        g.set(r.x1() - 2, 1, r.z0() + 2, "minecraft:blast_furnace[facing=south,lit=true]");
        g.set(r.x1() - 1, 1, r.z0() + 3, "minecraft:lava_cauldron");
        g.fill(r.x1() - 2, 4, r.z0() + 1, r.x1() - 2, 9, r.z0() + 1, "minecraft:bricks");
        g.set(r.x1() - 2, 10, r.z0() + 1, "minecraft:campfire[lit=true,signal_fire=false,facing=north]");
        g.set(r.cx(), 1, r.cz(), "minecraft:anvil[facing=east]");
        g.set(r.cx() - 2, 1, r.z0() + 1, "minecraft:grindstone[face=floor,facing=south]");
        g.set(r.x0() + 1, 1, r.z0() + 1, "minecraft:smithing_table");
        g.set(r.x0() + 1, 1, r.z0() + 2, "minecraft:barrel[facing=up,open=false]");
        g.set(r.x0() + 1, 1, r.cz() + 1, "minecraft:water_cauldron[level=3]");
        g.set(r.cx(), 4, r.cz(), B.lantern(true));
        return g.freeze();
    }

    static VoxelModel church(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(9, width / 2 * 2 + 1);
        int len = w * 2;
        Rect nave = new Rect(-w / 2, -len / 2, w / 2, len / 2);
        int h = Math.max(5, w / 2);
        Arch.room(g, nave, 0, h, "minecraft:stone_bricks", "minecraft:polished_andesite");
        for (int z = nave.z0() + 2; z <= nave.z1() - 2; z += 3) {
            g.fill(nave.x0(), 2, z, nave.x0(), h - 1, z, "minecraft:white_stained_glass_pane");
            g.fill(nave.x1(), 2, z, nave.x1(), h - 1, z, "minecraft:yellow_stained_glass_pane");
        }
        Arch.roof(g, RoofType.GABLE_Z, nave, h + 1, RoofMat.of("deepslate_tile"), 1, "minecraft:stone_bricks");
        for (int z = nave.z0() + 3; z <= nave.z1() - 4; z += 2) {
            g.set(nave.cx() - 2, 1, z, B.stairs("oak", B.N, false));
            g.set(nave.cx() - 3, 1, z, B.stairs("oak", B.N, false));
            g.set(nave.cx() + 2, 1, z, B.stairs("oak", B.N, false));
            g.set(nave.cx() + 3, 1, z, B.stairs("oak", B.N, false));
        }
        g.fill(nave.cx() - 1, 1, nave.z0() + 1, nave.cx() + 1, 1, nave.z0() + 1, "minecraft:chiseled_stone_bricks");
        g.set(nave.cx(), 2, nave.z0() + 1, "minecraft:candle[candles=3,lit=true]");
        g.fill(nave.cx(), 1, nave.z0() + 3, nave.cx(), 1, nave.z1() - 1, "minecraft:red_carpet");
        // Clocher
        int tw = Math.max(5, w / 2 * 2 - 1);
        Rect tower = Rect.centered(tw, tw).translate(0, nave.z1() + tw / 2);
        int th = h + w / 2 + 5;
        for (int y = 0; y < th; y++) {
            Arch.walls(g, tower, y, y, "minecraft:stone_bricks");
            Arch.air(g, tower.inset(1), y, y);
        }
        Arch.floor(g, tower, 0, "minecraft:polished_andesite");
        g.fill(tower.cx(), th - 4, tower.z0(), tower.cx(), th - 2, tower.z1(), B.AIR);
        g.fill(tower.x0(), th - 4, tower.cz(), tower.x1(), th - 2, tower.cz(), B.AIR);
        g.set(tower.cx(), th - 1, tower.cz(), "minecraft:bell[attachment=ceiling,facing=south]");
        Arch.floor(g, tower, th, "minecraft:stone_bricks");
        int spire = Arch.hip(g, tower, th + 1, RoofMat.of("deepslate_tile"), 0, false);
        g.fill(tower.cx(), spire, tower.cz(), tower.cx(), spire + 3, tower.cz(), "minecraft:gold_block");
        g.set(tower.cx() - 1, spire + 2, tower.cz(), "minecraft:gold_block");
        g.set(tower.cx() + 1, spire + 2, tower.cz(), "minecraft:gold_block");
        g.fill(tower.cx(), 1, nave.z1(), tower.cx(), 2, nave.z1(), B.AIR);
        g.fill(tower.cx(), 1, tower.z1(), tower.cx(), 2, tower.z1(), B.AIR);
        Arch.door(g, tower.cx(), 1, tower.z1(), "dark_oak", B.S);
        g.set(tower.cx(), h + 2, tower.z1(), "minecraft:white_stained_glass");
        return g.freeze();
    }

    static VoxelModel market(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(11, width);
        Rect plaza = Rect.centered(w, w);
        Arch.floor(g, plaza, 0, "minecraft:stone_bricks");
        String[] colors = {"red", "yellow", "blue", "green", "orange", "purple", "white", "cyan"};
        String[] goods = {"minecraft:melon", "minecraft:pumpkin", "minecraft:hay_block[axis=y]", "minecraft:barrel[facing=up,open=false]",
                "minecraft:chest[facing=south,type=single]", "minecraft:cake[bites=0]", "minecraft:bookshelf", "minecraft:composter[level=0]"};
        int step = 6;
        for (int sx = plaza.x0() + 2; sx + 3 <= plaza.x1() - 1; sx += step) {
            for (int sz = plaza.z0() + 2; sz + 3 <= plaza.z1() - 1; sz += step) {
                if (Math.abs(sx + 2 - plaza.cx()) <= 2 && Math.abs(sz + 2 - plaza.cz()) <= 2) {
                    continue;
                }
                Rect stall = new Rect(sx, sz, sx + 3, sz + 3);
                Arch.pillars(g, stall, 1, 3, "minecraft:spruce_fence");
                String c = colors[rng.nextInt(colors.length)];
                for (int x = stall.x0(); x <= stall.x1(); x++) {
                    for (int z = stall.z0(); z <= stall.z1(); z++) {
                        g.set(x, 4, z, ((x + z) & 1) == 0 ? B.wool(c) : B.wool("white"));
                    }
                }
                g.fill(stall.x0() + 1, 1, stall.z1(), stall.x1() - 1, 1, stall.z1(), "minecraft:spruce_slab[type=top]");
                g.set(stall.x0() + 1, 1, stall.z0() + 1, goods[rng.nextInt(goods.length)]);
                g.set(stall.x1() - 1, 1, stall.z0() + 1, goods[rng.nextInt(goods.length)]);
            }
        }
        g.fill(plaza.cx() - 1, 1, plaza.cz() - 1, plaza.cx() + 1, 1, plaza.cz() + 1, "minecraft:stone_brick_wall");
        g.set(plaza.cx(), 1, plaza.cz(), "minecraft:water[level=0]");
        g.set(plaza.cx(), 0, plaza.cz(), "minecraft:stone_bricks");
        g.fill(plaza.cx(), 1, plaza.cz(), plaza.cx(), 3, plaza.cz(), "minecraft:stone_brick_wall");
        g.set(plaza.cx(), 4, plaza.cz(), B.lantern(false));
        return g.freeze();
    }

    static VoxelModel stable(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(11, width);
        int d = Math.max(7, w / 2);
        Rect r = Rect.centered(w, d);
        Arch.room(g, r, 0, 4, "minecraft:spruce_planks", "minecraft:coarse_dirt");
        Arch.pillars(g, r, 1, 4, "minecraft:spruce_log[axis=y]");
        Arch.roof(g, RoofType.GABLE, r, 5, RoofMat.of("dark_oak"), 1, "minecraft:spruce_planks");
        Arch.floor(g, r, 5, "minecraft:spruce_planks");
        for (int x = r.x0() + 1; x <= r.x1() - 1; x++) {
            if ((x - r.x0()) % 4 == 0) {
                g.fill(x, 1, r.z0() + 1, x, 2, r.cz() - 1, "minecraft:spruce_fence");
            } else if ((x - r.x0()) % 4 == 2) {
                g.set(x, 1, r.z0() + 1, "minecraft:hay_block[axis=y]");
                g.set(x, 1, r.cz() - 1, "minecraft:spruce_fence_gate[facing=south,open=false,in_wall=false]");
            } else {
                g.set(x, 1, r.cz() - 1, "minecraft:spruce_fence");
            }
        }
        for (int x = r.x0() + 2; x < r.x1(); x += 4) {
            g.fill(x, 1, r.z1(), x + 1, 3, r.z1(), B.AIR);
        }
        g.set(r.cx(), 4, r.cz(), B.lantern(true));
        g.fill(r.x0(), 0, r.z1() + 1, r.x1(), 0, r.z1() + 4, "minecraft:coarse_dirt");
        for (int x = r.x0(); x <= r.x1(); x++) {
            g.set(x, 1, r.z1() + 4, "minecraft:oak_fence");
        }
        g.set(r.x1() - 1, 1, r.z1() + 2, "minecraft:water_cauldron[level=3]");
        return g.freeze();
    }

    static VoxelModel waterMill(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        House.Spec s = House.spec().walls("cobblestone", "oak_log[axis=y]").roof(RoofType.GABLE, "spruce").floors(2);
        Rect r = House.build(g, s, Math.max(7, width), seed, 0, 0, 0);
        int wr = Math.max(3, r.depth() / 2);
        int cx = r.x1() + 2;
        int cy = wr + 1;
        // Bief et roue à aubes
        g.fill(cx - 1, -3, r.z0() - 4, cx + 2, -3, r.z1() + 4, "minecraft:stone_bricks");
        g.fill(cx - 1, -2, r.z0() - 4, cx + 2, 0, r.z1() + 4, "minecraft:water[level=0]");
        g.fill(cx + 3, -3, r.z0() - 4, cx + 3, 0, r.z1() + 4, "minecraft:stone_bricks");
        for (int a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a);
            int y = cy + (int) Math.round(Math.sin(rad) * wr);
            int z = r.cz() + (int) Math.round(Math.cos(rad) * wr);
            g.set(cx, y, z, "minecraft:stripped_spruce_log[axis=x]");
            g.set(cx + 1, y, z, "minecraft:stripped_spruce_log[axis=x]");
        }
        for (int a = 0; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            Arch.line(g, cx, cy, r.cz(), cx, cy + (int) Math.round(Math.sin(rad) * wr), r.cz() + (int) Math.round(Math.cos(rad) * wr),
                    "minecraft:spruce_planks");
            Arch.line(g, cx + 1, cy, r.cz(), cx + 1, cy + (int) Math.round(Math.sin(rad) * wr), r.cz() + (int) Math.round(Math.cos(rad) * wr),
                    "minecraft:spruce_planks");
        }
        g.set(cx - 1, cy, r.cz(), "minecraft:spruce_log[axis=x]");
        return g.freeze();
    }

    static VoxelModel motte(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(21, width);
        int mr = w / 4;
        int mh = Math.max(4, w / 7);
        for (int i = 0; i < mh; i++) {
            Arch.disk(g, 0, i, -w / 6, mr + (mh - i), i == mh - 1 ? "minecraft:grass_block" : "minecraft:dirt");
        }
        Rect bailey = Rect.centered(w, w);
        for (int x = bailey.x0(); x <= bailey.x1(); x++) {
            for (int z = bailey.z0(); z <= bailey.z1(); z++) {
                if (bailey.onEdge(x, z) && !(z == bailey.z1() && Math.abs(x) <= 1)) {
                    g.fill(x, 0, z, x, 4, z, "minecraft:spruce_log[axis=y]");
                    if (((x + z) & 1) == 0) {
                        g.set(x, 5, z, "minecraft:spruce_log[axis=y]");
                    }
                }
            }
        }
        Rect tower = Rect.centered(Math.max(5, mr), Math.max(5, mr)).translate(0, -w / 6);
        for (int y = mh; y < mh + 10; y++) {
            Arch.walls(g, tower, y, y, "minecraft:spruce_planks");
            Arch.air(g, tower.inset(1), y, y);
        }
        Arch.pillars(g, tower, mh, mh + 10, "minecraft:spruce_log[axis=y]");
        Arch.floor(g, tower.grow(1), mh + 10, "minecraft:spruce_planks");
        Arch.walls(g, tower.grow(1), mh + 11, mh + 11, "minecraft:spruce_fence");
        Arch.hip(g, tower.grow(1), mh + 13, RoofMat.of("spruce"), 0, false);
        Arch.pillars(g, tower.grow(1), mh + 11, mh + 12, "minecraft:spruce_fence");
        Arch.door(g, tower.cx(), mh, tower.z1(), "spruce", B.S);
        for (int i = 0; i < mh; i++) {
            g.set(0, i, tower.z1() + 1 + (mh - i) + 2, B.stairs("spruce", B.N, false));
        }
        House.build(g, House.spec().walls("spruce_planks", "spruce_log[axis=y]").roof(RoofType.STEEP, "spruce"), Math.max(7, w / 5), seed,
                w / 3, 0, w / 4);
        return g.freeze();
    }

    static VoxelModel trebuchet(int length) {
        VoxelGrid g = new VoxelGrid();
        int l = Math.max(8, length);
        int h = l * 2 / 3;
        String wood = "minecraft:dark_oak_log[axis=z]";
        g.fill(-l / 4, 1, -l / 2, -l / 4, 1, l / 2, wood);
        g.fill(l / 4, 1, -l / 2, l / 4, 1, l / 2, wood);
        g.fill(-l / 4, 1, -l / 2, l / 4, 1, -l / 2, "minecraft:dark_oak_log[axis=x]");
        g.fill(-l / 4, 1, l / 2, l / 4, 1, l / 2, "minecraft:dark_oak_log[axis=x]");
        for (int side = -1; side <= 1; side += 2) {
            Arch.line(g, side * l / 4, 1, -l / 3, side * l / 4, h, 0, "minecraft:dark_oak_planks");
            Arch.line(g, side * l / 4, 1, l / 3, side * l / 4, h, 0, "minecraft:dark_oak_planks");
        }
        g.fill(-l / 4, h, 0, l / 4, h, 0, "minecraft:dark_oak_log[axis=x]");
        Arch.line(g, 0, h, 0, 0, h + l / 3, -l / 2, "minecraft:stripped_dark_oak_log[axis=z]");
        Arch.line(g, 0, h, 0, 0, h - l / 6, l / 4, "minecraft:stripped_dark_oak_log[axis=z]");
        Rect cw = new Rect(-1, l / 4 - 1, 1, l / 4 + 1);
        g.fill(cw.x0(), h - l / 6 - 3, cw.z0(), cw.x1(), h - l / 6 - 1, cw.z1(), "minecraft:cobblestone");
        Arch.line(g, 0, h + l / 3, -l / 2, 0, 2, -l / 2 + 1, "minecraft:chain[axis=y]");
        g.set(0, 2, -l / 2 + 2, "minecraft:stone");
        return g.freeze();
    }

    static VoxelModel tournament(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(25, width);
        int d = Math.max(13, w / 2);
        Rect field = Rect.centered(w, d);
        Arch.floor(g, field, 0, "minecraft:coarse_dirt");
        g.fill(field.x0() + 2, 1, 0, field.x1() - 2, 1, 0, "minecraft:oak_fence");
        for (int side = -1; side <= 1; side += 2) {
            int zEdge = side < 0 ? field.z0() - 1 : field.z1() + 1;
            for (int row = 0; row < 4; row++) {
                int z = zEdge + side * row;
                g.fill(field.x0() + 3, row, z, field.x1() - 3, row, z, B.stairs("spruce", side < 0 ? B.N : B.S, false));
                g.fill(field.x0() + 3, 0, z, field.x1() - 3, row - 1 < 0 ? 0 : row - 1, z, "minecraft:spruce_planks");
            }
            int zt = zEdge + side * 4;
            String color = side < 0 ? "red" : "blue";
            for (int x = field.x0() + 3; x <= field.x1() - 3; x++) {
                g.set(x, 4, zt, "minecraft:spruce_fence");
                g.set(x, 6, zt - side, B.wool(((x & 1) == 0) ? color : "white"));
                g.set(x, 6, zt - side * 2, B.wool(((x & 1) == 0) ? color : "white"));
            }
            for (int x = field.x0() + 3; x <= field.x1() - 3; x += 4) {
                g.fill(x, 4, zt, x, 5, zt, "minecraft:spruce_fence");
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            int x = side * (w / 2 + 3);
            Arch.cylinder(g, x, 0, 0, 3, 5, "minecraft:white_wool", "minecraft:grass_block", 0);
            Arch.cone(g, x, 5, 0, 4, 5, B.wool(side < 0 ? "red" : "blue"));
        }
        return g.freeze();
    }
}
