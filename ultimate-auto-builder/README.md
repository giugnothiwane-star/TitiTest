# Ultimate Auto Builder — NeoForge 1.21.1

Mod Minecraft Java **1.21.1** pour **NeoForge 21.1.x** (Java 21), en **un seul fichier JAR**, centré sur **un seul objet : l'Ultimate Crystal — Creative Architect** (`ultimateautobuilder:ultimate_crystal`).

Le mod transforme la partie en **aventure d'exploration** : on commence avec une petite maison en bois et un équipement modeste, puis on explore le monde à la recherche de **maisons, ruines, châteaux et donjons générés**, de **clés** de cinq raretés, de **cartes au trésor**, de **spawners de château** et, très rarement, d'un **Warden** — voire d'un **Boss Warden** — dont la victoire donne la **clé OP** du **sanctuaire OP**. Il n'y a **plus aucun menu de maisons à acheter ou à débloquer**.

| Système | Résumé |
|---|---|
| Départ | Kit unique pour chaque nouveau joueur : pioche, hache et épée en bois, 32 bûches de chêne, 32 pierres, 1 établi, 1 Ultimate Crystal ; **maison de départ en bois** construite à côté de lui (jamais sur lui ni sur une construction) |
| Exploration | 14 types de maisons (petite maison en bois, en pierre, abandonnée, délabrée, isolée, cachée en forêt, envahie de végétation, ruines, avec cave, à pièce secrète, à coffre caché, à passage dissimulé, rare, légendaire) et 6 châteaux/donjons (petite forteresse, château abandonné, château en ruines, forteresse souterraine, château maudit, château légendaire), générés dans les **nouveaux** chunks |
| Clés | Commune, peu commune, rare, légendaire, **OP** : chaque clé n'ouvre que les coffres verrouillés et portes de coffre-fort de **sa** rareté et n'est consommée qu'après une ouverture réussie |
| Trésors | Cartes sur parchemin avec **croix rouge** ; la position est fixée une fois pour toutes, le coffre est enterré sous une croix de terre stérile |
| Spawners | Les châteaux ont **trois spawners** : **bébé**, **moyen**, **normal** ; **4 créatures au plus par cycle**, créatures ordinaires **équiprobables**, épuisement après 3 cycles |
| Warden | **0,5 %** par tentative valide (châteaux maudits et légendaires) ; variantes bébé, moyenne, normale et **Boss Warden** (barre de boss) ; clé OP seulement pour une **vraie victoire** |
| Récompenses | Ensembles d'armure complets (cuir → Netherite), objets en attente si l'inventaire est plein, animation du cristal |
| Cristal | Hub : récompenses, découvertes, trésors et clés, constructeur, **shaders gratuits**, personnalisation, projets, historique, paramètres |

> ⚠️ **État de vérification — à lire.** Voir [Rapport de vérification](#rapport-de-vérification) : tout le code est compilé par la CI GitHub Actions contre le vrai NeoForge 1.21.1, les tests unitaires et les **GameTests** (scénarios exécutés dans un vrai serveur Minecraft) y passent. Un **vrai client Minecraft** est aussi lancé par la CI sous affichage virtuel (rendu logiciel Mesa) : le mod ouvre ses écrans, pose une construction et publie des captures d'écran, relues à chaque modification. Aucun humain n'a encore joué avec la souris, la manette, un téléphone ou une carte graphique réelle.

---

## Sommaire

1. [Installation](#installation)
2. [Compilation](#compilation)
3. [Aventure](#aventure)
4. [Utilisation du constructeur](#utilisation-du-constructeur)
5. [Ultimate Crystal et manette](#ultimate-crystal-et-manette)
6. [Catalogue et collection Sakura](#catalogue-et-collection-sakura)
7. [Image → Construction](#image--construction)
8. [Shaders gratuits et studio visuel](#shaders-gratuits-et-studio-visuel)
9. [Commandes](#commandes)
10. [Configuration](#configuration)
11. [Modèles personnalisés](#modèles-personnalisés)
12. [Sécurité multijoueur et performances](#sécurité-multijoueur-et-performances)
13. [Architecture](#architecture)
14. [Tests](#tests)
15. [Rapport de vérification](#rapport-de-vérification)
16. [Limites connues](#limites-connues)

---

## Installation

**Où trouver le fichier** : sur GitHub, onglet **Actions** → workflow **Ultimate Auto Builder** → dernière exécution réussie (✅) de la branche → section **Artifacts** → **`ultimate-auto-builder-jar`**. GitHub le fournit dans un `.zip` : l'ouvrir et prendre `ultimateautobuilder-1.0.0.jar` (un seul fichier, tout le mod est dedans). On peut aussi le produire soi-même avec `./gradlew build` (dans `build/libs/`).

**Avec l'application CurseForge** :

1. *Create Custom Profile* → Minecraft **1.21.1** → chargeur **NeoForge** (21.1.x).
2. Sur le profil : **⋯** → *Open Folder* → dossier `mods/` → y copier `ultimateautobuilder-1.0.0.jar`.
3. Lancer le profil. L'Ultimate Crystal est dans l'onglet créatif « Ultimate Auto Builder » ; en survie, il est donné à la première connexion (`giveCrystalOnFirstJoin`, activé par défaut) et se fabrique (pétales roses, 3 éclats d'améthyste, 1 diamant).

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
gradle test -PpreviewDir=/tmp/apercus -PpreviewFilter=sakura   # images PNG isométriques des modèles
```

## Aventure

### Départ
- À la **première connexion d'un joueur nouveau** (aucune minute de jeu enregistrée, jamais équipé par une ancienne version du mod) : kit exact — 1 pioche, 1 hache, 1 épée en bois, 32 bûches de chêne, 32 pierres, 1 établi, 1 Ultimate Crystal. Aucun diamant, aucune Netherite, aucun objet OP.
- **Maison de départ** : maison de chêne de 9 × 7 (lit, établi, four, coffre, lanterne, fenêtres, porte), construite à 10–26 blocs du joueur sur un terrain naturel plat et sec, porte tournée vers lui. Si aucun emplacement sûr n'existe, rien n'est construit (message).
- Une seule fois : l'état est enregistré dans le monde (`<monde>/data/ultimateautobuilder_adventure.dat`). Ni la mort ni la reconnexion ne redonnent le kit. Les joueurs existants ne reçoivent rien et ne perdent rien.

### Structures générées
- Uniquement dans les **chunks nouvellement générés** de l'Overworld (jamais sur une zone déjà explorée). Grilles déterministes : une maison au plus par case de 14 × 14 chunks, un château par case de 36 × 36 (réglables), position et type tirés de la graine du monde.
- Avant la pose : terrain naturel (aucune entité de bloc, donc aucun coffre ni établi de joueur), relief limité, pas d'eau, ni océan ni rivière, aucune structure vanilla, distance minimale avec les autres structures. Un emplacement refusé n'est jamais retenté.
- Pose **progressive** sous budget de temps (4 ms par tick par défaut) : aplanissement du terrain naturel, fondations jusqu'au sol, blocs, objets accrochés, formes des escaliers et barrières, puis remplissage des coffres. Reprise automatique après un redémarrage.
- **Maisons** : chaque type change réellement l'architecture (matériaux, état, étage, cave à trappe et échelle, cloison secrète à bibliothèques ciselées, coffre encastré sous un tapis, trappe cachée sous un tapis, clôture et portillon, lierre et feuillage, murs effondrés).
- **Châteaux** : enceinte crénelée, quatre tours à échelle et meurtrières, porte, cour, donjon avec **salle des trois spawners** (bébé, moyen, normal), escalier vers l'étage et **salle du trésor** fermée par une porte de coffre-fort. La forteresse souterraine est un donjon enterré desservi par un escalier-tunnel depuis une guérite.
- **Sanctuaire OP** : unique par monde, entre 1 500 et 2 500 blocs du point d'apparition (graine du monde) ; enceinte de quartz et purpur dont l'**unique entrée est une porte de coffre-fort OP**, grand hall, trois coffres verrouillés OP et deux coffres légendaires. Clic droit avec une clé OP : distance et direction.
- Un message signale chaque **découverte** ; la liste est dans le cristal.

### Clés, serrures et butin
| Clé | Où la trouver | Ce qu'elle ouvre |
|---|---|---|
| Commune | coffres communs et peu communs | coffres verrouillés communs |
| Peu commune | coffres peu communs et rares | maisons rares, châteaux abandonnés |
| Rare | coffres peu communs, rares, légendaires | forteresses souterraines, châteaux maudits |
| Légendaire | coffres rares et légendaires, ensemble en diamant | maisons et châteaux légendaires |
| OP | victoire contre un Warden des châteaux, ensemble en Netherite | sanctuaire OP |

Coffres verrouillés et portes de coffre-fort sont **incassables**. Une mauvaise clé n'est jamais consommée ; la bonne l'est après l'ouverture ; un second clic ne trouve plus de serrure. Le butin d'un coffre est tiré **une seule fois**, à sa première ouverture (tables de butin du jeu) : fermer et rouvrir ne régénère rien.

Tables (`data/ultimateautobuilder/loot_table/`) : **commune** (terre, bois, pierre, outils en bois et pierre, nourriture, cuir, un peu de fer et d'or, pièces d'or et de mailles ; jamais de diamant ni de Netherite), **peu commune** (outils et armures en fer, or, mailles), **rare** (fer enchanté, livres, diamant rare), **légendaire** (outils et armures en diamant enchantés, ensemble complet en diamant à 3 %, aucune Netherite), **OP** (outils en Netherite **Efficacité XXV**, armures Protection VI, ensemble complet en Netherite à 4 %), coffres verrouillés (un cran au-dessus), trésors de carte, Boss Warden (pioche **Efficacité XXX**).

### Spawners de château et Warden
- Trois spawners par château concerné : **bébé**, **moyen**, **normal**. La famille du spawner (morts-vivants, araignées, illageois ou mixte) fixe les créatures autorisées ; la catégorie fixe leur variante — les deux étapes sont distinctes et ne se mélangent jamais.
- **Cycle** : une tentative toutes les 2 secondes au plus, seulement si un joueur est à moins de 16 blocs ; **4 créatures au plus par cycle**, le cycle suivant attend que les 4 aient disparu puis 30 secondes ; **épuisement après 3 cycles** (réglable). État sauvegardé avec le bloc.
- **Variantes réelles** : zombies, husks, zombies-villageois et noyés utilisent le bébé natif de Minecraft ; les autres (squelettes, araignées, illageois…) passent par l'**attribut d'échelle** de la 1.21 — rendu et boîte de collision réduits ensemble — avec santé, dégâts et vitesse ajustés (bébé : taille ½, santé ½ ; moyen : taille ¾, santé ¾).
- **Équiprobabilité** : les créatures ordinaires éligibles ont exactement la même probabilité. Jamais : dragon de l'Ender, Wither, Warden (hors système spécial), géant, gardien ancien, ghast, ravageur, creeper (détruit les structures), enderman (déplace les blocs), slimes, golems, évocateur, shulker, piglins, hoglins, phantoms, breeze, vex.
- **Warden** : dans les spawners des châteaux maudits et légendaires seulement (salles hautes de 7 blocs), **un tirage à 0,5 % par tentative valide**. Variantes : normal 70 %, bébé 14 %, moyen 14 %, **Boss Warden 2 %** ; dans un spawner de catégorie, la distribution est restreinte aux variantes de cette catégorie (spawner bébé → bébé ; moyen → moyen ; normal → normal 70/72 ou boss 2/72 ≈ 2,8 %). Le Boss Warden (taille ×1,4, santé 1 000, dégâts ×1,5) a une barre de boss.
- **Récompense** : clé OP (et butin de boss pour le Boss Warden) **au joueur qui porte le coup fatal** (directement ou par projectile), une seule fois par Warden. Un Warden supprimé par commande, nettoyé ou mort sans coup d'un joueur ne donne rien.

### Ensembles d'armure
Portez les quatre pièces d'une même matière, puis **Récompenses → Récupérer** dans le cristal (une fois par ensemble) : cuir → 2 lingots de fer + 16 pains ; or → 8 diamants + 4 pommes dorées ; mailles → 16 lingots de fer + 8 émeraudes ; fer → 8 diamants + 16 émeraudes ; diamant → 4 lingots de Netherite + 1 clé légendaire ; Netherite → Pioche de l'Architecte (Netherite, Efficacité XXX, Solidité III, Fortune III) + 1 clé OP. Inventaire plein : les objets restent **en attente** dans le cristal.

## Utilisation du constructeur

1. Prendre l'Ultimate Crystal (onglet créatif « Ultimate Auto Builder », donné à la première connexion, recette, ou `/uab give`).
2. Le tenir en main et ouvrir le hub (clic droit, **B** ou **R1**), puis **4. Constructeur**.
3. Choisir une catégorie (onglets, ou **L1/R1** à la manette), chercher par nom ou mot-clé (insensible aux accents et à la casse : « sakura », « chateau », « maison japonaise »…), trier (nom, taille, complexité, catégorie) et filtrer par taille.
4. Sélectionner une carte : le panneau de droite affiche un grand aperçu 3D texturé (glisser pour tourner, molette pour zoomer), les dimensions réelles et le nombre de blocs.
5. Régler :
   - **Taille** : Petit, Normal, Grand, Géant ou Personnalisée (nombre de blocs, borné par modèle) ;
   - **Rotation** (0/90/180/270°) et **miroir** ;
   - **Mode** : *Normal*, *Prudent* (n'écrase que l'air et les blocs remplaçables), *Terrain* (fondations jusqu'au sol), *Aérien* (structure surélevée). En survie, le constructeur prélève les blocs dans l'inventaire (réglage serveur `freeBuild`, désactivé par défaut) ; rien n'est retiré en créatif ;
   - **Creux** : intérieur vide pour les grandes statues et créatures ;
   - **Position** : devant moi (distance réglable), ma position, coordonnées, zone sélectionnée, bloc visé.
6. **Aperçu** : la structure apparaît en fantôme translucide dans le monde, sans poser aucun bloc. Flèches / Pg.Préc / Pg.Suiv pour la déplacer, **R** tourner, **M** miroir, **=** / **-** taille, **Origine** recentrer, **Entrée** construire, **Retour arrière** annuler (touches configurables dans *Options → Commandes*).
7. **CONSTRUIRE** : le serveur revalide tout puis pose les blocs progressivement ; une barre de progression s'affiche en haut à droite. Les constructions géantes (au-delà du seuil configuré) demandent une confirmation.
8. **Historique** : annuler une construction en cours ou **défaire** une construction terminée (restauration des blocs remplacés).

Favoris, constructions récentes et préférences (taille, rotation, mode, position, tri) sont conservés dans `config/ultimateautobuilder/client-data.json`.

## Ultimate Crystal et manette

- Objet enregistré `ultimateautobuilder:ultimate_crystal` : non empilable, non consommé, épique, résistant au feu, effet brillant, pétales de cerisier discrets (désactivables), modèle **3D en main** et icône plate dans l'inventaire. C'est le **seul objet de l'onglet créatif**.
- Les anciens objets `ice_crystal` et `visual_crystal` restent enregistrés pour ne pas disparaître des mondes existants (ils ouvrent toujours le hub / le studio) ; ils n'ont plus de recette propre et se **convertissent en Ultimate Crystal** sur l'établi.
- Ouverture du menu :
  - **clic droit** avec le cristal (désactivable) ;
  - **touche configurable** (B par défaut) : fonctionne seulement si le cristal est tenu (sinon message d'aide) ; **V** ouvre directement le studio visuel ;
  - **R1 à la manette** : lu directement par l'API *gamepad* de **GLFW**, la bibliothèque de fenêtre/entrées déjà incluse dans Minecraft. Les manettes PlayStation (DualShock 4, DualSense) et Xbox reconnues par la base de correspondances SDL de GLFW exposent R1 sous `GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER`. Aucun mod externe n'est nécessaire.
- Par défaut : main principale uniquement (option *Main du cristal* → les deux mains).
- Pas de réouverture en boucle tant que la touche/le bouton reste enfoncé (détection de front montant + réarmement au relâchement).
- Rien ne se déclenche quand un autre écran est ouvert (inventaire, chat, pause…).
- **Navigation à la manette dans le menu** : croix/stick gauche pour se déplacer dans la grille puis dans les réglages, **A/Croix** valider, **B/Rond** retour, **L1/R1** catégorie précédente/suivante, **X/Carré** rotation, **Y/Triangle** taille, **Start/Options** construire. Tous les boutons sont configurables.
- Si un mod de manette (Controlify, Controllable…) gère déjà les boutons, désactiver *Manette (GLFW)* dans les paramètres et associer R1 à l'action « Ouvrir le menu » dans ce mod.
- Accroupi + clic droit sur un bloc avec le cristal : définit alternativement les coins 1 et 2 de la zone sélectionnée.

**Limite honnête** : sur certaines configurations (manette connectée via Steam Input qui la convertit, pilote non standard, manette absente de la base SDL de GLFW), GLFW ne voit pas la manette comme *gamepad* ; R1 n'est alors pas détecté et seuls le clic droit et la touche clavier fonctionnent. Le nom de la manette détectée s'affiche en bas du menu.

## Catalogue et collection Sakura

454 modèles, tous générés de façon procédurale et déterministe (aucun fichier de schéma lourd embarqué). Pikachu, les Pokémon et les personnages d'anime ont été **retirés** (catalogue, fichiers, tests, traductions) ; un GameTest vérifie leur absence.

| Catégorie | Modèles | Catégorie | Modèles |
|---|---:|---|---:|
| **Maisons Sakura et japonaises** | 20 | Arbres et nature | 20 |
| Maisons et villas | 48 | Îles flottantes et paysages | 20 |
| Châteaux et médiéval | 30 | Véhicules et machines | 20 |
| Villes et bâtiments urbains | 35 | Science-fiction et espace | 20 |
| Animaux et créatures | 30 | Donjons et temples | 20 |
| Dragons et monstres | 20 | Pixel art et sculptures | 14 |
| Statues géantes | 25 | Structures humoristiques | 10 |
| Structures diverses | 15 | **Mobs Minecraft** | 88 |
| **Inspirations (vos images)** | 19 | | |

Les bâtiments utilisent des générateurs architecturaux (fondations, murs à colombages, étages, fenêtres, toits à pans et débords, intérieurs meublés, éclairage, jardins).

### Collection Sakura (20 maisons)

Générateur dédié (`core/generator/Japanese.java`) : soubassement de pierre, poteaux et poutres en bois sombre, murs blancs ou roses, fenêtres à croisillons, engawa (véranda), toits à double pente avec débords et faîtage, intérieurs (tatamis, table basse, lanternes, rangements), jardins (cerisiers en fleurs, pétales au sol, lanternes de pierre, bassins, ponts rouges, torii, chemins de pierre, bambous).

Les 20 maisons Sakura sont disponibles dans le constructeur (blocs de l'inventaire en survie).

### Mobs Minecraft et constructions inspirées

- **Mobs Minecraft** (88) : les mobs de Minecraft 1.21.1 en sculptures de blocs (creeper, zombie, squelette, enderman tenant son bloc, araignée, sorcière, slime, ghast, blaze, warden, breeze, villageois, illageois, ravageur, piglin, wither, dragon de l'Ender, golems, animaux, poissons, axolotl…). Leur taille se règle par **dimension principale** (8 à 128 blocs).
- **Inspirations (vos images)** (19) : maisons roses sur lac et ponton, cottage rose, serre en dôme, maison sous un cerisier géant, cerisier géant avec cabane, cours asiatiques rose et blanche, palais à pagodes, maison asiatique avec jardin, ferme sur soubassement de pierre, chalet au toit sombre, maison hiver/automne, grand manoir en bois, villa blanche moderne, immeuble d'appartements éclairé, chaumière au moulin, château à drapeaux, manoir fortifié.

## Image → Construction

Hub de l'Ultimate Crystal → **8. Projets** → *Nouveau projet depuis une image* (ou bouton *Depuis une image* du catalogue). Tout est calculé **localement**, dans le jeu : **aucune intelligence artificielle en ligne, aucune image envoyée à un service extérieur**. Les « estimations » sont des règles mesurables (forme, couleurs, symétrie…), affichées comme telles et toujours corrigeables.

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

## Shaders gratuits et studio visuel

Hub → **2. Shaders gratuits** : un clic applique un profil **Performance** (étalonnage seul, une passe), **Équilibré** (+ vignette, netteté), **Qualité** (+ halo lumineux), **Ultra** (halo large en deux itérations) ou ouvre le réglage **Personnalisé** ; *Désactiver les effets* revient au rendu vanilla. Tout est gratuit, sans compte ni achat. Hub → **3. Personnalisation** ouvre directement chaque section du studio (22 sections : luminosité, ciel, atmosphère, herbe et végétation, arbres et bois, eau, pierre et matériaux, mobs, animaux, personnages et armures, ombres, reflets, couleurs, textures, shaders, performances, profils, prévisualisation, diagnostic, compatibilité, réinitialisation). Touche **V** avec le cristal en main : studio directement.

- **Post-traitement GLSL réel** (chaînes de passes de Minecraft, shaders validés par `glslang` dans la CI) : exposition, gamma, contraste, saturation, vibrance, température, teinte, éclaircissement des grottes et de la nuit, intensité des zones sombres, netteté, vignette, recoloration du ciel par la profondeur, halo lumineux (qualités Qualité/Ultra), comparaison avant/après. Une erreur de shader est captée et le rendu vanilla est rétabli.
- **Brouillard** (couleur, distance, sous l'eau), **teintes d'herbe et de feuillage** (par biome conservé, puis teinte appliquée).
- **Textures** : pack de ressources généré à partir des textures **vanilla d'origine** du jeu, recolorées par groupe (chaque essence de bois, pierres, métaux, eau, mobs, animaux, armures) ; textures personnelles PNG validées (≤ 1024 px, ≤ 4 Mo).
- **Profils** JSON versionnés : préréglages (normal, lumineux, très lumineux, grottes, nuit, naturel, saturé, cinéma, anime, fantastique, performance, ultra), création, renommage, duplication, suppression, **export/import** validés ; fichier corrompu mis de côté (`.corrompu`) sans planter.
- **Performances** : FPS moyens et « 1 % bas » mesurés sur la machine ; **banc d'essai** comparant le rendu sans effets puis avec le profil actif, mêmes conditions ; verdict selon l'écart (0-50 correct, 51-100 attention, 101-149 important, ≥ 150 critique, pondéré par le niveau de départ) et conseil de qualité. Seules des valeurs réellement mesurées sont affichées.

## Commandes

Préfixe `/uab` (alias `/ultimateautobuilder`) :

| Commande | Rôle |
|---|---|
| `/uab give [joueurs]` | Donne l'Ultimate Crystal (niveau d'opérateur 2) |
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
| `freeBuild` | non | Constructeur gratuit, ou *mode ressources* (blocs prélevés dans l'inventaire en survie) |
| `replaceExisting`, `protectValuableBlocks`, `protectedBlocks`, `fluidMode` | | Ce qui peut être remplacé |
| `respectProtectionEvents` | oui | Interroge les mods de protection avant chaque bloc |
| `hollowLargeStatues`, `aerialHeight`, `foundationMaxDepth`, `restrictToSelection` | | Options de construction |
| `historySize`, `prudentUndo`, `checkpointSeconds` | 20, oui, 30 | Historique, retour arrière, reprise après redémarrage |
| `maxImportKilobytes` | 8192 | Taille maximale d'un import et d'un modèle envoyé par Image → Construction |
| `starterKit`, `starterHouse` | oui, oui | Kit et maison de départ pour les nouveaux joueurs |
| `worldStructures` | oui | Maisons, châteaux et sanctuaire générés dans les nouveaux chunks |
| `houseSpacingChunks`, `castleSpacingChunks` | 14, 36 | Taille des grilles de génération (rareté) |
| `spawnerMaxCycles` | 3 | Cycles de 4 créatures avant épuisement d'un spawner |
| `structureBudgetMs` | 4 | Temps maximal par tick pour poser les structures générées |

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
│   ├── adventure/   Règles de spawn (équiprobabilité, Warden 0,5 %, variantes), cycles, clés, récompenses d'armure, générateurs de maisons/châteaux/sanctuaire, contrôle d'accès
│   └── render/      Rendu isométrique (miniatures de secours, tests)
├── builder/         Moteur serveur : plan, tâches de pose/restauration, gestionnaire, tickets de chunks
├── network/         Paquets client ↔ serveur
├── command/         /uab
├── config/          Configs client et serveur
├── storage/         Historique, journaux d'annulation, modèles personnalisés
├── adventure/       Données du monde, génération et pose des structures, serrures, trésors, variantes et Warden, kit, récompenses
├── block/           Coffre verrouillé, porte de coffre-fort, spawner de château
├── item/, registry/ Ultimate Crystal (et anciens cristaux), onglet créatif
├── client/          Hub de l'Ultimate Crystal, catalogue, paramètres, aperçus 3D texturés (render/), fantôme, manette, barre de progression, mode capture CI (dev/)
│   ├── image/       Écran Image → Construction, sélecteur de fichiers, réception téléphone, envoi au serveur
│   └── visual/      Studio visuel : profils, post-traitement GLSL, brouillard, couleurs, pack de textures, FPS
└── gametest/        Scénarios exécutés dans un serveur réel
```

## Tests

- **Tests unitaires** (`src/test`, JUnit 5) — catalogue (454 modèles, absence de Pikachu/Pokémon/anime, identifiants et noms uniques, objectifs par catégorie, recherche sans accents), génération (dimensions conformes à chaque taille, aucune cellule hors limites, palettes valides, déterminisme, couleur visible de chaque bloc), placement, import (.uabm, fichiers corrompus ou trop grands rejetés, .vox), rendu des miniatures, **aventure** (12 tests : équiprobabilité des créatures (test du χ²), exclusions, familles séparées, taux Warden de 0,5 % mesuré sur 2 millions de tirages, distribution des variantes, catégories jamais mélangées, cycle de 4 créatures et épuisement, clés, ensembles d'armure, chaque coffre/spawner/coffre verrouillé de chaque structure accessible à pied (parcours en largeur), trois spawners bébé/moyen/normal par château, planchers et toits complets), studio visuel (profils, bornes, couleurs, verdict FPS), **Image → Construction** (26 tests : chargement et rejets — vide, corrompu, tronqué, WebP, dimensions excessives —, réduction des grandes images, séparation sujet/fond, transparence, estimation du sujet, repli sans fond, hauteur et proportions, blocs vus/estimés, couleurs de face, enveloppe multi-vues, modes d'arrière, styles vanilla/voxel art/miniature/géant/personnalisé, niveau de détail, bâtiment creux, vues et couches, comparaison mesurée, plan et étapes, conversion vers le moteur, annuler/rétablir, projet sauvegardé/relu avec nom piégé, limite de volume).
- **GameTests** (`./gradlew runGameTestServer`, dans un vrai serveur) — petite maison, statue tournée et **maison Sakura** construites, Pikachu absent du catalogue, **tables de butin** tirées 60 fois chacune (enchantements compatibles, aucun diamant/Netherite dans les coffres communs, Efficacité XXV/XXX présente en OP), **clés** (mauvaise clé non consommée, bonne clé consommée une fois), **spawner bébé** (uniquement des bébés, 4 au plus), **variantes du Warden** et clé OP seulement après une vraie victoire, une seule fois, **kit de départ** exact et unique, **ensemble en fer** (rien avec 3 pièces, une seule récompense), **placeur** de structure (coffres remplis, porte), onglet créatif et recettes, annulation, retour arrière, refus hors hauteur, mode prudent, palette de tous les modèles résolue dans le registre 1.21.1, mode couche par couche avec pauses et reprises, zone vide uniquement qui refuse les obstacles, pause manuelle qui gèle la progression, **tous les blocs d'Image → Construction existent**, **chaîne complète image → analyse → reconstruction → format réseau → construction par le moteur**.
- **Scénarios manuels conseillés** : ouvrir le hub avec l'Ultimate Crystal (clic droit, B, V, R1) et sans cristal ; en survie : nouveau monde (kit, maison de départ), exploration jusqu'aux maisons et châteaux générés, clés et coffres verrouillés, carte au trésor jusqu'au coffre, combat contre les créatures des spawners ; multijoueur à deux joueurs (kit propre à chacun, récompense au vainqueur) ; naviguer au pad ; aperçu fantôme puis construction ; pause/reprise ; studio : préréglages, banc d'essai, export/import de profil ; Image → Construction : photo depuis le PC puis depuis un téléphone, génération, édition, envoi et construction ; serveur dédié avec un joueur non opérateur.

## Rapport de vérification

Réalisé dans l'environnement de développement (conteneur sans accès aux dépôts Mojang/NeoForge) et par la CI :

| Élément | État |
|---|---|
| Cœur `core/**` et parties pures du studio | ✅ Compilés localement avec JDK 21 : **75 tests JUnit, 0 échec** (dont 12 sur l'aventure : probabilités, cycles, clés, accessibilité des structures) |
| Couche Minecraft/NeoForge | ✅ **Compilée par la CI** contre NeoForge 21.1.x ; shaders GLSL validés par `glslang` |
| GameTests (vrai serveur 1.21.1) | ✅ 21 scénarios, dont : tables de butin tirées 60 fois chacune (enchantements compatibles, ni diamant ni Netherite dans les coffres communs, Efficacité XXV/XXX en OP), clés (mauvaise clé non consommée, bonne clé consommée une fois), spawner bébé (bébés uniquement, 4 au plus), variantes du Warden et clé OP seulement après une vraie victoire (une seule fois), kit de départ exact et unique, ensemble en fer (rien avec 3 pièces, une seule récompense), placeur de structure. Les premiers passages ont révélé puis fait corriger : placeur bloqué sous le fond du monde, débris de Netherite dans le butin légendaire |
| Client réel (CI, rendu logiciel) | ✅ Captures « Captures en jeu » : kit de départ dans la barre d'action, hub (accueil, clés, récompenses), récupération de la récompense de l'ensemble en fer avec animation (8 diamants + 16 émeraudes reçus), carte au trésor avec croix rouge, château abandonné posé (≈ 2 s sous budget de 4 ms/tick), Boss Warden (échelle 1,4, santé 1 000, barre de boss), maison légendaire, coffre verrouillé ouvert avec une clé rare (butin tiré). Elles ont révélé puis fait corriger : maison de départ refusée sur les mondes plats |
| Non vérifié | ❌ Partie réelle d'un humain (exploration longue, combats, manette), génération des structures pendant une vraie exploration de monde normal (seule la pose directe est capturée), apparition naturelle d'un Warden (0,5 % : non observée, testée statistiquement et par apparition forcée), multijoueur sur serveur dédié, carte graphique réelle, effets visuels des shaders |
| Gains de FPS | ❌ **Aucun chiffre annoncé** : rien n'a été mesuré sur une vraie machine |

## Limites connues

- **Aventure** :
  - les structures n'apparaissent que dans les chunks **générés après l'installation** (jamais sur une zone existante, pour ne rien abîmer) ; dans un monde déjà très exploré, il faut s'éloigner ;
  - les structures sont posées progressivement par le serveur après la génération du terrain (quelques secondes pour un château), pas par le générateur de monde de Minecraft : elles n'apparaissent pas dans `/locate` ;
  - les variantes « moyennes » et les bébés non natifs (squelettes, araignées, illageois, Warden) utilisent l'attribut d'échelle de la 1.21 : même modèle et mêmes textures que l'adulte, en plus petit ;
  - le Warden des spawners n'est invulnérable que pendant son émergence (comportement vanilla) ; il ne s'enfouit plus tant qu'il est suivi par le mod ;
  - Efficacité est limitée à 255 par Minecraft : les niveaux XXV et XXX sont réels, des niveaux de 1 000 ne sont pas possibles et ne sont pas proposés ;
  - les clés sont à usage unique et n'ouvrent que les serrures de leur rareté (choix explicite).
- **Aperçus 3D** : catalogue, grand aperçu, fantôme et Image → Construction utilisent les vrais modèles et textures des blocs. Les blocs dessinés par un rendu d'entité (coffres, lits, panneaux, bannières, têtes) et les fluides y sont représentés par un bloc d'apparence proche (tonneau, laine, verre bleu…) ; dans le monde, ce sont bien les vrais blocs qui sont posés. Les grands modèles sont réduits pour l'aperçu (40 blocs pour les cartes, 112 pour le grand aperçu) ; au-delà du nombre de faces configuré, le fantôme repasse en faces colorées.
- **Échelle d'interface** : les écrans du constructeur et d'Image → Construction garantissent une surface de 640×360 unités ; à l'échelle automatique sur un petit écran, le texte y est donc un peu plus petit que dans le reste du jeu (jamais sous un pixel réel par unité).
- **Retour arrière** : restaure les blocs et le contenu des blocs-entités remplacés ; les entités (mobs, objets au sol, cadres) ne sont pas restaurées. En mode prudent, un bloc modifié par un joueur après la construction est conservé.
- **Multijoueur et modèles personnalisés** : le client reçoit un aperçu réduit (48 blocs de haut max) pour les miniatures et le fantôme ; la construction utilise le modèle complet du serveur.
- **Mode ressources** : vérifie et retire les blocs de l'inventaire au lancement (rien n'est retiré en mode créatif) ; les blocs sans objet correspondant (feu, portails…) ne sont pas comptés.
- **R1** : dépend de la reconnaissance de la manette par GLFW (voir plus haut).
- **Image → Construction** : reconstruction **heuristique** à partir de silhouettes et de couleurs, pas une compréhension de la scène. Une seule photo ne montre ni la profondeur ni l'arrière : ils sont estimés (et signalés). Les sujets fins ou complexes (membres écartés, arches, intérieurs, objets transparents) sont simplifiés ; un fond chargé peut être conservé avec le sujet (signalé). Les vues complémentaires améliorent la forme mais une enveloppe de silhouettes ne reproduit pas les creux. Couleurs : correspondance avec la couleur moyenne approximative de chaque bloc. La réception depuis un téléphone nécessite que l'ordinateur et le téléphone soient sur le même réseau local et que le pare-feu l'autorise. Le décodage des images utilise `javax.imageio` de Java.
- **Studio visuel** : le post-traitement s'applique à l'image du monde (pas à l'interface) ; il ne calcule **ni ombres projetées ni reflets** : la section *Ombres* règle seulement l'intensité des tons sombres de l'image et la section *Reflets* indique que la fonction n'est pas disponible (il faudrait un pipeline de rendu dédié). La section *Compatibilité* détecte Iris, Oculus, Embeddium, Sodium… et conseille de désactiver le post-traitement quand un pack de shaders est actif, pour ne pas cumuler deux étalonnages. La recoloration de textures régénère le pack et recharge les ressources (quelques secondes).
- **Miniatures de secours** : pendant la préparation d'un aperçu 3D (une fraction de seconde), une miniature isométrique simplifiée s'affiche ; elle peut montrer de fines rayures claires sur les toits en escalier, qui n'existent pas dans le monde.
