package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Progression d'une tâche (serveur → client) pour la barre de progression. */
public record JobProgressPayload(int jobId, String title, int progress, int total, int state, String message) implements CustomPacketPayload {
    public static final Type<JobProgressPayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("job_progress"));
    public static final StreamCodec<FriendlyByteBuf, JobProgressPayload> CODEC = StreamCodec.ofMember(JobProgressPayload::write,
            JobProgressPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(jobId);
        buf.writeUtf(title, 128);
        buf.writeVarInt(progress);
        buf.writeVarInt(total);
        buf.writeVarInt(state);
        buf.writeUtf(message, 512);
    }

    private static JobProgressPayload read(FriendlyByteBuf buf) {
        return new JobProgressPayload(buf.readVarInt(), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(512));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
