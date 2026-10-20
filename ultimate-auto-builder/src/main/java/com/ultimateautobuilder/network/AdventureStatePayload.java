package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * État d'aventure du joueur (serveur → client) pour le hub du cristal : récompenses d'armure,
 * objets en attente, découvertes, Wardens vaincus, sanctuaire OP. Affichage uniquement : chaque
 * action est revérifiée par le serveur.
 *
 * @param armor état par ensemble (ordre de {@code ArmorSetReward}) : 0 verrouillé, 1 disponible, 2 récupéré
 * @param sanctuary {x, z, état} ou tableau vide si le joueur ne le connaît pas
 */
public record AdventureStatePayload(int[] armor, List<ItemStack> pending, List<Discovery> discovered, int wardens, int bosses,
                                    int[] sanctuary, int structuresPlaced) implements CustomPacketPayload {
    public record Discovery(String type, int x, int y, int z) {
    }

    private static final int MAX = 512;
    public static final Type<AdventureStatePayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("adventure_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdventureStatePayload> CODEC =
            StreamCodec.ofMember(AdventureStatePayload::write, AdventureStatePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarIntArray(armor);
        int n = Math.min(pending.size(), 64);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pending.get(i));
        }
        int d = Math.min(discovered.size(), MAX);
        buf.writeVarInt(d);
        for (int i = 0; i < d; i++) {
            Discovery x = discovered.get(i);
            buf.writeUtf(x.type(), 64);
            buf.writeVarInt(x.x());
            buf.writeVarInt(x.y());
            buf.writeVarInt(x.z());
        }
        buf.writeVarInt(wardens);
        buf.writeVarInt(bosses);
        buf.writeVarIntArray(sanctuary);
        buf.writeVarInt(structuresPlaced);
    }

    private static int count(RegistryFriendlyByteBuf buf, int max) {
        int n = buf.readVarInt();
        if (n < 0 || n > max) {
            throw new IllegalArgumentException("Liste trop longue : " + n);
        }
        return n;
    }

    private static AdventureStatePayload read(RegistryFriendlyByteBuf buf) {
        int[] armor = buf.readVarIntArray(16);
        List<ItemStack> pending = new ArrayList<>();
        for (int i = 0, n = count(buf, 64); i < n; i++) {
            pending.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        List<Discovery> disc = new ArrayList<>();
        for (int i = 0, n = count(buf, MAX); i < n; i++) {
            disc.add(new Discovery(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new AdventureStatePayload(armor, pending, disc, buf.readVarInt(), buf.readVarInt(), buf.readVarIntArray(3), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
