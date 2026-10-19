package com.ultimateautobuilder.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.ultimateautobuilder.builder.BlockResolver;
import com.ultimateautobuilder.core.model.B;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maillage d'un modèle rendu avec les vrais modèles et textures des blocs de Minecraft
 * (mêmes quads que dans le monde, teintes d'herbe et de feuillage comprises). Seules les faces
 * visibles sont émises : une face collée à un bloc plein opaque est omise. Le maillage est
 * construit hors du thread de rendu, puis téléversé une seule fois sur la carte graphique.
 */
public final class BlockMesh implements AutoCloseable {
    /** Données construites hors du thread de rendu, en attente de téléversement. */
    public static final class Built {
        final MeshData cutout, translucent;
        final ByteBufferBuilder cutoutBytes, translucentBytes;
        final float cx, cy, cz, radius;
        final int quads;
        final boolean truncated, allTranslucent;

        Built(MeshData cutout, MeshData translucent, ByteBufferBuilder cutoutBytes, ByteBufferBuilder translucentBytes, float cx, float cy, float cz,
              float radius, int quads, boolean truncated, boolean allTranslucent) {
            this.cutout = cutout;
            this.translucent = translucent;
            this.cutoutBytes = cutoutBytes;
            this.translucentBytes = translucentBytes;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.radius = radius;
            this.quads = quads;
            this.truncated = truncated;
            this.allTranslucent = allTranslucent;
        }

        /** Libère la mémoire si le maillage n'est jamais téléversé. */
        void discard() {
            closeQuietly(cutout);
            closeQuietly(translucent);
            cutoutBytes.close();
            translucentBytes.close();
        }

        public boolean truncated() {
            return truncated;
        }
    }

    private VertexBuffer cutout, translucent;
    private final float cx, cy, cz, radius;
    private final boolean allTranslucent;
    private final int quads;

    private BlockMesh(Built b) {
        RenderSystem.assertOnRenderThread();
        this.cx = b.cx;
        this.cy = b.cy;
        this.cz = b.cz;
        this.radius = b.radius;
        this.quads = b.quads;
        this.allTranslucent = b.allTranslucent;
        try {
            this.cutout = upload(b.cutout);
            this.translucent = upload(b.translucent);
        } finally {
            b.cutoutBytes.close();
            b.translucentBytes.close();
        }
    }

    private static VertexBuffer upload(MeshData mesh) {
        if (mesh == null) {
            return null;
        }
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        // upload() consomme et libère MeshData.
        vb.upload(mesh);
        VertexBuffer.unbind();
        return vb;
    }

    /** Téléverse un maillage construit (thread de rendu uniquement). */
    public static BlockMesh upload(Built b) {
        return new BlockMesh(b);
    }

    public int quads() {
        return quads;
    }

    public float radius() {
        return radius;
    }

    // ------------------------------------------------------------------ construction

    /** Source de blocs : position relative et état Minecraft. */
    public interface Source {
        void forEach(CellConsumer consumer);
    }

    @FunctionalInterface
    public interface CellConsumer {
        void accept(int x, int y, int z, BlockState state);
    }

    /** Modèle tel quel (aperçus de menu). */
    public static Source of(VoxelModel model) {
        return consumer -> {
            BlockState[] states = resolvePalette(model, Rotation.NONE, false);
            for (int i = 0; i < model.size(); i++) {
                long c = model.cell(i);
                BlockState s = states[VoxelModel.stateId(c)];
                if (s != null) {
                    consumer.accept(VoxelModel.cx(c), VoxelModel.cy(c), VoxelModel.cz(c), s);
                }
            }
        };
    }

    /** Modèle orienté comme il sera posé (fantôme dans le monde), positions relatives à l'origine. */
    public static Source placed(VoxelModel model, int rotation, boolean mirror) {
        return consumer -> {
            Rotation rot = switch (Math.floorMod(rotation, 4)) {
                case 1 -> Rotation.CLOCKWISE_90;
                case 2 -> Rotation.CLOCKWISE_180;
                case 3 -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
            BlockState[] states = resolvePalette(model, rot, mirror);
            Placement pl = new Placement(model, 0, 0, 0, rotation, mirror);
            for (int i = 0; i < model.size(); i++) {
                long c = model.cell(i);
                BlockState s = states[VoxelModel.stateId(c)];
                if (s != null) {
                    consumer.accept(pl.worldX(c), pl.worldY(c), pl.worldZ(c), s);
                }
            }
        };
    }

    private static BlockState[] resolvePalette(VoxelModel model, Rotation rot, boolean mirror) {
        BlockState[] states = new BlockState[model.paletteSize()];
        for (int i = 0; i < states.length; i++) {
            String text = model.paletteEntry(i);
            if (B.isAir(text)) {
                continue;
            }
            BlockState s = BlockResolver.state(text);
            if (mirror) {
                s = s.mirror(Mirror.FRONT_BACK);
            }
            states[i] = displayable(s.rotate(rot));
        }
        return states;
    }

    /**
     * Les fluides et les blocs dessinés par un rendu d'entité (coffres, lits…) n'ont pas de quads
     * de modèle : un bloc d'apparence proche les représente dans l'aperçu.
     */
    static BlockState displayable(BlockState s) {
        if (s.isAir()) {
            return null;
        }
        if (s.getRenderShape() == RenderShape.MODEL) {
            return s;
        }
        if (!s.getFluidState().isEmpty()) {
            return s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        if (path.contains("chest")) {
            return Blocks.BARREL.defaultBlockState();
        }
        if (path.endsWith("_bed")) {
            BlockState wool = BlockResolver.state("minecraft:" + path.replace("_bed", "_wool"));
            return wool.isAir() ? Blocks.WHITE_WOOL.defaultBlockState() : wool;
        }
        if (path.contains("shulker_box")) {
            return Blocks.PURPUR_BLOCK.defaultBlockState();
        }
        return null;
    }

    /**
     * @param alpha opacité des sommets (1 = opaque ; < 1 pour un fantôme)
     * @param maxQuads au-delà, le maillage est tronqué (signalé par {@link Built#truncated()})
     */
    public static Built build(Source source, float alpha, int maxQuads) {
        Long2ObjectOpenHashMap<BlockState> cells = new Long2ObjectOpenHashMap<>();
        int[] bounds = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        source.forEach((x, y, z, s) -> {
            cells.put(BlockPos.asLong(x, y, z), s);
            bounds[0] = Math.min(bounds[0], x);
            bounds[1] = Math.min(bounds[1], y);
            bounds[2] = Math.min(bounds[2], z);
            bounds[3] = Math.max(bounds[3], x);
            bounds[4] = Math.max(bounds[4], y);
            bounds[5] = Math.max(bounds[5], z);
        });
        boolean allTranslucent = alpha < 0.999f;
        ByteBufferBuilder cutBytes = new ByteBufferBuilder(256 * 1024);
        ByteBufferBuilder trBytes = new ByteBufferBuilder(allTranslucent ? 256 * 1024 : 16 * 1024);
        BufferBuilder cut = new BufferBuilder(cutBytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        BufferBuilder tr = new BufferBuilder(trBytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        int quads = 0;
        boolean truncated = false;
        if (!cells.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
            BlockColors colors = mc.getBlockColors();
            RandomSource rand = RandomSource.create();
            PoseStack pose = new PoseStack();
            Map<BlockState, Boolean> solid = new HashMap<>();
            int light = LightTexture.FULL_BRIGHT;
            Direction[] dirs = Direction.values();
            BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
            for (Long2ObjectMap.Entry<BlockState> e : cells.long2ObjectEntrySet()) {
                if (quads >= maxQuads) {
                    truncated = true;
                    break;
                }
                long key = e.getLongKey();
                BlockState state = e.getValue();
                int x = BlockPos.getX(key), y = BlockPos.getY(key), z = BlockPos.getZ(key);
                BakedModel model = dispatcher.getBlockModel(state);
                rand.setSeed(42L);
                ChunkRenderTypeSet types = model.getRenderTypes(state, rand, ModelData.EMPTY);
                boolean translucent = allTranslucent || types.contains(RenderType.translucent());
                BufferBuilder out = translucent ? tr : cut;
                pose.pushPose();
                pose.translate(x, y, z);
                for (int d = 0; d <= dirs.length; d++) {
                    Direction dir = d < dirs.length ? dirs[d] : null;
                    if (dir != null) {
                        BlockState nb = cells.get(n.set(x, y, z).move(dir).asLong());
                        // Face cachée par un bloc plein opaque, ou entre deux blocs identiques (verre contre verre).
                        if (nb != null && (solid.computeIfAbsent(nb, s -> s.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
                                || state.skipRendering(nb, dir))) {
                            continue;
                        }
                    }
                    for (RenderType rt : types) {
                        rand.setSeed(42L);
                        List<BakedQuad> list = model.getQuads(state, dir, rand, ModelData.EMPTY, rt);
                        for (BakedQuad q : list) {
                            float r = 1f, g = 1f, b = 1f;
                            if (q.isTinted()) {
                                int c = colors.getColor(state, null, null, q.getTintIndex());
                                r = ((c >> 16) & 255) / 255f;
                                g = ((c >> 8) & 255) / 255f;
                                b = (c & 255) / 255f;
                            }
                            out.putBulkData(pose.last(), q, r, g, b, alpha, light, OverlayTexture.NO_OVERLAY);
                            quads++;
                        }
                    }
                }
                pose.popPose();
            }
        }
        float cx = 0, cy = 0, cz = 0, radius = 1;
        if (!cells.isEmpty()) {
            cx = (bounds[0] + bounds[3] + 1) / 2f;
            cy = (bounds[1] + bounds[4] + 1) / 2f;
            cz = (bounds[2] + bounds[5] + 1) / 2f;
            float sx = bounds[3] - bounds[0] + 1, sy = bounds[4] - bounds[1] + 1, sz = bounds[5] - bounds[2] + 1;
            radius = (float) Math.sqrt(sx * sx + sy * sy + sz * sz) / 2f;
        }
        return new Built(cut.build(), tr.build(), cutBytes, trBytes, cx, cy, cz, radius, quads, truncated, allTranslucent);
    }

    // ------------------------------------------------------------------ dessin

    /** Dessine avec les matrices données (monde ou interface). */
    public void draw(Matrix4f modelView, Matrix4f projection) {
        RenderSystem.assertOnRenderThread();
        if (cutout != null) {
            drawBuffer(cutout, allTranslucent ? RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)
                    : RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS), modelView, projection);
        }
        if (translucent != null) {
            drawBuffer(translucent, RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), modelView, projection);
        }
    }

    private static void drawBuffer(VertexBuffer vb, RenderType type, Matrix4f modelView, Matrix4f projection) {
        type.setupRenderState();
        vb.bind();
        vb.drawWithShader(modelView, projection, RenderSystem.getShader());
        VertexBuffer.unbind();
        type.clearRenderState();
    }

    /**
     * Dessine le modèle centré dans un cadre de l'interface, en projection orthographique.
     *
     * @param yaw rotation autour de la verticale (0 = face +Z vers le joueur)
     * @param pitch inclinaison (positive = vu d'au-dessus)
     */
    public void drawInGui(GuiGraphics g, int x, int y, int w, int h, float yaw, float pitch, float zoom) {
        if (w <= 2 || h <= 2) {
            return;
        }
        g.flush();
        float z = Math.max(0.2f, Math.min(4f, zoom));
        float scale = Math.min(w, h) * 0.5f / Math.max(0.5f, radius) * z;
        Matrix4f m = new Matrix4f(g.pose().last().pose());
        // Profondeur : le modèle reste devant le fond (z ≈ 0) et dans le volume visible de l'interface.
        m.translate(x + w / 2f, y + h / 2f, 400f);
        m.scale(scale, -scale, scale);
        m.rotateX((float) Math.toRadians(pitch));
        m.rotateY((float) Math.toRadians(yaw));
        m.translate(-cx, -cy, -cz);
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m);
        g.enableScissor(x, y, x + w, y + h);
        Lighting.setupFor3DItems();
        draw(mv, RenderSystem.getProjectionMatrix());
        g.disableScissor();
        // Le modèle a écrit dans le tampon de profondeur : les éléments dessinés ensuite doivent rester visibles.
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    @Override
    public void close() {
        if (cutout != null) {
            cutout.close();
            cutout = null;
        }
        if (translucent != null) {
            translucent.close();
            translucent = null;
        }
    }

    private static void closeQuietly(MeshData m) {
        if (m != null) {
            try {
                m.close();
            } catch (RuntimeException ignored) {
                // Déjà libéré.
            }
        }
    }
}
