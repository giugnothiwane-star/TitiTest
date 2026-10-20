package com.ultimateautobuilder.core.adventure;

import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelModel;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Contrôle d'accessibilité d'une structure : parcours en largeur des positions où un joueur peut
 * se tenir (deux cellules libres au-dessus d'un appui), depuis l'entrée extérieure. Hors du
 * modèle, le terrain est supposé plat : plein en y ≤ 0, vide au-dessus. Les portes, trappes,
 * portillons et portes de coffre-fort comptent comme franchissables (ouvrables), de même que
 * les bibliothèques ciselées des passages secrets (à casser).
 */
public final class Walkability {
    private final VoxelModel m;
    private final Set<Long> reachable = new HashSet<>();

    private Walkability(Blueprint bp) {
        this.m = bp.model();
    }

    /** Positions (pieds) atteignables depuis l'entrée du plan. */
    public static Walkability of(Blueprint bp) {
        Walkability w = new Walkability(bp);
        int[] e = bp.entrance();
        w.explore(e[0], e[1], e[2]);
        return w;
    }

    private String at(int x, int y, int z) {
        String s = m.stateAt(x + m.anchorX(), y + m.anchorY(), z + m.anchorZ());
        if (s == null) {
            return y <= 0 ? "minecraft:dirt" : B.AIR;
        }
        return s;
    }

    static boolean passable(String s) {
        if (B.isAir(s)) {
            return true;
        }
        String id = B.idOf(s);
        return id.endsWith("_door") || id.endsWith("_trapdoor") || id.endsWith("_fence_gate") || id.endsWith("_carpet")
                || id.equals("minecraft:ladder") || id.contains("torch") || id.contains("lantern") || id.equals("minecraft:cobweb")
                || id.equals("minecraft:vine") || id.equals("minecraft:fern") || id.equals("minecraft:poppy")
                || id.equals("ultimateautobuilder:vault_door") || id.equals("minecraft:chiseled_bookshelf")
                || id.equals("minecraft:short_grass") || id.equals("minecraft:end_rod");
    }

    private boolean climbable(int x, int y, int z) {
        return B.idOf(at(x, y, z)).equals("minecraft:ladder");
    }

    private boolean support(int x, int y, int z) {
        String s = at(x, y, z);
        String id = B.idOf(s);
        return !B.isAir(s) && (!passable(s) || id.endsWith("_trapdoor") || id.equals("minecraft:ladder"));
    }

    boolean standable(int x, int y, int z) {
        return passable(at(x, y, z)) && passable(at(x, y + 1, z)) && (support(x, y - 1, z) || climbable(x, y, z));
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0xFFFFF) << 40) | ((long) (y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }

    private void explore(int sx, int sy, int sz) {
        int minX = -m.anchorX() - 6, maxX = m.sizeX() - m.anchorX() + 6;
        int minZ = -m.anchorZ() - 6, maxZ = m.sizeZ() - m.anchorZ() + 6;
        int minY = -m.anchorY() - 1, maxY = m.sizeY() - m.anchorY() + 2;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        if (!standable(sx, sy, sz)) {
            return;
        }
        q.add(new int[]{sx, sy, sz});
        reachable.add(key(sx, sy, sz));
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int x = p[0], y = p[1], z = p[2];
            boolean onLadder = climbable(x, y, z);
            for (int[] d : dirs) {
                int nx = x + d[0], nz = z + d[1];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) {
                    continue;
                }
                // Même niveau, marche d'un bloc (saut) ou chute jusqu'à trois blocs.
                if (standable(nx, y, nz)) {
                    visit(q, nx, y, nz);
                } else if (passable(at(x, y + 2, z)) && standable(nx, y + 1, nz)) {
                    visit(q, nx, y + 1, nz);
                } else if (passable(at(nx, y, nz)) && passable(at(nx, y + 1, nz))) {
                    for (int dy = 1; dy <= 3; dy++) {
                        if (y - dy < minY) {
                            break;
                        }
                        if (standable(nx, y - dy, nz)) {
                            visit(q, nx, y - dy, nz);
                            break;
                        }
                        if (!passable(at(nx, y - dy, nz))) {
                            break;
                        }
                    }
                }
            }
            // Échelles et trappes : montée et descente verticales.
            if (onLadder || climbable(x, y + 1, z)) {
                if (y + 1 <= maxY && passable(at(x, y + 2, z)) && (climbable(x, y + 1, z) || standable(x, y + 1, z))) {
                    visit(q, x, y + 1, z);
                }
            }
            String below = at(x, y - 1, z);
            if ((climbable(x, y - 1, z) || B.idOf(below).endsWith("_trapdoor")) && y - 1 >= minY) {
                if (climbable(x, y - 2, z) || standable(x, y - 1, z) || climbable(x, y - 1, z)) {
                    visit(q, x, y - 1, z);
                }
            }
        }
    }

    private void visit(ArrayDeque<int[]> q, int x, int y, int z) {
        if (reachable.add(key(x, y, z))) {
            q.add(new int[]{x, y, z});
        }
    }

    public boolean reachable(int x, int y, int z) {
        return reachable.contains(key(x, y, z));
    }

    public int size() {
        return reachable.size();
    }

    /**
     * Un bloc (coffre, spawner) est utilisable si une position atteignable se trouve à portée de
     * main : voisine horizontale ou diagonale, pieds au même niveau ou un bloc plus bas/haut.
     */
    public boolean usable(int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    if ((dx != 0 || dz != 0 || dy == 1) && reachable(x + dx, y + dy, z + dz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
