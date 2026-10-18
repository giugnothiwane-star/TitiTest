package com.ultimateautobuilder.core.importer;

import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.validation.Limits;
import com.ultimateautobuilder.core.validation.StructureValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulateur neutre utilisé par tous les importeurs (NBT vanilla, .vox, sélection en jeu) :
 * chaque bloc est vérifié au fil de l'eau pour qu'un fichier hostile soit rejeté avant
 * d'occuper de la mémoire.
 */
public final class RawStructure {
    private final int sizeX, sizeY, sizeZ;
    private final Limits limits;
    private final List<String> palette = new ArrayList<>();
    private final VoxelGrid grid = new VoxelGrid();
    private int blocks;
    private int skippedInvalid;

    public RawStructure(int sizeX, int sizeY, int sizeZ, Limits limits) throws ImportException {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new ImportException("Dimensions nulles ou négatives : " + sizeX + "x" + sizeY + "x" + sizeZ);
        }
        int max = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (max > limits.maxDimension()) {
            throw new ImportException("Dimension " + max + " supérieure à la limite de " + limits.maxDimension());
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.limits = limits;
    }

    /** Ajoute un état à la palette ; renvoie son index. Les états invalides deviennent null (ignorés). */
    public int addPaletteEntry(String state) throws ImportException {
        if (palette.size() >= limits.maxPaletteSize()) {
            throw new ImportException("Palette trop grande (> " + limits.maxPaletteSize() + " états)");
        }
        palette.add(StructureValidator.isValidState(state) ? state : null);
        return palette.size() - 1;
    }

    public int paletteSize() {
        return palette.size();
    }

    public String paletteEntry(int index) {
        return palette.get(index);
    }

    public void setPaletteEntry(int index, String state) {
        palette.set(index, StructureValidator.isValidState(state) ? state : null);
    }

    public void put(int x, int y, int z, int paletteIndex) throws ImportException {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
            throw new ImportException("Bloc hors des dimensions déclarées : " + x + "," + y + "," + z);
        }
        if (paletteIndex < 0 || paletteIndex >= palette.size()) {
            throw new ImportException("Index de palette invalide : " + paletteIndex);
        }
        String state = palette.get(paletteIndex);
        if (state == null) {
            skippedInvalid++;
            return;
        }
        if (B.isAir(state) || state.equals("minecraft:structure_void")) {
            return;
        }
        if (++blocks > limits.maxBlocks()) {
            throw new ImportException("Trop de blocs (> " + limits.maxBlocks() + ")");
        }
        grid.set(x, y, z, state);
    }

    public void put(int x, int y, int z, String state) throws ImportException {
        int idx = palette.indexOf(state);
        if (idx < 0) {
            idx = addPaletteEntry(state);
        }
        put(x, y, z, idx);
    }

    public int skippedInvalid() {
        return skippedInvalid;
    }

    /** Modèle final ancré au centre de la base. */
    public VoxelModel toModel() throws ImportException {
        if (grid.count() == 0) {
            throw new ImportException("La structure ne contient aucun bloc");
        }
        VoxelModel m = grid.freeze();
        return m.withAnchor(m.sizeX() / 2, 0, m.sizeZ() / 2);
    }
}
