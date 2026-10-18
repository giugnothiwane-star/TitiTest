package com.ultimateautobuilder.core.importer;

import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.validation.Limits;
import com.ultimateautobuilder.core.validation.StructureValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Format binaire « .uabm » des modèles personnalisés (compressé GZIP) :
 * en-tête "UABM", version, nom, dimensions, ancrage, palette, puis cellules en deltas varint.
 * La lecture est bornée (taille décompressée, dimensions, palette, nombre de blocs).
 */
public final class ModelCodec {
    private static final int MAGIC = 0x5541424D; // "UABM"
    private static final int VERSION = 1;
    private static final long MAX_DECOMPRESSED = 64L * 1024 * 1024;

    public record Named(String name, VoxelModel model) {
    }

    private ModelCodec() {
    }

    public static byte[] encode(String name, VoxelModel model) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        write(bytes, name, model);
        return bytes.toByteArray();
    }

    public static void write(OutputStream os, String name, VoxelModel m) throws IOException {
        GZIPOutputStream gz = new GZIPOutputStream(os);
        DataOutputStream out = new DataOutputStream(gz);
        out.writeInt(MAGIC);
        out.writeShort(VERSION);
        out.writeUTF(name == null ? "" : name);
        out.writeShort(m.sizeX());
        out.writeShort(m.sizeY());
        out.writeShort(m.sizeZ());
        out.writeShort(m.anchorX());
        out.writeShort(m.anchorY());
        out.writeShort(m.anchorZ());
        out.writeShort(m.paletteSize());
        for (int i = 0; i < m.paletteSize(); i++) {
            out.writeUTF(m.paletteEntry(i));
        }
        out.writeInt(m.size());
        long prev = 0;
        for (int i = 0; i < m.size(); i++) {
            long c = m.cell(i);
            long key = c >>> 16;
            writeVarLong(out, key - prev);
            prev = key;
            writeVarLong(out, VoxelModel.stateId(c));
        }
        out.flush();
        gz.finish();
    }

    public static Named decode(byte[] data, Limits limits) throws ImportException {
        return read(new ByteArrayInputStream(data), limits);
    }

    public static Named read(InputStream is, Limits limits) throws ImportException {
        try {
            DataInputStream in = new DataInputStream(new BoundedInputStream(new GZIPInputStream(is), MAX_DECOMPRESSED));
            if (in.readInt() != MAGIC) {
                throw new ImportException("Fichier .uabm invalide (signature)");
            }
            int version = in.readUnsignedShort();
            if (version != VERSION) {
                throw new ImportException("Version de fichier non prise en charge : " + version);
            }
            String name = in.readUTF();
            int sx = in.readUnsignedShort(), sy = in.readUnsignedShort(), sz = in.readUnsignedShort();
            int ax = in.readUnsignedShort(), ay = in.readUnsignedShort(), az = in.readUnsignedShort();
            int max = Math.max(sx, Math.max(sy, sz));
            if (max > limits.maxDimension() || max > VoxelModel.MAX_AXIS) {
                throw new ImportException("Dimensions hors limites : " + sx + "x" + sy + "x" + sz);
            }
            int paletteSize = in.readUnsignedShort();
            if (paletteSize == 0 || paletteSize > limits.maxPaletteSize()) {
                throw new ImportException("Taille de palette invalide : " + paletteSize);
            }
            String[] palette = new String[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                palette[i] = in.readUTF();
                if (!StructureValidator.isValidState(palette[i])) {
                    throw new ImportException("État de bloc invalide dans la palette : " + palette[i]);
                }
            }
            int count = in.readInt();
            if (count < 0 || count > limits.maxBlocks() || (long) count > (long) sx * sy * sz) {
                throw new ImportException("Nombre de blocs invalide : " + count);
            }
            long[] cells = new long[count];
            long key = 0;
            for (int i = 0; i < count; i++) {
                long delta = readVarLong(in);
                if (delta < 0 || (i > 0 && delta == 0)) {
                    throw new ImportException("Cellules non ordonnées ou dupliquées");
                }
                key += delta;
                long state = readVarLong(in);
                if (state < 0 || state >= paletteSize) {
                    throw new ImportException("Index de palette hors limites");
                }
                cells[i] = (key << 16) | state;
            }
            VoxelModel model;
            try {
                model = VoxelModel.of(sx, sy, sz, ax, ay, az, palette, cells);
            } catch (IllegalArgumentException e) {
                throw new ImportException("Données de modèle incohérentes : " + e.getMessage());
            }
            if (name.length() > 64) {
                name = name.substring(0, 64);
            }
            return new Named(name, model);
        } catch (EOFException e) {
            throw new ImportException("Fichier tronqué", e);
        } catch (IOException e) {
            throw new ImportException("Lecture impossible : " + e.getMessage(), e);
        }
    }

    static void writeVarLong(DataOutputStream out, long v) throws IOException {
        while ((v & ~0x7FL) != 0) {
            out.writeByte((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        out.writeByte((int) v);
    }

    static long readVarLong(DataInputStream in) throws IOException {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            int b = in.readUnsignedByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new IOException("Entier variable trop long");
    }

    /** Protège contre les « bombes de décompression ». */
    static final class BoundedInputStream extends InputStream {
        private final InputStream in;
        private long remaining;

        BoundedInputStream(InputStream in, long max) {
            this.in = in;
            this.remaining = max;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                throw new IOException("Fichier décompressé trop volumineux");
            }
            int b = in.read();
            if (b >= 0) {
                remaining--;
            }
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (remaining <= 0) {
                throw new IOException("Fichier décompressé trop volumineux");
            }
            int n = in.read(buf, off, (int) Math.min(len, remaining));
            if (n > 0) {
                remaining -= n;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /** Nom de fichier sûr (pas de traversée de répertoire). */
    public static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        String s = new String(name.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
        if (s.length() > 48) {
            s = s.substring(0, 48);
        }
        return s.replaceAll("^_+|_+$", "");
    }
}
