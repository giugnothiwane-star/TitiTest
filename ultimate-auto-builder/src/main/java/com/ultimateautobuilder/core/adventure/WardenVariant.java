package com.ultimateautobuilder.core.adventure;

/**
 * Variantes du Warden. {@link #weight} forme la distribution de sélection (total 100) appliquée
 * une fois le système Warden déclenché ; voir {@link SpawnRules#pickWardenVariant}.
 */
public enum WardenVariant {
    NORMAL("normal", "Warden", SpawnCategory.NORMAL, 70, 1.0, 1.0, 1.0),
    BABY("bebe", "Bébé Warden", SpawnCategory.BABY, 14, 0.5, 0.4, 0.5),
    MEDIUM("moyen", "Warden moyen", SpawnCategory.MEDIUM, 14, 0.75, 0.7, 0.75),
    /** Boss : uniquement dans les spawners « normaux » des châteaux maudits ou légendaires. */
    BOSS("boss", "Boss Warden", SpawnCategory.NORMAL, 2, 1.4, 2.0, 1.5);

    public final String key;
    public final String displayName;
    /** Catégorie de spawner dans laquelle la variante est admise. */
    public final SpawnCategory category;
    public final int weight;
    public final double scale;
    public final double health;
    public final double damage;

    WardenVariant(String key, String displayName, SpawnCategory category, int weight, double scale, double health, double damage) {
        this.key = key;
        this.displayName = displayName;
        this.category = category;
        this.weight = weight;
        this.scale = scale;
        this.health = health;
        this.damage = damage;
    }

    public static WardenVariant byKey(String key) {
        for (WardenVariant v : values()) {
            if (v.key.equals(key)) {
                return v;
            }
        }
        return null;
    }
}
