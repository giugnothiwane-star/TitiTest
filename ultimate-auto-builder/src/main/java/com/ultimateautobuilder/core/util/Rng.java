package com.ultimateautobuilder.core.util;

/**
 * Générateur pseudo-aléatoire SplitMix64 entièrement déterministe.
 * Les générateurs doivent produire exactement la même structure pour une même graine,
 * condition indispensable à la reprise d'une construction après redémarrage du serveur.
 */
public final class Rng {
    private long state;

    public Rng(long seed) {
        this.state = seed ^ 0x9E3779B97F4A7C15L;
    }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }
        return (int) Math.floorMod(nextLong(), (long) bound);
    }

    public int range(int minInclusive, int maxInclusive) {
        if (maxInclusive < minInclusive) {
            return minInclusive;
        }
        return minInclusive + nextInt(maxInclusive - minInclusive + 1);
    }

    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public double range(double min, double max) {
        return min + nextDouble() * (max - min);
    }

    public boolean chance(double probability) {
        return nextDouble() < probability;
    }

    public <T> T pick(T[] values) {
        return values[nextInt(values.length)];
    }

    /** Dérive une graine stable à partir d'une chaîne (identifiant de modèle). */
    public static long seedOf(String text) {
        long h = 1125899906842597L;
        for (int i = 0; i < text.length(); i++) {
            h = 31 * h + text.charAt(i);
        }
        return h;
    }
}
