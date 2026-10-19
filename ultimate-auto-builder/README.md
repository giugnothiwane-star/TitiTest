# Ultimate Auto Builder — NeoForge 1.21.1

Mod Minecraft Java **1.21.1** pour **NeoForge 21.1.x** (Java 21) : un menu en jeu permet de choisir parmi **403 constructions générées** (Pokémon, anime, maisons, châteaux, villes, animaux, dragons, statues, nature, îles, véhicules, science-fiction, donjons, pixel art, humour, divers), d'en régler la taille, l'orientation et la position, de les prévisualiser en fantôme puis de les faire construire **par le serveur**, progressivement et sans consommer l'inventaire.

Le menu s'ouvre avec le **Cristal de Glace** : clic droit, touche **B** (configurable) ou bouton **R1** d'une manette, uniquement lorsque le cristal est tenu.

> ⚠️ **État de vérification — à lire.** Voir [Rapport de vérification](#rapport-de-vérification) : le cœur de génération est compilé et testé ; la couche Minecraft/NeoForge n'a pas pu être compilée dans l'environnement de développement (dépôts Mojang/NeoForge bloqués) et est vérifiée par la CI GitHub Actions. Le jeu n'a **jamais** été lancé avec une interface graphique.

---

## Sommaire

1. [Installation](#installation)
2. [Compilation](#compilation)
3. [Utilisation](#utilisation)
4. [Cristal de Glace et manette](#cristal-de-glace-et-manette)
5. [Catalogue](#catalogue)
6. [Commandes](#commandes)
7. [Configuration](#configuration)
8. [Modèles personnalisés](#modèles-personnalisés)
9. [Sécurité multijoueur et performances](#sécurité-multijoueur-et-performances)
10. [Architecture](#architecture)
11. [Tests](#tests)
12. [Rapport de vérification](#rapport-de-vérification)
13. [Limites connues](#limites-connues)

---

## Installation

1. Installer **NeoForge 21.1.x** pour Minecraft **1.21.1** (installateur sur neoforged.net).
2. Copier `ultimateautobuilder-1.0.0.jar` dans le dossier `mods/` :
   - **Solo** : dans le dossier `mods/` du client ;
   - **Multijoueur** : sur le **serveur** et sur **chaque client** (le menu, l'aperçu et la manette sont côté client ; la construction est côté serveur).
3. Aucune autre dépendance : le mod n'exige ni Controlify, ni Controllable, ni aucune bibliothèque externe.

Le JAR est produit par `./gradlew build` dans `build/libs/`, ou téléchargeable comme artefact `ultimate-auto-builder-jar` de la CI GitHub Actions (onglet *Actions* → workflow *Ultimate Auto Builder*).

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

403 modèles, tous générés de façon procédurale et déterministe (aucun fichier de schéma lourd embarqué) :

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

Les personnages et créatures sont sculptés à partir de volumes (ellipsoïdes, cônes, troncs…) voxelisés à la taille demandée, avec une correction d'échelle mesurée et une épaisseur minimale pour rester lisibles en petite taille. Les bâtiments utilisent des générateurs architecturaux (murs, étages, fenêtres, toits, intérieurs, tours, remparts, rues). Les personnages sous licence sont des **interprétations voxel originales**, pas des copies d'œuvres existantes.

## Commandes

Préfixe `/uab` (alias `/ultimateautobuilder`) :

| Commande | Rôle |
|---|---|
| `/uab give [joueurs]` | Donne le Cristal de Glace (niveau d'opérateur 2) |
| `/uab menu` | Ouvre le menu |
| `/uab build <modèle> [petit\|normal\|grand\|geant] [confirmation]` | Construit devant le joueur |
| `/uab cancel [id]` | Annule une construction en cours |
| `/uab undo [id]` | Défait une construction terminée |
| `/uab history`, `/uab status` | Historique, constructions en cours |
| `/uab list [catégorie]` | Liste des modèles |
| `/uab pos1`, `/uab pos2` | Coins de zone à la position du joueur |
| `/uab save <nom>` | Enregistre la zone sélectionnée comme modèle |
| `/uab import <fichier> [nom]`, `/uab delete <id>` | Modèles personnalisés |

Permissions (API de permissions NeoForge, compatible LuckPerms) : `ultimateautobuilder.build`, `ultimateautobuilder.import`, `ultimateautobuilder.admin`. Par défaut : niveau d'opérateur configurable (`permissionLevel`, 2), et toujours autorisé pour le propriétaire d'une partie solo.

## Configuration

- **Client** — `config/ultimateautobuilder-client.toml` (ou menu *Paramètres*) : cristal requis, main du cristal, clic droit, particules, manette (activation et boutons), aperçu fantôme et nombre maximal de faces, barre de progression, distance par défaut.
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
│   └── render/      Rendu isométrique des miniatures
├── builder/         Moteur serveur : plan, tâches de pose/restauration, gestionnaire, tickets de chunks
├── network/         Paquets client ↔ serveur
├── command/         /uab
├── config/          Configs client et serveur
├── storage/         Historique, journaux d'annulation, modèles personnalisés
├── item/, registry/ Cristal de Glace, onglet créatif
├── client/          Menu, paramètres, aperçu fantôme, manette, miniatures, barre de progression
└── gametest/        Scénarios exécutés dans un serveur réel
```

## Tests

- **Tests unitaires** (`src/test`, JUnit 5) — catalogue (403 modèles, identifiants uniques, objectifs par catégorie, recherche sans accents), génération (dimensions conformes à chaque taille, aucune cellule hors limites, palettes valides, déterminisme), placement (rotations, miroir, bornes), import (.uabm aller-retour, fichiers corrompus ou trop grands rejetés, .vox), rendu des miniatures.
- **GameTests** (`./gradlew runGameTestServer`) — construction d'une petite maison et de Pikachu dans un vrai monde, annulation, retour arrière (zone vide ensuite), refus hors hauteur du monde, mode prudent qui préserve un bloc existant, résolution de tous les blocs des 403 modèles dans le registre 1.21.1, enregistrement du cristal.
- **Scénarios manuels conseillés** : ouvrir le menu avec le cristal (clic droit, B, R1) et sans le cristal ; maintenir R1 (une seule ouverture) ; naviguer au pad ; aperçu fantôme puis construction ; géant avec confirmation ; défaire ; serveur dédié avec un joueur non opérateur.

## Rapport de vérification

Réalisé dans l'environnement de développement (conteneur sans accès aux dépôts Mojang/NeoForge) :

| Élément | État |
|---|---|
| Cœur `core/**` (≈ 60 % du code) | ✅ **Compilé** avec JDK 21 et **29 tests JUnit exécutés, 0 échec** |
| Modèles | ✅ Les 403 modèles générés aux tailles Petit/Normal (tests) ; rendus isométriques PNG inspectés visuellement pour un large échantillon |
| Couche Minecraft/NeoForge (serveur, réseau, client, GUI) | ⚠️ **Non compilée localement** : `maven.neoforged.net`, `libraries.minecraft.net` et `piston-*.mojang.com` sont bloqués. Compilation et GameTests confiés à la CI GitHub Actions (`.github/workflows/ultimate-auto-builder.yml`) ; consulter le résultat de la dernière exécution |
| Jeu lancé avec interface graphique | ❌ **Jamais** : l'interface, l'aperçu fantôme, le modèle 3D du cristal et la détection R1 n'ont pas été vus en jeu |
| Version NeoForge | `gradle.properties` indique 21.1.209 ; la CI utilise automatiquement la dernière 21.1.x publiée |

## Limites connues

- **Aperçu fantôme** : rendu en faces translucides colorées (couleur moyenne de chaque bloc), pas en textures réelles ; au-delà du nombre de faces configuré, il est échantillonné.
- **Retour arrière** : restaure les blocs et le contenu des blocs-entités remplacés ; les entités (mobs, objets au sol, cadres) ne sont pas restaurées. En mode prudent, un bloc modifié par un joueur après la construction est conservé.
- **Multijoueur et modèles personnalisés** : le client reçoit un aperçu réduit (48 blocs de haut max) pour les miniatures et le fantôme ; la construction utilise le modèle complet du serveur.
- **Mode ressources** : vérifie et retire les blocs de l'inventaire au lancement (rien n'est retiré en mode créatif) ; les blocs sans objet correspondant (feu, portails…) ne sont pas comptés.
- **R1** : dépend de la reconnaissance de la manette par GLFW (voir plus haut).
