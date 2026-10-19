package com.ultimateautobuilder.network;

import com.ultimateautobuilder.client.ClientHooks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Enregistrement des paquets. Les gestionnaires côté client sont appelés via des lambdas :
 * la classe cliente n'est chargée que lorsqu'un paquet arrive réellement sur un client.
 */
public final class NetworkHandler {
    public static final String VERSION = "1";

    private NetworkHandler() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(VERSION);
        r.playToServer(BuildRequestPayload.TYPE, BuildRequestPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ServerPayloadHandler.onBuildRequest(payload, ctx.player())));
        r.playToServer(ActionPayload.TYPE, ActionPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ServerPayloadHandler.onAction(payload, ctx.player())));
        r.playToClient(StatePayload.TYPE, StatePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientHooks.onState(payload)));
        r.playToClient(JobProgressPayload.TYPE, JobProgressPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientHooks.onProgress(payload)));
        r.playToClient(SimplePayloads.OpenMenu.TYPE, SimplePayloads.OpenMenu.CODEC,
                (payload, ctx) -> ctx.enqueueWork(ClientHooks::openMenuFromServer));
        r.playToClient(SimplePayloads.CustomPreview.TYPE, SimplePayloads.CustomPreview.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientHooks.onCustomPreview(payload)));
    }
}
