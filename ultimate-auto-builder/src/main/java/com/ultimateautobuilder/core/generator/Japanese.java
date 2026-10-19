package com.ultimateautobuilder.core.generator;

import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.util.Rng;

/**
 * Architecture japonaise : soubassement de pierre, ossature de poteaux et de poutres, panneaux
 * shoji, coursive (engawa), toits irimoya (croupe + pignon) à avant-toits relevés, toits de
 * coursive entre les étages, intérieurs simples (tatamis, table basse, coussins, lanternes,
 * escalier). Façade et entrée côté +Z.
 */
public final class Japanese {
    private Japanese() {
    }

    public enum RoofStyle { IRIMOYA, HIP, GABLE, FLAT_MODERN }

    /** Paramètres d'un bâtiment. */
    public static final class Spec {
        public String post = "minecraft:stripped_dark_oak_log";
        public String beam = "minecraft:stripped_dark_oak_log";
        public String panel = "minecraft:dark_oak_planks";
        public String shoji = "minecraft:white_terracotta";
        public String window = "minecraft:white_stained_glass_pane";
        public String base = "minecraft:stone_bricks";
        public String baseTop = "minecraft:polished_andesite";
        public String deck = "minecraft:cherry_slab[type=bottom]";
        public String rail = "minecraft:cherry_fence";
        public String tatami = "minecraft:bamboo_mosaic";
        public String doorWood = "cherry";
        public String cushion = "pink";
        public String gableFill = "minecraft:dark_oak_planks";
        public RoofMat roof = RoofMat.of("deepslate_tile");
        public RoofStyle roofStyle = RoofStyle.IRIMOYA;
        public int floors = 1;
        public int floorHeight = 5;
        public int baseHeight = 1;
        /** Largeur de la coursive autour du rez-de-chaussée (0 = aucune). */
        public int engawa = 1;
        /** Retrait de chaque étage supérieur (pagode). */
        public int upperInset = 1;
        /** Balcon sur la coursive des étages. */
        public boolean balcony;
        /** Grandes baies vitrées (style moderne). */
        public boolean modern;
        public boolean interior = true;

        public Spec copy() {
            Spec s = new Spec();
            s.post = post;
            s.beam = beam;
            s.panel = panel;
            s.shoji = shoji;
            s.window = window;
            s.base = base;
            s.baseTop = baseTop;
            s.deck = deck;
            s.rail = rail;
            s.tatami = tatami;
            s.doorWood = doorWood;
            s.cushion = cushion;
            s.gableFill = gableFill;
            s.roof = roof;
            s.roofStyle = roofStyle;
            s.floors = floors;
            s.floorHeight = floorHeight;
            s.baseHeight = baseHeight;
            s.engawa = engawa;
            s.upperInset = upperInset;
            s.balcony = balcony;
            s.modern = modern;
            s.interior = interior;
            return s;
        }
    }

    private static String axis(String log, char a) {
        return log + "[axis=" + a + "]";
    }

    /**
     * Construit un bâtiment sur le rectangle {@code r}, sol extérieur à {@code y0}.
     *
     * @param doorFront porte au centre de la façade +Z
     * @return la hauteur du faîtage
     */
    public static int building(VoxelGrid g, Spec s, Rect r, int y0, long seed, boolean doorFront) {
        Rng rng = new Rng(seed);
        // Soubassement débordant sous la coursive, marche d'accès devant la porte.
        Rect deckRect = r.grow(s.engawa);
        int floorY = y0 + s.baseHeight;
        if (s.baseHeight > 0) {
            g.fill(deckRect.x0(), y0, deckRect.z0(), deckRect.x1(), floorY - 1, deckRect.z1(), s.base);
            for (int x = deckRect.x0(); x <= deckRect.x1(); x++) {
                for (int z = deckRect.z0(); z <= deckRect.z1(); z++) {
                    if (deckRect.onEdge(x, z)) {
                        g.set(x, floorY - 1, z, s.baseTop);
                    }
                }
            }
            if (doorFront) {
                for (int i = 0; i < s.baseHeight; i++) {
                    int z = deckRect.z1() + 1 + i;
                    for (int x = r.cx() - 1; x <= r.cx() + 1; x++) {
                        g.set(x, floorY - 1 - i, z, "minecraft:stone_brick_stairs[facing=north,half=bottom]");
                        g.fill(x, y0, z, x, floorY - 2 - i, z, s.base);
                    }
                }
            }
        }
        // Coursive : plancher de dalles autour du bâtiment, au niveau du sol intérieur.
        if (s.engawa > 0) {
            for (int x = deckRect.x0(); x <= deckRect.x1(); x++) {
                for (int z = deckRect.z0(); z <= deckRect.z1(); z++) {
                    if (!r.contains(x, z)) {
                        g.set(x, floorY, z, s.deck);
                    }
                }
            }
        }
        Rect cur = r;
        int y = floorY;
        int top = y;
        for (int f = 0; f < s.floors; f++) {
            boolean last = f == s.floors - 1;
            storey(g, s, cur, y, f == 0 && doorFront, f, rng);
            if (s.interior) {
                furnish(g, s, cur, y, f, last, rng);
            }
            int beamY = y + s.floorHeight - 1;
            if (!last) {
                Rect next = cur.inset(s.upperInset);
                if (next.width() < 5 || next.depth() < 5) {
                    next = cur;
                }
                // Toit de coursive (auvent) entre deux étages, puis balcon éventuel sur le retrait.
                skirt(g, s.roof, cur.grow(Math.max(1, s.engawa)), beamY + 1);
                if (s.balcony && next != cur) {
                    for (int x = cur.x0(); x <= cur.x1(); x++) {
                        for (int z = cur.z0(); z <= cur.z1(); z++) {
                            if (!next.contains(x, z)) {
                                g.set(x, beamY + 1, z, s.deck);
                                if (cur.onEdge(x, z)) {
                                    g.set(x, beamY + 2, z, s.rail);
                                }
                            }
                        }
                    }
                }
                cur = next;
                y = beamY + 1;
            } else {
                Rect roofRect = s.engawa > 0 && s.floors == 1 ? cur.grow(s.engawa) : cur.grow(1);
                top = roof(g, s, roofRect, beamY + 1, cur);
            }
        }
        // Poteaux de coursive sous l'avant-toit du rez-de-chaussée.
        if (s.engawa > 0 && s.floors == 1) {
            for (int[] c : new int[][]{{deckRect.x0(), deckRect.z0()}, {deckRect.x1(), deckRect.z0()}, {deckRect.x0(), deckRect.z1()},
                    {deckRect.x1(), deckRect.z1()}}) {
                g.fill(c[0], floorY + 1, c[1], c[0], floorY + s.floorHeight - 1, c[1], axis(s.post, 'y'));
            }
        }
        return top;
    }

    /** Un étage : poteaux, poutres, panneaux bas en bois, shoji ou baies, porte. */
    private static void storey(VoxelGrid g, Spec s, Rect r, int y, boolean door, int f, Rng rng) {
        int wallTop = y + s.floorHeight - 2;
        int beamY = wallTop + 1;
        g.fill(r.x0() + 1, y, r.z0() + 1, r.x1() - 1, y, r.z1() - 1, s.tatami);
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int z = r.z0(); z <= r.z1(); z++) {
                if (!r.onEdge(x, z)) {
                    continue;
                }
                boolean corner = (x == r.x0() || x == r.x1()) && (z == r.z0() || z == r.z1());
                boolean xEdge = z == r.z0() || z == r.z1();
                int along = xEdge ? x - r.x0() : z - r.z0();
                int len = xEdge ? r.width() : r.depth();
                boolean post = corner || along % 3 == 0 || along == len - 1;
                g.set(x, y, z, s.panel);
                for (int yy = y + 1; yy <= wallTop; yy++) {
                    if (post) {
                        g.set(x, yy, z, axis(s.post, 'y'));
                    } else if (s.modern) {
                        g.set(x, yy, z, "minecraft:glass_pane");
                    } else {
                        // Shoji : panneau clair, avec une rangée vitrée sur une travée sur deux.
                        boolean window = yy == y + 2 && along % 6 >= 3;
                        g.set(x, yy, z, window ? s.window : s.shoji);
                    }
                }
                g.set(x, y, z, post ? axis(s.post, 'y') : s.panel);
                g.set(x, beamY, z, corner ? axis(s.post, 'y') : axis(s.beam, xEdge ? 'x' : 'z'));
            }
        }
        // Plafond / plancher de l'étage suivant.
        g.fill(r.x0() + 1, beamY, r.z0() + 1, r.x1() - 1, beamY, r.z1() - 1, s.panel);
        if (door) {
            int cx = r.cx();
            g.set(cx, y, r.z1(), s.tatami);
            g.set(cx + 1, y, r.z1(), s.tatami);
            g.set(cx, y + 1, r.z1(), B.doorLower(s.doorWood, B.S));
            g.set(cx, y + 2, r.z1(), B.doorUpper(s.doorWood, B.S));
            // Double porte coulissante : la seconde s'ouvre en miroir.
            g.set(cx + 1, y + 1, r.z1(), B.mc(s.doorWood + "_door") + "[facing=south,half=lower,hinge=right,open=false]");
            g.set(cx + 1, y + 2, r.z1(), B.mc(s.doorWood + "_door") + "[facing=south,half=upper,hinge=right,open=false]");
            for (int yy = y + 3; yy <= wallTop; yy++) {
                g.set(cx, yy, r.z1(), s.shoji);
                g.set(cx + 1, yy, r.z1(), s.shoji);
            }
        }
    }

    /** Aménagement : table basse, coussins, lanternes, escalier vers l'étage. */
    private static void furnish(VoxelGrid g, Spec s, Rect r, int y, int f, boolean last, Rng rng) {
        Rect in = r.inset(1);
        if (in.width() < 3 || in.depth() < 3) {
            return;
        }
        int cx = in.cx(), cz = in.cz();
        g.set(cx, y + 1, cz, "minecraft:cherry_slab[type=bottom]");
        g.set(cx, y + 2, cz, "minecraft:lantern[hanging=false]");
        String carpet = B.mc(s.cushion + "_carpet");
        g.setIfEmpty(cx - 1, y + 1, cz, carpet);
        g.setIfEmpty(cx + 1, y + 1, cz, carpet);
        g.setIfEmpty(cx, y + 1, cz - 1, carpet);
        // Lanternes suspendues aux poutres.
        int ceil = y + s.floorHeight - 2;
        if (in.width() >= 5) {
            g.set(in.x0() + 1, ceil, in.z0() + 1, "minecraft:lantern[hanging=true]");
            g.set(in.x1() - 1, ceil, in.z1() - 1, "minecraft:lantern[hanging=true]");
        }
        // Coffre décoratif (barrique) et pot de fleur.
        g.setIfEmpty(in.x0(), y + 1, in.z0(), "minecraft:barrel[facing=up]");
        g.setIfEmpty(in.x1(), y + 1, in.z0(), "minecraft:potted_cherry_sapling");
        if (!last) {
            // Escalier le long du mur arrière vers l'étage suivant.
            int x = in.x0() + 1;
            for (int i = 0; i < s.floorHeight - 1 && x + i <= in.x1(); i++) {
                g.set(x + i, y + 1 + i, in.z0(), "minecraft:cherry_stairs[facing=east,half=bottom]");
                for (int yy = y + 2 + i; yy <= y + s.floorHeight; yy++) {
                    g.remove(x + i, yy, in.z0());
                }
            }
        }
    }

    /** Auvent entre deux étages : un rang d'escaliers autour du bâtiment, coins relevés. */
    public static void skirt(VoxelGrid g, RoofMat m, Rect r, int y) {
        for (int x = r.x0(); x <= r.x1(); x++) {
            g.setIfEmpty(x, y, r.z0(), m.stairs(B.S, false));
            g.setIfEmpty(x, y, r.z1(), m.stairs(B.N, false));
            g.setIfEmpty(x, y - 1, r.z0(), m.stairs(B.N, true));
            g.setIfEmpty(x, y - 1, r.z1(), m.stairs(B.S, true));
        }
        for (int z = r.z0() + 1; z < r.z1(); z++) {
            g.setIfEmpty(r.x0(), y, z, m.stairs(B.E, false));
            g.setIfEmpty(r.x1(), y, z, m.stairs(B.W, false));
            g.setIfEmpty(r.x0(), y - 1, z, m.stairs(B.W, true));
            g.setIfEmpty(r.x1(), y - 1, z, m.stairs(B.E, true));
        }
        upturn(g, m, r, y);
    }

    /** Coins relevés de l'avant-toit. */
    private static void upturn(VoxelGrid g, RoofMat m, Rect r, int y) {
        g.set(r.x0(), y + 1, r.z0(), m.slabBottom());
        g.set(r.x1(), y + 1, r.z0(), m.slabBottom());
        g.set(r.x0(), y + 1, r.z1(), m.slabBottom());
        g.set(r.x1(), y + 1, r.z1(), m.slabBottom());
    }

    private static int roof(VoxelGrid g, Spec s, Rect r, int y, Rect walls) {
        return switch (s.roofStyle) {
            case IRIMOYA -> irimoya(g, s.roof, r, y, s.gableFill, walls);
            case HIP -> {
                fascia(g, s.roof, r, y);
                upturn(g, s.roof, r, y);
                yield Arch.hip(g, r, y, s.roof, 0, false);
            }
            case GABLE -> Arch.gable(g, r, y, s.roof, 0, s.gableFill, r.width() >= r.depth(), 0);
            case FLAT_MODERN -> {
                // Toit plat à large débord, bandeau de bois.
                g.fill(r.x0() - 1, y, r.z0() - 1, r.x1() + 1, y, r.z1() + 1, s.roof.slabBottom());
                for (int x = r.x0() - 1; x <= r.x1() + 1; x++) {
                    g.set(x, y, r.z0() - 1, s.panel);
                    g.set(x, y, r.z1() + 1, s.panel);
                }
                yield y + 1;
            }
        };
    }

    /** Sous-face épaisse de l'avant-toit (escaliers renversés). */
    private static void fascia(VoxelGrid g, RoofMat m, Rect r, int y) {
        for (int x = r.x0(); x <= r.x1(); x++) {
            g.setIfEmpty(x, y - 1, r.z0(), m.stairs(B.N, true));
            g.setIfEmpty(x, y - 1, r.z1(), m.stairs(B.S, true));
        }
        for (int z = r.z0() + 1; z < r.z1(); z++) {
            g.setIfEmpty(r.x0(), y - 1, z, m.stairs(B.W, true));
            g.setIfEmpty(r.x1(), y - 1, z, m.stairs(B.E, true));
        }
    }

    /**
     * Toit irimoya : partie basse en croupe sur les quatre côtés, partie haute en pignon le long du
     * grand axe, triangles de pignon en bois, faîtage en blocs pleins et avant-toits relevés.
     */
    public static int irimoya(VoxelGrid g, RoofMat m, Rect r, int y, String gableFill, Rect walls) {
        fascia(g, m, r, y);
        upturn(g, m, r, y);
        boolean alongX = r.width() >= r.depth();
        int hipLevels = Math.max(1, Math.min(r.width(), r.depth()) / 5);
        Rect cur = r;
        int yy = y;
        for (int i = 0; i < hipLevels && cur.width() > 2 && cur.depth() > 2; i++) {
            ring(g, m, cur, yy);
            clear(g, cur.inset(1), yy);
            cur = cur.inset(1);
            yy++;
        }
        // Pignon : seuls les longs pans continuent de monter.
        while (true) {
            int a0 = alongX ? cur.z0() : cur.x0();
            int a1 = alongX ? cur.z1() : cur.x1();
            if (a1 - a0 < 0) {
                break;
            }
            if (a1 - a0 <= 1) {
                // Faîtage.
                if (alongX) {
                    g.fill(cur.x0(), yy, a0, cur.x1(), yy, a1, m.full());
                    g.fill(cur.x0(), yy + 1, a0, cur.x1(), yy + 1, a1, m.slabBottom());
                } else {
                    g.fill(a0, yy, cur.z0(), a1, yy, cur.z1(), m.full());
                    g.fill(a0, yy + 1, cur.z0(), a1, yy + 1, cur.z1(), m.slabBottom());
                }
                yy += 2;
                break;
            }
            if (alongX) {
                for (int x = cur.x0(); x <= cur.x1(); x++) {
                    g.set(x, yy, cur.z0(), m.stairs(B.S, false));
                    g.set(x, yy, cur.z1(), m.stairs(B.N, false));
                }
                for (int z = cur.z0() + 1; z < cur.z1(); z++) {
                    g.set(cur.x0(), yy, z, gableFill);
                    g.set(cur.x1(), yy, z, gableFill);
                    for (int x = cur.x0() + 1; x < cur.x1(); x++) {
                        g.remove(x, yy, z);
                    }
                }
                cur = new Rect(cur.x0(), cur.z0() + 1, cur.x1(), cur.z1() - 1);
            } else {
                for (int z = cur.z0(); z <= cur.z1(); z++) {
                    g.set(cur.x0(), yy, z, m.stairs(B.E, false));
                    g.set(cur.x1(), yy, z, m.stairs(B.W, false));
                }
                for (int x = cur.x0() + 1; x < cur.x1(); x++) {
                    g.set(x, yy, cur.z0(), gableFill);
                    g.set(x, yy, cur.z1(), gableFill);
                    for (int z = cur.z0() + 1; z < cur.z1(); z++) {
                        g.remove(x, yy, z);
                    }
                }
                cur = new Rect(cur.x0() + 1, cur.z0(), cur.x1() - 1, cur.z1());
            }
            yy++;
        }
        return yy;
    }

    private static void ring(VoxelGrid g, RoofMat m, Rect c, int y) {
        for (int x = c.x0(); x <= c.x1(); x++) {
            g.set(x, y, c.z0(), m.stairs(B.S, false));
            g.set(x, y, c.z1(), m.stairs(B.N, false));
        }
        for (int z = c.z0() + 1; z < c.z1(); z++) {
            g.set(c.x0(), y, z, m.stairs(B.E, false));
            g.set(c.x1(), y, z, m.stairs(B.W, false));
        }
    }

    private static void clear(VoxelGrid g, Rect r, int y) {
        if (r.x0() > r.x1() || r.z0() > r.z1()) {
            return;
        }
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int z = r.z0(); z <= r.z1(); z++) {
                g.remove(x, y, z);
            }
        }
    }

    // ------------------------------------------------------------------ jardin

    /** Lanterne de pierre (tōrō) : socle, fût, lanterne, chapeau. */
    public static void stoneLantern(VoxelGrid g, int x, int y, int z) {
        g.set(x, y, z, "minecraft:stone_brick_wall");
        g.set(x, y + 1, z, "minecraft:andesite_wall");
        g.set(x, y + 2, z, "minecraft:lantern[hanging=false]");
        g.set(x, y + 3, z, "minecraft:stone_brick_slab[type=bottom]");
    }

    /** Portique torii face à +Z : piliers rouges, linteaux, chapeau sombre relevé aux extrémités. */
    public static void torii(VoxelGrid g, int cx, int y, int z, int halfSpan, int height) {
        String red = "minecraft:red_terracotta";
        g.fill(cx - halfSpan, y, z, cx - halfSpan, y + height - 1, z, red);
        g.fill(cx + halfSpan, y, z, cx + halfSpan, y + height - 1, z, red);
        g.fill(cx - halfSpan, y + height - 2, z, cx + halfSpan, y + height - 2, z, red);
        g.fill(cx - halfSpan - 1, y + height, z, cx + halfSpan + 1, y + height, z, "minecraft:polished_blackstone");
        g.set(cx - halfSpan - 2, y + height, z, "minecraft:polished_blackstone_stairs[facing=east,half=top]");
        g.set(cx + halfSpan + 2, y + height, z, "minecraft:polished_blackstone_stairs[facing=west,half=top]");
        g.set(cx - halfSpan - 2, y + height + 1, z, "minecraft:polished_blackstone_slab[type=bottom]");
        g.set(cx + halfSpan + 2, y + height + 1, z, "minecraft:polished_blackstone_slab[type=bottom]");
        g.set(cx, y + height - 1, z, "minecraft:dark_oak_planks");
    }

    /** Bassin (eau au niveau du sol) bordé de pierres, nénuphars. */
    public static void pond(VoxelGrid g, int cx, int y, int cz, int rx, int rz, Rng rng) {
        for (int dx = -rx - 1; dx <= rx + 1; dx++) {
            for (int dz = -rz - 1; dz <= rz + 1; dz++) {
                double v = (double) (dx * dx) / ((rx + 0.5) * (rx + 0.5)) + (double) (dz * dz) / ((rz + 0.5) * (rz + 0.5));
                double edge = (double) (dx * dx) / ((rx + 1.5) * (rx + 1.5)) + (double) (dz * dz) / ((rz + 1.5) * (rz + 1.5));
                if (v <= 1.0) {
                    g.set(cx + dx, y, cz + dz, "minecraft:water");
                    g.remove(cx + dx, y + 1, cz + dz);
                    if (rng.nextInt(9) == 0) {
                        g.set(cx + dx, y + 1, cz + dz, "minecraft:lily_pad");
                    }
                } else if (edge <= 1.0) {
                    g.set(cx + dx, y, cz + dz, rng.nextInt(3) == 0 ? "minecraft:mossy_cobblestone" : "minecraft:cobblestone");
                }
            }
        }
    }

    /** Pont en arc le long de X au-dessus d'un bassin. */
    public static void bridge(VoxelGrid g, int x0, int x1, int y, int z) {
        int len = x1 - x0;
        for (int x = x0; x <= x1; x++) {
            int i = x - x0;
            int h = (int) Math.round(Math.sin(Math.PI * i / Math.max(1, len)) * Math.max(1, len / 4.0));
            String deck = "minecraft:cherry_slab[type=" + (h % 2 == 0 ? "bottom" : "top") + "]";
            int yy = y + 1 + h / 2;
            g.set(x, yy, z, deck);
            g.set(x, yy, z + 1, deck);
            g.set(x, yy + 1, z - 1, "minecraft:cherry_fence");
            g.set(x, yy + 1, z + 2, "minecraft:cherry_fence");
            g.set(x, yy, z - 1, "minecraft:cherry_planks");
            g.set(x, yy, z + 2, "minecraft:cherry_planks");
        }
    }

    /**
     * Cerisier en fleurs : tronc incliné, trois à quatre branches maîtresses et une couronne large
     * et arrondie faite de plusieurs masses de feuillage, pétales tombés au sol.
     */
    public static void cherryTree(VoxelGrid g, int x, int y, int z, int height, long seed) {
        Rng rng = new Rng(seed);
        int h = Math.max(6, height);
        int trunk = Math.max(3, (int) Math.round(h * 0.45));
        String log = "minecraft:cherry_log[axis=y]";
        String leaves = "minecraft:cherry_leaves[persistent=true]";
        // Tronc légèrement penché.
        int lean = rng.nextInt(4);
        int dx0 = lean == 0 ? 1 : lean == 1 ? -1 : 0, dz0 = lean == 2 ? 1 : lean == 3 ? -1 : 0;
        int tx = x, tz = z;
        for (int i = 0; i < trunk; i++) {
            if (i == trunk / 2) {
                tx += dx0;
                tz += dz0;
            }
            g.set(tx, y + i, tz, log);
        }
        int top = y + trunk - 1;
        double crown = Math.max(2.5, h * 0.42);
        int branches = 3 + rng.nextInt(2);
        double a0 = rng.nextDouble() * Math.PI * 2;
        for (int b = 0; b < branches; b++) {
            double a = a0 + b * Math.PI * 2 / branches + (rng.nextDouble() - 0.5) * 0.6;
            double len = crown * (0.55 + rng.nextDouble() * 0.25);
            double rise = h * (0.18 + rng.nextDouble() * 0.12);
            int steps = (int) Math.ceil(len);
            int ex = tx, ey = top, ez = tz;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                ex = tx + (int) Math.round(Math.cos(a) * len * t);
                ez = tz + (int) Math.round(Math.sin(a) * len * t);
                ey = top + (int) Math.round(rise * t);
                boolean horizontal = Math.abs(Math.cos(a)) > 0.5;
                g.set(ex, ey, ez, "minecraft:cherry_log[axis=" + (t < 0.4 ? "y" : horizontal ? "x" : "z") + "]");
            }
            blob(g, ex, ey + 1, ez, crown * 0.62, crown * 0.42, leaves, rng);
        }
        blob(g, tx, top + (int) Math.round(h * 0.3), tz, crown * 0.7, crown * 0.45, leaves, rng);
        // Pétales tombés autour du pied.
        int r = (int) Math.ceil(crown);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r && rng.nextInt(3) == 0 && !g.isSet(x + dx, y, z + dz) && g.isSet(x + dx, y - 1, z + dz)) {
                    g.set(x + dx, y, z + dz, "minecraft:pink_petals[facing=" + B.HORIZONTALS[rng.nextInt(4)] + ",flower_amount="
                            + (1 + rng.nextInt(4)) + "]");
                }
            }
        }
    }

    /** Masse de feuillage aplatie, bord irrégulier, sans écraser les branches. */
    private static void blob(VoxelGrid g, int cx, int cy, int cz, double rh, double rv, String leaves, Rng rng) {
        int ih = (int) Math.ceil(rh), iv = (int) Math.ceil(rv);
        for (int dx = -ih; dx <= ih; dx++) {
            for (int dy = -iv; dy <= iv; dy++) {
                for (int dz = -ih; dz <= ih; dz++) {
                    double v = (dx * dx + dz * dz) / (rh * rh) + (dy * dy) / (rv * rv);
                    // Bord dentelé : les cellules proches de la surface sont tirées au hasard.
                    if (v <= 0.75 || v <= 1.0 && rng.nextInt(3) != 0) {
                        g.setIfEmpty(cx + dx, cy + dy, cz + dz, leaves);
                    }
                }
            }
        }
    }
}
