package com.ultimateautobuilder.core.catalog.models;

import com.ultimateautobuilder.core.catalog.Catalog;

/** Assemble le catalogue intégré complet (≈ 400 modèles répartis en 16 catégories). */
public final class DefaultCatalog {
    private static volatile Catalog shared;

    private DefaultCatalog() {
    }

    /** Nouveau catalogue indépendant (tests, outils). */
    public static Catalog create() {
        Catalog c = new Catalog();
        PokemonModels.register(c);
        AnimeModels.register(c);
        HouseModels.register(c);
        CastleModels.register(c);
        CityModels.register(c);
        AnimalModels.register(c);
        DragonModels.register(c);
        StatueModels.register(c);
        NatureModels.register(c);
        IslandModels.register(c);
        VehicleModels.register(c);
        SciFiModels.register(c);
        DungeonModels.register(c);
        PixelArtModels.register(c);
        HumorModels.register(c);
        MiscModels.register(c);
        return c;
    }

    /** Instance partagée par le client et le serveur (les modèles personnalisés y sont ajoutés). */
    public static Catalog shared() {
        Catalog c = shared;
        if (c == null) {
            synchronized (DefaultCatalog.class) {
                c = shared;
                if (c == null) {
                    c = create();
                    shared = c;
                }
            }
        }
        return c;
    }
}
