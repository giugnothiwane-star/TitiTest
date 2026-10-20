package com.ultimateautobuilder.network;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Actions du menu (client → serveur) : annuler, retour arrière, paramètres, import, aperçu… */
public record ActionPayload(int action, int target, String arg) implements CustomPacketPayload {
    public static final int REQUEST_STATE = 0;
    public static final int CANCEL = 1;
    public static final int UNDO = 2;
    public static final int SET_SETTING = 3;
    public static final int DELETE_CUSTOM = 4;
    public static final int IMPORT = 5;
    public static final int SAVE_SELECTION = 6;
    public static final int REQUEST_CUSTOM_PREVIEW = 7;
    public static final int PAUSE = 8;
    public static final int RESUME = 9;
    // 10 à 12 : anciennes actions de déblocage et de quêtes (retirées), numéros non réutilisés.
    /** Récupère la récompense d'un ensemble d'armure porté (arg : clé de l'ensemble). */
    public static final int CLAIM_ARMOR = 14;
    /** Récupère les objets en attente (inventaire plein lors d'une récompense). */
    public static final int CLAIM_PENDING = 15;

    public static final Type<ActionPayload> TYPE = new Type<>(UltimateAutoBuilderMod.id("action"));
    public static final StreamCodec<FriendlyByteBuf, ActionPayload> CODEC = StreamCodec.ofMember(ActionPayload::write, ActionPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeVarInt(target);
        buf.writeUtf(arg == null ? "" : arg, 256);
    }

    private static ActionPayload read(FriendlyByteBuf buf) {
        return new ActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readUtf(256));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
