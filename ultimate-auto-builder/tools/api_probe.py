#!/usr/bin/env python3
"""Sonde d'API : affiche des extraits des sources Minecraft/NeoForge décompilées (jar de sources
produit par ModDevGradle) pour vérifier les signatures réellement disponibles. Usage CI uniquement."""
import glob, re, sys, zipfile

PROBES = [
    # (classe, motifs regex recherchés, lignes de contexte)
    ("net/minecraft/client/renderer/PostChain.java", [r"public .*\(", r"class PostChain"], 0),
    ("net/minecraft/client/renderer/PostPass.java", [r"public .*\("], 0),
    ("net/minecraft/client/renderer/EffectInstance.java", [r"public EffectInstance\(", r"shaders/program", r"ResourceLocation\.(parse|tryParse|withDefault)"], 2),
    ("com/mojang/blaze3d/shaders/Program.java", [r"shaders/program", r"getOrCreate", r"ResourceLocation"], 2),
    ("net/minecraft/client/renderer/GameRenderer.java", [r"postEffect", r"void loadEffect", r"checkEntityPostEffect", r"blurEffect"], 1),
    ("net/minecraft/server/packs/repository/Pack.java", [r"public static .*readMetaAndCreate", r"public .*\("], 0),
    ("net/minecraft/server/packs/PackLocationInfo.java", [r"record PackLocationInfo"], 0),
    ("net/minecraft/server/packs/repository/PackSelectionConfig.java", [r"record"], 0),
    ("net/minecraft/server/packs/PathPackResources.java", [r"class PathResourcesSupplier", r"public PathResourcesSupplier"], 1),
    ("net/neoforged/neoforge/event/AddPackFindersEvent.java", [r"public .*\("], 0),
    ("net/neoforged/neoforge/client/event/RegisterColorHandlersEvent.java", [r"public .*\(", r"class "], 0),
    ("net/neoforged/neoforge/client/event/ViewportEvent.java", [r"public .*\(", r"class "], 0),
    ("net/neoforged/neoforge/client/event/RenderLevelStageEvent.java", [r"Stage AFTER_", r"public .*get"], 0),
    ("com/mojang/blaze3d/platform/NativeImage.java", [r"public static NativeImage read\(", r"public void writeToFile", r"public int getPixelRGBA", r"public void setPixelRGBA", r"public byte\[\] asByteArray"], 0),
    ("net/minecraft/client/Minecraft.java", [r"public CompletableFuture<Void> reloadResourcePacks", r"public BlockColors getBlockColors", r"public LevelRenderer levelRenderer"], 0),
    ("net/minecraft/client/renderer/LevelRenderer.java", [r"public void allChanged"], 0),
    ("net/minecraft/client/gui/GuiGraphics.java", [r"public void blit\(ResourceLocation"], 0),
    ("net/minecraft/client/renderer/FogRenderer.java", [r"public static void setupFog", r"public static void setupColor"], 0),
    ("net/minecraft/client/OptionInstance.java", [r"UnitDouble", r"public void set\("], 0),
    ("net/minecraft/client/Options.java", [r"gamma"], 0),
    ("net/minecraft/client/color/block/BlockColors.java", [r"public void register", r"public int getColor"], 0),
    ("net/minecraft/client/renderer/BiomeColors.java", [r"public static int"], 0),
    ("net/minecraft/client/renderer/texture/DynamicTexture.java", [r"public .*\("], 0),
]

def main():
    jars = [j for j in glob.glob("build/**/*.jar", recursive=True) + glob.glob("/home/runner/.gradle/**/*sources*.jar", recursive=True)
            if "sources" in j and ("neoforge" in j or "minecraft" in j)]
    print("Jars de sources :", jars[:10])
    if not jars:
        return
    zips = [zipfile.ZipFile(j) for j in jars]
    for path, pats, ctx in PROBES:
        src = None
        for z in zips:
            if path in z.namelist():
                src = z.read(path).decode("utf-8", "replace")
                break
        print("\n===== " + path + (" (ABSENT)" if src is None else ""))
        if src is None:
            continue
        lines = src.split("\n")
        shown = set()
        for i, l in enumerate(lines):
            if any(re.search(p, l) for p in pats):
                for k in range(max(0, i - ctx), min(len(lines), i + ctx + 1)):
                    if k not in shown:
                        shown.add(k)
                        print("%5d %s" % (k + 1, lines[k].rstrip()[:220]))
    # Bibliothèques du jeu utiles (sélecteur de fichiers natif).
    print("\n===== Jars lwjgl tinyfd :", glob.glob("/home/runner/.gradle/**/lwjgl-tinyfd*.jar", recursive=True)[:4])

if __name__ == "__main__":
    main()
