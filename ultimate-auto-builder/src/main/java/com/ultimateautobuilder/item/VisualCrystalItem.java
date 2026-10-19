package com.ultimateautobuilder.item;

import com.ultimateautobuilder.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
 * Cristal de Création Visuelle : ouvre le studio graphique (clic droit, touche dédiée ou R1 quand il est
 * tenu). Purement client : il ne pose aucun bloc et n'envoie rien au serveur. Jamais consommé.
 */
public class VisualCrystalItem extends Item {
    public VisualCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            ClientHooks.onVisualCrystalUsed(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() && selected && level.getGameTime() % 8 == 0 && ClientHooks.crystalParticlesEnabled()) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.WITCH, entity.getX() + Math.cos(a) * 0.6, entity.getY() + 1.1 + level.getRandom().nextDouble() * 0.4,
                    entity.getZ() + Math.sin(a) * 0.6, 0, 0.01, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getClass() != UltimateCrystalItem.class) {
            tooltip.add(Component.translatable("item.ultimateautobuilder.legacy_crystal.tooltip").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.ultimateautobuilder.visual_crystal.tooltip1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.ultimateautobuilder.visual_crystal.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
