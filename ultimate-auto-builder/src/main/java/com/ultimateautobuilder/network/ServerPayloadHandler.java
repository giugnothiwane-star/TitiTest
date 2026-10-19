package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.builder.BuildManager;
import com.ultimateautobuilder.builder.BuildMode;
import com.ultimateautobuilder.builder.BuildRequest;
import com.ultimateautobuilder.builder.Job;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.core.model.ModelScaler;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.importer.ModelImporter;
import com.ultimateautobuilder.permission.Permissions;
import com.ultimateautobuilder.selection.Selections;
import com.ultimateautobuilder.storage.CustomModelStore;
import com.ultimateautobuilder.storage.HistoryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Traitement serveur des demandes du client : toute donnée reçue est revérifiée ici. */
public final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    static void onBuildRequest(BuildRequestPayload p, Player sender) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        if (!Permissions.canBuild(player)) {
            error(player, "Vous n'avez pas la permission de construire sur ce serveur");
            return;
        }
        BuildManager manager = BuildManager.get();
        if (manager == null) {
            return;
        }
        if (p.rotation() < 0 || p.rotation() > 3 || p.preset() < 0 || p.preset() >= SizePreset.values().length
                || p.mode() < 0 || p.mode() >= BuildMode.values().length) {
            error(player, "Paramètres de construction invalides");
            return;
        }
        ModelEntry entry = DefaultCatalog.shared().get(p.modelId());
        if (entry == null) {
            error(player, "Modèle inconnu sur ce serveur : " + p.modelId());
            return;
        }
        ServerLevel level = player.serverLevel();
        BuildRequest req = new BuildRequest(p.modelId(), SizePreset.byOrdinal(p.preset()), p.customSize(), p.rotation(), p.mirror(),
                new BlockPos(p.x(), p.y(), p.z()), level.dimension().location(), BuildMode.byOrdinal(p.mode()), p.hollow(), p.confirmGiant(),
                p.stepMode(), p.slow(), p.emptyOnly());
        try {
            Job job = manager.submit(player, level, req);
            info(player, "Construction de « " + entry.name() + " » lancée (n° " + job.id() + ")");
        } catch (IllegalStateException e) {
            error(player, e.getMessage());
        }
        sendState(player);
    }

    static void onAction(ActionPayload p, Player sender) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        BuildManager manager = BuildManager.get();
        if (manager == null) {
            return;
        }
        boolean admin = Permissions.isAdmin(player);
        try {
            switch (p.action()) {
                case ActionPayload.REQUEST_STATE -> {
                }
                case ActionPayload.CANCEL -> {
                    Job j = manager.cancel(player, p.target(), admin);
                    if (j == null) {
                        error(player, "Aucune construction à annuler");
                    } else {
                        info(player, "Annulation demandée : " + j.title());
                    }
                }
                case ActionPayload.PAUSE, ActionPayload.RESUME -> {
                    boolean pause = p.action() == ActionPayload.PAUSE;
                    Job j = manager.setPaused(player, p.target(), admin, pause);
                    if (j == null) {
                        error(player, "Aucune construction correspondante");
                    } else {
                        info(player, (pause ? "Pause : " : "Reprise : ") + j.title());
                    }
                }
                case ActionPayload.UNDO -> {
                    if (!Permissions.canBuild(player)) {
                        throw new IllegalStateException("Permission refusée");
                    }
                    var job = manager.undo(player, player.serverLevel(), p.target(), admin);
                    info(player, "Retour arrière lancé (" + job.total() + " blocs)");
                }
                case ActionPayload.SET_SETTING -> {
                    if (!admin) {
                        throw new IllegalStateException("Seuls les administrateurs peuvent modifier les paramètres du serveur");
                    }
                    int eq = p.arg().indexOf('=');
                    if (eq <= 0) {
                        throw new IllegalStateException("Paramètre mal formé");
                    }
                    String err = ServerConfig.apply(p.arg().substring(0, eq), p.arg().substring(eq + 1));
                    if (err != null) {
                        throw new IllegalStateException(err);
                    }
                }
                case ActionPayload.DELETE_CUSTOM -> {
                    if (!Permissions.canImport(player)) {
                        throw new IllegalStateException("Permission refusée");
                    }
                    if (!CustomModelStore.delete(p.arg())) {
                        throw new IllegalStateException("Modèle introuvable");
                    }
                    info(player, "Modèle supprimé");
                }
                case ActionPayload.IMPORT -> {
                    if (!Permissions.canImport(player)) {
                        throw new IllegalStateException("Permission refusée");
                    }
                    String[] parts = p.arg().split("\\|", 2);
                    var res = ModelImporter.importFile(player.server, parts[0], parts.length > 1 ? parts[1] : null);
                    info(player, "Modèle importé : " + res.name() + " (" + res.blocks() + " blocs"
                            + (res.skippedInvalid() > 0 ? ", " + res.skippedInvalid() + " blocs invalides ignorés" : "") + ")");
                }
                case ActionPayload.SAVE_SELECTION -> {
                    if (!Permissions.canImport(player)) {
                        throw new IllegalStateException("Permission refusée");
                    }
                    var sel = Selections.get(player);
                    if (sel == null) {
                        throw new IllegalStateException("Définissez d'abord deux coins avec le cristal (accroupi + clic droit)");
                    }
                    if (sel.volume() > (long) ServerConfig.limits().maxBlocks() * 4) {
                        throw new IllegalStateException("Zone sélectionnée trop grande");
                    }
                    VoxelModel model = ModelImporter.capture(player.serverLevel(), sel.min(), sel.max(), ServerConfig.limits());
                    var e = CustomModelStore.save(p.arg(), model);
                    info(player, "Modèle enregistré : " + e.name() + " (" + model.size() + " blocs)");
                }
                case ActionPayload.REQUEST_CUSTOM_PREVIEW -> {
                    ModelEntry e = DefaultCatalog.shared().get(p.arg());
                    if (e == null || !p.arg().startsWith("custom:")) {
                        return;
                    }
                    VoxelModel m = e.generate(e.sizes().normal(), false);
                    int h = Math.min(48, m.sizeY());
                    VoxelModel small = ModelScaler.toHeight(m, h);
                    byte[] data = ModelCodec.encode(e.name(), small);
                    if (data.length <= SimplePayloads.CustomPreview.MAX_BYTES) {
                        PacketDistributor.sendToPlayer(player, new SimplePayloads.CustomPreview(e.id(), data));
                    }
                    return;
                }
                default -> throw new IllegalStateException("Action inconnue");
            }
        } catch (IllegalStateException | ImportException e) {
            error(player, e.getMessage());
        } catch (IOException e) {
            error(player, "Erreur d'écriture : " + e.getMessage());
        } catch (RuntimeException e) {
            UltimateAutoBuilderMod.LOGGER.error("Action {} refusée", p.action(), e);
            error(player, "Erreur : " + e.getMessage());
        }
        sendState(player);
    }

    /** Envoie l'état complet au joueur (ouverture du menu, après chaque action). */
    public static void sendState(ServerPlayer player) {
        BuildManager manager = BuildManager.get();
        List<StatePayload.JobInfo> jobs = new ArrayList<>();
        List<StatePayload.HistoryInfo> history = new ArrayList<>();
        if (manager != null) {
            for (Job j : manager.jobs()) {
                jobs.add(new StatePayload.JobInfo(j.id(), j.title(), j.progress(), j.total(), j.state().ordinal(), j.ownerName(), j.paused(),
                        j.pauseReason()));
            }
            for (HistoryEntry e : manager.history().snapshot()) {
                history.add(new StatePayload.HistoryInfo(e.id, e.modelId, e.modelName, e.bounds, e.rotation, e.blocks, e.startedAt, e.status.name(),
                        e.undoAvailable, e.ownerName));
            }
        }
        List<StatePayload.CustomInfo> customs = new ArrayList<>();
        for (CustomModelStore.Info i : CustomModelStore.list()) {
            customs.add(new StatePayload.CustomInfo(i.id(), i.name(), i.sizeX(), i.sizeY(), i.sizeZ(), i.blocks()));
        }
        BlockPos c1 = Selections.corner(player, 1), c2 = Selections.corner(player, 2);
        var limits = ServerConfig.limits();
        ServerLevel level = player.serverLevel();
        PacketDistributor.sendToPlayer(player, new StatePayload(Permissions.canBuild(player), Permissions.isAdmin(player), Permissions.canImport(player),
                limits.maxBlocks(), limits.giantThreshold(), level.getMinBuildHeight(), level.getMaxBuildHeight(), ServerConfig.snapshot(), jobs, history,
                customs, Permissions.canImport(player) ? CustomModelStore.importableFiles() : List.of(),
                c1 == null ? 0 : c1.asLong(), c2 == null ? 0 : c2.asLong(), c1 != null, c2 != null));
    }

    static void info(ServerPlayer p, String msg) {
        p.sendSystemMessage(Component.literal("[Ultimate Auto Builder] " + msg).withStyle(ChatFormatting.AQUA));
    }

    static void error(ServerPlayer p, String msg) {
        p.sendSystemMessage(Component.literal("[Ultimate Auto Builder] " + msg).withStyle(ChatFormatting.RED));
    }
}
