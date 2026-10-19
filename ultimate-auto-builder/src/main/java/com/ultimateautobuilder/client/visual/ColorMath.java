package com.ultimateautobuilder.client.visual;

/**
 * Calculs de couleur purs (sans Minecraft) utilisés par la recoloration des textures, testés
 * unitairement. Conversions TSV écrites à la main : java.awt.Color chargerait l'AWT, à éviter à côté de GLFW.
 */
public final class ColorMath {
    private ColorMath() {
    }

    /** Teinte, saturation, luminosité puis mélange vers la couleur choisie ; renvoie 0xRRGGBB. */
    static int transform(int r, int g, int b, VisualProfile.Tint t, float[] hsv, int tr, int tg, int tb) {
        rgbToHsv(r, g, b, hsv);
        float h = hsv[0] + t.hue / 360f;
        h -= (float) Math.floor(h);
        float s = Math.max(0f, Math.min(1f, hsv[1] * t.saturation));
        float v = Math.max(0f, Math.min(1f, hsv[2] * t.brightness));
        int rgb = hsvToRgb(h, s, v);
        if (t.strength > 0f) {
            int rr = (rgb >> 16) & 255, gg = (rgb >> 8) & 255, bb = rgb & 255;
            // Mélange multiplicatif : conserve le relief de la texture tout en prenant la couleur.
            int mr = rr * tr / 255, mg = gg * tg / 255, mb = bb * tb / 255;
            rr = Math.round(rr + (mr - rr) * t.strength);
            gg = Math.round(gg + (mg - gg) * t.strength);
            bb = Math.round(bb + (mb - bb) * t.strength);
            rgb = (rr << 16) | (gg << 8) | bb;
        }
        return rgb;
    }

    static void rgbToHsv(int r, int g, int b, float[] out) {
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float v = max / 255f;
        float s = max == 0 ? 0f : (max - min) / (float) max;
        float h;
        if (max == min) {
            h = 0f;
        } else {
            float d = max - min;
            if (max == r) {
                h = (g - b) / d + (g < b ? 6f : 0f);
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h /= 6f;
        }
        out[0] = h;
        out[1] = s;
        out[2] = v;
    }

    static int hsvToRgb(float h, float s, float v) {
        float hh = (h - (float) Math.floor(h)) * 6f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

}
