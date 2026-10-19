package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.Castle;
import com.ultimateautobuilder.core.generator.House;
import com.ultimateautobuilder.core.generator.Trees;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Noise;
import com.ultimateautobuilder.core.util.Rng;

/** 20 îles flottantes et paysages. */
public final class IslandModels {
    private IslandModels() {
    }

    /** Matériaux d'une île : surface, sous-couche, roche. */
    record Soil(String top, String sub, String rock) {
        static final Soil GRASS = new Soil("minecraft:grass_block", "minecraft:dirt", "minecraft:stone");
        static final Soil SAND = new Soil("minecraft:sand", "minecraft:sandstone", "minecraft:sandstone");
        static final Soil SNOW = new Soil("minecraft:snow_block", "minecraft:dirt", "minecraft:stone");
        static final Soil MYCELIUM = new Soil("minecraft:mycelium", "minecraft:dirt", "minecraft:stone");
        static final Soil JUNGLE = new Soil("minecraft:moss_block", "minecraft:rooted_dirt", "minecraft:mossy_cobblestone");
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.ISLANDS);
        var I = Reg.ISLAND;
        r.gen("ile_flottante", "Petite île flottante", 2, I, p -> simpleIsland(p.size(), p.seed(), Soil.GRASS, 0), "ile", "flottante", "ciel");
        r.gen("ile_flottante_maison", "Île flottante avec maison", 3, I, p -> simpleIsland(p.size(), p.seed(), Soil.GRASS, 1), "ile", "maison", "ciel");
        r.gen("ile_cascade", "Île à la cascade gelée", 3, I, p -> simpleIsland(p.size(), p.seed(), Soil.GRASS, 2), "ile", "cascade", "glace");
        r.gen("archipel_flottant", "Archipel flottant", 4, I, p -> archipelago(p.size(), p.seed()), "archipel", "iles", "ponts");
        r.gen("ile_chateau", "Château dans le ciel", 5, I, p -> castleIsland(p.size(), p.seed()), "ile", "chateau", "ciel", "laputa");
        r.gen("ile_desert", "Île désertique", 2, I, p -> simpleIsland(p.size(), p.seed(), Soil.SAND, 3), "ile", "desert", "cactus");
        r.gen("ile_enneigee", "Île enneigée", 2, I, p -> simpleIsland(p.size(), p.seed(), Soil.SNOW, 4), "ile", "neige", "sapin");
        r.gen("ile_jungle", "Île de la jungle", 3, I, p -> simpleIsland(p.size(), p.seed(), Soil.JUNGLE, 5), "ile", "jungle", "tropicale");
        r.gen("ile_champignon", "Île aux champignons", 3, I, p -> simpleIsland(p.size(), p.seed(), Soil.MYCELIUM, 6), "ile", "champignon", "mycelium");
        r.gen("volcan", "Volcan", 4, I, p -> volcano(p.size(), p.seed()), "volcan", "lave", "montagne");
        r.gen("montagne", "Montagne enneigée", 3, I, p -> mountain(p.size(), p.seed(), false), "montagne", "neige", "sommet");
        r.gen("arche_rocheuse", "Arche rocheuse", 3, I, p -> rockArch(p.size(), p.seed()), "arche", "rocher", "falaise");
        r.gen("oasis", "Oasis", 3, I, p -> oasis(p.size(), p.seed()), "oasis", "desert", "palmier", "eau");
        r.gen("jardin_zen", "Jardin zen", 3, I, p -> zenGarden(p.size(), p.seed()), "jardin", "zen", "japon", "gravier");
        r.gen("glacier", "Glacier", 3, I, p -> mountain(p.size(), p.seed(), true), "glacier", "glace", "banquise");
        r.gen("lac_ponton", "Lac et ponton", 3, I, p -> lake(p.size(), p.seed()), "lac", "ponton", "peche");
        r.gen("falaise", "Falaise côtière", 3, I, p -> cliff(p.size(), p.seed()), "falaise", "cote", "rocher");
        r.gen("colline_fleurie", "Colline fleurie", 2, I, p -> flowerHill(p.size(), p.seed()), "colline", "fleurs", "prairie");
        r.gen("temple_celeste", "Temple céleste", 5, I, p -> skyTemple(p.size(), p.seed()), "temple", "ciel", "ile", "nuages");
        r.gen("ile_cristal", "Île de cristal", 4, I, p -> crystalIsland(p.size(), p.seed()), "ile", "cristal", "amethyste");
    }

    /** Île flottante : dessus bombé, dessous en cône irrégulier. Renvoie la hauteur de la surface. */
    static int island(VoxelGrid g, int cx, int cy, int cz, int r, long seed, Soil soil) {
        int depth = Math.max(4, (int) (r * 1.1));
        for (int dx = -r - 2; dx <= r + 2; dx++) {
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                double ang = Math.atan2(dz, dx);
                double edge = r * (0.85 + 0.25 * Noise.value2(seed, Math.cos(ang) * 2 + 5, Math.sin(ang) * 2 + 5));
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > edge) {
                    continue;
                }
                double k = d / edge;
                int bottom = (int) Math.round(depth * (1 - k * k) * (0.7 + 0.3 * Noise.value2(seed + 3, dx * 0.3, dz * 0.3)));
                int bump = (int) Math.round((1 - k) * 2 * Noise.value2(seed + 7, dx * 0.2, dz * 0.2));
                for (int y = -bottom; y <= bump; y++) {
                    String m = y == bump ? soil.top() : y >= bump - 2 ? soil.sub() : soil.rock();
                    if (y < bump - 2 && Noise.hash(seed, dx, y, dz) < 0.04) {
                        m = "minecraft:coal_ore";
                    }
                    g.set(cx + dx, cy + y, cz + dz, m);
                }
            }
        }
        return cy;
    }

    static VoxelModel simpleIsland(int width, long seed, Soil soil, int variant) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(6, width / 2);
        int cy = r + 4;
        island(g, 0, cy, 0, r, seed, soil);
        int top = cy + 1;
        switch (variant) {
            case 1 -> House.build(g, House.spec().walls("oak_planks", "oak_log[axis=y]").roof(RoofType.GABLE, "spruce"), Math.max(5, r), seed, 0, cy, 0);
            case 2 -> {
                Trees.oakInto(g, -r / 3, top, -r / 3, Math.max(6, r), seed, "minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]");
                for (int dz = -1; dz <= 1; dz++) {
                    g.fill(r / 2, cy, dz, r / 2 + 1, cy, dz, "minecraft:packed_ice");
                }
                for (int y = cy; y >= 0; y--) {
                    for (int dz = -1; dz <= 1; dz++) {
                        g.set(r + 1 + (cy - y) / 6, y, dz, (y + dz) % 3 == 0 ? "minecraft:blue_ice" : "minecraft:packed_ice");
                    }
                }
                Arch.disk(g, r + 2, 0, 0, 3, "minecraft:blue_ice");
            }
            case 3 -> {
                for (int i = 0; i < 3; i++) {
                    int x = rng.range(-r / 2, r / 2), z = rng.range(-r / 2, r / 2);
                    g.fill(x, top, z, x, top + rng.range(2, 4), z, "minecraft:cactus");
                }
                Trees.palmInto(g, r / 3, top, 0, Math.max(6, r), seed);
            }
            case 4 -> {
                for (int i = 0; i < 3; i++) {
                    Trees.coniferInto(g, rng.range(-r / 2, r / 2), top, rng.range(-r / 2, r / 2), Math.max(6, r), rng.nextLong());
                }
            }
            case 5 -> {
                Trees.palmInto(g, -r / 3, top, 0, Math.max(7, r + 2), seed);
                Trees.oakInto(g, r / 3, top, r / 4, Math.max(6, r), seed + 1, "minecraft:jungle_log[axis=y]", "minecraft:jungle_leaves[persistent=true]");
                for (int i = 0; i < r * 2; i++) {
                    int x = rng.range(-r, r), z = rng.range(-r, r);
                    if (g.isSet(x, cy, z) && !g.isSet(x, top, z)) {
                        g.set(x, top, z, rng.chance(0.5) ? "minecraft:fern" : "minecraft:short_grass");
                    }
                }
            }
            case 6 -> {
                VoxelModel m = mushroomModel(Math.max(8, r + 2), true);
                g.paste(m, -r / 4, top, 0);
                g.paste(mushroomModel(Math.max(6, r), false), r / 3, top, r / 3);
            }
            default -> Trees.oakInto(g, 0, top, 0, Math.max(6, r), seed, "minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]");
        }
        return g.freeze();
    }

    private static VoxelModel mushroomModel(int height, boolean red) {
        return NatureModels.mushroom(red).voxelize(height, false).freeze().groundAtBottom();
    }

    static VoxelModel archipelago(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(15, width);
        int n = 5;
        int[][] pos = new int[n][];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            int r = Math.max(3, w / 8 + rng.range(-1, 2));
            int x = i == 0 ? 0 : (int) Math.round(Math.cos(a) * w / 3);
            int z = i == 0 ? 0 : (int) Math.round(Math.sin(a) * w / 3);
            int y = w / 4 + rng.range(0, w / 6);
            pos[i] = new int[]{x, y, z};
            island(g, x, y, z, r, seed + i, Soil.GRASS);
            if (rng.chance(0.7)) {
                Trees.oakInto(g, x, y + 1, z, Math.max(5, r + 2), rng.nextLong(), "minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]");
            }
        }
        for (int i = 1; i < n; i++) {
            Arch.line(g, pos[0][0], pos[0][1] + 1, pos[0][2], pos[i][0], pos[i][1] + 1, pos[i][2], "minecraft:spruce_planks");
            Arch.line(g, pos[0][0], pos[0][1] + 2, pos[0][2], pos[i][0], pos[i][1] + 2, pos[i][2], "minecraft:air");
        }
        return g.freeze();
    }

    static VoxelModel castleIsland(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(12, width / 2);
        int cy = r + 6;
        island(g, 0, cy, 0, r, seed, Soil.GRASS);
        Castle.build(g, Castle.spec().roofs("light_blue_concrete").noHouses().walls("calcite", "smooth_quartz"), Math.max(17, r), seed, cy);
        return g.freeze();
    }

    static VoxelModel volcano(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(8, width / 2);
        int h = r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / r;
                if (d > 1) {
                    continue;
                }
                int top = (int) Math.round(h * (1 - d) * (0.85 + 0.3 * Noise.value2(seed, dx * 0.25, dz * 0.25)));
                double crater = 0.22;
                if (d < crater) {
                    top = (int) Math.round(h * (1 - crater)) - 3;
                }
                for (int y = 0; y <= top; y++) {
                    String m = y > top - 1 && d >= crater ? (Noise.hash(seed, dx, y, dz) < 0.3 ? "minecraft:magma_block" : "minecraft:basalt[axis=y]")
                            : "minecraft:blackstone";
                    g.set(dx, y, dz, m);
                }
                if (d < crater - 0.05) {
                    g.set(dx, top, dz, "minecraft:lava[level=0]");
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + 0.4;
            for (int t = 0; t < r; t++) {
                int x = (int) Math.round(Math.cos(a) * (r * 0.25 + t)), z = (int) Math.round(Math.sin(a) * (r * 0.25 + t));
                int y = (int) Math.round(h * (1 - (r * 0.25 + t) / (double) r));
                if (y > 0) {
                    g.set(x, y, z, "minecraft:magma_block");
                }
            }
        }
        return g.freeze();
    }

    static VoxelModel mountain(int width, long seed, boolean glacier) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(8, width / 2);
        int h = (int) (r * 1.2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / r;
                if (d > 1) {
                    continue;
                }
                double n = Noise.fbm2(seed, dx * 0.12, dz * 0.12, 3);
                int top = (int) Math.round(h * Math.pow(1 - d, 1.3) * (0.6 + 0.8 * n));
                for (int y = 0; y <= top; y++) {
                    String m;
                    if (glacier) {
                        m = y == top ? "minecraft:snow_block" : y > top - 3 ? "minecraft:packed_ice" : Noise.hash(seed, dx, y, dz) < 0.2
                                ? "minecraft:blue_ice" : "minecraft:packed_ice";
                    } else if (y == top) {
                        m = top > h * 0.6 ? "minecraft:snow_block" : top > h * 0.3 ? "minecraft:stone" : "minecraft:grass_block";
                    } else {
                        m = y > top - 2 && top <= h * 0.3 ? "minecraft:dirt" : (Noise.hash(seed, dx, y, dz) < 0.15 ? "minecraft:andesite" : "minecraft:stone");
                    }
                    g.set(dx, y, dz, m);
                }
            }
        }
        return g.freeze();
    }

    static VoxelModel rockArch(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(8, width / 2);
        int h = r;
        for (int a = 0; a <= 180; a += 2) {
            double rad = Math.toRadians(a);
            int x = (int) Math.round(Math.cos(rad) * r * 0.8), y = (int) Math.round(Math.sin(rad) * h);
            int th = Math.max(2, r / 4);
            for (int dx = -th; dx <= th; dx++) {
                for (int dy = -th; dy <= th; dy++) {
                    for (int dz = -th; dz <= th; dz++) {
                        if (dx * dx + dy * dy + dz * dz <= th * th + Noise.value3(seed, x + dx, y + dy, dz) * 4) {
                            String m = Noise.hash(seed, x + dx, y + dy, dz) < 0.3 ? "minecraft:red_sandstone" : "minecraft:terracotta";
                            g.set(x + dx, y + dy, dz, y + dy < 0 ? "minecraft:red_sandstone" : m);
                        }
                    }
                }
            }
            for (int dy = 0; dy < th; dy++) {
                // Pieds élargis
                if (a < 6 || a > 174) {
                    Arch.disk(g, x, dy, 0, th + 1.5, "minecraft:red_sandstone");
                }
            }
        }
        Arch.disk(g, 0, 0, 0, r + 2, "minecraft:red_sand");
        return g.freeze();
    }

    static VoxelModel oasis(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(8, width / 2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / r;
                if (d > 1) {
                    continue;
                }
                int dune = (int) Math.round(2 * Noise.value2(seed, dx * 0.15, dz * 0.15) * d);
                g.fill(dx, -2, dz, dx, dune, dz, "minecraft:sand");
                if (d < 0.38) {
                    g.fill(dx, -2, dz, dx, -2, dz, "minecraft:clay");
                    g.fill(dx, -1, dz, dx, 0, dz, d < 0.33 ? "minecraft:water[level=0]" : "minecraft:sand");
                    g.fill(dx, 1, dz, dx, 2, dz, B.AIR);
                } else if (d < 0.45 && rng.chance(0.3)) {
                    g.set(dx, dune + 1, dz, rng.chance(0.5) ? "minecraft:short_grass" : "minecraft:fern");
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 + 0.3;
            int x = (int) Math.round(Math.cos(a) * r * 0.5), z = (int) Math.round(Math.sin(a) * r * 0.5);
            Trees.palmInto(g, x, 1, z, Math.max(7, r), rng.nextLong());
        }
        return g.freeze();
    }

    static VoxelModel zenGarden(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(15, width);
        var rect = Arch.Rect.centered(w, w);
        for (int x = rect.x0(); x <= rect.x1(); x++) {
            for (int z = rect.z0(); z <= rect.z1(); z++) {
                if (rect.onEdge(x, z)) {
                    g.set(x, 0, z, "minecraft:dark_oak_planks");
                    g.set(x, 1, z, "minecraft:bamboo_fence");
                } else {
                    g.set(x, 0, z, ((z & 1) == 0) ? "minecraft:gravel" : "minecraft:white_concrete_powder");
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = rng.range(rect.x0() + 3, rect.x1() - 3), z = rng.range(rect.z0() + 3, rect.z1() - 3);
            Arch.ring(g, x, 0, z, 2.5, "minecraft:gravel");
            Arch.dome(g, x, 1, z, 1, 1, 1, "minecraft:mossy_cobblestone", false);
        }
        int px = rect.x0() + 3, pz = rect.z1() - 4;
        g.fill(px - 2, -2, pz - 2, px + 2, -2, pz + 2, "minecraft:stone");
        g.fill(px - 1, -1, pz - 1, px + 1, 0, pz + 1, "minecraft:water[level=0]");
        g.set(px, 1, pz, "minecraft:lily_pad");
        Trees.oakInto(g, rect.x1() - 4, 1, rect.z0() + 4, Math.max(7, w / 3), seed, "minecraft:dark_oak_log[axis=y]", "minecraft:red_terracotta");
        g.fill(rect.x0() + 3, 1, rect.z0() + 3, rect.x0() + 3, 2, rect.z0() + 3, "minecraft:stone_brick_wall");
        g.set(rect.x0() + 3, 3, rect.z0() + 3, B.lantern(false));
        for (int x = rect.cx() - 1; x <= rect.cx() + 1; x++) {
            for (int z = rect.cz(); z <= rect.z1() - 1; z += 2) {
                g.set(x, 0, z, "minecraft:smooth_stone");
            }
        }
        return g.freeze();
    }

    static VoxelModel lake(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(8, width / 2);
        for (int dx = -r - 2; dx <= r + 2; dx++) {
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                double ang = Math.atan2(dz, dx);
                double edge = r * (0.8 + 0.3 * Noise.value2(seed, Math.cos(ang) * 2 + 3, Math.sin(ang) * 2 + 3));
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > edge + 2) {
                    continue;
                }
                if (d > edge) {
                    g.set(dx, 0, dz, "minecraft:grass_block");
                    if (rng.chance(0.15)) {
                        g.set(dx, 1, dz, "minecraft:short_grass");
                    }
                    continue;
                }
                int depth = 1 + (int) Math.round(3 * (1 - d / edge));
                g.set(dx, -depth - 1, dz, "minecraft:clay");
                g.fill(dx, -depth, dz, dx, 0, dz, "minecraft:water[level=0]");
                g.fill(dx, 1, dz, dx, 2, dz, B.AIR);
                if (d < edge - 1 && rng.chance(0.02)) {
                    g.set(dx, 1, dz, "minecraft:lily_pad");
                }
            }
        }
        int len = r;
        for (int z = r - 2; z >= r - 2 - len; z--) {
            g.fill(-1, 1, z, 1, 1, z, "minecraft:spruce_planks");
            if (z % 3 == 0) {
                g.fill(-2, -3, z, -2, 2, z, "minecraft:spruce_log[axis=y]");
                g.fill(2, -3, z, 2, 2, z, "minecraft:spruce_log[axis=y]");
            }
        }
        g.set(2, 3, r - 2 - len, B.lantern(false));
        g.paste(VehicleModels.boat(9, seed), 5, 1, r / 2 - 2);
        return g.freeze();
    }

    static VoxelModel cliff(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width);
        int h = w;
        for (int x = -w / 2; x <= w / 2; x++) {
            for (int z = -w / 2; z <= w / 2; z++) {
                double edge = Noise.value2(seed, x * 0.15, 0) * 4;
                if (z > edge) {
                    g.fill(x, -3, z, x, -3, z, "minecraft:sand");
                    if (z > edge + 3) {
                        g.fill(x, -2, z, x, 0, z, "minecraft:water[level=0]");
                    } else {
                        g.fill(x, -2, z, x, -1, z, "minecraft:sand");
                    }
                    continue;
                }
                int top = (int) Math.round(h * (0.6 + 0.4 * Noise.fbm2(seed + 1, x * 0.08, z * 0.08, 3)));
                for (int y = -3; y <= top; y++) {
                    int band = Math.floorMod(y, 6);
                    g.set(x, y, z, y == top ? "minecraft:grass_block" : band < 2 ? "minecraft:stone" : band < 4 ? "minecraft:andesite" : "minecraft:tuff");
                }
            }
        }
        return g.freeze();
    }

    static VoxelModel flowerHill(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(8, width / 2);
        String[] flowers = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:allium",
                "minecraft:azure_bluet", "minecraft:pink_tulip", "minecraft:lily_of_the_valley"};
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / r;
                if (d > 1) {
                    continue;
                }
                int top = (int) Math.round(r * 0.45 * (1 + Math.cos(d * Math.PI)) / 2);
                g.fill(dx, 0, dz, dx, top - 1, dz, "minecraft:dirt");
                g.set(dx, top, dz, "minecraft:grass_block");
                if (rng.chance(0.35)) {
                    g.set(dx, top + 1, dz, flowers[(int) Math.floor(Noise.value2(seed, dx * 0.15, dz * 0.15) * flowers.length) % flowers.length]);
                }
            }
        }
        Trees.oakInto(g, 0, (int) Math.round(r * 0.45) + 1, 0, Math.max(7, r), seed, "minecraft:birch_log[axis=y]", "minecraft:birch_leaves[persistent=true]");
        return g.freeze();
    }

    static VoxelModel skyTemple(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(10, width / 2);
        int cy = r + 8;
        island(g, 0, cy, 0, r, seed, Soil.GRASS);
        VoxelModel temple = DungeonModels.greekTemple(Math.max(11, r));
        g.paste(temple, 0, cy + 1 + temple.anchorY(), 0);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            int x = (int) Math.round(Math.cos(a) * (r + 6)), z = (int) Math.round(Math.sin(a) * (r + 6));
            Arch.dome(g, x, cy - 6 + (i % 3) * 3, z, 3 + i % 2, 1, 2, "minecraft:white_wool", false);
        }
        return g.freeze();
    }

    static VoxelModel crystalIsland(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(7, width / 2);
        int cy = r + 4;
        island(g, 0, cy, 0, r, seed, new Soil("minecraft:amethyst_block", "minecraft:calcite", "minecraft:smooth_basalt"));
        for (int i = 0; i < r; i++) {
            int x = rng.range(-r / 2, r / 2), z = rng.range(-r / 2, r / 2);
            int h = rng.range(3, Math.max(4, r));
            for (int y = 0; y < h; y++) {
                double rr = Math.max(0.4, 1.5 * (1 - (double) y / h));
                Arch.disk(g, x + (y / 3) * (x > 0 ? 1 : -1), cy + 1 + y, z, rr, y > h * 0.7 ? "minecraft:purple_stained_glass" : "minecraft:amethyst_block");
            }
        }
        for (int i = 0; i < 6; i++) {
            int x = rng.range(-r / 2, r / 2), z = rng.range(-r / 2, r / 2);
            g.setIfEmpty(x, cy + 1, z, "minecraft:amethyst_cluster[facing=up]");
        }
        return g.freeze();
    }
}
