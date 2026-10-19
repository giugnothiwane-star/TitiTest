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
            .icon(() -> new ItemStack(ModItems.ICE_CRYSTAL.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.ICE_CRYSTAL.get());
                output.accept(ModItems.VISUAL_CRYSTAL.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
