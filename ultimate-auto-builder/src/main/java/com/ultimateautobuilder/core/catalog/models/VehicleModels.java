package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Shape;
import com.ultimateautobuilder.core.geom.Vec3;
import com.ultimateautobuilder.core.model.VoxelModel;

import static com.ultimateautobuilder.core.generator.Creature.horn;
import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.geom.Shapes.box;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** 20 véhicules et machines (avant du véhicule vers +Z). */
public final class VehicleModels {
    private static final String BLACK = "minecraft:black_concrete";
    private static final String GLASS = "minecraft:light_blue_stained_glass";
    private static final String IRON = "minecraft:iron_block";
    private static final String WHITE = "minecraft:white_concrete";

    private VehicleModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.VEHICLES);
        SizeSpec V = Reg.VEHICLE;
        r.sculpt("voiture", "Voiture citadine", 2, V, () -> car("minecraft:blue_concrete", false), "voiture", "auto");
        r.sculpt("voiture_sport", "Voiture de sport", 3, V, () -> car("minecraft:red_concrete", true), "voiture", "sport", "course", "bolide");
        r.sculpt("bus", "Bus scolaire", 2, SizeSpec.length(10, 24, 48, 96), VehicleModels::bus, "bus", "ecole", "transport");
        r.sculpt("camion", "Camion semi-remorque", 3, SizeSpec.length(12, 28, 56, 110), VehicleModels::truck, "camion", "remorque", "poids lourd");
        r.sculpt("locomotive", "Locomotive à vapeur", 4, SizeSpec.length(10, 24, 48, 96), VehicleModels::steamLoco, "train", "locomotive", "vapeur");
        r.sculpt("char_assaut", "Char d'assaut", 3, V, VehicleModels::tank, "char", "tank", "militaire");
        r.sculpt("avion_ligne", "Avion de ligne", 4, SizeSpec.length(16, 36, 70, 130), VehicleModels::airlinerSculpt, "avion", "ligne", "aeroport");
        r.sculpt("avion_chasse", "Avion de chasse", 3, V, VehicleModels::jet, "avion", "chasse", "jet", "militaire");
        r.sculpt("helicoptere", "Hélicoptère", 3, V, VehicleModels::helicopter, "helicoptere", "rotor");
        r.sculpt("voilier", "Voilier", 3, SizeSpec.length(10, 22, 44, 88), VehicleModels::sailboat, "bateau", "voilier", "mer");
        r.sculpt("bateau_pirate", "Bateau pirate", 5, SizeSpec.length(14, 32, 64, 120), VehicleModels::pirateShip, "bateau", "pirate", "navire", "galion");
        r.sculpt("sous_marin", "Sous-marin", 3, SizeSpec.length(10, 26, 52, 100), VehicleModels::submarine, "sous marin", "mer", "ocean");
        r.sculpt("fusee", "Fusée spatiale", 3, SizeSpec.height(12, 30, 60, 120), VehicleModels::rocket, "fusee", "espace", "lancement");
        r.sculpt("montgolfiere", "Montgolfière", 3, SizeSpec.height(10, 24, 48, 96), VehicleModels::balloon, "montgolfiere", "ballon", "ciel");
        r.sculpt("tracteur", "Tracteur", 3, V, VehicleModels::tractor, "tracteur", "ferme", "agricole");
        r.sculpt("pelleteuse", "Pelleteuse", 3, V, VehicleModels::excavator, "pelleteuse", "chantier", "engin");
        r.sculpt("dirigeable", "Dirigeable steampunk", 4, SizeSpec.length(14, 34, 68, 130), VehicleModels::airship, "dirigeable", "zeppelin", "steampunk");
        r.sculpt("mecha_geant", "Mécha de combat", 4, SizeSpec.height(12, 30, 60, 120), VehicleModels::mecha, "robot", "mecha", "gundam", "machine");
        r.sculpt("moto", "Moto", 2, SizeSpec.length(6, 14, 28, 56), VehicleModels::moto, "moto", "motard");
        r.sculpt("camion_pompiers", "Camion de pompiers", 3, SizeSpec.length(10, 24, 48, 96), VehicleModels::fireTruckSculpt, "pompiers", "camion", "echelle");
    }

    /** Voxelise une sculpture de véhicule à une longueur donnée (composition de scènes). */
    static VoxelModel atLength(Sculpt s, int length) {
        return s.voxelize(length, false, false).freeze().groundAtBottom();
    }

    static VoxelModel boat(int length, long seed) {
        return atLength(sailboat(), length);
    }

    static VoxelModel airliner(int length) {
        return atLength(airlinerSculpt(), length);
    }

    static VoxelModel locomotive(int length) {
        return atLength(steamLoco(), length);
    }

    static VoxelModel fireTruck(int length) {
        return atLength(fireTruckSculpt(), length);
    }

    private static Shape wheel(double x, double y, double z, double r, double w) {
        return frustum(new Vec3(x - w / 2, y, z), new Vec3(x + w / 2, y, z), r, r);
    }

    private static void wheels(Sculpt.Builder b, double halfWidth, double r, double... zs) {
        for (double z : zs) {
            b.sym(wheel(halfWidth, r, z, r, 1.0), BLACK);
            b.sym(wheel(halfWidth + 0.3, r, z, r * 0.45, 0.6), "minecraft:light_gray_concrete");
        }
    }

    // ------------------------------------------------------------------ route

    static Sculpt car(String paint, boolean sport) {
        Sculpt.Builder b = Sculpt.builder(6);
        double h = sport ? 2.0 : 2.6;
        b.add(box(-2.6, 0.8, -6, 2.6, 0.8 + h, 6), paint);
        b.add(box(-2.4, 0.8 + h, -3.4, 2.4, 0.8 + h + (sport ? 1.4 : 2.0), sport ? 1.6 : 2.4), GLASS);
        b.add(box(-2.4, 0.8 + h + (sport ? 1.3 : 1.9), -3.0, 2.4, 0.8 + h + (sport ? 1.6 : 2.3), sport ? 1.2 : 2.0), paint);
        wheels(b, 2.4, 1.2, 3.8, -3.8);
        b.paintSym(boxC(1.8, 2.2, 6.0, 0.6, 0.4, 0.3), "minecraft:sea_lantern");
        b.paintSym(boxC(1.8, 2.2, -6.0, 0.6, 0.4, 0.3), "minecraft:redstone_block");
        if (sport) {
            b.add(box(-2.6, 3.6, -6.4, 2.6, 3.9, -5.4), BLACK);
            b.sym(box(2.0, 2.8, -6.0, 2.4, 3.6, -5.6), BLACK);
        }
        return b.build();
    }

    static Sculpt bus() {
        Sculpt.Builder b = Sculpt.builder(8);
        String y = "minecraft:yellow_concrete";
        b.add(box(-2.8, 1.0, -11, 2.8, 6.6, 11), y);
        for (double z = -9.5; z < 8; z += 2.4) {
            b.paintSym(box(2.6, 3.8, z, 2.9, 5.6, z + 1.6), GLASS);
        }
        b.paint(box(-2.4, 3.6, 10.8, 2.4, 5.8, 11.2), GLASS);
        b.paint(box(-2.9, 2.8, -11.2, 2.9, 3.2, 11.2), BLACK);
        b.add(box(-2.6, 1.0, 11, 2.6, 2.4, 13), y);
        wheels(b, 2.6, 1.3, 8.4, -7.0);
        b.paintSym(boxC(2.0, 1.8, 13.0, 0.5, 0.3, 0.2), "minecraft:sea_lantern");
        b.add(box(1.4, 6.6, 6.0, 2.2, 7.0, 7.0), "minecraft:redstone_block");
        return b.build();
    }

    static Sculpt truck() {
        Sculpt.Builder b = Sculpt.builder(10);
        String cab = "minecraft:red_concrete";
        b.add(box(-2.8, 1.2, 8, 2.8, 7.6, 14), cab);
        b.paint(box(-2.6, 5.0, 13.8, 2.6, 7.0, 14.2), GLASS);
        b.paintSym(box(2.7, 5.0, 10, 2.9, 7.0, 12.6), GLASS);
        b.add(box(-2.6, 1.0, 14, 2.6, 2.6, 15.2), IRON);
        b.add(box(-2.8, 2.0, -18, 2.8, 9.4, 7.4), Mat.stripesY(9, 0.6, Mat.of(WHITE), Mat.of("minecraft:blue_concrete")));
        b.add(box(-2.6, 1.0, -18, 2.6, 2.0, 14), "minecraft:gray_concrete");
        b.sym(cylinderY(2.0, 7.6, 8.6, 3.0, 0.4), IRON);
        wheels(b, 2.6, 1.4, 12.0, 4.0, -12.0, -15.6);
        return b.build();
    }

    static Sculpt steamLoco() {
        Sculpt.Builder b = Sculpt.builder(10);
        b.add(frustum(new Vec3(0, 5.0, -2), new Vec3(0, 5.0, 11), 2.8, 2.8), BLACK);
        b.add(frustum(new Vec3(0, 5.0, 11), new Vec3(0, 5.0, 11.6), 2.4, 2.4), "minecraft:gray_concrete");
        b.add(frustum(new Vec3(0, 7.0, 9), new Vec3(0, 11.0, 9), 1.0, 1.4), BLACK);
        b.add(cylinderY(0, 7.4, 3.0, 1.2, 1.0), "minecraft:gold_block");
        b.add(box(-3.0, 2.0, -9, 3.0, 9.6, -2), "minecraft:red_concrete");
        b.paintSym(box(2.9, 6.0, -7.4, 3.1, 8.0, -3.4), GLASS);
        b.add(box(-3.4, 9.6, -9.6, 3.4, 10.2, -1.4), BLACK);
        b.add(box(-3.0, 1.6, -9, 3.0, 2.2, 12), "minecraft:gray_concrete");
        for (double z : new double[]{8.6, 4.0, -0.6}) {
            b.sym(wheel(2.6, 1.9, z, 1.9, 0.9), "minecraft:red_concrete");
        }
        b.sym(wheel(2.6, 1.1, -6.0, 1.1, 0.9), "minecraft:red_concrete");
        b.add(tri(new Vec3(-2.6, 0.4, 12), new Vec3(2.6, 0.4, 12), new Vec3(0, 2.2, 14.6), 0.5), "minecraft:red_concrete");
        b.paint(sphere(0, 5.0, 11.8, 0.8), "minecraft:sea_lantern");
        return b.build();
    }

    static Sculpt tank() {
        Sculpt.Builder b = Sculpt.builder(6);
        String green = "minecraft:green_terracotta";
        b.sym(box(2.4, 0.2, -6.6, 4.2, 2.6, 6.6), "minecraft:gray_concrete");
        for (double z = -5.6; z <= 5.6; z += 2.2) {
            b.sym(wheel(3.3, 1.4, z, 1.0, 2.0), BLACK);
        }
        b.add(box(-3.6, 1.8, -6.2, 3.6, 3.8, 6.2), green);
        b.add(ellipsoid(0, 4.4, -0.6, 2.8, 1.4, 3.2), green);
        b.add(frustum(new Vec3(0, 4.6, 2.0), new Vec3(0, 4.9, 11.0), 0.55, 0.5), green);
        b.add(cylinderY(-1.2, 5.6, -1.6, 0.8, 0.7), green);
        return b.build();
    }

    // ------------------------------------------------------------------ air

    static Sculpt airlinerSculpt() {
        Sculpt.Builder b = Sculpt.builder(10);
        b.add(frustum(new Vec3(0, 4.0, -14), new Vec3(0, 4.0, 14), 2.4, 2.4), WHITE);
        b.add(ellipsoid(0, 3.8, 14, 2.4, 2.2, 3.2), WHITE);
        b.add(frustum(new Vec3(0, 4.6, -14), new Vec3(0, 6.0, -20), 2.2, 0.6), WHITE);
        b.paint(boxC(0, 4.4, 15.8, 1.6, 0.6, 1.2), GLASS);
        for (double z = -12; z < 12; z += 1.6) {
            b.paintSym(boxC(2.3, 4.6, z, 0.4, 0.35, 0.4), GLASS);
        }
        b.paint(box(-2.6, 3.0, -14, 2.6, 3.4, 14), "minecraft:blue_concrete");
        b.sym(tri(new Vec3(1.4, 3.0, 4.0), new Vec3(16.0, 3.6, -4.0), new Vec3(1.4, 3.0, -4.0), 0.4), "minecraft:light_gray_concrete");
        b.sym(tri(new Vec3(1.0, 5.6, -16.0), new Vec3(6.6, 5.8, -20.0), new Vec3(1.0, 5.6, -19.6), 0.35), "minecraft:light_gray_concrete");
        b.add(tri(new Vec3(0, 6.0, -15.0), new Vec3(0, 12.0, -20.4), new Vec3(0, 6.0, -20.0), 0.4), "minecraft:blue_concrete");
        b.sym(frustum(new Vec3(6.0, 2.0, 2.4), new Vec3(6.0, 2.0, -2.0), 1.1, 1.0), "minecraft:light_gray_concrete");
        b.add(limb(0, 1.8, 11, 0, 0.6, 11, 0.3, 0.3), "minecraft:gray_concrete");
        b.sym(limb(2.0, 1.8, -1, 2.0, 0.6, -1, 0.3, 0.3), "minecraft:gray_concrete");
        b.add(sphere(0, 0.5, 11, 0.6), BLACK);
        b.sym(sphere(2.0, 0.5, -1, 0.6), BLACK);
        return b.build();
    }

    static Sculpt jet() {
        Sculpt.Builder b = Sculpt.builder(6);
        String gray = "minecraft:light_gray_concrete";
        b.add(frustum(new Vec3(0, 2.6, -7), new Vec3(0, 2.6, 6), 1.3, 1.2), gray);
        b.add(frustum(new Vec3(0, 2.6, 6), new Vec3(0, 2.6, 10), 1.2, 0.1), gray);
        b.add(ellipsoid(0, 3.8, 3.6, 0.8, 0.7, 2.0), GLASS);
        b.sym(tri(new Vec3(1.0, 2.4, 3.0), new Vec3(8.0, 2.4, -4.0), new Vec3(1.0, 2.4, -5.0), 0.3), gray);
        b.sym(tri(new Vec3(0.8, 2.8, -4.6), new Vec3(1.4, 6.6, -7.2), new Vec3(0.8, 2.8, -7.2), 0.3), gray);
        b.add(frustum(new Vec3(0, 2.6, -7), new Vec3(0, 2.6, -7.8), 1.0, 1.0), "minecraft:orange_concrete");
        b.add(limb(0, 1.4, 5, 0, 0.4, 5, 0.25, 0.25), BLACK);
        b.sym(limb(1.4, 1.4, -2, 1.4, 0.4, -2, 0.25, 0.25), BLACK);
        return b.build();
    }

    static Sculpt helicopter() {
        Sculpt.Builder b = Sculpt.builder(8);
        String red = "minecraft:red_concrete";
        b.add(ellipsoid(0, 3.8, 1.0, 2.4, 2.4, 4.0), red);
        b.paint(ellipsoid(0, 4.2, 3.6, 2.0, 1.8, 1.8), GLASS);
        b.add(limb(0, 4.4, -2.6, 0, 5.2, -11.0, 1.0, 0.45), red);
        b.add(tri(new Vec3(0, 5.0, -10.4), new Vec3(0, 8.0, -11.6), new Vec3(0, 5.0, -11.4), 0.3), red);
        b.add(limb(-0.6, 5.6, -11.0, 0.6, 5.6, -11.0, 0.2, 0.2), BLACK);
        b.add(boxC(0.7, 5.6, -11.0, 0.15, 2.0, 0.3), BLACK);
        b.add(limb(0, 6.0, 1.0, 0, 7.0, 1.0, 0.4, 0.4), BLACK);
        b.add(boxC(0, 7.0, 1.0, 9.0, 0.15, 0.4), "minecraft:gray_concrete");
        b.add(boxC(0, 7.0, 1.0, 0.4, 0.15, 9.0), "minecraft:gray_concrete");
        b.sym(limb(1.8, 0.4, -2.0, 1.8, 0.4, 4.0, 0.25, 0.25), BLACK);
        b.sym(limb(1.6, 1.6, 0, 1.8, 0.4, 0, 0.2, 0.2), BLACK);
        b.sym(limb(1.6, 1.6, 2.4, 1.8, 0.4, 2.4, 0.2, 0.2), BLACK);
        return b.build();
    }

    static Sculpt balloon() {
        Sculpt.Builder b = Sculpt.builder(24);
        Mat stripes = (x, y, z) -> {
            double a = Math.atan2(z, x);
            int k = (int) Math.floor((a + Math.PI) / (Math.PI / 6));
            return (k & 1) == 0 ? "minecraft:red_wool" : "minecraft:yellow_wool";
        };
        b.add(ellipsoid(0, 15.0, 0, 7.0, 8.0, 7.0), stripes);
        b.add(frustum(new Vec3(0, 9.0, 0), new Vec3(0, 6.6, 0), 4.6, 1.6), stripes);
        b.add(box(-1.6, 0, -1.6, 1.6, 2.2, 1.6), "minecraft:stripped_oak_log[axis=y]");
        b.carve(box(-1.0, 1.0, -1.0, 1.0, 2.4, 1.0));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                b.add(limb(sx * 1.5, 2.2, sz * 1.5, sx * 1.6, 6.6, sz * 1.6, 0.12, 0.12), "minecraft:chain[axis=y]");
            }
        }
        b.add(sphere(0, 5.0, 0, 0.5), "minecraft:orange_concrete");
        return b.build();
    }

    // ------------------------------------------------------------------ mer

    static Sculpt sailboat() {
        Sculpt.Builder b = Sculpt.builder(12);
        Shape hull = ellipsoid(0, 2.6, 0, 3.2, 2.6, 9.0).and(box(-4, 0, -10, 4, 2.8, 10));
        b.add(hull, WHITE);
        b.carve(ellipsoid(0, 3.2, 0, 2.6, 2.2, 8.2).and(box(-4, 2.0, -10, 4, 4, 10)));
        b.add(box(-2.6, 2.0, -6.4, 2.6, 2.2, 5.4), "minecraft:spruce_planks");
        b.paint(box(-4, 1.6, -10, 4, 2.0, 10), "minecraft:blue_concrete");
        b.add(limb(0, 2.0, 1.0, 0, 18.0, 1.0, 0.25, 0.2), "minecraft:spruce_log[axis=y]");
        b.add(tri(new Vec3(0, 3.6, 0.6), new Vec3(0, 17.0, 0.6), new Vec3(0, 3.6, -6.4), 0.2), "minecraft:white_wool");
        b.add(tri(new Vec3(0, 3.6, 1.6), new Vec3(0, 16.0, 1.4), new Vec3(0, 3.0, 8.0), 0.2), "minecraft:white_wool");
        b.add(box(-1.4, 2.2, -5.4, 1.4, 3.6, -2.6), "minecraft:spruce_planks");
        return b.build();
    }

    static Sculpt pirateShip() {
        Sculpt.Builder b = Sculpt.builder(20);
        String wood = "minecraft:dark_oak_planks";
        Shape hull = ellipsoid(0, 5.0, 0, 5.0, 5.0, 14.0).and(box(-6, 0, -15, 6, 7.4, 15));
        b.add(hull, wood);
        b.carve(ellipsoid(0, 5.8, 0, 4.2, 4.4, 13.0).and(box(-6, 4.4, -15, 6, 9, 15)));
        b.add(box(-4.2, 4.4, -11, 4.2, 4.6, 11), "minecraft:spruce_planks");
        b.paint(box(-6, 5.6, -15, 6, 6.0, 15), "minecraft:gold_block");
        for (double z = -8; z <= 8; z += 4) {
            b.paintSym(box(4.6, 4.0, z, 5.4, 4.8, z + 0.8), BLACK);
        }
        b.add(box(-4.4, 4.4, -14, 4.4, 9.4, -9), wood);
        b.paint(box(-4.6, 7.0, -14.4, 4.6, 8.2, -14.0), "minecraft:yellow_stained_glass");
        b.add(box(-3.4, 4.4, 10, 3.4, 7.0, 13.4), wood);
        b.add(limb(0, 6.0, 13.0, 0, 9.0, 19.0, 0.3, 0.25), "minecraft:spruce_log[axis=y]");
        double[] masts = {6.0, -1.0, -8.0};
        double[] heights = {22, 26, 21};
        for (int i = 0; i < 3; i++) {
            double z = masts[i], h = heights[i];
            b.add(limb(0, 4.4, z, 0, h, z, 0.35, 0.3), "minecraft:spruce_log[axis=y]");
            for (int k = 0; k < 2; k++) {
                double y0 = 8 + k * (h - 10) / 2.0, y1 = y0 + (h - 12) / 2.0;
                b.add(box(-5.0 + k, y0, z - 0.2, 5.0 - k, y1, z + 0.2), "minecraft:black_wool");
                b.add(limb(-5.4 + k, y1 + 0.2, z, 5.4 - k, y1 + 0.2, z, 0.2, 0.2), "minecraft:spruce_log[axis=x]");
            }
        }
        b.paint(sphere(0, 18.0, -1.0, 1.2), WHITE);
        b.add(box(0, 26.0, -1.0, 0.2, 28.0, 2.4), "minecraft:black_wool");
        return b.build();
    }

    static Sculpt submarine() {
        Sculpt.Builder b = Sculpt.builder(8);
        String y = "minecraft:yellow_concrete";
        b.add(ellipsoid(0, 3.2, 0, 3.0, 3.0, 12.0), y);
        b.add(box(-1.2, 5.6, -1.6, 1.2, 8.6, 3.0), y);
        b.add(limb(0, 8.6, 1.6, 0, 11.0, 1.6, 0.25, 0.25), "minecraft:gray_concrete");
        b.add(limb(0, 11.0, 1.6, 0, 11.0, 2.8, 0.25, 0.25), "minecraft:gray_concrete");
        for (double z = -7; z <= 7; z += 3.5) {
            b.paintSym(sphere(2.8, 3.6, z, 0.7), GLASS);
        }
        b.add(tri(new Vec3(0, 3.2, -11), new Vec3(0, 7.2, -13.6), new Vec3(0, -0.6, -13.6), 0.3), y);
        b.add(tri(new Vec3(0, 3.2, -11), new Vec3(3.6, 3.2, -13.6), new Vec3(-3.6, 3.2, -13.6), 0.3), y);
        b.add(boxC(0, 3.2, -14.0, 0.2, 1.6, 0.2), "minecraft:gray_concrete");
        b.add(boxC(0, 3.2, -14.0, 1.6, 0.2, 0.2), "minecraft:gray_concrete");
        return b.build();
    }

    // ------------------------------------------------------------------ espace, ferme, chantier

    static Sculpt rocket() {
        Sculpt.Builder b = Sculpt.builder(30);
        b.add(frustum(new Vec3(0, 3, 0), new Vec3(0, 22, 0), 2.6, 2.6), WHITE);
        b.add(frustum(new Vec3(0, 22, 0), new Vec3(0, 30, 0), 2.6, 0.1), "minecraft:red_concrete");
        b.paint(frustum(new Vec3(0, 12, 0), new Vec3(0, 13, 0), 2.8, 2.8), BLACK);
        b.paint(sphere(0, 17, 2.4, 0.9), GLASS);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90 + 45);
            b.add(tri(new Vec3(Math.cos(a) * 2.2, 9, Math.sin(a) * 2.2), new Vec3(Math.cos(a) * 5.6, 1.0, Math.sin(a) * 5.6),
                    new Vec3(Math.cos(a) * 2.2, 2.0, Math.sin(a) * 2.2), 0.35), "minecraft:red_concrete");
        }
        b.add(frustum(new Vec3(0, 3, 0), new Vec3(0, 1.2, 0), 1.6, 2.2), "minecraft:gray_concrete");
        b.add(frustum(new Vec3(0, 1.2, 0), new Vec3(0, 0, 0), 1.4, 0.6), "minecraft:orange_concrete");
        return b.build();
    }

    static Sculpt tractor() {
        Sculpt.Builder b = Sculpt.builder(8);
        String green = "minecraft:green_concrete";
        b.add(box(-1.8, 2.0, -1.0, 1.8, 4.4, 7.0), green);
        b.add(box(-2.2, 2.0, -4.2, 2.2, 7.6, -0.6), green);
        b.paint(box(-2.0, 4.6, -4.4, 2.0, 7.0, -0.4).minus(box(-1.6, 2, -4, 1.6, 8, -0.8)), GLASS);
        b.carve(box(-1.6, 4.6, -3.8, 1.6, 7.0, -1.0));
        b.add(box(-2.4, 7.6, -4.6, 2.4, 8.0, -0.2), "minecraft:yellow_concrete");
        b.sym(wheel(2.6, 2.6, -2.4, 2.6, 1.6), BLACK);
        b.sym(wheel(2.2, 1.4, 5.4, 1.4, 1.0), BLACK);
        b.sym(wheel(2.9, 2.6, -2.4, 1.2, 0.6), "minecraft:yellow_concrete");
        b.add(limb(1.0, 4.4, 4.0, 1.0, 7.0, 4.0, 0.3, 0.3), BLACK);
        return b.build();
    }

    static Sculpt excavator() {
        Sculpt.Builder b = Sculpt.builder(10);
        String y = "minecraft:yellow_concrete";
        b.sym(box(1.6, 0, -5, 3.4, 2.0, 5), "minecraft:gray_concrete");
        b.add(box(-3.0, 2.0, -4.6, 3.0, 4.6, 3.0), y);
        b.add(box(-3.0, 4.6, 0.0, 0.2, 8.4, 3.0), y);
        b.paint(box(-3.2, 5.2, 0.2, 0.0, 8.0, 3.2), GLASS);
        b.add(box(-2.6, 2.0, -6.4, 2.6, 5.2, -4.4), "minecraft:gray_concrete");
        b.add(limb(1.4, 5.0, 2.0, 1.4, 11.0, 7.6, 0.7, 0.6), y);
        b.add(limb(1.4, 11.0, 7.6, 1.4, 4.0, 12.6, 0.6, 0.5), y);
        b.add(box(0.2, 1.6, 12.0, 2.6, 4.0, 14.4), "minecraft:gray_concrete");
        b.carve(box(0.6, 2.4, 12.4, 2.2, 4.4, 14.0));
        return b.build();
    }

    static Sculpt airship() {
        Sculpt.Builder b = Sculpt.builder(16);
        String env = "minecraft:brown_wool";
        b.add(ellipsoid(0, 11.0, 0, 5.0, 5.0, 14.0), Mat.stripesX(3.0, 0.4, Mat.of(env), Mat.of("minecraft:brown_terracotta")));
        b.paint(boxC(0, 11, 0, 6, 6, 0.4), "minecraft:cut_copper");
        b.paint(boxC(0, 11, 6, 6, 6, 0.4), "minecraft:cut_copper");
        b.paint(boxC(0, 11, -6, 6, 6, 0.4), "minecraft:cut_copper");
        b.add(tri(new Vec3(0, 11, -12), new Vec3(0, 18, -16), new Vec3(0, 11, -16), 0.3), "minecraft:cut_copper");
        b.add(tri(new Vec3(0, 11, -12), new Vec3(5.6, 11, -16), new Vec3(-5.6, 11, -16), 0.3), "minecraft:cut_copper");
        b.add(box(-2.0, 1.0, -6, 2.0, 4.0, 6), "minecraft:spruce_planks");
        b.paintSym(box(1.9, 2.4, -5, 2.1, 3.4, 5), "minecraft:yellow_stained_glass");
        for (double z : new double[]{-5.6, 0, 5.6}) {
            b.sym(limb(1.6, 4.0, z, 2.6, 6.8, z, 0.15, 0.15), "minecraft:chain[axis=y]");
        }
        b.sym(limb(2.0, 3.0, -5.0, 5.0, 3.0, -5.0, 0.3, 0.3), "minecraft:cut_copper");
        b.sym(boxC(5.4, 3.0, -5.0, 0.2, 1.8, 0.4), "minecraft:spruce_planks");
        b.add(frustum(new Vec3(0, 2.0, 6), new Vec3(0, 2.0, 7.4), 0.8, 0.3), "minecraft:gold_block");
        return b.build();
    }

    static Sculpt mecha() {
        Sculpt.Builder b = Sculpt.builder(30);
        String w = WHITE;
        String blue = "minecraft:blue_concrete";
        String red = "minecraft:red_concrete";
        b.sym(box(1.0, 0, -2.4, 4.6, 2.0, 3.4), red);
        b.sym(box(1.4, 2.0, -1.6, 4.2, 9.0, 1.6), w);
        b.sym(box(1.2, 9.0, -1.8, 4.4, 13.0, 1.8), blue);
        b.add(box(-4.0, 12.6, -2.2, 4.0, 15.0, 2.2), "minecraft:gray_concrete");
        b.add(box(-5.2, 15.0, -3.0, 5.2, 21.6, 3.0), blue);
        b.paint(box(-3.0, 17.0, 2.8, 3.0, 20.6, 3.2), "minecraft:yellow_concrete");
        b.paint(box(-1.0, 15.6, 2.8, 1.0, 17.0, 3.2), red);
        b.sym(box(5.2, 18.0, -2.6, 8.6, 22.4, 2.6), w);
        b.sym(box(5.8, 10.0, -1.6, 8.0, 18.0, 1.6), w);
        b.sym(box(5.4, 7.6, -1.8, 8.4, 10.0, 1.8), "minecraft:gray_concrete");
        b.add(box(-1.8, 21.6, -1.8, 1.8, 25.0, 1.8), w);
        b.paint(box(-1.2, 23.0, 1.7, 1.2, 23.8, 1.9), "minecraft:lime_concrete");
        b.add(tri(new Vec3(0, 24.4, 1.8), new Vec3(2.8, 27.0, 1.6), new Vec3(0.4, 25.0, 1.8), 0.3), "minecraft:yellow_concrete");
        b.add(tri(new Vec3(0, 24.4, 1.8), new Vec3(-2.8, 27.0, 1.6), new Vec3(-0.4, 25.0, 1.8), 0.3), "minecraft:yellow_concrete");
        b.sym(box(1.6, 15.0, -5.6, 3.6, 21.0, -3.0), "minecraft:gray_concrete");
        b.add(box(-8.6, 9.0, -0.6, -7.8, 22.0, 0.6), "minecraft:light_blue_stained_glass");
        return b.build();
    }

    static Sculpt moto() {
        Sculpt.Builder b = Sculpt.builder(6);
        b.add(wheel(0, 1.6, 3.6, 1.6, 0.8), BLACK);
        b.add(wheel(0, 1.6, -3.6, 1.6, 0.8), BLACK);
        b.add(box(-0.8, 2.4, -2.6, 0.8, 4.0, 2.0), "minecraft:red_concrete");
        b.add(box(-0.7, 4.0, -3.4, 0.7, 4.6, -0.4), BLACK);
        b.add(limb(0, 2.0, 3.6, 0, 5.4, 2.2, 0.3, 0.3), "minecraft:gray_concrete");
        b.add(limb(-1.4, 5.4, 2.2, 1.4, 5.4, 2.2, 0.2, 0.2), BLACK);
        b.add(limb(0, 2.0, -3.6, 0, 3.4, -1.6, 0.3, 0.3), "minecraft:gray_concrete");
        b.add(sphere(0, 4.6, 3.0, 0.4), "minecraft:sea_lantern");
        b.add(limb(0.8, 1.8, -1.0, 0.8, 2.4, -4.4, 0.3, 0.3), IRON);
        return b.build();
    }

    static Sculpt fireTruckSculpt() {
        Sculpt.Builder b = Sculpt.builder(8);
        String red = "minecraft:red_concrete";
        b.add(box(-2.6, 1.2, -10, 2.6, 6.0, 7), red);
        b.add(box(-2.6, 1.2, 7, 2.6, 6.6, 11), red);
        b.paint(box(-2.4, 4.4, 10.8, 2.4, 6.2, 11.2), GLASS);
        b.paintSym(box(2.5, 4.4, 8, 2.7, 6.2, 10), GLASS);
        b.paint(box(-2.8, 2.6, -10.2, 2.8, 3.0, 11.2), WHITE);
        b.add(box(-1.0, 6.0, -10, 1.0, 6.6, 6), IRON);
        b.add(box(-0.8, 6.6, -12, 0.8, 7.0, 5), "minecraft:light_gray_concrete");
        b.add(box(1.4, 6.6, 8, 2.4, 7.0, 9), "minecraft:light_blue_concrete");
        b.add(box(-2.4, 6.6, 8, -1.4, 7.0, 9), "minecraft:redstone_block");
        wheels(b, 2.4, 1.3, 8.4, -2.0, -7.0);
        b.paintSym(boxC(1.8, 2.2, 11.0, 0.5, 0.3, 0.3), "minecraft:sea_lantern");
        b.add(horn(0, 3.0, -10, 0, 3.0, -11, 0.6), IRON);
        b.add(tube(0.3, 0.3, new Vec3(-2.7, 3.6, -6), new Vec3(-2.7, 3.6, 4)), "minecraft:white_concrete");
        return b.build();
    }
}
