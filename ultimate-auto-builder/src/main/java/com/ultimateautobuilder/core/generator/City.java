package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de quartiers : trame orthogonale de rues (chaussée, marquage, trottoirs,
 * lampadaires) dont chaque îlot reçoit un « remplisseur » (immeuble, maison, parc, usine…).
 * Les bâtiments sont orientés vers la rue située au sud de leur îlot.
 */
public final class City {
    /** Remplit un îlot. La façade attendue regarde vers +Z. */
    @FunctionalInterface
    public interface Lot {
        void fill(VoxelGrid g, Rect lot, Rng rng);
    }

    public record Weighted(Lot lot, int weight) {
    }

    public static final class Spec {
        public String road = "minecraft:gray_concrete";
        public String line = "minecraft:white_concrete";
        public String sidewalk = "minecraft:smooth_stone";
        public String ground = "minecraft:grass_block";
        public String lampPost = "minecraft:iron_bars";
        public String lampLight = "minecraft:lantern[hanging=false]";
        public int block = 22;
        public int roadWidth = 7;
        public boolean markings = true;
        public final List<Weighted> lots = new ArrayList<>();

        public Spec road(String road, String line, String sidewalk) { this.road = B.mc(road); this.line = line == null ? null : B.mc(line); this.sidewalk = B.mc(sidewalk); return this; }
        public Spec ground(String v) { ground = B.mc(v); return this; }
        public Spec lamps(String post, String light) { lampPost = B.mc(post); lampLight = B.mc(light); return this; }
        public Spec block(int size, int road) { block = size; roadWidth = road; return this; }
        public Spec noMarkings() { markings = false; return this; }
        public Spec lot(Lot l, int weight) { lots.add(new Weighted(l, weight)); return this; }
    }

    private City() {
    }

    public static Spec spec() {
        return new Spec();
    }

    public static VoxelModel generate(Spec s, int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        build(g, s, width, seed);
        return g.freeze();
    }

    public static void build(VoxelGrid g, Spec s, int width, long seed) {
        Rng rng = new Rng(seed);
        int period = s.block + s.roadWidth;
        int cells = Math.max(1, (width - s.roadWidth) / period);
        int total = cells * period + s.roadWidth;
        int x0 = -total / 2, z0 = -total / 2;
        // Chaussées
        for (int x = x0; x < x0 + total; x++) {
            for (int z = z0; z < z0 + total; z++) {
                int mx = Math.floorMod(x - x0, period), mz = Math.floorMod(z - z0, period);
                boolean roadX = mx < s.roadWidth, roadZ = mz < s.roadWidth;
                if (!roadX && !roadZ) {
                    continue;
                }
                int m = roadX ? mx : mz;
                boolean side = m == 0 || m == s.roadWidth - 1;
                String st = side ? s.sidewalk : s.road;
                if (s.markings && s.line != null && !side && m == s.roadWidth / 2 && !(roadX && roadZ)) {
                    int along = roadX ? z : x;
                    if (Math.floorMod(along, 4) < 2) {
                        st = s.line;
                    }
                }
                if (roadX && roadZ && s.markings && s.line != null && !side && (mx == 1 || mx == s.roadWidth - 2 || mz == 1 || mz == s.roadWidth - 2)
                        && ((mx + mz) & 1) == 0) {
                    st = s.line; // passages piétons
                }
                g.set(x, 0, z, st);
                g.fill(x, 1, z, x, 3, z, B.AIR);
                // Lampadaires aux angles des trottoirs
                if (side && roadX != roadZ && Math.floorMod(roadX ? z - z0 : x - x0, period) == s.roadWidth + 1) {
                    g.fill(x, 1, z, x, 3, z, s.lampPost);
                    g.set(x, 4, z, s.lampLight);
                }
            }
        }
        // Îlots
        int totalWeight = s.lots.stream().mapToInt(Weighted::weight).sum();
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                Rect lot = new Rect(x0 + i * period + s.roadWidth, z0 + j * period + s.roadWidth,
                        x0 + i * period + s.roadWidth + s.block - 1, z0 + j * period + s.roadWidth + s.block - 1);
                Arch.floor(g, lot, 0, s.ground);
                if (totalWeight <= 0) {
                    continue;
                }
                int pick = rng.nextInt(totalWeight);
                for (Weighted w : s.lots) {
                    pick -= w.weight();
                    if (pick < 0) {
                        w.lot().fill(g, lot, new Rng(rng.nextLong()));
                        break;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ remplisseurs courants

    public static Lot tower(Tower.Spec spec, double minH, double maxH) {
        return (g, lot, rng) -> {
            Tower.Spec t = spec.copy().height(rng.range(minH, maxH));
            int w = lot.width() - 4;
            Tower.build(g, t.noInteriorAir(), w, lot.depth() - 4, Tower.floorsFor(t, w), lot.cx(), 0, lot.cz());
            paveFront(g, lot, "minecraft:smooth_stone");
        };
    }

    public static Lot house(House.Spec spec) {
        return (g, lot, rng) -> House.build(g, spec, Math.max(5, lot.width() - 6), rng.nextLong(), lot.cx(), 0, lot.cz() - 1);
    }

    /** Plusieurs petites maisons par îlot (lotissement). */
    public static Lot houses(House.Spec... specs) {
        return (g, lot, rng) -> {
            int hw = Math.max(5, lot.width() / 2 - 3);
            int[][] pos = {{lot.x0() + hw / 2 + 1, lot.z1() - hw / 2 - 3}, {lot.x1() - hw / 2 - 1, lot.z1() - hw / 2 - 3}};
            for (int[] p : pos) {
                House.build(g, specs[rng.nextInt(specs.length)], hw, rng.nextLong(), p[0], 0, p[1]);
            }
            for (int x = lot.x0() + 1; x < lot.x1(); x += 3) {
                if (rng.chance(0.6)) {
                    Trees.oakInto(g, x, 1, lot.z0() + 2, Math.max(5, lot.width() / 3), rng.nextLong(), "minecraft:oak_log[axis=y]",
                            "minecraft:oak_leaves[persistent=true]");
                    x += 4;
                }
            }
        };
    }

    public static Lot park(String treeLog, String treeLeaves) {
        return (g, lot, rng) -> {
            for (int x = lot.x0(); x <= lot.x1(); x++) {
                g.set(x, 0, lot.cz(), "minecraft:dirt_path");
            }
            for (int z = lot.z0(); z <= lot.z1(); z++) {
                g.set(lot.cx(), 0, z, "minecraft:dirt_path");
            }
            Arch.disk(g, lot.cx(), 0, lot.cz(), 2.5, "minecraft:stone_bricks");
            Arch.disk(g, lot.cx(), 1, lot.cz(), 2.5, "minecraft:stone_brick_slab[type=bottom]");
            Arch.disk(g, lot.cx(), 1, lot.cz(), 1.5, "minecraft:water[level=0]");
            g.fill(lot.cx(), 1, lot.cz(), lot.cx(), 2, lot.cz(), "minecraft:stone_brick_wall");
            g.set(lot.cx(), 3, lot.cz(), "minecraft:sea_lantern");
            int th = Math.max(6, lot.width() / 3);
            int[][] q = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
            for (int[] d : q) {
                int tx = lot.cx() + d[0] * lot.width() / 4, tz = lot.cz() + d[1] * lot.depth() / 4;
                if (treeLog.contains("spruce")) {
                    Trees.coniferInto(g, tx, 1, tz, th, rng.nextLong());
                } else if (treeLog.contains("jungle")) {
                    Trees.palmInto(g, tx, 1, tz, th, rng.nextLong());
                } else if (treeLog.contains("cherry")) {
                    Trees.cherryInto(g, tx, 1, tz, th, rng.nextLong());
                } else {
                    Trees.oakInto(g, tx, 1, tz, th, rng.nextLong(), treeLog, treeLeaves);
                }
            }
            g.set(lot.cx() + 3, 1, lot.cz() + 1, B.stairs("oak", B.N, false));
            g.set(lot.cx() + 4, 1, lot.cz() + 1, B.stairs("oak", B.N, false));
        };
    }

    public static Lot plaza(String paving) {
        return (g, lot, rng) -> {
            Arch.floor(g, lot, 0, paving);
            Arch.disk(g, lot.cx(), 1, lot.cz(), Math.max(2, lot.width() / 5), "minecraft:smooth_stone_slab[type=bottom]");
            g.fill(lot.cx(), 1, lot.cz(), lot.cx(), Math.max(4, lot.width() / 3), lot.cz(), "minecraft:quartz_pillar[axis=y]");
            g.set(lot.cx(), Math.max(4, lot.width() / 3) + 1, lot.cz(), "minecraft:gold_block");
        };
    }

    public static Lot parking() {
        return (g, lot, rng) -> {
            Arch.floor(g, lot, 0, "minecraft:gray_concrete");
            String[] cars = {"red", "blue", "white", "black", "yellow", "green"};
            for (int x = lot.x0() + 1; x < lot.x1(); x += 3) {
                g.fill(x, 0, lot.z0() + 1, x, 0, lot.z0() + 5, "minecraft:white_concrete");
                g.fill(x, 0, lot.z1() - 5, x, 0, lot.z1() - 1, "minecraft:white_concrete");
                if (x + 2 < lot.x1() && rng.chance(0.6)) {
                    car(g, x + 1, lot.z0() + 2, cars[rng.nextInt(cars.length)]);
                }
                if (x + 2 < lot.x1() && rng.chance(0.6)) {
                    car(g, x + 1, lot.z1() - 4, cars[rng.nextInt(cars.length)]);
                }
            }
        };
    }

    /** Petite voiture 1×2×3 (orientée selon Z). */
    public static void car(VoxelGrid g, int x, int z, String color) {
        g.fill(x, 1, z, x, 1, z + 2, B.concrete(color));
        g.set(x, 2, z + 1, "minecraft:black_stained_glass");
        g.set(x, 2, z, B.concrete(color).replace("_concrete", "_carpet"));
    }

    public static Lot warehouse() {
        return (g, lot, rng) -> {
            Rect r = lot.inset(2);
            int h = 6;
            Arch.room(g, r, 0, h, "minecraft:light_gray_concrete", "minecraft:smooth_stone");
            for (int x = r.x0() + 2; x < r.x1(); x += 4) {
                g.fill(x, 1, r.z1(), x + 1, 4, r.z1(), "minecraft:iron_block");
            }
            Arch.roof(g, Arch.RoofType.GABLE_Z, r, h + 1, RoofMat.of("polished_andesite"), 0, "minecraft:light_gray_concrete");
            if (rng.chance(0.6)) {
                int cx = r.x0() + 2, cz = r.z0() + 2;
                Arch.cylinder(g, cx, 0, cz, 1, h + 12, "minecraft:bricks", "minecraft:bricks", 0);
                g.set(cx, h + 12, cz, "minecraft:campfire[lit=true,signal_fire=true,facing=north]");
            }
        };
    }

    public static Lot tanks() {
        return (g, lot, rng) -> {
            int rr = Math.max(2, lot.width() / 6);
            for (int i = -1; i <= 1; i += 2) {
                for (int j = -1; j <= 1; j += 2) {
                    int cx = lot.cx() + i * (rr + 2), cz = lot.cz() + j * (rr + 2);
                    Arch.cylinder(g, cx, 1, cz, rr, rr * 2 + 3, "minecraft:white_concrete", "minecraft:white_concrete", 0);
                    Arch.dome(g, cx, rr * 2 + 4, cz, rr, Math.max(1, rr / 2), rr, "minecraft:light_gray_concrete", false);
                    Arch.ring(g, cx, rr + 2, cz, rr, "minecraft:red_concrete");
                }
            }
            g.fill(lot.x0() + 1, 3, lot.cz(), lot.x1() - 1, 3, lot.cz(), "minecraft:iron_block");
        };
    }

    public static Lot water() {
        return (g, lot, rng) -> {
            g.fill(lot.x0(), -3, lot.z0(), lot.x1(), -3, lot.z1(), "minecraft:stone_bricks");
            g.fill(lot.x0(), -2, lot.z0(), lot.x1(), 0, lot.z1(), "minecraft:water[level=0]");
            Arch.walls(g, lot, -2, 0, "minecraft:stone_bricks");
        };
    }

    private static void paveFront(VoxelGrid g, Rect lot, String mat) {
        g.fill(lot.x0(), 0, lot.z1() - 1, lot.x1(), 0, lot.z1(), mat);
    }
}
