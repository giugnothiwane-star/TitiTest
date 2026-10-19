package com.ultimateautobuilder.client;

import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.validation.Limits;
import com.ultimateautobuilder.network.JobProgressPayload;
import com.ultimateautobuilder.network.StatePayload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** État reçu du serveur (droits, réglages, tâches, historique) et progression des constructions. */
public final class ClientState {
    public record Progress(int jobId, String title, int progress, int total, int state, String message, long receivedAt) {
        public boolean finished() {
            return state >= 4;
        }
    }

    private static volatile StatePayload state;
    private static final Map<Integer, Progress> PROGRESS = new LinkedHashMap<>();
    private static final Map<String, VoxelModel> CUSTOM_PREVIEWS = new ConcurrentHashMap<>();
    private static long stateVersion;

    private ClientState() {
    }

    public static StatePayload state() {
        return state;
    }

    public static long version() {
        return stateVersion;
    }

    static void setState(StatePayload s) {
        state = s;
        stateVersion++;
    }

    static synchronized void onProgress(JobProgressPayload p) {
        PROGRESS.put(p.jobId(), new Progress(p.jobId(), p.title(), p.progress(), p.total(), p.state(), p.message(), System.currentTimeMillis()));
    }

    /** Progressions visibles : en cours, ou terminées depuis moins de 6 secondes. */
    public static synchronized java.util.List<Progress> progress() {
        long now = System.currentTimeMillis();
        PROGRESS.values().removeIf(p -> p.finished() && now - p.receivedAt() > 6000);
        return new java.util.ArrayList<>(PROGRESS.values());
    }

    static void putCustomPreview(String id, byte[] data) {
        try {
            CUSTOM_PREVIEWS.put(id, ModelCodec.decode(data, new Limits(400_000, 512, 400_000, 4096)).model());
        } catch (ImportException e) {
            CUSTOM_PREVIEWS.remove(id);
        }
    }

    public static VoxelModel customPreview(String id) {
        return CUSTOM_PREVIEWS.get(id);
    }

    public static synchronized void reset() {
        state = null;
        PROGRESS.clear();
        CUSTOM_PREVIEWS.clear();
        stateVersion++;
    }
}
