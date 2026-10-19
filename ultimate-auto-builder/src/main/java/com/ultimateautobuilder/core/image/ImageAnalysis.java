package com.ultimateautobuilder.core.image;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Analyse locale d'une image : séparation sujet/fond, mesures (netteté, luminosité, contraste,
 * symétrie, remplissage, rétrécissement en partie haute) et estimation heuristique du type de sujet.
 * Aucune intelligence artificielle ni service distant : uniquement des règles mesurables, dont le
 * résultat est présenté comme une estimation que le joueur peut corriger.
 */
public final class ImageAnalysis {
    /** Côté maximal de l'image utilisée pour l'analyse (coût borné). */
    public static final int ANALYSIS_SIDE = 256;

    public enum Subject {
        BUILDING("Bâtiment"), CHARACTER("Personnage"), ANIMAL("Animal / créature"), OBJECT("Objet"), PIXEL_ART("Logo / pixel art"),
        VEHICLE("Véhicule"), TREE("Arbre / plante"), LANDSCAPE("Paysage / scène"), UNKNOWN("Indéterminé");

        public final String label;

        Subject(String label) {
            this.label = label;
        }
    }

    public enum Background { ALPHA, BORDER, NONE }

    public record Guess(Subject subject, double confidence, String reason) {
    }

    /**
     * @param mask sujet (true) à la résolution {@code width × height} de l'image analysée
     * @param x0 boîte englobante du sujet (inclusive) dans l'image analysée
     */
    public record Result(ImageData image, boolean[] mask, int x0, int y0, int x1, int y1, Background background, double fillRatio,
                         double brightness, double contrast, double sharpness, double symmetry, double topNarrowing, double aspect,
                         double saturation, int distinctColors, boolean pixelArt, List<Guess> guesses, List<String> warnings) {
        public int width() {
            return image.width();
        }

        public int height() {
            return image.height();
        }

        public int boxWidth() {
            return x1 - x0 + 1;
        }

        public int boxHeight() {
            return y1 - y0 + 1;
        }

        public Guess best() {
            return guesses.get(0);
        }

        public boolean inMask(int x, int y) {
            return mask[y * image.width() + x];
        }
    }

    private ImageAnalysis() {
    }

    public static Result analyze(ImageData source) {
        // Le pixel art est analysé sans lissage (agrandissement éventuel au plus proche voisin).
        ImageData img = source.downscale(ANALYSIS_SIDE);
        int w = img.width(), h = img.height();
        List<String> warnings = new ArrayList<>();
        boolean[] mask;
        Background bg;
        if (source.hasTransparency() && opaqueRatio(img) < 0.98) {
            mask = alphaMask(img);
            bg = Background.ALPHA;
        } else {
            mask = borderMask(img);
            bg = Background.BORDER;
        }
        mask = largestComponents(mask, w, h);
        int filled = count(mask);
        double ratio = (double) filled / (w * h);
        if (filled < Math.max(4, w * h / 100) || ratio > 0.97) {
            // Fond non séparable (photo sans contraste avec le fond, ou image entièrement occupée).
            mask = new boolean[w * h];
            java.util.Arrays.fill(mask, true);
            bg = Background.NONE;
            warnings.add("Fond non séparé du sujet : l'image entière est utilisée");
        }
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (mask[y * w + x]) {
                    x0 = Math.min(x0, x);
                    x1 = Math.max(x1, x);
                    y0 = Math.min(y0, y);
                    y1 = Math.max(y1, y);
                }
            }
        }
        filled = count(mask);
        double fill = (double) filled / ((x1 - x0 + 1) * (y1 - y0 + 1));
        double[] stats = lumaStats(img, mask);
        double sharp = sharpness(img, mask);
        double sym = symmetry(mask, w, x0, y0, x1, y1);
        double narrowing = topNarrowing(mask, w, x0, y0, x1, y1);
        double aspect = (double) (x1 - x0 + 1) / (y1 - y0 + 1);
        double sat = saturation(img, mask);
        int distinct = distinctColors(img, mask, 512);
        boolean pixelArt = distinct <= 48 && (source.width() <= 256 || sharp > 0.08);
        if (stats[0] < 0.18) {
            warnings.add("Image très sombre : les couleurs des blocs seront peu fidèles");
        } else if (stats[0] > 0.9) {
            warnings.add("Image très claire (surexposée)");
        }
        if (stats[1] < 0.05) {
            warnings.add("Contraste très faible");
        }
        if (!pixelArt && sharp < 0.012) {
            warnings.add("Image floue : les détails fins seront perdus");
        }
        if (source.width() < 32 || source.height() < 32) {
            warnings.add("Image de très petite taille");
        }
        List<Guess> guesses = guess(fill, sym, narrowing, aspect, sat, pixelArt, bg, ratio, img, mask);
        return new Result(img, mask, x0, y0, x1, y1, bg, fill, stats[0], stats[1], sharp, sym, narrowing, aspect, sat, distinct, pixelArt,
                guesses, warnings);
    }

    private static double opaqueRatio(ImageData img) {
        int n = 0;
        for (int c : img.argb()) {
            if (ImageData.alpha(c) >= 128) {
                n++;
            }
        }
        return (double) n / img.argb().length;
    }

    private static boolean[] alphaMask(ImageData img) {
        boolean[] m = new boolean[img.argb().length];
        for (int i = 0; i < m.length; i++) {
            m[i] = ImageData.alpha(img.argb()[i]) >= 128;
        }
        return m;
    }

    /**
     * Fond = pixels reliés au bord dont la couleur reste proche d'une couleur de bord dominante
     * (remplissage par diffusion dans l'espace CIELAB, seuil adapté au bruit mesuré du bord).
     */
    private static boolean[] borderMask(ImageData img) {
        int w = img.width(), h = img.height();
        double[][] lab = new double[w * h][];
        for (int i = 0; i < lab.length; i++) {
            lab[i] = BlockPalette.lab(img.argb()[i]);
        }
        List<Integer> border = new ArrayList<>();
        for (int x = 0; x < w; x++) {
            border.add(x);
            border.add((h - 1) * w + x);
        }
        for (int y = 1; y < h - 1; y++) {
            border.add(y * w);
            border.add(y * w + w - 1);
        }
        List<double[]> refs = borderClusters(lab, border);
        double noise = borderNoise(lab, w, h);
        double threshold = Math.max(9, Math.min(26, 6 + noise * 2.2));
        boolean[] background = new boolean[w * h];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i : border) {
            if (!background[i] && nearRef(lab[i], refs, threshold)) {
                background[i] = true;
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int i = queue.poll();
            int x = i % w, y = i / w;
            for (int d = 0; d < 4; d++) {
                int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0), ny = y + (d == 2 ? 1 : d == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                    continue;
                }
                int j = ny * w + nx;
                // Double critère : proche du fond de référence ET continuité locale (gradients doux).
                if (!background[j] && nearRef(lab[j], refs, threshold) && dist(lab[i], lab[j]) < threshold) {
                    background[j] = true;
                    queue.add(j);
                }
            }
        }
        boolean[] m = new boolean[w * h];
        for (int i = 0; i < m.length; i++) {
            m[i] = !background[i];
        }
        return m;
    }

    /** Jusqu'à trois couleurs de bord représentatives (regroupement glouton). */
    private static List<double[]> borderClusters(double[][] lab, List<Integer> border) {
        List<double[]> centers = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i : border) {
            double[] c = lab[i];
            int best = -1;
            double bd = 18;
            for (int k = 0; k < centers.size(); k++) {
                double d = dist(c, centers.get(k));
                if (d < bd) {
                    bd = d;
                    best = k;
                }
            }
            if (best < 0) {
                centers.add(c.clone());
                counts.add(1);
            } else {
                int n = counts.get(best) + 1;
                double[] ce = centers.get(best);
                for (int a = 0; a < 3; a++) {
                    ce[a] += (c[a] - ce[a]) / n;
                }
                counts.set(best, n);
            }
        }
        List<double[]> out = new ArrayList<>();
        int total = border.size();
        for (int k = 0; k < centers.size(); k++) {
            // Une couleur de bord marginale (le sujet touche le bord) n'est pas un fond.
            if (counts.get(k) >= total * 0.12 && out.size() < 3) {
                out.add(centers.get(k));
            }
        }
        if (out.isEmpty() && !centers.isEmpty()) {
            int best = 0;
            for (int k = 1; k < centers.size(); k++) {
                if (counts.get(k) > counts.get(best)) {
                    best = k;
                }
            }
            out.add(centers.get(best));
        }
        return out;
    }

    private static double borderNoise(double[][] lab, int w, int h) {
        double sum = 0;
        int n = 0;
        for (int x = 1; x < w; x++) {
            sum += dist(lab[x], lab[x - 1]);
            sum += dist(lab[(h - 1) * w + x], lab[(h - 1) * w + x - 1]);
            n += 2;
        }
        return n == 0 ? 0 : sum / n;
    }

    private static boolean nearRef(double[] c, List<double[]> refs, double t) {
        for (double[] r : refs) {
            if (dist(c, r) < t * 1.6) {
                return true;
            }
        }
        return false;
    }

    static double dist(double[] a, double[] b) {
        double dl = a[0] - b[0], da = a[1] - b[1], db = a[2] - b[2];
        return Math.sqrt(dl * dl + da * da + db * db);
    }

    /** Conserve la plus grande composante et celles d'au moins 8 % de sa taille (sujets en plusieurs morceaux). */
    static boolean[] largestComponents(boolean[] mask, int w, int h) {
        int[] label = new int[mask.length];
        List<Integer> sizes = new ArrayList<>();
        sizes.add(0);
        int[] stack = new int[mask.length];
        for (int i = 0; i < mask.length; i++) {
            if (!mask[i] || label[i] != 0) {
                continue;
            }
            int id = sizes.size(), sp = 0, size = 0;
            stack[sp++] = i;
            label[i] = id;
            while (sp > 0) {
                int p = stack[--sp];
                size++;
                int x = p % w, y = p / w;
                if (x > 0 && mask[p - 1] && label[p - 1] == 0) {
                    label[p - 1] = id;
                    stack[sp++] = p - 1;
                }
                if (x < w - 1 && mask[p + 1] && label[p + 1] == 0) {
                    label[p + 1] = id;
                    stack[sp++] = p + 1;
                }
                if (y > 0 && mask[p - w] && label[p - w] == 0) {
                    label[p - w] = id;
                    stack[sp++] = p - w;
                }
                if (y < h - 1 && mask[p + w] && label[p + w] == 0) {
                    label[p + w] = id;
                    stack[sp++] = p + w;
                }
            }
            sizes.add(size);
        }
        int max = 0;
        for (int s : sizes) {
            max = Math.max(max, s);
        }
        boolean[] keep = new boolean[sizes.size()];
        for (int k = 1; k < sizes.size(); k++) {
            keep[k] = sizes.get(k) >= Math.max(1, max * 0.08);
        }
        boolean[] out = new boolean[mask.length];
        for (int i = 0; i < mask.length; i++) {
            out[i] = label[i] != 0 && keep[label[i]];
        }
        // Trous intérieurs (fenêtres de la couleur du ciel, etc.) : tout fond non relié au bord fait partie du sujet.
        boolean[] outside = new boolean[mask.length];
        int sp = 0;
        for (int x = 0; x < w; x++) {
            sp = seed(out, outside, stack, sp, x);
            sp = seed(out, outside, stack, sp, (h - 1) * w + x);
        }
        for (int y = 0; y < h; y++) {
            sp = seed(out, outside, stack, sp, y * w);
            sp = seed(out, outside, stack, sp, y * w + w - 1);
        }
        while (sp > 0) {
            int p = stack[--sp];
            int x = p % w, y = p / w;
            if (x > 0) {
                sp = seed(out, outside, stack, sp, p - 1);
            }
            if (x < w - 1) {
                sp = seed(out, outside, stack, sp, p + 1);
            }
            if (y > 0) {
                sp = seed(out, outside, stack, sp, p - w);
            }
            if (y < h - 1) {
                sp = seed(out, outside, stack, sp, p + w);
            }
        }
        for (int i = 0; i < out.length; i++) {
            out[i] = !outside[i];
        }
        return out;
    }

    private static int seed(boolean[] fg, boolean[] outside, int[] stack, int sp, int p) {
        if (!fg[p] && !outside[p]) {
            outside[p] = true;
            stack[sp++] = p;
        }
        return sp;
    }

    private static int count(boolean[] m) {
        int n = 0;
        for (boolean b : m) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    private static double[] lumaStats(ImageData img, boolean[] mask) {
        double s = 0, s2 = 0;
        int n = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                double l = ImageData.luma(img.argb()[i]);
                s += l;
                s2 += l * l;
                n++;
            }
        }
        if (n == 0) {
            return new double[]{0, 0};
        }
        double mean = s / n;
        return new double[]{mean, Math.sqrt(Math.max(0, s2 / n - mean * mean))};
    }

    /** Netteté : moyenne de |laplacien| de la luminance dans le sujet (0 = flou total). */
    private static double sharpness(ImageData img, boolean[] mask) {
        int w = img.width(), h = img.height();
        double s = 0;
        int n = 0;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int i = y * w + x;
                if (!mask[i]) {
                    continue;
                }
                double c = ImageData.luma(img.argb()[i]);
                double lap = 4 * c - ImageData.luma(img.argb()[i - 1]) - ImageData.luma(img.argb()[i + 1]) - ImageData.luma(img.argb()[i - w])
                        - ImageData.luma(img.argb()[i + w]);
                s += Math.abs(lap);
                n++;
            }
        }
        return n == 0 ? 0 : s / n;
    }

    /** Symétrie gauche/droite de la silhouette (intersection / union avec son miroir). */
    private static double symmetry(boolean[] mask, int w, int x0, int y0, int x1, int y1) {
        int inter = 0, union = 0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                boolean a = mask[y * w + x], b = mask[y * w + (x0 + x1 - x)];
                if (a && b) {
                    inter++;
                }
                if (a || b) {
                    union++;
                }
            }
        }
        return union == 0 ? 0 : (double) inter / union;
    }

    /** Largeur du quart supérieur rapportée à la largeur de la moitié inférieure (toit = valeur faible). */
    private static double topNarrowing(boolean[] mask, int w, int x0, int y0, int x1, int y1) {
        int hh = y1 - y0 + 1;
        double top = avgWidth(mask, w, x0, x1, y0, y0 + Math.max(0, hh / 4 - 1));
        double bottom = avgWidth(mask, w, x0, x1, y0 + hh / 2, y1);
        return bottom <= 0 ? 1 : top / bottom;
    }

    private static double avgWidth(boolean[] mask, int w, int x0, int x1, int ya, int yb) {
        double s = 0;
        int n = 0;
        for (int y = ya; y <= yb; y++) {
            int c = 0;
            for (int x = x0; x <= x1; x++) {
                if (mask[y * w + x]) {
                    c++;
                }
            }
            s += c;
            n++;
        }
        return n == 0 ? 0 : s / n;
    }

    private static double saturation(ImageData img, boolean[] mask) {
        double s = 0;
        int n = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                int c = img.argb()[i];
                int mx = Math.max(ImageData.red(c), Math.max(ImageData.green(c), ImageData.blue(c)));
                int mn = Math.min(ImageData.red(c), Math.min(ImageData.green(c), ImageData.blue(c)));
                s += mx == 0 ? 0 : (double) (mx - mn) / mx;
                n++;
            }
        }
        return n == 0 ? 0 : s / n;
    }

    private static int distinctColors(ImageData img, boolean[] mask, int cap) {
        java.util.HashSet<Integer> set = new java.util.HashSet<>();
        for (int i = 0; i < mask.length && set.size() <= cap; i++) {
            if (mask[i]) {
                set.add(img.argb()[i] & 0xFFFFFF);
            }
        }
        return set.size();
    }

    /** Part de pixels verts (feuillage) et brun-gris dans le sujet. */
    private static double greenShare(ImageData img, boolean[] mask) {
        int g = 0, n = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                int c = img.argb()[i];
                int r = ImageData.red(c), gg = ImageData.green(c), b = ImageData.blue(c);
                if (gg > r + 12 && gg > b + 12) {
                    g++;
                }
                n++;
            }
        }
        return n == 0 ? 0 : (double) g / n;
    }

    private static List<Guess> guess(double fill, double sym, double narrowing, double aspect, double sat, boolean pixelArt, Background bg,
                                     double coverage, ImageData img, boolean[] mask) {
        double green = greenShare(img, mask);
        List<Guess> list = new ArrayList<>();
        // Scores bornés dans [0, 1] ; chaque règle est documentée dans le motif affiché.
        list.add(new Guess(Subject.BUILDING, clamp(0.25 + (fill > 0.6 ? 0.25 : 0) + (narrowing < 0.75 ? 0.2 : 0) + (sym > 0.75 ? 0.15 : 0)
                + (aspect > 0.7 && aspect < 2.5 ? 0.1 : 0) - green * 0.4),
                String.format(Locale.ROOT, "silhouette remplie à %.0f %%, haut %.0f %% de la base, symétrie %.0f %%", fill * 100, narrowing * 100,
                        sym * 100)));
        list.add(new Guess(Subject.PIXEL_ART, clamp(pixelArt ? 0.75 : 0.05), pixelArt ? "peu de couleurs distinctes, contours nets"
                : "trop de nuances pour du pixel art"));
        list.add(new Guess(Subject.CHARACTER, clamp(0.15 + (aspect < 0.75 ? 0.3 : 0) + (sym > 0.7 ? 0.15 : 0) + (fill < 0.7 ? 0.1 : 0)),
                String.format(Locale.ROOT, "silhouette verticale (rapport %.2f)", aspect)));
        list.add(new Guess(Subject.ANIMAL, clamp(0.15 + (aspect > 1.1 && aspect < 2.6 ? 0.25 : 0) + (fill < 0.75 ? 0.15 : 0) + (sym < 0.7 ? 0.1 : 0)),
                String.format(Locale.ROOT, "silhouette horizontale (rapport %.2f)", aspect)));
        list.add(new Guess(Subject.VEHICLE, clamp(0.1 + (aspect > 1.8 ? 0.3 : 0) + (fill > 0.5 ? 0.1 : 0) + (sat > 0.35 ? 0.05 : 0)),
                "silhouette très allongée"));
        list.add(new Guess(Subject.TREE, clamp(0.05 + green * 0.9 + (narrowing > 0.9 ? 0.05 : 0)),
                String.format(Locale.ROOT, "%.0f %% de pixels verts", green * 100)));
        list.add(new Guess(Subject.LANDSCAPE, clamp(bg == Background.NONE ? 0.55 + coverage * 0.1 : 0.05), bg == Background.NONE
                ? "aucun fond séparable : scène complète" : "sujet isolé du fond"));
        list.add(new Guess(Subject.OBJECT, clamp(0.25 + (bg == Background.ALPHA ? 0.1 : 0)), "valeur par défaut d'un sujet isolé"));
        list.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return List.copyOf(list);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(0.95, v));
    }
}
