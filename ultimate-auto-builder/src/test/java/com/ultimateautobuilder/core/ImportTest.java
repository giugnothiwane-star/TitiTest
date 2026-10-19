package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.core.importer.RawStructure;
import com.ultimateautobuilder.core.importer.VoxImporter;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.validation.Limits;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportTest {
    @Test
    void codecRoundTrip() throws Exception {
        VoxelModel m = DefaultCatalog.create().get("maison_bois").generate(SizePreset.NORMAL, 0, false);
        byte[] data = ModelCodec.encode("Ma maison", m);
        ModelCodec.Named back = ModelCodec.decode(data, Limits.DEFAULT);
        assertEquals("Ma maison", back.name());
        assertEquals(m.size(), back.model().size());
        assertEquals(m.anchorY(), back.model().anchorY());
        assertArrayEquals(m.paletteCopy(), back.model().paletteCopy());
        for (int i = 0; i < m.size(); i += 97) {
            assertEquals(m.cell(i), back.model().cell(i));
        }
    }

    @Test
    void codecRejectsMalformedFiles() throws Exception {
        VoxelModel m = DefaultCatalog.create().get("pikachu").generate(SizePreset.SMALL, 0, false);
        byte[] good = ModelCodec.encode("x", m);
        assertThrows(ImportException.class, () -> ModelCodec.decode(Arrays.copyOf(good, good.length / 2), Limits.DEFAULT), "tronqué");
        assertThrows(ImportException.class, () -> ModelCodec.decode(new byte[]{1, 2, 3, 4, 5}, Limits.DEFAULT), "pas du gzip");
        assertThrows(ImportException.class, () -> ModelCodec.decode(gzip(out -> {
            out.writeInt(0x12345678);
        }), Limits.DEFAULT), "mauvaise signature");
        // En-tête valide mais palette contenant un état invalide
        assertThrows(ImportException.class, () -> ModelCodec.decode(gzip(out -> {
            out.writeInt(0x5541424D);
            out.writeShort(1);
            out.writeUTF("n");
            for (int i = 0; i < 6; i++) {
                out.writeShort(i < 3 ? 2 : 0);
            }
            out.writeShort(1);
            out.writeUTF("pas un état]");
        }), Limits.DEFAULT));
        // Nombre de blocs incohérent avec le volume
        assertThrows(ImportException.class, () -> ModelCodec.decode(gzip(out -> {
            out.writeInt(0x5541424D);
            out.writeShort(1);
            out.writeUTF("n");
            for (int i = 0; i < 6; i++) {
                out.writeShort(i < 3 ? 2 : 0);
            }
            out.writeShort(1);
            out.writeUTF("minecraft:stone");
            out.writeInt(1_000_000);
        }), Limits.DEFAULT));
        // Dimensions supérieures aux limites
        assertThrows(ImportException.class, () -> ModelCodec.decode(good, new Limits(10, 2, 5, 16)));
    }

    @Test
    void rawStructureEnforcesLimits() throws Exception {
        Limits limits = new Limits(3, 8, 2, 4);
        assertThrows(ImportException.class, () -> new RawStructure(9, 1, 1, limits));
        assertThrows(ImportException.class, () -> new RawStructure(0, 1, 1, limits));
        RawStructure raw = new RawStructure(4, 4, 4, limits);
        int stone = raw.addPaletteEntry("minecraft:stone");
        int bad = raw.addPaletteEntry("<script>");
        raw.put(0, 0, 0, stone);
        raw.put(1, 0, 0, bad);
        assertEquals(1, raw.skippedInvalid());
        assertThrows(ImportException.class, () -> raw.put(4, 0, 0, stone), "hors dimensions");
        assertThrows(ImportException.class, () -> raw.put(0, 0, 0, 99), "palette");
        raw.put(1, 1, 1, stone);
        raw.put(2, 2, 2, stone);
        assertThrows(ImportException.class, () -> raw.put(3, 3, 3, stone), "trop de blocs");
        raw.addPaletteEntry("minecraft:dirt");
        raw.addPaletteEntry("minecraft:dirt");
        assertThrows(ImportException.class, () -> raw.addPaletteEntry("minecraft:sand"), "palette trop grande");
        assertEquals(3, raw.toModel().size());
    }

    @Test
    void voxImportMapsAxesAndColors() throws Exception {
        byte[] vox = buildVox(3, 2, 4, new int[][]{{0, 0, 0, 1}, {2, 1, 3, 2}}, new int[]{0xFF0000FF, 0xFF00FF00});
        VoxelModel m = VoxImporter.read(vox, Limits.DEFAULT);
        assertEquals(2, m.size());
        // Z de MagicaVoxel devient la hauteur
        assertTrue(m.sizeY() == 4, "hauteur " + m.sizeY());
        assertTrue(Arrays.asList(m.paletteCopy()).stream().anyMatch(s -> s.contains("red")), Arrays.toString(m.paletteCopy()));
        assertTrue(Arrays.asList(m.paletteCopy()).stream().anyMatch(s -> s.contains("lime") || s.contains("green") || s.contains("emerald")), Arrays.toString(m.paletteCopy()));
    }

    @Test
    void voxImportRejectsMalformedFiles() throws Exception {
        assertThrows(ImportException.class, () -> VoxImporter.read(new byte[]{'V', 'O', 'X'}, Limits.DEFAULT));
        assertThrows(ImportException.class, () -> VoxImporter.read("NOTAVOXFILE.....".getBytes(), Limits.DEFAULT));
        byte[] vox = buildVox(3, 2, 4, new int[][]{{0, 0, 0, 1}}, null);
        assertThrows(ImportException.class, () -> VoxImporter.read(Arrays.copyOf(vox, vox.length - 6), Limits.DEFAULT));
        byte[] outOfBounds = buildVox(2, 2, 2, new int[][]{{5, 0, 0, 1}}, null);
        assertThrows(ImportException.class, () -> VoxImporter.read(outOfBounds, Limits.DEFAULT));
        byte[] huge = buildVox(1000, 2, 2, new int[][]{{0, 0, 0, 1}}, null);
        assertThrows(ImportException.class, () -> VoxImporter.read(huge, Limits.DEFAULT));
    }

    interface Writer {
        void write(DataOutputStream out) throws Exception;
    }

    private static byte[] gzip(Writer w) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            w.write(out);
        }
        return bytes.toByteArray();
    }

    /** Construit un fichier .vox minimal (MAIN → SIZE, XYZI, RGBA optionnel). */
    static byte[] buildVox(int sx, int sy, int sz, int[][] voxels, int[] colors) {
        int sizeChunk = 12 + 12;
        int xyziChunk = 12 + 4 + voxels.length * 4;
        int rgbaChunk = colors == null ? 0 : 12 + 1024;
        ByteBuffer b = ByteBuffer.allocate(8 + 12 + sizeChunk + xyziChunk + rgbaChunk).order(ByteOrder.LITTLE_ENDIAN);
        b.put("VOX ".getBytes()).putInt(150);
        b.put("MAIN".getBytes()).putInt(0).putInt(sizeChunk + xyziChunk + rgbaChunk);
        b.put("SIZE".getBytes()).putInt(12).putInt(0).putInt(sx).putInt(sy).putInt(sz);
        b.put("XYZI".getBytes()).putInt(4 + voxels.length * 4).putInt(0).putInt(voxels.length);
        for (int[] v : voxels) {
            b.put((byte) v[0]).put((byte) v[1]).put((byte) v[2]).put((byte) v[3]);
        }
        if (colors != null) {
            b.put("RGBA".getBytes()).putInt(1024).putInt(0);
            for (int i = 0; i < 256; i++) {
                int c = i < colors.length ? colors[i] : 0xFF808080;
                // Stockage r, g, b, a ; la couleur i du fichier est l'index i + 1.
                b.put((byte) (c & 255)).put((byte) ((c >> 8) & 255)).put((byte) ((c >> 16) & 255)).put((byte) ((c >>> 24) & 255));
            }
        }
        return b.array();
    }
}
