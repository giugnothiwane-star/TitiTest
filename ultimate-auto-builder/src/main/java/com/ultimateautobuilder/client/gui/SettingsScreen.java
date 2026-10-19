package com.ultimateautobuilder.client.gui;

import com.ultimateautobuilder.client.ClientState;
import com.ultimateautobuilder.client.GamepadInput;
import com.ultimateautobuilder.config.ClientConfig;
import com.ultimateautobuilder.config.ServerConfig;
import com.ultimateautobuilder.network.ActionPayload;
import com.ultimateautobuilder.network.StatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paramètres : préférences du joueur (fichier client) et, pour les administrateurs, réglages du
 * serveur. Ces derniers sont envoyés au serveur, qui vérifie les droits et les bornes avant d'appliquer.
 */
public final class SettingsScreen extends Screen {
    private static final int ROW_H = 22;

    /** Libellés français des réglages serveur modifiables. */
    private static final Map<String, String> SERVER_LABELS = new LinkedHashMap<>();

    static {
        SERVER_LABELS.put("speedMode", "Vitesse de construction");
        SERVER_LABELS.put("customBlocksPerTick", "Blocs par tick (personnalisé)");
        SERVER_LABELS.put("tickBudgetMs", "Budget par tick (ms)");
        SERVER_LABELS.put("adaptiveThrottle", "Ralentissement adaptatif");
        SERVER_LABELS.put("maxBlocksPerBuild", "Blocs max par construction");
        SERVER_LABELS.put("maxDimension", "Dimension max (blocs)");
        SERVER_LABELS.put("giantThreshold", "Seuil de confirmation (blocs)");
        SERVER_LABELS.put("maxActiveBuilds", "Constructions simultanées");
        SERVER_LABELS.put("maxActiveBuildsPerPlayer", "Constructions par joueur");
        SERVER_LABELS.put("freeBuild", "Construction gratuite");
        SERVER_LABELS.put("replaceExisting", "Remplacer les blocs existants");
        SERVER_LABELS.put("protectValuableBlocks", "Protéger les blocs précieux");
        SERVER_LABELS.put("fluidMode", "Liquides existants");
        SERVER_LABELS.put("respectProtectionEvents", "Respecter les protections");
        SERVER_LABELS.put("particles", "Particules de construction");
        SERVER_LABELS.put("hollowLargeStatues", "Statues géantes creuses");
        SERVER_LABELS.put("aerialHeight", "Hauteur du mode aérien");
        SERVER_LABELS.put("restrictToSelection", "Limiter à la zone sélectionnée");
        SERVER_LABELS.put("historySize", "Taille de l'historique");
        SERVER_LABELS.put("prudentUndo", "Retour arrière prudent");
    }

    private interface Row {
        String label();

        void build(int x, int y, int w);
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private final List<AbstractWidget> navigable = new ArrayList<>();
    private int page;
    private int perPage;
    private int focus;
    private long stateVersion = -1;
    private String feedback = "";

    public SettingsScreen(Screen parent) {
        super(Component.literal("Paramètres — Ultimate Auto Builder"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ lignes

    private void collectRows() {
        rows.clear();
        if (ClientConfig.loaded()) {
            rows.add(header("Joueur"));
            rows.add(toggle("Cristal requis pour la touche/R1", ClientConfig.REQUIRE_CRYSTAL));
            rows.add(cycle("Main du cristal", ClientConfig.CRYSTAL_HAND, ClientConfig.CrystalHand.values(),
                    v -> v == ClientConfig.CrystalHand.MAIN_HAND ? "principale" : "les deux"));
            rows.add(toggle("Clic droit ouvre le menu", ClientConfig.RIGHT_CLICK_OPENS));
            rows.add(toggle("Particules du cristal", ClientConfig.CRYSTAL_PARTICLES));
            rows.add(toggle("Manette (GLFW)", ClientConfig.GAMEPAD_ENABLED));
            rows.add(cycle("Bouton d'ouverture", ClientConfig.GAMEPAD_OPEN, ClientConfig.GamepadButton.values(), SettingsScreen::buttonName));
            rows.add(toggle("Aperçu fantôme", ClientConfig.GHOST_PREVIEW));
            rows.add(intCycle("Faces max de l'aperçu", ClientConfig.GHOST_MAX_FACES, new int[]{10_000, 20_000, 40_000, 80_000, 160_000}));
            rows.add(toggle("Barre de progression", ClientConfig.SHOW_PROGRESS));
            rows.add(intCycle("Distance « devant moi »", ClientConfig.DEFAULT_DISTANCE, new int[]{3, 4, 6, 8, 12, 16, 24, 32}));
        }
        StatePayload st = ClientState.state();
        if (st != null && Minecraft.getInstance().player != null) {
            rows.add(header(st.admin() ? "Serveur (administrateur)" : "Serveur (lecture seule)"));
            for (Map.Entry<String, String> e : SERVER_LABELS.entrySet()) {
                String value = st.settings().get(e.getKey());
                if (value != null) {
                    rows.add(serverRow(e.getKey(), e.getValue(), value, st.admin()));
                }
            }
        }
    }

    private Row header(String title) {
        return new Row() {
            @Override
            public String label() {
                return "§b§l" + title;
            }

            @Override
            public void build(int x, int y, int w) {
            }
        };
    }

    private Row toggle(String label, ModConfigSpec.BooleanValue value) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                addNav(Button.builder(Component.literal(value.get() ? "§aOui" : "§cNon"), b -> {
                    value.set(!value.get());
                    value.save();
                    rebuildWidgets();
                }).bounds(x, y, w, 20).build());
            }
        };
    }

    private <E extends Enum<E>> Row cycle(String label, ModConfigSpec.EnumValue<E> value, E[] all, java.util.function.Function<E, String> name) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                addNav(Button.builder(Component.literal(name.apply(value.get())), b -> {
                    value.set(all[(value.get().ordinal() + 1) % all.length]);
                    value.save();
                    rebuildWidgets();
                }).bounds(x, y, w, 20).build());
            }
        };
    }

    private Row intCycle(String label, ModConfigSpec.IntValue value, int[] steps) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                addNav(Button.builder(Component.literal(Integer.toString(value.get())), b -> {
                    int cur = value.get();
                    int next = steps[0];
                    for (int s : steps) {
                        if (s > cur) {
                            next = s;
                            break;
                        }
                    }
                    value.set(next);
                    value.save();
                    rebuildWidgets();
                }).bounds(x, y, w, 20).build());
            }
        };
    }

    private Row serverRow(String key, String label, String value, boolean editable) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                Button.OnPress send;
                String shown = value;
                if ("true".equals(value) || "false".equals(value)) {
                    boolean b = Boolean.parseBoolean(value);
                    shown = b ? "§aOui" : "§cNon";
                    send = btn -> sendSetting(key, Boolean.toString(!b));
                } else if (key.equals("speedMode")) {
                    send = btn -> sendSetting(key, nextEnum(ServerConfig.SpeedMode.values(), value));
                } else if (key.equals("fluidMode")) {
                    shown = "REPLACE".equals(value) ? "remplacer" : "conserver";
                    send = btn -> sendSetting(key, nextEnum(ServerConfig.FluidMode.values(), value));
                } else {
                    // Valeur numérique : champ texte + validation, bornes vérifiées par le serveur.
                    EditBox box = new EditBox(font, x + 1, y + 1, w - 34, 18, Component.literal(label));
                    box.setFilter(t -> t.isEmpty() || t.matches("\\d{1,9}"));
                    box.setValue(value);
                    box.setEditable(editable);
                    addNav(box);
                    Button ok = Button.builder(Component.literal("OK"), btn -> {
                        if (!box.getValue().isEmpty()) {
                            sendSetting(key, box.getValue());
                        }
                    }).bounds(x + w - 30, y, 30, 20).build();
                    ok.active = editable;
                    addNav(ok);
                    return;
                }
                Button b = Button.builder(Component.literal(shown), send).bounds(x, y, w, 20).build();
                b.active = editable;
                if (!editable) {
                    b.setTooltip(Tooltip.create(Component.literal("Réservé aux administrateurs du serveur")));
                }
                addNav(b);
            }
        };
    }

    private static <E extends Enum<E>> String nextEnum(E[] all, String current) {
        for (int i = 0; i < all.length; i++) {
            if (all[i].name().equals(current)) {
                return all[(i + 1) % all.length].name();
            }
        }
        return all[0].name();
    }

    private void sendSetting(String key, String value) {
        PacketDistributor.sendToServer(new ActionPayload(ActionPayload.SET_SETTING, 0, key + "=" + value));
        feedback = "Envoyé au serveur : " + SERVER_LABELS.getOrDefault(key, key) + " = " + value;
    }

    private static String buttonName(ClientConfig.GamepadButton b) {
        return switch (b) {
            case RIGHT_BUMPER -> "R1 / RB";
            case LEFT_BUMPER -> "L1 / LB";
            case A -> "Croix / A";
            case B -> "Rond / B";
            case X -> "Carré / X";
            case Y -> "Triangle / Y";
            case START -> "Options / Start";
            case BACK -> "Share / Back";
            case GUIDE -> "PS / Guide";
            case LEFT_THUMB -> "L3";
            case RIGHT_THUMB -> "R3";
            case DPAD_UP -> "Croix haut";
            case DPAD_RIGHT -> "Croix droite";
            case DPAD_DOWN -> "Croix bas";
            case DPAD_LEFT -> "Croix gauche";
        };
    }

    private <T extends AbstractWidget> T addNav(T w) {
        navigable.add(w);
        return addRenderableWidget(w);
    }

    // ------------------------------------------------------------------ écran

    @Override
    protected void init() {
        navigable.clear();
        collectRows();
        stateVersion = ClientState.version();
        int top = 30;
        int bottom = height - 30;
        perPage = Math.max(1, (bottom - top) / ROW_H);
        int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int contentW = Math.min(420, width - 16);
        int x0 = (width - contentW) / 2;
        int controlW = Math.min(150, contentW / 2);
        int cx = x0 + contentW - controlW;
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= rows.size()) {
                break;
            }
            rows.get(idx).build(cx, top + i * ROW_H, controlW);
        }
        int by = height - 24;
        if (pages > 1) {
            Button prev = Button.builder(Component.literal("◀"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(width / 2 - 130, by, 20, 20).build();
            prev.active = page > 0;
            addNav(prev);
            Button next = Button.builder(Component.literal("▶"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(width / 2 + 110, by, 20, 20).build();
            next.active = page < pages - 1;
            addNav(next);
        }
        addNav(Button.builder(Component.literal("Terminé"), b -> onClose()).bounds(width / 2 - 100, by, 200, 20).build());
        focus = Math.max(0, Math.min(focus, navigable.size() - 1));
        if (GamepadInput.available() && !navigable.isEmpty()) {
            setFocused(navigable.get(focus));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (ClientState.version() != stateVersion) {
            rebuildWidgets();
        }
        // Navigation à la manette (le relais de ClientTicker ne vise que le menu principal).
        if (!GamepadInput.available() || navigable.isEmpty()) {
            return;
        }
        if (GamepadInput.direction(0) || GamepadInput.direction(3)) {
            focus = Math.floorMod(focus - 1, navigable.size());
            setFocused(navigable.get(focus));
        }
        if (GamepadInput.direction(2) || GamepadInput.direction(1)) {
            focus = Math.floorMod(focus + 1, navigable.size());
            setFocused(navigable.get(focus));
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_SELECT.get()) && navigable.get(focus) instanceof AbstractButton b && b.active) {
            int keep = focus;
            b.onPress();
            focus = keep;
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_BACK.get())) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        int top = 30;
        int contentW = Math.min(420, width - 16);
        int x0 = (width - contentW) / 2;
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= rows.size()) {
                break;
            }
            g.drawString(font, font.plainSubstrByWidth(rows.get(idx).label(), contentW - Math.min(150, contentW / 2) - 6), x0, top + i * ROW_H + 6,
                    0xFFE0E0E0, false);
        }
        if (!feedback.isEmpty()) {
            g.drawCenteredString(font, feedback, width / 2, height - 36, 0xFF7FE0FF);
        }
        if (ClientState.state() == null) {
            g.drawCenteredString(font, "Réglages serveur visibles une fois connecté à un monde", width / 2, height - 46, 0xFF888888);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
