package com.ultimateautobuilder.client;

import com.ultimateautobuilder.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Barres de progression des constructions du joueur, en haut à droite de l'écran. */
public final class ProgressHud {
    private static final int WIDTH = 150;

    private ProgressHud() {
    }

    public static void render(GuiGraphics g) {
        if (ClientConfig.loaded() && !ClientConfig.SHOW_PROGRESS.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        List<ClientState.Progress> list = ClientState.progress();
        if (list.isEmpty()) {
            return;
        }
        Font font = mc.font;
        int x = g.guiWidth() - WIDTH - 6;
        int y = 6;
        for (int i = 0; i < Math.min(4, list.size()); i++) {
            y = drawBar(g, font, list.get(i), x, y) + 4;
        }
        if (list.size() > 4) {
            g.drawString(font, "+" + (list.size() - 4) + " …", x, y, 0xFFAAAAAA);
        }
    }

    /** Dessine une barre et renvoie l'ordonnée sous celle-ci (réutilisé par le menu). */
    public static int drawBar(GuiGraphics g, Font font, ClientState.Progress p, int x, int y) {
        g.fill(x - 2, y - 2, x + WIDTH + 2, y + 22, 0xA0000000);
        String title = font.plainSubstrByWidth(p.title(), WIDTH - 40);
        g.drawString(font, title, x, y, 0xFFFFFFFF);
        int pct = p.total() <= 0 ? 0 : (int) Math.min(100, (long) p.progress() * 100 / p.total());
        String right = p.finished() ? stateLabel(p.state()) : pct + "%";
        g.drawString(font, right, x + WIDTH - font.width(right), y, color(p.state()));
        int barY = y + 11;
        g.fill(x, barY, x + WIDTH, barY + 4, 0xFF333333);
        g.fill(x, barY, x + WIDTH * pct / 100, barY + 4, 0xFF000000 | color(p.state()));
        if (p.message() != null && !p.message().isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(p.message(), WIDTH), x, barY + 6, 0xFFBBBBBB, false);
            return barY + 15;
        }
        return barY + 6;
    }

    /** Etats de Job : 0 génération, 1 chargement, 2 pose, 3 finitions, 4 terminé, 5 annulé, 6 échec. */
    static String stateLabel(int state) {
        return switch (state) {
            case 0 -> "Génération";
            case 1 -> "Chargement";
            case 2 -> "Construction";
            case 3 -> "Finitions";
            case 4 -> "Terminé";
            case 5 -> "Annulé";
            default -> "Échec";
        };
    }

    static int color(int state) {
        return switch (state) {
            case 4 -> 0x55FF55;
            case 5 -> 0xFFAA00;
            case 6 -> 0xFF5555;
            default -> 0x55CCFF;
        };
    }
}
