package com.ultimateautobuilder.storage;

import com.ultimateautobuilder.selection.Selections;
import net.minecraft.world.entity.player.Player;

/** Nettoyage à la déconnexion (le kit de départ est géré par {@code adventure.StarterKit}). */
public final class PlayerJoinHandler {
    private PlayerJoinHandler() {
    }

    public static void onLogout(Player player) {
        Selections.clear(player.getUUID());
    }
}
