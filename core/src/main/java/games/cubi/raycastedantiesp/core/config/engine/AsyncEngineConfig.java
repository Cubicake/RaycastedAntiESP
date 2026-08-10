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

public record AsyncEngineConfig(int asyncProcessingThreads) implements Config {
    public static AsyncEngineConfig load(ConfigurationNode node) {
        int threads = ConfigReader.integer(ConfigReader.node(node, "processing-threads"), "engine.async.processing-threads");
        if (threads < 1) {
            throw new ConfigLoadException("engine.async.processing-threads must be at least 1");
        }
        return new AsyncEngineConfig(threads);
    }
}
