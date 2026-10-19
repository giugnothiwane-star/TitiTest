package com.ultimateautobuilder.client.image;

import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.network.ModelUploadPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Envoi d'un modèle généré au serveur par morceaux, étalé sur plusieurs ticks (quelques morceaux
 * par tick) pour ne pas saturer la connexion. Le serveur revalide tout à la réception.
 */
public final class ModelSender {
    private static final int CHUNKS_PER_TICK = 4;
    private static final Deque<ModelUploadPayload> QUEUE = new ArrayDeque<>();
    private static int uploadCounter = 1;
    private static int sent, total;

    private ModelSender() {
    }

    /** @return nombre de morceaux à envoyer */
    public static synchronized int send(String name, VoxelModel model) throws IOException {
        byte[] data = ModelCodec.encode(name, model);
        int n = (data.length + ModelUploadPayload.CHUNK_BYTES - 1) / ModelUploadPayload.CHUNK_BYTES;
        if (n > ModelUploadPayload.MAX_CHUNKS) {
            throw new IOException("Modèle trop volumineux pour être envoyé (" + data.length / 1024 + " Ko)");
        }
        QUEUE.clear();
        int id = uploadCounter++;
        for (int i = 0; i < n; i++) {
            int from = i * ModelUploadPayload.CHUNK_BYTES;
            QUEUE.add(new ModelUploadPayload(id, i, n, name, Arrays.copyOfRange(data, from, Math.min(data.length, from + ModelUploadPayload.CHUNK_BYTES))));
        }
        sent = 0;
        total = n;
        return n;
    }

    /** Appelé à chaque tick client. */
    public static synchronized void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        if (net.minecraft.client.Minecraft.getInstance().getConnection() == null) {
            QUEUE.clear();
            return;
        }
        for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
            PacketDistributor.sendToServer(QUEUE.poll());
            sent++;
        }
    }

    public static synchronized boolean busy() {
        return !QUEUE.isEmpty();
    }

    public static synchronized String progress() {
        return total == 0 ? "" : sent + "/" + total;
    }
}
