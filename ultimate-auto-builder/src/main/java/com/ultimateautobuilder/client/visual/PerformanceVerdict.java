package com.ultimateautobuilder.client.visual;

/** Interprétation des mesures de FPS (classe pure, testée unitairement). */
public final class PerformanceVerdict {
    private PerformanceVerdict() {
    }

    public record Result(double fpsOff, double lowOff, double fpsOn, double lowOn, String verdict, VisualProfile.Quality advice) {
        public double lost() {
            return fpsOff - fpsOn;
        }
    }

    /**
     * Interprétation des mesures : seuils absolus demandés (0-50 acceptable, 51-100 avertissement,
     * 101-149 sérieux, 150+ critique) pondérés par la perte relative, car perdre 50 FPS sur 300 ou sur 80
     * n'a pas le même effet. Une qualité inférieure est conseillée quand la dégradation est sérieuse.
     */
    public static Result evaluate(double off, double lowOffV, double on, double lowOnV, VisualProfile.Quality q) {
        double lost = off - on;
        double rel = off > 0 ? lost / off : 0;
        String verdict;
        VisualProfile.Quality advice = null;
        if (lost <= 50 && rel <= 0.25 && on >= 50) {
            verdict = "Acceptable : perte de " + Math.round(lost) + " FPS (" + Math.round(rel * 100) + " %)";
        } else if (lost <= 100 && rel <= 0.40) {
            verdict = "Avertissement : perte de " + Math.round(lost) + " FPS (" + Math.round(rel * 100) + " %)";
            advice = lower(q);
        } else if (lost < 150 && rel <= 0.55) {
            verdict = "Dégradation sérieuse : perte de " + Math.round(lost) + " FPS (" + Math.round(rel * 100) + " %)";
            advice = lower(q);
        } else {
            verdict = "Dégradation critique : perte de " + Math.round(lost) + " FPS (" + Math.round(rel * 100) + " %)";
            advice = VisualProfile.Quality.PERFORMANCE;
        }
        if (lowOnV < lowOffV * 0.6 && advice == null) {
            verdict += " ; saccades accrues (1 % bas " + Math.round(lowOffV) + " → " + Math.round(lowOnV) + ")";
            advice = lower(q);
        }
        return new Result(off, lowOffV, on, lowOnV, verdict, advice == q ? null : advice);
    }

    private static VisualProfile.Quality lower(VisualProfile.Quality q) {
        return switch (q) {
            case ULTRA -> VisualProfile.Quality.HIGH;
            case HIGH -> VisualProfile.Quality.BALANCED;
            default -> VisualProfile.Quality.PERFORMANCE;
        };
    }
}
