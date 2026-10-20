package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Japanese;
import com.ultimateautobuilder.core.generator.Japanese.RoofStyle;
import com.ultimateautobuilder.core.generator.RoofMat;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

import java.util.List;

/**
 * Collection Sakura : vingt maisons japonaises entourées de cerisiers, chacune avec sa propre
 * composition (nombre d'étages, toiture, coursive, jardin, bassin, portique, serre…). Le terrain
 * (herbe) fait partie du modèle : y = 0 correspond au sol, la façade et l'entrée sont côté +Z.
 */
public final class SakuraModels {
    private static final SizeSpec SMALL = SizeSpec.width(15, 21, 31, 45);
    private static final SizeSpec MEDIUM = SizeSpec.width(19, 27, 39, 55);
    private static final SizeSpec LARGE = SizeSpec.width(27, 37, 51, 71);

    private static final String GRASS = "minecraft:grass_block[snowy=false]";
    private static final String PATH = "minecraft:dirt_path";

    private SakuraModels() {
    }

    @FunctionalInterface
    private interface Layout {
        void build(VoxelGrid g, Site site, Rng rng);
    }

    /** Terrain du modèle : rectangle d'herbe, avec la largeur demandée. */
    private record Site(int w, int d, Rect r) {
        int cx() {
            return r.cx();
        }

        int cz() {
            return r.cz();
        }
    }

    private static VoxelModel make(int width, double depthRatio, long seed, Layout layout) {
        int w = Math.max(13, width);
        int d = Math.max(13, (int) Math.round(w * depthRatio));
        Site site = new Site(w, d, Rect.centered(w, d));
        VoxelGrid g = new VoxelGrid();
        g.fill(site.r().x0(), 0, site.r().z0(), site.r().x1(), 0, site.r().z1(), GRASS);
        layout.build(g, site, new Rng(seed));
        return g.freeze().groundAtBottom();
    }

    private static Japanese.Spec classic() {
        return new Japanese.Spec();
    }

    private static Japanese.Spec pinkWhite() {
        Japanese.Spec s = new Japanese.Spec();
        s.post = "minecraft:stripped_cherry_log";
        s.beam = "minecraft:stripped_cherry_log";
        s.panel = "minecraft:cherry_planks";
        s.shoji = "minecraft:white_concrete";
        s.roof = RoofMat.of("cherry");
        s.gableFill = "minecraft:cherry_planks";
        s.base = "minecraft:smooth_quartz";
        s.baseTop = "minecraft:quartz_bricks";
        s.cushion = "white";
        return s;
    }

    private static Japanese.Spec wood() {
        Japanese.Spec s = new Japanese.Spec();
        s.post = "minecraft:spruce_log";
        s.beam = "minecraft:stripped_spruce_log";
        s.panel = "minecraft:spruce_planks";
        s.shoji = "minecraft:birch_planks";
        s.roof = RoofMat.of("dark_oak");
        s.gableFill = "minecraft:spruce_planks";
        s.deck = "minecraft:spruce_slab[type=bottom]";
        s.rail = "minecraft:spruce_fence";
        s.doorWood = "spruce";
        s.cushion = "red";
        return s;
    }

    /** Allée de terre battue de la porte jusqu'au bord avant. */
    private static void frontPath(VoxelGrid g, Site s, int fromZ, int width, String mat) {
        for (int z = fromZ; z <= s.r().z1(); z++) {
            for (int x = s.cx() - width / 2; x <= s.cx() + width / 2; x++) {
                g.set(x, 0, z, mat);
            }
        }
    }

    /** Pas japonais (pierres espacées) sur l'allée. */
    private static void steppingStones(VoxelGrid g, Site s, int fromZ) {
        for (int z = fromZ; z <= s.r().z1(); z += 2) {
            g.set(s.cx(), 0, z, "minecraft:stone_bricks");
            g.set(s.cx() + 1, 0, z, "minecraft:stone_bricks");
        }
    }

    private static void flowers(VoxelGrid g, int x0, int z0, int x1, int z1, Rng rng) {
        String[] f = {"minecraft:pink_tulip", "minecraft:white_tulip", "minecraft:allium", "minecraft:lily_of_the_valley", "minecraft:azure_bluet"};
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!g.isSet(x, 1, z) && rng.nextInt(3) != 0) {
                    g.set(x, 1, z, f[rng.nextInt(f.length)]);
                }
            }
        }
    }

    private static void shrub(VoxelGrid g, int x, int z, Rng rng) {
        String leaves = rng.nextInt(2) == 0 ? "minecraft:flowering_azalea_leaves[persistent=true]" : "minecraft:azalea_leaves[persistent=true]";
        g.setIfEmpty(x, 1, z, leaves);
        if (rng.nextInt(2) == 0) {
            g.setIfEmpty(x + 1, 1, z, leaves);
        }
    }

    /** Clôture de bambou sur le pourtour, ouverture devant l'allée. */
    private static void bambooFence(VoxelGrid g, Site s, int gateHalf) {
        Rect r = s.r();
        for (int x = r.x0(); x <= r.x1(); x++) {
            for (int z = r.z0(); z <= r.z1(); z++) {
                if (r.onEdge(x, z) && !(z == r.z1() && Math.abs(x - s.cx()) <= gateHalf)) {
                    g.set(x, 1, z, "minecraft:bamboo_fence");
                }
            }
        }
    }

    /** Rangée de cerisiers sur les côtés du terrain. */
    private static void sideTrees(VoxelGrid g, Site s, int count, int height, Rng rng) {
        for (int i = 0; i < count; i++) {
            int z = s.r().z0() + 3 + i * Math.max(4, (s.d() - 6) / Math.max(1, count));
            Japanese.cherryTree(g, s.r().x0() + 2, 1, Math.min(z, s.r().z1() - 3), height, rng.nextLong());
            Japanese.cherryTree(g, s.r().x1() - 2, 1, Math.min(z + 2, s.r().z1() - 3), height, rng.nextLong());
        }
    }

    /** Jardin sec (karesansui) : gravier ratissé et rochers moussus. */
    private static void rockGarden(VoxelGrid g, int x0, int z0, int x1, int z1, Rng rng) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                g.set(x, 0, z, "minecraft:gravel");
                g.remove(x, 1, z);
            }
        }
        for (int i = 0; i < Math.max(2, (x1 - x0) * (z1 - z0) / 12); i++) {
            int x = x0 + 1 + rng.nextInt(Math.max(1, x1 - x0 - 1));
            int z = z0 + 1 + rng.nextInt(Math.max(1, z1 - z0 - 1));
            g.set(x, 1, z, rng.nextInt(2) == 0 ? "minecraft:mossy_cobblestone" : "minecraft:andesite");
            if (rng.nextInt(3) == 0) {
                g.set(x + 1, 1, z, "minecraft:moss_carpet");
            }
        }
    }

    // ------------------------------------------------------------------ les vingt maisons

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.SAKURA);
        r.gen("sakura_petite_maison", "Petite maison japonaise sous les cerisiers", 2, SMALL, p -> make(p.size(), 0.95, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered(s.w() / 2 | 1, Math.max(5, s.d() / 3)).translate(0, -s.d() / 8);
            Japanese.Spec js = classic();
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 3, PATH);
            Japanese.cherryTree(g, h.x0() - 3, 1, h.z1() + 3, Math.max(7, s.w() / 3), rng.nextLong());
            Japanese.cherryTree(g, h.x1() + 3, 1, h.z0() - 1, Math.max(7, s.w() / 3), rng.nextLong());
            Japanese.stoneLantern(g, s.cx() + 3, 1, h.z1() + 4);
        }), "sakura", "japonaise", "petite", "depart", "cerisier", "maison");
        r.gen("sakura_minka_tradition", "Minka traditionnelle au toit de bois sombre", 3, MEDIUM, p -> make(p.size(), 0.9, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.6) | 1, Math.max(7, s.d() / 2)).translate(0, -s.d() / 10);
            Japanese.Spec js = wood();
            js.roofStyle = RoofStyle.GABLE;
            js.roof = RoofMat.of("dark_oak");
            js.floorHeight = 6;
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 3, "minecraft:coarse_dirt");
            Japanese.cherryTree(g, h.x1() + 3, 1, h.z1() + 3, Math.max(8, s.w() / 3), rng.nextLong());
            for (int x = s.r().x0() + 1; x < s.cx() - 2; x += 2) {
                g.set(x, 1, s.r().z1() - 1, "minecraft:hay_block");
            }
        }), "sakura", "japonaise", "traditionnelle", "minka", "bois", "maison");
        r.gen("sakura_rose_blanche", "Maison Sakura rose et blanche", 3, MEDIUM, p -> make(p.size(), 0.95, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.55) | 1, Math.max(7, s.d() / 2 - 1)).translate(0, -s.d() / 10);
            Japanese.Spec js = pinkWhite();
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            steppingStones(g, s, h.z1() + 3);
            sideTrees(g, s, 2, Math.max(8, s.w() / 3), rng);
            flowers(g, s.cx() + 3, h.z1() + 3, s.r().x1() - 4, s.r().z1() - 2, rng);
        }), "sakura", "rose", "blanche", "cerisier", "maison");
        r.gen("sakura_jardin_bois", "Maison en bois et jardin japonais", 3, MEDIUM, p -> make(p.size(), 1.0, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.5) | 1, Math.max(7, s.d() / 2 - 2)).translate(-s.w() / 8, -s.d() / 6);
            Japanese.building(g, wood(), h, 1, rng.nextLong(), true);
            Japanese.pond(g, h.x1() + 4, 0, h.z1() + 4, Math.max(2, s.w() / 9), Math.max(2, s.d() / 10), rng);
            rockGarden(g, s.r().x0() + 1, h.z1() + 3, h.cx() - 2, s.r().z1() - 1, rng);
            Japanese.cherryTree(g, s.r().x1() - 3, 1, s.r().z0() + 3, Math.max(8, s.w() / 3), rng.nextLong());
            Japanese.stoneLantern(g, h.x1() + 2, 1, h.z1() + 2);
        }), "sakura", "bois", "jardin", "japonais", "bassin", "maison");
        r.gen("sakura_terrasse_vue", "Maison à terrasse face aux cerisiers", 3, MEDIUM, p -> make(p.size(), 0.9, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.45) | 1, Math.max(7, s.d() / 2 - 1)).translate(-s.w() / 6, -s.d() / 8);
            Japanese.Spec js = classic();
            js.engawa = 2;
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            // Terrasse en bois sur le côté, garde-corps, vue sur deux cerisiers.
            int tx0 = h.x1() + 3, tx1 = Math.min(s.r().x1() - 2, h.x1() + 3 + s.w() / 4);
            g.fill(tx0, 2, h.z0(), tx1, 2, h.z1(), "minecraft:cherry_slab[type=bottom]");
            g.fill(tx0, 1, h.z0(), tx1, 1, h.z1(), "minecraft:stone_bricks");
            for (int x = tx0; x <= tx1; x++) {
                g.set(x, 3, h.z0(), "minecraft:cherry_fence");
                g.set(x, 3, h.z1(), "minecraft:cherry_fence");
            }
            for (int z = h.z0(); z <= h.z1(); z++) {
                g.set(tx1, 3, z, "minecraft:cherry_fence");
            }
            g.set((tx0 + tx1) / 2, 3, (h.z0() + h.z1()) / 2, "minecraft:cherry_slab[type=bottom]");
            Japanese.cherryTree(g, tx1 + 2, 1, h.z1() + 4, Math.max(8, s.w() / 3), rng.nextLong());
            Japanese.cherryTree(g, s.r().x0() + 2, 1, s.r().z1() - 3, Math.max(7, s.w() / 4), rng.nextLong());
            frontPath(g, s, h.z1() + 3, 3, PATH);
        }), "sakura", "terrasse", "vue", "cerisier", "maison");
        r.gen("sakura_villa_moderne", "Villa japonaise moderne", 4, MEDIUM, p -> make(p.size(), 0.9, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.6) | 1, Math.max(7, s.d() / 2)).translate(0, -s.d() / 8);
            Japanese.Spec js = pinkWhite();
            js.modern = true;
            js.roofStyle = RoofStyle.FLAT_MODERN;
            js.roof = RoofMat.of("smooth_quartz");
            js.floors = 2;
            js.upperInset = 2;
            js.balcony = true;
            js.rail = "minecraft:glass_pane";
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            // Bassin miroir rectangulaire devant la façade.
            g.fill(h.x0() + 1, 0, h.z1() + 3, h.cx() - 2, 0, h.z1() + 5, "minecraft:water");
            g.fill(h.cx() + 2, 0, h.z1() + 3, h.x1() - 1, 0, h.z1() + 5, "minecraft:smooth_stone");
            frontPath(g, s, h.z1() + 2, 1, "minecraft:smooth_stone");
            Japanese.cherryTree(g, s.r().x1() - 3, 1, s.r().z1() - 3, Math.max(8, s.w() / 3), rng.nextLong());
        }), "sakura", "moderne", "villa", "japonaise", "verre", "maison");
        r.gen("sakura_cour_interieure", "Maison à cour intérieure et cerisier", 4, LARGE, p -> make(p.size(), 1.0, p.seed(), (g, s, rng) -> {
            int wing = Math.max(5, s.w() / 6);
            Rect back = new Rect(s.r().x0() + 3, s.r().z0() + 3, s.r().x1() - 3, s.r().z0() + 3 + wing - 1);
            Rect left = new Rect(s.r().x0() + 3, back.z1() + 2, s.r().x0() + 3 + wing - 1, s.r().z1() - 6);
            Rect right = new Rect(s.r().x1() - 3 - wing + 1, back.z1() + 2, s.r().x1() - 3, s.r().z1() - 6);
            Japanese.Spec js = classic();
            Japanese.building(g, js, back, 1, rng.nextLong(), false);
            Japanese.building(g, js, left, 1, rng.nextLong(), false);
            Japanese.building(g, js, right, 1, rng.nextLong(), false);
            // Cour : gravier, cerisier central, lanternes aux angles, entrée par l'avant.
            rockGarden(g, left.x1() + 3, back.z1() + 3, right.x0() - 3, left.z1(), rng);
            Japanese.cherryTree(g, s.cx(), 1, (back.z1() + left.z1()) / 2 + 1, Math.max(9, s.w() / 3), rng.nextLong());
            Japanese.stoneLantern(g, left.x1() + 3, 1, left.z1());
            Japanese.stoneLantern(g, right.x0() - 3, 1, left.z1());
            frontPath(g, s, left.z1() + 1, 3, "minecraft:stone_bricks");
            Japanese.torii(g, s.cx(), 1, s.r().z1() - 2, 2, 5);
        }), "sakura", "cour", "interieure", "japonaise", "maison");
        r.gen("sakura_bassin_pont", "Maison au bassin et pont rouge", 4, MEDIUM, p -> make(p.size(), 1.05, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.5) | 1, Math.max(7, s.d() / 3)).translate(0, -s.d() / 4);
            Japanese.building(g, classic(), h, 1, rng.nextLong(), true);
            int pz = h.z1() + 3 + s.d() / 8;
            int rx = Math.max(3, s.w() / 3);
            Japanese.pond(g, s.cx(), 0, pz, rx, Math.max(2, s.d() / 8), rng);
            Japanese.bridge(g, s.cx() - 2, s.cx() + 2, 0, pz);
            // Allée qui contourne le pont : le pont relie les deux rives du bassin.
            for (int z = h.z1() + 2; z < pz - Math.max(2, s.d() / 8) - 1; z++) {
                g.set(s.cx(), 0, z, PATH);
            }
            Japanese.cherryTree(g, s.r().x0() + 3, 1, pz, Math.max(8, s.w() / 3), rng.nextLong());
            Japanese.cherryTree(g, s.r().x1() - 3, 1, h.z0(), Math.max(8, s.w() / 3), rng.nextLong());
        }), "sakura", "bassin", "pont", "japonaise", "maison");
        r.gen("sakura_allee_lanternes", "Maison et allée de lanternes", 3, MEDIUM, p -> make(p.size(), 1.2, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.55) | 1, Math.max(7, s.d() / 3)).translate(0, -s.d() / 4);
            Japanese.building(g, classic(), h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 3, "minecraft:stone_bricks");
            for (int z = h.z1() + 4; z < s.r().z1(); z += 4) {
                Japanese.stoneLantern(g, s.cx() - 3, 1, z);
                Japanese.stoneLantern(g, s.cx() + 3, 1, z);
            }
            sideTrees(g, s, 3, Math.max(7, s.w() / 4), rng);
        }), "sakura", "lanternes", "allee", "japonaise", "maison");
        r.gen("sakura_entree_torii", "Maison et portique torii", 3, MEDIUM, p -> make(p.size(), 1.1, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.55) | 1, Math.max(7, s.d() / 3)).translate(0, -s.d() / 5);
            Japanese.building(g, classic(), h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 3, "minecraft:gravel");
            steppingStones(g, s, h.z1() + 2);
            Japanese.torii(g, s.cx(), 1, s.r().z1() - 3, 2, Math.max(5, s.w() / 5));
            Japanese.cherryTree(g, s.r().x0() + 3, 1, s.r().z1() - 4, Math.max(8, s.w() / 3), rng.nextLong());
            Japanese.cherryTree(g, s.r().x1() - 3, 1, s.r().z1() - 6, Math.max(8, s.w() / 3), rng.nextLong());
        }), "sakura", "torii", "entree", "traditionnelle", "maison");
        r.gen("sakura_balcon_bois", "Maison à étage et balcon de bois", 4, MEDIUM, p -> make(p.size(), 0.95, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.55) | 1, Math.max(9, s.d() / 2)).translate(0, -s.d() / 10);
            Japanese.Spec js = wood();
            js.floors = 2;
            js.balcony = true;
            js.upperInset = 1;
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 3, PATH);
            sideTrees(g, s, 2, Math.max(8, s.w() / 3), rng);
        }), "sakura", "balcon", "etage", "bois", "maison");
        r.gen("sakura_foret", "Maison au cœur d'une forêt de cerisiers", 4, LARGE, p -> make(p.size(), 1.0, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.35) | 1, Math.max(7, s.d() / 3));
            Japanese.building(g, classic(), h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 3, PATH);
            int placed = 0;
            for (int i = 0; i < 60 && placed < Math.max(6, s.w() / 3); i++) {
                int x = s.r().x0() + 2 + rng.nextInt(s.w() - 4);
                int z = s.r().z0() + 2 + rng.nextInt(s.d() - 4);
                // Les couronnes (rayon ≈ 0,42 × hauteur) ne doivent pas recouvrir la maison ni l'allée.
                boolean nearHouse = h.grow(7).contains(x, z) || Math.abs(x - s.cx()) <= 5 && z > h.z1();
                if (!nearHouse) {
                    Japanese.cherryTree(g, x, 1, z, 7 + rng.nextInt(Math.max(2, s.w() / 5)), rng.nextLong());
                    placed++;
                }
            }
        }), "sakura", "foret", "cerisiers", "maison", "nature");
        r.gen("sakura_luxe", "Demeure japonaise de prestige", 5, LARGE, p -> make(p.size(), 1.05, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.5) | 1, Math.max(11, s.d() / 3)).translate(0, -s.d() / 5);
            Japanese.Spec js = classic();
            js.floors = 2;
            js.engawa = 2;
            js.baseHeight = 2;
            js.balcony = true;
            js.post = "minecraft:stripped_cherry_log";
            js.rail = "minecraft:cherry_fence";
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            int pz = h.z1() + 5 + s.d() / 10;
            Japanese.pond(g, s.cx() - s.w() / 5, 0, pz, Math.max(3, s.w() / 7), Math.max(2, s.d() / 9), rng);
            Japanese.bridge(g, s.cx() - s.w() / 5 - 2, s.cx() - s.w() / 5 + 2, 0, pz);
            frontPath(g, s, h.z1() + 3, 3, "minecraft:stone_bricks");
            Japanese.torii(g, s.cx(), 1, s.r().z1() - 3, 2, 6);
            for (int z = h.z1() + 4; z < s.r().z1() - 4; z += 4) {
                Japanese.stoneLantern(g, s.cx() + 3, 1, z);
            }
            rockGarden(g, s.cx() + 5, h.z1() + 4, s.r().x1() - 3, s.r().z1() - 6, rng);
            sideTrees(g, s, 3, Math.max(9, s.w() / 4), rng);
            bambooFence(g, s, 2);
        }), "sakura", "luxe", "prestige", "demeure", "japonaise", "maison");
        r.gen("sakura_ryokan", "Ryokan (auberge japonaise) et bain extérieur", 5, LARGE, p -> make(p.size(), 0.85, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.7) | 1, Math.max(9, s.d() / 3)).translate(0, -s.d() / 6);
            Japanese.Spec js = wood();
            js.floors = 2;
            js.balcony = true;
            js.upperInset = 1;
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            // Bain extérieur (onsen) bordé de pierres, lanternes.
            int bx = h.x1() - s.w() / 6, bz = h.z1() + 5;
            Japanese.pond(g, bx, 0, bz, Math.max(2, s.w() / 10), 2, rng);
            Japanese.stoneLantern(g, bx + Math.max(2, s.w() / 10) + 2, 1, bz);
            frontPath(g, s, h.z1() + 2, 3, "minecraft:stone_bricks");
            Japanese.cherryTree(g, s.r().x0() + 4, 1, h.z1() + 5, Math.max(9, s.w() / 4), rng.nextLong());
            bambooFence(g, s, 2);
        }), "sakura", "ryokan", "auberge", "onsen", "bain", "maison");
        r.gen("sakura_campagne", "Maison de campagne japonaise et rizières", 3, MEDIUM, p -> make(p.size(), 1.1, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.45) | 1, Math.max(7, s.d() / 3)).translate(s.w() / 6, -s.d() / 4);
            Japanese.Spec js = wood();
            js.roofStyle = RoofStyle.GABLE;
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            // Rizières : bassins peu profonds plantés, séparés par des diguettes.
            for (int px = s.r().x0() + 2; px < h.x0() - 3; px += 5) {
                for (int pz = h.z1() + 2; pz < s.r().z1() - 2; pz += 5) {
                    g.fill(px, 0, pz, px + 3, 0, pz + 3, "minecraft:water");
                    for (int x = px; x <= px + 3; x++) {
                        for (int z = pz; z <= pz + 3; z++) {
                            if ((x + z) % 2 == 0) {
                                g.set(x, 1, z, "minecraft:lily_pad");
                            }
                        }
                    }
                }
            }
            frontPath(g, s, h.z1() + 2, 1, PATH);
            Japanese.cherryTree(g, s.r().x1() - 3, 1, s.r().z1() - 3, Math.max(8, s.w() / 3), rng.nextLong());
        }), "sakura", "campagne", "rizieres", "ferme", "maison");
        r.gen("sakura_niveaux", "Maison japonaise sur trois niveaux", 4, MEDIUM, p -> make(p.size(), 0.95, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.5) | 1, (int) (s.w() * 0.5) | 1).translate(0, -s.d() / 10);
            Japanese.Spec js = classic();
            js.floors = 3;
            js.upperInset = 1;
            js.balcony = true;
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 3, PATH);
            sideTrees(g, s, 2, Math.max(9, s.w() / 3), rng);
        }), "sakura", "niveaux", "etages", "pagode", "maison");
        r.gen("sakura_jardin_pierres", "Maison et jardin de pierres", 3, MEDIUM, p -> make(p.size(), 1.0, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.5) | 1, Math.max(7, s.d() / 3)).translate(0, -s.d() / 5);
            Japanese.building(g, classic(), h, 1, rng.nextLong(), true);
            rockGarden(g, s.r().x0() + 2, h.z1() + 3, s.r().x1() - 2, s.r().z1() - 2, rng);
            steppingStones(g, s, h.z1() + 2);
            Japanese.cherryTree(g, s.r().x0() + 3, 1, s.r().z0() + 3, Math.max(8, s.w() / 3), rng.nextLong());
            Japanese.stoneLantern(g, s.cx() + 4, 1, h.z1() + 4);
        }), "sakura", "jardin", "pierres", "zen", "karesansui", "maison");
        r.gen("sakura_serre", "Maison japonaise et petite serre", 3, MEDIUM, p -> make(p.size(), 1.0, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.45) | 1, Math.max(7, s.d() / 3)).translate(-s.w() / 7, -s.d() / 6);
            Japanese.Spec js = pinkWhite();
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            // Serre : ossature de cerisier, parois et toit vitrés, plates-bandes intérieures.
            int gx0 = h.x1() + 4, gx1 = Math.min(s.r().x1() - 2, gx0 + Math.max(4, s.w() / 5)), gz0 = h.z0(), gz1 = h.z0() + Math.max(4, s.d() / 5);
            for (int x = gx0; x <= gx1; x++) {
                for (int z = gz0; z <= gz1; z++) {
                    boolean edge = x == gx0 || x == gx1 || z == gz0 || z == gz1;
                    boolean corner = (x == gx0 || x == gx1) && (z == gz0 || z == gz1);
                    g.set(x, 0, z, edge ? "minecraft:smooth_stone" : "minecraft:farmland[moisture=7]");
                    for (int y = 1; y <= 3; y++) {
                        if (corner) {
                            g.set(x, y, z, "minecraft:stripped_cherry_log[axis=y]");
                        } else if (edge) {
                            g.set(x, y, z, "minecraft:glass");
                        }
                    }
                    g.set(x, 4, z, edge ? "minecraft:cherry_slab[type=bottom]" : "minecraft:glass");
                }
            }
            for (int x = gx0 + 1; x < gx1; x++) {
                for (int z = gz0 + 1; z < gz1; z++) {
                    g.set(x, 0, z, "minecraft:grass_block[snowy=false]");
                    g.set(x, 1, z, (x + z) % 3 == 0 ? "minecraft:pink_tulip" : (x + z) % 3 == 1 ? "minecraft:allium" : "minecraft:fern");
                }
            }
            g.set((gx0 + gx1) / 2, 1, gz1, "minecraft:cherry_door[facing=south,half=lower,hinge=left,open=false]");
            g.set((gx0 + gx1) / 2, 2, gz1, "minecraft:cherry_door[facing=south,half=upper,hinge=left,open=false]");
            frontPath(g, s, h.z1() + 2, 3, PATH);
            Japanese.cherryTree(g, s.r().x0() + 3, 1, s.r().z1() - 4, Math.max(8, s.w() / 3), rng.nextLong());
        }), "sakura", "serre", "jardin", "fleurs", "maison");
        r.gen("sakura_chemin_fleurs", "Maison au chemin de pierre fleuri", 2, SMALL, p -> make(p.size(), 1.1, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered(s.w() / 2 | 1, Math.max(5, s.d() / 3)).translate(0, -s.d() / 4);
            Japanese.Spec js = pinkWhite();
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            frontPath(g, s, h.z1() + 2, 1, "minecraft:stone_bricks");
            flowers(g, s.cx() - 4, h.z1() + 3, s.cx() - 2, s.r().z1() - 1, rng);
            flowers(g, s.cx() + 2, h.z1() + 3, s.cx() + 4, s.r().z1() - 1, rng);
            Japanese.cherryTree(g, s.r().x0() + 2, 1, s.r().z1() - 3, Math.max(7, s.w() / 3), rng.nextLong());
            shrub(g, s.r().x1() - 2, s.r().z1() - 2, rng);
        }), "sakura", "chemin", "fleurs", "petite", "depart", "maison");
        r.gen("sakura_terrasse_surelevee", "Maison Sakura sur terrasse surélevée", 4, MEDIUM, p -> make(p.size(), 0.95, p.seed(), (g, s, rng) -> {
            Rect h = Rect.centered((int) (s.w() * 0.5) | 1, Math.max(7, s.d() / 3)).translate(0, -s.d() / 8);
            Japanese.Spec js = pinkWhite();
            js.baseHeight = 4;
            js.engawa = 2;
            js.base = "minecraft:stone_bricks";
            js.baseTop = "minecraft:chiseled_stone_bricks";
            Japanese.building(g, js, h, 1, rng.nextLong(), true);
            for (int x = h.x0() - 2; x <= h.x1() + 2; x++) {
                g.setIfEmpty(x, 6, h.z1() + 2, "minecraft:cherry_fence");
                g.setIfEmpty(x, 6, h.z0() - 2, "minecraft:cherry_fence");
            }
            frontPath(g, s, h.z1() + 6, 3, "minecraft:stone_bricks");
            sideTrees(g, s, 2, Math.max(9, s.w() / 3), rng);
            Japanese.stoneLantern(g, s.cx() - 3, 1, h.z1() + 7);
            Japanese.stoneLantern(g, s.cx() + 3, 1, h.z1() + 7);
        }), "sakura", "terrasse", "surelevee", "rose", "maison");
    }
}
