package com.ultimateautobuilder.builder;

import com.ultimateautobuilder.builder.undo.UndoRecord;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import com.ultimateautobuilder.core.validation.StructureValidator;
import com.ultimateautobuilder.selection.Selections;
import com.ultimateautobuilder.storage.HistoryEntry;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Construction progressive d'un modèle. Phases : génération (hors thread) → chargement des chunks
 * → fondations (mode terrain) → pose des blocs porteurs puis accrochés → recalcul des formes → fin.
 */
public final class PlaceJob extends Job {
    record Prepared(VoxelModel model, BuildPlan plan) {
    }

    private static final int FLAGS = Block.UPDATE_CLIENTS;
    private static final long LOAD_TIMEOUT_NANOS = 120_000_000_000L;

    final BuildRequest request;
    private final ModelEntry entry;
    private Future<Prepared> future;
    private BuildPlan plan;
    private BlockState[] states;
    private UndoRecord undo = new UndoRecord();
    private final LongArrayList foundation = new LongArrayList();
    private final java.util.ArrayList<BlockState> foundationStates = new java.util.ArrayList<>();

    private int phase; // 0 fondations, 1 pose, 2 formes, 3 terminé
    private int cursor;
    private int foundationCursor;
    private int shapeCursor;
    private int placed;
    private int skippedProtected;
    private int skippedConflicts;
    private int skippedFluids;
    private boolean resumed;
    /** Vérification « zone vide uniquement » : curseur et obstacles trouvés. */
    private int checkCursor;
    private int obstacles;
    private boolean zoneChecked;
    /** Curseur auquel la dernière pause d'étape a eu lieu (évite de s'arrêter deux fois au même endroit). */
    private int lastStepPause = -1;
    private boolean foundationPauseDone;
    private boolean ticketsAdded;
    private long waitStart;
    private final BlockPos.MutableBlockPos cursorPos = new BlockPos.MutableBlockPos();

    // Paramètres figés au lancement (une modification de config n'affecte pas une tâche en cours).
    private final boolean replaceExisting;
    private final boolean protectValuable;
    private final Set<String> protectedIds;
    private final ServerConfig.FluidMode fluidMode;
    private final boolean respectEvents;
    private final boolean particles;

    PlaceJob(int id, UUID owner, String ownerName, BuildRequest request, ModelEntry entry) {
        super(id, owner, ownerName, ResourceKey.create(Registries.DIMENSION, request.dimension()), entry.name());
        this.request = request;
        this.entry = entry;
        boolean cfg = ServerConfig.loaded();
        this.replaceExisting = (!cfg || ServerConfig.REPLACE_EXISTING.get()) && request.mode() != BuildMode.PRUDENT && !request.emptyOnly();
        this.protectValuable = !cfg || ServerConfig.PROTECT_VALUABLE.get();
        this.protectedIds = cfg ? ServerConfig.protectedBlocks() : Set.of();
        this.fluidMode = cfg ? ServerConfig.FLUID_MODE.get() : ServerConfig.FluidMode.REPLACE;
        this.respectEvents = !cfg || ServerConfig.RESPECT_PROTECTION_EVENTS.get();
        this.particles = !cfg || ServerConfig.PARTICLES.get();
    }

    public ModelEntry entry() {
        return entry;
    }

    void startGeneration(BuildManager manager) {
        boolean hollow = request.hollow() && (!ServerConfig.loaded() || ServerConfig.HOLLOW_STATUES.get());
        future = manager.generator().submit(() -> {
            VoxelModel model = entry.generate(request.preset(), request.customSize(), hollow);
            Placement placement = new Placement(model, request.origin().getX(), request.origin().getY(), request.origin().getZ(),
                    request.rotation(), request.mirror());
            return new Prepared(model, BuildPlan.prepare(model, placement));
        });
    }

    /** Reprise après redémarrage : progression et journal d'annulation déjà enregistrés. */
    void resumeFrom(CompoundTag t, UndoRecord savedUndo) {
        resumed = true;
        phase = t.getInt("phase");
        cursor = t.getInt("cursor");
        foundationCursor = t.getInt("foundationCursor");
        shapeCursor = t.getInt("shapeCursor");
        placed = t.getInt("placed");
        skippedProtected = t.getInt("skippedProtected");
        skippedConflicts = t.getInt("skippedConflicts");
        skippedFluids = t.getInt("skippedFluids");
        zoneChecked = t.getBoolean("zoneChecked") || cursor > 0;
        lastStepPause = t.contains("lastStepPause") ? t.getInt("lastStepPause") : -1;
        foundationPauseDone = t.getBoolean("foundationPauseDone");
        if (savedUndo != null) {
            undo = savedUndo;
        }
    }

    public Placement placement() {
        return plan == null ? null : plan.placement;
    }

    public int[] bounds() {
        return plan == null ? new int[6] : plan.placement.bounds();
    }

    @Override
    public int total() {
        return plan == null ? 0 : plan.total() + foundation.size();
    }

    @Override
    public int progress() {
        return plan == null ? 0 : Math.min(total(), foundationCursor + cursor);
    }

    public int placed() {
        return placed;
    }

    @Override
    public void cancel() {
        cancelRequested = true;
    }

    @Override
    public int tick(ServerLevel level, BuildManager manager, int budget, long deadline) {
        if (cancelRequested && !finished()) {
            state = State.CANCELLED;
            message = "Construction annulée (" + placed + " blocs posés)";
            return 0;
        }
        switch (state) {
            case GENERATING -> {
                if (future == null || !future.isDone()) {
                    return 0;
                }
                try {
                    Prepared p = future.get();
                    plan = p.plan();
                } catch (Exception e) {
                    fail("Erreur de génération : " + rootMessage(e));
                    return 0;
                }
                if (!resumed && !validate(level, manager)) {
                    return 0;
                }
                resolveStates();
                state = State.LOADING;
                waitStart = System.nanoTime();
                manager.history().update(id, plan.placement.bounds(), 0, HistoryEntry.Status.RUNNING);
                return 0;
            }
            case LOADING -> {
                if (!ticketsAdded) {
                    manager.chunks().acquire(level, id, plan.chunks);
                    ticketsAdded = true;
                }
                for (long c : plan.chunks) {
                    if (!level.hasChunk(ChunkPos.getX(c), ChunkPos.getZ(c))) {
                        if (System.nanoTime() - waitStart > LOAD_TIMEOUT_NANOS) {
                            fail("Chargement des chunks trop long");
                        }
                        return 0;
                    }
                }
                if (phase == 0 && request.mode() == BuildMode.TERRAIN && foundation.isEmpty()) {
                    computeFoundation(level);
                }
                state = State.PLACING;
                return 0;
            }
            case PLACING -> {
                if (request.slow()) {
                    // Rythme lent : un sixième du budget, pour regarder la construction s'élever.
                    budget = Math.max(4, budget / 6);
                }
                if (request.emptyOnly() && !zoneChecked) {
                    return checkZone(level, budget * 4, deadline);
                }
                int done = 0;
                if (phase == 0) {
                    done += placeFoundation(level, budget, deadline);
                    if (foundationCursor >= foundation.size()) {
                        phase = 1;
                        if (request.stepMode() != BuildRequest.STEP_AUTO && !foundation.isEmpty() && !foundationPauseDone) {
                            foundationPauseDone = true;
                            pause("fondations terminées");
                            return done;
                        }
                    }
                }
                if (phase == 1) {
                    done += placeMain(level, budget - done, deadline);
                    if (cursor >= plan.order.length) {
                        phase = 2;
                        state = State.SHAPING;
                    }
                }
                return done;
            }
            case SHAPING -> {
                int done = updateShapes(level, budget * 2, deadline);
                if (shapeCursor >= plan.shapeUpdates.length) {
                    phase = 3;
                    state = State.DONE;
                    message = summary();
                }
                return done;
            }
            default -> {
                return 0;
            }
        }
    }

    private boolean validate(ServerLevel level, BuildManager manager) {
        var report = StructureValidator.validate(plan.placement, ServerConfig.limits(), level.getMinBuildHeight(), level.getMaxBuildHeight());
        if (report.hasErrors()) {
            fail(report.summary());
            return false;
        }
        if (report.needsConfirmation() && !request.confirmGiant()) {
            fail("Construction géante (" + plan.model.size() + " blocs) : confirmez-la dans le menu ou avec /uab build … confirm");
            return false;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        int[] b = plan.placement.bounds();
        if (player != null && ServerConfig.loaded() && ServerConfig.RESTRICT_TO_SELECTION.get()) {
            var sel = Selections.get(player);
            if (sel == null || !sel.contains(b[0], b[1], b[2], b[3], b[4], b[5])) {
                fail("La construction doit tenir entièrement dans votre zone sélectionnée");
                return false;
            }
        }
        if (!level.getWorldBorder().isWithinBounds(new BlockPos(b[0], 0, b[2])) || !level.getWorldBorder().isWithinBounds(new BlockPos(b[3], 0, b[5]))) {
            fail("La construction dépasse la bordure du monde");
            return false;
        }
        if (player != null && !player.isCreative() && ServerConfig.loaded() && !ServerConfig.FREE_BUILD.get()) {
            return consumeMaterials(player);
        }
        return true;
    }

    /** Mode « consommation de ressources » (désactivé par défaut) : retire les matériaux d'avance. */
    private boolean consumeMaterials(ServerPlayer player) {
        Map<Item, Integer> needed = new HashMap<>();
        Item[] items = new Item[plan.model.paletteSize()];
        for (int i = 0; i < items.length; i++) {
            Item it = BlockResolver.state(plan.model.paletteEntry(i)).getBlock().asItem();
            items[i] = it;
        }
        for (int i = 0; i < plan.model.size(); i++) {
            Item it = items[VoxelModel.stateId(plan.model.cell(i))];
            if (it != Items.AIR) {
                needed.merge(it, 1, Integer::sum);
            }
        }
        Inventory inv = player.getInventory();
        StringBuilder missing = new StringBuilder();
        for (var e : needed.entrySet()) {
            int have = inv.countItem(e.getKey());
            if (have < e.getValue()) {
                if (!missing.isEmpty()) {
                    missing.append(", ");
                }
                missing.append(e.getValue() - have).append(" × ").append(BuiltInRegistries.ITEM.getKey(e.getKey()).getPath());
                if (missing.length() > 200) {
                    missing.append("…");
                    break;
                }
            }
        }
        if (!missing.isEmpty()) {
            fail("Matériaux manquants : " + missing);
            return false;
        }
        for (var e : needed.entrySet()) {
            int left = e.getValue();
            for (int s = 0; s < inv.getContainerSize() && left > 0; s++) {
                ItemStack st = inv.getItem(s);
                if (st.is(e.getKey())) {
                    int take = Math.min(left, st.getCount());
                    st.shrink(take);
                    left -= take;
                }
            }
        }
        inv.setChanged();
        return true;
    }

    private void resolveStates() {
        states = new BlockState[plan.model.paletteSize()];
        Rotation rot = switch (request.rotation()) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        for (int i = 0; i < states.length; i++) {
            BlockState s = BlockResolver.state(plan.model.paletteEntry(i));
            if (request.mirror()) {
                s = s.mirror(Mirror.FRONT_BACK);
            }
            states[i] = s.rotate(rot);
        }
    }

    private void computeFoundation(ServerLevel level) {
        int maxDepth = ServerConfig.loaded() ? ServerConfig.FOUNDATION_MAX_DEPTH.get() : 24;
        int[] cols = plan.bottomColumns;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i + 3 < cols.length; i += 4) {
            int x = cols[i], y = cols[i + 1], z = cols[i + 2];
            BlockState mat = states[cols[i + 3]];
            p.set(x, y, z);
            if (mat.isAir() || !mat.isCollisionShapeFullBlock(level, p)) {
                mat = Blocks.COBBLESTONE.defaultBlockState();
            }
            for (int d = 1; d <= maxDepth; d++) {
                int yy = y - d;
                if (yy < level.getMinBuildHeight()) {
                    break;
                }
                p.set(x, yy, z);
                BlockState cur = level.getBlockState(p);
                if (!cur.isAir() && !cur.canBeReplaced() && cur.getFluidState().isEmpty()) {
                    break;
                }
                foundation.add(p.asLong());
                foundationStates.add(mat);
            }
        }
    }

    private int placeFoundation(ServerLevel level, int budget, long deadline) {
        int done = 0;
        while (foundationCursor < foundation.size() && done < budget) {
            if ((done & 31) == 0 && System.nanoTime() > deadline) {
                break;
            }
            cursorPos.set(foundation.getLong(foundationCursor));
            if (!level.isLoaded(cursorPos)) {
                break;
            }
            apply(level, cursorPos, foundationStates.get(foundationCursor));
            foundationCursor++;
            done++;
        }
        return done;
    }

    /**
     * Option « zone vide uniquement » : chaque position de la structure doit être de l'air ou un bloc
     * remplaçable (herbe haute, neige…). La vérification est répartie sur plusieurs ticks.
     */
    private int checkZone(ServerLevel level, int budget, long deadline) {
        Placement pl = plan.placement;
        VoxelModel m = plan.model;
        int done = 0;
        while (checkCursor < plan.order.length && done < budget) {
            if ((done & 63) == 0 && System.nanoTime() > deadline) {
                return done;
            }
            long cell = m.cell(plan.order[checkCursor++]);
            if (states[VoxelModel.stateId(cell)].isAir()) {
                continue;
            }
            cursorPos.set(pl.worldX(cell), pl.worldY(cell), pl.worldZ(cell));
            if (!level.isLoaded(cursorPos)) {
                checkCursor--;
                return done;
            }
            BlockState cur = level.getBlockState(cursorPos);
            if (!cur.isAir() && !cur.canBeReplaced()) {
                obstacles++;
            }
            done++;
        }
        if (checkCursor >= plan.order.length) {
            zoneChecked = true;
            if (obstacles > 0) {
                fail("Zone non vide : " + obstacles + " bloc(s) gênant(s). Option « zone vide uniquement » active : rien n'a été posé");
            }
        }
        return done;
    }

    /** Vrai si le mode pas à pas impose une pause avant de poser la cellule d'indice {@code index} de l'ordre. */
    private boolean stepBoundary(int index) {
        int mode = request.stepMode();
        if (mode == BuildRequest.STEP_AUTO || index == lastStepPause || index == 0) {
            return false;
        }
        if (index == plan.solidCount) {
            return true; // passage aux éléments accrochés (portes, torches, plantes, fluides)
        }
        if (index > plan.solidCount) {
            return false;
        }
        int y = VoxelModel.cy(plan.model.cell(plan.order[index]));
        int prevY = VoxelModel.cy(plan.model.cell(plan.order[index - 1]));
        if (y == prevY) {
            return false;
        }
        if (mode == BuildRequest.STEP_LAYER) {
            return true;
        }
        int h = Math.max(1, plan.model.sizeY());
        return (y * 4) / h != (prevY * 4) / h;
    }

    private String stepLabel(int index) {
        if (index == plan.solidCount) {
            return "structure terminée, prochaine étape : détails (portes, lumières, plantes)";
        }
        int y = VoxelModel.cy(plan.model.cell(plan.order[index]));
        int h = Math.max(1, plan.model.sizeY());
        return request.stepMode() == BuildRequest.STEP_LAYER
                ? "couche " + y + "/" + (h - 1) + " terminée"
                : "étape " + ((y * 4) / h) + "/4 terminée";
    }

    private int placeMain(ServerLevel level, int budget, long deadline) {
        int done = 0;
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        Placement pl = plan.placement;
        VoxelModel m = plan.model;
        while (cursor < plan.order.length && done < budget) {
            if ((done & 31) == 0 && System.nanoTime() > deadline) {
                break;
            }
            if (stepBoundary(cursor)) {
                lastStepPause = cursor;
                pause(stepLabel(cursor));
                break;
            }
            long cell = m.cell(plan.order[cursor]);
            int y = pl.worldY(cell);
            if (y < minY || y >= maxY) {
                cursor++;
                continue;
            }
            cursorPos.set(pl.worldX(cell), y, pl.worldZ(cell));
            if (!level.isLoaded(cursorPos)) {
                break; // chunk pas encore prêt : on réessaie au prochain tick sans charger de force
            }
            apply(level, cursorPos, states[VoxelModel.stateId(cell)]);
            cursor++;
            done++;
            if (particles && (placed & 255) == 0 && placed > 0) {
                level.sendParticles(ParticleTypes.END_ROD, cursorPos.getX() + 0.5, cursorPos.getY() + 0.5, cursorPos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
            }
        }
        return done;
    }

    /** Pose d'un bloc avec toutes les vérifications ; enregistre l'ancien état pour l'annulation. */
    private void apply(ServerLevel level, BlockPos pos, BlockState target) {
        BlockState current = level.getBlockState(pos);
        if (current == target) {
            return;
        }
        if (current.getDestroySpeed(level, pos) < 0) {
            skippedProtected++;
            return;
        }
        if (protectValuable && !current.isAir() && protectedIds.contains(BuiltInRegistries.BLOCK.getKey(current.getBlock()).toString())) {
            skippedProtected++;
            return;
        }
        boolean replaceable = current.isAir() || current.canBeReplaced();
        if (!replaceable && !replaceExisting) {
            skippedConflicts++;
            if (request.emptyOnly() && !paused) {
                // Un bloc est apparu depuis la vérification : on s'arrête plutôt que de construire autour.
                pause("obstacle apparu en " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            }
            return;
        }
        if (fluidMode == ServerConfig.FluidMode.KEEP && !current.getFluidState().isEmpty()) {
            skippedFluids++;
            return;
        }
        if (respectEvents && isBlockedByProtection(level, pos, current, target)) {
            skippedProtected++;
            return;
        }
        CompoundTag beTag = null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            beTag = be.saveWithFullMetadata(level.registryAccess());
            // Vider le conteneur évite que son contenu ne tombe au sol (et ne soit dupliqué à l'annulation).
            Clearable.tryClear(be);
        }
        if (level.setBlock(pos, target, FLAGS)) {
            undo.record(pos, current, target, beTag);
            placed++;
        }
    }

    private boolean isBlockedByProtection(ServerLevel level, BlockPos pos, BlockState current, BlockState target) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            return false;
        }
        if (!current.isAir()) {
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, current, player);
            if (NeoForge.EVENT_BUS.post(breakEvent).isCanceled()) {
                return true;
            }
        }
        if (!target.isAir()) {
            BlockSnapshot snap = BlockSnapshot.create(level.dimension(), level, pos);
            BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(snap, current, player);
            return NeoForge.EVENT_BUS.post(placeEvent).isCanceled();
        }
        return false;
    }

    private int updateShapes(ServerLevel level, int budget, long deadline) {
        int done = 0;
        Placement pl = plan.placement;
        while (shapeCursor < plan.shapeUpdates.length && done < budget) {
            if ((done & 63) == 0 && System.nanoTime() > deadline) {
                break;
            }
            long cell = plan.model.cell(plan.shapeUpdates[shapeCursor]);
            cursorPos.set(pl.worldX(cell), pl.worldY(cell), pl.worldZ(cell));
            if (!level.isLoaded(cursorPos)) {
                break;
            }
            BlockState current = level.getBlockState(cursorPos);
            if (current.getBlock() == states[VoxelModel.stateId(cell)].getBlock()) {
                BlockState updated = Block.updateFromNeighbourShapes(current, level, cursorPos);
                if (updated != current) {
                    level.setBlock(cursorPos, updated, FLAGS);
                }
            }
            shapeCursor++;
            done++;
        }
        return done;
    }

    private void fail(String why) {
        state = State.FAILED;
        message = why;
    }

    private String summary() {
        StringBuilder sb = new StringBuilder(title).append(" terminé : ").append(placed).append(" blocs posés");
        if (skippedProtected > 0) {
            sb.append(", ").append(skippedProtected).append(" protégés");
        }
        if (skippedConflicts > 0) {
            sb.append(", ").append(skippedConflicts).append(" conflits évités");
        }
        if (skippedFluids > 0) {
            sb.append(", ").append(skippedFluids).append(" fluides conservés");
        }
        return sb.toString();
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    @Override
    public void finish(ServerLevel level, BuildManager manager) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        manager.chunks().release(level, id);
        boolean hasUndo = undo.size() > 0;
        if (hasUndo) {
            manager.storage().writeUndo(id, undo.save());
        }
        HistoryEntry.Status st = switch (state) {
            case DONE -> HistoryEntry.Status.DONE;
            case CANCELLED -> HistoryEntry.Status.CANCELLED;
            default -> HistoryEntry.Status.FAILED;
        };
        manager.history().finish(id, plan == null ? null : plan.placement.bounds(), placed, st, hasUndo);
        manager.storage().deleteCheckpoint(id);
    }

    @Override
    public CompoundTag checkpoint() {
        if (finished()) {
            return null;
        }
        CompoundTag t = new CompoundTag();
        t.putString("kind", "place");
        t.putInt("id", id);
        if (owner != null) {
            t.putUUID("owner", owner);
        }
        t.putString("ownerName", ownerName);
        t.put("request", request.save());
        t.putInt("phase", phase);
        t.putInt("cursor", cursor);
        t.putInt("foundationCursor", foundationCursor);
        t.putInt("shapeCursor", shapeCursor);
        t.putInt("placed", placed);
        t.putInt("skippedProtected", skippedProtected);
        t.putInt("skippedConflicts", skippedConflicts);
        t.putInt("skippedFluids", skippedFluids);
        t.putBoolean("zoneChecked", zoneChecked);
        t.putInt("lastStepPause", lastStepPause);
        t.putBoolean("foundationPauseDone", foundationPauseDone);
        t.put("undo", undo.save());
        return t;
    }
}
