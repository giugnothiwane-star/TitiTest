package com.ultimateautobuilder.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ultimateautobuilder.client.gui.BuilderScreen;
import com.ultimateautobuilder.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Logique client exécutée à chaque tick : ouverture du menu par la touche dédiée ou le bouton R1
 * (cristal tenu), relais des boutons de manette vers le menu, et commandes de l'aperçu en monde.
 */
public final class ClientTicker {
    /** Réarmé seulement quand la touche est physiquement relâchée : pas de réouverture en boucle. */
    private static boolean armed = true;

    private ClientTicker() {
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        GamepadInput.poll();
        boolean keyClicked = false;
        while (KeyBindings.OPEN_MENU.consumeClick()) {
            keyClicked = true;
        }
        if (!physicallyDown(mc, KeyBindings.OPEN_MENU)) {
            armed = true;
        }
        if (mc.player == null) {
            return;
        }
        if (mc.screen instanceof BuilderScreen screen) {
            screen.onGamepadTick();
            return;
        }
        if (mc.screen != null) {
            // Aucun déclenchement quand un autre écran (inventaire, chat, pause…) est ouvert.
            return;
        }
        boolean padOpen = GamepadInput.pressed(ClientConfig.loaded() ? ClientConfig.GAMEPAD_OPEN.get() : ClientConfig.GamepadButton.RIGHT_BUMPER);
        if ((keyClicked && armed) || padOpen) {
            boolean require = !ClientConfig.loaded() || ClientConfig.REQUIRE_CRYSTAL.get();
            if (require && !ClientHooks.holdingCrystal(mc.player)) {
                if (keyClicked) {
                    mc.player.displayClientMessage(Component.translatable("message.ultimateautobuilder.need_crystal").withStyle(ChatFormatting.AQUA), true);
                }
            } else {
                armed = false;
                ClientHooks.openMenu();
                return;
            }
        }
        PlacementController.tick(mc);
    }

    static boolean physicallyDown(Minecraft mc, KeyMapping key) {
        InputConstants.Key k = key.getKey();
        long window = mc.getWindow().getWindow();
        if (k.getType() == InputConstants.Type.KEYSYM) {
            return k.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, k.getValue());
        }
        if (k.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
        }
        return key.isDown();
    }
}
