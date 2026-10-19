package com.ultimateautobuilder.storage;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.core.model.ModelScaler;
import com.ultimateautobuilder.core.model.VoxelModel;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modèles personnalisés (enregistrés depuis une sélection ou importés) : fichiers .uabm du dossier
 * config/ultimateautobuilder/custom, ajoutés au catalogue sous l'identifiant « custom:nom ».
 */
public final class CustomModelStore {
    public record Info(String id, String name, int sizeX, int sizeY, int sizeZ, int blocks) {
    }

    private static final Map<String, Info> INFOS = new ConcurrentHashMap<>();

    private CustomModelStore() {
    }

    public static Path customDir() {
        return FMLPaths.CONFIGDIR.get().resolve(UltimateAutoBuilderMod.MOD_ID).resolve("custom");
    }

    public static Path importDir() {
        return FMLPaths.CONFIGDIR.get().resolve(UltimateAutoBuilderMod.MOD_ID).resolve("import");
    }

    /** Charge les modèles existants au démarrage ; les fichiers invalides sont signalés et ignorés. */
    public static void bootstrap() {
        try {
            Files.createDirectories(customDir());
            Files.createDirectories(importDir());
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Dossiers des modèles personnalisés inaccessibles", e);
            return;
        }
        try (var files = Files.list(customDir())) {
            files.filter(p -> p.getFileName().toString().endsWith(".uabm")).forEach(p -> {
                try (InputStream in = Files.newInputStream(p)) {
                    ModelCodec.Named named = ModelCodec.read(in, ServerConfig.limits());
                    String base = p.getFileName().toString().replace(".uabm", "");
                    register(base, named.name().isBlank() ? base : named.name(), named.model());
                } catch (IOException | ImportException e) {
                    UltimateAutoBuilderMod.LOGGER.warn("Modèle personnalisé ignoré ({}) : {}", p.getFileName(), e.getMessage());
                }
            });
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Lecture des modèles personnalisés impossible", e);
        }
    }

    /** Ajoute (ou remplace) un modèle dans le catalogue partagé. */
    public static synchronized ModelEntry register(String fileName, String displayName, VoxelModel model) {
        String safe = ModelCodec.sanitizeName(fileName);
        if (safe.isEmpty()) {
            throw new IllegalArgumentException("Nom invalide");
        }
        String id = "custom:" + safe;
        DefaultCatalog.shared().unregister(id);
        int h = Math.max(1, model.sizeY());
        int small = Math.max(4, h / 2);
        int normal = Math.max(small, h);
        int large = Math.max(normal, h * 3 / 2);
        int giant = Math.max(large, h * 2);
        ModelEntry e = new ModelEntry(id, displayName, Category.CUSTOM, new String[]{"personnalise", "import", safe}, 2,
                SizeSpec.height(small, normal, large, giant), false, p -> ModelScaler.toHeight(model, p.size()));
        DefaultCatalog.shared().register(e);
        INFOS.put(id, new Info(id, displayName, model.sizeX(), model.sizeY(), model.sizeZ(), model.size()));
        return e;
    }

    /** Enregistre sur disque puis dans le catalogue. */
    public static ModelEntry save(String name, VoxelModel model) throws IOException {
        String safe = ModelCodec.sanitizeName(name);
        if (safe.isEmpty()) {
            throw new IOException("Nom de modèle invalide");
        }
        Files.createDirectories(customDir());
        Path file = customDir().resolve(safe + ".uabm");
        try (OutputStream out = Files.newOutputStream(file)) {
            ModelCodec.write(out, name, model);
        }
        return register(safe, name, model);
    }

    public static synchronized boolean delete(String id) {
        if (!id.startsWith("custom:")) {
            return false;
        }
        String safe = ModelCodec.sanitizeName(id.substring(7));
        try {
            Files.deleteIfExists(customDir().resolve(safe + ".uabm"));
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Suppression impossible de {}", id, e);
            return false;
        }
        INFOS.remove(id);
        return DefaultCatalog.shared().unregister(id);
    }

    public static List<Info> list() {
        return new ArrayList<>(INFOS.values());
    }

    public static Info info(String id) {
        return INFOS.get(id);
    }

    /** Fichier du dossier d'import, sans possibilité de sortir du dossier. */
    public static Path resolveImport(String fileName) throws IOException {
        Path dir = importDir().toAbsolutePath().normalize();
        Path p = dir.resolve(fileName).normalize();
        if (!p.startsWith(dir) || !Files.isRegularFile(p)) {
            throw new IOException("Fichier introuvable dans " + importDir() + " : " + fileName);
        }
        return p;
    }

    public static List<String> importableFiles() {
        List<String> out = new ArrayList<>();
        try (var files = Files.list(importDir())) {
            files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".nbt") || n.endsWith(".vox") || n.endsWith(".uabm"))
                    .sorted().forEach(out::add);
        } catch (IOException ignored) {
            // Dossier absent : aucune proposition.
        }
        return out;
    }
}
