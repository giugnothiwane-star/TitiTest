package com.ultimateautobuilder.builder;

/** Modes de construction proposés dans l'interface. */
public enum BuildMode {
    /** Remplace les blocs selon les paramètres (blocs précieux et indestructibles toujours protégés). */
    NORMAL("Normal"),
    /** N'écrase que l'air et les blocs remplaçables ; les conflits sont comptés et signalés. */
    PRUDENT("Prudent"),
    /** Comme NORMAL, avec des fondations prolongées jusqu'au sol sous la structure. */
    TERRAIN("Terrain"),
    /** Élève la structure de la hauteur aérienne configurée, sans toucher au sol. */
    AERIAL("Aérien");

    private final String label;

    BuildMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static BuildMode byOrdinal(int i) {
        BuildMode[] v = values();
        return i >= 0 && i < v.length ? v[i] : NORMAL;
    }
}
