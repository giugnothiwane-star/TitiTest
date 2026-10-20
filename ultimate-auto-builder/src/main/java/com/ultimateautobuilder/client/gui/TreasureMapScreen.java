package com.ultimateautobuilder.client.gui;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.adventure.Sanctuary;
import com.ultimateautobuilder.adventure.Treasures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Carte au trésor : parchemin ancien, croix rouge à la position réelle du trésor, flèche du
 * joueur, rose des vents, distance et direction. L'échelle s'adapte pour que le joueur et la
 * croix restent visibles ; à moins de 4 blocs, la carte indique de creuser.
 */
public final class TreasureMapScreen extends Screen {
    private static final ResourceLocation PARCHMENT = UltimateAutoBuilderMod.id("textures/gui/treasure_map.png");
    private static final int SIZE = 220;
    private final ItemStack stack;

    public TreasureMapScreen(ItemStack stack) {
        super(Component.translatable("item." + UltimateAutoBuilderMod.MOD_ID + ".treasure_map"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        int size = Math.min(SIZE, Math.min(width - 20, height - 50));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, (height + size) / 2 + 6, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int size = Math.min(SIZE, Math.min(width - 20, height - 50));
        int x0 = (width - size) / 2, y0 = (height - size) / 2 - 10;
        g.blit(PARCHMENT, x0, y0, size, size, 0, 0, 256, 256, 256, 256);
        String ns = UltimateAutoBuilderMod.MOD_ID;
        Treasures.Target t = Treasures.target(stack);
        int ink = 0xFF4A2F18;
        g.drawCenteredString(font, Component.translatable("item." + ns + ".treasure_map").getString(), width / 2, y0 + 12, 0xFF5A3418);
        if (t == null || minecraft.player == null) {
            g.drawCenteredString(font, Component.translatable("item." + ns + ".treasure_map.unset").getString(), width / 2, y0 + size / 2, ink);
            return;
        }
        double dx = t.x() + 0.5 - minecraft.player.getX(), dz = t.z() + 0.5 - minecraft.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        // Échelle : la croix et le joueur tiennent dans 80 % du rayon du parchemin.
        double half = Math.max(48, dist * 1.25);
        double k = (size * 0.40) / half;
        int cx = x0 + size / 2, cy = y0 + size / 2 + 6;
        // Croix rouge épaisse à la position du trésor.
        int tx = cx + (int) Math.round(dx * k / 2), ty = cy + (int) Math.round(dz * k / 2);
        int px = cx - (int) Math.round(dx * k / 2), py = cy - (int) Math.round(dz * k / 2);
        for (int i = -6; i <= 6; i++) {
            for (int w = -1; w <= 1; w++) {
                g.fill(tx + i + w, ty + i, tx + i + w + 1, ty + i + 1, 0xFFB01818);
                g.fill(tx + i + w, ty - i, tx + i + w + 1, ty - i + 1, 0xFFB01818);
            }
        }
        // Pointillés du chemin et position du joueur.
        int steps = (int) Math.max(4, Math.hypot(tx - px, ty - py) / 6);
        for (int i = 1; i < steps; i += 2) {
            int qx = px + (tx - px) * i / steps, qy = py + (ty - py) * i / steps;
            g.fill(qx, qy, qx + 2, qy + 2, 0xC05A3418);
        }
        g.fill(px - 3, py - 3, px + 3, py + 3, 0xFF203060);
        g.fill(px - 1, py - 1, px + 1, py + 1, 0xFFE0E8FF);
        // Rose des vents.
        int rx = x0 + size - 26, ry = y0 + 34;
        g.drawCenteredString(font, "N", rx, ry - 12, 0xFF5A3418);
        g.fill(rx, ry - 3, rx + 1, ry + 8, 0xFF5A3418);
        g.fill(rx - 5, ry + 2, rx + 6, ry + 3, 0xFF5A3418);
        String dir = Component.translatable("direction." + ns + "." + Sanctuary.cardinal(dx, dz)).getString();
        String info = dist < 4 ? Component.translatable("gui." + ns + ".map_dig").getString()
                : Component.translatable("gui." + ns + ".map_distance", (int) Math.round(dist), dir).getString();
        g.drawCenteredString(font, info, width / 2, y0 + size - 30, dist < 4 ? 0xFFB01818 : 0xFF4A2F18);
        g.drawCenteredString(font, "X " + t.x() + "  Z " + t.z(), width / 2, y0 + size - 18, 0xFF6A4A28);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
