package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;

/**
 * Primitives architecturales sur grille voxel : toitures, tours, dômes, créneaux, ouvertures.
 * Convention : la façade principale regarde vers +Z ; y = 0 est le niveau du sol.
 */
public final class Arch {
    private Arch() {
    }

    /** Rectangle au sol (bornes incluses). */
    public record Rect(int x0, int z0, int x1, int z1) {
        public static Rect centered(int width, int depth) {
            int x0 = -(width / 2);
            int z0 = -(depth / 2);
            return new Rect(x0, z0, x0 + width - 1, z0 + depth - 1);
        }

        public int width() {
            return x1 - x0 + 1;
        }

        public int depth() {
            return z1 - z0 + 1;
        }

        public int cx() {
            return (x0 + x1) / 2;
        }

        public int cz() {
            return (z0 + z1) / 2;
        }

        public Rect inset(int n) {
            return new Rect(x0 + n, z0 + n, x1 - n, z1 - n);
        }

        public Rect grow(int n) {
            return inset(-n);
        }

        public Rect translate(int dx, int dz) {
            return new Rect(x0 + dx, z0 + dz, x1 + dx, z1 + dz);
        }

        public boolean contains(int x, int z) {
            return x >= x0 && x <= x1 && z >= z0 && z <= z1;
        }

        public boolean onEdge(int x, int z) {
            return contains(x, z) && (x == x0 || x == x1 || z == z0 || z == z1);
        }
    }

    public enum RoofType { GABLE, GABLE_Z, STEEP, HIP, PYRAMID, FLAT, SHED, MANSARD, PAGODA, DOME, GREEN, NONE }

    // ------------------------------------------------------------------ volumes de base

    public static void floor(VoxelGrid g, Rect r, int y, String mat) {
        g.fill(r.x0(), y, r.z0(), r.x1(), y, r.z1(), mat);
    }

    public static void air(VoxelGrid g, Rect r, int y0, int y1) {
        if (y1 >= y0) {
            g.fill(r.x0(), y0, r.z0(), r.x1(), y1, r.z1(), B.AIR);
        }
    }

    public static void walls(VoxelGrid g, Rect r, int y0, int y1, String mat) {
        g.walls(r.x0(), y0, r.z0(), r.x1(), y1, r.z1(), mat);
    }

    /** Boîte creuse : sol, murs, intérieur vidé (air explicite pour dégager le terrain). */
    public static void room(VoxelGrid g, Rect r, int y0, int height, String wall, String floorMat) {
        floor(g, r, y0, floorMat);
        walls(g, r, y0 + 1, y0 + height, wall);
        air(g, r.inset(1), y0 + 1, y0 + height);
    }

    public static void pillars(VoxelGrid g, Rect r, int y0, int y1, String mat) {
        g.fill(r.x0(), y0, r.z0(), r.x0(), y1, r.z0(), mat);
        g.fill(r.x1(), y0, r.z0(), r.x1(), y1, r.z0(), mat);
        g.fill(r.x0(), y0, r.z1(), r.x0(), y1, r.z1(), mat);
        g.fill(r.x1(), y0, r.z1(), r.x1(), y1, r.z1(), mat);
    }

    /** Créneaux : merlons un bloc sur deux sur le pourtour. */
    public static void crenellate(VoxelGrid g, Rect r, int y, String mat) {
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int z = r.z0(); z <= r.z1(); z++) {
                if (r.onEdge(x, z) && ((x + z) & 1) == 0) {
                    g.set(x, y, z, mat);
                }
            }
        }
    }

    /**
     * Fenêtres régulièrement espacées sur les quatre murs d'un rectangle.
     * Les coins et la zone de porte (façade avant, autour de x = doorX) sont préservés.
     */
    public static void windows(VoxelGrid g, Rect r, int y0, int height, int spacing, String pane, Integer doorX) {
        for (int x = r.x0() + 2; x <= r.x1() - 2; x++) {
            if ((x - r.x0()) % spacing != spacing / 2) {
                continue;
            }
            g.fill(x, y0, r.z0(), x, y0 + height - 1, r.z0(), pane);
            if (doorX == null || Math.abs(x - doorX) > 1) {
                g.fill(x, y0, r.z1(), x, y0 + height - 1, r.z1(), pane);
            }
        }
        for (int z = r.z0() + 2; z <= r.z1() - 2; z++) {
            if ((z - r.z0()) % spacing != spacing / 2) {
                continue;
            }
            g.fill(r.x0(), y0, z, r.x0(), y0 + height - 1, z, pane);
            g.fill(r.x1(), y0, z, r.x1(), y0 + height - 1, z, pane);
        }
    }

    /** Porte double hauteur dans le mur avant (z = r.z1). */
    public static void door(VoxelGrid g, int x, int y, int z, String wood, String facing) {
        g.set(x, y, z, B.doorLower(wood, facing));
        g.set(x, y + 1, z, B.doorUpper(wood, facing));
    }

    // ------------------------------------------------------------------ toitures

    /**
     * Toiture complète au-dessus du rectangle {@code r} à partir de la hauteur {@code y}.
     *
     * @return la hauteur atteinte par le faîtage
     */
    public static int roof(VoxelGrid g, RoofType type, Rect r, int y, RoofMat m, int overhang, String gableWall) {
        return switch (type) {
            case GABLE -> gable(g, r, y, m, overhang, gableWall, true, 1);
            case GABLE_Z -> gable(g, r, y, m, overhang, gableWall, false, 1);
            case STEEP -> gable(g, r, y, m, overhang, gableWall, r.width() >= r.depth(), 2);
            case HIP, PYRAMID -> hip(g, r, y, m, overhang, false);
            case PAGODA -> hip(g, r, y, m, overhang + 1, true);
            case FLAT -> flat(g, r, y, m.full(), m.slabBottom());
            case GREEN -> green(g, r, y);
            case SHED -> shed(g, r, y, m, overhang, gableWall);
            case MANSARD -> mansard(g, r, y, m, gableWall);
            case DOME -> {
                int rad = Math.min(r.width(), r.depth()) / 2;
                dome(g, r.cx(), y, r.cz(), rad, Math.max(2, rad * 3 / 4), rad, m.full(), true);
                yield y + rad;
            }
            case NONE -> y;
        };
    }

    public static int gable(VoxelGrid g, Rect r, int y, RoofMat m, int ov, String gableWall, boolean ridgeAlongX, int rise) {
        // On ramène le cas « faîtage selon Z » au cas X par permutation des axes.
        int a0 = ridgeAlongX ? r.z0() : r.x0();
        int a1 = ridgeAlongX ? r.z1() : r.x1();
        int b0 = (ridgeAlongX ? r.x0() : r.z0()) - ov;
        int b1 = (ridgeAlongX ? r.x1() : r.z1()) + ov;
        String lowSide = ridgeAlongX ? B.S : B.E;   // versant côté a0 monte vers +a
        String highSide = ridgeAlongX ? B.N : B.W;  // versant côté a1 monte vers -a
        int top = y;
        for (int i = 0; ; i++) {
            int pa = a0 - ov + i;
            int pb = a1 + ov - i;
            if (pa > pb) {
                break;
            }
            for (int k = 0; k < rise; k++) {
                int yy = y + i * rise + k;
                boolean last = k == rise - 1;
                top = yy;
                for (int bb = b0; bb <= b1; bb++) {
                    if (pa == pb) {
                        put(g, ridgeAlongX, bb, yy, pa, last ? m.slabBottom() : m.full());
                    } else {
                        put(g, ridgeAlongX, bb, yy, pa, last ? m.stairs(lowSide, false) : m.full());
                        put(g, ridgeAlongX, bb, yy, pb, last ? m.stairs(highSide, false) : m.full());
                    }
                }
                // Pignons et volume des combles.
                int inner0 = Math.max(pa + 1, ridgeAlongX ? r.z0() : r.x0());
                int inner1 = Math.min(pb - 1, ridgeAlongX ? r.z1() : r.x1());
                for (int aa = inner0; aa <= inner1; aa++) {
                    int e0 = ridgeAlongX ? r.x0() : r.z0();
                    int e1 = ridgeAlongX ? r.x1() : r.z1();
                    put(g, ridgeAlongX, e0, yy, aa, gableWall);
                    put(g, ridgeAlongX, e1, yy, aa, gableWall);
                    for (int bb = e0 + 1; bb < e1; bb++) {
                        put(g, ridgeAlongX, bb, yy, aa, B.AIR);
                    }
                }
            }
            if (pa == pb || pa + 1 == pb) {
                break;
            }
        }
        return top;
    }

    private static void put(VoxelGrid g, boolean alongX, int b, int y, int a, String state) {
        if (alongX) {
            g.set(b, y, a, state);
        } else {
            g.set(a, y, b, state);
        }
    }

    public static int hip(VoxelGrid g, Rect r, int y, RoofMat m, int ov, boolean upturned) {
        Rect cur = r.grow(ov);
        int yy = y;
        if (upturned) {
            // Avant-toits relevés aux angles (style pagode).
            for (Rect c : new Rect[]{cur}) {
                g.set(c.x0(), yy + 1, c.z0(), m.slabBottom());
                g.set(c.x1(), yy + 1, c.z0(), m.slabBottom());
                g.set(c.x0(), yy + 1, c.z1(), m.slabBottom());
                g.set(c.x1(), yy + 1, c.z1(), m.slabBottom());
            }
        }
        while (true) {
            if (cur.x0() > cur.x1() || cur.z0() > cur.z1()) {
                break;
            }
            if (cur.x0() == cur.x1() || cur.z0() == cur.z1()) {
                g.fill(cur.x0(), yy, cur.z0(), cur.x1(), yy, cur.z1(), m.slabBottom());
                break;
            }
            for (int x = cur.x0(); x <= cur.x1(); x++) {
                g.set(x, yy, cur.z0(), m.stairs(B.S, false));
                g.set(x, yy, cur.z1(), m.stairs(B.N, false));
            }
            for (int z = cur.z0() + 1; z < cur.z1(); z++) {
                g.set(cur.x0(), yy, z, m.stairs(B.E, false));
                g.set(cur.x1(), yy, z, m.stairs(B.W, false));
            }
            Rect inner = cur.inset(1);
            if (inner.x0() <= inner.x1() && inner.z0() <= inner.z1()) {
                Rect clip = new Rect(Math.max(inner.x0(), r.x0() + 1), Math.max(inner.z0(), r.z0() + 1),
                        Math.min(inner.x1(), r.x1() - 1), Math.min(inner.z1(), r.z1() - 1));
                if (clip.x0() <= clip.x1() && clip.z0() <= clip.z1()) {
                    air(g, clip, yy, yy);
                }
            }
            cur = inner;
            yy++;
        }
        return yy;
    }

    public static int flat(VoxelGrid g, Rect r, int y, String mat, String parapet) {
        floor(g, r, y, mat);
        for (int x = r.x0(); x <= r.x1(); x++) {
            g.set(x, y + 1, r.z0(), parapet);
            g.set(x, y + 1, r.z1(), parapet);
        }
        for (int z = r.z0(); z <= r.z1(); z++) {
            g.set(r.x0(), y + 1, z, parapet);
            g.set(r.x1(), y + 1, z, parapet);
        }
        return y + 1;
    }

    /** Toit végétalisé : terre, herbe et petites plantes. */
    public static int green(VoxelGrid g, Rect r, int y) {
        floor(g, r, y, "minecraft:dirt");
        floor(g, r, y + 1, "minecraft:grass_block");
        for (int x = r.x0() + 1; x < r.x1(); x += 2) {
            for (int z = r.z0() + 1; z < r.z1(); z += 3) {
                g.set(x, y + 2, z, ((x + z) & 3) == 0 ? "minecraft:azure_bluet" : "minecraft:short_grass");
            }
        }
        return y + 2;
    }

    public static int shed(VoxelGrid g, Rect r, int y, RoofMat m, int ov, String wall) {
        int depth = r.depth() + 2 * ov;
        int rise = Math.max(1, depth / 3);
        int top = y;
        for (int z = r.z0() - ov; z <= r.z1() + ov; z++) {
            int i = z - (r.z0() - ov);
            int h = y + rise - (i * rise) / depth;
            top = Math.max(top, h);
            for (int x = r.x0() - ov; x <= r.x1() + ov; x++) {
                g.set(x, h, z, m.full());
            }
            if (z >= r.z0() && z <= r.z1()) {
                for (int yy = y; yy < h; yy++) {
                    g.set(r.x0(), yy, z, wall);
                    g.set(r.x1(), yy, z, wall);
                    for (int x = r.x0() + 1; x < r.x1(); x++) {
                        g.set(x, yy, z, B.AIR);
                    }
                }
                if (z == r.z0()) {
                    for (int yy = y; yy < h; yy++) {
                        g.fill(r.x0(), yy, z, r.x1(), yy, z, wall);
                    }
                }
            }
        }
        return top;
    }

    public static int mansard(VoxelGrid g, Rect r, int y, RoofMat m, String wall) {
        Rect cur = r.grow(1);
        for (int i = 0; i < 3; i++) {
            walls(g, cur, y + i, y + i, m.full());
            if (i == 1) {
                Arch.windows(g, cur, y + 1, 1, 4, "minecraft:glass_pane", null);
            }
            if (i == 2) {
                cur = cur.inset(1);
            }
        }
        air(g, r.inset(1), y, y + 2);
        return hip(g, cur, y + 3, m, 0, false);
    }

    // ------------------------------------------------------------------ volumes courbes

    public static void dome(VoxelGrid g, int cx, int y, int cz, int rx, int ry, int rz, String mat, boolean hollow) {
        for (int dy = 0; dy <= ry; dy++) {
            for (int dz = -rz; dz <= rz; dz++) {
                for (int dx = -rx; dx <= rx; dx++) {
                    double v = sq(dx / (rx + 0.5)) + sq(dy / (ry + 0.5)) + sq(dz / (rz + 0.5));
                    if (v > 1) {
                        continue;
                    }
                    double inner = sq(dx / (rx - 0.5)) + sq(dy / (ry - 0.5)) + sq(dz / (rz - 0.5));
                    if (hollow && inner < 1 && rx > 1 && ry > 1 && rz > 1) {
                        g.set(cx + dx, y + dy, cz + dz, B.AIR);
                    } else {
                        g.set(cx + dx, y + dy, cz + dz, mat);
                    }
                }
            }
        }
    }

    public static void sphere(VoxelGrid g, int cx, int cy, int cz, int r, String mat, boolean hollow) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d <= r + 0.4 && (!hollow || d >= r - 0.6)) {
                        g.set(cx + dx, cy + dy, cz + dz, mat);
                    }
                }
            }
        }
    }

    /** Tour cylindrique : mur annulaire, planchers réguliers, intérieur dégagé. */
    public static void cylinder(VoxelGrid g, int cx, int y0, int cz, int r, int height, String wall, String floorMat, int floorEvery) {
        for (int y = y0; y < y0 + height; y++) {
            boolean floorLayer = y == y0 || (floorEvery > 0 && (y - y0) % floorEvery == 0);
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r + 0.45) {
                        continue;
                    }
                    if (d >= r - 0.55) {
                        g.set(cx + dx, y, cz + dz, wall);
                    } else {
                        g.set(cx + dx, y, cz + dz, floorLayer ? floorMat : B.AIR);
                    }
                }
            }
        }
    }

    public static void disk(VoxelGrid g, int cx, int y, int cz, double r, String mat) {
        int ri = (int) Math.ceil(r);
        for (int dz = -ri; dz <= ri; dz++) {
            for (int dx = -ri; dx <= ri; dx++) {
                if (dx * dx + dz * dz <= r * r + 0.3) {
                    g.set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    public static void ring(VoxelGrid g, int cx, int y, int cz, double r, String mat) {
        int ri = (int) Math.ceil(r);
        for (int dz = -ri; dz <= ri; dz++) {
            for (int dx = -ri; dx <= ri; dx++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= r + 0.45 && d >= r - 0.6) {
                    g.set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    /** Toit conique plein (tours de château, tourelles de conte de fées). */
    public static int cone(VoxelGrid g, int cx, int y, int cz, int r, int height, String mat) {
        for (int i = 0; i <= height; i++) {
            double ri = r * (1.0 - (double) i / (height + 1));
            if (ri < 0.5) {
                g.set(cx, y + i, cz, mat);
                continue;
            }
            ring(g, cx, y + i, cz, ri, mat);
            disk(g, cx, y + i, cz, Math.max(0, ri - 1), B.AIR);
        }
        return y + height;
    }

    /** Créneaux circulaires. */
    public static void crenelRing(VoxelGrid g, int cx, int y, int cz, int r, String mat) {
        int ri = r + 1;
        for (int dz = -ri; dz <= ri; dz++) {
            for (int dx = -ri; dx <= ri; dx++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= r + 0.45 && d >= r - 0.55 && ((dx + dz) & 1) == 0) {
                    g.set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    /** Ligne de blocs de (x0,y0,z0) à (x1,y1,z1) (algorithme DDA 3D). */
    public static void line(VoxelGrid g, int x0, int y0, int z0, int x1, int y1, int z1, String mat) {
        int n = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            g.set((int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t), (int) Math.round(z0 + (z1 - z0) * t), mat);
        }
    }

    private static double sq(double v) {
        return v * v;
    }
}
