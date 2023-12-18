# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)

## [Unreleased]

### Added

- Classic Flight mode: Turns on the original flight system by Rehjah
- 
### Changed

- More refactors for project readability
- Client Tick events fire once per tick instead of twice
- Tick events tweaked to compensate for event changes.
## [1.19.2 - 1.1.3] - December 16th, 2023

### Added

- New flight impulse for modern system. Still in development.

## [1.19.2 - 1.1.2] - December 7th, 2023

### Changed

- Improved project structure for better readability and cohesion


## [1.19.2 - 1.1.1] - December 6th, 2023

### Added

- FlightHandler class containing methods for player flight
- CommonEvents class containing common event listeners
- BetterFlightCommonConfig class containing common configs and static values
- Keybinding class for storing keybindings settings and constructors

### Fixed

- Farmer Delight's Nourishment Effect now works with Better Flight hunger drain.

### Changed

- Flight exhaustion no longer directly drains exhaustion, is queued like other exhaustion sources
- All Flight impulse handlers now use push() for movement.
- Refactored ServerLogic into seperate CommonEvents listener, FlightHandler, and BetterFlightCommonConfig classes
- Refactored ClientLogic so keybind builders are in seperate Keybinding class.
- ClientLogic.updateElytraStatus rewritten to future-proof against upcoming Curios API changes
- Started decoupling and simplifying event logic for flight
- ClientLogic.updateElytraStatus(player) renamed to isPlayerWearingElytra(player) for clarity of new usage
### Removed

- 

## [1.19.2-Forge-1.1.0] - November 30th, 2023

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