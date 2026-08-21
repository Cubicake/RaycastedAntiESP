/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.ConfigurationNode;

public interface ConfigExtension<T extends Config> {
    Class<T> type();

    T load(ConfigurationNode config, BlockProcessorConfig blockProcessorConfig);

    default boolean requiresRestart(T startupConfig, T nextConfig) {
        return !startupConfig.equals(nextConfig);
    }
}
