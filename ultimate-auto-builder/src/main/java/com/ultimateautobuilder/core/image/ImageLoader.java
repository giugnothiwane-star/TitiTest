package com.ultimateautobuilder.core.image;

import com.ultimateautobuilder.core.importer.ImportException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

/**
 * Chargement sécurisé d'une image utilisateur : taille de fichier bornée, dimensions lues dans
 * l'en-tête AVANT tout décodage (une image de 30 000 px n'est jamais décompressée entièrement),
 * sous-échantillonnage au décodage, formats réellement pris en charge par la JVM (PNG, JPEG, BMP, GIF).
 * L'image d'origine n'est jamais modifiée.
 */
public final class ImageLoader {
    public static final long MAX_FILE_BYTES = 25L * 1024 * 1024;
    public static final int MAX_SIDE = 16_384;
    public static final long MAX_PIXELS = 120_000_000L;
    /** Taille de travail maximale (côté le plus long) après chargement. */
    public static final int WORK_SIDE = 1024;

    private ImageLoader() {
    }

    /** Extensions acceptées (la vérification réelle se fait sur le contenu). */
    public static boolean isSupportedName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".bmp") || n.endsWith(".gif");
    }

    public static ImageData load(Path file) throws ImportException {
        try {
            if (!Files.isRegularFile(file)) {
                throw new ImportException("Fichier introuvable : " + file.getFileName());
            }
            long size = Files.size(file);
            if (size > MAX_FILE_BYTES) {
                throw new ImportException("Image trop volumineuse (" + size / (1024 * 1024) + " Mo, maximum " + MAX_FILE_BYTES / (1024 * 1024) + " Mo)");
            }
            if (size == 0) {
                throw new ImportException("Fichier vide");
            }
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".webp") || name.endsWith(".heic") || name.endsWith(".avif")) {
                throw new ImportException("Format non pris en charge (" + name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT)
                        + ") : convertissez l'image en PNG ou JPEG (capture d'écran ou « Exporter » depuis la galerie)");
            }
            try (InputStream in = Files.newInputStream(file)) {
                return decode(in);
            }
        } catch (IOException e) {
            throw new ImportException("Lecture impossible : " + e.getMessage());
        }
    }

    public static ImageData load(byte[] data) throws ImportException {
        if (data == null || data.length == 0) {
            throw new ImportException("Données vides");
        }
        if (data.length > MAX_FILE_BYTES) {
            throw new ImportException("Image trop volumineuse");
        }
        try {
            return decode(new ByteArrayInputStream(data));
        } catch (IOException e) {
            throw new ImportException("Lecture impossible : " + e.getMessage());
        }
    }

    private static ImageData decode(InputStream in) throws IOException, ImportException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            if (iis == null) {
                throw new ImportException("Flux d'image illisible");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new ImportException("Format non reconnu (formats acceptés : PNG, JPEG, BMP, GIF)");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w > MAX_SIDE || h > MAX_SIDE || (long) w * h > MAX_PIXELS) {
                    throw new ImportException("Dimensions refusées : " + w + "×" + h + " (maximum " + MAX_SIDE + " px de côté)");
                }
                ImageReadParam param = reader.getDefaultReadParam();
                // Sous-échantillonnage au décodage : la mémoire reste bornée même pour une photo de 50 Mpx.
                int sub = Math.max(1, Math.max(w, h) / (WORK_SIDE * 2));
                param.setSourceSubsampling(sub, sub, 0, 0);
                BufferedImage img = reader.read(0, param);
                return fromBuffered(img).downscale(WORK_SIDE);
            } catch (RuntimeException e) {
                // Fichiers malformés : les décodeurs peuvent lever des exceptions non vérifiées.
                throw new ImportException("Image corrompue ou illisible : " + e.getMessage());
            } finally {
                reader.dispose();
            }
        }
    }

    static ImageData fromBuffered(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        if (!img.getColorModel().hasAlpha()) {
            for (int i = 0; i < px.length; i++) {
                px[i] |= 0xFF000000;
            }
        }
        return new ImageData(w, h, px);
    }

    /** Écrit une image (copie de travail réduite d'un projet) en PNG. */
    public static void writePng(ImageData img, Path file) throws IOException {
        BufferedImage bi = new BufferedImage(img.width(), img.height(), BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, img.width(), img.height(), img.argb(), 0, img.width());
        Files.createDirectories(file.toAbsolutePath().getParent());
        if (!ImageIO.write(bi, "png", file.toFile())) {
            throw new IOException("Encodeur PNG indisponible");
        }
    }
}
