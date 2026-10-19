package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.House;
import com.ultimateautobuilder.core.generator.RoofMat;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

/** 15 structures diverses : ponts, infrastructures, attractions, ferme. */
public final class MiscModels {
    private MiscModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.MISC);
        SizeSpec LONG = SizeSpec.length(21, 41, 81, 161);
        r.gen("pont_suspendu", "Pont suspendu", 4, LONG, p -> suspensionBridge(p.size()), "pont", "suspendu", "golden gate", "cables");
        r.gen("pont_pierre", "Pont en pierre à arches", 3, LONG, p -> stoneBridge(p.size()), "pont", "pierre", "arches", "riviere");
        r.gen("aqueduc", "Aqueduc romain", 3, LONG, p -> aqueduct(p.size()), "aqueduc", "rome", "arches", "eau");
        r.gen("phare", "Phare", 3, SizeSpec.width(7, 11, 17, 27), p -> lighthouse(p.size()), "phare", "mer", "lumiere");
        r.gen("moulin_vent", "Moulin à vent", 3, SizeSpec.width(7, 11, 17, 27), p -> windmill(p.size()), "moulin", "vent", "ailes", "ferme");
        r.gen("eolienne", "Éolienne", 2, SizeSpec.height(16, 36, 70, 140), p -> windTurbine(p.size()), "eolienne", "energie", "ecologie");
        r.gen("grande_roue", "Grande roue", 4, SizeSpec.height(16, 36, 70, 130), p -> ferrisWheel(p.size()), "grande roue", "fete foraine", "attraction");
        r.gen("carrousel", "Carrousel", 3, SizeSpec.width(9, 15, 25, 41), p -> carousel(p.size()), "carrousel", "manege", "fete foraine");
        r.gen("fontaine", "Fontaine monumentale", 3, SizeSpec.width(9, 15, 25, 41), p -> fountain(p.size()), "fontaine", "eau", "place");
        r.gen("kiosque", "Kiosque à musique", 2, SizeSpec.width(7, 11, 17, 27), p -> gazebo(p.size()), "kiosque", "gloriette", "parc");
        r.gen("serre", "Grande serre", 3, Reg.BUILDING, p -> greenhouse(p.size(), p.seed()), "serre", "jardin", "plantes", "verre");
        r.gen("ferme_decorative", "Ferme décorative", 4, Reg.CASTLE, p -> farm(p.size(), p.seed()), "ferme", "champs", "grange", "animaux");
        r.gen("observatoire", "Observatoire astronomique", 3, Reg.HOUSE, p -> observatory(p.size()), "observatoire", "telescope", "etoiles");
        r.gen("puits", "Puits médiéval", 1, SizeSpec.width(5, 7, 11, 17), p -> well(p.size()), "puits", "eau", "village");
        r.gen("chateau_eau", "Château d'eau", 2, SizeSpec.height(12, 24, 48, 90), p -> waterTower(p.size()), "chateau d eau", "reservoir", "tour");
    }

    static VoxelModel suspensionBridge(int length) {
        VoxelGrid g = new VoxelGrid();
        int half = Math.max(10, length / 2);
        int deckY = Math.max(6, length / 8);
        int towerH = deckY + Math.max(10, length / 4);
        String red = "minecraft:red_concrete";
        g.fill(-3, deckY, -half, 3, deckY, half, "minecraft:gray_concrete");
        g.fill(0, deckY, -half, 0, deckY, half, "minecraft:white_concrete");
        g.fill(-3, deckY + 1, -half, -3, deckY + 1, half, "minecraft:iron_bars");
        g.fill(3, deckY + 1, -half, 3, deckY + 1, half, "minecraft:iron_bars");
        int tz = half * 2 / 3;
        for (int s = -1; s <= 1; s += 2) {
            for (int side = -1; side <= 1; side += 2) {
                g.fill(side * 4, 0, s * tz, side * 4 - side, towerH, s * tz + 1, red);
            }
            g.fill(-4, deckY + 4, s * tz, 4, deckY + 5, s * tz + 1, red);
            g.fill(-4, towerH - 2, s * tz, 4, towerH, s * tz + 1, red);
        }
        // Câbles porteurs paraboliques et suspentes
        for (int side = -1; side <= 1; side += 2) {
            int x = side * 4;
            for (int z = -half; z <= half; z++) {
                int y;
                if (Math.abs(z) <= tz) {
                    double t = (double) z / tz;
                    y = (int) Math.round(deckY + 2 + (towerH - deckY - 2) * t * t);
                } else {
                    double t = (double) (Math.abs(z) - tz) / (half - tz);
                    y = (int) Math.round(towerH - (towerH - deckY - 1) * t);
                }
                g.set(x, y, z, red);
                if (z % 3 == 0 && Math.abs(z) < tz) {
                    g.fill(x, deckY + 1, z, x, y - 1, z, "minecraft:chain[axis=y]");
                }
            }
        }
        g.fill(-6, -1, -half - 2, 6, -1, -half, "minecraft:stone");
        g.fill(-6, -1, half, 6, -1, half + 2, "minecraft:stone");
        return g.freeze();
    }

    static VoxelModel stoneBridge(int length) {
        VoxelGrid g = new VoxelGrid();
        int half = Math.max(10, length / 2);
        int h = Math.max(5, length / 8);
        int span = Math.max(6, length / 4);
        for (int z = -half; z <= half; z++) {
            int local = Math.floorMod(z + span / 2, span) - span / 2;
            int archTop = (int) Math.round(Math.sqrt(Math.max(0, 1 - Math.pow(2.0 * local / span, 2))) * (h - 1));
            for (int y = archTop; y <= h; y++) {
                g.fill(-2, y, z, 2, y, z, y == h ? "minecraft:stone_bricks" : "minecraft:cobblestone");
            }
            g.set(-3, h + 1, z, "minecraft:stone_brick_wall");
            g.set(3, h + 1, z, "minecraft:stone_brick_wall");
            g.fill(-2, h, z, 2, h, z, "minecraft:stone_bricks");
            g.fill(-1, h, z, 1, h, z, "minecraft:polished_andesite");
        }
        for (int z = -half; z <= half; z += 6) {
            g.set(-3, h + 2, z, B.lantern(false));
            g.set(3, h + 2, z, B.lantern(false));
        }
        return g.freeze();
    }

    static VoxelModel aqueduct(int length) {
        VoxelGrid g = new VoxelGrid();
        int half = Math.max(10, length / 2);
        int tier = Math.max(5, length / 6);
        String m = "minecraft:smooth_sandstone";
        for (int level = 0; level < 2; level++) {
            int y0 = level * tier;
            int span = level == 0 ? 6 : 3;
            for (int z = -half; z <= half; z++) {
                int local = Math.floorMod(z, span);
                boolean pier = local == 0;
                int archTop = pier ? y0 : y0 + (int) Math.round(Math.sqrt(Math.max(0, 1 - Math.pow(2.0 * (local - span / 2.0) / span, 2))) * (tier - 2));
                g.fill(-1, archTop, z, 1, y0 + tier - 1, z, m);
                if (pier) {
                    g.fill(-1, y0, z, 1, y0 + tier - 1, z, "minecraft:cut_sandstone");
                }
            }
        }
        int top = 2 * tier;
        g.fill(-2, top, -half, 2, top, half, m);
        g.fill(-2, top + 1, -half, -2, top + 1, half, m);
        g.fill(2, top + 1, -half, 2, top + 1, half, m);
        g.fill(-1, top + 1, -half + 1, 1, top + 1, half - 1, "minecraft:water[level=0]");
        g.fill(-1, top + 1, -half, 1, top + 1, -half, m);
        g.fill(-1, top + 1, half, 1, top + 1, half, m);
        return g.freeze();
    }

    public static VoxelModel lighthouse(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(3, width / 2);
        int h = r * 6;
        for (int y = 0; y < h; y++) {
            double ri = r - (double) y / h * (r * 0.35);
            String m = ((y / 4) & 1) == 0 ? "minecraft:white_concrete" : "minecraft:red_concrete";
            Arch.ring(g, 0, y, 0, ri, m);
            Arch.disk(g, 0, y, 0, Math.max(0, ri - 1.2), y % 6 == 0 ? "minecraft:spruce_planks" : B.AIR);
        }
        int ri = (int) Math.round(r * 0.65);
        Arch.disk(g, 0, h, 0, ri + 2, "minecraft:polished_andesite");
        Arch.ring(g, 0, h + 1, 0, ri + 2, "minecraft:iron_bars");
        Arch.ring(g, 0, h + 1, 0, Math.max(1, ri - 1), "minecraft:glass");
        Arch.ring(g, 0, h + 2, 0, Math.max(1, ri - 1), "minecraft:glass");
        Arch.disk(g, 0, h + 1, 0, Math.max(0, ri - 2), "minecraft:sea_lantern");
        Arch.disk(g, 0, h + 2, 0, Math.max(0, ri - 2), "minecraft:sea_lantern");
        Arch.dome(g, 0, h + 3, 0, ri, Math.max(2, ri), ri, "minecraft:red_concrete", false);
        for (int y = 1; y < h; y++) {
            g.set(0, y, -r + 2, "minecraft:ladder[facing=south]");
        }
        Arch.door(g, 0, 1, r, "spruce", B.S);
        return g.freeze();
    }

    static VoxelModel windmill(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(3, width / 2);
        int h = r * 4;
        for (int y = 0; y < h; y++) {
            double ri = r - (double) y / h * r * 0.3;
            Arch.ring(g, 0, y, 0, ri, "minecraft:white_terracotta");
            Arch.disk(g, 0, y, 0, Math.max(0, ri - 1.2), y % 5 == 0 ? "minecraft:spruce_planks" : B.AIR);
        }
        Arch.cone(g, 0, h, 0, (int) Math.round(r * 0.8), r + 2, "minecraft:dark_oak_planks");
        int hubZ = (int) Math.round(r * 0.8) + 1;
        int hubY = h;
        g.set(0, hubY, hubZ, "minecraft:dark_oak_log[axis=z]");
        g.set(0, hubY, hubZ + 1, "minecraft:dark_oak_log[axis=z]");
        int len = r * 3;
        for (int a = 0; a < 4; a++) {
            double ang = Math.toRadians(a * 90 + 20);
            int x1 = (int) Math.round(Math.cos(ang) * len), y1 = (int) Math.round(Math.sin(ang) * len);
            Arch.line(g, 0, hubY, hubZ + 1, x1, hubY + y1, hubZ + 1, "minecraft:spruce_log[axis=z]");
            double perp = ang + Math.PI / 2;
            for (int i = 2; i <= len; i++) {
                int bx = (int) Math.round(Math.cos(ang) * i), by = (int) Math.round(Math.sin(ang) * i);
                for (int w = 1; w <= 2; w++) {
                    int px = bx + (int) Math.round(Math.cos(perp) * w), py = by + (int) Math.round(Math.sin(perp) * w);
                    g.setIfEmpty(px, hubY + py, hubZ + 1, "minecraft:white_wool");
                }
            }
        }
        Arch.door(g, 0, 1, r, "dark_oak", B.S);
        return g.freeze();
    }

    static VoxelModel windTurbine(int height) {
        VoxelGrid g = new VoxelGrid();
        int h = Math.max(16, height * 2 / 3);
        for (int y = 0; y < h; y++) {
            Arch.disk(g, 0, y, 0, y < h / 2 ? 1.4 : 1.0, "minecraft:white_concrete");
        }
        g.fill(-1, h, -1, 1, h + 2, 4, "minecraft:white_concrete");
        g.set(0, h + 1, 5, "minecraft:light_gray_concrete");
        int len = Math.max(6, height / 3);
        for (int a = 0; a < 3; a++) {
            double ang = Math.toRadians(a * 120 + 90);
            Arch.line(g, 0, h + 1, 5, (int) Math.round(Math.cos(ang) * len), h + 1 + (int) Math.round(Math.sin(ang) * len), 5,
                    "minecraft:white_concrete");
        }
        Arch.disk(g, 0, 0, 0, 3, "minecraft:smooth_stone");
        return g.freeze();
    }

    static VoxelModel ferrisWheel(int height) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(7, height / 2 - 2);
        int cy = r + 3;
        String frame = "minecraft:white_concrete";
        for (int side = -1; side <= 1; side += 2) {
            int x = side * 2;
            for (int a = 0; a < 360; a += 2) {
                double rad = Math.toRadians(a);
                g.set(x, cy + (int) Math.round(Math.sin(rad) * r), (int) Math.round(Math.cos(rad) * r), frame);
            }
            for (int a = 0; a < 360; a += 30) {
                double rad = Math.toRadians(a);
                Arch.line(g, x, cy, 0, x, cy + (int) Math.round(Math.sin(rad) * r), (int) Math.round(Math.cos(rad) * r), "minecraft:iron_bars");
            }
            Arch.line(g, side * 3, 0, -r / 2, side * 2, cy, 0, "minecraft:light_gray_concrete");
            Arch.line(g, side * 3, 0, r / 2, side * 2, cy, 0, "minecraft:light_gray_concrete");
        }
        g.fill(-2, cy, 0, 2, cy, 0, "minecraft:iron_block");
        String[] colors = {"red", "yellow", "blue", "green", "orange", "purple"};
        for (int i = 0; i < 12; i++) {
            double rad = Math.toRadians(i * 30);
            int y = cy + (int) Math.round(Math.sin(rad) * r), z = (int) Math.round(Math.cos(rad) * r);
            g.fill(-1, y - 2, z, 1, y - 1, z, B.concrete(colors[i % colors.length]));
            g.set(0, y - 1, z, "minecraft:glass");
        }
        Arch.floor(g, Rect.centered(9, r * 2 + 4), 0, "minecraft:smooth_stone");
        return g.freeze();
    }

    static VoxelModel carousel(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(4, width / 2);
        Arch.disk(g, 0, 0, 0, r, "minecraft:polished_diorite");
        Arch.disk(g, 0, 1, 0, r - 0.5, "minecraft:smooth_quartz_slab[type=bottom]");
        g.fill(0, 1, 0, 0, 6, 0, "minecraft:gold_block");
        Arch.cylinder(g, 0, 1, 0, 1, 5, "minecraft:yellow_stained_glass", "minecraft:gold_block", 0);
        String[] colors = {"red", "white", "blue", "white"};
        for (int i = 0; i <= r / 2 + 1; i++) {
            Arch.ring(g, 0, 7 + i, 0, r + 1 - i * 2, B.wool(colors[i % colors.length]));
            Arch.disk(g, 0, 7 + i, 0, r - i * 2, B.wool(colors[(i + 1) % colors.length]));
        }
        int n = Math.max(6, r * 2);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            int x = (int) Math.round(Math.cos(a) * (r - 1.5)), z = (int) Math.round(Math.sin(a) * (r - 1.5));
            g.fill(x, 2, z, x, 6, z, "minecraft:end_rod[facing=up]");
            g.set(x, 3, z, B.wool(i % 2 == 0 ? "white" : "brown"));
        }
        return g.freeze();
    }

    static VoxelModel fountain(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(4, width / 2);
        Arch.disk(g, 0, -1, 0, r + 1, "minecraft:stone_bricks");
        Arch.disk(g, 0, 0, 0, r + 1, "minecraft:polished_andesite");
        Arch.ring(g, 0, 1, 0, r, "minecraft:stone_bricks");
        Arch.disk(g, 0, 1, 0, r - 1, "minecraft:water[level=0]");
        g.fill(0, 1, 0, 0, r, 0, "minecraft:quartz_pillar[axis=y]");
        int r2 = Math.max(2, r / 2);
        Arch.disk(g, 0, r / 2 + 1, 0, r2, "minecraft:smooth_quartz");
        Arch.ring(g, 0, r / 2 + 2, 0, r2, "minecraft:quartz_slab[type=bottom]");
        Arch.disk(g, 0, r / 2 + 2, 0, r2 - 1, "minecraft:water[level=0]");
        g.set(0, r / 2 + 2, 0, "minecraft:quartz_pillar[axis=y]");
        g.set(0, r + 1, 0, "minecraft:sea_lantern");
        return g.freeze();
    }

    public static VoxelModel gazebo(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(3, width / 2);
        Arch.disk(g, 0, 0, 0, r, "minecraft:white_concrete");
        Arch.disk(g, 0, 1, 0, r - 0.6, "minecraft:spruce_planks");
        int n = 8;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            int x = (int) Math.round(Math.cos(a) * (r - 1)), z = (int) Math.round(Math.sin(a) * (r - 1));
            g.fill(x, 2, z, x, 5, z, "minecraft:white_concrete");
            g.set(x, 2, z, "minecraft:white_concrete");
        }
        Arch.ring(g, 0, 2, 0, r - 1, "minecraft:white_stained_glass_pane");
        g.fill(-1, 2, r - 1, 1, 2, r - 1, B.AIR);
        Arch.cone(g, 0, 6, 0, r, r, "minecraft:green_concrete");
        g.set(0, 5, 0, B.lantern(true));
        return g.freeze();
    }

    static VoxelModel greenhouse(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(11, width) | 1;
        int len = w * 3 / 2;
        int r = w / 2;
        for (int z = -len / 2; z <= len / 2; z++) {
            for (int x = -r; x <= r; x++) {
                int y = (int) Math.round(Math.sqrt(Math.max(0, r * r - x * x)));
                boolean frame = z % 4 == 0 || Math.abs(z) == len / 2;
                for (int yy = 1; yy <= y; yy++) {
                    boolean edge = Math.abs(z) == len / 2 || yy == y || (Math.abs(x) == r);
                    if (edge) {
                        g.set(x, yy, z, frame ? "minecraft:white_concrete" : "minecraft:glass");
                    } else {
                        g.set(x, yy, z, B.AIR);
                    }
                }
                g.set(x, 0, z, Math.abs(x) <= 1 ? "minecraft:smooth_stone" : "minecraft:farmland[moisture=7]");
                if (Math.abs(x) > 1 && Math.abs(x) < r - 1 && Math.abs(z) < len / 2 - 1) {
                    String[] plants = {"minecraft:fern", "minecraft:poppy", "minecraft:azure_bluet", "minecraft:sweet_berry_bush[age=3]",
                            "minecraft:carrots[age=7]", "minecraft:potatoes[age=7]"};
                    g.set(x, 1, z, plants[rng.nextInt(plants.length)]);
                }
            }
        }
        g.fill(-1, 1, len / 2, 1, 3, len / 2, B.AIR);
        g.set(0, r - 1, 0, B.lantern(true));
        return g.freeze();
    }

    static VoxelModel farm(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int w = Math.max(25, width);
        int half = w / 2;
        House.Spec barn = House.spec().walls("red_terracotta", "white_concrete").trim("white_concrete").roof(RoofType.STEEP, "dark_oak")
                .door("spruce").furniture("spruce").depth(1.2);
        House.build(g, barn, Math.max(9, w / 4), seed, -half / 2, 0, -half / 2);
        House.build(g, House.spec().walls("white_terracotta", "oak_log[axis=y]").roof(RoofType.GABLE, "spruce")
                .with(House.Feature.PORCH, House.Feature.CHIMNEY), Math.max(7, w / 5), seed + 1, half / 2, 0, -half / 2);
        String[] crops = {"minecraft:wheat[age=7]", "minecraft:carrots[age=7]", "minecraft:potatoes[age=7]", "minecraft:beetroots[age=3]"};
        int fz0 = 2, fz1 = half - 2;
        int fieldW = Math.max(5, (w - 6) / 4);
        for (int f = 0; f < 4; f++) {
            int x0 = -half + 2 + f * (fieldW + 1);
            for (int x = x0; x < x0 + fieldW; x++) {
                for (int z = fz0; z <= fz1; z++) {
                    boolean water = (x - x0) == fieldW / 2 && (z - fz0) % 4 == 0;
                    g.set(x, 0, z, water ? "minecraft:water[level=0]" : "minecraft:farmland[moisture=7]");
                    if (!water) {
                        g.set(x, 1, z, crops[f]);
                    }
                }
            }
        }
        Rect pen = new Rect(-half, -half - 10, -half + 12, -half - 2);
        for (int x = pen.x0(); x <= pen.x1(); x++) {
            for (int z = pen.z0(); z <= pen.z1(); z++) {
                g.set(x, 0, z, "minecraft:grass_block");
                if (pen.onEdge(x, z)) {
                    g.set(x, 1, z, "minecraft:oak_fence");
                }
            }
        }
        g.set(pen.cx(), 1, pen.z1(), "minecraft:oak_fence_gate[facing=south,open=false,in_wall=false]");
        g.set(pen.x0() + 2, 1, pen.z0() + 2, "minecraft:hay_block[axis=y]");
        g.set(pen.x0() + 3, 1, pen.z0() + 2, "minecraft:water_cauldron[level=3]");
        for (int i = 0; i < 6; i++) {
            g.set(rng.range(-half, half), 1, rng.range(half - 1, half + 3), "minecraft:hay_block[axis=y]");
        }
        g.set(half - 3, 1, 0, "minecraft:composter[level=0]");
        return g.freeze();
    }

    static VoxelModel observatory(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(4, width / 2);
        Arch.cylinder(g, 0, 0, 0, r, 6, "minecraft:white_concrete", "minecraft:polished_andesite", 0);
        Arch.dome(g, 0, 6, 0, r, r, r, "minecraft:light_gray_concrete", true);
        for (int y = 6; y <= 6 + r; y++) {
            g.fill(0, y, 0, 1, y, r, B.AIR);
        }
        Arch.line(g, 0, 3, -1, 0, 6 + r - 1, r - 1, "minecraft:iron_block");
        g.set(0, 2, -1, "minecraft:iron_block");
        g.fill(-1, 1, -1, 1, 1, -1, "minecraft:polished_blackstone");
        Arch.door(g, 0, 1, r, "iron", B.S);
        return g.freeze();
    }

    static VoxelModel well(int width) {
        VoxelGrid g = new VoxelGrid();
        int r = Math.max(1, width / 3);
        Rect wr = Rect.centered(r * 2 + 3, r * 2 + 3);
        g.fill(wr.x0(), -4, wr.z0(), wr.x1(), 0, wr.z1(), "minecraft:cobblestone");
        g.fill(-r, -3, -r, r, 0, r, "minecraft:water[level=0]");
        Arch.walls(g, wr, 1, 1, "minecraft:cobblestone_wall");
        Arch.pillars(g, wr, 2, 3, "minecraft:oak_fence");
        Arch.roof(g, RoofType.GABLE, wr, 4, RoofMat.of("spruce"), 0, "minecraft:spruce_planks");
        g.set(0, 3, 0, "minecraft:chain[axis=y]");
        g.set(0, 2, 0, B.lantern(true));
        return g.freeze();
    }

    static VoxelModel waterTower(int height) {
        VoxelGrid g = new VoxelGrid();
        int h = Math.max(10, height * 2 / 3);
        int r = Math.max(3, height / 5);
        for (int[] c : new int[][]{{-r + 1, -r + 1}, {r - 1, -r + 1}, {-r + 1, r - 1}, {r - 1, r - 1}}) {
            g.fill(c[0], 0, c[1], c[0], h, c[1], "minecraft:iron_block");
        }
        for (int y = 3; y < h; y += 4) {
            Arch.line(g, -r + 1, y, -r + 1, r - 1, y + 3, -r + 1, "minecraft:iron_bars");
            Arch.line(g, -r + 1, y, r - 1, r - 1, y + 3, r - 1, "minecraft:iron_bars");
        }
        Arch.cylinder(g, 0, h, 0, r, r + 2, "minecraft:light_blue_terracotta", "minecraft:light_blue_terracotta", 0);
        Arch.disk(g, 0, h, 0, r, "minecraft:iron_block");
        Arch.cone(g, 0, h + r + 2, 0, r, Math.max(2, r / 2), "minecraft:gray_concrete");
        Arch.ring(g, 0, h, 0, r + 1, "minecraft:iron_bars");
        return g.freeze();
    }
}
