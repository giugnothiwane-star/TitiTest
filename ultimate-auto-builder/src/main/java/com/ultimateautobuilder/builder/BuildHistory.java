package com.ultimateautobuilder.builder;

import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.storage.HistoryEntry;
import com.ultimateautobuilder.storage.ModStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Historique des constructions du monde, limité en taille ; les journaux d'annulation anciens sont supprimés. */
public final class BuildHistory {
    private final List<HistoryEntry> entries = new ArrayList<>();
    private int nextId;
    private boolean dirty;

    public BuildHistory(ModStorage.HistoryData data) {
        this.entries.addAll(data.entries());
        this.nextId = data.nextId();
        // Une construction « en cours » sans point de reprise a été interrompue brutalement.
        for (HistoryEntry e : entries) {
            if (e.status == HistoryEntry.Status.RUNNING && !java.nio.file.Files.exists(ModStorage.jobFile(e.id))) {
                e.status = HistoryEntry.Status.FAILED;
                dirty = true;
            }
        }
    }

    public synchronized int allocateId() {
        dirty = true;
        return nextId++;
    }

    public synchronized void add(HistoryEntry e) {
        entries.add(0, e);
        trim();
        dirty = true;
    }

    public synchronized HistoryEntry get(int id) {
        for (HistoryEntry e : entries) {
            if (e.id == id) {
                return e;
            }
        }
        return null;
    }

    public synchronized void update(int id, int[] bounds, int blocks, HistoryEntry.Status status) {
        HistoryEntry e = get(id);
        if (e != null) {
            if (bounds != null) {
                System.arraycopy(bounds, 0, e.bounds, 0, 6);
            }
            e.blocks = blocks;
            e.status = status;
            dirty = true;
        }
    }

    public synchronized void finish(int id, int[] bounds, int blocks, HistoryEntry.Status status, boolean undoAvailable) {
        update(id, bounds, blocks, status);
        HistoryEntry e = get(id);
        if (e != null) {
            e.undoAvailable = undoAvailable;
        }
    }

    public synchronized void markUndone(int id) {
        HistoryEntry e = get(id);
        if (e != null) {
            e.status = HistoryEntry.Status.UNDONE;
            e.undoAvailable = false;
            dirty = true;
        }
    }

    /** Dernière construction annulable d'un joueur (ou de n'importe qui si owner est null). */
    public synchronized HistoryEntry lastUndoable(UUID owner) {
        for (HistoryEntry e : entries) {
            if (e.undoAvailable && (owner == null || owner.equals(e.owner))) {
                return e;
            }
        }
        return null;
    }

    public synchronized List<HistoryEntry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    private void trim() {
        int max = ServerConfig.loaded() ? ServerConfig.HISTORY_SIZE.get() : 20;
        while (entries.size() > max) {
            HistoryEntry old = entries.remove(entries.size() - 1);
            if (old.status == HistoryEntry.Status.RUNNING) {
                entries.add(old);
                break;
            }
            ModStorage.deleteAsync(ModStorage.undoFile(old.id));
        }
    }

    public synchronized void saveIfDirty() {
        if (dirty) {
            ModStorage.saveHistory(nextId, entries);
            dirty = false;
        }
    }

    public synchronized void saveNow() {
        ModStorage.saveHistory(nextId, entries);
        dirty = false;
    }
}
