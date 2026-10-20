package com.ultimateautobuilder.block;

import com.ultimateautobuilder.core.adventure.KeyTier;
import com.ultimateautobuilder.core.adventure.SpawnCategory;
import com.ultimateautobuilder.core.adventure.SpawnFamily;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Propriétés d'état des blocs d'aventure (noms identiques à ceux des plans du cœur). */
public final class Props {
    public enum Tier implements StringRepresentable {
        COMMON(KeyTier.COMMON), UNCOMMON(KeyTier.UNCOMMON), RARE(KeyTier.RARE), LEGENDARY(KeyTier.LEGENDARY), OP(KeyTier.OP);

        public final KeyTier tier;

        Tier(KeyTier tier) {
            this.tier = tier;
        }

        @Override
        public String getSerializedName() {
            return tier.key;
        }
    }

    public enum Category implements StringRepresentable {
        BABY(SpawnCategory.BABY), MEDIUM(SpawnCategory.MEDIUM), NORMAL(SpawnCategory.NORMAL);

        public final SpawnCategory category;

        Category(SpawnCategory category) {
            this.category = category;
        }

        @Override
        public String getSerializedName() {
            return category.key;
        }
    }

    public enum Family implements StringRepresentable {
        UNDEAD(SpawnFamily.UNDEAD), ARACHNID(SpawnFamily.ARACHNID), ILLAGER(SpawnFamily.ILLAGER), MIXED(SpawnFamily.MIXED);

        public final SpawnFamily family;

        Family(SpawnFamily family) {
            this.family = family;
        }

        @Override
        public String getSerializedName() {
            return family.key;
        }
    }

    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);
    public static final EnumProperty<Category> CATEGORY = EnumProperty.create("category", Category.class);
    public static final EnumProperty<Family> FAMILY = EnumProperty.create("family", Family.class);
    public static final BooleanProperty WARDEN = BooleanProperty.create("warden");
    public static final BooleanProperty DEPLETED = BooleanProperty.create("depleted");

    private Props() {
    }
}
