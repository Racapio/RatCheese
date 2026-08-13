# Rat Cheese Helper 🧀🐀

[English](README.md) · [Русский](README.ru.md) · **Français**

Mod Fabric côté client pour Hypixel Skyblock (Minecraft **26.1.2** et **26.2**) construit autour du familier **Rat**.

![Build](https://github.com/Racapio/RatCheese/actions/workflows/build.yml/badge.svg)

## Fonctionnalités

1. **Surbrillance du fromage.** Le fromage que fait apparaître votre rat ne vit qu'environ 10 secondes. Tant qu'il est dans votre champ de vision, le mod lui applique un contour lumineux (couleur configurable) et l'affiche **jusqu'à 5× plus grand** pour vous laisser le temps de l'attraper. Rien n'est révélé à travers les blocs opaques.
2. **HUD des buffs :**
   - qui vous avez buffé : `Vous → Zellion63 : +7✯ MF, 43s` — le nom est coloré selon le rang du joueur, le compte à rebours peut être remplacé par une barre de temps qui se réduit ;
   - les buffs que d'autres rats vous donnent ;
   - compteur de butin de rat (RAT BLESSING) avec une ligne flash pour chaque nouveau butin ;
   - une ligne « Vous sentez du FROMAGE à proximité ! » jusqu'à ce que le fromage soit ramassé ou disparaisse.
3. **Alertes** — titre plein écran + son à l'apparition du fromage (les deux sont optionnels).
4. **24 succès** — de « Premier Reniflement » à « Roi des Rats », dont 7 secrets (certains avec des indices cryptiques), dates de déblocage et statistiques à vie.

## Installation

1. [Fabric Loader](https://fabricmc.net/use/installer/) pour Minecraft 26.1.2 ou 26.2 (loader ≥ 0.19.3).
2. Dans `mods` :
   - [Fabric API](https://modrinth.com/mod/fabric-api) correspondant à votre version de Minecraft ;
   - `ratcheese-<version>+<version mc>.jar` (voir les [Releases](https://github.com/Racapio/RatCheese/releases) ou compilez-le vous-même) ;
   - optionnellement [Mod Menu](https://modrinth.com/mod/modmenu) — accès aux réglages directement depuis la liste des mods.

## Commandes

| Commande | Description |
|---|---|
| `/ratcheese` | Réglages (surbrillance, couleur, taille du fromage, HUD, alertes) |
| `/ratcheese hud` | Éditeur de HUD : glisser à la souris, molette pour l'échelle, flèches pour ajuster, clic droit pour réinitialiser |
| `/ratcheese achievements` | Liste des succès avec progression |
| `/ratcheese test` | Injecte des données d'exemple pour prévisualiser le HUD |
| `/ratcheese reset` | Réinitialise les buffs de la session et le compteur de butin |
| `/ratcheese scan` | Debug : liste les entités props dans un rayon de 8 blocs |
| `/ratcheese addname <texte>` / `addtexture <hash>` | Marqueurs supplémentaires de détection du fromage |

Config : `.minecraft/config/ratcheese.json`, données des succès : `ratcheese_achievements.json`.

## Détection du fromage

Le hash de texture de la tête de fromage d'Hypixel est intégré au mod — la surbrillance fonctionne dès l'installation. Si Hypixel la change un jour : placez-vous près d'un fromage, lancez `/ratcheese scan` et cliquez sur le **[+]** vert à côté de la bonne tête — la texture est ajoutée à votre config.

## Compiler depuis les sources

Nécessite JDK 25. Le projet est multi-version (Stonecutter), ciblant Minecraft 26.1.2 et 26.2 :

```
gradlew :26.1.2:build :26.2:build
```

Ou une seule version :

```
gradlew :26.1.2:build
```

Les jars se trouvent dans `versions/<version mc>/build/libs/`, ou dans `build/libs/<version du mod>/` après `gradlew :26.1.2:buildAndCollect :26.2:buildAndCollect`.

## Avertissement

Client-side uniquement : le mod n'automatise rien et ne lit que votre propre chat. Comme pour tout mod sur Hypixel — utilisez-le à vos risques et périls. Non affilié à Hypixel.
