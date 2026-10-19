package com.ultimateautobuilder.client.visual;

import com.mojang.blaze3d.platform.GlUtil;
import com.ultimateautobuilder.client.GamepadInput;
import com.ultimateautobuilder.config.ClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Studio de création visuelle. Les réglages de rendu (post-traitement, ciel, brouillard) s'appliquent
 * immédiatement et se voient derrière le studio (fond non flouté). Les teintes de végétation demandent
 * un recalcul des chunks et les textures un rechargement des ressources : ces deux opérations sont
 * déclenchées par des boutons explicites et annoncées comme telles.
 */
public final class VisualStudioScreen extends Screen {
    private static final String[] SECTIONS = {"Vue d'ensemble", "Luminosité", "Ciel", "Atmosphère", "Herbe et végétation", "Arbres et bois",
            "Eau", "Pierre et matériaux", "Mobs", "Animaux", "Personnages et armures", "Ombres", "Reflets", "Couleurs", "Textures", "Shaders",
            "Performances", "Profils", "Prévisualisation", "Diagnostic", "Compatibilité", "Réinitialisation"};
    private static final int ROW_H = 22;

    private static int section;
    private static int scroll;

    private interface Row {
        String label();

        void build(int x, int y, int w);

        default int height() {
            return ROW_H;
        }
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private final List<AbstractWidget> navigable = new ArrayList<>();
    private int focus;
    private int contentX, contentY, contentW, contentH, sideW;
    private String flash = "";
    private long flashUntil;
    private String newProfileName = "";
    private String renameTo = "";
    private boolean dirty;

    public VisualStudioScreen(Screen parent) {
        super(Component.literal("Studio de création visuelle"));
        this.parent = parent;
    }

    /** Ouvre directement une section (index dans {@link #SECTIONS}), par exemple depuis le hub du cristal. */
    public VisualStudioScreen(Screen parent, String sectionName) {
        this(parent);
        for (int i = 0; i < SECTIONS.length; i++) {
            if (SECTIONS[i].equals(sectionName)) {
                section = i;
                scroll = 0;
            }
        }
    }

    private VisualProfile p() {
        return VisualStore.current();
    }

    private void changed() {
        VisualStore.touch();
        dirty = true;
    }

    private void flash(String msg) {
        flash = msg;
        flashUntil = System.currentTimeMillis() + 5000;
    }

    // ------------------------------------------------------------------ widgets

    /** Curseur numérique lié à un champ du profil. */
    private final class Slider extends AbstractSliderButton {
        private final String name;
        private final double min, max;
        private final Consumer<Double> setter;
        private final String fmt;

        Slider(int x, int y, int w, String name, double min, double max, double current, String fmt, Consumer<Double> setter) {
            super(x, y, w, 20, Component.empty(), (current - min) / (max - min));
            this.name = name;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.fmt = fmt;
            updateMessage();
        }

        private double real() {
            return min + (max - min) * value;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(name + " : " + String.format(Locale.ROOT, fmt, real())));
        }

        @Override
        protected void applyValue() {
            setter.accept(real());
            changed();
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

    private Row text(String line) {
        return new Row() {
            @Override
            public String label() {
                return "§7" + line;
            }

            @Override
            public void build(int x, int y, int w) {
            }

            @Override
            public int height() {
                return 12;
            }
        };
    }

    private Row slider(String label, double min, double max, DoubleSupplier get, Consumer<Double> set, String fmt) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                nav(new Slider(x, y, w, label, min, max, get.getAsDouble(), fmt, set));
            }
        };
    }

    private Row toggle(String label, java.util.function.BooleanSupplier get, Consumer<Boolean> set) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                nav(Button.builder(Component.literal(get.getAsBoolean() ? "§aActivé" : "§cDésactivé"), b -> {
                    set.accept(!get.getAsBoolean());
                    changed();
                    rebuildWidgets();
                }).bounds(x, y, w, 20).build());
            }
        };
    }

    private Row button(String label, String text, Runnable action) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                nav(Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build());
            }
        };
    }

    /** Couleur : aperçu, champ hexadécimal et trois curseurs R/V/B. */
    private Row color(String label, IntSupplier get, IntConsumer set) {
        return new Row() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void build(int x, int y, int w) {
                int c = get.getAsInt();
                EditBox hex = new EditBox(font, x + 1, y + 1, 52, 18, Component.literal("Couleur"));
                hex.setMaxLength(7);
                hex.setValue(String.format("#%06X", c & 0xFFFFFF));
                hex.setResponder(t -> {
                    String v = t.startsWith("#") ? t.substring(1) : t;
                    if (v.matches("[0-9a-fA-F]{6}")) {
                        set.accept(Integer.parseInt(v, 16));
                        changed();
                    }
                });
                nav(hex);
                int sw = (w - 60) / 3;
                String[] names = {"R", "V", "B"};
                for (int k = 0; k < 3; k++) {
                    int shift = 16 - 8 * k;
                    nav(new Slider(x + 58 + k * (sw + 1), y, sw, names[k], 0, 255, (c >> shift) & 255, "%.0f", v -> {
                        int cur = get.getAsInt();
                        int nv = (cur & ~(255 << shift)) | ((int) Math.round(v) << shift);
                        set.accept(nv);
                        hex.setValue(String.format("#%06X", nv & 0xFFFFFF));
                    }));
                }
            }
        };
    }

    private <T extends AbstractWidget> T nav(T w) {
        navigable.add(w);
        return addRenderableWidget(w);
    }

    /** Lignes de recoloration d'un groupe de textures. */
    private void tintRows(TextureGroups.Group g) {
        VisualProfile.Tint t = p().tint(g.key());
        rows.add(header(g.label()));
        rows.add(toggle("Recolorer « " + g.label() + " »", () -> t.enabled, v -> t.enabled = v));
        if (t.enabled) {
            rows.add(slider("Teinte", -180, 180, () -> t.hue, v -> t.hue = v.floatValue(), "%+.0f°"));
            rows.add(slider("Saturation", 0, 2, () -> t.saturation, v -> t.saturation = v.floatValue(), "×%.2f"));
            rows.add(slider("Luminosité", 0.3, 1.8, () -> t.brightness, v -> t.brightness = v.floatValue(), "×%.2f"));
            rows.add(color("Couleur", () -> t.color, v -> t.color = v));
            rows.add(slider("Force de la couleur", 0, 1, () -> t.strength, v -> t.strength = v.floatValue(), "%.2f"));
        }
    }

    private void textureSection(TextureGroups.Section s, String intro) {
        rows.add(text(intro));
        rows.add(text("Les textures sont régénérées puis le jeu recharge ses ressources (quelques secondes)."));
        rows.add(button("", TexturePack.running() ? "Génération en cours…" : "Appliquer les textures", this::applyTextures));
        for (TextureGroups.Group g : TextureGroups.inSection(s)) {
            tintRows(g);
        }
    }

    private void applyTextures() {
        VisualStore.save(p());
        dirty = false;
        flash("Génération des textures…");
        TexturePack.regenerate(p()).thenAccept(msg -> Minecraft.getInstance().execute(() -> flash(msg)));
    }

    // ------------------------------------------------------------------ sections

    private void collect() {
        rows.clear();
        VisualProfile p = p();
        switch (section) {
            case 0 -> {
                rows.add(header("Profil actif : " + p.name + (dirty ? " (modifié)" : "")));
                rows.add(toggle("Effets de post-traitement", () -> p.postEnabled, v -> p.postEnabled = v));
                rows.add(new Row() {
                    @Override
                    public String label() {
                        return "Qualité";
                    }

                    @Override
                    public void build(int x, int y, int w) {
                        nav(Button.builder(Component.literal(p.quality.label), b -> {
                            VisualProfile.Quality[] all = VisualProfile.Quality.values();
                            p.quality = all[(p.quality.ordinal() + 1) % all.length];
                            changed();
                            rebuildWidgets();
                        }).bounds(x, y, w, 20).tooltip(Tooltip.create(Component.literal(
                                "Performance : 1 passe ; Équilibré : + vignette et netteté ; Qualité : + halo ; Ultra : halo large en 2 itérations")))
                                .build());
                    }
                });
                rows.add(text("Shaders : " + PostEffects.status()));
                rows.add(header("Préréglages"));
                for (String key : VisualProfile.PRESETS) {
                    VisualProfile preset = VisualProfile.preset(key);
                    rows.add(button(preset.name, "Charger", () -> {
                        VisualProfile copy = preset.copy(preset.name);
                        VisualStore.save(copy);
                        VisualStore.setCurrent(copy);
                        dirty = false;
                        flash("Préréglage « " + copy.name + " » chargé");
                        rebuildWidgets();
                    }));
                }
                rows.add(button("", "Enregistrer le profil", this::saveProfile));
            }
            case 1 -> {
                rows.add(slider("Exposition", 0.4, 2.5, () -> p.exposure, v -> p.exposure = v.floatValue(), "×%.2f"));
                rows.add(slider("Gamma", 0.5, 2.2, () -> p.gamma, v -> p.gamma = v.floatValue(), "%.2f"));
                rows.add(slider("Grottes et nuit (éclaircissement des zones sombres)", 0, 1, () -> p.shadowLift, v -> p.shadowLift = v.floatValue(), "%.2f"));
                rows.add(slider("Contraste", 0.5, 1.8, () -> p.contrast, v -> p.contrast = v.floatValue(), "×%.2f"));
                rows.add(text("Réglages appliqués par post-traitement (image finale) : la lumière du monde n'est pas modifiée."));
                for (String key : new String[]{"bright", "max", "caves", "night"}) {
                    VisualProfile preset = VisualProfile.preset(key);
                    rows.add(button(preset.name, "Appliquer", () -> {
                        p.exposure = preset.exposure;
                        p.gamma = preset.gamma;
                        p.shadowLift = preset.shadowLift;
                        p.contrast = preset.contrast;
                        p.temperature = preset.temperature;
                        p.postEnabled = true;
                        changed();
                        rebuildWidgets();
                    }));
                }
            }
            case 2 -> {
                rows.add(toggle("Ciel personnalisé", () -> p.skyEnabled, v -> p.skyEnabled = v));
                rows.add(color("Couleur du zénith", () -> p.skyZenith, v -> p.skyZenith = v));
                rows.add(color("Couleur de l'horizon", () -> p.skyHorizon, v -> p.skyHorizon = v));
                rows.add(slider("Intensité", 0, 1, () -> p.skyStrength, v -> p.skyStrength = v.floatValue(), "%.2f"));
                rows.add(text("Le ciel est détecté par la profondeur (pixels sans géométrie) ; soleil et lune restent visibles."));
                rows.add(text("Les étoiles sont procédurales et ne sont pas recolorées."));
            }
            case 3 -> {
                rows.add(toggle("Couleur du brouillard et de l'horizon", () -> p.fogColorEnabled, v -> p.fogColorEnabled = v));
                rows.add(color("Couleur", () -> p.fogColor, v -> p.fogColor = v));
                rows.add(slider("Force", 0, 1, () -> p.fogColorStrength, v -> p.fogColorStrength = v.floatValue(), "%.2f"));
                rows.add(slider("Distance du brouillard", 0.25, 4, () -> p.fogDistance, v -> p.fogDistance = v.floatValue(), "×%.2f"));
            }
            case 4 -> {
                rows.add(toggle("Couleur de l'herbe", () -> p.grassEnabled, v -> p.grassEnabled = v));
                rows.add(color("Herbe", () -> p.grassColor, v -> p.grassColor = v));
                rows.add(slider("Force (herbe)", 0, 1, () -> p.grassStrength, v -> p.grassStrength = v.floatValue(), "%.2f"));
                rows.add(toggle("Couleur du feuillage", () -> p.foliageEnabled, v -> p.foliageEnabled = v));
                rows.add(color("Feuillage", () -> p.foliageColor, v -> p.foliageColor = v));
                rows.add(slider("Force (feuillage)", 0, 1, () -> p.foliageStrength, v -> p.foliageStrength = v.floatValue(), "%.2f"));
                rows.add(button("", "Appliquer aux chunks affichés", () -> {
                    VisualStore.save(p);
                    AtmosphereHooks.rebuildChunks();
                    flash("Chunks recalculés avec les nouvelles teintes");
                }));
                textureSection(TextureGroups.Section.VEGETATION, "Textures des feuilles et des fleurs :");
            }
            case 5 -> textureSection(TextureGroups.Section.WOOD, "Chaque essence se règle séparément : modifier le chêne ne change pas le bouleau.");
            case 6 -> {
                rows.add(toggle("Brouillard sous l'eau personnalisé", () -> p.waterFogEnabled, v -> p.waterFogEnabled = v));
                rows.add(color("Couleur sous l'eau", () -> p.waterFogColor, v -> p.waterFogColor = v));
                rows.add(slider("Visibilité sous l'eau", 0.25, 4, () -> p.waterVisibility, v -> p.waterVisibility = v.floatValue(), "×%.2f"));
                textureSection(TextureGroups.Section.WATER, "Texture de l'eau (la couleur finale reste aussi teintée par le biome) :");
            }
            case 7 -> textureSection(TextureGroups.Section.STONE, "Pierre, ardoise, grès, briques, métaux et verre.");
            case 8 -> textureSection(TextureGroups.Section.MOBS, "Textures des monstres vanilla (animations et modèles inchangés).");
            case 9 -> textureSection(TextureGroups.Section.ANIMALS, "Textures des animaux vanilla.");
            case 10 -> textureSection(TextureGroups.Section.ARMOR, "Armures et apparences par défaut (les skins de joueurs téléchargés ne changent pas).");
            case 11 -> {
                rows.add(slider("Intensité des ombres", 0, 1, () -> p.shadowDepth, v -> p.shadowDepth = v.floatValue(), "%.2f"));
                rows.add(text("Assombrit les tons sombres de l'image. Ce mod ne calcule pas d'ombres projetées dynamiques :"));
                rows.add(text("cela demanderait un pipeline de rendu dédié (par exemple un pack de shaders via Iris/Oculus)."));
            }
            case 12 -> {
                rows.add(text("Reflets : non disponibles dans ce mod."));
                rows.add(text("Des reflets réels (eau, surfaces brillantes) exigent un rendu supplémentaire de la scène ;"));
                rows.add(text("aucun réglage factice n'est proposé. Un pack de shaders externe peut les fournir."));
            }
            case 13 -> {
                rows.add(slider("Saturation", 0, 2, () -> p.saturation, v -> p.saturation = v.floatValue(), "×%.2f"));
                rows.add(slider("Vibrance", -1, 1, () -> p.vibrance, v -> p.vibrance = v.floatValue(), "%+.2f"));
                rows.add(slider("Contraste", 0.5, 1.8, () -> p.contrast, v -> p.contrast = v.floatValue(), "×%.2f"));
                rows.add(slider("Température (froid ↔ chaud)", -1, 1, () -> p.temperature, v -> p.temperature = v.floatValue(), "%+.2f"));
                rows.add(color("Teinte globale", () -> p.tint, v -> p.tint = v));
                rows.add(slider("Force de la teinte", 0, 1, () -> p.tintStrength, v -> p.tintStrength = v.floatValue(), "%.2f"));
            }
            case 14 -> {
                Path dir = TexturePack.userDir();
                rows.add(text("Déposez vos PNG dans : " + dir));
                rows.add(text("Exemple : minecraft/textures/block/oak_planks.png remplace les planches de chêne."));
                rows.add(text("PNG uniquement, 1024×1024 et 4 Mo au maximum ; un fichier invalide est ignoré."));
                rows.add(text("Fichiers présents : " + TexturePack.userTextureCount() + " — dernier résultat : " + TexturePack.report()));
                rows.add(button("", "Ouvrir le dossier", () -> Util.getPlatform().openFile(dir.toFile())));
                rows.add(button("", "Appliquer les textures", this::applyTextures));
            }
            case 15 -> {
                rows.add(text("État : " + PostEffects.status()));
                if (!PostEffects.error().isEmpty()) {
                    rows.add(text("§cErreur : " + PostEffects.error()));
                }
                rows.add(slider("Vignette", 0, 1, () -> p.vignette, v -> p.vignette = v.floatValue(), "%.2f"));
                rows.add(slider("Netteté", 0, 1, () -> p.sharpen, v -> p.sharpen = v.floatValue(), "%.2f"));
                rows.add(slider("Halo (bloom) — qualités Qualité et Ultra", 0, 1.5, () -> p.bloomStrength, v -> p.bloomStrength = v.floatValue(), "%.2f"));
                rows.add(slider("Seuil du halo", 0.3, 0.98, () -> p.bloomThreshold, v -> p.bloomThreshold = v.floatValue(), "%.2f"));
                rows.add(button("", "Recompiler les shaders", () -> {
                    PostEffects.invalidate();
                    flash("Shaders rechargés au prochain rendu");
                }));
            }
            case 16 -> {
                rows.add(text(String.format(Locale.ROOT, "Actuellement : %.0f FPS (moyenne 2 s), 1 %% bas %.0f, dernière image %.1f ms",
                        FrameStats.fps(2_000_000_000L), FrameStats.low1(2_000_000_000L), FrameStats.lastFrameMs())));
                FrameStats.Phase ph = FrameStats.phase();
                if (ph != FrameStats.Phase.IDLE && ph != FrameStats.Phase.DONE) {
                    rows.add(text("Mesure en cours : " + Math.round(FrameStats.benchProgress() * 100) + " % — ne bougez pas la caméra"));
                    rows.add(button("", "Interrompre", FrameStats::cancelBenchmark));
                } else {
                    rows.add(button("", "Mesurer l'impact (13 s)", () -> {
                        FrameStats.startBenchmark();
                        flash("Mesure : 5 s sans effets puis 5 s avec le profil");
                    }));
                }
                PerformanceVerdict.Result r = FrameStats.result();
                if (r != null) {
                    rows.add(text(String.format(Locale.ROOT, "Sans effets : %.0f FPS (1 %% bas %.0f) — avec effets : %.0f FPS (1 %% bas %.0f)",
                            r.fpsOff(), r.lowOff(), r.fpsOn(), r.lowOn())));
                    rows.add(text(r.verdict()));
                    if (r.advice() != null) {
                        rows.add(button("Conseil", "Passer en qualité « " + r.advice().label + " »", () -> {
                            p.quality = r.advice();
                            changed();
                            rebuildWidgets();
                        }));
                    }
                }
                rows.add(text("Seuils : 0-50 FPS perdus acceptable, 51-100 avertissement, 101-149 sérieux, 150+ critique,"));
                rows.add(text("pondérés par la perte relative (perdre 50 FPS sur 80 est plus grave que sur 300)."));
            }
            case 17 -> profilesSection();
            case 18 -> {
                rows.add(slider("Comparaison avant / après (0 = désactivée)", 0, 0.95, () -> p.split, v -> p.split = v.floatValue(), "%.2f"));
                rows.add(text("À gauche de la ligne blanche : image d'origine ; à droite : votre profil."));
                rows.add(text("Le studio laisse voir le monde : les réglages de rendu s'appliquent en direct."));
            }
            case 19 -> {
                Minecraft mc = Minecraft.getInstance();
                rows.add(text("Post-traitement : " + PostEffects.status() + ", images traitées : " + PostEffects.frames()));
                rows.add(text("Dernière erreur shader : " + (PostEffects.error().isEmpty() ? "aucune" : PostEffects.error())));
                rows.add(text("Carte graphique : " + GlUtil.getRenderer() + " (" + GlUtil.getVendor() + ")"));
                rows.add(text("Fenêtre : " + mc.getWindow().getWidth() + "×" + mc.getWindow().getHeight()));
                rows.add(text("Pack de textures : " + TexturePack.report()));
                rows.add(text("Profils : " + VisualStore.listNames().size() + " — " + (VisualStore.lastError().isEmpty() ? "aucune erreur" : VisualStore.lastError())));
            }
            case 20 -> {
                for (String[] m : new String[][]{{"iris", "Iris"}, {"oculus", "Oculus"}, {"embeddium", "Embeddium"}, {"sodium", "Sodium"},
                        {"controlify", "Controlify"}, {"distanthorizons", "Distant Horizons"}}) {
                    boolean loaded = ModList.get().isLoaded(m[0]);
                    rows.add(text(m[1] + " : " + (loaded ? "§einstallé" : "absent")));
                }
                rows.add(text("Avec un pack de shaders actif (Iris/Oculus), désactivez le post-traitement pour éviter de cumuler deux étalonnages."));
                rows.add(text("Avec Sodium/Embeddium, les teintes d'herbe et de feuillage restent appliquées par les gestionnaires de couleurs."));
            }
            case 21 -> {
                rows.add(button("Profil actif", "Revenir aux valeurs par défaut", () -> confirm("Réinitialiser le profil actif ?", () -> {
                    VisualProfile d = new VisualProfile();
                    d.name = p.name;
                    VisualStore.save(d.sanitize());
                    VisualStore.setCurrent(d);
                    dirty = false;
                })));
                rows.add(button("Textures", "Vider le pack généré", () -> confirm("Supprimer toutes les textures générées ?", TexturePack::clear)));
                rows.add(button("Rendu", "Désactiver tous les effets", () -> {
                    p.postEnabled = false;
                    p.skyEnabled = false;
                    p.fogColorEnabled = false;
                    p.waterFogEnabled = false;
                    p.fogDistance = 1f;
                    changed();
                    rebuildWidgets();
                }));
            }
            default -> {
            }
        }
    }

    private void profilesSection() {
        rows.add(header("Profils enregistrés"));
        for (String name : VisualStore.listNames()) {
            rows.add(new Row() {
                @Override
                public String label() {
                    return (name.equals(p().name) ? "§a▶ " : "") + name;
                }

                @Override
                public void build(int x, int y, int w) {
                    int bw = (w - 12) / 4;
                    nav(Button.builder(Component.literal("Charger"), b -> {
                        VisualProfile v = VisualStore.get(name);
                        if (v != null) {
                            VisualStore.setCurrent(v);
                            dirty = false;
                            flash("Profil « " + name + " » chargé");
                            rebuildWidgets();
                        }
                    }).bounds(x, y, bw, 20).build());
                    nav(Button.builder(Component.literal("Dupliquer"), b -> {
                        VisualProfile d = VisualStore.duplicate(name);
                        flash(d != null ? "Copie « " + d.name + " » créée" : VisualStore.lastError());
                        rebuildWidgets();
                    }).bounds(x + bw + 4, y, bw, 20).build());
                    nav(Button.builder(Component.literal("Exporter"), b -> {
                        try {
                            flash("Exporté vers " + VisualStore.export(name));
                        } catch (java.io.IOException e) {
                            flash("Export impossible : " + e.getMessage());
                        }
                    }).bounds(x + 2 * (bw + 4), y, bw, 20).build());
                    nav(Button.builder(Component.literal("§cSupprimer"), b -> confirm("Supprimer le profil « " + name + " » ?", () -> {
                        VisualStore.delete(name);
                    })).bounds(x + 3 * (bw + 4), y, bw, 20).build());
                }
            });
        }
        rows.add(header("Nouveau profil, renommage"));
        rows.add(new Row() {
            @Override
            public String label() {
                return "Nouveau (copie du profil actif)";
            }

            @Override
            public void build(int x, int y, int w) {
                EditBox box = new EditBox(font, x + 1, y + 1, w - 70, 18, Component.literal("Nom"));
                box.setMaxLength(40);
                box.setValue(newProfileName);
                box.setResponder(t -> newProfileName = t);
                nav(box);
                nav(Button.builder(Component.literal("Créer"), b -> {
                    if (newProfileName.isBlank()) {
                        return;
                    }
                    VisualProfile c = p().copy(newProfileName.strip());
                    if (VisualStore.save(c)) {
                        VisualStore.setCurrent(c);
                        dirty = false;
                        newProfileName = "";
                        flash("Profil « " + c.name + " » créé");
                    } else {
                        flash(VisualStore.lastError());
                    }
                    rebuildWidgets();
                }).bounds(x + w - 64, y, 64, 20).build());
            }
        });
        rows.add(new Row() {
            @Override
            public String label() {
                return "Renommer le profil actif";
            }

            @Override
            public void build(int x, int y, int w) {
                EditBox box = new EditBox(font, x + 1, y + 1, w - 70, 18, Component.literal("Nouveau nom"));
                box.setMaxLength(40);
                box.setValue(renameTo);
                box.setResponder(t -> renameTo = t);
                nav(box);
                nav(Button.builder(Component.literal("Renommer"), b -> {
                    flash(VisualStore.rename(p().name, renameTo) ? "Profil renommé" : VisualStore.lastError());
                    renameTo = "";
                    rebuildWidgets();
                }).bounds(x + w - 64, y, 64, 20).build());
            }
        });
        rows.add(header("Importer (dossier visual/export)"));
        rows.add(button("", "Ouvrir le dossier d'export", () -> {
            try {
                java.nio.file.Files.createDirectories(VisualStore.exportDir());
            } catch (java.io.IOException ignored) {
                // Le dossier sera créé au premier export.
            }
            Util.getPlatform().openFile(VisualStore.exportDir().toFile());
        }));
        for (String f : VisualStore.importable()) {
            rows.add(button(f, "Importer", () -> {
                VisualProfile v = VisualStore.importFile(f);
                flash(v != null ? "Profil « " + v.name + " » importé (valeurs vérifiées)" : VisualStore.lastError());
                rebuildWidgets();
            }));
        }
    }

    private void saveProfile() {
        flash(VisualStore.save(p()) ? "Profil « " + p().name + " » enregistré" : VisualStore.lastError());
        VisualStore.setCurrent(p());
        dirty = false;
        rebuildWidgets();
    }

    private void confirm(String question, Runnable action) {
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                action.run();
            }
            minecraft.setScreen(this);
        }, Component.literal(question), Component.literal("Cette action ne peut pas être annulée.")));
    }

    // ------------------------------------------------------------------ écran

    @Override
    protected void init() {
        navigable.clear();
        sideW = Math.max(110, Math.min(150, width / 4));
        contentX = sideW + 16;
        contentY = 26;
        contentW = width - contentX - 10;
        contentH = height - contentY - 30;
        // Sommaire (défilant si l'écran est petit).
        int maxSide = Math.max(1, (height - 60) / 15);
        int first = Math.max(0, Math.min(section - maxSide / 2, SECTIONS.length - maxSide));
        for (int i = first; i < Math.min(SECTIONS.length, first + maxSide); i++) {
            int idx = i;
            Button b = Button.builder(Component.literal((i == section ? "§b▶ " : "") + SECTIONS[i]), btn -> {
                section = idx;
                scroll = 0;
                focus = 0;
                rebuildWidgets();
            }).bounds(6, 26 + (i - first) * 15, sideW, 14).build();
            addRenderableWidget(b);
        }
        collect();
        int controlW = Math.min(240, contentW / 2);
        int cx = contentX + contentW - controlW;
        int y = contentY - scroll;
        for (Row r : rows) {
            if (y >= contentY - 2 && y + 20 <= contentY + contentH) {
                r.build(cx, y, controlW);
            }
            y += r.height();
        }
        addRenderableWidget(Button.builder(Component.literal("Enregistrer"), b -> saveProfile()).bounds(width / 2 - 154, height - 24, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose()).bounds(width / 2 + 54, height - 24, 100, 20).build());
        focus = Math.max(0, Math.min(focus, navigable.size() - 1));
        if (GamepadInput.available() && !navigable.isEmpty()) {
            setFocused(navigable.get(focus));
        }
    }

    private int contentHeight() {
        int h = 0;
        for (Row r : rows) {
            h += r.height();
        }
        return h;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= contentX) {
            int max = Math.max(0, contentHeight() - contentH);
            int ns = Math.max(0, Math.min(max, scroll - (int) Math.signum(sy) * ROW_H));
            if (ns != scroll) {
                scroll = ns;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void tick() {
        super.tick();
        // Rafraîchit les sections dont le contenu évolue (mesure en cours, état des shaders).
        if ((section == 16 || section == 19 || section == 0 || section == 15) && minecraft != null && minecraft.level != null
                && minecraft.level.getGameTime() % 20 == 0 && !(getFocused() instanceof EditBox)) {
            rebuildWidgets();
        }
        if (!GamepadInput.available()) {
            return;
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_PREV_CATEGORY.get())) {
            section = Math.floorMod(section - 1, SECTIONS.length);
            scroll = 0;
            focus = 0;
            rebuildWidgets();
            return;
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_NEXT_CATEGORY.get())) {
            section = Math.floorMod(section + 1, SECTIONS.length);
            scroll = 0;
            focus = 0;
            rebuildWidgets();
            return;
        }
        if (navigable.isEmpty()) {
            if (GamepadInput.pressed(ClientConfig.GAMEPAD_BACK.get())) {
                onClose();
            }
            return;
        }
        if (GamepadInput.direction(0)) {
            moveFocus(-1);
        }
        if (GamepadInput.direction(2)) {
            moveFocus(1);
        }
        AbstractWidget w = navigable.get(focus);
        if (w instanceof AbstractSliderButton s) {
            if (GamepadInput.direction(3)) {
                s.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, 0, 0);
            }
            if (GamepadInput.direction(1)) {
                s.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0);
            }
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_SELECT.get()) && w instanceof AbstractButton b && b.active) {
            int keep = focus;
            b.onPress();
            focus = Math.min(keep, Math.max(0, navigable.size() - 1));
        }
        if (GamepadInput.pressed(ClientConfig.GAMEPAD_BACK.get())) {
            onClose();
        }
    }

    private void moveFocus(int d) {
        focus = Math.floorMod(focus + d, navigable.size());
        AbstractWidget w = navigable.get(focus);
        // Fait défiler la page pour garder l'élément visible.
        if (w.getY() < contentY + 4 || w.getY() + 20 > contentY + contentH) {
            scroll = Math.max(0, scroll + (d > 0 ? ROW_H * 3 : -ROW_H * 3));
            rebuildWidgets();
            focus = Math.max(0, Math.min(focus, navigable.size() - 1));
        }
        setFocused(navigable.get(focus));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        // Pas de flou ni d'assombrissement global : le monde reste visible pour juger les réglages en direct.
        g.fill(0, 0, sideW + 12, height, 0xB0101418);
        g.fill(contentX - 6, contentY - 4, width - 4, contentY + contentH + 2, 0x90101418);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawString(font, "§d✦ §fStudio de création visuelle §7— " + p().name + (dirty ? " §e(modifié)" : ""), 8, 8, 0xFFFFFFFF);
        int controlW = Math.min(240, contentW / 2);
        int labelW = contentW - controlW - 8;
        int y = contentY - scroll;
        for (Row r : rows) {
            if (y >= contentY - 2 && y + 10 <= contentY + contentH) {
                g.drawString(font, font.plainSubstrByWidth(r.label(), r.height() < ROW_H ? contentW : labelW), contentX, y + (r.height() < ROW_H ? 1 : 6),
                        0xFFE0E0E0, false);
            }
            y += r.height();
        }
        if (!flash.isEmpty() && System.currentTimeMillis() < flashUntil) {
            g.drawCenteredString(font, flash, width / 2, height - 36, 0xFF7FE0FF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (dirty) {
            VisualStore.save(p());
            VisualStore.setCurrent(p());
        }
        minecraft.setScreen(parent);
    }
}
