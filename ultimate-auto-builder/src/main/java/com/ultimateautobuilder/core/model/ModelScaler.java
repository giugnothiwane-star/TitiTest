package com.ultimateautobuilder.core.model;

/**
 * Mise à l'échelle au plus proche voisin d'un modèle figé (modèles importés ou enregistrés,
 * qui n'ont pas de description géométrique). Chaque cellule cible échantillonne la cellule
 * source correspondante : pas de trou à l'agrandissement, proportions conservées.
 */
public final class ModelScaler {
    private ModelScaler() {
    }

    /** Redimensionne pour que la hauteur vaille {@code targetHeight}. */
    public static VoxelModel toHeight(VoxelModel m, int targetHeight) {
        if (m.isEmpty() || m.sizeY() == targetHeight || targetHeight <= 0) {
            return m;
        }
        return scale(m, (double) targetHeight / m.sizeY());
    }

    public static VoxelModel scale(VoxelModel m, double k) {
        if (m.isEmpty() || Math.abs(k - 1.0) < 1e-9) {
            return m;
        }
        int nx = Math.max(1, (int) Math.round(m.sizeX() * k));
        int ny = Math.max(1, (int) Math.round(m.sizeY() * k));
        int nz = Math.max(1, (int) Math.round(m.sizeZ() * k));
        if (Math.max(nx, Math.max(ny, nz)) > VoxelModel.MAX_AXIS) {
            throw new IllegalArgumentException("Échelle trop grande");
        }
        VoxelGrid g = new VoxelGrid();
        // Index dense de la source : accès O(1) pendant l'échantillonnage.
        int sx = m.sizeX(), sy = m.sizeY(), sz = m.sizeZ();
        long volume = (long) sx * sy * sz;
        if (volume > 64_000_000L) {
            throw new IllegalArgumentException("Modèle source trop volumineux pour être redimensionné");
        }
        int[] dense = new int[(int) volume];
        for (int i = 0; i < m.size(); i++) {
            long c = m.cell(i);
            dense[(VoxelModel.cy(c) * sz + VoxelModel.cz(c)) * sx + VoxelModel.cx(c)] = VoxelModel.stateId(c) + 1;
        }
        for (int y = 0; y < ny; y++) {
            int syi = Math.min(sy - 1, (int) (y / k));
            for (int z = 0; z < nz; z++) {
                int szi = Math.min(sz - 1, (int) (z / k));
                for (int x = 0; x < nx; x++) {
                    int sxi = Math.min(sx - 1, (int) (x / k));
                    int v = dense[(syi * sz + szi) * sx + sxi];
                    if (v != 0) {
                        g.set(x, y, z, m.paletteEntry(v - 1));
                    }
                }
            }
        }
        VoxelModel out = g.freeze();
        return out.withAnchor((int) Math.round(m.anchorX() * k), (int) Math.round(m.anchorY() * k), (int) Math.round(m.anchorZ() * k));
    }
}
