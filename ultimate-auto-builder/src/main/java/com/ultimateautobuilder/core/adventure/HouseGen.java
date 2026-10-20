package com.ultimateautobuilder.core.adventure;

import com.ultimateautobuilder.core.model.B;

/**
 * Maisons d'exploration : chaque type modifie réellement l'architecture (matériaux, état,
 * cave, pièce secrète, passage, étage, clôture, végétation) et non la seule couleur.
 * Repère : plancher en y = 0, façade et porte vers +Z, x ∈ [0, w-1], z ∈ [0, d-1].
 */
final class HouseGen {
    private static final String[] WOODS = {"oak", "spruce", "birch", "dark_oak"};

    private HouseGen() {
    }

    /** Paramètres résolus d'une maison. */
    private static final class P {
        int w = 9, d = 7, floors = 1;
        String wood = "oak";
        String wall, corner, foundation, floor, roofMat, gable;
        boolean lit = true, cobwebs, brokenWindows, ruined, overgrown, forest, fence, cellar, hiddenPassage, secretRoom,
                hiddenChest, rare, legendary;
        double roofHoles;
        String mainLoot = "chests/common";
    }

    static Blueprint build(StructureType type, long seed) {
        Canvas c = new Canvas(seed);
        P p = params(type, c);
        int w = p.w, d = p.d, cx = w / 2;
        int storey = 5;
        int top = p.floors * storey - 1;

        // Soubassement et plancher.
        c.fill(0, 0, 0, w - 1, 0, d - 1, p.foundation);
        c.fill(1, 0, 1, w - 2, 0, d - 2, p.floor);
        // Pièces vidées avant le mobilier.
        c.clear(1, 1, 1, w - 2, top, d - 2);
        // Murs et poteaux d'angle.
        c.walls(0, 1, 0, w - 1, top, d - 1, p.wall);
        for (int y = 1; y <= top; y++) {
            c.set(0, y, 0, p.corner);
            c.set(w - 1, y, 0, p.corner);
            c.set(0, y, d - 1, p.corner);
            c.set(w - 1, y, d - 1, p.corner);
        }
        for (int f = 1; f < p.floors; f++) {
            c.fill(1, f * storey, 1, w - 2, f * storey, d - 2, B.planks(p.wood));
        }
        // Fenêtres sur chaque étage.
        for (int f = 0; f < p.floors; f++) {
            int y0 = f * storey + 2;
            for (int x = 2; x <= w - 3; x += 3) {
                if (Math.abs(x - cx) > 1) {
                    window(c, p, x, y0, d - 1);
                }
                window(c, p, x, y0, 0);
            }
            for (int z = 2; z <= d - 3; z += 3) {
                window(c, p, 0, y0, z);
                window(c, p, w - 1, y0, z);
            }
        }
        // Porte, perron et allée.
        c.door(cx, 1, d - 1, p.wood, B.S);
        c.clear(cx - 1, 1, d, cx + 1, 3, d + 2);
        for (int z = d; z <= d + 2; z++) {
            c.set(cx, 0, z, p.ruined ? "minecraft:gravel" : "minecraft:dirt_path");
        }

        furnish(c, p, w, d, storey);
        if (p.floors > 1) {
            // Escalier intérieur vers l'étage, le long du mur arrière (devant la pièce secrète si elle existe).
            int sz = p.secretRoom ? 4 : 1;
            c.stairsEast(1, 1, sz, 5, p.wood, storey);
            upstairs(c, p, w, d, storey);
        }
        if (p.cellar || p.hiddenPassage) {
            cellar(c, p, w, d);
        }
        if (p.secretRoom) {
            secretRoom(c, p, w, d);
        }
        if (p.hiddenChest) {
            // Coffre encastré dans le plancher, recouvert d'un tapis.
            c.lootChest(w - 3, 0, d - 3, B.N, "chests/uncommon");
            c.set(w - 3, 1, d - 3, B.carpet("brown"));
        }

        int roofY = top + 1;
        if (!p.ruined) {
            c.gableRoof(0, w - 1, 0, d - 1, roofY, p.roofMat, p.gable, B.planks(p.roofMat), p.roofHoles);
        } else {
            ruin(c, p, w, d, top);
        }
        if (p.overgrown || p.forest) {
            overgrow(c, p, w, d, top);
        }
        if (p.fence) {
            fence(c, w, d, cx);
        }
        return c.freeze(type, new int[]{cx, 1, d + 2});
    }

    private static P params(StructureType type, Canvas c) {
        P p = new P();
        p.wood = WOODS[c.rng.nextInt(WOODS.length)];
        p.w = 7 + 2 * c.rng.nextInt(2);
        p.d = 7 + c.rng.nextInt(2);
        p.wall = B.planks(p.wood);
        p.corner = B.log(p.wood, 'y');
        p.foundation = "minecraft:cobblestone";
        p.floor = B.planks(p.wood.equals("dark_oak") ? "spruce" : p.wood);
        p.roofMat = p.wood.equals("birch") ? "spruce" : "dark_oak";
        p.gable = B.planks(p.wood);
        switch (type) {
            case STONE_HOUSE -> {
                p.wall = "minecraft:stone_bricks";
                p.corner = "minecraft:polished_andesite";
                p.foundation = "minecraft:cobblestone";
                p.roofMat = "stone_brick";
                p.gable = "minecraft:stone_bricks";
                p.w = 9;
            }
            case ABANDONED_HOUSE -> {
                p.lit = false;
                p.cobwebs = true;
                p.brokenWindows = true;
                p.wood = "spruce";
                p.wall = B.planks("spruce");
                p.corner = B.log("spruce", 'y');
                p.mainLoot = "chests/common";
            }
            case DILAPIDATED_HOUSE -> {
                p.lit = false;
                p.cobwebs = true;
                p.brokenWindows = true;
                p.roofHoles = 0.18;
                p.foundation = "minecraft:mossy_cobblestone";
            }
            case ISOLATED_HOUSE -> {
                p.fence = true;
                p.w = 9;
            }
            case FOREST_HOUSE -> {
                p.forest = true;
                p.wood = "spruce";
                p.wall = B.planks("spruce");
                p.corner = B.log("spruce", 'y');
                p.mainLoot = "chests/uncommon";
            }
            case OVERGROWN_HOUSE -> {
                p.overgrown = true;
                p.foundation = "minecraft:mossy_cobblestone";
                p.lit = false;
            }
            case HOUSE_RUINS -> {
                p.ruined = true;
                p.lit = false;
                p.overgrown = true;
                p.wall = "minecraft:cobblestone";
                p.corner = "minecraft:mossy_cobblestone";
                p.foundation = "minecraft:mossy_cobblestone";
            }
            case CELLAR_HOUSE -> p.cellar = true;
            case SECRET_ROOM_HOUSE -> {
                p.secretRoom = true;
                p.d = 10;
            }
            case HIDDEN_CHEST_HOUSE -> p.hiddenChest = true;
            case HIDDEN_PASSAGE_HOUSE -> p.hiddenPassage = true;
            case RARE_HOUSE -> {
                p.rare = true;
                p.floors = 2;
                p.w = 11;
                p.d = 9;
                p.wall = "minecraft:white_terracotta";
                p.mainLoot = "chests/rare";
            }
            case LEGENDARY_HOUSE -> {
                p.legendary = true;
                p.floors = 2;
                p.w = 13;
                p.d = 11;
                p.wood = "dark_oak";
                p.wall = "minecraft:polished_deepslate";
                p.corner = "minecraft:stripped_dark_oak_log[axis=y]";
                p.foundation = "minecraft:deepslate_bricks";
                p.floor = B.planks("dark_oak");
                p.roofMat = "deepslate_tile";
                p.gable = "minecraft:deepslate_tiles";
                p.secretRoom = true;
                p.d = 13;
                p.mainLoot = "chests/rare";
            }
            case STARTER_HOUSE -> {
                // Maison de départ : chêne, plus large, coffre vide, éclairée.
                p.wood = "oak";
                p.w = 9;
                p.d = 7;
                p.wall = B.planks("oak");
                p.corner = B.log("oak", 'y');
                p.floor = B.planks("spruce");
                p.roofMat = "spruce";
                p.gable = B.planks("oak");
                p.mainLoot = null;
            }
            default -> {
            }
        }
        return p;
    }

    private static void window(Canvas c, P p, int x, int y, int z) {
        for (int dy = 0; dy < 2; dy++) {
            String s;
            if (p.brokenWindows && c.rng.chance(0.4)) {
                s = c.rng.chance(0.5) ? Canvas.CLEAR : "minecraft:cobweb";
            } else {
                s = B.pane(null);
            }
            c.set(x, y + dy, z, s);
        }
    }

    private static void furnish(Canvas c, P p, int w, int d, int storey) {
        // Lit contre le mur arrière droit, tête vers le nord.
        c.set(w - 2, 1, 1, B.bedHead(p.legendary ? "black" : "red", B.N));
        c.set(w - 2, 1, 2, B.bedFoot(p.legendary ? "black" : "red", B.N));
        c.set(w - 2, 1, d - 2, "minecraft:crafting_table");
        c.set(1, 1, d - 2, "minecraft:furnace[facing=east,lit=false]");
        if (p.mainLoot != null) {
            c.lootChest(1, 1, d / 2, B.E, p.mainLoot);
        } else {
            c.set(1, 1, d / 2, "minecraft:chest[facing=east,type=single,waterlogged=false]");
        }
        c.set(1, 1, d / 2 + 1, "minecraft:barrel[facing=up,open=false]");
        if (p.lit) {
            // Lanterne posée sur le tonneau : aucun support à risque sous le toit.
            c.set(1, 2, d / 2 + 1, B.lantern(false));
        }
        if (p.cobwebs) {
            for (int i = 0; i < 6; i++) {
                int x = 1 + c.rng.nextInt(w - 2), z = 1 + c.rng.nextInt(d - 2);
                if (Canvas.CLEAR.equals(c.get(x, 4, z))) {
                    c.set(x, 4, z, "minecraft:cobweb");
                }
            }
        }
        if (!p.cobwebs) {
            c.set(w / 2, 1, d / 2, B.carpet(p.legendary ? "purple" : "red"));
            c.set(w - 2, 1, d / 2, "minecraft:bookshelf");
            c.set(w - 2, 2, d / 2, "minecraft:flower_pot");
        }
    }

    private static void upstairs(Canvas c, P p, int w, int d, int storey) {
        int y = storey + 1;
        c.lootChest(w - 2, y, d - 2, B.W, p.legendary ? "chests/legendary" : "chests/rare");
        // Coffre verrouillé : clé peu commune (maison rare) ou légendaire (maison légendaire).
        c.lockedChest(w - 2, y, d - 3, B.W, p.legendary ? KeyTier.LEGENDARY : KeyTier.UNCOMMON);
        c.set(w - 2, y, 2, "minecraft:enchanting_table");
        c.set(w - 3, y, d - 2, "minecraft:bookshelf");
        c.set(w - 3, y + 1, d - 2, B.lantern(false));
        if (p.legendary) {
            c.set(w / 2, y, d - 2, "minecraft:gold_block");
        }
    }

    /** Cave sous la maison, accès par une trappe (cachée sous un tapis pour le passage dissimulé). */
    private static void cellar(Canvas c, P p, int w, int d) {
        int y0 = -5;
        c.fill(0, y0, 0, w - 1, -1, d - 1, "minecraft:cobblestone");
        c.clear(1, y0 + 1, 1, w - 2, -1, d - 2);
        int lx = 2, lz = 1;
        c.set(lx, 0, lz, B.trapdoor(p.wood, B.S, true, false));
        if (p.hiddenPassage) {
            c.set(lx, 1, lz, B.carpet("brown"));
        }
        for (int y = y0 + 1; y <= -1; y++) {
            c.set(lx, y, lz, "minecraft:ladder[facing=south]");
        }
        c.lootChest(w - 2, y0 + 1, d - 2, B.W, p.hiddenPassage ? "chests/rare" : "chests/uncommon");
        c.set(w - 2, y0 + 1, d - 3, "minecraft:barrel[facing=up,open=false]");
        c.set(w / 2, -1, d / 2, B.lantern(true));
    }

    /** Pièce secrète derrière une cloison : deux bibliothèques ciselées à casser. */
    private static void secretRoom(Canvas c, P p, int w, int d) {
        int zPart = 3;
        for (int x = 1; x <= w - 2; x++) {
            for (int y = 1; y <= 4; y++) {
                c.set(x, y, zPart, p.wall);
            }
        }
        int sx = w / 2;
        c.set(sx, 1, zPart, "minecraft:chiseled_bookshelf[facing=south,slot_0_occupied=false,slot_1_occupied=false,slot_2_occupied=false,"
                + "slot_3_occupied=false,slot_4_occupied=false,slot_5_occupied=false]");
        c.set(sx, 2, zPart, "minecraft:chiseled_bookshelf[facing=south,slot_0_occupied=false,slot_1_occupied=false,slot_2_occupied=false,"
                + "slot_3_occupied=false,slot_4_occupied=false,slot_5_occupied=false]");
        // Le lit d'origine (mur arrière) est déplacé : la pièce secrète occupe z ∈ [1, 2].
        c.clear(1, 1, 1, w - 2, 4, 2);
        c.lootChest(1, 1, 1, B.E, p.legendary ? "chests/legendary" : "chests/rare");
        c.set(w - 2, 1, 1, B.lantern(false));
        c.set(w - 2, 1, d - 4, B.bedHead("red", B.N));
        c.set(w - 2, 1, d - 3, B.bedFoot("red", B.N));
    }

    private static void ruin(Canvas c, P p, int w, int d, int top) {
        // Murs coupés à des hauteurs irrégulières, pas de toit.
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (!edge) {
                    continue;
                }
                int h = 1 + c.rng.nextInt(top);
                for (int y = h + 1; y <= top; y++) {
                    String s = c.get(x, y, z);
                    if (s != null && !s.contains("door")) {
                        c.set(x, y, z, Canvas.CLEAR);
                    }
                }
            }
        }
        c.weather(0, 0, 0, w - 1, top, d - 1, "minecraft:cobblestone", new String[]{"minecraft:mossy_cobblestone"}, 0.4);
    }

    private static void overgrow(Canvas c, P p, int w, int d, int top) {
        // Lierre accroché aux façades et feuillage sur le toit.
        for (int i = 0; i < w * 2; i++) {
            int x = 1 + c.rng.nextInt(w - 2);
            int y = 1 + c.rng.nextInt(top);
            if (c.get(x, y, -1) == null) {
                c.set(x, y, -1, "minecraft:vine[north=false,south=true,east=false,west=false,up=false]");
            }
            if (c.get(x, y, d) == null && Math.abs(x - w / 2) > 1) {
                c.set(x, y, d, "minecraft:vine[north=true,south=false,east=false,west=false,up=false]");
            }
        }
        if (p.forest && !p.ruined) {
            for (int x = -1; x <= w; x++) {
                if (c.rng.chance(0.5)) {
                    c.set(x, top + 2, -1, B.leaves("spruce"));
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            int x = -2 + c.rng.nextInt(w + 4);
            int z = d + 1 + c.rng.nextInt(2);
            if (Math.abs(x - w / 2) > 1) {
                c.set(x, 1, z, c.rng.chance(0.5) ? "minecraft:fern" : "minecraft:poppy");
            }
        }
    }

    private static void fence(Canvas c, int w, int d, int cx) {
        int x0 = -3, x1 = w + 2, z0 = -3, z1 = d + 3;
        for (int x = x0; x <= x1; x++) {
            c.set(x, 1, z0, "minecraft:oak_fence");
            if (x != cx) {
                c.set(x, 1, z1, "minecraft:oak_fence");
            }
        }
        for (int z = z0; z <= z1; z++) {
            c.set(x0, 1, z, "minecraft:oak_fence");
            c.set(x1, 1, z, "minecraft:oak_fence");
        }
        c.set(cx, 1, z1, "minecraft:oak_fence_gate[facing=south,in_wall=false,open=false,powered=false]");
        c.set(cx, 0, z1, "minecraft:dirt_path");
        c.set(x0 + 1, 1, z0 + 1, B.lantern(false));
    }
}
