package com.ultimateautobuilder.storage;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Persistance dans le dossier du monde (data/ultimateautobuilder) : historique, journaux
 * d'annulation et points de reprise des constructions en cours. Les écritures se font sur un
 * thread dédié à partir d'instantanés, avec remplacement atomique pour éviter les fichiers corrompus.
 */
public final class ModStorage {
    private static final long MAX_FILE_BYTES = 512L * 1024 * 1024;
    private static ExecutorService io;
    private static Path root;
    /** Instantanés pas encore écrits : une lecture immédiate (retour arrière rapide) doit les voir. */
    private static final java.util.concurrent.ConcurrentHashMap<Path, CompoundTag> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    private ModStorage() {
    }

    public static synchronized void open(MinecraftServer server) {
        root = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(UltimateAutoBuilderMod.MOD_ID);
        try {
            Files.createDirectories(root.resolve("undo"));
            Files.createDirectories(root.resolve("jobs"));
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.error("Impossible de créer le dossier de données {}", root, e);
        }
        if (io == null || io.isShutdown()) {
            io = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "UAB-Storage");
                t.setDaemon(true);
                return t;
            });
        }
    }

    /** Termine les écritures en attente (arrêt du serveur). */
    public static synchronized void close() {
        if (io != null) {
            io.shutdown();
            try {
                if (!io.awaitTermination(20, TimeUnit.SECONDS)) {
                    UltimateAutoBuilderMod.LOGGER.warn("Écritures Ultimate Auto Builder interrompues à l'arrêt");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            io = null;
        }
    }

    public static Path root() {
        return root;
    }

    public static Path undoFile(int id) {
        return root.resolve("undo").resolve(id + ".dat");
    }

    public static Path jobFile(int id) {
        return root.resolve("jobs").resolve(id + ".dat");
    }

    public static Path historyFile() {
        return root.resolve("history.dat");
    }

    /** Écriture asynchrone d'un instantané NBT (remplacement atomique). */
    public static void writeAsync(Path file, CompoundTag snapshot) {
        ExecutorService ex = io;
        if (ex == null) {
            writeNow(file, snapshot);
            return;
        }
        PENDING.put(file, snapshot);
        ex.execute(() -> {
            writeNow(file, snapshot);
            PENDING.remove(file, snapshot);
        });
    }

    public static void writeNow(Path file, CompoundTag tag) {
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            NbtIo.writeCompressed(tag, tmp);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                // Certains systèmes de fichiers refusent le déplacement atomique.
                NbtIo.writeCompressed(tag, file);
            } catch (IOException e2) {
                UltimateAutoBuilderMod.LOGGER.error("Échec d'écriture de {}", file, e2);
            }
        }
    }

    public static CompoundTag read(Path file) {
        if (file == null) {
            return null;
        }
        CompoundTag pending = PENDING.get(file);
        if (pending != null) {
            return pending;
        }
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return NbtIo.readCompressed(file, NbtAccounter.create(MAX_FILE_BYTES));
        } catch (IOException | RuntimeException e) {
            UltimateAutoBuilderMod.LOGGER.error("Fichier illisible ignoré : {}", file, e);
            return null;
        }
    }

    public static void deleteAsync(Path file) {
        PENDING.remove(file);
        Runnable r = () -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                UltimateAutoBuilderMod.LOGGER.warn("Suppression impossible : {}", file);
            }
        };
        ExecutorService ex = io;
        if (ex == null) {
            r.run();
        } else {
            ex.execute(r);
        }
    }

    public static List<Path> listJobs() {
        List<Path> out = new ArrayList<>();
        if (root == null) {
            return out;
        }
        try (var s = Files.list(root.resolve("jobs"))) {
            s.filter(p -> p.getFileName().toString().endsWith(".dat")).forEach(out::add);
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Lecture des points de reprise impossible", e);
        }
        return out;
    }

    // ------------------------------------------------------------------ historique

    public record HistoryData(int nextId, List<HistoryEntry> entries) {
    }

    public static HistoryData loadHistory() {
        CompoundTag t = read(historyFile());
        List<HistoryEntry> list = new ArrayList<>();
        int next = 1;
        if (t != null) {
            next = Math.max(1, t.getInt("nextId"));
            ListTag l = t.getList("entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < l.size(); i++) {
                list.add(HistoryEntry.load(l.getCompound(i)));
            }
        }
        return new HistoryData(next, list);
    }

    public static void saveHistory(int nextId, List<HistoryEntry> entries) {
        CompoundTag t = new CompoundTag();
        t.putInt("nextId", nextId);
        ListTag l = new ListTag();
        for (HistoryEntry e : entries) {
            l.add(e.save());
        }
        t.put("entries", l);
        writeAsync(historyFile(), t);
    }
}
