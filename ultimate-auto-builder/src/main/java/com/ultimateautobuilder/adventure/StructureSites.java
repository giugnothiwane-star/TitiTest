package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.adventure.AdventureGen;
import com.ultimateautobuilder.core.adventure.Blueprint;
import com.ultimateautobuilder.core.adventure.StructureType;
import com.ultimateautobuilder.core.placement.Placement;
import com.ultimateautobuilder.core.util.Rng;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Génération des structures d'exploration dans les nouveaux chunks de l'Overworld. Le monde est
 * découpé en grilles (maisons, châteaux) ; chaque case a au plus un emplacement candidat, choisi
 * de façon déterministe à partir de la graine du monde. Un site n'est enregistré que si son chunk
 * candidat vient d'être généré (jamais sur un terrain déjà exploré par les joueurs), puis il est
 * évalué quand tous ses chunks sont chargés : relief, eau, structures vanilla, blocs de joueurs,
 * distance aux autres sites. Un site refusé n'est plus jamais retenté.
 */
public final class StructureSites {
    private static final ConcurrentLinkedQueue<Long> NEW_CHUNKS = new ConcurrentLinkedQueue<>();
    private static final double HOUSE_CHANCE = 0.75, CASTLE_CHANCE = 0.6;
    private static final int MAX_SANCTUARY_MOVES = 24;

    private StructureSites() {
    }

    /** Appelé au chargement d'un chunk (peut provenir d'un autre fil) : simple mise en file. */
    public static void onChunkLoad(Level level, ChunkPos pos, boolean newChunk) {
        if (newChunk && level instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD) {
            NEW_CHUNKS.add(pos.toLong());
        }
    }

    public static void clear() {
        NEW_CHUNKS.clear();
    }

    private static boolean enabled() {
        return !ServerConfig.loaded() || ServerConfig.WORLD_STRUCTURES.get();
    }

    public static void tick(MinecraftServer server) {
        // Serveur de tests : aucune structure aléatoire ne doit perturber les zones de test.
        if (!enabled() || server instanceof net.minecraft.gametest.framework.GameTestServer) {
            NEW_CHUNKS.clear();
            return;
        }
        ServerLevel level = server.overworld();
        AdventureData data = AdventureData.get(server);
        for (int i = 0; i < 128; i++) {
            Long c = NEW_CHUNKS.poll();
            if (c == null) {
                break;
            }
            register(level, data, new ChunkPos(c));
        }
        if (server.getTickCount() % 20 == 0 && !StructurePlacer.busy()) {
            processOne(level, data);
        }
        if (server.getTickCount() % 40 == 0) {
            discover(server, data);
        }
    }

    private static int houseSpacing() {
        return ServerConfig.loaded() ? ServerConfig.HOUSE_SPACING.get() : 14;
    }

    private static int castleSpacing() {
        return ServerConfig.loaded() ? ServerConfig.CASTLE_SPACING.get() : 36;
    }

    /** Candidat d'une case de grille : position déterministe, jamais collée au bord de la case. */
    private static void register(ServerLevel level, AdventureData data, ChunkPos chunk) {
        long seed = level.getSeed();
        tryGrid(data, chunk, seed, "c", castleSpacing(), CASTLE_CHANCE, 0x6C8E9CF570932BD5L);
        tryGrid(data, chunk, seed, "h", houseSpacing(), HOUSE_CHANCE, 0x2545F4914F6CDD1DL);
    }

    private static void tryGrid(AdventureData data, ChunkPos chunk, long worldSeed, String prefix, int spacing, double chance, long salt) {
        int rx = Math.floorDiv(chunk.x, spacing), rz = Math.floorDiv(chunk.z, spacing);
        Rng r = new Rng(worldSeed ^ salt ^ ((long) rx * 341873128712L + (long) rz * 132897987541L));
        int margin = Math.max(1, spacing / 6);
        int cx = rx * spacing + margin + r.nextInt(Math.max(1, spacing - 2 * margin));
        int cz = rz * spacing + margin + r.nextInt(Math.max(1, spacing - 2 * margin));
        if (cx != chunk.x || cz != chunk.z || !r.chance(chance)) {
            return;
        }
        String key = prefix + ":" + rx + ":" + rz;
        if (data.site(key) != null) {
            return;
        }
        AdventureData.Site s = data.addSite(key);
        s.x = chunk.getMiddleBlockX();
        s.z = chunk.getMiddleBlockZ();
        s.seed = r.nextLong();
        s.rotation = r.nextInt(4);
        data.setDirty();
    }

    /** Évalue le premier site en attente dont tous les chunks sont chargés. */
    private static void processOne(ServerLevel level, AdventureData data) {
        for (AdventureData.Site s : data.sites()) {
            if (s.state != AdventureData.SiteState.PENDING) {
                continue;
            }
            int r = s.key.startsWith("h:") ? 2 : 4;
            if (!loaded(level, s.x, s.z, r)) {
                continue;
            }
            evaluate(level, data, s);
            return;
        }
    }

    private static boolean loaded(ServerLevel level, int x, int z, int chunkRadius) {
        int cx = x >> 4, cz = z >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (!level.getChunkSource().hasChunk(cx + dx, cz + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void evaluate(ServerLevel level, AdventureData data, AdventureData.Site s) {
        Rng r = new Rng(s.seed);
        BlockPos center = new BlockPos(s.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.x, s.z), s.z);
        if (s.type == null) {
            boolean forest = level.getBiome(center).is(BiomeTags.IS_FOREST);
            s.type = StructureType.pick(s.key.startsWith("c:") ? StructureType.Group.CASTLE : StructureType.Group.HOUSE, forest, r.nextDouble());
        }
        Blueprint bp = AdventureGen.build(s.type, s.seed);
        int[] origin = validate(level, data, s, bp, s.x, s.z, s.rotation);
        if (origin == null) {
            if (s.type == StructureType.OP_SANCTUARY && s.rotation < MAX_SANCTUARY_MOVES) {
                // Le sanctuaire doit exister : il glisse plus loin et sera réévalué au prochain chargement.
                double a = Math.atan2(s.z - level.getSharedSpawnPos().getZ(), s.x - level.getSharedSpawnPos().getX());
                s.x += (int) (Math.cos(a + s.rotation) * 96);
                s.z += (int) (Math.sin(a + s.rotation) * 96);
                s.rotation++;
            } else {
                s.state = AdventureData.SiteState.SKIPPED;
            }
            data.setDirty();
            return;
        }
        s.y = origin[1];
        s.state = AdventureData.SiteState.PLACING;
        data.setDirty();
        int rot = s.type == StructureType.OP_SANCTUARY ? 0 : s.rotation;
        UltimateAutoBuilderMod.LOGGER.info("Structure d'aventure « {} » en construction en {} {} {}", s.type.label, s.x, s.y, s.z);
        StructurePlacer.enqueue(level, bp, s.x, s.y, s.z, rot, box -> {
            data.markPlaced(s, box);
            UltimateAutoBuilderMod.LOGGER.info("Structure d'aventure « {} » terminée ({})", s.type.label, Arrays.toString(box));
        });
    }

    /**
     * Contrôle du terrain sous l'emprise : renvoie {x, y du plancher, z} ou null si l'endroit ne
     * convient pas (eau, pente forte, océan ou rivière, structure vanilla, blocs de joueur, voisin trop proche).
     */
    static int[] validate(ServerLevel level, AdventureData data, AdventureData.Site self, Blueprint bp, int x, int z, int rotation) {
        int[] fp = bp.footprint();
        int maxDev = bp.type().group == StructureType.Group.CASTLE || bp.type() == StructureType.OP_SANCTUARY ? 6 : 4;
        if (self != null) {
            for (AdventureData.Site o : data.sites()) {
                if (o != self && o.state != AdventureData.SiteState.SKIPPED) {
                    long dx = o.x - x, dz = o.z - z;
                    int need = Math.max(bp.type().clearance(), o.type == null ? 20 : o.type.clearance()) + 24;
                    if (dx * dx + dz * dz < (long) need * need) {
                        return null;
                    }
                }
            }
        }
        BlockPos c0 = new BlockPos(x, 64, z);
        if (level.getBiome(c0).is(BiomeTags.IS_OCEAN) || level.getBiome(c0).is(BiomeTags.IS_RIVER) || level.getBiome(c0).is(BiomeTags.IS_BEACH)) {
            return null;
        }
        int samples = 0, fluids = 0;
        int[] heights = new int[(fp[2] - fp[0] + 1) * (fp[3] - fp[1] + 1)];
        int n = 0;
        for (int dx = fp[0]; dx <= fp[2]; dx += 2) {
            for (int dz = fp[1]; dz <= fp[3]; dz += 2) {
                long p = Placement.rotateXZ(dx, dz, rotation, false);
                int wx = x + (int) (p >> 32), wz = z + (int) p;
                if (!level.isLoaded(new BlockPos(wx, 64, wz))) {
                    return null;
                }
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz) - 1;
                BlockPos top = new BlockPos(wx, h, wz);
                BlockState s = level.getBlockState(top);
                samples++;
                if (!s.getFluidState().isEmpty()) {
                    fluids++;
                }
                // Blocs de joueurs ou de structures (entités de bloc) : on ne construit jamais dessus.
                for (int dy = 0; dy <= 3; dy++) {
                    if (level.getBlockEntity(top.above(dy)) != null) {
                        return null;
                    }
                }
                heights[n++] = h;
            }
        }
        if (samples == 0 || fluids * 20 > samples) {
            return null;
        }
        int[] hs = Arrays.copyOf(heights, n);
        Arrays.sort(hs);
        int floor = hs[n / 2];
        int far = 0;
        for (int h : hs) {
            int dev = Math.abs(h - floor);
            if (dev > maxDev + 4) {
                return null;
            }
            if (dev > maxDev) {
                far++;
            }
        }
        if (far * 10 > n) {
            return null;
        }
        // La partie enterrée (cave, forteresse souterraine) doit rester au-dessus du fond du monde.
        if (floor + bp.minY() < level.getMinBuildHeight() + 2 || floor + bp.maxY() >= level.getMaxBuildHeight() - 2) {
            return null;
        }
        for (int[] corner : new int[][]{{fp[0], fp[1]}, {fp[2], fp[1]}, {fp[0], fp[3]}, {fp[2], fp[3]}, {0, 0}}) {
            long p = Placement.rotateXZ(corner[0], corner[1], rotation, false);
            if (level.structureManager().hasAnyStructureAt(new BlockPos(x + (int) (p >> 32), floor, z + (int) p))) {
                return null;
            }
        }
        return new int[]{x, floor, z};
    }

    /** Découvertes : entrée d'un joueur dans une structure posée (contrôle toutes les 2 secondes). */
    private static void discover(MinecraftServer server, AdventureData data) {
        for (ServerPlayer p : server.overworld().players()) {
            for (AdventureData.Site s : data.placedNear(p.getBlockX(), p.getBlockZ())) {
                if (s.type == null || !s.contains(p.getX(), p.getY(), p.getZ())) {
                    continue;
                }
                AdventureData.PlayerState ps = data.player(p.getUUID());
                if (ps.discovered.add(s.key)) {
                    if (s.type == StructureType.OP_SANCTUARY) {
                        ps.knowsSanctuary = true;
                    }
                    data.setDirty();
                    p.displayClientMessage(Component.translatable("message." + UltimateAutoBuilderMod.MOD_ID + ".discovered",
                            Component.translatable("structure." + UltimateAutoBuilderMod.MOD_ID + "." + s.type.key)).withStyle(ChatFormatting.AQUA), true);
                    p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 1.2F);
                }
            }
        }
    }
}
