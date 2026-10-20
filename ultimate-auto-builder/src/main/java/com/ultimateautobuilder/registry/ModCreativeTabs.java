package com.ultimateautobuilder.registry;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UltimateAutoBuilderMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ultimateautobuilder"))
            .icon(() -> new ItemStack(ModItems.ULTIMATE_CRYSTAL.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.ULTIMATE_CRYSTAL.get());
                output.accept(ModItems.COMMON_KEY.get());
                output.accept(ModItems.UNCOMMON_KEY.get());
                output.accept(ModItems.RARE_KEY.get());
                output.accept(ModItems.LEGENDARY_KEY.get());
                output.accept(ModItems.OP_KEY.get());
                output.accept(ModItems.TREASURE_MAP.get());
                output.accept(ModItems.LOCKED_CHEST.get());
                output.accept(ModItems.VAULT_DOOR.get());
                output.accept(ModItems.ADVENTURE_SPAWNER.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
