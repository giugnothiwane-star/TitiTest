package com.ultimateautobuilder.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Touches configurables (Options → Commandes → « Ultimate Auto Builder »). Un mod de manette
 * (Controlify, Controllable…) peut aussi associer un bouton, par exemple R1, à « Ouvrir le menu ».
 */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.ultimateautobuilder";

    public static final KeyMapping OPEN_MENU = new KeyMapping("key.ultimateautobuilder.open_menu", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping OPEN_STUDIO = new KeyMapping("key.ultimateautobuilder.open_studio", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ROTATE = new KeyMapping("key.ultimateautobuilder.rotate", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping MIRROR = new KeyMapping("key.ultimateautobuilder.mirror", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping CONFIRM = new KeyMapping("key.ultimateautobuilder.confirm", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, CATEGORY);
    public static final KeyMapping CANCEL_PREVIEW = new KeyMapping("key.ultimateautobuilder.cancel_preview", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSPACE, CATEGORY);
    public static final KeyMapping MOVE_FORWARD = new KeyMapping("key.ultimateautobuilder.move_forward", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, CATEGORY);
    public static final KeyMapping MOVE_BACK = new KeyMapping("key.ultimateautobuilder.move_back", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, CATEGORY);
    public static final KeyMapping MOVE_LEFT = new KeyMapping("key.ultimateautobuilder.move_left", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, CATEGORY);
    public static final KeyMapping MOVE_RIGHT = new KeyMapping("key.ultimateautobuilder.move_right", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, CATEGORY);
    public static final KeyMapping MOVE_UP = new KeyMapping("key.ultimateautobuilder.move_up", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_UP, CATEGORY);
    public static final KeyMapping MOVE_DOWN = new KeyMapping("key.ultimateautobuilder.move_down", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, CATEGORY);
    public static final KeyMapping SIZE_UP = new KeyMapping("key.ultimateautobuilder.size_up", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);
    public static final KeyMapping SIZE_DOWN = new KeyMapping("key.ultimateautobuilder.size_down", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, CATEGORY);
    public static final KeyMapping RECENTER = new KeyMapping("key.ultimateautobuilder.recenter", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_HOME, CATEGORY);

    private KeyBindings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping k : new KeyMapping[]{OPEN_MENU, OPEN_STUDIO, ROTATE, MIRROR, CONFIRM, CANCEL_PREVIEW, MOVE_FORWARD, MOVE_BACK, MOVE_LEFT, MOVE_RIGHT,
                MOVE_UP, MOVE_DOWN, SIZE_UP, SIZE_DOWN, RECENTER}) {
            event.register(k);
        }
    }

    /** Vide les clics en attente (évite qu'un appui ancien ne se déclenche plus tard). */
    public static void drain(KeyMapping k) {
        while (k.consumeClick()) {
            // rien
        }
    }
}
