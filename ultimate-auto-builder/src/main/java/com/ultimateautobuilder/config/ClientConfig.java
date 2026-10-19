package com.ultimateautobuilder.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Préférences propres au joueur (fichier config/ultimateautobuilder-client.toml). */
public final class ClientConfig {
    public enum CrystalHand { MAIN_HAND, ANY_HAND }

    public enum GamepadButton {
        A(0), B(1), X(2), Y(3), LEFT_BUMPER(4), RIGHT_BUMPER(5), BACK(6), START(7), GUIDE(8), LEFT_THUMB(9), RIGHT_THUMB(10),
        DPAD_UP(11), DPAD_RIGHT(12), DPAD_DOWN(13), DPAD_LEFT(14);

        public final int glfw;

        GamepadButton(int glfw) {
            this.glfw = glfw;
        }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue REQUIRE_CRYSTAL;
    public static final ModConfigSpec.EnumValue<CrystalHand> CRYSTAL_HAND;
    public static final ModConfigSpec.BooleanValue RIGHT_CLICK_OPENS;
    public static final ModConfigSpec.BooleanValue CRYSTAL_PARTICLES;

    public static final ModConfigSpec.BooleanValue GAMEPAD_ENABLED;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_OPEN;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_SELECT;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_BACK;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_ROTATE;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_SIZE;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_BUILD;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_PREV_CATEGORY;
    public static final ModConfigSpec.EnumValue<GamepadButton> GAMEPAD_NEXT_CATEGORY;

    public static final ModConfigSpec.BooleanValue GHOST_PREVIEW;
    public static final ModConfigSpec.IntValue GHOST_MAX_FACES;
    public static final ModConfigSpec.BooleanValue SHOW_PROGRESS;
    public static final ModConfigSpec.IntValue DEFAULT_DISTANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Cristal de Glace").push("crystal");
        REQUIRE_CRYSTAL = b.comment("La touche/le bouton d'ouverture n'agit que si le Cristal de Glace est tenu")
                .define("requireCrystal", true);
        CRYSTAL_HAND = b.comment("MAIN_HAND : main principale uniquement ; ANY_HAND : main principale ou secondaire")
                .defineEnum("crystalHand", CrystalHand.MAIN_HAND);
        RIGHT_CLICK_OPENS = b.comment("Un clic droit avec le cristal ouvre aussi le menu").define("rightClickOpens", true);
        CRYSTAL_PARTICLES = b.define("crystalParticles", true);
        b.pop();

        b.comment("Manette (lecture directe via GLFW, sans mod externe)").push("gamepad");
        GAMEPAD_ENABLED = b.comment("Désactiver si un mod de manette (Controlify, Controllable…) gère déjà les boutons")
                .define("enabled", true);
        GAMEPAD_OPEN = b.comment("R1 sur une manette PlayStation = RIGHT_BUMPER").defineEnum("openMenu", GamepadButton.RIGHT_BUMPER);
        GAMEPAD_SELECT = b.defineEnum("select", GamepadButton.A);
        GAMEPAD_BACK = b.defineEnum("back", GamepadButton.B);
        GAMEPAD_ROTATE = b.defineEnum("rotate", GamepadButton.X);
        GAMEPAD_SIZE = b.defineEnum("size", GamepadButton.Y);
        GAMEPAD_BUILD = b.defineEnum("build", GamepadButton.START);
        GAMEPAD_PREV_CATEGORY = b.defineEnum("previousCategory", GamepadButton.LEFT_BUMPER);
        GAMEPAD_NEXT_CATEGORY = b.comment("Dans le menu, R1 passe à la catégorie suivante").defineEnum("nextCategory", GamepadButton.RIGHT_BUMPER);
        b.pop();

        b.comment("Aperçu et affichage").push("display");
        GHOST_PREVIEW = b.comment("Aperçu fantôme de la structure dans le monde").define("ghostPreview", true);
        GHOST_MAX_FACES = b.defineInRange("ghostMaxFaces", 40_000, 1_000, 400_000);
        SHOW_PROGRESS = b.define("showProgress", true);
        DEFAULT_DISTANCE = b.comment("Distance par défaut du mode « devant moi »").defineInRange("defaultDistance", 6, 1, 128);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }

    public static boolean loaded() {
        return SPEC.isLoaded();
    }
}
