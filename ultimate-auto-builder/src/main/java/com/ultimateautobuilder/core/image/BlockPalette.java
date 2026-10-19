package com.ultimateautobuilder.core.image;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Correspondance couleur → bloc plein. Chaque entrée porte la couleur moyenne approximative de la
 * texture du bloc (Minecraft 1.21.1) ; la recherche se fait dans l'espace CIELAB (écart perçu) et
 * est mise en cache. Les familles permettent de restreindre la palette selon le style choisi.
 */
public final class BlockPalette {
    public enum Family { CONCRETE, WOOL, TERRACOTTA, STONE, WOOD, NATURAL, METAL, NETHER_END, GLASS }

    public record Entry(String state, int rgb, Family family, double[] lab) {
    }

    private static final List<Entry> ALL = new ArrayList<>();
    private static final Map<String, Integer> BY_STATE = new java.util.HashMap<>();

    static {
        String[] dyes = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
                "brown", "green", "red", "black"};
        int[] concrete = {0xCFD5D6, 0xE06101, 0xA9309F, 0x2489C7, 0xF1AF15, 0x5EA919, 0xD6658F, 0x373A3E, 0x7D7D73, 0x157788, 0x64209C,
                0x2D2F8F, 0x603C20, 0x495B24, 0x8E2121, 0x080A0F};
        int[] wool = {0xEAECED, 0xF17614, 0xBE45B4, 0x3AAFD9, 0xF9C628, 0x70B91A, 0xEE8DAC, 0x3F4448, 0x8E8E87, 0x158A91, 0x7A2AAD,
                0x35399D, 0x724829, 0x556E1C, 0xA12723, 0x15151A};
        int[] terracotta = {0xD2B2A1, 0xA25426, 0x96586D, 0x716D8A, 0xBA8523, 0x687635, 0xA24E4F, 0x3A2A24, 0x876B62, 0x575B5B, 0x764656,
                0x4A3C5B, 0x4D3324, 0x4C532A, 0x8F3D2F, 0x251711};
        for (int i = 0; i < dyes.length; i++) {
            add(dyes[i] + "_concrete", concrete[i], Family.CONCRETE);
            add(dyes[i] + "_wool", wool[i], Family.WOOL);
            add(dyes[i] + "_terracotta", terracotta[i], Family.TERRACOTTA);
        }
        add("terracotta", 0x985E44, Family.TERRACOTTA);
        Object[][] others = {
                {"stone", 0x7E7E7E, Family.STONE}, {"cobblestone", 0x807F80, Family.STONE}, {"stone_bricks", 0x7A797A, Family.STONE},
                {"smooth_stone", 0x9F9F9F, Family.STONE}, {"andesite", 0x888889, Family.STONE}, {"polished_andesite", 0x848786, Family.STONE},
                {"diorite", 0xBDBCBD, Family.STONE}, {"polished_diorite", 0xC1C1C3, Family.STONE}, {"granite", 0x956756, Family.STONE},
                {"polished_granite", 0x9A6B59, Family.STONE}, {"deepslate", 0x505052, Family.STONE}, {"cobbled_deepslate", 0x4D4D50, Family.STONE},
                {"deepslate_tiles", 0x363637, Family.STONE}, {"deepslate_bricks", 0x464647, Family.STONE}, {"tuff", 0x6C6D66, Family.STONE},
                {"calcite", 0xDFE0DC, Family.STONE}, {"dripstone_block", 0x866B5C, Family.STONE}, {"sandstone", 0xD8CB9B, Family.STONE},
                {"smooth_sandstone", 0xDFD6AA, Family.STONE}, {"red_sandstone", 0xBA631D, Family.STONE}, {"bricks", 0x966153, Family.STONE},
                {"mud_bricks", 0x89674F, Family.STONE}, {"quartz_block", 0xEBE5DE, Family.STONE}, {"blackstone", 0x2A2429, Family.STONE},
                {"polished_blackstone", 0x353139, Family.STONE}, {"prismarine", 0x639C97, Family.STONE},
                {"prismarine_bricks", 0x63AB9E, Family.STONE}, {"dark_prismarine", 0x335B4B, Family.STONE},
                {"oak_planks", 0xA2824E, Family.WOOD}, {"spruce_planks", 0x725430, Family.WOOD}, {"birch_planks", 0xC0AF79, Family.WOOD},
                {"jungle_planks", 0xA07350, Family.WOOD}, {"acacia_planks", 0xA85A32, Family.WOOD}, {"dark_oak_planks", 0x422B14, Family.WOOD},
                {"mangrove_planks", 0x753630, Family.WOOD}, {"cherry_planks", 0xE2B2AC, Family.WOOD}, {"bamboo_planks", 0xC1AD50, Family.WOOD},
                {"crimson_planks", 0x653046, Family.WOOD}, {"warped_planks", 0x2B6863, Family.WOOD},
                {"oak_log[axis=y]", 0x6D5532, Family.WOOD}, {"birch_log[axis=y]", 0xD8D7D2, Family.WOOD},
                {"spruce_log[axis=y]", 0x3A2510, Family.WOOD}, {"dark_oak_log[axis=y]", 0x3C2E1A, Family.WOOD},
                {"snow_block", 0xF9FEFE, Family.NATURAL}, {"clay", 0xA0A6B3, Family.NATURAL}, {"packed_ice", 0x8DB4FA, Family.NATURAL},
                {"blue_ice", 0x74A7FD, Family.NATURAL}, {"moss_block", 0x596D2D, Family.NATURAL}, {"dirt", 0x866043, Family.NATURAL},
                {"coarse_dirt", 0x77553B, Family.NATURAL}, {"mud", 0x3C393C, Family.NATURAL}, {"hay_block", 0xA68B0C, Family.NATURAL},
                {"bone_block", 0xE5E1CF, Family.NATURAL}, {"honeycomb_block", 0xE5941D, Family.NATURAL}, {"melon", 0x6F901E, Family.NATURAL},
                {"pumpkin", 0xC67618, Family.NATURAL}, {"sponge", 0xC3C04A, Family.NATURAL},
                {"gold_block", 0xF6D03D, Family.METAL}, {"iron_block", 0xDCDCDC, Family.METAL}, {"copper_block", 0xC06B4F, Family.METAL},
                {"exposed_copper", 0xA17D67, Family.METAL}, {"weathered_copper", 0x6C996E, Family.METAL},
                {"oxidized_copper", 0x52A284, Family.METAL}, {"diamond_block", 0x62EDE4, Family.METAL}, {"emerald_block", 0x2ACB57, Family.METAL},
                {"lapis_block", 0x1E438C, Family.METAL}, {"redstone_block", 0xAF1805, Family.METAL}, {"netherite_block", 0x423D3F, Family.METAL},
                {"coal_block", 0x100F0F, Family.METAL}, {"amethyst_block", 0x8561BF, Family.METAL}, {"raw_gold_block", 0xDDA92E, Family.METAL},
                {"obsidian", 0x0F0A18, Family.NETHER_END}, {"crying_obsidian", 0x200A3C, Family.NETHER_END},
                {"netherrack", 0x612626, Family.NETHER_END}, {"nether_bricks", 0x2C151A, Family.NETHER_END},
                {"red_nether_bricks", 0x450709, Family.NETHER_END}, {"nether_wart_block", 0x720202, Family.NETHER_END},
                {"warped_wart_block", 0x167779, Family.NETHER_END}, {"glowstone", 0xAB8354, Family.NETHER_END},
                {"shroomlight", 0xF09246, Family.NETHER_END}, {"magma_block", 0x8E3F1F, Family.NETHER_END},
                {"soul_soil", 0x4B392E, Family.NETHER_END}, {"purpur_block", 0xA97DA9, Family.NETHER_END},
                {"end_stone", 0xDBDE9E, Family.NETHER_END}, {"end_stone_bricks", 0xDAE0A2, Family.NETHER_END},
                {"glass", 0xC0DCE4, Family.GLASS}, {"light_blue_stained_glass", 0x6699D8, Family.GLASS},
                {"black_stained_glass", 0x191919, Family.GLASS}, {"gray_stained_glass", 0x4C4C4C, Family.GLASS}};
        for (Object[] o : others) {
            add((String) o[0], (Integer) o[1], (Family) o[2]);
        }
    }

    private final List<Entry> entries;
    private final Map<Integer, String> cache = new ConcurrentHashMap<>();

    private BlockPalette(List<Entry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Palette vide");
        }
        this.entries = entries;
    }

    private static void add(String id, int rgb, Family f) {
        ALL.add(new Entry("minecraft:" + id, rgb, f, lab(rgb)));
        BY_STATE.put("minecraft:" + id, rgb);
    }

    public static BlockPalette of(Set<Family> families) {
        List<Entry> list = new ArrayList<>();
        for (Entry e : ALL) {
            if (families.contains(e.family())) {
                list.add(e);
            }
        }
        return new BlockPalette(list);
    }

    public static BlockPalette full() {
        return of(EnumSet.complementOf(EnumSet.of(Family.GLASS)));
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Bloc le plus proche (écart CIELAB), mis en cache par couleur. */
    public String nearest(int rgb) {
        int key = rgb & 0xFFFFFF;
        return cache.computeIfAbsent(key, k -> {
            double[] l = lab(k);
            Entry best = entries.get(0);
            double bd = Double.MAX_VALUE;
            for (Entry e : entries) {
                double dl = l[0] - e.lab()[0], da = l[1] - e.lab()[1], db = l[2] - e.lab()[2];
                double d = dl * dl + da * da + db * db;
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            return best.state();
        });
    }

    /** Couleur moyenne d'un bloc : valeur de la palette, sinon estimation du catalogue (-1 si invisible). */
    public static int colorOf(String state) {
        Integer c = BY_STATE.get(state);
        return c != null ? c : com.ultimateautobuilder.core.render.ColorTable.rgb(state);
    }

    /** sRGB → CIELAB (illuminant D65). */
    public static double[] lab(int rgb) {
        double r = lin(((rgb >> 16) & 255) / 255.0), g = lin(((rgb >> 8) & 255) / 255.0), b = lin((rgb & 255) / 255.0);
        double x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047;
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883;
        double fx = f(x), fy = f(y), fz = f(z);
        return new double[]{116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    private static double lin(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double f(double t) {
        return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116;
    }

    public static double deltaE(int a, int b) {
        double[] la = lab(a), lb = lab(b);
        double dl = la[0] - lb[0], da = la[1] - lb[1], db = la[2] - lb[2];
        return Math.sqrt(dl * dl + da * da + db * db);
    }
}
