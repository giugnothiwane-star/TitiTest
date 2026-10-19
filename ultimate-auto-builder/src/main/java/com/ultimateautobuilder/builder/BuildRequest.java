package com.ultimateautobuilder.builder;

import com.ultimateautobuilder.core.catalog.SizePreset;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Demande de construction validée côté serveur (aucune donnée de bloc ne vient du client :
 * seulement l'identifiant du modèle et ses paramètres).
 */
public record BuildRequest(String modelId, SizePreset preset, int customSize, int rotation, boolean mirror, BlockPos origin,
                           ResourceLocation dimension, BuildMode mode, boolean hollow, boolean confirmGiant) {

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("model", modelId);
        t.putInt("preset", preset.ordinal());
        t.putInt("custom", customSize);
        t.putInt("rotation", rotation);
        t.putBoolean("mirror", mirror);
        t.putLong("origin", origin.asLong());
        t.putString("dimension", dimension.toString());
        t.putInt("mode", mode.ordinal());
        t.putBoolean("hollow", hollow);
        t.putBoolean("confirm", confirmGiant);
        return t;
    }

    public static BuildRequest load(CompoundTag t) {
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("dimension"));
        return new BuildRequest(t.getString("model"), SizePreset.byOrdinal(t.getInt("preset")), t.getInt("custom"),
                Math.floorMod(t.getInt("rotation"), 4), t.getBoolean("mirror"), BlockPos.of(t.getLong("origin")),
                dim == null ? ResourceLocation.withDefaultNamespace("overworld") : dim, BuildMode.byOrdinal(t.getInt("mode")),
                t.getBoolean("hollow"), t.getBoolean("confirm"));
    }

    public BuildRequest withOrigin(BlockPos o) {
        return new BuildRequest(modelId, preset, customSize, rotation, mirror, o, dimension, mode, hollow, confirmGiant);
    }
}
