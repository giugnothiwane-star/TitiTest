package com.ultimateautobuilder.core.geom;

import com.ultimateautobuilder.core.model.VoxelGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sculpture voxel : suite ordonnée d'opérations (ajout, peinture, creusement) sur des volumes
 * implicites. La voxelisation directe à l'échelle cible évite les trous d'un agrandissement
 * de grille et conserve les proportions à toutes les tailles.
 */
public final class Sculpt {
    public enum Mode {
        /** Remplit le volume (écrase). */
        ADD,
        /** Ne remplit que les cellules vides. */
        FILL,
        /** Recolore uniquement les cellules déjà occupées (yeux, motifs, joues). */
        PAINT,
        /** Retire les cellules. */
        CARVE
    }

    public record Op(Shape shape, Mat mat, Mode mode) {
    }

    private final double designHeight;
    private final List<Op> ops;

    private Sculpt(double designHeight, List<Op> ops) {
        this.designHeight = designHeight;
        this.ops = Collections.unmodifiableList(new ArrayList<>(ops));
    }

    public static Builder builder(double designHeight) {
        return new Builder(designHeight);
    }

    public double designHeight() {
        return designHeight;
    }

    public List<Op> ops() {
        return ops;
    }

    /**
     * Voxelise pour une dimension cible en blocs.
     *
     * @param target   taille souhaitée (blocs)
     * @param hollow   vider l'intérieur (réduction du nombre de blocs)
     * @param byHeight la taille pilote la hauteur ; sinon la plus grande dimension horizontale
     */
    public VoxelGrid voxelize(int target, boolean hollow, boolean byHeight) {
        double scale = Math.max(0.05, target / designHeight);
        VoxelGrid grid = new VoxelGrid();
        voxelizeInto(grid, scale, 0, 0, 0);
        // Les boîtes englobantes des formes pivotées sont conservatrices : on mesure la taille réelle
        // obtenue puis on corrige l'échelle une fois pour atteindre précisément la cible.
        int measured = measure(grid, byHeight);
        if (measured > 0 && Math.abs(measured - target) > Math.max(1, target / 40)) {
            scale *= (double) target / measured;
            grid = new VoxelGrid();
            voxelizeInto(grid, scale, 0, 0, 0);
        }
        if (hollow && target >= 24) {
            grid.hollowInterior();
        }
        return grid;
    }

    public VoxelGrid voxelize(int targetHeight, boolean hollow) {
        return voxelize(targetHeight, hollow, true);
    }

    private static int measure(VoxelGrid grid, boolean byHeight) {
        int[] b = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        grid.forEach((x, y, z, st) -> {
            b[0] = Math.min(b[0], x);
            b[1] = Math.max(b[1], x);
            b[2] = Math.min(b[2], y);
            b[3] = Math.max(b[3], y);
            b[4] = Math.min(b[4], z);
            b[5] = Math.max(b[5], z);
        });
        if (b[0] > b[1]) {
            return 0;
        }
        return byHeight ? b[3] - Math.min(0, b[2]) + 1 : Math.max(b[1] - b[0] + 1, b[5] - b[4] + 1);
    }

    /** Voxelise dans une grille existante avec un décalage en blocs (composition de scènes). */
    public void voxelizeInto(VoxelGrid grid, double scale, int ox, int oy, int oz) {
        // Demi-diagonale d'une cellule : un trait fin traverse toujours au moins un centre.
        double minRadius = 0.72 / scale;
        for (Op op : ops) {
            Shape shape = op.mode() == Mode.CARVE ? op.shape() : op.shape().thicken(minRadius);
            Aabb b = shape.bounds();
            if (b.isEmpty() || b.maxX() - b.minX() > 1e5) {
                continue;
            }
            int x0 = (int) Math.floor(b.minX() * scale) - 1, x1 = (int) Math.ceil(b.maxX() * scale) + 1;
            int y0 = (int) Math.floor(b.minY() * scale) - 1, y1 = (int) Math.ceil(b.maxY() * scale) + 1;
            int z0 = (int) Math.floor(b.minZ() * scale) - 1, z1 = (int) Math.ceil(b.maxZ() * scale) + 1;
            for (int y = y0; y <= y1; y++) {
                double dy = (y + 0.5) / scale;
                for (int z = z0; z <= z1; z++) {
                    double dz = (z + 0.5) / scale;
                    for (int x = x0; x <= x1; x++) {
                        double dx = (x + 0.5) / scale;
                        if (!shape.contains(dx, dy, dz)) {
                            continue;
                        }
                        int gx = x + ox, gy = y + oy, gz = z + oz;
                        if (op.mode() == Mode.CARVE) {
                            grid.remove(gx, gy, gz);
                            continue;
                        }
                        // Un matériau nul signifie « laisser la cellule telle quelle » (trous de feuillage).
                        String st = op.mat().at(dx, dy, dz);
                        if (st == null) {
                            continue;
                        }
                        switch (op.mode()) {
                            case ADD -> grid.set(gx, gy, gz, st);
                            case FILL -> {
                                if (!grid.isSet(gx, gy, gz)) {
                                    grid.set(gx, gy, gz, st);
                                }
                            }
                            case PAINT -> {
                                if (grid.isSet(gx, gy, gz)) {
                                    grid.set(gx, gy, gz, st);
                                }
                            }
                            default -> {
                            }
                        }
                    }
                }
            }
        }
    }

    /** Mise à l'échelle uniforme d'une forme (composition de sculptures). */
    private static final class Scaled implements Shape {
        private final Shape inner;
        private final double k;

        Scaled(Shape inner, double k) {
            this.inner = inner;
            this.k = k;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return inner.contains(x / k, y / k, z / k);
        }

        @Override
        public Aabb bounds() {
            Aabb b = inner.bounds();
            return new Aabb(b.minX() * k, b.minY() * k, b.minZ() * k, b.maxX() * k, b.maxY() * k, b.maxZ() * k);
        }

        @Override
        public Shape thicken(double minRadius) {
            return new Scaled(inner.thicken(minRadius / k), k);
        }
    }

    public static final class Builder {
        private final double designHeight;
        private final List<Op> ops = new ArrayList<>();

        private Builder(double designHeight) {
            if (designHeight <= 0) {
                throw new IllegalArgumentException("designHeight");
            }
            this.designHeight = designHeight;
        }

        public Builder add(Shape s, Mat m) {
            ops.add(new Op(s, m, Mode.ADD));
            return this;
        }

        public Builder add(Shape s, String state) {
            return add(s, Mat.of(state));
        }

        /** Ajoute la forme et son symétrique par rapport au plan X = 0. */
        public Builder sym(Shape s, Mat m) {
            add(s, m);
            return add(s.mirrorX(), m);
        }

        public Builder sym(Shape s, String state) {
            return sym(s, Mat.of(state));
        }

        public Builder fill(Shape s, String state) {
            ops.add(new Op(s, Mat.of(state), Mode.FILL));
            return this;
        }

        public Builder fill(Shape s, Mat m) {
            ops.add(new Op(s, m, Mode.FILL));
            return this;
        }

        public Builder paint(Shape s, Mat m) {
            ops.add(new Op(s, m, Mode.PAINT));
            return this;
        }

        public Builder paint(Shape s, String state) {
            return paint(s, Mat.of(state));
        }

        public Builder paintSym(Shape s, String state) {
            paint(s, state);
            return paint(s.mirrorX(), state);
        }

        public Builder paintSym(Shape s, Mat m) {
            paint(s, m);
            return paint(s.mirrorX(), m);
        }

        public Builder fillSym(Shape s, String state) {
            fill(s, state);
            return fill(s.mirrorX(), state);
        }

        public Builder carve(Shape s) {
            ops.add(new Op(s, Mat.of("minecraft:air"), Mode.CARVE));
            return this;
        }

        public Builder carveSym(Shape s) {
            carve(s);
            return carve(s.mirrorX());
        }

        public Builder addAll(Sculpt other) {
            ops.addAll(other.ops);
            return this;
        }

        /** Ajoute une autre sculpture déplacée, éventuellement pivotée (lacet en degrés) et mise à l'échelle. */
        public Builder addAll(Sculpt other, double dx, double dy, double dz, double yawDeg, double scale) {
            for (Op op : other.ops) {
                Shape s = op.shape();
                Mat m = op.mat();
                if (scale != 1.0) {
                    s = new Scaled(s, scale);
                    Mat inner = m;
                    m = (x, y, z) -> inner.at(x / scale, y / scale, z / scale);
                }
                if (yawDeg != 0) {
                    s = s.rotated(yawDeg, 0, 0, Vec3.ZERO);
                }
                Shape moved = s.translated(dx, dy, dz);
                Mat mm = m;
                double ryaw = Math.toRadians(-yawDeg);
                double c = Math.cos(ryaw), sn = Math.sin(ryaw);
                ops.add(new Op(moved, (x, y, z) -> {
                    double lx = x - dx, ly = y - dy, lz = z - dz;
                    return mm.at(lx * c + lz * sn, ly, -lx * sn + lz * c);
                }, op.mode()));
            }
            return this;
        }

        public Sculpt build() {
            return new Sculpt(designHeight, ops);
        }
    }
}
