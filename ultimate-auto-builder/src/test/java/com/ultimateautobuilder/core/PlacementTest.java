package com.ultimateautobuilder.core;

import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.StateRotation;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import com.ultimateautobuilder.core.validation.Limits;
import com.ultimateautobuilder.core.validation.StructureValidator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementTest {
    private static VoxelModel lShape() {
        VoxelGrid g = new VoxelGrid();
        g.fill(0, 0, 0, 4, 0, 0, "minecraft:stone");
        g.fill(0, 0, 1, 0, 0, 2, "minecraft:dirt");
        g.set(0, 3, 0, "minecraft:gold_block");
        return g.freeze();
    }

    @Test
    void gridStoresAndFreezesSortedCells() {
        VoxelGrid g = new VoxelGrid();
        g.set(-20, 5, 3, "minecraft:stone");
        g.set(10, -2, -7, "minecraft:dirt");
        g.set(10, -2, -7, "minecraft:grass_block");
        assertEquals(2, g.count());
        g.remove(10, -2, -7);
        assertEquals(1, g.count());
        assertNull(g.get(10, -2, -7));
        g.set(0, 0, 0, "minecraft:dirt");
        VoxelModel m = g.freeze();
        assertEquals(2, m.size());
        assertEquals(2, m.paletteSize(), "la palette doit être compactée");
        assertTrue(VoxelModel.cy(m.cell(0)) <= VoxelModel.cy(m.cell(1)), "ordre bas → haut");
        assertEquals("minecraft:stone", m.stateAt(m.anchorX() - 20, m.anchorY() + 5, m.anchorZ() + 3));
    }

    @Test
    void fourQuarterTurnsAreIdentity() {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                long p = Placement.rotateXZ(x, z, 4, false);
                assertEquals(x, (int) (p >> 32));
                assertEquals(z, (int) p);
            }
        }
        long r1 = Placement.rotateXZ(0, 1, 1, false);
        assertEquals(-1, (int) (r1 >> 32), "rotation horaire : +Z → -X");
        assertEquals(0, (int) r1);
        long m = Placement.rotateXZ(2, 1, 0, true);
        assertEquals(-2, (int) (m >> 32));
    }

    @Test
    void rotatedPlacementKeepsBlockCountAndSwapsFootprint() {
        VoxelModel m = lShape();
        for (int rot = 0; rot < 4; rot++) {
            Placement p = new Placement(m, 100, 64, -50, rot, false);
            Set<String> seen = new HashSet<>();
            int[] b = p.bounds();
            for (int i = 0; i < m.size(); i++) {
                long c = m.cell(i);
                int x = p.worldX(c), y = p.worldY(c), z = p.worldZ(c);
                assertTrue(x >= b[0] && x <= b[3] && y >= b[1] && y <= b[4] && z >= b[2] && z <= b[5], "hors boîte");
                assertTrue(seen.add(x + "," + y + "," + z), "collision après rotation");
            }
            int[] size = p.rotatedSize();
            if (rot % 2 == 0) {
                assertArrayEquals(new int[]{m.sizeX(), m.sizeY(), m.sizeZ()}, size);
            } else {
                assertArrayEquals(new int[]{m.sizeZ(), m.sizeY(), m.sizeX()}, size);
            }
        }
    }

    @Test
    void facadeFacesTheViewer() {
        assertEquals(0, Placement.rotationFacingViewer("north"));
        assertEquals(2, Placement.rotationFacingViewer("south"));
        // Joueur tourné vers l'est : la façade (+Z) doit regarder vers l'ouest (-X).
        long front = Placement.rotateXZ(0, 1, Placement.rotationFacingViewer("east"), false);
        assertEquals(-1, (int) (front >> 32));
    }

    @Test
    void validatorDetectsWorldLimitsAndGiantBuilds() {
        VoxelModel m = lShape();
        Placement ok = new Placement(m, 0, 64, 0, 0, false);
        assertFalse(StructureValidator.validate(ok, Limits.DEFAULT, -64, 320).hasErrors());
        Placement tooHigh = new Placement(m, 0, 318, 0, 0, false);
        assertTrue(StructureValidator.validate(tooHigh, Limits.DEFAULT, -64, 320).hasErrors());
        Placement tooLow = new Placement(m, 0, -70, 0, 0, false);
        assertTrue(StructureValidator.validate(tooLow, Limits.DEFAULT, -64, 320).hasErrors());
        Limits tiny = new Limits(5, 512, 3, 64);
        var report = StructureValidator.validate(ok, tiny, -64, 320);
        assertTrue(report.hasErrors(), "plus de 5 blocs → erreur");
        var confirm = StructureValidator.validate(ok, new Limits(100, 512, 3, 64), -64, 320);
        assertTrue(confirm.needsConfirmation());
        assertFalse(confirm.hasErrors());
        Limits narrow = new Limits(100, 2, 50, 64);
        assertTrue(StructureValidator.validate(ok, narrow, -64, 320).hasErrors(), "dimension 5 > 2");
    }

    @Test
    void stateSyntaxValidation() {
        assertTrue(StructureValidator.isValidState("minecraft:oak_stairs[facing=north,half=bottom]"));
        assertTrue(StructureValidator.isValidState("modid:custom_block"));
        assertFalse(StructureValidator.isValidState("minecraft:stone]"));
        assertFalse(StructureValidator.isValidState("../../etc/passwd"));
        assertFalse(StructureValidator.isValidState("minecraft:oak_stairs[facing=north"));
        assertFalse(StructureValidator.isValidState(null));
    }

    @Test
    void stateRotationRotatesFacingAxisAndConnections() {
        assertEquals("minecraft:oak_stairs[facing=east,half=bottom]", StateRotation.rotate(B.stairs("oak", B.N, false), 1));
        assertEquals("minecraft:oak_log[axis=z]", StateRotation.rotate("minecraft:oak_log[axis=x]", 1));
        assertEquals("minecraft:oak_log[axis=x]", StateRotation.rotate("minecraft:oak_log[axis=x]", 2));
        String fence = StateRotation.rotate("minecraft:oak_fence[north=true,east=false,south=false,west=false]", 1);
        assertTrue(fence.contains("east=true") && fence.contains("north=false"), fence);
        assertEquals("minecraft:stone", StateRotation.rotate("minecraft:stone", 3));
    }

    @Test
    void pasteWithRotationRotatesStates() {
        VoxelGrid src = new VoxelGrid();
        src.set(0, 0, 1, B.stairs("oak", B.S, false));
        VoxelGrid dst = new VoxelGrid();
        dst.paste(src, 10, 0, 10, 1, false);
        assertEquals(B.stairs("oak", B.W, false), dst.get(9, 0, 10));
    }

    @Test
    void hollowRemovesOnlyEnclosedCells() {
        VoxelGrid g = new VoxelGrid();
        g.fill(0, 0, 0, 4, 4, 4, "minecraft:stone");
        int removed = g.hollowInterior();
        assertEquals(27, removed);
        assertEquals(125 - 27, g.count());
        assertTrue(g.isSet(0, 0, 0));
        assertFalse(g.isSet(2, 2, 2));
    }
}
