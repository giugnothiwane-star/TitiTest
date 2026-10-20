package com.ultimateautobuilder.core.adventure;

import java.util.List;

/**
 * Familles de créatures qu'un spawner peut produire. La famille fixe l'ensemble des créatures
 * autorisées ; la catégorie du spawner fixe ensuite leur variante. Les deux étapes sont
 * distinctes : un spawner d'araignées ne produit jamais de zombies.
 */
public enum SpawnFamily {
    UNDEAD("morts_vivants", "Morts-vivants", List.of("minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager", "minecraft:drowned",
            "minecraft:skeleton", "minecraft:stray", "minecraft:bogged")),
    ARACHNID("araignees", "Araignées", List.of("minecraft:spider", "minecraft:cave_spider")),
    ILLAGER("illageois", "Illageois", List.of("minecraft:vindicator", "minecraft:pillager", "minecraft:witch")),
    MIXED("mixte", "Créatures mixtes", List.of("minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager", "minecraft:drowned",
            "minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:spider", "minecraft:cave_spider",
            "minecraft:vindicator", "minecraft:pillager", "minecraft:witch"));

    public final String key;
    public final String label;
    public final List<String> members;

    SpawnFamily(String key, String label, List<String> members) {
        this.key = key;
        this.label = label;
        this.members = members;
    }

    public static SpawnFamily byKey(String key) {
        for (SpawnFamily f : values()) {
            if (f.key.equals(key)) {
                return f;
            }
        }
        return null;
    }
}
