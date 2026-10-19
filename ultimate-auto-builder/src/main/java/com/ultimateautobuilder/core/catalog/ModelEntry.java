package com.ultimateautobuilder.core.catalog;

import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;
import com.ultimateautobuilder.core.util.TextNormalizer;

import java.util.Objects;

/**
 * Métadonnées légères d'un modèle. Les données lourdes (voxels) ne sont produites qu'à la
 * demande par {@link #generate}, jamais au chargement du catalogue.
 */
public final class ModelEntry {
    private final String id;
    private final String name;
    private final Category category;
    private final String[] tags;
    private final int complexity;
    private final SizeSpec sizes;
    private final boolean hollowable;
    private final ModelGenerator generator;
    private final String searchText;

    public ModelEntry(String id, String name, Category category, String[] tags, int complexity, SizeSpec sizes,
                      boolean hollowable, ModelGenerator generator) {
        if (id == null || !id.matches("[a-z0-9_]+(:[a-z0-9_./-]+)?")) {
            throw new IllegalArgumentException("Identifiant de modèle invalide: " + id);
        }
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.category = Objects.requireNonNull(category);
        this.tags = tags == null ? new String[0] : tags.clone();
        this.complexity = Math.max(1, Math.min(5, complexity));
        this.sizes = Objects.requireNonNull(sizes);
        this.hollowable = hollowable;
        this.generator = Objects.requireNonNull(generator);
        StringBuilder sb = new StringBuilder(TextNormalizer.normalize(name)).append(' ')
                .append(TextNormalizer.normalize(category.displayName())).append(' ').append(id.replace('_', ' '));
        for (String t : this.tags) {
            sb.append(' ').append(TextNormalizer.normalize(t));
        }
        this.searchText = " " + sb + " ";
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Category category() {
        return category;
    }

    public String[] tags() {
        return tags.clone();
    }

    public int complexity() {
        return complexity;
    }

    public SizeSpec sizes() {
        return sizes;
    }

    public boolean hollowable() {
        return hollowable;
    }

    String searchText() {
        return searchText;
    }

    public long seed() {
        return Rng.seedOf(id);
    }

    public VoxelModel generate(int size, boolean hollow) {
        return generator.generate(new GenParams(size, seed(), hollow && hollowable));
    }

    public VoxelModel generate(SizePreset preset, int customSize, boolean hollow) {
        return generate(sizes.resolve(preset, customSize), hollow);
    }

    @Override
    public String toString() {
        return id;
    }
}
