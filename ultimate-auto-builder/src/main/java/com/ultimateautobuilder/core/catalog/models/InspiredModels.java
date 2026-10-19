package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.Arch.Rect;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.Castle;
import com.ultimateautobuilder.core.generator.House;
import com.ultimateautobuilder.core.generator.House.Feature;
import com.ultimateautobuilder.core.generator.RoofMat;
import com.ultimateautobuilder.core.generator.Trees;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Rng;

/**
 * Constructions inspirées des captures fournies par le joueur : maisons de cerisier au bord de
 * l'eau, palais et cours asiatiques, serre en dôme, arbres-maisons, villas modernes, immeuble,
 * chaumière au moulin, châteaux. Toutes sont générées à la taille demandée (largeur au sol).
 */
public final class InspiredModels {
    private static final SizeSpec MEDIUM = SizeSpec.width(15, 23, 35, 55);
    private static final SizeSpec LARGE = SizeSpec.width(31, 45, 65, 95);

    private static final String CHERRY = "minecraft:cherry_planks";
    private static final String CHERRY_LEAVES = "minecraft:cherry_leaves[persistent=true]";
    private static final String WATER = "minecraft:water";
    private static final String LANTERN = "minecraft:lantern[hanging=false]";

    private InspiredModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.INSPIRED);
        r.gen("cerisier_maison_quai", "Maison de cerisier à double pignon et quai", 5, MEDIUM, p -> cherryLakeHouse(p.size(), p.seed(), true),
                "cerisier", "rose", "lac", "quai", "maison");
        r.gen("cerisier_maison_lac", "Maison de cerisier au bord du lac", 4, MEDIUM, p -> cherryLakeHouse(p.size(), p.seed(), false),
                "cerisier", "rose", "lac", "ponton", "maison");
        r.gen("cottage_rose", "Cottage rose au toit fleuri", 3, Reg.HOUSE, p -> pinkCottage(p.size(), p.seed()), "cerisier", "rose", "cottage");
        r.gen("serre_dome", "Ferme sous dôme de verre", 4, LARGE, p -> greenhouseDome(p.size(), p.seed()), "serre", "ferme", "dome", "verre");
        r.gen("maison_sous_cerisier", "Maison sous le grand cerisier", 4, MEDIUM, p -> houseUnderCherry(p.size(), p.seed()),
                "cerisier", "arbre", "maison", "lanterne");
        r.gen("cerisier_geant_cabane", "Cerisier géant et sa cabane", 5, LARGE, p -> giantCherryTree(p.size(), p.seed()),
                "cerisier", "arbre geant", "cabane", "jardin");
        r.gen("cour_asiatique_rose", "Cour asiatique aux toits roses", 5, LARGE, p -> asianCompound(p.size(), p.seed(), false),
                "asie", "japon", "chine", "cour", "cerisier");
        r.gen("cour_asiatique_blanche", "Cour asiatique blanche", 5, LARGE, p -> asianCompound(p.size(), p.seed(), true),
                "asie", "palais", "blanc", "cour", "lanternes");
        r.gen("palais_pagodes", "Palais aux pagodes", 5, LARGE, p -> pagodaPalace(p.size(), p.seed()), "asie", "pagode", "palais", "tours");
        r.gen("maison_asiatique_jardin", "Maison asiatique et potager", 4, MEDIUM, p -> asianGardenHouse(p.size(), p.seed()),
                "asie", "maison", "jardin", "potager");
        r.gen("ferme_soubassement", "Ferme en bois sur soubassement de pierre", 4, MEDIUM, p -> farmhouseOnStone(p.size(), p.seed(), false),
                "ferme", "bois", "pierre", "maison");
        r.gen("chalet_toit_sombre", "Chalet au toit d'ardoise", 4, MEDIUM, p -> farmhouseOnStone(p.size(), p.seed(), true),
                "chalet", "ardoise", "pierre", "maison");
        r.gen("maison_hiver_automne", "Maison d'hiver et son champ", 4, Reg.HOUSE, p -> snowyAutumnHouse(p.size(), p.seed()),
                "neige", "automne", "hiver", "maison", "champ");
        r.gen("grand_manoir_bois", "Grand manoir en bois", 5, MEDIUM, p -> woodenMansion(p.size(), p.seed()), "manoir", "bois", "cheminee");
        r.gen("villa_blanche_moderne", "Villa blanche en porte-à-faux", 4, MEDIUM, p -> modernVilla(p.size()), "moderne", "villa", "blanche", "beton");
        r.gen("immeuble_appartements_eclaire", "Immeuble d'appartements éclairé", 4, Reg.BUILDING, p -> apartmentTower(p.size(), p.seed()),
                "immeuble", "appartements", "moderne", "ville");
        r.gen("chaumiere_moulin", "Chaumière au moulin et potager", 4, MEDIUM, p -> windmillCottage(p.size(), p.seed()),
                "moulin", "chaumiere", "medieval", "champ");
        r.gen("chateau_drapeaux", "Château aux drapeaux à damier", 4, Reg.CASTLE, p -> flagCastle(p.size(), p.seed()),
                "chateau", "drapeaux", "medieval", "tours");
        r.gen("manoir_fortifie", "Manoir fortifié aux flèches d'ardoise", 5, Reg.CASTLE, p -> manorCastle(p.size(), p.seed()),
                "chateau", "manoir", "colombages", "fleches");
    }

    // ------------------------------------------------------------------ outils

    /** Plus haute cellule occupée de la colonne (x, z), ou {@code fallback}. */
    private static int top(VoxelGrid g, int x, int z, int fallback) {
        for (int y = 255; y >= -16; y--) {
            if (g.isSet(x, y, z)) {
                return y;
            }
        }
        return fallback;
    }

    private static void lanternPost(VoxelGrid g, int x, int y, int z, int h, String post) {
        g.fill(x, y, z, x, y + h - 1, z, post);
        g.set(x, y + h, z, LANTERN);
    }

    /** Buisson de feuilles (ellipsoïde), sans écraser l'existant. */
    private static void bush(VoxelGrid g, int cx, int cy, int cz, double rx, double ry, double rz, String leaves) {
        int ix = (int) Math.ceil(rx), iy = (int) Math.ceil(ry), iz = (int) Math.ceil(rz);
        for (int dx = -ix; dx <= ix; dx++) {
            for (int dy = -iy; dy <= iy; dy++) {
                for (int dz = -iz; dz <= iz; dz++) {
                    double v = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                    if (v <= 1.0) {
                        g.setIfEmpty(cx + dx, cy + dy, cz + dz, leaves);
                    }
                }
            }
        }
    }

    private static void disc(VoxelGrid g, int cx, int y, int cz, double r, String mat) {
        int ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                if (dx * dx + dz * dz <= r * r) {
                    g.set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    private static void ring(VoxelGrid g, int cx, int y, int cz, double r0, double r1, String mat) {
        int ir = (int) Math.ceil(r1);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d >= r0 && d <= r1) {
                    g.set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    /** Parcelle cultivée bordée d'eau : terre labourée + culture mûre. */
    private static void cropPlot(VoxelGrid g, int x0, int z0, int x1, int z1, int y, String crop) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean water = (z - z0) % 5 == 2 && x == (x0 + x1) / 2;
                if (water) {
                    g.set(x, y, z, WATER);
                } else {
                    g.set(x, y, z, "minecraft:farmland[moisture=7]");
                    g.set(x, y + 1, z, crop);
                }
            }
        }
    }

    private static void fenceRect(VoxelGrid g, int x0, int z0, int x1, int z1, int y, String fence, boolean gateFront) {
        for (int x = x0; x <= x1; x++) {
            g.set(x, y, z0, fence);
            if (!(gateFront && x == (x0 + x1) / 2)) {
                g.set(x, y, z1, fence);
            }
        }
        for (int z = z0; z <= z1; z++) {
            g.set(x0, y, z, fence);
            g.set(x1, y, z, fence);
        }
    }

    // ------------------------------------------------------------------ maisons de cerisier

    /**
     * Maison rose au bord de l'eau : rez-de-chaussée blanc ouvert, étage en cerisier avec terrasse
     * et balustrades, toiture double ou simple, quai sur pilotis avec lanternes.
     */
    static VoxelModel cherryLakeHouse(int width, long seed, boolean doubleGable) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        int d = Math.max(9, (int) Math.round(w * 0.6));
        Rect body = Rect.centered(w, d);
        String white = "minecraft:smooth_quartz";
        String pinkPost = "minecraft:pink_terracotta";
        // Soubassement blanc percé de deux arches.
        Arch.floor(g, body, 0, white);
        Arch.walls(g, body, 1, 4, white);
        Arch.air(g, body.inset(1), 1, 4);
        int arch = Math.max(2, w / 6);
        for (int side = -1; side <= 1; side += 2) {
            int cx = body.cx() + side * (w / 4);
            g.fill(cx - arch / 2, 1, body.z1(), cx + arch / 2, 3, body.z1(), B.AIR);
            g.fill(cx - arch / 2, 1, body.z1() - 1, cx + arch / 2, 1, body.z1() - 1, "minecraft:hay_block");
            g.fill(cx - arch / 2, 2, body.z1(), cx + arch / 2, 2, body.z1(), "minecraft:cherry_fence");
        }
        // Étage en cerisier, en retrait pour dégager une terrasse avant.
        Rect upper = new Rect(body.x0(), body.z0(), body.x1(), body.z1() - 3);
        Arch.floor(g, body, 5, CHERRY);
        Arch.walls(g, upper, 6, 9, CHERRY);
        Arch.air(g, upper.inset(1), 6, 9);
        Arch.pillars(g, upper, 6, 9, "minecraft:stripped_cherry_log[axis=y]");
        Arch.windows(g, upper, 7, 2, 3, "minecraft:glass_pane", upper.cx());
        Arch.door(g, upper.cx(), 6, upper.z1(), "cherry", B.S);
        // Balustrade de la terrasse et lanternes.
        for (int x = body.x0(); x <= body.x1(); x++) {
            if (Math.abs(x - body.cx()) > 1) {
                g.set(x, 6, body.z1(), "minecraft:cherry_fence");
            }
        }
        for (int z = upper.z1() + 1; z <= body.z1(); z++) {
            g.set(body.x0(), 6, z, "minecraft:cherry_fence");
            g.set(body.x1(), 6, z, "minecraft:cherry_fence");
        }
        lanternPost(g, body.x0(), 6, body.z1(), 1, "minecraft:cherry_fence");
        lanternPost(g, body.x1(), 6, body.z1(), 1, "minecraft:cherry_fence");
        g.set(body.cx() - 3, 6, upper.z1() + 1, "minecraft:chest[facing=south]");
        bush(g, body.cx() + 3, 7, upper.z1() + 1, 1.2, 1.5, 0.8, CHERRY_LEAVES);
        // Escalier central du sol à la terrasse.
        for (int i = 0; i < 5; i++) {
            g.set(body.cx(), 1 + i, body.z1() + 4 - i, B.stairs("cherry", B.N, false));
            g.set(body.cx() + 1, 1 + i, body.z1() + 4 - i, B.stairs("cherry", B.N, false));
            g.fill(body.cx(), 2 + i, body.z1() + 4 - i, body.cx() + 1, 4 + i, body.z1() + 4 - i, B.AIR);
        }
        // Toiture.
        int roofY = 10;
        Arch.floor(g, upper, roofY, CHERRY);
        if (doubleGable) {
            Rect left = new Rect(upper.x0(), upper.z0(), upper.cx(), upper.z1());
            Rect right = new Rect(upper.cx(), upper.z0(), upper.x1(), upper.z1());
            Arch.roof(g, RoofType.GABLE_Z, left, roofY, RoofMat.of("smooth_quartz"), 1, CHERRY);
            Arch.roof(g, RoofType.GABLE_Z, right, roofY, RoofMat.of("smooth_quartz"), 1, CHERRY);
            Arch.roof(g, RoofType.GABLE_Z, left.inset(1), roofY + 1, RoofMat.of("cherry"), 0, CHERRY);
            Arch.roof(g, RoofType.GABLE_Z, right.inset(1), roofY + 1, RoofMat.of("cherry"), 0, CHERRY);
        } else {
            Arch.roof(g, RoofType.GABLE, upper, roofY, RoofMat.of("cherry"), 1, CHERRY);
        }
        // Intérieur : lit, lanternes, bibliothèque.
        g.set(upper.x0() + 1, 6, upper.z0() + 1, B.bedHead("pink", B.N));
        g.set(upper.x0() + 1, 6, upper.z0() + 2, B.bedFoot("pink", B.N));
        g.set(upper.x1() - 1, 6, upper.z0() + 1, "minecraft:bookshelf");
        g.set(upper.cx(), 9, upper.cz(), "minecraft:lantern[hanging=true]");
        // Plan d'eau et quai sur pilotis.
        int pz0 = body.z1() + 5, pz1 = body.z1() + 5 + Math.max(6, d / 2 + 2);
        Rect pond = new Rect(body.x0() - 2, pz0, body.x1() + 2, pz1);
        g.fill(pond.x0(), -3, pond.z0(), pond.x1(), -1, pond.z1(), WATER);
        g.fill(pond.x0(), -4, pond.z0(), pond.x1(), -4, pond.z1(), "minecraft:sand");
        g.fill(body.x0() - 2, -3, body.z1() + 1, body.x1() + 2, -1, pz0 - 1, "minecraft:dirt");
        g.fill(body.x0() - 2, 0, body.z1() + 1, body.x1() + 2, 0, pz0 - 1, "minecraft:cherry_planks");
        int dockX0 = body.cx() - 2, dockX1 = body.cx() + 2;
        g.fill(dockX0, 0, pz0, dockX1, 0, pz1 - 2, CHERRY);
        int tx0 = body.cx() - w / 3, tx1 = body.cx() + w / 3;
        g.fill(tx0, 0, pz1 - 4, tx1, 0, pz1 - 2, CHERRY);
        for (int x = tx0; x <= tx1; x += 3) {
            g.fill(x, -3, pz1 - 2, x, 1, pz1 - 2, pinkPost);
            g.set(x, 2, pz1 - 2, "minecraft:cherry_trapdoor[half=bottom,facing=south,open=false]");
        }
        for (int z = pz0; z <= pz1 - 2; z += 3) {
            g.fill(dockX0, -3, z, dockX0, 1, z, pinkPost);
            g.fill(dockX1, -3, z, dockX1, 1, z, pinkPost);
        }
        lanternPost(g, tx0, 1, pz1 - 4, 1, "minecraft:cherry_fence");
        lanternPost(g, tx1, 1, pz1 - 4, 1, "minecraft:cherry_fence");
        lanternPost(g, body.x0() - 1, 1, body.z1() + 2, 1, "minecraft:cherry_fence");
        lanternPost(g, body.x1() + 1, 1, body.z1() + 2, 1, "minecraft:cherry_fence");
        // Haies et buissons de cerisier.
        Rng rng = new Rng(seed);
        for (int z = body.z0(); z <= body.z1(); z += 2) {
            bush(g, body.x1() + 2, 1, z, 1.5, 1.4 + rng.range(0, 0.6), 1.5, CHERRY_LEAVES);
        }
        bush(g, body.x0() - 2, 1, body.z1() + 1, 1.6, 2.2, 1.6, CHERRY_LEAVES);
        return g.freeze();
    }

    /** Cottage rose : cerisier, bordures de pierre, terrasse vitrée, fleurs de cerisier sur le toit. */
    static VoxelModel pinkCottage(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        House.Spec s = House.spec().walls("cherry_planks", "stripped_cherry_log[axis=y]").foundation("stone_bricks")
                .floorMat("cherry_planks").trim("stone_bricks").window("glass_pane").door("cherry").furniture("cherry")
                .colors("pink", "pink").roof(RoofType.GABLE, "stone_brick").overhang(1)
                .with(Feature.PORCH, Feature.LANTERNS, Feature.GARDEN);
        Rect r = House.build(g, s, width, seed, 0, 0, 0);
        Rng rng = new Rng(seed);
        int n = Math.max(3, r.width() / 4);
        for (int i = 0; i < n; i++) {
            int x = r.x0() + 1 + rng.nextInt(Math.max(1, r.width() - 2));
            int z = r.z0() + 1 + rng.nextInt(Math.max(1, r.depth() - 2));
            int y = top(g, x, z, 6) + 1;
            bush(g, x, y, z, 1.4, 1.0, 1.4, CHERRY_LEAVES);
        }
        bush(g, r.x0() - 2, 1, r.z1() + 1, 1.5, 2.0, 1.5, "minecraft:flowering_azalea_leaves[persistent=true]");
        bush(g, r.x1() + 2, 1, r.z1() + 1, 1.5, 2.0, 1.5, "minecraft:flowering_azalea_leaves[persistent=true]");
        return g.freeze();
    }

    /** Ferme circulaire : dôme de verre sur quatre parcelles, chemins, ceinture d'arbres, balise centrale. */
    static VoxelModel greenhouseDome(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int R = Math.max(15, width / 2);
        int ri = Math.max(8, (int) Math.round(R * 0.55));
        disc(g, 0, 0, 0, R, "minecraft:grass_block");
        ring(g, 0, 0, 0, R - 2.5, R - 0.5, "minecraft:spruce_planks");
        ring(g, 0, 1, 0, R - 0.9, R - 0.5, "minecraft:spruce_fence");
        disc(g, 0, 0, 0, ri + 2, "minecraft:moss_block");
        // Quatre parcelles séparées par des allées en croix.
        int q = ri - 1;
        cropPlot(g, -q, -q, -2, -2, 0, "minecraft:wheat[age=7]");
        cropPlot(g, 2, -q, q, -2, 0, "minecraft:carrots[age=7]");
        cropPlot(g, -q, 2, -2, q, 0, "minecraft:potatoes[age=7]");
        g.fill(2, 0, 2, q, 0, q, "minecraft:grass_block");
        fenceRect(g, 2, 2, q, q, 1, "minecraft:oak_fence", true);
        g.set(q - 1, 1, q - 1, "minecraft:beehive[facing=south]");
        g.set(3, 1, 3, "minecraft:hay_block");
        g.fill(-q, 0, -1, q, 0, 1, "minecraft:oak_planks");
        g.fill(-1, 0, -q, 1, 0, q, "minecraft:oak_planks");
        // Coupe ronde : tout ce qui dépasse le cercle intérieur redevient de la mousse.
        for (int x = -q; x <= q; x++) {
            for (int z = -q; z <= q; z++) {
                if (x * x + z * z > (ri - 0.5) * (ri - 0.5)) {
                    g.set(x, 0, z, "minecraft:moss_block");
                    g.remove(x, 1, z);
                }
            }
        }
        Arch.dome(g, 0, 1, 0, ri, ri, ri, "minecraft:glass", true);
        // Portes sur les quatre axes.
        g.fill(-1, 1, ri, 1, 3, ri, B.AIR);
        g.fill(-1, 1, -ri, 1, 3, -ri, B.AIR);
        g.fill(ri, 1, -1, ri, 3, 1, B.AIR);
        g.fill(-ri, 1, -1, -ri, 3, 1, B.AIR);
        // Arbre central et balise.
        Trees.oakInto(g, 0, 1, 0, Math.max(7, ri - 2), seed, "minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]");
        g.fill(-1, -1, -1, 1, -1, 1, "minecraft:iron_block");
        g.set(0, 0, 0, "minecraft:beacon");
        g.set(0, 1, 0, "minecraft:lime_stained_glass");
        for (int y = 2; y < ri; y++) {
            g.remove(0, y, 0);
        }
        // Ceinture d'arbres entre le dôme et le chemin.
        Rng rng = new Rng(seed);
        double ringR = (ri + R - 2.5) / 2.0;
        int trees = Math.max(8, (int) (2 * Math.PI * ringR / 6));
        for (int i = 0; i < trees; i++) {
            double a = i * 2 * Math.PI / trees;
            int x = (int) Math.round(Math.cos(a) * ringR), z = (int) Math.round(Math.sin(a) * ringR);
            if (Math.abs(x) <= 2 || Math.abs(z) <= 2) {
                continue;
            }
            Trees.oakInto(g, x, 1, z, 6 + rng.nextInt(3), seed + i, "minecraft:oak_log[axis=y]", "minecraft:oak_leaves[persistent=true]");
        }
        // Allées d'accès du dôme au chemin extérieur, lanternes.
        for (int k = ri; k <= R - 2; k++) {
            g.fill(-1, 0, k, 1, 0, k, "minecraft:spruce_planks");
            g.fill(-1, 0, -k, 1, 0, -k, "minecraft:spruce_planks");
            g.fill(k, 0, -1, k, 0, 1, "minecraft:spruce_planks");
            g.fill(-k, 0, -1, -k, 0, 1, "minecraft:spruce_planks");
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + Math.PI / 8;
            lanternPost(g, (int) Math.round(Math.cos(a) * (R - 3)), 1, (int) Math.round(Math.sin(a) * (R - 3)), 1, "minecraft:spruce_fence");
        }
        return g.freeze();
    }

    /** Petite maison sombre noyée sous la canopée d'un grand cerisier. */
    static VoxelModel houseUnderCherry(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width);
        int hw = Math.max(5, w / 3) | 1;
        Rect house = Rect.centered(hw, hw);
        Arch.room(g, house, 0, 5, "minecraft:mangrove_planks", "minecraft:dark_oak_planks");
        Arch.pillars(g, house, 1, 5, "minecraft:mangrove_log[axis=y]");
        Arch.door(g, house.cx(), 1, house.z1(), "cherry", B.S);
        Arch.windows(g, house, 2, 1, 3, "minecraft:glass_pane", house.cx());
        Arch.floor(g, house, 6, "minecraft:dark_oak_planks");
        g.set(house.x0() - 1, 4, house.z1() + 1, "minecraft:lantern[hanging=true]");
        g.set(house.x1() + 1, 4, house.z1() + 1, "minecraft:lantern[hanging=true]");
        g.set(house.x0() - 1, 5, house.z1() + 1, "minecraft:chain[axis=y]");
        g.set(house.x1() + 1, 5, house.z1() + 1, "minecraft:chain[axis=y]");
        // Tronc derrière la maison et canopée qui retombe jusqu'au sol.
        int trunkZ = house.z0() - 2;
        g.fill(-1, 0, trunkZ - 1, 1, 9, trunkZ + 1, "minecraft:cherry_log[axis=y]");
        int cy = 9;
        double rx = w / 2.0, rz = w / 2.2, ry = Math.max(4, w / 5.0);
        bush(g, 0, cy, 0, rx, ry, rz, CHERRY_LEAVES);
        Rng rng = new Rng(seed);
        for (int i = 0; i < w * 3; i++) {
            double a = rng.range(0, 2 * Math.PI);
            double rr = rng.range(0.6, 0.98);
            int x = (int) Math.round(Math.cos(a) * rx * rr), z = (int) Math.round(Math.sin(a) * rz * rr);
            if (house.inset(-1).contains(x, z)) {
                continue;
            }
            int len = 1 + rng.nextInt(Math.max(2, cy - 2));
            for (int k = 1; k <= len; k++) {
                g.setIfEmpty(x, cy - (int) (ry * 0.4) - k, z, CHERRY_LEAVES);
            }
        }
        // Le dessous de la canopée reste dégagé autour de la porte.
        g.fill(house.x0() - 1, 1, house.z1() + 1, house.x1() + 1, 3, house.z1() + 3, B.AIR);
        bush(g, house.x0() - 2, 1, house.z1() + 2, 1.6, 1.5, 1.2, CHERRY_LEAVES);
        bush(g, house.x1() + 2, 1, house.z1() + 2, 1.6, 1.5, 1.2, CHERRY_LEAVES);
        disc(g, 0, 0, 0, w / 2.0 + 1, "minecraft:grass_block");
        Arch.floor(g, house, 0, "minecraft:dark_oak_planks");
        return g.freeze();
    }

    /** Cerisier gigantesque à houppier sphérique, cabane accrochée au tronc et jardin circulaire. */
    static VoxelModel giantCherryTree(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(31, width);
        double R = w / 2.0;
        disc(g, 0, 0, 0, R * 0.55, "minecraft:grass_block");
        ring(g, 0, 0, 0, R * 0.2, R * 0.28, "minecraft:dirt_path");
        // Jardin : secteurs de cultures entre l'allée et la haie.
        for (int x = (int) -R; x <= R; x++) {
            for (int z = (int) -R; z <= R; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > R * 0.3 && dist < R * 0.47) {
                    double a = Math.atan2(z, x);
                    int sector = (int) Math.floor((a + Math.PI) / (Math.PI / 3));
                    if (Math.abs(x) <= 1 || Math.abs(z) <= 1) {
                        g.set(x, 0, z, "minecraft:dirt_path");
                    } else if ((x + z) % 4 == 0) {
                        g.set(x, 0, z, WATER);
                    } else {
                        String crop = switch (sector % 3) {
                            case 0 -> "minecraft:wheat[age=7]";
                            case 1 -> "minecraft:carrots[age=7]";
                            default -> "minecraft:beetroots[age=3]";
                        };
                        g.set(x, 0, z, "minecraft:farmland[moisture=7]");
                        g.set(x, 1, z, crop);
                    }
                }
            }
        }
        ring(g, 0, 1, 0, R * 0.48, R * 0.55, CHERRY_LEAVES);
        g.fill(-1, 1, (int) (R * 0.47), 1, 1, (int) (R * 0.56), B.AIR);
        // Tronc évasé à la base.
        int trunkH = (int) Math.round(R * 0.9);
        for (int y = 0; y < trunkH; y++) {
            double tr = Math.max(1.5, R * 0.16 * (1.0 - 0.5 * y / trunkH) + (y < 3 ? 1.5 - y * 0.5 : 0));
            disc(g, 0, y + 1, 0, tr, "minecraft:mangrove_log[axis=y]");
        }
        // Houppier sphérique et retombées.
        double cr = R * 0.75;
        int cy = trunkH + (int) (cr * 0.55);
        bush(g, 0, cy, 0, cr, cr * 0.85, cr, CHERRY_LEAVES);
        Rng rng = new Rng(seed);
        for (int i = 0; i < w * 4; i++) {
            double a = rng.range(0, 2 * Math.PI);
            double rr = rng.range(0.5, 0.95);
            int x = (int) Math.round(Math.cos(a) * cr * rr), z = (int) Math.round(Math.sin(a) * cr * rr);
            int y0 = cy - (int) (cr * 0.85 * Math.sqrt(Math.max(0, 1 - rr * rr)));
            int len = 1 + rng.nextInt(Math.max(2, (int) (cr * 0.6)));
            for (int k = 1; k <= len; k++) {
                g.setIfEmpty(x, y0 - k, z, CHERRY_LEAVES);
            }
        }
        // Cabane sur une plateforme accrochée au tronc.
        int py = trunkH - (int) (R * 0.15);
        int px0 = (int) (R * 0.12), px1 = px0 + Math.max(7, (int) (R * 0.35));
        Rect plat = new Rect(px0, -4, px1, 4);
        g.fill(plat.x0(), py, plat.z0(), plat.x1(), py, plat.z1(), CHERRY);
        Arch.walls(g, plat, py + 1, py + 1, "minecraft:cherry_fence");
        Rect cabin = new Rect(px0 + 2, -2, px1 - 2, 2);
        Arch.walls(g, cabin, py + 1, py + 4, "minecraft:crimson_planks");
        Arch.air(g, cabin.inset(1), py + 1, py + 4);
        Arch.door(g, cabin.cx(), py + 1, cabin.z1(), "cherry", B.S);
        Arch.roof(g, RoofType.HIP, cabin, py + 5, RoofMat.of("cherry"), 1, CHERRY);
        g.fill(cabin.x0() + 1, py + 1, cabin.z1() + 1, cabin.x1() - 1, py + 1, cabin.z1() + 1, B.AIR);
        g.set(plat.x0() + 1, py + 2, plat.z1(), LANTERN);
        g.set(plat.x1() - 1, py + 2, plat.z1(), LANTERN);
        for (int y = 1; y < py; y++) {
            g.set(px0, y, 5, "minecraft:ladder[facing=south]");
            g.setIfEmpty(px0, y, 4, "minecraft:mangrove_log[axis=y]");
        }
        // Lanternes autour de l'allée.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + Math.PI / 8;
            lanternPost(g, (int) Math.round(Math.cos(a) * R * 0.24), 1, (int) Math.round(Math.sin(a) * R * 0.24), 1, "minecraft:cherry_fence");
        }
        return g.freeze();
    }

    // ------------------------------------------------------------------ architecture asiatique

    private static House.Spec hall(boolean white, int floors) {
        return House.spec()
                .walls(white ? "white_concrete" : "white_terracotta", white ? "quartz_pillar[axis=y]" : "dark_oak_log[axis=y]")
                .trim(white ? "smooth_quartz" : "stripped_dark_oak_log[axis=x]").foundation(white ? "polished_diorite" : "stone_bricks")
                .floorMat(white ? "birch_planks" : "dark_oak_planks").door(white ? "birch" : "dark_oak").furniture(white ? "birch" : "dark_oak")
                .window(white ? "white_stained_glass_pane" : "glass_pane").colors("red", "red")
                .roof(RoofType.PAGODA, white ? "quartz" : "cherry").overhang(2).spacing(2).floors(floors).depth(0.55)
                .with(Feature.LANTERNS);
    }

    /** Cour fermée : quatre ailes à toits retroussés, pavillon principal, jardin de cerisiers et bassin. */
    static VoxelModel asianCompound(int width, long seed, boolean white) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(31, width) | 1;
        Rect site = Rect.centered(w, w);
        String base = white ? "minecraft:polished_andesite" : "minecraft:stone_bricks";
        g.fill(site.x0(), 0, site.z0(), site.x1(), 0, site.z1(), base);
        int wing = Math.max(11, w / 3) | 1;
        int off = w / 2 - wing / 4 - 2;
        // Ailes : construites face +Z puis tournées vers le centre.
        for (int side = 0; side < 4; side++) {
            VoxelGrid wg = new VoxelGrid();
            House.build(wg, hall(white, side == 0 ? 2 : 1), side == 0 ? wing + 4 : wing, seed + side, 0, 0, 0);
            int[][] pos = {{0, -off}, {off, 0}, {0, off}, {-off, 0}};
            int[] turns = {0, 1, 2, 3};
            g.paste(wg, pos[side][0], 1, pos[side][1], turns[side], false);
        }
        // Pavillons d'angle.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                VoxelGrid cg = new VoxelGrid();
                House.build(cg, hall(white, 2).depth(1.0), 7, seed + sx * 7 + sz, 0, 0, 0);
                g.paste(cg, sx * off, 1, sz * off, 0, false);
            }
        }
        // Jardin intérieur : herbe, allée, bassin, cerisiers, lanternes jaunes.
        int inner = Math.max(5, off - wing / 3 - 2);
        g.fill(-inner, 0, -inner, inner, 0, inner, "minecraft:grass_block");
        g.fill(-1, 0, -inner, 1, 0, w / 2, "minecraft:dirt_path");
        g.fill(-inner / 2, -1, -inner / 2, -2, 0, inner / 2, WATER);
        g.fill(-inner / 2 - 1, -2, -inner / 2 - 1, -1, -2, inner / 2 + 1, "minecraft:stone_bricks");
        Rng rng = new Rng(seed);
        int trees = Math.max(3, inner / 3);
        for (int i = 0; i < trees; i++) {
            int x = 2 + rng.nextInt(Math.max(1, inner - 3));
            int z = -inner + 2 + rng.nextInt(Math.max(1, 2 * inner - 3));
            Trees.cherryInto(g, i % 2 == 0 ? x : -x, 1, z, Math.max(6, inner), seed + i);
        }
        g.fill(-1, 1, 0, 1, 3, 0, white ? "minecraft:quartz_pillar[axis=y]" : "minecraft:dark_oak_log[axis=y]");
        g.set(0, 4, 0, "minecraft:lantern[hanging=false]");
        for (int i = -inner; i <= inner; i += 4) {
            g.set(i, 1, inner, "minecraft:yellow_wool");
            g.set(i, 2, inner, LANTERN);
        }
        // Portail d'entrée.
        int gz = site.z1();
        g.fill(-2, 1, gz, -2, 6, gz, "minecraft:red_concrete");
        g.fill(2, 1, gz, 2, 6, gz, "minecraft:red_concrete");
        g.fill(-4, 7, gz, 4, 7, gz, white ? "minecraft:smooth_quartz" : "minecraft:dark_oak_planks");
        g.fill(-3, 5, gz, 3, 5, gz, "minecraft:red_concrete");
        g.fill(-1, 1, gz, 1, 4, gz, B.AIR);
        return g.freeze();
    }

    /** Tour à étages décroissants coiffés de toits retroussés. */
    private static int pagodaTower(VoxelGrid g, int cx, int y, int cz, int baseW, int tiers, String wall, String post, String roof) {
        int w = baseW;
        for (int t = 0; t < tiers && w >= 5; t++) {
            Rect r = Rect.centered(w, w).translate(cx, cz);
            Arch.floor(g, r, y, "minecraft:dark_oak_planks");
            Arch.walls(g, r, y + 1, y + 3, wall);
            Arch.air(g, r.inset(1), y + 1, y + 3);
            Arch.pillars(g, r, y + 1, y + 3, post);
            Arch.windows(g, r, y + 2, 1, 2, "minecraft:pink_stained_glass_pane", t == 0 ? r.cx() : null);
            y = Arch.roof(g, RoofType.PAGODA, r, y + 4, RoofMat.of(roof), 1, wall);
            w -= 2;
        }
        g.fill(cx, y + 1, cz, cx, y + 3, cz, "minecraft:dark_oak_fence");
        return y;
    }

    /** Palais : grand pavillon central à cinq étages, tours secondaires, galeries et jardin. */
    static VoxelModel pagodaPalace(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(31, width) | 1;
        Rect site = Rect.centered(w, w);
        g.fill(site.x0(), 0, site.z0(), site.x1(), 0, site.z1(), "minecraft:stone_bricks");
        int big = Math.max(11, w / 3) | 1;
        g.fill(-big / 2 - 1, 1, site.z0() + 1, big / 2 + 1, 3, site.z0() + big + 1, "minecraft:stone_bricks");
        pagodaTower(g, 0, 4, site.z0() + big / 2 + 1, big, 4, "minecraft:white_terracotta", "minecraft:dark_oak_log[axis=y]", "cherry");
        int small = Math.max(7, big - 4) | 1;
        int sx = w / 2 - small / 2 - 1;
        pagodaTower(g, -sx, 1, site.z0() + small / 2 + 1, small, 3, "minecraft:white_terracotta", "minecraft:dark_oak_log[axis=y]", "cherry");
        pagodaTower(g, sx, 1, site.z0() + small / 2 + 1, small, 3, "minecraft:white_terracotta", "minecraft:dark_oak_log[axis=y]", "cherry");
        pagodaTower(g, -sx, 1, site.z1() - small / 2 - 1, small, 2, "minecraft:white_terracotta", "minecraft:dark_oak_log[axis=y]", "cherry");
        pagodaTower(g, sx, 1, site.z1() - small / 2 - 1, small, 2, "minecraft:white_terracotta", "minecraft:dark_oak_log[axis=y]", "cherry");
        // Galeries latérales reliant les tours.
        for (int side = -1; side <= 1; side += 2) {
            VoxelGrid gal = new VoxelGrid();
            House.build(gal, hall(false, 1).depth(0.35), Math.max(11, w - 2 * small - 4), seed + side, 0, 0, 0);
            g.paste(gal, side * sx, 1, 0, 1, false);
        }
        // Cour de cerisiers et allée centrale.
        int inner = Math.max(4, sx - small / 2 - 2);
        g.fill(-inner, 0, site.z0() + big + 3, inner, 0, site.z1() - 2, "minecraft:grass_block");
        g.fill(-1, 0, site.z0() + big + 2, 1, 0, site.z1(), "minecraft:smooth_stone");
        Rng rng = new Rng(seed);
        for (int i = 0; i < Math.max(4, inner / 2); i++) {
            int x = 3 + rng.nextInt(Math.max(1, inner - 3));
            int z = site.z0() + big + 4 + rng.nextInt(Math.max(1, site.z1() - site.z0() - big - 8));
            Trees.cherryInto(g, i % 2 == 0 ? x : -x, 1, z, Math.max(6, inner + 1), seed + 31 * i);
        }
        return g.freeze();
    }

    /** Maison asiatique rose sur socle de pierre, avec potager clos et cerisiers. */
    static VoxelModel asianGardenHouse(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        int hw = Math.max(9, (int) (w * 0.6)) | 1;
        Rect plinth = Rect.centered(hw + 4, hw + 2).translate(w / 2 - hw / 2 - 2, -2);
        for (int y = 0; y <= 2; y++) {
            g.fill(plinth.x0() + y, y, plinth.z0() + y, plinth.x1() - y, y, plinth.z1() - y, "minecraft:stone_bricks");
        }
        House.Spec s = House.spec().walls("pink_terracotta", "crimson_stem[axis=y]").trim("stripped_crimson_stem[axis=x]")
                .foundation("polished_andesite").floorMat("cherry_planks").door("crimson").furniture("cherry")
                .window("white_stained_glass_pane").colors("pink", "red").roof(RoofType.PAGODA, "cherry").overhang(2).floors(2)
                .spacing(2).with(Feature.LANTERNS, Feature.BALCONY);
        House.build(g, s, hw, seed, plinth.cx(), 3, plinth.cz());
        for (int i = 0; i < 3; i++) {
            g.fill(plinth.cx() - 1, i, plinth.z1() + 3 - i, plinth.cx() + 1, i, plinth.z1() + 3 - i, B.stairs("stone_brick", B.N, false));
        }
        // Potager clos à gauche et devant.
        int gx0 = -w / 2, gx1 = plinth.x0() - 2, gz0 = plinth.z1() - 1, gz1 = plinth.z1() + w / 3;
        if (gx1 - gx0 >= 4) {
            g.fill(gx0, 0, gz0, gx1, 0, gz1, "minecraft:dirt_path");
            cropPlot(g, gx0 + 1, gz0 + 1, (gx0 + gx1) / 2 - 1, gz1 - 1, 0, "minecraft:wheat[age=7]");
            cropPlot(g, (gx0 + gx1) / 2 + 1, gz0 + 1, gx1 - 1, gz1 - 1, 0, "minecraft:carrots[age=7]");
            fenceRect(g, gx0, gz0, gx1, gz1, 1, "minecraft:cherry_fence", true);
            for (int x = gx0; x <= gx1; x += 3) {
                g.set(x, 1, gz1, "minecraft:pink_terracotta");
                g.set(x, 2, gz1, LANTERN);
            }
        }
        Trees.cherryInto(g, plinth.x0() - 3, 0, plinth.z0() + 2, Math.max(6, w / 3), seed);
        Trees.cherryInto(g, plinth.cx(), 0, plinth.z1() + 6, Math.max(6, w / 3), seed + 1);
        return g.freeze();
    }

    // ------------------------------------------------------------------ maisons rurales

    /** Maison en bois posée sur un soubassement de pierre à arcades, escalier latéral, haies. */
    static VoxelModel farmhouseOnStone(int width, long seed, boolean slateRoof) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        int d = Math.max(9, (int) (w * 0.7));
        Rect base = Rect.centered(w, d);
        String stone = "minecraft:stone_bricks";
        Arch.floor(g, base, 0, "minecraft:cobblestone");
        Arch.walls(g, base, 1, 4, stone);
        Arch.air(g, base.inset(1), 1, 4);
        for (int x = base.x0() + 2; x <= base.x1() - 2; x += 4) {
            g.fill(x, 1, base.z1(), x + 1, 3, base.z1(), B.AIR);
            g.set(x, 3, base.z1(), B.stairs("stone_brick", B.E, true));
            g.set(x + 1, 3, base.z1(), B.stairs("stone_brick", B.W, true));
        }
        g.set(base.cx(), 1, base.z1() - 1, "minecraft:hay_block");
        House.Spec s = slateRoof
                ? House.spec().walls("spruce_planks", "dark_oak_log[axis=y]").trim("dark_oak_log[axis=x]").foundation("stone_bricks")
                .floorMat("spruce_planks").door("dark_oak").furniture("dark_oak").roof(RoofType.HIP, "deepslate_tile").overhang(1)
                .depth(0.7).floors(1).with(Feature.PORCH, Feature.CHIMNEY, Feature.LANTERNS)
                : House.spec().walls("oak_planks", "stripped_oak_log[axis=y]").trim("spruce_log[axis=x]").foundation("stone_bricks")
                .floorMat("oak_planks").door("spruce").furniture("oak").roof(RoofType.GABLE, "oak").overhang(1)
                .depth(0.7).floors(1).with(Feature.CHIMNEY, Feature.BALCONY, Feature.LANTERNS);
        House.build(g, s, w - 4, seed, 0, 5, -1);
        // Terrasse avant avec garde-corps et escalier latéral.
        g.fill(base.x0(), 5, base.z1() - 1, base.x1(), 5, base.z1(), "minecraft:spruce_planks");
        for (int x = base.x0(); x <= base.x1(); x++) {
            g.set(x, 6, base.z1(), "minecraft:spruce_fence");
        }
        for (int i = 0; i < 5; i++) {
            g.set(base.x1() + 1 + i, 4 - i, base.z1() - 1, B.stairs("spruce", B.W, false));
            g.set(base.x1() + 1 + i, 4 - i, base.z1(), B.stairs("spruce", B.W, false));
        }
        g.set(base.x1(), 6, base.z1(), B.AIR);
        // Haies et chemin.
        for (int x = base.x0() - 1; x <= base.x1() + 6; x++) {
            if (Math.abs(x - base.cx()) > 1) {
                g.set(x, 1, base.z1() + 3, "minecraft:oak_leaves[persistent=true]");
            }
        }
        g.fill(base.cx() - 1, 0, base.z1() + 1, base.cx() + 1, 0, base.z1() + 6, "minecraft:dirt_path");
        if (slateRoof) {
            g.set(base.x0() + 1, 1, base.z1() + 2, "minecraft:rose_bush[half=lower]");
            g.set(base.x0() + 1, 2, base.z1() + 2, "minecraft:rose_bush[half=upper]");
        }
        return g.freeze();
    }

    /** Maison de bois sombre sous la neige, buissons d'automne, champ de blé clos et lanternes. */
    static VoxelModel snowyAutumnHouse(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(11, width) | 1;
        House.Spec s = House.spec().walls("spruce_planks", "dark_oak_log[axis=y]").trim("dark_oak_log[axis=x]")
                .foundation("cobblestone").floorMat("spruce_planks").door("spruce").furniture("spruce").colors("orange", "brown")
                .roof(RoofType.STEEP, "dark_oak").floors(2).depth(0.8).with(Feature.SNOW, Feature.CHIMNEY, Feature.LANTERNS);
        Rect r = House.build(g, s, w, seed, 0, 0, 0);
        int fx0 = r.x0() - 1, fx1 = r.cx() - 1, fz0 = r.z1() + 2, fz1 = r.z1() + 2 + Math.max(5, w / 3);
        cropPlot(g, fx0 + 1, fz0 + 1, fx1 - 1, fz1 - 1, 0, "minecraft:wheat[age=7]");
        fenceRect(g, fx0, fz0, fx1, fz1, 1, "minecraft:spruce_fence", false);
        g.set(fx1, 2, fz1, LANTERN);
        g.set(fx1 - 1, 1, fz0 + 1, "minecraft:hay_block");
        g.fill(r.cx(), 0, r.z1() + 1, r.cx() + 1, 0, fz1 + 2, "minecraft:dirt_path");
        Rng rng = new Rng(seed);
        for (int i = 0; i < 5; i++) {
            int x = r.x0() + rng.nextInt(Math.max(1, r.width()));
            int z = r.z0() + rng.nextInt(Math.max(1, r.depth()));
            bush(g, x, top(g, x, z, 6) + 1, z, 1.2, 0.9, 1.2, i % 2 == 0 ? "minecraft:orange_wool" : "minecraft:red_terracotta");
        }
        bush(g, r.x1() + 2, 1, r.z1() + 1, 1.4, 1.6, 1.4, "minecraft:orange_wool");
        g.set(r.x1() - 1, 0, r.z1() + 2, "minecraft:chest[facing=south]");
        for (int x = r.x0() - 3; x <= r.x1() + 3; x++) {
            for (int z = r.z0() - 3; z <= fz1 + 3; z++) {
                if (!g.isSet(x, 0, z)) {
                    g.set(x, 0, z, "minecraft:snow_block");
                }
            }
        }
        return g.freeze();
    }

    /** Grand manoir en bois : deux ailes, colombages, grandes baies, cheminée de pierre. */
    static VoxelModel woodenMansion(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        House.Spec s = House.spec().walls("oak_planks", "spruce_log[axis=y]").trim("spruce_log[axis=x]").foundation("stone_bricks")
                .floorMat("spruce_planks").window("glass_pane").door("spruce").furniture("spruce").colors("red", "red")
                .roof(RoofType.GABLE, "oak").overhang(2).floors(2).depth(0.65)
                .with(Feature.WING, Feature.BALCONY, Feature.BIG_WINDOWS, Feature.CHIMNEY, Feature.ROOMS, Feature.LANTERNS, Feature.PORCH);
        Rect r = House.build(g, s, Math.max(15, width), seed, 0, 0, 0);
        g.fill(r.x0() - 2, 0, r.z1() + 1, r.x1() + 2, 0, r.z1() + 3, "minecraft:stone_bricks");
        for (int x = r.x0(); x <= r.x1(); x += 3) {
            g.set(x, 1, r.z1() + 3, "minecraft:red_tulip");
        }
        return g.freeze();
    }

    // ------------------------------------------------------------------ moderne

    /** Villa blanche à volumes décalés, bandeaux vitrés sombres et toit-terrasse. */
    static VoxelModel modernVilla(int width) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        int d = Math.max(11, (int) (w * 0.75));
        String white = "minecraft:white_concrete";
        String glass = "minecraft:black_stained_glass";
        Rect deck = Rect.centered(w + 4, d + 4);
        g.fill(deck.x0(), 0, deck.z0(), deck.x1(), 0, deck.z1(), "minecraft:smooth_quartz");
        Rect ground = Rect.centered(w - 4, d - 2).translate(-1, 0);
        Arch.room(g, ground, 1, 4, white, "minecraft:oak_planks");
        Arch.walls(g, ground, 2, 4, glass);
        Arch.pillars(g, ground, 1, 4, white);
        g.fill(ground.cx() - 2, 2, ground.z1(), ground.cx() + 2, 4, ground.z1(), white);
        Rect first = Rect.centered(w, d - 4).translate(1, -1);
        Arch.floor(g, first.grow(1), 5, white);
        Arch.walls(g, first, 6, 8, white);
        Arch.air(g, first.inset(1), 6, 8);
        g.fill(first.x0(), 7, first.z1(), first.x1(), 7, first.z1(), glass);
        g.fill(first.x0(), 7, first.z0(), first.x1(), 7, first.z0(), glass);
        g.fill(first.x0() + 2, 6, first.z1(), first.x0() + 4, 8, first.z1(), glass);
        Arch.floor(g, first.grow(1), 9, white);
        Rect top = Rect.centered(w / 2, d / 2).translate(w / 5, -d / 6);
        Arch.room(g, top, 10, 3, white, "minecraft:oak_planks");
        g.fill(top.x0(), 11, top.z1(), top.x1(), 12, top.z1(), glass);
        Arch.floor(g, top.grow(1), 13, white);
        Rect roofDeck = first.grow(1);
        for (int x = roofDeck.x0(); x <= roofDeck.x1(); x++) {
            g.setIfEmpty(x, 10, roofDeck.z0(), "minecraft:glass_pane");
            g.setIfEmpty(x, 10, roofDeck.z1(), "minecraft:glass_pane");
        }
        g.set(roofDeck.x0() + 2, 10, roofDeck.z1() - 2, "minecraft:oak_slab[type=bottom]");
        g.set(roofDeck.x0() + 3, 10, roofDeck.z1() - 2, "minecraft:oak_slab[type=bottom]");
        g.set(ground.x0() + 1, 2, ground.z0() + 1, "minecraft:bookshelf");
        g.set(first.cx(), 8, first.cz(), "minecraft:lantern[hanging=true]");
        g.set(deck.x0() + 1, 1, deck.z1() - 1, "minecraft:potted_azalea_bush");
        g.set(deck.x1() - 1, 1, deck.z1() - 1, "minecraft:potted_azalea_bush");
        return g.freeze();
    }

    /** Immeuble : cadre blanc, dalles sombres, façades vitrées, chambres éclairées, coursive végétalisée. */
    static VoxelModel apartmentTower(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(11, width) | 1;
        int d = Math.max(7, w / 2 + 2);
        int floors = Math.max(3, Math.min(10, w / 4 + 1));
        int fh = 5;
        Rect r = Rect.centered(w, d);
        String[] beds = {"blue", "red", "lime", "pink", "yellow", "light_blue", "purple", "orange"};
        int corridor = r.x1() - Math.max(3, w / 4);
        for (int f = 0; f < floors; f++) {
            int y = f * fh;
            Arch.floor(g, r, y, "minecraft:gray_concrete");
            Arch.walls(g, r, y + 1, y + fh - 1, "minecraft:white_concrete");
            Arch.air(g, r.inset(1), y + 1, y + fh - 1);
            g.fill(r.x0() + 1, y + 1, r.z1(), corridor - 1, y + fh - 1, r.z1(), "minecraft:glass_pane");
            g.fill(corridor, y + 1, r.z0() + 1, corridor, y + fh - 1, r.z1() - 1, "minecraft:white_concrete");
            g.fill(corridor + 1, y + 1, r.z1(), r.x1() - 1, y + fh - 1, r.z1(), B.AIR);
            g.fill(corridor + 1, y + 1, r.z1(), r.x1() - 1, y + 1, r.z1(), "minecraft:oak_leaves[persistent=true]");
            g.set(r.x1(), y + 3, r.z1(), "minecraft:end_rod[facing=up]");
            g.set(corridor, y + 3, r.z1() + 1, "minecraft:end_rod[facing=up]");
            // Chambre : lit, tapis, table de chevet, lanterne, rangements.
            int bx = r.x0() + 2 + (w / 6);
            String color = beds[(f + (int) (seed & 3)) % beds.length];
            g.fill(r.x0() + 1, y + 1, r.z0() + 1, corridor - 1, y + 1, r.z1() - 2, B.carpet(color));
            g.set(bx, y + 1, r.z0() + 1, B.bedHead(color, B.N));
            g.set(bx, y + 1, r.z0() + 2, B.bedFoot(color, B.N));
            g.set(bx - 1, y + 1, r.z0() + 1, "minecraft:spruce_planks");
            g.set(bx - 1, y + 2, r.z0() + 1, LANTERN);
            g.set(bx + 2, y + 1, r.z0() + 1, "minecraft:bookshelf");
            g.set(bx + 2, y + 2, r.z0() + 1, "minecraft:flower_pot");
            g.set(corridor - 1, y + 1, r.z0() + 1, "minecraft:furnace[facing=south]");
            g.set(corridor - 1, y + 2, r.z0() + 1, "minecraft:barrel[facing=south]");
        }
        int roof = floors * fh;
        Arch.floor(g, r.grow(1), roof, "minecraft:gray_concrete");
        g.fill(r.x0() + 1, roof + 1, r.z0() + 1, r.cx() - 1, roof + 1, r.z1() - 1, "minecraft:daylight_detector[inverted=false]");
        g.fill(r.cx() + 1, roof + 1, r.z0() + 1, r.x1() - 1, roof + 1, r.z1() - 1, "minecraft:daylight_detector[inverted=false]");
        g.fill(corridor + 1, 1, r.z1(), r.x1() - 1, 2, r.z1(), B.AIR);
        Arch.door(g, (corridor + r.x1()) / 2, 1, r.z1(), "iron", B.S);
        g.fill(r.x0() - 1, 0, r.z1() + 1, r.x1() + 1, 0, r.z1() + 3, "minecraft:smooth_stone");
        for (int x = r.x0() - 1; x < corridor; x++) {
            g.set(x, 1, r.z1() + 2, "minecraft:oak_leaves[persistent=true]");
        }
        return g.freeze();
    }

    // ------------------------------------------------------------------ médiéval

    /** Chaumière en pierre au toit de chaume, tour-moulin à ailes, potager, bassin et chemin pavé. */
    static VoxelModel windmillCottage(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        int w = Math.max(15, width) | 1;
        House.Spec s = House.spec().walls("stone_bricks", "oak_log[axis=y]").trim("oak_log[axis=x]").foundation("cobblestone")
                .floorMat("oak_planks").door("oak").furniture("oak").roof(RoofType.GABLE, "bamboo_mosaic").overhang(1)
                .depth(0.6).with(Feature.CHIMNEY, Feature.LANTERNS, Feature.PORCH);
        Rect r = House.build(g, s, Math.max(9, w * 2 / 3), seed, -w / 6, 0, -w / 8);
        // Tour du moulin à l'arrière droite.
        int tx = r.x1() - 1, tz = r.z0() + 1;
        int th = 12 + w / 4;
        Rect tower = Rect.centered(5, 5).translate(tx, tz);
        Arch.room(g, tower, 0, th, "minecraft:cobblestone", "minecraft:oak_planks");
        Arch.pillars(g, tower, 0, th, "minecraft:stone_bricks");
        Arch.roof(g, RoofType.PYRAMID, tower, th + 1, RoofMat.of("cobblestone"), 1, "minecraft:cobblestone");
        int hubY = th - 2, hubZ = tower.z1() + 1;
        g.set(tx, hubY, hubZ, "minecraft:oak_log[axis=z]");
        int blade = 5 + w / 6;
        for (int k = 1; k <= blade; k++) {
            g.set(tx + k, hubY, hubZ + 1, "minecraft:oak_fence");
            g.set(tx - k, hubY, hubZ + 1, "minecraft:oak_fence");
            g.set(tx, hubY + k, hubZ + 1, "minecraft:oak_fence");
            g.set(tx, hubY - k, hubZ + 1, "minecraft:oak_fence");
            if (k > 1) {
                g.set(tx + k, hubY + 1, hubZ + 1, "minecraft:white_wool");
                g.set(tx - k, hubY - 1, hubZ + 1, "minecraft:white_wool");
                g.set(tx - 1, hubY + k, hubZ + 1, "minecraft:white_wool");
                g.set(tx + 1, hubY - k, hubZ + 1, "minecraft:white_wool");
            }
        }
        // Potager clos, citrouilles et pastèques, bassin et chemin pavé.
        int fz0 = r.z1() + 2, fz1 = r.z1() + 2 + Math.max(6, w / 3);
        cropPlot(g, r.x0(), fz0 + 1, r.cx() - 1, fz1 - 1, 0, "minecraft:wheat[age=7]");
        for (int x = r.cx() + 1; x <= r.x1(); x++) {
            for (int z = fz0 + 1; z < fz1; z++) {
                g.set(x, 0, z, "minecraft:farmland[moisture=7]");
                if ((x + z) % 2 == 0) {
                    g.set(x, 1, z, (x % 3 == 0) ? "minecraft:melon" : "minecraft:pumpkin");
                }
            }
        }
        fenceRect(g, r.x0() - 1, fz0, r.x1() + 1, fz1, 1, "minecraft:spruce_fence", true);
        for (int z = r.z0() - 2; z <= fz1 + 2; z++) {
            g.set(r.x0() - 3, 0, z, WATER);
            g.set(r.x0() - 4, 0, z, WATER);
            g.set(r.x0() - 3, -1, z, "minecraft:dirt");
            g.set(r.x0() - 4, -1, z, "minecraft:dirt");
        }
        String[] path = {"minecraft:cobblestone", "minecraft:gravel", "minecraft:andesite", "minecraft:coarse_dirt"};
        Rng rng = new Rng(seed);
        for (int z = r.z1() + 1; z <= fz1 + 3; z++) {
            for (int x = r.x1() + 2; x <= r.x1() + 4; x++) {
                g.set(x, 0, z, path[rng.nextInt(path.length)]);
            }
        }
        return g.freeze();
    }

    /** Château à quatre tours carrées et drapeaux rouge et blanc à damier. */
    static VoxelModel flagCastle(int width, long seed) {
        Castle.Spec s = Castle.spec().walls("stone_bricks", "cobblestone").towers(Castle.TowerShape.SQUARE, 1.2).roofs(null)
                .flag("red_wool").ground("grass_block", "dirt_path");
        VoxelGrid g = Castle.generate(s, Math.max(25, width), seed).toGrid();
        int w = Math.max(25, width);
        int half = w / 2;
        int[][] corners = {{-half, -half}, {half - 1, -half}, {-half, half - 1}, {half - 1, half - 1}};
        for (int[] c : corners) {
            int y = top(g, c[0], c[1], 10) + 1;
            g.fill(c[0], y, c[1], c[0], y + 5, c[1], "minecraft:dark_oak_fence");
            for (int fx = 1; fx <= 6; fx++) {
                for (int fy = 0; fy < 3; fy++) {
                    g.set(c[0] + fx, y + 3 + fy, c[1], (fx + fy) % 2 == 0 ? "minecraft:red_wool" : "minecraft:white_wool");
                }
            }
        }
        // Champs de blé de part et d'autre de l'entrée.
        int fz = half + 2;
        cropPlot(g, -half + 1, fz, -3, fz + 4, 0, "minecraft:wheat[age=7]");
        cropPlot(g, 3, fz, half - 1, fz + 4, 0, "minecraft:wheat[age=7]");
        return g.freeze();
    }

    /** Manoir fortifié : enceinte claire, maisons à colombages, tours coiffées de flèches d'ardoise. */
    static VoxelModel manorCastle(int width, long seed) {
        Castle.Spec s = Castle.spec().walls("diorite", "stone_bricks").roofs("deepslate_tiles").cone(3)
                .flag("white_wool").ground("grass_block", "gravel")
                .houses(House.spec().walls("white_terracotta", "spruce_log[axis=y]").trim("spruce_log[axis=x]")
                        .roof(RoofType.STEEP, "deepslate_tile").furniture("spruce").door("spruce").floors(2).with(Feature.LANTERNS))
                .keep(House.spec().walls("stone_bricks", "spruce_log[axis=y]").trim("stripped_spruce_log[axis=x]")
                        .roof(RoofType.STEEP, "deepslate_tile").floors(3).depth(0.9).furniture("spruce").door("spruce")
                        .with(Feature.ROOMS, Feature.LANTERNS, Feature.BALCONY));
        return Castle.generate(s, Math.max(25, width), seed);
    }
}
