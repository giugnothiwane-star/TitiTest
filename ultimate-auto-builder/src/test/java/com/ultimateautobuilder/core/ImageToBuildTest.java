package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.image.BlockPalette;
import com.ultimateautobuilder.core.image.BuildPlanReport;
import com.ultimateautobuilder.core.image.BuildStyle;
import com.ultimateautobuilder.core.image.ImageAnalysis;
import com.ultimateautobuilder.core.image.ImageBuildProject;
import com.ultimateautobuilder.core.image.ImageData;
import com.ultimateautobuilder.core.image.ImageLoader;
import com.ultimateautobuilder.core.image.ImageModel;
import com.ultimateautobuilder.core.image.ModelEditor;
import com.ultimateautobuilder.core.image.ModelViews;
import com.ultimateautobuilder.core.image.Reconstructor;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.model.VoxelModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Image-to-Build : chargement, analyse, reconstruction, styles, plan, éditeur et projets. */
class ImageToBuildTest {
    @TempDir
    Path tmp;

    // ------------------------------------------------------------------ images de synthèse

    /** Maison : murs gris, toit rouge triangulaire, fenêtres bleu foncé, fond blanc uni. */
    static ImageData house() {
        int w = 120, h = 100;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = 0xFFFFFFFF;
                if (y >= 50 && y < 95 && x >= 20 && x < 100) {
                    c = 0xFF8A8A8A;
                    if (y >= 60 && y < 72 && ((x >= 30 && x < 42) || (x >= 78 && x < 90))) {
                        c = 0xFF1E2A5A;
                    }
                } else if (y >= 15 && y < 50) {
                    int half = (y - 15) * 45 / 35;
                    if (Math.abs(x - 60) <= half) {
                        c = 0xFFB0302A;
                    }
                }
                px[y * w + x] = c;
            }
        }
        return new ImageData(w, h, px);
    }

    /** Pixel art 16×16 sur fond transparent, quatre couleurs. */
    static ImageData pixelArt() {
        int[] px = new int[16 * 16];
        for (int y = 2; y < 14; y++) {
            for (int x = 3; x < 13; x++) {
                px[y * 16 + x] = y < 6 ? 0xFFFFD700 : (x + y) % 2 == 0 ? 0xFF20A040 : 0xFF2050C0;
            }
        }
        px[8 * 16 + 8] = 0xFFFF0000;
        return new ImageData(16, 16, px);
    }

    /** Disque coloré (vue de côté / de face d'un objet arrondi). */
    static ImageData disc(int size, int color) {
        int[] px = new int[size * size];
        double r = size * 0.4;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - size / 2.0, dy = y - size / 2.0;
                px[y * size + x] = dx * dx + dy * dy <= r * r ? color : 0xFF101010;
            }
        }
        return new ImageData(size, size, px);
    }

    static byte[] png(ImageData img) throws IOException {
        BufferedImage bi = new BufferedImage(img.width(), img.height(), BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, img.width(), img.height(), img.argb(), 0, img.width());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", out);
        return out.toByteArray();
    }

    static Reconstructor.Options opts(Reconstructor.Mode mode, BuildStyle style, int height) {
        return new Reconstructor.Options(mode, Reconstructor.BackMode.SYMMETRIC, style, null, 0, height, 0, Reconstructor.Detail.MEDIUM, false,
                ImageAnalysis.Subject.UNKNOWN, false, false);
    }

    // ------------------------------------------------------------------ chargement

    @Test
    void loadsPngAndKeepsDimensions() throws Exception {
        ImageData img = ImageLoader.load(png(house()));
        assertEquals(120, img.width());
        assertEquals(100, img.height());
        assertEquals(house().get(60, 80) | 0xFF000000, img.get(60, 80) | 0xFF000000);
    }

    @Test
    void rejectsEmptyCorruptedAndUnsupportedFiles() throws IOException {
        assertThrows(ImportException.class, () -> ImageLoader.load(new byte[0]));
        assertThrows(ImportException.class, () -> ImageLoader.load(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}));
        byte[] truncated = java.util.Arrays.copyOf(png(house()), 60);
        assertThrows(ImportException.class, () -> ImageLoader.load(truncated));
        Path webp = tmp.resolve("photo.webp");
        Files.write(webp, new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'});
        ImportException e = assertThrows(ImportException.class, () -> ImageLoader.load(webp));
        assertTrue(e.getMessage().contains("PNG"), e.getMessage());
        assertThrows(ImportException.class, () -> ImageLoader.load(tmp.resolve("absent.png")));
    }

    @Test
    void rejectsOversizedDimensionsBeforeDecoding() throws IOException {
        // 20 000 × 1 px : fichier minuscule mais côté supérieur à la limite.
        BufferedImage bi = new BufferedImage(20_000, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", out);
        ImportException e = assertThrows(ImportException.class, () -> ImageLoader.load(out.toByteArray()));
        assertTrue(e.getMessage().contains("Dimensions"), e.getMessage());
    }

    @Test
    void largeImagesAreReducedToWorkingSize() throws Exception {
        BufferedImage bi = new BufferedImage(3000, 1500, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", out);
        ImageData img = ImageLoader.load(out.toByteArray());
        assertTrue(Math.max(img.width(), img.height()) <= ImageLoader.WORK_SIDE);
        assertEquals(2.0, (double) img.width() / img.height(), 0.05);
    }

    // ------------------------------------------------------------------ analyse

    @Test
    void separatesSubjectFromUniformBackground() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        assertEquals(ImageAnalysis.Background.BORDER, a.background());
        assertFalse(a.inMask(2, 2), "le coin (fond) ne doit pas faire partie du sujet");
        assertTrue(a.inMask(60, 80), "le mur doit faire partie du sujet");
        assertTrue(a.inMask(35, 65), "les fenêtres intérieures restent dans le sujet");
        // La base du toit (demi-largeur 43 px autour de x = 60) déborde des murs.
        assertEquals(17, a.x0(), 1);
        assertEquals(103, a.x1(), 1);
        assertEquals(15, a.y0(), 1);
    }

    @Test
    void usesAlphaChannelWhenPresent() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(pixelArt());
        assertEquals(ImageAnalysis.Background.ALPHA, a.background());
        assertTrue(a.pixelArt());
        assertEquals(ImageAnalysis.Subject.PIXEL_ART, a.best().subject());
    }

    @Test
    void houseIsGuessedAsBuildingWithReason() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        assertEquals(ImageAnalysis.Subject.BUILDING, a.best().subject(), a.guesses().toString());
        assertTrue(a.best().confidence() < 1.0, "une estimation n'est jamais certaine");
        assertFalse(a.best().reason().isBlank());
    }

    @Test
    void fullFrameImageFallsBackToWholeImage() {
        int[] px = new int[64 * 64];
        for (int i = 0; i < px.length; i++) {
            px[i] = 0xFF000000 | (i * 2654435761L % 0xFFFFFF > 0 ? (int) (i * 2654435761L % 0xFFFFFF) : 0);
        }
        ImageAnalysis.Result a = ImageAnalysis.analyze(new ImageData(64, 64, px));
        assertEquals(64, a.boxWidth());
        assertEquals(64, a.boxHeight());
    }

    // ------------------------------------------------------------------ reconstruction

    @Test
    void reconstructionHonoursRequestedHeightAndProportions() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        Reconstructor.Result r = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.BUILDING, BuildStyle.FIDELE, 40));
        ImageModel m = r.model();
        assertEquals(40, m.sizeY());
        assertEquals(40.0 * a.boxWidth() / a.boxHeight(), m.sizeX(), 1.0);
        assertTrue(m.sizeZ() >= 3);
        assertEquals(Reconstructor.Mode.BUILDING, r.usedMode());
    }

    @Test
    void visibleAndEstimatedBlocksAreDistinguished() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        Reconstructor.Result r = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.INFLATE, BuildStyle.FIDELE, 32));
        assertTrue(r.visible() > 0);
        assertTrue(r.estimated() > 0);
        assertEquals(r.model().count(), r.visible() + r.estimated());
        assertTrue(r.uncertainties().stream().anyMatch(u -> u.contains("Profondeur")), r.uncertainties().toString());
        assertTrue(r.uncertainties().stream().anyMatch(u -> u.contains("Arrière")), r.uncertainties().toString());
    }

    @Test
    void frontFaceColorsMatchImage() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.FLAT, BuildStyle.FIDELE, 40)).model();
        // Toit (haut, centre) rouge ; mur (bas) gris.
        String roof = m.get(m.sizeX() / 2, m.sizeY() - 4, 0), wall = m.get(m.sizeX() / 2, 3, 0);
        assertNotNull(roof);
        assertNotNull(wall);
        int rc = BlockPalette.colorOf(roof), wc = BlockPalette.colorOf(wall);
        assertTrue(((rc >> 16) & 255) > ((rc >> 8) & 255) + 30, "toit rouge attendu, obtenu " + roof);
        assertTrue(Math.abs(((wc >> 16) & 255) - (wc & 255)) < 40, "mur gris attendu, obtenu " + wall);
        // Coin supérieur hors toit : vide.
        assertFalse(m.filled(0, m.sizeY() - 1, 0));
    }

    @Test
    void multiViewHullIsIntersectionOfSilhouettes() {
        ImageAnalysis.Result front = ImageAnalysis.analyze(disc(64, 0xFF3060D0));
        ImageAnalysis.Result side = ImageAnalysis.analyze(disc(64, 0xFFD06030));
        Reconstructor.Result r = Reconstructor.reconstruct(new Reconstructor.Views(front, side, null, null),
                opts(Reconstructor.Mode.AUTO, BuildStyle.FIDELE, 24));
        assertEquals(Reconstructor.Mode.MULTIVIEW, r.usedMode());
        ImageModel m = r.model();
        int c = m.sizeX() / 2;
        assertTrue(m.filled(c, m.sizeY() / 2, m.sizeZ() / 2) || m.filled(c, m.sizeY() / 2, 0));
        // Coin du cube englobant : hors des deux disques.
        assertFalse(m.filled(0, 0, 0));
        assertFalse(m.filled(m.sizeX() - 1, m.sizeY() - 1, m.sizeZ() - 1));
    }

    @Test
    void backModesChangeOnlyTheBack() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        Reconstructor.Options sym = opts(Reconstructor.Mode.FLAT, BuildStyle.FIDELE, 30);
        Reconstructor.Options flat = new Reconstructor.Options(Reconstructor.Mode.FLAT, Reconstructor.BackMode.FLAT_COLOR, BuildStyle.FIDELE, null,
                0, 30, 3, Reconstructor.Detail.MEDIUM, false, ImageAnalysis.Subject.UNKNOWN, false, false);
        ImageModel ms = Reconstructor.reconstruct(Reconstructor.Views.single(a), sym).model();
        ImageModel mf = Reconstructor.reconstruct(Reconstructor.Views.single(a), flat).model();
        Set<String> backBlocks = new HashSet<>();
        int z = mf.sizeZ() - 1;
        for (int y = 0; y < mf.sizeY(); y++) {
            for (int x = 0; x < mf.sizeX(); x++) {
                if (mf.get(x, y, z) != null) {
                    backBlocks.add(mf.get(x, y, z));
                }
            }
        }
        assertEquals(1, backBlocks.size(), "arrière uni : une seule couleur");
        assertEquals(ms.get(5, 3, 0), mf.get(5, 3, 0), "la face avant ne dépend pas du mode arrière");
    }

    @Test
    void vanillaStyleUsesNoConcreteOrWool() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.FLAT, BuildStyle.VANILLA, 30)).model();
        for (String s : m.materials().keySet()) {
            assertFalse(s.contains("concrete") || s.contains("wool"), s);
        }
    }

    @Test
    void voxelArtLimitsColorCount() {
        int w = 64;
        int[] px = new int[w * w];
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                px[y * w + x] = 0xFF000000 | (x * 4) << 16 | (y * 4) << 8 | 128;
            }
        }
        ImageAnalysis.Result a = ImageAnalysis.analyze(new ImageData(w, w, px));
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.FLAT, BuildStyle.VOXEL_ART, 48)).model();
        assertTrue(m.materials().size() <= BuildStyle.VOXEL_ART.posterize, "couleurs : " + m.materials().size());
    }

    @Test
    void miniatureAndGiantScaleTheModel() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel mini = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.FLAT, BuildStyle.MINIATURE, 40)).model();
        ImageModel giant = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.FLAT, BuildStyle.GEANT, 40)).model();
        assertEquals(20, mini.sizeY());
        assertEquals(80, giant.sizeY());
    }

    @Test
    void detailLevelCapsSize() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        Reconstructor.Options o = new Reconstructor.Options(Reconstructor.Mode.FLAT, null, BuildStyle.FIDELE, null, 0, 400, 0,
                Reconstructor.Detail.LOW, false, null, false, false);
        Reconstructor.Result r = Reconstructor.reconstruct(Reconstructor.Views.single(a), o);
        assertTrue(Math.max(r.model().sizeX(), r.model().sizeY()) <= Reconstructor.Detail.LOW.maxSide);
        assertFalse(r.notes().isEmpty());
    }

    @Test
    void customStyleRespectsChosenFamilies() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        Reconstructor.Options o = new Reconstructor.Options(Reconstructor.Mode.FLAT, null, BuildStyle.PERSONNALISE,
                EnumSet.of(BlockPalette.Family.WOOL), 0, 24, 1, Reconstructor.Detail.MEDIUM, false, null, false, false);
        for (String s : Reconstructor.reconstruct(Reconstructor.Views.single(a), o).model().materials().keySet()) {
            assertTrue(s.endsWith("_wool"), s);
        }
    }

    @Test
    void hollowBuildingKeepsFloorAndShell() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        Reconstructor.Options o = new Reconstructor.Options(Reconstructor.Mode.BUILDING, null, BuildStyle.FIDELE, null, 0, 30, 12,
                Reconstructor.Detail.MEDIUM, true, ImageAnalysis.Subject.BUILDING, false, false);
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), o).model();
        int cx = m.sizeX() / 2, cz = m.sizeZ() / 2;
        assertTrue(m.filled(cx, 0, cz), "plancher conservé");
        assertFalse(m.filled(cx, 5, cz), "intérieur vide");
        assertTrue(m.filled(cx, 5, 0), "façade pleine");
    }

    // ------------------------------------------------------------------ vues, plan, comparaison

    @Test
    void orthographicViewsAndLayersHaveModelDimensions() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.BUILDING, BuildStyle.FIDELE, 30)).model();
        ModelViews.Image front = ModelViews.render(m, ModelViews.View.FRONT, false);
        ModelViews.Image left = ModelViews.render(m, ModelViews.View.LEFT, true);
        ModelViews.Image top = ModelViews.render(m, ModelViews.View.TOP, false);
        assertEquals(m.sizeX(), front.width());
        assertEquals(m.sizeY(), front.height());
        assertEquals(m.sizeZ(), left.width());
        assertEquals(m.sizeZ(), top.height());
        ModelViews.Image layer = ModelViews.layer(m, 0);
        assertEquals(m.sizeX() * m.sizeZ(), layer.argb().length);
    }

    @Test
    void comparisonIsMeasuredNotInvented() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.FLAT, BuildStyle.FIDELE, 50)).model();
        ModelViews.Comparison c = ModelViews.compare(a, m);
        assertTrue(c.comparedPixels() > 0);
        assertTrue(c.silhouetteOverlap() > 0.85 && c.silhouetteOverlap() <= 1.0, "recouvrement " + c.silhouetteOverlap());
        assertTrue(c.meanDeltaE() >= 0 && c.meanDeltaE() < 25, "ΔE " + c.meanDeltaE());
    }

    @Test
    void planReportCountsEveryBlockOnce() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.INFLATE, BuildStyle.FIDELE, 32)).model();
        BuildPlanReport.Report r = BuildPlanReport.of(m, 400);
        int mats = r.materials().stream().mapToInt(BuildPlanReport.Material::count).sum();
        int steps = r.steps().stream().mapToInt(BuildPlanReport.Step::blocks).sum();
        assertEquals(m.count(), r.blocks());
        assertEquals(r.blocks(), mats);
        assertEquals(r.blocks(), steps);
        assertTrue(r.steps().size() <= 4);
        String plan = BuildPlanReport.textPlan(m, "Maison");
        assertTrue(plan.contains("Couche Y=0"));
        assertTrue(plan.contains("Légende"));
    }

    @Test
    void conversionToEngineModelKeepsAllBlocks() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.INFLATE, BuildStyle.FIDELE, 32)).model();
        VoxelModel vm = m.toVoxelModel();
        assertEquals(m.count(), vm.size());
        assertEquals(m.sizeY(), vm.sizeY());
    }

    // ------------------------------------------------------------------ éditeur et projet

    @Test
    void editorUndoRedoRestoresExactState() {
        ImageAnalysis.Result a = ImageAnalysis.analyze(house());
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), opts(Reconstructor.Mode.FLAT, BuildStyle.FIDELE, 30)).model();
        ModelEditor ed = new ModelEditor(m);
        String main = m.materials().keySet().iterator().next();
        int n = ed.replace(main, "minecraft:gold_block");
        assertTrue(n > 0);
        assertEquals(n, ed.model().materials().get("minecraft:gold_block"));
        int sx = ed.model().sizeX(), sz = ed.model().sizeZ();
        ed.rotate();
        assertEquals(sz, ed.model().sizeX());
        assertEquals(sx, ed.model().sizeZ());
        assertTrue(ed.undo());
        assertTrue(ed.undo());
        assertEquals(m.materials(), ed.model().materials());
        assertTrue(ed.redo());
        assertEquals(n, ed.model().materials().get("minecraft:gold_block"));
        int before = ed.model().sizeY();
        ed.cropLayers(500, 600);
        assertTrue(ed.model().sizeY() >= 1 && ed.model().sizeY() <= before);
    }

    @Test
    void projectSaveLoadRoundTrip() throws Exception {
        ImageData img = house();
        ImageAnalysis.Result a = ImageAnalysis.analyze(img);
        Reconstructor.Options o = new Reconstructor.Options(Reconstructor.Mode.BUILDING, Reconstructor.BackMode.SIMPLIFIED, BuildStyle.REALISTE,
                EnumSet.of(BlockPalette.Family.STONE), 0, 28, 10, Reconstructor.Detail.HIGH, true, ImageAnalysis.Subject.BUILDING, false, true);
        ImageModel m = Reconstructor.reconstruct(Reconstructor.Views.single(a), o).model();
        ImageBuildProject.save(tmp, "Ma maison ../../évasion", img, null, null, null, o, m);
        assertEquals(1, ImageBuildProject.list(tmp).size());
        String folder = ImageBuildProject.list(tmp).get(0);
        assertFalse(folder.contains("."), folder);
        ImageBuildProject.Loaded l = ImageBuildProject.load(tmp, folder);
        assertEquals(o, l.options());
        assertEquals(m.count(), l.model().count());
        assertEquals(m.count(ImageModel.VISIBLE), l.model().count(ImageModel.VISIBLE));
        assertEquals(m.materials(), l.model().materials());
        assertEquals(img.width(), l.front().width());
        assertThrows(ImportException.class, () -> ImageBuildProject.load(tmp, "inexistant"));
    }

    @Test
    void volumeLimitIsEnforced() {
        assertThrows(IllegalArgumentException.class, () -> new ImageModel(512, 512, 512));
        assertThrows(IllegalArgumentException.class, () -> new ImageModel(0, 10, 10));
    }
}
