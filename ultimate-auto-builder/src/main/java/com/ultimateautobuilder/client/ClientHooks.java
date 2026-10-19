package com.ultimateautobuilder.client;

import com.ultimateautobuilder.client.gui.BuilderScreen;
import com.ultimateautobuilder.config.ClientConfig;
import com.ultimateautobuilder.network.ActionPayload;
import com.ultimateautobuilder.network.JobProgressPayload;
import com.ultimateautobuilder.network.SimplePayloads;
import com.ultimateautobuilder.network.StatePayload;
import com.ultimateautobuilder.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Points d'entrée client appelés depuis le code commun. Cette classe n'est chargée que sur un client
 * (les appels sont tous protégés par une vérification de côté).
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    /** Clic droit avec le cristal. */
    public static void onCrystalUsed(InteractionHand hand) {
        if (ClientConfig.loaded() && !ClientConfig.RIGHT_CLICK_OPENS.get()) {
            return;
        }
        if (hand == InteractionHand.OFF_HAND && ClientConfig.loaded() && ClientConfig.CRYSTAL_HAND.get() == ClientConfig.CrystalHand.MAIN_HAND) {
            return;
        }
        openMenu();
    }

    public static boolean crystalParticlesEnabled() {
        return !ClientConfig.loaded() || ClientConfig.CRYSTAL_PARTICLES.get();
    }

    /** Vrai si le cristal est tenu dans une main autorisée par la configuration. */
    public static boolean holdingCrystal(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.getMainHandItem().is(ModItems.ICE_CRYSTAL.get())) {
            return true;
        }
        boolean offhandAllowed = ClientConfig.loaded() && ClientConfig.CRYSTAL_HAND.get() == ClientConfig.CrystalHand.ANY_HAND;
        return offhandAllowed && player.getOffhandItem().is(ModItems.ICE_CRYSTAL.get());
    }

    public static void openMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        PlacementController.stop();
        PacketDistributor.sendToServer(new ActionPayload(ActionPayload.REQUEST_STATE, 0, ""));
        mc.setScreen(new BuilderScreen());
    }

    public static void openMenuFromServer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !(mc.screen instanceof BuilderScreen)) {
            mc.setScreen(new BuilderScreen());
        }
    }

    public static void onState(StatePayload payload) {
        ClientState.setState(payload);
    }

    public static void onProgress(JobProgressPayload payload) {
        ClientState.onProgress(payload);
    }

    public static void onCustomPreview(SimplePayloads.CustomPreview payload) {
        ClientState.putCustomPreview(payload.id(), payload.data());
        ThumbnailCache.invalidate(payload.id());
    }
}
