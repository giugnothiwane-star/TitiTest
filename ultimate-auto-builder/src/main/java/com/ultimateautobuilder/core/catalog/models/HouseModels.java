package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.House;
import com.ultimateautobuilder.core.generator.House.Feature;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

import java.util.function.Supplier;

/** 45 maisons et villas (générateur architectural + quelques habitats spéciaux). */
public final class HouseModels {
    private HouseModels() {
    }

    private static void house(Reg r, String id, String name, int complexity, Supplier<House.Spec> spec, String... tags) {
        r.gen(id, name, complexity, Reg.HOUSE, p -> House.generate(spec.get(), p.size(), p.seed()), tags);
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.HOUSES);
        house(r, "maison_bois", "Petite maison en bois", 2, () -> House.spec(), "maison", "bois", "cabane", "debutant");
        house(r, "maison_pierre", "Petite maison en pierre", 2, () -> House.spec().walls("cobblestone", "stone_bricks")
                .roof(RoofType.GABLE, "dark_oak").trim("stone_bricks").with(Feature.CHIMNEY), "maison", "pierre");
        house(r, "maison_village", "Maison de village", 2, () -> House.spec().walls("oak_planks", "oak_log[axis=y]").floors(2)
                .roof(RoofType.GABLE, "oak").with(Feature.PORCH, Feature.LANTERNS), "maison", "village", "pnj");
        house(r, "maison_moderne", "Maison moderne", 3, () -> modern().floors(2).with(Feature.TERRACE), "maison", "moderne", "beton");
        house(r, "maison_moderne_piscine", "Maison moderne avec piscine", 4, () -> modern().floors(2).with(Feature.POOL, Feature.TERRACE),
                "maison", "moderne", "piscine");
        house(r, "villa_luxe", "Villa de luxe", 5, () -> House.spec().walls("quartz_block", "quartz_pillar[axis=y]")
                .foundation("smooth_stone").floorMat("smooth_quartz").trim("chiseled_quartz_block").window("glass_pane").door("birch")
                .furniture("birch").colors("light_blue", "light_blue").roof(RoofType.HIP, "smooth_quartz").floors(2).depth(0.7)
                .with(Feature.POOL, Feature.GARDEN, Feature.BALCONY, Feature.ROOMS, Feature.LANTERNS), "villa", "luxe", "piscine", "riche");
        house(r, "villa_contemporaine", "Villa contemporaine", 5, () -> modern().walls("white_concrete", "dark_oak_log[axis=y]").floors(2)
                .with(Feature.WING, Feature.POOL, Feature.TERRACE, Feature.ROOMS), "villa", "contemporaine", "moderne");
        house(r, "maison_japonaise", "Maison japonaise", 4, () -> House.spec().walls("white_terracotta", "dark_oak_log[axis=y]")
                .trim("stripped_dark_oak_log[axis=x]").foundation("stone_bricks").floorMat("bamboo_planks").door("bamboo").furniture("bamboo")
                .colors("green", "white").roof(RoofType.PAGODA, "deepslate_tile").overhang(2).spacing(2).window("white_stained_glass_pane")
                .with(Feature.GARDEN, Feature.LANTERNS), "maison", "japon", "asie", "zen");
        house(r, "maison_traditionnelle", "Maison traditionnelle", 3, () -> House.spec().walls("bricks", "stone_bricks").trim("stone_bricks")
                .roof(RoofType.GABLE, "deepslate_tile").floors(2).with(Feature.CHIMNEY, Feature.GARDEN), "maison", "traditionnelle", "briques");
        house(r, "maison_medievale", "Maison médiévale", 3, () -> House.spec().walls("white_terracotta", "dark_oak_log[axis=y]")
                .trim("dark_oak_log[axis=x]").foundation("cobblestone").roof(RoofType.STEEP, "dark_oak").floors(2).furniture("dark_oak")
                .with(Feature.CHIMNEY), "maison", "medieval", "colombages");
        house(r, "maison_montagne", "Maison de montagne", 3, () -> House.spec().walls("spruce_log[axis=y]", "stripped_spruce_log[axis=y]")
                .foundation("cobblestone").roof(RoofType.GABLE, "spruce").furniture("spruce").door("spruce").floors(2)
                .with(Feature.CHIMNEY, Feature.PORCH), "maison", "montagne", "rondins");
        house(r, "chalet_enneige", "Chalet enneigé", 3, () -> House.spec().walls("spruce_planks", "spruce_log[axis=y]")
                .foundation("stone_bricks").roof(RoofType.STEEP, "dark_oak").furniture("spruce").door("spruce").floors(2)
                .colors("white", "blue").with(Feature.SNOW, Feature.CHIMNEY, Feature.BALCONY), "chalet", "neige", "hiver", "ski");
        r.gen("maison_arbre", "Maison dans un arbre", 4, Reg.HOUSE, p -> treeHouse(p.size(), p.seed()), "maison", "arbre", "cabane", "foret");
        house(r, "maison_souterraine", "Maison souterraine", 3, () -> House.spec().walls("stone_bricks", "polished_andesite")
                .foundation("stone_bricks").floorMat("polished_andesite").with(Feature.UNDERGROUND), "maison", "souterraine", "bunker");
        house(r, "maison_futuriste", "Maison futuriste", 4, () -> House.spec().walls("white_concrete", "light_gray_concrete")
                .foundation("smooth_quartz").floorMat("white_concrete").trim("light_blue_concrete").window("light_blue_stained_glass")
                .door("iron").furniture("birch").colors("light_blue", "cyan").roof(RoofType.DOME, "smooth_quartz").floors(2)
                .with(Feature.BIG_WINDOWS, Feature.TERRACE), "maison", "futuriste", "science fiction");
        house(r, "maison_cyberpunk", "Maison cyberpunk", 4, () -> House.spec().walls("black_concrete", "purple_concrete")
                .foundation("polished_blackstone").floorMat("gray_concrete").trim("magenta_stained_glass").window("cyan_stained_glass")
                .door("iron").furniture("crimson").colors("magenta", "purple").roof(RoofType.FLAT, "polished_blackstone").floors(3)
                .with(Feature.BIG_WINDOWS, Feature.LANTERNS), "maison", "cyberpunk", "neon");
        house(r, "maison_pilotis", "Maison sur pilotis", 3, () -> House.spec().walls("jungle_planks", "jungle_log[axis=y]")
                .roof(RoofType.GABLE, "acacia").furniture("jungle").door("jungle").with(Feature.STILTS, Feature.PORCH), "maison", "pilotis", "lac");
        house(r, "maison_flottante", "Maison flottante", 4, () -> House.spec().walls("birch_planks", "birch_log[axis=y]")
                .roof(RoofType.HIP, "spruce").furniture("birch").door("birch").with(Feature.FLOATING), "maison", "flottante", "ciel", "ile");
        house(r, "maison_plage", "Maison de plage", 3, () -> House.spec().walls("light_blue_terracotta", "birch_log[axis=y]")
                .foundation("smooth_sandstone").floorMat("birch_planks").roof(RoofType.GABLE, "birch").furniture("birch").door("birch")
                .colors("yellow", "light_blue").with(Feature.PORCH, Feature.TERRACE), "maison", "plage", "mer", "vacances");
        house(r, "maison_campagne", "Maison de campagne", 3, () -> House.spec().walls("cobblestone", "oak_log[axis=y]").floors(2)
                .roof(RoofType.GABLE, "spruce").with(Feature.PORCH, Feature.GARDEN, Feature.FENCE, Feature.CHIMNEY), "maison", "campagne", "ferme");
        house(r, "manoir", "Manoir", 5, () -> House.spec().walls("dark_oak_planks", "stone_bricks").foundation("stone_bricks")
                .trim("stone_bricks").roof(RoofType.MANSARD, "deepslate_tile").furniture("dark_oak").door("dark_oak").floors(3).depth(0.7)
                .colors("red", "red").with(Feature.WING, Feature.TOWER, Feature.GARDEN, Feature.FENCE, Feature.ROOMS, Feature.CHIMNEY),
                "manoir", "hante", "riche");
        house(r, "palais", "Palais", 5, () -> House.spec().walls("quartz_bricks", "gold_block").foundation("smooth_quartz")
                .floorMat("polished_diorite").trim("gold_block").window("yellow_stained_glass_pane").door("birch").furniture("birch")
                .colors("purple", "purple").roof(RoofType.DOME, "quartz").floors(3).depth(0.6)
                .with(Feature.WING, Feature.TOWER, Feature.GARDEN, Feature.ROOMS, Feature.LANTERNS), "palais", "royal", "or");
        house(r, "maison_minimaliste", "Maison minimaliste", 2, () -> House.spec().walls("smooth_stone", "smooth_stone")
                .foundation("smooth_stone").floorMat("birch_planks").trim("smooth_stone").roof(RoofType.FLAT, "smooth_quartz").spacing(6)
                .furniture("birch").door("birch").colors("white", "white"), "maison", "minimaliste", "epure");
        house(r, "maison_verre", "Maison en verre", 3, () -> House.spec().walls("glass", "iron_block").foundation("smooth_stone")
                .floorMat("polished_andesite").trim("iron_block").window("glass").roof(RoofType.FLAT, "smooth_quartz").furniture("birch")
                .door("iron").colors("white", "black"), "maison", "verre", "transparente");
        house(r, "maison_quartz", "Maison en quartz", 3, () -> House.spec().walls("quartz_bricks", "quartz_pillar[axis=y]")
                .foundation("smooth_quartz").floorMat("smooth_quartz").trim("chiseled_quartz_block").roof(RoofType.HIP, "quartz")
                .furniture("birch").door("birch").floors(2), "maison", "quartz", "blanche");
        house(r, "maison_cuivre", "Maison en cuivre", 3, () -> House.spec().walls("cut_copper", "oxidized_cut_copper")
                .foundation("polished_deepslate").trim("oxidized_cut_copper").roof(RoofType.GABLE, "oxidized_cut_copper")
                .door("iron").furniture("spruce").floors(2), "maison", "cuivre", "steampunk");
        house(r, "maison_survie", "Maison de survie", 2, () -> House.spec().walls("cobblestone", "oak_log[axis=y]").roof(RoofType.GABLE, "oak")
                .with(Feature.FENCE, Feature.GARDEN), "maison", "survie", "debutant", "minecraft");
        house(r, "maison_fortifiee", "Maison fortifiée", 4, () -> House.spec().walls("stone_bricks", "polished_andesite")
                .foundation("cobblestone").trim("stone_bricks").window("iron_bars").roof(RoofType.FLAT, "stone_brick").floors(2)
                .furniture("dark_oak").door("iron").with(Feature.TOWER), "maison", "fortifiee", "defense");
        house(r, "maison_jardin", "Maison avec jardin", 3, () -> House.spec().walls("birch_planks", "oak_log[axis=y]")
                .roof(RoofType.HIP, "dark_oak").with(Feature.GARDEN, Feature.FENCE, Feature.PORCH), "maison", "jardin", "fleurs");
        house(r, "maison_garage", "Maison avec garage", 3, () -> modern().floors(2).with(Feature.GARAGE), "maison", "garage", "voiture");
        house(r, "maison_colombages", "Maison alsacienne à colombages", 3, () -> House.spec().walls("yellow_terracotta", "dark_oak_log[axis=y]")
                .trim("dark_oak_log[axis=x]").roof(RoofType.STEEP, "brick").floors(3).furniture("dark_oak").colors("red", "red")
                .with(Feature.LANTERNS), "maison", "alsace", "colombages");
        house(r, "cabane_pecheur", "Cabane de pêcheur", 2, () -> House.spec().walls("spruce_planks", "stripped_spruce_log[axis=y]")
                .roof(RoofType.GABLE, "spruce").furniture("spruce").door("spruce").with(Feature.STILTS), "cabane", "peche", "lac");
        r.gen("igloo", "Igloo", 2, Reg.HOUSE, p -> igloo(p.size()), "maison", "igloo", "neige", "glace");
        r.gen("yourte", "Yourte", 2, Reg.HOUSE, p -> yurt(p.size()), "maison", "yourte", "nomade", "tente");
        r.gen("maison_hobbit", "Maison de hobbit", 4, Reg.HOUSE, p -> hobbit(p.size(), p.seed()), "maison", "hobbit", "colline", "terrier");
        house(r, "maison_briques", "Maison en briques", 3, () -> House.spec().walls("bricks", "stone_bricks").trim("stone_bricks")
                .roof(RoofType.HIP, "deepslate_tile").floors(2).with(Feature.CHIMNEY), "maison", "briques", "anglaise");
        house(r, "ferme_grange", "Maison de ferme et grange", 3, () -> House.spec().walls("red_terracotta", "white_concrete")
                .trim("white_concrete").roof(RoofType.STEEP, "dark_oak").door("spruce").furniture("spruce").floors(2).depth(1.2)
                .with(Feature.GARDEN, Feature.FENCE), "ferme", "grange", "campagne");
        house(r, "maison_victorienne", "Maison victorienne", 4, () -> House.spec().walls("light_blue_terracotta", "white_concrete")
                .trim("white_concrete").roof(RoofType.STEEP, "deepslate_tile").floors(3).furniture("dark_oak").door("dark_oak")
                .colors("purple", "purple").with(Feature.TOWER, Feature.PORCH, Feature.CHIMNEY), "maison", "victorienne", "tourelle");
        house(r, "maison_mediterraneenne", "Maison méditerranéenne", 3, () -> House.spec().walls("white_terracotta", "smooth_sandstone")
                .foundation("smooth_sandstone").trim("smooth_sandstone").roof(RoofType.HIP, "red_sandstone").floors(2).furniture("acacia")
                .door("acacia").colors("orange", "blue").with(Feature.TERRACE, Feature.GARDEN), "maison", "mediterranee", "provence");
        house(r, "maison_adobe", "Maison en adobe", 2, () -> House.spec().walls("packed_mud", "stripped_spruce_log[axis=y]")
                .foundation("mud_bricks").trim("mud_bricks").roof(RoofType.FLAT, "mud_brick").furniture("acacia").door("acacia")
                .colors("orange", "orange"), "maison", "adobe", "desert", "mexique");
        house(r, "ranch", "Ranch", 3, () -> House.spec().walls("spruce_planks", "stripped_oak_log[axis=y]").roof(RoofType.GABLE, "spruce")
                .depth(0.45).furniture("spruce").door("spruce").with(Feature.PORCH, Feature.FENCE, Feature.WING), "ranch", "western", "cowboy");
        house(r, "bungalow", "Bungalow", 2, () -> House.spec().walls("white_concrete", "spruce_log[axis=y]").roof(RoofType.HIP, "dark_oak")
                .depth(0.7).with(Feature.PORCH, Feature.GARDEN), "maison", "bungalow", "plain pied");
        house(r, "maison_conteneur", "Maison conteneur", 3, () -> House.spec().walls("orange_concrete", "iron_block")
                .foundation("gray_concrete").floorMat("spruce_planks").trim("light_gray_concrete").roof(RoofType.FLAT, "polished_andesite")
                .door("iron").floors(2).spacing(4).with(Feature.TERRACE), "maison", "conteneur", "industrielle");
        r.gen("maison_champignon", "Maison champignon", 3, Reg.HOUSE, p -> mushroomHouse(p.size()), "maison", "champignon", "fantastique");
        house(r, "maison_ecologique", "Maison écologique", 3, () -> House.spec().walls("spruce_planks", "stripped_spruce_log[axis=y]")
                .roof(RoofType.GREEN, "spruce").floors(2).with(Feature.BIG_WINDOWS, Feature.GARDEN, Feature.SOLAR), "maison", "ecologique", "verte");
        house(r, "maison_tour", "Maison à tourelle", 3, () -> House.spec().walls("stone_bricks", "cobblestone").roof(RoofType.GABLE, "dark_oak")
                .floors(2).with(Feature.TOWER, Feature.CHIMNEY), "maison", "tourelle", "conte");
        house(r, "duplex", "Duplex de ville", 3, () -> House.spec().walls("bricks", "white_concrete").trim("white_concrete")
                .roof(RoofType.FLAT, "stone_brick").floors(3).depth(0.6).with(Feature.BALCONY, Feature.ROOMS), "maison", "duplex", "ville");
        house(r, "tiny_house", "Tiny house", 2, () -> House.spec().walls("birch_planks", "dark_oak_log[axis=y]").roof(RoofType.SHED, "dark_oak")
                .depth(0.5).furniture("birch").door("birch").with(Feature.TERRACE), "maison", "tiny", "mini");
    }

    static House.Spec modern() {
        return House.spec().walls("white_concrete", "light_gray_concrete").foundation("smooth_stone").floorMat("birch_planks")
                .trim("gray_concrete").window("glass").door("iron").furniture("birch").colors("gray", "white")
                .roof(RoofType.FLAT, "smooth_quartz").with(Feature.BIG_WINDOWS);
    }

    // ------------------------------------------------------------------ habitats spéciaux

    static VoxelModel treeHouse(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int h = Math.max(6, width * 2 / 3);
        int tr = Math.max(1, width / 8);
        Arch.cylinder(g, 0, 0, 0, tr, h + 2, "minecraft:oak_log[axis=y]", "minecraft:oak_log[axis=y]", 1);
        int pw = width + 2;
        Rect plat = Rect.centered(pw, pw);
        Arch.floor(g, plat, h, "minecraft:oak_planks");
        for (int x = plat.x0(); x <= plat.x1(); x++) {
            for (int z = plat.z0(); z <= plat.z1(); z++) {
                if (plat.onEdge(x, z)) {
                    g.set(x, h + 1, z, "minecraft:oak_fence");
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            int sx = (i & 1) == 0 ? plat.x0() + 1 : plat.x1() - 1;
            int sz = (i & 2) == 0 ? plat.z0() + 1 : plat.z1() - 1;
            Arch.line(g, sx, h - 1, sz, (int) Math.signum(sx) * tr, h - 1 - pw / 2, (int) Math.signum(sz) * tr, "minecraft:oak_log[axis=y]");
        }
        House.Spec s = House.spec().roof(RoofType.GABLE, "spruce");
        House.build(g, s, Math.max(5, width - 2), seed, 0, h + 1, -1);
        for (int y = 1; y < h; y++) {
            g.set(0, y, tr + 1, "minecraft:ladder[facing=south]");
        }
        g.set(0, h, tr + 1, B.AIR);
        int cr = Math.max(4, width / 2 + 2);
        int cy = h + Math.max(8, width / 2 + 5);
        for (int i = 0; i < 6; i++) {
            int dx = rng.range(-cr / 2, cr / 2), dz = rng.range(-cr / 2, cr / 2);
            int rr = Math.max(3, cr - rng.nextInt(3));
            for (int y = -rr / 2; y <= rr / 2; y++) {
                for (int x = -rr; x <= rr; x++) {
                    for (int z = -rr; z <= rr; z++) {
                        if (x * x + z * z + 4 * y * y <= rr * rr && !g.isSet(dx + x, cy + y, dz + z)) {
                            g.set(dx + x, cy + y, dz + z, B.leaves("oak"));
                        }
                    }
                }
            }
        }
        g.fill(0, h + 2, 0, 0, cy, 0, "minecraft:oak_log[axis=y]");
        return g.freeze();
    }

    static VoxelModel igloo(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(3, width / 2);
        int ry = Math.max(3, r * 3 / 4);
        Arch.disk(g, 0, 0, 0, r, "minecraft:snow_block");
        Arch.dome(g, 0, 1, 0, r, ry, r, "minecraft:snow_block", true);
        Arch.disk(g, 0, 1, 0, r - 1.2, "minecraft:white_carpet");
        // Tunnel d'entrée
        int tl = Math.max(2, r / 2);
        for (int z = r - 1; z <= r + tl; z++) {
            g.fill(-2, 1, z, 2, 3, z, "minecraft:snow_block");
            g.fill(-1, 1, z, 1, 2, z, B.AIR);
        }
        g.set(0, 2, r - 1, B.AIR);
        g.set(-r + 2, 1, 0, "minecraft:furnace[facing=east,lit=true]");
        g.set(r - 2, 1, 0, B.bedHead("light_blue", B.E));
        g.set(r - 3, 1, 0, B.bedFoot("light_blue", B.E));
        g.set(0, ry - 1, 0, B.lantern(true));
        for (int x = -r + 1; x <= r - 1; x += Math.max(2, r - 1)) {
            g.set(x, ry / 2 + 1, -r + 1, "minecraft:ice");
        }
        return g.freeze();
    }

    static VoxelModel yurt(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(3, width / 2);
        int wall = Math.max(3, r / 2 + 2);
        Arch.cylinder(g, 0, 0, 0, r, wall + 1, "minecraft:white_wool", "minecraft:spruce_planks", 0);
        Arch.disk(g, 0, 0, 0, r, "minecraft:spruce_planks");
        for (int y = 1; y <= wall; y += 2) {
            Arch.ring(g, 0, y, 0, r, "minecraft:red_wool");
        }
        Arch.cone(g, 0, wall + 1, 0, r + 1, Math.max(2, r * 2 / 3), "minecraft:light_gray_wool");
        g.set(0, wall + 2 + Math.max(2, r * 2 / 3), 0, "minecraft:spruce_fence");
        Arch.door(g, 0, 1, r, "spruce", B.S);
        Arch.disk(g, 0, 1, 0, Math.max(1, r - 2), "minecraft:orange_carpet");
        g.set(0, 1, 0, "minecraft:campfire[lit=false,signal_fire=false,facing=north]");
        g.set(-r + 1, 1, -1, B.bedHead("brown", B.N));
        g.set(-r + 1, 1, 0, B.bedFoot("brown", B.N));
        g.set(r - 1, 1, 0, "minecraft:chest[facing=west,type=single]");
        return g.freeze();
    }

    static VoxelModel hobbit(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(4, width / 2 + 1);
        int h = Math.max(4, r * 2 / 3 + 1);
        Arch.dome(g, 0, 0, 0, r + 2, h + 1, r + 2, "minecraft:dirt", false);
        Arch.dome(g, 0, 0, 0, r + 2, h + 1, r + 2, "minecraft:grass_block", true);
        Arch.dome(g, 0, 0, 0, r, h - 1, r, "minecraft:air", false);
        Arch.disk(g, 0, 0, 0, r, "minecraft:oak_planks");
        Arch.dome(g, 0, 1, 0, r, h - 2, r, "minecraft:stripped_oak_log[axis=y]", true);
        Arch.dome(g, 0, 1, 0, r - 1, h - 3, r - 1, "minecraft:air", false);
        // Porte ronde
        int dz = r + 2;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 1; dy <= 4; dy++) {
                double d = Math.sqrt(dx * dx + (dy - 2.0) * (dy - 2.0));
                for (int z = r - 1; z <= dz; z++) {
                    if (d <= 2.2) {
                        g.set(dx, dy, z, B.AIR);
                    }
                }
                if (d > 1.6 && d <= 2.6) {
                    g.set(dx, dy, dz, "minecraft:stone_bricks");
                }
            }
        }
        g.fill(-1, 1, dz, 1, 3, dz, "minecraft:green_concrete");
        g.set(0, 2, dz, B.AIR);
        g.set(1, 2, dz + 1, "minecraft:yellow_stained_glass_pane");
        g.fill(-1, 1, dz, 1, 2, dz, B.AIR);
        g.set(0, 1, dz, B.doorLower("oak", B.S));
        g.set(0, 2, dz, B.doorUpper("oak", B.S));
        for (int x = -1; x <= 1; x++) {
            for (int z = dz + 1; z <= dz + 6; z++) {
                g.set(x, 0, z, x == 0 ? "minecraft:dirt_path" : "minecraft:grass_block");
            }
        }
        g.set(-2, 1, dz + 3, "minecraft:oak_fence");
        g.set(-2, 2, dz + 3, B.lantern(false));
        g.set(2, 1, dz + 3, "minecraft:poppy");
        g.set(0, h - 2, 0, B.lantern(true));
        g.set(r - 2, 1, 0, "minecraft:furnace[facing=west,lit=false]");
        g.set(-r + 2, 1, 0, "minecraft:bookshelf");
        g.set(-r + 2, 2, 0, "minecraft:bookshelf");
        g.fill(r, 1, -1, r + 3, h, -1, "minecraft:bricks");
        g.fill(2, h + 1, -2, 2, h + 4, -2, "minecraft:bricks");
        g.set(2, h + 5, -2, "minecraft:campfire[lit=true,signal_fire=false,facing=north]");
        return g.freeze();
    }

    static VoxelModel mushroomHouse(int width) {
        VoxelGrid g = new VoxelGrid();
        int sr = Math.max(2, width / 4);
        int sh = Math.max(5, width / 2 + 3);
        Arch.cylinder(g, 0, 0, 0, sr + 1, sh, "minecraft:mushroom_stem", "minecraft:oak_planks", 4);
        int cr = Math.max(4, width / 2 + 1);
        Arch.dome(g, 0, sh, 0, cr, Math.max(3, cr * 2 / 3), cr, "minecraft:red_mushroom_block", false);
        for (int i = 0; i < 12; i++) {
            double a = i * 2.39996;
            double rr = cr * (0.3 + 0.6 * ((i * 37) % 10) / 10.0);
            int x = (int) Math.round(Math.cos(a) * rr), z = (int) Math.round(Math.sin(a) * rr);
            for (int y = sh + cr; y >= sh; y--) {
                if (g.isSet(x, y, z)) {
                    g.set(x, y, z, "minecraft:white_concrete");
                    break;
                }
            }
        }
        g.fill(-cr, sh, -cr, cr, sh, cr, B.AIR);
        Arch.disk(g, 0, sh, 0, cr, "minecraft:mushroom_stem");
        Arch.door(g, 0, 1, sr + 1, "oak", B.S);
        g.set(sr + 1, 3, 0, "minecraft:glass_pane");
        g.set(-sr - 1, 3, 0, "minecraft:glass_pane");
        g.set(0, sh - 1, 0, B.lantern(true));
        for (int y = 1; y < sh; y++) {
            g.set(-sr, y, 0, "minecraft:ladder[facing=east]");
        }
        return g.freeze();
    }
}
