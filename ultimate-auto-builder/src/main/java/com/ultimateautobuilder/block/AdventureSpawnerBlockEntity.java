package com.ultimateautobuilder.block;

import com.ultimateautobuilder.adventure.MobVariants;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.adventure.SpawnCategory;
import com.ultimateautobuilder.core.adventure.SpawnRules;
import com.ultimateautobuilder.core.adventure.SpawnerCycle;
import com.ultimateautobuilder.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Cycle d'un spawner de château : une décision par seconde au plus, uniquement si un joueur se
 * trouve à moins de 16 blocs. L'état (cycles, créatures suivies) est sauvegardé avec le bloc :
 * un rechargement de chunk ou un redémarrage ne remet rien à zéro.
 */
public class AdventureSpawnerBlockEntity extends BlockEntity {
    private static final int PERIOD = 20;
    private static final double ACTIVATION_RANGE = 16;

    private SpawnerCycle cycle;
    private final List<UUID> tracked = new ArrayList<>();

    public AdventureSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SPAWNER_ENTITY.get(), pos, state);
    }

    private SpawnerCycle cycle() {
        if (cycle == null) {
            cycle = new SpawnerCycle(ServerConfig.loaded() ? ServerConfig.SPAWNER_MAX_CYCLES.get() : SpawnerCycle.DEFAULT_MAX_CYCLES);
        }
        return cycle;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AdventureSpawnerBlockEntity be) {
        // Décalage par position : les spawners ne travaillent pas tous au même tick.
        if ((level.getGameTime() + pos.asLong()) % PERIOD != 0 || !(level instanceof ServerLevel sl)) {
            return;
        }
        if (!sl.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ACTIVATION_RANGE)) {
            return;
        }
        be.step(sl, pos, state);
    }

    private int alive(ServerLevel level) {
        Iterator<UUID> it = tracked.iterator();
        while (it.hasNext()) {
            Entity e = level.getEntity(it.next());
            if (e == null || !e.isAlive()) {
                it.remove();
            }
        }
        return tracked.size();
    }

    private void step(ServerLevel level, BlockPos pos, BlockState state) {
        SpawnerCycle c = cycle();
        SpawnerCycle.Action a = c.step(PERIOD, alive(level));
        if (a == SpawnerCycle.Action.DEPLETED) {
            level.setBlock(pos, state.setValue(Props.DEPLETED, true), 3);
            setChanged();
            return;
        }
        if (a == SpawnerCycle.Action.ATTEMPT) {
            SpawnCategory category = state.getValue(Props.CATEGORY).category;
            SpawnRules.Choice choice = SpawnRules.choose(state.getValue(Props.FAMILY).family, category, state.getValue(Props.WARDEN),
                    MobVariants::supports, MobVariants.dice(level.random));
            Entity spawned = choice == null ? null : MobVariants.spawn(level, pos, category, choice);
            if (spawned != null) {
                tracked.add(spawned.getUUID());
                c.onSpawned();
            } else {
                c.onFailed();
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cycle", new IntArrayTag(cycle().save()));
        ListTag list = new ListTag();
        for (UUID id : tracked) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("Tracked", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cycle", Tag.TAG_INT_ARRAY)) {
            cycle = SpawnerCycle.load(tag.getIntArray("Cycle"));
        }
        tracked.clear();
        ListTag list = tag.getList("Tracked", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size() && i < SpawnerCycle.PER_CYCLE * 2; i++) {
            try {
                tracked.add(NbtUtils.loadUUID(list.get(i)));
            } catch (IllegalArgumentException e) {
                // UUID illisible : ignoré, le cycle reprend avec les créatures valides.
            }
        }
    }
}
