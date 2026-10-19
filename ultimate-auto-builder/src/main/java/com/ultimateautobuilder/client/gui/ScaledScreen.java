package com.ultimateautobuilder.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Écran qui garantit une surface de travail minimale (640 × 360 unités d'interface) quelle que soit
 * l'échelle d'interface choisie : avec l'échelle automatique sur un écran 1080p, Minecraft n'offre
 * que 480 × 270 unités, trop peu pour une grille de modèles et un aperçu lisibles. Le contenu est
 * alors dessiné légèrement plus petit (jamais sous un pixel réel par unité) et les coordonnées de
 * la souris sont converties en conséquence.
 */
public abstract class ScaledScreen extends Screen {
    public static final int MIN_W = 640, MIN_H = 360;
    /** Unités virtuelles par unité d'interface réelle (≥ 1). */
    protected float uiScale = 1f;

    protected ScaledScreen(Component title) {
        super(title);
    }

    @Override
    protected final void init() {
        Minecraft mc = Minecraft.getInstance();
        int realW = mc.getWindow().getGuiScaledWidth(), realH = mc.getWindow().getGuiScaledHeight();
        double guiScale = mc.getWindow().getGuiScale();
        float k = Math.max(1f, Math.max((float) MIN_W / realW, (float) MIN_H / realH));
        // Jamais moins d'un pixel physique par unité : le texte reste net.
        uiScale = (float) Math.min(k, Math.max(1.0, guiScale));
        width = (int) Math.ceil(realW * uiScale);
        height = (int) Math.ceil(realH * uiScale);
        initScaled();
        fitButtonLabels();
    }

    /**
     * Raccourcit les libellés trop longs : le texte défilant des boutons vanilla est découpé avec des
     * coordonnées non transformées, il serait invisible à une autre échelle. Le texte complet reste
     * disponible en infobulle.
     */
    private void fitButtonLabels() {
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractButton b) {
                String text = b.getMessage().getString();
                int avail = b.getWidth() - 8;
                if (font.width(text) > avail && avail > 12) {
                    String cut = font.plainSubstrByWidth(text, avail - font.width("…")) + "…";
                    if (b.getTooltip() == null) {
                        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(text)));
                    }
                    b.setMessage(Component.literal(cut));
                }
            }
        }
    }

    /** À appeler après l'ajout dynamique de boutons hors de {@link #initScaled()}. */
    protected void refitLabels() {
        fitButtonLabels();
    }

    /** Équivalent de {@link #init()} dans l'espace virtuel. */
    protected abstract void initScaled();

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.pose().pushPose();
        g.pose().scale(1f / uiScale, 1f / uiScale, 1f);
        renderScaled(g, (int) (mouseX * uiScale), (int) (mouseY * uiScale), partialTick);
        g.pose().popPose();
    }

    /** Rendu dans l'espace virtuel (les infobulles différées des widgets restent à l'échelle réelle). */
    protected void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public final boolean mouseClicked(double mx, double my, int button) {
        return mouseClickedScaled(mx * uiScale, my * uiScale, button);
    }

    @Override
    public final boolean mouseReleased(double mx, double my, int button) {
        return mouseReleasedScaled(mx * uiScale, my * uiScale, button);
    }

    @Override
    public final boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return mouseDraggedScaled(mx * uiScale, my * uiScale, button, dx * uiScale, dy * uiScale);
    }

    @Override
    public final boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return mouseScrolledScaled(mx * uiScale, my * uiScale, sx, sy);
    }

    @Override
    public final void mouseMoved(double mx, double my) {
        super.mouseMoved(mx * uiScale, my * uiScale);
    }

    @Override
    public final boolean isMouseOver(double mx, double my) {
        return super.isMouseOver(mx * uiScale, my * uiScale);
    }

    protected boolean mouseClickedScaled(double mx, double my, int button) {
        return super.mouseClicked(mx, my, button);
    }

    protected boolean mouseReleasedScaled(double mx, double my, int button) {
        return super.mouseReleased(mx, my, button);
    }

    protected boolean mouseDraggedScaled(double mx, double my, int button, double dx, double dy) {
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    protected boolean mouseScrolledScaled(double mx, double my, double sx, double sy) {
        return super.mouseScrolled(mx, my, sx, sy);
    }

    /**
     * Découpe en coordonnées virtuelles : {@link GuiGraphics#enableScissor} ignore la transformation
     * courante, on convertit donc vers les unités réelles.
     */
    public static void enableScissor(GuiGraphics g, int x0, int y0, int x1, int y1) {
        org.joml.Vector3f a = g.pose().last().pose().transformPosition(new org.joml.Vector3f(x0, y0, 0));
        org.joml.Vector3f b = g.pose().last().pose().transformPosition(new org.joml.Vector3f(x1, y1, 0));
        g.enableScissor((int) Math.floor(Math.min(a.x, b.x)), (int) Math.floor(Math.min(a.y, b.y)), (int) Math.ceil(Math.max(a.x, b.x)),
                (int) Math.ceil(Math.max(a.y, b.y)));
    }
}
