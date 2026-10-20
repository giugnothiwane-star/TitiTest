package com.ultimateautobuilder.core.adventure;

import com.ultimateautobuilder.core.model.B;

/**
 * Châteaux et forteresses : enceinte crénelée, tours d'angle à échelle, porte, cour, donjon avec
 * salle des trois spawners (bébé, moyen, normal), escalier vers l'étage et salle du trésor
 * fermée par une porte de coffre-fort. La forteresse souterraine reprend le donjon sous terre,
 * desservi par un escalier-tunnel depuis une guérite.
 */
final class CastleGen {
    private CastleGen() {
    }

    private static final class P {
        int size = 33;
        int hallH = 5;
        String wall = "minecraft:stone_bricks", accent = "minecraft:cobblestone", floor = "minecraft:spruce_planks",
                ground = "minecraft:gravel", light = "minecraft:lantern[hanging=false]", stairMat = "stone_brick";
        boolean spawners = true, warden, ruined, cursed, legendary, weathered, underground;
        SpawnFamily family = SpawnFamily.MIXED;
        KeyTier vault;
        String chest = "chests/uncommon", treasure = "chests/rare";
    }

    static Blueprint build(StructureType type, long seed) {
        Canvas c = new Canvas(seed);
        P p = params(type, c);
        if (p.underground) {
            return underground(c, p, type);
        }
        int s = p.size, cx = s / 2;
        int wallH = 8, towerH = 13;
        // Cour vidée et sol.
        c.clear(1, 1, 1, s - 2, wallH + 2, s - 2);
        for (int x = 0; x < s; x++) {
            for (int z = 0; z < s; z++) {
                c.set(x, 0, z, c.rng.chance(0.3) ? p.accent : p.ground);
            }
        }
        // Enceinte.
        c.walls(0, 1, 0, s - 1, 0 + wallH, s - 1, p.wall);
        c.crenellations(0, 0, s - 1, s - 1, wallH + 1, p.wall);
        // Porte d'entrée.
        c.clear(cx - 1, 1, s - 1, cx + 1, 4, s - 1);
        c.set(cx - 2, 3, s, B.wallTorch(B.S));
        c.set(cx + 2, 3, s, B.wallTorch(B.S));
        c.clear(cx - 1, 1, s, cx + 1, 3, s + 3);
        for (int z = s - 1; z <= s + 3; z++) {
            for (int x = cx - 1; x <= cx + 1; x++) {
                c.set(x, 0, z, p.accent);
            }
        }
        for (int z = s - 2; z >= s - 12; z--) {
            c.set(cx, 0, z, p.accent);
        }
        // Tours d'angle.
        tower(c, p, 0, 0, towerH, true, true);
        tower(c, p, s - 7, 0, towerH, false, true);
        tower(c, p, 0, s - 7, towerH, true, false);
        tower(c, p, s - 7, s - 7, towerH, false, false);
        // Donjon.
        keep(c, p, s, 0);
        if (p.cursed) {
            for (int i = 0; i < s * 2; i++) {
                int x = 1 + c.rng.nextInt(s - 2), z = 1 + c.rng.nextInt(s - 2);
                if (p.ground.equals(c.get(x, 0, z)) || p.accent.equals(c.get(x, 0, z))) {
                    c.set(x, 0, z, "minecraft:sculk");
                }
            }
        }
        if (p.weathered || p.ruined) {
            String cracked = p.wall.equals("minecraft:stone_bricks") ? "minecraft:cracked_stone_bricks" : p.accent;
            String mossy = p.wall.equals("minecraft:stone_bricks") ? "minecraft:mossy_stone_bricks" : p.accent;
            c.weather(0, 1, 0, s - 1, towerH + 1, s - 1, p.wall, new String[]{cracked, mossy}, p.ruined ? 0.35 : 0.15);
        }
        if (p.ruined) {
            ruinTops(c, s, wallH, towerH);
        }
        return c.freeze(type, new int[]{cx, 1, s + 3});
    }

    private static P params(StructureType type, Canvas c) {
        P p = new P();
        switch (type) {
            case SMALL_FORTRESS -> {
                p.size = 25;
                p.family = c.rng.chance(0.5) ? SpawnFamily.UNDEAD : SpawnFamily.ARACHNID;
                p.chest = "chests/uncommon";
            }
            case ABANDONED_CASTLE -> {
                p.weathered = true;
                p.family = SpawnFamily.UNDEAD;
                p.vault = KeyTier.UNCOMMON;
                p.chest = "chests/uncommon";
                p.treasure = "chests/rare";
            }
            case RUINED_CASTLE -> {
                p.ruined = true;
                p.spawners = false;
                p.chest = "chests/uncommon";
            }
            case UNDERGROUND_FORTRESS -> {
                p.underground = true;
                p.family = c.rng.chance(0.5) ? SpawnFamily.ARACHNID : SpawnFamily.UNDEAD;
                p.vault = KeyTier.RARE;
                p.wall = "minecraft:deepslate_bricks";
                p.accent = "minecraft:cobbled_deepslate";
                p.floor = "minecraft:polished_deepslate";
                p.stairMat = "deepslate_brick";
                p.chest = "chests/rare";
                p.treasure = "chests/rare";
            }
            case CURSED_CASTLE -> {
                p.cursed = true;
                p.warden = true;
                p.hallH = 7;
                p.family = SpawnFamily.MIXED;
                p.vault = KeyTier.RARE;
                p.wall = "minecraft:deepslate_bricks";
                p.accent = "minecraft:cracked_deepslate_bricks";
                p.floor = "minecraft:polished_blackstone";
                p.ground = "minecraft:coarse_dirt";
                p.light = "minecraft:soul_lantern[hanging=false]";
                p.stairMat = "deepslate_brick";
                p.chest = "chests/rare";
                p.treasure = "chests/legendary";
            }
            case LEGENDARY_CASTLE -> {
                p.legendary = true;
                p.warden = true;
                p.size = 41;
                p.hallH = 7;
                p.family = SpawnFamily.MIXED;
                p.vault = KeyTier.LEGENDARY;
                p.wall = "minecraft:quartz_bricks";
                p.accent = "minecraft:chiseled_quartz_block";
                p.floor = "minecraft:polished_andesite";
                p.ground = "minecraft:grass_block";
                p.stairMat = "quartz";
                p.chest = "chests/rare";
                p.treasure = "chests/legendary";
            }
            default -> {
            }
        }
        return p;
    }

    /** Tour carrée 7×7 creuse, échelle intérieure jusqu'à la plate-forme crénelée, porte vers la cour. */
    private static void tower(Canvas c, P p, int x0, int z0, int h, boolean west, boolean north) {
        int x1 = x0 + 6, z1 = z0 + 6;
        c.clear(x0 + 1, 1, z0 + 1, x1 - 1, h - 1, z1 - 1);
        c.walls(x0, 1, z0, x1, h, z1, p.wall);
        c.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, p.floor);
        c.fill(x0 + 1, h, z0 + 1, x1 - 1, h, z1 - 1, p.wall);
        c.crenellations(x0, z0, x1, z1, h + 1, p.wall);
        // Porte côté cour.
        int dx = west ? x1 : x0, dz = z0 + 3;
        c.clear(dx, 1, dz, dx, 2, dz);
        // Échelle contre le mur extérieur de la tour.
        int lx = west ? x0 + 1 : x1 - 1;
        int lz = north ? z0 + 1 : z1 - 1;
        String face = north ? B.S : B.N;
        for (int y = 1; y <= h; y++) {
            c.set(lx, y, lz, "minecraft:ladder[facing=" + face + "]");
        }
        // Meurtrières.
        for (int y = 4; y <= h - 3; y += 4) {
            c.set(x0 + 3, y, z0, "minecraft:iron_bars");
            c.set(x0 + 3, y, z1, "minecraft:iron_bars");
            c.set(x0, y, z0 + 3, "minecraft:iron_bars");
            c.set(x1, y, z0 + 3, "minecraft:iron_bars");
        }
        c.set(west ? x1 - 1 : x0 + 1, 1, north ? z1 - 1 : z0 + 1, p.light);
        if (west && north) {
            c.lootChest(x0 + 3, 1, z0 + 3, B.S, p.chest);
        }
    }

    /** Donjon adossé au mur arrière (zBase = 0) ou souterrain (yBase négatif). */
    private static int[] keep(Canvas c, P p, int s, int yBase) {
        int kx0 = 8, kx1 = s - 9, kz0 = 1;
        int depth = s >= 41 ? 16 : s >= 33 ? 12 : 10;
        int kz1 = kz0 + depth - 1;
        int cx = s / 2;
        int hall = p.hallH;
        int f2 = yBase + hall + 1;
        int top = f2 + 4;
        c.clear(kx0 + 1, yBase + 1, kz0 + 1, kx1 - 1, top - 1, kz1 - 1);
        c.fill(kx0, yBase, kz0, kx1, yBase, kz1, p.floor);
        c.walls(kx0, yBase + 1, kz0, kx1, top, kz1, p.wall);
        c.fill(kx0 + 1, f2, kz0 + 1, kx1 - 1, f2, kz1 - 1, p.floor);
        c.fill(kx0, top, kz0, kx1, top, kz1, p.wall);
        if (!p.ruined) {
            c.crenellations(kx0, kz0, kx1, kz1, top + 1, p.wall);
        }
        // Grande porte.
        c.clear(cx - 1, yBase + 1, kz1, cx + 1, yBase + 3, kz1);
        // Salle des spawners : trois spawners principaux, un par catégorie.
        int sz = (kz0 + kz1) / 2 + 1;
        if (p.spawners) {
            c.spawner(kx0 + 3, yBase + 1, sz, SpawnCategory.BABY, p.family, p.warden);
            c.spawner(cx, yBase + 1, sz, SpawnCategory.MEDIUM, p.family, p.warden);
            c.spawner(kx1 - 3, yBase + 1, sz, SpawnCategory.NORMAL, p.family, p.warden);
        }
        for (int x = kx0 + 2; x <= kx1 - 2; x += 4) {
            c.set(x, yBase + 1, kz1 - 1, p.light);
        }
        // Escalier vers l'étage, le long du mur arrière.
        int n = hall + 1;
        c.stairsEast(kx0 + 1, yBase + 1, kz0 + 1, n, p.stairMat, f2);
        // Étage : antichambre puis salle du trésor derrière la porte de coffre-fort.
        int px = kx0 + n + 2;
        int ty = f2 + 1;
        if (p.vault != null && px <= kx1 - 3) {
            for (int z = kz0 + 1; z <= kz1 - 1; z++) {
                for (int y = ty; y <= top - 1; y++) {
                    c.set(px, y, z, p.wall);
                }
            }
            int vz = (kz0 + kz1) / 2;
            c.vaultDoor(px, ty, vz, p.vault);
            c.lockedChest(kx1 - 1, ty, vz, B.W, p.vault);
            c.lootChest(kx1 - 1, ty, vz - 2, B.W, p.treasure);
            c.set(kx1 - 1, ty, vz + 2, p.legendary ? "minecraft:gold_block" : "minecraft:barrel[facing=up,open=false]");
            c.set(px + 1, ty, kz1 - 1, p.light);
        } else {
            c.lootChest(kx1 - 1, ty, (kz0 + kz1) / 2, B.W, p.chest);
        }
        c.lootChest(kx0 + 1, ty, kz1 - 1, B.N, p.chest);
        c.set(kx0 + 2, ty, kz1 - 1, p.light);
        // Fenêtres de l'étage.
        for (int x = kx0 + 3; x <= kx1 - 3; x += 4) {
            c.set(x, ty + 1, kz1, "minecraft:iron_bars");
        }
        if (p.cursed) {
            for (int i = 0; i < 10; i++) {
                int x = kx0 + 1 + c.rng.nextInt(kx1 - kx0 - 1), z = kz0 + 1 + c.rng.nextInt(depth - 2);
                if (Canvas.CLEAR.equals(c.get(x, f2 - 1, z))) {
                    c.set(x, f2 - 1, z, "minecraft:cobweb");
                }
            }
        }
        return new int[]{kx0, kz0, kx1, kz1};
    }

    private static void ruinTops(Canvas c, int s, int wallH, int towerH) {
        for (int x = 0; x < s; x++) {
            for (int z = 0; z < s; z++) {
                boolean edge = x == 0 || z == 0 || x == s - 1 || z == s - 1;
                if (!edge) {
                    continue;
                }
                int cut = 2 + c.rng.nextInt(wallH);
                for (int y = cut; y <= towerH + 1; y++) {
                    String st = c.get(x, y, z);
                    if (st != null && !Canvas.CLEAR.equals(st) && !st.contains("ladder")) {
                        c.set(x, y, z, Canvas.CLEAR);
                    }
                }
            }
        }
    }

    /**
     * Forteresse souterraine : donjon sous terre (salle y = -11..-7) relié à la surface par un
     * escalier-tunnel de trois blocs de large et une guérite.
     */
    private static Blueprint underground(Canvas c, P p, StructureType type) {
        int s = 27, cx = s / 2;
        int yBase = -12;
        int[] k = keep(c, p, s, yBase);
        int kz1 = k[3];
        // Coquille de pierre autour du donjon : aucune cavité naturelle ne s'y ouvre.
        for (int y = yBase - 1; y <= yBase + p.hallH + 6; y++) {
            for (int x = k[0] - 1; x <= k[2] + 1; x++) {
                for (int z = k[1] - 1; z <= kz1 + 1; z++) {
                    c.g.setIfEmpty(x, y, z, p.accent);
                }
            }
        }
        // Couloir puis escalier vers la surface.
        int z0 = kz1 + 1;
        for (int z = z0; z <= z0 + 2; z++) {
            c.fill(cx - 1, yBase, z, cx + 1, yBase, z, p.floor);
            c.clear(cx - 1, yBase + 1, z, cx + 1, yBase + 3, z);
            for (int y = yBase; y <= yBase + 4; y++) {
                c.set(cx - 2, y, z, p.wall);
                c.set(cx + 2, y, z, p.wall);
            }
            c.fill(cx - 1, yBase + 4, z, cx + 1, yBase + 4, z, p.wall);
        }
        c.set(cx - 1, yBase + 1, z0 + 1, p.light);
        int n = -yBase;
        c.tunnelStairsSouth(cx, yBase + 1, z0 + 3, n, p.stairMat, p.wall, yBase);
        int exitZ = z0 + 3 + n;
        // Guérite de surface : son mur arrière est en retrait de la trémie pour laisser passer la tête.
        c.fill(cx - 3, 0, exitZ - 5, cx + 3, 0, exitZ + 2, p.accent);
        c.walls(cx - 3, 1, exitZ - 5, cx + 3, 4, exitZ + 2, p.wall);
        c.clear(cx - 2, 1, exitZ - 4, cx + 2, 4, exitZ + 1);
        c.fill(cx - 3, 5, exitZ - 5, cx + 3, 5, exitZ + 2, p.wall);
        c.crenellations(cx - 3, exitZ - 5, cx + 3, exitZ + 2, 6, p.wall);
        // Trémie : ouverture du sol au-dessus des dernières marches et marche finale au niveau du sol.
        for (int z = exitZ - 4; z <= exitZ - 2; z++) {
            c.clear(cx - 1, 0, z, cx + 1, 0, z);
        }
        for (int dx = -1; dx <= 1; dx++) {
            c.set(cx + dx, 0, exitZ - 1, B.stairs(p.stairMat, B.S, false));
        }
        c.clear(cx - 1, 1, exitZ + 2, cx + 1, 3, exitZ + 2);
        c.set(cx + 2, 1, exitZ + 1, p.light);
        return c.freeze(type, new int[]{cx, 1, exitZ + 4});
    }
}
