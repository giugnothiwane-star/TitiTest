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
                serverRun(mc, sp -> {
                    // Ensemble en fer complet porté : la récompense devient disponible dans le cristal.
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS));
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
                    net.minecraft.world.item.ItemStack map = new net.minecraft.world.item.ItemStack(com.ultimateautobuilder.registry.ModItems.TREASURE_MAP.get());
                    net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, map,
                            c -> c.putString(com.ultimateautobuilder.adventure.Treasures.K_TIER, "rare"));
                    sp.getInventory().add(map);
                    sp.getInventory().add(new net.minecraft.world.item.ItemStack(com.ultimateautobuilder.registry.ModItems.UNCOMMON_KEY.get(), 2));
                    sp.getInventory().add(new net.minecraft.world.item.ItemStack(com.ultimateautobuilder.registry.ModItems.RARE_KEY.get()));
                    com.ultimateautobuilder.adventure.AdventureState.send(sp);
                });
                hub(mc, 1);
            }
            case 17 -> {
                shot(mc, "11_hub_recompenses");
                press(mc, b -> b.getMessage().getString().equals("Récupérer") && b.active);
                wait = 25;
            }
            case 18 -> {
                shot(mc, "12_recompense_animation");
                hub(mc, 3);
            }
            case 19 -> {
                shot(mc, "13_hub_tresors_cles");
                hub(mc, 5);
            }
            case 20 -> {
                shot(mc, "14_hub_shaders");
                mc.setScreen(null);
                // Château abandonné posé par le vrai placeur à 40 blocs devant le joueur.
                serverRun(mc, sp -> placeNear(sp, com.ultimateautobuilder.core.adventure.StructureType.ABANDONED_CASTLE, 40));
                wait = 500;
            }
            case 21 -> {
                serverRun(mc, sp -> viewSite(sp, "capture:castle", 0, 30, 8, 20f));
                wait = 120;
            }
            case 22 -> {
                shot(mc, "15_chateau_abandonne");
                // Dans la cour, face au donjon et à la salle des spawners.
                serverRun(mc, sp -> viewSite(sp, "capture:castle", 0, 8, 2, 10f));
                wait = 300;
            }
            case 23 -> {
                shot(mc, "16_chateau_cour_spawners");
                // Un Boss Warden forcé (la probabilité réelle est de 0,5 % × 2/72) pour vérifier variante et barre de boss.
                serverRun(mc, sp -> forceBoss(sp));
                wait = 200;
            }
            case 24 -> {
                shot(mc, "17_boss_warden");
                serverRun(mc, sp -> {
                    for (var e : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.warden.Warden.class, sp.getBoundingBox().inflate(64))) {
                        e.discard();
                    }
                    sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                    placeNear(sp, com.ultimateautobuilder.core.adventure.StructureType.LEGENDARY_HOUSE, -30);
                });
                wait = 300;
            }
            case 25 -> {
                serverRun(mc, sp -> viewSite(sp, "capture:legendary_house", 0, 18, 6, 15f));
                wait = 120;
            }
            case 26 -> {
                shot(mc, "18_maison_legendaire");
                serverRun(mc, sp -> openLockedChestTest(sp));
                wait = 60;
            }
            case 27 -> {
                shot(mc, "19_coffre_verrouille_ouvert");
                mc.setScreen(null);
                selectItem(mc, com.ultimateautobuilder.registry.ModItems.TREASURE_MAP.get());
                wait = 40;
            }
            case 28 -> {
                if (mc.player != null) {
                    com.ultimateautobuilder.client.ClientHooks.openTreasureMap(mc.player.getMainHandItem());
                }
                wait = 40;
            }
            case 29 -> {
                shot(mc, "20_carte_tresor");
                mc.setScreen(null);
                serverRun(mc, sp -> viewStarterHouse(sp));
                wait = 120;
            }
            case 30 -> {
                selectCrystal(mc);
                shot(mc, "21_maison_depart_cristal");
                wait = 10;
            }
            default -> {
                UltimateAutoBuilderMod.LOGGER.info("Capture terminée, fermeture du jeu");
                mc.stop();
            }
        }
    }

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

    /** Exécute une action sur le joueur côté serveur intégré (outil de vérification uniquement). */
    private static void serverRun(Minecraft mc, java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            UltimateAutoBuilderMod.LOGGER.warn("Capture : pas de serveur intégré");
            return;
        }
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            var sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                try {
                    action.accept(sp);
                } catch (RuntimeException e) {
                    UltimateAutoBuilderMod.LOGGER.error("Capture : action serveur en échec", e);
                }
            }
        });
    }

    /** Pose une structure avec le placeur réel, à {@code dist} blocs devant (ou derrière) le joueur. */
    private static void placeNear(net.minecraft.server.level.ServerPlayer sp, com.ultimateautobuilder.core.adventure.StructureType type, int dist) {
        var level = sp.serverLevel();
        double yaw = Math.toRadians(sp.getYRot());
        int x = (int) Math.floor(sp.getX() - Math.sin(yaw) * dist), z = (int) Math.floor(sp.getZ() + Math.cos(yaw) * dist);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        var bp = com.ultimateautobuilder.core.adventure.AdventureGen.build(type, 1234L);
        var data = com.ultimateautobuilder.adventure.AdventureData.get(sp.server);
        String key = "capture:" + (type == com.ultimateautobuilder.core.adventure.StructureType.ABANDONED_CASTLE ? "castle" : "legendary_house");
        var site = data.addSite(key);
        site.type = type;
        site.x = x;
        site.y = y;
        site.z = z;
        site.state = com.ultimateautobuilder.adventure.AdventureData.SiteState.PLACING;
        UltimateAutoBuilderMod.LOGGER.info("Capture : pose de {} en {} {} {}", type.label, x, y, z);
        com.ultimateautobuilder.adventure.StructurePlacer.enqueue(level, bp, x, y, z, 0, box -> {
            data.markPlaced(site, box);
            UltimateAutoBuilderMod.LOGGER.info("Capture : {} posé ({})", type.label, java.util.Arrays.toString(box));
        });
    }

    /** Place le joueur devant l'entrée d'un site (façade +Z), à {@code back} blocs, regard vers la structure. */
    private static void viewSite(net.minecraft.server.level.ServerPlayer sp, String key, int dx, int back, int up, float pitch) {
        var s = com.ultimateautobuilder.adventure.AdventureData.get(sp.server).site(key);
        if (s == null) {
            UltimateAutoBuilderMod.LOGGER.warn("Capture : site {} absent", key);
            return;
        }
        int[] box = s.box;
        double cx = box == null ? s.x : (box[0] + box[3]) / 2.0 + 0.5;
        double front = box == null ? s.z : box[5];
        sp.connection.teleport(cx + dx, s.y + up, front + back, 180f, pitch);
    }

    /** Boss Warden forcé au centre de la cour du château posé, via le vrai code de variantes. */
    private static void forceBoss(net.minecraft.server.level.ServerPlayer sp) {
        var s = com.ultimateautobuilder.adventure.AdventureData.get(sp.server).site("capture:castle");
        if (s == null || s.box == null) {
            return;
        }
        var pos = new net.minecraft.core.BlockPos((s.box[0] + s.box[3]) / 2, s.y + 1, (s.box[2] + s.box[5]) / 2 + 6);
        var e = com.ultimateautobuilder.adventure.MobVariants.spawn(sp.serverLevel(), pos, com.ultimateautobuilder.core.adventure.SpawnCategory.NORMAL,
                new com.ultimateautobuilder.core.adventure.SpawnRules.Warden(com.ultimateautobuilder.core.adventure.WardenVariant.BOSS));
        UltimateAutoBuilderMod.LOGGER.info("Capture : Boss Warden {}", e == null ? "non placé (pas de place)" : "apparu, échelle "
                + ((net.minecraft.world.entity.LivingEntity) e).getScale() + ", santé " + ((net.minecraft.world.entity.LivingEntity) e).getMaxHealth());
    }

    /** Coffre verrouillé rare posé devant le joueur, ouvert avec une clé rare : vrai chemin serveur. */
    private static void openLockedChestTest(net.minecraft.server.level.ServerPlayer sp) {
        var level = sp.serverLevel();
        var pos = sp.blockPosition().relative(sp.getDirection(), 2);
        var state = com.ultimateautobuilder.registry.ModBlocks.LOCKED_CHEST.get().defaultBlockState()
                .setValue(com.ultimateautobuilder.block.Props.TIER, com.ultimateautobuilder.block.Props.Tier.RARE);
        level.setBlock(pos, state, 3);
        var key = new net.minecraft.world.item.ItemStack(com.ultimateautobuilder.registry.ModItems.RARE_KEY.get());
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, key);
        com.ultimateautobuilder.adventure.Locks.openChest(level, pos, state, sp, net.minecraft.world.InteractionHand.MAIN_HAND, key);
        UltimateAutoBuilderMod.LOGGER.info("Capture : coffre verrouillé → {}", level.getBlockState(pos));
    }

    /** Vue de la maison de départ du joueur si elle a été construite. */
    private static void viewStarterHouse(net.minecraft.server.level.ServerPlayer sp) {
        var s = com.ultimateautobuilder.adventure.AdventureData.get(sp.server).site("start:" + sp.getUUID());
        if (s == null || s.box == null) {
            UltimateAutoBuilderMod.LOGGER.warn("Capture : maison de départ absente (joueur existant ou aucun emplacement)");
            return;
        }
        double cx = (s.box[0] + s.box[3]) / 2.0, cz = (s.box[2] + s.box[5]) / 2.0;
        // Recul de 14 blocs dans la direction de la façade (+Z du plan, tourné comme la maison).
        long v = com.ultimateautobuilder.core.placement.Placement.rotateXZ(0, 14, s.rotation, false);
        double px = cx + (int) (v >> 32), pz = cz + (int) v;
        float look = (float) Math.toDegrees(Math.atan2(-(cx - px), cz - pz));
        sp.connection.teleport(px, s.y + 3, pz, look, 12f);
        UltimateAutoBuilderMod.LOGGER.info("Capture : maison de départ en {}", java.util.Arrays.toString(s.box));
    }

    private static void selectItem(Minecraft mc, net.minecraft.world.item.Item item) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) {
                inv.selected = i;
                return;
            }
        }
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
