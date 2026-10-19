package com.ultimateautobuilder.client.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Textures dynamiques réutilisables par emplacement (aperçus d'Image-to-Build). Une texture de même
 * taille est mise à jour sur place ; sinon l'ancienne est libérée. Thread de rendu uniquement.
 */
final class ImageTextures {
    private record Slot(ResourceLocation id, DynamicTexture tex, int w, int h) {
    }

    private final Map<String, Slot> slots = new HashMap<>();
    private int counter;

    void put(String key, int w, int h, int[] argb) {
        if (w <= 0 || h <= 0 || argb == null || argb.length != w * h) {
            return;
        }
        Slot s = slots.get(key);
        if (s == null || s.w != w || s.h != h) {
            release(key);
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation id = UltimateAutoBuilderMod.id("image_to_build/" + key + "_" + (counter++));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            s = new Slot(id, tex, w, h);
            slots.put(key, s);
        }
        NativeImage img = s.tex.getPixels();
        if (img == null) {
            return;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = argb[y * w + x];
                // NativeImage attend de l'ABGR.
                img.setPixelRGBA(x, y, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
            }
        }
        s.tex.upload();
    }

    boolean has(String key) {
        return slots.containsKey(key);
    }

    /** Dessine en conservant les proportions, centré dans le cadre ; renvoie false si absent. */
    boolean draw(GuiGraphics g, String key, int x, int y, int boxW, int boxH) {
        Slot s = slots.get(key);
        if (s == null) {
            return false;
        }
        double k = Math.min((double) boxW / s.w, (double) boxH / s.h);
        int dw = Math.max(1, (int) (s.w * k)), dh = Math.max(1, (int) (s.h * k));
        RenderSystem.enableBlend();
        g.blit(s.id, x + (boxW - dw) / 2, y + (boxH - dh) / 2, dw, dh, 0, 0, s.w, s.h, s.w, s.h);
        RenderSystem.disableBlend();
        return true;
    }

    void release(String key) {
        Slot s = slots.remove(key);
        if (s != null) {
            Minecraft.getInstance().getTextureManager().release(s.id);
        }
    }

    void releaseAll() {
        for (String k : slots.keySet().toArray(new String[0])) {
            release(k);
        }
    }
}
