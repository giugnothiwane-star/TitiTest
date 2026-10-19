package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.City;
import com.ultimateautobuilder.core.generator.House;
import com.ultimateautobuilder.core.generator.House.Feature;
import com.ultimateautobuilder.core.generator.RoofMat;
import com.ultimateautobuilder.core.generator.Tower;
import com.ultimateautobuilder.core.generator.Tower.Crown;
import com.ultimateautobuilder.core.generator.Trees;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

import java.util.function.Supplier;

/** 35 ensembles urbains et bâtiments de ville. */
public final class CityModels {
    private CityModels() {
    }

    private static void city(Reg r, String id, String name, int cx, Supplier<City.Spec> spec, String... tags) {
        r.gen(id, name, cx, Reg.CITY, p -> City.generate(spec.get(), p.size(), p.seed()), tags);
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.CITIES);
        city(r, "quartier_residentiel", "Quartier résidentiel", 5, () -> City.spec().block(24, 7)
                .lot(City.houses(House.spec(), HouseModels.modern(), House.spec().walls("bricks", "stone_bricks").roof(RoofType.HIP, "deepslate_tile")), 5)
                .lot(City.park("minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]"), 1), "ville", "residentiel", "lotissement");
        city(r, "centre_ville", "Centre-ville", 5, () -> City.spec().block(22, 7)
                .lot(City.tower(Tower.spec(), 1.5, 3.0), 4)
                .lot(City.tower(Tower.spec().colors("bricks", "glass", "stone_bricks").crown(Crown.FLAT).balconies(), 0.8, 1.5), 3)
                .lot(City.plaza("minecraft:polished_andesite"), 1)
                .lot(City.park("minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]"), 1), "ville", "centre", "immeubles");
        city(r, "ville_medievale", "Petite ville médiévale", 5, () -> City.spec().road("cobblestone", null, "stone_bricks").noMarkings()
                .lamps("oak_fence", "lantern[hanging=false]").block(20, 5)
                .lot(City.house(House.spec().walls("white_terracotta", "dark_oak_log[axis=y]").trim("dark_oak_log[axis=x]")
                        .roof(RoofType.STEEP, "dark_oak").floors(2)), 4)
                .lot(City.house(House.spec().walls("cobblestone", "oak_log[axis=y]").roof(RoofType.STEEP, "spruce").with(Feature.CHIMNEY)), 3)
                .lot(paste(p -> CastleModels.market(p, 1L)), 1)
                .lot(paste(p -> CastleModels.forge(p)), 1), "ville", "medieval", "bourg");
        city(r, "ville_futuriste", "Ville futuriste", 5, () -> City.spec().road("white_concrete", "light_blue_concrete", "smooth_quartz")
                .lamps("end_rod[facing=up]", "sea_lantern").ground("smooth_quartz").block(22, 7)
                .lot(City.tower(Tower.spec().colors("white_concrete", "light_blue_stained_glass", "smooth_quartz").crown(Crown.DOME)
                        .light("sea_lantern"), 2.0, 4.0), 4)
                .lot(City.tower(Tower.spec().colors("smooth_quartz", "cyan_stained_glass", "white_concrete").crown(Crown.SPIRE).setbacks(2), 2.5, 4.5), 3)
                .lot(City.park("minecraft:birch_log[axis=y]", "minecraft:flowering_azalea_leaves[persistent=true]"), 1), "ville", "futur", "science fiction");
        city(r, "ville_cyberpunk", "Ville cyberpunk", 5, () -> City.spec().road("black_concrete", "magenta_concrete", "polished_blackstone")
                .lamps("iron_bars", "pearlescent_froglight[axis=y]").ground("polished_blackstone").block(20, 7)
                .lot(City.tower(Tower.spec().colors("black_concrete", "magenta_stained_glass", "purple_concrete").crown(Crown.ANTENNA)
                        .sign("cyan").light("verdant_froglight[axis=y]"), 2.0, 4.5), 4)
                .lot(City.tower(Tower.spec().colors("gray_concrete", "cyan_stained_glass", "black_concrete").sign("magenta").crown(Crown.SPIRE),
                        1.5, 3.0), 3)
                .lot(City.parking(), 1), "ville", "cyberpunk", "neon", "nuit");
        city(r, "village_japonais", "Village japonais", 4, () -> City.spec().road("gravel", null, "stone_bricks").noMarkings()
                .lamps("stone_brick_wall", "lantern[hanging=false]").block(20, 5)
                .lot(City.house(House.spec().walls("white_terracotta", "dark_oak_log[axis=y]").trim("stripped_dark_oak_log[axis=x]")
                        .roof(RoofType.PAGODA, "deepslate_tile").overhang(2).door("bamboo").furniture("bamboo").window("white_stained_glass_pane")), 4)
                .lot(paste(p -> DungeonModels.pagoda(Math.max(9, p - 6))), 1)
                .lot(City.park("minecraft:cherry_log[axis=y]", "minecraft:cherry_leaves[persistent=true]"), 2), "village", "japon", "sakura");
        city(r, "village_enneige", "Village enneigé", 4, () -> City.spec().road("snow_block", null, "packed_ice").noMarkings().ground("snow_block")
                .lamps("spruce_fence", "lantern[hanging=false]").block(20, 5)
                .lot(City.house(House.spec().walls("spruce_planks", "spruce_log[axis=y]").roof(RoofType.STEEP, "dark_oak").furniture("spruce")
                        .door("spruce").with(Feature.SNOW, Feature.CHIMNEY)), 4)
                .lot(City.park("minecraft:spruce_log[axis=y]", "minecraft:spruce_leaves[persistent=true]"), 2), "village", "neige", "hiver", "noel");
        city(r, "village_desertique", "Village désertique", 4, () -> City.spec().road("sand", null, "smooth_sandstone").noMarkings().ground("sand")
                .lamps("sandstone_wall", "lantern[hanging=false]").block(18, 5)
                .lot(City.house(House.spec().walls("sandstone", "cut_sandstone").foundation("smooth_sandstone").trim("smooth_sandstone")
                        .roof(RoofType.FLAT, "smooth_sandstone").furniture("acacia").door("acacia")), 4)
                .lot(City.house(House.spec().walls("smooth_sandstone", "cut_sandstone").roof(RoofType.DOME, "sandstone").furniture("acacia")
                        .door("acacia")), 1)
                .lot(City.park("minecraft:jungle_log[axis=y]", "minecraft:jungle_leaves[persistent=true]"), 2), "village", "desert", "oasis");
        city(r, "zone_industrielle", "Zone industrielle", 4, () -> City.spec().block(24, 7).ground("gravel")
                .lot(City.warehouse(), 4).lot(City.tanks(), 2).lot(City.parking(), 1), "industrie", "usine", "entrepot");
        r.gen("port", "Port", 5, Reg.CITY, p -> port(p.size(), p.seed()), "port", "bateaux", "quai", "mer");
        r.gen("aeroport", "Aéroport décoratif", 5, Reg.CITY, p -> airport(p.size(), p.seed()), "aeroport", "avion", "piste");
        r.gen("gare", "Gare ferroviaire", 4, Reg.CITY, p -> station(p.size(), p.seed()), "gare", "train", "rails");
        r.gen("reseau_routes", "Réseau de routes", 3, Reg.CITY, p -> roads(p.size(), p.seed()), "routes", "rond point", "carrefour");
        city(r, "quartier_gratte_ciel", "Quartier de gratte-ciel", 5, () -> City.spec().block(22, 7)
                .lot(City.tower(Tower.spec().crown(Crown.ANTENNA).setbacks(2), 3.0, 5.5), 3)
                .lot(City.tower(Tower.spec().colors("black_concrete", "gray_stained_glass", "black_concrete").crown(Crown.HELIPAD), 3.0, 5.0), 2)
                .lot(City.tower(Tower.spec().colors("smooth_quartz", "blue_stained_glass", "quartz_block").crown(Crown.SPIRE), 3.5, 6.0), 2)
                .lot(City.plaza("minecraft:polished_diorite"), 1), "ville", "gratte ciel", "skyline", "manhattan");
        r.gen("centre_commercial", "Centre commercial", 4, Reg.CITY, p -> mall(p.size(), p.seed()), "magasins", "shopping", "parking");
        r.gen("parc_urbain", "Parc urbain", 3, Reg.CITY, p -> urbanPark(p.size(), p.seed()), "parc", "lac", "jardin public");
        city(r, "quartier_maisons_modernes", "Quartier de maisons modernes", 5, () -> City.spec().block(26, 7)
                .lot(City.house(HouseModels.modern().floors(2).with(Feature.POOL)), 3)
                .lot(City.house(HouseModels.modern().walls("white_concrete", "dark_oak_log[axis=y]").with(Feature.GARAGE)), 2)
                .lot(City.park("minecraft:birch_log[axis=y]", "minecraft:birch_leaves[persistent=true]"), 1), "ville", "moderne", "villas");

        SizeSpec BLD = Reg.BUILDING;
        building(r, "gratte_ciel_moderne", "Gratte-ciel moderne", 4, () -> Tower.spec().height(4.5).setbacks(2).crown(Crown.ANTENNA), "gratte ciel", "tour", "bureaux");
        building(r, "gratte_ciel_verre", "Gratte-ciel de verre", 4, () -> Tower.spec().colors("light_blue_stained_glass", "light_blue_stained_glass", "cyan_stained_glass")
                .height(5.0).column(6).crown(Crown.SPIRE), "gratte ciel", "verre", "tour");
        building(r, "tour_bureaux", "Tour de bureaux", 3, () -> Tower.spec().colors("gray_concrete", "black_stained_glass", "light_gray_concrete")
                .height(3.0).crown(Crown.HELIPAD), "bureaux", "tour", "entreprise");
        building(r, "immeuble_appartements", "Immeuble d'appartements", 3, () -> Tower.spec().colors("bricks", "glass", "white_concrete").height(1.6)
                .column(4).crown(Crown.FLAT).balconies(), "immeuble", "appartements", "logement");
        building(r, "hotel", "Hôtel de luxe", 4, () -> Tower.spec().colors("smooth_sandstone", "yellow_stained_glass", "gold_block").height(2.6)
                .crown(Crown.GARDEN).sign("red").balconies(), "hotel", "luxe", "vacances");
        r.gen("hopital", "Hôpital", 4, BLD, p -> hospital(p.size()), "hopital", "medecin", "urgences");
        r.gen("ecole", "École", 3, BLD, p -> school(p.size(), p.seed()), "ecole", "college", "classe");
        r.gen("caserne_pompiers", "Caserne de pompiers", 3, BLD, p -> fireStation(p.size(), p.seed()), "pompiers", "caserne", "camion");
        r.gen("commissariat", "Commissariat de police", 3, BLD, p -> police(p.size(), p.seed()), "police", "commissariat");
        r.gen("mairie", "Mairie", 4, BLD, p -> townHall(p.size()), "mairie", "hotel de ville", "horloge");
        r.gen("bibliotheque", "Bibliothèque", 3, BLD, p -> library(p.size()), "bibliotheque", "livres");
        r.gen("musee", "Musée", 4, BLD, p -> museum(p.size()), "musee", "art", "pyramide de verre");
        r.gen("stade", "Stade de football", 4, SizeSpec.width(31, 61, 91, 151), p -> stadium(p.size()), "stade", "football", "sport");
        r.gen("cinema", "Cinéma", 3, BLD, p -> cinema(p.size(), p.seed()), "cinema", "film");
        r.gen("restaurant", "Restaurant avec terrasse", 2, Reg.HOUSE, p -> restaurant(p.size(), p.seed()), "restaurant", "terrasse", "cafe");
        r.gen("supermarche", "Supermarché", 3, BLD, p -> supermarket(p.size(), p.seed()), "supermarche", "magasin", "courses");
        r.gen("station_service", "Station-service", 2, Reg.HOUSE, p -> gasStation(p.size()), "station", "essence", "voiture");
        r.gen("tour_treillis", "Tour en treillis métallique", 4, SizeSpec.height(30, 60, 120, 240), p -> latticeTower(p.size()), "tour", "eiffel", "paris", "metal");
    }

    private static void building(Reg r, String id, String name, int cx, Supplier<Tower.Spec> spec, String... tags) {
        r.gen(id, name, cx, Reg.BUILDING, p -> {
            VoxelGrid g = new VoxelGrid();
            Tower.Spec s = spec.get();
            Arch.floor(g, Rect.centered(p.size() + 6, p.size() + 6), 0, "minecraft:smooth_stone");
            Tower.build(g, s, p.size(), p.size(), Tower.floorsFor(s, p.size()), 0, 0, 0);
            return g.freeze();
        }, tags);
    }

    /** Insère un modèle autonome au centre d'un îlot. */
    static City.Lot paste(java.util.function.IntFunction<VoxelModel> model) {
        return (g, lot, rng) -> {
            VoxelModel m = model.apply(Math.max(7, lot.width() - 2));
            g.paste(m, lot.cx(), 0, lot.cz());
        };
    }

    // ------------------------------------------------------------------ outils de façade

    /** Péristyle classique : colonnes, fronton triangulaire et marches. */
    static void classical(VoxelGrid g, Rect r, int h, String mat, String column, RoofMat roof) {
        Arch.room(g, r, 0, h, mat, "minecraft:polished_diorite");
        Arch.windows(g, r, 2, h - 3, 3, "minecraft:glass_pane", r.cx());
        Rect porch = new Rect(r.x0(), r.z1() + 1, r.x1(), r.z1() + 4);
        Arch.floor(g, porch, 0, mat);
        for (int x = porch.x0(); x <= porch.x1(); x += 2) {
            g.fill(x, 1, porch.z1(), x, h, porch.z1(), column);
        }
        Arch.air(g, new Rect(porch.x0(), porch.z0(), porch.x1(), porch.z1() - 1), 1, h);
        Arch.floor(g, new Rect(r.x0() - 1, r.z0() - 1, r.x1() + 1, porch.z1() + 1), h + 1, mat);
        Arch.gable(g, new Rect(r.x0(), r.z0(), r.x1(), porch.z1()), h + 2, roof, 1, mat, false, 1);
        for (int x = porch.x0(); x <= porch.x1(); x++) {
            g.set(x, 0, porch.z1() + 1, B.stairs("quartz", B.N, false));
        }
        g.fill(r.cx() - 1, 1, r.z1(), r.cx() + 1, 3, r.z1(), B.AIR);
        Arch.door(g, r.cx(), 1, r.z1(), "dark_oak", B.S);
    }

    // ------------------------------------------------------------------ ensembles spécifiques

    static VoxelModel port(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(48, width);
        int half = w / 2;
        // Bassin au sud, quais au nord
        g.fill(-half, -4, 0, half, -4, half, "minecraft:stone_bricks");
        g.fill(-half, -3, 1, half, 0, half, "minecraft:water[level=0]");
        g.fill(-half, -3, 0, half, 0, 0, "minecraft:stone_bricks");
        g.fill(-half - 1, -3, 0, -half - 1, 0, half, "minecraft:stone_bricks");
        g.fill(half + 1, -3, 0, half + 1, 0, half, "minecraft:stone_bricks");
        g.fill(-half, 0, -half, half, 0, -1, "minecraft:stone_bricks");
        for (int x = -half + 6; x < half - 4; x += 14) {
            g.fill(x, 0, 1, x + 2, 0, half / 2, "minecraft:spruce_planks");
            for (int z = 2; z < half / 2; z += 3) {
                g.fill(x - 1, -3, z, x - 1, 0, z, "minecraft:spruce_log[axis=y]");
                g.fill(x + 3, -3, z, x + 3, 0, z, "minecraft:spruce_log[axis=y]");
            }
            VoxelModel boat = VehicleModels.boat(Math.max(10, half / 3), rng.nextLong());
            g.paste(boat, x + 8, 1, half / 4 + 2);
        }
        City.Lot wh = City.warehouse();
        int lw = Math.max(14, w / 4);
        for (int x = -half + lw / 2; x + lw / 2 <= half; x += lw + 3) {
            wh.fill(g, Rect.centered(lw, lw).translate(x, -half / 2), rng);
        }
        for (int x = -half + 4; x < half; x += 9) {
            g.set(x, 1, -2, "minecraft:barrel[facing=up,open=false]");
            g.fill(x + 2, 1, -3, x + 3, 2, -2, B.concrete(rng.pick(new String[]{"red", "blue", "orange", "green"})));
        }
        VoxelModel lighthouse = MiscModels.lighthouse(Math.max(9, w / 6));
        g.paste(lighthouse, half - 3, 0, half + lighthouse.sizeZ() / 2 + 2);
        return g.freeze();
    }

    static VoxelModel airport(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(48, width);
        int half = w / 2;
        int rw = Math.max(7, w / 8);
        g.fill(-half, 0, -rw, half, 0, rw, "minecraft:black_concrete");
        for (int x = -half + 2; x < half - 2; x += 6) {
            g.fill(x, 0, 0, x + 2, 0, 0, "minecraft:white_concrete");
        }
        for (int z = -rw + 1; z < rw; z += 2) {
            g.fill(-half + 1, 0, z, -half + 3, 0, z, "minecraft:white_concrete");
            g.fill(half - 3, 0, z, half - 1, 0, z, "minecraft:white_concrete");
        }
        for (int x = -half; x <= half; x += 4) {
            g.set(x, 0, -rw, "minecraft:sea_lantern");
            g.set(x, 0, rw, "minecraft:sea_lantern");
        }
        g.fill(-half / 2, 0, rw + 1, half / 2, 0, rw + half / 2, "minecraft:gray_concrete");
        Rect term = new Rect(-half / 2, rw + half / 3, half / 2, rw + half / 3 + Math.max(10, w / 6));
        Arch.room(g, term, 0, 7, "minecraft:white_concrete", "minecraft:polished_diorite");
        Arch.windows(g, term, 1, 6, 2, "minecraft:light_blue_stained_glass", null);
        Arch.dome(g, term.cx(), 8, term.cz(), term.width() / 2, 4, term.depth() / 2, "minecraft:light_blue_stained_glass", true);
        Arch.floor(g, term, 8, "minecraft:white_concrete");
        int tx = term.x1() + 6, tz = term.cz();
        Arch.cylinder(g, tx, 0, tz, 2, 26, "minecraft:white_concrete", "minecraft:white_concrete", 5);
        Arch.cylinder(g, tx, 26, tz, 4, 4, "minecraft:light_blue_stained_glass", "minecraft:white_concrete", 0);
        Arch.disk(g, tx, 30, tz, 4.5, "minecraft:white_concrete");
        g.fill(tx, 31, tz, tx, 35, tz, "minecraft:iron_bars");
        VoxelModel plane = VehicleModels.airliner(Math.max(20, w / 3));
        g.paste(plane, 0, 1, rw + half / 6 + 1);
        return g.freeze();
    }

    static VoxelModel station(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(40, width);
        int half = w / 2;
        // Voies ferrées
        for (int track = 0; track < 3; track++) {
            int z = -8 + track * 6;
            g.fill(-half, 0, z - 1, half, 0, z + 1, "minecraft:gravel");
            for (int x = -half; x <= half; x++) {
                g.set(x, 1, z, "minecraft:rail[shape=east_west]");
            }
            if (track < 2) {
                g.fill(-half / 2, 0, z + 2, half / 2, 1, z + 4, "minecraft:smooth_stone");
                g.fill(-half / 2, 1, z + 2, half / 2, 1, z + 4, "minecraft:smooth_stone_slab[type=bottom]");
                for (int x = -half / 2; x <= half / 2; x += 6) {
                    g.fill(x, 2, z + 3, x, 5, z + 3, "minecraft:iron_bars");
                }
                g.fill(-half / 2, 6, z + 1, half / 2, 6, z + 5, "minecraft:glass");
            }
        }
        Rect hall = new Rect(-half / 2, 6, half / 2, 6 + Math.max(10, w / 4));
        Arch.room(g, hall, 0, 9, "minecraft:bricks", "minecraft:polished_andesite");
        Arch.windows(g, hall, 2, 5, 3, "minecraft:glass_pane", hall.cx());
        Arch.roof(g, RoofType.GABLE, hall, 10, RoofMat.of("deepslate_tile"), 1, "minecraft:bricks");
        Rect clock = Rect.centered(5, 5).translate(0, hall.z1() - 2);
        for (int y = 0; y < 20; y++) {
            Arch.walls(g, clock, y, y, "minecraft:bricks");
        }
        g.set(0, 16, clock.z1(), "minecraft:white_concrete");
        g.set(0, 17, clock.z1(), "minecraft:black_concrete");
        Arch.hip(g, clock, 20, RoofMat.of("deepslate_tile"), 1, false);
        g.fill(-1, 1, hall.z1(), 1, 3, hall.z1(), B.AIR);
        VoxelModel train = VehicleModels.locomotive(Math.max(14, half / 2));
        g.paste(train, -half / 3, 1, -8);
        return g.freeze();
    }

    static VoxelModel roads(int width, long seed) {
        City.Spec s = City.spec().block(20, 7).lot(City.park("minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]"), 1)
                .lot((g, lot, rng) -> {
                    Arch.floor(g, lot, 0, "minecraft:grass_block");
                    for (int x = lot.x0() + 2; x <= lot.x1() - 2; x += 4) {
                        g.set(x, 1, lot.z0() + 2, "minecraft:oak_leaves[persistent=true]");
                        g.set(x, 1, lot.z1() - 2, "minecraft:oak_leaves[persistent=true]");
                    }
                }, 2);
        VoxelGrid g = new VoxelGrid();
        City.build(g, s, width, seed);
        // Rond-point central
        int r = 9;
        Arch.disk(g, 0, 0, 0, r + 4, "minecraft:gray_concrete");
        Arch.ring(g, 0, 0, 0, r + 4, "minecraft:white_concrete");
        Arch.disk(g, 0, 0, 0, r - 2, "minecraft:grass_block");
        Arch.ring(g, 0, 1, 0, r - 2, "minecraft:stone_brick_wall");
        Arch.disk(g, 0, 1, 0, r - 3, B.AIR);
        Trees.oakInto(g, 0, 1, 0, 10, seed, "minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]");
        return g.freeze();
    }

    static VoxelModel mall(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(40, width);
        Rect r = Rect.centered(w * 2 / 3, w / 3).translate(0, -w / 6);
        Arch.room(g, r, 0, 10, "minecraft:white_concrete", "minecraft:polished_diorite");
        Arch.floor(g, r.inset(1), 5, "minecraft:polished_diorite");
        Arch.air(g, Rect.centered(r.width() / 3, r.depth() - 4).translate(r.cx(), r.cz()), 5, 5);
        for (int x = r.x0() + 1; x < r.x1(); x++) {
            if ((x - r.x0()) % 5 != 0) {
                g.fill(x, 1, r.z1(), x, 4, r.z1(), "minecraft:glass");
                g.fill(x, 6, r.z1(), x, 9, r.z1(), "minecraft:light_blue_stained_glass");
            }
        }
        Arch.floor(g, r, 11, "minecraft:light_gray_concrete");
        Arch.dome(g, r.cx(), 11, r.cz(), r.width() / 6, 4, r.depth() / 3, "minecraft:glass", true);
        String[] signs = {"red", "blue", "orange", "green", "magenta", "cyan"};
        Rng rng = new Rng(seed);
        for (int x = r.x0() + 2; x < r.x1() - 3; x += 7) {
            g.fill(x, 10, r.z1() + 1, x + 4, 10, r.z1() + 1, B.concrete(signs[rng.nextInt(signs.length)]));
            for (int sx = x; sx <= x + 4; sx++) {
                for (int y = 1; y <= 4; y++) {
                    if (g.get(sx, y, r.z0() + 2) == null) {
                        g.set(sx, 1, r.z0() + 2, ((sx & 1) == 0) ? "minecraft:barrel[facing=up,open=false]" : "minecraft:chest[facing=south,type=single]");
                    }
                }
            }
        }
        g.fill(r.cx() - 2, 1, r.z1(), r.cx() + 2, 4, r.z1(), B.AIR);
        City.Lot park = City.parking();
        Rect lot = new Rect(-w / 2, r.z1() + 3, w / 2, r.z1() + 3 + w / 3);
        park.fill(g, lot, rng);
        return g.freeze();
    }

    static VoxelModel urbanPark(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(40, width);
        Rect r = Rect.centered(w, w);
        Arch.floor(g, r, 0, "minecraft:grass_block");
        int lr = w / 5;
        for (int x = -lr - 2; x <= lr + 2; x++) {
            for (int z = -lr; z <= lr; z++) {
                double v = (double) x * x / ((lr + 2) * (lr + 2)) + (double) z * z / (lr * lr);
                if (v <= 1) {
                    g.set(x + w / 8, -3, z, "minecraft:clay");
                    g.fill(x + w / 8, -2, z, x + w / 8, 0, z, v > 0.85 ? "minecraft:sand" : "minecraft:water[level=0]");
                    if (v <= 0.85 && rng.chance(0.04)) {
                        g.set(x + w / 8, 1, z, "minecraft:lily_pad");
                    }
                }
            }
        }
        for (int x = r.x0(); x <= r.x1(); x++) {
            int z = (int) Math.round(Math.sin(x / 8.0) * w / 6) - w / 4;
            g.fill(x, 0, z - 1, x, 0, z + 1, "minecraft:dirt_path");
        }
        for (int z = r.z0(); z <= r.z1(); z++) {
            g.fill(-w / 3 - 1, 0, z, -w / 3 + 1, 0, z, "minecraft:dirt_path");
        }
        for (int i = 0; i < w / 4; i++) {
            int x = rng.range(r.x0() + 3, r.x1() - 3), z = rng.range(r.z0() + 3, r.z1() - 3);
            if (Math.abs(x - w / 8) < lr + 4 && Math.abs(z) < lr + 2) {
                continue;
            }
            Trees.oakInto(g, x, 1, z, rng.range(7, 11), rng.nextLong(), "minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]");
        }
        VoxelModel kiosk = MiscModels.gazebo(9);
        g.paste(kiosk, -w / 3 + 7, 0, w / 4);
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int z = r.z0(); z <= r.z1(); z++) {
                if (r.onEdge(x, z) && !((Math.abs(x) <= 1) || (Math.abs(z) <= 1))) {
                    g.set(x, 1, z, "minecraft:iron_bars");
                }
            }
        }
        return g.freeze();
    }

    // ------------------------------------------------------------------ bâtiments publics

    static VoxelModel hospital(int width) {
        VoxelGrid g = new VoxelGrid();
        Tower.Spec s = Tower.spec().colors("white_concrete", "light_blue_stained_glass", "light_gray_concrete").height(1.4).crown(Crown.HELIPAD);
        Arch.floor(g, Rect.centered(width + 8, width + 8), 0, "minecraft:smooth_stone");
        int floors = Tower.floorsFor(s, width);
        int top = Tower.build(g, s, width, width, floors, 0, 0, 0);
        Rect r = Rect.centered(width, width);
        int cy = top - 6, cz = r.z1() + 1;
        g.fill(-1, cy - 3, cz, 1, cy + 3, cz, "minecraft:red_concrete");
        g.fill(-3, cy - 1, cz, 3, cy + 1, cz, "minecraft:red_concrete");
        g.fill(-4, 4, cz + 1, 4, 4, cz + 4, "minecraft:white_concrete");
        g.fill(-4, 1, cz + 4, -4, 3, cz + 4, "minecraft:iron_bars");
        g.fill(4, 1, cz + 4, 4, 3, cz + 4, "minecraft:iron_bars");
        return g.freeze();
    }

    static VoxelModel school(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        House.Spec s = House.spec().walls("bricks", "white_concrete").trim("white_concrete").window("glass_pane").roof(RoofType.HIP, "deepslate_tile")
                .floors(2).depth(0.5).furniture("birch").colors("blue", "blue").spacing(3).with(Feature.ROOMS);
        Rect r = House.build(g, s, Math.max(15, width), seed, 0, 0, 0);
        g.set(0, 9, r.z1() + 1, "minecraft:white_concrete");
        g.set(0, 9, r.z1() + 2, "minecraft:black_concrete");
        Rect yard = new Rect(r.x0(), r.z1() + 3, r.x1(), r.z1() + 3 + r.width() / 2);
        Arch.floor(g, yard, 0, "minecraft:lime_concrete");
        Arch.walls(g, yard, 0, 0, "minecraft:white_concrete");
        g.fill(yard.cx(), 0, yard.z0(), yard.cx(), 0, yard.z1(), "minecraft:white_concrete");
        for (int z : new int[]{yard.z0() + 1, yard.z1() - 1}) {
            g.fill(yard.cx() - 2, 1, z, yard.cx() + 2, 3, z, "minecraft:white_stained_glass_pane");
        }
        for (int x = yard.x0(); x <= yard.x1(); x++) {
            g.set(x, 1, yard.z1() + 1, "minecraft:iron_bars");
        }
        return g.freeze();
    }

    static VoxelModel fireStation(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        House.Spec s = House.spec().walls("red_terracotta", "stone_bricks").trim("stone_bricks").roof(RoofType.FLAT, "stone_brick").floors(2)
                .depth(0.7).with(Feature.ROOMS);
        Rect r = House.build(g, s, Math.max(15, width), seed, 0, 0, 0);
        for (int x = r.x0() + 2; x + 3 < r.x1(); x += 5) {
            if (Math.abs(x + 1 - r.cx()) <= 2) {
                continue;
            }
            g.fill(x, 1, r.z1(), x + 2, 3, r.z1(), "minecraft:red_concrete");
            g.fill(x, 3, r.z1(), x + 2, 3, r.z1(), "minecraft:white_concrete");
            g.fill(x, 0, r.z1() + 1, x + 2, 0, r.z1() + 6, "minecraft:gray_concrete");
        }
        Rect t = new Rect(r.x1() - 4, r.z0(), r.x1(), r.z0() + 4);
        for (int y = 0; y < 18; y++) {
            Arch.walls(g, t, y, y, "minecraft:red_terracotta");
        }
        Arch.floor(g, t, 18, "minecraft:stone_bricks");
        g.set(t.cx(), 19, t.cz(), "minecraft:bell[attachment=floor,facing=south]");
        VoxelModel truck = VehicleModels.fireTruck(10);
        g.paste(truck, r.x0() + 4, 1, r.z1() + 6);
        return g.freeze();
    }

    static VoxelModel police(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        House.Spec s = House.spec().walls("white_concrete", "blue_concrete").trim("blue_concrete").window("glass_pane").roof(RoofType.FLAT, "smooth_quartz")
                .floors(2).depth(0.6).door("iron").with(Feature.ROOMS);
        Rect r = House.build(g, s, Math.max(13, width), seed, 0, 0, 0);
        g.fill(r.cx() - 4, 9, r.z1() + 1, r.cx() + 4, 9, r.z1() + 1, "minecraft:blue_concrete");
        g.fill(r.cx() - 3, 9, r.z1() + 2, r.cx() + 3, 9, r.z1() + 2, "minecraft:white_concrete");
        g.set(r.x0(), 10, r.z1(), "minecraft:redstone_block");
        g.set(r.x1(), 10, r.z1(), "minecraft:lapis_block");
        City.car(g, r.x1() + 3, r.z1() - 2, "white");
        City.car(g, r.x1() + 5, r.z1() - 2, "blue");
        return g.freeze();
    }

    static VoxelModel townHall(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        Rect r = Rect.centered(w, w * 2 / 3);
        classical(g, r, 9, "minecraft:smooth_sandstone", "minecraft:quartz_pillar[axis=y]", RoofMat.of("deepslate_tile"));
        Rect clock = Rect.centered(5, 5).translate(0, r.cz());
        for (int y = 9; y < 22; y++) {
            Arch.walls(g, clock, y, y, "minecraft:smooth_sandstone");
            Arch.air(g, clock.inset(1), y, y);
        }
        g.set(0, 18, clock.z1(), "minecraft:white_concrete");
        g.set(0, 19, clock.z1(), "minecraft:black_concrete");
        Arch.dome(g, 0, 22, clock.cz(), 3, 3, 3, "minecraft:oxidized_copper", false);
        g.fill(0, 26, clock.cz(), 0, 28, clock.cz(), "minecraft:iron_bars");
        g.set(r.x0() + 1, 1, r.z1() + 4, "minecraft:oak_fence");
        g.fill(r.x0() + 1, 2, r.z1() + 4, r.x0() + 1, 6, r.z1() + 4, "minecraft:oak_fence");
        g.set(r.x0() + 2, 6, r.z1() + 4, "minecraft:blue_wool");
        g.set(r.x0() + 3, 6, r.z1() + 4, "minecraft:white_wool");
        g.set(r.x0() + 4, 6, r.z1() + 4, "minecraft:red_wool");
        return g.freeze();
    }

    static VoxelModel library(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(13, width) | 1;
        Rect r = Rect.centered(w, w * 2 / 3);
        classical(g, r, 8, "minecraft:stone_bricks", "minecraft:polished_diorite", RoofMat.of("stone_brick"));
        for (int z = r.z0() + 2; z < r.z1() - 2; z += 3) {
            g.fill(r.x0() + 2, 1, z, r.x1() - 2, 3, z, "minecraft:bookshelf");
            g.fill(r.cx() - 1, 1, z, r.cx() + 1, 3, z, B.AIR);
        }
        g.set(r.cx(), 1, r.z1() - 2, "minecraft:lectern[facing=south,has_book=false]");
        g.set(r.cx(), 7, r.cz(), B.lantern(true));
        return g.freeze();
    }

    static VoxelModel museum(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(17, width) | 1;
        Rect r = Rect.centered(w, w * 2 / 3);
        classical(g, r, 10, "minecraft:quartz_bricks", "minecraft:quartz_pillar[axis=y]", RoofMat.of("quartz"));
        int pr = Math.max(4, w / 5);
        int pz = r.z1() + 4 + pr + 3;
        Arch.floor(g, Rect.centered(pr * 2 + 7, pr * 2 + 7).translate(0, pz), 0, "minecraft:polished_diorite");
        for (int i = 0; i <= pr; i++) {
            Rect l = Rect.centered((pr - i) * 2 + 1, (pr - i) * 2 + 1).translate(0, pz);
            Arch.walls(g, l, i + 1, i + 1, "minecraft:light_blue_stained_glass");
            Arch.air(g, l.inset(1), i + 1, i + 1);
        }
        g.set(r.cx() - 3, 1, r.cz(), "minecraft:chiseled_quartz_block");
        g.set(r.cx() + 3, 1, r.cz(), "minecraft:chiseled_quartz_block");
        g.set(r.cx() - 3, 2, r.cz(), "minecraft:decorated_pot");
        g.set(r.cx() + 3, 2, r.cz(), "minecraft:decorated_pot");
        return g.freeze();
    }

    static VoxelModel stadium(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(31, width);
        int rx = w / 2, rz = w / 3;
        int px = rx - Math.max(6, w / 6), pz = rz - Math.max(5, w / 7);
        int tiers = Math.max(5, w / 8);
        for (int x = -rx; x <= rx; x++) {
            for (int z = -rz; z <= rz; z++) {
                double v = (double) x * x / (rx * rx) + (double) z * z / (rz * rz);
                if (v > 1) {
                    continue;
                }
                if (Math.abs(x) <= px && Math.abs(z) <= pz) {
                    boolean line = Math.abs(x) == px || Math.abs(z) == pz || x == 0;
                    g.set(x, 0, z, line ? "minecraft:white_concrete" : (((x / 3) & 1) == 0 ? "minecraft:lime_concrete" : "minecraft:green_concrete"));
                    continue;
                }
                double inner = Math.max(Math.abs((double) x) / (px + 1), Math.abs((double) z) / (pz + 1));
                int h = (int) Math.min(tiers, Math.max(1, (Math.sqrt(v) - 0.55) / 0.45 * tiers + 1));
                if (inner < 1) {
                    h = 1;
                }
                g.fill(x, 0, z, x, h - 1, z, "minecraft:light_gray_concrete");
                g.set(x, h, z, ((x + z) & 3) == 0 ? "minecraft:red_concrete" : "minecraft:white_concrete");
                if (v > 0.93) {
                    g.fill(x, h + 1, z, x, tiers + 2, z, "minecraft:light_gray_concrete");
                }
            }
        }
        Arch.ring(g, 0, 0, 0, 3, "minecraft:white_concrete");
        for (int side = -1; side <= 1; side += 2) {
            int gx = side * px;
            g.fill(gx, 1, -2, gx, 3, -2, "minecraft:white_concrete");
            g.fill(gx, 1, 2, gx, 3, 2, "minecraft:white_concrete");
            g.fill(gx, 3, -2, gx, 3, 2, "minecraft:white_concrete");
            g.fill(gx + side, 1, -1, gx + side, 2, 1, "minecraft:white_stained_glass_pane");
        }
        for (int[] c : new int[][]{{-rx + 2, -rz + 2}, {rx - 2, -rz + 2}, {-rx + 2, rz - 2}, {rx - 2, rz - 2}}) {
            g.fill(c[0], 0, c[1], c[0], tiers + 12, c[1], "minecraft:iron_block");
            g.fill(c[0] - 1, tiers + 12, c[1], c[0] + 1, tiers + 13, c[1], "minecraft:sea_lantern");
        }
        return g.freeze();
    }

    static VoxelModel cinema(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        Rect r = Rect.centered(w, w);
        Arch.room(g, r, 0, 10, "minecraft:black_concrete", "minecraft:red_concrete");
        Arch.floor(g, r, 11, "minecraft:gray_concrete");
        for (int i = 0; i < r.depth() - 6; i++) {
            int y = 1 + i / 2;
            g.fill(r.x0() + 2, 1, r.z1() - 3 - i, r.x1() - 2, y, r.z1() - 3 - i, "minecraft:red_concrete");
            g.fill(r.x0() + 2, y + 1, r.z1() - 3 - i, r.x1() - 2, y + 1, r.z1() - 3 - i, (i & 1) == 0 ? B.stairs("crimson", B.S, false) : B.AIR);
        }
        g.fill(r.x0() + 2, 3, r.z0() + 1, r.x1() - 2, 8, r.z0() + 1, "minecraft:white_concrete");
        Rect marquee = new Rect(r.x0() + 1, r.z1() + 1, r.x1() - 1, r.z1() + 3);
        Arch.floor(g, marquee, 5, "minecraft:black_concrete");
        for (int x = marquee.x0(); x <= marquee.x1(); x++) {
            g.set(x, 6, marquee.z1(), (x & 1) == 0 ? "minecraft:glowstone" : "minecraft:yellow_concrete");
            g.set(x, 4, marquee.z1(), (x & 1) == 1 ? "minecraft:glowstone" : "minecraft:yellow_concrete");
        }
        g.fill(r.cx() - 4, 7, marquee.z1(), r.cx() + 4, 9, marquee.z1(), "minecraft:red_concrete");
        g.fill(r.cx() - 3, 8, marquee.z1() + 1, r.cx() + 3, 8, marquee.z1() + 1, "minecraft:white_concrete");
        g.fill(r.cx() - 1, 1, r.z1(), r.cx() + 1, 3, r.z1(), B.AIR);
        return g.freeze();
    }

    static VoxelModel restaurant(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        House.Spec s = House.spec().walls("bricks", "dark_oak_log[axis=y]").roof(RoofType.FLAT, "dark_oak").window("glass").furniture("dark_oak")
                .colors("red", "red").with(Feature.ROOMS);
        Rect r = House.build(g, s, Math.max(9, width), seed, 0, 0, 0);
        g.fill(r.x0(), 4, r.z1() + 1, r.x1(), 4, r.z1() + 1, "minecraft:red_wool");
        g.fill(r.x0(), 5, r.z1() + 1, r.x1(), 5, r.z1() + 1, "minecraft:white_wool");
        Rect t = new Rect(r.x0(), r.z1() + 2, r.x1(), r.z1() + 2 + Math.max(4, r.depth() / 2));
        Arch.floor(g, t, 0, "minecraft:spruce_planks");
        for (int x = t.x0() + 1; x < t.x1(); x += 4) {
            int z = t.cz();
            g.set(x, 1, z, "minecraft:dark_oak_fence");
            g.set(x, 2, z, "minecraft:dark_oak_pressure_plate");
            g.set(x + 1, 1, z, B.stairs("dark_oak", B.E, false));
            g.set(x - 1, 1, z, B.stairs("dark_oak", B.W, false));
            g.fill(x, 3, z, x, 3, z, "minecraft:dark_oak_fence");
            Arch.disk(g, x, 4, z, 1.5, ((x / 4) & 1) == 0 ? "minecraft:red_wool" : "minecraft:white_wool");
        }
        for (int x = t.x0(); x <= t.x1(); x++) {
            g.set(x, 1, t.z1(), "minecraft:potted_red_tulip");
        }
        return g.freeze();
    }

    static VoxelModel supermarket(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(21, width);
        Rect r = Rect.centered(w, w * 2 / 3);
        Arch.room(g, r, 0, 7, "minecraft:white_concrete", "minecraft:smooth_stone");
        Arch.floor(g, r, 8, "minecraft:light_gray_concrete");
        g.fill(r.cx() - 3, 1, r.z1(), r.cx() + 3, 4, r.z1(), "minecraft:glass");
        g.fill(r.cx() - 1, 1, r.z1(), r.cx() + 1, 3, r.z1(), B.AIR);
        g.fill(r.x0() + 1, 7, r.z1() + 1, r.x1() - 1, 9, r.z1() + 1, "minecraft:orange_concrete");
        g.fill(r.x0() + 3, 8, r.z1() + 2, r.x1() - 3, 8, r.z1() + 2, "minecraft:white_concrete");
        for (int x = r.x0() + 3; x < r.x1() - 2; x += 4) {
            g.fill(x, 1, r.z0() + 2, x, 2, r.cz() + 1, "minecraft:barrel[facing=up,open=false]");
            g.fill(x + 1, 1, r.z0() + 2, x + 1, 2, r.cz() + 1, "minecraft:chest[facing=east,type=single]");
        }
        for (int x = r.cx() - 4; x <= r.cx() + 4; x += 2) {
            g.set(x, 1, r.z1() - 2, "minecraft:smooth_stone_slab[type=top]");
        }
        City.parking().fill(g, new Rect(r.x0(), r.z1() + 3, r.x1(), r.z1() + 3 + w / 2), new Rng(seed));
        return g.freeze();
    }

    static VoxelModel gasStation(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(11, width);
        Rect canopy = Rect.centered(w, w / 2 + 2);
        Arch.floor(g, canopy.grow(2), 0, "minecraft:gray_concrete");
        Arch.floor(g, canopy, 6, "minecraft:white_concrete");
        Arch.walls(g, canopy, 6, 6, "minecraft:red_concrete");
        for (int x = canopy.x0() + 2; x <= canopy.x1() - 2; x += Math.max(4, w / 3)) {
            g.fill(x, 1, canopy.cz(), x, 5, canopy.cz(), "minecraft:white_concrete");
            g.set(x + 1, 1, canopy.cz(), "minecraft:red_concrete");
            g.set(x + 1, 2, canopy.cz(), "minecraft:black_stained_glass");
            g.set(x - 1, 1, canopy.cz(), "minecraft:red_concrete");
            g.set(x - 1, 2, canopy.cz(), "minecraft:black_stained_glass");
        }
        Rect shop = new Rect(canopy.x0(), canopy.z0() - 8, canopy.x0() + Math.max(7, w / 2), canopy.z0() - 2);
        Arch.room(g, shop, 0, 4, "minecraft:white_concrete", "minecraft:smooth_stone");
        Arch.floor(g, shop, 5, "minecraft:red_concrete");
        g.fill(shop.x0() + 1, 1, shop.z1(), shop.x1() - 1, 3, shop.z1(), "minecraft:glass");
        g.fill(shop.cx(), 1, shop.z1(), shop.cx(), 2, shop.z1(), B.AIR);
        g.fill(canopy.x1() + 2, 1, canopy.z0(), canopy.x1() + 2, 9, canopy.z0(), "minecraft:iron_block");
        g.fill(canopy.x1() + 1, 9, canopy.z0(), canopy.x1() + 3, 11, canopy.z0(), "minecraft:red_concrete");
        City.car(g, canopy.x0() + 4, canopy.cz() + 1, "blue");
        return g.freeze();
    }

    static VoxelModel latticeTower(int height) {
        VoxelGrid g = new VoxelGrid();
        int h = Math.max(30, height);
        int base = h / 4;
        String iron = "minecraft:iron_bars";
        String beam = "minecraft:polished_deepslate";
        for (int y = 0; y <= h; y++) {
            double t = (double) y / h;
            double half = base * Math.pow(1 - t, 1.8) + 1.0;
            int hi = (int) Math.round(half);
            int leg = Math.max(1, (int) Math.round(half * 0.25));
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    for (int dx = 0; dx < leg; dx++) {
                        for (int dz = 0; dz < leg; dz++) {
                            g.set(sx * (hi - dx), y, sz * (hi - dz), beam);
                        }
                    }
                }
            }
            if (y % 4 == 0 || y == h / 4 || y == h / 2) {
                Rect ringR = new Rect(-hi, -hi, hi, hi);
                Arch.walls(g, ringR, y, y, (y == h / 4 || y == h / 2) ? beam : iron);
            } else {
                for (int k = -hi; k <= hi; k++) {
                    if (Math.floorMod(k + y, 4) == 0) {
                        g.set(k, y, -hi, iron);
                        g.set(k, y, hi, iron);
                        g.set(-hi, y, k, iron);
                        g.set(hi, y, k, iron);
                    }
                }
            }
        }
        for (int p : new int[]{h / 4, h / 2}) {
            double half = base * Math.pow(1 - (double) p / h, 1.8) + 2.0;
            Arch.floor(g, new Rect((int) -half, (int) -half, (int) half, (int) half), p, beam);
        }
        // Arches entre les pieds
        int b = base;
        for (int i = -b + 2; i <= b - 2; i++) {
            int y = (int) Math.round(Math.sqrt(Math.max(0, 1 - (double) i * i / (b * b))) * b * 0.5);
            g.set(i, y, -b, beam);
            g.set(i, y, b, beam);
            g.set(-b, y, i, beam);
            g.set(b, y, i, beam);
        }
        g.fill(0, h, 0, 0, h + 6, 0, "minecraft:lightning_rod[facing=up]");
        return g.freeze();
    }
}
