package com.ultimateautobuilder.importer;

import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.core.importer.RawStructure;
import com.ultimateautobuilder.core.importer.VoxImporter;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.validation.Limits;
import com.ultimateautobuilder.storage.CustomModelStore;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Importation de modèles depuis le dossier config/ultimateautobuilder/import :
 * structures Minecraft (.nbt, format des blocs de structure, mises à niveau par DataFixer),
 * modèles MagicaVoxel (.vox) et modèles Ultimate Auto Builder (.uabm). Tout fichier est
 * contrôlé (taille, dimensions, palette, positions) avant d'être ajouté au catalogue.
 */
public final class ModelImporter {
    private ModelImporter() {
    }

    public record Result(String id, String name, int blocks, int skippedInvalid) {
    }

    public static Result importFile(MinecraftServer server, String fileName, String displayName) throws ImportException {
        Path file;
        byte[] data;
        try {
            file = CustomModelStore.resolveImport(fileName);
            long maxBytes = (ServerConfig.loaded() ? ServerConfig.MAX_IMPORT_KB.get() : 8192) * 1024L;
            if (Files.size(file) > maxBytes) {
                throw new ImportException("Fichier trop volumineux (" + Files.size(file) / 1024 + " Ko, maximum " + maxBytes / 1024 + " Ko)");
            }
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ImportException(e.getMessage(), e);
        }
        Limits limits = ServerConfig.limits();
        String lower = fileName.toLowerCase(Locale.ROOT);
        VoxelModel model;
        int skipped = 0;
        if (lower.endsWith(".vox")) {
            model = VoxImporter.read(data, limits);
        } else if (lower.endsWith(".uabm")) {
            model = ModelCodec.decode(data, limits).model();
        } else if (lower.endsWith(".nbt")) {
            RawStructure raw = readStructureNbt(server, data, limits);
            skipped = raw.skippedInvalid();
            model = raw.toModel();
        } else {
            throw new ImportException("Format non pris en charge (formats acceptés : .nbt, .vox, .uabm)");
        }
        String name = displayName == null || displayName.isBlank() ? fileName.replaceAll("\\.[a-zA-Z]+$", "") : displayName;
        try {
            var entry = CustomModelStore.save(name, model);
            return new Result(entry.id(), entry.name(), model.size(), skipped);
        } catch (IOException e) {
            throw new ImportException("Enregistrement impossible : " + e.getMessage(), e);
        }
    }

    /** Lecture du format « structure » vanilla (size, palette/palettes, blocks). */
    static RawStructure readStructureNbt(MinecraftServer server, byte[] data, Limits limits) throws ImportException {
        CompoundTag tag;
        try {
            tag = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.create(64L * 1024 * 1024));
        } catch (IOException | RuntimeException e) {
            throw new ImportException("Fichier NBT illisible : " + e.getMessage(), e);
        }
        int version = NbtUtils.getDataVersion(tag, 500);
        if (version < SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
            tag = DataFixTypes.STRUCTURE.updateToCurrentVersion(server.getFixerUpper(), tag, version);
        }
        ListTag size = tag.getList("size", Tag.TAG_INT);
        if (size.size() != 3) {
            throw new ImportException("Champ « size » absent ou invalide");
        }
        RawStructure raw = new RawStructure(size.getInt(0), size.getInt(1), size.getInt(2), limits);
        ListTag palette = tag.contains("palettes", Tag.TAG_LIST) ? tag.getList("palettes", Tag.TAG_LIST).getList(0)
                : tag.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty()) {
            throw new ImportException("Palette vide");
        }
        for (int i = 0; i < palette.size(); i++) {
            BlockState st = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(i));
            String name = palette.getCompound(i).getString("Name");
            ResourceLocation rl = ResourceLocation.tryParse(name);
            boolean known = rl != null && BuiltInRegistries.BLOCK.containsKey(rl);
            // Bloc inconnu (mod absent) : on garde son nom pour que le résolveur applique un repli lisible.
            raw.addPaletteEntry(known ? BlockStateParser.serialize(st) : name);
        }
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            if (pos.size() != 3) {
                throw new ImportException("Position de bloc invalide (entrée " + i + ")");
            }
            raw.put(pos.getInt(0), pos.getInt(1), pos.getInt(2), b.getInt("state"));
        }
        return raw;
    }

    /** Capture d'une zone du monde en modèle personnalisé. */
    public static VoxelModel capture(ServerLevel level, BlockPos min, BlockPos max, Limits limits) throws ImportException {
        RawStructure raw = new RawStructure(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1, limits);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    p.set(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (st.isAir()) {
                        continue;
                    }
                    raw.put(x - min.getX(), y - min.getY(), z - min.getZ(), BlockStateParser.serialize(st));
                }
            }
        }
        return raw.toModel();
    }
}
