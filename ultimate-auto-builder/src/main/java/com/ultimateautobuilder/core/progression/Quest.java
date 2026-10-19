package com.ultimateautobuilder.core.progression;

import java.util.List;

/**
 * Quête vérifiée par le serveur : l'avancement est lu dans des données de jeu réelles
 * (statistiques de minage, progrès vanilla, compteurs du mod), jamais déclaré par le client.
 *
 * @param targets blocs, progrès ou catégorie selon le type d'objectif
 * @param prerequisite quête à terminer avant celle-ci (null : disponible dès le début)
 */
public record Quest(String id, String name, String description, Objective objective, List<String> targets, int required,
                    String prerequisite, Reward reward) {
    public enum Objective {
        /** Ouvrir le cristal. */
        OPEN_CRYSTAL,
        /** Ouvrir le catalogue des maisons. */
        OPEN_CATALOG,
        /** Blocs minés (statistique vanilla, somme des blocs cibles). */
        MINE,
        /** Progrès vanilla obtenu (1 = obtenu). */
        ADVANCEMENT,
        /** Constructions terminées par le joueur. */
        BUILD,
        /** Constructions terminées dans une catégorie (cible : clé de catégorie). */
        BUILD_CATEGORY,
        /** Constructions débloquées avec des matériaux. */
        UNLOCK
    }

    /**
     * @param tier niveau d'architecte atteint (null : inchangé)
     * @param unlocks constructions offertes
     */
    public record Reward(Tier tier, String title, List<String> unlocks) {
        public Reward {
            unlocks = unlocks == null ? List.of() : List.copyOf(unlocks);
        }

        public String describe() {
            StringBuilder sb = new StringBuilder();
            if (tier != null) {
                sb.append("niveau « ").append(tier.label).append(" »");
            }
            if (!unlocks.isEmpty()) {
                sb.append(sb.isEmpty() ? "" : ", ").append(unlocks.size()).append(unlocks.size() > 1 ? " constructions offertes" : " construction offerte");
            }
            if (title != null && !title.isBlank()) {
                sb.append(sb.isEmpty() ? "" : ", ").append("titre « ").append(title).append(" »");
            }
            return sb.isEmpty() ? "aucune" : sb.toString();
        }
    }

    public Quest {
        targets = targets == null ? List.of() : List.copyOf(targets);
        if (required <= 0) {
            throw new IllegalArgumentException("Objectif invalide pour " + id);
        }
    }

    /** États affichés au joueur. */
    public enum State {
        LOCKED("verrouillée"), IN_PROGRESS("en cours"), READY("récompense à récupérer"), DONE("terminée");

        public final String label;

        State(String label) {
            this.label = label;
        }
    }
}
