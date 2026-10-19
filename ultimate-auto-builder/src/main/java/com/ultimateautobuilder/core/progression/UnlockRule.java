package com.ultimateautobuilder.core.progression;

import java.util.List;

/**
 * Conditions de déblocage d'une construction : niveau d'architecte minimal et matériaux à
 * fournir une seule fois. Distinct des blocs nécessaires à la construction elle-même.
 */
public record UnlockRule(Tier tier, List<ItemCost> cost) {
    public static final UnlockRule FREE = new UnlockRule(Tier.DEBUTANT, List.of());

    public UnlockRule {
        tier = tier == null ? Tier.DEBUTANT : tier;
        cost = cost == null ? List.of() : List.copyOf(cost);
    }

    /** Débloquée d'office (construction de départ). */
    public boolean free() {
        return tier == Tier.DEBUTANT && cost.isEmpty();
    }
}
