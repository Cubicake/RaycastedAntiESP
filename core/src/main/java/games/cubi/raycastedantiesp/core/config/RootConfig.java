/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import games.cubi.raycastedantiesp.core.config.engine.EngineConfig;

import java.util.Map;

public record RootConfig(String configVersion, ChecksConfig checksConfig, EngineConfig engineConfig, BlockProcessorConfig blockProcessorConfig, DebugConfig debugConfig, UpdateConfig updateConfig, Map<Class<? extends Config>, Config> extensionConfigs) implements Config {
    public <T extends Config> T extensionConfig(Class<T> type) {
        Config config = extensionConfigs.get(type);
        return type.cast(config);
    }
}
