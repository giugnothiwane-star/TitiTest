package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.render.IsoRenderer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Outil de contrôle visuel : rend chaque modèle en PNG isométrique (activé uniquement avec
 * -PpreviewDir=… ; filtre optionnel -PpreviewFilter=samourai,chevalier).
 */
class PreviewRenderTest {
    @Test
    void renderPreviews() throws Exception {
        String dir = System.getProperty("uab.previewDir", "");
        Assumptions.assumeTrue(!dir.isBlank(), "Rendu des aperçus désactivé");
        String filter = System.getProperty("uab.previewFilter", "");
        File out = new File(dir);
        out.mkdirs();
        Catalog catalog = DefaultCatalog.create();
        List<ModelEntry> entries = catalog.all();
        for (ModelEntry e : entries) {
            if (!filter.isBlank() && !matches(e, filter)) {
                continue;
            }
            long t = System.nanoTime();
            SizePreset preset = SizePreset.valueOf(System.getProperty("uab.previewSize", "NORMAL"));
            VoxelModel m = e.generate(preset, 0, false);
            long ms = (System.nanoTime() - t) / 1_000_000;
            IsoRenderer.Image img = IsoRenderer.render(m, 0, 400_000);
            int scale = Math.max(1, Math.min(4, 360 / Math.max(img.width(), img.height())));
            BufferedImage bi = new BufferedImage(img.width() * scale, img.height() * scale, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < img.height() * scale; y++) {
                for (int x = 0; x < img.width() * scale; x++) {
                    int c = img.argb()[(y / scale) * img.width() + x / scale];
                    bi.setRGB(x, y, c == 0 ? 0xFF202428 : c);
                }
            }
            ImageIO.write(bi, "png", new File(out, e.id() + ".png"));
            ImageIO.write(front(m), "png", new File(out, e.id() + "_front.png"));
            System.out.printf("%-24s %4dx%4dx%4d %8d blocs %5d ms%n", e.id(), m.sizeX(), m.sizeY(), m.sizeZ(), m.size(), ms);
        }
    }

    private static boolean matches(ModelEntry e, String filter) {
        for (String f : filter.split(",")) {
            String t = f.trim();
            if (t.endsWith("*") ? (e.id().startsWith(t.substring(0, t.length() - 1)) || e.category().key().equals(t.substring(0, t.length() - 1)))
                    : e.id().equals(t)) {
                return true;
            }
        }
        return false;
    }

    /** Vue de face orthographique (depuis +Z) : premier bloc visible le long de -Z. */
    private static BufferedImage front(VoxelModel m) {
        int sx = Math.max(1, m.sizeX()), sy = Math.max(1, m.sizeY());
        int scale = Math.max(1, Math.min(6, 300 / Math.max(sx, sy)));
        int[] depth = new int[sx * sy];
        int[] col = new int[sx * sy];
        java.util.Arrays.fill(depth, -1);
        for (int i = 0; i < m.size(); i++) {
            long c = m.cell(i);
            int rgb = com.ultimateautobuilder.core.render.ColorTable.rgb(m.stateOf(c));
            if (rgb < 0) continue;
            int x = VoxelModel.cx(c), y = VoxelModel.cy(c), z = VoxelModel.cz(c);
            int idx = (sy - 1 - y) * sx + x;
            if (z > depth[idx]) { depth[idx] = z; col[idx] = rgb; }
        }
        BufferedImage bi = new BufferedImage(sx * scale, sy * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < sy * scale; y++) for (int x = 0; x < sx * scale; x++) {
            int idx = (y / scale) * sx + x / scale;
            bi.setRGB(x, y, depth[idx] < 0 ? 0xFF202428 : 0xFF000000 | col[idx]);
        }
        return bi;
    }
}
