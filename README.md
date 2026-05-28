### Latest stable version: v1.6.5 | Latest alpha version: Download from the Discord or compile from `main`

The latest stable version can currently only be found on Modrinth https://modrinth.com/plugin/raycasted-anti-esp/ or by compiling from `v1.6.x`. The latest alpha version can be found on the [Discord](https://discord.gg/hGTRAK2hNM) or by compiling from `main`.

This is an async plugin for PaperMC and its forks that hides entities (including players) and tile entities (blocks such as chests, banners, signs, etc) from players if they do not have line-of-sight.

The supported versions are 1.20.6+, with MC 26.x support in v2 only.

## Use cases:

- Prevent cheating (anti-esp hacks)
  - Block usage of pie-ray to locate underground bases
  - Prevent mods such as mini-maps or cheat clients from displaying the locations of hidden entities
- Increase client-side performance for low-end devices
  - Massive megabases containing hundreds of armour stands, item frames, banners etc can cause performance issues on low-end devices unable to process so many entities. Raycasted AntiESP will cull those entities for the client, reducing the number of entities to process.
- Hide nametags behind walls
  - Yes, this plugin is a bit overkill for doing that, yes you can do it anyways.
 
## Dependencies:
- Packetevents
  - In v1: Only needed if you are using the cull-players option and wish for the players to remain in the tablist
  - In v2: Required for all features

## Known issues:
- Due to the nature of the plugin, there will be a short delay once an entity should be visible before it appears, causing it to appear like it "popped" into view.

## Versioning:
Note that the following versioning information only applies to v2 and beyond.

RaycastedAntiESP binaries are composed of four distinct modules: the core, Locatable-lib (used for platform-independent location objects), [CubiLogging](https://github.com/Cubicake/CubiLogging/tree/main), and a platform adapter.

Each of these has its own versioning system. Locatable-lib and the logging api both declare public apis, and follow [Semantic Versioning](https://semver.org/#semantic-versioning-specification-semver). The platform adapter and core do not declare public apis, and thus [cannot follow semantic versioning](https://semver.org/#spec-item-1). They still follow a major.minor.patch versioning system, but which version number increments is less deterministic. Generally, major version bumps will be for when significant refactoring or rewriting has occurred, or a very significant distinct feature has been added or removed. Minor version bumps will be for the addition/removal of smaller distinct features, while patch version bumps are for bug fixes and tweaks or minor addition/removals which are related to the features added/removed in the minor version bumps.

In addition to the versions for each module, there is also an overall version for each platform binary, which is the version that is advertised in the description and file name, and is used for update checks. This version is a combination of the core and platform adapter versions, in the format `{core version}-PlatformName-{platform adapter version}`. This allows all platforms to share the same first three version numbers, while still allowing for differences in the platform adapter versions.

## Credits:

- Cubicake (Sole developer, creator and maintainer of RaycastedAntiESP)

### Special mentions:

- Strokkur424 and other contributors to [StrokkCommands](https://github.com/Strokkur424/StrokkCommands), an LGPL-licensed open-source annotation-based brigadier command tree generator.
  - While StrokkCommands is not essential for the functioning of the project, it makes handling commands infinitely easier.
- Retrooper, Booky10, and all other contributors to [PacketEvents](https://github.com/retrooper/packetevents), a GPL-licensed open-source library for handling minecraft packets.
  - PacketEvents is essential for the functioning of the plugin, as it allows for handling of packets across multiple platforms and Minecraft versions.
- All contributors to Paper and its upstream projects Spigot and Bukkit, without which none of this would be possible.

## Contributions:
Contributions via pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## Forking:

Please see the the [AGPL v3 license](LICENSE.md)—especially section 5, section 7, and section 13—before forking this repository, as there are some specific legal requirements for forks. 

In summary, translated into layman, plugin-specific terms, the most important requirements for forks are as follows:
1. Forks must not remove any names currently present in the authors/contributors list in the plugin's description, as this falls under the "preservation of specified reasonable legal notices or author attributions".
2. Forks must not claim to be the original software, as this falls under the "no misrepresentation of the origin of the material".
3. Per section 5 subsection c, derivative works such as forks must remain licenced under AGPLv3, and must include a copy of the license with the work. To be clear, copying any non-trivial amount of code from this repository into your own repository, whether via making a fork, directly copying and pasting or other means, makes it a derivative work and obligates you to license your derivative work under the AGPL v3. You must then include a copy of the license and make the source code (not decompiled code) accessible to all users of your program, explicitly including players on a multiplayer server as well as the server owners.
4. "Appropriate Legal Notices" such as the copyright notice, license notice, disclaimer, and link to the source code must be preserved. Currently this is implemented via the /raycastedantiespCredits command, and while you are not obligated to keep this specific implementation, if you remove the command you must provide an equally prominent way for all users of the plugin (including players on a multiplayer server) to easily view the same information.

`games.cubi.raycastedantiesp.paper.commands.Attribution` has been written with forks in mind, and a template has been left for forks to modify so that the AGPLv3 license can be obeyed with minimal effort from forkers.

## Copyright and Disclaimer:
Copyright © 2025-2026 Cubicake and Contributors

This project is licensed under the GNU Affero General Public License v3.0 only (AGPLv3). You may copy, modify, and redistribute this software only in compliance with the terms of that licence. A copy of the licence is provided in the LICENSE file.

If you modify and deploy this software for remote network interaction, including operating a public multiplayer server, you must make the complete corresponding source code of the modified version available to users interacting with the software over the network, as required by section 13 of the AGPLv3.

Any modified versions must be clearly marked as modified and must not be misrepresented as the original project.

This software is provided “as is”, without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and noninfringement. In no event shall the authors or copyright holders be liable for any claim, damages, or other liability arising from, out of, or in connection with the software or the use of the software.

The AGPL is the sole license this program is governed by. All statements found in this copyright notice and disclaimer, the above forking guidelines, or anywhere else in this program should be considered merely guidelines and interpretations of the AGPL unless they are direct extracts of the AGPL. If any statement conflicts with the AGPL, the AGPL takes legal precedence over any such statement. However, any such conflicting statements should be taken as good-faith non-binding requests by the authors.