<center><div align="center">

<img height="150" src="common/src/main/resources/icon.png" width="150"/>

# RyoamicLights

[![GitHub license](https://img.shields.io/github/license/ThinkingStudios/RyoamicLights?style=flat-square)](https://raw.githubusercontent.com/ThinkingStudios/RyoamicLights/1.20.4-architectury/LICENSE)
![Environment: Client](https://img.shields.io/badge/environment-client-1976d2?style=flat-square)

[LambDynamicLights](https://github.com/LambdAurora/LambDynamicLights) unofficial architectury port.

A dynamic lights mod for Minecraft.

<img alt="forge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/forge_vector.svg"> (1.16.5~1.20.1)
<img alt="neoforge" height="56" src="https://github.com/mc-wiki/minecraft-mod-heywiki/blob/master/docs/supports_neoforge.svg?raw=true"> (≥1.20.1)
<img alt="fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg">

</div></center>

## 📖 What's this mod?

This mod adds dynamic lights to Minecraft. Dynamic lights are lights created by an entity holding an
item which makes light as a block, or created by an entity on fire, etc.

Searching other mods to replace OptiFine?
[Check out this list!](https://lambdaurora.dev/optifine_alternatives)

## ✅ Features:

- Dynamic lights.
- Settings to select how smooth the dynamic lighting is.
- Works with some modded items.
- Any entity holding an item which emits light will emit light!
- Magma cubes emit light.
- Spectral arrows emit light.
- Burning entities emit light.
- Blazes emit light.
- Some items like torches, soul torches, etc. will not light up in water.
- Quick and simple API for developers.
- And more!

## Screenshots

### Items emit light

Dropped items which already emit light as a block, will also dynamically emit light!

![Torch](images/drop_item_light.png)

### Held items emit light

Light is emitted when entities hold light emitting items.

![Fox holding lantern](images/held_item_light.png)

### Fire! Fire! Fire!

Any entity on fire will emit light!

![Skeleton on fire!](images/entity_fire_light.png)

### Spectral arrows

Spectral arrows will emit a very weak light!

![Spectral arrows](images/spectral_arrows_light.png)

### Different luminance!

Light emitted from items depend on the light emitted from their respective blocks!

![light levels](images/different_luminance.png)

### Configuration GUI

![Configuration GUI](images/settings_main.png)
![Entities Configuration](images/settings_entities.png)

## 📖 Usage

Using this mod is very simple!

Install it in your mods folder along with [ObsidianUI](https://modrinth.com/mod/obsidianui), (and [Sodium (Fabric/Quilt/NeoForge)](https://modrinth.com/mod/sodium)/[Embeddium (Fabric/Forge/NeoForge)](https://modrinth.com/mod/embeddium) if wanted for better performances).

You will notice nothing at first but if you go into the video options or into the settings screen of the mod via Forge's Mod List, you will notice an option called Dynamic Lights which is by default off, choose the wanted configuration and enjoy!
You can also configure the mod by editing the file in `config/ryoamiclights.toml`.

### Build

Just do `./gradlew build` and everything should build just fine!

## 📖 How does it work internally?

Check [this documentation](https://github.com/LambdAurora/LambDynamicLights/blob/1.20/HOW_DOES_IT_WORK.md).

## 📖 Is there an API? How to use it as a developer?

Check [this documentation](https://github.com/ThinkingStudios/RyoamicLights/wiki/API).

# 📖 Compatibility

- [Sodium (Fabric/Quilt)](https://modrinth.com/mod/sodium)/[Embeddium (Fabric/Forge/NeoForge)](https://modrinth.com/mod/embeddium) is recommended for better performances.
- **OptiFine/OptiForge/OptiFabric is obviously incompatible.**
- [Incompatible list](https://github.com/ThinkingStudios/RyoamicLights/issues/6)
