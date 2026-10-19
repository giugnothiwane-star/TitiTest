package com.ultimateautobuilder.item;

import com.ultimateautobuilder.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ultimate Crystal — Creative Architect : cristal unique du mod. Clic droit (ou touche B, R1) :
 * hub central ; accroupi + clic droit sur un bloc : coins de zone (hérité du Cristal de Glace).
 * Jamais consommé.
 */
public class UltimateCrystalItem extends IceCrystalItem {
    public UltimateCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            ClientHooks.onCrystalUsed(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Pétales de cerisier discrets quand le cristal est en main (client uniquement).
        if (level.isClientSide() && selected && level.getGameTime() % 7 == 0 && ClientHooks.crystalParticlesEnabled()) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            // À hauteur de taille et à distance : au niveau des yeux, les pétales masqueraient la vue à la première personne.
            level.addParticle(ParticleTypes.CHERRY_LEAVES, entity.getX() + Math.cos(a) * 1.1, entity.getY() + 0.3 + level.getRandom().nextDouble() * 0.5,
                    entity.getZ() + Math.sin(a) * 1.1, 0, 0, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ultimateautobuilder.ultimate_crystal.tooltip1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.ultimateautobuilder.ultimate_crystal.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ultimateautobuilder.ultimate_crystal.tooltip3").withStyle(ChatFormatting.GRAY));
    }
}
