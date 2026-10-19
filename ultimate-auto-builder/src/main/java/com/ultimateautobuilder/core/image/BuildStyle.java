package com.ultimateautobuilder.core.image;

import java.util.EnumSet;
import java.util.Set;

/** Styles de conversion : palette autorisée, échelle, lissage et réduction des couleurs. */
public enum BuildStyle {
    FIDELE("Fidèle", "palette complète, couleurs au plus proche", 1.0, false, 0, true),
    VANILLA("Minecraft vanilla", "matériaux naturels (pierre, bois, terre cuite)", 1.0, true, 0, true),
    DETAILLE("Très détaillé", "palette complète, aucun lissage", 1.0, false, 0, false),
    REALISTE("Réaliste", "teintes sourdes, couleurs lissées", 1.0, true, 0, true),
    VOXEL_ART("Voxel art", "couleurs réduites et franches", 1.0, false, 10, true),
    MINIATURE("Miniature", "moitié de la taille demandée", 0.5, true, 0, true),
    GEANT("Géant", "double de la taille demandée (dans les limites)", 2.0, false, 0, true),
    PERSONNALISE("Personnalisé", "familles de blocs choisies", 1.0, false, 0, true);

    public final String label;
    public final String description;
    public final double scale;
    public final boolean smoothColors;
    /** Nombre de couleurs après réduction (0 = aucune réduction). */
    public final int posterize;
    /** Filtre de majorité sur les blocs (supprime les pixels isolés des photos). */
    public final boolean despeckle;

    BuildStyle(String label, String description, double scale, boolean smoothColors, int posterize, boolean despeckle) {
        this.label = label;
        this.description = description;
        this.scale = scale;
        this.smoothColors = smoothColors;
        this.posterize = posterize;
        this.despeckle = despeckle;
    }

    public BlockPalette palette(Set<BlockPalette.Family> custom) {
        return switch (this) {
            case VANILLA -> BlockPalette.of(EnumSet.of(BlockPalette.Family.STONE, BlockPalette.Family.WOOD, BlockPalette.Family.NATURAL,
                    BlockPalette.Family.TERRACOTTA));
            case REALISTE -> BlockPalette.of(EnumSet.of(BlockPalette.Family.STONE, BlockPalette.Family.WOOD, BlockPalette.Family.NATURAL,
                    BlockPalette.Family.TERRACOTTA, BlockPalette.Family.METAL));
            case VOXEL_ART -> BlockPalette.of(EnumSet.of(BlockPalette.Family.CONCRETE, BlockPalette.Family.WOOL));
            case PERSONNALISE -> custom == null || custom.isEmpty() ? BlockPalette.full() : BlockPalette.of(custom);
            default -> BlockPalette.full();
        };
    }
}
