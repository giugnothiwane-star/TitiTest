package com.ultimateautobuilder.core.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plan de construction calculé à partir du modèle réel : dimensions, matériaux, couches, étapes
 * (mêmes découpages que le moteur : par couche ou par quarts de hauteur) et plan texte exportable
 * donnant chaque bloc par coordonnées de couche.
 */
public final class BuildPlanReport {
    public record Material(String state, int count) {
        public int stacks() {
            return (count + 63) / 64;
        }

        public String label() {
            String id = state.contains("[") ? state.substring(0, state.indexOf('[')) : state;
            return id.startsWith("minecraft:") ? id.substring(10) : id;
        }
    }

    public record Step(int index, int fromY, int toY, int blocks) {
    }

    public record Report(int width, int height, int depth, int blocks, int visible, int estimated, List<Material> materials, int[] layers,
                         List<Step> steps, double seconds) {
    }

    private BuildPlanReport() {
    }

    /** @param blocksPerSecond rythme de pose du serveur (sert uniquement à l'estimation de durée affichée). */
    public static Report of(ImageModel m, int blocksPerSecond) {
        List<Material> mats = new ArrayList<>();
        for (Map.Entry<String, Integer> e : m.materials().entrySet()) {
            mats.add(new Material(e.getKey(), e.getValue()));
        }
        int[] layers = m.layerCounts();
        int total = 0;
        for (int c : layers) {
            total += c;
        }
        List<Step> steps = new ArrayList<>();
        int h = Math.max(1, m.sizeY());
        int start = 0, cur = 0, n = 0;
        for (int y = 0; y < m.sizeY(); y++) {
            int stage = (y * 4) / h;
            if (stage != cur) {
                steps.add(new Step(steps.size() + 1, start, y - 1, n));
                start = y;
                cur = stage;
                n = 0;
            }
            n += layers[y];
        }
        steps.add(new Step(steps.size() + 1, start, m.sizeY() - 1, n));
        double secs = blocksPerSecond <= 0 ? 0 : (double) total / blocksPerSecond;
        return new Report(m.sizeX(), m.sizeY(), m.sizeZ(), total, m.count(ImageModel.VISIBLE), m.count(ImageModel.ESTIMATED), List.copyOf(mats),
                layers, List.copyOf(steps), secs);
    }

    private static final String SYMBOLS = "#@%&*+=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789$?!<>^~";

    /** Plan texte : légende puis une grille par couche (x vers la droite, avant du modèle en bas). */
    public static String textPlan(ImageModel m, String name) {
        Report r = of(m, 0);
        StringBuilder sb = new StringBuilder();
        sb.append("Plan de construction : ").append(name).append('\n');
        sb.append(String.format(Locale.ROOT, "Dimensions : %d (largeur X) × %d (hauteur Y) × %d (profondeur Z), %d blocs%n", r.width(), r.height(),
                r.depth(), r.blocks()));
        sb.append("Origine : coin avant-gauche de la couche 0 ; « . » = vide\n\nLégende :\n");
        java.util.Map<String, Character> sym = new java.util.HashMap<>();
        int i = 0;
        for (Material mat : r.materials()) {
            char c = i < SYMBOLS.length() ? SYMBOLS.charAt(i) : '?';
            sym.put(mat.state(), c);
            sb.append("  ").append(c).append(" = ").append(mat.label()).append(" × ").append(mat.count()).append(" (").append(mat.stacks())
                    .append(" piles)\n");
            i++;
        }
        if (r.materials().size() > SYMBOLS.length()) {
            sb.append("  ? = matériaux supplémentaires (voir la liste du modèle)\n");
        }
        for (Step s : r.steps()) {
            sb.append("\nÉtape ").append(s.index()).append(" : couches ").append(s.fromY()).append(" à ").append(s.toY()).append(" (")
                    .append(s.blocks()).append(" blocs)\n");
        }
        for (int y = 0; y < m.sizeY(); y++) {
            if (r.layers()[y] == 0) {
                continue;
            }
            sb.append("\n--- Couche Y=").append(y).append(" (").append(r.layers()[y]).append(" blocs) ---\n");
            for (int z = m.sizeZ() - 1; z >= 0; z--) {
                sb.append(String.format(Locale.ROOT, "Z=%3d ", z));
                for (int x = 0; x < m.sizeX(); x++) {
                    String s = m.get(x, y, z);
                    sb.append(s == null ? '.' : sym.getOrDefault(s, '?'));
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
