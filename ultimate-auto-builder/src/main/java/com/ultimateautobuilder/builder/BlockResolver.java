package com.ultimateautobuilder.builder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ultimateautobuilder.UltimateAutoBuilderMod;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversion des états textuels du catalogue en {@link BlockState}, avec cache et repli :
 * un bloc absent de l'installation (modèle importé d'un autre mod) est remplacé par un bloc vanilla.
 */
public final class BlockResolver {
    public record Resolved(BlockState state, boolean fallback) {
    }

    private static final ConcurrentHashMap<String, Resolved> CACHE = new ConcurrentHashMap<>();

    private BlockResolver() {
    }

    public static Resolved resolve(String text) {
        return CACHE.computeIfAbsent(text, BlockResolver::parse);
    }

    public static BlockState state(String text) {
        return resolve(text).state();
    }

    private static Resolved parse(String text) {
        try {
            return new Resolved(BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), text, false).blockState(), false);
        } catch (CommandSyntaxException e) {
            // Propriétés inconnues : on tente le bloc seul avec son état par défaut.
            int bracket = text.indexOf('[');
            String id = bracket >= 0 ? text.substring(0, bracket) : text;
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) {
                Block block = BuiltInRegistries.BLOCK.get(rl);
                return new Resolved(block.defaultBlockState(), true);
            }
            BlockState fb = fallbackFor(id);
            UltimateAutoBuilderMod.LOGGER.debug("Bloc inconnu « {} », remplacé par {}", text, fb);
            return new Resolved(fb, true);
        }
    }

    /** Bloc vanilla de remplacement choisi d'après le nom du bloc manquant. */
    static BlockState fallbackFor(String id) {
        String p = id.toLowerCase(java.util.Locale.ROOT);
        if (p.contains("glass")) {
            return Blocks.GLASS.defaultBlockState();
        }
        if (p.contains("leaves") || p.contains("leaf")) {
            return Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        }
        if (p.contains("log") || p.contains("wood") || p.contains("plank")) {
            return Blocks.OAK_PLANKS.defaultBlockState();
        }
        if (p.contains("wool") || p.contains("carpet")) {
            return Blocks.WHITE_WOOL.defaultBlockState();
        }
        if (p.contains("brick")) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (p.contains("ore") || p.contains("metal") || p.contains("steel") || p.contains("iron")) {
            return Blocks.IRON_BLOCK.defaultBlockState();
        }
        if (p.contains("dirt") || p.contains("soil")) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (p.contains("lamp") || p.contains("light")) {
            return Blocks.GLOWSTONE.defaultBlockState();
        }
        if (p.endsWith(":air") || p.contains("void")) {
            return Blocks.AIR.defaultBlockState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
