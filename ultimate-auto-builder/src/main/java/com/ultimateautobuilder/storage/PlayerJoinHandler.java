package com.ultimateautobuilder.storage;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.registry.ModItems;
import com.ultimateautobuilder.selection.Selections;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Remise unique de l'Ultimate Crystal à la première connexion (option) et nettoyage à la déconnexion. */
public final class PlayerJoinHandler {
    // Nouvelle étiquette : les joueurs qui avaient reçu l'ancien Cristal de Glace reçoivent une fois l'Ultimate Crystal.
    private static final String TAG = UltimateAutoBuilderMod.MOD_ID + ":ultimate_crystal_given";

    private PlayerJoinHandler() {
    }

    public static void onLogin(Player player) {
        if (player.level().isClientSide() || !ServerConfig.loaded() || !ServerConfig.GIVE_CRYSTAL_ON_FIRST_JOIN.get()) {
            return;
        }
        // Sous-étiquette « PlayerPersisted » : conservée par NeoForge après la mort du joueur.
        CompoundTag root = player.getPersistentData();
        CompoundTag data = root.getCompound("PlayerPersisted");
        if (data.getBoolean(TAG)) {
            return;
        }
        data.putBoolean(TAG, true);
        root.put("PlayerPersisted", data);
        ItemStack stack = new ItemStack(ModItems.ULTIMATE_CRYSTAL.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void onLogout(Player player) {
        Selections.clear(player.getUUID());
    }
}
