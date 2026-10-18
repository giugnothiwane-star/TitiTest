package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Noise;
import com.ultimateautobuilder.core.util.Rng;

/**
 * Générateur de châteaux : enceinte crénelée avec chemin de ronde, tours d'angle et
 * intermédiaires, porte fortifiée à herse, douves et pont-levis, cour et donjon.
 */
public final class Castle {
    public enum TowerShape { ROUND, SQUARE }

    public static final class Spec {
        public String wall = "minecraft:stone_bricks";
        public String accent = "minecraft:cobblestone";
        public String floor = "minecraft:spruce_planks";
        public String path = "minecraft:cobblestone";
        public String ground = "minecraft:grass_block";
        public String roofMat = "minecraft:dark_oak_planks"; // toits coniques (null = créneaux)
        public String flag = "minecraft:red_wool";
        public TowerShape towers = TowerShape.ROUND;
        public boolean moat;
        public boolean concentric;
        public boolean keep = true;
        public boolean innerHouses = true;
        public boolean raised;
        public boolean ruined;
        public double towerScale = 1.0;
        public double heightScale = 1.0;
        public int coneHeight = 2; // multiplicateur du rayon
        public House.Spec keepSpec = House.spec().walls("stone_bricks", "cobblestone").foundation("cobblestone")
                .trim("stone_bricks").roof(RoofType.FLAT, "stone_brick").floors(3).depth(1.0).window("iron_bars").door("dark_oak")
                .furniture("dark_oak").with(House.Feature.ROOMS);
        public House.Spec houseSpec = House.spec().walls("white_terracotta", "dark_oak_log[axis=y]").trim("dark_oak_log[axis=x]")
                .roof(RoofType.STEEP, "dark_oak").furniture("dark_oak");

        public Spec walls(String wall, String accent) { this.wall = B.mc(wall); this.accent = B.mc(accent); return this; }
        public Spec roofs(String mat) { roofMat = mat == null ? null : B.mc(mat); return this; }
        public Spec flag(String v) { flag = B.mc(v); return this; }
        public Spec ground(String ground, String path) { this.ground = B.mc(ground); this.path = B.mc(path); return this; }
        public Spec towers(TowerShape t, double scale) { towers = t; towerScale = scale; return this; }
        public Spec moat() { moat = true; return this; }
        public Spec concentric() { concentric = true; return this; }
        public Spec raised() { raised = true; return this; }
        public Spec ruined() { ruined = true; return this; }
        public Spec noKeep() { keep = false; return this; }
        public Spec noHouses() { innerHouses = false; return this; }
        public Spec height(double k) { heightScale = k; return this; }
        public Spec cone(int k) { coneHeight = k; return this; }
        public Spec keep(House.Spec s) { keepSpec = s; return this; }
        public Spec houses(House.Spec s) { houseSpec = s; return this; }
    }

    private Castle() {
    }

    public static Spec spec() {
        return new Spec();
    }

    public static VoxelModel generate(Spec s, int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        build(g, s, width, seed, 0);
        if (s.ruined) {
            ruin(g, seed);
        }
        return g.freeze();
    }

    public static void build(VoxelGrid g, Spec s, int width, long seed, int oy) {
        Rng rng = new Rng(seed);
        int w = Math.max(17, width);
        int base = oy;
        Rect outer = Rect.centered(w, w);
        if (s.raised) {
            int h = Math.max(4, w / 8);
            for (int i = 0; i < h; i++) {
                Rect lr = outer.grow(h - i + 2);
                g.fill(lr.x0(), oy + i, lr.z0(), lr.x1(), oy + i, lr.z1(), i == h - 1 ? s.ground : "minecraft:stone");
            }
            for (int i = 0; i < h; i++) {
                g.set(0, oy + i, outer.z1() + h - i + 2, B.stairs("stone_brick", B.N, false));
                g.set(-1, oy + i, outer.z1() + h - i + 2, B.stairs("stone_brick", B.N, false));
                g.set(1, oy + i, outer.z1() + h - i + 2, B.stairs("stone_brick", B.N, false));
            }
            base = oy + h - 1;
        }
        if (s.moat) {
            moat(g, outer, base);
        }
        ring(g, s, outer, base, w, true);
        if (s.concentric && w >= 41) {
            Rect inner = outer.inset(Math.max(8, w / 6));
            courtyard(g, s, outer, base, rng, false);
            ring(g, s, inner, base, inner.width(), false);
            courtyard(g, s, inner, base, rng, true);
        } else {
            courtyard(g, s, outer, base, rng, true);
        }
    }

    /** Enceinte complète : murs, tours, porte. */
    private static void ring(VoxelGrid g, Spec s, Rect r, int base, int w, boolean outerRing) {
        int wallH = (int) Math.round(Math.max(6, Math.min(20, w / 5 + 3)) * s.heightScale);
        int t = w >= 60 ? 3 : 2;
        int tr = Math.max(2, (int) Math.round(w / 12.0 * s.towerScale));
        for (int i = 0; i < t; i++) {
            Arch.walls(g, r.inset(i), base, base + wallH, s.wall);
        }
        Arch.walls(g, r, base, base, s.accent);
        // Chemin de ronde et créneaux
        for (int i = 0; i < t; i++) {
            Arch.walls(g, r.inset(i), base + wallH, base + wallH, s.accent);
        }
        Arch.crenellate(g, r, base + wallH + 1, s.wall);
        // Meurtrières
        for (int x = r.x0() + 3; x <= r.x1() - 3; x += 4) {
            g.set(x, base + wallH - 2, r.z0(), "minecraft:iron_bars");
            g.set(x, base + wallH - 2, r.z1(), "minecraft:iron_bars");
        }
        for (int z = r.z0() + 3; z <= r.z1() - 3; z += 4) {
            g.set(r.x0(), base + wallH - 2, z, "minecraft:iron_bars");
            g.set(r.x1(), base + wallH - 2, z, "minecraft:iron_bars");
        }
        // Tours d'angle et intermédiaires
        int th = wallH + Math.max(4, tr + 2);
        int[][] spots = {{r.x0(), r.z0()}, {r.x1(), r.z0()}, {r.x0(), r.z1()}, {r.x1(), r.z1()}};
        for (int[] p : spots) {
            tower(g, s, p[0], base, p[1], tr, th);
        }
        if (w >= 55) {
            tower(g, s, r.cx(), base, r.z0(), tr, th);
            tower(g, s, r.x0(), base, r.cz(), tr, th);
            tower(g, s, r.x1(), base, r.cz(), tr, th);
        }
        gate(g, s, r, base, wallH, t, outerRing && s.moat);
        // Escaliers d'accès au chemin de ronde (côté cour)
        int sx = r.x0() + t;
        for (int i = 0; i < wallH; i++) {
            g.set(sx + i + 2, base + 1 + i, r.z1() - t, B.stairs("stone_brick", B.E, false));
            g.fill(sx + i + 2, base + 2 + i, r.z1() - t, sx + i + 2, base + 4 + i, r.z1() - t, B.AIR);
        }
    }

    public static void tower(VoxelGrid g, Spec s, int cx, int base, int cz, int r, int h) {
        if (s.towers == TowerShape.ROUND) {
            Arch.cylinder(g, cx, base, cz, r, h, s.wall, s.floor, 5);
            Arch.ring(g, cx, base + h, cz, r + 1, s.accent);
            Arch.disk(g, cx, base + h, cz, r, s.floor);
            if (s.roofMat != null) {
                Arch.cone(g, cx, base + h + 1, cz, r + 1, Math.max(3, (r + 1) * s.coneHeight), s.roofMat);
                int top = base + h + 2 + Math.max(3, (r + 1) * s.coneHeight);
                flag(g, s, cx, top, cz);
            } else {
                Arch.crenelRing(g, cx, base + h + 1, cz, r + 1, s.wall);
                flag(g, s, cx, base + h + 1, cz);
            }
            for (int y = base + 3; y < base + h - 1; y += 5) {
                g.set(cx, y, cz + r, "minecraft:glass_pane");
                g.set(cx + r, y, cz, "minecraft:glass_pane");
                g.set(cx - r, y, cz, "minecraft:glass_pane");
            }
        } else {
            Rect tr = new Rect(cx - r, cz - r, cx + r, cz + r);
            for (int y = base; y < base + h; y++) {
                Arch.walls(g, tr, y, y, s.wall);
                Arch.air(g, tr.inset(1), y, y);
                if ((y - base) % 5 == 0) {
                    Arch.floor(g, tr.inset(1), y, s.floor);
                }
            }
            Arch.pillars(g, tr, base, base + h, s.accent);
            Arch.floor(g, tr.grow(1), base + h, s.accent);
            if (s.roofMat != null) {
                RoofMat rm = roofMatFor(s.roofMat);
                int top = Arch.hip(g, tr, base + h + 1, rm, 1, false);
                flag(g, s, cx, top, cz);
            } else {
                Arch.crenellate(g, tr.grow(1), base + h + 1, s.wall);
                flag(g, s, cx, base + h + 1, cz);
            }
        }
    }

    private static RoofMat roofMatFor(String full) {
        String id = full.substring(full.indexOf(':') + 1);
        if (id.endsWith("_planks")) {
            return RoofMat.of(id.substring(0, id.length() - 7));
        }
        return switch (id) {
            case "deepslate_tiles" -> RoofMat.of("deepslate_tile");
            case "stone_bricks" -> RoofMat.of("stone_brick");
            case "bricks" -> RoofMat.of("brick");
            case "nether_bricks" -> RoofMat.of("nether_brick");
            case "red_nether_bricks" -> RoofMat.of("red_nether_brick");
            case "prismarine_bricks" -> RoofMat.of("prismarine_brick");
            case "quartz_block" -> RoofMat.of("quartz");
            case "purpur_block" -> RoofMat.of("purpur");
            default -> RoofMat.of("dark_oak");
        };
    }

    private static void flag(VoxelGrid g, Spec s, int x, int y, int z) {
        g.fill(x, y, z, x, y + 3, z, "minecraft:oak_fence");
        g.set(x + 1, y + 3, z, s.flag);
        g.set(x + 2, y + 3, z, s.flag);
        g.set(x + 1, y + 2, z, s.flag);
    }

    private static void gate(VoxelGrid g, Spec s, Rect r, int base, int wallH, int t, boolean bridge) {
        int gx = r.cx();
        int gz = r.z1();
        // Tours de porte carrées
        for (int side = -1; side <= 1; side += 2) {
            Rect gt = new Rect(gx + side * 4 - 2, gz - 2, gx + side * 4 + 2, gz + 2);
            for (int y = base; y <= base + wallH + 3; y++) {
                Arch.walls(g, gt, y, y, s.wall);
                Arch.air(g, gt.inset(1), y, y);
            }
            Arch.floor(g, gt, base + wallH + 3, s.accent);
            Arch.crenellate(g, gt, base + wallH + 4, s.wall);
        }
        // Passage voûté et herse relevée
        g.fill(gx - 1, base + 1, gz - t - 2, gx + 1, base + 4, gz + 2, B.AIR);
        g.fill(gx - 1, base, gz - t - 2, gx + 1, base, gz + 2, s.path);
        g.fill(gx - 1, base + 4, gz + 1, gx + 1, base + 4, gz + 1, "minecraft:iron_bars");
        g.set(gx - 1, base + 4, gz + 2, B.stairs("stone_brick", B.E, true));
        g.set(gx + 1, base + 4, gz + 2, B.stairs("stone_brick", B.W, true));
        g.set(gx - 2, base + 3, gz + 3, B.wallTorch(B.S));
        g.set(gx + 2, base + 3, gz + 3, B.wallTorch(B.S));
        if (bridge) {
            g.fill(gx - 1, base, gz + 3, gx + 1, base, gz + 7, "minecraft:spruce_planks");
            g.set(gx - 2, base + 1, gz + 7, "minecraft:spruce_fence");
            g.set(gx + 2, base + 1, gz + 7, "minecraft:spruce_fence");
            Arch.line(g, gx - 2, base + 2, gz + 7, gx - 2, base + 5, gz + 2, "minecraft:chain[axis=y]");
            Arch.line(g, gx + 2, base + 2, gz + 7, gx + 2, base + 5, gz + 2, "minecraft:chain[axis=y]");
        }
    }

    private static void moat(VoxelGrid g, Rect outer, int base) {
        Rect m = outer.grow(6);
        Rect inner = outer.grow(1);
        for (int x = m.x0(); x <= m.x1(); x++) {
            for (int z = m.z0(); z <= m.z1(); z++) {
                if (inner.contains(x, z)) {
                    continue;
                }
                boolean edge = m.onEdge(x, z);
                g.set(x, base - 3, z, "minecraft:stone_bricks");
                if (edge) {
                    g.fill(x, base - 2, z, x, base, z, "minecraft:stone_bricks");
                } else {
                    g.fill(x, base - 2, z, x, base, z, "minecraft:water[level=0]");
                }
            }
        }
        // Berge intérieure : sous les murs, déjà pleine ; on ferme le pied de l'enceinte.
        Arch.walls(g, inner, base - 3, base, "minecraft:stone_bricks");
    }

    private static void courtyard(VoxelGrid g, Spec s, Rect r, int base, Rng rng, boolean withKeep) {
        Rect in = r.inset(w3(r));
        for (int x = in.x0(); x <= in.x1(); x++) {
            for (int z = in.z0(); z <= in.z1(); z++) {
                if (!g.isSet(x, base, z) || B.isAir(g.get(x, base, z))) {
                    boolean path = Math.abs(x - r.cx()) <= 1 || Math.abs(z - r.cz()) <= 0;
                    g.set(x, base, z, path ? s.path : s.ground);
                    for (int y = base + 1; y <= base + 3; y++) {
                        if (!g.isSet(x, y, z)) {
                            g.set(x, y, z, B.AIR);
                        }
                    }
                }
            }
        }
        if (!withKeep) {
            return;
        }
        int w = r.width();
        if (s.keep) {
            int kw = Math.max(7, w / 3);
            if (kw % 2 == 0) {
                kw++;
            }
            int kz = r.cz() - Math.max(0, w / 10);
            House.build(g, s.keepSpec, kw, rng.nextLong(), r.cx(), base, kz);
            Rect k = Rect.centered(kw, Math.max(5, (int) Math.round(kw * s.keepSpec.depthRatio))).translate(r.cx(), kz);
            int top = base + House.floorsFor(s.keepSpec, kw) * s.keepSpec.floorHeight;
            if (s.keepSpec.roofType == RoofType.FLAT) {
                Arch.crenellate(g, k, top + 1, s.wall);
                tower(g, s, k.x0(), base, k.z0(), 1, top - base + 3);
                tower(g, s, k.x1(), base, k.z0(), 1, top - base + 3);
            }
        }
        if (s.innerHouses && w >= 33) {
            int hw = Math.max(7, w / 6);
            int off = w / 2 - hw / 2 - w3(r) - 2;
            House.build(g, s.houseSpec, hw, rng.nextLong(), r.cx() - off, base, r.cz() + off / 2);
            House.build(g, s.houseSpec, hw, rng.nextLong(), r.cx() + off, base, r.cz() + off / 2);
        }
        // Puits
        int wx = r.cx() + Math.max(4, w / 5), wz = r.cz() + Math.max(4, w / 4);
        Rect well = new Rect(wx - 1, wz - 1, wx + 1, wz + 1);
        g.fill(well.x0(), base - 3, well.z0(), well.x1(), base, well.z1(), "minecraft:cobblestone");
        g.fill(wx, base - 2, wz, wx, base, wz, "minecraft:water[level=0]");
        Arch.walls(g, well, base + 1, base + 1, "minecraft:cobblestone_wall");
        g.set(wx, base + 1, wz, B.AIR);
        Arch.pillars(g, well, base + 2, base + 3, "minecraft:oak_fence");
        g.fill(well.x0(), base + 4, well.z0(), well.x1(), base + 4, well.z1(), "minecraft:spruce_slab[type=bottom]");
        if (rng.chance(0.8)) {
            g.set(r.cx() - Math.max(4, w / 5), base + 1, r.cz() + Math.max(4, w / 4), "minecraft:hay_block[axis=y]");
        }
    }

    private static int w3(Rect r) {
        return r.width() >= 60 ? 3 : 2;
    }

    /** Ruines : érosion par bruit, surtout en hauteur, et végétation. */
    private static void ruin(VoxelGrid g, long seed) {
        java.util.List<int[]> remove = new java.util.ArrayList<>();
        java.util.List<int[]> moss = new java.util.ArrayList<>();
        g.forEach((x, y, z, st) -> {
            if (B.isAir(st) || B.isFluid(st) || y <= 1) {
                return;
            }
            if (B.isAttachment(st)) {
                remove.add(new int[]{x, y, z});
                return;
            }
            double n = Noise.value3(seed, x / 6.0, y / 5.0, z / 6.0);
            if (n + y * 0.012 > 0.62) {
                remove.add(new int[]{x, y, z});
            } else if (st.contains("stone_bricks") && Noise.hash(seed, x, y, z) < 0.3) {
                moss.add(new int[]{x, y, z});
            }
        });
        for (int[] p : remove) {
            g.remove(p[0], p[1], p[2]);
        }
        for (int[] p : moss) {
            g.set(p[0], p[1], p[2], Noise.hash(seed + 1, p[0], p[1], p[2]) < 0.5 ? "minecraft:mossy_stone_bricks" : "minecraft:cracked_stone_bricks");
        }
    }
}
