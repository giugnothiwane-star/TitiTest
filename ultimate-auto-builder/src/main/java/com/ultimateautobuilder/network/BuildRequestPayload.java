package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Demande de construction (client → serveur). Elle ne transporte que des paramètres :
 * le serveur régénère lui-même le modèle et applique toutes les limites.
 */
public record BuildRequestPayload(String modelId, int preset, int customSize, int rotation, boolean mirror, int x, int y, int z, int mode,
                                  boolean hollow, boolean confirmGiant) implements CustomPacketPayload {
    public static final Type<BuildRequestPayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("build_request"));
    public static final StreamCodec<FriendlyByteBuf, BuildRequestPayload> CODEC = StreamCodec.ofMember(BuildRequestPayload::write,
            BuildRequestPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(modelId, 128);
        buf.writeVarInt(preset);
        buf.writeVarInt(customSize);
        buf.writeVarInt(rotation);
        buf.writeBoolean(mirror);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeVarInt(mode);
        buf.writeBoolean(hollow);
        buf.writeBoolean(confirmGiant);
    }

    private static BuildRequestPayload read(FriendlyByteBuf buf) {
        return new BuildRequestPayload(buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
