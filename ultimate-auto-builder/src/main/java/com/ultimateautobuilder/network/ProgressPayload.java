package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Progression du joueur (serveur → client) : niveau, titre, application de la progression,
 * constructions débloquées et avancement réel de chaque quête. Affichage uniquement : le serveur
 * revérifie tout à chaque action.
 */
public record ProgressPayload(int tier, String title, boolean enforced, List<String> unlocked, List<QuestInfo> quests, int builds)
        implements CustomPacketPayload {
    public record QuestInfo(String id, int state, int value, int required) {
    }

    private static final int MAX = 2048;
    public static final Type<ProgressPayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("progress"));
    public static final StreamCodec<FriendlyByteBuf, ProgressPayload> CODEC = StreamCodec.ofMember(ProgressPayload::write, ProgressPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(tier);
        buf.writeUtf(title, 64);
        buf.writeBoolean(enforced);
        buf.writeVarInt(Math.min(unlocked.size(), MAX));
        unlocked.stream().limit(MAX).forEach(s -> buf.writeUtf(s, 128));
        buf.writeVarInt(Math.min(quests.size(), MAX));
        quests.stream().limit(MAX).forEach(q -> {
            buf.writeUtf(q.id(), 64);
            buf.writeVarInt(q.state());
            buf.writeVarInt(q.value());
            buf.writeVarInt(q.required());
        });
        buf.writeVarInt(builds);
    }

    private static int count(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX) {
            throw new IllegalArgumentException("Liste trop longue : " + n);
        }
        return n;
    }

    private static ProgressPayload read(FriendlyByteBuf buf) {
        int tier = buf.readVarInt();
        String title = buf.readUtf(64);
        boolean enforced = buf.readBoolean();
        List<String> unlocked = new ArrayList<>();
        for (int i = 0, n = count(buf); i < n; i++) {
            unlocked.add(buf.readUtf(128));
        }
        List<QuestInfo> quests = new ArrayList<>();
        for (int i = 0, n = count(buf); i < n; i++) {
            quests.add(new QuestInfo(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new ProgressPayload(tier, title, enforced, unlocked, quests, buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
