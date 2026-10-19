package com.ultimateautobuilder.client.visual;

import java.util.Arrays;

/**
 * Mesure des performances de rendu sur la machine du joueur : temps de chaque image (horloge
 * système, aucune synchronisation GPU forcée), FPS moyens et « 1 % bas ». Le banc d'essai compare
 * le rendu sans effets puis avec le profil actif, dans les mêmes conditions (même position, même vue :
 * le joueur est invité à ne pas bouger). Les résultats affichés sont uniquement ceux réellement mesurés.
 */
public final class FrameStats {
    private static final int CAPACITY = 4096;
    private static final long[] frameNanos = new long[CAPACITY];
    private static int count;
    private static int head;
    private static long last;

    /** Phases du banc d'essai. */
    public enum Phase { IDLE, WARMUP_OFF, MEASURE_OFF, WARMUP_ON, MEASURE_ON, DONE }

    private static Phase phase = Phase.IDLE;
    private static long phaseStart;
    private static double fpsOff, lowOff;
    private static PerformanceVerdict.Result result;
    private static final long WARMUP = 1_500_000_000L, MEASURE = 5_000_000_000L;

    private FrameStats() {
    }

    /** Appelé à la fin de chaque image. */
    public static void onFrame() {
        long now = System.nanoTime();
        if (last != 0) {
            frameNanos[head] = now - last;
            head = (head + 1) % CAPACITY;
            count = Math.min(CAPACITY, count + 1);
        }
        last = now;
        advanceBench(now);
    }

    private static double[] window(long spanNanos) {
        int n = 0;
        long total = 0;
        double[] tmp = new double[count];
        for (int i = 1; i <= count && total < spanNanos; i++) {
            long d = frameNanos[(head - i + CAPACITY) % CAPACITY];
            tmp[n++] = d;
            total += d;
        }
        return Arrays.copyOf(tmp, n);
    }

    /** FPS moyens sur la période demandée. */
    public static double fps(long spanNanos) {
        double[] w = window(spanNanos);
        if (w.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double d : w) {
            sum += d;
        }
        return w.length / (sum / 1e9);
    }

    /** « 1 % bas » : FPS correspondant au centile 99 des temps d'image (saccades). */
    public static double low1(long spanNanos) {
        double[] w = window(spanNanos);
        if (w.length < 20) {
            return fps(spanNanos);
        }
        Arrays.sort(w);
        double p99 = w[(int) Math.floor(w.length * 0.99) - 1];
        return 1e9 / p99;
    }

    public static double lastFrameMs() {
        return count == 0 ? 0 : frameNanos[(head - 1 + CAPACITY) % CAPACITY] / 1e6;
    }

    // ------------------------------------------------------------------ banc d'essai

    public static Phase phase() {
        return phase;
    }

    public static PerformanceVerdict.Result result() {
        return result;
    }

    public static void startBenchmark() {
        result = null;
        phase = Phase.WARMUP_OFF;
        phaseStart = System.nanoTime();
        PostEffects.setSuspended(true);
    }

    public static void cancelBenchmark() {
        phase = Phase.IDLE;
        PostEffects.setSuspended(false);
    }

    public static double benchProgress() {
        long el = System.nanoTime() - phaseStart;
        return switch (phase) {
            case WARMUP_OFF -> 0.0 + 0.1 * el / WARMUP;
            case MEASURE_OFF -> 0.1 + 0.4 * el / MEASURE;
            case WARMUP_ON -> 0.5 + 0.1 * el / WARMUP;
            case MEASURE_ON -> 0.6 + 0.4 * el / MEASURE;
            case DONE -> 1.0;
            default -> 0.0;
        };
    }

    private static void advanceBench(long now) {
        long el = now - phaseStart;
        switch (phase) {
            case WARMUP_OFF -> {
                if (el >= WARMUP) {
                    next(Phase.MEASURE_OFF, now);
                }
            }
            case MEASURE_OFF -> {
                if (el >= MEASURE) {
                    fpsOff = fps(MEASURE);
                    lowOff = low1(MEASURE);
                    PostEffects.setSuspended(false);
                    next(Phase.WARMUP_ON, now);
                }
            }
            case WARMUP_ON -> {
                if (el >= WARMUP) {
                    next(Phase.MEASURE_ON, now);
                }
            }
            case MEASURE_ON -> {
                if (el >= MEASURE) {
                    double on = fps(MEASURE), low = low1(MEASURE);
                    result = PerformanceVerdict.evaluate(fpsOff, lowOff, on, low, VisualStore.current().quality);
                    next(Phase.DONE, now);
                }
            }
            default -> {
            }
        }
    }

    private static void next(Phase p, long now) {
        phase = p;
        phaseStart = now;
    }
}
