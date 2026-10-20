package com.ultimateautobuilder.core.adventure;

import com.ultimateautobuilder.core.model.B;

/**
 * Sanctuaire OP : enceinte de quartz et de purpur fermée par une porte de coffre-fort OP,
 * jardin, grand hall à toit en pente, trois coffres verrouillés OP et deux coffres légendaires.
 */
final class SanctuaryGen {
    private SanctuaryGen() {
    }

    static Blueprint build(long seed) {
        Canvas c = new Canvas(seed);
        int s = 31, cx = s / 2;
        String wall = "minecraft:quartz_bricks", pillar = "minecraft:purpur_pillar[axis=y]", floor = "minecraft:polished_diorite";
        c.clear(1, 1, 1, s - 2, 8, s - 2);
        c.fill(0, 0, 0, s - 1, 0, s - 1, "minecraft:grass_block");
        c.walls(0, 1, 0, s - 1, 6, s - 1, wall);
        for (int x = 0; x < s; x += 6) {
            for (int y = 1; y <= 7; y++) {
                c.set(x, y, 0, pillar);
                c.set(x, y, s - 1, pillar);
                c.set(0, y, x, pillar);
                c.set(s - 1, y, x, pillar);
            }
        }
        c.crenellations(0, 0, s - 1, s - 1, 7, "minecraft:purpur_block");
        // Unique entrée : porte de coffre-fort OP.
        c.vaultDoor(cx, 1, s - 1, KeyTier.OP);
        c.clear(cx - 1, 1, s, cx + 1, 3, s + 3);
        for (int z = s - 1; z <= s + 3; z++) {
            c.set(cx, 0, z, "minecraft:polished_blackstone_bricks");
        }
        c.set(cx - 1, 3, s, B.wallTorch(B.S));
        c.set(cx + 1, 3, s, B.wallTorch(B.S));
        // Jardin et allée.
        for (int z = 18; z <= s - 2; z++) {
            c.set(cx, 0, z, "minecraft:polished_blackstone_bricks");
        }
        for (int i = 0; i < 4; i++) {
            int tx = i < 2 ? 5 : s - 6, tz = i % 2 == 0 ? 21 : 26;
            c.set(tx, 0, tz, "minecraft:dirt");
            for (int y = 1; y <= 3; y++) {
                c.set(tx, y, tz, B.log("cherry", 'y'));
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = 4; y <= 5; y++) {
                        if (Math.abs(dx) + Math.abs(dz) + (y - 4) <= 3 && !(dx == 0 && dz == 0 && y == 4)) {
                            c.set(tx + dx, y, tz + dz, B.leaves("cherry"));
                        }
                    }
                }
            }
            c.set(tx, 4, tz, B.log("cherry", 'y'));
        }
        // Grand hall.
        int hx0 = 5, hx1 = s - 6, hz0 = 2, hz1 = 16;
        c.fill(hx0, 0, hz0, hx1, 0, hz1, floor);
        c.clear(hx0 + 1, 1, hz0 + 1, hx1 - 1, 6, hz1 - 1);
        c.walls(hx0, 1, hz0, hx1, 6, hz1, wall);
        for (int y = 1; y <= 6; y++) {
            c.set(hx0, y, hz0, pillar);
            c.set(hx1, y, hz0, pillar);
            c.set(hx0, y, hz1, pillar);
            c.set(hx1, y, hz1, pillar);
        }
        for (int x = hx0 + 2; x <= hx1 - 2; x += 3) {
            c.set(x, 3, hz1, "minecraft:magenta_stained_glass_pane");
            c.set(x, 4, hz1, "minecraft:magenta_stained_glass_pane");
            c.set(x, 3, hz0, "minecraft:magenta_stained_glass_pane");
            c.set(x, 4, hz0, "minecraft:magenta_stained_glass_pane");
        }
        c.clear(cx - 1, 1, hz1, cx + 1, 4, hz1);
        c.gableRoof(hx0, hx1, hz0, hz1, 7, "purpur", "minecraft:purpur_block", "minecraft:purpur_block", 0);
        // Trésor : coffres OP verrouillés et coffres légendaires ouverts.
        c.lockedChest(cx - 4, 1, hz0 + 1, B.S, KeyTier.OP);
        c.lockedChest(cx, 1, hz0 + 1, B.S, KeyTier.OP);
        c.lockedChest(cx + 4, 1, hz0 + 1, B.S, KeyTier.OP);
        c.lootChest(hx0 + 1, 1, hz0 + 6, B.E, "chests/legendary");
        c.lootChest(hx1 - 1, 1, hz0 + 6, B.W, "chests/legendary");
        // Balise décorative sur pyramide de fer.
        c.fill(cx - 1, 1, hz0 + 6, cx + 1, 1, hz0 + 8, "minecraft:iron_block");
        c.set(cx, 2, hz0 + 7, "minecraft:beacon");
        for (int x = hx0 + 2; x <= hx1 - 2; x += 4) {
            c.set(x, 1, hz1 - 1, "minecraft:end_rod[facing=up]");
        }
        c.set(hx0 + 1, 1, hz1 - 1, "minecraft:amethyst_block");
        c.set(hx1 - 1, 1, hz1 - 1, "minecraft:amethyst_block");
        c.set(hx0 + 1, 2, hz1 - 1, "minecraft:amethyst_cluster[facing=up,waterlogged=false]");
        c.set(hx1 - 1, 2, hz1 - 1, "minecraft:amethyst_cluster[facing=up,waterlogged=false]");
        return c.freeze(StructureType.OP_SANCTUARY, new int[]{cx, 1, s + 3});
    }
}
