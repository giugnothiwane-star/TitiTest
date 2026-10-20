package com.ultimateautobuilder.registry;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.block.AdventureSpawnerBlock;
import com.ultimateautobuilder.block.AdventureSpawnerBlockEntity;
import com.ultimateautobuilder.block.LockedChestBlock;
import com.ultimateautobuilder.block.VaultDoorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocs d'aventure : coffre verrouillé, porte de coffre-fort, spawner de château. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UltimateAutoBuilderMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,
            UltimateAutoBuilderMod.MOD_ID);

    /** Incassables (comme la bedrock) : la seule ouverture passe par la bonne clé. */
    public static final DeferredBlock<LockedChestBlock> LOCKED_CHEST = BLOCKS.register("locked_chest",
            () -> new LockedChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0F, 3_600_000.0F).noLootTable()
                    .sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<VaultDoorBlock> VAULT_DOOR = BLOCKS.register("vault_door",
            () -> new VaultDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3_600_000.0F).noLootTable()
                    .sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<AdventureSpawnerBlock> ADVENTURE_SPAWNER = BLOCKS.register("adventure_spawner",
            () -> new AdventureSpawnerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(5.0F)
                    .sound(SoundType.METAL).noOcclusion().noLootTable()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdventureSpawnerBlockEntity>> SPAWNER_ENTITY =
            BLOCK_ENTITIES.register("adventure_spawner", () -> BlockEntityType.Builder.of(AdventureSpawnerBlockEntity::new,
                    ADVENTURE_SPAWNER.get()).build(null));

    private ModBlocks() {
    }
}
