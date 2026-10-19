package com.ultimateautobuilder.client;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.model.VoxelModel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Génération côté client (dimensions, nombre de blocs, aperçu fantôme). Les générateurs étant
 * déterministes, le client obtient exactement le même modèle que le serveur. Cache LRU de 4 modèles.
 */
public final class ClientModels {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UAB-ClientModels");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, CompletableFuture<VoxelModel>> CACHE = new LinkedHashMap<>(8, 0.75f, true);

    private ClientModels() {
    }

    public static synchronized CompletableFuture<VoxelModel> request(ModelEntry entry, int size, boolean hollow) {
        String key = entry.id() + "|" + size + "|" + hollow;
        CompletableFuture<VoxelModel> f = CACHE.get(key);
        if (f != null) {
            return f;
        }
        f = CompletableFuture.supplyAsync(() -> {
            if (entry.id().startsWith("custom:")) {
                VoxelModel preview = ClientState.customPreview(entry.id());
                if (preview != null && net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) {
                    return com.ultimateautobuilder.core.model.ModelScaler.toHeight(preview, size);
                }
            }
            return entry.generate(size, hollow);
        }, WORKER).exceptionally(e -> {
            UltimateAutoBuilderMod.LOGGER.warn("Génération client impossible pour {}", entry.id(), e);
            return VoxelModel.EMPTY;
        });
        CACHE.put(key, f);
        while (CACHE.size() > 4) {
            var it = CACHE.entrySet().iterator();
            it.next();
            it.remove();
        }
        return f;
    }

    /** Modèle disponible immédiatement, sinon null (calcul en cours). */
    public static VoxelModel now(ModelEntry entry, int size, boolean hollow) {
        CompletableFuture<VoxelModel> f = request(entry, size, hollow);
        return f.isDone() ? f.getNow(null) : null;
    }
}
