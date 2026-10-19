#!/usr/bin/env python3
"""Sonde d'API (CI uniquement) : affiche les signatures publiques des classes Minecraft utiles,
extraites par javap du jar compilé produit par ModDevGradle, et les chaînes constantes qui
indiquent un comportement (chemins de ressources, espaces de noms)."""
import glob, re, subprocess

CLASSES = [
    ("net.minecraft.client.renderer.PostChain", None),
    ("net.minecraft.client.renderer.PostPass", None),
    ("net.minecraft.client.renderer.EffectInstance", r"shaders|ResourceLocation"),
    ("com.mojang.blaze3d.shaders.Program", r"shaders|ResourceLocation|program"),
    ("net.minecraft.client.renderer.GameRenderer", r"post/|PostChain|loadEffect|blurEffect"),
    ("net.minecraft.server.packs.repository.Pack", None),
    ("net.minecraft.server.packs.PackLocationInfo", None),
    ("net.minecraft.server.packs.repository.PackSelectionConfig", None),
    ("net.minecraft.server.packs.PathPackResources$PathResourcesSupplier", None),
    ("net.minecraft.server.packs.repository.PackSource", None),
    ("com.mojang.blaze3d.platform.NativeImage", None),
    ("net.minecraft.client.renderer.texture.DynamicTexture", None),
    ("net.minecraft.client.color.block.BlockColors", None),
    ("net.minecraft.client.renderer.BiomeColors", None),
    ("net.minecraft.client.OptionInstance", None),
    ("net.minecraft.client.gui.components.AbstractSliderButton", None),
    ("net.minecraft.client.renderer.LevelRenderer", r"allChanged"),
    ("net.minecraft.client.Minecraft", r"reloadResourcePacks|getMainRenderTarget|getFps|fpsString"),
    ("com.mojang.blaze3d.pipeline.RenderTarget", None),
    ("net.minecraft.client.renderer.texture.TextureManager", None),
    ("org.lwjgl.util.tinyfd.TinyFileDialogs", r"tinyfd_openFileDialog|tinyfd_messageBox"),
]


def jars():
    out = [j for j in glob.glob("build/moddev/artifacts/*.jar") if "sources" not in j and "extra" not in j]
    out += glob.glob("/home/runner/.gradle/caches/modules-2/files-2.1/org.lwjgl/lwjgl-tinyfd/*/*/lwjgl-tinyfd-3.3.3.jar")
    return out


def main():
    cp = ":".join(jars())
    print("Classpath sonde :", cp)
    for cls, pattern in CLASSES:
        print("\n===== " + cls)
        r = subprocess.run(["javap", "-cp", cp, "-p", "-c" if pattern else "-public", cls], capture_output=True, text=True)
        if r.returncode != 0:
            print("  (introuvable) " + r.stderr.strip()[:200])
            continue
        for line in r.stdout.split("\n"):
            t = line.strip()
            is_sig = line.startswith("  ") and not line.startswith("    ") and "(" in t and t.endswith(";")
            is_hit = pattern and re.search(pattern, t) and ("ldc" in t or "invoke" in t or "String" in t)
            if (is_sig and (pattern is None or not t.startswith("private"))) or is_hit or t.startswith("public class") or t.startswith("public final"):
                print("  " + t[:230])


if __name__ == "__main__":
    main()
