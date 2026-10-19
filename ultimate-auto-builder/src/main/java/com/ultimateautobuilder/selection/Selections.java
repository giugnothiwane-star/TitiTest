package com.ultimateautobuilder.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Zones sélectionnées par joueur (deux coins), utilisées pour l'ancrage et l'enregistrement de modèles. */
public final class Selections {
    public record Selection(BlockPos a, BlockPos b, String dimension) {
        public BlockPos min() {
            return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        }

        public BlockPos max() {
            return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        }

        public long volume() {
            BlockPos lo = min(), hi = max();
            return (long) (hi.getX() - lo.getX() + 1) * (hi.getY() - lo.getY() + 1) * (hi.getZ() - lo.getZ() + 1);
        }

        public boolean contains(int x0, int y0, int z0, int x1, int y1, int z1) {
            BlockPos lo = min(), hi = max();
            return x0 >= lo.getX() && y0 >= lo.getY() && z0 >= lo.getZ() && x1 <= hi.getX() && y1 <= hi.getY() && z1 <= hi.getZ();
        }
    }

    private static final Map<UUID, BlockPos[]> CORNERS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> NEXT = new ConcurrentHashMap<>();

    private Selections() {
    }

    /** Définit alternativement le coin 1 puis le coin 2 ; renvoie le numéro du coin défini. */
    public static int toggleCorner(ServerPlayer player, BlockPos pos) {
        int next = NEXT.getOrDefault(player.getUUID(), 1);
        setCorner(player, next, pos);
        NEXT.put(player.getUUID(), next == 1 ? 2 : 1);
        return next;
    }

    public static void setCorner(ServerPlayer player, int corner, BlockPos pos) {
        BlockPos[] c = CORNERS.computeIfAbsent(player.getUUID(), k -> new BlockPos[2]);
        String dim = player.level().dimension().location().toString();
        if (!dim.equals(DIMENSIONS.get(player.getUUID()))) {
            c[0] = null;
            c[1] = null;
            DIMENSIONS.put(player.getUUID(), dim);
        }
        c[corner == 1 ? 0 : 1] = pos.immutable();
    }

    public static Selection get(ServerPlayer player) {
        BlockPos[] c = CORNERS.get(player.getUUID());
        if (c == null || c[0] == null || c[1] == null) {
            return null;
        }
        return new Selection(c[0], c[1], DIMENSIONS.getOrDefault(player.getUUID(), ""));
    }

    public static BlockPos corner(ServerPlayer player, int corner) {
        BlockPos[] c = CORNERS.get(player.getUUID());
        return c == null ? null : c[corner == 1 ? 0 : 1];
    }

    public static void clear(UUID player) {
        CORNERS.remove(player);
        DIMENSIONS.remove(player);
        NEXT.remove(player);
    }
}
