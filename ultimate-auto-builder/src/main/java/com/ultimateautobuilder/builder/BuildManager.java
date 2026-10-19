package com.ultimateautobuilder.builder;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.builder.undo.UndoRecord;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.network.JobProgressPayload;
import com.ultimateautobuilder.storage.HistoryEntry;
import com.ultimateautobuilder.storage.ModStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ordonnanceur des constructions. Chaque tick serveur, il répartit un budget de blocs et de temps
 * entre les tâches actives ; le budget se réduit automatiquement quand le tick serveur est déjà
 * long, et remonte progressivement quand le serveur est fluide.
 */
public final class BuildManager {
    private static volatile BuildManager instance;

    private final MinecraftServer server;
    private final List<Job> jobs = new ArrayList<>();
    private final ExecutorService generator;
    private final ChunkTickets chunks = new ChunkTickets();
    private final BuildHistory history;
    private final Storage storage = new Storage();
    private final Map<UUID, Long> lastRequestTick = new HashMap<>();
    private long tickStartNanos;
    private long tickCounter;
    private double throttle = 1.0;
    private double avgTickMs = 20;
    private long lastCheckpoint = System.nanoTime();

    private BuildManager(MinecraftServer server) {
        this.server = server;
        ModStorage.open(server);
        this.history = new BuildHistory(ModStorage.loadHistory());
        int threads = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2));
        this.generator = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "UAB-Generator");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    public static BuildManager get() {
        return instance;
    }

    // ------------------------------------------------------------------ cycle de vie

    public static void onServerStarted(MinecraftServer server) {
        BuildManager m = new BuildManager(server);
        instance = m;
        m.resumeCheckpoints();
    }

    public static void onServerStopping(MinecraftServer server) {
        BuildManager m = instance;
        if (m == null) {
            return;
        }
        for (Job j : m.jobs) {
            CompoundTag cp = j.checkpoint();
            if (cp != null) {
                ModStorage.writeNow(ModStorage.jobFile(j.id()), cp);
            }
            ServerLevel level = server.getLevel(j.dimension());
            m.chunks.release(level, j.id());
        }
        m.jobs.clear();
        m.history.saveNow();
        m.generator.shutdownNow();
        ModStorage.close();
        instance = null;
    }

    public static void onServerTickPre(MinecraftServer server) {
        BuildManager m = instance;
        if (m != null) {
            m.tickStartNanos = System.nanoTime();
        }
    }

    public static void onServerTickPost(MinecraftServer server) {
        BuildManager m = instance;
        if (m != null) {
            m.tick();
        }
    }

    // ------------------------------------------------------------------ accès

    public ExecutorService generator() {
        return generator;
    }

    public ChunkTickets chunks() {
        return chunks;
    }

    public BuildHistory history() {
        return history;
    }

    public Storage storage() {
        return storage;
    }

    public MinecraftServer server() {
        return server;
    }

    public List<Job> jobs() {
        return List.copyOf(jobs);
    }

    public Job job(int id) {
        for (Job j : jobs) {
            if (j.id() == id) {
                return j;
            }
        }
        return null;
    }

    public double throttle() {
        return throttle;
    }

    // ------------------------------------------------------------------ demandes

    /**
     * Lance une construction après toutes les vérifications de sécurité.
     *
     * @param player joueur demandeur, ou null pour une tâche système (tests, console)
     * @return le travail créé
     * @throws IllegalStateException message lisible si la demande est refusée
     */
    public PlaceJob submit(ServerPlayer player, ServerLevel level, BuildRequest request) {
        ModelEntry entry = DefaultCatalog.shared().get(request.modelId());
        if (entry == null) {
            throw new IllegalStateException("Modèle inconnu : " + request.modelId());
        }
        UUID owner = player == null ? null : player.getUUID();
        if (player != null && !com.ultimateautobuilder.progression.ProgressManager.canBuild(player, entry)) {
            throw new IllegalStateException(com.ultimateautobuilder.progression.ProgressManager.lockReason(player, entry));
        }
        if (player != null) {
            long cooldown = ServerConfig.loaded() ? ServerConfig.REQUEST_COOLDOWN_TICKS.get() : 20;
            Long last = lastRequestTick.get(owner);
            if (last != null && tickCounter - last < cooldown) {
                throw new IllegalStateException("Veuillez patienter avant de lancer une nouvelle construction");
            }
            int perPlayer = ServerConfig.loaded() ? ServerConfig.MAX_ACTIVE_JOBS_PER_PLAYER.get() : 1;
            long mine = jobs.stream().filter(j -> owner.equals(j.owner())).count();
            if (mine >= perPlayer) {
                throw new IllegalStateException("Vous avez déjà " + mine + " construction(s) en cours (limite : " + perPlayer + ")");
            }
            int maxDist = ServerConfig.loaded() ? ServerConfig.MAX_BUILD_DISTANCE.get() : 512;
            BlockPos o = request.origin();
            double dx = o.getX() - player.getX(), dz = o.getZ() - player.getZ();
            if (dx * dx + dz * dz > (double) maxDist * maxDist) {
                throw new IllegalStateException("Point de construction trop éloigné (maximum " + maxDist + " blocs)");
            }
        }
        int total = ServerConfig.loaded() ? ServerConfig.MAX_ACTIVE_JOBS.get() : 3;
        if (jobs.size() >= total) {
            throw new IllegalStateException("Trop de constructions simultanées sur le serveur (limite : " + total + ")");
        }
        if (!level.getWorldBorder().isWithinBounds(request.origin())) {
            throw new IllegalStateException("Le point de construction est hors de la bordure du monde");
        }
        int size = entry.sizes().resolve(request.preset(), request.customSize());
        if (size <= 0) {
            throw new IllegalStateException("Taille invalide");
        }
        BuildRequest effective = request;
        if (request.mode() == BuildMode.AERIAL) {
            int lift = ServerConfig.loaded() ? ServerConfig.AERIAL_HEIGHT.get() : 20;
            effective = request.withOrigin(request.origin().above(lift));
        }
        int id = history.allocateId();
        String ownerName = player == null ? "Serveur" : player.getGameProfile().getName();
        PlaceJob job = new PlaceJob(id, owner, ownerName, effective, entry);
        history.add(new HistoryEntry(id, entry.id(), entry.name(), level.dimension().location().toString(), new int[6], effective.rotation(),
                effective.mirror(), owner, ownerName, System.currentTimeMillis(), 0, HistoryEntry.Status.RUNNING, false));
        job.startGeneration(this);
        jobs.add(job);
        if (player != null) {
            lastRequestTick.put(owner, tickCounter);
        }
        return job;
    }

    /** Annule la dernière tâche du joueur (ou une tâche précise). */
    public Job cancel(ServerPlayer player, int jobId, boolean admin) {
        for (int i = jobs.size() - 1; i >= 0; i--) {
            Job j = jobs.get(i);
            boolean mine = player == null || player.getUUID().equals(j.owner());
            if ((jobId < 0 || j.id() == jobId) && (mine || admin)) {
                j.cancel();
                return j;
            }
        }
        return null;
    }

    /** Met en pause (ou reprend) une tâche du joueur ; les administrateurs agissent sur toutes. */
    public Job setPaused(ServerPlayer player, int jobId, boolean admin, boolean pause) {
        for (int i = jobs.size() - 1; i >= 0; i--) {
            Job j = jobs.get(i);
            boolean mine = player == null || player.getUUID().equals(j.owner());
            if ((jobId < 0 || j.id() == jobId) && (mine || admin) && !j.finished()) {
                if (pause) {
                    j.pause("demandée par " + (player == null ? "le serveur" : player.getGameProfile().getName()));
                    j.pauseAnnounced = true;
                } else {
                    j.resume();
                }
                return j;
            }
        }
        return null;
    }

    /** Lance le retour arrière d'une construction terminée. */
    public RestoreJob undo(ServerPlayer player, ServerLevel fallbackLevel, int historyId, boolean admin) {
        HistoryEntry e = historyId < 0 ? history.lastUndoable(admin ? null : (player == null ? null : player.getUUID())) : history.get(historyId);
        if (e == null || !e.undoAvailable) {
            throw new IllegalStateException("Aucune construction annulable");
        }
        if (player != null && !admin && !player.getUUID().equals(e.owner)) {
            throw new IllegalStateException("Vous ne pouvez annuler que vos propres constructions");
        }
        for (Job j : jobs) {
            if (j instanceof RestoreJob r && r.targetId() == e.id) {
                throw new IllegalStateException("Cette annulation est déjà en cours");
            }
            if (j.id() == e.id) {
                throw new IllegalStateException("Construction encore en cours : annulez-la d'abord");
            }
        }
        CompoundTag tag = ModStorage.read(ModStorage.undoFile(e.id));
        if (tag == null) {
            e.undoAvailable = false;
            throw new IllegalStateException("Journal d'annulation introuvable");
        }
        ResourceKey<net.minecraft.world.level.Level> dim = ResourceKey.create(Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(e.dimension));
        ServerLevel level = server.getLevel(dim);
        if (level == null) {
            level = fallbackLevel;
        }
        UndoRecord record = UndoRecord.load(tag, level.holderLookup(Registries.BLOCK));
        boolean prudent = !ServerConfig.loaded() || ServerConfig.UNDO_PRUDENT.get();
        RestoreJob job = new RestoreJob(history.allocateId(), player == null ? null : player.getUUID(),
                player == null ? "Serveur" : player.getGameProfile().getName(), level.dimension(), e.modelName, e.id, record, prudent);
        jobs.add(job);
        return job;
    }

    // ------------------------------------------------------------------ boucle principale

    private void tick() {
        tickCounter++;
        long now = System.nanoTime();
        double spentMs = (now - tickStartNanos) / 1_000_000.0;
        if (jobs.isEmpty()) {
            avgTickMs = avgTickMs * 0.9 + spentMs * 0.1;
            history.saveIfDirty();
            return;
        }
        int targetMspt = ServerConfig.loaded() ? ServerConfig.TARGET_MSPT.get() : 40;
        boolean adaptive = !ServerConfig.loaded() || ServerConfig.ADAPTIVE_THROTTLE.get();
        if (adaptive) {
            if (avgTickMs > targetMspt) {
                throttle = Math.max(0.05, throttle * 0.8);
            } else if (avgTickMs < targetMspt * 0.7) {
                throttle = Math.min(1.0, throttle * 1.08 + 0.01);
            }
        } else {
            throttle = 1.0;
        }
        int budgetMs = ServerConfig.loaded() ? ServerConfig.TICK_BUDGET_MS.get() : 15;
        // Le temps déjà consommé par le tick vanilla réduit d'autant le temps disponible.
        double available = Math.max(1.0, Math.min(budgetMs, targetMspt - spentMs)) * (adaptive ? Math.max(0.25, throttle) : 1.0);
        long deadline = now + (long) (available * 1_000_000);
        int blockBudget = Math.max(16, (int) (ServerConfig.blocksPerTick() * throttle));

        List<Job> active = new ArrayList<>();
        for (Job j : jobs) {
            if (!j.paused() || j.cancelPending()) {
                active.add(j);
            }
        }
        int share = Math.max(8, blockBudget / Math.max(1, active.size()));
        for (Job j : active) {
            ServerLevel level = server.getLevel(j.dimension());
            if (level == null) {
                j.cancel();
            }
            try {
                if (level != null) {
                    j.tick(level, this, share, deadline);
                }
            } catch (RuntimeException e) {
                UltimateAutoBuilderMod.LOGGER.error("Erreur pendant la construction {}", j.id(), e);
                j.cancel();
                j.message = "Erreur interne : " + e.getMessage();
                j.state = Job.State.FAILED;
            }
        }
        // Tâches terminées : nettoyage, historique, notification.
        Iterator<Job> it = jobs.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            if (!j.finished()) {
                continue;
            }
            it.remove();
            ServerLevel level = server.getLevel(j.dimension());
            try {
                j.finish(level, this);
            } catch (RuntimeException e) {
                UltimateAutoBuilderMod.LOGGER.error("Erreur de finalisation de la tâche {}", j.id(), e);
            }
            if (j instanceof PlaceJob pj && j.state() == Job.State.DONE && j.owner() != null) {
                // Quêtes « construire » : seules les constructions menées à terme comptent.
                com.ultimateautobuilder.progression.ProgressManager.onBuildCompleted(j.owner(), pj.entry(),
                        server.getPlayerList().getPlayer(j.owner()));
            }
            notifyOwner(j, true);
        }
        if (tickCounter % 10 == 0) {
            for (Job j : jobs) {
                notifyOwner(j, false);
            }
        }
        long cpInterval = (ServerConfig.loaded() ? ServerConfig.CHECKPOINT_SECONDS.get() : 30) * 1_000_000_000L;
        if (System.nanoTime() - lastCheckpoint > cpInterval) {
            lastCheckpoint = System.nanoTime();
            for (Job j : jobs) {
                CompoundTag cp = j.checkpoint();
                if (cp != null) {
                    ModStorage.writeAsync(ModStorage.jobFile(j.id()), cp);
                }
            }
        }
        history.saveIfDirty();
        double total = (System.nanoTime() - tickStartNanos) / 1_000_000.0;
        avgTickMs = avgTickMs * 0.85 + total * 0.15;
    }

    private void notifyOwner(Job j, boolean finalMessage) {
        if (j.owner() == null) {
            return;
        }
        ServerPlayer p = server.getPlayerList().getPlayer(j.owner());
        if (p == null) {
            return;
        }
        String status = finalMessage ? j.message() : j.paused() ? "En pause : " + j.pauseReason() : "";
        PacketDistributor.sendToPlayer(p, new JobProgressPayload(j.id(), j.title(), j.progress(), j.total(), j.state().ordinal(), status));
        if (!finalMessage && j.paused() && !j.pauseAnnounced) {
            j.pauseAnnounced = true;
            p.sendSystemMessage(Component.literal("[Ultimate Auto Builder] " + j.title() + " en pause : " + j.pauseReason()
                    + " — /uab resume " + j.id() + " ou bouton « Reprendre » de l'historique").withStyle(ChatFormatting.AQUA));
        }
        if (finalMessage && !j.message().isEmpty()) {
            ChatFormatting color = j.state() == Job.State.DONE ? ChatFormatting.GREEN : j.state() == Job.State.CANCELLED
                    ? ChatFormatting.YELLOW : ChatFormatting.RED;
            p.sendSystemMessage(Component.literal("[Ultimate Auto Builder] " + j.message()).withStyle(color));
        }
    }

    // ------------------------------------------------------------------ reprise après redémarrage

    private void resumeCheckpoints() {
        for (Path file : ModStorage.listJobs()) {
            CompoundTag t = ModStorage.read(file);
            if (t == null || !"place".equals(t.getString("kind"))) {
                continue;
            }
            try {
                BuildRequest req = BuildRequest.load(t.getCompound("request"));
                ModelEntry entry = DefaultCatalog.shared().get(req.modelId());
                if (entry == null) {
                    UltimateAutoBuilderMod.LOGGER.warn("Reprise impossible : modèle {} introuvable", req.modelId());
                    continue;
                }
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, req.dimension()));
                if (level == null) {
                    continue;
                }
                UUID owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
                PlaceJob job = new PlaceJob(t.getInt("id"), owner, t.getString("ownerName"), req, entry);
                UndoRecord undo = UndoRecord.load(t.getCompound("undo"), level.holderLookup(Registries.BLOCK));
                job.resumeFrom(t, undo);
                job.startGeneration(this);
                jobs.add(job);
                UltimateAutoBuilderMod.LOGGER.info("Reprise de la construction {} ({}) à l'étape {}", job.id(), entry.name(), t.getInt("cursor"));
            } catch (RuntimeException e) {
                UltimateAutoBuilderMod.LOGGER.error("Point de reprise invalide ignoré : {}", file, e);
            }
        }
    }

    /** Façade de persistance liée aux tâches. */
    public static final class Storage {
        public void writeUndo(int id, CompoundTag tag) {
            ModStorage.writeAsync(ModStorage.undoFile(id), tag);
        }

        public void deleteUndo(int id) {
            ModStorage.deleteAsync(ModStorage.undoFile(id));
        }

        public void deleteCheckpoint(int id) {
            ModStorage.deleteAsync(ModStorage.jobFile(id));
        }
    }
}
