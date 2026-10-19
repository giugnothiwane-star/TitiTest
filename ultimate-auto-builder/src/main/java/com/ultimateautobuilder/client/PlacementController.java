package com.ultimateautobuilder.client;

import com.ultimateautobuilder.builder.BuildMode;
import com.ultimateautobuilder.config.ClientConfig;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import com.ultimateautobuilder.core.validation.Limits;
import com.ultimateautobuilder.core.validation.StructureValidator;
import com.ultimateautobuilder.network.BuildRequestPayload;
import com.ultimateautobuilder.network.StatePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Paramètres de placement partagés par le menu et l'aperçu en monde : modèle, taille, rotation,
 * miroir, mode, position. L'aperçu ne pose aucun bloc ; seule la confirmation envoie une demande
 * au serveur, qui revalide tout.
 */
public final class PlacementController {
    public enum PositionMode {
        FRONT("Devant moi"), FEET("Ma position"), COORDS("Coordonnées"), SELECTION("Zone sélectionnée"), CROSSHAIR("Bloc visé");

        public final String label;

        PositionMode(String label) {
            this.label = label;
        }
    }

    /** Réglages courants (modifiés par le menu). */
    public static final class Settings {
        public ModelEntry entry;
        public SizePreset preset = SizePreset.NORMAL;
        public int customSize = 20;
        public int rotationOffset;
        public boolean mirror;
        public BuildMode mode = BuildMode.NORMAL;
        public boolean hollow = true;
        public PositionMode position = PositionMode.FRONT;
        public int distance = 6;
        public BlockPos coords;
        public int offX, offY, offZ;
        /** Pas à pas : 0 automatique, 1 couche par couche, 2 par étapes. */
        public int stepMode;
        public boolean slow;
        public boolean emptyOnly;

        public int size() {
            return entry == null ? 0 : entry.sizes().resolve(preset, customSize);
        }
    }

    public static final Settings SETTINGS = new Settings();
    private static boolean active;
    private static BlockPos frozenOrigin;

    private PlacementController() {
    }

    public static boolean active() {
        return active && SETTINGS.entry != null;
    }

    /** Démarre l'aperçu fantôme en monde. */
    public static void start() {
        if (SETTINGS.entry == null) {
            return;
        }
        active = true;
        frozenOrigin = null;
        ClientModels.request(SETTINGS.entry, SETTINGS.size(), SETTINGS.hollow);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.ultimateautobuilder.preview_help").withStyle(ChatFormatting.AQUA), false);
        }
    }

    public static void stop() {
        active = false;
        frozenOrigin = null;
    }

    public static VoxelModel model() {
        return SETTINGS.entry == null ? null : ClientModels.now(SETTINGS.entry, SETTINGS.size(), SETTINGS.hollow);
    }

    /** Rotation effective : orientation vers le joueur (mode « devant moi ») + décalage choisi. */
    public static int rotation(Minecraft mc) {
        int base = 0;
        if (mc.player != null && SETTINGS.position == PositionMode.FRONT) {
            base = Placement.rotationFacingViewer(mc.player.getDirection().getName());
        }
        return Math.floorMod(base + SETTINGS.rotationOffset, 4);
    }

    /** Point d'ancrage (bloc de sol sous le centre de la structure). */
    public static BlockPos origin(Minecraft mc, VoxelModel model) {
        if (frozenOrigin != null) {
            return frozenOrigin.offset(SETTINGS.offX, SETTINGS.offY, SETTINGS.offZ);
        }
        BlockPos base = baseOrigin(mc, model);
        return base == null ? null : base.offset(SETTINGS.offX, SETTINGS.offY, SETTINGS.offZ);
    }

    private static BlockPos baseOrigin(Minecraft mc, VoxelModel model) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        switch (SETTINGS.position) {
            case FEET -> {
                return mc.player.blockPosition().below();
            }
            case COORDS -> {
                return SETTINGS.coords != null ? SETTINGS.coords : mc.player.blockPosition().below();
            }
            case SELECTION -> {
                StatePayload st = ClientState.state();
                if (st == null || !st.hasCorner1() || !st.hasCorner2() || model == null) {
                    return null;
                }
                BlockPos a = BlockPos.of(st.corner1()), b = BlockPos.of(st.corner2());
                BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
                int[] bounds = new Placement(model, 0, 0, 0, rotation(mc), SETTINGS.mirror).bounds();
                return new BlockPos(min.getX() - bounds[0], min.getY() - bounds[1], min.getZ() - bounds[2]);
            }
            case CROSSHAIR -> {
                HitResult hit = mc.hitResult;
                if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
                    return bhr.getBlockPos();
                }
                return null;
            }
            default -> {
                Direction dir = mc.player.getDirection();
                int half = model == null ? 4 : Math.max(model.sizeX(), model.sizeZ()) / 2;
                BlockPos p = mc.player.blockPosition().relative(dir, SETTINGS.distance + half);
                // Le client ne reçoit que les cartes de hauteur WORLD_SURFACE et MOTION_BLOCKING : on part de
                // MOTION_BLOCKING puis on descend sous le feuillage pour poser la construction au sol.
                int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()) - 1;
                BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos(p.getX(), y, p.getZ());
                int floor = mc.level.getMinBuildHeight();
                while (q.getY() > floor && mc.level.getBlockState(q).is(net.minecraft.tags.BlockTags.LEAVES)) {
                    q.move(Direction.DOWN);
                }
                if (q.getY() <= floor) {
                    // Colonne non chargée : hauteur des pieds du joueur.
                    return new BlockPos(p.getX(), mc.player.blockPosition().getY() - 1, p.getZ());
                }
                return q.immutable();
            }
        }
    }

    /** Commandes clavier et manette pendant l'aperçu en monde. */
    static void tick(Minecraft mc) {
        if (!active()) {
            KeyBindings.drain(KeyBindings.ROTATE);
            KeyBindings.drain(KeyBindings.CONFIRM);
            return;
        }
        while (KeyBindings.ROTATE.consumeClick()) {
            SETTINGS.rotationOffset = (SETTINGS.rotationOffset + 1) & 3;
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_ROTATE.get())) {
            SETTINGS.rotationOffset = (SETTINGS.rotationOffset + 1) & 3;
        }
        while (KeyBindings.MIRROR.consumeClick()) {
            SETTINGS.mirror = !SETTINGS.mirror;
        }
        Direction fwd = mc.player.getDirection();
        Direction right = fwd.getClockWise();
        while (KeyBindings.MOVE_FORWARD.consumeClick()) {
            nudge(fwd, 1);
        }
        while (KeyBindings.MOVE_BACK.consumeClick()) {
            nudge(fwd, -1);
        }
        while (KeyBindings.MOVE_RIGHT.consumeClick()) {
            nudge(right, 1);
        }
        while (KeyBindings.MOVE_LEFT.consumeClick()) {
            nudge(right, -1);
        }
        for (int d = 0; d < 4; d++) {
            if (GamepadInput.direction(d)) {
                switch (d) {
                    case 0 -> nudge(fwd, 1);
                    case 1 -> nudge(right, 1);
                    case 2 -> nudge(fwd, -1);
                    default -> nudge(right, -1);
                }
            }
        }
        while (KeyBindings.MOVE_UP.consumeClick()) {
            SETTINGS.offY++;
        }
        while (KeyBindings.MOVE_DOWN.consumeClick()) {
            SETTINGS.offY--;
        }
        while (KeyBindings.SIZE_UP.consumeClick()) {
            changeSize(1);
        }
        while (KeyBindings.SIZE_DOWN.consumeClick()) {
            changeSize(-1);
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_SIZE.get())) {
            changeSize(1);
        }
        while (KeyBindings.RECENTER.consumeClick()) {
            SETTINGS.offX = SETTINGS.offY = SETTINGS.offZ = 0;
            frozenOrigin = null;
        }
        boolean cancel = false;
        while (KeyBindings.CANCEL_PREVIEW.consumeClick()) {
            cancel = true;
        }
        if (cancel || GamepadInput.pressed(ClientConfig.GAMEPAD_BACK.get())) {
            stop();
            mc.player.displayClientMessage(Component.translatable("message.ultimateautobuilder.preview_cancelled"), true);
            return;
        }
        boolean confirm = false;
        while (KeyBindings.CONFIRM.consumeClick()) {
            confirm = true;
        }
        if (confirm || GamepadInput.pressed(ClientConfig.GAMEPAD_BUILD.get()) || GamepadInput.pressed(ClientConfig.GAMEPAD_SELECT.get())) {
            requestBuild(mc, null);
        }
    }

    private static void nudge(Direction dir, int amount) {
        SETTINGS.offX += dir.getStepX() * amount;
        SETTINGS.offZ += dir.getStepZ() * amount;
    }

    private static void changeSize(int delta) {
        SizePreset[] presets = {SizePreset.SMALL, SizePreset.NORMAL, SizePreset.LARGE, SizePreset.GIANT};
        int i = Math.max(0, java.util.Arrays.asList(presets).indexOf(SETTINGS.preset));
        SETTINGS.preset = presets[Math.max(0, Math.min(3, i + delta))];
        if (SETTINGS.entry != null) {
            ClientModels.request(SETTINGS.entry, SETTINGS.size(), SETTINGS.hollow);
        }
    }

    /**
     * Vérifie puis envoie la demande. Une construction géante passe par une confirmation explicite.
     *
     * @param returnScreen écran à rouvrir si l'utilisateur refuse la confirmation (null = jeu)
     */
    public static void requestBuild(Minecraft mc, net.minecraft.client.gui.screens.Screen returnScreen) {
        if (SETTINGS.entry == null || mc.player == null) {
            return;
        }
        VoxelModel model = model();
        if (model == null) {
            mc.player.displayClientMessage(Component.translatable("message.ultimateautobuilder.computing"), true);
            return;
        }
        BlockPos origin = origin(mc, model);
        if (origin == null) {
            mc.player.displayClientMessage(Component.translatable("message.ultimateautobuilder.no_position").withStyle(ChatFormatting.RED), false);
            return;
        }
        StatePayload st = ClientState.state();
        int minY = st != null ? st.minY() : mc.level.getMinBuildHeight();
        int maxY = st != null ? st.maxY() : mc.level.getMaxBuildHeight();
        Limits limits = st != null ? new Limits(Math.max(1, st.maxBlocks()), 4095, Math.max(1, st.giantThreshold()), 4096) : Limits.DEFAULT;
        Placement pl = new Placement(model, origin.getX(), origin.getY() + aerialLift(st), origin.getZ(), rotation(mc), SETTINGS.mirror);
        var report = StructureValidator.validate(pl, limits, minY, maxY);
        if (report.hasErrors()) {
            mc.player.displayClientMessage(Component.literal(report.summary()).withStyle(ChatFormatting.RED), false);
            return;
        }
        if (report.needsConfirmation()) {
            mc.setScreen(new ConfirmScreen(yes -> {
                if (yes) {
                    send(mc, origin, true);
                    mc.setScreen(null);
                } else {
                    mc.setScreen(returnScreen);
                }
            }, Component.translatable("screen.ultimateautobuilder.giant_title"),
                    Component.translatable("screen.ultimateautobuilder.giant_message", model.size(), model.sizeX(), model.sizeY(), model.sizeZ())));
            return;
        }
        send(mc, origin, false);
        if (mc.screen != null) {
            mc.setScreen(null);
        }
    }

    private static int aerialLift(StatePayload st) {
        if (SETTINGS.mode != BuildMode.AERIAL || st == null) {
            return 0;
        }
        try {
            return Integer.parseInt(st.settings().getOrDefault("aerialHeight", "20"));
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    private static void send(Minecraft mc, BlockPos origin, boolean confirmed) {
        Settings s = SETTINGS;
        PacketDistributor.sendToServer(new BuildRequestPayload(s.entry.id(), s.preset.ordinal(), s.customSize, rotation(mc), s.mirror, origin.getX(),
                origin.getY(), origin.getZ(), s.mode.ordinal(), s.hollow, confirmed, s.stepMode, s.slow, s.emptyOnly));
        ClientData.addRecent(s.entry.id());
        ClientData.Data d = ClientData.get();
        d.preferredPreset = s.preset.ordinal();
        d.preferredRotationOffset = s.rotationOffset;
        d.preferredMode = s.mode.ordinal();
        d.positionMode = s.position.ordinal();
        d.distance = s.distance;
        d.hollow = s.hollow;
        ClientData.save();
        stop();
    }

    /** Fige la position courante (le fantôme ne suit plus le regard). */
    public static void freeze(Minecraft mc) {
        VoxelModel m = model();
        BlockPos o = baseOrigin(mc, m);
        if (o != null) {
            frozenOrigin = o;
        }
    }

    public static int aerialLiftForPreview() {
        return aerialLift(ClientState.state());
    }
}
