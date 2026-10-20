package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.builder.BlockResolver;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.adventure.AdventureGen;
import com.ultimateautobuilder.core.adventure.Blueprint;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Pose progressive des structures d'aventure, sous un budget de temps par tick : aplanissement
 * du terrain naturel au-dessus du plancher, fondations jusqu'au sol, blocs porteurs puis
 * accrochés, formes des escaliers et barrières, et enfin remplissage des coffres (une table de
 * butin par coffre, tirée une seule fois à l'ouverture). Aucun bloc portant une entité de bloc
 * (coffre de joueur…) n'est jamais remplacé. La pose est idempotente : reprise sans risque.
 */
public final class StructurePlacer {
    private static final ArrayDeque<Task> QUEUE = new ArrayDeque<>();
    private static final int FOUNDATION_DEPTH = 12;

    private enum Phase { CLEAR, FOUNDATION, SOLID, ATTACH, SHAPES, MARKERS, DONE }

    public static final class Task {
        final ServerLevel level;
        final Blueprint bp;
        final int ox, oy, oz, rotation;
        final boolean clearAbove;
        final Consumer<int[]> onDone;
        final BlockState[] states;
        final int[] fp;
        Phase phase = Phase.CLEAR;
        int index;

        Task(ServerLevel level, Blueprint bp, int ox, int oy, int oz, int rotation, Consumer<int[]> onDone) {
            this.level = level;
            this.bp = bp;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.rotation = Math.floorMod(rotation, 4);
            this.clearAbove = !AdventureGen.underground(bp.type());
            this.onDone = onDone;
            this.fp = bp.footprint();
            Rotation rot = switch (this.rotation) {
                case 1 -> Rotation.CLOCKWISE_90;
                case 2 -> Rotation.CLOCKWISE_180;
                case 3 -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
            VoxelModel m = bp.model();
            states = new BlockState[m.paletteSize()];
            for (int i = 0; i < states.length; i++) {
                String s = m.paletteEntry(i);
                states[i] = B.isAir(s) ? Blocks.AIR.defaultBlockState() : BlockResolver.state(s).rotate(rot);
            }
        }

        BlockPos world(int x, int y, int z) {
            long p = Placement.rotateXZ(x, z, rotation, false);
            return new BlockPos(ox + (int) (p >> 32), oy + y, oz + (int) p);
        }

        /** Boîte englobante monde. */
        int[] box() {
            int[] b = {Integer.MAX_VALUE, oy + bp.minY(), Integer.MAX_VALUE, Integer.MIN_VALUE, oy + bp.maxY(), Integer.MIN_VALUE};
            for (int[] c : new int[][]{{fp[0], fp[1]}, {fp[2], fp[1]}, {fp[0], fp[3]}, {fp[2], fp[3]}}) {
                BlockPos w = world(c[0], 0, c[1]);
                b[0] = Math.min(b[0], w.getX());
                b[2] = Math.min(b[2], w.getZ());
                b[3] = Math.max(b[3], w.getX());
                b[5] = Math.max(b[5], w.getZ());
            }
            return b;
        }
    }

    private StructurePlacer() {
    }

    public static Task enqueue(ServerLevel level, Blueprint bp, int ox, int oy, int oz, int rotation, Consumer<int[]> onDone) {
        Task t = new Task(level, bp, ox, oy, oz, rotation, onDone);
        QUEUE.add(t);
        return t;
    }

    public static boolean busy() {
        return !QUEUE.isEmpty();
    }

    public static void clear() {
        QUEUE.clear();
    }

    /** Travail du tick : une tâche à la fois, interrompue dès que le budget est consommé. */
    public static void tick(MinecraftServer server) {
        Task t = QUEUE.peek();
        if (t == null) {
            return;
        }
        long budget = (ServerConfig.loaded() ? ServerConfig.STRUCTURE_BUDGET_MS.get() : 4) * 1_000_000L;
        long end = System.nanoTime() + budget;
        try {
            while (t.phase != Phase.DONE && System.nanoTime() < end) {
                if (!step(t)) {
                    // Chunk déchargé : la pose reprend quand il sera de nouveau chargé.
                    QUEUE.poll();
                    QUEUE.add(t);
                    return;
                }
            }
        } catch (RuntimeException e) {
            UltimateAutoBuilderMod.LOGGER.error("Pose de la structure {} interrompue", t.bp.type().key, e);
            QUEUE.poll();
            return;
        }
        if (t.phase == Phase.DONE) {
            QUEUE.poll();
            t.onDone.accept(t.box());
        }
    }

    private static boolean natural(ServerLevel level, BlockPos p, BlockState s) {
        return !s.hasBlockEntity() && s.getDestroySpeed(level, p) >= 0 && level.getBlockEntity(p) == null;
    }

    /** Position hors des limites de hauteur du monde : jamais modifiable, simplement ignorée. */
    private static boolean outside(ServerLevel level, BlockPos p) {
        return level.isOutsideBuildHeight(p);
    }

    /** Une opération ; faux si la position n'est pas chargée. */
    private static boolean step(Task t) {
        ServerLevel level = t.level;
        VoxelModel m = t.bp.model();
        int w = t.fp[2] - t.fp[0] + 1, d = t.fp[3] - t.fp[1] + 1;
        switch (t.phase) {
            case CLEAR -> {
                int h = t.bp.maxY();
                int total = w * d * h;
                if (!t.clearAbove || t.index >= total) {
                    next(t, Phase.FOUNDATION);
                    return true;
                }
                int i = t.index;
                int x = t.fp[0] + i % w, z = t.fp[1] + (i / w) % d, y = 1 + i / (w * d);
                BlockPos p = t.world(x, y, z);
                if (outside(level, p)) {
                    t.index++;
                    return true;
                }
                if (!level.isLoaded(p)) {
                    return false;
                }
                BlockState s = level.getBlockState(p);
                if (!s.isAir() && natural(level, p, s)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
                t.index++;
            }
            case FOUNDATION -> {
                if (!t.clearAbove || t.index >= m.size()) {
                    next(t, Phase.SOLID);
                    return true;
                }
                long c = m.cell(t.index);
                int x = VoxelModel.cx(c) - m.anchorX(), y = VoxelModel.cy(c) - m.anchorY(), z = VoxelModel.cz(c) - m.anchorZ();
                if (y == 0) {
                    BlockState top = t.states[VoxelModel.stateId(c)];
                    if (!top.isAir()) {
                        String id = B.idOf(m.stateOf(c));
                        BlockState fill = id.contains("grass") || id.contains("dirt") || id.contains("path") || id.contains("gravel")
                                ? Blocks.DIRT.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
                        for (int dy = 1; dy <= FOUNDATION_DEPTH; dy++) {
                            BlockPos p = t.world(x, -dy, z);
                            if (outside(level, p)) {
                                break;
                            }
                            if (!level.isLoaded(p)) {
                                return false;
                            }
                            BlockState s = level.getBlockState(p);
                            if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) || !natural(level, p, s)) {
                                break;
                            }
                            level.setBlock(p, fill, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        }
                    }
                }
                t.index++;
            }
            case SOLID, ATTACH -> {
                if (t.index >= m.size()) {
                    next(t, t.phase == Phase.SOLID ? Phase.ATTACH : Phase.SHAPES);
                    return true;
                }
                long c = m.cell(t.index);
                String raw = m.stateOf(c);
                boolean attach = B.isAttachment(raw);
                if (attach == (t.phase == Phase.ATTACH)) {
                    BlockPos p = t.world(VoxelModel.cx(c) - m.anchorX(), VoxelModel.cy(c) - m.anchorY(), VoxelModel.cz(c) - m.anchorZ());
                    if (outside(level, p)) {
                        t.index++;
                        return true;
                    }
                    if (!level.isLoaded(p)) {
                        return false;
                    }
                    BlockState cur = level.getBlockState(p);
                    BlockState target = t.states[VoxelModel.stateId(c)];
                    if (cur != target && natural(level, p, cur)) {
                        level.setBlock(p, target, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
                t.index++;
            }
            case SHAPES -> {
                if (t.index >= m.size()) {
                    next(t, Phase.MARKERS);
                    return true;
                }
                long c = m.cell(t.index);
                if (B.needsShapeUpdate(m.stateOf(c))) {
                    BlockPos p = t.world(VoxelModel.cx(c) - m.anchorX(), VoxelModel.cy(c) - m.anchorY(), VoxelModel.cz(c) - m.anchorZ());
                    if (outside(level, p)) {
                        t.index++;
                        return true;
                    }
                    if (!level.isLoaded(p)) {
                        return false;
                    }
                    BlockState s = level.getBlockState(p);
                    BlockState u = Block.updateFromNeighbourShapes(s, level, p);
                    if (u != s) {
                        level.setBlock(p, u, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
                t.index++;
            }
            case MARKERS -> {
                if (t.index >= t.bp.markers().size()) {
                    next(t, Phase.DONE);
                    return true;
                }
                Blueprint.Marker mk = t.bp.markers().get(t.index);
                BlockPos p = t.world(mk.x(), mk.y(), mk.z());
                if (outside(level, p)) {
                    t.index++;
                    return true;
                }
                if (!level.isLoaded(p)) {
                    return false;
                }
                if ("loot".equals(mk.kind()) && level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity be) {
                    be.setLootTable(Rewards.lootKey(mk.data()), level.getSeed() ^ p.asLong() * 31L);
                    be.setChanged();
                }
                t.index++;
            }
            default -> {
            }
        }
        return true;
    }

    private static void next(Task t, Phase p) {
        t.phase = p;
        t.index = 0;
    }
}
