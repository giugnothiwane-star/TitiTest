package com.ultimateautobuilder.client;

import com.ultimateautobuilder.network.AdventureStatePayload;
import com.ultimateautobuilder.network.RewardFxPayload;
import com.ultimateautobuilder.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * État d'aventure reçu du serveur (affichage du hub) et animation courte de récompense :
 * le cristal pulse et s'illumine, quelques particules et un son, puis les objets sont révélés.
 * L'animation ne bloque rien (3,5 s, superposée à l'écran).
 */
public final class ClientAdventure {
    private static final long FX_MS = 3500;
    private static AdventureStatePayload state;
    private static long version;
    private static RewardFxPayload fx;
    private static long fxStart;

    private ClientAdventure() {
    }

    public static void set(AdventureStatePayload p) {
        state = p;
        version++;
    }

    public static AdventureStatePayload state() {
        return state;
    }

    public static long version() {
        return version;
    }

    public static void reset() {
        state = null;
        fx = null;
        version++;
    }

    public static void onRewardFx(RewardFxPayload p) {
        fx = p;
        fxStart = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.0F);
            mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.35F, 1.4F);
            // Une gerbe unique et légère (16 particules), jamais d'effet permanent.
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                mc.level.addParticle(ParticleTypes.END_ROD, mc.player.getX() + Math.cos(a) * 0.8, mc.player.getY() + 1.2,
                        mc.player.getZ() + Math.sin(a) * 0.8, Math.cos(a) * 0.05, 0.06, Math.sin(a) * 0.05);
            }
        }
    }

    /** Panneau de récompense en haut de l'écran (HUD et hub). */
    public static void renderFx(GuiGraphics g, int screenWidth) {
        if (fx == null) {
            return;
        }
        long t = System.currentTimeMillis() - fxStart;
        if (t > FX_MS) {
            fx = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float in = Mth.clamp(t / 250F, 0F, 1F), out = Mth.clamp((FX_MS - t) / 400F, 0F, 1F);
        float alpha = Math.min(in, out);
        int a = (int) (alpha * 220) << 24;
        int n = Math.min(fx.items().size(), 9);
        int w = Math.max(160, Math.max(font.width(fx.title()) + 40, n * 20 + 40));
        int x = (screenWidth - w) / 2, y = 8;
        g.fill(x, y, x + w, y + 46, a | 0x1A1030);
        g.fill(x, y, x + w, y + 1, a | 0xE07BB8);
        g.fill(x, y + 45, x + w, y + 46, a | 0xE07BB8);
        // Cristal : pulsation et halo pendant la première seconde, puis révélation des objets.
        float pulse = 1F + 0.25F * (float) Math.sin(Math.min(t, 1000) / 1000.0 * Math.PI * 3) * (1F - Math.min(1F, t / 1200F));
        g.pose().pushPose();
        g.pose().translate(x + 14, y + 14, 0);
        g.pose().scale(pulse, pulse, 1F);
        g.renderItem(new ItemStack(ModItems.ULTIMATE_CRYSTAL.get()), -8, -8);
        g.pose().popPose();
        if (t < 900) {
            int glow = (int) ((1F - t / 900F) * 120) << 24;
            g.fill(x + 2, y + 2, x + 26, y + 26, glow | 0xFFD6F0);
        }
        g.drawString(font, fx.title(), x + 30, y + 6, (Math.max(4, (int) (alpha * 255)) << 24) | 0xFFE0F5, false);
        int shown = (int) Mth.clamp((t - 600) / 150, 0, n);
        for (int i = 0; i < shown; i++) {
            ItemStack st = fx.items().get(i);
            int ix = x + 30 + i * 20;
            g.renderItem(st, ix, y + 22);
            g.renderItemDecorations(font, st, ix, y + 22);
        }
    }
}
