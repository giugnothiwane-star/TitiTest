package com.ultimateautobuilder.core.geom;

/**
 * Volume implicite exprimé en unités de conception (origine au sol, centre ; façade vers +Z).
 * Le voxeliseur teste le centre de chaque cellule après mise à l'échelle.
 */
public interface Shape {
    boolean contains(double x, double y, double z);

    Aabb bounds();

    /**
     * Version épaissie dont chaque rayon vaut au moins {@code minRadius}.
     * Garantit qu'un détail fin (queue, oreille, épée) reste continu aux petites échelles.
     */
    default Shape thicken(double minRadius) {
        return this;
    }

    default Shape rotated(double yawDeg, double pitchDeg, double rollDeg, Vec3 pivot) {
        return new Shapes.Rotated(this, yawDeg, pitchDeg, rollDeg, pivot);
    }

    default Shape translated(double dx, double dy, double dz) {
        return new Shapes.Translated(this, dx, dy, dz);
    }

    default Shape mirrorX() {
        return new Shapes.MirrorX(this);
    }

    default Shape and(Shape other) {
        return new Shapes.Intersection(this, other);
    }

    default Shape minus(Shape other) {
        return new Shapes.Difference(this, other);
    }
}
