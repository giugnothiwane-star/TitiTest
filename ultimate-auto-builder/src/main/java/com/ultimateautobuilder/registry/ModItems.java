package com.ultimateautobuilder.registry;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.adventure.KeyTier;
import com.ultimateautobuilder.item.IceCrystalItem;
import com.ultimateautobuilder.item.KeyItem;
import com.ultimateautobuilder.item.TreasureMapItem;
import com.ultimateautobuilder.item.UltimateCrystalItem;
import com.ultimateautobuilder.item.VisualCrystalItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UltimateAutoBuilderMod.MOD_ID);

    /** Cristal unique du mod : ultimateautobuilder:ultimate_crystal. */
    public static final DeferredItem<UltimateCrystalItem> ULTIMATE_CRYSTAL = ITEMS.register("ultimate_crystal",
            () -> new UltimateCrystalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /**
     * Anciens cristaux : conservés pour les mondes existants (sinon les objets disparaîtraient des
     * inventaires), retirés de l'onglet créatif ; convertibles en Ultimate Crystal à l'établi.
     */
    public static final DeferredItem<IceCrystalItem> ICE_CRYSTAL = ITEMS.register("ice_crystal",
            () -> new IceCrystalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Identifiant stable : ultimateautobuilder:visual_crystal. */
    public static final DeferredItem<VisualCrystalItem> VISUAL_CRYSTAL = ITEMS.register("visual_crystal",
            () -> new VisualCrystalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // Clés : une par rareté, à usage unique, empilables par 16.
    public static final DeferredItem<KeyItem> COMMON_KEY = key("common_key", KeyTier.COMMON, Rarity.COMMON);
    public static final DeferredItem<KeyItem> UNCOMMON_KEY = key("uncommon_key", KeyTier.UNCOMMON, Rarity.UNCOMMON);
    public static final DeferredItem<KeyItem> RARE_KEY = key("rare_key", KeyTier.RARE, Rarity.RARE);
    public static final DeferredItem<KeyItem> LEGENDARY_KEY = key("legendary_key", KeyTier.LEGENDARY, Rarity.EPIC);
    public static final DeferredItem<KeyItem> OP_KEY = key("op_key", KeyTier.OP, Rarity.EPIC);

    /** Carte au trésor : coordonnées fixées une seule fois, à la première prise en main. */
    public static final DeferredItem<TreasureMapItem> TREASURE_MAP = ITEMS.register("treasure_map",
            () -> new TreasureMapItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<BlockItem> LOCKED_CHEST = ITEMS.registerSimpleBlockItem("locked_chest", ModBlocks.LOCKED_CHEST);
    public static final DeferredItem<BlockItem> VAULT_DOOR = ITEMS.registerSimpleBlockItem("vault_door", ModBlocks.VAULT_DOOR);
    public static final DeferredItem<BlockItem> ADVENTURE_SPAWNER = ITEMS.registerSimpleBlockItem("adventure_spawner", ModBlocks.ADVENTURE_SPAWNER);

    private static DeferredItem<KeyItem> key(String name, KeyTier tier, Rarity rarity) {
        return ITEMS.register(name, () -> new KeyItem(tier, new Item.Properties().stacksTo(16).rarity(rarity)
                .fireResistant()));
    }

    public static KeyItem keyFor(KeyTier tier) {
        return switch (tier) {
            case COMMON -> COMMON_KEY.get();
            case UNCOMMON -> UNCOMMON_KEY.get();
            case RARE -> RARE_KEY.get();
            case LEGENDARY -> LEGENDARY_KEY.get();
            case OP -> OP_KEY.get();
        };
    }

    private ModItems() {
    }
}
