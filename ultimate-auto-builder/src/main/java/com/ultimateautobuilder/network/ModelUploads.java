package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.permission.Permissions;
import com.ultimateautobuilder.storage.CustomModelStore;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Réassemblage serveur des modèles envoyés par morceaux. Un seul envoi en cours par joueur,
 * taille totale bornée par la configuration d'import, délai d'expiration, permission vérifiée au
 * premier et au dernier morceau, puis décodage complet avec les limites du serveur.
 */
public final class ModelUploads {
    private static final long TIMEOUT_NANOS = 60_000_000_000L;

    private static final class Pending {
        final int uploadId, total;
        final String name;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        int next;
        long lastNanos = System.nanoTime();

        Pending(int uploadId, int total, String name) {
            this.uploadId = uploadId;
            this.total = total;
            this.name = name;
        }
    }

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private ModelUploads() {
    }

    static void onChunk(ModelUploadPayload p, ServerPlayer player) {
        UUID id = player.getUUID();
        try {
            long maxBytes = (ServerConfig.loaded() ? ServerConfig.MAX_IMPORT_KB.get() : 8192) * 1024L;
            if (p.index() == 0) {
                if (!Permissions.canImport(player)) {
                    throw new IllegalStateException("Vous n'avez pas la permission d'ajouter des modèles sur ce serveur");
                }
                if (p.total() <= 0 || p.total() > ModelUploadPayload.MAX_CHUNKS || (long) p.total() * ModelUploadPayload.CHUNK_BYTES > maxBytes
                        + ModelUploadPayload.CHUNK_BYTES) {
                    throw new IllegalStateException("Modèle trop volumineux pour ce serveur (maximum " + maxBytes / 1024 + " Ko)");
                }
                PENDING.put(id, new Pending(p.uploadId(), p.total(), p.name()));
            }
            Pending pending = PENDING.get(id);
            if (pending == null || pending.uploadId != p.uploadId() || pending.next != p.index() || pending.total != p.total()) {
                PENDING.remove(id);
                throw new IllegalStateException("Envoi interrompu ou désordonné : recommencez");
            }
            if (System.nanoTime() - pending.lastNanos > TIMEOUT_NANOS) {
                PENDING.remove(id);
                throw new IllegalStateException("Envoi expiré : recommencez");
            }
            pending.data.write(p.chunk());
            pending.next++;
            pending.lastNanos = System.nanoTime();
            if (pending.data.size() > maxBytes) {
                PENDING.remove(id);
                throw new IllegalStateException("Modèle trop volumineux pour ce serveur");
            }
            if (pending.next < pending.total) {
                return;
            }
            PENDING.remove(id);
            if (!Permissions.canImport(player)) {
                throw new IllegalStateException("Permission refusée");
            }
            ModelCodec.Named named = ModelCodec.decode(pending.data.toByteArray(), ServerConfig.limits());
            // decode() a déjà contrôlé dimensions, palette (syntaxe des états), nombre et ordre des blocs.
            VoxelModel model = named.model();
            if (model.isEmpty()) {
                throw new IllegalStateException("Modèle vide");
            }
            String name = pending.name.isBlank() ? named.name() : pending.name;
            var entry = CustomModelStore.save(name, model);
            ServerPayloadHandler.info(player, "Modèle reçu : « " + entry.name() + " » (" + model.size() + " blocs). Il est disponible dans la "
                    + "catégorie « Personnalisés » du menu du cristal.");
            ServerPayloadHandler.sendState(player);
        } catch (IllegalStateException | ImportException e) {
            ServerPayloadHandler.error(player, e.getMessage());
        } catch (IOException e) {
            ServerPayloadHandler.error(player, "Enregistrement impossible : " + e.getMessage());
        } catch (RuntimeException e) {
            PENDING.remove(id);
            UltimateAutoBuilderMod.LOGGER.error("Envoi de modèle refusé", e);
            ServerPayloadHandler.error(player, "Modèle refusé : " + e.getMessage());
        }
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
    }
}
