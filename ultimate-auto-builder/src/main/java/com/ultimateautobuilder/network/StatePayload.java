package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** État du serveur pour le menu (serveur → client) : droits, limites, réglages, tâches, historique, modèles personnalisés. */
public record StatePayload(boolean canBuild, boolean admin, boolean canImport, int maxBlocks, int giantThreshold, int minY, int maxY,
                           Map<String, String> settings, List<JobInfo> jobs, List<HistoryInfo> history, List<CustomInfo> customs,
                           List<String> importable, long corner1, long corner2, boolean hasCorner1, boolean hasCorner2)
        implements CustomPacketPayload {

    public record JobInfo(int id, String title, int progress, int total, int state, String owner) {
    }

    public record HistoryInfo(int id, String modelId, String name, int[] bounds, int rotation, int blocks, long startedAt, String status,
                              boolean undoAvailable, String owner) {
    }

    public record CustomInfo(String id, String name, int sx, int sy, int sz, int blocks) {
    }

    public static final Type<StatePayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("state"));
    public static final StreamCodec<FriendlyByteBuf, StatePayload> CODEC = StreamCodec.ofMember(StatePayload::write, StatePayload::read);
    private static final int MAX_LIST = 512;

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(canBuild);
        buf.writeBoolean(admin);
        buf.writeBoolean(canImport);
        buf.writeVarInt(maxBlocks);
        buf.writeVarInt(giantThreshold);
        buf.writeInt(minY);
        buf.writeInt(maxY);
        buf.writeVarInt(Math.min(settings.size(), MAX_LIST));
        settings.entrySet().stream().limit(MAX_LIST).forEach(e -> {
            buf.writeUtf(e.getKey(), 64);
            buf.writeUtf(e.getValue(), 256);
        });
        buf.writeVarInt(Math.min(jobs.size(), MAX_LIST));
        jobs.stream().limit(MAX_LIST).forEach(j -> {
            buf.writeVarInt(j.id());
            buf.writeUtf(j.title(), 128);
            buf.writeVarInt(j.progress());
            buf.writeVarInt(j.total());
            buf.writeVarInt(j.state());
            buf.writeUtf(j.owner(), 64);
        });
        buf.writeVarInt(Math.min(history.size(), MAX_LIST));
        history.stream().limit(MAX_LIST).forEach(h -> {
            buf.writeVarInt(h.id());
            buf.writeUtf(h.modelId(), 128);
            buf.writeUtf(h.name(), 128);
            buf.writeVarIntArray(h.bounds());
            buf.writeVarInt(h.rotation());
            buf.writeVarInt(h.blocks());
            buf.writeLong(h.startedAt());
            buf.writeUtf(h.status(), 32);
            buf.writeBoolean(h.undoAvailable());
            buf.writeUtf(h.owner(), 64);
        });
        buf.writeVarInt(Math.min(customs.size(), MAX_LIST));
        customs.stream().limit(MAX_LIST).forEach(c -> {
            buf.writeUtf(c.id(), 128);
            buf.writeUtf(c.name(), 128);
            buf.writeVarInt(c.sx());
            buf.writeVarInt(c.sy());
            buf.writeVarInt(c.sz());
            buf.writeVarInt(c.blocks());
        });
        buf.writeVarInt(Math.min(importable.size(), MAX_LIST));
        importable.stream().limit(MAX_LIST).forEach(s -> buf.writeUtf(s, 256));
        buf.writeLong(corner1);
        buf.writeLong(corner2);
        buf.writeBoolean(hasCorner1);
        buf.writeBoolean(hasCorner2);
    }

    private static int count(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_LIST) {
            throw new IllegalArgumentException("Liste trop longue : " + n);
        }
        return n;
    }

    private static StatePayload read(FriendlyByteBuf buf) {
        boolean canBuild = buf.readBoolean(), admin = buf.readBoolean(), canImport = buf.readBoolean();
        int maxBlocks = buf.readVarInt(), giant = buf.readVarInt(), minY = buf.readInt(), maxY = buf.readInt();
        Map<String, String> settings = new LinkedHashMap<>();
        for (int i = 0, n = count(buf); i < n; i++) {
            settings.put(buf.readUtf(64), buf.readUtf(256));
        }
        List<JobInfo> jobs = new ArrayList<>();
        for (int i = 0, n = count(buf); i < n; i++) {
            jobs.add(new JobInfo(buf.readVarInt(), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64)));
        }
        List<HistoryInfo> history = new ArrayList<>();
        for (int i = 0, n = count(buf); i < n; i++) {
            history.add(new HistoryInfo(buf.readVarInt(), buf.readUtf(128), buf.readUtf(128), buf.readVarIntArray(6), buf.readVarInt(),
                    buf.readVarInt(), buf.readLong(), buf.readUtf(32), buf.readBoolean(), buf.readUtf(64)));
        }
        List<CustomInfo> customs = new ArrayList<>();
        for (int i = 0, n = count(buf); i < n; i++) {
            customs.add(new CustomInfo(buf.readUtf(128), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        List<String> importable = new ArrayList<>();
        for (int i = 0, n = count(buf); i < n; i++) {
            importable.add(buf.readUtf(256));
        }
        long c1 = buf.readLong(), c2 = buf.readLong();
        boolean h1 = buf.readBoolean(), h2 = buf.readBoolean();
        return new StatePayload(canBuild, admin, canImport, maxBlocks, giant, minY, maxY, settings, jobs, history, customs, importable, c1, c2, h1, h2);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
