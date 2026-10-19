package com.ultimateautobuilder.client.dev;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.client.PlacementController;
import com.ultimateautobuilder.client.gui.BuilderScreen;
import com.ultimateautobuilder.client.gui.HubScreen;
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
    private static boolean builtShot;

    private CaptureRunner() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    private static int totalTicks, noWorldTicks, createTicks;
    private static boolean worldRequested;

    /** Appelé à chaque tick client. */
    public static void tick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        totalTicks++;
        if (totalTicks % 200 == 0) {
            UltimateAutoBuilderMod.LOGGER.info("Capture : tick {}, écran {}, monde {}, étape {}", totalTicks,
                    mc.screen == null ? "aucun" : mc.screen.getClass().getSimpleName(), mc.level != null, step);
        }
        if (totalTicks > 20 * 60 * 16) {
            UltimateAutoBuilderMod.LOGGER.error("Capture : délai dépassé, fermeture du jeu");
            mc.stop();
            return;
        }
        if (mc.player == null || mc.level == null) {
            noWorldTicks++;
            ensureWorld(mc);
            return;
        }
        ticks++;
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            UltimateAutoBuilderMod.LOGGER.info("Capture : étape {}", step);
            run(mc);
        } catch (RuntimeException e) {
            UltimateAutoBuilderMod.LOGGER.error("Capture : étape {} en échec", step, e);
            step++;
        }
    }

    /**
     * Sans monde ouvert (monde de test absent, accès rapide refusé) : ouverture de l'écran de
     * création puis appui sur « Créer un nouveau monde » avec les réglages par défaut.
     */
    private static void ensureWorld(Minecraft mc) {
        if (mc.screen instanceof net.minecraft.client.gui.screens.worldselection.CreateWorldScreen screen) {
            if (++createTicks == 60) {
                for (var child : screen.children()) {
                    if (child instanceof net.minecraft.client.gui.components.Button b
                            && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                            && t.getKey().equals("selectWorld.create")) {
                        UltimateAutoBuilderMod.LOGGER.info("Capture : création d'un monde");
                        b.onPress();
                        return;
                    }
                }
                UltimateAutoBuilderMod.LOGGER.warn("Capture : bouton de création introuvable");
            }
            return;
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            // Écran du tout premier lancement : on le passe, comme le ferait le joueur.
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            return;
        }
        boolean idle = mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen
                || mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen
                || mc.screen instanceof net.minecraft.client.gui.screens.AlertScreen
                || mc.screen instanceof net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
        if (!worldRequested && idle && noWorldTicks > 300) {
            worldRequested = true;
            UltimateAutoBuilderMod.LOGGER.info("Capture : aucun monde ouvert ({}), ouverture de l'écran de création",
                    mc.screen.getClass().getSimpleName());
            net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, mc.screen);
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                msg -> UltimateAutoBuilderMod.LOGGER.info("Capture : {} ({})", name, msg.getString()));
    }

    private static void run(Minecraft mc) {
        // Étapes 109-110 insérées après 9 : on y saute puis on revient à 10.
        if (step == 10 && !builtShot) {
            builtShot = true;
            step = 109;
        } else if (step == 111) {
            step = 10;
        }
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
                // Construction réelle par le serveur intégré, pour juger le rendu final dans le monde.
                PlacementController.requestBuild(mc, null);
                wait = 300;
            }
            case 9 -> {
                mc.setScreen(null);
                mc.player.setXRot(10f);
                wait = 40;
            }
            case 109 -> {
                shot(mc, "04b_construction_monde");
                wait = 10;
            }
            case 110 -> {
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
                survival(mc);
                mc.setScreen(HubScreen.openedFromCrystal());
                wait = 150;
            }
            case 16 -> {
                shot(mc, "10_hub_accueil");
                hub(mc, 4);
            }
            case 17 -> {
                shot(mc, "11_hub_quetes");
                hub(mc, 5);
            }
            case 18 -> {
                shot(mc, "12_hub_materiaux");
                hub(mc, 2);
            }
            case 19 -> {
                shot(mc, "13_hub_shaders");
                hub(mc, 1);
                wait = 150;
            }
            case 20 -> {
                shot(mc, "14_hub_maisons");
                mc.setScreen(new BuilderScreen(null, "sakura"));
                wait = 60;
            }
            case 21, 23, 25, 27 -> {
                String id = SAKURA_SHOTS[(step - 22) / 2];
                if (mc.screen instanceof BuilderScreen s) {
                    s.captureSelect(id);
                }
                wait = 300;
            }
            case 22, 24, 26, 28 -> {
                shot(mc, String.format("%02d_sakura_%s", 15 + (step - 23) / 2, SAKURA_SHOTS[(step - 23) / 2].substring(7)));
                wait = 10;
            }
            case 29 -> {
                // Parcours réel de déblocage : quêtes validées et matériaux fournis côté serveur, puis clics dans l'interface.
                prepareUnlock(mc);
                mc.setScreen(new BuilderScreen(null, "sakura"));
                wait = 100;
            }
            case 30 -> {
                if (mc.screen instanceof BuilderScreen s) {
                    s.captureSelect("sakura_minka_tradition");
                }
                wait = 200;
            }
            case 31 -> {
                shot(mc, "19_deblocage_disponible");
                press(mc, b -> b.getMessage().getString().contains("Débloquer"));
                wait = 40;
            }
            case 32 -> {
                shot(mc, "20_deblocage_confirmation");
                press(mc, b -> b.getMessage().equals(net.minecraft.network.chat.CommonComponents.GUI_YES));
                wait = 120;
            }
            case 33 -> {
                shot(mc, "21_deblocage_effectue");
                wait = 10;
            }
            case 34 -> {
                // Maison Sakura de départ construite dans le monde, cristal en main.
                mc.setScreen(null);
                selectCrystal(mc);
                mc.player.setYRot(mc.player.getYRot() + 90f);
                PlacementController.SETTINGS.entry = DefaultCatalog.shared().get("sakura_petite_maison");
                PlacementController.start();
                mc.player.setXRot(15f);
                wait = 300;
            }
            case 35 -> {
                PlacementController.requestBuild(mc, null);
                wait = 500;
            }
            case 36 -> {
                PlacementController.stop();
                stepBack(mc, 14, 3);
                wait = 80;
            }
            case 37 -> {
                shot(mc, "22_sakura_construite");
                wait = 10;
            }
            default -> {
                UltimateAutoBuilderMod.LOGGER.info("Capture terminée, fermeture du jeu");
                mc.stop();
            }
        }
    }

    private static final String[] SAKURA_SHOTS = {"sakura_petite_maison", "sakura_minka_tradition", "sakura_villa_moderne", "sakura_ryokan"};

    private static void hub(Minecraft mc, int section) {
        if (mc.screen instanceof HubScreen h) {
            h.captureSection(section);
        }
        wait = 60;
    }

    /**
     * Passe le joueur en survie sur le serveur intégré : la progression (verrous, coûts) s'applique
     * alors et devient visible sur les captures. Outil de vérification uniquement.
     */
    private static void survival(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) {
            UltimateAutoBuilderMod.LOGGER.warn("Capture : pas de serveur intégré, mode de jeu inchangé");
            return;
        }
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            var sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                // Monde de capture issu des GameTests (remise à la connexion désactivée dans sa configuration) : cristal donné ici.
                if (!sp.getInventory().hasAnyMatching(st -> st.is(com.ultimateautobuilder.registry.ModItems.ULTIMATE_CRYSTAL.get()))) {
                    sp.getInventory().add(new net.minecraft.world.item.ItemStack(com.ultimateautobuilder.registry.ModItems.ULTIMATE_CRYSTAL.get()));
                }
                UltimateAutoBuilderMod.LOGGER.info("Capture : joueur en survie (progression appliquée)");
            }
        });
    }

    /** Appuie sur le premier bouton de l'écran courant qui correspond. */
    private static void press(Minecraft mc, java.util.function.Predicate<net.minecraft.client.gui.components.Button> match) {
        if (mc.screen == null) {
            UltimateAutoBuilderMod.LOGGER.warn("Capture : aucun écran ouvert");
            return;
        }
        for (var child : mc.screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.Button b && b.active && match.test(b)) {
                UltimateAutoBuilderMod.LOGGER.info("Capture : appui sur « {} »", b.getMessage().getString());
                b.onPress();
                return;
            }
        }
        UltimateAutoBuilderMod.LOGGER.warn("Capture : bouton introuvable sur {}", mc.screen.getClass().getSimpleName());
    }

    /**
     * Fait atteindre le niveau « pierre » par le chemin normal (statistique de minage réelle,
     * réclamation des quêtes) et donne le coût exact de la minka : 16 bûches de cerisier, 32 pierres.
     */
    private static void prepareUnlock(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            var sp = server.getPlayerList().getPlayer(id);
            if (sp == null) {
                return;
            }
            sp.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Blocks.STONE), 64);
            com.ultimateautobuilder.progression.ProgressManager.markOpened(sp, false);
            com.ultimateautobuilder.progression.ProgressManager.markOpened(sp, true);
            for (String q : new String[]{"decouverte", "catalogue", "premiere_maison", "pierre"}) {
                var r = com.ultimateautobuilder.progression.ProgressManager.claim(sp, q);
                UltimateAutoBuilderMod.LOGGER.info("Capture : quête {} → {} ({})", q, r.status(), r.message());
            }
            sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHERRY_LOG, 16));
            sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 32));
            com.ultimateautobuilder.progression.ProgressManager.send(sp);
        });
    }

    /** Recule le joueur (téléportation serveur) pour cadrer toute la construction. */
    private static void stepBack(Minecraft mc, double back, double up) {
        var server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        java.util.UUID id = mc.player.getUUID();
        float yaw = mc.player.getYRot();
        server.execute(() -> {
            var sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                double rad = Math.toRadians(yaw);
                double dx = Math.sin(rad) * back, dz = -Math.cos(rad) * back;
                sp.connection.teleport(sp.getX() + dx, sp.getY() + up, sp.getZ() + dz, yaw, 20f);
            }
        });
    }

    /** Met l'Ultimate Crystal (donné à la première connexion) dans la main du joueur. */
    private static void selectCrystal(Minecraft mc) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(com.ultimateautobuilder.registry.ModItems.ULTIMATE_CRYSTAL.get())) {
                inv.selected = i;
                UltimateAutoBuilderMod.LOGGER.info("Capture : Ultimate Crystal en main (emplacement {})", i);
                return;
            }
        }
        UltimateAutoBuilderMod.LOGGER.warn("Capture : Ultimate Crystal absent de la barre d'action");
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
