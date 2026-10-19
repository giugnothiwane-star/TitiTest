package com.ultimateautobuilder.client;

import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.progression.ProgressionRules;
import com.ultimateautobuilder.core.progression.Quest;
import com.ultimateautobuilder.core.progression.Tier;
import com.ultimateautobuilder.core.progression.UnlockRule;
import com.ultimateautobuilder.network.ProgressPayload;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dernier état de progression reçu du serveur. Sert uniquement à l'affichage (verrous, quêtes) :
 * chaque action est revérifiée par le serveur.
 */
public final class ClientProgress {
    private static ProgressPayload last;
    private static Set<String> unlocked = Set.of();
    private static long version;

    private ClientProgress() {
    }

    public static void set(ProgressPayload p) {
        last = p;
        unlocked = new HashSet<>(p.unlocked());
        version++;
    }

    public static void reset() {
        last = null;
        unlocked = Set.of();
        version++;
    }

    public static long version() {
        return version;
    }

    public static boolean known() {
        return last != null;
    }

    /** Progression appliquée à ce joueur (survie) ; faux tant que l'état n'est pas reçu. */
    public static boolean enforced() {
        return last != null && last.enforced();
    }

    public static Tier tier() {
        return last == null ? Tier.DEBUTANT : Tier.byOrdinal(last.tier());
    }

    public static String title() {
        return last == null ? "" : last.title();
    }

    public static int builds() {
        return last == null ? 0 : last.builds();
    }

    public static boolean isUnlocked(ModelEntry e) {
        if (!enforced()) {
            return true;
        }
        UnlockRule r = ProgressionRules.rule(e);
        return r.free() || unlocked.contains(e.id());
    }

    /** Coût lisible dans la langue du jeu (« 16 Bûche de cerisier, 32 Pierre taillée »). */
    public static String describe(List<com.ultimateautobuilder.core.progression.ItemCost> cost) {
        if (cost.isEmpty()) {
            return "gratuit";
        }
        StringBuilder sb = new StringBuilder();
        for (var c : cost) {
            var rl = net.minecraft.resources.ResourceLocation.tryParse(c.item());
            String name = rl != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl)
                    ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl).getDescription().getString() : c.shortName();
            sb.append(sb.isEmpty() ? "" : ", ").append(c.count()).append(' ').append(name);
        }
        return sb.toString();
    }

    public static List<ProgressPayload.QuestInfo> quests() {
        return last == null ? List.of() : last.quests();
    }

    public static Quest.State state(ProgressPayload.QuestInfo q) {
        Quest.State[] all = Quest.State.values();
        return q.state() >= 0 && q.state() < all.length ? all[q.state()] : Quest.State.LOCKED;
    }
}
