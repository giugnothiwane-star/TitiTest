package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.block.Props;
import com.ultimateautobuilder.core.adventure.KeyTier;
import com.ultimateautobuilder.item.KeyItem;
import com.ultimateautobuilder.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Ouverture des serrures, côté serveur uniquement. La clé n'est retirée qu'après la
 * transformation réussie du bloc ; la serrure disparaît dans le même tick, un second clic (ou
 * un paquet répété) ne trouve donc plus rien à ouvrir et ne consomme rien.
 */
public final class Locks {
    private static final int MAX_DOOR_BLOCKS = 12;

    private Locks() {
    }

    private static String ns() {
        return UltimateAutoBuilderMod.MOD_ID;
    }

    /** Vérifie la clé ; renvoie la rareté de la serrure si la clé correspond, sinon informe le joueur. */
    private static boolean check(Player player, ItemStack stack, KeyTier lock) {
        Component lockName = Component.translatable("key_tier." + ns() + "." + lock.key);
        if (!(stack.getItem() instanceof KeyItem key)) {
            player.displayClientMessage(Component.translatable("message." + ns() + ".locked", lockName).withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        if (!key.tier().opens(lock)) {
            player.displayClientMessage(Component.translatable("message." + ns() + ".wrong_key", lockName).withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    public static void openChest(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack) {
        if (!state.is(ModBlocks.LOCKED_CHEST.get())) {
            return;
        }
        KeyTier tier = state.getValue(Props.TIER).tier;
        if (!check(player, stack, tier)) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).setValue(ChestBlock.TYPE, ChestType.SINGLE);
        if (!level.setBlock(pos, chest, 3) || !(level.getBlockEntity(pos) instanceof ChestBlockEntity be)) {
            player.displayClientMessage(Component.translatable("message." + ns() + ".open_failed").withStyle(ChatFormatting.RED), true);
            return;
        }
        // Graine stable : le butin d'une serrure ne dépend que du monde et de sa position.
        long seed = ((ServerLevel) level).getSeed() ^ pos.asLong() * 0x9E3779B97F4A7C15L;
        be.setLootTable(Rewards.lootKey(tier.lockedLootTable()), seed);
        be.setChanged();
        consume(player, stack);
        feedback((ServerLevel) level, pos, player, "message." + ns() + ".opened");
        player.openMenu(be);
    }

    public static void openDoor(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack) {
        if (!state.is(ModBlocks.VAULT_DOOR.get())) {
            return;
        }
        Props.Tier tierProp = state.getValue(Props.TIER);
        if (!check(player, stack, tierProp.tier)) {
            return;
        }
        // Tous les blocs voisins de la même porte (même rareté) s'ouvrent ensemble.
        Set<BlockPos> door = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(pos);
        while (!q.isEmpty() && door.size() < MAX_DOOR_BLOCKS) {
            BlockPos p = q.poll();
            if (!door.add(p)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                BlockState s = level.getBlockState(n);
                if (!door.contains(n) && s.is(ModBlocks.VAULT_DOOR.get()) && s.getValue(Props.TIER) == tierProp) {
                    q.add(n);
                }
            }
        }
        for (BlockPos p : door) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        consume(player, stack);
        feedback((ServerLevel) level, pos, player, "message." + ns() + ".door_opened");
    }

    private static void feedback(ServerLevel level, BlockPos pos, Player player, String key) {
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.1F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GREEN), true);
    }
}
