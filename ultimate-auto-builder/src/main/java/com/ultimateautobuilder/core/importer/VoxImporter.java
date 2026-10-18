package com.ultimateautobuilder.core.importer;

import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.render.ColorTable;
import com.ultimateautobuilder.core.validation.Limits;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Importeur MagicaVoxel (.vox, format 150/200). Seul le premier modèle (chunks SIZE/XYZI) est lu ;
 * chaque couleur de la palette RGBA est associée au bloc vanilla de teinte la plus proche.
 * L'axe vertical de MagicaVoxel (Z) devient l'axe Y de Minecraft.
 */
public final class VoxImporter {
    public static final int MAX_FILE_BYTES = 16 * 1024 * 1024;

    /** Blocs candidats pour la correspondance des couleurs (pleins, sans gravité, sans entité de bloc). */
    private static final String[] CANDIDATES;

    static {
        String[] dyes = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
                "purple", "blue", "brown", "green", "red", "black"};
        List<String> c = new ArrayList<>();
        for (String d : dyes) {
            c.add("minecraft:" + d + "_concrete");
            c.add("minecraft:" + d + "_terracotta");
            c.add("minecraft:" + d + "_wool");
        }
        String[] extra = {"minecraft:stone", "minecraft:andesite", "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
                "minecraft:dark_oak_planks", "minecraft:sandstone", "minecraft:smooth_quartz", "minecraft:snow_block", "minecraft:gold_block",
                "minecraft:diamond_block", "minecraft:emerald_block", "minecraft:lapis_block", "minecraft:iron_block", "minecraft:coal_block",
                "minecraft:bricks", "minecraft:terracotta", "minecraft:grass_block", "minecraft:dirt", "minecraft:packed_ice", "minecraft:obsidian",
                "minecraft:netherrack", "minecraft:prismarine", "minecraft:copper_block", "minecraft:amethyst_block", "minecraft:glowstone"};
        c.addAll(List.of(extra));
        CANDIDATES = c.toArray(new String[0]);
    }

    private VoxImporter() {
    }

    public static VoxelModel read(byte[] data, Limits limits) throws ImportException {
        if (data == null || data.length < 8) {
            throw new ImportException("Fichier .vox vide ou tronqué");
        }
        if (data.length > MAX_FILE_BYTES) {
            throw new ImportException("Fichier .vox trop volumineux");
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        String magic = readId(buf);
        if (!"VOX ".equals(magic)) {
            throw new ImportException("Signature .vox absente");
        }
        buf.getInt(); // version
        int[] size = null;
        int[] voxels = null;
        int voxelCount = 0;
        int[] palette = null;
        try {
            while (buf.remaining() >= 12) {
                String id = readId(buf);
                int content = buf.getInt();
                int children = buf.getInt();
                if (content < 0 || children < 0 || content > buf.remaining()) {
                    throw new ImportException("Chunk " + id + " de taille invalide");
                }
                int start = buf.position();
                switch (id) {
                    case "MAIN" -> {
                        // Le chunk MAIN n'a pas de contenu propre : on descend dans ses enfants.
                        buf.position(start + content);
                        continue;
                    }
                    case "SIZE" -> {
                        if (size == null) {
                            size = new int[]{buf.getInt(), buf.getInt(), buf.getInt()};
                        }
                    }
                    case "XYZI" -> {
                        if (voxels == null) {
                            int n = buf.getInt();
                            if (n < 0 || n > limits.maxBlocks() || (long) n * 4 > content - 4) {
                                throw new ImportException("Nombre de voxels invalide : " + n);
                            }
                            voxels = new int[n];
                            for (int i = 0; i < n; i++) {
                                voxels[i] = buf.getInt();
                            }
                            voxelCount = n;
                        }
                    }
                    case "RGBA" -> {
                        palette = new int[256];
                        for (int i = 0; i < 256 && buf.position() + 4 <= start + content; i++) {
                            int r = buf.get() & 255, g = buf.get() & 255, b = buf.get() & 255, a = buf.get() & 255;
                            // L'index i du fichier correspond à la couleur i + 1 des voxels.
                            palette[(i + 1) & 255] = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                    }
                    default -> {
                    }
                }
                buf.position(start + content);
            }
        } catch (java.nio.BufferUnderflowException e) {
            throw new ImportException("Fichier .vox tronqué", e);
        }
        if (size == null || voxels == null) {
            throw new ImportException("Aucun modèle trouvé dans le fichier .vox");
        }
        // Axes : X → X, Z (haut) → Y, Y → Z
        RawStructure raw = new RawStructure(size[0], size[2], size[1], limits);
        String[] mapped = new String[256];
        for (int i = 0; i < voxelCount; i++) {
            int v = voxels[i];
            int x = v & 255, y = (v >>> 8) & 255, z = (v >>> 16) & 255, ci = (v >>> 24) & 255;
            if (ci == 0) {
                continue;
            }
            if (mapped[ci] == null) {
                int argb = palette != null ? palette[ci] : defaultColor(ci);
                if (((argb >>> 24) & 255) == 0 && palette != null) {
                    mapped[ci] = "minecraft:air";
                } else {
                    mapped[ci] = nearestBlock(argb & 0xFFFFFF);
                }
            }
            raw.put(x, z, size[1] - 1 - y, mapped[ci]);
        }
        return raw.toModel();
    }

    /** Palette de repli (fichiers sans chunk RGBA) : dégradé de gris déterministe. */
    private static int defaultColor(int index) {
        int v = 255 - (index * 255 / 256);
        return 0xFF000000 | (v << 16) | (v << 8) | v;
    }

    public static String nearestBlock(int rgb) {
        String best = CANDIDATES[0];
        double bestD = Double.MAX_VALUE;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        for (String c : CANDIDATES) {
            int col = ColorTable.rgb(c);
            if (col < 0) {
                continue;
            }
            int cr = (col >> 16) & 255, cg = (col >> 8) & 255, cb = col & 255;
            // Distance pondérée (approximation perceptuelle « redmean »).
            double rm = (r + cr) / 2.0;
            double d = (2 + rm / 256) * (r - cr) * (r - cr) + 4 * (g - cg) * (g - cg) + (2 + (255 - rm) / 256) * (b - cb) * (b - cb);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    private static String readId(ByteBuffer buf) {
        byte[] id = new byte[4];
        buf.get(id);
        return new String(id, StandardCharsets.US_ASCII);
    }
}
