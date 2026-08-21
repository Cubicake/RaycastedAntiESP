/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import games.cubi.raycastedantiesp.core.config.raycast.ChunkSectionConfig;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.SoundEffectsConfig;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import org.spongepowered.configurate.ConfigurationNode;

public record ChecksConfig(PlayerConfig playerConfig, EntityConfig entityConfig, TileEntityConfig tileEntityConfig, SoundEffectsConfig soundEffectsConfig, ChunkSectionConfig chunkSectionConfig) implements Config {
    public static ChecksConfig load(ConfigurationNode root) {
        ConfigurationNode checks = ConfigReader.node(root, "checks");
        return new ChecksConfig(
                PlayerConfig.load(ConfigReader.node(checks, "player"), "checks.player"),
                EntityConfig.load(ConfigReader.node(checks, "entity"), "checks.entity"),
                TileEntityConfig.load(ConfigReader.node(checks, "tile-entity"), "checks.tile-entity"),
                SoundEffectsConfig.load(ConfigReader.node(checks, "sound-effects"), "checks.sound-effects"),
                ChunkSectionConfig.load(ConfigReader.node(checks, "chunk-section"), "checks.chunk-section")
        );
    }

    public boolean hasEnabledStatusChanges(ChecksConfig startup) {
        return playerConfig.enabled() != startup.playerConfig.enabled()
                || entityConfig.enabled() != startup.entityConfig.enabled()
                || chunkSectionConfig.enabled() != startup.chunkSectionConfig.enabled();
    }
}
