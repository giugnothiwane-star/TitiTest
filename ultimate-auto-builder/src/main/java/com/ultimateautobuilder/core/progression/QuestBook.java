package com.ultimateautobuilder.core.progression;

import com.ultimateautobuilder.core.progression.Quest.Objective;
import com.ultimateautobuilder.core.progression.Quest.Reward;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chaîne de quêtes de l'architecte. Chaque quête dépend de la précédente de sa branche ; les
 * récompenses font monter le niveau d'architecte (accès aux constructions) ou offrent des maisons.
 */
public final class QuestBook {
    private static final List<String> STONE = List.of("minecraft:stone", "minecraft:deepslate", "minecraft:cobblestone", "minecraft:andesite",
            "minecraft:diorite", "minecraft:granite", "minecraft:tuff");
    private static final List<Quest> QUESTS = List.of(
            new Quest("decouverte", "Découvrir l'Ultimate Crystal", "Utilisez le cristal pour ouvrir le menu principal.", Objective.OPEN_CRYSTAL,
                    List.of(), 1, null, new Reward(null, "Apprenti architecte", List.of())),
            new Quest("catalogue", "Ouvrir le catalogue", "Parcourez le catalogue des maisons depuis le cristal.", Objective.OPEN_CATALOG,
                    List.of(), 1, "decouverte", new Reward(null, null, List.of())),
            new Quest("premiere_maison", "Première maison", "Faites construire une maison de départ jusqu'au bout.", Objective.BUILD, List.of(), 1,
                    "catalogue", new Reward(null, "Bâtisseur", List.of())),
            new Quest("pierre", "Tailleur de pierre", "Minez 64 blocs de pierre (pierre, roche des abîmes, andésite…).", Objective.MINE, STONE, 64,
                    "premiere_maison", new Reward(Tier.PIERRE, null, List.of())),
            new Quest("charbon", "Charbon de bois", "Minez 16 minerais de charbon.", Objective.MINE,
                    List.of("minecraft:coal_ore", "minecraft:deepslate_coal_ore"), 16, "pierre", new Reward(null, null, List.of())),
            new Quest("cuivre", "Reflets de cuivre", "Minez 12 minerais de cuivre.", Objective.MINE,
                    List.of("minecraft:copper_ore", "minecraft:deepslate_copper_ore"), 12, "pierre", new Reward(null, null, List.of())),
            new Quest("maison_pierre", "Maison en pierre", "Débloquez une construction avec vos matériaux.", Objective.UNLOCK, List.of(), 1, "pierre",
                    new Reward(null, null, List.of())),
            new Quest("fer", "Forgeron", "Minez 24 minerais de fer.", Objective.MINE, List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore"), 24,
                    "charbon", new Reward(Tier.FER, null, List.of())),
            new Quest("or", "Filon d'or", "Minez 8 minerais d'or.", Objective.MINE,
                    List.of("minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore"), 8, "fer",
                    new Reward(null, null, List.of())),
            new Quest("bati_fer", "Chantiers en série", "Terminez 3 constructions.", Objective.BUILD, List.of(), 3, "fer",
                    new Reward(null, "Maître d'œuvre", List.of())),
            new Quest("diamant", "Éclat de diamant", "Minez 5 minerais de diamant.", Objective.MINE,
                    List.of("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"), 5, "fer", new Reward(Tier.DIAMANT, null, List.of())),
            new Quest("sakura", "Sous les cerisiers", "Construisez une maison de la collection Sakura.", Objective.BUILD_CATEGORY, List.of("sakura"), 1,
                    "diamant", new Reward(null, "Jardinier du printemps", List.of("sakura_luxe"))),
            new Quest("nether", "Porte du Nether", "Entrez dans le Nether.", Objective.ADVANCEMENT, List.of("minecraft:story/enter_the_nether"), 1,
                    "diamant", new Reward(null, null, List.of())),
            new Quest("netherite", "Débris antiques", "Minez 4 débris antiques.", Objective.MINE, List.of("minecraft:ancient_debris"), 4, "nether",
                    new Reward(Tier.NETHERITE, null, List.of())),
            new Quest("grand_architecte", "Grand architecte", "Terminez 10 constructions.", Objective.BUILD, List.of(), 10, "netherite",
                    new Reward(null, "Grand architecte", List.of())));
    private static final Map<String, Quest> BY_ID = new LinkedHashMap<>();

    static {
        for (Quest q : QUESTS) {
            if (BY_ID.put(q.id(), q) != null) {
                throw new IllegalStateException("Quête en double : " + q.id());
            }
        }
    }

    private QuestBook() {
    }

    public static List<Quest> all() {
        return QUESTS;
    }

    public static Quest get(String id) {
        return BY_ID.get(id);
    }
}
