package com.ultimateautobuilder.core.progression;

import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Table centrale des conditions de déblocage : niveau requis et coût en matériaux de chaque
 * construction. Règle par défaut selon la complexité et la catégorie, puis exceptions par
 * identifiant (collection Sakura notamment). Les modèles personnalisés du joueur sont libres.
 */
public final class ProgressionRules {
    private static final Map<String, UnlockRule> OVERRIDES = new HashMap<>();
    /** Catégories dont les modèles les plus complexes sont des constructions de prestige. */
    private static final Set<Category> PRESTIGE = Set.of(Category.CASTLES, Category.CITIES, Category.INSPIRED, Category.SAKURA);
    /** Grandes constructions : coût multiplié. */
    private static final Set<Category> LARGE = Set.of(Category.CASTLES, Category.CITIES, Category.ISLANDS);

    private ProgressionRules() {
    }

    /** Exception pour un modèle donné (appelée à l'enregistrement des collections). */
    public static synchronized void override(String id, UnlockRule rule) {
        OVERRIDES.put(id, rule);
    }

    public static synchronized UnlockRule rule(ModelEntry e) {
        if (e == null || e.category() == Category.CUSTOM || e.id().startsWith("custom:")) {
            return UnlockRule.FREE;
        }
        UnlockRule o = OVERRIDES.get(e.id());
        return o != null ? o : byDefault(e.category(), e.complexity());
    }

    static UnlockRule byDefault(Category c, int complexity) {
        double k = LARGE.contains(c) ? 1.5 : 1.0;
        return switch (complexity) {
            case 1, 2 -> UnlockRule.FREE;
            case 3 -> new UnlockRule(Tier.PIERRE, List.of(cost("minecraft:cobblestone", 48 * k)));
            case 4 -> new UnlockRule(Tier.FER, List.of(cost("minecraft:iron_ingot", 10 * k), cost("minecraft:cobblestone", 32 * k)));
            default -> PRESTIGE.contains(c)
                    ? new UnlockRule(Tier.NETHERITE, List.of(cost("minecraft:netherite_ingot", 1), cost("minecraft:diamond", 6 * k)))
                    : new UnlockRule(Tier.DIAMANT, List.of(cost("minecraft:diamond", 3 * k), cost("minecraft:iron_ingot", 16 * k)));
        };
    }

    public static ItemCost cost(String item, double count) {
        return new ItemCost(item, Math.max(1, (int) Math.round(count)));
    }

    /** Coût lisible : « 48 cobblestone, 3 diamond ». */
    public static String describe(List<ItemCost> cost) {
        if (cost.isEmpty()) {
            return "gratuit";
        }
        List<String> parts = new ArrayList<>();
        for (ItemCost c : cost) {
            parts.add(c.count() + " " + c.shortName());
        }
        return String.join(", ", parts);
    }
}
