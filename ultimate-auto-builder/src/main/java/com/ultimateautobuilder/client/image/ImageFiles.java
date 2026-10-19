package com.ultimateautobuilder.client.image;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.image.ImageLoader;
import net.minecraft.Util;
import net.neoforged.fml.loading.FMLPaths;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Dossiers d'Image-to-Build (côté client) et sélecteur de fichier natif. Les chemins utilisés sont
 * toujours sous config/ultimateautobuilder ou choisis explicitement par le joueur dans la boîte de dialogue.
 */
public final class ImageFiles {
    private ImageFiles() {
    }

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve(UltimateAutoBuilderMod.MOD_ID).resolve("image_to_build");
    }

    /** Images déposées par le joueur ou reçues du téléphone. */
    public static Path images() {
        return root().resolve("images");
    }

    public static Path projects() {
        return root().resolve("projets");
    }

    public static Path exports() {
        return root().resolve("exports");
    }

    public static void ensureDirs() {
        try {
            Files.createDirectories(images());
            Files.createDirectories(projects());
            Files.createDirectories(exports());
        } catch (IOException e) {
            UltimateAutoBuilderMod.LOGGER.warn("Création des dossiers Image-to-Build impossible", e);
        }
    }

    /** Images utilisables du dossier, les plus récentes d'abord. */
    public static List<Path> listImages() {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(images())) {
            s.filter(Files::isRegularFile).filter(p -> ImageLoader.isSupportedName(p.getFileName().toString())).forEach(out::add);
        } catch (IOException ignored) {
            // Dossier absent : liste vide.
        }
        out.sort((a, b) -> {
            try {
                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
            } catch (IOException e) {
                return 0;
            }
        });
        return out;
    }

    public static void openFolder(Path dir) {
        ensureDirs();
        Util.getPlatform().openFile(dir.toFile());
    }

    /**
     * Boîte de dialogue native d'ouverture. Sous macOS elle doit s'exécuter sur le thread principal
     * (Cocoa) ; ailleurs elle est lancée sur un thread séparé pour ne pas figer le rendu.
     * Le résultat (null si annulé ou indisponible) est transmis sur le thread du jeu.
     */
    public static void chooseImage(Consumer<Path> onResult, Consumer<String> onError) {
        Runnable task = () -> {
            String path = null;
            String error = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(5);
                filters.put(stack.UTF8("*.png")).put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg")).put(stack.UTF8("*.bmp"))
                        .put(stack.UTF8("*.gif"));
                filters.flip();
                path = TinyFileDialogs.tinyfd_openFileDialog("Choisir une image", images().toAbsolutePath() + java.io.File.separator, filters,
                        "Images (PNG, JPEG, BMP, GIF)", false);
            } catch (Throwable t) {
                // Bibliothèque native absente sur certains systèmes : le dossier d'images reste disponible.
                error = "Sélecteur de fichiers indisponible (" + t.getClass().getSimpleName() + ") : déposez l'image dans le dossier images";
            }
            String finalPath = path, finalError = error;
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                if (finalError != null) {
                    onError.accept(finalError);
                } else if (finalPath != null && !finalPath.isBlank()) {
                    onResult.accept(Path.of(finalPath));
                }
            });
        };
        if (Util.getPlatform() == Util.OS.OSX) {
            task.run();
        } else {
            Thread t = new Thread(task, "UAB-FileDialog");
            t.setDaemon(true);
            t.start();
        }
    }

    public static boolean isImageName(String name) {
        return ImageLoader.isSupportedName(name.toLowerCase(Locale.ROOT));
    }
}
