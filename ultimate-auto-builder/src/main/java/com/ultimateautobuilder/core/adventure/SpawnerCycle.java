package com.ultimateautobuilder.core.adventure;

/**
 * Cycle d'un spawner de château. Un cycle produit au plus {@link #PER_CYCLE} créatures, une par
 * tentative espacée de {@link #ATTEMPT_DELAY} ticks ; le cycle suivant ne commence qu'après la
 * disparition des créatures du cycle et un temps de repos. Après {@code maxCycles} cycles, le
 * spawner est épuisé : il ne produit plus rien.
 */
public final class SpawnerCycle {
    public static final int PER_CYCLE = 4;
    public static final int ATTEMPT_DELAY = 40;
    public static final int CYCLE_COOLDOWN = 600;
    public static final int DEFAULT_MAX_CYCLES = 3;

    public enum Action { WAIT, ATTEMPT, DEPLETED }

    private int maxCycles;
    private int cyclesDone;
    private int spawnedThisCycle;
    private int cooldown;

    public SpawnerCycle(int maxCycles) {
        this.maxCycles = Math.max(1, maxCycles);
    }

    /**
     * Décision pour {@code elapsed} ticks écoulés depuis l'appel précédent, avec {@code alive}
     * créatures de ce spawner encore présentes.
     */
    public Action step(int elapsed, int alive) {
        if (depleted()) {
            return Action.DEPLETED;
        }
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - Math.max(0, elapsed));
            if (cooldown > 0) {
                return Action.WAIT;
            }
        }
        if (spawnedThisCycle >= PER_CYCLE) {
            if (alive > 0) {
                return Action.WAIT;
            }
            // Cycle terminé : toutes ses créatures ont disparu.
            cyclesDone++;
            spawnedThisCycle = 0;
            if (depleted()) {
                return Action.DEPLETED;
            }
            cooldown = CYCLE_COOLDOWN;
            return Action.WAIT;
        }
        if (alive >= PER_CYCLE) {
            return Action.WAIT;
        }
        return Action.ATTEMPT;
    }

    /** À appeler après chaque créature réellement apparue. */
    public void onSpawned() {
        spawnedThisCycle = Math.min(PER_CYCLE, spawnedThisCycle + 1);
        cooldown = ATTEMPT_DELAY;
    }

    /** À appeler après une tentative sans apparition (aucune place libre) : nouvel essai plus tard. */
    public void onFailed() {
        cooldown = ATTEMPT_DELAY;
    }

    public boolean depleted() {
        return cyclesDone >= maxCycles;
    }

    public int cyclesDone() {
        return cyclesDone;
    }

    public int spawnedThisCycle() {
        return spawnedThisCycle;
    }

    public int maxCycles() {
        return maxCycles;
    }

    public int cooldown() {
        return cooldown;
    }

    public int[] save() {
        return new int[]{maxCycles, cyclesDone, spawnedThisCycle, cooldown};
    }

    /** Relecture tolérante : valeurs bornées. */
    public static SpawnerCycle load(int[] data) {
        SpawnerCycle c = new SpawnerCycle(data != null && data.length > 0 ? data[0] : DEFAULT_MAX_CYCLES);
        if (data != null && data.length >= 4) {
            c.cyclesDone = Math.max(0, Math.min(c.maxCycles, data[1]));
            c.spawnedThisCycle = Math.max(0, Math.min(PER_CYCLE, data[2]));
            c.cooldown = Math.max(0, Math.min(CYCLE_COOLDOWN, data[3]));
        }
        return c;
    }
}
