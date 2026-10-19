package com.ultimateautobuilder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ultimateautobuilder.builder.BuildManager;
import com.ultimateautobuilder.builder.BuildMode;
import com.ultimateautobuilder.builder.BuildRequest;
import com.ultimateautobuilder.builder.Job;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.model.VoxelModel;
import com.ultimateautobuilder.core.placement.Placement;
import com.ultimateautobuilder.importer.ModelImporter;
import com.ultimateautobuilder.network.ServerPayloadHandler;
import com.ultimateautobuilder.network.SimplePayloads;
import com.ultimateautobuilder.permission.Permissions;
import com.ultimateautobuilder.registry.ModItems;
import com.ultimateautobuilder.selection.Selections;
import com.ultimateautobuilder.storage.CustomModelStore;
import com.ultimateautobuilder.storage.HistoryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Commande /uab (alias /ultimateautobuilder) : remise du cristal, constructions, annulation,
 * historique, sélection, enregistrement et importation de modèles.
 */
public final class UabCommand {
    private UabCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("uab")
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .executes(c -> give(c.getSource(), List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> give(c.getSource(), EntityArgument.getPlayers(c, "targets")))))
                .then(Commands.literal("menu").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerPayloadHandler.sendState(p);
                    PacketDistributor.sendToPlayer(p, new SimplePayloads.OpenMenu());
                    return 1;
                }))
                .then(Commands.literal("build")
                        .then(Commands.argument("model", StringArgumentType.string())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(DefaultCatalog.shared().all().stream()
                                        .map(e -> e.id().contains(":") ? "\"" + e.id() + "\"" : e.id()), b))
                                .executes(c -> build(c, "normal", false))
                                .then(Commands.argument("size", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("petit", "normal", "grand", "geant"), b))
                                        .executes(c -> build(c, StringArgumentType.getString(c, "size"), false))
                                        .then(Commands.argument("confirm", BoolArgumentType.bool())
                                                .executes(c -> build(c, StringArgumentType.getString(c, "size"), BoolArgumentType.getBool(c, "confirm")))))))
                .then(Commands.literal("cancel")
                        .executes(c -> cancel(c.getSource(), -1))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> cancel(c.getSource(), IntegerArgumentType.getInteger(c, "id")))))
                .then(Commands.literal("undo")
                        .executes(c -> undo(c.getSource(), -1))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> undo(c.getSource(), IntegerArgumentType.getInteger(c, "id")))))
                .then(Commands.literal("history").executes(c -> history(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("list")
                        .executes(c -> list(c.getSource(), null))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Category.values()).map(Category::key), b))
                                .executes(c -> list(c.getSource(), StringArgumentType.getString(c, "category")))))
                .then(Commands.literal("pos1").executes(c -> corner(c.getSource(), 1)))
                .then(Commands.literal("pos2").executes(c -> corner(c.getSource(), 2)))
                .then(Commands.literal("save").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> save(c))))
                .then(Commands.literal("import")
                        .then(Commands.argument("file", StringArgumentType.string())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(CustomModelStore.importableFiles().stream().map(f -> "\"" + f + "\""), b))
                                .executes(c -> importFile(c, null))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(c -> importFile(c, StringArgumentType.getString(c, "name"))))))
                .then(Commands.literal("delete").then(Commands.argument("id", StringArgumentType.string())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(CustomModelStore.list().stream().map(i -> "\"" + i.id() + "\""), b))
                        .executes(c -> delete(c.getSource(), StringArgumentType.getString(c, "id")))));
        var node = dispatcher.register(root);
        dispatcher.register(Commands.literal("ultimateautobuilder").redirect(node));
    }

    private static int give(CommandSourceStack src, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            ItemStack stack = new ItemStack(ModItems.ICE_CRYSTAL.get());
            if (!p.getInventory().add(stack)) {
                p.drop(stack, false);
            }
        }
        src.sendSuccess(() -> Component.literal("Cristal de Glace donné à " + targets.size() + " joueur(s)"), true);
        return targets.size();
    }

    private static SizePreset parsePreset(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "petit", "small" -> SizePreset.SMALL;
            case "grand", "large" -> SizePreset.LARGE;
            case "geant", "géant", "giant" -> SizePreset.GIANT;
            default -> SizePreset.NORMAL;
        };
    }

    private static int build(CommandContext<CommandSourceStack> c, String size, boolean confirm) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerPlayer player = src.getPlayerOrException();
        if (!Permissions.canBuild(player)) {
            src.sendFailure(Component.literal("Permission refusée"));
            return 0;
        }
        String id = StringArgumentType.getString(c, "model");
        ModelEntry entry = DefaultCatalog.shared().get(id);
        if (entry == null) {
            src.sendFailure(Component.literal("Modèle inconnu : " + id));
            return 0;
        }
        String facing = player.getDirection().getName();
        int rotation = Placement.rotationFacingViewer(facing);
        int ahead = Math.max(4, entry.sizes().resolve(parsePreset(size), 0) / 2 + 3);
        BlockPos ground = player.blockPosition().below().relative(player.getDirection(), ahead);
        BuildRequest req = new BuildRequest(id, parsePreset(size), 0, rotation, false, ground, player.level().dimension().location(),
                BuildMode.NORMAL, true, confirm);
        try {
            Job j = BuildManager.get().submit(player, player.serverLevel(), req);
            src.sendSuccess(() -> Component.literal("Construction n° " + j.id() + " lancée : " + entry.name()).withStyle(ChatFormatting.AQUA), false);
            return 1;
        } catch (IllegalStateException e) {
            src.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int cancel(CommandSourceStack src, int id) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayer();
        boolean admin = p == null || Permissions.isAdmin(p);
        Job j = BuildManager.get().cancel(p, id, admin);
        if (j == null) {
            src.sendFailure(Component.literal("Aucune construction en cours à annuler"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Annulation demandée : " + j.title()), false);
        return 1;
    }

    private static int undo(CommandSourceStack src, int id) {
        ServerPlayer p = src.getPlayer();
        if (p != null && !Permissions.canBuild(p)) {
            src.sendFailure(Component.literal("Permission refusée"));
            return 0;
        }
        try {
            var job = BuildManager.get().undo(p, src.getLevel(), id, p == null || Permissions.isAdmin(p));
            src.sendSuccess(() -> Component.literal("Retour arrière lancé (" + job.total() + " blocs à restaurer)"), false);
            return 1;
        } catch (IllegalStateException e) {
            src.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int history(CommandSourceStack src) {
        List<HistoryEntry> entries = BuildManager.get().history().snapshot();
        if (entries.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Historique vide"), false);
            return 0;
        }
        for (HistoryEntry e : entries.subList(0, Math.min(10, entries.size()))) {
            String line = "#" + e.id + " " + e.modelName + " — " + e.status + " — " + e.blocks + " blocs — (" + e.bounds[0] + ", " + e.bounds[1]
                    + ", " + e.bounds[2] + ") — " + e.ownerName + (e.undoAvailable ? " — annulable" : "");
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return entries.size();
    }

    private static int status(CommandSourceStack src) {
        BuildManager m = BuildManager.get();
        List<Job> jobs = m.jobs();
        src.sendSuccess(() -> Component.literal(jobs.size() + " construction(s) active(s), cadence " + Math.round(m.throttle() * 100) + " %, "
                + ServerConfig.blocksPerTick() + " blocs/tick max"), false);
        for (Job j : jobs) {
            src.sendSuccess(() -> Component.literal("#" + j.id() + " " + j.title() + " : " + j.state() + " " + j.progress() + "/" + j.total()), false);
        }
        return jobs.size();
    }

    private static int list(CommandSourceStack src, String cat) {
        Category c = cat == null ? null : Category.byKey(cat);
        if (cat != null && c == null) {
            src.sendFailure(Component.literal("Catégorie inconnue : " + cat));
            return 0;
        }
        if (c == null) {
            for (Category k : Category.values()) {
                int n = DefaultCatalog.shared().count(k);
                src.sendSuccess(() -> Component.literal(k.key() + " : " + k.displayName() + " (" + n + ")"), false);
            }
            src.sendSuccess(() -> Component.literal("Total : " + DefaultCatalog.shared().size() + " modèles"), false);
            return DefaultCatalog.shared().size();
        }
        StringBuilder sb = new StringBuilder();
        for (ModelEntry e : DefaultCatalog.shared().byCategory(c)) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(e.id());
        }
        String text = sb.toString();
        src.sendSuccess(() -> Component.literal(c.displayName() + " : " + text), false);
        return 1;
    }

    private static int corner(CommandSourceStack src, int corner) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        BlockPos pos = p.blockPosition().below();
        Selections.setCorner(p, corner, pos);
        src.sendSuccess(() -> Component.literal("Coin " + corner + " : " + pos.toShortString()), false);
        return 1;
    }

    private static int save(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerPlayer p = src.getPlayerOrException();
        if (!Permissions.canImport(p)) {
            src.sendFailure(Component.literal("Permission refusée"));
            return 0;
        }
        var sel = Selections.get(p);
        if (sel == null) {
            src.sendFailure(Component.literal("Définissez deux coins : /uab pos1 et /uab pos2, ou accroupi + clic droit avec le cristal"));
            return 0;
        }
        try {
            VoxelModel m = ModelImporter.capture(p.serverLevel(), sel.min(), sel.max(), ServerConfig.limits());
            var e = CustomModelStore.save(StringArgumentType.getString(c, "name"), m);
            src.sendSuccess(() -> Component.literal("Modèle enregistré : " + e.id() + " (" + m.size() + " blocs)"), false);
            return 1;
        } catch (ImportException | IOException | IllegalArgumentException e) {
            src.sendFailure(Component.literal("Enregistrement refusé : " + e.getMessage()));
            return 0;
        }
    }

    private static int importFile(CommandContext<CommandSourceStack> c, String name) {
        CommandSourceStack src = c.getSource();
        ServerPlayer p = src.getPlayer();
        if (p != null && !Permissions.canImport(p)) {
            src.sendFailure(Component.literal("Permission refusée"));
            return 0;
        }
        try {
            var res = ModelImporter.importFile(src.getServer(), StringArgumentType.getString(c, "file"), name);
            src.sendSuccess(() -> Component.literal("Importé : " + res.id() + " (" + res.blocks() + " blocs"
                    + (res.skippedInvalid() > 0 ? ", " + res.skippedInvalid() + " ignorés" : "") + ")"), false);
            return 1;
        } catch (ImportException e) {
            src.sendFailure(Component.literal("Import refusé : " + e.getMessage()));
            return 0;
        }
    }

    private static int delete(CommandSourceStack src, String id) {
        ServerPlayer p = src.getPlayer();
        if (p != null && !Permissions.canImport(p)) {
            src.sendFailure(Component.literal("Permission refusée"));
            return 0;
        }
        if (CustomModelStore.delete(id)) {
            src.sendSuccess(() -> Component.literal("Modèle supprimé : " + id), false);
            return 1;
        }
        src.sendFailure(Component.literal("Modèle introuvable : " + id));
        return 0;
    }
}
