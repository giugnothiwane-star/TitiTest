package com.ultimateautobuilder;

import com.mojang.logging.LogUtils;
import com.ultimateautobuilder.builder.BuildManager;
import com.ultimateautobuilder.command.UabCommand;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.network.NetworkHandler;
import com.ultimateautobuilder.permission.Permissions;
import com.ultimateautobuilder.registry.ModCreativeTabs;
import com.ultimateautobuilder.registry.ModItems;
import com.ultimateautobuilder.storage.CustomModelStore;
import com.ultimateautobuilder.storage.PlayerJoinHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;

/**
 * Point d'entrée commun (client et serveur). Toute modification du monde est réalisée côté
 * serveur par {@link BuildManager} ; le client ne fait qu'envoyer des demandes validées par le serveur.
 */
@Mod(UltimateAutoBuilderMod.MOD_ID)
public final class UltimateAutoBuilderMod {
    public static final String MOD_ID = "ultimateautobuilder";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltimateAutoBuilderMod(IEventBus modBus, ModContainer container) {
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(NetworkHandler::register);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> UabCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((PermissionGatherEvent.Nodes e) -> Permissions.registerNodes(e));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> BuildManager.onServerStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> BuildManager.onServerStopping(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> BuildManager.onServerTickPre(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> BuildManager.onServerTickPost(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> PlayerJoinHandler.onLogin(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            PlayerJoinHandler.onLogout(e.getEntity());
            com.ultimateautobuilder.network.ModelUploads.forget(e.getEntity().getUUID());
        });

        // Le catalogue est purement calculatoire : son initialisation précoce évite un à-coup à la première ouverture.
        int count = DefaultCatalog.shared().size();
        LOGGER.info("Ultimate Auto Builder : {} modèles intégrés", count);
        CustomModelStore.bootstrap();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
