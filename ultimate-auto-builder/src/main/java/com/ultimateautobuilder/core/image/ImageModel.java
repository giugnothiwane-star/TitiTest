package com.ultimateautobuilder.core.image;

import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Volume dense issu d'une image : chaque bloc porte son état et son degré de certitude
 * (vu directement sur une image, ou estimé). Axes : x = largeur, y = hauteur (0 en bas),
 * z = profondeur (0 = face avant, celle de l'image principale). Volume borné à {@link #MAX_CELLS}.
 */
public final class ImageModel {
    public static final int MAX_CELLS = 4_194_304;
    public static final int MAX_SIDE = 512;
    public static final byte EMPTY = 0, VISIBLE = 1, ESTIMATED = 2;

    private final int sizeX, sizeY, sizeZ;
    private final short[] cells;
    private final byte[] certainty;
    private final List<String> palette;
    private final Map<String, Integer> paletteIndex;

    public ImageModel(int sizeX, int sizeY, int sizeZ) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || sizeX > MAX_SIDE || sizeY > MAX_SIDE || sizeZ > MAX_SIDE
                || (long) sizeX * sizeY * sizeZ > MAX_CELLS) {
            throw new IllegalArgumentException("Volume refusé : " + sizeX + "×" + sizeY + "×" + sizeZ);
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.cells = new short[sizeX * sizeY * sizeZ];
        this.certainty = new byte[cells.length];
        this.palette = new ArrayList<>();
        this.paletteIndex = new HashMap<>();
    }

    private ImageModel(ImageModel o) {
        this.sizeX = o.sizeX;
        this.sizeY = o.sizeY;
        this.sizeZ = o.sizeZ;
        this.cells = o.cells.clone();
        this.certainty = o.certainty.clone();
        this.palette = new ArrayList<>(o.palette);
        this.paletteIndex = new HashMap<>(o.paletteIndex);
    }

    public ImageModel copy() {
        return new ImageModel(this);
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    private int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    private int idOf(String state) {
        Integer id = paletteIndex.get(state);
        if (id == null) {
            if (palette.size() >= Short.MAX_VALUE - 1) {
                throw new IllegalStateException("Palette trop grande");
            }
            palette.add(state);
            id = palette.size();
            paletteIndex.put(state, id);
        }
        return id;
    }

    public void set(int x, int y, int z, String state, byte cert) {
        int i = index(x, y, z);
        cells[i] = (short) idOf(state);
        certainty[i] = cert == EMPTY ? ESTIMATED : cert;
    }

    public void clear(int x, int y, int z) {
        int i = index(x, y, z);
        cells[i] = 0;
        certainty[i] = EMPTY;
    }

    public boolean filled(int x, int y, int z) {
        return inside(x, y, z) && cells[index(x, y, z)] != 0;
    }

    /** État du bloc, ou null si vide. */
    public String get(int x, int y, int z) {
        int id = cells[index(x, y, z)];
        return id == 0 ? null : palette.get(id - 1);
    }

    public byte certainty(int x, int y, int z) {
        return certainty[index(x, y, z)];
    }

    public int count() {
        int n = 0;
        for (short c : cells) {
            if (c != 0) {
                n++;
            }
        }
        return n;
    }

    public int count(byte cert) {
        int n = 0;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] != 0 && certainty[i] == cert) {
                n++;
            }
        }
        return n;
    }

    /** Matériaux par quantité décroissante. */
    public Map<String, Integer> materials() {
        int[] counts = new int[palette.size() + 1];
        for (short c : cells) {
            counts[c]++;
        }
        List<Integer> ids = new ArrayList<>();
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > 0) {
                ids.add(i);
            }
        }
        ids.sort((a, b) -> Integer.compare(counts[b], counts[a]));
        Map<String, Integer> out = new LinkedHashMap<>();
        for (int id : ids) {
            out.put(palette.get(id - 1), counts[id]);
        }
        return out;
    }

    /** Nombre de blocs par couche (index = y). */
    public int[] layerCounts() {
        int[] out = new int[sizeY];
        for (int y = 0; y < sizeY; y++) {
            int n = 0;
            for (int z = 0; z < sizeZ; z++) {
                int base = index(0, y, z);
                for (int x = 0; x < sizeX; x++) {
                    if (cells[base + x] != 0) {
                        n++;
                    }
                }
            }
            out[y] = n;
        }
        return out;
    }

    /** Remplace un matériau par un autre ; renvoie le nombre de blocs modifiés. */
    public int replace(String from, String to) {
        Integer id = paletteIndex.get(from);
        if (id == null || from.equals(to)) {
            return 0;
        }
        short nid = (short) idOf(to);
        int n = 0;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == id) {
                cells[i] = nid;
                n++;
            }
        }
        return n;
    }

    public int remove(String state) {
        Integer id = paletteIndex.get(state);
        if (id == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == id) {
                cells[i] = 0;
                certainty[i] = EMPTY;
                n++;
            }
        }
        return n;
    }

    /** Vide l'intérieur (blocs dont les six voisins sont pleins) ; la couche du bas est conservée si demandé. */
    public int hollow(boolean keepFloor) {
        List<Integer> toClear = new ArrayList<>();
        for (int y = keepFloor ? 1 : 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    if (filled(x, y, z) && filled(x + 1, y, z) && filled(x - 1, y, z) && filled(x, y + 1, z) && filled(x, y - 1, z)
                            && filled(x, y, z + 1) && filled(x, y, z - 1)) {
                        toClear.add(index(x, y, z));
                    }
                }
            }
        }
        for (int i : toClear) {
            cells[i] = 0;
            certainty[i] = EMPTY;
        }
        return toClear.size();
    }

    /** Copie transformée : rotation d'un quart de tour autour de Y (sens horaire vu du dessus). */
    public ImageModel rotated() {
        ImageModel out = new ImageModel(sizeZ, sizeY, sizeX);
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int i = index(x, y, z);
                    if (cells[i] != 0) {
                        out.set(sizeZ - 1 - z, y, x, palette.get(cells[i] - 1), certainty[i]);
                    }
                }
            }
        }
        return out;
    }

    public ImageModel mirroredX() {
        ImageModel out = new ImageModel(sizeX, sizeY, sizeZ);
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int i = index(x, y, z);
                    if (cells[i] != 0) {
                        out.set(sizeX - 1 - x, y, z, palette.get(cells[i] - 1), certainty[i]);
                    }
                }
            }
        }
        return out;
    }

    /** Mise à l'échelle au plus proche voisin (aucune couleur inventée). */
    public ImageModel scaled(double k) {
        int nx = clampSide(sizeX * k), ny = clampSide(sizeY * k), nz = clampSide(sizeZ * k);
        while ((long) nx * ny * nz > MAX_CELLS) {
            nx = Math.max(1, nx * 9 / 10);
            ny = Math.max(1, ny * 9 / 10);
            nz = Math.max(1, nz * 9 / 10);
        }
        ImageModel out = new ImageModel(nx, ny, nz);
        for (int y = 0; y < ny; y++) {
            int sy = Math.min(sizeY - 1, y * sizeY / ny);
            for (int z = 0; z < nz; z++) {
                int sz = Math.min(sizeZ - 1, z * sizeZ / nz);
                for (int x = 0; x < nx; x++) {
                    int sx = Math.min(sizeX - 1, x * sizeX / nx);
                    int i = index(sx, sy, sz);
                    if (cells[i] != 0) {
                        out.set(x, y, z, palette.get(cells[i] - 1), certainty[i]);
                    }
                }
            }
        }
        return out;
    }

    private static int clampSide(double v) {
        return Math.max(1, Math.min(MAX_SIDE, (int) Math.round(v)));
    }

    /** Supprime les couches strictement au-dessus de {@code maxY} et en dessous de {@code minY}. */
    public ImageModel cropLayers(int minY, int maxY) {
        int a = Math.max(0, Math.min(sizeY - 1, Math.min(minY, maxY))), b = Math.max(a, Math.min(sizeY - 1, Math.max(minY, maxY)));
        ImageModel out = new ImageModel(sizeX, b - a + 1, sizeZ);
        for (int y = a; y <= b; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int i = index(x, y, z);
                    if (cells[i] != 0) {
                        out.set(x, y - a, z, palette.get(cells[i] - 1), certainty[i]);
                    }
                }
            }
        }
        return out;
    }

    /** Conversion vers le format du moteur de construction (sol en bas, ancre au centre de la base). */
    public VoxelModel toVoxelModel() {
        VoxelGrid g = new VoxelGrid();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int id = cells[index(x, y, z)];
                    if (id != 0) {
                        g.set(x, y, z, palette.get(id - 1));
                    }
                }
            }
        }
        return g.freeze().groundAtBottom();
    }

    /** Reconstruction depuis un modèle sauvegardé ; les certitudes sont reprises si elles correspondent. */
    public static ImageModel fromVoxelModel(VoxelModel m, byte[] cert) {
        ImageModel out = new ImageModel(Math.max(1, m.sizeX()), Math.max(1, m.sizeY()), Math.max(1, m.sizeZ()));
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (int i = 0; i < m.size(); i++) {
            long c = m.cell(i);
            minX = Math.min(minX, VoxelModel.cx(c));
            minY = Math.min(minY, VoxelModel.cy(c));
            minZ = Math.min(minZ, VoxelModel.cz(c));
        }
        boolean useCert = cert != null && cert.length == out.cells.length;
        for (int i = 0; i < m.size(); i++) {
            long c = m.cell(i);
            int x = VoxelModel.cx(c) - minX, y = VoxelModel.cy(c) - minY, z = VoxelModel.cz(c) - minZ;
            if (out.inside(x, y, z)) {
                int idx = out.index(x, y, z);
                out.set(x, y, z, m.stateOf(c), useCert ? cert[idx] : ESTIMATED);
            }
        }
        return out;
    }

    public byte[] certaintyCopy() {
        return Arrays.copyOf(certainty, certainty.length);
    }
}
