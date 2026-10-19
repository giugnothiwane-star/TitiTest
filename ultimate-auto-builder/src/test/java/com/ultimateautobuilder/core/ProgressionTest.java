package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.progression.ItemCost;
import com.ultimateautobuilder.core.progression.MaterialInventory;
import com.ultimateautobuilder.core.progression.PlayerProgress;
import com.ultimateautobuilder.core.progression.ProgressionRules;
import com.ultimateautobuilder.core.progression.Quest;
import com.ultimateautobuilder.core.progression.QuestBook;
import com.ultimateautobuilder.core.progression.Tier;
import com.ultimateautobuilder.core.progression.UnlockRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Progression : coûts de déblocage, transactions de matériaux, quêtes et sauvegarde. */
class ProgressionTest {
    private static Catalog catalog;

    @BeforeAll
    static void load() {
        catalog = DefaultCatalog.create();
    }

    /** Inventaire de test : compte les retraits pour détecter une double consommation. */
    static final class FakeInventory implements MaterialInventory {
        final Map<String, Integer> items = new HashMap<>();
        int removals;

        FakeInventory with(String item, int n) {
            items.merge(item, n, Integer::sum);
            return this;
        }

        @Override
        public int count(String item) {
            return items.getOrDefault(item, 0);
        }

        @Override
        public void remove(String item, int amount) {
            removals++;
            int left = count(item) - amount;
            if (left < 0) {
                throw new IllegalStateException("Retrait au-delà du stock");
            }
            items.put(item, left);
        }
    }

    /** Faits de jeu simulés. */
    static final class FakeFacts implements PlayerProgress.Facts {
        final Map<String, Integer> mined = new HashMap<>();
        final Set<String> advancements = new HashSet<>();

        @Override
        public int mined(List<String> blocks) {
            int n = 0;
            for (String b : blocks) {
                n += mined.getOrDefault(b, 0);
            }
            return n;
        }

        @Override
        public boolean advancement(String id) {
            return advancements.contains(id);
        }
    }

    @Test
    void everyCatalogEntryHasAValidRule() {
        Set<String> materials = Set.of("minecraft:cobblestone", "minecraft:iron_ingot", "minecraft:diamond", "minecraft:netherite_ingot",
                "minecraft:cherry_log", "minecraft:cherry_planks", "minecraft:pink_petals", "minecraft:lantern", "minecraft:gold_ingot",
                "minecraft:copper_ingot", "minecraft:quartz", "minecraft:oak_log", "minecraft:spruce_log", "minecraft:bamboo");
        int free = 0;
        for (ModelEntry e : catalog.all()) {
            UnlockRule r = ProgressionRules.rule(e);
            assertNotNull(r, e.id());
            if (r.free()) {
                free++;
            }
            for (ItemCost c : r.cost()) {
                assertTrue(materials.contains(c.item()), e.id() + " : matériau inattendu " + c.item());
            }
            // Un niveau supérieur ne doit jamais être gratuit.
            assertEquals(r.tier() == Tier.DEBUTANT, r.cost().isEmpty(), e.id());
        }
        assertTrue(free >= 10 && free < catalog.size() / 2, "constructions de départ : " + free);
    }

    @Test
    void startHousesExistAndLargerHousesCostMore() {
        long starters = catalog.search("", Category.HOUSES, null, null).stream().filter(e -> ProgressionRules.rule(e).free()).count();
        assertTrue(starters >= 5, "maisons de départ : " + starters);
        UnlockRule simple = ProgressionRules.rule(catalog.get("maison_bois"));
        assertTrue(simple.free());
        for (ModelEntry e : catalog.all()) {
            UnlockRule r = ProgressionRules.rule(e);
            if (e.complexity() >= 4) {
                assertTrue(r.tier().atLeast(Tier.FER), e.id() + " devrait demander au moins le fer");
            }
        }
    }

    @Test
    void unlockChecksTierThenMaterialsAndConsumesOnce() {
        PlayerProgress p = new PlayerProgress();
        UnlockRule rule = new UnlockRule(Tier.PIERRE, List.of(new ItemCost("minecraft:cobblestone", 48)));
        FakeInventory inv = new FakeInventory().with("minecraft:cobblestone", 100);
        assertEquals(PlayerProgress.UnlockStatus.TIER_TOO_LOW, p.unlock("x", rule, inv).status());
        assertEquals(0, inv.removals, "aucun retrait si le niveau est insuffisant");
        reachTier(p, Tier.PIERRE);
        PlayerProgress.UnlockResult ok = p.unlock("x", rule, inv);
        assertEquals(PlayerProgress.UnlockStatus.OK, ok.status(), ok.message());
        assertEquals(52, inv.count("minecraft:cobblestone"));
        // Double clic / requête répétée : rien n'est retiré une seconde fois.
        assertEquals(PlayerProgress.UnlockStatus.ALREADY, p.unlock("x", rule, inv).status());
        assertEquals(52, inv.count("minecraft:cobblestone"));
        assertEquals(1, inv.removals);
        assertTrue(p.isUnlocked("x", rule));
    }

    @Test
    void missingMaterialsAreReportedAndNothingIsTaken() {
        PlayerProgress p = new PlayerProgress();
        reachTier(p, Tier.DIAMANT);
        UnlockRule rule = new UnlockRule(Tier.DIAMANT, List.of(new ItemCost("minecraft:diamond", 3), new ItemCost("minecraft:iron_ingot", 16)));
        FakeInventory inv = new FakeInventory().with("minecraft:diamond", 5).with("minecraft:iron_ingot", 10);
        PlayerProgress.UnlockResult r = p.unlock("villa", rule, inv);
        assertEquals(PlayerProgress.UnlockStatus.MISSING_MATERIALS, r.status());
        assertEquals(List.of(new ItemCost("minecraft:iron_ingot", 6)), r.missing());
        assertEquals(0, inv.removals, "transaction refusée : aucun matériau retiré");
        assertEquals(5, inv.count("minecraft:diamond"));
        assertFalse(p.isUnlocked("villa", rule));
    }

    @Test
    void questsFollowTheirChainAndRewardOnce() {
        PlayerProgress p = new PlayerProgress();
        FakeFacts f = new FakeFacts();
        Quest first = QuestBook.get("decouverte");
        Quest stone = QuestBook.get("pierre");
        assertEquals(Quest.State.IN_PROGRESS, p.state(first, f));
        assertEquals(Quest.State.LOCKED, p.state(stone, f));
        assertEquals(PlayerProgress.ClaimStatus.NOT_READY, p.claim("decouverte", f).status(), "cliquer ne suffit pas");
        p.markCrystalOpened();
        assertEquals(Quest.State.READY, p.state(first, f));
        assertEquals(PlayerProgress.ClaimStatus.OK, p.claim("decouverte", f).status());
        assertEquals(PlayerProgress.ClaimStatus.ALREADY, p.claim("decouverte", f).status());
        // Les blocs minés avant d'atteindre la quête comptent (statistique réelle du monde).
        f.mined.put("minecraft:stone", 70);
        assertEquals(PlayerProgress.ClaimStatus.LOCKED, p.claim("pierre", f).status());
        p.markCatalogOpened();
        assertEquals(PlayerProgress.ClaimStatus.OK, p.claim("catalogue", f).status());
        p.recordBuild("houses");
        assertEquals(PlayerProgress.ClaimStatus.OK, p.claim("premiere_maison", f).status());
        assertEquals(Tier.DEBUTANT, p.tier());
        assertEquals(PlayerProgress.ClaimStatus.OK, p.claim("pierre", f).status());
        assertEquals(Tier.PIERRE, p.tier());
    }

    @Test
    void wholeChainIsCompletableAndReachesNetherite() {
        PlayerProgress p = new PlayerProgress();
        reachTier(p, Tier.NETHERITE);
        assertEquals(Tier.NETHERITE, p.tier());
        assertTrue(p.unlocked().contains("sakura_luxe"), "la quête Sakura offre la villa de luxe");
        for (Quest q : QuestBook.all()) {
            for (String u : q.reward().unlocks()) {
                assertNotNull(catalog.get(u), "récompense inexistante : " + u);
            }
            if (q.prerequisite() != null) {
                assertNotNull(QuestBook.get(q.prerequisite()), q.id());
            }
        }
    }

    @Test
    void progressSurvivesSaveAndCorruptedFields() {
        PlayerProgress p = new PlayerProgress();
        reachTier(p, Tier.FER);
        p.recordBuild("sakura");
        String text = p.serialize();
        List<String> warnings = new ArrayList<>();
        PlayerProgress back = PlayerProgress.parse(text, warnings);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(p.tier(), back.tier());
        assertEquals(p.claimed(), back.claimed());
        assertEquals(p.builds(), back.builds());
        assertEquals(p.categoryBuilds("sakura"), back.categoryBuilds("sakura"));
        // Un champ corrompu n'efface pas le reste.
        String broken = text.replace("builds=" + p.builds(), "builds=abc").replaceFirst("tier=FER", "tier=MITHRIL");
        PlayerProgress partial = PlayerProgress.parse(broken, warnings);
        assertFalse(warnings.isEmpty());
        assertEquals(p.claimed(), partial.claimed());
        assertEquals(0, partial.builds());
        assertEquals(Tier.DEBUTANT, partial.tier());
        assertNotNull(PlayerProgress.parse("\u0000\u0001 n'importe quoi", new ArrayList<>()));
    }

    /** Fait avancer la chaîne de quêtes jusqu'au niveau voulu avec des faits simulés. */
    static void reachTier(PlayerProgress p, Tier target) {
        FakeFacts f = new FakeFacts();
        f.mined.put("minecraft:stone", 1000);
        f.mined.put("minecraft:coal_ore", 100);
        f.mined.put("minecraft:copper_ore", 100);
        f.mined.put("minecraft:iron_ore", 100);
        f.mined.put("minecraft:gold_ore", 100);
        f.mined.put("minecraft:diamond_ore", 100);
        f.mined.put("minecraft:ancient_debris", 100);
        f.advancements.add("minecraft:story/enter_the_nether");
        p.markCrystalOpened();
        p.markCatalogOpened();
        for (int i = 0; i < 12; i++) {
            p.recordBuild(i == 0 ? "sakura" : "houses");
        }
        FakeInventory inv = new FakeInventory().with("minecraft:cobblestone", 1000);
        boolean progressed = true;
        while (!p.tier().atLeast(target) && progressed) {
            progressed = false;
            for (Quest q : QuestBook.all()) {
                if (p.tier().atLeast(target)) {
                    break;
                }
                if (q.objective() == Quest.Objective.UNLOCK && p.unlocks() == 0 && p.tier().atLeast(Tier.PIERRE)) {
                    p.unlock("test_unlock", new UnlockRule(Tier.PIERRE, List.of(new ItemCost("minecraft:cobblestone", 1))), inv);
                }
                if (p.claim(q.id(), f).status() == PlayerProgress.ClaimStatus.OK) {
                    progressed = true;
                }
            }
        }
    }
}
