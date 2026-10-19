package com.ultimateautobuilder.core.image;

import com.ultimateautobuilder.core.importer.ImportException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Projet Image-to-Build sauvegardé dans un dossier dédié : options (texte), copie de travail réduite
 * des images (PNG, l'original n'est jamais modifié) et modèle édité avec ses certitudes (binaire
 * compressé, validé à la relecture).
 */
public final class ImageBuildProject {
    public static final int FORMAT = 1;
    private static final int MAGIC = 0x55414249; // "UABI"
    private static final Pattern STATE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+(\\[[a-z0-9_=,]*])?");
    private static final long MAX_MODEL_BYTES = 32L * 1024 * 1024;

    public record Loaded(String name, ImageData front, ImageData side, ImageData back, ImageData top, Reconstructor.Options options,
                         ImageModel model) {
    }

    private ImageBuildProject() {
    }

    /** Nom de dossier sûr : lettres, chiffres, tirets ; jamais de chemin. */
    public static String folderName(String name) {
        String n = com.ultimateautobuilder.core.util.TextNormalizer.normalize(name == null ? "" : name).replaceAll("[^a-z0-9_-]+", "_");
        n = n.replaceAll("_+", "_").replaceAll("^_|_$", "");
        if (n.isEmpty()) {
            n = "projet";
        }
        return n.length() > 48 ? n.substring(0, 48) : n;
    }

    private static Path dir(Path root, String name) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path d = base.resolve(folderName(name)).normalize();
        if (!d.startsWith(base) || d.equals(base)) {
            throw new IOException("Nom de projet refusé");
        }
        return d;
    }

    public static Path save(Path root, String name, ImageData front, ImageData side, ImageData back, ImageData top, Reconstructor.Options o,
                            ImageModel model) throws IOException {
        Path d = dir(root, name);
        Files.createDirectories(d);
        Properties p = new Properties();
        p.setProperty("format", Integer.toString(FORMAT));
        p.setProperty("name", name == null ? "" : name.length() > 64 ? name.substring(0, 64) : name);
        p.setProperty("mode", o.mode().name());
        p.setProperty("back", o.back().name());
        p.setProperty("style", o.style().name());
        StringBuilder fam = new StringBuilder();
        for (BlockPalette.Family f : o.customFamilies()) {
            fam.append(fam.isEmpty() ? "" : ",").append(f.name());
        }
        p.setProperty("families", fam.toString());
        p.setProperty("width", Integer.toString(o.width()));
        p.setProperty("height", Integer.toString(o.height()));
        p.setProperty("depth", Integer.toString(o.depth()));
        p.setProperty("detail", o.detail().name());
        p.setProperty("hollow", Boolean.toString(o.hollow()));
        p.setProperty("subject", o.subject().name());
        p.setProperty("invertRelief", Boolean.toString(o.invertRelief()));
        p.setProperty("sideFlipped", Boolean.toString(o.sideFlipped()));
        writeImage(d, "front.png", front);
        writeImage(d, "side.png", side);
        writeImage(d, "back.png", back);
        writeImage(d, "top.png", top);
        if (model != null) {
            Path tmp = d.resolve("model.bin.tmp");
            try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(tmp)); DataOutputStream out = new DataOutputStream(os)) {
                writeModel(out, model);
            }
            Files.move(tmp, d.resolve("model.bin"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try (Writer w = Files.newBufferedWriter(d.resolve("project.properties"), StandardCharsets.UTF_8)) {
            p.store(w, "Projet Image-to-Build - Ultimate Auto Builder");
        }
        return d;
    }

    private static void writeImage(Path d, String file, ImageData img) throws IOException {
        Path f = d.resolve(file);
        if (img == null) {
            Files.deleteIfExists(f);
        } else {
            ImageLoader.writePng(img, f);
        }
    }

    public static Loaded load(Path root, String folder) throws ImportException {
        try {
            Path d = dir(root, folder);
            Path props = d.resolve("project.properties");
            if (!Files.isRegularFile(props)) {
                throw new ImportException("Projet introuvable : " + folder);
            }
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(props, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            if (parseInt(p, "format", 0) > FORMAT) {
                throw new ImportException("Projet créé par une version plus récente du mod");
            }
            ImageData front = ImageLoader.load(d.resolve("front.png"));
            ImageData side = optional(d.resolve("side.png"));
            ImageData back = optional(d.resolve("back.png"));
            ImageData top = optional(d.resolve("top.png"));
            Set<BlockPalette.Family> fams = EnumSet.noneOf(BlockPalette.Family.class);
            for (String s : p.getProperty("families", "").split(",")) {
                BlockPalette.Family f = parseEnum(BlockPalette.Family.class, s.trim(), null);
                if (f != null) {
                    fams.add(f);
                }
            }
            Reconstructor.Options o = new Reconstructor.Options(parseEnum(Reconstructor.Mode.class, p.getProperty("mode"), Reconstructor.Mode.AUTO),
                    parseEnum(Reconstructor.BackMode.class, p.getProperty("back"), Reconstructor.BackMode.SYMMETRIC),
                    parseEnum(BuildStyle.class, p.getProperty("style"), BuildStyle.FIDELE), fams, parseInt(p, "width", 0), parseInt(p, "height", 48),
                    parseInt(p, "depth", 0), parseEnum(Reconstructor.Detail.class, p.getProperty("detail"), Reconstructor.Detail.MEDIUM),
                    Boolean.parseBoolean(p.getProperty("hollow", "true")),
                    parseEnum(ImageAnalysis.Subject.class, p.getProperty("subject"), ImageAnalysis.Subject.UNKNOWN),
                    Boolean.parseBoolean(p.getProperty("invertRelief", "false")), Boolean.parseBoolean(p.getProperty("sideFlipped", "false")));
            ImageModel model = null;
            Path mf = d.resolve("model.bin");
            if (Files.isRegularFile(mf)) {
                if (Files.size(mf) > MAX_MODEL_BYTES) {
                    throw new ImportException("Modèle du projet trop volumineux");
                }
                try (InputStream is = new GZIPInputStream(Files.newInputStream(mf)); DataInputStream in = new DataInputStream(is)) {
                    model = readModel(in);
                }
            }
            return new Loaded(p.getProperty("name", folder), front, side, back, top, o, model);
        } catch (IOException | RuntimeException e) {
            throw new ImportException("Projet illisible : " + e.getMessage());
        }
    }

    private static ImageData optional(Path f) throws ImportException {
        return Files.isRegularFile(f) ? ImageLoader.load(f) : null;
    }

    public static List<String> list(Path root) {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return out;
        }
        try (Stream<Path> s = Files.list(root)) {
            s.filter(p -> Files.isRegularFile(p.resolve("project.properties"))).forEach(p -> out.add(p.getFileName().toString()));
        } catch (IOException ignored) {
            // Dossier illisible : liste vide.
        }
        out.sort(String::compareTo);
        return out;
    }

    static void writeModel(DataOutputStream out, ImageModel m) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(FORMAT);
        out.writeShort(m.sizeX());
        out.writeShort(m.sizeY());
        out.writeShort(m.sizeZ());
        List<String> palette = new ArrayList<>(m.materials().keySet());
        java.util.Map<String, Integer> ids = new java.util.HashMap<>();
        out.writeShort(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            out.writeUTF(palette.get(i));
            ids.put(palette.get(i), i + 1);
        }
        for (int y = 0; y < m.sizeY(); y++) {
            for (int z = 0; z < m.sizeZ(); z++) {
                for (int x = 0; x < m.sizeX(); x++) {
                    String s = m.get(x, y, z);
                    out.writeShort(s == null ? 0 : ids.get(s));
                    out.writeByte(s == null ? 0 : m.certainty(x, y, z));
                }
            }
        }
    }

    static ImageModel readModel(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) {
            throw new IOException("Signature invalide");
        }
        if (in.readInt() > FORMAT) {
            throw new IOException("Version de modèle inconnue");
        }
        int sx = in.readUnsignedShort(), sy = in.readUnsignedShort(), sz = in.readUnsignedShort();
        ImageModel m = new ImageModel(sx, sy, sz);
        int n = in.readUnsignedShort();
        String[] palette = new String[n];
        for (int i = 0; i < n; i++) {
            String s = in.readUTF().toLowerCase(Locale.ROOT);
            if (!STATE.matcher(s).matches()) {
                throw new IOException("État de bloc invalide : " + s);
            }
            palette[i] = s;
        }
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int id = in.readUnsignedShort();
                    byte cert = in.readByte();
                    if (id > n) {
                        throw new IOException("Index de palette hors limites");
                    }
                    if (id > 0) {
                        m.set(x, y, z, palette[id - 1], cert == ImageModel.VISIBLE ? ImageModel.VISIBLE : ImageModel.ESTIMATED);
                    }
                }
            }
        }
        return m;
    }

    private static int parseInt(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, Integer.toString(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String v, E def) {
        if (v == null) {
            return def;
        }
        try {
            return Enum.valueOf(type, v.trim());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
