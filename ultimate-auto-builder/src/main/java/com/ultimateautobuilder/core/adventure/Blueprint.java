package com.ultimateautobuilder.core.adventure;

import com.ultimateautobuilder.core.model.VoxelModel;

import java.util.List;

/**
 * Structure d'aventure prête à poser. Coordonnées de conception : y = 0 est le niveau du sol
 * (le plancher remplace la surface), la façade et l'entrée regardent vers +Z.
 * Les cellules « minecraft:cave_air » sont des volumes à vider (pièces, caves), les autres
 * cellules sont des blocs à poser.
 *
 * @param markers positions de conception des coffres à remplir (butin) et autres points spéciaux
 * @param entrance position de conception d'un point extérieur devant l'entrée (contrôle d'accès)
 */
public record Blueprint(StructureType type, VoxelModel model, List<Marker> markers, int[] entrance) {
    /** Point spécial : {@code kind} = « loot » (data = table de butin relative au mod). */
    public record Marker(int x, int y, int z, String kind, String data) {
    }

    /** Emprise horizontale en coordonnées de conception {minX, minZ, maxX, maxZ}. */
    public int[] footprint() {
        return new int[]{-model.anchorX(), -model.anchorZ(), model.sizeX() - 1 - model.anchorX(), model.sizeZ() - 1 - model.anchorZ()};
    }

    public int minY() {
        return -model.anchorY();
    }

    public int maxY() {
        return model.sizeY() - 1 - model.anchorY();
    }
}
