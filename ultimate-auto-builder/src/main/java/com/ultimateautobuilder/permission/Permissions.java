package com.ultimateautobuilder.permission;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.config.ServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Nœuds de permission NeoForge (compatibles avec les gestionnaires de permissions comme LuckPerms).
 * Par défaut : constructions réservées au niveau d'opérateur configuré, toujours permises en solo.
 */
public final class Permissions {
    public static final PermissionNode<Boolean> BUILD = new PermissionNode<>(UltimateAutoBuilderMod.MOD_ID, "build", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && defaultBuild(player));
    public static final PermissionNode<Boolean> ADMIN = new PermissionNode<>(UltimateAutoBuilderMod.MOD_ID, "admin", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && (isSingleplayerOwner(player) || player.hasPermissions(3)));
    public static final PermissionNode<Boolean> IMPORT = new PermissionNode<>(UltimateAutoBuilderMod.MOD_ID, "import", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && (isSingleplayerOwner(player) || player.hasPermissions(2)));

    private Permissions() {
    }

    public static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(BUILD, ADMIN, IMPORT);
    }

    private static boolean defaultBuild(ServerPlayer player) {
        if (ServerConfig.loaded() && ServerConfig.SINGLEPLAYER_ALWAYS_ALLOWED.get() && isSingleplayerOwner(player)) {
            return true;
        }
        int level = ServerConfig.loaded() ? ServerConfig.PERMISSION_LEVEL.get() : 2;
        return player.hasPermissions(level);
    }

    public static boolean isSingleplayerOwner(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null && server.isSingleplayerOwner(player.getGameProfile());
    }

    public static boolean canBuild(ServerPlayer player) {
        return check(player, BUILD);
    }

    public static boolean isAdmin(ServerPlayer player) {
        return check(player, ADMIN);
    }

    public static boolean canImport(ServerPlayer player) {
        return check(player, IMPORT);
    }

    private static boolean check(ServerPlayer player, PermissionNode<Boolean> node) {
        try {
            return PermissionAPI.getPermission(player, node);
        } catch (RuntimeException e) {
            // Gestionnaire de permissions indisponible (tests, démarrage) : on applique la règle par défaut.
            return node.getDefaultResolver().resolve(player, player.getUUID());
        }
    }
}
