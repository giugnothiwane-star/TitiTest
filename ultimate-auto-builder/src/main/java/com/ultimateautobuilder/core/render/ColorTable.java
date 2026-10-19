package com.ultimateautobuilder.core.render;

import com.ultimateautobuilder.core.model.B;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Couleur approximative (RGB) d'un état de bloc, pour les miniatures et les aperçus.
 * Indépendante de Minecraft : utilisable hors du thread de rendu.
 */
public final class ColorTable {
    private static final Map<String, Integer> DYES = new LinkedHashMap<>();
    private static final Map<String, Integer> KEYWORDS = new LinkedHashMap<>();
    private static final ConcurrentHashMap<String, Integer> CACHE = new ConcurrentHashMap<>();

    static {
        // Ordre important : les noms composés avant leurs suffixes (light_blue avant blue).
        DYES.put("light_blue", 0x3AAFD9);
        DYES.put("light_gray", 0x9D9D97);
        DYES.put("white", 0xE9ECEC);
        DYES.put("orange", 0xF07613);
        DYES.put("magenta", 0xBD44B3);
        DYES.put("yellow", 0xF8C527);
        DYES.put("lime", 0x70B919);
        DYES.put("pink", 0xED8DAC);
        DYES.put("gray", 0x3E4447);
        DYES.put("cyan", 0x158991);
        DYES.put("purple", 0x792AAC);
        DYES.put("blue", 0x35399D);
        DYES.put("brown", 0x724728);
        DYES.put("green", 0x546D1B);
        DYES.put("red", 0xA12722);
        DYES.put("black", 0x141519);

        KEYWORDS.put("air", -1);
        KEYWORDS.put("water", 0x3F76E4);
        KEYWORDS.put("lava", 0xD96415);
        KEYWORDS.put("cherry_leaves", 0xE7A8C9);
        KEYWORDS.put("azalea_leaves", 0x5A7A2C);
        KEYWORDS.put("birch_leaves", 0x6A9A40);
        KEYWORDS.put("spruce_leaves", 0x3B5E3B);
        KEYWORDS.put("leaves", 0x3E8B2A);
        KEYWORDS.put("red_sandstone", 0xBA6321);
        KEYWORDS.put("sandstone", 0xD8CB9B);
        KEYWORDS.put("sand", 0xDBCFA3);
        KEYWORDS.put("dark_oak", 0x42291A);
        KEYWORDS.put("dark_prismarine", 0x335B4B);
        KEYWORDS.put("prismarine", 0x63A397);
        KEYWORDS.put("sea_lantern", 0xACC8BE);
        KEYWORDS.put("stripped_birch", 0xC4B077);
        KEYWORDS.put("birch_log", 0xD8D7D2);
        KEYWORDS.put("birch", 0xC0AF79);
        KEYWORDS.put("spruce", 0x725430);
        KEYWORDS.put("jungle", 0xA07350);
        KEYWORDS.put("acacia", 0xA85A32);
        KEYWORDS.put("mangrove", 0x763631);
        KEYWORDS.put("cherry", 0xE2B2A9);
        KEYWORDS.put("bamboo", 0xC2AE52);
        KEYWORDS.put("crimson", 0x653045);
        KEYWORDS.put("warped", 0x2B6963);
        KEYWORDS.put("oak_log", 0x6D5532);
        KEYWORDS.put("oak", 0xA2834F);
        KEYWORDS.put("grass_block", 0x5D9B3A);
        KEYWORDS.put("moss", 0x596E2D);
        KEYWORDS.put("short_grass", 0x6FA548);
        KEYWORDS.put("tall_grass", 0x6FA548);
        KEYWORDS.put("fern", 0x5E9A3A);
        KEYWORDS.put("vine", 0x3F7A26);
        KEYWORDS.put("cactus", 0x5A8A2C);
        KEYWORDS.put("melon", 0x6F9A2A);
        KEYWORDS.put("pumpkin", 0xC67516);
        KEYWORDS.put("hay", 0xB49A22);
        KEYWORDS.put("mud_brick", 0x8A6A4F);
        KEYWORDS.put("packed_mud", 0x8E6B50);
        KEYWORDS.put("mud", 0x3C3A3D);
        KEYWORDS.put("podzol", 0x5B3F1D);
        KEYWORDS.put("coarse_dirt", 0x77553B);
        KEYWORDS.put("rooted_dirt", 0x90684D);
        KEYWORDS.put("dirt_path", 0x947A41);
        KEYWORDS.put("dirt", 0x866043);
        KEYWORDS.put("farmland", 0x5B3B23);
        KEYWORDS.put("gravel", 0x837F7E);
        KEYWORDS.put("clay", 0xA0A6B3);
        KEYWORDS.put("snow", 0xF3FAFA);
        KEYWORDS.put("powder_snow", 0xF3FAFA);
        KEYWORDS.put("blue_ice", 0x74A8FD);
        KEYWORDS.put("packed_ice", 0x8DB4FA);
        KEYWORDS.put("ice", 0x91B7FD);
        KEYWORDS.put("deepslate", 0x505053);
        KEYWORDS.put("blackstone", 0x2A2328);
        KEYWORDS.put("basalt", 0x515155);
        KEYWORDS.put("tuff", 0x6C6D66);
        KEYWORDS.put("calcite", 0xDFE0DC);
        KEYWORDS.put("diorite", 0xBDBDBD);
        KEYWORDS.put("andesite", 0x888889);
        KEYWORDS.put("granite", 0x956755);
        KEYWORDS.put("mossy", 0x6C7A5C);
        KEYWORDS.put("cobblestone", 0x7F7F7F);
        KEYWORDS.put("smooth_stone", 0x9E9E9E);
        KEYWORDS.put("stone_brick", 0x7A7A7A);
        KEYWORDS.put("end_stone", 0xDBDE9E);
        KEYWORDS.put("purpur", 0xA97DA9);
        KEYWORDS.put("netherrack", 0x612726);
        KEYWORDS.put("nether_brick", 0x2C161A);
        KEYWORDS.put("nether_wart", 0x731414);
        KEYWORDS.put("magma", 0x8E3F1F);
        KEYWORDS.put("shroomlight", 0xF19E46);
        KEYWORDS.put("glowstone", 0xFBDA74);
        KEYWORDS.put("obsidian", 0x0F0B19);
        KEYWORDS.put("crying_obsidian", 0x200A3C);
        KEYWORDS.put("quartz", 0xECE6DF);
        KEYWORDS.put("bone_block", 0xE5E1CF);
        KEYWORDS.put("bricks", 0x976253);
        KEYWORDS.put("brick", 0x976253);
        KEYWORDS.put("terracotta", 0x985E43);
        KEYWORDS.put("gold", 0xF6D03D);
        KEYWORDS.put("raw_gold", 0xDDA92F);
        KEYWORDS.put("iron_bars", 0x8A8A8A);
        KEYWORDS.put("iron", 0xDCDCDC);
        KEYWORDS.put("chain", 0x40444F);
        KEYWORDS.put("diamond", 0x62EDE4);
        KEYWORDS.put("emerald", 0x2ACB57);
        KEYWORDS.put("lapis", 0x1F4AA0);
        KEYWORDS.put("redstone_lamp", 0xC9844D);
        KEYWORDS.put("redstone", 0xAF1805);
        KEYWORDS.put("netherite", 0x433E3F);
        KEYWORDS.put("oxidized_copper", 0x52A284);
        KEYWORDS.put("weathered_copper", 0x6C9A6F);
        KEYWORDS.put("exposed_copper", 0xA17E68);
        KEYWORDS.put("copper", 0xC06C50);
        KEYWORDS.put("amethyst", 0x8562BE);
        KEYWORDS.put("coal", 0x101010);
        KEYWORDS.put("glass", 0xC2E1E8);
        KEYWORDS.put("lantern", 0xE3A43F);
        KEYWORDS.put("torch", 0xFFD25A);
        KEYWORDS.put("end_rod", 0xF0E8DC);
        KEYWORDS.put("bookshelf", 0x6E5233);
        KEYWORDS.put("crafting", 0x8E6B3E);
        KEYWORDS.put("furnace", 0x6E6E6E);
        KEYWORDS.put("smoker", 0x5A5248);
        KEYWORDS.put("chest", 0xA0702F);
        KEYWORDS.put("barrel", 0x8B6538);
        KEYWORDS.put("anvil", 0x444444);
        KEYWORDS.put("sponge", 0xC4C04A);
        KEYWORDS.put("slime", 0x77C065);
        KEYWORDS.put("honey", 0xF5A623);
        KEYWORDS.put("target", 0xE5B0A0);
        KEYWORDS.put("tnt", 0xD9412C);
        KEYWORDS.put("note_block", 0x5E3E2D);
        KEYWORDS.put("jukebox", 0x5E3E2D);
        KEYWORDS.put("beacon", 0x75E0DB);
        KEYWORDS.put("sculk", 0x0C2A31);
        KEYWORDS.put("dripstone", 0x866B5C);
        KEYWORDS.put("mushroom_stem", 0xCBC4B9);
        KEYWORDS.put("red_mushroom_block", 0xC82E2D);
        KEYWORDS.put("brown_mushroom_block", 0x95704F);
        KEYWORDS.put("stone", 0x7D7D7D);
        KEYWORDS.put("planks", 0xA2834F);
        KEYWORDS.put("log", 0x6D5532);
        KEYWORDS.put("wood", 0x6D5532);
    }

    private ColorTable() {
    }

    /** Couleur RGB (0xRRGGBB) ou -1 pour un bloc invisible (air). */
    public static int rgb(String state) {
        if (state == null) {
            return -1;
        }
        String id = B.idOf(state);
        return CACHE.computeIfAbsent(id, ColorTable::compute);
    }

    private static int compute(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        if (path.equals("air") || path.equals("cave_air") || path.equals("void_air") || path.equals("barrier")
                || path.equals("light") || path.equals("structure_void")) {
            return -1;
        }
        for (Map.Entry<String, Integer> e : DYES.entrySet()) {
            if (path.startsWith(e.getKey() + "_")) {
                int c = e.getValue();
                if (path.contains("terracotta")) {
                    return mix(c, 0x8E5A44, 0.45);
                }
                if (path.contains("glass")) {
                    return mix(c, 0xFFFFFF, 0.25);
                }
                if (path.contains("wool") || path.contains("carpet")) {
                    return mix(c, 0xFFFFFF, 0.06);
                }
                // Les chaînes « red_sandstone », « red_nether_bricks »… se rabattent sur les mots-clés.
                if (path.contains("concrete") || path.contains("bed") || path.contains("candle") || path.contains("banner")
                        || path.contains("shulker") || path.contains("dye") || path.contains("glazed")) {
                    return c;
                }
                if (!keywordMatch(path)) {
                    return c;
                }
            }
        }
        for (Map.Entry<String, Integer> e : KEYWORDS.entrySet()) {
            if (path.contains(e.getKey())) {
                return e.getValue();
            }
        }
        // Repli : couleur stable dérivée du nom.
        int h = path.hashCode();
        return 0x404040 | (h & 0x7F7F7F);
    }

    private static boolean keywordMatch(String path) {
        for (String k : KEYWORDS.keySet()) {
            if (path.contains(k) && !k.equals("stone") && !k.equals("planks")) {
                return true;
            }
        }
        return false;
    }

    public static int mix(int a, int b, double t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int shade(int rgb, double factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * factor));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * factor));
        int b = Math.min(255, (int) ((rgb & 255) * factor));
        return (r << 16) | (g << 8) | b;
    }
}
