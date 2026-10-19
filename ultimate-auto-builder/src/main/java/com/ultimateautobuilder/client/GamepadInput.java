package com.ultimateautobuilder.client;

import com.ultimateautobuilder.config.ClientConfig;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

/**
 * Lecture directe des manettes via l'API « gamepad » de GLFW (incluse dans Minecraft) : les
 * manettes PlayStation et Xbox reconnues par la base de correspondances SDL de GLFW exposent
 * R1 sous GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER. Seuls les fronts montants sont signalés, donc un
 * bouton maintenu ne déclenche qu'une seule action.
 */
public final class GamepadInput {
    private static final int BUTTONS = GLFW.GLFW_GAMEPAD_BUTTON_LAST + 1;
    private static final GLFWGamepadState STATE = GLFWGamepadState.create();
    private static final boolean[] down = new boolean[BUTTONS];
    private static final boolean[] pressed = new boolean[BUTTONS];
    private static final boolean[] axisDown = new boolean[4];
    private static final boolean[] axisPressed = new boolean[4]; // haut, droite, bas, gauche (stick gauche)
    private static int activeJoystick = -1;
    private static boolean available;

    private GamepadInput() {
    }

    /** À appeler une fois par tick client (thread principal). */
    public static void poll() {
        java.util.Arrays.fill(pressed, false);
        java.util.Arrays.fill(axisPressed, false);
        if (ClientConfig.loaded() && !ClientConfig.GAMEPAD_ENABLED.get()) {
            available = false;
            return;
        }
        int jid = findGamepad();
        available = jid >= 0;
        if (!available) {
            java.util.Arrays.fill(down, false);
            java.util.Arrays.fill(axisDown, false);
            return;
        }
        if (!GLFW.glfwGetGamepadState(jid, STATE)) {
            available = false;
            return;
        }
        for (int b = 0; b < BUTTONS; b++) {
            boolean now = STATE.buttons(b) == GLFW.GLFW_PRESS;
            pressed[b] = now && !down[b];
            down[b] = now;
        }
        float lx = STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X);
        float ly = STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
        boolean[] now = {ly < -0.6f, lx > 0.6f, ly > 0.6f, lx < -0.6f};
        for (int i = 0; i < 4; i++) {
            axisPressed[i] = now[i] && !axisDown[i];
            axisDown[i] = now[i];
        }
    }

    private static int findGamepad() {
        if (activeJoystick >= 0 && GLFW.glfwJoystickPresent(activeJoystick) && GLFW.glfwJoystickIsGamepad(activeJoystick)) {
            return activeJoystick;
        }
        activeJoystick = -1;
        for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST; jid++) {
            if (GLFW.glfwJoystickPresent(jid) && GLFW.glfwJoystickIsGamepad(jid)) {
                activeJoystick = jid;
                return jid;
            }
        }
        return -1;
    }

    public static boolean available() {
        return available;
    }

    public static String name() {
        return activeJoystick >= 0 ? GLFW.glfwGetGamepadName(activeJoystick) : null;
    }

    /** Front montant du bouton configuré. */
    public static boolean pressed(ClientConfig.GamepadButton button) {
        return available && button != null && pressed[button.glfw];
    }

    public static boolean isDown(ClientConfig.GamepadButton button) {
        return available && button != null && down[button.glfw];
    }

    /** Direction (0 haut, 1 droite, 2 bas, 3 gauche) : croix directionnelle ou stick gauche. */
    public static boolean direction(int dir) {
        if (!available) {
            return false;
        }
        int[] dpad = {GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN,
                GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT};
        return pressed[dpad[dir]] || axisPressed[dir];
    }
}
