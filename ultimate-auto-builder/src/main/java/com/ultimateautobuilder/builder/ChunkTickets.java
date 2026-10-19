package com.ultimateautobuilder.builder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Tickets de chargement temporaires : les chunks d'une construction restent chargés pendant la
 * pose sans être « forcés » durablement (les tickets ne sont pas sauvegardés avec le monde,
 * aucun chunk ne reste chargé après un arrêt brutal).
 */
public final class ChunkTickets {
    public static final TicketType<ChunkPos> TICKET = TicketType.create("ultimateautobuilder", Comparator.comparingLong(ChunkPos::toLong));

    private final Map<Integer, long[]> held = new HashMap<>();

    public void acquire(ServerLevel level, int jobId, long[] chunks) {
        release(level, jobId);
        for (long c : chunks) {
            ChunkPos pos = new ChunkPos(c);
            level.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
        }
        held.put(jobId, chunks.clone());
    }

    public void release(ServerLevel level, int jobId) {
        long[] chunks = held.remove(jobId);
        if (chunks == null || level == null) {
            return;
        }
        for (long c : chunks) {
            ChunkPos pos = new ChunkPos(c);
            level.getChunkSource().removeRegionTicket(TICKET, pos, 0, pos);
        }
    }

    public int heldChunks() {
        return held.values().stream().mapToInt(a -> a.length).sum();
    }
}
