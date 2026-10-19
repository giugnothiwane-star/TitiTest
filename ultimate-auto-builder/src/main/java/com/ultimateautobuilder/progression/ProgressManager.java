package com.ultimateautobuilder.progression;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.progression.MaterialInventory;
import com.ultimateautobuilder.core.progression.PlayerProgress;
import com.ultimateautobuilder.core.progression.ProgressionRules;
import com.ultimateautobuilder.core.progression.Quest;
import com.ultimateautobuilder.core.progression.QuestBook;
import com.ultimateautobuilder.core.progression.UnlockRule;
import com.ultimateautobuilder.network.ProgressPayload;
import com.ultimateautobuilder.storage.ModStorage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Progression côté serveur : lecture et écriture des fichiers de chaque joueur (dossier du monde),
 * vérification des quêtes sur les statistiques et progrès réels, transactions de déblocage sur le
 * vrai inventaire, contrôle des constructions verrouillées. Thread serveur uniquement.
 */
public final class ProgressManager {
    private static final Map<UUID, PlayerProgress> CACHE = new ConcurrentHashMap<>();

    private ProgressManager() {
    }

    // ------------------------------------------------------------------ fichiers

    private static Path file(UUID id) {
        Path root = ModStorage.root();
        return root == null ? null : root.resolve("progress").resolve(id + ".properties");
    }

    public static PlayerProgress get(ServerPlayer player) {
        return CACHE.computeIfAbsent(player.getUUID(), ProgressManager::load);
    }

    private static PlayerProgress load(UUID id) {
        Path f = file(id);
        if (f == null || !Files.isRegularFile(f)) {
            return new PlayerProgress();
        }
        try {
            String text = Files.readString(f, StandardCharsets.UTF_8);
            List<String> warnings = new ArrayList<>();
            PlayerProgress p = PlayerProgress.parse(text, warnings);
            if (!warnings.isEmpty()) {
                // Données partiellement invalides : copie de l'original avant la prochaine écriture.
                Files.copy(f, f.resolveSibling(id + ".properties.corrompu"), StandardCopyOption.REPLACE_EXISTING);
                UltimateAutoBuilderMod.LOGGER.warn("Progression de {} partiellement invalide : {}", id, warnings);
            }
            return p;
        } catch (IOException | RuntimeException e) {
            UltimateAutoBuilderMod.LOGGER.error("Progression de {} illisible, conservée à part", id, e);
            try {
                Files.copy(f, f.resolveSibling(id + ".properties.corrompu"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // La copie de secours est facultative.
            }
            return new PlayerProgress();
        }
    }

    /** Écriture atomique (fichier temporaire puis remplacement). */
    private static void save(UUID id, PlayerProgress p) {
        Path f = file(id);
        if (f == null) {
            return;
        }
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(id + ".properties.tmp");
            Files.writeString(tmp, p.serialize(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.error("Sauvegarde de la progression de {} impossible", id, e);
        }
    }

    /** Arrêt du serveur ou changement de monde : rien ne doit fuiter vers le monde suivant. */
    public static void clear() {
        CACHE.clear();
    }

    public static void forget(UUID id) {
        CACHE.remove(id);
    }

    // ------------------------------------------------------------------ règles

    /** La progression s'applique-t-elle à ce joueur ? */
    public static boolean enforced(ServerPlayer player) {
        ServerConfig.ProgressionMode mode = ServerConfig.loaded() ? ServerConfig.PROGRESSION_MODE.get() : ServerConfig.ProgressionMode.CREATIVE_FREE;
        return switch (mode) {
            case OFF -> false;
            case ALWAYS -> true;
            case CREATIVE_FREE -> !player.isCreative();
        };
    }

    public static boolean canBuild(ServerPlayer player, ModelEntry entry) {
        return !enforced(player) || get(player).isUnlocked(entry.id(), ProgressionRules.rule(entry));
    }

    /** Message expliquant pourquoi une construction est verrouillée. */
    public static String lockReason(ServerPlayer player, ModelEntry entry) {
        UnlockRule r = ProgressionRules.rule(entry);
        PlayerProgress p = get(player);
        if (!p.tier().atLeast(r.tier())) {
            return "« " + entry.name() + " » est verrouillée : niveau « " + r.tier().label + " » requis (quêtes du cristal)";
        }
        return "« " + entry.name() + " » est verrouillée : débloquez-la d'abord (" + ProgressionRules.describe(r.cost()) + ")";
    }

    // ------------------------------------------------------------------ faits de jeu

    /** Statistiques de minage et progrès vanilla : données du serveur, impossibles à falsifier par le client. */
    static PlayerProgress.Facts facts(ServerPlayer player) {
        return new PlayerProgress.Facts() {
            @Override
            public int mined(List<String> blocks) {
                int n = 0;
                for (String id : blocks) {
                    ResourceLocation rl = ResourceLocation.tryParse(id);
                    if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) {
                        Block b = BuiltInRegistries.BLOCK.get(rl);
                        n += player.getStats().getValue(Stats.BLOCK_MINED.get(b));
                    }
                }
                return n;
            }

            @Override
            public boolean advancement(String id) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null) {
                    return false;
                }
                AdvancementHolder adv = player.server.getAdvancements().get(rl);
                return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
            }
        };
    }

    /** Inventaire réel du joueur (inventaire principal, armure exclue). */
    static MaterialInventory inventory(ServerPlayer player) {
        return new MaterialInventory() {
            private Item item(String id) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                return rl != null && BuiltInRegistries.ITEM.containsKey(rl) ? BuiltInRegistries.ITEM.get(rl) : null;
            }

            @Override
            public int count(String id) {
                Item it = item(id);
                if (it == null) {
                    return 0;
                }
                int n = 0;
                for (ItemStack s : player.getInventory().items) {
                    if (s.is(it)) {
                        n += s.getCount();
                    }
                }
                for (ItemStack s : player.getInventory().offhand) {
                    if (s.is(it)) {
                        n += s.getCount();
                    }
                }
                return n;
            }

            @Override
            public void remove(String id, int amount) {
                Item it = item(id);
                if (it == null) {
                    throw new IllegalStateException("Objet inconnu : " + id);
                }
                int removed = player.getInventory().clearOrCountMatchingItems(s -> s.is(it), amount, player.inventoryMenu.getCraftSlots());
                if (removed != amount) {
                    throw new IllegalStateException("Retrait incomplet de " + id + " : " + removed + " / " + amount);
                }
            }
        };
    }

    // ------------------------------------------------------------------ actions

    public static PlayerProgress.UnlockResult unlock(ServerPlayer player, String modelId) {
        ModelEntry e = DefaultCatalog.shared().get(modelId);
        if (e == null) {
            return new PlayerProgress.UnlockResult(PlayerProgress.UnlockStatus.FREE, List.of(), "Modèle inconnu");
        }
        PlayerProgress p = get(player);
        PlayerProgress.UnlockResult r = p.unlock(e.id(), ProgressionRules.rule(e), inventory(player));
        if (r.status() == PlayerProgress.UnlockStatus.OK) {
            player.getInventory().setChanged();
            save(player.getUUID(), p);
        }
        return r;
    }

    public static PlayerProgress.ClaimResult claim(ServerPlayer player, String questId) {
        PlayerProgress p = get(player);
        PlayerProgress.ClaimResult r = p.claim(questId, facts(player));
        if (r.status() == PlayerProgress.ClaimStatus.OK) {
            save(player.getUUID(), p);
        }
        return r;
    }

    public static void markOpened(ServerPlayer player, boolean catalog) {
        PlayerProgress p = get(player);
        if (catalog ? p.markCatalogOpened() : p.markCrystalOpened()) {
            save(player.getUUID(), p);
        }
    }

    /** Construction terminée par un joueur (connecté ou non). */
    public static void onBuildCompleted(UUID owner, ModelEntry entry, ServerPlayer online) {
        PlayerProgress p = online != null ? get(online) : CACHE.computeIfAbsent(owner, ProgressManager::load);
        p.recordBuild(entry.category().key());
        save(owner, p);
        if (online != null) {
            send(online);
        }
    }

    /** État envoyé au client : niveau, constructions débloquées, quêtes avec leur avancement réel. */
    public static void send(ServerPlayer player) {
        PlayerProgress p = get(player);
        PlayerProgress.Facts f = facts(player);
        List<ProgressPayload.QuestInfo> quests = new ArrayList<>();
        for (Quest q : QuestBook.all()) {
            quests.add(new ProgressPayload.QuestInfo(q.id(), p.state(q, f).ordinal(), Math.min(p.value(q, f), 1_000_000), q.required()));
        }
        PacketDistributor.sendToPlayer(player, new ProgressPayload(p.tier().ordinal(), p.title(), enforced(player), List.copyOf(p.unlocked()),
                quests, p.builds()));
    }
}
