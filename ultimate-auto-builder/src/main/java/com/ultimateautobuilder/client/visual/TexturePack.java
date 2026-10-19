package com.ultimateautobuilder.client.visual;

import com.mojang.blaze3d.platform.NativeImage;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.PackSelectionConfig;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pack de ressources généré par le studio (dossier {@code visual/generated_pack}, toujours actif, au-dessus
 * des autres packs). Il contient :
 * <ul>
 *   <li>les textures vanilla recolorées par groupe (lues dans les ressources vanilla d'origine, jamais dans
 *       ce pack : les recolorations ne s'accumulent pas) ;</li>
 *   <li>les textures personnelles déposées dans {@code visual/textures/<espace>/<chemin>.png}, validées
 *       (signature PNG, décodage, taille maximale) avant copie.</li>
 * </ul>
 * La génération se fait hors du thread de rendu ; seul le rechargement des ressources (écran de
 * chargement de Minecraft) a lieu sur le thread principal, à la demande du joueur.
 */
public final class TexturePack {
    public static final String PACK_ID = "ultimateautobuilder_studio";
    private static final int MAX_SIDE = 1024;
    private static final long MAX_BYTES = 4L * 1024 * 1024;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static volatile String report = "Aucune génération pour l'instant";

    private TexturePack() {
    }

    public static Path packDir() {
        return VisualStore.root().resolve("generated_pack");
    }

    public static Path userDir() {
        return VisualStore.root().resolve("textures");
    }

    public static String report() {
        return report;
    }

    public static boolean running() {
        return RUNNING.get();
    }

    /** Crée le squelette du pack (nécessaire avant l'inventaire des packs au démarrage). */
    public static void ensureSkeleton() {
        try {
            Files.createDirectories(packDir().resolve("assets"));
            Files.createDirectories(userDir().resolve("minecraft").resolve("textures").resolve("block"));
            Path meta = packDir().resolve("pack.mcmeta");
            if (!Files.isRegularFile(meta)) {
                // Format 34 = ressources de Minecraft 1.21 / 1.21.1.
                Files.writeString(meta, "{\"pack\":{\"pack_format\":34,\"description\":\"Ultimate Auto Builder - textures du studio visuel\"}}",
                        StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            report = "Dossier du pack inaccessible : " + e.getMessage();
            UltimateAutoBuilderMod.LOGGER.warn(report, e);
        }
    }

    /** Déclare le pack au client (événement du bus du mod). */
    public static void addPackFinder(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        ensureSkeleton();
        event.addRepositorySource(consumer -> {
            PackLocationInfo info = new PackLocationInfo(PACK_ID, Component.literal("Ultimate Auto Builder — Studio visuel"), PackSource.BUILT_IN,
                    Optional.empty());
            Pack pack = Pack.readMetaAndCreate(info, new PathPackResources.PathResourcesSupplier(packDir()), PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false));
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    /**
     * Régénère le pack d'après le profil, puis recharge les ressources du jeu.
     *
     * @return futur terminé quand les fichiers sont écrits (le rechargement suit sur le thread principal)
     */
    public static CompletableFuture<String> regenerate(VisualProfile profile) {
        if (!RUNNING.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture("Génération déjà en cours");
        }
        VisualProfile snapshot = profile.copy(profile.name);
        Minecraft mc = Minecraft.getInstance();
        return CompletableFuture.supplyAsync(() -> generate(mc, snapshot)).whenComplete((msg, err) -> {
            RUNNING.set(false);
            report = err != null ? "Erreur : " + err.getMessage() : msg;
            mc.execute(mc::reloadResourcePacks);
        });
    }

    private static String generate(Minecraft mc, VisualProfile p) {
        ensureSkeleton();
        Path assets = packDir().resolve("assets");
        deleteTree(assets);
        int recolored = 0, missing = 0, custom = 0, rejected = 0;
        List<String> errors = new ArrayList<>();
        for (var entry : p.textures.entrySet()) {
            TextureGroups.Group group = TextureGroups.get(entry.getKey());
            VisualProfile.Tint tint = entry.getValue();
            if (group == null || tint.isIdentity()) {
                continue;
            }
            for (String path : group.textures()) {
                ResourceLocation rl = ResourceLocation.withDefaultNamespace(path);
                IoSupplier<InputStream> src = mc.getVanillaPackResources().getResource(PackType.CLIENT_RESOURCES, rl);
                if (src == null) {
                    missing++;
                    continue;
                }
                Path out = assets.resolve("minecraft").resolve(path);
                try (InputStream in = src.get(); NativeImage img = NativeImage.read(in)) {
                    recolor(img, tint);
                    Files.createDirectories(out.getParent());
                    img.writeToFile(out);
                    copyMeta(mc, rl, out);
                    recolored++;
                } catch (IOException | RuntimeException e) {
                    errors.add(path + " : " + e.getMessage());
                }
            }
        }
        // Textures personnelles : copiées telles quelles après validation (elles remplacent les recolorations).
        Path user = userDir();
        if (Files.isDirectory(user)) {
            try (var stream = Files.walk(user, 8)) {
                for (Path f : (Iterable<Path>) stream::iterator) {
                    if (!Files.isRegularFile(f) || !f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                        continue;
                    }
                    String why = validatePng(f);
                    if (why != null) {
                        rejected++;
                        errors.add(user.relativize(f) + " : " + why);
                        continue;
                    }
                    Path rel = user.relativize(f);
                    if (rel.getNameCount() < 3) {
                        rejected++;
                        errors.add(rel + " : chemin attendu <espace>/textures/…/fichier.png");
                        continue;
                    }
                    Path out = assets.resolve(rel.toString()).normalize();
                    if (!out.startsWith(assets)) {
                        rejected++;
                        continue;
                    }
                    Files.createDirectories(out.getParent());
                    Files.copy(f, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    custom++;
                }
            } catch (IOException e) {
                errors.add("Lecture des textures personnelles : " + e.getMessage());
            }
        }
        String msg = recolored + " texture(s) recolorée(s), " + custom + " texture(s) personnelle(s)"
                + (rejected > 0 ? ", " + rejected + " refusée(s)" : "") + (missing > 0 ? ", " + missing + " absente(s) de cette version" : "");
        if (!errors.isEmpty()) {
            msg += " — " + errors.get(0) + (errors.size() > 1 ? " (+" + (errors.size() - 1) + ")" : "");
            errors.forEach(err -> UltimateAutoBuilderMod.LOGGER.warn("Studio visuel : {}", err));
        }
        return msg;
    }

    /** Copie le fichier .mcmeta d'origine (animation de l'eau, etc.) : sans lui, une texture animée serait déformée. */
    private static void copyMeta(Minecraft mc, ResourceLocation rl, Path out) throws IOException {
        IoSupplier<InputStream> meta = mc.getVanillaPackResources().getResource(PackType.CLIENT_RESOURCES,
                rl.withPath(rl.getPath() + ".mcmeta"));
        if (meta != null) {
            try (InputStream in = meta.get()) {
                Files.copy(in, out.resolveSibling(out.getFileName() + ".mcmeta"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** Vérifie qu'un fichier est un PNG décodable et de taille raisonnable ; renvoie la raison du refus ou null. */
    static String validatePng(Path f) {
        try {
            long size = Files.size(f);
            if (size > MAX_BYTES) {
                return "fichier trop volumineux (" + size / 1024 + " Ko, max " + MAX_BYTES / 1024 + " Ko)";
            }
            byte[] head = new byte[8];
            try (InputStream in = Files.newInputStream(f)) {
                if (in.read(head) != 8 || (head[0] & 0xFF) != 0x89 || head[1] != 'P' || head[2] != 'N' || head[3] != 'G') {
                    return "ce n'est pas un fichier PNG";
                }
            }
            try (InputStream in = Files.newInputStream(f); NativeImage img = NativeImage.read(in)) {
                if (img.getWidth() > MAX_SIDE || img.getHeight() > MAX_SIDE) {
                    return "dimensions trop grandes (" + img.getWidth() + "×" + img.getHeight() + ", max " + MAX_SIDE + ")";
                }
            }
            return null;
        } catch (IOException | RuntimeException e) {
            return "illisible : " + e.getMessage();
        }
    }

    /** Recoloration pixel par pixel (format ABGR de NativeImage), alpha conservé. */
    static void recolor(NativeImage img, VisualProfile.Tint t) {
        float[] hsv = new float[3];
        int tr = (t.color >> 16) & 255, tg = (t.color >> 8) & 255, tb = t.color & 255;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int abgr = img.getPixelRGBA(x, y);
                int a = (abgr >>> 24) & 255;
                if (a == 0) {
                    continue;
                }
                int r = abgr & 255, g = (abgr >> 8) & 255, b = (abgr >> 16) & 255;
                int rgb = ColorMath.transform(r, g, b, t, hsv, tr, tg, tb);
                img.setPixelRGBA(x, y, (a << 24) | ((rgb & 255) << 16) | (((rgb >> 8) & 255) << 8) | ((rgb >> 16) & 255));
            }
        }
    }

    /** Fichiers personnels présents (pour l'affichage). */
    public static int userTextureCount() {
        if (!Files.isDirectory(userDir())) {
            return 0;
        }
        try (var s = Files.walk(userDir(), 8)) {
            return (int) s.filter(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).count();
        } catch (IOException e) {
            return 0;
        }
    }

    /** Vide le pack (réinitialisation) et recharge les ressources. */
    public static void clear() {
        deleteTree(packDir().resolve("assets"));
        ensureSkeleton();
        report = "Pack vidé";
        Minecraft mc = Minecraft.getInstance();
        mc.execute(mc::reloadResourcePacks);
    }

    private static void deleteTree(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    if (!d.equals(dir)) {
                        Files.delete(d);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Nettoyage du pack incomplet : {}", e.getMessage());
        }
    }

    /** Liste des dossiers d'espaces de noms personnels (affichage du studio). */
    public static List<String> userNamespaces() {
        List<String> out = new ArrayList<>();
        if (Files.isDirectory(userDir())) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(userDir())) {
                ds.forEach(p -> out.add(p.getFileName().toString()));
            } catch (IOException ignored) {
                // Affichage seulement.
            }
        }
        return out;
    }
}
