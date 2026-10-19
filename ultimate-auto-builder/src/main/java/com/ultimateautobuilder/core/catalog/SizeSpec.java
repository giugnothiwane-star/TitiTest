package com.ultimateautobuilder.core.catalog;

/**
 * Tailles disponibles d'un modèle. {@code kind} précise la dimension pilotée :
 * hauteur pour les personnages, largeur au sol pour les bâtiments et les villes.
 */
public record SizeSpec(Kind kind, int small, int normal, int large, int giant, int min, int max) {
    public enum Kind {
        HEIGHT("hauteur"),
        WIDTH("largeur"),
        LENGTH("longueur"),
        /** Plus grande dimension (mobs : hauteur pour les bipèdes, longueur pour les animaux allongés). */
        DIMENSION("dimension principale");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public SizeSpec {
        if (min <= 0 || max < min || small < min || giant > max || small > normal || normal > large || large > giant) {
            throw new IllegalArgumentException("SizeSpec incohérent");
        }
    }

    public static SizeSpec height(int small, int normal, int large, int giant) {
        return new SizeSpec(Kind.HEIGHT, small, normal, large, giant, Math.max(4, small / 2), giant * 2);
    }

    public static SizeSpec width(int small, int normal, int large, int giant) {
        return new SizeSpec(Kind.WIDTH, small, normal, large, giant, Math.max(5, small), giant * 2);
    }

    public static SizeSpec length(int small, int normal, int large, int giant) {
        return new SizeSpec(Kind.LENGTH, small, normal, large, giant, Math.max(5, small / 2), giant * 2);
    }

    public static SizeSpec dimension(int small, int normal, int large, int giant) {
        return new SizeSpec(Kind.DIMENSION, small, normal, large, giant, Math.max(4, small / 2), giant * 2);
    }

    /** Valeur de taille pour un préréglage ; la taille personnalisée est bornée. */
    public int resolve(SizePreset preset, int custom) {
        return switch (preset) {
            case SMALL -> small;
            case NORMAL -> normal;
            case LARGE -> large;
            case GIANT -> giant;
            case CUSTOM -> Math.max(min, Math.min(max, custom));
        };
    }
}
