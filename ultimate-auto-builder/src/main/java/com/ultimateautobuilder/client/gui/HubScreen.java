package com.ultimateautobuilder.client.gui;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.adventure.Sanctuary;
import com.ultimateautobuilder.adventure.Treasures;
import com.ultimateautobuilder.client.ClientAdventure;
import com.ultimateautobuilder.client.ClientState;
import com.ultimateautobuilder.client.KeyBindings;
import com.ultimateautobuilder.client.image.ImageBuildScreen;
import com.ultimateautobuilder.client.visual.PostEffects;
import com.ultimateautobuilder.client.visual.VisualProfile;
import com.ultimateautobuilder.client.visual.VisualStore;
import com.ultimateautobuilder.client.visual.VisualStudioScreen;
import com.ultimateautobuilder.core.adventure.ArmorSetReward;
import com.ultimateautobuilder.core.adventure.KeyTier;
import com.ultimateautobuilder.core.adventure.StructureType;
import com.ultimateautobuilder.item.KeyItem;
import com.ultimateautobuilder.network.ActionPayload;
import com.ultimateautobuilder.network.AdventureStatePayload;
import com.ultimateautobuilder.network.StatePayload;
import com.ultimateautobuilder.registry.ModItems;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Hub de l'Ultimate Crystal. Il ne vend ni ne débloque aucune maison : la progression vient de
 * l'exploration. Il montre les récompenses (ensembles d'armure, objets en attente), les
 * découvertes, les trésors et clés, et donne accès au constructeur, aux shaders gratuits, au
 * studio, aux projets, à l'historique et aux paramètres. Les récupérations sont validées par le
 * serveur ; l'écran n'affiche que l'état qu'il reçoit.
 */
public final class HubScreen extends ScaledScreen {
    private enum Section {
        HOME("Accueil"), REWARDS("1. Récompenses"), DISCOVERIES("2. Découvertes"), TREASURES("3. Trésors et clés"),
        BUILDER("4. Constructeur"), SHADERS("5. Shaders gratuits"), CUSTOMIZE("6. Personnalisation"), PROJECTS("7. Projets"),
        HISTORY("8. Historique"), SETTINGS("9. Paramètres");

        final String label;

        Section(String label) {
            this.label = label;
        }
    }

    private static final int HEADER_H = 34, SIDE_W = 132, ROW_H = 30;
    private static Section section = Section.HOME;

    private final Screen parent;
    private int mainX, mainY, mainW, mainH;
    private int scroll;
    private long advVersion = -1, stateVersion = -1;

    public HubScreen(Screen parent) {
        super(Component.literal("Ultimate Crystal — Creative Architect"));
        this.parent = parent;
    }

    /** Ouverture depuis le cristal : demande l'état à jour au serveur. */
    public static HubScreen openedFromCrystal() {
        PacketDistributor.sendToServer(new ActionPayload(ActionPayload.REQUEST_STATE, 0, ""));
        return new HubScreen(null);
    }

    private static String ns() {
        return UltimateAutoBuilderMod.MOD_ID;
    }

    // ------------------------------------------------------------------ construction

    @Override
    protected void initScaled() {
        advVersion = ClientAdventure.version();
        stateVersion = ClientState.version();
        int y = HEADER_H + 4;
        int bh = Math.max(16, Math.min(22, (height - HEADER_H - 36) / Section.values().length));
        for (Section s : Section.values()) {
            addRenderableWidget(Button.builder(Component.literal((s == section ? "§e▶ " : "") + s.label), x -> select(s))
                    .bounds(6, y, SIDE_W - 6, bh - 2).build());
            y += bh;
        }
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose()).bounds(6, height - 24, SIDE_W - 6, 20).build());
        mainX = SIDE_W + 8;
        mainY = HEADER_H + 4;
        mainW = width - mainX - 6;
        mainH = height - mainY - 6;
        switch (section) {
            case REWARDS -> initRewards();
            case BUILDER -> initBuilder();
            case SHADERS -> initShaders();
            case CUSTOMIZE -> initCustomize();
            case PROJECTS -> initProjects();
            case HISTORY -> addAction("Ouvrir l'historique complet", () -> open(new BuilderScreen(this, "history")));
            case SETTINGS -> initSettings();
            default -> {
            }
        }
    }

    private void select(Section s) {
        section = s;
        scroll = 0;
        rebuildWidgets();
    }

    /** Vérification visuelle automatique (captures en intégration continue). */
    public void captureSection(int index) {
        select(Section.values()[Math.max(0, Math.min(Section.values().length - 1, index))]);
    }

    private void open(Screen s) {
        minecraft.setScreen(s);
    }

    private void addAction(String label, Runnable r) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> r.run()).bounds(mainX + 6, height - 30, Math.min(260, mainW - 12), 20).build());
    }

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

    private void initRewards() {
        AdventureStatePayload st = ClientAdventure.state();
        int y = mainY + 34;
        for (ArmorSetReward set : ArmorSetReward.values()) {
            int state = st == null || st.armor().length <= set.ordinal() ? 0 : st.armor()[set.ordinal()];
            Button b = Button.builder(Component.literal(state == 2 ? "Récupérée" : "Récupérer"),
                    x -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.CLAIM_ARMOR, 0, set.key)))
                    .bounds(mainX + mainW - 92, y + 4, 84, 20).build();
            b.active = state == 1;
            if (state == 0) {
                b.setTooltip(Tooltip.create(Component.literal("Portez les quatre pièces de l'ensemble pour débloquer cette récompense")));
            }
            addRenderableWidget(b);
            y += ROW_H;
        }
        if (st != null && !st.pending().isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Récupérer les objets en attente"),
                    x -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.CLAIM_PENDING, 0, "")))
                    .bounds(mainX + 6, height - 30, Math.min(260, mainW - 12), 20).build());
        }
    }

    private void initBuilder() {
        buttonGrid(mainY + 70, 2, List.of("Catalogue complet", "Collection Sakura", "Maisons et villas", "★ Favoris", "Mes modèles", "Depuis une image"),
                List.of(null, null, null, null, "Zones capturées avec le cristal (accroupi + clic droit)", "Analyse locale, aucun envoi sur Internet"),
                i -> open(switch (i) {
                    case 0 -> new BuilderScreen(this, null);
                    case 1 -> new BuilderScreen(this, "sakura");
                    case 2 -> new BuilderScreen(this, "houses");
                    case 3 -> new BuilderScreen(this, "favorites");
                    case 4 -> new BuilderScreen(this, "custom");
                    default -> new ImageBuildScreen(this);
                }));
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
        buttonGrid(mainY + 40, mainW >= 460 ? 3 : 2, sections, null, i -> open(new VisualStudioScreen(this, sections.get(i))));
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
        if (advVersion != ClientAdventure.version() || stateVersion != ClientState.version()) {
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
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.fill(0, 0, width, HEADER_H, 0xD0141026);
        g.fill(0, HEADER_H - 1, width, HEADER_H, 0xFFE07BB8);
        g.fill(0, HEADER_H, SIDE_W + 2, height, 0xA0101018);
        g.fill(mainX, mainY, mainX + mainW, mainY + mainH, 0xB0101820);
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        super.renderScaled(g, mx, my, pt);
        renderHeader(g);
        g.drawString(font, "§l" + section.label.replaceFirst("^\\d+\\. ", ""), mainX + 6, mainY + 6, 0xFFE0F0FF, false);
        switch (section) {
            case HOME -> renderHome(g);
            case REWARDS -> renderRewards(g);
            case DISCOVERIES -> renderDiscoveries(g);
            case TREASURES -> renderTreasures(g);
            case BUILDER -> lines(g, mainY + 20, "Le constructeur pose une structure du catalogue à l'endroit choisi, couche par couche.",
                    "En survie, les blocs nécessaires sont prélevés dans votre inventaire (réglage serveur « freeBuild »).",
                    "Les maisons, ruines et châteaux d'aventure se découvrent dans le monde, pas dans ce menu.");
            case SHADERS -> renderShaders(g);
            case CUSTOMIZE -> lines(g, mainY + 20, "Couleurs du monde, ciel, eau, végétation, textures et profils : tout s'applique en direct.",
                    "Chaque profil est enregistré localement et peut être exporté ou partagé.");
            case PROJECTS -> renderProjects(g);
            case HISTORY -> renderHistory(g);
            case SETTINGS -> renderSettings(g);
        }
        ClientAdventure.renderFx(g, width);
    }

    private void renderHeader(GuiGraphics g) {
        g.drawString(font, "§d§lUltimate Crystal§r §7— Creative Architect", 8, 6, 0xFFFFFFFF, false);
        AdventureStatePayload st = ClientAdventure.state();
        String sub = st == null ? "§7En attente du serveur…"
                : "§7Découvertes §f" + st.discovered().size() + "§7 · Wardens vaincus §f" + st.wardens() + "§7 · Boss §f" + st.bosses()
                + (st.pending().isEmpty() ? "" : "§6 · " + st.pending().size() + " objet(s) en attente");
        g.drawString(font, font.plainSubstrByWidth(sub, width - 16), 8, 19, 0xFFFFFFFF, false);
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

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl) ? BuiltInRegistries.ITEM.get(rl) : null;
    }

    private void icon(GuiGraphics g, String id, int count, int x, int y) {
        Item it = item(id);
        if (it == null) {
            it = id.startsWith("special:") ? net.minecraft.world.item.Items.NETHERITE_PICKAXE : null;
        }
        if (it != null) {
            ItemStack st = new ItemStack(it, Math.max(1, Math.min(99, count)));
            g.renderItem(st, x, y);
            g.renderItemDecorations(font, st, x, y);
        }
    }

    private Map<KeyTier, Integer> keysInInventory() {
        Map<KeyTier, Integer> m = new EnumMap<>(KeyTier.class);
        if (minecraft.player != null) {
            for (ItemStack s : minecraft.player.getInventory().items) {
                if (s.getItem() instanceof KeyItem k) {
                    m.merge(k.tier(), s.getCount(), Integer::sum);
                }
            }
        }
        return m;
    }

    private void renderHome(GuiGraphics g) {
        int y = lines(g, mainY + 22,
                "§fExplorez le monde : maisons, ruines, châteaux et donjons contiennent coffres, clés et cartes au trésor.",
                "Les clés ouvrent les coffres verrouillés et portes de coffre-fort de leur rareté (usage unique).",
                "Les châteaux gardent trois spawners (bébé, moyen, normal). Dans les châteaux maudits et légendaires, un Warden"
                        + " peut s'éveiller (0,5 % par apparition) ; le vaincre donne la clé OP du sanctuaire.");
        y += 6;
        g.drawString(font, "§bVos clés", mainX + 6, y, 0xFFFFFFFF, false);
        y += 12;
        Map<KeyTier, Integer> keys = keysInInventory();
        int x = mainX + 6;
        for (KeyTier t : KeyTier.values()) {
            icon(g, BuiltInRegistries.ITEM.getKey(ModItems.keyFor(t)).toString(), 1, x, y);
            g.drawString(font, t.color + keys.getOrDefault(t, 0), x + 19, y + 5, 0xFFFFFFFF, false);
            x += 44;
        }
        y += 26;
        AdventureStatePayload st = ClientAdventure.state();
        if (st != null) {
            g.drawString(font, "§7Structures générées dans ce monde : §f" + st.structuresPlaced(), mainX + 6, y, 0xFFFFFFFF, false);
            y += 12;
            if (st.sanctuary().length == 3) {
                g.drawString(font, "§dSanctuaire OP : X " + st.sanctuary()[0] + "  Z " + st.sanctuary()[1] + "  " + sanctuaryDir(st), mainX + 6, y,
                        0xFFFFFFFF, false);
            } else {
                g.drawString(font, "§8Sanctuaire OP : emplacement inconnu (une clé OP le révèle)", mainX + 6, y, 0xFFFFFFFF, false);
            }
        }
    }

    private String sanctuaryDir(AdventureStatePayload st) {
        if (minecraft.player == null) {
            return "";
        }
        double dx = st.sanctuary()[0] - minecraft.player.getX(), dz = st.sanctuary()[1] - minecraft.player.getZ();
        return "§7(" + (int) Math.sqrt(dx * dx + dz * dz) + " blocs, " + Component.translatable("direction." + ns() + "." + Sanctuary.cardinal(dx, dz))
                .getString() + ")";
    }

    private void renderRewards(GuiGraphics g) {
        AdventureStatePayload st = ClientAdventure.state();
        lines(g, mainY + 18, "Portez les quatre pièces d'un même ensemble, puis récupérez sa récompense (une fois par ensemble).");
        int y = mainY + 34;
        for (ArmorSetReward set : ArmorSetReward.values()) {
            int state = st == null || st.armor().length <= set.ordinal() ? 0 : st.armor()[set.ordinal()];
            g.fill(mainX + 4, y, mainX + mainW - 4, y + ROW_H - 2, state == 1 ? 0xC0204A30 : state == 2 ? 0x80202028 : 0xC0242434);
            int x = mainX + 8;
            for (String piece : set.pieces()) {
                icon(g, piece, 1, x, y + 6);
                x += 17;
            }
            g.drawString(font, (state == 2 ? "§8" : "§f") + "Ensemble " + Component.translatable("armor_set." + ns() + "." + set.key).getString(),
                    x + 6, y + 4, 0xFFFFFFFF, false);
            g.drawString(font, state == 0 ? "§7verrouillé" : state == 1 ? "§adisponible" : "§8récupéré", x + 6, y + 15, 0xFFFFFFFF, false);
            int rx = mainX + mainW - 100 - set.rewards.size() * 20;
            for (ArmorSetReward.Reward r : set.rewards) {
                icon(g, r.item(), r.count(), rx, y + 6);
                rx += 20;
            }
            y += ROW_H;
        }
        if (st != null && !st.pending().isEmpty()) {
            g.drawString(font, "§6Objets en attente (inventaire plein) :", mainX + 6, y + 4, 0xFFFFFFFF, false);
            int x = mainX + 6;
            for (int i = 0; i < Math.min(st.pending().size(), (mainW - 12) / 18); i++) {
                ItemStack s = st.pending().get(i);
                g.renderItem(s, x, y + 16);
                g.renderItemDecorations(font, s, x, y + 16);
                x += 18;
            }
        }
    }

    private void renderDiscoveries(GuiGraphics g) {
        AdventureStatePayload st = ClientAdventure.state();
        if (st == null || st.discovered().isEmpty()) {
            lines(g, mainY + 22, "§7Aucune structure découverte pour l'instant. Entrez dans une maison, une ruine ou un château pour l'ajouter ici.");
            return;
        }
        int y = mainY + 22;
        int max = (mainH - 30) / 11;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, st.discovered().size() - max)));
        for (int i = scroll; i < st.discovered().size() && i < scroll + max; i++) {
            AdventureStatePayload.Discovery d = st.discovered().get(i);
            StructureType t = StructureType.byKey(d.type());
            String dist = "";
            if (minecraft.player != null) {
                double dx = d.x() - minecraft.player.getX(), dz = d.z() - minecraft.player.getZ();
                dist = " §7— " + (int) Math.sqrt(dx * dx + dz * dz) + " blocs " + Component.translatable("direction." + ns() + "."
                        + Sanctuary.cardinal(dx, dz)).getString();
            }
            g.drawString(font, font.plainSubstrByWidth("§f" + (t == null ? d.type() : t.label) + " §8(X " + d.x() + ", Z " + d.z() + ")" + dist,
                    mainW - 12), mainX + 6, y, 0xFFFFFFFF, false);
            y += 11;
        }
    }

    private void renderTreasures(GuiGraphics g) {
        int y = mainY + 22;
        g.drawString(font, "§bCartes au trésor de votre inventaire", mainX + 6, y, 0xFFFFFFFF, false);
        y += 12;
        int maps = 0;
        if (minecraft.player != null) {
            for (ItemStack s : minecraft.player.getInventory().items) {
                if (s.is(ModItems.TREASURE_MAP.get())) {
                    Treasures.Target t = Treasures.target(s);
                    String tier = Component.translatable("loot_tier." + ns() + "." + Treasures.tier(s).key).getString();
                    String line;
                    if (t == null) {
                        line = "§7Carte " + tier + " : position révélée à la prise en main";
                    } else {
                        double dx = t.x() - minecraft.player.getX(), dz = t.z() - minecraft.player.getZ();
                        line = "§fCarte " + tier + " : X " + t.x() + "  Z " + t.z() + " §7— " + (int) Math.sqrt(dx * dx + dz * dz) + " blocs "
                                + Component.translatable("direction." + ns() + "." + Sanctuary.cardinal(dx, dz)).getString();
                    }
                    g.renderItem(s, mainX + 6, y - 4);
                    g.drawString(font, font.plainSubstrByWidth(line, mainW - 34), mainX + 26, y, 0xFFFFFFFF, false);
                    y += 18;
                    maps++;
                    if (y > mainY + mainH / 2) {
                        break;
                    }
                }
            }
        }
        if (maps == 0) {
            g.drawString(font, "§7Aucune carte. Elles se trouvent dans les coffres des maisons et des ruines.", mainX + 6, y, 0xFFFFFFFF, false);
            y += 12;
        }
        y += 8;
        lines(g, y, "§bClés et serrures", "§fCommune§7 → coffres des maisons rares. §aPeu commune§7 → châteaux abandonnés.",
                "§9Rare§7 → forteresses souterraines et châteaux maudits. §6Légendaire§7 → maisons et châteaux légendaires.",
                "§dOP§7 → sanctuaire OP, obtenue en vainquant un Warden des châteaux ou avec l'ensemble en Netherite.",
                "§7Une mauvaise clé n'est jamais consommée ; la bonne clé l'est après l'ouverture.");
    }

    private void renderShaders(GuiGraphics g) {
        VisualProfile p = VisualStore.current();
        lines(g, mainY + 20, "§a100 % gratuit§7 : tous les profils sont inclus, sans achat, abonnement ni microtransaction.",
                "Profil actif : §f" + p.name + "§7 — qualité §f" + p.quality.label + "§7 — " + (p.postEnabled ? "§aeffets activés" : "§7effets désactivés"),
                "État : §f" + PostEffects.status());
        lines(g, mainY + 74 + 22 * 2 + 6,
                "Effets réalisés en post-traitement de l'image : étalonnage, vignette, netteté et halo lumineux. Les ombres dynamiques, "
                        + "les reflets en temps réel et les rayons volumétriques ne sont pas simulés.",
                "En cas d'échec de compilation, le rendu vanilla est conservé automatiquement.");
    }

    private void renderProjects(GuiGraphics g) {
        StatePayload st = ClientState.state();
        int y = Math.max(lines(g, mainY + 20, "Modèles enregistrés sur ce monde et projets créés à partir d'une image."), mainY + 66);
        if (st == null || st.customs().isEmpty()) {
            lines(g, y, "§7Aucun modèle enregistré : accroupi + clic droit avec le cristal pour sélectionner deux coins.");
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
        lines(g, mainY + 70, "Touches : " + KeyBindings.OPEN_MENU.getTranslatedKeyMessage().getString() + " = ce menu, "
                        + KeyBindings.OPEN_STUDIO.getTranslatedKeyMessage().getString() + " = studio visuel, "
                        + KeyBindings.ROTATE.getTranslatedKeyMessage().getString() + " = rotation de l'aperçu.",
                "Serveur : kit de départ, maison de départ, structures du monde et cycles des spawners se règlent dans la section « adventure »"
                        + " de la configuration serveur.");
    }

    @Override
    protected boolean mouseScrolledScaled(double mx, double my, double sx, double sy) {
        if (section == Section.DISCOVERIES && mx >= mainX) {
            scroll = Math.max(0, scroll - (int) Math.signum(sy));
            return true;
        }
        return super.mouseScrolledScaled(mx, my, sx, sy);
    }
}
