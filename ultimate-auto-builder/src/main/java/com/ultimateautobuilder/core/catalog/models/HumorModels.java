package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch.RoofType;
import com.ultimateautobuilder.core.generator.House;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Noise;

import static com.ultimateautobuilder.core.generator.Creature.eyes;
import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.geom.Shapes.box;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.torusY;

/** 10 structures humoristiques et absurdes. */
public final class HumorModels {
    private HumorModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.HUMOR);
        SizeSpec S = SizeSpec.height(8, 18, 36, 72);
        r.sculpt("toilettes_geantes", "Toilettes géantes", 2, S, HumorModels::toilet, "humour", "toilettes", "wc");
        r.gen("maison_envers", "Maison à l'envers", 3, Reg.HOUSE, p -> upsideDown(p.size(), p.seed()), "humour", "maison", "envers", "absurde");
        r.sculpt("burger_geant", "Burger géant", 2, S, HumorModels::burger, "humour", "burger", "nourriture");
        r.sculpt("pizza_geante", "Pizza géante", 2, SizeSpec.length(10, 24, 48, 96), HumorModels::pizza, "humour", "pizza", "nourriture");
        r.sculpt("canard_geant", "Canard en plastique géant", 2, S, HumorModels::duck, "humour", "canard", "bain");
        r.sculpt("donut_geant", "Donut géant", 2, SizeSpec.length(10, 24, 48, 96), HumorModels::donut, "humour", "donut", "beignet");
        r.sculpt("tasse_cafe", "Tasse de café géante", 2, S, HumorModels::mug, "humour", "cafe", "tasse");
        r.sculpt("banane_geante", "Banane géante", 2, SizeSpec.length(10, 24, 48, 96), HumorModels::banana, "humour", "banane", "fruit");
        r.sculpt("chaussette_geante", "Chaussette géante", 2, S, HumorModels::sock, "humour", "chaussette", "linge");
        r.sculpt("gateau_anniversaire", "Gâteau d'anniversaire", 3, S, HumorModels::cake, "humour", "gateau", "anniversaire", "fete");
    }

    static Sculpt toilet() {
        Sculpt.Builder b = Sculpt.builder(18);
        String w = "minecraft:white_concrete";
        b.add(frustum(new Vec3(0, 0, 1), new Vec3(0, 6, 1), 3.0, 4.2), w);
        b.add(ellipsoid(0, 7.0, 2.0, 5.0, 1.4, 6.0), w);
        b.carve(ellipsoid(0, 7.8, 2.2, 3.6, 1.6, 4.4));
        b.add(ellipsoid(0, 6.0, 2.2, 3.4, 0.6, 4.2), "minecraft:light_blue_stained_glass");
        b.add(box(-5, 7, -6, 5, 16, -2.4), w);
        b.add(box(-5.4, 16, -6.4, 5.4, 17, -2.0), w);
        b.add(box(-1.2, 17, -4.6, 1.2, 17.6, -3.6), "minecraft:light_gray_concrete");
        b.add(box(-5.2, 7.4, -3.0, 5.2, 13.6, -2.4), "minecraft:white_concrete");
        return b.build();
    }

    /** Maison générée normalement puis retournée : le toit touche le sol. */
    static VoxelModel upsideDown(int width, long seed) {
        VoxelModel house = House.generate(House.spec().roof(RoofType.GABLE, "spruce").floors(2), width, seed);
        VoxelGrid g = new VoxelGrid();
        int top = house.sizeY() - 1;
        for (int i = 0; i < house.size(); i++) {
            long c = house.cell(i);
            String st = flipVertical(house.stateOf(c));
            g.set(VoxelModel.cx(c) - house.anchorX(), top - VoxelModel.cy(c), VoxelModel.cz(c) - house.anchorZ(), st);
        }
        return g.freeze().groundAtBottom();
    }

    static String flipVertical(String s) {
        return s.replace("half=top", "half=TMP").replace("half=bottom", "half=top").replace("half=TMP", "half=bottom")
                .replace("type=top", "type=TMP").replace("type=bottom", "type=top").replace("type=TMP", "type=bottom")
                .replace("half=upper", "half=TMP").replace("half=lower", "half=upper").replace("half=TMP", "half=lower")
                .replace("hanging=true", "hanging=TMP").replace("hanging=false", "hanging=true").replace("hanging=TMP", "hanging=false")
                .replace("minecraft:campfire[lit=true", "minecraft:campfire[lit=false");
    }

    static Sculpt burger() {
        Sculpt.Builder b = Sculpt.builder(18);
        String bun = "minecraft:orange_terracotta";
        b.add(ellipsoid(0, 2.0, 0, 9, 2.4, 9).and(box(-10, 0, -10, 10, 3.4, 10)), bun);
        b.add(cylinderY(0, 3.4, 0, 2.0, 9.3), "minecraft:brown_concrete");
        b.add(cylinderY(0, 5.4, 0, 0.8, 9.6), "minecraft:yellow_concrete");
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            b.add(limb(Math.cos(a) * 9, 5.8, Math.sin(a) * 9, Math.cos(a) * 10.2, 4.0, Math.sin(a) * 10.2, 0.6, 0.4), "minecraft:yellow_concrete");
        }
        b.add(cylinderY(0, 6.2, 0, 0.8, 9.8), "minecraft:lime_concrete");
        b.add(cylinderY(0, 7.0, 0, 1.0, 9.0), "minecraft:red_concrete");
        b.add(ellipsoid(0, 8.0, 0, 9.4, 8.0, 9.4).and(box(-10, 8.0, -10, 10, 18, 10)), bun);
        b.paint(ellipsoid(0, 8.0, 0, 9.6, 8.2, 9.6), (x, y, z) -> y > 10 && Noise.hash(5, (int) Math.floor(x * 1.2),
                (int) Math.floor(y * 1.2), (int) Math.floor(z * 1.2)) < 0.07 ? "minecraft:white_concrete" : null);
        return b.build();
    }

    static Sculpt pizza() {
        Sculpt.Builder b = Sculpt.builder(4);
        b.add(cylinderY(0, 0, 0, 1.2, 12), "minecraft:orange_terracotta");
        b.add(cylinderY(0, 1.2, 0, 0.5, 11), "minecraft:red_concrete");
        b.add(cylinderY(0, 1.5, 0, 0.6, 10.4), Mat.mix(4, 1.0, 0.15, Mat.of("minecraft:yellow_concrete"), Mat.of("minecraft:yellow_terracotta")));
        for (int i = 0; i < 10; i++) {
            double a = i * 2.39996;
            double r = 2.0 + (i * 37 % 7);
            b.add(cylinderY(Math.cos(a) * r, 2.0, Math.sin(a) * r, 0.3, 1.4), "minecraft:red_terracotta");
        }
        b.carve(com.ultimateautobuilder.core.geom.Shapes.halfSpace(new Vec3(0, 0, 0), new Vec3(-1, 0, 0.6).normalize())
                .and(com.ultimateautobuilder.core.geom.Shapes.halfSpace(new Vec3(0, 0, 0), new Vec3(-1, 0, -0.6).normalize()))
                .and(box(-13, -1, -13, 13, 4, 13)));
        return b.build();
    }

    static Sculpt duck() {
        Sculpt.Builder b = Sculpt.builder(18);
        String y = "minecraft:yellow_concrete";
        b.add(ellipsoid(0, 5.0, -1.0, 6.0, 4.6, 8.0), y);
        b.add(frustum(new Vec3(0, 7.0, -8.0), new Vec3(0, 10.0, -10.4), 2.6, 0.4), y);
        b.add(sphere(0, 12.0, 4.0, 4.6), y);
        b.add(ellipsoid(0, 11.0, 8.6, 2.4, 1.0, 2.6), "minecraft:orange_concrete");
        eyes(b, 2.0, 13.4, 7.6, 0.9, "minecraft:black_concrete");
        b.sym(ellipsoid(5.4, 6.0, -1.0, 1.2, 2.6, 4.0), y);
        return b.build();
    }

    static Sculpt donut() {
        Sculpt.Builder b = Sculpt.builder(8);
        b.add(torusY(0, 3.2, 0, 7.0, 3.2), "minecraft:orange_terracotta");
        b.paint(torusY(0, 3.6, 0, 7.0, 3.4).and(box(-12, 4.4, -12, 12, 9, 12)), "minecraft:pink_concrete");
        b.paint(torusY(0, 3.6, 0, 7.0, 3.5).and(box(-12, 5.6, -12, 12, 9, 12)), (x, y, z) -> {
            double h = Noise.hash(9, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            return h < 0.06 ? "minecraft:light_blue_concrete" : h < 0.12 ? "minecraft:yellow_concrete" : h < 0.16 ? "minecraft:white_concrete" : null;
        });
        return b.build();
    }

    static Sculpt mug() {
        Sculpt.Builder b = Sculpt.builder(18);
        String w = "minecraft:white_concrete";
        b.add(cylinderY(0, 0, 0, 0.8, 10), w);
        b.add(cylinderY(0, 0.8, 0, 14, 6.5), w);
        b.carve(cylinderY(0, 2, 0, 13, 5.5));
        b.add(cylinderY(0, 2, 0, 10.4, 5.5), "minecraft:brown_concrete");
        b.add(torusY(0, 0, 0, 3.4, 1.0).rotated(0, 0, 90, Vec3.ZERO).translated(6.8, 8, 0), w);
        b.paint(cylinderY(0, 8.0, 0, 1.0, 6.7), "minecraft:red_concrete");
        b.add(limb(0, 14.0, 0, 0.6, 17.0, 0.4, 0.4, 0.3), "minecraft:white_stained_glass");
        return b.build();
    }

    static Sculpt banana() {
        Sculpt.Builder b = Sculpt.builder(6);
        Vec3[] pts = new Vec3[9];
        for (int i = 0; i < pts.length; i++) {
            double t = i / (double) (pts.length - 1);
            pts[i] = new Vec3(0, 2.6 + Math.sin(t * Math.PI) * 4.0, (t - 0.5) * 22);
        }
        b.add(tube(1.4, 1.4, pts), "minecraft:yellow_concrete");
        for (int i = 2; i < pts.length - 2; i++) {
            b.add(sphere(pts[i].x(), pts[i].y(), pts[i].z(), 2.3), "minecraft:yellow_concrete");
        }
        b.add(limb(pts[pts.length - 1], pts[pts.length - 1].add(new Vec3(0, 1.0, 1.4)), 0.8, 0.6), "minecraft:brown_concrete");
        b.add(sphere(pts[0].x(), pts[0].y(), pts[0].z() - 0.8, 0.9), "minecraft:brown_concrete");
        return b.build();
    }

    static Sculpt sock() {
        Sculpt.Builder b = Sculpt.builder(18);
        Mat stripes = Mat.stripesY(2.0, 1.0, Mat.of("minecraft:white_wool"), Mat.of("minecraft:red_wool"));
        b.add(limb(0, 6, -1, 0, 17, -1, 3.0, 3.0), stripes);
        b.add(limb(0, 3.2, -1, 0, 3.2, 8.0, 3.0, 2.8), stripes);
        b.add(sphere(0, 3.2, -1, 3.0), "minecraft:red_wool");
        b.add(sphere(0, 3.2, 8.0, 2.8), "minecraft:red_wool");
        b.add(cylinderY(0, 17, -1, 1.4, 3.4), "minecraft:white_wool");
        return b.build();
    }

    static Sculpt cake() {
        Sculpt.Builder b = Sculpt.builder(18);
        b.add(cylinderY(0, 0, 0, 0.6, 11), "minecraft:white_concrete");
        b.add(cylinderY(0, 0.6, 0, 5, 9.5), "minecraft:pink_concrete");
        b.paint(cylinderY(0, 3.0, 0, 0.8, 9.8), "minecraft:white_concrete");
        b.add(cylinderY(0, 5.6, 0, 4.5, 6.5), "minecraft:brown_concrete");
        b.paint(cylinderY(0, 7.6, 0, 0.6, 6.8), "minecraft:white_concrete");
        b.add(cylinderY(0, 10.1, 0, 0.5, 6.7), "minecraft:white_concrete");
        String[] colors = {"red", "yellow", "light_blue", "lime", "magenta"};
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5;
            double x = Math.cos(a) * 4, z = Math.sin(a) * 4;
            b.add(limb(x, 10.6, z, x, 14.0, z, 0.4, 0.4), "minecraft:" + colors[i] + "_concrete");
            b.add(sphere(x, 14.8, z, 0.5), "minecraft:glowstone");
        }
        b.add(boxC(0, 10.8, 0, 1.6, 0.3, 1.6), "minecraft:red_concrete");
        return b.build();
    }
}
