/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.engine;

import games.cubi.raycastedantiesp.core.config.Config;
import games.cubi.raycastedantiesp.core.config.ConfigLoadException;
import games.cubi.raycastedantiesp.core.config.ConfigReader;
import org.spongepowered.configurate.ConfigurationNode;

public record EngineConfig(EngineMode mode, AsyncEngineConfig asyncConfig) implements Config {
    public static EngineConfig load(ConfigurationNode root) {
        ConfigurationNode node = ConfigReader.node(root, "engine");
        String modeName = ConfigReader.string(ConfigReader.node(node, "mode"), "engine.mode");
        EngineMode mode = EngineMode.fromString(modeName);
        if (mode == null) {
            throw new ConfigLoadException("engine.mode has unsupported value '" + modeName + "'");
        }
        return new EngineConfig(mode, AsyncEngineConfig.load(ConfigReader.node(node, "async")));
    }

    public EngineMode getMode() {
        return mode;
    }
}
