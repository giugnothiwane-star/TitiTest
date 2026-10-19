package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.geom.Shape;

import static com.ultimateautobuilder.core.generator.Humanoid.HEAD;
import static com.ultimateautobuilder.core.generator.Humanoid.cap;
import static com.ultimateautobuilder.core.generator.Humanoid.spike;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;

import com.ultimateautobuilder.core.geom.Vec3;

/** Coiffures réutilisables du gabarit humanoïde. */
public final class HairStyles {
    private HairStyles() {
    }

    private static Shape bang(double x, double dx, double len) {
        return frustum(new Vec3(x, 30.0, 2.6), new Vec3(x + dx, 30.0 - len, 4.25), 1.0, 0.15);
    }

    public static final Humanoid.Hair SHORT = (b, s) -> {
        b.add(cap(0.8, 0.8), s.hair);
        b.add(ellipsoid(0, 29.4, 3.0, 3.7, 1.0, 1.2), s.hair);
    };

    public static final Humanoid.Hair MESSY = (b, s) -> {
        b.add(cap(0.9, 0.8), s.hair);
        b.sym(spike(0.7, 0.6, 0.3, 0.6, 1.3), s.hair);
        b.add(spike(0, 1, 0, 0.8, 1.4), s.hair);
        b.sym(spike(0.9, 0.2, -0.5, 0.6, 1.3), s.hair);
        b.sym(spike(0.4, 0.2, -1, 0.8, 1.3), s.hair);
        b.add(bang(-1.5, -0.3, 2.0), s.hair);
        b.add(bang(0.3, 0.1, 2.3), s.hair);
        b.add(bang(1.8, 0.4, 1.8), s.hair);
    };

    public static final Humanoid.Hair SAIYAN = (b, s) -> {
        b.add(cap(0.9, 0.4), s.hair);
        b.add(spike(0, 1, -0.1, 3.0, 1.7), s.hair);
        b.sym(spike(0.5, 1, -0.2, 3.4, 1.7), s.hair);
        b.sym(spike(1, 0.5, -0.3, 3.6, 1.7), s.hair);
        b.sym(spike(1, 0.05, -0.5, 3.0, 1.5), s.hair);
        b.add(spike(0, 0.55, -1, 4.0, 1.9), s.hair);
        b.sym(spike(0.6, 0.3, -1, 3.6, 1.7), s.hair);
        b.sym(spike(0.4, -0.25, -1, 3.0, 1.5), s.hair);
        b.add(bang(-1.7, -0.6, 2.4), s.hair);
        b.add(bang(0.2, 0.2, 2.9), s.hair);
        b.add(bang(1.8, 0.7, 2.2), s.hair);
    };

    public static final Humanoid.Hair SUPER_SAIYAN = (b, s) -> {
        b.add(cap(0.9, 0.2), s.hair);
        b.add(spike(0, 1, -0.2, 5.6, 1.9), s.hair);
        b.sym(spike(0.35, 1, -0.3, 5.2, 1.8), s.hair);
        b.sym(spike(0.8, 0.85, -0.35, 4.6, 1.8), s.hair);
        b.sym(spike(1, 0.45, -0.5, 4.0, 1.6), s.hair);
        b.add(spike(0, 0.45, -1, 4.2, 1.8), s.hair);
        b.sym(spike(0.5, 0.6, -1, 4.0, 1.7), s.hair);
        b.add(bang(0.3, 0.3, 3.0), s.hair);
    };

    public static final Humanoid.Hair VEGETA = (b, s) -> {
        b.add(cap(0.7, -0.6), s.hair);
        b.add(spike(0, 1, -0.15, 6.8, 2.3), s.hair);
        b.sym(spike(0.25, 1, -0.25, 6.2, 2.1), s.hair);
        b.sym(spike(0.5, 0.92, -0.3, 5.4, 1.9), s.hair);
        b.sym(spike(0.75, 0.7, -0.4, 4.4, 1.7), s.hair);
        b.add(spike(0, 0.7, -0.8, 4.6, 1.9), s.hair);
        // Pointe frontale (« M ») caractéristique.
        b.add(frustum(new Vec3(0, 31.0, 2.4), new Vec3(0, 29.2, 3.9), 1.2, 0.2), s.hair);
    };

    public static final Humanoid.Hair NARUTO = (b, s) -> {
        b.add(cap(1.0, 0.6), s.hair);
        for (int i = 0; i < 9; i++) {
            double a = Math.toRadians(i * 40 + 20);
            b.add(spike(Math.cos(a), 0.55, Math.sin(a) - 0.25, 2.4, 1.45), s.hair);
        }
        b.add(spike(0, 1, 0, 2.6, 1.6), s.hair);
        b.sym(spike(0.4, 1, 0.3, 2.4, 1.5), s.hair);
        b.add(bang(-1.8, -0.7, 2.3), s.hair);
        b.add(bang(0, 0, 2.6), s.hair);
        b.add(bang(1.8, 0.7, 2.3), s.hair);
    };

    public static final Humanoid.Hair SASUKE = (b, s) -> {
        b.add(cap(0.9, 0.4), s.hair);
        b.add(spike(0, 0.5, -1, 3.8, 2.0), s.hair);
        b.sym(spike(0.5, 0.45, -1, 3.4, 1.8), s.hair);
        b.sym(spike(0.3, 0.9, -1, 3.0, 1.6), s.hair);
        b.sym(limb(3.2, 29.0, 2.6, 3.4, 24.5, 3.0, 0.9, 0.6), s.hair);
        b.add(bang(0.6, 0.2, 2.6), s.hair);
    };

    public static final Humanoid.Hair TILTED = (b, s) -> {
        b.add(cap(0.9, 0.5), s.hair);
        b.add(spike(-0.55, 1, -0.2, 5.0, 2.3), s.hair);
        b.add(spike(-0.2, 1, -0.3, 4.4, 2.1), s.hair);
        b.add(spike(-0.85, 0.75, -0.2, 4.0, 1.9), s.hair);
        b.add(spike(0.3, 0.95, -0.3, 3.4, 1.9), s.hair);
        b.add(spike(0.75, 0.6, -0.4, 2.6, 1.7), s.hair);
        b.add(bang(-1.2, -0.4, 2.4), s.hair);
    };

    public static final Humanoid.Hair UPSWEPT = (b, s) -> {
        b.add(cap(0.9, 0.3), s.hair);
        b.add(spike(0, 1, -0.1, 3.6, 1.9), s.hair);
        b.sym(spike(0.4, 1, -0.1, 3.4, 1.8), s.hair);
        b.sym(spike(0.8, 0.8, -0.2, 2.8, 1.7), s.hair);
        b.sym(spike(0.3, 0.8, -0.7, 3.0, 1.7), s.hair);
        b.add(spike(0, 0.9, 0.4, 2.6, 1.7), s.hair);
    };

    public static final Humanoid.Hair BOWL = (b, s) -> {
        b.add(cap(1.1, 1.6), s.hair);
        b.sym(ellipsoid(3.4, 26.4, 0.4, 1.2, 3.0, 2.6), s.hair);
        b.add(ellipsoid(0, 29.2, 3.2, 3.9, 1.3, 1.2), s.hair);
    };

    public static final Humanoid.Hair LONG = (b, s) -> {
        b.add(cap(0.9, 0.6), s.hair);
        b.add(ellipsoid(0, 22.0, -2.7, 4.7, 7.2, 2.0), s.hair);
        b.sym(limb(3.6, 28.5, 1.0, 4.2, 20.5, -0.6, 1.2, 1.0), s.hair);
        b.add(ellipsoid(0, 29.5, 3.0, 3.8, 1.0, 1.1), s.hair);
    };

    public static final Humanoid.Hair BOB = (b, s) -> {
        b.add(cap(0.9, 0.8), s.hair);
        b.add(ellipsoid(0, 25.6, -1.6, 4.9, 3.6, 3.0), s.hair);
        b.add(bang(0, 0, 3.2), s.hair);
    };

    public static final Humanoid.Hair PONYTAIL = (b, s) -> {
        b.add(cap(0.8, 0.5), s.hair);
        b.add(limb(0, 25.8, -4.2, 0, 18.5, -5.0, 1.1, 0.8), s.hair);
        b.sym(limb(3.1, 29.2, 2.7, 3.4, 24.0, 3.0, 0.85, 0.6), s.hair);
    };

    public static final Humanoid.Hair SHOULDER = (b, s) -> {
        b.add(cap(1.0, 0.6), s.hair);
        b.add(ellipsoid(0, 24.2, -2.1, 5.0, 4.4, 2.5), s.hair);
        b.sym(spike(0.9, -0.2, -0.3, 1.4, 1.4), s.hair);
        b.add(bang(-1.4, -0.4, 2.3), s.hair);
        b.add(bang(1.0, 0.4, 2.5), s.hair);
    };

    public static final Humanoid.Hair UNDERCUT = (b, s) -> {
        b.add(cap(0.6, 0.6), s.hair);
        b.sym(frustum(new Vec3(0.5, 30.6, 2.4), new Vec3(2.8, 27.3, 4.0), 1.0, 0.25), s.hair);
    };

    /** Mèche couvrant un œil (Sanji). */
    public static final Humanoid.Hair SWEPT_EYE = (b, s) -> {
        b.add(cap(0.9, 0.7), s.hair);
        b.add(ellipsoid(-1.6, 27.4, 3.9, 2.4, 2.2, 0.8), s.hair);
        b.add(ellipsoid(0, 29.4, 2.8, 3.6, 1.0, 1.4), s.hair);
    };

    public static final Humanoid.Hair BALD = (b, s) -> {
    };

    /** Utilitaire pour les personnages composés d'un autre volume de tête. */
    static Vec3 head() {
        return HEAD;
    }
}
