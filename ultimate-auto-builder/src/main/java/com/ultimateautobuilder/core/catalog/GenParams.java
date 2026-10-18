package com.ultimateautobuilder.core.catalog;

/**
 * Paramètres de génération.
 *
 * @param size   valeur de la dimension pilotée (voir {@link SizeSpec.Kind})
 * @param seed   graine déterministe
 * @param hollow vider l'intérieur des volumes pleins (statues, créatures)
 */
public record GenParams(int size, long seed, boolean hollow) {
    public GenParams {
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
    }
}
