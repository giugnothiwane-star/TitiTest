package com.ultimateautobuilder.core.placement;

import com.ultimateautobuilder.core.model.VoxelModel;

/**
 * Transformation d'un modèle vers le monde : miroir (axe X du modèle), puis rotation horaire par
 * quarts de tour autour de l'axe Y — même convention que les structures vanilla :
 * rotation horaire de 90° : (x, z) → (-z, x).
 */
public final class Placement {
    private final VoxelModel model;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final int rotation;
    private final boolean mirror;

    public Placement(VoxelModel model, int originX, int originY, int originZ, int rotation, boolean mirror) {
        this.model = model;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.rotation = Math.floorMod(rotation, 4);
        this.mirror = mirror;
    }

    public VoxelModel model() {
        return model;
    }

    public int rotation() {
        return rotation;
    }

    public boolean mirror() {
        return mirror;
    }

    public int originX() {
        return originX;
    }

    public int originY() {
        return originY;
    }

    public int originZ() {
        return originZ;
    }

    /** Coordonnées monde d'une position relative au point d'ancrage du modèle. */
    public static long rotateXZ(int rx, int rz, int rotation, boolean mirror) {
        int x = mirror ? -rx : rx;
        int z = rz;
        for (int i = 0; i < Math.floorMod(rotation, 4); i++) {
            int nx = -z;
            z = x;
            x = nx;
        }
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public int worldX(long cell) {
        long p = rotateXZ(VoxelModel.cx(cell) - model.anchorX(), VoxelModel.cz(cell) - model.anchorZ(), rotation, mirror);
        return originX + (int) (p >> 32);
    }

    public int worldY(long cell) {
        return originY + VoxelModel.cy(cell) - model.anchorY();
    }

    public int worldZ(long cell) {
        long p = rotateXZ(VoxelModel.cx(cell) - model.anchorX(), VoxelModel.cz(cell) - model.anchorZ(), rotation, mirror);
        return originZ + (int) p;
    }

    /** Boîte englobante monde {minX, minY, minZ, maxX, maxY, maxZ}. */
    public int[] bounds() {
        if (model.isEmpty()) {
            return new int[]{originX, originY, originZ, originX, originY, originZ};
        }
        int[] xs = {0, model.sizeX() - 1};
        int[] zs = {0, model.sizeZ() - 1};
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int x : xs) {
            for (int z : zs) {
                long p = rotateXZ(x - model.anchorX(), z - model.anchorZ(), rotation, mirror);
                int wx = originX + (int) (p >> 32);
                int wz = originZ + (int) p;
                minX = Math.min(minX, wx);
                maxX = Math.max(maxX, wx);
                minZ = Math.min(minZ, wz);
                maxZ = Math.max(maxZ, wz);
            }
        }
        int minY = originY - model.anchorY();
        int maxY = minY + model.sizeY() - 1;
        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    /** Taille horizontale après rotation : {largeur X, hauteur, profondeur Z}. */
    public int[] rotatedSize() {
        int[] b = bounds();
        return new int[]{b[3] - b[0] + 1, b[4] - b[1] + 1, b[5] - b[2] + 1};
    }

    /** Rotation qui oriente la façade (+Z du modèle) vers un joueur regardant dans la direction donnée. */
    public static int rotationFacingViewer(String playerFacing) {
        // Le joueur regarde vers le nord (-Z) : la façade doit regarder vers le sud (+Z) → aucune rotation.
        return switch (playerFacing) {
            case "east" -> 1;   // façade vers l'ouest
            case "south" -> 2;  // façade vers le nord
            case "west" -> 3;   // façade vers l'est
            default -> 0;
        };
    }
}
