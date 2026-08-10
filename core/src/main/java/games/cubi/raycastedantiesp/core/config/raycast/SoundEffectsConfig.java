/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.Config;
import games.cubi.raycastedantiesp.core.config.ConfigReader;
import org.spongepowered.configurate.ConfigurationNode;

public record SoundEffectsConfig(
        boolean enabled,
        int maxOccludingCount,
        int alwaysPlayRadius,
        int raycastRadius
) implements Config {
    public static SoundEffectsConfig load(ConfigurationNode node, String path) {
        return new SoundEffectsConfig(
                ConfigReader.bool(ConfigReader.node(node, "enabled"), path + ".enabled"),
                ConfigReader.integer(ConfigReader.node(node, "max-occluding-count"), path + ".max-occluding-count"),
                ConfigReader.integer(ConfigReader.node(node, "always-play-radius"), path + ".always-play-radius"),
                ConfigReader.integer(ConfigReader.node(node, "raycast-radius"), path + ".raycast-radius")
        );
    }
}
