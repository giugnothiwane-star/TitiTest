package com.ultimateautobuilder.core.progression;

/** Inventaire d'un joueur vu par la progression (implémenté côté serveur sur le vrai inventaire). */
public interface MaterialInventory {
    /** Nombre d'exemplaires de l'objet présents. */
    int count(String item);

    /** Retire exactement {@code amount} exemplaires ; appelé uniquement après vérification. */
    void remove(String item, int amount);
}
