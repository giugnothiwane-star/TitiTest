package com.ultimateautobuilder.core.progression;

/** Quantité d'un objet Minecraft (identifiant complet, par exemple « minecraft:diamond »). */
public record ItemCost(String item, int count) {
    public ItemCost {
        if (item == null || !item.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Identifiant d'objet invalide : " + item);
        }
        if (count <= 0 || count > 64 * 36) {
            throw new IllegalArgumentException("Quantité invalide pour " + item + " : " + count);
        }
    }

    /** Nom court affichable (« diamond » pour « minecraft:diamond »). */
    public String shortName() {
        return item.substring(item.indexOf(':') + 1);
    }
}
