package com.ultimateautobuilder.core.adventure;

import java.util.List;

/**
 * Récompenses d'ensemble d'armure : attribuées une seule fois par ensemble et par joueur,
 * uniquement quand les quatre pièces de la même matière sont portées.
 * Les identifiants « special: » sont construits par le serveur (objets enchantés).
 */
public enum ArmorSetReward {
    LEATHER("leather", "cuir", "minecraft:leather_", List.of(new Reward("minecraft:iron_ingot", 2), new Reward("minecraft:bread", 16))),
    GOLD("gold", "or", "minecraft:golden_", List.of(new Reward("minecraft:diamond", 8), new Reward("minecraft:golden_apple", 4))),
    CHAIN("chain", "mailles", "minecraft:chainmail_", List.of(new Reward("minecraft:iron_ingot", 16), new Reward("minecraft:emerald", 8))),
    IRON("iron", "fer", "minecraft:iron_", List.of(new Reward("minecraft:diamond", 8), new Reward("minecraft:emerald", 16))),
    DIAMOND("diamond", "diamant", "minecraft:diamond_", List.of(new Reward("minecraft:netherite_ingot", 4),
            new Reward("ultimateautobuilder:legendary_key", 1))),
    NETHERITE("netherite", "Netherite", "minecraft:netherite_", List.of(new Reward("special:op_pickaxe", 1),
            new Reward("ultimateautobuilder:op_key", 1)));

    public record Reward(String item, int count) {
    }

    public final String key;
    public final String label;
    private final String prefix;
    public final List<Reward> rewards;

    ArmorSetReward(String key, String label, String prefix, List<Reward> rewards) {
        this.key = key;
        this.label = label;
        this.prefix = prefix;
        this.rewards = rewards;
    }

    /** Identifiants des quatre pièces : casque, plastron, jambières, bottes. */
    public List<String> pieces() {
        return List.of(prefix + "helmet", prefix + "chestplate", prefix + "leggings", prefix + "boots");
    }

    /** Ensemble complet porté (identifiants des emplacements tête, torse, jambes, pieds). */
    public static ArmorSetReward wornSet(String head, String chest, String legs, String feet) {
        for (ArmorSetReward s : values()) {
            List<String> p = s.pieces();
            if (p.get(0).equals(head) && p.get(1).equals(chest) && p.get(2).equals(legs) && p.get(3).equals(feet)) {
                return s;
            }
        }
        return null;
    }

    public static ArmorSetReward byKey(String key) {
        for (ArmorSetReward s : values()) {
            if (s.key.equals(key)) {
                return s;
            }
        }
        return null;
    }
}
