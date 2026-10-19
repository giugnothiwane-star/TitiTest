package com.ultimateautobuilder.config;

import com.ultimateautobuilder.core.validation.Limits;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration côté serveur (fichier serverconfig/ultimateautobuilder-server.toml du monde).
 * Synchronisée vers les clients ; seule la valeur du serveur fait foi.
 */
public final class ServerConfig {
    public enum SpeedMode {
        CAUTIOUS(128), BALANCED(512), FAST(1024), CUSTOM(-1);

        public final int blocksPerTick;

        SpeedMode(int blocksPerTick) {
            this.blocksPerTick = blocksPerTick;
        }
    }

    public enum FluidMode { REPLACE, KEEP }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<SpeedMode> SPEED_MODE;
    public static final ModConfigSpec.IntValue CUSTOM_BLOCKS_PER_TICK;
    public static final ModConfigSpec.IntValue TICK_BUDGET_MS;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_THROTTLE;
    public static final ModConfigSpec.IntValue TARGET_MSPT;

    public static final ModConfigSpec.IntValue MAX_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_DIMENSION;
    public static final ModConfigSpec.IntValue GIANT_THRESHOLD;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_JOBS;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_JOBS_PER_PLAYER;
    public static final ModConfigSpec.IntValue MAX_BUILD_DISTANCE;
    public static final ModConfigSpec.IntValue REQUEST_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue MAX_IMPORT_KB;

    public static final ModConfigSpec.BooleanValue FREE_BUILD;
    public static final ModConfigSpec.BooleanValue REPLACE_EXISTING;
    public static final ModConfigSpec.BooleanValue PROTECT_VALUABLE;
    public static final ModConfigSpec.ConfigValue<String> PROTECTED_BLOCKS;
    public static final ModConfigSpec.EnumValue<FluidMode> FLUID_MODE;
    public static final ModConfigSpec.BooleanValue RESPECT_PROTECTION_EVENTS;
    public static final ModConfigSpec.BooleanValue PARTICLES;
    public static final ModConfigSpec.BooleanValue HOLLOW_STATUES;
    public static final ModConfigSpec.IntValue AERIAL_HEIGHT;
    public static final ModConfigSpec.IntValue FOUNDATION_MAX_DEPTH;
    public static final ModConfigSpec.BooleanValue RESTRICT_TO_SELECTION;

    public static final ModConfigSpec.IntValue PERMISSION_LEVEL;
    public static final ModConfigSpec.BooleanValue SINGLEPLAYER_ALWAYS_ALLOWED;
    public static final ModConfigSpec.BooleanValue GIVE_CRYSTAL_ON_FIRST_JOIN;

    public static final ModConfigSpec.EnumValue<ProgressionMode> PROGRESSION_MODE;
    public static final ModConfigSpec.IntValue HISTORY_SIZE;
    public static final ModConfigSpec.BooleanValue UNDO_PRUDENT;
    public static final ModConfigSpec.IntValue CHECKPOINT_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Performances du moteur de construction progressive").push("performance");
        SPEED_MODE = b.comment("CAUTIOUS = 128 blocs/tick, BALANCED = 512, FAST = 1024, CUSTOM = valeur customBlocksPerTick")
                .defineEnum("speedMode", SpeedMode.BALANCED);
        CUSTOM_BLOCKS_PER_TICK = b.defineInRange("customBlocksPerTick", 512, 16, 16384);
        TICK_BUDGET_MS = b.comment("Temps maximal consacré aux constructions par tick serveur (ms)").defineInRange("tickBudgetMs", 15, 1, 40);
        ADAPTIVE_THROTTLE = b.comment("Réduit automatiquement la cadence quand le serveur est chargé").define("adaptiveThrottle", true);
        TARGET_MSPT = b.comment("Durée de tick visée (ms) par le régulateur adaptatif").defineInRange("targetMspt", 40, 20, 50);
        b.pop();

        b.comment("Limites de sécurité").push("limits");
        MAX_BLOCKS = b.defineInRange("maxBlocksPerBuild", 2_000_000, 1_000, 20_000_000);
        MAX_DIMENSION = b.defineInRange("maxDimension", 512, 16, 4095);
        GIANT_THRESHOLD = b.comment("Au-delà de ce nombre de blocs, une confirmation explicite est exigée")
                .defineInRange("giantThreshold", 150_000, 1_000, 20_000_000);
        MAX_ACTIVE_JOBS = b.defineInRange("maxActiveBuilds", 3, 1, 32);
        MAX_ACTIVE_JOBS_PER_PLAYER = b.defineInRange("maxActiveBuildsPerPlayer", 1, 1, 16);
        MAX_BUILD_DISTANCE = b.comment("Distance horizontale maximale entre le joueur et le point de construction")
                .defineInRange("maxBuildDistance", 512, 16, 30_000);
        REQUEST_COOLDOWN_TICKS = b.defineInRange("requestCooldownTicks", 20, 0, 1200);
        MAX_IMPORT_KB = b.defineInRange("maxImportKilobytes", 8192, 16, 65536);
        b.pop();

        b.comment("Règles de construction").push("building");
        FREE_BUILD = b.comment("Construction gratuite : aucun matériau n'est retiré de l'inventaire").define("freeBuild", true);
        REPLACE_EXISTING = b.comment("Remplacer les blocs existants (sinon seuls l'air et les blocs remplaçables le sont)")
                .define("replaceExisting", true);
        PROTECT_VALUABLE = b.define("protectValuableBlocks", true);
        PROTECTED_BLOCKS = b.comment("Blocs jamais remplacés (séparés par des virgules)")
                .define("protectedBlocks", "minecraft:beacon,minecraft:spawner,minecraft:trial_spawner,minecraft:vault,minecraft:diamond_block,"
                        + "minecraft:netherite_block,minecraft:emerald_block,minecraft:ancient_debris,minecraft:dragon_egg,"
                        + "minecraft:end_portal_frame,minecraft:respawn_anchor,minecraft:conduit,minecraft:shulker_box");
        FLUID_MODE = b.comment("REPLACE : les fluides sont remplacés ; KEEP : les cases contenant un fluide sont conservées")
                .defineEnum("fluidMode", FluidMode.REPLACE);
        RESPECT_PROTECTION_EVENTS = b.comment("Interroge les mods de protection (événements de pose/casse) avant chaque modification")
                .define("respectProtectionEvents", true);
        PARTICLES = b.define("particles", true);
        HOLLOW_STATUES = b.comment("Évide l'intérieur des statues et créatures de grande taille").define("hollowLargeStatues", true);
        AERIAL_HEIGHT = b.defineInRange("aerialHeight", 20, 4, 256);
        FOUNDATION_MAX_DEPTH = b.defineInRange("foundationMaxDepth", 24, 0, 128);
        RESTRICT_TO_SELECTION = b.comment("N'autorise que les constructions entièrement contenues dans la zone sélectionnée du joueur")
                .define("restrictToSelection", false);
        b.pop();

        b.comment("Permissions").push("permissions");
        PERMISSION_LEVEL = b.comment("Niveau d'opérateur requis pour construire (0 = tout le monde)").defineInRange("permissionLevel", 2, 0, 4);
        SINGLEPLAYER_ALWAYS_ALLOWED = b.define("singleplayerAlwaysAllowed", true);
        GIVE_CRYSTAL_ON_FIRST_JOIN = b.define("giveCrystalOnFirstJoin", true);
        b.pop();

        b.comment("Progression : niveaux d'architecte, quêtes et déblocage des constructions par matériaux").push("progression");
        PROGRESSION_MODE = b.comment("CREATIVE_FREE : progression en survie, tout est accessible en créatif ; ALWAYS : progression pour tous ; "
                + "OFF : toutes les constructions sont accessibles").defineEnum("progressionMode", ProgressionMode.CREATIVE_FREE);
        b.pop();

        b.comment("Historique et annulation").push("history");
        HISTORY_SIZE = b.defineInRange("historySize", 20, 1, 200);
        UNDO_PRUDENT = b.comment("Retour arrière prudent : un bloc modifié depuis la construction n'est pas restauré")
                .define("prudentUndo", true);
        CHECKPOINT_SECONDS = b.comment("Sauvegarde périodique des constructions en cours (secondes)").defineInRange("checkpointSeconds", 30, 5, 600);
        b.pop();
        SPEC = b.build();
    }

    private ServerConfig() {
    }

    /** Application de la progression. */
    public enum ProgressionMode { CREATIVE_FREE, ALWAYS, OFF }

    public static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static int blocksPerTick() {
        if (!loaded()) {
            return SpeedMode.BALANCED.blocksPerTick;
        }
        SpeedMode m = SPEED_MODE.get();
        return m == SpeedMode.CUSTOM ? CUSTOM_BLOCKS_PER_TICK.get() : m.blocksPerTick;
    }

    public static Limits limits() {
        if (!loaded()) {
            return Limits.DEFAULT;
        }
        return new Limits(MAX_BLOCKS.get(), MAX_DIMENSION.get(), GIANT_THRESHOLD.get(), 4096);
    }

    public static Set<String> protectedBlocks() {
        return Arrays.stream(PROTECTED_BLOCKS.get().split(",")).map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    /** Valeurs modifiables depuis le menu de paramètres (clé → valeur). */
    public static Map<String, ModConfigSpec.ConfigValue<?>> editable() {
        Map<String, ModConfigSpec.ConfigValue<?>> m = new LinkedHashMap<>();
        m.put("speedMode", SPEED_MODE);
        m.put("customBlocksPerTick", CUSTOM_BLOCKS_PER_TICK);
        m.put("tickBudgetMs", TICK_BUDGET_MS);
        m.put("adaptiveThrottle", ADAPTIVE_THROTTLE);
        m.put("maxBlocksPerBuild", MAX_BLOCKS);
        m.put("maxDimension", MAX_DIMENSION);
        m.put("giantThreshold", GIANT_THRESHOLD);
        m.put("maxActiveBuilds", MAX_ACTIVE_JOBS);
        m.put("maxActiveBuildsPerPlayer", MAX_ACTIVE_JOBS_PER_PLAYER);
        m.put("freeBuild", FREE_BUILD);
        m.put("replaceExisting", REPLACE_EXISTING);
        m.put("protectValuableBlocks", PROTECT_VALUABLE);
        m.put("fluidMode", FLUID_MODE);
        m.put("respectProtectionEvents", RESPECT_PROTECTION_EVENTS);
        m.put("particles", PARTICLES);
        m.put("hollowLargeStatues", HOLLOW_STATUES);
        m.put("aerialHeight", AERIAL_HEIGHT);
        m.put("restrictToSelection", RESTRICT_TO_SELECTION);
        m.put("progressionMode", PROGRESSION_MODE);
        m.put("historySize", HISTORY_SIZE);
        m.put("prudentUndo", UNDO_PRUDENT);
        return m;
    }

    /**
     * Applique une valeur textuelle en respectant le type et les bornes de l'entrée.
     *
     * @return message d'erreur, ou null si la valeur a été appliquée
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String apply(String key, String raw) {
        ModConfigSpec.ConfigValue<?> v = editable().get(key);
        if (v == null) {
            return "Paramètre inconnu : " + key;
        }
        try {
            if (v instanceof ModConfigSpec.IntValue iv) {
                int n = Integer.parseInt(raw.trim());
                Object spec = SPEC.getSpec().get(iv.getPath());
                if (spec instanceof ModConfigSpec.ValueSpec vs && !vs.test(n)) {
                    return "Valeur hors limites pour " + key;
                }
                iv.set(n);
            } else if (v instanceof ModConfigSpec.BooleanValue bv) {
                bv.set(Boolean.parseBoolean(raw.trim()));
            } else if (v instanceof ModConfigSpec.EnumValue ev) {
                Class<? extends Enum> cls = ((Enum) ev.getDefault()).getDeclaringClass();
                ev.set(Enum.valueOf(cls, raw.trim().toUpperCase(Locale.ROOT)));
            } else {
                return "Type non modifiable";
            }
            v.save();
            return null;
        } catch (IllegalArgumentException e) {
            return "Valeur invalide pour " + key + " : " + raw;
        }
    }

    /** Instantané des valeurs pour l'interface (clé → valeur textuelle). */
    public static Map<String, String> snapshot() {
        Map<String, String> m = new LinkedHashMap<>();
        if (!loaded()) {
            return m;
        }
        editable().forEach((k, v) -> m.put(k, String.valueOf(v.get())));
        return m;
    }
}
