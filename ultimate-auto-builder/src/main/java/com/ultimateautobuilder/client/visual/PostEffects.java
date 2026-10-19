package com.ultimateautobuilder.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Post-traitement du studio : une chaîne de passes GLSL chargée selon le niveau de qualité et
 * appliquée après le rendu du monde (avant la main et l'interface). Les paramètres du profil sont
 * transmis chaque image sous forme d'uniformes (aucune recompilation). En cas d'échec de compilation,
 * l'erreur est conservée pour le diagnostic et le rendu vanilla est utilisé : jamais d'écran noir.
 */
public final class PostEffects {
    private static PostChain chain;
    private static VisualProfile.Quality loadedQuality;
    private static int width = -1, height = -1;
    private static String error = "";
    private static boolean failed;
    /** Désactivation temporaire (mesure de référence du banc d'essai, commande de secours). */
    private static boolean suspended;
    private static long frames;

    private PostEffects() {
    }

    static ResourceLocation chainFor(VisualProfile.Quality q) {
        return switch (q) {
            case PERFORMANCE, BALANCED -> UltimateAutoBuilderMod.id("shaders/post/visual_grade.json");
            case HIGH -> UltimateAutoBuilderMod.id("shaders/post/visual_bloom.json");
            case ULTRA -> UltimateAutoBuilderMod.id("shaders/post/visual_bloom_ultra.json");
        };
    }

    public static void setSuspended(boolean value) {
        suspended = value;
    }

    public static boolean suspended() {
        return suspended;
    }

    public static boolean active() {
        return chain != null && !suspended && VisualStore.current().postEnabled;
    }

    public static String status() {
        if (failed) {
            return "Échec de compilation : rendu vanilla utilisé";
        }
        if (suspended) {
            return "Suspendu (mesure ou commande)";
        }
        if (!VisualStore.current().postEnabled) {
            return "Désactivé dans le profil";
        }
        return chain == null ? "En attente du premier rendu" : "Actif (" + loadedQuality.label + ", " + width + "×" + height + ")";
    }

    public static String error() {
        return error;
    }

    public static long frames() {
        return frames;
    }

    /** Force un rechargement (ressources rechargées, bouton « Recompiler »). */
    public static void invalidate() {
        close();
        failed = false;
        error = "";
    }

    public static void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        loadedQuality = null;
        width = height = -1;
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        VisualProfile p = VisualStore.current();
        if (!p.postEnabled || suspended) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!ensureLoaded(mc, p.quality)) {
            return;
        }
        upload(p);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(event.getPartialTick().getGameTimeDeltaTicks());
        mc.getMainRenderTarget().bindWrite(false);
        RenderSystem.enableDepthTest();
        frames++;
    }

    private static boolean ensureLoaded(Minecraft mc, VisualProfile.Quality q) {
        if (failed && q == loadedQuality) {
            return false;
        }
        if (chain != null && q != loadedQuality) {
            close();
        }
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), chainFor(q));
                failed = false;
                error = "";
            } catch (Exception e) {
                // IOException (fichier), JsonSyntaxException ou erreur de compilation GLSL : repli sur le rendu vanilla.
                failed = true;
                error = e.getClass().getSimpleName() + " : " + e.getMessage();
                UltimateAutoBuilderMod.LOGGER.error("Chargement des shaders du studio impossible ({})", chainFor(q), e);
                chain = null;
                loadedQuality = q;
                return false;
            }
            loadedQuality = q;
            width = height = -1;
        }
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w != width || h != height) {
            chain.resize(w, h);
            width = w;
            height = h;
        }
        return true;
    }

    private static void upload(VisualProfile p) {
        boolean balancedPlus = p.quality != VisualProfile.Quality.PERFORMANCE;
        set("Exposure", p.exposure);
        set("Contrast", p.contrast);
        set("Saturation", p.saturation);
        set("Vibrance", p.vibrance);
        set("Gamma", p.gamma);
        set("ShadowLift", p.shadowLift);
        set("ShadowDepth", p.shadowDepth);
        set("Temperature", p.temperature);
        set("TintR", ((p.tint >> 16) & 255) / 255f);
        set("TintG", ((p.tint >> 8) & 255) / 255f);
        set("TintB", (p.tint & 255) / 255f);
        set("TintStrength", p.tintStrength);
        // En qualité Performance, vignette et netteté sont réellement coupées (moins de lectures de texture).
        set("Vignette", balancedPlus ? p.vignette : 0f);
        set("Sharpen", balancedPlus ? p.sharpen : 0f);
        set("SkyEnabled", p.skyEnabled ? 1f : 0f);
        set("SkyStrength", p.skyStrength);
        set("ZenithR", ((p.skyZenith >> 16) & 255) / 255f);
        set("ZenithG", ((p.skyZenith >> 8) & 255) / 255f);
        set("ZenithB", (p.skyZenith & 255) / 255f);
        set("HorizonR", ((p.skyHorizon >> 16) & 255) / 255f);
        set("HorizonG", ((p.skyHorizon >> 8) & 255) / 255f);
        set("HorizonB", (p.skyHorizon & 255) / 255f);
        set("Split", p.split);
        set("BloomStrength", p.bloomStrength);
        set("Threshold", p.bloomThreshold);
    }

    private static void set(String name, float value) {
        chain.setUniform(name, value);
    }
}
