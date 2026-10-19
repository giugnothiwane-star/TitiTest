package com.ultimateautobuilder.core.image;

import java.util.HashMap;
import java.util.Map;

/**
 * Vues orthographiques et coupes d'un {@link ImageModel} (images ARGB, y = 0 en haut de l'image),
 * et comparaison mesurée entre l'image source et la face avant reconstruite.
 */
public final class ModelViews {
    public enum View { FRONT("Avant"), BACK("Arrière"), LEFT("Gauche"), RIGHT("Droite"), TOP("Dessus");

        public final String label;

        View(String label) {
            this.label = label;
        }
    }

    public record Image(int width, int height, int[] argb) {
    }

    /** Écart mesuré : recouvrement des silhouettes (intersection/union) et ΔE moyen des pixels communs. */
    public record Comparison(double silhouetteOverlap, double meanDeltaE, int comparedPixels) {
    }

    private ModelViews() {
    }

    /** Coloration avec option de surlignage des blocs estimés (teinte orangée). */
    public static Image render(ImageModel m, View v, boolean highlightEstimated) {
        int w, h;
        switch (v) {
            case LEFT, RIGHT -> {
                w = m.sizeZ();
                h = m.sizeY();
            }
            case TOP -> {
                w = m.sizeX();
                h = m.sizeZ();
            }
            default -> {
                w = m.sizeX();
                h = m.sizeY();
            }
        }
        int[] out = new int[w * h];
        Map<String, Integer> colors = new HashMap<>();
        int depthMax = switch (v) {
            case LEFT, RIGHT -> m.sizeX();
            case TOP -> m.sizeY();
            default -> m.sizeZ();
        };
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                for (int t = 0; t < depthMax; t++) {
                    int x, y, z;
                    switch (v) {
                        case FRONT -> {
                            x = px;
                            y = h - 1 - py;
                            z = t;
                        }
                        case BACK -> {
                            x = w - 1 - px;
                            y = h - 1 - py;
                            z = m.sizeZ() - 1 - t;
                        }
                        case LEFT -> {
                            x = t;
                            y = h - 1 - py;
                            z = w - 1 - px;
                        }
                        case RIGHT -> {
                            x = m.sizeX() - 1 - t;
                            y = h - 1 - py;
                            z = px;
                        }
                        default -> {
                            x = px;
                            y = m.sizeY() - 1 - t;
                            z = py;
                        }
                    }
                    String s = m.get(x, y, z);
                    if (s == null) {
                        continue;
                    }
                    int c = colors.computeIfAbsent(s, BlockPalette::colorOf);
                    if (c < 0) {
                        continue;
                    }
                    // Ombrage léger selon la profondeur : les reliefs restent lisibles en vue plate.
                    double shade = 1.0 - 0.35 * t / Math.max(1, depthMax);
                    c = shade(c, shade);
                    if (highlightEstimated && m.certainty(x, y, z) == ImageModel.ESTIMATED) {
                        c = mix(c, 0xFF9A3C, 0.45);
                    }
                    out[py * w + px] = 0xFF000000 | c;
                    break;
                }
            }
        }
        return new Image(w, h, out);
    }

    /** Coupe horizontale de la couche y, vue de dessus (avant en bas). */
    public static Image layer(ImageModel m, int y) {
        int w = m.sizeX(), h = m.sizeZ();
        int[] out = new int[w * h];
        int yy = Math.max(0, Math.min(m.sizeY() - 1, y));
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                String s = m.get(x, yy, z);
                int py = h - 1 - z;
                if (s != null) {
                    int c = BlockPalette.colorOf(s);
                    out[py * w + x] = c < 0 ? 0xFF303030 : 0xFF000000 | c;
                } else if (yy > 0 && m.filled(x, yy - 1, z)) {
                    // Couche inférieure en transparence : repère pour placer les blocs.
                    out[py * w + x] = 0x40FFFFFF;
                }
            }
        }
        return new Image(w, h, out);
    }

    /**
     * Compare la face avant au sujet de l'image (même résolution). Les deux grandeurs sont mesurées,
     * jamais un pourcentage de « ressemblance » inventé.
     */
    public static Comparison compare(ImageAnalysis.Result a, ImageModel m) {
        Image front = render(m, View.FRONT, false);
        ImageData src = cropSubject(a);
        ImageData scaled = src.resize(front.width(), front.height());
        int inter = 0, union = 0, n = 0;
        double sum = 0;
        for (int i = 0; i < front.argb().length; i++) {
            boolean inModel = ImageData.alpha(front.argb()[i]) > 0;
            boolean inImage = ImageData.alpha(scaled.argb()[i]) >= 110;
            if (inModel && inImage) {
                inter++;
                sum += BlockPalette.deltaE(front.argb()[i] & 0xFFFFFF, scaled.argb()[i] & 0xFFFFFF);
                n++;
            }
            if (inModel || inImage) {
                union++;
            }
        }
        return new Comparison(union == 0 ? 0 : (double) inter / union, n == 0 ? 0 : sum / n, n);
    }

    /** Sujet seul (fond transparent), recadré sur sa boîte englobante. */
    public static ImageData cropSubject(ImageAnalysis.Result a) {
        int bw = a.boxWidth(), bh = a.boxHeight();
        int[] px = new int[bw * bh];
        for (int y = 0; y < bh; y++) {
            for (int x = 0; x < bw; x++) {
                int sx = a.x0() + x, sy = a.y0() + y;
                px[y * bw + x] = a.inMask(sx, sy) ? (a.image().get(sx, sy) | 0xFF000000) : 0;
            }
        }
        return new ImageData(bw, bh, px);
    }

    static int shade(int rgb, double f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) Math.round(((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) Math.round((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }
}
