package com.ultimateautobuilder.core.adventure;

/**
 * Catégorie d'un spawner de château : elle impose la variante de toutes les créatures qu'il
 * produit. Les multiplicateurs s'appliquent aux attributs de base de la créature (taille réelle
 * via l'attribut d'échelle de Minecraft 1.21, donc rendu et boîte de collision cohérents).
 */
public enum SpawnCategory {
    BABY("bebe", "bébé", 0.5, 0.5, 0.6, 1.25),
    MEDIUM("moyen", "moyenne", 0.75, 0.75, 0.8, 1.1),
    NORMAL("normal", "normale", 1.0, 1.0, 1.0, 1.0);

    public final String key;
    public final String label;
    public final double scale;
    public final double health;
    public final double damage;
    public final double speed;

    SpawnCategory(String key, String label, double scale, double health, double damage, double speed) {
        this.key = key;
        this.label = label;
        this.scale = scale;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
    }

    public static SpawnCategory byKey(String key) {
        for (SpawnCategory c : values()) {
            if (c.key.equals(key)) {
                return c;
            }
        }
        return null;
    }
}
