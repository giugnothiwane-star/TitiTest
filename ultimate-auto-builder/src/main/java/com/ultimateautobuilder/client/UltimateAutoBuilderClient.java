package com.ultimateautobuilder.client;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.client.gui.SettingsScreen;
import com.ultimateautobuilder.client.render.PreviewRenderer;
import com.ultimateautobuilder.client.visual.AtmosphereHooks;
import com.ultimateautobuilder.client.visual.FrameStats;
import com.ultimateautobuilder.client.visual.PostEffects;
import com.ultimateautobuilder.client.visual.TexturePack;
import com.ultimateautobuilder.client.visual.VisualStore;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import com.ultimateautobuilder.config.ClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Point d'entrée exclusivement client : touches, manette, interface, aperçu, barre de progression et
 * studio visuel (post-traitement, atmosphère, couleurs de végétation, pack de textures, mesures).
 */
@Mod(value = UltimateAutoBuilderMod.MOD_ID, dist = Dist.CLIENT)
public final class UltimateAutoBuilderClient {
    public UltimateAutoBuilderClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SettingsScreen(parent));
        modBus.addListener((RegisterKeyMappingsEvent e) -> KeyBindings.register(e));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientTicker.onClientTick());
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> PreviewRenderer.onRenderLevel(e));
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) -> ProgressHud.render(e.getGuiGraphics()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientState.reset();
            ClientProgress.reset();
            PlacementController.stop();
            PreviewRenderer.reset();
            FrameStats.cancelBenchmark();
        });
        ClientData.load();

        // Studio visuel
        VisualStore.load();
        TexturePack.ensureSkeleton();
        modBus.addListener((RegisterColorHandlersEvent.Block e) -> AtmosphereHooks.registerBlockColors(e));
        modBus.addListener((AddPackFindersEvent e) -> TexturePack.addPackFinder(e));
        modBus.addListener((RegisterClientReloadListenersEvent e) ->
                e.registerReloadListener((ResourceManagerReloadListener) manager -> {
                    PostEffects.invalidate();
                    // Les maillages référencent l'ancien atlas de textures.
                    com.ultimateautobuilder.client.render.ModelPreviews.clearAll();
                    com.ultimateautobuilder.client.render.PreviewRenderer.reset();
                }));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> PostEffects.onRenderLevel(e));
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFogColor e) -> AtmosphereHooks.onFogColor(e));
        NeoForge.EVENT_BUS.addListener((ViewportEvent.RenderFog e) -> AtmosphereHooks.onRenderFog(e));
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e) -> FrameStats.onFrame());
    }
}
