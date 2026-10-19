package com.ultimateautobuilder.storage;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** Entrée d'historique d'une construction (persistée avec le monde). */
public final class HistoryEntry {
    public enum Status { RUNNING, DONE, CANCELLED, FAILED, UNDONE }

    public final int id;
    public final String modelId;
    public final String modelName;
    public final String dimension;
    public final int[] bounds; // minX, minY, minZ, maxX, maxY, maxZ
    public final int rotation;
    public final boolean mirror;
    public final UUID owner;
    public final String ownerName;
    public final long startedAt;
    public int blocks;
    public Status status;
    public boolean undoAvailable;

    public HistoryEntry(int id, String modelId, String modelName, String dimension, int[] bounds, int rotation, boolean mirror, UUID owner,
                        String ownerName, long startedAt, int blocks, Status status, boolean undoAvailable) {
        this.id = id;
        this.modelId = modelId;
        this.modelName = modelName;
        this.dimension = dimension;
        this.bounds = bounds.clone();
        this.rotation = rotation;
        this.mirror = mirror;
        this.owner = owner;
        this.ownerName = ownerName;
        this.startedAt = startedAt;
        this.blocks = blocks;
        this.status = status;
        this.undoAvailable = undoAvailable;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("model", modelId);
        t.putString("name", modelName);
        t.putString("dim", dimension);
        t.putIntArray("bounds", bounds);
        t.putInt("rot", rotation);
        t.putBoolean("mirror", mirror);
        if (owner != null) {
            t.putUUID("owner", owner);
        }
        t.putString("ownerName", ownerName);
        t.putLong("started", startedAt);
        t.putInt("blocks", blocks);
        t.putString("status", status.name());
        t.putBoolean("undo", undoAvailable);
        return t;
    }

    public static HistoryEntry load(CompoundTag t) {
        int[] b = t.getIntArray("bounds");
        if (b.length != 6) {
            b = new int[6];
        }
        Status st;
        try {
            st = Status.valueOf(t.getString("status"));
        } catch (IllegalArgumentException e) {
            st = Status.FAILED;
        }
        return new HistoryEntry(t.getInt("id"), t.getString("model"), t.getString("name"), t.getString("dim"), b, t.getInt("rot"),
                t.getBoolean("mirror"), t.hasUUID("owner") ? t.getUUID("owner") : null, t.getString("ownerName"), t.getLong("started"),
                t.getInt("blocks"), st, t.getBoolean("undo"));
    }
}
