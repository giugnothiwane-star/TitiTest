package com.ultimateautobuilder.client.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Atmosphère et végétation pilotées par le profil :
 * <ul>
 *   <li>couleur du brouillard (qui colore aussi l'horizon) et distance de brouillard, dans l'air et sous l'eau ;</li>
 *   <li>teinte de l'herbe et du feuillage : nos gestionnaires remplacent ceux du jeu et reproduisent exactement
 *       la couleur vanilla quand l'option est désactivée.</li>
 * </ul>
 * Les teintes de blocs sont calculées lors de la construction des chunks : un changement demande
 * un recalcul des chunks ({@link #rebuildChunks()}), déclenché explicitement par le bouton « Appliquer ».
 */
public final class AtmosphereHooks {
    private AtmosphereHooks() {
    }

    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        VisualProfile p = VisualStore.current();
        FogType type = e.getCamera().getFluidInCamera();
        if (type == FogType.WATER && p.waterFogEnabled) {
            blend(e, p.waterFogColor, 0.85f);
        } else if (type == FogType.NONE && p.fogColorEnabled) {
            blend(e, p.fogColor, p.fogColorStrength);
        }
    }

    private static void blend(ViewportEvent.ComputeFogColor e, int rgb, float k) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        e.setRed(e.getRed() + (r - e.getRed()) * k);
        e.setGreen(e.getGreen() + (g - e.getGreen()) * k);
        e.setBlue(e.getBlue() + (b - e.getBlue()) * k);
    }

    public static void onRenderFog(ViewportEvent.RenderFog e) {
        VisualProfile p = VisualStore.current();
        float scale = switch (e.getType()) {
            case NONE -> p.fogDistance;
            case WATER -> p.waterFogEnabled ? p.waterVisibility : 1f;
            default -> 1f;
        };
        if (Math.abs(scale - 1f) > 0.01f) {
            e.scaleFarPlaneDistance(scale);
            e.scaleNearPlaneDistance(scale);
            // NeoForge n'applique les distances modifiées que si l'événement est annulé.
            e.setCanceled(true);
        }
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }

    private static int grass(int vanilla) {
        VisualProfile p = VisualStore.current();
        return p.grassEnabled ? mix(vanilla, p.grassColor, p.grassStrength) : vanilla;
    }

    private static int foliage(int vanilla) {
        VisualProfile p = VisualStore.current();
        return p.foliageEnabled ? mix(vanilla, p.foliageColor, p.foliageStrength) : vanilla;
    }

    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor grassColor = (state, level, pos, tint) -> grass(level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor());
        event.register(grassColor, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
                Blocks.POTTED_FERN, Blocks.SUGAR_CANE);
        BlockColor foliageColor = (state, level, pos, tint) -> foliage(level != null && pos != null
                ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor());
        event.register(foliageColor, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.VINE);
        event.register((state, level, pos, tint) -> foliage(FoliageColor.getEvergreenColor()), Blocks.SPRUCE_LEAVES);
        event.register((state, level, pos, tint) -> foliage(FoliageColor.getBirchColor()), Blocks.BIRCH_LEAVES);
        event.register((state, level, pos, tint) -> foliage(FoliageColor.getMangroveColor()), Blocks.MANGROVE_LEAVES);
    }

    /** Reconstruit les chunks affichés pour appliquer les nouvelles teintes (coûteux : à la demande uniquement). */
    public static void rebuildChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null && mc.level != null) {
            mc.levelRenderer.allChanged();
        }
    }
}
