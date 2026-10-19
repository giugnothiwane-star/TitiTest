package com.ultimateautobuilder.client.dev;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.client.PlacementController;
import com.ultimateautobuilder.client.gui.BuilderScreen;
import com.ultimateautobuilder.client.image.ImageBuildScreen;
import com.ultimateautobuilder.client.image.ImageFiles;
import com.ultimateautobuilder.client.visual.VisualStudioScreen;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.image.ImageData;
import com.ultimateautobuilder.core.image.ImageLoader;
import com.ultimateautobuilder.network.ActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

/**
 * Captures d'écran automatiques pour la vérification visuelle en intégration continue.
 * Inactif sauf si le jeu est lancé avec {@code -Duab.capture=true} : le mod parcourt alors
 * les écrans principaux dans un monde solo, enregistre une capture de chacun puis quitte le jeu.
 */
public final class CaptureRunner {
    private static final boolean ENABLED = Boolean.getBoolean("uab.capture");
    private static int ticks = -1;
    private static int step;
    private static int wait;
    private static ImageBuildScreen imageScreen;

    private CaptureRunner() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    /** Appelé à chaque tick client. */
    public static void tick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            run(mc);
        } catch (RuntimeException e) {
            UltimateAutoBuilderMod.LOGGER.error("Capture : étape {} en échec", step, e);
            step++;
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                msg -> UltimateAutoBuilderMod.LOGGER.info("Capture : {} ({})", name, msg.getString()));
    }

    private static void run(Minecraft mc) {
        switch (step++) {
            case 0 -> wait = 200; // chargement des chunks autour du joueur
            case 1 -> {
                PacketDistributor.sendToServer(new ActionPayload(ActionPayload.REQUEST_STATE, 0, ""));
                BuilderScreen s = new BuilderScreen();
                mc.setScreen(s);
                s.captureSelect("maison_bois");
                wait = 300;
            }
            case 2 -> {
                shot(mc, "01_catalogue_maison");
                wait = 10;
            }
            case 3 -> {
                if (mc.screen instanceof BuilderScreen s) {
                    s.captureSelect(DefaultCatalog.shared().get("mob_creeper") != null ? "mob_creeper" : "maison_bois");
                }
                wait = 250;
            }
            case 4 -> {
                shot(mc, "02_catalogue_mob");
                wait = 10;
            }
            case 5 -> {
                if (mc.screen instanceof BuilderScreen s) {
                    s.captureSelect("cerisier_maison_quai");
                }
                wait = 300;
            }
            case 6 -> {
                shot(mc, "03_catalogue_inspiration");
                wait = 10;
            }
            case 7 -> {
                mc.setScreen(null);
                PlacementController.SETTINGS.entry = DefaultCatalog.shared().get("maison_bois");
                PlacementController.start();
                mc.player.setXRot(25f);
                wait = 300;
            }
            case 8 -> {
                shot(mc, "04_fantome_monde");
                wait = 10;
            }
            case 9 -> {
                PlacementController.stop();
                Path img = ImageFiles.images().resolve("capture_maison.png");
                ImageFiles.ensureDirs();
                try {
                    ImageLoader.writePng(sampleHouse(), img);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
                imageScreen = new ImageBuildScreen(null);
                mc.setScreen(imageScreen);
                imageScreen.captureLoad(img);
                wait = 100;
            }
            case 10 -> {
                shot(mc, "05_image_analyse");
                imageScreen.captureGenerate();
                wait = 200;
            }
            case 11 -> {
                imageScreen.capturePreview(0, false);
                wait = 200;
            }
            case 12 -> {
                shot(mc, "06_image_apercu_3d");
                imageScreen.capturePreview(10, false);
                wait = 150;
            }
            case 13 -> {
                shot(mc, "07_image_comparaison");
                imageScreen.capturePreview(0, true);
                wait = 150;
            }
            case 14 -> {
                shot(mc, "08_image_estimes");
                mc.setScreen(new VisualStudioScreen(null));
                wait = 100;
            }
            case 15 -> {
                shot(mc, "09_studio");
                wait = 60;
            }
            default -> {
                UltimateAutoBuilderMod.LOGGER.info("Capture terminée, fermeture du jeu");
                mc.stop();
            }
        }
    }

    /** Image de test : maison (murs, toit, fenêtres, porte) sur fond de ciel, avec dégradés. */
    private static ImageData sampleHouse() {
        int w = 320, h = 240;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sky = 0xFF000000 | ((170 + y / 6) << 16) | ((200 + y / 8) << 8) | 245;
                int c = sky;
                boolean wall = y >= 120 && y < 225 && x >= 60 && x < 260;
                int half = (y - 40) * 120 / 80;
                boolean roof = y >= 40 && y < 120 && Math.abs(x - 160) <= half;
                if (wall) {
                    int shade = (x + y) % 9 == 0 ? 12 : 0;
                    c = 0xFF000000 | ((200 - shade) << 16) | ((170 - shade) << 8) | (130 - shade);
                    boolean win = y >= 140 && y < 175 && ((x >= 85 && x < 125) || (x >= 195 && x < 235));
                    boolean door = y >= 165 && x >= 145 && x < 175;
                    if (win) {
                        c = (x % 20 == 0 || y == 157) ? 0xFFEFEFEF : 0xFF3A5A8A;
                    } else if (door) {
                        c = 0xFF5A3A1E;
                    }
                } else if (roof) {
                    int band = (y / 8) % 2 == 0 ? 0 : 18;
                    c = 0xFF000000 | ((150 - band) << 16) | ((50 - band / 2) << 8) | (40 - band / 2);
                } else if (y >= 225) {
                    c = 0xFF4E8A3A;
                }
                px[y * w + x] = c;
            }
        }
        return new ImageData(w, h, px);
    }
}
