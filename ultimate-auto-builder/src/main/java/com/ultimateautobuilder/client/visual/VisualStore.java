package com.ultimateautobuilder.client.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Profils graphiques du joueur : un fichier JSON par profil dans
 * {@code config/ultimateautobuilder/visual/profiles}, plus le nom du profil actif.
 * Un fichier illisible est renommé en {@code .corrompu} (jamais supprimé) et remplacé par le profil par défaut.
 */
public final class VisualStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_PROFILE_BYTES = 256 * 1024;
    public static final int MAX_PROFILES = 64;

    private static VisualProfile current = VisualProfile.preset("natural");
    private static String lastError = "";
    private static long version;

    private VisualStore() {
    }

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve(UltimateAutoBuilderMod.MOD_ID).resolve("visual");
    }

    public static Path profilesDir() {
        return root().resolve("profiles");
    }

    public static Path exportDir() {
        return root().resolve("export");
    }

    private static Path activeFile() {
        return root().resolve("active.txt");
    }

    public static VisualProfile current() {
        return current;
    }

    /** Incrémenté à chaque changement : les consommateurs (shaders, brouillard) savent quand se mettre à jour. */
    public static long version() {
        return version;
    }

    public static void touch() {
        version++;
    }

    public static String lastError() {
        return lastError;
    }

    /** Chargement au démarrage du client : profil actif, sinon création des préréglages fournis. */
    public static synchronized void load() {
        try {
            Files.createDirectories(profilesDir());
            if (listNames().isEmpty()) {
                for (String key : VisualProfile.PRESETS) {
                    save(VisualProfile.preset(key));
                }
            }
            String active = Files.isRegularFile(activeFile()) ? Files.readString(activeFile(), StandardCharsets.UTF_8).strip() : "Couleurs naturelles";
            VisualProfile p = read(fileFor(active));
            current = p != null ? p : VisualProfile.preset("natural");
        } catch (IOException e) {
            lastError = "Lecture des profils impossible : " + e.getMessage();
            UltimateAutoBuilderMod.LOGGER.warn(lastError, e);
        }
        touch();
    }

    public static synchronized void setCurrent(VisualProfile p) {
        current = p.sanitize();
        touch();
        try {
            Files.createDirectories(root());
            Files.writeString(activeFile(), current.name, StandardCharsets.UTF_8);
        } catch (IOException e) {
            lastError = "Profil actif non enregistré : " + e.getMessage();
        }
    }

    /** Nom de fichier sûr : lettres, chiffres, espaces et tirets ; jamais de chemin. */
    static String fileName(String name) {
        String n = name == null ? "" : name.strip();
        StringBuilder sb = new StringBuilder();
        for (char c : n.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
                sb.append(c);
            }
        }
        String s = sb.toString().strip().replace(' ', '_').toLowerCase(Locale.ROOT);
        return s.isEmpty() ? "profil" : s.length() > 40 ? s.substring(0, 40) : s;
    }

    private static Path fileFor(String name) {
        return profilesDir().resolve(fileName(name) + ".json");
    }

    public static synchronized List<String> listNames() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(profilesDir())) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(profilesDir(), "*.json")) {
            for (Path p : ds) {
                VisualProfile v = read(p);
                if (v != null) {
                    out.add(v.name);
                }
            }
        } catch (IOException e) {
            lastError = e.getMessage();
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static synchronized VisualProfile get(String name) {
        return read(fileFor(name));
    }

    /** Lecture validée ; un fichier corrompu est conservé à part et null est renvoyé. */
    static VisualProfile read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            if (Files.size(file) > MAX_PROFILE_BYTES) {
                throw new IOException("fichier trop volumineux");
            }
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                VisualProfile p = GSON.fromJson(r, VisualProfile.class);
                if (p == null) {
                    throw new JsonParseException("profil vide");
                }
                return p.sanitize();
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            lastError = "Profil illisible " + file.getFileName() + " : " + e.getMessage();
            UltimateAutoBuilderMod.LOGGER.warn(lastError);
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".corrompu"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // Le fichier reste en place ; il sera ignoré à chaque lecture.
            }
            return null;
        }
    }

    public static synchronized boolean save(VisualProfile p) {
        p.sanitize();
        try {
            Files.createDirectories(profilesDir());
            if (!Files.exists(fileFor(p.name)) && listNames().size() >= MAX_PROFILES) {
                lastError = "Nombre maximal de profils atteint (" + MAX_PROFILES + ")";
                return false;
            }
            write(fileFor(p.name), p);
            return true;
        } catch (IOException e) {
            lastError = "Enregistrement impossible : " + e.getMessage();
            return false;
        }
    }

    private static void write(Path file, VisualProfile p) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(p, w);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public static synchronized boolean delete(String name) {
        try {
            boolean ok = Files.deleteIfExists(fileFor(name));
            if (current.name.equals(name)) {
                setCurrent(VisualProfile.preset("natural"));
            }
            return ok;
        } catch (IOException e) {
            lastError = e.getMessage();
            return false;
        }
    }

    public static synchronized boolean rename(String oldName, String newName) {
        VisualProfile p = get(oldName);
        if (p == null || newName == null || newName.isBlank() || Files.exists(fileFor(newName))) {
            lastError = "Renommage impossible (nom vide ou déjà utilisé)";
            return false;
        }
        p.name = newName.strip();
        if (!save(p)) {
            return false;
        }
        try {
            Files.deleteIfExists(fileFor(oldName));
        } catch (IOException e) {
            lastError = e.getMessage();
        }
        if (current.name.equals(oldName)) {
            setCurrent(p);
        }
        return true;
    }

    /** Copie d'un profil sous un nom libre (« Nom (2) », « Nom (3) »…). */
    public static synchronized VisualProfile duplicate(String name) {
        VisualProfile src = get(name);
        if (src == null) {
            return null;
        }
        String base = src.name;
        String candidate = base + " (copie)";
        for (int i = 2; Files.exists(fileFor(candidate)) && i < 100; i++) {
            candidate = base + " (" + i + ")";
        }
        VisualProfile copy = src.copy(candidate);
        return save(copy) ? copy : null;
    }

    /** Exporte vers {@code visual/export/<nom>.json} ; renvoie le chemin écrit. */
    public static synchronized Path export(String name) throws IOException {
        VisualProfile p = get(name);
        if (p == null) {
            throw new IOException("profil introuvable");
        }
        Files.createDirectories(exportDir());
        Path out = exportDir().resolve(fileName(name) + ".json");
        write(out, p);
        return out;
    }

    /** Fichiers du dossier d'export disponibles pour l'import. */
    public static List<String> importable() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(exportDir())) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(exportDir(), "*.json")) {
            for (Path p : ds) {
                out.add(p.getFileName().toString());
            }
        } catch (IOException e) {
            lastError = e.getMessage();
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** Importe un fichier du dossier d'export (nom de fichier seul : aucun chemin arbitraire). */
    public static synchronized VisualProfile importFile(String fileName) {
        if (fileName == null || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..") || !fileName.endsWith(".json")) {
            lastError = "Nom de fichier refusé";
            return null;
        }
        Path f = exportDir().resolve(fileName).normalize();
        if (!f.startsWith(exportDir().normalize())) {
            lastError = "Chemin refusé";
            return null;
        }
        VisualProfile p = read(f);
        if (p == null) {
            return null;
        }
        if (Files.exists(fileFor(p.name))) {
            p.name = p.name + " (importé)";
        }
        return save(p) ? p : null;
    }
}
