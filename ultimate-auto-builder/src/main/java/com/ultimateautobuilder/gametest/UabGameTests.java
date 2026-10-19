package com.ultimateautobuilder.gametest;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.builder.BlockResolver;
import com.ultimateautobuilder.builder.BuildManager;
import com.ultimateautobuilder.builder.BuildMode;
import com.ultimateautobuilder.builder.BuildRequest;
import com.ultimateautobuilder.builder.Job;
import com.ultimateautobuilder.builder.PlaceJob;
import com.ultimateautobuilder.builder.RestoreJob;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.progression.ItemCost;
import com.ultimateautobuilder.core.progression.PlayerProgress;
import com.ultimateautobuilder.core.progression.ProgressionRules;
import com.ultimateautobuilder.core.progression.Tier;
import com.ultimateautobuilder.core.progression.UnlockRule;
import com.ultimateautobuilder.progression.ProgressManager;
import com.ultimateautobuilder.registry.ModCreativeTabs;
import com.ultimateautobuilder.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Scénarios exécutés dans un vrai serveur Minecraft ({@code ./gradlew runGameTestServer}).
 * Chaque test a son propre lot (batch) : les constructions ne se concurrencent pas pour la
 * limite de tâches simultanées.
 */
@GameTestHolder(UltimateAutoBuilderMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UabGameTests {
    // NeoForge préfixe lui-même l'espace de noms du @GameTestHolder.
    private static final String TEMPLATE = "empty_48";
    /** Point d'ancrage relatif au gabarit : centre, juste au-dessus du sol (y = 0). */
    private static final BlockPos CENTER = new BlockPos(24, 1, 24);

    private UabGameTests() {
    }

    private static BuildRequest request(GameTestHelper h, String modelId, SizePreset preset, int custom, int rotation, BuildMode mode) {
        ServerLevel level = h.getLevel();
        return new BuildRequest(modelId, preset, custom, rotation, false, h.absolutePos(CENTER), level.dimension().location(), mode, true, true);
    }

    private static PlaceJob submit(GameTestHelper h, BuildRequest req) {
        BuildManager m = BuildManager.get();
        if (m == null) {
            h.fail("BuildManager non initialisé");
            throw new IllegalStateException();
        }
        return m.submit(null, h.getLevel(), req);
    }

    private static int countNonAir(ServerLevel level, int[] b) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = b[0]; x <= b[3]; x++) {
            for (int y = b[1]; y <= b[4]; y++) {
                for (int z = b[2]; z <= b[5]; z++) {
                    if (!level.getBlockState(p.set(x, y, z)).isAir()) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** Mode couche par couche : la tâche se met en pause, puis se termine après des reprises successives. */
    @GameTest(template = TEMPLATE, timeoutTicks = 2400, batch = "uab_steps")
    public static void stepModePausesAndResumes(GameTestHelper h) {
        BuildRequest req = new BuildRequest("maison_bois", SizePreset.CUSTOM, 9, 0, false, h.absolutePos(CENTER),
                h.getLevel().dimension().location(), BuildMode.NORMAL, true, true, BuildRequest.STEP_LAYER, false, false);
        PlaceJob job = submit(h, req);
        int[] pauses = new int[1];
        h.onEachTick(() -> {
            if (job.paused()) {
                pauses[0]++;
                h.assertTrue(job.progress() < job.total(), "Pause alors que tout est posé");
                job.resume();
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(job.state() != Job.State.FAILED, "Échec : " + job.message());
            h.assertTrue(job.state() == Job.State.DONE, "Construction en cours : " + job.state());
            h.assertTrue(pauses[0] >= 3, "Trop peu de pauses en mode couche par couche : " + pauses[0]);
        });
    }

    /** Option « zone vide uniquement » : un obstacle dans l'emprise fait échouer la demande sans rien poser. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_empty")
    public static void emptyOnlyRefusesObstacles(GameTestHelper h) {
        h.setBlock(new BlockPos(CENTER.getX(), 3, CENTER.getZ()), Blocks.SPONGE);
        BuildRequest req = new BuildRequest("maison_bois", SizePreset.CUSTOM, 9, 0, false, h.absolutePos(CENTER),
                h.getLevel().dimension().location(), BuildMode.NORMAL, true, true, BuildRequest.STEP_AUTO, false, true);
        PlaceJob job = submit(h, req);
        h.succeedWhen(() -> {
            h.assertTrue(job.state() == Job.State.FAILED, "La demande aurait dû être refusée (état " + job.state() + ")");
            h.assertTrue(job.placed() == 0, job.placed() + " blocs posés malgré l'obstacle");
            h.assertBlockPresent(Blocks.SPONGE, new BlockPos(CENTER.getX(), 3, CENTER.getZ()));
        });
    }

    /** Pause manuelle : aucune progression tant que la tâche est en pause. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_pause")
    public static void manualPauseFreezesProgress(GameTestHelper h) {
        PlaceJob job = submit(h, request(h, "chateau_fort", SizePreset.SMALL, 0, 0, BuildMode.NORMAL));
        int[] frozen = {-1};
        h.runAfterDelay(3, () -> {
            BuildManager.get().setPaused(null, job.id(), true, true);
            frozen[0] = job.progress();
        });
        h.runAfterDelay(40, () -> {
            h.assertTrue(job.paused(), "La tâche devrait être en pause");
            h.assertTrue(job.progress() == frozen[0], "Progression pendant la pause : " + frozen[0] + " → " + job.progress());
            job.cancel();
        });
        h.succeedWhen(() -> h.assertTrue(job.state() == Job.State.CANCELLED, "Annulation en pause non traitée : " + job.state()));
    }

    /** Petite maison, construction gratuite par le serveur (aucun inventaire impliqué). */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_house")
    public static void buildSmallHouse(GameTestHelper h) {
        PlaceJob job = submit(h, request(h, "maison_bois", SizePreset.CUSTOM, 9, 0, BuildMode.NORMAL));
        h.succeedWhen(() -> {
            h.assertTrue(job.state() != Job.State.FAILED, "Échec : " + job.message());
            h.assertTrue(job.state() == Job.State.DONE, "Construction en cours : " + job.state());
            int placed = countNonAir(h.getLevel(), job.bounds());
            h.assertTrue(placed > 50, "Trop peu de blocs posés : " + placed);
            h.assertTrue(job.progress() == job.total(), "Progression incomplète " + job.progress() + "/" + job.total());
        });
    }

    /** Statue sculptée (samouraï) en petite taille avec rotation ; Pikachu n'existe plus dans le catalogue. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_statue")
    public static void buildRotatedStatue(GameTestHelper h) {
        h.assertTrue(DefaultCatalog.shared().get("pikachu") == null, "Pikachu est encore présent dans le catalogue");
        PlaceJob job = submit(h, request(h, "samourai", SizePreset.SMALL, 0, 1, BuildMode.NORMAL));
        h.succeedWhen(() -> {
            h.assertTrue(job.state() != Job.State.FAILED, "Échec : " + job.message());
            h.assertTrue(job.state() == Job.State.DONE, "Construction en cours : " + job.state());
            int[] b = job.bounds();
            h.assertTrue(b[4] - b[1] + 1 >= 6, "Hauteur inattendue : " + (b[4] - b[1] + 1));
            int placed = countNonAir(h.getLevel(), b);
            h.assertTrue(placed >= job.total() * 9 / 10, "Blocs posés : " + placed + " / " + job.total());
        });
    }

    /** L'annulation arrête la pose et laisse la tâche dans l'état CANCELLED. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_cancel")
    public static void cancelBuild(GameTestHelper h) {
        PlaceJob job = submit(h, request(h, "chateau_fort", SizePreset.SMALL, 0, 0, BuildMode.NORMAL));
        h.runAfterDelay(2, job::cancel);
        h.succeedWhen(() -> h.assertTrue(job.state() == Job.State.CANCELLED, "État attendu CANCELLED, obtenu " + job.state()));
    }

    /** Construction puis retour arrière : la zone doit retrouver exactement son état initial. */
    @GameTest(template = TEMPLATE, timeoutTicks = 2400, batch = "uab_undo")
    public static void undoBuild(GameTestHelper h) {
        // Instantané du gabarit avant construction (le sol du gabarit peut se trouver dans l'emprise).
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        int size = 48;
        BlockState[] before = new BlockState[size * size * size];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    before[(x * size + y) * size + z] = level.getBlockState(p.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z));
                }
            }
        }
        PlaceJob job = submit(h, request(h, "maison_pierre", SizePreset.CUSTOM, 9, 0, BuildMode.NORMAL));
        RestoreJob[] restore = new RestoreJob[1];
        int[][] bounds = new int[1][];
        h.onEachTick(() -> {
            if (restore[0] == null && job.state() == Job.State.DONE) {
                bounds[0] = job.bounds();
                restore[0] = BuildManager.get().undo(null, h.getLevel(), job.id(), true);
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(job.state() != Job.State.FAILED, "Échec : " + job.message());
            h.assertTrue(restore[0] != null && restore[0].state() == Job.State.DONE, "Retour arrière en cours");
            int[] b = bounds[0];
            int left = 0;
            StringBuilder sample = new StringBuilder();
            BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
            for (int x = b[0]; x <= b[3]; x++) {
                for (int y = b[1]; y <= b[4]; y++) {
                    for (int z = b[2]; z <= b[5]; z++) {
                        int rx = x - origin.getX(), ry = y - origin.getY(), rz = z - origin.getZ();
                        BlockState s = level.getBlockState(q.set(x, y, z));
                        boolean inside = rx >= 0 && ry >= 0 && rz >= 0 && rx < size && ry < size && rz < size;
                        boolean ok = inside ? s == before[(rx * size + ry) * size + rz] : s.isAir();
                        if (!ok) {
                            if (left < 8) {
                                sample.append(s).append(" @").append(rx).append(',').append(ry).append(',').append(rz).append("; ");
                            }
                            left++;
                        }
                    }
                }
            }
            // Le mode prudent laisse volontairement en place les blocs modifiés depuis la construction.
            h.assertTrue(left <= restore[0].conflicts(), left + " blocs différents de l'état initial après le retour arrière (" + restore[0].message() + ") : " + sample);
        });
    }

    /** Une structure qui dépasse la hauteur du monde est refusée sans poser de bloc. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_height")
    public static void rejectsOutOfHeight(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos top = new BlockPos(h.absolutePos(CENTER).getX(), level.getMaxBuildHeight() - 3, h.absolutePos(CENTER).getZ());
        BuildRequest req = new BuildRequest("tour_sorcier", SizePreset.NORMAL, 0, 0, false, top, level.dimension().location(), BuildMode.NORMAL, true, true);
        PlaceJob job = submit(h, req);
        h.succeedWhen(() -> {
            h.assertTrue(job.state() == Job.State.FAILED, "La construction hors limites aurait dû échouer (état " + job.state() + ")");
            h.assertTrue(job.progress() == 0, "Des blocs ont été posés malgré l'échec");
        });
    }

    /** Mode prudent : un bloc existant dans la zone n'est jamais écrasé. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_prudent")
    public static void prudentKeepsExisting(GameTestHelper h) {
        List<BlockPos> pillar = new ArrayList<>();
        for (int y = 1; y <= 6; y++) {
            BlockPos p = new BlockPos(CENTER.getX(), y, CENTER.getZ());
            h.setBlock(p, Blocks.SPONGE);
            pillar.add(p);
        }
        PlaceJob job = submit(h, request(h, "maison_bois", SizePreset.CUSTOM, 9, 0, BuildMode.PRUDENT));
        h.succeedWhen(() -> {
            h.assertTrue(job.state() == Job.State.DONE, "Construction en cours : " + job.state());
            for (BlockPos p : pillar) {
                h.assertBlockPresent(Blocks.SPONGE, p);
            }
        });
    }

    /** Chaque bloc utilisé par les 400 modèles existe réellement dans Minecraft 1.21.1. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "uab_palette")
    public static void paletteResolves(GameTestHelper h) {
        List<String> unknown = new ArrayList<>();
        int checked = 0;
        for (ModelEntry e : DefaultCatalog.shared().all()) {
            if (e.id().startsWith("custom:")) {
                continue;
            }
            VoxelModel m = e.generate(e.sizes().small(), true);
            for (String state : m.paletteCopy()) {
                if (B.isAir(state)) {
                    continue;
                }
                checked++;
                if (BlockResolver.resolve(state).fallback() && unknown.size() < 20) {
                    unknown.add(e.id() + " → " + state);
                }
            }
        }
        h.assertTrue(checked > 0, "Aucune palette vérifiée");
        h.assertTrue(unknown.isEmpty(), "Blocs inconnus : " + unknown);
        h.succeed();
    }

    /** Tous les blocs proposés par Image-to-Build existent dans cette version du jeu. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20, batch = "uab_image_palette")
    public static void imagePaletteResolves(GameTestHelper h) {
        List<String> unknown = new ArrayList<>();
        var all = com.ultimateautobuilder.core.image.BlockPalette.of(java.util.EnumSet.allOf(com.ultimateautobuilder.core.image.BlockPalette.Family.class));
        for (var e : all.entries()) {
            if (BlockResolver.resolve(e.state()).fallback()) {
                unknown.add(e.state());
            }
        }
        h.assertTrue(unknown.isEmpty(), "Blocs inconnus : " + unknown);
        h.succeed();
    }

    /** Chaîne complète : image de synthèse → analyse → reconstruction → format réseau → construction par le moteur. */
    @GameTest(template = TEMPLATE, timeoutTicks = 2400, batch = "uab_image_build")
    public static void imageModelBuilds(GameTestHelper h) {
        int w = 60, hh = 50;
        int[] px = new int[w * hh];
        for (int y = 0; y < hh; y++) {
            for (int x = 0; x < w; x++) {
                boolean wall = y >= 25 && y < 48 && x >= 10 && x < 50;
                boolean roof = y >= 8 && y < 25 && Math.abs(x - 30) <= (y - 8) * 22 / 17;
                px[y * w + x] = wall ? 0xFF8A8A8A : roof ? 0xFFB0302A : 0xFFFFFFFF;
            }
        }
        var a = com.ultimateautobuilder.core.image.ImageAnalysis.analyze(new com.ultimateautobuilder.core.image.ImageData(w, hh, px));
        var o = new com.ultimateautobuilder.core.image.Reconstructor.Options(com.ultimateautobuilder.core.image.Reconstructor.Mode.BUILDING, null,
                null, null, 0, 14, 8, null, true, a.best().subject(), false, false);
        var r = com.ultimateautobuilder.core.image.Reconstructor.reconstruct(com.ultimateautobuilder.core.image.Reconstructor.Views.single(a), o);
        VoxelModel model;
        try {
            byte[] data = com.ultimateautobuilder.core.importer.ModelCodec.encode("Maison image", r.model().toVoxelModel());
            model = com.ultimateautobuilder.core.importer.ModelCodec.decode(data, com.ultimateautobuilder.config.ServerConfig.limits()).model();
        } catch (Exception e) {
            h.fail("Encodage du modèle impossible : " + e.getMessage());
            return;
        }
        h.assertTrue(model.size() == r.model().count(), "Blocs perdus à l'encodage : " + model.size() + " / " + r.model().count());
        var entry = com.ultimateautobuilder.storage.CustomModelStore.register("gametest_image", "Maison image", model);
        PlaceJob job = submit(h, request(h, entry.id(), SizePreset.NORMAL, 0, 0, BuildMode.NORMAL));
        h.succeedWhen(() -> {
            h.assertTrue(job.state() != Job.State.FAILED, "Échec : " + job.message());
            h.assertTrue(job.state() == Job.State.DONE, "Construction en cours : " + job.state());
            int placed = countNonAir(h.getLevel(), job.bounds());
            h.assertTrue(placed >= model.size() * 9 / 10, "Blocs posés : " + placed + " / " + model.size());
        });
    }

    /**
     * Cristal unique : l'Ultimate Crystal est enregistré, fabricable, seul objet de l'onglet créatif ;
     * les anciens cristaux restent enregistrés (mondes existants) et convertibles.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 20, batch = "uab_item")
    public static void crystalRegistered(GameTestHelper h) {
        h.assertTrue(ModItems.ULTIMATE_CRYSTAL.getId().toString().equals("ultimateautobuilder:ultimate_crystal"), "Identifiant inattendu");
        h.assertTrue(ModItems.ULTIMATE_CRYSTAL.get().getDefaultInstance().getMaxStackSize() == 1, "Le cristal doit être non empilable");
        h.assertTrue(ModItems.ICE_CRYSTAL.getId().toString().equals("ultimateautobuilder:ice_crystal"), "Ancien cristal absent du registre");
        h.assertTrue(ModItems.VISUAL_CRYSTAL.getId().toString().equals("ultimateautobuilder:visual_crystal"), "Ancien cristal visuel absent du registre");
        var recipes = h.getLevel().getServer().getRecipeManager();
        for (String r : new String[]{"ultimate_crystal", "ultimate_crystal_from_ice_crystal", "ultimate_crystal_from_visual_crystal"}) {
            h.assertTrue(recipes.byKey(UltimateAutoBuilderMod.id(r)).isPresent(), "Recette absente : " + r);
        }
        CreativeModeTab tab = ModCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), false, h.getLevel().registryAccess()));
        List<String> items = tab.getDisplayItems().stream().map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).toList();
        h.assertTrue(items.equals(List.of("ultimateautobuilder:ultimate_crystal")), "Onglet créatif inattendu : " + items);
        h.succeed();
    }

    /** Une maison de la collection Sakura se construit entièrement dans un vrai monde. */
    @GameTest(template = TEMPLATE, timeoutTicks = 2400, batch = "uab_sakura")
    public static void buildSakuraHouse(GameTestHelper h) {
        ModelEntry e = DefaultCatalog.shared().get("sakura_petite_maison");
        h.assertTrue(e != null, "Maison Sakura absente du catalogue");
        PlaceJob job = submit(h, request(h, "sakura_petite_maison", SizePreset.SMALL, 0, 0, BuildMode.NORMAL));
        h.succeedWhen(() -> {
            h.assertTrue(job.state() != Job.State.FAILED, "Échec : " + job.message());
            h.assertTrue(job.state() == Job.State.DONE, "Construction en cours : " + job.state());
            int placed = countNonAir(h.getLevel(), job.bounds());
            h.assertTrue(placed >= job.total() * 9 / 10, "Blocs posés : " + placed + " / " + job.total());
        });
    }

    /**
     * Progression avec un vrai joueur serveur : construction verrouillée refusée, quêtes validées
     * sur les statistiques réelles, matériaux retirés une seule fois, état relu depuis le disque.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "uab_progress")
    public static void progressionUnlocksWithRealInventory(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ServerConfig.ProgressionMode before = ServerConfig.PROGRESSION_MODE.get();
        // Le joueur factice est en créatif : la progression est forcée pour ce test.
        ServerConfig.PROGRESSION_MODE.set(ServerConfig.ProgressionMode.ALWAYS);
        try {
            ProgressManager.forget(player.getUUID());
            ModelEntry locked = null;
            for (ModelEntry e : DefaultCatalog.shared().all()) {
                if (ProgressionRules.rule(e).tier() == Tier.PIERRE && !ProgressionRules.rule(e).cost().isEmpty()) {
                    locked = e;
                    break;
                }
            }
            h.assertTrue(locked != null, "Aucune construction de niveau pierre");
            UnlockRule rule = ProgressionRules.rule(locked);
            h.assertFalse(ProgressManager.canBuild(player, locked), "Construction verrouillée acceptée");
            String refused = null;
            try {
                BuildManager.get().submit(player, h.getLevel(), request(h, locked.id(), SizePreset.SMALL, 0, 0, BuildMode.NORMAL));
            } catch (IllegalStateException ex) {
                refused = ex.getMessage();
            }
            h.assertTrue(refused != null && refused.contains("verrouillée"), "Le serveur doit refuser une construction verrouillée : " + refused);
            h.assertTrue(ProgressManager.unlock(player, locked.id()).status() == PlayerProgress.UnlockStatus.TIER_TOO_LOW, "Niveau non vérifié");

            // Chaîne de quêtes jusqu'au niveau pierre, sur des faits réels.
            h.assertTrue(ProgressManager.claim(player, "decouverte").status() == PlayerProgress.ClaimStatus.NOT_READY, "Quête validée sans action");
            ProgressManager.markOpened(player, false);
            h.assertTrue(ProgressManager.claim(player, "decouverte").status() == PlayerProgress.ClaimStatus.OK, "Ouverture du cristal non comptée");
            h.assertTrue(ProgressManager.claim(player, "decouverte").status() == PlayerProgress.ClaimStatus.ALREADY, "Récompense donnée deux fois");
            ProgressManager.markOpened(player, true);
            h.assertTrue(ProgressManager.claim(player, "catalogue").status() == PlayerProgress.ClaimStatus.OK, "Quête catalogue");
            ProgressManager.onBuildCompleted(player.getUUID(), DefaultCatalog.shared().get("maison_bois"), null);
            h.assertTrue(ProgressManager.claim(player, "premiere_maison").status() == PlayerProgress.ClaimStatus.OK, "Quête première maison");
            h.assertTrue(ProgressManager.claim(player, "pierre").status() == PlayerProgress.ClaimStatus.NOT_READY, "Minage non vérifié");
            player.awardStat(Stats.BLOCK_MINED.get(Blocks.STONE), 64);
            h.assertTrue(ProgressManager.claim(player, "pierre").status() == PlayerProgress.ClaimStatus.OK, "Statistique de minage ignorée");
            h.assertTrue(ProgressManager.get(player).tier() == Tier.PIERRE, "Niveau pierre non atteint");

            // Matériaux insuffisants : rien n'est retiré.
            player.getInventory().clearContent();
            ItemCost first = rule.cost().get(0);
            Item firstItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(first.item()));
            give(player, firstItem, 1);
            h.assertTrue(ProgressManager.unlock(player, locked.id()).status() == PlayerProgress.UnlockStatus.MISSING_MATERIALS, "Matériaux non vérifiés");
            h.assertTrue(player.getInventory().countItem(firstItem) == 1, "Objets retirés malgré un refus");

            // Coût exact + 10 : seul le coût est retiré, une seule fois.
            player.getInventory().clearContent();
            for (ItemCost c : rule.cost()) {
                give(player, BuiltInRegistries.ITEM.get(ResourceLocation.parse(c.item())), c.count() + 10);
            }
            var ok = ProgressManager.unlock(player, locked.id());
            h.assertTrue(ok.status() == PlayerProgress.UnlockStatus.OK, "Déblocage refusé : " + ok.message());
            h.assertTrue(ProgressManager.unlock(player, locked.id()).status() == PlayerProgress.UnlockStatus.ALREADY, "Double déblocage");
            for (ItemCost c : rule.cost()) {
                int left = player.getInventory().countItem(BuiltInRegistries.ITEM.get(ResourceLocation.parse(c.item())));
                h.assertTrue(left == 10, c.item() + " : il reste " + left + " au lieu de 10");
            }
            h.assertTrue(ProgressManager.canBuild(player, locked), "Construction toujours verrouillée après déblocage");

            // Relecture depuis le fichier du monde.
            ProgressManager.forget(player.getUUID());
            PlayerProgress reloaded = ProgressManager.get(player);
            h.assertTrue(reloaded.tier() == Tier.PIERRE, "Niveau perdu à la relecture");
            h.assertTrue(reloaded.unlocked().contains(locked.id()), "Déblocage perdu à la relecture");
            h.assertTrue(reloaded.claimed().contains("pierre"), "Quête perdue à la relecture");
        } finally {
            ServerConfig.PROGRESSION_MODE.set(before);
        }
        h.succeed();
    }

    private static void give(ServerPlayer player, Item item, int count) {
        for (int left = count; left > 0; ) {
            int n = Math.min(left, item.getDefaultMaxStackSize());
            player.getInventory().add(new ItemStack(item, n));
            left -= n;
        }
    }
}
