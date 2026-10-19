package com.ultimateautobuilder.core.image;

/**
 * Image en mémoire (ARGB 0xAARRGGBB), indépendante de toute bibliothèque graphique.
 */
public record ImageData(int width, int height, int[] argb) {
    public ImageData {
        if (width <= 0 || height <= 0 || argb == null || argb.length != width * height) {
            throw new IllegalArgumentException("Image invalide " + width + "×" + height);
        }
    }

    public int get(int x, int y) {
        return argb[y * width + x];
    }

    public static int alpha(int c) {
        return (c >>> 24) & 255;
    }

    public static int red(int c) {
        return (c >> 16) & 255;
    }

    public static int green(int c) {
        return (c >> 8) & 255;
    }

    public static int blue(int c) {
        return c & 255;
    }

    public static double luma(int c) {
        return (0.2126 * red(c) + 0.7152 * green(c) + 0.0722 * blue(c)) / 255.0;
    }

    /** Réduction par moyenne de zone (aucun crénelage), conservant les proportions. */
    public ImageData downscale(int maxSide) {
        int side = Math.max(width, height);
        if (side <= maxSide) {
            return this;
        }
        double k = (double) maxSide / side;
        int nw = Math.max(1, (int) Math.round(width * k)), nh = Math.max(1, (int) Math.round(height * k));
        return resize(nw, nh);
    }

    /** Rééchantillonnage par moyenne de zone (réduction) ou plus proche voisin (agrandissement). */
    public ImageData resize(int nw, int nh) {
        int[] out = new int[nw * nh];
        double sx = (double) width / nw, sy = (double) height / nh;
        for (int y = 0; y < nh; y++) {
            int y0 = (int) Math.floor(y * sy), y1 = Math.max(y0 + 1, (int) Math.floor((y + 1) * sy));
            for (int x = 0; x < nw; x++) {
                int x0 = (int) Math.floor(x * sx), x1 = Math.max(x0 + 1, (int) Math.floor((x + 1) * sx));
                long a = 0, r = 0, g = 0, b = 0, n = 0;
                for (int yy = y0; yy < Math.min(y1, height); yy++) {
                    for (int xx = x0; xx < Math.min(x1, width); xx++) {
                        int c = argb[yy * width + xx];
                        int ca = alpha(c);
                        a += ca;
                        // Couleurs pondérées par l'opacité : un bord transparent ne noircit pas la moyenne.
                        r += (long) red(c) * ca;
                        g += (long) green(c) * ca;
                        b += (long) blue(c) * ca;
                        n++;
                    }
                }
                if (n == 0 || a == 0) {
                    out[y * nw + x] = 0;
                } else {
                    out[y * nw + x] = (int) ((a / n) << 24 | (r / a) << 16 | (g / a) << 8 | (b / a));
                }
            }
        }
        return new ImageData(nw, nh, out);
    }

    public boolean hasTransparency() {
        for (int c : argb) {
            if (alpha(c) < 250) {
                return true;
            }
        }
        return false;
    }
}
