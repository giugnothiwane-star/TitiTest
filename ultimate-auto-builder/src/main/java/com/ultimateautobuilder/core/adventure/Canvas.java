package com.ultimateautobuilder.core.adventure;

import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.util.Rng;

import java.util.ArrayList;
import java.util.List;

/**
 * Surface de dessin des structures d'aventure : grille voxel, marqueurs et primitives
 * architecturales (pièces vidées, escaliers praticables, portes, toits).
 */
final class Canvas {
    static final String CLEAR = "minecraft:cave_air";

    final VoxelGrid g = new VoxelGrid();
    final List<Blueprint.Marker> markers = new ArrayList<>();
    final Rng rng;

    Canvas(long seed) {
        this.rng = new Rng(seed);
    }

    void set(int x, int y, int z, String s) {
        g.set(x, y, z, s);
    }

    String get(int x, int y, int z) {
        return g.get(x, y, z);
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, String s) {
        g.fill(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), s);
    }

    /** Volume à vider (air garanti après la pose, y compris sous terre). */
    void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(x0, y0, z0, x1, y1, z1, CLEAR);
    }

    /** Murs d'une boîte (sans sol ni plafond). */
    void walls(int x0, int y0, int z0, int x1, int y1, int z1, String s) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                set(x, y, z0, s);
                set(x, y, z1, s);
            }
            for (int z = z0; z <= z1; z++) {
                set(x0, y, z, s);
                set(x1, y, z, s);
            }
        }
    }

    /** Coffre à remplir par une table de butin du mod. */
    void lootChest(int x, int y, int z, String facing, String table) {
        set(x, y, z, "minecraft:chest[facing=" + facing + ",type=single,waterlogged=false]");
        markers.add(new Blueprint.Marker(x, y, z, "loot", table));
    }

    void lockedChest(int x, int y, int z, String facing, KeyTier tier) {
        set(x, y, z, "ultimateautobuilder:locked_chest[facing=" + facing + ",tier=" + tier.key + "]");
    }

    /** Porte de coffre-fort de deux blocs de haut. */
    void vaultDoor(int x, int y, int z, KeyTier tier) {
        set(x, y, z, "ultimateautobuilder:vault_door[tier=" + tier.key + "]");
        set(x, y + 1, z, "ultimateautobuilder:vault_door[tier=" + tier.key + "]");
    }

    void spawner(int x, int y, int z, SpawnCategory c, SpawnFamily f, boolean warden) {
        set(x, y, z, "ultimateautobuilder:adventure_spawner[category=" + c.key + ",depleted=false,family=" + f.key + ",warden=" + warden + "]");
    }

    /** Porte en bois de deux blocs ; {@code facing} = côté vers lequel regarde la façade. */
    void door(int x, int y, int z, String wood, String facing) {
        String opposite = B.opposite(facing);
        set(x, y, z, B.doorLower(wood, opposite));
        set(x, y + 1, z, B.doorUpper(wood, opposite));
    }

    /**
     * Escalier droit praticable montant vers +X depuis (x0, y0, z) sur {@code n} marches ;
     * ouvre automatiquement le plancher {@code floorY} au-dessus des marches pour le passage de la tête.
     */
    void stairsEast(int x0, int y0, int z, int n, String material, int floorY) {
        for (int i = 0; i < n; i++) {
            int y = y0 + i;
            set(x0 + i, y, z, B.stairs(material, B.E, false));
            // Marge d'un bloc : la tête du joueur passe sous le plancher pendant la montée.
            for (int h = y + 1; h <= y + 3; h++) {
                if (h == floorY && i < n - 1) {
                    set(x0 + i, h, z, CLEAR);
                }
            }
        }
    }

    /**
     * Escalier droit de trois blocs de large montant vers +Z, avec tunnel creusé au-dessus
     * (souterrains) : murs et plafond en {@code shell}, marches soutenues jusqu'à {@code bottom}.
     */
    void tunnelStairsSouth(int x, int y0, int z0, int n, String material, String shell, int bottom) {
        for (int i = 0; i < n; i++) {
            int y = y0 + i, z = z0 + i;
            for (int dx = -1; dx <= 1; dx++) {
                for (int yy = bottom; yy < y; yy++) {
                    set(x + dx, yy, z, shell);
                }
                set(x + dx, y, z, com.ultimateautobuilder.core.model.B.stairs(material, com.ultimateautobuilder.core.model.B.S, false));
                clear(x + dx, y + 1, z, x + dx, y + 3, z);
            }
            for (int yy = bottom; yy <= y + 4; yy++) {
                set(x - 2, yy, z, shell);
                set(x + 2, yy, z, shell);
            }
            fill(x - 1, y + 4, z, x + 1, y + 4, z, shell);
        }
    }

    /** Toit à deux pans, faîtage selon X, du rectangle [x0..x1] × [z0..z1] à partir de yBase. */
    int gableRoof(int x0, int x1, int z0, int z1, int yBase, String stairMat, String gableFill, String ridge, double holes) {
        int zf = z0 - 1, zb = z1 + 1;
        int y = yBase;
        while (zf <= zb) {
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                if (zf == zb) {
                    if (!rng.chance(holes)) {
                        set(x, y, zf, ridge);
                    }
                } else {
                    if (!rng.chance(holes)) {
                        set(x, y, zf, B.stairs(stairMat, B.S, false));
                    }
                    if (!rng.chance(holes)) {
                        set(x, y, zb, B.stairs(stairMat, B.N, false));
                    }
                }
            }
            // Pignons pleins et combles vidés.
            for (int z = zf + 1; z <= zb - 1; z++) {
                set(x0, y, z, gableFill);
                set(x1, y, z, gableFill);
                for (int x = x0 + 1; x <= x1 - 1; x++) {
                    set(x, y, z, CLEAR);
                }
            }
            zf++;
            zb--;
            y++;
        }
        return y;
    }

    /** Toit plat crénelé (forteresses). */
    void crenellations(int x0, int z0, int x1, int z1, int y, String s) {
        for (int x = x0; x <= x1; x++) {
            if ((x - x0) % 2 == 0) {
                set(x, y, z0, s);
                set(x, y, z1, s);
            }
        }
        for (int z = z0; z <= z1; z++) {
            if ((z - z0) % 2 == 0) {
                set(x0, y, z, s);
                set(x1, y, z, s);
            }
        }
    }

    /** Usure : remplace aléatoirement des blocs d'un matériau par ses variantes abîmées. */
    void weather(int x0, int y0, int z0, int x1, int y1, int z1, String from, String[] to, double p) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (from.equals(get(x, y, z)) && rng.chance(p)) {
                        set(x, y, z, to[rng.nextInt(to.length)]);
                    }
                }
            }
        }
    }

    Blueprint freeze(StructureType type, int[] entrance) {
        return new Blueprint(type, g.freeze(), List.copyOf(markers), entrance);
    }
}
