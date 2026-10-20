package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.core.adventure.StructureType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Câblage des systèmes d'aventure sur le bus d'événements (côté serveur). */
public final class AdventureEvents {
    private AdventureEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> Sanctuary.ensureSite(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            StructurePlacer.clear();
            StructureSites.clear();
            WardenEvents.clear();
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            StructureSites.tick(e.getServer());
            StructurePlacer.tick(e.getServer());
            StarterKit.tick(e.getServer());
            Treasures.tick(e.getServer());
            WardenEvents.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
            if (!e.getLevel().isClientSide()) {
                StructureSites.onChunkLoad((net.minecraft.world.level.Level) e.getLevel(), e.getChunk().getPos(), e.isNewChunk());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                StarterKit.onLogin(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide()) {
                WardenEvents.track(e.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            if (!e.getLevel().isClientSide()) {
                WardenEvents.untrack(e.getEntity());
            }
        });
        // Priorité la plus basse et événements annulés ignorés : seule une mort réelle compte.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            if (!e.getEntity().level().isClientSide()) {
                WardenEvents.onDeath(e.getEntity(), e.getSource());
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingEquipmentChangeEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp && e.getSlot().isArmor()) {
                ArmorRewards.onEquipmentChange(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerContainerEvent.Open e) -> {
            if (e.getEntity() instanceof ServerPlayer sp && e.getContainer() instanceof ChestMenu menu
                    && menu.getContainer() instanceof BlockEntity be) {
                Treasures.onContainerOpen(sp, be.getBlockPos());
            }
        });
    }

    /** Nom affichable d'un type de structure (utilisé par le hub). */
    public static String label(StructureType t) {
        return t == null ? "?" : t.label;
    }
}
