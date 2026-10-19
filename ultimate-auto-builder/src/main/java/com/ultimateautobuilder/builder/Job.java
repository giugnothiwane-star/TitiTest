package com.ultimateautobuilder.builder;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Tâche exécutée progressivement sur le thread serveur (construction ou restauration). */
public abstract class Job {
    public enum State { GENERATING, LOADING, PLACING, SHAPING, DONE, CANCELLED, FAILED }

    protected final int id;
    protected final UUID owner;
    protected final String ownerName;
    protected final ResourceKey<Level> dimension;
    protected final String title;
    protected volatile State state = State.GENERATING;
    protected String message = "";
    /** Annulation demandée par un joueur ou le serveur (traitée au prochain tick, même en pause). */
    protected volatile boolean cancelRequested;
    /** En pause : la tâche garde ses chunks et sa progression mais ne pose plus rien. */
    protected volatile boolean paused;
    protected String pauseReason = "";
    /** Vrai une fois la mise en pause annoncée au joueur (évite de répéter le message). */
    boolean pauseAnnounced;
    protected final long createdAt = System.currentTimeMillis();

    protected Job(int id, UUID owner, String ownerName, ResourceKey<Level> dimension, String title) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.dimension = dimension;
        this.title = title;
    }

    public int id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public String title() {
        return title;
    }

    public State state() {
        return state;
    }

    public String message() {
        return message;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public boolean paused() {
        return paused && !finished();
    }

    public boolean cancelPending() {
        return cancelRequested;
    }

    public String pauseReason() {
        return pauseReason;
    }

    /** Met la tâche en pause (sans effet si elle est terminée). */
    public void pause(String reason) {
        if (!finished()) {
            paused = true;
            pauseReason = reason == null ? "" : reason;
        }
    }

    public void resume() {
        paused = false;
        pauseReason = "";
        pauseAnnounced = false;
    }

    public boolean finished() {
        return state == State.DONE || state == State.CANCELLED || state == State.FAILED;
    }

    public abstract int total();

    public abstract int progress();

    /** Avance la tâche ; renvoie le nombre de blocs traités (0 si en attente). */
    public abstract int tick(ServerLevel level, BuildManager manager, int blockBudget, long deadlineNanos);

    /** Annulation demandée : la tâche se termine proprement au prochain tick. */
    public abstract void cancel();

    /** Libère les ressources (tickets de chunks) et enregistre le résultat. */
    public abstract void finish(ServerLevel level, BuildManager manager);

    /** Point de reprise (null si la tâche ne se reprend pas). */
    public abstract net.minecraft.nbt.CompoundTag checkpoint();
}
