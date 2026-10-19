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
import com.ultimateautobuilder.registry.ModItems;
import net.minecraft.core.BlockPos;
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

    /** Personnage sculpté (Pikachu) en petite taille, avec rotation. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "uab_pikachu")
    public static void buildPikachu(GameTestHelper h) {
        PlaceJob job = submit(h, request(h, "pikachu", SizePreset.SMALL, 0, 1, BuildMode.NORMAL));
        h.succeedWhen(() -> {
            h.assertTrue(job.state() != Job.State.FAILED, "Échec : " + job.message());
            h.assertTrue(job.state() == Job.State.DONE, "Construction en cours : " + job.state());
            int[] b = job.bounds();
            h.assertTrue(b[4] - b[1] + 1 >= 6, "Hauteur inattendue : " + (b[4] - b[1] + 1));
            boolean yellow = false;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = b[0]; x <= b[3] && !yellow; x++) {
                for (int y = b[1]; y <= b[4] && !yellow; y++) {
                    for (int z = b[2]; z <= b[5] && !yellow; z++) {
                        BlockState s = h.getLevel().getBlockState(p.set(x, y, z));
                        yellow = s.is(Blocks.YELLOW_CONCRETE) || s.is(Blocks.YELLOW_WOOL) || s.is(Blocks.YELLOW_TERRACOTTA) || s.is(Blocks.GOLD_BLOCK);
                    }
                }
            }
            h.assertTrue(yellow, "Aucun bloc jaune : le modèle Pikachu n'a pas été posé correctement");
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

    /** L'objet Cristal de Glace est enregistré sous un identifiant stable. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20, batch = "uab_item")
    public static void crystalRegistered(GameTestHelper h) {
        h.assertTrue(ModItems.ICE_CRYSTAL.getId().toString().equals("ultimateautobuilder:ice_crystal"), "Identifiant inattendu");
        h.assertTrue(ModItems.ICE_CRYSTAL.get().getDefaultInstance().getMaxStackSize() == 1, "Le cristal doit être non empilable");
        h.succeed();
    }
}
