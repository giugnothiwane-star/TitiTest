package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Récompense remise (serveur → client) : déclenche l'animation du cristal. Affichage seulement. */
public record RewardFxPayload(String title, List<ItemStack> items) implements CustomPacketPayload {
    private static final int MAX = 64;
    public static final Type<RewardFxPayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("reward_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RewardFxPayload> CODEC = StreamCodec.ofMember(RewardFxPayload::write, RewardFxPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(title, 256);
        int n = Math.min(items.size(), MAX);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, items.get(i));
        }
    }

    private static RewardFxPayload read(RegistryFriendlyByteBuf buf) {
        String title = buf.readUtf(256);
        int n = buf.readVarInt();
        if (n < 0 || n > MAX) {
            throw new IllegalArgumentException("Liste trop longue : " + n);
        }
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new RewardFxPayload(title, items);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
