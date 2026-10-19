package com.ultimateautobuilder.item;

import com.ultimateautobuilder.client.ClientHooks;
import com.ultimateautobuilder.selection.Selections;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Cristal de Glace : ouvre le menu Ultimate Auto Builder (clic droit, touche dédiée ou R1 de la
 * manette lorsqu'il est tenu). Accroupi + clic droit sur un bloc : définit les coins de la zone
 * sélectionnée. L'objet n'est ni consommé ni usé.
 */
public class IceCrystalItem extends Item {
    public IceCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            // Code client isolé dans ClientHooks : jamais chargé sur un serveur dédié.
            ClientHooks.onCrystalUsed(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide() && player instanceof ServerPlayer sp) {
            BlockPos pos = context.getClickedPos();
            int corner = Selections.toggleCorner(sp, pos);
            sp.displayClientMessage(Component.translatable("message.ultimateautobuilder.corner", corner, pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Quelques flocons discrets quand le cristal est en main (côté client uniquement, coût négligeable).
        if (level.isClientSide() && selected && level.getGameTime() % 6 == 0 && ClientHooks.crystalParticlesEnabled()) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.SNOWFLAKE, entity.getX() + Math.cos(a) * 0.6, entity.getY() + 1.1 + level.getRandom().nextDouble() * 0.4,
                    entity.getZ() + Math.sin(a) * 0.6, 0, 0.01, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ultimateautobuilder.ice_crystal.tooltip1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.ultimateautobuilder.ice_crystal.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ultimateautobuilder.ice_crystal.tooltip3").withStyle(ChatFormatting.GRAY));
    }
}
