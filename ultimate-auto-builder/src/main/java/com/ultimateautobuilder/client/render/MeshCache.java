package com.ultimateautobuilder.client.render;

import com.ultimateautobuilder.UltimateAutoBuilderMod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Cache LRU de maillages texturés : construction sur un thread d'arrière-plan (une à la fois,
 * priorité basse), téléversement sur le thread de rendu, libération des maillages évincés.
 */
public final class MeshCache {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UAB-Meshes");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private record Ready(String key, BlockMesh.Built built) {
    }

    private final int capacity;
    private final Map<String, BlockMesh> meshes = new LinkedHashMap<>(32, 0.75f, true);
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final Set<String> failed = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Ready> ready = new ConcurrentLinkedQueue<>();

    public MeshCache(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    /**
     * Maillage prêt, ou null pendant sa construction (l'appelant affiche un repli).
     * Thread de rendu uniquement.
     */
    public BlockMesh get(String key, Supplier<BlockMesh.Built> builder) {
        drain();
        BlockMesh m = meshes.get(key);
        if (m != null || failed.contains(key)) {
            return m;
        }
        if (pending.add(key)) {
            WORKER.execute(() -> {
                try {
                    BlockMesh.Built b = builder.get();
                    if (b == null) {
                        failed.add(key);
                        pending.remove(key);
                    } else {
                        ready.add(new Ready(key, b));
                    }
                } catch (RuntimeException | LinkageError e) {
                    // Échec (modèle de bloc d'un autre mod, etc.) : l'appelant garde son repli.
                    UltimateAutoBuilderMod.LOGGER.warn("Aperçu 3D impossible pour {} : {}", key, e.toString());
                    failed.add(key);
                    pending.remove(key);
                }
            });
        }
        return null;
    }

    public boolean isPending(String key) {
        return pending.contains(key);
    }

    private void drain() {
        Ready r;
        while ((r = ready.poll()) != null) {
            pending.remove(r.key());
            try {
                BlockMesh old = meshes.put(r.key(), BlockMesh.upload(r.built()));
                if (old != null) {
                    old.close();
                }
            } catch (RuntimeException e) {
                UltimateAutoBuilderMod.LOGGER.warn("Téléversement de l'aperçu 3D impossible : {}", e.toString());
                failed.add(r.key());
            }
        }
        while (meshes.size() > capacity) {
            Iterator<Map.Entry<String, BlockMesh>> it = meshes.entrySet().iterator();
            it.next().getValue().close();
            it.remove();
        }
    }

    /** Supprime les maillages dont la clé commence par le préfixe donné. */
    public void invalidate(String prefix) {
        drain();
        Iterator<Map.Entry<String, BlockMesh>> it = meshes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BlockMesh> e = it.next();
            if (e.getKey().startsWith(prefix)) {
                e.getValue().close();
                it.remove();
            }
        }
        failed.removeIf(k -> k.startsWith(prefix));
    }

    public void clear() {
        drain();
        for (BlockMesh m : meshes.values()) {
            m.close();
        }
        meshes.clear();
        failed.clear();
    }
}
