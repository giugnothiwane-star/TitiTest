package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.network.Net;
import com.ultimateautobuilder.network.RewardFxPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Remise des récompenses : directement dans l'inventaire ; ce qui ne rentre pas est conservé
 * dans les objets en attente du joueur (récupérables depuis le cristal), jamais perdu.
 */
public final class Rewards {
    private Rewards() {
    }

    /** Donne les objets ; renvoie le nombre de piles mises en attente faute de place. */
    public static int give(ServerPlayer player, List<ItemStack> stacks, Component title) {
        AdventureData data = AdventureData.get(player.server);
        AdventureData.PlayerState ps = data.player(player.getUUID());
        int pending = 0;
        List<ItemStack> shown = new ArrayList<>();
        for (ItemStack st : stacks) {
            if (st.isEmpty()) {
                continue;
            }
            shown.add(st.copy());
            ItemStack left = st.copy();
            player.getInventory().add(left);
            if (!left.isEmpty()) {
                ps.pending.add(left);
                pending++;
            }
        }
        if (pending > 0) {
            data.setDirty();
            player.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".inventory_full", pending)
                    .withStyle(ChatFormatting.GOLD), false);
        }
        player.getInventory().setChanged();
        Net.send(player, new RewardFxPayload(title.getString(), shown));
        return pending;
    }

    /** Remet les objets en attente tant qu'il y a de la place ; le reste reste en attente. */
    public static int claimPending(ServerPlayer player) {
        AdventureData data = AdventureData.get(player.server);
        AdventureData.PlayerState ps = data.player(player.getUUID());
        int given = 0;
        List<ItemStack> keep = new ArrayList<>();
        List<ItemStack> shown = new ArrayList<>();
        for (ItemStack st : ps.pending) {
            ItemStack left = st.copy();
            int before = left.getCount();
            player.getInventory().add(left);
            if (left.getCount() < before) {
                ItemStack moved = st.copy();
                moved.setCount(before - left.getCount());
                shown.add(moved);
                given++;
            }
            if (!left.isEmpty()) {
                keep.add(left);
            }
        }
        ps.pending.clear();
        ps.pending.addAll(keep);
        data.setDirty();
        if (!shown.isEmpty()) {
            Net.send(player, new RewardFxPayload(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID
                    + ".pending_title").getString(), shown));
        }
        return given;
    }

    /** Objet identifié par un identifiant de registre, ou spécial (« special: »). */
    public static ItemStack stack(ServerPlayer player, String id, int count) {
        if (id.equals("special:op_pickaxe")) {
            return opPickaxe(player);
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            UltimateAutoBuilderMod.LOGGER.error("Récompense invalide : {}", id);
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(rl);
        return new ItemStack(item, Math.max(1, Math.min(count, item.getDefaultMaxStackSize() * 4)));
    }

    /** Objet OP légendaire : pioche en Netherite Efficacité XXX, Solidité III, Fortune III. */
    public static ItemStack opPickaxe(ServerPlayer player) {
        ItemStack st = new ItemStack(Items.NETHERITE_PICKAXE);
        var reg = player.server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        enchant(st, reg.getHolderOrThrow(Enchantments.EFFICIENCY), 30);
        enchant(st, reg.getHolderOrThrow(Enchantments.UNBREAKING), 3);
        enchant(st, reg.getHolderOrThrow(Enchantments.FORTUNE), 3);
        st.set(DataComponents.CUSTOM_NAME, Component.translatable("item." + UltimateAutoBuilderMod.MOD_ID + ".op_pickaxe")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        st.set(DataComponents.RARITY, Rarity.EPIC);
        return st;
    }

    private static void enchant(ItemStack st, Holder<Enchantment> e, int level) {
        // Vérification de compatibilité : jamais d'enchantement sur un objet qui ne le supporte pas.
        if (e.value().canEnchant(st)) {
            st.enchant(e, level);
        }
    }

    /** Tire une table de butin du mod pour la remettre directement au joueur. */
    public static List<ItemStack> roll(ServerPlayer player, String table) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, UltimateAutoBuilderMod.id(table));
        LootTable lt = player.server.reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(player.serverLevel()).withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        return new ArrayList<>(lt.getRandomItems(params));
    }

    public static ResourceKey<LootTable> lootKey(String table) {
        return ResourceKey.create(Registries.LOOT_TABLE, UltimateAutoBuilderMod.id(table));
    }
}
