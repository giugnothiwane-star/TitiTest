package com.ultimateautobuilder.builder.undo;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Journal d'annulation : pour chaque position modifiée, l'état remplacé, l'état posé et les
 * données éventuelles de l'entité de bloc remplacée (coffres, panneaux…). Les états sont
 * dédupliqués dans une palette pour rester compacts.
 */
public final class UndoRecord {
    private final LongArrayList positions = new LongArrayList();
    private final IntArrayList oldStates = new IntArrayList();
    private final IntArrayList newStates = new IntArrayList();
    private final List<BlockState> palette = new ArrayList<>();
    private final Object2IntOpenHashMap<BlockState> paletteIndex = new Object2IntOpenHashMap<>();
    private final Int2ObjectOpenHashMap<CompoundTag> blockEntities = new Int2ObjectOpenHashMap<>();

    public UndoRecord() {
        paletteIndex.defaultReturnValue(-1);
    }

    private int idx(BlockState s) {
        int i = paletteIndex.getInt(s);
        if (i < 0) {
            i = palette.size();
            palette.add(s);
            paletteIndex.put(s, i);
        }
        return i;
    }

    public synchronized void record(BlockPos pos, BlockState old, BlockState placed, CompoundTag blockEntity) {
        int entry = positions.size();
        positions.add(pos.asLong());
        oldStates.add(idx(old));
        newStates.add(idx(placed));
        if (blockEntity != null) {
            blockEntities.put(entry, blockEntity);
        }
    }

    public synchronized int size() {
        return positions.size();
    }

    public synchronized long position(int i) {
        return positions.getLong(i);
    }

    public synchronized BlockState oldState(int i) {
        return palette.get(oldStates.getInt(i));
    }

    public synchronized BlockState placedState(int i) {
        return palette.get(newStates.getInt(i));
    }

    public synchronized CompoundTag blockEntity(int i) {
        return blockEntities.get(i);
    }

    /** Copie sérialisable (instantané pris sur le thread serveur, écrit ensuite hors thread). */
    public synchronized CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.put("pos", new LongArrayTag(positions.toLongArray()));
        t.put("old", new IntArrayTag(oldStates.toIntArray()));
        t.put("new", new IntArrayTag(newStates.toIntArray()));
        ListTag pal = new ListTag();
        for (BlockState s : palette) {
            pal.add(NbtUtils.writeBlockState(s));
        }
        t.put("palette", pal);
        ListTag bes = new ListTag();
        for (Int2ObjectMap.Entry<CompoundTag> e : blockEntities.int2ObjectEntrySet()) {
            CompoundTag be = new CompoundTag();
            be.putInt("i", e.getIntKey());
            be.put("tag", e.getValue().copy());
            bes.add(be);
        }
        t.put("be", bes);
        return t;
    }

    public static UndoRecord load(CompoundTag t, HolderGetter<Block> blocks) {
        UndoRecord r = new UndoRecord();
        ListTag pal = t.getList("palette", Tag.TAG_COMPOUND);
        for (int i = 0; i < pal.size(); i++) {
            BlockState s = NbtUtils.readBlockState(blocks, pal.getCompound(i));
            r.palette.add(s);
            r.paletteIndex.put(s, i);
        }
        long[] pos = t.getLongArray("pos");
        int[] old = t.getIntArray("old");
        int[] nw = t.getIntArray("new");
        int n = Math.min(pos.length, Math.min(old.length, nw.length));
        for (int i = 0; i < n; i++) {
            if (old[i] < 0 || old[i] >= r.palette.size() || nw[i] < 0 || nw[i] >= r.palette.size()) {
                continue;
            }
            r.positions.add(pos[i]);
            r.oldStates.add(old[i]);
            r.newStates.add(nw[i]);
        }
        ListTag bes = t.getList("be", Tag.TAG_COMPOUND);
        for (int i = 0; i < bes.size(); i++) {
            CompoundTag be = bes.getCompound(i);
            int idx = be.getInt("i");
            if (idx >= 0 && idx < r.positions.size()) {
                r.blockEntities.put(idx, be.getCompound("tag"));
            }
        }
        return r;
    }
}
