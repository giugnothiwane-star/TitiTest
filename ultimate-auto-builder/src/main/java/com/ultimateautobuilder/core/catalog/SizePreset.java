package com.ultimateautobuilder.core.catalog;

public enum SizePreset {
    SMALL("Petit"),
    NORMAL("Normal"),
    LARGE("Grand"),
    GIANT("Géant"),
    CUSTOM("Personnalisé");

    private final String displayName;

    SizePreset(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static SizePreset byOrdinal(int ordinal) {
        SizePreset[] v = values();
        return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NORMAL;
    }
}
