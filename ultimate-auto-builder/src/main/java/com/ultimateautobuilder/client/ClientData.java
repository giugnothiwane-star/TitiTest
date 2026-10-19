package com.ultimateautobuilder.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Données personnelles du joueur conservées entre les sessions : favoris, constructions récentes,
 * taille, orientation et mode préférés, dernière catégorie. Fichier JSON écrit uniquement lors
 * d'un changement (jamais à chaque tick).
 */
public final class ClientData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_RECENTS = 30;

    /** Structure sérialisée. */
    public static final class Data {
        public List<String> favorites = new ArrayList<>();
        public List<String> recents = new ArrayList<>();
        public int preferredPreset = 1;
        public int preferredRotationOffset;
        public int preferredMode;
        public int positionMode;
        public int distance = 6;
        public boolean hollow = true;
        public String lastCategory = "all";
        public int sort;
    }

    private static Data data = new Data();

    private ClientData() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(UltimateAutoBuilderMod.MOD_ID).resolve("client-data.json");
    }

    public static synchronized void load() {
        Path f = file();
        if (!Files.isRegularFile(f)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                if (d.favorites == null) {
                    d.favorites = new ArrayList<>();
                }
                if (d.recents == null) {
                    d.recents = new ArrayList<>();
                }
                data = d;
            }
        } catch (IOException | JsonParseException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Données client illisibles, valeurs par défaut utilisées", e);
        }
    }

    public static synchronized void save() {
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling("client-data.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, w);
            }
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Sauvegarde des données client impossible", e);
        }
    }

    public static synchronized Data get() {
        return data;
    }

    public static synchronized boolean isFavorite(String id) {
        return data.favorites.contains(id);
    }

    public static synchronized void toggleFavorite(String id) {
        if (!data.favorites.remove(id)) {
            data.favorites.add(id);
        }
        save();
    }

    public static synchronized void addRecent(String id) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(id);
        set.addAll(data.recents);
        data.recents = new ArrayList<>(set);
        while (data.recents.size() > MAX_RECENTS) {
            data.recents.remove(data.recents.size() - 1);
        }
        save();
    }
}
