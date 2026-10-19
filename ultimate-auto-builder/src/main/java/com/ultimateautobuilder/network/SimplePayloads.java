package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Paquets serveur → client de petite taille. */
public final class SimplePayloads {
    private SimplePayloads() {
    }

    /** Ouvre le menu (commande /uab menu). */
    public record OpenMenu() implements CustomPacketPayload {
        public static final Type<OpenMenu> TYPE = new Type<>(UltimateAutoBuilderMod.id("open_menu"));
        public static final StreamCodec<FriendlyByteBuf, OpenMenu> CODEC = StreamCodec.unit(new OpenMenu());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Aperçu réduit d'un modèle personnalisé (format .uabm, ≤ 256 Ko). */
    public record CustomPreview(String id, byte[] data) implements CustomPacketPayload {
        public static final int MAX_BYTES = 256 * 1024;
        public static final Type<CustomPreview> TYPE = new Type<>(UltimateAutoBuilderMod.id("custom_preview"));
        public static final StreamCodec<FriendlyByteBuf, CustomPreview> CODEC = StreamCodec.ofMember(CustomPreview::write, CustomPreview::read);

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(id, 128);
            buf.writeByteArray(data);
        }

        private static CustomPreview read(FriendlyByteBuf buf) {
            return new CustomPreview(buf.readUtf(128), buf.readByteArray(MAX_BYTES));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
