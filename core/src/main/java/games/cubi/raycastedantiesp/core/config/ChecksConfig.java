/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import games.cubi.raycastedantiesp.core.config.raycast.ChunkSectionConfig;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.SoundEffectsConfig;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record ChecksConfig(
        @Setting("player") PlayerConfig playerConfig,
        @Setting("entity") EntityConfig entityConfig,
        @Setting("tile-entity") TileEntityConfig tileEntityConfig,
        @Setting("sound-effects") SoundEffectsConfig soundEffectsConfig,
        @Setting("chunk-section") ChunkSectionConfig chunkSectionConfig
) implements Config {
    public static final ChecksConfig DEFAULT = new ChecksConfig(
            PlayerConfig.DEFAULT,
            EntityConfig.DEFAULT,
            TileEntityConfig.DEFAULT,
            SoundEffectsConfig.DEFAULT,
            ChunkSectionConfig.DEFAULT
    );

    public boolean hasEnabledStatusChanges(ChecksConfig startup) {
        return playerConfig.enabled() != startup.playerConfig.enabled()
                || entityConfig.enabled() != startup.entityConfig.enabled()
                || chunkSectionConfig.enabled() != startup.chunkSectionConfig.enabled();
    }
}
