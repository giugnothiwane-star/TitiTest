package com.ultimateautobuilder.core.catalog;

import com.ultimateautobuilder.core.util.TextNormalizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Registre des modèles : enregistrement, recherche, filtres et tris. */
public final class Catalog {
    public enum Sort {
        NAME("Nom"),
        SIZE("Taille"),
        COMPLEXITY("Complexité"),
        CATEGORY("Catégorie");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final LinkedHashMap<String, ModelEntry> entries = new LinkedHashMap<>();
    private final EnumMap<Category, List<ModelEntry>> byCategory = new EnumMap<>(Category.class);

    public synchronized void register(ModelEntry entry) {
        if (entries.containsKey(entry.id())) {
            throw new IllegalStateException("Modèle déjà enregistré: " + entry.id());
        }
        entries.put(entry.id(), entry);
        byCategory.computeIfAbsent(entry.category(), c -> new ArrayList<>()).add(entry);
    }

    public synchronized boolean unregister(String id) {
        ModelEntry e = entries.remove(id);
        if (e == null) {
            return false;
        }
        List<ModelEntry> list = byCategory.get(e.category());
        if (list != null) {
            list.remove(e);
        }
        return true;
    }

    public synchronized ModelEntry get(String id) {
        return entries.get(id);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized List<ModelEntry> all() {
        return new ArrayList<>(entries.values());
    }

    public synchronized List<ModelEntry> byCategory(Category category) {
        return new ArrayList<>(byCategory.getOrDefault(category, Collections.emptyList()));
    }

    public synchronized int count(Category category) {
        return byCategory.getOrDefault(category, Collections.emptyList()).size();
    }

    /**
     * Recherche insensible à la casse et aux accents. Chaque mot de la requête doit apparaître
     * (en préfixe de mot) dans le nom, la catégorie ou les mots-clés du modèle.
     */
    public List<ModelEntry> search(String query, Category category, Predicate<ModelEntry> extraFilter, Sort sort) {
        String[] words = TextNormalizer.tokens(query);
        List<ModelEntry> source = category == null ? all() : byCategory(category);
        List<ModelEntry> out = new ArrayList<>();
        for (ModelEntry e : source) {
            if (extraFilter != null && !extraFilter.test(e)) {
                continue;
            }
            if (matches(e, words)) {
                out.add(e);
            }
        }
        out.sort(comparator(sort));
        return out;
    }

    static boolean matches(ModelEntry e, String[] words) {
        String text = e.searchText();
        for (String w : words) {
            if (!text.contains(" " + w) && !(w.length() >= 4 && text.contains(w))) {
                return false;
            }
        }
        return true;
    }

    public static Comparator<ModelEntry> comparator(Sort sort) {
        Comparator<ModelEntry> byName = Comparator.comparing(e -> TextNormalizer.normalize(e.name()));
        if (sort == null) {
            return byName;
        }
        return switch (sort) {
            case NAME -> byName;
            case SIZE -> Comparator.<ModelEntry>comparingInt(e -> e.sizes().normal()).thenComparing(byName);
            case COMPLEXITY -> Comparator.<ModelEntry>comparingInt(ModelEntry::complexity).thenComparing(byName);
            case CATEGORY -> Comparator.<ModelEntry, Integer>comparing(e -> e.category().ordinal()).thenComparing(byName);
        };
    }

    public static Predicate<ModelEntry> idIn(Collection<String> ids) {
        Set<String> set = ids instanceof Set<String> s ? s : new java.util.HashSet<>(ids);
        return e -> set.contains(e.id());
    }

    /** Filtre par ordre de grandeur de la taille normale. */
    public static Predicate<ModelEntry> sizeBetween(int min, int max) {
        return e -> e.sizes().normal() >= min && e.sizes().normal() <= max;
    }

    public synchronized Map<Category, Integer> counts() {
        EnumMap<Category, Integer> m = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            m.put(c, count(c));
        }
        return m;
    }
}
