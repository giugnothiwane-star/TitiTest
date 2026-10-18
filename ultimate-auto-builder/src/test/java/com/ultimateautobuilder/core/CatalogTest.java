package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogTest {
    private static Catalog catalog;

    @BeforeAll
    static void load() {
        catalog = DefaultCatalog.create();
    }

    @Test
    void containsAtLeast400Models() {
        assertTrue(catalog.size() >= 400, "Catalogue : " + catalog.size() + " modèles");
    }

    @Test
    void everyCategoryReachesItsTarget() {
        for (Category c : Category.values()) {
            assertTrue(catalog.count(c) >= c.target(), c + " : " + catalog.count(c) + " / " + c.target());
        }
    }

    @Test
    void idsAndNamesAreUnique() {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (ModelEntry e : catalog.all()) {
            assertTrue(ids.add(e.id()), "Identifiant en double : " + e.id());
            assertTrue(names.add(e.name()), "Nom en double : " + e.name());
            assertFalse(e.name().isBlank());
        }
    }

    @Test
    void searchIgnoresCaseAndAccents() {
        assertEquals("pikachu", catalog.search("PIKACHU", null, null, Catalog.Sort.NAME).get(0).id());
        assertFalse(catalog.search("evoli", null, null, Catalog.Sort.NAME).isEmpty());
        List<ModelEntry> withAccent = catalog.search("château", null, null, Catalog.Sort.NAME);
        List<ModelEntry> without = catalog.search("chateau", null, null, Catalog.Sort.NAME);
        assertEquals(withAccent, without);
        assertTrue(without.size() >= 10);
    }

    @Test
    void searchFindsCommonTerms() {
        assertTrue(catalog.search("maison", null, null, null).size() >= 30);
        assertTrue(catalog.search("dragon", null, null, null).size() >= 6);
        assertTrue(catalog.search("goku", null, null, null).size() >= 3);
        assertTrue(catalog.search("pokemon dracaufeu", null, null, null).size() == 1);
        assertTrue(catalog.search("zzzz introuvable", null, null, null).isEmpty());
    }

    @Test
    void searchByCategoryAndSort() {
        List<ModelEntry> pokemons = catalog.search("", Category.POKEMON, null, Catalog.Sort.NAME);
        assertEquals(catalog.count(Category.POKEMON), pokemons.size());
        for (int i = 1; i < pokemons.size(); i++) {
            assertTrue(Catalog.comparator(Catalog.Sort.NAME).compare(pokemons.get(i - 1), pokemons.get(i)) <= 0);
        }
        List<ModelEntry> bySize = catalog.search("", null, null, Catalog.Sort.SIZE);
        for (int i = 1; i < bySize.size(); i++) {
            assertTrue(bySize.get(i - 1).sizes().normal() <= bySize.get(i).sizes().normal());
        }
        List<ModelEntry> favorites = catalog.search("", null, Catalog.idIn(List.of("goku", "pikachu")), Catalog.Sort.NAME);
        assertEquals(2, favorites.size());
    }

    @Test
    void sizePresetsAreOrdered() {
        for (ModelEntry e : catalog.all()) {
            var s = e.sizes();
            assertTrue(s.small() <= s.normal() && s.normal() <= s.large() && s.large() <= s.giant(), e.id());
        }
    }
}
