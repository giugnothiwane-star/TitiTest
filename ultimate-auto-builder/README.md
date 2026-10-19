# Ultimate Auto Builder — NeoForge 1.21.1

Mod Minecraft Java **1.21.1** pour **NeoForge 21.1.x** (Java 21), en **un seul fichier JAR** qui contient deux outils :

- **Cristal de Glace — Ultimate Auto Builder** : **510 constructions générées** (Pokémon, anime, maisons, châteaux, villes, animaux, dragons, statues, nature, îles, véhicules, science-fiction, donjons, pixel art, humour, divers, **88 mobs Minecraft**, **19 constructions inspirées de vos images**) et **Image → Construction** (recréer en blocs le sujet d'une photo ou d'une image venant du PC, d'un téléphone ou d'une tablette). Taille, orientation, position, aperçu fantôme, puis construction **par le serveur**, progressive, avec pause/reprise, mode pas à pas et retour arrière.
- **Cristal de Création Visuelle — studio graphique** : luminosité, ciel, brouillard, eau, herbe et feuillage, teintes par essence de bois / pierre / mobs / armures, post-traitement GLSL réel (exposition, contraste, saturation, halo…), profils, mesure des FPS.

Le menu de construction s'ouvre avec le **Cristal de Glace** (clic droit, touche **B** ou **R1** à la manette) ; le studio avec le **Cristal de Création Visuelle** (clic droit, touche **V** ou **R1** quand ce cristal est tenu).

> ⚠️ **État de vérification — à lire.** Voir [Rapport de vérification](#rapport-de-vérification) : tout le code est compilé par la CI GitHub Actions contre le vrai NeoForge 1.21.1, les tests unitaires et les **GameTests** (scénarios exécutés dans un vrai serveur Minecraft) y passent. Le jeu n'a en revanche **jamais été lancé avec une interface graphique** : les écrans, les shaders à l'écran et la manette n'ont pas été vus en jeu.

---

## Sommaire

1. [Installation](#installation)
2. [Compilation](#compilation)
3. [Utilisation](#utilisation)
4. [Cristal de Glace et manette](#cristal-de-glace-et-manette)
5. [Catalogue](#catalogue)
6. [Image → Construction](#image--construction)
7. [Cristal de Création Visuelle](#cristal-de-création-visuelle)
8. [Commandes](#commandes)
9. [Configuration](#configuration)
10. [Modèles personnalisés](#modèles-personnalisés)
11. [Sécurité multijoueur et performances](#sécurité-multijoueur-et-performances)
12. [Architecture](#architecture)
13. [Tests](#tests)
14. [Rapport de vérification](#rapport-de-vérification)
15. [Limites connues](#limites-connues)

---

## Installation

**Où trouver le fichier** : sur GitHub, onglet **Actions** → workflow **Ultimate Auto Builder** → dernière exécution réussie (✅) de la branche → section **Artifacts** → **`ultimate-auto-builder-jar`**. GitHub le fournit dans un `.zip` : l'ouvrir et prendre `ultimateautobuilder-1.0.0.jar` (un seul fichier, les deux cristaux sont dedans). On peut aussi le produire soi-même avec `./gradlew build` (dans `build/libs/`).

**Avec l'application CurseForge** :

1. *Create Custom Profile* → Minecraft **1.21.1** → chargeur **NeoForge** (21.1.x).
2. Sur le profil : **⋯** → *Open Folder* → dossier `mods/` → y copier `ultimateautobuilder-1.0.0.jar`.
3. Lancer le profil. Les deux cristaux sont dans l'onglet créatif « Ultimate Auto Builder ».

**Sans CurseForge** : installer NeoForge 21.1.x pour Minecraft 1.21.1 (neoforged.net) puis copier le JAR dans `mods/`.

- **Solo** : uniquement le client.
- **Multijoueur** : sur le **serveur** et sur **chaque client** (menus, aperçu, studio et manette côté client ; construction côté serveur).
- Aucune autre dépendance (ni Controlify, ni Controllable, ni bibliothèque externe).

## Compilation

Prérequis : JDK 21 et un accès réseau à `maven.neoforged.net`, `libraries.minecraft.net`, `piston-meta.mojang.com`, `piston-data.mojang.com` et `resources.download.minecraft.net`.

```bash
cd ultimate-auto-builder
./gradlew build                          # compilation + tests unitaires + JAR
./gradlew build -Pneo_version=21.1.xxx   # forcer une version précise de NeoForge 21.1
./gradlew runGameTestServer              # scénarios en jeu sur un vrai serveur (s'arrête seul)
./gradlew runClient                      # lancer le jeu de développement
```

Le cœur (génération, catalogue, rendu isométrique, import, validation) ne dépend pas de Minecraft et se teste seul, sans réseau Mojang/NeoForge :

```bash
cd ultimate-auto-builder/tools/core-tests
gradle test
gradle test -PpreviewDir=/tmp/apercus -PpreviewFilter=pikachu   # images PNG isométriques des modèles
```

## Utilisation

1. Prendre le Cristal de Glace (onglet créatif « Ultimate Auto Builder », recette, ou `/uab give`).
2. Le tenir en main et ouvrir le menu (clic droit, **B** ou **R1**).
3. Choisir une catégorie (onglets, ou **L1/R1** à la manette), chercher par nom ou mot-clé (insensible aux accents et à la casse : « dracaufeu », « chateau », « pokemon feu »…), trier (nom, taille, complexité, catégorie) et filtrer par taille.
4. Sélectionner une carte : le panneau de droite affiche une grande miniature (cliquer pour la faire pivoter), les dimensions réelles et le nombre de blocs.
5. Régler :
   - **Taille** : Petit, Normal, Grand, Géant ou Personnalisée (nombre de blocs, borné par modèle) ;
   - **Rotation** (0/90/180/270°) et **miroir** ;
   - **Mode** : *Normal*, *Prudent* (n'écrase que l'air et les blocs remplaçables), *Terrain* (fondations jusqu'au sol), *Aérien* (structure surélevée). La gratuité (*mode gratuit* ou *mode ressources*) est un réglage serveur ;
   - **Creux** : intérieur vide pour les grandes statues et créatures ;
   - **Position** : devant moi (distance réglable), ma position, coordonnées, zone sélectionnée, bloc visé.
6. **Aperçu** : la structure apparaît en fantôme translucide dans le monde, sans poser aucun bloc. Flèches / Pg.Préc / Pg.Suiv pour la déplacer, **R** tourner, **M** miroir, **=** / **-** taille, **Origine** recentrer, **Entrée** construire, **Retour arrière** annuler (touches configurables dans *Options → Commandes*).
7. **CONSTRUIRE** : le serveur revalide tout puis pose les blocs progressivement ; une barre de progression s'affiche en haut à droite. Les constructions géantes (au-delà du seuil configuré) demandent une confirmation.
8. **Historique** : annuler une construction en cours ou **défaire** une construction terminée (restauration des blocs remplacés).

Favoris, constructions récentes et préférences (taille, rotation, mode, position, tri) sont conservés dans `config/ultimateautobuilder/client-data.json`.

## Cristal de Glace et manette

- Objet enregistré `ultimateautobuilder:ice_crystal` : non empilable, non consommé, épique, résistant au feu, effet brillant, particules discrètes (désactivables), modèle **3D en main** et icône plate dans l'inventaire.
- Ouverture du menu :
  - **clic droit** avec le cristal (désactivable) ;
  - **touche configurable** (B par défaut) : fonctionne seulement si le cristal est tenu (sinon message d'aide) ;
  - **R1 à la manette** : lu directement par l'API *gamepad* de **GLFW**, la bibliothèque de fenêtre/entrées déjà incluse dans Minecraft. Les manettes PlayStation (DualShock 4, DualSense) et Xbox reconnues par la base de correspondances SDL de GLFW exposent R1 sous `GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER`. Aucun mod externe n'est nécessaire.
- Par défaut : main principale uniquement (option *Main du cristal* → les deux mains).
- Pas de réouverture en boucle tant que la touche/le bouton reste enfoncé (détection de front montant + réarmement au relâchement).
- Rien ne se déclenche quand un autre écran est ouvert (inventaire, chat, pause…).
- **Navigation à la manette dans le menu** : croix/stick gauche pour se déplacer dans la grille puis dans les réglages, **A/Croix** valider, **B/Rond** retour, **L1/R1** catégorie précédente/suivante, **X/Carré** rotation, **Y/Triangle** taille, **Start/Options** construire. Tous les boutons sont configurables.
- Si un mod de manette (Controlify, Controllable…) gère déjà les boutons, désactiver *Manette (GLFW)* dans les paramètres et associer R1 à l'action « Ouvrir le menu » dans ce mod.
- Accroupi + clic droit sur un bloc avec le cristal : définit alternativement les coins 1 et 2 de la zone sélectionnée.

**Limite honnête** : sur certaines configurations (manette connectée via Steam Input qui la convertit, pilote non standard, manette absente de la base SDL de GLFW), GLFW ne voit pas la manette comme *gamepad* ; R1 n'est alors pas détecté et seuls le clic droit et la touche clavier fonctionnent. Le nom de la manette détectée s'affiche en bas du menu.

## Catalogue

510 modèles, tous générés de façon procédurale et déterministe (aucun fichier de schéma lourd embarqué) :

| Catégorie | Modèles | Catégorie | Modèles |
|---|---:|---|---:|
| Pokémon | 40 | Arbres et nature | 20 |
| Anime et personnages | 35 | Îles flottantes et paysages | 20 |
| Maisons et villas | 48 | Véhicules et machines | 20 |
| Châteaux et médiéval | 30 | Science-fiction et espace | 20 |
| Villes et bâtiments urbains | 35 | Donjons et temples | 20 |
| Animaux et créatures | 30 | Pixel art et sculptures | 15 |
| Dragons et monstres | 20 | Structures humoristiques | 10 |
| Statues géantes | 25 | Structures diverses | 15 |
| **Mobs Minecraft** | 88 | **Inspirations (vos images)** | 19 |

Les personnages et créatures sont sculptés à partir de volumes (ellipsoïdes, cônes, troncs…) voxelisés à la taille demandée, avec une correction d'échelle mesurée et une épaisseur minimale pour rester lisibles en petite taille. Les bâtiments utilisent des générateurs architecturaux (murs, étages, fenêtres, toits, intérieurs, tours, remparts, rues). Les personnages sous licence sont des **interprétations voxel originales**, pas des copies d'œuvres existantes.

### Mobs Minecraft et constructions inspirées

- **Mobs Minecraft** (88) : les mobs de Minecraft 1.21.1 en sculptures de blocs (creeper, zombie, squelette, enderman tenant son bloc, araignée, sorcière, slime, ghast, blaze, warden, breeze, villageois, illageois, ravageur, piglin, wither, dragon de l'Ender, golems, animaux, poissons, axolotl…). Leur taille se règle par **dimension principale** (8 à 128 blocs) : un mob allongé ou plat n'est plus démesuré.
- **Inspirations (vos images)** (19) : maisons roses sur lac et ponton, cottage rose, serre en dôme, maison sous un cerisier géant, cerisier géant avec cabane, cours asiatiques rose et blanche, palais à pagodes, maison asiatique avec jardin, ferme sur soubassement de pierre, chalet au toit sombre, maison hiver/automne, grand manoir en bois, villa blanche moderne, immeuble d'appartements éclairé, chaumière au moulin, château à drapeaux, manoir fortifié.

## Image → Construction

Menu du Cristal de Glace → bouton **Image → Construction**. Tout est calculé **localement**, dans le jeu : **aucune intelligence artificielle en ligne, aucune image envoyée à un service extérieur**. Les « estimations » sont des règles mesurables (forme, couleurs, symétrie…), affichées comme telles et toujours corrigeables.

1. **Image** — choisir la vue à remplir (*Avant* obligatoire ; *Côté*, *Arrière*, *Dessus* facultatives), puis :
   - **ordinateur** : sélecteur de fichiers natif (ou fichiers déposés dans `config/ultimateautobuilder/image_to_build/images/`) ;
   - **téléphone / tablette** : bouton *Recevoir depuis un téléphone* → le jeu affiche une adresse `http://192.168.x.x:port/?t=…` à ouvrir dans le navigateur du téléphone **connecté au même Wi-Fi** ; on y choisit une photo et elle arrive dans le jeu. La réception ne démarre qu'à la demande, s'arrête seule après 10 minutes ou à la fermeture de l'écran, n'accepte que le jeton aléatoire affiché, 25 Mo maximum, et vérifie que le fichier est une vraie image. Le pare-feu de l'ordinateur peut demander une autorisation.
   - Formats : PNG, JPEG, BMP, GIF (25 Mo, 16 384 px de côté maximum ; les dimensions sont lues **avant** décodage). WebP/HEIC/AVIF : à convertir (Safari sur iPhone convertit automatiquement les photos HEIC en JPEG lors de l'envoi). L'image d'origine n'est jamais modifiée.
2. **Analyse** — séparation du sujet et du fond (transparence de l'image, sinon couleur des bords en CIELAB ; le fond retiré est affiché assombri), mesures (luminosité, contraste, netteté, symétrie, remplissage, saturation, nombre de couleurs, détection du pixel art), avertissements (image floue, sombre, petite…) et **estimation du type de sujet** avec son motif (bâtiment, personnage, animal, objet, pixel art, véhicule, arbre, paysage). Le joueur peut choisir un autre sujet.
3. **Options** :
   - **Reconstruction** : *Automatique* (selon le sujet), *Volume arrondi* (épaisseur déduite de la distance au contour), *Bas-relief* (la luminosité donne la saillie), *Plat extrudé*, *Bâtiment* (façade extrudée, toits en pignon conservés, intérieur vidé avec plancher), *Plusieurs vues* (intersection des silhouettes avant/côté/dessus) ;
   - **Arrière** : symétrique, simplifié (couleurs lissées) ou uni — ou réel si une vue arrière est fournie ;
   - **Style** : Fidèle, Minecraft vanilla (pierre, bois, terre cuite, matériaux naturels), Très détaillé, Réaliste, Voxel art (10 couleurs franches), Miniature (½), Géant (×2), Personnalisé (familles de blocs à cocher) ;
   - **Dimensions** : hauteur, largeur et profondeur en blocs (0 = proportions de l'image) ; **niveau de détail** Faible/Moyen/Élevé/Ultra (48/96/160/256 blocs max par côté) ; volume total borné à 4,2 millions de cellules ;
   - intérieur creux, relief inversé, sens de la vue de côté.
4. **Aperçu** — vue 3D sous 4 angles, vues avant/arrière/gauche/droite/dessus, **coupe couche par couche** (molette), **comparaison avant/après** (sujet de l'image à côté de la face avant du modèle). Chaque bloc est marqué **vu** (couleur prise directement sur un pixel) ou **estimé** (profondeur, côtés, arrière, intérieur) ; les estimés peuvent être surlignés en orange. Les chiffres affichés sont **mesurés** : recouvrement des silhouettes et écart de couleur moyen ΔE — jamais un pourcentage de « ressemblance » inventé. La liste des **incertitudes** indique ce que l'image ne montre pas (profondeur, arrière, membres cachés, aménagement intérieur…).
5. **Édition** (annuler / rétablir) : tourner, miroir, agrandir/réduire, creuser, retirer la couche du haut ou du bas, **remplacer un matériau** par une couleur proche (plusieurs propositions) ou le retirer.
6. **Matériaux et plan** — liste des matériaux (quantités et piles), couches, **étapes** identiques au mode « par étapes » du moteur, export d'un **plan texte** donnant chaque bloc par couche et coordonnées (légende par symbole), export du modèle en `.uabm`.
7. **Projets** — *Enregistrer le projet* conserve images (copie réduite), options et modèle édité dans `config/ultimateautobuilder/image_to_build/projets/` ; les projets récents se rouvrent depuis l'onglet *Image* (tout est revalidé à la lecture).
8. **Envoyer au serveur et construire** — le modèle est envoyé par morceaux ; le serveur vérifie la permission `ultimateautobuilder.import`, la taille (`maxImportKilobytes`) et revalide entièrement le modèle (dimensions, palette, blocs), puis l'ajoute aux **Personnalisés**. On le construit ensuite comme n'importe quel modèle : aperçu fantôme, rotation, taille, couche par couche ou par étapes, pause/reprise, zone vide uniquement, annulation et retour arrière.

## Cristal de Création Visuelle

Objet `ultimateautobuilder:visual_crystal` (recette : améthyste + diamant, ou onglet créatif). Le **studio** (clic droit, **V**, ou **R1** quand il est tenu) regroupe 22 sections : luminosité, ciel, atmosphère, herbe et végétation, arbres et bois (chaque essence), eau, pierre et matériaux, mobs, animaux, personnages et armures, ombres, reflets, couleurs, textures, shaders, performances, profils, prévisualisation, diagnostic, compatibilité, réinitialisation.

- **Post-traitement GLSL réel** (chaînes de passes de Minecraft, shaders validés par `glslang` dans la CI) : exposition, gamma, contraste, saturation, vibrance, température, teinte, éclaircissement des grottes et de la nuit, intensité des zones sombres, netteté, vignette, recoloration du ciel par la profondeur, halo lumineux (qualité Élevée/Ultra), comparaison avant/après. Une erreur de shader est captée et le rendu vanilla est rétabli.
- **Brouillard** (couleur, distance, sous l'eau), **teintes d'herbe et de feuillage** (par biome conservé, puis teinte appliquée).
- **Textures** : pack de ressources généré à partir des textures **vanilla d'origine** du jeu, recolorées par groupe (chaque essence de bois, pierres, métaux, eau, mobs, animaux, armures) ; textures personnelles PNG validées (≤ 1024 px, ≤ 4 Mo).
- **Profils** JSON versionnés : préréglages (normal, lumineux, très lumineux, grottes, nuit, naturel, saturé, cinéma, anime, fantastique, performance, ultra), création, renommage, duplication, suppression, **export/import** validés ; fichier corrompu mis de côté (`.corrompu`) sans planter.
- **Performances** : FPS moyens et « 1 % bas » mesurés sur la machine ; **banc d'essai** comparant le rendu sans effets puis avec le profil actif, mêmes conditions ; verdict selon l'écart (0-50 correct, 51-100 attention, 101-149 important, ≥ 150 critique, pondéré par le niveau de départ) et conseil de qualité. Seules des valeurs réellement mesurées sont affichées.

## Commandes

Préfixe `/uab` (alias `/ultimateautobuilder`) :

| Commande | Rôle |
|---|---|
| `/uab give [joueurs]` | Donne le Cristal de Glace (niveau d'opérateur 2) |
| `/uab menu` | Ouvre le menu |
| `/uab build <modèle> [petit\|normal\|grand\|geant] [confirmation]` | Construit devant le joueur |
| `/uab cancel [id]` | Annule une construction en cours |
| `/uab pause [id]`, `/uab resume [id]` | Met en pause / reprend une construction |
| `/uab undo [id]` | Défait une construction terminée |
| `/uab history`, `/uab status` | Historique, constructions en cours |
| `/uab list [catégorie]` | Liste des modèles |
| `/uab pos1`, `/uab pos2` | Coins de zone à la position du joueur |
| `/uab save <nom>` | Enregistre la zone sélectionnée comme modèle |
| `/uab import <fichier> [nom]`, `/uab delete <id>` | Modèles personnalisés |

Permissions (API de permissions NeoForge, compatible LuckPerms) : `ultimateautobuilder.build`, `ultimateautobuilder.import`, `ultimateautobuilder.admin`. Par défaut : niveau d'opérateur configurable (`permissionLevel`, 2), et toujours autorisé pour le propriétaire d'une partie solo.

## Configuration

- **Client** — `config/ultimateautobuilder-client.toml` (ou menu *Paramètres*) : cristal requis, main du cristal, clic droit, particules, manette (activation et boutons), aperçu fantôme et nombre maximal de faces, barre de progression, distance par défaut. Studio visuel : `config/ultimateautobuilder/visual/` (profils, pack généré). Image → Construction : `config/ultimateautobuilder/image_to_build/` (images, projets, exports).
- **Réglages de construction dans le menu** : *Étapes* (automatique, couche par couche, par étapes — la construction se met en pause à chaque palier, bouton *Reprendre*), *Rythme* (normal ou lent), *Zone vide uniquement* (refuse de démarrer si la zone contient des blocs et indique combien), et boutons *Pause* / *Reprendre* / *Annuler* dans l'historique.
- **Serveur** — `<monde>/serverconfig/ultimateautobuilder-server.toml` (modifiable depuis le menu *Paramètres* par un administrateur, le serveur vérifie droits et bornes) :

| Réglage | Défaut | Rôle |
|---|---|---|
| `speedMode` | `BALANCED` | `CAUTIOUS` 128, `BALANCED` 512, `FAST` 1024 blocs/tick, `CUSTOM` |
| `tickBudgetMs`, `adaptiveThrottle`, `targetMspt` | 15, oui, 40 | Budget de temps par tick et ralentissement automatique si le serveur peine |
| `maxBlocksPerBuild`, `maxDimension`, `giantThreshold` | 2 000 000, 512, 150 000 | Limites et seuil de confirmation |
| `maxActiveBuilds`, `maxActiveBuildsPerPlayer`, `maxBuildDistance`, `requestCooldownTicks` | 3, 1, 512, 20 | Anti-abus |
| `freeBuild` | oui | Gratuit, ou *mode ressources* (blocs prélevés dans l'inventaire) |
| `replaceExisting`, `protectValuableBlocks`, `protectedBlocks`, `fluidMode` | | Ce qui peut être remplacé |
| `respectProtectionEvents` | oui | Interroge les mods de protection avant chaque bloc |
| `hollowLargeStatues`, `aerialHeight`, `foundationMaxDepth`, `restrictToSelection` | | Options de construction |
| `historySize`, `prudentUndo`, `checkpointSeconds` | 20, oui, 30 | Historique, retour arrière, reprise après redémarrage |
| `maxImportKilobytes` | 8192 | Taille maximale d'un import et d'un modèle envoyé par Image → Construction |

## Modèles personnalisés

Formats acceptés dans `config/ultimateautobuilder/import/` :

- `.nbt` — structures Minecraft (bloc structure, avec mise à jour automatique des anciennes versions par le DataFixer) ;
- `.vox` — MagicaVoxel (couleurs converties vers les blocs les plus proches) ;
- `.uabm` — format interne compressé (palette + cellules).

Chaque import est validé (taille du fichier, dimensions, nombre de blocs, syntaxe des états, blocs inconnus remplacés et comptés). Les modèles sont stockés dans `config/ultimateautobuilder/custom/` et apparaissent dans la catégorie « Structures personnalisées ». Une zone du monde peut aussi être enregistrée (`/uab save` ou bouton *Sauver zone*).

## Sécurité multijoueur et performances

- Le client n'envoie qu'un identifiant de modèle et des paramètres ; le serveur vérifie permissions, existence du modèle, bornes de taille, distance, bordure du monde, hauteur, limites de blocs, confirmation des constructions géantes, quotas et délai entre demandes.
- Génération des modèles sur un pool de threads dédié (jamais sur le thread serveur) ; pose progressive avec budget de blocs **et** de temps par tick, régulée selon la charge mesurée.
- Chunks maintenus chargés par tickets pendant la construction ; ordre de pose « blocs pleins puis éléments attachés » ; mise à jour des formes (clôtures, vitres, escaliers) après la pose.
- Blocs précieux et indestructibles protégés, contenu des coffres sauvegardé avant remplacement, événements de protection respectés.
- Journal d'annulation par construction ; reprise automatique des constructions interrompues par un redémarrage (points de reprise).
- Écritures disque asynchrones et atomiques.

## Architecture

```
com.ultimateautobuilder
├── core/            Pur Java, aucune dépendance Minecraft (testable seul)
│   ├── geom/        Volumes (SDF), voxelisation, matériaux
│   ├── generator/   Humanoïdes, coiffures, créatures, quadrupèdes, maisons, châteaux, tours, villes, arbres
│   ├── catalog/     Catalogue, catégories, tailles, recherche/tri, 16 fichiers de modèles
│   ├── model/       Grille voxel creuse, modèle compact, rotation des états, mise à l'échelle
│   ├── placement/   Rotation/miroir/bornes
│   ├── validation/  Limites et validation
│   ├── importer/    .uabm, .vox, structure brute
│   ├── image/       Image → Construction : chargement, analyse, reconstruction, styles, vues, plan, éditeur, projets
│   └── render/      Rendu isométrique des miniatures
├── builder/         Moteur serveur : plan, tâches de pose/restauration, gestionnaire, tickets de chunks
├── network/         Paquets client ↔ serveur
├── command/         /uab
├── config/          Configs client et serveur
├── storage/         Historique, journaux d'annulation, modèles personnalisés
├── item/, registry/ Cristal de Glace, onglet créatif
├── client/          Menu, paramètres, aperçu fantôme, manette, miniatures, barre de progression
│   ├── image/       Écran Image → Construction, sélecteur de fichiers, réception téléphone, envoi au serveur
│   └── visual/      Studio visuel : profils, post-traitement GLSL, brouillard, couleurs, pack de textures, FPS
└── gametest/        Scénarios exécutés dans un serveur réel
```

## Tests

- **Tests unitaires** (`src/test`, JUnit 5) — catalogue (510 modèles, identifiants et noms uniques, objectifs par catégorie, recherche sans accents), génération (dimensions conformes à chaque taille, aucune cellule hors limites, palettes valides, déterminisme, couleur visible de chaque bloc), placement, import (.uabm, fichiers corrompus ou trop grands rejetés, .vox), rendu des miniatures, studio visuel (profils, bornes, couleurs, verdict FPS), **Image → Construction** (26 tests : chargement et rejets — vide, corrompu, tronqué, WebP, dimensions excessives —, réduction des grandes images, séparation sujet/fond, transparence, estimation du sujet, repli sans fond, hauteur et proportions, blocs vus/estimés, couleurs de face, enveloppe multi-vues, modes d'arrière, styles vanilla/voxel art/miniature/géant/personnalisé, niveau de détail, bâtiment creux, vues et couches, comparaison mesurée, plan et étapes, conversion vers le moteur, annuler/rétablir, projet sauvegardé/relu avec nom piégé, limite de volume).
- **GameTests** (`./gradlew runGameTestServer`, dans un vrai serveur) — petite maison et Pikachu construits, annulation, retour arrière, refus hors hauteur, mode prudent, palette de tous les modèles résolue dans le registre 1.21.1, cristaux enregistrés, mode couche par couche avec pauses et reprises, zone vide uniquement qui refuse les obstacles, pause manuelle qui gèle la progression, **tous les blocs d'Image → Construction existent**, **chaîne complète image → analyse → reconstruction → format réseau → construction par le moteur**.
- **Scénarios manuels conseillés** : ouvrir les menus avec chaque cristal (clic droit, B/V, R1) et sans cristal ; naviguer au pad ; aperçu fantôme puis construction ; pause/reprise ; studio : préréglages, banc d'essai, export/import de profil ; Image → Construction : photo depuis le PC puis depuis un téléphone, génération, édition, envoi et construction ; serveur dédié avec un joueur non opérateur.

## Rapport de vérification

Réalisé dans l'environnement de développement (conteneur sans accès aux dépôts Mojang/NeoForge) :

| Élément | État |
|---|---|
| Cœur `core/**` et parties pures du studio | ✅ Compilés localement avec JDK 21 : **63 tests JUnit, 0 échec** |
| Modèles | ✅ Les 510 modèles générés (tests) ; rendus isométriques PNG inspectés visuellement pour un large échantillon, dont les mobs, les constructions inspirées et des reconstructions d'images |
| Couche Minecraft/NeoForge (serveur, réseau, client, écrans, shaders) | ✅ **Compilée par la CI** GitHub Actions contre NeoForge 21.1.x (dernière version publiée), avec tests unitaires ; shaders GLSL validés par `glslang` ; signatures d'API vérifiées par une sonde `javap` |
| GameTests | ✅ Exécutés par la CI dans un vrai serveur Minecraft 1.21.1 (voir le résultat de la dernière exécution dans l'onglet *Actions*) |
| Jeu lancé avec interface graphique | ❌ **Jamais** : les écrans, l'aperçu fantôme, le rendu des shaders, le pack de textures généré, le sélecteur de fichiers, la réception depuis un téléphone et la manette n'ont pas été vus ni utilisés en jeu |
| Gains de FPS | ❌ **Aucun chiffre annoncé** : le studio mesure les FPS sur la machine du joueur ; rien n'a été mesuré ici |

## Limites connues

- **Aperçu fantôme** : rendu en faces translucides colorées (couleur moyenne de chaque bloc), pas en textures réelles ; au-delà du nombre de faces configuré, il est échantillonné.
- **Retour arrière** : restaure les blocs et le contenu des blocs-entités remplacés ; les entités (mobs, objets au sol, cadres) ne sont pas restaurées. En mode prudent, un bloc modifié par un joueur après la construction est conservé.
- **Multijoueur et modèles personnalisés** : le client reçoit un aperçu réduit (48 blocs de haut max) pour les miniatures et le fantôme ; la construction utilise le modèle complet du serveur.
- **Mode ressources** : vérifie et retire les blocs de l'inventaire au lancement (rien n'est retiré en mode créatif) ; les blocs sans objet correspondant (feu, portails…) ne sont pas comptés.
- **R1** : dépend de la reconnaissance de la manette par GLFW (voir plus haut).
- **Image → Construction** : reconstruction **heuristique** à partir de silhouettes et de couleurs, pas une compréhension de la scène. Une seule photo ne montre ni la profondeur ni l'arrière : ils sont estimés (et signalés). Les sujets fins ou complexes (membres écartés, arches, intérieurs, objets transparents) sont simplifiés ; un fond chargé peut être conservé avec le sujet (signalé). Les vues complémentaires améliorent la forme mais une enveloppe de silhouettes ne reproduit pas les creux. Couleurs : correspondance avec la couleur moyenne approximative de chaque bloc. La réception depuis un téléphone nécessite que l'ordinateur et le téléphone soient sur le même réseau local et que le pare-feu l'autorise. Le décodage des images utilise `javax.imageio` de Java.
- **Studio visuel** : le post-traitement s'applique à l'image du monde (pas à l'interface) ; il ne calcule **ni ombres projetées ni reflets** : la section *Ombres* règle seulement l'intensité des tons sombres de l'image et la section *Reflets* indique que la fonction n'est pas disponible (il faudrait un pipeline de rendu dédié). La section *Compatibilité* détecte Iris, Oculus, Embeddium, Sodium… et conseille de désactiver le post-traitement quand un pack de shaders est actif, pour ne pas cumuler deux étalonnages. La recoloration de textures régénère le pack et recharge les ressources (quelques secondes).
- **Miniatures** : le rendu isométrique logiciel peut montrer de fines rayures claires sur les toits en escalier ; elles n'existent pas dans le monde.
