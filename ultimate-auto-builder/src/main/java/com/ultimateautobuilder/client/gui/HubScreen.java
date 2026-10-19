package com.ultimateautobuilder.client.gui;

import com.ultimateautobuilder.client.ClientData;
import com.ultimateautobuilder.client.ClientProgress;
import com.ultimateautobuilder.client.ClientState;
import com.ultimateautobuilder.client.KeyBindings;
import com.ultimateautobuilder.client.image.ImageBuildScreen;
import com.ultimateautobuilder.client.render.ModelPreviews;
import com.ultimateautobuilder.client.visual.PostEffects;
import com.ultimateautobuilder.client.visual.VisualProfile;
import com.ultimateautobuilder.client.visual.VisualStore;
import com.ultimateautobuilder.client.visual.VisualStudioScreen;
import com.ultimateautobuilder.core.catalog.Catalog;
import com.ultimateautobuilder.core.catalog.Category;
import com.ultimateautobuilder.core.catalog.ModelEntry;
import com.ultimateautobuilder.core.catalog.models.DefaultCatalog;
import com.ultimateautobuilder.core.progression.ItemCost;
import com.ultimateautobuilder.core.progression.PlayerProgress;
import com.ultimateautobuilder.core.progression.ProgressionRules;
import com.ultimateautobuilder.core.progression.Quest;
import com.ultimateautobuilder.core.progression.QuestBook;
import com.ultimateautobuilder.core.progression.Tier;
import com.ultimateautobuilder.core.progression.UnlockRule;
import com.ultimateautobuilder.network.ActionPayload;
import com.ultimateautobuilder.network.ProgressPayload;
import com.ultimateautobuilder.network.StatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Hub de l'Ultimate Crystal : point d'entrée unique vers les maisons, les shaders gratuits, la
 * personnalisation, les quêtes, les matériaux, les constructions débloquées, les favoris, les
 * projets, l'historique et les paramètres. Tout ce qui est affiché vient de l'état envoyé par le
 * serveur ou de l'inventaire réel du joueur ; les actions (déblocage, récompense) sont revérifiées
 * côté serveur.
 */
public final class HubScreen extends ScaledScreen {
    private enum Section {
        HOME("Accueil"), HOUSES("1. Maisons"), SHADERS("2. Shaders gratuits"), CUSTOMIZE("3. Personnalisation"), QUESTS("4. Quêtes"),
        MATERIALS("5. Matériaux"), UNLOCKED("6. Débloquées"), FAVORITES("7. Favoris"), PROJECTS("8. Projets"), HISTORY("9. Historique"),
        SETTINGS("10. Paramètres");

        final String label;

        Section(String label) {
            this.label = label;
        }
    }

    /** Matériaux suivis (déblocages et paliers). */
    private static final String[] MATERIALS = {"minecraft:cobblestone", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond",
            "minecraft:netherite_ingot", "minecraft:copper_ingot", "minecraft:quartz", "minecraft:cherry_log", "minecraft:cherry_planks",
            "minecraft:pink_petals", "minecraft:lantern", "minecraft:oak_log", "minecraft:spruce_log", "minecraft:bamboo"};
    private static final int HEADER_H = 34, SIDE_W = 132, ROW_H = 36, CARD = 64;

    private static Section section = Section.HOME;

    private final Screen parent;
    private int mainX, mainY, mainW, mainH;
    private int questScroll;
    private long progressVersion = -1, stateVersion = -1;
    /** Zones cliquables dessinées à la main (cartes, boutons de quêtes). */
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Integer> inventory = new HashMap<>();
    private long inventoryAt;

    private record Hit(int x0, int y0, int x1, int y1, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    public HubScreen(Screen parent) {
        super(Component.literal("Ultimate Crystal — Creative Architect"));
        this.parent = parent;
    }

    /** Ouverture depuis le cristal : compte pour la quête « Découvrir l'Ultimate Crystal ». */
    public static HubScreen openedFromCrystal() {
        PacketDistributor.sendToServer(new ActionPayload(ActionPayload.REQUEST_STATE, 0, ""));
        PacketDistributor.sendToServer(new ActionPayload(ActionPayload.OPENED, 0, ""));
        return new HubScreen(null);
    }

    // ------------------------------------------------------------------ construction

    @Override
    protected void initScaled() {
        progressVersion = ClientProgress.version();
        stateVersion = ClientState.version();
        int y = HEADER_H + 4;
        int bh = Math.max(16, Math.min(22, (height - HEADER_H - 36) / Section.values().length));
        for (Section s : Section.values()) {
            Button b = Button.builder(Component.literal((s == section ? "§e▶ " : "") + s.label), x -> select(s))
                    .bounds(6, y, SIDE_W - 6, bh - 2).build();
            addRenderableWidget(b);
            y += bh;
        }
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose()).bounds(6, height - 24, SIDE_W - 6, 20).build());
        mainX = SIDE_W + 8;
        mainY = HEADER_H + 4;
        mainW = width - mainX - 6;
        mainH = height - mainY - 6;
        switch (section) {
            case HOUSES -> initHouses();
            case SHADERS -> initShaders();
            case CUSTOMIZE -> initCustomize();
            case UNLOCKED -> addAction("Ouvrir les constructions débloquées", () -> open(new BuilderScreen(this, "unlocked")));
            case FAVORITES -> addAction("Ouvrir les favoris", () -> open(new BuilderScreen(this, "favorites")));
            case PROJECTS -> initProjects();
            case HISTORY -> addAction("Ouvrir l'historique complet", () -> open(new BuilderScreen(this, "history")));
            case SETTINGS -> initSettings();
            case HOME -> addAction("Parcourir les maisons", () -> select(Section.HOUSES));
            default -> {
            }
        }
    }

    private void select(Section s) {
        section = s;
        questScroll = 0;
        rebuildWidgets();
    }

    /** Vérification visuelle automatique (captures en intégration continue). */
    public void captureSection(int index) {
        select(Section.values()[Math.max(0, Math.min(Section.values().length - 1, index))]);
    }

    private void open(Screen s) {
        minecraft.setScreen(s);
    }

    /** Bouton d'action principal, en bas de la zone centrale. */
    private void addAction(String label, Runnable r) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> r.run()).bounds(mainX + 6, height - 30, Math.min(260, mainW - 12), 20).build());
    }

    /** Grille de boutons dans la zone centrale, à partir de la hauteur {@code top}. */
    private void buttonGrid(int top, int cols, List<String> labels, List<String> tips, Consumer<Integer> onClick) {
        int gap = 4;
        int w = (mainW - 12 - gap * (cols - 1)) / cols;
        for (int i = 0; i < labels.size(); i++) {
            int idx = i;
            Button.Builder b = Button.builder(Component.literal(labels.get(i)), x -> onClick.accept(idx))
                    .bounds(mainX + 6 + (i % cols) * (w + gap), top + (i / cols) * 22, w, 20);
            if (tips != null && tips.get(i) != null) {
                b.tooltip(Tooltip.create(Component.literal(tips.get(i))));
            }
            addRenderableWidget(b.build());
        }
    }

    private int columns() {
        return mainW >= 460 ? 3 : 2;
    }

    private void initHouses() {
        Catalog cat = DefaultCatalog.shared();
        Map<Category, int[]> counts = new EnumMap<>(Category.class);
        for (ModelEntry e : cat.all()) {
            int[] c = counts.computeIfAbsent(e.category(), k -> new int[2]);
            c[0]++;
            if (ClientProgress.isUnlocked(e)) {
                c[1]++;
            }
        }
        List<String> labels = new ArrayList<>(), tips = new ArrayList<>();
        List<Category> order = new ArrayList<>();
        for (Category c : Category.values()) {
            int[] n = counts.get(c);
            if (n == null || c == Category.CUSTOM) {
                continue;
            }
            order.add(c);
            labels.add((c == Category.SAKURA ? "§d✿ " : "") + c.displayName() + " (" + n[1] + "/" + n[0] + ")");
            tips.add(n[1] + " construction(s) accessibles sur " + n[0]);
        }
        buttonGrid(mainY + 26 + CARD + 18, columns(), labels, tips, i -> open(new BuilderScreen(this, "cat:" + order.get(i).key())));
        addAction("Catalogue complet", () -> open(new BuilderScreen(this, "houses")));
    }

    private void initShaders() {
        VisualProfile.Quality[] q = VisualProfile.Quality.values();
        List<String> labels = new ArrayList<>(), tips = new ArrayList<>();
        for (VisualProfile.Quality v : q) {
            labels.add(v.label);
            tips.add(switch (v) {
                case PERFORMANCE -> "Étalonnage des couleurs seul (une passe) : impact minimal";
                case BALANCED -> "Étalonnage + vignette + netteté";
                case HIGH -> "Ajoute un halo lumineux (bloom)";
                case ULTRA -> "Halo large en deux itérations : le plus coûteux";
            });
        }
        labels.add("Personnalisé…");
        tips.add("Réglages fins (vignette, netteté, halo) dans le studio");
        labels.add("Désactiver les effets");
        tips.add("Rendu vanilla, aucun coût");
        buttonGrid(mainY + 74, 3, labels, tips, i -> {
            VisualProfile p = VisualStore.current();
            if (i < q.length) {
                p.quality = q[i];
                p.postEnabled = true;
            } else if (i == q.length) {
                open(new VisualStudioScreen(this, "Shaders"));
                return;
            } else {
                p.postEnabled = false;
            }
            VisualStore.save(p);
            VisualStore.setCurrent(p);
        });
        addAction("Mesurer l'impact sur les FPS", () -> open(new VisualStudioScreen(this, "Performances")));
    }

    private void initCustomize() {
        List<String> sections = List.of("Vue d'ensemble", "Luminosité", "Ciel", "Atmosphère", "Herbe et végétation", "Arbres et bois", "Eau",
                "Pierre et matériaux", "Couleurs", "Textures", "Profils", "Prévisualisation");
        buttonGrid(mainY + 40, columns(), sections, null, i -> open(new VisualStudioScreen(this, sections.get(i))));
    }

    private void initProjects() {
        buttonGrid(mainY + 40, 2, List.of("Mes modèles enregistrés", "Nouveau projet depuis une image"),
                List.of("Zones capturées avec le cristal et modèles importés", "Analyse locale d'une image : rien n'est envoyé sur Internet"),
                i -> open(i == 0 ? new BuilderScreen(this, "custom") : new ImageBuildScreen(this)));
    }

    private void initSettings() {
        buttonGrid(mainY + 40, 2, List.of("Paramètres du mod", "Touches et manette"), null,
                i -> open(i == 0 ? new SettingsScreen(this) : new KeyBindsScreen(this, minecraft.options)));
    }

    @Override
    public void tick() {
        super.tick();
        // Nouvelles données du serveur (quête validée, déblocage) : on reconstruit les compteurs.
        if (progressVersion != ClientProgress.version() || stateVersion != ClientState.version()) {
            rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ rendu

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        hits.clear();
        super.renderScaled(g, mx, my, pt);
        renderHeader(g);
        g.drawString(font, "§l" + section.label.replaceFirst("^\\d+\\. ", ""), mainX + 6, mainY + 6, 0xFFE0F0FF, false);
        switch (section) {
            case HOME -> renderHome(g, mx, my);
            case HOUSES -> renderHouses(g, mx, my);
            case SHADERS -> renderShaders(g);
            case CUSTOMIZE -> lines(g, mainY + 20, "Couleurs du monde, ciel, eau, végétation, textures et profils : tout s'applique en direct.",
                    "Chaque profil est enregistré localement et peut être exporté ou partagé.");
            case QUESTS -> renderQuests(g, mx, my);
            case MATERIALS -> renderMaterials(g);
            case UNLOCKED -> renderEntries(g, mx, my, unlockedEntries(), "unlocked",
                    ClientProgress.enforced() ? "Constructions accessibles avec votre niveau et vos déblocages." : "Mode créatif : toutes les constructions sont accessibles.");
            case FAVORITES -> renderEntries(g, mx, my, favoriteEntries(), "favorites", "Ajoutez un favori avec l'étoile dans la fiche d'une construction.");
            case PROJECTS -> renderProjects(g);
            case HISTORY -> renderHistory(g);
            case SETTINGS -> renderSettings(g);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.fill(0, 0, width, HEADER_H, 0xD0141026);
        g.fill(0, HEADER_H - 1, width, HEADER_H, 0xFFE07BB8);
        g.fill(0, HEADER_H, SIDE_W + 2, height, 0xA0101018);
        // Panneau central sous les boutons (dessinés ensuite par Screen.render).
        g.fill(mainX, mainY, mainX + mainW, mainY + mainH, 0xB0101820);
    }

    private void renderHeader(GuiGraphics g) {
        g.drawString(font, "§d§lUltimate Crystal§r §7— Creative Architect", 8, 6, 0xFFFFFFFF, false);
        String sub;
        if (!ClientProgress.known()) {
            sub = "§7Progression en attente du serveur…";
        } else if (!ClientProgress.enforced()) {
            sub = "§aMode créatif : toutes les constructions sont accessibles §7(progression non appliquée)";
        } else {
            String title = ClientProgress.title().isBlank() ? "" : " §7· « " + ClientProgress.title() + " »";
            sub = "§e" + ClientProgress.tier().label + title;
        }
        g.drawString(font, font.plainSubstrByWidth(sub, width - 230), 8, 19, 0xFFFFFFFF, false);
        // Barre de progression de la chaîne de quêtes (réclamées / total).
        int done = 0, total = 0;
        for (ProgressPayload.QuestInfo q : ClientProgress.quests()) {
            total++;
            if (ClientProgress.state(q) == Quest.State.DONE) {
                done++;
            }
        }
        if (total > 0) {
            int bw = 180, bx = width - bw - 10;
            g.drawString(font, "Quêtes " + done + "/" + total, bx, 6, 0xFFCCCCDD, false);
            bar(g, bx, 19, bw, 8, done / (float) total, 0xFFE07BB8);
            renderItem(g, ClientProgress.tier().icon, bx - 22, 9);
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float f, int color) {
        g.fill(x, y, x + w, y + h, 0xFF222230);
        g.fill(x, y, x + Math.round(w * Math.max(0, Math.min(1, f))), y + h, color);
    }

    private void renderItem(GuiGraphics g, String id, int x, int y) {
        Item it = item(id);
        if (it != null) {
            g.renderItem(new ItemStack(it), x, y);
        }
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl) ? BuiltInRegistries.ITEM.get(rl) : null;
    }

    private int lines(GuiGraphics g, int y, String... text) {
        for (String t : text) {
            for (var line : font.split(Component.literal(t), mainW - 12)) {
                g.drawString(font, line, mainX + 6, y, 0xFFB8C4D0, false);
                y += 10;
            }
        }
        return y;
    }

    /** Rangée de cartes 3D cliquables (rendu texturé réel du modèle). */
    private void cards(GuiGraphics g, int mx, int my, List<ModelEntry> entries, int y, String openSection) {
        int n = Math.max(1, (mainW - 12 + 4) / (CARD + 4));
        for (int i = 0; i < Math.min(n, entries.size()); i++) {
            ModelEntry e = entries.get(i);
            int x = mainX + 6 + i * (CARD + 4);
            boolean hover = mx >= x && mx < x + CARD && my >= y && my < y + CARD + 12;
            g.fill(x, y, x + CARD, y + CARD + 12, hover ? 0xD0303046 : 0xC0181822);
            ModelPreviews.drawCard(g, e, x + 2, y + 2, CARD - 4, CARD - 4, 0);
            if (!ClientProgress.isUnlocked(e)) {
                g.fill(x, y, x + CARD, y + CARD, 0x90000000);
                g.drawCenteredString(font, "§6Verrouillée", x + CARD / 2, y + CARD / 2 - 4, 0xFFFFFFFF);
            }
            g.drawString(font, font.plainSubstrByWidth(e.name(), CARD - 4), x + 2, y + CARD + 2, 0xFFE0E0E0, false);
            String sec = openSection;
            hits.add(new Hit(x, y, x + CARD, y + CARD + 12, () -> open(new BuilderScreen(this, sec))));
        }
        if (entries.isEmpty()) {
            g.drawString(font, "§7Aucune construction pour l'instant.", mainX + 6, y + 4, 0xFFFFFFFF, false);
        }
    }

    private void renderHome(GuiGraphics g, int mx, int my) {
        int y = mainY + 22;
        // Échelle des niveaux : le niveau atteint est mis en évidence.
        Tier current = ClientProgress.tier();
        int tw = Math.max(80, (mainW - 12) / Tier.values().length);
        for (Tier t : Tier.values()) {
            int x = mainX + 6 + t.ordinal() * tw;
            boolean reached = current.atLeast(t);
            g.fill(x, y, x + tw - 4, y + 30, t == current ? 0xC0603050 : reached ? 0xA0303040 : 0x80141418);
            renderItem(g, t.icon, x + 3, y + 7);
            int c = reached ? 0xFFFFFFFF : 0xFF707080;
            for (int i = 0; i < 2 && i < font.split(Component.literal(t.label), tw - 26).size(); i++) {
                g.drawString(font, font.split(Component.literal(t.label), tw - 26).get(i), x + 22, y + 5 + i * 10, c, false);
            }
        }
        y += 38;
        int unlockedCount = unlockedEntries().size();
        g.drawString(font, "Constructions terminées : §f" + ClientProgress.builds() + "§7   Accessibles : §f" + unlockedCount + "§7 / "
                + DefaultCatalog.shared().size() + "§7   Favoris : §f" + ClientData.get().favorites.size(), mainX + 6, y, 0xFFAAB4C0, false);
        y += 16;
        g.drawString(font, "§bQuêtes en cours", mainX + 6, y, 0xFFFFFFFF, false);
        y += 12;
        int shown = 0;
        for (ProgressPayload.QuestInfo qi : ClientProgress.quests()) {
            Quest.State st = ClientProgress.state(qi);
            Quest q = QuestBook.get(qi.id());
            if (q == null || (st != Quest.State.IN_PROGRESS && st != Quest.State.READY) || shown >= 3) {
                continue;
            }
            g.drawString(font, (st == Quest.State.READY ? "§a✔ " : "§e• ") + q.name() + " §7— " + q.description(), mainX + 6, y, 0xFFFFFFFF, false);
            bar(g, mainX + 6, y + 10, Math.min(200, mainW - 12), 4, qi.value() / (float) qi.required(), st == Quest.State.READY ? 0xFF50D070 : 0xFFE0B040);
            y += 20;
            shown++;
        }
        if (shown == 0) {
            g.drawString(font, ClientProgress.known() ? "§7Toutes les quêtes disponibles sont terminées." : "§7En attente du serveur…", mainX + 6, y, 0xFFFFFFFF, false);
            y += 14;
        }
        y += 4;
        g.drawString(font, "§d✿ Collection Sakura", mainX + 6, y, 0xFFFFFFFF, false);
        if (y + 12 + CARD + 12 < height - 34) {
            cards(g, mx, my, DefaultCatalog.shared().byCategory(Category.SAKURA), y + 12, "sakura");
        }
    }

    private void renderHouses(GuiGraphics g, int mx, int my) {
        g.drawString(font, "§d✿ Collection Sakura §7— cliquez pour ouvrir le catalogue", mainX + 6, mainY + 18, 0xFFFFFFFF, false);
        cards(g, mx, my, DefaultCatalog.shared().byCategory(Category.SAKURA), mainY + 30, "sakura");
        g.drawString(font, "§7Catégories (accessibles / total) :", mainX + 6, mainY + 30 + CARD + 6, 0xFFFFFFFF, false);
    }

    private void renderShaders(GuiGraphics g) {
        VisualProfile p = VisualStore.current();
        int y = lines(g, mainY + 20, "§a100 % gratuit§7 : tous les profils sont inclus, sans achat, abonnement ni microtransaction.",
                "Profil actif : §f" + p.name + "§7 — qualité §f" + p.quality.label + "§7 — " + (p.postEnabled ? "§aeffets activés" : "§7effets désactivés"),
                "État : §f" + PostEffects.status());
        if (!PostEffects.error().isEmpty()) {
            y = lines(g, y, "§cErreur : " + PostEffects.error());
        }
        lines(g, mainY + 74 + 22 * 2 + 6,
                "Effets réalisés en post-traitement de l'image : étalonnage, vignette, netteté et halo lumineux. Les ombres dynamiques, "
                        + "les reflets en temps réel et les rayons volumétriques ne sont pas simulés : utilisez un pack de shaders dédié pour cela.",
                "En cas d'échec de compilation, le rendu vanilla est conservé automatiquement.");
    }

    private void renderQuests(GuiGraphics g, int mx, int my) {
        List<ProgressPayload.QuestInfo> quests = ClientProgress.quests();
        if (quests.isEmpty()) {
            lines(g, mainY + 22, ClientProgress.known() ? "Aucune quête." : "En attente de l'état du serveur…");
            return;
        }
        int top = mainY + 20, bottom = mainY + mainH - 4;
        int visible = Math.max(1, (bottom - top) / ROW_H);
        questScroll = Math.max(0, Math.min(questScroll, quests.size() - visible));
        enableScissor(g, mainX, top, mainX + mainW, bottom);
        for (int i = questScroll; i < quests.size() && i < questScroll + visible + 1; i++) {
            ProgressPayload.QuestInfo qi = quests.get(i);
            Quest q = QuestBook.get(qi.id());
            if (q == null) {
                continue;
            }
            Quest.State st = ClientProgress.state(qi);
            int y = top + (i - questScroll) * ROW_H;
            g.fill(mainX + 4, y, mainX + mainW - 4, y + ROW_H - 3, switch (st) {
                case READY -> 0xC0204A30;
                case DONE -> 0x80202028;
                case IN_PROGRESS -> 0xC0303040;
                case LOCKED -> 0x80141418;
            });
            String color = switch (st) {
                case READY -> "§a";
                case DONE -> "§8";
                case IN_PROGRESS -> "§e";
                case LOCKED -> "§7";
            };
            int textW = mainW - 110;
            g.drawString(font, font.plainSubstrByWidth(color + q.name() + " §7(" + st.label + ")", textW), mainX + 8, y + 3, 0xFFFFFFFF, false);
            String desc = st == Quest.State.LOCKED && q.prerequisite() != null && QuestBook.get(q.prerequisite()) != null
                    ? "Après : " + QuestBook.get(q.prerequisite()).name() : q.description();
            g.drawString(font, font.plainSubstrByWidth("§7" + desc, textW), mainX + 8, y + 13, 0xFFFFFFFF, false);
            g.drawString(font, font.plainSubstrByWidth("§bRécompense : " + q.reward().describe(), textW), mainX + 8, y + 23, 0xFFFFFFFF, false);
            int bx = mainX + mainW - 98;
            if (st == Quest.State.READY) {
                boolean hover = mx >= bx && mx < bx + 90 && my >= y + 8 && my < y + 26;
                g.fill(bx, y + 8, bx + 90, y + 26, hover ? 0xFF40A060 : 0xFF2E7A48);
                g.drawCenteredString(font, "Récupérer", bx + 45, y + 13, 0xFFFFFFFF);
                String id = q.id();
                hits.add(new Hit(bx, Math.max(top, y + 8), bx + 90, Math.min(bottom, y + 26),
                        () -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.CLAIM_QUEST, 0, id))));
            } else if (st != Quest.State.DONE) {
                g.drawString(font, Math.min(qi.value(), qi.required()) + " / " + qi.required(), bx, y + 6, 0xFFDDDDDD, false);
                bar(g, bx, y + 17, 90, 5, qi.value() / (float) qi.required(), st == Quest.State.LOCKED ? 0xFF505060 : 0xFFE0B040);
            } else {
                g.drawString(font, "§8✔ Terminée", bx, y + 13, 0xFFFFFFFF, false);
            }
        }
        g.disableScissor();
        if (quests.size() > visible) {
            int track = bottom - top;
            int barH = Math.max(12, track * visible / quests.size());
            int by = top + (track - barH) * questScroll / Math.max(1, quests.size() - visible);
            g.fill(mainX + mainW - 3, by, mainX + mainW - 1, by + barH, 0xC0A0D8FF);
        }
    }

    /** Inventaire du joueur lu au plus deux fois par seconde (comptage sur 41 emplacements). */
    private Map<String, Integer> inventory() {
        long now = System.currentTimeMillis();
        if (now - inventoryAt > 500 && minecraft.player != null) {
            inventoryAt = now;
            inventory.clear();
            var inv = minecraft.player.getInventory();
            for (var list : List.of(inv.items, inv.offhand)) {
                for (ItemStack s : list) {
                    if (!s.isEmpty()) {
                        inventory.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
                    }
                }
            }
        }
        return inventory;
    }

    private void renderMaterials(GuiGraphics g) {
        Map<String, Integer> inv = inventory();
        int y = mainY + 20;
        int cols = Math.max(2, (mainW - 12) / 96);
        for (int i = 0; i < MATERIALS.length; i++) {
            int x = mainX + 6 + (i % cols) * 96, yy = y + (i / cols) * 20;
            renderItem(g, MATERIALS[i], x, yy);
            int n = inv.getOrDefault(MATERIALS[i], 0);
            g.drawString(font, (n > 0 ? "§f" : "§8") + n, x + 20, yy + 5, 0xFFFFFFFF, false);
        }
        y += ((MATERIALS.length + cols - 1) / cols) * 20 + 8;
        if (!ClientProgress.enforced()) {
            lines(g, y, "Mode créatif : aucun matériau n'est demandé pour débloquer les constructions.");
            return;
        }
        g.drawString(font, "§bProchains déblocages possibles §7(coût unique, distinct des blocs de construction)", mainX + 6, y, 0xFFFFFFFF, false);
        y += 12;
        Tier tier = ClientProgress.tier();
        int shown = 0;
        com.ultimateautobuilder.core.progression.MaterialInventory mi = new com.ultimateautobuilder.core.progression.MaterialInventory() {
            @Override
            public int count(String id) {
                return inv.getOrDefault(id, 0);
            }

            @Override
            public void remove(String id, int amount) {
                throw new UnsupportedOperationException("Lecture seule côté client");
            }
        };
        // Les constructions finançables d'abord, puis celles dont il manque le moins d'objets.
        List<ModelEntry> candidates = new ArrayList<>();
        for (ModelEntry e : DefaultCatalog.shared().all()) {
            UnlockRule r = ProgressionRules.rule(e);
            if (!ClientProgress.isUnlocked(e) && tier.atLeast(r.tier()) && e.category() != Category.CUSTOM) {
                candidates.add(e);
            }
        }
        candidates.sort(java.util.Comparator.comparingInt(e -> missingCount(ProgressionRules.rule(e), mi)));
        for (ModelEntry e : candidates) {
            if (y > mainY + mainH - 14) {
                break;
            }
            List<ItemCost> miss = PlayerProgress.missing(ProgressionRules.rule(e).cost(), mi);
            String status = miss.isEmpty() ? "§a✔ matériaux suffisants" : "§cmanque " + ProgressionRules.describe(miss);
            g.drawString(font, font.plainSubstrByWidth("§f" + e.name() + " §7— " + ProgressionRules.describe(ProgressionRules.rule(e).cost()) + " — " + status,
                    mainW - 12), mainX + 6, y, 0xFFFFFFFF, false);
            y += 11;
            shown++;
        }
        if (shown == 0) {
            lines(g, y, "Aucune construction verrouillée à votre niveau : terminez des quêtes pour atteindre le niveau suivant.");
        }
    }

    private static int missingCount(UnlockRule r, com.ultimateautobuilder.core.progression.MaterialInventory mi) {
        int n = 0;
        for (ItemCost c : PlayerProgress.missing(r.cost(), mi)) {
            n += c.count();
        }
        return n;
    }

    private List<ModelEntry> unlockedEntries() {
        List<ModelEntry> out = new ArrayList<>();
        for (ModelEntry e : DefaultCatalog.shared().all()) {
            if (e.category() != Category.CUSTOM && ClientProgress.isUnlocked(e)) {
                out.add(e);
            }
        }
        return out;
    }

    private List<ModelEntry> favoriteEntries() {
        List<ModelEntry> out = new ArrayList<>();
        for (String id : ClientData.get().favorites) {
            ModelEntry e = DefaultCatalog.shared().get(id);
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    private void renderEntries(GuiGraphics g, int mx, int my, List<ModelEntry> entries, String sec, String intro) {
        int y = lines(g, mainY + 20, intro, entries.size() + " construction(s).");
        cards(g, mx, my, entries, y + 4, sec);
        if (entries.size() > 1 && y + CARD + 24 + CARD + 12 < height - 34) {
            int perRow = Math.max(1, (mainW - 12 + 4) / (CARD + 4));
            if (entries.size() > perRow) {
                cards(g, mx, my, entries.subList(perRow, entries.size()), y + CARD + 20, sec);
            }
        }
    }

    private void renderProjects(GuiGraphics g) {
        StatePayload st = ClientState.state();
        int y = lines(g, mainY + 20, "Modèles enregistrés sur ce monde et projets créés à partir d'une image.");
        y = Math.max(y, mainY + 66);
        if (st == null || st.customs().isEmpty()) {
            lines(g, y, "§7Aucun modèle enregistré : accroupi + clic droit avec le cristal pour sélectionner deux coins, puis « Enregistrer la sélection ».");
            return;
        }
        for (StatePayload.CustomInfo c : st.customs()) {
            if (y > mainY + mainH - 14) {
                break;
            }
            g.drawString(font, "§f" + c.name() + " §7— " + c.sx() + "×" + c.sy() + "×" + c.sz() + ", " + c.blocks() + " blocs", mainX + 6, y, 0xFFFFFFFF, false);
            y += 11;
        }
    }

    private void renderHistory(GuiGraphics g) {
        StatePayload st = ClientState.state();
        int y = mainY + 22;
        if (st == null || st.history().isEmpty()) {
            lines(g, y, "§7Aucune construction dans l'historique de ce monde.");
            return;
        }
        for (StatePayload.HistoryInfo h : st.history()) {
            if (y > height - 46) {
                break;
            }
            g.drawString(font, font.plainSubstrByWidth("§f" + h.name() + " §7— " + h.blocks() + " blocs — " + h.status()
                    + (h.undoAvailable() ? " §8(retour arrière possible)" : ""), mainW - 12), mainX + 6, y, 0xFFFFFFFF, false);
            y += 11;
        }
    }

    private void renderSettings(GuiGraphics g) {
        lines(g, mainY + 20, "Réglages du mod (aperçu, construction, interface) et touches configurables.");
        String mode = !ClientProgress.known() ? "inconnue" : ClientProgress.enforced() ? "appliquée (matériaux et quêtes requis)" : "non appliquée (mode créatif ou désactivée)";
        lines(g, mainY + 70,
                "Touches : " + KeyBindings.OPEN_MENU.getTranslatedKeyMessage().getString() + " = ce menu, "
                        + KeyBindings.OPEN_STUDIO.getTranslatedKeyMessage().getString() + " = studio visuel, "
                        + KeyBindings.ROTATE.getTranslatedKeyMessage().getString() + " = rotation de l'aperçu, "
                        + KeyBindings.CONFIRM.getTranslatedKeyMessage().getString() + " = confirmer la construction.",
                "Progression sur ce serveur : " + mode + ". Les administrateurs la règlent dans Paramètres du mod → progression.");
    }

    // ------------------------------------------------------------------ entrées

    @Override
    protected boolean mouseClickedScaled(double mx, double my, int button) {
        if (super.mouseClickedScaled(mx, my, button)) {
            return true;
        }
        if (button == 0) {
            for (Hit h : List.copyOf(hits)) {
                if (h.contains(mx, my)) {
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                    h.action().run();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean mouseScrolledScaled(double mx, double my, double sx, double sy) {
        if (section == Section.QUESTS && mx >= mainX) {
            questScroll = Math.max(0, questScroll - (int) Math.signum(sy));
            return true;
        }
        return super.mouseScrolledScaled(mx, my, sx, sy);
    }
}
