package com.ultimateautobuilder.block;

import com.mojang.serialization.MapCodec;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.adventure.Locks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Porte de coffre-fort : bloc incassable qui disparaît (avec les blocs voisins de la même porte)
 * quand on l'ouvre avec la clé de sa rareté.
 */
public class VaultDoorBlock extends Block {
    public static final MapCodec<VaultDoorBlock> CODEC = simpleCodec(VaultDoorBlock::new);

    public VaultDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(Props.TIER, Props.Tier.COMMON));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Props.TIER);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        Locks.openDoor(level, pos, state, player, hand, stack);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".locked",
                    Component.translatable("key_tier." + UltimateAutoBuilderMod.MOD_ID + "." + state.getValue(Props.TIER).getSerializedName())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
