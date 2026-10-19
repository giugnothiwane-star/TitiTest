package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle décrit par des pavés en « pixels » de modèle Minecraft (1/16 de bloc), comme les
 * modèles d'entités du jeu. La voxelisation se fait à la hauteur demandée : chaque pavé est
 * converti en intervalles de blocs, avec au moins un bloc d'épaisseur pour que les petits
 * détails (yeux, museau) restent visibles en petite taille. L'ordre d'ajout fait foi :
 * un pavé ajouté plus tard recouvre les précédents (détails peints sur les faces).
 * Convention : y vers le haut depuis le sol, façade (visage) vers +Z, origine au centre.
 */
public final class BoxModel {
    /** Matériau fonction de la position en blocs (motifs déterministes). */
    @FunctionalInterface
    public interface Paint {
        String at(int x, int y, int z);
    }

    private record Box(double x0, double y0, double z0, double x1, double y1, double z1, Paint paint) {
    }

    private final List<Box> boxes = new ArrayList<>();
    private double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    private double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

    public static Paint solid(String state) {
        String s = state.contains(":") ? state : "minecraft:" + state;
        return (x, y, z) -> s;
    }

    /** Mélange pseudo-aléatoire stable de deux matériaux (pelage, peau marbrée). */
    public static Paint speckle(String a, String b, double ratioB, long seed) {
        String sa = a.contains(":") ? a : "minecraft:" + a;
        String sb = b.contains(":") ? b : "minecraft:" + b;
        return (x, y, z) -> {
            long h = seed ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
            h ^= h >>> 13;
            h *= 0x5bd1e995L;
            h ^= h >>> 15;
            return ((h & 0xFFFF) / 65535.0) < ratioB ? sb : sa;
        };
    }

    /** Bandes horizontales (abeille, poisson tropical, robe du vagabond). */
    public static Paint bandsY(String a, String b, int period) {
        String sa = a.contains(":") ? a : "minecraft:" + a;
        String sb = b.contains(":") ? b : "minecraft:" + b;
        int p = Math.max(1, period);
        return (x, y, z) -> Math.floorMod(y, 2 * p) < p ? sa : sb;
    }

    /** Bandes verticales le long de X. */
    public static Paint bandsX(String a, String b, int period) {
        String sa = a.contains(":") ? a : "minecraft:" + a;
        String sb = b.contains(":") ? b : "minecraft:" + b;
        int p = Math.max(1, period);
        return (x, y, z) -> Math.floorMod(x, 2 * p) < p ? sa : sb;
    }

    /** Bandes le long de Z (corps allongés vus de profil). */
    public static Paint bandsZ(String a, String b, int period) {
        String sa = a.contains(":") ? a : "minecraft:" + a;
        String sb = b.contains(":") ? b : "minecraft:" + b;
        int p = Math.max(1, period);
        return (x, y, z) -> Math.floorMod(z, 2 * p) < p ? sa : sb;
    }

    /** Pavé : coin (x, y, z) et dimensions (w, h, d) en pixels de modèle. */
    public BoxModel box(double x, double y, double z, double w, double h, double d, String state) {
        return box(x, y, z, w, h, d, solid(state));
    }

    public BoxModel box(double x, double y, double z, double w, double h, double d, Paint paint) {
        if (w <= 0 || h <= 0 || d <= 0) {
            throw new IllegalArgumentException("Pavé de dimensions nulles");
        }
        boxes.add(new Box(x, y, z, x + w, y + h, z + d, paint));
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y + h);
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x + w);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z + d);
        return this;
    }

    /** Pavé symétrique par rapport au plan x = 0 (pattes, bras, yeux). */
    public BoxModel sym(double x, double y, double z, double w, double h, double d, String state) {
        box(x, y, z, w, h, d, state);
        return box(-x - w, y, z, w, h, d, state);
    }

    public BoxModel sym(double x, double y, double z, double w, double h, double d, Paint paint) {
        box(x, y, z, w, h, d, paint);
        return box(-x - w, y, z, w, h, d, paint);
    }

    /** Détail peint sur la face avant d'un volume dont la façade est à z = front (épaisseur d'un demi-pixel). */
    public BoxModel face(double x, double y, double w, double h, double front, String state) {
        return box(x, y, front - 0.5, w, h, 0.5, state);
    }

    public BoxModel faceSym(double x, double y, double w, double h, double front, String state) {
        face(x, y, w, h, front, state);
        return face(-x - w, y, w, h, front, state);
    }

    public double heightPx() {
        return boxes.isEmpty() ? 0 : maxY - minY;
    }

    /** Plus grande dimension du modèle, en pixels. */
    public double mainDimensionPx() {
        return boxes.isEmpty() ? 0 : Math.max(heightPx(), Math.max(maxX - minX, maxZ - minZ));
    }

    /**
     * Voxelise de sorte que la plus grande dimension mesure {@code target} blocs (une tortue ou un
     * dragon ne deviennent pas démesurés parce qu'ils sont plus longs que hauts). L'ancre est au centre, au sol.
     */
    public VoxelModel build(int target) {
        if (boxes.isEmpty()) {
            return VoxelModel.EMPTY;
        }
        double s = Math.max(1, target) / mainDimensionPx();
        VoxelGrid g = new VoxelGrid();
        for (Box b : boxes) {
            int x0 = lo(b.x0 * s), x1 = hi(b.x0 * s, b.x1 * s);
            int y0 = lo((b.y0 - minY) * s), y1 = hi((b.y0 - minY) * s, (b.y1 - minY) * s);
            int z0 = lo(b.z0 * s), z1 = hi(b.z0 * s, b.z1 * s);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        g.set(x, y, z, b.paint.at(x, y, z));
                    }
                }
            }
        }
        return g.freeze().groundAtBottom();
    }

    private static int lo(double v) {
        return (int) Math.floor(v + 1e-6);
    }

    /** Dernier indice couvert ; au moins un bloc même pour un pavé plus fin qu'un bloc. */
    private static int hi(double a, double b) {
        return Math.max(lo(a), (int) Math.ceil(b - 1e-6) - 1);
    }
}
