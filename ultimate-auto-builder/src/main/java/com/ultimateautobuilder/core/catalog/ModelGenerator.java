package com.ultimateautobuilder.core.catalog;

import com.ultimateautobuilder.core.model.VoxelModel;

/**
 * Interface commune à tous les générateurs (voxel, architecture, paysages, villes, importés).
 * Une implémentation doit être pure : sans état partagé et sans accès au monde, afin de pouvoir
 * s'exécuter hors du thread serveur et être rejouée à l'identique.
 */
@FunctionalInterface
public interface ModelGenerator {
    VoxelModel generate(GenParams params);
}
