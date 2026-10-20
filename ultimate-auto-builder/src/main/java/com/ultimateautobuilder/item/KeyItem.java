package com.ultimateautobuilder.item;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.adventure.Sanctuary;
import com.ultimateautobuilder.core.adventure.KeyTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Clé d'aventure : ouvre uniquement les serrures de sa rareté et n'est consommée qu'après une
 * ouverture réussie. La clé OP indique en plus la direction du sanctuaire OP.
 */
public class KeyItem extends Item {
    private final KeyTier tier;

    public KeyItem(KeyTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public KeyTier tier() {
        return tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier == KeyTier.LEGENDARY || tier == KeyTier.OP;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (tier == KeyTier.OP && !level.isClientSide() && player instanceof ServerPlayer sp) {
            Sanctuary.showDirection(sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item." + UltimateAutoBuilderMod.MOD_ID + ".key.tooltip",
                Component.translatable("key_tier." + UltimateAutoBuilderMod.MOD_ID + "." + tier.key)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item." + UltimateAutoBuilderMod.MOD_ID + ".key.single_use").withStyle(ChatFormatting.DARK_GRAY));
        if (tier == KeyTier.OP) {
            tooltip.add(Component.translatable("item." + UltimateAutoBuilderMod.MOD_ID + ".op_key.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
