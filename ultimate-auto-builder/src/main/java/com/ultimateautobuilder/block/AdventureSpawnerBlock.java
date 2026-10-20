package com.ultimateautobuilder.block;

import com.mojang.serialization.MapCodec;
import com.ultimateautobuilder.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Spawner de château : catégorie (bébé, moyen, normal), famille de créatures et autorisation
 * du système Warden sont portées par l'état du bloc ; le cycle est géré par son entité de bloc.
 */
public class AdventureSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<AdventureSpawnerBlock> CODEC = simpleCodec(AdventureSpawnerBlock::new);

    public AdventureSpawnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(Props.CATEGORY, Props.Category.NORMAL).setValue(Props.FAMILY, Props.Family.MIXED)
                .setValue(Props.WARDEN, false).setValue(Props.DEPLETED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Props.CATEGORY, Props.FAMILY, Props.WARDEN, Props.DEPLETED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdventureSpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(Props.DEPLETED)) {
            return null;
        }
        return createTickerHelper(type, ModBlocks.SPAWNER_ENTITY.get(), AdventureSpawnerBlockEntity::serverTick);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (dropExperience) {
            popExperience(level, pos, 15 + level.random.nextInt(15) + level.random.nextInt(15));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Flammes discrètes tant que le spawner est actif, comme un spawner vanilla (client, blocs proches seulement).
        if (!state.getValue(Props.DEPLETED) && random.nextInt(3) == 0) {
            double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
            level.addParticle(state.getValue(Props.WARDEN) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }
}
