package com.ultimateautobuilder.core.model;

import java.util.Arrays;

/**
 * Modèle voxel immuable et compact : chaque cellule est un long (y, z, x, palette) trié,
 * donc l'itération se fait naturellement couche par couche, du bas vers le haut.
 */
public final class VoxelModel {
    public static final int MAX_AXIS = 4095;
    public static final VoxelModel EMPTY = new VoxelModel(0, 0, 0, 0, 0, 0, new String[0], new long[0]);

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final String[] palette;
    private final long[] cells;

    VoxelModel(int sizeX, int sizeY, int sizeZ, int anchorX, int anchorY, int anchorZ, String[] palette, long[] cells) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.palette = palette;
        this.cells = cells;
    }

    /** Fabrique utilisée par les importeurs : les cellules sont validées et triées. */
    public static VoxelModel of(int sizeX, int sizeY, int sizeZ, int anchorX, int anchorY, int anchorZ,
                                String[] palette, long[] cells) {
        if (sizeX < 0 || sizeY < 0 || sizeZ < 0 || sizeX > MAX_AXIS || sizeY > MAX_AXIS || sizeZ > MAX_AXIS) {
            throw new IllegalArgumentException("Dimensions invalides");
        }
        long[] copy = cells.clone();
        for (long c : copy) {
            if (cx(c) >= sizeX || cy(c) >= sizeY || cz(c) >= sizeZ || stateId(c) >= palette.length) {
                throw new IllegalArgumentException("Cellule hors limites");
            }
        }
        Arrays.sort(copy);
        for (int i = 1; i < copy.length; i++) {
            if ((copy[i] >>> 16) == (copy[i - 1] >>> 16)) {
                throw new IllegalArgumentException("Cellule en double");
            }
        }
        return new VoxelModel(sizeX, sizeY, sizeZ, anchorX, anchorY, anchorZ, palette.clone(), copy);
    }

    static long pack(int x, int y, int z, int state) {
        return ((long) y << 40) | ((long) z << 28) | ((long) x << 16) | state;
    }

    public static long packCell(int x, int y, int z, int state) {
        if (x < 0 || y < 0 || z < 0 || x > MAX_AXIS || y > MAX_AXIS || z > MAX_AXIS || state < 0 || state > 0xFFFF) {
            throw new IllegalArgumentException("Cellule invalide");
        }
        return pack(x, y, z, state);
    }

    public static int cx(long c) {
        return (int) ((c >>> 16) & 0xFFF);
    }

    public static int cy(long c) {
        return (int) ((c >>> 40) & 0xFFF);
    }

    public static int cz(long c) {
        return (int) ((c >>> 28) & 0xFFF);
    }

    public static int stateId(long c) {
        return (int) (c & 0xFFFF);
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

    public int anchorX() {
        return anchorX;
    }

    public int anchorY() {
        return anchorY;
    }

    public int anchorZ() {
        return anchorZ;
    }

    public int size() {
        return cells.length;
    }

    public long cell(int index) {
        return cells[index];
    }

    public String stateOf(long cell) {
        return palette[stateId(cell)];
    }

    public int paletteSize() {
        return palette.length;
    }

    public String paletteEntry(int id) {
        return palette[id];
    }

    public String[] paletteCopy() {
        return palette.clone();
    }

    public boolean isEmpty() {
        return cells.length == 0;
    }

    public long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }

    /** Nombre de blocs non-air (les blocs d'air servent à dégager l'intérieur des bâtiments). */
    public int solidCount() {
        boolean[] air = new boolean[palette.length];
        for (int i = 0; i < palette.length; i++) {
            air[i] = B.isAir(palette[i]);
        }
        int n = 0;
        for (long c : cells) {
            if (!air[stateId(c)]) {
                n++;
            }
        }
        return n;
    }

    /** Recherche dichotomique d'une cellule ; renvoie l'état ou null. */
    public String stateAt(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
            return null;
        }
        long probe = pack(x, y, z, 0);
        int lo = 0, hi = cells.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long v = cells[mid] >>> 16;
            long p = probe >>> 16;
            if (v < p) {
                lo = mid + 1;
            } else if (v > p) {
                hi = mid - 1;
            } else {
                return palette[stateId(cells[mid])];
            }
        }
        return null;
    }

    /** Même modèle avec un autre point d'ancrage (partage les données, aucune copie). */
    public VoxelModel withAnchor(int ax, int ay, int az) {
        return new VoxelModel(sizeX, sizeY, sizeZ, ax, ay, az, palette, cells);
    }

    /** Ancre posée sur la couche la plus basse (créatures, statues : rien sous le sol). */
    public VoxelModel groundAtBottom() {
        return withAnchor(anchorX, 0, anchorZ);
    }

    /** Copie mutable, recentrée sur l'origine de conception. */
    public VoxelGrid toGrid() {
        VoxelGrid g = new VoxelGrid();
        for (long c : cells) {
            g.set(cx(c) - anchorX, cy(c) - anchorY, cz(c) - anchorZ, palette[stateId(c)]);
        }
        return g;
    }

    @Override
    public String toString() {
        return "VoxelModel[" + sizeX + "x" + sizeY + "x" + sizeZ + ", " + cells.length + " blocs]";
    }
}
