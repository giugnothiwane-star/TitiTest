package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.adventure.LootTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;

/**
 * Cartes au trésor. La position est tirée une seule fois (à la première prise en main), écrite
 * dans la carte et dans les données du monde ; elle ne change plus jamais. Le coffre enterré est
 * posé dès que le chunk de la croix est chargé, avec une croix de terre stérile en surface, et
 * son butin est tiré une seule fois (table de butin vanilla, consommée à l'ouverture).
 */
public final class Treasures {
    public static final String K_TIER = "uab_treasure_tier", K_ID = "uab_treasure_id", K_X = "uab_treasure_x", K_Z = "uab_treasure_z";

    public record Target(String id, int x, int z) {
    }

    private Treasures() {
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static LootTier tier(ItemStack stack) {
        LootTier t = LootTier.byKey(tag(stack).getString(K_TIER));
        return t == null || t == LootTier.OP ? LootTier.COMMON : t;
    }

    public static boolean hasTarget(ItemStack stack) {
        return target(stack) != null;
    }

    public static Target target(ItemStack stack) {
        CompoundTag t = tag(stack);
        if (!t.contains(K_ID) || !t.contains(K_X) || !t.contains(K_Z)) {
            return null;
        }
        return new Target(t.getString(K_ID), t.getInt(K_X), t.getInt(K_Z));
    }

    /** Fixe la cible d'une carte vierge : entre 120 et 400 blocs du joueur, dans l'Overworld. */
    public static void assign(ItemStack stack, ServerPlayer player) {
        if (hasTarget(stack) || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        LootTier tier = tier(stack);
        double a = player.getRandom().nextDouble() * Math.PI * 2;
        double r = 120 + player.getRandom().nextDouble() * 280;
        int x = (int) (player.getX() + Math.cos(a) * r), z = (int) (player.getZ() + Math.sin(a) * r);
        String id = UUID.randomUUID().toString();
        AdventureData data = AdventureData.get(player.server);
        AdventureData.Treasure t = data.addTreasure(id);
        t.x = x;
        t.z = z;
        t.tier = tier;
        data.setDirty();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, c -> {
            c.putString(K_ID, id);
            c.putInt(K_X, x);
            c.putInt(K_Z, z);
            c.putString(K_TIER, tier.key);
        });
        player.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".map_revealed")
                .withStyle(ChatFormatting.GOLD), true);
    }

    /** Pose des coffres dont le chunk est chargé (au plus deux par passage). */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0) {
            return;
        }
        ServerLevel level = server.overworld();
        AdventureData data = AdventureData.get(server);
        int placed = 0;
        for (AdventureData.Treasure t : data.treasures()) {
            if (t.state != AdventureData.TreasureState.PENDING || !level.getChunkSource().hasChunk(t.x >> 4, t.z >> 4)) {
                continue;
            }
            place(level, t);
            data.setDirty();
            if (++placed >= 2) {
                return;
            }
        }
    }

    static void place(ServerLevel level, AdventureData.Treasure t) {
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, t.x, t.z) - 1;
        BlockPos chest = new BlockPos(t.x, Math.max(level.getMinBuildHeight() + 2, surface - 2), t.z);
        // Ne jamais écraser un bloc de joueur ou indestructible : on descend jusqu'à un emplacement sûr.
        for (int i = 0; i < 6; i++) {
            BlockState s = level.getBlockState(chest);
            if (!s.hasBlockEntity() && s.getDestroySpeed(level, chest) >= 0) {
                break;
            }
            chest = chest.below();
        }
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.NORTH), Block.UPDATE_ALL);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
            be.setLootTable(Rewards.lootKey(t.tier.treasureTable()), level.getSeed() ^ chest.asLong() * 17L);
            be.setChanged();
        }
        // Croix en surface (terre stérile), visible en suivant la carte.
        BlockPos top = new BlockPos(t.x, surface, t.z);
        for (int[] o : new int[][]{{0, 0}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) {
            BlockPos p = top.offset(o[0], 0, o[1]);
            BlockState s = level.getBlockState(p);
            if (s.getFluidState().isEmpty() && !s.hasBlockEntity() && s.isSolidRender(level, p) && s.getDestroySpeed(level, p) >= 0) {
                level.setBlock(p, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        t.y = chest.getY();
        t.state = AdventureData.TreasureState.PLACED;
    }

    /** Ouverture d'un coffre : si c'est un trésor de carte, il est marqué comme trouvé. */
    public static void onContainerOpen(ServerPlayer player, BlockPos pos) {
        AdventureData data = AdventureData.get(player.server);
        for (AdventureData.Treasure t : data.treasures()) {
            if (t.state == AdventureData.TreasureState.PLACED && t.x == pos.getX() && t.z == pos.getZ() && t.y == pos.getY()) {
                t.state = AdventureData.TreasureState.FOUND;
                data.setDirty();
                player.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".treasure_found")
                        .withStyle(ChatFormatting.GOLD), false);
                player.serverLevel().playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
                return;
            }
        }
    }
}
