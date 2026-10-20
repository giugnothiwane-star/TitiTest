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

    /** Clic droit avec le Cristal de Création Visuelle. */
    public static void onVisualCrystalUsed(InteractionHand hand) {
        if (ClientConfig.loaded() && !ClientConfig.RIGHT_CLICK_OPENS.get()) {
            return;
        }
        if (hand == InteractionHand.OFF_HAND && ClientConfig.loaded() && ClientConfig.CRYSTAL_HAND.get() == ClientConfig.CrystalHand.MAIN_HAND) {
            return;
        }
        openStudio();
    }

    /** Vrai si le Cristal de Création Visuelle est tenu dans une main autorisée. */
    public static boolean holdingVisualCrystal(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        if (isVisualCrystal(player.getMainHandItem())) {
            return true;
        }
        boolean offhandAllowed = ClientConfig.loaded() && ClientConfig.CRYSTAL_HAND.get() == ClientConfig.CrystalHand.ANY_HAND;
        return offhandAllowed && isVisualCrystal(player.getOffhandItem());
    }

    /** L'Ultimate Crystal donne aussi accès au studio (touche V). */
    private static boolean isVisualCrystal(net.minecraft.world.item.ItemStack s) {
        return s.is(ModItems.VISUAL_CRYSTAL.get()) || s.is(ModItems.ULTIMATE_CRYSTAL.get());
    }

    private static boolean isMenuCrystal(net.minecraft.world.item.ItemStack s) {
        return s.is(ModItems.ICE_CRYSTAL.get()) || s.is(ModItems.ULTIMATE_CRYSTAL.get());
    }

    public static void openStudio() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        PlacementController.stop();
        mc.setScreen(new com.ultimateautobuilder.client.visual.VisualStudioScreen(null));
    }

    public static boolean crystalParticlesEnabled() {
        return !ClientConfig.loaded() || ClientConfig.CRYSTAL_PARTICLES.get();
    }

    /** Vrai si le cristal est tenu dans une main autorisée par la configuration. */
    public static boolean holdingCrystal(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        if (isMenuCrystal(player.getMainHandItem())) {
            return true;
        }
        boolean offhandAllowed = ClientConfig.loaded() && ClientConfig.CRYSTAL_HAND.get() == ClientConfig.CrystalHand.ANY_HAND;
        return offhandAllowed && isMenuCrystal(player.getOffhandItem());
    }

    public static void openMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        PlacementController.stop();
        mc.setScreen(com.ultimateautobuilder.client.gui.HubScreen.openedFromCrystal());
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

    public static void onAdventure(com.ultimateautobuilder.network.AdventureStatePayload payload) {
        ClientAdventure.set(payload);
    }

    public static void onRewardFx(com.ultimateautobuilder.network.RewardFxPayload payload) {
        ClientAdventure.onRewardFx(payload);
    }

    /** Clic droit avec une carte au trésor. */
    public static void openTreasureMap(net.minecraft.world.item.ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
            mc.setScreen(new com.ultimateautobuilder.client.gui.TreasureMapScreen(stack.copy()));
        }
    }

    public static void onCustomPreview(SimplePayloads.CustomPreview payload) {
        ClientState.putCustomPreview(payload.id(), payload.data());
        ThumbnailCache.invalidate(payload.id());
        com.ultimateautobuilder.client.render.ModelPreviews.invalidate(payload.id());
    }
}
