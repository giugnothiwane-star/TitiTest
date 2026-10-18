package com.ultimateautobuilder.core.generator;

import java.util.Map;

/** Matériau de toiture décliné en escalier, dalle et bloc plein (noms vanilla 1.21.1). */
public record RoofMat(String base, String stairsBase, String slab, String full) {
    private static final Map<String, String> FULL = Map.ofEntries(
            Map.entry("oak", "minecraft:oak_planks"), Map.entry("spruce", "minecraft:spruce_planks"),
            Map.entry("birch", "minecraft:birch_planks"), Map.entry("jungle", "minecraft:jungle_planks"),
            Map.entry("acacia", "minecraft:acacia_planks"), Map.entry("dark_oak", "minecraft:dark_oak_planks"),
            Map.entry("mangrove", "minecraft:mangrove_planks"), Map.entry("cherry", "minecraft:cherry_planks"),
            Map.entry("bamboo", "minecraft:bamboo_planks"), Map.entry("crimson", "minecraft:crimson_planks"),
            Map.entry("warped", "minecraft:warped_planks"), Map.entry("stone", "minecraft:stone"),
            Map.entry("cobblestone", "minecraft:cobblestone"), Map.entry("mossy_cobblestone", "minecraft:mossy_cobblestone"),
            Map.entry("stone_brick", "minecraft:stone_bricks"), Map.entry("mossy_stone_brick", "minecraft:mossy_stone_bricks"),
            Map.entry("brick", "minecraft:bricks"), Map.entry("mud_brick", "minecraft:mud_bricks"),
            Map.entry("sandstone", "minecraft:sandstone"), Map.entry("smooth_sandstone", "minecraft:smooth_sandstone"),
            Map.entry("red_sandstone", "minecraft:red_sandstone"), Map.entry("prismarine", "minecraft:prismarine"),
            Map.entry("prismarine_brick", "minecraft:prismarine_bricks"), Map.entry("dark_prismarine", "minecraft:dark_prismarine"),
            Map.entry("nether_brick", "minecraft:nether_bricks"), Map.entry("red_nether_brick", "minecraft:red_nether_bricks"),
            Map.entry("quartz", "minecraft:quartz_block"), Map.entry("smooth_quartz", "minecraft:smooth_quartz"),
            Map.entry("purpur", "minecraft:purpur_block"), Map.entry("end_stone_brick", "minecraft:end_stone_bricks"),
            Map.entry("blackstone", "minecraft:blackstone"), Map.entry("polished_blackstone", "minecraft:polished_blackstone"),
            Map.entry("polished_blackstone_brick", "minecraft:polished_blackstone_bricks"),
            Map.entry("deepslate_tile", "minecraft:deepslate_tiles"), Map.entry("deepslate_brick", "minecraft:deepslate_bricks"),
            Map.entry("polished_deepslate", "minecraft:polished_deepslate"), Map.entry("cobbled_deepslate", "minecraft:cobbled_deepslate"),
            Map.entry("cut_copper", "minecraft:cut_copper"), Map.entry("oxidized_cut_copper", "minecraft:oxidized_cut_copper"),
            Map.entry("weathered_cut_copper", "minecraft:weathered_cut_copper"), Map.entry("tuff_brick", "minecraft:tuff_bricks"),
            Map.entry("polished_tuff", "minecraft:polished_tuff"), Map.entry("polished_andesite", "minecraft:polished_andesite"),
            Map.entry("polished_granite", "minecraft:polished_granite"), Map.entry("polished_diorite", "minecraft:polished_diorite"),
            Map.entry("granite", "minecraft:granite"), Map.entry("bamboo_mosaic", "minecraft:bamboo_mosaic"));

    public static RoofMat of(String base) {
        String full = FULL.get(base);
        if (full == null) {
            throw new IllegalArgumentException("Matériau de toit inconnu: " + base);
        }
        return new RoofMat(base, base, "minecraft:" + base + "_slab", full);
    }

    public String stairs(String facing, boolean top) {
        return "minecraft:" + stairsBase + "_stairs[facing=" + facing + ",half=" + (top ? "top" : "bottom") + "]";
    }

    public String slabBottom() {
        return slab + "[type=bottom]";
    }

    public String slabTop() {
        return slab + "[type=top]";
    }
}
