package com.ultimateautobuilder.builder;

import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.Arrays;

/**
 * Plan de pose préparé hors du thread serveur (calcul pur, sans accès au monde) :
 * ordre des blocs (porteurs de bas en haut, puis éléments accrochés et fluides),
 * liste des blocs dont la forme dépend des voisins, chunks concernés et colonnes de fondation.
 */
public final class BuildPlan {
    final VoxelModel model;
    final Placement placement;
    /** Indices de cellules dans l'ordre de pose. */
    final int[] order;
    /** Nombre d'entrées de la première passe (blocs porteurs). */
    final int solidCount;
    /** Cellules à recalculer après la pose (escaliers, clôtures, vitres…). */
    final int[] shapeUpdates;
    /** Chunks touchés (ChunkPos.asLong), bordure d'un chunk incluse. */
    final long[] chunks;
    /** Colonnes du dessous de la structure : x, y (bloc le plus bas), z, index de palette. */
    final int[] bottomColumns;

    private BuildPlan(VoxelModel model, Placement placement, int[] order, int solidCount, int[] shapeUpdates, long[] chunks, int[] bottomColumns) {
        this.model = model;
        this.placement = placement;
        this.order = order;
        this.solidCount = solidCount;
        this.shapeUpdates = shapeUpdates;
        this.chunks = chunks;
        this.bottomColumns = bottomColumns;
    }

    public static BuildPlan prepare(VoxelModel model, Placement placement) {
        int n = model.size();
        boolean[] late = new boolean[model.paletteSize()];
        boolean[] shape = new boolean[model.paletteSize()];
        boolean[] air = new boolean[model.paletteSize()];
        for (int i = 0; i < model.paletteSize(); i++) {
            String s = model.paletteEntry(i);
            late[i] = B.isAttachment(s) || B.isFluid(s);
            shape[i] = B.needsShapeUpdate(s);
            air[i] = B.isAir(s);
        }
        int[] order = new int[n];
        int solid = 0;
        for (int i = 0; i < n; i++) {
            if (!late[VoxelModel.stateId(model.cell(i))]) {
                solid++;
            }
        }
        int si = 0, li = solid;
        int shapeCount = 0;
        for (int i = 0; i < n; i++) {
            int st = VoxelModel.stateId(model.cell(i));
            if (late[st]) {
                order[li++] = i;
            } else {
                order[si++] = i;
            }
            if (shape[st]) {
                shapeCount++;
            }
        }
        int[] shapes = new int[shapeCount];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (shape[VoxelModel.stateId(model.cell(i))]) {
                shapes[k++] = i;
            }
        }
        // Chunks et colonnes inférieures (pour les fondations du mode terrain)
        LongOpenHashSet chunkSet = new LongOpenHashSet();
        int[] b = placement.bounds();
        for (int cx = (b[0] >> 4) - 1; cx <= (b[3] >> 4) + 1; cx++) {
            for (int cz = (b[2] >> 4) - 1; cz <= (b[5] >> 4) + 1; cz++) {
                chunkSet.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
            }
        }
        int sx = model.sizeX(), sz = model.sizeZ();
        int[] lowest = new int[sx * sz];
        int[] lowestCell = new int[sx * sz];
        Arrays.fill(lowest, Integer.MAX_VALUE);
        for (int i = 0; i < n; i++) {
            long c = model.cell(i);
            if (air[VoxelModel.stateId(c)] || late[VoxelModel.stateId(c)]) {
                continue;
            }
            int idx = VoxelModel.cz(c) * sx + VoxelModel.cx(c);
            int y = VoxelModel.cy(c);
            if (y < lowest[idx]) {
                lowest[idx] = y;
                lowestCell[idx] = i;
            }
        }
        int cols = 0;
        for (int v : lowest) {
            if (v != Integer.MAX_VALUE) {
                cols++;
            }
        }
        int[] bottom = new int[cols * 4];
        int j = 0;
        for (int idx = 0; idx < lowest.length; idx++) {
            if (lowest[idx] == Integer.MAX_VALUE) {
                continue;
            }
            long c = model.cell(lowestCell[idx]);
            bottom[j++] = placement.worldX(c);
            bottom[j++] = placement.worldY(c);
            bottom[j++] = placement.worldZ(c);
            bottom[j++] = VoxelModel.stateId(c);
        }
        return new BuildPlan(model, placement, order, solid, shapes, chunkSet.toLongArray(), bottom);
    }

    public int total() {
        return order.length;
    }
}
