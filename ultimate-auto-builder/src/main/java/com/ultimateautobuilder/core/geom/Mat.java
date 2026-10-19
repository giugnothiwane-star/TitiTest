package com.ultimateautobuilder.core.geom;

import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.util.Noise;

/**
 * Matériau éventuellement variable dans l'espace (rayures, taches, dégradés).
 * Les coordonnées reçues sont en unités de conception pour que le motif suive l'échelle.
 */
@FunctionalInterface
public interface Mat {
    String at(double x, double y, double z);

    static Mat of(String state) {
        String s = B.mc(state);
        return (x, y, z) -> s;
    }

    /** Rayures horizontales (le long de Y) de période {@code period}. */
    static Mat stripesY(double period, double width, Mat base, Mat stripe) {
        return (x, y, z) -> (Math.floorMod((long) Math.floor(y / period * 1000), 1000L) < width / period * 1000)
                ? stripe.at(x, y, z) : base.at(x, y, z);
    }

    /** Rayures verticales selon X (zèbre, tigre). */
    static Mat stripesX(double period, double width, Mat base, Mat stripe) {
        return (x, y, z) -> (Math.floorMod((long) Math.floor((x + y * 0.35) / period * 1000), 1000L) < width / period * 1000)
                ? stripe.at(x, y, z) : base.at(x, y, z);
    }

    /** Taches pseudo-aléatoires (vache, girafe, dalmatien). */
    static Mat spots(long seed, double cell, double threshold, Mat base, Mat spot) {
        return (x, y, z) -> Noise.value3(seed, x / cell, y / cell, z / cell) > threshold ? spot.at(x, y, z) : base.at(x, y, z);
    }

    /** Mélange de deux matériaux selon le bruit (pierre moussue, roche). */
    static Mat mix(long seed, double cell, double ratio, Mat a, Mat b) {
        return (x, y, z) -> Noise.hash(seed, (int) Math.floor(x / cell), (int) Math.floor(y / cell), (int) Math.floor(z / cell)) < ratio
                ? b.at(x, y, z) : a.at(x, y, z);
    }

    /** Dégradé vertical : {@code below} sous {@code yLimit}, {@code above} au-dessus. */
    static Mat splitY(double yLimit, Mat below, Mat above) {
        return (x, y, z) -> y < yLimit ? below.at(x, y, z) : above.at(x, y, z);
    }

    /** Face avant (z > zLimit) d'une autre matière : ventre clair, plastron. */
    static Mat front(double zLimit, Mat back, Mat front) {
        return (x, y, z) -> z > zLimit ? front.at(x, y, z) : back.at(x, y, z);
    }

    /** Damier 3D (haori de Tanjiro, carrelages). */
    static Mat checker(double cell, Mat a, Mat b) {
        return (x, y, z) -> ((Math.floorMod((long) Math.floor(x / cell), 2L) + Math.floorMod((long) Math.floor(y / cell), 2L)
                + Math.floorMod((long) Math.floor(z / cell), 2L)) % 2 == 0) ? a.at(x, y, z) : b.at(x, y, z);
    }
}
