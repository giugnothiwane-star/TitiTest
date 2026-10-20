package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.adventure.AdventureGen;
import com.ultimateautobuilder.core.adventure.ArmorSetReward;
import com.ultimateautobuilder.core.adventure.Blueprint;
import com.ultimateautobuilder.core.adventure.KeyTier;
import com.ultimateautobuilder.core.adventure.SpawnCategory;
import com.ultimateautobuilder.core.adventure.SpawnFamily;
import com.ultimateautobuilder.core.adventure.SpawnRules;
import com.ultimateautobuilder.core.adventure.SpawnerCycle;
import com.ultimateautobuilder.core.adventure.StructureType;
import com.ultimateautobuilder.core.adventure.Walkability;
import com.ultimateautobuilder.core.adventure.WardenVariant;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Règles d'aventure : probabilités, cycles des spawners, clés, récompenses et structures. */
class AdventureTest {
    private static SpawnRules.Dice dice(long seed) {
        Rng r = new Rng(seed);
        return new SpawnRules.Dice() {
            @Override
            public double nextDouble() {
                return r.nextDouble();
            }

            @Override
            public int nextInt(int bound) {
                return r.nextInt(bound);
            }
        };
    }

    @Test
    void ordinaryCreaturesAreEquiprobable() {
        List<String> pool = SpawnRules.eligible(SpawnFamily.MIXED, SpawnCategory.NORMAL, id -> true);
        assertEquals(12, pool.size());
        Map<String, Integer> counts = new HashMap<>();
        SpawnRules.Dice d = dice(1);
        int n = 240_000;
        for (int i = 0; i < n; i++) {
            counts.merge(SpawnRules.pickOrdinary(pool, d), 1, Integer::sum);
        }
        double expected = (double) n / pool.size();
        double chi2 = 0;
        for (String id : pool) {
            double diff = counts.getOrDefault(id, 0) - expected;
            chi2 += diff * diff / expected;
        }
        // 11 degrés de liberté : 31,3 correspond à p = 0,001.
        assertTrue(chi2 < 31.3, "répartition non uniforme, chi² = " + chi2 + " " + counts);
    }

    @Test
    void forbiddenEntitiesAreNeverEligible() {
        for (SpawnFamily f : SpawnFamily.values()) {
            for (SpawnCategory c : SpawnCategory.values()) {
                List<String> pool = SpawnRules.eligible(f, c, id -> true);
                assertFalse(pool.isEmpty(), f + " " + c);
                for (String id : pool) {
                    assertFalse(SpawnRules.FORBIDDEN.contains(id), id);
                }
                assertFalse(pool.contains("minecraft:warden"), "le Warden n'est jamais une créature ordinaire");
                assertFalse(pool.contains("minecraft:ender_dragon"));
            }
        }
        // Une combinaison sans variante possible est retirée, jamais remplacée par un adulte.
        List<String> babies = SpawnRules.eligible(SpawnFamily.UNDEAD, SpawnCategory.BABY, SpawnRules::hasNativeBaby);
        assertEquals(List.of("minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager", "minecraft:drowned"), babies);
    }

    @Test
    void familiesStaySeparate() {
        SpawnRules.Dice d = dice(7);
        for (int i = 0; i < 20_000; i++) {
            SpawnRules.Choice ch = SpawnRules.choose(SpawnFamily.ARACHNID, SpawnCategory.MEDIUM, false, id -> true, d);
            assertTrue(ch instanceof SpawnRules.Ordinary o && SpawnFamily.ARACHNID.members.contains(o.entityId()), String.valueOf(ch));
        }
    }

    @Test
    void wardenTriggersAtHalfAPercentPerValidAttempt() {
        SpawnRules.Dice d = dice(42);
        int n = 2_000_000, wardens = 0;
        for (int i = 0; i < n; i++) {
            if (SpawnRules.choose(SpawnFamily.MIXED, SpawnCategory.NORMAL, true, id -> true, d) instanceof SpawnRules.Warden) {
                wardens++;
            }
        }
        double rate = (double) wardens / n;
        // Écart-type ≈ 0,00005 : tolérance de 6 écarts-types.
        assertEquals(0.005, rate, 0.0003, "taux observé " + rate);
        // Sans autorisation (maisons, châteaux ordinaires) : jamais de Warden.
        for (int i = 0; i < 100_000; i++) {
            assertTrue(SpawnRules.choose(SpawnFamily.MIXED, SpawnCategory.NORMAL, false, id -> true, d) instanceof SpawnRules.Ordinary);
        }
    }

    @Test
    void wardenVariantsFollowTheirDistribution() {
        int total = 0;
        for (WardenVariant v : WardenVariant.values()) {
            total += v.weight;
        }
        assertEquals(100, total, "distribution normalisée");
        SpawnRules.Dice d = dice(3);
        Map<WardenVariant, Integer> counts = new EnumMap<>(WardenVariant.class);
        int n = 500_000;
        for (int i = 0; i < n; i++) {
            counts.merge(SpawnRules.pickWardenVariant(null, d), 1, Integer::sum);
        }
        assertEquals(0.70, counts.get(WardenVariant.NORMAL) / (double) n, 0.005);
        assertEquals(0.14, counts.get(WardenVariant.BABY) / (double) n, 0.005);
        assertEquals(0.14, counts.get(WardenVariant.MEDIUM) / (double) n, 0.005);
        assertEquals(0.02, counts.get(WardenVariant.BOSS) / (double) n, 0.002);
        assertTrue(counts.get(WardenVariant.BOSS) < counts.get(WardenVariant.BABY), "le boss est la variante la plus rare");
    }

    @Test
    void spawnerCategoriesNeverMix() {
        SpawnRules.Dice d = dice(9);
        for (int i = 0; i < 100_000; i++) {
            assertEquals(WardenVariant.BABY, SpawnRules.pickWardenVariant(SpawnCategory.BABY, d));
            assertEquals(WardenVariant.MEDIUM, SpawnRules.pickWardenVariant(SpawnCategory.MEDIUM, d));
            WardenVariant v = SpawnRules.pickWardenVariant(SpawnCategory.NORMAL, d);
            assertTrue(v == WardenVariant.NORMAL || v == WardenVariant.BOSS, String.valueOf(v));
        }
        int boss = 0, n = 400_000;
        for (int i = 0; i < n; i++) {
            if (SpawnRules.pickWardenVariant(SpawnCategory.NORMAL, d) == WardenVariant.BOSS) {
                boss++;
            }
        }
        // Distribution conditionnée : 2 / 72 ≈ 2,78 %.
        assertEquals(2.0 / 72, boss / (double) n, 0.002);
    }

    @Test
    void spawnerCycleProducesAtMostFourThenDepletes() {
        SpawnerCycle c = new SpawnerCycle(3);
        int spawned = 0, alive = 0, maxAlive = 0;
        for (int t = 0; t < 20_000; t++) {
            SpawnerCycle.Action a = c.step(20, alive);
            if (a == SpawnerCycle.Action.ATTEMPT) {
                c.onSpawned();
                spawned++;
                alive++;
                assertTrue(c.spawnedThisCycle() <= SpawnerCycle.PER_CYCLE);
            }
            maxAlive = Math.max(maxAlive, alive);
            // Les créatures meurent un peu plus tard.
            if (t % 37 == 0 && alive > 0) {
                alive--;
            }
            if (a == SpawnerCycle.Action.DEPLETED) {
                break;
            }
        }
        assertEquals(12, spawned, "3 cycles × 4 créatures");
        assertTrue(maxAlive <= 4, "jamais plus de 4 créatures vivantes : " + maxAlive);
        assertTrue(c.depleted());
        assertEquals(SpawnerCycle.Action.DEPLETED, c.step(1000, 0));
        // Rechargement : l'état est conservé et borné.
        SpawnerCycle back = SpawnerCycle.load(new int[]{3, 1, 9, 99999});
        assertEquals(1, back.cyclesDone());
        assertEquals(4, back.spawnedThisCycle());
        assertTrue(back.cooldown() <= SpawnerCycle.CYCLE_COOLDOWN);
    }

    @Test
    void keysOpenOnlyTheirOwnTier() {
        for (KeyTier k : KeyTier.values()) {
            for (KeyTier lock : KeyTier.values()) {
                assertEquals(k == lock, k.opens(lock), k + " → " + lock);
            }
        }
        assertFalse(KeyTier.COMMON.opens(KeyTier.OP));
        assertFalse(KeyTier.LEGENDARY.opens(KeyTier.COMMON));
    }

    @Test
    void armorSetsMatchOnlyFullSets() {
        assertEquals(ArmorSetReward.IRON, ArmorSetReward.wornSet("minecraft:iron_helmet", "minecraft:iron_chestplate",
                "minecraft:iron_leggings", "minecraft:iron_boots"));
        assertNull(ArmorSetReward.wornSet("minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:air"));
        assertNull(ArmorSetReward.wornSet("minecraft:iron_helmet", "minecraft:diamond_chestplate", "minecraft:iron_leggings",
                "minecraft:iron_boots"));
        assertEquals(ArmorSetReward.GOLD, ArmorSetReward.wornSet("minecraft:golden_helmet", "minecraft:golden_chestplate",
                "minecraft:golden_leggings", "minecraft:golden_boots"));
        assertEquals(List.of(new ArmorSetReward.Reward("minecraft:iron_ingot", 2), new ArmorSetReward.Reward("minecraft:bread", 16)),
                ArmorSetReward.LEATHER.rewards);
        assertEquals("ultimateautobuilder:op_key", ArmorSetReward.NETHERITE.rewards.get(1).item());
    }

    @Test
    void everyStructureIsBuildableAndAccessible() {
        for (StructureType t : StructureType.values()) {
            for (long seed = 1; seed <= 6; seed++) {
                Blueprint bp = AdventureGen.build(t, seed * 7919 + t.ordinal());
                String where = t + " graine " + seed;
                assertFalse(bp.model().isEmpty(), where);
                Walkability w = Walkability.of(bp);
                assertTrue(w.size() > 20, where + " : entrée inaccessible");
                for (Blueprint.Marker mk : bp.markers()) {
                    assertTrue(w.usable(mk.x(), mk.y(), mk.z()), where + " : coffre inaccessible en " + mk);
                }
                int spawners = 0;
                Map<SpawnCategory, Integer> cats = new EnumMap<>(SpawnCategory.class);
                VoxelModel m = bp.model();
                for (int i = 0; i < m.size(); i++) {
                    long c = m.cell(i);
                    String s = m.stateOf(c);
                    int x = VoxelModel.cx(c) - m.anchorX(), y = VoxelModel.cy(c) - m.anchorY(), z = VoxelModel.cz(c) - m.anchorZ();
                    String id = B.idOf(s);
                    if (id.equals("ultimateautobuilder:adventure_spawner")) {
                        spawners++;
                        String cat = s.replaceAll(".*category=([a-z]+).*", "$1");
                        cats.merge(SpawnCategory.byKey(cat), 1, Integer::sum);
                        assertTrue(w.usable(x, y, z), where + " : spawner inaccessible");
                    }
                    if (id.equals("ultimateautobuilder:locked_chest")) {
                        assertTrue(w.usable(x, y, z), where + " : coffre verrouillé inaccessible");
                    }
                }
                if (spawners > 0) {
                    assertEquals(3, spawners, where + " : trois spawners principaux");
                    for (SpawnCategory c : SpawnCategory.values()) {
                        assertEquals(1, cats.getOrDefault(c, 0), where + " : un spawner " + c);
                    }
                }
            }
        }
    }

    @Test
    void structuresAreDeterministicAndVaried() {
        Blueprint a = AdventureGen.build(StructureType.CURSED_CASTLE, 99);
        Blueprint b = AdventureGen.build(StructureType.CURSED_CASTLE, 99);
        assertEquals(a.model().size(), b.model().size());
        assertEquals(a.markers(), b.markers());
        // Les types de maisons diffèrent réellement (taille ou palette), pas seulement par la couleur.
        java.util.Set<String> signatures = new java.util.HashSet<>();
        for (StructureType t : StructureType.values()) {
            if (t.group == StructureType.Group.HOUSE) {
                Blueprint bp = AdventureGen.build(t, 5);
                signatures.add(bp.model().size() + ":" + bp.model().sizeY() + ":" + bp.markers().size());
            }
        }
        assertTrue(signatures.size() >= 10, "variété insuffisante : " + signatures.size());
    }

    @Test
    void housesHaveFoundationsAndCompleteRoofs() {
        for (StructureType t : List.of(StructureType.SMALL_WOOD_HOUSE, StructureType.STONE_HOUSE, StructureType.RARE_HOUSE,
                StructureType.LEGENDARY_HOUSE, StructureType.STARTER_HOUSE, StructureType.CELLAR_HOUSE)) {
            Blueprint bp = AdventureGen.build(t, 11);
            VoxelModel m = bp.model();
            int[] f = bp.footprint();
            // Plancher continu sous la maison (aucune maison flottante), toit au-dessus de chaque colonne intérieure.
            int w = 0, d = 0;
            for (int x = 0; x < 30; x++) {
                if (m.stateAt(x + m.anchorX(), m.anchorY(), m.anchorZ()) != null) {
                    w = x + 1;
                }
            }
            for (int z = 0; z < 30; z++) {
                if (m.stateAt(m.anchorX(), m.anchorY(), z + m.anchorZ()) != null) {
                    d = z + 1;
                }
            }
            assertTrue(w >= 7 && d >= 7, t + " " + w + "×" + d);
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    String floor = m.stateAt(x + m.anchorX(), m.anchorY(), z + m.anchorZ());
                    assertNotNull(floor, t + " : trou dans le plancher en " + x + "," + z);
                    boolean roof = false;
                    for (int y = 3; y <= f.length * 0 + m.sizeY(); y++) {
                        String s = m.stateAt(x + m.anchorX(), y + m.anchorY(), z + m.anchorZ());
                        if (s != null && !B.isAir(s) && !s.contains("lantern") && !s.contains("cobweb")) {
                            roof = true;
                        }
                    }
                    assertTrue(roof, t + " : colonne sans toit en " + x + "," + z);
                }
            }
        }
    }
}
