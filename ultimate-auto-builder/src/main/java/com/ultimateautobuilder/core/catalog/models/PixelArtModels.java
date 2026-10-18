package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;

import java.util.Map;

import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;

/** 12 pixel arts verticaux et 3 sculptures paramétriques. */
public final class PixelArtModels {
    private static final Map<Character, String> PALETTE = Map.ofEntries(
            Map.entry('K', "minecraft:black_concrete"), Map.entry('W', "minecraft:white_concrete"),
            Map.entry('R', "minecraft:red_concrete"), Map.entry('Y', "minecraft:yellow_concrete"),
            Map.entry('G', "minecraft:green_concrete"), Map.entry('L', "minecraft:lime_concrete"),
            Map.entry('C', "minecraft:light_blue_concrete"), Map.entry('D', "minecraft:cyan_concrete"),
            Map.entry('B', "minecraft:brown_concrete"), Map.entry('T', "minecraft:end_stone"),
            Map.entry('P', "minecraft:pink_concrete"), Map.entry('O', "minecraft:orange_concrete"),
            Map.entry('X', "minecraft:purple_concrete"), Map.entry('S', "minecraft:gray_concrete"),
            Map.entry('N', "minecraft:blue_concrete"));

    private PixelArtModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.PIXELART);
        SizeSpec S = Reg.PIXEL;
        art(r, "pixel_coeur", "Cœur", S, new String[]{
                "..RR....RR..",
                ".RWRR..RRRR.",
                "RWRRRRRRRRRR",
                "RRRRRRRRRRRR",
                "RRRRRRRRRRRR",
                ".RRRRRRRRRR.",
                "..RRRRRRRR..",
                "...RRRRRR...",
                "....RRRR....",
                ".....RR....."}, "pixel art", "coeur", "amour");
        art(r, "pixel_creeper", "Visage de Creeper", S, new String[]{
                "GLGGLGGL",
                "LGGLGGLG",
                "GKKGLKKG",
                "LKKLGKKL",
                "GLGKKGLG",
                "LGKKKKGL",
                "GLKKKKLG",
                "LGKGLKGL"}, "pixel art", "creeper", "minecraft");
        art(r, "pixel_epee", "Épée en diamant", S, new String[]{
                "..............KK",
                ".............KCK",
                "............KCDK",
                "...........KCDK.",
                "..........KCDK..",
                ".........KCDK...",
                "....KK..KCDK....",
                "....KDKKCDK.....",
                ".....KDCDK......",
                ".....KBDK.......",
                "....KBBKDK......",
                "...KBBK.KK......",
                ".KKBBK..........",
                "KBBKK...........",
                "KBK.............",
                "KK.............."}, "pixel art", "epee", "diamant", "minecraft");
        art(r, "pixel_pioche", "Pioche en diamant", S, new String[]{
                "....KKKKKKKK....",
                "..KKCCCCCCCCKK..",
                ".KCCDDDDDDDDCCK.",
                "KCDDKKKKKKKKDDCK",
                "KDDK...KK...KDDK",
                "KDK...KBBK...KDK",
                "KK...KBBK.....KK",
                "....KBBK........",
                "...KBBK.........",
                "..KBBK..........",
                ".KBBK...........",
                "KBBK............",
                "KBK.............",
                "KK.............."}, "pixel art", "pioche", "outil", "minecraft");
        art(r, "pixel_champignon", "Champignon rouge", S, new String[]{
                ".....KKKKKK.....",
                "...KKRRWWRRKK...",
                "..KRRRWWWWRRRK..",
                ".KRWWRRWWRRWWRK.",
                ".KWWWWRRRRWWWWK.",
                "KRWWWRRRRRRWWWRK",
                "KRRRRRRRRRRRRRRK",
                "KRRWWWRRRRWWWRRK",
                ".KKKKKKKKKKKKKK.",
                "...KTTKTTKTTK...",
                "...KTTKTTKTTK...",
                "...KTTTTTTTTK...",
                "....KTTTTTTK....",
                ".....KKKKKK....."}, "pixel art", "champignon", "jeu video");
        art(r, "pixel_smiley", "Smiley", S, new String[]{
                "...YYYYYY...",
                "..YYYYYYYY..",
                ".YYYYYYYYYY.",
                "YYYKYYYYKYYY",
                "YYYKYYYYKYYY",
                "YYYYYYYYYYYY",
                "YKYYYYYYYYKY",
                "YYKYYYYYYKYY",
                ".YYKKKKKKYY.",
                "..YYYYYYYY..",
                "...YYYYYY..."}, "pixel art", "smiley", "sourire", "emoji");
        art(r, "pixel_etoile", "Étoile", S, new String[]{
                "......Y......",
                ".....YYY.....",
                ".....YOY.....",
                "....YYYYY....",
                "YYYYYYYYYYYYY",
                ".YYYYYYYYYYY.",
                "..YYYKYKYYY..",
                "...YYYYYYY...",
                "..YYYYYYYYY..",
                "..YYYY.YYYY..",
                ".YYY.....YYY.",
                ".YY.......YY."}, "pixel art", "etoile", "star");
        art(r, "pixel_envahisseur", "Envahisseur de l'espace", S, new String[]{
                "..L.....L..",
                "...L...L...",
                "..LLLLLLL..",
                ".LL.LLL.LL.",
                "LLLLLLLLLLL",
                "L.LLLLLLL.L",
                "L.L.....L.L",
                "...LL.LL..."}, "pixel art", "invader", "alien", "arcade");
        art(r, "pixel_pomme", "Pomme", S, new String[]{
                "......B.....",
                ".....BGG....",
                "....B.......",
                "..RRRRRRRR..",
                ".RRWRRRRRRR.",
                "RRWWRRRRRRRR",
                "RRWRRRRRRRRR",
                "RRRRRRRRRRRR",
                "RRRRRRRRRRRR",
                ".RRRRRRRRRR.",
                "..RRRRRRRR..",
                "...RR..RR..."}, "pixel art", "pomme", "fruit");
        art(r, "pixel_diamant", "Diamant", S, new String[]{
                "...CCCCCC...",
                "..CWCCCCDC..",
                ".CWCCCCCCDC.",
                "CCCCCCCCCCDD",
                ".CCCCCCCCDD.",
                "..CCCCCCDD..",
                "...CCCCDD...",
                "....CCDD....",
                ".....DD....."}, "pixel art", "diamant", "gemme", "minecraft");
        r.gen("pixel_arc_en_ciel", "Arc-en-ciel", 2, SizeSpec.height(8, 16, 32, 64), p -> rainbow(p.size()), "pixel art", "arc en ciel", "couleurs");
        art(r, "pixel_pikachu", "Pikachu en pixel art", S, new String[]{
                "K............K",
                "YK..........KY",
                "YYK........KYY",
                ".YYKYYYYYYKYY.",
                "..YYYYYYYYYY..",
                ".YYYYYYYYYYYY.",
                ".YYKWYYYYKWYY.",
                ".YYKKYYYYKKYY.",
                ".RRYYYKKYYYRR.",
                ".RRYYYYYYYYRR.",
                "..YYYKYYKYYY..",
                "...YYYKKYYY...",
                "....YYYYYY...."}, "pixel art", "pikachu", "pokemon");
        r.sculpt("helice_adn", "Double hélice d'ADN", 3, SizeSpec.height(12, 30, 60, 120), PixelArtModels::dna, "sculpture", "adn", "science");
        r.sculpt("noeud_torique", "Nœud torique", 4, SizeSpec.height(10, 24, 48, 96), PixelArtModels::torusKnot, "sculpture", "noeud", "mathematiques");
        r.sculpt("spirale_abstraite", "Spirale abstraite", 3, SizeSpec.height(10, 24, 48, 96), PixelArtModels::spiral, "sculpture", "spirale", "art moderne");
    }

    private static void art(Reg r, String id, String name, SizeSpec sizes, String[] rows, String... tags) {
        r.gen(id, name, 2, sizes, p -> pixelArt(rows, p.size()), tags);
    }

    /**
     * Pixel art vertical (plan XY, face vers +Z) mis à l'échelle au plus proche voisin.
     * Épaisseur de 2 blocs au-delà d'une échelle ×3 pour la solidité visuelle.
     */
    public static VoxelModel pixelArt(String[] rows, int height) {
        if (rows.length == 0) {
            return VoxelModel.EMPTY;
        }
        int cols = 0;
        for (String row : rows) {
            cols = Math.max(cols, row.length());
        }
        double k = (double) Math.max(1, height) / rows.length;
        int outH = Math.max(1, (int) Math.round(rows.length * k));
        int outW = Math.max(1, (int) Math.round(cols * k));
        int depth = k >= 3 ? 2 : 1;
        VoxelGrid g = new VoxelGrid();
        for (int y = 0; y < outH; y++) {
            String row = rows[Math.min(rows.length - 1, (int) ((outH - 1 - y) / k))];
            for (int x = 0; x < outW; x++) {
                int col = Math.min(cols - 1, (int) (x / k));
                char ch = col < row.length() ? row.charAt(col) : '.';
                String state = PALETTE.get(ch);
                if (state != null) {
                    for (int z = 0; z < depth; z++) {
                        g.set(x - outW / 2, y, -z, state);
                    }
                }
            }
        }
        return g.freeze().groundAtBottom();
    }

    static VoxelModel rainbow(int height) {
        String[] colors = {"red", "orange", "yellow", "lime", "light_blue", "blue", "purple"};
        VoxelGrid g = new VoxelGrid();
        int outer = Math.max(7, height);
        double band = Math.max(1.0, outer / 12.0);
        for (int x = -outer; x <= outer; x++) {
            for (int y = 0; y <= outer; y++) {
                double d = Math.sqrt(x * x + y * y);
                int idx = (int) Math.floor((outer - d) / band);
                if (d <= outer && idx >= 0 && idx < colors.length) {
                    g.set(x, y, 0, "minecraft:" + colors[idx] + "_concrete");
                }
            }
        }
        for (int s = -1; s <= 1; s += 2) {
            int cx = s * (outer - (int) (band * 3.5));
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (dx * dx + dy * dy * 3 <= 10) {
                        g.set(cx + dx, dy, 1, "minecraft:white_wool");
                    }
                }
            }
        }
        return g.freeze().groundAtBottom();
    }

    static Sculpt dna() {
        Sculpt.Builder b = Sculpt.builder(30);
        String[] rungs = {"minecraft:red_concrete", "minecraft:yellow_concrete", "minecraft:lime_concrete", "minecraft:light_blue_concrete"};
        int n = 60;
        Vec3 pa = null, pb = null;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double a = t * Math.PI * 4;
            Vec3 a1 = new Vec3(Math.cos(a) * 5, 2 + t * 26, Math.sin(a) * 5);
            Vec3 b1 = new Vec3(-Math.cos(a) * 5, 2 + t * 26, -Math.sin(a) * 5);
            if (pa != null) {
                b.add(limb(pa, a1, 0.8, 0.8), "minecraft:blue_concrete");
                b.add(limb(pb, b1, 0.8, 0.8), "minecraft:purple_concrete");
            }
            if (i % 4 == 0) {
                b.add(limb(a1, a1.lerp(b1, 0.5), 0.4, 0.4), rungs[(i / 4) % 4]);
                b.add(limb(a1.lerp(b1, 0.5), b1, 0.4, 0.4), rungs[(i / 4 + 2) % 4]);
            }
            pa = a1;
            pb = b1;
        }
        b.add(cylinderY(0, 0, 0, 2, 7), "minecraft:polished_blackstone");
        return b.build();
    }

    static Sculpt torusKnot() {
        Sculpt.Builder b = Sculpt.builder(24);
        int n = 240;
        Vec3 prev = null;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n * Math.PI * 2;
            double r = 2 + Math.cos(3 * t);
            Vec3 p = new Vec3(r * Math.cos(2 * t) * 3.2, 12 + Math.sin(3 * t) * 3.2 * 1.6, r * Math.sin(2 * t) * 3.2);
            if (prev != null) {
                b.add(limb(prev, p, 1.3, 1.3), (x, y, z) -> {
                    double h = (Math.atan2(z, x) + Math.PI) / (Math.PI * 2);
                    return h < 0.33 ? "minecraft:magenta_concrete" : h < 0.66 ? "minecraft:cyan_concrete" : "minecraft:yellow_concrete";
                });
            }
            prev = p;
        }
        b.add(cylinderY(0, 0, 0, 3, 3), "minecraft:polished_deepslate");
        b.add(limb(0, 3, 0, 0, 6, 0, 0.6, 0.6), "minecraft:polished_deepslate");
        return b.build();
    }

    static Sculpt spiral() {
        Sculpt.Builder b = Sculpt.builder(24);
        int n = 120;
        Vec3 prev = null;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double a = t * Math.PI * 5;
            double r = 8 * (1 - t) + 1;
            Vec3 p = new Vec3(Math.cos(a) * r, 1 + t * 22, Math.sin(a) * r);
            if (prev != null) {
                b.add(limb(prev, p, 1.0 + (1 - t) * 0.8, 1.0 + (1 - t) * 0.8), t < 0.5 ? "minecraft:orange_concrete" : "minecraft:white_concrete");
            }
            prev = p;
        }
        b.add(sphere(0, 23.5, 0, 1.4), "minecraft:gold_block");
        b.add(cylinderY(0, 0, 0, 1, 9), "minecraft:smooth_stone");
        return b.build();
    }
}
