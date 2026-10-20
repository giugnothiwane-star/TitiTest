package com.ultimateautobuilder.item;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.adventure.Treasures;
import com.ultimateautobuilder.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Carte au trésor : à la première prise en main, le serveur fixe définitivement la position du
 * trésor (enregistrée dans la carte et dans les données du monde). Clic droit : parchemin avec la
 * croix rouge, la position du joueur, la distance et la direction.
 */
public class TreasureMapItem extends Item {
    public TreasureMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer sp && level.getGameTime() % 20 == 0 && !Treasures.hasTarget(stack)) {
            Treasures.assign(stack, sp);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            ClientHooks.openTreasureMap(stack);
        } else if (player instanceof ServerPlayer sp && !Treasures.hasTarget(stack)) {
            Treasures.assign(stack, sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Treasures.Target t = Treasures.target(stack);
        String ns = UltimateAutoBuilderMod.MOD_ID;
        tooltip.add(Component.translatable("item." + ns + ".treasure_map.tier",
                Component.translatable("loot_tier." + ns + "." + Treasures.tier(stack).key)).withStyle(ChatFormatting.GOLD));
        if (t == null) {
            tooltip.add(Component.translatable("item." + ns + ".treasure_map.unset").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item." + ns + ".treasure_map.target", t.x(), t.z()).withStyle(ChatFormatting.GRAY));
        }
    }
}
