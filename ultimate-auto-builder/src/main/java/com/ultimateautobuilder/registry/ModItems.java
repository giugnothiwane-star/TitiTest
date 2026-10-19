package com.ultimateautobuilder.registry;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.item.IceCrystalItem;
import com.ultimateautobuilder.item.VisualCrystalItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UltimateAutoBuilderMod.MOD_ID);

    /** Identifiant stable : ultimateautobuilder:ice_crystal. */
    public static final DeferredItem<IceCrystalItem> ICE_CRYSTAL = ITEMS.register("ice_crystal",
            () -> new IceCrystalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Identifiant stable : ultimateautobuilder:visual_crystal. */
    public static final DeferredItem<VisualCrystalItem> VISUAL_CRYSTAL = ITEMS.register("visual_crystal",
            () -> new VisualCrystalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    private ModItems() {
    }
}
