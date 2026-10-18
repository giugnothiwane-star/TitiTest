package com.ultimateautobuilder.core.validation;

/**
 * Limites de sécurité appliquées à toute construction et à tout modèle importé.
 *
 * @param maxBlocks          nombre maximal de blocs d'une construction
 * @param maxDimension       dimension maximale sur chaque axe
 * @param giantThreshold     au-delà, une confirmation explicite est exigée
 * @param maxPaletteSize     nombre maximal d'états distincts (fichiers importés)
 */
public record Limits(int maxBlocks, int maxDimension, int giantThreshold, int maxPaletteSize) {
    public static final Limits DEFAULT = new Limits(2_000_000, 512, 150_000, 4096);

    public Limits {
        if (maxBlocks <= 0 || maxDimension <= 0 || giantThreshold <= 0 || maxPaletteSize <= 0) {
            throw new IllegalArgumentException("Limites invalides");
        }
    }
}
