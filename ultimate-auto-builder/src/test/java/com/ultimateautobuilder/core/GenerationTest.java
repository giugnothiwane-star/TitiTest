package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.render.IsoRenderer;
import com.ultimateautobuilder.core.validation.Limits;
import com.ultimateautobuilder.core.validation.StructureValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Génère réellement chaque modèle du catalogue et vérifie les invariants. */
class GenerationTest {
    private static Catalog catalog;

    @BeforeAll
    static void load() {
        catalog = DefaultCatalog.create();
    }

    @Test
    void everyModelGeneratesValidVoxelsAtSmallAndNormalSize() {
        for (ModelEntry e : catalog.all()) {
            for (SizePreset preset : new SizePreset[]{SizePreset.SMALL, SizePreset.NORMAL}) {
                VoxelModel m = e.generate(preset, 0, true);
                assertFalse(m.isEmpty(), e.id() + " " + preset + " est vide");
                assertTrue(m.solidCount() > 0, e.id() + " ne contient que de l'air");
                assertTrue(Math.max(m.sizeX(), Math.max(m.sizeY(), m.sizeZ())) <= Limits.DEFAULT.maxDimension(), e.id());
                for (int i = 0; i < m.paletteSize(); i++) {
                    assertTrue(StructureValidator.isValidState(m.paletteEntry(i)), e.id() + " : état invalide " + m.paletteEntry(i));
                }
                assertTrue(m.anchorX() >= 0 && m.anchorX() < Math.max(1, m.sizeX()) + 64, e.id());
            }
        }
    }

    @Test
    void largePresetStaysWithinDefaultLimits() {
        for (ModelEntry e : catalog.all()) {
            VoxelModel m = e.generate(SizePreset.LARGE, 0, true);
            assertTrue(m.size() <= Limits.DEFAULT.maxBlocks(), e.id() + " : " + m.size() + " blocs");
        }
    }

    @Test
    void generationIsDeterministic() {
        for (String id : new String[]{"pikachu", "chene_geant", "quartier_residentiel", "labyrinthe", "ile_flottante", "village_medieval"}) {
            ModelEntry e = catalog.get(id);
            VoxelModel a = e.generate(SizePreset.NORMAL, 0, false);
            VoxelModel b = e.generate(SizePreset.NORMAL, 0, false);
            assertEquals(a.size(), b.size(), id);
            long[] ca = new long[a.size()];
            long[] cb = new long[b.size()];
            for (int i = 0; i < a.size(); i++) {
                ca[i] = a.cell(i);
                cb[i] = b.cell(i);
            }
            assertArrayEquals(ca, cb, id);
            assertArrayEquals(a.paletteCopy(), b.paletteCopy(), id);
        }
    }

    @Test
    void sculptedCharactersReachTheRequestedHeight() {
        for (String id : new String[]{"pikachu", "goku", "dracaufeu", "naruto", "statue_liberte", "chene_geant"}) {
            ModelEntry e = catalog.get(id);
            assertEquals(SizeSpec.Kind.HEIGHT, e.sizes().kind());
            for (SizePreset p : new SizePreset[]{SizePreset.SMALL, SizePreset.NORMAL, SizePreset.LARGE, SizePreset.GIANT}) {
                int target = e.sizes().resolve(p, 0);
                VoxelModel m = e.generate(p, 0, true);
                assertTrue(Math.abs(m.sizeY() - target) <= Math.max(1, target / 20) + 1,
                        id + " " + p + " : hauteur " + m.sizeY() + " pour " + target);
            }
        }
    }

    @Test
    void scalingKeepsProportionsAndHasNoHoles() {
        ModelEntry e = catalog.get("pikachu");
        VoxelModel small = e.generate(40, false);
        VoxelModel big = e.generate(80, false);
        double ratioSmall = (double) small.sizeX() / small.sizeY();
        double ratioBig = (double) big.sizeX() / big.sizeY();
        assertEquals(ratioSmall, ratioBig, 0.08);
        // Sans évidement, une coupe horizontale au milieu du corps est pleine (pas de trou de mise à l'échelle).
        int y = big.sizeY() / 4;
        int cx = big.anchorX(), cz = big.anchorZ();
        for (int dx = -3; dx <= 3; dx++) {
            assertTrue(big.stateAt(cx + dx, y, cz) != null, "trou à " + dx);
        }
    }

    @Test
    void hollowingReducesBlockCountOfGiantStatues() {
        ModelEntry e = catalog.get("ronflex");
        VoxelModel full = e.generate(60, false);
        VoxelModel hollow = e.generate(60, true);
        assertTrue(hollow.size() < full.size() * 0.6, full.size() + " → " + hollow.size());
    }

    @Test
    void customSizeIsClamped() {
        ModelEntry e = catalog.get("pikachu");
        assertEquals(e.sizes().max(), e.sizes().resolve(SizePreset.CUSTOM, 100_000));
        assertEquals(e.sizes().min(), e.sizes().resolve(SizePreset.CUSTOM, -5));
    }

    @Test
    void thumbnailsRender() {
        VoxelModel m = catalog.get("goku").generate(SizePreset.NORMAL, 0, false);
        IsoRenderer.Image img = IsoRenderer.fit(IsoRenderer.render(m, 0, 50_000), 64);
        assertEquals(64 * 64, img.argb().length);
        assertTrue(Arrays.stream(img.argb()).anyMatch(c -> c != 0));
    }
}
