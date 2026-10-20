package com.ultimateautobuilder.core.adventure;

/**
 * Types de structures générées dans le monde. {@code weight} est le poids de tirage dans son
 * groupe (les types rares ont un poids faible), {@code forest} réserve le type aux biomes boisés.
 */
public enum StructureType {
    // Maisons
    SMALL_WOOD_HOUSE("petite_maison_bois", "Petite maison en bois", Group.HOUSE, 16, false),
    STONE_HOUSE("maison_pierre", "Maison en pierre", Group.HOUSE, 12, false),
    ABANDONED_HOUSE("maison_abandonnee", "Maison abandonnée", Group.HOUSE, 10, false),
    DILAPIDATED_HOUSE("maison_delabree", "Maison délabrée", Group.HOUSE, 9, false),
    ISOLATED_HOUSE("maison_isolee", "Maison isolée", Group.HOUSE, 8, false),
    FOREST_HOUSE("maison_foret", "Maison cachée dans la forêt", Group.HOUSE, 10, true),
    OVERGROWN_HOUSE("maison_vegetation", "Maison envahie par la végétation", Group.HOUSE, 7, false),
    HOUSE_RUINS("maison_ruines", "Maison en ruines", Group.HOUSE, 9, false),
    CELLAR_HOUSE("maison_cave", "Maison avec cave", Group.HOUSE, 7, false),
    SECRET_ROOM_HOUSE("maison_piece_secrete", "Maison à la pièce secrète", Group.HOUSE, 5, false),
    HIDDEN_CHEST_HOUSE("maison_coffre_cache", "Maison au coffre caché", Group.HOUSE, 6, false),
    HIDDEN_PASSAGE_HOUSE("maison_passage", "Maison au passage dissimulé", Group.HOUSE, 5, false),
    RARE_HOUSE("maison_rare", "Maison rare", Group.HOUSE, 3, false),
    LEGENDARY_HOUSE("maison_legendaire", "Maison légendaire", Group.HOUSE, 1, false),
    // Châteaux et donjons
    SMALL_FORTRESS("petite_forteresse", "Petite forteresse", Group.CASTLE, 10, false),
    ABANDONED_CASTLE("chateau_abandonne", "Château abandonné", Group.CASTLE, 8, false),
    RUINED_CASTLE("chateau_ruines", "Château en ruines", Group.CASTLE, 9, false),
    UNDERGROUND_FORTRESS("forteresse_souterraine", "Forteresse souterraine", Group.CASTLE, 6, false),
    CURSED_CASTLE("chateau_maudit", "Château maudit", Group.CASTLE, 3, false),
    LEGENDARY_CASTLE("chateau_legendaire", "Château légendaire", Group.CASTLE, 1, false),
    // Uniques
    OP_SANCTUARY("sanctuaire_op", "Sanctuaire OP", Group.UNIQUE, 0, false),
    STARTER_HOUSE("maison_depart", "Maison de départ", Group.UNIQUE, 0, false);

    public enum Group { HOUSE, CASTLE, UNIQUE }

    public final String key;
    public final String label;
    public final Group group;
    public final int weight;
    public final boolean forest;

    StructureType(String key, String label, Group group, int weight, boolean forest) {
        this.key = key;
        this.label = label;
        this.group = group;
        this.weight = weight;
        this.forest = forest;
    }

    public static StructureType byKey(String key) {
        for (StructureType t : values()) {
            if (t.key.equals(key)) {
                return t;
            }
        }
        return null;
    }

    /** Tirage pondéré dans un groupe ; {@code forestBiome} autorise les types forestiers. */
    public static StructureType pick(Group group, boolean forestBiome, double roll) {
        int total = 0;
        for (StructureType t : values()) {
            if (t.group == group && (forestBiome || !t.forest)) {
                total += t.weight;
            }
        }
        double r = roll * total;
        int acc = 0;
        StructureType last = null;
        for (StructureType t : values()) {
            if (t.group == group && (forestBiome || !t.forest) && t.weight > 0) {
                acc += t.weight;
                last = t;
                if (r < acc) {
                    return t;
                }
            }
        }
        return last;
    }

    /** Espacement minimal en blocs avec les autres structures. */
    public int clearance() {
        return group == Group.CASTLE || this == OP_SANCTUARY ? 40 : 20;
    }
}
