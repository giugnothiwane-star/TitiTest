package com.ultimateautobuilder.core.catalog;

/** Catégories du catalogue, avec l'objectif de répartition du cahier des charges. */
public enum Category {
    POKEMON("pokemon", "Pokémon", 40),
    ANIME("anime", "Anime et personnages", 35),
    HOUSES("houses", "Maisons et villas", 45),
    CASTLES("castles", "Châteaux et médiéval", 30),
    CITIES("cities", "Villes et bâtiments urbains", 35),
    ANIMALS("animals", "Animaux et créatures", 30),
    DRAGONS("dragons", "Dragons et monstres", 20),
    STATUES("statues", "Statues géantes", 25),
    NATURE("nature", "Arbres et nature", 20),
    ISLANDS("islands", "Îles flottantes et paysages", 20),
    VEHICLES("vehicles", "Véhicules et machines", 20),
    SCIFI("scifi", "Science-fiction et espace", 20),
    DUNGEONS("dungeons", "Donjons et temples", 20),
    PIXELART("pixelart", "Pixel art et sculptures", 15),
    HUMOR("humor", "Structures humoristiques", 10),
    MISC("misc", "Structures diverses", 15),
    MOBS("mobs", "Mobs Minecraft", 88),
    INSPIRED("inspirations", "Inspirations (vos images)", 19),
    CUSTOM("custom", "Structures personnalisées", 0);

    private final String key;
    private final String displayName;
    private final int target;

    Category(String key, String displayName, int target) {
        this.key = key;
        this.displayName = displayName;
        this.target = target;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public int target() {
        return target;
    }

    public static Category byKey(String key) {
        for (Category c : values()) {
            if (c.key.equals(key)) {
                return c;
            }
        }
        return null;
    }
}
