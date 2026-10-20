package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.adventure.SpawnCategory;
import com.ultimateautobuilder.core.adventure.SpawnRules;
import com.ultimateautobuilder.core.adventure.WardenVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Variantes réelles des créatures de spawner. Les bébés natifs (zombies) utilisent la mécanique
 * de Minecraft ; les autres tailles passent par l'attribut d'échelle de la 1.21 (rendu et boîte
 * de collision réduits ensemble), avec santé, dégâts et vitesse ajustés. La variante est
 * enregistrée dans les données de l'entité et conservée à la sauvegarde.
 */
public final class MobVariants {
    public static final String TAG_VARIANT = UltimateAutoBuilderMod.MOD_ID + ":variant";
    public static final String TAG_WARDEN = UltimateAutoBuilderMod.MOD_ID + ":warden";
    private static final ResourceLocation MOD_SCALE = UltimateAutoBuilderMod.id("variant_scale");
    private static final ResourceLocation MOD_HEALTH = UltimateAutoBuilderMod.id("variant_health");
    private static final ResourceLocation MOD_DAMAGE = UltimateAutoBuilderMod.id("variant_damage");
    private static final ResourceLocation MOD_SPEED = UltimateAutoBuilderMod.id("variant_speed");

    private MobVariants() {
    }

    public static SpawnRules.Dice dice(RandomSource random) {
        return new SpawnRules.Dice() {
            @Override
            public double nextDouble() {
                return random.nextDouble();
            }

            @Override
            public int nextInt(int bound) {
                return random.nextInt(bound);
            }
        };
    }

    /** Une créature n'est éligible que si elle existe et est un Mob (attributs d'échelle disponibles). */
    public static boolean supports(String entityId) {
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        return rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl);
    }

    /** Crée et place la créature choisie ; null si aucune position sûre n'a été trouvée. */
    public static Entity spawn(ServerLevel level, BlockPos spawner, SpawnCategory category, SpawnRules.Choice choice) {
        String id = choice instanceof SpawnRules.Warden ? "minecraft:warden" : ((SpawnRules.Ordinary) choice).entityId();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }
        // La taille est appliquée avant la recherche de place : la boîte de collision est la bonne.
        if (choice instanceof SpawnRules.Warden w) {
            applyWarden(mob, w.variant());
        } else {
            applyCategory(mob, id, category);
        }
        if (!place(level, spawner, mob)) {
            mob.discard();
            return null;
        }
        boolean warden = choice instanceof SpawnRules.Warden;
        BlockPos at = mob.blockPosition();
        if (mob instanceof Zombie) {
            // Groupe imposé : aucune décision aléatoire de bébé ni de poulet chevauché.
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER,
                    new Zombie.ZombieGroupData(false, false));
            ((Zombie) mob).setBaby(category == SpawnCategory.BABY);
        } else {
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(at), warden ? MobSpawnType.TRIGGERED : MobSpawnType.SPAWNER, null);
        }
        // Aucun passager ni monture créés en plus (araignée chevauchée) : quatre entités par cycle, pas davantage.
        mob.ejectPassengers();
        mob.stopRiding();
        mob.setHealth(mob.getMaxHealth());
        if (warden) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(mob);
        if (choice instanceof SpawnRules.Warden w) {
            WardenEvents.onSpawned(level, (Warden) mob, w.variant());
        }
        level.levelEvent(2004, spawner, 0);
        return mob;
    }

    static void applyCategory(Mob mob, String id, SpawnCategory c) {
        mob.getPersistentData().putString(TAG_VARIANT, c.key);
        if (c == SpawnCategory.NORMAL || (c == SpawnCategory.BABY && SpawnRules.hasNativeBaby(id))) {
            return;
        }
        modify(mob, Attributes.SCALE, MOD_SCALE, c.scale - 1.0);
        modify(mob, Attributes.MAX_HEALTH, MOD_HEALTH, c.health - 1.0);
        modify(mob, Attributes.ATTACK_DAMAGE, MOD_DAMAGE, c.damage - 1.0);
        modify(mob, Attributes.MOVEMENT_SPEED, MOD_SPEED, c.speed - 1.0);
        mob.refreshDimensions();
    }

    static void applyWarden(Mob mob, WardenVariant v) {
        mob.getPersistentData().putString(TAG_WARDEN, v.key);
        mob.getPersistentData().putString(TAG_VARIANT, v.category.key);
        if (v != WardenVariant.NORMAL) {
            modify(mob, Attributes.SCALE, MOD_SCALE, v.scale - 1.0);
            modify(mob, Attributes.MAX_HEALTH, MOD_HEALTH, v.health - 1.0);
            modify(mob, Attributes.ATTACK_DAMAGE, MOD_DAMAGE, v.damage - 1.0);
        }
        if (v == WardenVariant.BOSS) {
            mob.setCustomName(Component.literal(v.displayName).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            mob.setCustomNameVisible(true);
        } else if (v != WardenVariant.NORMAL) {
            mob.setCustomName(Component.literal(v.displayName).withStyle(ChatFormatting.DARK_AQUA));
        }
        mob.refreshDimensions();
    }

    private static void modify(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null || amount == 0.0) {
            return;
        }
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /**
     * Cherche une position libre autour du spawner : sol porteur, aucune collision, aucun liquide,
     * jamais sur un joueur (au moins 3 blocs).
     */
    private static boolean place(ServerLevel level, BlockPos spawner, Mob mob) {
        RandomSource r = level.random;
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = spawner.getX() + r.nextInt(9) - 4;
            int z = spawner.getZ() + r.nextInt(9) - 4;
            int y = spawner.getY() + r.nextInt(3) - 1;
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos below = feet.below();
            BlockState ground = level.getBlockState(below);
            if (!ground.isFaceSturdy(level, below, Direction.UP) || !level.isLoaded(feet)) {
                continue;
            }
            mob.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360F, 0F);
            Player near = level.getNearestPlayer(mob, 3.0);
            if (near == null && level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    /** Variante Warden d'une entité, ou null. */
    public static WardenVariant wardenVariant(Entity e) {
        return e instanceof Warden ? WardenVariant.byKey(e.getPersistentData().getString(TAG_WARDEN)) : null;
    }

    static void notify(ServerLevel level, BlockPos pos, Component msg, double radius) {
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(pos) < radius * radius) {
                p.displayClientMessage(msg, true);
            }
        }
    }
}
