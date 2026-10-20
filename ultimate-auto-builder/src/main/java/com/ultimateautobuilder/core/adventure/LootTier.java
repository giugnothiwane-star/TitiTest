package com.ultimateautobuilder.core.adventure;

/** Niveaux de butin des coffres ordinaires des structures et des trésors. */
public enum LootTier {
    COMMON("common"), UNCOMMON("uncommon"), RARE("rare"), LEGENDARY("legendary"), OP("op");

    public final String key;

    LootTier(String key) {
        this.key = key;
    }

    public String chestTable() {
        return "chests/" + key;
    }

    public String treasureTable() {
        return "chests/treasure_" + key;
    }

    public static LootTier byKey(String key) {
        for (LootTier t : values()) {
            if (t.key.equals(key)) {
                return t;
            }
        }
        return null;
    }
}
