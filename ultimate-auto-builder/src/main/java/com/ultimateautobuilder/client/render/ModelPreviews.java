package com.ultimateautobuilder.client.render;

import com.ultimateautobuilder.client.ThumbnailCache;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.model.ModelScaler;
import com.ultimateautobuilder.core.model.VoxelModel;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Aperçus 3D texturés du catalogue : cartes (petit maillage, vue fixe) et grand aperçu
 * (maillage plus détaillé, orientable). Les modèles trop grands sont réduits au plus proche
 * voisin avant le maillage pour borner la mémoire graphique et le temps de construction.
 */
public final class ModelPreviews {
    /** Côté maximal (en blocs) du modèle utilisé pour une carte et pour le grand aperçu. */
    public static final int CARD_SIDE = 40, DETAIL_SIDE = 112;
    private static final int CARD_QUADS = 60_000, DETAIL_QUADS = 400_000;

    private static final MeshCache CARDS = new MeshCache(64);
    private static final MeshCache DETAIL = new MeshCache(6);

    /** Vue par défaut : façade et flanc droit, vus légèrement d'au-dessus. */
    public static final float BASE_YAW = -35f, BASE_PITCH = 28f;

    private ModelPreviews() {
    }

    /** Réduit un modèle pour que son plus grand côté ne dépasse pas {@code maxSide}. */
    public static VoxelModel lod(VoxelModel m, int maxSide) {
        int side = Math.max(m.sizeX(), Math.max(m.sizeY(), m.sizeZ()));
        return side <= maxSide ? m : ModelScaler.scale(m, (double) maxSide / side);
    }

    /** Carte du catalogue ; renvoie false si le maillage n'est pas encore prêt (afficher un repli). */
    public static boolean drawCard(GuiGraphics g, ModelEntry e, int x, int y, int w, int h, int view) {
        BlockMesh mesh = CARDS.get("card:" + e.id(), () -> {
            VoxelModel m = ThumbnailCache.previewModel(e);
            return m == null || m.isEmpty() ? null : BlockMesh.build(BlockMesh.of(lod(m, CARD_SIDE)), 1f, CARD_QUADS);
        });
        if (mesh == null) {
            return false;
        }
        mesh.drawInGui(g, x, y, w, h, BASE_YAW - 90f * view, BASE_PITCH, 0.95f);
        return true;
    }

    /**
     * Grand aperçu du modèle à la taille choisie.
     *
     * @param variant distingue les tailles / options (clé du cache)
     */
    public static boolean drawDetail(GuiGraphics g, ModelEntry e, VoxelModel model, String variant, int x, int y, int w, int h, float yaw,
                                     float pitch, float zoom) {
        if (model == null || model.isEmpty()) {
            return false;
        }
        BlockMesh mesh = DETAIL.get("detail:" + e.id() + "#" + variant,
                () -> BlockMesh.build(BlockMesh.of(lod(model, DETAIL_SIDE)), 1f, DETAIL_QUADS));
        if (mesh == null) {
            return false;
        }
        mesh.drawInGui(g, x, y, w, h, yaw, pitch, zoom);
        return true;
    }

    public static boolean detailPending(ModelEntry e, String variant) {
        return DETAIL.isPending("detail:" + e.id() + "#" + variant);
    }

    /** Un modèle personnalisé a changé (aperçu reçu du serveur, suppression…). */
    public static void invalidate(String modelId) {
        CARDS.invalidate("card:" + modelId);
        DETAIL.invalidate("detail:" + modelId + "#");
    }

    /** Ressources rechargées : les maillages référencent les anciens modèles de blocs. */
    public static void clearAll() {
        CARDS.clear();
        DETAIL.clear();
    }
}
