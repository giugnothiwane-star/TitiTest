package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Morceau d'un modèle .uabm envoyé par le client (Image-to-Build). Les paquets client → serveur
 * sont limités à 32 Ko par Minecraft : le modèle est découpé, réassemblé puis entièrement revalidé.
 */
public record ModelUploadPayload(int uploadId, int index, int total, String name, byte[] chunk) implements CustomPacketPayload {
    public static final int CHUNK_BYTES = 30_000;
    public static final int MAX_CHUNKS = 2048;

    public static final Type<ModelUploadPayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("model_upload"));
    public static final StreamCodec<FriendlyByteBuf, ModelUploadPayload> CODEC = StreamCodec.ofMember(ModelUploadPayload::write,
            ModelUploadPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(uploadId);
        buf.writeVarInt(index);
        buf.writeVarInt(total);
        buf.writeUtf(name == null ? "" : name, 64);
        buf.writeByteArray(chunk);
    }

    private static ModelUploadPayload read(FriendlyByteBuf buf) {
        return new ModelUploadPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64), buf.readByteArray(CHUNK_BYTES));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
