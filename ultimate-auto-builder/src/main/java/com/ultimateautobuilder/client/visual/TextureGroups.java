package com.ultimateautobuilder.client.visual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groupes de textures vanilla recolorables séparément (une essence de bois, une famille de mobs…).
 * Seules les textures de ces listes sont régénérées : modifier le chêne ne touche jamais au bouleau.
 * Une texture absente de la version installée est simplement ignorée.
 */
public final class TextureGroups {
    public enum Section {
        WOOD("Arbres et bois"), STONE("Pierre et matériaux"), WATER("Eau"), MOBS("Mobs"), ANIMALS("Animaux"),
        ARMOR("Personnages et armures"), VEGETATION("Herbe et végétation");

        public final String label;

        Section(String label) {
            this.label = label;
        }
    }

    public record Group(String key, String label, Section section, List<String> textures) {
    }

    private static final Map<String, Group> GROUPS = new LinkedHashMap<>();

    static {
        for (String wood : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"}) {
            String label = switch (wood) {
                case "oak" -> "Chêne";
                case "spruce" -> "Sapin";
                case "birch" -> "Bouleau";
                case "jungle" -> "Bois de la jungle";
                case "acacia" -> "Acacia";
                case "dark_oak" -> "Chêne noir";
                case "mangrove" -> "Palétuvier";
                default -> "Cerisier";
            };
            add("wood_" + wood, label, Section.WOOD, "block/" + wood + "_planks", "block/" + wood + "_log", "block/" + wood + "_log_top",
                    "block/stripped_" + wood + "_log", "block/stripped_" + wood + "_log_top", "block/" + wood + "_door_top",
                    "block/" + wood + "_door_bottom", "block/" + wood + "_trapdoor");
        }
        add("wood_bamboo", "Bambou", Section.WOOD, "block/bamboo_planks", "block/bamboo_block", "block/bamboo_block_top",
                "block/bamboo_mosaic", "block/bamboo_door_top", "block/bamboo_door_bottom");
        add("stone", "Pierre", Section.STONE, "block/stone", "block/cobblestone", "block/stone_bricks", "block/smooth_stone",
                "block/mossy_cobblestone", "block/mossy_stone_bricks", "block/cracked_stone_bricks", "block/chiseled_stone_bricks");
        add("deepslate", "Ardoise des abîmes", Section.STONE, "block/deepslate", "block/deepslate_top", "block/cobbled_deepslate",
                "block/deepslate_bricks", "block/deepslate_tiles", "block/polished_deepslate");
        add("sandstone", "Grès", Section.STONE, "block/sandstone", "block/sandstone_top", "block/sandstone_bottom", "block/cut_sandstone",
                "block/smooth_sandstone");
        add("bricks", "Briques", Section.STONE, "block/bricks", "block/mud_bricks", "block/nether_bricks");
        add("metals", "Métaux", Section.STONE, "block/iron_block", "block/gold_block", "block/copper_block", "block/cut_copper",
                "block/netherite_block", "block/iron_bars", "block/chain");
        add("glass", "Verre", Section.STONE, "block/glass", "block/glass_pane_top");
        add("water", "Eau", Section.WATER, "block/water_still", "block/water_flow", "block/water_overlay");
        add("leaves_texture", "Feuilles (texture)", Section.VEGETATION, "block/oak_leaves", "block/birch_leaves", "block/spruce_leaves",
                "block/jungle_leaves", "block/acacia_leaves", "block/dark_oak_leaves", "block/cherry_leaves");
        add("flowers", "Fleurs", Section.VEGETATION, "block/poppy", "block/dandelion", "block/blue_orchid", "block/allium", "block/azure_bluet",
                "block/oxeye_daisy", "block/cornflower", "block/pink_petals");
        add("zombie", "Zombies", Section.MOBS, "entity/zombie/zombie", "entity/zombie/husk", "entity/zombie/drowned",
                "entity/zombie_villager/zombie_villager");
        add("skeleton", "Squelettes", Section.MOBS, "entity/skeleton/skeleton", "entity/skeleton/stray", "entity/skeleton/wither_skeleton",
                "entity/skeleton/bogged");
        add("creeper", "Creeper", Section.MOBS, "entity/creeper/creeper");
        add("enderman", "Enderman", Section.MOBS, "entity/enderman/enderman", "entity/enderman/enderman_eyes");
        add("spider", "Araignées", Section.MOBS, "entity/spider/spider", "entity/spider/cave_spider");
        add("slime", "Slimes et cubes de magma", Section.MOBS, "entity/slime/slime", "entity/slime/magmacube");
        add("nether_mobs", "Mobs du Nether", Section.MOBS, "entity/blaze", "entity/ghast/ghast", "entity/piglin/piglin",
                "entity/piglin/piglin_brute", "entity/piglin/zombified_piglin", "entity/hoglin/hoglin", "entity/strider/strider");
        add("illagers", "Illageois et sorcières", Section.MOBS, "entity/illager/pillager", "entity/illager/vindicator",
                "entity/illager/evoker", "entity/witch", "entity/illager/ravager");
        add("villagers", "Villageois", Section.MOBS, "entity/villager/villager", "entity/wandering_trader", "entity/iron_golem/iron_golem");
        add("cow", "Vaches", Section.ANIMALS, "entity/cow/cow", "entity/cow/red_mooshroom", "entity/cow/brown_mooshroom");
        add("pig", "Cochons", Section.ANIMALS, "entity/pig/pig");
        add("sheep", "Moutons", Section.ANIMALS, "entity/sheep/sheep", "entity/sheep/sheep_fur");
        add("chicken", "Poulets", Section.ANIMALS, "entity/chicken");
        add("horse", "Chevaux", Section.ANIMALS, "entity/horse/horse_white", "entity/horse/horse_creamy", "entity/horse/horse_chestnut",
                "entity/horse/horse_brown", "entity/horse/horse_black", "entity/horse/horse_gray", "entity/horse/horse_darkbrown",
                "entity/horse/donkey", "entity/horse/mule");
        add("wolf", "Loups", Section.ANIMALS, "entity/wolf/wolf", "entity/wolf/wolf_tame", "entity/wolf/wolf_angry");
        add("cat", "Chats", Section.ANIMALS, "entity/cat/tabby", "entity/cat/black", "entity/cat/red", "entity/cat/siamese",
                "entity/cat/british_shorthair", "entity/cat/calico", "entity/cat/persian", "entity/cat/ragdoll", "entity/cat/white",
                "entity/cat/jellie", "entity/cat/all_black", "entity/cat/ocelot");
        add("fox", "Renards", Section.ANIMALS, "entity/fox/fox", "entity/fox/fox_sleep", "entity/fox/snow_fox");
        add("bear", "Ours et pandas", Section.ANIMALS, "entity/bear/polarbear", "entity/panda/panda");
        add("aquatic", "Créatures aquatiques", Section.ANIMALS, "entity/squid/squid", "entity/squid/glow_squid", "entity/dolphin",
                "entity/fish/cod", "entity/fish/salmon", "entity/fish/pufferfish", "entity/turtle/big_sea_turtle", "entity/axolotl/axolotl_lucy",
                "entity/axolotl/axolotl_wild", "entity/axolotl/axolotl_gold", "entity/axolotl/axolotl_cyan", "entity/axolotl/axolotl_blue");
        add("bees_birds", "Abeilles, oiseaux, lapins", Section.ANIMALS, "entity/bee/bee", "entity/parrot/parrot_red_blue",
                "entity/rabbit/brown", "entity/rabbit/white", "entity/bat");
        add("armor_iron", "Armure de fer", Section.ARMOR, "models/armor/iron_layer_1", "models/armor/iron_layer_2");
        add("armor_gold", "Armure en or", Section.ARMOR, "models/armor/gold_layer_1", "models/armor/gold_layer_2");
        add("armor_diamond", "Armure en diamant", Section.ARMOR, "models/armor/diamond_layer_1", "models/armor/diamond_layer_2");
        add("armor_netherite", "Armure en netherite", Section.ARMOR, "models/armor/netherite_layer_1", "models/armor/netherite_layer_2");
        add("players", "Apparences par défaut (Steve, Alex…)", Section.ARMOR, "entity/player/wide/steve", "entity/player/wide/alex",
                "entity/player/slim/steve", "entity/player/slim/alex");
    }

    private TextureGroups() {
    }

    private static void add(String key, String label, Section section, String... textures) {
        List<String> paths = new ArrayList<>();
        for (String t : textures) {
            paths.add("textures/" + t + ".png");
        }
        GROUPS.put(key, new Group(key, label, section, Collections.unmodifiableList(paths)));
    }

    public static Group get(String key) {
        return GROUPS.get(key);
    }

    public static List<Group> inSection(Section s) {
        List<Group> out = new ArrayList<>();
        for (Group g : GROUPS.values()) {
            if (g.section() == s) {
                out.add(g);
            }
        }
        return out;
    }

    public static java.util.Collection<Group> all() {
        return GROUPS.values();
    }
}
