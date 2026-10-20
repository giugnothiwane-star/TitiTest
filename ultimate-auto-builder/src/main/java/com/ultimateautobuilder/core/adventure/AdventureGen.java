package com.ultimateautobuilder.core.adventure;

/** Point d'entrée de génération des structures d'aventure (déterministe pour une graine donnée). */
public final class AdventureGen {
    private AdventureGen() {
    }

    public static Blueprint build(StructureType type, long seed) {
        return switch (type.group) {
            case HOUSE -> HouseGen.build(type, seed);
            case CASTLE -> CastleGen.build(type, seed);
            case UNIQUE -> type == StructureType.OP_SANCTUARY ? SanctuaryGen.build(seed) : HouseGen.build(type, seed);
        };
    }

    /** Structures entièrement souterraines (hormis l'entrée) : le terrain au-dessus n'est pas aplani. */
    public static boolean underground(StructureType type) {
        return type == StructureType.UNDERGROUND_FORTRESS;
    }
}
