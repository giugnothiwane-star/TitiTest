package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.adventure.ArmorSetReward;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Récompenses d'ensemble d'armure : vérifiées sur les quatre emplacements réellement équipés,
 * accordées une seule fois par ensemble et par joueur (retirer puis remettre une pièce ne
 * donne rien de plus), réclamées depuis le cristal.
 */
public final class ArmorRewards {
    public enum State { LOCKED, AVAILABLE, CLAIMED }

    private ArmorRewards() {
    }

    static String id(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    public static ArmorSetReward worn(ServerPlayer p) {
        return ArmorSetReward.wornSet(id(p.getItemBySlot(EquipmentSlot.HEAD)), id(p.getItemBySlot(EquipmentSlot.CHEST)),
                id(p.getItemBySlot(EquipmentSlot.LEGS)), id(p.getItemBySlot(EquipmentSlot.FEET)));
    }

    public static State state(ServerPlayer p, AdventureData.PlayerState ps, ArmorSetReward set) {
        if (ps.armorClaimed.contains(set.key)) {
            return State.CLAIMED;
        }
        return worn(p) == set ? State.AVAILABLE : State.LOCKED;
    }

    /** Changement d'équipement : signale une fois qu'une récompense est disponible. */
    public static void onEquipmentChange(ServerPlayer p) {
        ArmorSetReward set = worn(p);
        if (set == null) {
            return;
        }
        AdventureData data = AdventureData.get(p.server);
        AdventureData.PlayerState ps = data.player(p.getUUID());
        if (!ps.armorClaimed.contains(set.key) && ps.armorNotified.add(set.key)) {
            data.setDirty();
            p.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".armor_ready",
                    Component.translatable("armor_set." + UltimateAutoBuilderMod.MOD_ID + "." + set.key)).withStyle(ChatFormatting.GOLD), false);
        }
    }

    public static void claim(ServerPlayer p, String key) {
        ArmorSetReward set = ArmorSetReward.byKey(key);
        String ns = UltimateAutoBuilderMod.MOD_ID;
        if (set == null) {
            return;
        }
        AdventureData data = AdventureData.get(p.server);
        AdventureData.PlayerState ps = data.player(p.getUUID());
        if (ps.armorClaimed.contains(set.key)) {
            p.displayClientMessage(Component.translatable("message." + ns + ".already_claimed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (worn(p) != set) {
            p.displayClientMessage(Component.translatable("message." + ns + ".armor_not_worn",
                    Component.translatable("armor_set." + ns + "." + set.key)).withStyle(ChatFormatting.RED), true);
            return;
        }
        // Marquée avant la remise : une requête répétée ne peut plus rien obtenir.
        ps.armorClaimed.add(set.key);
        data.setDirty();
        List<ItemStack> items = new ArrayList<>();
        for (ArmorSetReward.Reward r : set.rewards) {
            ItemStack st = Rewards.stack(p, r.item(), r.count());
            if (!st.isEmpty()) {
                items.add(st);
            }
        }
        if (set == ArmorSetReward.NETHERITE) {
            ps.knowsSanctuary = true;
        }
        Rewards.give(p, items, Component.translatable("message." + ns + ".armor_reward_title",
                Component.translatable("armor_set." + ns + "." + set.key)));
    }
}
