package com.ultimateautobuilder.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ultimateautobuilder.client.PlacementController;
import com.ultimateautobuilder.config.ClientConfig;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import com.ultimateautobuilder.core.render.ColorTable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.concurrent.CompletableFuture;

/**
 * Aperçu fantôme : boîte englobante + faces extérieures translucides. Aucun bloc n'est posé.
 * Les faces visibles sont calculées hors du thread de rendu, une fois par couple (modèle, rotation,
 * miroir), en coordonnées relatives à l'origine ; seul le décalage change à chaque image.
 */
public final class PreviewRenderer {
    /** Faces précalculées : x, y, z relatifs, direction (0..5), couleur RGB, par groupes de 5 entiers. */
    private record Faces(VoxelModel model, int rotation, boolean mirror, int maxFaces, int[] data, int count, boolean truncated) {
    }

    private static CompletableFuture<Faces> pending;
    private static Faces current;

    private PreviewRenderer() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !PlacementController.active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        VoxelModel model = PlacementController.model();
        if (model == null || model.isEmpty()) {
            return;
        }
        BlockPos origin = PlacementController.origin(mc, model);
        if (origin == null) {
            return;
        }
        int rotation = PlacementController.rotation(mc);
        boolean mirror = PlacementController.SETTINGS.mirror;
        int lift = PlacementController.aerialLiftForPreview();
        Placement pl = new Placement(model, origin.getX(), origin.getY() + lift, origin.getZ(), rotation, mirror);
        int[] b = pl.bounds();

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // Cadre : cyan si la hauteur est valide, rouge sinon.
        boolean inHeight = b[1] >= mc.level.getMinBuildHeight() && b[4] < mc.level.getMaxBuildHeight();
        float r = inHeight ? 0.35f : 1f, g = inHeight ? 0.9f : 0.25f, bl = inHeight ? 1f : 0.25f;
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, new AABB(b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1), r, g, bl, 1f);
        buffers.endBatch(RenderType.lines());

        if (!ClientConfig.loaded() || ClientConfig.GHOST_PREVIEW.get()) {
            int maxFaces = ClientConfig.loaded() ? ClientConfig.GHOST_MAX_FACES.get() : 40_000;
            BlockMesh mesh = ghost(model, rotation, mirror, maxFaces);
            if (mesh != null) {
                // Fantôme texturé : vrais modèles de blocs, semi-transparents, décalés en profondeur
                // pour ne pas scintiller contre les blocs déjà présents.
                pose.pushPose();
                pose.translate(origin.getX(), origin.getY() + lift, origin.getZ());
                Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-1f, -10f);
                mesh.draw(mv, RenderSystem.getProjectionMatrix());
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
                pose.popPose();
                pose.popPose();
                return;
            }
            Faces faces = faces(model, rotation, mirror, maxFaces);
            if (faces != null && faces.count() > 0) {
                pose.pushPose();
                pose.translate(origin.getX(), origin.getY() + lift, origin.getZ());
                VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
                emit(pose.last().pose(), quads, faces);
                buffers.endBatch(RenderType.debugQuads());
                pose.popPose();
            }
        }
        pose.popPose();
    }

    private static final java.util.concurrent.ExecutorService GHOST_WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UAB-Ghost");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static BlockMesh ghostMesh;
    private static Object[] ghostKey, pendingKey, failedKey;
    private static CompletableFuture<BlockMesh.Built> ghostPending;

    private static boolean sameKey(Object[] k, VoxelModel model, int rotation, boolean mirror) {
        return k != null && k[0] == model && (Integer) k[1] == rotation && (Boolean) k[2] == mirror;
    }

    /**
     * Maillage texturé du fantôme, ou null tant qu'il se construit, s'il dépasse la limite de faces
     * ou en cas d'échec : les faces colorées servent alors de repli.
     */
    private static BlockMesh ghost(VoxelModel model, int rotation, boolean mirror, int maxFaces) {
        if (sameKey(ghostKey, model, rotation, mirror)) {
            return ghostMesh;
        }
        if (ghostPending != null && ghostPending.isDone()) {
            BlockMesh.Built b = ghostPending.getNow(null);
            Object[] k = pendingKey;
            ghostPending = null;
            pendingKey = null;
            if (b == null || b.truncated()) {
                if (b != null) {
                    b.discard();
                }
                failedKey = k;
            } else if (sameKey(k, model, rotation, mirror)) {
                if (ghostMesh != null) {
                    ghostMesh.close();
                }
                ghostMesh = BlockMesh.upload(b);
                ghostKey = k;
                return ghostMesh;
            } else {
                b.discard();
            }
        }
        if (ghostPending == null && !sameKey(failedKey, model, rotation, mirror)) {
            Object[] k = {model, rotation, mirror};
            pendingKey = k;
            ghostPending = CompletableFuture.supplyAsync(() -> BlockMesh.build(BlockMesh.placed(model, rotation, mirror), 0.6f, maxFaces * 2),
                    GHOST_WORKER).exceptionally(t -> {
                        com.ultimateautobuilder.UltimateAutoBuilderMod.LOGGER.warn("Fantôme texturé impossible : {}", t.toString());
                        return null;
                    });
        }
        return null;
    }

    /** Faces du cache, ou null si le calcul est en cours (le cadre reste affiché en attendant). */
    private static Faces faces(VoxelModel model, int rotation, boolean mirror, int maxFaces) {
        Faces c = current;
        if (c != null && c.model() == model && c.rotation() == rotation && c.mirror() == mirror && c.maxFaces() == maxFaces) {
            return c;
        }
        if (pending != null && pending.isDone()) {
            Faces f = pending.getNow(null);
            pending = null;
            if (f != null) {
                current = f;
                if (f.model() == model && f.rotation() == rotation && f.mirror() == mirror && f.maxFaces() == maxFaces) {
                    return f;
                }
            }
        }
        if (pending == null) {
            pending = CompletableFuture.supplyAsync(() -> compute(model, rotation, mirror, maxFaces));
        }
        return null;
    }

    private static long key(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    private static final int[][] DIRS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    static Faces compute(VoxelModel model, int rotation, boolean mirror, int maxFaces) {
        int n = model.size();
        String[] palette = model.paletteCopy();
        boolean[] air = new boolean[palette.length];
        int[] colors = new int[palette.length];
        for (int i = 0; i < palette.length; i++) {
            air[i] = B.isAir(palette[i]);
            colors[i] = ColorTable.rgb(palette[i]) & 0xFFFFFF;
        }
        Placement pl = new Placement(model, 0, 0, 0, rotation, mirror);
        LongOpenHashSet occupied = new LongOpenHashSet(n);
        int[] xs = new int[n], ys = new int[n], zs = new int[n], st = new int[n];
        int solid = 0;
        for (int i = 0; i < n; i++) {
            long c = model.cell(i);
            int s = VoxelModel.stateId(c);
            if (air[s]) {
                continue;
            }
            xs[solid] = pl.worldX(c);
            ys[solid] = pl.worldY(c);
            zs[solid] = pl.worldZ(c);
            st[solid] = s;
            occupied.add(key(xs[solid], ys[solid], zs[solid]));
            solid++;
        }
        // Premier passage : compte des faces exposées, pour échantillonner uniformément si besoin.
        int exposed = 0;
        for (int i = 0; i < solid; i++) {
            for (int[] d : DIRS) {
                if (!occupied.contains(key(xs[i] + d[0], ys[i] + d[1], zs[i] + d[2]))) {
                    exposed++;
                }
            }
        }
        int stride = Math.max(1, (exposed + maxFaces - 1) / Math.max(1, maxFaces));
        int[] data = new int[Math.min(exposed, maxFaces) * 5 + 5];
        int count = 0, seen = 0;
        for (int i = 0; i < solid && count < maxFaces; i++) {
            for (int f = 0; f < 6 && count < maxFaces; f++) {
                int[] d = DIRS[f];
                if (occupied.contains(key(xs[i] + d[0], ys[i] + d[1], zs[i] + d[2]))) {
                    continue;
                }
                if (seen++ % stride != 0) {
                    continue;
                }
                int o = count * 5;
                data[o] = xs[i];
                data[o + 1] = ys[i];
                data[o + 2] = zs[i];
                data[o + 3] = f;
                data[o + 4] = colors[st[i]];
                count++;
            }
        }
        return new Faces(model, rotation, mirror, maxFaces, data, count, stride > 1);
    }

    private static void emit(Matrix4f m, VertexConsumer vc, Faces faces) {
        int[] d = faces.data();
        // Légère dilatation pour éviter le scintillement avec les blocs existants.
        final float e = 0.002f;
        for (int i = 0; i < faces.count(); i++) {
            int o = i * 5;
            float x0 = d[o] - e, y0 = d[o + 1] - e, z0 = d[o + 2] - e;
            float x1 = d[o] + 1 + e, y1 = d[o + 1] + 1 + e, z1 = d[o + 2] + 1 + e;
            int face = d[o + 3];
            int rgb = d[o + 4];
            // Ombrage simple par orientation pour garder le relief lisible.
            float shade = switch (face) {
                case 0 -> 0.55f;
                case 1 -> 1f;
                case 2, 3 -> 0.8f;
                default -> 0.68f;
            };
            int r = (int) (((rgb >> 16) & 0xFF) * shade), g = (int) (((rgb >> 8) & 0xFF) * shade), b = (int) ((rgb & 0xFF) * shade);
            int a = 110;
            switch (face) {
                case 0 -> quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
                case 1 -> quad(vc, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
                case 2 -> quad(vc, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
                case 3 -> quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
                case 4 -> quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
                default -> quad(vc, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
            }
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy,
                             float cz, float dx, float dy, float dz, int r, int g, int b, int a) {
        vc.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        vc.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        vc.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        vc.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
    }

    /** Libère le cache (déconnexion). */
    public static void reset() {
        current = null;
        pending = null;
        if (ghostMesh != null) {
            ghostMesh.close();
            ghostMesh = null;
        }
        ghostKey = null;
        failedKey = null;
    }
}
