package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.Reg;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.Arch;
import com.ultimateautobuilder.core.generator.HairStyles;
import com.ultimateautobuilder.core.generator.Humanoid;
import com.ultimateautobuilder.core.geom.Mat;
import com.ultimateautobuilder.core.geom.Sculpt;
import com.ultimateautobuilder.core.geom.Vec3;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelGrid;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.util.Noise;
import com.ultimateautobuilder.core.util.Rng;

import static com.ultimateautobuilder.core.generator.Creature.tube;
import static com.ultimateautobuilder.core.geom.Shapes.box;
import static com.ultimateautobuilder.core.geom.Shapes.boxC;
import static com.ultimateautobuilder.core.geom.Shapes.cylinderY;
import static com.ultimateautobuilder.core.geom.Shapes.ellipsoid;
import static com.ultimateautobuilder.core.geom.Shapes.frustum;
import static com.ultimateautobuilder.core.geom.Shapes.limb;
import static com.ultimateautobuilder.core.geom.Shapes.sphere;
import static com.ultimateautobuilder.core.geom.Shapes.torusY;
import static com.ultimateautobuilder.core.geom.Shapes.tri;

/** 20 constructions de science-fiction et d'astronomie. */
public final class SciFiModels {
    private SciFiModels() {
    }

    public static void register(Catalog c) {
        Reg r = new Reg(c, Category.SCIFI);
        SizeSpec P = SizeSpec.height(10, 24, 48, 96);
        r.sculpt("planete_terre", "Planète Terre", 3, P, SciFiModels::earth, "planete", "terre", "monde", "globe");
        r.sculpt("planete_mars", "Planète Mars", 2, P, () -> rocky(Mat.mix(5, 2.5, 0.3, Mat.of("minecraft:red_terracotta"),
                Mat.of("minecraft:orange_terracotta")), "minecraft:brown_terracotta", 31), "planete", "mars", "rouge");
        r.sculpt("saturne", "Saturne et ses anneaux", 3, P, SciFiModels::saturn, "planete", "saturne", "anneaux");
        r.sculpt("jupiter", "Jupiter", 3, P, SciFiModels::jupiter, "planete", "jupiter", "geante gazeuse");
        r.sculpt("lune", "La Lune", 2, P, () -> rocky(Mat.of("minecraft:light_gray_concrete"), "minecraft:gray_concrete", 32), "lune", "satellite", "crateres");
        r.sculpt("soleil", "Soleil", 3, P, SciFiModels::sun, "soleil", "etoile", "lumiere");
        r.sculpt("systeme_solaire", "Maquette du système solaire", 5, SizeSpec.length(24, 60, 120, 220), SciFiModels::solarSystem,
                "systeme solaire", "planetes", "orbites");
        r.sculpt("station_spatiale", "Station spatiale", 4, SizeSpec.length(14, 34, 68, 130), SciFiModels::spaceStation, "station", "espace", "iss");
        r.sculpt("ovni", "Soucoupe volante", 3, SizeSpec.length(10, 24, 48, 96), SciFiModels::ufo, "ovni", "soucoupe", "alien");
        r.sculpt("extraterrestre", "Extraterrestre", 3, Reg.CHARACTER, SciFiModels::alien, "alien", "extraterrestre", "martien");
        r.gen("base_lunaire", "Base lunaire", 4, Reg.ISLAND, p -> moonBase(p.size(), p.seed()), "base", "lune", "colonie", "dome");
        r.sculpt("satellite", "Satellite de communication", 2, SizeSpec.length(10, 22, 44, 88), SciFiModels::satellite, "satellite", "antenne", "orbite");
        r.sculpt("croiseur_spatial", "Croiseur spatial", 4, SizeSpec.length(20, 50, 100, 180), SciFiModels::cruiser, "vaisseau", "croiseur", "flotte");
        r.sculpt("chasseur_spatial", "Chasseur spatial", 3, SizeSpec.length(10, 22, 44, 88), SciFiModels::fighter, "vaisseau", "chasseur", "x wing");
        r.sculpt("station_sphere", "Station de combat sphérique", 5, P, SciFiModels::battleStation, "station", "sphere", "empire");
        r.sculpt("portail_stellaire", "Portail stellaire", 3, P, SciFiModels::stargate, "portail", "anneau", "teleportation");
        r.sculpt("trou_noir", "Trou noir", 3, SizeSpec.length(14, 34, 68, 130), SciFiModels::blackHole, "trou noir", "galaxie", "espace");
        r.sculpt("asteroide", "Astéroïde minier", 2, P, SciFiModels::asteroid, "asteroide", "roche", "minerais");
        r.sculpt("androide", "Androïde", 3, Reg.CHARACTER, SciFiModels::android, "robot", "androide", "cyborg");
        r.gen("pas_de_tir", "Pas de tir et fusée", 4, SizeSpec.height(20, 40, 80, 150), p -> launchPad(p.size()), "fusee", "lancement", "nasa");
    }

    static Mat sphereNoise(long seed, double scale, double threshold, Mat a, Mat b) {
        return (x, y, z) -> {
            double l = Math.sqrt(x * x + (y - 12) * (y - 12) + z * z) + 1e-9;
            double n = Noise.value3(seed, x / l * scale + 10, (y - 12) / l * scale + 10, z / l * scale + 10);
            return n > threshold ? b.at(x, y, z) : a.at(x, y, z);
        };
    }

    static Sculpt earth() {
        Sculpt.Builder b = Sculpt.builder(24);
        Mat land = Mat.splitY(2.5, Mat.of("minecraft:snow_block"), Mat.splitY(21.5, sphereNoise(41, 2.0, 0.6,
                Mat.of("minecraft:green_concrete"), Mat.of("minecraft:yellow_terracotta")), Mat.of("minecraft:snow_block")));
        b.add(sphere(0, 12, 0, 12), sphereNoise(40, 2.6, 0.52, Mat.of("minecraft:blue_concrete"), land));
        b.paint(sphere(0, 12, 0, 12.6), (x, y, z) -> Noise.value3(42, x * 0.35, y * 0.35, z * 0.35) > 0.72 ? "minecraft:white_concrete" : null);
        return b.build();
    }

    static Sculpt rocky(Mat surface, String crater, long seed) {
        Sculpt.Builder b = Sculpt.builder(24);
        b.add(sphere(0, 12, 0, 12), surface);
        Rng rng = new Rng(seed);
        for (int i = 0; i < 9; i++) {
            Vec3 d = new Vec3(rng.range(-1, 1), rng.range(-1, 1), rng.range(-1, 1)).normalize();
            double cr = rng.range(1.5, 3.2);
            Vec3 p = new Vec3(0, 12, 0).add(d.mul(12.4));
            b.carve(sphere(p.x(), p.y(), p.z(), cr));
            b.paint(sphere(p.x(), p.y(), p.z(), cr + 1.0), crater);
        }
        return b.build();
    }

    static Sculpt saturn() {
        Sculpt.Builder b = Sculpt.builder(24);
        b.add(sphere(0, 12, 0, 8), Mat.stripesY(2.0, 0.7, Mat.of("minecraft:yellow_terracotta"), Mat.of("minecraft:white_terracotta")));
        b.add(torusY(0, 12, 0, 13, 0.6).rotated(0, 0, 18, new Vec3(0, 12, 0)), "minecraft:orange_terracotta");
        b.add(torusY(0, 12, 0, 15.2, 0.5).rotated(0, 0, 18, new Vec3(0, 12, 0)), "minecraft:white_terracotta");
        b.add(torusY(0, 12, 0, 11, 0.5).rotated(0, 0, 18, new Vec3(0, 12, 0)), "minecraft:brown_terracotta");
        return b.build();
    }

    static Sculpt jupiter() {
        Sculpt.Builder b = Sculpt.builder(24);
        Mat bands = (x, y, z) -> {
            int band = (int) Math.floor((y + Noise.value2(7, x * 0.3, z * 0.3) * 1.2) / 2.2);
            return switch (Math.floorMod(band, 4)) {
                case 0 -> "minecraft:white_terracotta";
                case 1 -> "minecraft:orange_terracotta";
                case 2 -> "minecraft:brown_terracotta";
                default -> "minecraft:yellow_terracotta";
            };
        };
        b.add(sphere(0, 12, 0, 12), bands);
        b.paint(ellipsoid(4, 9, 10, 3.0, 1.6, 2.6), "minecraft:red_terracotta");
        return b.build();
    }

    static Sculpt sun() {
        Sculpt.Builder b = Sculpt.builder(24);
        b.add(sphere(0, 12, 0, 10), Mat.mix(9, 1.5, 0.35, Mat.of("minecraft:shroomlight"), Mat.of("minecraft:orange_concrete")));
        Rng rng = new Rng(33);
        for (int i = 0; i < 14; i++) {
            Vec3 d = new Vec3(rng.range(-1, 1), rng.range(-1, 1), rng.range(-1, 1)).normalize();
            Vec3 p = new Vec3(0, 12, 0).add(d.mul(9.5));
            b.add(frustum(p, p.add(d.mul(rng.range(2.0, 3.5))), 1.4, 0.2), "minecraft:glowstone");
        }
        return b.build();
    }

    static Sculpt solarSystem() {
        Sculpt.Builder b = Sculpt.builder(8);
        b.add(sphere(0, 6, 0, 5.5), "minecraft:shroomlight");
        String[] mats = {"minecraft:gray_concrete", "minecraft:orange_terracotta", "minecraft:blue_concrete", "minecraft:red_terracotta",
                "minecraft:white_terracotta", "minecraft:yellow_terracotta", "minecraft:light_blue_concrete", "minecraft:blue_terracotta"};
        double[] sizes = {0.8, 1.2, 1.3, 1.0, 3.0, 2.6, 1.8, 1.7};
        double dist = 8;
        for (int i = 0; i < 8; i++) {
            dist += sizes[i] + 3.2;
            double a = i * 0.8;
            double x = Math.cos(a) * dist, z = Math.sin(a) * dist;
            b.add(torusY(0, 1.0, 0, dist, 0.15), "minecraft:white_stained_glass");
            b.add(limb(x, 1, z, x, 6, z, 0.2, 0.2), "minecraft:iron_bars");
            b.add(sphere(x, 6, z, sizes[i]), mats[i]);
            if (i == 5) {
                b.add(torusY(x, 6, z, sizes[i] + 1.4, 0.3), "minecraft:white_terracotta");
            }
        }
        b.add(cylinderY(0, 0, 0, 1.0, 3.5), "minecraft:polished_blackstone");
        return b.build();
    }

    static Sculpt spaceStation() {
        Sculpt.Builder b = Sculpt.builder(10);
        String w = "minecraft:white_concrete";
        b.add(limb(-16, 6, 0, 16, 6, 0, 0.6, 0.6), "minecraft:light_gray_concrete");
        b.add(frustum(new Vec3(0, 6, -8), new Vec3(0, 6, 8), 2.0, 2.0), w);
        b.add(frustum(new Vec3(-4, 6, -3), new Vec3(4, 6, -3), 1.6, 1.6), w);
        b.add(sphere(0, 6, 8, 2.2), w);
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 2; k++) {
                double x = side * (8 + k * 6);
                b.add(box(x - 2.2, 6.2, -7, x + 2.2, 6.4, -1), Mat.stripesX(1.1, 0.25, Mat.of("minecraft:blue_concrete"), Mat.of("minecraft:light_gray_concrete")));
                b.add(box(x - 2.2, 6.2, 1, x + 2.2, 6.4, 7), Mat.stripesX(1.1, 0.25, Mat.of("minecraft:blue_concrete"), Mat.of("minecraft:light_gray_concrete")));
            }
        }
        b.paint(boxC(0, 6, 0, 2.2, 2.2, 0.2), "minecraft:gold_block");
        b.add(limb(0, 8, 2, 0, 11, 2, 0.2, 0.2), "minecraft:iron_bars");
        b.add(limb(0, 6, -8, 0, 6, -11, 0.8, 0.8), "minecraft:light_gray_concrete");
        b.paint(boxC(0, 6, 10.0, 0.6, 0.6, 0.4), "minecraft:light_blue_stained_glass");
        return b.build();
    }

    static Sculpt ufo() {
        Sculpt.Builder b = Sculpt.builder(10);
        b.add(ellipsoid(0, 5.0, 0, 11, 2.0, 11), "minecraft:light_gray_concrete");
        b.add(ellipsoid(0, 6.6, 0, 5, 3.6, 5).and(box(-6, 6.0, -6, 6, 11, 6)), "minecraft:light_blue_stained_glass");
        b.paint(torusY(0, 5.0, 0, 9.5, 0.7), (x, y, z) -> (Math.floorMod((long) Math.floor(Math.atan2(z, x) * 4), 2L) == 0)
                ? "minecraft:sea_lantern" : "minecraft:lime_concrete");
        b.add(ellipsoid(0, 3.4, 0, 4, 1.0, 4), "minecraft:gray_concrete");
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3;
            b.add(limb(Math.cos(a) * 4, 3.4, Math.sin(a) * 4, Math.cos(a) * 6, 0.4, Math.sin(a) * 6, 0.25, 0.25), "minecraft:iron_block");
        }
        return b.build();
    }

    static Sculpt alien() {
        String skin = "minecraft:lime_concrete";
        return Humanoid.build(Humanoid.spec().skin(skin).top(skin).sleeve(skin).pants(skin).shoes(skin).style(HairStyles.BALD).noEyebrows()
                .mouth(null).eyes("minecraft:black_concrete").bulk(0.8)
                .extra(b -> {
                    b.add(ellipsoid(0, 28.0, 0, 5.4, 5.4, 4.8), skin);
                    b.paintSym(ellipsoid(2.0, 27.0, 4.0, 1.6, 2.4, 1.4).rotated(0, 0, -25, new Vec3(2, 27, 4)), "minecraft:black_concrete");
                    b.sym(limb(1.4, 32.0, 0, 2.6, 35.0, 0, 0.25, 0.2), skin);
                    b.sym(sphere(2.6, 35.2, 0, 0.6), "minecraft:magenta_concrete");
                    b.paint(boxC(0, 18.0, 2.4, 2, 2, 0.8), "minecraft:green_concrete");
                }));
    }

    static VoxelModel moonBase(int width, long seed) {
        VoxelGrid g = new VoxelGrid();
        Rng rng = new Rng(seed);
        int r = Math.max(10, width / 2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                g.set(dx, 0, dz, Noise.hash(seed, dx, 0, dz) < 0.2 ? "minecraft:gray_concrete_powder" : "minecraft:light_gray_concrete_powder");
                g.set(dx, -1, dz, "minecraft:stone");
            }
        }
        for (int i = 0; i < 4; i++) {
            int cx = rng.range(-r / 2, r / 2), cz = rng.range(-r / 2, r / 2);
            Arch.ring(g, cx, 1, cz, 2, "minecraft:gray_concrete_powder");
        }
        int dr = Math.max(4, r / 3);
        Arch.dome(g, -dr, 1, 0, dr, dr, dr, "minecraft:glass", true);
        Arch.disk(g, -dr, 0, 0, dr, "minecraft:white_concrete");
        Arch.dome(g, dr + 2, 1, -dr / 2, dr - 1, dr - 1, dr - 1, "minecraft:white_concrete", true);
        Arch.disk(g, dr + 2, 0, -dr / 2, dr - 1, "minecraft:white_concrete");
        g.fill(0, 1, -1, dr - 1, 3, 1, "minecraft:white_concrete");
        g.fill(0, 2, 0, dr - 1, 2, 0, B.AIR);
        g.fill(-dr, 1, 0, -dr, 1, 0, "minecraft:moss_block");
        g.fill(-dr + 1, 1, 1, -dr + 1, 1, 1, "minecraft:moss_block");
        g.set(-dr, 2, 0, "minecraft:oak_sapling[stage=0]");
        g.fill(dr + 2, 1 + dr, -dr / 2, dr + 2, dr + 5, -dr / 2, "minecraft:iron_bars");
        Arch.dome(g, dr + 2, dr + 6, -dr / 2, 2, 1, 2, "minecraft:white_concrete", false);
        g.fill(r / 2, 1, r / 2, r / 2 + 2, 1, r / 2 + 3, "minecraft:white_concrete");
        g.set(r / 2 + 1, 2, r / 2 + 1, "minecraft:light_blue_stained_glass");
        g.fill(-2, 1, r - 3, -2, 5, r - 3, "minecraft:iron_bars");
        g.fill(-1, 3, r - 3, 2, 5, r - 3, "minecraft:white_concrete");
        g.fill(-1, 5, r - 3, 0, 5, r - 3, "minecraft:blue_concrete");
        return g.freeze();
    }

    static Sculpt satellite() {
        Sculpt.Builder b = Sculpt.builder(8);
        b.add(boxC(0, 5, 0, 2, 2, 2.6), "minecraft:gold_block");
        Mat panel = Mat.stripesX(1.0, 0.2, Mat.of("minecraft:blue_concrete"), Mat.of("minecraft:light_gray_concrete"));
        b.sym(limb(2, 5, 0, 4, 5, 0, 0.25, 0.25), "minecraft:light_gray_concrete");
        b.sym(box(4, 4.9, -2.2, 12, 5.1, 2.2), panel);
        b.add(frustum(new Vec3(0, 5, 2.6), new Vec3(0, 5, 4.0), 0.4, 0.4), "minecraft:light_gray_concrete");
        b.add(frustum(new Vec3(0, 5, 4.0), new Vec3(0, 5, 5.0), 1.0, 3.0), "minecraft:white_concrete");
        b.carve(frustum(new Vec3(0, 5, 4.6), new Vec3(0, 5, 5.2), 0.6, 2.6));
        b.add(limb(0, 3, 0, 0, 0, 0, 0.3, 0.3), "minecraft:iron_bars");
        return b.build();
    }

    static Sculpt cruiser() {
        Sculpt.Builder b = Sculpt.builder(10);
        String hull = "minecraft:light_gray_concrete";
        b.add(tri(new Vec3(0, 4, 25), new Vec3(-11, 4, -15), new Vec3(11, 4, -15), 2.4), hull);
        b.add(tri(new Vec3(0, 6.6, 18), new Vec3(-7, 6.6, -14), new Vec3(7, 6.6, -14), 1.4), hull);
        b.add(boxC(0, 10.0, -10, 3.0, 2.4, 2.4), hull);
        b.add(boxC(0, 12.6, -10, 5.0, 0.6, 1.4), hull);
        b.sym(sphere(4.4, 13.6, -10, 0.9), "minecraft:light_gray_concrete");
        b.paint(boxC(0, 12.6, -8.6, 4.6, 0.3, 0.3), "minecraft:light_blue_stained_glass");
        for (int i = -1; i <= 1; i++) {
            b.add(frustum(new Vec3(i * 5, 4, -15), new Vec3(i * 5, 4, -16.6), 1.6, 1.6), "minecraft:light_blue_concrete");
        }
        b.paint(boxC(0, 5.2, 0, 12, 0.3, 20), Mat.mix(12, 0.8, 0.2, Mat.of(hull), Mat.of("minecraft:gray_concrete")));
        return b.build();
    }

    static Sculpt fighter() {
        Sculpt.Builder b = Sculpt.builder(8);
        String w = "minecraft:white_concrete";
        b.add(frustum(new Vec3(0, 4, -6), new Vec3(0, 4, 4), 1.2, 1.0), w);
        b.add(frustum(new Vec3(0, 4, 4), new Vec3(0, 4, 11), 1.0, 0.4), w);
        b.add(ellipsoid(0, 5.0, 1.0, 0.7, 0.7, 1.6), "minecraft:black_stained_glass");
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                Vec3 tip = new Vec3(sx * 9, 4 + sy * 3, -3);
                b.add(tri(new Vec3(0, 4, -2), tip, new Vec3(0, 4, -6), 0.25), w);
                b.add(limb(tip, tip.add(new Vec3(0, 0, 7)), 0.25, 0.2), "minecraft:light_gray_concrete");
                Vec3 eng = new Vec3(sx * 2.0, 4 + sy * 1.2, -4);
                b.add(frustum(eng, eng.add(new Vec3(0, 0, 4)), 0.7, 0.7), "minecraft:light_gray_concrete");
                b.add(frustum(eng, eng.add(new Vec3(0, 0, -0.6)), 0.5, 0.5), "minecraft:red_concrete");
            }
        }
        b.paint(boxC(0, 4.9, -1, 0.3, 0.3, 4), "minecraft:red_concrete");
        return b.build();
    }

    static Sculpt battleStation() {
        Sculpt.Builder b = Sculpt.builder(24);
        b.add(sphere(0, 12, 0, 12), Mat.mix(13, 0.9, 0.25, Mat.of("minecraft:gray_concrete"), Mat.of("minecraft:light_gray_concrete")));
        b.carve(torusY(0, 12, 0, 12.2, 0.5));
        b.carve(sphere(5, 17, 9.5, 3.4));
        b.paint(sphere(5, 17, 9.5, 4.2), "minecraft:black_concrete");
        b.add(sphere(5, 17, 8.2, 0.7), "minecraft:lime_concrete");
        return b.build();
    }

    static Sculpt stargate() {
        Sculpt.Builder b = Sculpt.builder(24);
        Vec3 c = new Vec3(0, 12.5, 0);
        b.add(torusY(0, 12.5, 0, 10.5, 1.7).rotated(0, 90, 0, c),
                (x, y, z) -> Math.floorMod((long) Math.floor(Math.atan2(y - 12.5, x) * 4.5), 3L) == 0 ? "minecraft:sea_lantern" : "minecraft:gray_concrete");
        b.add(ellipsoid(0, 12.5, 0, 9.0, 9.0, 0.3), "minecraft:light_blue_stained_glass");
        b.add(box(-8, 0, -3, 8, 1.2, 3), "minecraft:polished_deepslate");
        for (int i = 0; i < 4; i++) {
            b.add(box(-3, 0, 3 + i * 1.2, 3, 1.2 - i * 0.3, 4.2 + i * 1.2), "minecraft:polished_deepslate");
        }
        return b.build();
    }

    static Sculpt blackHole() {
        Sculpt.Builder b = Sculpt.builder(10);
        Vec3 c = new Vec3(0, 6, 0);
        b.add(sphere(0, 6, 0, 4.2), "minecraft:black_concrete");
        for (int i = 0; i < 5; i++) {
            double r = 6 + i * 2.3;
            String m = i < 2 ? "minecraft:yellow_concrete" : i < 4 ? "minecraft:orange_concrete" : "minecraft:red_concrete";
            b.add(torusY(0, 6, 0, r, 0.7).rotated(0, 0, 12, c), m);
        }
        b.add(torusY(0, 6, 0, 5.2, 0.5).rotated(0, 90, 0, c), "minecraft:white_concrete");
        return b.build();
    }

    static Sculpt asteroid() {
        Sculpt.Builder b = Sculpt.builder(24);
        Rng rng = new Rng(34);
        Mat rock = (x, y, z) -> {
            double h = Noise.hash(34, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            return h < 0.04 ? "minecraft:diamond_ore" : h < 0.09 ? "minecraft:gold_ore" : h < 0.15 ? "minecraft:iron_ore"
                    : h < 0.45 ? "minecraft:cobblestone" : "minecraft:stone";
        };
        for (int i = 0; i < 9; i++) {
            b.add(sphere(rng.range(-5, 5), 12 + rng.range(-4, 4), rng.range(-5, 5), rng.range(5, 8)), rock);
        }
        for (int i = 0; i < 6; i++) {
            Vec3 p = new Vec3(rng.range(-9, 9), 12 + rng.range(-9, 9), rng.range(-9, 9));
            b.carve(sphere(p.x(), p.y(), p.z(), rng.range(1.5, 2.6)));
        }
        b.add(limb(0, 0, 0, 0, 5, 0, 0.3, 0.3), "minecraft:iron_bars");
        return b.build();
    }

    static Sculpt android() {
        String m = "minecraft:white_concrete";
        String d = "minecraft:light_gray_concrete";
        return Humanoid.build(Humanoid.spec().skin(m).hair(d).top(m).sleeve(d).forearm(m).hands(d).pants(d).shoes("minecraft:black_concrete")
                .eyes("minecraft:cyan_concrete").mouth("minecraft:gray_concrete").style(HairStyles.BALD).noEyebrows()
                .extra(b -> {
                    b.add(Humanoid.cap(0.5, -0.4), d);
                    b.paint(boxC(0, 27.0, 3.4, 3.4, 0.8, 1.2), "minecraft:black_concrete");
                    b.paint(boxC(0, 27.0, 4.0, 2.8, 0.35, 0.8), "minecraft:cyan_concrete");
                    b.paint(sphere(0, 19.0, 2.6, 1.0), "minecraft:light_blue_concrete");
                    b.paint(boxC(0, 16.0, 2.4, 0.2, 2.0, 0.8), "minecraft:cyan_concrete");
                    b.sym(sphere(4.2, 26.4, 0, 0.8), "minecraft:light_blue_concrete");
                }));
    }

    static VoxelModel launchPad(int height) {
        VoxelGrid g = new VoxelGrid();
        int h = Math.max(20, height);
        int pad = Math.max(8, h / 4);
        Arch.floor(g, Arch.Rect.centered(pad * 2 + 1, pad * 2 + 1), 0, "minecraft:gray_concrete");
        g.fill(-3, 0, -3, 3, 0, 3, "minecraft:black_concrete");
        VoxelModel rk = VehicleModels.rocket().voxelize(h * 4 / 5, false, true).freeze().groundAtBottom();
        g.paste(rk, 0, 1, 0);
        int tx = rk.sizeX() / 2 + 3;
        for (int y = 1; y <= h * 4 / 5 + 2; y++) {
            Arch.walls(g, new Arch.Rect(tx, -2, tx + 3, 1), y, y, y % 4 == 0 ? "minecraft:red_concrete" : "minecraft:iron_bars");
        }
        for (int y = 8; y < h * 4 / 5; y += 8) {
            g.fill(rk.sizeX() / 2 + 1, y, 0, tx, y, 0, "minecraft:red_concrete");
        }
        return g.freeze();
    }
}
