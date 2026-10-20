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
     * Cristal unique et objets d'aventure : identifiants stables, recettes chargées, onglet créatif
     * (cristal, clés, carte, blocs d'aventure) ; les anciens cristaux restent enregistrés.
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
        h.assertTrue(items.size() == 10 && items.get(0).equals("ultimateautobuilder:ultimate_crystal") && items.contains("ultimateautobuilder:op_key")
                && !items.contains("ultimateautobuilder:ice_crystal"), "Onglet créatif inattendu : " + items);
        h.succeed();
    }

    private static ServerPlayer survivalPlayer(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        // Aucune maison de départ dans le monde de test.
        var data = com.ultimateautobuilder.adventure.AdventureData.get(h.getLevel().getServer());
        data.player(p.getUUID()).starterHouseDone = true;
        return p;
    }

    /**
     * Tables de butin : chaque table du mod se tire réellement ; les enchantements sont compatibles
     * avec leur objet ; ni diamant ni Netherite dans les coffres communs et peu communs ; pas de
     * lingot de Netherite dans les coffres légendaires ordinaires.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "uab_loot")
    public static void lootTablesAreBalanced(GameTestHelper h) {
        ServerPlayer p = survivalPlayer(h);
        String[] tables = {"chests/common", "chests/uncommon", "chests/rare", "chests/legendary", "chests/op", "chests/locked_common",
                "chests/locked_uncommon", "chests/locked_rare", "chests/locked_legendary", "chests/locked_op", "chests/treasure_common",
                "chests/treasure_uncommon", "chests/treasure_rare", "chests/treasure_legendary", "entities/boss_warden"};
        int maxEff = 0;
        for (String t : tables) {
            int total = 0;
            for (int i = 0; i < 60; i++) {
                for (ItemStack st : com.ultimateautobuilder.adventure.Rewards.roll(p, t)) {
                    total++;
                    String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
                    if (t.equals("chests/common") || t.equals("chests/uncommon")) {
                        h.assertFalse(id.contains("diamond") || id.contains("netherite"), t + " contient " + id);
                    }
                    if (t.equals("chests/legendary")) {
                        h.assertFalse(id.equals("minecraft:netherite_ingot") || id.startsWith("minecraft:netherite_"), t + " contient " + id);
                    }
                    for (var e : st.getEnchantments().entrySet()) {
                        h.assertTrue(e.getKey().value().canEnchant(st), t + " : enchantement incompatible sur " + id);
                        if (e.getKey().is(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY)) {
                            maxEff = Math.max(maxEff, e.getIntValue());
                        }
                    }
                }
            }
            h.assertTrue(total > 0, "Table vide ou absente : " + t);
        }
        h.assertTrue(maxEff >= 25, "Efficacité XXV/XXX attendue dans les récompenses OP : " + maxEff);
        h.succeed();
    }

    /** Clés : une mauvaise clé n'est pas consommée ; la bonne ouvre le coffre une seule fois. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "uab_keys")
    public static void keysOpenOnlyMatchingLocks(GameTestHelper h) {
        ServerPlayer p = survivalPlayer(h);
        BlockPos pos = h.absolutePos(CENTER);
        BlockState locked = com.ultimateautobuilder.registry.ModBlocks.LOCKED_CHEST.get().defaultBlockState()
                .setValue(com.ultimateautobuilder.block.Props.TIER, com.ultimateautobuilder.block.Props.Tier.RARE);
        h.getLevel().setBlock(pos, locked, 3);
        ItemStack wrong = new ItemStack(ModItems.COMMON_KEY.get(), 3);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wrong);
        com.ultimateautobuilder.adventure.Locks.openChest(h.getLevel(), pos, locked, p, net.minecraft.world.InteractionHand.MAIN_HAND, wrong);
        h.assertTrue(h.getLevel().getBlockState(pos).is(com.ultimateautobuilder.registry.ModBlocks.LOCKED_CHEST.get()), "Ouvert avec la mauvaise clé");
        h.assertTrue(wrong.getCount() == 3, "Mauvaise clé consommée");
        ItemStack right = new ItemStack(ModItems.RARE_KEY.get(), 2);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, right);
        com.ultimateautobuilder.adventure.Locks.openChest(h.getLevel(), pos, locked, p, net.minecraft.world.InteractionHand.MAIN_HAND, right);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CHEST), "Le coffre ne s'est pas ouvert");
        h.assertTrue(right.getCount() == 1, "Clé non consommée ou consommée deux fois : " + right.getCount());
        // Le butin est tiré à l'ouverture (menu ouvert pour le joueur) : le coffre est rempli, sa table consommée.
        h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity be
                && (be.getLootTable() != null || !be.isEmpty()), "Coffre ouvert sans butin");
        // Second clic : le coffre est ordinaire, plus rien n'est consommé.
        com.ultimateautobuilder.adventure.Locks.openChest(h.getLevel(), pos, h.getLevel().getBlockState(pos), p,
                net.minecraft.world.InteractionHand.MAIN_HAND, right);
        h.assertTrue(right.getCount() == 1, "Double consommation");
        h.succeed();
    }

    /** Spawner « bébé » : uniquement des bébés, jamais plus de 4 créatures par cycle. */
    @GameTest(template = TEMPLATE, timeoutTicks = 900, batch = "uab_spawner")
    public static void babySpawnerProducesOnlyBabies(GameTestHelper h) {
        ServerPlayer p = survivalPlayer(h);
        BlockPos pos = h.absolutePos(CENTER);
        p.moveTo(pos.getX() + 6.5, pos.getY(), pos.getZ() + 6.5);
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        h.getLevel().setBlock(pos, com.ultimateautobuilder.registry.ModBlocks.ADVENTURE_SPAWNER.get().defaultBlockState()
                .setValue(com.ultimateautobuilder.block.Props.CATEGORY, com.ultimateautobuilder.block.Props.Category.BABY)
                .setValue(com.ultimateautobuilder.block.Props.FAMILY, com.ultimateautobuilder.block.Props.Family.MIXED), 3);
        h.succeedWhen(() -> {
            var mobs = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(8),
                    m -> m.getPersistentData().contains(com.ultimateautobuilder.adventure.MobVariants.TAG_VARIANT));
            h.assertTrue(mobs.size() <= 4, "Plus de 4 créatures : " + mobs.size());
            for (var m : mobs) {
                h.assertTrue("bebe".equals(m.getPersistentData().getString(com.ultimateautobuilder.adventure.MobVariants.TAG_VARIANT)),
                        "Variante inattendue : " + m.getType());
                boolean baby = m.isBaby() || m.getScale() < 0.6F;
                h.assertTrue(baby, "Créature adulte dans le spawner bébé : " + m.getType() + " échelle " + m.getScale());
            }
            h.assertTrue(mobs.size() == 4, "Cycle en cours : " + mobs.size() + " / 4");
        });
    }

    /** Variantes du Warden et récompense : clé OP seulement pour une vraie victoire, une seule fois. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "uab_warden")
    public static void wardenVariantsAndReward(GameTestHelper h) {
        ServerPlayer p = survivalPlayer(h);
        p.getInventory().clearContent();
        BlockPos pos = h.absolutePos(CENTER);
        p.moveTo(pos.getX() + 10.5, pos.getY(), pos.getZ() + 10.5);
        java.util.Map<com.ultimateautobuilder.core.adventure.WardenVariant, net.minecraft.world.entity.LivingEntity> spawned = new java.util.EnumMap<>(
                com.ultimateautobuilder.core.adventure.WardenVariant.class);
        for (var v : com.ultimateautobuilder.core.adventure.WardenVariant.values()) {
            var e = com.ultimateautobuilder.adventure.MobVariants.spawn(h.getLevel(), pos, com.ultimateautobuilder.core.adventure.SpawnCategory.NORMAL,
                    new com.ultimateautobuilder.core.adventure.SpawnRules.Warden(v));
            h.assertTrue(e != null, "Warden " + v + " non placé");
            spawned.put(v, (net.minecraft.world.entity.LivingEntity) e);
        }
        var boss = spawned.get(com.ultimateautobuilder.core.adventure.WardenVariant.BOSS);
        var normal = spawned.get(com.ultimateautobuilder.core.adventure.WardenVariant.NORMAL);
        var baby = spawned.get(com.ultimateautobuilder.core.adventure.WardenVariant.BABY);
        h.assertTrue(baby.getScale() < normal.getScale() && normal.getScale() < boss.getScale(), "Tailles incohérentes");
        h.assertTrue(boss.getMaxHealth() > normal.getMaxHealth() && baby.getMaxHealth() < normal.getMaxHealth(), "Santé incohérente");
        var medium = spawned.get(com.ultimateautobuilder.core.adventure.WardenVariant.MEDIUM);
        // Le Warden est invulnérable pendant son émergence (vanilla) : tout se vérifie une fois debout.
        h.succeedWhen(() -> {
            for (var e : spawned.values()) {
                h.assertTrue(!e.isAlive() || e.getPose() != net.minecraft.world.entity.Pose.EMERGING, "Émergence en cours");
            }
            h.assertTrue(baby.getBbHeight() < 2.0F && boss.getBbHeight() > 3.5F, "Boîtes de collision : bébé " + baby.getBbHeight()
                    + ", boss " + boss.getBbHeight());
            // Disparition sans victoire (commande) : aucune clé.
            normal.hurt(h.getLevel().damageSources().genericKill(), 100000F);
            h.assertTrue(p.getInventory().countItem(ModItems.OP_KEY.get()) == 0, "Clé donnée sans victoire");
            // Vraie victoire du joueur : une clé OP, une seule.
            medium.hurt(h.getLevel().damageSources().playerAttack(p), 100000F);
            h.assertTrue(!medium.isAlive(), "Le Warden moyen devrait être vaincu");
            h.assertTrue(p.getInventory().countItem(ModItems.OP_KEY.get()) == 1, "Clé OP attendue : " + p.getInventory().countItem(ModItems.OP_KEY.get()));
            com.ultimateautobuilder.adventure.WardenEvents.onDeath(medium, h.getLevel().damageSources().playerAttack(p));
            h.assertTrue(p.getInventory().countItem(ModItems.OP_KEY.get()) == 1, "Récompense donnée deux fois");
            baby.discard();
            boss.discard();
        });
    }

    /** Kit de départ : contenu exact, une seule fois (reconnexion sans doublon). */
    @GameTest(template = TEMPLATE, timeoutTicks = 20, batch = "uab_kit")
    public static void starterKitGivenOnce(GameTestHelper h) {
        ServerPlayer p = survivalPlayer(h);
        p.getInventory().clearContent();
        var data = com.ultimateautobuilder.adventure.AdventureData.get(h.getLevel().getServer());
        // La connexion du joueur factice a pu donner le kit : on repart d'un joueur jamais équipé.
        data.player(p.getUUID()).kitGiven = false;
        p.getPersistentData().remove("PlayerPersisted");
        com.ultimateautobuilder.adventure.StarterKit.onLogin(p);
        var inv = p.getInventory();
        h.assertTrue(inv.countItem(net.minecraft.world.item.Items.WOODEN_PICKAXE) == 1 && inv.countItem(net.minecraft.world.item.Items.WOODEN_AXE) == 1
                && inv.countItem(net.minecraft.world.item.Items.WOODEN_SWORD) == 1, "Outils en bois manquants");
        h.assertTrue(inv.countItem(net.minecraft.world.item.Items.OAK_LOG) == 32 && inv.countItem(net.minecraft.world.item.Items.STONE) == 32,
                "32 bois et 32 pierres attendus");
        h.assertTrue(inv.countItem(net.minecraft.world.item.Items.CRAFTING_TABLE) == 1 && inv.countItem(ModItems.ULTIMATE_CRYSTAL.get()) == 1,
                "Établi et cristal attendus");
        h.assertTrue(inv.countItem(net.minecraft.world.item.Items.DIAMOND) == 0, "Aucun objet OP au départ");
        com.ultimateautobuilder.adventure.StarterKit.onLogin(p);
        h.assertTrue(inv.countItem(net.minecraft.world.item.Items.OAK_LOG) == 32, "Kit donné deux fois");
        h.succeed();
    }

    /** Ensemble en fer porté : 8 diamants et 16 émeraudes, une seule fois. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20, batch = "uab_armor")
    public static void armorSetRewardOnce(GameTestHelper h) {
        ServerPlayer p = survivalPlayer(h);
        p.getInventory().clearContent();
        com.ultimateautobuilder.adventure.ArmorRewards.claim(p, "iron");
        h.assertTrue(p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == 0, "Récompense sans l'ensemble complet");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS));
        com.ultimateautobuilder.adventure.ArmorRewards.claim(p, "iron");
        h.assertTrue(p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == 0, "Récompense avec trois pièces seulement");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
        com.ultimateautobuilder.adventure.ArmorRewards.claim(p, "iron");
        h.assertTrue(p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == 8
                && p.getInventory().countItem(net.minecraft.world.item.Items.EMERALD) == 16, "8 diamants et 16 émeraudes attendus");
        com.ultimateautobuilder.adventure.ArmorRewards.claim(p, "iron");
        h.assertTrue(p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == 8, "Récompense donnée deux fois");
        h.succeed();
    }

    /** Placeur d'aventure : maison posée, coffre marqué rempli par sa table de butin, porte présente. */
    @GameTest(template = TEMPLATE, timeoutTicks = 600, batch = "uab_placer")
    public static void adventureHousePlaced(GameTestHelper h) {
        var bp = com.ultimateautobuilder.core.adventure.AdventureGen.build(com.ultimateautobuilder.core.adventure.StructureType.HIDDEN_CHEST_HOUSE, 42L);
        BlockPos o = h.absolutePos(new BlockPos(18, 0, 18));
        int[][] box = new int[1][];
        com.ultimateautobuilder.adventure.StructurePlacer.enqueue(h.getLevel(), bp, o.getX(), o.getY(), o.getZ(), 0, b -> box[0] = b);
        h.succeedWhen(() -> {
            h.assertTrue(box[0] != null, "Pose en cours");
            for (var mk : bp.markers()) {
                BlockPos c = o.offset(mk.x(), mk.y(), mk.z());
                h.assertTrue(h.getLevel().getBlockEntity(c) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity be
                        && be.getLootTable() != null, "Coffre sans butin en " + c);
            }
            int[] e = bp.entrance();
            BlockPos door = o.offset(e[0], 1, e[2] - 3);
            h.assertTrue(h.getLevel().getBlockState(door).getBlock() instanceof net.minecraft.world.level.block.DoorBlock, "Porte absente en " + door);
        });
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

    private static void give(ServerPlayer player, Item item, int count) {
        for (int left = count; left > 0; ) {
            int n = Math.min(left, item.getDefaultMaxStackSize());
            player.getInventory().add(new ItemStack(item, n));
            left -= n;
        }
    }
}
