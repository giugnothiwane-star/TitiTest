package com.ultimateautobuilder.core.adventure;

/**
 * Raretés des clés et des serrures. Une clé n'ouvre que les serrures de sa propre rareté
 * (règle explicite : une clé légendaire n'ouvre pas une serrure commune, une clé commune
 * n'ouvre jamais une serrure OP). Les clés sont à usage unique, consommées après une ouverture
 * réussie seulement.
 */
public enum KeyTier {
    COMMON("common", "commune", "§f"),
    UNCOMMON("uncommon", "peu commune", "§a"),
    RARE("rare", "rare", "§9"),
    LEGENDARY("legendary", "légendaire", "§6"),
    OP("op", "OP", "§d");

    public final String key;
    public final String label;
    public final String color;

    KeyTier(String key, String label, String color) {
        this.key = key;
        this.label = label;
        this.color = color;
    }

    public boolean opens(KeyTier lock) {
        return this == lock;
    }

    /** Table de butin d'un coffre verrouillé de cette rareté (chemin dans data/ultimateautobuilder/loot_table). */
    public String lockedLootTable() {
        return "chests/locked_" + key;
    }

    public static KeyTier byKey(String key) {
        for (KeyTier t : values()) {
            if (t.key.equals(key)) {
                return t;
            }
        }
        return null;
    }
}
