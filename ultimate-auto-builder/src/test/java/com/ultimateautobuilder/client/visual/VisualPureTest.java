package com.ultimateautobuilder.client.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualPureTest {
    @Test
    void sanitizeClampsExtremeAndInvalidValues() {
        VisualProfile p = new VisualProfile();
        p.exposure = Float.NaN;
        p.gamma = 99f;
        p.contrast = -5f;
        p.fogDistance = Float.POSITIVE_INFINITY;
        p.name = "   ";
        p.quality = null;
        p.textures.put("groupe_inconnu", new VisualProfile.Tint());
        p.tint("wood_oak").hue = 999f;
        p.sanitize();
        assertEquals(1f, p.exposure);
        assertEquals(2.2f, p.gamma);
        assertEquals(0.5f, p.contrast);
        assertEquals(1f, p.fogDistance);
        assertEquals("Profil", p.name);
        assertEquals(VisualProfile.Quality.BALANCED, p.quality);
        assertFalse(p.textures.containsKey("groupe_inconnu"), "Un groupe inconnu doit être ignoré");
        assertEquals(180f, p.textures.get("wood_oak").hue);
    }

    @Test
    void presetsAreValidAndDistinct() {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (String key : VisualProfile.PRESETS) {
            VisualProfile p = VisualProfile.preset(key);
            assertTrue(names.add(p.name), "Préréglage en double : " + p.name);
            assertTrue(Float.isFinite(p.exposure) && Float.isFinite(p.gamma));
        }
        assertFalse(VisualProfile.preset("normal").postEnabled, "« Normal » = rendu vanilla");
        assertTrue(VisualProfile.preset("caves").shadowLift > VisualProfile.preset("bright").shadowLift);
    }

    @Test
    void copyIsDeep() {
        VisualProfile a = VisualProfile.preset("fantasy");
        a.tint("wood_oak").enabled = true;
        VisualProfile b = a.copy("B");
        b.tint("wood_oak").enabled = false;
        assertTrue(a.tint("wood_oak").enabled);
        assertEquals(a.grassColor, b.grassColor);
    }

    @Test
    void textureGroupsAreWellFormed() {
        assertNotNull(TextureGroups.get("wood_oak"));
        assertNotNull(TextureGroups.get("wood_birch"));
        assertTrue(TextureGroups.get("wood_oak").textures().stream().noneMatch(t -> t.contains("birch")),
                "Le groupe chêne ne doit pas toucher au bouleau");
        for (TextureGroups.Group g : TextureGroups.all()) {
            for (String t : g.textures()) {
                assertTrue(t.startsWith("textures/") && t.endsWith(".png") && !t.contains(".."), t);
            }
        }
        for (TextureGroups.Section s : TextureGroups.Section.values()) {
            assertFalse(TextureGroups.inSection(s).isEmpty(), s.label);
        }
    }

    @Test
    void hsvRoundTripAndIdentityTint() {
        float[] hsv = new float[3];
        int[][] samples = {{0, 0, 0}, {255, 255, 255}, {200, 30, 30}, {12, 180, 90}, {40, 60, 230}, {128, 128, 0}};
        for (int[] c : samples) {
            ColorMath.rgbToHsv(c[0], c[1], c[2], hsv);
            int rgb = ColorMath.hsvToRgb(hsv[0], hsv[1], hsv[2]);
            assertTrue(Math.abs(((rgb >> 16) & 255) - c[0]) <= 1 && Math.abs(((rgb >> 8) & 255) - c[1]) <= 1 && Math.abs((rgb & 255) - c[2]) <= 1,
                    java.util.Arrays.toString(c) + " → " + Integer.toHexString(rgb));
        }
        VisualProfile.Tint id = new VisualProfile.Tint();
        id.enabled = true;
        assertEquals(0x4080C0, ColorMath.transform(0x40, 0x80, 0xC0, id, hsv, 255, 255, 255) & 0xFFFFFF);
        VisualProfile.Tint red = new VisualProfile.Tint();
        red.enabled = true;
        red.color = 0xFF0000;
        red.strength = 1f;
        int out = ColorMath.transform(200, 200, 200, red, hsv, 255, 0, 0);
        assertEquals(0, out & 0xFFFF, "Une teinte rouge pleine annule le vert et le bleu");
    }

    @Test
    void verdictThresholds() {
        assertTrue(PerformanceVerdict.evaluate(300, 200, 260, 180, VisualProfile.Quality.HIGH).verdict().startsWith("Acceptable"));
        PerformanceVerdict.Result severe = PerformanceVerdict.evaluate(200, 150, 70, 40, VisualProfile.Quality.ULTRA);
        assertTrue(severe.verdict().startsWith("Dégradation"), severe.verdict());
        assertNotNull(severe.advice());
        // Perdre 50 FPS sur 80 est bien plus grave que sur 300.
        assertFalse(PerformanceVerdict.evaluate(80, 60, 30, 20, VisualProfile.Quality.BALANCED).verdict().startsWith("Acceptable"));
        PerformanceVerdict.Result crit = PerformanceVerdict.evaluate(400, 300, 220, 150, VisualProfile.Quality.ULTRA);
        assertTrue(crit.verdict().contains("critique"), crit.verdict());
        assertEquals(VisualProfile.Quality.PERFORMANCE, crit.advice());
        assertNull(PerformanceVerdict.evaluate(300, 200, 290, 195, VisualProfile.Quality.PERFORMANCE).advice());
    }
}
