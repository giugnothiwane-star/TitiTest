package com.ultimateautobuilder.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ultimateautobuilder.builder.BuildMode;
import com.ultimateautobuilder.client.ClientData;
import com.ultimateautobuilder.client.ClientModels;
import com.ultimateautobuilder.client.ClientState;
import com.ultimateautobuilder.client.GamepadInput;
import com.ultimateautobuilder.client.PlacementController;
import com.ultimateautobuilder.client.ProgressHud;
import com.ultimateautobuilder.client.ThumbnailCache;
import com.ultimateautobuilder.config.ClientConfig;
import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.SizePreset;
import com.ultimateautobuilder.core.catalog.SizeSpec;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.model.ModelScaler;
import com.ultimateautobuilder.core.model.VoxelModel;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Menu principal : catalogue (catégories, recherche, filtres, tris, cartes avec miniatures), panneau
 * de détail (taille, rotation, miroir, mode, position), historique et modèles personnalisés.
 * Utilisable à la souris, au clavier et à la manette. Le serveur revalide chaque demande.
 */
public final class BuilderScreen extends Screen {
    private enum Page { CATALOG, HISTORY, CUSTOM }

    /** Zone ayant le focus manette : grille de cartes ou commandes (détail / listes). */
    private enum Zone { GRID, CONTROLS }

    private record Tab(String label, Category category, int special) {
    }

    private static final int SPECIAL_NONE = 0, SPECIAL_ALL = 1, SPECIAL_FAVORITES = 2, SPECIAL_RECENTS = 3;
    private static final int CARD_W = 70, CARD_H = 80, GAP = 4, ROW_H = 22;
    private static final String[] SIZE_FILTERS = {"Toutes tailles", "Petites", "Moyennes", "Grandes"};
    private static final String[] ROTATIONS = {"0°", "90°", "180°", "270°"};

    // État conservé entre deux ouvertures du menu (session de jeu).
    private static boolean initialized;
    private static Page page = Page.CATALOG;
    private static int tabIndex;
    private static int tabScroll;
    private static String query = "";
    private static int pageIndex;
    private static String selectedId;
    private static Catalog.Sort sort = Catalog.Sort.NAME;
    private static int sizeFilter;
    private static int listScroll;
    private static String coordX = "", coordY = "", coordZ = "";
    private static String saveName = "";

    private final List<Tab> tabs = new ArrayList<>();
    private final List<AbstractWidget> controls = new ArrayList<>();
    private Catalog view;
    private long viewVersion = -1;
    private List<ModelEntry> results = List.of();
    private boolean compact;
    private boolean detailOpen;
    private Zone zone = Zone.GRID;
    private int controlFocus;
    private int thumbView;
    private boolean lastCanBuild;

    // Géométrie calculée dans init().
    private int gridX, gridY, gridW, gridH, cols, rows;
    private int panelX, panelY, panelW, panelH, thumbSize;
    private int tabsY, tabsX0, tabsX1;
    private EditBox searchBox;

    public BuilderScreen() {
        super(Component.literal("Ultimate Auto Builder"));
        if (!initialized) {
            initialized = true;
            applyPreferences();
        }
    }

    /** Reprend les préférences sauvegardées (taille, orientation, mode, position…). */
    private static void applyPreferences() {
        ClientData.Data d = ClientData.get();
        PlacementController.Settings s = PlacementController.SETTINGS;
        s.preset = SizePreset.byOrdinal(d.preferredPreset);
        s.rotationOffset = Math.floorMod(d.preferredRotationOffset, 4);
        s.mode = BuildMode.byOrdinal(d.preferredMode);
        PlacementController.PositionMode[] pm = PlacementController.PositionMode.values();
        s.position = d.positionMode >= 0 && d.positionMode < pm.length ? pm[d.positionMode] : PlacementController.PositionMode.FRONT;
        int defDistance = ClientConfig.loaded() ? ClientConfig.DEFAULT_DISTANCE.get() : 6;
        s.distance = d.distance > 0 ? Math.min(128, d.distance) : defDistance;
        s.hollow = d.hollow;
        Catalog.Sort[] sorts = Catalog.Sort.values();
        sort = d.sort >= 0 && d.sort < sorts.length ? sorts[d.sort] : Catalog.Sort.NAME;
    }

    // ------------------------------------------------------------------ données

    /**
     * Catalogue vu par le client : modèles intégrés + modèles personnalisés annoncés par le serveur.
     * En multijoueur, les modèles personnalisés locaux ne sont pas proposés (le serveur ne les a pas).
     */
    private void refreshView() {
        long v = ClientState.version();
        if (view != null && v == viewVersion) {
            return;
        }
        viewVersion = v;
        Catalog c = new Catalog();
        Catalog shared = DefaultCatalog.shared();
        for (ModelEntry e : shared.all()) {
            if (e.category() != Category.CUSTOM) {
                c.register(e);
            }
        }
        StatePayload st = ClientState.state();
        if (st != null) {
            boolean integrated = Minecraft.getInstance().getSingleplayerServer() != null;
            for (StatePayload.CustomInfo ci : st.customs()) {
                ModelEntry local = integrated ? shared.get(ci.id()) : null;
                try {
                    c.register(local != null ? local : remoteCustom(ci));
                } catch (RuntimeException ex) {
                    // Identifiant invalide ou doublon : le modèle est simplement ignoré côté interface.
                }
            }
        }
        view = c;
        buildTabs();
        recompute();
    }

    /** Entrée locale pour un modèle personnalisé du serveur ; mêmes tailles que côté serveur. */
    private static ModelEntry remoteCustom(StatePayload.CustomInfo ci) {
        int h = Math.max(1, ci.sy());
        int small = Math.max(4, h / 2);
        int normal = Math.max(small, h);
        int large = Math.max(normal, h * 3 / 2);
        int giant = Math.max(large, h * 2);
        String id = ci.id();
        return new ModelEntry(id, ci.name(), Category.CUSTOM, new String[]{"personnalise", "import"}, 2,
                SizeSpec.height(small, normal, large, giant), false, p -> {
            VoxelModel preview = ClientState.customPreview(id);
            return preview == null ? VoxelModel.EMPTY : ModelScaler.toHeight(preview, p.size());
        });
    }

    private void buildTabs() {
        tabs.clear();
        tabs.add(new Tab("Tout (" + view.size() + ")", null, SPECIAL_ALL));
        tabs.add(new Tab("★ Favoris", null, SPECIAL_FAVORITES));
        tabs.add(new Tab("Récents", null, SPECIAL_RECENTS));
        for (Category c : Category.values()) {
            tabs.add(new Tab(c.displayName() + " (" + view.count(c) + ")", c, SPECIAL_NONE));
        }
        tabIndex = Math.max(0, Math.min(tabs.size() - 1, tabIndex));
    }

    private void recompute() {
        if (view == null) {
            return;
        }
        Tab tab = tabs.get(tabIndex);
        Predicate<ModelEntry> filter = switch (sizeFilter) {
            case 1 -> Catalog.sizeBetween(1, 16);
            case 2 -> Catalog.sizeBetween(17, 40);
            case 3 -> Catalog.sizeBetween(41, Integer.MAX_VALUE);
            default -> null;
        };
        if (tab.special() == SPECIAL_FAVORITES || tab.special() == SPECIAL_RECENTS) {
            List<String> ids = tab.special() == SPECIAL_FAVORITES ? ClientData.get().favorites : ClientData.get().recents;
            Predicate<ModelEntry> in = Catalog.idIn(new HashSet<>(ids));
            filter = filter == null ? in : filter.and(in);
        }
        List<ModelEntry> found = view.search(query, tab.category(), filter, sort);
        if (tab.special() == SPECIAL_RECENTS && query.isBlank()) {
            // Les récents restent dans l'ordre d'utilisation, plus parlant qu'un tri alphabétique.
            List<String> order = ClientData.get().recents;
            found.sort((a, b) -> Integer.compare(order.indexOf(a.id()), order.indexOf(b.id())));
        }
        results = found;
        int per = Math.max(1, cols * rows);
        int maxPage = Math.max(0, (results.size() - 1) / per);
        pageIndex = Math.max(0, Math.min(pageIndex, maxPage));
    }

    private ModelEntry selected() {
        if (selectedId == null || view == null) {
            return null;
        }
        return view.get(selectedId);
    }

    private void select(ModelEntry e) {
        selectedId = e == null ? null : e.id();
        PlacementController.SETTINGS.entry = e;
        thumbView = 0;
        if (e != null) {
            ClientModels.request(e, PlacementController.SETTINGS.size(), PlacementController.SETTINGS.hollow);
        }
    }

    // ------------------------------------------------------------------ mise en page

    @Override
    protected void init() {
        refreshView();
        compact = width < 560;
        int margin = 8;
        int top = 18;

        // Barre de recherche, tri et filtre de taille.
        int filterW = compact ? 70 : 90;
        int searchW = Math.max(60, Math.min(240, width - 2 * margin - 2 * (filterW + 4)));
        searchBox = new EditBox(font, margin, top, searchW, 16, Component.literal("Recherche"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Rechercher (nom, mot-clé…)"));
        searchBox.setValue(query);
        searchBox.setResponder(s -> {
            if (!s.equals(query)) {
                query = s;
                pageIndex = 0;
                recompute();
            }
        });
        addRenderableWidget(searchBox);
        int fx = margin + searchW + 4;
        addRenderableWidget(Button.builder(Component.literal("Tri : " + sort.label()), b -> {
            sort = Catalog.Sort.values()[(sort.ordinal() + 1) % Catalog.Sort.values().length];
            ClientData.get().sort = sort.ordinal();
            ClientData.save();
            recompute();
            rebuildWidgets();
        }).bounds(fx, top - 1, filterW, 18).build());
        addRenderableWidget(Button.builder(Component.literal(SIZE_FILTERS[sizeFilter]), b -> {
            sizeFilter = (sizeFilter + 1) % SIZE_FILTERS.length;
            pageIndex = 0;
            recompute();
            rebuildWidgets();
        }).bounds(fx + filterW + 4, top - 1, filterW, 18).build());

        // Onglets de catégories (dessinés à la main, défilement par flèches).
        tabsY = top + 22;
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> switchTab(-1)).bounds(margin, tabsY, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> switchTab(1)).bounds(width - margin - 14, tabsY, 14, 14).build());
        tabsX0 = margin + 16;
        tabsX1 = width - margin - 16;

        // Zones principales.
        int contentTop = tabsY + 20;
        int contentBottom = height - 38;
        panelW = compact ? width - 2 * margin : Math.max(170, Math.min(220, width / 3));
        panelX = compact ? margin : width - margin - panelW;
        panelY = contentTop;
        panelH = contentBottom - contentTop;
        gridX = margin;
        gridY = contentTop;
        gridW = compact ? width - 2 * margin : panelX - margin - 8;
        gridH = contentBottom - contentTop;
        cols = Math.max(1, (gridW + GAP) / (CARD_W + GAP));
        rows = Math.max(1, (gridH + GAP) / (CARD_H + GAP));
        recompute();

        controls.clear();
        switch (page) {
            case CATALOG -> {
                if (!compact || detailOpen) {
                    initDetail();
                }
            }
            case HISTORY -> initHistory();
            case CUSTOM -> initCustom();
        }
        initBottomBar(height - 24);
        controlFocus = Math.max(0, Math.min(controlFocus, controls.size() - 1));
        if (zone == Zone.CONTROLS && !controls.isEmpty()) {
            setFocused(controls.get(controlFocus));
        }
        StatePayload st = ClientState.state();
        lastCanBuild = st != null && st.canBuild();
    }

    private void initBottomBar(int y) {
        int n = 4;
        int bw = Math.min(110, (width - 16 - (n - 1) * 4) / n);
        int x = (width - (n * bw + (n - 1) * 4)) / 2;
        addRenderableWidget(Button.builder(Component.literal(page == Page.HISTORY ? "Catalogue" : "Historique"),
                b -> setPage(page == Page.HISTORY ? Page.CATALOG : Page.HISTORY)).bounds(x, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal(page == Page.CUSTOM ? "Catalogue" : "Personnalisés"),
                b -> setPage(page == Page.CUSTOM ? Page.CATALOG : Page.CUSTOM)).bounds(x + bw + 4, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Paramètres"), b -> minecraft.setScreen(new SettingsScreen(this)))
                .bounds(x + 2 * (bw + 4), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose()).bounds(x + 3 * (bw + 4), y, bw, 20).build());
    }

    private void setPage(Page p) {
        page = p;
        listScroll = 0;
        zone = p == Page.CATALOG ? Zone.GRID : Zone.CONTROLS;
        controlFocus = 0;
        if (p != Page.CATALOG) {
            PacketDistributor.sendToServer(new ActionPayload(ActionPayload.REQUEST_STATE, 0, ""));
        }
        rebuildWidgets();
    }

    /** Hauteur de la grande miniature selon la place restante sous les commandes. */
    private int detailRows() {
        PlacementController.Settings s = PlacementController.SETTINGS;
        int r = 6;
        if (s.preset == SizePreset.CUSTOM) {
            r++;
        }
        if (s.position == PlacementController.PositionMode.FRONT || s.position == PlacementController.PositionMode.COORDS) {
            r++;
        }
        if (compact) {
            r++;
        }
        return r;
    }

    private void initDetail() {
        ModelEntry e = selected();
        if (e == null) {
            return;
        }
        PlacementController.Settings s = PlacementController.SETTINGS;
        if (s.entry != e) {
            s.entry = e;
        }
        int x = panelX + 4;
        int w = panelW - 8;
        int half = (w - 4) / 2;
        int textH = 34;
        thumbSize = Math.max(32, Math.min(compact ? 72 : 112, panelH - detailRows() * ROW_H - textH - 8));
        int y = panelY + thumbSize + textH + 4;

        controls.add(addRenderableWidget(Button.builder(Component.literal(sizeLabel(e)), b -> {
            cycleSize(1);
            rebuildWidgets();
        }).bounds(x, y, w, 20).tooltip(Tooltip.create(Component.literal("Petit, Normal, Grand, Géant ou taille personnalisée"))).build()));
        y += ROW_H;
        if (s.preset == SizePreset.CUSTOM) {
            EditBox custom = new EditBox(font, x + 1, y + 1, w - 2, 18, Component.literal("Taille personnalisée"));
            custom.setFilter(t -> t.isEmpty() || t.matches("\\d{1,4}"));
            custom.setValue(Integer.toString(s.customSize));
            custom.setResponder(t -> {
                if (!t.isEmpty()) {
                    int v = Integer.parseInt(t);
                    s.customSize = Math.max(e.sizes().min(), Math.min(e.sizes().max(), v));
                    ClientModels.request(e, s.size(), s.hollow);
                }
            });
            custom.setTooltip(Tooltip.create(Component.literal("Entre " + e.sizes().min() + " et " + e.sizes().max() + " blocs (" + e.sizes().kind().label() + ")")));
            controls.add(addRenderableWidget(custom));
            y += ROW_H;
        }
        controls.add(addRenderableWidget(Button.builder(Component.literal("Rotation : " + ROTATIONS[s.rotationOffset & 3]), b -> {
            s.rotationOffset = (s.rotationOffset + 1) & 3;
            thumbView = s.rotationOffset;
            rebuildWidgets();
        }).bounds(x, y, half, 20).build()));
        controls.add(addRenderableWidget(Button.builder(Component.literal("Miroir : " + (s.mirror ? "oui" : "non")), b -> {
            s.mirror = !s.mirror;
            rebuildWidgets();
        }).bounds(x + half + 4, y, half, 20).build()));
        y += ROW_H;
        controls.add(addRenderableWidget(Button.builder(Component.literal("Mode : " + s.mode.label()), b -> {
            s.mode = BuildMode.values()[(s.mode.ordinal() + 1) % BuildMode.values().length];
            rebuildWidgets();
        }).bounds(x, y, half, 20).tooltip(Tooltip.create(Component.literal(modeHelp(s.mode)))).build()));
        Button hollow = Button.builder(Component.literal("Creux : " + (s.hollow && e.hollowable() ? "oui" : "non")), b -> {
            s.hollow = !s.hollow;
            ClientModels.request(e, s.size(), s.hollow);
            rebuildWidgets();
        }).bounds(x + half + 4, y, half, 20).tooltip(Tooltip.create(Component.literal("Intérieur vide : moins de blocs, construction plus rapide"))).build();
        hollow.active = e.hollowable();
        controls.add(addRenderableWidget(hollow));
        y += ROW_H;
        controls.add(addRenderableWidget(Button.builder(Component.literal("Position : " + s.position.label), b -> {
            PlacementController.PositionMode[] all = PlacementController.PositionMode.values();
            s.position = all[(s.position.ordinal() + 1) % all.length];
            if (s.position == PlacementController.PositionMode.COORDS && coordX.isEmpty() && minecraft.player != null) {
                BlockPos p = minecraft.player.blockPosition().below();
                coordX = Integer.toString(p.getX());
                coordY = Integer.toString(p.getY());
                coordZ = Integer.toString(p.getZ());
                updateCoords();
            }
            rebuildWidgets();
        }).bounds(x, y, w, 20).build()));
        y += ROW_H;
        if (s.position == PlacementController.PositionMode.FRONT) {
            controls.add(addRenderableWidget(Button.builder(Component.literal("−"), b -> {
                s.distance = Math.max(1, s.distance - 1);
                rebuildWidgets();
            }).bounds(x, y, 20, 20).build()));
            controls.add(addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                s.distance = Math.min(128, s.distance + 1);
                rebuildWidgets();
            }).bounds(x + w - 20, y, 20, 20).build()));
            y += ROW_H;
        } else if (s.position == PlacementController.PositionMode.COORDS) {
            int cw = (w - 8) / 3;
            controls.add(addRenderableWidget(coordBox(x + 1, y + 1, cw - 2, coordX, v -> coordX = v)));
            controls.add(addRenderableWidget(coordBox(x + cw + 5, y + 1, cw - 2, coordY, v -> coordY = v)));
            controls.add(addRenderableWidget(coordBox(x + 2 * (cw + 4) + 1, y + 1, cw - 2, coordZ, v -> coordZ = v)));
            y += ROW_H;
        }
        boolean fav = ClientData.isFavorite(e.id());
        controls.add(addRenderableWidget(Button.builder(Component.literal(fav ? "★ Favori" : "☆ Favori"), b -> {
            ClientData.toggleFavorite(e.id());
            recompute();
            rebuildWidgets();
        }).bounds(x, y, half, 20).build()));
        controls.add(addRenderableWidget(Button.builder(Component.literal("Aperçu"), b -> {
            PlacementController.start();
            onClose();
        }).bounds(x + half + 4, y, half, 20).tooltip(Tooltip.create(Component.literal(
                "Fantôme dans le monde, aucun bloc posé. Flèches/PgPréc/PgSuiv : déplacer, R : tourner, M : miroir, Entrée : construire, Retour arrière : annuler")))
                .build()));
        y += ROW_H;
        StatePayload st = ClientState.state();
        Button build = Button.builder(Component.literal("§lCONSTRUIRE"), b -> PlacementController.requestBuild(minecraft, this))
                .bounds(x, y, w, 20).build();
        build.active = st == null || st.canBuild();
        if (!build.active) {
            build.setTooltip(Tooltip.create(Component.literal("Vous n'avez pas la permission de construire sur ce serveur")));
        }
        controls.add(addRenderableWidget(build));
        y += ROW_H;
        if (compact) {
            controls.add(addRenderableWidget(Button.builder(Component.literal("← Retour"), b -> {
                detailOpen = false;
                zone = Zone.GRID;
                rebuildWidgets();
            }).bounds(x, y, w, 20).build()));
        }
    }

    private EditBox coordBox(int x, int y, int w, String value, java.util.function.Consumer<String> sink) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal("Coordonnée"));
        box.setFilter(t -> t.isEmpty() || t.equals("-") || t.matches("-?\\d{1,8}"));
        box.setValue(value);
        box.setResponder(t -> {
            sink.accept(t);
            updateCoords();
        });
        return box;
    }

    private static void updateCoords() {
        try {
            PlacementController.SETTINGS.coords = new BlockPos(Integer.parseInt(coordX), Integer.parseInt(coordY), Integer.parseInt(coordZ));
        } catch (NumberFormatException e) {
            PlacementController.SETTINGS.coords = null;
        }
    }

    private void initHistory() {
        StatePayload st = ClientState.state();
        if (st == null) {
            return;
        }
        int x = gridX;
        int w = width - 16;
        int bw = 70;
        int visible = Math.max(1, (panelH - 14) / ROW_H);
        int total = st.jobs().size() + st.history().size();
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, total - visible)));
        int y = panelY + 14;
        int index = 0;
        for (StatePayload.JobInfo j : st.jobs()) {
            if (index++ < listScroll || index - listScroll > visible) {
                continue;
            }
            controls.add(addRenderableWidget(Button.builder(Component.literal("Annuler"),
                    b -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.CANCEL, j.id(), ""))).bounds(x + w - bw, y, bw, 20).build()));
            y += ROW_H;
        }
        for (StatePayload.HistoryInfo h : st.history()) {
            if (index++ < listScroll || index - listScroll > visible) {
                continue;
            }
            Button undo = Button.builder(Component.literal("Défaire"),
                    b -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.UNDO, h.id(), ""))).bounds(x + w - bw, y, bw, 20).build();
            undo.active = h.undoAvailable() && st.canBuild();
            undo.setTooltip(Tooltip.create(Component.literal(h.undoAvailable()
                    ? "Restaure les blocs remplacés (les blocs modifiés depuis sont conservés en mode prudent)"
                    : "Retour arrière indisponible pour cette construction")));
            controls.add(addRenderableWidget(undo));
            y += ROW_H;
        }
    }

    private void initCustom() {
        StatePayload st = ClientState.state();
        if (st == null) {
            return;
        }
        int x = gridX;
        int w = width - 16;
        int bw = 70;
        int y = panelY + 14;
        int bottom = panelY + panelH;
        if (st.canImport()) {
            EditBox name = new EditBox(font, x + 1, y + 1, Math.max(60, w - 2 * (bw + 4) - 140), 18, Component.literal("Nom"));
            name.setMaxLength(48);
            name.setHint(Component.literal("Nom du modèle"));
            name.setValue(saveName);
            name.setResponder(t -> saveName = t);
            controls.add(addRenderableWidget(name));
            Button save = Button.builder(Component.literal("Sauver zone"), b -> {
                if (!saveName.isBlank()) {
                    PacketDistributor.sendToServer(new ActionPayload(ActionPayload.SAVE_SELECTION, 0, saveName.trim()));
                }
            }).bounds(x + w - 2 * bw - 4, y, bw, 20).tooltip(Tooltip.create(Component.literal(
                    "Enregistre la zone délimitée par les deux coins (accroupi + clic droit avec le cristal)"))).build();
            save.active = st.hasCorner1() && st.hasCorner2();
            controls.add(addRenderableWidget(save));
            y += ROW_H + 10;
        }
        int visible = Math.max(1, (bottom - y - 12) / ROW_H);
        int total = st.customs().size() + (st.canImport() ? st.importable().size() : 0);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, total - visible)));
        y += 12;
        int index = 0;
        for (StatePayload.CustomInfo c : st.customs()) {
            if (index++ < listScroll || index - listScroll > visible) {
                continue;
            }
            controls.add(addRenderableWidget(Button.builder(Component.literal("Choisir"), b -> {
                tabIndex = indexOfCategory(Category.CUSTOM);
                query = "";
                refreshView();
                select(view.get(c.id()));
                page = Page.CATALOG;
                detailOpen = true;
                zone = Zone.CONTROLS;
                recompute();
                rebuildWidgets();
            }).bounds(x + w - 2 * bw - 4, y, bw, 20).build()));
            Button del = Button.builder(Component.literal("Supprimer"), b -> minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(yes -> {
                if (yes) {
                    PacketDistributor.sendToServer(new ActionPayload(ActionPayload.DELETE_CUSTOM, 0, c.id()));
                }
                minecraft.setScreen(this);
            }, Component.literal("Supprimer « " + c.name() + " » ?"), Component.literal("Le fichier du modèle sera effacé du serveur."))))
                    .bounds(x + w - bw, y, bw, 20).build();
            del.active = st.canImport();
            controls.add(addRenderableWidget(del));
            y += ROW_H;
        }
        if (st.canImport()) {
            for (String file : st.importable()) {
                if (index++ < listScroll || index - listScroll > visible) {
                    continue;
                }
                controls.add(addRenderableWidget(Button.builder(Component.literal("Importer"),
                        b -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.IMPORT, 0, file))).bounds(x + w - bw, y, bw, 20).build()));
                y += ROW_H;
            }
        }
    }

    private int indexOfCategory(Category c) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).category() == c) {
                return i;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ actions

    private void switchTab(int delta) {
        if (tabs.isEmpty()) {
            return;
        }
        tabIndex = Math.floorMod(tabIndex + delta, tabs.size());
        pageIndex = 0;
        if (page != Page.CATALOG) {
            page = Page.CATALOG;
        }
        Tab t = tabs.get(tabIndex);
        ClientData.get().lastCategory = t.category() != null ? t.category().key() : "special" + t.special();
        recompute();
        ensureTabVisible();
        rebuildWidgets();
    }

    private void ensureTabVisible() {
        tabScroll = Math.min(tabScroll, tabIndex);
        int x = 0;
        int avail = tabsX1 - tabsX0;
        // Avance le défilement tant que l'onglet sélectionné dépasse à droite.
        while (tabScroll < tabIndex) {
            x = 0;
            for (int i = tabScroll; i <= tabIndex; i++) {
                x += font.width(tabs.get(i).label()) + 10;
            }
            if (x <= avail) {
                break;
            }
            tabScroll++;
        }
    }

    private static void cycleSize(int delta) {
        PlacementController.Settings s = PlacementController.SETTINGS;
        SizePreset[] all = SizePreset.values();
        s.preset = all[Math.floorMod(s.preset.ordinal() + delta, all.length)];
        if (s.entry != null) {
            if (s.preset == SizePreset.CUSTOM && (s.customSize < s.entry.sizes().min() || s.customSize > s.entry.sizes().max())) {
                s.customSize = s.entry.sizes().normal();
            }
            ClientModels.request(s.entry, s.size(), s.hollow);
        }
    }

    private static String sizeLabel(ModelEntry e) {
        PlacementController.Settings s = PlacementController.SETTINGS;
        return "Taille : " + s.preset.displayName() + " (" + e.sizes().resolve(s.preset, s.customSize) + " blocs, " + e.sizes().kind().label() + ")";
    }

    private static String modeHelp(BuildMode m) {
        return switch (m) {
            case NORMAL -> "Remplace les blocs existants (blocs précieux protégés selon la configuration)";
            case PRUDENT -> "N'écrase que l'air, l'herbe haute, la neige… Les conflits sont signalés";
            case TERRAIN -> "Ajoute des fondations jusqu'au sol sous la structure";
            case AERIAL -> "Construit la structure en hauteur, sans toucher au sol";
        };
    }

    private void moveSelection(int dx, int dy) {
        if (results.isEmpty()) {
            return;
        }
        int per = Math.max(1, cols * rows);
        int idx = indexOfSelected();
        if (idx < 0) {
            idx = pageIndex * per;
        } else {
            idx += dx + dy * cols;
        }
        idx = Math.max(0, Math.min(results.size() - 1, idx));
        pageIndex = idx / per;
        select(results.get(idx));
        rebuildWidgets();
    }

    private int indexOfSelected() {
        if (selectedId == null) {
            return -1;
        }
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).id().equals(selectedId)) {
                return i;
            }
        }
        return -1;
    }

    private void changePage(int delta) {
        if (page != Page.CATALOG) {
            listScroll = Math.max(0, listScroll + delta * 5);
            rebuildWidgets();
            return;
        }
        int per = Math.max(1, cols * rows);
        int maxPage = Math.max(0, (results.size() - 1) / per);
        int np = Math.max(0, Math.min(maxPage, pageIndex + delta));
        if (np != pageIndex) {
            pageIndex = np;
            rebuildWidgets();
        }
    }

    private void openDetail() {
        if (selected() == null) {
            return;
        }
        detailOpen = true;
        zone = Zone.CONTROLS;
        controlFocus = 0;
        rebuildWidgets();
    }

    // ------------------------------------------------------------------ manette

    /** Appelé à chaque tick client quand ce menu est ouvert (voir ClientTicker). */
    public void onGamepadTick() {
        if (!GamepadInput.available()) {
            return;
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_PREV_CATEGORY.get())) {
            switchTab(-1);
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_NEXT_CATEGORY.get())) {
            switchTab(1);
        }
        if (page == Page.CATALOG && selected() != null) {
            if (GamepadInput.pressed(ClientConfig.GAMEPAD_ROTATE.get())) {
                PlacementController.SETTINGS.rotationOffset = (PlacementController.SETTINGS.rotationOffset + 1) & 3;
                thumbView = PlacementController.SETTINGS.rotationOffset;
                rebuildWidgets();
            }
            if (GamepadInput.pressed(ClientConfig.GAMEPAD_SIZE.get())) {
                cycleSize(1);
                rebuildWidgets();
            }
            if (GamepadInput.pressed(ClientConfig.GAMEPAD_BUILD.get())) {
                PlacementController.requestBuild(minecraft, this);
                return;
            }
        }
        boolean select = GamepadInput.pressed(ClientConfig.GAMEPAD_SELECT.get());
        boolean back = GamepadInput.pressed(ClientConfig.GAMEPAD_BACK.get());
        if (zone == Zone.GRID && page == Page.CATALOG) {
            if (GamepadInput.direction(0)) {
                moveSelection(0, -1);
            }
            if (GamepadInput.direction(2)) {
                moveSelection(0, 1);
            }
            if (GamepadInput.direction(3)) {
                moveSelection(-1, 0);
            }
            if (GamepadInput.direction(1)) {
                int idx = indexOfSelected();
                if (!compact && idx >= 0 && (idx % cols == cols - 1 || idx == results.size() - 1) && !controls.isEmpty()) {
                    // Bord droit de la grille : passage au panneau de détail.
                    focusControls();
                } else {
                    moveSelection(1, 0);
                }
            }
            if (select) {
                if (selected() == null) {
                    moveSelection(0, 0);
                } else if (compact) {
                    openDetail();
                } else {
                    focusControls();
                }
            }
            if (back) {
                onClose();
            }
            return;
        }
        // Zone des commandes : haut/bas parcourent les boutons, A les active.
        if (controls.isEmpty()) {
            if (back) {
                leaveControls();
            }
            return;
        }
        if (GamepadInput.direction(0)) {
            focusControl(controlFocus - 1);
        }
        if (GamepadInput.direction(2)) {
            focusControl(controlFocus + 1);
        }
        if (page == Page.CATALOG && !compact && GamepadInput.direction(3)) {
            leaveControls();
            return;
        }
        if (select) {
            AbstractWidget w = controls.get(controlFocus);
            int remembered = controlFocus;
            if (w instanceof AbstractButton b && b.active) {
                b.onPress();
            } else if (w instanceof EditBox box) {
                setFocused(box);
            }
            controlFocus = remembered;
            if (minecraft.screen == this && !controls.isEmpty()) {
                focusControl(controlFocus);
            }
        }
        if (back) {
            leaveControls();
        }
    }

    private void focusControls() {
        zone = Zone.CONTROLS;
        focusControl(0);
    }

    private void focusControl(int i) {
        if (controls.isEmpty()) {
            return;
        }
        controlFocus = Math.floorMod(i, controls.size());
        setFocused(controls.get(controlFocus));
    }

    private void leaveControls() {
        if (page != Page.CATALOG) {
            setPage(Page.CATALOG);
            return;
        }
        zone = Zone.GRID;
        if (compact) {
            detailOpen = false;
        }
        setFocused(null);
        rebuildWidgets();
    }

    // ------------------------------------------------------------------ entrées clavier / souris

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean typing = getFocused() instanceof EditBox;
        if (key == GLFW.GLFW_KEY_F && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            setFocused(searchBox);
            searchBox.setFocused(true);
            return true;
        }
        if (typing) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_DOWN) {
                setFocused(null);
                return true;
            }
            return super.keyPressed(key, scan, mods);
        }
        if (page == Page.CATALOG && zone == Zone.GRID) {
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> {
                    moveSelection(-1, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveSelection(1, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    moveSelection(0, -1);
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    moveSelection(0, 1);
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    if (compact && !detailOpen) {
                        openDetail();
                    } else if (selected() != null) {
                        PlacementController.requestBuild(minecraft, this);
                    }
                    return true;
                }
                default -> {
                }
            }
        }
        if (key == GLFW.GLFW_KEY_PAGE_UP) {
            changePage(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            changePage(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && compact && detailOpen) {
            leaveControls();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        // Onglets.
        if (my >= tabsY && my < tabsY + 14) {
            int x = tabsX0;
            for (int i = tabScroll; i < tabs.size(); i++) {
                int w = font.width(tabs.get(i).label()) + 8;
                if (x + w > tabsX1) {
                    break;
                }
                if (mx >= x && mx < x + w) {
                    switchTab(i - tabIndex);
                    return true;
                }
                x += w + 2;
            }
        }
        if (page == Page.CATALOG && (!compact || !detailOpen)) {
            ModelEntry hit = cardAt(mx, my);
            if (hit != null) {
                select(hit);
                zone = Zone.GRID;
                if (compact) {
                    openDetail();
                    return true;
                }
                rebuildWidgets();
                return true;
            }
        }
        // Clic sur la grande miniature : vue suivante.
        if (page == Page.CATALOG && selected() != null && (!compact || detailOpen) && mx >= panelX + (panelW - thumbSize) / 2.0
                && mx < panelX + (panelW + thumbSize) / 2.0 && my >= panelY && my < panelY + thumbSize) {
            thumbView = (thumbView + 1) & 3;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (super.mouseScrolled(mx, my, sx, sy)) {
            return true;
        }
        if (my >= tabsY && my < tabsY + 14) {
            tabScroll = Math.max(0, Math.min(tabs.size() - 1, tabScroll - (int) Math.signum(sy)));
            return true;
        }
        if (sy != 0) {
            changePage(sy > 0 ? -1 : 1);
            return true;
        }
        return false;
    }

    private ModelEntry cardAt(double mx, double my) {
        if (mx < gridX || my < gridY || mx >= gridX + cols * (CARD_W + GAP) || my >= gridY + rows * (CARD_H + GAP)) {
            return null;
        }
        int c = (int) ((mx - gridX) / (CARD_W + GAP));
        int r = (int) ((my - gridY) / (CARD_H + GAP));
        int idx = pageIndex * cols * rows + r * cols + c;
        return idx >= 0 && idx < results.size() ? results.get(idx) : null;
    }

    // ------------------------------------------------------------------ cycle de vie

    @Override
    public void tick() {
        super.tick();
        long before = viewVersion;
        refreshView();
        StatePayload st = ClientState.state();
        boolean canBuild = st != null && st.canBuild();
        // Les listes (historique, personnalisés) et les droits dépendent de l'état serveur.
        if (before != viewVersion && (page != Page.CATALOG || canBuild != lastCanBuild || selected() == null && selectedId != null)) {
            rebuildWidgets();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ClientData.save();
        super.onClose();
    }

    // ------------------------------------------------------------------ rendu

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        if (page == Page.CATALOG && (!compact || detailOpen) && selected() != null) {
            g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xB0101820);
            g.renderOutline(panelX - 2, panelY - 2, panelW + 4, panelH + 4, 0xFF3A6A8A);
        }
        if (page != Page.CATALOG) {
            g.fill(gridX - 2, panelY - 2, width - 6, panelY + panelH + 2, 0xB0101820);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawString(font, "§b❄ §fUltimate Auto Builder", 8, 5, 0xFFFFFFFF);
        renderTabs(g, mx, my);
        switch (page) {
            case CATALOG -> {
                if (!compact || !detailOpen) {
                    renderGrid(g, mx, my);
                }
                if (!compact || detailOpen) {
                    renderDetail(g);
                }
            }
            case HISTORY -> renderHistory(g);
            case CUSTOM -> renderCustom(g);
        }
        List<ClientState.Progress> progress = ClientState.progress();
        if (!progress.isEmpty() && !compact) {
            ProgressHud.drawBar(g, font, progress.get(0), width - 160, 4);
        }
        String pad = GamepadInput.available() ? "Manette : " + GamepadInput.name() : null;
        if (pad != null && !compact) {
            g.drawString(font, pad, width - 8 - font.width(pad), height - 35, 0xFF777777, false);
        }
    }

    private void renderTabs(GuiGraphics g, int mx, int my) {
        int x = tabsX0;
        for (int i = tabScroll; i < tabs.size(); i++) {
            String label = tabs.get(i).label();
            int w = font.width(label) + 8;
            if (x + w > tabsX1) {
                break;
            }
            boolean sel = i == tabIndex && page == Page.CATALOG;
            boolean hover = mx >= x && mx < x + w && my >= tabsY && my < tabsY + 14;
            g.fill(x, tabsY, x + w, tabsY + 14, sel ? 0xFF2A7FB0 : hover ? 0xFF3A3A4A : 0xFF22222C);
            g.drawString(font, label, x + 4, tabsY + 3, sel ? 0xFFFFFFFF : 0xFFCCCCCC, false);
            x += w + 2;
        }
    }

    private void renderGrid(GuiGraphics g, int mx, int my) {
        int per = Math.max(1, cols * rows);
        int start = pageIndex * per;
        if (results.isEmpty()) {
            Tab t = tabs.isEmpty() ? null : tabs.get(tabIndex);
            String msg = t != null && t.special() == SPECIAL_FAVORITES && query.isBlank() ? "Aucun favori : ouvrez un modèle et cliquez sur « ☆ Favori »"
                    : "Aucun modèle ne correspond à la recherche";
            g.drawCenteredString(font, msg, gridX + gridW / 2, gridY + 30, 0xFFAAAAAA);
            return;
        }
        RenderSystem.enableBlend();
        ModelEntry hovered = null;
        for (int i = 0; i < per && start + i < results.size(); i++) {
            ModelEntry e = results.get(start + i);
            int x = gridX + (i % cols) * (CARD_W + GAP);
            int y = gridY + (i / cols) * (CARD_H + GAP);
            boolean sel = e.id().equals(selectedId);
            boolean hover = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;
            if (hover) {
                hovered = e;
            }
            g.fill(x, y, x + CARD_W, y + CARD_H, sel ? 0xE0204A66 : hover ? 0xD0303040 : 0xC0181820);
            g.renderOutline(x, y, CARD_W, CARD_H, sel ? (zone == Zone.GRID ? 0xFF7FE0FF : 0xFF4FA0C0) : 0xFF333344);
            ResourceLocation tex = ThumbnailCache.get(e, 0);
            int ts = 52;
            int tx = x + (CARD_W - ts) / 2;
            if (tex != null) {
                g.blit(tex, tx, y + 3, ts, ts, 0, 0, ThumbnailCache.SIZE, ThumbnailCache.SIZE, ThumbnailCache.SIZE, ThumbnailCache.SIZE);
            } else {
                g.drawCenteredString(font, "…", x + CARD_W / 2, y + 24, 0xFF666666);
            }
            if (ClientData.isFavorite(e.id())) {
                g.drawString(font, "★", x + CARD_W - 9, y + 2, 0xFFFFD040, true);
            }
            // Nom sur deux lignes au plus ; le nom complet s'affiche au survol.
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(e.name()), CARD_W - 4);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                net.minecraft.util.FormattedCharSequence line = lines.get(l);
                if (l == 1 && lines.size() > 2) {
                    String rest = font.plainSubstrByWidth(e.name().substring(Math.min(e.name().length(), firstLineLength(e.name()))), CARD_W - 12);
                    line = Component.literal(rest.strip() + "…").getVisualOrderText();
                }
                g.drawCenteredString(font, line, x + CARD_W / 2, y + CARD_H - 21 + l * 10, 0xFFFFFFFF);
            }
        }
        if (hovered != null) {
            java.util.List<Component> tip = new java.util.ArrayList<>();
            tip.add(Component.literal(hovered.name()).withStyle(net.minecraft.ChatFormatting.WHITE));
            tip.add(Component.literal(hovered.category().displayName() + " · complexité " + "★".repeat(hovered.complexity()))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            g.renderComponentTooltip(font, tip, mx, my);
        }
        int maxPage = Math.max(0, (results.size() - 1) / per);
        String info = results.size() + " modèle" + (results.size() > 1 ? "s" : "") + " — page " + (pageIndex + 1) + "/" + (maxPage + 1);
        g.drawString(font, info, gridX, height - 35, 0xFF999999, false);
        if (!compact && selected() == null) {
            g.drawCenteredString(font, "Sélectionnez un modèle", panelX + panelW / 2, panelY + 40, 0xFF888888);
        }
    }

    /** Longueur (en caractères) de la première ligne affichée dans une carte. */
    private int firstLineLength(String name) {
        java.util.List<net.minecraft.network.chat.FormattedText> parts = font.getSplitter().splitLines(name, CARD_W - 4,
                net.minecraft.network.chat.Style.EMPTY);
        return parts.isEmpty() ? name.length() : parts.get(0).getString().length();
    }

    private void renderDetail(GuiGraphics g) {
        ModelEntry e = selected();
        if (e == null) {
            return;
        }
        PlacementController.Settings s = PlacementController.SETTINGS;
        ResourceLocation tex = ThumbnailCache.get(e, thumbView);
        int tx = panelX + (panelW - thumbSize) / 2;
        RenderSystem.enableBlend();
        if (tex != null) {
            g.blit(tex, tx, panelY, thumbSize, thumbSize, 0, 0, ThumbnailCache.SIZE, ThumbnailCache.SIZE, ThumbnailCache.SIZE, ThumbnailCache.SIZE);
        } else {
            g.drawCenteredString(font, "Génération de l'aperçu…", panelX + panelW / 2, panelY + thumbSize / 2, 0xFF888888);
        }
        g.drawString(font, "⟳", tx + thumbSize - 8, panelY + thumbSize - 9, 0xFF6688AA, false);
        int y = panelY + thumbSize + 2;
        g.drawString(font, font.plainSubstrByWidth("§l" + e.name(), panelW - 8), panelX + 4, y, 0xFFFFFFFF);
        g.drawString(font, font.plainSubstrByWidth(e.category().displayName() + " · complexité " + e.complexity() + "/5", panelW - 8), panelX + 4, y + 11,
                0xFF99AABB, false);
        VoxelModel m = ClientModels.now(e, s.size(), s.hollow);
        String dims;
        if (m == null) {
            dims = "Calcul des dimensions…";
        } else if (m.isEmpty()) {
            dims = e.id().startsWith("custom:") ? "Aperçu en attente du serveur" : "Modèle vide";
        } else {
            boolean swap = (s.rotationOffset & 1) == 1;
            int sx = swap ? m.sizeZ() : m.sizeX(), sz = swap ? m.sizeX() : m.sizeZ();
            dims = sx + "×" + m.sizeY() + "×" + sz + " · " + String.format("%,d", m.solidCount()).replace(',', ' ') + " blocs";
        }
        StatePayload st = ClientState.state();
        int color = 0xFFCCCCCC;
        if (m != null && st != null && m.solidCount() > st.maxBlocks()) {
            color = 0xFFFF6666;
            dims += " (limite serveur dépassée)";
        } else if (m != null && st != null && m.solidCount() >= st.giantThreshold()) {
            color = 0xFFFFAA44;
        }
        g.drawString(font, font.plainSubstrByWidth(dims, panelW - 8), panelX + 4, y + 22, color, false);
        if (s.position == PlacementController.PositionMode.FRONT) {
            int rowY = rowOfDistance();
            if (rowY >= 0) {
                g.drawCenteredString(font, "Distance : " + s.distance + " blocs", panelX + panelW / 2, rowY + 6, 0xFFFFFFFF);
            }
        }
        if (st != null && !"true".equals(st.settings().getOrDefault("freeBuild", "true"))) {
            g.drawString(font, "Mode ressources : l'inventaire est utilisé", panelX + 4, panelY + panelH - 9, 0xFFFFAA44, false);
        }
    }

    /** Ordonnée de la ligne « distance » (entre les boutons − et +), ou -1. */
    private int rowOfDistance() {
        for (AbstractWidget w : controls) {
            if (w instanceof Button b && "−".equals(b.getMessage().getString())) {
                return b.getY();
            }
        }
        return -1;
    }

    private void renderHistory(GuiGraphics g) {
        StatePayload st = ClientState.state();
        g.drawString(font, "§lConstructions en cours et historique", gridX + 2, panelY, 0xFFFFFFFF);
        if (st == null) {
            g.drawString(font, "En attente du serveur…", gridX + 2, panelY + 18, 0xFFAAAAAA, false);
            return;
        }
        int visible = Math.max(1, (panelH - 14) / ROW_H);
        int y = panelY + 14;
        int index = 0;
        int textW = width - 16 - 80;
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm");
        for (StatePayload.JobInfo j : st.jobs()) {
            if (index++ < listScroll || index - listScroll > visible) {
                continue;
            }
            int pct = j.total() <= 0 ? 0 : (int) Math.min(100, (long) j.progress() * 100 / j.total());
            String line = "▶ " + j.title() + " — " + pct + "% (" + j.owner() + ")";
            g.drawString(font, font.plainSubstrByWidth(line, textW), gridX + 2, y + 6, 0xFF7FE0FF, false);
            y += ROW_H;
        }
        for (StatePayload.HistoryInfo h : st.history()) {
            if (index++ < listScroll || index - listScroll > visible) {
                continue;
            }
            int[] b = h.bounds();
            String where = b != null && b.length >= 3 ? " @ " + b[0] + " " + b[1] + " " + b[2] : "";
            String line = fmt.format(new Date(h.startedAt())) + "  " + h.name() + " — " + h.blocks() + " blocs, " + statusLabel(h.status()) + where
                    + " (" + h.owner() + ")";
            g.drawString(font, font.plainSubstrByWidth(line, textW), gridX + 2, y + 6, 0xFFDDDDDD, false);
            y += ROW_H;
        }
        if (st.jobs().isEmpty() && st.history().isEmpty()) {
            g.drawString(font, "Aucune construction pour le moment.", gridX + 2, panelY + 18, 0xFFAAAAAA, false);
        }
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "DONE" -> "terminée";
            case "CANCELLED" -> "annulée";
            case "FAILED" -> "échouée";
            case "UNDONE" -> "défaite";
            case "RUNNING" -> "en cours";
            default -> status.toLowerCase(java.util.Locale.ROOT);
        };
    }

    private void renderCustom(GuiGraphics g) {
        StatePayload st = ClientState.state();
        g.drawString(font, "§lModèles personnalisés", gridX + 2, panelY, 0xFFFFFFFF);
        if (st == null) {
            g.drawString(font, "En attente du serveur…", gridX + 2, panelY + 18, 0xFFAAAAAA, false);
            return;
        }
        int y = panelY + 14;
        int textW = width - 16 - 150;
        if (st.canImport()) {
            String corners = st.hasCorner1() && st.hasCorner2() ? "Zone : " + fmt(st.corner1()) + " → " + fmt(st.corner2())
                    : "Coins : accroupi + clic droit avec le cristal sur deux blocs";
            int nameRight = gridX + Math.max(60, width - 16 - 2 * 74 - 140) + 6;
            g.drawString(font, font.plainSubstrByWidth(corners, Math.max(40, width - 16 - 2 * 74 - (nameRight - gridX) - 4)), nameRight, y + 6,
                    0xFF99AABB, false);
            y += ROW_H + 10;
        } else {
            g.drawString(font, "Import réservé aux joueurs autorisés (permission ultimateautobuilder.import).", gridX + 2, y + 6, 0xFFAAAAAA, false);
            y += ROW_H;
        }
        g.drawString(font, "Fichiers acceptés dans config/ultimateautobuilder/import : .nbt (structure), .vox (MagicaVoxel), .uabm", gridX + 2, y,
                0xFF777777, false);
        y += 12;
        int visible = Math.max(1, (panelY + panelH - y) / ROW_H);
        int index = 0;
        for (StatePayload.CustomInfo c : st.customs()) {
            if (index++ < listScroll || index - listScroll > visible) {
                continue;
            }
            String line = c.name() + " — " + c.sx() + "×" + c.sy() + "×" + c.sz() + ", " + c.blocks() + " blocs";
            g.drawString(font, font.plainSubstrByWidth(line, textW), gridX + 2, y + 6, 0xFFDDDDDD, false);
            y += ROW_H;
        }
        if (st.canImport()) {
            for (String f : st.importable()) {
                if (index++ < listScroll || index - listScroll > visible) {
                    continue;
                }
                g.drawString(font, font.plainSubstrByWidth("Fichier à importer : " + f, textW), gridX + 2, y + 6, 0xFF7FE0FF, false);
                y += ROW_H;
            }
        }
        if (st.customs().isEmpty() && (!st.canImport() || st.importable().isEmpty())) {
            g.drawString(font, "Aucun modèle personnalisé.", gridX + 2, y + 6, 0xFFAAAAAA, false);
        }
    }

    private static String fmt(long packed) {
        BlockPos p = BlockPos.of(packed);
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }
}
