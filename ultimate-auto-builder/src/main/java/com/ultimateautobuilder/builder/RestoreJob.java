package com.ultimateautobuilder.builder;

import com.ultimateautobuilder.builder.undo.UndoRecord;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Retour arrière d'une construction : restaure, du dernier au premier, les états remplacés.
 * En mode prudent, une position modifiée depuis la construction (bloc différent de celui posé)
 * est laissée intacte et comptée comme conflit.
 */
public final class RestoreJob extends Job {
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private final int targetId;
    private final UndoRecord record;
    private final boolean prudent;
    private final long[] chunks;
    private int cursor;
    private int restored;
    private int conflicts;
    private boolean ticketsAdded;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    RestoreJob(int id, UUID owner, String ownerName, ResourceKey<Level> dimension, String title, int targetId, UndoRecord record, boolean prudent) {
        super(id, owner, ownerName, dimension, "Annulation : " + title);
        this.targetId = targetId;
        this.record = record;
        this.prudent = prudent;
        LongOpenHashSet set = new LongOpenHashSet();
        for (int i = 0; i < record.size(); i++) {
            long p = record.position(i);
            set.add(ChunkPos.asLong(BlockPos.getX(p) >> 4, BlockPos.getZ(p) >> 4));
        }
        this.chunks = set.toLongArray();
        this.state = State.LOADING;
    }

    public int targetId() {
        return targetId;
    }

    public int conflicts() {
        return conflicts;
    }

    @Override
    public int total() {
        return record.size();
    }

    @Override
    public int progress() {
        return cursor;
    }

    @Override
    public void cancel() {
        cancelRequested = true;
    }

    @Override
    public int tick(ServerLevel level, BuildManager manager, int budget, long deadline) {
        if (cancelRequested && !finished()) {
            state = State.CANCELLED;
            message = "Annulation interrompue (" + restored + " blocs restaurés)";
            return 0;
        }
        if (state == State.LOADING) {
            if (!ticketsAdded) {
                manager.chunks().acquire(level, id, chunks);
                ticketsAdded = true;
            }
            for (long c : chunks) {
                if (!level.hasChunk(ChunkPos.getX(c), ChunkPos.getZ(c))) {
                    return 0;
                }
            }
            state = State.PLACING;
        }
        if (state != State.PLACING) {
            return 0;
        }
        int done = 0;
        int n = record.size();
        while (cursor < n && done < budget) {
            if ((done & 31) == 0 && System.nanoTime() > deadline) {
                break;
            }
            int i = n - 1 - cursor;
            pos.set(record.position(i));
            if (!level.isLoaded(pos)) {
                break;
            }
            BlockState current = level.getBlockState(pos);
            BlockState placed = record.placedState(i);
            BlockState old = record.oldState(i);
            if (prudent && current.getBlock() != placed.getBlock()) {
                conflicts++;
            } else if (current != old) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    Clearable.tryClear(be);
                }
                level.setBlock(pos, old, FLAGS);
                CompoundTag tag = record.blockEntity(i);
                if (tag != null) {
                    BlockEntity restoredBe = level.getBlockEntity(pos);
                    if (restoredBe != null) {
                        restoredBe.loadWithComponents(tag, level.registryAccess());
                        restoredBe.setChanged();
                    }
                }
                restored++;
            }
            cursor++;
            done++;
        }
        if (cursor >= n) {
            state = State.DONE;
            message = "Retour arrière terminé : " + restored + " blocs restaurés" + (conflicts > 0 ? ", " + conflicts
                    + " blocs modifiés depuis la construction laissés en place" : "");
        }
        return done;
    }

    @Override
    public void finish(ServerLevel level, BuildManager manager) {
        manager.chunks().release(level, id);
        if (state == State.DONE) {
            manager.history().markUndone(targetId);
            manager.storage().deleteUndo(targetId);
        }
    }

    @Override
    public CompoundTag checkpoint() {
        // Une annulation interrompue se relance simplement : le journal reste sur disque.
        return null;
    }
}
