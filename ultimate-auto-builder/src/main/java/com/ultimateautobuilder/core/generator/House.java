package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

import java.util.EnumSet;
import java.util.Set;

/**
 * Générateur architectural de maisons : fondations, étages, murs, ouvertures, escaliers,
 * toiture, intérieur meublé et annexes (porche, balcon, piscine, garage, jardin…).
 * La largeur demandée pilote l'emprise ; le nombre d'étages et les annexes suivent l'échelle.
 */
public final class House {
    public enum Feature {
        INTERIOR, CHIMNEY, PORCH, BALCONY, POOL, GARAGE, GARDEN, FENCE, STILTS, FLOATING, UNDERGROUND,
        SNOW, BIG_WINDOWS, TOWER, LANTERNS, WING, TERRACE, SOLAR, ROOMS
    }

    /** Spécification de style (valeurs par défaut : petite maison en bois). */
    public static final class Spec {
        public String wall = "minecraft:oak_planks";
        public String corner = "minecraft:oak_log[axis=y]";
        public String foundation = "minecraft:cobblestone";
        public String floor = "minecraft:spruce_planks";
        public String trim = "minecraft:stripped_oak_log[axis=x]";
        public String window = "minecraft:glass_pane";
        public String doorWood = "oak";
        public String furnitureWood = "oak";
        public String carpet = "red";
        public String bed = "red";
        public RoofMat roof = RoofMat.of("spruce");
        public RoofType roofType = RoofType.GABLE;
        public int overhang = 1;
        public int floors = 1;
        public int floorHeight = 4;
        public double depthRatio = 0.8;
        public int windowSpacing = 3;
        public final Set<Feature> features = EnumSet.of(Feature.INTERIOR);

        public Spec walls(String wall, String corner) { this.wall = B.mc(wall); this.corner = B.mc(corner); return this; }
        public Spec foundation(String v) { foundation = B.mc(v); return this; }
        public Spec floorMat(String v) { floor = B.mc(v); return this; }
        public Spec trim(String v) { trim = B.mc(v); return this; }
        public Spec window(String v) { window = B.mc(v); return this; }
        public Spec door(String wood) { doorWood = wood; return this; }
        public Spec furniture(String wood) { furnitureWood = wood; return this; }
        public Spec colors(String carpet, String bed) { this.carpet = carpet; this.bed = bed; return this; }
        public Spec roof(RoofType type, String mat) { roofType = type; roof = RoofMat.of(mat); return this; }
        public Spec overhang(int v) { overhang = v; return this; }
        public Spec floors(int v) { floors = v; return this; }
        public Spec floorHeight(int v) { floorHeight = v; return this; }
        public Spec depth(double ratio) { depthRatio = ratio; return this; }
        public Spec spacing(int v) { windowSpacing = Math.max(2, v); return this; }
        public Spec with(Feature... f) { features.addAll(java.util.List.of(f)); return this; }
        public Spec without(Feature... f) { java.util.List.of(f).forEach(features::remove); return this; }
        public boolean has(Feature f) { return features.contains(f); }
    }

    private House() {
    }

    public static Spec spec() {
        return new Spec();
    }

    public static VoxelModel generate(Spec s, int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        build(g, s, width, seed, 0, 0, 0);
        return g.freeze();
    }

    /** Nombre d'étages adapté à la largeur (référence : 15 blocs = taille normale). */
    public static int floorsFor(Spec s, int width) {
        return Math.max(1, Math.min(12, (int) Math.round(s.floors * Math.pow(width / 15.0, 0.55))));
    }

    /**
     * Construit la maison dans une grille existante (utilisé aussi par les villes et les châteaux).
     *
     * @return le rectangle occupé par le corps principal
     */
    public static Rect build(VoxelGrid g, Spec s, int width, long seed, int ox, int oy, int oz) {
        Rng rng = new Rng(seed);
        int w = Math.max(5, width);
        int d = Math.max(5, (int) Math.round(w * s.depthRatio));
        int floors = floorsFor(s, w);
        int fh = Math.max(3, s.floorHeight + (w >= 45 ? 1 : 0));
        Rect r = Rect.centered(w, d).translate(ox, oz);
        int base = oy;
        if (s.has(Feature.STILTS)) {
            base += 4;
        }
        if (s.has(Feature.UNDERGROUND)) {
            base -= floors * fh + 1;
        }
        if (s.has(Feature.FLOATING)) {
            base += 14;
            floatingRock(g, r, base - 1, rng);
        }
        // Fondations (débord d'un bloc) et pilotis
        g.fill(r.x0() - 1, base - 1, r.z0() - 1, r.x1() + 1, base - 1, r.z1() + 1, s.foundation);
        if (s.has(Feature.STILTS)) {
            for (int x = r.x0() - 1; x <= r.x1() + 1; x += Math.max(3, (w + 1) / 4)) {
                for (int z = r.z0() - 1; z <= r.z1() + 1; z += Math.max(3, (d + 1) / 3)) {
                    g.fill(x, oy - 2, z, x, base - 2, z, s.corner);
                }
            }
            for (int i = 0; base - 1 - i >= oy; i++) {
                g.set(r.cx(), base - 1 - i, r.z1() + 2 + i, s.roof.stairs(B.N, false));
            }
        }
        int top = base;
        for (int f = 0; f < floors; f++) {
            int y0 = base + f * fh;
            Arch.floor(g, r, y0, f == 0 ? s.foundation : s.floor);
            Arch.floor(g, r.inset(1), y0, s.floor);
            Arch.walls(g, r, y0 + 1, y0 + fh - 1, s.wall);
            Arch.air(g, r.inset(1), y0 + 1, y0 + fh - 1);
            Arch.pillars(g, r, y0, y0 + fh - 1, s.corner);
            if (f > 0) {
                Arch.walls(g, r, y0, y0, s.trim);
                Arch.pillars(g, r, y0, y0, s.corner);
            }
            if (s.has(Feature.BIG_WINDOWS)) {
                bigWindows(g, r, y0 + 1, fh - 1, s.window, f == 0 ? r.cx() : null);
            } else {
                int wh = fh >= 4 ? 2 : 1;
                Arch.windows(g, r, y0 + 2, wh, s.windowSpacing, s.window, f == 0 ? r.cx() : null);
            }
            top = y0 + fh;
        }
        // Plafond du dernier étage (support de la toiture)
        Arch.floor(g, r, top, s.floor);
        Arch.walls(g, r, top, top, s.trim);
        // Porte d'entrée et éclairage
        Arch.door(g, r.cx(), base + 1, r.z1(), s.doorWood, B.S);
        g.set(r.cx(), base, r.z1() + 1, s.has(Feature.UNDERGROUND) ? s.floor : s.roof.slabBottom().replace("[type=bottom]", "[type=top]"));
        g.fill(r.cx() - 1, base + 1, r.z1() + 1, r.cx() + 1, base + 3, r.z1() + 1, B.AIR);
        g.set(r.cx() - 1, base + 2, r.z1() + 1, B.wallTorch(B.S));
        g.set(r.cx() + 1, base + 2, r.z1() + 1, B.wallTorch(B.S));
        // Escaliers ou échelle entre étages
        for (int f = 0; f + 1 < floors; f++) {
            stairs(g, r, base + f * fh, fh, s, w);
        }
        // Intérieur
        if (s.has(Feature.INTERIOR)) {
            for (int f = 0; f < floors; f++) {
                furnish(g, r, base + f * fh, fh, f, floors, s, rng, w >= 21 && s.has(Feature.ROOMS));
            }
        }
        // Toiture
        int ridge = top;
        if (s.has(Feature.UNDERGROUND)) {
            Arch.green(g, r.grow(1), top);
            g.fill(r.cx(), top, r.z1() + 1, r.cx(), oy + 1, r.z1() + 1, B.AIR);
            for (int y = base + 1; y <= oy; y++) {
                int z = r.z1() + 1 + (y - base);
                g.set(r.cx(), y, z, s.roof.stairs(B.S, false));
                g.fill(r.cx(), y + 1, z, r.cx(), oy + 2, z, B.AIR);
                g.set(r.cx() - 1, y, z, s.foundation);
                g.set(r.cx() + 1, y, z, s.foundation);
            }
        } else {
            ridge = Arch.roof(g, s.roofType, r, top + 1, s.roof, s.overhang, s.wall);
            if (s.has(Feature.SNOW)) {
                snowRoof(g, r.grow(s.overhang), top + 1, ridge + 1);
            }
        }
        if (s.has(Feature.CHIMNEY)) {
            int cx = r.x1() - 1, cz = r.z0() + 1;
            g.fill(cx, base + 1, cz, cx, ridge + 2, cz, "minecraft:bricks");
            g.set(cx, ridge + 3, cz, "minecraft:campfire[lit=true,signal_fire=false,facing=north]");
            g.set(cx, base + 1, cz - 0, "minecraft:bricks");
        }
        if (s.has(Feature.SOLAR) && (s.roofType == RoofType.FLAT || s.roofType == RoofType.GREEN)) {
            for (int x = r.x0() + 2; x < r.x1() - 1; x += 3) {
                for (int z = r.z0() + 2; z < r.z1() - 1; z += 2) {
                    g.set(x, top + 2, z, "minecraft:daylight_detector[inverted=false,power=0]");
                    g.set(x + 1, top + 2, z, "minecraft:daylight_detector[inverted=false,power=0]");
                }
            }
        }
        if (s.has(Feature.TOWER)) {
            int tr = Math.max(2, w / 6);
            int tx = r.x1(), tz = r.z1();
            int th = floors * fh + fh;
            Arch.cylinder(g, tx, base, tz, tr, th, s.wall, s.floor, fh);
            Arch.cone(g, tx, base + th, tz, tr + 1, tr * 2 + 2, s.roof.full());
            g.set(tx, base + 2, tz + tr, s.window);
            g.set(tx, base + 2 + fh, tz + tr, s.window);
        }
        if (s.has(Feature.WING)) {
            Spec ws = copyForWing(s);
            int ww = Math.max(5, w * 2 / 3);
            int wd = Math.max(5, d * 2 / 3);
            Rect wr = new Rect(r.x0() - ww, r.z1() - wd + 1, r.x0(), r.z1());
            for (int f = 0; f < Math.max(1, floors - 1); f++) {
                int y0 = base + f * fh;
                Arch.floor(g, wr, y0, ws.floor);
                Arch.walls(g, wr, y0 + 1, y0 + fh - 1, ws.wall);
                Arch.air(g, wr.inset(1), y0 + 1, y0 + fh - 1);
                Arch.pillars(g, wr, y0, y0 + fh - 1, ws.corner);
                Arch.windows(g, wr, y0 + 2, 2, ws.windowSpacing, ws.window, null);
            }
            int wtop = base + Math.max(1, floors - 1) * fh;
            Arch.floor(g, wr, wtop, ws.floor);
            g.fill(r.x0(), base + 1, wr.z0() + 1, r.x0(), base + 2, wr.z1() - 1, B.AIR);
            Arch.roof(g, s.roofType == RoofType.FLAT ? RoofType.FLAT : RoofType.GABLE_Z, wr, wtop + 1, s.roof, s.overhang, s.wall);
        }
        // Annexes extérieures
        if (s.has(Feature.PORCH)) {
            porch(g, r, base, s);
        }
        if (s.has(Feature.BALCONY) && floors > 1) {
            balcony(g, r, base + fh, s);
        }
        if (s.has(Feature.TERRACE)) {
            Rect t = new Rect(r.x0(), r.z1() + 1, r.x1(), r.z1() + Math.max(3, d / 3));
            Arch.floor(g, t, base, s.floor);
            Arch.air(g, t, base + 1, base + 3);
            for (int x = t.x0(); x <= t.x1(); x++) {
                g.set(x, base + 1, t.z1(), "minecraft:glass_pane");
            }
            g.set(t.x0() + 1, base + 1, t.z1() - 1, "minecraft:potted_azalea_bush");
            g.set(t.x1() - 1, base + 1, t.z1() - 1, "minecraft:potted_azalea_bush");
        }
        if (s.has(Feature.GARAGE)) {
            garage(g, r, base, s, w);
        }
        if (s.has(Feature.POOL)) {
            pool(g, r, oy, w, d);
        }
        if (s.has(Feature.GARDEN)) {
            garden(g, r, oy, rng, s);
        }
        if (s.has(Feature.FENCE)) {
            Rect fr = r.grow(s.has(Feature.GARDEN) ? 4 : 2);
            for (int x = fr.x0(); x <= fr.x1(); x++) {
                for (int z = fr.z0(); z <= fr.z1(); z++) {
                    if (fr.onEdge(x, z) && !(z == fr.z1() && Math.abs(x - r.cx()) <= 1) && g.get(x, oy + 1, z) == null) {
                        g.set(x, oy + 1, z, "minecraft:" + s.furnitureWood + "_fence");
                    }
                }
            }
        }
        if (s.has(Feature.LANTERNS)) {
            for (int z = r.z1() + 3; z <= r.z1() + 6; z += 3) {
                g.set(r.cx() - 2, oy + 1, z, "minecraft:" + s.furnitureWood + "_fence");
                g.set(r.cx() - 2, oy + 2, z, B.lantern(false));
                g.set(r.cx() + 2, oy + 1, z, "minecraft:" + s.furnitureWood + "_fence");
                g.set(r.cx() + 2, oy + 2, z, B.lantern(false));
            }
        }
        return r;
    }

    private static Spec copyForWing(Spec s) {
        return s;
    }

    // ------------------------------------------------------------------ éléments

    private static void bigWindows(VoxelGrid g, Rect r, int y0, int h, String glass, Integer doorX) {
        for (int x = r.x0() + 1; x < r.x1(); x++) {
            if (((x - r.x0()) % 4) == 0) {
                continue;
            }
            g.fill(x, y0, r.z0(), x, y0 + h - 1, r.z0(), glass);
            if (doorX == null || Math.abs(x - doorX) > 1) {
                g.fill(x, y0, r.z1(), x, y0 + h - 1, r.z1(), glass);
            }
        }
        for (int z = r.z0() + 1; z < r.z1(); z++) {
            if (((z - r.z0()) % 4) == 0) {
                continue;
            }
            g.fill(r.x0(), y0, z, r.x0(), y0 + h - 1, z, glass);
            g.fill(r.x1(), y0, z, r.x1(), y0 + h - 1, z, glass);
        }
    }

    /** Escalier droit le long du mur arrière, ou échelle si la maison est trop étroite. */
    private static void stairs(VoxelGrid g, Rect r, int y0, int fh, Spec s, int w) {
        int x0 = r.x0() + 1;
        int z = r.z0() + 1;
        if (w < 9 || r.width() - 2 < fh + 1) {
            for (int y = y0 + 1; y <= y0 + fh; y++) {
                g.set(x0, y, z, "minecraft:ladder[facing=south]");
            }
            g.set(x0, y0 + fh + 1, z, B.AIR);
            return;
        }
        String mat = s.furnitureWood;
        for (int i = 0; i < fh; i++) {
            int x = x0 + i;
            g.set(x, y0 + 1 + i, z, B.stairs(mat, B.E, false));
            g.fill(x, y0 + 2 + i, z, x, y0 + fh + 1, z, B.AIR);
            if (i > 0) {
                g.set(x - 1, y0 + i, z, B.planks(mat));
            }
        }
        // Trémie de l'étage supérieur
        g.fill(x0, y0 + fh, z, x0 + fh - 1, y0 + fh, z, B.AIR);
        g.set(x0 + fh - 1, y0 + fh, z, B.stairs(mat, B.E, false));
    }

    /** Meubles le long des murs : chaque étage reçoit un usage différent. */
    private static void furnish(VoxelGrid g, Rect r, int y0, int fh, int f, int floors, Spec s, Rng rng, boolean rooms) {
        Rect in = r.inset(1);
        int y = y0 + 1;
        String wood = s.furnitureWood;
        // Plafonnier
        g.set(in.cx(), y0 + fh - 1, in.cz(), B.lantern(true));
        if (in.width() >= 5 && in.depth() >= 5) {
            g.set(in.cx(), y, in.cz(), B.carpet(s.carpet));
        }
        if (rooms && in.width() >= 13) {
            // Cloison centrale avec passage
            int wx = in.cx();
            g.fill(wx, y, in.z0(), wx, y0 + fh - 1, in.z1(), s.wall);
            g.fill(wx, y, in.cz() - 1, wx, y + 1, in.cz() + 1, B.AIR);
            g.set(wx - 2, y0 + fh - 1, in.cz(), B.lantern(true));
            g.set(wx + 2, y0 + fh - 1, in.cz(), B.lantern(true));
        }
        boolean ground = f == 0;
        boolean topFloor = f == floors - 1;
        int x1 = in.x1();
        int z1 = in.z1();
        if (ground) {
            // Cuisine dans l'angle arrière droit
            g.set(x1, y, in.z0(), "minecraft:furnace[facing=west,lit=false]");
            if (in.depth() > 3) {
                g.set(x1, y, in.z0() + 1, "minecraft:crafting_table");
            }
            if (in.depth() > 4) {
                g.set(x1, y, in.z0() + 2, "minecraft:smoker[facing=west,lit=false]");
            }
            g.set(x1, y + 1, in.z0(), "minecraft:" + wood + "_trapdoor[facing=west,half=top,open=false]");
            // Table et chaises
            if (in.width() >= 6 && in.depth() >= 5) {
                int tx = in.x0() + 1 + (in.width() >= 9 ? 1 : 0), tz = z1 - 1;
                g.set(tx, y, tz, "minecraft:" + wood + "_fence");
                g.set(tx, y + 1, tz, "minecraft:" + wood + "_pressure_plate");
                g.set(tx + 1, y, tz, B.stairs(wood, B.E, false));
                if (tx - 1 >= in.x0()) {
                    g.set(tx - 1, y, tz, B.stairs(wood, B.W, false));
                }
            }
            g.set(in.x0(), y, z1, "minecraft:barrel[facing=up,open=false]");
            g.set(x1, y, z1, "minecraft:potted_red_tulip");
        } else {
            // Chambre : lit, coffre, bibliothèque
            g.set(x1, y, z1, B.bedHead(s.bed, B.S));
            if (z1 - 1 >= in.z0()) {
                g.set(x1, y, z1 - 1, B.bedFoot(s.bed, B.S));
            }
            if (x1 - 1 > in.x0()) {
                g.set(x1 - 1, y, z1, "minecraft:chest[facing=north,type=single]");
            }
            for (int z = in.z0() + 1; z <= Math.min(in.z0() + 3, z1 - 2); z++) {
                g.set(x1, y, z, "minecraft:bookshelf");
                g.set(x1, y + 1, z, "minecraft:bookshelf");
            }
            if (topFloor) {
                g.set(in.x0(), y, z1, "minecraft:potted_fern");
            }
            g.set(in.x0(), y, in.cz(), "minecraft:" + wood + "_trapdoor[facing=east,half=bottom,open=false]");
        }
        if (rng.chance(0.5) && in.width() > 8) {
            g.set(in.cx() + 2, y, z1, "minecraft:jukebox");
        }
    }

    private static void porch(VoxelGrid g, Rect r, int base, Spec s) {
        int depth = Math.max(2, r.depth() / 4);
        Rect p = new Rect(r.x0(), r.z1() + 1, r.x1(), r.z1() + depth);
        Arch.floor(g, p, base, s.floor);
        Arch.air(g, p, base + 1, base + 3);
        for (int x = p.x0(); x <= p.x1(); x += Math.max(2, (p.width() - 1) / 3)) {
            g.fill(x, base + 1, p.z1(), x, base + 3, p.z1(), "minecraft:" + s.furnitureWood + "_fence");
        }
        g.fill(p.x1(), base + 1, p.z1(), p.x1(), base + 3, p.z1(), "minecraft:" + s.furnitureWood + "_fence");
        for (int x = p.x0(); x <= p.x1(); x++) {
            for (int z = p.z0(); z <= p.z1(); z++) {
                g.set(x, base + 4, z, s.roof.slabBottom());
            }
        }
        g.set(r.cx(), base, p.z1() + 1, s.roof.stairs(B.N, false));
        g.set(p.x0() + 1, base + 1, p.z1() - 1, B.stairs(s.furnitureWood, B.N, false));
    }

    private static void balcony(VoxelGrid g, Rect r, int y, Spec s) {
        Rect b = new Rect(r.cx() - 2, r.z1() + 1, r.cx() + 2, r.z1() + 2);
        Arch.floor(g, b, y, s.floor);
        for (int x = b.x0(); x <= b.x1(); x++) {
            g.set(x, y + 1, b.z1(), "minecraft:" + s.furnitureWood + "_fence");
        }
        g.set(b.x0(), y + 1, b.z0(), "minecraft:" + s.furnitureWood + "_fence");
        g.set(b.x1(), y + 1, b.z0(), "minecraft:" + s.furnitureWood + "_fence");
        g.fill(r.cx(), y + 1, r.z1(), r.cx(), y + 2, r.z1(), B.AIR);
        Arch.door(g, r.cx(), y + 1, r.z1(), s.doorWood, B.S);
    }

    private static void garage(VoxelGrid g, Rect r, int base, Spec s, int w) {
        int gw = Math.max(5, Math.min(9, w / 2 + 1));
        Rect gr = new Rect(r.x1(), r.z1() - Math.max(5, Math.min(r.depth(), 9)) + 1, r.x1() + gw, r.z1());
        Arch.floor(g, gr, base, "minecraft:smooth_stone");
        Arch.walls(g, gr, base + 1, base + 4, s.wall);
        Arch.air(g, gr.inset(1), base + 1, base + 4);
        Arch.floor(g, gr, base + 5, s.roof.full());
        int doorX0 = gr.x0() + 2, doorX1 = gr.x1() - 1;
        g.fill(doorX0, base + 1, gr.z1(), doorX1, base + 3, gr.z1(), "minecraft:white_concrete");
        g.fill(doorX0, base + 3, gr.z1(), doorX1, base + 3, gr.z1(), "minecraft:light_gray_concrete");
        g.fill(gr.x0(), base + 1, gr.z0() + 1, gr.x0(), base + 2, gr.z0() + 1, B.AIR);
        g.set(gr.x0() + 1, base + 1, gr.z0() + 1, "minecraft:anvil[facing=north]");
        g.set(gr.x1() - 1, base + 1, gr.z0() + 1, "minecraft:chest[facing=south,type=single]");
        g.fill(doorX0, base, gr.z1() + 1, doorX1, base, gr.z1() + 6, "minecraft:gray_concrete");
    }

    private static void pool(VoxelGrid g, Rect r, int oy, int w, int d) {
        int pw = Math.max(5, w * 2 / 3);
        int pd = Math.max(4, d / 2);
        Rect deck = new Rect(r.cx() - pw / 2 - 1, r.z1() + 3, r.cx() - pw / 2 + pw, r.z1() + 4 + pd);
        Rect water = deck.inset(1);
        Arch.floor(g, deck, oy, "minecraft:smooth_quartz");
        g.fill(deck.x0(), oy - 3, deck.z0(), deck.x1(), oy - 1, deck.z1(), "minecraft:prismarine_bricks");
        g.fill(water.x0(), oy - 2, water.z0(), water.x1(), oy, water.z1(), "minecraft:water[level=0]");
        g.fill(water.x0(), oy - 3, water.z0(), water.x1(), oy - 3, water.z1(), "minecraft:sea_lantern");
        Arch.air(g, deck, oy + 1, oy + 3);
        g.set(deck.x0(), oy + 1, deck.z1(), "minecraft:white_carpet");
        g.set(deck.x1(), oy + 1, deck.z1(), "minecraft:white_carpet");
    }

    private static void garden(VoxelGrid g, Rect r, int oy, Rng rng, Spec s) {
        Rect gr = r.grow(4);
        String[] flowers = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:oxeye_daisy",
                "minecraft:azure_bluet", "minecraft:allium", "minecraft:red_tulip", "minecraft:lily_of_the_valley"};
        for (int x = gr.x0(); x <= gr.x1(); x++) {
            for (int z = gr.z0(); z <= gr.z1(); z++) {
                if (g.isSet(x, oy, z) || r.grow(1).contains(x, z)) {
                    continue;
                }
                boolean path = Math.abs(x - r.cx()) <= 0 && z > r.z1();
                g.set(x, oy, z, path ? "minecraft:dirt_path" : "minecraft:grass_block");
                if (!path && !g.isSet(x, oy + 1, z)) {
                    if (gr.inset(1).onEdge(x, z) && rng.chance(0.6)) {
                        g.set(x, oy + 1, z, B.leaves("oak"));
                    } else if (rng.chance(0.12)) {
                        g.set(x, oy + 1, z, flowers[rng.nextInt(flowers.length)]);
                    }
                }
            }
        }
    }

    private static void snowRoof(VoxelGrid g, Rect area, int y0, int y1) {
        for (int x = area.x0(); x <= area.x1(); x++) {
            for (int z = area.z0(); z <= area.z1(); z++) {
                for (int y = y1; y >= y0; y--) {
                    String st = g.get(x, y, z);
                    if (st == null || B.isAir(st)) {
                        continue;
                    }
                    if (st.contains("_slab[type=bottom]") || st.contains("_planks") || st.endsWith("_bricks") || st.endsWith("_tiles")) {
                        g.set(x, y, z, "minecraft:snow_block");
                    }
                    break;
                }
            }
        }
    }

    /** Socle rocheux en cône inversé sous une maison flottante. */
    private static void floatingRock(VoxelGrid g, Rect r, int top, Rng rng) {
        int rx = r.width() / 2 + 2, rz = r.depth() / 2 + 2;
        int depth = Math.max(5, Math.max(rx, rz));
        for (int dy = 0; dy <= depth; dy++) {
            double k = 1.0 - (double) dy / (depth + 1);
            for (int dx = -rx; dx <= rx; dx++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    double v = (double) dx * dx / (rx * rx) + (double) dz * dz / (rz * rz);
                    if (v <= k * k * (0.85 + rng.nextDouble() * 0.15)) {
                        String m = dy == 0 ? "minecraft:grass_block" : dy < 3 ? "minecraft:dirt" : "minecraft:stone";
                        g.set(r.cx() + dx, top - dy, r.cz() + dz, m);
                    }
                }
            }
        }
    }
}
