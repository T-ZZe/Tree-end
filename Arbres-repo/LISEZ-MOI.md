# Arbres 1.2

## Compiler
GitHub Actions (`.github/workflows/build.yml`) ou `mvn package` -> `target/Arbres-1.2.jar` (Java 25, Paper 26.3).
Supprime l'ancien `config.yml` du plugin avant de relancer (nouvelles sections).

## Corrections
- Nether : chaque fungus garde sa couleur (crimson / warped), troncs 2x2 seulement sur sol plein, ombrelle réduite sous un plafond, tronc prioritaire sur les verrues voisines.
- Pics de glace : les pics normaux sont supprimés, il ne reste que la version améliorée (plus fréquente).
- Jardin pâle : le coeur de creaking ne coupe plus la détection de l'arbre (restes en l'air) et les arbres reconvertis ou plantés reçoivent des coeurs (`pale.heart-chance`).
- Forêt tachetée : les peupliers (3 couleurs de feuilles) sont refaits en grands arbres, + feuilles mortes, buissons rouges, terre stérile, peupliers tombés.

## Nouveautés
- End : plus d'arbres en bois. Zones mortes sur les îles extérieures : arbres d'os (branches fourchues, griffes, toiles, crânes), sol mort, ossements, biome mort.
- Nouveau biome : Forêt de cristaux (aiguilles d'améthyste, amas lumineux, calcite), zones de 80 à 180 blocs.
- Oasis : plus grandes, fleurs, nénuphars, ruines de grès. Campement : grande tente rayée, lit, coffre, tonneau, établi, lanterne, feu de camp, souches, torches.
