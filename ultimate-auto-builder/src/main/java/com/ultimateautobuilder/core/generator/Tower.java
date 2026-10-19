package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;

/**
 * Immeubles et gratte-ciel : façade rythmée (montants, vitrages, bandeaux d'étage), retraits
 * successifs, hall d'entrée, noyau central et équipements de toiture.
 */
public final class Tower {
    public enum Crown { FLAT, ANTENNA, HELIPAD, SPIRE, DOME, GARDEN }

    public static final class Spec {
        public String frame = "minecraft:light_gray_concrete";
        public String glass = "minecraft:light_blue_stained_glass";
        public String band = "minecraft:gray_concrete";
        public String slab = "minecraft:smooth_stone";
        public String lobby = "minecraft:polished_andesite";
        public double heightFactor = 2.5; // hauteur ≈ largeur × facteur
        public int setbacks = 1;
        public int column = 3;            // espacement des montants
        public Crown crown = Crown.ANTENNA;
        public boolean interiorAir = true;
        public boolean balconies;
        public String sign;               // couleur du bandeau d'enseigne au sommet (null = aucun)
        public String light = "minecraft:sea_lantern";

        public Spec colors(String frame, String glass, String band) { this.frame = B.mc(frame); this.glass = B.mc(glass); this.band = B.mc(band); return this; }
        public Spec height(double k) { heightFactor = k; return this; }
        public Spec setbacks(int n) { setbacks = n; return this; }
        public Spec column(int c) { column = Math.max(2, c); return this; }
        public Spec crown(Crown c) { crown = c; return this; }
        public Spec noInteriorAir() { interiorAir = false; return this; }
        public Spec balconies() { balconies = true; return this; }
        public Spec sign(String color) { sign = color; return this; }
        public Spec light(String l) { light = B.mc(l); return this; }
        public Spec copy() {
            Spec c = new Spec();
            c.frame = frame; c.glass = glass; c.band = band; c.slab = slab; c.lobby = lobby; c.heightFactor = heightFactor;
            c.setbacks = setbacks; c.column = column; c.crown = crown; c.interiorAir = interiorAir; c.balconies = balconies;
            c.sign = sign; c.light = light;
            return c;
        }
    }

    private Tower() {
    }

    public static Spec spec() {
        return new Spec();
    }

    public static int floorsFor(Spec s, int width) {
        return Math.max(3, (int) Math.round(width * s.heightFactor / 4.0));
    }

    /** Construit la tour centrée en (ox, oz) ; renvoie la hauteur du toit. */
    public static int build(VoxelGrid g, Spec s, int width, int depth, int floors, int ox, int oy, int oz) {
        Rect r = Rect.centered(Math.max(5, width), Math.max(5, depth)).translate(ox, oz);
        int fh = 4;
        int y = oy;
        Arch.floor(g, r.grow(1), y, s.lobby);
        int stage = 0;
        int nextSetback = s.setbacks > 0 ? floors * 3 / 5 : Integer.MAX_VALUE;
        for (int f = 0; f < floors; f++) {
            if (f == nextSetback && r.width() > 9 && r.depth() > 9) {
                Arch.floor(g, r, y, s.band);
                Arch.walls(g, r, y + 1, y + 1, s.frame);
                r = r.inset(2);
                stage++;
                nextSetback = stage < s.setbacks ? f + Math.max(2, (floors - f) * 2 / 3) : Integer.MAX_VALUE;
            }
            int y0 = y;
            Arch.floor(g, r, y0, f == 0 ? s.lobby : s.slab);
            Arch.walls(g, r, y0, y0, s.band);
            for (int x = r.x0(); x <= r.x1(); x++) {
                boolean col = (x - r.x0()) % s.column == 0 || x == r.x1();
                g.fill(x, y0 + 1, r.z0(), x, y0 + fh - 1, r.z0(), col ? s.frame : s.glass);
                g.fill(x, y0 + 1, r.z1(), x, y0 + fh - 1, r.z1(), col ? s.frame : s.glass);
            }
            for (int z = r.z0(); z <= r.z1(); z++) {
                boolean col = (z - r.z0()) % s.column == 0 || z == r.z1();
                g.fill(r.x0(), y0 + 1, z, r.x0(), y0 + fh - 1, z, col ? s.frame : s.glass);
                g.fill(r.x1(), y0 + 1, z, r.x1(), y0 + fh - 1, z, col ? s.frame : s.glass);
            }
            if (s.interiorAir) {
                Arch.air(g, r.inset(1), y0 + 1, y0 + fh - 1);
                g.set(r.cx(), y0 + fh - 1, r.cz() + 2, s.light);
            } else {
                g.set(r.cx(), y0 + fh - 1, r.cz(), s.light);
            }
            // Noyau (ascenseurs, escaliers) et éclairage
            g.fill(r.cx() - 1, y0 + 1, r.cz() - 1, r.cx() + 1, y0 + fh - 1, r.cz() - 1, "minecraft:iron_block");
            if (s.balconies && f > 0) {
                for (int x = r.x0() + 1; x < r.x1(); x += 4) {
                    g.fill(x, y0, r.z1() + 1, x + 1, y0, r.z1() + 1, s.slab);
                    g.fill(x, y0 + 1, r.z1() + 2, x + 1, y0 + 1, r.z1() + 2, "minecraft:glass_pane");
                    g.set(x, y0 + 1, r.z1() + 1, B.AIR);
                    g.set(x + 1, y0 + 1, r.z1() + 1, B.AIR);
                }
            }
            y += fh;
        }
        // Hall d'entrée : double porte vitrée et auvent
        int cx = Rect.centered(Math.max(5, width), Math.max(5, depth)).translate(ox, oz).cx();
        Rect base = Rect.centered(Math.max(5, width), Math.max(5, depth)).translate(ox, oz);
        g.fill(cx - 1, oy + 1, base.z1(), cx + 1, oy + 3, base.z1(), B.AIR);
        g.set(cx - 1, oy + 1, base.z1(), B.doorLower("iron", B.S));
        g.set(cx - 1, oy + 2, base.z1(), B.doorUpper("iron", B.S));
        g.set(cx + 1, oy + 1, base.z1(), B.doorLower("iron", B.S).replace("hinge=left", "hinge=right"));
        g.set(cx + 1, oy + 2, base.z1(), B.doorUpper("iron", B.S).replace("hinge=left", "hinge=right"));
        g.fill(cx - 2, oy + 4, base.z1() + 1, cx + 2, oy + 4, base.z1() + 2, s.band);
        // Toiture
        Arch.floor(g, r, y, s.band);
        Arch.walls(g, r, y + 1, y + 1, s.frame);
        int top = y + 1;
        switch (s.crown) {
            case ANTENNA -> {
                g.fill(r.cx() + 1, y + 1, r.cz() + 1, r.cx() + 2, y + 2, r.cz() + 2, "minecraft:iron_block");
                g.fill(r.cx(), y + 1, r.cz(), r.cx(), y + 1 + Math.max(6, floors / 2), r.cz(), "minecraft:iron_bars");
                g.set(r.cx(), y + 2 + Math.max(6, floors / 2), r.cz(), "minecraft:redstone_block");
                top = y + 2 + Math.max(6, floors / 2);
            }
            case HELIPAD -> {
                Rect pad = Rect.centered(Math.min(r.width() - 2, 9), Math.min(r.depth() - 2, 9)).translate(r.cx(), r.cz());
                Arch.floor(g, pad, y + 1, "minecraft:gray_concrete");
                for (int dz = -2; dz <= 2; dz++) {
                    g.set(r.cx() - 1, y + 1, r.cz() + dz, "minecraft:yellow_concrete");
                    g.set(r.cx() + 1, y + 1, r.cz() + dz, "minecraft:yellow_concrete");
                }
                g.set(r.cx(), y + 1, r.cz(), "minecraft:yellow_concrete");
            }
            case SPIRE -> {
                int h = Math.max(6, floors / 2);
                Rect cur = r.inset(1);
                for (int i = 0; i < h && cur.width() >= 1 && cur.depth() >= 1; i++) {
                    Arch.walls(g, cur, y + 1 + i, y + 1 + i, s.frame);
                    if (i % 2 == 1 && cur.width() > 2) {
                        cur = cur.inset(1);
                    }
                }
                g.fill(r.cx(), y + 1, r.cz(), r.cx(), y + h + 6, r.cz(), "minecraft:iron_bars");
                top = y + h + 6;
            }
            case DOME -> {
                int rad = Math.max(2, Math.min(r.width(), r.depth()) / 2 - 1);
                Arch.dome(g, r.cx(), y + 1, r.cz(), rad, rad, rad, s.glass, true);
                top = y + 1 + rad;
            }
            case GARDEN -> {
                Arch.floor(g, r.inset(1), y + 1, "minecraft:grass_block");
                for (int x = r.x0() + 2; x < r.x1(); x += 3) {
                    for (int z = r.z0() + 2; z < r.z1(); z += 3) {
                        g.set(x, y + 2, z, B.leaves("azalea"));
                    }
                }
            }
            default -> {
            }
        }
        if (s.sign != null) {
            int sy = y - 2;
            for (int x = r.x0() + 1; x < r.x1(); x++) {
                g.set(x, sy, r.z1() + 1, B.concrete(s.sign));
            }
        }
        return top;
    }
}
