# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)

## [1.19.2-Forge-1.1.1]

### Added

- 

### Fixed

- 

### Changed

- Flight exhaustion no longer directly drains exhaustion, is queued like other exhaustion sources (makes this compatbility with Farmer Delight's Nourishment effect)
- 
- All Flight impulse handlers now use push() for movement.

### Removed

- 

## [1.19.2-Forge-1.1.0]

### Added

- CHANGELOG.md for documentation of changes
- Repositories reference to cursemaven
- Self(ArkVeil) to credits
- properties for easier versioning in gradle.properties

### Fixed

- ElytraSlot curio compatability ([#8](https://github.com/rejahtavi/betterflight/issues/8))

### Changed

- Split main into branches for individual versions of minecraft
- Mappings channel: official 1.19 -> official 1.19.2
- Updated Dependencies: 
  - Forge 1.19-41.1.0 -> Forge 1.19.2-43.3.0
  - Curios 1.19-5.1.0.4 -> Curios 1.19.2-5.1.1.0
- Repository and dependencies use CurseMaven now.
- SElytraChargePacket.send(Player, Int) uses NETWORK.send() instead of NETWORK.sendTo()
- References in build.gradle changed to use properties
- mod.toml uses new properties for info

### Removed

- Direct repository link to theillusivec4 maven