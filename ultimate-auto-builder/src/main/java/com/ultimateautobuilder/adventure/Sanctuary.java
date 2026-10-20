package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.adventure.StructureType;
import com.ultimateautobuilder.core.util.Rng;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sanctuaire OP : structure unique par monde, à une position déterministe (graine du monde)
 * entre 1 500 et 2 500 blocs du point d'apparition. Enregistré une fois comme site ; il est
 * construit quand ses chunks sont chargés, jamais dupliqué.
 */
public final class Sanctuary {
    public static final String SITE_KEY = "sanctuary";

    private Sanctuary() {
    }

    public static BlockPos position(MinecraftServer server) {
        Rng r = new Rng(server.overworld().getSeed() ^ 0x5A4C7A11L);
        double angle = r.nextDouble() * Math.PI * 2;
        double radius = 1500 + r.nextDouble() * 1000;
        BlockPos spawn = server.overworld().getSharedSpawnPos();
        return new BlockPos(spawn.getX() + (int) (Math.cos(angle) * radius), 0, spawn.getZ() + (int) (Math.sin(angle) * radius));
    }

    /** Enregistre le site au démarrage du serveur s'il n'existe pas encore. */
    public static void ensureSite(MinecraftServer server) {
        AdventureData data = AdventureData.get(server);
        if (data.site(SITE_KEY) != null) {
            return;
        }
        BlockPos p = position(server);
        AdventureData.Site s = data.addSite(SITE_KEY);
        s.type = StructureType.OP_SANCTUARY;
        s.x = p.getX();
        s.z = p.getZ();
        s.seed = server.overworld().getSeed() ^ 0x0F0F1234L;
        data.setDirty();
    }

    public static void showDirection(ServerPlayer player) {
        AdventureData data = AdventureData.get(player.server);
        AdventureData.Site s = data.site(SITE_KEY);
        if (s == null) {
            return;
        }
        data.player(player.getUUID()).knowsSanctuary = true;
        data.setDirty();
        double dx = s.x - player.getX(), dz = s.z - player.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        String ns = UltimateAutoBuilderMod.MOD_ID;
        player.displayClientMessage(Component.translatable("message." + ns + ".sanctuary_direction", dist,
                Component.translatable("direction." + ns + "." + cardinal(dx, dz)), s.x, s.z,
                Component.translatable(s.state == AdventureData.SiteState.PLACED ? "message." + ns + ".sanctuary_built"
                        : "message." + ns + ".sanctuary_waiting")).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    /** Point cardinal (n, ne, e, se, s, so, o, no) de la direction (dx, dz) ; +Z = sud. */
    public static String cardinal(double dx, double dz) {
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        if (deg < 0) {
            deg += 360;
        }
        String[] names = {"n", "ne", "e", "se", "s", "so", "o", "no"};
        return names[(int) Math.round(deg / 45.0) % 8];
    }
}
