package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.adventure.AdventureGen;
import com.ultimateautobuilder.core.adventure.Blueprint;
import com.ultimateautobuilder.core.adventure.StructureType;
import com.ultimateautobuilder.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Kit et maison de départ, une seule fois par joueur et par monde. Seuls les joueurs réellement
 * nouveaux sont concernés (aucun temps de jeu enregistré et jamais équipés par une ancienne
 * version) : les anciennes sauvegardes ne reçoivent rien et ne perdent rien. La maison est posée
 * près du joueur sur un terrain naturel, porte tournée vers lui, jamais sur lui.
 */
public final class StarterKit {
    private static final String LEGACY_TAG = UltimateAutoBuilderMod.MOD_ID + ":ultimate_crystal_given";
    private static final String OLD_TAG = UltimateAutoBuilderMod.MOD_ID + ":crystal_given";
    private static final int MAX_ATTEMPTS = 15;

    private StarterKit() {
    }

    public static List<ItemStack> kit() {
        return List.of(new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_SWORD),
                new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.STONE, 32), new ItemStack(Items.CRAFTING_TABLE),
                new ItemStack(ModItems.ULTIMATE_CRYSTAL.get()));
    }

    /** Joueur nouveau : aucune minute de jeu et aucune trace des anciennes versions du mod. */
    static boolean isNewPlayer(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound("PlayerPersisted");
        if (persisted.getBoolean(LEGACY_TAG) || persisted.getBoolean(OLD_TAG)) {
            return false;
        }
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) < 20 * 60;
    }

    public static void onLogin(ServerPlayer player) {
        if (!ServerConfig.loaded()) {
            return;
        }
        AdventureData data = AdventureData.get(player.server);
        AdventureData.PlayerState ps = data.player(player.getUUID());
        if (ps.kitGiven) {
            return;
        }
        boolean fresh = isNewPlayer(player);
        ps.kitGiven = true;
        // Un joueur existant n'a pas de maison de départ imposée.
        ps.starterHouseDone = !fresh || !ServerConfig.STARTER_HOUSE.get();
        data.setDirty();
        if (fresh && ServerConfig.STARTER_KIT.get()) {
            CompoundTag persisted = player.getPersistentData().getCompound("PlayerPersisted");
            persisted.putBoolean(LEGACY_TAG, true);
            player.getPersistentData().put("PlayerPersisted", persisted);
            Rewards.give(player, kit(), Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".kit_title"));
            player.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".kit_given")
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }

    /** Maisons de départ en attente (un essai toutes les 2 secondes, quelques essais au plus). */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 20 || StructurePlacer.busy() || server instanceof net.minecraft.gametest.framework.GameTestServer) {
            return;
        }
        AdventureData data = AdventureData.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            AdventureData.PlayerState ps = data.player(p.getUUID());
            if (!ps.kitGiven || ps.starterHouseDone || p.level().dimension() != Level.OVERWORLD || !p.onGround()) {
                continue;
            }
            ps.starterAttempts++;
            if (tryPlace((ServerLevel) p.level(), data, p) || ps.starterAttempts >= MAX_ATTEMPTS) {
                if (ps.starterAttempts >= MAX_ATTEMPTS && !ps.starterHouseDone) {
                    p.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".starter_no_space")
                            .withStyle(ChatFormatting.GRAY), false);
                }
                ps.starterHouseDone = true;
            }
            data.setDirty();
            return;
        }
    }

    private static boolean tryPlace(ServerLevel level, AdventureData data, ServerPlayer p) {
        Blueprint bp = AdventureGen.build(StructureType.STARTER_HOUSE, p.getUUID().getMostSignificantBits());
        int[] fp = bp.footprint();
        // Recherche en anneaux autour du joueur ; la porte (+Z du plan) est tournée vers lui.
        for (int dist = 10; dist <= 26; dist += 4) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                int x = (int) Math.floor(p.getX() + Math.cos(a) * dist), z = (int) Math.floor(p.getZ() + Math.sin(a) * dist);
                int rot = rotationFacing(p.getX() - x, p.getZ() - z);
                int[] origin = StructureSites.validate(level, data, null, bp, x, z, rot);
                if (origin == null || data.siteWithin(x, z, 24) || onPlayer(fp, rot, x, z, p)) {
                    continue;
                }
                AdventureData.Site s = data.addSite("start:" + p.getUUID());
                s.type = StructureType.STARTER_HOUSE;
                s.x = x;
                s.y = origin[1];
                s.z = z;
                s.rotation = rot;
                s.state = AdventureData.SiteState.PLACING;
                data.setDirty();
                StructurePlacer.enqueue(level, bp, x, origin[1], z, rot, box -> {
                    data.markPlaced(s, box);
                    p.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".starter_house_ready", x, z)
                            .withStyle(ChatFormatting.GREEN), false);
                });
                return true;
            }
        }
        return false;
    }

    /** Rotation qui tourne la façade (+Z du plan) vers la direction (dx, dz). */
    static int rotationFacing(double dx, double dz) {
        if (Math.abs(dz) >= Math.abs(dx)) {
            return dz >= 0 ? 0 : 2;
        }
        // Rotation horaire d'un quart : +Z devient -X.
        return dx < 0 ? 1 : 3;
    }

    /** Vrai si l'emprise (avec une marge de 3 blocs) recouvre le joueur. */
    private static boolean onPlayer(int[] fp, int rot, int x, int z, ServerPlayer p) {
        for (int dx = fp[0] - 3; dx <= fp[2] + 3; dx++) {
            for (int dz = fp[1] - 3; dz <= fp[3] + 3; dz++) {
                long w = com.ultimateautobuilder.core.placement.Placement.rotateXZ(dx, dz, rot, false);
                if (x + (int) (w >> 32) == p.getBlockX() && z + (int) w == p.getBlockZ()) {
                    return true;
                }
            }
        }
        return false;
    }
}
