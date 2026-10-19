package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.generator.BoxModel;
import com.ultimateautobuilder.core.model.VoxelModel;

import java.util.function.Supplier;

import static com.ultimateautobuilder.core.generator.BoxModel.bandsX;
import static com.ultimateautobuilder.core.generator.BoxModel.bandsY;
import static com.ultimateautobuilder.core.generator.BoxModel.speckle;

/**
 * Mobs de Minecraft Java 1.21.1 reproduits en blocs à partir de la géométrie des modèles du jeu
 * (pavés en pixels de modèle). Façade vers +Z, origine au centre et au sol.
 */
public final class MobModels {
    public static final SizeSpec MOB = SizeSpec.dimension(8, 16, 32, 64);
    public static final SizeSpec BIG_MOB = SizeSpec.dimension(16, 32, 64, 128);

    private static final String BLACK = "black_concrete", WHITE = "white_concrete", GRAY = "gray_concrete",
            LGRAY = "light_gray_concrete", RED = "red_concrete", PINK = "pink_concrete", ORANGE = "orange_concrete",
            YELLOW = "yellow_concrete", LIME = "lime_concrete", GREEN = "green_concrete", CYAN = "cyan_concrete",
            LBLUE = "light_blue_concrete", BLUE = "blue_concrete", PURPLE = "purple_concrete", MAGENTA = "magenta_concrete",
            BROWN = "brown_concrete", TAN = "white_terracotta", SKIN = "orange_terracotta";

    private MobModels() {
    }

    private static void mob(Catalog c, String id, String name, int complexity, SizeSpec spec, Supplier<BoxModel> model, String... tags) {
        String[] all = new String[tags.length + 2];
        all[0] = "mob";
        all[1] = "minecraft";
        System.arraycopy(tags, 0, all, 2, tags.length);
        c.register(new ModelEntry("mob_" + id, name, Category.MOBS, all, complexity, spec, true, p -> {
            VoxelModel m = model.get().build(p.size());
            if (!p.hollow() || m.size() < 4000) {
                return m;
            }
            var g = m.toGrid();
            g.hollowInterior();
            return g.freeze().groundAtBottom();
        }));
    }

    public static void register(Catalog c) {
        // Monstres
        mob(c, "zombie", "Zombie", 2, MOB, () -> biped(speckle(GREEN, "green_terracotta", 0.25, 1), CYAN, "blue_terracotta", true)
                .faceSym(1, 27, 2, 1, 4, BLACK).face(-2, 25, 4, 1, 4, "green_terracotta"), "zombie", "monstre", "mort-vivant");
        mob(c, "husk", "Zombie desséché", 2, MOB, () -> biped(speckle("terracotta", "brown_terracotta", 0.25, 2), "brown_terracotta",
                "gray_terracotta", true).faceSym(1, 27, 2, 1, 4, BLACK), "husk", "desert", "zombie");
        mob(c, "drowned", "Noyé", 2, MOB, () -> biped(speckle(CYAN, "light_blue_terracotta", 0.3, 3), "lime_terracotta", "blue_terracotta", true)
                .faceSym(1, 27, 2, 1, 4, LBLUE).box(5, 10, 2, 1, 20, 1, "prismarine_bricks").box(4, 29, 1, 3, 1, 3, "prismarine_bricks"),
                "drowned", "ocean", "trident", "zombie");
        mob(c, "skeleton", "Squelette", 2, MOB, () -> skeleton(bandsY(LGRAY, WHITE, 1), "gray_concrete", true), "skeleton", "arc", "os");
        mob(c, "stray", "Vagabond", 2, MOB, () -> skeleton(bandsY(LGRAY, "light_blue_terracotta", 1), "light_blue_terracotta", true)
                .box(-4.5, 14, -2.5, 9, 6, 5, speckle("light_blue_terracotta", "gray_concrete", 0.3, 4)), "stray", "glace", "squelette");
        mob(c, "bogged", "Embourbé", 2, MOB, () -> skeleton(bandsY("lime_terracotta", LGRAY, 1), GREEN, true)
                .box(-3, 32, -2, 2, 2, 2, "red_mushroom_block").box(1, 32, 0, 2, 2, 2, "brown_mushroom_block"), "bogged", "marais", "squelette");
        mob(c, "wither_skeleton", "Squelette wither", 3, MOB, () -> skeleton(bandsY(BLACK, "blackstone", 1), GRAY, false, "blackstone")
                .box(6, 4, 2, 1, 22, 1, "gray_concrete").box(5, 11, 1, 3, 1, 3, BROWN), "wither skeleton", "nether", "forteresse");
        mob(c, "creeper", "Creeper", 2, MOB, MobModels::creeper, "creeper", "explosion", "vert");
        mob(c, "spider", "Araignée", 3, MOB, () -> spider("black_terracotta", "gray_terracotta"), "spider", "monstre", "nuit");
        mob(c, "cave_spider", "Araignée venimeuse", 3, MOB, () -> spider("blue_terracotta", "cyan_terracotta"), "cave spider", "mine", "poison");
        mob(c, "enderman", "Enderman", 2, MOB, MobModels::enderman, "enderman", "end", "teleportation");
        mob(c, "endermite", "Endermite", 1, MOB, () -> segmented("purple_terracotta", MAGENTA, 4), "endermite", "end");
        mob(c, "silverfish", "Poisson d'argent", 1, MOB, () -> segmented(LGRAY, GRAY, 6), "silverfish", "insecte", "forteresse");
        mob(c, "witch", "Sorcière", 3, MOB, MobModels::witch, "witch", "potion", "chapeau");
        mob(c, "slime", "Slime", 1, MOB, () -> cube("slime_block", LIME, GREEN), "slime", "marais", "cube");
        mob(c, "magma_cube", "Cube de magma", 1, MOB, () -> cube("magma_block", "red_nether_bricks", ORANGE), "magma cube", "nether", "cube");
        mob(c, "ghast", "Ghast", 2, MOB, MobModels::ghast, "ghast", "nether", "fantome");
        mob(c, "blaze", "Blaze", 3, MOB, MobModels::blaze, "blaze", "nether", "feu");
        mob(c, "phantom", "Phantom", 2, MOB, MobModels::phantom, "phantom", "nuit", "insomnie");
        mob(c, "guardian", "Gardien", 3, MOB, () -> guardian("prismarine", "dark_prismarine", 1.0), "guardian", "ocean", "temple");
        mob(c, "elder_guardian", "Grand gardien", 3, MOB, () -> guardian("light_gray_terracotta", "white_terracotta", 2.0), "elder guardian", "ocean", "boss");
        mob(c, "shulker", "Shulker", 2, MOB, MobModels::shulker, "shulker", "end", "coquille");
        mob(c, "vex", "Vex", 2, MOB, () -> spirit(LGRAY, "white_stained_glass", GRAY), "vex", "illageois", "esprit");
        mob(c, "warden", "Warden", 4, BIG_MOB, MobModels::warden, "warden", "deep dark", "sculk", "boss");
        mob(c, "breeze", "Breeze", 3, MOB, MobModels::breeze, "breeze", "vent", "trial chambers");
        mob(c, "zombie_villager", "Zombie villageois", 2, MOB, () -> villager(speckle(GREEN, "green_terracotta", 0.3, 5), "brown_terracotta", "green_terracotta"),
                "zombie villager", "zombie", "village");
        // Illageois
        mob(c, "pillager", "Pillard", 3, MOB, () -> illager("gray_terracotta", "cyan_terracotta", BROWN).box(4, 14, 4, 2, 2, 8, BROWN)
                .box(2, 15, 10, 6, 1, 1, LGRAY), "pillager", "arbalete", "patrouille");
        mob(c, "vindicator", "Vindicateur", 3, MOB, () -> illager("gray_terracotta", "black_terracotta", GRAY).box(6, 6, 1, 1, 12, 1, BROWN)
                .box(6, 4, 0, 1, 3, 4, LGRAY), "vindicator", "hache", "manoir");
        mob(c, "evoker", "Évocateur", 3, MOB, () -> illager("gray_terracotta", "black_concrete", YELLOW).box(-4.5, 6, 2.5, 9, 2, 0.5, YELLOW),
                "evoker", "magie", "totem");
        mob(c, "ravager", "Ravageur", 4, BIG_MOB, MobModels::ravager, "ravager", "illageois", "bete");
        // Nether
        mob(c, "piglin", "Piglin", 3, MOB, () -> piglin(speckle("pink_terracotta", PINK, 0.25, 6), BROWN, "gold_block"), "piglin", "nether", "or");
        mob(c, "piglin_brute", "Piglin barbare", 3, MOB, () -> piglin(speckle("pink_terracotta", PINK, 0.25, 7), BLACK, "gold_block")
                .box(-4.5, 12, -2.5, 9, 4, 5, "gold_block").box(5, 6, 2, 1, 14, 1, "gold_block"), "piglin brute", "bastion", "or");
        mob(c, "zombified_piglin", "Piglin zombifié", 3, MOB, () -> piglin(speckle("pink_terracotta", "lime_terracotta", 0.35, 8), "brown_terracotta", "gold_block")
                .box(-2, 14, 1.5, 4, 6, 1, LGRAY), "zombified piglin", "nether", "zombie");
        mob(c, "hoglin", "Hoglin", 3, MOB, () -> hoglin(speckle("pink_terracotta", "terracotta", 0.3, 9), "brown_terracotta"), "hoglin", "nether", "chasse");
        mob(c, "zoglin", "Zoglin", 3, MOB, () -> hoglin(speckle("lime_terracotta", "pink_terracotta", 0.4, 10), LGRAY), "zoglin", "nether", "zombie");
        mob(c, "strider", "Arpenteur", 2, MOB, MobModels::strider, "strider", "lave", "nether");
        // Boss
        mob(c, "wither", "Wither", 4, BIG_MOB, MobModels::wither, "wither", "boss", "nether");
        mob(c, "ender_dragon", "Dragon de l'Ender", 5, BIG_MOB, MobModels::enderDragon, "ender dragon", "boss", "end", "dragon");
        // Golems et personnages
        mob(c, "iron_golem", "Golem de fer", 3, MOB, MobModels::ironGolem, "iron golem", "village", "protecteur");
        mob(c, "snow_golem", "Golem de neige", 2, MOB, MobModels::snowGolem, "snow golem", "neige", "citrouille");
        mob(c, "villager", "Villageois", 2, MOB, () -> villager(TAN, BROWN, "brown_terracotta"), "villager", "village", "commerce");
        mob(c, "villager_farmer", "Villageois fermier", 2, MOB, () -> villager(TAN, "yellow_terracotta", BROWN).box(-6, 32, -6, 12, 1, 12, "hay_block")
                .box(-3, 33, -3, 6, 2, 6, "hay_block"), "farmer", "village", "ble");
        mob(c, "villager_librarian", "Villageois bibliothécaire", 2, MOB, () -> villager(TAN, WHITE, RED).box(-4.5, 32, -4.5, 9, 2, 9, RED),
                "librarian", "village", "livres");
        mob(c, "villager_smith", "Villageois forgeron", 2, MOB, () -> villager(TAN, BROWN, BLACK).box(-4.5, 8, 3, 9, 14, 1, BLACK),
                "armorer", "village", "forge");
        mob(c, "wandering_trader", "Marchand ambulant", 2, MOB, () -> villager(TAN, BLUE, "light_blue_terracotta").box(-4.5, 28, -4.5, 9, 4, 9, BLUE),
                "wandering trader", "commerce", "lamas");
        mob(c, "steve", "Steve", 2, MOB, () -> player(SKIN, "brown_concrete", CYAN, BLUE, "blue_terracotta"), "steve", "joueur", "personnage");
        mob(c, "alex", "Alex", 2, MOB, () -> player(TAN, ORANGE, LIME, "brown_terracotta", BROWN), "alex", "joueur", "personnage");
        mob(c, "allay", "Allay", 2, MOB, () -> spirit(LBLUE, "light_blue_stained_glass", BLUE), "allay", "aide", "esprit");
        // Animaux de ferme
        mob(c, "cow", "Vache", 2, MOB, () -> cow(speckle("brown_concrete", WHITE, 0.3, 11), WHITE), "cow", "ferme", "lait");
        mob(c, "mooshroom", "Champimeuh", 2, MOB, () -> cow(speckle(RED, WHITE, 0.25, 12), WHITE).box(-4, 22, -6, 3, 2, 3, "red_mushroom_block")
                .box(1, 22, 2, 3, 2, 3, "red_mushroom_block").box(-2, 28, 10, 3, 2, 3, "red_mushroom_block"), "mooshroom", "champignon", "vache");
        mob(c, "pig", "Cochon", 2, MOB, MobModels::pig, "pig", "ferme", "jambon");
        mob(c, "sheep", "Mouton", 2, MOB, () -> sheep("white_wool"), "sheep", "laine", "ferme");
        mob(c, "sheep_pink", "Mouton rose", 2, MOB, () -> sheep("pink_wool"), "sheep", "laine", "rose");
        mob(c, "chicken", "Poulet", 1, MOB, MobModels::chicken, "chicken", "oeuf", "ferme");
        mob(c, "horse", "Cheval", 3, MOB, () -> horse(speckle("brown_concrete", "brown_terracotta", 0.3, 13), BLACK, false), "horse", "monture");
        mob(c, "horse_white", "Cheval blanc", 3, MOB, () -> horse(speckle(WHITE, LGRAY, 0.15, 14), LGRAY, false), "horse", "monture", "blanc");
        mob(c, "donkey", "Âne", 2, MOB, () -> horse(speckle("gray_terracotta", "light_gray_terracotta", 0.25, 15), "brown_terracotta", true), "donkey", "monture");
        mob(c, "mule", "Mule", 2, MOB, () -> horse(speckle("brown_terracotta", "black_terracotta", 0.25, 16), BLACK, true), "mule", "monture");
        mob(c, "skeleton_horse", "Cheval squelette", 3, MOB, () -> horse(bandsY(LGRAY, WHITE, 1), GRAY, false), "skeleton horse", "orage");
        mob(c, "zombie_horse", "Cheval zombie", 3, MOB, () -> horse(speckle(GREEN, "green_terracotta", 0.3, 17), "green_terracotta", false), "zombie horse");
        mob(c, "llama", "Lama", 2, MOB, () -> llama(speckle(TAN, "light_gray_terracotta", 0.2, 18), null), "llama", "montagne", "crachat");
        mob(c, "trader_llama", "Lama de marchand", 2, MOB, () -> llama(speckle("brown_terracotta", "terracotta", 0.2, 19), bandsX(BLUE, YELLOW, 1)),
                "trader llama", "commerce");
        mob(c, "camel", "Dromadaire", 3, MOB, MobModels::camel, "camel", "desert", "monture");
        mob(c, "goat", "Chèvre", 2, MOB, MobModels::goat, "goat", "montagne", "cornes");
        mob(c, "rabbit", "Lapin", 1, MOB, MobModels::rabbit, "rabbit", "carotte");
        // Animaux sauvages
        mob(c, "wolf", "Loup", 2, MOB, () -> wolf(speckle(LGRAY, WHITE, 0.3, 20), false), "wolf", "chien", "os");
        mob(c, "wolf_tamed", "Loup apprivoisé", 2, MOB, () -> wolf(speckle(LGRAY, WHITE, 0.3, 21), true), "wolf", "chien", "collier");
        mob(c, "fox", "Renard", 2, MOB, MobModels::fox, "fox", "taiga", "baies");
        mob(c, "cat", "Chat", 2, MOB, () -> feline(speckle(ORANGE, "orange_terracotta", 0.3, 22), WHITE), "cat", "village", "chaton");
        mob(c, "ocelot", "Ocelot", 2, MOB, () -> feline(speckle(YELLOW, BROWN, 0.25, 23), YELLOW), "ocelot", "jungle", "felin");
        mob(c, "polar_bear", "Ours polaire", 3, MOB, () -> bear(speckle(WHITE, "snow_block", 0.3, 24), WHITE, false), "polar bear", "banquise");
        mob(c, "panda", "Panda", 3, MOB, () -> bear(WHITE, BLACK, true), "panda", "bambou", "jungle");
        mob(c, "sniffer", "Renifleur", 4, MOB, MobModels::sniffer, "sniffer", "graine", "ancien");
        mob(c, "armadillo", "Tatou", 2, MOB, MobModels::armadillo, "armadillo", "savane", "carapace");
        mob(c, "frog", "Grenouille", 1, MOB, () -> frog(speckle("orange_terracotta", "terracotta", 0.3, 25)), "frog", "marais");
        mob(c, "frog_green", "Grenouille verte", 1, MOB, () -> frog(speckle(GREEN, "lime_terracotta", 0.3, 26)), "frog", "marais", "froid");
        mob(c, "turtle", "Tortue", 2, MOB, MobModels::turtle, "turtle", "plage", "carapace");
        mob(c, "bee", "Abeille", 2, MOB, MobModels::bee, "bee", "miel", "ruche");
        mob(c, "parrot", "Perroquet", 2, MOB, MobModels::parrot, "parrot", "jungle", "oiseau");
        mob(c, "bat", "Chauve-souris", 1, MOB, MobModels::bat, "bat", "grotte");
        // Créatures aquatiques
        mob(c, "axolotl", "Axolotl", 2, MOB, MobModels::axolotl, "axolotl", "grotte luxuriante");
        mob(c, "dolphin", "Dauphin", 2, MOB, MobModels::dolphin, "dolphin", "ocean");
        mob(c, "squid", "Poulpe", 2, MOB, () -> squid("blue_concrete", "light_blue_terracotta"), "squid", "encre", "ocean");
        mob(c, "glow_squid", "Poulpe luisant", 2, MOB, () -> squid("cyan_concrete", "prismarine_bricks"), "glow squid", "grotte");
        mob(c, "cod", "Morue", 1, MOB, () -> fish("brown_terracotta", "white_terracotta", 9), "cod", "poisson");
        mob(c, "salmon", "Saumon", 1, MOB, () -> fish("red_terracotta", "gray_terracotta", 11), "salmon", "poisson", "riviere");
        mob(c, "tropical_fish", "Poisson tropical", 1, MOB, () -> fish(bandsX(ORANGE, WHITE, 1), BLACK, 7), "tropical fish", "recif");
        mob(c, "pufferfish", "Poisson-globe", 1, MOB, MobModels::pufferfish, "pufferfish", "poison");
    }

    // ------------------------------------------------------------------ gabarits

    /** Bipède standard (zombie, joueur) : jambes 4×12, buste 8×12×4, tête 8³. */
    private static BoxModel biped(BoxModel.Paint skin, String shirt, String pants, boolean armsForward) {
        BoxModel m = new BoxModel()
                .sym(0, 0, -2, 4, 12, 4, pants)
                .box(-4, 12, -2, 8, 12, 4, shirt)
                .box(-4, 24, -4, 8, 8, 8, skin);
        if (armsForward) {
            m.sym(4, 20, -2, 4, 4, 4, shirt).sym(4, 20, 2, 4, 4, 8, skin);
        } else {
            m.sym(4, 12, -2, 4, 12, 4, skin).sym(4, 18, -2, 4, 6, 4, shirt);
        }
        return m;
    }

    private static BoxModel player(String skin, String hair, String shirt, String pants, String shoes) {
        return biped(BoxModel.solid(skin), shirt, pants, false)
                .sym(0, 0, -2, 4, 2, 4, shoes)
                .box(-4, 29, -4, 8, 3, 8, hair).box(-4, 24, -4, 8, 8, 2, hair).sym(3, 26, -4, 1, 3, 6, hair)
                .faceSym(1, 27, 2, 1, 4, WHITE).faceSym(1, 27, 1, 1, 4, "blue_concrete")
                .face(-2, 25, 4, 1, 4, "brown_terracotta");
    }

    private static BoxModel skeleton(BoxModel.Paint bones, String bow, boolean withBow) {
        return skeleton(bones, bow, withBow, LGRAY);
    }

    private static BoxModel skeleton(BoxModel.Paint bones, String bow, boolean withBow, String head) {
        BoxModel m = new BoxModel()
                .sym(1, 0, -1, 2, 12, 2, bones)
                .box(-4, 12, -2, 8, 12, 4, bones)
                .sym(5, 12, -1, 2, 12, 2, bones)
                .box(-4, 24, -4, 8, 8, 8, BoxModel.solid(head))
                .faceSym(1, 26, 2, 2, 4, BLACK).face(-1, 25, 2, 1, 4, GRAY);
        if (withBow) {
            m.box(-7, 8, 2, 1, 14, 1, bow).box(-7, 14, 1, 1, 2, 3, LGRAY);
        }
        return m;
    }

    private static BoxModel creeper() {
        BoxModel.Paint skin = speckle(LIME, GREEN, 0.35, 30);
        return new BoxModel()
                .sym(0, 0, 2, 4, 6, 4, skin).sym(0, 0, -6, 4, 6, 4, skin)
                .box(-4, 6, -2, 8, 12, 4, skin)
                .box(-4, 18, -4, 8, 8, 8, skin)
                .faceSym(1, 22, 2, 2, 4, BLACK)
                .face(-1, 20, 2, 2, 4, BLACK).faceSym(1, 18.5, 1, 2.5, 4, BLACK);
    }

    private static BoxModel enderman() {
        return new BoxModel()
                .sym(1, 0, -1, 2, 30, 2, BLACK)
                .box(-4, 30, -2, 8, 12, 4, BLACK)
                .sym(4, 12, -1, 2, 30, 2, BLACK)
                .box(-4, 42, -4, 8, 8, 8, BLACK)
                .faceSym(1, 45, 3, 1, 4, MAGENTA).faceSym(2, 45, 1, 1, 4, PURPLE)
                .box(-4, 10, 2, 8, 8, 8, "grass_block");
    }

    private static BoxModel spider(String body, String legs) {
        BoxModel m = new BoxModel()
                .box(-4, 5, 3, 8, 8, 8, body)
                .box(-3, 6, -3, 6, 6, 6, body)
                .box(-5, 5, -15, 10, 8, 12, speckle(body, legs, 0.2, 31));
        for (int i = 0; i < 4; i++) {
            double z = 3 - i * 3;
            m.sym(3, 9, z, 8, 2, 2, legs).sym(11, 0, z, 2, 11, 2, legs);
        }
        return m.faceSym(1, 9, 2, 2, 11, RED).faceSym(2.5, 11.5, 1, 1, 11, RED).faceSym(0.5, 7, 1, 1, 11, RED);
    }

    private static BoxModel segmented(String body, String tip, int segments) {
        BoxModel m = new BoxModel();
        for (int i = 0; i < segments; i++) {
            double w = 4 - Math.abs(i - segments / 2.0) * 0.5;
            m.box(-w / 2, 0, -i * 2.0, w, Math.max(2, w - 1), 2, i == 0 ? tip : body);
        }
        return m.faceSym(0.5, 1, 1, 1, 2, BLACK);
    }

    private static BoxModel witch() {
        return villager(TAN, PURPLE, "purple_terracotta")
                .box(-5, 32, -5, 10, 2, 10, BLACK).box(-3.5, 34, -3.5, 7, 3, 7, BLACK).box(-2, 37, -2, 4, 3, 4, BLACK)
                .box(-1, 40, -1, 2, 2, 2, BLACK).box(-3.5, 34, -3.5, 7, 1, 7, GREEN)
                .box(-0.5, 25, 6, 1, 1, 1, GREEN);
    }

    private static BoxModel cube(String outer, String inner, String eyes) {
        return new BoxModel().box(-8, 0, -8, 16, 16, 16, outer).box(-3, 5, -3, 6, 6, 6, inner)
                .faceSym(2, 10, 2, 2, 8, eyes).face(-1, 5, 2, 1, 8, eyes);
    }

    private static BoxModel ghast() {
        BoxModel m = new BoxModel().box(-8, 14, -8, 16, 16, 16, speckle(WHITE, "white_terracotta", 0.1, 32));
        double[][] t = {{-6, -6, 12}, {-1, -6, 9}, {4, -6, 13}, {-6, -1, 10}, {-1, -1, 14}, {4, -1, 9}, {-6, 4, 13}, {-1, 4, 11}, {4, 4, 10}};
        for (double[] v : t) {
            m.box(v[0], 14 - v[2], v[1], 2, v[2], 2, WHITE);
        }
        return m.faceSym(2, 22, 3, 1, 8, GRAY).faceSym(2, 20, 1, 2, 8, GRAY).face(-2, 17, 4, 2, 8, GRAY);
    }

    private static BoxModel blaze() {
        BoxModel m = new BoxModel().box(-4, 16, -4, 8, 8, 8, speckle(YELLOW, ORANGE, 0.35, 33));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            double r = i < 4 ? 9 : i < 8 ? 7 : 5;
            double y = i < 4 ? 12 : i < 8 ? 6 : 0;
            m.box(Math.cos(a) * r - 1, y, Math.sin(a) * r - 1, 2, 8, 2, "gold_block");
        }
        return m.faceSym(1, 19, 2, 1, 4, BLACK).box(-1, 6, -1, 2, 10, 2, "gray_stained_glass");
    }

    private static BoxModel phantom() {
        String skin = "blue_terracotta";
        return new BoxModel()
                .box(-2.5, 4, -4.5, 5, 3, 9, skin)
                .box(-3.5, 4, 4.5, 7, 3, 5, skin)
                .sym(2.5, 6, -4, 10, 1, 8, skin).sym(12.5, 7, -3, 8, 1, 6, "light_gray_terracotta")
                .box(-1.5, 4, -10, 3, 2, 6, skin).box(-1, 4, -14, 2, 1, 4, skin)
                .faceSym(1, 5, 2, 1, 9.5, LIME);
    }

    private static BoxModel guardian(String body, String dark, double scale) {
        double s = scale;
        BoxModel m = new BoxModel().box(-6 * s, 4 * s, -8 * s, 12 * s, 12 * s, 16 * s, speckle(body, dark, 0.25, 34))
                .box(-3 * s, 7 * s, -14 * s, 6 * s, 6 * s, 6 * s, body).box(-2 * s, 8 * s, -20 * s, 4 * s, 4 * s, 6 * s, dark);
        double[][] spikes = {{-1, 16, -1, 2, 6, 2}, {-1, -2, -1, 2, 6, 2}, {-9, 9, -1, 6, 2, 2}, {3, 9, -1, 6, 2, 2}, {-1, 9, 7, 2, 2, 6}};
        for (double[] v : spikes) {
            m.box(v[0] * s, v[1] * s, v[2] * s, v[3] * s, v[4] * s, v[5] * s, ORANGE);
        }
        return m.face(-2 * s, 8 * s, 4 * s, 4 * s, 8 * s, WHITE).face(-1 * s, 9 * s, 2 * s, 2 * s, 8 * s, RED);
    }

    private static BoxModel shulker() {
        return new BoxModel().box(-8, 0, -8, 16, 8, 16, "purpur_block").box(-8, 12, -8, 16, 4, 16, "purpur_block")
                .box(-7, 8, -7, 14, 4, 14, "purple_terracotta").box(-3, 6, -3, 6, 7, 6, YELLOW)
                .faceSym(1, 9, 1, 1, 3, BLACK);
    }

    private static BoxModel spirit(String body, String wings, String detail) {
        return new BoxModel().box(-2, 6, -1, 4, 6, 2, body).box(-2.5, 12, -2.5, 5, 5, 5, body)
                .sym(2, 7, -1, 1, 5, 1, body).box(-1, 3, -0.5, 2, 3, 1, body)
                .sym(1, 10, -6, 6, 6, 1, wings).faceSym(0.5, 14, 1, 1, 2.5, detail);
    }

    private static BoxModel warden() {
        BoxModel.Paint skin = speckle("black_terracotta", "cyan_terracotta", 0.25, 35);
        return new BoxModel()
                .sym(1, 0, -3, 6, 13, 6, skin)
                .box(-9, 13, -5.5, 18, 21, 11, skin)
                .box(-6, 20, 5, 12, 10, 0.5, "light_blue_terracotta").box(-3, 22, 5.2, 6, 6, 0.5, "sea_lantern")
                .sym(9, 6, -4, 8, 28, 8, skin)
                .box(-8, 34, -5, 16, 16, 10, skin)
                .sym(8, 42, -1, 7, 8, 2, "cyan_terracotta").sym(13, 44, -1, 3, 10, 2, "cyan_terracotta")
                .face(-6, 36, 12, 3, 5, "black_concrete").faceSym(2, 42, 3, 3, 5, "light_blue_terracotta");
    }

    private static BoxModel breeze() {
        BoxModel m = new BoxModel().box(-4, 18, -4, 8, 8, 8, speckle(LBLUE, WHITE, 0.3, 36))
                .box(-3, 10, -3, 6, 8, 6, "light_blue_stained_glass").box(-2, 4, -2, 4, 6, 4, "white_stained_glass")
                .box(-1, 0, -1, 2, 4, 2, "light_blue_stained_glass");
        for (int i = 0; i < 3; i++) {
            double a = i * 2 * Math.PI / 3;
            m.box(Math.cos(a) * 7 - 1, 14, Math.sin(a) * 7 - 1, 2, 10, 2, "light_blue_terracotta");
        }
        return m.faceSym(1, 21, 2, 2, 4, "blue_concrete");
    }

    private static BoxModel villager(Object skin, String robe, String detail) {
        BoxModel.Paint sk = skin instanceof BoxModel.Paint p ? p : BoxModel.solid((String) skin);
        return new BoxModel()
                .sym(0, 0, -2, 4, 12, 4, detail)
                .box(-4, 6, -3, 8, 18, 6, robe)
                .box(-6, 17, 3, 12, 4, 4, robe).sym(4, 17, -1, 2, 6, 4, robe)
                .box(-4, 24, -4, 8, 10, 8, sk)
                .box(-1, 25, 4, 2, 4, 2, sk)
                .face(-4, 30, 8, 1, 4, "brown_terracotta").faceSym(1, 28, 2, 1, 4, WHITE).faceSym(1.5, 28, 1, 1, 4, "green_concrete");
    }

    private static BoxModel illager(String skin, String robe, String detail) {
        return villager(skin, robe, robe).box(-4, 6, 3, 8, 1, 0.5, detail).faceSym(1, 28, 2, 1, 4.1, BLACK);
    }

    private static BoxModel ravager() {
        String skin = "gray_terracotta";
        return new BoxModel()
                .sym(3, 0, 4, 6, 18, 6, skin).sym(3, 0, -12, 6, 18, 6, skin)
                .box(-8, 18, -14, 16, 18, 26, speckle(skin, "light_gray_terracotta", 0.25, 37))
                .box(-6, 22, 10, 12, 8, 6, skin)
                .box(-8, 16, 14, 16, 18, 14, speckle("light_gray_terracotta", skin, 0.3, 38))
                .box(-4, 14, 26, 8, 6, 4, "black_terracotta")
                .sym(8, 26, 18, 6, 2, 2, TAN).sym(12, 28, 18, 2, 8, 2, TAN)
                .faceSym(3, 26, 2, 2, 28, BLACK)
                .box(-7, 36, -6, 14, 2, 10, BROWN);
    }

    private static BoxModel piglin(BoxModel.Paint skin, String cloth, String gold) {
        return new BoxModel()
                .sym(0, 0, -2, 4, 12, 4, cloth)
                .box(-4, 12, -2, 8, 12, 4, skin).box(-4.5, 12, -2.5, 9, 4, 5, cloth)
                .sym(4, 12, -2, 4, 12, 4, skin)
                .box(-5, 24, -4, 10, 8, 8, skin)
                .box(-2, 25, 4, 4, 4, 1, "pink_terracotta").faceSym(0.5, 26, 1, 2, 5, BLACK)
                .sym(5, 26, -2, 1, 5, 4, skin).sym(-6, 28, 4, 1, 3, 1, "white_concrete")
                .faceSym(1, 29, 2, 1, 4, BLACK).box(-6, 8, 2, 1, 12, 1, gold);
    }

    private static BoxModel hoglin(BoxModel.Paint skin, String mane) {
        return new BoxModel()
                .sym(2, 0, 6, 5, 12, 5, skin).sym(2, 0, -12, 5, 12, 5, skin)
                .box(-8, 12, -14, 16, 14, 26, skin)
                .box(-1, 26, -12, 2, 4, 22, mane)
                .box(-7, 10, 12, 14, 10, 16, skin)
                .sym(7, 16, 18, 2, 8, 2, WHITE)
                .sym(7, 20, 14, 4, 1, 4, skin)
                .faceSym(2, 15, 2, 2, 28, BLACK);
    }

    private static BoxModel strider() {
        return new BoxModel()
                .sym(2, 0, -2, 4, 16, 4, "purple_terracotta")
                .box(-8, 16, -8, 16, 14, 16, speckle("red_terracotta", RED, 0.3, 39))
                .faceSym(2, 23, 3, 2, 8, BLACK).face(-3, 19, 6, 1, 8, BLACK)
                .sym(3, 30, -6, 1, 6, 1, PURPLE).sym(6, 30, 2, 1, 7, 1, PURPLE).box(-1, 30, 4, 1, 5, 1, PURPLE);
    }

    private static BoxModel wither() {
        String bone = "blackstone";
        return new BoxModel()
                .box(-1.5, 0, -1.5, 3, 16, 3, bone)
                .box(-5, 8, -2, 10, 2, 3, bone).box(-5, 11, -2, 10, 2, 3, bone).box(-5, 14, -2, 10, 2, 3, bone)
                .box(-10, 16, -1.5, 20, 3, 3, bone)
                .box(-4, 19, -4, 8, 8, 8, BLACK).faceSym(1, 22, 2, 2, 4, WHITE)
                .box(-13, 17, -3, 6, 6, 6, BLACK).box(7, 17, -3, 6, 6, 6, BLACK)
                .face(-12, 19, 4, 1, 3, WHITE).face(8, 19, 4, 1, 3, WHITE);
    }

    private static BoxModel enderDragon() {
        BoxModel.Paint skin = speckle(BLACK, "black_terracotta", 0.3, 40);
        BoxModel m = new BoxModel()
                .sym(5, 0, 8, 6, 22, 6, skin).sym(5, 0, -20, 6, 22, 6, skin)
                .box(-12, 22, -24, 24, 24, 48, skin)
                .box(-1.5, 46, -20, 3, 4, 3, GRAY).box(-1.5, 46, -6, 3, 4, 3, GRAY).box(-1.5, 46, 8, 3, 4, 3, GRAY);
        for (int i = 0; i < 5; i++) {
            m.box(-5, 34 + i * 2, 24 + i * 9, 10, 10, 10, skin).box(-1, 44 + i * 2, 27 + i * 9, 2, 3, 3, GRAY);
        }
        m.box(-8, 42, 69, 16, 16, 16, skin).box(-6, 40, 85, 12, 6, 16, skin).box(-6, 36, 85, 12, 4, 16, "black_terracotta")
                .sym(3, 58, 72, 2, 4, 6, GRAY).faceSym(2, 52, 4, 2, 85.5, MAGENTA);
        for (int i = 0; i < 8; i++) {
            m.box(-5, 30 - i, -34 - i * 10, 10, 10, 10, skin).box(-1, 40 - i, -31 - i * 10, 2, 3, 3, GRAY);
        }
        return m.sym(12, 42, -14, 56, 4, 4, skin).sym(12, 44, -14, 52, 1, 30, speckle(GRAY, BLACK, 0.3, 41));
    }

    private static BoxModel ironGolem() {
        BoxModel.Paint iron = speckle("white_terracotta", "light_gray_terracotta", 0.35, 42);
        return new BoxModel()
                .sym(1, 0, -2.5, 6, 16, 5, iron)
                .box(-9, 16, -5.5, 18, 12, 11, iron).box(-4.5, 12, -3, 9, 5, 6, iron)
                .sym(9, 2, -3, 4, 30, 6, iron)
                .box(-4, 28, -4, 8, 10, 8, iron).box(-1, 29, 4, 2, 4, 2, iron)
                .faceSym(1, 33, 2, 1, 4, RED).face(-4, 35, 8, 1, 4, "light_gray_terracotta")
                .box(-9, 22, 5.5, 2, 6, 0.5, GREEN).box(4, 18, 5.5, 3, 8, 0.5, GREEN).box(9, 12, 3, 4, 6, 0.5, GREEN);
    }

    private static BoxModel snowGolem() {
        return new BoxModel()
                .box(-6, 0, -6, 12, 12, 12, "snow_block").box(-5, 12, -5, 10, 10, 10, "snow_block")
                .box(-4, 22, -4, 8, 8, 8, "carved_pumpkin[facing=south]")
                .sym(5, 18, -0.5, 9, 1, 1, "dark_oak_planks");
    }

    private static BoxModel cow(BoxModel.Paint hide, String face) {
        return new BoxModel()
                .sym(2, 0, 4, 4, 12, 4, hide).sym(2, 0, -8, 4, 12, 4, hide)
                .box(-6, 12, -9, 12, 10, 18, hide)
                .box(-2, 11, -6, 4, 1, 4, PINK)
                .box(-4, 16, 9, 8, 8, 6, hide)
                .face(-3, 16, 6, 4, 15, face).face(-2, 16, 4, 2, 15.1, "light_gray_concrete")
                .faceSym(2, 21, 2, 1, 15, BLACK)
                .sym(4, 23, 11, 1, 3, 1, LGRAY);
    }

    private static BoxModel pig() {
        return new BoxModel()
                .sym(1, 0, 3, 4, 6, 4, PINK).sym(1, 0, -7, 4, 6, 4, PINK)
                .box(-5, 6, -8, 10, 8, 16, PINK)
                .box(-4, 8, 8, 8, 8, 8, PINK)
                .box(-2, 9, 16, 4, 3, 1, "pink_terracotta").faceSym(0.5, 10, 1, 1, 17, "brown_terracotta")
                .faceSym(2, 13, 2, 1, 16, WHITE).faceSym(2, 13, 1, 1, 16.1, BLACK);
    }

    private static BoxModel sheep(String wool) {
        return new BoxModel()
                .sym(1, 0, 4, 4, 12, 4, "white_terracotta").sym(1, 0, -8, 4, 12, 4, "white_terracotta")
                .sym(0.5, 6, 3.5, 5, 6, 5, wool).sym(0.5, 6, -8.5, 5, 6, 5, wool)
                .box(-6, 11, -10, 12, 10, 20, wool)
                .box(-3, 15, 9, 6, 6, 8, "white_terracotta").box(-3.5, 19, 9, 7, 3, 6, wool)
                .faceSym(1, 18, 2, 1, 17, BLACK).face(-1, 16, 2, 1, 17, "pink_terracotta");
    }

    private static BoxModel chicken() {
        return new BoxModel()
                .sym(0.5, 0, -0.5, 1, 5, 1, ORANGE).sym(0, 0, 0, 2, 0.5, 2, ORANGE)
                .box(-3, 5, -4, 6, 6, 8, WHITE)
                .sym(3, 6, -3, 1, 4, 6, "white_terracotta")
                .box(-2, 9, 3, 4, 6, 3, WHITE)
                .box(-2, 11, 6, 4, 2, 2, ORANGE).box(-1, 9, 6, 2, 2, 2, RED)
                .faceSym(1, 13, 1, 1, 6, BLACK);
    }

    private static BoxModel horse(BoxModel.Paint hide, String mane, boolean longEars) {
        BoxModel m = new BoxModel()
                .sym(1, 0, 6, 4, 11, 4, hide).sym(1, 0, -11, 4, 11, 4, hide)
                .sym(1, 0, 6, 4, 2, 4, "gray_terracotta").sym(1, 0, -11, 4, 2, 4, "gray_terracotta")
                .box(-5, 11, -11, 10, 10, 22, hide)
                .box(-3, 17, 7, 6, 12, 7, hide)
                .box(-3, 25, 12, 6, 5, 11, hide)
                .box(-1, 22, 6, 2, 9, 2, mane).box(-1, 27, 8, 2, 4, 5, mane)
                .box(-1.5, 8, -14, 3, 12, 3, mane)
                .faceSym(2, 27, 1, 1, 23, BLACK);
        return longEars ? m.sym(1, 30, 12, 2, 6, 1, hide) : m.sym(1, 30, 12, 2, 3, 1, hide);
    }

    private static BoxModel llama(BoxModel.Paint wool, BoxModel.Paint carpet) {
        BoxModel m = new BoxModel()
                .sym(1, 0, 4, 4, 14, 4, wool).sym(1, 0, -8, 4, 14, 4, wool)
                .box(-6, 14, -9, 12, 10, 18, wool)
                .box(-4, 20, 6, 8, 18, 6, wool)
                .box(-2, 32, 12, 4, 4, 4, wool)
                .sym(1, 38, 7, 2, 3, 2, wool)
                .faceSym(2, 34, 1, 1, 12, BLACK);
        if (carpet != null) {
            m.box(-6.5, 20, -9.5, 13, 4.5, 19, carpet).box(-7, 14, -2, 1, 8, 6, BROWN).box(6, 14, -2, 1, 8, 6, BROWN);
        }
        return m;
    }

    private static BoxModel camel() {
        BoxModel.Paint hide = speckle("yellow_terracotta", "white_terracotta", 0.3, 43);
        return new BoxModel()
                .sym(2, 0, 7, 4, 21, 4, hide).sym(2, 0, -12, 4, 21, 4, hide)
                .box(-7, 21, -14, 14, 12, 28, hide)
                .box(-5, 33, -6, 10, 6, 10, hide).box(-3, 39, -4, 6, 2, 6, hide)
                .box(-3, 26, 13, 6, 18, 6, hide)
                .box(-3.5, 40, 16, 7, 6, 12, hide)
                .faceSym(2, 42, 1, 1, 28, BLACK).sym(3.5, 44, 17, 1, 2, 2, hide)
                .box(-7.5, 30, -14.5, 15, 2, 29, "red_terracotta");
    }

    private static BoxModel goat() {
        BoxModel.Paint fur = speckle(WHITE, "white_terracotta", 0.3, 44);
        return new BoxModel()
                .sym(1, 0, 4, 3, 10, 3, fur).sym(1, 0, -7, 3, 10, 3, fur)
                .box(-4.5, 10, -8, 9, 11, 16, fur)
                .box(-2.5, 16, 7, 5, 7, 10, fur)
                .sym(1, 23, 7, 1, 6, 2, "light_gray_terracotta")
                .box(-1, 13, 14, 2, 3, 2, "white_terracotta")
                .faceSym(2.5, 20, 0.6, 1, 17, BLACK);
    }

    private static BoxModel rabbit() {
        BoxModel.Paint fur = speckle(BROWN, "brown_terracotta", 0.3, 45);
        return new BoxModel()
                .sym(1, 0, -3, 2, 2, 5, fur).sym(1, 0, 3, 1, 3, 1, fur)
                .box(-3, 1, -4, 6, 5, 9, fur)
                .box(-2.5, 4, 4, 5, 4, 5, fur)
                .sym(0.5, 8, 6, 2, 5, 1, fur)
                .box(-1.5, 3, -6, 3, 3, 2, WHITE)
                .faceSym(1.5, 6, 1, 1, 9, BLACK).face(-0.5, 5, 1, 1, 9, PINK);
    }

    private static BoxModel wolf(BoxModel.Paint fur, boolean collar) {
        BoxModel m = new BoxModel()
                .sym(0.5, 0, 3, 2, 8, 2, fur).sym(0.5, 0, -6, 2, 8, 2, fur)
                .box(-3, 7, -7, 6, 6, 9, fur)
                .box(-4, 7, 1, 8, 7, 6, fur)
                .box(-3, 8, 6, 6, 6, 4, fur)
                .box(-1.5, 8, 10, 3, 3, 4, fur).face(-0.5, 10, 1, 1, 14, BLACK)
                .sym(1, 14, 7, 2, 2, 1, fur)
                .box(-1, 11, -10, 2, 2, 8, fur)
                .faceSym(1, 12, 1, 1, 10, BLACK);
        return collar ? m.box(-3.5, 8, 5.5, 7, 1, 1.5, RED) : m;
    }

    private static BoxModel fox() {
        return new BoxModel()
                .sym(0.5, 0, 3, 2, 6, 2, BLACK).sym(0.5, 0, -5, 2, 6, 2, BLACK)
                .box(-3, 5, -6, 6, 6, 11, speckle(ORANGE, "orange_terracotta", 0.25, 46))
                .box(-4, 7, 4, 8, 6, 6, ORANGE).box(-2, 7, 10, 4, 2, 3, WHITE).face(-1, 8, 2, 1, 13, BLACK)
                .sym(1, 13, 6, 2, 2, 1, BLACK)
                .box(-2, 6, -15, 4, 4, 9, ORANGE).box(-2, 6, -17, 4, 4, 2, WHITE)
                .box(-2.5, 5, 3, 5, 3, 2, WHITE)
                .faceSym(1, 10, 1, 1, 10, BLACK);
    }

    private static BoxModel feline(BoxModel.Paint fur, String chest) {
        return new BoxModel()
                .sym(0.5, 0, 3, 2, 6, 2, fur).sym(0.5, 0, -7, 2, 6, 2, fur)
                .box(-2, 5, -8, 4, 5, 13, fur)
                .box(-2.5, 6, 5, 5, 4, 5, fur).box(-1.5, 6, 10, 3, 2, 1, chest)
                .sym(0.5, 10, 7, 1.5, 1.5, 2, fur)
                .box(-0.5, 7, -16, 1, 1, 8, fur)
                .faceSym(1, 8, 1, 1, 10, LIME);
    }

    private static BoxModel bear(Object fur, String dark, boolean panda) {
        BoxModel.Paint f = fur instanceof BoxModel.Paint p ? p : BoxModel.solid((String) fur);
        BoxModel m = new BoxModel()
                .sym(1, 0, 6, 6, 10, 6, panda ? BoxModel.solid(dark) : f).sym(1, 0, -12, 6, 10, 6, panda ? BoxModel.solid(dark) : f)
                .box(-7, 10, -13, 14, 14, 26, f)
                .box(-5, 12, 13, 10, 10, 7, f)
                .box(-2, 12, 20, 4, 3, 3, panda ? WHITE : "white_terracotta").face(-1, 14, 2, 1, 23, BLACK)
                .sym(3, 22, 15, 2, 2, 1, panda ? dark : "white_terracotta");
        if (panda) {
            m.box(-7.5, 16, 5, 15, 8, 4, dark).faceSym(1, 16, 3, 3, 20, dark).faceSym(2, 17, 1, 1, 20.1, WHITE);
        } else {
            m.faceSym(2, 17, 1, 1, 20, BLACK);
        }
        return m;
    }

    private static BoxModel sniffer() {
        BoxModel.Paint hide = speckle("red_terracotta", "terracotta", 0.3, 47);
        return new BoxModel()
                .sym(4, 0, 8, 6, 10, 6, hide).sym(4, 0, -6, 6, 10, 6, hide).sym(4, 0, -20, 6, 10, 6, hide)
                .box(-12, 10, -24, 24, 22, 42, hide)
                .box(-12.5, 28, -22, 25, 5, 38, speckle("moss_block", GREEN, 0.3, 48))
                .box(-7, 16, 18, 14, 12, 10, hide)
                .box(-3, 12, 28, 6, 8, 8, "yellow_terracotta").box(-3.5, 12, 35, 7, 2, 1, "brown_terracotta")
                .sym(7, 20, 22, 2, 8, 2, hide)
                .faceSym(4, 24, 2, 2, 28, BLACK);
    }

    private static BoxModel armadillo() {
        return new BoxModel()
                .sym(1, 0, 2, 2, 3, 2, "brown_terracotta").sym(1, 0, -4, 2, 3, 2, "brown_terracotta")
                .box(-3.5, 3, -6, 7, 6, 12, bandsY("pink_terracotta", "brown_terracotta", 1))
                .box(-2, 3, 6, 4, 4, 4, "pink_terracotta")
                .sym(1, 7, 7, 1, 3, 1, "pink_terracotta")
                .box(-1, 4, -9, 2, 2, 3, "pink_terracotta")
                .faceSym(0.5, 5, 1, 1, 10, BLACK);
    }

    private static BoxModel frog(BoxModel.Paint skin) {
        return new BoxModel()
                .sym(2, 0, -3, 3, 2, 5, skin).sym(2, 0, 2, 2, 2, 2, skin)
                .box(-3.5, 1, -4.5, 7, 3, 9, skin)
                .box(-3.5, 4, -1, 7, 2, 6, skin)
                .sym(1, 6, 2, 3, 2, 3, skin).faceSym(1.5, 6.5, 1, 1, 5, BLACK)
                .face(-3, 4.5, 6, 0.5, 5, "brown_terracotta");
    }

    private static BoxModel turtle() {
        return new BoxModel()
                .sym(5, 0, 4, 8, 1, 4, "green_terracotta").sym(4, 0, -7, 4, 1, 4, "green_terracotta")
                .box(-9, 1, -10, 18, 3, 20, "green_terracotta")
                .box(-8, 4, -9, 16, 3, 18, speckle(GREEN, "brown_terracotta", 0.3, 49))
                .box(-5, 7, -6, 10, 2, 12, speckle(GREEN, "brown_terracotta", 0.3, 50))
                .box(-3, 1, 10, 6, 5, 6, "lime_terracotta")
                .faceSym(1.5, 4, 1, 1, 16, BLACK);
    }

    private static BoxModel bee() {
        return new BoxModel()
                .box(-3.5, 3, -5, 7, 7, 10, BoxModel.bandsZ(YELLOW, BLACK, 2))
                .face(-3.5, 3, 7, 7, 5, YELLOW).faceSym(1, 6, 2, 2, 5.1, BLACK).faceSym(1, 7, 1, 1, 5.2, CYAN)
                .sym(-1, 1, 0, 1, 2, 1, BLACK)
                .sym(1, 10, -3, 6, 1, 5, "white_stained_glass")
                .box(-0.5, 5, -7, 1, 1, 2, BLACK)
                .sym(1, 9, 5, 1, 3, 1, BLACK);
    }

    private static BoxModel parrot() {
        return new BoxModel()
                .sym(0.5, 0, -0.5, 1, 2, 1, "gray_terracotta")
                .box(-1.5, 2, -1.5, 3, 6, 4, RED)
                .sym(1.5, 3, -1.5, 1, 5, 3, speckle(BLUE, YELLOW, 0.35, 51))
                .box(-1, 8, -1, 2, 3, 3, RED).box(-0.5, 8, 2, 1, 2, 1, "yellow_terracotta")
                .box(-1, 0, -4, 2, 3, 2, BLUE).box(-0.5, 11, -1, 1, 2, 2, RED)
                .faceSym(0.5, 9.5, 0.5, 0.5, 2, BLACK);
    }

    private static BoxModel bat() {
        return new BoxModel()
                .box(-3, 4, -3, 6, 7, 6, "brown_terracotta").box(-3, 11, -3, 6, 5, 6, "brown_terracotta")
                .sym(1, 16, 0, 2, 2, 1, "brown_terracotta")
                .sym(3, 6, -0.5, 10, 8, 1, "black_terracotta").sym(13, 8, -0.5, 6, 6, 1, "black_terracotta")
                .faceSym(1, 13, 1, 1, 3, BLACK).box(-1, 0, -1, 2, 4, 2, "brown_terracotta");
    }

    private static BoxModel axolotl() {
        return new BoxModel()
                .sym(2, 0, 2, 3, 1, 2, PINK).sym(2, 0, -4, 3, 1, 2, PINK)
                .box(-2, 1, -5, 4, 3, 10, PINK)
                .box(-4, 1, 5, 8, 5, 5, PINK)
                .sym(4, 3, 7, 3, 4, 1, MAGENTA).box(-3, 6, 7, 6, 2, 1, MAGENTA)
                .box(-0.5, 1, -13, 1, 4, 8, "pink_stained_glass")
                .faceSym(2, 3, 1, 1, 10, BLACK).face(-1, 2, 2, 0.5, 10, "magenta_terracotta");
    }

    private static BoxModel dolphin() {
        return new BoxModel()
                .box(-4, 0, -6, 8, 7, 13, speckle(LGRAY, GRAY, 0.2, 52))
                .box(-4, 0, -6, 8, 2, 13, "white_terracotta")
                .box(-3, 1, 7, 6, 5, 5, LGRAY).box(-1, 1, 12, 2, 2, 4, "white_terracotta")
                .box(-0.5, 7, -2, 1, 5, 4, GRAY)
                .sym(4, 1, 0, 6, 1, 3, GRAY)
                .box(-2, 2, -13, 4, 4, 7, LGRAY).box(-5, 3, -16, 10, 1, 4, GRAY)
                .faceSym(2, 4, 1, 1, 12, BLACK);
    }

    private static BoxModel squid(String body, String tentacles) {
        BoxModel m = new BoxModel().box(-6, 12, -6, 12, 16, 12, body);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            m.box(Math.cos(a) * 5 - 1, 0, Math.sin(a) * 5 - 1, 2, 12, 2, tentacles);
        }
        return m.faceSym(2, 18, 2, 3, 6, WHITE).faceSym(2, 18, 1, 2, 6.1, BLACK);
    }

    private static BoxModel fish(Object body, String belly, double length) {
        BoxModel.Paint b = body instanceof BoxModel.Paint p ? p : BoxModel.solid((String) body);
        return new BoxModel()
                .box(-1.5, 1, -length / 2, 3, 4, length, b)
                .box(-1.5, 0, -length / 2 + 1, 3, 1, length - 2, belly)
                .box(-0.5, 5, -1, 1, 2, 3, b)
                .box(-0.5, 0.5, -length / 2 - 3, 1, 5, 3, b)
                .faceSym(0.5, 3, 1, 1, length / 2, BLACK);
    }

    private static BoxModel pufferfish() {
        return new BoxModel()
                .box(-4, 1, -4, 8, 8, 8, speckle(YELLOW, "yellow_terracotta", 0.3, 53))
                .box(-4, 1, -4, 8, 2, 8, WHITE)
                .sym(4, 4, -1, 2, 1, 1, LIME).box(-1, 9, -1, 1, 2, 1, LIME).box(1, 9, 1, 1, 2, 1, LIME)
                .box(-0.5, 2, -6, 1, 5, 2, YELLOW)
                .faceSym(1, 5, 2, 2, 4, BLACK).face(-1, 3, 2, 1, 4, ORANGE);
    }
}
