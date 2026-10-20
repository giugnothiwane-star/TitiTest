package com.ultimateautobuilder.core.adventure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Règles de sélection des créatures des spawners, centralisées et testables.
 *
 * <p>Pour chaque tentative de spawn valide : un seul tirage décide du système Warden
 * ({@link #WARDEN_CHANCE}, 0,5 %). Sinon, une créature ordinaire est tirée uniformément parmi les
 * créatures éligibles de la famille du spawner. Le Warden n'appartient jamais à la liste des
 * créatures ordinaires.</p>
 */
public final class SpawnRules {
    /** Probabilité de déclencher le système Warden à chaque tentative valide. */
    public static final double WARDEN_CHANCE = 0.005;

    /** Créatures jamais sélectionnées automatiquement (taille, destruction, invincibilité, mécanique propre). */
    public static final Set<String> FORBIDDEN = Set.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden",
            "minecraft:giant", "minecraft:elder_guardian", "minecraft:ghast", "minecraft:ravager", "minecraft:creeper",
            "minecraft:enderman", "minecraft:slime", "minecraft:magma_cube", "minecraft:iron_golem", "minecraft:evoker",
            "minecraft:shulker", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:hoglin", "minecraft:zoglin",
            "minecraft:phantom", "minecraft:breeze", "minecraft:vex");

    /** Source d'aléa minimale (adaptable au générateur de Minecraft). */
    public interface Dice {
        double nextDouble();

        int nextInt(int bound);
    }

    public sealed interface Choice permits Ordinary, Warden {
    }

    public record Ordinary(String entityId) implements Choice {
    }

    public record Warden(WardenVariant variant) implements Choice {
    }

    private SpawnRules() {
    }

    /**
     * Créatures ordinaires éligibles pour une famille et une catégorie. {@code supportsCategory}
     * retire les combinaisons qui ne peuvent pas recevoir la variante demandée (aucun repli vers
     * une créature adulte).
     */
    public static List<String> eligible(SpawnFamily family, SpawnCategory category, Predicate<String> supportsCategory) {
        List<String> out = new ArrayList<>();
        for (String id : family.members) {
            if (!FORBIDDEN.contains(id) && supportsCategory.test(id) && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Tirage uniforme : chaque créature éligible a exactement la même probabilité. */
    public static String pickOrdinary(List<String> eligible, Dice dice) {
        if (eligible.isEmpty()) {
            return null;
        }
        return eligible.get(dice.nextInt(eligible.size()));
    }

    /**
     * Variante de Warden. Sans contrainte ({@code category == null}) : distribution complète
     * (normal 70 %, bébé 14 %, moyen 14 %, boss 2 %). Dans un spawner de catégorie : distribution
     * conditionnée aux variantes admises par cette catégorie (bébé → bébé, moyen → moyen,
     * normal → normal 70/72 ou boss 2/72), afin que les catégories ne se mélangent jamais.
     */
    public static WardenVariant pickWardenVariant(SpawnCategory category, Dice dice) {
        int total = 0;
        for (WardenVariant v : WardenVariant.values()) {
            if (category == null || v.category == category) {
                total += v.weight;
            }
        }
        if (total <= 0) {
            return null;
        }
        double r = dice.nextDouble() * total;
        double acc = 0;
        WardenVariant last = null;
        for (WardenVariant v : WardenVariant.values()) {
            if (category == null || v.category == category) {
                acc += v.weight;
                last = v;
                if (r < acc) {
                    return v;
                }
            }
        }
        return last;
    }

    /**
     * Une tentative complète : tirage Warden (si le spawner l'autorise) puis, sinon, créature
     * ordinaire équiprobable. Renvoie null si aucune créature n'est éligible.
     */
    public static Choice choose(SpawnFamily family, SpawnCategory category, boolean wardenAllowed, Predicate<String> supportsCategory, Dice dice) {
        if (wardenAllowed && dice.nextDouble() < WARDEN_CHANCE) {
            WardenVariant v = pickWardenVariant(category, dice);
            if (v != null) {
                return new Warden(v);
            }
        }
        String id = pickOrdinary(eligible(family, category, supportsCategory), dice);
        return id == null ? null : new Ordinary(id);
    }

    /** Variantes « bébé » natives de Minecraft (sinon la variante passe par l'attribut d'échelle). */
    public static boolean hasNativeBaby(String entityId) {
        return entityId.equals("minecraft:zombie") || entityId.equals("minecraft:husk") || entityId.equals("minecraft:zombie_villager")
                || entityId.equals("minecraft:drowned");
    }
}
