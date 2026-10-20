package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.adventure.WardenVariant;
import com.ultimateautobuilder.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wardens générés par les spawners du mod : barre de boss du Boss Warden, maintien de la
 * rencontre (pas de retour sous terre), et récompense uniquement après une vraie victoire
 * d'un joueur (coup fatal porté par le joueur ou son projectile), une seule fois par Warden.
 */
public final class WardenEvents {
    private static final Map<UUID, Tracked> ACTIVE = new HashMap<>();
    private static final double BAR_RANGE = 48;

    private record Tracked(Warden warden, ServerBossEvent bar) {
    }

    private WardenEvents() {
    }

    static void onSpawned(ServerLevel level, Warden w, WardenVariant v) {
        String ns = UltimateAutoBuilderMod.MOD_ID;
        MobVariants.notify(level, w.blockPosition(), Component.translatable(v == WardenVariant.BOSS ? "message." + ns + ".boss_warden_awakes"
                : "message." + ns + ".warden_awakes", v.displayName).withStyle(ChatFormatting.DARK_AQUA), 40);
        track(w);
    }

    /** Enregistrement à l'apparition et au rechargement du chunk (événement d'entrée dans le monde). */
    public static void track(Entity e) {
        if (!(e instanceof Warden w) || MobVariants.wardenVariant(w) == null || ACTIVE.containsKey(w.getUUID())) {
            return;
        }
        WardenVariant v = MobVariants.wardenVariant(w);
        ServerBossEvent bar = null;
        if (v == WardenVariant.BOSS) {
            bar = new ServerBossEvent(Component.literal(v.displayName).withStyle(ChatFormatting.DARK_PURPLE), BossEvent.BossBarColor.PURPLE,
                    BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setDarkenScreen(true);
        }
        ACTIVE.put(w.getUUID(), new Tracked(w, bar));
    }

    public static void untrack(Entity e) {
        Tracked t = ACTIVE.remove(e.getUUID());
        if (t != null && t.bar() != null) {
            t.bar().removeAllPlayers();
        }
    }

    /** Toutes les 10 ticks : quelques Wardens au plus, coût négligeable. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0 || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Tracked> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            Tracked t = it.next();
            Warden w = t.warden();
            if (w.isRemoved() || !w.isAlive()) {
                if (t.bar() != null) {
                    t.bar().removeAllPlayers();
                }
                it.remove();
                continue;
            }
            // Rencontre maintenue : le Warden ne s'enfouit pas tant qu'il est suivi.
            w.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
            ServerBossEvent bar = t.bar();
            if (bar != null) {
                bar.setProgress(Math.max(0F, Math.min(1F, w.getHealth() / w.getMaxHealth())));
                List<ServerPlayer> inRange = new ArrayList<>();
                for (ServerPlayer p : ((ServerLevel) w.level()).players()) {
                    if (p.distanceToSqr(w) < BAR_RANGE * BAR_RANGE) {
                        inRange.add(p);
                    }
                }
                for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
                    if (!inRange.contains(p)) {
                        bar.removePlayer(p);
                    }
                }
                for (ServerPlayer p : inRange) {
                    bar.addPlayer(p);
                }
            }
        }
    }

    /** Joueur auteur du coup fatal (direct ou projectile), sinon null : aucune récompense. */
    static ServerPlayer victor(DamageSource source) {
        Entity e = source.getEntity();
        if (e instanceof ServerPlayer p) {
            return p;
        }
        if (source.getDirectEntity() instanceof Projectile pr && pr.getOwner() instanceof ServerPlayer p) {
            return p;
        }
        return null;
    }

    public static void onDeath(Entity entity, DamageSource source) {
        WardenVariant v = MobVariants.wardenVariant(entity);
        if (v == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        untrack(entity);
        ServerPlayer player = victor(source);
        String ns = UltimateAutoBuilderMod.MOD_ID;
        if (player == null) {
            UltimateAutoBuilderMod.LOGGER.info("Warden {} mort sans victoire d'un joueur : aucune récompense", entity.getUUID());
            return;
        }
        AdventureData data = AdventureData.get(level.getServer());
        // Protection contre les événements de mort répétés : une seule récompense par Warden.
        if (!data.markRewarded(entity.getUUID())) {
            return;
        }
        AdventureData.PlayerState ps = data.player(player.getUUID());
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(ModItems.OP_KEY.get()));
        if (v == WardenVariant.BOSS) {
            ps.bosses++;
            loot.addAll(Rewards.roll(player, "entities/boss_warden"));
        } else {
            ps.wardens++;
        }
        ps.knowsSanctuary = true;
        data.setDirty();
        level.playSound(null, entity.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        Rewards.give(player, loot, Component.translatable(v == WardenVariant.BOSS ? "message." + ns + ".boss_defeated" : "message." + ns
                + ".warden_defeated", v.displayName));
        player.displayClientMessage(Component.translatable("message." + ns + ".op_key_obtained").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        if (v == WardenVariant.BOSS) {
            MobVariants.notify(level, entity.blockPosition(), Component.translatable("message." + ns + ".boss_defeated_by",
                    player.getDisplayName()).withStyle(ChatFormatting.GOLD), 96);
        }
    }

    public static void clear() {
        for (Tracked t : ACTIVE.values()) {
            if (t.bar() != null) {
                t.bar().removeAllPlayers();
            }
        }
        ACTIVE.clear();
    }
}
