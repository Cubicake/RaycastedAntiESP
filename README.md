### Latest stable version: v1.6.5

The latest stable version can currently only be found on Modrinth https://modrinth.com/plugin/raycasted-anti-esp/ or by compiling from `v1.6.x`. The latest alpha version can be found on the [Discord](https://discord.gg/hGTRAK2hNM) or by compiling from `main`.

This is a predominantly async plugin for PaperMC and its forks that hides/culls entities (and tile entities) from players if they do not have line-of-sight.

The supported versions are 1.21.x PaperMC and Pufferfish. Other server versions and software may work too. v2 alpha builds support 26.x.

## Use cases:

- Prevent cheating (anti-esp hacks)
  - Block usage of pie-ray to locate underground bases
  - Prevent mods such as mini-maps or cheat clients from displaying the locations of hidden entities
- Increase client-side performance for low-end devices
  - Massive megabases containing hundreds of armour stands, item frames, banners etc can cause performance issues on low-end devices unable to process so many entities. REO will cull those entities for the client, reducing the number of entities to process.
- Hide nametags behind walls
  - Yes, this plugin is a bit overkill for doing that, yes you can do it anyways.
 
## Dependencies:
- Packetevents (soft depend)
  - Only needed if you are using the cull-players option and wish for the players to remain in the tablist

## Known issues:
- Due to the nature of the plugin, there will be a short delay once an entity should be visible before it appears, causing it to appear like it "popped" into view. This issue is partially resolved by turning engine-mode to 2, and is worse for players with higher ping.

## Versioning:
Note that the following versioning information only applies to v2 and beyond.

RaycastedAntiESP binaries are composed of four distinct modules: the core, Locatable-lib (used for platform-independent location objects), [CubiLogging](https://github.com/Cubicake/CubiLogging/tree/main), and a platform adapter.

Each of these has its own versioning system. Locatable-lib and the logging api both declare public apis, and follow [Semantic Versioning](https://semver.org/#semantic-versioning-specification-semver). The platform adapter and core do not declare public apis, and thus [cannot follow semantic versioning](https://semver.org/#spec-item-1). They still follow a major.minor.patch versioning system, but which version number increments is less deterministic. Generally, major version bumps will be for when significant refactoring or rewriting has occurred, or a very significant distinct feature has been added or removed. Minor version bumps will be for the addition/removal of smaller distinct features, while patch version bumps are for bug fixes and tweaks or minor addition/removals which are related to the features added/removed in the minor version bumps.

In addition to the versions for each module, there is also an overall version for each platform binary, which is the version that is advertised in the description and file name, and is used for update checks. This version is a combination of the core and platform adapter versions, in the format `{core version}-PlatformName-{platform adapter version}`. This allows all platforms to share the same first three version numbers, while still allowing for differences in the platform adapter versions.

## Contributions:
Contributions via pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for details.
