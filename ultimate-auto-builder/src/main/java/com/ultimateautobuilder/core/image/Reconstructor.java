package com.ultimateautobuilder.core.image;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reconstruction 3D d'un sujet à partir d'une ou plusieurs vues. Chaque bloc est marqué « vu »
 * (sa couleur provient directement d'un pixel de l'image) ou « estimé » (profondeur, arrière, côtés
 * ou intérieur déduits). Les parties impossibles à connaître sont listées dans les incertitudes.
 */
public final class Reconstructor {
    public enum Mode {
        AUTO("Automatique"), INFLATE("Volume arrondi"), RELIEF("Bas-relief (luminosité)"), FLAT("Plat extrudé"),
        BUILDING("Bâtiment (façade extrudée)"), MULTIVIEW("Plusieurs vues (enveloppe)");

        public final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    public enum BackMode {
        SYMMETRIC("Arrière symétrique (copie de l'avant)"), SIMPLIFIED("Arrière simplifié (couleurs lissées)"),
        FLAT_COLOR("Arrière uni (couleur dominante)");

        public final String label;

        BackMode(String label) {
            this.label = label;
        }
    }

    public enum Detail {
        LOW("Faible", 48), MEDIUM("Moyen", 96), HIGH("Élevé", 160), ULTRA("Ultra", 256);

        public final String label;
        public final int maxSide;

        Detail(String label, int maxSide) {
            this.label = label;
            this.maxSide = maxSide;
        }
    }

    /**
     * @param height hauteur voulue en blocs ; {@code width}/{@code depth} à 0 = déduits des proportions
     * @param subject type de sujet retenu (estimation ou choix du joueur)
     * @param sideFlipped la vue de côté montre l'avant du sujet à droite (au lieu de la gauche)
     */
    public record Options(Mode mode, BackMode back, BuildStyle style, Set<BlockPalette.Family> customFamilies, int width, int height, int depth,
                          Detail detail, boolean hollow, ImageAnalysis.Subject subject, boolean invertRelief, boolean sideFlipped) {
        public Options {
            mode = mode == null ? Mode.AUTO : mode;
            back = back == null ? BackMode.SYMMETRIC : back;
            style = style == null ? BuildStyle.FIDELE : style;
            customFamilies = customFamilies == null ? EnumSet.noneOf(BlockPalette.Family.class) : EnumSet.copyOf(withDefault(customFamilies));
            detail = detail == null ? Detail.MEDIUM : detail;
            subject = subject == null ? ImageAnalysis.Subject.UNKNOWN : subject;
            width = Math.max(0, Math.min(ImageModel.MAX_SIDE, width));
            height = Math.max(1, Math.min(ImageModel.MAX_SIDE, height));
            depth = Math.max(0, Math.min(ImageModel.MAX_SIDE, depth));
        }

        private static Set<BlockPalette.Family> withDefault(Set<BlockPalette.Family> s) {
            return s.isEmpty() ? EnumSet.noneOf(BlockPalette.Family.class) : s;
        }

        public static Options defaults(ImageAnalysis.Result a) {
            return new Options(Mode.AUTO, BackMode.SYMMETRIC, BuildStyle.FIDELE, null, 0, 48, 0, Detail.MEDIUM, true, a.best().subject(), false,
                    false);
        }
    }

    /** Vues complémentaires facultatives (déjà analysées). */
    public record Views(ImageAnalysis.Result front, ImageAnalysis.Result side, ImageAnalysis.Result back, ImageAnalysis.Result top) {
        public static Views single(ImageAnalysis.Result front) {
            return new Views(front, null, null, null);
        }
    }

    public record Result(ImageModel model, Mode usedMode, int visible, int estimated, List<String> uncertainties, List<String> notes) {
    }

    private Reconstructor() {
    }

    public static Mode resolveMode(Options o, Views v) {
        if (o.mode() != Mode.AUTO) {
            return o.mode() == Mode.MULTIVIEW && v.side() == null ? Mode.INFLATE : o.mode();
        }
        if (v.side() != null) {
            return Mode.MULTIVIEW;
        }
        return switch (o.subject()) {
            case BUILDING -> Mode.BUILDING;
            case PIXEL_ART -> Mode.FLAT;
            case LANDSCAPE -> Mode.RELIEF;
            default -> Mode.INFLATE;
        };
    }

    public static Result reconstruct(Views views, Options o) {
        ImageAnalysis.Result a = views.front();
        List<String> unc = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Mode mode = resolveMode(o, views);
        if (o.mode() == Mode.MULTIVIEW && views.side() == null) {
            notes.add("Aucune vue de côté fournie : volume arrondi utilisé à la place de l'enveloppe multi-vues");
        }
        // Dimensions : hauteur imposée, largeur selon les proportions du sujet, puis limites de détail et de volume.
        double aspect = (double) a.boxWidth() / a.boxHeight();
        double k = o.style().scale;
        int h = (int) Math.round(o.height() * k);
        int w = o.width() > 0 ? (int) Math.round(o.width() * k) : (int) Math.round(h * aspect);
        int d = o.depth() > 0 ? (int) Math.round(o.depth() * k) : defaultDepth(mode, o.subject(), w, h, views);
        int cap = o.detail().maxSide;
        int side = Math.max(w, Math.max(h, d));
        if (side > cap) {
            double s = (double) cap / side;
            w = (int) Math.round(w * s);
            h = (int) Math.round(h * s);
            d = (int) Math.round(d * s);
            notes.add("Taille réduite au niveau de détail « " + o.detail().label + " » (" + cap + " blocs maximum par côté)");
        }
        w = Math.max(1, w);
        h = Math.max(1, h);
        d = Math.max(1, d);
        while ((long) w * h * d > ImageModel.MAX_CELLS) {
            w = Math.max(1, w * 9 / 10);
            h = Math.max(1, h * 9 / 10);
            d = Math.max(1, d * 9 / 10);
            if (!notes.contains("Volume réduit pour rester dans la limite de mémoire")) {
                notes.add("Volume réduit pour rester dans la limite de mémoire");
            }
        }
        BlockPalette palette = o.style().palette(o.customFamilies());
        Plane front = Plane.of(a, w, h, a.pixelArt(), o.style());
        Plane back = backPlane(views, front, o, w, h);
        ImageModel model = new ImageModel(w, h, d);
        // Le filtre anti-mouchetis détruirait les contours d'un pixel art.
        boolean ds = o.style().despeckle && !a.pixelArt();
        switch (mode) {
            case MULTIVIEW -> multiView(model, front, back, views, o, palette, unc, ds);
            case RELIEF -> relief(model, front, palette, o.invertRelief(), ds);
            case FLAT, BUILDING -> extrude(model, front, back, palette, ds);
            default -> inflate(model, front, back, palette, ds);
        }
        if (mode == Mode.BUILDING) {
            int removed = o.hollow() ? model.hollow(true) : 0;
            if (removed > 0) {
                notes.add("Intérieur du bâtiment vidé (" + removed + " blocs), plancher conservé");
            }
        } else if (o.hollow() && d >= 3) {
            int removed = model.hollow(false);
            if (removed > 0) {
                notes.add("Intérieur creusé (" + removed + " blocs économisés)");
            }
        }
        // Incertitudes : ce que l'image ne montre pas.
        if (mode != Mode.MULTIVIEW) {
            unc.add("Profondeur estimée : aucune vue de côté");
        }
        if (views.back() == null && mode != Mode.RELIEF) {
            unc.add("Arrière non visible : " + o.back().label.toLowerCase(java.util.Locale.ROOT));
        }
        if (mode == Mode.BUILDING) {
            unc.add("Murs latéraux et arrière déduits de la façade ; aménagement intérieur inconnu");
        }
        switch (o.subject()) {
            case CHARACTER -> unc.add("Membres masqués ou derrière le corps non reconstruits");
            case ANIMAL -> unc.add("Pattes et queue cachées par le corps non reconstruites");
            case VEHICLE -> unc.add("Roues et côté opposé déduits par symétrie");
            case TREE -> unc.add("Branches internes et feuillage arrière estimés");
            default -> {
            }
        }
        if (a.background() == ImageAnalysis.Background.NONE) {
            unc.add("Sujet non isolé du fond : le décor fait partie du modèle");
        }
        unc.addAll(a.warnings());
        return new Result(model, mode, model.count(ImageModel.VISIBLE), model.count(ImageModel.ESTIMATED), List.copyOf(unc), List.copyOf(notes));
    }

    private static int defaultDepth(Mode mode, ImageAnalysis.Subject s, int w, int h, Views v) {
        int base = Math.min(w, h);
        return switch (mode) {
            case FLAT -> Math.max(1, Math.min(3, base / 16 + 1));
            case RELIEF -> Math.max(2, (int) Math.round(base * 0.15));
            case BUILDING -> Math.max(3, (int) Math.round(w * 0.8));
            case MULTIVIEW -> {
                ImageAnalysis.Result side = v.side();
                yield Math.max(1, (int) Math.round(h * (double) side.boxWidth() / side.boxHeight()));
            }
            default -> Math.max(2, (int) Math.round(switch (s) {
                case ANIMAL, VEHICLE -> w * 0.45;
                case CHARACTER -> w * 0.5;
                case TREE -> w * 0.9;
                case OBJECT -> base * 0.6;
                default -> base * 0.5;
            }));
        };
    }

    // ------------------------------------------------------------------ plans de couleur

    /** Image de face rééchantillonnée : masque et couleurs (0xRRGGBB), y = 0 en BAS. */
    static final class Plane {
        final int w, h;
        final boolean[] mask;
        final int[] rgb;

        Plane(int w, int h, boolean[] mask, int[] rgb) {
            this.w = w;
            this.h = h;
            this.mask = mask;
            this.rgb = rgb;
        }

        boolean in(int x, int y) {
            return x >= 0 && y >= 0 && x < w && y < h && mask[y * w + x];
        }

        int color(int x, int y) {
            return rgb[y * w + x];
        }

        static Plane of(ImageAnalysis.Result a, int w, int h, boolean nearest, BuildStyle style) {
            ImageData img = a.image();
            int bw = a.boxWidth(), bh = a.boxHeight();
            int[] crop = new int[bw * bh];
            for (int y = 0; y < bh; y++) {
                for (int x = 0; x < bw; x++) {
                    int sx = a.x0() + x, sy = a.y0() + y;
                    int c = img.get(sx, sy);
                    // Le fond devient transparent : la moyenne de zone ne mélange pas sujet et fond.
                    crop[y * bw + x] = a.inMask(sx, sy) ? (c | 0xFF000000) : 0;
                }
            }
            ImageData cropped = new ImageData(bw, bh, crop);
            ImageData r = nearest ? nearestResize(cropped, w, h) : cropped.resize(w, h);
            boolean[] mask = new boolean[w * h];
            int[] rgb = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = r.get(x, h - 1 - y);
                    mask[y * w + x] = ImageData.alpha(c) >= 110;
                    rgb[y * w + x] = c & 0xFFFFFF;
                }
            }
            Plane p = new Plane(w, h, mask, rgb);
            if (style.smoothColors && !nearest) {
                p = p.smoothed(1);
            }
            if (style.posterize > 0) {
                p = p.posterized(style.posterize);
            }
            return p;
        }

        /** Lissage préservant les contours : moyenne des voisins de couleur proche. */
        Plane smoothed(int radius) {
            int[] out = new int[rgb.length];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = rgb[y * w + x];
                    if (!mask[y * w + x]) {
                        out[y * w + x] = c;
                        continue;
                    }
                    long r = 0, g = 0, b = 0;
                    int n = 0;
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            if (in(x + dx, y + dy)) {
                                int o = rgb[(y + dy) * w + x + dx];
                                if (Math.abs(ImageData.red(o) - ImageData.red(c)) + Math.abs(ImageData.green(o) - ImageData.green(c))
                                        + Math.abs(ImageData.blue(o) - ImageData.blue(c)) < 90) {
                                    r += ImageData.red(o);
                                    g += ImageData.green(o);
                                    b += ImageData.blue(o);
                                    n++;
                                }
                            }
                        }
                    }
                    out[y * w + x] = n == 0 ? c : (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
                }
            }
            return new Plane(w, h, mask, out);
        }

        /** Flou de zone fort (arrière simplifié). */
        Plane blurred(int radius) {
            int[] out = new int[rgb.length];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    long r = 0, g = 0, b = 0;
                    int n = 0;
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            if (in(x + dx, y + dy)) {
                                int o = rgb[(y + dy) * w + x + dx];
                                r += ImageData.red(o);
                                g += ImageData.green(o);
                                b += ImageData.blue(o);
                                n++;
                            }
                        }
                    }
                    out[y * w + x] = n == 0 ? rgb[y * w + x] : (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
                }
            }
            return new Plane(w, h, mask, out);
        }

        int dominant() {
            long r = 0, g = 0, b = 0;
            int n = 0;
            for (int i = 0; i < rgb.length; i++) {
                if (mask[i]) {
                    r += ImageData.red(rgb[i]);
                    g += ImageData.green(rgb[i]);
                    b += ImageData.blue(rgb[i]);
                    n++;
                }
            }
            return n == 0 ? 0x808080 : (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
        }

        Plane uniform(int color) {
            int[] out = new int[rgb.length];
            java.util.Arrays.fill(out, color);
            return new Plane(w, h, mask, out);
        }

        Plane mirrored() {
            boolean[] m = new boolean[mask.length];
            int[] c = new int[rgb.length];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    m[y * w + x] = mask[y * w + (w - 1 - x)];
                    c[y * w + x] = rgb[y * w + (w - 1 - x)];
                }
            }
            return new Plane(w, h, m, c);
        }

        /** Réduction à k couleurs (k-moyennes déterministes, initialisation par quantiles de luminance). */
        Plane posterized(int k) {
            List<Integer> pts = new ArrayList<>();
            for (int i = 0; i < rgb.length; i++) {
                if (mask[i]) {
                    pts.add(rgb[i]);
                }
            }
            if (pts.size() <= k) {
                return this;
            }
            List<Integer> sorted = new ArrayList<>(pts);
            sorted.sort((p, q) -> Double.compare(ImageData.luma(p), ImageData.luma(q)));
            double[][] centers = new double[k][3];
            for (int c = 0; c < k; c++) {
                int col = sorted.get((int) ((c + 0.5) * sorted.size() / k));
                centers[c] = new double[]{ImageData.red(col), ImageData.green(col), ImageData.blue(col)};
            }
            int[] assign = new int[pts.size()];
            for (int iter = 0; iter < 8; iter++) {
                double[][] sum = new double[k][3];
                int[] cnt = new int[k];
                for (int i = 0; i < pts.size(); i++) {
                    int col = pts.get(i);
                    int best = nearestCenter(centers, col);
                    assign[i] = best;
                    sum[best][0] += ImageData.red(col);
                    sum[best][1] += ImageData.green(col);
                    sum[best][2] += ImageData.blue(col);
                    cnt[best]++;
                }
                for (int c = 0; c < k; c++) {
                    if (cnt[c] > 0) {
                        centers[c] = new double[]{sum[c][0] / cnt[c], sum[c][1] / cnt[c], sum[c][2] / cnt[c]};
                    }
                }
            }
            int[] out = rgb.clone();
            for (int i = 0; i < rgb.length; i++) {
                if (mask[i]) {
                    double[] c = centers[nearestCenter(centers, rgb[i])];
                    out[i] = ((int) c[0] << 16) | ((int) c[1] << 8) | (int) c[2];
                }
            }
            return new Plane(w, h, mask, out);
        }

        private static int nearestCenter(double[][] centers, int col) {
            int best = 0;
            double bd = Double.MAX_VALUE;
            for (int c = 0; c < centers.length; c++) {
                double dr = centers[c][0] - ImageData.red(col), dg = centers[c][1] - ImageData.green(col), db = centers[c][2] - ImageData.blue(col);
                double dd = dr * dr + dg * dg + db * db;
                if (dd < bd) {
                    bd = dd;
                    best = c;
                }
            }
            return best;
        }
    }

    static ImageData nearestResize(ImageData src, int w, int h) {
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            int sy = Math.min(src.height() - 1, (int) ((y + 0.5) * src.height() / h));
            for (int x = 0; x < w; x++) {
                int sx = Math.min(src.width() - 1, (int) ((x + 0.5) * src.width() / w));
                out[y * w + x] = src.get(sx, sy);
            }
        }
        return new ImageData(w, h, out);
    }

    private static Plane backPlane(Views views, Plane front, Options o, int w, int h) {
        if (views.back() != null) {
            // Vue arrière réelle : miroir horizontal pour l'aligner sur le repère de la face avant.
            return Plane.of(views.back(), w, h, views.back().pixelArt(), o.style()).mirrored();
        }
        return switch (o.back()) {
            case SIMPLIFIED -> front.blurred(Math.max(1, Math.min(w, h) / 10));
            case FLAT_COLOR -> front.uniform(front.dominant());
            default -> front;
        };
    }

    // ------------------------------------------------------------------ modes de reconstruction

    /** Blocs de face déterminés une fois par pixel, avec filtre de majorité anti-mouchetis. */
    private static String[] blocks(Plane p, BlockPalette palette, boolean despeckle) {
        String[] out = new String[p.w * p.h];
        for (int i = 0; i < out.length; i++) {
            if (p.mask[i]) {
                out[i] = palette.nearest(p.rgb[i]);
            }
        }
        if (!despeckle) {
            return out;
        }
        String[] res = out.clone();
        for (int y = 0; y < p.h; y++) {
            for (int x = 0; x < p.w; x++) {
                String c = out[y * p.w + x];
                if (c == null) {
                    continue;
                }
                String[] n = {at(out, p, x + 1, y), at(out, p, x - 1, y), at(out, p, x, y + 1), at(out, p, x, y - 1)};
                for (String cand : n) {
                    if (cand == null || cand.equals(c)) {
                        continue;
                    }
                    int same = 0, own = 0;
                    for (String s : n) {
                        if (cand.equals(s)) {
                            same++;
                        }
                        if (c.equals(s)) {
                            own++;
                        }
                    }
                    if (same >= 3 && own == 0) {
                        res[y * p.w + x] = cand;
                        break;
                    }
                }
            }
        }
        return res;
    }

    private static String at(String[] b, Plane p, int x, int y) {
        return x < 0 || y < 0 || x >= p.w || y >= p.h ? null : b[y * p.w + x];
    }

    private static int mixRgb(int a, int b, double t) {
        int r = (int) Math.round(ImageData.red(a) + (ImageData.red(b) - ImageData.red(a)) * t);
        int g = (int) Math.round(ImageData.green(a) + (ImageData.green(b) - ImageData.green(a)) * t);
        int bl = (int) Math.round(ImageData.blue(a) + (ImageData.blue(b) - ImageData.blue(a)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Remplit la colonne (x, y) de z0 à z1 : face avant vue, face arrière et côtés estimés. */
    private static void column(ImageModel m, int x, int y, int z0, int z1, String frontBlock, String backBlock, int frontRgb, int backRgb,
                               BlockPalette palette, boolean backVisible) {
        for (int z = z0; z <= z1; z++) {
            if (z == z0) {
                m.set(x, y, z, frontBlock, ImageModel.VISIBLE);
            } else if (z == z1) {
                m.set(x, y, z, backBlock, backVisible ? ImageModel.VISIBLE : ImageModel.ESTIMATED);
            } else {
                double t = (double) (z - z0) / Math.max(1, z1 - z0);
                m.set(x, y, z, palette.nearest(mixRgb(frontRgb, backRgb, t)), ImageModel.ESTIMATED);
            }
        }
    }

    private static void extrude(ImageModel m, Plane front, Plane back, BlockPalette palette, boolean ds) {
        String[] fb = blocks(front, palette, ds), bb = blocks(back, palette, ds);
        int d = m.sizeZ();
        for (int y = 0; y < front.h; y++) {
            for (int x = 0; x < front.w; x++) {
                if (!front.in(x, y)) {
                    continue;
                }
                int i = y * front.w + x;
                String backBlock = bb[i] != null ? bb[i] : fb[i];
                column(m, x, y, 0, d - 1, fb[i], backBlock, front.color(x, y), back.in(x, y) ? back.color(x, y) : front.color(x, y), palette, false);
            }
        }
    }

    /** Volume arrondi : épaisseur ∝ √(distance au contour), centrée sur le plan médian. */
    private static void inflate(ImageModel m, Plane front, Plane back, BlockPalette palette, boolean ds) {
        int w = front.w, h = front.h, d = m.sizeZ();
        int[] dist = distance(front);
        int max = 1;
        for (int v : dist) {
            max = Math.max(max, v);
        }
        String[] fb = blocks(front, palette, ds), bb = blocks(back, palette, ds);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!front.in(x, y)) {
                    continue;
                }
                int i = y * w + x;
                int t = Math.max(1, (int) Math.round(d * Math.sqrt((double) dist[i] / max)));
                int z0 = (d - t) / 2, z1 = z0 + t - 1;
                String backBlock = bb[i] != null ? bb[i] : fb[i];
                column(m, x, y, z0, z1, fb[i], backBlock, front.color(x, y), back.in(x, y) ? back.color(x, y) : front.color(x, y), palette, false);
            }
        }
    }

    /** Bas-relief : la luminosité fixe la saillie ; plaque de fond à l'arrière. */
    private static void relief(ImageModel m, Plane front, BlockPalette palette, boolean invert, boolean ds) {
        int d = m.sizeZ();
        String[] fb = blocks(front, palette, ds);
        for (int y = 0; y < front.h; y++) {
            for (int x = 0; x < front.w; x++) {
                if (!front.in(x, y)) {
                    continue;
                }
                double l = ImageData.luma(front.color(x, y));
                if (invert) {
                    l = 1 - l;
                }
                int t = 1 + (int) Math.round((d - 1) * l);
                int z0 = d - t;
                String b = fb[y * front.w + x];
                m.set(x, y, z0, b, ImageModel.VISIBLE);
                for (int z = z0 + 1; z < d; z++) {
                    m.set(x, y, z, b, ImageModel.ESTIMATED);
                }
            }
        }
    }

    /** Enveloppe visuelle : un bloc existe s'il appartient à la silhouette de chaque vue fournie. */
    private static void multiView(ImageModel m, Plane front, Plane back, Views views, Options o, BlockPalette palette, List<String> unc,
                                  boolean ds) {
        int w = m.sizeX(), h = m.sizeY(), d = m.sizeZ();
        Plane side = Plane.of(views.side(), d, h, views.side().pixelArt(), o.style());
        if (o.sideFlipped()) {
            side = side.mirrored();
        }
        Plane top = views.top() == null ? null : Plane.of(views.top(), w, d, views.top().pixelArt(), o.style());
        String[] fb = blocks(front, palette, ds), bb = blocks(back, palette, ds), sb = blocks(side, palette, ds);
        boolean backSeen = views.back() != null;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < d; z++) {
                if (!side.in(z, y)) {
                    continue;
                }
                for (int x = 0; x < w; x++) {
                    // Vue de dessus : ligne 0 de l'image = arrière du sujet (vue depuis le haut, avant en bas).
                    if (!front.in(x, y) || (top != null && !top.in(x, z))) {
                        continue;
                    }
                    m.set(x, y, z, "minecraft:stone", ImageModel.ESTIMATED);
                }
            }
        }
        // Coloration des surfaces : chaque face visible prend la couleur de la vue qui la montre.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int zf = -1, zb = -1;
                for (int z = 0; z < d; z++) {
                    if (m.filled(x, y, z)) {
                        if (zf < 0) {
                            zf = z;
                        }
                        zb = z;
                    }
                }
                if (zf < 0) {
                    continue;
                }
                int i = y * w + x;
                for (int z = zf; z <= zb; z++) {
                    if (!m.filled(x, y, z)) {
                        continue;
                    }
                    boolean sideSurface = !m.filled(x - 1, y, z) || !m.filled(x + 1, y, z);
                    int si = y * d + z;
                    if (z == zf && fb[i] != null) {
                        m.set(x, y, z, fb[i], ImageModel.VISIBLE);
                    } else if (z == zb) {
                        m.set(x, y, z, bb[i] != null ? bb[i] : fb[i], backSeen ? ImageModel.VISIBLE : ImageModel.ESTIMATED);
                    } else if (sideSurface && sb[si] != null) {
                        // Seul le côté réellement photographié (droit) est « vu » ; l'autre est supposé symétrique.
                        m.set(x, y, z, sb[si], !m.filled(x + 1, y, z) ? ImageModel.VISIBLE : ImageModel.ESTIMATED);
                    } else {
                        double t = (double) (z - zf) / Math.max(1, zb - zf);
                        m.set(x, y, z, palette.nearest(mixRgb(front.color(x, y), back.in(x, y) ? back.color(x, y) : front.color(x, y), t)),
                                ImageModel.ESTIMATED);
                    }
                }
            }
        }
        unc.add("Formes concaves invisibles sur les silhouettes (creux, arches) non reconstruites");
        if (views.top() == null) {
            unc.add("Aucune vue de dessus : coins de l'enveloppe possiblement trop pleins");
        }
    }

    /** Distance de chanfrein (3-4) au contour du masque, en unités entières. */
    static int[] distance(Plane p) {
        int w = p.w, h = p.h;
        int inf = Integer.MAX_VALUE / 4;
        int[] dist = new int[w * h];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = p.mask[i] ? inf : 0;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (dist[i] == 0) {
                    continue;
                }
                int v = dist[i];
                v = Math.min(v, x > 0 ? dist[i - 1] + 3 : 3);
                v = Math.min(v, y > 0 ? dist[i - w] + 3 : 3);
                v = Math.min(v, x > 0 && y > 0 ? dist[i - w - 1] + 4 : 4);
                v = Math.min(v, x < w - 1 && y > 0 ? dist[i - w + 1] + 4 : 4);
                dist[i] = v;
            }
        }
        for (int y = h - 1; y >= 0; y--) {
            for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x;
                if (dist[i] == 0) {
                    continue;
                }
                int v = dist[i];
                v = Math.min(v, x < w - 1 ? dist[i + 1] + 3 : 3);
                v = Math.min(v, y < h - 1 ? dist[i + w] + 3 : 3);
                v = Math.min(v, x < w - 1 && y < h - 1 ? dist[i + w + 1] + 4 : 4);
                v = Math.min(v, x > 0 && y < h - 1 ? dist[i + w - 1] + 4 : 4);
                dist[i] = v;
            }
        }
        return dist;
    }
}
