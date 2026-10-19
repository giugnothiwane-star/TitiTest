package com.ultimateautobuilder.core.progression;

/** Niveaux d'architecte : chaque niveau donne accès aux constructions de son rang. */
public enum Tier {
    DEBUTANT("Architecte débutant", "minecraft:oak_planks"),
    PIERRE("Constructeur en pierre", "minecraft:cobblestone"),
    FER("Architecte du fer", "minecraft:iron_ingot"),
    DIAMANT("Architecte du diamant", "minecraft:diamond"),
    NETHERITE("Architecte Netherite", "minecraft:netherite_ingot");

    public final String label;
    /** Objet servant d'icône au niveau. */
    public final String icon;

    Tier(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public static Tier byOrdinal(int i) {
        Tier[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }

    public boolean atLeast(Tier other) {
        return ordinal() >= other.ordinal();
    }
}
