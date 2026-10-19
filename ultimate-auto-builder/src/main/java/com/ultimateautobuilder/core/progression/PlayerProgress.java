package com.ultimateautobuilder.core.progression;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Progression d'un joueur : niveau d'architecte, constructions débloquées, quêtes réclamées et
 * compteurs. Toutes les opérations sont déterministes et vérifient leurs conditions : une quête
 * ne se réclame qu'une fois, un déblocage ne consomme les matériaux qu'une fois et seulement si
 * tous sont présents. Utilisé sur le thread serveur uniquement.
 */
public final class PlayerProgress {
    public static final int FORMAT = 1;

    /** Faits de jeu lus par le serveur (statistiques vanilla, progrès). */
    public interface Facts {
        /** Total des blocs minés parmi les identifiants donnés. */
        int mined(List<String> blocks);

        boolean advancement(String id);
    }

    public enum UnlockStatus { OK, ALREADY, FREE, TIER_TOO_LOW, MISSING_MATERIALS }

    public record UnlockResult(UnlockStatus status, List<ItemCost> missing, String message) {
    }

    public enum ClaimStatus { OK, UNKNOWN, LOCKED, NOT_READY, ALREADY }

    public record ClaimResult(ClaimStatus status, String message) {
    }

    private Tier tier = Tier.DEBUTANT;
    private String title = "";
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Set<String> claimed = new LinkedHashSet<>();
    private final Map<String, Integer> categoryBuilds = new TreeMap<>();
    private int builds;
    private int unlocks;
    private boolean openedCrystal, openedCatalog;

    public Tier tier() {
        return tier;
    }

    public String title() {
        return title;
    }

    public Set<String> unlocked() {
        return Collections.unmodifiableSet(unlocked);
    }

    public Set<String> claimed() {
        return Collections.unmodifiableSet(claimed);
    }

    public int builds() {
        return builds;
    }

    public int unlocks() {
        return unlocks;
    }

    public int categoryBuilds(String key) {
        return categoryBuilds.getOrDefault(key, 0);
    }

    /** Renvoie vrai si l'état a changé (sauvegarde nécessaire). */
    public boolean markCrystalOpened() {
        boolean changed = !openedCrystal;
        openedCrystal = true;
        return changed;
    }

    public boolean markCatalogOpened() {
        boolean changed = !openedCatalog;
        openedCatalog = true;
        return changed;
    }

    public void recordBuild(String categoryKey) {
        builds++;
        if (categoryKey != null && !categoryKey.isBlank()) {
            categoryBuilds.merge(categoryKey, 1, Integer::sum);
        }
    }

    // ------------------------------------------------------------------ constructions

    public boolean isUnlocked(String id, UnlockRule rule) {
        return rule.free() || unlocked.contains(id);
    }

    /**
     * Débloque une construction : niveau suffisant puis matériaux tous présents, retirés en une
     * seule fois. Une construction déjà débloquée ne coûte rien (double clic, requête répétée).
     */
    public UnlockResult unlock(String id, UnlockRule rule, MaterialInventory inv) {
        if (rule.free()) {
            return new UnlockResult(UnlockStatus.FREE, List.of(), "Construction disponible dès le départ");
        }
        if (unlocked.contains(id)) {
            return new UnlockResult(UnlockStatus.ALREADY, List.of(), "Déjà débloquée");
        }
        if (!tier.atLeast(rule.tier())) {
            return new UnlockResult(UnlockStatus.TIER_TOO_LOW, List.of(), "Niveau requis : " + rule.tier().label + " (actuel : " + tier.label + ")");
        }
        List<ItemCost> missing = missing(rule.cost(), inv);
        if (!missing.isEmpty()) {
            return new UnlockResult(UnlockStatus.MISSING_MATERIALS, missing, "Il manque : " + ProgressionRules.describe(missing));
        }
        for (ItemCost c : rule.cost()) {
            inv.remove(c.item(), c.count());
        }
        unlocked.add(id);
        unlocks++;
        return new UnlockResult(UnlockStatus.OK, List.of(), "Débloquée contre " + ProgressionRules.describe(rule.cost()));
    }

    public static List<ItemCost> missing(List<ItemCost> cost, MaterialInventory inv) {
        List<ItemCost> out = new ArrayList<>();
        for (ItemCost c : cost) {
            int have = inv.count(c.item());
            if (have < c.count()) {
                out.add(new ItemCost(c.item(), c.count() - have));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ quêtes

    public int value(Quest q, Facts f) {
        return switch (q.objective()) {
            case OPEN_CRYSTAL -> openedCrystal ? 1 : 0;
            case OPEN_CATALOG -> openedCatalog ? 1 : 0;
            case MINE -> Math.max(0, f.mined(q.targets()));
            case ADVANCEMENT -> q.targets().isEmpty() ? 0 : f.advancement(q.targets().get(0)) ? 1 : 0;
            case BUILD -> builds;
            case BUILD_CATEGORY -> q.targets().isEmpty() ? 0 : categoryBuilds(q.targets().get(0));
            case UNLOCK -> unlocks;
        };
    }

    public Quest.State state(Quest q, Facts f) {
        if (claimed.contains(q.id())) {
            return Quest.State.DONE;
        }
        if (q.prerequisite() != null && !claimed.contains(q.prerequisite())) {
            return Quest.State.LOCKED;
        }
        return value(q, f) >= q.required() ? Quest.State.READY : Quest.State.IN_PROGRESS;
    }

    /** Réclame la récompense d'une quête terminée ; une seule fois. */
    public ClaimResult claim(String questId, Facts f) {
        Quest q = QuestBook.get(questId);
        if (q == null) {
            return new ClaimResult(ClaimStatus.UNKNOWN, "Quête inconnue");
        }
        switch (state(q, f)) {
            case DONE -> {
                return new ClaimResult(ClaimStatus.ALREADY, "Récompense déjà récupérée");
            }
            case LOCKED -> {
                return new ClaimResult(ClaimStatus.LOCKED, "Terminez d'abord : " + QuestBook.get(q.prerequisite()).name());
            }
            case IN_PROGRESS -> {
                return new ClaimResult(ClaimStatus.NOT_READY, "Objectif non atteint : " + value(q, f) + " / " + q.required());
            }
            default -> {
            }
        }
        claimed.add(q.id());
        Quest.Reward r = q.reward();
        if (r.tier() != null && r.tier().ordinal() > tier.ordinal()) {
            tier = r.tier();
        }
        if (r.title() != null && !r.title().isBlank()) {
            title = r.title();
        }
        unlocked.addAll(r.unlocks());
        return new ClaimResult(ClaimStatus.OK, "Récompense : " + r.describe());
    }

    // ------------------------------------------------------------------ sauvegarde

    /** Format texte (propriétés) : lisible, tolérant aux valeurs invalides champ par champ. */
    public String serialize() {
        Properties p = new Properties();
        p.setProperty("format", Integer.toString(FORMAT));
        p.setProperty("tier", tier.name());
        p.setProperty("title", title);
        p.setProperty("unlocked", String.join(",", unlocked));
        p.setProperty("claimed", String.join(",", claimed));
        p.setProperty("builds", Integer.toString(builds));
        p.setProperty("unlocks", Integer.toString(unlocks));
        p.setProperty("openedCrystal", Boolean.toString(openedCrystal));
        p.setProperty("openedCatalog", Boolean.toString(openedCatalog));
        StringBuilder cats = new StringBuilder();
        categoryBuilds.forEach((k, v) -> cats.append(cats.isEmpty() ? "" : ",").append(k).append(':').append(v));
        p.setProperty("categoryBuilds", cats.toString());
        StringWriter w = new StringWriter();
        try {
            p.store(w, "Progression Ultimate Auto Builder");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return w.toString();
    }

    /**
     * Relecture : chaque champ invalide est ignoré et signalé dans {@code warnings}, sans perdre
     * le reste de la progression.
     */
    public static PlayerProgress parse(String text, List<String> warnings) {
        PlayerProgress pp = new PlayerProgress();
        Properties p = new Properties();
        try {
            p.load(new StringReader(text == null ? "" : text));
        } catch (IOException | IllegalArgumentException e) {
            warnings.add("Fichier illisible : " + e.getMessage());
            return pp;
        }
        try {
            pp.tier = Tier.valueOf(p.getProperty("tier", "DEBUTANT").trim());
        } catch (IllegalArgumentException e) {
            warnings.add("Niveau invalide : " + p.getProperty("tier"));
        }
        pp.title = p.getProperty("title", "").trim();
        if (pp.title.length() > 64) {
            pp.title = pp.title.substring(0, 64);
        }
        for (String id : p.getProperty("unlocked", "").split(",")) {
            String s = id.trim();
            if (s.matches("[a-z0-9_:.-]{1,128}")) {
                pp.unlocked.add(s);
            } else if (!s.isEmpty()) {
                warnings.add("Construction ignorée : " + s);
            }
        }
        for (String id : p.getProperty("claimed", "").split(",")) {
            String s = id.trim();
            if (QuestBook.get(s) != null) {
                pp.claimed.add(s);
            } else if (!s.isEmpty()) {
                warnings.add("Quête inconnue ignorée : " + s);
            }
        }
        pp.builds = nonNegative(p, "builds", warnings);
        pp.unlocks = nonNegative(p, "unlocks", warnings);
        pp.openedCrystal = Boolean.parseBoolean(p.getProperty("openedCrystal", "false").trim());
        pp.openedCatalog = Boolean.parseBoolean(p.getProperty("openedCatalog", "false").trim());
        for (String kv : p.getProperty("categoryBuilds", "").split(",")) {
            int c = kv.indexOf(':');
            if (c <= 0) {
                continue;
            }
            try {
                int v = Integer.parseInt(kv.substring(c + 1).trim());
                if (v > 0) {
                    pp.categoryBuilds.put(kv.substring(0, c).trim(), v);
                }
            } catch (NumberFormatException e) {
                warnings.add("Compteur invalide : " + kv);
            }
        }
        return pp;
    }

    private static int nonNegative(Properties p, String key, List<String> warnings) {
        try {
            return Math.max(0, Integer.parseInt(p.getProperty(key, "0").trim()));
        } catch (NumberFormatException e) {
            warnings.add("Valeur invalide pour " + key);
            return 0;
        }
    }
}
