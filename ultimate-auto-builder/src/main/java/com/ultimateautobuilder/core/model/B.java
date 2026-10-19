package com.ultimateautobuilder.core.model;

/**
 * Fabrique d'états de blocs vanilla 1.21.1 sous forme de chaînes « id[propriétés] ».
 * Convention de conception : la façade des structures regarde vers +Z (sud).
 */
public final class B {
    public static final String AIR = "minecraft:air";

    // Directions horizontales
    public static final String N = "north";
    public static final String S = "south";
    public static final String E = "east";
    public static final String W = "west";

    private B() {
    }

    public static String mc(String id) {
        return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
    }

    public static boolean isAir(String state) {
        return state != null && (state.equals(AIR) || state.equals("minecraft:cave_air") || state.equals("minecraft:void_air"));
    }

    /** Identifiant sans propriétés. */
    public static String idOf(String state) {
        int i = state.indexOf('[');
        return i < 0 ? state : state.substring(0, i);
    }

    public static String opposite(String dir) {
        return switch (dir) {
            case N -> S;
            case S -> N;
            case E -> W;
            case W -> E;
            default -> dir;
        };
    }

    /** Escalier : la partie haute (dossier) est du côté {@code facing}. */
    public static String stairs(String material, String facing, boolean top) {
        return mc(material + "_stairs") + "[facing=" + facing + ",half=" + (top ? "top" : "bottom") + "]";
    }

    public static String slab(String material, boolean top) {
        return mc(material + "_slab") + "[type=" + (top ? "top" : "bottom") + "]";
    }

    public static String doubleSlab(String material) {
        return mc(material + "_slab") + "[type=double]";
    }

    public static String doorLower(String wood, String facing) {
        return mc(wood + "_door") + "[facing=" + facing + ",half=lower,hinge=left,open=false]";
    }

    public static String doorUpper(String wood, String facing) {
        return mc(wood + "_door") + "[facing=" + facing + ",half=upper,hinge=left,open=false]";
    }

    public static String trapdoor(String wood, String facing, boolean top, boolean open) {
        return mc(wood + "_trapdoor") + "[facing=" + facing + ",half=" + (top ? "top" : "bottom") + ",open=" + open + "]";
    }

    public static String log(String wood, char axis) {
        return mc(wood + "_log") + "[axis=" + axis + "]";
    }

    public static String axis(String id, char axis) {
        return mc(id) + "[axis=" + axis + "]";
    }

    public static String leaves(String wood) {
        return mc(wood + "_leaves") + "[persistent=true]";
    }

    public static String facing(String id, String facing) {
        return mc(id) + "[facing=" + facing + "]";
    }

    public static String lantern(boolean hanging) {
        return "minecraft:lantern[hanging=" + hanging + "]";
    }

    public static String wallTorch(String facing) {
        return "minecraft:wall_torch[facing=" + facing + "]";
    }

    public static String bedFoot(String color, String facing) {
        return mc(color + "_bed") + "[facing=" + facing + ",part=foot]";
    }

    public static String bedHead(String color, String facing) {
        return mc(color + "_bed") + "[facing=" + facing + ",part=head]";
    }

    public static String wool(String color) {
        return mc(color + "_wool");
    }

    public static String concrete(String color) {
        return mc(color + "_concrete");
    }

    public static String terracotta(String color) {
        return mc(color + "_terracotta");
    }

    public static String glass(String color) {
        return mc(color + "_stained_glass");
    }

    public static String pane(String color) {
        return color == null ? "minecraft:glass_pane" : mc(color + "_stained_glass_pane");
    }

    public static String carpet(String color) {
        return mc(color + "_carpet");
    }

    /** Escalier de bois, de pierre… : renvoie le préfixe « matériau » utilisable avec {@link #stairs}. */
    public static String planks(String wood) {
        return mc(wood + "_planks");
    }

    /** Blocs dont la forme dépend des voisins : recalculée après la pose complète. */
    public static boolean needsShapeUpdate(String state) {
        String id = idOf(state);
        return id.endsWith("_stairs") || id.endsWith("_fence") || id.endsWith("_pane") || id.endsWith("_wall")
                || id.equals("minecraft:iron_bars") || id.equals("minecraft:glass_pane") || id.endsWith("_fence_gate")
                || id.equals("minecraft:chain") || id.endsWith("redstone_wire");
    }

    /**
     * Blocs qui doivent être posés après les blocs porteurs (ils s'accrochent à un support
     * ou occupent deux cellules) afin de ne pas se détacher pendant la construction.
     */
    public static boolean isAttachment(String state) {
        String id = idOf(state);
        return id.contains("torch") || id.contains("lantern") || id.endsWith("_door") || id.endsWith("_carpet")
                || id.endsWith("_bed") || id.contains("button") || id.contains("pressure_plate") || id.equals("minecraft:ladder")
                || id.equals("minecraft:vine") || id.contains("flower") || id.contains("tulip") || id.endsWith("_sapling")
                || id.equals("minecraft:short_grass") || id.equals("minecraft:tall_grass") || id.equals("minecraft:fern")
                || id.equals("minecraft:large_fern") || id.contains("potted_") || id.equals("minecraft:flower_pot")
                || id.endsWith("_sign") || id.endsWith("_banner") || id.equals("minecraft:lever") || id.contains("rail")
                || id.equals("minecraft:poppy") || id.equals("minecraft:dandelion") || id.equals("minecraft:cornflower")
                || id.equals("minecraft:lily_of_the_valley") || id.equals("minecraft:azure_bluet") || id.equals("minecraft:allium")
                || id.equals("minecraft:oxeye_daisy") || id.equals("minecraft:blue_orchid") || id.equals("minecraft:snow")
                || id.equals("minecraft:sea_pickle") || id.equals("minecraft:lily_pad") || id.contains("candle")
                || id.equals("minecraft:sweet_berry_bush") || id.equals("minecraft:wheat") || id.equals("minecraft:carrots")
                || id.equals("minecraft:potatoes") || id.equals("minecraft:beetroots") || id.equals("minecraft:sugar_cane")
                || id.equals("minecraft:bamboo") || id.equals("minecraft:glow_lichen") || id.equals("minecraft:end_rod")
                || id.equals("minecraft:cactus") || id.equals("minecraft:red_mushroom") || id.equals("minecraft:brown_mushroom")
                || id.equals("minecraft:dead_bush") || id.equals("minecraft:peony") || id.equals("minecraft:rose_bush")
                || id.equals("minecraft:lilac") || id.equals("minecraft:sunflower") || id.equals("minecraft:pink_petals")
                || id.equals("minecraft:hanging_roots") || id.endsWith("_button") || id.equals("minecraft:cobweb");
    }

    /** Fluides : leur pose peut entraîner un écoulement, le moteur les traite à part. */
    public static boolean isFluid(String state) {
        String id = idOf(state);
        return id.equals("minecraft:water") || id.equals("minecraft:lava");
    }
}
