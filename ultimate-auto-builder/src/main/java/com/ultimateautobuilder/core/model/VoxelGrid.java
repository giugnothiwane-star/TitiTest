package com.ultimateautobuilder.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grille voxel creuse et mutable utilisée pendant la génération.
 * Stockage par sections 16x16x16 allouées à la demande : la mémoire reste proportionnelle
 * au volume réellement occupé, même pour des structures très étendues mais clairsemées.
 */
public final class VoxelGrid {
    public static final int COORD_LIMIT = 1 << 19;
    private static final int MAX_PALETTE = 0xFFFE;

    private final HashMap<Long, short[]> sections = new HashMap<>();
    private final ArrayList<String> palette = new ArrayList<>();
    private final HashMap<String, Integer> paletteIndex = new HashMap<>();
    private int count;

    // Cache de la dernière section accédée : les remplissages parcourent des cellules contiguës.
    private long lastKey = Long.MIN_VALUE;
    private short[] lastSection;

    @FunctionalInterface
    public interface CellVisitor {
        void visit(int x, int y, int z, String state);
    }

    private static long key(int sx, int sy, int sz) {
        return ((long) (sx & 0x1FFFFF) << 42) | ((long) (sy & 0x1FFFFF) << 21) | (sz & 0x1FFFFF);
    }

    private static int unpackSection(long key, int shift) {
        int v = (int) ((key >>> shift) & 0x1FFFFF);
        // Extension de signe sur 21 bits.
        return (v << 11) >> 11;
    }

    private static int index(int x, int y, int z) {
        return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
    }

    private static void checkCoords(int x, int y, int z) {
        if (x <= -COORD_LIMIT || x >= COORD_LIMIT || y <= -COORD_LIMIT || y >= COORD_LIMIT
                || z <= -COORD_LIMIT || z >= COORD_LIMIT) {
            throw new IllegalArgumentException("Coordonnées hors limites: " + x + "," + y + "," + z);
        }
    }

    private short[] section(int x, int y, int z, boolean create) {
        long k = key(x >> 4, y >> 4, z >> 4);
        if (k == lastKey && lastSection != null) {
            return lastSection;
        }
        short[] s = sections.get(k);
        if (s == null && create) {
            s = new short[4096];
            sections.put(k, s);
        }
        if (s != null) {
            lastKey = k;
            lastSection = s;
        }
        return s;
    }

    public int paletteId(String state) {
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("État de bloc vide");
        }
        Integer id = paletteIndex.get(state);
        if (id != null) {
            return id;
        }
        if (palette.size() >= MAX_PALETTE) {
            throw new IllegalStateException("Palette trop grande");
        }
        int newId = palette.size();
        palette.add(state);
        paletteIndex.put(state, newId);
        return newId;
    }

    public String paletteEntry(int id) {
        return palette.get(id);
    }

    public void set(int x, int y, int z, String state) {
        if (state == null) {
            remove(x, y, z);
            return;
        }
        setId(x, y, z, paletteId(state));
    }

    public void setId(int x, int y, int z, int id) {
        checkCoords(x, y, z);
        short[] s = section(x, y, z, true);
        int i = index(x, y, z);
        if (s[i] == 0) {
            count++;
        }
        s[i] = (short) (id + 1);
    }

    /** Pose le bloc uniquement si la cellule est vide. */
    public void setIfEmpty(int x, int y, int z, String state) {
        if (!isSet(x, y, z)) {
            set(x, y, z, state);
        }
    }

    public void remove(int x, int y, int z) {
        short[] s = section(x, y, z, false);
        if (s == null) {
            return;
        }
        int i = index(x, y, z);
        if (s[i] != 0) {
            s[i] = 0;
            count--;
        }
    }

    /** Identifiant de palette, ou -1 si la cellule est vide. */
    public int getId(int x, int y, int z) {
        short[] s = section(x, y, z, false);
        if (s == null) {
            return -1;
        }
        return (s[index(x, y, z)] & 0xFFFF) - 1;
    }

    public String get(int x, int y, int z) {
        int id = getId(x, y, z);
        return id < 0 ? null : palette.get(id);
    }

    public boolean isSet(int x, int y, int z) {
        return getId(x, y, z) >= 0;
    }

    /** Vrai si la cellule contient un bloc solide (ni vide, ni air explicite). */
    public boolean isSolid(int x, int y, int z) {
        int id = getId(x, y, z);
        return id >= 0 && !B.isAir(palette.get(id));
    }

    public int count() {
        return count;
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, String state) {
        int ax = Math.min(x0, x1), bx = Math.max(x0, x1);
        int ay = Math.min(y0, y1), by = Math.max(y0, y1);
        int az = Math.min(z0, z1), bz = Math.max(z0, z1);
        if (state == null) {
            for (int y = ay; y <= by; y++) for (int z = az; z <= bz; z++) for (int x = ax; x <= bx; x++) remove(x, y, z);
            return;
        }
        int id = paletteId(state);
        for (int y = ay; y <= by; y++) {
            for (int z = az; z <= bz; z++) {
                for (int x = ax; x <= bx; x++) {
                    setId(x, y, z, id);
                }
            }
        }
    }

    public void fillIfEmpty(int x0, int y0, int z0, int x1, int y1, int z1, String state) {
        int ax = Math.min(x0, x1), bx = Math.max(x0, x1);
        int ay = Math.min(y0, y1), by = Math.max(y0, y1);
        int az = Math.min(z0, z1), bz = Math.max(z0, z1);
        int id = paletteId(state);
        for (int y = ay; y <= by; y++) {
            for (int z = az; z <= bz; z++) {
                for (int x = ax; x <= bx; x++) {
                    if (getId(x, y, z) < 0) {
                        setId(x, y, z, id);
                    }
                }
            }
        }
    }

    /** Murs verticaux d'un rectangle (sans sol ni plafond). */
    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, String state) {
        fill(x0, y0, z0, x1, y1, z0, state);
        fill(x0, y0, z1, x1, y1, z1, state);
        fill(x0, y0, z0, x0, y1, z1, state);
        fill(x1, y0, z0, x1, y1, z1, state);
    }

    /** Parcours de toutes les cellules occupées (ordre non garanti). */
    public void forEach(CellVisitor visitor) {
        List<Map.Entry<Long, short[]>> entries = new ArrayList<>(sections.entrySet());
        for (Map.Entry<Long, short[]> e : entries) {
            long k = e.getKey();
            int bx = unpackSection(k, 42) << 4;
            int by = unpackSection(k, 21) << 4;
            int bz = unpackSection(k, 0) << 4;
            short[] s = e.getValue();
            for (int i = 0; i < 4096; i++) {
                int v = s[i] & 0xFFFF;
                if (v != 0) {
                    visitor.visit(bx + (i & 15), by + (i >> 8), bz + ((i >> 4) & 15), palette.get(v - 1));
                }
            }
        }
    }

    /**
     * Supprime les cellules solides entièrement entourées de solides sur leurs 6 faces.
     * Réduit fortement le nombre de blocs des statues géantes sans changer leur apparence.
     */
    public int hollowInterior() {
        List<int[]> interior = new ArrayList<>();
        forEach((x, y, z, state) -> {
            if (!B.isAir(state)
                    && isSolid(x + 1, y, z) && isSolid(x - 1, y, z)
                    && isSolid(x, y + 1, z) && isSolid(x, y - 1, z)
                    && isSolid(x, y, z + 1) && isSolid(x, y, z - 1)) {
                interior.add(new int[]{x, y, z});
            }
        });
        for (int[] p : interior) {
            remove(p[0], p[1], p[2]);
        }
        return interior.size();
    }

    /**
     * Copie une autre grille avec un décalage et une rotation horaire de {@code quarterTurns} × 90°
     * autour de l'axe Y (convention Minecraft : (x, z) → (-z, x)). Les états orientés sont pivotés.
     */
    public void paste(VoxelGrid other, int dx, int dy, int dz, int quarterTurns, boolean onlyIfEmpty) {
        int turns = Math.floorMod(quarterTurns, 4);
        HashMap<String, String> rotated = new HashMap<>();
        other.forEach((x, y, z, state) -> {
            int rx = x, rz = z;
            for (int t = 0; t < turns; t++) {
                int nx = -rz;
                rz = rx;
                rx = nx;
            }
            String st = turns == 0 ? state : rotated.computeIfAbsent(state, s -> StateRotation.rotate(s, turns));
            if (onlyIfEmpty) {
                setIfEmpty(rx + dx, y + dy, rz + dz, st);
            } else {
                set(rx + dx, y + dy, rz + dz, st);
            }
        });
    }

    public void paste(VoxelModel model, int dx, int dy, int dz) {
        for (int i = 0; i < model.size(); i++) {
            long c = model.cell(i);
            set(VoxelModel.cx(c) - model.anchorX() + dx, VoxelModel.cy(c) - model.anchorY() + dy,
                    VoxelModel.cz(c) - model.anchorZ() + dz, model.stateOf(c));
        }
    }

    /**
     * Fige la grille en modèle immuable normalisé. L'origine de conception (0,0,0)
     * devient le point d'ancrage du modèle.
     */
    public VoxelModel freeze() {
        if (count == 0) {
            return VoxelModel.EMPTY;
        }
        int[] bounds = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int[] used = new int[palette.size()];
        forEach((x, y, z, state) -> {
            bounds[0] = Math.min(bounds[0], x);
            bounds[1] = Math.min(bounds[1], y);
            bounds[2] = Math.min(bounds[2], z);
            bounds[3] = Math.max(bounds[3], x);
            bounds[4] = Math.max(bounds[4], y);
            bounds[5] = Math.max(bounds[5], z);
        });
        int sx = bounds[3] - bounds[0] + 1;
        int sy = bounds[4] - bounds[1] + 1;
        int sz = bounds[5] - bounds[2] + 1;
        if (sx > VoxelModel.MAX_AXIS || sy > VoxelModel.MAX_AXIS || sz > VoxelModel.MAX_AXIS) {
            throw new IllegalStateException("Structure trop grande: " + sx + "x" + sy + "x" + sz);
        }
        // Compactage de la palette : seuls les états réellement utilisés sont conservés.
        for (short[] s : sections.values()) {
            for (short v : s) {
                int id = (v & 0xFFFF) - 1;
                if (id >= 0) {
                    used[id]++;
                }
            }
        }
        int[] remap = new int[palette.size()];
        List<String> compact = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            if (used[i] > 0) {
                remap[i] = compact.size();
                compact.add(palette.get(i));
            } else {
                remap[i] = -1;
            }
        }
        long[] cells = new long[count];
        int[] n = {0};
        int minX = bounds[0], minY = bounds[1], minZ = bounds[2];
        forEach((x, y, z, state) -> {
            int id = remap[paletteIndex.get(state)];
            cells[n[0]++] = VoxelModel.pack(x - minX, y - minY, z - minZ, id);
        });
        java.util.Arrays.sort(cells);
        return new VoxelModel(sx, sy, sz, -minX, -minY, -minZ, compact.toArray(new String[0]), cells);
    }
}
