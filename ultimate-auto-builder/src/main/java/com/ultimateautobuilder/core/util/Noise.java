package com.ultimateautobuilder.core.util;

/**
 * Bruit de valeur lissé (2D/3D) sans état global, utilisé pour les reliefs des îles,
 * les feuillages et les motifs de matériaux. Déterministe pour une graine donnée.
 */
public final class Noise {
    private Noise() {
    }

    public static double hash(long seed, int x, int y, int z) {
        long h = seed;
        h ^= x * 0x27D4EB2F165667C5L;
        h ^= y * 0x165667B19E3779F9L;
        h ^= z * 0x85EBCA77C2B2AE63L;
        h = (h ^ (h >>> 31)) * 0x7FB5D329728EA185L;
        h = (h ^ (h >>> 27)) * 0x81DADEF4BC2DD44DL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double value2(long seed, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fz = smooth(z - z0);
        double a = hash(seed, x0, 0, z0);
        double b = hash(seed, x0 + 1, 0, z0);
        double c = hash(seed, x0, 0, z0 + 1);
        double d = hash(seed, x0 + 1, 0, z0 + 1);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    public static double value3(long seed, double x, double y, double z) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double c000 = hash(seed, x0, y0, z0);
        double c100 = hash(seed, x0 + 1, y0, z0);
        double c010 = hash(seed, x0, y0 + 1, z0);
        double c110 = hash(seed, x0 + 1, y0 + 1, z0);
        double c001 = hash(seed, x0, y0, z0 + 1);
        double c101 = hash(seed, x0 + 1, y0, z0 + 1);
        double c011 = hash(seed, x0, y0 + 1, z0 + 1);
        double c111 = hash(seed, x0 + 1, y0 + 1, z0 + 1);
        double x00 = lerp(c000, c100, fx);
        double x10 = lerp(c010, c110, fx);
        double x01 = lerp(c001, c101, fx);
        double x11 = lerp(c011, c111, fx);
        return lerp(lerp(x00, x10, fy), lerp(x01, x11, fy), fz);
    }

    /** Bruit fractal 2D normalisé dans [0, 1]. */
    public static double fbm2(long seed, double x, double z, int octaves) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double freq = 1;
        for (int i = 0; i < octaves; i++) {
            sum += amp * value2(seed + i * 7919L, x * freq, z * freq);
            norm += amp;
            amp *= 0.5;
            freq *= 2;
        }
        return sum / norm;
    }
}
