package com.ultimateautobuilder.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.render.IsoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Miniatures isométriques générées à la demande (seules les cartes visibles en demandent),
 * calculées sur un thread d'arrière-plan puis téléversées en texture sur le thread de rendu.
 * Cache LRU borné : les textures évincées sont libérées.
 */
public final class ThumbnailCache {
    public static final int SIZE = 64;
    private static final int MAX_TEXTURES = 192;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UAB-Thumbnails");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private record Pending(String key, int[] argb) {
    }

    private static final Map<String, ResourceLocation> TEXTURES = new LinkedHashMap<>(64, 0.75f, true);
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> CUSTOM_REQUESTED = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Pending> READY = new ConcurrentLinkedQueue<>();
    private static int counter;

    private ThumbnailCache() {
    }

    /** Texture de la miniature, ou null si elle est en cours de calcul. Thread de rendu uniquement. */
    public static ResourceLocation get(ModelEntry entry, int view) {
        uploadReady();
        String key = entry.id() + "#" + view;
        ResourceLocation tex = TEXTURES.get(key);
        if (tex != null) {
            return tex;
        }
        if (REQUESTED.add(key)) {
            WORKER.execute(() -> {
                try {
                    VoxelModel m = modelFor(entry);
                    if (m == null) {
                        REQUESTED.remove(key);
                        return;
                    }
                    IsoRenderer.Image img = IsoRenderer.fit(IsoRenderer.render(m, view, 120_000), SIZE);
                    READY.add(new Pending(key, img.argb()));
                } catch (RuntimeException e) {
                    UltimateAutoBuilderMod.LOGGER.debug("Miniature impossible pour {}", entry.id(), e);
                }
            });
        }
        return null;
    }

    private static VoxelModel modelFor(ModelEntry entry) {
        if (entry.id().startsWith("custom:")) {
            VoxelModel preview = ClientState.customPreview(entry.id());
            if (preview != null) {
                return preview;
            }
            if (Minecraft.getInstance().getSingleplayerServer() == null) {
                // Multijoueur : le modèle est sur le serveur, on demande une seule fois un aperçu réduit.
                if (CUSTOM_REQUESTED.add(entry.id())) {
                    Minecraft.getInstance().execute(() -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.ultimateautobuilder.network.ActionPayload(
                        com.ultimateautobuilder.network.ActionPayload.REQUEST_CUSTOM_PREVIEW, 0, entry.id())));
                }
                return null;
            }
        }
        // Taille « Petit » plafonnée : suffisante pour une vignette et rapide à générer.
        int size = Math.min(entry.sizes().resolve(SizePreset.NORMAL, 0), Math.max(entry.sizes().small(), 40));
        return entry.generate(size, false);
    }

    private static void uploadReady() {
        Pending p;
        while ((p = READY.poll()) != null) {
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int argb = p.argb()[y * SIZE + x];
                    // NativeImage attend de l'ABGR.
                    int abgr = (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
                    img.setPixelRGBA(x, y, abgr);
                }
            }
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation loc = UltimateAutoBuilderMod.id("thumb/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(loc, tex);
            TEXTURES.put(p.key(), loc);
            evict();
        }
    }

    private static void evict() {
        while (TEXTURES.size() > MAX_TEXTURES) {
            var it = TEXTURES.entrySet().iterator();
            var e = it.next();
            it.remove();
            REQUESTED.remove(e.getKey());
            Minecraft.getInstance().getTextureManager().release(e.getValue());
        }
    }

    public static void invalidate(String modelId) {
        var it = TEXTURES.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getKey().startsWith(modelId + "#")) {
                it.remove();
                Minecraft.getInstance().getTextureManager().release(e.getValue());
            }
        }
        REQUESTED.removeIf(k -> k.startsWith(modelId + "#"));
        CUSTOM_REQUESTED.remove(modelId);
    }
}
