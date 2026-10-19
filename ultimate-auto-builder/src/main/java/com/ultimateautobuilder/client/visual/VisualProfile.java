package com.ultimateautobuilder.client.visual;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Profil graphique sérialisé en JSON (format versionné). Toutes les valeurs sont bornées par
 * {@link #sanitize()} : un fichier importé ou modifié à la main ne peut produire ni NaN ni valeur
 * extrême (écran blanc, noirs bouchés). Les couleurs sont stockées en 0xRRGGBB.
 */
public final class VisualProfile {
    public static final int FORMAT_VERSION = 1;

    /** Niveaux de qualité : ils changent réellement les passes de rendu utilisées. */
    public enum Quality {
        /** Étalonnage seul (une passe). */
        PERFORMANCE("Performance"),
        /** Étalonnage + vignette + netteté (une passe, plus de calculs). */
        BALANCED("Équilibré"),
        /** + halo lumineux (bloom) : extraction, flou horizontal et vertical. */
        HIGH("Élevé"),
        /** Bloom à grand rayon en deux itérations. */
        ULTRA("Ultra");

        public final String label;

        Quality(String label) {
            this.label = label;
        }
    }

    /** Recoloration d'un groupe de textures (bois de chêne, zombies…). */
    public static final class Tint {
        public boolean enabled;
        /** Décalage de teinte en degrés (-180..180). */
        public float hue;
        /** Multiplicateur de saturation (0..2). */
        public float saturation = 1f;
        /** Multiplicateur de luminosité (0.3..1.8). */
        public float brightness = 1f;
        /** Couleur de teinte appliquée par mélange. */
        public int color = 0xFFFFFF;
        /** Force du mélange vers {@link #color} (0..1). */
        public float strength;

        Tint copy() {
            Tint t = new Tint();
            t.enabled = enabled;
            t.hue = hue;
            t.saturation = saturation;
            t.brightness = brightness;
            t.color = color;
            t.strength = strength;
            return t;
        }

        void sanitize() {
            hue = clamp(hue, -180f, 180f);
            saturation = clamp(saturation, 0f, 2f);
            brightness = clamp(brightness, 0.3f, 1.8f);
            color &= 0xFFFFFF;
            strength = clamp(strength, 0f, 1f);
        }

        boolean isIdentity() {
            return !enabled || (Math.abs(hue) < 0.5f && Math.abs(saturation - 1f) < 0.01f && Math.abs(brightness - 1f) < 0.01f && strength < 0.01f);
        }
    }

    public int formatVersion = FORMAT_VERSION;
    public String name = "Personnalisé";

    // Rendu général
    public boolean postEnabled = true;
    public Quality quality = Quality.BALANCED;
    public float exposure = 1f;
    public float contrast = 1f;
    public float saturation = 1f;
    public float vibrance;
    public float gamma = 1f;
    /** Éclaircissement des zones sombres (grottes, nuit), 0..1. */
    public float shadowLift;
    /** Intensité des ombres (assombrissement des tons sombres), 0..1. */
    public float shadowDepth;
    public float temperature;
    public int tint = 0xFFFFFF;
    public float tintStrength;
    public float vignette = 0.25f;
    public float sharpen = 0.2f;
    public float bloomStrength = 0.35f;
    public float bloomThreshold = 0.72f;

    // Ciel et atmosphère
    public boolean skyEnabled;
    public int skyZenith = 0x4A7BE0;
    public int skyHorizon = 0xB9D6FF;
    public float skyStrength = 0.5f;
    public boolean fogColorEnabled;
    public int fogColor = 0xC0D8FF;
    public float fogColorStrength = 0.5f;
    /** Multiplicateur de la distance du brouillard (1 = vanilla, plus grand = moins de brouillard). */
    public float fogDistance = 1f;
    public boolean waterFogEnabled;
    public int waterFogColor = 0x2E6BD8;
    public float waterVisibility = 1f;

    // Végétation (couleurs de teinte des blocs)
    public boolean grassEnabled;
    public int grassColor = 0x79C05A;
    public float grassStrength = 0.6f;
    public boolean foliageEnabled;
    public int foliageColor = 0x59AE30;
    public float foliageStrength = 0.6f;

    // Comparaison avant/après (0 = désactivée, sinon position de la séparation 0..1)
    public float split;

    /** Recolorations par groupe de textures (clé = identifiant de groupe, voir {@link TextureGroups}). */
    public Map<String, Tint> textures = new LinkedHashMap<>();

    public VisualProfile copy(String newName) {
        VisualProfile p = new VisualProfile();
        p.name = newName;
        p.postEnabled = postEnabled;
        p.quality = quality;
        p.exposure = exposure;
        p.contrast = contrast;
        p.saturation = saturation;
        p.vibrance = vibrance;
        p.gamma = gamma;
        p.shadowLift = shadowLift;
        p.shadowDepth = shadowDepth;
        p.temperature = temperature;
        p.tint = tint;
        p.tintStrength = tintStrength;
        p.vignette = vignette;
        p.sharpen = sharpen;
        p.bloomStrength = bloomStrength;
        p.bloomThreshold = bloomThreshold;
        p.skyEnabled = skyEnabled;
        p.skyZenith = skyZenith;
        p.skyHorizon = skyHorizon;
        p.skyStrength = skyStrength;
        p.fogColorEnabled = fogColorEnabled;
        p.fogColor = fogColor;
        p.fogColorStrength = fogColorStrength;
        p.fogDistance = fogDistance;
        p.waterFogEnabled = waterFogEnabled;
        p.waterFogColor = waterFogColor;
        p.waterVisibility = waterVisibility;
        p.grassEnabled = grassEnabled;
        p.grassColor = grassColor;
        p.grassStrength = grassStrength;
        p.foliageEnabled = foliageEnabled;
        p.foliageColor = foliageColor;
        p.foliageStrength = foliageStrength;
        p.split = split;
        p.textures = new LinkedHashMap<>();
        textures.forEach((k, v) -> p.textures.put(k, v.copy()));
        return p;
    }

    public Tint tint(String group) {
        return textures.computeIfAbsent(group, k -> new Tint());
    }

    /** Borne toutes les valeurs ; remplace les valeurs absentes ou non finies par les valeurs par défaut. */
    public VisualProfile sanitize() {
        VisualProfile d = new VisualProfile();
        if (name == null || name.isBlank()) {
            name = "Profil";
        }
        name = name.strip();
        if (name.length() > 40) {
            name = name.substring(0, 40);
        }
        if (quality == null) {
            quality = Quality.BALANCED;
        }
        exposure = clampOr(exposure, 0.4f, 2.5f, d.exposure);
        contrast = clampOr(contrast, 0.5f, 1.8f, d.contrast);
        saturation = clampOr(saturation, 0f, 2f, d.saturation);
        vibrance = clampOr(vibrance, -1f, 1f, d.vibrance);
        gamma = clampOr(gamma, 0.5f, 2.2f, d.gamma);
        shadowLift = clampOr(shadowLift, 0f, 1f, 0f);
        shadowDepth = clampOr(shadowDepth, 0f, 1f, 0f);
        temperature = clampOr(temperature, -1f, 1f, 0f);
        tintStrength = clampOr(tintStrength, 0f, 1f, 0f);
        vignette = clampOr(vignette, 0f, 1f, d.vignette);
        sharpen = clampOr(sharpen, 0f, 1f, d.sharpen);
        bloomStrength = clampOr(bloomStrength, 0f, 1.5f, d.bloomStrength);
        bloomThreshold = clampOr(bloomThreshold, 0.3f, 0.98f, d.bloomThreshold);
        skyStrength = clampOr(skyStrength, 0f, 1f, d.skyStrength);
        fogColorStrength = clampOr(fogColorStrength, 0f, 1f, d.fogColorStrength);
        fogDistance = clampOr(fogDistance, 0.25f, 4f, 1f);
        waterVisibility = clampOr(waterVisibility, 0.25f, 4f, 1f);
        grassStrength = clampOr(grassStrength, 0f, 1f, d.grassStrength);
        foliageStrength = clampOr(foliageStrength, 0f, 1f, d.foliageStrength);
        split = clampOr(split, 0f, 0.95f, 0f);
        tint &= 0xFFFFFF;
        skyZenith &= 0xFFFFFF;
        skyHorizon &= 0xFFFFFF;
        fogColor &= 0xFFFFFF;
        waterFogColor &= 0xFFFFFF;
        grassColor &= 0xFFFFFF;
        foliageColor &= 0xFFFFFF;
        if (textures == null) {
            textures = new LinkedHashMap<>();
        }
        // Les groupes inconnus (ancienne version, fichier modifié) sont ignorés proprement.
        textures.keySet().removeIf(k -> k == null || TextureGroups.get(k) == null);
        textures.values().removeIf(v -> v == null);
        textures.values().forEach(Tint::sanitize);
        formatVersion = FORMAT_VERSION;
        return this;
    }

    static float clamp(float v, float lo, float hi) {
        return Float.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo;
    }

    private static float clampOr(float v, float lo, float hi, float fallback) {
        return Float.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : fallback;
    }

    // ------------------------------------------------------------------ préréglages

    public static VisualProfile preset(String key) {
        VisualProfile p = new VisualProfile();
        switch (key) {
            case "normal" -> {
                p.name = "Normal";
                p.postEnabled = false;
            }
            case "bright" -> {
                p.name = "Luminosité élevée";
                p.exposure = 1.25f;
                p.shadowLift = 0.25f;
                p.gamma = 1.15f;
            }
            case "max" -> {
                p.name = "Luminosité maximale";
                p.exposure = 1.45f;
                p.shadowLift = 0.55f;
                p.gamma = 1.35f;
                p.contrast = 0.95f;
            }
            case "caves" -> {
                p.name = "Grottes très lumineuses";
                p.shadowLift = 0.8f;
                p.gamma = 1.5f;
                p.exposure = 1.1f;
            }
            case "night" -> {
                p.name = "Nuit claire";
                p.shadowLift = 0.45f;
                p.temperature = -0.25f;
                p.gamma = 1.25f;
            }
            case "natural" -> {
                p.name = "Couleurs naturelles";
                p.saturation = 1.05f;
                p.vibrance = 0.15f;
                p.contrast = 1.05f;
                p.temperature = 0.05f;
            }
            case "saturated" -> {
                p.name = "Couleurs saturées";
                p.saturation = 1.35f;
                p.vibrance = 0.35f;
                p.contrast = 1.1f;
            }
            case "cinematic" -> {
                p.name = "Cinématique";
                p.quality = Quality.HIGH;
                p.contrast = 1.18f;
                p.saturation = 0.9f;
                p.temperature = 0.12f;
                p.vignette = 0.5f;
                p.bloomStrength = 0.5f;
                p.skyEnabled = true;
            }
            case "anime" -> {
                p.name = "Anime";
                p.quality = Quality.HIGH;
                p.saturation = 1.4f;
                p.vibrance = 0.4f;
                p.sharpen = 0.5f;
                p.bloomStrength = 0.6f;
                p.skyEnabled = true;
                p.skyZenith = 0x2F7BFF;
                p.skyHorizon = 0xFFD6F0;
            }
            case "fantasy" -> {
                p.name = "Fantastique";
                p.quality = Quality.ULTRA;
                p.tint = 0xE7C8FF;
                p.tintStrength = 0.15f;
                p.bloomStrength = 0.8f;
                p.skyEnabled = true;
                p.skyZenith = 0x6B3FD8;
                p.skyHorizon = 0xFFB6E1;
                p.grassEnabled = true;
                p.grassColor = 0x6FD8A8;
                p.foliageEnabled = true;
                p.foliageColor = 0xE78BC8;
            }
            case "performance" -> {
                p.name = "Performance";
                p.quality = Quality.PERFORMANCE;
                p.vignette = 0f;
                p.sharpen = 0f;
                p.bloomStrength = 0f;
            }
            case "ultra" -> {
                p.name = "Ultra";
                p.quality = Quality.ULTRA;
                p.contrast = 1.08f;
                p.vibrance = 0.2f;
                p.bloomStrength = 0.55f;
                p.sharpen = 0.35f;
                p.skyEnabled = true;
            }
            default -> p.name = "Personnalisé";
        }
        return p.sanitize();
    }

    public static final String[] PRESETS = {"normal", "bright", "max", "caves", "night", "natural", "saturated", "cinematic", "anime",
            "fantasy", "performance", "ultra"};
}
