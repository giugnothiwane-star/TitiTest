package com.ultimateautobuilder.client;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.client.gui.SettingsScreen;
import com.ultimateautobuilder.client.render.PreviewRenderer;
import com.ultimateautobuilder.config.ClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Point d'entrée exclusivement client : touches, manette, interface, aperçu et barre de progression. */
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
            PlacementController.stop();
            PreviewRenderer.reset();
        });
        ClientData.load();
    }
}
