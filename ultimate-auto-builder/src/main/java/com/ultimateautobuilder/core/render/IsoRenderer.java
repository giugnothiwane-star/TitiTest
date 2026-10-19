package com.ultimateautobuilder.core.render;

import com.ultimateautobuilder.core.model.VoxelModel;

/**
 * Rendu isométrique logiciel d'un modèle voxel vers une image ARGB.
 * Utilisé pour les miniatures du catalogue ; s'exécute hors du thread de rendu.
 * Chaque voxel est dessiné en 2×3 pixels (face supérieure claire, faces latérales ombrées),
 * dans l'ordre arrière → avant grâce à un tri par seau sur x + y + z.
 */
public final class IsoRenderer {
    private IsoRenderer() {
    }

    public record Image(int width, int height, int[] argb) {
    }

    /**
     * @param view rotation de la vue par quarts de tour (0 = façade et flanc droit visibles)
     * @param maxCells au-delà, le modèle est sous-échantillonné pour borner le coût
     */
    public static Image render(VoxelModel model, int view, int maxCells) {
        if (model.isEmpty()) {
            return new Image(1, 1, new int[1]);
        }
        int step = 1;
        while ((long) (model.sizeX() / step + 1) * (model.sizeY() / step + 1) * (model.sizeZ() / step + 1) > (long) maxCells * 8
                && step < 64) {
            step++;
        }
        int v = Math.floorMod(view, 4);
        int sx = (model.sizeX() + step - 1) / step;
        int sy = (model.sizeY() + step - 1) / step;
        int sz = (model.sizeZ() + step - 1) / step;
        int rx = (v & 1) == 0 ? sx : sz;
        int rz = (v & 1) == 0 ? sz : sx;
        int[] colors = new int[model.paletteSize()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = ColorTable.rgb(model.paletteEntry(i));
        }
        // Grille dense sous-échantillonnée (le premier bloc visible d'une cellule l'emporte).
        int[] grid = new int[rx * sy * rz];
        java.util.Arrays.fill(grid, -1);
        for (int i = 0; i < model.size(); i++) {
            long c = model.cell(i);
            int col = colors[VoxelModel.stateId(c)];
            if (col < 0) {
                continue;
            }
            int x = VoxelModel.cx(c) / step, y = VoxelModel.cy(c) / step, z = VoxelModel.cz(c) / step;
            int tx, tz;
            switch (v) {
                case 1 -> {
                    tx = z;
                    tz = sx - 1 - x;
                }
                case 2 -> {
                    tx = sx - 1 - x;
                    tz = sz - 1 - z;
                }
                case 3 -> {
                    tx = sz - 1 - z;
                    tz = x;
                }
                default -> {
                    tx = x;
                    tz = z;
                }
            }
            int idx = (y * rz + tz) * rx + tx;
            if (grid[idx] < 0) {
                grid[idx] = col;
            }
        }
        int width = 2 * (rx + rz) + 2;
        int height = (rx + rz) + 2 * sy + 3;
        int[] img = new int[width * height];
        int maxD = rx + sy + rz;
        // Seaux par profondeur : du plus lointain (petit x+y+z) au plus proche.
        for (int d = 0; d <= maxD; d++) {
            for (int y = Math.max(0, d - rx - rz + 2); y < sy && y <= d; y++) {
                for (int tz = 0; tz < rz; tz++) {
                    int tx = d - y - tz;
                    if (tx < 0 || tx >= rx) {
                        continue;
                    }
                    int col = grid[(y * rz + tz) * rx + tx];
                    if (col < 0) {
                        continue;
                    }
                    // Faces masquées par des voisins : inutile de dessiner.
                    boolean topHidden = y + 1 < sy && grid[((y + 1) * rz + tz) * rx + tx] >= 0;
                    boolean rightHidden = tx + 1 < rx && grid[(y * rz + tz) * rx + tx + 1] >= 0;
                    boolean frontHidden = tz + 1 < rz && grid[(y * rz + tz + 1) * rx + tx] >= 0;
                    if (topHidden && rightHidden && frontHidden) {
                        continue;
                    }
                    int px = 2 * (tx - tz) + 2 * rz;
                    int py = (tx + tz) - 2 * y + 2 * sy;
                    int top = 0xFF000000 | ColorTable.shade(col, 1.08);
                    int left = 0xFF000000 | ColorTable.shade(col, 0.82);
                    int right = 0xFF000000 | ColorTable.shade(col, 0.64);
                    put(img, width, height, px, py, top);
                    put(img, width, height, px + 1, py, top);
                    put(img, width, height, px, py + 1, left);
                    put(img, width, height, px + 1, py + 1, right);
                    put(img, width, height, px, py + 2, left);
                    put(img, width, height, px + 1, py + 2, right);
                }
            }
        }
        return new Image(width, height, img);
    }

    private static void put(int[] img, int w, int h, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < w && y < h) {
            img[y * w + x] = argb;
        }
    }

    /** Redimensionne au plus proche voisin en conservant les proportions, centré dans size×size. */
    public static Image fit(Image src, int size) {
        int[] out = new int[size * size];
        double k = Math.min((double) size / src.width(), (double) size / src.height());
        int w = Math.max(1, (int) (src.width() * k));
        int h = Math.max(1, (int) (src.height() * k));
        int ox = (size - w) / 2, oy = (size - h) / 2;
        for (int y = 0; y < h; y++) {
            int sy = Math.min(src.height() - 1, (int) (y / k));
            for (int x = 0; x < w; x++) {
                int sxp = Math.min(src.width() - 1, (int) (x / k));
                out[(y + oy) * size + x + ox] = src.argb()[sy * src.width() + sxp];
            }
        }
        return new Image(size, size, out);
    }
}
