package com.ultimateautobuilder.block;

import com.mojang.serialization.MapCodec;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.adventure.Locks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Coffre verrouillé : incassable, il ne s'ouvre qu'avec une clé de sa rareté. À l'ouverture, il
 * devient un coffre ordinaire dont le butin (table « locked_rareté ») est tiré une seule fois.
 */
public class LockedChestBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<LockedChestBlock> CODEC = simpleCodec(LockedChestBlock::new);

    public LockedChestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Props.TIER, Props.Tier.COMMON));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, Props.TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        Locks.openChest(level, pos, state, player, hand, stack);
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
