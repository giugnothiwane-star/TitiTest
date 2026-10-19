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
                           ResourceLocation dimension, BuildMode mode, boolean hollow, boolean confirmGiant,
                           int stepMode, boolean slow, boolean emptyOnly) {
    /** Construction automatique, rythme normal. */
    public static final int STEP_AUTO = 0;
    /** Pause après chaque couche. */
    public static final int STEP_LAYER = 1;
    /** Pause entre les grandes étapes (fondations, quarts de hauteur, détails, finitions). */
    public static final int STEP_STAGES = 2;

    public BuildRequest {
        stepMode = Math.max(STEP_AUTO, Math.min(STEP_STAGES, stepMode));
    }

    public BuildRequest(String modelId, SizePreset preset, int customSize, int rotation, boolean mirror, BlockPos origin,
                        ResourceLocation dimension, BuildMode mode, boolean hollow, boolean confirmGiant) {
        this(modelId, preset, customSize, rotation, mirror, origin, dimension, mode, hollow, confirmGiant, STEP_AUTO, false, false);
    }

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
        t.putInt("step", stepMode);
        t.putBoolean("slow", slow);
        t.putBoolean("emptyOnly", emptyOnly);
        return t;
    }

    public static BuildRequest load(CompoundTag t) {
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("dimension"));
        return new BuildRequest(t.getString("model"), SizePreset.byOrdinal(t.getInt("preset")), t.getInt("custom"),
                Math.floorMod(t.getInt("rotation"), 4), t.getBoolean("mirror"), BlockPos.of(t.getLong("origin")),
                dim == null ? ResourceLocation.withDefaultNamespace("overworld") : dim, BuildMode.byOrdinal(t.getInt("mode")),
                t.getBoolean("hollow"), t.getBoolean("confirm"), t.getInt("step"), t.getBoolean("slow"), t.getBoolean("emptyOnly"));
    }

    public BuildRequest withOrigin(BlockPos o) {
        return new BuildRequest(modelId, preset, customSize, rotation, mirror, o, dimension, mode, hollow, confirmGiant, stepMode, slow, emptyOnly);
    }
}
