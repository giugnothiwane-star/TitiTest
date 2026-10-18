package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.RoofMat;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Noise;
import com.ultimateautobuilder.core.util.Rng;

import java.util.ArrayDeque;
import java.util.Deque;

/** 20 donjons, temples et monuments anciens. */
public final class DungeonModels {
    private DungeonModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.DUNGEONS);
        SizeSpec BLD = Reg.BUILDING;
        SizeSpec BIG = SizeSpec.width(21, 41, 65, 101);
        r.gen("parthenon", "Temple grec (Parthénon)", 4, BIG, p -> greekTemple(p.size()), "temple", "grece", "colonnes", "antique");
        r.gen("pyramide_egypte", "Grande pyramide d'Égypte", 3, BIG, p -> egyptPyramid(p.size()), "pyramide", "egypte", "pharaon");
        r.gen("pyramide_maya", "Pyramide maya", 4, BIG, p -> stepPyramid(p.size(), "minecraft:mossy_stone_bricks", "minecraft:stone_bricks",
                "minecraft:chiseled_stone_bricks", false), "pyramide", "maya", "jungle", "aztec");
        r.gen("temple_azteque", "Temple aztèque", 4, BIG, p -> stepPyramid(p.size(), "minecraft:smooth_sandstone", "minecraft:cut_red_sandstone",
                "minecraft:red_concrete", true), "temple", "azteque", "mexique", "soleil");
        r.gen("pagode", "Pagode à cinq étages", 4, BLD, p -> pagoda(p.size()), "pagode", "japon", "temple", "bouddhiste");
        r.gen("temple_chinois", "Temple chinois", 4, BLD, p -> chineseTemple(p.size()), "temple", "chine", "asie");
        r.gen("temple_jungle", "Temple de la jungle", 3, BLD, p -> jungleTemple(p.size(), p.seed()), "temple", "jungle", "aventure", "ruines");
        r.gen("temple_desert", "Temple du désert", 3, BLD, p -> desertTemple(p.size()), "temple", "desert", "minecraft");
        r.gen("donjon_souterrain", "Donjon souterrain", 4, BIG, p -> undergroundDungeon(p.size(), p.seed()), "donjon", "souterrain", "salles", "couloirs");
        r.gen("labyrinthe", "Labyrinthe de pierre", 3, BIG, p -> maze(p.size(), p.seed(), "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", 4),
                "labyrinthe", "dedale", "minotaure");
        r.gen("mausolee", "Mausolée", 3, BLD, p -> mausoleum(p.size()), "tombeau", "mausolee", "crypte");
        r.gen("cathedrale", "Cathédrale gothique", 5, BIG, p -> cathedral(p.size()), "cathedrale", "gothique", "eglise", "vitraux");
        r.gen("temple_dome", "Temple à dôme", 4, BLD, p -> domeTemple(p.size()), "temple", "dome", "coupole");
        r.gen("stonehenge", "Cercle de pierres levées", 2, BIG, p -> stonehenge(p.size(), p.seed()), "stonehenge", "megalithe", "druide");
        r.gen("angkor", "Temple aux cinq tours", 5, BIG, p -> angkor(p.size()), "angkor", "temple", "cambodge", "tours");
        r.gen("ziggurat", "Ziggourat", 3, BIG, p -> ziggurat(p.size()), "ziggurat", "mesopotamie", "babylone");
        r.gen("colisee", "Colisée", 5, SizeSpec.width(31, 61, 91, 141), p -> colosseum(p.size()), "colisee", "rome", "arene", "gladiateurs");
        r.gen("ruines_antiques", "Ruines antiques", 3, BIG, p -> ruins(p.size(), p.seed()), "ruines", "antique", "colonnes");
        r.gen("forteresse_nether", "Forteresse du Nether", 4, BIG, p -> netherFortress(p.size()), "nether", "forteresse", "minecraft", "blaze");
        r.gen("temple_glace", "Temple de glace", 4, BLD, p -> iceTemple(p.size()), "temple", "glace", "hiver", "givre");
    }

    // ------------------------------------------------------------------ antiquité

    static VoxelModel greekTemple(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        int d = w * 2 - 1;
        Rect base = Rect.centered(w, d);
        for (int i = 0; i < 3; i++) {
            Arch.floor(g, base.grow(2 - i), i, "minecraft:smooth_quartz");
        }
        int ch = Math.max(7, w / 2);
        Rect cella = base.inset(4);
        Arch.room(g, cella, 2, ch, "minecraft:quartz_bricks", "minecraft:smooth_quartz");
        for (int x = base.x0(); x <= base.x1(); x += 2) {
            for (int z = base.z0(); z <= base.z1(); z += 2) {
                if (base.onEdge(x, z)) {
                    g.fill(x, 3, z, x, ch + 2, z, "minecraft:quartz_pillar[axis=y]");
                }
            }
        }
        Arch.floor(g, base.grow(1), ch + 3, "minecraft:smooth_quartz");
        Arch.walls(g, base.grow(1), ch + 4, ch + 4, "minecraft:chiseled_quartz_block");
        Arch.gable(g, base.grow(1), ch + 5, RoofMat.of("quartz"), 0, "minecraft:quartz_bricks", false, 1);
        g.fill(cella.cx() - 1, 3, cella.z1(), cella.cx() + 1, 6, cella.z1(), B.AIR);
        g.fill(cella.cx(), 3, cella.z0() + 2, cella.cx(), 7, cella.z0() + 2, "minecraft:gold_block");
        return g.freeze();
    }

    static VoxelModel egyptPyramid(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        int half = w / 2;
        for (int y = 0; y <= half; y++) {
            Rect l = Rect.centered(w - 2 * y, w - 2 * y);
            Arch.floor(g, l, y, y >= half - 1 ? "minecraft:gold_block" : "minecraft:smooth_sandstone");
            if (y > 0 && y < half - 1) {
                Arch.air(g, l.inset(2), y, y);
            }
        }
        // Couloir d'entrée et chambre funéraire
        for (int z = 0; z <= half; z++) {
            g.fill(-1, 1, z, 1, 3, z, B.AIR);
        }
        Rect chamber = Rect.centered(5, 5);
        Arch.floor(g, chamber, 0, "minecraft:chiseled_sandstone");
        g.set(0, 1, 0, "minecraft:chest[facing=south,type=single]");
        g.set(-2, 2, -2, B.wallTorch(B.S));
        g.fill(-2, 1, half - 1, 2, 4, half - 1, "minecraft:chiseled_sandstone");
        g.fill(-1, 1, half - 1, 1, 3, half - 1, B.AIR);
        return g.freeze();
    }

    static VoxelModel stepPyramid(int width, String mat, String step, String accent, boolean twin) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        int tiers = Math.max(4, w / 6);
        int stepW = Math.max(2, (w / 2 - 4) / tiers);
        int y = 0;
        for (int t = 0; t < tiers; t++) {
            Rect l = Rect.centered(w - 2 * t * stepW, w - 2 * t * stepW);
            for (int k = 0; k < 3; k++) {
                Arch.floor(g, l, y + k, k == 1 ? step : mat);
            }
            y += 3;
        }
        // Escalier frontal
        for (int i = 0; i < y; i++) {
            int z = w / 2 - (i * stepW / 3);
            g.fill(-2, i, z, 2, i, z, B.stairs(mat.contains("sandstone") ? "smooth_sandstone" : "stone_brick", B.N, false));
            g.fill(-2, 0, z, 2, Math.max(0, i - 1), z, mat);
        }
        Rect top = Rect.centered(w - 2 * tiers * stepW + 2 * stepW, w - 2 * tiers * stepW + 2 * stepW);
        if (twin) {
            for (int side = -1; side <= 1; side += 2) {
                Rect shrine = Rect.centered(Math.max(3, top.width() / 2 - 1), Math.max(3, top.depth() - 2)).translate(side * top.width() / 4, 0);
                Arch.room(g, shrine, y - 1, 4, accent, mat);
                Arch.roof(g, RoofType.FLAT, shrine, y + 4, RoofMat.of("red_sandstone"), 0, accent);
            }
        } else {
            Rect shrine = Rect.centered(Math.max(5, top.width() - 2), Math.max(5, top.depth() - 2));
            Arch.room(g, shrine, y - 1, 4, step, mat);
            Arch.floor(g, shrine.grow(1), y + 4, accent);
            Arch.crenellate(g, shrine.grow(1), y + 5, accent);
            g.fill(-1, y, shrine.z1(), 1, y + 2, shrine.z1(), B.AIR);
        }
        return g.freeze();
    }

    static VoxelModel ziggurat(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(17, width) | 1;
        int y = 0;
        int cur = w;
        int t = 0;
        String[] mats = {"minecraft:mud_bricks", "minecraft:packed_mud", "minecraft:terracotta"};
        while (cur >= 7) {
            Rect l = Rect.centered(cur, cur * 3 / 4);
            int h = Math.max(3, w / 8);
            for (int k = 0; k < h; k++) {
                Arch.floor(g, l, y + k, mats[t % mats.length]);
            }
            y += h;
            cur -= Math.max(4, w / 5);
            t++;
        }
        for (int i = 0; i < y; i++) {
            g.fill(-1, i, w * 3 / 8 - i, 1, i, w * 3 / 8 - i, B.stairs("mud_brick", B.N, false));
        }
        Rect shrine = Rect.centered(5, 5);
        Arch.room(g, shrine, y, 4, "minecraft:blue_glazed_terracotta[facing=north]", "minecraft:mud_bricks");
        Arch.floor(g, shrine.grow(1), y + 5, "minecraft:gold_block");
        return g.freeze();
    }

    // ------------------------------------------------------------------ Asie

    public static VoxelModel pagoda(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(9, width) | 1;
        Arch.floor(g, Rect.centered(w + 4, w + 4), 0, "minecraft:stone_bricks");
        int y = 1;
        int tw = w;
        RoofMat roof = RoofMat.of("deepslate_tile");
        for (int tier = 0; tier < 5; tier++) {
            Rect t = Rect.centered(Math.max(3, tw), Math.max(3, tw));
            int h = 4;
            Arch.floor(g, t, y - 1, "minecraft:dark_oak_planks");
            Arch.walls(g, t, y, y + h - 2, "minecraft:red_concrete");
            Arch.air(g, t.inset(1), y, y + h - 2);
            Arch.pillars(g, t, y, y + h - 2, "minecraft:dark_oak_log[axis=y]");
            g.fill(t.cx(), y, t.z1(), t.cx(), y + 1, t.z1(), tier == 0 ? B.AIR : "minecraft:white_stained_glass_pane");
            Arch.hip(g, t, y + h - 1, roof, 2, true);
            Arch.air(g, t.inset(1), y + h - 1, y + h - 1);
            y += h;
            tw -= 2;
        }
        g.fill(0, y - 2, 0, 0, y + 5, 0, "minecraft:gold_block");
        for (int i = 0; i < 4; i++) {
            Arch.ring(g, 0, y + i, 0, 0.9, "minecraft:gold_block");
        }
        return g.freeze();
    }

    static VoxelModel chineseTemple(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(13, width) | 1;
        Rect base = Rect.centered(w + 4, w);
        for (int i = 0; i < 2; i++) {
            Arch.floor(g, base.grow(1 - i), i, "minecraft:polished_andesite");
        }
        Rect hall = Rect.centered(w, w * 2 / 3);
        for (int x = hall.x0(); x <= hall.x1(); x += 2) {
            g.fill(x, 2, hall.z1(), x, 6, hall.z1(), "minecraft:red_concrete");
        }
        Arch.room(g, hall.inset(1), 1, 5, "minecraft:red_terracotta", "minecraft:dark_oak_planks");
        Arch.floor(g, hall.grow(1), 7, "minecraft:dark_oak_planks");
        Arch.hip(g, hall, 8, RoofMat.of("deepslate_tile"), 2, true);
        Rect upper = hall.inset(2);
        Arch.walls(g, upper, 9, 10, "minecraft:red_concrete");
        Arch.hip(g, upper, 11, RoofMat.of("deepslate_tile"), 2, true);
        g.fill(-1, 2, hall.z1() - 1, 1, 4, hall.z1() - 1, B.AIR);
        g.set(0, 2, hall.z0() + 2, "minecraft:gold_block");
        g.set(0, 3, hall.z0() + 2, "minecraft:gold_block");
        for (int side = -1; side <= 1; side += 2) {
            int x = side * (w / 2 + 2);
            g.fill(x, 2, hall.z1() + 3, x, 3, hall.z1() + 3, "minecraft:stone_brick_wall");
            g.set(x, 4, hall.z1() + 3, B.lantern(false));
            Arch.cone(g, x, 5, hall.z1() + 3, 1, 1, "minecraft:red_concrete");
        }
        return g.freeze();
    }

    static VoxelModel angkor(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(21, width) | 1;
        String stone = "minecraft:tuff_bricks";
        Rect base = Rect.centered(w, w);
        for (int i = 0; i < 4; i++) {
            Rect l = base.inset(i * 2);
            Arch.floor(g, l, i * 2, stone);
            Arch.floor(g, l, i * 2 + 1, "minecraft:polished_tuff");
            Arch.walls(g, l, i * 2 + 2, i * 2 + 2, "minecraft:tuff_brick_wall");
        }
        int[][] towers = {{0, 0}, {-w / 3, -w / 3}, {w / 3, -w / 3}, {-w / 3, w / 3}, {w / 3, w / 3}};
        for (int i = 0; i < towers.length; i++) {
            int h = i == 0 ? w : w * 2 / 3;
            int r = i == 0 ? Math.max(3, w / 8) : Math.max(2, w / 11);
            int y0 = i == 0 ? 8 : 4;
            for (int y = 0; y < h; y++) {
                double ri = r * (1 - Math.pow((double) y / h, 1.6)) + 0.4;
                Rect l = Rect.centered((int) Math.round(ri * 2) | 1, (int) Math.round(ri * 2) | 1).translate(towers[i][0], towers[i][1]);
                Arch.walls(g, l, y0 + y, y0 + y, y % 3 == 0 ? "minecraft:chiseled_tuff" : stone);
            }
        }
        for (int i = 0; i < 8; i++) {
            g.fill(-1, i, w / 2 - i, 1, i, w / 2 - i, B.stairs("tuff_brick", B.N, false));
        }
        return g.freeze();
    }

    // ------------------------------------------------------------------ aventure

    static VoxelModel jungleTemple(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(13, width) | 1;
        Rect r = Rect.centered(w, w + 2);
        String stone = "minecraft:cobblestone";
        Arch.room(g, r, 0, 6, stone, "minecraft:mossy_cobblestone");
        Arch.floor(g, r, 7, stone);
        Rect up = r.inset(2);
        Arch.room(g, up, 7, 4, "minecraft:mossy_cobblestone", stone);
        Arch.floor(g, up, 12, stone);
        Arch.crenellate(g, up, 13, "minecraft:mossy_cobblestone");
        g.fill(-1, 1, r.z1(), 1, 3, r.z1(), B.AIR);
        g.fill(-1, 8, up.z1(), 1, 10, up.z1(), B.AIR);
        g.set(0, 1, r.z0() + 2, "minecraft:chest[facing=south,type=single]");
        g.set(-2, 1, r.z0() + 2, "minecraft:chiseled_stone_bricks");
        g.set(2, 1, r.z0() + 2, "minecraft:chiseled_stone_bricks");
        Rng rng = new Rng(seed);
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int y = 1; y <= 12; y++) {
                if (rng.chance(0.18) && "minecraft:cobblestone".equals(g.get(x, y, r.z1())) && g.get(x, y, r.z1() + 1) == null) {
                    g.set(x, y, r.z1() + 1, "minecraft:vine[south=false,north=true,east=false,west=false,up=false]");
                }
            }
        }
        for (int i = 0; i < 7; i++) {
            g.fill(-2, i, r.z1() + 1 + (7 - i), 2, i, r.z1() + 1 + (7 - i), B.stairs("cobblestone", B.N, false));
        }
        return g.freeze();
    }

    static VoxelModel desertTemple(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        Rect r = Rect.centered(w, w);
        for (int y = 0; y < w / 2; y++) {
            Rect l = r.inset(y / 2);
            Arch.walls(g, l, y, y, "minecraft:sandstone");
        }
        Arch.floor(g, r, 0, "minecraft:sandstone");
        Arch.air(g, r.inset(1), 1, w / 4);
        Arch.floor(g, r.inset(w / 4), w / 2, "minecraft:sandstone");
        g.set(0, 0, 0, "minecraft:blue_terracotta");
        g.set(1, 0, 0, "minecraft:orange_terracotta");
        g.set(-1, 0, 0, "minecraft:orange_terracotta");
        g.set(0, 0, 1, "minecraft:orange_terracotta");
        g.set(0, 0, -1, "minecraft:orange_terracotta");
        for (int side = -1; side <= 1; side += 2) {
            int tx = side * (w / 2 - 2);
            Rect t = Rect.centered(5, 5).translate(tx, r.z1() - 2);
            for (int y = 0; y < w / 2 + 4; y++) {
                Arch.walls(g, t, y, y, "minecraft:cut_sandstone");
                Arch.air(g, t.inset(1), y, y);
            }
            g.set(tx, w / 2 + 1, t.z1(), "minecraft:orange_terracotta");
            Arch.floor(g, t, w / 2 + 4, "minecraft:smooth_sandstone");
        }
        g.fill(-1, 1, r.z1(), 1, 3, r.z1(), B.AIR);
        g.fill(-1, 3, r.z1(), 1, 3, r.z1(), "minecraft:chiseled_sandstone");
        g.fill(-1, 1, r.z1(), 1, 2, r.z1(), B.AIR);
        g.set(0, 1, r.z0() + 2, "minecraft:chest[facing=south,type=single]");
        return g.freeze();
    }

    static VoxelModel undergroundDungeon(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(21, width);
        int depth = 9;
        Rect r = Rect.centered(w, w);
        // Bloc de roche dans lequel salles et couloirs sont creusés
        g.fill(r.x0(), -depth, r.z0(), r.x1(), -1, r.z1(), "minecraft:stone");
        int rooms = Math.max(4, w / 7);
        int[][] centers = new int[rooms][];
        for (int i = 0; i < rooms; i++) {
            int rw = rng.range(5, Math.max(6, w / 5)), rd = rng.range(5, Math.max(6, w / 5));
            int cx = rng.range(r.x0() + rw / 2 + 2, r.x1() - rw / 2 - 2), cz = rng.range(r.z0() + rd / 2 + 2, r.z1() - rd / 2 - 2);
            centers[i] = new int[]{cx, cz};
            Rect room = Rect.centered(rw, rd).translate(cx, cz);
            Arch.walls(g, room.grow(1), -depth + 1, -2, "minecraft:mossy_stone_bricks");
            Arch.air(g, room, -depth + 2, -4);
            Arch.floor(g, room, -depth + 1, "minecraft:cracked_stone_bricks");
            g.set(room.cx(), -5, room.cz(), B.lantern(true));
            if (rng.chance(0.5)) {
                g.set(room.x0(), -depth + 2, room.z0(), "minecraft:chest[facing=south,type=single]");
            }
            if (rng.chance(0.4)) {
                g.set(room.x1(), -depth + 2, room.z1(), "minecraft:cobweb");
            }
        }
        for (int i = 1; i < rooms; i++) {
            int[] a = centers[i - 1], b = centers[i];
            for (int x = Math.min(a[0], b[0]); x <= Math.max(a[0], b[0]); x++) {
                g.fill(x, -depth + 2, a[1], x, -depth + 4, a[1], B.AIR);
                g.set(x, -depth + 1, a[1], "minecraft:stone_bricks");
            }
            for (int z = Math.min(a[1], b[1]); z <= Math.max(a[1], b[1]); z++) {
                g.fill(b[0], -depth + 2, z, b[0], -depth + 4, z, B.AIR);
                g.set(b[0], -depth + 1, z, "minecraft:stone_bricks");
            }
        }
        // Entrée : escalier en colimaçon carré depuis la surface
        int ex = centers[0][0], ez = centers[0][1];
        Rect shaft = Rect.centered(3, 3).translate(ex, ez);
        for (int y = -depth + 2; y <= 0; y++) {
            Arch.air(g, shaft, y, y);
        }
        for (int y = -depth + 1; y <= 0; y++) {
            g.set(ex - 1, y, ez - 1, "minecraft:ladder[facing=south]");
        }
        g.fill(ex - 2, -1, ez - 2, ex - 2, -1, ez - 2, "minecraft:stone_bricks");
        Arch.walls(g, shaft.grow(1), 1, 2, "minecraft:mossy_stone_bricks");
        g.fill(ex, 1, shaft.z1() + 1, ex, 2, shaft.z1() + 1, B.AIR);
        Arch.floor(g, shaft.grow(1), 3, "minecraft:stone_brick_slab[type=bottom]");
        // L'ancrage reste l'origine de conception : la surface (y = 0) est posée au niveau du sol.
        return g.freeze();
    }

    /** Labyrinthe parfait par exploration en profondeur (pile explicite, sans récursion). */
    static VoxelModel maze(int width, long seed, String wall, String accent, int height) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int cells = Math.max(4, (width - 1) / 3);
        int size = cells * 3 + 1;
        boolean[][] open = new boolean[size][size];
        boolean[][] visited = new boolean[cells][cells];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        visited[0][0] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            int[] order = {0, 1, 2, 3};
            for (int i = 3; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int t = order[i];
                order[i] = order[j];
                order[j] = t;
            }
            boolean moved = false;
            for (int k : order) {
                int nx = c[0] + dirs[k][0], nz = c[1] + dirs[k][1];
                if (nx < 0 || nz < 0 || nx >= cells || nz >= cells || visited[nx][nz]) {
                    continue;
                }
                visited[nx][nz] = true;
                carveCell(open, c[0], c[1], nx, nz);
                stack.push(new int[]{nx, nz});
                moved = true;
                break;
            }
            if (!moved) {
                stack.pop();
            }
        }
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                carveCell(open, i, j, i, j);
            }
        }
        int half = size / 2;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int wx = x - half, wz = z - half;
                g.set(wx, 0, wz, "minecraft:smooth_stone");
                if (!open[x][z]) {
                    for (int y = 1; y <= height; y++) {
                        g.set(wx, y, wz, Noise.hash(seed, x, y, z) < 0.25 ? accent : wall);
                    }
                } else {
                    g.fill(wx, 1, wz, wx, height, wz, B.AIR);
                }
            }
        }
        // Entrée au sud, sortie au nord, trésor au centre
        g.fill(-half + 1, 1, half, -half + 2, height, half, B.AIR);
        g.fill(half - 2, 1, -half, half - 1, height, -half, B.AIR);
        g.set(0, 1, 0, "minecraft:chest[facing=south,type=single]");
        return g.freeze();
    }

    private static void carveCell(boolean[][] open, int ax, int az, int bx, int bz) {
        int x0 = Math.min(ax, bx) * 3 + 1, z0 = Math.min(az, bz) * 3 + 1;
        int x1 = Math.max(ax, bx) * 3 + 2, z1 = Math.max(az, bz) * 3 + 2;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                open[x][z] = true;
            }
        }
    }

    static VoxelModel mausoleum(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(9, width) | 1;
        Rect r = Rect.centered(w, w);
        Arch.floor(g, r.grow(1), 0, "minecraft:polished_andesite");
        Arch.room(g, r, 1, w / 2 + 2, "minecraft:polished_diorite", "minecraft:polished_andesite");
        for (int x = r.x0(); x <= r.x1(); x += 2) {
            g.fill(x, 2, r.z1() + 2, x, w / 2 + 3, r.z1() + 2, "minecraft:quartz_pillar[axis=y]");
        }
        Arch.floor(g, new Rect(r.x0(), r.z1() + 1, r.x1(), r.z1() + 2), w / 2 + 4, "minecraft:polished_diorite");
        Arch.dome(g, 0, w / 2 + 4, 0, w / 2, w / 2, w / 2, "minecraft:polished_andesite", true);
        g.fill(-1, 2, r.z1(), 1, 4, r.z1(), B.AIR);
        g.fill(-1, 2, 0, 1, 2, 2, "minecraft:polished_blackstone");
        g.set(0, 3, 0, "minecraft:candle[candles=4,lit=true]");
        Rect stairs = new Rect(-2, -6, 2, -2);
        Arch.room(g, Rect.centered(w - 2, w - 2), -6, 4, "minecraft:deepslate_bricks", "minecraft:deepslate_tiles");
        for (int i = 0; i < 6; i++) {
            g.set(r.x0() + 2 + i, 1 - i, r.z0() + 2, B.stairs("deepslate_brick", B.W, false));
            g.fill(r.x0() + 2 + i, 2 - i, r.z0() + 2, r.x0() + 2 + i, 3 - i, r.z0() + 2, B.AIR);
        }
        g.set(0, -5, 0, "minecraft:chest[facing=south,type=single]");
        g.set(stairs.cx(), -3, stairs.cz(), B.lantern(true));
        return g.freeze();
    }

    static VoxelModel cathedral(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(17, width) | 1;
        int len = w * 2;
        Rect nave = new Rect(-w / 4, -len / 2, w / 4, len / 2);
        int h = Math.max(10, w * 2 / 3);
        Arch.room(g, nave, 0, h, "minecraft:stone_bricks", "minecraft:polished_andesite");
        for (Rect aisle : new Rect[]{new Rect(-w / 2, -len / 2 + 2, -w / 4, len / 2 - 6), new Rect(w / 4, -len / 2 + 2, w / 2, len / 2 - 6)}) {
            Arch.room(g, aisle, 0, h / 2, "minecraft:stone_bricks", "minecraft:polished_andesite");
            Arch.roof(g, RoofType.FLAT, aisle, h / 2 + 1, RoofMat.of("stone_brick"), 0, "minecraft:stone_bricks");
            for (int z = aisle.z0() + 2; z < aisle.z1(); z += 4) {
                int bx = aisle.x0() < 0 ? aisle.x0() - 1 : aisle.x1() + 1;
                Arch.line(g, bx, 1, z, bx < 0 ? nave.x0() : nave.x1(), h - 1, z, "minecraft:stone_brick_wall");
            }
        }
        g.fill(-w / 4 + 1, 1, -len / 2 + 3, w / 4 - 1, h / 2 - 1, len / 2 - 7, B.AIR);
        String[] glass = {"minecraft:red_stained_glass", "minecraft:blue_stained_glass", "minecraft:yellow_stained_glass", "minecraft:purple_stained_glass"};
        for (int z = nave.z0() + 2; z <= nave.z1() - 2; z += 3) {
            for (int y = h / 2 + 2; y < h - 1; y++) {
                g.set(nave.x0(), y, z, glass[(y + z) & 3]);
                g.set(nave.x1(), y, z, glass[(y + z + 1) & 3]);
            }
        }
        Arch.gable(g, nave, h + 1, RoofMat.of("deepslate_tile"), 1, "minecraft:stone_bricks", false, 2);
        // Façade : deux tours et rosace
        for (int side = -1; side <= 1; side += 2) {
            Rect t = Rect.centered(Math.max(5, w / 4) | 1, Math.max(5, w / 4) | 1).translate(side * (w / 4 + 1), nave.z1());
            int th = h * 2;
            for (int y = 0; y < th; y++) {
                Arch.walls(g, t, y, y, "minecraft:stone_bricks");
                Arch.air(g, t.inset(1), y, y);
            }
            Arch.pillars(g, t, 0, th, "minecraft:polished_andesite");
            int top = Arch.hip(g, t, th, RoofMat.of("deepslate_tile"), 0, false);
            g.fill(t.cx(), top, t.cz(), t.cx(), top + 3, t.cz(), "minecraft:gold_block");
        }
        int ry = h - 2;
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d <= 3.4) {
                    g.set(dx, ry + dy, nave.z1(), d > 2.5 ? "minecraft:gold_block" : glass[Math.floorMod(dx * 3 + dy, 4)]);
                }
            }
        }
        g.fill(-1, 1, nave.z1(), 1, 5, nave.z1(), B.AIR);
        Arch.door(g, 0, 1, nave.z1(), "dark_oak", B.S);
        for (int z = nave.z0() + 4; z < nave.z1() - 3; z += 2) {
            g.fill(nave.x0() + 1, 1, z, -2, 1, z, B.stairs("dark_oak", B.N, false));
            g.fill(2, 1, z, nave.x1() - 1, 1, z, B.stairs("dark_oak", B.N, false));
        }
        g.fill(-2, 1, nave.z0() + 1, 2, 1, nave.z0() + 2, "minecraft:quartz_block");
        g.set(0, 2, nave.z0() + 1, "minecraft:gold_block");
        return g.freeze();
    }

    static VoxelModel domeTemple(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(13, width) | 1;
        int r = w / 2;
        Arch.floor(g, Rect.centered(w + 4, w + 4), 0, "minecraft:polished_diorite");
        Arch.cylinder(g, 0, 1, 0, r, Math.max(5, r), "minecraft:quartz_bricks", "minecraft:smooth_quartz", 0);
        for (int a = 0; a < 360; a += 30) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * (r + 2));
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * (r + 2));
            g.fill(x, 1, z, x, Math.max(5, r) + 1, z, "minecraft:quartz_pillar[axis=y]");
        }
        Arch.disk(g, 0, Math.max(5, r) + 1, 0, r + 2.5, "minecraft:smooth_quartz");
        Arch.dome(g, 0, Math.max(5, r) + 2, 0, r, r, r, "minecraft:oxidized_copper", true);
        g.fill(0, Math.max(5, r) + r + 2, 0, 0, Math.max(5, r) + r + 4, 0, "minecraft:gold_block");
        g.fill(-1, 2, r, 1, 4, r, B.AIR);
        return g.freeze();
    }

    static VoxelModel stonehenge(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(8, width / 2);
        int h = Math.max(4, r / 2);
        Arch.disk(g, 0, 0, 0, r + 3, "minecraft:grass_block");
        int n = Math.max(10, r * 2);
        for (int i = 0; i < n; i += 2) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            int x0 = (int) Math.round(Math.cos(a0) * r), z0 = (int) Math.round(Math.sin(a0) * r);
            int x1 = (int) Math.round(Math.cos(a1) * r), z1 = (int) Math.round(Math.sin(a1) * r);
            if (rng.chance(0.15)) {
                continue;
            }
            g.fill(x0, 1, z0, x0, h, z0, "minecraft:andesite");
            g.fill(x1, 1, z1, x1, h, z1, "minecraft:andesite");
            Arch.line(g, x0, h + 1, z0, x1, h + 1, z1, "minecraft:stone");
        }
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * (0.2 + i * 0.15);
            int x = (int) Math.round(Math.cos(a) * r / 2), z = (int) Math.round(Math.sin(a) * r / 2);
            g.fill(x, 1, z, x + 1, h + 2, z, "minecraft:andesite");
        }
        g.fill(-1, 1, -1, 1, 1, 0, "minecraft:polished_andesite");
        return g.freeze();
    }

    static VoxelModel colosseum(int width) {
        VoxelGrid g = new VoxelGrid();
        int rx = Math.max(15, width / 2), rz = rx * 4 / 5;
        int levels = 4;
        int lh = Math.max(4, rx / 6);
        for (int x = -rx; x <= rx; x++) {
            for (int z = -rz; z <= rz; z++) {
                double v = Math.sqrt((double) x * x / (rx * rx) + (double) z * z / (rz * rz));
                if (v > 1.0) {
                    continue;
                }
                if (v < 0.55) {
                    g.set(x, 0, z, "minecraft:sand");
                    continue;
                }
                int h = (int) Math.round((v - 0.55) / 0.45 * levels * lh);
                if (v > 0.93) {
                    double ang = Math.atan2(z / (double) rz, x / (double) rx);
                    int arch = (int) Math.floor((ang + Math.PI) / (Math.PI * 2) * 48);
                    int fullH = levels * lh + 1;
                    String m = "minecraft:smooth_sandstone";
                    for (int y = 0; y <= fullH; y++) {
                        int local = y % lh;
                        boolean opening = (arch & 1) == 0 && local >= 1 && local <= lh - 2 && y < fullH - 1;
                        if (Math.floorMod(x * 7 + z * 13, 97) < 18 && y > fullH - lh) {
                            continue; // érosion du dernier niveau
                        }
                        g.set(x, y, z, opening ? (v > 0.97 ? B.AIR : m) : m);
                    }
                } else {
                    g.fill(x, 0, z, x, Math.max(0, h - 1), z, "minecraft:sandstone");
                    g.set(x, h, z, B.stairs("smooth_sandstone", x * x * rz * rz > z * z * rx * rx ? (x > 0 ? B.W : B.E) : (z > 0 ? B.N : B.S), false));
                }
            }
        }
        g.fill(-2, 1, rz - 2, 2, lh - 2, rz + 1, B.AIR);
        return g.freeze();
    }

    static VoxelModel ruins(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(17, width);
        Rect r = Rect.centered(w, w * 2 / 3);
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int z = r.z0(); z <= r.z1(); z++) {
                g.set(x, 0, z, rng.chance(0.3) ? "minecraft:cracked_stone_bricks" : rng.chance(0.4) ? "minecraft:mossy_stone_bricks" : "minecraft:grass_block");
            }
        }
        for (int x = r.x0() + 1; x <= r.x1() - 1; x += 3) {
            for (int z : new int[]{r.z0() + 1, r.z1() - 1}) {
                int h = rng.chance(0.3) ? rng.range(1, 3) : rng.range(5, Math.max(6, w / 3));
                g.fill(x, 1, z, x, h, z, "minecraft:quartz_pillar[axis=y]");
                if (h > 4 && rng.chance(0.5) && x + 3 <= r.x1() - 1) {
                    g.fill(x, h + 1, z, x + 3, h + 1, z, "minecraft:smooth_quartz");
                }
                if (h <= 3) {
                    g.fill(x + 1, 1, z + (z < 0 ? 1 : -1), x + 3, 1, z + (z < 0 ? 1 : -1), "minecraft:quartz_pillar[axis=x]");
                }
            }
        }
        g.fill(-2, 1, -1, 2, 2, 1, "minecraft:chiseled_quartz_block");
        g.set(0, 3, 0, "minecraft:decorated_pot");
        return g.freeze();
    }

    static VoxelModel netherFortress(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(25, width);
        int half = w / 2;
        String nb = "minecraft:nether_bricks";
        int y = 10;
        for (int axis = 0; axis < 2; axis++) {
            for (int i = -half; i <= half; i++) {
                for (int k = -2; k <= 2; k++) {
                    int x = axis == 0 ? i : k, z = axis == 0 ? k : i;
                    g.set(x, y, z, nb);
                    if (Math.abs(k) == 2) {
                        g.set(x, y + 1, z, "minecraft:nether_brick_fence");
                    }
                }
                if (Math.floorMod(i, 8) == 0) {
                    for (int k = -2; k <= 2; k += 4) {
                        int x = axis == 0 ? i : k, z = axis == 0 ? k : i;
                        g.fill(x, 0, z, x, y - 1, z, nb);
                    }
                }
            }
        }
        Rect hall = Rect.centered(11, 11);
        Arch.room(g, hall, y, 6, nb, nb);
        Arch.windows(g, hall, y + 2, 2, 3, "minecraft:nether_brick_fence", null);
        Arch.floor(g, hall, y + 7, nb);
        Arch.crenellate(g, hall, y + 8, nb);
        g.fill(-2, y + 1, hall.z1(), 2, y + 4, hall.z1(), B.AIR);
        g.fill(-2, y + 1, hall.z0(), 2, y + 4, hall.z0(), B.AIR);
        g.fill(hall.x0(), y + 1, -2, hall.x0(), y + 4, 2, B.AIR);
        g.fill(hall.x1(), y + 1, -2, hall.x1(), y + 4, 2, B.AIR);
        g.fill(-1, y + 1, -1, 1, y + 1, 1, "minecraft:soul_sand");
        g.set(0, y + 2, 0, "minecraft:nether_wart[age=3]");
        g.set(3, y + 1, 3, "minecraft:chest[facing=north,type=single]");
        g.set(-3, y + 1, -3, "minecraft:magma_block");
        return g.freeze();
    }

    static VoxelModel iceTemple(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(13, width) | 1;
        Rect r = Rect.centered(w, w);
        Arch.floor(g, r.grow(2), 0, "minecraft:snow_block");
        Arch.room(g, r, 1, w / 2, "minecraft:packed_ice", "minecraft:blue_ice");
        Arch.windows(g, r, 3, 2, 3, "minecraft:light_blue_stained_glass", 0);
        for (int[] c : new int[][]{{r.x0(), r.z0()}, {r.x1(), r.z0()}, {r.x0(), r.z1()}, {r.x1(), r.z1()}}) {
            Arch.cylinder(g, c[0], 1, c[1], 2, w / 2 + 4, "minecraft:blue_ice", "minecraft:packed_ice", 0);
            Arch.cone(g, c[0], w / 2 + 5, c[1], 3, 6, "minecraft:packed_ice");
        }
        int top = w / 2 + 2;
        Arch.hip(g, r, top, RoofMat.of("prismarine"), 1, false);
        g.fill(-1, 2, r.z1(), 1, 4, r.z1(), B.AIR);
        g.fill(0, 2, 0, 0, 5, 0, "minecraft:blue_ice");
        g.set(0, 6, 0, "minecraft:sea_lantern");
        return g.freeze();
    }
}
