package com.ultimateautobuilder.core.catalog;

import com.ultimateautobuilder.core.geom.Sculpt;

import java.util.function.Supplier;

/** Aide à l'enregistrement concis des modèles d'une catégorie. */
public final class Reg {
    public static final SizeSpec CHARACTER = SizeSpec.height(8, 20, 40, 80);
    public static final SizeSpec CREATURE = SizeSpec.height(8, 18, 36, 72);
    public static final SizeSpec BIG_CREATURE = SizeSpec.height(12, 26, 50, 96);
    public static final SizeSpec STATUE = SizeSpec.height(12, 28, 56, 110);
    public static final SizeSpec HOUSE = SizeSpec.width(7, 15, 30, 60);
    public static final SizeSpec BUILDING = SizeSpec.width(11, 21, 35, 61);
    public static final SizeSpec CASTLE = SizeSpec.width(25, 41, 65, 101);
    public static final SizeSpec CITY = SizeSpec.width(48, 80, 128, 192);
    public static final SizeSpec TREE = SizeSpec.height(10, 22, 40, 72);
    public static final SizeSpec ISLAND = SizeSpec.width(15, 31, 55, 95);
    public static final SizeSpec VEHICLE = SizeSpec.length(8, 18, 36, 72);
    public static final SizeSpec PIXEL = SizeSpec.height(8, 16, 32, 64);

    private final Catalog catalog;
    private final Category category;

    public Reg(Catalog catalog, Category category) {
        this.catalog = catalog;
        this.category = category;
    }

    /** Modèle sculpté : la sculpture est construite paresseusement puis réutilisée (thread-safe). */
    public void sculpt(String id, String name, int complexity, SizeSpec sizes, Supplier<Sculpt> supplier, String... tags) {
        Lazy<Sculpt> lazy = new Lazy<>(supplier);
        catalog.register(new ModelEntry(id, name, category, tags, complexity, sizes, true,
                p -> lazy.get().voxelize(p.size(), p.hollow(), sizes.kind() == SizeSpec.Kind.HEIGHT).freeze().groundAtBottom()));
    }

    public void gen(String id, String name, int complexity, SizeSpec sizes, ModelGenerator generator, String... tags) {
        catalog.register(new ModelEntry(id, name, category, tags, complexity, sizes, false, generator));
    }

    public void genHollowable(String id, String name, int complexity, SizeSpec sizes, ModelGenerator generator, String... tags) {
        catalog.register(new ModelEntry(id, name, category, tags, complexity, sizes, true, generator));
    }

    public static final class Lazy<T> {
        private final Supplier<T> supplier;
        private volatile T value;

        public Lazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            T v = value;
            if (v == null) {
                synchronized (this) {
                    v = value;
                    if (v == null) {
                        v = supplier.get();
                        value = v;
                    }
                }
            }
            return v;
        }
    }
}
