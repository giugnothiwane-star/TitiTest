package com.ultimateautobuilder.client.image;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.client.ClientState;
import com.ultimateautobuilder.core.image.BlockPalette;
import com.ultimateautobuilder.core.image.BuildPlanReport;
import com.ultimateautobuilder.core.image.BuildStyle;
import com.ultimateautobuilder.core.image.ImageAnalysis;
import com.ultimateautobuilder.core.image.ImageBuildProject;
import com.ultimateautobuilder.core.image.ImageData;
import com.ultimateautobuilder.core.image.ImageLoader;
import com.ultimateautobuilder.core.image.ImageModel;
import com.ultimateautobuilder.core.image.ModelEditor;
import com.ultimateautobuilder.core.image.ModelViews;
import com.ultimateautobuilder.core.image.Reconstructor;
import com.ultimateautobuilder.core.importer.ImportException;
import com.ultimateautobuilder.core.importer.ModelCodec;
import com.ultimateautobuilder.client.render.BlockMesh;
import com.ultimateautobuilder.client.render.MeshCache;
import com.ultimateautobuilder.client.render.ModelPreviews;
import com.ultimateautobuilder.network.StatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Image-to-Build : import d'une image (PC, dossier, téléphone/tablette), analyse locale, reconstruction
 * 3D, aperçus (3D, vues orthographiques, couches, comparaison), matériaux, édition avec annulation,
 * plan de construction, projets, puis envoi au serveur qui l'ajoute aux modèles personnalisés.
 * Tous les calculs lourds s'exécutent sur un thread dédié : le jeu reste fluide pendant l'analyse.
 */
public final class ImageBuildScreen extends Screen {
    private enum Tab { IMPORT("Image"), ANALYSIS("Analyse"), OPTIONS("Options"), PREVIEW("Aperçu"), MATERIALS("Matériaux"), PLAN("Plan");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private enum ViewSlot { FRONT("Avant"), SIDE("Côté"), BACK("Arrière"), TOP("Dessus");

        final String label;

        ViewSlot(String label) {
            this.label = label;
        }
    }

    private static final String[] PREVIEWS = {"3D (avant-droite)", "3D (arrière-droite)", "3D (arrière-gauche)", "3D (avant-gauche)", "Vue avant",
            "Vue arrière", "Vue gauche", "Vue droite", "Vue de dessus", "Couche", "Comparaison"};

    // Une seule tâche lourde à la fois ; un compteur de génération ignore les résultats périmés.
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UAB-ImageToBuild");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private final Screen parent;
    private final ImageTextures textures = new ImageTextures();
    private final AtomicInteger generation = new AtomicInteger();

    // État conservé entre deux ouvertures de l'écran pendant la session.
    private static final State S = new State();

    private static final class State {
        Tab tab = Tab.IMPORT;
        ViewSlot slot = ViewSlot.FRONT;
        final ImageData[] images = new ImageData[4];
        final ImageAnalysis.Result[] analyses = new ImageAnalysis.Result[4];
        final String[] names = new String[4];
        Reconstructor.Mode mode = Reconstructor.Mode.AUTO;
        Reconstructor.BackMode back = Reconstructor.BackMode.SYMMETRIC;
        BuildStyle style = BuildStyle.FIDELE;
        Set<BlockPalette.Family> families = EnumSet.of(BlockPalette.Family.CONCRETE, BlockPalette.Family.WOOL);
        Reconstructor.Detail detail = Reconstructor.Detail.MEDIUM;
        int width, height = 48, depth;
        boolean hollow = true, invertRelief, sideFlipped, highlight, subjectChosen;
        ImageAnalysis.Subject subject = ImageAnalysis.Subject.UNKNOWN;
        ModelEditor editor;
        Reconstructor.Result result;
        Reconstructor.Options usedOptions;
        String projectName = "Ma construction";
        int preview, layer, matScroll, listScroll;
        final Map<String, Integer> replaceChoice = new java.util.HashMap<>();
    }

    private volatile String status = "";
    private final MeshCache meshes = new MeshCache(4);
    private float previewYaw, previewPitch = 25f, previewZoom = 1f;
    private boolean previewDragging;
    private volatile boolean busy;
    private int modelVersion;
    private BuildPlanReport.Report report;
    private ModelViews.Comparison comparison;
    private EditBox heightBox, widthBox, depthBox, nameBox;
    private final List<Path> files = new ArrayList<>();
    private List<String> projects = new ArrayList<>();
    private final boolean[] imageUploaded = new boolean[4];
    private int cx, cy, cw, ch;

    public ImageBuildScreen(Screen parent) {
        super(Component.literal("Image → Construction"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ mise en page

    @Override
    protected void init() {
        ImageFiles.ensureDirs();
        cx = 8;
        cy = 40;
        cw = width - 16;
        ch = height - cy - 30;
        int n = Tab.values().length;
        int tw = Math.min(90, (cw - (n - 1) * 3) / n);
        for (int i = 0; i < n; i++) {
            Tab t = Tab.values()[i];
            Button b = Button.builder(Component.literal((S.tab == t ? "§e" : "") + t.label), x -> {
                S.tab = t;
                rebuildWidgets();
            }).bounds(cx + i * (tw + 3), 18, tw, 18).build();
            b.active = t == Tab.IMPORT || S.analyses[0] != null;
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.literal("Retour"), b -> onClose()).bounds(width - 88, height - 24, 80, 20).build());
        switch (S.tab) {
            case IMPORT -> initImport();
            case ANALYSIS -> initAnalysis();
            case OPTIONS -> initOptions();
            case PREVIEW -> initPreview();
            case MATERIALS -> initMaterials();
            case PLAN -> initPlan();
        }
    }

    private Button button(String label, int x, int y, int w, Button.OnPress press) {
        return addRenderableWidget(Button.builder(Component.literal(label), press).bounds(x, y, w, 20).build());
    }

    private int half() {
        return Math.min(cw / 2 - 6, 260);
    }

    private void initImport() {
        int w = half(), x = cx, y = cy;
        button("Emplacement : " + S.slot.label, x, y, w, b -> {
            S.slot = ViewSlot.values()[(S.slot.ordinal() + 1) % ViewSlot.values().length];
            rebuildWidgets();
        });
        y += 24;
        button("Choisir une image sur l'ordinateur…", x, y, w, b -> ImageFiles.chooseImage(this::loadImage, msg -> status = "§c" + msg));
        y += 24;
        PhoneUploadServer srv = PhoneUploadServer.current();
        button(srv == null ? "Recevoir depuis un téléphone / une tablette" : "Arrêter la réception téléphone", x, y, w, b -> {
            if (PhoneUploadServer.current() != null) {
                PhoneUploadServer.stopAll();
            } else {
                try {
                    PhoneUploadServer.start(this::onPhoneImage);
                } catch (IOException e) {
                    status = "§cRéception impossible : " + e.getMessage();
                }
            }
            rebuildWidgets();
        });
        y += 24;
        button("Ouvrir le dossier des images", x, y, w, b -> ImageFiles.openFolder(ImageFiles.images()));
        y += 24;
        if (S.images[S.slot.ordinal()] != null && S.slot != ViewSlot.FRONT) {
            button("Retirer la vue « " + S.slot.label + " »", x, y, w, b -> {
                int i = S.slot.ordinal();
                S.images[i] = null;
                S.analyses[i] = null;
                S.names[i] = null;
                textures.release("img" + i);
                imageUploaded[i] = false;
                rebuildWidgets();
            });
            y += 24;
        }
        // Liste des images du dossier (les plus récentes d'abord).
        files.clear();
        files.addAll(ImageFiles.listImages());
        int rows = Math.max(1, (cy + ch - y - 70) / 22);
        S.listScroll = Math.max(0, Math.min(S.listScroll, Math.max(0, files.size() - rows)));
        for (int i = 0; i < rows && S.listScroll + i < files.size(); i++) {
            Path p = files.get(S.listScroll + i);
            button(font.plainSubstrByWidth(p.getFileName().toString(), w - 10), x, y + 12 + i * 22, w, b -> loadImage(p));
        }
        if (files.size() > rows) {
            button("▲", x + w + 2, y + 12, 16, b -> {
                S.listScroll = Math.max(0, S.listScroll - rows);
                rebuildWidgets();
            });
            button("▼", x + w + 2, y + 12 + 22, 16, b -> {
                S.listScroll += rows;
                rebuildWidgets();
            });
        }
        // Projets enregistrés.
        projects = ImageBuildProject.list(ImageFiles.projects());
        int py = cy + ch - 46;
        int shown = Math.min(projects.size(), 3);
        for (int i = 0; i < shown; i++) {
            String folder = projects.get(projects.size() - 1 - i);
            button("Projet : " + folder, x + (w / 3 + 2) * i, py + 22, w / 3, b -> loadProject(folder));
        }
    }

    private void initAnalysis() {
        ImageAnalysis.Result a = S.analyses[0];
        int w = half();
        button("Sujet : " + S.subject.label + (S.subjectChosen ? " (choisi)" : " (estimé)"), cx, cy, w, b -> {
            S.subject = ImageAnalysis.Subject.values()[(S.subject.ordinal() + 1) % ImageAnalysis.Subject.values().length];
            S.subjectChosen = true;
            rebuildWidgets();
        });
        if (S.subjectChosen && a != null) {
            button("Revenir à l'estimation", cx, cy + 24, w, b -> {
                S.subject = a.best().subject();
                S.subjectChosen = false;
                rebuildWidgets();
            });
        }
        button("Étape suivante : options →", cx, cy + ch - 22, w, b -> {
            S.tab = Tab.OPTIONS;
            rebuildWidgets();
        });
    }

    private void initOptions() {
        int w = half(), x = cx, y = cy;
        button("Reconstruction : " + S.mode.label, x, y, w, b -> {
            S.mode = Reconstructor.Mode.values()[(S.mode.ordinal() + 1) % Reconstructor.Mode.values().length];
            rebuildWidgets();
        });
        y += 22;
        button(S.back.label, x, y, w, b -> {
            S.back = Reconstructor.BackMode.values()[(S.back.ordinal() + 1) % Reconstructor.BackMode.values().length];
            rebuildWidgets();
        });
        y += 22;
        button("Style : " + S.style.label, x, y, w, b -> {
            S.style = BuildStyle.values()[(S.style.ordinal() + 1) % BuildStyle.values().length];
            rebuildWidgets();
        });
        y += 22;
        button("Détail : " + S.detail.label + " (" + S.detail.maxSide + " max)", x, y, w, b -> {
            S.detail = Reconstructor.Detail.values()[(S.detail.ordinal() + 1) % Reconstructor.Detail.values().length];
            rebuildWidgets();
        });
        y += 22;
        button("Intérieur creux : " + (S.hollow ? "oui" : "non"), x, y, w / 2 - 2, b -> {
            S.hollow = !S.hollow;
            rebuildWidgets();
        });
        button("Relief inversé : " + (S.invertRelief ? "oui" : "non"), x + w / 2 + 2, y, w / 2 - 2, b -> {
            S.invertRelief = !S.invertRelief;
            rebuildWidgets();
        });
        y += 22;
        button("Vue de côté : avant à " + (S.sideFlipped ? "droite" : "gauche"), x, y, w, b -> {
            S.sideFlipped = !S.sideFlipped;
            rebuildWidgets();
        });
        y += 26;
        int bw = (w - 8) / 3;
        heightBox = numberBox(x, y + 10, bw, S.height, "Hauteur");
        widthBox = numberBox(x + bw + 4, y + 10, bw, S.width, "Largeur");
        depthBox = numberBox(x + 2 * (bw + 4), y + 10, bw, S.depth, "Profondeur");
        y += 36;
        if (S.style == BuildStyle.PERSONNALISE) {
            int col = 0;
            for (BlockPalette.Family f : BlockPalette.Family.values()) {
                boolean on = S.families.contains(f);
                button((on ? "§a✔ " : "§7✘ ") + familyLabel(f), x + (col % 3) * (bw + 4), y + (col / 3) * 22, bw, b -> {
                    if (!S.families.remove(f)) {
                        S.families.add(f);
                    }
                    rebuildWidgets();
                });
                col++;
            }
        }
        Button gen = button("§lGÉNÉRER LE MODÈLE", cx, cy + ch - 22, w, b -> generate());
        gen.active = !busy && S.analyses[0] != null;
    }

    private EditBox numberBox(int x, int y, int w, int value, String hint) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(hint));
        box.setMaxLength(3);
        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(box);
        return box;
    }

    private void initPreview() {
        int x = cx, y = cy, w = 150;
        button("◀", x, y, 20, b -> {
            S.preview = (S.preview + PREVIEWS.length - 1) % PREVIEWS.length;
            rebuildWidgets();
        });
        button(PREVIEWS[S.preview], x + 22, y, w - 44, b -> {
            S.preview = (S.preview + 1) % PREVIEWS.length;
            rebuildWidgets();
        });
        button("▶", x + w - 20, y, 20, b -> {
            S.preview = (S.preview + 1) % PREVIEWS.length;
            rebuildWidgets();
        });
        y += 22;
        button("Estimés en orange : " + (S.highlight ? "oui" : "non"), x, y, w, b -> {
            S.highlight = !S.highlight;
            rebuildWidgets();
        });
        y += 22;
        if (S.preview == 9) {
            button("Couche −", x, y, w / 2 - 1, b -> S.layer = Math.max(0, S.layer - 1));
            button("Couche +", x + w / 2 + 1, y, w / 2 - 1, b -> S.layer = S.editor == null ? 0 : Math.min(S.editor.model().sizeY() - 1, S.layer + 1));
            y += 22;
        }
        y += 6;
        // Éditeur : toutes les opérations sont annulables.
        Button undo = button("Annuler", x, y, w / 2 - 1, b -> edit(ModelEditor::undo));
        Button redo = button("Rétablir", x + w / 2 + 1, y, w / 2 - 1, b -> edit(ModelEditor::redo));
        undo.active = S.editor != null && S.editor.canUndo();
        redo.active = S.editor != null && S.editor.canRedo();
        y += 22;
        button("Tourner ¼ tour", x, y, w / 2 - 1, b -> edit(e -> {
            e.rotate();
            return true;
        }));
        button("Miroir", x + w / 2 + 1, y, w / 2 - 1, b -> edit(e -> {
            e.mirror();
            return true;
        }));
        y += 22;
        button("Taille −20 %", x, y, w / 2 - 1, b -> edit(e -> {
            e.scale(0.8);
            return true;
        }));
        button("Taille +25 %", x + w / 2 + 1, y, w / 2 - 1, b -> edit(e -> {
            e.scale(1.25);
            return true;
        }));
        y += 22;
        button("Creuser l'intérieur", x, y, w, b -> edit(e -> e.hollow() > 0));
        y += 22;
        button("Retirer couche du haut", x, y, w, b -> edit(e -> e.cropLayers(0, e.model().sizeY() - 2)));
        y += 22;
        button("Retirer couche du bas", x, y, w, b -> edit(e -> e.cropLayers(1, e.model().sizeY() - 1)));
        for (var wd : renderables) {
            if (wd instanceof Button bt && bt.getY() >= cy + 44 && S.editor == null) {
                bt.active = false;
            }
        }
    }

    private void initMaterials() {
        if (S.editor == null) {
            return;
        }
        Map<String, Integer> mats = S.editor.model().materials();
        List<String> keys = new ArrayList<>(mats.keySet());
        int rows = Math.max(1, (ch - 30) / 22);
        S.matScroll = Math.max(0, Math.min(S.matScroll, Math.max(0, keys.size() - rows)));
        int bx = cx + Math.min(cw - 250, 300);
        for (int i = 0; i < rows && S.matScroll + i < keys.size(); i++) {
            String state = keys.get(S.matScroll + i);
            int y = cy + 14 + i * 22;
            List<String> alts = alternatives(state);
            int choice = Math.floorMod(S.replaceChoice.getOrDefault(state, 0), Math.max(1, alts.size()));
            if (!alts.isEmpty()) {
                String alt = alts.get(choice);
                button("→ " + label(alt), bx, y, 130, b -> {
                    S.replaceChoice.remove(state);
                    edit(e -> e.replace(state, alt) > 0);
                });
                button("↻", bx + 132, y, 20, b -> {
                    S.replaceChoice.put(state, choice + 1);
                    rebuildWidgets();
                });
            }
            button("Retirer", bx + 156, y, 60, b -> edit(e -> e.remove(state) > 0));
        }
        if (keys.size() > rows) {
            button("▲", cx + cw - 20, cy + 14, 18, b -> {
                S.matScroll = Math.max(0, S.matScroll - rows);
                rebuildWidgets();
            });
            button("▼", cx + cw - 20, cy + 36, 18, b -> {
                S.matScroll += rows;
                rebuildWidgets();
            });
        }
    }

    private void initPlan() {
        int w = half(), x = cx + cw - w, y = cy;
        nameBox = new EditBox(font, x, y + 10, w, 18, Component.literal("Nom"));
        nameBox.setMaxLength(48);
        nameBox.setValue(S.projectName);
        nameBox.setResponder(v -> S.projectName = v);
        addRenderableWidget(nameBox);
        y += 34;
        Button send = button("§lENVOYER AU SERVEUR ET CONSTRUIRE", x, y, w, b -> sendToServer());
        StatePayload st = ClientState.state();
        send.active = S.editor != null && !ModelSender.busy() && st != null && st.canImport();
        y += 24;
        button("Enregistrer le projet", x, y, w, b -> saveProject()).active = S.analyses[0] != null;
        y += 22;
        button("Exporter le plan (.txt)", x, y, w, b -> exportPlan()).active = S.editor != null;
        y += 22;
        button("Exporter le modèle (.uabm)", x, y, w, b -> exportModel()).active = S.editor != null;
        y += 22;
        button("Ouvrir le dossier des exports", x, y, w, b -> ImageFiles.openFolder(ImageFiles.exports()));
    }

    // ------------------------------------------------------------------ actions

    private void loadImage(Path file) {
        int slot = S.slot.ordinal();
        busy = true;
        status = "Chargement de " + file.getFileName() + "…";
        int gen = generation.incrementAndGet();
        WORKER.execute(() -> {
            try {
                ImageData img = ImageLoader.load(file);
                ImageAnalysis.Result a = ImageAnalysis.analyze(img);
                minecraft.execute(() -> {
                    if (gen != generation.get()) {
                        return;
                    }
                    S.images[slot] = img;
                    S.analyses[slot] = a;
                    S.names[slot] = file.getFileName().toString();
                    imageUploaded[slot] = false;
                    if (slot == 0) {
                        textures.release("mask");
                        textures.release("subject");
                        S.subject = a.best().subject();
                        S.subjectChosen = false;
                        S.height = defaultHeight(a);
                        S.tab = Tab.ANALYSIS;
                    }
                    busy = false;
                    status = "§aImage chargée : " + file.getFileName() + " (" + img.width() + "×" + img.height() + ")";
                    rebuildWidgets();
                });
            } catch (ImportException | RuntimeException e) {
                finish(gen, "§cImage refusée : " + e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ mode capture (CI)

    public void captureLoad(Path file) {
        S.slot = ViewSlot.FRONT;
        loadImage(file);
    }

    public void captureGenerate() {
        generate();
    }

    public void capturePreview(int view, boolean highlight) {
        S.tab = Tab.PREVIEW;
        S.preview = Math.floorMod(view, PREVIEWS.length);
        S.highlight = highlight;
        rebuildWidgets();
    }

    private void onPhoneImage(Path file) {
        status = "§aImage reçue du téléphone : " + file.getFileName();
        if (minecraft != null && minecraft.screen == this) {
            loadImage(file);
        }
    }

    private static int defaultHeight(ImageAnalysis.Result a) {
        if (a.pixelArt()) {
            // Pixel art : 1 pixel = 1 bloc quand la taille le permet.
            return Math.max(8, Math.min(128, a.boxHeight()));
        }
        return switch (a.best().subject()) {
            case BUILDING -> 32;
            case TREE -> 40;
            default -> 48;
        };
    }

    private void generate() {
        readBoxes();
        ImageAnalysis.Result front = S.analyses[0];
        if (front == null) {
            return;
        }
        Reconstructor.Options o = options();
        Reconstructor.Views v = new Reconstructor.Views(front, S.analyses[1], S.analyses[2], S.analyses[3]);
        busy = true;
        status = "Reconstruction en cours…";
        int gen = generation.incrementAndGet();
        WORKER.execute(() -> {
            try {
                long t0 = System.nanoTime();
                Reconstructor.Result r = Reconstructor.reconstruct(v, o);
                long ms = (System.nanoTime() - t0) / 1_000_000;
                minecraft.execute(() -> {
                    if (gen != generation.get()) {
                        return;
                    }
                    S.result = r;
                    S.usedOptions = o;
                    if (S.editor == null) {
                        S.editor = new ModelEditor(r.model());
                    } else {
                        S.editor.replaceAll(r.model());
                    }
                    S.layer = 0;
                    modelChanged();
                    busy = false;
                    status = "§aModèle généré en " + ms + " ms : " + r.model().count() + " blocs (" + r.usedMode().label + ")";
                    S.tab = Tab.PREVIEW;
                    rebuildWidgets();
                });
            } catch (RuntimeException e) {
                UltimateAutoBuilderMod.LOGGER.error("Reconstruction impossible", e);
                finish(gen, "§cReconstruction impossible : " + e.getMessage());
            }
        });
    }

    private Reconstructor.Options options() {
        return new Reconstructor.Options(S.mode, S.back, S.style, S.families, S.width, S.height, S.depth, S.detail, S.hollow, S.subject,
                S.invertRelief, S.sideFlipped);
    }

    private void readBoxes() {
        S.height = parse(heightBox, S.height, 1);
        S.width = parse(widthBox, S.width, 0);
        S.depth = parse(depthBox, S.depth, 0);
    }

    private static int parse(EditBox box, int def, int min) {
        if (box == null) {
            return def;
        }
        try {
            return Math.max(min, Math.min(ImageModel.MAX_SIDE, Integer.parseInt(box.getValue().trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void finish(int gen, String msg) {
        minecraft.execute(() -> {
            if (gen == generation.get()) {
                busy = false;
            }
            status = msg;
            rebuildWidgets();
        });
    }

    @FunctionalInterface
    private interface EditOp {
        boolean apply(ModelEditor e);
    }

    private void edit(EditOp op) {
        if (S.editor == null || busy) {
            return;
        }
        try {
            if (op.apply(S.editor)) {
                modelChanged();
                status = "Modèle : " + S.editor.model().sizeX() + "×" + S.editor.model().sizeY() + "×" + S.editor.model().sizeZ() + ", "
                        + S.editor.model().count() + " blocs";
            }
        } catch (RuntimeException e) {
            status = "§c" + e.getMessage();
        }
        rebuildWidgets();
    }

    private void modelChanged() {
        modelVersion++;
        ImageModel m = S.editor.model();
        S.layer = Math.min(S.layer, m.sizeY() - 1);
        report = BuildPlanReport.of(m, 0);
        comparison = S.analyses[0] == null ? null : ModelViews.compare(S.analyses[0], m);
    }

    private void sendToServer() {
        if (S.editor == null) {
            return;
        }
        String name = S.projectName == null || S.projectName.isBlank() ? "image" : S.projectName.trim();
        try {
            int n = ModelSender.send(name, S.editor.model().toVoxelModel());
            status = "§aEnvoi au serveur (" + n + " morceau" + (n > 1 ? "x" : "") + ")… Le modèle apparaîtra dans « Personnalisés ».";
        } catch (IOException | RuntimeException e) {
            status = "§cEnvoi impossible : " + e.getMessage();
        }
        rebuildWidgets();
    }

    private void saveProject() {
        readBoxes();
        String name = S.projectName == null || S.projectName.isBlank() ? "projet" : S.projectName.trim();
        Reconstructor.Options o = S.usedOptions != null ? S.usedOptions : options();
        ImageData[] imgs = S.images.clone();
        ImageModel model = S.editor == null ? null : S.editor.model();
        WORKER.execute(() -> {
            try {
                Path d = ImageBuildProject.save(ImageFiles.projects(), name, imgs[0], imgs[1], imgs[2], imgs[3], o, model);
                minecraft.execute(() -> status = "§aProjet enregistré : " + d.getFileName());
            } catch (IOException | RuntimeException e) {
                minecraft.execute(() -> status = "§cEnregistrement impossible : " + e.getMessage());
            }
        });
    }

    private void loadProject(String folder) {
        busy = true;
        status = "Ouverture du projet " + folder + "…";
        int gen = generation.incrementAndGet();
        WORKER.execute(() -> {
            try {
                ImageBuildProject.Loaded l = ImageBuildProject.load(ImageFiles.projects(), folder);
                ImageData[] imgs = {l.front(), l.side(), l.back(), l.top()};
                ImageAnalysis.Result[] an = new ImageAnalysis.Result[4];
                for (int i = 0; i < 4; i++) {
                    an[i] = imgs[i] == null ? null : ImageAnalysis.analyze(imgs[i]);
                }
                minecraft.execute(() -> {
                    if (gen != generation.get()) {
                        return;
                    }
                    for (int i = 0; i < 4; i++) {
                        S.images[i] = imgs[i];
                        S.analyses[i] = an[i];
                        S.names[i] = imgs[i] == null ? null : folder;
                        imageUploaded[i] = false;
                    }
                    textures.release("mask");
                    textures.release("subject");
                    Reconstructor.Options o = l.options();
                    S.mode = o.mode();
                    S.back = o.back();
                    S.style = o.style();
                    S.families = o.customFamilies().isEmpty() ? EnumSet.of(BlockPalette.Family.CONCRETE) : EnumSet.copyOf(o.customFamilies());
                    S.width = o.width();
                    S.height = o.height();
                    S.depth = o.depth();
                    S.detail = o.detail();
                    S.hollow = o.hollow();
                    S.subject = o.subject();
                    S.subjectChosen = true;
                    S.invertRelief = o.invertRelief();
                    S.sideFlipped = o.sideFlipped();
                    S.projectName = l.name();
                    S.usedOptions = o;
                    S.result = null;
                    S.editor = l.model() == null ? null : new ModelEditor(l.model());
                    if (S.editor != null) {
                        modelChanged();
                    }
                    busy = false;
                    S.tab = S.editor != null ? Tab.PREVIEW : Tab.OPTIONS;
                    status = "§aProjet ouvert : " + l.name();
                    rebuildWidgets();
                });
            } catch (ImportException | RuntimeException e) {
                finish(gen, "§c" + e.getMessage());
            }
        });
    }

    private void exportPlan() {
        ImageModel m = S.editor.model();
        String name = S.projectName;
        WORKER.execute(() -> {
            try {
                Path f = ImageFiles.exports().resolve(ImageBuildProject.folderName(name) + "_plan.txt");
                Files.writeString(f, BuildPlanReport.textPlan(m, name), StandardCharsets.UTF_8);
                minecraft.execute(() -> status = "§aPlan exporté : " + f.getFileName());
            } catch (IOException | RuntimeException e) {
                minecraft.execute(() -> status = "§cExport impossible : " + e.getMessage());
            }
        });
    }

    private void exportModel() {
        ImageModel m = S.editor.model();
        String name = S.projectName;
        WORKER.execute(() -> {
            try {
                Path f = ImageFiles.exports().resolve(ImageBuildProject.folderName(name) + ".uabm");
                try (OutputStream out = Files.newOutputStream(f)) {
                    ModelCodec.write(out, name, m.toVoxelModel());
                }
                minecraft.execute(() -> status = "§aModèle exporté : " + f.getFileName()
                        + " (copiez-le dans config/ultimateautobuilder/import d'un serveur pour l'importer)");
            } catch (IOException | RuntimeException e) {
                minecraft.execute(() -> status = "§cExport impossible : " + e.getMessage());
            }
        });
    }

    /** Matériaux de couleur proche (même palette complète), du plus proche au plus éloigné. */
    private static List<String> alternatives(String state) {
        int c = BlockPalette.colorOf(state);
        List<BlockPalette.Entry> all = new ArrayList<>(BlockPalette.full().entries());
        all.removeIf(e -> e.state().equals(state));
        if (c >= 0) {
            all.sort((a, b) -> Double.compare(BlockPalette.deltaE(a.rgb(), c), BlockPalette.deltaE(b.rgb(), c)));
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(8, all.size()); i++) {
            out.add(all.get(i).state());
        }
        return out;
    }

    private static String label(String state) {
        String id = state.contains("[") ? state.substring(0, state.indexOf('[')) : state;
        return id.startsWith("minecraft:") ? id.substring(10) : id;
    }

    private static String familyLabel(BlockPalette.Family f) {
        return switch (f) {
            case CONCRETE -> "Béton";
            case WOOL -> "Laine";
            case TERRACOTTA -> "Terre cuite";
            case STONE -> "Pierres";
            case WOOD -> "Bois";
            case NATURAL -> "Naturels";
            case METAL -> "Métaux";
            case NETHER_END -> "Nether / End";
            case GLASS -> "Verre";
        };
    }

    // ------------------------------------------------------------------ rendu

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.fill(cx - 4, cy - 4, cx + cw + 4, cy + ch + 4, 0x90101418);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawString(font, "§b❄ §fImage → Construction §7— reconstruction locale (aucune IA en ligne, aucune donnée envoyée hors du jeu)", 8, 5,
                0xFFFFFFFF);
        String st = status + (ModelSender.busy() ? " §7(envoi " + ModelSender.progress() + ")" : "");
        g.drawString(font, font.plainSubstrByWidth(st, width - 110), 8, height - 18, 0xFFE0E0E0);
        if (busy) {
            g.drawString(font, "§e⏳ Calcul en cours…", width - 200, 5, 0xFFFFFFFF);
        }
        switch (S.tab) {
            case IMPORT -> renderImport(g);
            case ANALYSIS -> renderAnalysis(g);
            case OPTIONS -> renderOptions(g);
            case PREVIEW -> renderPreview(g);
            case MATERIALS -> renderMaterials(g);
            case PLAN -> renderPlan(g);
        }
    }

    private void uploadImages() {
        for (int i = 0; i < 4; i++) {
            if (S.images[i] != null && !imageUploaded[i]) {
                ImageData img = S.images[i].downscale(256);
                textures.put("img" + i, img.width(), img.height(), img.argb());
                imageUploaded[i] = true;
            }
        }
    }

    private void renderImport(GuiGraphics g) {
        uploadImages();
        int w = half();
        int listY = cy + 24 * (S.images[S.slot.ordinal()] != null && S.slot != ViewSlot.FRONT ? 5 : 4);
        g.drawString(font, "§7Images du dossier (cliquez pour l'utiliser comme vue « " + S.slot.label + " ») :", cx, listY, 0xFFFFFFFF);
        if (files.isEmpty()) {
            g.drawString(font, "§8(aucune image : déposez des PNG/JPEG dans le dossier)", cx, listY + 14, 0xFFFFFFFF);
        }
        if (!projects.isEmpty()) {
            g.drawString(font, "§7Projets enregistrés récents :", cx, cy + ch - 34, 0xFFFFFFFF);
        }
        int px = cx + w + 26, pw = cx + cw - px;
        PhoneUploadServer srv = PhoneUploadServer.current();
        int y = cy;
        if (srv != null) {
            g.drawString(font, "§aRéception active §7(" + srv.secondsLeft() / 60 + " min restantes)", px, y, 0xFFFFFFFF);
            g.drawString(font, "§fSur le téléphone (même Wi-Fi), ouvrez :", px, y + 12, 0xFFFFFFFF);
            g.drawString(font, "§e" + srv.url(), px, y + 24, 0xFFFFFFFF);
            g.drawString(font, "§7Le pare-feu peut demander une autorisation.", px, y + 36, 0xFFFFFFFF);
            y += 52;
        }
        int bw = Math.max(40, (pw - 12) / 4), bh = Math.min(bw, cy + ch - y - 30);
        for (int i = 0; i < 4; i++) {
            int x = px + i * (bw + 4);
            g.fill(x, y + 12, x + bw, y + 12 + bh, ViewSlot.values()[i] == S.slot ? 0x60406080 : 0x40000000);
            g.drawString(font, ViewSlot.values()[i].label + (i == 0 ? " §c*" : ""), x + 2, y + 2, 0xFFFFFFFF);
            if (!textures.draw(g, "img" + i, x + 2, y + 14, bw - 4, bh - 4)) {
                g.drawCenteredString(font, "§8vide", x + bw / 2, y + 12 + bh / 2 - 4, 0xFFFFFFFF);
            }
        }
        g.drawString(font, "§7* vue obligatoire. Côté / arrière / dessus : facultatives (reconstruction multi-vues).", px, y + bh + 18, 0xFFFFFFFF);
        g.drawString(font, "§7Formats : PNG, JPEG, BMP, GIF (25 Mo, 16 384 px max). WebP/HEIC : convertir en PNG/JPEG.", px, y + bh + 30, 0xFFFFFFFF);
    }

    private void renderAnalysis(GuiGraphics g) {
        ImageAnalysis.Result a = S.analyses[0];
        if (a == null) {
            return;
        }
        uploadImages();
        int px = cx + half() + 10, pw = cx + cw - px;
        if (!textures.has("mask")) {
            int[] argb = new int[a.width() * a.height()];
            for (int i = 0; i < argb.length; i++) {
                int c = a.image().argb()[i];
                // Sujet en couleurs, fond assombri : le joueur voit ce qui a été retenu.
                argb[i] = a.mask()[i] ? (c | 0xFF000000) : 0xFF000000 | ((c >> 2) & 0x3F3F3F);
            }
            textures.put("mask", a.width(), a.height(), argb);
        }
        textures.draw(g, "mask", px, cy, pw, ch - 20);
        int y = cy + 50;
        List<String> lines = new ArrayList<>();
        lines.add("§fImage : §7" + S.names[0] + " (" + S.images[0].width() + "×" + S.images[0].height() + ")");
        lines.add("§fFond : §7" + switch (a.background()) {
            case ALPHA -> "transparence de l'image";
            case BORDER -> "séparé par la couleur des bords";
            case NONE -> "non séparé (image entière)";
        });
        lines.add(String.format(Locale.ROOT, "§fSujet : §7%d×%d px, rempli à %.0f %%, symétrie %.0f %%", a.boxWidth(), a.boxHeight(), a.fillRatio() * 100,
                a.symmetry() * 100));
        lines.add(String.format(Locale.ROOT, "§fLuminosité %.0f %%, contraste %.0f %%, netteté %.3f, saturation %.0f %%", a.brightness() * 100,
                a.contrast() * 100, a.sharpness(), a.saturation() * 100));
        lines.add("§fCouleurs distinctes : §7" + (a.distinctColors() > 512 ? "> 512" : a.distinctColors()) + (a.pixelArt() ? " (pixel art)" : ""));
        lines.add("§fEstimations (heuristiques locales) :");
        for (int i = 0; i < Math.min(3, a.guesses().size()); i++) {
            ImageAnalysis.Guess gu = a.guesses().get(i);
            lines.add(String.format(Locale.ROOT, "§7 %s %.0f %% — %s", gu.subject().label, gu.confidence() * 100, gu.reason()));
        }
        for (String wmsg : a.warnings()) {
            lines.add("§6⚠ " + wmsg);
        }
        for (String l : lines) {
            for (var seq : font.split(Component.literal(l), half())) {
                g.drawString(font, seq, cx, y, 0xFFFFFFFF);
                y += 10;
            }
            y += 2;
        }
    }

    private void renderOptions(GuiGraphics g) {
        int bw = (half() - 8) / 3;
        int y = cy + 22 * 6 + 4;
        g.drawString(font, "§7Hauteur", cx, y, 0xFFFFFFFF);
        g.drawString(font, "§7Largeur (0 = auto)", cx + bw + 4, y, 0xFFFFFFFF);
        g.drawString(font, "§7Profondeur (0 = auto)", cx + 2 * (bw + 4), y, 0xFFFFFFFF);
        int px = cx + half() + 12, pw = cx + cw - px;
        List<String> help = new ArrayList<>();
        help.add("§fStyle : §7" + S.style.description);
        help.add("§fReconstruction : §7" + switch (S.mode) {
            case AUTO -> "choisie selon le sujet (bâtiment → façade, pixel art → plat, paysage → relief, sinon volume)";
            case INFLATE -> "volume arrondi, épaisseur déduite de la distance au contour";
            case RELIEF -> "bas-relief, la luminosité fixe la saillie";
            case FLAT -> "image extrudée sur la profondeur choisie";
            case BUILDING -> "façade extrudée, toits en pignon conservés, intérieur vidé avec plancher";
            case MULTIVIEW -> "intersection des silhouettes avant / côté / dessus (vue de côté requise)";
        });
        help.add("§fSujet retenu : §7" + S.subject.label);
        int views = 0;
        for (int i = 1; i < 4; i++) {
            views += S.analyses[i] != null ? 1 : 0;
        }
        help.add("§fVues complémentaires : §7" + views + (S.analyses[1] == null ? " (sans vue de côté, la profondeur est estimée)" : ""));
        help.add("§7Les blocs « vus » viennent directement d'un pixel ; les autres sont estimés et listés comme tels dans l'aperçu.");
        int hy = cy;
        for (String l : help) {
            for (var seq : font.split(Component.literal(l), pw)) {
                g.drawString(font, seq, px, hy, 0xFFFFFFFF);
                hy += 10;
            }
            hy += 4;
        }
    }

    private void renderPreview(GuiGraphics g) {
        if (S.editor == null) {
            g.drawCenteredString(font, "§7Générez d'abord un modèle (onglet Options).", cx + cw / 2, cy + ch / 2, 0xFFFFFFFF);
            return;
        }
        int px = previewX(), pw = previewW();
        g.fill(px, cy, px + pw, cy + ch, 0x50000000);
        boolean drawn;
        if (S.preview == 10) {
            // Comparaison : photo à gauche, face avant texturée du modèle à droite.
            int half = (pw - 6) / 2;
            uploadSubject();
            textures.draw(g, "subject", px + 2, cy + 2, half, ch - 16);
            drawn = drawMesh(g, "full", px + half + 4, cy + 2, half, ch - 16, 180f, 0f, 1f);
            g.drawString(font, "§7Gauche : sujet de l'image — droite : face avant du modèle", px + 4, cy + ch - 12, 0xFFFFFFFF);
        } else if (S.preview == 9) {
            drawn = drawMesh(g, "layer", px + 2, cy + 2, pw - 4, ch - 16, 180f, 90f, previewZoom);
            g.drawString(font, "§7Couche Y=" + S.layer + " (vue de dessus, avant en bas) — molette : couche", px + 4, cy + ch - 12, 0xFFFFFFFF);
        } else {
            float yaw, pitch;
            switch (S.preview) {
                case 4 -> { yaw = 180f; pitch = 0f; }
                case 5 -> { yaw = 0f; pitch = 0f; }
                case 6 -> { yaw = 90f; pitch = 0f; }
                case 7 -> { yaw = -90f; pitch = 0f; }
                case 8 -> { yaw = 180f; pitch = 90f; }
                default -> { yaw = 180f + 35f - 90f * S.preview + previewYaw; pitch = previewPitch; }
            }
            drawn = drawMesh(g, S.highlight ? "estimated" : "full", px + 2, cy + 2, pw - 4, ch - 16, yaw, pitch, previewZoom);
            g.drawString(font, S.preview < 4 ? "§7glisser : tourner · molette : zoom" + (S.highlight ? " · §6verre orange = blocs estimés" : "")
                    : "§7molette : zoom" + (S.highlight ? " · §6verre orange = blocs estimés" : ""), px + 4, cy + ch - 12, 0xFFFFFFFF);
        }
        if (!drawn) {
            g.drawCenteredString(font, "§7Préparation de l'aperçu 3D…", px + pw / 2, cy + ch / 2, 0xFFFFFFFF);
        }
        // Panneau d'informations.
        int ix = px + pw + 8, iw = cx + cw - ix, y = cy;
        ImageModel m = S.editor.model();
        List<String> lines = new ArrayList<>();
        lines.add("§fDimensions : §7" + m.sizeX() + " × " + m.sizeY() + " × " + m.sizeZ());
        if (report != null) {
            lines.add("§fBlocs : §7" + report.blocks());
            lines.add("§a■ §fvus : §7" + report.visible() + "  §6■ §festimés : §7" + report.estimated());
        }
        if (comparison != null) {
            lines.add(String.format(Locale.ROOT, "§fSilhouette commune : §7%.0f %%", comparison.silhouetteOverlap() * 100));
            lines.add(String.format(Locale.ROOT, "§fÉcart de couleur moyen : §7ΔE %.1f", comparison.meanDeltaE()));
        }
        if (S.result != null) {
            lines.add("§fIncertitudes :");
            for (String u : S.result.uncertainties()) {
                lines.add("§6• §7" + u);
            }
            for (String n : S.result.notes()) {
                lines.add("§b• §7" + n);
            }
            lines.add("§7Corrigez-les dans Options (arrière, profondeur, sujet, vues).");
        }
        for (String l : lines) {
            for (var seq : font.split(Component.literal(l), iw)) {
                if (y > cy + ch - 10) {
                    return;
                }
                g.drawString(font, seq, ix, y, 0xFFFFFFFF);
                y += 10;
            }
        }
    }

    private int previewX() {
        return cx + 160;
    }

    private int previewW() {
        return cx + cw - previewX() - 170;
    }

    private boolean overPreview(double mx, double my) {
        return S.tab == Tab.PREVIEW && S.editor != null && mx >= previewX() && mx < previewX() + previewW() && my >= cy && my < cy + ch;
    }

    /**
     * Rendu texturé (vrais blocs) du modèle courant. Variantes : modèle complet, blocs estimés
     * remplacés par du verre orange, ou couche seule.
     */
    private boolean drawMesh(GuiGraphics g, String variant, int x, int y, int w, int h, float yaw, float pitch, float zoom) {
        ImageModel m = S.editor.model();
        int version = modelVersion, layer = S.layer;
        String key = variant + ":" + version + (variant.equals("layer") ? ":" + layer : "");
        BlockMesh mesh = meshes.get(key, () -> {
            ImageModel src = switch (variant) {
                case "layer" -> m.cropLayers(layer, layer);
                case "estimated" -> estimatedAsGlass(m);
                default -> m;
            };
            return BlockMesh.build(BlockMesh.of(ModelPreviews.lod(src.toVoxelModel(), 128)), 1f, 500_000);
        });
        if (mesh == null) {
            return false;
        }
        mesh.drawInGui(g, x, y, w, h, yaw, pitch, zoom);
        return true;
    }

    /** Copie où chaque bloc estimé devient du verre orange : on voit ce qui provient réellement de l'image. */
    private static ImageModel estimatedAsGlass(ImageModel m) {
        ImageModel out = m.copy();
        for (int y = 0; y < m.sizeY(); y++) {
            for (int z = 0; z < m.sizeZ(); z++) {
                for (int x = 0; x < m.sizeX(); x++) {
                    if (m.get(x, y, z) != null && m.certainty(x, y, z) == ImageModel.ESTIMATED) {
                        out.set(x, y, z, "minecraft:orange_stained_glass", ImageModel.ESTIMATED);
                    }
                }
            }
        }
        return out;
    }

    private void uploadSubject() {
        ImageAnalysis.Result a = S.analyses[0];
        if (a != null && !textures.has("subject")) {
            ImageData img = ModelViews.cropSubject(a);
            textures.put("subject", img.width(), img.height(), img.argb());
        }
    }

    private void renderMaterials(GuiGraphics g) {
        if (S.editor == null) {
            g.drawCenteredString(font, "§7Générez d'abord un modèle.", cx + cw / 2, cy + ch / 2, 0xFFFFFFFF);
            return;
        }
        Map<String, Integer> mats = S.editor.model().materials();
        g.drawString(font, "§f" + mats.size() + " matériaux — §7remplacement par une couleur proche (↻ pour changer de proposition)", cx, cy, 0xFFFFFFFF);
        List<String> keys = new ArrayList<>(mats.keySet());
        int rows = Math.max(1, (ch - 30) / 22);
        for (int i = 0; i < rows && S.matScroll + i < keys.size(); i++) {
            String s = keys.get(S.matScroll + i);
            int y = cy + 14 + i * 22;
            int c = BlockPalette.colorOf(s);
            g.fill(cx, y + 2, cx + 16, y + 18, 0xFF000000 | (c < 0 ? 0x404040 : c));
            int count = mats.get(s);
            g.drawString(font, label(s) + " §7× " + count + " (" + (count + 63) / 64 + " piles)", cx + 22, y + 6, 0xFFFFFFFF);
        }
    }

    private void renderPlan(GuiGraphics g) {
        int w = half();
        g.drawString(font, "§7Nom du modèle / projet :", cx + cw - w, cy, 0xFFFFFFFF);
        if (S.editor == null || report == null) {
            g.drawString(font, "§7Générez d'abord un modèle.", cx, cy, 0xFFFFFFFF);
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("§fDimensions : §7" + report.width() + " (X) × " + report.height() + " (Y) × " + report.depth() + " (Z)");
        lines.add("§fBlocs : §7" + report.blocks() + " (" + report.materials().size() + " matériaux)");
        int layers = 0;
        for (int c : report.layers()) {
            layers += c > 0 ? 1 : 0;
        }
        lines.add("§fCouches occupées : §7" + layers);
        lines.add("§fÉtapes (mode « par étapes » du moteur) :");
        for (BuildPlanReport.Step s : report.steps()) {
            lines.add("§7 " + s.index() + ". couches " + s.fromY() + " à " + s.toY() + " — " + s.blocks() + " blocs");
        }
        lines.add("§fMatériaux principaux :");
        for (int i = 0; i < Math.min(6, report.materials().size()); i++) {
            BuildPlanReport.Material mt = report.materials().get(i);
            lines.add("§7 " + mt.label() + " × " + mt.count() + " (" + mt.stacks() + " piles)");
        }
        lines.add("");
        lines.add("§7Après l'envoi, le modèle se construit comme les autres : menu du cristal → Personnalisés, avec aperçu, rotation,");
        lines.add("§7étapes (couche par couche ou par étapes), pause/reprise, annulation et retour arrière.");
        StatePayload st = ClientState.state();
        if (st != null && !st.canImport()) {
            lines.add("§cCe serveur ne vous autorise pas à ajouter des modèles (permission d'import).");
        }
        int y = cy;
        for (String l : lines) {
            for (var seq : font.split(Component.literal(l), cw - w - 16)) {
                g.drawString(font, seq, cx, y, 0xFFFFFFFF);
                y += 10;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button == 0 && overPreview(mx, my) && S.preview < 4) {
            previewDragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (previewDragging && button == 0) {
            previewYaw += (float) dx * 1.2f;
            previewPitch = Math.max(-89f, Math.min(89f, previewPitch + (float) dy * 1.2f));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (previewDragging && button == 0) {
            previewDragging = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (overPreview(mx, my) && S.preview != 9 && sy != 0) {
            previewZoom = Math.max(0.4f, Math.min(4f, previewZoom * (sy > 0 ? 1.15f : 1 / 1.15f)));
            return true;
        }
        if (S.tab == Tab.PREVIEW && S.preview == 9 && S.editor != null) {
            S.layer = Math.max(0, Math.min(S.editor.model().sizeY() - 1, S.layer + (sy > 0 ? 1 : -1)));
            return true;
        }
        if (S.tab == Tab.MATERIALS) {
            S.matScroll = Math.max(0, S.matScroll + (sy > 0 ? -1 : 1));
            rebuildWidgets();
            return true;
        }
        if (S.tab == Tab.IMPORT) {
            S.listScroll = Math.max(0, S.listScroll + (sy > 0 ? -1 : 1));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    protected void rebuildWidgets() {
        if (S.tab == Tab.OPTIONS) {
            readBoxes();
        }
        if (S.tab != Tab.ANALYSIS) {
            textures.release("mask");
        }
        super.rebuildWidgets();
    }

    @Override
    public void onClose() {
        if (S.tab == Tab.OPTIONS) {
            readBoxes();
        }
        PhoneUploadServer.stopAll();
        textures.releaseAll();
        meshes.clear();
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        textures.releaseAll();
        meshes.clear();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
